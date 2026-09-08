-- V3: 用户密码字段（从验证码登录切换为密码登录）
-- 管理员默认密码由 AdminSeedRunner 在启动时写入（admin123）

ALTER TABLE `user` ADD COLUMN password_hash VARCHAR(255) AFTER avatar;
