package com.itson.jgantt.domain.exception;

public class GanttDomainException extends RuntimeException {

    public GanttDomainException(String message) {
        super(message);
    }

    public GanttDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}