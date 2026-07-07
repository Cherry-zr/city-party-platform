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
- `GET /api/signups/my`

## Favorite

- `POST /api/activities/{activityId}/favorite`
- `DELETE /api/activities/{activityId}/favorite`
- `GET /api/favorites/my`

## Admin

管理员接口要求登录用户角色为 `ADMIN`。

- `GET /api/admin/dashboard`
- `GET /api/admin/users`
- `GET /api/admin/activities`
- `GET /api/admin/signups`
- `GET /api/admin/credits`
- `GET /api/admin/reports`
