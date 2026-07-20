package com.minicommerce;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * order-domain 모듈의 헥사고날 경계를 강제한다(ADR-004 Phase 7, ADR-005 S3-3c로 order-api 이관).
 * order-api가 order-domain/order-infra를 모두 의존하므로 이 모듈의 테스트 클래스패스에서
 * 전체 컴파일 결과물을 스캔해 검사한다.
 */
@AnalyzeClasses(packages = "com.minicommerce")
class OrderModuleArchTest {

    @ArchTest
    static final ArchRule order_domain_and_application_must_not_depend_on_jpa_or_web =
            noClasses().that().resideInAnyPackage(
                    "com.minicommerce.order.domain..",
                    "com.minicommerce.order.application.."
            ).should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.springframework.web.."
            );

    @ArchTest
    static final ArchRule order_domain_must_not_depend_on_application_or_adapter =
            noClasses().that().resideInAPackage("com.minicommerce.order.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.minicommerce.order.application..",
                            "com.minicommerce.order.adapter.."
                    );

    @ArchTest
    static final ArchRule order_application_must_not_depend_on_adapter =
            noClasses().that().resideInAPackage("com.minicommerce.order.application..")
                    .should().dependOnClassesThat().resideInAPackage("com.minicommerce.order.adapter..");

    // inventory 완전분리(GH #3 S3)의 "order는 inventory-core에 컴파일 의존하지 않는다" 경계는
    // ArchUnit이 아니라 Gradle 모듈 그래프가 강제한다 — order-* 모듈이 project(':inventory:inventory-core')를
    // 선언하지 않아 core 클래스가 컴파일 클래스패스에 아예 없다. 패키지 기반 ArchUnit 규칙으로는
    // 이 경계를 표현할 수 없다: inventory-core와 inventory-events(만료 이벤트 계약, order가 정당하게
    // 소비)가 같은 com.minicommerce.inventory 패키지를 공유하기 때문이다.
}
