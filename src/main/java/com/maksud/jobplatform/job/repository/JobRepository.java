package com.maksud.jobplatform.job.repository;

import com.maksud.jobplatform.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, String> {
}
