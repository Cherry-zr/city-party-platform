USE city_party_platform;

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  sender_nickname VARCHAR(80) NULL,
  sender_avatar VARCHAR(500) NULL,
  content VARCHAR(1000) NOT NULL,
  message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
  created_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_chat_activity_time (activity_id, created_at),
  INDEX idx_chat_sender (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='chat message';

SET @sender_nickname_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'chat_message'
    AND COLUMN_NAME = 'sender_nickname'
);
SET @alter_sender_nickname_sql = IF(
  @sender_nickname_exists = 0,
  'ALTER TABLE chat_message ADD COLUMN sender_nickname VARCHAR(80) NULL AFTER sender_id',
  'SELECT 1'
);
PREPARE alter_sender_nickname_stmt FROM @alter_sender_nickname_sql;
EXECUTE alter_sender_nickname_stmt;
DEALLOCATE PREPARE alter_sender_nickname_stmt;

SET @sender_avatar_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'chat_message'
    AND COLUMN_NAME = 'sender_avatar'
);
SET @alter_sender_avatar_sql = IF(
  @sender_avatar_exists = 0,
  'ALTER TABLE chat_message ADD COLUMN sender_avatar VARCHAR(500) NULL AFTER sender_nickname',
  'SELECT 1'
);
PREPARE alter_sender_avatar_stmt FROM @alter_sender_avatar_sql;
EXECUTE alter_sender_avatar_stmt;
DEALLOCATE PREPARE alter_sender_avatar_stmt;

SET @message_type_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'chat_message'
    AND COLUMN_NAME = 'message_type'
);
SET @alter_message_type_sql = IF(
  @message_type_exists = 0,
  'ALTER TABLE chat_message ADD COLUMN message_type VARCHAR(20) NOT NULL DEFAULT ''TEXT'' AFTER content',
  'SELECT 1'
);
PREPARE alter_message_type_stmt FROM @alter_message_type_sql;
EXECUTE alter_message_type_stmt;
DEALLOCATE PREPARE alter_message_type_stmt;

SET @idx_chat_activity_time_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'chat_message'
    AND INDEX_NAME = 'idx_chat_activity_time'
);
SET @alter_idx_chat_activity_time_sql = IF(
  @idx_chat_activity_time_exists = 0,
  'ALTER TABLE chat_message ADD INDEX idx_chat_activity_time (activity_id, created_at)',
  'SELECT 1'
);
PREPARE alter_idx_chat_activity_time_stmt FROM @alter_idx_chat_activity_time_sql;
EXECUTE alter_idx_chat_activity_time_stmt;
DEALLOCATE PREPARE alter_idx_chat_activity_time_stmt;

SET @idx_chat_sender_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'chat_message'
    AND INDEX_NAME = 'idx_chat_sender'
);
SET @alter_idx_chat_sender_sql = IF(
  @idx_chat_sender_exists = 0,
  'ALTER TABLE chat_message ADD INDEX idx_chat_sender (sender_id)',
  'SELECT 1'
);
PREPARE alter_idx_chat_sender_stmt FROM @alter_idx_chat_sender_sql;
EXECUTE alter_idx_chat_sender_stmt;
DEALLOCATE PREPARE alter_idx_chat_sender_stmt;
