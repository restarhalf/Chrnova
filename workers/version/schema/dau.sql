-- Chrnova 匿名日活统计表
-- aid 为客户端本地随机生成的设备标识，不含学号等个人信息；
-- 以 (UTC 日期, aid) 为主键去重，一天内多次心跳只计一次。
CREATE TABLE IF NOT EXISTS dau (
  day        TEXT NOT NULL,
  aid        TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
  PRIMARY KEY (day, aid)
);
