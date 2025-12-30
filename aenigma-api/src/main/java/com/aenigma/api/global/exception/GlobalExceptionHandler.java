package com.aenigma.api.global.exception;

import com.aenigma.domain.common.exception.DomainErrorCode;
import com.aenigma.domain.common.exception.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 전역 예외 핸들러
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * BusinessException 처리 (API 모듈)
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode, e.getMessage());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    /**
     * DomainException 처리 (Domain 모듈)
     */
    @ExceptionHandler(DomainException.class)
    protected ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
        log.warn("DomainException: {} (code={})", e.getMessage(), e.getErrorCode().getCode());
        DomainErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(e.getMessage())
                .status(errorCode.getStatus())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation error: {}", e.getMessage());
        BindingResult bindingResult = e.getBindingResult();

        List<ErrorResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .value(error.getRejectedValue() != null ? error.getRejectedValue().toString() : "")
                        .reason(error.getDefaultMessage())
                        .build())
                .toList();

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, fieldErrors);
        return new ResponseEntity<>(response, ErrorCode.INVALID_INPUT_VALUE.getStatus());
    }

    /**
     * HTTP Method 오류 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.warn("Method not allowed: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED);
        return new ResponseEntity<>(response, ErrorCode.METHOD_NOT_ALLOWED.getStatus());
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getMessage());
        return new ResponseEntity<>(response, ErrorCode.INVALID_INPUT_VALUE.getStatus());
    }

    /**
     * IllegalStateException 처리
     */
    @ExceptionHandler(IllegalStateException.class)
    protected ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.warn("IllegalStateException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getMessage());
        return new ResponseEntity<>(response, ErrorCode.INVALID_INPUT_VALUE.getStatus());
    }

    /**
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(response, ErrorCode.INTERNAL_SERVER_ERROR.getStatus());
    }
}
