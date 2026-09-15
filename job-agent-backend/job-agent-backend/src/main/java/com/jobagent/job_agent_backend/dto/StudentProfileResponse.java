package com.jobagent.job_agent_backend.dto;

public class StudentProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private Integer graduationYear;
    private String degree;
    private String branch;
    private String currentStatus;
    private String experienceType;
    private String lookingFor;
    private String preferredLocation;
    private String skills;

    public StudentProfileResponse() {
    }

    public StudentProfileResponse(
            Long id,
            String fullName,
            String email,
            Integer graduationYear,
            String degree,
            String branch,
            String currentStatus,
            String experienceType,
            String lookingFor,
            String preferredLocation,
            String skills) {

        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.graduationYear = graduationYear;
        this.degree = degree;
        this.branch = branch;
        this.currentStatus = currentStatus;
        this.experienceType = experienceType;
        this.lookingFor = lookingFor;
        this.preferredLocation = preferredLocation;
        this.skills = skills;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public String getDegree() {
        return degree;
    }

    public String getBranch() {
        return branch;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getExperienceType() {
        return experienceType;
    }

    public String getLookingFor() {
        return lookingFor;
    }

    public String getPreferredLocation() {
        return preferredLocation;
    }

    public String getSkills() {
        return skills;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public void setExperienceType(String experienceType) {
        this.experienceType = experienceType;
    }

    public void setLookingFor(String lookingFor) {
        this.lookingFor = lookingFor;
    }

    public void setPreferredLocation(String preferredLocation) {
        this.preferredLocation = preferredLocation;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }
}