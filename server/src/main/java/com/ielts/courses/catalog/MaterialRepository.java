package com.ielts.courses.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findByCourseId(Long courseId);

    @Transactional
    void deleteByCourseId(Long courseId);
}
