import { useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { getSlotsByStation } from "../services/slotService";

const wsUrl = import.meta.env.VITE_WS_URL || "/ws";

export function useStationSlots(stationId) {
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!stationId) {
      return undefined;
    }

    let isActive = true;
    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000
    });

    const loadSlots = async () => {
      try {
        setLoading(true);
        const data = await getSlotsByStation(stationId);
        if (isActive) {
          setSlots(data);
          setError("");
        }
      } catch (err) {
        if (isActive) {
          setError(err.response?.data?.message || "Unable to load slots");
        }
      } finally {
        if (isActive) {
          setLoading(false);
        }
      }
    };

    loadSlots();

    client.onConnect = () => {
      client.subscribe(`/topic/stations/${stationId}/slots`, (message) => {
        if (isActive) {
          setSlots(JSON.parse(message.body));
        }
      });
    };

    client.activate();

    return () => {
      isActive = false;
      client.deactivate();
    };
  }, [stationId]);

  return { slots, setSlots, loading, error };
}
