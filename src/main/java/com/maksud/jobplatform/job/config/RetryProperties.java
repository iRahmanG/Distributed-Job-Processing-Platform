package com.maksud.jobplatform.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "job-platform.retry")
@Data
public class RetryProperties {

    private int maxAttempts;

    private int delaySeconds;

}