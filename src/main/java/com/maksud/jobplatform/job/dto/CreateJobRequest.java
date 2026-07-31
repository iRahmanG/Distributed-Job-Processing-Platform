package com.maksud.jobplatform.job.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.maksud.jobplatform.job.entity.enums.JobPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateJobRequest(
        @NotBlank @Size(max = 100)
        String jobType,
        @NotNull
        JobPriority priority,
        @NotNull
        JsonNode payload
) {}