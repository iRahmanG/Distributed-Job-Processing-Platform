package com.maksud.jobplatform.job.service;

import com.github.f4b6a3.ulid.UlidCreator;
import com.maksud.jobplatform.job.dto.CreateJobRequest;
import com.maksud.jobplatform.job.dto.CreateJobResponse;
import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.entity.JobPayload;
import com.maksud.jobplatform.job.mapper.JobMapper;
import com.maksud.jobplatform.job.repository.JobPayloadRepository;
import com.maksud.jobplatform.job.repository.JobRepository;
import com.maksud.jobplatform.outbox.entity.OutboxEvent;
import com.maksud.jobplatform.outbox.mapper.OutboxMapper;
import com.maksud.jobplatform.outbox.repository.OutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobPayloadRepository jobPayloadRepository;
    private final JobMapper jobMapper;
    private final OutboxRepository outboxRepository;
    private final OutboxMapper outboxMapper;


    @Override
    @Transactional
    public CreateJobResponse createJob(CreateJobRequest request) {

        String jobId = UlidCreator.getUlid().toString();
        String eventId = UlidCreator.getUlid().toString();
        LocalDateTime now = LocalDateTime.now();

        Job job = jobMapper.toJob(request, jobId, now);
        JobPayload payload = jobMapper.toJobPayload(request, job, now);
        OutboxEvent outboxEvent = outboxMapper.toOutBoxEvent(job, now, eventId);

        jobRepository.save(job);
        jobPayloadRepository.save(payload);
        outboxRepository.save(outboxEvent);

        return jobMapper.toResponse(job);
    }
}
