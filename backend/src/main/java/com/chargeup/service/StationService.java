package com.chargeup.service;

import com.chargeup.dto.booking.BookingResponse;
import com.chargeup.dto.station.OwnerDashboardResponse;
import com.chargeup.dto.station.StationRequest;
import com.chargeup.dto.station.StationResponse;
import com.chargeup.dto.station.StationSearchRequest;
import com.chargeup.entity.BookingStatus;
import com.chargeup.entity.PaymentStatus;
import com.chargeup.entity.Role;
import com.chargeup.entity.Slot;
import com.chargeup.entity.SlotState;
import com.chargeup.entity.Station;
import com.chargeup.entity.StationOperatingStatus;
import com.chargeup.entity.StationVerificationStatus;
import com.chargeup.exception.BadRequestException;
import com.chargeup.exception.ResourceNotFoundException;
import com.chargeup.exception.UnauthorizedException;
import com.chargeup.repository.BookingRepository;
import com.chargeup.repository.PaymentRepository;
import com.chargeup.repository.SlotRepository;
import com.chargeup.repository.StationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
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
    private final StationPhotoService stationPhotoService;
    private final boolean autoVerifyOwnerStations;

    public StationService(
        StationRepository stationRepository,
        SlotRepository slotRepository,
        BookingRepository bookingRepository,
        PaymentRepository paymentRepository,
        CurrentUserService currentUserService,
        MappingService mappingService,
        StationPhotoService stationPhotoService,
        @Value("${app.station.auto-verify-owner-created:true}") boolean autoVerifyOwnerStations
    ) {
        this.stationRepository = stationRepository;
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
        this.mappingService = mappingService;
        this.stationPhotoService = stationPhotoService;
        this.autoVerifyOwnerStations = autoVerifyOwnerStations;
    }

    @Transactional(readOnly = true)
    public List<StationResponse> getAllStations() {
        if (currentUserService.getCurrentUser().getRole() == Role.DRIVER) {
            throw new BadRequestException("Drivers must use nearby station search");
        }
        return stationRepository.findByVerificationStatusAndOperatingStatus(
                StationVerificationStatus.VERIFIED,
                StationOperatingStatus.ACTIVE
            ).stream()
            .map(this::mapWithCounts)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<StationResponse> searchNearbyStations(StationSearchRequest request) {
        validateSearch(request);
        String city = blankToNull(request.city());
        String pincode = blankToNull(request.pincode());
        int radiusKm = request.radiusKm() == null ? 10 : request.radiusKm();

        return stationRepository.findPublicStationsForSearch(city, pincode).stream()
            .map(station -> mapSearchCandidate(station, request))
            .filter(Objects::nonNull)
            .filter(station -> station.distanceKm() == null || station.distanceKm() <= radiusKm)
            .sorted(Comparator
                .comparing(StationResponse::distanceKm, Comparator.nullsLast(Double::compareTo))
                .thenComparing(StationResponse::availableSlots, Comparator.reverseOrder()))
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
        var station = findStation(stationId);
        if (!isPublic(station)) {
            var currentUser = currentUserService.getCurrentUser();
            boolean ownsStation = station.getOwner().getId().equals(currentUser.getId());
            if (!ownsStation && currentUser.getRole() != Role.ADMIN) {
                throw new ResourceNotFoundException("Station not found");
            }
        }
        return mapWithCounts(station);
    }

    @Transactional
    public StationResponse createStation(StationRequest request) {
        var owner = currentUserService.getCurrentUser();
        requireOwner(owner.getRole());

        var existing = findDuplicateStation(request, null).orElse(null);
        if (existing != null) {
            if (!existing.getOwner().getId().equals(owner.getId())) {
                throw new BadRequestException("Another owner already has a station pinned at this location");
            }
            copyStationFields(existing, request);
            applyOwnerVerificationMode(existing);
            existing.setOperatingStatus(StationOperatingStatus.ACTIVE);
            var savedExisting = stationRepository.save(existing);
            createStarterSlots(savedExisting);
            return mapWithCounts(savedExisting);
        }

        var station = new Station();
        copyStationFields(station, request);
        applyOwnerVerificationMode(station);
        station.setOwner(owner);

        var saved = stationRepository.save(station);
        createStarterSlots(saved);
        return mapWithCounts(saved);
    }

    @Transactional
    public StationResponse updateStation(Long stationId, StationRequest request) {
        var station = findOwnedStation(stationId);
        requireUniqueStation(request, stationId);
        copyStationFields(station, request);
        applyOwnerVerificationMode(station);
        return mapWithCounts(stationRepository.save(station));
    }

    @Transactional
    public StationResponse uploadPhoto(Long stationId, MultipartFile file) {
        var station = findOwnedStation(stationId);
        station.getPhotoUrls().add(stationPhotoService.store(file));
        return mapWithCounts(stationRepository.save(station));
    }

    @Transactional
    public StationResponse verifyStation(Long stationId, boolean verified) {
        requireAdmin(currentUserService.getCurrentUser().getRole());
        var station = findStation(stationId);
        station.setVerificationStatus(verified ? StationVerificationStatus.VERIFIED : StationVerificationStatus.REJECTED);
        return mapWithCounts(stationRepository.save(station));
    }

    @Transactional
    public StationResponse setOperatingStatus(Long stationId, StationOperatingStatus status) {
        var station = findOwnedStation(stationId);
        station.setOperatingStatus(status);
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
            bookingRepository.countBySlotStationOwnerIdAndStatus(owner.getId(), BookingStatus.BOOKED),
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
        long availableSlots = slots.stream().filter(slot -> slot.getState() == SlotState.AVAILABLE).count();
        return mappingService.toStationResponse(station, totalSlots, availableSlots);
    }

    private StationResponse mapSearchCandidate(Station station, StationSearchRequest request) {
        var slots = slotRepository.findByStationIdOrderByStartTimeAsc(station.getId());
        long availableSlots = slots.stream().filter(slot -> slot.getState() == SlotState.AVAILABLE).count();
        Double distanceKm = hasCoordinates(request) ? haversineKm(
            request.latitude(),
            request.longitude(),
            station.getLatitude(),
            station.getLongitude()
        ) : null;

        if (Boolean.TRUE.equals(request.availableOnly()) && availableSlots == 0) {
            return null;
        }
        if (blankToNull(request.connectorType()) != null
            && !station.getConnectorType().toLowerCase(Locale.ROOT).contains(request.connectorType().toLowerCase(Locale.ROOT))) {
            return null;
        }
        if (Boolean.TRUE.equals(request.fastCharging()) && station.getChargingSpeedKw().doubleValue() < 50) {
            return null;
        }
        if (request.maxPrice() != null && station.getPricePerKwh().compareTo(request.maxPrice()) > 0) {
            return null;
        }

        long occupiedSlots = Math.max(0, slots.size() - availableSlots);
        return mappingService.toStationResponse(station, slots.size(), availableSlots, roundDistance(distanceKm), occupiedSlots * 15);
    }

    private void copyStationFields(Station station, StationRequest request) {
        station.setName(request.name().trim());
        station.setLocation(request.location().trim());
        station.setCity(request.city().trim());
        station.setPincode(request.pincode().trim());
        station.setLatitude(request.latitude());
        station.setLongitude(request.longitude());
        station.setChargerType(request.chargerType().trim());
        station.setConnectorType(request.connectorType().trim());
        station.setChargingSpeedKw(request.chargingSpeedKw());
        station.setSlotCount(request.slotCount());
        station.setPricePerKwh(request.pricePerKwh());
        station.setOpeningHours(request.openingHours().trim());
        if (request.photoUrls() != null) {
            station.setPhotoUrls(new ArrayList<>(request.photoUrls().stream().filter(url -> !url.isBlank()).map(String::trim).toList()));
        }
    }

    private void applyOwnerVerificationMode(Station station) {
        station.setVerificationStatus(autoVerifyOwnerStations
            ? StationVerificationStatus.VERIFIED
            : StationVerificationStatus.PENDING);
    }

    private void createStarterSlots(Station station) {
        int slotsToCreate = Math.max(1, Math.min(station.getSlotCount(), 12));
        LocalDateTime start = LocalDateTime.now()
            .plusHours(2)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);

        for (int index = 0; index < slotsToCreate; index++) {
            LocalDateTime slotStart = start.plusHours(index);
            LocalDateTime slotEnd = slotStart.plusHours(1);
            if (slotRepository.existsByStationIdAndStartTimeAndEndTime(station.getId(), slotStart, slotEnd)) {
                continue;
            }

            var slot = new Slot();
            slot.setStation(station);
            slot.setStartTime(slotStart);
            slot.setEndTime(slotEnd);
            slot.setPrice(station.getPricePerKwh());
            slot.setAvailable(true);
            slot.setState(SlotState.AVAILABLE);
            slotRepository.save(slot);
        }
    }

    private void requireUniqueStation(StationRequest request, Long ignoredId) {
        if (findDuplicateStation(request, ignoredId).isPresent()) {
            throw new BadRequestException("A station with this name is already pinned at this location");
        }
    }

    private java.util.Optional<Station> findDuplicateStation(StationRequest request, Long ignoredId) {
        double tolerance = 0.0005;
        return stationRepository.findDuplicateLocation(
            request.name().trim(),
            request.latitude() - tolerance,
            request.latitude() + tolerance,
            request.longitude() - tolerance,
            request.longitude() + tolerance,
            ignoredId
        );
    }

    private void validateSearch(StationSearchRequest request) {
        if (request.radiusKm() != null && !List.of(5, 10, 25).contains(request.radiusKm())) {
            throw new BadRequestException("Radius must be 5, 10, or 25 km");
        }
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new BadRequestException("Latitude and longitude must be provided together");
        }
        if (!hasCoordinates(request) && blankToNull(request.city()) == null && blankToNull(request.pincode()) == null) {
            throw new BadRequestException("Provide GPS coordinates, city, or pincode to find nearby stations");
        }
    }

    private boolean hasCoordinates(StationSearchRequest request) {
        return request.latitude() != null && request.longitude() != null;
    }

    private Double roundDistance(Double distanceKm) {
        return distanceKm == null ? null : Math.round(distanceKm * 100.0) / 100.0;
    }

    private double haversineKm(double latitude, double longitude, double stationLatitude, double stationLongitude) {
        double earthRadiusKm = 6371.0088;
        double latitudeDelta = Math.toRadians(stationLatitude - latitude);
        double longitudeDelta = Math.toRadians(stationLongitude - longitude);
        double sinLatitude = Math.sin(latitudeDelta / 2);
        double sinLongitude = Math.sin(longitudeDelta / 2);
        double a = sinLatitude * sinLatitude
            + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(stationLatitude))
            * sinLongitude * sinLongitude;
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireOwner(Role role) {
        if (role != Role.OWNER) {
            throw new UnauthorizedException("Owner access required");
        }
    }

    private void requireAdmin(Role role) {
        if (role != Role.ADMIN) {
            throw new UnauthorizedException("Admin access required");
        }
    }

    private boolean isPublic(Station station) {
        return station.getVerificationStatus() == StationVerificationStatus.VERIFIED
            && station.getOperatingStatus() == StationOperatingStatus.ACTIVE;
    }
}
