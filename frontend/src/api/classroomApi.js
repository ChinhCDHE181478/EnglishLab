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

  return {
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size: 0,
  };
};

const asList = (data) => (Array.isArray(data) ? data : data?.content || data?.items || []);

export const classroomApi = {
  async getClassroomOfferings(params = {}) {
    const response = await axiosClient.get('/api/classroom-offerings', {
      params,
      skipAuthRedirect: true,
    });
    return normalizePage(unwrapData(response));
  },

  async getClassroomOffering(slugOrId) {
    const response = await axiosClient.get(`/api/classroom-offerings/${slugOrId}`, {
      skipAuthRedirect: true,
    });
    return unwrapData(response);
  },

  async registerForClass(classroomId, payload = {}) {
    const response = await axiosClient.post(`/api/student/classrooms/${classroomId}/register`, payload);
    return unwrapData(response);
  },

  async getMyClassRegistration(classroomId) {
    const response = await axiosClient.get(`/api/student/classrooms/${classroomId}/registration/me`);
    return unwrapData(response);
  },

  async getMyClassRegistrations() {
    const response = await axiosClient.get('/api/student/classrooms/registrations/me');
    return asList(unwrapData(response));
  },

  async cancelClassRegistration(classroomId) {
    const response = await axiosClient.post(`/api/student/classrooms/${classroomId}/registration/cancel`);
    return unwrapData(response);
  },

  async submitTuitionProof(classroomId, { file, amount, paymentKind, note }) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('amount', String(amount));
    if (paymentKind) formData.append('paymentKind', paymentKind);
    if (note) formData.append('note', note);
    // FormData: axiosClient strips default application/json so browser sets multipart boundary.
    const response = await axiosClient.post(`/api/student/classrooms/${classroomId}/tuition-proofs`, formData);
    return unwrapData(response);
  },

  async getMyTuitionProofs(classroomId) {
    const response = await axiosClient.get(`/api/student/classrooms/${classroomId}/tuition-proofs`);
    return asList(unwrapData(response));
  },

  async getMyTuitionHistory(classroomId) {
    const response = await axiosClient.get(`/api/student/classrooms/${classroomId}/tuition-history`);
    return asList(unwrapData(response));
  },

  async getMyClassrooms() {
    const response = await axiosClient.get('/api/student/classrooms/my-classrooms', {
      skipAuthRedirect: true,
    });
    return asList(unwrapData(response));
  },

  async getMyClassroom(id) {
    const response = await axiosClient.get(`/api/student/classrooms/${id}`);
    return unwrapData(response);
  },

  async getMyClassroomSessions(id) {
    const response = await axiosClient.get(`/api/student/classrooms/${id}/sessions`);
    return asList(unwrapData(response));
  },

  async joinVirtualSession(classroomId, sessionId) {
    const response = await axiosClient.post(`/api/student/classrooms/${classroomId}/join`, null, {
      params: { sessionId },
    });
    return unwrapData(response);
  },

  async getMyClassroomHomework(id) {
    const response = await axiosClient.get(`/api/student/classrooms/${id}/homework`);
    return asList(unwrapData(response));
  },

  async getClassroomPractice(id) {
    const response = await axiosClient.get(`/api/student/classrooms/${id}/practice`);
    return asList(unwrapData(response));
  },

  async completeClassroomPractice(id, exerciseId, payload) {
    const response = await axiosClient.post(`/api/student/classrooms/${id}/practice/${exerciseId}/complete`, payload);
    return unwrapData(response);
  },

  async getMyHomework() {
    const response = await axiosClient.get('/api/student/classrooms/my-homework');
    return asList(unwrapData(response));
  },

  async submitHomework(homeworkId, payload) {
    const response = await axiosClient.post(`/api/student/classrooms/homework/${homeworkId}/submit`, payload);
    return unwrapData(response);
  },

  async uploadHomeworkSubmissionAttachment(file) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axiosClient.post('/api/student/classrooms/homework/attachments', formData);
    return unwrapData(response);
  },

  async getMyGradebook(classroomId) {
    const response = await axiosClient.get(`/api/student/classrooms/${classroomId}/gradebook/me`);
    return unwrapData(response);
  },

  async getMyClassroomMaterials(id) {
    const response = await axiosClient.get(`/api/student/classrooms/${id}/materials`);
    return asList(unwrapData(response));
  },

  async getMyClassroomAnnouncements(id) {
    const response = await axiosClient.get(`/api/student/classrooms/${id}/announcements`);
    return asList(unwrapData(response));
  },

  async getMyClassroomSyllabus(id) {
    const response = await axiosClient.get(`/api/student/classrooms/${id}/syllabus`);
    return asList(unwrapData(response));
  },

  async getMyAttendance(classroomId) {
    const response = await axiosClient.get(`/api/student/classrooms/${classroomId}/attendance/me`);
    return asList(unwrapData(response));
  },

  async getTeacherAssignedClassrooms() {
    const response = await axiosClient.get('/api/teacher/classrooms/assigned');
    return asList(unwrapData(response));
  },

  async getTeacherClassroom(id) {
    const response = await axiosClient.get(`/api/teacher/classrooms/${id}`);
    return unwrapData(response);
  },

  async getTeacherClassroomSessions(id) {
    const response = await axiosClient.get(`/api/teacher/classrooms/${id}/sessions`);
    return asList(unwrapData(response));
  },

  async createTeacherSession(classroomId, payload) {
    const response = await axiosClient.post(`/api/teacher/classrooms/${classroomId}/sessions`, payload);
    return unwrapData(response);
  },

  async updateTeacherSession(sessionId, payload) {
    const response = await axiosClient.put(`/api/teacher/classrooms/sessions/${sessionId}`, payload);
    return unwrapData(response);
  },

  async deleteTeacherSession(sessionId) {
    const response = await axiosClient.delete(`/api/teacher/classrooms/sessions/${sessionId}`);
    return unwrapData(response);
  },

  async openVirtualSession(sessionId) {
    const response = await axiosClient.post(`/api/teacher/classrooms/sessions/${sessionId}/open`);
    return unwrapData(response);
  },

  async closeVirtualSession(sessionId) {
    const response = await axiosClient.post(`/api/teacher/classrooms/sessions/${sessionId}/close`);
    return unwrapData(response);
  },

  async updateSessionLarkLink(sessionId, payload) {
    const response = await axiosClient.patch(`/api/teacher/classrooms/sessions/${sessionId}/lark-link`, payload);
    return unwrapData(response);
  },

  async getSessionAttendance(sessionId) {
    const response = await axiosClient.get(`/api/teacher/classrooms/sessions/${sessionId}/attendance`);
    return asList(unwrapData(response));
  },

  async saveAttendance(payload) {
    const response = await axiosClient.post('/api/teacher/classrooms/attendance', payload);
    return asList(unwrapData(response));
  },

  async getTeacherHomework(classroomId) {
    const response = await axiosClient.get(`/api/teacher/classrooms/${classroomId}/homework`);
    return asList(unwrapData(response));
  },

  async createHomework(classroomId, payload) {
    const response = await axiosClient.post(`/api/teacher/classrooms/${classroomId}/homework`, payload);
    return unwrapData(response);
  },

  async uploadHomeworkAttachment(file) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axiosClient.post('/api/teacher/classrooms/homework/attachments', formData);
    return unwrapData(response);
  },

  async getHomeworkRubrics(skill) {
    const response = await axiosClient.get('/api/teacher/classrooms/homework/rubrics', {
      params: skill ? { skill } : {},
    });
    return asList(unwrapData(response));
  },

  async getHomeworkAiAssessmentOptions() {
    const response = await axiosClient.get('/api/teacher/classrooms/homework/ai-assessment-options');
    return response.data;
  },

  async updateHomework(homeworkId, payload) {
    const response = await axiosClient.put(`/api/teacher/classrooms/homework/${homeworkId}`, payload);
    return unwrapData(response);
  },

  async deleteHomework(homeworkId) {
    const response = await axiosClient.delete(`/api/teacher/classrooms/homework/${homeworkId}`);
    return unwrapData(response);
  },

  async gradeHomework(homeworkId, studentId, payload) {
    const response = await axiosClient.post(`/api/teacher/classrooms/homework/${homeworkId}/students/${studentId}/grade`, payload);
    return unwrapData(response);
  },

  async getHomeworkSubmissions(homeworkId) {
    const response = await axiosClient.get(`/api/teacher/classrooms/homework/${homeworkId}/submissions`);
    return asList(unwrapData(response));
  },

  async getTeacherGradebook(classroomId) {
    const response = await axiosClient.get(`/api/teacher/classrooms/${classroomId}/gradebook`);
    return asList(unwrapData(response));
  },

  async updateGradebookEntry(classroomId, payload) {
    const response = await axiosClient.put(`/api/teacher/classrooms/${classroomId}/gradebook`, payload);
    return unwrapData(response);
  },

  async publishGradebook(classroomId) {
    const response = await axiosClient.post(`/api/teacher/classrooms/${classroomId}/gradebook/publish`);
    return asList(unwrapData(response));
  },

  async unpublishGradebook(classroomId) {
    const response = await axiosClient.post(`/api/teacher/classrooms/${classroomId}/gradebook/unpublish`);
    return asList(unwrapData(response));
  },

  async getTeacherMaterials(classroomId) {
    const response = await axiosClient.get(`/api/teacher/classrooms/${classroomId}/materials`);
    return asList(unwrapData(response));
  },

  async createTeacherMaterial(classroomId, payload) {
    const response = await axiosClient.post(`/api/teacher/classrooms/${classroomId}/materials`, payload);
    return unwrapData(response);
  },

  async deleteTeacherMaterial(materialId) {
    const response = await axiosClient.delete(`/api/teacher/classrooms/materials/${materialId}`);
    return unwrapData(response);
  },

  async getTeacherAnnouncements(classroomId) {
    const response = await axiosClient.get(`/api/teacher/classrooms/${classroomId}/announcements`);
    return asList(unwrapData(response));
  },

  async checkTeacherChangeConflict(payload) {
    const response = await axiosClient.post('/api/teacher/classrooms/requests/check-conflict', payload);
    return unwrapData(response);
  },

  async getAvailableRooms(sessionId, params = {}) {
    const response = await axiosClient.get(`/api/teacher/classrooms/sessions/${sessionId}/available-rooms`, { params });
    return asList(unwrapData(response));
  },

  async getAvailableTeachers(sessionId, params = {}) {
    const response = await axiosClient.get(`/api/teacher/classrooms/sessions/${sessionId}/available-teachers`, { params });
    return asList(unwrapData(response));
  },

  async createChangeRequest(payload) {
    const response = await axiosClient.post('/api/teacher/classrooms/requests', payload);
    return unwrapData(response);
  },

  async getMyChangeRequests() {
    const response = await axiosClient.get('/api/teacher/classrooms/requests/mine');
    return asList(unwrapData(response));
  },

  async getPendingChangeRequests() {
    const response = await axiosClient.get('/api/training-manager/requests/pending');
    return asList(unwrapData(response));
  },

  async checkChangeRequestConflict(requestId) {
    const response = await axiosClient.post(`/api/training-manager/requests/${requestId}/conflict-check`);
    return unwrapData(response);
  },

  async getTrainingManagerDashboard() {
    const response = await axiosClient.get('/api/training-manager/dashboard');
    return unwrapData(response);
  },

  async approveChangeRequest(requestId, payload) {
    const response = await axiosClient.post(`/api/training-manager/requests/${requestId}/approve`, payload);
    return unwrapData(response);
  },

  async rejectChangeRequest(requestId, payload) {
    const response = await axiosClient.post(`/api/training-manager/requests/${requestId}/reject`, payload);
    return unwrapData(response);
  },

  async getManagerClassrooms() {
    const response = await axiosClient.get('/api/training-manager/classrooms');
    return asList(unwrapData(response));
  },

  async getManagerClassroom(id) {
    const response = await axiosClient.get(`/api/training-manager/classrooms/${id}`);
    return unwrapData(response);
  },

  async getTrainingManagerTeachers() {
    const response = await axiosClient.get('/api/training-manager/classrooms/teachers');
    return asList(unwrapData(response));
  },

  async getTrainingManagerRooms() {
    const response = await axiosClient.get('/api/training-manager/classrooms/rooms');
    return asList(unwrapData(response));
  },

  async getTrainingManagerCurriculumPrograms(deliveryMode) {
    const response = await axiosClient.get('/api/training-manager/classrooms/curriculum-programs', {
      params: deliveryMode ? { deliveryMode } : undefined,
    });
    return asList(unwrapData(response));
  },

  async getTrainingManagerPrograms(deliveryMode) {
    const response = await axiosClient.get('/api/training-manager/classrooms/training-programs', {
      params: deliveryMode ? { deliveryMode } : undefined,
    });
    return asList(unwrapData(response));
  },

  async createManagerClassroom(payload) {
    const response = await axiosClient.post('/api/training-manager/classrooms', payload);
    return unwrapData(response);
  },

  async updateManagerClassroom(id, payload) {
    const response = await axiosClient.put(`/api/training-manager/classrooms/${id}`, payload);
    return unwrapData(response);
  },

  async publishManagerClassroom(id) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/${id}/publish`);
    return unwrapData(response);
  },

  async getTrainingManagerClassroomSessions(id) {
    const response = await axiosClient.get(`/api/training-manager/classrooms/${id}/sessions`);
    return asList(unwrapData(response));
  },

  async createTrainingManagerClassroomSession(id, payload) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/${id}/sessions`, payload);
    return unwrapData(response);
  },

  async updateTrainingManagerClassroomSession(sessionId, payload) {
    const response = await axiosClient.put(`/api/training-manager/classrooms/sessions/${sessionId}`, payload);
    return unwrapData(response);
  },

  async enrollStudent(classroomId, payload) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/${classroomId}/enroll`, payload);
    return unwrapData(response);
  },

  async removeStudent(classroomId, studentId) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/${classroomId}/students/${studentId}/remove`);
    return unwrapData(response);
  },

  async transferStudent(classroomId, payload) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/${classroomId}/transfer-student`, payload);
    return unwrapData(response);
  },

  async checkConflict(payload) {
    const response = await axiosClient.post('/api/training-manager/classrooms/conflict-check', payload);
    return unwrapData(response);
  },

  async confirmClassRegistration(enrollmentId) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/enrollments/${enrollmentId}/confirm`);
    return unwrapData(response);
  },

  async rejectClassRegistration(enrollmentId, payload = {}) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/enrollments/${enrollmentId}/reject`, payload);
    return unwrapData(response);
  },

  async recordTuitionPayment(enrollmentId, payload) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/enrollments/${enrollmentId}/tuition`, payload);
    return unwrapData(response);
  },

  async getTuitionHistory(enrollmentId) {
    const response = await axiosClient.get(`/api/training-manager/classrooms/enrollments/${enrollmentId}/tuition-history`);
    return asList(unwrapData(response));
  },

  async getPendingTuitionProofs() {
    const response = await axiosClient.get('/api/training-manager/classrooms/tuition-proofs/pending');
    return asList(unwrapData(response));
  },

  async getEnrollmentTuitionProofs(enrollmentId) {
    const response = await axiosClient.get(`/api/training-manager/classrooms/enrollments/${enrollmentId}/tuition-proofs`);
    return asList(unwrapData(response));
  },

  async confirmTuitionProof(proofId) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/tuition-proofs/${proofId}/confirm`);
    return unwrapData(response);
  },

  async rejectTuitionProof(proofId, payload = {}) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/tuition-proofs/${proofId}/reject`, payload);
    return unwrapData(response);
  },

  async getTrainingManagerPendingCurriculum() {
    const response = await axiosClient.get('/api/training-manager/classrooms/curriculum-programs/pending-review');
    return asList(unwrapData(response));
  },

  async approveCurriculumProgram(programId) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/curriculum-programs/${programId}/approve`);
    return unwrapData(response);
  },

  async rejectCurriculumProgram(programId, payload = {}) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/curriculum-programs/${programId}/reject`, payload);
    return unwrapData(response);
  },

  async assignStudentToClass(enrollmentId, payload = {}) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/enrollments/${enrollmentId}/assign`, payload);
    return unwrapData(response);
  },

  async transferClassEnrollment(enrollmentId, payload) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/enrollments/${enrollmentId}/transfer`, payload);
    return unwrapData(response);
  },

  async checkEnrollmentConflict(enrollmentId) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/enrollments/${enrollmentId}/conflict-check`);
    return unwrapData(response);
  },

  async getTrainingManagerClassrooms() {
    const response = await axiosClient.get('/api/training-manager/classrooms');
    return asList(unwrapData(response));
  },

  async getTrainingManagerRegistrations(params = {}) {
    const response = await axiosClient.get('/api/training-manager/classrooms/registrations', { params });
    return asList(unwrapData(response));
  },

  async getTrainingManagerEnrollment(enrollmentId) {
    const response = await axiosClient.get(`/api/training-manager/classrooms/enrollments/${enrollmentId}`);
    return unwrapData(response);
  },

  async getContentManagerMaterialLibrary() {
    const response = await axiosClient.get('/api/content-manager/material-library');
    return asList(unwrapData(response));
  },

  async createContentManagerMaterialLibraryItem(payload) {
    const response = await axiosClient.post('/api/content-manager/material-library', payload);
    return unwrapData(response);
  },

  async updateContentManagerMaterialLibraryItem(materialId, payload) {
    const response = await axiosClient.put(`/api/content-manager/material-library/${materialId}`, payload);
    return unwrapData(response);
  },

  async deleteContentManagerMaterialLibraryItem(materialId) {
    const response = await axiosClient.delete(`/api/content-manager/material-library/${materialId}`);
    return unwrapData(response);
  },

  async uploadContentManagerMaterialLibraryFile(file) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axiosClient.post('/api/content-manager/material-library/upload', formData);
    return unwrapData(response);
  },

  async getStudentNotifications() {
    const response = await axiosClient.get('/api/student/notifications');
    return asList(unwrapData(response));
  },

  async getUnreadNotificationCount() {
    const response = await axiosClient.get('/api/student/notifications/unread-count');
    return unwrapData(response);
  },

  async markNotificationRead(notificationId) {
    const response = await axiosClient.patch(`/api/student/notifications/${notificationId}/read`);
    return unwrapData(response);
  },

  async markAllNotificationsRead() {
    const response = await axiosClient.patch('/api/student/notifications/read-all');
    return unwrapData(response);
  },

  async listCampuses() {
    const response = await axiosClient.get('/api/training-manager/infrastructure/campuses');
    return asList(unwrapData(response));
  },

  async createCampus(payload) {
    const response = await axiosClient.post('/api/training-manager/infrastructure/campuses', payload);
    return unwrapData(response);
  },

  async updateCampus(id, payload) {
    const response = await axiosClient.put(`/api/training-manager/infrastructure/campuses/${id}`, payload);
    return unwrapData(response);
  },

  async listRooms(campusId) {
    const response = await axiosClient.get('/api/training-manager/infrastructure/rooms', {
      params: campusId ? { campusId } : undefined,
    });
    return asList(unwrapData(response));
  },

  async createRoom(payload) {
    const response = await axiosClient.post('/api/training-manager/infrastructure/rooms', payload);
    return unwrapData(response);
  },

  async updateRoom(id, payload) {
    const response = await axiosClient.put(`/api/training-manager/infrastructure/rooms/${id}`, payload);
    return unwrapData(response);
  },

  async listSessionTemplates() {
    const response = await axiosClient.get('/api/training-manager/infrastructure/session-templates');
    return asList(unwrapData(response));
  },

  async createSessionTemplate(payload) {
    const response = await axiosClient.post('/api/training-manager/infrastructure/session-templates', payload);
    return unwrapData(response);
  },

  async updateSessionTemplate(id, payload) {
    const response = await axiosClient.put(`/api/training-manager/infrastructure/session-templates/${id}`, payload);
    return unwrapData(response);
  },

  async generateSessionsFromTemplate(offeringId, payload) {
    const response = await axiosClient.post(`/api/training-manager/infrastructure/classrooms/${offeringId}/generate-sessions`, payload);
    return asList(unwrapData(response));
  },

  async closeClassroomOffering(id) {
    const response = await axiosClient.post(`/api/training-manager/classrooms/${id}/close`);
    return unwrapData(response);
  },

  async listTeacherQuizzes(offeringId) {
    const response = await axiosClient.get(`/api/teacher/classrooms/${offeringId}/quizzes`);
    return asList(unwrapData(response));
  },

  async createTeacherQuiz(offeringId, payload) {
    const response = await axiosClient.post(`/api/teacher/classrooms/${offeringId}/quizzes`, payload);
    return unwrapData(response);
  },

  async openTeacherQuiz(quizId) {
    const response = await axiosClient.patch(`/api/teacher/quizzes/${quizId}/open`);
    return unwrapData(response);
  },

  async closeTeacherQuiz(quizId) {
    const response = await axiosClient.patch(`/api/teacher/quizzes/${quizId}/close`);
    return unwrapData(response);
  },

  async deleteTeacherQuiz(quizId) {
    const response = await axiosClient.delete(`/api/teacher/quizzes/${quizId}`);
    return unwrapData(response);
  },

  async listStudentQuizzes() {
    const response = await axiosClient.get('/api/student/classrooms/quizzes');
    return asList(unwrapData(response));
  },

  async submitStudentQuiz(quizId, answersJson) {
    const response = await axiosClient.post(`/api/student/quizzes/${quizId}/submit`, { answersJson });
    return unwrapData(response);
  },

  async createAttendanceDispute(attendanceId, reason) {
    const response = await axiosClient.post(`/api/student/attendance/${attendanceId}/disputes`, { reason });
    return unwrapData(response);
  },

  async listMyAttendanceDisputes() {
    const response = await axiosClient.get('/api/student/attendance/disputes');
    return asList(unwrapData(response));
  },

  async listAttendanceDisputesForClass(offeringId) {
    const response = await axiosClient.get(`/api/teacher/classrooms/${offeringId}/attendance-disputes`);
    return asList(unwrapData(response));
  },

  async listPendingAttendanceDisputes() {
    const response = await axiosClient.get('/api/training-manager/attendance-disputes/pending');
    return asList(unwrapData(response));
  },

  async reviewAttendanceDispute(disputeId, payload) {
    const response = await axiosClient.post(`/api/training-manager/attendance-disputes/${disputeId}/review`, payload);
    return unwrapData(response);
  },

  async getContentManagerPrograms(deliveryMode) {
    const response = await axiosClient.get('/api/content-manager/training-programs', {
      params: deliveryMode ? { deliveryMode } : undefined,
    });
    return asList(unwrapData(response));
  },

  async getContentManagerProgram(id) {
    const response = await axiosClient.get(`/api/content-manager/training-programs/${id}`);
    return unwrapData(response);
  },

  async createContentManagerProgram(payload) {
    const response = await axiosClient.post('/api/content-manager/training-programs', payload);
    return unwrapData(response);
  },

  async updateContentManagerProgram(id, payload) {
    const response = await axiosClient.put(`/api/content-manager/training-programs/${id}`, payload);
    return unwrapData(response);
  },

  async cloneContentManagerProgram(id) {
    const response = await axiosClient.post(`/api/content-manager/training-programs/${id}/clone`);
    return unwrapData(response);
  },

  async archiveContentManagerProgram(id) {
    const response = await axiosClient.delete(`/api/content-manager/training-programs/${id}`);
    return unwrapData(response);
  },

  async getManagerPendingContentApprovals() {
    const response = await axiosClient.get('/api/manager/content-approvals/pending');
    return asList(unwrapData(response));
  },

  async approveManagerMaterial(materialId, reviewNote) {
    const response = await axiosClient.post(`/api/manager/content-approvals/materials/${materialId}/approve`, { reviewNote });
    return unwrapData(response);
  },

  async rejectManagerMaterial(materialId, reviewNote) {
    const response = await axiosClient.post(`/api/manager/content-approvals/materials/${materialId}/reject`, { reviewNote });
    return unwrapData(response);
  },

  async approveManagerSyllabus(itemId, reviewNote) {
    const response = await axiosClient.post(`/api/manager/content-approvals/syllabus/${itemId}/approve`, { reviewNote });
    return unwrapData(response);
  },

  async rejectManagerSyllabus(itemId, reviewNote) {
    const response = await axiosClient.post(`/api/manager/content-approvals/syllabus/${itemId}/reject`, { reviewNote });
    return unwrapData(response);
  },

  async updateOfferingRecording(offeringId, payload) {
    const response = await axiosClient.put(`/api/training-manager/recordings/classrooms/${offeringId}`, payload);
    return unwrapData(response);
  },

  async updateSessionRecording(sessionId, payload) {
    const response = await axiosClient.put(`/api/training-manager/recordings/sessions/${sessionId}`, payload);
    return unwrapData(response);
  },

  async getManagerRecordingSessions(offeringId) {
    const response = await axiosClient.get(`/api/training-manager/recordings/classrooms/${offeringId}/sessions`);
    return asList(unwrapData(response));
  },

  async syncLarkRecording(sessionId) {
    const response = await axiosClient.post(`/api/training-manager/recordings/sessions/${sessionId}/sync-lark`);
    return unwrapData(response);
  },
};

export default classroomApi;
