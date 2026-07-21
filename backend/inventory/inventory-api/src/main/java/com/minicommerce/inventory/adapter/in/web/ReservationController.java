package com.minicommerce.inventory.adapter.in.web;

import com.minicommerce.inventory.application.port.in.GetReservationUseCase;
import com.minicommerce.inventory.application.port.in.ReleaseReservationUseCase;
import com.minicommerce.inventory.application.port.in.ReservationView;
import com.minicommerce.inventory.application.port.in.ReserveStockUseCase;
import com.minicommerce.inventory.application.port.in.ReserveStockUseCase.ReserveResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * order↔inventory 분산 사가의 동기 REST 경계(GH #3 S3). 전부 내부 전용(/internal — ingress 비노출).
 *
 * <ul>
 *   <li>POST: 예약(멱등 키=orderId). 201 신규 / 200 재시도 수렴 / 409 품절·재예약 불가</li>
 *   <li>DELETE: 보상 release — 항상 204(멱등, 보상 호출자는 성공/무의미를 구분할 필요 없음)</li>
 *   <li>GET: 상태 조회(order-api 결제 승인 전 사전 가드)</li>
 * </ul>
 *
 * <p>confirm/restock은 REST가 아니라 order.paid/order.canceled Kafka 구독으로 처리한다
 * (GH #3 S4 코레오그래피 — {@code OrderEventKafkaConsumer}).
 */
@RestController
@RequestMapping("/internal/inventory/reservations")
class ReservationController {

    private final ReserveStockUseCase reserveStockUseCase;
    private final ReleaseReservationUseCase releaseReservationUseCase;
    private final GetReservationUseCase getReservationUseCase;

    ReservationController(
            ReserveStockUseCase reserveStockUseCase,
            ReleaseReservationUseCase releaseReservationUseCase,
            GetReservationUseCase getReservationUseCase
    ) {
        this.reserveStockUseCase = reserveStockUseCase;
        this.releaseReservationUseCase = releaseReservationUseCase;
        this.getReservationUseCase = getReservationUseCase;
    }

    @PostMapping
    ResponseEntity<ReservationView> reserve(@Valid @RequestBody ReserveRequest request) {
        ReserveResult result = reserveStockUseCase.reserve(request.toCommand());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.view());
    }

    @DeleteMapping("/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void release(@PathVariable String orderId) {
        releaseReservationUseCase.release(orderId);
    }

    @GetMapping("/{orderId}")
    ResponseEntity<ReservationView> get(@PathVariable String orderId) {
        return getReservationUseCase.get(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
