USE city_party_platform;

SET @signup_user_status_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_signup'
    AND INDEX_NAME = 'idx_signup_user_status_activity'
);
SET @add_signup_user_status_index_sql = IF(
  @signup_user_status_index_exists = 0,
  'ALTER TABLE activity_signup ADD INDEX idx_signup_user_status_activity (user_id, status, deleted, activity_id)',
  'SELECT 1'
);
PREPARE add_signup_user_status_index_stmt FROM @add_signup_user_status_index_sql;
EXECUTE add_signup_user_status_index_stmt;
DEALLOCATE PREPARE add_signup_user_status_index_stmt;

SET @waitlist_user_status_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_waitlist'
    AND INDEX_NAME = 'idx_waitlist_user_status_activity'
);
SET @add_waitlist_user_status_index_sql = IF(
  @waitlist_user_status_index_exists = 0,
  'ALTER TABLE activity_waitlist ADD INDEX idx_waitlist_user_status_activity (user_id, status, deleted, activity_id)',
  'SELECT 1'
);
PREPARE add_waitlist_user_status_index_stmt FROM @add_waitlist_user_status_index_sql;
EXECUTE add_waitlist_user_status_index_stmt;
DEALLOCATE PREPARE add_waitlist_user_status_index_stmt;

SET @notice_user_read_time_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'system_notice'
    AND INDEX_NAME = 'idx_notice_user_read_time'
);
SET @add_notice_user_read_time_index_sql = IF(
  @notice_user_read_time_index_exists = 0,
  'ALTER TABLE system_notice ADD INDEX idx_notice_user_read_time (user_id, read_flag, deleted, created_at)',
  'SELECT 1'
);
PREPARE add_notice_user_read_time_stmt FROM @add_notice_user_read_time_index_sql;
EXECUTE add_notice_user_read_time_stmt;
DEALLOCATE PREPARE add_notice_user_read_time_stmt;
