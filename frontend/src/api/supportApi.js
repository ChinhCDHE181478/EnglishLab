import axiosClient from './axiosClient';

const unwrapData = (response) => response?.data?.data ?? response?.data;

const supportApi = {
  async createTicket(data) {
    return unwrapData(await axiosClient.post('/api/student/support-tickets', data));
  },
  async listMyTickets() {
    return unwrapData(await axiosClient.get('/api/student/support-tickets')) ?? [];
  },
  async getMyTicket(ticketId) {
    return unwrapData(await axiosClient.get(`/api/student/support-tickets/${ticketId}`));
  },
  async replyAsLearner(ticketId, message) {
    return unwrapData(await axiosClient.post(`/api/student/support-tickets/${ticketId}/replies`, { message }));
  },
  async updateMyTicketStatus(ticketId, status) {
    return unwrapData(await axiosClient.patch(`/api/student/support-tickets/${ticketId}/status`, { status }));
  },
  async listQueue(params = {}) {
    return unwrapData(await axiosClient.get('/api/training-manager/support-tickets', { params })) ?? [];
  },
  async getForStaff(ticketId) {
    return unwrapData(await axiosClient.get(`/api/training-manager/support-tickets/${ticketId}`));
  },
  async claim(ticketId) {
    return unwrapData(await axiosClient.post(`/api/training-manager/support-tickets/${ticketId}/claim`));
  },
  async replyAsStaff(ticketId, message) {
    return unwrapData(await axiosClient.post(`/api/training-manager/support-tickets/${ticketId}/replies`, { message }));
  },
  async updateAsStaff(ticketId, data) {
    return unwrapData(await axiosClient.patch(`/api/training-manager/support-tickets/${ticketId}`, data));
  },
};

export default supportApi;
