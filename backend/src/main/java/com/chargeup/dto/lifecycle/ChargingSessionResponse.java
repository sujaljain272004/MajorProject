package com.chargeup.dto.lifecycle;

import com.chargeup.entity.ChargingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ChargingSessionResponse(
    Long id,
    Long bookingId,
    Long stationId,
    String stationName,
    Long slotId,
    ChargingStatus status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BigDecimal energyConsumed,
    Long durationMinutes,
    int progressPercent,
    Long estimatedRemainingMinutes
) {
}
