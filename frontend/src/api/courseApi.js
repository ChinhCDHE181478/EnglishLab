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

  if (Array.isArray(data?.items)) {
    return {
      content: data.items,
      totalElements: data.totalElements ?? data.total ?? data.items.length,
      totalPages: data.totalPages ?? 1,
      number: data.number ?? data.page ?? 0,
      size: data.size ?? data.items.length,
    };
  }

  if (Array.isArray(data?.courses)) {
    return {
      content: data.courses,
      totalElements: data.totalElements ?? data.total ?? data.courses.length,
      totalPages: data.totalPages ?? 1,
      number: data.number ?? data.page ?? 0,
      size: data.size ?? data.courses.length,
    };
  }

  return {
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size: 0,
  };
};

export const courseApi = {
  async getOnlineCourses(params = {}) {
    const response = await axiosClient.get('/api/online-courses', {
      params,
      skipAuthRedirect: true,
    });
    return normalizePage(unwrapData(response));
  },

  async getOnlineCourse(slugOrId) {
    const response = await axiosClient.get(`/api/online-courses/${slugOrId}`, {
      skipAuthRedirect: true,
    });
    return unwrapData(response);
  },

  async registerOnlineCourse(courseId) {
    const response = await axiosClient.post(`/api/student/online-courses/${courseId}/register`);
    return unwrapData(response);
  },

  async getMyOnlineCourses() {
    const response = await axiosClient.get('/api/student/online-courses/my-enrollments', {
      skipAuthRedirect: true,
    });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async updateLessonProgress(courseId, lessonId, completed = true) {
    const response = await axiosClient.patch(`/api/student/online-courses/${courseId}/lessons/${lessonId}/progress`, null, {
      params: { completed },
    });
    return unwrapData(response);
  },

  async getManagedOnlineCourses(params = {}) {
    const response = await axiosClient.get('/api/content-manager/online-courses', { params });
    return normalizePage(unwrapData(response));
  },

  async createOnlineCourse(payload) {
    const response = await axiosClient.post('/api/content-manager/online-courses', payload);
    return unwrapData(response);
  },

  async updateOnlineCourse(id, payload) {
    const response = await axiosClient.put(`/api/content-manager/online-courses/${id}`, payload);
    return unwrapData(response);
  },

  async publishOnlineCourse(id) {
    const response = await axiosClient.patch(`/api/content-manager/online-courses/${id}/publish`);
    return unwrapData(response);
  },

  async archiveOnlineCourse(id) {
    const response = await axiosClient.patch(`/api/content-manager/online-courses/${id}/archive`);
    return unwrapData(response);
  },

  async deleteOnlineCourse(id) {
    const response = await axiosClient.delete(`/api/content-manager/online-courses/${id}`);
    return unwrapData(response);
  },
};

export default courseApi;
