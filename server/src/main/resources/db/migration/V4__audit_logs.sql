-- V4: 审计日志（管理员操作记录 + 用户访问/浏览事件）

-- 管理员操作日志：AOP 自动采集 /api/admin/** 的写操作（POST/PUT/DELETE）
CREATE TABLE `admin_operation_log` (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT,
  admin_name    VARCHAR(64),
  module        VARCHAR(32)  NOT NULL,
  action        VARCHAR(64)  NOT NULL,
  method        VARCHAR(8)   NOT NULL,
  path          VARCHAR(255) NOT NULL,
  params        MEDIUMTEXT,
  ip            VARCHAR(64),
  user_agent    VARCHAR(255),
  success       TINYINT      NOT NULL DEFAULT 1,
  error         VARCHAR(500),
  duration_ms   INT,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_aol_created (created_at),
  KEY idx_aol_admin (admin_user_id),
  KEY idx_aol_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户事件日志：登录/登出、页面浏览（PV）、课程浏览、课时播放、课件预览等
CREATE TABLE `user_event_log` (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id      BIGINT,
  event_type   VARCHAR(32)  NOT NULL,
  target_type  VARCHAR(32),
  target_id    VARCHAR(64),
  target_name  VARCHAR(255),
  detail       VARCHAR(500),
  ip           VARCHAR(64),
  user_agent   VARCHAR(255),
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_uel_user_time (user_id, created_at),
  KEY idx_uel_type_time (event_type, created_at),
  KEY idx_uel_created (created_at),
  KEY idx_uel_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
