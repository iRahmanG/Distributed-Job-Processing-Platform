package com.maksud.jobplatform.deadletter.repository;

import com.maksud.jobplatform.deadletter.entity.DeadLetterJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeadLetterRepository extends JpaRepository<DeadLetterJob, String> {

    List<DeadLetterJob> findAllByOrderByFailedAtDesc();

    Optional<DeadLetterJob> findByJobId(String jobId);
}
