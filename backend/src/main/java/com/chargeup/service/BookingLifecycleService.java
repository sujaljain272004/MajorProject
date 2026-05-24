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
        bookingRepository.findExpiredReservations(now).forEach(booking -> cancelAndRelease(booking, SlotState.AVAILABLE));
        bookingRepository.findMissedArrivals(now).forEach(booking -> cancelAndRelease(booking, SlotState.AVAILABLE));
    }

    private void cancelAndRelease(com.chargeup.entity.Booking booking, SlotState releasedState) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.getSlot().setState(releasedState);
        booking.getSlot().setAvailable(releasedState == SlotState.AVAILABLE);
        slotRepository.save(booking.getSlot());
        bookingRepository.save(booking);
        slotBroadcastService.publishStationSlots(booking.getSlot().getStation().getId());
    }
}
