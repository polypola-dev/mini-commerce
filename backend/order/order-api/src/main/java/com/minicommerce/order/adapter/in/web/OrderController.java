package com.minicommerce.order.adapter.in.web;

import com.minicommerce.order.application.PlaceOrderCommand;
import com.minicommerce.order.application.port.in.CancelOrderUseCase;
import com.minicommerce.order.application.port.in.ConfirmPaymentUseCase;
import com.minicommerce.order.application.port.in.GetOrdersUseCase;
import com.minicommerce.order.application.port.in.PlaceOrderUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
    private final ConfirmPaymentUseCase confirmPaymentUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final GetOrdersUseCase getOrdersUseCase;

    public OrderController(PlaceOrderUseCase placeOrderUseCase,
                           ConfirmPaymentUseCase confirmPaymentUseCase,
                           CancelOrderUseCase cancelOrderUseCase,
                           GetOrdersUseCase getOrdersUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.confirmPaymentUseCase = confirmPaymentUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
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

    @PostMapping("/{orderId}/confirm-payment")
    OrderResponse confirmPayment(@PathVariable String orderId, @Valid @RequestBody ConfirmPaymentRequest request,
                                 HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        return OrderResponse.from(confirmPaymentUseCase.confirm(orderId, customerId, request.paymentKey(), request.amount()));
    }

    @PostMapping("/{orderId}/cancel")
    OrderResponse cancel(@PathVariable String orderId, @RequestBody(required = false) CancelOrderRequest request,
                         HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        String reason = request != null && request.cancelReason() != null && !request.cancelReason().isBlank()
                ? request.cancelReason() : "고객 변심";
        return OrderResponse.from(cancelOrderUseCase.cancel(orderId, customerId, reason));
    }

    record ConfirmPaymentRequest(@NotBlank String paymentKey, @NotNull BigDecimal amount) {
    }

    record CancelOrderRequest(String cancelReason) {
    }
}
