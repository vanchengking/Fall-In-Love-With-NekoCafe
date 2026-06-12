-- M2 门店/桌位种子人工验收脚本（FR-STORE-001 / FR-STORE-002）
-- 用法：mysql -u<user> -p --default-character-set=utf8mb4 <db> < db/verify_m2_seed.sql
-- 期望结果见各段注释；与 backend StoreSeedMigrationTest 的静态校验口径一致。

-- [1] 门店总数 38、城市数 12
SELECT COUNT(*) AS store_total, COUNT(DISTINCT city) AS city_total FROM stores;

-- [2] 城市分布（期望：北京5 上海5 广州4 深圳4 杭州3 成都3 南京3 武汉3 西安2 重庆2 苏州2 天津2）
SELECT city, COUNT(*) AS stores FROM stores GROUP BY city ORDER BY stores DESC, city;

-- [3] 必填字段缺失门店数（期望 0）：城市/地址/电话/营业时间/营业时间文本
SELECT COUNT(*) AS missing_field_stores FROM stores
WHERE name IS NULL OR name = '' OR city IS NULL OR city = ''
   OR address IS NULL OR address = '' OR phone IS NULL OR phone = ''
   OR open_time IS NULL OR close_time IS NULL
   OR business_hours_text IS NULL OR business_hours_text = '';

-- [4] 桌位总量（期望 152 张 / 560 座）与每店桌位数（期望 min=4, max=4）
SELECT COUNT(*) AS table_total, SUM(seats) AS seat_total FROM dining_tables;
SELECT MIN(cnt) AS min_tables_per_store, MAX(cnt) AS max_tables_per_store
FROM (SELECT store_id, COUNT(*) AS cnt FROM dining_tables GROUP BY store_id) per_store;

-- [5] 无桌位或无猫咪互动区桌位的门店（期望均为空集）
SELECT s.id, s.name FROM stores s
WHERE NOT EXISTS (SELECT 1 FROM dining_tables dt WHERE dt.store_id = s.id);
SELECT s.id, s.name FROM stores s
WHERE NOT EXISTS (SELECT 1 FROM dining_tables dt WHERE dt.store_id = s.id AND dt.cat_zone = 1);

-- [6] 桌位区域值域（期望仅 window/main/party/quiet，前端选桌逻辑依赖该口径）
SELECT DISTINCT area FROM dining_tables ORDER BY area;
