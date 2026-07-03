package com.minicommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * shop-api는 order:order-domain에 컴파일 의존하지만(notification의 Kafka 이벤트 계약 때문,
 * ADR-005 S3-3b) order-infra(포트 어댑터)는 order-api로 이관돼 더 이상 없다. 기본
 * 컴포넌트스캔이 order.application의 OrderService(@Service)까지 주우면 ProductQueryPort 등
 * 어댑터 빈이 없어 기동이 깨지므로 패키지째 제외한다 — shop-api는 이벤트 레코드 타입만 필요하다.
 * OrderService를 클래스 리터럴로 직접 참조하는 방식은 order 모듈의 non-exposed 타입 의존이 돼
 * Modulith 위반으로 잡히므로(ModularityVerificationTest에서 실제 확인) 정규식 패턴으로 우회한다.
 * TypeExcludeFilter/AutoConfigurationExcludeFilter는 @SpringBootApplication이 기본으로 거는
 * 필터라, @ComponentScan을 직접 선언하면서 유실되지 않도록 그대로 재선언한다(Spring Boot 공식
 * 문서가 안내하는 패턴).
 */
@Modulith
@EnableAsync
@EnableScheduling
@SpringBootApplication
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.minicommerce\\.order\\.application\\..*")
})
public class MiniCommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiniCommerceApplication.class, args);
    }
}
