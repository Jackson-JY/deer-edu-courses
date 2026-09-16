package com.ielts.courses.writing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 学生写作提交（题目随作文一并提交）。 */
@Entity
@Table(name = "writing_submission")
@Getter
@Setter
public class WritingSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_prompt", nullable = false, columnDefinition = "TEXT")
    private String taskPrompt;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "word_count", nullable = false)
    private int wordCount;

    /** PENDING / AI_DRAFTING / AWAITING_REVIEW / CORRECTED / FAILED */
    @Column(length = 24, nullable = false)
    private String status = "PENDING";

    @Column(name = "ai_error", length = 500)
    private String aiError;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
