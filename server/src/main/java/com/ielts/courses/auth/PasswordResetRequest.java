package com.ielts.courses.auth;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 密码重置申请：学生提交，管理员核实后签发一次性重置码 */
@Entity
@Table(name = "password_reset_request")
public class PasswordResetRequest {

    public static final String PENDING = "PENDING";
    public static final String CODE_ISSUED = "CODE_ISSUED";
    public static final String USED = "USED";
    public static final String REJECTED = "REJECTED";
    public static final String EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "status", nullable = false, length = 16)
    private String status = PENDING;

    @Column(name = "reset_code_hash", length = 100)
    private String resetCodeHash;

    @Column(name = "code_expire_at")
    private LocalDateTime codeExpireAt;

    @Column(name = "request_ip", length = 64)
    private String requestIp;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResetCodeHash() { return resetCodeHash; }
    public void setResetCodeHash(String resetCodeHash) { this.resetCodeHash = resetCodeHash; }

    public LocalDateTime getCodeExpireAt() { return codeExpireAt; }
    public void setCodeExpireAt(LocalDateTime codeExpireAt) { this.codeExpireAt = codeExpireAt; }

    public String getRequestIp() { return requestIp; }
    public void setRequestIp(String requestIp) { this.requestIp = requestIp; }

    public Long getHandledBy() { return handledBy; }
    public void setHandledBy(Long handledBy) { this.handledBy = handledBy; }

    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
