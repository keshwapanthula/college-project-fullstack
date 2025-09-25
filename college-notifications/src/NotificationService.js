import axios from "axios";
const API_URL = "http://localhost:8080/api/notifications/";
export const createsqlNotification = (notification) => {
  return axios.post(`${API_URL}`, notification);
};
export const createMongoNotification = (notification) => {
  return axios.post(`${API_URL}mongo`, notification);
};

export const getAllNotifications = () => {
  return axios.get(`${API_URL}`);
};

export const getAllMongoNotifications = () => {
  return axios.get(`${API_URL}mongo`);
};

export const deleteNotification = (id) => {
  return axios.delete(`${API_URL}${id}`);
};

export const deleteMongoNotification = (id) => {
  return axios.delete(`${API_URL}mongo/${id}`);
};

export const updateNotification = (id, notification) => {
  return axios.put(`${API_URL}${id}`, notification);
};

export const updateMongoNotification = (id, notification) => {
  return axios.put(`${API_URL}mongo/${id}`, notification);
};

export const getNotificationById = (id) => {
  return axios.get(`${API_URL}${id}`);
};

export const getMongoNotificationById = (id) => {
  return axios.get(`${API_URL}mongo/${id}`);
};

export const getNotificationsByUserId = (userId) => {
  return axios.get(`${API_URL}user/${userId}`);
};

export const getMongoNotificationsByUserId = (userId) => {
  return axios.get(`${API_URL}mongo/user/${userId}`);
};

export const markAsRead = (id) => {
  return axios.put(`${API_URL}${id}/read`);
};

export const markMongoAsRead = (id) => {
  return axios.put(`${API_URL}mongo/${id}/read`);
};

export const markAllAsRead = (userId) => {
  return axios.put(`${API_URL}user/${userId}/read`);
};

export const markAllMongoAsRead = (userId) => {
  return axios.put(`${API_URL}mongo/user/${userId}/read`);
};

export const getUnreadCount = (userId) => {
  return axios.get(`${API_URL}user/${userId}/unread/count`);
};

export const getMongoUnreadCount = (userId) => {
  return axios.get(`${API_URL}mongo/user/${userId}/unread/count`);
};

export const getNotificationsByType = (type) => {
  return axios.get(`${API_URL}type/${type}`);
};

export const getMongoNotificationsByType = (type) => {
  return axios.get(`${API_URL}mongo/type/${type}`);
};

export const getRecentNotifications = (userId, limit = 5) => {
  return axios.get(`${API_URL}user/${userId}/recent?limit=${limit}`);
};

export const getRecentMongoNotifications = (userId, limit = 5) => {
  return axios.get(`${API_URL}mongo/user/${userId}/recent?limit=${limit}`);
};