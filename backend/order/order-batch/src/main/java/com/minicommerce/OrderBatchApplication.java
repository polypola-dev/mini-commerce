package com.minicommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * order-batch 부팅 모듈의 진입점(MSA S4 → GH #3 S3).
 * IncompleteEventSweeper(@Scheduled)가 order-api/order-admin이 orderdb 아웃박스에 남긴
 * 미발행 이벤트를 스윕해 Kafka로 재시도 발행하고, InventoryEventKafkaConsumer가
 * inventory.reservation.expired를 구독해 주문을 EXPIRED로 전이한다.
 * 재고 만료 리퍼(ExpiredReservationReleaser)는 inventory 완전분리와 함께 inventory-api로 이관됐다.
 */
@Modulith
@EnableScheduling
@SpringBootApplication
public class OrderBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderBatchApplication.class, args);
    }
}
