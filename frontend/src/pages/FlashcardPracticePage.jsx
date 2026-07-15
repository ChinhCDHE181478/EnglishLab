import { useCallback, useEffect, useState } from 'react';
import { BookOpenCheck, RefreshCw } from 'lucide-react';
import courseApi from '../api/courseApi';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import WorkspaceFlashcards from '../components/course-workspace/WorkspaceFlashcards';
import BrandedSelect from '../components/ui/BrandedSelect';

export default function FlashcardPracticePage() {
  const [courses, setCourses] = useState([]);
  const [courseId, setCourseId] = useState('');
  const [terms, setTerms] = useState([]);
  const [loadingCourses, setLoadingCourses] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const loadTerms = useCallback(async () => {
    if (!courseId) {
      setTerms([]);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const data = await courseApi.getGlobalFlashcardPractice({ courseId });
      setTerms(data);
    } catch (requestError) {
      setTerms([]);
      setError(requestError.response?.data?.message || 'Chưa thể tải flashcard. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  }, [courseId]);

  useEffect(() => {
    const loadCourses = async () => {
      setLoadingCourses(true);
      try {
        setCourses(await courseApi.getMyOnlineCourses());
      } catch (requestError) {
        setError(requestError.response?.data?.message || 'Chưa thể tải danh sách khóa học.');
      } finally {
        setLoadingCourses(false);
      }
    };
    loadCourses();
  }, []);

  useEffect(() => {
    const fetchTerms = async () => {
      await loadTerms();
    };
    fetchTerms();
  }, [loadTerms]);

  return (
    <LearnerPageShell
      actions={<button className="inline-flex items-center gap-2 rounded-full border border-[#dfbfbd] bg-white px-5 py-2.5 text-sm font-extrabold text-[#730014] shadow-sm transition hover:bg-[#fff6f6] disabled:opacity-50" disabled={loading || !courseId} onClick={loadTerms} type="button"><RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />Làm mới</button>}
      description="Chọn khóa học để ôn flashcard đúng với nội dung bạn đang học."
      eyebrow="Flashcard practice"
      title="Luyện từ vựng"
    >
      <section className="mb-8 rounded-3xl border border-[#ead9db] bg-white p-5 shadow-sm">
        <div className="flex items-start gap-3"><span className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#f7eeee] text-[#730014]"><BookOpenCheck className="h-5 w-5" /></span><div><h2 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Chọn khóa học</h2><p className="mt-1 text-xs text-[#806765]">Flashcard sẽ được tải sau khi bạn chọn một khóa học.</p></div></div>
        <BrandedSelect
          buttonClassName="mt-4 rounded-2xl border-[#dfbfbd] bg-[#fffdfc] px-4 py-3 text-sm font-bold text-[#4b0009] shadow-none focus:border-[#8a0018]"
          disabled={loadingCourses}
          onChange={(event) => setCourseId(event.target.value)}
          options={courses.map((course) => ({ label: course.courseTitle || course.title, value: course.courseId || course.id }))}
          placeholder={loadingCourses ? 'Đang tải khóa học...' : '-- Chọn khóa học --'}
          value={courseId}
        />
      </section>

      {error ? <div className="mb-6 rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm font-semibold text-red-700">{error}</div> : null}
      {!courseId && !loadingCourses ? <section className="rounded-[28px] border border-dashed border-[#dfbfbd] bg-white p-12 text-center font-semibold text-[#584140] shadow-sm">Chọn khóa học để bắt đầu luyện flashcard.</section> : null}
      {loading ? <section className="rounded-[28px] border border-[#ead9db] bg-white p-12 text-center font-semibold text-[#584140] shadow-sm">Đang tải bộ flashcard...</section> : null}
      {courseId && !loading ? <>
        {terms.length ? <p className="mb-4 text-sm font-semibold text-[#6a5553]">Đã tải {terms.length} thẻ không trùng lặp.</p> : null}
        <WorkspaceFlashcards
          emptyStateDescription="Khóa học này chưa có flashcard."
          termsOverride={terms}
        />
      </> : null}
    </LearnerPageShell>
  );
}
