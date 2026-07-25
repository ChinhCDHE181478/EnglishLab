import { useEffect, useState } from 'react';
import { CheckCircle2, FileQuestion, Plus, RefreshCw, Trash2, XCircle } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import VietnameseDateTimeInput from '../ui/VietnameseDateTimeInput';

const emptyQuestion = { prompt: '', options: ['A', 'B', 'C', 'D'], correctAnswer: '', explanation: '' };
const emptyForm = {
  title: '',
  description: '',
  timeLimitMinutes: 15,
  passingScore: 50,
  dueAt: '',
  questions: [{ ...emptyQuestion }],
};

export default function TeacherQuizSection({ classroomId }) {
  const [quizzes, setQuizzes] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(true);
  const [workingId, setWorkingId] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadQuizzes = async () => {
    setLoading(true);
    setError('');
    try {
      setQuizzes(await classroomApi.listTeacherQuizzes(classroomId));
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được bài kiểm tra.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadQuizzes();
  }, [classroomId]);

  const updateQuestion = (index, patch) => {
    setForm((current) => ({
      ...current,
      questions: current.questions.map((question, questionIndex) => (
        questionIndex === index ? { ...question, ...patch } : question
      )),
    }));
  };

  const updateOption = (questionIndex, optionIndex, value) => {
    const question = form.questions[questionIndex];
    const options = question.options.map((option, index) => (index === optionIndex ? value : option));
    updateQuestion(questionIndex, { options });
  };

  const addQuestion = () => {
    setForm((current) => ({ ...current, questions: [...current.questions, { ...emptyQuestion, options: [...emptyQuestion.options] }] }));
  };

  const removeQuestion = (index) => {
    setForm((current) => ({
      ...current,
      questions: current.questions.length <= 1 ? current.questions : current.questions.filter((_, questionIndex) => questionIndex !== index),
    }));
  };

  const createQuiz = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    if (!form.title.trim()) {
      setError('Vui lòng nhập tiêu đề bài kiểm tra.');
      return;
    }
    const invalidQuestion = form.questions.find((question) => (
      !question.prompt.trim()
      || !question.correctAnswer.trim()
      || question.options.filter((option) => option.trim()).length < 2
    ));
    if (invalidQuestion) {
      setError('Mỗi câu hỏi cần nội dung, ít nhất 2 lựa chọn và đáp án đúng.');
      return;
    }
    setSubmitting(true);
    try {
      await classroomApi.createTeacherQuiz(classroomId, {
        title: form.title.trim(),
        description: form.description,
        timeLimitMinutes: Number(form.timeLimitMinutes || 0) || null,
        passingScore: Number(form.passingScore || 50),
        dueAt: form.dueAt ? new Date(form.dueAt).toISOString() : null,
        questions: form.questions.map((question, index) => ({
          sortOrder: index,
          prompt: question.prompt.trim(),
          optionsJson: JSON.stringify(question.options.filter((option) => option.trim())),
          correctAnswer: question.correctAnswer.trim(),
          explanation: question.explanation,
        })),
      });
      setForm(emptyForm);
      setShowForm(false);
      setSuccess('Đã tạo bài kiểm tra. Bạn có thể mở bài khi sẵn sàng.');
      await loadQuizzes();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tạo được bài kiểm tra.');
    } finally {
      setSubmitting(false);
    }
  };

  const changeStatus = async (quiz, action) => {
    setWorkingId(quiz.id);
    setError('');
    setSuccess('');
    try {
      if (action === 'open') {
        await classroomApi.openTeacherQuiz(quiz.id);
        setSuccess('Đã mở bài kiểm tra cho học viên.');
      } else if (action === 'close') {
        await classroomApi.closeTeacherQuiz(quiz.id);
        setSuccess('Đã đóng bài kiểm tra.');
      } else {
        await classroomApi.deleteTeacherQuiz(quiz.id);
        setSuccess('Đã xóa bài kiểm tra.');
      }
      await loadQuizzes();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không cập nhật được bài kiểm tra.');
    } finally {
      setWorkingId(null);
    }
  };

  return (
    <section className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-[#dfbfbd]/25 bg-white p-5 shadow-sm">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#730014]">Quiz operations</p>
          <h3 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Bài kiểm tra lớp học</h3>
          <p className="mt-1 text-sm text-[#584140]">Tạo quiz nhanh, mở/đóng theo tiến độ lớp và điểm được đồng bộ vào gradebook.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-4 py-3 text-sm font-extrabold text-[#730014]" onClick={loadQuizzes} type="button">
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </button>
          <button className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-4 py-3 text-sm font-extrabold text-white" onClick={() => setShowForm((value) => !value)} type="button">
            <Plus className="h-4 w-4" />
            {showForm ? 'Ẩn form' : 'Tạo quiz'}
          </button>
        </div>
      </div>

      {error ? <Notice tone="error">{error}</Notice> : null}
      {success ? <Notice tone="success">{success}</Notice> : null}

      {showForm ? (
        <form className="space-y-5 rounded-2xl border border-[#dfbfbd]/25 bg-white p-5 shadow-sm" onSubmit={createQuiz}>
          <div className="grid gap-4 md:grid-cols-2">
            <Field label="Tiêu đề">
              <input className={inputClass} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} value={form.title} />
            </Field>
            <Field label="Hạn nộp">
              <VietnameseDateTimeInput className={inputClass} onChange={(value) => setForm((current) => ({ ...current, dueAt: value }))} value={form.dueAt} />
            </Field>
            <Field label="Thời lượng (phút)">
              <input className={inputClass} min="1" onChange={(event) => setForm((current) => ({ ...current, timeLimitMinutes: event.target.value }))} type="number" value={form.timeLimitMinutes} />
            </Field>
            <Field label="Điểm đạt (%)">
              <input className={inputClass} min="1" max="100" onChange={(event) => setForm((current) => ({ ...current, passingScore: event.target.value }))} type="number" value={form.passingScore} />
            </Field>
          </div>
          <Field label="Mô tả">
            <textarea className={`${inputClass} min-h-24`} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} value={form.description} />
          </Field>

          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h4 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Câu hỏi</h4>
              <button className="rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-bold text-[#730014]" onClick={addQuestion} type="button">Thêm câu</button>
            </div>
            {form.questions.map((question, questionIndex) => (
              <div className="rounded-2xl border border-[#f0e4e2] bg-[#fffafb] p-4" key={questionIndex}>
                <div className="flex items-center justify-between gap-3">
                  <p className="font-bold text-[#730014]">Câu {questionIndex + 1}</p>
                  <button className="text-rose-600" onClick={() => removeQuestion(questionIndex)} type="button"><Trash2 className="h-4 w-4" /></button>
                </div>
                <div className="mt-3 space-y-3">
                  <input className={inputClass} onChange={(event) => updateQuestion(questionIndex, { prompt: event.target.value })} placeholder="Nội dung câu hỏi" value={question.prompt} />
                  <div className="grid gap-2 md:grid-cols-2">
                    {question.options.map((option, optionIndex) => (
                      <input className={inputClass} key={optionIndex} onChange={(event) => updateOption(questionIndex, optionIndex, event.target.value)} placeholder={`Lựa chọn ${optionIndex + 1}`} value={option} />
                    ))}
                  </div>
                  <input className={inputClass} onChange={(event) => updateQuestion(questionIndex, { correctAnswer: event.target.value })} placeholder="Đáp án đúng (nhập đúng nội dung lựa chọn)" value={question.correctAnswer} />
                  <input className={inputClass} onChange={(event) => updateQuestion(questionIndex, { explanation: event.target.value })} placeholder="Giải thích sau khi chấm (tuỳ chọn)" value={question.explanation} />
                </div>
              </div>
            ))}
          </div>

          <button className="rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white disabled:opacity-60" disabled={submitting} type="submit">
            {submitting ? 'Đang tạo...' : 'Lưu bài kiểm tra'}
          </button>
        </form>
      ) : null}

      <div className="grid gap-4">
        {loading ? (
          <div className="rounded-2xl border border-[#dfbfbd]/25 bg-white px-5 py-12 text-center text-sm font-bold text-[#8b706e]">Đang tải bài kiểm tra...</div>
        ) : quizzes.length ? quizzes.map((quiz) => (
          <article className="rounded-2xl border border-[#dfbfbd]/25 bg-white p-5 shadow-sm" key={quiz.id}>
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <StatusBadge status={quiz.status} />
                <h4 className="mt-3 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{quiz.title}</h4>
                <p className="mt-1 text-sm text-[#584140]">{quiz.description || 'Chưa có mô tả.'}</p>
                <p className="mt-2 text-xs font-bold text-[#8b706e]">
                  {quiz.questions?.length || 0} câu · {quiz.timeLimitMinutes || '-'} phút · Hạn {quiz.dueAt ? new Date(quiz.dueAt).toLocaleString('vi-VN') : 'không giới hạn'}
                </p>
              </div>
              <div className="flex flex-wrap gap-2">
                {quiz.status !== 'OPEN' ? (
                  <button className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-3 text-sm font-extrabold text-white disabled:opacity-60" disabled={workingId === quiz.id} onClick={() => changeStatus(quiz, 'open')} type="button">
                    <CheckCircle2 className="h-4 w-4" />
                    Mở
                  </button>
                ) : (
                  <button className="inline-flex items-center gap-2 rounded-xl border border-amber-200 px-4 py-3 text-sm font-extrabold text-amber-700 disabled:opacity-60" disabled={workingId === quiz.id} onClick={() => changeStatus(quiz, 'close')} type="button">
                    <XCircle className="h-4 w-4" />
                    Đóng
                  </button>
                )}
                {quiz.status !== 'OPEN' ? (
                  <button className="inline-flex items-center gap-2 rounded-xl border border-rose-200 px-4 py-3 text-sm font-extrabold text-rose-700 disabled:opacity-60" disabled={workingId === quiz.id} onClick={() => changeStatus(quiz, 'delete')} type="button">
                    <Trash2 className="h-4 w-4" />
                    Xóa
                  </button>
                ) : null}
              </div>
            </div>
          </article>
        )) : (
          <div className="rounded-2xl border border-dashed border-[#dfbfbd] bg-white px-5 py-14 text-center">
            <FileQuestion className="mx-auto h-12 w-12 text-[#730014]" />
            <h4 className="mt-3 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">Chưa có quiz nào</h4>
            <p className="mt-2 text-sm text-[#584140]">Tạo quiz đầu tiên để kiểm tra nhanh mức độ hiểu bài của lớp.</p>
          </div>
        )}
      </div>
    </section>
  );
}

function StatusBadge({ status }) {
  const meta = {
    DRAFT: 'bg-slate-100 text-slate-600',
    OPEN: 'bg-emerald-100 text-emerald-700',
    CLOSED: 'bg-rose-100 text-rose-700',
  };
  const label = { DRAFT: 'Bản nháp', OPEN: 'Đang mở', CLOSED: 'Đã đóng' }[status] || status;
  return <span className={`rounded-full px-3 py-1 text-xs font-extrabold ${meta[status] || 'bg-slate-100 text-slate-600'}`}>{label}</span>;
}

function Field({ children, label }) {
  return (
    <label className="block space-y-2">
      <span className="text-xs font-bold uppercase tracking-wider text-[#8b706e]">{label}</span>
      {children}
    </label>
  );
}

function Notice({ children, tone }) {
  const className = tone === 'error'
    ? 'border-rose-200 bg-rose-50 text-rose-700'
    : 'border-emerald-200 bg-emerald-50 text-emerald-700';
  return <div className={`rounded-2xl border px-5 py-3 text-sm font-bold ${className}`}>{children}</div>;
}

const inputClass = 'w-full rounded-2xl border border-[#dfbfbd]/60 bg-white px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014]';
