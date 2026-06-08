SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

ALTER TABLE stores
  ADD COLUMN IF NOT EXISTS business_hours_text VARCHAR(128) NULL AFTER close_time,
  ADD COLUMN IF NOT EXISTS photo_url VARCHAR(255) NULL AFTER business_hours_text,
  ADD COLUMN IF NOT EXISTS equipment_desc TEXT NULL AFTER photo_url,
  ADD COLUMN IF NOT EXISTS area_detail TEXT NULL AFTER equipment_desc;

ALTER TABLE dining_tables
  ADD COLUMN IF NOT EXISTS photo_url VARCHAR(255) NULL AFTER status,
  ADD COLUMN IF NOT EXISTS area_detail VARCHAR(255) NULL AFTER photo_url,
  ADD COLUMN IF NOT EXISTS device_note VARCHAR(255) NULL AFTER area_detail;
