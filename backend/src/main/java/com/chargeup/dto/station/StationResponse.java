package com.chargeup.dto.station;

import com.chargeup.entity.StationOperatingStatus;
import com.chargeup.entity.StationVerificationStatus;
import java.math.BigDecimal;
import java.util.List;

public record StationResponse(
    Long id,
    String name,
    String location,
    String city,
    String pincode,
    Double latitude,
    Double longitude,
    String chargerType,
    String connectorType,
    BigDecimal chargingSpeedKw,
    Integer slotCount,
    BigDecimal pricePerKwh,
    String openingHours,
    List<String> photoUrls,
    StationVerificationStatus verificationStatus,
    StationOperatingStatus operatingStatus,
    Long ownerId,
    String ownerName,
    long totalSlots,
    long availableSlots,
    Double distanceKm,
    long estimatedWaitMinutes
) {
}
