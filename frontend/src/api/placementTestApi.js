import axiosClient from './axiosClient';

const unwrapData = (response) => {
  const body = response?.data;
  return body?.data?.data ?? body?.data ?? body?.result ?? body;
};

const placementTestApi = {
  async getCurrent() {
    return unwrapData(await axiosClient.get('/api/student/placement-tests/current'));
  },

  async submitCurrent(payload) {
    return unwrapData(await axiosClient.post('/api/student/placement-tests/current/submit', payload));
  },

  async getManagedDefinition() {
    return unwrapData(await axiosClient.get('/api/content-manager/placement-test'));
  },

  async saveManagedDefinition(payload) {
    return unwrapData(await axiosClient.put('/api/content-manager/placement-test', payload));
  },

  async getMonitoring() {
    return unwrapData(await axiosClient.get('/api/content-manager/placement-test/monitoring'));
  },

  async uploadSpeakingAudio(file, onUploadProgress) {
    const formData = new FormData();
    formData.append('file', file);
    return unwrapData(await axiosClient.post('/api/student/assessments/audio', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress,
    }));
  },
};

export default placementTestApi;
