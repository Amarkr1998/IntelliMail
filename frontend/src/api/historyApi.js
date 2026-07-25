import axiosClient from './axiosClient';

export const getHistory = (page = 0, size = 20) =>
  axiosClient.get('/api/history', { params: { page, size } }).then((res) => res.data.data);

export const deleteHistoryEntry = (id) =>
  axiosClient.delete(`/api/history/${id}`).then((res) => res.data.data);

export const regenerateReply = (emailRequestId) =>
  axiosClient.post(`/api/history/${emailRequestId}/regenerate`).then((res) => res.data.data);

export const setFavorite = (replyId, favorite) =>
  axiosClient
    .patch(`/api/history/replies/${replyId}/favorite`, null, { params: { favorite } })
    .then((res) => res.data.data);
