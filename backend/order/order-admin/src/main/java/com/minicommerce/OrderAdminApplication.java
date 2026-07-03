package com.minicommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * order-admin 부팅 모듈의 진입점(MSA S4).
 * 관리자 주문 엔드포인트(OrderAdminController)만 서빙한다. 재고 리퍼/이벤트 스윕은 order-batch 담당.
 */
@SpringBootApplication
public class OrderAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderAdminApplication.class, args);
    }
}
