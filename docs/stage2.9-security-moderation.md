# Stage 2.9 安全与活动审核升级

## 数据库升级

全新数据库只需要使用最新的 database/schema.sql。已有的 \`city_party_platform\` 数据库必须先备份，再连接 MySQL 客户端执行：

    SOURCE D:/last_one-form-group/city-party-platform/database/stage2.9-migration.sql;

Migration 只增加字段和索引，不删除历史数据。由于历史活动在旧版本中已经公开，迁移会将其回填为 APPROVED，并使用 created_at 作为兼容的 audit_time。新活动默认保存为 PENDING。

审核状态与活动生命周期状态相互独立：

- audit_status：PENDING、APPROVED、REJECTED
- status：SIGNING、FULL、UPCOMING、ONGOING、FINISHED、CANCELLED

## 注册安全配置

后端启动前必须设置 JWT_SECRET。不要把真实值写入仓库：

    $env:JWT_SECRET="请替换为至少 32 位的本机随机字符串"

可选限流环境变量：

- REGISTRATION_MAX_ATTEMPTS：单 IP 注册窗口最大次数，默认 5
- REGISTRATION_WINDOW_SECONDS：注册窗口，默认 600
- REGISTRATION_CHALLENGE_MAX_ATTEMPTS：challenge 获取上限，默认 20
- REGISTRATION_CHALLENGE_WINDOW_SECONDS：challenge 获取窗口，默认 300
- REGISTRATION_VERIFY_MAX_ATTEMPTS：滑块验证上限，默认 30
- REGISTRATION_VERIFY_WINDOW_SECONDS：滑块验证窗口，默认 300

注册滑块 challenge 默认有效 120 秒；验证成功后的注册令牌默认有效 300 秒。challenge 和注册令牌成功使用后都会从 Redis 原子消费，不能重放。

## 反向代理

默认只使用 TCP 连接的远端地址，不信任客户端自行发送的 X-Forwarded-For。只有应用明确部署在可信反向代理之后，才配置代理 IP：

    $env:TRUSTED_PROXIES="127.0.0.1,10.0.0.10"

当前配置接受精确 IP 列表，不接受 CIDR。代理本身还应覆盖外部传入的转发头，避免将伪造头继续传递给应用。

## 人工验收

1. 普通用户发布活动，确认“我的活动”显示“平台待审核”，首页和地图搜索不到该活动。
2. 管理员打开“活动管理”，按审核状态筛选，执行通过或拒绝。
3. 通过后确认活动进入首页，且最新通过的活动排在前面。
4. 拒绝后确认发布者能看到原因；修改原活动后状态回到 PENDING，拒绝原因和审核元数据清空。
5. 首页切换分类，确认普通列表和“为你推荐”同时跟随分类。
6. 地图拒绝定位权限，确认页面不回退北京；再手动选城或重新定位。
7. 发布页输入地点关键词，选择高德联想项；改变活动城市后，确认旧地址和坐标被清空。

## 验证命令

    Set-Location backend
    mvn test

    Set-Location ..\frontend
    npm run build
    npm run test:e2e
