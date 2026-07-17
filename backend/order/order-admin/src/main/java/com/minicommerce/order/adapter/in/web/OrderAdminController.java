package com.minicommerce.order.adapter.in.web;

import com.minicommerce.global.PageResult;
import com.minicommerce.order.application.port.in.CancelOrderUseCase;
import com.minicommerce.order.application.port.out.OrderRepository;
import com.minicommerce.order.domain.OrderStatus;
import com.minicommerce.order.domain.exception.OrderNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/orders")
@Transactional
public class OrderAdminController {

    private final OrderRepository orderRepository;
    private final CancelOrderUseCase cancelOrderUseCase;

    public OrderAdminController(OrderRepository orderRepository, CancelOrderUseCase cancelOrderUseCase) {
        this.orderRepository = orderRepository;
        this.cancelOrderUseCase = cancelOrderUseCase;
    }

    @GetMapping
    @Transactional(readOnly = true)
    PageResult<OrderResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PageResult<com.minicommerce.order.domain.Order> result =
                orderRepository.findAllPaged(status, q, page, size, sortBy, sortDir);
        return new PageResult<>(
                result.content().stream().map(OrderResponse::from).toList(),
                result.totalElements(),
                result.totalPages(),
                result.page(),
                result.size());
    }

    @PutMapping("/{orderId}/status")
    OrderResponse updateStatus(@PathVariable String orderId,
                               @Valid @RequestBody UpdateOrderStatusRequest request) {
        if (request.status() == OrderStatus.CANCELED) {
            // 취소는 PG 환불 + 재입고가 수반되므로 상태만 바꾸는 이 경로로 처리하면 안 된다 — 전용 취소 API로 유도.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CANCELED는 주문 취소 API(POST /{orderId}/cancel)를 사용하세요.");
        }
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.updateStatus(request.status());
        return OrderResponse.from(orderRepository.save(order));
    }

    @PostMapping("/{orderId}/cancel")
    OrderResponse cancel(@PathVariable String orderId,
                         @Valid @RequestBody AdminCancelOrderRequest request) {
        return OrderResponse.from(cancelOrderUseCase.cancelByAdmin(orderId, request.cancelReason()));
    }
}
