package com.chargeup.controller;

import com.chargeup.dto.station.OwnerDashboardResponse;
import com.chargeup.dto.station.StationRequest;
import com.chargeup.dto.station.StationResponse;
import com.chargeup.service.StationService;
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
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public List<StationResponse> getStations() {
        return stationService.getAllStations();
    }

    @GetMapping("/{stationId}")
    public StationResponse getStation(@PathVariable Long stationId) {
        return stationService.getStation(stationId);
    }

    @GetMapping("/owner")
    public List<StationResponse> getMyStations() {
        return stationService.getMyStations();
    }

    @GetMapping("/owner/dashboard")
    public OwnerDashboardResponse getOwnerDashboard() {
        return stationService.getOwnerDashboard();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StationResponse createStation(@Valid @RequestBody StationRequest request) {
        return stationService.createStation(request);
    }

    @PutMapping("/{stationId}")
    public StationResponse updateStation(@PathVariable Long stationId, @Valid @RequestBody StationRequest request) {
        return stationService.updateStation(stationId, request);
    }

    @DeleteMapping("/{stationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStation(@PathVariable Long stationId) {
        stationService.deleteStation(stationId);
    }
}
