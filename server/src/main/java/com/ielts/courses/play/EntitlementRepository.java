package com.ielts.courses.play;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EntitlementRepository extends JpaRepository<Entitlement, Long> {

    Optional<Entitlement> findByUserIdAndCourseIdAndStatus(Long userId, Long courseId, String status);

    default Optional<Entitlement> findActive(Long userId, Long courseId) {
        return findByUserIdAndCourseIdAndStatus(userId, courseId, "ACTIVE")
                .filter(e -> e.getExpireAt() == null || e.getExpireAt().isAfter(LocalDateTime.now()));
    }
}
