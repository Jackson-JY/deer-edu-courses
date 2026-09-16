package com.ielts.courses.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.ielts.courses.auth.LoginUser;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.CurrentUser;
import com.ielts.courses.user.User;
import com.ielts.courses.user.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 教师/管理员：审核 AI 草稿、编辑并发布批改。TEACHER、ADMIN 角色可访问。 */
@RestController
@RequestMapping("/api/writing/teacher")
public class WritingTeacherController {

    private final WritingSubmissionRepository submissionRepository;
    private final WritingCorrectionRepository correctionRepository;
    private final WritingAnnotationRepository annotationRepository;
    private final AiCorrectionService aiCorrectionService;
    private final WritingDtoMapper dtoMapper;
    private final UserRepository userRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public WritingTeacherController(WritingSubmissionRepository submissionRepository,
                                    WritingCorrectionRepository correctionRepository,
                                    WritingAnnotationRepository annotationRepository,
                                    AiCorrectionService aiCorrectionService,
                                    WritingDtoMapper dtoMapper,
                                    UserRepository userRepository) {
        this.submissionRepository = submissionRepository;
        this.correctionRepository = correctionRepository;
        this.annotationRepository = annotationRepository;
        this.aiCorrectionService = aiCorrectionService;
        this.dtoMapper = dtoMapper;
        this.userRepository = userRepository;
    }

    /** 提交列表：默认返回未批改的全部，可按状态过滤。 */
    @GetMapping("/submissions")
    public ApiResult<List<Map<String, Object>>> list(
            @RequestParam(required = false) String status) {
        List<WritingSubmission> list = (status == null || status.isBlank())
                ? submissionRepository.findByStatusNotOrderByIdDesc("CORRECTED")
                : submissionRepository.findByStatusOrderByIdAsc(status.trim());

        Set<Long> userIds = list.stream().map(WritingSubmission::getUserId).collect(Collectors.toSet());
        Map<Long, String> nameMap = new LinkedHashMap<>();
        for (User u : userRepository.findAllById(userIds)) {
            nameMap.put(u.getId(), u.getNickname() != null && !u.getNickname().isBlank()
                    ? u.getNickname() : maskPhone(u.getPhone()));
        }

        List<Map<String, Object>> rows = list.stream().map(s -> {
            WritingCorrection c = correctionRepository.findBySubmissionId(s.getId()).orElse(null);
            Map<String, Object> row = dtoMapper.submissionRow(s, c);
            row.put("userId", s.getUserId());
            row.put("studentName", nameMap.getOrDefault(s.getUserId(), "UID" + s.getUserId()));
            return row;
        }).toList();
        return ApiResult.ok(rows);
    }

    /** 审核详情：提交 + 草稿批改（DRAFT 也可见）。 */
    @GetMapping("/submissions/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable long id) {
        WritingSubmission submission = submissionRepository.findById(id).orElse(null);
        if (submission == null) {
            return ApiResult.error(40404, "作文不存在", "NOT_FOUND");
        }
        Map<String, Object> data = new HashMap2();
        data.putAll(dtoMapper.submission(submission));
        data.put("studentName", resolveName(submission.getUserId()));
        correctionRepository.findBySubmissionId(id).ifPresent(c -> data.put("correction",
                dtoMapper.correction(c, annotationRepository.findByCorrectionIdOrderBySeqAsc(c.getId()))));
        return ApiResult.ok(data);
    }

    /** 保存教师编辑后的草稿（不发布）。 */
    @PutMapping("/corrections/{id}")
    @Transactional
    public ApiResult<Map<String, Object>> save(@PathVariable long id,
                                               @RequestBody JsonNode payload) {
        WritingCorrection correction = correctionRepository.findById(id).orElse(null);
        if (correction == null) {
            return ApiResult.error(40404, "批改不存在", "NOT_FOUND");
        }
        applyPayload(correction, payload);
        correction = correctionRepository.save(correction);
        replaceAnnotations(correction, payload.path("annotations"));

        WritingSubmission submission = submissionRepository.findById(correction.getSubmissionId()).orElseThrow();
        if (!"CORRECTED".equals(submission.getStatus())) {
            submission.setStatus("AWAITING_REVIEW");
            submission.setAiError(null);
            submissionRepository.save(submission);
        }
        return ApiResult.ok(dtoMapper.correction(correction,
                annotationRepository.findByCorrectionIdOrderBySeqAsc(correction.getId())));
    }

    /** 发布批改：学生端立即可见。 */
    @PostMapping("/corrections/{id}/publish")
    @Transactional
    public ApiResult<Map<String, Object>> publish(@CurrentUser LoginUser user,
                                                  @PathVariable long id) {
        WritingCorrection correction = correctionRepository.findById(id).orElse(null);
        if (correction == null) {
            return ApiResult.error(40404, "批改不存在", "NOT_FOUND");
        }
        applyPayload(correction, null);
        correction.setStatus("PUBLISHED");
        correction.setCorrectorId(user.userId());
        correctionRepository.save(correction);

        WritingSubmission submission = submissionRepository.findById(correction.getSubmissionId()).orElseThrow();
        submission.setStatus("CORRECTED");
        submissionRepository.save(submission);
        return ApiResult.ok(dtoMapper.correction(correction,
                annotationRepository.findByCorrectionIdOrderBySeqAsc(correction.getId())));
    }

    /** 重新生成 AI 草稿（已发布的不允许）。 */
    @PostMapping("/submissions/{id}/regenerate")
    public ApiResult<Void> regenerate(@PathVariable long id) {
        WritingSubmission submission = submissionRepository.findById(id).orElse(null);
        if (submission == null) {
            return ApiResult.error(40404, "作文不存在", "NOT_FOUND");
        }
        if ("CORRECTED".equals(submission.getStatus())) {
            return ApiResult.error(40001, "已发布的批改不能重新生成", null);
        }
        aiCorrectionService.generateDraft(id);
        return ApiResult.ok();
    }

    /** 新建空白批改草稿（AI 不可用时教师手工批改；已有草稿则直接返回）。 */
    @PostMapping("/submissions/{id}/correction")
    @Transactional
    public ApiResult<Map<String, Object>> createBlank(@PathVariable long id) {
        WritingSubmission submission = submissionRepository.findById(id).orElse(null);
        if (submission == null) {
            return ApiResult.error(40404, "作文不存在", "NOT_FOUND");
        }
        WritingCorrection correction = correctionRepository.findBySubmissionId(id).orElse(null);
        if (correction == null) {
            correction = new WritingCorrection();
            correction.setSubmissionId(id);
            BigDecimal d = new BigDecimal("5.0");
            correction.setScoreTr(d);
            correction.setScoreCc(d);
            correction.setScoreLr(d);
            correction.setScoreGa(d);
            correction.setOverall(d);
            correction.setModelIdeas("[]");
            correction.setStatus("DRAFT");
            correction = correctionRepository.save(correction);

            submission.setStatus("AWAITING_REVIEW");
            submissionRepository.save(submission);
        }
        return ApiResult.ok(dtoMapper.correction(correction,
                annotationRepository.findByCorrectionIdOrderBySeqAsc(correction.getId())));
    }

    // ===== 内部方法 =====

    private void applyPayload(WritingCorrection correction, JsonNode payload) {
        if (payload == null || !payload.has("scores")) {
            // 发布前确保总分已算
            if (correction.getOverall() == null && correction.getScoreTr() != null) {
                correction.setOverall(overallBand(correction.getScoreTr(), correction.getScoreCc(),
                        correction.getScoreLr(), correction.getScoreGa()));
            }
            return;
        }
        JsonNode scores = payload.path("scores");
        BigDecimal tr = band(scores, "tr");
        BigDecimal cc = band(scores, "cc");
        BigDecimal lr = band(scores, "lr");
        BigDecimal ga = band(scores, "ga");
        correction.setScoreTr(tr);
        correction.setScoreCc(cc);
        correction.setScoreLr(lr);
        correction.setScoreGa(ga);
        correction.setOverall(overallBand(tr, cc, lr, ga));

        JsonNode comments = payload.path("comments");
        correction.setCommentTr(truncate(comments.path("tr").asText(""), 990));
        correction.setCommentCc(truncate(comments.path("cc").asText(""), 990));
        correction.setCommentLr(truncate(comments.path("lr").asText(""), 990));
        correction.setCommentGa(truncate(comments.path("ga").asText(""), 990));
        correction.setClosingRemark(truncate(payload.path("closingRemark").asText(""), 490));
        correction.setModelEssay(truncate(payload.path("modelEssay").asText(""), 20000));

        JsonNode ideas = payload.path("modelIdeas");
        if (ideas.isArray()) {
            ArrayNode clean = mapper.createArrayNode();
            for (JsonNode col : ideas) {
                ArrayNode points = mapper.createArrayNode();
                JsonNode rawPoints = col.path("points");
                if (rawPoints.isArray()) {
                    for (JsonNode p : rawPoints) {
                        String pt = p.asText("").trim();
                        if (!pt.isEmpty()) points.add(truncate(pt, 200));
                    }
                }
                if (points.size() > 0) {
                    clean.addObject()
                            .put("heading", truncate(col.path("heading").asText(""), 60))
                            .set("points", points);
                }
            }
            try {
                correction.setModelIdeas(mapper.writeValueAsString(clean));
            } catch (Exception ignored) {
                // 保留原值
            }
        }
        correction.setStatus("DRAFT");
    }

    private void replaceAnnotations(WritingCorrection correction, JsonNode arr) {
        if (arr == null || !arr.isArray()) return;
        WritingSubmission submission = submissionRepository.findById(correction.getSubmissionId()).orElseThrow();
        int essayLen = submission.getContent().length();

        List<JsonNode> valid = new ArrayList<>();
        for (JsonNode an : arr) {
            int start = an.path("startPos").asInt(-1);
            int end = an.path("endPos").asInt(-1);
            if (start < 0 || end <= start || end > essayLen) continue;
            String category = an.path("category").asText("G").trim().toUpperCase();
            if (!category.equals("G") && !category.equals("V") && !category.equals("C") && !category.equals("T")) {
                category = "G";
            }
            valid.add(an);
        }
        valid.sort((a, b) -> Integer.compare(a.path("startPos").asInt(), b.path("startPos").asInt()));

        annotationRepository.deleteByCorrectionId(correction.getId());
        annotationRepository.flush();

        int seq = 1;
        for (JsonNode an : valid) {
            int start = an.path("startPos").asInt();
            int end = an.path("endPos").asInt();
            WritingAnnotation entity = new WritingAnnotation();
            entity.setCorrectionId(correction.getId());
            entity.setSeq(seq++);
            entity.setCategory(an.path("category").asText("G"));
            entity.setStartPos(start);
            entity.setEndPos(end);
            String quote = an.path("quote").asText("");
            entity.setQuote(quote.isBlank() ? submission.getContent().substring(start, end) : truncate(quote, 490));
            entity.setNote(truncate(an.path("note").asText(""), 1990));
            entity.setSuggestion(truncate(an.path("suggestion").asText(""), 490));
            annotationRepository.save(entity);
        }
    }

    private String resolveName(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getNickname() != null && !u.getNickname().isBlank()
                        ? u.getNickname() : maskPhone(u.getPhone()))
                .orElse("UID" + userId);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone == null ? "" : phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private static BigDecimal band(JsonNode scores, String field) {
        double v = scores.path(field).asDouble(-1);
        if (v < 0) v = 5;
        v = Math.max(4, Math.min(9, v));
        return new BigDecimal(Math.round(v * 2) / 2.0).setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal overallBand(BigDecimal tr, BigDecimal cc, BigDecimal lr, BigDecimal ga) {
        BigDecimal avg = tr.add(cc).add(lr).add(ga)
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
        return avg.multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    /** 仅为缩短声明的别名。 */
    private static class HashMap2 extends LinkedHashMap<String, Object> {
    }
}
