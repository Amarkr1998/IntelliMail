import axiosClient from './axiosClient';

export const getSubscription = () =>
  axiosClient.get('/api/billing/subscription').then((res) => res.data.data);

export const createCheckoutSession = (planId) =>
  axiosClient.post('/api/billing/checkout-session', { planId }).then((res) => res.data.data);

export const createPortalSession = () =>
  axiosClient.post('/api/billing/portal-session').then((res) => res.data.data);
