package com.jobagent.job_agent_backend.dto;

public record ApiErrorResponse(
        int status,
        String error,
        String message) {
}
