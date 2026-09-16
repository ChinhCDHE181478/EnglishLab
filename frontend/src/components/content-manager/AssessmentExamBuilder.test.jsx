/** @vitest-environment jsdom */

import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import AssessmentExamBuilder from './AssessmentExamBuilder';

globalThis.IS_REACT_ACT_ENVIRONMENT = true;

const createAssessment = (group = {}) => ({
  title: 'Bài nghe thử',
  skill: 'LISTENING',
  timeLimitMinutes: 30,
  objectiveAnswerKey: JSON.stringify({ 1: 'Keiko' }),
  uiConfigJson: JSON.stringify({
    title: 'Bài nghe thử',
    key: 'listening_test',
    durationMinutes: 30,
    audioUrl: '/audio/test.mp3',
    parts: [{
      key: 'part_1',
      partNumber: 1,
      title: 'Part 1',
      questionGroups: [{
        title: 'Questions 1-1',
        instructions: 'Complete the form.',
        type: 'text',
        questions: [{ number: 1, promptBefore: 'First name' }],
        ...group,
      }],
    }],
  }),
});

const findButton = (container, label) => Array.from(container.querySelectorAll('button'))
  .find((button) => button.textContent.trim() === label);

describe('AssessmentExamBuilder', () => {
  let container;
  let root;

  beforeEach(() => {
    container = document.createElement('div');
    document.body.appendChild(container);
  });

  afterEach(async () => {
    await act(async () => root?.unmount());
    container.remove();
  });

  const renderBuilder = async (assessment) => {
    await act(async () => {
      root = createRoot(container);
      root.render(<AssessmentExamBuilder assessment={assessment} onChange={() => {}} />);
    });
    await act(async () => findButton(container, 'Biên soạn bài nghe').click());
  };

  it('keeps empty optional question fields collapsed until requested', async () => {
    await renderBuilder(createAssessment());

    expect(container.textContent).not.toContain('Nội dung sau ô trả lời');

    await act(async () => findButton(container, 'Thông tin bổ sung của câu hỏi').click());

    expect(container.textContent).toContain('Nội dung sau ô trả lời');
    expect(container.textContent).toContain('Mốc audio/transcript');
  });

  it('migrates and reveals legacy group descriptions', async () => {
    await renderBuilder(createAssessment({ description: 'Personal details for homestay application' }));

    const descriptionField = Array.from(container.querySelectorAll('label'))
      .find((label) => label.textContent.includes('Nội dung dẫn nhập hoặc biểu mẫu'))
      ?.querySelector('textarea');

    expect(descriptionField?.value).toBe('Personal details for homestay application');
  });
});
