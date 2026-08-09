export const requestExamFullscreen = async () => {
  if (document.fullscreenElement) return true;
  if (!document.documentElement?.requestFullscreen) return false;
  try {
    await document.documentElement.requestFullscreen();
    return Boolean(document.fullscreenElement);
  } catch {
    return false;
  }
};

export const exitExamFullscreen = async () => {
  if (!document.fullscreenElement || !document.exitFullscreen) return;
  try {
    await document.exitFullscreen();
  } catch {
    // The browser may already be leaving fullscreen during navigation.
  }
};

export const exitExamFullscreenWhenDetached = (rootRef) => {
  window.setTimeout(() => {
    // React Strict Mode runs an effect cleanup immediately after mounting in
    // development. The exam root is still connected during that check.
    if (!rootRef?.current?.isConnected) {
      void exitExamFullscreen();
    }
  }, 0);
};
