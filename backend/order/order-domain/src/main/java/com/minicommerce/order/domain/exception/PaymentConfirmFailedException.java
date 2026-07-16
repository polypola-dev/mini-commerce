package com.minicommerce.order.domain.exception;

import com.minicommerce.global.BusinessException;

/** PG(Toss)가 결제 승인을 거절했을 때 발생한다. 어댑터가 게이트웨이 에러 메시지를 실어 던진다. */
public class PaymentConfirmFailedException extends BusinessException {
    public PaymentConfirmFailedException(String message) {
        super(OrderErrorCode.PAYMENT_CONFIRM_FAILED, message);
    }
}
