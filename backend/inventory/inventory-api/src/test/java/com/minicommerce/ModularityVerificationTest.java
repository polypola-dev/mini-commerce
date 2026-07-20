package com.minicommerce;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * inventory-api 클래스패스 기준 Spring Modulith 모듈 경계 검증(GH #3 S3).
 * order-api/order-batch/shop-api의 동명 테스트와 동일 패턴.
 */
class ModularityVerificationTest {

    @Test
    void verifyModularity() {
        ApplicationModules.of(InventoryApiApplication.class).verify();
    }
}
