import axiosClient from './axiosClient';

export const getProfile = () =>
  axiosClient.get('/api/users/profile').then((res) => res.data.data);

export const updateProfile = (payload) =>
  axiosClient.put('/api/users/profile', payload).then((res) => res.data.data);
