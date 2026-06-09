package com.minicommerce.order.adapter.in.web;

import com.minicommerce.order.application.port.out.OrderRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    List<OrderResponse> listAll() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @PutMapping("/{orderId}/status")
    OrderResponse updateStatus(@PathVariable String orderId,
                               @Valid @RequestBody UpdateOrderStatusRequest request) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Order not found: " + orderId));
        order.updateStatus(request.status());
        return OrderResponse.from(orderRepository.save(order));
    }
}
