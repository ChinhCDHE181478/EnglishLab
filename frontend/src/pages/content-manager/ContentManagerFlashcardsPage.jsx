import { useEffect, useMemo, useRef, useState } from 'react';
import { Archive, Check, CheckCircle2, ChevronLeft, ChevronRight, ClipboardCopy, Download, Edit3, FileUp, Layers3, Plus, RefreshCw, Save, Search, SquareStack, Trash2, X } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import curriculumApi from '../../api/curriculumApi';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import BrandedSelect from '../../components/ui/BrandedSelect';
import FlashcardDictionaryAssistant from '../../components/flashcard/FlashcardDictionaryAssistant';
import { usePagination } from '../../components/ui/Pagination';
import { useAppDialog } from '../../components/ui/AppDialog';
import {
  DANGER_BUTTON_CLASS,
  EMPTY_STATE_CLASS,
  ERROR_NOTICE_CLASS,
  FIELD_CLASS,
  PANEL_CLASS,
  PRIMARY_BUTTON_CLASS,
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

const IMPORT_TERM_DELIMITERS = [
  { label: 'Tab', value: '\t' },
  { label: 'Dấu phẩy', value: ',' },
];
const IMPORT_ROW_DELIMITERS = [
  { label: 'Dòng mới', value: '\n' },
  { label: 'Dấu chấm phẩy', value: ';' },
];

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

const allOption = { label: 'Tất cả', value: 'ALL' };

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

const parseImportedCards = (text, termDelimiter, rowDelimiter) => {
  const normalizedText = String(text || '').replace(/\r\n?/g, '\n').trim();
  if (!normalizedText) return { cards: [], invalidRows: [] };

  const parsedRows = parseDelimitedText(normalizedText, termDelimiter, rowDelimiter);
  const hasHeader = isSpreadsheetHeaderRow(parsedRows[0] || []);
  const dataRows = hasHeader ? parsedRows.slice(1) : parsedRows;
  const cards = [];
  const invalidRows = [];

  dataRows.forEach((columns, index) => {
    if (!columns.some(Boolean)) return;
    const [front, back, example = '', commonMistake = ''] = columns;
    if (!front || !back) {
      invalidRows.push(index + (hasHeader ? 2 : 1));
      return;
    }
    cards.push({ front, back, example, commonMistake });
  });

  return { cards, invalidRows };
};

const parseDelimitedText = (text, columnDelimiter, rowDelimiter) => {
  const rows = [];
  let row = [];
  let current = '';
  let quoted = false;

  for (let index = 0; index < text.length; index += 1) {
    const character = text[index];
    if (character === '"') {
      if (quoted && text[index + 1] === '"') {
        current += '"';
        index += 1;
      } else {
        quoted = !quoted;
      }
    } else if (!quoted && text.startsWith(columnDelimiter, index)) {
      row.push(current.trim());
      current = '';
      index += columnDelimiter.length - 1;
    } else if (!quoted && text.startsWith(rowDelimiter, index)) {
      row.push(current.trim());
      rows.push(row);
      row = [];
      current = '';
      index += rowDelimiter.length - 1;
    } else {
      current += character;
    }
  }
  row.push(current.trim());
  rows.push(row);
  return rows;
};

const buildExportText = (cards, termDelimiter, rowDelimiter) =>
  cards
    .filter((card) => String(card.front || '').trim() && String(card.back || '').trim())
    .map((card) =>
      [card.front, card.back, card.example, card.commonMistake]
        .map((value) => escapeDelimitedValue(value, termDelimiter, rowDelimiter))
        .join(termDelimiter),
    )
    .join(rowDelimiter);

const escapeDelimitedValue = (value, columnDelimiter, rowDelimiter) => {
  const text = String(value || '').trim();
  if (!text.includes('"') && !text.includes(columnDelimiter) && !text.includes(rowDelimiter) && !text.includes('\n')) {
    return text;
  }
  return `"${text.replace(/"/g, '""')}"`;
};

const buildExportFileName = (title, extension) => {
  const baseName = String(title || 'bo-the-ghi-nho')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
    .toLowerCase();
  return `${baseName || 'bo-the-ghi-nho'}.${extension}`;
};

const normalizeSpreadsheetHeader = (value) =>
  String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');

const isSpreadsheetHeaderRow = (row) => {
  const headers = row.map(normalizeSpreadsheetHeader);
  return ['thuatngu', 'term'].includes(headers[0]) && ['dinhnghia', 'nghia', 'definition', 'meaning'].includes(headers[1]);
};

const parseSpreadsheetRows = (rows) => {
  const hasHeader = isSpreadsheetHeaderRow(rows[0] || []);
  const dataRows = hasHeader ? rows.slice(1) : rows;
  const cards = [];
  const invalidRows = [];

  dataRows.forEach((row, index) => {
    if (!Array.isArray(row) || row.every((cell) => !String(cell ?? '').trim())) return;
    const [front, back, example = '', commonMistake = ''] = row.map((cell) => String(cell ?? '').trim());
    if (!front || !back) {
      invalidRows.push(index + (hasHeader ? 2 : 1));
      return;
    }
    cards.push({ front, back, example, commonMistake });
  });

  return { cards, invalidRows };
};

export default function ContentManagerFlashcardsPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const [searchParams, setSearchParams] = useSearchParams();
  const [sets, setSets] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [cards, setCards] = useState([createEmptyCard()]);
  const [editingId, setEditingId] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [filters, setFilters] = useState({ examCategory: 'ALL', skill: 'ALL', status: 'ALL' });
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [transferMode, setTransferMode] = useState(null);
  const [importMethod, setImportMethod] = useState('TEXT');
  const [importText, setImportText] = useState('');
  const [importTermDelimiter, setImportTermDelimiter] = useState('\t');
  const [importRowDelimiter, setImportRowDelimiter] = useState('\n');
  const [importStrategy, setImportStrategy] = useState('APPEND');
  const [spreadsheetImport, setSpreadsheetImport] = useState({ cards: [], invalidRows: [], fileName: '' });
  const [readingSpreadsheet, setReadingSpreadsheet] = useState(false);
  const [exportTermDelimiter, setExportTermDelimiter] = useState('\t');
  const [exportRowDelimiter, setExportRowDelimiter] = useState('\n');
  const [copied, setCopied] = useState(false);
  const editorRef = useRef(null);

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

  useEffect(() => {
    if (searchParams.get('new') !== '1') return;
    startNew();
    setSearchParams({}, { replace: true });
  }, [searchParams, setSearchParams]);

  const filteredSets = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    return sets.filter((item) => {
      const keywordMatched = !normalized || [item.title, item.description, item.examCategory, item.skill, item.tags, item.status, item.id]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(normalized));
      const examMatched = filters.examCategory === 'ALL' || item.examCategory === filters.examCategory;
      const skillMatched = filters.skill === 'ALL' || (item.skill || '') === filters.skill;
      const statusMatched = filters.status === 'ALL' || item.status === filters.status;
      return keywordMatched && examMatched && skillMatched && statusMatched;
    });
  }, [sets, filters, keyword]);

  const sortedSets = useMemo(
    () => [...filteredSets].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || String(a.title).localeCompare(String(b.title))),
    [filteredSets],
  );

  const resetKey = `${keyword}:${filters.examCategory}:${filters.skill}:${filters.status}`;
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(sortedSets, 8, resetKey);
  const importPreview = useMemo(
    () => parseImportedCards(importText, importTermDelimiter, importRowDelimiter),
    [importRowDelimiter, importTermDelimiter, importText],
  );
  const activeImportPreview = importMethod === 'EXCEL' ? spreadsheetImport : importPreview;
  const exportText = useMemo(
    () => buildExportText(cards, exportTermDelimiter, exportRowDelimiter),
    [cards, exportRowDelimiter, exportTermDelimiter],
  );

  const stats = useMemo(() => {
    const totalCards = sets.reduce((sum, item) => sum + countCards(item.cardsJson), 0);
    return [
      { label: 'Tổng bộ thẻ', value: sets.length, icon: SquareStack, tone: 'text-[#4b0009]' },
      { label: 'Đã xuất bản', value: sets.filter((item) => item.status === 'PUBLISHED').length, icon: CheckCircle2, tone: 'text-emerald-700' },
      { label: 'Bản nháp', value: sets.filter((item) => item.status === 'DRAFT').length, icon: Edit3, tone: 'text-amber-700' },
      { label: 'Tổng số thẻ', value: totalCards, icon: Layers3, tone: 'text-[#005236]' },
    ];
  }, [sets]);

  const startNew = () => {
    setEditingId(null);
    setForm(emptyForm);
    setCards([createEmptyCard()]);
    setEditorOpen(true);
    setError('');
    setSuccess('');
    window.setTimeout(() => {
      editorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 0);
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
    window.setTimeout(() => {
      editorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 0);
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

  const openImport = () => {
    setImportMethod('TEXT');
    setImportText('');
    setImportStrategy('APPEND');
    setSpreadsheetImport({ cards: [], invalidRows: [], fileName: '' });
    setTransferMode('IMPORT');
    setError('');
    setSuccess('');
  };

  const applyImport = () => {
    if (!activeImportPreview.cards.length) {
      setError('Không tìm thấy thẻ hợp lệ trong nội dung nhập.');
      return;
    }
    setCards((current) => (
      importStrategy === 'REPLACE'
        ? activeImportPreview.cards
        : [...current.filter((card) => card.front.trim() || card.back.trim() || card.example.trim() || card.commonMistake.trim()), ...activeImportPreview.cards]
    ));
    setTransferMode(null);
    setSuccess(`Đã đưa ${activeImportPreview.cards.length} thẻ vào trình chỉnh sửa. Hãy lưu bộ thẻ để hoàn tất.`);
  };

  const readSpreadsheet = async (file) => {
    if (!file) return;
    if (!/\.(xlsx|xls)$/i.test(file.name)) {
      setError('Chỉ hỗ trợ tệp Excel .xlsx hoặc .xls.');
      return;
    }

    setReadingSpreadsheet(true);
    setError('');
    try {
      const XLSX = await import('@e965/xlsx');
      const workbook = XLSX.read(await file.arrayBuffer(), { type: 'array' });
      const sheet = workbook.Sheets[workbook.SheetNames[0]];
      const rows = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: '', raw: false });
      setSpreadsheetImport({ ...parseSpreadsheetRows(rows), fileName: file.name });
    } catch {
      setSpreadsheetImport({ cards: [], invalidRows: [], fileName: file.name });
      setError('Không đọc được tệp Excel. Hãy kiểm tra tệp có bị hỏng hoặc đặt mật khẩu hay không.');
    } finally {
      setReadingSpreadsheet(false);
    }
  };

  const copyExportText = async () => {
    try {
      await navigator.clipboard.writeText(exportText);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setError('Trình duyệt không cho phép sao chép tự động. Hãy chọn và sao chép nội dung xuất thủ công.');
    }
  };

  const downloadExportText = () => {
    const blob = new Blob([exportText], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = buildExportFileName(form.title, 'txt');
    anchor.click();
    URL.revokeObjectURL(url);
  };

  const downloadExportSpreadsheet = async () => {
    try {
      const XLSX = await import('@e965/xlsx');
      const rows = [
        ['Thuật ngữ', 'Định nghĩa', 'Ví dụ', 'Lỗi thường gặp'],
        ...cards.map((card) => [
          String(card.front || '').trim(),
          String(card.back || '').trim(),
          String(card.example || '').trim(),
          String(card.commonMistake || '').trim(),
        ]),
      ];
      const worksheet = XLSX.utils.aoa_to_sheet(rows);
      worksheet['!cols'] = [{ wch: 28 }, { wch: 38 }, { wch: 55 }, { wch: 45 }];
      worksheet['!autofilter'] = { ref: `A1:D${Math.max(rows.length, 1)}` };
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, 'Thẻ ghi nhớ');
      XLSX.writeFile(workbook, buildExportFileName(form.title, 'xlsx'));
    } catch {
      setError('Không tạo được tệp Excel. Hãy thử lại.');
    }
  };

  const downloadSpreadsheetTemplate = async () => {
    try {
      const XLSX = await import('@e965/xlsx');
      const rows = [
        ['Thuật ngữ', 'Định nghĩa', 'Ví dụ', 'Lỗi thường gặp'],
        [
          'immediate family',
          'gia đình ruột thịt gần gũi nhất',
          'My immediate family lives in Hanoi.',
          'Không dùng để chỉ họ hàng xa.',
        ],
      ];
      const worksheet = XLSX.utils.aoa_to_sheet(rows);
      worksheet['!cols'] = [{ wch: 28 }, { wch: 38 }, { wch: 55 }, { wch: 45 }];
      worksheet['!autofilter'] = { ref: 'A1:D2' };
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, 'Mẫu thẻ ghi nhớ');
      XLSX.writeFile(workbook, 'mau-import-the-ghi-nho-englishlab.xlsx');
    } catch {
      setError('Không tạo được bản mẫu Excel. Hãy thử lại.');
    }
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
    if (!await confirmDialog(`Lưu trữ bộ flashcard “${set.title}”?`, {
      title: 'Lưu trữ bộ flashcard',
      confirmLabel: 'Lưu trữ',
      tone: 'danger',
    })) return;
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

  const restoreSet = async (set) => {
    if (!await confirmDialog(`Khôi phục bộ flashcard “${set.title}” về bản nháp?`, {
      title: 'Khôi phục bộ flashcard',
      confirmLabel: 'Khôi phục',
    })) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const saved = await curriculumApi.updateFlashcardSet(set.id, {
        ...toForm(set),
        status: 'DRAFT',
      });
      setSets((current) => current.map((item) => (
        String(item.id) === String(saved.id) ? saved : item
      )));
      if (String(editingId) === String(set.id)) {
        setForm(toForm(saved));
      }
      setSuccess('Đã khôi phục bộ flashcard về bản nháp.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không khôi phục được bộ flashcard.');
    } finally {
      setWorking(false);
    }
  };

  const publishSet = async (set) => {
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const saved = await curriculumApi.updateFlashcardSet(set.id, { ...toForm(set), status: 'PUBLISHED' });
      setSets((current) => current.map((item) => (String(item.id) === String(saved.id) ? saved : item)));
      if (String(editingId) === String(saved.id)) setForm(toForm(saved));
      setSuccess('Đã xuất bản bộ flashcard.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không xuất bản được bộ flashcard.');
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-6">
      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      {editorOpen && (
        <FlashcardEditorModal onClose={closeEditor}>
          <section className="space-y-5" ref={editorRef}>
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
                  <RichTextEditor
                    helperText=""
                    onChange={(value) => setForm({ ...form, description: value })}
                    placeholder="Mô tả bộ flashcard..."
                    size="compact"
                    value={form.description}
                  />
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
                  <button type="button" onClick={openImport} className={SECONDARY_BUTTON_CLASS}>
                    <FileUp className="h-4 w-4" /> Nhập thẻ
                  </button>
                  <button type="button" onClick={() => setTransferMode('EXPORT')} className={SECONDARY_BUTTON_CLASS}>
                    <Download className="h-4 w-4" /> Xuất thẻ
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
                        <FlashcardDictionaryAssistant
                          example={card.example}
                          exampleInputClassName={TEXTAREA_CLASS}
                          meaning={card.back}
                          meaningInputClassName={FIELD_CLASS}
                          onExampleChange={(example) => updateCard(index, 'example', example)}
                          onMeaningChange={(back) => updateCard(index, 'back', back)}
                          showLabels
                          term={card.front}
                        />
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
        </FlashcardEditorModal>
      )}

      <div className="grid gap-6 md:grid-cols-4">
            {stats.map((item) => {
              const Icon = item.icon;
              return (
                <section className="rounded-xl border border-[#dcc0bf]/30 bg-white p-4 shadow-[0_4px_12px_rgba(75,0,9,0.05)]" key={item.label}>
                  <div className="mb-1 flex items-center justify-between gap-3">
                    <span className={`text-xs font-bold uppercase tracking-[0.12em] ${item.tone}`}>{item.label}</span>
                    <Icon className={`h-5 w-5 ${item.tone}`} />
                  </div>
                  <p className="font-['Manrope'] text-3xl font-extrabold text-[#0b1c30]">{item.value}</p>
                </section>
              );
            })}
          </div>

          <section className="rounded-xl border border-[#dcc0bf]/30 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap items-center gap-4">
          <div className="min-w-[300px] flex-1">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-[#897270]" />
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Tìm bộ thẻ, ID hoặc chủ đề..."
                className="w-full rounded-lg border border-[#dcc0bf]/50 bg-[#f8f9ff] py-2 pl-10 pr-4 text-sm text-[#0b1c30] outline-none transition focus:border-[#4b0009] focus:bg-white focus:ring-4 focus:ring-[#4b0009]/5"
              />
            </div>
          </div>
          <div className="grid w-full gap-3 sm:grid-cols-3 lg:w-auto">
            <FilterSelect label="Danh mục" onChange={(event) => setFilters((current) => ({ ...current, examCategory: event.target.value }))} options={[allOption, ...examOptions]} value={filters.examCategory} />
            <FilterSelect label="Kỹ năng" onChange={(event) => setFilters((current) => ({ ...current, skill: event.target.value }))} options={[allOption, ...skillOptions]} value={filters.skill} />
            <FilterSelect label="Trạng thái" onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))} options={[allOption, ...statusOptions]} value={filters.status} />
          </div>
          <button
            aria-label="Làm mới danh sách flashcard"
            type="button"
            onClick={loadSets}
            className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#dcc0bf]/40 text-[#564241] transition hover:bg-[#eff4ff]"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
          </section>

          <section className="overflow-hidden rounded-xl border border-[#dcc0bf]/30 bg-white shadow-sm">
        {loading ? (
          <div className="p-6 text-sm font-semibold text-slate-500">Đang tải flashcard...</div>
        ) : sortedSets.length === 0 ? (
          <div className={EMPTY_STATE_CLASS}>Chưa có bộ flashcard trong ngân hàng.</div>
        ) : (
          <div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[1040px] border-collapse text-left">
                <thead>
                  <tr className="border-b border-[#dcc0bf]/30 bg-[#fbf3f4]">
                    {['Tên bộ thẻ', 'Danh mục', 'Kỹ năng', 'Số thẻ', 'Trạng thái', 'Cập nhật lần cuối', 'Thao tác'].map((heading) => (
                      <th
                        className={`px-6 py-4 text-xs font-bold uppercase tracking-[0.12em] text-[#8e7371] ${heading === 'Số thẻ' ? 'text-center' : ''} ${heading === 'Thao tác' ? 'text-right' : ''}`}
                        key={heading}
                      >
                        {heading}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#dcc0bf]/15">
                  {pageItems.map((set) => (
                    <tr className="transition hover:bg-[#eff4ff]" key={set.id}>
                      <td className="px-6 py-5">
                        <div className="min-w-0">
                          <p className="max-w-[300px] overflow-hidden text-sm font-bold leading-5 text-[#4b0009] [display:-webkit-box] [-webkit-box-orient:vertical] [-webkit-line-clamp:2]">{set.title}</p>
                        </div>
                      </td>
                      <td className="px-6 py-5 text-sm text-[#0b1c30]">{formatExamCategory(set.examCategory)}</td>
                      <td className="px-6 py-5">
                        <span className="inline-flex rounded-lg border border-[#dcc0bf]/40 bg-[#dce9ff] px-2.5 py-1 text-[11px] font-bold uppercase tracking-[0.08em] text-[#564241]">
                          {formatSkill(set.skill)}
                        </span>
                      </td>
                      <td className="px-6 py-5 text-center text-sm font-semibold text-[#0b1c30]">{countCards(set.cardsJson)}</td>
                      <td className="px-6 py-5"><StatusPill status={set.status} /></td>
                      <td className="px-6 py-5 text-sm text-[#564241]">{formatDate(set.updatedAt || set.createdAt)}</td>
                      <td className="px-6 py-5 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            type="button"
                            onClick={() => openEdit(set)}
                            className="inline-flex items-center gap-1.5 rounded-lg border border-[#dcc0bf]/50 px-3 py-1.5 text-xs font-bold text-[#4b0009] transition hover:bg-[#fff7f7]"
                          >
                            <Edit3 className="h-3.5 w-3.5" />
                            Chỉnh sửa
                          </button>
                          {set.status === 'ARCHIVED' ? (
                            <button
                              type="button"
                              onClick={() => restoreSet(set)}
                              disabled={working}
                              className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-[#4b0009] px-4 py-1.5 text-xs font-bold text-white transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-45"
                            >
                              <RefreshCw className="h-3.5 w-3.5" />
                              Khôi phục
                            </button>
                          ) : set.status === 'DRAFT' ? (
                            <button
                              type="button"
                              onClick={() => publishSet(set)}
                              disabled={working}
                              className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-[#4b0009] px-4 py-1.5 text-xs font-bold text-white transition hover:bg-[#730014] disabled:opacity-45"
                            >
                              <CheckCircle2 className="h-3.5 w-3.5" />
                              Xuất bản
                            </button>
                          ) : set.status === 'PUBLISHED' ? (
                            <button
                              type="button"
                              onClick={() => archiveSet(set)}
                              disabled={working}
                              className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-[#4b0009] px-4 py-1.5 text-xs font-bold text-white transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-45"
                            >
                              <Archive className="h-3.5 w-3.5" />
                              Lưu trữ
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <TablePagination page={page} pageSize={8} totalItems={totalItems} totalPages={totalPages} onChange={setPage} />
          </div>
        )}
          </section>

      {transferMode === 'IMPORT' ? (
        <ImportDialog
          importMethod={importMethod}
          invalidRows={activeImportPreview.invalidRows}
          onApply={applyImport}
          onClose={() => setTransferMode(null)}
          onDownloadTemplate={downloadSpreadsheetTemplate}
          onExcelFileChange={readSpreadsheet}
          onImportMethodChange={setImportMethod}
          onRowDelimiterChange={(event) => setImportRowDelimiter(event.target.value)}
          onStrategyChange={setImportStrategy}
          onTermDelimiterChange={(event) => setImportTermDelimiter(event.target.value)}
          onTextChange={setImportText}
          previewCards={activeImportPreview.cards}
          readingSpreadsheet={readingSpreadsheet}
          rowDelimiter={importRowDelimiter}
          spreadsheetFileName={spreadsheetImport.fileName}
          strategy={importStrategy}
          termDelimiter={importTermDelimiter}
          text={importText}
        />
      ) : null}

      {transferMode === 'EXPORT' ? (
        <ExportDialog
          copied={copied}
          exportText={exportText}
          onClose={() => setTransferMode(null)}
          onCopy={copyExportText}
          onDownload={downloadExportText}
          onDownloadSpreadsheet={downloadExportSpreadsheet}
          onRowDelimiterChange={(event) => setExportRowDelimiter(event.target.value)}
          onTermDelimiterChange={(event) => setExportTermDelimiter(event.target.value)}
          rowDelimiter={exportRowDelimiter}
          termDelimiter={exportTermDelimiter}
        />
      ) : null}
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
  return labels[String(value || '').toUpperCase()] || 'Chưa có';
}

function formatExamCategory(value) {
  const labels = {
    IELTS: 'IELTS',
    TOEIC: 'TOEIC',
    GENERAL: 'Tiếng Anh tổng quát',
  };
  return labels[String(value || '').toUpperCase()] || value || '-';
}

function formatStatus(value) {
  const labels = {
    DRAFT: 'Nháp',
    PUBLISHED: 'Đã xuất bản',
    ARCHIVED: 'Lưu trữ',
  };
  return labels[String(value || '').toUpperCase()] || value || '-';
}

function StatusPill({ status }) {
  const normalized = String(status || '').toUpperCase();
  const tone = normalized === 'PUBLISHED'
    ? 'border-emerald-500/20 bg-emerald-100 text-emerald-700'
    : normalized === 'DRAFT'
      ? 'border-amber-500/20 bg-amber-100 text-amber-700'
      : 'border-slate-500/20 bg-slate-100 text-slate-700';

  return (
    <span className={`inline-flex whitespace-nowrap rounded-lg border px-2.5 py-1 text-[11px] font-bold ${tone}`}>
      {formatStatus(status)}
    </span>
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

function TablePagination({ page, pageSize, totalItems, totalPages, onChange }) {
  const from = totalItems ? (page - 1) * pageSize + 1 : 0;
  const to = Math.min(page * pageSize, totalItems);
  const pages = buildPageItems(page, totalPages);

  return (
    <div className="flex flex-col gap-3 border-t border-[#dcc0bf]/20 bg-[#eff4ff]/30 px-6 py-4 sm:flex-row sm:items-center sm:justify-between">
      <p className="text-sm text-[#564241]">
        Hiển thị <span className="font-bold text-[#0b1c30]">{from} - {to}</span> của <span className="font-bold text-[#0b1c30]">{totalItems}</span> bộ thẻ
      </p>
      <div className="flex items-center gap-2">
        <button
          className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-[#dcc0bf]/40 text-[#564241] transition hover:bg-[#eff4ff] disabled:opacity-30"
          disabled={page <= 1}
          onClick={() => onChange(page - 1)}
          type="button"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        {pages.map((item, index) => (
          item === 'dots' ? (
            <span className="px-1 text-sm text-[#564241]" key={`${item}-${index}`}>...</span>
          ) : (
            <button
              className={`inline-flex h-8 w-8 items-center justify-center rounded-lg text-sm font-bold transition ${item === page ? 'bg-[#4b0009] text-white' : 'text-[#0b1c30] hover:bg-[#eff4ff]'}`}
              key={item}
              onClick={() => onChange(item)}
              type="button"
            >
              {item}
            </button>
          )
        ))}
        <button
          className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-[#dcc0bf]/40 text-[#564241] transition hover:bg-[#eff4ff] disabled:opacity-30"
          disabled={page >= totalPages}
          onClick={() => onChange(page + 1)}
          type="button"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

function buildPageItems(currentPage, totalPages) {
  if (totalPages <= 5) return Array.from({ length: totalPages }, (_, index) => index + 1);
  const items = [1];
  if (currentPage > 3) items.push('dots');
  const start = Math.max(2, currentPage - 1);
  const end = Math.min(totalPages - 1, currentPage + 1);
  for (let item = start; item <= end; item += 1) items.push(item);
  if (currentPage < totalPages - 2) items.push('dots');
  items.push(totalPages);
  return items;
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function ImportDialog({
  text,
  importMethod,
  termDelimiter,
  rowDelimiter,
  strategy,
  previewCards,
  invalidRows,
  spreadsheetFileName,
  readingSpreadsheet,
  onTextChange,
  onImportMethodChange,
  onExcelFileChange,
  onDownloadTemplate,
  onTermDelimiterChange,
  onRowDelimiterChange,
  onStrategyChange,
  onApply,
  onClose,
}) {
  return (
    <TransferDialog onClose={onClose} title="Nhập bộ thẻ">
      <div className="grid grid-cols-2 gap-2 rounded-xl border border-[#dcc0bf]/30 bg-[#eff4ff] p-1">
        {[
          { label: 'Dán văn bản', value: 'TEXT' },
          { label: 'Tệp Excel', value: 'EXCEL' },
        ].map((option) => (
          <button
            className={`rounded-lg px-4 py-3 text-sm font-bold transition ${
              importMethod === option.value ? 'bg-[#4b0009] text-white' : 'text-[#4b0009] hover:bg-white'
            }`}
            key={option.value}
            onClick={() => onImportMethodChange(option.value)}
            type="button"
          >
            {option.label}
          </button>
        ))}
      </div>

      {importMethod === 'TEXT' ? (
        <>
          <p className="mt-4 text-sm leading-6 text-[#564241]">
            Mỗi dòng gồm bốn cột theo thứ tự: Thuật ngữ, Định nghĩa, Ví dụ, Lỗi thường gặp.
          </p>
          <textarea
            className="mt-4 min-h-52 w-full rounded-xl border border-[#dcc0bf]/50 bg-[#f8f9ff] px-4 py-4 font-mono text-sm leading-6 text-[#0b1c30] outline-none focus:border-[#4b0009] focus:bg-white"
            onChange={(event) => onTextChange(event.target.value)}
            placeholder={'immediate family\tyour closest family members\tMy immediate family lives nearby.\tKhông dùng cho họ hàng xa.'}
            value={text}
          />
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <TransferSelect label="Giữa các cột" onChange={onTermDelimiterChange} options={IMPORT_TERM_DELIMITERS} value={termDelimiter} />
            <TransferSelect label="Giữa các thẻ" onChange={onRowDelimiterChange} options={IMPORT_ROW_DELIMITERS} value={rowDelimiter} />
          </div>
        </>
      ) : (
        <div className="mt-4 rounded-xl border border-dashed border-[#dcc0bf] bg-[#f8f9ff] p-6 text-center">
          <FileUp className="mx-auto h-8 w-8 text-[#4b0009]" />
          <p className="mt-3 font-bold text-[#4b0009]">Chọn tệp Excel có bốn cột</p>
          <p className="mt-1 text-sm text-[#564241]">Thuật ngữ - Định nghĩa - Ví dụ - Lỗi thường gặp</p>
          <label className="mt-4 inline-flex cursor-pointer items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-3 text-sm font-bold text-white">
            <FileUp className="h-4 w-4" />
            {readingSpreadsheet ? 'Đang đọc tệp...' : 'Chọn tệp .xlsx hoặc .xls'}
            <input
              accept=".xlsx,.xls"
              className="sr-only"
              disabled={readingSpreadsheet}
              onChange={(event) => onExcelFileChange(event.target.files?.[0])}
              type="file"
            />
          </label>
          <button
            className="ml-2 mt-4 inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf] bg-white px-4 py-3 text-sm font-bold text-[#4b0009] transition hover:bg-[#eff4ff]"
            onClick={onDownloadTemplate}
            type="button"
          >
            <Download className="h-4 w-4" />
            Tải bản mẫu Excel
          </button>
          {spreadsheetFileName ? <p className="mt-3 text-sm font-bold text-[#4b0009]">{spreadsheetFileName}</p> : null}
        </div>
      )}

      <div className="mt-4">
        <p className="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-[#564241]">Cách nhập</p>
        <div className="grid grid-cols-2 gap-2 rounded-xl border border-[#dcc0bf]/30 bg-[#eff4ff] p-1">
          {[
            { label: 'Nối thêm', value: 'APPEND' },
            { label: 'Thay thế bộ hiện tại', value: 'REPLACE' },
          ].map((option) => (
            <button
              className={`rounded-lg px-4 py-3 text-sm font-bold transition ${
                strategy === option.value ? 'bg-[#4b0009] text-white' : 'text-[#4b0009] hover:bg-white'
              }`}
              key={option.value}
              onClick={() => onStrategyChange(option.value)}
              type="button"
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-5 rounded-xl border border-[#dcc0bf]/30 bg-[#f8f9ff] p-4">
        <div className="flex items-center justify-between gap-3">
          <p className="font-bold text-[#4b0009]">Xem trước</p>
          <span className="text-sm text-[#564241]">{previewCards.length} thẻ hợp lệ</span>
        </div>
        {invalidRows.length ? (
          <p className="mt-2 text-sm text-[#93000a]">Dòng chưa đúng định dạng: {invalidRows.join(', ')}</p>
        ) : null}
        <div className="mt-3 max-h-56 space-y-2 overflow-y-auto">
          {previewCards.slice(0, 20).map((card, index) => (
            <div key={`${card.front}-${index}`} className="grid gap-2 rounded-lg bg-white px-3 py-2 text-sm sm:grid-cols-2">
              <span className="font-bold text-[#0b1c30]">{card.front}</span>
              <span className="text-[#564241]">{card.back}</span>
              <span className="text-[#564241]">{card.example || 'Chưa có ví dụ'}</span>
              <span className="text-[#564241]">{card.commonMistake || 'Chưa có lỗi thường gặp'}</span>
            </div>
          ))}
          {!previewCards.length ? <p className="text-sm text-[#564241]">Chưa có dữ liệu hợp lệ để xem trước.</p> : null}
        </div>
      </div>

      <div className="mt-5 flex justify-end gap-3">
        <button className="rounded-lg border border-[#dcc0bf] px-4 py-3 text-sm font-bold text-[#4b0009]" onClick={onClose} type="button">Hủy</button>
        <button className="rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white disabled:opacity-50" disabled={!previewCards.length} onClick={onApply} type="button">
          Nhập {previewCards.length || ''} thẻ
        </button>
      </div>
    </TransferDialog>
  );
}

function ExportDialog({
  exportText,
  termDelimiter,
  rowDelimiter,
  copied,
  onTermDelimiterChange,
  onRowDelimiterChange,
  onCopy,
  onDownload,
  onDownloadSpreadsheet,
  onClose,
}) {
  return (
    <TransferDialog onClose={onClose} title="Xuất bộ thẻ">
      <p className="text-sm leading-6 text-[#564241]">
        Xuất đầy đủ Thuật ngữ, Định nghĩa, Ví dụ và Lỗi thường gặp.
      </p>
      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <TransferSelect label="Giữa các cột" onChange={onTermDelimiterChange} options={IMPORT_TERM_DELIMITERS} value={termDelimiter} />
        <TransferSelect label="Giữa các thẻ" onChange={onRowDelimiterChange} options={IMPORT_ROW_DELIMITERS} value={rowDelimiter} />
      </div>
      <textarea
        className="mt-4 min-h-64 w-full rounded-xl border border-[#dcc0bf]/50 bg-[#f8f9ff] px-4 py-4 font-mono text-sm leading-6 text-[#0b1c30] outline-none"
        readOnly
        value={exportText}
      />
      <p className="mt-2 text-xs leading-5 text-[#564241]">
        Tệp văn bản và Excel đều chứa đủ bốn trường. Khi nhập vào Quizlet, hệ thống bên đó có thể chỉ sử dụng hai cột đầu.
      </p>
      <div className="mt-5 flex flex-wrap justify-end gap-3">
        <button className="rounded-lg border border-[#dcc0bf] px-4 py-3 text-sm font-bold text-[#4b0009]" onClick={onClose} type="button">Đóng</button>
        <button className="inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf] px-4 py-3 text-sm font-bold text-[#4b0009]" onClick={onDownload} type="button">
          <Download className="h-4 w-4" />
          Tải tệp .txt
        </button>
        <button className="inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf] px-4 py-3 text-sm font-bold text-[#4b0009]" onClick={onDownloadSpreadsheet} type="button">
          <Download className="h-4 w-4" />
          Tải tệp Excel
        </button>
        <button className="inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white" onClick={onCopy} type="button">
          {copied ? <Check className="h-4 w-4" /> : <ClipboardCopy className="h-4 w-4" />}
          {copied ? 'Đã sao chép' : 'Sao chép văn bản'}
        </button>
      </div>
    </TransferDialog>
  );
}

function TransferDialog({ title, children, onClose }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#1a0004]/45 p-4 backdrop-blur-sm" role="dialog" aria-modal="true">
      <div className="max-h-[92dvh] w-full max-w-3xl overflow-y-auto rounded-2xl border border-[#dcc0bf] bg-white p-6 shadow-[0_30px_90px_rgba(48,0,8,0.28)]">
        <div className="mb-5 flex items-center justify-between gap-4">
          <h2 className="font-['Manrope'] text-2xl font-extrabold text-[#4b0009]">{title}</h2>
          <button aria-label="Đóng" className="rounded-lg border border-[#dcc0bf]/50 p-2 text-[#4b0009] hover:bg-[#eff4ff]" onClick={onClose} type="button">
            <X className="h-5 w-5" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

function TransferSelect({ label, value, onChange, options }) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.12em] text-[#564241]">{label}</span>
      <BrandedSelect
        buttonClassName="rounded-lg border-[#dcc0bf]/50 bg-[#f8f9ff] shadow-none"
        onChange={onChange}
        options={options}
        value={value}
      />
    </label>
  );
}

function FlashcardEditorModal({ children, onClose }) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-hidden px-3 py-4 sm:px-6 animate-fade-in" role="dialog" aria-modal="true">
      <button
        aria-label="Đóng modal"
        className="absolute inset-0 bg-[#1a0004]/45 backdrop-blur-sm"
        onClick={onClose}
        type="button"
      />
      <div className="relative z-10 w-full max-w-[1000px] pointer-events-auto bg-[#fafafa] rounded-3xl border border-[#dcc0bf]/35 p-6 shadow-2xl overflow-y-auto max-h-[90vh]">
        {children}
      </div>
    </div>
  );
}
