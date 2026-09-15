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

/**
 * 用户事件日志。
 * eventType：LOGIN / LOGIN_FAIL / LOGOUT / REGISTER / PAGE_VIEW /
 *            COURSE_VIEW / LESSON_VIEW / MATERIAL_PREVIEW
 */
@Entity
@Table(name = "user_event_log")
@Getter
@Setter
public class UserEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 匿名访问为空（未登录浏览） */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "event_type", length = 32, nullable = false)
    private String eventType;

    /** PAGE / COURSE / LESSON / MATERIAL */
    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "target_name")
    private String targetName;

    private String detail;

    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
