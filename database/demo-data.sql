USE city_party_platform;

START TRANSACTION;

-- Repeatable import: remove only records owned by the cp_demo_ users or title marker.
SET @demo_marker = '[CITY_PARTY_DEMO]%';
DELETE FROM system_notice WHERE title LIKE @demo_marker OR user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM report WHERE content LIKE @demo_marker OR reporter_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM activity_review WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker)
  OR reviewer_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%') OR target_user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM aa_bill_item WHERE bill_id IN (SELECT id FROM aa_bill WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker));
DELETE FROM aa_bill WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker);
DELETE FROM chat_message WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker) OR sender_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM credit_record WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%')
  OR (source_type='ACTIVITY' AND source_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker));
DELETE FROM activity_favorite WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker) OR user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM activity_waitlist WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker) OR user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM activity_signup WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker) OR user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM activity_tag WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE @demo_marker);
DELETE FROM activity WHERE title LIKE @demo_marker OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM user_interest WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM user_profile WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'cp_demo_%');
DELETE FROM user WHERE username LIKE 'cp_demo_%';
DELETE FROM interest_tag WHERE name LIKE '[CITY_PARTY_DEMO]%';

SET @demo_password_hash = 'pbkdf2$120000$0102030405060708090a0b0c0d0e0f10$17f694190d6cbba30002f23723608c55af175158cfa45c7ca2a8940ada1d47aa';

INSERT INTO user (username,phone,password_hash,role,status,credit_score,created_at,updated_at,deleted) VALUES
('cp_demo_admin',NULL,@demo_password_hash,'ADMIN','NORMAL',100,DATE_SUB(NOW(),INTERVAL 75 DAY),NOW(),0),
('cp_demo_host_a',NULL,@demo_password_hash,'USER','NORMAL',108,DATE_SUB(NOW(),INTERVAL 45 DAY),NOW(),0),
('cp_demo_host_b',NULL,@demo_password_hash,'USER','NORMAL',95,DATE_SUB(NOW(),INTERVAL 20 DAY),NOW(),0),
('cp_demo_member_a',NULL,@demo_password_hash,'USER','NORMAL',72,DATE_SUB(NOW(),INTERVAL 8 DAY),NOW(),0),
('cp_demo_member_b',NULL,@demo_password_hash,'USER','NORMAL',102,DATE_SUB(NOW(),INTERVAL 3 DAY),NOW(),0),
('cp_demo_member_c',NULL,@demo_password_hash,'USER','NORMAL',88,DATE_SUB(NOW(),INTERVAL 1 DAY),NOW(),0);

SET @admin_id=(SELECT id FROM user WHERE username='cp_demo_admin');
SET @host_a=(SELECT id FROM user WHERE username='cp_demo_host_a');
SET @host_b=(SELECT id FROM user WHERE username='cp_demo_host_b');
SET @member_a=(SELECT id FROM user WHERE username='cp_demo_member_a');
SET @member_b=(SELECT id FROM user WHERE username='cp_demo_member_b');
SET @member_c=(SELECT id FROM user WHERE username='cp_demo_member_c');

INSERT INTO user_profile (user_id,nickname,avatar_url,city,bio,created_at,updated_at,deleted) VALUES
(@admin_id,'演示管理员',NULL,'北京','[CITY_PARTY_DEMO] 平台运营演示账号',DATE_SUB(NOW(),INTERVAL 75 DAY),NOW(),0),
(@host_a,'演示发起人 A',NULL,'北京','[CITY_PARTY_DEMO] 户外与桌游活动发起人',DATE_SUB(NOW(),INTERVAL 45 DAY),NOW(),0),
(@host_b,'演示发起人 B',NULL,'上海','[CITY_PARTY_DEMO] 学习与探店活动发起人',DATE_SUB(NOW(),INTERVAL 20 DAY),NOW(),0),
(@member_a,'演示成员 A',NULL,'北京','[CITY_PARTY_DEMO] 普通参与者',DATE_SUB(NOW(),INTERVAL 8 DAY),NOW(),0),
(@member_b,'演示成员 B',NULL,'上海','[CITY_PARTY_DEMO] 普通参与者',DATE_SUB(NOW(),INTERVAL 3 DAY),NOW(),0),
(@member_c,'演示成员 C',NULL,'杭州','[CITY_PARTY_DEMO] 普通参与者',DATE_SUB(NOW(),INTERVAL 1 DAY),NOW(),0);

INSERT INTO interest_tag (name,sort_order,created_at) VALUES
('[CITY_PARTY_DEMO] 周末',901,NOW()),('[CITY_PARTY_DEMO] 新手友好',902,NOW()),('[CITY_PARTY_DEMO] 轻社交',903,NOW());
SET @tag_weekend=(SELECT id FROM interest_tag WHERE name='[CITY_PARTY_DEMO] 周末');
SET @tag_beginner=(SELECT id FROM interest_tag WHERE name='[CITY_PARTY_DEMO] 新手友好');
SET @tag_social=(SELECT id FROM interest_tag WHERE name='[CITY_PARTY_DEMO] 轻社交');
INSERT INTO user_interest (user_id,tag_id,created_at) VALUES
(@host_a,@tag_weekend,NOW()),(@host_a,@tag_beginner,NOW()),(@host_b,@tag_social,NOW()),(@member_a,@tag_beginner,NOW()),(@member_b,@tag_social,NOW());

INSERT INTO activity (creator_id,title,category,tags,start_time,end_time,signup_deadline,city,address,longitude,latitude,min_participants,max_participants,cost_type,cost_amount,aa_rule,cover_url,description,notes,need_approval,status,approved_count,favorite_count,created_at,updated_at,deleted) VALUES
(@host_a,'[CITY_PARTY_DEMO] 城市徒步回顾','户外','周末,新手友好',DATE_SUB(NOW(),INTERVAL 50 DAY),DATE_ADD(DATE_SUB(NOW(),INTERVAL 50 DAY),INTERVAL 3 HOUR),DATE_SUB(NOW(),INTERVAL 51 DAY),'北京','演示地点 A',116.400000,39.900000,2,8,'FREE',0,NULL,NULL,'[CITY_PARTY_DEMO] 已结束活动，用于评价和完成报名演示',NULL,0,'FINISHED',3,1,DATE_SUB(NOW(),INTERVAL 60 DAY),NOW(),0),
(@host_b,'[CITY_PARTY_DEMO] 取消的观影局','观影','轻社交',DATE_SUB(NOW(),INTERVAL 20 DAY),DATE_ADD(DATE_SUB(NOW(),INTERVAL 20 DAY),INTERVAL 2 HOUR),DATE_SUB(NOW(),INTERVAL 21 DAY),'上海','演示地点 B',121.470000,31.230000,2,6,'AA',60,'演示 AA 规则',NULL,'[CITY_PARTY_DEMO] 已取消活动',NULL,1,'CANCELLED',0,0,DATE_SUB(NOW(),INTERVAL 35 DAY),NOW(),0),
(@host_a,'[CITY_PARTY_DEMO] 桌游满员局','桌游','新手友好,轻社交',DATE_ADD(NOW(),INTERVAL 3 DAY),DATE_ADD(DATE_ADD(NOW(),INTERVAL 3 DAY),INTERVAL 3 HOUR),DATE_ADD(NOW(),INTERVAL 2 DAY),'北京','演示地点 C',116.320000,39.980000,3,3,'AA',45,'演示 AA 规则',NULL,'[CITY_PARTY_DEMO] 满员并存在候补',NULL,0,'FULL',3,2,DATE_SUB(NOW(),INTERVAL 14 DAY),NOW(),0),
(@host_b,'[CITY_PARTY_DEMO] 周末自习室','学习','周末,安静',DATE_ADD(NOW(),INTERVAL 5 DAY),DATE_ADD(DATE_ADD(NOW(),INTERVAL 5 DAY),INTERVAL 6 HOUR),DATE_ADD(NOW(),INTERVAL 4 DAY),'杭州','演示地点 D',120.150000,30.280000,2,10,'FREE',0,NULL,NULL,'[CITY_PARTY_DEMO] 报名中活动',NULL,1,'SIGNING',1,0,DATE_SUB(NOW(),INTERVAL 7 DAY),NOW(),0),
(@host_b,'[CITY_PARTY_DEMO] 咖啡探店','探店','低预算,轻社交',DATE_ADD(NOW(),INTERVAL 7 DAY),DATE_ADD(DATE_ADD(NOW(),INTERVAL 7 DAY),INTERVAL 2 HOUR),DATE_ADD(NOW(),INTERVAL 6 DAY),'上海','演示地点 E',121.450000,31.220000,2,5,'ESTIMATE',55,NULL,NULL,'[CITY_PARTY_DEMO] 即将开始活动',NULL,0,'UPCOMING',1,1,DATE_SUB(NOW(),INTERVAL 2 DAY),NOW(),0);

SET @finished=(SELECT id FROM activity WHERE title='[CITY_PARTY_DEMO] 城市徒步回顾');
SET @cancelled=(SELECT id FROM activity WHERE title='[CITY_PARTY_DEMO] 取消的观影局');
SET @full=(SELECT id FROM activity WHERE title='[CITY_PARTY_DEMO] 桌游满员局');
SET @signing=(SELECT id FROM activity WHERE title='[CITY_PARTY_DEMO] 周末自习室');
SET @upcoming=(SELECT id FROM activity WHERE title='[CITY_PARTY_DEMO] 咖啡探店');

INSERT INTO activity_tag (activity_id,tag_name,created_at) VALUES
(@finished,'新手友好',NOW()),(@finished,'周末',NOW()),(@cancelled,'轻社交',NOW()),(@full,'桌游',NOW()),(@full,'新手友好',NOW()),(@signing,'学习',NOW()),(@upcoming,'探店',NOW());

INSERT INTO activity_signup (activity_id,user_id,status,apply_message,reviewed_at,created_at,updated_at,deleted) VALUES
(@finished,@member_a,'COMPLETED','[CITY_PARTY_DEMO] 已完成报名',DATE_SUB(NOW(),INTERVAL 51 DAY),DATE_SUB(NOW(),INTERVAL 55 DAY),NOW(),0),
(@finished,@member_b,'COMPLETED','[CITY_PARTY_DEMO] 已完成报名',DATE_SUB(NOW(),INTERVAL 51 DAY),DATE_SUB(NOW(),INTERVAL 54 DAY),NOW(),0),
(@cancelled,@member_a,'CANCELLED','[CITY_PARTY_DEMO] 主动退出',NULL,DATE_SUB(NOW(),INTERVAL 30 DAY),NOW(),0),
(@full,@member_a,'APPROVED','[CITY_PARTY_DEMO] 报名成功',DATE_SUB(NOW(),INTERVAL 10 DAY),DATE_SUB(NOW(),INTERVAL 12 DAY),NOW(),0),
(@full,@member_b,'APPROVED','[CITY_PARTY_DEMO] 报名成功',DATE_SUB(NOW(),INTERVAL 9 DAY),DATE_SUB(NOW(),INTERVAL 11 DAY),NOW(),0),
(@full,@member_c,'WAITING','[CITY_PARTY_DEMO] 候补报名',NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),NOW(),0),
(@signing,@member_a,'PENDING','[CITY_PARTY_DEMO] 待审核',NULL,DATE_SUB(NOW(),INTERVAL 4 DAY),NOW(),0),
(@signing,@member_c,'REJECTED','[CITY_PARTY_DEMO] 已拒绝',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),NOW(),0),
(@upcoming,@member_b,'APPROVED','[CITY_PARTY_DEMO] 报名成功',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),NOW(),0);

INSERT INTO activity_waitlist (activity_id,user_id,status,queue_no,created_at,updated_at,deleted) VALUES
(@full,@member_c,'WAITING',1,DATE_SUB(NOW(),INTERVAL 5 DAY),NOW(),0);

INSERT INTO activity_favorite (user_id,activity_id,created_at,deleted) VALUES
(@member_a,@full,DATE_SUB(NOW(),INTERVAL 10 DAY),0),(@member_b,@upcoming,DATE_SUB(NOW(),INTERVAL 1 DAY),0);

INSERT INTO activity_review (activity_id,reviewer_id,target_user_id,rating,content,tags,credit_delta,created_at,deleted) VALUES
(@finished,@member_a,@host_a,5,'[CITY_PARTY_DEMO] 组织清晰','守时,友好',2,DATE_SUB(NOW(),INTERVAL 48 DAY),0),
(@finished,@member_b,@host_a,4,'[CITY_PARTY_DEMO] 体验良好','友好',1,DATE_SUB(NOW(),INTERVAL 47 DAY),0),
(@finished,@host_a,@member_a,3,'[CITY_PARTY_DEMO] 正常参与','参与',0,DATE_SUB(NOW(),INTERVAL 46 DAY),0);

INSERT INTO credit_record (user_id,change_score,before_score,after_score,reason,source_type,source_id,created_at,deleted) VALUES
(@member_a,-28,100,72,'[CITY_PARTY_DEMO] 多次临时退出','ACTIVITY',@cancelled,DATE_SUB(NOW(),INTERVAL 25 DAY),0),
(@host_a,8,100,108,'[CITY_PARTY_DEMO] 优质组织评价','ACTIVITY',@finished,DATE_SUB(NOW(),INTERVAL 45 DAY),0),
(@member_c,-12,100,88,'[CITY_PARTY_DEMO] 未按时确认','ACTIVITY',@full,DATE_SUB(NOW(),INTERVAL 4 DAY),0);

INSERT INTO system_notice (user_id,type,title,content,related_id,read_flag,created_at,deleted) VALUES
(@member_c,'WAITLIST_JOINED','[CITY_PARTY_DEMO] 已加入候补','演示候补通知',@full,0,DATE_SUB(NOW(),INTERVAL 5 DAY),0),
(@member_b,'SIGNUP_APPROVED','[CITY_PARTY_DEMO] 报名成功','演示报名通知',@upcoming,1,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(@host_a,'REVIEW_RECEIVED','[CITY_PARTY_DEMO] 收到新评价','演示评价通知',@finished,0,DATE_SUB(NOW(),INTERVAL 46 DAY),0);

INSERT INTO chat_message (activity_id,sender_id,sender_nickname,sender_avatar,content,message_type,created_at,deleted) VALUES
(@full,@host_a,'演示发起人 A',NULL,'[CITY_PARTY_DEMO] 欢迎参加桌游活动','TEXT',DATE_SUB(NOW(),INTERVAL 8 DAY),0),
(@full,@member_a,'演示成员 A',NULL,'[CITY_PARTY_DEMO] 已收到活动说明','TEXT',DATE_SUB(NOW(),INTERVAL 7 DAY),0),
(@finished,@member_b,'演示成员 B',NULL,'[CITY_PARTY_DEMO] 感谢组织','TEXT',DATE_SUB(NOW(),INTERVAL 49 DAY),0);

INSERT INTO report (reporter_id,target_type,target_id,reason,content,status,created_at,updated_at,deleted) VALUES
(@member_a,'ACTIVITY',@cancelled,'演示举报','[CITY_PARTY_DEMO] 演示举报记录','RESOLVED',DATE_SUB(NOW(),INTERVAL 22 DAY),NOW(),0);

COMMIT;
