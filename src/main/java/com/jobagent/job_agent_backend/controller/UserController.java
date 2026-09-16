package com.jobagent.job_agent_backend.controller;

import com.jobagent.job_agent_backend.dto.UserProfileResponse;
import com.jobagent.job_agent_backend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/profile")
    public UserProfileResponse getProfile() {
        return userService.getCurrentUserProfile();
    }
}
