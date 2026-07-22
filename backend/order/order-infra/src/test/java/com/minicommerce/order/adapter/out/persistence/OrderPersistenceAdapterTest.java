package com.minicommerce.order.adapter.out.persistence;

import com.minicommerce.order.application.port.out.OrderRepository;
import com.minicommerce.order.domain.Order;
import com.minicommerce.order.domain.OrderLineDraft;
import com.minicommerce.order.domain.OrderStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 1(도메인 순수화: POJO + JpaEntity 분리) 리팩터링의 안전망.
 * 도메인 저장/조회 라운드트립과 상태 변경 후 재저장 시 라인 보존을 검증한다.
 * 이 테스트는 리팩터링 전/후 모두 통과해야 한다(도메인 API 기준 검증).
 */
@DataJpaTest
@Import({OrderPersistenceAdapter.class, OrderPersistenceMapper.class})
class OrderPersistenceAdapterTest {

    // 영속 컬럼이 uuid로 전환됐으므로(GH #20) 매퍼가 UUID.fromString으로 변환한다 → 유효 UUID를 쓴다.
    // 도메인 Order.id/customerId는 String 그대로라 getId()는 넘긴 문자열을 그대로 돌려준다.
    private static final String ORDER_1 = "00000000-0000-7000-8000-0000000000e1";
    private static final String ORDER_2 = "00000000-0000-7000-8000-0000000000e2";
    private static final String ORDER_3 = "00000000-0000-7000-8000-0000000000e3";
    private static final String CUST_1 = "00000000-0000-7000-8000-0000000000c1";
    private static final String CUST_2 = "00000000-0000-7000-8000-0000000000c2";
    private static final String PROD_1 = "00000000-0000-7000-8000-0000000000a1";
    private static final String PROD_2 = "00000000-0000-7000-8000-0000000000a2";

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager em;

    private Order newOrder(String id, String customerId) {
        return new Order(
                id, customerId,
                List.of(
                        new OrderLineDraft(PROD_1, "사과", BigDecimal.valueOf(2000), 2L, null),
                        new OrderLineDraft(PROD_2, "바나나", BigDecimal.valueOf(1500), 1L, "한 송이")
                ),
                "받는사람", "010-0000-0000", "서울시 강남구", "101동 101호", "12345"
        );
    }

    @Test
    @DisplayName("save → findById: 주문과 주문라인이 그대로 저장/조회된다")
    void save_and_findById_roundTrip() {
        orderRepository.save(newOrder(ORDER_1, CUST_1));
        em.flush();
        em.clear();

        Optional<Order> found = orderRepository.findById(ORDER_1);

        assertThat(found).isPresent();
        Order order = found.get();
        assertThat(order.getCustomerId()).isEqualTo(CUST_1);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        // 2000*2 + 1500*1 = 5500
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(5500));
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getShippingRecipient()).isEqualTo("받는사람");
        assertThat(order.getShippingZipCode()).isEqualTo("12345");
        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getLines()).extracting(l -> l.getProductId())
                .containsExactlyInAnyOrder(PROD_1, PROD_2);
        assertThat(order.getLines()).extracting(l -> l.getSelectedOptionValue())
                .containsExactlyInAnyOrder(null, "한 송이");
    }

    @Test
    @DisplayName("findAllByCustomerId: 해당 고객의 주문만 조회된다")
    void findAllByCustomerId() {
        orderRepository.save(newOrder(ORDER_1, CUST_1));
        orderRepository.save(newOrder(ORDER_2, CUST_1));
        orderRepository.save(newOrder(ORDER_3, CUST_2));
        em.flush();
        em.clear();

        List<Order> result = orderRepository.findAllByCustomerId(CUST_1);

        assertThat(result).extracting(Order::getId)
                .containsExactlyInAnyOrder(ORDER_1, ORDER_2);
    }

    @Test
    @DisplayName("상태 변경 후 재저장: 상태가 갱신되고 주문라인이 유실되지 않는다 (merge/orphan 회귀 방지)")
    void updateStatus_thenSave_preservesLines() {
        orderRepository.save(newOrder(ORDER_1, CUST_1));
        em.flush();
        em.clear();

        Order loaded = orderRepository.findById(ORDER_1).orElseThrow();
        loaded.updateStatus(OrderStatus.SHIPPED);
        orderRepository.save(loaded);
        em.flush();
        em.clear();

        Order reloaded = orderRepository.findById(ORDER_1).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(reloaded.getLines()).hasSize(2);
        assertThat(reloaded.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(5500));
    }
}
