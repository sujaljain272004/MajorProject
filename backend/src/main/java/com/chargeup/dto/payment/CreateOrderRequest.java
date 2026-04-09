package com.chargeup.dto.payment;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
    @NotNull Long bookingId
) {
}
