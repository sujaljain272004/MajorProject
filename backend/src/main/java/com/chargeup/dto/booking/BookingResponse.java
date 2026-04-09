package com.chargeup.dto.booking;

import com.chargeup.entity.BookingStatus;
import com.chargeup.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
    Long id,
    BookingStatus status,
    String paymentId,
    Long slotId,
    Long stationId,
    String stationName,
    String location,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BigDecimal amount,
    PaymentStatus paymentStatus,
    String bookedBy,
    LocalDateTime createdAt
) {
}
