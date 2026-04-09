package com.chargeup.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class SlotBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SlotService slotService;

    public SlotBroadcastService(SimpMessagingTemplate messagingTemplate, SlotService slotService) {
        this.messagingTemplate = messagingTemplate;
        this.slotService = slotService;
    }

    public void publishStationSlots(Long stationId) {
        messagingTemplate.convertAndSend("/topic/stations/" + stationId + "/slots", slotService.getSlotsForStation(stationId));
    }
}
