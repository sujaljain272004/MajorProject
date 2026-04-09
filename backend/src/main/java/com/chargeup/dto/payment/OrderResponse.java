package com.chargeup.dto.payment;

import java.math.BigDecimal;

public record OrderResponse(
    Long bookingId,
    String razorpayOrderId,
    String key,
    BigDecimal amount,
    String currency
) {
}
