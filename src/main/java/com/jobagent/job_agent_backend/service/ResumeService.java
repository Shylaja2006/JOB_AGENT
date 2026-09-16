package com.jobagent.job_agent_backend.service;

import com.jobagent.job_agent_backend.dto.ResumeRequest;
import com.jobagent.job_agent_backend.dto.ResumeResponse;
import com.jobagent.job_agent_backend.dto.ResumeUploadResponse;
import com.jobagent.job_agent_backend.entity.Resume;
import com.jobagent.job_agent_backend.entity.User;
import com.jobagent.job_agent_backend.repository.ResumeRepository;
import com.jobagent.job_agent_backend.repository.UserRepository;
import org.apache.tika.Tika;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public class ResumeService {

    private static final Logger logger =
            LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;

    public ResumeService(
            ResumeRepository resumeRepository,
            UserRepository userRepository) {

        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
    }

    public boolean resumeAlreadyExists() {

        return findCurrentUser()
                .map(resumeRepository::existsByUser)
                .orElse(false);
    }

    public Optional<ResumeResponse> createResume(
            ResumeRequest request) {

        User user = findCurrentUser().orElse(null);

        if (user == null
                || resumeRepository.existsByUser(user)) {

            return Optional.empty();
        }

        Resume resume = new Resume();

        resume.setUser(user);

        applyRequest(resume, request);

        return Optional.of(
                toResponse(
                        resumeRepository.save(resume)
                )
        );
    }

    public Optional<ResumeResponse> getResume() {

        return findCurrentUserResume()
                .map(this::toResponse);
    }

    public Optional<ResumeResponse> updateResume(
            ResumeRequest request) {

        return findCurrentUserResume()
                .map(resume -> {

                    applyRequest(resume, request);

                    return toResponse(
                            resumeRepository.save(resume)
                    );
                });
    }

    public boolean deleteResume() {

        Optional<Resume> resume =
                findCurrentUserResume();

        if (resume.isEmpty()) {
            return false;
        }

        resumeRepository.delete(resume.get());

        return true;
    }

    // =========================
    // RESUME FILE UPLOAD
    // =========================

    public Optional<ResumeUploadResponse> uploadResume(
            MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }

        String lowerName =
                fileName.toLowerCase();

        if (!(lowerName.endsWith(".pdf")
                || lowerName.endsWith(".doc")
                || lowerName.endsWith(".docx"))) {

            return Optional.empty();
        }

        try {

            Tika tika = new Tika();

            String extractedText =
                    tika.parseToString(
                            file.getInputStream()
                    );

            if (extractedText == null
                    || extractedText.isBlank()) {

                return Optional.empty();
            }

            // Extract resume information
            String title =
                    extractTitle(extractedText);

            String skills =
                    extractSkills(extractedText);

            String summary =
                    extractSummary(extractedText);

            String experience =
                    extractSection(
                            extractedText,
                            "experience",
                            "education"
                    );

            String education =
                    extractSection(
                            extractedText,
                            "education",
                            null
                    );

            ResumeRequest request =
                    new ResumeRequest();

            request.setTitle(
                    title.isBlank()
                            ? "My Resume"
                            : title
            );

            request.setSummary(summary);
            request.setSkills(skills);
            request.setExperience(experience);
            request.setEducation(education);

            ResumeResponse savedResume;

            if (resumeAlreadyExists()) {

                savedResume =
                        updateResume(request)
                                .orElse(null);

            } else {

                savedResume =
                        createResume(request)
                                .orElse(null);
            }

            if (savedResume == null) {
                return Optional.empty();
            }

            return Optional.of(
                    new ResumeUploadResponse(
                            fileName,
                            "Resume uploaded and processed successfully",
                            savedResume
                    )
            );

        } catch (Exception e) {

            logger.error("Resume upload processing failed: {}", e.getMessage());

            return Optional.empty();
        }
    }

    // =========================
    // CURRENT USER
    // =========================

    private Optional<User> findCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof String email)) {

            return Optional.empty();
        }

        return userRepository.findByEmail(email);
    }

    // =========================
    // CURRENT USER RESUME
    // =========================

    private Optional<Resume> findCurrentUserResume() {

        return findCurrentUser()
                .flatMap(resumeRepository::findByUser);
    }

    // =========================
    // APPLY REQUEST
    // =========================

    private void applyRequest(
            Resume resume,
            ResumeRequest request) {

        resume.setTitle(
                request.getTitle()
        );

        resume.setSummary(
                request.getSummary()
        );

        resume.setSkills(
                request.getSkills()
        );

        resume.setExperience(
                request.getExperience()
        );

        resume.setEducation(
                request.getEducation()
        );
    }

    // =========================
    // RESPONSE
    // =========================

    private ResumeResponse toResponse(
            Resume resume) {

        return new ResumeResponse(
                resume.getId(),
                resume.getTitle(),
                resume.getSummary(),
                resume.getSkills(),
                resume.getExperience(),
                resume.getEducation(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }

    // =========================
    // EXTRACT TITLE
    // =========================

    private String extractTitle(String text) {

        String[] lines =
                text.split("\\r?\\n");

        for (String line : lines) {

            String cleaned =
                    line.trim();

            if (!cleaned.isBlank()
                    && cleaned.length() <= 150) {

                return cleaned;
            }
        }

        return "My Resume";
    }

    // =========================
    // EXTRACT SKILLS
    // =========================

    private String extractSkills(String text) {

        String lower =
                text.toLowerCase();

        String[] knownSkills = {

                "java",
                "spring boot",
                "spring",
                "react",
                "javascript",
                "typescript",
                "python",
                "sql",
                "mysql",
                "postgresql",
                "mongodb",
                "html",
                "css",
                "git",
                "github",
                "docker",
                "aws",
                "azure",
                "rest api",
                "microservices",
                "hibernate",
                "jpa"
        };

        StringBuilder result =
                new StringBuilder();

        for (String skill : knownSkills) {

            if (lower.contains(skill)) {

                if (!result.isEmpty()) {
                    result.append(", ");
                }

                result.append(skill);
            }
        }

        return result.toString();
    }

    // =========================
    // EXTRACT SUMMARY
    // =========================

    private String extractSummary(String text) {

        return extractSection(
                text,
                "summary",
                "experience"
        );
    }

    // =========================
    // EXTRACT SECTION
    // =========================

    private String extractSection(
            String text,
            String startHeading,
            String endHeading) {

        String lowerText =
                text.toLowerCase();

        int start =
                lowerText.indexOf(
                        startHeading.toLowerCase()
                );

        if (start == -1) {
            return "";
        }

        start += startHeading.length();

        int end = text.length();

        if (endHeading != null) {

            int found =
                    lowerText.indexOf(
                            endHeading.toLowerCase(),
                            start
                    );

            if (found != -1) {
                end = found;
            }
        }

        String result =
                text.substring(start, end)
                        .trim();

        if (result.length() > 5000) {

            result =
                    result.substring(0, 5000);
        }

        return result;
    }
}