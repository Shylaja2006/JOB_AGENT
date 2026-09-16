package com.jobagent.job_agent_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class JobApplicationRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @NotBlank(message = "Company is required")
    @Size(max = 150, message = "Company must not exceed 150 characters")
    private String company;

    @Size(max = 150, message = "Location must not exceed 150 characters")
    private String location;

    @Size(max = 500, message = "Job URL must not exceed 500 characters")
    private String jobUrl;

    @Pattern(
            regexp = "SAVED|APPLIED|INTERVIEW|REJECTED|OFFER|OFFERED",
            message = "Status must be SAVED, APPLIED, INTERVIEW, REJECTED, or OFFER")
    @Size(max = 50, message = "Status must not exceed 50 characters")
    private String status;

    private String notes;

    public JobApplicationRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
