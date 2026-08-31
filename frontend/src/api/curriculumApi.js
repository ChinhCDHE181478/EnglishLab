import axiosClient from './axiosClient';
import { normalizePage } from '../utils/pagination';

const unwrapData = (response) => response?.data?.data ?? response?.data;
const asList = (data) => (Array.isArray(data) ? data : data?.content || data?.items || []);

export const curriculumApi = {
  async getInstructorLedCourses(params = {}) {
    const response = await axiosClient.get('/api/content-manager/curriculum-programs', { params });
    return asList(unwrapData(response));
  },

  async getInstructorLedCoursesPage(params = {}) {
    const response = await axiosClient.get('/api/content-manager/curriculum-programs/page', { params });
    return normalizePage(unwrapData(response));
  },

  async getInstructorLedCourse(id) {
    const response = await axiosClient.get(`/api/content-manager/curriculum-programs/${id}`);
    return unwrapData(response);
  },

  async createInstructorLedCourse(payload) {
    const response = await axiosClient.post('/api/content-manager/curriculum-programs', payload);
    return unwrapData(response);
  },

  async updateInstructorLedCourse(id, payload) {
    const response = await axiosClient.put(`/api/content-manager/curriculum-programs/${id}`, payload);
    return unwrapData(response);
  },

  async archiveInstructorLedCourse(id) {
    const response = await axiosClient.delete(`/api/content-manager/curriculum-programs/${id}`);
    return unwrapData(response);
  },

  async cloneInstructorLedCourse(id) {
    const response = await axiosClient.post(`/api/content-manager/curriculum-programs/${id}/clone`);
    return unwrapData(response);
  },

  async publishInstructorLedCourse(id) {
    const response = await axiosClient.post(`/api/content-manager/curriculum-programs/${id}/publish`);
    return unwrapData(response);
  },

  async createCourseUnit(programId, payload) {
    const response = await axiosClient.post(`/api/content-manager/curriculum-programs/${programId}/units`, payload);
    return unwrapData(response);
  },

  async updateCourseUnit(unitId, payload) {
    const response = await axiosClient.put(`/api/content-manager/curriculum-units/${unitId}`, payload);
    return unwrapData(response);
  },

  async deleteCourseUnit(unitId) {
    const response = await axiosClient.delete(`/api/content-manager/curriculum-units/${unitId}`);
    return unwrapData(response);
  },

  async createCourseLesson(unitId, payload) {
    const response = await axiosClient.post(`/api/content-manager/curriculum-units/${unitId}/session-plans`, payload);
    return unwrapData(response);
  },

  async updateCourseLesson(lessonId, payload) {
    const response = await axiosClient.put(`/api/content-manager/curriculum-session-plans/${lessonId}`, payload);
    return unwrapData(response);
  },

  async deleteCourseLesson(lessonId) {
    const response = await axiosClient.delete(`/api/content-manager/curriculum-session-plans/${lessonId}`);
    return unwrapData(response);
  },

  async attachUnitMaterial(unitId, payload) {
    const response = await axiosClient.post(`/api/content-manager/curriculum-units/${unitId}/materials`, payload);
    return unwrapData(response);
  },

  async attachUnitExercise(unitId, payload) {
    const response = await axiosClient.post(`/api/content-manager/curriculum-units/${unitId}/exercises`, payload);
    return unwrapData(response);
  },

  async attachUnitAssessment(unitId, payload) {
    const response = await axiosClient.post(`/api/content-manager/curriculum-units/${unitId}/assessments`, payload);
    return unwrapData(response);
  },

  async attachUnitFlashcard(unitId, payload) {
    const response = await axiosClient.post(`/api/content-manager/curriculum-units/${unitId}/flashcards`, payload);
    return unwrapData(response);
  },

  async detachReference(type, referenceId) {
    const response = await axiosClient.delete(`/api/content-manager/curriculum-refs/${type}/${referenceId}`);
    return unwrapData(response);
  },

  async getAssessmentBank(params = {}) {
    const response = await axiosClient.get('/api/content-manager/assessment-bank', { params });
    return asList(unwrapData(response));
  },

  async getAssessmentBankPage(params = {}) {
    const response = await axiosClient.get('/api/content-manager/assessment-bank/page', { params });
    return normalizePage(unwrapData(response));
  },

  async getAssessmentBankStats(params = {}) {
    const response = await axiosClient.get('/api/content-manager/assessment-bank/stats', { params });
    return unwrapData(response) || {};
  },

  async createAssessmentBankItem(payload) {
    const response = await axiosClient.post('/api/content-manager/assessment-bank', payload);
    return unwrapData(response);
  },

  async updateAssessmentBankItem(id, payload) {
    const response = await axiosClient.put(`/api/content-manager/assessment-bank/${id}`, payload);
    return unwrapData(response);
  },

  async archiveAssessmentBankItem(id) {
    const response = await axiosClient.delete(`/api/content-manager/assessment-bank/${id}`);
    return unwrapData(response);
  },

  async getFlashcardSets(params = {}) {
    const response = await axiosClient.get('/api/content-manager/flashcard-sets', { params });
    return asList(unwrapData(response));
  },

  async getFlashcardSet(id) {
    const response = await axiosClient.get(`/api/content-manager/flashcard-sets/${id}`);
    return unwrapData(response);
  },

  async getFlashcardSetsPage(params = {}) {
    const response = await axiosClient.get('/api/content-manager/flashcard-sets/page', { params });
    return normalizePage(unwrapData(response));
  },

  async getFlashcardSetStats(params = {}) {
    const response = await axiosClient.get('/api/content-manager/flashcard-sets/stats', { params });
    return unwrapData(response) || {};
  },

  async createFlashcardSet(payload) {
    const response = await axiosClient.post('/api/content-manager/flashcard-sets', payload);
    return unwrapData(response);
  },

  async updateFlashcardSet(id, payload) {
    const response = await axiosClient.put(`/api/content-manager/flashcard-sets/${id}`, payload);
    return unwrapData(response);
  },

  async archiveFlashcardSet(id) {
    const response = await axiosClient.delete(`/api/content-manager/flashcard-sets/${id}`);
    return unwrapData(response);
  },
};

export default curriculumApi;
