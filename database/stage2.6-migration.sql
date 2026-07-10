USE city_party_platform;

SET @signup_activity_user_unique_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_signup'
    AND INDEX_NAME = 'uk_signup_activity_user'
);
SET @add_signup_activity_user_unique_sql = IF(
  @signup_activity_user_unique_exists = 0,
  'ALTER TABLE activity_signup ADD UNIQUE KEY uk_signup_activity_user (activity_id, user_id)',
  'SELECT 1'
);
PREPARE add_signup_activity_user_unique_stmt FROM @add_signup_activity_user_unique_sql;
EXECUTE add_signup_activity_user_unique_stmt;
DEALLOCATE PREPARE add_signup_activity_user_unique_stmt;
