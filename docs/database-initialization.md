# 数据库初始化说明

## 空数据卷初始化

MySQL 官方镜像仅在数据目录为空时执行 `/docker-entrypoint-initdb.d`。本项目只挂载一个初始化入口：

```text
database/schema.sql -> /docker-entrypoint-initdb.d/001-schema.sql
```

`database/schema.sql` 是当前完整基线，已经包含 Stage 2.1～2.7 所需表、聊天字段、候补结构、评价与信用结构、通知索引、报名唯一约束及看板所依赖字段。

空卷首次启动时不要同时执行 `stage2.*-migration.sql`。历史 migration 只用于升级旧数据库；将完整 schema 和 migration 叠加可能产生重复字段或索引冲突。

## 初始化验证

```powershell
docker compose up -d
docker compose ps
docker exec city-party-mysql sh -c 'mysql -ucity_party -p"$MYSQL_PASSWORD" city_party_platform -e "SHOW TABLES;"'
docker exec city-party-mysql sh -c 'mysql -ucity_party -p"$MYSQL_PASSWORD" city_party_platform -e "SHOW INDEX FROM activity_signup;"'
```

密码由容器内环境变量读取，不要把真实值直接拼进命令参数或文档。

应存在 18 张业务表，并且 `activity_signup` 应包含 `uk_signup_activity_user(activity_id,user_id)`。

## 旧库升级

旧库不能通过重新执行 `schema.sql` 升级，因为完整 schema 包含重建表逻辑。旧库应先在仓库外备份，再按现有 Stage migration 顺序和实际数据库状态逐项核对执行。无法确认的结构不得猜测修改。
