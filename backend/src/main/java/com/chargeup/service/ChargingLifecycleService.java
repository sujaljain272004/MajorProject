package com.chargeup.service;

import com.chargeup.dto.lifecycle.ChargingSessionResponse;
import com.chargeup.dto.lifecycle.InvoiceResponse;
import com.chargeup.dto.lifecycle.LifecycleStatusResponse;
import com.chargeup.dto.lifecycle.QrCheckinRequest;
import com.chargeup.entity.Booking;
import com.chargeup.entity.BookingStatus;
import com.chargeup.entity.ChargingSession;
import com.chargeup.entity.ChargingStatus;
import com.chargeup.entity.Invoice;
import com.chargeup.entity.PaymentStatus;
import com.chargeup.entity.QrCheckin;
import com.chargeup.entity.QrVerificationStatus;
import com.chargeup.entity.Role;
import com.chargeup.entity.SlotState;
import com.chargeup.exception.BadRequestException;
import com.chargeup.exception.ResourceNotFoundException;
import com.chargeup.exception.UnauthorizedException;
import com.chargeup.repository.ChargingSessionRepository;
import com.chargeup.repository.InvoiceRepository;
import com.chargeup.repository.PaymentRepository;
import com.chargeup.repository.QrCheckinRepository;
import com.chargeup.repository.BookingRepository;
import com.chargeup.repository.SlotRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargingLifecycleService {

    private static final int QR_EARLY_ARRIVAL_MINUTES = 30;
    private static final BigDecimal GST_RATE = new BigDecimal("0.18");
    private static final BigDecimal OVERTIME_PER_MINUTE = new BigDecimal("5.00");

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final PaymentRepository paymentRepository;
    private final QrCheckinRepository qrCheckinRepository;
    private final ChargingSessionRepository chargingSessionRepository;
    private final InvoiceRepository invoiceRepository;
    private final MappingService mappingService;
    private final CurrentUserService currentUserService;
    private final SlotBroadcastService slotBroadcastService;
    private final LifecycleRealtimeService lifecycleRealtimeService;

    public ChargingLifecycleService(
        BookingService bookingService,
        BookingRepository bookingRepository,
        SlotRepository slotRepository,
        PaymentRepository paymentRepository,
        QrCheckinRepository qrCheckinRepository,
        ChargingSessionRepository chargingSessionRepository,
        InvoiceRepository invoiceRepository,
        MappingService mappingService,
        CurrentUserService currentUserService,
        SlotBroadcastService slotBroadcastService,
        LifecycleRealtimeService lifecycleRealtimeService
    ) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.paymentRepository = paymentRepository;
        this.qrCheckinRepository = qrCheckinRepository;
        this.chargingSessionRepository = chargingSessionRepository;
        this.invoiceRepository = invoiceRepository;
        this.mappingService = mappingService;
        this.currentUserService = currentUserService;
        this.slotBroadcastService = slotBroadcastService;
        this.lifecycleRealtimeService = lifecycleRealtimeService;
    }

    @Transactional(readOnly = true)
    public LifecycleStatusResponse getStatus(Long bookingId) {
        return toLifecycleResponse(bookingService.getOwnedBookingEntity(bookingId));
    }

    @Transactional(readOnly = true)
    public String getQrCode(Long bookingId) {
        var booking = bookingService.getOwnedBookingEntity(bookingId);
        ensureOwnerOwnsBooking(booking);
        return qrCode(booking);
    }

    @Transactional
    public LifecycleStatusResponse checkIn(QrCheckinRequest request) {
        var booking = bookingService.getOwnedBookingEntity(request.bookingId());
        ensureDriverOwnsBooking(booking);

        var checkin = new QrCheckin();
        checkin.setBooking(booking);
        checkin.setQrCode(request.qrCode());

        if (!qrCode(booking).equals(request.qrCode())) {
            checkin.setVerificationStatus(QrVerificationStatus.INVALID);
            qrCheckinRepository.save(checkin);
            throw new BadRequestException("QR code does not match this booking slot");
        }
        if (qrCheckinRepository.existsByBookingIdAndVerificationStatus(booking.getId(), QrVerificationStatus.VERIFIED)) {
            checkin.setVerificationStatus(QrVerificationStatus.DUPLICATE);
            qrCheckinRepository.save(checkin);
            throw new BadRequestException("This booking has already been checked in");
        }
        requireState(booking, BookingStatus.BOOKED, "Only paid bookings can be checked in");
        if (!isWithinCheckinWindow(booking, LocalDateTime.now())) {
            checkin.setVerificationStatus(QrVerificationStatus.EXPIRED);
            qrCheckinRepository.save(checkin);
            throw new BadRequestException("Check-in is outside the allowed booking window");
        }

        checkin.setVerificationStatus(QrVerificationStatus.VERIFIED);
        qrCheckinRepository.save(checkin);

        booking.setStatus(BookingStatus.ARRIVED);
        booking.setActualArrivalTime(LocalDateTime.now());
        booking.setQrCheckinVerified(true);
        booking.getSlot().setState(SlotState.ARRIVED);

        slotRepository.save(booking.getSlot());
        bookingRepository.save(booking);
        publishAll(booking, "DRIVER_ARRIVED");
        return toLifecycleResponse(booking);
    }

    @Transactional
    public LifecycleStatusResponse startCharging(Long bookingId) {
        var booking = bookingService.getOwnedBookingEntity(bookingId);
        ensureDriverOwnsBooking(booking);
        requireState(booking, BookingStatus.ARRIVED, "Driver must check in before charging can start");
        if (!booking.isQrCheckinVerified()) {
            throw new BadRequestException("QR check-in is required before charging");
        }
        chargingSessionRepository.findByBookingId(bookingId).ifPresent(session -> {
            throw new BadRequestException("Charging session already exists for this booking");
        });

        var now = LocalDateTime.now();
        var session = new ChargingSession();
        session.setBooking(booking);
        session.setChargerId(booking.getSlot().getId());
        session.setStartTime(now);
        session.setChargingStatus(ChargingStatus.CHARGING);
        chargingSessionRepository.save(session);

        booking.setStatus(BookingStatus.CHARGING);
        booking.setChargingStartTime(now);
        booking.getSlot().setState(SlotState.CHARGING);
        booking.getSlot().setAvailable(false);

        slotRepository.save(booking.getSlot());
        bookingRepository.save(booking);
        publishAll(booking, "CHARGING_STARTED");
        return toLifecycleResponse(booking);
    }

    @Transactional
    public LifecycleStatusResponse stopCharging(Long bookingId) {
        var booking = bookingService.getOwnedBookingEntity(bookingId);
        ensureDriverOrOwner(booking);
        requireState(booking, BookingStatus.CHARGING, "Only active charging sessions can be stopped");
        var session = chargingSessionRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Charging session not found"));

        updateSessionMetrics(session, LocalDateTime.now());
        session.setChargingStatus(ChargingStatus.COMPLETED);
        session.setEndTime(LocalDateTime.now());
        chargingSessionRepository.save(session);

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setChargingEndTime(session.getEndTime());
        booking.setOvertimeAmount(calculateOvertime(booking, session.getEndTime()));
        booking.setTotalAmount(booking.getSlot().getPrice().add(booking.getOvertimeAmount()));
        booking.getSlot().setState(SlotState.AVAILABLE);
        booking.getSlot().setAvailable(true);

        slotRepository.save(booking.getSlot());
        bookingRepository.save(booking);
        generateInvoice(booking, session);
        publishAll(booking, "CHARGING_COMPLETED");
        return toLifecycleResponse(booking);
    }

    @Transactional
    public LifecycleStatusResponse requestExtension(Long bookingId) {
        var booking = bookingService.getOwnedBookingEntity(bookingId);
        ensureDriverOwnsBooking(booking);
        if (booking.getStatus() != BookingStatus.CHARGING) {
            throw new BadRequestException("Extension can be requested only during charging");
        }
        booking.setExtensionRequested(true);
        booking.setExtensionApproved(false);
        bookingRepository.save(booking);
        publishAll(booking, "EXTENSION_REQUESTED");
        return toLifecycleResponse(booking);
    }

    @Transactional
    public LifecycleStatusResponse decideExtension(Long bookingId, boolean approved) {
        var booking = bookingService.getOwnedBookingEntity(bookingId);
        ensureOwnerOwnsBooking(booking);
        if (!Boolean.TRUE.equals(booking.getExtensionRequested())) {
            throw new BadRequestException("No extension request is pending");
        }
        booking.setExtensionApproved(approved);
        bookingRepository.save(booking);
        publishAll(booking, approved ? "EXTENSION_APPROVED" : "EXTENSION_REJECTED");
        return toLifecycleResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<ChargingSessionResponse> getOwnerLiveSessions() {
        var owner = currentUserService.getCurrentUser();
        if (owner.getRole() != Role.OWNER) {
            throw new UnauthorizedException("Owner access required");
        }
        return chargingSessionRepository.findOwnerLiveSessions(owner.getId(), List.of(ChargingStatus.READY, ChargingStatus.CHARGING))
            .stream()
            .map(this::toSessionResponse)
            .toList();
    }

    private LifecycleStatusResponse toLifecycleResponse(Booking booking) {
        var session = chargingSessionRepository.findDetailedByBookingId(booking.getId())
            .map(this::toSessionResponse)
            .orElse(null);
        var invoice = invoiceRepository.findByBookingId(booking.getId())
            .map(this::toInvoiceResponse)
            .orElse(null);
        boolean driver = currentUserService.getCurrentUser().getRole() == Role.DRIVER;
        return new LifecycleStatusResponse(
            mappingService.toBookingResponse(booking),
            driver ? null : qrCode(booking),
            booking.getStatus() == BookingStatus.BOOKED && isWithinCheckinWindow(booking, LocalDateTime.now()),
            booking.getStatus() == BookingStatus.ARRIVED,
            booking.getStatus() == BookingStatus.CHARGING,
            session,
            invoice
        );
    }

    private ChargingSessionResponse toSessionResponse(ChargingSession session) {
        if (session.getChargingStatus() == ChargingStatus.CHARGING) {
            updateSessionMetrics(session, LocalDateTime.now());
        }
        var booking = session.getBooking();
        long bookedMinutes = Math.max(1, Duration.between(booking.getSlot().getStartTime(), booking.getSlot().getEndTime()).toMinutes());
        int progress = (int) Math.min(100, (session.getChargingDurationMinutes() * 100) / bookedMinutes);
        long remaining = Math.max(0, bookedMinutes - session.getChargingDurationMinutes());
        return new ChargingSessionResponse(
            session.getId(),
            booking.getId(),
            booking.getSlot().getStation().getId(),
            booking.getSlot().getStation().getName(),
            booking.getSlot().getId(),
            session.getChargingStatus(),
            session.getStartTime(),
            session.getEndTime(),
            session.getEnergyConsumed(),
            session.getChargingDurationMinutes(),
            progress,
            remaining
        );
    }

    private InvoiceResponse toInvoiceResponse(Invoice invoice) {
        return new InvoiceResponse(
            invoice.getId(),
            invoice.getBooking().getId(),
            invoice.getAmount(),
            invoice.getGst(),
            invoice.getEnergyUsed(),
            invoice.getChargingDurationMinutes(),
            invoice.getGeneratedAt()
        );
    }

    private void updateSessionMetrics(ChargingSession session, LocalDateTime now) {
        long duration = Math.max(0, Duration.between(session.getStartTime(), now).toMinutes());
        BigDecimal speedKw = session.getBooking().getSlot().getStation().getChargingSpeedKw();
        BigDecimal energy = speedKw
            .multiply(BigDecimal.valueOf(duration))
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        session.setChargingDurationMinutes(duration);
        session.setEnergyConsumed(energy);
    }

    private Invoice generateInvoice(Booking booking, ChargingSession session) {
        return invoiceRepository.findByBookingId(booking.getId()).orElseGet(() -> {
            BigDecimal amount = booking.getTotalAmount();
            BigDecimal gst = amount.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
            var invoice = new Invoice();
            invoice.setBooking(booking);
            invoice.setAmount(amount.add(gst));
            invoice.setGst(gst);
            invoice.setEnergyUsed(session.getEnergyConsumed());
            invoice.setChargingDurationMinutes(session.getChargingDurationMinutes());
            return invoiceRepository.save(invoice);
        });
    }

    private BigDecimal calculateOvertime(Booking booking, LocalDateTime endTime) {
        long overtimeMinutes = Math.max(0, Duration.between(booking.getSlot().getEndTime(), endTime).toMinutes());
        return OVERTIME_PER_MINUTE.multiply(BigDecimal.valueOf(overtimeMinutes));
    }

    private boolean isWithinCheckinWindow(Booking booking, LocalDateTime now) {
        return true;
    }

    private void publishAll(Booking booking, String event) {
        var status = toLifecycleResponse(booking);
        lifecycleRealtimeService.publishBooking(status);
        lifecycleRealtimeService.publishOwnerActivity(booking, event);
        slotBroadcastService.publishStationSlots(booking.getSlot().getStation().getId());
    }

    private String qrCode(Booking booking) {
        return "CHARGEUP:" + booking.getId() + ":" + booking.getSlot().getId() + ":" + booking.getSlot().getStation().getId();
    }

    private void requireState(Booking booking, BookingStatus expected, String message) {
        if (booking.getStatus() != expected) {
            throw new BadRequestException(message + ". Current state: " + booking.getStatus());
        }
    }

    private void ensureDriverOwnsBooking(Booking booking) {
        var user = currentUserService.getCurrentUser();
        if (user.getRole() != Role.DRIVER || !booking.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Driver booking access required");
        }
    }

    private void ensureOwnerOwnsBooking(Booking booking) {
        var user = currentUserService.getCurrentUser();
        if (user.getRole() != Role.OWNER || !booking.getSlot().getStation().getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("Owner station access required");
        }
    }

    private void ensureDriverOrOwner(Booking booking) {
        var user = currentUserService.getCurrentUser();
        boolean driver = user.getRole() == Role.DRIVER && booking.getUser().getId().equals(user.getId());
        boolean owner = user.getRole() == Role.OWNER && booking.getSlot().getStation().getOwner().getId().equals(user.getId());
        if (!driver && !owner) {
            throw new UnauthorizedException("Access denied to charging session");
        }
    }
}
