import { useEffect, useMemo, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useParams } from "react-router-dom";
import LoadingSpinner from "../components/LoadingSpinner";
import { mockPaymentSuccess } from "../services/paymentService";
import {
  checkInWithQr,
  getLifecycleStatus,
  requestExtension,
  startCharging,
  stopCharging
} from "../services/lifecycleService";

const wsUrl = import.meta.env.VITE_WS_URL || "/ws";

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString() : "-";
}

export default function ChargingLifecyclePage() {
  const { bookingId } = useParams();
  const [status, setStatus] = useState(null);
  const [qrCode, setQrCode] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const booking = status?.booking;
  const session = status?.session;
  const invoice = status?.invoice;

  const steps = useMemo(() => ["RESERVED", "BOOKED", "ARRIVED", "CHARGING", "COMPLETED"], []);
  const currentStep = Math.max(0, steps.indexOf(booking?.status));

  const loadStatus = async () => {
    try {
      const data = await getLifecycleStatus(bookingId);
      setStatus(data);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Unable to load charging lifecycle");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStatus();

    const refreshId = window.setInterval(loadStatus, 15000);
    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000
    });
    client.onConnect = () => {
      client.subscribe(`/topic/bookings/${bookingId}`, (message) => {
        setStatus(JSON.parse(message.body));
      });
    };
    client.activate();

    return () => {
      window.clearInterval(refreshId);
      client.deactivate();
    };
  }, [bookingId]);

  const runAction = async (action, message) => {
    try {
      setBusy(true);
      setError("");
      const data = await action();
      if (data?.booking) {
        setStatus(data);
      }
      setSuccess(message);
      await loadStatus();
    } catch (err) {
      setError(err.response?.data?.message || "Action failed");
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <LoadingSpinner label="Loading charging lifecycle..." />;
  }

  if (!booking) {
    return <div className="error-banner">Booking not found.</div>;
  }

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <span className="eyebrow">Charging lifecycle</span>
          <h1>{booking.stationName}</h1>
          <p>{booking.location}</p>
        </div>
        <a
          className="secondary-button"
          href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(booking.location)}`}
          rel="noreferrer"
          target="_blank"
        >
          Navigate
        </a>
      </div>

      {error && <div className="error-banner">{error}</div>}
      {success && <div className="success-banner">{success}</div>}

      <section className="detail-panel lifecycle-panel">
        <div className="lifecycle-steps">
          {steps.map((step, index) => (
            <div className={`lifecycle-step ${index <= currentStep ? "active" : ""}`} key={step}>
              <span>{index + 1}</span>
              <strong>{step}</strong>
            </div>
          ))}
        </div>
      </section>

      <div className="detail-grid">
        <section className="detail-panel">
          <h2>Booking</h2>
          <div className="info-list">
            <p>Status: {booking.status}</p>
            <p>Slot: {formatDateTime(booking.startTime)} - {formatDateTime(booking.endTime)}</p>
            <p>Amount: INR {booking.amount}</p>
            <p>Payment: {booking.paymentStatus || "NOT_STARTED"}</p>
          </div>
          <div className="row-actions">
            {booking.status === "RESERVED" && (
              <button className="primary-button" disabled={busy} onClick={() => runAction(() => mockPaymentSuccess(booking.id), "Payment confirmed for this test booking.")}>
                Confirm Payment
              </button>
            )}
            {booking.status === "CHARGING" && (
              <button className="secondary-button" disabled={busy} onClick={() => runAction(() => requestExtension(booking.id), "Extension request sent to owner.")}>
                Request Extension
              </button>
            )}
          </div>
        </section>

        <section className="detail-panel">
          <h2>QR check-in</h2>
          <div className="form-grid">
            <label>
              Station QR
              <input
                placeholder="Scan or enter station QR"
                value={qrCode}
                onChange={(event) => setQrCode(event.target.value)}
              />
            </label>
            <button
              className="primary-button"
              disabled={busy || booking.status !== "BOOKED"}
              onClick={() => runAction(() => checkInWithQr(booking.id, qrCode), "Physical check-in verified.")}
            >
              Verify Arrival
            </button>
          </div>
        </section>
      </div>

      <section className="detail-panel">
        <div className="section-heading">
          <h2>Charging session</h2>
          <span className="badge">{session?.status || booking.status}</span>
        </div>

        {session ? (
          <div className="charging-progress">
            <div className="progress-bar">
              <span style={{ width: `${session.progressPercent}%` }} />
            </div>
            <div className="stats-grid">
              <article className="metric-card">
                <span>Energy</span>
                <strong>{session.energyConsumed} kWh</strong>
              </article>
              <article className="metric-card">
                <span>Elapsed</span>
                <strong>{session.durationMinutes} min</strong>
              </article>
              <article className="metric-card">
                <span>Remaining</span>
                <strong>{session.estimatedRemainingMinutes} min</strong>
              </article>
            </div>
          </div>
        ) : (
          <div className="empty-state">Charging has not started yet.</div>
        )}

        <div className="row-actions">
          <button className="primary-button" disabled={busy || booking.status !== "ARRIVED"} onClick={() => runAction(() => startCharging(booking.id), "Charging started.")}>
            Start Charging
          </button>
          <button className="secondary-button" disabled={busy || booking.status !== "CHARGING"} onClick={() => runAction(() => stopCharging(booking.id), "Charging completed and invoice generated.")}>
            Stop Charging
          </button>
        </div>
      </section>

      {invoice && (
        <section className="detail-panel invoice-panel">
          <div className="section-heading">
            <h2>Invoice</h2>
            <button className="secondary-button" onClick={() => window.print()}>Download</button>
          </div>
          <div className="booking-summary">
            <div><span>Invoice</span><strong>#{invoice.id}</strong></div>
            <div><span>Energy</span><strong>{invoice.energyUsed} kWh</strong></div>
            <div><span>Duration</span><strong>{invoice.chargingDurationMinutes} min</strong></div>
            <div><span>GST</span><strong>INR {invoice.gst}</strong></div>
            <div><span>Total</span><strong>INR {invoice.amount}</strong></div>
          </div>
        </section>
      )}
    </section>
  );
}
