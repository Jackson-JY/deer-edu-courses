package com.ielts.courses.admin;

import com.ielts.courses.catalog.Course;
import com.ielts.courses.catalog.CourseRepository;
import com.ielts.courses.catalog.LessonRepository;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.play.EntitlementRepository;
import com.ielts.courses.play.WatchRecordRepository;
import com.ielts.courses.user.User;
import com.ielts.courses.user.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EntitlementRepository entitlementRepository;
    private final WatchRecordRepository watchRecordRepository;
    private final LessonRepository lessonRepository;

    public AdminDashboardController(CourseRepository courseRepository, UserRepository userRepository,
                                    EntitlementRepository entitlementRepository,
                                    WatchRecordRepository watchRecordRepository,
                                    LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.entitlementRepository = entitlementRepository;
        this.watchRecordRepository = watchRecordRepository;
        this.lessonRepository = lessonRepository;
    }

    @GetMapping
    public ApiResult<Map<String, Object>> stats() {
        LocalDateTime now = LocalDateTime.now();

        // 基础数据
        long courseCount = courseRepository.count();
        long onlineCourseCount = courseRepository.countByStatus("ONLINE");
        long userCount = userRepository.count();
        long lessonCount = lessonRepository.count();
        long activeEntitlementCount = entitlementRepository.countByStatus("ACTIVE");
        long activeUserCount = entitlementRepository.countDistinctActiveUsers("ACTIVE", now);

        // 学习数据
        long totalWatchSeconds = watchRecordRepository.sumTotalDurationWatched();
        long finishedLessonCount = watchRecordRepository.countByFinished(1);
        long activeLearnerCount = watchRecordRepository.countDistinctActiveLearners();

        Map<String, Object> data = new HashMap<>();
        data.put("courseCount", courseCount);
        data.put("onlineCourseCount", onlineCourseCount);
        data.put("lessonCount", lessonCount);
        data.put("userCount", userCount);
        data.put("activeEntitlementCount", activeEntitlementCount);
        data.put("activeUserCount", activeUserCount);
        data.put("totalWatchMinutes", totalWatchSeconds / 60);
        data.put("finishedLessonCount", finishedLessonCount);
        data.put("activeLearnerCount", activeLearnerCount);

        // 课程学习排行榜（按去重学员数降序）
        List<Object[]> rows = watchRecordRepository.statsByCourse();
        List<Map<String, Object>> courseRanking = new ArrayList<>();
        for (Object[] row : rows) {
            Long courseId = (Long) row[0];
            Long learners = (Long) row[1];
            Long seconds = (Long) row[2];
            String title = courseRepository.findById(courseId).map(Course::getTitle).orElse("未知课程");
            Map<String, Object> m = new HashMap<>();
            m.put("courseId", courseId);
            m.put("title", title);
            m.put("learners", learners);
            m.put("watchMinutes", seconds / 60);
            courseRanking.add(m);
        }
        data.put("courseRanking", courseRanking);

        // 学员观看时长排行榜（按累计观看时长降序）
        List<Object[]> userRows = watchRecordRepository.statsByUser();
        Set<Long> userIds = userRows.stream().map(r -> (Long) r[0]).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> userRanking = new ArrayList<>();
        for (Object[] row : userRows) {
            Long userId = (Long) row[0];
            Long seconds = (Long) row[1];
            Long finished = ((Number) row[2]).longValue();
            User u = userMap.get(userId);
            Map<String, Object> m = new HashMap<>();
            m.put("userId", userId);
            String nickname = (u != null && u.getNickname() != null && !u.getNickname().isBlank())
                    ? u.getNickname() : ("学员" + userId);
            m.put("nickname", nickname);
            m.put("phone", u != null ? maskPhone(u.getPhone()) : "");
            m.put("watchMinutes", seconds / 60);
            m.put("finishedLessons", finished);
            userRanking.add(m);
        }
        data.put("userRanking", userRanking);

        return ApiResult.ok(data);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone == null ? "" : phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
