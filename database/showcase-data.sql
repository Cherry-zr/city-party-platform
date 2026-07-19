USE city_party_platform;

START TRANSACTION;

-- Repeatable import: remove only records owned by showcase users or the showcase marker.
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
DELETE FROM aa_bill_item WHERE bill_id IN (
  SELECT id FROM aa_bill WHERE activity_id IN (
    SELECT id FROM activity
    WHERE notes LIKE @showcase_marker
       OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
  )
);
DELETE FROM aa_bill WHERE activity_id IN (
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
DELETE FROM activity_tag WHERE activity_id IN (
  SELECT id FROM activity
  WHERE notes LIKE @showcase_marker
     OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%')
);
DELETE FROM activity
WHERE notes LIKE @showcase_marker
   OR creator_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%');
DELETE FROM user_interest WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%');
DELETE FROM user_profile WHERE user_id IN (SELECT id FROM user WHERE username LIKE 'cp_showcase_%');
DELETE FROM user WHERE username LIKE 'cp_showcase_%';

-- `npm run showcase:seed` sets this session variable from SHOWCASE_PASSWORD.
-- The opaque fallback keeps direct SQL imports valid but is not a documented login credential.
SET @showcase_password_hash = COALESCE(
  NULLIF(@showcase_password_hash, ''),
  'pbkdf2$120000$cc1c29a2b916048d579aac3772487a3d$0d12b02da25db36930acb02d9f05eb006557d18238a4fd93708f9a442aaa5300'
);

INSERT INTO user (username,phone,password_hash,role,status,credit_score,created_at,updated_at,deleted) VALUES
('cp_showcase_admin',NULL,@showcase_password_hash,'ADMIN','NORMAL',100,DATE_SUB(NOW(),INTERVAL 28 DAY),NOW(),0),
('cp_showcase_host_a',NULL,@showcase_password_hash,'USER','NORMAL',112,DATE_SUB(NOW(),INTERVAL 21 DAY),NOW(),0),
('cp_showcase_host_b',NULL,@showcase_password_hash,'USER','NORMAL',106,DATE_SUB(NOW(),INTERVAL 14 DAY),NOW(),0),
('cp_showcase_member_a',NULL,@showcase_password_hash,'USER','NORMAL',103,DATE_SUB(NOW(),INTERVAL 10 DAY),NOW(),0),
('cp_showcase_member_b',NULL,@showcase_password_hash,'USER','NORMAL',98,DATE_SUB(NOW(),INTERVAL 7 DAY),NOW(),0),
('cp_showcase_member_c',NULL,@showcase_password_hash,'USER','NORMAL',91,DATE_SUB(NOW(),INTERVAL 4 DAY),NOW(),0),
('cp_showcase_member_d',NULL,@showcase_password_hash,'USER','NORMAL',105,DATE_SUB(NOW(),INTERVAL 2 DAY),NOW(),0),
('cp_showcase_member_e',NULL,@showcase_password_hash,'USER','NORMAL',96,DATE_SUB(NOW(),INTERVAL 1 DAY),NOW(),0);

SET @showcase_admin=(SELECT id FROM user WHERE username='cp_showcase_admin');
SET @showcase_host_a=(SELECT id FROM user WHERE username='cp_showcase_host_a');
SET @showcase_host_b=(SELECT id FROM user WHERE username='cp_showcase_host_b');
SET @showcase_member_a=(SELECT id FROM user WHERE username='cp_showcase_member_a');
SET @showcase_member_b=(SELECT id FROM user WHERE username='cp_showcase_member_b');
SET @showcase_member_c=(SELECT id FROM user WHERE username='cp_showcase_member_c');
SET @showcase_member_d=(SELECT id FROM user WHERE username='cp_showcase_member_d');
SET @showcase_member_e=(SELECT id FROM user WHERE username='cp_showcase_member_e');

INSERT INTO user_profile (user_id,nickname,avatar_url,city,bio,created_at,updated_at,deleted) VALUES
(@showcase_admin,'本地演示管理员','/showcase/avatars/admin.svg','北京','[CITY_PARTY_SHOWCASE] 仅用于本地作品展示',DATE_SUB(NOW(),INTERVAL 28 DAY),NOW(),0),
(@showcase_host_a,'林澈','/showcase/avatars/host-lin.svg','北京','[CITY_PARTY_SHOWCASE] 运动与桌游活动发起人',DATE_SUB(NOW(),INTERVAL 21 DAY),NOW(),0),
(@showcase_host_b,'周屿','/showcase/avatars/host-zhou.svg','北京','[CITY_PARTY_SHOWCASE] 摄影与学习活动发起人',DATE_SUB(NOW(),INTERVAL 14 DAY),NOW(),0),
(@showcase_member_a,'陈安','/showcase/avatars/member-chen.svg','北京','[CITY_PARTY_SHOWCASE] 本地展示参与者',DATE_SUB(NOW(),INTERVAL 10 DAY),NOW(),0),
(@showcase_member_b,'苏宁','/showcase/avatars/member-su.svg','北京','[CITY_PARTY_SHOWCASE] 本地展示参与者',DATE_SUB(NOW(),INTERVAL 7 DAY),NOW(),0),
(@showcase_member_c,'程野','/showcase/avatars/member-cheng.svg','北京','[CITY_PARTY_SHOWCASE] 本地展示参与者',DATE_SUB(NOW(),INTERVAL 4 DAY),NOW(),0),
(@showcase_member_d,'许禾','/showcase/avatars/member-xu.svg','北京','[CITY_PARTY_SHOWCASE] 本地展示参与者',DATE_SUB(NOW(),INTERVAL 2 DAY),NOW(),0),
(@showcase_member_e,'沈星','/showcase/avatars/member-shen.svg','北京','[CITY_PARTY_SHOWCASE] 本地展示参与者',DATE_SUB(NOW(),INTERVAL 1 DAY),NOW(),0);

INSERT INTO activity (creator_id,title,category,tags,start_time,end_time,signup_deadline,city,address,longitude,latitude,min_participants,max_participants,cost_type,cost_amount,aa_rule,cover_url,description,notes,need_approval,status,approved_count,favorite_count,created_at,updated_at,deleted) VALUES
(@showcase_host_a,'周末羽毛球搭子','运动','羽毛球,新手友好,周末',DATE_ADD(NOW(),INTERVAL 2 DAY),DATE_ADD(DATE_ADD(NOW(),INTERVAL 2 DAY),INTERVAL 2 HOUR),DATE_ADD(NOW(),INTERVAL 1 DAY),'北京','天坛体育活动中心',116.410800,39.882300,4,8,'AA',45,'场地和球费到场后均摊','/showcase/covers/badminton.svg','周末轻松打球，提供基础热身与双打轮换，新手也可以参加。','[CITY_PARTY_SHOWCASE] badminton',1,'SIGNING',4,6,DATE_SUB(NOW(),INTERVAL 10 MINUTE),NOW(),0),
(@showcase_host_a,'东城桌游新手局','桌游','桌游,新手友好,轻社交',DATE_ADD(NOW(),INTERVAL 4 DAY),DATE_ADD(DATE_ADD(NOW(),INTERVAL 4 DAY),INTERVAL 3 HOUR),DATE_ADD(NOW(),INTERVAL 3 DAY),'北京','东四共享活动空间',116.424700,39.918600,3,5,'AA',58,'场地与饮品费用现场均摊','/showcase/covers/board-game.svg','准备了规则简单、互动友好的桌游，主持人会带领第一轮教学。','[CITY_PARTY_SHOWCASE] board-game',0,'FULL',5,8,DATE_SUB(NOW(),INTERVAL 20 MINUTE),NOW(),0),
(@showcase_host_b,'城市摄影漫步','户外','摄影,城市漫步,日落',DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_ADD(DATE_SUB(NOW(),INTERVAL 3 DAY),INTERVAL 3 HOUR),DATE_SUB(NOW(),INTERVAL 4 DAY),'北京','景山公园东门',116.395600,39.925100,3,8,'FREE',0,NULL,'/showcase/covers/photowalk.svg','沿城市中轴线进行轻量摄影漫步，手机和相机都可以参加。','[CITY_PARTY_SHOWCASE] photowalk',0,'FINISHED',4,7,DATE_SUB(NOW(),INTERVAL 30 MINUTE),NOW(),0),
(@showcase_host_b,'周末电影观影交流','观影','电影,映后交流,周末',DATE_ADD(NOW(),INTERVAL 6 DAY),DATE_ADD(DATE_ADD(NOW(),INTERVAL 6 DAY),INTERVAL 3 HOUR),DATE_ADD(NOW(),INTERVAL 5 DAY),'北京','崇文门电影交流空间',116.424100,39.899800,3,8,'FIXED',68,'票价以报名确认信息为准','/showcase/covers/movie-night.svg','一起观看剧情片，结束后留出四十分钟做轻松的映后交流。','[CITY_PARTY_SHOWCASE] movie-night',1,'UPCOMING',5,5,DATE_SUB(NOW(),INTERVAL 40 MINUTE),NOW(),0),
(@showcase_host_a,'公园轻松夜跑','运动','夜跑,公园,轻量运动',DATE_ADD(NOW(),INTERVAL 1 DAY),DATE_ADD(DATE_ADD(NOW(),INTERVAL 1 DAY),INTERVAL 90 MINUTE),DATE_ADD(NOW(),INTERVAL 12 HOUR),'北京','龙潭中湖公园北门',116.443100,39.884500,3,12,'FREE',0,NULL,'/showcase/covers/night-run.svg','五公里轻松配速，跑前热身、跑后拉伸，适合恢复跑和入门跑者。','[CITY_PARTY_SHOWCASE] night-run',0,'SIGNING',3,4,DATE_SUB(NOW(),INTERVAL 50 MINUTE),NOW(),0),
(@showcase_host_b,'咖啡馆编程学习局','学习','编程,自习,结伴学习',DATE_ADD(NOW(),INTERVAL 5 DAY),DATE_ADD(DATE_ADD(NOW(),INTERVAL 5 DAY),INTERVAL 4 HOUR),DATE_ADD(NOW(),INTERVAL 4 DAY),'北京','朝阳门社区咖啡空间',116.435500,39.922600,2,10,'ESTIMATE',38,'饮品各自结算','/showcase/covers/coding-study.svg','专注学习三轮，每轮五十分钟，中间安排短暂交流和问题分享。','[CITY_PARTY_SHOWCASE] coding-study',1,'SIGNING',3,5,DATE_SUB(NOW(),INTERVAL 60 MINUTE),NOW(),0);

SET @showcase_badminton=(SELECT id FROM activity WHERE BINARY title='周末羽毛球搭子' AND notes LIKE @showcase_marker);
SET @showcase_board_game=(SELECT id FROM activity WHERE BINARY title='东城桌游新手局' AND notes LIKE @showcase_marker);
SET @showcase_photowalk=(SELECT id FROM activity WHERE BINARY title='城市摄影漫步' AND notes LIKE @showcase_marker);
SET @showcase_movie=(SELECT id FROM activity WHERE BINARY title='周末电影观影交流' AND notes LIKE @showcase_marker);
SET @showcase_run=(SELECT id FROM activity WHERE BINARY title='公园轻松夜跑' AND notes LIKE @showcase_marker);
SET @showcase_coding=(SELECT id FROM activity WHERE BINARY title='咖啡馆编程学习局' AND notes LIKE @showcase_marker);

INSERT INTO activity_tag (activity_id,tag_name,created_at) VALUES
(@showcase_badminton,'羽毛球',NOW()),(@showcase_badminton,'新手友好',NOW()),
(@showcase_board_game,'桌游',NOW()),(@showcase_board_game,'轻社交',NOW()),
(@showcase_photowalk,'摄影',NOW()),(@showcase_photowalk,'城市漫步',NOW()),
(@showcase_movie,'电影',NOW()),(@showcase_movie,'映后交流',NOW()),
(@showcase_run,'夜跑',NOW()),(@showcase_run,'轻量运动',NOW()),
(@showcase_coding,'编程',NOW()),(@showcase_coding,'结伴学习',NOW());

INSERT INTO activity_signup (activity_id,user_id,status,apply_message,reviewed_at,created_at,updated_at,deleted) VALUES
(@showcase_badminton,@showcase_member_a,'APPROVED','周末一起活动',DATE_SUB(NOW(),INTERVAL 4 HOUR),DATE_SUB(NOW(),INTERVAL 6 HOUR),NOW(),0),
(@showcase_badminton,@showcase_member_b,'APPROVED','希望参加双打',DATE_SUB(NOW(),INTERVAL 4 HOUR),DATE_SUB(NOW(),INTERVAL 6 HOUR),NOW(),0),
(@showcase_badminton,@showcase_member_c,'APPROVED','新手参加',DATE_SUB(NOW(),INTERVAL 3 HOUR),DATE_SUB(NOW(),INTERVAL 5 HOUR),NOW(),0),
(@showcase_badminton,@showcase_member_d,'APPROVED','会自带球拍',DATE_SUB(NOW(),INTERVAL 2 HOUR),DATE_SUB(NOW(),INTERVAL 4 HOUR),NOW(),0),
(@showcase_board_game,@showcase_member_a,'APPROVED','第一次参加桌游局',DATE_SUB(NOW(),INTERVAL 20 HOUR),DATE_SUB(NOW(),INTERVAL 22 HOUR),NOW(),0),
(@showcase_board_game,@showcase_member_b,'APPROVED','喜欢合作类桌游',DATE_SUB(NOW(),INTERVAL 19 HOUR),DATE_SUB(NOW(),INTERVAL 21 HOUR),NOW(),0),
(@showcase_board_game,@showcase_member_c,'APPROVED','可以提前到场',DATE_SUB(NOW(),INTERVAL 18 HOUR),DATE_SUB(NOW(),INTERVAL 20 HOUR),NOW(),0),
(@showcase_board_game,@showcase_member_d,'APPROVED','期待新手教学',DATE_SUB(NOW(),INTERVAL 17 HOUR),DATE_SUB(NOW(),INTERVAL 19 HOUR),NOW(),0),
(@showcase_board_game,@showcase_member_e,'APPROVED','周末有时间',DATE_SUB(NOW(),INTERVAL 16 HOUR),DATE_SUB(NOW(),INTERVAL 18 HOUR),NOW(),0),
(@showcase_board_game,@showcase_host_b,'WAITING','满员后加入候补',NULL,DATE_SUB(NOW(),INTERVAL 12 HOUR),NOW(),0),
(@showcase_photowalk,@showcase_host_a,'COMPLETED','一起记录城市',DATE_SUB(NOW(),INTERVAL 6 DAY),DATE_SUB(NOW(),INTERVAL 7 DAY),NOW(),0),
(@showcase_photowalk,@showcase_member_a,'COMPLETED','使用手机拍摄',DATE_SUB(NOW(),INTERVAL 6 DAY),DATE_SUB(NOW(),INTERVAL 7 DAY),NOW(),0),
(@showcase_photowalk,@showcase_member_b,'COMPLETED','喜欢街头摄影',DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),NOW(),0),
(@showcase_photowalk,@showcase_member_c,'COMPLETED','参加日落路线',DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),NOW(),0),
(@showcase_movie,@showcase_host_a,'APPROVED','参加映后交流',DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),NOW(),0),
(@showcase_movie,@showcase_member_a,'APPROVED','周末观影',DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),NOW(),0),
(@showcase_movie,@showcase_member_b,'APPROVED','对影片主题感兴趣',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),NOW(),0),
(@showcase_movie,@showcase_member_c,'APPROVED','会准时到场',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),NOW(),0),
(@showcase_movie,@showcase_member_d,'APPROVED','期待交流环节',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),NOW(),0),
(@showcase_run,@showcase_host_b,'APPROVED','轻松配速参加',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),NOW(),0),
(@showcase_run,@showcase_member_a,'APPROVED','五公里恢复跑',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),NOW(),0),
(@showcase_run,@showcase_member_b,'APPROVED','一起完成拉伸',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),NOW(),0),
(@showcase_run,@showcase_member_c,'PENDING','申请参加夜跑',NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),NOW(),0),
(@showcase_coding,@showcase_host_a,'APPROVED','准备学习前端工程化',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),NOW(),0),
(@showcase_coding,@showcase_member_a,'APPROVED','准备整理项目代码',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),NOW(),0),
(@showcase_coding,@showcase_member_d,'APPROVED','专注学习三轮',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),NOW(),0),
(@showcase_coding,@showcase_member_e,'PENDING','希望参加学习局',NULL,DATE_SUB(NOW(),INTERVAL 12 HOUR),NOW(),0),
(@showcase_coding,@showcase_member_b,'REJECTED','时间暂未确认',DATE_SUB(NOW(),INTERVAL 6 HOUR),DATE_SUB(NOW(),INTERVAL 18 HOUR),NOW(),0);

INSERT INTO activity_waitlist (activity_id,user_id,status,queue_no,created_at,updated_at,deleted) VALUES
(@showcase_board_game,@showcase_host_b,'WAITING',1,DATE_SUB(NOW(),INTERVAL 12 HOUR),NOW(),0);

INSERT INTO activity_favorite (user_id,activity_id,created_at,deleted) VALUES
(@showcase_member_a,@showcase_board_game,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(@showcase_member_a,@showcase_movie,DATE_SUB(NOW(),INTERVAL 2 DAY),0),
(@showcase_member_b,@showcase_photowalk,DATE_SUB(NOW(),INTERVAL 5 DAY),0),
(@showcase_member_d,@showcase_coding,DATE_SUB(NOW(),INTERVAL 1 DAY),0);

INSERT INTO activity_review (activity_id,reviewer_id,target_user_id,rating,content,tags,credit_delta,created_at,deleted) VALUES
(@showcase_photowalk,@showcase_member_a,@showcase_host_b,5,'路线安排清晰，交流节奏很舒服。','组织清晰,友好',2,DATE_SUB(NOW(),INTERVAL 2 DAY),0),
(@showcase_photowalk,@showcase_member_b,@showcase_host_b,5,'日落拍摄点选择很好，收获很多。','摄影,守时',2,DATE_SUB(NOW(),INTERVAL 2 DAY),0),
(@showcase_photowalk,@showcase_host_b,@showcase_member_a,5,'按时到场，也主动帮助其他成员。','守时,互助',2,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(@showcase_photowalk,@showcase_host_b,@showcase_member_b,4,'沟通顺畅，活动过程中很投入。','友好,认真',1,DATE_SUB(NOW(),INTERVAL 1 DAY),0);

INSERT INTO credit_record (user_id,change_score,before_score,after_score,reason,source_type,source_id,created_at,deleted) VALUES
(@showcase_member_a,2,98,100,'按时完成桌游报名确认','ACTIVITY',@showcase_board_game,DATE_SUB(NOW(),INTERVAL 5 DAY),0),
(@showcase_member_a,1,100,101,'羽毛球活动准时签到','ACTIVITY',@showcase_badminton,DATE_SUB(NOW(),INTERVAL 3 DAY),0),
(@showcase_member_a,2,101,103,'摄影活动互评加分','ACTIVITY',@showcase_photowalk,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(@showcase_host_b,4,102,106,'连续获得优质组织评价','ACTIVITY',@showcase_photowalk,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(@showcase_host_a,5,107,112,'活动组织反馈良好','ACTIVITY',@showcase_board_game,DATE_SUB(NOW(),INTERVAL 2 DAY),0),
(@showcase_member_c,-9,100,91,'报名确认超时','ACTIVITY',@showcase_run,DATE_SUB(NOW(),INTERVAL 1 DAY),0);

INSERT INTO system_notice (user_id,type,title,content,related_id,read_flag,created_at,deleted) VALUES
(@showcase_member_a,'SIGNUP_APPROVED','桌游报名已通过','东城桌游新手局报名已确认，请留意活动提醒。',@showcase_board_game,0,DATE_SUB(NOW(),INTERVAL 8 HOUR),0),
(@showcase_member_a,'CHAT_MESSAGE','活动群聊有新消息','发起人更新了桌游活动的集合说明。',@showcase_board_game,0,DATE_SUB(NOW(),INTERVAL 5 HOUR),0),
(@showcase_member_a,'REVIEW_RECEIVED','摄影活动收到评价','你在城市摄影漫步中收到一条新的成员评价。',@showcase_photowalk,1,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(@showcase_host_b,'WAITLIST_JOINED','已加入桌游候补','当前候补顺位为第 1 位，名额释放后会自动提醒。',@showcase_board_game,0,DATE_SUB(NOW(),INTERVAL 12 HOUR),0);

INSERT INTO chat_message (activity_id,sender_id,sender_nickname,sender_avatar,content,message_type,created_at,deleted) VALUES
(@showcase_board_game,@showcase_host_a,'林澈','/showcase/avatars/host-lin.svg','大家好，活动当天提前十分钟到场就可以。','TEXT',DATE_SUB(NOW(),INTERVAL 9 HOUR),0),
(@showcase_board_game,@showcase_member_a,'陈安','/showcase/avatars/member-chen.svg','收到，我第一次参加桌游局，期待新手教学。','TEXT',DATE_SUB(NOW(),INTERVAL 8 HOUR),0),
(@showcase_board_game,@showcase_member_b,'苏宁','/showcase/avatars/member-su.svg','我可以早点到，帮忙整理桌面和卡牌。','TEXT',DATE_SUB(NOW(),INTERVAL 7 HOUR),0),
(@showcase_board_game,@showcase_host_a,'林澈','/showcase/avatars/host-lin.svg','谢谢，第一轮会从规则最简单的合作游戏开始。','TEXT',DATE_SUB(NOW(),INTERVAL 6 HOUR),0),
(@showcase_board_game,@showcase_member_a,'陈安','/showcase/avatars/member-chen.svg','好的，集合地点和交通方式都确认了。','TEXT',DATE_SUB(NOW(),INTERVAL 5 HOUR),0);

INSERT INTO report (reporter_id,target_type,target_id,reason,content,status,created_at,updated_at,deleted) VALUES
(@showcase_member_c,'ACTIVITY',@showcase_movie,'信息确认','[CITY_PARTY_SHOWCASE] 本地展示用已处理举报记录','RESOLVED',DATE_SUB(NOW(),INTERVAL 2 DAY),NOW(),0);

COMMIT;
