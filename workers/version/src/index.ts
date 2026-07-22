import { Hono } from 'hono';
import { cors } from 'hono/cors';

interface Env {
  VERSION_KV: KVNamespace;
  AUTH_TOKEN: string;
}

interface VersionInfo {
  version: string;
  changelog: string;
  updatedAt: string;
}

const app = new Hono<{ Bindings: Env }>();

app.use('*', cors());

// Public: Get version.json
app.get('/version.json', async (c) => {
  const data = await c.env.VERSION_KV.get('latest', 'json');
  if (!data) {
    return c.json({ version: '0.0.0', changelog: '', updatedAt: '' });
  }
  return c.json(data);
});

// Protected: Update version
app.post('/api/version', async (c) => {
  const token = c.req.header('Authorization')?.replace('Bearer ', '');
  if (token !== c.env.AUTH_TOKEN) {
    return c.json({ error: 'Unauthorized' }, 401);
  }

  const body = await c.req.json<{ version: string; changelog: string }>();
  if (!body.version || typeof body.version !== 'string') {
    return c.json({ error: 'version is required' }, 400);
  }

  const info: VersionInfo = {
    version: body.version,
    changelog: body.changelog || '',
    updatedAt: new Date().toISOString(),
  };

  await c.env.VERSION_KV.put('latest', JSON.stringify(info));
  return c.json({ ok: true, data: info });
});

// Protected: Get version (for web UI)
app.get('/api/version', async (c) => {
  const token = c.req.header('Authorization')?.replace('Bearer ', '');
  if (token !== c.env.AUTH_TOKEN) {
    return c.json({ error: 'Unauthorized' }, 401);
  }

  const data = await c.env.VERSION_KV.get('latest', 'json');
  return c.json(data || { version: '', changelog: '', updatedAt: '' });
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
          <label>更新日志</label>
          <textarea id="changelogInput" placeholder="输入更新内容..."></textarea>
        </div>
        <button id="saveBtn" onclick="save()">保存</button>
        <div id="status" class="status"></div>
      </div>

      <div class="preview">
        <div class="preview-label">version.json 预览</div>
        <code id="preview">{\n  "version": "",\n  "changelog": ""\n}</code>
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
        document.getElementById('changelogInput').value = data.changelog || '';
        updatePreview();
      } catch (e) {
        alert('登录失败，请检查令牌');
      }
    }

    function updatePreview() {
      const version = document.getElementById('versionInput').value;
      const changelog = document.getElementById('changelogInput').value;
      const obj = { version, changelog };
      document.getElementById('preview').textContent = JSON.stringify(obj, null, 2);
    }

    document.getElementById('versionInput').addEventListener('input', updatePreview);
    document.getElementById('changelogInput').addEventListener('input', updatePreview);

    async function save() {
      const version = document.getElementById('versionInput').value;
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
          body: JSON.stringify({ version, changelog })
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

    // Enter key to login
    document.getElementById('tokenInput').addEventListener('keypress', (e) => {
      if (e.key === 'Enter') login();
    });
  </script>
</body>
</html>`;
}

export default app;
