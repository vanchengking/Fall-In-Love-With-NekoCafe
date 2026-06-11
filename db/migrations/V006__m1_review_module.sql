-- 评价模块扩展：新增 reviews 表 + 补全字段
-- 适用于 MySQL 8+

-- 1. 创建 reviews 表
CREATE TABLE IF NOT EXISTS reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  order_id BIGINT,

  -- 评分维度
  rating              INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  food_rating         INT CHECK (food_rating BETWEEN 1 AND 5),
  service_rating      INT CHECK (service_rating BETWEEN 1 AND 5),
  environment_rating  INT CHECK (environment_rating BETWEEN 1 AND 5),
  cat_rating          INT CHECK (cat_rating BETWEEN 1 AND 5),

  content       TEXT,
  is_anonymous  BOOLEAN NOT NULL DEFAULT FALSE,

  -- 商家回复
  reply       TEXT,
  replied_at  DATETIME,

  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_reviews_user   FOREIGN KEY (user_id)  REFERENCES users(id)   ON DELETE CASCADE,
  CONSTRAINT fk_reviews_store  FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
  CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders(id)  ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. 为兼容旧代码：如果 reviews 表已存在但缺少字段，则逐个添加（安全可重复执行）
-- MySQL 不支持 IF NOT EXISTS 用于 ADD COLUMN，所以用存储过程包装
DROP PROCEDURE IF EXISTS add_review_columns;
DELIMITER $$
CREATE PROCEDURE add_review_columns()
BEGIN
  -- food_rating
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reviews' AND COLUMN_NAME = 'food_rating'
  ) THEN
    ALTER TABLE reviews ADD COLUMN food_rating INT CHECK (food_rating BETWEEN 1 AND 5);
  END IF;

  -- service_rating
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reviews' AND COLUMN_NAME = 'service_rating'
  ) THEN
    ALTER TABLE reviews ADD COLUMN service_rating INT CHECK (service_rating BETWEEN 1 AND 5);
  END IF;

  -- environment_rating
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reviews' AND COLUMN_NAME = 'environment_rating'
  ) THEN
    ALTER TABLE reviews ADD COLUMN environment_rating INT CHECK (environment_rating BETWEEN 1 AND 5);
  END IF;

  -- cat_rating
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reviews' AND COLUMN_NAME = 'cat_rating'
  ) THEN
    ALTER TABLE reviews ADD COLUMN cat_rating INT CHECK (cat_rating BETWEEN 1 AND 5);
  END IF;

  -- is_anonymous
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reviews' AND COLUMN_NAME = 'is_anonymous'
  ) THEN
    ALTER TABLE reviews ADD COLUMN is_anonymous BOOLEAN NOT NULL DEFAULT FALSE;
  END IF;

  -- reply
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reviews' AND COLUMN_NAME = 'reply'
  ) THEN
    ALTER TABLE reviews ADD COLUMN reply TEXT;
  END IF;

  -- replied_at
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reviews' AND COLUMN_NAME = 'replied_at'
  ) THEN
    ALTER TABLE reviews ADD COLUMN replied_at DATETIME;
  END IF;

  -- order_id
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reviews' AND COLUMN_NAME = 'order_id'
  ) THEN
    ALTER TABLE reviews ADD COLUMN order_id BIGINT;
    ALTER TABLE reviews ADD CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;
  END IF;
END$$
DELIMITER ;

CALL add_review_columns();
DROP PROCEDURE IF EXISTS add_review_columns;
