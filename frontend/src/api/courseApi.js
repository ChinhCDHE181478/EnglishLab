import axiosClient from './axiosClient';
import { normalizePage } from '../utils/pagination';

const unwrapData = (response) => response?.data?.data ?? response?.data;

export const courseApi = {
  async uploadOnlineCourseThumbnail(file) {
    const formData = new FormData();
    formData.append('file', file);
    return unwrapData(await axiosClient.post('/api/content-manager/online-courses/thumbnail', formData));
  },

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

  async getCourseDiscussions(courseId, { filter = 'ALL', moduleId, page = 0, size = 10 } = {}) {
    const response = await axiosClient.get(`/api/online-courses/${courseId}/discussions`, {
      params: { filter, moduleId: moduleId || undefined, page, size },
      skipAuthRedirect: true,
    });
    return normalizePage(unwrapData(response));
  },

  async createCourseDiscussion(courseId, payload) {
    const response = await axiosClient.post(`/api/student/online-courses/${courseId}/discussions`, payload);
    return unwrapData(response);
  },

  async getGlobalFlashcardPractice(params = {}) {
    const response = await axiosClient.get('/api/student/flashcards/practice', { params });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async getRecommendedCourses() {
    const response = await axiosClient.get('/api/student/online-courses/recommendations');
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async getMyLearningPath() {
    const response = await axiosClient.get('/api/student/learning-path');
    return unwrapData(response);
  },

  async getLearnerLessonNotes() {
    const response = await axiosClient.get('/api/student/learning/notes');
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async createLearnerLessonNote(courseId, lessonId, payload) {
    const response = await axiosClient.post(`/api/student/learning/courses/${courseId}/lessons/${lessonId}/notes`, payload);
    return unwrapData(response);
  },

  async updateLearnerLessonNote(noteId, payload) {
    const response = await axiosClient.put(`/api/student/learning/notes/${noteId}`, payload);
    return unwrapData(response);
  },

  async deleteLearnerLessonNote(noteId) {
    await axiosClient.delete(`/api/student/learning/notes/${noteId}`);
  },

  async getLessonDiscussions(courseId, lessonId, { filter = 'ALL', page = 0, size = 10 } = {}) {
    const response = await axiosClient.get(`/api/online-courses/${courseId}/lessons/${lessonId}/discussions`, {
      params: { filter, page, size },
      skipAuthRedirect: true,
    });
    return normalizePage(unwrapData(response));
  },

  async createLessonDiscussion(courseId, lessonId, payload) {
    const response = await axiosClient.post(`/api/student/online-courses/${courseId}/lessons/${lessonId}/discussions`, payload);
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

  async reportDiscussionThread(threadId, { reasonCategory, reason } = {}) {
    const response = await axiosClient.post(`/api/student/online-courses/discussions/${threadId}/reports`, { reasonCategory, reason });
    return unwrapData(response);
  },

  async reportDiscussionReply(replyId, { reasonCategory, reason } = {}) {
    const response = await axiosClient.post(`/api/student/online-courses/discussions/replies/${replyId}/reports`, { reasonCategory, reason });
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

  async getManagedLearningPaths(params = {}) {
    const response = await axiosClient.get('/api/content-manager/learning-paths', { params });
    return normalizePage(unwrapData(response));
  },

  async getLearningPathOffers() {
    const response = await axiosClient.get('/api/learning-paths');
    return unwrapData(response) ?? [];
  },

  async getLearningPathOffer(code) {
    const response = await axiosClient.get(`/api/learning-paths/${encodeURIComponent(code)}`);
    return unwrapData(response);
  },

  async createManagedLearningPath(payload) {
    const response = await axiosClient.post('/api/content-manager/learning-paths', payload);
    return unwrapData(response);
  },

  async updateManagedLearningPath(pathId, payload) {
    const response = await axiosClient.put(`/api/content-manager/learning-paths/${pathId}`, payload);
    return unwrapData(response);
  },

  async addManagedLearningPathCourses(pathId, courseIds) {
    const response = await axiosClient.post(`/api/content-manager/learning-paths/${pathId}/courses`, { courseIds });
    return unwrapData(response);
  },

  async reorderManagedLearningPathCourses(pathId, courseIds) {
    const response = await axiosClient.put(`/api/content-manager/learning-paths/${pathId}/courses/order`, { courseIds });
    return unwrapData(response);
  },

  async deleteManagedLearningPath(pathId) {
    await axiosClient.delete(`/api/content-manager/learning-paths/${pathId}`);
  },

  async getDiscussionModerationReports(status = 'PENDING', category = '') {
    const params = { status };
    if (category) params.category = category;
    const response = await axiosClient.get('/api/content-manager/discussion-reports', { params });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async hideReportedDiscussion(reportId, payload = {}) {
    const response = await axiosClient.post(`/api/content-manager/discussion-reports/${reportId}/hide`, payload);
    return unwrapData(response);
  },

  async dismissDiscussionReport(reportId, payload = {}) {
    const response = await axiosClient.post(`/api/content-manager/discussion-reports/${reportId}/dismiss`, payload);
    return unwrapData(response);
  },

  async getManagedOnlineCourse(slugOrId) {
    const response = await axiosClient.get(`/api/content-manager/online-courses/${slugOrId}`);
    return unwrapData(response);
  },

  async getManagedOnlineCoursePreview(slugOrId) {
    const response = await axiosClient.get(`/api/content-manager/online-courses/${slugOrId}/preview`);
    return unwrapData(response);
  },

  async getOnlineCourseVersions(courseId) {
    const response = await axiosClient.get(`/api/content-manager/online-courses/${courseId}/versions`);
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.items || [];
  },

  async getManagedOnlineCourseVersionPreview(courseId, versionId) {
    const response = await axiosClient.get(`/api/content-manager/online-courses/${courseId}/versions/${versionId}/preview`);
    return unwrapData(response);
  },

  async createOnlineCourseVersion(courseId, changeNote = '') {
    const response = await axiosClient.post(`/api/content-manager/online-courses/${courseId}/versions`, { changeNote });
    return unwrapData(response);
  },

  async updateOnlineCourseVersion(courseId, versionId, payload) {
    const response = await axiosClient.put(`/api/content-manager/online-courses/${courseId}/versions/${versionId}`, payload);
    return unwrapData(response);
  },

  async publishOnlineCourseVersion(courseId, versionId) {
    const response = await axiosClient.patch(`/api/content-manager/online-courses/${courseId}/versions/${versionId}/publish`);
    return unwrapData(response);
  },

  async reorderOnlineCourseModules(courseId, items) {
    const response = await axiosClient.patch(`/api/content-manager/online-courses/${courseId}/modules/reorder`, { items });
    return unwrapData(response);
  },

  async reorderOnlineCourseLessons(courseId, moduleId, items) {
    const response = await axiosClient.patch(`/api/content-manager/online-courses/${courseId}/modules/${moduleId}/lessons/reorder`, { items });
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

  async getContentManagerRubrics(params = {}) {
    const response = await axiosClient.get('/api/content-manager/rubrics', { params });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || data?.items || [];
  },

  async getLearningPathOffersPage(params = {}) {
    const response = await axiosClient.get('/api/learning-paths/page', { params, skipAuthRedirect: true });
    return normalizePage(unwrapData(response));
  },

  async getManagedCourseCategoriesPage(params = {}) {
    const response = await axiosClient.get('/api/content-manager/course-categories/page', { params });
    return normalizePage(unwrapData(response));
  },

  async getDiscussionModerationReportsPage(params = {}) {
    const response = await axiosClient.get('/api/content-manager/discussion-reports/page', { params });
    return normalizePage(unwrapData(response));
  },

  async getContentManagerRubricsPage(params = {}) {
    const response = await axiosClient.get('/api/content-manager/rubrics/page', { params });
    return normalizePage(unwrapData(response));
  },

  async getContentManagerRubricStats(params = {}) {
    const response = await axiosClient.get('/api/content-manager/rubrics/stats', { params });
    return unwrapData(response) || {};
  },

  async createContentManagerRubric(payload) {
    const response = await axiosClient.post('/api/content-manager/rubrics', payload);
    return unwrapData(response);
  },

  async updateContentManagerRubric(id, payload) {
    const response = await axiosClient.put(`/api/content-manager/rubrics/${id}`, payload);
    return unwrapData(response);
  },

  async deactivateContentManagerRubric(id) {
    const response = await axiosClient.delete(`/api/content-manager/rubrics/${id}`);
    return unwrapData(response);
  },

  async reactivateContentManagerRubric(id) {
    const response = await axiosClient.patch(`/api/content-manager/rubrics/${id}/reactivate`);
    return unwrapData(response);
  },

  async createOnlineCourse(payload) {
    const response = await axiosClient.post('/api/content-manager/online-courses', payload);
    return unwrapData(response);
  },

  async updateOnlineCourse(id, payload) {
    const response = await axiosClient.put(`/api/content-manager/online-courses/${id}`, payload);
    return unwrapData(response);
  },

  async publishOnlineCourseDraft(id) {
    const versionsResponse = await axiosClient.get(`/api/content-manager/online-courses/${id}/versions`);
    const versions = unwrapData(versionsResponse) || [];
    const draft = versions.find((version) => version.status === 'DRAFT')
      || versions.find((version) => version.status === 'PENDING_REVIEW');
    if (!draft) {
      throw new Error('Khóa học chưa có phiên bản nháp để xuất bản.');
    }
    await axiosClient.patch(`/api/content-manager/online-courses/${id}/versions/${draft.id}/publish`);
    const courseResponse = await axiosClient.get(`/api/content-manager/online-courses/${id}`);
    return unwrapData(courseResponse);
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

  async getExerciseBankItems(params = {}) {
    const response = await axiosClient.get('/api/content-manager/exercise-bank', { params });
    return unwrapData(response);
  },

  async getExerciseBankItemsPage(params = {}) {
    const response = await axiosClient.get('/api/content-manager/exercise-bank/page', { params });
    return normalizePage(unwrapData(response));
  },

  async getExerciseBankStats(params = {}) {
    const response = await axiosClient.get('/api/content-manager/exercise-bank/stats', { params });
    return unwrapData(response) || {};
  },

  async createExerciseBankItem(payload) {
    const response = await axiosClient.post('/api/content-manager/exercise-bank', payload);
    return unwrapData(response);
  },

  async updateExerciseBankItem(id, payload) {
    const response = await axiosClient.put(`/api/content-manager/exercise-bank/${id}`, payload);
    return unwrapData(response);
  },

  async deleteExerciseBankItem(id) {
    const response = await axiosClient.delete(`/api/content-manager/exercise-bank/${id}`);
    return unwrapData(response);
  },

  async getManagerOnlineEnrollments(params = {}) {
    const response = await axiosClient.get('/api/manager/enrollments', { params });
    return unwrapData(response);
  },

  async getManagerOnlineEnrollmentsPage(params = {}) {
    const response = await axiosClient.get('/api/manager/enrollments/page', { params });
    return normalizePage(unwrapData(response));
  },

  async updateManagerOnlineEnrollment(enrollmentId, payload) {
    const response = await axiosClient.put(`/api/manager/enrollments/${enrollmentId}`, payload);
    return unwrapData(response);
  },
};

export default courseApi;
