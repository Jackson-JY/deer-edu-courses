package com.ielts.courses.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLog, Long> {

    @Query(value = """
            SELECT * FROM admin_operation_log
            WHERE (:adminId IS NULL OR admin_user_id = :adminId)
              AND (:module IS NULL OR module = :module)
              AND (:keyword IS NULL OR action LIKE CONCAT('%', :keyword, '%')
                   OR path LIKE CONCAT('%', :keyword, '%')
                   OR admin_name LIKE CONCAT('%', :keyword, '%'))
            ORDER BY id DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM admin_operation_log
            WHERE (:adminId IS NULL OR admin_user_id = :adminId)
              AND (:module IS NULL OR module = :module)
              AND (:keyword IS NULL OR action LIKE CONCAT('%', :keyword, '%')
                   OR path LIKE CONCAT('%', :keyword, '%')
                   OR admin_name LIKE CONCAT('%', :keyword, '%'))
            """,
            nativeQuery = true)
    Page<AdminOperationLog> search(@Param("adminId") Long adminId,
                                   @Param("module") String module,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime since);

    List<AdminOperationLog> findTop10ByOrderByIdDesc();
}
