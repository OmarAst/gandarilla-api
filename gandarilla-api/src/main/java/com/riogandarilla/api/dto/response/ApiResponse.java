package com.riogandarilla.api.dto.response;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpStatus;

public record ApiResponse<T>(
        MetaResponse meta,
        T data
) {
    public static <T> ApiResponse<T> success(T data) {
        return success(HttpStatus.OK, "Consulta realizada correctamente", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return success(HttpStatus.OK, message, data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return success(HttpStatus.CREATED, message, data);
    }

    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data) {
        return new ApiResponse<>(
                new MetaResponse(status.value(), status.getReasonPhrase(), message, requestId()),
                data
        );
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message, T data) {
        return success(status, message, data);
    }

    private static String requestId() {
        String requestId = ThreadContext.get("requestId");
        return requestId == null || requestId.isBlank() ? "unknown" : requestId;
    }
}
