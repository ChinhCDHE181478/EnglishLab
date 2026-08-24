import axiosClient from './axiosClient';

const unwrapData = (response) => response?.data?.data ?? response?.data;

const paymentBody = (courseIds = [], couponCode = '', classroomOfferingIds = [], learningPathId = null) => ({
  courseIds,
  classroomOfferingIds,
  learningPathId,
  couponCode,
});

export const paymentApi = {
  async quotePayment(courseIds = [], couponCode = '', classroomOfferingIds = [], learningPathId = null) {
    const response = await axiosClient.post('/api/student/payments/quote', paymentBody(courseIds, couponCode, classroomOfferingIds, learningPathId));
    return unwrapData(response);
  },

  async createPayosLink(courseIds = [], couponCode = '', classroomOfferingIds = [], learningPathId = null) {
    const response = await axiosClient.post('/api/student/payments/payos/link', paymentBody(courseIds, couponCode, classroomOfferingIds, learningPathId));
    return unwrapData(response);
  },

  async getPaymentOrderStatus(orderCode) {
    const response = await axiosClient.get(`/api/student/payments/orders/${orderCode}`);
    return unwrapData(response);
  },

  async listMyOrders() {
    const response = await axiosClient.get('/api/student/payments/orders');
    return unwrapData(response) ?? [];
  },

  async pageMyOrders(params = {}) {
    const response = await axiosClient.get('/api/student/payments/orders/page', { params });
    return unwrapData(response);
  },

  async getRevenueAnalytics() {
    const response = await axiosClient.get('/api/content-manager/revenue/analytics');
    return unwrapData(response);
  },

  async confirmPayosWebhook() {
    const response = await axiosClient.post('/api/student/payments/payos/confirm-webhook');
    return unwrapData(response);
  },
};

export default paymentApi;
