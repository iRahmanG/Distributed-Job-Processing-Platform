package com.maksud.jobplatform;

import com.maksud.jobplatform.job.config.RetryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(RetryProperties.class)
public class
DistributedJobPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(DistributedJobPlatformApplication.class, args);
	}

}
