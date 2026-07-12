# 接口说明

接口前缀为 `/api`。除登录和只返回 `UP/DOWN` 的 `GET /actuator/health` 外，均使用 `Authorization: Bearer <token>`。

## 管理端

- 认证：`POST /auth/login`、`GET /auth/me`、`POST /auth/logout`
- 看板：`GET /dashboard/cards`、`GET /dashboard/trend`、`GET /dashboard/agent-ranking`、`GET /dashboard/todos`
- 订单：`GET/POST /orders`、`GET/PUT /orders/{id}`、`POST /orders/{id}/confirm`、`DELETE /orders/{id}`、`POST /orders/{id}/restore`
- 导出：`GET /orders/campus-network/export`，可传 `orderId` 单条导出
- 代理：`GET/POST /agents`、`GET/PUT /agents/{id}`、`PUT /agents/{id}/status`
- 问题：`GET/POST /issues`、`GET /issues/{id}`、`PUT /issues/{id}/status`、`POST /issues/{id}/images`
- 配置：`GET /configs`、`PUT /configs/{key}`
- 管理员：`GET/POST /system/users`、`PUT /system/users/{id}`、`PUT /system/users/{id}/status`
- 日志：`GET /operation-logs`

## 小程序

- 首页：`GET /mini/home`
- 代理订单：`GET/POST /mini/orders`
- 自己的问题：`GET/POST /mini/issues`

分页返回 `records`、`total`、`page`、`pageSize`。业务错误使用 HTTP 400，并返回明确的 `message`。

## 关键规则

- 所有统计只包含已确认、未作废订单。
- 订单列表固定按 `created_at DESC, id DESC` 排序。
- 校园网导出仅包含已确认、未作废订单；导出成功才更新为 `EXPORTED`。
- 代理订单按登录代理的 `agent_id` 强制隔离，不能依赖前端传参。
- 普通用户和代理问题按当前登录用户 ID 隔离。
