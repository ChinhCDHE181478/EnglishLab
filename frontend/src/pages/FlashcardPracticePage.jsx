import { useCallback, useEffect, useState } from 'react';
import { BookOpenCheck, Heart, Layers3, RefreshCw } from 'lucide-react';
import courseApi from '../api/courseApi';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import WorkspaceFlashcards from '../components/course-workspace/WorkspaceFlashcards';

const SOURCES = [
  { value: 'ENROLLED', label: 'Từ khóa đã đăng ký', description: 'Các khóa học bạn đang sở hữu', icon: BookOpenCheck },
  { value: 'WISHLIST', label: 'Từ wishlist', description: 'Các khóa học bạn đã lưu', icon: Heart },
  { value: 'ALL', label: 'Tất cả bộ có thể học', description: 'Flashcard từ khóa học đã xuất bản', icon: Layers3 },
];

export default function FlashcardPracticePage() {
  const [source, setSource] = useState('ENROLLED');
  const [terms, setTerms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadTerms = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await courseApi.getGlobalFlashcardPractice({ source });
      setTerms(data);
    } catch (requestError) {
      setTerms([]);
      setError(requestError.response?.data?.message || 'Chưa thể tải flashcard. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  }, [source]);

  useEffect(() => {
    const fetchTerms = async () => {
      await loadTerms();
    };
    fetchTerms();
  }, [loadTerms]);

  return (
    <LearnerPageShell
      actions={<button className="inline-flex items-center gap-2 rounded-full border border-[#dfbfbd] bg-white px-5 py-2.5 text-sm font-extrabold text-[#730014] shadow-sm transition hover:bg-[#fff6f6] disabled:opacity-50" disabled={loading} onClick={loadTerms} type="button"><RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />Làm mới</button>}
      description="Ôn tập flashcard từ các khóa học đã đăng ký và danh sách yêu thích."
      eyebrow="Flashcard practice"
      title="Luyện từ vựng"
    >
      <section className="mb-8 grid gap-3 md:grid-cols-3" aria-label="Nguồn flashcard">
        {SOURCES.map(({ value, label, description, icon: Icon }) => {
          const active = source === value;
          return (
            <button className={`rounded-2xl border p-5 text-left shadow-sm transition ${active ? 'border-[#8a0018] bg-[#fff7f7] ring-2 ring-[#8a0018]/10' : 'border-[#ead9db] bg-white hover:border-[#8a0018]/40'}`} key={value} onClick={() => setSource(value)} type="button">
              <div className="flex items-center gap-3"><span className={`flex h-10 w-10 items-center justify-center rounded-xl ${active ? 'bg-[#8a0018] text-white' : 'bg-[#f7eeee] text-[#730014]'}`}><Icon className="h-5 w-5" /></span><div><h2 className="font-['Manrope'] text-sm font-extrabold text-[#2b2828]">{label}</h2><p className="mt-1 text-xs text-[#806765]">{description}</p></div></div>
            </button>
          );
        })}
      </section>

      {error ? <div className="mb-6 rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm font-semibold text-red-700">{error}</div> : null}
      {loading ? <section className="rounded-[28px] border border-[#ead9db] bg-white p-12 text-center font-semibold text-[#584140] shadow-sm">Đang tải bộ flashcard...</section> : null}
      {!loading ? <>
        {terms.length ? <p className="mb-4 text-sm font-semibold text-[#6a5553]">Đã tải {terms.length} thẻ không trùng lặp.</p> : null}
        <WorkspaceFlashcards
          emptyStateDescription="Chưa có flashcard phù hợp với nguồn bạn đã chọn."
          termsOverride={terms}
        />
      </> : null}
    </LearnerPageShell>
  );
}
