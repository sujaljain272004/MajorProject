package com.chargeup.dto.slot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SlotResponse(
    Long id,
    Long stationId,
    String stationName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BigDecimal price,
    boolean available,
    Long version
) {
}
