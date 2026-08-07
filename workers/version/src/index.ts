import { Hono } from 'hono';
import { cors } from 'hono/cors';

interface Env {
  VERSION_KV: KVNamespace;
  AUTH_TOKEN: string;
}

interface VersionInfo {
  version: string;
  url: string;
  changelog: string;
  updatedAt: string;
}

const DEFAULT_URL = 'https://pan.quark.cn/s/2326de687ab1?pwd=E97u';

/**
 * 更新包下载地址的域名白名单。
 * 更新地址会被下发给全体客户端，若不加限制，一旦 AUTH_TOKEN 泄漏，
 * 攻击者即可向所有用户推送任意（恶意）安装包 —— 属供应链攻击。
 * 新增分发渠道时在此登记，仅允许 HTTPS。
 */
const ALLOWED_URL_HOSTS = [
  'pan.quark.cn',
  'github.com'
];

/** 版本号格式：1~4 段数字，可带 -beta.1 之类的后缀 */
const VERSION_PATTERN = /^\d+(\.\d+){0,3}(-[0-9A-Za-z.-]+)?$/;

const MAX_CHANGELOG_LENGTH = 5000;

/** 写接口限流：同一 IP 60 秒内最多 20 次 */
const WRITE_RATE_LIMIT = 20;
const WRITE_RATE_WINDOW_SEC = 60;

const app = new Hono<{ Bindings: Env }>();

/**
 * 恒定时间字符串比较，避免通过响应耗时逐字节爆破令牌。
 */
function timingSafeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return diff === 0;
}

/**
 * 校验管理令牌。
 * 注意 `!expected`：若 AUTH_TOKEN 未通过 `wrangler secret put` 配置，
 * env 值为 undefined，而未带 Authorization 头时 token 同样是 undefined，
 * 直接用 `!==` 比较会让两者相等从而放行 —— 必须显式拒绝未配置的情况。
 */
function isAuthorized(c: any): boolean {
  const expected = c.env.AUTH_TOKEN;
  if (!expected) return false;
  const token = c.req.header('Authorization')?.replace('Bearer ', '')?.trim();
  if (!token) return false;
  return timingSafeEqual(token, expected);
}

function clientIp(c: any): string {
  return c.req.header('CF-Connecting-IP') || c.req.header('X-Forwarded-For') || 'unknown';
}

/**
 * 基于 KV 的滑动窗口限流。KV 最终一致，用于防滥用足够，不适合强一致场景。
 * 返回 true 表示放行。
 */
async function allowRequest(kv: KVNamespace, bucket: string, limit: number, windowSec: number): Promise<boolean> {
  const key = `rl:${bucket}:${Math.floor(Date.now() / 1000 / windowSec)}`;
  const current = parseInt((await kv.get(key)) || '0', 10) || 0;
  if (current >= limit) return false;
  await kv.put(key, String(current + 1), { expirationTtl: Math.max(60, windowSec * 2) });
  return true;
}

/** 校验下载地址：必须是 HTTPS 且落在白名单域名内 */
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

// 仅公开接口开放跨域；/api/* 由同源的管理页调用，无需放开 CORS。
app.use('/version.json', cors());

// Public: Get version.json
app.get('/version.json', async (c) => {
  const data = await c.env.VERSION_KV.get('latest', 'json');
  if (!data) {
    return c.json({ version: '0.0.0', url: DEFAULT_URL, changelog: '', updatedAt: '' });
  }
  return c.json(data);
});

// Protected: Update version
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

// Protected: Get version (for web UI)
app.get('/api/version', async (c) => {
  if (!isAuthorized(c)) {
    return c.json({ error: 'Unauthorized' }, 401);
  }

  const data = await c.env.VERSION_KV.get('latest', 'json');
  return c.json(data || { version: '', url: DEFAULT_URL, changelog: '', updatedAt: '' });
});

// Web UI
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
      } catch (e) {
        alert('登录失败，请检查令牌');
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
