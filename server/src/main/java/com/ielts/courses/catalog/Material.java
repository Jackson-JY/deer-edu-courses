package com.ielts.courses.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "material")
@Getter
@Setter
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "lesson_id")
    private Long lessonId;

    private String name;

    @Column(length = 16)
    private String type;

    @Column(name = "cos_key")
    private String cosKey;

    @Column(name = "preview_only")
    private Integer previewOnly = 1;
}
