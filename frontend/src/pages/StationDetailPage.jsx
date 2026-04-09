import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import LoadingSpinner from "../components/LoadingSpinner";
import MockMap from "../components/MockMap";
import SlotGrid from "../components/SlotGrid";
import { useAuth } from "../hooks/useAuth";
import { useStationSlots } from "../hooks/useStationSlots";
import { getStation } from "../services/stationService";

export default function StationDetailPage() {
  const { stationId } = useParams();
  const [station, setStation] = useState(null);
  const [loadingStation, setLoadingStation] = useState(true);
  const [error, setError] = useState("");
  const { user } = useAuth();
  const { slots, loading: loadingSlots, error: slotError } = useStationSlots(stationId);

  useEffect(() => {
    const loadStation = async () => {
      try {
        setLoadingStation(true);
        const data = await getStation(stationId);
        setStation(data);
        setError("");
      } catch (err) {
        setError(err.response?.data?.message || "Unable to load station");
      } finally {
        setLoadingStation(false);
      }
    };

    loadStation();
  }, [stationId]);

  if (loadingStation) {
    return <LoadingSpinner label="Loading station details..." />;
  }

  if (!station) {
    return <div className="error-banner">Station not found.</div>;
  }

  return (
    <section className="page-stack">
      <div className="detail-header">
        <div>
          <span className="eyebrow">Station detail</span>
          <h1>{station.name}</h1>
          <p>{station.location}</p>
        </div>
        {user?.role === "OWNER" && (
          <Link className="secondary-button" to="/admin">
            Manage in Admin Portal
          </Link>
        )}
      </div>

      {error && <div className="error-banner">{error}</div>}
      {slotError && <div className="error-banner">{slotError}</div>}

      <div className="detail-grid">
        <div className="detail-panel">
          <h2>Location Overview</h2>
          <MockMap station={station} />
        </div>
        <div className="detail-panel">
          <h2>Live Availability</h2>
          <div className="info-list">
            <p>Open slots update instantly over WebSocket.</p>
            <p>Total configured slots: {station.totalSlots}</p>
            <p>Current free slots: {slots.filter((slot) => slot.available).length}</p>
          </div>
        </div>
      </div>

      <section className="detail-panel">
        <div className="section-heading">
          <h2>Charging Slots</h2>
          <span className="badge">{loadingSlots ? "Refreshing..." : "Live"}</span>
        </div>
        <SlotGrid slots={slots} showBookingAction={user?.role === "DRIVER"} />
      </section>
    </section>
  );
}
