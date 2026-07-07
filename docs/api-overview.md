# API Overview

所有业务接口返回统一结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

除 `/api/auth/**` 外，接口默认需要请求头：

```text
Authorization: Bearer <token>
```

## Auth

- `GET /api/auth/captcha`
- `POST /api/auth/register`
- `POST /api/auth/login`

## User

- `GET /api/user/me`
- `PUT /api/user/profile`
- `GET /api/users/{id}/public-profile`

## File

- `POST /api/file/upload/avatar`
- `POST /api/file/upload/activity-cover`

## Activity

- `POST /api/activities`
- `GET /api/activities`
- `GET /api/activities/nearby`
- `GET /api/activities/{id}`
- `GET /api/activities/my`
- `GET /api/activities/{id}/signups`
- `POST /api/activities/{id}/waitlist`
- `POST /api/activities/{id}/waitlist/cancel`
- `GET /api/activities/{id}/waitlist`

### Nearby Activity

`GET /api/activities/nearby`

常用查询参数：

- `longitude`、`latitude`：当前位置经纬度，可为空。
- `distanceKm`：距离范围，支持 1、3、5、10 等公里数，默认 5。
- `category`、`tag`、`city`：可选筛选条件。
- `current`、`size`：分页参数。

当经纬度存在时，后端使用 Haversine 公式计算 `distanceKm` 并按距离升序返回；经纬度为空时，按 `city` 降级查询。

### Waitlist

- `POST /api/activities/{id}/waitlist`：活动满员时加入候补队列。
- `POST /api/activities/{id}/waitlist/cancel`：取消候补。
- `GET /api/activities/{id}/waitlist`：发起人或管理员查看候补列表。

候补状态：

- `WAITING`：候补中。
- `PROMOTED`：已自动转正。
- `CANCELLED`：已取消。

Redis Key：`activity:waitlist:{activityId}`。Redis List 维护顺序，MySQL `activity_waitlist` 保存持久记录。

## Signup

- `POST /api/activities/{activityId}/signup`
- `POST /api/activities/{activityId}/signup/cancel`
- `POST /api/signups/{signupId}/review`
- `POST /api/signups`（兼容接口，请求体传 `activityId`，等价于报名活动）
- `POST /api/signups/{signupId}/approve`（兼容接口，等价于审核通过）
- `POST /api/signups/{signupId}/reject`（兼容接口，等价于审核拒绝）
- `GET /api/signups/my`

报名状态补充：

- `WAITING`：候补中。
- `PROMOTED` 或 `APPROVED`：报名成功。
- 用户退出 `APPROVED` 报名后，系统会自动尝试转正第一位候补用户。

## Notice

- `GET /api/notices/my`
- `PUT /api/notices/{id}/read`
- `GET /api/notices/unread-count`

候补转正后会生成系统通知：

```text
type = WAITLIST_PROMOTED
title = 候补转正通知
```

本阶段只保存 MySQL 通知记录，不做 WebSocket 实时推送。

## Favorite

- `POST /api/activities/{activityId}/favorite`
- `DELETE /api/activities/{activityId}/favorite`
- `POST /api/favorites/{activityId}`（兼容接口，等价于收藏活动）
- `GET /api/favorites/my`

## API Not Found

未匹配的 `/api/**` 请求返回统一结构，不再进入服务器内部错误分支：

```json
{
  "code": 404,
  "message": "接口不存在",
  "data": null
}
```

## Admin

管理员接口要求登录用户角色为 `ADMIN`。

- `GET /api/admin/dashboard`
- `GET /api/admin/users`
- `GET /api/admin/activities`
- `GET /api/admin/signups`，支持 `status` 筛选，例如 `WAITING`、`PROMOTED`、`APPROVED`
- `GET /api/admin/credits`
- `GET /api/admin/reports`

## AMap Config

前端读取以下 Vite 环境变量：

```text
VITE_AMAP_KEY=你的高德地图 JS API Key
VITE_AMAP_SECURITY_CODE=你的高德 JS API 安全密钥
```

真实 Key 只应写入 `frontend/.env.development` 或本地私有环境文件，不要提交到 Git。
