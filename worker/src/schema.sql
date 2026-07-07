CREATE TABLE IF NOT EXISTS papers (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  folder TEXT NOT NULL,
  path TEXT NOT NULL,
  size INTEGER DEFAULT 0,
  device_id TEXT NOT NULL,
  downloads INTEGER DEFAULT 0,
  created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_papers_folder ON papers(folder);
