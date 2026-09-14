package com.ielts.courses.catalog;

import com.ielts.courses.common.ApiException;
import com.ielts.courses.common.BizCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final MaterialRepository materialRepository;

    public CatalogService(CourseRepository courseRepository, ChapterRepository chapterRepository,
                          LessonRepository lessonRepository, MaterialRepository materialRepository) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.materialRepository = materialRepository;
    }

    public Page<Course> listPublicCourses(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("sort").descending());
        return courseRepository.findByStatusIn(List.of("ONLINE"), pageable);
    }

    public CourseDetail getCourseDetail(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(BizCode.NOT_FOUND));
        List<Chapter> chapters = chapterRepository.findByCourseIdOrderBySortAsc(courseId);
        for (Chapter chapter : chapters) {
            chapter.setLessons(lessonRepository.findByChapterIdOrderBySortAsc(chapter.getId()));
        }
        List<Material> materials = materialRepository.findByCourseIdOrderByIdAsc(courseId);
        return new CourseDetail(course, chapters, materials);
    }

    public Course findById(Long courseId) {
        return courseRepository.findById(courseId).orElse(null);
    }

    public record CourseDetail(Course course, List<Chapter> chapters, List<Material> materials) {
    }
}
