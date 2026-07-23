import axiosClient from './axiosClient';

const unwrapData = (response) => response?.data?.data ?? response?.data;

const normalizePage = (payload) => {
  const data = payload?.data ?? payload;
  if (Array.isArray(data)) {
    return {
      content: data,
      totalElements: data.length,
      totalPages: 1,
      number: 0,
      size: data.length,
    };
  }
  if (Array.isArray(data?.content)) {
    return data;
  }
  return {
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size: 0,
  };
};

export const packageApi = {
  async getPackageTypes() {
    const response = await axiosClient.get('/api/content-manager/packages/types');
    const data = unwrapData(response);
    return Array.isArray(data) ? data : [];
  },

  async getBundleCandidates() {
    const response = await axiosClient.get('/api/content-manager/packages/candidates');
    const data = unwrapData(response);
    return Array.isArray(data) ? data : [];
  },

  async listPackages(params = {}) {
    const response = await axiosClient.get('/api/content-manager/packages', { params });
    return normalizePage(unwrapData(response));
  },

  async getPackage(id) {
    const response = await axiosClient.get(`/api/content-manager/packages/${id}`);
    return unwrapData(response);
  },

  async createBundle(payload) {
    const response = await axiosClient.post('/api/content-manager/packages', payload);
    return unwrapData(response);
  },

  async updateBundle(id, payload) {
    const response = await axiosClient.put(`/api/content-manager/packages/${id}`, payload);
    return unwrapData(response);
  },

  async replaceBundleItems(id, childPackageIds = []) {
    const response = await axiosClient.put(`/api/content-manager/packages/${id}/bundle-items`, {
      childPackageIds,
    });
    return unwrapData(response);
  },

  async publishBundle(id) {
    const response = await axiosClient.patch(`/api/content-manager/packages/${id}/publish`);
    return unwrapData(response);
  },

  async archiveBundle(id) {
    const response = await axiosClient.patch(`/api/content-manager/packages/${id}/archive`);
    return unwrapData(response);
  },

  async deleteBundle(id) {
    const response = await axiosClient.delete(`/api/content-manager/packages/${id}`);
    return unwrapData(response);
  },
};

export default packageApi;
