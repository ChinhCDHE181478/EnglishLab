import axiosClient from './axiosClient';

const unwrapData = (response) => response?.data?.data ?? response?.data;

export const paymentApi = {
  async quotePayment(courseIds = [], couponCode = '', classroomOfferingIds = []) {
    const response = await axiosClient.post('/api/student/payments/quote', {
      courseIds,
      classroomOfferingIds,
      couponCode,
    });
    return unwrapData(response);
  },

  async createPayosLink(courseIds = [], couponCode = '', classroomOfferingIds = []) {
    const response = await axiosClient.post('/api/student/payments/payos/link', {
      courseIds,
      classroomOfferingIds,
      couponCode,
    });
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

  async downloadReceipt(orderCode) {
    const response = await axiosClient.get(`/api/student/payments/orders/${orderCode}/receipt`, {
      responseType: 'blob',
    });
    return response.data;
  },

  async listStaffOrders(status = 'PAID') {
    const response = await axiosClient.get('/api/content-manager/payments/orders', {
      params: status ? { status } : undefined,
    });
    return unwrapData(response) ?? [];
  },

  async refundCourseOrder(orderCode, reason) {
    const response = await axiosClient.post(`/api/content-manager/payments/orders/${orderCode}/refund`, { reason });
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
