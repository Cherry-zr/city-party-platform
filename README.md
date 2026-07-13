# 同城活动发现与陌生人组局平台

这是一个面向简历展示和毕业设计场景的全栈项目，核心目标是让同城用户可以发现线下活动、发布活动、报名参与、进入候补队列、群聊沟通、完成评价，并让管理员通过后台查看运营数据和基础业务记录。

项目当前定位是本地可运行、可演示、可继续扩展的完整练习项目；尚未做公网部署，也不描述为线上运营项目。

## 核心功能

- 用户注册、登录、验证码、JWT 鉴权。
- 用户资料、兴趣标签、个人中心、信用分记录。
- 活动发布、编辑、取消、结束、列表、详情、附近活动地图。
- 活动报名、发起人审核、退出报名、候补队列、候补转正。
- 活动收藏、系统通知、WebSocket 群聊。
- 活动结束后互评，评价会影响信用分并写入信用记录。
- 管理员后台：用户、活动、报名、评价、信用、通知、举报只读管理。
- 管理员运营数据看板：概览、趋势、分布、业务质量指标和热门活动排行。
- 项目专用 MySQL / Redis Docker Compose 开发环境。
- 后端单元测试、前端 Playwright 冒烟测试、GitHub Actions CI 和 Gitleaks 安全扫描。

## 技术栈

后端：

- Java 17
- Spring Boot 3.3.7
- Maven
- MyBatis-Plus
- MySQL 8.0
- Redis
- JWT
- WebSocket
- Knife4j

前端：

- Vue 3
- Vite 5
- JavaScript
- Vue Router
- Pinia
- Axios
- Vant
- Element Plus
- ECharts
- Playwright

工程化：

- Docker Compose：仅管理 MySQL 和 Redis
- GitHub Actions：后端测试/打包、前端构建、安全扫描
- Gitleaks：持续密钥扫描

## 系统结构

```text
city-party-platform/
├─ backend/                  # Spring Boot 后端
├─ frontend/                 # Vue3 前端
├─ database/                 # schema、migration、演示数据和清理脚本
├─ docs/                     # 项目文档、简历材料和面试材料
├─ screenshots/              # 安全验收截图
├─ compose.yaml              # MySQL / Redis 本地开发环境
├─ .env.example              # Compose 环境变量模板
└─ README.md
```

更多结构说明见 [docs/architecture.md](docs/architecture.md)。

## 环境要求

- Windows 10/11 + PowerShell
- JDK 17
- Maven 3.8+
- Node.js 20 或兼容版本
- npm
- Docker Desktop
- MySQL 8.0 和 Redis 7 可通过项目 Compose 启动

当前项目默认端口：

| 服务 | 地址 |
| --- | --- |
| 后端 | `http://127.0.0.1:8080` |
| 前端 | `http://127.0.0.1:5173` |
| Docker MySQL | `127.0.0.1:13306` |
| Docker Redis | `127.0.0.1:16379` |

## 环境变量

根目录 `.env.example` 用于 Docker Compose：

```text
MYSQL_HOST_PORT=13306
REDIS_HOST_PORT=16379
MYSQL_PASSWORD=replace_with_a_long_random_password
MYSQL_ROOT_PASSWORD=replace_with_another_long_random_password
TZ=Asia/Shanghai
```

前端 `frontend/.env.example` 用于高德地图：

```text
VITE_AMAP_KEY=your_amap_js_api_key
VITE_AMAP_SECURITY_CODE=your_amap_security_js_code
```

后端常用进程级环境变量：

```powershell
$env:MYSQL_HOST="127.0.0.1"
$env:MYSQL_PORT="13306"
$env:MYSQL_DATABASE="city_party_platform"
$env:MYSQL_USERNAME="city_party"
$env:MYSQL_PASSWORD="你的本地数据库密码"
$env:REDIS_HOST="127.0.0.1"
$env:REDIS_PORT="16379"
```

不要提交真实 `.env`、数据库密码、JWT secret、高德 Key、Token、日志或数据库备份。

## Docker MySQL / Redis

本项目的 Compose 只管理 MySQL 和 Redis，不容器化前端或后端。

```powershell
Set-Location D:\last_one-form-group\city-party-platform
Copy-Item .env.example .env
notepad .env
docker compose config
docker compose up -d
docker compose ps
```

默认容器：

- `city-party-mysql`
- `city-party-redis`

默认数据卷：

- `city_party_mysql_data`
- `city_party_redis_data`

详细说明见 [docs/docker-development.md](docs/docker-development.md)。

## 数据库初始化和 migration

空数据卷首次启动时，Compose 会挂载：

```text
database/schema.sql -> /docker-entrypoint-initdb.d/001-schema.sql
```

`database/schema.sql` 是当前完整基线，已经包含 Stage 2.1 到 Stage 2.7 所需结构。空库首次启动不要再叠加执行 `stage2.*-migration.sql`。

旧库升级时，先在仓库外备份，再按真实库状态选择执行：

- `database/stage2.1-migration.sql`
- `database/stage2.2-migration.sql`
- `database/stage2.3-migration.sql`
- `database/stage2.4-migration.sql`
- `database/stage2.6-migration.sql`

详细说明见 [docs/database-design.md](docs/database-design.md) 和 [docs/development-setup.md](docs/development-setup.md)。

## 导入演示数据

演示数据脚本只处理统一标识的数据：

- 用户名前缀：`cp_demo_`
- 活动和内容标识：`[CITY_PARTY_DEMO]`

导入：

```powershell
Set-Location D:\last_one-form-group\city-party-platform
$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
cmd.exe /d /s /c 'docker exec -i city-party-mysql sh -c "MYSQL_PWD=\"$MYSQL_PASSWORD\" exec mysql --default-character-set=utf8mb4 -ucity_party city_party_platform" < database\demo-data.sql'
```

清理：

```powershell
cmd.exe /d /s /c 'docker exec -i city-party-mysql sh -c "MYSQL_PWD=\"$MYSQL_PASSWORD\" exec mysql --default-character-set=utf8mb4 -ucity_party city_party_platform" < database\demo-cleanup.sql'
```

详细说明见 [docs/demo-guide.md](docs/demo-guide.md)。

## 后端启动

```powershell
Set-Location D:\last_one-form-group\city-party-platform\backend
$env:MYSQL_HOST="127.0.0.1"
$env:MYSQL_PORT="13306"
$env:MYSQL_DATABASE="city_party_platform"
$env:MYSQL_USERNAME="city_party"
$env:MYSQL_PASSWORD="你的本地数据库密码"
$env:REDIS_HOST="127.0.0.1"
$env:REDIS_PORT="16379"
mvn spring-boot:run
```

后端地址：

```text
http://127.0.0.1:8080
```

Knife4j：

```text
http://127.0.0.1:8080/doc.html
```

## 前端启动

```powershell
Set-Location D:\last_one-form-group\city-party-platform\frontend
npm ci
npm run dev
```

前端地址：

```text
http://127.0.0.1:5173
```

Vite 代理：

- `/api` -> `http://127.0.0.1:8080`
- `/uploads` -> `http://127.0.0.1:8080`
- `/ws` -> `ws://127.0.0.1:8080/ws`

## 测试命令

后端：

```powershell
Set-Location D:\last_one-form-group\city-party-platform\backend
mvn test
mvn clean package -DskipTests
mvn dependency:tree
```

前端：

```powershell
Set-Location D:\last_one-form-group\city-party-platform\frontend
npm ci
npm run build
npm run test:e2e
npm audit
npm audit --omit=dev
```

根目录：

```powershell
Set-Location D:\last_one-form-group\city-party-platform
docker compose config --quiet
git diff --check
git status --short
```

阶段 6 最终验收结果：

- 后端测试：85 个，通过 85，失败 0，错误 0，跳过 0。
- Playwright：2 个，通过 2，失败 0。
- 前端构建：通过，Vite 转换模块 2530。

详细说明见 [docs/testing-guide.md](docs/testing-guide.md)。

## 演示步骤

1. 启动 Docker MySQL / Redis。
2. 导入演示数据。
3. 启动后端。
4. 启动前端。
5. 使用演示管理员登录后台。
6. 访问 `/admin/dashboard` 查看运营概览。
7. 访问 `/admin/analytics` 查看数据分析。
8. 使用普通演示用户访问用户端活动列表、详情、发布、编辑、取消、结束等流程。

完整步骤见 [docs/demo-guide.md](docs/demo-guide.md)。

## 截图

安全截图保存在 `screenshots/`，已检查不包含密码、JWT、数据库凭据、高德 Key 或私人联系方式。

| 页面 | 截图 |
| --- | --- |
| 管理员运营概览 | `screenshots/stage2.7-admin-overview.png` |
| 管理员数据分析 | `screenshots/stage2.7-admin-analytics.png` |
| 登录安全 | `screenshots/stage2.6-login-security.png` |
| 数据库唯一索引 | `screenshots/stage2.6-db-unique-index.png` |
| 后端测试 | `screenshots/stage2.6-backend-tests.png` |

## 安全说明

- JWT 只保存用户 ID、用户名、角色和过期时间；HTTP 拦截器和 WebSocket 握手会重新查询数据库用户状态和角色。
- 新注册密码使用 PBKDF2；旧 SHA-256 哈希在登录成功后自动升级。
- `/api/admin/**` 后端强制要求当前数据库用户仍为 `ADMIN` 且状态为 `NORMAL`。
- 报名通过数据库唯一约束和条件更新减少重复报名和并发超员。
- 文件上传校验扩展名和文件头，只允许 JPG、PNG、WebP。
- 首页数据看板概览使用 Redis 5 分钟缓存；Redis 异常时回退数据库。
- CI 中增加 Gitleaks 扫描，发现疑似真实密钥时工作流失败。

## 已知限制

- 目前没有公网部署。
- AA 账单、固定搭子、举报处理仍是预留或基础结构。
- 数据看板只缓存首页概览，关键业务写入后的主动失效尚未统一接入。
- `echarts` 存在 production moderate 级别依赖审计提示，兼容修复需要评估 ECharts 6 升级。
- `vite` / `esbuild` 存在开发依赖审计提示，后续可结合构建链升级统一处理。
- GitHub Actions 当前成功，但 GitHub 平台提示 Node.js 20 action runtime 未来弃用 warning，后续可按平台建议升级。

## 后续公开部署方向

- 独立生成生产环境 JWT secret、数据库密码和地图 Key。
- 禁用或删除演示账号。
- 使用 HTTPS、反向代理和域名。
- 对上传目录接入对象存储或静态资源服务。
- 按公网环境配置 CORS 和 WebSocket Origin。
- 接入日志脱敏、备份策略、监控告警和依赖升级计划。
- 重新执行 Gitleaks、npm audit、Maven dependency 审计和全量测试。
