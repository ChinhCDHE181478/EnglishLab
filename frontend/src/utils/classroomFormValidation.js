const hasValue = (value) => value !== '' && value !== null && value !== undefined;

export function validateClassroomOfferingForm(form) {
  const capacity = Number(form?.maxCapacity);
  if (!Number.isInteger(capacity) || capacity < 1) {
    return 'Sĩ số tối đa phải là số nguyên lớn hơn 0.';
  }
  if (form?.startDate && form?.endDate && form.endDate < form.startDate) {
    return 'Ngày kết thúc phải từ ngày bắt đầu trở đi.';
  }
  const price = hasValue(form?.price) ? Number(form.price) : null;
  const salePrice = hasValue(form?.salePrice) ? Number(form.salePrice) : null;
  if ((price !== null && (!Number.isFinite(price) || price < 0))
      || (salePrice !== null && (!Number.isFinite(salePrice) || salePrice < 0))) {
    return 'Học phí và giá ưu đãi không được âm.';
  }
  if (price !== null && salePrice !== null && salePrice > price) {
    return 'Giá ưu đãi không được lớn hơn học phí gốc.';
  }
  if (['UPCOMING', 'ACTIVE'].includes(form?.classroomStatus)) {
    if (!form.startDate || !form.endDate) {
      return 'Lớp sắp mở hoặc đang hoạt động phải có đủ ngày bắt đầu và kết thúc.';
    }
    if (!form.primaryTeacherId) {
      return 'Lớp sắp mở hoặc đang hoạt động phải có giáo viên chính.';
    }
    if (form.deliveryMode === 'OFFLINE' && !form.defaultRoomId) {
      return 'Lớp học trực tiếp phải có phòng học.';
    }
  }
  return '';
}

export function validateClassroomSessionForm(form) {
  if (!form?.sessionDate || !form?.startTime || !form?.endTime) {
    return 'Vui lòng nhập đủ ngày học, giờ bắt đầu và giờ kết thúc.';
  }
  if (form.endTime <= form.startTime) {
    return 'Giờ kết thúc phải sau giờ bắt đầu.';
  }
  return '';
}
