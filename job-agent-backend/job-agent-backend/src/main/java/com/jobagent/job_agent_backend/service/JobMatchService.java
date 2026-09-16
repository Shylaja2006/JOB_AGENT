package com.jobagent.job_agent_backend.service;

import com.jobagent.job_agent_backend.dto.JobMatchResponse;
import com.jobagent.job_agent_backend.entity.JobApplication;
import com.jobagent.job_agent_backend.entity.Resume;
import com.jobagent.job_agent_backend.entity.User;
import com.jobagent.job_agent_backend.repository.JobApplicationRepository;
import com.jobagent.job_agent_backend.repository.ResumeRepository;
import com.jobagent.job_agent_backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class JobMatchService {

    private static final Pattern SKILL_SPLIT = Pattern.compile("[,;|/\\n]+");
    private static final Pattern NON_LETTER_OR_DIGIT = Pattern.compile("[^a-z0-9+#\\.]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "of", "in", "to", "for", "with", "at", "on",
            "is", "as", "by", "from", "using", "use");

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final JobApplicationRepository jobApplicationRepository;

    public JobMatchService(
            UserRepository userRepository,
            ResumeRepository resumeRepository,
            JobApplicationRepository jobApplicationRepository) {
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    public Optional<List<JobMatchResponse>> listMatches() {
        return findCurrentUserResume().map(resume ->
                jobApplicationRepository.findByUser(resume.getUser()).stream()
                        .map(application -> toMatchResponse(resume, application))
                        .sorted(Comparator.comparingInt(JobMatchResponse::getScore).reversed()
                                .thenComparing(JobMatchResponse::getApplicationId))
                        .toList());
    }

    public Optional<JobMatchResponse> getMatch(Long applicationId) {
        Optional<Resume> resume = findCurrentUserResume();
        if (resume.isEmpty()) {
            return Optional.empty();
        }

        return jobApplicationRepository.findByIdAndUser(applicationId, resume.get().getUser())
                .map(application -> toMatchResponse(resume.get(), application));
    }

    private Optional<Resume> findCurrentUserResume() {
        return findCurrentUser().flatMap(resumeRepository::findByUser);
    }

    private Optional<User> findCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof String email)) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email);
    }

    private JobMatchResponse toMatchResponse(Resume resume, JobApplication application) {
        List<String> resumeSkills = extractResumeSkills(resume);
        String applicationText = normalize(
                join(application.getTitle(), application.getCompany(),
                        application.getLocation(), application.getNotes()));

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String skill : resumeSkills) {
            if (containsSkill(applicationText, skill)) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }

        int score = 0;
        if (!resumeSkills.isEmpty()) {
            score = (int) Math.round(matched.size() * 100.0 / resumeSkills.size());
        }

        return new JobMatchResponse(
                application.getId(),
                application.getTitle(),
                application.getCompany(),
                application.getStatus(),
                score,
                matched,
                missing);
    }

    private List<String> extractResumeSkills(Resume resume) {
        Set<String> skills = new LinkedHashSet<>();

        if (resume.getSkills() != null && !resume.getSkills().isBlank()) {
            for (String part : SKILL_SPLIT.split(resume.getSkills())) {
                String skill = part.trim().toLowerCase(Locale.ROOT);
                if (skill.length() >= 2) {
                    skills.add(skill);
                }
            }
        }

        if (skills.isEmpty()) {
            String blob = join(
                    resume.getTitle(),
                    resume.getSummary(),
                    resume.getExperience(),
                    resume.getEducation());
            for (String token : tokenize(blob)) {
                skills.add(token);
            }
        }

        return new ArrayList<>(skills);
    }

    private boolean containsSkill(String applicationText, String skill) {
        String normalizedSkill = normalize(skill);
        if (normalizedSkill.isBlank() || applicationText.isBlank()) {
            return false;
        }
        return Pattern.compile("(?:^|\\s)" + Pattern.quote(normalizedSkill) + "(?:\\s|$)")
                .matcher(applicationText)
                .find();
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(NON_LETTER_OR_DIGIT.split(text.toLowerCase(Locale.ROOT)))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .distinct()
                .toList();
    }

    private String join(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(' ');
                }
                builder.append(part);
            }
        }
        return builder.toString();
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
