import axiosClient from './axiosClient';

const unwrapData = (response) => response?.data?.data ?? response?.data;

export const commerceApi = {
  async getCart() {
    const response = await axiosClient.get('/api/student/commerce/cart');
    return unwrapData(response) ?? [];
  },

  async addToCart(courseId) {
    const response = await axiosClient.post(`/api/student/commerce/cart/${courseId}`);
    return unwrapData(response);
  },

  async removeFromCart(courseId) {
    await axiosClient.delete(`/api/student/commerce/cart/${courseId}`);
  },

  async clearCart() {
    await axiosClient.delete('/api/student/commerce/cart');
  },

  async syncCart(courseIds) {
    const response = await axiosClient.post('/api/student/commerce/cart/sync', { courseIds });
    return unwrapData(response) ?? [];
  },

  async getWishlist() {
    const response = await axiosClient.get('/api/student/commerce/wishlist');
    return unwrapData(response) ?? [];
  },

  async addToWishlist(courseId) {
    const response = await axiosClient.post(`/api/student/commerce/wishlist/${courseId}`);
    return unwrapData(response);
  },

  async removeFromWishlist(courseId) {
    await axiosClient.delete(`/api/student/commerce/wishlist/${courseId}`);
  },

  async moveWishlistToCart(courseId) {
    const response = await axiosClient.post(`/api/student/commerce/wishlist/${courseId}/move-to-cart`);
    return unwrapData(response);
  },
};

export default commerceApi;
