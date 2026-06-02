-- 数据库初始化脚本
-- 建议使用 Flyway / Liquibase 管理版本

CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(64) UNIQUE NOT NULL,
  created_at TIMESTAMP DEFAULT now()
);
