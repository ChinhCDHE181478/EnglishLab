/** @vitest-environment jsdom */

import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AppDialogProvider } from '../ui/AppDialog';
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

  const renderBuilder = async (assessment, buttonLabel = 'Biên soạn bài nghe', onChange = () => {}) => {
    await act(async () => {
      root = createRoot(container);
      root.render(
        <AppDialogProvider>
          <AssessmentExamBuilder assessment={assessment} onChange={onChange} />
        </AppDialogProvider>,
      );
    });
    await act(async () => findButton(container, buttonLabel).click());
  };

  it('keeps empty optional question fields collapsed until requested', async () => {
    await renderBuilder(createAssessment());

    expect(container.textContent).not.toContain('Nội dung sau ô trả lời');

    await act(async () => findButton(container, 'Thông tin bổ sung của câu hỏi').click());

    expect(container.textContent).toContain('Nội dung sau ô trả lời');
  });

  it('preserves a legacy group description through save even without a dedicated field', async () => {
    // "Hướng dẫn" and the old "Nội dung dẫn nhập hoặc biểu mẫu" field were merged into one
    // ("Hướng dẫn") field. Legacy descriptionHtml content is no longer editable in the builder,
    // but a save must not silently drop it — the learner view still falls back to it.
    const onChange = vi.fn();
    await renderBuilder(createAssessment({ description: 'Personal details for homestay application' }), 'Biên soạn bài nghe', onChange);

    await act(async () => findButton(container, 'Lưu cấu trúc bài nghe').click());

    const configCall = onChange.mock.calls.find(([field]) => field === 'uiConfigJson');
    const saved = JSON.parse(configCall[1]);
    expect(saved.parts[0].questionGroups[0].descriptionHtml).toBe('Personal details for homestay application');
  });

  it('keeps IELTS Listening focused on one assessment audio source', async () => {
    await renderBuilder(createAssessment({
      audioUrl: '/audio/group.mp3',
      perQuestionAudio: true,
      questions: [{ number: 1, promptBefore: 'First name', imageUrl: '/images/question.png', audioUrl: '/audio/question.mp3' }],
    }));

    expect(container.textContent).toContain('Audio của toàn bài nghe');
    expect(container.textContent).not.toContain('Mô tả ngắn');
    // Non-TOEIC Listening has no TOEIC-only media fields left, so the whole "Thông tin bổ sung
    // của nhóm" section is hidden instead of showing an empty collapsible.
    expect(findButton(container, 'Thông tin bổ sung của nhóm')).toBeUndefined();

    await act(async () => findButton(container, 'Thông tin bổ sung của câu hỏi').click());

    expect(container.textContent).not.toContain('Audio nhóm');
    expect(container.textContent).not.toContain('Audio từng câu');
    expect(container.textContent).not.toContain('Ảnh câu hỏi');
    expect(container.textContent).not.toContain('Audio câu hỏi');
  });

  it('edits Reading passage inside the part that the learner view consumes', async () => {
    const reading = {
      ...createAssessment(),
      title: 'Bài đọc thử',
      skill: 'READING',
      uiConfigJson: JSON.stringify({
        title: 'Bài đọc thử',
        key: 'reading_test',
        durationMinutes: 30,
        parts: [{
          key: 'part_1',
          partNumber: 1,
          title: 'Passage 1',
          passage: { title: 'The future of work', paragraphs: [{ html: '<p>Existing passage</p>' }] },
          questionGroups: [{
            title: 'Questions 1-1',
            instructions: 'Choose the answer.',
            type: 'text',
            questions: [{ number: 1, promptBefore: 'Answer' }],
          }],
        }],
      }),
    };

    await renderBuilder(reading, 'Biên soạn bài đọc');

    expect(container.textContent).toContain('Nội dung bài đọc');
    expect(container.querySelector('[contenteditable="true"]')?.innerHTML).toBe('<p>Existing passage</p>');
    expect(container.textContent).not.toContain('Audio câu hỏi');
  });

  it('stores Writing prompts as rich HTML without nesting paragraph containers', async () => {
    const onChange = vi.fn();
    const writing = {
      title: 'Bài viết thử',
      skill: 'WRITING',
      timeLimitMinutes: 60,
      uiConfigJson: JSON.stringify({
        title: 'Bài viết thử',
        key: 'writing_test',
        durationMinutes: 60,
        tasks: [{
          key: 'task_1',
          title: 'Task 1',
          heading: 'Writing Task 1',
          promptParagraphs: ['Legacy prompt'],
          minimumWords: 150,
          recommendedMinutes: 20,
        }],
      }),
    };

    await renderBuilder(writing, 'Biên soạn đề viết', onChange);
    const editor = container.querySelector('[contenteditable="true"]');
    expect(editor?.parentElement?.tagName).toBe('DIV');

    await act(async () => {
      editor.innerHTML = '<p><strong>Describe the chart.</strong></p><p>Summarise the main features.</p>';
      editor.dispatchEvent(new Event('input', { bubbles: true }));
    });
    await act(async () => findButton(container, 'Lưu cấu trúc đề viết').click());

    const configCall = onChange.mock.calls.find(([field]) => field === 'uiConfigJson');
    const saved = JSON.parse(configCall[1]);
    expect(saved.tasks[0].promptHtml).toBe('<p><strong>Describe the chart.</strong></p><p>Summarise the main features.</p>');
    expect(saved.tasks[0].promptHtml).not.toContain('<p><p>');
  });
});
