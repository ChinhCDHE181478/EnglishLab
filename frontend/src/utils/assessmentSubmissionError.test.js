import { describe, expect, it } from 'vitest';
import {
  getAssessmentSubmissionErrorMessage,
  isTemporaryAssessmentSubmissionError,
} from './assessmentSubmissionError';

describe('assessment submission errors', () => {
  it('does not retry validation errors', () => {
    expect(isTemporaryAssessmentSubmissionError({ response: { status: 400 } })).toBe(false);
    expect(isTemporaryAssessmentSubmissionError({ response: { status: 422 } })).toBe(false);
  });

  it('queues network and temporary server failures', () => {
    expect(isTemporaryAssessmentSubmissionError({ code: 'ERR_NETWORK' })).toBe(true);
    expect(isTemporaryAssessmentSubmissionError({ response: { status: 429 } })).toBe(true);
    expect(isTemporaryAssessmentSubmissionError({ response: { status: 503 } })).toBe(true);
  });

  it('keeps the actionable backend validation message', () => {
    expect(getAssessmentSubmissionErrorMessage({
      response: { data: { message: 'Bài nghe hoặc bài đọc không dùng bộ tiêu chí viết.' } },
    })).toBe('Bài nghe hoặc bài đọc không dùng bộ tiêu chí viết.');
  });
});
