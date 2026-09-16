package com.ielts.courses.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 写作模块实体 → 前端 Map 的统一映射。 */
@Component
public class WritingDtoMapper {

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> submission(WritingSubmission s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("userId", s.getUserId());
        m.put("taskPrompt", s.getTaskPrompt());
        m.put("content", s.getContent());
        m.put("wordCount", s.getWordCount());
        m.put("status", s.getStatus());
        m.put("aiError", s.getAiError());
        m.put("createdAt", s.getCreatedAt() == null ? null : s.getCreatedAt().toString());
        return m;
    }

    /** 列表行：不含正文/题目等大字段，附总分（已发布时）。 */
    public Map<String, Object> submissionRow(WritingSubmission s, WritingCorrection c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("wordCount", s.getWordCount());
        m.put("status", s.getStatus());
        m.put("createdAt", s.getCreatedAt() == null ? null : s.getCreatedAt().toString());
        m.put("promptPreview", preview(s.getTaskPrompt(), 120));
        m.put("essayPreview", preview(s.getContent(), 120));
        m.put("overall", c == null ? null : c.getOverall());
        return m;
    }

    public Map<String, Object> correction(WritingCorrection c, List<WritingAnnotation> annotations) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("submissionId", c.getSubmissionId());
        m.put("correctorId", c.getCorrectorId());

        Map<String, Object> scores = new LinkedHashMap<>();
        scores.put("tr", c.getScoreTr());
        scores.put("cc", c.getScoreCc());
        scores.put("lr", c.getScoreLr());
        scores.put("ga", c.getScoreGa());
        scores.put("overall", c.getOverall());
        m.put("scores", scores);

        Map<String, Object> comments = new LinkedHashMap<>();
        comments.put("tr", c.getCommentTr());
        comments.put("cc", c.getCommentCc());
        comments.put("lr", c.getCommentLr());
        comments.put("ga", c.getCommentGa());
        m.put("comments", comments);

        m.put("closingRemark", c.getClosingRemark());
        JsonNode ideas = parseIdeas(c.getModelIdeas());
        m.put("modelIdeas", ideas == null ? List.of() : ideas);
        m.put("modelEssay", c.getModelEssay());
        m.put("status", c.getStatus());
        m.put("createdAt", c.getCreatedAt() == null ? null : c.getCreatedAt().toString());
        m.put("annotations", annotations.stream().map(this::annotation).toList());
        return m;
    }

    public Map<String, Object> annotation(WritingAnnotation a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("seq", a.getSeq());
        m.put("category", a.getCategory());
        m.put("startPos", a.getStartPos());
        m.put("endPos", a.getEndPos());
        m.put("quote", a.getQuote());
        m.put("note", a.getNote());
        m.put("suggestion", a.getSuggestion());
        return m;
    }

    private JsonNode parseIdeas(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private static String preview(String s, int max) {
        if (s == null) return "";
        String t = s.trim().replaceAll("\\s+", " ");
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
