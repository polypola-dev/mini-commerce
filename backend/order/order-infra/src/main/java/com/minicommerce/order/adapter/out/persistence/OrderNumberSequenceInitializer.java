package com.minicommerce.order.adapter.out.persistence;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 해당 날짜의 카운터 행을 새 트랜잭션에서 만드는 협력 빈(GH #19).
 *
 * <p>{@code REQUIRES_NEW}인 이유: 최초 주문 시 카운터 행 INSERT가 동시 요청과 경합하면 한쪽은
 * 중복키 예외를 받는데, 이를 <b>본 주문 저장 트랜잭션 안</b>에서 삽입·처리하면 그 트랜잭션이
 * rollback-only로 오염돼 이후 락 조회까지 실패한다. 그래서 삽입만 별도 트랜잭션으로 격리한다.
 *
 * <p>중복키 예외는 <b>여기서 잡지 않는다</b> — 실패한 삽입이 이 트랜잭션을 rollback-only로 만들어
 * 정상 return 후 커밋 시점에 다시 예외가 터지기 때문이다. 예외는 이 트랜잭션 경계 <b>바깥</b>
 * (호출자 {@link OrderNumberAdapter})에서 잡아 삼킨다.
 */
@Component
class OrderNumberSequenceInitializer {

    private final JpaOrderNumberSequenceRepository repository;

    OrderNumberSequenceInitializer(JpaOrderNumberSequenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createRow(LocalDate date) {
        repository.saveAndFlush(new OrderNumberSequenceJpaEntity(date));
    }
}
