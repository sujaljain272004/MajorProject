import { Link } from "react-router-dom";

export default function StationCard({ station }) {
  return (
    <article className="station-card">
      <div className="station-card-header">
        <div>
          <h3>{station.name}</h3>
          <p>{station.location}</p>
        </div>
        <span className="badge">{station.availableSlots}/{station.totalSlots} free</span>
      </div>

      <div className="station-metrics">
        <div>
          <span>Lat</span>
          <strong>{station.latitude}</strong>
        </div>
        <div>
          <span>Lng</span>
          <strong>{station.longitude}</strong>
        </div>
      </div>

      <Link className="primary-button full-width" to={`/stations/${station.id}`}>
        View Slots
      </Link>
    </article>
  );
}
