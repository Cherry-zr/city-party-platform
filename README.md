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

当前开发机已检查到 Java 17.0.16、Maven 3.9.15、Node.js v24.11.0、npm 11.6.1、MySQL 8.0.33，Redis 6379 端口已连通。

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
8. 报名活动：`POST /api/activities/{id}/signup`
9. 退出报名：`POST /api/activities/{id}/signup/cancel`
10. 审核报名：`POST /api/signups/{signupId}/review`
11. 收藏活动：`POST /api/activities/{id}/favorite`
12. 我的收藏：`GET /api/favorites/my`
13. 后台看板：`GET /api/admin/dashboard`
14. 后台用户：`GET /api/admin/users`
15. 后台活动：`GET /api/admin/activities`
16. 后台报名：`GET /api/admin/signups`

请求需要在 `Authorization` 请求头携带：

```text
Bearer 登录返回的token
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

## 第二阶段待开发功能

- 高德地图真实接入、地图选点、附近活动 Marker
- WebSocket 活动群聊和实时通知
- Redis 候补队列与候补自动转正
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
