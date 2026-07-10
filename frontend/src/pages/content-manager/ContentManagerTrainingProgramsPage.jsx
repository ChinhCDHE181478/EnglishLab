import { useEffect, useMemo, useState } from 'react';
import { Plus, RefreshCw, Save } from 'lucide-react';
import { useLocation } from 'react-router-dom';
import classroomApi from '../../api/classroomApi';
import curriculumApi from '../../api/curriculumApi';
import {
  ProgramFilterBar,
  ProgramPageHero,
  ProgramTable,
} from '../../components/curriculum/CurriculumProgramUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import {
  DANGER_BUTTON_CLASS,
  ERROR_NOTICE_CLASS,
  FIELD_CLASS,
  GHOST_BUTTON_CLASS,
  PRIMARY_BUTTON_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
  TEXTAREA_CLASS,
} from '../../utils/formStyles';

const modeConfig = {
  OFFLINE: {
    title: 'Danh sách chương trình',
    subtitle: 'Lọc, theo dõi trạng thái và thao tác nhanh trên chương trình đào tạo.',
    deliveryMode: 'OFFLINE',
  },
  VIRTUAL: {
    title: 'Danh sách chương trình',
    subtitle: 'Lọc, theo dõi trạng thái và thao tác nhanh trên chương trình đào tạo.',
    deliveryMode: 'VIRTUAL',
  },
};

const emptyForm = {
  title: '',
  code: '',
  slug: '',
  curriculumProgramId: '',
  materialIds: [],
  shortDescription: '',
  description: '',
  targetScore: '',
  entryLevel: '',
  targetOutcome: '',
  defaultCapacity: 18,
  price: '',
  salePrice: '',
  duration: '',
  studyMode: 'Offline tại trung tâm',
  syllabusSummary: '',
  programOutcomes: '',
  teacherGuide: '',
  interactionActivities: '',
  status: 'DRAFT',
  displayOrder: 0,
  featured: false,
};

const examOptions = [
  { label: 'IELTS', value: 'IELTS' },
  { label: 'TOEIC', value: 'TOEIC' },
  { label: 'General English', value: 'GENERAL' },
];

const statusOptions = [
  { label: 'Nháp', value: 'DRAFT' },
  { label: 'Chờ duyệt', value: 'PENDING_REVIEW' },
  { label: 'Đã xuất bản', value: 'PUBLISHED' },
  { label: 'Từ chối', value: 'REJECTED' },
  { label: 'Lưu trữ', value: 'ARCHIVED' },
];

const formStatusOptions = [
  { label: 'Nháp', value: 'DRAFT' },
  { label: 'Đã xuất bản', value: 'PUBLISHED' },
  { label: 'Lưu trữ', value: 'ARCHIVED' },
];

const usageFilterOptions = [
  { label: 'Tất cả', value: 'ALL' },
  { label: 'Đang có lớp dùng', value: 'ACTIVE' },
  { label: 'Chưa có lớp dùng', value: 'UNUSED' },
  { label: 'Đã từng có lớp', value: 'USED' },
];

const sortOptions = [
  { label: 'Mới cập nhật', value: 'UPDATED_DESC' },
  { label: 'Tên A-Z', value: 'TITLE_ASC' },
  { label: 'Nhiều lớp đang dùng', value: 'ACTIVE_DESC' },
  { label: 'Thứ tự hiển thị', value: 'DISPLAY_ORDER' },
];

const platformOptions = [
  { label: 'Lark', value: 'LARK' },
  { label: 'Zoom', value: 'ZOOM' },
  { label: 'Google Meet', value: 'GOOGLE_MEET' },
  { label: 'Liên kết thủ công', value: 'MANUAL' },
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
  curriculumProgramId: program.curriculumProgramId ? String(program.curriculumProgramId) : '',
  materialIds: (program.materials || []).map((item) => String(item.materialId || item.id)),
  shortDescription: program.shortDescription || '',
  description: program.description || '',
  targetScore: program.targetScore ?? '',
  entryLevel: program.entryLevel || '',
  targetOutcome: program.targetOutcome || '',
  defaultCapacity: program.defaultCapacity ?? 18,
  price: program.price ?? '',
  salePrice: program.salePrice ?? '',
  duration: program.duration || '',
  studyMode: program.studyMode || (program.deliveryMode === 'VIRTUAL' ? 'Virtual' : 'Offline tại trung tâm'),
  syllabusSummary: program.syllabusSummary || '',
  programOutcomes: program.programOutcomes || '',
  teacherGuide: program.teacherGuide || '',
  interactionActivities: program.interactionActivities || '',
  status: program.status || 'DRAFT',
  displayOrder: program.displayOrder ?? 0,
  featured: Boolean(program.featured),
});

export default function ContentManagerTrainingProgramsPage({ mode = 'OFFLINE' }) {
  const location = useLocation();
  const resolvedMode = location.pathname.includes('virtual') ? 'VIRTUAL' : mode;
  const config = modeConfig[resolvedMode] || modeConfig.OFFLINE;

  const [programs, setPrograms] = useState([]);
  const [curriculums, setCurriculums] = useState([]);
  const [materials, setMaterials] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [examFilter, setExamFilter] = useState('ALL');
  const [levelFilter, setLevelFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [usageFilter, setUsageFilter] = useState('ALL');
  const [platformFilter, setPlatformFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState('UPDATED_DESC');
  const [editorOpen, setEditorOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadPrograms = async () => {
    setLoading(true);
    setError('');
    try {
      const [programData, curriculumData, materialData] = await Promise.all([
        classroomApi.getContentManagerPrograms(config.deliveryMode),
        curriculumApi.getCurriculumPrograms(),
        classroomApi.getContentManagerMaterialLibrary(),
      ]);
      setPrograms(programData);
      setCurriculums(curriculumData);
      setMaterials(materialData);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách chương trình.');
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
    return programs.filter((item) => {
      if (normalized) {
        const haystack = [
          item.title,
          item.code,
          item.slug,
          item.curriculumProgramTitle,
          item.curriculumProgramCode,
          item.curriculumProgramExamCategory,
          item.entryLevel,
          item.status,
        ].filter(Boolean).map((value) => String(value).toLowerCase());
        if (!haystack.some((value) => value.includes(normalized))) return false;
      }
      if (examFilter !== 'ALL' && item.curriculumProgramExamCategory !== examFilter) return false;
      if (levelFilter.trim() && !String(item.entryLevel || '').toLowerCase().includes(levelFilter.trim().toLowerCase())) return false;
      if (statusFilter !== 'ALL' && item.status !== statusFilter) return false;
      if (usageFilter === 'ACTIVE' && !(item.activeClassroomCount > 0)) return false;
      if (usageFilter === 'UNUSED' && (item.activeClassroomCount > 0 || item.classroomCount > 0)) return false;
      if (usageFilter === 'USED' && !(item.classroomCount > 0)) return false;
      return true;
    });
  }, [programs, keyword, examFilter, levelFilter, statusFilter, usageFilter, platformFilter, config.deliveryMode]);

  const sortedPrograms = useMemo(() => {
    const list = [...filteredPrograms];
    if (sortBy === 'TITLE_ASC') {
      return list.sort((a, b) => String(a.title).localeCompare(String(b.title)));
    }
    if (sortBy === 'ACTIVE_DESC') {
      return list.sort((a, b) => (b.activeClassroomCount ?? 0) - (a.activeClassroomCount ?? 0) || String(a.title).localeCompare(String(b.title)));
    }
    if (sortBy === 'DISPLAY_ORDER') {
      return list.sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || String(a.title).localeCompare(String(b.title)));
    }
    return list.sort((a, b) => new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0) || String(a.title).localeCompare(String(b.title)));
  }, [filteredPrograms, sortBy]);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    sortedPrograms,
    8,
    `${keyword}-${examFilter}-${levelFilter}-${statusFilter}-${usageFilter}-${platformFilter}-${sortBy}-${config.deliveryMode}`,
  );

  const resetFilters = () => {
    setKeyword('');
    setExamFilter('ALL');
    setLevelFilter('');
    setStatusFilter('ALL');
    setUsageFilter('ALL');
    setPlatformFilter('ALL');
    setSortBy('UPDATED_DESC');
    setPage(1);
  };

  const detailBasePath = config.deliveryMode === 'VIRTUAL'
    ? '/content-manager/virtual-programs'
    : '/content-manager/offline-programs';

  const updateForm = (patch) => setForm((current) => ({ ...current, ...patch }));

  const resetForm = () => {
    setEditingId(null);
    setForm(emptyForm);
    setEditorOpen(false);
    setError('');
    setSuccess('');
  };

  const openCreate = () => {
    setEditingId(null);
    setForm(emptyForm);
    setEditorOpen(true);
    setSuccess('');
    setError('');
  };

  const openEdit = async (program) => {
    setWorking(true);
    setSuccess('');
    setError('');
    try {
      const detail = await classroomApi.getContentManagerProgram(program.id);
      setEditingId(detail.id);
      setForm(toForm(detail));
      setEditorOpen(true);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được thông tin chương trình.');
    } finally {
      setWorking(false);
    }
  };

  const saveProgram = async () => {
    if (!form.title.trim()) {
      setError('Vui lòng nhập tên chương trình.');
      return;
    }
    const generatedCode = form.code.trim() || makeCode(form.title, config.deliveryMode);
    if (!generatedCode) {
      setError('Vui lòng nhập mã chương trình.');
      return;
    }
    if (!form.curriculumProgramId) {
      setError('Vui lòng chọn giáo trình gốc cho chương trình.');
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
      curriculumProgramId: Number(form.curriculumProgramId),
      materialIds: form.materialIds.map((id) => Number(id)),
      targetScore: form.targetScore || null,
      defaultCapacity: Number(form.defaultCapacity || 1),
      price: form.price === '' ? 0 : Number(form.price),
      salePrice: form.salePrice === '' ? null : Number(form.salePrice),
      displayOrder: Number(form.displayOrder || 0),
      featured: Boolean(form.featured),
    };
    try {
      const saved = editingId
        ? await classroomApi.updateContentManagerProgram(editingId, payload)
        : await classroomApi.createContentManagerProgram(payload);
      setPrograms((current) => {
        if (editingId) {
          return current.map((item) => (String(item.id) === String(saved.id) ? saved : item));
        }
        return [saved, ...current];
      });
      setEditingId(saved.id);
      setForm(toForm(saved));
      setEditorOpen(true);
      setSuccess(editingId ? 'Đã cập nhật chương trình.' : 'Đã tạo chương trình mới.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được chương trình.');
    } finally {
      setWorking(false);
    }
  };

  const cloneProgram = async (program) => {
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const clone = await classroomApi.cloneContentManagerProgram(program.id);
      const draftClone = { ...clone, status: 'DRAFT', statusLabel: 'Bản nháp' };
      setPrograms((current) => [draftClone, ...current]);
      setEditingId(draftClone.id);
      setForm(toForm(draftClone));
      setSuccess(`Đã nhân bản chương trình thành "${draftClone.title}" (trạng thái nháp).`);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không nhân bản được chương trình.');
    } finally {
      setWorking(false);
    }
  };

  const archiveProgram = async (program) => {
    if (program.activeClassroomCount > 0) {
      setError(`Chương trình đang được ${program.activeClassroomCount} lớp sắp khai giảng / đang diễn ra sử dụng, không thể lưu trữ.`);
      return;
    }
    if (!window.confirm(`Lưu trữ chương trình "${program.title}"?`)) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await classroomApi.archiveContentManagerProgram(program.id);
      setPrograms((current) => current.map((item) => (
        String(item.id) === String(program.id) ? { ...item, status: 'ARCHIVED' } : item
      )));
      if (String(editingId) === String(program.id)) {
        updateForm({ status: 'ARCHIVED' });
      }
      setSuccess('Đã lưu trữ chương trình.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu trữ được chương trình.');
    } finally {
      setWorking(false);
    }
  };

  const publishedCount = programs.filter((p) => p.status === 'PUBLISHED').length;
  const activeUsageCount = programs.filter((p) => (p.activeClassroomCount ?? 0) > 0).length;

  return (
    <div className="space-y-5">
      <ProgramPageHero
        mode={config.deliveryMode}
        stats={[
          { label: 'Tổng chương trình', value: programs.length },
          { label: 'Đã xuất bản', value: publishedCount },
          { label: 'Lớp đang dùng', value: activeUsageCount },
        ]}
        subtitle={config.subtitle}
        title={config.title}
        actions={(
          <>
            <button className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#730014] active:scale-[0.98]" onClick={openCreate} type="button">
              <Plus className="h-4 w-4" />
              Tạo chương trình
            </button>
            <button className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg border border-[#dcc0bf]/40 bg-white px-5 py-3 text-sm font-bold text-[#4b0009] shadow-sm transition hover:bg-[#eff4ff] active:scale-[0.98]" onClick={loadPrograms} type="button">
              <RefreshCw className="h-4 w-4" />
              Tải lại
            </button>
          </>
        )}
      />

      {error ? <div className={ERROR_NOTICE_CLASS}>{error}</div> : null}
      {success ? <div className={SUCCESS_NOTICE_CLASS}>{success}</div> : null}

      <ProgramFilterBar
        examFilter={examFilter}
        examOptions={examOptions}
        keyword={keyword}
        levelFilter={levelFilter}
        loading={loading}
        onExamFilterChange={(event) => setExamFilter(event.target.value)}
        onKeywordChange={setKeyword}
        onLevelFilterChange={setLevelFilter}
        onPlatformFilterChange={(event) => setPlatformFilter(event.target.value)}
        onRefresh={loadPrograms}
        onReset={resetFilters}
        onSortChange={(event) => setSortBy(event.target.value)}
        onStatusFilterChange={(event) => setStatusFilter(event.target.value)}
        onUsageFilterChange={(event) => setUsageFilter(event.target.value)}
        platformFilter={platformFilter}
        platformOptions={platformOptions}
        resultCount={totalItems}
        showPlatform={false}
        sortBy={sortBy}
        sortOptions={sortOptions}
        statusFilter={statusFilter}
        statusOptions={statusOptions}
        usageFilter={usageFilter}
        usageOptions={usageFilterOptions}
      />

      <ProgramTable
        detailBasePath={detailBasePath}
        loading={loading}
        onArchive={archiveProgram}
        onClone={cloneProgram}
        onEdit={openEdit}
        onPageChange={setPage}
        page={page}
        pageSize={8}
        programs={pageItems}
        totalItems={totalItems}
        totalPages={totalPages}
        working={working}
      />

      {editorOpen ? (
        <ProgramEditorModal onClose={resetForm}>
        <div className="rounded-xl bg-white p-5 shadow-2xl">
          <div className="mb-5 flex items-start justify-between gap-3">
            <div>
              <h3 className="font-['Manrope'] text-xl font-extrabold text-[#0b1c30]">
                {editingId ? 'Chỉnh sửa chương trình' : 'Tạo chương trình mới'}
              </h3>
              <p className="mt-1 text-sm text-[#564241]">Metadata chương trình đào tạo — phần giáo trình/unit được biên soạn trong Biên soạn giáo trình.</p>
            </div>
            <button className={GHOST_BUTTON_CLASS} onClick={resetForm} type="button">Đóng</button>
          </div>

          <div className="max-h-[calc(100dvh-220px)] overflow-y-auto pr-1">
          <div className="space-y-4">
            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Tên chương trình</span>
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
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Giáo trình gốc</span>
                <BrandedSelect
                  value={form.curriculumProgramId}
                  onChange={(event) => updateForm({ curriculumProgramId: event.target.value })}
                  options={curriculums.map((item) => ({
                    label: item.title,
                    value: String(item.id),
                    description: [item.code, item.examCategory, item.totalUnits ? `${item.totalUnits} unit` : null].filter(Boolean).join(' · '),
                  }))}
                  placeholder={curriculums.length ? 'Chọn giáo trình' : 'Kho giáo trình đang trống'}
                />
              </div>
              <div>
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Trạng thái</span>
                <BrandedSelect
                  onChange={(event) => updateForm({ status: event.target.value })}
                  options={formStatusOptions}
                  value={['DRAFT', 'PUBLISHED', 'ARCHIVED'].includes(form.status) ? form.status : 'DRAFT'}
                />
              </div>
            </div>

            <div className="grid gap-3 md:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Cấp độ đầu vào</span>
                <input value={form.entryLevel} onChange={(event) => updateForm({ entryLevel: event.target.value })} className={FIELD_CLASS} />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Target</span>
                <input value={form.targetScore} onChange={(event) => updateForm({ targetScore: event.target.value })} className={FIELD_CLASS} />
              </label>
            </div>

            <div className="grid gap-3 md:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Sĩ số mặc định</span>
                <input type="number" min="1" value={form.defaultCapacity} onChange={(event) => updateForm({ defaultCapacity: event.target.value })} className={FIELD_CLASS} />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Thời lượng</span>
                <input value={form.duration} onChange={(event) => updateForm({ duration: event.target.value })} className={FIELD_CLASS} />
              </label>
            </div>

            <div className="grid gap-3 md:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Học phí</span>
                <input type="number" min="0" value={form.price} onChange={(event) => updateForm({ price: event.target.value })} className={FIELD_CLASS} />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Giá ưu đãi</span>
                <input type="number" min="0" value={form.salePrice} onChange={(event) => updateForm({ salePrice: event.target.value })} className={FIELD_CLASS} />
              </label>
            </div>

            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Mô tả ngắn</span>
              <input value={form.shortDescription} onChange={(event) => updateForm({ shortDescription: event.target.value })} className={FIELD_CLASS} />
            </label>

            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Mô tả chương trình</span>
              <textarea value={form.description} onChange={(event) => updateForm({ description: event.target.value })} rows={3} className={TEXTAREA_CLASS} />
            </label>

            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Chuẩn đầu ra chương trình</span>
              <textarea value={form.targetOutcome} onChange={(event) => updateForm({ targetOutcome: event.target.value })} rows={3} className={TEXTAREA_CLASS} />
            </label>

            <section className="rounded-xl border border-[#dcc0bf]/30 bg-[#f8f9ff] p-4">
              <div className="mb-3 flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-extrabold text-[#4b0009]">Tài liệu đi kèm chương trình</p>
                  <p className="mt-1 text-xs text-[#584140]">Các tài liệu này sẽ được tự động gắn vào lớp khi lớp chọn chương trình.</p>
                </div>
                <span className="rounded-lg bg-white px-2.5 py-1 text-xs font-bold text-[#4b0009]">{form.materialIds.length} tài liệu</span>
              </div>
              <div className="grid max-h-56 gap-2 overflow-y-auto pr-1">
                {materials.length ? materials.map((item) => {
                  const value = String(item.id);
                  const checked = form.materialIds.includes(value);
                  return (
                    <label className={`flex cursor-pointer items-start gap-3 rounded-lg border px-3 py-2.5 text-sm transition ${checked ? 'border-[#4b0009] bg-white' : 'border-[#dcc0bf]/35 bg-[#fcfbfb] hover:bg-white'}`} key={item.id}>
                      <input
                        checked={checked}
                        className="mt-1 h-4 w-4 accent-[#4b0009]"
                        onChange={(event) => updateForm({
                          materialIds: event.target.checked
                            ? [...form.materialIds, value]
                            : form.materialIds.filter((id) => id !== value),
                        })}
                        type="checkbox"
                      />
                      <span>
                        <span className="block font-bold text-[#0b1c30]">{item.title}</span>
                        <span className="text-xs text-[#584140]">{[item.materialType, item.fileType, item.provider].filter(Boolean).join(' · ') || 'Tài liệu'}</span>
                      </span>
                    </label>
                  );
                }) : (
                  <p className="rounded-lg border border-dashed border-[#dcc0bf]/50 bg-white px-3 py-6 text-center text-sm font-semibold text-[#584140]">Kho học liệu đang trống.</p>
                )}
              </div>
            </section>

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
                {working ? 'Đang lưu...' : 'Lưu chương trình'}
              </button>
              <button type="button" onClick={resetForm} className={SECONDARY_BUTTON_CLASS}>
                <Plus className="h-4 w-4" />
                Mới
              </button>
            </div>
          </div>
          </div>
        </div>
        </ProgramEditorModal>
      ) : null}
    </div>
  );
}

function ProgramEditorModal({ children, onClose }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-[#1a0004]/45 px-3 py-4 backdrop-blur-sm sm:px-6" role="dialog" aria-modal="true">
      <button aria-label="Đóng modal" className="fixed inset-0 cursor-default" onClick={onClose} type="button" />
      <div className="relative z-10 w-full max-w-[760px]">
        {children}
      </div>
    </div>
  );
}
