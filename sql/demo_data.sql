USE campus_business;

DROP PROCEDURE IF EXISTS seed_campus_demo;

DELIMITER $$
CREATE PROCEDURE seed_campus_demo () BEGIN DECLARE i INT DEFAULT 1;

DECLARE uid BIGINT;

DECLARE aid BIGINT;

DECLARE oid BIGINT;

DECLARE business VARCHAR(30);

DECLARE audit VARCHAR(20);

DECLARE source VARCHAR(20);

DELETE FROM support_issue_image
WHERE
  issue_id IN (
    SELECT
      id
    FROM
      support_issue
    WHERE
      issue_no LIKE 'DEMOISSUE%'
  );

DELETE FROM support_issue
WHERE
  issue_no LIKE 'DEMOISSUE%';

DELETE FROM order_campus_network
WHERE
  order_id IN (
    SELECT
      id
    FROM
      biz_order
    WHERE
      order_no LIKE 'DEMO%'
  );

DELETE FROM order_driving_school
WHERE
  order_id IN (
    SELECT
      id
    FROM
      biz_order
    WHERE
      order_no LIKE 'DEMO%'
  );

DELETE FROM order_renewal
WHERE
  order_id IN (
    SELECT
      id
    FROM
      biz_order
    WHERE
      order_no LIKE 'DEMO%'
  );

DELETE FROM biz_order
WHERE
  order_no LIKE 'DEMO%';

DELETE FROM agent
WHERE
  agent_no LIKE 'DEMOAG%';

DELETE FROM sys_user
WHERE
  username LIKE 'demo_agent%';

WHILE i <= 8 DO
INSERT INTO
  sys_user (
    username,
    phone,
    password,
    real_name,
    role_code,
    status
  )
SELECT
  CONCAT('demo_agent', LPAD(i, 2, '0')),
  CONCAT('1391000', LPAD(i, 4, '0')),
  password,
  CONCAT('演示代理', LPAD(i, 2, '0')),
  'AGENT',
  1
FROM
  sys_user
WHERE
  username = 'agent01';

SET
  uid = LAST_INSERT_ID();

INSERT INTO
  agent (agent_no, user_id, name, phone, level, status)
VALUES
  (
    CONCAT('DEMOAG', LPAD(i, 4, '0')),
    uid,
    CONCAT('演示代理', LPAD(i, 2, '0')),
    CONCAT('1391000', LPAD(i, 4, '0')),
    ELT(
      1 + MOD(i -1, 3),
      'NORMAL',
      'ADVANCED',
      'CAMPUS_LEADER'
    ),
    1
  );

SET
  i = i + 1;

END
WHILE;

SET
  i = 1;

WHILE i <= 160 DO
SET
  business = ELT(
    1 + MOD(i -1, 4),
    'CAMPUS_CARD',
    'CAMPUS_NETWORK',
    'DRIVING_SCHOOL',
    'RENEWAL'
  );

SET
  audit = IF(MOD(i, 7) = 0, 'PENDING', 'CONFIRMED');

SET
  source = ELT(1 + MOD(i -1, 3), 'ONLINE', 'AGENT', 'STORE');

SELECT
  id INTO aid
FROM
  agent
WHERE
  agent_no = CONCAT('DEMOAG', LPAD(1 + MOD(i -1, 8), 4, '0'));

INSERT INTO
  biz_order (
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
    CONCAT(
      'DEMO',
      DATE_FORMAT(
        DATE_SUB(NOW(), INTERVAL MOD(i, 60) DAY),
        '%Y%m%d'
      ),
      LPAD(i, 6, '0')
    ),
    business,
    CONCAT('演示学生', LPAD(i, 3, '0')),
    CONCAT('137', LPAD(20000000 + i, 8, '0')),
    IF(
      business = 'DRIVING_SCHOOL',
      NULL,
      CONCAT('185', LPAD(30000000 + i, 8, '0'))
    ),
    source,
    aid,
    2,
    audit,
    IF(MOD(i, 9) = 0, '演示数据：已电话确认', NULL),
    DATE_ADD(
      DATE_SUB(NOW(), INTERVAL MOD(i, 60) DAY),
      INTERVAL MOD(i, 18) HOUR
    )
  );

SET
  oid = LAST_INSERT_ID();

IF business = 'CAMPUS_NETWORK' THEN
INSERT INTO
  order_campus_network (
    order_id,
    student_no,
    id_card_last_six,
    export_status
  )
VALUES
  (
    oid,
    CONCAT('2026', LPAD(i, 8, '0')),
    LPAD(310000 + MOD(i, 9999), 6, '0'),
    IF(MOD(i, 5) = 0, 'EXPORTED', 'NOT_EXPORTED')
  );

ELSEIF business = 'DRIVING_SCHOOL' THEN
INSERT INTO
  order_driving_school (
    order_id,
    license_type,
    class_type,
    payment_amount
  )
VALUES
  (
    oid,
    IF(MOD(i, 2) = 0, 'C1', 'C2'),
    IF(MOD(i, 3) = 0, 'FULL', 'NORMAL'),
    2800 + MOD(i, 4) * 300
  );

ELSEIF business = 'RENEWAL' THEN
INSERT INTO
  order_renewal (order_id, renewal_amount)
VALUES
  (oid, 50 + MOD(i, 6) * 20);

END IF;

SET
  i = i + 1;

END
WHILE;

SET
  i = 1;

WHILE i <= 30 DO
INSERT INTO
  support_issue (
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
    CONCAT('DEMOISSUE', LPAD(i, 6, '0')),
    3,
    'AGENT',
    '张三',
    '13800000003',
    ELT(
      1 + MOD(i -1, 6),
      'CAMPUS_CARD',
      'CAMPUS_NETWORK',
      'DRIVING_SCHOOL',
      'RENEWAL',
      'ACCOUNT',
      'OTHER'
    ),
    CONCAT('这是用于功能测试的演示问题 ', LPAD(i, 2, '0')),
    IF(
      MOD(i, 4) = 0,
      NULL,
      CONCAT('185', LPAD(40000000 + i, 8, '0'))
    ),
    ELT(
      1 + MOD(i -1, 4),
      'PENDING',
      'PROCESSING',
      'RESOLVED',
      'CLOSED'
    ),
    DATE_SUB(NOW(), INTERVAL i HOUR)
  );

SET
  i = i + 1;

END
WHILE;

END $$ DELIMITER;

CALL seed_campus_demo ();

DROP PROCEDURE seed_campus_demo;
