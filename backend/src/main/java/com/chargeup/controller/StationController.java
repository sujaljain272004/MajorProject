package com.chargeup.controller;

import com.chargeup.dto.station.OwnerDashboardResponse;
import com.chargeup.dto.station.StationRequest;
import com.chargeup.dto.station.StationResponse;
import com.chargeup.dto.station.StationSearchRequest;
import com.chargeup.entity.StationOperatingStatus;
import com.chargeup.service.StationPhotoService;
import com.chargeup.service.StationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;
    private final StationPhotoService stationPhotoService;

    public StationController(StationService stationService, StationPhotoService stationPhotoService) {
        this.stationService = stationService;
        this.stationPhotoService = stationPhotoService;
    }

    @GetMapping
    public List<StationResponse> getStations() {
        return stationService.getAllStations();
    }

    @GetMapping("/nearby")
    public List<StationResponse> getNearbyStations(@Valid @ModelAttribute StationSearchRequest request) {
        return stationService.searchNearbyStations(request);
    }

    @GetMapping("/photos/{filename}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String filename) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(stationPhotoService.load(filename));
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

    @PostMapping(value = "/{stationId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StationResponse uploadStationPhoto(@PathVariable Long stationId, @RequestParam("file") MultipartFile file) {
        return stationService.uploadPhoto(stationId, file);
    }

    @PostMapping("/{stationId}/verification")
    public StationResponse verifyStation(@PathVariable Long stationId, @RequestParam boolean verified) {
        return stationService.verifyStation(stationId, verified);
    }

    @PostMapping("/{stationId}/status")
    public StationResponse setStationStatus(@PathVariable Long stationId, @RequestParam StationOperatingStatus status) {
        return stationService.setOperatingStatus(stationId, status);
    }

    @DeleteMapping("/{stationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStation(@PathVariable Long stationId) {
        stationService.deleteStation(stationId);
    }
}
