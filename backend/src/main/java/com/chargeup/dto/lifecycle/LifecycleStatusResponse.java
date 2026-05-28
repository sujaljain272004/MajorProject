package com.chargeup.dto.lifecycle;

import com.chargeup.dto.booking.BookingResponse;

public record LifecycleStatusResponse(
    BookingResponse booking,
    String qrCode,
    boolean canCheckIn,
    boolean canStartCharging,
    boolean canStopCharging,
    ChargingSessionResponse session,
    InvoiceResponse invoice
) {
}
