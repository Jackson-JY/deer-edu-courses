package com.ielts.courses.writing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WritingGrantRepository extends JpaRepository<WritingGrant, Long> {

    Optional<WritingGrant> findByUserId(Long userId);

    List<WritingGrant> findAllBy();
}
