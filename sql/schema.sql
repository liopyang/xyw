CREATE DATABASE IF NOT EXISTS campus_business DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE campus_business;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  phone VARCHAR(20),
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  role_code VARCHAR(20) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_phone (phone),
  INDEX idx_user_role (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS agent (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  agent_no VARCHAR(30) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  level VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_agent_user (user_id),
  UNIQUE KEY uk_agent_phone (phone),
  CONSTRAINT fk_agent_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS biz_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL UNIQUE,
  business_type VARCHAR(30) NOT NULL,
  customer_name VARCHAR(50) NOT NULL,
  contact_phone VARCHAR(20) NOT NULL,
  business_number VARCHAR(30),
  source_channel VARCHAR(20) NOT NULL,
  agent_id BIGINT,
  created_by BIGINT NOT NULL,
  audit_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  remark VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  deleted_by BIGINT,
  deleted_at DATETIME,
  INDEX idx_order_created (created_at),
  INDEX idx_order_type_status (business_type, audit_status, deleted),
  INDEX idx_order_agent (agent_id),
  INDEX idx_order_number (business_type, business_number),
  INDEX idx_order_phone (business_type, contact_phone),
  CONSTRAINT fk_order_agent FOREIGN KEY (agent_id) REFERENCES agent (id),
  CONSTRAINT fk_order_creator FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS order_campus_network (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL UNIQUE,
  student_no VARCHAR(30) NOT NULL,
  id_card_last_six CHAR(6) NOT NULL,
  export_status VARCHAR(20) NOT NULL DEFAULT 'NOT_EXPORTED',
  CONSTRAINT fk_network_order FOREIGN KEY (order_id) REFERENCES biz_order (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS order_driving_school (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL UNIQUE,
  license_type VARCHAR(10) NOT NULL,
  class_type VARCHAR(20) NOT NULL,
  payment_amount DECIMAL(10, 2) NOT NULL,
  CONSTRAINT fk_driving_order FOREIGN KEY (order_id) REFERENCES biz_order (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS order_renewal (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL UNIQUE,
  renewal_amount DECIMAL(10, 2) NOT NULL,
  CONSTRAINT fk_renewal_order FOREIGN KEY (order_id) REFERENCES biz_order (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS support_issue (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  issue_no VARCHAR(32) NOT NULL UNIQUE,
  submitter_id BIGINT NOT NULL,
  submitter_type VARCHAR(20) NOT NULL,
  submitter_name VARCHAR(50) NOT NULL,
  contact_phone VARCHAR(20) NOT NULL,
  issue_type VARCHAR(30) NOT NULL,
  description TEXT NOT NULL,
  business_number VARCHAR(30),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  process_remark VARCHAR(1000),
  processor_id BIGINT,
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_issue_status (status),
  INDEX idx_issue_submitter (submitter_id, submitter_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS support_issue_image (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  issue_id BIGINT NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_issue_image FOREIGN KEY (issue_id) REFERENCES support_issue (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS business_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(80) NOT NULL UNIQUE,
  config_value VARCHAR(255) NOT NULL,
  description VARCHAR(255),
  updated_by BIGINT,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT NOT NULL,
  operator_name VARCHAR(50) NOT NULL,
  module VARCHAR(30) NOT NULL,
  operation_type VARCHAR(30) NOT NULL,
  target_id BIGINT,
  operation_description VARCHAR(500),
  ip_address VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_log_created (created_at),
  INDEX idx_log_operator (operator_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
