package com.ielts.courses.play;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchRecordRepository extends JpaRepository<WatchRecord, Long> {

    Optional<WatchRecord> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<WatchRecord> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<WatchRecord> findByUserIdAndCourseIdOrderByUpdatedAtDesc(Long userId, Long courseId);

    @Transactional
    void deleteByLessonId(Long lessonId);

    @Transactional
    void deleteByCourseId(Long courseId);

    /** 累计观看总时长（秒） */
    @Query("select coalesce(sum(w.durationWatched), 0) from WatchRecord w")
    long sumTotalDurationWatched();

    /** 已学完的课时记录数 */
    long countByFinished(Integer finished);

    /** 有过观看行为的去重学员数 */
    @Query("select count(distinct w.userId) from WatchRecord w")
    long countDistinctActiveLearners();

    /** 按课程分组的观看统计，返回 [courseId, 去重学员数, 总观看秒数] */
    @Query("select w.courseId, count(distinct w.userId), coalesce(sum(w.durationWatched), 0) " +
            "from WatchRecord w group by w.courseId order by count(distinct w.userId) desc")
    List<Object[]> statsByCourse();

    /** 按学员分组的观看统计，返回 [userId, 总观看秒数, 学完课时数]，按总观看时长降序 */
    @Query("select w.userId, coalesce(sum(w.durationWatched), 0), " +
            "sum(case when w.finished = 1 then 1 else 0 end) " +
            "from WatchRecord w group by w.userId order by sum(w.durationWatched) desc")
    List<Object[]> statsByUser();
}

