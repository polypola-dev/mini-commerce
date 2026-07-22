package com.minicommerce.inventory;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {
    @Id
    private UUID id;

    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private Instant expiresAt;

    // EAGER: inventory 완전분리(GH #3 S3) 후 reserve/restock는 트랜잭션 없이 실행되므로(reserve는
    // "원장 커밋 → Redis Lua" 순서 계약상 트랜잭션으로 감쌀 수 없다), 세션 밖에서 lines를 읽을 때
    // LazyInitializationException이 난다. 예약당 라인 수가 적어 EAGER 비용이 무의미하다.
    @ElementCollection(fetch = FetchType.EAGER)
    private List<ReservationLine> lines = new ArrayList<>();

    protected InventoryReservation() {
    }

    public InventoryReservation(UUID id, UUID orderId, Instant expiresAt, List<ReservationLine> lines) {
        this.id = id;
        this.orderId = orderId;
        this.expiresAt = expiresAt;
        this.lines = new ArrayList<>(lines);
        this.status = ReservationStatus.RESERVED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public List<ReservationLine> getLines() {
        return List.copyOf(lines);
    }

    public void confirm() {
        if (status == ReservationStatus.RESERVED) {
            status = ReservationStatus.CONFIRMED;
        }
    }

    /**
     * 결제가 이긴 경합(payment-wins, GH #3 S4/D-C)에서 RELEASED/EXPIRED 예약을 CONFIRMED로 강제한다.
     * 결제가 이미 승인된 주문이므로 재고 재차감(호출자 Lua)과 함께 원장을 확정 상태로 되돌린다.
     */
    public void forceConfirm() {
        status = ReservationStatus.CONFIRMED;
    }

    /**
     * 결제가 이긴 경합인데 재고가 이미 다른 주문에 채여 force-confirm이 불가능한 경우 예약을 OVERSOLD로 표시한다
     * (payment-wins force-confirm이 오버셀을 만난 상태 — 이후 주문은 정직하게 자동 취소+환불로 처리한다).
     */
    public void markOversold() {
        status = ReservationStatus.OVERSOLD;
    }

    public void release() {
        if (status == ReservationStatus.RESERVED) {
            status = ReservationStatus.RELEASED;
        }
    }

    /**
     * 확정된 재고를 취소로 되돌린다. 이미 RESTOCKED면 이중 복원 방지를 위해 no-op(false 반환).
     * OVERSOLD 예약도 no-op(false) — force-confirm이 재고 부족으로 DECRBY를 아예 하지 않았으므로
     * 되돌려줄 재고가 없다(order.canceled 소비 시 IllegalStateException으로 인한 컨슈머 무한 재시도 방지).
     */
    public boolean restock() {
        if (status == ReservationStatus.RESTOCKED || status == ReservationStatus.OVERSOLD) {
            return false;
        }
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot restock reservation in status: " + status);
        }
        status = ReservationStatus.RESTOCKED;
        return true;
    }
}
