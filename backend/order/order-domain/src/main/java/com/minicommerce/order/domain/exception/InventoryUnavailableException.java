package com.minicommerce.order.domain.exception;

/**
 * inventory 서비스에 연결할 수 없거나(다운/타임아웃) 5xx를 반환할 때(GH #3 S3).
 * order-api가 503으로 매핑한다 — 주문 생성/결제는 실패하지만 조회·기존 주문은 영향 없다
 * (graceful degradation).
 */
public class InventoryUnavailableException extends RuntimeException {
    public InventoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
