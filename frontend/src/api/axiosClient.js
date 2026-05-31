import axios from 'axios';

const axiosClient = axios.create({
  baseURL: 'http://localhost:8080',
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
