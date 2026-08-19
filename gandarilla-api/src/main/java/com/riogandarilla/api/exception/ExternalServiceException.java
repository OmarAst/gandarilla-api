package com.riogandarilla.api.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends ApiException {
    public ExternalServiceException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }
}
