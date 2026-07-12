# 校园业务管理系统后端

Java 17、Spring Boot 3、Spring Security、JWT、MyBatis-Plus、MySQL 8 和 EasyExcel。

## 初始化数据库

```bash
mysql -uroot -p < ../sql/schema.sql
mysql -uroot -p < ../sql/data.sql
```

## 配置与启动

```bash
export DB_USERNAME=root
export DB_PASSWORD=你的数据库密码
export JWT_SECRET=至少32字节的随机密钥
mvn spring-boot:run
```

服务地址：`http://localhost:8080`。

管理端切换真实接口：

```text
VITE_API_BASE_URL=http://localhost:8080/api
VITE_USE_MOCK=false
```

## 初始化账号

`sql/data.sql` 会创建本地演示账号，但仓库不记录其可用明文密码。首次启动后请通过安全的人工流程为本地账号设置密码；生产环境不得沿用演示账号凭据。

## 当前接口

- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `GET /api/dashboard/cards`
- `GET /api/dashboard/trend`
- `GET /api/dashboard/agent-ranking`
- `GET /api/dashboard/todos`
- `GET /api/orders`
- `GET /api/orders/{id}`
- `POST /api/orders`
- `PUT /api/orders/{id}`
- `POST /api/orders/{id}/confirm`
- `DELETE /api/orders/{id}`
- `POST /api/orders/{id}/restore`
- `GET /api/orders/campus-network/export`

订单列表由数据库按 `created_at DESC, id DESC` 排序。校园网导出接口只接受已确认、未作废的订单；待确认订单即使显示为“未导出”也不能导出。
