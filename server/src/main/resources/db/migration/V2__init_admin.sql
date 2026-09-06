-- 初始化管理员账号
-- 默认管理员手机号：13800138000
-- 本地启动后，使用手机号 + 短信验证码（开发环境验证码见日志）即可登录管理后台

INSERT INTO `user` (phone, nickname, status, created_at, updated_at)
VALUES ('13800138000', '系统管理员', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO `staff` (user_id, role, status, created_at)
SELECT id, 'ADMIN', 1, NOW() FROM `user` WHERE phone = '13800138000'
ON DUPLICATE KEY UPDATE status = 1;
