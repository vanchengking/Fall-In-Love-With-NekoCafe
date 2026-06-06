-- NekoCafe 预约后台补全迁移（V003）
-- 在 V001__init.sql + V002__d01_upgrade.sql 之上补齐预约后台所需对象：
--   1. 活跃占用状态已含 created/booked/seated/dining —— 由 V002 的生成列 active_slot_key /
--      user_active_slot_key 维护，本迁移不再重建生成列以免破坏既有数据，仅做兜底校验。
--   2. "同用户同门店同时段"活跃唯一约束 uq_reservations_user_slot —— V002 已建，本迁移兜底确保存在。
--   3. 新增推荐计算日志表 recommendation_logs。
--   4. 给 reservation_events 增加按状态检索索引（兼容已有表）。
-- 设计为可安全应用于"已经执行过 V001/V002"的数据库（含 Flyway/RDS 场景）：
--   - 新增对象使用 CREATE TABLE IF NOT EXISTS；
--   - 约束/索引通过 information_schema 检查后再 ALTER，避免在已存在对象上直接失败。
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ---------------------------------------------------------------------------
-- 3. 推荐计算日志（最小审计表）。推荐接口每次成功计算写一条；写入失败不影响主流程。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recommendation_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  store_id BIGINT NULL,
  preferences_json JSON NULL,
  result_snapshot_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_recommendation_logs_user (user_id, created_at),
  KEY idx_recommendation_logs_store (store_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 2 & 4. 幂等地补齐唯一约束与索引（仅当缺失时才创建）
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS neko_v003_upgrade;
CREATE PROCEDURE neko_v003_upgrade()
BEGIN
  -- 兜底：确保"同用户同门店同时段"活跃唯一约束存在（依赖 V002 生成列 user_active_slot_key）
  IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'reservations'
          AND COLUMN_NAME = 'user_active_slot_key'
     )
     AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'reservations'
          AND INDEX_NAME = 'uq_reservations_user_slot'
     ) THEN
    ALTER TABLE reservations
      ADD UNIQUE KEY uq_reservations_user_slot (user_active_slot_key);
  END IF;

  -- 为事件审计补充按目标状态检索的索引（V002 仅建了 reservation_id 维度的索引）
  IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'reservation_events'
          AND INDEX_NAME = 'idx_reservation_events_to_status'
     ) THEN
    ALTER TABLE reservation_events
      ADD KEY idx_reservation_events_to_status (to_status);
  END IF;
END;

CALL neko_v003_upgrade();
DROP PROCEDURE IF EXISTS neko_v003_upgrade;
