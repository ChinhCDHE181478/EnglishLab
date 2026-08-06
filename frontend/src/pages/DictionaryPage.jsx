import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  BookmarkPlus,
  BookOpenText,
  Check,
  Headphones,
  LoaderCircle,
  Layers3,
  PencilLine,
  Search,
  Trash2,
  Volume2,
  BookMarked,
  GraduationCap,
  Trophy,
  HelpCircle,
  Star,
  ArrowRight,
  Volume1,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import dictionaryApi from '../api/dictionaryApi';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import { useAppDialog } from '../components/ui/AppDialog';
import BrandedSelect from '../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../components/ui/Pagination';

const STATUS_OPTIONS = [
  { label: 'Tất cả trạng thái', value: '' },
  { label: 'Đang học', value: 'LEARNING' },
  { label: 'Đã thuộc', value: 'MASTERED' },
];

const POPULAR_WORDS = [
  'resilience',
  'eloquent',
  'mitigate',
  'impeccable',
  'ubiquitous',
  'aesthetic',
];

const getErrorMessage = (error, fallback) => (
  error?.response?.data?.message
  || error?.response?.data?.error
  || error?.message
  || fallback
);

const getPrimaryDefinition = (entry) => {
  const definition = entry?.meanings
    ?.flatMap((meaning) => meaning.definitions || [])
    .find((item) => item.definition);
  return entry?.meaningVietnamese || definition?.definition || '';
};

export default function DictionaryPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const [query, setQuery] = useState('');
  const [entry, setEntry] = useState(null);
  const [lookupLoading, setLookupLoading] = useState(false);
  const [lookupError, setLookupError] = useState('');
  const [saveNote, setSaveNote] = useState('');
  const [saving, setSaving] = useState(false);
  const [savedItems, setSavedItems] = useState([]);
  const [savedLoading, setSavedLoading] = useState(true);
  const [savedError, setSavedError] = useState('');
  const [savedKeyword, setSavedKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [editingItem, setEditingItem] = useState(null);
  const [editingNote, setEditingNote] = useState('');
  const [workingId, setWorkingId] = useState(null);
  const [isPlayingAudio, setIsPlayingAudio] = useState(false);

  const loadSaved = useCallback(async () => {
    setSavedLoading(true);
    setSavedError('');
    try {
      const items = await dictionaryApi.listSaved({
        keyword: savedKeyword.trim() || undefined,
        status: statusFilter || undefined,
      });
      setSavedItems(items);
    } catch (error) {
      setSavedItems([]);
      setSavedError(getErrorMessage(error, 'Không thể tải sổ từ của bạn.'));
    } finally {
      setSavedLoading(false);
    }
  }, [savedKeyword, statusFilter]);

  useEffect(() => {
    const timer = window.setTimeout(loadSaved, 250);
    return () => window.clearTimeout(timer);
  }, [loadSaved]);

  const savedWordSet = useMemo(
    () => new Set(savedItems.map((item) => String(item.word || '').toLowerCase())),
    [savedItems]
  );

  // Vocabulary stats calculations
  const stats = useMemo(() => {
    const total = savedItems.length;
    const mastered = savedItems.filter((item) => item.status === 'MASTERED').length;
    const learning = total - mastered;
    const percent = total > 0 ? Math.round((mastered / total) * 100) : 0;
    return { total, mastered, learning, percent };
  }, [savedItems]);

  const paginationKey = `${savedKeyword}|${statusFilter}|${savedItems.length}`;
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(savedItems, 6, paginationKey);

  const lookup = async (event) => {
    event?.preventDefault();
    const normalized = query.trim();
    if (!normalized) {
      setLookupError('Vui lòng nhập một từ hoặc cụm từ tiếng Anh để bắt đầu tra cứu.');
      return;
    }
    setLookupLoading(true);
    setLookupError('');
    setEntry(null);
    setSaveNote('');
    try {
      const result = await dictionaryApi.lookup(normalized);
      setEntry(result);
    } catch (error) {
      setLookupError(getErrorMessage(error, 'Không thể tra cứu từ này. Hãy kiểm tra lại chính tả hoặc thử lại sau.'));
    } finally {
      setLookupLoading(false);
    }
  };

  const handlePopularClick = async (word) => {
    setQuery(word);
    setLookupLoading(true);
    setLookupError('');
    setEntry(null);
    setSaveNote('');
    try {
      const result = await dictionaryApi.lookup(word);
      setEntry(result);
    } catch (error) {
      setLookupError(getErrorMessage(error, 'Không thể tra cứu từ này.'));
    } finally {
      setLookupLoading(false);
    }
  };

  const playAudio = async () => {
    if (!entry?.audioUrl || isPlayingAudio) return;
    setIsPlayingAudio(true);
    try {
      const audio = new Audio(entry.audioUrl);
      audio.onended = () => setIsPlayingAudio(false);
      audio.onerror = () => setIsPlayingAudio(false);
      await audio.play();
    } catch {
      setLookupError('Trình duyệt không thể phát bản ghi phát âm này.');
      setIsPlayingAudio(false);
    }
  };

  const saveCurrentWord = async () => {
    if (!entry) return;
    setSaving(true);
    setLookupError('');
    try {
      await dictionaryApi.save({
        word: entry.word,
        phonetic: entry.phonetic,
        primaryDefinition: getPrimaryDefinition(entry),
        note: saveNote.trim() || null,
      });
      setSaveNote('');
      await loadSaved();
    } catch (error) {
      setLookupError(getErrorMessage(error, 'Không thể lưu từ vào sổ từ.'));
    } finally {
      setSaving(false);
    }
  };

  const updateItem = async (item, patch) => {
    setWorkingId(item.id);
    setSavedError('');
    try {
      await dictionaryApi.update(item.id, {
        note: patch.note ?? item.note ?? null,
        status: patch.status ?? item.status,
      });
      setEditingItem(null);
      await loadSaved();
    } catch (error) {
      setSavedError(getErrorMessage(error, 'Không thể cập nhật từ đã lưu.'));
    } finally {
      setWorkingId(null);
    }
  };

  const removeItem = async (item) => {
    const confirmed = await confirmDialog(`Xóa từ “${item.word}” khỏi sổ từ của bạn?`, {
      title: 'Xóa từ đã lưu',
      confirmLabel: 'Xóa khỏi sổ từ',
      cancelLabel: 'Giữ lại',
      tone: 'danger',
    });
    if (!confirmed) return;
    setWorkingId(item.id);
    setSavedError('');
    try {
      await dictionaryApi.remove(item.id);
      await loadSaved();
    } catch (error) {
      setSavedError(getErrorMessage(error, 'Không thể xóa từ đã lưu.'));
    } finally {
      setWorkingId(null);
    }
  };

  const isCurrentWordSaved = entry && savedWordSet.has(String(entry.word || '').toLowerCase());

  return (
    <LearnerPageShell
      eyebrow="Công cụ học tập"
      title="Từ điển & Sổ từ cá nhân"
      description="Tra cứu nghĩa, nghe phát âm chuẩn và lưu từ mới trực tiếp vào bộ nhớ cá nhân để ôn tập bằng flashcard."
    >
      <div className="grid flex-1 gap-8 xl:grid-cols-[minmax(0,1.2fr)_minmax(380px,0.8fr)]">
        {/* Left Column: Search & Definitions */}
        <div className="space-y-6">
          <section className="rounded-3xl border border-[#ead9db] bg-white p-6 shadow-sm transition hover:shadow-md md:p-8">
            <div className="flex items-start gap-4">
              <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#8a0018]">
                <BookOpenText className="h-6 w-6" />
              </span>
              <div>
                <h2 className="font-['Manrope'] text-xl font-black text-[#2b2828]">Tra cứu từ vựng</h2>
                <p className="mt-1 text-sm leading-6 text-[#756361]">Tìm kiếm phiên âm, nghĩa tiếng Việt, định nghĩa Anh-Anh và ví dụ trực quan.</p>
              </div>
            </div>

            <form className="mt-6 flex flex-col gap-3 sm:flex-row" onSubmit={lookup}>
              <label className="relative flex-1">
                <Search className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-[#9b8582]" />
                <input
                  autoComplete="off"
                  className="h-12 w-full rounded-2xl border border-[#dfbfbd] bg-[#fffdfc] pl-12 pr-4 text-sm font-semibold text-[#2b2828] outline-none transition focus:border-[#8a0018] focus:ring-4 focus:ring-[#8a0018]/5"
                  maxLength={120}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Ví dụ: resilience, eloquent, mitigate..."
                  value={query}
                />
              </label>
              <button
                className="inline-flex h-12 items-center justify-center gap-2 rounded-2xl bg-[#4b0009] px-6 text-sm font-extrabold text-white transition hover:bg-[#730014] hover:shadow active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={lookupLoading}
                type="submit"
              >
                {lookupLoading ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />}
                {lookupLoading ? 'Đang tra cứu...' : 'Tra nghĩa'}
              </button>
            </form>

            {/* Popular Search Recommendations */}
            <div className="mt-4 flex flex-wrap items-center gap-2">
              <span className="text-xs font-bold text-[#8c716f]">Gợi ý tra nhanh:</span>
              {POPULAR_WORDS.map((word) => (
                <button
                  key={word}
                  onClick={() => handlePopularClick(word)}
                  type="button"
                  className="rounded-lg bg-[#fff1f2]/60 px-2.5 py-1 text-xs font-bold text-[#8a0018] transition hover:bg-[#fff1f2] hover:text-[#730014]"
                >
                  {word}
                </button>
              ))}
            </div>

            {lookupError ? (
              <p className="mt-5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700">{lookupError}</p>
            ) : null}
          </section>

          {!entry && !lookupLoading && !lookupError ? (
            <div className="flex items-start gap-4 rounded-2xl border border-[#ead9db] bg-[#fffdfc] px-5 py-5">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#fff1f2] text-[#8a0018]">
                <BookOpenText className="h-5 w-5" />
              </span>
              <div className="min-w-0">
                <h3 className="font-['Manrope'] text-base font-extrabold text-[#341c1d]">Tra một từ tiếng Anh để bắt đầu</h3>
                <p className="mt-1 text-sm leading-6 text-[#756361]">
                  Kết quả gồm nghĩa tiếng Việt, phiên âm, phát âm và ví dụ. Từ cần ghi nhớ có thể thêm ngay vào flashcard cá nhân.
                </p>
              </div>
            </div>
          ) : null}

          {/* Loader */}
          {lookupLoading ? (
            <div className="rounded-3xl border border-[#ead9db] bg-white p-12 text-center shadow-sm">
              <LoaderCircle className="mx-auto h-8 w-8 animate-spin text-[#8a0018]" />
              <p className="mt-4 font-semibold text-slate-600">Đang tìm kiếm định nghĩa từ nguồn dữ liệu học thuật...</p>
            </div>
          ) : null}

          {/* Lookup Result Content */}
          {entry && !lookupLoading ? (
            <div className="space-y-6">
              {/* Word Header Card */}
              <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-[#4b0009] to-[#8a0018] p-6 text-white shadow-md md:p-8">
                <div className="absolute right-0 top-0 -mr-12 -mt-12 h-40 w-40 rounded-full bg-white/5 blur-2xl" />
                <div className="flex flex-wrap items-center justify-between gap-4">
                  <div>
                    <h3 className="font-['Manrope'] text-4xl font-black capitalize tracking-tight">{entry.word}</h3>
                    <p className="mt-1.5 text-sm font-bold text-pink-100/80">{entry.phonetic || 'Chưa có phiên âm'}</p>
                  </div>
                  {entry.audioUrl ? (
                    <button
                      className={`inline-flex items-center gap-2 rounded-2xl border border-white/20 bg-white/10 px-5 py-3 text-sm font-extrabold text-white transition hover:bg-white hover:text-[#4b0009] active:scale-95 ${isPlayingAudio ? 'animate-pulse bg-white/20' : ''}`}
                      onClick={playAudio}
                      type="button"
                    >
                      {isPlayingAudio ? (
                        <Volume1 className="h-4 w-4 animate-bounce" />
                      ) : (
                        <Volume2 className="h-4 w-4" />
                      )}
                      Nghe phát âm
                    </button>
                  ) : null}
                </div>
              </div>

              {/* Vietnamese Translation Banner */}
              {entry.vietnameseMeaningAvailable ? (
                <div className="rounded-2xl border border-emerald-100 bg-emerald-50/50 px-5 py-4 flex items-start gap-3 shadow-sm">
                  <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-emerald-100 text-emerald-800 font-bold text-sm">译</span>
                  <div>
                    <p className="text-[10px] font-extrabold uppercase tracking-wider text-emerald-700">Nghĩa tiếng Việt cơ bản</p>
                    <p className="mt-1 text-lg font-bold text-slate-800">{entry.meaningVietnamese}</p>
                  </div>
                </div>
              ) : (
                <p className="rounded-2xl border border-amber-100 bg-amber-50/50 px-4 py-3.5 text-xs font-bold leading-5 text-amber-800">
                  ⚠️ Chưa có nghĩa dịch chuẩn tiếng Việt. Bạn vẫn có thể xem các định nghĩa tiếng Anh chi tiết bên dưới.
                </p>
              )}

              {/* Definitions Part of Speech cards */}
              <div className="space-y-4">
                {(entry.meanings || []).map((meaning, meaningIndex) => (
                  <article className="rounded-3xl border border-[#ead9db] bg-white p-6 shadow-sm transition hover:shadow-md" key={`${meaning.partOfSpeech}-${meaningIndex}`}>
                    <div className="flex items-center">
                      <span className="rounded-full bg-[#fff1f2] px-4 py-1 text-xs font-extrabold uppercase tracking-wider text-[#8a0018]">
                        {meaning.partOfSpeech || 'Nghĩa'}
                      </span>
                    </div>
                    
                    <ol className="mt-5 space-y-5">
                      {(meaning.definitions || []).map((definition, definitionIndex) => (
                        <li className="grid grid-cols-[28px_1fr] gap-2" key={`${definition.definition}-${definitionIndex}`}>
                          <span className="pt-0.5 text-sm font-semibold tabular-nums text-[#8a0018]/55">{String(definitionIndex + 1).padStart(2, '0')}</span>
                          <div>
                            <p className="text-[15px] font-normal leading-7 text-[#2b2828]">{definition.definition}</p>
                            {definition.example ? (
                              <div className="mt-2 border-l-2 border-[#dfbfbd] bg-slate-50/60 py-1.5 pl-3 pr-2 rounded-r-xl italic text-slate-600 text-sm">
                                “{definition.example}”
                              </div>
                            ) : null}
                          </div>
                        </li>
                      ))}
                    </ol>

                    {meaning.synonyms?.length ? (
                      <div className="mt-5 border-t border-slate-100 pt-4 text-xs leading-6 text-[#756361]">
                        <strong className="text-[#584140] font-extrabold uppercase tracking-wider mr-2 text-[10px]">Đồng nghĩa:</strong>
                        {meaning.synonyms.slice(0, 8).join(', ')}
                      </div>
                    ) : null}
                    {meaning.antonyms?.length ? (
                      <div className="mt-2 text-xs leading-6 text-[#756361]">
                        <strong className="text-[#584140] font-extrabold uppercase tracking-wider mr-2 text-[10px]">Trái nghĩa:</strong>
                        {meaning.antonyms.slice(0, 8).join(', ')}
                      </div>
                    ) : null}
                  </article>
                ))}
              </div>

              {/* Add Note & Save word Card */}
              <div className="rounded-3xl border border-[#dfbfbd] bg-[#fffafa] p-6 shadow-sm">
                <h4 className="font-['Manrope'] text-base font-black text-[#341c1d]">Thêm ghi chú ghi nhớ cá nhân</h4>
                <p className="text-xs text-[#756361] mt-1">Viết collocation, ngữ cảnh sử dụng hoặc mẹo cá nhân để ôn tập hiệu quả hơn.</p>
                
                <textarea
                  className="mt-3 min-h-24 w-full resize-y rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm leading-6 outline-none transition focus:border-[#8a0018]"
                  id="dictionary-note"
                  maxLength={1000}
                  onChange={(event) => setSaveNote(event.target.value)}
                  placeholder="Ví dụ: collocation, ngữ cảnh sử dụng hoặc câu ví dụ tự viết..."
                  value={saveNote}
                />
                
                <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
                  <span className="text-xs font-semibold text-[#8c716f]">{saveNote.length}/1.000 ký tự</span>
                  <button
                    className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-6 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014] hover:shadow active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
                    disabled={saving || isCurrentWordSaved}
                    onClick={saveCurrentWord}
                    type="button"
                  >
                    {isCurrentWordSaved ? <Check className="h-4 w-4" /> : <BookmarkPlus className="h-4 w-4" />}
                    {isCurrentWordSaved ? 'Đã lưu vào sổ từ' : saving ? 'Đang thêm...' : 'Lưu vào sổ từ cá nhân'}
                  </button>
                </div>
              </div>
            </div>
          ) : null}
        </div>

        {/* Right Column: Personal Wordbook */}
        <div className="space-y-6">
          <section className="rounded-3xl border border-[#ead9db] bg-white p-6 shadow-sm md:p-8">
            <div className="flex items-start gap-4">
              <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#8a0018]">
                <BookMarked className="h-6 w-6" />
              </span>
              <div className="flex-1 min-w-0">
                <h2 className="font-['Manrope'] text-xl font-black text-[#2b2828]">Sổ từ cá nhân</h2>
                <p className="mt-1 text-sm text-[#756361] truncate">Lưu trữ từ đã tra cứu và theo dõi tiến độ thuộc từ.</p>
              </div>
            </div>

            {/* Gamified Vocabulary Stats Widget */}
            <div className="mt-6 rounded-2xl bg-[#fffafb] border border-[#dfbfbd]/50 p-4">
              <div className="flex items-center justify-between text-xs font-bold text-[#8c716f]">
                <span className="flex items-center gap-1"><Trophy className="h-3.5 w-3.5 text-amber-500" /> Đã thuộc</span>
                <span>{stats.mastered}/{stats.total} từ ({stats.percent}%)</span>
              </div>
              {/* Progress bar */}
              <div className="mt-2 h-2.5 w-full rounded-full bg-slate-100 overflow-hidden">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-[#8a0018] to-emerald-600 transition-all duration-500"
                  style={{ width: `${stats.percent}%` }}
                />
              </div>
              <div className="mt-3 grid grid-cols-2 gap-2 text-center text-xs border-t border-slate-100 pt-3">
                <div>
                  <p className="text-[10px] font-bold text-[#8c716f] uppercase">Đang học</p>
                  <p className="text-base font-extrabold text-[#730014] mt-0.5">{stats.learning}</p>
                </div>
                <div className="border-l border-slate-100">
                  <p className="text-[10px] font-bold text-[#8c716f] uppercase">Đã thuộc</p>
                  <p className="text-base font-extrabold text-emerald-700 mt-0.5">{stats.mastered}</p>
                </div>
              </div>
            </div>

            {/* Practice Button */}
            {stats.total > 0 && (
              <Link
                className="mt-4 flex items-center justify-between gap-3 rounded-2xl bg-[#4b0009] px-4 py-3.5 text-sm font-extrabold text-white transition hover:bg-[#730014] shadow-sm active:scale-95"
                to="/flashcards/practice?source=personal"
              >
                <span className="inline-flex items-center gap-2">
                  <Layers3 className="h-4.5 w-4.5" />
                  Ôn tập qua Flashcard cá nhân
                </span>
                <span className="rounded-full bg-white/20 px-3 py-0.5 text-xs font-black flex items-center gap-1">
                  Luyện tập <ArrowRight className="h-3 w-3" />
                </span>
              </Link>
            )}

            {/* Filter controls */}
            <div className="mt-5 grid gap-3 sm:grid-cols-[1fr_150px] xl:grid-cols-1 2xl:grid-cols-[1fr_150px]">
              <label className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#9b8582]" />
                <input
                  className="h-10 w-full rounded-xl border border-[#dfbfbd] bg-[#fffdfc] pl-9 pr-3 text-xs font-semibold outline-none transition focus:border-[#8a0018]"
                  onChange={(event) => setSavedKeyword(event.target.value)}
                  placeholder="Tìm từ đã lưu..."
                  value={savedKeyword}
                />
              </label>
              <BrandedSelect
                buttonClassName="h-10 rounded-xl border-[#dfbfbd] bg-[#fffdfc] py-2 text-xs shadow-none"
                onChange={(event) => setStatusFilter(event.target.value)}
                options={STATUS_OPTIONS}
                value={statusFilter}
              />
            </div>

            {savedError ? <p className="mt-4 rounded-xl bg-rose-50 p-3 text-xs font-semibold text-rose-700">{savedError}</p> : null}

            {/* Load State */}
            {savedLoading ? (
              <div className="flex min-h-[320px] flex-col items-center justify-center text-sm font-semibold text-[#756361] py-8">
                <LoaderCircle className="h-6 w-6 animate-spin text-[#8a0018]" />
                <p className="mt-3 text-xs text-slate-500">Đang đồng bộ sổ từ cá nhân...</p>
              </div>
            ) : null}

            {/* Empty State */}
            {!savedLoading && !savedError && pageItems.length === 0 ? (
              <div className="mt-5 flex min-h-[320px] flex-col items-center justify-center rounded-[22px] border border-dashed border-[#dfbfbd] bg-[#fffafa] px-6 py-8 text-center">
                <HelpCircle className="h-8 w-8 text-[#8a0018]/40" />
                <h3 className="mt-3.5 text-base font-extrabold text-[#341c1d]">Sổ từ chưa có dữ liệu</h3>
                <p className="mt-2 text-xs leading-5 text-[#756361] max-w-xs">
                  {savedKeyword || statusFilter ? 'Không tìm thấy từ khớp với bộ lọc hiện tại.' : 'Hãy tra từ mới ở khung bên trái rồi bấm nút lưu để tạo sổ từ cá nhân.'}
                </p>
              </div>
            ) : null}

            {/* Wordbook list items */}
            {!savedLoading && pageItems.length > 0 ? (
              <div className="mt-5 space-y-3">
                {pageItems.map((item) => (
                  <article className="group rounded-[20px] border border-[#ead9db] bg-white p-4 transition-all duration-200 hover:border-[#dfbfbd] hover:bg-[#fffafa]/40 hover:shadow-sm" key={item.id}>
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-1.5">
                          <h3 className="truncate font-['Manrope'] text-lg font-black capitalize text-[#4b0009]">{item.word}</h3>
                          <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-[9px] font-extrabold uppercase tracking-wider ${
                            item.status === 'MASTERED' ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : 'bg-amber-50 text-amber-700 border border-amber-100'
                          }`}>
                            {item.status === 'MASTERED' ? 'Đã thuộc' : 'Đang học'}
                          </span>
                        </div>
                        {item.phonetic ? <p className="mt-0.5 text-xs font-semibold text-[#8c716f]">{item.phonetic}</p> : null}
                      </div>
                      
                      {/* Action buttons (Edit note & Remove) */}
                      <div className="flex shrink-0 gap-0.5 opacity-80 group-hover:opacity-100">
                        <button
                          aria-label={`Sửa ghi chú ${item.word}`}
                          className="rounded-lg p-1.5 text-[#756361] transition hover:bg-[#fff1f2] hover:text-[#8a0018]"
                          onClick={() => {
                            setEditingItem(item);
                            setEditingNote(item.note || '');
                          }}
                          type="button"
                        >
                          <PencilLine className="h-4 w-4" />
                        </button>
                        <button
                          aria-label={`Xóa ${item.word}`}
                          className="rounded-lg p-1.5 text-[#756361] transition hover:bg-rose-50 hover:text-rose-700 disabled:opacity-50"
                          disabled={workingId === item.id}
                          onClick={() => removeItem(item)}
                          type="button"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                    
                    {/* Definition */}
                    <p className="mt-2 text-xs leading-5 text-[#584140] line-clamp-2">{item.primaryDefinition}</p>
                    
                    {/* Note */}
                    {item.note ? (
                      <p className="mt-2 rounded-xl bg-[#fff7f7] border border-[#f5ecec] px-3 py-2 text-[11px] leading-relaxed text-[#756361]">
                        <span className="font-extrabold text-[#8a0018] uppercase tracking-wider text-[9px] mr-1 block">Ghi chú cá nhân:</span>
                        {item.note}
                      </p>
                    ) : null}
                    
                    {/* Status change toggle button */}
                    <button
                      className={`mt-3 inline-flex items-center gap-1.5 rounded-xl border px-3 py-1.5 text-[10px] font-extrabold uppercase tracking-wider transition ${
                        item.status === 'MASTERED'
                          ? 'border-[#dfbfbd] bg-[#fff1f2] text-[#8a0018] hover:bg-[#fbe3e6]'
                          : 'border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                      } disabled:opacity-50`}
                      disabled={workingId === item.id}
                      onClick={() => updateItem(item, { status: item.status === 'MASTERED' ? 'LEARNING' : 'MASTERED' })}
                      type="button"
                    >
                      <Check className="h-3 w-3" />
                      {item.status === 'MASTERED' ? 'Đưa về đang học' : 'Đánh dấu đã thuộc'}
                    </button>
                  </article>
                ))}
              </div>
            ) : null}

            {/* Pagination */}
            {!savedLoading && totalPages > 1 ? (
              <div className="mt-5 border-t border-slate-100 pt-4">
                <Pagination onChange={setPage} page={page} pageSize={6} totalItems={totalItems} totalPages={totalPages} />
              </div>
            ) : null}
          </section>
        </div>
      </div>

      {/* Edit Notes Modal */}
      {editingItem ? (
        <div className="fixed inset-0 z-[80] flex items-center justify-center bg-[#260006]/45 p-4 backdrop-blur-sm">
          <form
            className="w-full max-w-lg rounded-[28px] border border-[#ead9db] bg-white p-6 shadow-2xl transition-all"
            onSubmit={(event) => {
              event.preventDefault();
              updateItem(editingItem, { note: editingNote.trim() || null });
            }}
          >
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div>
                <span className="text-[10px] font-extrabold uppercase tracking-widest text-[#8a0018]">Hiệu chỉnh</span>
                <h2 className="font-['Manrope'] text-xl font-black text-[#341c1d]">Ghi chú cho “{editingItem.word}”</h2>
              </div>
              <button
                type="button"
                className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-50 hover:text-slate-600"
                onClick={() => setEditingItem(null)}
              >
                ✕
              </button>
            </div>
            
            <p className="mt-3 text-xs leading-5 text-[#756361]">Cập nhật câu ví dụ, từ đồng nghĩa hoặc mẹo giúp nhớ từ này của riêng bạn.</p>
            
            <textarea
              autoFocus
              className="mt-4 min-h-36 w-full rounded-2xl border border-[#dfbfbd] p-4 text-sm leading-6 outline-none transition focus:border-[#8a0018] focus:ring-4 focus:ring-[#8a0018]/5"
              maxLength={1000}
              onChange={(event) => setEditingNote(event.target.value)}
              placeholder="Ví dụ collocation, ngữ cảnh sử dụng..."
              value={editingNote}
            />
            
            <div className="mt-5 flex items-center justify-between text-xs font-semibold text-slate-400">
              <span>{editingNote.length}/1.000 ký tự</span>
              <div className="flex gap-2">
                <button
                  className="rounded-xl border border-[#dfbfbd] px-4 py-2.5 text-xs font-bold text-[#730014] transition hover:bg-slate-50"
                  onClick={() => setEditingItem(null)}
                  type="button"
                >
                  Hủy
                </button>
                <button
                  className="rounded-xl bg-[#4b0009] px-5 py-2.5 text-xs font-extrabold text-white transition hover:bg-[#730014] disabled:opacity-60"
                  disabled={workingId === editingItem.id}
                  type="submit"
                >
                  {workingId === editingItem.id ? 'Đang lưu...' : 'Lưu ghi chú'}
                </button>
              </div>
            </div>
          </form>
        </div>
      ) : null}
    </LearnerPageShell>
  );
}
