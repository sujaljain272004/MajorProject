import { Link } from "react-router-dom";

function formatDateTime(value) {
  return new Date(value).toLocaleString();
}

export default function SlotGrid({ slots, showBookingAction = true }) {
  if (!slots.length) {
    return <div className="empty-state">No slots have been configured for this station yet.</div>;
  }

  return (
    <div className="slot-grid">
      {slots.map((slot) => (
        <article className={`slot-card ${slot.available ? "available" : "booked"}`} key={slot.id}>
          <div className="slot-card-top">
            <span className={`status-pill ${slot.available ? "success" : "danger"}`}>
              {slot.state || (slot.available ? "AVAILABLE" : "BOOKED")}
            </span>
            <strong>INR {slot.price}</strong>
          </div>
          <h4>{slot.stationName}</h4>
          <p>{formatDateTime(slot.startTime)}</p>
          <p>Ends {formatDateTime(slot.endTime)}</p>
          {showBookingAction && (
            <Link
              className={`primary-button full-width ${!slot.available ? "disabled-link" : ""}`}
              to={slot.available ? `/booking/${slot.id}` : "#"}
              onClick={(event) => {
                if (!slot.available) {
                  event.preventDefault();
                }
              }}
            >
              {slot.available ? "Book Slot" : "Unavailable"}
            </Link>
          )}
        </article>
      ))}
    </div>
  );
}
