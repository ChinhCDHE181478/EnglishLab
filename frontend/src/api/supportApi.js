import axiosClient from './axiosClient';

const unwrapData = (response) => response?.data?.data ?? response?.data;
const supportQueueBase = (scope = 'staff') => `/api/${scope === 'manager' ? 'manager' : 'staff'}/support-tickets`;

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
  async listQueue(params = {}, scope = 'staff') {
    return unwrapData(await axiosClient.get(supportQueueBase(scope), { params })) ?? [];
  },
  async pageQueue(params = {}, scope = 'staff') {
    return unwrapData(await axiosClient.get(`${supportQueueBase(scope)}/page`, { params }));
  },
  async getForStaff(ticketId, scope = 'staff') {
    return unwrapData(await axiosClient.get(`${supportQueueBase(scope)}/${ticketId}`));
  },
  async claim(ticketId, scope = 'staff') {
    return unwrapData(await axiosClient.post(`${supportQueueBase(scope)}/${ticketId}/claim`));
  },
  async replyAsStaff(ticketId, message, scope = 'staff') {
    return unwrapData(await axiosClient.post(`${supportQueueBase(scope)}/${ticketId}/replies`, { message }));
  },
  async updateAsStaff(ticketId, data, scope = 'staff') {
    return unwrapData(await axiosClient.patch(`${supportQueueBase(scope)}/${ticketId}`, data));
  },
};

export default supportApi;
