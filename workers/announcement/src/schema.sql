-- Chrnova 公告 D1 表结构
-- 执行： wrangler d1 execute chrnova-announcement-db --local --file=src/schema.sql
--       wrangler d1 execute chrnova-announcement-db --remote --file=src/schema.sql

CREATE TABLE IF NOT EXISTS announcements (
  id         TEXT PRIMARY KEY,
  title      TEXT NOT NULL,
  content    TEXT NOT NULL DEFAULT '',
  priority   INTEGER NOT NULL DEFAULT 0,   -- 0=普通 1=重要（客户端展示徽标）
  pinned     INTEGER NOT NULL DEFAULT 0,   -- 1=置顶（列表优先展示）
  status     TEXT NOT NULL DEFAULT 'published',  -- published / draft
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_announcements_status_created
  ON announcements(status, created_at DESC);
