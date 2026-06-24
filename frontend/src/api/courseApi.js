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

  async getOnlineCourseCategories() {
    const response = await axiosClient.get('/api/online-courses/categories', {
      skipAuthRedirect: true,
    });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async getEnrolledCourseContent(courseId) {
    const response = await axiosClient.get(`/api/student/online-courses/${courseId}/content`);
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

  async getCourseCompletion(courseId) {
    const response = await axiosClient.get(`/api/student/online-courses/${courseId}/completion`);
    return unwrapData(response);
  },

  async getCourseCertificate(courseId) {
    const response = await axiosClient.get(`/api/student/online-courses/${courseId}/certificate`);
    return unwrapData(response);
  },

  async getMyCourseRating(courseId) {
    const response = await axiosClient.get(`/api/student/online-courses/${courseId}/rating`);
    return unwrapData(response);
  },

  async saveCourseRating(courseId, payload) {
    const response = await axiosClient.post(`/api/student/online-courses/${courseId}/rating`, payload);
    return unwrapData(response);
  },

  async verifyCourseCertificate(verificationCode) {
    const response = await axiosClient.get(`/api/online-courses/certificates/${encodeURIComponent(verificationCode)}`, {
      skipAuthRedirect: true,
    });
    return unwrapData(response);
  },

  async updateLessonProgress(courseId, lessonId, completed = true) {
    const response = await axiosClient.patch(`/api/student/online-courses/${courseId}/lessons/${lessonId}/progress`, null, {
      params: { completed },
    });
    return unwrapData(response);
  },

  async getVocabularyTerms(courseId) {
    const response = await axiosClient.get(`/api/student/online-courses/${courseId}/vocabulary`);
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async getCourseAssessments(courseId) {
    const response = await axiosClient.get(`/api/student/courses/${courseId}/assessments`);
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async getCourseDiscussions(courseId, filter = 'ALL') {
    const response = await axiosClient.get(`/api/online-courses/${courseId}/discussions`, {
      params: { filter },
      skipAuthRedirect: true,
    });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async createCourseDiscussion(courseId, payload) {
    const response = await axiosClient.post(`/api/student/online-courses/${courseId}/discussions`, payload);
    return unwrapData(response);
  },

  async createDiscussionReply(threadId, payload) {
    const response = await axiosClient.post(`/api/student/online-courses/discussions/${threadId}/replies`, payload);
    return unwrapData(response);
  },

  async markDiscussionResolved(threadId, replyId = null) {
    const response = await axiosClient.patch(`/api/student/online-courses/discussions/${threadId}/resolved`, null, {
      params: replyId ? { replyId } : undefined,
    });
    return unwrapData(response);
  },

  async toggleDiscussionReplyHelpful(replyId) {
    const response = await axiosClient.post(`/api/student/online-courses/discussions/replies/${replyId}/helpful`);
    return unwrapData(response);
  },

  async toggleDiscussionThreadReaction(threadId, type) {
    const response = await axiosClient.post(`/api/student/online-courses/discussions/${threadId}/reactions`, { type });
    return unwrapData(response);
  },

  async toggleDiscussionReplyReaction(replyId, type) {
    const response = await axiosClient.post(`/api/student/online-courses/discussions/replies/${replyId}/reactions`, { type });
    return unwrapData(response);
  },

  async getDiscussionThreadReactions(threadId) {
    const response = await axiosClient.get(`/api/online-courses/discussions/${threadId}/reactions`, {
      skipAuthRedirect: true,
    });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async getDiscussionReplyReactions(replyId) {
    const response = await axiosClient.get(`/api/online-courses/discussions/replies/${replyId}/reactions`, {
      skipAuthRedirect: true,
    });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async reportDiscussionThread(threadId, payload = {}) {
    const response = await axiosClient.post(`/api/student/online-courses/discussions/${threadId}/reports`, payload);
    return unwrapData(response);
  },

  async reportDiscussionReply(replyId, payload = {}) {
    const response = await axiosClient.post(`/api/student/online-courses/discussions/replies/${replyId}/reports`, payload);
    return unwrapData(response);
  },

  async submitAssessment(assessmentId, payload) {
    const response = await axiosClient.post(`/api/student/assessments/${assessmentId}/submit`, payload);
    return unwrapData(response);
  },

  async uploadAssessmentAudio(file, onUploadProgress) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axiosClient.post('/api/student/assessments/audio', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress,
    });
    return unwrapData(response);
  },

  async updateVocabularyProgress(courseId, termKey, payload = {}) {
    const response = await axiosClient.patch(`/api/student/online-courses/${courseId}/vocabulary/${encodeURIComponent(termKey)}/progress`, null, {
      params: payload,
    });
    return unwrapData(response);
  },

  async getManagedOnlineCourses(params = {}) {
    const response = await axiosClient.get('/api/content-manager/online-courses', { params });
    return normalizePage(unwrapData(response));
  },

  async getManagedOnlineCourse(slugOrId) {
    const response = await axiosClient.get(`/api/content-manager/online-courses/${slugOrId}`);
    return unwrapData(response);
  },

  async getManagedCourseStats() {
    const response = await axiosClient.get('/api/content-manager/online-courses/stats');
    return unwrapData(response);
  },

  async getManagedCourseCategories() {
    const response = await axiosClient.get('/api/content-manager/course-categories');
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async createManagedCourseCategory(payload) {
    const response = await axiosClient.post('/api/content-manager/course-categories', payload);
    return unwrapData(response);
  },

  async updateManagedCourseCategory(id, payload) {
    const response = await axiosClient.put(`/api/content-manager/course-categories/${id}`, payload);
    return unwrapData(response);
  },

  async deleteManagedCourseCategory(id) {
    const response = await axiosClient.delete(`/api/content-manager/course-categories/${id}`);
    return unwrapData(response);
  },

  async getManagedCourseAssessments(courseId) {
    const response = await axiosClient.get(`/api/content-manager/online-courses/${courseId}/assessments`);
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async saveManagedCourseAssessments(courseId, payload) {
    const response = await axiosClient.put(`/api/content-manager/online-courses/${courseId}/assessments`, payload);
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async getManagedAssessmentRubrics() {
    const response = await axiosClient.get('/api/content-manager/online-courses/assessment-rubrics');
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
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

  async uploadLessonVideo(courseId, lessonId, file, title, onUploadProgress) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axiosClient.post(`/api/content-manager/online-courses/${courseId}/lessons/${lessonId}/bunny-video`, formData, {
      params: title ? { title } : undefined,
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress,
    });
    return unwrapData(response);
  },

  async refreshLessonTranscript(courseId, lessonId) {
    const response = await axiosClient.post(`/api/content-manager/online-courses/${courseId}/lessons/${lessonId}/transcript/youtube`);
    return unwrapData(response);
  },

  async getDiscountCodes(params = {}) {
    const response = await axiosClient.get('/api/content-manager/discount-codes', { params });
    return normalizePage(unwrapData(response));
  },

  async createDiscountCode(payload) {
    const response = await axiosClient.post('/api/content-manager/discount-codes', payload);
    return unwrapData(response);
  },

  async updateDiscountCode(id, payload) {
    const response = await axiosClient.put(`/api/content-manager/discount-codes/${id}`, payload);
    return unwrapData(response);
  },

  async deleteDiscountCode(id) {
    const response = await axiosClient.delete(`/api/content-manager/discount-codes/${id}`);
    return unwrapData(response);
  },
};

export default courseApi;
