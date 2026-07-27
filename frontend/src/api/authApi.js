import axiosClient from './axiosClient';

export const register = (payload) =>
  axiosClient.post('/api/auth/register', payload).then((res) => res.data.data);

export const login = (payload) =>
  axiosClient.post('/api/auth/login', payload).then((res) => res.data.data);

export const refresh = (refreshToken) =>
  axiosClient.post('/api/auth/refresh', { refreshToken }).then((res) => res.data.data);

export const forgotPassword = (email) =>
  axiosClient.post('/api/auth/forgot-password', { email }).then((res) => res.data.data);

export const resetPassword = (token, newPassword) =>
  axiosClient.post('/api/auth/reset-password', { token, newPassword }).then((res) => res.data.data);
