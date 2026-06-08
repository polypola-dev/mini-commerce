package com.minicommerce.order.adapter.in.web;

import com.minicommerce.order.application.PlaceOrderCommand;
import com.minicommerce.order.application.port.in.CompletePaymentUseCase;
import com.minicommerce.order.application.port.in.PlaceOrderUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    public OrderController(PlaceOrderUseCase placeOrderUseCase, CompletePaymentUseCase completePaymentUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.completePaymentUseCase = completePaymentUseCase;
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
                        .toList()
        );
        return OrderResponse.from(placeOrderUseCase.place(command, customerId));
    }

    @PostMapping("/{orderId}/complete-payment")
    OrderResponse completeFakePayment(@PathVariable String orderId) {
        return OrderResponse.from(completePaymentUseCase.complete(orderId));
    }
}
