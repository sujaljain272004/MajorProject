package com.chargeup.service;

import com.chargeup.dto.lifecycle.LifecycleStatusResponse;
import com.chargeup.entity.Booking;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class LifecycleRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public LifecycleRealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishBooking(LifecycleStatusResponse status) {
        messagingTemplate.convertAndSend("/topic/bookings/" + status.booking().id(), status);
    }

    public void publishOwnerActivity(Booking booking, String event) {
        messagingTemplate.convertAndSend(
            "/topic/owners/" + booking.getSlot().getStation().getOwner().getId() + "/operations",
            java.util.Map.of(
                "event", event,
                "bookingId", booking.getId(),
                "stationId", booking.getSlot().getStation().getId(),
                "stationName", booking.getSlot().getStation().getName(),
                "status", booking.getStatus().name()
            )
        );
    }
}
