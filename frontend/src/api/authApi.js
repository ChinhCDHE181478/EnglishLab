import axiosClient from './axiosClient';

// Failed sign-in is an expected form error, not an expired application session.
export const login = async (data) => axiosClient.post('/api/auth/login', data, { skipAuthRedirect: true });

export const register = async (data) => axiosClient.post('/api/auth/register', data);

export const verifyEmail = async (data) => axiosClient.post('/api/auth/verify-email', data);

export const resendVerificationEmail = async (email) => axiosClient.post('/api/auth/resend-verification', { email });

export const forgotPassword = async (email) => axiosClient.post('/api/auth/forgot-password', { email });

export const resetPassword = async (data) => axiosClient.post('/api/auth/reset-password', data);

export const getCurrentUser = async () => axiosClient.get('/api/user/me');

export const updateCurrentUser = async (data) => axiosClient.put('/api/user/me', data);

export const uploadCurrentUserAvatar = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  // FormData: axiosClient drops default application/json so the browser sets multipart boundary.
  return axiosClient.post('/api/user/me/avatar', formData);
};

export const deleteCurrentUserAvatar = async () => axiosClient.delete('/api/user/me/avatar');

export const changeCurrentUserPassword = async (data) => axiosClient.put('/api/user/me/password', data);

export const getCurrentUserNotificationPreferences = async () => (
  axiosClient.get('/api/user/me/notification-preferences')
);

export const updateCurrentUserNotificationPreferences = async (data) => (
  axiosClient.put('/api/user/me/notification-preferences', data)
);

export const getHomeMessage = async () => axiosClient.get('/api/home');

export const loginWithGoogle = async (accessToken) => axiosClient.post('/api/auth/google', { accessToken }, { skipAuthRedirect: true });
