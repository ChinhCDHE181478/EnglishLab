/** @vitest-environment jsdom */

import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AppDialogProvider } from '../ui/AppDialog';
import RichTextEditor from './RichTextEditor';

globalThis.IS_REACT_ACT_ENVIRONMENT = true;

const selectedElement = () => {
  const node = window.getSelection()?.anchorNode;
  return node?.nodeType === window.Node.TEXT_NODE ? node.parentElement : node;
};

const selectText = (element) => {
  const editor = element.closest('[contenteditable]');
  editor?.focus();
  const range = document.createRange();
  const textNode = element.firstChild;
  range.setStart(textNode, 0);
  range.setEnd(textNode, textNode.textContent.length);
  const selection = window.getSelection();
  selection.removeAllRanges();
  selection.addRange(range);
  document.dispatchEvent(new Event('selectionchange'));
  editor?.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));
};

describe('RichTextEditor', () => {
  let container;
  let root;

  beforeEach(() => {
    container = document.createElement('div');
    document.body.appendChild(container);
    document.queryCommandState = vi.fn((command) => {
      const element = selectedElement();
      if (command === 'bold') return Boolean(element?.closest('strong, b'));
      return false;
    });
    document.execCommand = vi.fn(() => true);
  });

  afterEach(async () => {
    await act(async () => root?.unmount());
    container.remove();
    vi.restoreAllMocks();
  });

  it('marks H2 as active when the selection is inside a heading', async () => {
    await act(async () => {
      root = createRoot(container);
      root.render(
        <AppDialogProvider>
          <RichTextEditor
            helperText=""
            onChange={() => {}}
            value="<h2>Tiêu đề</h2><p><strong>Nội dung đậm</strong> bình thường</p>"
          />
        </AppDialogProvider>,
      );
    });

    await act(async () => selectText(container.querySelector('h2')));
    expect(container.querySelector('[aria-label="Tiêu đề lớn"]').getAttribute('aria-pressed')).toBe('true');
    expect(container.querySelector('[aria-label="Đoạn văn"]').getAttribute('aria-pressed')).toBe('false');

  });

  it('marks Bold as active when the selection is inside bold content', async () => {
    await act(async () => {
      root = createRoot(container);
      root.render(
        <AppDialogProvider>
          <RichTextEditor
            helperText=""
            onChange={() => {}}
            value="<p><strong>Nội dung đậm</strong> bình thường</p>"
          />
        </AppDialogProvider>,
      );
    });

    await act(async () => selectText(container.querySelector('[contenteditable] strong')));
    expect(container.querySelector('[aria-label="In đậm"]').getAttribute('aria-pressed')).toBe('true');
    expect(container.querySelector('[aria-label="Đoạn văn"]').getAttribute('aria-pressed')).toBe('true');
  });

  it('restores the editor selection before running a toolbar command', async () => {
    await act(async () => {
      root = createRoot(container);
      root.render(
        <AppDialogProvider>
          <RichTextEditor helperText="" onChange={() => {}} value="<p>Nội dung</p>" />
        </AppDialogProvider>,
      );
    });

    const paragraph = container.querySelector('[contenteditable] p');
    await act(async () => selectText(paragraph));

    await act(async () => {
      container.querySelector('[aria-label="Tiêu đề lớn"]').click();
    });

    expect(document.execCommand).toHaveBeenCalledWith('formatBlock', false, 'h2');
    expect(window.getSelection().toString()).toBe('Nội dung');
  });
});
