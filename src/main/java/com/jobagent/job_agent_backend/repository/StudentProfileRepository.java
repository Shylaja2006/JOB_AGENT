package com.jobagent.job_agent_backend.repository;

import com.jobagent.job_agent_backend.entity.StudentProfile;
import com.jobagent.job_agent_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentProfileRepository
        extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUser(User user);

    boolean existsByUser(User user);

    void deleteByUser(User user);
}