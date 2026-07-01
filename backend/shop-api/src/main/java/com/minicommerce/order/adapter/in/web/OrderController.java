package com.minicommerce.order.adapter.in.web;

import com.minicommerce.order.application.PlaceOrderCommand;
import com.minicommerce.order.application.port.in.CompletePaymentUseCase;
import com.minicommerce.order.application.port.in.GetOrdersUseCase;
import com.minicommerce.order.application.port.in.PlaceOrderUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final CompletePaymentUseCase completePaymentUseCase;
    private final GetOrdersUseCase getOrdersUseCase;

    public OrderController(PlaceOrderUseCase placeOrderUseCase,
                           CompletePaymentUseCase completePaymentUseCase,
                           GetOrdersUseCase getOrdersUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.completePaymentUseCase = completePaymentUseCase;
        this.getOrdersUseCase = getOrdersUseCase;
    }

    @GetMapping
    List<OrderResponse> getMyOrders(HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        return getOrdersUseCase.getOrders(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @GetMapping("/{orderId}")
    OrderResponse getOrder(@PathVariable String orderId, HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        return OrderResponse.from(getOrdersUseCase.getOrder(orderId, customerId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest
    ) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        PlaceOrderCommand command = new PlaceOrderCommand(
                request.items().stream()
                        .map(i -> new PlaceOrderCommand.OrderItem(i.productId(), i.quantity(), i.selectedOptionId()))
                        .toList(),
                request.shippingRecipient(),
                request.shippingPhone(),
                request.shippingAddress(),
                request.shippingDetailAddress(),
                request.shippingZipCode()
        );
        return OrderResponse.from(placeOrderUseCase.place(command, customerId));
    }

    @PostMapping("/{orderId}/complete-payment")
    OrderResponse completeFakePayment(@PathVariable String orderId) {
        return OrderResponse.from(completePaymentUseCase.complete(orderId));
    }
}
