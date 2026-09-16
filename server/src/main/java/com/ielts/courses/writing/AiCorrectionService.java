package com.ielts.courses.writing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 写作批改：调用 LLM 生成结构化批改 JSON，落库委托给 {@link WritingDraftPersister}（独立事务）。
 * AI 失败只更新提交状态为 FAILED，不抛出到调用方（异步触发）。
 */
@Service
public class AiCorrectionService {

    private static final Logger log = LoggerFactory.getLogger(AiCorrectionService.class);

    private final LlmClient llmClient;
    private final WritingSubmissionRepository submissionRepository;
    private final WritingDraftPersister draftPersister;

    public AiCorrectionService(LlmClient llmClient,
                               WritingSubmissionRepository submissionRepository,
                               WritingDraftPersister draftPersister) {
        this.llmClient = llmClient;
        this.submissionRepository = submissionRepository;
        this.draftPersister = draftPersister;
    }

    /** 异步生成 AI 草稿（由提交/重新生成接口触发）。 */
    @Async
    public void generateDraft(Long submissionId) {
        WritingSubmission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) return;
        submission.setStatus("AI_DRAFTING");
        submission.setAiError(null);
        submissionRepository.save(submission);
        try {
            if (!llmClient.configured()) {
                throw new IllegalStateException("LLM API Key 未配置");
            }
            String rawJson = llmClient.chatJson(buildMessages(submission));
            // 经独立 Bean 调用，@Transactional 才会生效；LLM 调用在事务之外
            draftPersister.persistDraft(submission, rawJson);
        } catch (Exception e) {
            log.warn("AI 批改生成失败 submission={}: {}", submissionId, e.getMessage());
            WritingSubmission s = submissionRepository.findById(submissionId).orElse(null);
            if (s != null) {
                s.setStatus("FAILED");
                s.setAiError(truncate(e.getMessage(), 480));
                submissionRepository.save(s);
            }
        }
    }

    private List<Map<String, String>> buildMessages(WritingSubmission s) {
        String system = """
                You are a senior IELTS examiner with years of experience marking Writing Task 2 essays. \
                You grade strictly according to the official IELTS Writing Task 2 public band descriptors on four criteria: \
                Task Response (TR), Coherence and Cohesion (CC), Lexical Resource (LR), Grammatical Range and Accuracy (GA).

                You must output ONLY one JSON object (no markdown fences, no commentary) with this exact shape:
                {
                  "scores": {"TR": <number>, "CC": <number>, "LR": <number>, "GA": <number>},
                  "criterionComments": {"TR": "<one sentence in Chinese>", "CC": "...", "LR": "...", "GA": "..."},
                  "annotations": [
                    {"quote": "<EXACT verbatim substring copied from the student's essay>",
                     "category": "G|V|C|T",
                     "note": "<concise explanation in English>",
                     "suggestion": "<corrected word/phrase/sentence in English>"}
                  ],
                  "modelIdeas": [
                    {"heading": "<column heading in English, adapted to the question, e.g. NEGATIVE / POSITIVE, PROBLEMS / SOLUTIONS>",
                     "points": ["<idea phrase>", "..."]}
                  ],
                  "modelEssay": "<a Band 8 model essay, 260-300 words, plain text with paragraph breaks>",
                  "closingRemark": "<one short warm encouraging sentence in Chinese>"
                }

                Rules:
                - Band scores use half bands (5, 5.5, 6 ... 9). Be rigorous; only fully developed, error-free answers deserve 7+.
                - Provide 8 to 16 annotations covering the most important issues. Each "quote" MUST be an exact substring \
                of the essay, copied character-for-character including the original spacing and punctuation; do not paraphrase it.
                - Categories: G = grammar, V = vocabulary/collocation/word choice/formality, C = cohesion/logical linking/referencing/paragraphing, \
                T = task response/position/idea development/clarity.
                - Each note briefly explains the problem; suggestion gives the concrete correction.
                - criterionComments are in Chinese: first affirm what the writer did well, then point out the key growth area; \
                be gentle and encouraging, never harsh.
                - closingRemark is a brief, warm Chinese blessing.
                - modelEssay must fully answer the given prompt at Band 8 level. modelIdeas contains exactly two columns, \
                each with 3 or 4 short idea phrases derived from the model essay.
                - If the essay is clearly under 250 words, reflect that in the TR score and note it in an annotation.""";

        String userPrompt = "IELTS Writing Task 2 prompt:\n" + s.getTaskPrompt().trim()
                + "\n\nStudent essay (" + s.getWordCount() + " words):\n" + s.getContent().trim();

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        messages.add(Map.of("role", "user", "content", userPrompt));
        return messages;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
