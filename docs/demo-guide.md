# 演示数据和演示步骤

## 演示数据范围

脚本：

- `database/demo-data.sql`
- `database/demo-cleanup.sql`

统一标识：

- 用户：`cp_demo_`
- 活动、通知、聊天、举报内容：`[CITY_PARTY_DEMO]`

覆盖数据：

- 1 个管理员
- 5 个普通/发起人用户
- 5 个活动
- 多个活动分类
- 多个活动状态：`FINISHED`、`CANCELLED`、`FULL`、`SIGNING`、`UPCOMING`
- 报名成功、待审核、拒绝、退出、候补
- 评价
- 信用记录
- 通知
- 聊天消息
- 举报记录
- 多个日期，用于趋势图变化

## 导入演示数据

```powershell
Set-Location D:\last_one-form-group\city-party-platform
$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
cmd.exe /d /s /c 'docker exec -i city-party-mysql sh -c "MYSQL_PWD=\"$MYSQL_PASSWORD\" exec mysql --default-character-set=utf8mb4 -ucity_party city_party_platform" < database\demo-data.sql'
```

脚本会先删除旧的演示标识数据，再重新导入，因此可重复执行。

## 清理演示数据

```powershell
cmd.exe /d /s /c 'docker exec -i city-party-mysql sh -c "MYSQL_PWD=\"$MYSQL_PASSWORD\" exec mysql --default-character-set=utf8mb4 -ucity_party city_party_platform" < database\demo-cleanup.sql'
```

清理脚本只删除演示标识数据，不清理普通业务数据，不使用 `TRUNCATE`。

## 验证数量

```sql
SELECT COUNT(*) FROM user WHERE username LIKE 'cp_demo_%';
SELECT COUNT(*) FROM activity WHERE title LIKE '[CITY_PARTY_DEMO]%';
SELECT COUNT(*) FROM activity_signup
WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE '[CITY_PARTY_DEMO]%');
SELECT COUNT(*) FROM activity_waitlist
WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE '[CITY_PARTY_DEMO]%');
SELECT COUNT(*) FROM activity_review
WHERE activity_id IN (SELECT id FROM activity WHERE title LIKE '[CITY_PARTY_DEMO]%');
```

阶段 4 验收中的演示数据数量：

| 数据 | 数量 |
| --- | ---: |
| 用户 | 6 |
| 活动 | 5 |
| 报名 | 9 |
| 候补 | 1 |
| 评价 | 3 |
| 信用记录 | 3 |
| 通知 | 3 |
| 聊天消息 | 3 |

## 浏览器演示步骤

1. 启动 Docker MySQL / Redis。
2. 导入演示数据。
3. 启动后端。
4. 启动前端。
5. 打开 `http://127.0.0.1:5173`。
6. 使用演示管理员登录后台。
7. 访问 `/admin/dashboard`。
8. 访问 `/admin/analytics`。
9. 切换最近 90 天、本年度和自定义日期范围。
10. 使用普通演示用户登录用户端，查看活动列表、详情和个人中心。

## 安全注意

- 演示账号只用于本地开发。
- 演示密码不得用于公网环境。
- 截图不要展示密码、JWT、数据库凭据、高德 Key 或私人联系方式。
