import axiosClient from './axiosClient';

export const createOrganization = (name, slug) =>
  axiosClient.post('/api/organizations', { name, slug }).then((res) => res.data.data);

export const checkSlugAvailability = (slug) =>
  axiosClient.get('/api/organizations/slug-available', { params: { slug } }).then((res) => res.data.data);

export const getMyOrganization = () =>
  axiosClient.get('/api/organizations/me').then((res) => res.data.data);

export const getMembers = (page = 0, size = 20) =>
  axiosClient.get('/api/organizations/members', { params: { page, size } }).then((res) => res.data.data);

export const removeMember = (userId) =>
  axiosClient.delete(`/api/organizations/members/${userId}`).then((res) => res.data.data);

export const inviteMember = (email, orgRole) =>
  axiosClient.post('/api/organizations/invitations', { email, orgRole }).then((res) => res.data.data);

export const acceptInvitation = (token) =>
  axiosClient.post('/api/organizations/invitations/accept', { token }).then((res) => res.data.data);
