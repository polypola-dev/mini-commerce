package com.minicommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * inventory 완전분리 서비스(GH #3, 전략 c). 전용 inventorydb + 공유 Redis(stock:*, reservation:*)를
 * 소유한다. 재고 조회/설정 REST(S2) + 예약 사가 REST/만료 리퍼/이벤트 외부화(S3)를 담당한다.
 * 리퍼(ExpiredReservationReleaser)는 app.batch.enabled=true로 이 프로세스가 소유한다
 * (order-batch에서 이관 — @EnableScheduling + SchedulerLockConfig도 함께).
 */
@EnableScheduling
@SpringBootApplication
public class InventoryApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApiApplication.class, args);
    }
}
