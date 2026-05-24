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
          <span>Distance</span>
          <strong>{station.distanceKm == null ? station.city : `${station.distanceKm} km`}</strong>
        </div>
        <div>
          <span>Price</span>
          <strong>INR {station.pricePerKwh}/kWh</strong>
        </div>
      </div>

      <div className="station-tags">
        <span>{station.connectorType}</span>
        <span>{station.chargingSpeedKw} kW</span>
        {station.estimatedWaitMinutes > 0 && <span>Wait {station.estimatedWaitMinutes} min</span>}
      </div>

      <a
        className="secondary-button full-width"
        href={`https://www.google.com/maps/dir/?api=1&destination=${station.latitude},${station.longitude}`}
        rel="noreferrer"
        target="_blank"
      >
        Navigate
      </a>
      <Link className="primary-button full-width" to={`/stations/${station.id}`}>
        View Slots
      </Link>
    </article>
  );
}
