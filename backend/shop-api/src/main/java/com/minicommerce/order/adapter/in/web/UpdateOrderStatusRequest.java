package com.minicommerce.order.adapter.in.web;

import com.minicommerce.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {}
