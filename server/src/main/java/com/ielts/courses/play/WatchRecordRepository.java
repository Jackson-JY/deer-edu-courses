package com.ielts.courses.play;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchRecordRepository extends JpaRepository<WatchRecord, Long> {

    Optional<WatchRecord> findByUserIdAndLessonId(Long userId, Long lessonId);
}
