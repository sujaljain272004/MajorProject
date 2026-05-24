import { useEffect, useState } from "react";
import LoadingSpinner from "../components/LoadingSpinner";
import NearbyStationsMap from "../components/NearbyStationsMap";
import StationCard from "../components/StationCard";
import { useAuth } from "../hooks/useAuth";
import { getNearbyStations } from "../services/stationService";

export default function DashboardPage() {
  const [stations, setStations] = useState([]);
  const [position, setPosition] = useState(null);
  const [locationMode, setLocationMode] = useState("locating");
  const [radiusKm, setRadiusKm] = useState(10);
  const [search, setSearch] = useState({ city: "", pincode: "" });
  const [filters, setFilters] = useState({
    connectorType: "",
    maxPrice: "",
    fastCharging: false,
    availableOnly: true
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const { user } = useAuth();

  useEffect(() => {
    if (!navigator.geolocation) {
      setLocationMode("fallback");
      setLoading(false);
      return;
    }

    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setPosition({ latitude: coords.latitude, longitude: coords.longitude });
        setLocationMode("gps");
      },
      () => {
        setLocationMode("fallback");
        setLoading(false);
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    );
  }, []);

  const buildParams = () => {
    const params = {
      radiusKm,
      connectorType: filters.connectorType || undefined,
      maxPrice: filters.maxPrice || undefined,
      fastCharging: filters.fastCharging || undefined,
      availableOnly: filters.availableOnly || undefined
    };

    if (locationMode === "gps" && position) {
      params.latitude = position.latitude;
      params.longitude = position.longitude;
    } else {
      params.city = search.city || undefined;
      params.pincode = search.pincode || undefined;
    }
    return params;
  };

  const loadStations = async () => {
    if (locationMode !== "gps" && !search.city.trim() && !search.pincode.trim()) {
      setStations([]);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      const data = await getNearbyStations(buildParams());
      setStations(data);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Unable to load nearby stations");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStations();
    if (locationMode !== "gps") {
      return undefined;
    }

    const refreshId = window.setInterval(loadStations, 30000);
    return () => window.clearInterval(refreshId);
  }, [position, locationMode, radiusKm, filters.connectorType, filters.maxPrice, filters.fastCharging, filters.availableOnly]);

  const handleFallbackSubmit = async (event) => {
    event.preventDefault();
    setLocationMode("fallback");
    await loadStations();
  };

  const mapCenter = position || (stations[0] ? {
    latitude: stations[0].latitude,
    longitude: stations[0].longitude
  } : null);

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <span className="eyebrow">Welcome back</span>
          <h1>{user?.role === "OWNER" ? "Network snapshot" : "Nearby EV charging stations"}</h1>
          <p>Stations are fetched from live GPS, then refreshed while availability changes.</p>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <section className="detail-panel nearby-controls">
        <div className="section-heading">
          <h2>Find a compatible charger</h2>
          <span className="badge">{locationMode === "gps" ? "Live GPS" : "City or pincode"}</span>
        </div>

        {locationMode !== "gps" && (
          <form className="search-row" onSubmit={handleFallbackSubmit}>
            <label>
              City
              <input
                value={search.city}
                onChange={(event) => setSearch((current) => ({ ...current, city: event.target.value }))}
                placeholder="Bengaluru"
              />
            </label>
            <label>
              Pincode
              <input
                value={search.pincode}
                onChange={(event) => setSearch((current) => ({ ...current, pincode: event.target.value }))}
                placeholder="560001"
              />
            </label>
            <button className="primary-button" type="submit">Search</button>
          </form>
        )}

        <div className="filter-grid">
          <label>
            Radius
            <select value={radiusKm} onChange={(event) => setRadiusKm(Number(event.target.value))}>
              <option value={5}>5 km</option>
              <option value={10}>10 km</option>
              <option value={25}>25 km</option>
            </select>
          </label>
          <label>
            Connector
            <input
              value={filters.connectorType}
              onChange={(event) => setFilters((current) => ({ ...current, connectorType: event.target.value }))}
              placeholder="CCS2"
            />
          </label>
          <label>
            Max price/kWh
            <input
              min="1"
              type="number"
              value={filters.maxPrice}
              onChange={(event) => setFilters((current) => ({ ...current, maxPrice: event.target.value }))}
              placeholder="25"
            />
          </label>
          <label className="check-control">
            <input
              checked={filters.fastCharging}
              onChange={(event) => setFilters((current) => ({ ...current, fastCharging: event.target.checked }))}
              type="checkbox"
            />
            Fast charging
          </label>
          <label className="check-control">
            <input
              checked={filters.availableOnly}
              onChange={(event) => setFilters((current) => ({ ...current, availableOnly: event.target.checked }))}
              type="checkbox"
            />
            Available now
          </label>
        </div>
      </section>

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
          <span>Search Radius</span>
          <strong>{radiusKm} km</strong>
        </article>
      </div>

      <NearbyStationsMap center={mapCenter} stations={stations} />

      {loading ? (
        <LoadingSpinner label="Loading verified nearby stations..." />
      ) : !stations.length ? (
        <div className="empty-state">
          {locationMode === "fallback" && !search.city && !search.pincode
            ? "GPS is unavailable. Search by city or pincode to find verified stations."
            : "No verified compatible stations were found in this search area."}
          <button className="secondary-button retry-button" onClick={loadStations}>Retry</button>
        </div>
      ) : (
        <div className="station-grid">
          {stations.map((station) => (
            <StationCard key={station.id} station={station} />
          ))}
        </div>
      )}
    </section>
  );
}
