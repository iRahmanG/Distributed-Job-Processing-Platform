package com.maksud.jobplatform.deadletter.service;

import com.maksud.jobplatform.deadletter.dto.DeadLetterDetailsResponse;
import com.maksud.jobplatform.deadletter.dto.DeadLetterResponse;
import com.maksud.jobplatform.job.entity.Job;

import java.util.List;

public interface DeadLetterService {
    void moveToDeadLetter(
            Job job,
            String payload,
            Exception exception
    );

    List<DeadLetterResponse> getAllJobs();

    DeadLetterDetailsResponse getJob(String jobId);

    void replayJob(String jobId);

    void deleteJob(String jobId);

}
