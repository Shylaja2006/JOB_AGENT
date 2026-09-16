package com.jobagent.job_agent_backend.controller;

import com.jobagent.job_agent_backend.dto.StudentProfileRequest;
import com.jobagent.job_agent_backend.dto.StudentProfileResponse;
import com.jobagent.job_agent_backend.service.StudentProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student-profile")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(
            StudentProfileService studentProfileService) {

        this.studentProfileService = studentProfileService;
    }

    @PostMapping
    public ResponseEntity<StudentProfileResponse> createProfile(
            @Valid @RequestBody StudentProfileRequest request) {

        if (studentProfileService.profileAlreadyExists()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        return studentProfileService
                .createProfile(request)
                .map(response ->
                        ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response)
                )
                .orElseGet(() ->
                        ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .build()
                );
    }

    @GetMapping
    public ResponseEntity<StudentProfileResponse> getProfile() {

        return studentProfileService
                .getProfile()
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }

    @PutMapping
    public ResponseEntity<StudentProfileResponse> updateProfile(
            @Valid @RequestBody StudentProfileRequest request) {

        return studentProfileService
                .updateProfile(request)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteProfile() {

        if (!studentProfileService.deleteProfile()) {
            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity
                .noContent()
                .build();
    }
}