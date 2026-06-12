-- M1 会员积分流水（FR-MEMBER-002）：所有积分变更必须落流水，可追溯、防重复发放
-- MySQL 8+
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS point_transactions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  delta INT NOT NULL,
  balance_after INT NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_id BIGINT NULL,
  reason VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- source_id 非空时按 (source_type, source_id) 防重复发放；MySQL 唯一索引允许多行 NULL，
  -- 因此人工调整等无来源单据的流水可使用 NULL source_id 而不受约束限制
  UNIQUE KEY uq_point_transactions_source (source_type, source_id),
  KEY idx_point_transactions_user_created (user_id, created_at),
  CONSTRAINT fk_point_transactions_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
