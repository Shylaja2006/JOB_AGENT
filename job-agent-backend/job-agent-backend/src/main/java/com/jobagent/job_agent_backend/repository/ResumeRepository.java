package com.jobagent.job_agent_backend.repository;

import com.jobagent.job_agent_backend.entity.Resume;
import com.jobagent.job_agent_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByUser(User user);

    boolean existsByUser(User user);
}
