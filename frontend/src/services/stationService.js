import api from "./api";

export const getStations = async () => {
  const { data } = await api.get("/stations");
  return data;
};

export const getStation = async (stationId) => {
  const { data } = await api.get(`/stations/${stationId}`);
  return data;
};

export const getOwnerStations = async () => {
  const { data } = await api.get("/stations/owner");
  return data;
};

export const getOwnerDashboard = async () => {
  const { data } = await api.get("/stations/owner/dashboard");
  return data;
};

export const createStation = async (payload) => {
  const { data } = await api.post("/stations", payload);
  return data;
};

export const updateStation = async (stationId, payload) => {
  const { data } = await api.put(`/stations/${stationId}`, payload);
  return data;
};

export const deleteStation = async (stationId) => {
  await api.delete(`/stations/${stationId}`);
};
