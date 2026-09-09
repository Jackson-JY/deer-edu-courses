package com.ielts.courses.catalog;

import com.ielts.courses.auth.LoginUser;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.CurrentUser;
import com.ielts.courses.play.Entitlement;
import com.ielts.courses.play.EntitlementRepository;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;
    private final EntitlementRepository entitlementRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;

    public CatalogController(CatalogService catalogService, EntitlementRepository entitlementRepository,
                             LessonRepository lessonRepository, CourseRepository courseRepository) {
        this.catalogService = catalogService;
        this.entitlementRepository = entitlementRepository;
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
    }

    @GetMapping("/courses")
    public ApiResult<Map<String, Object>> listCourses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Course> coursePage = catalogService.listPublicCourses(page, size);
        List<Course> courses = coursePage.getContent();
        List<Long> courseIds = courses.stream().map(Course::getId).toList();

        // 批量查询课时数和学员数，消除 N+1
        Map<Long, Long> lessonCounts = batchLessonCounts(courseIds);
        Map<Long, Long> studentCounts = batchStudentCounts(courseIds);

        List<Map<String, Object>> list = courses.stream()
                .map(c -> toCourseCard(c,
                        lessonCounts.getOrDefault(c.getId(), 0L),
                        studentCounts.getOrDefault(c.getId(), 0L)))
                .collect(Collectors.toList());
        Map<String, Object> meta = Map.of(
                "page", page,
                "size", size,
                "total", coursePage.getTotalElements()
        );
        return ApiResult.ok(Map.of("list", list, "meta", meta));
    }

    @GetMapping("/courses/{id}")
    public ApiResult<Map<String, Object>> courseDetail(@PathVariable long id,
                                                       @CurrentUser LoginUser user) {
        CatalogService.CourseDetail detail = catalogService.getCourseDetail(id);
        Course course = detail.course();
        String ctaState = resolveCtaState(user, id);
        // 已开通（有有效权益）的用户，其付费课时应视为解锁；未开通者仅试看课时可看
        boolean entitled = "LEARN".equals(ctaState);

        Map<String, Object> result = new HashMap<>();
        result.put("id", course.getId());
        result.put("title", course.getTitle());
        result.put("cover", course.getCover());
        result.put("intro", course.getIntro());
        result.put("teacher", Map.of("id", course.getTeacherId() == null ? 0 : course.getTeacherId(),
                "name", "讲师", "avatar", "", "intro", ""));
        result.put("targetScore", course.getTargetScore());
        result.put("price", course.getPrice());
        result.put("originalPrice", course.getOriginalPrice());
        result.put("validityType", course.getValidityType());
        result.put("validityDays", course.getValidityDays());
        result.put("fixedExpireAt", course.getFixedExpireAt());
        result.put("inSubscription", course.getInSubscription() != null && course.getInSubscription() == 1);
        result.put("sales", (int) entitlementRepository.countActiveStudentsByCourse(id, LocalDateTime.now()));
        result.put("ctaState", ctaState);
        result.put("chapters", detail.chapters().stream().map(ch -> toChapterMap(ch, entitled)).collect(Collectors.toList()));
        result.put("materials", detail.materials().stream().map(this::toMaterialMap).collect(Collectors.toList()));
        return ApiResult.ok(result);
    }

    @GetMapping("/my/courses")
    public ApiResult<List<Map<String, Object>>> myCourses(@CurrentUser LoginUser user) {
        if (user == null) return ApiResult.ok(List.of());
        List<Entitlement> entitlements = entitlementRepository.findAllActive(user.userId());
        if (entitlements.isEmpty()) return ApiResult.ok(List.of());

        // 批量查课程和课时数，消除 N+1
        List<Long> courseIds = entitlements.stream().map(Entitlement::getCourseId).toList();
        List<Course> courses = courseRepository.findAllById(courseIds);
        Map<Long, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<Long, Long> lessonCounts = batchLessonCounts(courseIds);

        // 同一课程可能被重复授权，按 courseId 去重，永久权益优先、否则保留最晚过期的
        Map<Long, Entitlement> distinctEntitlements = new LinkedHashMap<>();
        for (Entitlement e : entitlements) {
            distinctEntitlements.merge(e.getCourseId(), e, (a, b) -> {
                if (b.getExpireAt() == null) return b;
                if (a.getExpireAt() == null) return a;
                return b.getExpireAt().isAfter(a.getExpireAt()) ? b : a;
            });
        }

        List<Map<String, Object>> list = distinctEntitlements.values().stream()
                .map(e -> {
                    Course course = courseMap.get(e.getCourseId());
                    if (course == null) return null;
                    long lessonCount = lessonCounts.getOrDefault(course.getId(), 0L);
                    return Map.<String, Object>of(
                            "id", course.getId(),
                            "title", course.getTitle() == null ? "" : course.getTitle(),
                            "cover", course.getCover() == null ? "" : course.getCover(),
                            "targetScore", course.getTargetScore() == null ? "" : course.getTargetScore(),
                            "lessonCount", (int) lessonCount,
                            "expireAt", e.getExpireAt() == null ? "" : e.getExpireAt().toString()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        return ApiResult.ok(list);
    }

    private String resolveCtaState(LoginUser user, long courseId) {
        if (user == null) return "GUEST_BUY";
        boolean hasEntitlement = entitlementRepository.findActive(user.userId(), courseId).isPresent();
        return hasEntitlement ? "LEARN" : "BUY";
    }

    /** 批量查询课时数，返回 courseId → count 映射 */
    private Map<Long, Long> batchLessonCounts(List<Long> courseIds) {
        if (courseIds.isEmpty()) return Map.of();
        return lessonRepository.countByCourseIdBatch(courseIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    /** 批量查询学员数，返回 courseId → count 映射 */
    private Map<Long, Long> batchStudentCounts(List<Long> courseIds) {
        if (courseIds.isEmpty()) return Map.of();
        return entitlementRepository.countActiveStudentsBatch(courseIds, LocalDateTime.now()).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private Map<String, Object> toCourseCard(Course course, long lessonCount, long studentCount) {
        return Map.of(
                "id", course.getId(),
                "title", course.getTitle(),
                "cover", course.getCover() == null ? "" : course.getCover(),
                "teacherName", "讲师",
                "targetScore", course.getTargetScore() == null ? "" : course.getTargetScore(),
                "lessonCount", (int) lessonCount,
                "price", course.getPrice() == null ? 0 : course.getPrice(),
                "originalPrice", course.getOriginalPrice() == null ? 0 : course.getOriginalPrice(),
                "inSubscription", course.getInSubscription() != null && course.getInSubscription() == 1,
                "sales", (int) studentCount
        );
    }

    private Map<String, Object> toChapterMap(Chapter chapter, boolean entitled) {
        return Map.of(
                "id", chapter.getId(),
                "title", chapter.getTitle(),
                "lessons", chapter.getLessons().stream().map(ls -> toLessonMap(ls, entitled)).collect(Collectors.toList())
        );
    }

    private Map<String, Object> toLessonMap(Lesson lesson, boolean entitled) {
        boolean trial = lesson.getFreeTrial() != null && lesson.getFreeTrial() == 1;
        return Map.of(
                "id", lesson.getId(),
                "title", lesson.getTitle(),
                "duration", lesson.getDuration() == null ? 0 : lesson.getDuration(),
                "freeTrial", trial,
                // 试看课时对所有人开放；其余课时仅在用户已开通课程时解锁
                "locked", !trial && !entitled,
                "progress", 0
        );
    }

    private Map<String, Object> toMaterialMap(Material material) {
        return Map.of(
                "id", material.getId(),
                "name", material.getName(),
                "type", material.getType(),
                "previewOnly", material.getPreviewOnly() != null && material.getPreviewOnly() == 1
        );
    }
}
