package com.riogandarilla.api.exception;

import org.springframework.http.HttpStatus;

public class PdfGenerationException extends ApiException {
    public PdfGenerationException(String code, String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, code, message);
    }
}
