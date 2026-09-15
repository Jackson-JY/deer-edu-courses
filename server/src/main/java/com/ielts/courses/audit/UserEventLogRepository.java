package com.ielts.courses.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserEventLogRepository extends JpaRepository<UserEventLog, Long> {

    @Query(value = """
            SELECT * FROM user_event_log
            WHERE (:userId IS NULL OR user_id = :userId)
              AND (:eventType IS NULL OR event_type = :eventType)
              AND (:targetType IS NULL OR target_type = :targetType)
            ORDER BY id DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM user_event_log
            WHERE (:userId IS NULL OR user_id = :userId)
              AND (:eventType IS NULL OR event_type = :eventType)
              AND (:targetType IS NULL OR target_type = :targetType)
            """,
            nativeQuery = true)
    Page<UserEventLog> search(@Param("userId") Long userId,
                              @Param("eventType") String eventType,
                              @Param("targetType") String targetType,
                              Pageable pageable);

    /** 浏览类事件 PV */
    @Query(value = """
            SELECT COUNT(*) FROM user_event_log
            WHERE event_type IN ('PAGE_VIEW','COURSE_VIEW','LESSON_VIEW')
              AND created_at >= :since
            """, nativeQuery = true)
    long countPvSince(@Param("since") LocalDateTime since);

    /** 浏览类事件 UV：登录用户按 user_id，匿名按 IP 合并去重 */
    @Query(value = """
            SELECT COUNT(DISTINCT COALESCE(CONCAT('u', user_id), CONCAT('i', ip)))
            FROM user_event_log
            WHERE event_type IN ('PAGE_VIEW','COURSE_VIEW','LESSON_VIEW')
              AND created_at >= :since
            """, nativeQuery = true)
    long countUvSince(@Param("since") LocalDateTime since);

    /** 近 N 天每日 PV/UV（仅返回有数据的日期，空日期由 Service 补齐） */
    @Query(value = """
            SELECT DATE(created_at) AS d,
                   COUNT(*) AS pv,
                   COUNT(DISTINCT COALESCE(CONCAT('u', user_id), CONCAT('i', ip))) AS uv
            FROM user_event_log
            WHERE event_type IN ('PAGE_VIEW','COURSE_VIEW','LESSON_VIEW')
              AND created_at >= :since
            GROUP BY DATE(created_at)
            ORDER BY d
            """, nativeQuery = true)
    List<Object[]> dailyPvUv(@Param("since") LocalDateTime since);

    /** 近 N 天课程浏览榜 */
    @Query(value = """
            SELECT target_id, MAX(target_name) AS name, COUNT(*) AS c
            FROM user_event_log
            WHERE event_type = 'COURSE_VIEW' AND created_at >= :since
            GROUP BY target_id
            ORDER BY c DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> topCourses(@Param("since") LocalDateTime since);

    /** 当日各事件类型计数 */
    @Query(value = """
            SELECT event_type, COUNT(*) FROM user_event_log
            WHERE created_at >= :since GROUP BY event_type
            """, nativeQuery = true)
    List<Object[]> countByTypeSince(@Param("since") LocalDateTime since);

    List<UserEventLog> findTop20ByOrderByIdDesc();
}
