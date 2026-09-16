package com.jobagent.job_agent_backend.controller;

import com.jobagent.job_agent_backend.dto.JobApplicationRequest;
import com.jobagent.job_agent_backend.dto.JobApplicationResponse;
import com.jobagent.job_agent_backend.dto.JobApplicationStatusRequest;
import com.jobagent.job_agent_backend.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    @PostMapping
    public ResponseEntity<JobApplicationResponse> createApplication(
            @Valid @RequestBody JobApplicationRequest request) {
        return jobApplicationService.createApplication(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<JobApplicationResponse>> listApplications() {
        return ResponseEntity.ok(jobApplicationService.listApplications());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<JobApplicationResponse> getApplication(@PathVariable Long id) {
        return jobApplicationService.getApplication(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<JobApplicationResponse> updateApplication(
            @PathVariable Long id,
            @Valid @RequestBody JobApplicationRequest request) {
        return jobApplicationService.updateApplication(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id:\\d+}/status")
    public ResponseEntity<JobApplicationResponse> updateApplicationStatus(
            @PathVariable Long id,
            @Valid @RequestBody JobApplicationStatusRequest request) {
        return jobApplicationService.updateApplicationStatus(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        if (!jobApplicationService.deleteApplication(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
