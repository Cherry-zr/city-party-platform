# 数据库设计

## 基线脚本

当前完整建库基线：

```text
database/schema.sql
backend/src/main/resources/schema.sql
```

Docker 空数据卷首次启动时只执行 `database/schema.sql`。旧库升级才按阶段执行 migration。

## 表数量

当前 schema 包含 18 张业务表：

1. `user`
2. `user_profile`
3. `interest_tag`
4. `user_interest`
5. `activity`
6. `activity_tag`
7. `activity_signup`
8. `activity_waitlist`
9. `activity_favorite`
10. `chat_message`
11. `credit_record`
12. `partner_request`
13. `partner_relation`
14. `aa_bill`
15. `aa_bill_item`
16. `activity_review`
17. `report`
18. `system_notice`

## 核心表

### user

保存登录账号、角色、状态和信用分。

关键字段：

- `username`：唯一登录账号。
- `password_hash`：PBKDF2 或旧 SHA-256 哈希。
- `role`：`USER` / `ADMIN`。
- `status`：`NORMAL` / `DISABLED`。
- `credit_score`：信用分。
- `deleted`：逻辑删除标记。

关键索引：

- `idx_user_role(role)`
- `idx_user_deleted(deleted)`

### activity

保存活动基础信息。

关键字段：

- `creator_id`
- `title`
- `category`
- `start_time`
- `end_time`
- `signup_deadline`
- `city`
- `longitude`
- `latitude`
- `min_participants`
- `max_participants`
- `need_approval`
- `status`
- `approved_count`
- `favorite_count`

状态：

- `SIGNING`
- `FULL`
- `UPCOMING`
- `ONGOING`
- `FINISHED`
- `CANCELLED`

关键索引：

- `idx_activity_creator(creator_id)`
- `idx_activity_category(category)`
- `idx_activity_city(city)`
- `idx_activity_status(status)`
- `idx_activity_deleted(deleted)`

### activity_signup

保存报名记录。

状态：

- `PENDING`
- `APPROVED`
- `REJECTED`
- `WAITING`
- `CANCELLED`
- `COMPLETED`
- `ABSENT`
- `PROMOTED`

关键约束：

- `uk_signup_activity_user(activity_id,user_id)`

该约束用于防止同一用户对同一活动产生并发重复报名。

关键索引：

- `idx_signup_activity(activity_id)`
- `idx_signup_user(user_id)`
- `idx_signup_status(status)`
- `idx_signup_user_status_activity(user_id,status,deleted,activity_id)`

### activity_waitlist

保存候补队列的持久记录。

状态：

- `WAITING`
- `PROMOTED`
- `CANCELLED`

关键约束：

- `uk_waitlist_activity_user(activity_id,user_id)`

关键索引：

- `idx_waitlist_activity_status(activity_id,status)`
- `idx_waitlist_user(user_id)`
- `idx_waitlist_queue(activity_id,queue_no)`
- `idx_waitlist_user_status_activity(user_id,status,deleted,activity_id)`

### activity_review

保存活动评价。

关键字段：

- `activity_id`
- `reviewer_id`
- `target_user_id`
- `rating`
- `content`
- `tags`
- `credit_delta`

关键约束：

- `uk_review_activity_reviewer_target(activity_id,reviewer_id,target_user_id)`

该约束防止同一活动中同一评价人重复评价同一目标用户。

### credit_record

保存信用分变化记录。

关键字段：

- `user_id`
- `change_score`
- `before_score`
- `after_score`
- `reason`
- `source_type`
- `source_id`

常见来源：

- `ACTIVITY_REVIEW`
- `ACTIVITY`
- 兼容旧数据中的 `REVIEW`

### system_notice

保存站内通知。

关键字段：

- `user_id`
- `type`
- `title`
- `content`
- `related_id`
- `read_flag`

关键索引：

- `idx_notice_user_read_time(user_id,read_flag,deleted,created_at)`

## 预留表

以下表当前主要用于结构预留或基础展示：

- `partner_request`
- `partner_relation`
- `aa_bill`
- `aa_bill_item`
- `report`

README 和简历材料中不把这些预留结构描述为完整生产功能。

## migration 策略

旧库升级脚本：

- `database/stage2.1-migration.sql`
- `database/stage2.2-migration.sql`
- `database/stage2.3-migration.sql`
- `database/stage2.4-migration.sql`
- `database/stage2.6-migration.sql`

规则：

- 空库首次启动使用完整 `schema.sql`。
- 旧库升级前必须先备份。
- 不要重复叠加完整 schema 和 migration。
- 不执行 `TRUNCATE` 正常业务表。
- 不提交数据库备份。

