package com.ielts.courses.play;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface EntitlementRepository extends JpaRepository<Entitlement, Long> {

    Optional<Entitlement> findByUserIdAndCourseIdAndStatus(Long userId, Long courseId, String status);

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
