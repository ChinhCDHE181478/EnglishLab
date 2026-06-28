import { useEffect, useMemo, useState } from 'react';
import { Building2, RefreshCw, Save, Search, Video } from 'lucide-react';
import { useLocation } from 'react-router-dom';
import classroomApi from '../../api/classroomApi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import {
  ERROR_NOTICE_CLASS,
  FIELD_CLASS,
  PANEL_CLASS,
  PRIMARY_BUTTON_CLASS,
  SEARCH_INPUT_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
  TEXTAREA_CLASS,
} from '../../utils/formStyles';

const modeConfig = {
  OFFLINE: {
    title: 'Chương trình học tại trung tâm',
    subtitle: 'Quản lý đầu ra, cấp độ, địa điểm và tài liệu cho lớp offline.',
    deliveryMode: 'OFFLINE',
    icon: Building2,
  },
  VIRTUAL: {
    title: 'Chương trình học trực tuyến',
    subtitle: 'Biên soạn hướng dẫn giảng viên, hoạt động tương tác và kế hoạch buổi học ảo.',
    deliveryMode: 'VIRTUAL',
    icon: Video,
  },
};

const emptyForm = {
  entryLevel: '',
  targetOutcome: '',
  programOutcomes: '',
  teacherGuide: '',
  interactionActivities: '',
  syllabusSummary: '',
};

export default function ContentManagerTrainingProgramsPage({ mode = 'OFFLINE' }) {
  const location = useLocation();
  const resolvedMode = location.pathname.includes('virtual') ? 'VIRTUAL' : mode;
  const config = modeConfig[resolvedMode] || modeConfig.OFFLINE;
  const Icon = config.icon;

  const [programs, setPrograms] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [form, setForm] = useState(emptyForm);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadPrograms = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getContentManagerPrograms(config.deliveryMode);
      setPrograms(data);
      if (!selectedId && data.length > 0) {
        setSelectedId(String(data[0].id));
      }
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách chương trình.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPrograms();
  }, [config.deliveryMode]);

  const selectedProgram = useMemo(
    () => programs.find((item) => String(item.id) === String(selectedId)) || null,
    [programs, selectedId],
  );

  useEffect(() => {
    if (!selectedProgram) {
      setForm(emptyForm);
      return;
    }
    setForm({
      entryLevel: selectedProgram.entryLevel || '',
      targetOutcome: selectedProgram.targetOutcome || '',
      programOutcomes: selectedProgram.programOutcomes || '',
      teacherGuide: selectedProgram.teacherGuide || '',
      interactionActivities: selectedProgram.interactionActivities || '',
      syllabusSummary: selectedProgram.syllabusSummary || '',
    });
  }, [selectedProgram]);

  const filteredPrograms = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) return programs;
    return programs.filter((item) => [item.title, item.slug, item.entryLevel]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(normalized)));
  }, [programs, keyword]);

  const programOptions = filteredPrograms.map((item) => ({
    label: `${item.title} · ${item.entryLevel || 'Chưa gắn cấp độ'}`,
    value: String(item.id),
  }));

  const saveProgram = async () => {
    if (!selectedId) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const updated = await classroomApi.updateContentManagerProgramProfile(selectedId, form);
      setPrograms((current) => current.map((item) => (String(item.id) === String(selectedId) ? { ...item, ...updated } : item)));
      setSuccess('Đã lưu hồ sơ chương trình.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được hồ sơ chương trình.');
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="mb-2 inline-flex items-center gap-2 rounded-full bg-[#730014]/10 px-3 py-1 text-xs font-bold text-[#730014]">
            <Icon className="h-3.5 w-3.5" />
            {config.deliveryMode === 'OFFLINE' ? 'Offline' : 'Virtual'}
          </div>
          <h2 className="text-2xl font-bold text-slate-900">{config.title}</h2>
          <p className="mt-1 max-w-3xl text-sm text-slate-600">{config.subtitle}</p>
        </div>
        <button type="button" onClick={loadPrograms} className={SECONDARY_BUTTON_CLASS}>
          <RefreshCw className="h-4 w-4" />
          Tải lại
        </button>
      </div>

      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      <div className="grid gap-6 lg:grid-cols-[320px_minmax(0,1fr)]">
        <div className={`${PANEL_CLASS} space-y-4`}>
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Tìm lớp học..."
              className={SEARCH_INPUT_CLASS}
            />
          </div>
          {loading ? (
            <p className="text-sm text-slate-500">Đang tải...</p>
          ) : programOptions.length === 0 ? (
            <p className="text-sm text-slate-500">Chưa có lớp {config.deliveryMode === 'OFFLINE' ? 'offline' : 'trực tuyến'}.</p>
          ) : (
            <BrandedSelect
              label="Chọn lớp học"
              value={selectedId}
              onChange={setSelectedId}
              options={programOptions}
            />
          )}
        </div>

        <div className={PANEL_CLASS}>
          {!selectedProgram ? (
            <p className="text-sm text-slate-500">Chọn một lớp để chỉnh sửa hồ sơ chương trình.</p>
          ) : (
            <div className="space-y-4">
              <div>
                <h3 className="text-lg font-bold text-slate-900">{selectedProgram.title}</h3>
                <p className="text-sm text-slate-500">{selectedProgram.deliveryModeLabel} · {selectedProgram.classroomStatus}</p>
              </div>
              <div className="grid gap-4 md:grid-cols-2">
                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Cấp độ đầu vào</span>
                  <input value={form.entryLevel} onChange={(e) => setForm({ ...form, entryLevel: e.target.value })} className={FIELD_CLASS} />
                </label>
                <label className="block md:col-span-2">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Đầu ra mục tiêu</span>
                  <textarea value={form.targetOutcome} onChange={(e) => setForm({ ...form, targetOutcome: e.target.value })} rows={2} className={TEXTAREA_CLASS} />
                </label>
                <label className="block md:col-span-2">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Chuẩn đầu ra chương trình</span>
                  <textarea value={form.programOutcomes} onChange={(e) => setForm({ ...form, programOutcomes: e.target.value })} rows={4} className={TEXTAREA_CLASS} />
                </label>
                {config.deliveryMode === 'VIRTUAL' && (
                  <>
                    <label className="block md:col-span-2">
                      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Hướng dẫn giảng viên</span>
                      <textarea value={form.teacherGuide} onChange={(e) => setForm({ ...form, teacherGuide: e.target.value })} rows={4} className={TEXTAREA_CLASS} />
                    </label>
                    <label className="block md:col-span-2">
                      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Hoạt động tương tác</span>
                      <textarea value={form.interactionActivities} onChange={(e) => setForm({ ...form, interactionActivities: e.target.value })} rows={4} className={TEXTAREA_CLASS} />
                    </label>
                  </>
                )}
                <label className="block md:col-span-2">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Tóm tắt giáo trình</span>
                  <textarea value={form.syllabusSummary} onChange={(e) => setForm({ ...form, syllabusSummary: e.target.value })} rows={3} className={TEXTAREA_CLASS} />
                </label>
              </div>
              <button
                type="button"
                disabled={working}
                onClick={saveProgram}
                className={PRIMARY_BUTTON_CLASS}
              >
                <Save className="h-4 w-4" />
                {working ? 'Đang lưu...' : 'Lưu hồ sơ chương trình'}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
