package com.jobagent.job_agent_backend.controller;

import com.jobagent.job_agent_backend.dto.ResumeRequest;
import com.jobagent.job_agent_backend.dto.ResumeResponse;
import com.jobagent.job_agent_backend.service.ResumeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.jobagent.job_agent_backend.dto.ResumeUploadResponse;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping
    public ResponseEntity<ResumeResponse> createResume(@Valid @RequestBody ResumeRequest request) {
        if (resumeService.resumeAlreadyExists()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return resumeService.createResume(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<ResumeResponse> getResume() {
        return resumeService.getResume()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<ResumeResponse> updateResume(@Valid @RequestBody ResumeRequest request) {
        return resumeService.updateResume(request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteResume() {
        if (!resumeService.deleteResume()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/upload")
public ResponseEntity<ResumeUploadResponse> uploadResume(
        @RequestParam("file") MultipartFile file) {

    return resumeService.uploadResume(file)
            .map(response ->
                    ResponseEntity.status(HttpStatus.CREATED).body(response)
            )
            .orElseGet(() ->
                    ResponseEntity.badRequest().build()
            );
}
}
