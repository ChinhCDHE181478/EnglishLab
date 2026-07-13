const resolveDate = (value) => {
  if (!value) return null;
  const raw = String(value).trim();
  if (!raw) return null;

  let parsed = new Date(raw);
  if (!Number.isNaN(parsed.getTime())) return parsed;

  // Handle common backend datetime strings like "2026-06-20 10:30:00"
  if (raw.includes(' ') && !raw.includes('T')) {
    parsed = new Date(raw.replace(' ', 'T'));
    if (!Number.isNaN(parsed.getTime())) return parsed;
  }

  return null;
};

const isTimeOnlyValue = (value) => /^\d{1,2}:\d{2}(:\d{2})?$/.test(String(value || '').trim());

export const formatClassroomDate = (value) => {
  if (!value) return 'Đang cập nhật';
  const date = resolveDate(value);
  if (!date) return 'Đang cập nhật';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
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
  if (isTimeOnlyValue(value)) return formatClassroomTime(value);
  const date = resolveDate(value);
  if (!date) return 'Đang cập nhật';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
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
    REFUND: 'Hoàn tiền học phí',
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
    OPEN: 'Đang mở',
    CLOSED: 'Đã đóng',
  };
  return map[status] || status || 'Đang cập nhật';
};

export const getHomeworkMaxScore = (homework) => {
  const parsed = Number(homework?.maxScore);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 10;
};

export const getSubmissionFeedback = (submission) => submission?.teacherFeedback || submission?.feedback || '';

export const toDateTimeLocalValue = (value) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const pad = (part) => String(part).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

export const fromDateTimeLocalValue = (value) => {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  const pad = (part) => String(part).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
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

const sanitizeFileName = (value) => String(value || 'tai-lieu')
  .trim()
  .replace(/[<>:"/\\|?*]+/g, '-')
  .replace(/\s+/g, '-')
  .slice(0, 120) || 'tai-lieu';

export const buildMaterialDownloadName = (material) => {
  const baseName = sanitizeFileName(material?.title);
  const fileType = String(material?.fileType || '').replace(/^\./, '');
  if (fileType && !baseName.toLowerCase().endsWith(`.${fileType.toLowerCase()}`)) {
    return `${baseName}.${fileType}`;
  }
  return baseName;
};

export const downloadClassroomMaterial = async (material) => {
  const url = material?.fileUrl;
  if (!url) return;

  const fileName = buildMaterialDownloadName(material);
  try {
    const response = await fetch(url, { credentials: 'include' });
    if (!response.ok) {
      window.open(url, '_blank', 'noopener,noreferrer');
      return;
    }
    const blob = await response.blob();
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(objectUrl);
  } catch {
    window.open(url, '_blank', 'noopener,noreferrer');
  }
};
