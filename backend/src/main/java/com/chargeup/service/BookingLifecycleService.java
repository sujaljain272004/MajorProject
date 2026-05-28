package com.chargeup.service;

import com.chargeup.entity.BookingStatus;
import com.chargeup.entity.SlotState;
import com.chargeup.repository.BookingRepository;
import com.chargeup.repository.SlotRepository;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingLifecycleService {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final SlotBroadcastService slotBroadcastService;

    public BookingLifecycleService(
        BookingRepository bookingRepository,
        SlotRepository slotRepository,
        SlotBroadcastService slotBroadcastService
    ) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.slotBroadcastService = slotBroadcastService;
    }

    @Scheduled(fixedDelayString = "${app.booking.lifecycle-delay-ms:60000}")
    @Transactional
    public void releaseExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();
        bookingRepository.findExpiredReservations(now).forEach(this::expireAndRelease);
        bookingRepository.findMissedArrivals(now).forEach(this::expireAndRelease);

        // Auto release expired slot reservations
        slotRepository.findByStateAndReservationExpiryBefore(SlotState.RESERVED, now)
            .forEach(this::releaseSlot);
    }

    private void releaseSlot(com.chargeup.entity.Slot slot) {
        slot.setState(SlotState.AVAILABLE);
        slot.setAvailable(true);
        slot.setReservedBy(null);
        slot.setReservationExpiry(null);
        slotRepository.save(slot);
        slotBroadcastService.publishStationSlots(slot.getStation().getId());
    }

    private void expireAndRelease(com.chargeup.entity.Booking booking) {
        booking.setStatus(BookingStatus.EXPIRED);
        booking.getSlot().setState(SlotState.AVAILABLE);
        booking.getSlot().setAvailable(true);
        slotRepository.save(booking.getSlot());
        bookingRepository.save(booking);
        slotBroadcastService.publishStationSlots(booking.getSlot().getStation().getId());
    }
}
