INSERT IGNORE INTO reservations (id, user_id, store_id, table_id, recommended_cat_id, reservation_date, reservation_time, party_size, status, note) VALUES
(3, 1, 1, 1, 1, CURDATE(), '10:00', 2, 'finished', '安静角落'),
(4, 2, 1, 2, 2, CURDATE(), '11:30', 4, 'finished', '生日聚会'),
(5, 3, 1, 3, 1, CURDATE(), '14:00', 3, 'seated', '带小朋友'),
(6, 4, 1, 1, 2, CURDATE(), '16:00', 2, 'booked', '第一次来'),
(7, 5, 1, 2, 1, CURDATE(), '18:30', 5, 'booked', '公司团建'),
(8, 1, 1, 3, 2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '10:00', 2, 'finished', '周末早午餐'),
(9, 2, 1, 1, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:00', 3, 'finished', ''),
(10, 3, 1, 2, 2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '16:30', 2, 'finished', ''),
(11, 4, 1, 3, 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '11:00', 4, 'finished', '朋友聚会'),
(12, 5, 1, 1, 2, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '15:00', 2, 'cancelled', ''),
(13, 1, 1, 2, 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '10:30', 2, 'finished', ''),
(14, 2, 1, 3, 2, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '14:00', 3, 'no_show', ''),
(15, 3, 1, 1, 1, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '16:00', 2, 'finished', ''),
(16, 1, 1, 2, 2, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '11:00', 2, 'finished', ''),
(17, 1, 1, 3, 1, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '15:00', 3, 'finished', '');

INSERT IGNORE INTO orders (id, reservation_id, user_id, store_id, status, payment_status, total_cents) VALUES
(1, 3, 1, 1, 'paid', 'sandbox_paid', 6400),
(2, 4, 2, 1, 'paid', 'sandbox_paid', 12600),
(3, 5, 3, 1, 'paid', 'sandbox_paid', 9400),
(4, 8, 1, 1, 'paid', 'sandbox_paid', 5800),
(5, 9, 2, 1, 'paid', 'sandbox_paid', 7000),
(6, 10, 3, 1, 'paid', 'sandbox_paid', 3600),
(7, 11, 4, 1, 'paid', 'sandbox_paid', 15800),
(8, 13, 1, 1, 'paid', 'sandbox_paid', 8600),
(9, 15, 3, 1, 'paid', 'sandbox_paid', 6400),
(10, 16, 1, 1, 'paid', 'sandbox_paid', 5800),
(11, 17, 1, 1, 'paid', 'sandbox_paid', 9200);

INSERT IGNORE INTO order_items (order_id, menu_item_id, quantity, unit_price_cents) VALUES
(1, 1, 2, 3200),
(2, 2, 1, 5800), (2, 3, 1, 3600), (2, 1, 1, 3200),
(3, 2, 1, 5800), (3, 1, 1, 3200),
(4, 2, 1, 5800),
(5, 1, 1, 3200), (5, 3, 1, 3600),
(6, 3, 1, 3600),
(7, 2, 2, 5800), (7, 3, 1, 3600), (7, 1, 1, 3200),
(8, 1, 1, 3200), (8, 2, 1, 5800),
(9, 1, 2, 3200),
(10, 2, 1, 5800),
(11, 3, 1, 3600), (11, 2, 1, 5800);

INSERT IGNORE INTO reviews (id, reservation_id, user_id, store_id, cat_id, rating, content) VALUES
(3, 3, 1, 1, 1, 5, '团子太可爱了，安静地趴在桌上，环境非常舒适！'),
(4, 4, 2, 1, 2, 4, '拿铁很活泼，拍照很配合，甜品也不错。'),
(5, 5, 3, 1, 1, 5, '带孩子来的，团子特别温柔，小朋友玩得很开心。'),
(6, 8, 1, 1, 2, 4, '周末早午餐体验很好，猫咪很亲人。'),
(7, 9, 2, 1, 1, 5, '第三次来了，每次都很满意，推荐猫爪拿铁！'),
(8, 11, 4, 1, 2, 3, '人有点多，等位时间长，但猫咪很可爱。'),
(9, 13, 1, 1, 1, 5, '最爱的猫咖，团子是镇店之宝！'),
(10, 15, 3, 1, 2, 4, '拿铁真的很爱拍照，抓拍了好多可爱瞬间。'),
(11, 16, 1, 1, 2, 5, '三文鱼碗很好吃，猫咪也很配合。'),
(12, 17, 1, 1, 1, 4, '安静的下午，团子陪我看书，完美。');

INSERT IGNORE INTO cat_health_records (cat_id, weight_kg, vaccine_note, interaction_note) VALUES
(1, 4.60, '第一针疫苗完成', '精神状态良好'),
(1, 4.70, '', '今天互动温和，喜欢被摸头'),
(1, 4.75, '第二针疫苗完成', '稍微有点紧张'),
(1, 4.80, '', '恢复正常，互动活泼'),
(1, 4.82, '年度体检正常', '食欲良好'),
(2, 5.00, '第一针疫苗完成', '活泼好动'),
(2, 5.10, '', '今天很喜欢拍照'),
(2, 5.15, '第二针疫苗完成', '互动正常'),
(2, 5.20, '', '食欲旺盛'),
(2, 5.22, '年度体检正常', '精力充沛，爱追逗猫棒');

INSERT IGNORE INTO payment_transactions (order_id, txn_ref, channel, status, amount_cents) VALUES
(1, 'SBX-001', 'sandbox', 'paid', 6400),
(2, 'SBX-002', 'sandbox', 'paid', 12600),
(3, 'SBX-003', 'sandbox', 'paid', 9400),
(4, 'SBX-004', 'sandbox', 'paid', 5800),
(5, 'SBX-005', 'sandbox', 'paid', 7000),
(6, 'SBX-006', 'sandbox', 'paid', 3600),
(7, 'SBX-007', 'sandbox', 'paid', 15800),
(8, 'SBX-008', 'sandbox', 'paid', 8600),
(9, 'SBX-009', 'sandbox', 'paid', 6400),
(10, 'SBX-010', 'sandbox', 'paid', 5800),
(11, 'SBX-011', 'sandbox', 'paid', 9200);

INSERT IGNORE INTO reservation_events (reservation_id, from_status, to_status, actor_role, actor_user_id, note) VALUES
(3, 'booked', 'seated', 'staff', 2, '顾客已到店'),
(3, 'seated', 'finished', 'staff', 2, '用餐完毕'),
(4, 'booked', 'seated', 'staff', 2, '顾客已到店'),
(4, 'seated', 'finished', 'staff', 2, '用餐完毕'),
(5, 'booked', 'seated', 'staff', 2, '顾客已到店'),
(8, 'booked', 'seated', 'staff', 2, '顾客已到店'),
(8, 'seated', 'finished', 'staff', 2, '用餐完毕'),
(9, 'booked', 'seated', 'staff', 2, '顾客已到店'),
(9, 'seated', 'finished', 'staff', 2, '用餐完毕'),
(10, 'booked', 'seated', 'staff', 2, '顾客已到店'),
(10, 'seated', 'finished', 'staff', 2, '用餐完毕'),
(11, 'booked', 'seated', 'staff', 2, '顾客已到店'),
(11, 'seated', 'finished', 'staff', 2, '用餐完毕'),
(12, 'booked', 'cancelled', 'customer', 5, '临时有事取消'),
(13, 'booked', 'seated', 'staff', 2, '顾客已到店'),
(13, 'seated', 'finished', 'staff', 2, '用餐完毕'),
(15, 'booked', 'seated', 'staff', 2, '顾客已到店'),
(15, 'seated', 'finished', 'staff', 2, '用餐完毕'),
(16, 'booked', 'seated', 'staff', 2, '顾客已到店'),
(16, 'seated', 'finished', 'staff', 2, '用餐完毕'),
(17, 'booked', 'seated', 'staff', 2, '顾客已到店'),
(17, 'seated', 'finished', 'staff', 2, '用餐完毕');

INSERT IGNORE INTO operation_alerts (store_id, level, title, detail) VALUES
(1, 'warning', '晚高峰桌位紧张', '18:00-20:00 预约集中，建议增加临时桌位。'),
(1, 'info', '今日复购率上升', '近7日复购率达45%，较上周增长8%。'),
(1, 'warning', '团子体重偏轻', '团子近两周体重下降0.1kg，建议关注饮食。'),
(1, 'info', '周末预约爆满', '本周六日预约率达95%，建议提前安排人手。'),
(1, 'warning', '拿铁疫苗即将到期', '拿铁年度疫苗将于下周到期，请安排接种。');
