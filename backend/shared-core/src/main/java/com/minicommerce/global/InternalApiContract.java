package com.minicommerce.global;

/**
 * 서비스간 {@code /internal/**} 호출의 전송 계약(B3, ADR-020).
 *
 * <p>발신측(catalog, order-infra의 RestClient 설정)과 수신측(shared-web의
 * {@code InternalAuthFilter})이 서로 다른 모듈이라 헤더 이름이 양쪽에 문자열로 흩어지기 쉽다.
 * 두 진영이 공통으로 의존하는 유일한 지점이 shared-core라 여기에 둔다 — 순수 상수라
 * "spring 의존 금지" 원칙과 충돌하지 않는다.
 */
public final class InternalApiContract {

    /** 서비스간 인증 키를 싣는 헤더. 값의 출처는 {@code INTERNAL_API_KEY}(CONFIGURATION.md). */
    public static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    private InternalApiContract() {
    }
}
