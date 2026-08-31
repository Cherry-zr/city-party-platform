# 系统架构说明

## 项目定位

本项目是一个本地可运行的全栈简历项目，围绕“同城活动发现与陌生人组局”实现用户端、管理端、数据看板、Docker 开发环境和基础 CI。项目当前用于本地演示和学习，不描述为线上运营项目。

## 总体结构

```text
city-party-platform/
├─ backend/      Spring Boot 后端
├─ frontend/     Vue3 前端
├─ database/     数据库 schema、migration、演示数据
├─ docs/         项目文档
├─ screenshots/  安全验收截图
└─ compose.yaml  MySQL / Redis 本地开发环境
```

## 后端分层

后端入口是 `backend/src/main/java/com/cityparty/CityPartyApplication.java`。

主要分层：

- `controller`：HTTP 接口入口，返回统一 `Result<T>`。
- `service`：业务规则、事务、权限复核、状态流转。
- `mapper`：MyBatis-Plus Mapper 或 XML 聚合 SQL。
- `entity`：数据库实体。
- `dto`：请求对象。
- `vo`：响应对象。
- `common/security`：JWT、当前用户上下文和 HTTP 拦截器。
- `common/websocket`：WebSocket 握手鉴权、在线会话、实时通知和群聊推送。
- `common/utils`：密码、分页、文件上传等工具。
- `module/recommendation`：候选召回、纯特征评分、稳定排序、推荐理由和 Redis 缓存。

## 前端结构

前端入口：

- `frontend/src/main.js`
- `frontend/src/App.vue`

主要目录：

- `frontend/src/router/index.js`：用户端和后台路由。
- `frontend/src/api/`：Axios API 封装。
- `frontend/src/stores/`：Pinia 登录态和通知状态。
- `frontend/src/views/mobile/`：移动端用户页面。
- `frontend/src/views/admin/`：管理员后台页面。
- `frontend/src/components/`：活动卡片、地图选择、图表组件。
- `frontend/e2e/`：Playwright 冒烟测试。

## 请求链路

```text
Vue 页面
  ↓
frontend/src/api/request.js
  ↓
Vite dev proxy: /api -> 127.0.0.1:8080
  ↓
Spring MVC Controller
  ↓
JwtInterceptor / UserContext
  ↓
Service 业务规则
  ↓
Mapper / MySQL / Redis
  ↓
Result<T> 统一响应
```

个性化推荐链路仍在当前单体应用内，不是独立微服务：

```text
Vue Home
  ↓
RecommendationController
  ↓
RecommendationService
  ↓
RecommendationScorer
  ↓
MySQL + Redis
```

`RecommendationService` 从 MySQL 召回最多 100 条原始活动并批量读取发起人，先评分排序，再只对 Top N 执行完整 VO 转换。Redis 仅缓存 5 分钟推荐结果；缓存异常时回退 MySQL 实时计算。

## WebSocket 链路

开发环境连接地址：

```text
ws://127.0.0.1:5173/ws?token=<JWT>
```

真实后端端点：

```text
/ws
```

Vite 将 `/ws` 代理到后端。`JwtHandshakeInterceptor` 从查询参数读取 token，解析后重新查询数据库用户状态，禁用用户或已删除用户不能建立连接。`CityPartyWebSocketHandler` 只处理 `CHAT` 类型消息，并通过 `ActivityChatService` 复核用户是否有活动群聊访问权。

## 管理后台

后台路由在 `frontend/src/router/index.js` 中挂载到 `/admin`，前端通过 `meta.admin` 做路由拦截；后端通过 `JwtInterceptor` 对 `/api/admin/**` 做强制角色校验。

后台页面：

- `/admin/dashboard`：运营概览。
- `/admin/analytics`：独立数据分析页。
- `/admin/users`：用户管理。
- `/admin/activities`：活动管理。
- `/admin/signups`：报名管理。
- `/admin/reviews`：评价管理。
- `/admin/credits`：信用记录。
- `/admin/notices`：通知管理。
- `/admin/reports`：举报列表。

## 存储组件

MySQL：

- 保存用户、活动、报名、候补、收藏、群聊、评价、信用、通知、举报和预留业务表。
- `activity_signup(activity_id,user_id)` 和 `activity_review(activity_id,reviewer_id,target_user_id)` 使用唯一约束防重复。

Redis：

- 验证码：`captcha:{key}`。
- 候补队列：`activity:waitlist:{activityId}`。
- 看板概览缓存：`city-party:admin:dashboard:overview`。
- 个性化推荐缓存：`city-party:recommendation:activities:{userId}:{location}:{limit}:{interestFingerprint}`。

## CI 和安全扫描

GitHub Actions：

- `.github/workflows/ci.yml`：后端 Maven 测试和打包，前端 npm ci 和 build。
- `.github/workflows/security-scan.yml`：Gitleaks 全历史密钥扫描。
