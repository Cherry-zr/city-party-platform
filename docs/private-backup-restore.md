# 私人数据库备份与恢复

## 安全边界

- 私人备份必须保存在仓库外，或明确被忽略的本地目录。
- 禁止提交 `.sql`、压缩包、真实手机号、邮箱、Token 或密码。
- 恢复前先确认目标是项目专用数据库，禁止覆盖来源不明的数据。
- 本文不包含任何真实备份路径、账号或密码。

## 创建仓库外备份

以下示例会交互式询问密码：

```powershell
$backupRoot = Join-Path $env:USERPROFILE "city-party-private-backups"
New-Item -ItemType Directory -Force $backupRoot | Out-Null
$backupFile = Join-Path $backupRoot ("city_party_platform_" + (Get-Date -Format "yyyyMMdd_HHmmss") + ".sql")
mysqldump -h 127.0.0.1 -P 13306 -u city_party -p --single-transaction --routines --triggers city_party_platform --result-file=$backupFile
```

不要把 `$backupFile` 移入仓库。

## 恢复到项目专用容器

1. 停止本机后端，避免恢复期间继续写入。
2. 确认备份来源和目标数据库。
3. 先为目标当前状态创建仓库外备份。
4. 交互式恢复：

```powershell
mysql -h 127.0.0.1 -P 13306 -u city_party -p city_party_platform
```

进入 MySQL 后执行：

```sql
SOURCE C:/Users/你的用户目录/city-party-private-backups/已确认的备份.sql;
```

恢复完成后验证表数量、关键索引、管理员状态和后端启动日志。不要将私人备份复制进容器镜像或 Git。
