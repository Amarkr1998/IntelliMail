import axiosClient from './axiosClient';

export const submitVoicePrompt = (transcript, language) =>
  axiosClient.post('/api/voice/prompt', { transcript, language: language || null }).then((res) => res.data.data);

export const getVoiceHistory = (page = 0, size = 5) =>
  axiosClient.get('/api/voice/history', { params: { page, size } }).then((res) => res.data.data);
