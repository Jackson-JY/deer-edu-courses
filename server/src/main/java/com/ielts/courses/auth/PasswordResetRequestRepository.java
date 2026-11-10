package com.ielts.courses.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {

    Optional<PasswordResetRequest> findFirstByPhoneAndStatusOrderByIdDesc(String phone, String status);

    /** 同一手机号 24 小时内是否已有待处理申请（防重复提交） */
    boolean existsByPhoneAndStatusInAndCreatedAtAfter(String phone, Collection<String> statuses, LocalDateTime after);

    Page<PasswordResetRequest> findAllByStatusOrderByIdDesc(String status, Pageable pageable);

    Page<PasswordResetRequest> findAllByOrderByIdDesc(Pageable pageable);

    long countByStatus(String status);
}
