import { useEffect, useMemo, useRef, useState } from 'react';
import { BarChart3, CheckCircle2, Headphones, Layers3, LoaderCircle, RefreshCw, Save, Target, Users } from 'lucide-react';
import placementTestApi from '../../api/placementTestApi';
import AssessmentExamBuilder from '../../components/content-manager/AssessmentExamBuilder';
import { ManagerFilterBar, ManagerTaxonomyBadge } from '../../components/content-manager/ManagerListUi';
import { Panel } from '../../components/content-manager/ContentManagerUi';
import ManagementToast from '../../components/ui/ManagementToast';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import { getContentManagerError, getContentManagerFeedbackMessage } from '../../utils/contentManagerFeedback';

const RECENT_ATTEMPTS_PAGE_SIZE = 10;

const TABS = [
  { key: 'monitoring', label: 'Kết quả' },
  { key: 'scoring', label: 'Cách chấm' },
  { key: 'listening', label: 'Nghe', examTypes: ['IELTS', 'SKILL'] },
  { key: 'reading', label: 'Đọc', examTypes: ['IELTS', 'SKILL'] },
  { key: 'writing', label: 'Viết', examTypes: ['IELTS', 'SKILL'] },
  { key: 'speaking', label: 'Nói', examTypes: ['IELTS', 'SKILL'] },
  { key: 'toeic', label: 'Đề TOEIC', examTypes: ['TOEIC'] },
];

const TEST_TYPES = [
  {
    key: 'IELTS',
    enabledField: 'ieltsEnabled',
    label: 'IELTS Placement',
    summary: 'Đánh giá đầy đủ Listening, Reading, Writing và Speaking.',
    icon: Layers3,
    accent: 'border-[#8a0018] bg-[#fff7f7] text-[#730014]',
    badge: '4 kỹ năng',
  },
  {
    key: 'TOEIC',
    enabledField: 'toeicEnabled',
    label: 'TOEIC Placement',
    summary: 'Listening và Reading theo cấu trúc 7 part, chấm theo đáp án.',
    icon: Headphones,
    accent: 'border-[#21446d] bg-[#f5f9ff] text-[#21446d]',
    badge: '2 phần · 7 part',
  },
  {
    key: 'SKILL',
    enabledField: 'skillAssessmentEnabled',
    label: 'Đánh giá kỹ năng',
    summary: 'Học viên tự chọn một hoặc nhiều kỹ năng cần kiểm tra.',
    icon: Target,
    accent: 'border-[#63368f] bg-[#faf7ff] text-[#63368f]',
    badge: 'Chọn từng kỹ năng',
  },
];

const TOEIC_PARTS = [
  ['Part 1', 'Photographs', 6],
  ['Part 2', 'Question-Response', 25],
  ['Part 3', 'Conversations', 39],
  ['Part 4', 'Talks', 30],
  ['Part 5', 'Incomplete Sentences', 30],
  ['Part 6', 'Text Completion', 16],
  ['Part 7', 'Reading Comprehension', 54],
];

const TOEIC_SECTION_TABS = [
  { key: 'listening', label: 'Listening', skill: 'LISTENING' },
  { key: 'reading', label: 'Reading', skill: 'READING' },
];

const parseConfig = (value, fallback = {}) => {
  try {
    const parsed = JSON.parse(String(value || ''));
    return parsed && typeof parsed === 'object' ? parsed : fallback;
  } catch {
    return fallback;
  }
};

export default function ContentManagerPlacementTestPage() {
  const [definition, setDefinition] = useState(null);
  const [activeExamType, setActiveExamType] = useState('IELTS');
  const [activeTab, setActiveTab] = useState('monitoring');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [monitoring, setMonitoring] = useState(null);
  const [monitoringLoading, setMonitoringLoading] = useState(true);
  const [monitoringExamType, setMonitoringExamType] = useState('IELTS');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const monitoringRequestRef = useRef(0);

  useEffect(() => {
    let active = true;
    const loadPlacementTest = async () => {
      try {
        const [response, monitoringResponse] = await Promise.all([
          placementTestApi.getManagedDefinition(),
          placementTestApi.getMonitoring(monitoringExamType),
        ]);
        if (!active) return;
        setDefinition(toDraft(response));
        setMonitoring(monitoringResponse);
      } catch (requestError) {
        if (active) setError(getContentManagerError(requestError, 'Không tải được dữ liệu bài đánh giá đầu vào.'));
      } finally {
        if (!active) return;
        setLoading(false);
        setMonitoringLoading(false);
      }
    };

    loadPlacementTest();
    return () => { active = false; };
  }, []);

  const visibleTabs = useMemo(() => {
    return TABS.filter((tab) => !tab.examTypes || tab.examTypes.includes(activeExamType));
  }, [activeExamType]);

  useEffect(() => {
    if (!visibleTabs.some((tab) => tab.key === activeTab)) {
      setActiveTab('monitoring');
    }
  }, [activeTab, visibleTabs]);

  const selectExamType = (examType) => {
    setActiveExamType(examType);
    setActiveTab('monitoring');
    setError('');
    setNotice('');
    if (monitoringExamType !== examType) refreshMonitoring(examType);
  };

  const updateDefinition = (field, value) => setDefinition((current) => ({ ...current, [field]: value }));
  const updateConfig = (skill, updater) => setDefinition((current) => ({
    ...current,
    [skill]: typeof updater === 'function' ? updater(current[skill]) : updater,
  }));

  const applyObjectiveChange = (skill, field, value) => {
    updateConfig(skill, (current) => {
      if (field === 'uiConfigJson') {
        const next = parseConfig(value, current);
        return { ...next, answerKey: current.answerKey || {} };
      }
      if (field === 'objectiveAnswerKey') {
        return { ...current, answerKey: parseConfig(value, {}) };
      }
      return current;
    });
  };

  const applySubjectiveChange = (skill, field, value) => {
    updateConfig(skill, (current) => {
      if (field === 'uiConfigJson') {
        return parseConfig(value, current);
      }
      if (field === 'timeLimitMinutes') {
        return { ...current, durationMinutes: Number(value || 0) };
      }
      return current;
    });
  };

  const save = async () => {
    if (!definition) return;
    setSaving(true);
    setError('');
    setNotice('');
    try {
      const response = await placementTestApi.saveManagedDefinition(toPayload(definition));
      setDefinition(toDraft(response));
      setNotice('Đã lưu cấu hình bài đánh giá đầu vào.');
    } catch (requestError) {
      setError(getContentManagerError(requestError, 'Không thể lưu cấu hình bài đánh giá đầu vào.'));
    } finally {
      setSaving(false);
    }
  };

  const refreshMonitoring = async (examType = monitoringExamType) => {
    const requestId = monitoringRequestRef.current + 1;
    monitoringRequestRef.current = requestId;
    setMonitoringLoading(true);
    try {
      const response = await placementTestApi.getMonitoring(examType);
      if (monitoringRequestRef.current !== requestId) return;
      setMonitoring(response);
      setMonitoringExamType(examType);
    } catch (requestError) {
      if (monitoringRequestRef.current !== requestId) return;
      setError(getContentManagerError(requestError, 'Không tải được dữ liệu theo dõi.'));
    } finally {
      if (monitoringRequestRef.current === requestId) setMonitoringLoading(false);
    }
  };

  const applyToeicSectionChange = (sectionKey, field, value) => {
    updateConfig('toeic', (current) => {
      const base = current || buildDefaultToeicConfig();
      const section = base[sectionKey] || {};
      if (field === 'uiConfigJson') {
        const next = parseConfig(value, section);
        delete next.answerKey;
        return {
          ...base,
          [sectionKey]: { ...next, answerKey: section.answerKey || {} },
        };
      }
      if (field === 'objectiveAnswerKey') {
        const sectionAnswerKey = parseConfig(value, {});
        const otherSectionKey = sectionKey === 'listening' ? 'reading' : 'listening';
        const otherAnswerKey = base[otherSectionKey]?.answerKey || {};
        return {
          ...base,
          [sectionKey]: { ...section, answerKey: sectionAnswerKey },
          answerKey: { ...otherAnswerKey, ...sectionAnswerKey },
        };
      }
      if (field === 'timeLimitMinutes') {
        return {
          ...base,
          [sectionKey]: { ...section, durationMinutes: Number(value || 0) },
        };
      }
      return base;
    });
  };

  if (loading) {
    return <Panel className="flex min-h-[420px] items-center justify-center gap-3 text-sm font-semibold text-[#584140]"><LoaderCircle className="h-5 w-5 animate-spin text-[#730014]" /> Đang tải bài đánh giá đầu vào...</Panel>;
  }
  if (!definition) {
    return (
      <>
        <ManagementToast message={error} onClose={() => setError('')} />
        <Panel className="min-h-[320px] p-6 text-sm font-semibold text-[#93000a]">
          {getContentManagerFeedbackMessage(error) || 'Không có dữ liệu để quản lý.'}
        </Panel>
      </>
    );
  }

  return (
    <div className="space-y-6">
      <section className="overflow-hidden rounded-[28px] border border-[#eadfdc] bg-[radial-gradient(circle_at_top_right,rgba(138,0,24,0.09),transparent_38%),linear-gradient(135deg,#fffdfb,#f8f2ee)] p-6 lg:p-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div className="max-w-3xl">
            <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Hệ thống đánh giá đầu vào</p>
            <h1 className="mt-2 font-['Manrope'] text-2xl font-extrabold tracking-tight text-[#0b1c30] sm:text-3xl">Ba dạng bài, ba mục tiêu đánh giá</h1>
            <p className="mt-2 text-sm leading-relaxed text-[#8b706e]">Chọn dạng bài để biên soạn nội dung và theo dõi kết quả.</p>
          </div>
          <div className="flex flex-wrap items-center justify-end gap-3">
            <button className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-[#4b0009] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#730014] disabled:opacity-50" disabled={saving} onClick={save} type="button">
              <Save aria-hidden="true" className="h-4 w-4" /> {saving ? 'Đang lưu...' : 'Lưu toàn bộ thay đổi'}
            </button>
          </div>
        </div>

        <div aria-label="Chọn dạng bài đánh giá" className="mt-6 grid gap-3 lg:grid-cols-3" role="tablist">
          {TEST_TYPES.map((testType) => {
            const Icon = testType.icon;
            const selected = activeExamType === testType.key;
            const enabled = Boolean(definition[testType.enabledField]);
            return (
              <section
                className={`min-h-[168px] rounded-2xl border-2 p-5 transition ${selected ? testType.accent : 'border-transparent bg-white/80 text-[#0b1c30] hover:border-[#dfcfcb]'}`}
                key={testType.key}
              >
                <div className="flex items-start justify-between gap-3">
                  <button
                    aria-label={`Mở cấu hình ${testType.label}`}
                    aria-selected={selected}
                    className="flex h-11 w-11 items-center justify-center rounded-xl bg-white shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#730014]"
                    onClick={() => selectExamType(testType.key)}
                    role="tab"
                    type="button"
                  >
                    <Icon aria-hidden="true" className="h-5 w-5" />
                  </button>
                  <label className="inline-flex cursor-pointer items-center gap-2 rounded-full bg-white px-3 py-1.5 text-[11px] font-extrabold uppercase tracking-wider">
                    <input
                      checked={enabled}
                      className="h-4 w-4 accent-[#4b0009]"
                      onChange={(event) => updateDefinition(testType.enabledField, event.target.checked)}
                      type="checkbox"
                    />
                    {enabled ? 'Đang bật' : 'Đang tắt'}
                  </label>
                </div>
                <button className="mt-4 block w-full text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#730014]" onClick={() => selectExamType(testType.key)} type="button">
                  <span className="flex items-center justify-between gap-3">
                    <span className="font-['Manrope'] text-lg font-extrabold">{testType.label}</span>
                    <span className="rounded-full bg-white px-3 py-1 text-[10px] font-extrabold uppercase tracking-wider">{testType.badge}</span>
                  </span>
                  <span className="mt-1 block text-sm leading-6 opacity-80">{testType.summary}</span>
                </button>
              </section>
            );
          })}
        </div>
      </section>

      <ManagerFilterBar>
        <div className="flex min-w-0 flex-1 gap-2 overflow-x-auto">
          {visibleTabs.map((tab) => (
            <button
              className={`shrink-0 rounded-lg px-4 py-2.5 text-sm font-bold transition ${activeTab === tab.key ? 'bg-[#4b0009] text-white' : 'text-[#4b0009] hover:bg-[#eff4ff]'}`}
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              type="button"
            >
              {tab.label}
            </button>
          ))}
        </div>
        <span className="shrink-0 rounded-full border border-[#eadfdc] bg-white px-4 py-2"><ManagerTaxonomyBadge kind="exam" value={activeExamType} /></span>
      </ManagerFilterBar>

      <ManagementToast message={error} onClose={() => setError('')} />
      <ManagementToast message={notice} onClose={() => setNotice('')} tone="success" title="Đã lưu bài đánh giá" />

      {activeTab === 'monitoring' ? (
        <Monitoring
          examType={activeExamType}
          loading={monitoringLoading}
          monitoring={monitoring}
          onRefresh={() => refreshMonitoring(activeExamType)}
        />
      ) : null}
      {activeTab === 'scoring' ? <PlacementScoringGuide examType={activeExamType} /> : null}
      {activeTab === 'listening' ? <ObjectiveEditor label="Bài đánh giá kỹ năng Nghe" skill="LISTENING" config={definition.listening} onChange={(field, value) => applyObjectiveChange('listening', field, value)} /> : null}
      {activeTab === 'reading' ? <ObjectiveEditor label="Bài đánh giá kỹ năng Đọc" skill="READING" config={definition.reading} onChange={(field, value) => applyObjectiveChange('reading', field, value)} /> : null}
      {activeTab === 'writing' ? <SubjectiveEditor config={definition.writing} label="Bài đánh giá kỹ năng Viết" skill="WRITING" onChange={(field, value) => applySubjectiveChange('writing', field, value)} /> : null}
      {activeTab === 'speaking' ? <SubjectiveEditor config={definition.speaking} label="Bài đánh giá kỹ năng Nói" skill="SPEAKING" onChange={(field, value) => applySubjectiveChange('speaking', field, value)} /> : null}
      {activeTab === 'toeic' ? <ToeicEditor config={definition.toeic} onChangeSection={applyToeicSectionChange} onReset={() => updateConfig('toeic', buildDefaultToeicConfig())} /> : null}
    </div>
  );
}

const IELTS_LISTENING_BANDS = [
  ['39–40', '9.0'], ['37–38', '8.5'], ['35–36', '8.0'], ['33–34', '7.5'], ['30–32', '7.0'],
  ['27–29', '6.5'], ['23–26', '6.0'], ['20–22', '5.5'], ['16–19', '5.0'], ['13–15', '4.5'],
  ['10–12', '4.0'], ['7–9', '3.5'], ['5–6', '3.0'], ['3–4', '2.5'], ['0–2', '0'],
];

const IELTS_READING_BANDS = [
  ['40', '9.0'], ['39', '8.5'], ['38', '8.0'], ['36–37', '7.5'], ['34–35', '7.0'],
  ['32–33', '6.5'], ['30–31', '6.0'], ['27–29', '5.5'], ['23–26', '5.0'], ['19–22', '4.5'],
  ['15–18', '4.0'], ['12–14', '3.5'], ['8–11', '3.0'], ['5–7', '2.5'], ['0–4', '0'],
];

function PlacementScoringGuide({ examType }) {
  if (examType === 'TOEIC') {
    return (
      <div className="space-y-6">
        <ScoringIntro title="Cách tính điểm TOEIC" description="Listening và Reading được chấm theo đáp án, sau đó quy đổi độc lập trước khi cộng tổng." />
        <div className="grid gap-5 lg:grid-cols-2">
          <ScoringCard title="Quy đổi từng kỹ năng">
            <p>Mỗi phần được quy đổi từ tỷ lệ câu đúng sang thang <strong>5–495</strong> và làm tròn đến 5 điểm gần nhất.</p>
            <p className="mt-3 rounded-xl bg-slate-50 px-4 py-3 font-bold text-[#0b1c30]">Tổng điểm = Listening + Reading · Tối đa 990</p>
          </ScoringCard>
          <ScoringCard title="Mức trình độ gợi ý">
            <ScoreThreshold label="Beginner" value="Dưới 450" />
            <ScoreThreshold label="Intermediate" value="450–699" />
            <ScoreThreshold label="Advanced" value="Từ 700" />
          </ScoringCard>
        </div>
      </div>
    );
  }

  const diagnosticOnly = examType === 'SKILL';
  return (
    <div className="space-y-6">
      <ScoringIntro
        title={diagnosticOnly ? 'Cách chấm đánh giá kỹ năng' : 'Cách tính band IELTS đầu vào'}
        description={diagnosticOnly ? 'Chỉ các kỹ năng học viên chọn mới được chấm; kết quả dùng để chẩn đoán năng lực.' : 'Bốn kỹ năng được chấm riêng trên thang 0–9 và làm tròn theo nửa band.'}
      />
      <div className="grid gap-5 xl:grid-cols-2">
        <BandTable rows={IELTS_LISTENING_BANDS} title="Listening" />
        <BandTable rows={IELTS_READING_BANDS} title="Reading" />
      </div>
      <div className="grid gap-5 xl:grid-cols-2">
        <ScoringCard title="Writing">
          <CriteriaGrid items={['Task response', 'Coherence', 'Lexical resource', 'Grammar']} />
          <p className="mt-4">Bài quá ngắn hoặc không có nội dung có thể bị giới hạn band. Kết quả được làm tròn theo nửa band.</p>
        </ScoringCard>
        <ScoringCard title="Speaking">
          <CriteriaGrid items={['Fluency & coherence', 'Lexical resource', 'Grammar', 'Pronunciation']} />
          <p className="mt-4">Ưu tiên đánh giá bản ghi âm. Nếu không đủ bằng chứng nói thật, kỹ năng này chưa được tính điểm.</p>
        </ScoringCard>
      </div>
      <ScoringCard title={diagnosticOnly ? 'Kết quả chẩn đoán' : 'Band tổng và xếp lớp'}>
        <p><strong>Band tổng</strong> là trung bình các kỹ năng có kết quả, sau đó làm tròn đến 0,5 gần nhất.</p>
        <p className="mt-2">{diagnosticOnly ? 'Kết quả này không tự động xác nhận trình độ xếp lớp.' : 'Nhân viên cần rà soát Writing, Speaking và xác nhận trình độ cuối cùng trước khi dùng kết quả để xếp lớp.'}</p>
      </ScoringCard>
    </div>
  );
}

function ScoringIntro({ description, title }) {
  return (
    <section className="rounded-2xl border border-[#eadfdc] bg-[linear-gradient(135deg,#fffdfb,#fff5f5)] p-6">
      <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Quy tắc đánh giá</p>
      <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#0b1c30]">{title}</h2>
      <p className="mt-2 text-sm leading-6 text-[#584140]">{description}</p>
    </section>
  );
}

function ScoringCard({ children, title }) {
  return (
    <Panel className="p-6">
      <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">{title}</h3>
      <div className="mt-4 text-sm leading-6 text-[#584140]">{children}</div>
    </Panel>
  );
}

function BandTable({ rows, title }) {
  return (
    <ScoringCard title={`${title}: số câu đúng → band`}>
      <div className="grid grid-cols-3 gap-2 sm:grid-cols-5">
        {rows.map(([correct, band]) => (
          <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-center" key={correct}>
            <p className="text-xs font-bold text-slate-500">{correct} câu</p>
            <p className="mt-1 font-extrabold text-[#730014]">{band}</p>
          </div>
        ))}
      </div>
    </ScoringCard>
  );
}

function CriteriaGrid({ items }) {
  return <div className="grid gap-2 sm:grid-cols-2">{items.map((item) => <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 font-bold text-[#0b1c30]" key={item}>{item}</div>)}</div>;
}

function ScoreThreshold({ label, value }) {
  return <div className="flex items-center justify-between border-b border-slate-100 py-2 last:border-0"><span className="font-bold text-[#0b1c30]">{label}</span><span>{value}</span></div>;
}

function Monitoring({ examType = 'IELTS', loading, monitoring, onRefresh }) {
  const isToeic = examType === 'TOEIC';
  const distribution = monitoring?.bandDistribution || [];
  const recentAttempts = monitoring?.recentAttempts || [];
  const {
    page: attemptsPage,
    setPage: setAttemptsPage,
    totalPages: attemptsTotalPages,
    pageItems: paginatedAttempts,
    totalItems: attemptsTotalItems,
  } = usePagination(recentAttempts, RECENT_ATTEMPTS_PAGE_SIZE, examType);
  const maximum = Math.max(...distribution.map((item) => Number(item.count || 0)), 1);
  const scoreLabel = isToeic ? 'Điểm' : 'Band';
  const formatScore = (value) => {
    if (value == null) return '—';
    return isToeic ? String(Math.round(Number(value))) : Number(value).toFixed(1);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div><p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Báo cáo</p><h2 className="mt-1 flex items-baseline gap-1.5 font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Kết quả <ManagerTaxonomyBadge kind="exam" value={examType} /></h2></div>
        <button className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm font-bold text-[#730014]" onClick={onRefresh} type="button">
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /> Làm mới số liệu
        </button>
      </div>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MonitorCard icon={Users} label="Người tham gia" value={monitoring?.uniqueParticipants || 0} />
        <MonitorCard icon={BarChart3} label="Tổng lượt làm" value={monitoring?.totalAttempts || 0} />
        <MonitorCard icon={CheckCircle2} label="Đã hoàn thành" value={monitoring?.completedAttempts || 0} />
        <MonitorCard icon={BarChart3} label={`${scoreLabel} tổng trung bình`} value={formatScore(monitoring?.averageOverallBand)} />
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.05fr_.95fr]">
        <Panel className="p-6">
          <h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">
            {isToeic ? 'Phân hóa điểm TOEIC' : 'Phân hóa band đầu vào'}
          </h3>
          <div className="mt-6 space-y-5">
            {distribution.length ? distribution.map((item) => (
              <div className="space-y-2" key={item.label}>
                <div className="flex justify-between text-sm">
                  <span className="font-semibold text-[#4b0009]">{item.label}</span>
                  <span className="font-bold">{item.count}</span>
                </div>
                <div className="h-3 overflow-hidden rounded-full bg-[#f1e3e4]">
                  <div className="h-full rounded-full bg-[#730014]" style={{ width: `${item.count ? Math.max((Number(item.count) / maximum) * 100, 8) : 0}%` }} />
                </div>
              </div>
            )) : <p className="text-sm text-[#584140]">Chưa có lượt làm để phân tích.</p>}
          </div>
        </Panel>
        <Panel className="p-6">
          <h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">
            {isToeic ? 'Điểm trung bình theo kỹ năng' : 'Band trung bình theo kỹ năng'}
          </h3>
          <div className={`mt-6 grid gap-3 ${isToeic ? 'grid-cols-2' : 'grid-cols-2'}`}>
            {(isToeic
              ? [['Listening', monitoring?.averageListeningBand], ['Reading', monitoring?.averageReadingBand]]
              : [['Nghe', monitoring?.averageListeningBand], ['Đọc', monitoring?.averageReadingBand], ['Viết', monitoring?.averageWritingBand], ['Nói', monitoring?.averageSpeakingBand]]
            ).map(([label, value]) => (
              <div className="rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-4" key={label}>
                <p className="text-xs font-bold uppercase tracking-[.14em] text-[#8b706e]">{label}</p>
                <p className="mt-2 text-2xl font-extrabold text-[#4b0009]">{formatScore(value)}</p>
              </div>
            ))}
          </div>
        </Panel>
      </section>

      <Panel className="overflow-hidden">
        <div className="border-b border-[#f0e3e4] px-6 py-5">
          <h3 className="flex items-baseline gap-1.5 font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">Lượt làm gần đây · <ManagerTaxonomyBadge kind="exam" value={examType} /></h3>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full text-left">
            <thead className="bg-[#fbf3f4] text-[11px] font-extrabold uppercase tracking-wider text-slate-500">
              <tr>
                {(isToeic
                  ? ['Học viên', 'Tổng', 'Listening', 'Reading', 'Trạng thái', 'Nộp lúc']
                  : ['Học viên', 'Tổng', 'Nghe', 'Đọc', 'Viết', 'Nói', 'Trạng thái', 'Nộp lúc']
                ).map((label) => <th className="px-5 py-4" key={label}>{label}</th>)}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#f0e3e4]">
              {recentAttempts.length ? paginatedAttempts.map((attempt) => (
                <tr key={attempt.id}>
                  <td className="px-5 py-4">
                    <p className="text-sm font-bold text-[#0b1c30]">{attempt.learnerName || 'Chưa có tên'}</p>
                    <p className="text-xs text-[#735b59]">{attempt.learnerEmail}</p>
                  </td>
                  <td className="px-5 py-4 text-sm font-bold text-[#0b1c30]">{formatScore(attempt.overallBand)}</td>
                  <td className="px-5 py-4 text-sm">{formatScore(attempt.listeningBand)}</td>
                  <td className="px-5 py-4 text-sm">{formatScore(attempt.readingBand)}</td>
                  {isToeic ? null : (
                    <>
                      <td className="px-5 py-4 text-sm">{formatScore(attempt.writingBand)}</td>
                      <td className="px-5 py-4 text-sm">{formatScore(attempt.speakingBand)}</td>
                    </>
                  )}
                  <td className="px-5 py-4 text-sm">{attempt.status === 'COMPLETED' ? 'Hoàn thành' : 'Đã chấm khách quan'}</td>
                  <td className="px-5 py-4 text-sm">{formatDate(attempt.submittedAt)}</td>
                </tr>
              )) : (
                <tr>
                  <td className="px-5 py-10 text-sm text-[#584140]" colSpan={isToeic ? 6 : 8}>Chưa có lượt làm nào.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {recentAttempts.length ? (
          <div className="border-t border-[#f0e3e4] px-5 py-4">
            <Pagination
              alwaysVisible
              onChange={setAttemptsPage}
              page={attemptsPage}
              pageSize={RECENT_ATTEMPTS_PAGE_SIZE}
              totalItems={attemptsTotalItems}
              totalPages={attemptsTotalPages}
            />
          </div>
        ) : null}
      </Panel>
    </div>
  );
}

function MonitorCard({ icon: Icon, label, value }) { return <Panel className="p-5"><div className="flex justify-between gap-3"><div><p className="text-sm text-[#584140]">{label}</p><p className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#4b0009]">{value}</p></div><span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]"><Icon className="h-5 w-5" /></span></div></Panel>; }

function ToeicEditor({ config, onChangeSection, onReset }) {
  const [sectionTab, setSectionTab] = useState('listening');
  const section = config?.[sectionTab] || {};
  const activeMeta = TOEIC_SECTION_TABS.find((tab) => tab.key === sectionTab);
  const sectionAnswerKey = {
    ...(config?.answerKey || {}),
    ...(section.answerKey || {}),
  };
  const { answerKey: _ignored, ...uiConfig } = section;

  return (
    <div className="space-y-5">
      <Panel className="p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">Biên soạn đề TOEIC</h3>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-[#584140]">
              Soạn Listening và Reading giống phần Nghe/Đọc IELTS: thêm part, nhóm câu, ảnh, audio và đáp án trực tiếp.
            </p>
          </div>
          <button className="rounded-xl border border-[#dfbfbd] px-4 py-2 text-xs font-extrabold text-[#730014]" onClick={onReset} type="button">
            Nạp khung 7 part
          </button>
        </div>
        <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {TOEIC_PARTS.map(([part, title, count]) => (
            <div className="rounded-2xl border border-[#f0e3e4] bg-[#fffafb] p-4" key={part}>
              <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">{part}</p>
              <p className="mt-1 font-extrabold text-[#1a1c1c]">{title}</p>
              <p className="mt-1 text-xs font-semibold text-[#584140]">{count} câu</p>
            </div>
          ))}
        </div>
      </Panel>

      <div className="flex gap-2">
        {TOEIC_SECTION_TABS.map((tab) => (
          <button
            className={`rounded-lg px-4 py-2.5 text-sm font-bold transition ${sectionTab === tab.key ? 'bg-[#4b0009] text-white' : 'border border-[#dfbfbd] bg-white text-[#4b0009]'}`}
            key={tab.key}
            onClick={() => setSectionTab(tab.key)}
            type="button"
          >
            {tab.label}
          </button>
        ))}
      </div>

      <Panel className="p-6">
        <h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">TOEIC {activeMeta?.label}</h3>
        <p className="mt-2 text-sm leading-6 text-[#584140]">Biên soạn phần thi, câu hỏi và đáp án trực quan. Đáp án không được hiển thị cho học viên.</p>
        <div className="mt-5">
          <AssessmentExamBuilder
            assessment={{
              title: section.title || `TOEIC ${activeMeta?.label}`,
              skill: activeMeta?.skill,
              examType: 'TOEIC',
              uiConfigJson: JSON.stringify({
                ...uiConfig,
                examType: 'TOEIC',
                type: sectionTab === 'reading' ? 'toeic_reading_exam' : 'toeic_listening_exam',
              }),
              objectiveAnswerKey: JSON.stringify(sectionAnswerKey),
              timeLimitMinutes: section.durationMinutes || (sectionTab === 'listening' ? 45 : 75),
            }}
            onChange={(field, value) => onChangeSection(sectionTab, field, value)}
          />
        </div>
      </Panel>
    </div>
  );
}

function ObjectiveEditor({ config, label, onChange, skill }) {
  const { answerKey, ...uiConfig } = config || {};
  return <Panel className="p-6"><h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">{label}</h3><p className="mt-2 text-sm leading-6 text-[#584140]">Biên soạn phần thi, câu hỏi và đáp án trực quan. Đáp án không được hiển thị cho học viên.</p><AssessmentExamBuilder assessment={{ title: config?.title || label, skill, uiConfigJson: JSON.stringify(uiConfig), objectiveAnswerKey: JSON.stringify(answerKey || {}), timeLimitMinutes: config?.durationMinutes || 40 }} onChange={onChange} /></Panel>;
}

function SubjectiveEditor({ config, label, onChange, skill }) {
  return <Panel className="p-6"><h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">{label}</h3><p className="mt-2 text-sm leading-6 text-[#584140]">Biên soạn nội dung đề bằng cùng bộ công cụ với Nghe và Đọc.</p><AssessmentExamBuilder assessment={{ title: config?.title || label, skill, uiConfigJson: JSON.stringify(config || {}), objectiveAnswerKey: '', timeLimitMinutes: config?.durationMinutes || (skill === 'WRITING' ? 60 : 15) }} onChange={onChange} /></Panel>;
}

function toDraft(response) {
  const legacyEnabled = response.status === 'PUBLISHED';
  return {
    ...response,
    examType: response.examType || 'IELTS',
    ieltsEnabled: response.ieltsEnabled ?? legacyEnabled,
    toeicEnabled: response.toeicEnabled ?? legacyEnabled,
    skillAssessmentEnabled: response.skillAssessmentEnabled ?? legacyEnabled,
    listening: parseConfig(response.listeningConfigJson),
    reading: parseConfig(response.readingConfigJson),
    writing: parseConfig(response.writingConfigJson),
    speaking: parseConfig(response.speakingConfigJson),
    toeic: parseConfig(response.toeicConfigJson, buildDefaultToeicConfig()),
  };
}

function toPayload(draft) {
  const toeic = draft.toeic || buildDefaultToeicConfig();
  const toeicPayload = {
    ...toeic,
    answerKey: {
      ...(toeic.listening?.answerKey || {}),
      ...(toeic.reading?.answerKey || {}),
      ...(toeic.answerKey || {}),
    },
  };
  const anyExamEnabled = draft.ieltsEnabled || draft.toeicEnabled || draft.skillAssessmentEnabled;
  return {
    title: draft.title,
    description: draft.description,
    examType: draft.examType || 'IELTS',
    maxAttempts: Number(draft.maxAttempts),
    status: anyExamEnabled ? 'PUBLISHED' : 'ARCHIVED',
    ieltsEnabled: Boolean(draft.ieltsEnabled),
    toeicEnabled: Boolean(draft.toeicEnabled),
    skillAssessmentEnabled: Boolean(draft.skillAssessmentEnabled),
    listeningConfigJson: JSON.stringify(draft.listening),
    readingConfigJson: JSON.stringify(draft.reading),
    writingConfigJson: JSON.stringify(draft.writing),
    speakingConfigJson: JSON.stringify(draft.speaking),
    toeicConfigJson: JSON.stringify(toeicPayload),
  };
}

function buildDefaultToeicConfig() {
  const listeningParts = TOEIC_PARTS.slice(0, 4).map(([part, title], index) => ({
    key: `toeic_listening_part_${index + 1}`,
    partNumber: Number(part.replace('Part ', '')),
    part: Number(part.replace('Part ', '')),
    title,
    summary: '',
    questionGroups: [{
      title: title,
      instructions: '',
      descriptionHtml: '',
      passageHtml: '',
      type: 'single_choice',
      hideOptionText: index < 2,
      perQuestionAudio: index < 2,
      audioUrl: '',
      questions: [],
    }],
  }));
  const readingParts = TOEIC_PARTS.slice(4).map(([part, title], index) => ({
    key: `toeic_reading_part_${index + 5}`,
    partNumber: Number(part.replace('Part ', '')),
    part: Number(part.replace('Part ', '')),
    title,
    summary: '',
    questionGroups: [{
      title: title,
      instructions: '',
      descriptionHtml: '',
      passageHtml: '',
      type: 'single_choice',
      hideOptionText: false,
      perQuestionAudio: false,
      audioUrl: '',
      questions: [],
    }],
  }));
  return {
    type: 'toeic_full_test',
    examType: 'TOEIC',
    title: 'TOEIC Placement',
    durationMinutes: 120,
    listening: {
      type: 'toeic_listening_exam',
      examType: 'TOEIC',
      title: 'TOEIC Listening',
      durationMinutes: 45,
      parts: listeningParts,
      answerKey: {},
    },
    reading: {
      type: 'toeic_reading_exam',
      examType: 'TOEIC',
      title: 'TOEIC Reading',
      durationMinutes: 75,
      parts: readingParts,
      answerKey: {},
    },
    answerKey: {},
  };
}

function formatBand(value) { return value == null ? '—' : Number(value).toFixed(1); }
function formatDate(value) { return value ? new Date(value).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : '—'; }
