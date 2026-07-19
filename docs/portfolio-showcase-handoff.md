# CityParty 展示资产交接

> 供后续 Vue Portfolio 任务读取。本文只描述 CityParty 仓库及其展示资产；当前任务未读取或修改个人作品集项目。

## 项目定位

- 项目名称：CityParty — 同城活动发现与陌生人组局平台
- 仓库：<https://github.com/Cherry-zr/city-party-platform>
- 展示口径：可在本地完整运行的全栈作品项目
- 部署状态：未开放公网访问
- 数据口径：截图中的活动、用户、聊天、通知、信用与后台统计均为本地展示数据，不是线上用户或运营指标

推荐的一句话介绍：

> CityParty 是一个基于 Vue 3 与 Spring Boot 的同城活动组局平台，覆盖活动发现、地图浏览、报名候补、实时群聊、互评信用和管理员数据分析等业务流程。

## 已核实技术栈

| 层级 | 技术 |
| --- | --- |
| 用户端 | Vue 3、Vite、Vue Router、Pinia、Axios、Vant |
| 管理端 | Vue 3、Element Plus、ECharts |
| 后端 | Java 17、Spring Boot、MyBatis-Plus、Maven |
| 数据与缓存 | MySQL、Redis |
| 地图与实时通信 | 高德地图 JS API、WebSocket |
| 工程化 | Playwright、JUnit、Docker Compose、GitHub Actions、Gitleaks |

## 可展示功能

- 活动列表、分类检索、活动详情与地图附近活动浏览
- 活动发布、编辑、取消、结束等生命周期操作
- 报名审核、容量控制、候补排队与候补状态展示
- 活动群聊、系统通知及未读状态
- 活动结束后的成员互评与信用记录
- 管理员概览、趋势图、分布图、业务质量指标与活动管理

## 可用于项目介绍的技术难点

1. **报名与候补一致性**：数据库唯一约束和条件更新用于降低重复报名与并发超员风险；Redis 维护候补顺序，MySQL 保存最终业务状态并提供回退查询。
2. **鉴权与账号安全**：JWT 请求鉴权会复核数据库中的用户状态与角色，密码使用 PBKDF2，并保留旧密码摘要的升级路径。
3. **实时互动闭环**：活动成员通过 WebSocket 群聊，系统通知维护未读状态；活动完成后可互评并形成信用记录。
4. **展示截图可靠性**：自动化流程同时检查接口、字体、图片自然尺寸、地图标记与像素多样性、ECharts 画布及连续两次视觉稳定性，任何关键条件失败都不会发布正式截图。

这些内容应描述为“项目实现”或“本地验证结果”，不要扩展为生产级高并发、真实线上运营规模或真实用户增长。

## 展示数据与截图流程

- 数据脚本：`database/showcase-data.sql`
- 清理脚本：`database/showcase-cleanup.sql`
- 用户前缀：`cp_showcase_`
- 关联内容标识：`[CITY_PARTY_SHOWCASE]`
- 本地封面：`frontend/public/showcase/covers/`
- 本地头像：`frontend/public/showcase/avatars/`
- 截图脚本：`frontend/scripts/capture-showcase.mjs`
- 正式截图目录：`screenshots/showcase/`

截图上下文只保留真实 API 返回中的展示活动，后台图表使用与种子数据一致的确定性展示数据，从而避免本机历史验收记录混入作品截图。高德地图仍使用实际地图资源，脚本会检查道路、地名、地图图层、活动标记及地图区域的颜色变化。

在 `frontend/` 目录中运行：

```powershell
$env:MYSQL_PASSWORD="<local database password>"
$env:SHOWCASE_PASSWORD="<local-only showcase account password>"
npm run showcase:seed
npm run capture:showcase
npm run showcase:cleanup
```

运行截图前还需启动后端、前端和 Redis，并在本地配置高德地图 Key 与安全密钥。不得把这些环境变量的实际值写入文档、截图、提交或 PR。

## 截图资产清单

| 文件 | 视口 | 内容 | README 精选 | Portfolio 建议 |
| --- | --- | --- | --- | --- |
| `screenshots/showcase/mobile-home.png` | 430×932 | 活动发现与本地封面 | 是 | 首屏主图 |
| `screenshots/showcase/mobile-map.png` | 430×932 | 真实地图、道路地名、标记与附近活动 | 是 | 核心亮点 |
| `screenshots/showcase/mobile-activity-detail.png` | 430×932 | 活动详情和报名状态 | 是 | 核心流程 |
| `screenshots/showcase/mobile-waitlist.png` | 430×932 | 满员活动与候补状态 | 否 | 流程补充 |
| `screenshots/showcase/mobile-chat.png` | 430×932 | 活动群聊 | 是 | 核心流程 |
| `screenshots/showcase/mobile-notices.png` | 430×932 | 通知与未读状态 | 否 | 能力补充 |
| `screenshots/showcase/mobile-credit.png` | 430×932 | 信用分与变化记录 | 否 | 闭环补充 |
| `screenshots/showcase/admin-dashboard.png` | 1440×900 | 管理概览、趋势、排行与质量指标 | 是 | 后台主图 |
| `screenshots/showcase/admin-analytics.png` | 1440×900 | 多维度趋势分析 | 是 | 技术补充 |
| `screenshots/showcase/admin-activities.png` | 1440×900 | 六条展示活动的管理表格 | 否 | 管理能力补充 |

Portfolio 推荐优先使用 README 精选的 6 张；若页面空间有限，可保留 `mobile-home.png`、`mobile-map.png`、`mobile-chat.png` 与 `admin-dashboard.png` 四张。

## 验证与表述边界

- 本轮已验证：后端 `mvn test`（94 项通过）、前端 `npm run build`、现有 Playwright 冒烟测试（4 项通过）和 10 张截图的自动门禁流程。
- `docs/final-acceptance.md` 中的测试数量属于仓库既有最终验收记录，不是实时覆盖率或线上运行指标。
- README 和 Portfolio 可以写“本地演示可运行”“覆盖完整核心业务流程”，不得写“已公网运营”“拥有真实用户”“生产级高并发”或未经验证的覆盖率。
- 图片、地图、图表和数据均应按本地展示资产介绍，不得把截图中的数量包装为真实平台运营数据。
