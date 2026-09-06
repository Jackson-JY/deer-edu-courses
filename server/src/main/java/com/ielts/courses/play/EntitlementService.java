package com.ielts.courses.play;

import com.ielts.courses.catalog.Lesson;
import com.ielts.courses.catalog.LessonRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EntitlementService {

    private final EntitlementRepository entitlementRepository;
    private final LessonRepository lessonRepository;

    public EntitlementService(EntitlementRepository entitlementRepository, LessonRepository lessonRepository) {
        this.entitlementRepository = entitlementRepository;
        this.lessonRepository = lessonRepository;
    }

    public boolean canPlay(long userId, long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return false;
        }
        if (isFreeTrial(lesson)) {
            return true;
        }
        return entitlementRepository.findActive(userId, lesson.getCourseId()).isPresent();
    }

    public boolean canPlayCourse(long userId, long courseId) {
        return entitlementRepository.findActive(userId, courseId).isPresent();
    }

    public boolean isFreeTrial(Lesson lesson) {
        return lesson.getFreeTrial() != null && lesson.getFreeTrial() == 1;
    }

    public Optional<Entitlement> findActive(long userId, long courseId) {
        return entitlementRepository.findActive(userId, courseId);
    }
}
