export const COURSE_VERSION_STATUS_META = {
  DRAFT: { label: 'Bản nháp — chưa gửi', className: 'border-amber-300 bg-amber-50 text-amber-800' },
  PENDING_REVIEW: { label: 'Chờ duyệt', className: 'border-amber-200 bg-amber-50 text-amber-700' },
  PUBLISHED: { label: 'Đang xuất bản', className: 'border-[#4b0009]/30 bg-[#4b0009] text-white' },
  RETIRED: { label: 'Ngừng áp dụng', className: 'border-slate-200 bg-slate-100 text-slate-500' },
};

export const getCourseVersionLabel = (version) => {
  if (!version) return 'Chưa có phiên bản';
  if (version.status === 'DRAFT') return `Bản nháp v${version.versionNumber} — chưa gửi`;
  const status = COURSE_VERSION_STATUS_META[version.status]?.label || version.status || 'Không rõ';
  return `${status} v${version.versionNumber}`;
};

export const findEditableCourseVersion = (versions = []) => (
  versions.find((version) => version.status === 'DRAFT') || null
);
