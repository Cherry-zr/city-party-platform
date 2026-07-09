CREATE DATABASE IF NOT EXISTS city_party_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE city_party_platform;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS system_notice;
DROP TABLE IF EXISTS report;
DROP TABLE IF EXISTS activity_review;
DROP TABLE IF EXISTS aa_bill_item;
DROP TABLE IF EXISTS aa_bill;
DROP TABLE IF EXISTS partner_relation;
DROP TABLE IF EXISTS partner_request;
DROP TABLE IF EXISTS credit_record;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS activity_favorite;
DROP TABLE IF EXISTS activity_waitlist;
DROP TABLE IF EXISTS activity_signup;
DROP TABLE IF EXISTS activity_tag;
DROP TABLE IF EXISTS activity;
DROP TABLE IF EXISTS user_interest;
DROP TABLE IF EXISTS interest_tag;
DROP TABLE IF EXISTS user_profile;
DROP TABLE IF EXISTS user;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE COMMENT 'login username',
  phone VARCHAR(30) NULL COMMENT 'phone number',
  password_hash VARCHAR(128) NOT NULL COMMENT 'password hash',
  role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN',
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/DISABLED',
  credit_score INT NOT NULL DEFAULT 100 COMMENT 'credit score',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_user_role (role),
  INDEX idx_user_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user';

CREATE TABLE user_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  avatar_url VARCHAR(255) NULL,
  city VARCHAR(50) NULL,
  bio VARCHAR(500) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_profile_user (user_id),
  INDEX idx_profile_city (city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user profile';

CREATE TABLE interest_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  sort_order INT NOT NULL DEFAULT 100,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='interest tag';

CREATE TABLE user_interest (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_user_tag (user_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user interest';

CREATE TABLE activity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  creator_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  category VARCHAR(30) NOT NULL COMMENT 'activity category',
  tags VARCHAR(255) NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  signup_deadline DATETIME NOT NULL,
  city VARCHAR(50) NOT NULL,
  address VARCHAR(255) NOT NULL,
  longitude DECIMAL(10,6) NULL,
  latitude DECIMAL(10,6) NULL,
  min_participants INT NOT NULL,
  max_participants INT NOT NULL,
  cost_type VARCHAR(20) NOT NULL COMMENT 'FREE/AA/FIXED/ESTIMATE',
  cost_amount DECIMAL(10,2) NULL,
  aa_rule VARCHAR(500) NULL,
  cover_url VARCHAR(255) NULL,
  description TEXT NOT NULL,
  notes VARCHAR(1000) NULL,
  need_approval TINYINT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'SIGNING' COMMENT 'SIGNING/FULL/UPCOMING/ONGOING/FINISHED/CANCELLED',
  approved_count INT NOT NULL DEFAULT 0,
  favorite_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_activity_creator (creator_id),
  INDEX idx_activity_category (category),
  INDEX idx_activity_city (city),
  INDEX idx_activity_status (status),
  INDEX idx_activity_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='activity';

CREATE TABLE activity_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  tag_name VARCHAR(50) NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_activity_tag_activity (activity_id),
  INDEX idx_activity_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='activity tag';

CREATE TABLE activity_signup (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL COMMENT 'PENDING/APPROVED/REJECTED/WAITING/CANCELLED/COMPLETED/ABSENT/PROMOTED',
  apply_message VARCHAR(300) NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_signup_activity (activity_id),
  INDEX idx_signup_user (user_id),
  INDEX idx_signup_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='activity signup';

CREATE TABLE activity_waitlist (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING/PROMOTED/CANCELLED',
  queue_no BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_waitlist_activity_user (activity_id, user_id),
  INDEX idx_waitlist_activity_status (activity_id, status),
  INDEX idx_waitlist_user (user_id),
  INDEX idx_waitlist_queue (activity_id, queue_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='activity waitlist';

CREATE TABLE activity_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_favorite_user_activity (user_id, activity_id),
  INDEX idx_favorite_activity (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='activity favorite';

CREATE TABLE chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  created_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='chat message';

CREATE TABLE credit_record (
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

CREATE TABLE partner_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  from_user_id BIGINT NOT NULL,
  to_user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  message VARCHAR(300) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='partner request';

CREATE TABLE partner_relation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  partner_user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='partner relation';

CREATE TABLE aa_bill (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  creator_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
  description VARCHAR(500) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='aa bill';

CREATE TABLE aa_bill_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  bill_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  pay_status VARCHAR(20) NOT NULL DEFAULT 'UNCONFIRMED',
  confirmed_at DATETIME NULL,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='aa bill item';

CREATE TABLE activity_review (
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

CREATE TABLE report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL COMMENT 'USER/ACTIVITY',
  target_id BIGINT NOT NULL,
  reason VARCHAR(100) NOT NULL,
  content VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='report';

CREATE TABLE system_notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(50) NOT NULL,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(500) NOT NULL,
  related_id BIGINT NULL,
  read_flag TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='system notice';
