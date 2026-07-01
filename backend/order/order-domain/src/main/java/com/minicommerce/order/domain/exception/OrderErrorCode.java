package com.minicommerce.order.domain.exception;

import com.minicommerce.global.ErrorCode;
import com.minicommerce.global.ErrorType;

/** order 컨텍스트의 에러 카탈로그. 순수 enum(Spring/JPA 의존 없음). */
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER_404", "주문을 찾을 수 없습니다.", ErrorType.NOT_FOUND);

    private final String code;
    private final String message;
    private final ErrorType type;

    OrderErrorCode(String code, String message, ErrorType type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }

    @Override
    public ErrorType getType() { return type; }
}
