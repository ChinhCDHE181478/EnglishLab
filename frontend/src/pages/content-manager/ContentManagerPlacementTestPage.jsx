import { useEffect, useMemo, useState } from 'react';
import { BarChart3, BookOpen, CheckCircle2, Headphones, LoaderCircle, Mic2, NotebookPen, RefreshCw, Save, Users } from 'lucide-react';
import placementTestApi from '../../api/placementTestApi';
import AssessmentExamBuilder from '../../components/content-manager/AssessmentExamBuilder';
import { ManagerFilterBar, ManagerStatsGrid } from '../../components/content-manager/ManagerListUi';
import { Panel, TextField } from '../../components/content-manager/ContentManagerUi';

const TABS = [
  { key: 'overview', label: 'Thiết lập chung' },
  { key: 'monitoring', label: 'Theo dõi kết quả' },
  { key: 'listening', label: 'Nghe' },
  { key: 'reading', label: 'Đọc' },
  { key: 'writing', label: 'Viết' },
  { key: 'speaking', label: 'Nói' },
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
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    let active = true;
    Promise.all([placementTestApi.getManagedDefinition(), placementTestApi.getMonitoring()])
      .then(([response, monitoringResponse]) => {
        if (!active) return;
        setDefinition(toDraft(response));
        setMonitoring(monitoringResponse);
      })
      .catch((requestError) => active && setError(requestError?.response?.data?.message || 'Không tải được dữ liệu bài đánh giá đầu vào.'))
      .finally(() => {
        if (!active) return;
        setLoading(false);
        setMonitoringLoading(false);
      });
    return () => { active = false; };
  }, []);

  const questionCounts = useMemo(() => ({
    listening: countQuestions(definition?.listening),
    reading: countQuestions(definition?.reading),
    writing: definition?.writing?.tasks?.length || 0,
    speaking: countSpeakingPrompts(definition?.speaking),
  }), [definition]);
  const statItems = useMemo(() => [
    { label: 'Câu nghe', value: questionCounts.listening, icon: Headphones, tone: 'text-[#4b0009]' },
    { label: 'Câu đọc', value: questionCounts.reading, icon: BookOpen, tone: 'text-[#005236]' },
    { label: 'Task viết', value: questionCounts.writing, icon: NotebookPen, tone: 'text-amber-700' },
    { label: 'Prompt nói', value: questionCounts.speaking, icon: Mic2, tone: 'text-emerald-700' },
  ], [questionCounts]);

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

  const refreshMonitoring = async () => {
    setMonitoringLoading(true);
    try {
      setMonitoring(await placementTestApi.getMonitoring());
    } catch (requestError) {
      setError(requestError?.response?.data?.message || 'Không tải được dữ liệu theo dõi.');
    } finally {
      setMonitoringLoading(false);
    }
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
          {TABS.map((tab) => (
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
      {activeTab === 'monitoring' ? <Monitoring monitoring={monitoring} loading={monitoringLoading} onRefresh={refreshMonitoring} /> : null}
      {activeTab === 'listening' ? <ObjectiveEditor label="Bài đánh giá kỹ năng Nghe" skill="LISTENING" config={definition.listening} onChange={(field, value) => applyObjectiveChange('listening', field, value)} /> : null}
      {activeTab === 'reading' ? <ObjectiveEditor label="Bài đánh giá kỹ năng Đọc" skill="READING" config={definition.reading} onChange={(field, value) => applyObjectiveChange('reading', field, value)} /> : null}
      {activeTab === 'writing' ? <SubjectiveEditor config={definition.writing} label="Bài đánh giá kỹ năng Viết" skill="WRITING" onChange={(field, value) => applySubjectiveChange('writing', field, value)} /> : null}
      {activeTab === 'speaking' ? <SubjectiveEditor config={definition.speaking} label="Bài đánh giá kỹ năng Nói" skill="SPEAKING" onChange={(field, value) => applySubjectiveChange('speaking', field, value)} /> : null}
    </div>
  );
}

function Monitoring({ loading, monitoring, onRefresh }) {
  const distribution = monitoring?.bandDistribution || [];
  const maximum = Math.max(...distribution.map((item) => Number(item.count || 0)), 1);
  return <div className="space-y-6"><div className="flex justify-end"><button className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm font-bold text-[#730014]" onClick={onRefresh} type="button"><RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /> Làm mới số liệu</button></div>
    <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4"><MonitorCard icon={Users} label="Người tham gia" value={monitoring?.uniqueParticipants || 0} /><MonitorCard icon={BarChart3} label="Tổng lượt làm" value={monitoring?.totalAttempts || 0} /><MonitorCard icon={CheckCircle2} label="Đã hoàn thành" value={monitoring?.completedAttempts || 0} /><MonitorCard icon={BarChart3} label="Band tổng trung bình" value={formatBand(monitoring?.averageOverallBand)} /></section>
    <section className="grid gap-6 xl:grid-cols-[1.05fr_.95fr]"><Panel className="p-6"><h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">Phân hóa band đầu vào</h3><div className="mt-6 space-y-5">{distribution.length ? distribution.map((item) => <div className="space-y-2" key={item.label}><div className="flex justify-between text-sm"><span className="font-semibold text-[#4b0009]">{item.label}</span><span className="font-bold">{item.count}</span></div><div className="h-3 overflow-hidden rounded-full bg-[#f1e3e4]"><div className="h-full rounded-full bg-[#730014]" style={{ width: `${item.count ? Math.max((Number(item.count) / maximum) * 100, 8) : 0}%` }} /></div></div>) : <p className="text-sm text-[#584140]">Chưa có lượt làm để phân tích.</p>}</div></Panel>
      <Panel className="p-6"><h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">Band trung bình theo kỹ năng</h3><div className="mt-6 grid grid-cols-2 gap-3">{[['Nghe', monitoring?.averageListeningBand], ['Đọc', monitoring?.averageReadingBand], ['Viết', monitoring?.averageWritingBand], ['Nói', monitoring?.averageSpeakingBand]].map(([label, value]) => <div className="rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-4" key={label}><p className="text-xs font-bold uppercase tracking-[.14em] text-[#8b706e]">{label}</p><p className="mt-2 text-2xl font-extrabold text-[#4b0009]">{formatBand(value)}</p></div>)}</div></Panel></section>
    <Panel className="overflow-hidden"><div className="border-b border-[#f0e3e4] px-6 py-5"><h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">Lượt làm gần đây</h3></div><div className="overflow-x-auto"><table className="min-w-full text-left"><thead className="bg-[#fbf3f4] text-xs uppercase tracking-[.15em] text-[#8e7371]"><tr>{['Học viên', 'Tổng', 'Nghe', 'Đọc', 'Viết', 'Nói', 'Trạng thái', 'Nộp lúc'].map((label) => <th className="px-5 py-4 font-semibold" key={label}>{label}</th>)}</tr></thead><tbody className="divide-y divide-[#f0e3e4]">{monitoring?.recentAttempts?.length ? monitoring.recentAttempts.map((attempt) => <tr key={attempt.id}><td className="px-5 py-4"><p className="font-semibold">{attempt.learnerName || 'Chưa có tên'}</p><p className="text-xs text-[#735b59]">{attempt.learnerEmail}</p></td><td className="px-5 py-4 font-bold">{formatBand(attempt.overallBand)}</td><td className="px-5 py-4 text-sm">{formatBand(attempt.listeningBand)}</td><td className="px-5 py-4 text-sm">{formatBand(attempt.readingBand)}</td><td className="px-5 py-4 text-sm">{formatBand(attempt.writingBand)}</td><td className="px-5 py-4 text-sm">{formatBand(attempt.speakingBand)}</td><td className="px-5 py-4 text-sm">{attempt.status === 'COMPLETED' ? 'Hoàn thành' : 'Đã chấm khách quan'}</td><td className="px-5 py-4 text-sm">{formatDate(attempt.submittedAt)}</td></tr>) : <tr><td className="px-5 py-10 text-sm text-[#584140]" colSpan={8}>Chưa có lượt làm nào.</td></tr>}</tbody></table></div></Panel>
  </div>;
}

function MonitorCard({ icon: Icon, label, value }) { return <Panel className="p-5"><div className="flex justify-between gap-3"><div><p className="text-sm text-[#584140]">{label}</p><p className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#4b0009]">{value}</p></div><span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]"><Icon className="h-5 w-5" /></span></div></Panel>; }

function Overview({ definition, onChange }) {
  return <Panel className="p-6"><div className="grid gap-4 lg:grid-cols-2">
    <TextField label="Tên bài đánh giá" onChange={(event) => onChange('title', event.target.value)} value={definition.title} />
    <TextField label="Số lượt làm tối đa" onChange={(event) => onChange('maxAttempts', Number(event.target.value))} value={definition.maxAttempts} />
    <div className="lg:col-span-2"><TextField label="Mô tả" onChange={(event) => onChange('description', event.target.value)} rows={3} textarea value={definition.description} /></div>
  </div><label className="mt-5 flex items-center gap-3 rounded-2xl border border-[#f0e3e4] bg-[#fffafb] px-4 py-3 text-sm font-semibold text-[#1a1c1c]"><input checked={definition.active} className="h-4 w-4 accent-[#730014]" onChange={(event) => onChange('active', event.target.checked)} type="checkbox" /> Cho phép học viên làm bài đánh giá đầu vào</label>
  </Panel>;
}

function ObjectiveEditor({ config, label, onChange, skill }) {
  const { answerKey, ...uiConfig } = config || {};
  return <Panel className="p-6"><h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">{label}</h3><p className="mt-2 text-sm leading-6 text-[#584140]">Biên soạn phần thi, câu hỏi và đáp án trực quan. Đáp án không được hiển thị cho học viên.</p><AssessmentExamBuilder assessment={{ title: config?.title || label, skill, uiConfigJson: JSON.stringify(uiConfig), objectiveAnswerKey: JSON.stringify(answerKey || {}), timeLimitMinutes: config?.durationMinutes || 40 }} onChange={onChange} /></Panel>;
}

function SubjectiveEditor({ config, label, onChange, skill }) {
  return <Panel className="p-6"><h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">{label}</h3><p className="mt-2 text-sm leading-6 text-[#584140]">Biên soạn nội dung đề bằng cùng bộ công cụ với Nghe và Đọc.</p><AssessmentExamBuilder assessment={{ title: config?.title || label, skill, uiConfigJson: JSON.stringify(config || {}), objectiveAnswerKey: '', timeLimitMinutes: config?.durationMinutes || (skill === 'WRITING' ? 60 : 15) }} onChange={onChange} /></Panel>;
}

function toDraft(response) {
  return { ...response, listening: parseConfig(response.listeningConfigJson), reading: parseConfig(response.readingConfigJson), writing: parseConfig(response.writingConfigJson), speaking: parseConfig(response.speakingConfigJson) };
}

function toPayload(draft) {
  return { title: draft.title, description: draft.description, maxAttempts: Number(draft.maxAttempts), active: Boolean(draft.active), listeningConfigJson: JSON.stringify(draft.listening), readingConfigJson: JSON.stringify(draft.reading), writingConfigJson: JSON.stringify(draft.writing), speakingConfigJson: JSON.stringify(draft.speaking) };
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
