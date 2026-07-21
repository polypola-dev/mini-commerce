package com.minicommerce.order.domain.exception;

/**
 * catalog 서비스에 연결할 수 없거나 서킷이 열려 상품/옵션 조회를 시도조차 못 할 때(D6).
 * order-api가 503으로 매핑한다 — {@link InventoryUnavailableException}과 같은 계약이며,
 * 주문 생성은 실패하지만 조회·기존 주문은 영향 없다(graceful degradation).
 *
 * <p>"상품이 없음"(catalog 404)은 정상 비즈니스 응답이라 이 예외가 아니라
 * {@code EntityNotFoundException} → 404로 남는다. 서킷 통계에서도 실패로 세지 않는다.
 */
public class CatalogUnavailableException extends RuntimeException {
    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
