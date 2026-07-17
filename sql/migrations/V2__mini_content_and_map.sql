-- 仅用于测试环境。执行前请备份；不要直接在生产库运行。
USE campus_business;

CREATE TABLE mini_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sys_user_id BIGINT NULL,
  openid VARCHAR(64) NOT NULL,
  unionid VARCHAR(64),
  nickname VARCHAR(80),
  avatar_url VARCHAR(500),
  phone VARCHAR(20),
  role_code VARCHAR(20) NOT NULL DEFAULT 'USER',
  status TINYINT NOT NULL DEFAULT 1,
  last_login_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_mini_user_openid (openid),
  UNIQUE KEY uk_mini_user_sys_user (sys_user_id),
  KEY idx_mini_user_role_status (role_code, status, deleted),
  CONSTRAINT fk_mini_user_sys_user FOREIGN KEY (sys_user_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE media_asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  object_key VARCHAR(500) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  image_width INT,
  image_height INT,
  storage_provider VARCHAR(30) NOT NULL DEFAULT 'S3',
  usage_status VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
  uploaded_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_media_object_key (object_key),
  KEY idx_media_deleted_created (deleted, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE mini_content_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_code VARCHAR(40) NOT NULL,
  category_name VARCHAR(80) NOT NULL,
  description VARCHAR(500),
  sort_order INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_mini_category_code (category_code),
  KEY idx_mini_category_status_sort (status, deleted, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE mini_content_article (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  article_no VARCHAR(40) NOT NULL,
  category_id BIGINT NOT NULL,
  title VARCHAR(150) NOT NULL,
  subtitle VARCHAR(200),
  summary VARCHAR(500),
  cover_media_id BIGINT,
  content_blocks_json JSON NOT NULL,
  publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  published_at DATETIME,
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_mini_article_no (article_no),
  KEY idx_mini_article_category_status (category_id, publish_status, deleted, sort_order),
  CONSTRAINT fk_mini_article_category FOREIGN KEY (category_id) REFERENCES mini_content_category (id),
  CONSTRAINT fk_mini_article_cover FOREIGN KEY (cover_media_id) REFERENCES media_asset (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE mini_content_publish_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  article_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  content_snapshot_json JSON NOT NULL,
  operation_type VARCHAR(20) NOT NULL,
  published_by BIGINT NOT NULL,
  published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_publish_article_version (article_id, version_no),
  KEY idx_publish_time (published_at),
  CONSTRAINT fk_publish_article FOREIGN KEY (article_id) REFERENCES mini_content_article (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE mini_home_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(80) NOT NULL,
  config_value_json JSON NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  updated_by BIGINT NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_mini_home_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE campus_place_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_name VARCHAR(80) NOT NULL,
  icon VARCHAR(80),
  sort_order INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_place_category_status_sort (status, deleted, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE campus_place (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  place_no VARCHAR(40) NOT NULL,
  category_id BIGINT NOT NULL,
  place_name VARCHAR(120) NOT NULL,
  longitude DECIMAL(10, 7) NOT NULL,
  latitude DECIMAL(10, 7) NOT NULL,
  address VARCHAR(300) NOT NULL,
  summary VARCHAR(500),
  contact_phone VARCHAR(30),
  business_hours VARCHAR(150),
  cover_media_id BIGINT,
  detail_blocks_json JSON,
  sort_order INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_campus_place_no (place_no),
  KEY idx_campus_place_category_status (category_id, status, deleted, sort_order),
  CONSTRAINT fk_campus_place_category FOREIGN KEY (category_id) REFERENCES campus_place_category (id),
  CONSTRAINT fk_campus_place_cover FOREIGN KEY (cover_media_id) REFERENCES media_asset (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO
  mini_content_category (
    category_code,
    category_name,
    description,
    sort_order,
    status,
    created_by,
    updated_by
  )
SELECT
  s.code,
  s.name,
  s.description,
  s.sort_order,
  1,
  u.id,
  u.id
FROM
  (
    SELECT
      'CAMPUS_CARD' code,
      '校园卡' name,
      '校园卡信息' description,
      10 sort_order
    UNION ALL
    SELECT
      'CAMPUS_NETWORK',
      '校园网',
      '校园网信息',
      20
    UNION ALL
    SELECT
      'DRIVING_SCHOOL',
      '驾校',
      '驾校信息',
      30
    UNION ALL
    SELECT
      'FAQ',
      '常见问题',
      '常见问题',
      40
    UNION ALL
    SELECT
      'CAMPUS_GUIDE',
      '校园指南',
      '校园生活指南',
      50
  ) s
  JOIN (
    SELECT
      id
    FROM
      sys_user
    WHERE
      deleted = 0
    ORDER BY
      id
    LIMIT
      1
  ) u
ON DUPLICATE KEY UPDATE
  category_name = VALUES(category_name),
  description = VALUES(description),
  sort_order = VALUES(sort_order);

INSERT INTO
  campus_place_category (category_name, sort_order, status)
SELECT
  s.name,
  s.sort_order,
  1
FROM
  (
    SELECT
      '校园服务点' name,
      10 sort_order
    UNION ALL
    SELECT
      '营业厅',
      20
    UNION ALL
    SELECT
      '校园网服务点',
      30
    UNION ALL
    SELECT
      '驾校咨询点',
      40
    UNION ALL
    SELECT
      '便利店',
      50
    UNION ALL
    SELECT
      '打印店',
      60
    UNION ALL
    SELECT
      '快递点',
      70
    UNION ALL
    SELECT
      '食堂',
      80
    UNION ALL
    SELECT
      '宿舍',
      90
    UNION ALL
    SELECT
      '教学楼',
      100
    UNION ALL
    SELECT
      '其他',
      110
  ) s
WHERE
  NOT EXISTS (
    SELECT
      1
    FROM
      campus_place_category
  );
