package com.jobagent.job_agent_backend.service;

import com.jobagent.job_agent_backend.dto.JobApplicationRequest;
import com.jobagent.job_agent_backend.dto.JobApplicationResponse;
import com.jobagent.job_agent_backend.dto.JobApplicationStatusRequest;
import com.jobagent.job_agent_backend.entity.JobApplication;
import com.jobagent.job_agent_backend.entity.User;
import com.jobagent.job_agent_backend.repository.JobApplicationRepository;
import com.jobagent.job_agent_backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;

    public JobApplicationService(
            JobApplicationRepository jobApplicationRepository,
            UserRepository userRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.userRepository = userRepository;
    }

    public Optional<JobApplicationResponse> createApplication(JobApplicationRequest request) {
        User user = findCurrentUser().orElse(null);
        if (user == null) {
            return Optional.empty();
        }

        JobApplication application = new JobApplication();
        application.setUser(user);
        applyRequest(application, request);

        return Optional.of(toResponse(jobApplicationRepository.save(application)));
    }

    public List<JobApplicationResponse> listApplications() {
        return findCurrentUser()
                .map(user -> jobApplicationRepository.findByUser(user).stream()
                        .map(this::toResponse)
                        .toList())
                .orElse(Collections.emptyList());
    }

    public Optional<JobApplicationResponse> getApplication(Long id) {
        return findOwnedApplication(id).map(this::toResponse);
    }

    public Optional<JobApplicationResponse> updateApplication(Long id, JobApplicationRequest request) {
        return findOwnedApplication(id).map(application -> {
            applyRequest(application, request);
            return toResponse(jobApplicationRepository.save(application));
        });
    }

    public Optional<JobApplicationResponse> updateApplicationStatus(
            Long id,
            JobApplicationStatusRequest request) {
        return findOwnedApplication(id).map(application -> {
            application.setStatus(normalizeStatus(request.getStatus()));
            return toResponse(jobApplicationRepository.save(application));
        });
    }

    public boolean deleteApplication(Long id) {
        Optional<JobApplication> application = findOwnedApplication(id);
        if (application.isEmpty()) {
            return false;
        }
        jobApplicationRepository.delete(application.get());
        return true;
    }

    private Optional<User> findCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof String email)) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email);
    }

    private Optional<JobApplication> findOwnedApplication(Long id) {
        return findCurrentUser().flatMap(user -> jobApplicationRepository.findByIdAndUser(id, user));
    }

    private void applyRequest(JobApplication application, JobApplicationRequest request) {
        application.setTitle(request.getTitle());
        application.setCompany(request.getCompany());
        application.setLocation(request.getLocation());
        application.setJobUrl(request.getJobUrl());
        application.setNotes(request.getNotes());

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            application.setStatus("SAVED");
        } else {
            application.setStatus(normalizeStatus(request.getStatus()));
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "SAVED";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("OFFERED") ? "OFFER" : normalized;
    }

    private JobApplicationResponse toResponse(JobApplication application) {
        return new JobApplicationResponse(
                application.getId(),
                application.getTitle(),
                application.getCompany(),
                application.getLocation(),
                application.getJobUrl(),
                normalizeStatus(application.getStatus()),
                application.getNotes(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
