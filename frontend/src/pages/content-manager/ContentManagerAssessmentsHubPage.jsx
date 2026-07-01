import { useEffect, useMemo, useState } from 'react';
import { Archive, Edit3, Plus, RefreshCw, Save, Search, X } from 'lucide-react';
import curriculumApi from '../../api/curriculumApi';
import AssessmentExamBuilder from '../../components/content-manager/AssessmentExamBuilder';
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

const pageMap = {
  listening: {
    title: 'Ngân hàng luyện nghe',
    skill: 'LISTENING',
    type: 'LESSON_PRACTICE',
    matcher: (item) => String(item.skill || '').toUpperCase() === 'LISTENING',
  },
  reading: {
    title: 'Ngân hàng luyện đọc',
    skill: 'READING',
    type: 'LESSON_PRACTICE',
    matcher: (item) => String(item.skill || '').toUpperCase() === 'READING',
  },
  writing: {
    title: 'Ngân hàng luyện viết',
    skill: 'WRITING',
    type: 'WRITING_TASK',
    matcher: (item) => String(item.skill || '').toUpperCase() === 'WRITING' || String(item.type || '').toUpperCase() === 'WRITING_TASK',
  },
  speaking: {
    title: 'Ngân hàng luyện nói',
    skill: 'SPEAKING',
    type: 'SPEAKING_TASK',
    matcher: (item) => String(item.skill || '').toUpperCase() === 'SPEAKING' || String(item.type || '').toUpperCase() === 'SPEAKING_TASK',
  },
  mockExams: {
    title: 'Ngân hàng đề thi thử',
    skill: 'MIXED',
    type: 'MOCK_TEST',
    matcher: (item) => String(item.type || '').toUpperCase() === 'MOCK_TEST',
  },
};

const typeOptions = [
  { label: 'Bài luyện trong bài học', value: 'LESSON_PRACTICE' },
  { label: 'Bài kiểm tra mô-đun', value: 'MODULE_TEST' },
  { label: 'Đề thi thử', value: 'MOCK_TEST' },
  { label: 'Bài luyện viết', value: 'WRITING_TASK' },
  { label: 'Bài luyện nói', value: 'SPEAKING_TASK' },
  { label: 'Quiz', value: 'QUIZ' },
];

const skillOptions = [
  { label: 'Nghe', value: 'LISTENING' },
  { label: 'Đọc', value: 'READING' },
  { label: 'Viết', value: 'WRITING' },
  { label: 'Nói', value: 'SPEAKING' },
  { label: 'Từ vựng', value: 'VOCABULARY' },
  { label: 'Ngữ pháp', value: 'GRAMMAR' },
  { label: 'Tổng hợp', value: 'MIXED' },
];

const statusOptions = [
  { label: 'Nháp', value: 'DRAFT' },
  { label: 'Đã xuất bản', value: 'PUBLISHED' },
  { label: 'Lưu trữ', value: 'ARCHIVED' },
];

const aiOptions = [
  { label: 'Không dùng AI', value: 'NONE' },
  { label: 'Giải thích đáp án', value: 'EXPLAIN_ONLY' },
  { label: 'Phản hồi theo tiêu chí', value: 'RUBRIC_FEEDBACK' },
  { label: 'Ước lượng band', value: 'ESTIMATED_BAND' },
];

const emptyForm = (pageConfig) => ({
  title: '',
  description: '',
  type: pageConfig?.type || 'LESSON_PRACTICE',
  skill: pageConfig?.skill || 'LISTENING',
  aiEvaluationMode: 'NONE',
  instructions: '',
  objectiveAnswerKey: '',
  uiConfigJson: '',
  passingScore: '',
  maxScore: 100,
  timeLimitMinutes: '',
  status: 'DRAFT',
  displayOrder: 0,
});

const toForm = (item = {}, pageConfig) => ({
  title: item.title || '',
  description: item.description || '',
  type: item.type || pageConfig?.type || 'LESSON_PRACTICE',
  skill: item.skill || pageConfig?.skill || 'LISTENING',
  aiEvaluationMode: item.aiEvaluationMode || 'NONE',
  instructions: item.instructions || '',
  objectiveAnswerKey: item.objectiveAnswerKey || '',
  uiConfigJson: item.uiConfigJson || '',
  passingScore: item.passingScore ?? '',
  maxScore: item.maxScore ?? 100,
  timeLimitMinutes: item.timeLimitMinutes ?? '',
  status: item.status || 'DRAFT',
  displayOrder: item.displayOrder ?? 0,
});

const supportedBuilderSkills = new Set(['LISTENING', 'READING', 'WRITING', 'SPEAKING']);

export default function ContentManagerAssessmentsHubPage({ pageKey }) {
  const pageConfig = pageMap[pageKey] || pageMap.listening;
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(() => emptyForm(pageConfig));
  const [editingId, setEditingId] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadItems = async () => {
    setLoading(true);
    setError('');
    try {
      setItems(await curriculumApi.getAssessmentBank());
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được ngân hàng đề.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadItems();
    setEditingId(null);
    setEditorOpen(false);
    setForm(emptyForm(pageConfig));
  }, [pageKey]);

  const filteredItems = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    return items
      .filter((item) => pageConfig.matcher(item))
      .filter((item) => {
        if (!normalized) return true;
        return [item.title, item.description, item.type, item.skill, item.status]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(normalized));
      });
  }, [items, keyword, pageConfig]);

  const sortedItems = useMemo(
    () => [...filteredItems].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || String(a.title).localeCompare(String(b.title))),
    [filteredItems],
  );

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(sortedItems, 8, `${pageKey}-${keyword}`);

  const updateForm = (field, value) => setForm((current) => ({ ...current, [field]: value }));

  const startNew = () => {
    setEditingId(null);
    setForm(emptyForm(pageConfig));
    setEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const closeEditor = () => {
    setEditingId(null);
    setForm(emptyForm(pageConfig));
    setEditorOpen(false);
    setError('');
    setSuccess('');
  };

  const openEdit = (item) => {
    setEditingId(item.id);
    setForm(toForm(item, pageConfig));
    setEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const saveItem = async () => {
    if (!form.title.trim()) {
      setError('Vui lòng nhập tên đề.');
      return;
    }
    if (form.uiConfigJson) {
      try {
        JSON.parse(form.uiConfigJson);
      } catch {
        setError('Cấu hình nội dung đề phải là JSON hợp lệ.');
        return;
      }
    }
    if (form.objectiveAnswerKey) {
      try {
        JSON.parse(form.objectiveAnswerKey);
      } catch {
        setError('Đáp án khách quan phải là JSON hợp lệ.');
        return;
      }
    }
    setWorking(true);
    setError('');
    setSuccess('');
    const payload = {
      ...form,
      passingScore: form.passingScore === '' ? null : Number(form.passingScore),
      maxScore: form.maxScore === '' ? null : Number(form.maxScore),
      timeLimitMinutes: form.timeLimitMinutes === '' ? null : Number(form.timeLimitMinutes),
      displayOrder: Number(form.displayOrder || 0),
    };
    try {
      const saved = editingId
        ? await curriculumApi.updateAssessmentBankItem(editingId, payload)
        : await curriculumApi.createAssessmentBankItem(payload);
      setItems((current) => {
        if (editingId) {
          return current.map((item) => (String(item.id) === String(saved.id) ? saved : item));
        }
        return [saved, ...current];
      });
      setEditingId(saved.id);
      setForm(toForm(saved, pageConfig));
      setEditorOpen(true);
      setSuccess(editingId ? 'Đã cập nhật đề.' : 'Đã tạo đề mới trong ngân hàng.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được đề.');
    } finally {
      setWorking(false);
    }
  };

  const archiveItem = async (item) => {
    if (!window.confirm(`Lưu trữ đề "${item.title}"?`)) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await curriculumApi.archiveAssessmentBankItem(item.id);
      setItems((current) => current.map((row) => (
        String(row.id) === String(item.id) ? { ...row, status: 'ARCHIVED' } : row
      )));
      if (String(editingId) === String(item.id)) updateForm('status', 'ARCHIVED');
      setSuccess('Đã lưu trữ đề.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu trữ được đề.');
    } finally {
      setWorking(false);
    }
  };

  const canUseBuilder = supportedBuilderSkills.has(String(form.skill || '').toUpperCase());

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="font-['Manrope'] text-2xl font-extrabold text-slate-900">{pageConfig.title}</h2>
          <p className="mt-1 max-w-3xl text-sm text-slate-600">
            Quản lý các bài luyện dùng chung để gắn vào khóa học, giáo trình hoặc bài học khi cần.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={startNew} className={PRIMARY_BUTTON_CLASS}>
            <Plus className="h-4 w-4" /> Thêm bài mới
          </button>
          <button type="button" onClick={loadItems} className={SECONDARY_BUTTON_CLASS}>
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /> Làm mới
          </button>
        </div>
      </div>

      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      {editorOpen ? (
        <section className={`${PANEL_CLASS} space-y-5`}>
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">
                {editingId ? 'Chỉnh sửa đề luyện tập' : 'Tạo đề mới'}
              </h3>
              <p className="mt-1 text-sm text-slate-600">
                Nội dung tạo ở đây sẽ nằm trong ngân hàng dùng chung, sau đó có thể gắn vào nhiều khóa học hoặc giáo trình.
              </p>
            </div>
            <button type="button" onClick={closeEditor} className={SECONDARY_BUTTON_CLASS}>
              <X className="h-4 w-4" /> Đóng
            </button>
          </div>

          <div className="grid gap-6 xl:grid-cols-[minmax(0,440px)_1fr]">
            <div className="space-y-4">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Tên đề</span>
                <input value={form.title} onChange={(event) => updateForm('title', event.target.value)} className={FIELD_CLASS} />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Mô tả</span>
                <textarea value={form.description} onChange={(event) => updateForm('description', event.target.value)} rows={3} className={TEXTAREA_CLASS} />
              </label>
              <div className="grid gap-3 md:grid-cols-2">
                <div>
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Kỹ năng</span>
                  <BrandedSelect value={form.skill} onChange={(event) => updateForm('skill', event.target.value)} options={skillOptions} />
                </div>
                <div>
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Loại đề</span>
                  <BrandedSelect value={form.type} onChange={(event) => updateForm('type', event.target.value)} options={typeOptions} />
                </div>
              </div>
              <div className="grid gap-3 md:grid-cols-2">
                <div>
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Chấm tự động</span>
                  <BrandedSelect value={form.aiEvaluationMode} onChange={(event) => updateForm('aiEvaluationMode', event.target.value)} options={aiOptions} />
                </div>
                <div>
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Trạng thái</span>
                  <BrandedSelect value={form.status} onChange={(event) => updateForm('status', event.target.value)} options={statusOptions} />
                </div>
              </div>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Hướng dẫn làm bài</span>
                <textarea value={form.instructions} onChange={(event) => updateForm('instructions', event.target.value)} rows={3} className={TEXTAREA_CLASS} />
              </label>
              <div className="grid gap-3 md:grid-cols-3">
                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Điểm đạt</span>
                  <input type="number" value={form.passingScore} onChange={(event) => updateForm('passingScore', event.target.value)} className={FIELD_CLASS} />
                </label>
                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Điểm tối đa</span>
                  <input type="number" value={form.maxScore} onChange={(event) => updateForm('maxScore', event.target.value)} className={FIELD_CLASS} />
                </label>
                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Thời lượng</span>
                  <input type="number" value={form.timeLimitMinutes} onChange={(event) => updateForm('timeLimitMinutes', event.target.value)} className={FIELD_CLASS} />
                </label>
              </div>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Thứ tự</span>
                <input type="number" value={form.displayOrder} onChange={(event) => updateForm('displayOrder', event.target.value)} className={FIELD_CLASS} />
              </label>
              <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4">
                <button type="button" disabled={working} onClick={saveItem} className={PRIMARY_BUTTON_CLASS}>
                  <Save className="h-4 w-4" /> Lưu đề
                </button>
                <button type="button" onClick={startNew} className={SECONDARY_BUTTON_CLASS}>
                  <Plus className="h-4 w-4" /> Tạo đề khác
                </button>
              </div>
            </div>

            <div className="rounded-[24px] border border-[#ead8d6] bg-white/80 p-4">
              {canUseBuilder ? (
                <AssessmentExamBuilder assessment={form} onChange={updateForm} />
              ) : (
                <div className="space-y-4">
                  <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">Cấu hình nội dung đề</h3>
                  <label className="block">
                    <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">UI config JSON</span>
                    <textarea value={form.uiConfigJson} onChange={(event) => updateForm('uiConfigJson', event.target.value)} rows={8} className={`${TEXTAREA_CLASS} font-mono text-xs`} />
                  </label>
                  <label className="block">
                    <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Đáp án khách quan JSON</span>
                    <textarea value={form.objectiveAnswerKey} onChange={(event) => updateForm('objectiveAnswerKey', event.target.value)} rows={6} className={`${TEXTAREA_CLASS} font-mono text-xs`} />
                  </label>
                </div>
              )}
            </div>
          </div>
        </section>
      ) : null}

      <section className="space-y-4">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h3 className="font-['Manrope'] text-xl font-extrabold text-slate-900">{pageConfig.title}</h3>
            <p className="mt-1 text-sm text-slate-600">Chọn một bài để chỉnh sửa hoặc lưu trữ.</p>
          </div>
          <span className="rounded-full bg-[#fff1f0] px-3 py-1 text-xs font-bold text-[#8a0010]">
            {totalItems} bài
          </span>
        </div>

        <div className={PANEL_CLASS}>
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Tìm đề, loại hoặc kỹ năng..."
              className={SEARCH_INPUT_CLASS}
            />
          </div>
        </div>

        {loading ? (
          <p className="text-sm font-semibold text-slate-500">Đang tải ngân hàng đề...</p>
        ) : sortedItems.length === 0 ? (
          <div className={EMPTY_STATE_CLASS}>Chưa có đề trong mục này.</div>
        ) : (
          <div className="space-y-3">
            {pageItems.map((item) => (
              <article key={item.id} className={`${CARD_CLASS} transition hover:border-[#dfbfbd]`}>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="break-words font-['Manrope'] text-lg font-extrabold text-slate-900">{item.title}</h3>
                      <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">{formatLabel(item.status)}</span>
                    </div>
                    <p className="mt-1 text-sm font-semibold text-slate-500">
                      {[formatLabel(item.skill), formatLabel(item.type), item.timeLimitMinutes ? `${item.timeLimitMinutes} phút` : null].filter(Boolean).join(' · ')}
                    </p>
                    {item.description ? <p className="mt-2 text-sm leading-6 text-slate-600">{item.description}</p> : null}
                  </div>
                  <div className="flex shrink-0 flex-wrap gap-2">
                    <button type="button" onClick={() => openEdit(item)} className={GHOST_BUTTON_CLASS}>
                      <Edit3 className="h-3.5 w-3.5" /> Sửa
                    </button>
                    <button type="button" onClick={() => archiveItem(item)} disabled={working} className={DANGER_BUTTON_CLASS}>
                      <Archive className="h-3.5 w-3.5" /> Lưu trữ
                    </button>
                  </div>
                </div>
              </article>
            ))}
            <Pagination page={page} totalPages={totalPages} onChange={setPage} totalItems={totalItems} pageSize={8} />
          </div>
        )}
      </section>
    </div>
  );
}

function formatLabel(value) {
  const text = String(value || '').toUpperCase();
  const labels = {
    LISTENING: 'Nghe',
    READING: 'Đọc',
    WRITING: 'Viết',
    SPEAKING: 'Nói',
    VOCABULARY: 'Từ vựng',
    GRAMMAR: 'Ngữ pháp',
    MIXED: 'Tổng hợp',
    MOCK_TEST: 'Đề thi thử',
    MODULE_TEST: 'Bài kiểm tra mô-đun',
    LESSON_PRACTICE: 'Bài luyện trong bài học',
    WRITING_TASK: 'Bài luyện viết',
    SPEAKING_TASK: 'Bài luyện nói',
    QUIZ: 'Quiz',
    DRAFT: 'Nháp',
    PUBLISHED: 'Đã xuất bản',
    ARCHIVED: 'Lưu trữ',
  };
  return labels[text] || value || '-';
}
