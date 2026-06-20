export const formatClassroomDate = (value) => {
  if (!value) return 'Đang cập nhật';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(value));
};

export const formatClassroomTime = (value) => {
  if (!value) return '';
  const normalized = String(value).length <= 5 ? `${value}:00` : value;
  const date = new Date(`1970-01-01T${normalized}`);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
};

export const formatClassroomDateTime = (value) => {
  if (!value) return 'Đang cập nhật';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
};

export const formatClassroomPrice = (value) => {
  if (value == null || value === '') return 'Liên hệ';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(Number(value));
};

export const formatDeliveryMode = (mode, label) => {
  if (label) return label;
  if (mode === 'VIRTUAL') return 'Trực tuyến';
  if (mode === 'OFFLINE') return 'Tại trung tâm';
  return 'Đang cập nhật';
};

export const formatOfflineLocation = (item, fallback = 'Cơ sở Hà Nội') =>
  item?.offlineAddress || fallback;

export const formatOfflineSessionLocation = (session, fallback = 'Cơ sở Hà Nội') => {
  const room = session?.roomName;
  const address = session?.offlineAddress || fallback;
  if (!room) return 'Đang xếp phòng';
  return `${room} · ${address}`;
};

export const formatOfferingStatus = (status) => {
  const map = {
    DRAFT: 'Nháp',
    UPCOMING: 'Sắp khai giảng',
    ACTIVE: 'Đang học',
    OPEN: 'Đang mở đăng ký',
    FULL: 'Đã đủ chỗ',
    IN_PROGRESS: 'Đang học',
    COMPLETED: 'Đã kết thúc',
    CANCELLED: 'Đã hủy',
    CLOSED: 'Đã đóng',
  };
  return map[status] || status || 'Đang cập nhật';
};

export const formatRegistrationStatus = (status, label) => {
  if (label) return label;
  const map = {
    PENDING_CONFIRMATION: 'Chờ xác nhận',
    PENDING_TUITION_PAYMENT: 'Chờ thanh toán học phí',
    DEPOSIT_PAID: 'Đã đặt cọc',
    PARTIALLY_PAID: 'Thanh toán một phần',
    FULLY_PAID: 'Đã thanh toán đủ',
    ASSIGNED: 'Đã được xếp lớp',
    WAITLIST: 'Chờ xếp lớp',
    REJECTED: 'Từ chối',
    CANCELLED: 'Đã hủy',
  };
  return map[status] || status || 'Đang cập nhật';
};

export const formatTuitionSettlement = (type, label) => {
  if (label) return label;
  const map = {
    NEED_ADDITIONAL_PAYMENT: 'Cần thanh toán thêm',
    HAS_BALANCE: 'Có số dư',
    NEED_REFUND: 'Cần xử lý hoàn tiền',
  };
  return map[type] || null;
};

export const formatTuitionPaymentKind = (kind, label) => {
  if (label) return label;
  const map = {
    DEPOSIT: 'Đặt cọc',
    PARTIAL: 'Thanh toán một phần',
    FULL: 'Thanh toán đủ',
    MANUAL_CONFIRMATION: 'Xác nhận thủ công',
  };
  return map[kind] || kind || 'Ghi nhận học phí';
};

export const formatSessionStatus = (status) => {
  const map = {
    SCHEDULED: 'Đã lên lịch',
    OPEN: 'Đang diễn ra',
    COMPLETED: 'Đã kết thúc',
    CANCELLED: 'Đã hủy',
  };
  return map[status] || status || 'Đang cập nhật';
};

export const formatAttendanceStatus = (status) => {
  const map = {
    PRESENT: 'Có mặt',
    ABSENT: 'Vắng',
    LATE: 'Đi muộn',
    EXCUSED: 'Có phép',
  };
  return map[status] || status || 'Chưa ghi nhận';
};

export const formatHomeworkStatus = (status, overdue) => {
  if (overdue) return 'Quá hạn';
  const map = {
    DRAFT: 'Nháp',
    PUBLISHED: 'Đã giao',
    CLOSED: 'Đã đóng',
  };
  return map[status] || status || 'Đang cập nhật';
};

export const formatGradebookFinalResult = (value) => {
  if (value == null || value === '') return 'Chưa công bố';
  if (typeof value === 'number') return `${value}/10`;
  const text = String(value).trim();
  if (/^-?\d+(\.\d+)?$/.test(text)) return `${Number(text)}/10`;
  return text;
};

export const isGradebookPassed = (value) => {
  if (value == null || value === '') return false;
  if (typeof value === 'number') return value >= 5;
  const text = String(value).trim();
  if (/^-?\d+(\.\d+)?$/.test(text)) return Number(text) >= 5;
  const upper = text.toUpperCase();
  return upper === 'PASSED' || text === 'ĐẠT' || upper.includes('PASS');
};

export const openLarkMeeting = (url) => {
  if (!url) {
    return { ok: false, message: 'Chưa có liên kết Lark cho buổi học này.' };
  }

  const popup = window.open(url, '_blank', 'noopener,noreferrer');
  if (!popup) {
    return {
      ok: false,
      message: 'Trình duyệt đã chặn cửa sổ mới. Hãy cho phép popup hoặc mở liên kết thủ công.',
      url,
    };
  }

  return { ok: true };
};
