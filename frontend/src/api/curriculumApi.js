import axiosClient from './axiosClient';

const unwrapData = (response) => response?.data?.data ?? response?.data;
const asList = (data) => (Array.isArray(data) ? data : data?.content || data?.items || []);

export const curriculumApi = {
  async getCurriculumPrograms(params = {}) {
    const response = await axiosClient.get('/api/content-manager/curriculum-programs', { params });
    return asList(unwrapData(response));
  },

  async getCurriculumProgram(id) {
    const response = await axiosClient.get(`/api/content-manager/curriculum-programs/${id}`);
    return unwrapData(response);
  },

  async createCurriculumProgram(payload) {
    const response = await axiosClient.post('/api/content-manager/curriculum-programs', payload);
    return unwrapData(response);
  },

  async updateCurriculumProgram(id, payload) {
    const response = await axiosClient.put(`/api/content-manager/curriculum-programs/${id}`, payload);
    return unwrapData(response);
  },

  async archiveCurriculumProgram(id) {
    const response = await axiosClient.delete(`/api/content-manager/curriculum-programs/${id}`);
    return unwrapData(response);
  },

  async cloneCurriculumProgram(id) {
    const response = await axiosClient.post(`/api/content-manager/curriculum-programs/${id}/clone`);
    return unwrapData(response);
  },

  async publishCurriculumProgram(id) {
    const response = await axiosClient.post(`/api/content-manager/curriculum-programs/${id}/publish`);
    return unwrapData(response);
  },

  async createCurriculumUnit(programId, payload) {
    const response = await axiosClient.post(`/api/content-manager/curriculum-programs/${programId}/units`, payload);
    return unwrapData(response);
  },

  async updateCurriculumUnit(unitId, payload) {
    const response = await axiosClient.put(`/api/content-manager/curriculum-units/${unitId}`, payload);
    return unwrapData(response);
  },

  async deleteCurriculumUnit(unitId) {
    const response = await axiosClient.delete(`/api/content-manager/curriculum-units/${unitId}`);
    return unwrapData(response);
  },

  async createCurriculumSessionPlan(unitId, payload) {
    const response = await axiosClient.post(`/api/content-manager/curriculum-units/${unitId}/session-plans`, payload);
    return unwrapData(response);
  },

  async updateCurriculumSessionPlan(sessionPlanId, payload) {
    const response = await axiosClient.put(`/api/content-manager/curriculum-session-plans/${sessionPlanId}`, payload);
    return unwrapData(response);
  },

  async deleteCurriculumSessionPlan(sessionPlanId) {
    const response = await axiosClient.delete(`/api/content-manager/curriculum-session-plans/${sessionPlanId}`);
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

  async getFlashcardSets() {
    const response = await axiosClient.get('/api/content-manager/flashcard-sets');
    return asList(unwrapData(response));
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
