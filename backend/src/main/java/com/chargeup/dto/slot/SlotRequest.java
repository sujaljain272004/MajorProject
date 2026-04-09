package com.chargeup.dto.slot;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SlotRequest(
    @NotNull @Future LocalDateTime startTime,
    @NotNull @Future LocalDateTime endTime,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price
) {
}
