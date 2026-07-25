import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./axiosClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
  },
}));

import axiosClient from './axiosClient';
import courseApi from './courseApi';

describe('course version workflow API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('uses Content Manager endpoints for draft creation and direct publishing', async () => {
    axiosClient.get.mockResolvedValue({ data: [{ id: 12, status: 'DRAFT' }] });
    axiosClient.post.mockResolvedValue({ data: { id: 12, status: 'DRAFT' } });
    axiosClient.patch.mockResolvedValue({ data: { id: 12, status: 'PUBLISHED' } });

    await courseApi.getOnlineCourseVersions(7);
    await courseApi.createOnlineCourseVersion(7, 'Bổ sung bài nghe');
    await courseApi.publishOnlineCourseVersion(7, 12);

    expect(axiosClient.get).toHaveBeenCalledWith('/api/content-manager/online-courses/7/versions');
    expect(axiosClient.post).toHaveBeenCalledWith('/api/content-manager/online-courses/7/versions', { changeNote: 'Bổ sung bài nghe' });
    expect(axiosClient.patch).toHaveBeenCalledWith('/api/content-manager/online-courses/7/versions/12/publish');
    expect(axiosClient.patch.mock.calls.some(([url]) => url.startsWith('/api/manager/'))).toBe(false);
    expect(axiosClient.patch.mock.calls.some(([url]) => /student|progress|enrollment|certificate/i.test(url))).toBe(false);
  });

  it('publishes the active draft from the course-level action', async () => {
    axiosClient.get
      .mockResolvedValueOnce({ data: [{ id: 15, status: 'DRAFT' }] })
      .mockResolvedValueOnce({ data: { id: 7, status: 'PUBLISHED' } });
    axiosClient.patch.mockResolvedValue({ data: { id: 15, status: 'PUBLISHED' } });

    const result = await courseApi.publishOnlineCourseDraft(7);

    expect(axiosClient.patch).toHaveBeenCalledWith('/api/content-manager/online-courses/7/versions/15/publish');
    expect(result.status).toBe('PUBLISHED');
  });
});
