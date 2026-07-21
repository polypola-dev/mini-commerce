package com.minicommerce.inventory.adapter.in.web;

import com.minicommerce.inventory.application.port.in.ReserveStockUseCase.ReserveCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * 예약 사가 POST 본문의 웹 계약. 필드 이름/구조는 {@link ReserveCommand}와 동일하게 유지한다 —
 * order-infra {@code InventoryRestAdapter}가 보내는 JSON이 그대로 바인딩돼야 한다.
 *
 * <p>포트 레코드를 직접 {@code @RequestBody}로 받지 않는 이유는 두 가지다. 검증 애너테이션이
 * application 레이어로 새지 않게 하고(shop-api/order-api의 {@code *Request} 레코드와 동일 관례),
 * 누락 필드를 경계에서 400으로 끊어 유즈케이스가 null을 만나지 않게 한다 — 이전에는
 * {@code items}가 null이면 {@code items().stream()}에서 NPE가 나 500으로 응답했다.
 */
record ReserveRequest(
        @NotBlank String orderId,
        @NotEmpty @Valid List<Item> items
) {

    record Item(
            @NotBlank String productId,
            @Positive long quantity
    ) {
    }

    ReserveCommand toCommand() {
        return new ReserveCommand(orderId, items.stream()
                .map(item -> new ReserveCommand.Item(item.productId(), item.quantity()))
                .toList());
    }
}
