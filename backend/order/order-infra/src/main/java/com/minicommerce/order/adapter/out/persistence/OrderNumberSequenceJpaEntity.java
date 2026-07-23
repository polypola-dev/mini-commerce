package com.minicommerce.order.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 일별(KST) 주문번호 일련번호 카운터(GH #19). 날짜(PK) 한 행이 그 날의 마지막 발급 번호를 들고 있고,
 * 채번은 이 행을 비관적 락(PESSIMISTIC_WRITE)으로 잠근 뒤 증가시켜 동시 주문에도 중복/스킵이
 * 없게 한다. 카운터 증가는 주문 저장과 같은 트랜잭션(OrderPersistenceService) 안에서 일어난다.
 */
@Entity
@Table(name = "order_number_sequences")
class OrderNumberSequenceJpaEntity {

    @Id
    private LocalDate orderDate;

    @Column(nullable = false)
    private long lastSeq;

    protected OrderNumberSequenceJpaEntity() {
    }

    OrderNumberSequenceJpaEntity(LocalDate orderDate) {
        this.orderDate = orderDate;
        this.lastSeq = 0L;
    }

    /** 다음 일련번호로 증가시키고 그 값을 반환한다. 락을 쥔 상태에서만 호출한다. */
    long next() {
        return ++lastSeq;
    }

    LocalDate getOrderDate() { return orderDate; }
    long getLastSeq() { return lastSeq; }
}
