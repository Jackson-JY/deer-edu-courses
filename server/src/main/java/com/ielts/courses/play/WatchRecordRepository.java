package com.ielts.courses.play;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchRecordRepository extends JpaRepository<WatchRecord, Long> {

    Optional<WatchRecord> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<WatchRecord> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<WatchRecord> findByUserIdAndCourseIdOrderByUpdatedAtDesc(Long userId, Long courseId);
}
