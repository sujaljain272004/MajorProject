import api from "./api";

export const createBooking = async (slotId) => {
  const { data } = await api.post("/bookings", { slotId });
  return data;
};

export const cancelBooking = async (bookingId) => {
  const { data } = await api.post(`/bookings/${bookingId}/cancel`);
  return data;
};

export const getMyBookings = async () => {
  const { data } = await api.get("/bookings");
  return data;
};

export const getBooking = async (bookingId) => {
  const { data } = await api.get(`/bookings/${bookingId}`);
  return data;
};

export const getOwnerBookings = async () => {
  const { data } = await api.get("/bookings/owner/all");
  return data;
};
