package com.atsResumeChecker.controller;

import com.atsResumeChecker.dto.ResumeAnalysisResponse;
import com.atsResumeChecker.service.FileTextExtractorService;
import com.atsResumeChecker.service.ResumeAnalysisService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resume")
@Validated
@RequiredArgsConstructor
public class ResumeController {

    @Autowired
    private ResumeAnalysisService resumeAnalysisService;
    @Autowired
    private FileTextExtractorService fileTextExtractorService;

    @PostMapping(
            value = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResumeAnalysisResponse> analyze(
            @RequestPart("file") MultipartFile file,
            @RequestPart("targetRole") @NotBlank String targetRole
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String resumeText = fileTextExtractorService.extractText(file);
        ResumeAnalysisResponse response =
                resumeAnalysisService.analyze(resumeText, targetRole);

        return ResponseEntity.ok(response);
    }
}
