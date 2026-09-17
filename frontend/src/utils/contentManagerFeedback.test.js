import { describe, expect, it } from 'vitest';
import {
  createContentManagerError,
  getContentManagerError,
  getContentManagerFeedbackMessage,
} from './contentManagerFeedback';

describe('contentManagerFeedback', () => {
  it('keeps the backend message and error code', () => {
    const feedback = getContentManagerError({
      response: {
        status: 409,
        data: { message: 'Mã đã tồn tại.', code: 'DATA_CONFLICT' },
      },
    }, 'Không thể lưu dữ liệu.');

    expect(feedback).toEqual({ message: 'Mã đã tồn tại.', code: 'DATA_CONFLICT' });
  });

  it('uses an HTTP code when the backend omits its code', () => {
    const feedback = getContentManagerError({
      response: { status: 404, data: {} },
    }, 'Không tìm thấy dữ liệu.');

    expect(feedback).toEqual({ message: 'Không tìm thấy dữ liệu.', code: 'HTTP_404' });
  });

  it('distinguishes network failures from local validation failures', () => {
    expect(getContentManagerError({ request: {} }, 'Không tải được dữ liệu.').code).toBe('NETWORK_ERROR');
    expect(createContentManagerError('Vui lòng nhập tên.').code).toBe('VALIDATION_ERROR');
  });

  it('reads a display message from both feedback objects and strings', () => {
    expect(getContentManagerFeedbackMessage({ message: 'Có lỗi.' })).toBe('Có lỗi.');
    expect(getContentManagerFeedbackMessage('Có lỗi.')).toBe('Có lỗi.');
  });
});
