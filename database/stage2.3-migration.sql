USE city_party_platform;

CREATE TABLE IF NOT EXISTS activity_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  target_user_id BIGINT NOT NULL,
  rating INT NOT NULL,
  content VARCHAR(500) NULL,
  tags VARCHAR(255) NULL,
  credit_delta INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_review_activity_reviewer_target (activity_id, reviewer_id, target_user_id),
  INDEX idx_review_activity_time (activity_id, created_at),
  INDEX idx_review_reviewer_time (reviewer_id, created_at),
  INDEX idx_review_target_time (target_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='activity review';

CREATE TABLE IF NOT EXISTS credit_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  change_score INT NOT NULL,
  before_score INT NOT NULL,
  after_score INT NOT NULL,
  reason VARCHAR(255) NOT NULL,
  source_type VARCHAR(50) NULL,
  source_id BIGINT NULL,
  created_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_credit_user_time (user_id, created_at),
  INDEX idx_credit_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='credit record';

SET @target_user_id_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_review'
    AND COLUMN_NAME = 'target_user_id'
);
SET @alter_target_user_id_sql = IF(
  @target_user_id_exists = 0,
  'ALTER TABLE activity_review ADD COLUMN target_user_id BIGINT NULL AFTER reviewer_id',
  'SELECT 1'
);
PREPARE alter_target_user_id_stmt FROM @alter_target_user_id_sql;
EXECUTE alter_target_user_id_stmt;
DEALLOCATE PREPARE alter_target_user_id_stmt;

SET @rating_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_review'
    AND COLUMN_NAME = 'rating'
);
SET @alter_rating_sql = IF(
  @rating_exists = 0,
  'ALTER TABLE activity_review ADD COLUMN rating INT NULL AFTER target_user_id',
  'SELECT 1'
);
PREPARE alter_rating_stmt FROM @alter_rating_sql;
EXECUTE alter_rating_stmt;
DEALLOCATE PREPARE alter_rating_stmt;

SET @tags_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_review'
    AND COLUMN_NAME = 'tags'
);
SET @alter_tags_sql = IF(
  @tags_exists = 0,
  'ALTER TABLE activity_review ADD COLUMN tags VARCHAR(255) NULL AFTER content',
  'SELECT 1'
);
PREPARE alter_tags_stmt FROM @alter_tags_sql;
EXECUTE alter_tags_stmt;
DEALLOCATE PREPARE alter_tags_stmt;

SET @credit_delta_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_review'
    AND COLUMN_NAME = 'credit_delta'
);
SET @alter_credit_delta_sql = IF(
  @credit_delta_exists = 0,
  'ALTER TABLE activity_review ADD COLUMN credit_delta INT NOT NULL DEFAULT 0 AFTER tags',
  'SELECT 1'
);
PREPARE alter_credit_delta_stmt FROM @alter_credit_delta_sql;
EXECUTE alter_credit_delta_stmt;
DEALLOCATE PREPARE alter_credit_delta_stmt;

SET @reviewed_user_id_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_review'
    AND COLUMN_NAME = 'reviewed_user_id'
);
SET @backfill_target_user_id_sql = IF(
  @reviewed_user_id_exists > 0,
  'UPDATE activity_review SET target_user_id = reviewed_user_id WHERE target_user_id IS NULL',
  'SELECT 1'
);
PREPARE backfill_target_user_id_stmt FROM @backfill_target_user_id_sql;
EXECUTE backfill_target_user_id_stmt;
DEALLOCATE PREPARE backfill_target_user_id_stmt;

SET @overall_score_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_review'
    AND COLUMN_NAME = 'overall_score'
);
SET @backfill_rating_sql = IF(
  @overall_score_exists > 0,
  'UPDATE activity_review SET rating = overall_score WHERE rating IS NULL',
  'SELECT 1'
);
PREPARE backfill_rating_stmt FROM @backfill_rating_sql;
EXECUTE backfill_rating_stmt;
DEALLOCATE PREPARE backfill_rating_stmt;

ALTER TABLE activity_review
  MODIFY COLUMN target_user_id BIGINT NOT NULL,
  MODIFY COLUMN rating INT NOT NULL;

SET @legacy_reviewed_user_id_nullable_sql = IF(
  @reviewed_user_id_exists > 0,
  'ALTER TABLE activity_review MODIFY COLUMN reviewed_user_id BIGINT NULL',
  'SELECT 1'
);
PREPARE legacy_reviewed_user_id_nullable_stmt FROM @legacy_reviewed_user_id_nullable_sql;
EXECUTE legacy_reviewed_user_id_nullable_stmt;
DEALLOCATE PREPARE legacy_reviewed_user_id_nullable_stmt;

SET @punctuality_score_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_review'
    AND COLUMN_NAME = 'punctuality_score'
);
SET @legacy_punctuality_score_nullable_sql = IF(
  @punctuality_score_exists > 0,
  'ALTER TABLE activity_review MODIFY COLUMN punctuality_score INT NULL',
  'SELECT 1'
);
PREPARE legacy_punctuality_score_nullable_stmt FROM @legacy_punctuality_score_nullable_sql;
EXECUTE legacy_punctuality_score_nullable_stmt;
DEALLOCATE PREPARE legacy_punctuality_score_nullable_stmt;

SET @communication_score_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_review'
    AND COLUMN_NAME = 'communication_score'
);
SET @legacy_communication_score_nullable_sql = IF(
  @communication_score_exists > 0,
  'ALTER TABLE activity_review MODIFY COLUMN communication_score INT NULL',
  'SELECT 1'
);
PREPARE legacy_communication_score_nullable_stmt FROM @legacy_communication_score_nullable_sql;
EXECUTE legacy_communication_score_nullable_stmt;
DEALLOCATE PREPARE legacy_communication_score_nullable_stmt;

SET @authenticity_score_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_review'
    AND COLUMN_NAME = 'authenticity_score'
);
SET @legacy_authenticity_score_nullable_sql = IF(
  @authenticity_score_exists > 0,
  'ALTER TABLE activity_review MODIFY COLUMN authenticity_score INT NULL',
  'SELECT 1'
);
PREPARE legacy_authenticity_score_nullable_stmt FROM @legacy_authenticity_score_nullable_sql;
EXECUTE legacy_authenticity_score_nullable_stmt;
DEALLOCATE PREPARE legacy_authenticity_score_nullable_stmt;

SET @legacy_overall_score_nullable_sql = IF(
  @overall_score_exists > 0,
  'ALTER TABLE activity_review MODIFY COLUMN overall_score INT NULL',
  'SELECT 1'
);
PREPARE legacy_overall_score_nullable_stmt FROM @legacy_overall_score_nullable_sql;
EXECUTE legacy_overall_score_nullable_stmt;
DEALLOCATE PREPARE legacy_overall_score_nullable_stmt;

SET @review_unique_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_review'
    AND INDEX_NAME = 'uk_review_activity_reviewer_target'
);
SET @alter_review_unique_index_sql = IF(
  @review_unique_index_exists = 0,
  'ALTER TABLE activity_review ADD UNIQUE KEY uk_review_activity_reviewer_target (activity_id, reviewer_id, target_user_id)',
  'SELECT 1'
);
PREPARE alter_review_unique_index_stmt FROM @alter_review_unique_index_sql;
EXECUTE alter_review_unique_index_stmt;
DEALLOCATE PREPARE alter_review_unique_index_stmt;

SET @review_activity_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_review'
    AND INDEX_NAME = 'idx_review_activity_time'
);
SET @alter_review_activity_index_sql = IF(
  @review_activity_index_exists = 0,
  'ALTER TABLE activity_review ADD INDEX idx_review_activity_time (activity_id, created_at)',
  'SELECT 1'
);
PREPARE alter_review_activity_index_stmt FROM @alter_review_activity_index_sql;
EXECUTE alter_review_activity_index_stmt;
DEALLOCATE PREPARE alter_review_activity_index_stmt;

SET @review_reviewer_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_review'
    AND INDEX_NAME = 'idx_review_reviewer_time'
);
SET @alter_review_reviewer_index_sql = IF(
  @review_reviewer_index_exists = 0,
  'ALTER TABLE activity_review ADD INDEX idx_review_reviewer_time (reviewer_id, created_at)',
  'SELECT 1'
);
PREPARE alter_review_reviewer_index_stmt FROM @alter_review_reviewer_index_sql;
EXECUTE alter_review_reviewer_index_stmt;
DEALLOCATE PREPARE alter_review_reviewer_index_stmt;

SET @review_target_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_review'
    AND INDEX_NAME = 'idx_review_target_time'
);
SET @alter_review_target_index_sql = IF(
  @review_target_index_exists = 0,
  'ALTER TABLE activity_review ADD INDEX idx_review_target_time (target_user_id, created_at)',
  'SELECT 1'
);
PREPARE alter_review_target_index_stmt FROM @alter_review_target_index_sql;
EXECUTE alter_review_target_index_stmt;
DEALLOCATE PREPARE alter_review_target_index_stmt;

SET @credit_deleted_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'credit_record'
    AND COLUMN_NAME = 'deleted'
);
SET @alter_credit_deleted_sql = IF(
  @credit_deleted_exists = 0,
  'ALTER TABLE credit_record ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 AFTER created_at',
  'SELECT 1'
);
PREPARE alter_credit_deleted_stmt FROM @alter_credit_deleted_sql;
EXECUTE alter_credit_deleted_stmt;
DEALLOCATE PREPARE alter_credit_deleted_stmt;

SET @credit_user_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'credit_record'
    AND INDEX_NAME = 'idx_credit_user_time'
);
SET @alter_credit_user_index_sql = IF(
  @credit_user_index_exists = 0,
  'ALTER TABLE credit_record ADD INDEX idx_credit_user_time (user_id, created_at)',
  'SELECT 1'
);
PREPARE alter_credit_user_index_stmt FROM @alter_credit_user_index_sql;
EXECUTE alter_credit_user_index_stmt;
DEALLOCATE PREPARE alter_credit_user_index_stmt;

SET @credit_source_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'credit_record'
    AND INDEX_NAME = 'idx_credit_source'
);
SET @alter_credit_source_index_sql = IF(
  @credit_source_index_exists = 0,
  'ALTER TABLE credit_record ADD INDEX idx_credit_source (source_type, source_id)',
  'SELECT 1'
);
PREPARE alter_credit_source_index_stmt FROM @alter_credit_source_index_sql;
EXECUTE alter_credit_source_index_stmt;
DEALLOCATE PREPARE alter_credit_source_index_stmt;
