package com.maksud.jobplatform.job.repository;

import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {

    @Modifying
    @Query("""
            UPDATE Job j
            SET j.status = :processingStatus,
                j.updatedAt = :updatedAt
            WHERE j.jobId = :jobId
              AND j.status = :queuedStatus
            """)
    int claimJob(
            @Param("jobId") String jobId,
            @Param("queuedStatus") JobStatus queuedStatus,
            @Param("processingStatus") JobStatus processingStatus,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying
    @Query("""
            UPDATE Job j
            SET j.status = :status,
                j.updatedAt = :updatedAt
            WHERE j.jobId = :jobId
            """)
    int updateStatus(
            @Param("jobId") String jobId,
            @Param("status") JobStatus status,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying
    @Query("""
            UPDATE Job j
            SET j.status = :status,
                j.retryCount = :retryCount,
                j.updatedAt = :updatedAt
            WHERE j.jobId = :jobId
            """)
    int updateExecutionResult(
            @Param("jobId") String jobId,
            @Param("status") JobStatus status,
            @Param("retryCount") int retryCount,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying
    @Query("""
            UPDATE Job j
            SET j.status = :status,
                j.retryCount = :retryCount,
                j.nextRetryAt = :nextRetryAt,
                j.updatedAt = :updatedAt
            WHERE j.jobId = :jobId
            """)
    int updateRetryState(
            @Param("jobId") String jobId,
            @Param("status") JobStatus status,
            @Param("retryCount") int retryCount,
            @Param("nextRetryAt") LocalDateTime nextRetryAt,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Query("""
            SELECT j
            FROM Job j
            WHERE j.status = :status
              AND j.retryCount < :maxRetry
            ORDER BY j.updatedAt ASC
            """)
    List<Job> findRetryableJobs(
            @Param("status") JobStatus status,
            @Param("maxRetry") int maxRetry
    );

    List<Job> findTop100ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
            JobStatus status,
            LocalDateTime now
    );

    List<Job> findTop100ByStatusOrderByUpdatedAtAsc(JobStatus status);
}
