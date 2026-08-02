-- Chrnova 课程评价 D1 表结构
-- 执行： wrangler d1 execute chrnova-evaluate-db --local --file=src/schema.sql
--       wrangler d1 execute chrnova-evaluate-db --remote --file=src/schema.sql

CREATE TABLE IF NOT EXISTS evaluations (
  id           TEXT PRIMARY KEY,
  course_name  TEXT NOT NULL,
  teacher      TEXT NOT NULL DEFAULT '',
  rating       INTEGER NOT NULL DEFAULT 0,
  content      TEXT NOT NULL DEFAULT '',
  anonymous    INTEGER NOT NULL DEFAULT 0,
  author       TEXT NOT NULL DEFAULT '',
  user_no      TEXT NOT NULL DEFAULT '',
  device_id    TEXT NOT NULL DEFAULT '',
  likes        INTEGER NOT NULL DEFAULT 0,
  status       TEXT NOT NULL DEFAULT 'pending',
  created_at   INTEGER NOT NULL,
  updated_at   INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_evaluations_course ON evaluations(course_name);
CREATE INDEX IF NOT EXISTS idx_evaluations_status ON evaluations(status);
CREATE INDEX IF NOT EXISTS idx_evaluations_created ON evaluations(created_at);

-- 点赞记录：同一设备对同一评价只能点赞一次（用于切换点赞状态）
CREATE TABLE IF NOT EXISTS evaluation_likes (
  device_id     TEXT NOT NULL,
  evaluation_id TEXT NOT NULL,
  created_at    INTEGER NOT NULL,
  PRIMARY KEY (device_id, evaluation_id)
);

CREATE INDEX IF NOT EXISTS idx_likes_eval ON evaluation_likes(evaluation_id);
