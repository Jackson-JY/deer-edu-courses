package com.ielts.courses.play;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface EntitlementRepository extends JpaRepository<Entitlement, Long> {

    Optional<Entitlement> findByUserIdAndCourseIdAndStatus(Long userId, Long courseId, String status);

    /** 统计某门课程当前有效（ACTIVE 且未过期）的去重学员数，用于"人在学/位学员"展示 */
    @Query("select count(distinct e.userId) from Entitlement e " +
            "where e.courseId = :courseId and e.status = 'ACTIVE' " +
            "and (e.expireAt is null or e.expireAt > :now)")
    long countActiveStudentsByCourse(@Param("courseId") Long courseId, @Param("now") LocalDateTime now);

    default Optional<Entitlement> findActive(Long userId, Long courseId) {
        return findByUserIdAndCourseIdAndStatus(userId, courseId, "ACTIVE")
                .filter(e -> e.getExpireAt() == null || e.getExpireAt().isAfter(LocalDateTime.now()));
    }

    List<Entitlement> findByUserIdAndStatus(Long userId, String status);

    default List<Entitlement> findAllActive(Long userId) {
        return findByUserIdAndStatus(userId, "ACTIVE").stream()
                .filter(e -> e.getExpireAt() == null || e.getExpireAt().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
    }
}
