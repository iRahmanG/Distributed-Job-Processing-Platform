package com.maksud.jobplatform.job.exception;

public class InvalidJobStatusTransitionException extends RuntimeException{

    public InvalidJobStatusTransitionException(
            String message
    ) {
        super(message);
    }
}
