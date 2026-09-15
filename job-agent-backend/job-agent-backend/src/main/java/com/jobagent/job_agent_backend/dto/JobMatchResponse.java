package com.jobagent.job_agent_backend.dto;

import java.util.ArrayList;
import java.util.List;

public class JobMatchResponse {

    private Long applicationId;
    private String title;
    private String company;
    private String status;
    private int score;
    private List<String> matchedSkills = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();

    public JobMatchResponse() {
    }

    public JobMatchResponse(
            Long applicationId,
            String title,
            String company,
            String status,
            int score,
            List<String> matchedSkills,
            List<String> missingSkills) {
        this.applicationId = applicationId;
        this.title = title;
        this.company = company;
        this.status = status;
        this.score = score;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills;
    }
}
