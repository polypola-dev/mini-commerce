package com.minicommerce.order.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaOrderNumberSequenceRepository extends JpaRepository<OrderNumberSequenceJpaEntity, LocalDate> {

    /**
     * 해당 날짜의 카운터 행을 비관적 쓰기 락으로 조회한다(SELECT ... FOR UPDATE). 같은 날 동시
     * 채번을 이 락으로 직렬화해 일련번호 충돌을 막는다. H2·Postgres 모두 행 락을 지원한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM OrderNumberSequenceJpaEntity s WHERE s.orderDate = :date")
    Optional<OrderNumberSequenceJpaEntity> findForUpdate(@Param("date") LocalDate date);
}
