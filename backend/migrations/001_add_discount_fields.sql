-- 为 orders 表添加原价和折扣率字段
-- 执行此SQL以支持会员折扣功能

ALTER TABLE orders 
ADD COLUMN original_total_cents INT AFTER total_cents,
ADD COLUMN discount_rate DOUBLE AFTER original_total_cents;

-- 为现有数据设置默认值（原价=总价，折扣率=1.0表示无折扣）
UPDATE orders 
SET original_total_cents = total_cents, 
    discount_rate = 1.0 
WHERE original_total_cents IS NULL;
