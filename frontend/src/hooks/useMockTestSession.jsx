import { useState } from 'react';
import ListeningExamMode from '../components/course-assessment/ListeningExamMode';
import ReadingExamMode from '../components/course-assessment/ReadingExamMode';
import ToeicExamMode from '../components/course-assessment/ToeicExamMode';
import WritingExamMode from '../components/course-assessment/WritingExamMode';
import SpeakingExamMode from '../components/course-assessment/SpeakingExamMode';
import mockTestApi from '../api/mockTestApi';
import placementTestApi from '../api/placementTestApi';
import { exitExamFullscreen, requestExamFullscreen } from '../utils/examFullscreen';
import { isToeicExamConfig, parseJson, resolveMockConfig } from '../utils/mockTestExam';

const COMPLETED_SCORES_KEY = 'englishlab_mock_completed_scores_v4';

function readScores() {
  try {
    const saved = localStorage.getItem(COMPLETED_SCORES_KEY);
    return saved ? JSON.parse(saved) : {};
  } catch {
    return {};
  }
}

export default function useMockTestSession() {
  const [activeTest, setActiveTest] = useState(null);
  const [activeConfig, setActiveConfig] = useState(null);
  const [activeSkill, setActiveSkill] = useState(null);
  const [result, setResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [completedScoresMap, setCompletedScoresMap] = useState(readScores);

  const saveScoreResult = (testId, resultData) => {
    setCompletedScoresMap((prev) => {
      const updated = { ...prev, [testId]: resultData };
      try {
        localStorage.setItem(COMPLETED_SCORES_KEY, JSON.stringify(updated));
      } catch {
        /* ignore quota */
      }
      return updated;
    });
  };

  const closeExam = async () => {
    setActiveTest(null);
    setActiveConfig(null);
    setActiveSkill(null);
    setSubmitting(false);
    await exitExamFullscreen();
  };

  const startTest = async (item) => {
    if (!item?.id) return;
    setError('');
    setResult(null);
    const fullscreenStarted = await requestExamFullscreen();
    if (!fullscreenStarted) {
      setError('Không thể bật chế độ toàn màn hình. Hãy cho phép trình duyệt mở toàn màn hình rồi thử lại.');
      return;
    }
    try {
      const detail = await mockTestApi.getMockTest(item.id);
      const resolved = resolveMockConfig(detail, parseJson(detail.uiConfigJson));
      setActiveTest(detail);
      setActiveConfig(resolved.config);
      setActiveSkill(resolved.skill);
    } catch (requestError) {
      if (fullscreenStarted) await exitExamFullscreen();
      setError(requestError?.response?.data?.message || 'Không mở được đề thi thử.');
    }
  };

  const handleObjectiveSubmit = async (payload) => {
    setSubmitting(true);
    setError('');
    try {
      const savedAttempt = await mockTestApi.submitMockTest(activeTest.id, {
        objectiveAnswersJson: payload.objectiveAnswersJson,
      });
      const resObj = {
        title: savedAttempt.mockTestTitle || activeTest?.title,
        skill: savedAttempt.skill || activeSkill,
        correct: savedAttempt.correctCount,
        total: savedAttempt.totalQuestions,
        percent: savedAttempt.percent,
        score: savedAttempt.score,
        submittedAt: savedAttempt.submittedAt || new Date().toISOString(),
      };
      setResult(resObj);
      if (activeTest?.id) saveScoreResult(activeTest.id, resObj);
      await closeExam();
    } catch (requestError) {
      setError(requestError?.response?.data?.message || 'Chưa thể nộp bài thi thử.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubjectiveSubmit = async (_assessmentId, payload = {}) => {
    setSubmitting(true);
    setError('');
    try {
      const savedAttempt = await mockTestApi.submitMockTest(activeTest.id, {
        objectiveAnswersJson: payload.objectiveAnswersJson,
        submittedText: payload.submittedText || '',
        submittedAudioUrl: payload.submittedAudioUrl || '',
      });
      const resObj = {
        title: savedAttempt.mockTestTitle || activeTest?.title,
        skill: savedAttempt.skill || activeSkill,
        submittedText: savedAttempt.submittedText || payload.submittedText || '',
        submittedAudioUrl: savedAttempt.submittedAudioUrl || payload.submittedAudioUrl || '',
        submittedAt: savedAttempt.submittedAt || new Date().toISOString(),
        score: savedAttempt.score ?? null,
        aiFeedbackJson: savedAttempt.aiFeedbackJson || '',
        status: savedAttempt.status,
        message: savedAttempt.status === 'FAILED'
          ? 'Chưa thể chấm bài tự động. Vui lòng thử lại sau.'
          : 'Bài thi đã được AI chấm và lưu kết quả.',
      };
      setResult(resObj);
      if (activeTest?.id) saveScoreResult(activeTest.id, resObj);
      await closeExam();
      return savedAttempt;
    } catch (requestError) {
      setError(requestError?.response?.data?.message || 'Chưa thể nộp bài thi thử.');
      throw requestError;
    } finally {
      setSubmitting(false);
    }
  };

  let examView = null;
  const useToeicUi = activeTest && activeConfig && isToeicExamConfig(activeConfig, activeTest);
  if (activeTest && activeConfig && useToeicUi && (activeSkill === 'LISTENING' || activeSkill === 'READING')) {
    examView = (
      <ToeicExamMode
        assessment={{ title: activeTest.title, timeLimitMinutes: activeTest.timeLimitMinutes || activeConfig.durationMinutes }}
        config={activeConfig}
        onClose={closeExam}
        onSubmit={handleObjectiveSubmit}
        skillLabel={activeSkill === 'READING' ? 'TOEIC Reading' : 'TOEIC Listening'}
        submitLabel={activeSkill === 'READING' ? 'Nộp bài Reading' : 'Nộp bài Listening'}
        submitting={submitting}
      />
    );
  } else if (activeTest && activeConfig && activeSkill === 'LISTENING') {
    examView = (
      <ListeningExamMode
        assessment={{ title: activeTest.title, timeLimitMinutes: activeTest.timeLimitMinutes || activeConfig.durationMinutes }}
        config={activeConfig}
        onClose={closeExam}
        onSubmit={handleObjectiveSubmit}
        submitLabel="Nộp bài Listening"
        submitting={submitting}
      />
    );
  } else if (activeTest && activeConfig && activeSkill === 'READING') {
    examView = (
      <ReadingExamMode
        assessment={{ title: activeTest.title, timeLimitMinutes: activeTest.timeLimitMinutes || activeConfig.durationMinutes }}
        config={activeConfig}
        onClose={closeExam}
        onSubmit={handleObjectiveSubmit}
        submitLabel="Nộp bài Reading"
        submitting={submitting}
      />
    );
  } else if (activeTest && activeConfig && activeSkill === 'WRITING') {
    examView = (
      <WritingExamMode
        assessment={{ title: activeTest.title, timeLimitMinutes: activeTest.timeLimitMinutes || activeConfig.durationMinutes }}
        config={activeConfig}
        onClose={closeExam}
        onSubmit={handleSubjectiveSubmit}
        submitLabel="Nộp bài Writing"
        submitting={submitting}
      />
    );
  } else if (activeTest && activeConfig && activeSkill === 'SPEAKING') {
    examView = (
      <SpeakingExamMode
        config={{ ...activeConfig, submissionLabel: activeTest.title }}
        onClose={closeExam}
        onSubmit={(payload) => handleSubjectiveSubmit('mock-speaking', payload)}
        submitting={submitting}
        uploadAudio={placementTestApi.uploadSpeakingAudio}
      />
    );
  }

  return {
    examView,
    error,
    setError,
    result,
    setResult,
    startTest,
    completedScoresMap,
  };
}
