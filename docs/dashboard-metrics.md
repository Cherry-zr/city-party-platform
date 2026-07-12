# Stage 2.7 管理员运营数据看板指标口径

所有日期按应用服务器默认时区（当前项目约定为部署环境时区）计算，查询使用左闭右开区间 `[startDate 00:00, endDate + 1 day 00:00)`。所有业务表均排除 `deleted=1`。空计数为 `0`，空比率/平均值为 `0.00`，分母为零时返回 `0.00`。

| 指标 | 业务含义 | 时间字段 | 包含状态 | 排除状态 | 计算方式 | 接口 |
|---|---|---|---|---|---|---|
| 用户总数/今日新增 | 已注册且未软删除用户/今日注册数 | `user.created_at` | `NORMAL`,`DISABLED` | 软删除 | `COUNT(*)` | `/overview` |
| 活动总数/今日新增 | 已创建活动/今日发布数 | `activity.created_at` | 全部活动状态 | 软删除 | `COUNT(*)` | `/overview` |
| 报名总数/今日新增 | 报名申请记录/今日申请数 | `activity_signup.created_at` | 全部报名状态 | 软删除 | `COUNT(*)` | `/overview` |
| 评价总数/今日新增 | 有效评价/今日评价数 | `activity_review.created_at` | 全部评分 | 软删除 | `COUNT(*)` | `/overview` |
| 用户增长趋势 | 每日注册用户数 | `user.created_at` | 全部 | 软删除 | 按自然日分组，缺失日补 0 | `/trends` |
| 活动发布趋势 | 每日创建活动数 | `activity.created_at` | 全部 | 软删除 | 按自然日分组，缺失日补 0 | `/trends` |
| 报名趋势 | 每日报名申请数 | `activity_signup.created_at` | 全部 | 软删除 | 按自然日分组，缺失日补 0 | `/trends` |
| 评价趋势 | 每日评价数 | `activity_review.created_at` | 全部 | 软删除 | 按自然日分组，缺失日补 0 | `/trends` |
| 报名状态分布 | 各报名状态记录量 | 无 | 全部真实状态 | 软删除 | 按 `status` 分组 | `/distributions` |
| 活动状态分布 | 各活动生命周期状态量 | 无 | 全部真实状态 | 软删除 | 按 `status` 分组 | `/distributions` |
| 分类分布 | 各活动分类数量 | 无 | 全部 | 软删除 | 空分类归为“未分类” | `/distributions` |
| 信用分布 | 用户信用分层 | 无 | 全部用户状态 | 软删除 | `<80`,`80-99`,`100-119`,`120+` | `/distributions` |
| 评分分布 | 1～5 分评价量 | 无 | 全部评分 | 软删除 | 按 `rating` 分组 | `/distributions` |
| 报名成功率 | 选定期内成功报名占申请比例 | `activity_signup.created_at` | 分子 `APPROVED/PROMOTED/COMPLETED`；分母全部 | 软删除 | 成功数/报名总数×100 | `/quality` |
| 平均参与率 | 选定期发布活动的平均满员程度 | `activity.created_at` | 非 `CANCELLED` | `CANCELLED`、软删除 | 平均 `approved_count/max_participants×100` | `/quality` |
| 平均评分 | 选定期评价平均星级 | `activity_review.created_at` | 全部 | 软删除 | `AVG(rating)` | `/quality` |
| 候补数 | 选定期进入且仍在候补的记录 | `activity_waitlist.created_at` | `WAITING` | 其他状态、软删除 | `COUNT(*)` | `/quality` |
| 退出数 | 选定期创建后最终取消的报名 | `activity_signup.created_at` | `CANCELLED` | 其他状态、软删除 | `COUNT(*)` | `/quality` |
| 异常信用用户数 | 当前信用低于基础安全线的用户 | 无 | `credit_score < 80` | 软删除 | `COUNT(*)` | `/quality` |
| 热门活动排行 | 成功报名优先、当前候补次优先 | 报名 `created_at` | 成功报名状态；候补 `WAITING` | 软删除 | 两指标降序，最多 50 条 | `/popular-activities` |

支持 `TODAY`、`THIS_WEEK`（周一开始）、`THIS_MONTH`、`LAST_7_DAYS`、`LAST_30_DAYS`、`LAST_90_DAYS`、`THIS_YEAR`、`CUSTOM`。`CUSTOM` 必须同时传 `startDate/endDate`，开始不得晚于结束，跨度不得超过一年。

首页概览使用 `city-party:` 统一前缀缓存，TTL 5 分钟；Redis 读写异常直接回退数据库。其他分析接口不缓存。现有活动、报名、候补和评价关联索引可用于排行关联；趋势查询可能在大数据量时需要观察慢查询后再决定是否增加 `(deleted, created_at)` 索引，本阶段不做无证据的索引扩张。
