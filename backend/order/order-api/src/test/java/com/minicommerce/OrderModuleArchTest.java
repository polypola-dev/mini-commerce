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
}
