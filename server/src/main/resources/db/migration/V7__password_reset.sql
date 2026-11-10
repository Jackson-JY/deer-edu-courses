-- V7: 找回密码（管理端审核 + 一次性重置码）
-- 学生提交申请 → 管理员线下核实后生成 6 位重置码 → 学生自助设置新密码
CREATE TABLE `password_reset_request` (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id          BIGINT       NOT NULL,
  phone            VARCHAR(20)  NOT NULL,
  status           VARCHAR(16)  NOT NULL DEFAULT 'PENDING',  -- PENDING / CODE_ISSUED / USED / REJECTED / EXPIRED
  reset_code_hash  VARCHAR(100),                             -- BCrypt(6位重置码)，未签发为 NULL
  code_expire_at   DATETIME,
  request_ip       VARCHAR(64),
  handled_by       BIGINT,                                   -- 处理的管理员 user_id
  handled_at       DATETIME,
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_prr_status (status, created_at),
  KEY idx_prr_user (user_id),
  KEY idx_prr_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
