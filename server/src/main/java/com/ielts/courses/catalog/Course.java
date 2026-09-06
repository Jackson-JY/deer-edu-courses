package com.ielts.courses.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "course")
@Getter
@Setter
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String cover;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String intro;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "teacher_id")
    private Long teacherId;

    private Integer price = 0;

    @Column(name = "original_price")
    private Integer originalPrice = 0;

    @Column(name = "validity_type", length = 16)
    private String validityType = "PERMANENT";

    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(name = "fixed_expire_at")
    private LocalDateTime fixedExpireAt;

    @Column(name = "in_subscription")
    private Integer inSubscription = 0;

    @Column(name = "target_score", length = 8)
    private String targetScore;

    @Column(length = 16)
    private String status = "DRAFT";

    private Integer sort = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
