package com.ielts.courses.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 管理员操作日志（/api/admin/** 写操作，AOP 自动采集） */
@Entity
@Table(name = "admin_operation_log")
@Getter
@Setter
public class AdminOperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id")
    private Long adminUserId;

    /** 操作者昵称/手机号快照 */
    @Column(name = "admin_name")
    private String adminName;

    /** 模块：courses/users/materials/vod/security/upload 等 */
    private String module;

    /** 动作描述：新建课程/删除课时/上架 等 */
    private String action;

    private String method;

    private String path;

    /** 请求参数 JSON（脱敏、截断） */
    private String params;

    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    /** 1 成功 / 0 失败 */
    private Integer success = 1;

    private String error;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
