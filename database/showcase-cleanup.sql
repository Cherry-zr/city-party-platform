USE city_party_platform;

START TRANSACTION;

SET @showcase_marker = '[CITY_PARTY_SHOWCASE]%';

DELETE FROM system_notice
WHERE content LIKE @showcase_marker
   OR user_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
   OR related_id IN (
     SELECT id FROM activity
     WHERE notes LIKE @showcase_marker
        OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
   );

DELETE FROM report
WHERE content LIKE @showcase_marker
   OR reporter_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
   OR (target_type = 'ACTIVITY' AND target_id IN (
     SELECT id FROM activity
     WHERE notes LIKE @showcase_marker
        OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
   ));

DELETE FROM activity_review
WHERE activity_id IN (
      SELECT id FROM activity
      WHERE notes LIKE @showcase_marker
         OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
   )
   OR reviewer_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
   OR target_user_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%');

DELETE FROM aa_bill_item
WHERE bill_id IN (
  SELECT id FROM aa_bill
  WHERE activity_id IN (
    SELECT id FROM activity
    WHERE notes LIKE @showcase_marker
       OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
  )
);

DELETE FROM aa_bill
WHERE activity_id IN (
  SELECT id FROM activity
  WHERE notes LIKE @showcase_marker
     OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
);

DELETE FROM chat_message
WHERE activity_id IN (
      SELECT id FROM activity
      WHERE notes LIKE @showcase_marker
         OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
   )
   OR sender_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%');

DELETE FROM credit_record
WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
   OR (source_type = 'ACTIVITY' AND source_id IN (
     SELECT id FROM activity
     WHERE notes LIKE @showcase_marker
        OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
   ));

DELETE FROM activity_favorite
WHERE activity_id IN (
      SELECT id FROM activity
      WHERE notes LIKE @showcase_marker
         OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
   )
   OR user_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%');

DELETE FROM activity_waitlist
WHERE activity_id IN (
      SELECT id FROM activity
      WHERE notes LIKE @showcase_marker
         OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
   )
   OR user_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%');

DELETE FROM activity_signup
WHERE activity_id IN (
      SELECT id FROM activity
      WHERE notes LIKE @showcase_marker
         OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
   )
   OR user_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%');

DELETE FROM activity_tag
WHERE activity_id IN (
  SELECT id FROM activity
  WHERE notes LIKE @showcase_marker
     OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
);

DELETE FROM activity
WHERE notes LIKE @showcase_marker
   OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%');

DELETE FROM user_interest
WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%');

DELETE FROM user_profile
WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%');

DELETE FROM user
WHERE username LIKE 'cp_showcase_%';

COMMIT;
