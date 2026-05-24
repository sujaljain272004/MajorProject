package com.chargeup.dto.station;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record StationRequest(
    @NotBlank String name,
    @NotBlank String location,
    @NotBlank String city,
    @NotBlank @Size(max = 12) String pincode,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
    @NotBlank String chargerType,
    @NotBlank String connectorType,
    @NotNull @Positive BigDecimal chargingSpeedKw,
    @NotNull @Positive Integer slotCount,
    @NotNull @Positive BigDecimal pricePerKwh,
    @NotBlank String openingHours,
    List<@Size(max = 1024) String> photoUrls
) {
}
