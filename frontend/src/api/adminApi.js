import axiosClient from './axiosClient';

const unwrap = (response) => response?.data?.data ?? response?.data;

export const adminApi = {
  async getDashboard() { return unwrap(await axiosClient.get('/api/admin/dashboard')); },
  async getUsers(params = {}) { return unwrap(await axiosClient.get('/api/admin/users', { params })); },
  async createUser(payload) { return unwrap(await axiosClient.post('/api/admin/users', payload)); },
  async updateUser(id, payload) { return unwrap(await axiosClient.put(`/api/admin/users/${id}`, payload)); },
  async updateUserRoles(id, roles) { return unwrap(await axiosClient.patch(`/api/admin/users/${id}/roles`, { roles })); },
  async updateUserStatus(id, enabled) { return unwrap(await axiosClient.patch(`/api/admin/users/${id}/status`, { enabled })); },
  async getSystemConfig() { return unwrap(await axiosClient.get('/api/admin/system/config')); },
  async getAuditLogs(params = {}) { return unwrap(await axiosClient.get('/api/admin/audit-logs', { params })); },
  async getBroadcasts(params = {}) { return unwrap(await axiosClient.get('/api/admin/broadcasts', { params })); },
  async createBroadcast(payload) { return unwrap(await axiosClient.post('/api/admin/broadcasts', payload)); },
  async updateBroadcast(id, payload) { return unwrap(await axiosClient.put(`/api/admin/broadcasts/${id}`, payload)); },
  async scheduleBroadcast(id, scheduledAt) { return unwrap(await axiosClient.post(`/api/admin/broadcasts/${id}/schedule`, { scheduledAt })); },
  async sendBroadcast(id) { return unwrap(await axiosClient.post(`/api/admin/broadcasts/${id}/send`)); },
  async cancelBroadcast(id) { return unwrap(await axiosClient.post(`/api/admin/broadcasts/${id}/cancel`)); },
  async getApiMonitoring() { return unwrap(await axiosClient.get('/api/admin/monitoring')); },
  async getBackupCapabilities() { return unwrap(await axiosClient.get('/api/admin/backups/capabilities')); },
  async getBackups(params = {}) { return unwrap(await axiosClient.get('/api/admin/backups', { params })); },
  async createBackup() { return unwrap(await axiosClient.post('/api/admin/backups')); },
  async downloadBackup(id) { return axiosClient.get(`/api/admin/backups/${id}/download`, { responseType: 'blob' }); },
  async restoreBackup(file, confirmation) {
    const form = new FormData();
    form.append('file', file);
    form.append('confirmation', confirmation);
    return unwrap(await axiosClient.post('/api/admin/backups/restore', form));
  },
  async deleteBackup(id) { return axiosClient.delete(`/api/admin/backups/${id}`); },
};

export default adminApi;
