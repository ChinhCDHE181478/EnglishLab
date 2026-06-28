import { useEffect, useMemo, useState } from 'react';
import { Archive, Building2, Edit3, Plus, RefreshCw, Save, Search, Video } from 'lucide-react';
import { useLocation } from 'react-router-dom';
import curriculumApi from '../../api/curriculumApi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import {
  CARD_CLASS,
  DANGER_BUTTON_CLASS,
  EMPTY_STATE_CLASS,
  ERROR_NOTICE_CLASS,
  FIELD_CLASS,
  GHOST_BUTTON_CLASS,
  PANEL_CLASS,
  PRIMARY_BUTTON_CLASS,
  SEARCH_INPUT_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
  TEXTAREA_CLASS,
} from '../../utils/formStyles';

const modeConfig = {
  OFFLINE: {
    title: 'Chương trình offline',
    subtitle: 'CRUD giáo trình mẫu dùng tại trung tâm theo band, target và cấp độ đầu vào.',
    deliveryMode: 'OFFLINE',
    icon: Building2,
  },
  VIRTUAL: {
    title: 'Chương trình virtual',
    subtitle: 'CRUD giáo trình trực tuyến với teacher guide và hoạt động tương tác riêng.',
    deliveryMode: 'VIRTUAL',
    icon: Video,
  },
};

const emptyForm = {
  title: '',
  code: '',
  slug: '',
  examCategory: 'IELTS',
  targetBand: '',
  targetScore: '',
  entryLevel: '',
  outcomes: '',
  teacherGuide: '',
  interactionActivities: '',
  totalSessions: 0,
  status: 'DRAFT',
  displayOrder: 0,
};

const examOptions = [
  { label: 'IELTS', value: 'IELTS' },
  { label: 'TOEIC', value: 'TOEIC' },
  { label: 'General English', value: 'GENERAL' },
];

const statusOptions = [
  { label: 'Nháp', value: 'DRAFT' },
  { label: 'Đã xuất bản', value: 'PUBLISHED' },
  { label: 'Lưu trữ', value: 'ARCHIVED' },
];

const toSlug = (value) => String(value || '')
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/đ/g, 'd')
  .replace(/Đ/g, 'D')
  .replace(/[^\w\s-]/g, '')
  .trim()
  .replace(/\s+/g, '-')
  .replace(/-+/g, '-')
  .toLowerCase();

const makeCode = (title, mode) => {
  const words = String(title || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .replace(/[^\w\s]/g, ' ')
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 4)
    .map((word) => word.slice(0, 4).toUpperCase());
  return [mode, ...words].filter(Boolean).join('-');
};

const toForm = (program = {}) => ({
  title: program.title || '',
  code: program.code || '',
  slug: program.slug || '',
  examCategory: program.examCategory || 'IELTS',
  targetBand: program.targetBand ?? '',
  targetScore: program.targetScore ?? '',
  entryLevel: program.entryLevel || '',
  outcomes: program.outcomes || '',
  teacherGuide: program.teacherGuide || '',
  interactionActivities: program.interactionActivities || '',
  totalSessions: program.totalSessions ?? 0,
  status: program.status || 'DRAFT',
  displayOrder: program.displayOrder ?? 0,
});

export default function ContentManagerTrainingProgramsPage({ mode = 'OFFLINE' }) {
  const location = useLocation();
  const resolvedMode = location.pathname.includes('virtual') ? 'VIRTUAL' : mode;
  const config = modeConfig[resolvedMode] || modeConfig.OFFLINE;
  const Icon = config.icon;

  const [programs, setPrograms] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadPrograms = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await curriculumApi.getCurriculumPrograms({ deliveryMode: config.deliveryMode });
      setPrograms(data);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách giáo trình.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPrograms();
    setEditingId(null);
    setForm(emptyForm);
  }, [config.deliveryMode]);

  const filteredPrograms = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) return programs;
    return programs.filter((item) => [
      item.title,
      item.code,
      item.slug,
      item.examCategory,
      item.entryLevel,
      item.status,
    ].filter(Boolean).some((value) => String(value).toLowerCase().includes(normalized)));
  }, [programs, keyword]);

  const sortedPrograms = useMemo(
    () => [...filteredPrograms].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || String(a.title).localeCompare(String(b.title))),
    [filteredPrograms],
  );

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    sortedPrograms,
    8,
    `${keyword}-${config.deliveryMode}`,
  );

  const updateForm = (patch) => setForm((current) => ({ ...current, ...patch }));

  const resetForm = () => {
    setEditingId(null);
    setForm(emptyForm);
    setError('');
    setSuccess('');
  };

  const openEdit = (program) => {
    setEditingId(program.id);
    setForm(toForm(program));
    setSuccess('');
    setError('');
  };

  const saveProgram = async () => {
    if (!form.title.trim()) {
      setError('Vui lòng nhập tên giáo trình.');
      return;
    }
    const generatedCode = form.code.trim() || makeCode(form.title, config.deliveryMode);
    if (!generatedCode) {
      setError('Vui lòng nhập mã giáo trình.');
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    const payload = {
      ...form,
      code: generatedCode,
      slug: form.slug.trim() || toSlug(form.title),
      deliveryMode: config.deliveryMode,
      targetBand: form.targetBand === '' ? null : Number(form.targetBand),
      targetScore: form.targetScore === '' ? null : Number(form.targetScore),
      totalSessions: Number(form.totalSessions || 0),
      displayOrder: Number(form.displayOrder || 0),
    };
    try {
      const saved = editingId
        ? await curriculumApi.updateCurriculumProgram(editingId, payload)
        : await curriculumApi.createCurriculumProgram(payload);
      setPrograms((current) => {
        if (editingId) {
          return current.map((item) => (String(item.id) === String(saved.id) ? saved : item));
        }
        return [saved, ...current];
      });
      setEditingId(saved.id);
      setForm(toForm(saved));
      setSuccess(editingId ? 'Đã cập nhật giáo trình.' : 'Đã tạo giáo trình mới.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được giáo trình.');
    } finally {
      setWorking(false);
    }
  };

  const archiveProgram = async (program) => {
    if (!window.confirm(`Lưu trữ giáo trình "${program.title}"?`)) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await curriculumApi.archiveCurriculumProgram(program.id);
      setPrograms((current) => current.map((item) => (
        String(item.id) === String(program.id) ? { ...item, status: 'ARCHIVED' } : item
      )));
      if (String(editingId) === String(program.id)) {
        updateForm({ status: 'ARCHIVED' });
      }
      setSuccess('Đã lưu trữ giáo trình.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu trữ được giáo trình.');
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
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={resetForm} className={SECONDARY_BUTTON_CLASS}>
            <Plus className="h-4 w-4" />
            Tạo mới
          </button>
          <button type="button" onClick={loadPrograms} className={SECONDARY_BUTTON_CLASS}>
            <RefreshCw className="h-4 w-4" />
            Tải lại
          </button>
        </div>
      </div>

      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_440px]">
        <section className="space-y-4">
          <div className={PANEL_CLASS}>
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Tìm giáo trình, code, band hoặc cấp độ..."
                className={SEARCH_INPUT_CLASS}
              />
            </div>
          </div>

          {loading ? (
            <p className="text-sm font-semibold text-slate-500">Đang tải giáo trình...</p>
          ) : sortedPrograms.length === 0 ? (
            <div className={EMPTY_STATE_CLASS}>Chưa có giáo trình {config.deliveryMode === 'OFFLINE' ? 'offline' : 'virtual'}.</div>
          ) : (
            <div className="space-y-3">
              {pageItems.map((program) => (
                <article key={program.id} className={`${CARD_CLASS} transition hover:border-[#dfbfbd]`}>
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="break-words font-['Manrope'] text-lg font-extrabold text-slate-900">{program.title}</h3>
                        <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">{program.status}</span>
                      </div>
                      <p className="mt-1 text-sm font-semibold text-slate-500">{program.code} · {program.examCategory}</p>
                      <div className="mt-3 flex flex-wrap gap-2 text-xs font-semibold text-slate-600">
                        {program.targetBand ? <span className="rounded-full bg-slate-50 px-2 py-1">IELTS {program.targetBand}</span> : null}
                        {program.targetScore ? <span className="rounded-full bg-slate-50 px-2 py-1">Target {program.targetScore}</span> : null}
                        {program.entryLevel ? <span className="rounded-full bg-slate-50 px-2 py-1">{program.entryLevel}</span> : null}
                        <span className="rounded-full bg-slate-50 px-2 py-1">{program.totalSessions || 0} buổi</span>
                      </div>
                      {program.outcomes ? <p className="mt-3 line-clamp-2 text-sm leading-6 text-slate-600">{program.outcomes}</p> : null}
                    </div>
                    <div className="flex shrink-0 flex-wrap gap-2">
                      <button type="button" onClick={() => openEdit(program)} className={GHOST_BUTTON_CLASS}>
                        <Edit3 className="h-3.5 w-3.5" />
                        Sửa
                      </button>
                      <button type="button" onClick={() => archiveProgram(program)} disabled={working} className={DANGER_BUTTON_CLASS}>
                        <Archive className="h-3.5 w-3.5" />
                        Lưu trữ
                      </button>
                    </div>
                  </div>
                </article>
              ))}
              <Pagination page={page} totalPages={totalPages} onChange={setPage} totalItems={totalItems} pageSize={8} />
            </div>
          )}
        </section>

        <aside className={PANEL_CLASS}>
          <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">
            {editingId ? 'Chỉnh sửa giáo trình' : 'Tạo giáo trình'}
          </h3>
          <p className="mt-1 text-sm text-slate-500">Giáo trình là nguồn nội dung dùng chung; lớp học chỉ chọn giáo trình này khi mở lớp.</p>

          <div className="mt-5 space-y-4">
            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Tên giáo trình</span>
              <input
                value={form.title}
                onChange={(event) => updateForm({
                  title: event.target.value,
                  code: form.code || makeCode(event.target.value, config.deliveryMode),
                  slug: form.slug || toSlug(event.target.value),
                })}
                className={FIELD_CLASS}
              />
            </label>

            <div className="grid gap-3 md:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Mã</span>
                <input value={form.code} onChange={(event) => updateForm({ code: event.target.value })} className={FIELD_CLASS} />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Slug</span>
                <input value={form.slug} onChange={(event) => updateForm({ slug: event.target.value })} className={FIELD_CLASS} />
              </label>
            </div>

            <div className="grid gap-3 md:grid-cols-2">
              <div>
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Nhóm thi</span>
                <BrandedSelect
                  value={form.examCategory}
                  onChange={(event) => updateForm({ examCategory: event.target.value })}
                  options={examOptions}
                />
              </div>
              <div>
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Trạng thái</span>
                <BrandedSelect
                  value={form.status}
                  onChange={(event) => updateForm({ status: event.target.value })}
                  options={statusOptions}
                />
              </div>
            </div>

            <div className="grid gap-3 md:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Band IELTS</span>
                <input
                  type="number"
                  min="0"
                  max="9"
                  step="0.5"
                  value={form.targetBand}
                  onChange={(event) => updateForm({ targetBand: event.target.value })}
                  className={FIELD_CLASS}
                />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Target TOEIC</span>
                <input
                  type="number"
                  min="0"
                  value={form.targetScore}
                  onChange={(event) => updateForm({ targetScore: event.target.value })}
                  className={FIELD_CLASS}
                />
              </label>
            </div>

            <div className="grid gap-3 md:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Cấp độ đầu vào</span>
                <input value={form.entryLevel} onChange={(event) => updateForm({ entryLevel: event.target.value })} className={FIELD_CLASS} />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Tổng số buổi</span>
                <input
                  type="number"
                  min="0"
                  value={form.totalSessions}
                  onChange={(event) => updateForm({ totalSessions: event.target.value })}
                  className={FIELD_CLASS}
                />
              </label>
            </div>

            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Chuẩn đầu ra</span>
              <textarea value={form.outcomes} onChange={(event) => updateForm({ outcomes: event.target.value })} rows={4} className={TEXTAREA_CLASS} />
            </label>

            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Hướng dẫn giảng viên</span>
              <textarea value={form.teacherGuide} onChange={(event) => updateForm({ teacherGuide: event.target.value })} rows={4} className={TEXTAREA_CLASS} />
            </label>

            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Hoạt động tương tác</span>
              <textarea value={form.interactionActivities} onChange={(event) => updateForm({ interactionActivities: event.target.value })} rows={4} className={TEXTAREA_CLASS} />
            </label>

            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Thứ tự hiển thị</span>
              <input
                type="number"
                min="0"
                value={form.displayOrder}
                onChange={(event) => updateForm({ displayOrder: event.target.value })}
                className={FIELD_CLASS}
              />
            </label>

            <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4">
              <button type="button" disabled={working} onClick={saveProgram} className={PRIMARY_BUTTON_CLASS}>
                <Save className="h-4 w-4" />
                {working ? 'Đang lưu...' : 'Lưu giáo trình'}
              </button>
              <button type="button" onClick={resetForm} className={SECONDARY_BUTTON_CLASS}>
                <Plus className="h-4 w-4" />
                Mới
              </button>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
