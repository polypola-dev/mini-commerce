package com.minicommerce.order.adapter.out.inventory;

import com.minicommerce.order.application.port.out.InventoryPort;
import com.minicommerce.order.domain.exception.InventoryUnavailableException;
import com.minicommerce.order.domain.exception.OutOfStockException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
 *
 * <p><b>서킷브레이커(D6)</b>: 서킷 실패로 세는 것은 {@code InventoryUnavailableException}
 * (연결 실패/타임아웃/5xx)뿐이다. 품절({@code OutOfStockException})과 예약 충돌·잘못된 요청
 * ({@code IllegalStateException})은 inventory-api가 <b>정상 동작 중</b>이라는 증거이므로
 * {@code ignoreExceptions}로 통계에서 제외한다. 이걸 빠뜨리면 인기 상품 하나가 품절된 것만으로
 * 서킷이 열려 멀쩡한 inventory-api로 가는 모든 주문이 끊긴다.
 *
 * <p>폴백은 서킷이 열린 경우({@link CallNotPermittedException})에만 걸고, 기존
 * {@code InventoryUnavailableException} 계약으로 감싸 던진다 — order-api의
 * {@code InventorySagaExceptionHandler}가 그대로 503으로 매핑한다.
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
    @CircuitBreaker(name = "inventory", fallbackMethod = "reserveFallback")
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
        } catch (HttpClientErrorException.BadRequest e) {
            // 본문 검증 실패 — inventory-api가 아니라 이쪽 요청 조립이 잘못됐다는 뜻이다.
            // InventoryUnavailableException으로 감싸면 재시도·보상이 붙는데 몇 번을 보내도
            // 같은 400이라 무의미하다. reservation-conflict와 같이 버그로 드러낸다.
            throw new IllegalStateException("Malformed reserve request for order " + orderId
                    + ": " + e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new InventoryUnavailableException("Inventory reserve failed for order " + orderId, e);
        }
    }

    StockHold reserveFallback(String orderId, List<StockItem> items, CallNotPermittedException cause) {
        throw new InventoryUnavailableException("Inventory circuit open for reserve of order " + orderId, cause);
    }

    @Override
    @CircuitBreaker(name = "inventory", fallbackMethod = "releaseFallback")
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

    void releaseFallback(String orderId, CallNotPermittedException cause) {
        throw new InventoryUnavailableException("Inventory circuit open for release of order " + orderId, cause);
    }

    @Override
    @CircuitBreaker(name = "inventory", fallbackMethod = "statusFallback")
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

    ReservationState statusFallback(String orderId, CallNotPermittedException cause) {
        throw new InventoryUnavailableException("Inventory circuit open for status of order " + orderId, cause);
    }

    // confirm/restock은 REST가 아니라 order.paid/order.canceled 발행으로 트리거된다(GH #3 S4).
}
