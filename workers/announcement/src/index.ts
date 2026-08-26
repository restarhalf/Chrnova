import { Hono } from 'hono';
import { cors } from 'hono/cors';

interface Env {
  DB: D1Database;
  ADMIN_TOKEN: string;
  IMG_BB_API_KEY: string;
}

interface Announcement {
  id: string;
  title: string;
  content: string;
  priority: number;
  pinned: number;
  status: string;
  created_at: number;
  updated_at: number;
}

interface AdConfig {
  id: number;
  image_url: string | null;
  target_url: string | null;
  announcement_id: string | null;
  enabled: number;
  updated_at: number;
}

/** 宽松 URL 校验：仅要求 http(s):// 开头，避免把明显非链接串入库 */
const URL_RE = /^https?:\/\/.+/i;

/** 字段上限：与客户端模型对应，防止超长内容拖垮列表与详情页 */
const MAX_TITLE_LEN = 200;
const MAX_CONTENT_LEN = 20000;

/** 恒定时间比较，避免令牌逐字节爆破 */
function timingSafeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

/**
 * 校验管理令牌。
 * 关键：`!expected` 守卫——若 ADMIN_TOKEN 未通过 `wrangler secret put` 配置，
 * env 值为 undefined，而未带 Authorization 头时 token 同样是 undefined，
 * 直接用 `!==` 比较会让两者相等从而放行。必须显式拒绝未配置的情况。
 */
function isAdmin(c: any): boolean {
  const expected = c.env.ADMIN_TOKEN;
  if (!expected) return false;
  const token = c.req.header('Authorization')?.replace(/^Bearer\s+/i, '')?.trim();
  if (!token) return false;
  return timingSafeEqual(token, expected);
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

/** 校验新增/编辑入参，返回错误信息（null 表示合法） */
function validateInput(body: any): string | null {
  if (typeof body.title !== 'string' || body.title.trim() === '') {
    return '标题不能为空';
  }
  if (body.title.trim().length > MAX_TITLE_LEN) {
    return `标题过长（上限 ${MAX_TITLE_LEN} 字）`;
  }
  if (typeof body.content !== 'string' || body.content.length > MAX_CONTENT_LEN) {
    return `内容过长（上限 ${MAX_CONTENT_LEN} 字）`;
  }
  if (body.priority !== undefined && body.priority !== 0 && body.priority !== 1) {
    return 'priority 仅支持 0（普通）或 1（重要）';
  }
  if (body.pinned !== undefined && body.pinned !== 0 && body.pinned !== 1) {
    return 'pinned 仅支持 0 或 1';
  }
  if (body.status !== undefined && body.status !== 'published' && body.status !== 'draft' && body.status !== 'ad') {
    return 'status 仅支持 published、draft 或 ad';
  }
  return null;
}

const app = new Hono<{ Bindings: Env }>();

// CORS 仅对公开只读接口开放，且限定为自有前端域；管理接口同源（管理后台同源部署），无需 CORS。
const CORS_ORIGINS = [
  'https://chrnova.restarhalf.dpdns.org',
  'http://localhost:5173',
  'http://127.0.0.1:5173',
];
app.use('/announcements', cors({ origin: CORS_ORIGINS }));
app.use('/announcements/*', cors({ origin: CORS_ORIGINS }));
app.use('/ad', cors({ origin: CORS_ORIGINS }));

// ─── 公开只读接口（仅返回已发布） ───

// Get published announcements
app.get('/announcements', async (c) => {
  const limitParam = c.req.query('limit');
  const limit = Math.min(Math.max(parseInt(limitParam || '50', 10) || 50, 1), 100);

  const result = await c.env.DB.prepare(
    'SELECT * FROM announcements WHERE status = \'published\' ORDER BY pinned DESC, created_at DESC LIMIT ?'
  ).bind(limit).all<Announcement>();

  return c.json(result.results.map((a) => toWireAnnouncement(a, new URL(c.req.url).origin)));
});

// Get published announcement detail
app.get('/announcements/:id', async (c) => {
  const id = c.req.param('id');
  const result = await c.env.DB.prepare(
    'SELECT * FROM announcements WHERE id = ? AND status IN (\'published\', \'ad\')'
  ).bind(id).first<Announcement>();

  if (!result) {
    return c.json({ error: 'Announcement not found' }, 404);
  }
  return c.json(toWireAnnouncement(result, new URL(c.req.url).origin));
});

// 运行时自愈：确保 ad_config 表存在（与 src/schema.sql 结构一致）。
// 即使部署后未手动执行迁移，接口也能自动建表，避免 "no such table" 导致 500。
async function ensureAdTable(c: any): Promise<void> {
  await c.env.DB.prepare(
    `CREATE TABLE IF NOT EXISTS ad_config (
      id INTEGER PRIMARY KEY CHECK (id = 1),
      image_url TEXT,
      target_url TEXT,
      announcement_id TEXT,
      enabled INTEGER NOT NULL DEFAULT 0,
      updated_at INTEGER NOT NULL
    )`
  ).run();
}

// 公告列表页顶部广告位：单例配置（ad_config.id=1）
app.get('/ad', async (c) => {
  await ensureAdTable(c);
  const row = await c.env.DB.prepare(
    'SELECT * FROM ad_config WHERE id = 1'
  ).first<AdConfig>();
  // 未配置 / 未启用 / 三个字段全空 → 返回 null，客户端据此隐藏广告位
  if (!row || !row.enabled) return c.json(null);
  if (!row.image_url && !row.target_url && !row.announcement_id) return c.json(null);
  const origin = new URL(c.req.url).origin;
  return c.json({
    imageUrl: row.image_url ? maybeProxyImage(row.image_url, origin) : null,
    targetUrl: row.target_url || null,
    announcementId: row.announcement_id || null,
  });
});

// ─── 管理接口（Bearer ADMIN_TOKEN） ───

const adminAuth = async (c: any, next: any) => {
  if (!isAdmin(c)) {
    return c.json({ error: 'Unauthorized' }, 401);
  }
  await next();
};

// Admin: list all announcements (含草稿；管理端需要 status 字段，返回原始行)
// 广告位配置（单例 id=1）随公告列表一起下发，不再单独提供 /admin/api/ad 读接口
app.get('/admin/api/announcements', adminAuth, async (c) => {
  const result = await c.env.DB.prepare(
    'SELECT * FROM announcements ORDER BY pinned DESC, created_at DESC'
  ).all<Announcement>();
  await ensureAdTable(c);
  const adRow = await c.env.DB.prepare(
    'SELECT * FROM ad_config WHERE id = 1'
  ).first<AdConfig>();
  const adConfig = adRow
    ? {
        imageUrl: adRow.image_url || '',
        targetUrl: adRow.target_url || '',
        announcementId: adRow.announcement_id || '',
        enabled: !!adRow.enabled,
      }
    : { imageUrl: '', targetUrl: '', announcementId: '', enabled: false };
  return c.json({ items: result.results, adConfig });
});

// Admin: get detail
app.get('/admin/api/announcements/:id', adminAuth, async (c) => {
  const id = c.req.param('id');
  const result = await c.env.DB.prepare(
    'SELECT * FROM announcements WHERE id = ?'
  ).bind(id).first<Announcement>();
  if (!result) return c.json({ error: 'Not found' }, 404);
  return c.json(result);
});

// Admin: create
app.post('/admin/api/announcements', adminAuth, async (c) => {
  if (!rateLimit(`admin:${clientIp(c)}`, 30, 60)) {
    return c.json({ error: '操作过于频繁，请稍后再试' }, 429);
  }

  let body: any;
  try {
    body = await c.req.json();
  } catch (_) {
    return c.json({ error: 'Invalid JSON body' }, 400);
  }

  const invalid = validateInput(body);
  if (invalid) return c.json({ error: invalid }, 400);

  const id = crypto.randomUUID();
  const now = Math.floor(Date.now() / 1000);

  await c.env.DB.prepare(
    `INSERT INTO announcements (id, title, content, priority, pinned, status, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
  )
    .bind(
      id,
      body.title.trim(),
      body.content ?? '',
      body.priority ?? 0,
      body.pinned ?? 0,
      body.status ?? 'published',
      now,
      now,
    )
    .run();

  const created = await c.env.DB.prepare(
    'SELECT * FROM announcements WHERE id = ?'
  ).bind(id).first<Announcement>();
  return c.json(created, 201);
});

// Admin: update
app.patch('/admin/api/announcements/:id', adminAuth, async (c) => {
  if (!rateLimit(`admin:${clientIp(c)}`, 30, 60)) {
    return c.json({ error: '操作过于频繁，请稍后再试' }, 429);
  }

  const id = c.req.param('id');
  const existing = await c.env.DB.prepare(
    'SELECT * FROM announcements WHERE id = ?'
  ).bind(id).first<Announcement>();
  if (!existing) return c.json({ error: 'Not found' }, 404);

  let body: any;
  try {
    body = await c.req.json();
  } catch (_) {
    return c.json({ error: 'Invalid JSON body' }, 400);
  }

  const invalid = validateInput({
    title: body.title ?? existing.title,
    content: body.content ?? existing.content,
    priority: body.priority ?? existing.priority,
    pinned: body.pinned ?? existing.pinned,
    status: body.status ?? existing.status,
  });
  if (invalid) return c.json({ error: invalid }, 400);

  const now = Math.floor(Date.now() / 1000);
  await c.env.DB.prepare(
    `UPDATE announcements SET title = ?, content = ?, priority = ?, pinned = ?, status = ?, updated_at = ?
     WHERE id = ?`
  )
    .bind(
      (body.title ?? existing.title).trim(),
      body.content ?? existing.content,
      body.priority ?? existing.priority,
      body.pinned ?? existing.pinned,
      body.status ?? existing.status,
      now,
      id,
    )
    .run();

  const updated = await c.env.DB.prepare(
    'SELECT * FROM announcements WHERE id = ?'
  ).bind(id).first<Announcement>();
  return c.json(updated);
});

// Admin: delete
app.delete('/admin/api/announcements/:id', adminAuth, async (c) => {
  if (!rateLimit(`admin:${clientIp(c)}`, 20, 60)) {
    return c.json({ error: '操作过于频繁，请稍后再试' }, 429);
  }

  const id = c.req.param('id');
  const result = await c.env.DB.prepare(
    'SELECT * FROM announcements WHERE id = ?'
  ).bind(id).first<Announcement>();
  if (!result) return c.json({ error: 'Not found' }, 404);

  await c.env.DB.prepare('DELETE FROM announcements WHERE id = ?').bind(id).run();
  return c.json({ ok: true });
});

// Admin: upsert ad config（单例，id 固定为 1；读取已并入 GET /admin/api/announcements）
app.put('/admin/api/ad', adminAuth, async (c) => {
  await ensureAdTable(c);
  if (!rateLimit(`admin:${clientIp(c)}`, 30, 60)) {
    return c.json({ error: '操作过于频繁，请稍后再试' }, 429);
  }
  let body: any;
  try {
    body = await c.req.json();
  } catch (_) {
    return c.json({ error: 'Invalid JSON body' }, 400);
  }

  const imageUrl = typeof body.imageUrl === 'string' ? body.imageUrl.trim() : '';
  const targetUrl = typeof body.targetUrl === 'string' ? body.targetUrl.trim() : '';
  const announcementId = typeof body.announcementId === 'string' ? body.announcementId.trim() : '';
  const enabled = body.enabled === true || body.enabled === 'true' || body.enabled === 1;

  if (imageUrl && !URL_RE.test(imageUrl)) {
    return c.json({ error: '横幅图需以 http(s):// 开头' }, 400);
  }
  if (targetUrl && !URL_RE.test(targetUrl)) {
    return c.json({ error: '跳转链接需以 http(s):// 开头' }, 400);
  }
  if (!imageUrl && !targetUrl && !announcementId) {
    return c.json({ error: '至少填写横幅图或跳转目标' }, 400);
  }

  const now = Math.floor(Date.now() / 1000);
  await c.env.DB.prepare(
    `INSERT INTO ad_config (id, image_url, target_url, announcement_id, enabled, updated_at)
     VALUES (1, ?, ?, ?, ?, ?)
     ON CONFLICT(id) DO UPDATE SET
       image_url=excluded.image_url,
       target_url=excluded.target_url,
       announcement_id=excluded.announcement_id,
       enabled=excluded.enabled,
       updated_at=excluded.updated_at`
  )
    .bind(
      imageUrl || null,
      targetUrl || null,
      announcementId || null,
      enabled ? 1 : 0,
      now,
    )
    .run();

  return c.json({ ok: true });
});

// ─── 图片上传（代理 imgbb，API key 仅存于 Worker secret，不暴露给前端/客户端） ───

/** 单张图片上限：imgbb 免费 key 单图上限 32MB，这里保守限制 10MB */
const MAX_IMG_BYTES = 10 * 1024 * 1024;
const IMG_DATA_URI_RE = /^data:image\/(jpeg|png|webp|gif);base64,([A-Za-z0-9+/=]+)$/;

// Admin: upload image → imgbb，返回可直链访问的图片 URL
app.post('/admin/api/upload', adminAuth, async (c) => {
  // 与 ADMIN_TOKEN 相同思路：key 未配置时显式拒绝，避免误以为可用
  if (!c.env.IMG_BB_API_KEY) {
    return c.json({ error: '服务器未配置 IMG_BB_API_KEY' }, 500);
  }
  if (!rateLimit(`upload:${clientIp(c)}`, 20, 60)) {
    return c.json({ error: '上传过于频繁，请稍后再试' }, 429);
  }

  let body: any;
  try {
    body = await c.req.json();
  } catch (_) {
    return c.json({ error: 'Invalid JSON body' }, 400);
  }

  const image = typeof body.image === 'string' ? body.image.trim() : '';
  const m = image.match(IMG_DATA_URI_RE);
  if (!m) {
    return c.json({ error: '仅支持 jpeg/png/webp/gif 图片' }, 400);
  }
  // imgbb 的 image 参数要求纯 base64：带 `data:image/...;base64,` 前缀会被其校验
  // 拒绝（"Invalid base64 string."）。剥掉前缀，并按 base64 长度估算解码后字节数。
  const base64 = m[2].replace(/\s+/g, '');
  const approxBytes = Math.floor((base64.length * 3) / 4);
  if (approxBytes > MAX_IMG_BYTES) {
    return c.json({ error: '图片过大（上限 10MB）' }, 400);
  }

  const form = new URLSearchParams();
  form.set('key', c.env.IMG_BB_API_KEY);
  form.set('image', base64);

  let resp: Response;
  try {
    resp = await fetch('https://api.imgbb.com/1/upload', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: form.toString(),
      signal: AbortSignal.timeout(30_000),
    });
  } catch (_) {
    return c.json({ error: '图片服务暂时不可用，请稍后重试' }, 502);
  }

  const data: any = await resp.json().catch(() => null);
  if (!resp.ok || !data?.data?.url) {
    const msg = data?.error?.message || `图片上传失败（HTTP ${resp.status}）`;
    return c.json({ error: String(msg) }, 502);
  }
  return c.json({
    url: data.data.display_url || data.data.url,
    thumbUrl: data.data.thumb?.url || null,
    size: data.data.size || null,
  });
});

/**
 * 转换对外输出：剥离 status 管理字段，并把 DB 的 pinned 0/1 转为布尔，
 * 与客户端 @Serializable 模型（pinned: Boolean）一一对应。
 * 同时把正文中直连图床的地址改写为本 Worker 的 /img 反代——
 * imgbb 的 i.ibb.co 在大陆网络经常不可达，走自有域名过 Cloudflare 更稳。
 */
function toWireAnnouncement(
  a: Announcement,
  origin: string,
): Omit<Announcement, 'status' | 'pinned'> & { pinned: boolean } {
  const { status, pinned, ...rest } = a;
  return { ...rest, content: rewriteImageUrls(a.content, origin), pinned: !!pinned };
}

/** 允许反代的图床域名（含子域） */
const IMG_PROXY_HOSTS = ['ibb.co'];
const IMG_PROXY_URL_RE = new RegExp(
  `https?://([a-z0-9-]+\\.)*(${IMG_PROXY_HOSTS.join('|').replace(/\./g, '\\.')})/[^\\s)\\]]+`,
  'gi',
);

function rewriteImageUrls(content: string, origin: string): string {
  if (!content) return content;
  return content.replace(IMG_PROXY_URL_RE, (m) => `${origin}/img?url=${encodeURIComponent(m)}`);
}

/**
 * 单条图片 URL 的反代改写：ibb.co（含子域）走本 Worker 的 /img 反代，
 * 绕开 imgbb 大陆直连不可达；其余域名（如自有 CDN）原样返回，避免被 /img 的白名单拒绝。
 */
function maybeProxyImage(url: string, origin: string): string {
  try {
    const u = new URL(url);
    const host = u.hostname.toLowerCase();
    const proxied = u.protocol === 'https:' &&
      IMG_PROXY_HOSTS.some((h) => host === h || host.endsWith(`.${h}`));
    if (proxied) return `${origin}/img?url=${encodeURIComponent(url)}`;
  } catch {
    /* 非法 URL 原样返回，由上层校验拦截 */
  }
  return url;
}

// ─── 图片反代（公开）：客户端经自有域名拉取图床资源 ───

app.get('/img', async (c) => {
  const raw = c.req.query('url') || '';
  let target: URL;
  try {
    target = new URL(raw);
  } catch {
    return c.json({ error: 'Invalid url' }, 400);
  }
  const host = target.hostname.toLowerCase();
  const allowed = target.protocol === 'https:' &&
    IMG_PROXY_HOSTS.some((h) => host === h || host.endsWith(`.${h}`));
  if (!allowed) {
    return c.json({ error: 'Host not allowed' }, 400);
  }
  // 图片请求数远多于写接口，限流阈值放宽；进程内窗口仅防单实例滥用
  if (!rateLimit(`img:${clientIp(c)}`, 120, 60)) {
    return c.json({ error: '操作过于频繁，请稍后再试' }, 429);
  }

  let upstream: Response;
  try {
    upstream = await fetch(target.toString(), { signal: AbortSignal.timeout(20_000) });
  } catch (_) {
    return c.json({ error: '图片服务暂时不可用' }, 502);
  }
  if (!upstream.ok || !upstream.body) {
    return c.json({ error: `图片获取失败（HTTP ${upstream.status}）` }, 502);
  }
  const size = parseInt(upstream.headers.get('Content-Length') || '0', 10);
  if (size > MAX_IMG_BYTES) {
    return c.json({ error: '图片过大' }, 502);
  }

  const headers = new Headers();
  headers.set(
    'Content-Type',
    upstream.headers.get('Content-Type') || 'application/octet-stream',
  );
  // 图床直链内容不变，可长缓存，减轻 Worker 与图床压力
  headers.set('Cache-Control', 'public, max-age=31536000, immutable');
  return new Response(upstream.body, { status: 200, headers });
});

// ─── Admin Web UI ───

app.get('/', (c) => {
  return c.html(getAdminHTML());
});

function getAdminHTML(): string {
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Chrnova 公告管理</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f5f5f5; color: #333; }
    .container { max-width: 900px; margin: 0 auto; padding: 24px 16px; }
    h1 { font-size: 22px; font-weight: 600; margin-bottom: 20px; }

    .login-box { background: #fff; border-radius: 12px; padding: 24px; max-width: 400px; margin: 80px auto; box-shadow: 0 1px 3px rgba(0,0,0,.1); }
    .login-box h1 { text-align: center; margin-bottom: 24px; }

    .form-group { margin-bottom: 14px; }
    .form-group label { display: block; font-size: 13px; font-weight: 500; margin-bottom: 6px; color: #555; }
    .form-group input, .form-group select, .form-group textarea { width: 100%; padding: 10px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; }
    .form-group input:focus, .form-group select:focus, .form-group textarea:focus { outline: none; border-color: #0066ff; }
    .form-group textarea { min-height: 120px; resize: vertical; }
    .form-row { display: flex; gap: 12px; }
    .form-row .form-group { flex: 1; }

    button { padding: 10px 20px; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; transition: background .2s; }
    .btn-primary { background: #0066ff; color: #fff; }
    .btn-primary:hover { background: #0052cc; }
    .btn-danger { background: #ff3b30; color: #fff; }
    .btn-danger:hover { background: #d32f2f; }
    .btn-ghost { background: transparent; color: #0066ff; }
    .btn-ghost:hover { background: #f0f0f0; }
    .btn-sm { padding: 6px 12px; font-size: 12px; }
    button:disabled { opacity: .5; cursor: not-allowed; }

    .toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .stat-bar { display: flex; gap: 16px; margin-bottom: 20px; }
    .stat { background: #fff; border-radius: 10px; padding: 16px 20px; box-shadow: 0 1px 2px rgba(0,0,0,.06); flex: 1; text-align: center; }
    .stat-num { font-size: 24px; font-weight: 700; color: #0066ff; }
    .stat-label { font-size: 12px; color: #888; margin-top: 4px; }

    .list { display: flex; flex-direction: column; gap: 8px; }
    .card { background: #fff; border-radius: 10px; padding: 14px 16px; box-shadow: 0 1px 2px rgba(0,0,0,.06); display: flex; justify-content: space-between; align-items: center; gap: 12px; }
    .info { flex: 1; min-width: 0; }
    .title { font-size: 15px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .meta { font-size: 12px; color: #888; margin-top: 4px; display: flex; gap: 10px; flex-wrap: wrap; }
    .badge { padding: 2px 8px; border-radius: 6px; font-size: 11px; font-weight: 500; }
    .badge-pinned { background: #ffe8cc; color: #b45309; }
    .badge-important { background: #fee2e2; color: #b91c1c; }
    .badge-draft { background: #e5e7eb; color: #4b5563; }
    .badge-ad { background: #e0f2fe; color: #0369a1; }

    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
    .modal { background: #fff; border-radius: 14px; padding: 24px; width: 90%; max-width: 560px; box-shadow: 0 8px 32px rgba(0,0,0,.2); }
    .modal h2 { font-size: 18px; margin-bottom: 16px; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 20px; }
    .checkbox-line { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #555; }
    .checkbox-line input { width: auto; }

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
    <h1>Chrnova 公告管理</h1>
    <div class="form-group">
      <label>Access Token</label>
      <input type="password" id="tokenInput" placeholder="输入管理令牌">
    </div>
    <div style="text-align:center"><button class="btn-primary" onclick="login()">登录</button></div>
  </div>

  <div id="mainView" class="container hidden">
    <h1>Chrnova 公告管理</h1>
    <div class="stat-bar">
      <div class="stat"><div class="stat-num" id="totalCount">-</div><div class="stat-label">全部公告</div></div>
      <div class="stat"><div class="stat-num" id="publishedCount">-</div><div class="stat-label">已发布</div></div>
      <div class="stat"><div class="stat-num" id="pinnedCount">-</div><div class="stat-label">置顶</div></div>
    </div>
    <div class="toolbar">
      <span style="font-size:13px;color:#888;">管理公告内容，客户端首页与公告列表实时可见</span>
      <div style="display:flex;gap:8px;">
        <button class="btn-ghost" onclick="openAd()">广告位配置</button>
        <button class="btn-primary" onclick="openCreate()">+ 新建公告</button>
      </div>
    </div>
    <div id="list" class="list"></div>
    <div id="emptyState" class="empty hidden">还没有公告</div>
  </div>

  <div id="editModal" class="modal-overlay hidden" onclick="if(event.target===this)closeModal()">
    <div class="modal">
      <h2 id="modalTitle">新建公告</h2>
      <div class="form-group">
        <label>标题</label>
        <input type="text" id="editTitle" maxlength="200" placeholder="公告标题">
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>优先级</label>
          <select id="editPriority">
            <option value="0">普通</option>
            <option value="1">重要</option>
          </select>
        </div>
        <div class="form-group">
          <label>状态</label>
          <select id="editStatus">
            <option value="published">发布</option>
            <option value="draft">草稿</option>
            <option value="ad">广告位（不进列表）</option>
          </select>
        </div>
      </div>
      <div class="form-group checkbox-line">
        <input type="checkbox" id="editPinned"><label for="editPinned">置顶显示</label>
      </div>
      <div class="form-group">
        <label>内容</label>
        <textarea id="editContent" maxlength="20000" placeholder="公告正文"></textarea>
        <div style="display:flex;align-items:center;gap:8px;margin-top:8px;">
          <button type="button" class="btn-ghost btn-sm" id="uploadBtn" onclick="document.getElementById('imgInput').click()">上传图片</button>
          <input type="file" id="imgInput" accept="image/jpeg,image/png,image/webp,image/gif" style="display:none;">
          <span style="font-size:12px;color:#888;">支持 jpeg/png/webp/gif，≤10MB，插入后以图片形式展示在公告正文</span>
        </div>
      </div>
      <div class="modal-actions">
        <button class="btn-ghost" onclick="closeModal()">取消</button>
        <button class="btn-primary" id="saveBtn" onclick="save()">保存</button>
      </div>
    </div>
  </div>

  <div id="adModal" class="modal-overlay hidden" onclick="if(event.target===this)closeAd()">
    <div class="modal">
      <h2>广告位配置（公告列表页顶部横幅）</h2>
      <div class="form-group">
        <label>横幅图 URL</label>
        <input type="text" id="adImageUrl" placeholder="https://... 图片直链（ibb.co 链接请填 i.ibb.co 直链）">
      </div>
      <div class="form-group">
        <label>跳转链接（仅单个外链时点击直接跳转）</label>
        <input type="text" id="adTargetUrl" placeholder="https://... 留空则使用下方关联公告">
      </div>
      <div class="form-group">
        <label>关联公告（无外链时，点击走与公告详情一致的打开方式）</label>
        <select id="adAnnouncementId">
          <option value="">— 不关联（点击无反应）—</option>
        </select>
        <span style="font-size:12px;color:#888;">先建一条状态为「广告位（不进列表）」的公告，这里即可选择它</span>
      </div>
      <div class="form-group checkbox-line">
        <input type="checkbox" id="adEnabled"><label for="adEnabled">启用广告位</label>
      </div>
      <div class="modal-actions">
        <button class="btn-ghost" onclick="closeAd()">取消</button>
        <button class="btn-primary" id="adSaveBtn" onclick="saveAd()">保存</button>
      </div>
    </div>
  </div>

  <script>
    let token = '';
    let all = [];
    let adConfig = { imageUrl: '', targetUrl: '', announcementId: '', enabled: false };
    let editingId = null;

    function login() {
      token = document.getElementById('tokenInput').value;
      if (!token) return;
      apiFetch('/admin/api/announcements')
        .then(r => r.json())
        .then(data => {
          document.getElementById('loginView').classList.add('hidden');
          document.getElementById('mainView').classList.remove('hidden');
          all = data.items || [];
          adConfig = data.adConfig || { imageUrl: '', targetUrl: '', announcementId: '', enabled: false };
          render();
        })
        .catch(() => alert('登录失败'));
    }

    async function apiFetch(url, opts = {}) {
      const res = await fetch(url, {
        ...opts,
        headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json', ...(opts.headers || {}) }
      });
      if (res.status === 401) { alert('令牌失效'); throw new Error('Unauthorized'); }
      if (!res.ok) throw new Error((await res.json().catch(() => ({}))).error || '请求失败');
      return res;
    }

    function render() {
      const list = document.getElementById('list');
      const empty = document.getElementById('emptyState');
      if (!all.length) { list.innerHTML = ''; empty.classList.remove('hidden'); return; }
      empty.classList.add('hidden');
      list.innerHTML = '';
      all.forEach(a => {
        const date = new Date(a.created_at * 1000).toLocaleDateString('zh-CN');
        const badges = [];
        if (a.pinned) badges.push('<span class="badge badge-pinned">置顶</span>');
        if (a.priority) badges.push('<span class="badge badge-important">重要</span>');
        if (a.status === 'ad') badges.push('<span class="badge badge-ad">广告位</span>');
        else if (a.status !== 'published') badges.push('<span class="badge badge-draft">草稿</span>');
        const card = document.createElement('div');
        card.className = 'card';
        card.innerHTML =
          '<div class="info">' +
            '<div class="title">' + esc(a.title) + ' ' + badges.join(' ') + '</div>' +
            '<div class="meta"><span>' + date + '</span>' +
              '<span>' + (a.content || '').length + ' 字</span></div>' +
          '</div>' +
          '<div style="display:flex;gap:6px;flex-shrink:0;">' +
            '<button class="btn-ghost btn-sm" id="editBtn">编辑</button>' +
            '<button class="btn-danger btn-sm" id="delBtn">删除</button>' +
          '</div>';
        card.querySelector('#editBtn').addEventListener('click', () => openEdit(a));
        card.querySelector('#delBtn').addEventListener('click', () => confirmDelete(a));
        list.appendChild(card);
      });
      document.getElementById('totalCount').textContent = all.length;
      document.getElementById('publishedCount').textContent = all.filter(a => a.status === 'published').length;
      document.getElementById('pinnedCount').textContent = all.filter(a => a.pinned).length;
    }

    function openCreate() {
      editingId = null;
      document.getElementById('modalTitle').textContent = '新建公告';
      document.getElementById('editTitle').value = '';
      document.getElementById('editContent').value = '';
      document.getElementById('editPriority').value = '0';
      document.getElementById('editStatus').value = 'published';
      document.getElementById('editPinned').checked = false;
      document.getElementById('editModal').classList.remove('hidden');
    }

    function openEdit(a) {
      editingId = a.id;
      document.getElementById('modalTitle').textContent = '编辑公告';
      document.getElementById('editTitle').value = a.title;
      document.getElementById('editContent').value = a.content;
      document.getElementById('editPriority').value = String(a.priority);
      document.getElementById('editStatus').value = a.status;
      document.getElementById('editPinned').checked = !!a.pinned;
      document.getElementById('editModal').classList.remove('hidden');
    }

    function closeModal() { document.getElementById('editModal').classList.add('hidden'); }

    // ─── 广告位配置（随公告列表一起加载，无需单独请求） ───
    function openAd() {
      const ad = adConfig || { imageUrl: '', targetUrl: '', announcementId: '', enabled: false };
      const sel = document.getElementById('adAnnouncementId');
      sel.innerHTML = '<option value="">— 不关联（点击无反应）—</option>';
      all.forEach((a) => {
        const o = document.createElement('option');
        o.value = a.id;
        o.textContent = (a.status === 'ad' ? '[广告位] ' : '') + (a.title || a.id);
        sel.appendChild(o);
      });
      sel.value = ad.announcementId || '';
      document.getElementById('adImageUrl').value = ad.imageUrl || '';
      document.getElementById('adTargetUrl').value = ad.targetUrl || '';
      document.getElementById('adEnabled').checked = !!ad.enabled;
      document.getElementById('adModal').classList.remove('hidden');
    }

    function closeAd() { document.getElementById('adModal').classList.add('hidden'); }

    async function saveAd() {
      const payload = {
        imageUrl: document.getElementById('adImageUrl').value.trim(),
        targetUrl: document.getElementById('adTargetUrl').value.trim(),
        announcementId: document.getElementById('adAnnouncementId').value.trim(),
        enabled: document.getElementById('adEnabled').checked,
      };
      if (!payload.imageUrl && !payload.targetUrl && !payload.announcementId) {
        toast('至少填写横幅图或跳转目标', false); return;
      }
      const btn = document.getElementById('adSaveBtn');
      btn.disabled = true; btn.textContent = '保存中...';
      try {
        await apiFetch('/admin/api/ad', { method: 'PUT', body: JSON.stringify(payload) });
        adConfig = { ...payload };
        toast('广告位配置已保存', true);
        closeAd();
      } catch (e) { toast(e.message || '保存失败', false); }
      finally { btn.disabled = false; btn.textContent = '保存'; }
    }

    async function save() {
      const payload = {
        title: document.getElementById('editTitle').value.trim(),
        content: document.getElementById('editContent').value,
        priority: parseInt(document.getElementById('editPriority').value, 10),
        pinned: document.getElementById('editPinned').checked ? 1 : 0,
        status: document.getElementById('editStatus').value,
      };
      if (!payload.title) { toast('标题不能为空', false); return; }
      const btn = document.getElementById('saveBtn');
      btn.disabled = true; btn.textContent = '保存中...';
      try {
        if (editingId) {
          await apiFetch('/admin/api/announcements/' + editingId, { method: 'PATCH', body: JSON.stringify(payload) });
          toast('已更新', true);
        } else {
          await apiFetch('/admin/api/announcements', { method: 'POST', body: JSON.stringify(payload) });
          toast('已创建', true);
        }
        closeModal();
        const res = await apiFetch('/admin/api/announcements');
        const data = await res.json();
        all = data.items || [];
        adConfig = data.adConfig || adConfig;
        render();
      } catch (e) { toast(e.message || '保存失败', false); }
      finally { btn.disabled = false; btn.textContent = '保存'; }
    }

    function confirmDelete(a) {
      if (!confirm('确定删除「' + a.title + '」？此操作不可撤销。')) return;
      apiFetch('/admin/api/announcements/' + a.id, { method: 'DELETE' })
        .then(async () => {
          toast('已删除', true);
          const res = await apiFetch('/admin/api/announcements');
          const data = await res.json();
          all = data.items || [];
          adConfig = data.adConfig || adConfig;
          render();
        })
        .catch(e => toast('删除失败', false));
    }

    function toast(msg, ok) {
      const el = document.createElement('div');
      el.className = 'toast ' + (ok ? 'toast-success' : 'toast-error');
      el.textContent = msg;
      document.body.appendChild(el);
      setTimeout(() => el.remove(), 2500);
    }

    function esc(s) { const d = document.createElement('div'); d.textContent = s || ''; return d.innerHTML; }

    // ─── 图片上传：FileReader 读 data URI → /admin/api/upload → 光标处插入 ![图片](url) ───
    document.getElementById('imgInput').addEventListener('change', uploadImage);

    async function uploadImage() {
      const input = document.getElementById('imgInput');
      const file = input.files && input.files[0];
      if (!file) return;
      if (!/^image\\/(jpeg|png|webp|gif)$/i.test(file.type)) {
        toast('仅支持 jpeg/png/webp/gif 图片', false);
        input.value = '';
        return;
      }
      if (file.size > 10 * 1024 * 1024) {
        toast('图片不能超过 10MB', false);
        input.value = '';
        return;
      }
      const btn = document.getElementById('uploadBtn');
      btn.disabled = true; btn.textContent = '上传中...';
      try {
        const dataUri = await readAsDataURL(file);
        const res = await apiFetch('/admin/api/upload', { method: 'POST', body: JSON.stringify({ image: dataUri }) });
        const data = await res.json();
        // 直链经本 Worker 的 /img 反代下发，绕开 imgbb 大陆直连不可达问题；
        // 公开接口返回时也会对历史直链自动改写，这里提前写入保持一致
        const proxied = location.origin + '/img?url=' + encodeURIComponent(data.url);
        insertAtCursor('editContent', '![图片](' + proxied + ')\\n');
        toast('图片已上传，将显示在公告正文', true);
      } catch (e) {
        toast(e.message || '上传失败', false);
      } finally {
        btn.disabled = false; btn.textContent = '上传图片';
        input.value = '';
      }
    }

    function readAsDataURL(file) {
      return new Promise((resolve, reject) => {
        const r = new FileReader();
        r.onload = () => resolve(r.result);
        r.onerror = () => reject(new Error('读取文件失败'));
        r.readAsDataURL(file);
      });
    }

    function insertAtCursor(id, text) {
      const el = document.getElementById(id);
      const start = el.selectionStart ?? el.value.length;
      const end = el.selectionEnd ?? el.value.length;
      el.value = el.value.slice(0, start) + text + el.value.slice(end);
      el.focus();
      const pos = start + text.length;
      el.setSelectionRange(pos, pos);
    }

    document.getElementById('tokenInput').addEventListener('keypress', e => { if (e.key === 'Enter') login(); });
  </script>
</body>
</html>`;
}

export default app;
