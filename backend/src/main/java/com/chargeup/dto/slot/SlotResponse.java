package com.chargeup.dto.slot;

import com.chargeup.entity.SlotState;
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
    SlotState state,
    Long version
) {
}
