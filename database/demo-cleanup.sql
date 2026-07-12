USE city_party_platform;

START TRANSACTION;

SET @demo_marker = '[CITY_PARTY_DEMO]%';

DELETE FROM system_notice
WHERE title LIKE @demo_marker
   OR user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');

DELETE FROM report
WHERE content LIKE @demo_marker
   OR reporter_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');

DELETE FROM activity_review
WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker)
   OR reviewer_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%')
   OR target_user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');

DELETE FROM aa_bill_item WHERE bill_id IN (
  SELECT id FROM aa_bill WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker)
);
DELETE FROM aa_bill WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker);
DELETE FROM chat_message
WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker)
   OR sender_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM credit_record
WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%')
   OR (source_type = 'ACTIVITY' AND source_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker));
DELETE FROM activity_favorite WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker)
   OR user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM activity_waitlist WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker)
   OR user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM activity_signup WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker)
   OR user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM activity_tag WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker);
DELETE FROM activity WHERE title LIKE @demo_marker
   OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM user_interest WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM user_profile WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM user WHERE username LIKE 'cp_demo_%';
DELETE FROM interest_tag WHERE name LIKE '[CITY_PARTY_DEMO]%';

COMMIT;
