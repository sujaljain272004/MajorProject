package com.chargeup.controller;

import com.chargeup.dto.lifecycle.ChargingSessionResponse;
import com.chargeup.dto.lifecycle.LifecycleStatusResponse;
import com.chargeup.dto.lifecycle.QrCheckinRequest;
import com.chargeup.service.ChargingLifecycleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lifecycle")
public class LifecycleController {

    private final ChargingLifecycleService chargingLifecycleService;

    public LifecycleController(ChargingLifecycleService chargingLifecycleService) {
        this.chargingLifecycleService = chargingLifecycleService;
    }

    @GetMapping("/bookings/{bookingId}")
    public LifecycleStatusResponse getStatus(@PathVariable Long bookingId) {
        return chargingLifecycleService.getStatus(bookingId);
    }

    @GetMapping("/bookings/{bookingId}/qr")
    public Map<String, String> getQrCode(@PathVariable Long bookingId) {
        return Map.of("qrCode", chargingLifecycleService.getQrCode(bookingId));
    }

    @PostMapping("/checkins")
    public LifecycleStatusResponse checkIn(@Valid @RequestBody QrCheckinRequest request) {
        return chargingLifecycleService.checkIn(request);
    }

    @PostMapping("/bookings/{bookingId}/start")
    public LifecycleStatusResponse startCharging(@PathVariable Long bookingId) {
        return chargingLifecycleService.startCharging(bookingId);
    }

    @PostMapping("/bookings/{bookingId}/stop")
    public LifecycleStatusResponse stopCharging(@PathVariable Long bookingId) {
        return chargingLifecycleService.stopCharging(bookingId);
    }

    @PostMapping("/bookings/{bookingId}/extension")
    public LifecycleStatusResponse requestExtension(@PathVariable Long bookingId) {
        return chargingLifecycleService.requestExtension(bookingId);
    }

    @PostMapping("/bookings/{bookingId}/extension/decision")
    public LifecycleStatusResponse decideExtension(@PathVariable Long bookingId, @RequestParam boolean approved) {
        return chargingLifecycleService.decideExtension(bookingId, approved);
    }

    @GetMapping("/owner/sessions")
    public List<ChargingSessionResponse> getOwnerLiveSessions() {
        return chargingLifecycleService.getOwnerLiveSessions();
    }
}
