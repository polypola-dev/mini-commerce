package com.minicommerce.order.domain.exception;

/**
 * 주문 생성 시 재고 부족(GH #3 S3). 과거 inventory 모듈의 동명 예외를 in-process로 받던 것을,
 * 원격 reserve의 409(out-of-stock)를 어댑터가 이 도메인 예외로 복원하는 구조로 대체했다.
 * BusinessException 체계가 아닌 이유: 프론트가 소비하는 기존 ProblemDetail(409, type=out-of-stock)
 * 응답 계약을 그대로 보존하기 위해 전용 어드바이스로 매핑한다(order-api InventorySagaExceptionHandler).
 */
public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String message) {
        super(message);
    }
}
