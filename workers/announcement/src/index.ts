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
  if (body.status !== undefined && body.status !== 'published' && body.status !== 'draft') {
    return 'status 仅支持 published 或 draft';
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

// ─── 公开只读接口（仅返回已发布） ───

// Get published announcements
app.get('/announcements', async (c) => {
  const limitParam = c.req.query('limit');
  const limit = Math.min(Math.max(parseInt(limitParam || '50', 10) || 50, 1), 100);

  const result = await c.env.DB.prepare(
    'SELECT * FROM announcements WHERE status = \'published\' ORDER BY pinned DESC, created_at DESC LIMIT ?'
  ).bind(limit).all<Announcement>();

  return c.json(result.results.map(toWireAnnouncement));
});

// Get published announcement detail
app.get('/announcements/:id', async (c) => {
  const id = c.req.param('id');
  const result = await c.env.DB.prepare(
    'SELECT * FROM announcements WHERE id = ? AND status = \'published\''
  ).bind(id).first<Announcement>();

  if (!result) {
    return c.json({ error: 'Announcement not found' }, 404);
  }
  return c.json(toWireAnnouncement(result));
});

// ─── 管理接口（Bearer ADMIN_TOKEN） ───

const adminAuth = async (c: any, next: any) => {
  if (!isAdmin(c)) {
    return c.json({ error: 'Unauthorized' }, 401);
  }
  await next();
};

// Admin: list all announcements (含草稿；管理端需要 status 字段，返回原始行)
app.get('/admin/api/announcements', adminAuth, async (c) => {
  const result = await c.env.DB.prepare(
    'SELECT * FROM announcements ORDER BY pinned DESC, created_at DESC'
  ).all<Announcement>();
  return c.json(result.results);
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
 */
function toWireAnnouncement(a: Announcement): Omit<Announcement, 'status' | 'pinned'> & { pinned: boolean } {
  const { status, pinned, ...rest } = a;
  return { ...rest, pinned: !!pinned };
}

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
      <button class="btn-primary" onclick="openCreate()">+ 新建公告</button>
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

  <script>
    let token = '';
    let all = [];
    let editingId = null;

    function login() {
      token = document.getElementById('tokenInput').value;
      if (!token) return;
      apiFetch('/admin/api/announcements')
        .then(r => r.json())
        .then(items => {
          document.getElementById('loginView').classList.add('hidden');
          document.getElementById('mainView').classList.remove('hidden');
          all = items;
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
        if (a.status !== 'published') badges.push('<span class="badge badge-draft">草稿</span>');
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
        all = await res.json();
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
          all = await res.json();
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
        insertAtCursor('editContent', '![图片](' + data.url + ')\\n');
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
