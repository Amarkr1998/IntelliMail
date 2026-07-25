import axiosClient from './axiosClient';

export const generateReply = (payload) =>
  axiosClient.post('/api/email/generate', payload).then((res) => res.data.data);

export const improveEmail = (payload) =>
  axiosClient.post('/api/email/improve', payload).then((res) => res.data.data);

export const translateEmail = (payload) =>
  axiosClient.post('/api/email/translate', payload).then((res) => res.data.data);

export const summarizeEmail = (payload) =>
  axiosClient.post('/api/email/summarize', payload).then((res) => res.data.data);

export const generateSubject = (payload) =>
  axiosClient.post('/api/email/subject', payload).then((res) => res.data.data);

export const generateFollowup = (payload) =>
  axiosClient.post('/api/email/followup', payload).then((res) => res.data.data);

export const generateCustom = (payload) =>
  axiosClient.post('/api/email/custom', payload).then((res) => res.data.data);
