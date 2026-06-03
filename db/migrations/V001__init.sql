-- NekoCafe 智慧餐饮预约平台初始化脚本
-- MySQL 8+

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  mobile_number VARCHAR(32) UNIQUE NOT NULL,
  role VARCHAR(24) NOT NULL DEFAULT 'customer',
  member_level VARCHAR(24) NOT NULL DEFAULT 'silver',
  points INT NOT NULL DEFAULT 0,
  preferences JSON NOT NULL DEFAULT (JSON_ARRAY()),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS stores (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(96) NOT NULL,
  city VARCHAR(64) NOT NULL,
  address VARCHAR(255) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  open_time TIME NOT NULL DEFAULT '10:30:00',
  close_time TIME NOT NULL DEFAULT '22:30:00',
  latitude DECIMAL(10, 6),
  longitude DECIMAL(10, 6),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS dining_tables (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,
  seats INT NOT NULL CHECK (seats > 0),
  area VARCHAR(64) NOT NULL DEFAULT 'main',
  cat_zone BOOLEAN NOT NULL DEFAULT false,
  status VARCHAR(24) NOT NULL DEFAULT 'available',
  UNIQUE KEY uq_dining_tables_store_code (store_id, code),
  CONSTRAINT fk_dining_tables_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS cats (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  breed VARCHAR(64) NOT NULL,
  personality_tags JSON NOT NULL DEFAULT (JSON_ARRAY()),
  health_status VARCHAR(32) NOT NULL DEFAULT 'healthy',
  weight_kg DECIMAL(4, 2),
  last_vaccine_at DATE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_cats_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS menu_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  name VARCHAR(96) NOT NULL,
  category VARCHAR(48) NOT NULL,
  price_cents INT NOT NULL CHECK (price_cents >= 0),
  tags JSON NOT NULL DEFAULT (JSON_ARRAY()),
  status VARCHAR(24) NOT NULL DEFAULT 'available',
  CONSTRAINT fk_menu_items_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS reservations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  table_id BIGINT,
  recommended_cat_id BIGINT,
  reservation_date DATE NOT NULL,
  reservation_time TIME NOT NULL,
  party_size INT NOT NULL CHECK (party_size > 0),
  status VARCHAR(24) NOT NULL DEFAULT 'booked',
  note TEXT,
  active_slot_key VARCHAR(160)
    GENERATED ALWAYS AS (
      CASE
        WHEN table_id IS NOT NULL AND status IN ('booked', 'seated')
        THEN CONCAT(table_id, '#', reservation_date, '#', reservation_time)
        ELSE NULL
      END
    ) STORED,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_reservations_active_slot (active_slot_key),
  KEY idx_reservations_store_date (store_id, reservation_date),
  CONSTRAINT fk_reservations_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_reservations_store FOREIGN KEY (store_id) REFERENCES stores(id),
  CONSTRAINT fk_reservations_table FOREIGN KEY (table_id) REFERENCES dining_tables(id),
  CONSTRAINT fk_reservations_cat FOREIGN KEY (recommended_cat_id) REFERENCES cats(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id BIGINT,
  user_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'created',
  payment_status VARCHAR(24) NOT NULL DEFAULT 'sandbox_paid',
  total_cents INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_orders_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE SET NULL,
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  menu_item_id BIGINT NOT NULL,
  quantity INT NOT NULL CHECK (quantity > 0),
  unit_price_cents INT NOT NULL CHECK (unit_price_cents >= 0),
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_items_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS cat_health_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  cat_id BIGINT NOT NULL,
  recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  weight_kg DECIMAL(4, 2),
  vaccine_note VARCHAR(255),
  interaction_note TEXT,
  CONSTRAINT fk_cat_health_records_cat FOREIGN KEY (cat_id) REFERENCES cats(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS operation_alerts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  level VARCHAR(16) NOT NULL DEFAULT 'info',
  title VARCHAR(96) NOT NULL,
  detail TEXT,
  resolved BOOLEAN NOT NULL DEFAULT false,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_operation_alerts_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO users (id, name, mobile_number, role, member_level, points, preferences)
VALUES
  (1, '林小满', '13800000001', 'customer', 'gold', 1280, JSON_ARRAY('quiet', 'window', 'sweet', 'ragdoll')),
  (2, '高店员', '13800000002', 'staff', 'silver', 0, JSON_ARRAY()),
  (3, '葛店长', '13800000003', 'manager', 'silver', 0, JSON_ARRAY()),
  (4, '总部运营', '13800000004', 'operator', 'silver', 0, JSON_ARRAY()),
  (5, '卢猫咪管家', '13800000005', 'cat_keeper', 'silver', 0, JSON_ARRAY()),
  (6, '系统管理员', '13800000006', 'admin', 'silver', 0, JSON_ARRAY());

INSERT IGNORE INTO stores (id, name, city, address, phone, latitude, longitude)
VALUES
  (1, 'NekoCafe 五道口店', '北京', '海淀区成府路 99 号', '010-88880001', 39.992821, 116.337388),
  (2, 'NekoCafe 西直门店', '北京', '西城区西直门外大街 18 号', '010-88880002', 39.940362, 116.353714);

INSERT IGNORE INTO dining_tables (id, store_id, code, seats, area, cat_zone, status)
VALUES
  (1, 1, 'A01', 2, 'window', true, 'available'),
  (2, 1, 'A02', 4, 'window', true, 'available'),
  (3, 1, 'B01', 4, 'main', false, 'available'),
  (4, 1, 'C01', 6, 'party', false, 'available'),
  (5, 2, 'W01', 2, 'window', true, 'available'),
  (6, 2, 'M01', 4, 'main', false, 'available');

INSERT IGNORE INTO cats (id, store_id, name, breed, personality_tags, health_status, weight_kg, last_vaccine_at)
VALUES
  (1, 1, '团子', '布偶', JSON_ARRAY('quiet', 'gentle', 'ragdoll'), 'healthy', 4.80, CURRENT_DATE - INTERVAL 90 DAY),
  (2, 1, '拿铁', '英短', JSON_ARRAY('active', 'playful', 'photo'), 'healthy', 5.20, CURRENT_DATE - INTERVAL 120 DAY),
  (3, 1, '芝麻', '狸花', JSON_ARRAY('curious', 'interactive'), 'observe', 4.10, CURRENT_DATE - INTERVAL 60 DAY),
  (4, 2, '豆花', '金渐层', JSON_ARRAY('quiet', 'friendly'), 'healthy', 4.50, CURRENT_DATE - INTERVAL 80 DAY);

INSERT IGNORE INTO menu_items (id, store_id, name, category, price_cents, tags, status)
VALUES
  (1, 1, '猫爪拿铁', 'drink', 3200, JSON_ARRAY('coffee', 'signature'), 'available'),
  (2, 1, '三文鱼能量碗', 'meal', 5800, JSON_ARRAY('healthy', 'salmon'), 'available'),
  (3, 1, '毛线球芝士蛋糕', 'dessert', 3600, JSON_ARRAY('sweet', 'cheese'), 'available'),
  (4, 1, '低敏燕麦奶', 'drink', 2800, JSON_ARRAY('vegan', 'oat'), 'available'),
  (5, 2, '布偶蓝莓松饼', 'dessert', 3900, JSON_ARRAY('sweet', 'blueberry'), 'available');

INSERT IGNORE INTO reservations (id, user_id, store_id, table_id, recommended_cat_id, reservation_date, reservation_time, party_size, status, note)
VALUES
  (1, 1, 1, 1, 1, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), '18:30:00', 2, 'booked', '希望靠窗，喜欢安静猫咪'),
  (2, 1, 1, 2, 2, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), '12:00:00', 3, 'booked', '生日聚餐');

INSERT IGNORE INTO cat_health_records (id, cat_id, weight_kg, vaccine_note, interaction_note)
VALUES
  (1, 1, 4.80, '疫苗记录正常', '今日互动温和，适合安静桌位推荐'),
  (2, 2, 5.20, '下次疫苗提醒已生成', '互动积极，适合拍照用户');

INSERT IGNORE INTO operation_alerts (id, store_id, level, title, detail)
VALUES
  (1, 1, 'warning', '晚高峰桌位紧张', '18:00-20:00 已有多条预约，建议店员提前确认到店。');
