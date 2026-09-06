package com.ielts.courses.auth;

import com.ielts.courses.common.BizCode;
import com.ielts.courses.user.Staff;
import com.ielts.courses.user.StaffRepository;
import com.ielts.courses.user.User;
import com.ielts.courses.user.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final SmsCodeService smsCodeService;
    private final JwtIssuer jwtIssuer;
    private final StringRedisTemplate redisTemplate;

    public AuthService(UserRepository userRepository, StaffRepository staffRepository,
                       SmsCodeService smsCodeService, JwtIssuer jwtIssuer,
                       StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.smsCodeService = smsCodeService;
        this.jwtIssuer = jwtIssuer;
        this.redisTemplate = redisTemplate;
    }

    public LoginResult loginOrRegister(String phone, String code) {
        if (!smsCodeService.verify(phone, code)) {
            throw new com.ielts.courses.common.ApiException(BizCode.BAD_REQUEST, "验证码错误或已过期");
        }

        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> createUser(phone));

        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new com.ielts.courses.common.ApiException(BizCode.BAD_REQUEST, "账号已被封禁");
        }

        String sid = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("login:session:" + user.getId(), sid,
                Duration.ofSeconds(jwtIssuerTtlSeconds()));

        List<String> roles = staffRepository.findByUserIdAndStatus(user.getId(), 1).stream()
                .map(Staff::getRole)
                .collect(Collectors.toList());
        if (roles.isEmpty()) {
            roles = List.of("USER");
        }

        String token = jwtIssuer.issue(user.getId(), sid, roles, true);
        return new LoginResult(token, toProfile(user, roles));
    }

    public void logout(long userId) {
        redisTemplate.delete("login:session:" + userId);
    }

    public UserProfile getProfile(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.ielts.courses.common.ApiException(BizCode.NOT_FOUND));
        List<String> roles = staffRepository.findByUserIdAndStatus(userId, 1).stream()
                .map(Staff::getRole)
                .collect(Collectors.toList());
        if (roles.isEmpty()) {
            roles = List.of("USER");
        }
        return toProfile(user, roles);
    }

    private User createUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickname("学员" + phone.substring(phone.length() - 4));
        user.setStatus(1);
        return userRepository.save(user);
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
        // JwtIssuer 内部从 JwtProperties 读取，这里为了简单直接返回 7 天
        return 604800L;
    }

    public record LoginResult(String token, UserProfile profile) {
    }

    public record UserProfile(Long id, String nickname, String avatar, String phone,
                              List<String> roles, boolean subscribed) {
    }
}
