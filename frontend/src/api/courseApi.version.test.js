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

  it('uses version-scoped endpoints for draft, review and manager publish', async () => {
    axiosClient.get.mockResolvedValue({ data: [{ id: 12, status: 'DRAFT' }] });
    axiosClient.post.mockResolvedValue({ data: { id: 12, status: 'DRAFT' } });
    axiosClient.patch.mockResolvedValue({ data: { id: 12, status: 'PENDING_REVIEW' } });

    await courseApi.getOnlineCourseVersions(7);
    await courseApi.createOnlineCourseVersion(7, 'Bổ sung bài nghe');
    await courseApi.submitOnlineCourseVersion(7, 12);
    await courseApi.publishOnlineCourseVersion(7, 12);

    expect(axiosClient.get).toHaveBeenCalledWith('/api/content-manager/online-courses/7/versions');
    expect(axiosClient.post).toHaveBeenCalledWith('/api/content-manager/online-courses/7/versions', { changeNote: 'Bổ sung bài nghe' });
    expect(axiosClient.patch).toHaveBeenNthCalledWith(1, '/api/content-manager/online-courses/7/versions/12/submit-review');
    expect(axiosClient.patch).toHaveBeenNthCalledWith(2, '/api/manager/online-courses/7/versions/12/publish');
    expect(axiosClient.patch.mock.calls.some(([url]) => /student|progress|enrollment|certificate/i.test(url))).toBe(false);
  });

  it('routes the legacy send-review action through the active draft version', async () => {
    axiosClient.get
      .mockResolvedValueOnce({ data: [{ id: 15, status: 'DRAFT' }] })
      .mockResolvedValueOnce({ data: { id: 7, status: 'PENDING_REVIEW' } });
    axiosClient.patch.mockResolvedValue({ data: { id: 15, status: 'PENDING_REVIEW' } });

    const result = await courseApi.submitOnlineCourseForReview(7);

    expect(axiosClient.patch).toHaveBeenCalledWith('/api/content-manager/online-courses/7/versions/15/submit-review');
    expect(result.status).toBe('PENDING_REVIEW');
    expect(axiosClient.patch.mock.calls.some(([url]) => url.endsWith('/publish'))).toBe(false);
  });
});
