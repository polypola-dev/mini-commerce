package com.minicommerce.catalog;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/** catalog 모듈은 라이브러리라 부팅 클래스가 없어 @DataJpaTest가 찾을 설정 진입점을 테스트 소스에 둔다. */
@SpringBootConfiguration
@EnableAutoConfiguration
class CatalogTestApplication {
}
