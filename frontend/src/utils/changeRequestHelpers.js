const FIELD_LABELS = {
  sessionDate: 'Ngày học',
  startTime: 'Giờ bắt đầu',
  endTime: 'Giờ kết thúc',
  teacherId: 'Giáo viên (ID)',
  roomId: 'Phòng học (ID)',
  googleMeetUrl: 'Liên kết Google Meet',
  status: 'Trạng thái buổi',
  classroomOfferingId: 'Lớp học (ID)',
};

export function parseChangeRequestValues(json) {
  if (!json) return {};
  try {
    return typeof json === 'string' ? JSON.parse(json) : json;
  } catch {
    return {};
  }
}

export function formatChangeRequestValue(key, value) {
  if (value == null || value === '') return '—';
  if (key === 'sessionDate') return String(value);
  if (key === 'startTime' || key === 'endTime') return String(value).slice(0, 5);
  return String(value);
}

export function buildChangeRequestDiff(oldValuesJson, newValuesJson) {
  const oldValues = parseChangeRequestValues(oldValuesJson);
  const newValues = parseChangeRequestValues(newValuesJson);
  const keys = new Set([...Object.keys(oldValues), ...Object.keys(newValues)]);
  const rows = [];

  keys.forEach((key) => {
    const oldValue = oldValues[key];
    const newValue = newValues[key];
    if (oldValue === newValue) return;
    if (oldValue == null && newValue == null) return;
    rows.push({
      key,
      label: FIELD_LABELS[key] || key,
      oldValue: formatChangeRequestValue(key, oldValue),
      newValue: formatChangeRequestValue(key, newValue),
    });
  });

  return rows;
}

export function hasBlockingConflict(conflictResult) {
  if (!conflictResult) return false;
  return Boolean(
    conflictResult.hasBlockingConflict
    || conflictResult.hasConflict
    || (conflictResult.conflicts && conflictResult.conflicts.length > 0),
  );
}
