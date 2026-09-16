package com.ielts.courses.writing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 写作批改结果（AI 草稿 → 教师审核发布）。 */
@Entity
@Table(name = "writing_correction")
@Getter
@Setter
public class WritingCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false, unique = true)
    private Long submissionId;

    @Column(name = "corrector_id")
    private Long correctorId;

    @Column(name = "score_tr", precision = 3, scale = 1)
    private BigDecimal scoreTr;

    @Column(name = "score_cc", precision = 3, scale = 1)
    private BigDecimal scoreCc;

    @Column(name = "score_lr", precision = 3, scale = 1)
    private BigDecimal scoreLr;

    @Column(name = "score_ga", precision = 3, scale = 1)
    private BigDecimal scoreGa;

    @Column(precision = 3, scale = 1)
    private BigDecimal overall;

    @Column(name = "comment_tr", length = 1000)
    private String commentTr;

    @Column(name = "comment_cc", length = 1000)
    private String commentCc;

    @Column(name = "comment_lr", length = 1000)
    private String commentLr;

    @Column(name = "comment_ga", length = 1000)
    private String commentGa;

    @Column(name = "closing_remark", length = 500)
    private String closingRemark;

    /** 范文思路提纲 JSON：[{"heading":"NEGATIVE","points":[...]}] */
    @Column(name = "model_ideas", columnDefinition = "JSON")
    private String modelIdeas;

    @Column(name = "model_essay", columnDefinition = "MEDIUMTEXT")
    private String modelEssay;

    /** DRAFT / PUBLISHED */
    @Column(length = 16, nullable = false)
    private String status = "DRAFT";

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
