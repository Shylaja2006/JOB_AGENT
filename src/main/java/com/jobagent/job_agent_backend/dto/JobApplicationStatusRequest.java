package com.jobagent.job_agent_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class JobApplicationStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
            regexp = "SAVED|APPLIED|INTERVIEW|REJECTED|OFFER|OFFERED",
            message = "Status must be SAVED, APPLIED, INTERVIEW, REJECTED, or OFFER")
    private String status;

    public JobApplicationStatusRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
