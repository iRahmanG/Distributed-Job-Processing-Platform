package com.maksud.jobplatform.job.repository;

import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {

    @Modifying
    @Query("""
            UPDATE Job j
            SET j.status = :processingStatus,
                j.update = :updateAt
            WHERE j.jobId = :jobId
            AND j.status = :queuedStatus
            """)
    int claimJob(
            @Param("jobId") String jobId,
            @Param("queuedStatus")JobStatus queuedStatus,
            @Param("processingStatus") JobStatus processingStatus,
            @Param("updatedAt")LocalDateTime updatedAt
    );
}
