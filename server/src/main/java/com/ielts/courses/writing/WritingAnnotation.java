package com.ielts.courses.writing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 行内批注：锚定作文 [startPos, endPos) 字符区间。 */
@Entity
@Table(name = "writing_annotation")
@Getter
@Setter
public class WritingAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correction_id", nullable = false)
    private Long correctionId;

    @Column(nullable = false)
    private Integer seq;

    /** G 语法 / V 词汇 / C 衔接 / T 任务 */
    @Column(length = 2, nullable = false)
    private String category;

    @Column(name = "start_pos", nullable = false)
    private int startPos;

    @Column(name = "end_pos", nullable = false)
    private int endPos;

    @Column(length = 500)
    private String quote;

    @Column(length = 2000)
    private String note;

    @Column(length = 500)
    private String suggestion;
}
