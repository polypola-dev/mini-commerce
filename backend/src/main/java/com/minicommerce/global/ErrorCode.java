package com.minicommerce.global;

/** 각 도메인의 에러 코드 Enum이 구현하는 규격. 순수 인터페이스(Spring/JPA 의존 없음). */
public interface ErrorCode {
    String getCode();      // 시스템 에러 코드 (예: "ORDER_404")
    String getMessage();   // 기본 에러 메시지
    ErrorType getType();   // HTTP 상태 매핑용 분류
}
