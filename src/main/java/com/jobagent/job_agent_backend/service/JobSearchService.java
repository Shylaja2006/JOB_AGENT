package com.jobagent.job_agent_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.job_agent_backend.client.AdzunaJobClient;
import com.jobagent.job_agent_backend.dto.JobSearchResponse;
import com.jobagent.job_agent_backend.entity.Resume;
import com.jobagent.job_agent_backend.entity.User;
import com.jobagent.job_agent_backend.repository.ResumeRepository;
import com.jobagent.job_agent_backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class JobSearchService {

    private static final Logger logger =
            LoggerFactory.getLogger(JobSearchService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AdzunaJobClient adzunaJobClient;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;

    private static final int MAX_RESUME_SKILL_BONUS = 50;
    private static final Pattern RESUME_SKILL_SPLIT =
            Pattern.compile("[,;|\\n]+");

    private static final Pattern EXPERIENCED_ROLE_PATTERN = Pattern.compile(
            "\\b(?:3|4|5|6|7|8|9|10|[1-9]\\d)(?:\\.\\d+)?\\s*(?:\\+\\s*)?"
                    + "(?:years?|yrs?)\\b"
                    + "|\\b(?:1|2)(?:\\.\\d+)?\\s*(?:-|to|–)\\s*"
                    + "(?:3|4|5|6|7|8|9|10|[1-9]\\d)(?:\\.\\d+)?\\s*"
                    + "(?:years?|yrs?)\\b"
                    + "|\\b(?:minimum|at least)\\s+(?:3|4|5|6|7|8|9|10|[1-9]\\d)"
                    + "(?:\\.\\d+)?\\s*(?:years?|yrs?)\\b");

    private static final Pattern BLOCKED_TITLE_PATTERN = Pattern.compile(
            "\\b(?:senior|sr\\.?|lead|principal|staff|architect|manager|director|head|"
                    + "vp|vice president)\\b");

    public JobSearchService(
            AdzunaJobClient adzunaJobClient,
            ResumeRepository resumeRepository,
            UserRepository userRepository) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.adzunaJobClient = adzunaJobClient;
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
    }

    // ============================================================
    // NORMAL JOB SEARCH
    // ============================================================

    public java.util.Optional<List<JobSearchResponse>> search(
            String query,
            String location) {

        try {

            List<JobSearchResponse> jobs =
                    fetchJobs(query, location);

            return java.util.Optional.of(jobs);

        } catch (Exception e) {

            logger.error("Job search failed: {}", e.getMessage());

            return java.util.Optional.empty();
        }
    }

    // ============================================================
    // RECOMMENDED JOBS - FRESHER ONLY
    // ============================================================

    public java.util.Optional<List<JobSearchResponse>> recommendedJobs(
            String location) {
        return recommendedJobs(location, null, null, null);
    }

    public java.util.Optional<List<JobSearchResponse>> recommendedJobs(
            String location,
            String role,
            String skills,
            Integer minimumScore) {

        try {

            if (location == null || location.isBlank()) {
                location = "Hyderabad";
            }

            List<JobSearchResponse> allJobs =
                    fetchFresherJobs(location);

            List<String> resumeSkills =
                    loadCurrentResume()
                            .map(this::extractResumeSkills)
                            .orElseGet(List::of);

            List<JobSearchResponse> recommendedJobs =
                    new ArrayList<>();

            Set<String> seenJobs =
                    new HashSet<>();

            for (JobSearchResponse job : allJobs) {

                if (job == null) {
                    continue;
                }

                String title =
                        safe(job.getTitle()).trim();

                if (title.isBlank()) {
                    continue;
                }

                // ------------------------------------------------
                // REMOVE DUPLICATES
                // ------------------------------------------------

                String uniqueKey =
                        title.toLowerCase()
                                + "|"
                                + safe(job.getCompanyName())
                                .toLowerCase();

                if (!seenJobs.add(uniqueKey)) {
                    continue;
                }

                // ------------------------------------------------
                // ONLY FRESHER JOBS
                // ------------------------------------------------

                if (!isFresherFriendly(job)) {
                    continue;
                }

                // ------------------------------------------------
                // CALCULATE SCORE
                // ------------------------------------------------

                List<String> matchedSkills =
                        findMatchedSkills(job, resumeSkills);

                int score =
                        calculateScore(job)
                                + Math.min(
                                matchedSkills.size() * 10,
                                MAX_RESUME_SKILL_BONUS
                        );

                if (!matchesRole(job, role)
                        || !matchesSkills(job, skills)
                        || (minimumScore != null && score < minimumScore)) {
                    continue;
                }

                // ------------------------------------------------
                // IMPORTANT:
                // Create a NEW JobSearchResponse with the score.
                //
                // JobSearchResponse does not have setScore().
                // ------------------------------------------------

                JobSearchResponse scoredJob =
                        new JobSearchResponse(
                                job.getTitle(),
                                job.getCompanyName(),
                                job.getDescription(),
                                job.getLocation(),
                                job.getUrl(),
                                score,
                                job.getRequiredSkills(),
                                matchedSkills
                        );

                recommendedJobs.add(scoredJob);
            }

            // ----------------------------------------------------
            // HIGHEST SCORE FIRST
            // ----------------------------------------------------

            recommendedJobs.sort(
                    Comparator.comparingInt(
                            JobSearchResponse::getScore
                    ).reversed()
            );

            // Maximum 20 jobs
            if (recommendedJobs.size() > 20) {

                recommendedJobs =
                        new ArrayList<>(
                                recommendedJobs.subList(0, 20)
                        );
            }

            return java.util.Optional.of(
                    recommendedJobs
            );

        } catch (Exception e) {

            logger.error("Recommended job search failed: {}", e.getMessage());

            return java.util.Optional.empty();
        }
    }

    private boolean matchesRole(JobSearchResponse job, String role) {
        if (role == null || role.isBlank()) {
            return true;
        }

        String normalizedRole = normalizeSearchText(role);
        String jobText = normalizeSearchText(
                safe(job.getTitle()) + " " + safe(job.getDescription()));

        if (jobText.contains(normalizedRole)) {
            return true;
        }

        return normalizedRole.split(" ").length > 1
                && java.util.Arrays.stream(normalizedRole.split(" "))
                .filter(term -> !term.isBlank())
                .allMatch(jobText::contains);
    }

    private boolean matchesSkills(JobSearchResponse job, String skills) {
        if (skills == null || skills.isBlank()) {
            return true;
        }

        String jobText = normalizeSearchText(
                safe(job.getTitle()) + " "
                        + safe(job.getDescription()) + " "
                        + String.join(" ", job.getRequiredSkills()));

        return java.util.Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .map(this::normalizeSearchText)
                .anyMatch(jobText::contains);
    }

    private String normalizeSearchText(String value) {
        return " " + safe(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                + " ";
    }

    // ============================================================
    // FETCH NORMAL JOBS
    // ============================================================

    private List<JobSearchResponse> fetchJobs(
            String query,
            String location) {

        List<JobSearchResponse> jobs =
                new ArrayList<>();

        try {

            String searchQuery =
                    query == null || query.isBlank()
                            ? "software developer"
                            : query;

            String url =
                    UriComponentsBuilder
                            .fromUriString(
                                    "https://remotive.com/api/remote-jobs"
                            )
                            .queryParam(
                                    "search",
                                    searchQuery
                            )
                            .build()
                            .toUriString();

            String response =
                    restTemplate.getForObject(
                            url,
                            String.class
                    );

            if (response == null) {
                return jobs;
            }

            JsonNode root =
                    objectMapper.readTree(response);

            JsonNode jobArray =
                    root.get("jobs");

            if (jobArray == null
                    || !jobArray.isArray()) {

                return jobs;
            }

            for (JsonNode node : jobArray) {

                JobSearchResponse job =
                        mapJob(node);

                if (job == null) {
                    continue;
                }

                if (location != null
                        && !location.isBlank()) {

                    String jobLocation =
                            safe(job.getLocation())
                                    .toLowerCase();

                    String requestedLocation =
                            location.toLowerCase();

                    // Remote jobs are allowed.
                    if (!jobLocation.contains(
                            requestedLocation)
                            && !jobLocation.contains(
                            "remote")) {

                        continue;
                    }
                }

                jobs.add(job);
            }

        } catch (Exception e) {

            logger.error("Job provider response parsing failed: {}", e.getMessage());
        }

        return jobs;
    }

    // ============================================================
    // FETCH MANY FRESHER SEARCH QUERIES
    // ============================================================

    private List<JobSearchResponse> fetchFresherJobs(
            String location) {

        List<JobSearchResponse> adzunaJobs = fetchAdzunaFresherJobs(location);
        if (!adzunaJobs.isEmpty()) {
            return adzunaJobs;
        }

        return fetchRemotiveFresherJobs(location);
    }

    private List<JobSearchResponse> fetchAdzunaFresherJobs(String location) {
        List<JobSearchResponse> jobs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        String[] queries = {
                "java developer",
                "spring boot developer",
                "backend developer",
                "frontend developer",
                "react developer",
                "full stack developer",
                "software developer",
                "software engineer",
                "python developer",
                "trainee developer",
                "graduate developer",
                "junior developer",
                "software developer internship"
        };

        for (String query : queries) {
            for (AdzunaJobClient.AdzunaJob adzunaJob
                    : adzunaJobClient.search(query, location, 20)) {

                JobSearchResponse job = mapAdzunaJob(adzunaJob);
                if (job == null || !isFresherFriendly(job)) {
                    continue;
                }

                String key = safe(job.getTitle()).trim().toLowerCase(Locale.ROOT)
                        + "|" + safe(job.getCompanyName()).trim().toLowerCase(Locale.ROOT)
                        + "|" + safe(job.getLocation()).trim().toLowerCase(Locale.ROOT);
                if (seen.add(key)) {
                    jobs.add(job);
                }
            }
        }

        return jobs;
    }

    private List<JobSearchResponse> fetchRemotiveFresherJobs(
            String location) {

        List<JobSearchResponse> jobs =
                new ArrayList<>();

        String[] queries = {

                // ------------------------------------------------
                // JAVA
                // ------------------------------------------------

                "java developer intern",
                "java developer fresher",
                "junior java developer",
                "entry level java developer",
                "trainee java developer",
                "graduate java developer",
                "java software engineer intern",

                // ------------------------------------------------
                // SPRING BOOT
                // ------------------------------------------------

                "spring boot intern",
                "spring boot fresher",
                "junior spring boot developer",
                "entry level spring boot developer",
                "trainee spring boot developer",

                // ------------------------------------------------
                // REACT / FRONTEND
                // ------------------------------------------------

                "react developer intern",
                "react developer fresher",
                "junior react developer",
                "entry level react developer",
                "frontend developer intern",
                "frontend developer fresher",
                "junior frontend developer",
                "entry level frontend developer",

                // ------------------------------------------------
                // BACKEND
                // ------------------------------------------------

                "backend developer intern",
                "backend developer fresher",
                "junior backend developer",
                "entry level backend developer",

                // ------------------------------------------------
                // FULL STACK
                // ------------------------------------------------

                "full stack developer intern",
                "full stack developer fresher",
                "junior full stack developer",
                "entry level full stack developer",

                // ------------------------------------------------
                // SOFTWARE
                // ------------------------------------------------

                "software developer intern",
                "software developer fresher",
                "junior software developer",
                "entry level software developer",
                "software engineer intern",
                "junior software engineer",
                "graduate software engineer",

                // ------------------------------------------------
                // TRAINEE / GRADUATE
                // ------------------------------------------------

                "trainee software developer",
                "trainee developer",
                "graduate developer",
                "graduate software developer",
                "entry level software engineer"
        };

        Set<String> seen =
                new HashSet<>();

        for (String query : queries) {

            try {

                String url =
                        UriComponentsBuilder
                                .fromUriString(
                                        "https://remotive.com/api/remote-jobs"
                                )
                                .queryParam(
                                        "search",
                                        query
                                )
                                .build()
                                .toUriString();

                String response =
                        restTemplate.getForObject(
                                url,
                                String.class
                        );

                if (response == null) {
                    continue;
                }

                JsonNode root =
                        objectMapper.readTree(response);

                JsonNode jobArray =
                        root.get("jobs");

                if (jobArray == null
                        || !jobArray.isArray()) {

                    continue;
                }

                for (JsonNode node : jobArray) {

                    JobSearchResponse job =
                            mapJob(node);

                    if (job == null) {
                        continue;
                    }

                    String title =
                            safe(job.getTitle());

                    if (title.isBlank()) {
                        continue;
                    }

                    // ------------------------------------------------
                    // LOCATION FILTER
                    // ------------------------------------------------

                    String jobLocation =
                            safe(job.getLocation())
                                    .toLowerCase();

                    String requestedLocation =
                            safe(location)
                                    .toLowerCase();

                    if (!requestedLocation.isBlank()
                            && !jobLocation.contains(
                            requestedLocation)
                            && !jobLocation.contains(
                            "remote")) {

                        continue;
                    }

                    // ------------------------------------------------
                    // UNIQUE JOB
                    // ------------------------------------------------

                    String key =
                            title.toLowerCase()
                                    + "|"
                                    + safe(
                                    job.getCompanyName()
                            ).toLowerCase();

                    if (seen.add(key)) {
                        jobs.add(job);
                    }
                }

            } catch (Exception e) {

                // Continue searching with the next query.
                System.out.println(
                        "Fresher search failed for: "
                                + query
                );
            }
        }

        return jobs;
    }

    // ============================================================
    // MAP API JOB
    // ============================================================

    private JobSearchResponse mapJob(
            JsonNode node) {

        try {

            String title =
                    getText(node, "title");

            String company =
                    getText(node, "company_name");

            String description =
                    getText(node, "description");

            String location =
                    getText(
                            node,
                            "candidate_required_location"
                    );

            String url =
                    getText(node, "url");

            return new JobSearchResponse(
                    title,
                    company,
                    cleanDescription(description),
                    location,
                    url,
                    0,
                    extractSkills(
                            title + " " + description
                    ),
                    new ArrayList<>()
            );

        } catch (Exception e) {

            return null;
        }
    }

    private JobSearchResponse mapAdzunaJob(
            AdzunaJobClient.AdzunaJob job) {

            if (job == null) {
                return null;
            }

            String title = safe(job.getTitle()).trim();
            String description = cleanDescription(job.getDescription());
            if (title.isBlank()) {
                return null;
            }

            return new JobSearchResponse(
                    title,
                    safe(job.getCompanyName()).trim(),
                    description,
                    safe(job.getLocation()).trim(),
                    safe(job.getRedirectUrl()).trim(),
                    0,
                    extractSkills(title + " " + description),
                    new ArrayList<>());
    }

    // ============================================================
    // FRESHER FILTER
    // ============================================================

    private boolean isFresherFriendly(JobSearchResponse job) {
        String title = safe(job.getTitle()).toLowerCase(Locale.ROOT).trim();
        String text = (title + " " + safe(job.getDescription()))
                .toLowerCase(Locale.ROOT)
                .replace('\u2013', '-');

        if (title.isBlank() || BLOCKED_TITLE_PATTERN.matcher(title).find()) {
            return false;
        }

        if (EXPERIENCED_ROLE_PATTERN.matcher(text).find()) {
            return false;
        }

        return title.contains("developer")
                || title.contains("software engineer")
                || title.contains("programmer")
                || title.contains("java")
                || title.contains("spring boot")
                || title.contains("backend")
                || title.contains("back-end")
                || title.contains("frontend")
                || title.contains("front-end")
                || title.contains("react")
                || title.contains("full stack")
                || title.contains("full-stack")
                || title.contains("python");
    }

    // ============================================================
    // RECOMMENDATION SCORE
    // ============================================================

    private int calculateScore(
            JobSearchResponse job) {

        String title =
                safe(job.getTitle())
                        .toLowerCase();

        String description =
                safe(job.getDescription())
                        .toLowerCase();

        String text =
                title + " " + description;

        int score = 0;

        // --------------------------------------------------------
        // FRESHER SIGNALS
        // --------------------------------------------------------

        if (text.contains("fresher")
                || text.contains("freshers")) {

            score += 30;
        }

        if (text.contains("intern")
                || text.contains("internship")) {

            score += 30;
        }

        if (text.contains("entry level")
                || text.contains("entry-level")) {

            score += 25;
        }

        if (text.contains("junior")) {

            score += 25;
        }

        if (text.contains("trainee")) {

            score += 25;
        }

        if (text.contains("graduate")
                || text.contains("new grad")) {

            score += 20;
        }

        if (text.contains("0-1 year")
                || text.contains("0 to 1 year")
                || text.contains("0-2 years")
                || text.contains("0 to 2 years")) {

            score += 25;
        }

        // --------------------------------------------------------
        // CORE TECHNOLOGIES
        // --------------------------------------------------------

        if (text.contains("java")) {
            score += 15;
        }

        if (text.contains("spring boot")) {
            score += 15;
        }

        if (text.contains("react")) {
            score += 15;
        }

        if (text.contains("javascript")) {
            score += 10;
        }

        if (text.contains("sql")
                || text.contains("mysql")
                || text.contains("postgresql")) {

            score += 8;
        }

        if (text.contains("python")) {
            score += 8;
        }

        if (text.contains("full stack")
                || text.contains("full-stack")) {

            score += 12;
        }

        if (text.contains("backend")
                || text.contains("back-end")) {

            score += 10;
        }

        if (text.contains("frontend")
                || text.contains("front-end")) {

            score += 10;
        }

        // --------------------------------------------------------
        // TITLE MATCH BONUS
        // --------------------------------------------------------

        if (title.contains("java developer")) {
            score += 10;
        }

        if (title.contains("spring boot")) {
            score += 10;
        }

        if (title.contains("react developer")) {
            score += 10;
        }

        if (title.contains("full stack")) {
            score += 8;
        }

        if (title.contains("software developer")) {
            score += 8;
        }

        if (title.contains("software engineer")) {
            score += 8;
        }

        // --------------------------------------------------------
        // REMOTE BONUS
        // --------------------------------------------------------

        if (safe(job.getLocation())
                .toLowerCase()
                .contains("remote")) {

            score += 3;
        }

        return score;
    }

    private List<String> findMatchedSkills(
            JobSearchResponse job,
            List<String> resumeSkills) {

        if (resumeSkills.isEmpty()) {
            return List.of();
        }

        String jobText = safe(job.getTitle())
                + " "
                + safe(job.getDescription())
                + " "
                + String.join(" ", job.getRequiredSkills());
        String normalizedJobText = normalizeSkillText(jobText);

        Set<String> matchedSkills = new LinkedHashSet<>();
        for (String skill : resumeSkills) {
            String normalizedSkill = normalizeSkillText(skill);
            if (!normalizedSkill.isBlank()
                    && containsSkill(normalizedJobText, normalizedSkill)) {
                matchedSkills.add(normalizedSkill);
            }
        }

        return new ArrayList<>(matchedSkills);
    }

    private List<String> extractResumeSkills(Resume resume) {
        Set<String> skills = new LinkedHashSet<>();

        if (resume.getSkills() != null && !resume.getSkills().isBlank()) {
            for (String part : RESUME_SKILL_SPLIT.split(resume.getSkills())) {
                addResumeSkill(skills, part);
            }
        }

        String resumeText = String.join(
                " ",
                safe(resume.getTitle()),
                safe(resume.getSummary()),
                safe(resume.getExperience()),
                safe(resume.getEducation()));

        for (String skill : extractSkills(resumeText)) {
            addResumeSkill(skills, skill);
        }

        return new ArrayList<>(skills);
    }

    private void addResumeSkill(
            Set<String> skills,
            String skill) {

        String cleaned = safe(skill)
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);

        if (cleaned.length() >= 2) {
            skills.add(cleaned);
        }
    }

    private boolean containsSkill(
            String normalizedText,
            String normalizedSkill) {

        return Pattern.compile(
                        "(?<![a-z0-9+#.])"
                                + Pattern.quote(normalizedSkill)
                                + "(?![a-z0-9+#.])")
                .matcher(normalizedText)
                .find();
    }

    private String normalizeSkillText(String value) {
        return safe(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Optional<Resume> loadCurrentResume() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof String email)) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email)
                .flatMap(resumeRepository::findByUser);
    }

    // ============================================================
    // EXTRACT SKILLS
    // ============================================================

    private List<String> extractSkills(
            String text) {

        List<String> skills =
                new ArrayList<>();

        String lower =
                safe(text).toLowerCase();

        String[] possibleSkills = {

                "java",
                "spring boot",
                "spring",

                "react",
                "javascript",
                "typescript",

                "html",
                "css",

                "python",

                "sql",
                "mysql",
                "postgresql",

                "mongodb",

                "git",
                "github",

                "docker",
                "aws",

                "rest api",
                "microservices"
        };

        for (String skill :
                possibleSkills) {

            if (lower.contains(skill)) {
                skills.add(skill);
            }
        }

        return skills;
    }

    // ============================================================
    // CLEAN DESCRIPTION
    // ============================================================

    private String cleanDescription(
            String description) {

        if (description == null) {
            return "";
        }

        return description
                .replaceAll(
                        "<[^>]*>",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    // ============================================================
    // JSON TEXT HELPER
    // ============================================================

    private String getText(
            JsonNode node,
            String field) {

        JsonNode value =
                node.get(field);

        if (value == null
                || value.isNull()) {

            return "";
        }

        return value.asText("");
    }

    // ============================================================
    // SAFE STRING
    // ============================================================

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }
}