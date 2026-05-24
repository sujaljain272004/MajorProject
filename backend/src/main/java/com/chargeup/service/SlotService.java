package com.chargeup.service;

import com.chargeup.dto.slot.SlotRequest;
import com.chargeup.dto.slot.SlotResponse;
import com.chargeup.entity.Role;
import com.chargeup.entity.Slot;
import com.chargeup.entity.SlotState;
import com.chargeup.exception.BadRequestException;
import com.chargeup.exception.ResourceNotFoundException;
import com.chargeup.exception.UnauthorizedException;
import com.chargeup.repository.BookingRepository;
import com.chargeup.repository.SlotRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlotService {

    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final StationService stationService;
    private final CurrentUserService currentUserService;
    private final MappingService mappingService;

    public SlotService(
        SlotRepository slotRepository,
        BookingRepository bookingRepository,
        StationService stationService,
        CurrentUserService currentUserService,
        MappingService mappingService
    ) {
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.stationService = stationService;
        this.currentUserService = currentUserService;
        this.mappingService = mappingService;
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> getSlotsForStation(Long stationId) {
        stationService.getStation(stationId);
        return slotRepository.findByStationIdOrderByStartTimeAsc(stationId).stream()
            .map(mappingService::toSlotResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public SlotResponse getSlot(Long slotId) {
        return mappingService.toSlotResponse(findSlot(slotId));
    }

    @Transactional
    public SlotResponse createSlot(Long stationId, SlotRequest request) {
        ensureOwner();
        validateSlotWindow(request);

        var station = stationService.findOwnedStation(stationId);
        if (slotRepository.existsByStationIdAndStartTimeAndEndTime(stationId, request.startTime(), request.endTime())) {
            throw new BadRequestException("A slot already exists for that time window");
        }

        var slot = new Slot();
        slot.setStation(station);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setPrice(request.price());
        slot.setAvailable(true);
        slot.setState(SlotState.AVAILABLE);
        return mappingService.toSlotResponse(slotRepository.save(slot));
    }

    @Transactional
    public SlotResponse updateSlot(Long slotId, SlotRequest request) {
        ensureOwner();
        validateSlotWindow(request);
        if (bookingRepository.existsBySlotId(slotId)) {
            throw new BadRequestException("Cannot edit a slot that already has bookings");
        }

        var slot = findOwnedSlot(slotId);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setPrice(request.price());
        return mappingService.toSlotResponse(slotRepository.save(slot));
    }

    @Transactional
    public void deleteSlot(Long slotId) {
        ensureOwner();
        if (bookingRepository.existsBySlotId(slotId)) {
            throw new BadRequestException("Cannot delete a slot that already has bookings");
        }
        slotRepository.delete(findOwnedSlot(slotId));
    }

    @Transactional(readOnly = true)
    public Slot findSlot(Long slotId) {
        return slotRepository.findById(slotId)
            .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
    }

    @Transactional(readOnly = true)
    public Slot findOwnedSlot(Long slotId) {
        var slot = findSlot(slotId);
        if (slot.getStation().getOwner().getId().equals(currentUserService.getCurrentUser().getId())) {
            return slot;
        }
        throw new UnauthorizedException("You do not own this slot");
    }

    private void validateSlotWindow(SlotRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("End time must be after start time");
        }
    }

    private void ensureOwner() {
        if (currentUserService.getCurrentUser().getRole() != Role.OWNER) {
            throw new UnauthorizedException("Owner access required");
        }
    }
}
