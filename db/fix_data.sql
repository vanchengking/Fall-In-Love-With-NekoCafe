-- ============================================================
-- 修复数据：添加更多顾客用户 + 分散预约 + 填充审计日志
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ── 添加更多顾客用户 ──
INSERT IGNORE INTO users (id, name, mobile_number, role, member_level, points, preferences) VALUES
(7,  '周子墨', '13800007777', 'customer', 'gold',   860,  JSON_ARRAY('quiet','window')),
(8,  '吴天宇', '13800008888', 'customer', 'silver', 320,  JSON_ARRAY('active','photo')),
(9,  '郑梦涵', '13800009999', 'customer', 'gold',   1560, JSON_ARRAY('sweet','gentle')),
(10, '何俊杰', '13800010000', 'customer', 'silver', 200,  JSON_ARRAY('coffee','quiet')),
(11, '孙小雪', '13900001111', 'customer', 'gold',   720,  JSON_ARRAY('photo','curious')),
(12, '刘思远', '13900002222', 'customer', 'silver', 450,  JSON_ARRAY('healthy','quiet')),
(13, '黄雅琴', '13900003333', 'customer', 'gold',   980,  JSON_ARRAY('sweet','gentle')),
(14, '马晓晨', '13900004444', 'customer', 'silver', 150,  JSON_ARRAY('active','playful')),
(15, '朱雨辰', '13600001111', 'customer', 'gold',   1120, JSON_ARRAY('quiet','coffee')),
(16, '许博文', '13600002222', 'customer', 'silver', 380,  JSON_ARRAY('photo','window')),
(17, '韩冰洁', '13600003333', 'customer', 'gold',   670,  JSON_ARRAY('sweet','quiet')),
(18, '曹文轩', '13600004444', 'customer', 'silver', 290,  JSON_ARRAY('active','coffee')),
(19, '蔡明辉', '13600005555', 'customer', 'gold',   1340, JSON_ARRAY('gentle','photo')),
(20, '田雨橙', '13600006666', 'customer', 'silver', 510,  JSON_ARRAY('quiet','window')),
(21, '钱思源', '13600007777', 'customer', 'gold',   890,  JSON_ARRAY('active','sweet')),
(22, '赵雨萱', '13800006666', 'customer', 'silver', 180,  JSON_ARRAY('coffee','photo'));

-- ── 更新预约，使用不同用户 ──
-- 五道口店
UPDATE reservations SET user_id = 7  WHERE id = 1001;  -- 周子墨
UPDATE reservations SET user_id = 8  WHERE id = 1002;  -- 吴天宇
UPDATE reservations SET user_id = 9  WHERE id = 1003;  -- 郑梦涵
UPDATE reservations SET user_id = 10 WHERE id = 1004;  -- 何俊杰
UPDATE reservations SET user_id = 1  WHERE id = 1005;  -- 林小满
UPDATE reservations SET user_id = 22 WHERE id = 1006;  -- 赵雨萱
UPDATE reservations SET user_id = 7  WHERE id = 1007;  -- 周子墨
UPDATE reservations SET user_id = 8  WHERE id = 1008;  -- 吴天宇
UPDATE reservations SET user_id = 9  WHERE id = 1009;  -- 郑梦涵
UPDATE reservations SET user_id = 10 WHERE id = 1010;  -- 何俊杰

-- 朝阳大悦城店
UPDATE reservations SET user_id = 11 WHERE id = 2001;  -- 孙小雪
UPDATE reservations SET user_id = 12 WHERE id = 2002;  -- 刘思远
UPDATE reservations SET user_id = 13 WHERE id = 2003;  -- 黄雅琴
UPDATE reservations SET user_id = 14 WHERE id = 2004;  -- 马晓晨
UPDATE reservations SET user_id = 1  WHERE id = 2005;  -- 林小满
UPDATE reservations SET user_id = 11 WHERE id = 2006;  -- 孙小雪
UPDATE reservations SET user_id = 12 WHERE id = 2007;  -- 刘思远

-- 西单店
UPDATE reservations SET user_id = 15 WHERE id = 3001;  -- 朱雨辰
UPDATE reservations SET user_id = 16 WHERE id = 3002;  -- 许博文
UPDATE reservations SET user_id = 17 WHERE id = 3003;  -- 韩冰洁
UPDATE reservations SET user_id = 18 WHERE id = 3004;  -- 曹文轩
UPDATE reservations SET user_id = 19 WHERE id = 3005;  -- 蔡明辉
UPDATE reservations SET user_id = 20 WHERE id = 3006;  -- 田雨橙
UPDATE reservations SET user_id = 21 WHERE id = 3007;  -- 钱思源

-- ── 更新订单用户 ──
UPDATE orders SET user_id = 7  WHERE id = 2001;
UPDATE orders SET user_id = 8  WHERE id = 2002;
UPDATE orders SET user_id = 9  WHERE id = 2003;
UPDATE orders SET user_id = 1  WHERE id = 2004;
UPDATE orders SET user_id = 10 WHERE id = 2005;
UPDATE orders SET user_id = 22 WHERE id = 2006;
UPDATE orders SET user_id = 11 WHERE id = 3001;
UPDATE orders SET user_id = 12 WHERE id = 3002;
UPDATE orders SET user_id = 1  WHERE id = 3003;
UPDATE orders SET user_id = 17 WHERE id = 4001;
UPDATE orders SET user_id = 15 WHERE id = 4002;
UPDATE orders SET user_id = 16 WHERE id = 4003;

-- ── 更新评价用户 ──
UPDATE reviews SET user_id = 1  WHERE id = 1;
UPDATE reviews SET user_id = 22 WHERE id = 2;
UPDATE reviews SET user_id = 13 WHERE id = 3;
UPDATE reviews SET user_id = 9  WHERE id = 4;
UPDATE reviews SET user_id = 10 WHERE id = 5;
UPDATE reviews SET user_id = 11 WHERE id = 6;
UPDATE reviews SET user_id = 14 WHERE id = 7;
UPDATE reviews SET user_id = 15 WHERE id = 8;
UPDATE reviews SET user_id = 19 WHERE id = 9;
UPDATE reviews SET user_id = 21 WHERE id = 10;

-- ── 填充审计日志 ──
DELETE FROM audit_logs;
INSERT INTO audit_logs (id, actor_user_id, actor_role, action, target_type, target_id, detail, created_at) VALUES
(1,  2,  'staff',      'STATUS_CHANGE', 'reservations', '1002', '{"method":"PATCH","status":200,"desc":"确认入座"}',           CONCAT(CURDATE(), ' 16:05:00')),
(2,  2,  'staff',      'STATUS_CHANGE', 'reservations', '1003', '{"method":"PATCH","status":200,"desc":"开始用餐"}',           CONCAT(CURDATE(), ' 17:05:00')),
(3,  2,  'staff',      'CREATE',        'orders',       '2001', '{"method":"POST","status":201,"desc":"创建订单 6800 分"}',    CONCAT(CURDATE(), ' 18:35:00')),
(4,  2,  'staff',      'STATUS_CHANGE', 'orders',       '2003', '{"method":"PATCH","status":200,"desc":"标记出餐"}',           CONCAT(CURDATE(), ' 17:25:00')),
(5,  6,  'admin',      'UPDATE',        'stores',       '1',    '{"method":"PATCH","status":200,"desc":"更新门店电话"}',       CONCAT(CURDATE(), ' 10:00:00')),
(6,  6,  'admin',      'CREATE',        'users',        '7',    '{"method":"POST","status":201,"desc":"新增顾客 周子墨"}',     CONCAT(CURDATE(), ' 09:30:00')),
(7,  6,  'admin',      'CREATE',        'users',        '8',    '{"method":"POST","status":201,"desc":"新增顾客 吴天宇"}',     CONCAT(CURDATE(), ' 09:31:00')),
(8,  5,  'cat_keeper', 'UPDATE',        'cats',         '3',    '{"method":"PATCH","status":200,"desc":"更新芝麻健康状态为观察中"}', CONCAT(CURDATE(), ' 10:15:00')),
(9,  5,  'cat_keeper', 'CREATE',        'cat_health_records', '10', '{"method":"POST","status":201,"desc":"芝麻体重记录 4.50kg"}', CONCAT(CURDATE(), ' 10:20:00')),
(10, 5,  'cat_keeper', 'UPDATE',        'cats',         '7',    '{"method":"PATCH","status":200,"desc":"更新提拉米苏健康状态为生病"}', CONCAT(CURDATE(), ' 11:00:00')),
(11, 3,  'manager',    'CREATE',        'menu_items',   '7',    '{"method":"POST","status":201,"desc":"新增菜品 抹茶拿铁"}',   CONCAT(CURDATE() - INTERVAL 1 DAY, ' 14:00:00')),
(12, 3,  'manager',    'CREATE',        'menu_items',   '8',    '{"method":"POST","status":201,"desc":"新增菜品 提拉米苏"}',   CONCAT(CURDATE() - INTERVAL 1 DAY, ' 14:05:00')),
(13, 4,  'operator',   'UPDATE',        'stores',       '2',    '{"method":"PATCH","status":200,"desc":"更新朝阳店地址"}',     CONCAT(CURDATE() - INTERVAL 1 DAY, ' 15:00:00')),
(14, 4,  'operator',   'CREATE',        'stores',       '3',    '{"method":"POST","status":201,"desc":"新增西单店"}',           CONCAT(CURDATE() - INTERVAL 2 DAY, ' 10:00:00')),
(15, 6,  'admin',      'CREATE',        'coupons',      '1',    '{"method":"POST","status":201,"desc":"创建优惠券 NEKO20"}',   CONCAT(CURDATE() - INTERVAL 3 DAY, ' 09:00:00')),
(16, 2,  'staff',      'STATUS_CHANGE', 'reservations', '1005', '{"method":"PATCH","status":200,"desc":"完桌"}',               CONCAT(CURDATE(), ' 16:20:00')),
(17, 2,  'staff',      'STATUS_CHANGE', 'orders',       '2004', '{"method":"PATCH","status":200,"desc":"标记已支付"}',         CONCAT(CURDATE(), ' 16:25:00')),
(18, 2,  'staff',      'STATUS_CHANGE', 'reservations', '2002', '{"method":"PATCH","status":200,"desc":"确认入座"}',           CONCAT(CURDATE(), ' 16:35:00')),
(19, 2,  'staff',      'STATUS_CHANGE', 'reservations', '2003', '{"method":"PATCH","status":200,"desc":"开始用餐"}',           CONCAT(CURDATE(), ' 17:35:00')),
(20, 6,  'admin',      'DELETE',        'menu_items',   '99',   '{"method":"DELETE","status":200,"desc":"删除已下架菜品"}',     CONCAT(CURDATE(), ' 11:30:00'));

SELECT '✅ 数据修复完成' AS result;
