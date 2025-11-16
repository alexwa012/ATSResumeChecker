package com.atsResumeChecker.service;

import com.atsResumeChecker.config.PerplexityClient;
import com.atsResumeChecker.dto.ResumeAnalysisResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResumeAnalysisService {

    @Autowired
    private PerplexityClient perplexityClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumeAnalysisResponse analyze(String resumeText, String targetRole) {

        String systemPrompt = """
            You are an expert ATS (Applicant Tracking System) and resume evaluator.
            Your job is to analyze resumes and return structured JSON.

            RULES (IMPORTANT):
            - Respond with ONLY valid JSON. DO NOT add any explanation or text outside JSON.
            - DO NOT wrap JSON in backticks or markdown code blocks.
            - JSON keys MUST be exactly: atsScore, missingKeywords, strengths, improvements, summary.
            - atsScore: integer between 0 and 100.
            - missingKeywords: array of strings.
            - strengths: array of strings.
            - improvements: array of strings.
            - summary: string (2-3 sentence summary).
            """;

        String userPrompt = """
            Analyze the following resume for the target role: %s

            Resume content:
            %s

            Respond ONLY with JSON in this exact structure (keys and types must match):

            {
              "atsScore": 85,
              "missingKeywords": ["Spring Security", "Kafka"],
              "strengths": [
                "Strong experience in Java and Spring Boot",
                "Hands-on microservices and REST API development"
              ],
              "improvements": [
                "Add more quantifiable achievements",
                "Highlight experience with cloud platforms (AWS, Azure, etc.)"
              ],
              "summary": "Overall, this resume is ..."
            }
            """.formatted(targetRole, resumeText);

        try {
            String raw = perplexityClient.callPerplexity(systemPrompt, userPrompt);

            // Debug: see exactly what the model returns
            System.out.println("Perplexity raw content:\n" + raw);

            // Clean up markdown fences if present
            String json = cleanJson(raw);

            // Either map directly, or (safer) parse as Map and fill DTO
            Map<String, Object> map = objectMapper.readValue(
                    json,
                    new TypeReference<Map<String, Object>>() {}
            );

            return mapToResponse(map);

        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze resume: " + e.getMessage(), e);
        }
    }

    private String cleanJson(String content) {
        String trimmed = content.trim();
        // Remove ```json ... ``` or ``` ... ```
        if (trimmed.startsWith("```")) {
            int firstNewLine = trimmed.indexOf('\n');
            if (firstNewLine != -1) {
                trimmed = trimmed.substring(firstNewLine + 1);
            }
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence != -1) {
                trimmed = trimmed.substring(0, lastFence);
            }
        }
        return trimmed.trim();
    }

    private ResumeAnalysisResponse mapToResponse(Map<String, Object> map) {
        ResumeAnalysisResponse resp = new ResumeAnalysisResponse();

        // atsScore
        Object ats = map.get("atsScore");
        if (ats instanceof Number) {
            resp.setAtsScore(((Number) ats).intValue());
        }

        // helper to cast list
        resp.setMissingKeywords(asStringList(map.get("missingKeywords")));
        resp.setStrengths(asStringList(map.get("strengths")));
        resp.setImprovements(asStringList(map.get("improvements")));

        Object summary = map.get("summary");
        if (summary != null) {
            resp.setSummary(summary.toString());
        }

        return resp;
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object obj) {
        if (obj == null) return null;
        List<String> result = new ArrayList<>();
        if (obj instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) result.add(o.toString());
            }
            return result;
        }
        return null;
    }
}
