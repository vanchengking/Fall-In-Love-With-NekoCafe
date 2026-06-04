-- NekoCafe 智慧餐饮预约平台 D-01 正式化升级脚本
-- 在 V001__init.sql 之上补齐 D-01 数据模型：扩展状态机、猫咪档案字段，
-- 新增 reservation_events / payment_transactions / reviews / vaccine_records / coupons / audit_logs，
-- 并补充索引与幂等种子数据。MySQL 8+，由 docker-entrypoint-initdb.d 在首次初始化时按文件名顺序执行。

-- ---------------------------------------------------------------------------
-- 1. 扩展猫咪档案字段（照片、生日、可互动状态）
-- ---------------------------------------------------------------------------
ALTER TABLE cats
  ADD COLUMN photo_url VARCHAR(255) NULL AFTER breed,
  ADD COLUMN birthday DATE NULL AFTER photo_url,
  ADD COLUMN interactive_status VARCHAR(24) NOT NULL DEFAULT 'interactive' AFTER health_status;

-- ---------------------------------------------------------------------------
-- 2. 预约状态机扩展为 created -> booked -> seated -> dining -> finished/cancelled/no_show
--    重建占位生成列，使 created/booked/seated/dining 视为占用桌位的活跃状态。
--    新增"同一用户同一门店同一时段"唯一约束（活跃状态下）。
-- ---------------------------------------------------------------------------
ALTER TABLE reservations DROP INDEX uq_reservations_active_slot;
ALTER TABLE reservations DROP COLUMN active_slot_key;

ALTER TABLE reservations
  ADD COLUMN active_slot_key VARCHAR(160)
    GENERATED ALWAYS AS (
      CASE
        WHEN table_id IS NOT NULL AND status IN ('created', 'booked', 'seated', 'dining')
        THEN CONCAT(table_id, '#', reservation_date, '#', reservation_time)
        ELSE NULL
      END
    ) STORED;

ALTER TABLE reservations
  ADD COLUMN user_active_slot_key VARCHAR(200)
    GENERATED ALWAYS AS (
      CASE
        WHEN status IN ('created', 'booked', 'seated', 'dining')
        THEN CONCAT(user_id, '#', store_id, '#', reservation_date, '#', reservation_time)
        ELSE NULL
      END
    ) STORED;

ALTER TABLE reservations
  ADD UNIQUE KEY uq_reservations_active_slot (active_slot_key),
  ADD UNIQUE KEY uq_reservations_user_slot (user_active_slot_key),
  ADD KEY idx_reservations_user_store_date (user_id, store_id, reservation_date);

-- ---------------------------------------------------------------------------
-- 3. 预约状态变更事件记录（状态机审计）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservation_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id BIGINT NOT NULL,
  from_status VARCHAR(24) NULL,
  to_status VARCHAR(24) NOT NULL,
  actor_role VARCHAR(24) NULL,
  actor_user_id BIGINT NULL,
  note VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_reservation_events_reservation (reservation_id, created_at),
  CONSTRAINT fk_reservation_events_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 4. 支付沙箱流水
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_transactions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  reservation_id BIGINT NULL,
  amount_cents INT NOT NULL DEFAULT 0,
  channel VARCHAR(24) NOT NULL DEFAULT 'sandbox',
  status VARCHAR(24) NOT NULL DEFAULT 'paid',
  txn_ref VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_payment_transactions_ref (txn_ref),
  KEY idx_payment_transactions_order (order_id),
  CONSTRAINT fk_payment_transactions_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_payment_transactions_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 5. 顾客评价
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  cat_id BIGINT NULL,
  rating TINYINT NOT NULL DEFAULT 5 CHECK (rating BETWEEN 1 AND 5),
  content TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_reviews_store (store_id, created_at),
  CONSTRAINT fk_reviews_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE SET NULL,
  CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_reviews_store FOREIGN KEY (store_id) REFERENCES stores(id),
  CONSTRAINT fk_reviews_cat FOREIGN KEY (cat_id) REFERENCES cats(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 6. 疫苗接种记录（疫苗名称、接种日期、下次接种日期）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vaccine_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  cat_id BIGINT NOT NULL,
  vaccine_name VARCHAR(96) NOT NULL,
  vaccinated_at DATE NOT NULL,
  next_due_at DATE NULL,
  note VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_vaccine_records_cat (cat_id, next_due_at),
  CONSTRAINT fk_vaccine_records_cat FOREIGN KEY (cat_id) REFERENCES cats(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 7. 优惠券（可选）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS coupons (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(48) NOT NULL,
  title VARCHAR(96) NOT NULL,
  discount_cents INT NOT NULL DEFAULT 0,
  min_spend_cents INT NOT NULL DEFAULT 0,
  valid_from DATE NULL,
  valid_to DATE NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'active',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_coupons_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 8. 审计日志（可选）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor_user_id BIGINT NULL,
  actor_role VARCHAR(24) NULL,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(48) NULL,
  target_id VARCHAR(64) NULL,
  detail TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_audit_logs_target (target_type, target_id),
  KEY idx_audit_logs_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 9. 补充索引
-- ---------------------------------------------------------------------------
ALTER TABLE orders ADD KEY idx_orders_user_store (user_id, store_id, created_at);
ALTER TABLE cat_health_records ADD KEY idx_cat_health_records_cat (cat_id, recorded_at);

-- ---------------------------------------------------------------------------
-- 10. 幂等种子数据：完善猫咪档案 + 新增疫苗/评价/优惠券/支付样例
-- ---------------------------------------------------------------------------
UPDATE cats SET photo_url = CONCAT('/assets/cats/', id, '.png'),
                birthday = CURRENT_DATE - INTERVAL (365 * 2 + id * 30) DAY,
                interactive_status = CASE WHEN health_status = 'healthy' THEN 'interactive' ELSE 'rest' END
WHERE photo_url IS NULL;

INSERT IGNORE INTO vaccine_records (id, cat_id, vaccine_name, vaccinated_at, next_due_at, note)
VALUES
  (1, 1, '猫三联', CURRENT_DATE - INTERVAL 90 DAY, CURRENT_DATE + INTERVAL 275 DAY, '年度加强'),
  (2, 1, '狂犬疫苗', CURRENT_DATE - INTERVAL 90 DAY, CURRENT_DATE + INTERVAL 275 DAY, NULL),
  (3, 2, '猫三联', CURRENT_DATE - INTERVAL 120 DAY, CURRENT_DATE + INTERVAL 245 DAY, NULL),
  (4, 3, '猫三联', CURRENT_DATE - INTERVAL 60 DAY, CURRENT_DATE + INTERVAL 305 DAY, '观察中');

INSERT IGNORE INTO reviews (id, reservation_id, user_id, store_id, cat_id, rating, content)
VALUES
  (1, 1, 1, 1, 1, 5, '团子很安静，靠窗位置体验很好！'),
  (2, 2, 1, 1, 2, 4, '拿铁很爱拍照，甜品也不错。');

INSERT IGNORE INTO coupons (id, code, title, discount_cents, min_spend_cents, valid_from, valid_to, status)
VALUES
  (1, 'NEKO10', '新客立减 10 元', 1000, 5000, CURRENT_DATE - INTERVAL 7 DAY, CURRENT_DATE + INTERVAL 30 DAY, 'active'),
  (2, 'CATLOVER', '猫咪日 8 折券', 2000, 8000, CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE + INTERVAL 14 DAY, 'active');
