# 安全与并发设计

## JWT 鉴权

JWT 由 `JwtUtils` 生成，签名算法是 HMAC-SHA256。payload 包含：

- `userId`
- `username`
- `role`
- `exp`

HTTP 请求由 `JwtInterceptor` 处理：

1. 提取 `Authorization: Bearer <token>`。
2. 校验 token 格式、签名和过期时间。
3. 根据 token 中的 `userId` 查询数据库。
4. 用户不存在、已删除或状态不是 `NORMAL` 时返回 401。
5. 访问 `/api/admin/**` 时，要求数据库中的当前角色为 `ADMIN`，否则返回 403。
6. 请求结束后清理 `UserContext`。

这样可以避免旧 JWT 在用户被禁用或角色被移除后继续拥有旧权限。

## 密码存储

新注册用户使用 PBKDF2：

- 算法：`PBKDF2WithHmacSHA256`
- 迭代次数：120000
- 盐长度：16 字节
- 输出长度：256 bit
- 格式：`pbkdf2$iterations$salt$hash`

旧数据兼容：

- 旧 SHA-256 哈希仍可登录。
- 登录成功后自动升级为 PBKDF2。

## 管理员权限

前端：

- `/admin` 路由带 `meta.admin`。
- 普通用户访问后台会被前端路由导回用户端。

后端：

- `/api/admin/**` 由 `JwtInterceptor` 强制校验数据库中的当前角色。
- 普通用户直接请求后台接口返回 403。
- 未登录请求返回 401。

权限不依赖菜单隐藏。

## WebSocket 鉴权

WebSocket 握手由 `JwtHandshakeInterceptor` 处理：

1. 从 `/ws?token=<JWT>` 查询参数读取 token。
2. 校验 token。
3. 重新查询数据库用户状态。
4. 无 token 或用户失效时拒绝握手。

消息阶段由 `CityPartyWebSocketHandler` 处理：

- 只接受 `CHAT` 类型。
- 发送前调用 `ActivityChatService` 检查活动群聊权限。
- 只有活动发起人或 `APPROVED` 报名用户可以进入活动群聊。

## 文件上传校验

`FileUploadUtils` 做两层校验：

1. 扩展名只允许 `jpg`、`jpeg`、`png`、`webp`。
2. 读取文件头校验真实图片签名。

空文件、伪造扩展名或超出后端 multipart 限制的文件会被拒绝。

上传目录：

- `backend/uploads/avatar/`
- `backend/uploads/activity/`

运行上传文件不应提交到 Git。

## 防止重复报名

业务层：

- 报名前查询同一活动同一用户未删除报名。
- 已处于 `PENDING`、`APPROVED`、`WAITING` 时直接拒绝。

数据库层：

- `activity_signup` 存在唯一约束 `uk_signup_activity_user(activity_id,user_id)`。

测试覆盖：

- 重复报名。
- 唯一约束冲突。
- 多用户竞争最后一个名额。

## 减少并发超员

活动人数使用 `activity.approved_count`。

通过名额时调用数据库条件更新：

```text
approved_count < max_participants
```

只有更新成功才认为占位成功。并发场景下，失败请求返回业务错误，不会继续写成成功状态。

## 候补转正

候补使用 Redis List 维护顺序，MySQL 保存持久状态。

转正流程：

1. 有名额空出时触发。
2. 先从 Redis `activity:waitlist:{activityId}` 执行 `LPOP`。
3. Redis 不可用或为空时，回退查询 MySQL 中最早的 `WAITING` 候补。
4. 条件更新 `activity.approved_count`。
5. 条件更新 `activity_waitlist` 状态为 `PROMOTED`。
6. 更新或创建对应 `activity_signup` 为 `APPROVED`。
7. 发送系统通知。

## Redis 与数据库一致性

验证码：

- Redis 是唯一存储，验证码过期或删除后不可复用。

候补：

- MySQL 是最终可信状态。
- Redis 只维护队列顺序。
- Redis 队列失效时可从 MySQL `WAITING` 记录回退。

看板：

- 只缓存首页概览，key 为 `city-party:admin:dashboard:overview`，TTL 5 分钟。
- Redis 读写异常时记录 warn 并回退数据库。
- 趋势、分布、质量指标、排行不缓存。
- 当前依靠 TTL 保证最终一致性，关键业务写入后的主动失效留待后续统一评估。

## 安全扫描

本地和远程使用 Gitleaks 扫描可见 Git 历史。CI 配置在 `.github/workflows/security-scan.yml`，发现疑似真实密钥时失败。

工作流关闭 artifact 上传和自动评论，避免扫描命中时公开敏感上下文。

