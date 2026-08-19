package com.riogandarilla.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riogandarilla.api.dto.response.ApiResponse;
import com.riogandarilla.api.dto.response.ErrorResponse;
import com.riogandarilla.api.utils.RequestIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                      String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse error = new ErrorResponse(
                request.getRequestURI(),
                code,
                status.getReasonPhrase(),
                List.of(message),
                RequestIdSupport.current(),
                OffsetDateTime.now()
        );
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(status, message, error));
    }
}
