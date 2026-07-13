# Docker 开发环境

## 管理范围

`compose.yaml` 只管理：

- `city-party-mysql`
- `city-party-redis`

本阶段不容器化前端或后端。前端和后端仍在 Windows 本机运行。

## 端口

默认宿主机端口：

- MySQL：`127.0.0.1:13306`
- Redis：`127.0.0.1:16379`

使用 13306 和 16379 是为了避免占用本机常见的 3306 和 6379。如果你确认 3306 或 6379 可用，可以只修改本地 `.env`，不要改 `compose.yaml`：

```text
MYSQL_HOST_PORT=3306
REDIS_HOST_PORT=6379
```

## 首次启动

```powershell
Set-Location D:\last_one-form-group\city-party-platform
Copy-Item .env.example .env
notepad .env
docker compose config
docker compose up -d
docker compose ps
```

`.env` 被 Git 忽略，不要提交真实密码。

## 数据卷

Compose 使用项目专属卷：

- `city_party_mysql_data`
- `city_party_redis_data`

不要删除其他项目容器或数据卷。

## 健康检查

```powershell
docker compose ps
docker inspect --format '{{json .State.Health}}' city-party-mysql
docker inspect --format '{{json .State.Health}}' city-party-redis
docker exec city-party-redis redis-cli PING
```

Redis 应返回：

```text
PONG
```

## 初始化机制

空数据卷首次启动时，MySQL 镜像执行：

```text
database/schema.sql -> /docker-entrypoint-initdb.d/001-schema.sql
```

该脚本是当前完整 schema。空卷不要再叠加执行历史 migration。

## 停止和清理

停止容器但保留数据：

```powershell
docker compose stop
```

删除容器但保留数据卷：

```powershell
docker compose down
```

删除数据卷会丢失项目 Docker 数据，只有确认不需要数据且已备份后才手动执行：

```powershell
docker compose down -v
```

## 后端连接容器

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

## 私人备份恢复

私人数据库备份不得提交到仓库。备份恢复说明见：

```text
docs/private-backup-restore.md
```
