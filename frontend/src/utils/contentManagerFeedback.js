const DEFAULT_ERROR_CODE = 'CONTENT_MANAGER_ERROR';

export const createContentManagerError = (message, code = 'VALIDATION_ERROR') => ({
  message,
  code,
});

export const getContentManagerError = (error, fallback, fallbackCode = DEFAULT_ERROR_CODE) => {
  const response = error?.response;
  const data = response?.data;
  const status = response?.status;

  if (!response && error?.code === 'ECONNABORTED') {
    return createContentManagerError('Yêu cầu mất quá nhiều thời gian. Vui lòng thử lại.', 'REQUEST_TIMEOUT');
  }
  if (!response && error?.request) {
    return createContentManagerError(fallback, 'NETWORK_ERROR');
  }

  if (!response) {
    return createContentManagerError(error?.message || fallback, fallbackCode);
  }

  return createContentManagerError(
    data?.message || fallback,
    data?.code || (status ? `HTTP_${status}` : fallbackCode),
  );
};

export const getContentManagerFeedbackMessage = (feedback) => (
  typeof feedback === 'object' ? feedback?.message || '' : feedback || ''
);
