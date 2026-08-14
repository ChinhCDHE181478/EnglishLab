import axiosClient from './axiosClient';

const unwrapData = (response) => response?.data?.data ?? response?.data;

const asList = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.items)) return data.items;
  return [];
};

const enrollmentRequestApi = {
  async getCourseOfferings(deliveryType) {
    const response = await axiosClient.get('/api/course-offerings', {
      params: deliveryType && deliveryType !== 'ALL' ? { deliveryType } : undefined,
      skipAuthRedirect: true,
    });
    const data = unwrapData(response);
    return Array.isArray(data) ? data : data?.content || [];
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

  async listForStaff(status) {
    const response = await axiosClient.get('/api/staff/enrollment-requests', {
      params: status && status !== 'ALL' ? { status } : undefined,
    });
    return asList(unwrapData(response));
  },

  async createAtCenter(payload) {
    const response = await axiosClient.post('/api/staff/enrollment-requests/center', payload);
    return unwrapData(response);
  },

  async scheduleTest(requestId, payload) {
    const response = await axiosClient.patch(
      `/api/staff/enrollment-requests/${requestId}/schedule-test`,
      payload,
    );
    return unwrapData(response);
  },

  async completeTest(requestId, payload) {
    const response = await axiosClient.patch(
      `/api/staff/enrollment-requests/${requestId}/complete-test`,
      payload,
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

  async validateClassroomProposalSchedule(payload, excludeProposalId) {
    const response = await axiosClient.post('/api/staff/classroom-proposals/validate-schedule', payload, {
      params: excludeProposalId ? { excludeProposalId } : undefined,
    });
    return unwrapData(response);
  },

  async getClassroomProposalAvailability(payload, excludeProposalId) {
    const response = await axiosClient.post('/api/staff/classroom-proposals/availability', payload, {
      params: excludeProposalId ? { excludeProposalId } : undefined,
    });
    return unwrapData(response);
  },

  async getAvailableClassroomIds(requestId) {
    const response = await axiosClient.get(`/api/staff/enrollment-requests/${requestId}/available-classrooms`);
    return asList(unwrapData(response));
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

  async getManagerEnrollmentDemand() {
    const response = await axiosClient.get('/api/manager/enrollment-demand');
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
