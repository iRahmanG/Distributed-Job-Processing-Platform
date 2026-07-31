package com.maksud.jobplatform.job.controller;

import com.maksud.jobplatform.job.dto.CreateJobRequest;
import com.maksud.jobplatform.job.dto.CreateJobResponse;
import com.maksud.jobplatform.job.entity.Job;
import com.maksud.jobplatform.job.repository.JobRepository;
import com.maksud.jobplatform.job.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/jobs")
public class JobController {

    private final JobService jobService;

    @PostMapping
    ResponseEntity<CreateJobResponse> createJob(@Valid @RequestBody CreateJobRequest request){
        CreateJobResponse response = jobService.createJob(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
