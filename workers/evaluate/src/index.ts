import { Hono } from 'hono';
import { cors } from 'hono/cors';

interface Env {
  DB: D1Database;
  ADMIN_TOKEN: string;
  ADMIN_PASSWORD: string;
}

interface EvaluationRow {
  id: string;
  course_name: string;
  teacher: string;
  rating: number;
  content: string;
  anonymous: number;
  author: string;
  user_no: string;
  user_hash: string;
  device_id: string;
  likes: number;
  status: string;
  created_at: number;
  updated_at: number;
}

// ─── Security helpers & constants ───

const SESSION_TTL_SEC = 3600;
const MAX_CONTENT_LEN = 2000;
const MAX_AUTHOR_LEN = 50;
const MAX_COURSE_LEN = 100;

/** 恒定时间比较，避免令牌逐字节爆破 */
function timingSafeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

function clientIp(c: any): string {
  return c.req.header('CF-Connecting-IP') || c.req.header('X-Forwarded-For') || 'unknown';
}

/**
 * 进程内滑动窗口限流（轻量，防单实例滥用）。
 * 注意：Cloudflare Worker 实例间状态不共享，分布式限流需改用 KV / Durable Object。
 */
const rateBuckets = new Map<string, { count: number; reset: number }>();
function rateLimit(bucket: string, limit: number, windowSec: number): boolean {
  const now = Date.now();
  const b = rateBuckets.get(bucket);
  if (!b || now > b.reset) {
    rateBuckets.set(bucket, { count: 1, reset: now + windowSec * 1000 });
    return true;
  }
  if (b.count >= limit) return false;
  b.count++;
  return true;
}

// ─── HMAC 签名会话：登录后签发，过期失效，本身不是主密钥 ───
function b64url(buf: ArrayBuffer | Uint8Array): string {
  const bytes = buf instanceof Uint8Array ? buf : new Uint8Array(buf);
  let s = '';
  for (let i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]);
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
function b64urlDecodeBytes(s: string): Uint8Array {
  const t = s.replace(/-/g, '+').replace(/_/g, '/');
  const bin = atob(t);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}
function b64urlDecodeStr(s: string): string {
  return atob(s.replace(/-/g, '+').replace(/_/g, '/'));
}
async function signSession(secret: string, payload: object): Promise<string> {
  const enc = (o: object) => b64url(new TextEncoder().encode(JSON.stringify(o)));
  const data = `${enc({ alg: 'HS256', typ: 'JWT' })}.${enc(payload)}`;
  const key = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );
  const sig = await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(data));
  return `${data}.${b64url(sig)}`;
}
async function verifySession(secret: string, token: string): Promise<boolean> {
  const parts = token.split('.');
  if (parts.length !== 3) return false;
  const key = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['verify']
  );
  const ok = await crypto.subtle.verify(
    'HMAC',
    key,
    b64urlDecodeBytes(parts[2]),
    new TextEncoder().encode(`${parts[0]}.${parts[1]}`)
  );
  if (!ok) return false;
  try {
    const payload = JSON.parse(b64urlDecodeStr(parts[1]));
    return typeof payload.exp === 'number' && payload.exp > Math.floor(Date.now() / 1000);
  } catch {
    return false;
  }
}

/**
 * 管理鉴权：未配置 ADMIN_TOKEN 时显式拒绝（防 undefined === undefined 误放行）。
 * 接受签名会话令牌；过渡期同时兼容原始 ADMIN_TOKEN，便于已持有主密钥的调用方平滑迁移。
 */
async function checkAdmin(c: any): Promise<boolean> {
  const expected = c.env.ADMIN_TOKEN;
  if (!expected) return false;
  const token = c.req.header('Authorization')?.replace(/^Bearer\s+/i, '')?.trim();
  if (!token) return false;
  if (timingSafeEqual(token, expected)) return true;
  return verifySession(expected, token);
}

const app = new Hono<{ Bindings: Env }>();

// CORS 仅对公开只读接口开放；管理接口同源（管理后台同源部署），无需 CORS。
app.use('/courses', cors());
app.use('/evaluations', cors());
app.use('/evaluations/*', cors());

// ─── Helpers ───

/** 取 X-User-Hash（学号 SHA-256，客户端计算）。 */
function userHashOf(c: any): string {
  return (c.req.header('X-User-Hash') || '').trim();
}

/** 取 X-Device-Id（仅用于本地评价归属判定）。 */
function deviceIdOf(c: any): string {
  return (c.req.header('X-Device-Id') || '').trim();
}

function rowToEval(r: EvaluationRow, liked: boolean, userHashVisible: boolean) {
  return {
    id: r.id,
    course_name: r.course_name,
    teacher: r.teacher,
    rating: r.rating,
    content: r.content,
    anonymous: !!r.anonymous,
    // 匿名时隐藏作者信息
    author: r.anonymous ? '' : r.author,
    // 不返回学号明文（user_no）与设备 ID；user_hash 仅对"本人"或"管理员"可见，
    // 防止公开接口泄露他人标识（客户端依赖本人 user_hash 做"我的评价"过滤）。
    user_hash: userHashVisible ? r.user_hash : '',
    likes: r.likes,
    status: r.status,
    created_at: r.created_at,
    liked,
  };
}

/** 检查当前 user_hash 是否已封号 */
async function isBanned(db: D1Database, userHash: string): Promise<boolean> {
  if (!userHash) return false;
  const row = await db
    .prepare('SELECT 1 FROM banned_users WHERE user_hash = ?')
    .bind(userHash)
    .first();
  return !!row;
}

// 为一组评价计算当前用户是否已点赞；includeUserHash=true 时（管理员）始终暴露 user_hash
async function withLiked(c: any, rows: EvaluationRow[], includeUserHash = false): Promise<any[]> {
  const uh = userHashOf(c);
  return Promise.all(
    rows.map(async (r) => {
      let liked = false;
      if (uh) {
        const lr = await c.env.DB.prepare(
          'SELECT 1 FROM evaluation_likes WHERE user_hash = ? AND evaluation_id = ?'
        )
          .bind(uh, r.id)
          .first();
        liked = !!lr;
      }
      const visible = includeUserHash || r.user_hash === uh;
      return rowToEval(r, liked, visible);
    })
  );
}

function nowSec(): number {
  return Math.floor(Date.now() / 1000);
}

// ─── Public: list course summaries (aggregated) ───
// 按 (course_name, teacher) 联合聚合：平均分、评价数、最新评价时间。
// 同一门课不同老师各自独立一条（教学班差异大，不应混合平均）。
// 用于评价列表页的"课程卡片"视图（类似评分应用的课程列表）。
app.get('/courses', async (c) => {
  try {
    const rows = await c.env.DB.prepare(
      `SELECT
         course_name,
         COALESCE(NULLIF(TRIM(teacher), ''), '教师未知') AS teacher,
         ROUND(AVG(rating), 1) AS avg_rating,
         COUNT(*) AS eval_count,
         MAX(created_at) AS latest_at
       FROM evaluations
       WHERE status = 'approved'
       GROUP BY course_name, COALESCE(NULLIF(TRIM(teacher), ''), '教师未知')
       ORDER BY latest_at DESC`
    ).all<{
      course_name: string;
      teacher: string;
      avg_rating: number;
      eval_count: number;
      latest_at: number;
    }>();

    return c.json({ items: rows.results || [] });
  } catch (e) {
    return c.json({ error: 'List courses failed' }, 500);
  }
});

// ─── Public: list evaluations ───
// 去掉审核后，所有评价默认可见。仍保留 status 字段做向前兼容（旧数据可能含 pending/rejected）。
app.get('/evaluations', async (c) => {
  const course = c.req.query('course');
  const teacher = c.req.query('teacher');
  const page = Math.max(1, parseInt(c.req.query('page') || '1', 10) || 1);
  const size = Math.min(100, Math.max(1, parseInt(c.req.query('size') || '20', 10) || 20));

  const where: string[] = [];
  const params: any[] = [];

  if (course) {
    where.push('course_name = ?');
    params.push(course);
  }
  if (teacher) {
    // 与 /courses 聚合的规范化保持一致：空 teacher 视为"教师未知"
    // 用 LIKE 模糊匹配，"教师未知" 也能正确命中空 teacher
    where.push("COALESCE(NULLIF(TRIM(teacher), ''), '教师未知') LIKE ?");
    params.push(`%${teacher}%`);
  }

  const w = where.length ? 'WHERE ' + where.join(' AND ') : '';

  const countRes = await c.env.DB.prepare(
    `SELECT COUNT(*) as cnt FROM evaluations ${w}`
  )
    .bind(...params)
    .first<{ cnt: number }>();
  const total = countRes?.cnt ?? 0;

  const rows = await c.env.DB.prepare(
    `SELECT * FROM evaluations ${w} ORDER BY created_at DESC LIMIT ? OFFSET ?`
  )
    .bind(...params, size, (page - 1) * size)
    .all<EvaluationRow>();

  const items = await withLiked(c, rows.results);
  return c.json({ items, total, page, size });
});

// ─── Public: evaluation detail ───
app.get('/evaluations/:id', async (c) => {
  const id = c.req.param('id');
  const row = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ?')
    .bind(id)
    .first<EvaluationRow>();

  if (!row) {
    return c.json({ error: 'Evaluation not found' }, 404);
  }

  const uh = userHashOf(c);
  const liked = uh
    ? !!(await c.env.DB.prepare(
        'SELECT 1 FROM evaluation_likes WHERE user_hash = ? AND evaluation_id = ?'
      )
        .bind(uh, id)
        .first())
    : false;

  return c.json(rowToEval(row, liked, row.user_hash === uh));
});

// ─── Public: create evaluation ───
// 需要登录（X-User-Hash 非空）；封号用户禁止提交。
app.post('/evaluations', async (c) => {
  try {
    const uh = userHashOf(c);
    if (!uh) {
      return c.json({ error: 'X-User-Id header required (login required)' }, 401);
    }
    if (await isBanned(c.env.DB, uh)) {
      return c.json({ error: '账号已被封禁，无法提交评价' }, 403);
    }

    if (!rateLimit(`submit:${uh}`, 10, 60)) {
      return c.json({ error: '提交过于频繁，请稍后再试' }, 429);
    }

    const body = await c.req.json<{
      course_name?: string;
      teacher?: string;
      rating?: number;
      content?: string;
      anonymous?: boolean;
      author?: string;
    }>();

    const courseName = (body.course_name || '').trim();
    if (!courseName) {
      return c.json({ error: 'course_name is required' }, 400);
    }
    if (courseName.length > MAX_COURSE_LEN) {
      return c.json({ error: 'course_name 过长' }, 400);
    }

    const rating = Math.round(body.rating ?? 0);
    if (rating < 1 || rating > 5) {
      return c.json({ error: 'rating must be between 1 and 5' }, 400);
    }

    const content = (body.content || '').trim();
    if (!content) {
      return c.json({ error: 'content is required' }, 400);
    }
    if (content.length > MAX_CONTENT_LEN) {
      return c.json({ error: 'content 过长' }, 400);
    }

    const id = crypto.randomUUID();
    const ts = nowSec();
    const anonymous = body.anonymous ? 1 : 0;
    const authorRaw = (body.author || '').trim();
    if (authorRaw.length > MAX_AUTHOR_LEN) {
      return c.json({ error: 'author 过长' }, 400);
    }
    const author = anonymous ? '' : authorRaw;
    const deviceId = deviceIdOf(c);
    // 不再存储学号明文，仅保留 user_hash（来自 X-User-Hash 头）
    await c.env.DB.prepare(
      `INSERT INTO evaluations
        (id, course_name, teacher, rating, content, anonymous, author, user_no, user_hash, device_id, likes, status, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, '', ?, ?, 0, 'approved', ?, ?)`
    )
      .bind(id, courseName, (body.teacher || '').trim(), rating, content, anonymous, author, uh, deviceId, ts, ts)
      .run();

    const row = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ?').bind(id).first<EvaluationRow>();
    return c.json(rowToEval(row!, false, row!.user_hash === uh), 201);
  } catch (e) {
    return c.json({ error: 'Create failed' }, 500);
  }
});

// ─── Public: toggle like (interaction) ───
// 需要登录（X-User-Hash 非空）；封号用户禁止点赞。
app.post('/evaluations/:id/like', async (c) => {
  const id = c.req.param('id');
  const uh = userHashOf(c);
  if (!uh) {
    return c.json({ error: 'X-User-Id header required (login required)' }, 401);
  }
  if (await isBanned(c.env.DB, uh)) {
    return c.json({ error: '账号已被封禁，无法点赞' }, 403);
  }

  if (!rateLimit(`like:${uh}`, 30, 60)) {
    return c.json({ error: '操作过于频繁，请稍后再试' }, 429);
  }

  const row = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ?').bind(id).first<EvaluationRow>();
  if (!row) {
    return c.json({ error: 'Evaluation not found' }, 404);
  }

  const existing = await c.env.DB.prepare(
    'SELECT 1 FROM evaluation_likes WHERE user_hash = ? AND evaluation_id = ?'
  )
    .bind(uh, id)
    .first();

  let liked: boolean;
  if (existing) {
    await c.env.DB.prepare('DELETE FROM evaluation_likes WHERE user_hash = ? AND evaluation_id = ?')
      .bind(uh, id)
      .run();
    await c.env.DB.prepare('UPDATE evaluations SET likes = MAX(0, likes - 1) WHERE id = ?').bind(id).run();
    liked = false;
  } else {
    await c.env.DB.prepare('INSERT INTO evaluation_likes (user_hash, evaluation_id, created_at) VALUES (?, ?, ?)')
      .bind(uh, id, nowSec())
      .run();
    await c.env.DB.prepare('UPDATE evaluations SET likes = likes + 1 WHERE id = ?').bind(id).run();
    liked = true;
  }

  const updated = await c.env.DB.prepare('SELECT likes FROM evaluations WHERE id = ?').bind(id).first<{ likes: number }>();
  return c.json({ likes: updated?.likes ?? 0, liked });
});

// ─── Public: delete own evaluation ───
// 需要登录（X-User-Hash 非空）；封号用户禁止删除。
app.delete('/evaluations/:id', async (c) => {
  const id = c.req.param('id');
  const uh = userHashOf(c);
  if (!uh) {
    return c.json({ error: 'X-User-Id header required (login required)' }, 401);
  }
  if (await isBanned(c.env.DB, uh)) {
    return c.json({ error: '账号已被封禁，无法删除评价' }, 403);
  }

  const row = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ? AND user_hash = ?')
    .bind(id, uh)
    .first<EvaluationRow>();
  if (!row) {
    return c.json({ error: 'Evaluation not found or unauthorized' }, 404);
  }

  await c.env.DB.prepare('DELETE FROM evaluation_likes WHERE evaluation_id = ?').bind(id).run();
  await c.env.DB.prepare('DELETE FROM evaluations WHERE id = ?').bind(id).run();
  return c.json({ ok: true });
});

// ─── Public: update own evaluation ───
// 需登录（X-User-Hash 非空）；封号用户禁止修改；只能改本人提交的评价。
// 可改字段：teacher / rating / content / anonymous / author（course_name 不可改）。
app.patch('/evaluations/:id', async (c) => {
  try {
    const id = c.req.param('id');
    const uh = userHashOf(c);
    if (!uh) {
      return c.json({ error: 'X-User-Hash header required (login required)' }, 401);
    }
    if (await isBanned(c.env.DB, uh)) {
      return c.json({ error: '账号已被封禁，无法修改评价' }, 403);
    }

    const row = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ? AND user_hash = ?')
      .bind(id, uh)
      .first<EvaluationRow>();
    if (!row) {
      return c.json({ error: 'Evaluation not found or unauthorized' }, 404);
    }

    const body = await c.req.json<{
      teacher?: string;
      rating?: number;
      content?: string;
      anonymous?: boolean;
      author?: string;
    }>();

    const rating = body.rating !== undefined ? Math.round(body.rating) : row.rating;
    if (rating < 1 || rating > 5) {
      return c.json({ error: 'rating must be between 1 and 5' }, 400);
    }
    const content = body.content !== undefined ? (body.content).trim() : row.content;
    if (!content) {
      return c.json({ error: 'content is required' }, 400);
    }
    if (content.length > MAX_CONTENT_LEN) {
      return c.json({ error: 'content 过长' }, 400);
    }
    const anonymous = body.anonymous !== undefined ? (body.anonymous ? 1 : 0) : row.anonymous;
    const teacher = body.teacher !== undefined ? (body.teacher).trim() : row.teacher;
    const authorRaw = body.author !== undefined ? (body.author).trim() : row.author;
    if (authorRaw.length > MAX_AUTHOR_LEN) {
      return c.json({ error: 'author 过长' }, 400);
    }
    const author = anonymous ? '' : authorRaw;
    const ts = nowSec();

    await c.env.DB.prepare(
      `UPDATE evaluations
       SET teacher = ?, rating = ?, content = ?, anonymous = ?, author = ?, updated_at = ?
       WHERE id = ?`
    )
      .bind(teacher, rating, content, anonymous, author, ts, id)
      .run();

    const updated = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ?').bind(id).first<EvaluationRow>();
    return c.json(rowToEval(updated!, false, updated!.user_hash === uh));
  } catch (e) {
    return c.json({ error: 'Update failed' }, 500);
  }
});

// ─── Admin auth ───
const adminAuth = async (c: any, next: any) => {
  if (!(await checkAdmin(c))) {
    return c.json({ error: 'Unauthorized' }, 401);
  }
  await next();
};

// 管理员登录：校验管理密码后签发 HMAC 签名会话令牌（1 小时过期，本身不是主密钥）
app.post('/admin/login', async (c) => {
  if (!rateLimit(`login:${clientIp(c)}`, 5, 300)) {
    return c.json({ error: '尝试次数过多，请 5 分钟后再试' }, 429);
  }
  try {
    const body = await c.req.json<{ password?: string }>();
    if (!body.password || body.password !== c.env.ADMIN_PASSWORD) {
      return c.json({ error: 'Invalid password' }, 401);
    }
    const token = await signSession(c.env.ADMIN_TOKEN, {
      sub: 'admin',
      exp: Math.floor(Date.now() / 1000) + SESSION_TTL_SEC,
    });
    return c.json({ token, ok: true });
  } catch {
    return c.json({ error: 'Bad request' }, 400);
  }
});

// 管理员：列出全部评价（可按课程筛选）
app.get('/admin/evaluations', adminAuth, async (c) => {
  const course = c.req.query('course');
  const page = Math.max(1, parseInt(c.req.query('page') || '1', 10) || 1);
  const size = Math.min(100, Math.max(1, parseInt(c.req.query('size') || '20', 10) || 20));

  const where: string[] = [];
  const params: any[] = [];
  if (course) {
    where.push('course_name = ?');
    params.push(course);
  }
  const w = where.length ? 'WHERE ' + where.join(' AND ') : '';

  const countRes = await c.env.DB.prepare(`SELECT COUNT(*) as cnt FROM evaluations ${w}`)
    .bind(...params)
    .first<{ cnt: number }>();
  const total = countRes?.cnt ?? 0;

  const rows = await c.env.DB.prepare(
    `SELECT * FROM evaluations ${w} ORDER BY created_at DESC LIMIT ? OFFSET ?`
  )
    .bind(...params, size, (page - 1) * size)
    .all<EvaluationRow>();

  const items = await withLiked(c, rows.results, true);
  return c.json({ items, total, page, size });
});

// 管理员：评价详情
app.get('/admin/evaluations/:id', adminAuth, async (c) => {
  const id = c.req.param('id');
  const row = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ?').bind(id).first<EvaluationRow>();
  if (!row) {
    return c.json({ error: 'Not found' }, 404);
  }
  return c.json(rowToEval(row, false, true));
});

// 管理员：修改评价（内容 / 评分等，不再有审核状态字段）
app.patch('/admin/evaluations/:id', adminAuth, async (c) => {
  const id = c.req.param('id');
  const row = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ?').bind(id).first<EvaluationRow>();
  if (!row) {
    return c.json({ error: 'Not found' }, 404);
  }

  const body = await c.req.json<{
    content?: string;
    rating?: number;
    teacher?: string;
    anonymous?: boolean;
    course_name?: string;
    author?: string;
  }>();

  const sets: string[] = [];
  const params: any[] = [];

  if (body.content !== undefined) {
    sets.push('content = ?');
    params.push(body.content);
  }
  if (body.rating !== undefined) {
    sets.push('rating = ?');
    params.push(Math.max(1, Math.min(5, Math.round(body.rating))));
  }
  if (body.teacher !== undefined) {
    sets.push('teacher = ?');
    params.push(body.teacher);
  }
  if (body.course_name !== undefined) {
    sets.push('course_name = ?');
    params.push(body.course_name);
  }
  if (body.anonymous !== undefined) {
    sets.push('anonymous = ?');
    params.push(body.anonymous ? 1 : 0);
    if (body.anonymous) {
      sets.push('author = ?');
      params.push('');
    } else if (body.author !== undefined) {
      sets.push('author = ?');
      params.push(body.author);
    }
  } else if (body.author !== undefined) {
    sets.push('author = ?');
    params.push(body.author);
  }

  if (sets.length === 0) {
    return c.json(rowToEval(row, false, true));
  }

  sets.push('updated_at = ?');
  params.push(nowSec());
  params.push(id);

  await c.env.DB.prepare(`UPDATE evaluations SET ${sets.join(', ')} WHERE id = ?`)
    .bind(...params)
    .run();

  const updated = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ?').bind(id).first<EvaluationRow>();
  return c.json(rowToEval(updated!, false, true));
});

// 管理员：删除任意评价
app.delete('/admin/evaluations/:id', adminAuth, async (c) => {
  const id = c.req.param('id');
  await c.env.DB.prepare('DELETE FROM evaluation_likes WHERE evaluation_id = ?').bind(id).run();
  await c.env.DB.prepare('DELETE FROM evaluations WHERE id = ?').bind(id).run();
  return c.json({ ok: true });
});

// 管理员：列出封号用户
app.get('/admin/bans', adminAuth, async (c) => {
  const rows = await c.env.DB.prepare(
    'SELECT * FROM banned_users ORDER BY banned_at DESC LIMIT 500'
  ).all<{ user_hash: string; reason: string; banned_at: number }>();
  return c.json({ items: rows.results });
});

// 管理员：封号（按 user_hash）
app.post('/admin/bans', adminAuth, async (c) => {
  try {
    const body = await c.req.json<{ user_hash?: string; reason?: string }>();
    const uh = (body.user_hash || '').trim();
    if (!uh) {
      return c.json({ error: 'user_hash is required' }, 400);
    }
    const reason = (body.reason || '').trim();
    await c.env.DB.prepare(
      `INSERT INTO banned_users (user_hash, reason, banned_at) VALUES (?, ?, ?)
       ON CONFLICT(user_hash) DO UPDATE SET reason = excluded.reason, banned_at = excluded.banned_at`
    )
      .bind(uh, reason, nowSec())
      .run();
    return c.json({ ok: true });
  } catch (e) {
    return c.json({ error: 'Ban failed' }, 500);
  }
});

// 管理员：解封（按 user_hash）
app.delete('/admin/bans/:user_hash', adminAuth, async (c) => {
  const uh = c.req.param('user_hash');
  await c.env.DB.prepare('DELETE FROM banned_users WHERE user_hash = ?').bind(uh).run();
  return c.json({ ok: true });
});

// ─── Admin Web UI（根地址即为管理后台入口）───
app.get('/', (c) => c.html(getAdminHTML()));

function getAdminHTML(): string {
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Chrnova 课程评价管理</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f5f5f5; color: #333; }
    .container { max-width: 960px; margin: 0 auto; padding: 24px 16px; }
    h1 { font-size: 22px; font-weight: 600; margin-bottom: 20px; }

    .login-box { background: #fff; border-radius: 12px; padding: 24px; max-width: 400px; margin: 80px auto; box-shadow: 0 1px 3px rgba(0,0,0,.1); }
    .login-box h1 { text-align: center; margin-bottom: 24px; }
    .form-group { margin-bottom: 14px; }
    .form-group label { display: block; font-size: 13px; font-weight: 500; margin-bottom: 6px; color: #555; }
    .form-group input, .form-group select, .form-group textarea { width: 100%; padding: 10px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; }
    .form-group input:focus, .form-group select:focus, .form-group textarea:focus { outline: none; border-color: #0066ff; }
    .form-group textarea { min-height: 80px; resize: vertical; }

    button { padding: 10px 20px; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; transition: background .2s; }
    .btn-primary { background: #0066ff; color: #fff; }
    .btn-primary:hover { background: #0052cc; }
    .btn-success { background: #1a7a1a; color: #fff; }
    .btn-success:hover { background: #146614; }
    .btn-danger { background: #ff3b30; color: #fff; }
    .btn-danger:hover { background: #d32f2f; }
    .btn-ghost { background: transparent; color: #0066ff; }
    .btn-ghost:hover { background: #f0f0f0; }
    .btn-sm { padding: 6px 12px; font-size: 12px; }
    button:disabled { opacity: .5; cursor: not-allowed; }

    .tabs { display: flex; gap: 4px; margin-bottom: 16px; border-bottom: 1px solid #e5e5e5; }
    .tab { padding: 10px 16px; cursor: pointer; border: none; background: transparent; font-size: 14px; color: #666; border-bottom: 2px solid transparent; }
    .tab.active { color: #0066ff; border-bottom-color: #0066ff; font-weight: 600; }

    .toolbar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; flex-wrap: wrap; }
    .toolbar input[type="text"] { flex: 1; min-width: 160px; padding: 9px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; }
    .toolbar select { padding: 9px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; background: #fff; }

    .stat-bar { display: flex; gap: 16px; margin-bottom: 20px; }
    .stat { background: #fff; border-radius: 10px; padding: 16px 20px; box-shadow: 0 1px 2px rgba(0,0,0,.06); flex: 1; text-align: center; }
    .stat-num { font-size: 24px; font-weight: 700; color: #0066ff; }
    .stat-label { font-size: 12px; color: #888; margin-top: 4px; }

    .eval-list, .ban-list { display: flex; flex-direction: column; gap: 8px; }
    .eval-card, .ban-card { background: #fff; border-radius: 10px; padding: 14px 16px; box-shadow: 0 1px 2px rgba(0,0,0,.06); }
    .eval-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
    .eval-course { font-size: 15px; font-weight: 600; }
    .eval-meta { font-size: 12px; color: #888; margin-top: 4px; display: flex; gap: 12px; flex-wrap: wrap; }
    .eval-content { font-size: 14px; color: #444; margin-top: 8px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
    .eval-actions { display: flex; gap: 6px; margin-top: 10px; flex-wrap: wrap; }
    .ban-hash { font-family: monospace; font-size: 13px; color: #444; word-break: break-all; }
    .ban-reason { font-size: 12px; color: #888; margin-top: 4px; }
    .ban-time { font-size: 12px; color: #aaa; margin-top: 2px; }

    .stars { color: #ff9500; letter-spacing: 2px; }

    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
    .modal { background: #fff; border-radius: 14px; padding: 24px; width: 92%; max-width: 520px; box-shadow: 0 8px 32px rgba(0,0,0,.2); max-height: 88vh; overflow-y: auto; }
    .modal h2 { font-size: 18px; margin-bottom: 16px; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 20px; }

    .toast { position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); padding: 12px 24px; border-radius: 10px; font-size: 14px; z-index: 200; animation: fadeInUp .3s; }
    .toast-success { background: #1a7a1a; color: #fff; }
    .toast-error { background: #cc0000; color: #fff; }
    @keyframes fadeInUp { from { opacity: 0; transform: translate(-50%, 10px); } to { opacity: 1; transform: translate(-50%, 0); } }

    .empty { text-align: center; padding: 60px 0; color: #999; }
    .hidden { display: none !important; }
  </style>
</head>
<body>
  <div id="loginView" class="login-box">
    <h1>Chrnova 课程评价管理</h1>
    <div class="form-group">
      <label>管理密码</label>
      <input type="password" id="passwordInput" placeholder="输入管理员密码">
    </div>
    <div style="text-align:center"><button class="btn-primary" onclick="login()">登录</button></div>
  </div>

  <div id="mainView" class="container hidden">
    <h1>Chrnova 课程评价管理</h1>

    <div class="tabs">
      <button class="tab active" id="tabEvals" onclick="switchTab('evals')">评价管理</button>
      <button class="tab" id="tabBans" onclick="switchTab('bans')">封号管理</button>
    </div>

    <!-- 评价管理 -->
    <div id="evalsPanel">
      <div class="stat-bar">
        <div class="stat"><div class="stat-num" id="totalCount">-</div><div class="stat-label">评价总数</div></div>
        <div class="stat"><div class="stat-num" id="totalLikes">-</div><div class="stat-label">总点赞</div></div>
      </div>

      <div class="toolbar">
        <input type="text" id="searchInput" placeholder="搜索课程 / 内容 / 作者..." oninput="debounceSearch()">
        <button class="btn-ghost btn-sm" onclick="loadEvaluations()">刷新</button>
      </div>

      <div id="evalList" class="eval-list"></div>
      <div id="emptyState" class="empty hidden">没有评价</div>
    </div>

    <!-- 封号管理 -->
    <div id="bansPanel" class="hidden">
      <div class="toolbar">
        <input type="text" id="banHashInput" placeholder="user_hash (学号 SHA-256)">
        <input type="text" id="banReasonInput" placeholder="封号原因（可选）">
        <button class="btn-danger btn-sm" onclick="banUser()">封号</button>
        <button class="btn-ghost btn-sm" onclick="loadBans()">刷新</button>
      </div>
      <div id="banList" class="ban-list"></div>
      <div id="banEmptyState" class="empty hidden">没有封号记录</div>
    </div>
  </div>

  <!-- Detail / Edit Modal -->
  <div id="detailModal" class="modal-overlay hidden" onclick="if(event.target===this)this.classList.add('hidden')">
    <div class="modal">
      <h2 id="detailTitle">-</h2>
      <div id="detailBody" style="font-size:14px; line-height:1.8; color:#555;"></div>
      <div class="modal-actions">
        <button class="btn-ghost" onclick="closeModal('detailModal')">关闭</button>
        <button class="btn-danger" id="detailDeleteBtn" onclick="confirmDelete()">删除</button>
        <button class="btn-danger" id="detailBanBtn" onclick="confirmBan()">封号作者</button>
      </div>
    </div>
  </div>

  <script>
    let token = '';
    let allEvals = [];
    let currentId = '';
    let currentEval = null;
    let searchTimer = null;
    let currentTab = 'evals';

    function login() {
      const password = document.getElementById('passwordInput').value;
      if (!password) return;
      fetch('/admin/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password })
      })
        .then(async r => {
          if (!r.ok) throw new Error((await r.json()).error || '登录失败');
          return r.json();
        })
        .then(data => {
          token = data.token;
          document.getElementById('loginView').classList.add('hidden');
          document.getElementById('mainView').classList.remove('hidden');
          loadEvaluations();
        })
        .catch(e => alert(e.message));
    }

    async function apiFetch(url, opts = {}) {
      const res = await fetch(url, {
        ...opts,
        headers: { 'Authorization': 'Bearer ' + token, ...(opts.headers || {}) }
      });
      if (res.status === 401) { alert('令牌失效，请重新登录'); throw new Error('Unauthorized'); }
      if (!res.ok) {
        const t = await res.text();
        try { const j = JSON.parse(t); throw new Error(j.error || t); } catch { throw new Error(t); }
      }
      return res;
    }

    function switchTab(tab) {
      currentTab = tab;
      document.getElementById('tabEvals').classList.toggle('active', tab === 'evals');
      document.getElementById('tabBans').classList.toggle('active', tab === 'bans');
      document.getElementById('evalsPanel').classList.toggle('hidden', tab !== 'evals');
      document.getElementById('bansPanel').classList.toggle('hidden', tab !== 'bans');
      if (tab === 'bans') loadBans();
    }

    async function loadEvaluations() {
      try {
        const res = await apiFetch('/admin/evaluations');
        const data = await res.json();
        allEvals = data.items || [];
        renderEvals();
        updateStats();
      } catch (e) { console.error(e); }
    }

    function renderEvals() {
      const q = document.getElementById('searchInput').value.trim().toLowerCase();
      const list = document.getElementById('evalList');
      const empty = document.getElementById('emptyState');
      const filtered = q
        ? allEvals.filter(e =>
            (e.course_name || '').toLowerCase().includes(q) ||
            (e.content || '').toLowerCase().includes(q) ||
            (e.author || '').toLowerCase().includes(q) ||
            (e.user_no || '').toLowerCase().includes(q))
        : allEvals;
      if (!filtered.length) { list.innerHTML = ''; empty.classList.remove('hidden'); return; }
      empty.classList.add('hidden');
      list.innerHTML = '';
      filtered.forEach(e => {
        const card = document.createElement('div');
        card.className = 'eval-card';
        const stars = '★'.repeat(e.rating) + '☆'.repeat(5 - e.rating);
        const anon = e.anonymous ? '匿名' : (e.author || '未署名');
        card.innerHTML =
          '<div class="eval-head">' +
            '<div>' +
              '<div class="eval-course">' + esc(e.course_name) + '</div>' +
              '<div class="eval-meta">' +
                '<span class="stars">' + stars + '</span>' +
                '<span>' + esc(e.teacher || '教师未知') + '</span>' +
                '<span>' + anon + '</span>' +
                '<span>👍 ' + (e.likes || 0) + '</span>' +
              '</div>' +
            '</div>' +
          '</div>' +
          (e.content ? '<div class="eval-content">' + esc(e.content) + '</div>' : '') +
          '<div class="eval-actions">' +
            '<button class="btn-primary btn-sm">查看 / 编辑</button>' +
          '</div>';
        card.querySelector('.btn-primary').addEventListener('click', function () { showDetail(e.id); });
        list.appendChild(card);
      });
    }

    function updateStats() {
      document.getElementById('totalCount').textContent = allEvals.length;
      document.getElementById('totalLikes').textContent = allEvals.reduce((s, e) => s + (e.likes || 0), 0);
    }

    function debounceSearch() { clearTimeout(searchTimer); searchTimer = setTimeout(loadEvaluations, 300); }

    async function showDetail(id) {
      try {
        const res = await apiFetch('/admin/evaluations/' + id);
        const e = await res.json();
        currentId = id;
        currentEval = e;
        document.getElementById('detailTitle').textContent = e.course_name;
        const stars = '★'.repeat(e.rating) + '☆'.repeat(5 - e.rating);
        const anon = e.anonymous ? '匿名' : (e.author || '未署名');
        document.getElementById('detailBody').innerHTML =
          '<b>课程:</b> ' + esc(e.course_name) + '<br>' +
          '<b>教师:</b> ' + esc(e.teacher || '未知') + '<br>' +
          '<b>评分:</b> <span class="stars">' + stars + '</span><br>' +
          '<b>作者:</b> ' + esc(anon) + '<br>' +
          '<b>用户标识:</b> <span style="font-family:monospace;word-break:break-all">' + esc(e.user_hash || '-') + '</span><br>' +
          '<b>点赞:</b> ' + (e.likes || 0) + '<br>' +
          '<b>提交时间:</b> ' + new Date(e.created_at * 1000).toLocaleString('zh-CN') + '<br>' +
          '<b>内容:</b><br>' + esc(e.content);
        const banBtn = document.getElementById('detailBanBtn');
        // 仅当存在 user_hash 时显示封号按钮
        banBtn.style.display = e.user_hash ? '' : 'none';
        document.getElementById('detailModal').classList.remove('hidden');
      } catch (e) { toast('加载失败', false); }
    }

    function confirmDelete() {
      if (!confirm('确定删除该评价？此操作不可撤销。')) return;
      apiFetch('/admin/evaluations/' + currentId, { method: 'DELETE' })
        .then(() => { closeModal('detailModal'); toast('已删除', true); loadEvaluations(); })
        .catch(e => toast('删除失败: ' + e.message, false));
    }

    function confirmBan() {
      if (!currentEval) return;
      const hash = (currentEval.user_hash || '').trim();
      if (!hash) { toast('该评价无用户标识，无法封号', false); return; }
      if (!confirm('确定封号该作者？\\n用户标识：' + hash + '\\n封号后该用户无法提交评价、点赞、删除。')) return;
      const reason = prompt('封号原因（可选）：') || '';
      apiFetch('/admin/bans', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ user_hash: hash, reason: reason.trim() })
      })
        .then(() => { toast('已封号', true); loadBans(); })
        .catch(e => toast('封号失败: ' + e.message, false));
    }

    async function loadBans() {
      try {
        const res = await apiFetch('/admin/bans');
        const data = await res.json();
        const items = data.items || [];
        const list = document.getElementById('banList');
        const empty = document.getElementById('banEmptyState');
        if (!items.length) { list.innerHTML = ''; empty.classList.remove('hidden'); return; }
        empty.classList.add('hidden');
        list.innerHTML = '';
        items.forEach(b => {
          const card = document.createElement('div');
          card.className = 'ban-card';
          card.innerHTML =
            '<div class="ban-hash">' + esc(b.user_hash) + '</div>' +
            (b.reason ? '<div class="ban-reason">原因: ' + esc(b.reason) + '</div>' : '') +
            '<div class="ban-time">封号时间: ' + new Date(b.banned_at * 1000).toLocaleString('zh-CN') + '</div>' +
            '<div class="eval-actions"><button class="btn-ghost btn-sm">解封</button></div>';
          card.querySelector('button').addEventListener('click', function () {
            if (!confirm('确定解封该用户？')) return;
            apiFetch('/admin/bans/' + encodeURIComponent(b.user_hash), { method: 'DELETE' })
              .then(() => { toast('已解封', true); loadBans(); })
              .catch(e => toast('解封失败: ' + e.message, false));
          });
          list.appendChild(card);
        });
      } catch (e) { console.error(e); }
    }

    function banUser() {
      const hash = document.getElementById('banHashInput').value.trim();
      const reason = document.getElementById('banReasonInput').value.trim();
      if (!hash) { alert('请输入 user_hash'); return; }
      apiFetch('/admin/bans', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ user_hash: hash, reason: reason })
      })
        .then(() => {
          document.getElementById('banHashInput').value = '';
          document.getElementById('banReasonInput').value = '';
          toast('已封号', true);
          loadBans();
        })
        .catch(e => toast('封号失败: ' + e.message, false));
    }

    function closeModal(id) { document.getElementById(id).classList.add('hidden'); }

    function toast(msg, ok) {
      const el = document.createElement('div');
      el.className = 'toast ' + (ok ? 'toast-success' : 'toast-error');
      el.textContent = msg;
      document.body.appendChild(el);
      setTimeout(() => el.remove(), 2500);
    }

    function esc(s) { const d = document.createElement('div'); d.textContent = s || ''; return d.innerHTML; }

    document.getElementById('passwordInput').addEventListener('keypress', e => { if (e.key === 'Enter') login(); });
  </script>
</body>
</html>`;
}

export default app;
