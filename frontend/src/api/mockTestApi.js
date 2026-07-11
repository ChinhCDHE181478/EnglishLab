import axiosClient from './axiosClient';

const unwrapData = (response) => response?.data?.data ?? response?.data;
const asList = (data) => (Array.isArray(data) ? data : data?.content || data?.items || []);

const mockTestApi = {
  async listMockTests() {
    const response = await axiosClient.get('/api/student/mock-tests');
    return asList(unwrapData(response));
  },

  async getMockTest(id) {
    const response = await axiosClient.get(`/api/student/mock-tests/${id}`);
    return unwrapData(response);
  },

  async submitMockTest(id, payload) {
    const response = await axiosClient.post(`/api/student/mock-tests/${id}/submit`, payload);
    return unwrapData(response);
  },
};

export default mockTestApi;
