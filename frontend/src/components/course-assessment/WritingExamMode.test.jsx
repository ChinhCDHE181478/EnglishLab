/** @vitest-environment jsdom */

import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import WritingExamMode from './WritingExamMode';

globalThis.IS_REACT_ACT_ENVIRONMENT = true;

describe('WritingExamMode rich prompt', () => {
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

  it('renders prompt paragraphs as HTML without nested paragraph elements', async () => {
    await act(async () => {
      root = createRoot(container);
      root.render(
        <WritingExamMode
          assessment={{ title: 'Writing test', timeLimitMinutes: 60 }}
          config={{
            title: 'Writing test',
            durationMinutes: 60,
            tasks: [{
              key: 'task_1',
              title: 'Task 1',
              heading: 'Writing Task 1',
              promptHtml: '<p><strong>Describe the chart.</strong></p><p>Summarise the main features.</p>',
              promptParagraphs: ['Legacy prompt must not win'],
              minimumWords: 150,
              recommendedMinutes: 20,
            }],
          }}
          onClose={() => {}}
          onSubmit={() => {}}
        />,
      );
    });

    expect(container.textContent).toContain('Describe the chart.');
    expect(container.textContent).toContain('Summarise the main features.');
    expect(container.textContent).not.toContain('<p>');
    expect(container.textContent).not.toContain('Legacy prompt must not win');
    expect(container.querySelector('p p')).toBeNull();
  });
});
