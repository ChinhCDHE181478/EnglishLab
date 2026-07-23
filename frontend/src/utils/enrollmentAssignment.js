const toLocalDateKey = (value) => {
  const date = value instanceof Date && !Number.isNaN(value.getTime()) ? value : new Date();
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-');
};

export const isAssignableClassroom = (classroom, today = new Date()) => {
  if (!classroom || classroom.classroomStatus !== 'UPCOMING') return false;
  if (classroom.packageStatus !== 'PUBLISHED') return false;
  if (!classroom.startDate || classroom.startDate <= toLocalDateKey(today)) return false;

  const capacity = Number(classroom.maxCapacity || 0);
  const enrolled = Number(classroom.enrolledCount || 0);
  return capacity <= 0 || enrolled < capacity;
};

export const getEnrollmentRequestActions = (status) => {
  const canCompleteConsultation = ['SUBMITTED', 'UNDER_STAFF_REVIEW'].includes(status);
  const canAssign = status === 'WAITING_FOR_CLASS';

  return {
    canCompleteConsultation,
    canAssign,
    canReject: canCompleteConsultation || canAssign,
  };
};

export const loadStaffEnrollmentData = async (loadRequests, loadClassrooms) => {
  const [requestResult, classroomResult] = await Promise.allSettled([
    Promise.resolve().then(loadRequests),
    Promise.resolve().then(loadClassrooms),
  ]);

  return {
    requests: requestResult.status === 'fulfilled' && Array.isArray(requestResult.value)
      ? requestResult.value
      : [],
    classrooms: classroomResult.status === 'fulfilled' && Array.isArray(classroomResult.value)
      ? classroomResult.value
      : [],
    requestError: requestResult.status === 'rejected' ? requestResult.reason : null,
    classroomError: classroomResult.status === 'rejected' ? classroomResult.reason : null,
  };
};

export const getStaffEnrollmentLoadError = (error, resource = 'requests') => {
  const responseMessage = error?.response?.data?.message;
  if (responseMessage) return responseMessage;

  if (error?.response?.status === 403) {
    return resource === 'classrooms'
      ? 'Không thể tải danh sách lớp: tài khoản hiện tại không có quyền vận hành lớp học.'
      : 'Không thể tải yêu cầu đăng ký: tài khoản hiện tại không có quyền Staff để xử lý.';
  }

  if (!error?.response) {
    return resource === 'classrooms'
      ? 'Không thể kết nối máy chủ để tải danh sách lớp.'
      : 'Không thể kết nối máy chủ để tải danh sách yêu cầu đăng ký.';
  }

  return resource === 'classrooms'
    ? 'Không thể tải danh sách lớp để xếp học viên.'
    : 'Không thể tải danh sách yêu cầu đăng ký.';
};
