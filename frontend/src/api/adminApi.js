import axiosClient from './axiosClient';

const unwrap = (response) => response?.data?.data ?? response?.data;

export const adminApi = {
  async getDashboard() { return unwrap(await axiosClient.get('/api/admin/dashboard')); },
  async getUsers(params = {}) { return unwrap(await axiosClient.get('/api/admin/users', { params })); },
  async createUser(payload) { return unwrap(await axiosClient.post('/api/admin/users', payload)); },
  async updateUser(id, payload) { return unwrap(await axiosClient.put(`/api/admin/users/${id}`, payload)); },
  async updateUserRoles(id, roles) { return unwrap(await axiosClient.patch(`/api/admin/users/${id}/roles`, { roles })); },
  async updateUserStatus(id, enabled) { return unwrap(await axiosClient.patch(`/api/admin/users/${id}/status`, { enabled })); },
};

export default adminApi;
