import axiosClient from './axiosClient';

const homeApi = {
  async getHomePage() {
    const response = await axiosClient.get('/api/home', { skipAuthRedirect: true });
    return response?.data || { teachers: [], testimonials: [] };
  },
};

export default homeApi;
