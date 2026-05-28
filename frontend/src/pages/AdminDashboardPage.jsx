import { useEffect, useState } from "react";
import LoadingSpinner from "../components/LoadingSpinner";
import { getOwnerBookings } from "../services/bookingService";
import { decideExtension, getBookingQr, getOwnerLiveSessions, stopCharging } from "../services/lifecycleService";
import { createSlot, deleteSlot, getSlotsByStation, updateSlot } from "../services/slotService";
import StationPinPreview from "../components/StationPinPreview";
import {
  createStation,
  deleteStation,
  getOwnerDashboard,
  setStationStatus,
  uploadStationPhoto,
  updateStation
} from "../services/stationService";

const initialStationForm = {
  name: "",
  location: "",
  city: "",
  pincode: "",
  latitude: "",
  longitude: "",
  chargerType: "DC Fast",
  chargingSpeedKw: "",
  connectorType: "CCS2",
  slotCount: "",
  pricePerKwh: "",
  openingHours: "24 hours",
  photoUrls: []
};

const initialSlotForm = {
  startTime: "",
  endTime: "",
  price: ""
};

function toLocalDateTimeInput(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  const timezoneOffset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - timezoneOffset).toISOString().slice(0, 16);
}

export default function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [ownerBookings, setOwnerBookings] = useState([]);
  const [liveSessions, setLiveSessions] = useState([]);
  const [bookingQrCodes, setBookingQrCodes] = useState({});
  const [selectedStationId, setSelectedStationId] = useState(null);
  const [selectedStationSlots, setSelectedStationSlots] = useState([]);
  const [stationForm, setStationForm] = useState(initialStationForm);
  const [stationPhoto, setStationPhoto] = useState(null);
  const [slotForm, setSlotForm] = useState(initialSlotForm);
  const [editingStationId, setEditingStationId] = useState(null);
  const [editingSlotId, setEditingSlotId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadDashboard = async (preferredStationId = selectedStationId) => {
    try {
      setLoading(true);
      const [dashboardData, bookingData] = await Promise.all([
        getOwnerDashboard(),
        getOwnerBookings()
      ]);
      const sessionData = await getOwnerLiveSessions();
      setDashboard(dashboardData);
      setOwnerBookings(bookingData);
      setLiveSessions(sessionData);

      const nextStationId = preferredStationId || dashboardData.stations[0]?.id || null;
      setSelectedStationId(nextStationId);

      if (nextStationId) {
        const slots = await getSlotsByStation(nextStationId);
        setSelectedStationSlots(slots);
      } else {
        setSelectedStationSlots([]);
      }
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Unable to load owner dashboard");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  useEffect(() => {
    const loadStationSlots = async () => {
      if (!selectedStationId) {
        setSelectedStationSlots([]);
        return;
      }

      try {
        const slots = await getSlotsByStation(selectedStationId);
        setSelectedStationSlots(slots);
      } catch (err) {
        setError(err.response?.data?.message || "Unable to load station slots");
      }
    };

    loadStationSlots();
  }, [selectedStationId]);

  const resetStationForm = () => {
    setStationForm(initialStationForm);
    setStationPhoto(null);
    setEditingStationId(null);
  };

  const resetSlotForm = () => {
    setSlotForm(initialSlotForm);
    setEditingSlotId(null);
  };

  const handleStationSubmit = async (event) => {
    event.preventDefault();
    try {
      setSaving(true);
      setSuccess("");
      const payload = {
        ...stationForm,
        latitude: Number(stationForm.latitude),
        longitude: Number(stationForm.longitude),
        chargingSpeedKw: Number(stationForm.chargingSpeedKw),
        slotCount: Number(stationForm.slotCount),
        pricePerKwh: Number(stationForm.pricePerKwh)
      };
      let savedStation;

      if (editingStationId) {
        savedStation = await updateStation(editingStationId, payload);
        setSuccess("Station updated.");
      } else {
        savedStation = await createStation(payload);
        setSuccess(savedStation.verificationStatus === "VERIFIED"
          ? "Station is live and starter slots are ready for nearby drivers."
          : "Station submitted for admin verification.");
      }

      if (stationPhoto) {
        await uploadStationPhoto(savedStation.id, stationPhoto);
      }

      resetStationForm();
      await loadDashboard(savedStation.id);
    } catch (err) {
      setError(err.response?.data?.message || "Unable to save station");
    } finally {
      setSaving(false);
    }
  };

  const handleSlotSubmit = async (event) => {
    event.preventDefault();
    if (!selectedStationId) {
      setError("Select a station before managing slots");
      return;
    }

    try {
      setSaving(true);
      setSuccess("");
      const payload = {
        ...slotForm,
        price: Number(slotForm.price)
      };

      if (editingSlotId) {
        await updateSlot(editingSlotId, payload);
        setSuccess("Slot updated.");
      } else {
        await createSlot(selectedStationId, payload);
        setSuccess("Slot created.");
      }

      resetSlotForm();
      await loadDashboard();
    } catch (err) {
      setError(err.response?.data?.message || "Unable to save slot");
    } finally {
      setSaving(false);
    }
  };

  const handleEditStation = (station) => {
    setEditingStationId(station.id);
    setStationForm({
      name: station.name,
      location: station.location,
      city: station.city,
      pincode: station.pincode,
      latitude: station.latitude,
      longitude: station.longitude,
      chargerType: station.chargerType,
      chargingSpeedKw: station.chargingSpeedKw,
      connectorType: station.connectorType,
      slotCount: station.slotCount,
      pricePerKwh: station.pricePerKwh,
      openingHours: station.openingHours,
      photoUrls: station.photoUrls || []
    });
  };

  const handlePinCurrentLocation = () => {
    if (!navigator.geolocation) {
      setError("Geolocation is unavailable in this browser");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setStationForm((current) => ({
          ...current,
          latitude: coords.latitude.toFixed(6),
          longitude: coords.longitude.toFixed(6)
        }));
        setError("");
      },
      () => setError("Location permission is required to pin from this device"),
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };

  const handleEditSlot = (slot) => {
    setEditingSlotId(slot.id);
    setSlotForm({
      startTime: toLocalDateTimeInput(slot.startTime),
      endTime: toLocalDateTimeInput(slot.endTime),
      price: slot.price
    });
  };

  const handleDeleteStation = async (stationId) => {
    try {
      await deleteStation(stationId);
      if (selectedStationId === stationId) {
        setSelectedStationId(null);
      }
      await loadDashboard();
    } catch (err) {
      setError(err.response?.data?.message || "Unable to delete station");
    }
  };

  const handleDeleteSlot = async (slotId) => {
    try {
      await deleteSlot(slotId);
      await loadDashboard();
    } catch (err) {
      setError(err.response?.data?.message || "Unable to delete slot");
    }
  };

  const handleStationStatus = async (station) => {
    try {
      const nextStatus = station.operatingStatus === "PAUSED" ? "ACTIVE" : "PAUSED";
      await setStationStatus(station.id, nextStatus);
      setSuccess(nextStatus === "ACTIVE" ? "Station resumed." : "Station paused.");
      await loadDashboard(station.id);
    } catch (err) {
      setError(err.response?.data?.message || "Unable to update station status");
    }
  };

  const handleShowQr = async (bookingId) => {
    try {
      const data = await getBookingQr(bookingId);
      setBookingQrCodes((current) => ({ ...current, [bookingId]: data.qrCode }));
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Unable to load station QR");
    }
  };

  const handleExtensionDecision = async (bookingId, approved) => {
    try {
      await decideExtension(bookingId, approved);
      setSuccess(approved ? "Extension approved." : "Extension rejected.");
      await loadDashboard();
    } catch (err) {
      setError(err.response?.data?.message || "Unable to update extension request");
    }
  };

  const handleStopCharging = async (bookingId) => {
    try {
      await stopCharging(bookingId);
      setSuccess("Charging stopped and invoice generated.");
      await loadDashboard();
    } catch (err) {
      setError(err.response?.data?.message || "Unable to stop charging session");
    }
  };

  if (loading) {
    return <LoadingSpinner label="Loading owner dashboard..." />;
  }

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <span className="eyebrow">Owner portal</span>
          <h1>Station operations and revenue dashboard</h1>
          <p>Manage stations, add charging slots, track bookings, and monitor revenue from confirmed sessions.</p>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}
      {success && <div className="success-banner">{success}</div>}

      <div className="stats-grid">
        <article className="metric-card">
          <span>Total Stations</span>
          <strong>{dashboard?.totalStations || 0}</strong>
        </article>
        <article className="metric-card">
          <span>Total Slots</span>
          <strong>{dashboard?.totalSlots || 0}</strong>
        </article>
        <article className="metric-card">
          <span>Confirmed Bookings</span>
          <strong>{dashboard?.confirmedBookings || 0}</strong>
        </article>
        <article className="metric-card">
          <span>Revenue</span>
          <strong>INR {dashboard?.totalRevenue || 0}</strong>
        </article>
      </div>

      <div className="admin-grid">
        <section className="detail-panel">
          <div className="section-heading">
            <h2>{editingStationId ? "Edit station" : "Add station"}</h2>
            {editingStationId && (
              <button className="ghost-button" onClick={resetStationForm}>
                Reset
              </button>
            )}
          </div>

          <form className="form-grid" onSubmit={handleStationSubmit}>
            <label>
              Name
              <input
                required
                value={stationForm.name}
                onChange={(event) => setStationForm((current) => ({ ...current, name: event.target.value }))}
              />
            </label>
            <label>
              Location
              <input
                required
                value={stationForm.location}
                onChange={(event) => setStationForm((current) => ({ ...current, location: event.target.value }))}
              />
            </label>
            <label>
              City
              <input
                required
                value={stationForm.city}
                onChange={(event) => setStationForm((current) => ({ ...current, city: event.target.value }))}
              />
            </label>
            <label>
              Pincode
              <input
                required
                value={stationForm.pincode}
                onChange={(event) => setStationForm((current) => ({ ...current, pincode: event.target.value }))}
              />
            </label>
            <label>
              Latitude
              <input
                required
                type="number"
                step="0.0001"
                value={stationForm.latitude}
                onChange={(event) => setStationForm((current) => ({ ...current, latitude: event.target.value }))}
              />
            </label>
            <label>
              Longitude
              <input
                required
                type="number"
                step="0.0001"
                value={stationForm.longitude}
                onChange={(event) => setStationForm((current) => ({ ...current, longitude: event.target.value }))}
              />
            </label>
            <button className="secondary-button" onClick={handlePinCurrentLocation} type="button">
              Pin Device Location
            </button>
            <StationPinPreview latitude={stationForm.latitude} longitude={stationForm.longitude} />
            <label>
              Charger Type
              <input
                required
                value={stationForm.chargerType}
                onChange={(event) => setStationForm((current) => ({ ...current, chargerType: event.target.value }))}
              />
            </label>
            <label>
              Connector Type
              <input
                required
                value={stationForm.connectorType}
                onChange={(event) => setStationForm((current) => ({ ...current, connectorType: event.target.value }))}
              />
            </label>
            <label>
              Charging Speed (kW)
              <input
                required
                min="1"
                type="number"
                step="0.01"
                value={stationForm.chargingSpeedKw}
                onChange={(event) => setStationForm((current) => ({ ...current, chargingSpeedKw: event.target.value }))}
              />
            </label>
            <label>
              Number of Slots
              <input
                required
                min="1"
                type="number"
                value={stationForm.slotCount}
                onChange={(event) => setStationForm((current) => ({ ...current, slotCount: event.target.value }))}
              />
            </label>
            <label>
              Dynamic Price (INR/kWh)
              <input
                required
                min="1"
                type="number"
                step="0.01"
                value={stationForm.pricePerKwh}
                onChange={(event) => setStationForm((current) => ({ ...current, pricePerKwh: event.target.value }))}
              />
            </label>
            <label>
              Opening Hours
              <input
                required
                value={stationForm.openingHours}
                onChange={(event) => setStationForm((current) => ({ ...current, openingHours: event.target.value }))}
              />
            </label>
            <label>
              Station Photo
              <input
                accept="image/jpeg,image/png,image/webp"
                type="file"
                onChange={(event) => setStationPhoto(event.target.files?.[0] || null)}
              />
            </label>
            <button className="primary-button" disabled={saving} type="submit">
              {saving ? "Saving..." : editingStationId ? "Update Station" : "Create Station"}
            </button>
          </form>
        </section>

        <section className="detail-panel">
          <div className="section-heading">
            <h2>Your stations</h2>
            <span className="badge">{dashboard?.stations?.length || 0} configured</span>
          </div>

          <div className="owner-stations">
            {(dashboard?.stations || []).map((station) => (
              <article className={`owner-station-card ${selectedStationId === station.id ? "selected" : ""}`} key={station.id}>
                <button className="link-button" onClick={() => setSelectedStationId(station.id)}>
                  <strong>{station.name}</strong>
                  <span>{station.location}</span>
                  <span>{station.availableSlots}/{station.totalSlots} available</span>
                  <span>{station.verificationStatus} / {station.operatingStatus}</span>
                </button>
                <div className="row-actions">
                  <button className="secondary-button" onClick={() => handleEditStation(station)}>
                    Edit
                  </button>
                  <button className="ghost-button" onClick={() => handleDeleteStation(station.id)}>
                    Delete
                  </button>
                  <button className="ghost-button" onClick={() => handleStationStatus(station)}>
                    {station.operatingStatus === "PAUSED" ? "Resume" : "Pause"}
                  </button>
                </div>
              </article>
            ))}
          </div>
        </section>
      </div>

      <div className="admin-grid">
        <section className="detail-panel">
          <div className="section-heading">
            <h2>{editingSlotId ? "Edit slot" : "Add slot"}</h2>
            {editingSlotId && (
              <button className="ghost-button" onClick={resetSlotForm}>
                Reset
              </button>
            )}
          </div>

          <form className="form-grid" onSubmit={handleSlotSubmit}>
            <label>
              Start Time
              <input
                required
                type="datetime-local"
                value={slotForm.startTime}
                onChange={(event) => setSlotForm((current) => ({ ...current, startTime: event.target.value }))}
              />
            </label>
            <label>
              End Time
              <input
                required
                type="datetime-local"
                value={slotForm.endTime}
                onChange={(event) => setSlotForm((current) => ({ ...current, endTime: event.target.value }))}
              />
            </label>
            <label>
              Price
              <input
                required
                type="number"
                min="1"
                step="0.01"
                value={slotForm.price}
                onChange={(event) => setSlotForm((current) => ({ ...current, price: event.target.value }))}
              />
            </label>
            <button className="primary-button" disabled={saving || !selectedStationId} type="submit">
              {saving ? "Saving..." : editingSlotId ? "Update Slot" : "Create Slot"}
            </button>
          </form>
        </section>

        <section className="detail-panel">
          <div className="section-heading">
            <h2>Slots for selected station</h2>
            <span className="badge">{selectedStationSlots.length} slots</span>
          </div>

          {!selectedStationId ? (
            <div className="empty-state">Select a station to manage its slots.</div>
          ) : (
            <div className="table-card">
              <table>
                <thead>
                  <tr>
                    <th>Start</th>
                    <th>End</th>
                    <th>Price</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedStationSlots.map((slot) => (
                    <tr key={slot.id}>
                      <td>{new Date(slot.startTime).toLocaleString()}</td>
                      <td>{new Date(slot.endTime).toLocaleString()}</td>
                      <td>INR {slot.price}</td>
                      <td>{slot.available ? "Available" : "Booked"}</td>
                      <td className="table-actions">
                        <button className="secondary-button" onClick={() => handleEditSlot(slot)}>
                          Edit
                        </button>
                        <button className="ghost-button" onClick={() => handleDeleteSlot(slot.id)}>
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>

      <section className="detail-panel">
        <div className="section-heading">
          <h2>Live charging sessions</h2>
          <span className="badge">{liveSessions.length} active</span>
        </div>
        {!liveSessions.length ? (
          <div className="empty-state">No vehicles are charging right now.</div>
        ) : (
          <div className="session-grid">
            {liveSessions.map((session) => (
              <article className="session-card" key={session.id}>
                <strong>{session.stationName}</strong>
                <span>{session.status}</span>
                <div className="progress-bar"><span style={{ width: `${session.progressPercent}%` }} /></div>
                <p>{session.energyConsumed} kWh · {session.durationMinutes} min elapsed</p>
                <div className="row-actions">
                  <button className="ghost-button" onClick={() => handleStopCharging(session.bookingId)}>Stop</button>
                  <button className="secondary-button" onClick={() => handleExtensionDecision(session.bookingId, true)}>Approve extension</button>
                  <button className="ghost-button" onClick={() => handleExtensionDecision(session.bookingId, false)}>Reject</button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="detail-panel">
        <div className="section-heading">
          <h2>Recent bookings</h2>
          <span className="badge">{ownerBookings.length} total</span>
        </div>
        <div className="table-card">
          <table>
            <thead>
              <tr>
                <th>Driver</th>
                <th>Station</th>
                <th>Window</th>
                <th>Amount</th>
                <th>Booking</th>
                <th>Payment</th>
                <th>Ops</th>
              </tr>
            </thead>
            <tbody>
              {ownerBookings.map((booking) => (
                <tr key={booking.id}>
                  <td>{booking.bookedBy}</td>
                  <td>{booking.stationName}</td>
                  <td>{new Date(booking.startTime).toLocaleString()}</td>
                  <td>INR {booking.amount}</td>
                  <td>{booking.status}</td>
                  <td>{booking.paymentStatus || "NOT_STARTED"}</td>
                  <td className="table-actions">
                    {["BOOKED", "ARRIVED"].includes(booking.status) && (
                      <button className="secondary-button" onClick={() => handleShowQr(booking.id)}>
                        QR
                      </button>
                    )}
                    {booking.status === "CHARGING" && (
                      <button className="ghost-button" onClick={() => handleStopCharging(booking.id)}>
                        Stop
                      </button>
                    )}
                    {bookingQrCodes[booking.id] && <code className="qr-code">{bookingQrCodes[booking.id]}</code>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}
