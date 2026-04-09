import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import LoadingSpinner from "../components/LoadingSpinner";
import { createBooking } from "../services/bookingService";
import { getSlot } from "../services/slotService";

function formatDateTime(value) {
  return new Date(value).toLocaleString();
}

export default function BookingPage() {
  const { slotId } = useParams();
  const navigate = useNavigate();
  const [slot, setSlot] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadSlot = async () => {
      try {
        setLoading(true);
        const data = await getSlot(slotId);
        setSlot(data);
        setError("");
      } catch (err) {
        setError(err.response?.data?.message || "Unable to load slot");
      } finally {
        setLoading(false);
      }
    };

    loadSlot();
  }, [slotId]);

  const handleBooking = async () => {
    try {
      setSubmitting(true);
      const booking = await createBooking(Number(slotId));
      navigate(`/payment/${booking.id}`);
    } catch (err) {
      setError(err.response?.data?.message || "Booking failed");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <LoadingSpinner label="Loading booking details..." />;
  }

  if (!slot) {
    return <div className="error-banner">Slot not found.</div>;
  }

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <span className="eyebrow">Confirm booking</span>
          <h1>Review your charging slot</h1>
          <p>ChargeUp locks the slot inside a transaction to prevent double booking.</p>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <article className="booking-summary">
        <div>
          <span>Station</span>
          <strong>{slot.stationName}</strong>
        </div>
        <div>
          <span>Start</span>
          <strong>{formatDateTime(slot.startTime)}</strong>
        </div>
        <div>
          <span>End</span>
          <strong>{formatDateTime(slot.endTime)}</strong>
        </div>
        <div>
          <span>Price</span>
          <strong>INR {slot.price}</strong>
        </div>
        <div>
          <span>Status</span>
          <strong>{slot.available ? "Available" : "Already booked"}</strong>
        </div>
      </article>

      <button className="primary-button fit-button" disabled={!slot.available || submitting} onClick={handleBooking}>
        {submitting ? "Locking slot..." : slot.available ? "Book and Continue to Payment" : "Slot Unavailable"}
      </button>
    </section>
  );
}
