import { useEffect, useMemo, useState } from 'react';
import { Archive, Brain, Edit3, Plus, RefreshCw, Save, Search } from 'lucide-react';
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

const emptyForm = {
  title: '',
  description: '',
  examCategory: 'IELTS',
  skill: '',
  tags: '',
  cardsJson: '[\n  { "front": "", "back": "" }\n]',
  status: 'DRAFT',
  displayOrder: 0,
};

const examOptions = [
  { label: 'IELTS', value: 'IELTS' },
  { label: 'TOEIC', value: 'TOEIC' },
  { label: 'General English', value: 'GENERAL' },
];

const skillOptions = [
  { label: 'Chưa phân kỹ năng', value: '' },
  { label: 'Listening', value: 'LISTENING' },
  { label: 'Reading', value: 'READING' },
  { label: 'Writing', value: 'WRITING' },
  { label: 'Speaking', value: 'SPEAKING' },
  { label: 'Vocabulary', value: 'VOCABULARY' },
  { label: 'Grammar', value: 'GRAMMAR' },
  { label: 'Mixed', value: 'MIXED' },
];

const statusOptions = [
  { label: 'Nháp', value: 'DRAFT' },
  { label: 'Đã xuất bản', value: 'PUBLISHED' },
  { label: 'Lưu trữ', value: 'ARCHIVED' },
];

const toForm = (set = {}) => ({
  title: set.title || '',
  description: set.description || '',
  examCategory: set.examCategory || 'IELTS',
  skill: set.skill || '',
  tags: set.tags || '',
  cardsJson: set.cardsJson || emptyForm.cardsJson,
  status: set.status || 'DRAFT',
  displayOrder: set.displayOrder ?? 0,
});

const countCards = (cardsJson) => {
  try {
    const parsed = JSON.parse(cardsJson || '[]');
    return Array.isArray(parsed) ? parsed.length : 0;
  } catch {
    return 0;
  }
};

export default function ContentManagerFlashcardsPage() {
  const [sets, setSets] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadSets = async () => {
    setLoading(true);
    setError('');
    try {
      setSets(await curriculumApi.getFlashcardSets());
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được ngân hàng flashcard.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSets();
  }, []);

  const filteredSets = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) return sets;
    return sets.filter((item) => [item.title, item.description, item.examCategory, item.skill, item.tags, item.status]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(normalized)));
  }, [sets, keyword]);

  const sortedSets = useMemo(
    () => [...filteredSets].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || String(a.title).localeCompare(String(b.title))),
    [filteredSets],
  );

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(sortedSets, 8, keyword);

  const resetForm = () => {
    setEditingId(null);
    setForm(emptyForm);
    setError('');
    setSuccess('');
  };

  const openEdit = (set) => {
    setEditingId(set.id);
    setForm(toForm(set));
    setError('');
    setSuccess('');
  };

  const saveSet = async () => {
    if (!form.title.trim()) {
      setError('Vui lòng nhập tên bộ flashcard.');
      return;
    }
    try {
      JSON.parse(form.cardsJson || '[]');
    } catch {
      setError('Nội dung thẻ phải là JSON hợp lệ.');
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    const payload = {
      ...form,
      displayOrder: Number(form.displayOrder || 0),
      skill: form.skill || null,
    };
    try {
      const saved = editingId
        ? await curriculumApi.updateFlashcardSet(editingId, payload)
        : await curriculumApi.createFlashcardSet(payload);
      setSets((current) => {
        if (editingId) {
          return current.map((item) => (String(item.id) === String(saved.id) ? saved : item));
        }
        return [saved, ...current];
      });
      setEditingId(saved.id);
      setForm(toForm(saved));
      setSuccess(editingId ? 'Đã cập nhật bộ flashcard.' : 'Đã tạo bộ flashcard mới.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được bộ flashcard.');
    } finally {
      setWorking(false);
    }
  };

  const archiveSet = async (set) => {
    if (!window.confirm(`Lưu trữ bộ flashcard "${set.title}"?`)) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await curriculumApi.archiveFlashcardSet(set.id);
      setSets((current) => current.map((item) => (
        String(item.id) === String(set.id) ? { ...item, status: 'ARCHIVED' } : item
      )));
      if (String(editingId) === String(set.id)) {
        setForm((current) => ({ ...current, status: 'ARCHIVED' }));
      }
      setSuccess('Đã lưu trữ bộ flashcard.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu trữ được bộ flashcard.');
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="mb-2 inline-flex items-center gap-2 rounded-full bg-[#730014]/10 px-3 py-1 text-xs font-bold text-[#730014]">
            <Brain className="h-3.5 w-3.5" />
            Bank dùng chung
          </div>
          <h2 className="font-['Manrope'] text-2xl font-extrabold text-slate-900">Ngân hàng flashcard</h2>
          <p className="mt-1 max-w-3xl text-sm text-slate-600">
            Tạo và chỉnh sửa bộ flashcard độc lập. Curriculum hoặc course chỉ gắn reference tới bộ này, không tạo bản sao.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={resetForm} className={SECONDARY_BUTTON_CLASS}>
            <Plus className="h-4 w-4" /> Tạo mới
          </button>
          <button type="button" onClick={loadSets} className={SECONDARY_BUTTON_CLASS}>
            <RefreshCw className="h-4 w-4" /> Tải lại
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
                placeholder="Tìm bộ thẻ, kỹ năng hoặc tag..."
                className={SEARCH_INPUT_CLASS}
              />
            </div>
          </div>

          {loading ? (
            <p className="text-sm font-semibold text-slate-500">Đang tải flashcard...</p>
          ) : sortedSets.length === 0 ? (
            <div className={EMPTY_STATE_CLASS}>Chưa có bộ flashcard trong bank.</div>
          ) : (
            <div className="space-y-3">
              {pageItems.map((set) => (
                <article key={set.id} className={`${CARD_CLASS} transition hover:border-[#dfbfbd]`}>
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="break-words font-['Manrope'] text-lg font-extrabold text-slate-900">{set.title}</h3>
                        <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">{set.status}</span>
                      </div>
                      <p className="mt-1 text-sm font-semibold text-slate-500">
                        {[set.examCategory, set.skill || 'Chưa phân kỹ năng', `${countCards(set.cardsJson)} thẻ`].join(' · ')}
                      </p>
                      {set.description ? <p className="mt-2 text-sm leading-6 text-slate-600">{set.description}</p> : null}
                      {set.tags ? <p className="mt-2 text-xs font-semibold text-slate-500">Tags: {set.tags}</p> : null}
                    </div>
                    <div className="flex shrink-0 flex-wrap gap-2">
                      <button type="button" onClick={() => openEdit(set)} className={GHOST_BUTTON_CLASS}>
                        <Edit3 className="h-3.5 w-3.5" /> Sửa
                      </button>
                      <button type="button" onClick={() => archiveSet(set)} disabled={working} className={DANGER_BUTTON_CLASS}>
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

        <aside className={PANEL_CLASS}>
          <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">
            {editingId ? 'Chỉnh sửa bộ flashcard' : 'Tạo bộ flashcard'}
          </h3>
          <div className="mt-5 space-y-4">
            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Tên bộ thẻ</span>
              <input value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} className={FIELD_CLASS} />
            </label>
            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Mô tả</span>
              <textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} rows={3} className={TEXTAREA_CLASS} />
            </label>
            <div className="grid gap-3 md:grid-cols-2">
              <div>
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Nhóm thi</span>
                <BrandedSelect value={form.examCategory} onChange={(event) => setForm({ ...form, examCategory: event.target.value })} options={examOptions} />
              </div>
              <div>
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Kỹ năng</span>
                <BrandedSelect value={form.skill} onChange={(event) => setForm({ ...form, skill: event.target.value })} options={skillOptions} />
              </div>
            </div>
            <div className="grid gap-3 md:grid-cols-2">
              <div>
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Trạng thái</span>
                <BrandedSelect value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })} options={statusOptions} />
              </div>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Thứ tự</span>
                <input
                  type="number"
                  min="0"
                  value={form.displayOrder}
                  onChange={(event) => setForm({ ...form, displayOrder: event.target.value })}
                  className={FIELD_CLASS}
                />
              </label>
            </div>
            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Tags</span>
              <input value={form.tags} onChange={(event) => setForm({ ...form, tags: event.target.value })} className={FIELD_CLASS} />
            </label>
            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Nội dung thẻ JSON</span>
              <textarea
                value={form.cardsJson}
                onChange={(event) => setForm({ ...form, cardsJson: event.target.value })}
                rows={10}
                className={`${TEXTAREA_CLASS} font-mono text-xs`}
              />
            </label>
            <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4">
              <button type="button" disabled={working} onClick={saveSet} className={PRIMARY_BUTTON_CLASS}>
                <Save className="h-4 w-4" /> Lưu bộ thẻ
              </button>
              <button type="button" onClick={resetForm} className={SECONDARY_BUTTON_CLASS}>
                <Plus className="h-4 w-4" /> Mới
              </button>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
