const FALLBACK_MESSAGE = 'Hiện chưa thể hoàn tất việc gửi bài. Vui lòng thử lại.';

export const getAssessmentSubmissionErrorMessage = (error) => {
  const responseMessage = error?.response?.data?.message;
  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return responseMessage.trim();
  }
  if (typeof error?.message === 'string' && error.message.trim()) {
    return error.message.trim();
  }
  return FALLBACK_MESSAGE;
};

export const isTemporaryAssessmentSubmissionError = (error) => {
  const status = Number(error?.response?.status || 0);
  if (status) {
    return status === 408 || status === 425 || status === 429 || status >= 500;
  }

  // Axios has no response for connection refusal, DNS failure and a request
  // timeout. Those are safe to queue because the server did not reject the
  // submitted content.
  return !error?.response;
};
