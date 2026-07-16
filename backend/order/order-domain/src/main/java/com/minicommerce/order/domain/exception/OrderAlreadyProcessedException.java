package com.minicommerce.order.domain.exception;

import com.minicommerce.global.BusinessException;

/** PENDING_PAYMENT가 아닌 주문에 결제 승인을 재요청할 때 발생한다(멱등/리플레이 가드). */
public class OrderAlreadyProcessedException extends BusinessException {
    public OrderAlreadyProcessedException(String orderId) {
        super(OrderErrorCode.ORDER_ALREADY_PROCESSED, "Order already processed: " + orderId);
    }
}
