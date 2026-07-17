package com.minicommerce.order.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record AdminCancelOrderRequest(@NotBlank String cancelReason) {}
