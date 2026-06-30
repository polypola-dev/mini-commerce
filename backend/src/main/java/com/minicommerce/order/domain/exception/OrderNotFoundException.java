package com.minicommerce.order.domain.exception;

import com.minicommerce.global.BusinessException;

/** 주문을 찾을 수 없을 때 발생하는 도메인 예외. */
public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(String orderId) {
        super(OrderErrorCode.ORDER_NOT_FOUND, "Order not found: " + orderId);
    }
}
