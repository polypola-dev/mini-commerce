package com.minicommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * order-batch 부팅 모듈의 진입점(MSA S4).
 * inventory의 ExpiredReservationReleaser(@Scheduled)와 order-infra의
 * ReservationExpiredEventListener(@ApplicationModuleListener, 비동기)를 이 프로세스가 전담
 * 실행한다 — order-api는 고객/관리자 웹 트래픽에만 집중하도록 두 애너테이션을 여기로 옮겼다.
 * IncompleteEventSweeper가 order-api에서 발행된 미발행(outstanding) 이벤트도 함께 스윕한다.
 */
@Modulith
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class OrderBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderBatchApplication.class, args);
    }
}
