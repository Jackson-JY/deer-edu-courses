package com.ielts.courses.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WritingCorrectionRepository extends JpaRepository<WritingCorrection, Long> {

    Optional<WritingCorrection> findBySubmissionId(Long submissionId);
}
