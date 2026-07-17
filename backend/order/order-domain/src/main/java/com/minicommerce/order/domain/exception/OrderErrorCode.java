package com.minicommerce.order.domain.exception;

import com.minicommerce.global.ErrorCode;
import com.minicommerce.global.ErrorType;

/** order 컨텍스트의 에러 카탈로그. 순수 enum(Spring/JPA 의존 없음). */
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER_404", "주문을 찾을 수 없습니다.", ErrorType.NOT_FOUND),
    PAYMENT_AMOUNT_MISMATCH("ORDER_PAYMENT_AMOUNT_MISMATCH", "결제 금액이 주문 금액과 일치하지 않습니다.", ErrorType.INVALID),
    ORDER_ALREADY_PROCESSED("ORDER_ALREADY_PROCESSED", "이미 처리된 주문입니다.", ErrorType.CONFLICT),
    PAYMENT_CONFIRM_FAILED("ORDER_PAYMENT_CONFIRM_FAILED", "결제 승인에 실패했습니다.", ErrorType.INVALID),
    ORDER_CANCEL_NOT_ALLOWED("ORDER_CANCEL_NOT_ALLOWED", "취소할 수 없는 주문입니다.", ErrorType.CONFLICT),
    PAYMENT_REFUND_FAILED("ORDER_PAYMENT_REFUND_FAILED", "결제 취소(환불)에 실패했습니다.", ErrorType.INVALID);

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
