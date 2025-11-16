package com.atsResumeChecker.service;


import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileTextExtractorService {

    private final Tika tika = new Tika();

    public String extractText(MultipartFile file) {
        try {
            // Detects file type and extracts text
            return tika.parseToString(file.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from resume: " + e.getMessage(), e);
        }
    }
}

