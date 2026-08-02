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
  device_id: string;
  likes: number;
  status: string;
  created_at: number;
  updated_at: number;
}

type EvalStatus = 'pending' | 'approved' | 'rejected';

const app = new Hono<{ Bindings: Env }>();

app.use('*', cors());

// ─── Helpers ───

function deviceIdOf(c: any): string {
  return (c.req.header('X-Device-Id') || '').trim();
}

function rowToEval(r: EvaluationRow, liked: boolean) {
  return {
    id: r.id,
    course_name: r.course_name,
    teacher: r.teacher,
    rating: r.rating,
    content: r.content,
    anonymous: !!r.anonymous,
    // 匿名时隐藏作者信息
    author: r.anonymous ? '' : r.author,
    user_no: r.user_no,
    device_id: r.device_id,
    likes: r.likes,
    status: r.status,
    created_at: r.created_at,
    liked,
  };
}

// 为一组评价计算当前设备是否已点赞
async function withLiked(c: any, rows: EvaluationRow[]): Promise<any[]> {
  const dev = deviceIdOf(c);
  return Promise.all(
    rows.map(async (r) => {
      let liked = false;
      if (dev) {
        const lr = await c.env.DB.prepare(
          'SELECT 1 FROM evaluation_likes WHERE device_id = ? AND evaluation_id = ?'
        )
          .bind(dev, r.id)
          .first();
        liked = !!lr;
      }
      return rowToEval(r, liked);
    })
  );
}

function nowSec(): number {
  return Math.floor(Date.now() / 1000);
}

// ─── Public: list evaluations ───
// 仅返回已通过审核的评价；若携带 X-Device-Id，则额外包含该设备自己提交的评价（含待审）。
app.get('/evaluations', async (c) => {
  const course = c.req.query('course');
  const teacher = c.req.query('teacher');
  const page = Math.max(1, parseInt(c.req.query('page') || '1', 10) || 1);
  const size = Math.min(100, Math.max(1, parseInt(c.req.query('size') || '20', 10) || 20));
  const dev = deviceIdOf(c);

  const where: string[] = [];
  const params: any[] = [];

  if (dev) {
    where.push('(status = ? OR device_id = ?)');
    params.push('approved', dev);
  } else {
    where.push('status = ?');
    params.push('approved');
  }
  if (course) {
    where.push('course_name = ?');
    params.push(course);
  }
  if (teacher) {
    where.push('teacher LIKE ?');
    params.push(`%${teacher}%`);
  }

  const countRes = await c.env.DB.prepare(
    `SELECT COUNT(*) as cnt FROM evaluations WHERE ${where.join(' AND ')}`
  )
    .bind(...params)
    .first<{ cnt: number }>();
  const total = countRes?.cnt ?? 0;

  const rows = await c.env.DB.prepare(
    `SELECT * FROM evaluations WHERE ${where.join(' AND ')} ORDER BY created_at DESC LIMIT ? OFFSET ?`
  )
    .bind(...params, size, (page - 1) * size)
    .all<EvaluationRow>();

  const items = await withLiked(c, rows.results);
  return c.json({ items, total, page, size });
});

// ─── Public: evaluation detail ───
app.get('/evaluations/:id', async (c) => {
  const id = c.req.param('id');
  const dev = deviceIdOf(c);
  const row = await c.env.DB.prepare(
    'SELECT * FROM evaluations WHERE id = ? AND (status = ? OR device_id = ?)'
  )
    .bind(id, 'approved', dev)
    .first<EvaluationRow>();

  if (!row) {
    return c.json({ error: 'Evaluation not found' }, 404);
  }

  const liked = dev
    ? !!(await c.env.DB.prepare(
        'SELECT 1 FROM evaluation_likes WHERE device_id = ? AND evaluation_id = ?'
      )
        .bind(dev, id)
        .first())
    : false;

  return c.json(rowToEval(row, liked));
});

// ─── Public: create evaluation ───
// 后端不做“仅限已选课程”的限制（由客户端限制），收到即存储为 pending 待审核。
app.post('/evaluations', async (c) => {
  try {
    const body = await c.req.json<{
      course_name?: string;
      teacher?: string;
      rating?: number;
      content?: string;
      anonymous?: boolean;
      author?: string;
      user_no?: string;
    }>();

    const courseName = (body.course_name || '').trim();
    if (!courseName) {
      return c.json({ error: 'course_name is required' }, 400);
    }

    const rating = Math.round(body.rating ?? 0);
    if (rating < 1 || rating > 5) {
      return c.json({ error: 'rating must be between 1 and 5' }, 400);
    }

    const content = (body.content || '').trim();
    if (!content) {
      return c.json({ error: 'content is required' }, 400);
    }

    const id = crypto.randomUUID();
    const ts = nowSec();
    const anonymous = body.anonymous ? 1 : 0;
    const author = anonymous ? '' : (body.author || '').trim();
    const deviceId = deviceIdOf(c);
    const userNo = (body.user_no || '').trim();

    await c.env.DB.prepare(
      `INSERT INTO evaluations
        (id, course_name, teacher, rating, content, anonymous, author, user_no, device_id, likes, status, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 'pending', ?, ?)`
    )
      .bind(id, courseName, (body.teacher || '').trim(), rating, content, anonymous, author, userNo, deviceId, ts, ts)
      .run();

    const row = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ?').bind(id).first<EvaluationRow>();
    return c.json(rowToEval(row!, false), 201);
  } catch (e) {
    return c.json({ error: `Create failed: ${e}` }, 500);
  }
});

// ─── Public: toggle like (interaction) ───
app.post('/evaluations/:id/like', async (c) => {
  const id = c.req.param('id');
  const dev = deviceIdOf(c);
  if (!dev) {
    return c.json({ error: 'X-Device-Id header required' }, 400);
  }

  const row = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ?').bind(id).first<EvaluationRow>();
  if (!row) {
    return c.json({ error: 'Evaluation not found' }, 404);
  }

  const existing = await c.env.DB.prepare(
    'SELECT 1 FROM evaluation_likes WHERE device_id = ? AND evaluation_id = ?'
  )
    .bind(dev, id)
    .first();

  let liked: boolean;
  if (existing) {
    await c.env.DB.prepare('DELETE FROM evaluation_likes WHERE device_id = ? AND evaluation_id = ?')
      .bind(dev, id)
      .run();
    await c.env.DB.prepare('UPDATE evaluations SET likes = MAX(0, likes - 1) WHERE id = ?').bind(id).run();
    liked = false;
  } else {
    await c.env.DB.prepare('INSERT INTO evaluation_likes (device_id, evaluation_id, created_at) VALUES (?, ?, ?)')
      .bind(dev, id, nowSec())
      .run();
    await c.env.DB.prepare('UPDATE evaluations SET likes = likes + 1 WHERE id = ?').bind(id).run();
    liked = true;
  }

  const updated = await c.env.DB.prepare('SELECT likes FROM evaluations WHERE id = ?').bind(id).first<{ likes: number }>();
  return c.json({ likes: updated?.likes ?? 0, liked });
});

// ─── Public: delete own evaluation ───
app.delete('/evaluations/:id', async (c) => {
  const id = c.req.param('id');
  const dev = deviceIdOf(c);
  if (!dev) {
    return c.json({ error: 'X-Device-Id header required' }, 400);
  }

  const row = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ? AND device_id = ?')
    .bind(id, dev)
    .first<EvaluationRow>();
  if (!row) {
    return c.json({ error: 'Evaluation not found or unauthorized' }, 404);
  }

  await c.env.DB.prepare('DELETE FROM evaluation_likes WHERE evaluation_id = ?').bind(id).run();
  await c.env.DB.prepare('DELETE FROM evaluations WHERE id = ?').bind(id).run();
  return c.json({ ok: true });
});

// ─── Admin auth ───
const adminAuth = async (c: any, next: any) => {
  const token = c.req.header('Authorization')?.replace('Bearer ', '');
  if (token !== c.env.ADMIN_TOKEN) {
    return c.json({ error: 'Unauthorized' }, 401);
  }
  await next();
};

// 管理员登录：使用管理密码换取管理令牌（Bearer 鉴权）
app.post('/admin/login', async (c) => {
  try {
    const body = await c.req.json<{ password?: string }>();
    if (!body.password || body.password !== c.env.ADMIN_PASSWORD) {
      return c.json({ error: 'Invalid password' }, 401);
    }
    return c.json({ token: c.env.ADMIN_TOKEN, ok: true });
  } catch {
    return c.json({ error: 'Bad request' }, 400);
  }
});

// 管理员：列出全部评价（可按状态/课程筛选）
app.get('/admin/evaluations', adminAuth, async (c) => {
  const status = c.req.query('status');
  const course = c.req.query('course');
  const page = Math.max(1, parseInt(c.req.query('page') || '1', 10) || 1);
  const size = Math.min(100, Math.max(1, parseInt(c.req.query('size') || '20', 10) || 20));

  const where: string[] = [];
  const params: any[] = [];
  if (status) {
    where.push('status = ?');
    params.push(status);
  }
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

  const items = await withLiked(c, rows.results);
  return c.json({ items, total, page, size });
});

// 管理员：评价详情
app.get('/admin/evaluations/:id', adminAuth, async (c) => {
  const id = c.req.param('id');
  const row = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ?').bind(id).first<EvaluationRow>();
  if (!row) {
    return c.json({ error: 'Not found' }, 404);
  }
  return c.json(rowToEval(row, false));
});

// 管理员：修改评价（审核状态 / 内容 / 评分等）
app.patch('/admin/evaluations/:id', adminAuth, async (c) => {
  const id = c.req.param('id');
  const row = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ?').bind(id).first<EvaluationRow>();
  if (!row) {
    return c.json({ error: 'Not found' }, 404);
  }

  const body = await c.req.json<{
    status?: EvalStatus;
    content?: string;
    rating?: number;
    teacher?: string;
    anonymous?: boolean;
    course_name?: string;
    author?: string;
  }>();

  const sets: string[] = [];
  const params: any[] = [];

  if (body.status !== undefined) {
    sets.push('status = ?');
    params.push(body.status);
  }
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
    return c.json(rowToEval(row, false));
  }

  sets.push('updated_at = ?');
  params.push(nowSec());
  params.push(id);

  await c.env.DB.prepare(`UPDATE evaluations SET ${sets.join(', ')} WHERE id = ?`)
    .bind(...params)
    .run();

  const updated = await c.env.DB.prepare('SELECT * FROM evaluations WHERE id = ?').bind(id).first<EvaluationRow>();
  return c.json(rowToEval(updated!, false));
});

// 管理员：删除任意评价
app.delete('/admin/evaluations/:id', adminAuth, async (c) => {
  const id = c.req.param('id');
  await c.env.DB.prepare('DELETE FROM evaluation_likes WHERE evaluation_id = ?').bind(id).run();
  await c.env.DB.prepare('DELETE FROM evaluations WHERE id = ?').bind(id).run();
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

    .toolbar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; flex-wrap: wrap; }
    .toolbar input[type="text"] { flex: 1; min-width: 160px; padding: 9px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; }
    .toolbar select { padding: 9px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; background: #fff; }

    .stat-bar { display: flex; gap: 16px; margin-bottom: 20px; }
    .stat { background: #fff; border-radius: 10px; padding: 16px 20px; box-shadow: 0 1px 2px rgba(0,0,0,.06); flex: 1; text-align: center; }
    .stat-num { font-size: 24px; font-weight: 700; color: #0066ff; }
    .stat-label { font-size: 12px; color: #888; margin-top: 4px; }

    .eval-list { display: flex; flex-direction: column; gap: 8px; }
    .eval-card { background: #fff; border-radius: 10px; padding: 14px 16px; box-shadow: 0 1px 2px rgba(0,0,0,.06); }
    .eval-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
    .eval-course { font-size: 15px; font-weight: 600; }
    .eval-meta { font-size: 12px; color: #888; margin-top: 4px; display: flex; gap: 12px; flex-wrap: wrap; }
    .eval-content { font-size: 14px; color: #444; margin-top: 8px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
    .eval-actions { display: flex; gap: 6px; margin-top: 10px; flex-wrap: wrap; }
    .badge { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 12px; }
    .badge-pending { background: #fff4e0; color: #b26a00; }
    .badge-approved { background: #e3f5e3; color: #1a7a1a; }
    .badge-rejected { background: #fde3e1; color: #cc0000; }
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

    <div class="stat-bar">
      <div class="stat"><div class="stat-num" id="totalCount">-</div><div class="stat-label">评价总数</div></div>
      <div class="stat"><div class="stat-num" id="pendingCount">-</div><div class="stat-label">待审核</div></div>
      <div class="stat"><div class="stat-num" id="approvedCount">-</div><div class="stat-label">已通过</div></div>
    </div>

    <div class="toolbar">
      <input type="text" id="searchInput" placeholder="搜索课程 / 内容 / 作者..." oninput="debounceSearch()">
      <select id="statusFilter" onchange="loadEvaluations()">
        <option value="">全部状态</option>
        <option value="pending">待审核</option>
        <option value="approved">已通过</option>
        <option value="rejected">已拒绝</option>
      </select>
      <button class="btn-ghost btn-sm" onclick="loadEvaluations()">刷新</button>
    </div>

    <div id="evalList" class="eval-list"></div>
    <div id="emptyState" class="empty hidden">没有评价</div>
  </div>

  <!-- Detail / Edit Modal -->
  <div id="detailModal" class="modal-overlay hidden" onclick="if(event.target===this)this.classList.add('hidden')">
    <div class="modal">
      <h2 id="detailTitle">-</h2>
      <div id="detailBody" style="font-size:14px; line-height:1.8; color:#555;"></div>
      <div class="form-group" style="margin-top:16px;">
        <label>审核状态</label>
        <select id="detailStatus">
          <option value="pending">待审核</option>
          <option value="approved">通过</option>
          <option value="rejected">拒绝</option>
        </select>
      </div>
      <div class="modal-actions">
        <button class="btn-ghost" onclick="closeModal('detailModal')">关闭</button>
        <button class="btn-danger" id="detailDeleteBtn" onclick="confirmDelete()">删除</button>
        <button class="btn-primary" onclick="saveDetail()">保存</button>
      </div>
    </div>
  </div>

  <script>
    let token = '';
    let allEvals = [];
    let currentId = '';
    let searchTimer = null;

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

    async function loadEvaluations() {
      const q = document.getElementById('searchInput').value.trim();
      const status = document.getElementById('statusFilter').value;
      const params = new URLSearchParams();
      if (status) params.set('status', status);
      if (q) params.set('q', q);

      try {
        const res = await apiFetch('/admin/evaluations?' + params.toString());
        const data = await res.json();
        allEvals = data.items || [];
        renderEvals();
        updateStats();
      } catch (e) { console.error(e); }
    }

    function renderEvals() {
      const list = document.getElementById('evalList');
      const empty = document.getElementById('emptyState');
      if (!allEvals.length) { list.innerHTML = ''; empty.classList.remove('hidden'); return; }
      empty.classList.add('hidden');
      list.innerHTML = '';
      allEvals.forEach(e => {
        const card = document.createElement('div');
        card.className = 'eval-card';
        const stars = '★'.repeat(e.rating) + '☆'.repeat(5 - e.rating);
        const statusText = { pending: '待审核', approved: '已通过', rejected: '已拒绝' }[e.status] || e.status;
        const anon = e.anonymous ? '匿名' : (e.author || (e.user_no ? '学号 ' + e.user_no : '未知'));
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
            '<span class="badge badge-' + e.status + '">' + statusText + '</span>' +
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
      document.getElementById('pendingCount').textContent = allEvals.filter(e => e.status === 'pending').length;
      document.getElementById('approvedCount').textContent = allEvals.filter(e => e.status === 'approved').length;
    }

    function debounceSearch() { clearTimeout(searchTimer); searchTimer = setTimeout(loadEvaluations, 300); }

    async function showDetail(id) {
      try {
        const res = await apiFetch('/admin/evaluations/' + id);
        const e = await res.json();
        currentId = id;
        document.getElementById('detailTitle').textContent = e.course_name;
        const stars = '★'.repeat(e.rating) + '☆'.repeat(5 - e.rating);
        const anon = e.anonymous ? '匿名' : (e.author || (e.user_no ? '学号 ' + e.user_no : '未知'));
        document.getElementById('detailBody').innerHTML =
          '<b>课程:</b> ' + esc(e.course_name) + '<br>' +
          '<b>教师:</b> ' + esc(e.teacher || '未知') + '<br>' +
          '<b>评分:</b> <span class="stars">' + stars + '</span><br>' +
          '<b>作者:</b> ' + esc(anon) + '<br>' +
          '<b>点赞:</b> ' + (e.likes || 0) + '<br>' +
          '<b>状态:</b> ' + e.status + '<br>' +
          '<b>提交时间:</b> ' + new Date(e.created_at * 1000).toLocaleString('zh-CN') + '<br>' +
          '<b>内容:</b><br>' + esc(e.content);
        document.getElementById('detailStatus').value = e.status;
        document.getElementById('detailModal').classList.remove('hidden');
      } catch (e) { toast('加载失败', false); }
    }

    async function saveDetail() {
      const status = document.getElementById('detailStatus').value;
      try {
        await apiFetch('/admin/evaluations/' + currentId, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status })
        });
        closeModal('detailModal');
        toast('已保存', true);
        loadEvaluations();
      } catch (e) { toast('保存失败: ' + e.message, false); }
    }

    function confirmDelete() {
      if (!confirm('确定删除该评价？此操作不可撤销。')) return;
      apiFetch('/admin/evaluations/' + currentId, { method: 'DELETE' })
        .then(() => { closeModal('detailModal'); toast('已删除', true); loadEvaluations(); })
        .catch(e => toast('删除失败: ' + e.message, false));
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
