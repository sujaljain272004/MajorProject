package com.chargeup.dto.station;

public record StationResponse(
    Long id,
    String name,
    String location,
    Double latitude,
    Double longitude,
    Long ownerId,
    String ownerName,
    long totalSlots,
    long availableSlots
) {
}
