package com.minicommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;

/**
 * order-api 부팅 모듈의 진입점(MSA S3-3b, 스케줄링 분리는 S4).
 * order-domain/order-infra/inventory/shared-web의 빈을 기본 스캔 범위(com.minicommerce)로 조립한다.
 * inventory의 ExpiredReservationReleaser(@Scheduled)와 order-infra의
 * ReservationExpiredEventListener(@ApplicationModuleListener)는 빈으로는 함께 뜨지만, 이 프로세스는
 * @EnableScheduling/@EnableAsync를 켜지 않아 실제로 트리거되지 않는다 — 그 실행은 order-batch
 * 전담이다(ADR-005 S4). order-api는 OrderPlaced/OrderPaid 이벤트 발행(아웃박스 기록)을 위해
 * @Modulith와 이벤트 런타임만 유지한다.
 */
@Modulith
@SpringBootApplication
public class OrderApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApiApplication.class, args);
    }
}
