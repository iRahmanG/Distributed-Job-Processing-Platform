package com.maksud.jobplatform.job.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maksud.jobplatform.common.exception.PayloadSerializationException;
import com.maksud.jobplatform.job.dto.CreateJobRequest;
import com.maksud.jobplatform.job.dto.CreateJobResponse;
import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.JobPayload;
import com.maksud.jobplatform.job.entity.enums.JobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JobMapper {

    private final ObjectMapper objectMapper;

    public Job toJob(CreateJobRequest request,
                     String jobId,
                     LocalDateTime now) {

        return Job.builder()
                .jobId(jobId)
                .jobType(request.jobType())
                .priority(request.priority())
                .status(JobStatus.CREATED)
                .retryCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public JobPayload toJobPayload(CreateJobRequest request,
                                   Job job,
                                   LocalDateTime now) {

        try {
            return JobPayload.builder()
                    .job(job)
                    .payload(objectMapper.writeValueAsString(request.payload()))
                    .createdAt(now)
                    .build();
        } catch (JsonProcessingException e) {
            throw new PayloadSerializationException("Failed to serialize payload", e);
        }
    }

    public CreateJobResponse toResponse(Job job) {

        return new CreateJobResponse(
                job.getJobId(),
                job.getStatus().name()
        );
    }
}
