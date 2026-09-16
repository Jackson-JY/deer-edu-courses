package com.ielts.courses.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 草稿落库：解析批改 JSON、定位批注字符偏移、替换旧批注。
 * 独立成 Bean 以保证 @Transactional 经代理生效（不能由 AiCorrectionService 自调用）。
 * LLM 调用发生在本事务之外，避免长时间占用数据库连接。
 */
@Service
public class WritingDraftPersister {

    private static final Logger log = LoggerFactory.getLogger(WritingDraftPersister.class);

    private final WritingCorrectionRepository correctionRepository;
    private final WritingAnnotationRepository annotationRepository;
    private final WritingSubmissionRepository submissionRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public WritingDraftPersister(WritingCorrectionRepository correctionRepository,
                                 WritingAnnotationRepository annotationRepository,
                                 WritingSubmissionRepository submissionRepository) {
        this.correctionRepository = correctionRepository;
        this.annotationRepository = annotationRepository;
        this.submissionRepository = submissionRepository;
    }

    /** 解析 AI JSON、定位批注偏移并持久化（一个事务内完成删除+写入）。 */
    @Transactional
    public void persistDraft(WritingSubmission submission, String rawJson) throws Exception {
        JsonNode root = mapper.readTree(extractJson(rawJson));

        WritingCorrection correction = correctionRepository.findBySubmissionId(submission.getId())
                .orElseGet(WritingCorrection::new);
        correction.setSubmissionId(submission.getId());

        JsonNode scores = root.path("scores");
        BigDecimal tr = band(scores, "TR");
        BigDecimal cc = band(scores, "CC");
        BigDecimal lr = band(scores, "LR");
        BigDecimal ga = band(scores, "GA");
        correction.setScoreTr(tr);
        correction.setScoreCc(cc);
        correction.setScoreLr(lr);
        correction.setScoreGa(ga);
        correction.setOverall(overallBand(tr, cc, lr, ga));

        JsonNode comments = root.path("criterionComments");
        correction.setCommentTr(text(comments, "TR", 990));
        correction.setCommentCc(text(comments, "CC", 990));
        correction.setCommentLr(text(comments, "LR", 990));
        correction.setCommentGa(text(comments, "GA", 990));
        correction.setClosingRemark(truncate(root.path("closingRemark").asText(""), 490));
        correction.setModelEssay(truncate(root.path("modelEssay").asText(""), 20000));

        JsonNode ideas = root.path("modelIdeas");
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
            correction.setModelIdeas(mapper.writeValueAsString(clean));
        }

        correction.setStatus("DRAFT");
        correction = correctionRepository.save(correction);

        // 替换旧批注（删除与新写入必须在同一事务内）
        annotationRepository.deleteByCorrectionId(correction.getId());
        annotationRepository.flush();

        String essay = submission.getContent();
        List<int[]> usedRanges = new ArrayList<>();
        List<Located> locatedList = new ArrayList<>();
        JsonNode arr = root.path("annotations");
        if (arr.isArray()) {
            for (JsonNode an : arr) {
                String quote = an.path("quote").asText("").trim();
                if (quote.isEmpty()) continue;
                int[] range = locateQuote(essay, quote, usedRanges);
                if (range == null) {
                    log.debug("批注 quote 无法定位，已跳过: {}", truncate(quote, 60));
                    continue;
                }
                String category = an.path("category").asText("G").trim().toUpperCase();
                if (!category.equals("G") && !category.equals("V") && !category.equals("C") && !category.equals("T")) {
                    category = "G";
                }
                Located located = new Located();
                located.start = range[0];
                located.end = range[1];
                located.category = category;
                located.quote = essay.substring(range[0], range[1]);
                located.note = truncate(an.path("note").asText(""), 1990);
                located.suggestion = truncate(an.path("suggestion").asText(""), 490);
                locatedList.add(located);
                usedRanges.add(range);
            }
        }

        // 按原文出现顺序编号
        locatedList.sort((a, b) -> Integer.compare(a.start, b.start));
        int seq = 1;
        for (Located l : locatedList) {
            WritingAnnotation entity = new WritingAnnotation();
            entity.setCorrectionId(correction.getId());
            entity.setSeq(seq++);
            entity.setCategory(l.category);
            entity.setStartPos(l.start);
            entity.setEndPos(l.end);
            entity.setQuote(l.quote);
            entity.setNote(l.note);
            entity.setSuggestion(l.suggestion);
            annotationRepository.save(entity);
        }

        submission.setStatus("AWAITING_REVIEW");
        submission.setAiError(null);
        submissionRepository.save(submission);
    }

    /** 在原文中定位 quote：先精确匹配，再空白宽松匹配；避开已占用区间。 */
    private int[] locateQuote(String essay, String quote, List<int[]> usedRanges) {
        int[] exact = pickMatch(essay, quote, false, usedRanges);
        if (exact != null) return exact;
        return pickMatch(essay, quote, true, usedRanges);
    }

    private int[] pickMatch(String essay, String quote, boolean looseWhitespace, List<int[]> usedRanges) {
        List<int[]> candidates = new ArrayList<>();
        if (!looseWhitespace) {
            int from = 0;
            while (true) {
                int idx = essay.indexOf(quote, from);
                if (idx < 0) break;
                candidates.add(new int[]{idx, idx + quote.length()});
                from = idx + 1;
            }
        } else {
            // 将 quote 内的空白转为 \s+（Pattern.quote 的 \Q..\E 之间插入）
            String quoted = Pattern.quote(quote).replaceAll("(?:\\\\s\\+|\\s)+", "\\\\E\\\\s+\\\\Q");
            Matcher m = Pattern.compile(quoted, Pattern.DOTALL).matcher(essay);
            while (m.find()) {
                candidates.add(new int[]{m.start(), m.end()});
            }
        }
        if (candidates.isEmpty()) return null;
        for (int[] c : candidates) {
            boolean overlaps = false;
            for (int[] used : usedRanges) {
                if (c[0] < used[1] && used[0] < c[1]) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) return c;
        }
        return candidates.get(0);
    }

    private static String extractJson(String s) {
        String t = s == null ? "" : s.trim();
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) return t.substring(start, end + 1);
        return t;
    }

    private static BigDecimal band(JsonNode scores, String field) {
        double v = scores.path(field).asDouble(-1);
        if (v < 0) v = 5;
        v = Math.max(4, Math.min(9, v));
        return new BigDecimal(Math.round(v * 2) / 2.0).setScale(1, RoundingMode.HALF_UP);
    }

    /** 雅思总分：四项均值四舍五入到最近 0.5。 */
    private static BigDecimal overallBand(BigDecimal tr, BigDecimal cc, BigDecimal lr, BigDecimal ga) {
        BigDecimal avg = tr.add(cc).add(lr).add(ga)
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
        return avg.multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
    }

    private static String text(JsonNode node, String field, int max) {
        return truncate(node.path(field).asText(""), max);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private static class Located {
        int start;
        int end;
        String category;
        String quote;
        String note;
        String suggestion;
    }
}
