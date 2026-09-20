-- Stage 2.9: add an activity-content moderation model without reusing lifecycle status.
-- Back up the target database before running this migration.

USE city_party_platform;

SET @audit_status_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity'
    AND COLUMN_NAME = 'audit_status'
);
SET @add_audit_status_sql = IF(
  @audit_status_exists = 0,
  'ALTER TABLE activity ADD COLUMN audit_status VARCHAR(20) NULL AFTER need_approval',
  'SELECT 1'
);
PREPARE add_audit_status_stmt FROM @add_audit_status_sql;
EXECUTE add_audit_status_stmt;
DEALLOCATE PREPARE add_audit_status_stmt;

SET @reject_reason_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity'
    AND COLUMN_NAME = 'reject_reason'
);
SET @add_reject_reason_sql = IF(
  @reject_reason_exists = 0,
  'ALTER TABLE activity ADD COLUMN reject_reason VARCHAR(500) NULL AFTER audit_status',
  'SELECT 1'
);
PREPARE add_reject_reason_stmt FROM @add_reject_reason_sql;
EXECUTE add_reject_reason_stmt;
DEALLOCATE PREPARE add_reject_reason_stmt;

SET @audit_time_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity'
    AND COLUMN_NAME = 'audit_time'
);
SET @add_audit_time_sql = IF(
  @audit_time_exists = 0,
  'ALTER TABLE activity ADD COLUMN audit_time DATETIME NULL AFTER reject_reason',
  'SELECT 1'
);
PREPARE add_audit_time_stmt FROM @add_audit_time_sql;
EXECUTE add_audit_time_stmt;
DEALLOCATE PREPARE add_audit_time_stmt;

SET @reviewer_id_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity'
    AND COLUMN_NAME = 'reviewer_id'
);
SET @add_reviewer_id_sql = IF(
  @reviewer_id_exists = 0,
  'ALTER TABLE activity ADD COLUMN reviewer_id BIGINT NULL AFTER audit_time',
  'SELECT 1'
);
PREPARE add_reviewer_id_stmt FROM @add_reviewer_id_sql;
EXECUTE add_reviewer_id_stmt;
DEALLOCATE PREPARE add_reviewer_id_stmt;

-- Existing activities were already public before moderation existed. Preserve that behavior
-- and use created_at only as a compatibility audit timestamp.
UPDATE activity
SET audit_status = 'APPROVED',
    reject_reason = NULL,
    audit_time = COALESCE(created_at, NOW()),
    reviewer_id = NULL
WHERE audit_status IS NULL;

ALTER TABLE activity
  MODIFY COLUMN audit_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    COMMENT 'PENDING/APPROVED/REJECTED';

SET @public_audit_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity'
    AND INDEX_NAME = 'idx_activity_public_audit_time'
);
SET @add_public_audit_index_sql = IF(
  @public_audit_index_exists = 0,
  'ALTER TABLE activity ADD INDEX idx_activity_public_audit_time (audit_status, deleted, audit_time, id)',
  'SELECT 1'
);
PREPARE add_public_audit_index_stmt FROM @add_public_audit_index_sql;
EXECUTE add_public_audit_index_stmt;
DEALLOCATE PREPARE add_public_audit_index_stmt;

SET @public_category_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity'
    AND INDEX_NAME = 'idx_activity_public_category_time'
);
SET @add_public_category_index_sql = IF(
  @public_category_index_exists = 0,
  'ALTER TABLE activity ADD INDEX idx_activity_public_category_time (audit_status, deleted, category, audit_time, id)',
  'SELECT 1'
);
PREPARE add_public_category_index_stmt FROM @add_public_category_index_sql;
EXECUTE add_public_category_index_stmt;
DEALLOCATE PREPARE add_public_category_index_stmt;
