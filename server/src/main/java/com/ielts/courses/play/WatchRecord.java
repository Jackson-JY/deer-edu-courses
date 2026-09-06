package com.ielts.courses.play;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_record")
@Getter
@Setter
public class WatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "lesson_id")
    private Long lessonId;

    private Integer progress = 0;

    @Column(name = "last_position")
    private Integer lastPosition = 0;

    @Column(name = "duration_watched")
    private Integer durationWatched = 0;

    private Integer finished = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
