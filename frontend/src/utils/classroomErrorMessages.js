const CONFLICT_TYPE_MESSAGES = {
  TEACHER_SCHEDULE: 'Giáo viên đã có lịch trùng trong khung giờ này.',
  LEARNER_SCHEDULE: 'Học viên đã có lớp khác trùng lịch.',
  ROOM_SCHEDULE: 'Phòng học đã được sử dụng trong khung giờ này.',
  LARK_TEACHER_OVERLAP: 'Giáo viên đang có buổi học trực tuyến Lark trùng thời gian.',
  CLASS_CAPACITY: 'Lớp học đã đạt sĩ số tối đa.',
  SESSION_LOCKED: 'Buổi học đã khóa, không thể thay đổi.',
  DUPLICATE_ENROLLMENT: 'Học viên đã được ghi danh vào lớp này.',
};

const STATUS_MESSAGES = {
  400: 'Yêu cầu không hợp lệ. Vui lòng kiểm tra lại thông tin.',
  401: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  403: 'Bạn không có quyền thực hiện thao tác này.',
  404: 'Không tìm thấy dữ liệu lớp học.',
  409: 'Thao tác bị xung đột lịch hoặc trạng thái lớp học.',
  500: 'Máy chủ đang gặp sự cố. Vui lòng thử lại sau.',
};

const formatConflictItems = (conflicts) => {
  const items = conflicts?.conflicts || conflicts?.items || conflicts?.conflictItems || [];
  if (!Array.isArray(items) || !items.length) return [];

  return items.map((item) => {
    if (item?.message) return item.message;
    if (item?.type && CONFLICT_TYPE_MESSAGES[item.type]) return CONFLICT_TYPE_MESSAGES[item.type];
    return 'Phát hiện xung đột lịch hoặc tài nguyên.';
  });
};

export const getClassroomErrorMessage = (error, fallback = 'Không thể xử lý yêu cầu lớp học. Vui lòng thử lại.') => {
  const status = error?.response?.status;
  const data = error?.response?.data;

  if (status === 409) {
    const conflictLines = formatConflictItems(data?.conflicts);
    if (conflictLines.length) {
      return conflictLines.join(' ');
    }
    return data?.message || STATUS_MESSAGES[409];
  }

  if (data?.message) {
    return data.message;
  }

  if (status && STATUS_MESSAGES[status]) {
    return STATUS_MESSAGES[status];
  }

  return fallback;
};

export const getConflictSummary = (conflictResult) => {
  const lines = formatConflictItems(conflictResult);
  if (lines.length) return lines.join(' ');
  if (conflictResult?.hasBlockingConflict || conflictResult?.hasConflict) return 'Phát hiện xung đột lịch hoặc tài nguyên.';
  return '';
};

export default getClassroomErrorMessage;
