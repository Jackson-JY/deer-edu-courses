package com.ielts.courses.auth;

import com.ielts.courses.common.BizCode;
import com.ielts.courses.user.Staff;
import com.ielts.courses.user.StaffRepository;
import com.ielts.courses.user.User;
import com.ielts.courses.user.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final JwtIssuer jwtIssuer;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, StaffRepository staffRepository,
                       JwtIssuer jwtIssuer, StringRedisTemplate redisTemplate,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.jwtIssuer = jwtIssuer;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    /** 用户注册：手机号 + 密码 + 昵称，默认 STUDENT 角色 */
    public LoginResult register(String phone, String password, String nickname) {
        if (userRepository.findByPhone(phone).isPresent()) {
            throw new com.ielts.courses.common.ApiException(BizCode.BAD_REQUEST, "该手机号已注册，请直接登录");
        }

        User user = new User();
        user.setPhone(phone);
        user.setNickname(nickname != null && !nickname.isBlank()
                ? nickname.trim()
                : "学员" + phone.substring(phone.length() - 4));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(1);
        user = userRepository.save(user);

        // 默认学生角色
        Staff staff = new Staff();
        staff.setUserId(user.getId());
        staff.setRole("STUDENT");
        staff.setStatus(1);
        staffRepository.save(staff);

        return issueTokenAndResult(user);
    }

    /** 密码登录 */
    public LoginResult loginByPassword(String phone, String password) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new com.ielts.courses.common.ApiException(BizCode.BAD_REQUEST, "手机号或密码错误"));

        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new com.ielts.courses.common.ApiException(BizCode.BAD_REQUEST, "账号已被封禁");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new com.ielts.courses.common.ApiException(BizCode.BAD_REQUEST, "手机号或密码错误");
        }

        return issueTokenAndResult(user);
    }

    public void logout(long userId) {
        redisTemplate.delete("login:session:" + userId);
    }

    /** 登录失败时判断尝试的账号是否为管理员（用于管理员登录审计） */
    public boolean isAdminPhone(String phone) {
        return userRepository.findByPhone(phone)
                .map(u -> resolveRoles(u.getId()).contains("ADMIN"))
                .orElse(false);
    }

    public UserProfile getProfile(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.ielts.courses.common.ApiException(BizCode.NOT_FOUND));
        List<String> roles = resolveRoles(userId);
        return toProfile(user, roles);
    }

    private LoginResult issueTokenAndResult(User user) {
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new com.ielts.courses.common.ApiException(BizCode.BAD_REQUEST, "账号已被封禁");
        }

        String sid = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("login:session:" + user.getId(), sid,
                Duration.ofSeconds(jwtIssuerTtlSeconds()));

        List<String> roles = resolveRoles(user.getId());

        String token = jwtIssuer.issue(user.getId(), sid, roles, true);
        return new LoginResult(token, toProfile(user, roles));
    }

    private List<String> resolveRoles(long userId) {
        List<String> roles = staffRepository.findByUserIdAndStatus(userId, 1).stream()
                .map(Staff::getRole)
                .collect(Collectors.toList());
        if (roles.isEmpty()) {
            roles = List.of("STUDENT");
        }
        return roles;
    }

    private UserProfile toProfile(User user, List<String> roles) {
        return new UserProfile(
                user.getId(),
                user.getNickname(),
                user.getAvatar(),
                maskPhone(user.getPhone()),
                roles,
                false
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private long jwtIssuerTtlSeconds() {
        // JwtIssuer 内部从 JwtProperties 读取，这里返回 7 天
        return 604800L;
    }

    public record LoginResult(String token, UserProfile profile) {
    }

    public record UserProfile(Long id, String nickname, String avatar, String phone,
                              List<String> roles, boolean subscribed) {
    }
}
