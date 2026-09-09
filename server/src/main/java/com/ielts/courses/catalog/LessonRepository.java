package com.ielts.courses.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdOrderBySortAsc(Long courseId);

    List<Lesson> findByChapterIdOrderBySortAsc(Long chapterId);

    long countByCourseId(Long courseId);

    @Transactional
    long deleteByCourseId(Long courseId);

    /** 批量查询多门课程的课时数，返回 [courseId, count] 列表 */
    @Query("select l.courseId, count(l) from Lesson l where l.courseId in :courseIds group by l.courseId")
    List<Object[]> countByCourseIdBatch(@Param("courseIds") List<Long> courseIds);
}
