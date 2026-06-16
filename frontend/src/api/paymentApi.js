import axiosClient from './axiosClient';

const unwrapData = (response) => response?.data?.data ?? response?.data;

export const paymentApi = {
  async createPayosLink(courseIds) {
    const response = await axiosClient.post('/api/student/payments/payos/link', { courseIds });
    return unwrapData(response);
  },

  async getPaymentOrderStatus(orderCode) {
    const response = await axiosClient.get(`/api/student/payments/orders/${orderCode}`);
    return unwrapData(response);
  },

  async confirmPayosWebhook() {
    const response = await axiosClient.post('/api/student/payments/payos/confirm-webhook');
    return unwrapData(response);
  },
};

export default paymentApi;
