import { Hono } from 'hono';
import { cors } from 'hono/cors';

interface Env {
  DB: D1Database;
  GITHUB_OWNER: string;
  GITHUB_REPO: string;
  GITHUB_STAR_REPO: string;
  GITHUB_TOKEN: string;
  ADMIN_TOKEN: string;
}

interface Paper {
  id: string;
  title: string;
  course: string;
  folder: string;
  year: number;
  school: string;
  path: string;
  size: number;
  device_id: string;
  downloads: number;
  created_at: number;
}

// ─── Security helpers & constants ───

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

/** 上传允许的文件扩展名（白名单，防止上传可执行 / 脚本文件） */
const ALLOWED_EXT = ['pdf', 'doc', 'docx'];
/** 单文件大小上限：20 MB */
const MAX_UPLOAD_BYTES = 20 * 1024 * 1024;

/** 去除标题中的特殊字符，防止 Content-Disposition 头注入 / 文件名穿越 */
function safeFileName(title: string): string {
  return (title || '').replace(/[^\w一-龥\-_. ]/g, '_').replace(/\s+/g, '_').slice(0, 120) || 'paper';
}

/** 剥离 device_id 等敏感字段，仅返回公开安全字段（防 IDOR 泄漏） */
function publicPaper(p: Paper): Omit<Paper, 'device_id'> {
  const { device_id, ...rest } = p;
  return rest;
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

const app = new Hono<{ Bindings: Env }>();

// CORS 仅对公开只读接口开放；管理接口同源（管理后台同源部署），无需 CORS。
app.use('/papers', cors());
app.use('/papers/*', cors());
app.use('/courses', cors());
app.use('/folders', cors());
app.use('/download/*', cors());
app.use('/verify-star', cors());

// Get all papers with optional filters
app.get('/papers', async (c) => {
  const folder = c.req.query('folder');
  const course = c.req.query('course');
  const q = c.req.query('q');

  let query = 'SELECT * FROM papers WHERE 1=1';
  const params: any[] = [];

  if (folder) {
    query += ' AND folder = ?';
    params.push(folder);
  }
  if (course) {
    query += ' AND course = ?';
    params.push(course);
  }
  if (q) {
    query += ' AND (title LIKE ? OR course LIKE ?)';
    params.push(`%${q}%`, `%${q}%`);
  }

  query += ' ORDER BY created_at DESC';

  const result = await c.env.DB.prepare(query).bind(...params).all<Paper>();
  return c.json(result.results.map(publicPaper));
});

// Get unique courses
app.get('/courses', async (c) => {
  const result = await c.env.DB.prepare(
    'SELECT DISTINCT course FROM papers ORDER BY course'
  ).all<{ course: string }>();
  return c.json(result.results.map((r) => r.course));
});

// Get unique folders
app.get('/folders', async (c) => {
  const result = await c.env.DB.prepare(
    'SELECT DISTINCT folder FROM papers ORDER BY folder'
  ).all<{ folder: string }>();
  return c.json(result.results.map((r) => r.folder));
});

// Get paper detail
app.get('/papers/:id', async (c) => {
  const id = c.req.param('id');
  const result = await c.env.DB.prepare(
    'SELECT * FROM papers WHERE id = ?'
  ).bind(id).first<Paper>();

  if (!result) {
    return c.json({ error: 'Paper not found' }, 404);
  }
  return c.json(publicPaper(result));
});

// Download paper (proxy download from GitHub)
app.get('/download/:id', async (c) => {
  const id = c.req.param('id');
  const result = await c.env.DB.prepare(
    'SELECT * FROM papers WHERE id = ?'
  ).bind(id).first<Paper>();

  if (!result) {
    return c.json({ error: 'Paper not found' }, 404);
  }

  await c.env.DB.prepare(
    'UPDATE papers SET downloads = downloads + 1 WHERE id = ?'
  ).bind(id).run();

  const rawUrl = `https://raw.githubusercontent.com/${c.env.GITHUB_OWNER}/${c.env.GITHUB_REPO}/main/${result.path}`;
  const ghResponse = await fetch(rawUrl, {
    headers: {
      'User-Agent': 'ChrnovaPapers-Worker',
      'Authorization': `token ${c.env.GITHUB_TOKEN}`,
    },
  });

  if (!ghResponse.ok) {
    return c.json({ error: 'Failed to fetch from GitHub' }, 500);
  }

  return new Response(ghResponse.body, {
    headers: {
      'Content-Type': 'application/pdf',
      'Content-Disposition': `attachment; filename="${safeFileName(result.title)}.pdf"`,
    },
  });
});

// Upload paper
// 公开接口，依赖 X-Device-Id 做软鉴权（客户端契约，不可改为管理令牌以免破坏 App）。
// 缓解措施：文件类型 / 大小白名单 + 限流 + 错误信息脱敏。
app.post('/upload', async (c) => {
  const deviceId = c.req.header('X-Device-Id');
  if (!deviceId) {
    return c.json({ error: 'X-Device-Id header required' }, 400);
  }

  if (!rateLimit(`upload:${clientIp(c)}:${deviceId}`, 30, 60)) {
    return c.json({ error: '上传过于频繁，请稍后再试' }, 429);
  }

  try {
    const formData = await c.req.formData();
    const file = formData.get('file') as File | null;
    const title = formData.get('title') as string;
    const folder = formData.get('folder') as string;

    if (!file || !title || !folder) {
      return c.json({ error: 'Missing required fields' }, 400);
    }

    if (file.size > MAX_UPLOAD_BYTES) {
      return c.json({ error: `文件过大，上限 ${MAX_UPLOAD_BYTES / 1024 / 1024} MB` }, 413);
    }

    const ext = (file.name.split('.').pop() || '').toLowerCase();
    if (!ALLOWED_EXT.includes(ext)) {
      return c.json({ error: `不支持的文件类型，仅允许：${ALLOWED_EXT.join(', ')}` }, 400);
    }

    const id = crypto.randomUUID();
    const path = `papers/${id}.${ext}`;
    const createdAt = Math.floor(Date.now() / 1000);

    const arrayBuffer = await file.arrayBuffer();
    const uint8Array = new Uint8Array(arrayBuffer);
    let binary = '';
    for (let i = 0; i < uint8Array.byteLength; i++) {
      binary += String.fromCharCode(uint8Array[i]);
    }
    const base64Content = btoa(binary);

    const githubResponse = await fetch(
      `https://api.github.com/repos/${c.env.GITHUB_OWNER}/${c.env.GITHUB_REPO}/contents/${path}`,
      {
        method: 'PUT',
        headers: {
          Authorization: `token ${c.env.GITHUB_TOKEN}`,
          'Content-Type': 'application/json',
          'User-Agent': 'ChrnovaPapers-Worker',
        },
        body: JSON.stringify({
          message: `Upload ${title}`,
          content: base64Content,
        }),
      }
    );

    if (!githubResponse.ok) {
      return c.json({ error: 'GitHub 上传失败，请稍后再试' }, 500);
    }

    await c.env.DB.prepare(
      `INSERT INTO papers (id, title, folder, path, size, device_id, downloads, created_at)
       VALUES (?, ?, ?, ?, ?, ?, 0, ?)`
    )
      .bind(id, title, folder, path, file.size, deviceId, createdAt)
      .run();

    return c.json({ id, path }, 201);
  } catch (e) {
    return c.json({ error: 'Upload failed' }, 500);
  }
});

// Verify GitHub star
app.get('/verify-star', async (c) => {
  const username = c.req.query('username');
  if (!username) {
    return c.json({ error: 'Missing username parameter' }, 400);
  }

  try {
    const token = c.env.GITHUB_TOKEN;
    if (!token) {
      return c.json({ starred: false, error: 'GITHUB_TOKEN not configured' }, 500);
    }

    const targetRepo = `${c.env.GITHUB_OWNER}/${c.env.GITHUB_STAR_REPO}`;
    const response = await fetch(
      `https://api.github.com/users/${encodeURIComponent(username)}/starred?per_page=100`,
      {
        headers: {
          'User-Agent': 'ChrnovaPapers-Worker',
          'Accept': 'application/vnd.github.v3+json',
          'Authorization': `token ${token}`,
        },
      }
    );

    if (!response.ok) {
      return c.json({ starred: false, error: `GitHub API 请求失败 (${response.status})` }, 502);
    }

    const starredRepos = await response.json<Array<{ full_name: string }>>();
    const hasStarred = starredRepos.some(
      (repo) => repo.full_name.toLowerCase() === targetRepo.toLowerCase()
    );

    return c.json({ starred: hasStarred, username });
  } catch (e) {
    return c.json({ starred: false, error: 'Verification failed' }, 500);
  }
});

// Delete paper (only by device_id match)
app.delete('/papers/:id', async (c) => {
  const id = c.req.param('id');
  const deviceId = c.req.header('X-Device-Id');

  if (!deviceId) {
    return c.json({ error: 'X-Device-Id header required' }, 400);
  }

  if (!rateLimit(`del:${clientIp(c)}:${deviceId}`, 20, 60)) {
    return c.json({ error: '操作过于频繁，请稍后再试' }, 429);
  }

  const result = await c.env.DB.prepare(
    'SELECT * FROM papers WHERE id = ? AND device_id = ?'
  ).bind(id, deviceId).first<Paper>();

  if (!result) {
    return c.json({ error: 'Paper not found or unauthorized' }, 404);
  }

  await c.env.DB.prepare('DELETE FROM papers WHERE id = ?').bind(id).run();
  return c.json({ message: 'Paper deleted' });
});

// ─── Admin Web UI ───

app.get('/', (c) => {
  return c.html(getAdminHTML());
});

// Admin auth middleware
const adminAuth = async (c: any, next: any) => {
  if (!isAdmin(c)) {
    return c.json({ error: 'Unauthorized' }, 401);
  }
  await next();
};

// Admin: list all papers
app.get('/admin/api/papers', adminAuth, async (c) => {
  const q = c.req.query('q');
  const folder = c.req.query('folder');

  let query = 'SELECT * FROM papers WHERE 1=1';
  const params: any[] = [];

  if (folder) {
    query += ' AND folder = ?';
    params.push(folder);
  }
  if (q) {
    query += ' AND (title LIKE ? OR course LIKE ? OR folder LIKE ?)';
    params.push(`%${q}%`, `%${q}%`, `%${q}%`);
  }

  query += ' ORDER BY created_at DESC';

  const result = await c.env.DB.prepare(query).bind(...params).all<Paper>();
  return c.json(result.results);
});

// Admin: get paper detail
app.get('/admin/api/papers/:id', adminAuth, async (c) => {
  const id = c.req.param('id');
  const result = await c.env.DB.prepare('SELECT * FROM papers WHERE id = ?').bind(id).first<Paper>();
  if (!result) return c.json({ error: 'Not found' }, 404);
  return c.json(result);
});

// Admin: delete paper (no device_id check)
app.delete('/admin/api/papers/:id', adminAuth, async (c) => {
  const id = c.req.param('id');
  const result = await c.env.DB.prepare('SELECT * FROM papers WHERE id = ?').bind(id).first<Paper>();
  if (!result) return c.json({ error: 'Not found' }, 404);

  // Get SHA from GitHub then delete
  try {
    const meta = await fetch(
      `https://api.github.com/repos/${c.env.GITHUB_OWNER}/${c.env.GITHUB_REPO}/contents/${result.path}`,
      {
        headers: {
          Authorization: `token ${c.env.GITHUB_TOKEN}`,
          'User-Agent': 'ChrnovaPapers-Worker',
        },
      }
    );
    if (meta.ok) {
      const fileData = await meta.json<{ sha: string }>();
      await fetch(
        `https://api.github.com/repos/${c.env.GITHUB_OWNER}/${c.env.GITHUB_REPO}/contents/${result.path}`,
        {
          method: 'DELETE',
          headers: {
            Authorization: `token ${c.env.GITHUB_TOKEN}`,
            'Content-Type': 'application/json',
            'User-Agent': 'ChrnovaPapers-Worker',
          },
          body: JSON.stringify({ message: `Delete ${result.title}`, sha: fileData.sha }),
        }
      );
    }
  } catch (_) { /* GitHub deletion best-effort */ }

  await c.env.DB.prepare('DELETE FROM papers WHERE id = ?').bind(id).run();
  return c.json({ ok: true });
});

// Admin: upload paper
app.post('/admin/api/upload', adminAuth, async (c) => {
  try {
    const formData = await c.req.formData();
    const file = formData.get('file') as File | null;
    const title = formData.get('title') as string;
    const folder = formData.get('folder') as string;
    const course = (formData.get('course') as string) || '';

    if (!file || !title || !folder) {
      return c.json({ error: 'Missing required fields' }, 400);
    }

    if (file.size > MAX_UPLOAD_BYTES) {
      return c.json({ error: `文件过大，上限 ${MAX_UPLOAD_BYTES / 1024 / 1024} MB` }, 413);
    }
    const ext = (file.name.split('.').pop() || '').toLowerCase();
    if (!ALLOWED_EXT.includes(ext)) {
      return c.json({ error: `不支持的文件类型，仅允许：${ALLOWED_EXT.join(', ')}` }, 400);
    }

    const id = crypto.randomUUID();
    const path = `papers/${id}.${ext}`;
    const createdAt = Math.floor(Date.now() / 1000);

    const arrayBuffer = await file.arrayBuffer();
    const uint8Array = new Uint8Array(arrayBuffer);
    let binary = '';
    for (let i = 0; i < uint8Array.byteLength; i++) {
      binary += String.fromCharCode(uint8Array[i]);
    }
    const base64Content = btoa(binary);

    const githubResponse = await fetch(
      `https://api.github.com/repos/${c.env.GITHUB_OWNER}/${c.env.GITHUB_REPO}/contents/${path}`,
      {
        method: 'PUT',
        headers: {
          Authorization: `token ${c.env.GITHUB_TOKEN}`,
          'Content-Type': 'application/json',
          'User-Agent': 'ChrnovaPapers-Worker',
        },
        body: JSON.stringify({
          message: `Upload ${title}`,
          content: base64Content,
        }),
      }
    );

    if (!githubResponse.ok) {
      return c.json({ error: 'GitHub 上传失败，请稍后再试' }, 500);
    }

    await c.env.DB.prepare(
      `INSERT INTO papers (id, title, folder, path, size, device_id, downloads, created_at)
       VALUES (?, ?, ?, ?, ?, ?, 0, ?)`
    )
      .bind(id, title, folder, path, file.size, 'admin', createdAt)
      .run();

    return c.json({ id, path }, 201);
  } catch (e) {
    return c.json({ error: 'Upload failed' }, 500);
  }
});

// Admin: get unique folders
app.get('/admin/api/folders', adminAuth, async (c) => {
  const result = await c.env.DB.prepare(
    'SELECT DISTINCT folder FROM papers ORDER BY folder'
  ).all<{ folder: string }>();
  return c.json(result.results.map((r) => r.folder));
});

function getAdminHTML(): string {
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Chrnova 课件管理</title>
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
    .form-group textarea { min-height: 60px; resize: vertical; }

    button { padding: 10px 20px; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; transition: background .2s; }
    .btn-primary { background: #0066ff; color: #fff; }
    .btn-primary:hover { background: #0052cc; }
    .btn-danger { background: #ff3b30; color: #fff; }
    .btn-danger:hover { background: #d32f2f; }
    .btn-ghost { background: transparent; color: #0066ff; }
    .btn-ghost:hover { background: #f0f0f0; }
    .btn-sm { padding: 6px 12px; font-size: 12px; }
    button:disabled { opacity: .5; cursor: not-allowed; }

    .toolbar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; flex-wrap: wrap; }
    .toolbar input[type="text"] { flex: 1; min-width: 180px; padding: 9px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; }
    .toolbar select { padding: 9px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; background: #fff; }

    .paper-list { display: flex; flex-direction: column; gap: 8px; }
    .paper-card { background: #fff; border-radius: 10px; padding: 14px 16px; box-shadow: 0 1px 2px rgba(0,0,0,.06); display: flex; justify-content: space-between; align-items: center; gap: 12px; transition: box-shadow .2s; }
    .paper-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,.1); }
    .paper-info { flex: 1; min-width: 0; }
    .paper-title { font-size: 15px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .paper-meta { font-size: 12px; color: #888; margin-top: 4px; display: flex; gap: 12px; flex-wrap: wrap; }
    .paper-actions { display: flex; gap: 6px; flex-shrink: 0; }

    .stat-bar { display: flex; gap: 16px; margin-bottom: 20px; }
    .stat { background: #fff; border-radius: 10px; padding: 16px 20px; box-shadow: 0 1px 2px rgba(0,0,0,.06); flex: 1; text-align: center; }
    .stat-num { font-size: 24px; font-weight: 700; color: #0066ff; }
    .stat-label { font-size: 12px; color: #888; margin-top: 4px; }

    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
    .modal { background: #fff; border-radius: 14px; padding: 24px; width: 90%; max-width: 480px; box-shadow: 0 8px 32px rgba(0,0,0,.2); }
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
    <h1>Chrnova 课件管理</h1>
    <div class="form-group">
      <label>Access Token</label>
      <input type="password" id="tokenInput" placeholder="输入管理令牌">
    </div>
    <div style="text-align:center"><button class="btn-primary" onclick="login()">登录</button></div>
  </div>

  <div id="mainView" class="container hidden">
    <h1>Chrnova 课件管理</h1>

    <div class="stat-bar">
      <div class="stat"><div class="stat-num" id="totalCount">-</div><div class="stat-label">总课件</div></div>
      <div class="stat"><div class="stat-num" id="totalDownloads">-</div><div class="stat-label">总下载</div></div>
      <div class="stat"><div class="stat-num" id="totalFolders">-</div><div class="stat-label">文件夹</div></div>
    </div>

    <div class="toolbar">
      <input type="text" id="searchInput" placeholder="搜索课件..." oninput="debounceSearch()">
      <select id="folderFilter" onchange="loadPapers()"><option value="">全部文件夹</option></select>
      <button class="btn-primary" onclick="showUpload()">+ 上传</button>
    </div>

    <div id="paperList" class="paper-list"></div>
    <div id="emptyState" class="empty hidden">没有课件</div>
  </div>

  <!-- Upload Modal -->
  <div id="uploadModal" class="modal-overlay hidden" onclick="if(event.target===this)this.classList.add('hidden')">
    <div class="modal">
      <h2>上传课件</h2>
      <div class="form-group">
        <label>文件</label>
        <input type="file" id="uploadFile" accept=".pdf,.doc,.docx">
      </div>
      <div class="form-group">
        <label>标题</label>
        <input type="text" id="uploadTitle" placeholder="课件标题">
      </div>
      <div class="form-group">
        <label>文件夹</label>
        <input type="text" id="uploadFolder" placeholder="例如: 2024春季" list="folderSuggestions">
        <datalist id="folderSuggestions"></datalist>
      </div>
      <div class="modal-actions">
        <button class="btn-ghost" onclick="closeModal('uploadModal')">取消</button>
        <button class="btn-primary" id="uploadBtn" onclick="doUpload()">上传</button>
      </div>
    </div>
  </div>

  <!-- Detail Modal -->
  <div id="detailModal" class="modal-overlay hidden" onclick="if(event.target===this)this.classList.add('hidden')">
    <div class="modal">
      <h2 id="detailTitle">-</h2>
      <div id="detailBody" style="font-size:14px; line-height:1.8; color:#555;"></div>
      <div class="modal-actions">
        <button class="btn-ghost" onclick="closeModal('detailModal')">关闭</button>
      </div>
    </div>
  </div>

  <script>
    let token = '';
    let allPapers = [];
    let searchTimer = null;

    function login() {
      token = document.getElementById('tokenInput').value;
      if (!token) return;
      apiFetch('/admin/api/folders')
        .then(r => r.json())
        .then(folders => {
          document.getElementById('loginView').classList.add('hidden');
          document.getElementById('mainView').classList.remove('hidden');
          const sel = document.getElementById('folderFilter');
          const dl = document.getElementById('folderSuggestions');
          folders.forEach(f => {
            sel.add(new Option(f, f));
            dl.appendChild(new Option(f, f));
          });
          loadPapers();
        })
        .catch(() => alert('登录失败'));
    }

    async function apiFetch(url, opts = {}) {
      const res = await fetch(url, {
        ...opts,
        headers: { 'Authorization': 'Bearer ' + token, ...(opts.headers || {}) }
      });
      if (res.status === 401) { alert('令牌失效'); throw new Error('Unauthorized'); }
      if (!res.ok) throw new Error(await res.text());
      return res;
    }

    async function loadPapers() {
      const q = document.getElementById('searchInput').value;
      const folder = document.getElementById('folderFilter').value;
      const params = new URLSearchParams();
      if (q) params.set('q', q);
      if (folder) params.set('folder', folder);

      try {
        const res = await apiFetch('/admin/api/papers?' + params);
        allPapers = await res.json();
        renderPapers();
        updateStats();
      } catch (e) { console.error(e); }
    }

    function renderPapers() {
      const list = document.getElementById('paperList');
      const empty = document.getElementById('emptyState');
      if (!allPapers.length) { list.innerHTML = ''; empty.classList.remove('hidden'); return; }
      empty.classList.add('hidden');
      list.innerHTML = '';
      allPapers.forEach(p => {
        const size = p.size > 1048576 ? (p.size / 1048576).toFixed(1) + ' MB' : (p.size / 1024).toFixed(0) + ' KB';
        const date = new Date(p.created_at * 1000).toLocaleDateString('zh-CN');
        const card = document.createElement('div');
        card.className = 'paper-card';
        card.innerHTML =
          '<div class="paper-info">' +
            '<div class="paper-title">' + esc(p.title) + '</div>' +
            '<div class="paper-meta">' +
              '<span>' + esc(p.folder) + '</span>' +
              '<span>' + size + '</span>' +
              '<span>' + p.downloads + ' 次下载</span>' +
              '<span>' + date + '</span>' +
            '</div>' +
          '</div>' +
          '<div class="paper-actions">' +
            '<button class="btn-danger btn-sm">删除</button>' +
          '</div>';
        card.querySelector('.paper-info').addEventListener('click', function() { showDetail(p.id); });
        card.querySelector('.btn-danger').addEventListener('click', function() { confirmDelete(p.id, p.title); });
        list.appendChild(card);
      });
    }

    function updateStats() {
      document.getElementById('totalCount').textContent = allPapers.length;
      document.getElementById('totalDownloads').textContent = allPapers.reduce((s, p) => s + p.downloads, 0);
      const folders = new Set(allPapers.map(p => p.folder));
      document.getElementById('totalFolders').textContent = folders.size;
    }

    function debounceSearch() { clearTimeout(searchTimer); searchTimer = setTimeout(loadPapers, 300); }

    function showUpload() {
      document.getElementById('uploadModal').classList.remove('hidden');
      document.getElementById('uploadFile').value = '';
      document.getElementById('uploadTitle').value = '';
      document.getElementById('uploadFolder').value = '';
    }

    async function doUpload() {
      const file = document.getElementById('uploadFile').files[0];
      const title = document.getElementById('uploadTitle').value;
      const folder = document.getElementById('uploadFolder').value;
      if (!file || !title || !folder) { alert('请填写所有字段'); return; }

      const btn = document.getElementById('uploadBtn');
      btn.disabled = true; btn.textContent = '上传中...';

      try {
        const fd = new FormData();
        fd.append('file', file);
        fd.append('title', title);
        fd.append('folder', folder);
        await apiFetch('/admin/api/upload', { method: 'POST', body: fd });
        closeModal('uploadModal');
        toast('上传成功', true);
        loadPapers();
      } catch (e) { toast('上传失败: ' + e.message, false); }
      finally { btn.disabled = false; btn.textContent = '上传'; }
    }

    function confirmDelete(id, title) {
      if (!confirm('确定删除「' + title + '」？此操作不可撤销。')) return;
      apiFetch('/admin/api/papers/' + id, { method: 'DELETE' })
        .then(() => { toast('已删除', true); loadPapers(); })
        .catch(e => toast('删除失败', false));
    }

    async function showDetail(id) {
      try {
        const res = await apiFetch('/admin/api/papers/' + id);
        const p = await res.json();
        document.getElementById('detailTitle').textContent = p.title;
        const size = p.size > 1048576 ? (p.size / 1048576).toFixed(1) + ' MB' : (p.size / 1024).toFixed(0) + ' KB';
        document.getElementById('detailBody').innerHTML =
          '<b>文件夹:</b> ' + esc(p.folder) + '<br>' +
          '<b>大小:</b> ' + size + '<br>' +
          '<b>下载量:</b> ' + p.downloads + '<br>' +
          '<b>上传设备:</b> ' + esc(p.device_id || '-') + '<br>' +
          '<b>上传时间:</b> ' + new Date(p.created_at * 1000).toLocaleString('zh-CN') + '<br>' +
          '<b>路径:</b> <code style="font-size:12px; background:#f5f5f5; padding:2px 6px; border-radius:4px;">' + esc(p.path) + '</code>';
        document.getElementById('detailModal').classList.remove('hidden');
      } catch (e) { toast('加载失败', false); }
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

    document.getElementById('tokenInput').addEventListener('keypress', e => { if (e.key === 'Enter') login(); });
  </script>
</body>
</html>`;
}

export default app;
