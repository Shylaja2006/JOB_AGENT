package com.jobagent.job_agent_backend.service;

import com.jobagent.job_agent_backend.dto.StudentProfileRequest;
import com.jobagent.job_agent_backend.dto.StudentProfileResponse;
import com.jobagent.job_agent_backend.entity.StudentProfile;
import com.jobagent.job_agent_backend.entity.User;
import com.jobagent.job_agent_backend.repository.StudentProfileRepository;
import com.jobagent.job_agent_backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    public StudentProfileService(
            StudentProfileRepository studentProfileRepository,
            UserRepository userRepository) {

        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
    }

    public Optional<StudentProfileResponse> createProfile(
            StudentProfileRequest request) {

        Optional<User> userOptional = findCurrentUser();

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();

        if (studentProfileRepository.existsByUser(user)) {
            return Optional.empty();
        }

        StudentProfile profile = new StudentProfile();

        profile.setUser(user);

        updateProfileFields(profile, request);

        StudentProfile saved =
                studentProfileRepository.save(profile);

        return Optional.of(toResponse(saved));
    }

    public Optional<StudentProfileResponse> getProfile() {

        Optional<User> userOptional = findCurrentUser();

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        return studentProfileRepository
                .findByUser(userOptional.get())
                .map(this::toResponse);
    }

    public Optional<StudentProfileResponse> updateProfile(
            StudentProfileRequest request) {

        Optional<User> userOptional = findCurrentUser();

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        Optional<StudentProfile> profileOptional =
                studentProfileRepository
                        .findByUser(userOptional.get());

        if (profileOptional.isEmpty()) {
            return Optional.empty();
        }

        StudentProfile profile = profileOptional.get();

        updateProfileFields(profile, request);

        StudentProfile updated =
                studentProfileRepository.save(profile);

        return Optional.of(toResponse(updated));
    }

    public boolean deleteProfile() {

        Optional<User> userOptional = findCurrentUser();

        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();

        Optional<StudentProfile> profile =
                studentProfileRepository.findByUser(user);

        if (profile.isEmpty()) {
            return false;
        }

        studentProfileRepository.delete(profile.get());

        return true;
    }

    public boolean profileAlreadyExists() {

        return findCurrentUser()
                .map(studentProfileRepository::existsByUser)
                .orElse(false);
    }

    private void updateProfileFields(
            StudentProfile profile,
            StudentProfileRequest request) {

        profile.setFullName(request.getFullName().trim());
        profile.setGraduationYear(request.getGraduationYear());
        profile.setDegree(request.getDegree().trim());
        profile.setBranch(request.getBranch().trim());
        profile.setCurrentStatus(request.getCurrentStatus().trim());
        profile.setExperienceType(request.getExperienceType().trim());
        profile.setLookingFor(request.getLookingFor().trim());

        if (request.getPreferredLocation() != null) {
            profile.setPreferredLocation(
                    request.getPreferredLocation().trim()
            );
        } else {
            profile.setPreferredLocation(null);
        }

        if (request.getSkills() != null) {
            profile.setSkills(
                    request.getSkills().trim()
            );
        } else {
            profile.setSkills(null);
        }
    }

    private StudentProfileResponse toResponse(
            StudentProfile profile) {

        String email = profile.getUser() != null
                ? profile.getUser().getEmail()
                : null;

        return new StudentProfileResponse(
                profile.getId(),
                profile.getFullName(),
                email,
                profile.getGraduationYear(),
                profile.getDegree(),
                profile.getBranch(),
                profile.getCurrentStatus(),
                profile.getExperienceType(),
                profile.getLookingFor(),
                profile.getPreferredLocation(),
                profile.getSkills()
        );
    }

    private Optional<User> findCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || authentication.getPrincipal() == null) {
            return Optional.empty();
        }

        Object principal =
                authentication.getPrincipal();

        if (!(principal instanceof String email)) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email);
    }
}