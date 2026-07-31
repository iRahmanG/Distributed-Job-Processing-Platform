package com.maksud.jobplatform.job.repository;

import com.maksud.jobplatform.job.entity.JobPayload;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPayloadRepository extends JpaRepository<JobPayload, Long> {
}
