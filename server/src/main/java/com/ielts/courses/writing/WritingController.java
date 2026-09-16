package com.ielts.courses.writing;

import com.ielts.courses.auth.LoginUser;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 学生写作：提交作文、查看我的作文与已发布批改。 */
@RestController
@RequestMapping("/api/writing")
public class WritingController {

    private final WritingSubmissionRepository submissionRepository;
    private final WritingCorrectionRepository correctionRepository;
    private final WritingAnnotationRepository annotationRepository;
    private final AiCorrectionService aiCorrectionService;
    private final WritingQuotaService quotaService;
    private final WritingDtoMapper dtoMapper;

    public WritingController(WritingSubmissionRepository submissionRepository,
                             WritingCorrectionRepository correctionRepository,
                             WritingAnnotationRepository annotationRepository,
                             AiCorrectionService aiCorrectionService,
                             WritingQuotaService quotaService,
                             WritingDtoMapper dtoMapper) {
        this.submissionRepository = submissionRepository;
        this.correctionRepository = correctionRepository;
        this.annotationRepository = annotationRepository;
        this.aiCorrectionService = aiCorrectionService;
        this.quotaService = quotaService;
        this.dtoMapper = dtoMapper;
    }

    public record SubmitRequest(
            @NotBlank @Size(max = 5000) String taskPrompt,
            @NotBlank @Size(max = 12000) String content) {
    }

    /** 我的写作额度：是否开通 + 本月已用/剩余。 */
    @GetMapping("/quota")
    public ApiResult<Map<String, Object>> quota(@CurrentUser LoginUser user) {
        return ApiResult.ok(quotaService.quota(user.userId()));
    }

    /** 提交作文（题目一并粘贴），需已开通权限且本月额度未用尽；随后异步生成 AI 草稿。 */
    @PostMapping("/submissions")
    public ApiResult<Map<String, Object>> submit(@CurrentUser LoginUser user,
                                                 @Valid @RequestBody SubmitRequest req) {
        quotaService.checkCanSubmit(user.userId());

        WritingSubmission submission = new WritingSubmission();
        submission.setUserId(user.userId());
        submission.setTaskPrompt(req.taskPrompt().trim());
        submission.setContent(req.content().trim());
        submission.setWordCount(countWords(req.content()));
        submission.setStatus("PENDING");
        submission = submissionRepository.save(submission);

        aiCorrectionService.generateDraft(submission.getId());
        return ApiResult.ok(dtoMapper.submission(submission));
    }

    /** 我的作文列表（含批改状态/总分）。 */
    @GetMapping("/submissions/mine")
    public ApiResult<List<Map<String, Object>>> mine(@CurrentUser LoginUser user) {
        List<WritingSubmission> list = submissionRepository.findByUserIdOrderByIdDesc(user.userId());
        List<Map<String, Object>> rows = list.stream().map(s -> {
            WritingCorrection c = correctionRepository.findBySubmissionId(s.getId()).orElse(null);
            return dtoMapper.submissionRow(s, c);
        }).toList();
        return ApiResult.ok(rows);
    }

    /** 作文详情：本人可看；批改仅在已发布后返回，否则返回状态供轮询。 */
    @GetMapping("/submissions/{id}")
    public ApiResult<Map<String, Object>> detail(@CurrentUser LoginUser user,
                                                 @PathVariable long id) {
        WritingSubmission submission = submissionRepository.findById(id).orElse(null);
        if (submission == null || !submission.getUserId().equals(user.userId())) {
            return ApiResult.error(40404, "作文不存在", "NOT_FOUND");
        }
        Map<String, Object> data = new HashMap<>(dtoMapper.submission(submission));
        correctionRepository.findBySubmissionId(id).ifPresent(c -> {
            if ("PUBLISHED".equals(c.getStatus())) {
                List<WritingAnnotation> annotations =
                        annotationRepository.findByCorrectionIdOrderBySeqAsc(c.getId());
                data.put("correction", dtoMapper.correction(c, annotations));
            }
        });
        return ApiResult.ok(data);
    }

    static int countWords(String content) {
        if (content == null || content.isBlank()) return 0;
        return (int) java.util.Arrays.stream(content.trim().split("\\s+"))
                .filter(w -> !w.isEmpty())
                .count();
    }
}
