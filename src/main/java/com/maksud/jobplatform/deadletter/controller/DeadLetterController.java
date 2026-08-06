package com.maksud.jobplatform.deadletter.controller;

import com.maksud.jobplatform.deadletter.dto.DeadLetterDetailsResponse;
import com.maksud.jobplatform.deadletter.dto.DeadLetterResponse;
import com.maksud.jobplatform.deadletter.service.DeadLetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dlq")
@RequiredArgsConstructor
public class DeadLetterController {

    private final DeadLetterService deadLetterService;

    @GetMapping
    public List<DeadLetterResponse> getAll() {

        return deadLetterService.getAllJobs();
    }

    @GetMapping("/{jobId}")
    public DeadLetterDetailsResponse getJob(@PathVariable String jobId) {

        return deadLetterService.getJob(jobId);
    }

    @PostMapping("/{jobId}/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void replay(@PathVariable String jobId) {

        deadLetterService.replayJob(jobId);
    }

    @DeleteMapping("/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String jobId) {

        deadLetterService.deleteJob(jobId);
    }
}
