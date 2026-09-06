package com.ielts.courses.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdOrderBySortAsc(Long courseId);

    List<Lesson> findByChapterIdOrderBySortAsc(Long chapterId);

    long countByCourseId(Long courseId);
}
