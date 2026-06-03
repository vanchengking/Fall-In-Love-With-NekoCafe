CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  mobile_number VARCHAR(32) UNIQUE NOT NULL,
  role VARCHAR(24) NOT NULL DEFAULT 'customer',
  member_level VARCHAR(24) NOT NULL DEFAULT 'silver',
  points INTEGER NOT NULL DEFAULT 0,
  preferences TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS stores (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(96) NOT NULL,
  city VARCHAR(64) NOT NULL,
  address VARCHAR(255) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  open_time TIME NOT NULL DEFAULT '10:30',
  close_time TIME NOT NULL DEFAULT '22:30',
  latitude NUMERIC(10, 6),
  longitude NUMERIC(10, 6),
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS dining_tables (
  id BIGSERIAL PRIMARY KEY,
  store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
  code VARCHAR(32) NOT NULL,
  seats INTEGER NOT NULL CHECK (seats > 0),
  area VARCHAR(64) NOT NULL DEFAULT 'main',
  cat_zone BOOLEAN NOT NULL DEFAULT false,
  status VARCHAR(24) NOT NULL DEFAULT 'available',
  UNIQUE (store_id, code)
);

CREATE TABLE IF NOT EXISTS cats (
  id BIGSERIAL PRIMARY KEY,
  store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
  name VARCHAR(64) NOT NULL,
  breed VARCHAR(64) NOT NULL,
  personality_tags TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
  health_status VARCHAR(32) NOT NULL DEFAULT 'healthy',
  weight_kg NUMERIC(4, 2),
  last_vaccine_at DATE,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS menu_items (
  id BIGSERIAL PRIMARY KEY,
  store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
  name VARCHAR(96) NOT NULL,
  category VARCHAR(48) NOT NULL,
  price_cents INTEGER NOT NULL CHECK (price_cents >= 0),
  tags TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
  status VARCHAR(24) NOT NULL DEFAULT 'available'
);

CREATE TABLE IF NOT EXISTS reservations (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  store_id BIGINT NOT NULL REFERENCES stores(id),
  table_id BIGINT REFERENCES dining_tables(id),
  recommended_cat_id BIGINT REFERENCES cats(id),
  reservation_date DATE NOT NULL,
  reservation_time TIME NOT NULL,
  party_size INTEGER NOT NULL CHECK (party_size > 0),
  status VARCHAR(24) NOT NULL DEFAULT 'booked',
  note TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_reservations_active_slot
  ON reservations (table_id, reservation_date, reservation_time)
  WHERE table_id IS NOT NULL AND status IN ('booked', 'seated');

CREATE TABLE IF NOT EXISTS orders (
  id BIGSERIAL PRIMARY KEY,
  reservation_id BIGINT REFERENCES reservations(id) ON DELETE SET NULL,
  user_id BIGINT NOT NULL REFERENCES users(id),
  store_id BIGINT NOT NULL REFERENCES stores(id),
  status VARCHAR(24) NOT NULL DEFAULT 'created',
  payment_status VARCHAR(24) NOT NULL DEFAULT 'sandbox_paid',
  total_cents INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS order_items (
  id BIGSERIAL PRIMARY KEY,
  order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  menu_item_id BIGINT NOT NULL REFERENCES menu_items(id),
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  unit_price_cents INTEGER NOT NULL CHECK (unit_price_cents >= 0)
);

CREATE TABLE IF NOT EXISTS cat_health_records (
  id BIGSERIAL PRIMARY KEY,
  cat_id BIGINT NOT NULL REFERENCES cats(id) ON DELETE CASCADE,
  recorded_at TIMESTAMP NOT NULL DEFAULT now(),
  weight_kg NUMERIC(4, 2),
  vaccine_note VARCHAR(255),
  interaction_note TEXT
);

CREATE TABLE IF NOT EXISTS operation_alerts (
  id BIGSERIAL PRIMARY KEY,
  store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
  level VARCHAR(16) NOT NULL DEFAULT 'info',
  title VARCHAR(96) NOT NULL,
  detail TEXT,
  resolved BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO users (id, name, mobile_number, role, member_level, points, preferences)
VALUES
  (1, '林小满', '13800000001', 'customer', 'gold', 1280, ARRAY['quiet', 'window', 'sweet', 'ragdoll']),
  (2, '高店员', '13800000002', 'staff', 'silver', 0, ARRAY[]::TEXT[]),
  (3, '葛店长', '13800000003', 'manager', 'silver', 0, ARRAY[]::TEXT[]),
  (4, '总部运营', '13800000004', 'operator', 'silver', 0, ARRAY[]::TEXT[]),
  (5, '卢猫咪管家', '13800000005', 'cat_keeper', 'silver', 0, ARRAY[]::TEXT[]),
  (6, '系统管理员', '13800000006', 'admin', 'silver', 0, ARRAY[]::TEXT[])
ON CONFLICT (id) DO NOTHING;

INSERT INTO stores (id, name, city, address, phone, latitude, longitude)
VALUES
  (1, 'NekoCafe 五道口店', '北京', '海淀区成府路 99 号', '010-88880001', 39.992821, 116.337388),
  (2, 'NekoCafe 西直门店', '北京', '西城区西直门外大街 18 号', '010-88880002', 39.940362, 116.353714)
ON CONFLICT (id) DO NOTHING;

INSERT INTO dining_tables (id, store_id, code, seats, area, cat_zone, status)
VALUES
  (1, 1, 'A01', 2, 'window', true, 'available'),
  (2, 1, 'A02', 4, 'window', true, 'available'),
  (3, 1, 'B01', 4, 'main', false, 'available'),
  (4, 1, 'C01', 6, 'party', false, 'available'),
  (5, 2, 'W01', 2, 'window', true, 'available'),
  (6, 2, 'M01', 4, 'main', false, 'available')
ON CONFLICT (id) DO NOTHING;

INSERT INTO cats (id, store_id, name, breed, personality_tags, health_status, weight_kg, last_vaccine_at)
VALUES
  (1, 1, '团子', '布偶', ARRAY['quiet', 'gentle', 'ragdoll'], 'healthy', 4.80, CURRENT_DATE - INTERVAL '90 days'),
  (2, 1, '拿铁', '英短', ARRAY['active', 'playful', 'photo'], 'healthy', 5.20, CURRENT_DATE - INTERVAL '120 days'),
  (3, 1, '芝麻', '狸花', ARRAY['curious', 'interactive'], 'observe', 4.10, CURRENT_DATE - INTERVAL '60 days'),
  (4, 2, '豆花', '金渐层', ARRAY['quiet', 'friendly'], 'healthy', 4.50, CURRENT_DATE - INTERVAL '80 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO menu_items (id, store_id, name, category, price_cents, tags, status)
VALUES
  (1, 1, '猫爪拿铁', 'drink', 3200, ARRAY['coffee', 'signature'], 'available'),
  (2, 1, '三文鱼能量碗', 'meal', 5800, ARRAY['healthy', 'salmon'], 'available'),
  (3, 1, '毛线球芝士蛋糕', 'dessert', 3600, ARRAY['sweet', 'cheese'], 'available'),
  (4, 1, '低敏燕麦奶', 'drink', 2800, ARRAY['vegan', 'oat'], 'available'),
  (5, 2, '布偶蓝莓松饼', 'dessert', 3900, ARRAY['sweet', 'blueberry'], 'available')
ON CONFLICT (id) DO NOTHING;

INSERT INTO reservations (id, user_id, store_id, table_id, recommended_cat_id, reservation_date, reservation_time, party_size, status, note)
VALUES
  (1, 1, 1, 1, 1, CURRENT_DATE + INTERVAL '1 day', '18:30', 2, 'booked', '希望靠窗，喜欢安静猫咪'),
  (2, 1, 1, 2, 2, CURRENT_DATE + INTERVAL '2 days', '12:00', 3, 'booked', '生日聚餐')
ON CONFLICT (id) DO NOTHING;

INSERT INTO cat_health_records (cat_id, weight_kg, vaccine_note, interaction_note)
VALUES
  (1, 4.80, '疫苗记录正常', '今日互动温和，适合安静桌位推荐'),
  (2, 5.20, '下次疫苗提醒已生成', '互动积极，适合拍照用户')
ON CONFLICT DO NOTHING;

INSERT INTO operation_alerts (store_id, level, title, detail)
VALUES
  (1, 'warning', '晚高峰桌位紧张', '18:00-20:00 已有多条预约，建议店员提前确认到店。')
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('users', 'id'), GREATEST((SELECT MAX(id) FROM users), 1), true);
SELECT setval(pg_get_serial_sequence('stores', 'id'), GREATEST((SELECT MAX(id) FROM stores), 1), true);
SELECT setval(pg_get_serial_sequence('dining_tables', 'id'), GREATEST((SELECT MAX(id) FROM dining_tables), 1), true);
SELECT setval(pg_get_serial_sequence('cats', 'id'), GREATEST((SELECT MAX(id) FROM cats), 1), true);
SELECT setval(pg_get_serial_sequence('menu_items', 'id'), GREATEST((SELECT MAX(id) FROM menu_items), 1), true);
SELECT setval(pg_get_serial_sequence('reservations', 'id'), GREATEST((SELECT MAX(id) FROM reservations), 1), true);
