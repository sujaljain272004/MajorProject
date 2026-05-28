import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import LoadingSpinner from "../components/LoadingSpinner";
import { cancelBooking, getMyBookings } from "../services/bookingService";

function formatDateTime(value) {
  return new Date(value).toLocaleString();
}

export default function UserBookingsPage() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const loadBookings = async () => {
    try {
      setLoading(true);
      const data = await getMyBookings();
      setBookings(data);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Unable to load bookings");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBookings();
  }, []);

  const handleCancel = async (bookingId) => {
    try {
      await cancelBooking(bookingId);
      await loadBookings();
    } catch (err) {
      setError(err.response?.data?.message || "Unable to cancel booking");
    }
  };

  if (loading) {
    return <LoadingSpinner label="Loading your bookings..." />;
  }

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <span className="eyebrow">My bookings</span>
          <h1>Charging session history</h1>
          <p>Review past reservations, continue pending payments, or cancel upcoming sessions.</p>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {!bookings.length ? (
        <div className="empty-state">You do not have any bookings yet.</div>
      ) : (
        <div className="table-card">
          <table>
            <thead>
              <tr>
                <th>Station</th>
                <th>Window</th>
                <th>Amount</th>
                <th>Booking</th>
                <th>Payment</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {bookings.map((booking) => (
                <tr key={booking.id}>
                  <td>{booking.stationName}</td>
                  <td>{formatDateTime(booking.startTime)}</td>
                  <td>INR {booking.amount}</td>
                  <td>{booking.status}</td>
                  <td>{booking.paymentStatus || "NOT_STARTED"}</td>
                  <td className="table-actions">
                    {booking.status === "RESERVED" && (
                      <button className="secondary-button" onClick={() => navigate(`/payment/${booking.id}`)}>
                        Pay Now
                      </button>
                    )}
                    {["BOOKED", "ARRIVED", "CHARGING", "COMPLETED"].includes(booking.status) && (
                      <button className="primary-button" onClick={() => navigate(`/charging/${booking.id}`)}>
                        Charging Flow
                      </button>
                    )}
                    {booking.status !== "CANCELLED" && booking.status !== "COMPLETED" && (
                      <button className="ghost-button" onClick={() => handleCancel(booking.id)}>
                        Cancel
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
