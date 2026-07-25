import axiosClient from './axiosClient';

export const getAnalytics = (from, to) =>
  axiosClient.get('/api/analytics', { params: { from, to } }).then((res) => res.data.data);
