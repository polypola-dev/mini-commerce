package com.minicommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * order-batch 부팅 모듈의 최소 진입점(Phase 6 스켈레톤).
 * 실제 배치 Job 스캐폴드, DB·이벤트 설정은 후속 작업에서 채운다.
 */
@SpringBootApplication
public class OrderBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderBatchApplication.class, args);
    }
}
