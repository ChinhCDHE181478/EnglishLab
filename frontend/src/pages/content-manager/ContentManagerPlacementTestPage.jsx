import { useEffect, useMemo, useState } from 'react';
import { BarChart3, CheckCircle2, LoaderCircle, Plus, RefreshCw, Save, Trash2, Users } from 'lucide-react';
import placementTestApi from '../../api/placementTestApi';
import AssessmentExamBuilder from '../../components/content-manager/AssessmentExamBuilder';
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

const createTask = (index) => ({
  key: `task_${index + 1}`,
  title: `Task ${index + 1}`,
  heading: `Writing Task ${index + 1}`,
  summary: '',
  recommendedMinutes: 20,
  minimumWords: 150,
  promptParagraphs: [],
});

const createSpeakingPart = (index) => ({
  key: `part_${index + 1}`,
  title: `Part ${index + 1}`,
  prepSeconds: 0,
  answerSeconds: 120,
  prompts: [],
});

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
    speaking: definition?.speaking?.parts?.reduce((sum, part) => sum + (part.prompts?.length || 0), 0) || 0,
  }), [definition]);

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
      <Panel className="overflow-hidden p-0">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-[#f0e3e4] px-6 py-5">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">Bộ đánh giá đầu vào</p>
            <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#4b0009]">{definition.title}</h2>
          </div>
          <button className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-bold text-white disabled:opacity-50" disabled={saving} onClick={save} type="button">
            <Save className="h-4 w-4" /> {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
          </button>
        </div>
        <div className="flex overflow-x-auto border-b border-[#f0e3e4] px-4">
          {TABS.map((tab) => <button className={`shrink-0 border-b-2 px-4 py-4 text-sm font-bold ${activeTab === tab.key ? 'border-[#730014] text-[#730014]' : 'border-transparent text-[#735b59]'}`} key={tab.key} onClick={() => setActiveTab(tab.key)} type="button">{tab.label}</button>)}
        </div>
      </Panel>

      {error ? <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">{error}</div> : null}
      {notice ? <div className="flex items-center gap-2 rounded-2xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm font-semibold text-emerald-800"><CheckCircle2 className="h-5 w-5" /> {notice}</div> : null}

      {activeTab === 'overview' ? <Overview definition={definition} counts={questionCounts} onChange={updateDefinition} /> : null}
      {activeTab === 'monitoring' ? <Monitoring monitoring={monitoring} loading={monitoringLoading} onRefresh={refreshMonitoring} /> : null}
      {activeTab === 'listening' ? <ObjectiveEditor label="Bài đánh giá kỹ năng Nghe" skill="LISTENING" config={definition.listening} onChange={(field, value) => applyObjectiveChange('listening', field, value)} /> : null}
      {activeTab === 'reading' ? <ObjectiveEditor label="Bài đánh giá kỹ năng Đọc" skill="READING" config={definition.reading} onChange={(field, value) => applyObjectiveChange('reading', field, value)} /> : null}
      {activeTab === 'writing' ? <WritingEditor config={definition.writing} onChange={(next) => updateConfig('writing', next)} /> : null}
      {activeTab === 'speaking' ? <SpeakingEditor config={definition.speaking} onChange={(next) => updateConfig('speaking', next)} /> : null}
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

function Overview({ definition, counts, onChange }) {
  return <Panel className="p-6"><div className="grid gap-4 lg:grid-cols-2">
    <TextField label="Tên bài đánh giá" onChange={(event) => onChange('title', event.target.value)} value={definition.title} />
    <TextField label="Số lượt làm tối đa" onChange={(event) => onChange('maxAttempts', Number(event.target.value))} value={definition.maxAttempts} />
    <div className="lg:col-span-2"><TextField label="Mô tả" onChange={(event) => onChange('description', event.target.value)} rows={3} textarea value={definition.description} /></div>
  </div><label className="mt-5 flex items-center gap-3 rounded-2xl border border-[#f0e3e4] bg-[#fffafb] px-4 py-3 text-sm font-semibold text-[#1a1c1c]"><input checked={definition.active} className="h-4 w-4 accent-[#730014]" onChange={(event) => onChange('active', event.target.checked)} type="checkbox" /> Cho phép học viên làm bài đánh giá đầu vào</label>
    <div className="mt-6 grid gap-3 md:grid-cols-4">{[['Nghe', counts.listening], ['Đọc', counts.reading], ['Viết', counts.writing], ['Nói', counts.speaking]].map(([label, count]) => <div className="rounded-2xl border border-[#eadcdc] bg-white p-4" key={label}><p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">{label}</p><p className="mt-2 text-2xl font-extrabold text-[#4b0009]">{count}</p><p className="text-sm text-[#584140]">mục đang có</p></div>)}</div>
  </Panel>;
}

function ObjectiveEditor({ config, label, onChange, skill }) {
  const { answerKey, ...uiConfig } = config || {};
  return <Panel className="p-6"><h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">{label}</h3><p className="mt-2 text-sm leading-6 text-[#584140]">Biên soạn phần thi, câu hỏi và đáp án trực quan. Đáp án không được hiển thị cho học viên.</p><AssessmentExamBuilder assessment={{ title: config?.title || label, skill, uiConfigJson: JSON.stringify(uiConfig), objectiveAnswerKey: JSON.stringify(answerKey || {}), timeLimitMinutes: config?.durationMinutes || 40 }} onChange={onChange} /></Panel>;
}

function WritingEditor({ config, onChange }) {
  const tasks = config?.tasks || [];
  const update = (patch) => onChange({ ...config, ...patch });
  const updateTask = (index, patch) => update({ tasks: tasks.map((task, itemIndex) => itemIndex === index ? { ...task, ...patch } : task) });
  return <Panel className="space-y-5 p-6"><h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">Bài đánh giá kỹ năng Viết</h3><div className="grid gap-4 md:grid-cols-2"><TextField label="Tên đề" onChange={(event) => update({ title: event.target.value })} value={config?.title || ''} /><TextField label="Thời lượng (phút)" onChange={(event) => update({ durationMinutes: Number(event.target.value) })} value={config?.durationMinutes || ''} /></div>{tasks.map((task, index) => <div className="rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-5" key={task.key || index}><div className="flex justify-end"><button className="inline-flex items-center gap-2 text-sm font-bold text-[#93000a]" onClick={() => update({ tasks: tasks.filter((_, itemIndex) => itemIndex !== index) })} type="button"><Trash2 className="h-4 w-4" /> Xóa nhiệm vụ</button></div><div className="grid gap-4 md:grid-cols-2"><TextField label="Tên nhiệm vụ" onChange={(event) => updateTask(index, { title: event.target.value })} value={task.title || ''} /><TextField label="Tiêu đề hiển thị" onChange={(event) => updateTask(index, { heading: event.target.value })} value={task.heading || ''} /><TextField label="Số từ tối thiểu" onChange={(event) => updateTask(index, { minimumWords: Number(event.target.value) })} value={task.minimumWords || ''} /><TextField label="Thời gian gợi ý (phút)" onChange={(event) => updateTask(index, { recommendedMinutes: Number(event.target.value) })} value={task.recommendedMinutes || ''} /></div><div className="mt-4"><TextField label="Yêu cầu ngắn" onChange={(event) => updateTask(index, { summary: event.target.value })} rows={2} textarea value={task.summary || ''} /></div><div className="mt-4"><TextField label="Đề bài, mỗi đoạn một dòng" onChange={(event) => updateTask(index, { promptParagraphs: event.target.value.split('\n').filter(Boolean) })} rows={5} textarea value={(task.promptParagraphs || []).join('\n')} /></div></div>)}<button className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-4 py-3 text-sm font-bold text-[#730014]" onClick={() => update({ tasks: [...tasks, createTask(tasks.length)] })} type="button"><Plus className="h-4 w-4" /> Thêm nhiệm vụ viết</button></Panel>;
}

function SpeakingEditor({ config, onChange }) {
  const parts = config?.parts || [];
  const update = (patch) => onChange({ ...config, ...patch });
  const updatePart = (index, patch) => update({ parts: parts.map((part, itemIndex) => itemIndex === index ? { ...part, ...patch } : part) });
  return <Panel className="space-y-5 p-6"><h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">Bài đánh giá kỹ năng Nói</h3><div className="grid gap-4 md:grid-cols-2"><TextField label="Tên đề" onChange={(event) => update({ title: event.target.value })} value={config?.title || ''} /><TextField label="Thời lượng (phút)" onChange={(event) => update({ durationMinutes: Number(event.target.value) })} value={config?.durationMinutes || ''} /></div>{parts.map((part, index) => <div className="rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-5" key={part.key || index}><div className="flex justify-end"><button className="inline-flex items-center gap-2 text-sm font-bold text-[#93000a]" onClick={() => update({ parts: parts.filter((_, itemIndex) => itemIndex !== index) })} type="button"><Trash2 className="h-4 w-4" /> Xóa phần</button></div><div className="grid gap-4 md:grid-cols-3"><TextField label="Tên phần" onChange={(event) => updatePart(index, { title: event.target.value })} value={part.title || ''} /><TextField label="Chuẩn bị (giây)" onChange={(event) => updatePart(index, { prepSeconds: Number(event.target.value) })} value={part.prepSeconds || 0} /><TextField label="Trả lời (giây)" onChange={(event) => updatePart(index, { answerSeconds: Number(event.target.value) })} value={part.answerSeconds || 0} /></div><div className="mt-4"><TextField label="Câu hỏi, mỗi dòng một câu" onChange={(event) => { const existing = part.prompts || []; updatePart(index, { prompts: event.target.value.split('\n').filter(Boolean).map((text, promptIndex) => ({ ...existing[promptIndex], text })) }); }} rows={6} textarea value={(part.prompts || []).map((prompt) => prompt.text || '').join('\n')} /></div></div>)}<button className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-4 py-3 text-sm font-bold text-[#730014]" onClick={() => update({ parts: [...parts, createSpeakingPart(parts.length)] })} type="button"><Plus className="h-4 w-4" /> Thêm phần nói</button></Panel>;
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

function formatBand(value) { return value == null ? '—' : Number(value).toFixed(1); }
function formatDate(value) { return value ? new Date(value).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : '—'; }
