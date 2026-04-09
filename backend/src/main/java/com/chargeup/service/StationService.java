package com.chargeup.service;

import com.chargeup.dto.booking.BookingResponse;
import com.chargeup.dto.station.OwnerDashboardResponse;
import com.chargeup.dto.station.StationRequest;
import com.chargeup.dto.station.StationResponse;
import com.chargeup.entity.BookingStatus;
import com.chargeup.entity.PaymentStatus;
import com.chargeup.entity.Role;
import com.chargeup.entity.Station;
import com.chargeup.exception.BadRequestException;
import com.chargeup.exception.ResourceNotFoundException;
import com.chargeup.exception.UnauthorizedException;
import com.chargeup.repository.BookingRepository;
import com.chargeup.repository.PaymentRepository;
import com.chargeup.repository.SlotRepository;
import com.chargeup.repository.StationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StationService {

    private final StationRepository stationRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;
    private final MappingService mappingService;

    public StationService(
        StationRepository stationRepository,
        SlotRepository slotRepository,
        BookingRepository bookingRepository,
        PaymentRepository paymentRepository,
        CurrentUserService currentUserService,
        MappingService mappingService
    ) {
        this.stationRepository = stationRepository;
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
        this.mappingService = mappingService;
    }

    @Transactional(readOnly = true)
    public List<StationResponse> getAllStations() {
        return stationRepository.findAll().stream()
            .map(this::mapWithCounts)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<StationResponse> getMyStations() {
        var currentUser = currentUserService.getCurrentUser();
        requireOwner(currentUser.getRole());
        return stationRepository.findByOwnerId(currentUser.getId()).stream()
            .map(this::mapWithCounts)
            .toList();
    }

    @Transactional(readOnly = true)
    public StationResponse getStation(Long stationId) {
        return mapWithCounts(findStation(stationId));
    }

    @Transactional
    public StationResponse createStation(StationRequest request) {
        var owner = currentUserService.getCurrentUser();
        requireOwner(owner.getRole());

        var station = new Station();
        station.setName(request.name());
        station.setLocation(request.location());
        station.setLatitude(request.latitude());
        station.setLongitude(request.longitude());
        station.setOwner(owner);

        return mapWithCounts(stationRepository.save(station));
    }

    @Transactional
    public StationResponse updateStation(Long stationId, StationRequest request) {
        var station = findOwnedStation(stationId);
        station.setName(request.name());
        station.setLocation(request.location());
        station.setLatitude(request.latitude());
        station.setLongitude(request.longitude());
        return mapWithCounts(stationRepository.save(station));
    }

    @Transactional
    public void deleteStation(Long stationId) {
        var station = findOwnedStation(stationId);
        if (!slotRepository.findByStationIdOrderByStartTimeAsc(stationId).isEmpty()) {
            throw new BadRequestException("Delete station slots before removing the station");
        }
        stationRepository.delete(station);
    }

    @Transactional(readOnly = true)
    public OwnerDashboardResponse getOwnerDashboard() {
        var owner = currentUserService.getCurrentUser();
        requireOwner(owner.getRole());

        List<StationResponse> stations = stationRepository.findByOwnerId(owner.getId()).stream()
            .map(this::mapWithCounts)
            .toList();

        List<BookingResponse> recentBookings = bookingRepository.findDetailedByOwnerId(owner.getId()).stream()
            .limit(10)
            .map(mappingService::toBookingResponse)
            .toList();

        return new OwnerDashboardResponse(
            stations.size(),
            slotRepository.countByStationOwnerId(owner.getId()),
            slotRepository.countByStationOwnerIdAndAvailableTrue(owner.getId()),
            bookingRepository.countBySlotStationOwnerId(owner.getId()),
            bookingRepository.countBySlotStationOwnerIdAndStatus(owner.getId(), BookingStatus.CONFIRMED),
            paymentRepository.sumRevenueByOwnerIdAndStatus(owner.getId(), PaymentStatus.PAID),
            stations,
            recentBookings
        );
    }

    @Transactional(readOnly = true)
    public Station findStation(Long stationId) {
        return stationRepository.findById(stationId)
            .orElseThrow(() -> new ResourceNotFoundException("Station not found"));
    }

    @Transactional(readOnly = true)
    public Station findOwnedStation(Long stationId) {
        var currentUser = currentUserService.getCurrentUser();
        requireOwner(currentUser.getRole());

        var station = findStation(stationId);
        if (!station.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not own this station");
        }
        return station;
    }

    private StationResponse mapWithCounts(Station station) {
        var slots = slotRepository.findByStationIdOrderByStartTimeAsc(station.getId());
        long totalSlots = slots.size();
        long availableSlots = slots.stream().filter(com.chargeup.entity.Slot::isAvailable).count();
        return mappingService.toStationResponse(station, totalSlots, availableSlots);
    }

    private void requireOwner(Role role) {
        if (role != Role.OWNER) {
            throw new UnauthorizedException("Owner access required");
        }
    }
}
