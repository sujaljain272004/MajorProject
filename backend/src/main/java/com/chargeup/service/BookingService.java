package com.chargeup.service;

import com.chargeup.dto.booking.BookingCreateRequest;
import com.chargeup.dto.booking.BookingResponse;
import com.chargeup.entity.Booking;
import com.chargeup.entity.BookingStatus;
import com.chargeup.entity.PaymentStatus;
import com.chargeup.entity.Role;
import com.chargeup.exception.BadRequestException;
import com.chargeup.exception.ResourceNotFoundException;
import com.chargeup.exception.UnauthorizedException;
import com.chargeup.repository.BookingRepository;
import com.chargeup.repository.PaymentRepository;
import com.chargeup.repository.SlotRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;
    private final MappingService mappingService;
    private final SlotBroadcastService slotBroadcastService;

    public BookingService(
        BookingRepository bookingRepository,
        SlotRepository slotRepository,
        PaymentRepository paymentRepository,
        CurrentUserService currentUserService,
        MappingService mappingService,
        SlotBroadcastService slotBroadcastService
    ) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
        this.mappingService = mappingService;
        this.slotBroadcastService = slotBroadcastService;
    }

    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request) {
        var user = currentUserService.getCurrentUser();
        if (user.getRole() != Role.DRIVER) {
            throw new UnauthorizedException("Driver access required");
        }

        // Pessimistic row locking ensures only one concurrent booking transaction can claim a slot.
        var slot = slotRepository.findByIdForUpdate(request.slotId())
            .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));

        if (!slot.isAvailable()) {
            throw new BadRequestException("Slot is already booked");
        }
        if (!slot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Cannot book a slot that has already started");
        }

        slot.setAvailable(false);
        slotRepository.save(slot);

        var booking = new Booking();
        booking.setUser(user);
        booking.setSlot(slot);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        var saved = bookingRepository.save(booking);
        var detailed = bookingRepository.findDetailedById(saved.getId()).orElse(saved);

        slotBroadcastService.publishStationSlots(slot.getStation().getId());
        return mappingService.toBookingResponse(detailed);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        var booking = getOwnedBookingEntity(bookingId);

        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new BadRequestException("Booking is already canceled");
        }

        booking.setStatus(BookingStatus.CANCELED);
        booking.getSlot().setAvailable(true);
        slotRepository.save(booking.getSlot());

        var payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
            payment.setStatus(PaymentStatus.REFUND_PENDING);
            paymentRepository.save(payment);
        }

        var saved = bookingRepository.save(booking);
        slotBroadcastService.publishStationSlots(saved.getSlot().getStation().getId());
        return mappingService.toBookingResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {
        return bookingRepository.findDetailedByUserId(currentUserService.getCurrentUser().getId()).stream()
            .map(mappingService::toBookingResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long bookingId) {
        return mappingService.toBookingResponse(getOwnedBookingEntity(bookingId));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getOwnerBookings() {
        var currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.OWNER) {
            throw new UnauthorizedException("Owner access required");
        }

        return bookingRepository.findDetailedByOwnerId(currentUser.getId()).stream()
            .map(mappingService::toBookingResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Booking getOwnedBookingEntity(Long bookingId) {
        var booking = bookingRepository.findDetailedById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        var currentUser = currentUserService.getCurrentUser();
        boolean userOwnsBooking = booking.getUser().getId().equals(currentUser.getId());
        boolean ownerOwnsStation = booking.getSlot().getStation().getOwner().getId().equals(currentUser.getId());

        if (!userOwnsBooking && !ownerOwnsStation) {
            throw new UnauthorizedException("Access denied to booking");
        }

        return booking;
    }
}
