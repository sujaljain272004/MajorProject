package com.chargeup.dto.lifecycle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QrCheckinRequest(
    @NotNull Long bookingId,
    @NotBlank String qrCode
) {
}
