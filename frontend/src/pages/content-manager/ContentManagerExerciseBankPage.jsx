import { useEffect, useMemo, useState } from 'react';
import { Minus, Plus, RefreshCw, Save, Search, Trash2 } from 'lucide-react';
import courseApi from '../../api/courseApi';
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

const emptyForm = {
  title: '',
  skill: 'WRITING',
  level: '',
  exerciseType: 'HOMEWORK',
  prompt: '',
  answerKey: '',
  explanation: '',
  tags: '',
  active: true,
};

export default function ContentManagerExerciseBankPage() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [composerOpen, setComposerOpen] = useState(false);
  const [skillFilter, setSkillFilter] = useState('ALL');
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadItems = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await courseApi.getExerciseBankItems({
        skill: skillFilter === 'ALL' ? undefined : skillFilter,
        includeInactive: true,
      });
      setItems(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được ngân hàng bài tập.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadItems(); }, [skillFilter]);

  const filteredItems = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    return items.filter((item) => !normalized || [item.title, item.prompt, item.tags]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(normalized)));
  }, [items, keyword]);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    filteredItems,
    10,
    `${keyword}|${skillFilter}`,
  );

  const resetForm = (open = true) => {
    setEditingId(null);
    setForm(emptyForm);
    setComposerOpen(open);
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
      active: item.active !== false,
    });
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
    if (!window.confirm('Tạm ngưng bài tập này?')) return;
    await courseApi.deleteExerciseBankItem(id);
    await loadItems();
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="font-['Manrope'] text-2xl font-extrabold text-slate-900">Ngân hàng bài tập</h2>
          <p className="mt-1 text-sm text-slate-600">Tạo và tái sử dụng đề bài tập, quiz theo kỹ năng và cấp độ.</p>
        </div>
        <button type="button" onClick={loadItems} className={SECONDARY_BUTTON_CLASS}>
          <RefreshCw className="h-4 w-4" /> Tải lại
        </button>
      </div>

      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      <div className={PANEL_CLASS}>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">
            {editingId ? 'Chỉnh sửa bài tập' : 'Thêm bài tập mới'}
          </h3>
          <button
            type="button"
            onClick={() => setComposerOpen((current) => !current)}
            className={SECONDARY_BUTTON_CLASS}
          >
            {composerOpen ? <Minus className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
            {composerOpen ? 'Đóng' : 'Mở biểu mẫu'}
          </button>
        </div>

        {composerOpen ? (
          <div className="mt-5 space-y-4 border-t border-slate-100 pt-5">
            <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="Tiêu đề" className={FIELD_CLASS} />
            <div className="grid gap-4 md:grid-cols-2">
              <BrandedSelect label="Kỹ năng" value={form.skill} onChange={(event) => setForm({ ...form, skill: event.target.value })} options={skillOptions.filter((o) => o.value !== 'ALL')} />
              <BrandedSelect label="Loại bài" value={form.exerciseType} onChange={(event) => setForm({ ...form, exerciseType: event.target.value })} options={typeOptions} />
            </div>
            <input value={form.level} onChange={(e) => setForm({ ...form, level: e.target.value })} placeholder="Cấp độ (vd: IELTS 6.0)" className={FIELD_CLASS} />
            <textarea value={form.prompt} onChange={(e) => setForm({ ...form, prompt: e.target.value })} placeholder="Đề bài" rows={4} className={TEXTAREA_CLASS} />
            <textarea value={form.answerKey} onChange={(e) => setForm({ ...form, answerKey: e.target.value })} placeholder="Đáp án / rubric" rows={3} className={TEXTAREA_CLASS} />
            <textarea value={form.explanation} onChange={(e) => setForm({ ...form, explanation: e.target.value })} placeholder="Giải thích" rows={3} className={TEXTAREA_CLASS} />
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
        ) : null}
      </div>

      <div className="space-y-4">
          <div className="flex flex-wrap gap-3">
            <div className="relative min-w-[220px] flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Tìm bài tập..." className={SEARCH_INPUT_CLASS} />
            </div>
            <div className="w-48">
              <BrandedSelect value={skillFilter} onChange={(event) => setSkillFilter(event.target.value)} options={skillOptions} />
            </div>
          </div>
          {loading ? (
            <p className="text-sm font-semibold text-slate-500">Đang tải...</p>
          ) : pageItems.map((item) => (
            <div key={item.id} className={`${CARD_CLASS} transition hover:border-[#dfbfbd]`}>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">{item.title}</h3>
                    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold">{item.skill}</span>
                    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold">{item.exerciseType}</span>
                    {!item.active && <span className="rounded-full bg-rose-50 px-2 py-0.5 text-xs font-semibold text-rose-700">Đã tạm ngưng</span>}
                  </div>
                  <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-700">{item.prompt}</p>
                </div>
                <div className="flex gap-2">
                  <button type="button" onClick={() => openEdit(item)} className={GHOST_BUTTON_CLASS}>Sửa</button>
                  {item.active && (
                    <button type="button" onClick={() => deactivateItem(item.id)} className={DANGER_BUTTON_CLASS}>
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
          {!loading && filteredItems.length === 0 && (
            <div className={EMPTY_STATE_CLASS}>Chưa có bài tập phù hợp.</div>
          )}
          <Pagination page={page} totalPages={totalPages} onChange={setPage} totalItems={totalItems} pageSize={10} />
      </div>
    </div>
  );
}
