# 数据库字段说明

- `sys_user`：统一登录账号，`role_code` 为 OWNER、ADMIN、AGENT、USER。
- `agent`：代理业务资料，通过 `user_id` 关联登录账号。
- `biz_order`：四类业务公共字段、审核状态、来源、归属代理及软删除字段。
- `order_campus_network`：学号、身份证后六位和导出状态。
- `order_driving_school`：车型、班型和缴费数额。
- `order_renewal`：续费金额。
- `support_issue`：用户/代理问题和处理结果。
- `support_issue_image`：问题图片 URL。
- `business_config`：重复窗口和驾校默认价格。
- `operation_log`：关键管理操作。

所有表使用 InnoDB、`utf8mb4` 和 `datetime`。业务删除使用软删除字段，订单额外记录操作人和作废时间。
