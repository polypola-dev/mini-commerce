package com.minicommerce.order.domain.exception;

import com.minicommerce.global.BusinessException;

/** PG(Toss)가 환불(취소)을 거절했을 때 발생한다. 어댑터가 게이트웨이 에러 메시지를 실어 던진다. */
public class PaymentRefundFailedException extends BusinessException {
    public PaymentRefundFailedException(String message) {
        super(OrderErrorCode.PAYMENT_REFUND_FAILED, message);
    }
}
