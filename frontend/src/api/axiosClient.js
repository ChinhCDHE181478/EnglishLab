import axios from 'axios';

const configuredTimeout = Number(import.meta.env.VITE_API_TIMEOUT_MS);
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? '' : 'http://localhost:8080');

const axiosClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: Number.isFinite(configuredTimeout) && configuredTimeout > 0 ? configuredTimeout : 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - attach access token to every authenticated request.
axiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Default Content-Type is application/json. For FormData uploads the browser must
    // set multipart/form-data with a boundary; otherwise the server returns 500.
    if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
      if (typeof config.headers?.delete === 'function') {
        config.headers.delete('Content-Type');
      } else if (config.headers) {
        delete config.headers['Content-Type'];
        delete config.headers['content-type'];
      }
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle 401 unauthorized globally.
// Public/optional requests can set skipAuthRedirect=true so guest pages do not get forced to login.
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const shouldSkipRedirect = error.config?.skipAuthRedirect;

    if (error.response && error.response.status === 401 && !shouldSkipRedirect) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }

    return Promise.reject(error);
  }
);

export default axiosClient;
