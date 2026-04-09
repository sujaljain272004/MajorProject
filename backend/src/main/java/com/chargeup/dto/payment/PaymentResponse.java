package com.chargeup.dto.payment;

import com.chargeup.entity.PaymentStatus;
import java.math.BigDecimal;

public record PaymentResponse(
    Long id,
    Long bookingId,
    BigDecimal amount,
    PaymentStatus status,
    String razorpayOrderId,
    String razorpayPaymentId
) {
}
