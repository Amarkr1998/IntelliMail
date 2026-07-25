// Manifest V3 service worker: the only place in the extension that talks to
// the backend or touches stored credentials. The content script and popup
// never call fetch() directly or hold a token in memory — they send a
// message here and get a plain { success, data | error } response back.
// This keeps host_permissions scoped to this one file and gives us a single
// place to implement token refresh, mirroring the frontend's axios
// interceptor from Module 10.

const DEFAULT_API_BASE_URL = 'http://localhost:8080';

const STORAGE_KEYS = {
  ACCESS_TOKEN: 'intellimail_access_token',
  REFRESH_TOKEN: 'intellimail_refresh_token',
  USER: 'intellimail_user',
  API_BASE_URL: 'intellimail_api_base_url',
};

const ACTION_ENDPOINTS = {
  GENERATE_REPLY: '/api/email/generate',
  IMPROVE: '/api/email/improve',
  TRANSLATE: '/api/email/translate',
  SUMMARIZE: '/api/email/summarize',
  SUBJECT: '/api/email/subject',
  FOLLOWUP: '/api/email/followup',
  CUSTOM: '/api/email/custom',
};

async function getApiBaseUrl() {
  const result = await chrome.storage.local.get(STORAGE_KEYS.API_BASE_URL);
  return result[STORAGE_KEYS.API_BASE_URL] || DEFAULT_API_BASE_URL;
}

async function getTokens() {
  const result = await chrome.storage.local.get([STORAGE_KEYS.ACCESS_TOKEN, STORAGE_KEYS.REFRESH_TOKEN]);
  return {
    accessToken: result[STORAGE_KEYS.ACCESS_TOKEN] || null,
    refreshToken: result[STORAGE_KEYS.REFRESH_TOKEN] || null,
  };
}

async function setTokens(accessToken, refreshToken) {
  // chrome.storage.local is sandboxed per-extension and inaccessible to web
  // pages (unlike localStorage, which the Gmail page itself could read) -
  // the right place to keep a JWT for a browser extension.
  await chrome.storage.local.set({
    [STORAGE_KEYS.ACCESS_TOKEN]: accessToken,
    [STORAGE_KEYS.REFRESH_TOKEN]: refreshToken,
  });
}

async function clearSession() {
  await chrome.storage.local.remove([STORAGE_KEYS.ACCESS_TOKEN, STORAGE_KEYS.REFRESH_TOKEN, STORAGE_KEYS.USER]);
}

async function refreshAccessToken() {
  const { refreshToken } = await getTokens();
  if (!refreshToken) {
    return false;
  }
  try {
    const baseUrl = await getApiBaseUrl();
    const response = await fetch(`${baseUrl}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!response.ok) {
      return false;
    }
    const body = await response.json();
    await setTokens(body.data.accessToken, body.data.refreshToken);
    return true;
  } catch {
    return false;
  }
}

async function apiFetch(path, options = {}, allowRetry = true) {
  const baseUrl = await getApiBaseUrl();
  const { accessToken } = await getTokens();

  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  const response = await fetch(`${baseUrl}${path}`, { ...options, headers });

  if (response.status === 401 && allowRetry && !path.startsWith('/api/auth/')) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return apiFetch(path, options, false);
    }
    await clearSession();
  }

  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(body?.message || `Request failed with status ${response.status}`);
  }
  return body?.data;
}

async function login(email, password) {
  const baseUrl = await getApiBaseUrl();
  const response = await fetch(`${baseUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(body?.message || 'Login failed');
  }
  await setTokens(body.data.accessToken, body.data.refreshToken);
  await chrome.storage.local.set({ [STORAGE_KEYS.USER]: body.data.user });
  return body.data.user;
}

async function getAuthState() {
  const { accessToken } = await getTokens();
  const stored = await chrome.storage.local.get(STORAGE_KEYS.USER);
  return { authenticated: Boolean(accessToken), user: stored[STORAGE_KEYS.USER] || null };
}

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  (async () => {
    try {
      switch (message.type) {
        case 'LOGIN': {
          const user = await login(message.payload.email, message.payload.password);
          sendResponse({ success: true, data: user });
          break;
        }
        case 'LOGOUT': {
          await clearSession();
          sendResponse({ success: true });
          break;
        }
        case 'GET_AUTH_STATE': {
          sendResponse({ success: true, data: await getAuthState() });
          break;
        }
        case 'SET_API_BASE_URL': {
          await chrome.storage.local.set({ [STORAGE_KEYS.API_BASE_URL]: message.payload.baseUrl });
          sendResponse({ success: true });
          break;
        }
        default: {
          const endpoint = ACTION_ENDPOINTS[message.type];
          if (!endpoint) {
            sendResponse({ success: false, error: `Unknown action: ${message.type}` });
            return;
          }
          const data = await apiFetch(endpoint, { method: 'POST', body: JSON.stringify(message.payload) });
          sendResponse({ success: true, data });
        }
      }
    } catch (error) {
      sendResponse({ success: false, error: error.message || 'Unexpected error' });
    }
  })();
  return true; // keep the message channel open for the async sendResponse above
});
