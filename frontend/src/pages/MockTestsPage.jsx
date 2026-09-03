import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, BookOpen, Headphones, Mic, PenLine, Search } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import mockTestApi from '../api/mockTestApi';
import placementTestApi from '../api/placementTestApi';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import MockSkillPack from '../components/mock-tests/MockSkillPack';
import MockYearBook from '../components/mock-tests/MockYearBook';
import HomeworkAiFeedbackPanel from '../components/classroom/HomeworkAiFeedbackPanel';
import BrandLoadingState from '../components/ui/BrandLoadingState';
import useMockTestSession from '../hooks/useMockTestSession';
import {
  buildMockLibrary,
  filterLibrary,
  monthProgress,
  monthSkillPresence,
  SKILL_ORDER,
  TOEIC_SKILL_ORDER,
  splitMockTests,
} from '../utils/mockTestLibrary';

const SKILL_MARK = {
  LISTENING: Headphones,
  READING: BookOpen,
  WRITING: PenLine,
  SPEAKING: Mic,
};

function MockResult({ result }) {
  if (!result) return null;
  return (
    <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900">
      <p className="font-extrabold">{result.title}</p>
      {result.total != null ? (
        <p className="mt-1 text-xs">
          {result.correct}/{result.total} câu đúng
          {result.percent != null ? ` · ${Number(result.percent).toFixed(0)}%` : ''}
          {result.score != null ? ` · ${result.score}` : ''}.
        </p>
      ) : (
        <>
          <p className="mt-1 text-xs">
            {result.score != null ? `Điểm: ${result.score}. ` : ''}{result.message}
          </p>
          {result.aiFeedbackJson ? (
            <div className="mt-3 text-left">
              <HomeworkAiFeedbackPanel value={result.aiFeedbackJson} />
            </div>
          ) : null}
        </>
      )}
    </div>
  );
}

export default function MockTestsPage() {
  const { exam, year, monthKey } = useParams();
  const examType = String(exam || '').toLowerCase() === 'toeic' ? 'TOEIC' : 'IELTS';
  const [tests, setTests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [keyword, setKeyword] = useState('');
  const [hasPlacement, setHasPlacement] = useState(false);
  const session = useMockTestSession();

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      setLoadError('');
      try {
        const [data, placement] = await Promise.all([
          mockTestApi.listMockTests(),
          placementTestApi.getCurrent().catch(() => null),
        ]);
        if (!active) return;
        setTests(Array.isArray(data) ? data : []);
        setHasPlacement(Boolean(placement?.latestAttempt));
      } catch (requestError) {
        if (active) setLoadError(requestError?.response?.data?.message || 'Không tải được ngân hàng đề thi thử.');
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => { active = false; };
  }, []);

  const { practiceTests } = useMemo(() => splitMockTests(tests), [tests]);
  const ieltsLibrary = useMemo(() => buildMockLibrary(tests, 'IELTS'), [tests]);
  const toeicLibrary = useMemo(() => buildMockLibrary(tests, 'TOEIC'), [tests]);
  const fullLibrary = examType === 'TOEIC' ? toeicLibrary : ieltsLibrary;
  const overviewLibrary = useMemo(
    () => [...filterLibrary(ieltsLibrary, { keyword }), ...filterLibrary(toeicLibrary, { keyword })],
    [ieltsLibrary, toeicLibrary, keyword]
  );
  const overviewLibraries = useMemo(() => [...ieltsLibrary, ...toeicLibrary], [ieltsLibrary, toeicLibrary]);
  const selectedYear = fullLibrary.find((entry) => String(entry.year) === String(year));
  const selectedMonth = selectedYear?.months.find((month) => month.monthKey === monthKey);
  const scores = session.completedScoresMap;

  const overview = useMemo(() => {
    let packs = 0;
    let completed = 0;
    let total = 0;
    overviewLibraries.forEach((yearEntry) => {
      yearEntry.months.forEach((month) => {
        packs += month.packs.length;
        const progress = monthProgress(month, scores);
        completed += progress.completed;
        total += progress.total;
      });
    });
    return {
      years: overviewLibraries.length,
      packs,
      completed,
      total,
      percent: total ? Math.round((completed * 100) / total) : 0,
    };
  }, [overviewLibraries, scores]);

  const continueMonth = useMemo(() => {
    for (const yearEntry of overviewLibraries) {
      for (const month of yearEntry.months) {
        const progress = monthProgress(month, scores);
        if (progress.total > 0 && progress.completed < progress.total) {
          return {
            exam: yearEntry.examType === 'TOEIC' ? 'toeic' : 'ielts',
            year: yearEntry.year,
            month,
            progress,
          };
        }
      }
    }
    return null;
  }, [overviewLibraries, scores]);

  const scrollToYear = (examKey, targetYear) => {
    const node = document.getElementById(`mock-year-${examKey}-${targetYear}`);
    node?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  if (session.examView) return session.examView;

  if (year && monthKey) {
    return (
      <LearnerPageShell
        title={selectedMonth ? `${examType} · ${selectedMonth.monthLabel} ${selectedYear.year}` : `Đề thi thử ${examType}`}
        description={selectedMonth ? `${selectedMonth.packs.length} đề thi thử ${examType}.` : ''}
        actions={(
          <Link className="inline-flex items-center gap-2 text-sm font-extrabold text-[#730014]" to="/mock-tests">
            <ArrowLeft className="h-4 w-4" />
            Quay lại thư viện
          </Link>
        )}
      >
        <div className="flex flex-1 flex-col space-y-5">
          {session.error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-xs font-bold text-rose-700">{session.error}</div> : null}
          <MockResult result={session.result} />
          {loading ? (
            <BrandLoadingState className="flex-1 py-16" message="Đang tải đề thi thử..." />
          ) : selectedMonth ? (
            selectedMonth.packs.map((pack) => (
              <MockSkillPack
                completedScoresMap={session.completedScoresMap}
                heading={`${selectedMonth.monthLabel} · ${pack.title}`}
                examType={examType}
                key={pack.testNumber}
                onStart={session.startTest}
                pack={pack}
              />
            ))
          ) : (
            <div className="flex flex-1 items-center justify-center rounded-2xl border border-dashed border-[#ead9db] bg-[#fffaf9] p-12 text-center text-sm font-bold text-[#564241]">
              Không tìm thấy đề của tháng này.
            </div>
          )}
        </div>
      </LearnerPageShell>
    );
  }

  return (
    <LearnerPageShell
      eyebrow="Thư viện đề thi thử"
      title="Đề thi thử IELTS và TOEIC"
      description="Chọn sách IELTS hoặc TOEIC, rồi mở đề để làm bài."
      actions={continueMonth ? (
        <Link
          className="inline-flex items-center rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-extrabold text-white hover:bg-[#4b0009]"
          to={`/mock-tests/${continueMonth.exam}/${continueMonth.year}/${continueMonth.month.monthKey}`}
        >
          Tiếp tục {continueMonth.month.monthLabel} {continueMonth.year}
        </Link>
      ) : null}
    >
      <div className="flex flex-1 flex-col space-y-6">
        <div className="grid gap-3 sm:grid-cols-3">
          <div className="rounded-2xl border border-[#eadcdc] bg-white px-4 py-4">
            <p className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500">Năm</p>
            <p className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#0b1c30]">{overview.years}</p>
          </div>
          <div className="rounded-2xl border border-[#eadcdc] bg-white px-4 py-4">
            <p className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500">Đề</p>
            <p className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#0b1c30]">{overview.packs}</p>
          </div>
          <div className="rounded-2xl border border-[#eadcdc] bg-white px-4 py-4">
            <p className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500">Đã làm</p>
            <p className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#0b1c30]">
              {overview.total ? `${overview.completed}/${overview.total}` : '0'}
            </p>
            {overview.total ? (
              <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-[#f3e8e8]">
                <div className="h-full rounded-full bg-[#730014]" style={{ width: `${overview.percent}%` }} />
              </div>
            ) : null}
          </div>
        </div>

        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div className="relative w-full max-w-md">
            <input
              className="h-10 w-full rounded-full border border-[#ead9db] bg-white pl-4 pr-10 text-sm outline-none focus:border-[#730014]"
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Tìm tháng hoặc số đề..."
              value={keyword}
            />
            <Search className="pointer-events-none absolute right-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[#8b706e]" />
          </div>
          {overviewLibrary.length > 1 ? (
            <div className="flex flex-wrap gap-2">
              {overviewLibrary.map((yearEntry) => (
                <button
                  className="rounded-full border border-[#ead9db] bg-white px-4 py-1.5 text-xs font-extrabold text-[#564241] hover:border-[#730014] hover:text-[#730014]"
                  key={`${yearEntry.examType}-${yearEntry.year}`}
                  onClick={() => scrollToYear(yearEntry.examType, yearEntry.year)}
                  type="button"
                >
                  {yearEntry.examType} {yearEntry.year}
                </button>
              ))}
            </div>
          ) : null}
        </div>

        {loadError || session.error ? (
          <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-xs font-bold text-rose-700">
            {loadError || session.error}
          </div>
        ) : null}
        <MockResult result={session.result} />

        {!hasPlacement ? (
          <Link
            className="flex items-center justify-between gap-4 rounded-2xl border border-[#eadcdc] bg-white px-5 py-4 hover:border-[#730014]"
            to="/placement-test"
          >
            <div>
              <p className="font-['Manrope'] text-sm font-extrabold text-[#0b1c30]">Đánh giá đầu vào</p>
              <p className="mt-1 text-xs text-[#8b706e]">IELTS 4 kỹ năng hoặc TOEIC.</p>
            </div>
            <span className="shrink-0 text-sm font-extrabold text-[#730014]">Bắt đầu</span>
          </Link>
        ) : practiceTests.length ? (
          <Link
            className="flex items-center justify-between gap-4 rounded-2xl border border-[#eadcdc] bg-white px-5 py-4 hover:border-[#730014]"
            to="/mock-tests/practice"
          >
            <div>
              <p className="font-['Manrope'] text-sm font-extrabold text-[#0b1c30]">Đề luyện thêm</p>
              <p className="mt-1 text-xs text-[#8b706e]">{practiceTests.length} đề luyện thêm.</p>
            </div>
            <span className="shrink-0 text-sm font-extrabold text-[#730014]">Mở đề</span>
          </Link>
        ) : null}

        {loading ? (
          <BrandLoadingState className="flex-1 py-16" message="Đang tải đề thi thử..." />
        ) : overviewLibrary.length ? (
          <div className="space-y-6">
            {overviewLibrary.map((yearEntry) => {
              const packCount = yearEntry.months.reduce((sum, month) => sum + month.packs.length, 0);
              const examKey = yearEntry.examType === 'TOEIC' ? 'toeic' : 'ielts';
              const examSkills = yearEntry.examType === 'TOEIC' ? TOEIC_SKILL_ORDER : SKILL_ORDER;
              return (
                <article
                  className="scroll-mt-6 overflow-visible rounded-[28px] border border-[#eadcdc] bg-white p-5 sm:p-8"
                  id={`mock-year-${yearEntry.examType}-${yearEntry.year}`}
                  key={`${yearEntry.examType}-${yearEntry.year}`}
                >
                  <div className="grid items-center gap-6 lg:grid-cols-[240px_minmax(0,1fr)]">
                    <MockYearBook examType={yearEntry.examType} year={yearEntry.year} />
                    <div className="min-w-0 space-y-4">
                      <div>
                        <h2 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">{yearEntry.title}</h2>
                        <p className="mt-1 text-sm text-[#8b706e]">
                          {yearEntry.months.length} {yearEntry.examType === 'TOEIC' ? 'bộ đề' : 'tháng'} · {packCount} đề
                        </p>
                      </div>
                      <div className="grid gap-3 sm:grid-cols-2">
                        {yearEntry.months.map((month) => {
                          const progress = monthProgress(month, scores);
                          const presentSkills = new Set(monthSkillPresence(month));
                          return (
                            <Link
                              className="rounded-2xl border border-[#eadcdc] bg-[#fffaf9] px-4 py-4 text-left transition hover:border-[#730014]"
                              key={month.monthKey}
                              to={`/mock-tests/${examKey}/${yearEntry.year}/${month.monthKey}`}
                            >
                              <div className="flex items-start justify-between gap-3">
                                <div>
                                  <p className="font-['Manrope'] text-sm font-extrabold text-[#0b1c30]">{month.monthLabel}</p>
                                  <p className="mt-1 text-xs text-[#8b706e]">
                                    {month.packs.length} đề · {month.packs.map((pack) => pack.title).join(' · ')}
                                  </p>
                                </div>
                                {progress.total ? (
                                  <span className="shrink-0 text-xs font-bold text-[#730014]">{progress.percent}%</span>
                                ) : null}
                              </div>
                              <div className="mt-3 flex items-center gap-1.5">
                                {examSkills.map((skill) => {
                                  const Mark = SKILL_MARK[skill];
                                  const available = presentSkills.has(skill);
                                  const done = (month.packs || []).some((pack) => {
                                    const item = pack.skills[skill];
                                    return item && scores[item.id];
                                  });
                                  return (
                                    <span
                                      className={`inline-flex h-7 w-7 items-center justify-center rounded-full ${
                                        done
                                          ? 'bg-[#730014] text-white'
                                          : available
                                            ? 'bg-white text-[#730014] ring-1 ring-[#eadcdc]'
                                            : 'bg-[#f3ecec] text-[#c4b4b3]'
                                      }`}
                                      key={skill}
                                      title={skill}
                                    >
                                      <Mark className="h-3 w-3" />
                                    </span>
                                  );
                                })}
                              </div>
                              {progress.total ? (
                                <div className="mt-3 h-1 overflow-hidden rounded-full bg-[#f3e8e8]">
                                  <div className="h-full rounded-full bg-[#730014]" style={{ width: `${progress.percent}%` }} />
                                </div>
                              ) : null}
                            </Link>
                          );
                        })}
                      </div>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        ) : (
          <div className="flex flex-1 items-center justify-center rounded-2xl border border-dashed border-[#ead9db] bg-[#fffaf9] p-12 text-center text-sm font-bold text-[#564241]">
            Chưa có đề thi thử phù hợp.
          </div>
        )}
      </div>
    </LearnerPageShell>
  );
}
