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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;
    private final EntitlementRepository entitlementRepository;
    private final LessonRepository lessonRepository;

    public CatalogController(CatalogService catalogService, EntitlementRepository entitlementRepository,
                             LessonRepository lessonRepository) {
        this.catalogService = catalogService;
        this.entitlementRepository = entitlementRepository;
        this.lessonRepository = lessonRepository;
    }

    @GetMapping("/courses")
    public ApiResult<Map<String, Object>> listCourses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Course> coursePage = catalogService.listPublicCourses(page, size);
        List<Map<String, Object>> list = coursePage.getContent().stream()
                .map(this::toCourseCard)
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
        result.put("sales", 0);
        result.put("ctaState", ctaState);
        result.put("chapters", detail.chapters().stream().map(this::toChapterMap).collect(Collectors.toList()));
        result.put("materials", detail.materials().stream().map(this::toMaterialMap).collect(Collectors.toList()));
        return ApiResult.ok(result);
    }

    @GetMapping("/my/courses")
    public ApiResult<List<Map<String, Object>>> myCourses(@CurrentUser LoginUser user) {
        if (user == null) return ApiResult.ok(List.of());
        List<Entitlement> entitlements = entitlementRepository.findAllActive(user.userId());
        List<Map<String, Object>> list = entitlements.stream()
                .map(e -> {
                    Course course = catalogService.findById(e.getCourseId());
                    if (course == null) return null;
                    long lessonCount = lessonRepository.countByCourseId(course.getId());
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

    private Map<String, Object> toCourseCard(Course course) {
        long lessonCount = lessonRepository.countByCourseId(course.getId());
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
                "sales", 0
        );
    }

    private Map<String, Object> toChapterMap(Chapter chapter) {
        return Map.of(
                "id", chapter.getId(),
                "title", chapter.getTitle(),
                "lessons", chapter.getLessons().stream().map(this::toLessonMap).collect(Collectors.toList())
        );
    }

    private Map<String, Object> toLessonMap(Lesson lesson) {
        boolean trial = lesson.getFreeTrial() != null && lesson.getFreeTrial() == 1;
        return Map.of(
                "id", lesson.getId(),
                "title", lesson.getTitle(),
                "duration", lesson.getDuration() == null ? 0 : lesson.getDuration(),
                "freeTrial", trial,
                "locked", !trial,
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
