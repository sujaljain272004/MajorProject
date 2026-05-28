import api from "./api";

export const createOrder = async (bookingId) => {
  const { data } = await api.post("/payments/order", { bookingId });
  return data;
};

export const verifyPayment = async (payload) => {
  const { data } = await api.post("/payments/verify", payload);
  return data;
};

export const getBookingPayment = async (bookingId) => {
  const { data } = await api.get(`/payments/booking/${bookingId}`);
  return data;
};

export const mockPaymentSuccess = async (bookingId) => {
  const { data } = await api.post(`/payments/booking/${bookingId}/mock-success`);
  return data;
};
