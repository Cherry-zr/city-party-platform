# 个性化活动推荐算法

## 定位与技术取舍

Stage 2.8 实现的是基于内容和业务特征的多维加权排序，不是机器学习或大规模推荐系统。当前项目没有足够的真实点击、浏览、报名转化和长期留存数据，使用协同过滤、深度学习或独立推荐微服务会增加训练、部署和解释成本，却无法得到可靠模型。因此本阶段选择一周内可完成、可测试、可解释的规则算法。

推荐模块仍位于现有 Spring Boot 单体应用中：`RecommendationService` 负责召回、批量取数、排序、Top N 转换和缓存，`RecommendationScorer` 只负责纯特征计算。

## 候选活动召回

每次最多召回 100 条原始 `Activity`。候选必须同时满足：

- `deleted = 0`。
- `status IN ('SIGNING', 'FULL')`，满员活动仍可进入候补。
- `start_time > now`。
- `signup_deadline >= now`。
- 发起人不是当前用户。
- 当前用户不存在 `PENDING`、`APPROVED`、`WAITING`、`PROMOTED` 或 `COMPLETED` 报名。

时间统一来自注入的 `Clock`。服务会对 Mapper 返回结果再做一次同规则防御性过滤，确保测试和异常数据下也不会输出不合格活动。

## 兴趣特征

用户兴趣集合：

```text
U = user_interest -> interest_tag.name
```

活动特征集合：

```text
A = activity.tags + activity.category
```

两类集合都先执行 `trim`、忽略空字符串和去重。兴趣理由只展示真实交集中的标签。

设：

```text
intersection = |U ∩ A|
union        = |U ∪ A|
Jaccard      = intersection / union
Coverage     = intersection / |A|

InterestScore = 100 * (0.7 * Jaccard + 0.3 * Coverage)
```

无交集时得分为 0；完全匹配时为 100。用户没有兴趣数据时，该特征为 `unavailable`，接口中的 `scoreDetail.interest` 返回 `null`，而不是以 0 分继续占用 35% 权重。

## 距离特征

只有请求和活动都同时具备经纬度时才启用距离特征。Haversine 使用地球平均半径：

```text
R = 6371.0088 km
```

得到 `distanceKm` 后执行指数衰减：

```text
DistanceScore = 100 * exp(-distanceKm / 5.0)
```

接口距离保留两位小数。缺少任一坐标时，`distanceKm` 和 `scoreDetail.distance` 均为 `null`，并从最终权重分母中移除。

## 热度特征

```text
SignupScore = maxParticipants > 0
  ? 100 * min(approvedCount / maxParticipants, 1)
  : 0

FavoriteScore = 100 * (1 - exp(-favoriteCount / 5.0))

HotnessScore = 0.7 * SignupScore + 0.3 * FavoriteScore
```

热度只使用已有报名人数、容量和收藏数，不新增浏览量或行为埋点。

## 时间特征

`daysUntilStart` 使用当前时间到活动开始时间的实际时长换算：

```text
daysUntilStart <= 3:
  TimeScore = 100

daysUntilStart > 3:
  TimeScore = 100 * exp(-(daysUntilStart - 3) / 14.0)
```

所有结果限制在 0 到 100。召回阶段已经排除已开始活动；纯算法仍对空时间或非未来时间安全返回 0。

## 发起人信用特征

```text
CreditScore = clamp((creditScore - 60) / 50.0 * 100, 0, 100)
```

例如 60、80、100、110 分分别映射为 0、40、80、100。发起人批量查询不到或已删除时，信用特征为 `unavailable`，不会使整个推荐请求失败。

## 最终加权排序

基础权重：

```text
Interest = 0.35
Distance = 0.25
Hotness  = 0.15
Time     = 0.15
Credit   = 0.10
```

缺失特征采用动态归一化：

```text
FinalScore = Σ(weight_i * score_i)
             / Σ(weight_i for available features)
```

缺失维度既不进入分子，也不进入分母。所有特征和最终分数都会限制在 0 到 100，并在接口中保留两位小数，避免 `NaN`、`Infinity`、负数或超过 100。

最终稳定排序规则：

1. `recommendationScore DESC`。
2. `startTime ASC`。
3. `activityId ASC`。

## 冷启动与可解释理由

新用户没有兴趣、请求没有位置时，仍使用热度、时间和发起人信用进行排序。推荐不会因为缺少画像或定位而消失。

每条活动最多返回 3 个理由，可能包括真实匹配兴趣、实际计算距离、报名热度、开始时间和发起人信用。理由不使用“AI 认为”“大数据推荐”等无法由当前计算证明的文案。

## Redis 缓存与故障回退

推荐结果以 JSON 写入现有 Redis，TTL 为 5 分钟。Key 至少区分：

```text
userId + 四舍五入到 3 位小数的位置 + limit + 兴趣集合 fingerprint
```

兴趣 fingerprint 使用户修改兴趣后自然切换到新缓存；位置取 3 位小数避免轻微定位漂移制造大量 Key。Redis 读取、反序列化或写入失败时记录 `warn` 并回退实时计算，推荐接口不因 Redis 不可用而返回 500。

## 查询量与复杂度

设候选数为 `N`，当前 `N <= 100`：

- 特征计算在标签数量较少时接近 `O(N)`。
- 排序为 `O(N log N)`。
- 发起人使用 `selectBatchIds` 批量读取。
- 先对原始 `Activity` 评分和排序，再只对 Top N 调用 `ActivityService.toVO()`。

因此不会对 100 个候选逐个查询发起人，也不会在排序前对所有候选执行包含候补、资料、收藏和报名查询的 `toVO()`。

## 当前局限与演进方向

当前权重来自业务规则，不会自动学习用户长期偏好；兴趣依赖显式标签，热门新活动和标签质量会影响结果；5 分钟缓存也只提供短期最终一致性。

当真实行为数据和数据量足够后，可在保持当前可解释召回与安全降级的前提下，逐步引入离线权重评估、点击/报名转化指标、更多候选召回通道和可审计的实验流程。是否使用协同过滤或学习排序应由真实数据量、效果指标和运维成本决定，而不是仅为增加技术名词。
