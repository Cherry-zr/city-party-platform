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
4. 修改资料：`PUT /api/user/profile`
5. 发布活动：`POST /api/activities`
6. 活动列表：`GET /api/activities`
7. 活动详情：`GET /api/activities/{id}`
8. 附近活动：`GET /api/activities/nearby`
9. 报名活动：`POST /api/activities/{id}/signup`
10. 退出报名：`POST /api/activities/{id}/signup/cancel`
11. 加入候补：`POST /api/activities/{id}/waitlist`
12. 取消候补：`POST /api/activities/{id}/waitlist/cancel`
13. 查看候补列表：`GET /api/activities/{id}/waitlist`
14. 审核报名：`POST /api/signups/{signupId}/review`
15. 报名兼容接口：`POST /api/signups`，请求体传 `activityId`
16. 审核通过兼容接口：`POST /api/signups/{signupId}/approve`
17. 审核拒绝兼容接口：`POST /api/signups/{signupId}/reject`
18. 收藏活动：`POST /api/activities/{id}/favorite`
19. 收藏兼容接口：`POST /api/favorites/{activityId}`
20. 我的收藏：`GET /api/favorites/my`
21. 我的通知：`GET /api/notices/my`
22. 标记通知已读：`PUT /api/notices/{id}/read`
23. 未读通知数：`GET /api/notices/unread-count`
24. 后台看板：`GET /api/admin/dashboard`
25. 后台用户：`GET /api/admin/users`
26. 后台活动：`GET /api/admin/activities`
27. 后台报名：`GET /api/admin/signups`

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

## 第二阶段 2.2 待开发功能

- WebSocket 活动群聊和实时通知
- 完整信用分加减规则
- 管理员人工调整信用分
- AA 账单分摊与模拟支付确认
- 固定搭子申请与兴趣推荐
- 系统通知已读未读
- 双向评价
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
