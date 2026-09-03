import { useDeferredValue, useEffect, useMemo, useRef, useState } from 'react';
import { Archive, CheckCircle2, Dumbbell, Edit3, Layers3, Plus, RefreshCw, Save, Search, X } from 'lucide-react';
import courseApi from '../../api/courseApi';
import {
  ManagerEmptyState,
  ManagerFilterBar,
  ManagerStatsGrid,
  ManagerStatusBadge,
  ManagerTable,
  ManagerTablePagination,
} from '../../components/content-manager/ManagerListUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import AssessmentExamBuilder from '../../components/content-manager/AssessmentExamBuilder';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import { usePagination } from '../../components/ui/Pagination';
import { useAppDialog } from '../../components/ui/AppDialog';
import { EMPTY_PAGE, pageParams } from '../../utils/pagination';
import {
  ERROR_NOTICE_CLASS,
  FIELD_CLASS,
  PANEL_CLASS,
  PRIMARY_BUTTON_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
  TEXTAREA_CLASS,
} from '../../utils/formStyles';

const skillOptions = [
  { label: 'Tất cả', value: 'ALL' },
  { label: 'Listening', value: 'LISTENING' },
  { label: 'Reading', value: 'READING' },
  { label: 'Writing', value: 'WRITING' },
  { label: 'Speaking', value: 'SPEAKING' },
  { label: 'Grammar', value: 'GRAMMAR' },
  { label: 'Vocabulary', value: 'VOCABULARY' },
];

const typeOptions = [
  { label: 'Bài tập về nhà', value: 'HOMEWORK' },
  { label: 'Quiz', value: 'QUIZ' },
  { label: 'Luyện tập', value: 'PRACTICE' },
];

const statusOptions = [
  { label: 'Tất cả', value: 'ALL' },
  { label: 'Đang dùng', value: 'PUBLISHED' },
  { label: 'Đã tạm ngưng', value: 'ARCHIVED' },
];

const emptyForm = {
  title: '',
  skill: 'WRITING',
  level: '',
  exerciseType: 'HOMEWORK',
  prompt: '',
  answerKey: '',
  explanation: '',
  tags: '',
  status: 'PUBLISHED',
};

export default function ContentManagerExerciseBankPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [composerOpen, setComposerOpen] = useState(false);
  const [skillFilter, setSkillFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [pageResult, setPageResult] = useState(EMPTY_PAGE);
  const [statsData, setStatsData] = useState({ total: 0, published: 0, archived: 0, skills: 0 });
  const editorRef = useRef(null);
  const deferredKeyword = useDeferredValue(keyword);
  const resetKey = `${deferredKeyword}|${skillFilter}|${typeFilter}|${statusFilter}`;
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    pageResult.content,
    8,
    resetKey,
    pageResult,
  );

  const loadItems = async () => {
    setLoading(true);
    setError('');
    try {
      const params = {
        skill: skillFilter === 'ALL' ? undefined : skillFilter,
        exerciseType: typeFilter === 'ALL' ? undefined : typeFilter,
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        keyword: deferredKeyword.trim() || undefined,
      };
      const [data, summary] = await Promise.all([
        courseApi.getExerciseBankItemsPage(pageParams(page, 8, params)),
        courseApi.getExerciseBankStats({ skill: params.skill }),
      ]);
      setPageResult(data);
      setItems(data.content);
      setStatsData(summary);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được ngân hàng bài tập.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadItems(); }, [deferredKeyword, page, skillFilter, statusFilter, typeFilter]);

  const stats = useMemo(() => [
    { label: 'Tổng bài tập', value: statsData.total, icon: Dumbbell, tone: 'text-[#4b0009]' },
    { label: 'Đang dùng', value: statsData.published, icon: CheckCircle2, tone: 'text-emerald-700' },
    { label: 'Tạm ngưng', value: statsData.archived, icon: Archive, tone: 'text-slate-700' },
    { label: 'Kỹ năng', value: statsData.skills, icon: Layers3, tone: 'text-[#005236]' },
  ], [statsData]);

  const resetForm = (open = true) => {
    setEditingId(null);
    setForm(emptyForm);
    setComposerOpen(open);
    if (open) {
      window.setTimeout(() => {
        editorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 0);
    }
  };

  const openEdit = (item) => {
    setEditingId(item.id);
    setComposerOpen(true);
    setForm({
      title: item.title || '',
      skill: item.skill || 'WRITING',
      level: item.level || '',
      exerciseType: item.exerciseType || 'HOMEWORK',
      prompt: item.prompt || '',
      answerKey: item.answerKey || '',
      explanation: item.explanation || '',
      tags: item.tags || '',
      status: item.status || 'DRAFT',
    });
    window.setTimeout(() => {
      editorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 0);
  };

  const saveItem = async () => {
    if (!form.title.trim() || !form.prompt.trim()) {
      setError('Vui lòng nhập tiêu đề và đề bài.');
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      let saved;
      if (editingId) {
        saved = await courseApi.updateExerciseBankItem(editingId, form);
      } else {
        saved = await courseApi.createExerciseBankItem(form);
      }
      setItems((current) => {
        if (!saved?.id) return current;
        const exists = current.some((item) => item.id === saved.id);
        return exists
          ? current.map((item) => (item.id === saved.id ? saved : item))
          : [saved, ...current];
      });
      await loadItems();
      resetForm(false);
      setSuccess('Đã lưu bài tập.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được bài tập.');
    } finally {
      setWorking(false);
    }
  };

  const deactivateItem = async (id) => {
    if (!await confirmDialog('Bài tập sẽ không còn khả dụng cho nội dung mới.', {
      title: 'Tạm ngưng bài tập',
      confirmLabel: 'Tạm ngưng',
      tone: 'danger',
    })) return;
    await courseApi.deleteExerciseBankItem(id);
    await loadItems();
  };

  const updateSystemPractice = (field, value) => {
    if (field === 'uiConfigJson') {
      setForm((current) => ({ ...current, prompt: value }));
    } else if (field === 'objectiveAnswerKey') {
      setForm((current) => ({ ...current, answerKey: value }));
    }
  };

  const restoreItem = async (item) => {
    if (!await confirmDialog(`Khôi phục bài tập “${item.title}”?`, {
      title: 'Khôi phục bài tập',
      confirmLabel: 'Khôi phục',
    })) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await courseApi.updateExerciseBankItem(item.id, { ...item, status: 'PUBLISHED' });
      await loadItems();
      setSuccess('Đã khôi phục bài tập.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không khôi phục được bài tập.');
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-6">
      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      {composerOpen ? (
        <section className={`${PANEL_CLASS} scroll-mt-24 space-y-5`} ref={editorRef}>
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h3 className="font-['Manrope'] text-xl font-extrabold text-slate-900">
                {editingId ? 'Chỉnh sửa bài tập' : 'Thêm bài tập mới'}
              </h3>
              <p className="mt-1 text-sm text-slate-600">Nội dung ở đây dùng chung cho giáo trình, lớp học và hoạt động luyện tập.</p>
            </div>
            <button type="button" onClick={() => resetForm(false)} className={SECONDARY_BUTTON_CLASS}>
              <X className="h-4 w-4" /> Đóng
            </button>
          </div>

          <div className="space-y-4">
            <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="Tiêu đề" className={FIELD_CLASS} />
            <div className="grid gap-4 md:grid-cols-2">
              <BrandedSelect label="Kỹ năng" value={form.skill} onChange={(event) => setForm({ ...form, skill: event.target.value })} options={skillOptions.filter((o) => o.value !== 'ALL')} />
              <BrandedSelect label="Loại bài" value={form.exerciseType} onChange={(event) => setForm({ ...form, exerciseType: event.target.value })} options={typeOptions} />
            </div>
            <input value={form.level} onChange={(e) => setForm({ ...form, level: e.target.value })} placeholder="Cấp độ (vd: IELTS 6.0)" className={FIELD_CLASS} />
            {form.exerciseType === 'PRACTICE' && ['LISTENING', 'READING'].includes(form.skill) ? (
              <div className="rounded-2xl border border-[#ead9db] bg-[#fffdfd] p-5">
                <h4 className="font-['Manrope'] text-lg font-extrabold text-[#1a1c1c]">Biên soạn bài luyện tập trên hệ thống</h4>
                <p className="mt-2 text-sm leading-6 text-[#584140]">Dùng cùng trình biên soạn với Module Test để tạo phần thi, câu hỏi, lựa chọn và đáp án chấm tự động.</p>
                <div className="mt-5">
                  <AssessmentExamBuilder
                    assessment={{
                      title: form.title || 'Bài luyện tập',
                      skill: form.skill,
                      uiConfigJson: form.prompt,
                      objectiveAnswerKey: form.answerKey,
                      timeLimitMinutes: 10,
                    }}
                    onChange={updateSystemPractice}
                  />
                </div>
              </div>
            ) : (
              <>
                <RichTextEditor
                  helperText=""
                  label="Đề bài"
                  onChange={(value) => setForm({ ...form, prompt: value })}
                  placeholder="Đề bài"
                  size="form"
                  value={form.prompt}
                />
                <textarea value={form.answerKey} onChange={(e) => setForm({ ...form, answerKey: e.target.value })} placeholder="Đáp án / rubric" rows={3} className={TEXTAREA_CLASS} />
              </>
            )}
            <RichTextEditor
              helperText=""
              label="Giải thích"
              onChange={(value) => setForm({ ...form, explanation: value })}
              placeholder="Giải thích"
              size="compact"
              value={form.explanation}
            />
            <input value={form.tags} onChange={(e) => setForm({ ...form, tags: e.target.value })} placeholder="Thẻ (cách nhau bởi dấu phẩy)" className={FIELD_CLASS} />
            <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4">
              <button type="button" onClick={saveItem} disabled={working} className={PRIMARY_BUTTON_CLASS}>
                <Save className="h-4 w-4" /> Lưu
              </button>
              <button type="button" onClick={() => resetForm(true)} className={SECONDARY_BUTTON_CLASS}>
                <Plus className="h-4 w-4" /> Mới
              </button>
            </div>
          </div>
        </section>
      ) : (
        <>
          <ManagerStatsGrid stats={stats} />

          <ManagerFilterBar>
            <div className="w-full min-w-0 flex-1 sm:min-w-[300px]">
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-[#897270]" />
                <input
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                  placeholder="Tìm bài tập, kỹ năng hoặc tag..."
                  className="w-full rounded-lg border border-[#dcc0bf]/50 bg-[#f8f9ff] py-2 pl-10 pr-4 text-sm text-[#0b1c30] outline-none transition focus:border-[#4b0009] focus:bg-white focus:ring-4 focus:ring-[#4b0009]/5"
                />
              </div>
            </div>
            <div className="grid w-full gap-3 sm:grid-cols-3 lg:w-auto">
              <FilterSelect label="Kỹ năng" onChange={(event) => setSkillFilter(event.target.value)} options={skillOptions} value={skillFilter} />
              <FilterSelect label="Loại bài" onChange={(event) => setTypeFilter(event.target.value)} options={[{ label: 'Tất cả', value: 'ALL' }, ...typeOptions]} value={typeFilter} />
              <FilterSelect label="Trạng thái" onChange={(event) => setStatusFilter(event.target.value)} options={statusOptions} value={statusFilter} />
            </div>
            <button
              aria-label="Làm mới ngân hàng bài tập"
              className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#dcc0bf]/40 text-[#564241] transition hover:bg-[#eff4ff]"
              onClick={loadItems}
              type="button"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
            <button type="button" onClick={() => resetForm(true)} className="inline-flex items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#730014]">
              <Plus className="h-4 w-4" />
              Thêm bài mới
            </button>
          </ManagerFilterBar>

          {loading ? (
            <div className="rounded-xl border border-[#dcc0bf]/30 bg-white p-6 text-sm font-semibold text-slate-500">Đang tải ngân hàng bài tập...</div>
          ) : totalItems === 0 ? (
            <ManagerEmptyState>Chưa có bài tập phù hợp.</ManagerEmptyState>
          ) : (
            <section className="overflow-hidden rounded-xl border border-[#dcc0bf]/30 bg-white shadow-sm">
              <ManagerTable
                columns={[
                  { label: 'Tên bài tập', key: 'title' },
                  { label: 'Kỹ năng', key: 'skill' },
                  { label: 'Loại bài', key: 'type' },
                  { label: 'Cấp độ', key: 'level' },
                  { label: 'Trạng thái', key: 'status' },
                  { label: 'Thao tác', key: 'actions', align: 'right' },
                ]}
                minWidth="1040px"
              >
                {pageItems.map((item) => (
                  <tr className="transition hover:bg-[#eff4ff]" key={item.id}>
                    <td className="px-6 py-5">
                      <p className="max-w-[340px] overflow-hidden text-sm font-bold leading-5 text-[#4b0009] [display:-webkit-box] [-webkit-box-orient:vertical] [-webkit-line-clamp:2]">{item.title}</p>
                      {item.prompt ? <p className="mt-1 max-w-[340px] truncate text-xs text-[#564241]">{summarizeExerciseContent(item.prompt)}</p> : null}
                    </td>
                    <td className="px-6 py-5"><ManagerStatusBadge tone="info">{formatSkill(item.skill)}</ManagerStatusBadge></td>
                    <td className="px-6 py-5 text-sm text-[#0b1c30]">{formatExerciseType(item.exerciseType)}</td>
                    <td className="px-6 py-5 text-sm text-[#564241]">{item.level || '-'}</td>
                    <td className="px-6 py-5"><ManagerStatusBadge tone={item.status === 'PUBLISHED' ? 'success' : 'neutral'}>{item.status === 'PUBLISHED' ? 'Đang dùng' : 'Đã tạm ngưng'}</ManagerStatusBadge></td>
                    <td className="px-6 py-5 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          className="inline-flex items-center gap-1.5 rounded-lg border border-[#dcc0bf]/50 px-3 py-1.5 text-xs font-bold text-[#4b0009] transition hover:bg-[#fff7f7]"
                          onClick={() => openEdit(item)}
                          type="button"
                        >
                          <Edit3 className="h-3.5 w-3.5" />
                          Chỉnh sửa
                        </button>
                        {item.status !== 'PUBLISHED' ? (
                          <button className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-[#4b0009] px-4 py-1.5 text-xs font-bold text-white transition hover:bg-[#730014]" onClick={() => restoreItem(item)} type="button">
                            <RefreshCw className="h-3.5 w-3.5" />
                            Khôi phục
                          </button>
                        ) : (
                          <button className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-[#4b0009] px-4 py-1.5 text-xs font-bold text-white transition hover:bg-[#730014]" onClick={() => deactivateItem(item.id)} type="button">
                            <Archive className="h-3.5 w-3.5" />
                            Tạm ngưng
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </ManagerTable>
              <ManagerTablePagination itemLabel="bài tập" onChange={setPage} page={page} pageSize={8} totalItems={totalItems} totalPages={totalPages} />
            </section>
          )}
        </>
      )}
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
      buttonClassName="h-10 min-w-[150px] rounded-lg border-[#dcc0bf]/50 bg-[#f8f9ff] py-2 text-sm shadow-none"
      onChange={onChange}
      options={normalizedOptions}
      value={value}
    />
  );
}

function formatSkill(value) {
  const labels = {
    LISTENING: 'Nghe',
    READING: 'Đọc',
    WRITING: 'Viết',
    SPEAKING: 'Nói',
    GRAMMAR: 'Ngữ pháp',
    VOCABULARY: 'Từ vựng',
  };
  return labels[String(value || '').toUpperCase()] || value || '-';
}

function formatExerciseType(value) {
  const labels = {
    HOMEWORK: 'Bài tập về nhà',
    QUIZ: 'Quiz',
    PRACTICE: 'Luyện tập',
  };
  return labels[String(value || '').toUpperCase()] || value || '-';
}

function summarizeExerciseContent(value) {
  const text = String(value || '').trim();
  if (!text.startsWith('{') && !text.startsWith('[')) return text;

  try {
    const config = JSON.parse(text);
    const parts = Array.isArray(config.parts) ? config.parts : [];
    const directQuestions = Array.isArray(config.questions) ? config.questions.length : 0;
    const nestedQuestions = parts.reduce((total, part) => {
      const partQuestions = Array.isArray(part.questions) ? part.questions.length : 0;
      const groupedQuestions = (part.questionGroups || []).reduce(
        (sum, group) => sum + (group.questions?.length || group.questionNumbers?.length || 0),
        0,
      );
      return total + partQuestions + groupedQuestions;
    }, 0);
    const details = [
      parts.length ? `${parts.length} phần` : null,
      directQuestions + nestedQuestions ? `${directQuestions + nestedQuestions} câu` : null,
      Number(config.durationMinutes || config.timeLimitMinutes)
        ? `${Number(config.durationMinutes || config.timeLimitMinutes)} phút`
        : null,
    ].filter(Boolean);
    return details.length ? details.join(' · ') : 'Nội dung đã được biên soạn trên hệ thống';
  } catch {
    return 'Nội dung đã được biên soạn trên hệ thống';
  }
}
