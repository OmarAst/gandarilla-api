package com.riogandarilla.api.exception;

import org.springframework.http.HttpStatus;

public class WhatsAppIntegrationException extends ApiException {
    public WhatsAppIntegrationException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }
}
