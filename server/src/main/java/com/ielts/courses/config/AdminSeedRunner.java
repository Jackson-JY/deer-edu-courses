package com.ielts.courses.config;

import com.ielts.courses.user.Staff;
import com.ielts.courses.user.StaffRepository;
import com.ielts.courses.user.User;
import com.ielts.courses.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时确保管理员账号可用：
 * - 若管理员 (13800138000) 未设置密码，写入默认密码 admin123（BCrypt 加密）
 * - 确保存在 ADMIN 角色记录
 */
@Component
public class AdminSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeedRunner.class);
    private static final String ADMIN_PHONE = "13800138000";
    private static final String DEFAULT_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeedRunner(UserRepository userRepository, StaffRepository staffRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        userRepository.findByPhone(ADMIN_PHONE).ifPresent(user -> {
            if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
                user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
                userRepository.save(user);
                log.warn("[SEED] 管理员默认密码已设置为: {}（手机号 {}），请尽快登录后台修改密码", DEFAULT_PASSWORD, ADMIN_PHONE);
            }
            boolean hasAdmin = staffRepository.findByUserIdAndStatus(user.getId(), 1).stream()
                    .anyMatch(s -> "ADMIN".equals(s.getRole()));
            if (!hasAdmin) {
                Staff staff = new Staff();
                staff.setUserId(user.getId());
                staff.setRole("ADMIN");
                staff.setStatus(1);
                staffRepository.save(staff);
                log.info("[SEED] 已为管理员 {} 补建 ADMIN 角色记录", ADMIN_PHONE);
            }
        });
    }
}
