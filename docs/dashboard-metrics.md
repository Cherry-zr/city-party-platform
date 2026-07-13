# 管理员运营数据看板指标口径

## 通用规则

- 接口前缀：`/api/admin/dashboard`
- 权限：当前数据库用户必须是 `ADMIN`，且状态为 `NORMAL`。
- 统一响应：`Result<T>`。
- 时间边界：左闭右开 `[startDate 00:00:00, endDate + 1 day 00:00:00)`。
- 时区：沿用应用运行环境时区，当前本地开发环境使用 `Asia/Shanghai`。
- 逻辑删除：统计均排除 `deleted=1`。
- 空数据：计数返回 `0`，列表返回空数组，趋势补齐日期并填 `0`。
- 除零：比率和平均值无分母时返回 `0.00`。
- 缓存：只缓存首页概览，Redis key 为 `city-party:admin:dashboard:overview`，TTL 5 分钟；Redis 不可用时回退数据库。

## 时间范围参数

支持枚举：

- `TODAY`
- `THIS_WEEK`
- `THIS_MONTH`
- `LAST_7_DAYS`
- `LAST_30_DAYS`
- `LAST_90_DAYS`
- `THIS_YEAR`
- `CUSTOM`

校验规则：

- 非法枚举返回业务错误。
- `CUSTOM` 必须同时传 `startDate` 和 `endDate`。
- `startDate` 不得晚于 `endDate`。
- 时间跨度不得超过一年。

## 接口列表

| 接口 | 说明 | 是否缓存 |
| --- | --- | --- |
| `GET /api/admin/dashboard/overview` | 首页概览和今日新增 | 是 |
| `GET /api/admin/dashboard/trends` | 用户、活动、报名、评价趋势 | 否 |
| `GET /api/admin/dashboard/distributions` | 状态、分类、信用、评分分布 | 否 |
| `GET /api/admin/dashboard/quality` | 业务质量指标 | 否 |
| `GET /api/admin/dashboard/popular-activities` | 热门活动排行 | 否 |

## 指标明细

| 名称 | 业务含义 | 时间字段 | 包含状态 | 排除状态 | 计算方式 | 接口 |
| --- | --- | --- | --- | --- | --- | --- |
| 用户总数 | 当前未逻辑删除用户数 | 无 | `NORMAL`、`DISABLED` | `deleted=1` | `COUNT(*) FROM user WHERE deleted=0` | `/overview` |
| 活动总数 | 当前未逻辑删除活动数 | 无 | 全部活动状态 | `deleted=1` | `COUNT(*) FROM activity WHERE deleted=0` | `/overview` |
| 报名总数 | 当前未逻辑删除报名记录数 | 无 | 全部报名状态 | `deleted=1` | `COUNT(*) FROM activity_signup WHERE deleted=0` | `/overview` |
| 评价总数 | 当前未逻辑删除评价数 | 无 | 全部评分 | `deleted=1` | `COUNT(*) FROM activity_review WHERE deleted=0` | `/overview` |
| 今日新增用户 | 今日注册用户数 | `user.created_at` | `NORMAL`、`DISABLED` | `deleted=1` | `created_at >= today 00:00` | `/overview` |
| 今日新增活动 | 今日创建活动数 | `activity.created_at` | 全部活动状态 | `deleted=1` | `created_at >= today 00:00` | `/overview` |
| 今日新增报名 | 今日报名记录数 | `activity_signup.created_at` | 全部报名状态 | `deleted=1` | `created_at >= today 00:00` | `/overview` |
| 今日新增评价 | 今日评价数 | `activity_review.created_at` | 全部评分 | `deleted=1` | `created_at >= today 00:00` | `/overview` |
| 用户增长趋势 | 时间范围内每日注册用户数 | `user.created_at` | `NORMAL`、`DISABLED` | `deleted=1` | 按自然日分组，缺失日期补 0 | `/trends` |
| 活动发布趋势 | 时间范围内每日创建活动数 | `activity.created_at` | 全部活动状态 | `deleted=1` | 按自然日分组，缺失日期补 0 | `/trends` |
| 报名趋势 | 时间范围内每日报名记录数 | `activity_signup.created_at` | 全部报名状态 | `deleted=1` | 按自然日分组，缺失日期补 0 | `/trends` |
| 评价趋势 | 时间范围内每日评价数 | `activity_review.created_at` | 全部评分 | `deleted=1` | 按自然日分组，缺失日期补 0 | `/trends` |
| 报名状态分布 | 各报名状态记录量 | 无 | `PENDING`、`APPROVED`、`REJECTED`、`WAITING`、`CANCELLED`、`COMPLETED`、`ABSENT`、`PROMOTED` | `deleted=1` | 按 `status` 分组 | `/distributions` |
| 活动状态分布 | 各活动生命周期状态数量 | 无 | `SIGNING`、`FULL`、`UPCOMING`、`ONGOING`、`FINISHED`、`CANCELLED` | `deleted=1` | 按 `status` 分组 | `/distributions` |
| 分类分布 | 各活动分类数量 | 无 | 全部活动状态 | `deleted=1` | `category` 分组，空值归为“未分类” | `/distributions` |
| 信用分布 | 用户信用分区间分布 | 无 | 全部未删除用户 | `deleted=1` | `<80`、`80-99`、`100-119`、`120+` | `/distributions` |
| 评分分布 | 1 到 5 分评价数量 | 无 | 全部评分 | `deleted=1` | 按 `rating` 分组 | `/distributions` |
| 报名成功率 | 时间范围内成功报名占全部报名比例 | `activity_signup.created_at` | 分子：`APPROVED`、`PROMOTED`、`COMPLETED`；分母：全部报名状态 | `deleted=1` | `成功数 / 报名总数 * 100` | `/quality` |
| 平均参与率 | 时间范围内发布活动的平均满员程度 | `activity.created_at` | 非 `CANCELLED` 活动 | `CANCELLED`、`deleted=1` | `AVG(approved_count / max_participants * 100)` | `/quality` |
| 平均评分 | 时间范围内评价平均分 | `activity_review.created_at` | 全部评分 | `deleted=1` | `AVG(rating)` | `/quality` |
| 候补数 | 时间范围内创建且当前仍在候补的记录 | `activity_waitlist.created_at` | `WAITING` | 其他状态、`deleted=1` | `COUNT(*)` | `/quality` |
| 退出数 | 时间范围内创建且最终取消的报名记录 | `activity_signup.created_at` | `CANCELLED` | 其他状态、`deleted=1` | `COUNT(*)` | `/quality` |
| 异常信用用户数 | 当前信用分低于 80 的用户数量 | 无 | `credit_score < 80` | `deleted=1` | `COUNT(*)` | `/quality` |
| 热门活动排行 | 时间范围内成功报名和候补较多的活动 | 报名和候补 `created_at` | 报名：`APPROVED`、`PROMOTED`、`COMPLETED`；候补：`WAITING` | `deleted=1` | 活动聚合后按成功报名、候补、活动 ID 降序；limit 1 到 50 | `/popular-activities` |

## SQL 对应文件

- Service：`backend/src/main/java/com/cityparty/module/admin/service/DashboardService.java`
- Mapper：`backend/src/main/java/com/cityparty/module/admin/mapper/DashboardMapper.java`
- XML：`backend/src/main/resources/mapper/DashboardMapper.xml`
- VO：`backend/src/main/java/com/cityparty/module/admin/vo/DashboardAnalyticsVO.java`
- 时间枚举：`backend/src/main/java/com/cityparty/module/admin/dto/DashboardPeriod.java`

## 主动缓存失效说明

当前首页概览依赖 5 分钟 TTL 保证最终一致性，尚未侵入用户、活动、报名、评价等写服务做主动失效。关键业务写入后的主动失效留待后续统一评估。
