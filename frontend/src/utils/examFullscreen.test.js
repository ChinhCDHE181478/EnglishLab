import { afterEach, describe, expect, it, vi } from 'vitest';
import { exitExamFullscreenWhenDetached } from './examFullscreen';

const waitForDeferredExit = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('exam fullscreen cleanup', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    delete globalThis.document;
    delete globalThis.window;
  });

  it('keeps fullscreen during the Strict Mode cleanup check', async () => {
    const exitFullscreen = vi.fn().mockResolvedValue(undefined);
    globalThis.document = { fullscreenElement: {}, exitFullscreen };
    globalThis.window = { setTimeout };

    exitExamFullscreenWhenDetached({ current: { isConnected: true } });
    await waitForDeferredExit();

    expect(exitFullscreen).not.toHaveBeenCalled();
  });

  it('exits fullscreen after the exam root is actually detached', async () => {
    const exitFullscreen = vi.fn().mockResolvedValue(undefined);
    globalThis.document = { fullscreenElement: {}, exitFullscreen };
    globalThis.window = { setTimeout };

    exitExamFullscreenWhenDetached({ current: null });
    await waitForDeferredExit();

    expect(exitFullscreen).toHaveBeenCalledOnce();
  });
});
