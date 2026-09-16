package com.ielts.courses.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WritingSubmissionRepository extends JpaRepository<WritingSubmission, Long> {

    List<WritingSubmission> findByUserIdOrderByIdDesc(Long userId);

    List<WritingSubmission> findByStatusOrderByIdAsc(String status);

    List<WritingSubmission> findByStatusNotOrderByIdDesc(String status);

    /** 本月已提交篇数（额度统计用） */
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);
}
