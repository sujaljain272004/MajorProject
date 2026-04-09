package com.chargeup.dto.station;

import com.chargeup.dto.booking.BookingResponse;
import java.math.BigDecimal;
import java.util.List;

public record OwnerDashboardResponse(
    long totalStations,
    long totalSlots,
    long availableSlots,
    long totalBookings,
    long confirmedBookings,
    BigDecimal totalRevenue,
    List<StationResponse> stations,
    List<BookingResponse> recentBookings
) {
}
