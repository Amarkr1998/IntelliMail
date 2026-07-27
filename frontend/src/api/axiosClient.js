import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const ACCESS_TOKEN_KEY = 'intellimail_access_token';
export const REFRESH_TOKEN_KEY = 'intellimail_refresh_token';

const axiosClient = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
});

axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let pendingRequests = [];

function subscribeTokenRefresh(callback) {
  pendingRequests.push(callback);
}

function onRefreshed(newAccessToken) {
  pendingRequests.forEach((callback) => callback(newAccessToken));
  pendingRequests = [];
}

function clearSessionAndRedirect() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
}

// On a 401 from any authenticated request, transparently refresh the access
// token once and replay the original request — so a short-lived (15 min)
// access token never surfaces as a logged-out state during normal use.
axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error;

    const isAuthEndpoint = config?.url?.startsWith('/api/auth/');
    if (response?.status !== 401 || config._retry || isAuthEndpoint) {
      return Promise.reject(error);
    }

    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (!refreshToken) {
      clearSessionAndRedirect();
      return Promise.reject(error);
    }

    config._retry = true;

    if (!isRefreshing) {
      isRefreshing = true;
      try {
        const { data } = await axios.post(`${baseURL}/api/auth/refresh`, { refreshToken });
        const { accessToken, refreshToken: newRefreshToken } = data.data;
        localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
        localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken);
        onRefreshed(accessToken);
      } catch (refreshError) {
        pendingRequests = [];
        clearSessionAndRedirect();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return new Promise((resolve, reject) => {
      subscribeTokenRefresh((newAccessToken) => {
        config.headers.Authorization = `Bearer ${newAccessToken}`;
        axiosClient(config).then(resolve).catch(reject);
      });
    });
  },
);

export default axiosClient;
