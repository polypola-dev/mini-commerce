package com.minicommerce.global;

/**
 * 모든 도메인 비즈니스 예외의 베이스. Spring 컨텍스트 없이 동작해야 하므로 순수 RuntimeException을 상속한다.
 * ErrorCode를 보유해 전역 핸들러가 단일 진입점으로 처리할 수 있게 한다.
 */
public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
