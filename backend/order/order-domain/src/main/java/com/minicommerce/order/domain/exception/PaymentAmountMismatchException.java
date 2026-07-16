package com.minicommerce.order.domain.exception;

import com.minicommerce.global.BusinessException;

/** 클라이언트가 보낸 결제 금액이 서버에 저장된 주문 금액과 다를 때 발생한다(금액 위변조 방지). */
public class PaymentAmountMismatchException extends BusinessException {
    public PaymentAmountMismatchException(String orderId) {
        super(OrderErrorCode.PAYMENT_AMOUNT_MISMATCH, "Payment amount mismatch for order: " + orderId);
    }
}
