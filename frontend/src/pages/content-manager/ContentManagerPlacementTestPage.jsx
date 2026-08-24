import { useEffect, useMemo, useState } from 'react';
import { BarChart3, BookOpen, CheckCircle2, Headphones, LoaderCircle, Mic2, NotebookPen, RefreshCw, Save, Users } from 'lucide-react';
import placementTestApi from '../../api/placementTestApi';
import AssessmentExamBuilder from '../../components/content-manager/AssessmentExamBuilder';
import { ManagerFilterBar, ManagerStatsGrid } from '../../components/content-manager/ManagerListUi';
import { Panel, TextField } from '../../components/content-manager/ContentManagerUi';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import BrandedSelect from '../../components/ui/BrandedSelect';

const TABS = [
  { key: 'overview', label: 'Thiết lập chung' },
  { key: 'monitoring', label: 'Theo dõi kết quả' },
  { key: 'listening', label: 'Nghe', examTypes: ['IELTS'] },
  { key: 'reading', label: 'Đọc', examTypes: ['IELTS'] },
  { key: 'writing', label: 'Viết', examTypes: ['IELTS'] },
  { key: 'speaking', label: 'Nói', examTypes: ['IELTS'] },
  { key: 'toeic', label: 'TOEIC', examTypes: ['TOEIC'] },
];

const examTypeOptions = [
  { label: 'IELTS 4 kỹ năng', value: 'IELTS' },
  { label: 'TOEIC Listening & Reading', value: 'TOEIC' },
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
  const [activeTab, setActiveTab] = useState('overview');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [monitoring, setMonitoring] = useState(null);
  const [monitoringLoading, setMonitoringLoading] = useState(true);
  const [monitoringExamType, setMonitoringExamType] = useState('IELTS');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

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
        if (active) setError(requestError?.response?.data?.message || 'Không tải được dữ liệu bài đánh giá đầu vào.');
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
    const examType = definition?.examType || 'IELTS';
    return TABS.filter((tab) => !tab.examTypes || tab.examTypes.includes(examType));
  }, [definition?.examType]);

  useEffect(() => {
    if (!visibleTabs.some((tab) => tab.key === activeTab)) {
      setActiveTab('overview');
    }
  }, [activeTab, visibleTabs]);

  const questionCounts = useMemo(() => ({
    listening: countQuestions(definition?.listening),
    reading: countQuestions(definition?.reading),
    writing: definition?.writing?.tasks?.length || 0,
    speaking: countSpeakingPrompts(definition?.speaking),
    toeicListening: countQuestions(definition?.toeic?.listening),
    toeicReading: countQuestions(definition?.toeic?.reading),
  }), [definition]);
  const isToeicExam = (definition?.examType || 'IELTS') === 'TOEIC';
  const statItems = useMemo(() => (
    isToeicExam
      ? [
        { label: 'Câu Listening', value: questionCounts.toeicListening, icon: Headphones, tone: 'text-[#4b0009]' },
        { label: 'Câu Reading', value: questionCounts.toeicReading, icon: BookOpen, tone: 'text-[#005236]' },
        { label: 'Part TOEIC', value: 7, icon: NotebookPen, tone: 'text-amber-700' },
        { label: 'Đáp án', value: Object.keys(definition?.toeic?.answerKey || {}).length, icon: Mic2, tone: 'text-emerald-700' },
      ]
      : [
        { label: 'Câu nghe', value: questionCounts.listening, icon: Headphones, tone: 'text-[#4b0009]' },
        { label: 'Câu đọc', value: questionCounts.reading, icon: BookOpen, tone: 'text-[#005236]' },
        { label: 'Task viết', value: questionCounts.writing, icon: NotebookPen, tone: 'text-amber-700' },
        { label: 'Prompt nói', value: questionCounts.speaking, icon: Mic2, tone: 'text-emerald-700' },
      ]
  ), [definition?.toeic?.answerKey, isToeicExam, questionCounts]);

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
      setError(requestError?.response?.data?.message || 'Không thể lưu cấu hình bài đánh giá đầu vào.');
    } finally {
      setSaving(false);
    }
  };

  const refreshMonitoring = async (examType = monitoringExamType) => {
    setMonitoringLoading(true);
    try {
      setMonitoring(await placementTestApi.getMonitoring(examType));
      setMonitoringExamType(examType);
    } catch (requestError) {
      setError(requestError?.response?.data?.message || 'Không tải được dữ liệu theo dõi.');
    } finally {
      setMonitoringLoading(false);
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
    return <Panel className="min-h-[320px] p-6 text-sm font-semibold text-[#93000a]">{error || 'Không có dữ liệu để quản lý.'}</Panel>;
  }

  return (
    <div className="space-y-6">
      <ManagerStatsGrid stats={statItems} />

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
        <button className="inline-flex shrink-0 items-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#730014] disabled:opacity-50" disabled={saving} onClick={save} type="button">
          <Save className="h-4 w-4" /> {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
        </button>
      </ManagerFilterBar>

      {error ? <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">{error}</div> : null}
      {notice ? <div className="flex items-center gap-2 rounded-2xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm font-semibold text-emerald-800"><CheckCircle2 className="h-5 w-5" /> {notice}</div> : null}

      {activeTab === 'overview' ? <Overview definition={definition} onChange={updateDefinition} /> : null}
      {activeTab === 'monitoring' ? (
        <Monitoring
          examType={monitoringExamType}
          loading={monitoringLoading}
          monitoring={monitoring}
          onChangeExamType={refreshMonitoring}
          onRefresh={() => refreshMonitoring(monitoringExamType)}
        />
      ) : null}
      {activeTab === 'listening' ? <ObjectiveEditor label="Bài đánh giá kỹ năng Nghe" skill="LISTENING" config={definition.listening} onChange={(field, value) => applyObjectiveChange('listening', field, value)} /> : null}
      {activeTab === 'reading' ? <ObjectiveEditor label="Bài đánh giá kỹ năng Đọc" skill="READING" config={definition.reading} onChange={(field, value) => applyObjectiveChange('reading', field, value)} /> : null}
      {activeTab === 'writing' ? <SubjectiveEditor config={definition.writing} label="Bài đánh giá kỹ năng Viết" skill="WRITING" onChange={(field, value) => applySubjectiveChange('writing', field, value)} /> : null}
      {activeTab === 'speaking' ? <SubjectiveEditor config={definition.speaking} label="Bài đánh giá kỹ năng Nói" skill="SPEAKING" onChange={(field, value) => applySubjectiveChange('speaking', field, value)} /> : null}
      {activeTab === 'toeic' ? <ToeicEditor config={definition.toeic} onChangeSection={applyToeicSectionChange} onReset={() => updateConfig('toeic', buildDefaultToeicConfig())} /> : null}
    </div>
  );
}

function Monitoring({ examType = 'IELTS', loading, monitoring, onChangeExamType, onRefresh }) {
  const isToeic = examType === 'TOEIC';
  const distribution = monitoring?.bandDistribution || [];
  const maximum = Math.max(...distribution.map((item) => Number(item.count || 0)), 1);
  const scoreLabel = isToeic ? 'Điểm' : 'Band';
  const formatScore = (value) => {
    if (value == null) return '—';
    return isToeic ? String(Math.round(Number(value))) : Number(value).toFixed(1);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex gap-2">
          {['IELTS', 'TOEIC'].map((type) => (
            <button
              className={`rounded-lg px-4 py-2.5 text-sm font-bold transition ${examType === type ? 'bg-[#4b0009] text-white' : 'border border-[#dfbfbd] bg-white text-[#4b0009] hover:bg-[#fff7f7]'}`}
              key={type}
              onClick={() => onChangeExamType?.(type)}
              type="button"
            >
              {type}
            </button>
          ))}
        </div>
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
          <h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">Lượt làm gần đây · {examType}</h3>
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
              {monitoring?.recentAttempts?.length ? monitoring.recentAttempts.map((attempt) => (
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
      </Panel>
    </div>
  );
}

function MonitorCard({ icon: Icon, label, value }) { return <Panel className="p-5"><div className="flex justify-between gap-3"><div><p className="text-sm text-[#584140]">{label}</p><p className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#4b0009]">{value}</p></div><span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]"><Icon className="h-5 w-5" /></span></div></Panel>; }

function Overview({ definition, onChange }) {
  return <Panel className="p-6"><div className="grid gap-4 lg:grid-cols-2">
    <TextField label="Tên bài đánh giá" onChange={(event) => onChange('title', event.target.value)} value={definition.title} />
    <label className="block">
      <span className="mb-2 block text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">Case đề</span>
      <BrandedSelect
        onChange={(event) => onChange('examType', event.target.value)}
        options={examTypeOptions}
        value={definition.examType || 'IELTS'}
      />
    </label>
    <div className="lg:col-span-2">
      <RichTextEditor
        label="Mô tả"
        onChange={(html) => onChange('description', html)}
        placeholder="Mô tả bài đánh giá đầu vào..."
        size="compact"
        value={definition.description}
      />
    </div>
  </div><label className="mt-5 flex items-center gap-3 rounded-2xl border border-[#f0e3e4] bg-[#fffafb] px-4 py-3 text-sm font-semibold text-[#1a1c1c]"><input checked={definition.active} className="h-4 w-4 accent-[#4b0009]" onChange={(event) => onChange('active', event.target.checked)} type="checkbox" /> Cho phép học viên làm bài đánh giá đầu vào</label>
  </Panel>;
}

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
  return { ...response, examType: response.examType || 'IELTS', listening: parseConfig(response.listeningConfigJson), reading: parseConfig(response.readingConfigJson), writing: parseConfig(response.writingConfigJson), speaking: parseConfig(response.speakingConfigJson), toeic: parseConfig(response.toeicConfigJson, buildDefaultToeicConfig()) };
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
  return {
    title: draft.title,
    description: draft.description,
    examType: draft.examType || 'IELTS',
    maxAttempts: Number(draft.maxAttempts),
    active: Boolean(draft.active),
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

function countQuestions(config) {
  return (config?.parts || []).reduce((total, part) => total + (part.questionGroups || []).reduce((groupTotal, group) => groupTotal + (group.questionNumbers?.length || group.questions?.length || 0), 0), 0);
}

function countSpeakingPrompts(config) {
  if (Array.isArray(config?.variants)) {
    return config.variants.reduce((sum, variant) => sum + (variant.parts || []).reduce((partSum, part) => partSum + (part.prompts?.length || 0), 0), 0);
  }
  return config?.parts?.reduce((sum, part) => sum + (part.prompts?.length || 0), 0) || 0;
}

function formatBand(value) { return value == null ? '—' : Number(value).toFixed(1); }
function formatDate(value) { return value ? new Date(value).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : '—'; }
