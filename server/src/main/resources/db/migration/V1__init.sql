-- 雅思课程网站 初始化 schema（对应 docs/需求与方案梳理.md 第 10 节）
-- 金额单位：分。时间 UTC。

CREATE TABLE `user` (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  unionid     VARCHAR(64) UNIQUE,
  openid_h5   VARCHAR(64),
  openid_web  VARCHAR(64),
  nickname    VARCHAR(64),
  avatar      VARCHAR(255),
  phone       VARCHAR(20) UNIQUE,
  status      TINYINT NOT NULL DEFAULT 1,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `staff` (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  role        VARCHAR(20) NOT NULL,
  status      TINYINT NOT NULL DEFAULT 1,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_staff_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `course` (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  title           VARCHAR(128) NOT NULL,
  cover           VARCHAR(255),
  intro           MEDIUMTEXT,
  category_id     BIGINT,
  teacher_id      BIGINT,
  price           INT NOT NULL DEFAULT 0,
  original_price  INT NOT NULL DEFAULT 0,
  validity_type   VARCHAR(16) NOT NULL DEFAULT 'PERMANENT',
  validity_days   INT,
  fixed_expire_at DATETIME,
  in_subscription TINYINT NOT NULL DEFAULT 0,
  target_score    VARCHAR(8),
  status          VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  sort            INT NOT NULL DEFAULT 0,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_course_status (status),
  KEY idx_course_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `chapter` (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id  BIGINT NOT NULL,
  title      VARCHAR(128) NOT NULL,
  sort       INT NOT NULL DEFAULT 0,
  KEY idx_chapter_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `lesson` (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  chapter_id  BIGINT NOT NULL,
  course_id   BIGINT NOT NULL,
  title       VARCHAR(128) NOT NULL,
  vod_file_id VARCHAR(64),
  duration    INT NOT NULL DEFAULT 0,
  free_trial  TINYINT NOT NULL DEFAULT 0,
  sort        INT NOT NULL DEFAULT 0,
  KEY idx_lesson_course (course_id),
  KEY idx_lesson_chapter (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `material` (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id     BIGINT NOT NULL,
  lesson_id     BIGINT,
  name          VARCHAR(128) NOT NULL,
  type          VARCHAR(16) NOT NULL,
  cos_key       VARCHAR(255) NOT NULL,
  preview_only  TINYINT NOT NULL DEFAULT 1,
  KEY idx_material_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `bundle` (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  title       VARCHAR(128) NOT NULL,
  price       INT NOT NULL DEFAULT 0,
  status      VARCHAR(16) NOT NULL DEFAULT 'ONLINE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `bundle_course` (
  bundle_id   BIGINT NOT NULL,
  course_id   BIGINT NOT NULL,
  PRIMARY KEY (bundle_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `sub_plan` (
  id      BIGINT PRIMARY KEY AUTO_INCREMENT,
  title   VARCHAR(64) NOT NULL,
  period  VARCHAR(16) NOT NULL,
  price   INT NOT NULL DEFAULT 0,
  status  VARCHAR(16) NOT NULL DEFAULT 'ONLINE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user_subscription` (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL,
  plan_id    BIGINT NOT NULL,
  start_at   DATETIME NOT NULL,
  expire_at  DATETIME NOT NULL,
  status     VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  KEY idx_usub_user (user_id),
  KEY idx_usub_expire (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `coupon` (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  name       VARCHAR(64) NOT NULL,
  type       VARCHAR(16) NOT NULL,
  rule_json  JSON NOT NULL,
  start_at   DATETIME NOT NULL,
  end_at     DATETIME NOT NULL,
  status     VARCHAR(16) NOT NULL DEFAULT 'ONLINE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user_coupon` (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  coupon_id     BIGINT NOT NULL,
  status        VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
  used_order_id BIGINT,
  received_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ucoupon_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `order` (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no          VARCHAR(32) NOT NULL UNIQUE,
  user_id           BIGINT NOT NULL,
  item_type         VARCHAR(16) NOT NULL,
  item_id           BIGINT NOT NULL,
  origin_amount     INT NOT NULL,
  discount_amount   INT NOT NULL DEFAULT 0,
  pay_amount        INT NOT NULL,
  coupon_id         BIGINT,
  status            VARCHAR(16) NOT NULL DEFAULT 'UNPAID',
  pay_channel       VARCHAR(16),
  wx_transaction_id VARCHAR(64),
  paid_at           DATETIME,
  closed_at         DATETIME,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_order_user (user_id),
  KEY idx_order_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `entitlement` (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id         BIGINT NOT NULL,
  course_id       BIGINT NOT NULL,
  source          VARCHAR(16) NOT NULL,
  source_order_id BIGINT,
  expire_at       DATETIME,
  status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  UNIQUE KEY uk_user_course (user_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `watch_record` (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id          BIGINT NOT NULL,
  lesson_id        BIGINT NOT NULL,
  course_id        BIGINT NOT NULL,
  progress         INT NOT NULL DEFAULT 0,
  last_position    INT NOT NULL DEFAULT 0,
  duration_watched INT NOT NULL DEFAULT 0,
  finished         TINYINT NOT NULL DEFAULT 0,
  updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_lesson (user_id, lesson_id),
  KEY idx_wr_user_course (user_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `refund` (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id     BIGINT NOT NULL,
  amount       INT NOT NULL,
  reason       VARCHAR(255),
  status       VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  wx_refund_id VARCHAR(64),
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_refund_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
