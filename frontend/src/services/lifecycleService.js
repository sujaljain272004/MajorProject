import api from "./api";

export const getLifecycleStatus = async (bookingId) => {
  const { data } = await api.get(`/lifecycle/bookings/${bookingId}`);
  return data;
};

export const getBookingQr = async (bookingId) => {
  const { data } = await api.get(`/lifecycle/bookings/${bookingId}/qr`);
  return data;
};

export const checkInWithQr = async (bookingId, qrCode) => {
  const { data } = await api.post("/lifecycle/checkins", { bookingId, qrCode });
  return data;
};

export const startCharging = async (bookingId) => {
  const { data } = await api.post(`/lifecycle/bookings/${bookingId}/start`);
  return data;
};

export const stopCharging = async (bookingId) => {
  const { data } = await api.post(`/lifecycle/bookings/${bookingId}/stop`);
  return data;
};

export const requestExtension = async (bookingId) => {
  const { data } = await api.post(`/lifecycle/bookings/${bookingId}/extension`);
  return data;
};

export const decideExtension = async (bookingId, approved) => {
  const { data } = await api.post(`/lifecycle/bookings/${bookingId}/extension/decision`, null, { params: { approved } });
  return data;
};

export const getOwnerLiveSessions = async () => {
  const { data } = await api.get("/lifecycle/owner/sessions");
  return data;
};
