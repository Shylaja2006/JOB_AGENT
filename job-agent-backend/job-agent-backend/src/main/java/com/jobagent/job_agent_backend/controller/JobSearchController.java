package com.jobagent.job_agent_backend.controller;

import com.jobagent.job_agent_backend.dto.JobSearchResponse;
import com.jobagent.job_agent_backend.dto.ApiErrorResponse;
import com.jobagent.job_agent_backend.service.JobSearchService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobSearchController {

    private final JobSearchService jobSearchService;

    public JobSearchController(
            JobSearchService jobSearchService) {

        this.jobSearchService = jobSearchService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<JobSearchResponse>> searchJobs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String location) {

        return jobSearchService.search(query, location)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity
                                .notFound()
                                .build()
                );
    }

    @GetMapping("/recommended")
    public ResponseEntity<?> recommendedJobs(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String minScore) {

        Integer minimumScore = null;
        if (minScore != null && !minScore.isBlank()) {
            try {
                minimumScore = Integer.valueOf(minScore.trim());
            } catch (NumberFormatException exception) {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponse(
                                400,
                                "Bad Request",
                                "minScore must be an integer between 0 and 100"));
            }

            if (minimumScore < 0 || minimumScore > 100) {
                return ResponseEntity.badRequest()
                        .body(new ApiErrorResponse(
                                400,
                                "Bad Request",
                                "minScore must be between 0 and 100"));
            }
        }

        return jobSearchService
                .recommendedJobs(location, role, skills, minimumScore)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity
                                .notFound()
                                .build()
                );
    }
}