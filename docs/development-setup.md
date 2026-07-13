# 本地开发环境说明

## 推荐环境

- Windows 10/11
- PowerShell
- JDK 17
- Maven 3.8+
- Node.js 20
- npm
- Docker Desktop

## 克隆后检查

```powershell
Set-Location D:\last_one-form-group\city-party-platform
git status --short
git branch --show-current
```

## 启动 MySQL 和 Redis

```powershell
Copy-Item .env.example .env
notepad .env
docker compose config
docker compose up -d
docker compose ps
```

默认端口：

- MySQL：`127.0.0.1:13306`
- Redis：`127.0.0.1:16379`

## 导入演示数据

```powershell
$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
cmd.exe /d /s /c 'docker exec -i city-party-mysql sh -c "MYSQL_PWD=\"$MYSQL_PASSWORD\" exec mysql --default-character-set=utf8mb4 -ucity_party city_party_platform" < database\demo-data.sql'
```

## 启动后端

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

后端地址：

```text
http://127.0.0.1:8080
```

## 启动前端

```powershell
Set-Location D:\last_one-form-group\city-party-platform\frontend
npm ci
npm run dev
```

前端地址：

```text
http://127.0.0.1:5173
```

## 高德地图配置

如果要使用地图页面和发布页选点功能：

```powershell
Set-Location D:\last_one-form-group\city-party-platform\frontend
Copy-Item .env.example .env.development
notepad .env.development
```

填写：

```text
VITE_AMAP_KEY=your_amap_js_api_key
VITE_AMAP_SECURITY_CODE=your_amap_security_js_code
```

不要提交真实 Key。

## 常用测试

```powershell
Set-Location D:\last_one-form-group\city-party-platform\backend
mvn test
mvn clean package -DskipTests
```

```powershell
Set-Location D:\last_one-form-group\city-party-platform\frontend
npm run build
npm run test:e2e
```

## 常见问题

### Docker Compose 缺少密码

如果 `docker compose config` 提示 `MYSQL_ROOT_PASSWORD` 或 `MYSQL_PASSWORD` 缺失，说明根目录 `.env` 尚未创建或占位值未填写。

### 端口被占用

修改根目录 `.env`：

```text
MYSQL_HOST_PORT=23306
REDIS_HOST_PORT=26379
```

然后重新执行：

```powershell
docker compose up -d
```

### npm audit 提示漏洞

当前存在 `echarts`、`vite`、`esbuild` 相关审计提示。不要直接执行：

```powershell
npm audit fix --force
```

因为它会触发大版本升级，应后续单独评估兼容性。
