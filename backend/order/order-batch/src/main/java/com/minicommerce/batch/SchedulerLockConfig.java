package com.minicommerce.batch;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * order-batch를 replica 2개 이상으로 스케일 아웃해도 IncompleteEventSweeper와
 * ExpiredReservationReleaser(inventory 모듈)가 인스턴스마다 중복 실행되지 않도록,
 * 이미 의존 중인 Redis를 분산 락 저장소로 사용한다(아키텍처 리뷰 지적사항).
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
class SchedulerLockConfig {

    @Bean
    LockProvider lockProvider(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockProvider(redisConnectionFactory, "order-batch");
    }
}
