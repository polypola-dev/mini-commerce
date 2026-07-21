package com.minicommerce.order.domain.exception;

import com.minicommerce.global.BusinessException;
import com.minicommerce.order.domain.OrderStatus;

/** PAID가 아닌 주문에 취소를 요청할 때 발생한다(PENDING_PAYMENT/CANCELED/SHIPPED 등). */
public class OrderCancelNotAllowedException extends BusinessException {

    /**
     * 취소 시도 시점의 주문 상태. 오버셀 자동취소 리스너가 "이미 CANCELED(멱등 재전달)"과
     * "그 외 취소 불가(SHIPPED/DELIVERED 등 — 환불 미실행 사고)"를 구분하는 데 쓴다. null일 수 있다.
     */
    private final OrderStatus currentStatus;

    public OrderCancelNotAllowedException(String orderId) {
        this(orderId, null);
    }

    public OrderCancelNotAllowedException(String orderId, OrderStatus currentStatus) {
        super(OrderErrorCode.ORDER_CANCEL_NOT_ALLOWED, "Order cannot be canceled: " + orderId
                + (currentStatus != null ? " (status=" + currentStatus + ")" : ""));
        this.currentStatus = currentStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}
