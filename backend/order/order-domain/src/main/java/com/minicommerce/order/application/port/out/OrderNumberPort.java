package com.minicommerce.order.application.port.out;

import java.time.Instant;

/**
 * 고객 노출용 표시 전용 주문번호 채번 포트(GH #19). 구현체(order-infra)가 "KST 하루 단위로
 * 리셋되는 일련번호"를 동시성 안전하게 발급하고 {@code ORD-YYYYMMDD-NNNN} 형식 문자열로 반환한다.
 *
 * <p>{@code createdAt}(주문 생성 시각)을 넘겨 번호의 날짜 접두사가 주문 시각과 일관되도록 한다
 * (채번 시점이 아니라 주문 시점 기준). 자정 경계에서 접두사가 흔들리지 않게 하기 위함이다.
 */
public interface OrderNumberPort {
    String generate(Instant createdAt);
}
