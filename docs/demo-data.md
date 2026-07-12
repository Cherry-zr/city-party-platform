# 演示数据说明

## 标识与覆盖范围

演示账号统一使用 `cp_demo_` 前缀；演示活动、内容和通知统一使用 `[CITY_PARTY_DEMO]` 标识。脚本覆盖：

- 管理员、普通用户和两名活动发起人
- 户外、观影、桌游、学习、探店分类
- `FINISHED/CANCELLED/FULL/SIGNING/UPCOMING` 活动状态
- 报名成功、完成、待审核、拒绝、退出和候补
- 评价、信用变化、异常信用用户、通知、聊天和举报
- 最近 75 天内多个日期，用于趋势与分布图

演示密码采用项目当前 PBKDF2 格式。演示账号仅用于本地开发，不是真实账号；密码为 `Demo@2026`，公开环境必须禁用或删除这些账号。

## 导入

PowerShell 通过标准输入导入，不把密码写入命令：

```powershell
$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
Get-Content -Raw -Encoding UTF8 database\demo-data.sql | docker exec -i city-party-mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --default-character-set=utf8mb4 -ucity_party city_party_platform'
```

密码由容器内的项目环境变量读取，不会出现在命令文本或标准输出中。脚本会先删除旧的统一演示标识数据，再重新生成，因此可重复运行，日期趋势会以本次导入时间为基准刷新。

## 单独清理

```powershell
$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
Get-Content -Raw -Encoding UTF8 database\demo-cleanup.sql | docker exec -i city-party-mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --default-character-set=utf8mb4 -ucity_party city_party_platform'
```

清理脚本只匹配 `cp_demo_%` 用户及 `[CITY_PARTY_DEMO]%` 关联数据，不删除普通业务账号或普通活动。执行前仍应核对脚本和目标数据库；不要对业务表执行 `TRUNCATE`。

## 验证

导入后建议确认演示数据数量、登录和看板：

```sql
SELECT COUNT(*) FROM user WHERE username LIKE 'cp_demo_%';
SELECT status, COUNT(*) FROM activity WHERE title LIKE '[CITY_PARTY_DEMO]%' GROUP BY status;
SELECT status, COUNT(*) FROM activity_signup
WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE '[CITY_PARTY_DEMO]%')
GROUP BY status;
```
