# Chrnova Version Worker

Cloudflare Worker for Chrnova app version update checking.

## 功能

- `GET /version.json` - 公开接口，返回版本信息
- `GET /` - Web UI，用于管理版本
- `POST /api/version` - 受保护接口，更新版本信息
- `GET /api/version` - 受保护接口，获取当前版本

## 部署

1. 安装依赖：
   ```bash
   npm install
   ```

2. 创建 KV 命名空间：
   ```bash
   npx wrangler kv:namespace create VERSION_KV
   ```

3. 更新 `wrangler.toml` 中的 KV ID

4. 设置 AUTH_TOKEN：
   ```bash
   npx wrangler secret put AUTH_TOKEN
   ```

5. 部署：
   ```bash
   npm run deploy
   ```

## 使用

1. 访问 Worker URL（如 `https://chrnova-version-worker.restarhalf.workers.dev`）
2. 输入 AUTH_TOKEN 登录
3. 填写版本号和更新日志
4. 点击保存

App 会自动检查 `/version.json` 获取最新版本信息。
