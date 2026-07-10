# API Overview

所有业务接口返回统一结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

除 `/api/auth/**` 外，接口默认需要请求头：

```text
Authorization: Bearer <token>
```

## Auth

- `GET /api/auth/captcha`
- `POST /api/auth/register`
- `POST /api/auth/login`

## User

- `GET /api/user/me`
- `GET /api/user/profile-overview`
- `PUT /api/user/profile`
- `GET /api/users/{id}/public-profile`

### Profile Overview

`GET /api/user/profile-overview` 聚合当前登录用户的个人中心数据：

- 基础资料、兴趣标签、当前信用分和信用等级。
- 发布活动数、已通过报名参与活动数、候补中活动数。
- 收到评价数、平均评分和未读通知数。

无评价时 `averageRating` 返回 `null`。信用等级为：110–120 优秀、100–109 良好、80–99 正常、60–79 待提升。

## File

- `POST /api/file/upload/avatar`
- `POST /api/file/upload/activity-cover`

## Activity

- `POST /api/activities`
- `PUT /api/activities/{id}`
- `PATCH /api/activities/{id}/cancel`
- `PATCH /api/activities/{id}/finish`
- `GET /api/activities`
- `GET /api/activities/nearby`
- `GET /api/activities/{id}`
- `GET /api/activities/my?type=published`
- `GET /api/activities/{id}/signups`
- `POST /api/activities/{id}/waitlist`
- `POST /api/activities/{id}/waitlist/cancel`
- `GET /api/activities/{id}/waitlist`
- `GET /api/activities/{activityId}/chat/access`
- `GET /api/activities/{activityId}/chat/messages`
- `POST /api/activities/{activityId}/chat/messages`

### Nearby Activity

`GET /api/activities/nearby`

常用查询参数：

- `longitude`、`latitude`：当前位置经纬度，可为空。
- `distanceKm`：距离范围，支持 1、3、5、10 等公里数，默认 5。
- `category`、`tag`、`city`：可选筛选条件。
- `current`、`size`：分页参数。

当经纬度存在时，后端先按经纬度边界框做数据库预过滤，再使用 Haversine 公式计算 `distanceKm` 并按距离升序返回；经纬度为空时，按 `city` 降级查询。`distanceKm` 最大按 100 公里处理。

### Activity Lifecycle

- `PUT /api/activities/{id}`：活动发起人或管理员编辑活动，使用与发布活动一致的请求体。
- `PATCH /api/activities/{id}/cancel`：活动发起人或管理员取消活动，取消后不能继续报名。
- `PATCH /api/activities/{id}/finish`：活动发起人或管理员结束活动，若结束时间晚于当前时间，会更新为当前时间。

编辑活动时，`maxParticipants` 不能小于当前已通过报名人数。已取消或已结束活动不能继续编辑。

### My Activities

`GET /api/activities/my` 支持 `current`、`size` 和以下 `type`：

- `published`：我发布的活动，也是缺省值，兼容 Stage 2.3 以前的调用。
- `joined`：报名状态为 `APPROVED`、`PROMOTED` 或 `COMPLETED` 的活动。
- `waiting`：`activity_waitlist` 中当前状态为 `WAITING` 的活动。
- `finished`：当前用户发布或已通过报名参与，并且 `endTime` 已到的活动。

### Waitlist

- `POST /api/activities/{id}/waitlist`：活动满员时加入候补队列。
- `POST /api/activities/{id}/waitlist/cancel`：取消候补。
- `GET /api/activities/{id}/waitlist`：发起人或管理员查看候补列表。

候补状态：

- `WAITING`：候补中。
- `PROMOTED`：已自动转正。
- `CANCELLED`：已取消。

Redis Key：`activity:waitlist:{activityId}`。Redis List 维护顺序，MySQL `activity_waitlist` 保存持久记录。

### Activity Chat

- `GET /api/activities/{activityId}/chat/access`：检查当前用户是否可以进入活动群聊。
- `GET /api/activities/{activityId}/chat/messages?current=1&size=50`：分页获取活动群聊历史消息。
- `POST /api/activities/{activityId}/chat/messages`：发送群聊消息，兼容 HTTP 测试。

群聊权限：

- 活动发起人可以进入。
- `APPROVED` 报名用户可以进入。
- 未登录、未报名、`PENDING`、`WAITING`、`REJECTED`、`CANCELLED` 用户不能进入。

HTTP 发送请求体：

```json
{
  "content": "大家几点集合？"
}
```

## Signup

- `POST /api/activities/{activityId}/signup`
- `POST /api/activities/{activityId}/signup/cancel`
- `POST /api/signups/{signupId}/review`
- `POST /api/signups`（兼容接口，请求体传 `activityId`，等价于报名活动）
- `POST /api/signups/{signupId}/approve`（兼容接口，等价于审核通过）
- `POST /api/signups/{signupId}/reject`（兼容接口，等价于审核拒绝）
- `GET /api/signups/my`

报名状态补充：

- `WAITING`：候补中。
- `PROMOTED` 或 `APPROVED`：报名成功。
- 用户退出 `APPROVED` 报名后，系统会自动尝试转正第一位候补用户。
- Stage 2.6 起，活动人数通过数据库条件更新占位，并建议执行 `database/stage2.6-migration.sql` 增加同一用户同一活动的唯一约束。

## Notice

- `GET /api/notices/my`
- `PUT /api/notices/{id}/read`
- `PUT /api/notices/read-all`
- `GET /api/notices/unread-count`

候补转正后会生成系统通知：

```text
type = WAITLIST_PROMOTED
title = 候补转正通知
```

Stage 2.2 会在通知写入 MySQL 后通过 WebSocket 向在线用户实时推送。

评价成功后会生成评价通知：

```text
type = ACTIVITY_REVIEW
title = 你收到了一条活动评价
```

## Activity Review

- `GET /api/activities/{activityId}/reviews/targets`：获取当前用户可评价的活动成员。
- `POST /api/activities/{activityId}/reviews`：提交评价。
- `GET /api/activities/{activityId}/reviews?current=1&size=20`：分页获取活动评价。
- `GET /api/reviews/my?type=sent&current=1&size=20`：获取我发出的评价。
- `GET /api/reviews/my?type=received&current=1&size=20`：获取我收到的评价。

查看和提交评价的用户必须是活动发起人或 `APPROVED` 报名成员，并且活动的 `endTime` 已到。被评价人也必须是同一活动成员，不能评价自己或重复评价同一成员。

提交请求：

```json
{
  "targetUserId": 3,
  "rating": 5,
  "content": "准时到场，沟通顺畅",
  "tags": ["准时", "友好", "好沟通"]
}
```

评分对应的信用分变化：

| 评分 | 原始变化 |
|---|---:|
| 5 | +2 |
| 4 | +1 |
| 3 | 0 |
| 2 | -2 |
| 1 | -4 |

信用分最终限制在 60–120，评价和信用日志记录的是边界处理后的实际变化值。

## Credit

- `GET /api/credit/logs?current=1&size=20`：分页获取当前用户信用分变更记录。
- `GET /api/credit/overview?current=1&size=20`：获取当前信用分、信用等级和分页变更记录。

信用日志复用既有 `credit_record` 表。评价产生的记录使用：

```text
source_type = ACTIVITY_REVIEW
source_id = activity_review.id
```

对于 `ACTIVITY_REVIEW` 及兼容的旧 `REVIEW` 来源，响应会尽量补充 `relatedActivityId` 和 `relatedActivityTitle`。无法解析关联活动时，这两个字段为 `null`。

## WebSocket

开发环境连接地址：

```text
ws://127.0.0.1:5173/ws?token=<JWT>
```

后端实际端点为 `/ws`，前端 Vite dev server 会把 `/ws` 代理到 `ws://127.0.0.1:8080/ws`。

聊天发送：

```json
{
  "type": "CHAT",
  "activityId": 1,
  "content": "大家几点集合？"
}
```

聊天广播：

```json
{
  "type": "CHAT",
  "activityId": 1,
  "messageId": 100,
  "senderId": 2,
  "senderNickname": "user02",
  "senderAvatar": "/uploads/avatar/user02.png",
  "content": "大家几点集合？",
  "createdAt": "2026-07-08 19:00:00"
}
```

通知推送：

```json
{
  "type": "NOTICE",
  "noticeId": 1,
  "noticeType": "WAITLIST_PROMOTED",
  "title": "候补转正通知",
  "content": "你候补的活动已转为报名成功",
  "relatedId": 20,
  "createdAt": "2026-07-08 19:00:00"
}
```

错误消息：

```json
{
  "type": "ERROR",
  "message": "报名成功后才能进入活动群聊"
}
```

## Favorite

- `POST /api/activities/{activityId}/favorite`
- `DELETE /api/activities/{activityId}/favorite`
- `POST /api/favorites/{activityId}`（兼容接口，等价于收藏活动）
- `GET /api/favorites/my`

## API Not Found

未匹配的 `/api/**` 请求返回统一结构，不再进入服务器内部错误分支：

```json
{
  "code": 404,
  "message": "接口不存在",
  "data": null
}
```

## Admin

管理员接口要求登录用户角色为 `ADMIN`。JWT 解析后的角色由后端拦截器校验，普通用户即使直接请求 `/api/admin/**` 也会收到：

```json
{
  "code": 403,
  "message": "无管理员权限",
  "data": null
}
```

Stage 2.5 管理接口：

- `GET /api/admin/dashboard`：用户、活动、报名、评价、通知总数。
- `GET /api/admin/users?keyword=&current=1&size=10`：用户列表，关键词匹配账号、手机号或昵称。
- `GET /api/admin/users/{id}`：用户基础资料与信用分详情。
- `GET /api/admin/activities?keyword=&category=&status=&current=1&size=10`：活动列表和状态筛选。
- `GET /api/admin/activities/{id}`：活动详情。
- `GET /api/admin/activities/{id}/signups?current=1&size=20`：指定活动的报名用户。
- `GET /api/admin/activities/{id}/waitlist?current=1&size=20`：指定活动的候补用户。
- `GET /api/admin/signups?activityId=&status=&current=1&size=10`：全局报名记录，保留已有兼容接口。
- `GET /api/admin/reviews?activityId=&userId=&current=1&size=10`：评价记录；`userId` 同时匹配评价人和被评价人。
- `GET /api/admin/credits?userId=&current=1&size=10`：信用变化明细及可解析的关联活动。
- `GET /api/admin/notices?userId=&current=1&size=10`：系统通知与接收用户。
- `GET /api/admin/reports?current=1&size=10`：保留第一阶段兼容接口，本阶段不扩展处理流程。

所有分页接口都会将 `current` 的最小值限制为 `1`，将 `size` 限制在 `1` 到 `100`。Stage 2.5 仅提供查询能力，不提供删除、禁用、下架、队列调整、信用分修改或通知群发。

数据库继续使用已有 `user.role` 和业务表，不需要执行 `database/stage2.5-migration.sql`。

## Stage 2.6 Hardening

- `database/schema.sql` 和 `backend/src/main/resources/schema.sql` 已同步 Stage 2.2 聊天字段和索引。
- `database/stage2.6-migration.sql` 新增 `activity_signup(activity_id, user_id)` 唯一约束。
- 所有主要分页入口将 `current` 限制为至少 `1`，`size` 限制为最多 `100`。
- JWT HTTP 拦截器和 WebSocket 握手会复核数据库用户状态和角色。
- 新注册密码使用 PBKDF2，旧 SHA-256 哈希仍可登录；上传图片会校验文件头。
- CORS 和 WebSocket Origin 可通过 `CORS_ALLOWED_ORIGIN_PATTERNS` 配置。

## AMap Config

前端读取以下 Vite 环境变量：

```text
VITE_AMAP_KEY=你的高德地图 JS API Key
VITE_AMAP_SECURITY_CODE=你的高德 JS API 安全密钥
```

真实 Key 只应写入 `frontend/.env.development` 或本地私有环境文件，不要提交到 Git。
