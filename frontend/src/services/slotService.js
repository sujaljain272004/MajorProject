import api from "./api";

export const getSlotsByStation = async (stationId) => {
  const { data } = await api.get(`/stations/${stationId}/slots`);
  return data;
};

export const getSlot = async (slotId) => {
  const { data } = await api.get(`/slots/${slotId}`);
  return data;
};

export const createSlot = async (stationId, payload) => {
  const { data } = await api.post(`/stations/${stationId}/slots`, payload);
  return data;
};

export const updateSlot = async (slotId, payload) => {
  const { data } = await api.put(`/slots/${slotId}`, payload);
  return data;
};

export const deleteSlot = async (slotId) => {
  await api.delete(`/slots/${slotId}`);
};
