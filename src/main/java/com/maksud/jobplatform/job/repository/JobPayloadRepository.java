package com.maksud.jobplatform.job.repository;

import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.JobPayload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobPayloadRepository extends JpaRepository<JobPayload, Long> {
    Optional<JobPayload> findByJob(Job job);
}
