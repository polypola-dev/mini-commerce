package com.minicommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * inventory 완전분리 서비스(GH #3, 전략 c). 전용 inventorydb + 공유 Redis(stock:*, reservation:*)를
 * 소유한다. S1 스켈레톤 — 재고 조회/설정 REST는 S2, 예약 사가 API/이벤트/리퍼 이관은 S3.
 * 리퍼(ExpiredReservationReleaser)는 아직 order-batch 소유라 여기서는 app.batch.enabled를 켜지 않는다.
 */
@SpringBootApplication
public class InventoryApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApiApplication.class, args);
    }
}
