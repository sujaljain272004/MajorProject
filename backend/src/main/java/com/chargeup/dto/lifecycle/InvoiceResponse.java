package com.chargeup.dto.lifecycle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceResponse(
    Long id,
    Long bookingId,
    BigDecimal amount,
    BigDecimal gst,
    BigDecimal energyUsed,
    Long chargingDurationMinutes,
    LocalDateTime generatedAt
) {
}
