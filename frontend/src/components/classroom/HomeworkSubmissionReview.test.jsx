// @vitest-environment jsdom
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';
import HomeworkSubmissionReview, { hasHomeworkTeacherEvaluation } from './HomeworkSubmissionReview';

describe('HomeworkSubmissionReview', () => {
  it('shows teacher feedback and inline annotations even when score is absent', () => {
    const text = 'I go to school yesterday.';
    const submission = {
      status: 'SUBMITTED',
      textAnswer: text,
      teacherFeedback: 'Em cần kiểm tra lại thì quá khứ.',
      annotations: [{
        id: 'annotation-1',
        type: 'CORRECTION',
        startOffset: 2,
        endOffset: 4,
        selectedText: 'go',
        replacementText: 'went',
      }],
    };

    const markup = renderToStaticMarkup(
      <HomeworkSubmissionReview homework={{ maxScore: 10 }} submission={submission} />,
    );

    expect(hasHomeworkTeacherEvaluation(submission)).toBe(true);
    expect(markup).toContain('Đánh giá của giảng viên');
    expect(markup).toContain('Em cần kiểm tra lại thì quá khứ.');
    expect(markup).toContain('went');
    expect(markup).not.toContain('/ 10 điểm');
  });

  it('keeps a submitted answer visible while teacher evaluation is pending', () => {
    const markup = renderToStaticMarkup(
      <HomeworkSubmissionReview
        homework={{ maxScore: 10 }}
        submission={{ status: 'SUBMITTED', textAnswer: 'My submitted answer.' }}
      />,
    );

    expect(markup).toContain('Đang chờ đánh giá');
    expect(markup).toContain('My submitted answer.');
  });

  it('shows the complete AI evaluation instead of only its summary', () => {
    const markup = renderToStaticMarkup(
      <HomeworkSubmissionReview
        homework={{ maxScore: 10 }}
        submission={{
          status: 'GRADED',
          score: 7.5,
          teacherFeedback: '<p>Nhận xét cuối của giáo viên.</p>',
          aiFeedbackJson: JSON.stringify({
            summary: 'Bài viết bám đúng chủ đề.',
            criteria: [{ name: 'Task Response', score: 7, feedback: 'Luận điểm khá rõ.' }],
            strengths: ['Bố cục hợp lý.'],
            weaknesses: ['Ví dụ còn chung chung.'],
            suggestions: ['Bổ sung ví dụ cụ thể.'],
          }),
        }}
      />,
    );

    expect(markup).toContain('Phân tích hỗ trợ từ AI');
    expect(markup).toContain('Task Response');
    expect(markup).toContain('Bố cục hợp lý.');
    expect(markup).toContain('Ví dụ còn chung chung.');
    expect(markup).toContain('Bổ sung ví dụ cụ thể.');
    expect(markup).toContain('Nhận xét cuối của giáo viên.');
  });
});
