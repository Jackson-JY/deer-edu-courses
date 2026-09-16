-- V6: 写作批改使用权限（管理员开通后学生方可提交，每月限 3 篇）
CREATE TABLE `writing_grant` (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT      NOT NULL UNIQUE,
  expire_at  DATETIME,                    -- NULL 表示永久有效
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_wg_expire (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
