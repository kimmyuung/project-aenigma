package com.aenigma.domain.common.exception;

import lombok.Getter;

/**
 * 도메인 비즈니스 예외
 */
@Getter
public class DomainException extends RuntimeException {

    private final DomainErrorCode errorCode;

    public DomainException(DomainErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public DomainException(DomainErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DomainException(DomainErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
