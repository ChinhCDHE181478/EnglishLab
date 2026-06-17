import axiosClient from './axiosClient';

export const login = (data) => axiosClient.post('/api/auth/login', data);

export const register = (data) => axiosClient.post('/api/auth/register', data);

export const verifyEmail = (token) => axiosClient.post(`/api/auth/verify-email?token=${encodeURIComponent(token)}`);

export const resendVerificationEmail = (email) => axiosClient.post('/api/auth/resend-verification', { email });

export const forgotPassword = (email) => axiosClient.post('/api/auth/forgot-password', { email });

export const resetPassword = (data) => axiosClient.post('/api/auth/reset-password', data);

export const getCurrentUser = () => axiosClient.get('/api/user/me');

export const updateCurrentUser = (data) => axiosClient.put('/api/user/me', data);

export const getHomeMessage = () => axiosClient.get('/api/home');

export const loginWithGoogle = (accessToken) => axiosClient.post('/api/auth/google', { accessToken });

export const loginWithFacebook = (accessToken) => axiosClient.post('/api/auth/facebook', { accessToken });
