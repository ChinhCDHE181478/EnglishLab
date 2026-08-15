import { useEffect, useMemo, useState } from 'react';
import {
  ArrowRight,
  BookOpen,
  CheckCircle2,
  Clock,
  FileQuestion,
  GraduationCap,
  Headphones,
  ListFilter,
  Mic,
  PenLine,
  Search,
  Sparkles,
  Trophy,
  Users,
  Zap,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import Header from '../components/ai-learning/Header';
import ListeningExamMode from '../components/course-assessment/ListeningExamMode';
import ReadingExamMode from '../components/course-assessment/ReadingExamMode';
import WritingExamMode from '../components/course-assessment/WritingExamMode';
import SpeakingExamMode from '../components/course-assessment/SpeakingExamMode';
import CourseFooter from '../components/course/CourseFooter';
import BrandedSelect from '../components/ui/BrandedSelect';
import BrandLoadingState from '../components/ui/BrandLoadingState';
import mockTestApi from '../api/mockTestApi';
import Pagination, { usePagination } from '../components/ui/Pagination';
import placementTestApi from '../api/placementTestApi';
import { exitExamFullscreen, requestExamFullscreen } from '../utils/examFullscreen';

const sortOptions = [
  { label: 'Mới nhất (Newest)', value: 'NEWEST' },
  { label: 'Nhiều lượt thi nhất (Popular)', value: 'POPULAR' },
  { label: 'Điểm tối đa cao nhất', value: 'SCORE' },
];

const resolveExamType = (item) => {
  const explicit = item.rubric?.examType || item.examType;
  if (explicit) return String(explicit).toUpperCase();
  const searchable = `${item.title || ''} ${item.description || ''}`.toUpperCase();
  if (searchable.includes('TOEIC')) return 'TOEIC';
  if (searchable.includes('IELTS')) return 'IELTS';
  return 'GENERAL';
};

const skillMeta = {
  LISTENING: { label: 'Listening', icon: Headphones, color: 'text-sky-700 bg-sky-50 border-sky-200', defaultDuration: '30 phút' },
  READING: { label: 'Reading', icon: BookOpen, color: 'text-emerald-700 bg-emerald-50 border-emerald-200', defaultDuration: '60 phút' },
  WRITING: { label: 'Writing', icon: PenLine, color: 'text-violet-700 bg-violet-50 border-violet-200', defaultDuration: '60 phút' },
  SPEAKING: { label: 'Speaking', icon: Mic, color: 'text-amber-700 bg-amber-50 border-amber-200', defaultDuration: '15 phút' },
  MIXED: { label: 'Full Mock', icon: FileQuestion, color: 'text-[#730014] bg-[#fff0f1] border-[#f5d0d3]', defaultDuration: '120 phút' },
};

const TOEIC_PART_START = {
  LISTENING: { 1: 1, 2: 7, 3: 32, 4: 71 },
  READING: { 5: 101, 6: 131, 7: 147 },
};

export default function MockTestsPage() {
  const [tests, setTests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [keyword, setKeyword] = useState('');
  const [examTab, setExamTab] = useState('ALL'); // ALL, ACADEMIC, GENERAL
  const [selectedSkill, setSelectedSkill] = useState('ALL'); // ALL, LISTENING, READING, WRITING, SPEAKING
  const [sortBy, setSortBy] = useState('NEWEST');
  const [activeTest, setActiveTest] = useState(null);
  const [activeConfig, setActiveConfig] = useState(null);
  const [activeSkill, setActiveSkill] = useState(null);
  const [result, setResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const data = await mockTestApi.listMockTests();
        if (active) setTests(data);
      } catch (requestError) {
        if (active) setError(requestError?.response?.data?.message || 'Không tải được ngân hàng đề thi thử.');
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => { active = false; };
  }, []);

  // Filter & Sort REAL tests from backend API
  const filteredTests = useMemo(() => {
    const query = keyword.trim().toLowerCase();
    let resultList = tests.filter((item) => {
      const type = resolveExamType(item);
      const matchesExamTab =
        examTab === 'ALL' ||
        (examTab === 'ACADEMIC' && type === 'IELTS') ||
        (examTab === 'GENERAL' && type === 'TOEIC');
      const matchesSkill = selectedSkill === 'ALL' || item.skill === selectedSkill;
      const matchesKeyword = !query || [item.title, item.description, item.skill, item.type]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(query));
      return matchesExamTab && matchesSkill && matchesKeyword;
    });

    if (sortBy === 'POPULAR') {
      resultList = [...resultList].sort((a, b) => (b.attemptsCount || b.id) - (a.attemptsCount || a.id));
    } else if (sortBy === 'SCORE') {
      resultList = [...resultList].sort((a, b) => (b.maxScore || 0) - (a.maxScore || 0));
    }
    return resultList;
  }, [examTab, tests, keyword, selectedSkill, sortBy]);

  const { page, setPage, totalPages, pageItems: paginatedTests, totalItems } = usePagination(
    filteredTests,
    6,
    `mock-tests-${examTab}-${selectedSkill}-${keyword}-${sortBy}`
  );

  const startTest = async (item) => {
    setError('');
    setResult(null);
    const fullscreenStarted = await requestExamFullscreen();
    if (!fullscreenStarted) {
      setError('Không thể bật chế độ toàn màn hình. Hãy cho phép trình duyệt mở toàn màn hình rồi thử lại.');
      return;
    }
    try {
      const detail = await mockTestApi.getMockTest(item.id);
      const config = parseJson(detail.uiConfigJson);
      const resolved = resolveMockConfig(detail, config);
      setActiveTest(detail);
      setActiveConfig(resolved.config);
      setActiveSkill(resolved.skill);
    } catch (requestError) {
      if (fullscreenStarted) await exitExamFullscreen();
      setError(requestError?.response?.data?.message || 'Không mở được đề thi thử.');
    }
  };

  const closeExam = () => {
    setActiveTest(null);
    setActiveConfig(null);
    setActiveSkill(null);
    setSubmitting(false);
  };

  const handleObjectiveSubmit = async (payload) => {
    setSubmitting(true);
    setError('');
    try {
      const savedAttempt = await mockTestApi.submitMockTest(activeTest.id, {
        objectiveAnswersJson: payload.objectiveAnswersJson,
      });
      setResult({
        title: savedAttempt.mockTestTitle || activeTest?.title,
        skill: savedAttempt.skill || activeSkill,
        correct: savedAttempt.correctCount,
        total: savedAttempt.totalQuestions,
        percent: savedAttempt.percent,
        score: savedAttempt.score,
        submittedAt: savedAttempt.submittedAt,
      });
      closeExam();
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
      setResult({
        title: savedAttempt.mockTestTitle || activeTest?.title,
        skill: savedAttempt.skill || activeSkill,
        submittedText: savedAttempt.submittedText || payload.submittedText || '',
        submittedAudioUrl: savedAttempt.submittedAudioUrl || payload.submittedAudioUrl || '',
        submittedAt: savedAttempt.submittedAt,
        message: 'Bài thi thử đã được lưu. Giáo viên có thể dùng dữ liệu này để review nếu cần.',
      });
      closeExam();
      return savedAttempt;
    } catch (requestError) {
      setError(requestError?.response?.data?.message || 'Chưa thể nộp bài thi thử.');
      throw requestError;
    } finally {
      setSubmitting(false);
    }
  };

  if (activeTest && activeConfig && activeSkill === 'LISTENING') {
    return (
      <ListeningExamMode
        assessment={{ title: activeTest.title, timeLimitMinutes: activeTest.timeLimitMinutes || activeConfig.durationMinutes }}
        config={activeConfig}
        onClose={closeExam}
        onSubmit={handleObjectiveSubmit}
        submitLabel="Nộp bài thi thử Listening"
        submitting={submitting}
      />
    );
  }

  if (activeTest && activeConfig && activeSkill === 'READING') {
    return (
      <ReadingExamMode
        assessment={{ title: activeTest.title, timeLimitMinutes: activeTest.timeLimitMinutes || activeConfig.durationMinutes }}
        config={activeConfig}
        onClose={closeExam}
        onSubmit={handleObjectiveSubmit}
        submitLabel="Nộp bài thi thử Reading"
        submitting={submitting}
      />
    );
  }

  if (activeTest && activeConfig && activeSkill === 'WRITING') {
    return (
      <WritingExamMode
        assessment={{ title: activeTest.title, timeLimitMinutes: activeTest.timeLimitMinutes || activeConfig.durationMinutes }}
        config={activeConfig}
        onClose={closeExam}
        onSubmit={handleSubjectiveSubmit}
        submitLabel="Nộp bài thi thử Writing"
        submitting={submitting}
      />
    );
  }

  if (activeTest && activeConfig && activeSkill === 'SPEAKING') {
    return (
      <SpeakingExamMode
        config={{ ...activeConfig, submissionLabel: activeTest.title }}
        onClose={closeExam}
        onSubmit={(payload) => handleSubjectiveSubmit('mock-speaking', payload)}
        submitting={submitting}
        uploadAudio={placementTestApi.uploadSpeakingAudio}
      />
    );
  }

  return (
    <div className="flex min-h-[100dvh] flex-col bg-[#fffaf9] text-[#0b1c30]">
      <Header />

      <main className="mx-auto flex w-full max-w-[1240px] flex-1 flex-col px-4 py-8 sm:px-6 lg:px-8 space-y-7">
        
        {/* Main Header Container with EnglishLab Brand Styling */}
        <div className="rounded-3xl border border-[#ead9db] bg-white p-6 sm:p-8 shadow-xs space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#ead9db] pb-5">
            <div>
              <div className="flex items-center gap-2 text-xs font-bold text-[#8b706e]">
                <Link className="hover:underline text-[#730014]" to="/">Trang chủ</Link>
                <span>/</span>
                <span>Thư viện đề thi thử</span>
              </div>
              <h1 className="mt-2 font-['Manrope'] text-2xl sm:text-3xl font-extrabold text-[#0b1c30] tracking-tight">
                IELTS & TOEIC Exam Library
              </h1>
              <p className="mt-1 text-xs sm:text-sm text-[#564241]">
                Ngân hàng đề thi thử trực tuyến chính thức của EnglishLab, lấy trực tiếp từ hệ thống dữ liệu thực tế.
              </p>
            </div>

            {/* <span className="inline-flex items-center gap-1.5 self-start sm:self-auto rounded-full bg-[#fff0f1] px-3.5 py-1.5 text-xs font-extrabold text-[#730014] border border-[#f5d0d3]">
              <Sparkles className="h-3.5 w-3.5" /> Data thực tế ({filteredTests.length} đề thi)
            </span> */}
          </div>

          {/* 1. Top Category Tabs (All Tests, Academic Test, General Training Test) */}
          <div className="flex flex-wrap items-center gap-6 border-b border-[#ead9db] pb-1">
            <button
              className={`flex items-center gap-2 pb-3 text-xs sm:text-sm font-extrabold transition border-b-2 ${
                examTab === 'ALL'
                  ? 'border-[#730014] text-[#730014]'
                  : 'border-transparent text-[#8b706e] hover:text-[#0b1c30]'
              }`}
              onClick={() => { setExamTab('ALL'); setPage(1); }}
              type="button"
            >
              <ListFilter className="h-4 w-4" />
              Tất cả đề thi (All Tests)
            </button>

            <button
              className={`flex items-center gap-2 pb-3 text-xs sm:text-sm font-extrabold transition border-b-2 ${
                examTab === 'ACADEMIC'
                  ? 'border-[#730014] text-[#730014]'
                  : 'border-transparent text-[#8b706e] hover:text-[#0b1c30]'
              }`}
              onClick={() => { setExamTab('ACADEMIC'); setPage(1); }}
              type="button"
            >
              <GraduationCap className="h-4 w-4" />
              Academic Test (IELTS)
            </button>

            <button
              className={`flex items-center gap-2 pb-3 text-xs sm:text-sm font-extrabold transition border-b-2 ${
                examTab === 'GENERAL'
                  ? 'border-[#730014] text-[#730014]'
                  : 'border-transparent text-[#8b706e] hover:text-[#0b1c30]'
              }`}
              onClick={() => { setExamTab('GENERAL'); setPage(1); }}
              type="button"
            >
              <Users className="h-4 w-4" />
              General Training (TOEIC)
            </button>
          </div>

          {/* 2. Skill Pills Filter Bar - EnglishLab Burgundy Brand Tones */}
          <div className="flex flex-wrap items-center gap-2.5">
            <button
              className={`flex items-center gap-2 rounded-full px-4.5 py-1.5 text-xs font-extrabold transition ${
                selectedSkill === 'ALL'
                  ? 'bg-[#730014] text-white shadow-xs'
                  : 'border border-[#ead9db] bg-white text-[#564241] hover:bg-[#fff0f1] hover:text-[#730014]'
              }`}
              onClick={() => { setSelectedSkill('ALL'); setPage(1); }}
              type="button"
            >
              <span className="grid grid-cols-2 gap-0.5 w-3.5 h-3.5">
                <span className="bg-current rounded-xs" />
                <span className="bg-current rounded-xs" />
                <span className="bg-current rounded-xs" />
                <span className="bg-current rounded-xs" />
              </span>
              Tất cả kỹ năng (All Skills)
            </button>

            {['LISTENING', 'READING', 'WRITING', 'SPEAKING'].map((sk) => {
              const meta = skillMeta[sk];
              const Icon = meta.icon;
              const isSelected = selectedSkill === sk;
              return (
                <button
                  className={`flex items-center gap-2 rounded-full px-4 py-1.5 text-xs font-bold transition ${
                    isSelected
                      ? 'bg-[#730014] text-white shadow-xs'
                      : 'border border-[#ead9db] bg-white text-[#564241] hover:bg-[#fff0f1] hover:text-[#730014]'
                  }`}
                  key={sk}
                  onClick={() => { setSelectedSkill(sk); setPage(1); }}
                  type="button"
                >
                  <Icon className="h-3.5 w-3.5 opacity-80" />
                  {meta.label}
                </button>
              );
            })}
          </div>

          {/* 3. Search Bar & Sort Dropdown */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-[#fffaf9] border border-[#ead9db] p-3 rounded-2xl">
            <div className="relative w-full sm:w-[380px]">
              <input
                className="h-10 w-full rounded-xl border border-[#ead9db] bg-white pl-4 pr-10 text-xs outline-none focus:border-[#730014] transition"
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Tìm đề thi từ dữ liệu thật..."
                value={keyword}
              />
              <Search className="pointer-events-none absolute right-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[#8b706e]" />
            </div>

            <div className="flex items-center gap-2 w-full sm:w-auto">
              <span className="text-xs font-bold text-[#8b706e] whitespace-nowrap">Sắp xếp:</span>
              <div className="w-48">
                <BrandedSelect onChange={(event) => setSortBy(event.target.value)} options={sortOptions} value={sortBy} />
              </div>
            </div>
          </div>
        </div>

        {/* Error Notice */}
        {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-xs font-bold text-rose-700">{error}</div> : null}
        {result ? <MockResult result={result} /> : null}

        {/* Loading State */}
        {loading ? (
          <BrandLoadingState className="py-12" message="Đang tải ngân hàng đề thi thử từ hệ thống..." />
        ) : filteredTests.length ? (
          <div className="space-y-6">
            {/* Render REAL Tests Grid with EnglishLab Brand 3D Books & Real API Data */}
            <div className="grid gap-6 md:grid-cols-2">
              {paginatedTests.map((item) => {
                const resolvedExam = resolveExamType(item);
                const meta = skillMeta[item.skill] || skillMeta.MIXED;
                const Icon = meta.icon;
                const attempts = item.attemptsCount || item.takeCount || 0;

                return (
                  <article
                    className="group flex flex-col sm:flex-row items-start gap-5 rounded-3xl border border-[#ead9db] bg-white p-6 shadow-xs transition duration-300 hover:border-[#730014] hover:shadow-md"
                    key={item.id}
                  >
                    {/* 3D Hardcover Book Graphic (EnglishLab Crimson Tone) */}
                    <div className="flex justify-center w-full sm:w-auto shrink-0">
                      <Brand3DBookCover
                        category={resolvedExam}
                        skillLabel={meta.label}
                        title={item.title}
                      />
                    </div>

                    {/* Info Panel on the Right - REAL Data from API */}
                    <div className="flex flex-1 flex-col justify-between h-full space-y-3.5 w-full">
                      <div>
                        {/* Exam Type & Skill Badges */}
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="rounded-md border border-[#f5d0d3] bg-[#fff0f1] px-2.5 py-0.5 text-[11px] font-extrabold uppercase text-[#730014]">
                              {resolvedExam}
                            </span>
                            <span className={`rounded-md border px-2.5 py-0.5 text-[11px] font-bold ${meta.color}`}>
                              <Icon className="mr-1 inline-block h-3 w-3" />
                              {meta.label}
                            </span>
                          </div>

                          <span className="flex items-center gap-1 text-xs font-semibold text-[#8b706e]">
                            <Clock className="h-3.5 w-3.5 text-[#8b706e]" />
                            {item.timeLimitMinutes ? `${item.timeLimitMinutes} phút` : meta.defaultDuration}
                          </span>
                        </div>

                        {/* Real Test Title */}
                        <h2 className="mt-2.5 font-['Manrope'] text-lg font-black text-[#0b1c30] group-hover:text-[#730014] transition-colors leading-snug">
                          {item.title}
                        </h2>

                        {/* Real Description / Instructions */}
                        <p className="mt-1.5 line-clamp-2 text-xs leading-relaxed text-[#564241]">
                          {item.description || item.instructions || 'Đề thi thử chính thức trong hệ thống EnglishLab.'}
                        </p>
                      </div>

                      {/* Real Stats & Action CTA Button */}
                      <div className="pt-3 border-t border-[#f2e6e7] flex items-center justify-between gap-3">
                        <div className="flex items-center gap-1.5 text-xs font-bold text-[#8b706e]">
                          <Zap className="h-4 w-4 text-amber-500 fill-amber-400 shrink-0" />
                          <span>{attempts > 0 ? `${attempts.toLocaleString('vi-VN')} lượt thi` : 'Đề thi mới'}</span>
                        </div>

                        <button
                          className="inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4.5 py-2 text-xs font-extrabold text-white shadow-xs transition hover:bg-[#8a0018] active:scale-95"
                          onClick={() => startTest(item)}
                          type="button"
                        >
                          Vào thi thử <ArrowRight className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>

            {/* Shared Pagination Component */}
            {filteredTests.length > 6 && (
              <div className="flex justify-end pt-4">
                <Pagination
                  onChange={setPage}
                  page={page}
                  pageSize={6}
                  totalItems={totalItems}
                  totalPages={totalPages}
                />
              </div>
            )}
          </div>
        ) : (
          <div className="rounded-2xl border border-dashed border-[#ead9db] bg-[#fffaf9] p-12 text-center text-xs font-bold text-[#564241]">
            Chưa có đề thi thử phù hợp bộ lọc tìm kiếm hiện tại.
          </div>
        )}
      </main>

      <CourseFooter />
    </div>
  );
}

{/* EnglishLab Brand 3D Hardcover Book Graphic Component (Matching Brand Tones) */}
function Brand3DBookCover({ title, category = 'IELTS', skillLabel = 'MOCK' }) {
  const isToeic = String(category).toUpperCase() === 'TOEIC';

  return (
    <div className="relative group/book cursor-pointer perspective-[1000px] w-[135px] sm:w-[150px] h-[185px] sm:h-[205px] shrink-0 select-none">
      {/* Book Soft Shadow */}
      <div className="absolute -bottom-3 left-3 right-3 h-4 rounded-full bg-black/20 blur-md transition-all duration-300 group-hover/book:scale-105" />

      {/* 3D Book Container */}
      <div className="relative w-full h-full duration-500 ease-out transform-style-3d group-hover/book:[transform:rotateY(-24deg)_rotateX(4deg)]">
        {/* Book Spine (Left 3D side) */}
        <div
          className={`absolute left-0 top-0 w-[22px] h-full rounded-l-xs shadow-inner flex items-center justify-center origin-left [transform:rotateY(-90deg)] ${
            isToeic
              ? 'bg-gradient-to-r from-[#0b1c30] via-[#163558] to-[#071324]'
              : 'bg-gradient-to-r from-[#3b0007] via-[#4d000a] to-[#730014]'
          }`}
        >
          <span className="text-[9px] font-black uppercase tracking-widest text-amber-200/90 -rotate-90 whitespace-nowrap drop-shadow-xs">
            ENGLISH LAB · {category}
          </span>
        </div>

        {/* Book Pages Edge (Right 3D side) */}
        <div className="absolute right-1 top-[3px] w-[16px] h-[calc(100%-6px)] bg-[linear-gradient(to_right,#fcfbfa_0%,#eee9de_50%,#fcfbfa_100%)] shadow-inner rounded-r-xs border-r border-slate-300 [transform:translateZ(-14px)_rotateY(90deg)]" />

        {/* Hardcover Front Cover (EnglishLab Brand Tone) */}
        <div
          className={`absolute inset-0 rounded-r-xl border border-white/20 p-3 text-white shadow-xl origin-left transition-transform duration-500 group-hover/book:[transform:rotateY(-26deg)] ${
            isToeic
              ? 'bg-gradient-to-br from-[#0b1c30] via-[#163558] to-[#071324]'
              : 'bg-gradient-to-br from-[#4b0009] via-[#730014] to-[#8a0018]'
          }`}
        >
          {/* Decorative Gold & Subtle Glow Shapes */}
          <div className="absolute -right-6 -top-6 h-24 w-24 rounded-full bg-amber-400/15 blur-xl pointer-events-none" />

          <div className="relative z-10 flex h-full flex-col justify-between border border-amber-300/30 p-2 rounded-lg bg-black/10 backdrop-blur-xs">
            {/* Header Badge */}
            <div className="flex items-center justify-between border-b border-amber-300/30 pb-1">
              <span className="text-[8.5px] font-black uppercase tracking-widest text-amber-300 drop-shadow-xs">
                {category} EXAM
              </span>
              <span className="text-[8.5px] font-extrabold text-amber-200">{skillLabel}</span>
            </div>

            {/* Main Emblem & Title */}
            <div className="my-auto text-center py-1">
              <div className="mx-auto mb-1 flex h-7 w-7 items-center justify-center rounded-full bg-amber-400/20 text-amber-300 border border-amber-300/40 shadow-xs">
                <Trophy size={14} />
              </div>
              <h4 className="font-['Manrope'] text-[11px] font-black uppercase tracking-tight text-white leading-tight line-clamp-2 drop-shadow-xs">
                {title || 'MOCK TEST'}
              </h4>
            </div>

            {/* Footer Tag */}
            <div className="border-t border-amber-300/30 pt-1 text-center">
              <span className="text-[7.5px] font-black uppercase tracking-widest text-amber-300/90">
                ENGLISH LAB
              </span>
            </div>
          </div>
        </div>

        {/* Hardcover Back */}
        <div
          className={`absolute inset-0 rounded-r-xl shadow-md [transform:translateZ(-18px)] ${
            isToeic ? 'bg-[#050e1a]' : 'bg-[#240005]'
          }`}
        />
      </div>
    </div>
  );
}

function MockResult({ result }) {
  return (
    <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-xs text-emerald-900 shadow-xs">
      <div className="flex items-start gap-3">
        <CheckCircle2 className="mt-0.5 h-4.5 w-4.5 text-emerald-700 shrink-0" />
        <div>
          <p className="font-extrabold text-sm">{result.title}</p>
          {result.total != null ? (
            <p className="mt-1 leading-relaxed">
              Kết quả bài thi: {result.correct}/{result.total} câu đúng
              {result.percent != null ? ` · ${Number(result.percent).toFixed(0)}%` : ''}
              {result.score != null ? ` · ${result.score} điểm` : ''}.
            </p>
          ) : (
            <p className="mt-1 leading-relaxed">{result.message}</p>
          )}
          {result.submittedAt ? (
            <p className="mt-1 text-[11px] font-bold opacity-80">
              Đã lưu kết quả lúc {new Date(result.submittedAt).toLocaleString('vi-VN')}
            </p>
          ) : null}
        </div>
      </div>
    </div>
  );
}

function parseJson(value, fallback = {}) {
  if (value && typeof value === 'object') {
    return value;
  }
  try {
    const parsed = JSON.parse(String(value || ''));
    return parsed && typeof parsed === 'object' ? parsed : fallback;
  } catch {
    return fallback;
  }
}

function resolveMockConfig(test, config) {
  const skill = String(test.skill || 'MIXED').toUpperCase();
  if (skill !== 'MIXED') {
    const keyedConfig = config.sections?.[skill.toLowerCase()] || config[skill.toLowerCase()] || config;
    return { skill, config: normalizeObjectiveConfig(keyedConfig, skill) };
  }
  const sections = config.sections || config;
  if (sections?.toeic?.listening) {
    return { skill: 'LISTENING', config: normalizeObjectiveConfig(sections.toeic.listening, 'LISTENING') };
  }
  if (sections?.toeic?.reading) {
    return { skill: 'READING', config: normalizeObjectiveConfig(sections.toeic.reading, 'READING') };
  }
  for (const key of ['listening', 'reading', 'writing', 'speaking']) {
    if (sections?.[key]) {
      const resolvedSkill = key.toUpperCase();
      return { skill: resolvedSkill, config: normalizeObjectiveConfig(sections[key], resolvedSkill) };
    }
  }
  return { skill: 'READING', config };
}

function normalizeObjectiveConfig(config = {}, skill = '') {
  if (!['LISTENING', 'READING'].includes(skill)) return config;
  const parts = Array.isArray(config.parts) ? config.parts : [];
  const firstPartAudioUrl = parts.find((part) => part.audioUrl)?.audioUrl || '';
  if (!parts.length || parts.every((part) => Array.isArray(part.questionGroups))) {
    return skill === 'LISTENING' && firstPartAudioUrl && !config.audioUrl
      ? { ...config, audioUrl: firstPartAudioUrl }
      : config;
  }
  return {
    ...config,
    audioUrl: config.audioUrl || firstPartAudioUrl || '',
    parts: parts.map((part, index) => normalizeToeicStylePart(part, index, skill)),
  };
}

function normalizeToeicStylePart(part = {}, index = 0, skill = '') {
  const partNumber = Number(part.partNumber || part.part || index + 1);
  const start = TOEIC_PART_START[skill]?.[partNumber] || Number(part.startQuestion || 1);
  const sourceQuestions = Array.isArray(part.questions) && part.questions.length
    ? part.questions
    : Array.from({ length: Number(part.questionCount || 0) }, (_, questionIndex) => ({ number: start + questionIndex }));
  const questions = sourceQuestions.map((question, questionIndex) => normalizeToeicStyleQuestion(question, question.number || start + questionIndex));
  return {
    ...part,
    key: part.key || `toeic_${skill.toLowerCase()}_${partNumber}`,
    partNumber,
    title: part.title || `Part ${partNumber}`,
    questionRange: questions.length ? `Questions ${questions[0].number}-${questions[questions.length - 1].number}` : '',
    passage: part.passage || {
      title: part.title || `Part ${partNumber}`,
      paragraphs: part.description || part.instructions ? [{ text: part.description || part.instructions }] : [],
    },
    questionGroups: [{
      title: part.groupTitle || part.title || `Part ${partNumber}`,
      instructions: part.instructions || '',
      type: part.type || 'single_choice',
      questions,
    }],
  };
}

function normalizeToeicStyleQuestion(question = {}, fallbackNumber) {
  const options = Array.isArray(question.options) && question.options.length
    ? question.options.map((option, index) => normalizeToeicStyleOption(option, index))
    : ['A', 'B', 'C', 'D'].map((option, index) => normalizeToeicStyleOption(option, index));
  return {
    ...question,
    number: Number(question.number || question.id || fallbackNumber),
    prompt: question.prompt || question.question || question.text || `Câu ${fallbackNumber}`,
    options,
  };
}

function normalizeToeicStyleOption(option, index) {
  if (option && typeof option === 'object') {
    const value = String(option.value || option.key || String.fromCharCode(65 + index)).trim();
    return { value, label: option.label || option.text || value };
  }
  const text = String(option || '').trim();
  const match = text.match(/^([A-D])[\).:\s-]*(.*)$/i);
  if (match) {
    return { value: match[1].toUpperCase(), label: match[2] || match[1].toUpperCase() };
  }
  const value = String.fromCharCode(65 + index);
  return { value, label: text || value };
}
