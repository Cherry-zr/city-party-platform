# 核心业务流程

## 注册与登录

1. 前端请求 `GET /api/auth/captcha` 获取验证码 key 和验证码文本。
2. 用户提交 `POST /api/auth/register` 或 `POST /api/auth/login`。
3. 后端从 Redis 读取 `captcha:{key}` 并校验，校验成功后删除验证码。
4. 注册时写入 `user` 和 `user_profile`。
5. 登录时校验密码，若旧 SHA-256 哈希匹配成功，则升级为 PBKDF2。
6. 登录成功返回 JWT 和当前用户信息。

## JWT 鉴权

1. 前端在 `Authorization: Bearer <token>` 中携带 JWT。
2. `JwtInterceptor` 校验签名和过期时间。
3. 拦截器重新查询 `user` 表，确认用户存在、未删除、状态为 `NORMAL`。
4. 访问 `/api/admin/**` 时，拦截器要求数据库中的当前角色为 `ADMIN`。
5. 校验通过后写入 `UserContext`，请求结束后清理。

## 活动发布

1. 登录用户调用 `POST /api/activities`。
2. 后端校验开始时间、结束时间、报名截止时间。
3. 写入 `activity`，默认状态为 `SIGNING`。
4. 活动标签写入 `activity_tag`。

编辑、取消、结束：

- `PUT /api/activities/{id}`：发起人或管理员可编辑。
- `PATCH /api/activities/{id}/cancel`：发起人或管理员可取消。
- `PATCH /api/activities/{id}/finish`：发起人或管理员可结束。
- 已取消或已结束活动不能继续编辑。
- 编辑时 `maxParticipants` 不能小于当前 `approved_count`。

## 报名流程

1. 用户调用 `POST /api/activities/{activityId}/signup`。
2. 发起人不能报名自己发布的活动。
3. 只有 `SIGNING` 或 `FULL` 状态活动进入报名逻辑。
4. 后端检查同一活动同一用户已有报名记录。
5. 活动需要审核时，状态为 `PENDING`；不需要审核时，状态为 `APPROVED`。
6. 不需要审核且报名成功时，调用数据库条件更新增加 `approved_count`。
7. 如果活动已满，返回业务错误，用户可加入候补队列。

数据库约束：

- `activity_signup` 上存在 `uk_signup_activity_user(activity_id,user_id)`，用于防止并发重复报名。

## 发起人审核

1. 发起人调用 `POST /api/signups/{signupId}/review`。
2. 只允许审核 `PENDING` 报名。
3. 审核通过前检查活动是否已满。
4. 审核通过时条件增加 `activity.approved_count`。
5. 审核拒绝时状态变为 `REJECTED`。

## 取消报名和人数回退

1. 用户调用 `POST /api/activities/{activityId}/signup/cancel`。
2. 若原状态为 `APPROVED`，报名状态改为 `CANCELLED`。
3. 后端减少 `activity.approved_count`。
4. 减少后尝试调用候补转正。
5. 若原状态为 `WAITING`，转为取消候补。

## 候补队列

加入候补：

1. 用户调用 `POST /api/activities/{id}/waitlist`。
2. 活动必须为 `SIGNING` 或 `FULL`，且当前已满。
3. 已报名成功或已在候补中的用户不能重复加入。
4. 写入或复用 `activity_signup`，状态为 `WAITING`。
5. 写入或复用 `activity_waitlist`，状态为 `WAITING`。
6. 将 waitlist id 写入 Redis List：`activity:waitlist:{activityId}`。

候补转正：

1. 报名退出或其他人数回退后触发。
2. 优先从 Redis `LPOP` 获取下一个候补。
3. Redis 不可用或队列为空时，回退查询 MySQL 中最早的 `WAITING` 记录。
4. 通过数据库条件更新抢占名额。
5. 将候补记录改为 `PROMOTED`，报名记录改为 `APPROVED`。
6. 写入系统通知并通过 WebSocket 推送。

## 评价与信用

1. 活动结束后，发起人和已通过报名的成员可以互评。
2. 同一活动中，同一评价人对同一目标用户只能评价一次。
3. 评价写入 `activity_review`。
4. 根据信用规则更新目标用户信用分并写入 `credit_record`。
5. 信用分边界限制在 60 到 120。
6. 生成系统通知。

评分对应信用变化：

| 评分 | 原始变化 |
| --- | ---: |
| 5 | +2 |
| 4 | +1 |
| 3 | 0 |
| 2 | -2 |
| 1 | -4 |

## 群聊

1. 用户通过 `/ws?token=<JWT>` 建立 WebSocket。
2. 握手阶段校验 token 并复核数据库用户状态。
3. 发送 `CHAT` 消息时，后端检查用户是否是活动发起人或 `APPROVED` 报名成员。
4. 消息写入 `chat_message`。
5. 只广播给有权限进入该活动群聊的在线用户。

## 管理员数据看板

1. 管理员访问 `/admin/dashboard` 或 `/admin/analytics`。
2. 前端请求 `/api/admin/dashboard/*`。
3. 后端拦截器复核数据库角色。
4. 首页概览读取 Redis 缓存，失败时回退 MySQL。
5. 趋势、分布、质量指标、排行实时查询 MySQL。

