import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./axiosClient', () => ({
  default: {
    get: vi.fn(),
  },
}));

import axiosClient from './axiosClient';
import courseApi from './courseApi';

describe('Content Manager course preview API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads the dedicated read-only preview endpoint without learner progress calls', async () => {
    const payload = { course: { slug: 'ielts-foundation' }, previewMode: true };
    axiosClient.get.mockResolvedValue({ data: payload });

    const result = await courseApi.getManagedOnlineCoursePreview('ielts-foundation');

    expect(result).toEqual(payload);
    expect(axiosClient.get).toHaveBeenCalledTimes(1);
    expect(axiosClient.get).toHaveBeenCalledWith('/api/content-manager/online-courses/ielts-foundation/preview');
    expect(axiosClient.get.mock.calls.some(([url]) => /student|progress|enrollment|certificate/i.test(url))).toBe(false);
  });

  it('loads the exact immutable snapshot selected by version id', async () => {
    const payload = { course: { title: 'Nội dung v2' }, previewMode: true };
    axiosClient.get.mockResolvedValue({ data: payload });

    const result = await courseApi.getManagedOnlineCourseVersionPreview(7, 12);

    expect(result).toEqual(payload);
    expect(axiosClient.get).toHaveBeenCalledWith('/api/content-manager/online-courses/7/versions/12/preview');
  });
});
