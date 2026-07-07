USE city_party_platform;

INSERT INTO user (id, username, phone, password_hash, role, status, credit_score, created_at, updated_at, deleted) VALUES
(1, 'admin', '13800000000', '9128b35904e4257a6dfa066fa69513d091a1c9e2ca1e099429736c840f0d4ae0', 'ADMIN', 'NORMAL', 100, NOW(), NOW(), 0),
(2, 'user01', '13800000001', '9128b35904e4257a6dfa066fa69513d091a1c9e2ca1e099429736c840f0d4ae0', 'USER', 'NORMAL', 100, NOW(), NOW(), 0),
(3, 'user02', '13800000002', '9128b35904e4257a6dfa066fa69513d091a1c9e2ca1e099429736c840f0d4ae0', 'USER', 'NORMAL', 96, NOW(), NOW(), 0),
(4, 'user03', '13800000003', '9128b35904e4257a6dfa066fa69513d091a1c9e2ca1e099429736c840f0d4ae0', 'USER', 'NORMAL', 102, NOW(), NOW(), 0),
(5, 'user04', '13800000004', '9128b35904e4257a6dfa066fa69513d091a1c9e2ca1e099429736c840f0d4ae0', 'USER', 'NORMAL', 99, NOW(), NOW(), 0);

INSERT INTO user_profile (id, user_id, nickname, avatar_url, city, bio, created_at, updated_at, deleted) VALUES
(1, 1, '平台管理员', NULL, '北京', '负责平台内容审核与运营管理。', NOW(), NOW(), 0),
(2, 2, '周末电影搭子', NULL, '北京', '喜欢观影、桌游和轻社交。', NOW(), NOW(), 0),
(3, 3, '城市探店人', NULL, '上海', '咖啡、展览、徒步都可以。', NOW(), NOW(), 0),
(4, 4, '运动新手', NULL, '广州', '想找羽毛球和夜跑伙伴。', NOW(), NOW(), 0),
(5, 5, '自习搭子', NULL, '杭州', '周末图书馆学习，互相监督。', NOW(), NOW(), 0);

INSERT INTO interest_tag (id, name, sort_order, created_at) VALUES
(1, 'AA制', 1, NOW()),
(2, '新手友好', 2, NOW()),
(3, '女生优先', 3, NOW()),
(4, '地铁附近', 4, NOW()),
(5, '周末', 5, NOW()),
(6, '轻社交', 6, NOW()),
(7, '免费', 7, NOW()),
(8, '低预算', 8, NOW()),
(9, '长期搭子', 9, NOW()),
(10, '同校优先', 10, NOW());

INSERT INTO user_interest (user_id, tag_id, created_at) VALUES
(2, 1, NOW()), (2, 2, NOW()), (2, 5, NOW()),
(3, 4, NOW()), (3, 6, NOW()), (3, 8, NOW()),
(4, 2, NOW()), (4, 5, NOW()), (4, 7, NOW()),
(5, 9, NOW()), (5, 10, NOW()), (5, 6, NOW());

INSERT INTO activity (id, creator_id, title, category, tags, start_time, end_time, signup_deadline, city, address, longitude, latitude, min_participants, max_participants, cost_type, cost_amount, aa_rule, cover_url, description, notes, need_approval, status, approved_count, favorite_count, created_at, updated_at, deleted) VALUES
(1, 2, '周末影院看新片', '观影', 'AA制,周末,轻社交', '2026-08-01 19:00:00', '2026-08-01 22:00:00', '2026-08-01 12:00:00', '北京', '朝阳区合生汇电影院', 116.480000, 39.920000, 2, 6, 'AA', 80.00, '电影票和饮品现场 AA。', NULL, '一起看电影，结束后可以简单聊聊剧情。', '请准时到场，迟到提前说明。', 0, 'SIGNING', 2, 2, NOW(), NOW(), 0),
(2, 3, '上海咖啡探店小队', '探店', '新手友好,地铁附近,低预算', '2026-08-03 14:00:00', '2026-08-03 17:00:00', '2026-08-02 22:00:00', '上海', '静安区南京西路咖啡店', 121.459000, 31.229000, 2, 4, 'ESTIMATE', 60.00, '各自点单，费用自理。', NULL, '找一家安静咖啡店探店拍照，适合轻松聊天。', '不强制拍照，尊重个人隐私。', 1, 'SIGNING', 1, 1, NOW(), NOW(), 0),
(3, 4, '广州羽毛球新手局', '运动', '新手友好,周末,免费', '2026-08-08 09:30:00', '2026-08-08 11:30:00', '2026-08-07 20:00:00', '广州', '天河体育中心羽毛球馆', 113.320000, 23.135000, 4, 8, 'FIXED', 35.00, '场地费平摊，每人约 35 元。', NULL, '新手友好，不卷水平，主要出汗和认识朋友。', '自带球拍，穿运动鞋。', 0, 'FULL', 8, 0, NOW(), NOW(), 0),
(4, 5, '杭州图书馆自习搭子', '学习', '长期搭子,同校优先,轻社交', '2026-08-10 10:00:00', '2026-08-10 17:00:00', '2026-08-09 21:00:00', '杭州', '浙江图书馆自习区', 120.150000, 30.280000, 2, 5, 'FREE', 0.00, '无费用。', NULL, '一起安静自习，中午可一起吃饭。', '请保持安静，勿频繁闲聊。', 1, 'SIGNING', 0, 1, NOW(), NOW(), 0),
(5, 2, '桌游轻社交体验局', '桌游', 'AA制,新手友好,轻社交', '2026-07-20 15:00:00', '2026-07-20 18:00:00', '2026-07-19 21:00:00', '北京', '海淀区桌游店', 116.310000, 39.980000, 3, 6, 'AA', 50.00, '桌游店包间费按人数平摊。', NULL, '玩轻策略桌游，新人会讲规则。', '避免放鸽子，临时有事请提前退出。', 0, 'UPCOMING', 3, 0, NOW(), NOW(), 0);

INSERT INTO activity_tag (activity_id, tag_name, created_at) VALUES
(1, 'AA制', NOW()), (1, '周末', NOW()), (1, '轻社交', NOW()),
(2, '新手友好', NOW()), (2, '地铁附近', NOW()), (2, '低预算', NOW()),
(3, '新手友好', NOW()), (3, '周末', NOW()), (3, '免费', NOW()),
(4, '长期搭子', NOW()), (4, '同校优先', NOW()), (4, '轻社交', NOW()),
(5, 'AA制', NOW()), (5, '新手友好', NOW()), (5, '轻社交', NOW());

INSERT INTO activity_signup (id, activity_id, user_id, status, apply_message, reviewed_at, created_at, updated_at, deleted) VALUES
(1, 1, 3, 'APPROVED', '我也想看这部电影。', NOW(), NOW(), NOW(), 0),
(2, 1, 4, 'APPROVED', '周末有空参加。', NOW(), NOW(), NOW(), 0),
(3, 2, 2, 'APPROVED', '想一起探店。', NOW(), NOW(), NOW(), 0),
(4, 2, 4, 'PENDING', '我在附近，想参加。', NULL, NOW(), NOW(), 0),
(5, 5, 3, 'APPROVED', '喜欢桌游。', NOW(), NOW(), NOW(), 0),
(6, 5, 4, 'APPROVED', '新人可以吗？', NOW(), NOW(), NOW(), 0),
(7, 5, 5, 'APPROVED', '想体验轻社交。', NOW(), NOW(), NOW(), 0);

INSERT INTO activity_favorite (id, user_id, activity_id, created_at, deleted) VALUES
(1, 3, 1, NOW(), 0),
(2, 4, 1, NOW(), 0),
(3, 2, 2, NOW(), 0),
(4, 5, 4, NOW(), 0);

INSERT INTO credit_record (user_id, change_score, before_score, after_score, reason, source_type, source_id, created_at) VALUES
(3, -4, 100, 96, '测试数据：历史退出活动', 'SIGNUP', 1, NOW()),
(4, 2, 100, 102, '测试数据：活动评价良好', 'REVIEW', 1, NOW());

INSERT INTO report (reporter_id, target_type, target_id, reason, content, status, created_at, updated_at, deleted) VALUES
(2, 'ACTIVITY', 3, '费用说明不清晰', '这是第一阶段举报入口测试数据。', 'PENDING', NOW(), NOW(), 0);
