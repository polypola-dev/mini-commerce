package com.minicommerce.catalog;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("findByActiveTrueOrderByNameAsc: active=true인 상품만 이름 오름차순으로 반환")
    void findByActiveTrueOrderByNameAsc_returnsOnlyActiveProductsSortedByName() {
        // given: active 상품 2개 + inactive 상품 1개 저장
        // Product 생성자는 항상 active=true로 초기화하므로 JPQL 직접 업데이트로 inactive 설정
        productRepository.save(new Product("p1", "사과", "새콤달콤한 과일", BigDecimal.valueOf(2000), 20, "img2.jpg"));
        productRepository.save(new Product("p2", "바나나", "달콤한 과일", BigDecimal.valueOf(1500), 10, "img1.jpg"));
        productRepository.save(new Product("p3", "딸기", "붉은 딸기", BigDecimal.valueOf(3000), 5, "img3.jpg"));

        // p3를 inactive로 직접 변경
        em.createQuery("UPDATE Product p SET p.active = false WHERE p.id = 'p3'").executeUpdate();
        em.flush();
        em.clear();

        // when
        List<Product> result = productRepository.findByActiveTrueOrderByNameAsc();

        // then: active=true인 상품만 이름 오름차순 정렬 (바나나 < 사과 유니코드 순)
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getName)
                .containsExactly("바나나", "사과");
    }

    @Test
    @DisplayName("searchActive: name 또는 description에 검색어 포함 시 반환 (대소문자 무관, inactive 상품 제외)")
    void searchActive_returnsMatchingActiveProducts() {
        // given
        // p1: name에 "Apple" 포함
        productRepository.save(new Product("p1", "Apple Juice", "Fresh fruit drink", BigDecimal.valueOf(2000), 10, "img1.jpg"));
        // p2: description에 "apple" 포함
        productRepository.save(new Product("p2", "Grape Jam", "Sweet spread with apple flavor", BigDecimal.valueOf(3000), 5, "img2.jpg"));
        // p3: "apple" 미포함 + inactive → 결과에서 제외되어야 함
        productRepository.save(new Product("p3", "Banana", "Yellow fruit", BigDecimal.valueOf(1000), 20, "img3.jpg"));

        // p3를 inactive로 설정
        em.createQuery("UPDATE Product p SET p.active = false WHERE p.id = 'p3'").executeUpdate();
        em.flush();
        em.clear();

        // when: 소문자 "apple"로 검색 (name의 "Apple"과 description의 "apple" 모두 매칭되어야 함)
        List<Product> result = productRepository.searchActive("apple");

        // then: p1 (name 매칭) + p2 (description 매칭), inactive p3는 제외
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getId)
                .containsExactlyInAnyOrder("p1", "p2");
    }

    @Test
    @DisplayName("searchActive: 일치하는 결과가 없으면 빈 리스트 반환")
    void searchActive_returnsEmptyList_whenNoMatch() {
        // given: 검색어와 무관한 상품만 저장
        productRepository.save(new Product("p1", "Apple", "Fresh fruit", BigDecimal.valueOf(2000), 10, "img1.jpg"));

        // when: 존재하지 않는 검색어로 조회
        List<Product> result = productRepository.searchActive("존재하지않는상품xyz");

        // then
        assertThat(result).isEmpty();
    }
}
