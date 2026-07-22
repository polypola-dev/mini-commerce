package com.minicommerce.order.adapter.out.persistence;

import com.minicommerce.order.domain.Order;
import com.minicommerce.order.domain.OrderLine;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 도메인 {@code Order} ↔ {@code OrderJpaEntity} 매핑. 라인 id를 보존해 재저장 시 merge가 되도록 한다.
 *  도메인은 String id를 쓰고 영속 엔티티는 uuid를 쓰므로, 이 매퍼가 String↔UUID 변환을 전담한다. */
@Component
class OrderPersistenceMapper {

    OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity(
                UUID.fromString(order.getId()), toUuid(order.getCustomerId()), order.getStatus(), order.getTotalAmount(), order.getCreatedAt(),
                order.getPaymentKey(), order.getShippingRecipient(), order.getShippingPhone(), order.getShippingAddress(),
                order.getShippingDetailAddress(), order.getShippingZipCode());
        for (OrderLine line : order.getLines()) {
            entity.addLine(new OrderLineJpaEntity(
                    line.getId(), UUID.fromString(line.getProductId()), line.getProductName(),
                    line.getUnitPrice(), line.getQuantity(), line.getSelectedOptionValue()));
        }
        return entity;
    }

    Order toDomain(OrderJpaEntity entity) {
        List<OrderLine> lines = entity.getLines().stream()
                .map(l -> OrderLine.reconstitute(l.getId(), l.getProductId().toString(), l.getProductName(),
                        l.getUnitPrice(), l.getQuantity(), l.getSelectedOptionValue()))
                .toList();
        return Order.reconstitute(
                entity.getId().toString(), toStr(entity.getCustomerId()), entity.getStatus(), entity.getTotalAmount(), entity.getCreatedAt(),
                entity.getPaymentKey(), entity.getShippingRecipient(), entity.getShippingPhone(), entity.getShippingAddress(),
                entity.getShippingDetailAddress(), entity.getShippingZipCode(), lines);
    }

    private static UUID toUuid(String value) {
        return value != null ? UUID.fromString(value) : null;
    }

    private static String toStr(UUID value) {
        return value != null ? value.toString() : null;
    }
}
