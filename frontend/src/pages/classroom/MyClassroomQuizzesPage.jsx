import { useEffect, useMemo, useState } from 'react';
import { CheckCircle2, FileQuestion, RefreshCw, Send } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import LearnerPageShell from '../../components/learner/LearnerPageShell';
import BrandLoadingState from '../../components/ui/BrandLoadingState';

export default function MyClassroomQuizzesPage() {
  const [quizzes, setQuizzes] = useState([]);
  const [answers, setAnswers] = useState({});
  const [loading, setLoading] = useState(true);
  const [submittingId, setSubmittingId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadQuizzes = async () => {
    setLoading(true);
    setError('');
    try {
      setQuizzes(await classroomApi.listStudentQuizzes());
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được bài kiểm tra lớp học.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadQuizzes();
  }, []);

  const pendingCount = useMemo(() => quizzes.filter((quiz) => !quiz.submitted).length, [quizzes]);

  const updateAnswer = (quizId, questionId, value) => {
    setAnswers((current) => ({
      ...current,
      [quizId]: {
        ...(current[quizId] || {}),
        [questionId]: value,
      },
    }));
  };

  const submitQuiz = async (quiz) => {
    setError('');
    setSuccess('');
    const quizAnswers = answers[quiz.id] || {};
    const missingQuestion = (quiz.questions || []).find((question) => !quizAnswers[question.id]);
    if (missingQuestion) {
      setError('Vui lòng trả lời đầy đủ tất cả câu hỏi trước khi nộp.');
      return;
    }
    setSubmittingId(quiz.id);
    try {
      await classroomApi.submitStudentQuiz(quiz.id, JSON.stringify(quizAnswers));
      setSuccess('Đã nộp bài kiểm tra. Điểm số đã được cập nhật vào bảng điểm lớp.');
      await loadQuizzes();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không nộp được bài kiểm tra.');
    } finally {
      setSubmittingId(null);
    }
  };

  return (
    <LearnerPageShell
      title="Bài kiểm tra lớp học"
      description="Làm các bài quiz đang mở từ giáo viên và theo dõi kết quả ngay sau khi nộp."
    >
      <div className="space-y-6">
        <section className="overflow-hidden rounded-[28px] border border-[#dfbfbd]/25 bg-gradient-to-br from-[#4b0009] via-[#730014] to-[#a6122a] p-6 text-white shadow-xl">
          <div className="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
            <div>
              <div className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-3 py-1 text-xs font-extrabold uppercase tracking-[0.16em] text-white/80">
                <FileQuestion className="h-4 w-4" />
                Classroom quiz
              </div>
              <h2 className="mt-5 font-['Manrope'] text-3xl font-black tracking-tight">Kiểm tra nhanh sau buổi học</h2>
              <p className="mt-3 max-w-2xl text-sm leading-7 text-white/75">Hoàn thành quiz trước hạn để giáo viên nắm được điểm mạnh/yếu của lớp.</p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/10 p-5 text-center">
              <p className="font-['Manrope'] text-4xl font-black">{pendingCount}</p>
              <p className="mt-1 text-xs font-bold uppercase tracking-[0.16em] text-white/60">bài cần làm</p>
            </div>
          </div>
        </section>

        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="space-y-2">
            {error ? <Notice tone="error">{error}</Notice> : null}
            {success ? <Notice tone="success">{success}</Notice> : null}
          </div>
          <button className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm font-extrabold text-[#730014]" onClick={loadQuizzes} type="button">
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </button>
        </div>

        {loading ? (
          <BrandLoadingState className="rounded-[28px]" message="Đang tải quiz..." />
        ) : !quizzes.length ? (
          <section className="flex min-h-[420px] flex-col items-center justify-center rounded-[28px] border border-dashed border-[#dfbfbd] bg-white px-6 text-center">
            <CheckCircle2 className="h-14 w-14 text-emerald-600" />
            <h3 className="mt-4 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Chưa có quiz đang mở</h3>
            <p className="mt-2 max-w-xl text-sm leading-7 text-[#584140]">Khi giáo viên mở bài kiểm tra mới, bài sẽ xuất hiện tại đây.</p>
          </section>
        ) : (
          <section className="grid gap-6">
            {quizzes.map((quiz) => (
              <QuizCard
                answers={answers[quiz.id] || {}}
                key={quiz.id}
                onAnswer={(questionId, value) => updateAnswer(quiz.id, questionId, value)}
                onSubmit={() => submitQuiz(quiz)}
                quiz={quiz}
                submitting={submittingId === quiz.id}
              />
            ))}
          </section>
        )}
      </div>
    </LearnerPageShell>
  );
}

function QuizCard({ answers, onAnswer, onSubmit, quiz, submitting }) {
  const overdue = quiz.dueAt && new Date(quiz.dueAt).getTime() < Date.now();
  return (
    <article className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-6 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <span className={`rounded-full px-3 py-1 text-xs font-extrabold ${quiz.submitted ? 'bg-emerald-100 text-emerald-700' : overdue ? 'bg-rose-100 text-rose-700' : 'bg-[#fff1f3] text-[#730014]'}`}>
            {quiz.submitted ? 'Đã nộp' : overdue ? 'Quá hạn' : 'Đang mở'}
          </span>
          <h3 className="mt-3 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{quiz.title}</h3>
          <p className="mt-2 text-sm leading-7 text-[#584140]">{quiz.description || 'Làm bài và nộp để nhận điểm ngay.'}</p>
        </div>
        <div className="rounded-2xl bg-[#fcf8f8] px-4 py-3 text-sm text-[#584140]">
          <p><strong>{quiz.questions?.length || 0}</strong> câu hỏi</p>
          <p>Hạn: <strong>{quiz.dueAt ? new Date(quiz.dueAt).toLocaleString('vi-VN') : 'Không giới hạn'}</strong></p>
          {quiz.submitted ? <p>Điểm: <strong>{quiz.myScore ?? '—'}</strong></p> : null}
        </div>
      </div>

      {!quiz.submitted ? (
        <div className="mt-6 space-y-5">
          {(quiz.questions || []).map((question, index) => (
            <div className="rounded-2xl border border-[#f0e4e2] bg-[#fffafb] p-4" key={question.id}>
              <p className="font-bold text-[#2b2828]">Câu {index + 1}. {question.prompt}</p>
              <div className="mt-3 grid gap-2 md:grid-cols-2">
                {parseOptions(question.optionsJson).map((option) => (
                  <label className={`flex cursor-pointer items-center gap-3 rounded-2xl border px-4 py-3 text-sm font-semibold transition ${answers[question.id] === option ? 'border-[#730014] bg-[#fff1f3] text-[#4b0009]' : 'border-[#dfbfbd]/25 bg-white text-[#584140]'}`} key={option}>
                    <input
                      checked={answers[question.id] === option}
                      className="accent-[#4b0009]"
                      disabled={overdue}
                      onChange={() => onAnswer(question.id, option)}
                      type="radio"
                    />
                    {option}
                  </label>
                ))}
              </div>
            </div>
          ))}
          <button className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white disabled:cursor-not-allowed disabled:opacity-60" disabled={submitting || overdue} onClick={onSubmit} type="button">
            <Send className="h-4 w-4" />
            {overdue ? 'Đã quá hạn' : submitting ? 'Đang nộp...' : 'Nộp bài'}
          </button>
        </div>
      ) : null}
    </article>
  );
}

function Notice({ children, tone }) {
  const className = tone === 'error'
    ? 'border-rose-200 bg-rose-50 text-rose-700'
    : 'border-emerald-200 bg-emerald-50 text-emerald-700';
  return <div className={`rounded-2xl border px-5 py-3 text-sm font-bold ${className}`}>{children}</div>;
}

function parseOptions(optionsJson) {
  try {
    const parsed = JSON.parse(optionsJson || '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}
