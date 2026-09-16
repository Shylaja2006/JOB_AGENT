package com.jobagent.job_agent_backend.dto;

import java.util.List;

public class JobSearchResponse {

    private String title;
    private String companyName;
    private String description;
    private String location;
    private String url;
    private int score;
    private List<String> requiredSkills;
    private List<String> matchedSkills;

    public JobSearchResponse(
            String title,
            String companyName,
            String description,
            String location,
            String url,
            int score,
            List<String> requiredSkills,
            List<String> matchedSkills) {

        this.title = title;
        this.companyName = companyName;
        this.description = description;
        this.location = location;
        this.url = url;
        this.score = score;
        this.requiredSkills = requiredSkills;
        this.matchedSkills = matchedSkills;
    }

    public String getTitle() {
        return title;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public String getUrl() {
        return url;
    }

    public int getScore() {
        return score;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }
}