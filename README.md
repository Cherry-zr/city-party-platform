# 同城活动发现与陌生人组局平台

这是一个毕业论文方向的全栈项目，目标是连接同城陌生用户，支持用户发起、发现、报名、收藏和管理线下活动。第一阶段实现核心业务最小闭环，地图、WebSocket 群聊、AA 账单、固定搭子、完整信用分规则等功能先预留结构，后续分阶段扩展。

## 技术栈

后端：

- Java 17
- Spring Boot 3.x
- Maven
- MySQL 8.x
- Redis
- MyBatis-Plus
- JWT 拦截器鉴权
- Knife4j 接口文档
- 本地文件上传

前端：

- Vue3
- Vite
- JavaScript
- Vue Router
- Pinia
- Axios
- Vant
- Element Plus

## 目录结构

```text
city-party-platform/
├─ backend/
├─ frontend/
├─ database/
├─ docs/
├─ screenshots/
└─ README.md
```

## 环境要求

- Java 17
- Maven 3.8+
- Node.js 18+
- npm 9+
- MySQL 8.x
- Redis 6+

当前开发机已检查到 Java 17.0.16、Maven 3.9.15、Node.js v24.11.0、npm 11.6.1、MySQL 8.0.33。验证码和候补队列依赖 Redis，请保证 Redis 6379 端口可连通。

## MySQL 初始化

先确认本机 MySQL 账号可以登录。推荐使用环境变量，不要把真实密码写进代码。

PowerShell 示例： 

```powershell
$env:MYSQL_HOST="127.0.0.1"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="city_party_platform"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="你的MySQL密码"

mysql -u$env:MYSQL_USERNAME -p$env:MYSQL_PASSWORD < database\schema.sql
mysql -u$env:MYSQL_USERNAME -p$env:MYSQL_PASSWORD < database\data.sql
```

如果已经有 Stage 1.1 数据库，不想重建全库，可以执行 Stage 2.1 增量脚本：

```powershell
mysql -u$env:MYSQL_USERNAME -p$env:MYSQL_PASSWORD < database\stage2.1-migration.sql
```

Stage 2.2 在不重建数据库的前提下新增活动群聊字段，请继续执行增量迁移：

```powershell
mysql -u$env:MYSQL_USERNAME -p$env:MYSQL_PASSWORD < database\stage2.2-migration.sql
```

如果不想使用 root，建议创建专用开发账号：

```sql
CREATE USER 'city_party'@'localhost' IDENTIFIED BY 'city_party_123456';
GRANT ALL PRIVILEGES ON city_party_platform.* TO 'city_party'@'localhost';
FLUSH PRIVILEGES;
```

## Redis 启动要求

后端验证码依赖 Redis。确认 Redis 已监听本地 6379：

```powershell
Test-NetConnection 127.0.0.1 -Port 6379
```

`TcpTestSucceeded` 为 `True` 才能正常使用验证码接口。

## 后端启动

```powershell
cd D:\last_one-form-group\city-party-platform\backend
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="你的MySQL密码"
mvn spring-boot:run
```

默认后端地址：

```text
http://127.0.0.1:8080
```

Knife4j 文档地址：

```text
http://127.0.0.1:8080/doc.html
```

## 前端启动

```powershell
cd D:\last_one-form-group\city-party-platform\frontend
npm install
npm run dev
```

默认前端地址：

```text
http://127.0.0.1:5173
```

Vite 已配置代理：

- `/api` -> `http://127.0.0.1:8080`
- `/uploads` -> `http://127.0.0.1:8080`

## 高德地图配置

Stage 2.1 使用高德地图 JS API 2.0 实现地图浏览、Marker 展示和发布活动选点。不要把真实 Key 写进源码，也不要提交真实 `.env` 文件。

1. 复制前端环境变量模板：

```powershell
cd D:\last_one-form-group\city-party-platform\frontend
Copy-Item .env.example .env.development
```

2. 在 `frontend\.env.development` 中填写自己的高德配置：

```text
VITE_AMAP_KEY=你的高德地图 JS API Key
VITE_AMAP_SECURITY_CODE=你的高德 JS API 安全密钥
```

3. 重启前端：

```powershell
npm run dev
```

如果未配置 Key 或安全密钥，地图页和选点弹窗会显示明确提示，发布活动仍可手动填写城市、地址、经度和纬度。

## Redis 候补队列说明

Stage 2.1 使用 Redis List 维护候补顺序，Key 格式为：

```text
activity:waitlist:{activityId}
```

选择 Redis List 的原因是业务只需要先进先出：用户加入候补时 `RPUSH`，有人退出已通过报名后 `LPOP` 取第一位自动转正。MySQL 的 `activity_waitlist` 表会同时保存候补记录，避免 Redis 数据丢失后业务状态不可追踪。

## 测试账号

所有测试账号密码均为 `123456`。

| 角色 | 账号 |
| --- | --- |
| 管理员 | admin |
| 普通用户 | user01 |
| 普通用户 | user02 |
| 普通用户 | user03 |
| 普通用户 | user04 |

## 核心接口测试步骤

1. 获取验证码：`GET /api/auth/captcha`
2. 登录：`POST /api/auth/login`
3. 当前用户：`GET /api/user/me`
4. 个人中心概览：`GET /api/user/profile-overview`
5. 修改资料：`PUT /api/user/profile`
6. 发布活动：`POST /api/activities`
7. 编辑活动：`PUT /api/activities/{id}`
8. 取消活动：`PATCH /api/activities/{id}/cancel`
9. 结束活动：`PATCH /api/activities/{id}/finish`
10. 活动列表：`GET /api/activities`
11. 活动详情：`GET /api/activities/{id}`
12. 我的活动分类：`GET /api/activities/my?type=published`
13. 附近活动：`GET /api/activities/nearby`
14. 报名活动：`POST /api/activities/{id}/signup`
15. 退出报名：`POST /api/activities/{id}/signup/cancel`
16. 加入候补：`POST /api/activities/{id}/waitlist`
17. 取消候补：`POST /api/activities/{id}/waitlist/cancel`
18. 查看候补列表：`GET /api/activities/{id}/waitlist`
19. 审核报名：`POST /api/signups/{signupId}/review`
20. 收藏活动：`POST /api/activities/{id}/favorite`
21. 我的收藏：`GET /api/favorites/my`
22. 我的通知：`GET /api/notices/my`
23. 标记通知已读：`PUT /api/notices/{id}/read`
24. 未读通知数：`GET /api/notices/unread-count`
25. 信用中心概览：`GET /api/credit/overview`
26. 我的评价：`GET /api/reviews/my?type=received`
27. 后台看板：`GET /api/admin/dashboard`
28. 后台评价：`GET /api/admin/reviews`
29. 后台信用记录：`GET /api/admin/credits`
30. 后台通知记录：`GET /api/admin/notices`

请求需要在 `Authorization` 请求头携带：

```text
Bearer 登录返回的token
```

未匹配的 `/api/**` 请求会返回统一错误结构：

```json
{
  "code": 404,
  "message": "接口不存在",
  "data": null
}
```

## 第一阶段已完成功能

- 登录注册与 JWT 鉴权
- Redis 文本验证码
- USER / ADMIN 角色权限
- 当前用户信息、资料编辑、兴趣标签
- 用户公开主页
- 头像上传、活动封面上传
- 活动发布、列表、详情、分类筛选
- 报名、退出、发起人审核
- 收藏、取消收藏、我的收藏
- 用户端 H5 页面
- 管理员后台基础页面
- 数据看板、用户管理、活动管理、报名管理、信用分展示、举报入口
- 数据库结构和测试数据
- Knife4j 接口文档

## 第二阶段 2.1 已完成功能

- 高德地图配置占位和安全密钥读取
- 用户端 `/map` 附近活动地图页
- 活动 Marker、距离筛选、地图详情跳转
- 发布活动地图选点和经纬度回填
- `GET /api/activities/nearby` 附近活动接口
- Redis List 候补队列和 MySQL 候补记录
- 候补自动转正和系统通知记录
- 个人中心系统通知基础列表与已读状态
- 后台活动候补人数展示和报名状态筛选

## 第二阶段 2.2 已完成功能

- WebSocket 活动群聊：`ws://127.0.0.1:5173/ws?token=<JWT>`，前端 dev server 通过 `/ws` 代理到后端。
- 活动群聊权限：活动发起人和 `APPROVED` 报名用户可进入，未报名、待审核、候补、拒绝、退出用户不可进入。
- 聊天记录 MySQL 持久化：`chat_message` 保存活动、发送人、昵称、头像、内容、类型和发送时间。
- 群聊历史消息接口：`GET /api/activities/{activityId}/chat/messages`。
- HTTP 兼容发消息接口：`POST /api/activities/{activityId}/chat/messages`。
- 群聊权限检查接口：`GET /api/activities/{activityId}/chat/access`。
- 系统通知 WebSocket 实时推送：候补转正等通知写入 MySQL 后会推送给在线用户。
- 通知未读数量和已读状态强化：新增 `PUT /api/notices/read-all`，保留单条已读和未读数接口。

WebSocket 聊天发送格式：

```json
{
  "type": "CHAT",
  "activityId": 1,
  "content": "大家几点集合？"
}
```

WebSocket 聊天广播格式：

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

WebSocket 通知推送格式：

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

## 第二阶段 2.3 已完成功能

- 活动结束后，活动发起人和 `APPROVED` 报名成员可以互相评价。
- 单次评价包含 1–5 分、最多 500 字评价内容和最多 5 个标签。
- 同一活动、评价人、被评价人只能生成一条评价，前后端校验与数据库唯一约束共同防重。
- 信用分规则：5 分 `+2`、4 分 `+1`、3 分 `0`、2 分 `-2`、1 分 `-4`，最终分数限制在 60–120。
- 评价、信用分更新、`credit_record` 日志和系统通知在同一事务中完成。
- 评价通知使用 `ACTIVITY_REVIEW` 类型，并复用 Stage 2.2 WebSocket `NOTICE` 实时推送。
- 新增活动评价、我的评价、信用分明细三个移动端页面。

Stage 2.3 数据库迁移：

```powershell
cd D:\last_one-form-group\city-party-platform
mysql -u$env:MYSQL_USERNAME -p$env:MYSQL_PASSWORD city_party_platform
```

进入 MySQL 后执行：

```sql
SOURCE database/stage2.3-migration.sql;
DESC activity_review;
DESC credit_record;
```

## 第二阶段 2.4 已完成功能

- 个人中心集中展示基础资料、信用分、信用等级、活动统计、评价统计和实时未读通知数。
- 信用等级分为：110–120 优秀、100–109 良好、80–99 正常、60–79 待提升。
- “我的活动”支持我发布的、我参与的、候补中、已结束四类分页列表。
- 已结束活动按 `endTime <= 当前时间` 判断，并限定为当前用户发布或已通过报名参与的活动。
- “我的评价”继续复用 Stage 2.3 的发出/收到评价页面和接口。
- 信用中心复用 `credit_record`，展示变化原因、前后分值、时间及可解析的关联活动。
- 通知中心继续复用 `system_notice` 和 Stage 2.2 WebSocket `NOTICE`，支持单条已读和一键已读。
- 移动端补齐加载、空状态、错误重试、未登录跳转和登录后站内回跳。

Stage 2.4 数据库迁移只补充查询索引，不新增业务表或字段：

```powershell
cd D:\last_one-form-group\city-party-platform
mysql -u$env:MYSQL_USERNAME -p$env:MYSQL_PASSWORD city_party_platform
```

进入 MySQL 后执行：

```sql
SOURCE database/stage2.4-migration.sql;
SHOW INDEX FROM activity_signup;
SHOW INDEX FROM activity_waitlist;
SHOW INDEX FROM system_notice;
```

## 第二阶段 2.5 已完成功能

- 复用现有 `user.role` 字段识别管理员，角色值为 `ADMIN`。
- `/api/admin/**` 由后端 JWT 拦截器统一校验；普通用户调用时返回业务响应码 `403`。
- 个人中心仅对管理员显示“管理员后台”入口，后台页面路径为 `/admin/dashboard`。
- 数据看板展示用户、活动、报名、评价和通知总数。
- 用户管理支持账号、手机号和昵称搜索，并可查看基础资料、信用分及创建时间。
- 活动管理支持关键词、分类和状态筛选，详情中可以查看报名及候补用户。
- 报名、评价、信用记录和系统通知提供只读分页查询。
- 不提供用户禁用、活动下架、数据删除、人工信用分调整或通知群发。
- Stage 2.5 复用已有 `user`、`activity`、`activity_signup`、`activity_waitlist`、`activity_review`、`credit_record` 和 `system_notice`，不新增迁移脚本。

Windows PowerShell 验收命令：

```powershell
Set-Location D:\last_one-form-group\city-party-platform\backend
mvn test

Set-Location D:\last_one-form-group\city-party-platform\frontend
npm run build
```

## 第二阶段 2.6 已完成功能

- 完整建库脚本已同步 `chat_message` 的昵称、头像、消息类型字段和聊天查询索引。
- 新增 `database/stage2.6-migration.sql`，给 `activity_signup` 增加 `activity_id + user_id` 唯一约束，防止并发重复报名。
- 报名、审核通过和候补转正改为数据库条件更新活动人数，降低并发超员风险。
- 用户侧分页统一限制 `current >= 1`、`size <= 100`，附近活动查询增加经纬度边界框预过滤。
- 活动发起人或管理员可以编辑、取消、结束活动；移动端详情页和发布页已支持对应操作。
- JWT 拦截器和 WebSocket 握手会复核数据库中的用户状态和角色，避免旧 token 继续使用旧权限。
- 新注册密码使用 PBKDF2 哈希，兼容旧 SHA-256 哈希；上传图片增加文件头校验。
- CORS 和 WebSocket Origin 支持通过 `CORS_ALLOWED_ORIGIN_PATTERNS` 环境变量配置。
- 登录/注册页不再默认填入演示密码，也不再自动填入验证码。

Stage 2.6 数据库迁移：

```powershell
cd D:\last_one-form-group\city-party-platform
mysql -u$env:MYSQL_USERNAME -p$env:MYSQL_PASSWORD city_party_platform
```

进入 MySQL 后执行：

```sql
SOURCE database/stage2.6-migration.sql;
SHOW INDEX FROM activity_signup;
```

如果旧库中已经存在同一活动、同一用户的重复报名记录，新增唯一约束前需要先人工清理重复数据。

## 后续待开发功能

- AA 账单分摊与模拟支付确认
- 固定搭子申请与兴趣推荐
- 举报处理完整流程
- 推荐算法和热门活动排行

## 常见错误

### MySQL 登录失败

错误示例：

```text
ERROR 1045 (28000): Access denied for user 'root'@'localhost'
```

说明当前密码不正确。请用自己的 MySQL 账号密码设置环境变量：

```powershell
$env:MYSQL_USERNAME="你的账号"
$env:MYSQL_PASSWORD="你的密码"
```

### Redis 连接失败

确认 Redis 6379 是否可用：

```powershell
Test-NetConnection 127.0.0.1 -Port 6379
```

### npm audit 提示漏洞

当前依赖安装后可能提示漏洞。不要直接执行 `npm audit fix --force`，它可能升级主版本并引入兼容性问题。第一阶段先保证项目可运行，后续单独评估依赖升级。
