package com.minicommerce.inventory.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * inventory-api를 replica 2개 이상으로 스케일 아웃해도 ExpiredReservationReleaser(리퍼)가
 * 인스턴스마다 중복 실행되지 않도록 Redis를 분산 락 저장소로 사용한다(GH #3 S3 — 리퍼가
 * order-batch에서 이관되면서 LockProvider 소유권도 함께 왔다. 락 키 prefix가 달라 과거
 * order-batch 락과 충돌하지 않는다).
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
class SchedulerLockConfig {

    @Bean
    LockProvider lockProvider(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockProvider(redisConnectionFactory, "inventory-api");
    }
}
