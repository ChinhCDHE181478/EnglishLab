import { describe, expect, it } from 'vitest';
import { findEditableCourseVersion, getCourseVersionLabel } from './courseVersionUi';

describe('course version badges', () => {
  it('renders explicit version labels and selects only a draft as editable', () => {
    const versions = [
      { id: 2, versionNumber: 2, status: 'DRAFT' },
      { id: 1, versionNumber: 1, status: 'PUBLISHED' },
    ];

    expect(getCourseVersionLabel(versions[0])).toBe('Bản nháp v2 — chưa gửi');
    expect(getCourseVersionLabel(versions[1])).toBe('Đang xuất bản v1');
    expect(findEditableCourseVersion(versions)).toEqual(versions[0]);
    expect(findEditableCourseVersion([{ id: 3, status: 'PENDING_REVIEW' }])).toBeNull();
  });
});
