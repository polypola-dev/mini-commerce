package com.minicommerce.order.domain;

import com.minicommerce.order.domain.exception.OrderCancelNotAllowedException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 주문 애그리거트 루트. 순수 POJO — JPA/Spring 등 기술 의존을 갖지 않는다.
 * 영속성 매핑은 adapter.out.persistence 의 JpaEntity + Mapper가 담당한다.
 */
public class Order {
    private final String id;
    // 고객 노출용 표시 전용 번호(ORD-YYYYMMDD-NNNN, GH #19). PK(UUID)와 분리 — 조회/인증/권한
    // 판단의 키로는 절대 쓰지 않는다(URL 노출 시 IDOR성 열거 방지). 채번은 DB 시퀀스가 필요해
    // 생성 시점엔 null이고, 영속 단계(OrderPersistenceService)에서 assignOrderNumber로 채운다.
    private String orderNumber;
    private final String customerId;
    private OrderStatus status;
    private final BigDecimal totalAmount;
    private final Instant createdAt;
    private String paymentKey;

    private final String shippingRecipient;
    private final String shippingPhone;
    private final String shippingAddress;
    private final String shippingDetailAddress;
    private final String shippingZipCode;

    private final List<OrderLine> lines;

    public Order(String id, String customerId, List<OrderLineDraft> lineDrafts) {
        this(id, customerId, lineDrafts, null, null, null, null, null);
    }

    public Order(String id, String customerId, List<OrderLineDraft> lineDrafts,
                 String shippingRecipient, String shippingPhone,
                 String shippingAddress, String shippingDetailAddress, String shippingZipCode) {
        this.id = id;
        this.customerId = customerId;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.createdAt = Instant.now();
        this.totalAmount = lineDrafts.stream()
                .map(OrderLineDraft::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.lines = lineDrafts.stream()
                .map(draft -> new OrderLine(draft.productId(), draft.productName(), draft.unitPrice(), draft.quantity(), draft.selectedOptionValue()))
                .toList();
        this.shippingRecipient = shippingRecipient;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
        this.shippingDetailAddress = shippingDetailAddress;
        this.shippingZipCode = shippingZipCode;
    }

    private Order(String id, String orderNumber, String customerId, OrderStatus status, BigDecimal totalAmount, Instant createdAt,
                  String paymentKey, String shippingRecipient, String shippingPhone, String shippingAddress,
                  String shippingDetailAddress, String shippingZipCode, List<OrderLine> lines) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.paymentKey = paymentKey;
        this.shippingRecipient = shippingRecipient;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
        this.shippingDetailAddress = shippingDetailAddress;
        this.shippingZipCode = shippingZipCode;
        this.lines = lines;
    }

    /** 영속성에서 도메인 객체를 복원할 때만 사용 (Mapper 전용). */
    public static Order reconstitute(String id, String orderNumber, String customerId, OrderStatus status, BigDecimal totalAmount,
                                     Instant createdAt, String paymentKey, String shippingRecipient, String shippingPhone,
                                     String shippingAddress, String shippingDetailAddress, String shippingZipCode,
                                     List<OrderLine> lines) {
        return new Order(id, orderNumber, customerId, status, totalAmount, createdAt, paymentKey, shippingRecipient, shippingPhone,
                shippingAddress, shippingDetailAddress, shippingZipCode, lines);
    }

    public String getId() { return id; }
    public String getOrderNumber() { return orderNumber; }

    /**
     * 표시 전용 주문번호를 부여한다(영속 단계 전용, GH #19). 채번은 DB 시퀀스가 필요해 생성 시점엔
     * 비어 있고, 저장 트랜잭션 안에서 한 번만 채운다. 이미 부여됐으면 무시해 재저장 시 번호가 바뀌지 않는다.
     */
    public void assignOrderNumber(String orderNumber) {
        if (this.orderNumber == null) {
            this.orderNumber = orderNumber;
        }
    }
    public String getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderLine> getLines() { return List.copyOf(lines); }
    public String getShippingRecipient() { return shippingRecipient; }
    public String getShippingPhone() { return shippingPhone; }
    public String getShippingAddress() { return shippingAddress; }
    public String getShippingDetailAddress() { return shippingDetailAddress; }
    public String getShippingZipCode() { return shippingZipCode; }
    public String getPaymentKey() { return paymentKey; }

    public void markPaid(String paymentKey) {
        if (status == OrderStatus.PENDING_PAYMENT) {
            status = OrderStatus.PAID;
            this.paymentKey = paymentKey;
        }
    }

    /** 결제 완료된 주문을 취소한다(PG 환불 + 재입고 이후 호출). PAID가 아니면 취소 불가. */
    public void markCanceled() {
        if (status != OrderStatus.PAID) {
            throw new OrderCancelNotAllowedException(id, status);
        }
        status = OrderStatus.CANCELED;
    }

    /** 결제 대기 중 재고 예약이 만료됐을 때(이탈/타임아웃) 리퍼가 호출한다. 이미 결제된 주문은 건드리지 않는다. */
    public void markExpired() {
        if (status == OrderStatus.PENDING_PAYMENT) {
            status = OrderStatus.EXPIRED;
        }
    }

    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
    }
}
