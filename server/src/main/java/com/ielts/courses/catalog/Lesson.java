package com.ielts.courses.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lesson")
@Getter
@Setter
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(name = "course_id")
    private Long courseId;

    private String title;

    @Column(name = "vod_file_id", length = 64)
    private String vodFileId;

    private Integer duration = 0;

    @Column(name = "free_trial")
    private Integer freeTrial = 0;

    private Integer sort = 0;
}
