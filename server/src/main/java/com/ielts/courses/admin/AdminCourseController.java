package com.ielts.courses.admin;

import com.ielts.courses.catalog.*;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.play.EntitlementRepository;
import com.ielts.courses.play.WatchRecordRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final MaterialRepository materialRepository;
    private final WatchRecordRepository watchRecordRepository;
    private final EntitlementRepository entitlementRepository;

    public AdminCourseController(CourseRepository courseRepository, ChapterRepository chapterRepository,
                                 LessonRepository lessonRepository, MaterialRepository materialRepository,
                                 WatchRecordRepository watchRecordRepository,
                                 EntitlementRepository entitlementRepository) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.materialRepository = materialRepository;
        this.watchRecordRepository = watchRecordRepository;
        this.entitlementRepository = entitlementRepository;
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list() {
        List<Course> courses = courseRepository.findAll();
        List<Map<String, Object>> list = courses.stream().map(this::toAdminCourseMap).collect(Collectors.toList());
        return ApiResult.ok(list);
    }

    @PostMapping
    public ApiResult<Course> create(@Valid @RequestBody CourseReq req) {
        Course course = new Course();
        copyFromReq(course, req);
        course.setStatus("DRAFT");
        return ApiResult.ok(courseRepository.save(course));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        Course course = courseRepository.findById(id).orElseThrow();
        Map<String, Object> result = toAdminCourseMap(course);
        List<Chapter> chapters = chapterRepository.findByCourseIdOrderBySortAsc(id);
        for (Chapter chapter : chapters) {
            chapter.setLessons(lessonRepository.findByChapterIdOrderBySortAsc(chapter.getId()));
        }
        result.put("chapters", chapters);
        result.put("materials", materialRepository.findByCourseIdOrderByIdAsc(id));
        return ApiResult.ok(result);
    }

    @PutMapping("/{id}")
    public ApiResult<Course> update(@PathVariable Long id, @Valid @RequestBody CourseReq req) {
        Course course = courseRepository.findById(id).orElseThrow();
        copyFromReq(course, req);
        return ApiResult.ok(courseRepository.save(course));
    }

    @PostMapping("/{id}/publish")
    public ApiResult<Course> publish(@PathVariable Long id) {
        Course course = courseRepository.findById(id).orElseThrow();
        course.setStatus("ONLINE");
        return ApiResult.ok(courseRepository.save(course));
    }

    @PostMapping("/{id}/offline")
    public ApiResult<Course> offline(@PathVariable Long id) {
        Course course = courseRepository.findById(id).orElseThrow();
        course.setStatus("OFFLINE");
        return ApiResult.ok(courseRepository.save(course));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResult<Void> delete(@PathVariable Long id) {
        // 级联删除：课时 → 章节 → 课件 → 观看记录 → 学员权益，均按 courseId 批量删除
        lessonRepository.deleteByCourseId(id);
        chapterRepository.deleteByCourseId(id);
        materialRepository.deleteByCourseId(id);
        watchRecordRepository.deleteByCourseId(id);
        entitlementRepository.deleteByCourseId(id);
        courseRepository.deleteById(id);
        return ApiResult.ok();
    }

    @PostMapping("/{courseId}/chapters")
    public ApiResult<Chapter> createChapter(@PathVariable Long courseId, @Valid @RequestBody ChapterReq req) {
        Chapter chapter = new Chapter();
        chapter.setCourseId(courseId);
        chapter.setTitle(req.title());
        chapter.setSort(req.sort() == null ? 0 : req.sort());
        return ApiResult.ok(chapterRepository.save(chapter));
    }

    @PutMapping("/{courseId}/chapters/{chapterId}")
    public ApiResult<Chapter> updateChapter(@PathVariable Long chapterId, @Valid @RequestBody ChapterReq req) {
        Chapter chapter = chapterRepository.findById(chapterId).orElseThrow();
        chapter.setTitle(req.title());
        chapter.setSort(req.sort() == null ? 0 : req.sort());
        return ApiResult.ok(chapterRepository.save(chapter));
    }

    @DeleteMapping("/{courseId}/chapters/{chapterId}")
    @Transactional
    public ApiResult<Void> deleteChapter(@PathVariable Long chapterId) {
        // 级联删除章节下课时关联的观看记录与课件，再删课时和章节
        List<Lesson> lessons = lessonRepository.findByChapterIdOrderBySortAsc(chapterId);
        for (Lesson ls : lessons) {
            watchRecordRepository.deleteByLessonId(ls.getId());
            materialRepository.deleteByLessonId(ls.getId());
        }
        lessonRepository.deleteAll(lessons);
        chapterRepository.deleteById(chapterId);
        return ApiResult.ok();
    }

    @PostMapping("/{courseId}/chapters/{chapterId}/lessons")
    public ApiResult<Lesson> createLesson(@PathVariable Long courseId, @PathVariable Long chapterId,
                                          @Valid @RequestBody LessonReq req) {
        Lesson lesson = new Lesson();
        lesson.setCourseId(courseId);
        lesson.setChapterId(chapterId);
        lesson.setTitle(req.title());
        lesson.setVodFileId(req.vodFileId());
        lesson.setDuration(req.duration() == null ? 0 : req.duration());
        lesson.setFreeTrial(Boolean.TRUE.equals(req.freeTrial()) ? 1 : 0);
        lesson.setSort(req.sort() == null ? 0 : req.sort());
        return ApiResult.ok(lessonRepository.save(lesson));
    }

    @PutMapping("/{courseId}/lessons/{lessonId}")
    public ApiResult<Lesson> updateLesson(@PathVariable Long lessonId, @Valid @RequestBody LessonReq req) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        lesson.setTitle(req.title());
        lesson.setVodFileId(req.vodFileId());
        lesson.setDuration(req.duration() == null ? 0 : req.duration());
        lesson.setFreeTrial(Boolean.TRUE.equals(req.freeTrial()) ? 1 : 0);
        lesson.setSort(req.sort() == null ? 0 : req.sort());
        return ApiResult.ok(lessonRepository.save(lesson));
    }

    @DeleteMapping("/{courseId}/lessons/{lessonId}")
    @Transactional
    public ApiResult<Void> deleteLesson(@PathVariable Long lessonId) {
        watchRecordRepository.deleteByLessonId(lessonId);
        materialRepository.deleteByLessonId(lessonId);
        lessonRepository.deleteById(lessonId);
        return ApiResult.ok();
    }

    /** 批量调整章节下课时顺序：按 lessonIds 数组顺序重设 sort（0,1,2...）。 */
    @PutMapping("/{courseId}/chapters/{chapterId}/lessons/sort")
    @Transactional
    public ApiResult<Void> sortLessons(@PathVariable Long courseId, @PathVariable Long chapterId,
                                       @RequestBody List<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) {
            return ApiResult.ok();
        }
        List<Lesson> lessons = lessonRepository.findByChapterIdOrderBySortAsc(chapterId);
        Map<Long, Lesson> byId = lessons.stream().collect(Collectors.toMap(Lesson::getId, l -> l));
        for (int i = 0; i < lessonIds.size(); i++) {
            Lesson lesson = byId.get(lessonIds.get(i));
            // 仅接受属于该章节且该课程的课时，忽略非法 id
            if (lesson != null && courseId.equals(lesson.getCourseId())) {
                lesson.setSort(i);
            }
        }
        lessonRepository.saveAll(byId.values());
        return ApiResult.ok();
    }

    private void copyFromReq(Course course, CourseReq req) {
        course.setTitle(req.title());
        course.setCover(req.cover());
        course.setIntro(req.intro());
        course.setTeacherId(req.teacherId());
        course.setPrice(req.price() == null ? 0 : req.price());
        course.setOriginalPrice(req.originalPrice() == null ? 0 : req.originalPrice());
        course.setValidityType(req.validityType() == null ? "PERMANENT" : req.validityType());
        course.setValidityDays(req.validityDays());
        course.setFixedExpireAt(req.fixedExpireAt());
        course.setInSubscription(Boolean.TRUE.equals(req.inSubscription()) ? 1 : 0);
        course.setTargetScore(req.targetScore());
        course.setSort(req.sort() == null ? 0 : req.sort());
    }

    private Map<String, Object> toAdminCourseMap(Course course) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", course.getId());
        map.put("title", course.getTitle());
        map.put("cover", course.getCover() == null ? "" : course.getCover());
        map.put("intro", course.getIntro() == null ? "" : course.getIntro());
        map.put("price", course.getPrice());
        map.put("originalPrice", course.getOriginalPrice());
        map.put("validityType", course.getValidityType());
        map.put("validityDays", course.getValidityDays());
        map.put("inSubscription", course.getInSubscription() != null && course.getInSubscription() == 1);
        map.put("targetScore", course.getTargetScore() == null ? "" : course.getTargetScore());
        map.put("status", course.getStatus());
        map.put("sort", course.getSort());
        return map;
    }

    public record CourseReq(
            @NotBlank String title,
            String cover,
            String intro,
            Long teacherId,
            Integer price,
            Integer originalPrice,
            String validityType,
            Integer validityDays,
            java.time.LocalDateTime fixedExpireAt,
            Boolean inSubscription,
            String targetScore,
            Integer sort
    ) {
    }

    public record ChapterReq(@NotBlank String title, Integer sort) {
    }

    public record LessonReq(
            @NotBlank String title,
            String vodFileId,
            Integer duration,
            Boolean freeTrial,
            Integer sort
    ) {
    }
}
