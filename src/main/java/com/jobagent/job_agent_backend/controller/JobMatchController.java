package com.jobagent.job_agent_backend.controller;

import com.jobagent.job_agent_backend.dto.JobMatchResponse;
import com.jobagent.job_agent_backend.service.JobMatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class JobMatchController {

    private final JobMatchService jobMatchService;

    public JobMatchController(JobMatchService jobMatchService) {
        this.jobMatchService = jobMatchService;
    }

    @GetMapping("/matches")
    public ResponseEntity<List<JobMatchResponse>> listMatches() {
        return jobMatchService.listMatches()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/match")
    public ResponseEntity<JobMatchResponse> getMatch(@PathVariable Long id) {
        return jobMatchService.getMatch(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
