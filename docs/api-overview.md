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
- `GET /api/activities/{id}`
- `GET /api/activities/my`
- `GET /api/activities/{id}/signups`

## Signup

- `POST /api/activities/{activityId}/signup`
- `POST /api/activities/{activityId}/signup/cancel`
- `POST /api/signups/{signupId}/review`
- `POST /api/signups`（兼容接口，请求体传 `activityId`，等价于报名活动）
- `POST /api/signups/{signupId}/approve`（兼容接口，等价于审核通过）
- `POST /api/signups/{signupId}/reject`（兼容接口，等价于审核拒绝）
- `GET /api/signups/my`

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
- `GET /api/admin/signups`
- `GET /api/admin/credits`
- `GET /api/admin/reports`
