import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { CORS_ORIGINS, clientIp, timingSafeEqual } from '../../shared/http';

interface Env {
  VERSION_KV: KVNamespace;
  AUTH_TOKEN: string;
  
  DAU_DB?: D1Database;
}

interface VersionInfo {
  version: string;
  url: string;
  changelog: string;
  updatedAt: string;
}


interface GrayConfig {
  version: string;
  url: string;
  changelog: string;
  uids: string[];
  updatedAt: string;
}

const DEFAULT_URL = 'https://pan.quark.cn/s/2326de687ab1?pwd=E97u';


const ALLOWED_URL_HOSTS = [
  'pan.quark.cn',
  'github.com'
];


const VERSION_PATTERN = /^\d+(\.\d+){0,3}(-[0-9A-Za-z.-]+)?$/;

const MAX_CHANGELOG_LENGTH = 5000;


const WRITE_RATE_LIMIT = 20;
const WRITE_RATE_WINDOW_SEC = 60;


const PING_RATE_LIMIT = 30;
const PING_RATE_WINDOW_SEC = 60;


const AID_PATTERN = /^[0-9A-Za-z]{16,64}$/;


const DAU_RETENTION_DAYS = 730;

const app = new Hono<{ Bindings: Env }>();
app.use('/version.json', cors({ origin: CORS_ORIGINS }));




function isAuthorized(c: any): boolean {
  const expected = c.env.AUTH_TOKEN;
  if (!expected) return false;
  const token = c.req.header('Authorization')?.replace('Bearer ', '')?.trim();
  if (!token) return false;
  return timingSafeEqual(token, expected);
}


async function allowRequest(kv: KVNamespace, bucket: string, limit: number, windowSec: number): Promise<boolean> {
  const key = `rl:${bucket}:${Math.floor(Date.now() / 1000 / windowSec)}`;
  const current = parseInt((await kv.get(key)) || '0', 10) || 0;
  if (current >= limit) return false;
  await kv.put(key, String(current + 1), { expirationTtl: Math.max(60, windowSec * 2) });
  return true;
}


function isAllowedUrl(raw: string): boolean {
  let parsed: URL;
  try {
    parsed = new URL(raw);
  } catch {
    return false;
  }
  if (parsed.protocol !== 'https:') return false;
  const host = parsed.hostname.toLowerCase();
  return ALLOWED_URL_HOSTS.some((allowed) => host === allowed || host.endsWith(`.${allowed}`));
}


async function sha256Hex(input: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(input));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('');
}


async function parseGrayTargets(raw: unknown): Promise<string[]> {
  const items = Array.isArray(raw)
    ? raw
    : typeof raw === 'string'
      ? raw.split(/[\s,;，；]+/)
      : [];
  const hashes = new Set<string>();
  for (const item of items) {
    if (typeof item !== 'string') continue;
    const trimmed = item.trim();
    if (!trimmed) continue;
    if (hashes.size >= 1000) break;
    hashes.add(await sha256Hex(trimmed));
  }
  return [...hashes];
}







app.get('/version.json', async (c) => {
  const data = await c.env.VERSION_KV.get('latest', 'json');
  const uid = (c.req.query('uid') || '').trim().toLowerCase();
  if (uid) {
    const gray = await c.env.VERSION_KV.get<GrayConfig>('gray', 'json');
    if (gray?.uids?.includes(uid)) {
      return c.json({
        version: gray.version,
        url: gray.url,
        changelog: gray.changelog,
        updatedAt: gray.updatedAt,
      });
    }
  }
  if (!data) {
    return c.json({ version: '0.0.0', url: DEFAULT_URL, changelog: '', updatedAt: '' });
  }
  return c.json(data);
});




app.post('/ping', async (c) => {
  const db = c.env.DAU_DB;
  if (!db) return c.body(null, 204);

  if (!(await allowRequest(c.env.VERSION_KV, `p:${clientIp(c)}`, PING_RATE_LIMIT, PING_RATE_WINDOW_SEC))) {
    return c.json({ error: '操作过于频繁，请稍后再试' }, 429);
  }

  let body: { aid?: unknown };
  try {
    body = await c.req.json();
  } catch {
    return c.json({ error: 'Invalid JSON body' }, 400);
  }
  const aid = typeof body.aid === 'string' ? body.aid.trim() : '';
  if (!AID_PATTERN.test(aid)) {
    return c.json({ error: 'Invalid aid' }, 400);
  }

  const day = new Date().toISOString().slice(0, 10);
  await db.prepare('INSERT INTO dau (day, aid) VALUES (?1, ?2) ON CONFLICT DO NOTHING').bind(day, aid).run();


  if (Math.random() < 0.01) {
    const cutoff = new Date(Date.now() - DAU_RETENTION_DAYS * 86_400_000).toISOString().slice(0, 10);
    await db.prepare('DELETE FROM dau WHERE day < ?1').bind(cutoff).run();
  }
  return c.body(null, 204);
});


app.get('/api/stats/dau', async (c) => {
  if (!isAuthorized(c)) {
    return c.json({ error: 'Unauthorized' }, 401);
  }
  const db = c.env.DAU_DB;
  if (!db) return c.json({ error: 'DAU 数据库未配置' }, 503);

  const days = Math.min(Math.max(Number.parseInt(c.req.query('days') || '30', 10) || 30, 1), 365);
  const since = new Date(Date.now() - days * 86_400_000).toISOString().slice(0, 10);
  const result = await db
    .prepare('SELECT day, COUNT(*) AS count FROM dau WHERE day >= ?1 GROUP BY day ORDER BY day DESC LIMIT ?2')
    .bind(since, days)
    .all<{ day: string; count: number }>();
  return c.json({ ok: true, data: result.results ?? [] });
});


app.post('/api/version', async (c) => {
  if (!(await allowRequest(c.env.VERSION_KV, `w:${clientIp(c)}`, WRITE_RATE_LIMIT, WRITE_RATE_WINDOW_SEC))) {
    return c.json({ error: '操作过于频繁，请稍后再试' }, 429);
  }
  if (!isAuthorized(c)) {
    return c.json({ error: 'Unauthorized' }, 401);
  }

  let body: { version?: string; url?: string; changelog?: string };
  try {
    body = await c.req.json();
  } catch {
    return c.json({ error: 'Invalid JSON body' }, 400);
  }

  const version = (body.version || '').trim();
  if (!version || !VERSION_PATTERN.test(version)) {
    return c.json({ error: 'version 格式不合法（示例：1.2.3 或 1.2.3-beta.1）' }, 400);
  }

  const url = (body.url || '').trim() || DEFAULT_URL;
  if (!isAllowedUrl(url)) {
    return c.json(
      { error: `下载地址必须是 HTTPS 且属于白名单域名：${ALLOWED_URL_HOSTS.join(', ')}` },
      400
    );
  }

  const changelog = (body.changelog || '').slice(0, MAX_CHANGELOG_LENGTH);

  const info: VersionInfo = {
    version,
    url,
    changelog,
    updatedAt: new Date().toISOString(),
  };

  await c.env.VERSION_KV.put('latest', JSON.stringify(info));
  return c.json({ ok: true, data: info });
});


app.get('/api/version', async (c) => {
  if (!isAuthorized(c)) {
    return c.json({ error: 'Unauthorized' }, 401);
  }

  const data = await c.env.VERSION_KV.get('latest', 'json');
  return c.json(data || { version: '', url: DEFAULT_URL, changelog: '', updatedAt: '' });
});


app.get('/api/gray', async (c) => {
  if (!isAuthorized(c)) {
    return c.json({ error: 'Unauthorized' }, 401);
  }
  const data = await c.env.VERSION_KV.get<GrayConfig>('gray', 'json');
  return c.json({
    version: data?.version || '',
    url: data?.url || '',
    changelog: data?.changelog || '',
    count: data?.uids?.length || 0,
    updatedAt: data?.updatedAt || '',
  });
});


app.post('/api/gray', async (c) => {
  if (!(await allowRequest(c.env.VERSION_KV, `w:${clientIp(c)}`, WRITE_RATE_LIMIT, WRITE_RATE_WINDOW_SEC))) {
    return c.json({ error: '操作过于频繁，请稍后再试' }, 429);
  }
  if (!isAuthorized(c)) {
    return c.json({ error: 'Unauthorized' }, 401);
  }

  let body: { version?: string; url?: string; changelog?: string; userNos?: string[] | string };
  try {
    body = await c.req.json();
  } catch {
    return c.json({ error: 'Invalid JSON body' }, 400);
  }

  const version = (body.version || '').trim();
  if (!version || !VERSION_PATTERN.test(version)) {
    return c.json({ error: 'version 格式不合法（示例：1.2.3 或 1.2.3-beta.1）' }, 400);
  }

  const url = (body.url || '').trim() || DEFAULT_URL;
  if (!isAllowedUrl(url)) {
    return c.json(
      { error: `下载地址必须是 HTTPS 且属于白名单域名：${ALLOWED_URL_HOSTS.join(', ')}` },
      400
    );
  }

  const changelog = (body.changelog || '').slice(0, MAX_CHANGELOG_LENGTH);
  const uids = await parseGrayTargets(body.userNos);

  const info: GrayConfig = {
    version,
    url,
    changelog,
    uids,
    updatedAt: new Date().toISOString(),
  };

  await c.env.VERSION_KV.put('gray', JSON.stringify(info));
  return c.json({ ok: true, data: { ...info, count: uids.length } });
});


app.delete('/api/gray', async (c) => {
  if (!isAuthorized(c)) {
    return c.json({ error: 'Unauthorized' }, 401);
  }
  await c.env.VERSION_KV.delete('gray');
  return c.json({ ok: true });
});


app.get('/', (c) => {
  return c.html(getWebUI());
});

function getWebUI(): string {
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Chrnova 版本管理</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: #f5f5f5;
      color: #333;
      min-height: 100vh;
    }
    .container {
      max-width: 600px;
      margin: 0 auto;
      padding: 40px 20px;
    }
    h1 {
      font-size: 24px;
      font-weight: 600;
      margin-bottom: 32px;
      color: #1a1a1a;
    }
    .login-box, .form-box {
      background: white;
      border-radius: 12px;
      padding: 24px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }
    .form-group {
      margin-bottom: 20px;
    }
    label {
      display: block;
      font-size: 14px;
      font-weight: 500;
      margin-bottom: 8px;
      color: #555;
    }
    input, textarea {
      width: 100%;
      padding: 12px;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      font-size: 14px;
      transition: border-color 0.2s;
    }
    input:focus, textarea:focus {
      outline: none;
      border-color: #0066ff;
    }
    textarea {
      min-height: 120px;
      resize: vertical;
      font-family: inherit;
    }
    button {
      width: 100%;
      padding: 12px;
      background: #0066ff;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.2s;
    }
    button:hover { background: #0052cc; }
    button:disabled {
      background: #ccc;
      cursor: not-allowed;
    }
    .status {
      margin-top: 16px;
      padding: 12px;
      border-radius: 8px;
      font-size: 14px;
      display: none;
    }
    .status.success {
      display: block;
      background: #e6f7e6;
      color: #1a7a1a;
    }
    .status.error {
      display: block;
      background: #ffe6e6;
      color: #cc0000;
    }
    .current-version {
      margin-top: 24px;
      padding: 16px;
      background: #f8f9fa;
      border-radius: 8px;
    }
    .current-version h3 {
      font-size: 14px;
      color: #666;
      margin-bottom: 8px;
    }
    .version-display {
      font-size: 20px;
      font-weight: 600;
      color: #1a1a1a;
    }
    .updated-at {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
    }
    .preview {
      margin-top: 24px;
      padding: 16px;
      background: #1a1a1a;
      border-radius: 8px;
      color: #a0a0a0;
      font-family: 'SF Mono', Monaco, monospace;
      font-size: 12px;
      white-space: pre-wrap;
      word-break: break-all;
    }
    .preview-label {
      font-size: 12px;
      color: #666;
      margin-bottom: 8px;
    }
    .hidden { display: none; }
    .dau-row {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      padding: 3px 0;
    }
    .dau-day { color: #666; width: 84px; flex-shrink: 0; }
    .dau-bar-track {
      flex: 1;
      background: #eef1f5;
      border-radius: 4px;
      height: 14px;
      overflow: hidden;
    }
    .dau-bar { height: 100%; background: #0066ff; border-radius: 4px; }
    .dau-count { width: 56px; text-align: right; flex-shrink: 0; color: #333; font-variant-numeric: tabular-nums; }
    .dau-empty { font-size: 13px; color: #999; }
  </style>
</head>
<body>
  <div class="container">
    <h1>Chrnova 版本管理</h1>

    <!-- Login -->
    <div id="loginBox" class="login-box">
      <div class="form-group">
        <label>Access Token</label>
        <input type="password" id="tokenInput" placeholder="输入访问令牌">
      </div>
      <button onclick="login()">登录</button>
    </div>

    <!-- Main Form -->
    <div id="mainBox" class="form-box hidden">
      <div class="current-version">
        <h3>当前版本</h3>
        <div class="version-display" id="currentVersion">-</div>
        <div class="updated-at" id="updatedAt">-</div>
      </div>

      <div style="margin-top: 24px;">
        <div class="form-group">
          <label>版本号</label>
          <input type="text" id="versionInput" placeholder="例如: 1.2.0">
        </div>
        <div class="form-group">
          <label>下载链接</label>
          <input type="text" id="urlInput" placeholder="夸克网盘分享链接">
        </div>
        <div class="form-group">
          <label>更新日志</label>
          <textarea id="changelogInput" placeholder="输入更新内容..."></textarea>
        </div>
        <button id="saveBtn" onclick="save()">保存</button>
        <div id="status" class="status"></div>
      </div>

      <div class="current-version" style="margin-top: 24px;">
        <h3>灰度发布（指定学号）</h3>
        <div id="grayState" style="font-size: 13px; color: #666; margin-bottom: 16px;">-</div>
        <div class="form-group">
          <label>灰度版本号</label>
          <input type="text" id="grayVersionInput" placeholder="例如: 1.3.0-beta.1">
        </div>
        <div class="form-group">
          <label>灰度下载链接（留空则用正式版默认链接）</label>
          <input type="text" id="grayUrlInput" placeholder="">
        </div>
        <div class="form-group">
          <label>灰度更新日志</label>
          <textarea id="grayChangelogInput" placeholder="输入灰度版本更新内容..."></textarea>
        </div>
        <div class="form-group">
          <label>灰度名单（学号，逗号或换行分隔；留空保存 = 关闭灰度）</label>
          <textarea id="grayUserNosInput" placeholder="例如:&#10;2021081125,&#10;2021081126"></textarea>
        </div>
        <div style="display: flex; gap: 8px;">
          <button id="graySaveBtn" onclick="saveGray()" style="flex: 1; width: auto;">保存灰度</button>
          <button id="grayClearBtn" onclick="clearGray()" style="flex: 1; width: auto; background: #cc0000;">清除灰度</button>
        </div>
        <div id="grayStatusMsg" class="status"></div>
      </div>

      <div class="current-version" style="margin-top: 24px;">
        <h3>日活统计（近 30 天）</h3>
        <div id="dauList" class="dau-empty">加载中...</div>
      </div>

      <div class="preview">
        <div class="preview-label">version.json 预览</div>
        <code id="preview">{\n  "version": "",\n  "url": "",\n  "changelog": ""\n}</code>
      </div>
    </div>
  </div>

  <script>
    let authToken = '';

    function login() {
      authToken = document.getElementById('tokenInput').value;
      if (!authToken) return;
      loadVersion();
    }

    async function loadVersion() {
      try {
        const res = await fetch('/api/version', {
          headers: { 'Authorization': 'Bearer ' + authToken }
        });
        if (!res.ok) throw new Error('Unauthorized');
        const data = await res.json();

        document.getElementById('loginBox').classList.add('hidden');
        document.getElementById('mainBox').classList.remove('hidden');

        document.getElementById('currentVersion').textContent = data.version || '-';
        document.getElementById('updatedAt').textContent = data.updatedAt
          ? '更新于 ' + new Date(data.updatedAt).toLocaleString()
          : '-';
        document.getElementById('versionInput').value = data.version || '';
        document.getElementById('urlInput').value = data.url || '';
        document.getElementById('changelogInput').value = data.changelog || '';
        updatePreview();
        loadGray();
        loadDau();
      } catch (e) {
        alert('登录失败，请检查令牌');
      }
    }

    function updateGrayState(data) {
      const el = document.getElementById('grayState');
      if (data && data.version && Number(data.count) > 0) {
        el.textContent = '进行中：' + data.version + ' · ' + data.count
          + ' 名指定用户 · 更新于 ' + new Date(data.updatedAt).toLocaleString();
      } else {
        el.textContent = '未启用';
      }
    }

    async function loadGray() {
      try {
        const res = await fetch('/api/gray', {
          headers: { 'Authorization': 'Bearer ' + authToken }
        });
        if (!res.ok) return;
        const data = await res.json();
        document.getElementById('grayVersionInput').value = data.version || '';
        document.getElementById('grayUrlInput').value = data.url || '';
        document.getElementById('grayChangelogInput').value = data.changelog || '';
        document.getElementById('grayUserNosInput').value = '';
        updateGrayState(data);
      } catch (e) { }
    }

    async function saveGray() {
      const version = document.getElementById('grayVersionInput').value.trim();
      const url = document.getElementById('grayUrlInput').value.trim();
      const changelog = document.getElementById('grayChangelogInput').value;
      const userNos = document.getElementById('grayUserNosInput').value;

      if (!version) {
        showGrayStatus('请输入灰度版本号', false);
        return;
      }

      const btn = document.getElementById('graySaveBtn');
      btn.disabled = true;
      btn.textContent = '保存中...';

      try {
        const res = await fetch('/api/gray', {
          method: 'POST',
          headers: {
            'Authorization': 'Bearer ' + authToken,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ version, url, changelog, userNos })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Save failed');
        updateGrayState(data.data);
        showGrayStatus(data.data.count > 0 ? '灰度已保存（' + data.data.count + ' 名用户）' : '灰度已保存（名单为空，已关闭）', true);
      } catch (e) {
        showGrayStatus('保存失败: ' + e.message, false);
      } finally {
        btn.disabled = false;
        btn.textContent = '保存灰度';
      }
    }

    async function clearGray() {
      const btn = document.getElementById('grayClearBtn');
      btn.disabled = true;

      try {
        const res = await fetch('/api/gray', {
          method: 'DELETE',
          headers: { 'Authorization': 'Bearer ' + authToken }
        });
        if (!res.ok) throw new Error('Clear failed');
        ['grayVersionInput', 'grayUrlInput'].forEach((id) => { document.getElementById(id).value = ''; });
        document.getElementById('grayChangelogInput').value = '';
        document.getElementById('grayUserNosInput').value = '';
        updateGrayState(null);
        showGrayStatus('灰度已清除，全部用户回到正式版', true);
      } catch (e) {
        showGrayStatus('清除失败: ' + e.message, false);
      } finally {
        btn.disabled = false;
      }
    }

    function showGrayStatus(msg, success) {
      const el = document.getElementById('grayStatusMsg');
      el.textContent = msg;
      el.className = 'status ' + (success ? 'success' : 'error');
      setTimeout(() => { el.className = 'status'; }, 3000);
    }

    async function loadDau() {
      const el = document.getElementById('dauList');
      try {
        const res = await fetch('/api/stats/dau?days=30', {
          headers: { 'Authorization': 'Bearer ' + authToken }
        });
        if (res.status === 503) {
          el.textContent = '未配置 D1 数据库（见 wrangler.toml 说明）';
          return;
        }
        if (!res.ok) throw new Error('加载失败');
        const data = await res.json();
        const rows = data.data || [];
        if (!rows.length) {
          el.textContent = '暂无数据';
          return;
        }
        const max = Math.max.apply(null, rows.map(function (r) { return r.count; })) || 1;
        el.className = '';
        el.innerHTML = rows.map(function (r) {
          const width = Math.max(2, Math.round(r.count / max * 100));
          return '<div class="dau-row">'
            + '<span class="dau-day">' + String(r.day).slice(5) + '</span>'
            + '<div class="dau-bar-track"><div class="dau-bar" style="width:' + width + '%"></div></div>'
            + '<span class="dau-count">' + Number(r.count) + '</span>'
            + '</div>';
        }).join('');
      } catch (e) {
        el.textContent = '日活数据加载失败';
      }
    }

    function updatePreview() {
      const version = document.getElementById('versionInput').value;
      const url = document.getElementById('urlInput').value;
      const changelog = document.getElementById('changelogInput').value;
      const obj = { version, url, changelog };
      document.getElementById('preview').textContent = JSON.stringify(obj, null, 2);
    }

    document.getElementById('versionInput').addEventListener('input', updatePreview);
    document.getElementById('urlInput').addEventListener('input', updatePreview);
    document.getElementById('changelogInput').addEventListener('input', updatePreview);

    async function save() {
      const version = document.getElementById('versionInput').value;
      const url = document.getElementById('urlInput').value;
      const changelog = document.getElementById('changelogInput').value;

      if (!version) {
        showStatus('请输入版本号', false);
        return;
      }

      const btn = document.getElementById('saveBtn');
      btn.disabled = true;
      btn.textContent = '保存中...';

      try {
        const res = await fetch('/api/version', {
          method: 'POST',
          headers: {
            'Authorization': 'Bearer ' + authToken,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ version, url, changelog })
        });

        if (!res.ok) throw new Error('Save failed');
        const data = await res.json();

        document.getElementById('currentVersion').textContent = data.data.version;
        document.getElementById('updatedAt').textContent = '更新于 ' + new Date(data.data.updatedAt).toLocaleString();
        showStatus('保存成功', true);
      } catch (e) {
        showStatus('保存失败: ' + e.message, false);
      } finally {
        btn.disabled = false;
        btn.textContent = '保存';
      }
    }

    function showStatus(msg, success) {
      const el = document.getElementById('status');
      el.textContent = msg;
      el.className = 'status ' + (success ? 'success' : 'error');
      setTimeout(() => { el.className = 'status'; }, 3000);
    }

    document.getElementById('tokenInput').addEventListener('keypress', (e) => {
      if (e.key === 'Enter') login();
    });
  </script>
</body>
</html>`;
}

export default app;
