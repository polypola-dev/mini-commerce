package com.minicommerce.order.adapter.out.inventory;

import com.minicommerce.order.application.port.out.InventoryPort;
import com.minicommerce.order.domain.exception.InventoryUnavailableException;
import com.minicommerce.order.domain.exception.OutOfStockException;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * inventory-api 원격 호출 어댑터(GH #3 S3) — 과거 in-process InventoryAdapter를 대체한다.
 * 에러 계약: 409의 ProblemDetail type으로 도메인 예외를 복원(out-of-stock → OutOfStockException),
 * 연결 실패/타임아웃/5xx → InventoryUnavailableException(order-api가 503으로 매핑).
 */
@Component
public class InventoryRestAdapter implements InventoryPort {

    private static final String OUT_OF_STOCK_TYPE = "out-of-stock";

    private final RestClient restClient;

    public InventoryRestAdapter(@Qualifier("inventorySagaRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    record ReservationRequest(String orderId, List<Item> items) {
        record Item(String productId, long quantity) {
        }
    }

    record ReservationResponse(String reservationId, String orderId, String status, Instant expiresAt) {
    }

    @Override
    public StockHold reserve(String orderId, List<StockItem> items) {
        ReservationRequest request = new ReservationRequest(orderId, items.stream()
                .map(item -> new ReservationRequest.Item(item.productId(), item.quantity()))
                .toList());
        try {
            ReservationResponse response = restClient.post()
                    .uri("/internal/inventory/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ReservationResponse.class);
            if (response == null) {
                throw new InventoryUnavailableException("Empty reserve response for order " + orderId, null);
            }
            return new StockHold(response.reservationId(), response.expiresAt(), items);
        } catch (HttpClientErrorException.Conflict e) {
            if (e.getResponseBodyAsString().contains(OUT_OF_STOCK_TYPE)) {
                throw new OutOfStockException("Out of stock for order " + orderId);
            }
            // reservation-conflict: 신규 orderId(UUID) 예약에서는 정상 흐름상 나오지 않는다.
            throw new IllegalStateException("Reservation conflict for order " + orderId
                    + ": " + e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new InventoryUnavailableException("Inventory reserve failed for order " + orderId, e);
        }
    }

    @Override
    public void release(String orderId) {
        try {
            restClient.delete()
                    .uri("/internal/inventory/reservations/{orderId}", orderId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpServerErrorException | ResourceAccessException e) {
            // 호출자(place 보상)가 로그 후 원 예외를 우선한다 — 리퍼 백스톱 전제.
            throw new InventoryUnavailableException("Inventory release failed for order " + orderId, e);
        }
    }

    @Override
    public ReservationState status(String orderId) {
        try {
            ReservationResponse response = restClient.get()
                    .uri("/internal/inventory/reservations/{orderId}", orderId)
                    .retrieve()
                    .body(ReservationResponse.class);
            if (response == null || response.status() == null) {
                return ReservationState.NOT_FOUND;
            }
            return ReservationState.valueOf(response.status());
        } catch (HttpClientErrorException.NotFound e) {
            return ReservationState.NOT_FOUND;
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new InventoryUnavailableException("Inventory status query failed for order " + orderId, e);
        }
    }

    // confirm/restock은 REST가 아니라 order.paid/order.canceled 발행으로 트리거된다(GH #3 S4).
}
