SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

SET @create_idx_reservations_store_date_status = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'reservations'
        AND index_name = 'idx_reservations_store_date_status'
    ),
    'SELECT 1',
    'CREATE INDEX idx_reservations_store_date_status ON reservations (store_id, reservation_date, status)'
  )
);
PREPARE stmt FROM @create_idx_reservations_store_date_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @create_idx_orders_store_payment_created = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'orders'
        AND index_name = 'idx_orders_store_payment_created'
    ),
    'SELECT 1',
    'CREATE INDEX idx_orders_store_payment_created ON orders (store_id, payment_status, created_at)'
  )
);
PREPARE stmt FROM @create_idx_orders_store_payment_created;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @create_idx_users_member_level_created = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'users'
        AND index_name = 'idx_users_member_level_created'
    ),
    'SELECT 1',
    'CREATE INDEX idx_users_member_level_created ON users (member_level, created_at)'
  )
);
PREPARE stmt FROM @create_idx_users_member_level_created;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
