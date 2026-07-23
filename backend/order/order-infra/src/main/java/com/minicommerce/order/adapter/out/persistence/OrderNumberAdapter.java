package com.minicommerce.order.adapter.out.persistence;

import com.minicommerce.order.application.port.out.OrderNumberPort;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * {@link OrderNumberPort} 구현(GH #19) — {@code ORD-YYYYMMDD-NNNN} 형식의 표시 전용 주문번호를
 * 발급한다. 날짜는 주문 생성 시각을 KST로 환산한 값이고, 일련번호는 그 날짜의 카운터 행을
 * 비관적 락으로 잠근 뒤 1 증가시킨 값이라 동시 주문에도 중복·스킵이 없다.
 *
 * <p>이 메서드는 {@code OrderPersistenceService}의 저장 트랜잭션 안에서 호출된다 — 잠근 행은
 * 그 트랜잭션이 끝날 때까지 유지되고, 저장이 롤백되면 카운터 증가도 함께 되돌아간다.
 */
@Component
class OrderNumberAdapter implements OrderNumberPort {

    // "하루"의 경계는 KST 자정 기준으로 통일한다(GH #19).
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JpaOrderNumberSequenceRepository repository;
    private final OrderNumberSequenceInitializer initializer;

    OrderNumberAdapter(JpaOrderNumberSequenceRepository repository, OrderNumberSequenceInitializer initializer) {
        this.repository = repository;
        this.initializer = initializer;
    }

    @Override
    public String generate(Instant createdAt) {
        LocalDate date = LocalDate.ofInstant(createdAt, KST);
        // 흔한 경로(그날 행이 이미 있음)는 락 조회 한 번으로 끝난다 — 추가 커넥션을 쓰지 않는다.
        // 그날 첫 주문일 때만 별도 트랜잭션(REQUIRES_NEW)에서 행을 선확보(경합 시 중복은 흡수)한 뒤
        // 다시 잠근다. 이렇게 해야 정상 경로에서 커넥션을 이중 점유하지 않는다.
        OrderNumberSequenceJpaEntity sequence = repository.findForUpdate(date).orElse(null);
        if (sequence == null) {
            // 그날 첫 채번 — 별도 트랜잭션(REQUIRES_NEW)에서 행을 만든다. 동시 첫 채번이 겹치면 한쪽은
            // 중복키로 실패하는데, 그 예외는 이 트랜잭션 경계 바깥인 여기서 잡아 삼킨다(본 트랜잭션은
            // 오염되지 않는다). 그 뒤 다시 잠그면 누가 만들었든 행이 존재한다.
            try {
                initializer.createRow(date);
            } catch (DataIntegrityViolationException concurrentCreateWon) {
                // 동시 요청이 먼저 같은 날짜 행을 만들었다 — 아래 재조회로 그 행을 잠근다.
            }
            sequence = repository.findForUpdate(date)
                    .orElseThrow(() -> new IllegalStateException(
                            "order number sequence row missing for date=" + date + " after createRow"));
        }
        long seq = sequence.next();
        // 카운터 엔티티는 현재 영속성 컨텍스트에서 관리되므로 트랜잭션 커밋 시 lastSeq가 flush된다.
        // 4자리 제로패딩(하루 1만건 초과 시 자릿수는 자연스럽게 늘어난다).
        return "ORD-" + date.format(DATE_PREFIX) + "-" + String.format("%04d", seq);
    }
}
