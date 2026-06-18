import axiosClient from './axiosClient';

const unwrapData = (response) => {
  const body = response?.data;
  return body?.data?.data ?? body?.data ?? body?.result ?? body;
};

const placementTestApi = {
  async getMockOne() {
    return unwrapData(await axiosClient.get('/api/student/placement-tests/mock-1'));
  },

  async submitMockOne(payload) {
    return unwrapData(await axiosClient.post('/api/student/placement-tests/mock-1/submit', payload));
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
