import { useEffect, useMemo, useState } from 'react';
import { BookOpen, CheckCircle2, FileQuestion, Headphones, Mic, PenLine, Search } from 'lucide-react';
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

const skillOptions = [
  { label: 'Tất cả kỹ năng', value: 'ALL' },
  { label: 'Listening', value: 'LISTENING' },
  { label: 'Reading', value: 'READING' },
  { label: 'Writing', value: 'WRITING' },
  { label: 'Speaking', value: 'SPEAKING' },
  { label: 'Tổng hợp', value: 'MIXED' },
];

const examOptions = [
  { label: 'Tất cả kỳ thi', value: 'ALL' },
  { label: 'IELTS', value: 'IELTS' },
  { label: 'TOEIC', value: 'TOEIC' },
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
  LISTENING: { label: 'Listening', icon: Headphones },
  READING: { label: 'Reading', icon: BookOpen },
  WRITING: { label: 'Writing', icon: PenLine },
  SPEAKING: { label: 'Speaking', icon: Mic },
  MIXED: { label: 'Mock tổng hợp', icon: FileQuestion },
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
  const [examType, setExamType] = useState('ALL');
  const [skill, setSkill] = useState('ALL');
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

  const filteredTests = useMemo(() => {
    const query = keyword.trim().toLowerCase();
    return tests.filter((item) => {
      const matchesExam = examType === 'ALL' || resolveExamType(item) === examType;
      const matchesSkill = skill === 'ALL' || item.skill === skill;
      const matchesKeyword = !query || [item.title, item.description, item.skill, item.type]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(query));
      return matchesExam && matchesSkill && matchesKeyword;
    });
  }, [examType, tests, keyword, skill]);

  const { page, setPage, totalPages, pageItems: paginatedTests, totalItems } = usePagination(
    filteredTests,
    6,
    `mock-tests-${examType}-${skill}-${keyword}`
  );

  const startTest = async (item) => {
    setError('');
    setResult(null);
    try {
      const detail = await mockTestApi.getMockTest(item.id);
      const config = parseJson(detail.uiConfigJson);
      const resolved = resolveMockConfig(detail, config);
      setActiveTest(detail);
      setActiveConfig(resolved.config);
      setActiveSkill(resolved.skill);
    } catch (requestError) {
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
    <div className="flex min-h-[100dvh] flex-col bg-[#f8f4f1]">
      <Header />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-10">
        <section className="rounded-[32px] border border-[#dfbfbd]/40 bg-white p-6 shadow-xl md:p-9">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.2em] text-[#8a0018]">Mock Test</p>
              <h1 className="mt-3 font-['Manrope'] text-4xl font-black text-[#341c1d]">Ngân hàng đề thi thử</h1>
              <p className="mt-3 max-w-3xl text-sm leading-7 text-[#584140]">
                Chọn đề IELTS, TOEIC hoặc từng kỹ năng để luyện trong giao diện thi. Các đề được lấy từ ngân hàng đề thi thử đã xuất bản.
              </p>
            </div>
            <div className="grid gap-3 sm:grid-cols-2 lg:w-[680px] lg:grid-cols-[minmax(240px,1fr)_170px_190px]">
              <div className="relative">
                <Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[#8b706e]" />
                <input
                  className="h-12 w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb] pl-11 pr-4 text-sm outline-none focus:border-[#8a0018]"
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="Tìm đề thi thử..."
                  value={keyword}
                />
              </div>
              <BrandedSelect onChange={(event) => setExamType(event.target.value)} options={examOptions} value={examType} />
              <BrandedSelect onChange={(event) => setSkill(event.target.value)} options={skillOptions} value={skill} />
            </div>
          </div>

          {error ? <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700">{error}</div> : null}
          {result ? <MockResult result={result} /> : null}

          {loading ? (
            <BrandLoadingState className="mt-8" message="Đang tải ngân hàng đề thi thử..." />
          ) : filteredTests.length ? (
            <div className="space-y-6">
              <div className="mt-8 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
                {paginatedTests.map((item) => {
                  const meta = skillMeta[item.skill] || skillMeta.MIXED;
                  const Icon = meta.icon;
                  return (
                    <article className="flex min-h-[260px] flex-col rounded-2xl border border-[#ead7d5] bg-[#fffdfc] p-5 transition hover:border-[#8a0018]/35 hover:shadow-md" key={item.id}>
                      <div className="flex items-start justify-between gap-3">
                        <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff0f1] text-[#8a0018]">
                          <Icon className="h-5 w-5" />
                        </span>
                        <div className="flex flex-wrap justify-end gap-2">
                          <span className="rounded-full border border-[#ead7d5] bg-white px-3 py-1 text-xs font-black text-[#8a0018]">{resolveExamType(item)}</span>
                          <span className="rounded-full bg-[#fff0f1] px-3 py-1 text-xs font-black text-[#8a0018]">{meta.label}</span>
                        </div>
                      </div>
                      <h2 className="mt-4 font-['Manrope'] text-xl font-black text-[#341c1d]">{item.title}</h2>
                      <p className="mt-2 line-clamp-3 flex-1 text-sm leading-6 text-[#584140]">{item.description || item.instructions || 'Đề thi thử đã sẵn sàng.'}</p>
                      <div className="mt-4 flex flex-wrap gap-2 text-xs font-bold text-[#8b706e]">
                        <span>{item.timeLimitMinutes ? `${item.timeLimitMinutes} phút` : 'Không giới hạn thời gian'}</span>
                        <span>·</span>
                        <span>{item.maxScore ? `${item.maxScore} điểm` : 'Thi thử'}</span>
                      </div>
                      <button className="mt-5 rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-black text-white transition hover:bg-[#650012]" onClick={() => startTest(item)} type="button">
                        Vào thi thử
                      </button>
                    </article>
                  );
                })}
              </div>

              {filteredTests.length > 6 && (
                <div className="flex justify-end">
                  <Pagination
                    page={page}
                    totalPages={totalPages}
                    onChange={setPage}
                    totalItems={totalItems}
                    pageSize={6}
                  />
                </div>
              )}
            </div>
          ) : (
            <div className="mt-8 rounded-2xl border border-dashed border-[#dfbfbd] bg-[#fffafb] p-10 text-center text-sm font-bold text-[#584140]">
              Chưa có đề thi thử phù hợp bộ lọc.
            </div>
          )}
        </section>
      </main>
      <CourseFooter />
    </div>
  );
}

function MockResult({ result }) {
  return (
    <div className="mt-6 rounded-2xl border border-emerald-200 bg-emerald-50 p-5 text-sm text-emerald-900">
      <div className="flex items-start gap-3">
        <CheckCircle2 className="mt-0.5 h-5 w-5 text-emerald-700" />
        <div>
          <p className="font-black">{result.title}</p>
          {result.total != null ? (
            <p className="mt-1">Kết quả: {result.correct}/{result.total} câu đúng{result.percent != null ? ` · ${Number(result.percent).toFixed(0)}%` : ''}{result.score != null ? ` · ${result.score} điểm` : ''}.</p>
          ) : (
            <p className="mt-1">{result.message}</p>
          )}
          {result.submittedAt ? <p className="mt-1 text-xs font-semibold opacity-80">Đã lưu lúc {new Date(result.submittedAt).toLocaleString('vi-VN')}</p> : null}
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
