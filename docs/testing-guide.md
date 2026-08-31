# 测试体系说明

## 后端测试

测试目录：

```text
backend/src/test/java/com/cityparty
```

运行：

```powershell
Set-Location D:\last_one-form-group\city-party-platform\backend
mvn test
```

Stage 2.8 最终验收结果：

- 测试总数：120
- 失败：0
- 错误：0
- 跳过：0

覆盖方向：

- 用户注册、登录、密码兼容和升级。
- JWT HTTP 拦截器数据库复核。
- 被禁用用户旧 JWT 失效。
- 角色变更后旧 JWT 失效。
- 活动发布、编辑、取消、结束。
- 非发起人编辑权限。
- 重复报名。
- 唯一约束冲突。
- 多用户竞争最后一个名额。
- 审核通过导致满员。
- 候补转正。
- 候补与新报名竞争。
- 取消报名人数回退。
- 取消和结束后的状态限制。
- WebSocket 鉴权。
- 文件上传空文件、伪造图片、文件头校验。
- 管理员统计权限。
- 统计日期校验。
- Redis 故障回退。
- 空统计和除零。
- `RecommendationScorerTest`：兴趣、距离、热度、时间、信用和缺失特征动态权重。
- `RecommendationServiceTest`：候选过滤、FULL 候补、兴趣/位置排序、冷启动、稳定排序、Top N、Redis 缓存与故障回退。

## 后端打包

```powershell
Set-Location D:\last_one-form-group\city-party-platform\backend
mvn clean package
```

Stage 2.8 最终验收：120 项测试通过并成功生成 JAR。

## Maven 依赖树

```powershell
Set-Location D:\last_one-form-group\city-party-platform\backend
mvn dependency:tree
```

阶段 6 结论：

- 未发现明显多版本冲突。
- 未发现测试依赖错误进入 compile/runtime 的问题。
- 未做 Spring Boot、MyBatis、Redis 等框架大版本升级。

## 前端构建

```powershell
Set-Location D:\last_one-form-group\city-party-platform\frontend
npm run build
```

Stage 2.8 最终验收：

- `npm run build`：通过。
- Vite 转换模块：2549。

既存 warning：

- ECharts / 前端主 chunk 超过 Vite 默认 500 KB 提示线。
- `@vueuse/core` PURE 注释位置 warning。

这些 warning 不影响当前构建成功。

## Playwright 冒烟测试

测试文件：

```text
frontend/e2e/city-party-smoke.spec.js
```

运行：

```powershell
Set-Location D:\last_one-form-group\city-party-platform\frontend
npm run test:e2e
```

需要环境变量提供测试账号：

```powershell
$env:E2E_ADMIN_USERNAME="本地演示管理员账号"
$env:E2E_ADMIN_PASSWORD="本地演示管理员密码"
$env:E2E_USER_USERNAME="本地普通用户账号"
$env:E2E_USER_PASSWORD="本地普通用户密码"
$env:E2E_API_BASE_URL="http://127.0.0.1:8080"
$env:PLAYWRIGHT_BASE_URL="http://127.0.0.1:5173"
```

测试策略：

- 只读流程使用固定演示账号。
- 写操作创建带统一前缀的唯一活动。
- 写操作使用隔离验收数据库，验收后删除本次临时容器。
- 不清空或清理开发数据库。

Stage 2.8 最终验收：

- Playwright 用例数：6
- 失败：0
- E2E 临时数据残留：0

覆盖方向：

- 登录。
- 活动列表。
- 活动详情。
- 发布。
- 编辑。
- 取消。
- 结束。
- 管理员后台首页。
- 数据分析页。
- 时间筛选。
- 普通用户后台拦截。
- 关键元素加载。
- 明显控制台错误检查。
- 登录用户首页“为你推荐”、推荐理由和推荐活动详情跳转。
- 访客首页不展示推荐区且不请求推荐接口。
- 搜索、分类筛选和定位拒绝不清空推荐结果。
- 推荐场景通过 mock API 和 WebSocket 隔离，不依赖 CI 的真实定位权限。

## npm audit

```powershell
Set-Location D:\last_one-form-group\city-party-platform\frontend
npm audit
npm audit --omit=dev
```

阶段 6 结果：

- 总漏洞：3
- production 依赖：`echarts` moderate
- dev 依赖：`vite` high，`esbuild` moderate

未执行 `npm audit fix --force`。原因是当前兼容修复需要大版本升级，超出阶段 6 和阶段 7 文档范围。

## Compose 配置检查

```powershell
Set-Location D:\last_one-form-group\city-party-platform
docker compose config --quiet
```

如根目录没有 `.env`，该命令会提示缺少 `MYSQL_PASSWORD` 或 `MYSQL_ROOT_PASSWORD`。本地开发时需要先复制 `.env.example` 为 `.env` 并填写本地密码。

## Git 检查

```powershell
Set-Location D:\last_one-form-group\city-party-platform
git diff --check
git status --short
```

提交前不要包含：

- `.env`
- 数据库备份
- 日志
- Token
- JWT
- 密码
- Playwright 报告
- trace
- video
- `frontend/dist/`
- `backend/target/`

