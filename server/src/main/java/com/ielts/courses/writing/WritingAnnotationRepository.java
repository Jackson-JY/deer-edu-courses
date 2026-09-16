package com.ielts.courses.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WritingAnnotationRepository extends JpaRepository<WritingAnnotation, Long> {

    List<WritingAnnotation> findByCorrectionIdOrderBySeqAsc(Long correctionId);

    void deleteByCorrectionId(Long correctionId);
}
