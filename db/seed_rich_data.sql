-- ============================================================
-- NekoCafe 丰富演示数据填充脚本
-- 执行方式: docker exec -i fall-in-love-with-nekocafe-mysql-1 mysql -uneko -pneko neko_cafe < db/seed_rich_data.sql
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ── 清空现有数据（保留表结构）──
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE reservation_events;
TRUNCATE TABLE payment_transactions;
TRUNCATE TABLE audit_logs;
TRUNCATE TABLE reviews;
TRUNCATE TABLE cat_health_records;
TRUNCATE TABLE vaccine_records;
TRUNCATE TABLE operation_alerts;
TRUNCATE TABLE coupons;
TRUNCATE TABLE reservations;
TRUNCATE TABLE menu_items;
TRUNCATE TABLE cats;
TRUNCATE TABLE dining_tables;
TRUNCATE TABLE stores;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
--  用户（6 个角色）
-- ============================================================
INSERT INTO users (id, name, mobile_number, role, member_level, points, preferences) VALUES
(1, '林小满',     '13800000001', 'customer',   'gold',   1280, JSON_ARRAY('quiet','window','sweet','ragdoll')),
(2, '高店员',     '13800000002', 'staff',      'silver', 0,    JSON_ARRAY()),
(3, '葛店长',     '13800000003', 'manager',    'silver', 0,    JSON_ARRAY()),
(4, '总部运营',   '13800000004', 'operator',   'silver', 0,    JSON_ARRAY()),
(5, '卢猫咪管家', '13800000005', 'cat_keeper', 'silver', 0,    JSON_ARRAY()),
(6, '系统管理员', '13800000006', 'admin',      'silver', 0,    JSON_ARRAY());

-- ============================================================
--  门店（3 家）
-- ============================================================
INSERT INTO stores (id, name, city, address, phone, open_time, close_time, latitude, longitude) VALUES
(1, 'NekoCafe 五道口店',     '北京', '海淀区成府路 99 号 2F',       '010-82880001', '10:00:00', '22:00:00', 39.992821, 116.337388),
(2, 'NekoCafe 朝阳大悦城店', '北京', '朝阳区朝阳北路 101 号 5F',    '010-85880002', '10:00:00', '22:00:00', 39.920000, 116.470000),
(3, 'NekoCafe 西单店',       '北京', '西城区西单北大街 110 号 B1',   '010-66880003', '10:30:00', '21:30:00', 39.910000, 116.370000);

-- ============================================================
--  桌位（29 张，覆盖全部状态）
-- ============================================================
INSERT INTO dining_tables (id, store_id, code, seats, area, cat_zone, status) VALUES
-- 五道口店（10 桌）
(101, 1, 'A01', 2, 'window', 1, 'occupied'),
(102, 1, 'A02', 4, 'window', 1, 'dining'),
(103, 1, 'A03', 2, 'window', 1, 'reserved'),
(104, 1, 'B01', 4, 'main',   0, 'available'),
(105, 1, 'B02', 6, 'main',   0, 'reserved'),
(106, 1, 'B03', 4, 'main',   1, 'cleaning'),
(107, 1, 'B04', 4, 'main',   0, 'available'),
(108, 1, 'C01', 6, 'party',  1, 'cleaning'),
(109, 1, 'C02', 8, 'party',  0, 'available'),
(110, 1, 'C03', 8, 'party',  0, 'occupied'),
-- 朝阳大悦城店（10 桌）
(201, 2, 'A01', 2, 'window', 1, 'occupied'),
(202, 2, 'A02', 2, 'window', 1, 'dining'),
(203, 2, 'A03', 4, 'window', 1, 'reserved'),
(204, 2, 'B01', 4, 'main',   0, 'available'),
(205, 2, 'B02', 4, 'main',   0, 'available'),
(206, 2, 'B03', 6, 'main',   1, 'reserved'),
(207, 2, 'B04', 4, 'main',   0, 'cleaning'),
(208, 2, 'C01', 6, 'party',  1, 'available'),
(209, 2, 'C02', 8, 'party',  0, 'occupied'),
(210, 2, 'C03', 8, 'party',  0, 'available'),
-- 西单店（9 桌）
(301, 3, 'A01', 2, 'window', 1, 'reserved'),
(302, 3, 'A02', 4, 'window', 1, 'occupied'),
(303, 3, 'B01', 4, 'main',   0, 'dining'),
(304, 3, 'B02', 4, 'main',   0, 'available'),
(305, 3, 'B03', 6, 'main',   1, 'available'),
(306, 3, 'B04', 4, 'main',   0, 'cleaning'),
(307, 3, 'C01', 6, 'party',  1, 'available'),
(308, 3, 'C02', 8, 'party',  0, 'reserved'),
(309, 3, 'C03', 8, 'party',  0, 'available');

-- ============================================================
--  猫咪（11 只，覆盖所有健康状态）
-- ============================================================
INSERT INTO cats (id, store_id, name, breed, personality_tags, health_status, weight_kg, last_vaccine_at, photo_url) VALUES
(1,  1, '团子',     '布偶猫',       JSON_ARRAY('quiet','gentle'),              'healthy', 4.80, CURDATE() - INTERVAL 3 DAY, 'https://placekitten.com/400/300'),
(2,  1, '拿铁',     '英国短毛猫',   JSON_ARRAY('active','photo'),              'healthy', 5.20, CURDATE() - INTERVAL 2 DAY, 'https://placekitten.com/401/301'),
(3,  1, '芝麻',     '异国短毛猫',   JSON_ARRAY('lazy','friendly'),             'observe', 4.50, CURDATE() - INTERVAL 1 DAY, 'https://placekitten.com/402/302'),
(4,  1, '抹茶',     '美国短毛猫',   JSON_ARRAY('sweet','gentle'),              'healthy', 3.90, CURDATE(),                   'https://placekitten.com/403/303'),
(5,  1, '布丁',     '苏格兰折耳猫', JSON_ARRAY('quiet','lazy'),                'resting', 4.10, CURDATE() - INTERVAL 3 DAY, 'https://placekitten.com/404/304'),
(6,  2, '摩卡',     '暹罗猫',       JSON_ARRAY('active','curious'),            'healthy', 3.60, CURDATE() - INTERVAL 2 DAY, 'https://placekitten.com/405/305'),
(7,  2, '提拉米苏', '俄罗斯蓝猫',   JSON_ARRAY('shy','gentle'),                'sick',    4.30, CURDATE() - INTERVAL 1 DAY, 'https://placekitten.com/406/306'),
(8,  2, '乌龙',     '孟加拉豹猫',   JSON_ARRAY('active','playful'),            'healthy', 5.00, CURDATE(),                   'https://placekitten.com/407/307'),
(9,  3, '芋圆',     '金渐层',       JSON_ARRAY('gentle','photo'),              'healthy', 4.60, CURDATE() - INTERVAL 2 DAY, 'https://placekitten.com/408/308'),
(10, 3, '奶盖',     '银渐层',       JSON_ARRAY('quiet','curious'),             'healthy', 3.80, CURDATE() - INTERVAL 1 DAY, 'https://placekitten.com/409/309'),
(11, 3, '豆花',     '英短蓝猫',     JSON_ARRAY('lazy','friendly'),             'resting', 5.10, CURDATE() - INTERVAL 3 DAY, 'https://placekitten.com/410/310');

-- ============================================================
--  菜单（16 道，覆盖 3 家门店）
-- ============================================================
INSERT INTO menu_items (id, store_id, name, category, price_cents, tags, photo_url, status) VALUES
(1,  1, '猫爪拿铁',         'drink',   3200, JSON_ARRAY('coffee','signature'),      'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=300&h=200&fit=crop', 'available'),
(2,  1, '三文鱼能量碗',     'meal',    5800, JSON_ARRAY('healthy','salmon'),         'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=300&h=200&fit=crop', 'available'),
(3,  1, '毛线球芝士蛋糕',   'dessert', 3600, JSON_ARRAY('sweet','cheese'),           'https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=300&h=200&fit=crop', 'available'),
(4,  1, '猫咪冰淇淋',       'dessert', 2800, JSON_ARRAY('sweet','ice-cream'),        'https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=300&h=200&fit=crop', 'available'),
(5,  1, '手冲埃塞俄比亚',   'drink',   4200, JSON_ARRAY('coffee','single-origin'),   'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=300&h=200&fit=crop', 'available'),
(6,  1, '全日早午餐盘',     'meal',    6800, JSON_ARRAY('healthy','brunch'),         'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=300&h=200&fit=crop', 'available'),
(7,  1, '抹茶拿铁',         'drink',   3000, JSON_ARRAY('tea','matcha'),             'https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=300&h=200&fit=crop', 'available'),
(8,  1, '提拉米苏',         'dessert', 3800, JSON_ARRAY('sweet','coffee'),           'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=300&h=200&fit=crop', 'available'),
(9,  2, '焦糖玛奇朵',       'drink',   3600, JSON_ARRAY('coffee','sweet'),           'https://images.unsplash.com/photo-1485808191679-5f86510681a2?w=300&h=200&fit=crop', 'available'),
(10, 2, '烟熏三文鱼贝果',   'meal',    4600, JSON_ARRAY('healthy','salmon'),         'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=300&h=200&fit=crop', 'available'),
(11, 2, '草莓千层蛋糕',     'dessert', 4200, JSON_ARRAY('sweet','strawberry'),       'https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=300&h=200&fit=crop', 'available'),
(12, 2, '冰美式',           'drink',   2200, JSON_ARRAY('coffee','iced'),            'https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=300&h=200&fit=crop', 'available'),
(13, 3, '桂花拿铁',         'drink',   3400, JSON_ARRAY('coffee','floral'),          'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=300&h=200&fit=crop', 'available'),
(14, 3, '牛油果吐司',       'meal',    3800, JSON_ARRAY('healthy','avocado'),        'https://images.unsplash.com/photo-1541519227354-08fa5d50c44d?w=300&h=200&fit=crop', 'available'),
(15, 3, '芒果慕斯',         'dessert', 3200, JSON_ARRAY('sweet','mango'),            'https://images.unsplash.com/photo-1488477181946-6428a0291777?w=300&h=200&fit=crop', 'available'),
(16, 3, '燕麦奶拿铁',       'drink',   3000, JSON_ARRAY('coffee','oat-milk'),        'https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=300&h=200&fit=crop', 'available');

-- ============================================================
--  预约（25 条，覆盖全部状态 + 多时间段）
-- ============================================================
INSERT INTO reservations (id, user_id, store_id, table_id, recommended_cat_id, reservation_date, reservation_time, party_size, status, note) VALUES
-- 五道口 今天
(1001, 1, 1, 103, 3,  CURDATE(), '18:30:00', 2, 'booked',    '希望坐窗边'),
(1002, 1, 1, 101, 1,  CURDATE(), '16:00:00', 3, 'seated',    ''),
(1003, 1, 1, 102, 2,  CURDATE(), '17:00:00', 2, 'dining',    '少冰'),
(1004, 1, 1, 105, 4,  CURDATE(), '19:30:00', 4, 'booked',    '公司团建'),
(1005, 1, 1, 110, 2,  CURDATE(), '15:00:00', 5, 'finished',  ''),
(1006, 1, 1, 106, 5,  CURDATE(), '16:30:00', 2, 'finished',  ''),
(1007, 1, 1, NULL, 1, CURDATE(), '11:00:00', 2, 'cancelled', '临时有事'),
(1008, 1, 1, NULL, 4, CURDATE(), '12:00:00', 3, 'no_show',   ''),
-- 五道口 明天
(1009, 1, 1, 108, 1,  CURDATE() + INTERVAL 1 DAY, '19:00:00', 6, 'booked', '生日聚会，需要蛋糕'),
(1010, 1, 1, 109, 2,  CURDATE() + INTERVAL 1 DAY, '14:00:00', 4, 'booked', ''),
-- 朝阳 今天
(2001, 1, 2, 203, 8,  CURDATE(), '18:00:00', 2, 'booked',    ''),
(2002, 1, 2, 201, 6,  CURDATE(), '16:30:00', 2, 'seated',    '对猫毛过敏但还是想来'),
(2003, 1, 2, 202, 7,  CURDATE(), '17:30:00', 3, 'dining',    ''),
(2004, 1, 2, 206, 6,  CURDATE(), '19:00:00', 4, 'booked',    '带小朋友'),
(2005, 1, 2, 209, 7,  CURDATE(), '14:00:00', 6, 'finished',  ''),
(2006, 1, 2, NULL, 8, CURDATE(), '10:30:00', 2, 'no_show',   ''),
-- 朝阳 明天
(2007, 1, 2, 208, 6,  CURDATE() + INTERVAL 1 DAY, '18:30:00', 5, 'booked', '朋友聚会'),
-- 西单 今天
(3001, 1, 3, 301, 9,  CURDATE(), '17:00:00', 2, 'booked',    ''),
(3002, 1, 3, 302, 10, CURDATE(), '16:00:00', 2, 'seated',    '第一次来'),
(3003, 1, 3, 303, 11, CURDATE(), '15:30:00', 3, 'dining',    ''),
(3004, 1, 3, 308, 10, CURDATE(), '19:00:00', 5, 'booked',    '商务洽谈'),
(3005, 1, 3, NULL, 9, CURDATE(), '11:30:00', 2, 'cancelled', '改期'),
-- 西单 明天
(3006, 1, 3, 307, 11, CURDATE() + INTERVAL 1 DAY, '18:00:00', 4, 'booked', ''),
(3007, 1, 3, 309, 9,  CURDATE() + INTERVAL 1 DAY, '15:00:00', 6, 'booked', '团建活动');

-- ============================================================
--  订单（12 条，覆盖全部状态）
-- ============================================================
INSERT INTO orders (id, reservation_id, user_id, store_id, status, payment_status, total_cents, created_at) VALUES
(2001, 1001, 1, 1, 'created',       'sandbox_paid', 6800,  CONCAT(CURDATE(), ' 18:35:00')),
(2002, 1002, 1, 1, 'preparing',     'sandbox_paid', 12200, CONCAT(CURDATE(), ' 16:10:00')),
(2003, 1003, 1, 1, 'served',        'sandbox_paid', 12800, CONCAT(CURDATE(), ' 17:10:00')),
(2004, 1005, 1, 1, 'paid',          'sandbox_paid', 16600, CONCAT(CURDATE(), ' 15:05:00')),
(2005, 1004, 1, 1, 'created',       'sandbox_paid', 18800, CONCAT(CURDATE(), ' 19:35:00')),
(2006, 1006, 1, 1, 'exception',     'sandbox_paid', 7200,  CONCAT(CURDATE(), ' 16:40:00')),
(3001, 2001, 1, 2, 'created',       'sandbox_paid', 8200,  CONCAT(CURDATE(), ' 18:05:00')),
(3002, 2002, 1, 2, 'preparing',     'sandbox_paid', 8200,  CONCAT(CURDATE(), ' 16:35:00')),
(3003, 2005, 1, 2, 'paid',          'sandbox_paid', 14600, CONCAT(CURDATE(), ' 14:05:00')),
(4001, 3003, 1, 3, 'served',        'sandbox_paid', 7200,  CONCAT(CURDATE(), ' 15:35:00')),
(4002, 3001, 1, 3, 'created',       'sandbox_paid', 7200,  CONCAT(CURDATE(), ' 17:05:00')),
(4003, 3002, 1, 3, 'paid',          'sandbox_paid', 10200, CONCAT(CURDATE() - INTERVAL 1 DAY, ' 16:05:00'));

-- ============================================================
--  订单明细
-- ============================================================
INSERT INTO order_items (id, order_id, menu_item_id, quantity, unit_price_cents) VALUES
(1,  2001, 5,  1, 4200),
(2,  2001, 3,  1, 3600),
(3,  2002, 1,  2, 3200),
(4,  2002, 2,  1, 5800),
(5,  2003, 2,  1, 5800),
(6,  2003, 5,  1, 4200),
(7,  2003, 4,  1, 2800),
(8,  2004, 1,  3, 3200),
(9,  2004, 6,  1, 6800),
(10, 2005, 2,  2, 5800),
(11, 2005, 1,  1, 3200),
(12, 2005, 4,  1, 2800),
(13, 2006, 3,  1, 3600),
(14, 2006, 1,  1, 3200),
(15, 3001, 9,  1, 3600),
(16, 3001, 11, 1, 4200),
(17, 3002, 12, 2, 2200),
(18, 3002, 10, 1, 4600),
(19, 3003, 9,  3, 3600),
(20, 3003, 11, 1, 4200),
(21, 4001, 13, 2, 3400),
(22, 4001, 15, 1, 3200),
(23, 4002, 16, 1, 3000),
(24, 4002, 14, 1, 3800),
(25, 4003, 13, 2, 3400),
(26, 4003, 15, 1, 3200);

-- ============================================================
--  评价（10 条）
-- ============================================================
INSERT INTO reviews (id, user_id, store_id, cat_id, rating, content, created_at) VALUES
(1,  1, 1, 2,  5, '环境非常好，猫咪很亲人，拿铁拉花太可爱了！下次还来。',         CONCAT(CURDATE(), ' 16:30:00')),
(2,  1, 1, 5,  4, '团子太萌了，一直在腿上睡觉。甜品也不错，就是等位有点久。',   CONCAT(CURDATE(), ' 17:00:00')),
(3,  1, 2, 7,  5, '带小朋友来的，玩得特别开心。猫咪都很健康干净，工作人员也很热情。', CONCAT(CURDATE() - INTERVAL 1 DAY, ' 15:45:00')),
(4,  1, 1, 1,  5, '抹茶蛋糕绝了！猫咪摩卡特别活泼，拍照超配合。',               CONCAT(CURDATE() - INTERVAL 1 DAY, ' 14:00:00')),
(5,  1, 1, 4,  4, '环境安静适合办公，手冲咖啡水准在线。布丁猫太治愈了。',       CONCAT(CURDATE() - INTERVAL 2 DAY, ' 16:00:00')),
(6,  1, 2, 6,  5, '第三次来了，每次都有新猫咪。店员很专业，讲解猫咪性格。',     CONCAT(CURDATE() - INTERVAL 2 DAY, ' 18:00:00')),
(7,  1, 2, 8,  4, '猫爪拿铁必点！拉花是猫爪形状，舍不得喝。',                   CONCAT(CURDATE() - INTERVAL 3 DAY, ' 15:00:00')),
(8,  1, 3, 9,  5, '西单店环境很好，桂花拿铁特别香。芋圆超级乖。',               CONCAT(CURDATE() - INTERVAL 1 DAY, ' 18:00:00')),
(9,  1, 3, 10, 4, '牛油果吐司很新鲜，芒果慕斯甜度刚好。奶盖猫很安静。',         CONCAT(CURDATE() - INTERVAL 2 DAY, ' 17:00:00')),
(10, 1, 3, 11, 3, '豆花在睡觉没互动到，有点遗憾。饮品不错。',                   CONCAT(CURDATE() - INTERVAL 3 DAY, ' 14:00:00'));

-- ============================================================
--  猫咪健康记录（26 条）
-- ============================================================
INSERT INTO cat_health_records (id, cat_id, weight_kg, vaccine_note, interaction_note, recorded_at) VALUES
(1,  1,  4.50, '狂犬疫苗第一针',   '今日安静，适合安静桌位',       CONCAT(CURDATE() - INTERVAL 3 DAY, ' 10:00:00')),
(2,  1,  4.60, '',                 '主动蹭人，性格温顺',           CONCAT(CURDATE() - INTERVAL 2 DAY, ' 14:00:00')),
(3,  1,  4.80, '狂犬疫苗第二针',   '状态良好，食欲正常',           CONCAT(CURDATE() - INTERVAL 1 DAY, ' 11:00:00')),
(4,  1,  4.80, '',                 '今日互动 5 次，顾客好评',      CONCAT(CURDATE(), ' 15:00:00')),
(5,  2,  5.00, '猫三联疫苗',       '活泼好动，喜欢逗猫棒',         CONCAT(CURDATE() - INTERVAL 2 DAY, ' 10:00:00')),
(6,  2,  5.10, '',                 '镜头感十足，配合拍照',         CONCAT(CURDATE() - INTERVAL 1 DAY, ' 16:00:00')),
(7,  2,  5.20, '',                 '今日互动 8 次，最受欢迎',      CONCAT(CURDATE(), ' 14:00:00')),
(8,  3,  4.60, '驱虫已完成',       '食欲略有下降',                 CONCAT(CURDATE() - INTERVAL 2 DAY, ' 09:00:00')),
(9,  3,  4.50, '',                 '精神一般，建议观察',           CONCAT(CURDATE() - INTERVAL 1 DAY, ' 10:00:00')),
(10, 3,  4.50, '',                 '今日活动量减少',               CONCAT(CURDATE(), ' 11:00:00')),
(11, 4,  3.70, '猫三联疫苗第一针', '性格甜美，喜欢被抱',           CONCAT(CURDATE(), ' 10:00:00')),
(12, 4,  3.90, '',                 '今日互动 3 次',                CONCAT(CURDATE(), ' 16:00:00')),
(13, 5,  4.00, '狂犬疫苗',         '大部分时间在睡觉',             CONCAT(CURDATE() - INTERVAL 3 DAY, ' 14:00:00')),
(14, 5,  4.10, '',                 '偶尔醒来散步',                 CONCAT(CURDATE() - INTERVAL 1 DAY, ' 15:00:00')),
(15, 6,  3.40, '猫三联疫苗',       '好奇心旺盛，探索欲强',         CONCAT(CURDATE() - INTERVAL 2 DAY, ' 11:00:00')),
(16, 6,  3.60, '',                 '今日互动 6 次',                CONCAT(CURDATE(), ' 13:00:00')),
(17, 7,  4.40, '驱虫已完成',       '精神不佳，需关注',             CONCAT(CURDATE() - INTERVAL 1 DAY, ' 09:00:00')),
(18, 7,  4.30, '',                 '已安排兽医检查',               CONCAT(CURDATE(), ' 10:00:00')),
(19, 8,  4.80, '狂犬疫苗',         '精力充沛，追逐逗猫棒',         CONCAT(CURDATE(), ' 11:00:00')),
(20, 8,  5.00, '',                 '今日互动 7 次',                CONCAT(CURDATE(), ' 15:00:00')),
(21, 9,  4.40, '猫三联疫苗',       '温柔亲人，适合拍照',           CONCAT(CURDATE() - INTERVAL 2 DAY, ' 10:00:00')),
(22, 9,  4.60, '',                 '今日互动 4 次',                CONCAT(CURDATE(), ' 14:00:00')),
(23, 10, 3.60, '驱虫已完成',       '安静陪伴型',                   CONCAT(CURDATE() - INTERVAL 1 DAY, ' 11:00:00')),
(24, 10, 3.80, '',                 '今日互动 2 次',                CONCAT(CURDATE(), ' 15:00:00')),
(25, 11, 5.00, '狂犬疫苗',         '典型沙发土豆',                 CONCAT(CURDATE() - INTERVAL 3 DAY, ' 10:00:00')),
(26, 11, 5.10, '',                 '偶尔慢悠悠走动',               CONCAT(CURDATE() - INTERVAL 1 DAY, ' 14:00:00'));

-- ============================================================
--  运营告警（5 条）
-- ============================================================
INSERT INTO operation_alerts (id, store_id, level, title, detail, resolved) VALUES
(1, 1, 'warning', '晚高峰桌位紧张',         '18:00-20:00 预约集中（4 桌），建议提前协调加桌。', 0),
(2, 1, 'info',    '团子休息提醒',           '团子 18:00 后需休息，建议安排其他猫咪接替靠窗猫区。', 0),
(3, 1, 'warning', '毛线球芝士蛋糕库存不足', '当前剩余 3 份，预计今日售罄，请通知后厨备货。', 0),
(4, 1, 'info',    '赵雨萱订单异常跟进',     '花生过敏订单，已协调退菜，待顾客确认。', 0),
(5, 1, 'warning', 'B03 桌清台超时',         'B03 已空闲 15 分钟未清理，影响后续预约。', 0);

-- ============================================================
--  审计日志（10 条）
-- ============================================================
INSERT INTO audit_logs (id, actor_user_id, actor_role, action, target_type, target_id, detail, created_at) VALUES
(1,  2, 'staff',      'CREATE',        'orders',       '2001', '{"method":"POST","path":"/api/orders","status":201}',           CONCAT(CURDATE(), ' 19:35:12')),
(2,  NULL, 'system',  'STATUS_CHANGE', 'reservations', '1001', '{"method":"PATCH","path":"/api/reservations/1001/status","status":200}', CONCAT(CURDATE(), ' 18:30:05')),
(3,  2, 'staff',      'STATUS_CHANGE', 'orders',       '2003', '{"method":"PATCH","path":"/api/orders/2003/status","status":200}',       CONCAT(CURDATE(), ' 17:25:30')),
(4,  NULL, 'system',  'STATUS_CHANGE', 'orders',       '2004', '{"method":"PATCH","path":"/api/orders/2004/status","status":200}',       CONCAT(CURDATE(), ' 16:20:45')),
(5,  3, 'manager',    'UPDATE',        'stores',       '1',    '{"method":"PATCH","path":"/api/stores/1","status":200}',                CONCAT(CURDATE(), ' 16:00:12')),
(6,  5, 'cat_keeper', 'CREATE',        'cat_health_records', '14', '{"method":"POST","path":"/api/cat-health-records","status":201}',  CONCAT(CURDATE(), ' 15:30:00')),
(7,  6, 'admin',      'CREATE',        'users',        '7',    '{"method":"POST","path":"/api/users","status":201}',                    CONCAT(CURDATE(), ' 14:00:08')),
(8,  2, 'staff',      'DELETE',        'menu_items',   '99',   '{"method":"DELETE","path":"/api/menu-items/99","status":200}',           CONCAT(CURDATE(), ' 11:30:22')),
(9,  NULL, 'system',  'STATUS_CHANGE', 'reservations', '1005', '{"method":"PATCH","path":"/api/reservations/1005/status","status":200}', CONCAT(CURDATE() - INTERVAL 1 DAY, ' 20:00:00')),
(10, 4, 'operator',   'UPDATE',        'stores',       '2',    '{"method":"PATCH","path":"/api/stores/2","status":200}',                CONCAT(CURDATE() - INTERVAL 1 DAY, ' 18:15:33'));

-- ============================================================
--  优惠券（4 条）
-- ============================================================
INSERT INTO coupons (id, code, title, discount_cents, min_spend_cents, valid_from, valid_to, status) VALUES
(1, 'NEKO20',  '新客立减 20 元',   2000, 5000,  CURDATE(), CURDATE() + INTERVAL 1 DAY, 'active'),
(2, 'CAT50',   '猫咪日半价',       5000, 10000, CURDATE(), CURDATE() + INTERVAL 1 DAY, 'active'),
(3, 'WEEKEND', '周末满减',         3000, 8000,  CURDATE() - INTERVAL 1 DAY, CURDATE() + INTERVAL 1 DAY, 'active'),
(4, 'SPRING',  '春日限定优惠',     1500, 3000,  CURDATE() - INTERVAL 3 DAY, CURDATE() - INTERVAL 1 DAY, 'expired');

-- ============================================================
--  完成
-- ============================================================
SELECT '✅ 数据填充完成' AS result;
