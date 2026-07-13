# 最终验收记录

## 验收范围

本项目已完成到 Stage 2.7 收尾阶段，包括：

- Stage 1.1 基础闭环。
- Stage 2.1 地图和候补。
- Stage 2.2 WebSocket 群聊和通知。
- Stage 2.3 评价和信用。
- Stage 2.4 个人中心。
- Stage 2.5 管理员后台基础管理。
- Stage 2.6 数据库一致性、安全和并发加固。
- Stage 2.7 后端数据看板、前端后台 UI、Docker 开发环境、测试体系、CI、安全扫描和项目材料。

## Git 基线

阶段 7 开始前：

- 分支：`stage2.7-dashboard-and-finalization`
- HEAD：`22c7431607efc08c9a3be231f9c810db31c7e11e`
- 工作区：干净

## 自动化验证结果

阶段 6 最终补充验收已记录：

| 项目 | 结果 |
| --- | --- |
| `mvn test` | 通过，85 个测试，失败 0，错误 0，跳过 0 |
| `mvn clean package -DskipTests` | 通过 |
| `mvn dependency:tree` | 通过 |
| `npm ci` | 通过 |
| `npm run build` | 通过，Vite 转换模块 2530 |
| `npm run test:e2e` | 通过，2 个用例，失败 0 |
| `docker compose config --quiet` | 通过 |
| `git diff --check` | 通过 |
| Gitleaks 本地扫描 | 21 个提交，未发现泄漏 |
| GitHub Actions CI | 成功 |
| GitHub Actions Security Scan | 成功 |

## 浏览器验收

已验收页面：

- 用户端登录。
- 活动列表。
- 活动详情。
- 活动发布。
- 活动编辑。
- 活动取消。
- 活动结束。
- 管理员后台首页 `/admin/dashboard`。
- 独立数据分析页 `/admin/analytics`。
- 最近 90 天、本年度、自定义日期筛选。
- 普通用户后台路由拦截。

浏览器控制台：

- Playwright 冒烟测试中未发现新增明显 error / warning。

## 安全截图

安全截图目录：

```text
screenshots/
```

重点截图：

| 文件 | 内容 |
| --- | --- |
| `screenshots/stage2.7-admin-overview.png` | 管理员运营概览 |
| `screenshots/stage2.7-admin-analytics.png` | 管理员数据分析页 |
| `screenshots/stage2.6-login-security.png` | 登录安全 |
| `screenshots/stage2.6-db-unique-index.png` | 数据库唯一索引 |
| `screenshots/stage2.6-backend-tests.png` | 后端测试 |

已人工检查：

- 不展示密码。
- 不展示 JWT 或 Token。
- 不展示数据库凭据。
- 不展示高德 Key。
- 不展示私人邮箱或手机号。
- 不展示浏览器 Cookie。

部分截图包含演示/测试活动名称和演示数据，属于本地验收材料。

## 当前可演示能力

- 用户注册登录和资料。
- 活动发现、发布、编辑、取消、结束。
- 地图附近活动。
- 报名、审核、退出、候补、候补转正。
- 活动群聊。
- 活动评价和信用分。
- 系统通知。
- 管理员后台基础查询。
- 管理员数据看板。
- Docker MySQL / Redis 环境。
- 演示数据导入和清理。
- 后端、前端、E2E、CI、安全扫描。

## 已知限制

- 未做公网部署。
- 未做真实支付。
- AA 账单、固定搭子、举报处理仍是预留或基础功能。
- 数据看板只缓存首页概览，业务写入主动失效留待后续统一评估。
- 当前依赖审计仍有非阻塞风险：`echarts`、`vite`、`esbuild`。
- GitHub Actions 存在 Node.js 20 runtime 未来弃用 warning，当前不影响运行成功。

## 进入后续阶段前建议

- 如果准备公开仓库，再执行一次 Gitleaks 和人工敏感信息审查。
- 重新确认 `.env`、日志、备份、上传文件未被提交。
- 重新跑完整测试命令。
- 若准备部署公网，先替换所有本地开发密钥并删除演示账号。

