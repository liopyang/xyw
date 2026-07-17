USE campus_business;

INSERT IGNORE INTO
  sys_user (
    id,
    username,
    phone,
    password,
    real_name,
    role_code,
    status
  )
VALUES
  (
    1,
    'admin',
    '13800000001',
    '$2y$10$TXviIkaICWuZHxyhF7wD4.g1bZsq1DJFGqw.zHhTv60i80Wdd.73W',
    '系统老板',
    'OWNER',
    1
  ),
  (
    2,
    'staff',
    '13800000002',
    '$2y$10$wfliwI1ul3.c2LjhSsjbO.Qj/UTCoJDodAEq3QgULSinzBeSTAg5m',
    '门店管理员',
    'ADMIN',
    1
  ),
  (
    3,
    'agent01',
    '13800000003',
    '$2y$10$U7YZ0swNEFgUqhi.gScLCu.b3QwxdjDSkOlmsJjQ0iPRgXFQ275Wm',
    '张三',
    'AGENT',
    1
  ),
  (
    4,
    'user01',
    '13800000004',
    '$2y$10$Q7tGX17Te3fzGnV0RQbV8.QemhJ9b0ESaUUDp6cvetL3moUWAycNa',
    '普通用户',
    'USER',
    1
  );

INSERT IGNORE INTO
  agent (id, agent_no, user_id, name, phone, level, status)
VALUES
  (
    1,
    'AG2026070001',
    3,
    '张三',
    '13800000003',
    'NORMAL',
    1
  );

INSERT INTO
  business_config (config_key, config_value, description, updated_by)
VALUES
  ('duplicateWindowDays', '30', '重复订单限制天数', 1),
  ('drivingC1NormalPrice', '2800', 'C1普通班默认价格', 1),
  ('drivingC1FullPrice', '3600', 'C1全包班默认价格', 1),
  ('drivingC2NormalPrice', '3000', 'C2普通班默认价格', 1),
  ('drivingC2FullPrice', '3900', 'C2全包班默认价格', 1)
ON DUPLICATE KEY UPDATE
  description = VALUES(description);

INSERT IGNORE INTO
  biz_order (
    id,
    order_no,
    business_type,
    customer_name,
    contact_phone,
    business_number,
    source_channel,
    agent_id,
    created_by,
    audit_status,
    remark,
    created_at
  )
VALUES
  (
    1,
    'CARD20260711000001',
    'CAMPUS_CARD',
    '李欣怡',
    '13810000001',
    '18650000001',
    'AGENT',
    1,
    3,
    'CONFIRMED',
    '学生已确认',
    DATE_SUB(NOW(), INTERVAL 2 HOUR)
  ),
  (
    2,
    'NET20260711000002',
    'CAMPUS_NETWORK',
    '王浩然',
    '13810000002',
    '18650000002',
    'ONLINE',
    1,
    3,
    'CONFIRMED',
    '待导出',
    DATE_SUB(NOW(), INTERVAL 3 HOUR)
  ),
  (
    3,
    'DRIVE20260710000003',
    'DRIVING_SCHOOL',
    '陈雨桐',
    '13810000003',
    NULL,
    'AGENT',
    1,
    3,
    'PENDING',
    'C2全包班',
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  ),
  (
    4,
    'RENEW20260710000004',
    'RENEWAL',
    '刘子轩',
    '13810000004',
    '18650000004',
    'STORE',
    NULL,
    2,
    'CONFIRMED',
    '门店续费',
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  ),
  (
    5,
    'NET20260709000005',
    'CAMPUS_NETWORK',
    '周思涵',
    '13810000005',
    '18650000005',
    'AGENT',
    1,
    3,
    'CONFIRMED',
    NULL,
    DATE_SUB(NOW(), INTERVAL 2 DAY)
  ),
  (
    6,
    'CARD20260708000006',
    'CAMPUS_CARD',
    '赵文博',
    '13810000006',
    '18650000006',
    'STORE',
    NULL,
    2,
    'CONFIRMED',
    NULL,
    DATE_SUB(NOW(), INTERVAL 3 DAY)
  );

INSERT IGNORE INTO
  order_campus_network (
    id,
    order_id,
    student_no,
    id_card_last_six,
    export_status
  )
VALUES
  (1, 2, '2026070002', '320102', 'NOT_EXPORTED'),
  (2, 5, '2026070005', '320105', 'EXPORTED');

INSERT IGNORE INTO
  order_driving_school (
    id,
    order_id,
    license_type,
    class_type,
    payment_amount
  )
VALUES
  (1, 3, 'C2', 'FULL', 3900.00);

INSERT IGNORE INTO
  order_renewal (id, order_id, renewal_amount)
VALUES
  (1, 4, 100.00);

INSERT IGNORE INTO
  support_issue (
    id,
    issue_no,
    submitter_id,
    submitter_type,
    submitter_name,
    contact_phone,
    issue_type,
    description,
    business_number,
    status,
    submitted_at
  )
VALUES
  (
    1,
    'ISSUE20260711000001',
    3,
    'AGENT',
    '张三',
    '13800000003',
    'CAMPUS_NETWORK',
    '校园网订单办理后暂时无法使用',
    '18650000002',
    'PENDING',
    DATE_SUB(NOW(), INTERVAL 1 HOUR)
  ),
  (
    2,
    'ISSUE20260710000002',
    4,
    'USER',
    '普通用户',
    '13800000004',
    'ACCOUNT',
    '忘记登录密码，需要协助重置',
    NULL,
    'PROCESSING',
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  );
