import { useEffect, useMemo, useState } from 'react';
import { Archive, Edit3, Plus, RefreshCw, Save, Search, Trash2, X } from 'lucide-react';
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

const createEmptyCard = () => ({
  front: '',
  back: '',
  example: '',
  commonMistake: '',
});

const emptyForm = {
  title: '',
  description: '',
  examCategory: 'IELTS',
  skill: 'VOCABULARY',
  tags: '',
  cardsJson: '[]',
  status: 'DRAFT',
  displayOrder: 0,
};

const examOptions = [
  { label: 'IELTS', value: 'IELTS' },
  { label: 'TOEIC', value: 'TOEIC' },
  { label: 'Tiếng Anh tổng quát', value: 'GENERAL' },
];

const skillOptions = [
  { label: 'Từ vựng', value: 'VOCABULARY' },
  { label: 'Ngữ pháp', value: 'GRAMMAR' },
  { label: 'Nghe', value: 'LISTENING' },
  { label: 'Đọc', value: 'READING' },
  { label: 'Viết', value: 'WRITING' },
  { label: 'Nói', value: 'SPEAKING' },
  { label: 'Tổng hợp', value: 'MIXED' },
  { label: 'Chưa phân kỹ năng', value: '' },
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
  skill: set.skill || 'VOCABULARY',
  tags: set.tags || '',
  cardsJson: set.cardsJson || '[]',
  status: set.status || 'DRAFT',
  displayOrder: set.displayOrder ?? 0,
});

const parseCards = (cardsJson) => {
  try {
    const parsed = JSON.parse(cardsJson || '[]');
    if (!Array.isArray(parsed)) return [createEmptyCard()];
    const normalized = parsed.map((card) => ({
      front: card.front ?? card.term ?? '',
      back: card.back ?? card.definition ?? '',
      example: card.example ?? card.exampleSentence ?? '',
      commonMistake: card.commonMistake ?? card.commonErrors ?? card.errorNote ?? '',
    }));
    return normalized.length > 0 ? normalized : [createEmptyCard()];
  } catch {
    return [createEmptyCard()];
  }
};

const serializeCards = (cards) => JSON.stringify(cards.map((card) => ({
  front: card.front.trim(),
  back: card.back.trim(),
  example: card.example.trim(),
  commonMistake: card.commonMistake.trim(),
})), null, 2);

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
  const [cards, setCards] = useState([createEmptyCard()]);
  const [editingId, setEditingId] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
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

  const startNew = () => {
    setEditingId(null);
    setForm(emptyForm);
    setCards([createEmptyCard()]);
    setEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const closeEditor = () => {
    setEditingId(null);
    setForm(emptyForm);
    setCards([createEmptyCard()]);
    setEditorOpen(false);
    setError('');
    setSuccess('');
  };

  const openEdit = (set) => {
    setEditingId(set.id);
    setForm(toForm(set));
    setCards(parseCards(set.cardsJson));
    setEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const updateCard = (index, field, value) => {
    setCards((current) => current.map((card, cardIndex) => (
      cardIndex === index ? { ...card, [field]: value } : card
    )));
  };

  const addCard = () => {
    setCards((current) => [...current, createEmptyCard()]);
  };

  const removeCard = (index) => {
    setCards((current) => (current.length === 1 ? [createEmptyCard()] : current.filter((_, cardIndex) => cardIndex !== index)));
  };

  const saveSet = async () => {
    if (!form.title.trim()) {
      setError('Vui lòng nhập tên bộ flashcard.');
      return;
    }
    const validCards = cards.filter((card) => card.front.trim() || card.back.trim() || card.example.trim() || card.commonMistake.trim());
    if (validCards.length === 0 || validCards.some((card) => !card.front.trim() || !card.back.trim())) {
      setError('Mỗi thẻ cần có ít nhất thuật ngữ và định nghĩa.');
      return;
    }

    setWorking(true);
    setError('');
    setSuccess('');
    const payload = {
      ...form,
      cardsJson: serializeCards(validCards),
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
      setCards(parseCards(saved.cardsJson));
      setEditorOpen(true);
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
      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      {editorOpen ? (
        <section className={`${PANEL_CLASS} space-y-5`}>
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h3 className="font-['Manrope'] text-xl font-extrabold text-slate-900">
                {editingId ? 'Chỉnh sửa bộ flashcard' : 'Tạo bộ flashcard'}
              </h3>
              <p className="mt-1 text-sm text-slate-600">
                Nhập từng thẻ bằng form rõ ràng; hệ thống sẽ tự lưu thành dữ liệu dùng chung cho khóa học và giáo trình.
              </p>
            </div>
            <button type="button" onClick={closeEditor} className={SECONDARY_BUTTON_CLASS}>
              <X className="h-4 w-4" /> Đóng
            </button>
          </div>

          <div className="grid gap-4 xl:grid-cols-[minmax(0,420px)_1fr]">
            <div className="space-y-4 rounded-[24px] border border-[#ead8d6] bg-white/80 p-4">
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
                <input value={form.tags} onChange={(event) => setForm({ ...form, tags: event.target.value })} className={FIELD_CLASS} placeholder="family, relationships, IELTS" />
              </label>
              <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4">
                <button type="button" disabled={working} onClick={saveSet} className={PRIMARY_BUTTON_CLASS}>
                  <Save className="h-4 w-4" /> Lưu bộ thẻ
                </button>
                <button type="button" onClick={startNew} className={SECONDARY_BUTTON_CLASS}>
                  <Plus className="h-4 w-4" /> Tạo bộ khác
                </button>
              </div>
            </div>

            <div className="space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h4 className="font-['Manrope'] text-lg font-extrabold text-slate-900">Danh sách thẻ</h4>
                  <p className="text-sm text-slate-600">Mỗi thẻ nên có thuật ngữ, định nghĩa, ví dụ và lỗi thường gặp.</p>
                </div>
                <button type="button" onClick={addCard} className={SECONDARY_BUTTON_CLASS}>
                  <Plus className="h-4 w-4" /> Thêm thẻ
                </button>
              </div>

              <div className="space-y-3">
                {cards.map((card, index) => (
                  <article key={index} className="rounded-[22px] border border-[#ead8d6] bg-white/85 p-4">
                    <div className="mb-4 flex items-center justify-between gap-3">
                      <h5 className="font-['Manrope'] text-base font-extrabold text-slate-900">Thẻ {index + 1}</h5>
                      <button type="button" onClick={() => removeCard(index)} className={DANGER_BUTTON_CLASS}>
                        <Trash2 className="h-3.5 w-3.5" /> Xóa thẻ
                      </button>
                    </div>
                    <div className="grid gap-3 md:grid-cols-2">
                      <label className="block">
                        <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Thuật ngữ</span>
                        <input value={card.front} onChange={(event) => updateCard(index, 'front', event.target.value)} className={FIELD_CLASS} />
                      </label>
                      <label className="block">
                        <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Định nghĩa</span>
                        <input value={card.back} onChange={(event) => updateCard(index, 'back', event.target.value)} className={FIELD_CLASS} />
                      </label>
                    </div>
                    <div className="mt-3 grid gap-3 md:grid-cols-2">
                      <label className="block">
                        <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Ví dụ</span>
                        <textarea value={card.example} onChange={(event) => updateCard(index, 'example', event.target.value)} rows={3} className={TEXTAREA_CLASS} />
                      </label>
                      <label className="block">
                        <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Lỗi thường gặp</span>
                        <textarea value={card.commonMistake} onChange={(event) => updateCard(index, 'commonMistake', event.target.value)} rows={3} className={TEXTAREA_CLASS} />
                      </label>
                    </div>
                  </article>
                ))}
              </div>
            </div>
          </div>
        </section>
      ) : null}

      <section className={`${PANEL_CLASS} space-y-4`}>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h3 className="font-['Manrope'] text-xl font-extrabold text-slate-900">Bộ flashcard dùng chung</h3>
            <p className="mt-1 text-sm text-slate-600">Chọn một bộ thẻ để chỉnh sửa, hoặc tạo bộ mới để gắn vào khóa học sau.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button type="button" onClick={startNew} className={PRIMARY_BUTTON_CLASS}>
              <Plus className="h-4 w-4" /> Thêm bộ thẻ mới
            </button>
            <button type="button" onClick={loadSets} className={SECONDARY_BUTTON_CLASS}>
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /> Làm mới
            </button>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="relative min-w-[260px] flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Tìm bộ thẻ, kỹ năng hoặc tag..."
              className={SEARCH_INPUT_CLASS}
            />
          </div>
          <span className="rounded-full bg-[#fff1f0] px-3 py-1 text-xs font-bold text-[#8a0010]">
            {totalItems} bộ
          </span>
        </div>

        {loading ? (
          <p className="text-sm font-semibold text-slate-500">Đang tải flashcard...</p>
        ) : sortedSets.length === 0 ? (
          <div className={EMPTY_STATE_CLASS}>Chưa có bộ flashcard trong ngân hàng.</div>
        ) : (
          <div className="space-y-3">
            {pageItems.map((set) => (
              <article key={set.id} className={`${CARD_CLASS} transition hover:border-[#dfbfbd]`}>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="break-words font-['Manrope'] text-lg font-extrabold text-slate-900">{set.title}</h3>
                      <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">{formatStatus(set.status)}</span>
                    </div>
                    <p className="mt-1 text-sm font-semibold text-slate-500">
                      {[set.examCategory, formatSkill(set.skill), `${countCards(set.cardsJson)} thẻ`].join(' · ')}
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
    </div>
  );
}

function formatSkill(value) {
  const labels = {
    LISTENING: 'Nghe',
    READING: 'Đọc',
    WRITING: 'Viết',
    SPEAKING: 'Nói',
    VOCABULARY: 'Từ vựng',
    GRAMMAR: 'Ngữ pháp',
    MIXED: 'Tổng hợp',
  };
  return labels[String(value || '').toUpperCase()] || 'Chưa phân kỹ năng';
}

function formatStatus(value) {
  const labels = {
    DRAFT: 'Nháp',
    PUBLISHED: 'Đã xuất bản',
    ARCHIVED: 'Lưu trữ',
  };
  return labels[String(value || '').toUpperCase()] || value || '-';
}
