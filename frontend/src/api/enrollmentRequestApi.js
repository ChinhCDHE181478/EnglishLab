import axiosClient from './axiosClient';

const unwrapData = (response) => response?.data?.data ?? response?.data;

const enrollmentRequestApi = {
  async getCourseOfferings(deliveryType) {
    const response = await axiosClient.get('/api/course-offerings', {
      params: deliveryType && deliveryType !== 'ALL' ? { deliveryType } : undefined,
      skipAuthRedirect: true,
    });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || [];
  },

  async getCourseOffering(slugOrId) {
    const response = await axiosClient.get(`/api/course-offerings/${slugOrId}`, {
      skipAuthRedirect: true,
    });
    return unwrapData(response);
  },

  async submit(payload) {
    const response = await axiosClient.post('/api/student/course-enrollment-requests', payload);
    return unwrapData(response);
  },

  async listMine() {
    const response = await axiosClient.get('/api/student/course-enrollment-requests/my');
    const data = unwrapData(response);
    return Array.isArray(data) ? data : [];
  },

  async refreshPlacement(requestId) {
    const response = await axiosClient.patch(`/api/student/course-enrollment-requests/${requestId}/refresh-placement`);
    return unwrapData(response);
  },

  async cancel(requestId) {
    const response = await axiosClient.patch(`/api/student/course-enrollment-requests/${requestId}/cancel`);
    return unwrapData(response);
  },

  async listForStaff(status) {
    const response = await axiosClient.get('/api/staff/enrollment-requests', {
      params: status && status !== 'ALL' ? { status } : undefined,
    });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : [];
  },

  async confirmPlacementLevel(requestId, payload) {
    const response = await axiosClient.patch(`/api/staff/enrollment-requests/${requestId}/placement-level`, payload);
    return unwrapData(response);
  },

  async completeConsultation(requestId, note = '') {
    const response = await axiosClient.patch(
      `/api/staff/enrollment-requests/${requestId}/consultation-complete`,
      { note },
    );
    return unwrapData(response);
  },

  async assignClass(requestId, payload) {
    const response = await axiosClient.patch(
      `/api/staff/enrollment-requests/${requestId}/assign-class`,
      payload,
    );
    return unwrapData(response);
  },

  async reject(requestId, reason) {
    const response = await axiosClient.patch(`/api/staff/enrollment-requests/${requestId}/reject`, { reason });
    return unwrapData(response);
  },

  async listPlacementReviews() {
    const response = await axiosClient.get('/api/staff/placement-reviews');
    const data = unwrapData(response);
    return Array.isArray(data) ? data : [];
  },

  async confirmPlacementReview(attemptId, payload) {
    const response = await axiosClient.patch(`/api/staff/placement-reviews/${attemptId}/review`, payload);
    return unwrapData(response);
  },

  async listStaffClassroomProposals(status) {
    const response = await axiosClient.get('/api/staff/classroom-proposals', {
      params: status && status !== 'ALL' ? { status } : undefined,
    });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : [];
  },

  async createClassroomProposal(payload) {
    const response = await axiosClient.post('/api/staff/classroom-proposals', payload);
    return unwrapData(response);
  },

  async updateClassroomProposal(proposalId, payload) {
    const response = await axiosClient.put(`/api/staff/classroom-proposals/${proposalId}`, payload);
    return unwrapData(response);
  },

  async submitClassroomProposal(proposalId) {
    const response = await axiosClient.patch(`/api/staff/classroom-proposals/${proposalId}/submit`);
    return unwrapData(response);
  },

  async listManagerClassroomProposals(status = 'PENDING_APPROVAL') {
    const response = await axiosClient.get('/api/manager/classroom-proposals', {
      params: { status },
    });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : [];
  },

  async approveClassroomProposal(proposalId) {
    const response = await axiosClient.patch(`/api/manager/classroom-proposals/${proposalId}/approve`);
    return unwrapData(response);
  },

  async rejectClassroomProposal(proposalId, reason) {
    const response = await axiosClient.patch(`/api/manager/classroom-proposals/${proposalId}/reject`, { reason });
    return unwrapData(response);
  },
};

export default enrollmentRequestApi;
