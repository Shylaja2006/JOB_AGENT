package com.jobagent.job_agent_backend.dto;

public class ResumeUploadResponse {

    private String fileName;
    private String message;
    private ResumeResponse resume;

    public ResumeUploadResponse() {
    }

    public ResumeUploadResponse(
            String fileName,
            String message,
            ResumeResponse resume) {

        this.fileName = fileName;
        this.message = message;
        this.resume = resume;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ResumeResponse getResume() {
        return resume;
    }

    public void setResume(ResumeResponse resume) {
        this.resume = resume;
    }
}