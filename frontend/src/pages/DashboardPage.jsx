import { useEffect, useState } from "react";
import LoadingSpinner from "../components/LoadingSpinner";
import StationCard from "../components/StationCard";
import { useAuth } from "../hooks/useAuth";
import { getStations } from "../services/stationService";

export default function DashboardPage() {
  const [stations, setStations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const { user } = useAuth();

  useEffect(() => {
    const loadStations = async () => {
      try {
        setLoading(true);
        const data = await getStations();
        setStations(data);
        setError("");
      } catch (err) {
        setError(err.response?.data?.message || "Unable to load stations");
      } finally {
        setLoading(false);
      }
    };

    loadStations();
  }, []);

  if (loading) {
    return <LoadingSpinner label="Loading nearby charging stations..." />;
  }

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <span className="eyebrow">Welcome back</span>
          <h1>{user?.role === "OWNER" ? "Network snapshot" : "Nearby EV charging stations"}</h1>
          <p>Live slot availability updates arrive instantly as bookings are created or canceled.</p>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="stats-grid">
        <article className="metric-card">
          <span>Total Stations</span>
          <strong>{stations.length}</strong>
        </article>
        <article className="metric-card">
          <span>Available Slots</span>
          <strong>{stations.reduce((sum, station) => sum + station.availableSlots, 0)}</strong>
        </article>
        <article className="metric-card">
          <span>Coverage</span>
          <strong>Bengaluru</strong>
        </article>
      </div>

      <div className="station-grid">
        {stations.map((station) => (
          <StationCard key={station.id} station={station} />
        ))}
      </div>
    </section>
  );
}
