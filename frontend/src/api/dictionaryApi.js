import axiosClient from './axiosClient';

const unwrapData = (response) => response?.data?.data ?? response?.data;

const dictionaryApi = {
  async lookup(word, config = {}) {
    const response = await axiosClient.get('/api/dictionary/lookup', { ...config, params: { word } });
    return unwrapData(response);
  },

  async listSaved(params = {}) {
    const response = await axiosClient.get('/api/student/dictionary/saved', { params });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : [];
  },

  async pageSaved(params = {}) {
    const response = await axiosClient.get('/api/student/dictionary/saved/page', { params });
    return unwrapData(response);
  },

  async getSavedStats() {
    const response = await axiosClient.get('/api/student/dictionary/saved/stats');
    return unwrapData(response);
  },

  async isSaved(word) {
    const response = await axiosClient.get('/api/student/dictionary/saved/contains', { params: { word } });
    return Boolean(unwrapData(response)?.saved);
  },

  async save(payload) {
    const response = await axiosClient.post('/api/student/dictionary/saved', payload);
    return unwrapData(response);
  },

  async update(id, payload) {
    const response = await axiosClient.put(`/api/student/dictionary/saved/${id}`, payload);
    return unwrapData(response);
  },

  async remove(id) {
    await axiosClient.delete(`/api/student/dictionary/saved/${id}`);
  },
};

export default dictionaryApi;
