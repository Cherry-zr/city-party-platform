USE city_party_platform;

DROP PROCEDURE IF EXISTS stage2_6_ensure_signup_activity_user_unique;

DELIMITER $$

CREATE PROCEDURE stage2_6_ensure_signup_activity_user_unique()
BEGIN
  DECLARE named_index_rows INT DEFAULT 0;
  DECLARE named_index_non_unique INT DEFAULT 1;
  DECLARE named_index_columns VARCHAR(255) DEFAULT NULL;
  DECLARE equivalent_index_count INT DEFAULT 0;

  SELECT COUNT(*),
         COALESCE(MAX(NON_UNIQUE), 1),
         GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',')
    INTO named_index_rows, named_index_non_unique, named_index_columns
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_signup'
    AND INDEX_NAME = 'uk_signup_activity_user';

  IF named_index_rows > 0 THEN
    IF named_index_rows <> 2
       OR named_index_non_unique <> 0
       OR named_index_columns <> 'activity_id,user_id' THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'uk_signup_activity_user exists with an unexpected definition';
    END IF;
  ELSE
    SELECT COUNT(*)
      INTO equivalent_index_count
    FROM (
      SELECT INDEX_NAME
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'activity_signup'
        AND NON_UNIQUE = 0
      GROUP BY INDEX_NAME
      HAVING COUNT(*) = 2
         AND GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') = 'activity_id,user_id'
    ) AS equivalent_indexes;

    IF equivalent_index_count > 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Equivalent activity signup unique index exists with an unexpected name';
    ELSE
      ALTER TABLE activity_signup
        ADD UNIQUE KEY uk_signup_activity_user (activity_id, user_id);
    END IF;
  END IF;
END$$

DELIMITER ;

CALL stage2_6_ensure_signup_activity_user_unique();
DROP PROCEDURE IF EXISTS stage2_6_ensure_signup_activity_user_unique;
