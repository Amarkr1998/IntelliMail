const DEFAULT_API_BASE_URL = 'http://localhost:8080';

function sendMessage(message) {
  return new Promise((resolve) => {
    chrome.runtime.sendMessage(message, (response) => resolve(response));
  });
}

async function refreshView() {
  const state = await sendMessage({ type: 'GET_AUTH_STATE' });
  const loggedOutView = document.getElementById('logged-out-view');
  const loggedInView = document.getElementById('logged-in-view');

  if (state?.data?.authenticated) {
    loggedOutView.classList.add('popup-hidden');
    loggedInView.classList.remove('popup-hidden');
    document.getElementById('user-name').textContent =
      state.data.user?.fullName || state.data.user?.email || 'User';
  } else {
    loggedOutView.classList.remove('popup-hidden');
    loggedInView.classList.add('popup-hidden');
  }
}

document.getElementById('login-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const email = document.getElementById('email').value;
  const password = document.getElementById('password').value;
  const errorEl = document.getElementById('login-error');
  errorEl.textContent = '';

  const response = await sendMessage({ type: 'LOGIN', payload: { email, password } });
  if (response?.success) {
    await refreshView();
  } else {
    errorEl.textContent = response?.error || 'Login failed';
  }
});

document.getElementById('logout-button').addEventListener('click', async () => {
  await sendMessage({ type: 'LOGOUT' });
  await refreshView();
});

document.getElementById('save-settings').addEventListener('click', async () => {
  const baseUrl = document.getElementById('api-base-url').value.trim() || DEFAULT_API_BASE_URL;
  await sendMessage({ type: 'SET_API_BASE_URL', payload: { baseUrl } });
});

(async function init() {
  const stored = await chrome.storage.local.get('intellimail_api_base_url');
  document.getElementById('api-base-url').value = stored.intellimail_api_base_url || DEFAULT_API_BASE_URL;
  await refreshView();
})();
