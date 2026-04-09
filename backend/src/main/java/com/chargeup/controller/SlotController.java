package com.chargeup.controller;

import com.chargeup.dto.slot.SlotRequest;
import com.chargeup.dto.slot.SlotResponse;
import com.chargeup.service.SlotBroadcastService;
import com.chargeup.service.SlotService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SlotController {

    private final SlotService slotService;
    private final SlotBroadcastService slotBroadcastService;

    public SlotController(SlotService slotService, SlotBroadcastService slotBroadcastService) {
        this.slotService = slotService;
        this.slotBroadcastService = slotBroadcastService;
    }

    @GetMapping("/stations/{stationId}/slots")
    public List<SlotResponse> getSlotsForStation(@PathVariable Long stationId) {
        return slotService.getSlotsForStation(stationId);
    }

    @GetMapping("/slots/{slotId}")
    public SlotResponse getSlot(@PathVariable Long slotId) {
        return slotService.getSlot(slotId);
    }

    @PostMapping("/stations/{stationId}/slots")
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse createSlot(@PathVariable Long stationId, @Valid @RequestBody SlotRequest request) {
        var slot = slotService.createSlot(stationId, request);
        slotBroadcastService.publishStationSlots(stationId);
        return slot;
    }

    @PutMapping("/slots/{slotId}")
    public SlotResponse updateSlot(@PathVariable Long slotId, @Valid @RequestBody SlotRequest request) {
        var slot = slotService.updateSlot(slotId, request);
        slotBroadcastService.publishStationSlots(slot.stationId());
        return slot;
    }

    @DeleteMapping("/slots/{slotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSlot(@PathVariable Long slotId) {
        Long stationId = slotService.findSlot(slotId).getStation().getId();
        slotService.deleteSlot(slotId);
        slotBroadcastService.publishStationSlots(stationId);
    }
}
