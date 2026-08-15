import { describe, expect, it } from 'vitest';
import {
  validateClassroomOfferingForm,
  validateClassroomSessionForm,
} from './classroomFormValidation';

const validOffering = {
  maxCapacity: '20',
  startDate: '2026-08-10',
  endDate: '2026-09-10',
  price: '2000000',
  salePrice: '1500000',
  classroomStatus: 'UPCOMING',
  primaryTeacherId: '3',
  deliveryMode: 'OFFLINE',
  defaultRoomId: '2',
  offlineAddress: '',
};

describe('classroom form validation', () => {
  it('rejects an offering whose end date precedes its start date', () => {
    expect(validateClassroomOfferingForm({
      ...validOffering,
      endDate: '2026-08-09',
    })).toBe('Ngày kết thúc phải từ ngày bắt đầu trở đi.');
  });

  it('rejects a sale price above the original price', () => {
    expect(validateClassroomOfferingForm({
      ...validOffering,
      salePrice: '2500000',
    })).toBe('Giá ưu đãi không được lớn hơn học phí gốc.');
  });

  it('requires operational classes to have a teacher', () => {
    expect(validateClassroomOfferingForm({
      ...validOffering,
      primaryTeacherId: '',
    })).toBe('Lớp sắp mở hoặc đang hoạt động phải có giáo viên chính.');
  });

  it('requires offline operational classes to have a room', () => {
    expect(validateClassroomOfferingForm({
      ...validOffering,
      defaultRoomId: '',
    })).toBe('Lớp học trực tiếp phải có phòng học.');
  });

  it('rejects a session whose end time is not after its start time', () => {
    expect(validateClassroomSessionForm({
      sessionDate: '2026-08-10',
      startTime: '20:00',
      endTime: '18:00',
    })).toBe('Giờ kết thúc phải sau giờ bắt đầu.');
  });

  it('accepts valid offering and session forms', () => {
    expect(validateClassroomOfferingForm(validOffering)).toBe('');
    expect(validateClassroomSessionForm({
      sessionDate: '2026-08-10',
      startTime: '18:00',
      endTime: '20:00',
    })).toBe('');
  });
});
