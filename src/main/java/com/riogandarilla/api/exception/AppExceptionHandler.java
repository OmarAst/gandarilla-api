package com.riogandarilla.api.exception;

import com.riogandarilla.api.dto.response.ApiResponse;
import com.riogandarilla.api.dto.response.ErrorResponse;
import com.riogandarilla.api.dto.response.MetaResponse;
import com.riogandarilla.api.utils.RequestIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.List;

@Log4j2
@RestControllerAdvice
public class AppExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleApiException(
            ApiException exception,
            HttpServletRequest request
    ) {
        if (exception.status().is5xxServerError()) {
            log.error("Error de servicio code={} path={}", exception.code(), request.getRequestURI(), exception);
        } else {
            log.warn("Solicitud rechazada code={} path={} message={}",
                    exception.code(), request.getRequestURI(), exception.getMessage());
        }
        return build(exception.status(), exception.code(), exception.getMessage(), request, exception.details());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La solicitud contiene datos inválidos", request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<String> details = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La solicitud contiene parámetros inválidos", request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        log.warn("JSON inválido path={}", request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "INVALID_JSON",
                "El cuerpo de la solicitud no es válido", request,
                List.of("Verifica el formato JSON y los valores numéricos enviados"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Uno o más parámetros son inválidos", request,
                List.of("Valor inválido para el parámetro: " + exception.getName()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "Método HTTP no permitido", request,
                List.of("Método recibido: " + exception.getMethod()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Tipo de contenido no soportado", request,
                List.of("Usa Content-Type: application/json"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "Recurso no encontrado", request, List.of("La ruta solicitada no existe"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn("Conflicto de integridad de datos path={}", request.getRequestURI());
        return build(HttpStatus.CONFLICT, "DATA_INTEGRITY_CONFLICT",
                "La operación entra en conflicto con los datos existentes", request,
                List.of("Verifica que el folio, la casa y los datos relacionados sean válidos"));
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDatabaseUnavailable(
            CannotGetJdbcConnectionException exception,
            HttpServletRequest request
    ) {
        log.error("Base de datos no disponible path={}", request.getRequestURI(), exception);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_UNAVAILABLE",
                "La base de datos no está disponible en este momento", request,
                List.of("Verifica la conexión de PostgreSQL y las variables DB_URL, DB_USERNAME y DB_PASSWORD"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleGeneric(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Error inesperado path={}", request.getRequestURI(), exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrió un error inesperado", request,
                List.of("Consulta el requestId en los logs del servidor"));
    }

    private ResponseEntity<ApiResponse<ErrorResponse>> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<String> details
    ) {
        String requestId = RequestIdSupport.current();
        ErrorResponse error = new ErrorResponse(
                request.getRequestURI(),
                code,
                status.getReasonPhrase(),
                details,
                requestId,
                OffsetDateTime.now()
        );
        ApiResponse<ErrorResponse> response = new ApiResponse<>(
                new MetaResponse(status.value(), status.getReasonPhrase(), message, requestId),
                error
        );
        return ResponseEntity.status(status).body(response);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
