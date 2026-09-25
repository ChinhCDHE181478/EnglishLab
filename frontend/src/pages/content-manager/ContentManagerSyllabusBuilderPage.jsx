import { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import {
  Archive,
  BookMarked,
  BookOpen,
  Check,
  CheckCircle2,
  ChevronDown,
  Clock,
  Coins,
  Copy,
  DollarSign,
  Download,
  Eye,
  FileSpreadsheet,
  FileText,
  Filter,
  GraduationCap,
  HelpCircle,
  Info,
  Layers,
  Link2,
  LoaderCircle,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Search,
  Sparkles,
  Star,
  Target,
  Trash2,
  UploadCloud,
  Users,
  Wand2,
  X,
} from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import classroomApi from '../../api/classroomApi';
import curriculumApi from '../../api/curriculumApi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import ManagementToast from '../../components/ui/ManagementToast';
import {
  EnglishEntryLevelField,
  IeltsBandSelect,
  ToeicScoreField,
} from '../../components/content-manager/EnglishScoreFields';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import RichTextHtml from '../../components/content-manager/RichTextHtml';
import { HeaderActions, Panel } from '../../components/content-manager/ContentManagerUi';
import { ManagerTaxonomyBadge } from '../../components/content-manager/ManagerListUi';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import { useAppDialog } from '../../components/ui/AppDialog';
import { getContentManagerError } from '../../utils/contentManagerFeedback';
import {
  FIELD_CLASS,
  TEXTAREA_CLASS,
} from '../../utils/formStyles';
import {
  ENGLISH_EXAM_OPTIONS,
  ENGLISH_SKILL_OPTIONS,
  getEnglishProfileDefaults,
  normalizeEnglishEntryLevel,
  normalizeEnglishExamCategory,
  readEnglishFocusSkills,
  validateEnglishCourseProfile,
} from '../../utils/englishCourseProfile';
import {
  downloadInstructorLedCourseExcelTemplate,
  importCourseUnitsWithLessons,
  parseInstructorLedCourseExcelFile,
} from '../../utils/curriculumExcel';
import { getLessonOrderInUnit } from '../../utils/classroomHelpers';

const emptyUnit = {
  title: '',
  description: '',
  displayOrder: 0,
};

const emptyLesson = {
  sessionNumber: 1,
  displayOrder: 0,
  plannedSessionCount: 1,
  title: '',
  description: '',
  learningObjectives: '',
};

const emptyAttach = {
  unitId: '',
  type: 'MATERIAL',
  resourceId: '',
};

const emptyInstructorLedCourseForm = {
  title: '',
  code: '',
  shortDescription: '',
  description: '',
  durationLabel: '',
  examCategory: 'IELTS',
  focusSkills: ['LISTENING', 'READING', 'WRITING', 'SPEAKING'],
  targetBand: 6.5,
  targetScore: '',
  entryLevel: '4.0',
  outcomes: '',
  status: 'DRAFT',
};

const typeOptions = [
  { label: 'Học liệu trung tâm', value: 'MATERIAL', description: 'Thêm tài liệu từ kho học liệu trung tâm.' },
  { label: 'Bài tập & Đề luyện tập', value: 'ASSESSMENT', description: 'Thêm bài tập / đề luyện tập từ 4 mục Luyện nghe, Luyện đọc, Luyện viết, Luyện nói.' },
  { label: 'Bộ Flashcard', value: 'FLASHCARD', description: 'Thêm bộ flashcard từ kho từ vựng.' },
];

const refGroups = [
  { key: 'materials', title: 'Học liệu', icon: BookMarked },
  { key: 'exercises', title: 'Bài tập', icon: Layers },
  { key: 'assessments', title: 'Đề đánh giá', icon: GraduationCap },
  { key: 'flashcards', title: 'Flashcard', icon: Sparkles },
];

const asList = (value) => (Array.isArray(value) ? value : value?.content || value?.items || []);
const countStructuredLessons = (items = []) => items.reduce(
  (total, unit) => total + (unit.lessons?.length || 0),
  0,
);

const PAGE_SIZE = 8;
const UNIT_PAGE_SIZE = 6;

const parseStructuredResource = (value) => {
  if (typeof value !== 'string' || !value.trim().startsWith('{')) return null;
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' ? parsed : null;
  } catch {
    return null;
  }
};

const describeStructuredResource = (config) => {
  const parts = Array.isArray(config?.parts) ? config.parts : [];
  const questionCount = parts.reduce((total, part) => (
    total + (part.questionGroups || []).reduce((partTotal, group) => (
      partTotal + (group.questionNumbers?.length || group.questions?.length || 0)
    ), 0)
  ), 0);
  return [
    config?.durationMinutes ? `${config.durationMinutes} phút` : null,
    questionCount ? `${questionCount} câu hỏi` : null,
    parts.length ? `${parts.length} phần làm bài` : null,
  ].filter(Boolean).join(' · ') || 'Nội dung làm trực tiếp trên hệ thống';
};

// Các kỹ năng mà khóa học có giảng viên được phép tham chiếu tới kho đề luyện tập
// (đồng bộ với 4 tab Luyện nghe / Luyện đọc / Luyện viết / Luyện nói).
const SKILL_DISPLAY_LABELS = {
  LISTENING: 'Luyện nghe',
  READING: 'Luyện đọc',
  WRITING: 'Luyện viết',
  SPEAKING: 'Luyện nói',
};

const TYPE_DISPLAY_LABELS = {
  LESSON_PRACTICE: 'Bài luyện trong bài học',
  WRITING_TASK: 'Bài luyện viết',
  SPEAKING_TASK: 'Bài luyện nói',
  QUIZ: 'Quiz',
  MOCK_TEST: 'Đề thi thử',
};

const normalizeAssessmentKey = (value) => String(value || '').toUpperCase().trim();

const formatAssessmentSkill = (skill) => SKILL_DISPLAY_LABELS[normalizeAssessmentKey(skill)] || String(skill || '');

const formatAssessmentType = (type) => TYPE_DISPLAY_LABELS[normalizeAssessmentKey(type)] || String(type || '');

const getReadableResourceText = (value) => {
  const config = parseStructuredResource(value);
  return config ? describeStructuredResource(config) : value;
};

const makeCode = (title, examCategory) => {
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
  return [examCategory || 'ILC', ...words].filter(Boolean).join('-');
};

const toInstructorLedCourseForm = (course) => {
  const examCategory = normalizeEnglishExamCategory(course?.examCategory);
  const defaults = getEnglishProfileDefaults(examCategory);
  return {
    ...emptyInstructorLedCourseForm,
    ...course,
    shortDescription: course?.shortDescription || '',
    description: course?.description || '',
    durationLabel: course?.durationLabel || '',
    examCategory,
    focusSkills: readEnglishFocusSkills(course?.focusSkills, examCategory),
    targetBand: examCategory === 'IELTS' ? (course?.targetBand ?? defaults.targetBand) : '',
    targetScore: examCategory === 'TOEIC' ? (course?.targetScore ?? defaults.targetScore) : '',
    entryLevel: normalizeEnglishEntryLevel(course?.entryLevel, examCategory),
    outcomes: course?.outcomes || course?.learningOutcomes || '',
  };
};

const toInstructorLedCoursePayload = (form, forceDraft = false) => ({
  title: form.title.trim(),
  code: form.code.trim() || makeCode(form.title, form.examCategory),
  shortDescription: form.shortDescription?.trim() || null,
  description: form.description?.trim() || null,
  durationLabel: form.durationLabel?.trim() || null,
  examCategory: form.examCategory,
  focusSkills: form.focusSkills.join(','),
  targetBand: form.targetBand === '' ? null : Number(form.targetBand),
  targetScore: form.targetScore === '' ? null : Number(form.targetScore),
  entryLevel: form.entryLevel?.trim() || null,
  outcomes: form.outcomes?.trim() || null,
  status: forceDraft ? 'DRAFT' : (form.status || 'DRAFT'),
});

export default function ContentManagerInstructorLedCoursesPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedInstructorLedCourseId = searchParams.get('courseId') || searchParams.get('programId') || '';
  const requestedUnitId = searchParams.get('unitId');
  const requestedPanel = searchParams.get('panel');
  const [instructorLedCourses, setInstructorLedCourses] = useState([]);
  const [selectedInstructorLedCourseId, setSelectedInstructorLedCourseId] = useState(requestedInstructorLedCourseId);
  const [instructorLedCourseDetail, setInstructorLedCourseDetail] = useState(null);
  const [instructorLedCourseForm, setInstructorLedCourseForm] = useState(emptyInstructorLedCourseForm);
  const [instructorLedCourseCreatorOpen, setInstructorLedCourseCreatorOpen] = useState(false);
  const [instructorLedCourseEditorOpen, setInstructorLedCourseEditorOpen] = useState(false);
  const [unitForm, setUnitForm] = useState(emptyUnit);
  const [editingUnitId, setEditingUnitId] = useState(null);
  const [unitEditorOpen, setUnitEditorOpen] = useState(requestedPanel === 'unit');
  const [lessonForm, setLessonForm] = useState(emptyLesson);
  const [editingLessonId, setEditingLessonId] = useState(null);
  const [lessonUnitId, setLessonUnitId] = useState(null);
  const [lessonEditorOpen, setLessonEditorOpen] = useState(false);
  const [attachForm, setAttachForm] = useState(() => ({ ...emptyAttach, unitId: requestedUnitId || '' }));
  const [resourcePanelOpen, setResourcePanelOpen] = useState(requestedPanel === 'resource');
  const [expandedUnitIds, setExpandedUnitIds] = useState(() => new Set());
  const [keyword, setKeyword] = useState('');
  const [banks, setBanks] = useState({
    materials: [],
    exercises: [],
    assessments: [],
    flashcards: [],
  });
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [excelImportOpen, setExcelImportOpen] = useState(false);
  const [excelExamCategory, setExcelExamCategory] = useState('IELTS');
  const [parsedExcel, setParsedExcel] = useState(null);
  const [excelReading, setExcelReading] = useState(false);
  const [excelImporting, setExcelImporting] = useState(false);
  const [excelError, setExcelError] = useState('');

  const loadInstructorLedCourses = async () => {
    setLoading(true);
    setError('');
    try {
      const courseData = await curriculumApi.getInstructorLedCourses();
      setInstructorLedCourses(courseData);
      setSelectedInstructorLedCourseId((current) => (
        current && courseData.some((course) => String(course.id) === current) ? current : ''
      ));
    } catch (err) {
      setError(getContentManagerError(err, 'Không tải được danh sách khóa học.'));
    } finally {
      setLoading(false);
    }
  };

  const loadInstructorLedCourseDetail = async (instructorLedCourseId) => {
    if (!instructorLedCourseId) {
      setInstructorLedCourseDetail(null);
      return;
    }
    setError('');
    try {
      const data = await curriculumApi.getInstructorLedCourse(instructorLedCourseId);
      setInstructorLedCourseDetail(data);
      setAttachForm((current) => ({
        ...current,
        unitId: current.unitId || (data?.units?.[0]?.id ? String(data.units[0].id) : ''),
      }));
    } catch (err) {
      setError(getContentManagerError(err, 'Không tải được nội dung khóa học.'));
    }
  };

  const loadBanks = async () => {
    try {
      const [materials, assessments, flashcards] = await Promise.all([
        classroomApi.getContentManagerMaterialLibrary(),
        curriculumApi.getAssessmentBank(),
        curriculumApi.getFlashcardSets(),
      ]);
      const isPublished = (item) => String(item?.status || '').toUpperCase() === 'PUBLISHED';
      setBanks({
        materials: asList(materials).filter(isPublished),
        exercises: [],
        assessments: asList(assessments).filter(isPublished),
        flashcards: asList(flashcards).filter(isPublished),
      });
    } catch (err) {
      setError(getContentManagerError(err, 'Không tải được các kho tài nguyên.'));
    }
  };

  const reloadAll = async () => {
    await Promise.all([loadInstructorLedCourses(), loadBanks()]);
    if (selectedInstructorLedCourseId) {
      await loadInstructorLedCourseDetail(selectedInstructorLedCourseId);
    }
  };

  useEffect(() => {
    loadInstructorLedCourses();
    loadBanks();
  }, []);

  useEffect(() => {
    if (selectedInstructorLedCourseId) {
      loadInstructorLedCourseDetail(selectedInstructorLedCourseId);
      setEditingUnitId(null);
      setUnitForm(emptyUnit);
      setAttachForm({ ...emptyAttach, unitId: requestedUnitId || '' });
    } else {
      setInstructorLedCourseDetail(null);
    }
  }, [requestedUnitId, selectedInstructorLedCourseId]);

  const openInstructorLedCourseWorkspace = (course) => {
    setSelectedInstructorLedCourseId(String(course.id));
    setExpandedUnitIds(new Set());
    setSearchParams({ courseId: String(course.id) }, { replace: true });
    setKeyword('');
    setError('');
    setSuccess('');
  };

  const closeInstructorLedCourseWorkspace = () => {
    setSelectedInstructorLedCourseId('');
    setExpandedUnitIds(new Set());
    setSearchParams({}, { replace: true });
    setInstructorLedCourseDetail(null);
    setKeyword('');
    setEditingUnitId(null);
    setUnitForm(emptyUnit);
    setAttachForm(emptyAttach);
    setError('');
    setSuccess('');
  };

  const selectInstructorLedCourse = (instructorLedCourseId) => {
    setSelectedInstructorLedCourseId(instructorLedCourseId);
    setExpandedUnitIds(new Set());
    setSearchParams(instructorLedCourseId ? { courseId: instructorLedCourseId } : {}, { replace: true });
  };

  const updateInstructorLedCourseForm = (patch) => {
    setInstructorLedCourseForm((current) => {
      const next = { ...current, ...patch };
      if (Object.prototype.hasOwnProperty.call(patch, 'title')) {
        next.code = current.code || makeCode(patch.title, current.examCategory);
      }
      return next;
    });
  };

  const openInstructorLedCourseCreator = () => {
    setInstructorLedCourseForm(emptyInstructorLedCourseForm);
    setInstructorLedCourseCreatorOpen(true);
    setError('');
    setSuccess('');
  };

  const closeInstructorLedCourseCreator = () => {
    setInstructorLedCourseCreatorOpen(false);
    setInstructorLedCourseForm(emptyInstructorLedCourseForm);
  };

  const openInstructorLedCourseEditor = (course) => {
    const target = course || instructorLedCourseDetail;
    if (!target) return;
    setInstructorLedCourseForm(toInstructorLedCourseForm(target));
    setInstructorLedCourseEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const closeInstructorLedCourseEditor = () => {
    setInstructorLedCourseEditorOpen(false);
    setInstructorLedCourseForm(emptyInstructorLedCourseForm);
  };

  const createInstructorLedCourse = async (event) => {
    event.preventDefault();
    if (!instructorLedCourseForm.title.trim()) {
      setError('Vui lòng nhập tên khóa học.');
      return;
    }
    const generatedCode = instructorLedCourseForm.code.trim() || makeCode(instructorLedCourseForm.title, instructorLedCourseForm.examCategory);
    if (!generatedCode) {
      setError('Vui lòng nhập mã khóa học.');
      return;
    }
    const profileError = validateEnglishCourseProfile(instructorLedCourseForm);
    if (profileError) {
      setError(profileError);
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const saved = await curriculumApi.createInstructorLedCourse({
        ...toInstructorLedCoursePayload(instructorLedCourseForm, true),
        code: generatedCode,
        displayOrder: 0,
      });
      setInstructorLedCourses((current) => [saved, ...current]);
      setSelectedInstructorLedCourseId(String(saved.id));
      setSearchParams({ courseId: String(saved.id) }, { replace: true });
      setInstructorLedCourseDetail(saved);
      closeInstructorLedCourseCreator();
      setSuccess('Đã tạo khóa học. Bắt đầu thêm Unit và bài học.');
    } catch (err) {
      setError(getContentManagerError(err, 'Không tạo được khóa học.'));
    } finally {
      setWorking(false);
    }
  };

  const updateInstructorLedCourse = async (event) => {
    event.preventDefault();
    if (!instructorLedCourseForm.title.trim()) {
      setError('Vui lòng nhập tên khóa học.');
      return;
    }
    const profileError = validateEnglishCourseProfile(instructorLedCourseForm);
    if (profileError) {
      setError(profileError);
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const targetId = instructorLedCourseForm.id || selectedInstructorLedCourseId;
      const saved = await curriculumApi.updateInstructorLedCourse(
        targetId,
        toInstructorLedCoursePayload(instructorLedCourseForm),
      );
      setInstructorLedCourses((current) => current.map((item) => (String(item.id) === String(targetId) ? saved : item)));
      if (String(selectedInstructorLedCourseId) === String(targetId)) {
        setInstructorLedCourseDetail(saved);
      }
      closeInstructorLedCourseEditor();
      setSuccess('Đã cập nhật thông tin khóa học.');
    } catch (err) {
      setError(getContentManagerError(err, 'Không cập nhật được khóa học.'));
    } finally {
      setWorking(false);
    }
  };

  const cloneInstructorLedCourse = async (course) => {
    if (!await confirmDialog({
      title: 'Nhân bản khóa học',
      message: `Bạn có chắc muốn nhân bản khóa học “${course.title}”? Bản sao sẽ ở trạng thái Bản nháp.`,
      confirmText: 'Nhân bản',
    })) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const cloned = await curriculumApi.cloneInstructorLedCourse(course.id);
      setInstructorLedCourses((current) => [cloned, ...current]);
      setSuccess(`Đã nhân bản khóa học thành “${cloned.title}”.`);
    } catch (err) {
      setError(getContentManagerError(err, 'Không nhân bản được khóa học.'));
    } finally {
      setWorking(false);
    }
  };

  const publishInstructorLedCourse = async (course) => {
    if (!await confirmDialog({
      title: 'Xuất bản khóa học',
      message: `Xuất bản khóa học “${course.title}”? Sau khi xuất bản, khóa học sẽ sẵn sàng để mở lớp đào tạo.`,
      confirmText: 'Xuất bản',
      tone: 'primary',
    })) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const published = await curriculumApi.publishInstructorLedCourse(course.id);
      setInstructorLedCourses((current) => current.map((item) => (item.id === course.id ? published : item)));
      if (String(selectedInstructorLedCourseId) === String(course.id)) {
        setInstructorLedCourseDetail(published);
      }
      setSuccess(`Đã xuất bản thành công khóa học “${published.title}”.`);
    } catch (err) {
      setError(getContentManagerError(err, 'Không thể xuất bản khóa học. Vui lòng kiểm tra lại cấu trúc bài học và chuẩn đầu ra.'));
    } finally {
      setWorking(false);
    }
  };

  const archiveInstructorLedCourse = async (course) => {
    if (!await confirmDialog({
      title: 'Lưu trữ khóa học',
      message: `Bạn có chắc muốn lưu trữ khóa học “${course.title}”? Khóa học sẽ không thể dùng để mở lớp mới.`,
      confirmText: 'Lưu trữ',
      tone: 'danger',
    })) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await curriculumApi.archiveInstructorLedCourse(course.id);
      setInstructorLedCourses((current) => current.filter((item) => item.id !== course.id));
      if (String(selectedInstructorLedCourseId) === String(course.id)) {
        closeInstructorLedCourseWorkspace();
      }
      setSuccess('Đã lưu trữ khóa học.');
    } catch (err) {
      setError(getContentManagerError(err, 'Không lưu trữ được khóa học.'));
    } finally {
      setWorking(false);
    }
  };

  const closeExcelImport = () => {
    setExcelImportOpen(false);
    setParsedExcel(null);
    setExcelError('');
  };

  const readInstructorLedCourseExcel = async (file) => {
    if (!file) return;
    if (!/\.(xlsx|xls)$/i.test(file.name)) {
      setExcelError('Chỉ hỗ trợ tệp Excel định dạng .xlsx hoặc .xls.');
      return;
    }
    setExcelReading(true);
    setExcelError('');
    setParsedExcel(null);
    try {
      setParsedExcel(await parseInstructorLedCourseExcelFile(file));
    } catch (err) {
      setExcelError(err.message || 'Không đọc được tệp Excel.');
    } finally {
      setExcelReading(false);
    }
  };

  const importInstructorLedCourseFromExcel = async () => {
    if (!parsedExcel) return;
    setExcelImporting(true);
    setExcelError('');
    try {
      const profileDefaults = getEnglishProfileDefaults(excelExamCategory);
      const course = await curriculumApi.createInstructorLedCourse({
        ...toInstructorLedCoursePayload({
          ...emptyInstructorLedCourseForm,
          title: parsedExcel.title,
          code: makeCode(parsedExcel.title, excelExamCategory),
          examCategory: excelExamCategory,
          ...profileDefaults,
        }, true),
        displayOrder: 0,
      });
      const importResult = await importCourseUnitsWithLessons(
        curriculumApi,
        course.id,
        parsedExcel.units,
      );
      setInstructorLedCourses((current) => [course, ...current]);
      setSelectedInstructorLedCourseId(String(course.id));
      setSearchParams({ courseId: String(course.id) }, { replace: true });
      await loadInstructorLedCourseDetail(course.id);
      closeExcelImport();
      setSuccess(`Đã import thành công khóa học “${parsedExcel.title}” (${importResult.createdUnits} Unit, ${importResult.createdLessons} bài học).`);
    } catch (err) {
      setExcelError(getContentManagerError(err, 'Không import được tệp Excel.'));
    } finally {
      setExcelImporting(false);
    }
  };

  const openEditUnit = (unit) => {
    setEditingUnitId(unit.id);
    setUnitForm({
      title: unit.title || '',
      description: unit.description || '',
      displayOrder: unit.displayOrder ?? 0,
    });
    setUnitEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const resetUnitForm = () => {
    setEditingUnitId(null);
    setUnitForm({
      title: '',
      description: '',
      displayOrder: (instructorLedCourseDetail?.units?.length || 0) + 1,
    });
    setUnitEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const closeUnitEditor = () => {
    setUnitEditorOpen(false);
    setEditingUnitId(null);
    setUnitForm(emptyUnit);
  };

  const saveUnit = async (event) => {
    if (event) event.preventDefault();
    if (!selectedInstructorLedCourseId) {
      setError('Vui lòng chọn khóa học trước khi thêm Unit.');
      return;
    }
    if (!unitForm.title.trim()) {
      setError('Vui lòng nhập tên Unit.');
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    const payload = {
      title: unitForm.title.trim(),
      description: unitForm.description?.trim() || null,
      displayOrder: Number(unitForm.displayOrder || 0),
    };
    try {
      if (editingUnitId) {
        await curriculumApi.updateCourseUnit(editingUnitId, payload);
        setSuccess('Đã cập nhật Unit.');
      } else {
        await curriculumApi.createCourseUnit(selectedInstructorLedCourseId, payload);
        setSuccess('Đã thêm Unit mới vào khóa học.');
      }
      await loadInstructorLedCourseDetail(selectedInstructorLedCourseId);
      closeUnitEditor();
    } catch (err) {
      setError(getContentManagerError(err, 'Không lưu được Unit.'));
    } finally {
      setWorking(false);
    }
  };

  const deleteUnit = async (unit) => {
    if (!await confirmDialog({
      title: `Xóa Unit “${unit.title}”?`,
      message: 'Toàn bộ bài học và tài nguyên liên kết trong Unit này sẽ bị xóa.',
      confirmText: 'Xóa Unit',
      tone: 'danger',
    })) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await curriculumApi.deleteCourseUnit(unit.id);
      await loadInstructorLedCourseDetail(selectedInstructorLedCourseId);
      setSuccess(`Đã xóa Unit “${unit.title}”.`);
    } catch (err) {
      setError(getContentManagerError(err, 'Không xóa được Unit.'));
    } finally {
      setWorking(false);
    }
  };

  const openLessonCreator = (unit) => {
    setLessonUnitId(unit.id);
    setEditingLessonId(null);
    const maxSession = (unit.lessons || []).reduce(
      (max, lesson) => Math.max(max, getLessonOrderInUnit(lesson)),
      0,
    );
    setLessonForm({
      sessionNumber: maxSession + 1,
      displayOrder: maxSession + 1,
      plannedSessionCount: 1,
      title: '',
      description: '',
      learningObjectives: '',
    });
    setLessonEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const openLessonEditor = (unit, lesson) => {
    setLessonUnitId(unit.id);
    setEditingLessonId(lesson.id);
    setLessonForm({
      sessionNumber: lesson.sessionNumber ?? 1,
      displayOrder: lesson.displayOrder ?? 0,
      plannedSessionCount: lesson.plannedSessionCount ?? 1,
      title: lesson.title || '',
      description: lesson.description || '',
      learningObjectives: lesson.learningObjectives || '',
    });
    setLessonEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const closeLessonEditor = () => {
    setLessonEditorOpen(false);
    setEditingLessonId(null);
    setLessonUnitId(null);
    setLessonForm(emptyLesson);
  };

  const saveLesson = async (event) => {
    event.preventDefault();
    if (!lessonUnitId || !lessonForm.title.trim()) {
      setError('Vui lòng nhập tiêu đề bài học.');
      return;
    }
    if (!Number.isInteger(Number(lessonForm.sessionNumber)) || Number(lessonForm.sessionNumber) < 1) {
      setError('Thứ tự bài trong unit phải là số nguyên bắt đầu từ 1.');
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    const payload = {
      sessionNumber: Number(lessonForm.sessionNumber),
      displayOrder: Number(lessonForm.displayOrder || 0),
      plannedSessionCount: Math.max(1, Number(lessonForm.plannedSessionCount || 1)),
      title: lessonForm.title.trim(),
      description: lessonForm.description?.trim() || null,
      learningObjectives: lessonForm.learningObjectives?.trim() || null,
    };
    try {
      if (editingLessonId) {
        await curriculumApi.updateCourseLesson(editingLessonId, payload);
        setSuccess('Đã cập nhật bài học.');
      } else {
        await curriculumApi.createCourseLesson(lessonUnitId, payload);
        setSuccess('Đã thêm bài học vào Unit.');
      }
      await loadInstructorLedCourseDetail(selectedInstructorLedCourseId);
      closeLessonEditor();
    } catch (err) {
      setError(getContentManagerError(err, 'Không lưu được bài học.'));
    } finally {
      setWorking(false);
    }
  };

  const deleteLesson = async (unit, lesson) => {
    const order = getLessonOrderInUnit(lesson) || lesson.sessionNumber;
    if (!await confirmDialog({
      title: `Xóa Bài ${order}?`,
      message: `Bạn có chắc muốn xóa bài học “${lesson.title}”?`,
      confirmText: 'Xóa bài học',
      tone: 'danger',
    })) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await curriculumApi.deleteCourseLesson(lesson.id);
      await loadInstructorLedCourseDetail(selectedInstructorLedCourseId);
      setSuccess(`Đã xóa Bài ${order}.`);
    } catch (err) {
      setError(getContentManagerError(err, 'Không xóa được bài học.'));
    } finally {
      setWorking(false);
    }
  };

  const openResourcePanel = (unitId) => {
    setAttachForm({ ...emptyAttach, unitId: String(unitId) });
    setResourcePanelOpen(true);
    setError('');
    setSuccess('');
  };

  const closeResourcePanel = () => {
    setResourcePanelOpen(false);
    setAttachForm(emptyAttach);
  };

  const attachResource = async (event) => {
    event.preventDefault();
    if (!attachForm.unitId || !attachForm.resourceId) {
      setError('Vui lòng chọn Unit và tài nguyên cần gắn.');
      return;
    }
    if (attachedResourceIds.has(String(attachForm.resourceId))) {
      setError('Tài nguyên này đã tồn tại trong Unit đã chọn.');
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    const payload = {
      resourceId: Number(attachForm.resourceId),
      displayOrder: 0,
    };
    try {
      if (attachForm.type === 'MATERIAL') await curriculumApi.attachUnitMaterial(attachForm.unitId, payload);
      else if (attachForm.type === 'ASSESSMENT') await curriculumApi.attachUnitAssessment(attachForm.unitId, payload);
      else if (attachForm.type === 'FLASHCARD') await curriculumApi.attachUnitFlashcard(attachForm.unitId, payload);
      await loadInstructorLedCourseDetail(selectedInstructorLedCourseId);
      closeResourcePanel();
      setSuccess('Đã gắn tài nguyên vào Unit.');
    } catch (err) {
      setError(getContentManagerError(err, 'Không gắn được tài nguyên.'));
    } finally {
      setWorking(false);
    }
  };

  const detachResource = async (ref) => {
    if (!await confirmDialog({
      title: `Gỡ tài nguyên “${ref.title || 'Tài nguyên'}”?`,
      message: 'Tài nguyên sẽ được gỡ khỏi Unit nhưng vẫn tồn tại trong ngân hàng tài nguyên gốc.',
      confirmText: 'Gỡ tài nguyên',
      tone: 'danger',
    })) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await curriculumApi.detachReference(ref.type, ref.id);
      await loadInstructorLedCourseDetail(selectedInstructorLedCourseId);
      setSuccess('Đã gỡ tài nguyên khỏi Unit.');
    } catch (err) {
      setError(getContentManagerError(err, 'Không gỡ được tài nguyên.'));
    } finally {
      setWorking(false);
    }
  };

  const units = useMemo(() => instructorLedCourseDetail?.units || [], [instructorLedCourseDetail]);

  const toggleUnit = (unitId) => {
    setExpandedUnitIds((current) => {
      const next = new Set(current);
      if (next.has(unitId)) next.delete(unitId);
      else next.add(unitId);
      return next;
    });
  };

  const filteredUnits = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) return units;
    return units.filter((unit) => {
      const matchUnit = unit.title?.toLowerCase().includes(normalized)
        || unit.description?.toLowerCase().includes(normalized);
      const matchLesson = (unit.lessons || []).some((lesson) => (
        lesson.title?.toLowerCase().includes(normalized)
        || lesson.description?.toLowerCase().includes(normalized)
        || lesson.learningObjectives?.toLowerCase().includes(normalized)
        || String(lesson.sessionNumber).includes(normalized)
      ));
      return matchUnit || matchLesson;
    });
  }, [units, keyword]);

  const { page, setPage, totalPages, totalItems, pageItems } = usePagination(
    filteredUnits,
    UNIT_PAGE_SIZE,
    `${selectedInstructorLedCourseId}|${keyword}`,
  );

  const instructorLedCourseOptions = useMemo(() => [
    { label: 'Chọn khóa học...', value: '' },
    ...instructorLedCourses.map((p) => ({
      label: `${p.title} (${p.code || 'Chưa có mã'})`,
      value: String(p.id),
      description: `${p.examCategory || 'IELTS'} · ${p.units?.length || p.totalUnits || 0} Unit`,
    })),
  ], [instructorLedCourses]);

  const unitOptions = useMemo(() => [
    { label: 'Chọn Unit nhận tài nguyên', value: '' },
    ...units.map((u) => ({
      label: `Unit ${u.displayOrder ?? 0}: ${u.title}`,
      value: String(u.id),
    })),
  ], [units]);

  const currentAttachUnit = useMemo(() => {
    return units.find((u) => String(u.id) === String(attachForm.unitId)) || null;
  }, [units, attachForm.unitId]);

  const attachedResourceIds = useMemo(() => {
    if (!currentAttachUnit) return new Set();
    const typeKey = attachForm.type.toLowerCase() + 's';
    const list = currentAttachUnit[typeKey] || [];
    return new Set(list.map((ref) => String(ref.resourceId || ref.id)));
  }, [currentAttachUnit, attachForm.type]);

  const resourceOptions = useMemo(() => {
    const items = banks[attachForm.type.toLowerCase() + 's'] || [];
    const availableItems = items.filter((item) => !attachedResourceIds.has(String(item.id)));
    const placeholder = availableItems.length
      ? 'Tìm kiếm theo tên, kỹ năng hoặc loại bài...'
      : 'Không còn tài nguyên khả dụng (đã gắn hết vào Unit)';
    const descriptionFor = (item) => {
      if (attachForm.type === 'ASSESSMENT') {
        const skill = normalizeAssessmentKey(item.skill);
        const type = normalizeAssessmentKey(item.type);
        const skillLabel = formatAssessmentSkill(item.skill);
        const typeLabel = formatAssessmentType(item.type);
        const exam = item.examCategory ? String(item.examCategory) : '';
        return [skillLabel, skill, typeLabel, type, exam].filter(Boolean).join(' · ');
      }
      if (attachForm.type === 'MATERIAL') {
        return item.materialType || item.category || '';
      }
      if (attachForm.type === 'FLASHCARD') {
        return item.topic || item.level || '';
      }
      return '';
    };
    return [
      { label: placeholder, value: '' },
      ...availableItems.map((item) => ({
        label: item.title || item.name || `Tài nguyên #${item.id}`,
        value: String(item.id),
        description: descriptionFor(item),
      })),
    ];
  }, [banks, attachForm.type, attachedResourceIds]);

  const selectedResource = useMemo(() => {
    const items = banks[attachForm.type.toLowerCase() + 's'] || [];
    return items.find((item) => String(item.id) === String(attachForm.resourceId));
  }, [banks, attachForm.type, attachForm.resourceId]);

  return (
    <div className="space-y-5">
      <ManagementToast actionLabel="Thử lại" message={error} onAction={() => reloadAll()} onClose={() => setError('')} />
      <ManagementToast message={excelError} onClose={() => setExcelError('')} />
      <ManagementToast message={success} onClose={() => setSuccess('')} tone="success" title="Đã cập nhật khóa học" />

      {excelImportOpen ? (
        <InstructorLedCourseExcelImportModal
          examCategory={excelExamCategory}
          importing={excelImporting}
          onClose={closeExcelImport}
          onDownloadTemplate={downloadInstructorLedCourseExcelTemplate}
          onExamCategoryChange={setExcelExamCategory}
          onFileChange={readInstructorLedCourseExcel}
          onImport={importInstructorLedCourseFromExcel}
          parsed={parsedExcel}
          reading={excelReading}
        />
      ) : null}

      {instructorLedCourseCreatorOpen ? (
        <InstructorLedCourseModal
          form={instructorLedCourseForm}
          onChange={updateInstructorLedCourseForm}
          onClose={closeInstructorLedCourseCreator}
          onSubmit={createInstructorLedCourse}
          saving={working}
        />
      ) : null}

      {instructorLedCourseEditorOpen ? (
        <InstructorLedCourseModal
          form={instructorLedCourseForm}
          mode="edit"
          onChange={updateInstructorLedCourseForm}
          onClose={closeInstructorLedCourseEditor}
          onSubmit={updateInstructorLedCourse}
          saving={working}
        />
      ) : null}

      {!selectedInstructorLedCourseId ? (
        <InstructorLedCourseListPanel
          loading={loading}
          onArchive={archiveInstructorLedCourse}
          onClone={cloneInstructorLedCourse}
          onCreate={openInstructorLedCourseCreator}
          onEdit={openInstructorLedCourseEditor}
          onImport={() => { setExcelImportOpen(true); setExcelError(''); }}
          onOpen={openInstructorLedCourseWorkspace}
          onPublish={publishInstructorLedCourse}
          onRefresh={loadInstructorLedCourses}
          instructorLedCourses={instructorLedCourses}
        />
      ) : (
        <>
          <Panel className="overflow-hidden rounded-xl border-[#e9d7d6]/80 bg-white shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-4 border-b border-[#eef1f6] bg-[#fbf3f4] px-6 py-5">
              <div className="min-w-0">
                <button
                  className="mb-3 inline-flex items-center gap-2 rounded-lg border border-[#dfbfbd] bg-white px-3 py-1.5 text-xs font-bold text-[#730014] transition hover:bg-[#fff2f3] cursor-pointer"
                  onClick={closeInstructorLedCourseWorkspace}
                  type="button"
                >
                  <span aria-hidden="true">&larr;</span>
                  Quay lại danh sách khóa học
                </button>
                <div className="flex flex-wrap items-center gap-3">
                  <ManagerTaxonomyBadge kind="exam" value={instructorLedCourseDetail?.examCategory || 'IELTS'} />
                  <StatusPill status={instructorLedCourseDetail?.status} />
                </div>
                <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#26364a] sm:text-3xl">
                  {instructorLedCourseDetail?.title}
                </h2>
                <p className="mt-1 text-sm text-[#69778a]">
                  Mã: <span className="font-semibold text-[#26364a]">{instructorLedCourseDetail?.code || '-'}</span>
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <button
                  className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#dcc0bf]/50 bg-white px-3 text-xs font-bold text-[#4b0009] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95 cursor-pointer"
                  onClick={() => openInstructorLedCourseEditor(instructorLedCourseDetail)}
                  type="button"
                >
                  <Pencil className="h-3.5 w-3.5" /> Sửa thông tin
                </button>
                <button
                  className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-[#fffafb] px-3 text-xs font-bold text-[#730014] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95 cursor-pointer"
                  onClick={() => cloneInstructorLedCourse(instructorLedCourseDetail)}
                  type="button"
                >
                  <Copy className="h-3.5 w-3.5" /> Nhân bản
                </button>
                {instructorLedCourseDetail?.status === 'DRAFT' ? (
                  <button
                    className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg bg-[#730014] px-3 text-xs font-bold text-white whitespace-nowrap transition hover:bg-[#8a0018] active:scale-95 cursor-pointer"
                    disabled={working}
                    onClick={() => publishInstructorLedCourse(instructorLedCourseDetail)}
                    type="button"
                  >
                    <CheckCircle2 className="h-3.5 w-3.5" /> Xuất bản
                  </button>
                ) : null}
                <button
                  aria-label="Tải lại chi tiết khóa học"
                  className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-[#ecdedd] bg-white text-[#730014] transition hover:bg-[#fff2f3] cursor-pointer"
                  onClick={reloadAll}
                  type="button"
                >
                  <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                </button>
              </div>
            </div>

            <div className="grid gap-3 px-6 py-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
              <InfoTile label="Nhóm thi" value={<ManagerTaxonomyBadge kind="exam" value={instructorLedCourseDetail?.examCategory} />} />
              <InfoTile label="Target đầu ra" value={instructorLedCourseDetail?.targetBand ? `Band ${instructorLedCourseDetail.targetBand}` : (instructorLedCourseDetail?.targetScore ? `${instructorLedCourseDetail.targetScore} điểm` : '-')} />
              <InfoTile label="Điểm đầu vào" value={instructorLedCourseDetail?.entryLevel || '-'} />
              <InfoTile label="Số Unit" value={units.length} />
              <InfoTile label="Tổng bài học" value={`${countStructuredLessons(instructorLedCourseDetail?.units || units)} bài`} />
              <InfoTile label="Thời lượng" value={instructorLedCourseDetail?.durationLabel || '-'} />
            </div>
          </Panel>

          <div className="space-y-5">
            <Panel className="rounded-xl border-[#e9d7d6]/80 bg-white p-4 shadow-sm">
              <div className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_320px]">
                <div className="relative">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#c2acab]" />
                  <input
                    className="h-11 w-full rounded-lg border border-[#ecdedd] bg-[#fffafb] py-2 pl-10 pr-4 text-sm text-[#1a1c1c] outline-none transition focus:border-[#730014] focus:bg-white"
                    onChange={(event) => setKeyword(event.target.value)}
                    placeholder="Tìm Unit, bài học, tiêu đề hoặc mục tiêu học tập..."
                    value={keyword}
                  />
                </div>
                <BrandedSelect
                  buttonClassName="h-11 rounded-lg border-[#ecdedd] bg-white py-2 text-sm shadow-none"
                  onChange={(event) => selectInstructorLedCourse(event.target.value)}
                  options={instructorLedCourseOptions}
                  placeholder={loading ? 'Đang tải khóa học...' : 'Đổi sang khóa học khác'}
                  searchable={true}
                  value={selectedInstructorLedCourseId}
                />
              </div>
            </Panel>

            {unitEditorOpen && (
              <UnitEditorModal onClose={closeUnitEditor}>
                <section className="space-y-5">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Cấu trúc khóa học</p>
                      <h3 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
                        {editingUnitId ? 'Chỉnh sửa Unit' : 'Thêm Unit mới'}
                      </h3>
                    </div>
                    <button className="rounded-lg p-2 text-slate-400 hover:bg-slate-100" onClick={closeUnitEditor} type="button">
                      <X className="h-5 w-5" />
                    </button>
                  </div>
                  <div className="grid gap-4 lg:grid-cols-2">
                    <label className="block">
                      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Tên Unit</span>
                      <input className={FIELD_CLASS} onChange={(event) => setUnitForm({ ...unitForm, title: event.target.value })} required value={unitForm.title} />
                    </label>
                    <label className="block">
                      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Thứ tự hiển thị</span>
                      <input className={FIELD_CLASS} min="0" onChange={(event) => setUnitForm({ ...unitForm, displayOrder: event.target.value })} type="number" value={unitForm.displayOrder} />
                    </label>
                  </div>
                  <label className="block">
                    <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Mô tả Unit</span>
                    <RichTextEditor
                      helperText=""
                      onChange={(value) => setUnitForm({ ...unitForm, description: value })}
                      placeholder="Mô tả nội dung trọng tâm của Unit..."
                      size="compact"
                      value={unitForm.description}
                    />
                  </label>
                  <div className="flex justify-end gap-2 border-t border-[#dcc0bf]/20 pt-4">
                    <button className="rounded-xl border border-[#dfbfbd] px-4 py-2.5 text-sm font-bold text-[#730014]" onClick={closeUnitEditor} type="button">Hủy</button>
                    <button className="rounded-xl bg-[#4b0009] px-5 py-2.5 text-sm font-extrabold text-white disabled:opacity-60" disabled={working} onClick={saveUnit} type="button">
                      {working ? 'Đang lưu...' : 'Lưu Unit'}
                    </button>
                  </div>
                </section>
              </UnitEditorModal>
            )}

            {lessonEditorOpen && (
              <UnitEditorModal onClose={closeLessonEditor}>
                <form className="space-y-5" onSubmit={saveLesson}>
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Bài học trong khóa học</p>
                      <h3 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
                        {editingLessonId ? 'Chỉnh sửa bài học' : 'Thêm bài học mới'}
                      </h3>
                    </div>
                    <button aria-label="Đóng" className="rounded-lg p-2 text-slate-400 hover:bg-slate-100" onClick={closeLessonEditor} type="button"><X className="h-5 w-5" /></button>
                  </div>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <label className="block">
                      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Thứ tự trong unit</span>
                      <input className={FIELD_CLASS} min="1" onChange={(event) => setLessonForm((current) => ({ ...current, sessionNumber: event.target.value, displayOrder: event.target.value }))} required type="number" value={lessonForm.sessionNumber} />
                    </label>
                    <label className="block">
                      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Số buổi dự kiến</span>
                      <input className={FIELD_CLASS} min="1" onChange={(event) => setLessonForm((current) => ({ ...current, plannedSessionCount: event.target.value }))} required type="number" value={lessonForm.plannedSessionCount} />
                    </label>
                  </div>
                  <label className="block">
                    <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Tiêu đề bài học</span>
                    <input className={FIELD_CLASS} maxLength={220} onChange={(event) => setLessonForm((current) => ({ ...current, title: event.target.value }))} required value={lessonForm.title} />
                  </label>
                  <label className="block">
                    <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Mô tả bài học</span>
                    <RichTextEditor helperText="" onChange={(value) => setLessonForm((current) => ({ ...current, description: value }))} placeholder="Nội dung chính của bài học..." size="compact" value={lessonForm.description} />
                  </label>
                  <label className="block">
                    <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Mục tiêu học tập</span>
                    <RichTextEditor helperText="" onChange={(value) => setLessonForm((current) => ({ ...current, learningObjectives: value }))} placeholder="Mục tiêu người học cần đạt được..." size="compact" value={lessonForm.learningObjectives} />
                  </label>
                  <div className="flex justify-end gap-2 border-t border-[#dcc0bf]/20 pt-4">
                    <button className="rounded-xl border border-[#dfbfbd] px-4 py-2.5 text-sm font-bold text-[#730014]" onClick={closeLessonEditor} type="button">Hủy</button>
                    <button className="rounded-xl bg-[#4b0009] px-5 py-2.5 text-sm font-extrabold text-white disabled:opacity-60" disabled={working} type="submit">{working ? 'Đang lưu...' : 'Lưu bài học'}</button>
                  </div>
                </form>
              </UnitEditorModal>
            )}

            {resourcePanelOpen && (
              <ResourceAttachModal onClose={closeResourcePanel}>
                <form className="space-y-5" onSubmit={attachResource}>
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Học liệu & Bài tập</p>
                      <h3 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Thêm học liệu vào Unit</h3>
                    </div>
                    <button aria-label="Đóng" className="rounded-lg p-2 text-slate-400 hover:bg-slate-100" onClick={closeResourcePanel} type="button"><X className="h-5 w-5" /></button>
                  </div>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <FieldSelect label="Unit nhận học liệu" onChange={(value) => setAttachForm({ ...attachForm, unitId: value })} options={unitOptions} value={attachForm.unitId} />
                    <FieldSelect label="Loại học liệu" onChange={(value) => setAttachForm({ ...attachForm, type: value, resourceId: '' })} options={typeOptions} value={attachForm.type} />
                  </div>
                  <FieldSelect
                    disabled={!resourceOptions.length}
                    label="Chọn học liệu / bài tập từ ngân hàng"
                    onChange={(value) => setAttachForm({ ...attachForm, resourceId: value })}
                    options={resourceOptions}
                    placeholder={resourceOptions.length
                      ? 'Gõ để tìm theo tên, kỹ năng (Nghe/Đọc/Viết/Nói) hoặc loại bài...'
                      : 'Kho tài liệu này đang trống'}
                    searchable
                    value={attachForm.resourceId}
                  />
                  {selectedResource ? (
                    <div className="rounded-xl border border-[#dcc0bf]/40 bg-[#fff8f8] p-3.5">
                      <p className="text-xs font-bold text-[#4b0009]">{selectedResource.title}</p>
                      <p className="mt-1 text-xs text-[#8b706e]">{getReadableResourceText(selectedResource.description || selectedResource.prompt || selectedResource.materialType || 'Nội dung sẵn sàng')}</p>
                    </div>
                  ) : null}
                  <div className="flex justify-end gap-2 border-t border-[#dcc0bf]/20 pt-4">
                    <button className="rounded-xl border border-[#dfbfbd] px-4 py-2.5 text-sm font-bold text-[#730014]" onClick={closeResourcePanel} type="button">Hủy</button>
                    <button className="rounded-xl bg-[#4b0009] px-5 py-2.5 text-sm font-extrabold text-white disabled:opacity-60" disabled={working || !attachForm.resourceId} type="submit">{working ? 'Đang thêm...' : 'Thêm vào Unit'}</button>
                  </div>
                </form>
              </ResourceAttachModal>
            )}

            <Panel className="overflow-hidden rounded-xl border-[#e9d7d6]/80 bg-white shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#eef1f6] px-6 py-4">
                <div>
                  <h3 className="font-['Manrope'] text-lg font-extrabold text-[#26364a]">Cấu trúc khóa học</h3>
                  <p className="mt-0.5 text-xs text-[#8b706e]">{filteredUnits.length} Unit trong cấu trúc khóa học.</p>
                </div>
                <button className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg bg-[#4b0009] px-3.5 text-xs font-bold text-white whitespace-nowrap transition hover:bg-[#730014] active:scale-95" onClick={resetUnitForm} type="button">
                  <Plus className="h-3.5 w-3.5" /> Thêm Unit
                </button>
              </div>

              {loading ? (
                <div className="p-8 text-center text-sm font-semibold text-slate-500">Đang tải cấu trúc khóa học...</div>
              ) : filteredUnits.length === 0 ? (
                <div className="p-8 text-center text-sm font-semibold text-[#584140]">Khóa học này chưa có Unit nào. Hãy bấm "Thêm Unit" để bắt đầu biên soạn.</div>
              ) : (
                <>
                  <div className="space-y-5 p-5 sm:p-6">
                    {pageItems.map((unit) => {
                      const resourceCount = refGroups.reduce((total, group) => total + (unit[group.key]?.length || 0), 0);
                      const expanded = expandedUnitIds.has(unit.id);
                      const lessons = [...(unit.lessons || [])].sort((left, right) => (
                        Number(left.sessionNumber || 0) - Number(right.sessionNumber || 0)
                        || Number(left.displayOrder || 0) - Number(right.displayOrder || 0)
                      ));
                      return (
                        <article className="overflow-hidden rounded-xl border border-[#e9d7d6]/80 bg-white shadow-xs" key={unit.id}>
                          <div className="flex flex-col gap-4 bg-[#fbf3f4] px-5 py-4 sm:flex-row sm:items-start sm:justify-between">
                            <div className="min-w-0">
                              <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Unit {unit.displayOrder ?? 0}</p>
                              <h4 className="mt-1 font-['Manrope'] text-lg font-extrabold text-[#26364a]">{unit.title}</h4>
                              {unit.description ? <RichTextHtml asPlain className="mt-1.5 max-w-3xl text-xs leading-5 text-[#584140]" value={unit.description} /> : null}
                              <div className="mt-2.5 flex flex-wrap gap-2">
                                <span className="rounded-lg bg-white px-2.5 py-1 text-xs font-extrabold text-[#4b0009]">{lessons.length} bài học</span>
                                <span className="rounded-lg bg-white px-2.5 py-1 text-xs font-bold text-[#8b706e]">{resourceCount} tài nguyên</span>
                              </div>
                            </div>
                            <div className="flex shrink-0 flex-wrap gap-2">
                              <button
                                aria-expanded={expanded}
                                className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#dcc0bf]/50 bg-white px-3 text-xs font-bold text-[#4b0009] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95"
                                onClick={() => toggleUnit(unit.id)}
                                type="button"
                              >
                                <ChevronDown aria-hidden="true" className={`h-3.5 w-3.5 transition-transform ${expanded ? '' : '-rotate-90'}`} />
                                {expanded ? 'Thu gọn' : 'Mở rộng'}
                              </button>
                              <button className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#dcc0bf]/50 bg-white px-3 text-xs font-bold text-[#4b0009] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95" onClick={() => openEditUnit(unit)} type="button">Sửa Unit</button>
                              <button className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-[#fffafb] px-3 text-xs font-bold text-[#730014] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95" onClick={() => openResourcePanel(unit.id)} type="button">+ Thêm học liệu</button>
                              <button aria-label={`Xóa Unit ${unit.title}`} className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-rose-200 bg-white text-rose-700 transition hover:bg-rose-50 disabled:opacity-50 active:scale-95" disabled={working} onClick={() => deleteUnit(unit)} type="button"><Trash2 className="h-3.5 w-3.5" /></button>
                            </div>
                          </div>

                          {expanded ? <div className="space-y-3 border-t border-[#eef1f6] p-4 sm:p-5">
                            {lessons.length ? lessons.map((lesson) => (
                              <div className="ml-2 flex flex-col gap-3 border-l-2 border-[#dfbfbd] bg-[#fffdfd] py-3 pl-4 pr-3 sm:flex-row sm:items-start sm:justify-between" key={lesson.id}>
                                <div className="min-w-0">
                                  <div className="flex flex-wrap items-center gap-2">
                                    <p className="text-xs font-extrabold uppercase tracking-[0.12em] text-[#730014]">Bài {getLessonOrderInUnit(lesson)}</p>
                                    <span className="rounded-md bg-[#fbf3f4] px-2 py-0.5 text-[11px] font-bold text-[#730014]">
                                      {lesson.plannedSessionCount || 1} buổi dự kiến
                                    </span>
                                  </div>
                                  <h5 className="mt-1 text-sm font-extrabold text-[#26364a]">{lesson.title}</h5>
                                  {lesson.description ? <RichTextHtml asPlain className="mt-1 line-clamp-2 text-xs leading-5 text-[#584140]" value={lesson.description} /> : null}
                                  {lesson.learningObjectives ? <RichTextHtml asPlain className="mt-1 line-clamp-2 text-xs leading-5 text-slate-500" value={lesson.learningObjectives} /> : null}
                                </div>
                                <div className="flex shrink-0 gap-2">
                                  <button className="inline-flex h-7 shrink-0 items-center justify-center rounded-lg border border-[#dcc0bf]/50 bg-white px-2.5 text-xs font-bold text-[#4b0009] transition hover:bg-[#fff2f3] active:scale-95" onClick={() => openLessonEditor(unit, lesson)} type="button">Chỉnh sửa</button>
                                  <button aria-label={`Xóa Bài ${getLessonOrderInUnit(lesson)}`} className="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-lg border border-rose-200 bg-white text-rose-700 transition hover:bg-rose-50 disabled:opacity-50 active:scale-95" disabled={working} onClick={() => deleteLesson(unit, lesson)} type="button"><Trash2 className="h-3.5 w-3.5" /></button>
                                </div>
                              </div>
                            )) : <p className="px-3 py-2 text-xs text-slate-500">Chưa có bài học nào trong Unit này.</p>}
                            <button className="ml-2 inline-flex h-7 items-center gap-1.5 rounded-lg border border-dashed border-[#c99599] bg-white px-3 text-xs font-extrabold text-[#730014] hover:bg-[#fff7f7] active:scale-95" onClick={() => openLessonCreator(unit)} type="button">
                              <Plus className="h-3 w-3" /> Thêm bài học
                            </button>

                            <UnitResourceGroups onDetach={detachResource} unit={unit} working={working} />
                          </div> : null}
                        </article>
                      );
                    })}
                  </div>
                  <div className="border-t border-[#eef1f6] bg-[#fffafb]/25 px-6 py-4">
                    <Pagination page={page} pageSize={UNIT_PAGE_SIZE} totalItems={totalItems} totalPages={totalPages} onChange={setPage} />
                  </div>
                </>
              )}
            </Panel>
          </div>
        </>
      )}
    </div>
  );
}

function InfoTile({ label, value }) {
  return (
    <div className="rounded-xl border border-[#e9d7d6]/80 bg-[#fcfbfb] px-4 py-3">
      <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8b706e]">{label}</p>
      <p className="mt-1.5 break-words text-sm font-extrabold text-[#26364a]">{value}</p>
    </div>
  );
}

function FieldSelect({ label, value, options, onChange, placeholder, disabled, searchable = false }) {
  return (
    <div>
      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">{label}</span>
      <BrandedSelect
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        options={options}
        placeholder={placeholder}
        searchable={searchable}
        value={value}
      />
    </div>
  );
}

function FilterSelect({ compact = false, label, prefix, value, onChange, options }) {
  const normalized = options.map((option) => {
    const normalizedOption = typeof option === 'string' ? { label: option, value: option } : option;
    return {
      ...normalizedOption,
      label: compact && prefix ? `${prefix}: ${normalizedOption.label}` : normalizedOption.label,
    };
  });

  return (
    <div>
      {label ? <span className="mb-2 block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">{label}</span> : null}
      <BrandedSelect buttonClassName={compact ? 'h-11 rounded-lg border-[#ecdedd] bg-white py-2 text-sm shadow-none' : undefined} onChange={onChange} options={normalized} value={value} />
    </div>
  );
}

function FormSection({ children, number, title }) {
  return (
    <div className="rounded-2xl border border-[#e9d7d6]/80 bg-white p-5 sm:p-6 shadow-2xs">
      <div className="mb-5 flex items-center gap-3 border-b border-[#f0e4e5] pb-4">
        <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-[#4b0009] text-xs font-black text-white">
          {number}
        </span>
        <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">{title}</h3>
      </div>
      {children}
    </div>
  );
}

function InstructorLedCourseModal({ form, mode = 'create', onChange, onClose, onSubmit, saving }) {
  const editing = mode === 'edit';

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && !saving) onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose, saving]);

  return createPortal(
    <div className="fixed inset-0 z-50 flex min-h-0 items-center justify-center overflow-hidden p-4 sm:p-6 animate-fade-in" role="dialog" aria-modal="true">
      <button
        aria-label="Đóng modal"
        className="absolute inset-0 bg-[#1a0004]/55 backdrop-blur-sm"
        disabled={saving}
        onClick={onClose}
        type="button"
      />
      <div className="relative z-10 flex max-h-[calc(100dvh-2rem)] w-full max-w-[860px] min-h-0 flex-col overflow-hidden rounded-2xl bg-[#fffafb] shadow-2xl sm:max-h-[calc(100dvh-3rem)]">
        <form className="flex min-h-0 flex-1 flex-col" onSubmit={onSubmit}>
          {/* Header */}
          <div className="flex items-start justify-between border-b border-[#dcc0bf]/25 bg-white p-5 sm:p-6">
            <div>
              <span className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">
                Khóa học có giảng viên
              </span>
              <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
                {editing ? 'Chỉnh sửa thông tin khóa học' : 'Tạo khóa học mới'}
              </h2>
              <p className="mt-1 text-xs text-[#8b706e]">
                {editing
                  ? 'Cập nhật thông tin, đầu vào, mục tiêu và mô tả khóa học.'
                  : 'Thiết lập khung khóa học để bắt đầu xây dựng Unit, bài học và tài nguyên học tập.'}
              </p>
            </div>
            <button
              aria-label="Đóng modal"
              className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
              disabled={saving}
              onClick={onClose}
              type="button"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Form Body - Scrollable Sections */}
          <div className="min-h-0 flex-1 space-y-5 overflow-y-auto overscroll-contain p-5 sm:p-6">

            {/* Section 01: Thông tin khóa học */}
            <FormSection number="01" title="Thông tin khóa học">
              <div className="grid gap-4 md:grid-cols-2">
                <div className="md:col-span-2">
                  <label className="block">
                    <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
                      Tên khóa học <span className="text-rose-600">*</span>
                    </span>
                    <input
                      className={FIELD_CLASS}
                      onChange={(event) => onChange({ title: event.target.value })}
                      placeholder="Ví dụ: IELTS Master Speaking & Writing 6.5+"
                      required
                      value={form.title}
                    />
                  </label>
                </div>

                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
                    Mã khóa học
                  </span>
                  <input
                    className={FIELD_CLASS}
                    onChange={(event) => onChange({ code: event.target.value })}
                    placeholder="Để trống để tự sinh mã..."
                    value={form.code}
                  />
                </label>

                <FieldSelect
                  label="Danh mục / Nhóm thi"
                  onChange={(value) => onChange({ examCategory: value, ...getEnglishProfileDefaults(value) })}
                  options={ENGLISH_EXAM_OPTIONS}
                  value={form.examCategory}
                />

                <FieldSelect
                  label="Trạng thái khóa học"
                  onChange={(value) => onChange({ status: value })}
                  options={[
                    { label: 'Bản nháp (DRAFT)', value: 'DRAFT' },
                    { label: 'Đã xuất bản (PUBLISHED)', value: 'PUBLISHED' },
                    { label: 'Đã lưu trữ (ARCHIVED)', value: 'ARCHIVED' },
                  ]}
                  value={form.status || 'DRAFT'}
                />

                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
                    Thời gian hoàn thành dự kiến
                  </span>
                  <input
                    className={FIELD_CLASS}
                    onChange={(event) => onChange({ durationLabel: event.target.value })}
                    placeholder="Ví dụ: 24 buổi (48 giờ) · 3 tháng"
                    value={form.durationLabel}
                  />
                </label>
              </div>
            </FormSection>

            {/* Section 02: Đầu vào và kết quả */}
            <FormSection number="02" title="Đầu vào và kết quả">
              <div className="grid gap-4 md:grid-cols-2">
                {form.examCategory === 'IELTS' ? (
                  <>
                    <EnglishEntryLevelField
                      examCategory={form.examCategory}
                      onChange={(value) => onChange({ entryLevel: value })}
                      value={form.entryLevel}
                    />
                    <IeltsBandSelect
                      label="Band IELTS mục tiêu"
                      onChange={(value) => onChange({ targetBand: value })}
                      value={form.targetBand}
                    />
                  </>
                ) : null}

                {form.examCategory === 'TOEIC' ? (
                  <>
                    <EnglishEntryLevelField
                      examCategory={form.examCategory}
                      onChange={(value) => onChange({ entryLevel: value })}
                      value={form.entryLevel}
                    />
                    <ToeicScoreField
                      label="Điểm TOEIC mục tiêu"
                      onChange={(value) => onChange({ targetScore: value })}
                      value={form.targetScore}
                    />
                  </>
                ) : null}

                {form.examCategory === 'GENERAL_ENGLISH' ? (
                  <EnglishEntryLevelField
                    examCategory={form.examCategory}
                    onChange={(value) => onChange({ entryLevel: value })}
                    value={form.entryLevel}
                  />
                ) : null}

              </div>

              <div className="mt-4">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
                  Kỹ năng trọng tâm
                </span>
                <div className="flex flex-wrap gap-2">
                  {ENGLISH_SKILL_OPTIONS.map((skill) => {
                    const selected = form.focusSkills.includes(skill.value);
                    return (
                      <button
                        className={`rounded-full border px-3.5 py-1.5 text-xs font-bold transition cursor-pointer ${
                          selected
                            ? 'border-[#730014] bg-[#730014] text-white shadow-2xs'
                            : 'border-[#dcc0bf] bg-white text-[#584140] hover:bg-[#fff0f1]'
                        }`}
                        key={skill.value}
                        onClick={() => onChange({
                          focusSkills: selected
                            ? form.focusSkills.filter((value) => value !== skill.value)
                            : [...form.focusSkills, skill.value],
                        })}
                        type="button"
                      >
                        {skill.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="mt-4">
                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
                    Đầu ra / kết quả hoàn thành khóa học
                  </span>
                  <RichTextEditor
                    helperText=""
                    onChange={(value) => onChange({ outcomes: value })}
                    placeholder="Học viên đạt được gì sau khi hoàn thành khóa học..."
                    size="form"
                    value={form.outcomes}
                  />
                </label>
              </div>
            </FormSection>

            {/* Section 03: Giới thiệu khóa học */}
            <FormSection number="03" title="Giới thiệu khóa học">
              <div className="space-y-4">
                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
                    Mô tả ngắn
                  </span>
                  <RichTextEditor
                    helperText=""
                    onChange={(value) => onChange({ shortDescription: value })}
                    placeholder="Tóm tắt hấp dẫn về khóa học (hiển thị trên thẻ/catalog)..."
                    size="compact"
                    value={form.shortDescription}
                  />
                </label>

                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
                    Mô tả đầy đủ
                  </span>
                  <RichTextEditor
                    helperText=""
                    onChange={(value) => onChange({ description: value })}
                    placeholder="Mô tả chi tiết nội dung, đối tượng học viên, lộ trình..."
                    size="form"
                    value={form.description}
                  />
                </label>
              </div>
            </FormSection>

          </div>

          {/* Footer */}
          <div className="flex flex-wrap items-center justify-end gap-3 border-t border-[#dcc0bf]/20 bg-white p-5 sm:p-6">
            <button
              className="rounded-xl border border-[#dfbfbd] bg-white px-5 py-2.5 text-sm font-semibold text-[#4b0009] hover:bg-slate-50 transition disabled:opacity-50 cursor-pointer"
              disabled={saving}
              onClick={onClose}
              type="button"
            >
              Hủy
            </button>
            <button
              className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-6 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-[#730014] active:scale-95 transition disabled:opacity-60 cursor-pointer"
              disabled={saving}
              type="submit"
            >
              {editing ? <Save className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
              {saving ? 'Đang lưu...' : (editing ? 'Lưu thay đổi' : 'Tạo khóa học')}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body,
  );
}

function InstructorLedCourseListPanel({
  instructorLedCourses,
  loading,
  onArchive,
  onClone,
  onCreate,
  onEdit,
  onImport,
  onOpen,
  onPublish,
  onRefresh,
}) {
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [examFilter, setExamFilter] = useState('ALL');
  const [listPage, setListPage] = useState(1);

  const statusOptions = useMemo(() => {
    const values = [...new Set(instructorLedCourses.map((item) => item.status).filter(Boolean))];
    return [{ label: 'Tất cả', value: 'ALL' }, ...values.map((value) => ({ label: formatLabel(value), value }))];
  }, [instructorLedCourses]);

  const examOptions = useMemo(() => [
    { label: 'Tất cả', value: 'ALL' },
    { label: 'IELTS', value: 'IELTS' },
    { label: 'TOEIC', value: 'TOEIC' },
    { label: 'General English', value: 'GENERAL_ENGLISH' },
  ], []);

  const filteredInstructorLedCourses = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return instructorLedCourses.filter((item) => {
      const status = item.status || '';
      const examCategory = item.examCategory || '';
      const statusMatched = statusFilter === 'ALL' || status === statusFilter;
      const examMatched = examFilter === 'ALL' || examCategory === examFilter;
      const haystack = [
        item.title,
        item.code,
        item.examCategory,
        item.entryLevel,
        item.targetBand,
        item.targetScore,
      ].filter(Boolean).join(' ').toLowerCase();
      return statusMatched && examMatched && (!normalizedKeyword || haystack.includes(normalizedKeyword));
    });
  }, [instructorLedCourses, keyword, examFilter, statusFilter]);

  const totalItems = filteredInstructorLedCourses.length;
  const totalListPages = Math.max(1, Math.ceil(totalItems / PAGE_SIZE));
  const visibleInstructorLedCourses = filteredInstructorLedCourses.slice((listPage - 1) * PAGE_SIZE, listPage * PAGE_SIZE);

  useEffect(() => {
    setListPage(1);
  }, [keyword, examFilter, statusFilter]);

  useEffect(() => {
    if (listPage > totalListPages) setListPage(totalListPages);
  }, [listPage, totalListPages]);

  return (
    <div className="space-y-5">
      <HeaderActions>
        <button className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#730014] active:scale-[0.98]" onClick={onCreate} type="button">
          <Plus className="h-4 w-4" />
          Tạo khóa học mới
        </button>
        <button className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg border border-[#dfbfbd] bg-[#fff8f8] px-5 py-3 text-sm font-bold text-[#730014] shadow-sm transition hover:bg-[#fff0f1] active:scale-[0.98]" onClick={onImport} type="button">
          <FileSpreadsheet className="h-4 w-4" />
          Import từ Excel
        </button>
      </HeaderActions>

      <Panel className="rounded-xl border-[#e9d7d6]/80 bg-white p-4 shadow-sm">
        <div className="grid gap-3 xl:grid-cols-[minmax(320px,1fr)_180px_180px_44px]">
          <label className="block">
            <span className="relative block">
              <Filter className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#c2acab]" />
              <input
                className="h-11 w-full rounded-lg border border-[#ecdedd] bg-[#fffafb] py-2 pl-10 pr-4 text-sm text-[#1a1c1c] outline-none transition focus:border-[#730014] focus:bg-white"
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Tìm theo tiêu đề khóa học, mã hoặc mục tiêu..."
                value={keyword}
              />
            </span>
          </label>
          <FilterSelect compact onChange={(event) => setExamFilter(event.target.value)} options={examOptions} prefix="Nhóm thi" value={examFilter} />
          <FilterSelect compact onChange={(event) => setStatusFilter(event.target.value)} options={statusOptions} prefix="Trạng thái" value={statusFilter} />
          <div>
            <button
              aria-label="Làm mới danh sách khóa học"
              className="inline-flex h-11 w-11 items-center justify-center rounded-lg border border-[#ecdedd] bg-white text-[#730014] transition hover:bg-[#fff2f3]"
              onClick={onRefresh}
              type="button"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      </Panel>

      <Panel className="overflow-hidden rounded-xl border-[#e9d7d6]/80 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="min-w-[1040px] w-full text-left">
            <thead className="bg-[#fbf3f4] text-[11px] font-extrabold uppercase tracking-wider text-[#8e7371]">
              <tr>
                <th className="px-6 py-4 whitespace-nowrap">Khóa học</th>
                <th className="px-6 py-4 whitespace-nowrap">Nhóm thi & Target</th>
                <th className="px-6 py-4 text-center whitespace-nowrap">Cấu trúc</th>
                <th className="px-6 py-4 text-center whitespace-nowrap">Trạng thái</th>
                <th className="px-6 py-4 whitespace-nowrap">Cập nhật</th>
                <th className="px-6 py-4 text-right whitespace-nowrap">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#eef1f6]">
              {loading ? (
                Array.from({ length: PAGE_SIZE }).map((_, index) => (
                  <tr key={index}>
                    {Array.from({ length: 6 }).map((__, cellIndex) => (
                      <td key={cellIndex} className="px-6 py-5">
                        <div className="h-4 animate-pulse rounded bg-[#eef1f6]" />
                      </td>
                    ))}
                  </tr>
                ))
              ) : visibleInstructorLedCourses.length ? (
                visibleInstructorLedCourses.map((course) => (
                  <tr key={course.id} className="bg-white transition hover:bg-[#fbfdff]">
                    <td className="px-6 py-5">
                      <div className="min-w-[240px] max-w-[380px]">
                        <p className="overflow-hidden text-sm font-extrabold leading-5 text-[#26364a] [display:-webkit-box] [-webkit-box-orient:vertical] [-webkit-line-clamp:2]">
                          {course.title}
                        </p>
                        <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[#8b706e]">
                          <span className="font-semibold text-[#584140]">{course.code || '-'}</span>
                          {course.durationLabel ? (
                            <span className="text-[11px] text-slate-500">
                              · {course.durationLabel}
                            </span>
                          ) : null}
                        </div>
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-5">
                      <ManagerTaxonomyBadge kind="level" value={formatInstructorLedCourseExam(course)} />
                    </td>
                    <td className="px-6 py-5 text-center text-xs font-semibold text-[#0b1c30] whitespace-nowrap">
                      <span className="rounded-md bg-slate-100 px-2.5 py-1 font-bold text-[#0b1c30]">
                        {Number(course.totalUnits ?? course.units?.length ?? 0)} Unit · {course.totalLessons ?? countStructuredLessons(course.units || [])} bài học
                      </span>
                    </td>
                    <td className="px-6 py-5 text-center whitespace-nowrap"><StatusPill status={course.status} /></td>
                    <td className="px-6 py-5 text-xs text-[#69778a] whitespace-nowrap">{formatDate(course.updatedAt)}</td>
                    <td className="whitespace-nowrap px-6 py-4 text-right">
                      <div className="inline-flex items-center justify-end gap-1.5">
                        <button
                          className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg bg-[#4b0009] px-3 text-xs font-bold text-white whitespace-nowrap transition hover:bg-[#730014] active:scale-95"
                          onClick={() => onOpen(course)}
                          type="button"
                        >
                          <BookOpen className="h-3.5 w-3.5" />
                          Biên soạn
                        </button>
                        <button
                          className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#dcc0bf]/50 bg-white px-3 text-xs font-bold text-[#4b0009] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95"
                          onClick={() => onEdit(course)}
                          type="button"
                        >
                          <Pencil className="h-3.5 w-3.5" />
                          Chỉnh sửa
                        </button>
                        <button
                          className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-[#fffafb] px-3 text-xs font-bold text-[#730014] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95"
                          onClick={() => onClone(course)}
                          type="button"
                        >
                          <Copy className="h-3.5 w-3.5" />
                          Nhân bản
                        </button>
                        {course.status === 'DRAFT' ? (
                          <button
                            className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg bg-[#730014] px-3 text-xs font-bold text-white whitespace-nowrap transition hover:bg-[#8a0018] active:scale-95"
                            onClick={() => onPublish(course)}
                            type="button"
                          >
                            <CheckCircle2 className="h-3.5 w-3.5" />
                            Xuất bản
                          </button>
                        ) : null}
                        <button
                          aria-label={`Lưu trữ ${course.title}`}
                          className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-rose-200 bg-white text-rose-700 transition hover:bg-rose-50 active:scale-95"
                          onClick={() => onArchive(course)}
                          title="Lưu trữ"
                          type="button"
                        >
                          <Archive className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td className="px-6 py-10 text-center text-sm text-[#584140]" colSpan={6}>
                    Không có khóa học nào khớp với bộ lọc đã chọn.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="flex flex-col gap-3 border-t border-[#eef1f6] px-6 py-4 sm:flex-row sm:items-center sm:justify-between bg-[#fffafb]/25">
          <p className="text-sm text-[#69778a]">
            Hiển thị <span className="font-bold text-[#26364a]">{totalItems ? (listPage - 1) * PAGE_SIZE + 1 : 0} - {Math.min(listPage * PAGE_SIZE, totalItems)}</span> của <span className="font-bold text-[#26364a]">{totalItems}</span> khóa học
          </p>
          <Pagination
            onChange={setListPage}
            page={listPage}
            pageSize={PAGE_SIZE}
            totalItems={totalItems}
            totalPages={totalListPages}
          />
        </div>
      </Panel>
    </div>
  );
}

function UnitResourceGroups({ onDetach, unit, working }) {
  const total = refGroups.reduce((sum, group) => sum + (unit?.[group.key]?.length || 0), 0);
  if (!total) {
    return <p className="mt-2 text-xs font-medium text-slate-400">Chưa có tài nguyên nào được gắn vào Unit này.</p>;
  }

  return (
    <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      {refGroups.map((group) => {
        const references = unit?.[group.key] || [];
        if (!references.length) return null;
        const Icon = group.icon;
        return (
          <section className="overflow-hidden rounded-xl border border-[#dcc0bf]/35 bg-white p-3 shadow-xs" key={group.key}>
            <div className="flex items-center justify-between gap-2 border-b border-[#f0e4e2] pb-2">
              <div className="flex items-center gap-1.5">
                <Icon className="h-3.5 w-3.5 text-[#730014]" />
                <span className="text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">{group.title}</span>
              </div>
              <span className="rounded bg-[#fbf3f4] px-1.5 py-0.5 text-[10px] font-bold text-[#4b0009]">{references.length}</span>
            </div>
            <div className="mt-2 divide-y divide-[#f0e4e2]">
              {references.map((reference) => (
                <div className="flex items-start justify-between gap-2 py-2" key={`${reference.type}-${reference.id}`}>
                  <div className="min-w-0">
                    <p className="line-clamp-1 text-xs font-bold text-[#0b1c30]">{reference.title || `Tài nguyên #${reference.resourceId}`}</p>
                    <p className="mt-0.5 line-clamp-1 text-[11px] text-[#8b706e]">
                      {[reference.skill, getReadableResourceText(reference.subtitle)].filter(Boolean).join(' · ') || 'Tài nguyên liên kết'}
                    </p>
                  </div>
                  <button
                    aria-label={`Gỡ ${reference.title || 'tài nguyên'} khỏi unit`}
                    className="inline-flex h-6 w-6 shrink-0 items-center justify-center rounded text-rose-700 transition hover:bg-rose-50 disabled:opacity-50"
                    disabled={working}
                    onClick={() => onDetach(reference)}
                    title="Gỡ khỏi Unit"
                    type="button"
                  >
                    <Trash2 className="h-3 w-3" />
                  </button>
                </div>
              ))}
            </div>
          </section>
        );
      })}
    </div>
  );
}

function StatusPill({ status }) {
  const label = formatLabel(status);
  const normalized = String(status || '').toUpperCase();
  const tone = normalized === 'PUBLISHED'
    ? 'bg-emerald-100 text-emerald-700'
    : normalized === 'DRAFT'
      ? 'bg-amber-100 text-amber-700'
      : 'bg-slate-100 text-slate-700';

  return (
    <span className={`inline-flex whitespace-nowrap rounded-full px-3 py-1 text-[11px] font-bold ${tone}`}>
      {label}
    </span>
  );
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatLabel(value) {
  if (!value) return '-';
  const normalized = String(value).toUpperCase();
  const labels = {
    BEGINNER: 'Cơ bản',
    INTERMEDIATE: 'Trung cấp',
    ADVANCED: 'Nâng cao',
    DRAFT: 'Nháp',
    PUBLISHED: 'Đã xuất bản',
    ARCHIVED: 'Lưu trữ',
  };
  return labels[normalized] || (String(value).charAt(0) + String(value).slice(1).toLowerCase());
}

function formatInstructorLedCourseExam(course) {
  const examCategory = String(course?.examCategory || 'IELTS').toUpperCase();
  if (examCategory === 'IELTS' && course?.targetBand) return `IELTS ${course.targetBand}`;
  if (examCategory === 'TOEIC' && course?.targetScore) return `TOEIC ${course.targetScore}`;
  if (examCategory === 'GENERAL_ENGLISH') return course?.entryLevel ? `General English · ${course.entryLevel}` : 'General English';
  return course?.entryLevel ? `${examCategory} · ${course.entryLevel}` : examCategory;
}

function formatCurrency(amount) {
  if (amount == null || amount === '' || Number(amount) === 0) return '-';
  const num = Number(amount);
  if (isNaN(num)) return '-';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(num);
}

function UnitEditorModal({ children, onClose }) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose]);

  return createPortal(
    <div className="fixed inset-0 z-50 flex min-h-0 items-center justify-center overflow-hidden p-4 sm:p-6 animate-fade-in" role="dialog" aria-modal="true">
      <button
        aria-label="Đóng modal"
        className="absolute inset-0 bg-[#1a0004]/55 backdrop-blur-sm"
        onClick={onClose}
        type="button"
      />
      <div className="relative z-10 flex max-h-[calc(100dvh-2rem)] w-full max-w-[760px] min-h-0 flex-col overflow-hidden rounded-2xl border border-[#dcc0bf]/35 bg-white shadow-2xl sm:max-h-[calc(100dvh-3rem)]">
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-5 sm:p-6">{children}</div>
      </div>
    </div>,
    document.body,
  );
}

function ResourceAttachModal({ children, onClose }) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose]);

  return createPortal(
    <div className="fixed inset-0 z-50 flex min-h-0 items-center justify-center overflow-hidden p-4 sm:p-6 animate-fade-in" role="dialog" aria-modal="true">
      <button
        aria-label="Đóng modal"
        className="absolute inset-0 bg-[#1a0004]/55 backdrop-blur-sm"
        onClick={onClose}
        type="button"
      />
      <div className="relative z-10 flex max-h-[calc(100dvh-2rem)] w-full max-w-[760px] min-h-0 flex-col overflow-hidden rounded-2xl border border-[#dcc0bf]/35 bg-white shadow-2xl sm:max-h-[calc(100dvh-3rem)]">
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-5 sm:p-6">{children}</div>
      </div>
    </div>,
    document.body,
  );
}

function InstructorLedCourseExcelImportModal({
  examCategory,
  importing,
  onClose,
  onDownloadTemplate,
  onExamCategoryChange,
  onFileChange,
  onImport,
  parsed,
  reading,
}) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  return createPortal(
    <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 backdrop-blur-sm" role="dialog" aria-modal="true">
      <button aria-label="Đóng" className="absolute inset-0 bg-[#1a0004]/50" onClick={onClose} type="button" />
      <div className="relative z-10 max-h-[92dvh] w-full max-w-xl overflow-y-auto rounded-2xl border border-[#ead9db] bg-white p-6 shadow-2xl">
        <div className="flex items-start justify-between gap-4 border-b border-[#f1e4e5] pb-4">
          <div>
            <span className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Khởi tạo nhanh</span>
            <h3 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Import khóa học từ Excel</h3>
          </div>
          <button aria-label="Đóng" className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-slate-500 hover:bg-rose-50 hover:text-rose-700" onClick={onClose} type="button"><X className="h-4 w-4" /></button>
        </div>
        <div className="mt-4">
          <label className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Nhóm thi</label>
          <BrandedSelect onChange={(event) => onExamCategoryChange(event.target.value)} options={ENGLISH_EXAM_OPTIONS} value={examCategory} />
        </div>
        <div className="mt-4 flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-[#dfbfbd] bg-[#fffafb] p-6 text-center">
          <UploadCloud className="h-10 w-10 text-[#8a0018]" />
          <p className="mt-3 text-sm font-bold text-[#2b2828]">Chọn tệp Excel theo bản mẫu</p>
          <p className="mt-1 text-xs text-slate-400">Chỉ nhận định dạng .xlsx hoặc .xls</p>
          <label className="mt-4 cursor-pointer rounded-xl bg-[#4b0009] px-4 py-2 text-xs font-extrabold text-white transition hover:bg-[#730014]">
            Chọn tệp Excel
            <input accept=".xlsx,.xls" className="hidden" onChange={(event) => { onFileChange(event.target.files?.[0]); event.target.value = ''; }} type="file" />
          </label>
        </div>
        <button className="mt-3 inline-flex items-center gap-1.5 text-xs font-bold text-[#8a0018] hover:underline" onClick={onDownloadTemplate} type="button">
          <Download className="h-3.5 w-3.5" /> Tải bản mẫu Excel chuẩn (Mau_Import_Khoa_Hoc.xlsx)
        </button>
        {reading ? <div className="mt-4 flex items-center gap-2 rounded-xl bg-slate-50 p-3 text-xs font-semibold text-slate-600"><LoaderCircle className="h-4 w-4 animate-spin text-[#8a0018]" /> Đang đọc tệp Excel...</div> : null}
        {parsed ? (
          <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50/60 p-4 text-xs text-slate-700">
            <div className="flex items-center gap-2 font-extrabold text-emerald-800"><Check className="h-4 w-4 text-emerald-600" /> Đã đọc tệp: {parsed.fileName}</div>
            <div className="mt-2 space-y-1 pl-6">
              <p><strong>Tên khóa học:</strong> {parsed.title}</p>
              <p><strong>Số Unit:</strong> {parsed.units.length}</p>
              <p><strong>Tổng số bài học:</strong> {parsed.units.reduce((total, unit) => total + unit.lessons.length, 0)}</p>
            </div>
          </div>
        ) : null}
        <div className="mt-5 flex justify-end gap-2 border-t border-slate-100 pt-3">
          <button className="rounded-xl border border-[#dfbfbd] px-4 py-2 text-xs font-bold text-[#730014] hover:bg-slate-50" onClick={onClose} type="button">Hủy</button>
          <button className="rounded-xl bg-[#4b0009] px-5 py-2 text-xs font-extrabold text-white transition hover:bg-[#730014] disabled:opacity-60" disabled={!parsed || importing} onClick={onImport} type="button">
            {importing ? 'Đang tạo khóa học...' : 'Khởi tạo khóa học từ Excel'}
          </button>
        </div>
      </div>
    </div>,
    document.body,
  );
}

export { ContentManagerInstructorLedCoursesPage as ContentManagerSyllabusBuilderPage };
