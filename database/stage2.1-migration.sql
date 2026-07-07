USE city_party_platform;

CREATE TABLE IF NOT EXISTS activity_waitlist (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING/PROMOTED/CANCELLED',
  queue_no BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_waitlist_activity_user (activity_id, user_id),
  INDEX idx_waitlist_activity_status (activity_id, status),
  INDEX idx_waitlist_user (user_id),
  INDEX idx_waitlist_queue (activity_id, queue_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动候补队列表';

SET @related_id_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'system_notice'
    AND COLUMN_NAME = 'related_id'
);

SET @alter_related_id_sql = IF(
  @related_id_exists = 0,
  'ALTER TABLE system_notice ADD COLUMN related_id BIGINT NULL AFTER content',
  'SELECT 1'
);

PREPARE alter_related_id_stmt FROM @alter_related_id_sql;
EXECUTE alter_related_id_stmt;
DEALLOCATE PREPARE alter_related_id_stmt;
