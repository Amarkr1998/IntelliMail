import axiosClient from './axiosClient';

export const getTemplates = (page = 0, size = 20) =>
  axiosClient.get('/api/templates', { params: { page, size } }).then((res) => res.data.data);

export const createTemplate = (payload) =>
  axiosClient.post('/api/templates', payload).then((res) => res.data.data);

export const updateTemplate = (id, payload) =>
  axiosClient.put(`/api/templates/${id}`, payload).then((res) => res.data.data);

export const deleteTemplate = (id) =>
  axiosClient.delete(`/api/templates/${id}`).then((res) => res.data.data);
