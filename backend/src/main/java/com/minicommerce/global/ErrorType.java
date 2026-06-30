package com.minicommerce.global;

/** 비즈니스 에러의 분류. HTTP 상태와의 매핑은 웹 계층(예외 핸들러)에서만 수행한다(common은 순수 유지). */
public enum ErrorType {
    NOT_FOUND,
    CONFLICT,
    INVALID,
    FORBIDDEN,
    INTERNAL
}
