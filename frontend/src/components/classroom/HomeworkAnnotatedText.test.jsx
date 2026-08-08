import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';
import HomeworkAnnotatedText from './HomeworkAnnotatedText';

describe('HomeworkAnnotatedText', () => {
  it('keeps annotation notes outside the selectable answer text', () => {
    const text = 'I go to school yesterday and I study English everyday.';
    const annotations = [
      {
        id: 'annotation-1',
        type: 'CORRECTION',
        startOffset: 2,
        endOffset: 4,
        selectedText: 'go',
        replacementText: 'went',
      },
      {
        id: 'annotation-2',
        type: 'NOTE',
        startOffset: 31,
        endOffset: 36,
        selectedText: 'study',
        note: 'Kiểm tra lại thì của động từ.',
      },
    ];

    const markup = renderToStaticMarkup(
      <HomeworkAnnotatedText annotations={annotations} text={text} />,
    );
    const canvasMarkup = markup.match(
      /<div data-annotation-canvas="true"[^>]*>([\s\S]*?)<\/div>/,
    )?.[1];
    const selectableText = canvasMarkup?.replace(/<[^>]*>/g, '');

    expect(selectableText).toBe(text);
    expect(markup).toContain('Kiểm tra lại thì của động từ.');
  });
});
