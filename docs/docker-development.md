# 项目专用 MySQL 与 Redis Docker 开发环境

## 管理范围

`compose.yaml` 只管理：

- `city-party-mysql`：MySQL 8.0.33
- `city-party-redis`：Redis 7.2 Alpine

前端和后端继续在 Windows 本机运行。Compose 使用独立的 `city_party_mysql_data`、`city_party_redis_data` 卷，不操作其他项目容器或卷。

## 首次启动

先检查默认端口是否可用：

```powershell
Get-NetTCPConnection -LocalPort 13306,16379 -State Listen -ErrorAction SilentlyContinue
```

创建本地配置并替换密码占位值：

```powershell
Set-Location D:\last_one-form-group\city-party-platform
Copy-Item .env.example .env
notepad .env
docker compose config
docker compose up -d
docker compose ps
```

`.env` 已被 Git 忽略。不要将真实密码写入 `.env.example`、README、命令历史或截图。

如 `13306/16379` 被占用，只修改本地 `.env`：

```text
MYSQL_HOST_PORT=23306
REDIS_HOST_PORT=26379
```

当前开发机的 6379 可能已有本项目早期使用的 `campus-market-redis`。新 Compose 默认使用 16379 和独立卷，可以并存验证；确认旧 Redis 数据不再需要前，不要停止、删除或复用它的卷。若以后决定只保留新容器，可先备份并停止旧容器，再通过本地 `.env` 将 `REDIS_HOST_PORT` 改为 `6379`。

## 健康与日志

```powershell
docker compose ps
docker inspect --format '{{json .State.Health}}' city-party-mysql
docker inspect --format '{{json .State.Health}}' city-party-redis
docker compose logs mysql --tail 100
docker compose logs redis --tail 100
```

首次初始化失败时，先查看日志。只有确认项目卷是本次失败创建且不含需要保留的数据后，才考虑删除项目卷重新初始化；不要删除其他项目卷。

## 停止与清理

停止容器但保留数据：

```powershell
docker compose stop
```

删除容器但保留数据卷：

```powershell
docker compose down
```

`docker compose down -v` 会删除项目数据库和 Redis 数据，只能在确认不需要项目数据并完成仓库外备份后手动执行。本项目不会自动执行该命令。
