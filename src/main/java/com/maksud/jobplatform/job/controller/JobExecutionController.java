package com.maksud.jobplatform.job.controller;

import com.maksud.jobplatform.job.dto.JobExecutionDetailsResponse;
import com.maksud.jobplatform.job.dto.JobExecutionResponse;
import com.maksud.jobplatform.job.service.JobExecutionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobExecutionController {

    private final JobExecutionQueryService jobExecutionQueryService;

    @GetMapping("/{jobId}/executions")
    public ResponseEntity<List<JobExecutionResponse>> getExecutions(
            @PathVariable String jobId
    ) {

        return ResponseEntity.ok(
                jobExecutionQueryService.getExecutions(jobId)
        );
    }

    @GetMapping("/{jobId}/executions/{executionId}")
    public ResponseEntity<JobExecutionDetailsResponse> getExecution(
            @PathVariable String jobId,
            @PathVariable String executionId
    ) {

        return ResponseEntity.ok(
                jobExecutionQueryService.getExecution(
                        jobId,
                        executionId
                )
        );
    }
}