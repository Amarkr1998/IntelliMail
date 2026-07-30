import axiosClient from './axiosClient';

export const runTask = (goal, context, conversationId, referenceContext) =>
  axiosClient
    .post('/api/agent/tasks', {
      goal,
      context: context || null,
      conversationId: conversationId || null,
      referenceContext: referenceContext || null,
    })
    .then((res) => res.data.data);

export const confirmPendingAction = (taskId) =>
  axiosClient.post(`/api/agent/tasks/${taskId}/confirm`).then((res) => res.data.data);

export const rejectPendingAction = (taskId) =>
  axiosClient.post(`/api/agent/tasks/${taskId}/reject`).then((res) => res.data.data);

export const listTasks = (page = 0, size = 20) =>
  axiosClient.get('/api/agent/tasks', { params: { page, size } }).then((res) => res.data.data);

export const getTask = (taskId) =>
  axiosClient.get(`/api/agent/tasks/${taskId}`).then((res) => res.data.data);

export const downloadPdf = (taskId) =>
  axiosClient.get(`/api/agent/tasks/${taskId}/export/pdf`, { responseType: 'blob' }).then((res) => res.data);
