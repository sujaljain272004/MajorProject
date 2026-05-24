package com.chargeup.service;

import com.chargeup.dto.auth.UserSummary;
import com.chargeup.dto.booking.BookingResponse;
import com.chargeup.dto.payment.PaymentResponse;
import com.chargeup.dto.slot.SlotResponse;
import com.chargeup.dto.station.StationResponse;
import com.chargeup.entity.Booking;
import com.chargeup.entity.Payment;
import com.chargeup.entity.Slot;
import com.chargeup.entity.Station;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MappingService {

    public UserSummary toUserSummary(com.chargeup.entity.User user) {
        return new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public StationResponse toStationResponse(Station station, long totalSlots, long availableSlots) {
        return toStationResponse(station, totalSlots, availableSlots, null, 0);
    }

    public StationResponse toStationResponse(
        Station station,
        long totalSlots,
        long availableSlots,
        Double distanceKm,
        long estimatedWaitMinutes
    ) {
        return new StationResponse(
            station.getId(),
            station.getName(),
            station.getLocation(),
            station.getCity(),
            station.getPincode(),
            station.getLatitude(),
            station.getLongitude(),
            station.getChargerType(),
            station.getConnectorType(),
            station.getChargingSpeedKw(),
            station.getSlotCount(),
            station.getPricePerKwh(),
            station.getOpeningHours(),
            List.copyOf(station.getPhotoUrls()),
            station.getVerificationStatus(),
            station.getOperatingStatus(),
            station.getOwner().getId(),
            station.getOwner().getName(),
            totalSlots,
            availableSlots,
            distanceKm,
            estimatedWaitMinutes
        );
    }

    public SlotResponse toSlotResponse(Slot slot) {
        return new SlotResponse(
            slot.getId(),
            slot.getStation().getId(),
            slot.getStation().getName(),
            slot.getStartTime(),
            slot.getEndTime(),
            slot.getPrice(),
            slot.isAvailable(),
            slot.getState(),
            slot.getVersion()
        );
    }

    public BookingResponse toBookingResponse(Booking booking) {
        var slot = booking.getSlot();
        var station = slot.getStation();
        var payment = booking.getPayment();

        return new BookingResponse(
            booking.getId(),
            booking.getStatus(),
            booking.getPaymentId(),
            slot.getId(),
            station.getId(),
            station.getName(),
            station.getLocation(),
            slot.getStartTime(),
            slot.getEndTime(),
            slot.getPrice(),
            payment == null ? null : payment.getStatus(),
            booking.getUser().getName(),
            booking.getCreatedAt()
        );
    }

    public PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getBooking().getId(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getRazorpayOrderId(),
            payment.getRazorpayPaymentId()
        );
    }

    public BigDecimal slotPrice(Booking booking) {
        return booking.getSlot().getPrice();
    }
}
