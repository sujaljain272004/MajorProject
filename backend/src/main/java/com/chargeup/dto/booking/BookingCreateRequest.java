package com.chargeup.dto.booking;

import jakarta.validation.constraints.NotNull;

public record BookingCreateRequest(
    @NotNull Long slotId
) {
}
