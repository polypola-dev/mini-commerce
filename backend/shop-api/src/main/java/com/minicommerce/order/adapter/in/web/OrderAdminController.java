package com.minicommerce.order.adapter.in.web;

import com.minicommerce.global.PageResult;
import com.minicommerce.order.application.port.out.OrderRepository;
import com.minicommerce.order.domain.exception.OrderNotFoundException;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@Transactional
public class OrderAdminController {

    private final OrderRepository orderRepository;

    public OrderAdminController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.updateStatus(request.status());
        return OrderResponse.from(orderRepository.save(order));
    }
}
