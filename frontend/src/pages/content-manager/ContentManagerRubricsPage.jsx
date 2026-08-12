import { useEffect, useMemo, useRef, useState } from 'react';
import { Archive, CheckCircle2, Edit3, Layers3, Plus, RefreshCw, RotateCcw, Save, Search, SlidersHorizontal, Trash2, X, XCircle } from 'lucide-react';
import courseApi from '../../api/courseApi';
import {
  ManagerEmptyState,
  ManagerFilterBar,
  ManagerStatsGrid,
  ManagerStatusBadge,
  ManagerTable,
  ManagerTablePagination,
} from '../../components/content-manager/ManagerListUi';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { usePagination } from '../../components/ui/Pagination';
import { stripRichTextToPlain } from '../../utils/lessonRichText';

const skillOptions = [
  { label: 'Tất cả kỹ năng', value: 'ALL' },
  { label: 'Listening', value: 'LISTENING' },
  { label: 'Reading', value: 'READING' },
  { label: 'Writing', value: 'WRITING' },
  { label: 'Speaking', value: 'SPEAKING' },
  { label: 'Vocabulary', value: 'VOCABULARY' },
  { label: 'Grammar', value: 'GRAMMAR' },
  { label: 'Mixed', value: 'MIXED' },
];

const activeOptions = [
  { label: 'Đang dùng', value: 'ACTIVE' },
  { label: 'Tất cả', value: 'ALL' },
  { label: 'Đã tạm ngưng', value: 'INACTIVE' },
];

const emptyCriterion = {
  name: '',
  weight: 25,
  description: '',
  bandDescriptors: '',
  displayOrder: 1,
};

const emptyForm = {
  name: '',
  examType: 'IELTS',
  skill: 'WRITING',
  taskType: '',
  scoringScale: 'Estimated IELTS band 0-9',
  description: '',
  active: true,
  criteria: [
    { ...emptyCriterion, name: 'Task Achievement', displayOrder: 1 },
    { ...emptyCriterion, name: 'Coherence and Cohesion', displayOrder: 2 },
    { ...emptyCriterion, name: 'Lexical Resource', displayOrder: 3 },
    { ...emptyCriterion, name: 'Grammar Range and Accuracy', displayOrder: 4 },
  ],
};

export default function ContentManagerRubricsPage() {
  const [rubrics, setRubrics] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [skillFilter, setSkillFilter] = useState('ALL');
  const [activeFilter, setActiveFilter] = useState('ACTIVE');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const editorRef = useRef(null);

  const loadRubrics = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await courseApi.getContentManagerRubrics({
        includeInactive: activeFilter !== 'ACTIVE',
        skill: skillFilter === 'ALL' ? undefined : skillFilter,
      });
      setRubrics(data);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách rubrics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRubrics();
  }, [activeFilter, skillFilter]);

  const filteredRubrics = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return rubrics
      .filter((rubric) => {
        if (activeFilter === 'ACTIVE') return rubric.active !== false;
        if (activeFilter === 'INACTIVE') return rubric.active === false;
        return true;
      })
      .filter((rubric) => {
        if (!normalizedKeyword) return true;
        return [rubric.name, rubric.examType, rubric.skill, rubric.taskType, rubric.scoringScale, rubric.description]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(normalizedKeyword));
      });
  }, [activeFilter, keyword, rubrics]);

  const stats = useMemo(() => ({
    total: rubrics.length,
    active: rubrics.filter((rubric) => rubric.active !== false).length,
    inactive: rubrics.filter((rubric) => rubric.active === false).length,
    criteria: rubrics.reduce((sum, rubric) => sum + (rubric.criteria?.length || 0), 0),
  }), [rubrics]);

  const statItems = useMemo(() => [
    { label: 'Tổng rubric', value: stats.total, icon: SlidersHorizontal, tone: 'text-[#4b0009]' },
    { label: 'Đang dùng', value: stats.active, icon: CheckCircle2, tone: 'text-emerald-700' },
    { label: 'Tạm ngưng', value: stats.inactive, icon: Archive, tone: 'text-slate-700' },
    { label: 'Rule', value: stats.criteria, icon: Layers3, tone: 'text-[#005236]' },
  ], [stats]);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    filteredRubrics,
    8,
    `${keyword}|${skillFilter}|${activeFilter}`,
  );

  const totalWeight = useMemo(
    () => form.criteria.reduce((sum, criterion) => sum + Number(criterion.weight || 0), 0),
    [form.criteria],
  );

  const editRubric = (rubric) => {
    setEditingId(rubric.id);
    setEditorOpen(true);
    setForm({
      name: rubric.name || '',
      examType: rubric.examType || '',
      skill: rubric.skill || 'MIXED',
      taskType: rubric.taskType || '',
      scoringScale: rubric.scoringScale || '',
      description: rubric.description || '',
      active: rubric.active !== false,
      criteria: (rubric.criteria || []).length
        ? rubric.criteria.map((criterion, index) => ({
            id: criterion.id,
            name: criterion.name || '',
            weight: criterion.weight ?? 0,
            description: criterion.description || '',
            bandDescriptors: criterion.bandDescriptors || '',
            displayOrder: criterion.displayOrder ?? index + 1,
          }))
        : [{ ...emptyCriterion }],
    });
    setSuccess('');
    setError('');
    window.setTimeout(() => {
      editorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 0);
  };

  const resetForm = (open = false) => {
    setEditingId(null);
    setForm(emptyForm);
    setEditorOpen(open);
    setError('');
    setSuccess('');
    if (open) {
      window.setTimeout(() => {
        editorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 0);
    }
  };

  const updateCriterion = (index, patch) => {
    setForm((current) => ({
      ...current,
      criteria: current.criteria.map((criterion, criterionIndex) => (
        criterionIndex === index ? { ...criterion, ...patch } : criterion
      )),
    }));
  };

  const addCriterion = () => {
    setForm((current) => ({
      ...current,
      criteria: [
        ...current.criteria,
        { ...emptyCriterion, displayOrder: current.criteria.length + 1 },
      ],
    }));
  };

  const removeCriterion = (index) => {
    setForm((current) => ({
      ...current,
      criteria: current.criteria.length <= 1
        ? current.criteria
        : current.criteria
            .filter((_, criterionIndex) => criterionIndex !== index)
            .map((criterion, nextIndex) => ({ ...criterion, displayOrder: nextIndex + 1 })),
    }));
  };

  const saveRubric = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    if (!form.name.trim()) {
      setError('Vui lòng nhập tên rubric.');
      return;
    }
    if (!form.criteria.length || form.criteria.some((criterion) => !criterion.name.trim())) {
      setError('Mỗi rubric cần ít nhất một tiêu chí và tên tiêu chí không được để trống.');
      return;
    }
    if (totalWeight <= 0) {
      setError('Tổng trọng số tiêu chí phải lớn hơn 0.');
      return;
    }
    setWorking(true);
    try {
      const payload = {
        name: form.name.trim(),
        examType: form.examType,
        skill: form.skill,
        taskType: form.taskType,
        scoringScale: form.scoringScale,
        description: form.description,
        active: form.active,
        criteria: form.criteria.map((criterion, index) => ({
          id: criterion.id || null,
          name: criterion.name.trim(),
          weight: Number(criterion.weight || 0),
          description: criterion.description,
          bandDescriptors: criterion.bandDescriptors,
          displayOrder: Number(criterion.displayOrder || index + 1),
        })),
      };
      if (editingId) {
        await courseApi.updateContentManagerRubric(editingId, payload);
        setSuccess('Đã cập nhật rubric và các rule chấm điểm.');
      } else {
        await courseApi.createContentManagerRubric(payload);
        setSuccess('Đã tạo rubric mới.');
      }
      resetForm();
      await loadRubrics();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được rubric.');
    } finally {
      setWorking(false);
    }
  };

  const toggleRubricActive = async (rubric) => {
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      if (rubric.active === false) {
        await courseApi.reactivateContentManagerRubric(rubric.id);
        setSuccess('Đã kích hoạt lại rubric.');
      } else {
        await courseApi.deactivateContentManagerRubric(rubric.id);
        setSuccess('Đã tạm ngưng rubric. Các assessment đang dùng rubric này vẫn giữ dữ liệu tham chiếu hiện có.');
      }
      await loadRubrics();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không cập nhật được trạng thái rubric.');
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-6">
      {error ? <Notice tone="error">{error}</Notice> : null}
      {success ? <Notice tone="success">{success}</Notice> : null}

      {editorOpen && (
        <RubricEditorModal onClose={() => resetForm(false)}>
          <form className="space-y-5" onSubmit={saveRubric} ref={editorRef}>
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#730014]">
                  {editingId ? 'Edit rubric' : 'New rubric'}
                </p>
                <h3 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-slate-900">
                  {editingId ? 'Sửa rubric' : 'Tạo rubric mới'}
                </h3>
                <p className="mt-1 text-sm text-slate-500">
                  Tổng trọng số hiện tại: <strong className={totalWeight === 100 ? 'text-emerald-700' : 'text-amber-700'}>{totalWeight}%</strong>
                  {totalWeight !== 100 ? ' (không bắt buộc 100%, nhưng nên chuẩn hóa để dễ đọc)' : ''}
                </p>
              </div>
              <div className="flex flex-wrap gap-2">
                <button
                  className="inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf] px-4 py-3 text-sm font-extrabold text-slate-600 transition hover:bg-slate-50"
                  onClick={() => resetForm(true)}
                  type="button"
                >
                  <RotateCcw className="h-4 w-4" />
                  Reset
                </button>
                <button
                  className="inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf] px-4 py-3 text-sm font-extrabold text-[#4b0009] transition hover:bg-[#eff4ff]"
                  onClick={() => resetForm(false)}
                  type="button"
                >
                  <X className="h-4 w-4" />
                  Đóng
                </button>
              </div>
            </div>

            {error ? <div className="mb-4"><Notice tone="error">{error}</Notice></div> : null}

            <div className="grid gap-4 md:grid-cols-2">
              <TextField label="Tên rubric" onChange={(value) => setForm((current) => ({ ...current, name: value }))} value={form.name} />
              <TextField label="Loại kỳ thi" onChange={(value) => setForm((current) => ({ ...current, examType: value }))} value={form.examType} />
              <Picker
                label="Kỹ năng"
                onChange={(value) => setForm((current) => ({ ...current, skill: value }))}
                options={skillOptions.filter((option) => option.value !== 'ALL')}
                value={form.skill}
              />
              <TextField label="Loại task" onChange={(value) => setForm((current) => ({ ...current, taskType: value }))} value={form.taskType} />
              <TextField label="Thang điểm" onChange={(value) => setForm((current) => ({ ...current, scoringScale: value }))} value={form.scoringScale} />
              <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <input
                  checked={form.active}
                  className="h-4 w-4 accent-[#4b0009]"
                  onChange={(event) => setForm((current) => ({ ...current, active: event.target.checked }))}
                  type="checkbox"
                />
                <span className="text-sm font-bold text-slate-700">Cho phép sử dụng rubric này</span>
              </label>
            </div>
            <RichTextEditor
              label="Mô tả"
              onChange={(value) => setForm((current) => ({ ...current, description: value }))}
              placeholder="Mô tả rubric..."
              size="compact"
              value={form.description}
            />

            <div className="space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <h4 className="font-['Manrope'] text-lg font-extrabold text-slate-900">Tiêu chí / rule chấm điểm</h4>
                <button
                  className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm font-extrabold text-[#730014] transition hover:bg-[#fff4f5]"
                  onClick={addCriterion}
                  type="button"
                >
                  <Plus className="h-4 w-4" />
                  Thêm rule
                </button>
              </div>
              {form.criteria.map((criterion, index) => (
                <CriterionEditor
                  criterion={criterion}
                  index={index}
                  key={`${criterion.id || 'new'}-${index}`}
                  onChange={(patch) => updateCriterion(index, patch)}
                  onRemove={() => removeCriterion(index)}
                  removable={form.criteria.length > 1}
                />
              ))}
            </div>

            <button
              className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-60"
              disabled={working}
              type="submit"
            >
              <Save className="h-4 w-4" />
              {working ? 'Đang lưu...' : editingId ? 'Cập nhật rubric' : 'Tạo rubric'}
            </button>
          </form>
        </RubricEditorModal>
      )}

      <ManagerStatsGrid stats={statItems} />

      <ManagerFilterBar>
        <div className="min-w-[300px] flex-1">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-[#897270]" />
            <input
              className="w-full rounded-lg border border-[#dcc0bf]/50 bg-[#f8f9ff] py-2 pl-10 pr-4 text-sm text-[#0b1c30] outline-none transition focus:border-[#4b0009] focus:bg-white focus:ring-4 focus:ring-[#4b0009]/5"
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Tìm rubric, task, thang điểm..."
              value={keyword}
            />
          </div>
        </div>
        <div className="grid w-full gap-3 sm:grid-cols-2 lg:w-auto">
          <FilterSelect label="Kỹ năng" onChange={(event) => setSkillFilter(event.target.value)} options={skillOptions} value={skillFilter} />
          <FilterSelect label="Trạng thái" onChange={(event) => setActiveFilter(event.target.value)} options={activeOptions} value={activeFilter} />
        </div>
        <button
          aria-label="Làm mới rubrics"
          className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#dcc0bf]/40 text-[#564241] transition hover:bg-[#eff4ff]"
          onClick={loadRubrics}
          type="button"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
        <button className="inline-flex items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#730014]" onClick={() => resetForm(true)} type="button">
          <Plus className="h-4 w-4" />
          Tạo rubric mới
        </button>
      </ManagerFilterBar>

      {loading ? (
        <div className="rounded-xl border border-[#dcc0bf]/30 bg-white p-6 text-sm font-semibold text-slate-500">Đang tải rubrics...</div>
      ) : !filteredRubrics.length ? (
        <ManagerEmptyState>Chưa có rubric phù hợp.</ManagerEmptyState>
      ) : (
        <section className="overflow-hidden rounded-xl border border-[#dcc0bf]/30 bg-white shadow-sm">
          <ManagerTable
            columns={[
              { label: 'Tên rubric', key: 'name' },
              { label: 'Kỹ năng', key: 'skill' },
              { label: 'Task', key: 'task' },
              { label: 'Rule', key: 'rules', align: 'center' },
              { label: 'Trạng thái', key: 'status' },
              { label: 'Thao tác', key: 'actions', align: 'right' },
            ]}
            minWidth="1040px"
          >
            {pageItems.map((rubric) => (
              <tr className="transition hover:bg-[#eff4ff]" key={rubric.id}>
                <td className="px-6 py-5">
                  <p className="max-w-[340px] overflow-hidden text-sm font-bold leading-5 text-[#4b0009] [display:-webkit-box] [-webkit-box-orient:vertical] [-webkit-line-clamp:2]">{rubric.name}</p>
                  {rubric.scoringScale ? <p className="mt-1 max-w-[340px] truncate text-xs text-[#564241]">{rubric.scoringScale}</p> : null}
                </td>
                <td className="px-6 py-5"><ManagerStatusBadge tone="info">{rubric.skill || '-'}</ManagerStatusBadge></td>
                <td className="px-6 py-5 text-sm text-[#0b1c30]">{rubric.taskType || '-'}</td>
                <td className="px-6 py-5 text-center text-sm font-semibold text-[#0b1c30]">{rubric.criteria?.length || 0}</td>
                <td className="px-6 py-5"><ManagerStatusBadge tone={rubric.active === false ? 'neutral' : 'success'}>{rubric.active === false ? 'Tạm ngưng' : 'Đang dùng'}</ManagerStatusBadge></td>
                <td className="px-6 py-5 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <button className="inline-flex items-center gap-1.5 rounded-lg border border-[#dcc0bf]/50 px-3 py-1.5 text-xs font-bold text-[#4b0009] transition hover:bg-[#fff7f7]" onClick={() => editRubric(rubric)} type="button">
                      <Edit3 className="h-3.5 w-3.5" />
                      Chỉnh sửa
                    </button>
                    <button
                      className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-[#4b0009] px-4 py-1.5 text-xs font-bold text-white transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-45"
                      disabled={working}
                      onClick={() => toggleRubricActive(rubric)}
                      type="button"
                    >
                      {rubric.active === false ? <RefreshCw className="h-3.5 w-3.5" /> : <Archive className="h-3.5 w-3.5" />}
                      {rubric.active === false ? 'Khôi phục' : 'Tạm ngưng'}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </ManagerTable>
          <ManagerTablePagination itemLabel="rubric" onChange={setPage} page={page} pageSize={8} totalItems={totalItems} totalPages={totalPages} />
        </section>
      )}
    </div>
  );
}

function HeroStat({ label, value }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/10 p-4 text-center">
      <p className="font-['Manrope'] text-3xl font-black">{value}</p>
      <p className="mt-1 text-xs font-bold uppercase tracking-[0.14em] text-white/60">{label}</p>
    </div>
  );
}

function FilterSelect({ label, value, onChange, options }) {
  const normalizedOptions = options.map((option) => ({
    ...option,
    label: `${label}: ${option.label}`,
  }));

  return (
    <BrandedSelect
      buttonClassName="h-10 min-w-[170px] rounded-lg border-[#dcc0bf]/50 bg-[#f8f9ff] py-2 text-sm shadow-none"
      onChange={onChange}
      options={normalizedOptions}
      value={value}
    />
  );
}

function CriterionEditor({ criterion, index, onChange, onRemove, removable }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-extrabold text-[#730014]">Rule {index + 1}</p>
        <button
          className="inline-flex items-center gap-1 rounded-xl border border-rose-200 bg-white px-3 py-2 text-xs font-extrabold text-rose-700 disabled:cursor-not-allowed disabled:opacity-40"
          disabled={!removable}
          onClick={onRemove}
          type="button"
        >
          <Trash2 className="h-3.5 w-3.5" />
          Xóa
        </button>
      </div>
      <div className="mt-3 grid gap-3 md:grid-cols-[1fr_110px_110px]">
        <TextField label="Tên rule" onChange={(value) => onChange({ name: value })} value={criterion.name} />
        <TextField label="Weight" onChange={(value) => onChange({ weight: value })} type="number" value={criterion.weight} />
        <TextField label="Thứ tự" onChange={(value) => onChange({ displayOrder: value })} type="number" value={criterion.displayOrder} />
      </div>
      <div className="mt-3 space-y-4">
        <RichTextEditor
          label="Mô tả rule"
          onChange={(value) => onChange({ description: value })}
          placeholder="Mô tả tiêu chí..."
          size="compact"
          value={criterion.description}
        />
        <RichTextEditor
          label="Band descriptors / rule chi tiết"
          onChange={(value) => onChange({ bandDescriptors: value })}
          placeholder="Mô tả band / rule chi tiết..."
          size="form"
          value={criterion.bandDescriptors}
        />
      </div>
    </div>
  );
}

function RubricCard({ onEdit, onToggleActive, rubric, working }) {
  const totalWeight = (rubric.criteria || []).reduce((sum, criterion) => sum + Number(criterion.weight || 0), 0);
  return (
    <article className="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm transition hover:border-[#dfbfbd]">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap gap-2">
            <Badge>{rubric.skill}</Badge>
            {rubric.examType ? <Badge>{rubric.examType}</Badge> : null}
            {rubric.active === false ? <Badge tone="muted">Tạm ngưng</Badge> : <Badge tone="success">Đang dùng</Badge>}
          </div>
          <h3 className="mt-3 font-['Manrope'] text-xl font-extrabold text-slate-900">{rubric.name}</h3>
          <p className="mt-1 text-sm font-semibold text-slate-500">
            {rubric.taskType || 'Chưa gắn task'} · {rubric.scoringScale || 'Chưa gắn thang điểm'} · Tổng weight {totalWeight}%
          </p>
          {rubric.description ? <p className="mt-3 text-sm leading-7 text-slate-600">{stripRichTextToPlain(rubric.description)}</p> : null}
        </div>
        <div className="flex flex-wrap justify-end gap-2">
          <button
            className="rounded-2xl border border-slate-200 px-4 py-3 text-sm font-extrabold text-[#730014] transition hover:bg-[#fff4f5]"
            onClick={onEdit}
            type="button"
          >
            Sửa
          </button>
          <button
            className={`inline-flex items-center gap-2 rounded-2xl border px-4 py-3 text-sm font-extrabold transition disabled:cursor-not-allowed disabled:opacity-60 ${
              rubric.active === false
                ? 'border-emerald-200 text-emerald-700 hover:bg-emerald-50'
                : 'border-rose-200 text-rose-700 hover:bg-rose-50'
            }`}
            disabled={working}
            onClick={onToggleActive}
            type="button"
          >
            {rubric.active === false ? <CheckCircle2 className="h-4 w-4" /> : <XCircle className="h-4 w-4" />}
            {rubric.active === false ? 'Kích hoạt' : 'Tạm ngưng'}
          </button>
        </div>
      </div>
      <div className="mt-5 grid gap-3 md:grid-cols-2">
        {(rubric.criteria || []).map((criterion) => (
          <div className="rounded-2xl border border-slate-100 bg-slate-50 p-4" key={criterion.id || `${rubric.id}-${criterion.displayOrder}-${criterion.name}`}>
            <div className="flex items-start justify-between gap-3">
              <p className="font-bold text-slate-900">{criterion.name}</p>
              <span className="rounded-full bg-white px-2.5 py-1 text-xs font-extrabold text-[#730014]">{criterion.weight ?? 0}%</span>
            </div>
            {criterion.description ? <p className="mt-2 text-sm leading-6 text-slate-600">{stripRichTextToPlain(criterion.description)}</p> : null}
            {criterion.bandDescriptors ? <p className="mt-2 text-xs leading-5 text-slate-500">{criterion.bandDescriptors}</p> : null}
          </div>
        ))}
      </div>
    </article>
  );
}

function Picker({ label, onChange, options, value }) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{label}</span>
      <BrandedSelect onChange={(event) => onChange(event.target.value)} options={options} value={value} />
    </label>
  );
}

function TextField({ label, onChange, textarea = false, type = 'text', value }) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{label}</span>
      {textarea ? (
        <textarea
          className="min-h-24 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-[#730014]"
          onChange={(event) => onChange(event.target.value)}
          value={value}
        />
      ) : (
        <input
          className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-[#730014]"
          onChange={(event) => onChange(event.target.value)}
          type={type}
          value={value}
        />
      )}
    </label>
  );
}

function Badge({ children, tone }) {
  const className = tone === 'muted'
    ? 'bg-slate-100 text-slate-500'
    : tone === 'success'
      ? 'bg-emerald-100 text-emerald-700'
      : 'bg-[#fff1f3] text-[#730014]';
  return <span className={`rounded-full px-3 py-1 text-xs font-extrabold ${className}`}>{children}</span>;
}

function Notice({ children, tone }) {
  const className = tone === 'error'
    ? 'border-rose-200 bg-rose-50 text-rose-700'
    : 'border-emerald-200 bg-emerald-50 text-emerald-700';
  return <div className={`rounded-2xl border px-5 py-4 text-sm font-bold ${className}`}>{children}</div>;
}

function RubricEditorModal({ children, onClose }) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-hidden p-4 sm:p-6 backdrop-blur-sm bg-black/45 animate-fade-in" role="dialog" aria-modal="true">
      <button
        aria-label="Đóng modal"
        className="absolute inset-0 cursor-default"
        onClick={onClose}
        type="button"
      />
      <div className="relative z-10 flex max-h-[calc(100dvh-2.5rem)] w-full max-w-[800px] min-h-0 flex-col overflow-hidden rounded-3xl border border-[#dcc0bf]/35 bg-[#fafafa] shadow-2xl pointer-events-auto">
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-6">
          {children}
        </div>
      </div>
    </div>
  );
}
