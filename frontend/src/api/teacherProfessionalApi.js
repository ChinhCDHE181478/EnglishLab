import axiosClient from './axiosClient';

const unwrapData = (response) => response?.data?.data ?? response?.data;
const asList = (value) => (Array.isArray(value) ? value : []);

const teacherProfessionalApi = {
  async listForStaff() {
    return asList(unwrapData(await axiosClient.get('/api/staff/teachers')));
  },

  async getForStaff(teacherId) {
    return unwrapData(await axiosClient.get(`/api/staff/teachers/${teacherId}`));
  },

  async updateProfile(teacherId, payload) {
    return unwrapData(await axiosClient.put(`/api/staff/teachers/${teacherId}/profile`, payload));
  },

  async createCredential(teacherId, payload) {
    return unwrapData(await axiosClient.post(`/api/staff/teachers/${teacherId}/credentials`, payload));
  },

  async updateCredential(teacherId, credentialId, payload) {
    return unwrapData(await axiosClient.put(`/api/staff/teachers/${teacherId}/credentials/${credentialId}`, payload));
  },

  async verifyCredential(teacherId, credentialId, payload) {
    return unwrapData(await axiosClient.patch(`/api/staff/teachers/${teacherId}/credentials/${credentialId}/verify`, payload));
  },

  async deleteCredential(teacherId, credentialId) {
    await axiosClient.delete(`/api/staff/teachers/${teacherId}/credentials/${credentialId}`);
  },

  async listForManager() {
    return asList(unwrapData(await axiosClient.get('/api/manager/teacher-performance')));
  },

  async getForManager(teacherId) {
    return unwrapData(await axiosClient.get(`/api/manager/teacher-performance/${teacherId}`));
  },

  async getMine() {
    return unwrapData(await axiosClient.get('/api/teacher/professional-profile'));
  },

  async getMyFeedbackSummary() {
    return unwrapData(await axiosClient.get('/api/teacher/professional-profile/feedback-summary'));
  },

  async getGoogleMeetConnection() {
    return unwrapData(await axiosClient.get('/api/teacher/google-meet/connection'));
  },

  async connectGoogleMeet() {
    return unwrapData(await axiosClient.post('/api/teacher/google-meet/connect'));
  },

  async disconnectGoogleMeet() {
    await axiosClient.delete('/api/teacher/google-meet/connection');
  },
};

export default teacherProfessionalApi;
