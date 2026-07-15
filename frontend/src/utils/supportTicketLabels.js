export const supportCategoryLabels = {
  ACCOUNT: 'Tài khoản',
  PAYMENT: 'Thanh toán',
  ONLINE_COURSE: 'Khóa học online',
  CLASSROOM: 'Lớp học',
  TECHNICAL: 'Lỗi kỹ thuật',
  OTHER: 'Khác',
};

export const supportStatusLabels = {
  OPEN: 'Mới gửi',
  IN_PROGRESS: 'Đang xử lý',
  WAITING_FOR_LEARNER: 'Chờ học viên',
  RESOLVED: 'Đã giải quyết',
  CLOSED: 'Đã đóng',
};

export const supportPriorityLabels = {
  LOW: 'Thấp',
  NORMAL: 'Bình thường',
  HIGH: 'Cao',
  URGENT: 'Khẩn cấp',
};

export const supportStatusClasses = {
  OPEN: 'border-blue-200 bg-blue-50 text-blue-700',
  IN_PROGRESS: 'border-amber-200 bg-amber-50 text-amber-700',
  WAITING_FOR_LEARNER: 'border-violet-200 bg-violet-50 text-violet-700',
  RESOLVED: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  CLOSED: 'border-slate-200 bg-slate-100 text-slate-600',
};

export const supportPriorityClasses = {
  LOW: 'text-slate-500',
  NORMAL: 'text-blue-700',
  HIGH: 'text-orange-700',
  URGENT: 'text-rose-700',
};

export const supportCategoryOptions = Object.entries(supportCategoryLabels)
  .map(([value, label]) => ({ value, label }));

export const supportStatusOptions = Object.entries(supportStatusLabels)
  .map(([value, label]) => ({ value, label }));

export const supportPriorityOptions = Object.entries(supportPriorityLabels)
  .map(([value, label]) => ({ value, label }));

export const isSupportTicketTerminal = (status) => ['RESOLVED', 'CLOSED'].includes(status);

export const formatSupportTime = (value) => {
  if (!value) return '';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(new Date(value));
};

export const supportApiError = (error, fallback) => error?.response?.data?.message || fallback;
