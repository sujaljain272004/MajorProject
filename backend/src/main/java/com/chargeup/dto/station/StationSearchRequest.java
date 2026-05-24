package com.chargeup.dto.station;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record StationSearchRequest(
    @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
    @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
    @Positive Integer radiusKm,
    String city,
    String pincode,
    String connectorType,
    Boolean fastCharging,
    Boolean availableOnly,
    @Positive BigDecimal maxPrice
) {
}
