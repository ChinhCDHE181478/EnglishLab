import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  ArrowLeft,
  BookOpen,
  Check,
  ChevronRight,
  ClipboardCopy,
  Download,
  FileUp,
  Layers3,
  LibraryBig,
  Plus,
  RefreshCw,
  Trash2,
  X,
} from 'lucide-react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { Panel, StatusBadge, TextField } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import FlashcardImportDialog from '../../components/flashcard/FlashcardImportDialog';

const HEADING_REGEX = /^###\s+\d+\.\s+(.+)$/gm;
const IMPORT_TERM_DELIMITERS = [
  { label: 'Tab', value: '\t' },
  { label: 'Dấu phẩy', value: ',' },
];
const IMPORT_ROW_DELIMITERS = [
  { label: 'Dòng mới', value: '\n' },
  { label: 'Dấu chấm phẩy', value: ';' },
];

const cleanInlineMarkdown = (text = '') =>
  String(text).replace(/\*\*/g, '').replace(/^["']|["']$/g, '').trim();

const findField = (block, labels) => {
  const line = block
    .split('\n')
    .find((item) => labels.some((label) => new RegExp(`^\\*\\*${label}:\\*\\*`, 'i').test(item.trim())));
  return line ? cleanInlineMarkdown(line.replace(/^\*\*[^:]+:\*\*\s*/i, '')) : '';
};

const createEmptyCard = () => ({
  term: '',
  meaning: '',
  example: '',
  commonError: '',
});

const parseLessonVocabularyTerms = (lesson, module, course) => {
  const content = lesson?.contentText || '';
  const matches = [...content.matchAll(HEADING_REGEX)];
  if (!matches.length) return [];

  return matches
    .map((match, index) => {
      const headerStart = match.index ?? 0;
      const headerEnd = headerStart + match[0].length;
      const blockEnd = matches[index + 1]?.index ?? content.length;
      const block = content.slice(headerEnd, blockEnd);
      const meaning = findField(block, ['Meaning']);
      if (!meaning) return null;

      return {
        localId: `${course.id}-${lesson.id}-${index}`,
        termIndex: index,
        term: cleanInlineMarkdown(match[1]),
        meaning,
        example: findField(block, ['IELTS example', 'Example']),
        commonError: findField(block, ['Common error to avoid', 'Common error']),
      };
    })
    .filter(Boolean);
};

const extractFlashcardSets = (courses) =>
  (courses || []).flatMap((course) =>
    (course.modules || []).flatMap((module) =>
      (module.lessons || [])
        .map((lesson) => {
          const cards = parseLessonVocabularyTerms(lesson, module, course);
          if (!cards.length) return null;

          return {
            setId: `${course.id}-${module.id}-${lesson.id}`,
            courseId: course.id,
            courseSlug: course.slug,
            courseTitle: course.title,
            courseStatus: course.status,
            moduleId: module.id,
            moduleTitle: module.title,
            lessonId: lesson.id,
            lessonTitle: lesson.title,
            lessonDescription: lesson.description || '',
            updatedAt: course.updatedAt || course.createdAt || null,
            cardCount: cards.length,
            previewTerms: cards.slice(0, 4).map((item) => item.term),
            cards,
          };
        })
        .filter(Boolean),
    ),
  );

const toCoursePayload = (course) => {
  const modules = course.modules || [];
  const totalLessons = modules.reduce((sum, module) => sum + (module.lessons?.length || 0), 0);
  const totalHours = Math.max(
    1,
    Math.ceil(
      modules.reduce(
        (sum, module) =>
          sum +
          (module.lessons || []).reduce(
            (lessonSum, lesson) => lessonSum + Number(lesson.durationMinutes || 0),
            0,
          ),
        0,
      ) / 60,
    ),
  );

  return {
    title: course.title,
    shortDescription: course.shortDescription,
    description: course.description,
    category: course.category,
    level: course.level,
    status: course.status,
    targetScore: course.targetScore,
    recommendedCurrentBandMin: course.recommendedCurrentBandMin ?? null,
    targetBand: course.targetBand ?? null,
    learningPathCode: course.learningPathCode ?? null,
    learningPathName: course.learningPathName ?? null,
    learningPathOrder: Number(course.learningPathOrder || 0),
    targetOutcome: course.targetOutcome ?? null,
    recommendedNextCourseSlug: course.recommendedNextCourseSlug ?? null,
    duration: course.duration,
    price: Number(course.price || 0),
    thumbnailUrl: course.thumbnailUrl,
    displayOrder: Number(course.displayOrder || 0),
    featured: Boolean(course.featured),
    modules: modules.map((module, moduleIndex) => ({
      id: module.id,
      title: module.title,
      description: module.description,
      displayOrder: moduleIndex + 1,
      lessons: (module.lessons || []).map((lesson, lessonIndex) => ({
        id: lesson.id,
        title: lesson.title,
        description: lesson.description,
        contentType: String(lesson.contentType || 'VIDEO').toUpperCase(),
        contentText: lesson.contentText,
        videoUrl: lesson.videoUrl,
        materialUrl: lesson.materialUrl,
        durationMinutes: Number(lesson.durationMinutes || 0),
        displayOrder: lessonIndex + 1,
        preview: Boolean(lesson.preview),
      })),
    })),
  };
};

const buildVocabularyBlock = (term, index) => {
  const lines = [
    `### ${index + 1}. ${term.term.trim()}`,
    `**Meaning:** ${term.meaning.trim()}`,
  ];

  if (term.example.trim()) {
    lines.push(`**IELTS example:** ${term.example.trim()}`);
  }

  if (term.commonError.trim()) {
    lines.push(`**Common error to avoid:** ${term.commonError.trim()}`);
  }

  return lines.join('\n');
};

const rebuildLessonVocabularySection = (contentText, cards) => {
  const matches = [...String(contentText || '').matchAll(HEADING_REGEX)];
  const cardBlock = cards.map((card, index) => buildVocabularyBlock(card, index)).join('\n\n');

  if (!matches.length) {
    const prefix = String(contentText || '').trim();
    return prefix ? `${prefix}\n\n${cardBlock}` : cardBlock;
  }

  const start = matches[0].index ?? 0;
  const before = String(contentText || '').slice(0, start).replace(/\s*$/, '');
  return before ? `${before}\n\n${cardBlock}` : cardBlock;
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
    const [term, meaning, example = '', commonError = ''] = columns;
    if (!term || !meaning) {
      invalidRows.push(index + (hasHeader ? 2 : 1));
      return;
    }
    cards.push({ term, meaning, example, commonError });
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
    .filter((card) => String(card.term || '').trim() && String(card.meaning || '').trim())
    .map((card) =>
      [card.term, card.meaning, card.example, card.commonError]
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
  const dataRows = isSpreadsheetHeaderRow(rows[0] || []) ? rows.slice(1) : rows;
  const cards = [];
  const invalidRows = [];

  dataRows.forEach((row, index) => {
    if (!Array.isArray(row) || row.every((cell) => !String(cell ?? '').trim())) return;
    const [term, meaning, example = '', commonError = ''] = row.map((cell) => String(cell ?? '').trim());
    if (!term || !meaning) {
      invalidRows.push(index + (dataRows === rows ? 1 : 2));
      return;
    }
    cards.push({ term, meaning, example, commonError });
  });

  return { cards, invalidRows };
};

export default function ContentManagerFlashcardsPage() {
  const navigate = useNavigate();
  const { courseSlug } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [editingSet, setEditingSet] = useState(null);
  const [editorCards, setEditorCards] = useState([]);
  const [success, setSuccess] = useState('');
  const [transferMode, setTransferMode] = useState(null);
  const [importMethod, setImportMethod] = useState('TEXT');
  const [importText, setImportText] = useState('');
  const [importTermDelimiter, setImportTermDelimiter] = useState('\t');
  const [importRowDelimiter, setImportRowDelimiter] = useState('\n');
  const [importStrategy, setImportStrategy] = useState('APPEND');
  const [spreadsheetImport, setSpreadsheetImport] = useState({
    cards: [],
    invalidRows: [],
    fileName: '',
  });
  const [readingSpreadsheet, setReadingSpreadsheet] = useState(false);
  const [exportTermDelimiter, setExportTermDelimiter] = useState('\t');
  const [exportRowDelimiter, setExportRowDelimiter] = useState('\n');
  const [copied, setCopied] = useState(false);
  const loadCourses = async (activeRef = { current: true }) => {
    setLoading(true);
    setError('');
    try {
      const page = await courseApi.getManagedOnlineCourses({ page: 0, size: 200 });
      if (!activeRef.current) return;
      setCourses(page.content || []);
    } catch {
      if (activeRef.current) setError('Không tải được dữ liệu thẻ ghi nhớ từ khóa học.');
    } finally {
      if (activeRef.current) setLoading(false);
    }
  };

  useEffect(() => {
    const activeRef = { current: true };
    loadCourses(activeRef);
    return () => {
      activeRef.current = false;
    };
  }, []);

  const allSets = useMemo(() => extractFlashcardSets(courses), [courses]);

  const groupedCourses = useMemo(() => {
    const sourceSets =
      statusFilter === 'ALL'
        ? allSets
        : allSets.filter((item) => String(item.courseStatus) === statusFilter);

    const courseMap = new Map();

    sourceSets.forEach((item) => {
      if (!courseMap.has(item.courseId)) {
        courseMap.set(item.courseId, {
          courseId: item.courseId,
          courseSlug: item.courseSlug,
          courseTitle: item.courseTitle,
          courseStatus: item.courseStatus,
          setCount: 0,
          cardCount: 0,
          moduleMap: new Map(),
        });
      }

      const courseEntry = courseMap.get(item.courseId);
      courseEntry.setCount += 1;
      courseEntry.cardCount += item.cardCount;

      if (!courseEntry.moduleMap.has(item.moduleId)) {
        courseEntry.moduleMap.set(item.moduleId, {
          moduleId: item.moduleId,
          moduleTitle: item.moduleTitle,
          setCount: 0,
          cardCount: 0,
          sets: [],
        });
      }

      const moduleEntry = courseEntry.moduleMap.get(item.moduleId);
      moduleEntry.setCount += 1;
      moduleEntry.cardCount += item.cardCount;
      moduleEntry.sets.push(item);
    });

    return Array.from(courseMap.values()).map((courseEntry) => ({
      courseId: courseEntry.courseId,
      courseSlug: courseEntry.courseSlug,
      courseTitle: courseEntry.courseTitle,
      courseStatus: courseEntry.courseStatus,
      setCount: courseEntry.setCount,
      cardCount: courseEntry.cardCount,
      modules: Array.from(courseEntry.moduleMap.values()),
    }));
  }, [allSets, statusFilter]);
  const selectedCourse = useMemo(
    () => groupedCourses.find((item) => item.courseSlug === courseSlug) || null,
    [courseSlug, groupedCourses],
  );
  const selectedModuleId = searchParams.get('module');

  const selectedModule = useMemo(
    () => selectedCourse?.modules?.find((item) => String(item.moduleId) === String(selectedModuleId)) || null,
    [selectedCourse, selectedModuleId],
  );

  useEffect(() => {
    setEditingSet(null);
    setEditorCards([]);
  }, [courseSlug, selectedModuleId]);

  useEffect(() => {
    if (!selectedModule?.sets?.length) {
      setEditingSet(null);
      setEditorCards([]);
      return;
    }

    const currentSetStillExists = selectedModule.sets.some(
      (setItem) => String(setItem.setId) === String(editingSet?.setId),
    );
    if (!currentSetStillExists) {
      setEditingSet(selectedModule.sets[0]);
      setEditorCards(selectedModule.sets[0].cards.map((card) => ({ ...card })));
    }
  }, [editingSet?.setId, selectedModule]);

  const openEditor = (flashcardSet) => {
    setEditingSet(flashcardSet);
    setEditorCards(flashcardSet.cards.map((card) => ({ ...card })));
  };

  const updateEditorCard = (index, patch) => {
    setEditorCards((current) =>
      current.map((card, cardIndex) => (cardIndex === index ? { ...card, ...patch } : card)),
    );
  };

  const removeEditorCard = (index) => {
    setEditorCards((current) => current.filter((_, cardIndex) => cardIndex !== index));
  };

  const addEditorCard = () => {
    setEditorCards((current) => [...current, createEmptyCard()]);
  };

  const importPreview = useMemo(
    () => parseImportedCards(importText, importTermDelimiter, importRowDelimiter),
    [importRowDelimiter, importTermDelimiter, importText],
  );
  const activeImportPreview = importMethod === 'EXCEL' ? spreadsheetImport : importPreview;
  const exportText = useMemo(
    () => buildExportText(editorCards, exportTermDelimiter, exportRowDelimiter),
    [editorCards, exportRowDelimiter, exportTermDelimiter],
  );

  const openImport = () => {
    setImportMethod('TEXT');
    setImportText('');
    setImportStrategy('APPEND');
    setSpreadsheetImport({ cards: [], invalidRows: [], fileName: '' });
    setTransferMode('IMPORT');
    setError('');
  };

  const applyImport = () => {
    if (!activeImportPreview.cards.length) {
      setError('Không tìm thấy thẻ hợp lệ trong nội dung nhập.');
      return;
    }
    setEditorCards((current) =>
      importStrategy === 'REPLACE'
        ? activeImportPreview.cards
        : [...current, ...activeImportPreview.cards],
    );
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
    anchor.download = buildExportFileName(editingSet?.lessonTitle, 'txt');
    anchor.click();
    URL.revokeObjectURL(url);
  };

  const downloadExportSpreadsheet = async () => {
    try {
      const XLSX = await import('@e965/xlsx');
      const rows = [
        ['Thuật ngữ', 'Định nghĩa', 'Ví dụ', 'Lỗi thường gặp'],
        ...editorCards.map((card) => [
          String(card.term || '').trim(),
          String(card.meaning || '').trim(),
          String(card.example || '').trim(),
          String(card.commonError || '').trim(),
        ]),
      ];
      const worksheet = XLSX.utils.aoa_to_sheet(rows);
      worksheet['!cols'] = [{ wch: 28 }, { wch: 38 }, { wch: 55 }, { wch: 45 }];
      worksheet['!autofilter'] = { ref: `A1:D${Math.max(rows.length, 1)}` };
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, 'Thẻ ghi nhớ');
      XLSX.writeFile(workbook, buildExportFileName(editingSet?.lessonTitle, 'xlsx'));
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

  const handleSave = async () => {
    if (!editingSet?.courseSlug || !editingSet?.lessonId) return;

    const normalizedCards = editorCards
      .map((card) => ({
        term: String(card.term || '').trim(),
        meaning: String(card.meaning || '').trim(),
        example: String(card.example || '').trim(),
        commonError: String(card.commonError || '').trim(),
      }))
      .filter((card) => card.term && card.meaning);

    if (!normalizedCards.length) {
      setError('Một bộ thẻ ghi nhớ phải có ít nhất một thẻ hợp lệ.');
      return;
    }

    setSaving(true);
    setError('');
    setSuccess('');

    try {
      const fullCourse = await courseApi.getManagedOnlineCourse(editingSet.courseSlug);
      const nextCourse = {
        ...fullCourse,
        modules: (fullCourse.modules || []).map((module) => ({
          ...module,
          lessons: (module.lessons || []).map((lesson) => {
            if (String(lesson.id) !== String(editingSet.lessonId)) return lesson;
            return {
              ...lesson,
              contentText: rebuildLessonVocabularySection(lesson.contentText, normalizedCards),
            };
          }),
        })),
      };

      await courseApi.updateOnlineCourse(fullCourse.id, toCoursePayload(nextCourse));

      setCourses((current) =>
        current.map((course) => {
          if (String(course.id) !== String(fullCourse.id)) return course;
          return { ...course, ...nextCourse, updatedAt: new Date().toISOString() };
        }),
      );

      const updatedSet = {
        ...editingSet,
        cards: normalizedCards,
        cardCount: normalizedCards.length,
        previewTerms: normalizedCards.slice(0, 4).map((item) => item.term),
        updatedAt: new Date().toISOString(),
      };
      setEditingSet(updatedSet);
      setEditorCards(normalizedCards.map((card, index) => ({ ...card, localId: `${updatedSet.setId}-${index}` })));
      setSuccess('Đã lưu toàn bộ thay đổi của bộ thẻ ghi nhớ.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được bộ thẻ ghi nhớ.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <motion.div
      animate={{ opacity: 1, y: 0 }}
      className="space-y-6"
      initial={{ opacity: 0, y: 14 }}
      transition={{ duration: 0.32, ease: 'easeOut' }}
    >
      <div className="flex justify-end">
        <button
          className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/65 bg-white px-4 py-3 text-sm font-bold text-[#730014] transition hover:bg-[#fff2f3]"
          onClick={() => loadCourses()}
          type="button"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới dữ liệu
        </button>
      </div>

      {error ? (
        <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">
          {error}
        </div>
      ) : null}
      {success ? (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm font-semibold text-emerald-700">
          {success}
        </div>
      ) : null}

      {!courseSlug ? (
        <>
          <Panel className="p-6">
            <div className="grid gap-3 lg:grid-cols-[1fr_auto]">
              <FilterField label="Trạng thái khóa học">
                <BrandedSelect
                  onChange={(event) => setStatusFilter(event.target.value)}
                  options={[
                    { label: 'Tất cả trạng thái', value: 'ALL' },
                    { label: 'Nháp', value: 'DRAFT' },
                    { label: 'Đã xuất bản', value: 'PUBLISHED' },
                    { label: 'Lưu trữ', value: 'ARCHIVED' },
                  ]}
                  value={statusFilter}
                />
              </FilterField>
              <div className="flex items-end">
                <div className="rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb] px-4 py-3 text-sm text-[#584140]">
                  {groupedCourses.length} khóa học có thẻ ghi nhớ
                </div>
              </div>
            </div>
          </Panel>

          <Panel className="overflow-hidden">
            <div className="border-b border-[#eadcdc] px-6 py-5">
              <TabHeading
                icon={BookOpen}
                subtitle="Chọn một khóa học để xem các mô-đun đang chứa bộ thẻ."
                title="Khóa học có thẻ ghi nhớ"
              />
            </div>
            {loading ? (
              <div className="space-y-3 p-6">
                {[1, 2, 3].map((item) => (
                  <div key={item} className="h-24 animate-pulse rounded-2xl bg-[#f4e8e9]" />
                ))}
              </div>
            ) : groupedCourses.length ? (
              <div className="divide-y divide-[#f0e3e4]">
                {groupedCourses.map((course) => (
                  <article key={course.courseId} className="flex flex-wrap items-center justify-between gap-4 px-6 py-5 transition hover:bg-[#fffafb]">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-3">
                        <h3 className="font-['Manrope'] text-lg font-extrabold text-[#1a1c1c]">{course.courseTitle}</h3>
                        <StatusBadge label={course.courseStatus} />
                      </div>
                      <p className="mt-2 text-sm text-[#6f5553]">
                        {course.modules.length} mô-đun · {course.setCount} bộ · {course.cardCount} thẻ
                      </p>
                    </div>
                    <button
                      className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]"
                      onClick={() => navigate(`/content-manager/flashcards/${course.courseSlug}`)}
                      type="button"
                    >
                      Xem mô-đun
                      <ChevronRight className="h-4 w-4" />
                    </button>
                  </article>
                ))}
              </div>
            ) : (
              <p className="px-6 py-12 text-center text-sm text-[#584140]">Chưa có khóa học nào chứa thẻ ghi nhớ để quản lý.</p>
            )}
          </Panel>
        </>
      ) : selectedCourse ? (
        !selectedModule ? (
          <div className="space-y-6">
            <BackLink label="Tất cả khóa học" to="/content-manager/flashcards" />
            <Panel className="overflow-hidden">
              <div className="flex flex-wrap items-start justify-between gap-4 border-b border-[#eadcdc] px-6 py-5">
                <TabHeading icon={Layers3} subtitle={selectedCourse.courseTitle} title="Mô-đun có thẻ ghi nhớ" />
                <div className="flex items-center gap-3">
                  <StatusBadge label={selectedCourse.courseStatus} />
                  <Link
                    className="rounded-xl border border-[#dfbfbd] px-4 py-2.5 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]"
                    to={`/content-manager/courses/${selectedCourse.courseSlug}/builder`}
                  >
                    Biên soạn khóa học
                  </Link>
                </div>
              </div>
              <div className="grid gap-4 p-6 md:grid-cols-2 xl:grid-cols-3">
                {selectedCourse.modules.map((module) => (
                  <button
                    key={module.moduleId}
                    className="group rounded-2xl border border-[#eadcdc] bg-white p-5 text-left transition hover:border-[#730014]/40 hover:bg-[#fffafb]"
                    onClick={() => setSearchParams({ module: String(module.moduleId) })}
                    type="button"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <h3 className="font-['Manrope'] text-lg font-extrabold text-[#1a1c1c]">{module.moduleTitle}</h3>
                        <p className="mt-2 text-sm text-[#6f5553]">{module.setCount} bộ · {module.cardCount} thẻ</p>
                      </div>
                      <ChevronRight className="mt-1 h-5 w-5 text-[#a88d8b] transition group-hover:translate-x-1 group-hover:text-[#730014]" />
                    </div>
                  </button>
                ))}
              </div>
            </Panel>
          </div>
        ) : (
          <div className="space-y-6">
            <button
              className="inline-flex items-center gap-2 text-sm font-semibold text-[#730014] hover:underline"
              onClick={() => {
                setSearchParams({}, { replace: true });
                setEditingSet(null);
                setEditorCards([]);
              }}
              type="button"
            >
              <ArrowLeft className="h-4 w-4" />
              Danh sách mô-đun
            </button>

            <Panel className="p-6">
              <TabHeading
                icon={LibraryBig}
                subtitle={`${selectedCourse.courseTitle} · ${selectedModule.moduleTitle}`}
                title="Bộ thẻ trong mô-đun"
              />
              <div className="mt-5 flex flex-wrap gap-3" role="tablist" aria-label="Bộ thẻ trong mô-đun">
                {selectedModule.sets.map((flashcardSet) => {
                  const isEditing = String(flashcardSet.setId) === String(editingSet?.setId);
                  return (
                    <button
                      key={flashcardSet.setId}
                      aria-selected={isEditing}
                      className={`min-w-64 flex-1 rounded-2xl border px-5 py-4 text-left transition ${
                        isEditing
                          ? 'border-[#730014] bg-[#fff2f3] shadow-[0_12px_28px_rgba(115,0,20,0.08)]'
                          : 'border-[#eadcdc] bg-white hover:border-[#730014]/35'
                      }`}
                      onClick={() => openEditor(flashcardSet)}
                      role="tab"
                      type="button"
                    >
                      <p className="font-semibold text-[#1a1c1c]">{flashcardSet.lessonTitle}</p>
                      <p className="mt-2 text-sm text-[#6f5553]">
                        {flashcardSet.cardCount} thẻ · Cập nhật {formatDate(flashcardSet.updatedAt)}
                      </p>
                    </button>
                  );
                })}
              </div>
            </Panel>

            {editingSet ? (
              <InlineFlashcardSetEditor
                cards={editorCards}
                loading={saving}
                onAddCard={addEditorCard}
                onChangeCard={updateEditorCard}
                onExport={() => {
                  setCopied(false);
                  setTransferMode('EXPORT');
                }}
                onImport={openImport}
                onRemoveCard={removeEditorCard}
                onSave={handleSave}
                setInfo={editingSet}
              />
            ) : null}
          </div>
        )
      ) : (
        <NotFoundState message="Không tìm thấy khóa học hoặc khóa học này chưa có thẻ ghi nhớ." />
      )}

      {transferMode === 'IMPORT' ? (
        <FlashcardImportDialog
          importMethod={importMethod}
          invalidRows={activeImportPreview.invalidRows}
          onApply={applyImport}
          onClose={() => setTransferMode(null)}
          onDownloadTemplate={downloadSpreadsheetTemplate}
          onExcelFileChange={readSpreadsheet}
          onImportMethodChange={setImportMethod}
          onRowDelimiterChange={setImportRowDelimiter}
          onStrategyChange={setImportStrategy}
          onTermDelimiterChange={setImportTermDelimiter}
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
          onRowDelimiterChange={setExportRowDelimiter}
          onTermDelimiterChange={setExportTermDelimiter}
          rowDelimiter={exportRowDelimiter}
          termDelimiter={exportTermDelimiter}
        />
      ) : null}
    </motion.div>
  );
}

export function InlineFlashcardSetEditor({
  setInfo,
  cards,
  loading,
  onChangeCard,
  onExport,
  onImport,
  onRemoveCard,
  onAddCard,
  onSave,
  onSetTitleChange,
}) {
  return (
    <Panel className="flex min-h-[420px] flex-col overflow-hidden">
      <div className="border-b border-[#f0e3e4] px-6 py-5">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="min-w-0 flex-1">
            <h2 className="font-['Manrope'] text-2xl font-extrabold text-[#1a1c1c]">
              {setInfo.lessonTitle}
            </h2>
            <p className="mt-2 text-sm text-[#584140]">
              {setInfo.courseTitle} • {setInfo.moduleTitle}
            </p>
          </div>
          <div className="flex shrink-0 flex-wrap gap-2 self-start">
            <button
              className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] bg-white px-4 py-2.5 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]"
              onClick={onImport}
              type="button"
            >
              <FileUp className="h-4 w-4" />
              Nhập thẻ
            </button>
            <button
              className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] bg-white px-4 py-2.5 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]"
              onClick={onExport}
              type="button"
            >
              <Download className="h-4 w-4" />
              Xuất thẻ
            </button>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-6 py-6">
        <div className="mb-4 flex items-center justify-between gap-3">
          <p className="text-sm text-[#584140]">
            Bạn đang sửa toàn bộ thẻ của bài học này trong một lần lưu.
          </p>
          <button
            className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/65 bg-white px-4 py-3 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]"
            onClick={onAddCard}
            type="button"
          >
            <Plus className="h-4 w-4" />
            Thêm thẻ mới
          </button>
        </div>

        <div className="space-y-4">
          {cards.map((card, index) => (
            <div
              key={`${index}-${card.term}-${card.meaning}`}
              className="rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] p-5"
            >
              <div className="mb-4 flex items-center justify-between gap-3">
                <h3 className="font-semibold text-[#4b0009]">Thẻ {index + 1}</h3>
                <button
                  className="inline-flex items-center gap-2 rounded-xl border border-[#f0d4d7] px-3 py-2 text-sm font-medium text-[#93000a] transition hover:bg-[#fff6f7]"
                  onClick={() => onRemoveCard(index)}
                  type="button"
                >
                  <Trash2 className="h-4 w-4" />
                  Xóa thẻ
                </button>
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <TextField
                  label="Thuật ngữ"
                  onChange={(event) => onChangeCard(index, { term: event.target.value })}
                  value={card.term}
                />
                <TextField
                  label="Nghĩa"
                  onChange={(event) => onChangeCard(index, { meaning: event.target.value })}
                  value={card.meaning}
                />
                <div className="md:col-span-2">
                  <TextField
                    label="Ví dụ"
                    onChange={(event) => onChangeCard(index, { example: event.target.value })}
                    rows={4}
                    textarea
                    value={card.example}
                  />
                </div>
                <div className="md:col-span-2">
                  <TextField
                    label="Lỗi thường gặp"
                    onChange={(event) => onChangeCard(index, { commonError: event.target.value })}
                    rows={4}
                    textarea
                    value={card.commonError}
                  />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="flex items-center justify-between gap-3 border-t border-[#f0e3e4] bg-[#fffafb] px-6 py-4">
        <p className="text-sm text-[#584140]">
          Toàn bộ thay đổi của bộ thẻ này sẽ được lưu lại vào bài học nguồn.
        </p>
        <button
          className="rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#730014] disabled:opacity-60"
          disabled={loading}
          onClick={onSave}
          type="button"
        >
          {loading ? 'Đang lưu...' : 'Lưu bộ thẻ'}
        </button>
      </div>
    </Panel>
  );
}

function FilterField({ label, children }) {
  return (
    <div className="space-y-2">
      <label className="block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">
        {label}
      </label>
      {children}
    </div>
  );
}

function TabHeading({ icon: Icon, title, subtitle }) {
  return (
    <div className="flex items-center gap-3">
      <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]">
        <Icon className="h-5 w-5" />
      </span>
      <div>
        <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">{title}</h2>
        <p className="mt-1 text-sm text-[#6f5553]">{subtitle}</p>
      </div>
    </div>
  );
}

function BackLink({ label, to }) {
  return (
    <Link className="inline-flex items-center gap-2 text-sm font-semibold text-[#730014] hover:underline" to={to}>
      <ArrowLeft className="h-4 w-4" />
      {label}
    </Link>
  );
}

function NotFoundState({ message }) {
  return (
    <Panel className="flex min-h-[320px] flex-col items-center justify-center p-8 text-center">
      <p className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">{message}</p>
      <Link className="mt-4 rounded-xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white" to="/content-manager/flashcards">
        Quay lại danh sách khóa học
      </Link>
    </Panel>
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
      <p className="text-sm leading-6 text-[#6f5553]">
        Xuất đầy đủ Thuật ngữ, Định nghĩa, Ví dụ và Lỗi thường gặp.
      </p>
      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <TransferSelect label="Giữa các cột" onChange={onTermDelimiterChange} options={IMPORT_TERM_DELIMITERS} value={termDelimiter} />
        <TransferSelect label="Giữa các thẻ" onChange={onRowDelimiterChange} options={IMPORT_ROW_DELIMITERS} value={rowDelimiter} />
      </div>
      <textarea
        className="mt-4 min-h-64 w-full rounded-2xl border border-[#dfbfbd] bg-[#fffdfd] px-4 py-4 font-mono text-sm leading-6 text-[#1a1c1c] outline-none"
        readOnly
        value={exportText}
      />
      <p className="mt-2 text-xs leading-5 text-[#8b706e]">
        Tệp văn bản và Excel đều chứa đủ bốn trường. Khi nhập vào Quizlet, hệ thống bên đó có thể chỉ sử dụng hai cột đầu.
      </p>
      <div className="mt-5 flex flex-wrap justify-end gap-3">
        <button className="rounded-xl border border-[#dfbfbd] px-4 py-3 text-sm font-semibold text-[#730014]" onClick={onClose} type="button">Đóng</button>
        <button className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-4 py-3 text-sm font-semibold text-[#730014]" onClick={onDownload} type="button">
          <Download className="h-4 w-4" />
          Tải tệp .txt
        </button>
        <button className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-4 py-3 text-sm font-semibold text-[#730014]" onClick={onDownloadSpreadsheet} type="button">
          <Download className="h-4 w-4" />
          Tải tệp Excel
        </button>
        <button className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-5 py-3 text-sm font-semibold text-white" onClick={onCopy} type="button">
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
      <div className="max-h-[92dvh] w-full max-w-3xl overflow-y-auto rounded-[28px] border border-[#dfbfbd] bg-white p-6 shadow-[0_30px_90px_rgba(48,0,8,0.28)]">
        <div className="mb-5 flex items-center justify-between gap-4">
          <h2 className="font-['Manrope'] text-2xl font-extrabold text-[#4b0009]">{title}</h2>
          <button aria-label="Đóng" className="rounded-xl border border-[#eadcdc] p-2 text-[#730014] hover:bg-[#fff2f3]" onClick={onClose} type="button">
            <X className="h-5 w-5" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

function TransferSelect({ label, value, options, onChange }) {
  return (
    <div>
      <p className="mb-2 text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">{label}</p>
      <BrandedSelect options={options} value={value} onChange={(event) => onChange(event.target.value)} />
    </div>
  );
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}
