package com.minicommerce.order.domain.exception;

import com.minicommerce.global.BusinessException;

/** PAID가 아닌 주문에 취소를 요청할 때 발생한다(PENDING_PAYMENT/CANCELED/SHIPPED 등). */
public class OrderCancelNotAllowedException extends BusinessException {
    public OrderCancelNotAllowedException(String orderId) {
        super(OrderErrorCode.ORDER_CANCEL_NOT_ALLOWED, "Order cannot be canceled: " + orderId);
    }
}
