import { useEffect, useMemo, useRef, useState } from 'react';
import { ArrowLeft, CheckCircle2, Clock3, History, Play, RotateCcw } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import classroomApi from '../../api/classroomApi';
import { ClassroomEmptyState, ClassroomErrorState, ClassroomLoadingState } from '../../components/classroom/ClassroomUi';
import ListeningExamMode from '../../components/course-assessment/ListeningExamMode';
import ReadingExamMode from '../../components/course-assessment/ReadingExamMode';
import ToeicExamMode from '../../components/course-assessment/ToeicExamMode';
import LearnerPageShell from '../../components/learner/LearnerPageShell';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { requestExamFullscreen } from '../../utils/examFullscreen';
import { isToeicExamConfig } from '../../utils/mockTestExam';

const parseExamConfig = (instruction) => {
  try {
    const parsed = JSON.parse(instruction || '');
    return Array.isArray(parsed?.parts) && parsed.parts.length ? parsed : null;
  } catch {
    return null;
  }
};

const formatDateTime = (value) => {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('vi-VN');
};

const formatDuration = (seconds) => {
  if (seconds == null) return '—';
  const minutes = Math.floor(seconds / 60);
  return `${minutes}:${String(seconds % 60).padStart(2, '0')}`;
};

const countQuestions = (config) => (config?.parts || []).reduce((total, part) => (
  total + (part.questionGroups || []).reduce((groupTotal, group) => (
    groupTotal + (group.questionNumbers?.length || group.questions?.length || 0)
  ), 0)
), 0);

const toAnswerMap = (objectiveAnswersJson) => {
  try {
    const payload = JSON.parse(objectiveAnswersJson || '{}');
    return Object.fromEntries((payload.responses || []).map((response) => [
      String(response.questionNumber),
      response.answer,
    ]));
  } catch {
    return {};
  }
};

export default function PracticeRunnerPage() {
  const { classroomId, exerciseId } = useParams();
  const [practice, setPractice] = useState(null);
  const [attempts, setAttempts] = useState([]);
  const [latestResult, setLatestResult] = useState(null);
  const [examOpen, setExamOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const startedAtRef = useRef(null);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [practiceItems, attemptItems] = await Promise.all([
        classroomApi.getClassroomPractice(classroomId),
        classroomApi.getClassroomPracticeAttempts(classroomId, exerciseId),
      ]);
      setPractice(practiceItems.find((item) => String(item.exerciseId) === String(exerciseId)) || null);
      setAttempts(attemptItems);
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể mở bài luyện tập.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [classroomId, exerciseId]);

  const examConfig = useMemo(() => parseExamConfig(practice?.instruction), [practice?.instruction]);
  const questionCount = countQuestions(examConfig);
  const isListening = String(practice?.skill || '').toUpperCase() === 'LISTENING'
    || examConfig?.type === 'ielts_listening_exam';

  const openExam = async () => {
    setLatestResult(null);
    setError('');
    const fullscreenStarted = await requestExamFullscreen();
    if (!fullscreenStarted) {
      setError('Không thể bật chế độ toàn màn hình. Hãy cho phép trình duyệt mở toàn màn hình rồi thử lại.');
      return;
    }
    startedAtRef.current = new Date();
    setExamOpen(true);
  };

  const submitExam = async (examPayload) => {
    setSubmitting(true);
    setError('');
    try {
      const objectivePayload = JSON.parse(examPayload.objectiveAnswersJson || '{}');
      const durationSeconds = Math.max(
        0,
        Number(examConfig?.durationMinutes || 10) * 60 - Number(objectivePayload.remainingSeconds || 0),
      );
      const result = await classroomApi.submitClassroomPracticeAttempt(classroomId, exerciseId, {
        answersJson: JSON.stringify(toAnswerMap(examPayload.objectiveAnswersJson)),
        durationSeconds,
        startedAt: startedAtRef.current?.toISOString() || new Date().toISOString(),
      });
      setLatestResult(result);
      setAttempts((current) => [result, ...current]);
      setExamOpen(false);
      return result;
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể nộp lượt luyện tập.'));
      throw err;
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <LearnerPageShell title="Đang mở bài luyện tập"><ClassroomLoadingState label="Đang tải đề và lịch sử làm bài..." /></LearnerPageShell>;
  }
  if (error && !practice) {
    return <LearnerPageShell title="Không thể mở bài"><ClassroomErrorState message={error} onRetry={load} /></LearnerPageShell>;
  }
  if (!practice) {
    return <LearnerPageShell title="Không tìm thấy bài luyện tập"><ClassroomEmptyState actionLabel="Về danh sách luyện tập" actionTo="/my-practice" description="Bài này không còn thuộc giáo trình của lớp hoặc bạn không có quyền truy cập." title="Không tìm thấy bài" /></LearnerPageShell>;
  }

  const useToeicUi = isToeicExamConfig(examConfig);
  const ExamMode = useToeicUi ? ToeicExamMode : (isListening ? ListeningExamMode : ReadingExamMode);
  const assessment = {
    id: practice.exerciseId,
    title: practice.title,
    skill: practice.skill,
    timeLimitMinutes: examConfig?.durationMinutes || 10,
  };

  return (
    <LearnerPageShell
      actions={<Link className="inline-flex items-center gap-2 rounded-full border border-[#dfbfbd] bg-white px-5 py-2.5 text-sm font-extrabold text-[#730014]" to="/my-practice"><ArrowLeft className="h-4 w-4" />Danh sách luyện tập</Link>}
      description={`${practice.classroomTitle} · Unit ${practice.unitDisplayOrder}: ${practice.unitTitle}`}
      eyebrow="Luyện tập theo giáo trình"
      title={practice.title}
    >
      <div className="grid flex-1 gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <main className="space-y-5">
          {examConfig ? (
            <section className="overflow-hidden rounded-[30px] border border-[#ead9db] bg-white shadow-sm">
              <div className="bg-[radial-gradient(circle_at_top_right,_rgba(115,0,20,0.12),_transparent_42%),linear-gradient(135deg,#fffdfd,#fff7f8)] p-8">
                <p className="text-xs font-black uppercase tracking-[0.18em] text-[#730014]">Bài luyện tập</p>
                <h2 className="mt-3 font-['Manrope'] text-3xl font-black text-[#1a1c1c]">{examConfig.title || practice.title}</h2>
                <p className="mt-3 max-w-3xl text-sm leading-7 text-[#6f5b59]">Bài luyện tập mở trong giao diện đề thi: có đồng hồ, điều hướng phần/câu, theo dõi câu đã làm và nộp kết quả trực tiếp trên hệ thống.</p>
                <div className="mt-6 flex flex-wrap gap-3 text-xs font-extrabold text-[#584140]">
                  <span className="rounded-full border border-[#dfbfbd] bg-white px-4 py-2">{questionCount} câu hỏi</span>
                  <span className="rounded-full border border-[#dfbfbd] bg-white px-4 py-2">{examConfig.durationMinutes || 10} phút</span>
                  <span className="rounded-full border border-[#dfbfbd] bg-white px-4 py-2">{isListening ? 'Listening' : 'Reading'} test mode</span>
                </div>
                <button className="mt-8 inline-flex items-center gap-2 rounded-2xl bg-[#730014] px-7 py-4 text-sm font-black text-white shadow-[0_16px_34px_rgba(115,0,20,0.22)] transition hover:-translate-y-0.5" onClick={openExam} type="button"><Play className="h-5 w-5 fill-current" />{attempts.length ? 'Làm một lượt mới' : 'Vào chế độ làm bài'}</button>
              </div>
              {(examConfig.rules || []).length ? <div className="border-t border-[#ead9db] p-6"><h3 className="font-black text-[#1a1c1c]">Quy tắc làm bài</h3><ul className="mt-3 space-y-2 text-sm leading-6 text-[#584140]">{examConfig.rules.map((rule) => <li key={rule}>• {rule}</li>)}</ul></div> : null}
            </section>
          ) : (
            <section className="rounded-[28px] border border-amber-200 bg-amber-50 p-7">
              <h2 className="text-lg font-black text-amber-900">Bài luyện tập chưa được biên soạn theo định dạng hệ thống</h2>
              <p className="mt-3 text-sm leading-7 text-amber-800">Bài luyện tập này chưa có cấu trúc câu hỏi và đáp án hợp lệ. Vui lòng quay lại sau khi nội dung được cập nhật.</p>
            </section>
          )}

          {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-700">{error}</div> : null}
          {latestResult ? (
            <section className="rounded-[28px] border border-emerald-200 bg-emerald-50 p-6">
              <div className="flex items-center gap-3"><CheckCircle2 className="h-6 w-6 text-emerald-700" /><h2 className="text-lg font-black text-emerald-900">Hoàn thành lượt #{latestResult.attemptNumber}</h2></div>
              <p className="mt-3 text-sm font-bold text-emerald-800">Kết quả: {latestResult.correctAnswers}/{latestResult.totalQuestions} câu đúng · {Math.round(latestResult.scorePercent || 0)}%</p>
              {latestResult.explanation ? <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-emerald-900">{latestResult.explanation}</p> : null}
              <button className="mt-5 inline-flex items-center gap-2 rounded-xl border border-emerald-300 bg-white px-5 py-3 text-sm font-extrabold text-emerald-800" onClick={openExam} type="button"><RotateCcw className="h-4 w-4" />Luyện lại</button>
            </section>
          ) : null}
        </main>

        <aside className="min-h-0 rounded-[28px] border border-[#ead9db] bg-white p-5 shadow-sm xl:sticky xl:top-24 xl:max-h-[calc(100dvh-7rem)] xl:overflow-y-auto">
          <div className="flex items-center gap-2 border-b border-[#f0e4e5] pb-4"><History className="h-5 w-5 text-[#730014]" /><h2 className="font-black text-[#1a1c1c]">Lịch sử làm bài</h2></div>
          {attempts.length ? <div className="mt-4 space-y-3">{attempts.map((attempt) => (
            <article className="rounded-2xl border border-gray-200 p-4" key={attempt.id}>
              <div className="flex items-center justify-between gap-3"><p className="text-sm font-extrabold text-[#1a1c1c]">Lượt #{attempt.attemptNumber}</p>{attempt.scorePercent != null ? <span className="rounded-full bg-[#fff0f1] px-2.5 py-1 text-xs font-extrabold text-[#730014]">{Math.round(attempt.scorePercent)}%</span> : null}</div>
              <p className="mt-2 text-xs text-[#806765]">{formatDateTime(attempt.completedAt)}</p>
              <p className="mt-2 inline-flex items-center gap-1 text-xs font-semibold text-[#584140]"><Clock3 className="h-3.5 w-3.5" />{formatDuration(attempt.durationSeconds)}</p>
            </article>
          ))}</div> : <p className="mt-5 rounded-2xl bg-[#fffafb] p-4 text-sm leading-6 text-[#806765]">Chưa có lượt làm nào. Mỗi lần nộp sẽ được lưu riêng tại đây.</p>}
        </aside>
      </div>

      {examOpen && examConfig ? (
        <ExamMode
          assessment={assessment}
          config={examConfig}
          onClose={() => setExamOpen(false)}
          onSubmit={submitExam}
          skillLabel={isListening ? 'TOEIC Listening' : 'TOEIC Reading'}
          skipAudioCheck={isListening && !examConfig.audioUrl}
          submitLabel="Nộp lượt luyện tập"
          submitting={submitting}
        />
      ) : null}
    </LearnerPageShell>
  );
}
