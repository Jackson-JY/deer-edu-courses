package com.ielts.courses.play;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "entitlement")
@Getter
@Setter
public class Entitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(length = 16)
    private String source;

    @Column(name = "source_order_id")
    private Long sourceOrderId;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(length = 16)
    private String status = "ACTIVE";
}
