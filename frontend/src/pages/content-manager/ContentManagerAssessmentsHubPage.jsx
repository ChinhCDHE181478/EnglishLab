import { useDeferredValue, useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useAppDialog } from '../../components/ui/AppDialog';
import {
  Archive,
  BookOpen,
  CheckCircle2,
  Clock3,
  Edit3,
  FileQuestion,
  Headphones,
  Mic2,
  NotebookPen,
  Plus,
  RefreshCw,
  Save,
  Search,
  X,
} from 'lucide-react';
import courseApi from '../../api/courseApi';
import curriculumApi from '../../api/curriculumApi';
import AssessmentExamBuilder from '../../components/content-manager/AssessmentExamBuilder';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import RichTextHtml from '../../components/content-manager/RichTextHtml';
import {
  ManagerEmptyState,
  ManagerFilterBar,
  ManagerStatsGrid,
  ManagerStatusBadge,
  ManagerTable,
  ManagerTablePagination,
} from '../../components/content-manager/ManagerListUi';
import ListeningPracticeWorkspace from '../../components/content-manager/skill-practice/ListeningPracticeWorkspace';
import ReadingPracticeWorkspace from '../../components/content-manager/skill-practice/ReadingPracticeWorkspace';
import SkillPracticeShell from '../../components/content-manager/skill-practice/SkillPracticeShell';
import SpeakingPracticeWorkspace from '../../components/content-manager/skill-practice/SpeakingPracticeWorkspace';
import ManagementToast from '../../components/ui/ManagementToast';
import WritingPracticeWorkspace from '../../components/content-manager/skill-practice/WritingPracticeWorkspace';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { usePagination } from '../../components/ui/Pagination';
import {
  ERROR_NOTICE_CLASS,
  FIELD_CLASS,
  PANEL_CLASS,
  PRIMARY_BUTTON_CLASS,
  SECONDARY_BUTTON_CLASS,
} from '../../utils/formStyles';
import { EMPTY_PAGE, pageParams } from '../../utils/pagination';

const strictSkill = (skill) => (item) => String(item.skill || '').toUpperCase() === skill;

const pageMap = {
  listening: {
    title: 'Luyện nghe',
    subtitle: 'Chỉ quản lý nội dung Listening. Bài tạo mới trong trang này luôn là bài nghe.',
    skill: 'LISTENING',
    type: 'LESSON_PRACTICE',
    allowedTypes: ['LESSON_PRACTICE', 'MODULE_TEST', 'QUIZ'],
    lockedSkill: true,
    createLabel: 'Tạo bài nghe',
    editLabel: 'Chỉnh sửa bài nghe',
    emptyLabel: 'Chưa có bài luyện nghe nào.',
    loadingLabel: 'Đang tải bài luyện nghe...',
    searchPlaceholder: 'Tìm bài nghe theo tiêu đề hoặc mô tả...',
    successNoun: 'bài nghe',
    tableTitle: 'Tên bài nghe',
    itemLabel: 'bài nghe',
    totalLabel: 'Bài nghe',
    statsIcon: Headphones,
    matcher: strictSkill('LISTENING'),
  },
  reading: {
    title: 'Luyện đọc',
    subtitle: 'Chỉ quản lý nội dung Reading. Bài tạo mới trong trang này luôn là bài đọc.',
    skill: 'READING',
    type: 'LESSON_PRACTICE',
    allowedTypes: ['LESSON_PRACTICE', 'MODULE_TEST', 'QUIZ'],
    lockedSkill: true,
    createLabel: 'Tạo bài đọc',
    editLabel: 'Chỉnh sửa bài đọc',
    emptyLabel: 'Chưa có bài luyện đọc nào.',
    loadingLabel: 'Đang tải bài luyện đọc...',
    searchPlaceholder: 'Tìm bài đọc theo tiêu đề hoặc mô tả...',
    successNoun: 'bài đọc',
    tableTitle: 'Tên bài đọc',
    itemLabel: 'bài đọc',
    totalLabel: 'Bài đọc',
    statsIcon: BookOpen,
    matcher: strictSkill('READING'),
  },
  writing: {
    title: 'Luyện viết',
    subtitle: 'Chỉ quản lý nội dung Writing. Đề tạo mới trong trang này luôn là đề viết.',
    skill: 'WRITING',
    type: 'WRITING_TASK',
    allowedTypes: ['WRITING_TASK', 'MODULE_TEST'],
    lockedSkill: true,
    createLabel: 'Tạo đề viết',
    editLabel: 'Chỉnh sửa đề viết',
    emptyLabel: 'Chưa có đề luyện viết nào.',
    loadingLabel: 'Đang tải đề luyện viết...',
    searchPlaceholder: 'Tìm đề viết theo tiêu đề hoặc mô tả...',
    successNoun: 'đề viết',
    tableTitle: 'Tên đề viết',
    itemLabel: 'đề viết',
    totalLabel: 'Đề viết',
    statsIcon: NotebookPen,
    matcher: strictSkill('WRITING'),
  },
  speaking: {
    title: 'Luyện nói',
    subtitle: 'Chỉ quản lý nội dung Speaking. Đề tạo mới trong trang này luôn là đề nói.',
    skill: 'SPEAKING',
    type: 'SPEAKING_TASK',
    allowedTypes: ['SPEAKING_TASK', 'MODULE_TEST'],
    lockedSkill: true,
    createLabel: 'Tạo đề nói',
    editLabel: 'Chỉnh sửa đề nói',
    emptyLabel: 'Chưa có đề luyện nói nào.',
    loadingLabel: 'Đang tải đề luyện nói...',
    searchPlaceholder: 'Tìm đề nói theo tiêu đề hoặc mô tả...',
    successNoun: 'đề nói',
    tableTitle: 'Tên đề nói',
    itemLabel: 'đề nói',
    totalLabel: 'Đề nói',
    statsIcon: Mic2,
    matcher: strictSkill('SPEAKING'),
  },
  mockExams: {
    title: 'Ngân hàng đề thi thử',
    subtitle: 'Tạo đề thi thử IELTS hoặc TOEIC theo kỹ năng, biên soạn trực quan không cần JSON.',
    skill: 'LISTENING',
    type: 'MOCK_TEST',
    createLabel: 'Tạo đề thi thử',
    editLabel: 'Chỉnh sửa đề thi thử',
    emptyLabel: 'Chưa có đề thi thử nào.',
    loadingLabel: 'Đang tải ngân hàng đề thi thử...',
    searchPlaceholder: 'Tìm đề thi thử theo tiêu đề, loại kỳ thi hoặc kỹ năng...',
    successNoun: 'đề thi thử',
    tableTitle: 'Tên đề',
    itemLabel: 'đề',
    totalLabel: 'Tổng đề',
    statsIcon: FileQuestion,
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

const examCategoryOptions = [
  { label: 'IELTS', value: 'IELTS' },
  { label: 'TOEIC', value: 'TOEIC' },
];

const ieltsMockSkillOptions = [
  { label: 'Nghe', value: 'LISTENING' },
  { label: 'Đọc', value: 'READING' },
  { label: 'Viết', value: 'WRITING' },
  { label: 'Nói', value: 'SPEAKING' },
];

const toeicMockSkillOptions = [
  { label: 'Listening', value: 'LISTENING' },
  { label: 'Reading', value: 'READING' },
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

const allOption = { label: 'Tất cả', value: 'ALL' };

const parseUiConfig = (value) => {
  try {
    const parsed = JSON.parse(String(value || ''));
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
};

const resolveExamCategory = (itemOrForm = {}) => {
  const fromField = String(itemOrForm.examCategory || '').toUpperCase();
  if (fromField === 'TOEIC' || fromField === 'IELTS') return fromField;
  const config = parseUiConfig(itemOrForm.uiConfigJson);
  if (String(config.examType || '').toUpperCase() === 'TOEIC') return 'TOEIC';
  if (String(config.type || '').toLowerCase().startsWith('toeic_')) return 'TOEIC';
  if (/\bTOEIC\b/i.test(String(itemOrForm.title || ''))) return 'TOEIC';
  return 'IELTS';
};

const withExamTypeInConfig = (uiConfigJson, examCategory, skill) => {
  const config = parseUiConfig(uiConfigJson);
  const normalized = examCategory === 'TOEIC' ? 'TOEIC' : 'IELTS';
  config.examType = normalized;
  if (['LISTENING', 'READING'].includes(String(skill || '').toUpperCase())) {
    if (normalized === 'TOEIC') {
      config.type = skill === 'READING' ? 'toeic_reading_exam' : 'toeic_listening_exam';
    } else if (!String(config.type || '').startsWith('ielts_') && !String(config.type || '').startsWith('speaking') && !String(config.type || '').startsWith('writing')) {
      config.type = skill === 'READING' ? 'ielts_reading_exam' : 'ielts_listening_exam';
    }
  }
  return JSON.stringify(config);
};

const emptyForm = (pageConfig) => {
  const isMock = pageConfig?.type === 'MOCK_TEST';
  const skill = pageConfig?.skill || 'LISTENING';
  const examCategory = isMock ? 'IELTS' : 'IELTS';
  return {
    title: '',
    description: '',
    type: pageConfig?.type || 'LESSON_PRACTICE',
    skill,
    examCategory,
    aiEvaluationMode: ['WRITING', 'SPEAKING'].includes(skill) ? 'RUBRIC_FEEDBACK' : 'EXPLAIN_ONLY',
    rubricId: '',
    instructions: '',
    objectiveAnswerKey: '',
    uiConfigJson: isMock ? withExamTypeInConfig('{}', examCategory, skill) : '',
    passingScore: '',
    maxScore: 100,
    timeLimitMinutes: '',
    status: 'DRAFT',
  };
};

const toForm = (item = {}, pageConfig) => {
  const examCategory = resolveExamCategory(item);
  return {
    title: item.title || '',
    description: item.description || '',
    type: item.type || pageConfig?.type || 'LESSON_PRACTICE',
    skill: item.skill || pageConfig?.skill || 'LISTENING',
    examCategory,
    aiEvaluationMode: item.aiEvaluationMode || (['WRITING', 'SPEAKING'].includes(item.skill) ? 'RUBRIC_FEEDBACK' : 'EXPLAIN_ONLY'),
    rubricId: item.rubric?.id ? String(item.rubric.id) : '',
    instructions: item.instructions || '',
    objectiveAnswerKey: item.objectiveAnswerKey || '',
    uiConfigJson: item.uiConfigJson || '',
    passingScore: item.passingScore ?? '',
    maxScore: item.maxScore ?? 100,
    timeLimitMinutes: item.timeLimitMinutes ?? '',
    status: item.status || 'DRAFT',
  };
};

const supportedBuilderSkills = new Set(['LISTENING', 'READING', 'WRITING', 'SPEAKING']);

export default function ContentManagerAssessmentsHubPage({ pageKey }) {
  const { confirm: confirmDialog } = useAppDialog();
  const pageConfig = pageMap[pageKey] || pageMap.listening;
  const isSkillLocked = Boolean(pageConfig.lockedSkill);
  const isMockExamsPage = pageKey === 'mockExams';
  const [items, setItems] = useState([]);
  const [rubrics, setRubrics] = useState([]);
  const [form, setForm] = useState(() => emptyForm(pageConfig));
  const [editingId, setEditingId] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [filters, setFilters] = useState({ type: 'ALL', status: 'ALL', examCategory: 'ALL' });
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [pageResult, setPageResult] = useState(EMPTY_PAGE);
  const [statsData, setStatsData] = useState({ total: 0, published: 0, draft: 0, timed: 0 });
  const editorRef = useRef(null);
  const deferredKeyword = useDeferredValue(keyword);
  const resetKey = `${pageKey}-${deferredKeyword}-${filters.type}-${filters.status}-${filters.examCategory}`;
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    pageResult.content,
    8,
    resetKey,
    pageResult,
  );

  const lockFormToPage = (draft) => (
    isSkillLocked ? { ...draft, skill: pageConfig.skill, type: pageConfig.type } : draft
  );

  const loadItems = async () => {
    setLoading(true);
    setError('');
    try {
      const baseParams = isSkillLocked ? { skill: pageConfig.skill } : { type: pageConfig.type };
      const params = {
        ...baseParams,
        type: !isSkillLocked && filters.type !== 'ALL' ? filters.type : baseParams.type,
        status: filters.status === 'ALL' ? undefined : filters.status,
        examCategory: isMockExamsPage && filters.examCategory !== 'ALL'
          ? filters.examCategory
          : undefined,
        keyword: deferredKeyword.trim() || undefined,
        sort: ['updatedAt,desc', 'title,asc'],
      };
      const [data, summary, rubricItems] = await Promise.all([
        curriculumApi.getAssessmentBankPage(pageParams(page, 8, params)),
        curriculumApi.getAssessmentBankStats(baseParams),
        courseApi.getManagedAssessmentRubrics(),
      ]);
      setPageResult(data);
      setItems(data.content);
      setStatsData(summary);
      setRubrics(rubricItems);
    } catch (err) {
      setError(err?.response?.data?.message || `Không tải được ${pageConfig.successNoun}.`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadItems();
  }, [deferredKeyword, filters.examCategory, filters.status, filters.type, page, pageKey]);

  useEffect(() => {
    setEditingId(null);
    setEditorOpen(false);
    setKeyword('');
    setFilters({ type: 'ALL', status: 'ALL', examCategory: 'ALL' });
    setForm(emptyForm(pageConfig));
  }, [pageKey]);
  const stats = useMemo(() => [
    { label: pageConfig.totalLabel, value: statsData.total, icon: pageConfig.statsIcon, tone: 'text-[#4b0009]' },
    { label: 'Đã xuất bản', value: statsData.published, icon: CheckCircle2, tone: 'text-emerald-700' },
    { label: 'Bản nháp', value: statsData.draft, icon: Edit3, tone: 'text-amber-700' },
    { label: 'Có thời lượng', value: statsData.timed, icon: Clock3, tone: 'text-[#005236]' },
  ], [pageConfig, statsData]);

  const updateForm = (field, value) => {
    if (isSkillLocked && (field === 'skill' || field === 'type')) return;
    setForm((current) => {
      let next = lockFormToPage({ ...current, [field]: value });
      if (isMockExamsPage && field === 'examCategory') {
        const examCategory = value === 'TOEIC' ? 'TOEIC' : 'IELTS';
        const skill = examCategory === 'TOEIC' && !['LISTENING', 'READING'].includes(String(current.skill || '').toUpperCase())
          ? 'LISTENING'
          : current.skill;
        next = {
          ...next,
          examCategory,
          skill,
          type: 'MOCK_TEST',
          uiConfigJson: withExamTypeInConfig(current.uiConfigJson, examCategory, skill),
        };
      }
      if (isMockExamsPage && field === 'skill') {
        next = {
          ...next,
          uiConfigJson: withExamTypeInConfig(current.uiConfigJson, current.examCategory || 'IELTS', value),
        };
      }
      if (isMockExamsPage && field === 'uiConfigJson') {
        next = {
          ...next,
          uiConfigJson: withExamTypeInConfig(value, current.examCategory || 'IELTS', current.skill),
        };
      }
      if (field === 'skill') {
        next.rubricId = '';
        next.aiEvaluationMode = ['WRITING', 'SPEAKING'].includes(value) ? 'RUBRIC_FEEDBACK' : 'EXPLAIN_ONLY';
      }
      return next;
    });
  };

  const startNew = () => {
    setEditingId(null);
    setForm(lockFormToPage(emptyForm(pageConfig)));
    setEditorOpen(true);
    setError('');
    setSuccess('');
    window.setTimeout(() => {
      editorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 0);
  };

  const closeEditor = () => {
    setEditingId(null);
    setForm(lockFormToPage(emptyForm(pageConfig)));
    setEditorOpen(false);
    setError('');
    setSuccess('');
  };

  const itemBelongsToPage = (item) => !isSkillLocked || String(item.skill || '').toUpperCase() === pageConfig.skill;

  const openEdit = (item) => {
    if (!itemBelongsToPage(item)) {
      setError('Nội dung này không thuộc kỹ năng hiện tại. Vui lòng mở đúng trang kỹ năng để chỉnh sửa.');
      setSuccess('');
      setEditorOpen(false);
      return;
    }
    setEditingId(item.id);
    setForm(lockFormToPage(toForm(item, pageConfig)));
    setEditorOpen(true);
    setError('');
    setSuccess('');
    window.setTimeout(() => {
      editorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 0);
  };

  const buildPayload = (draft) => {
    const lockedDraft = lockFormToPage(draft);
    const examCategory = isMockExamsPage ? resolveExamCategory(lockedDraft) : null;
    return {
      ...lockedDraft,
      uiConfigJson: isMockExamsPage
        ? withExamTypeInConfig(lockedDraft.uiConfigJson, examCategory, lockedDraft.skill)
        : lockedDraft.uiConfigJson,
      passingScore: lockedDraft.passingScore === '' ? null : Number(lockedDraft.passingScore),
      maxScore: lockedDraft.maxScore === '' ? null : Number(lockedDraft.maxScore),
      timeLimitMinutes: lockedDraft.timeLimitMinutes === '' ? null : Number(lockedDraft.timeLimitMinutes),
    };
  };

  const saveItem = async () => {
    if (!form.title.trim()) {
      setError(isSkillLocked ? `Vui lòng nhập tên ${pageConfig.successNoun}.` : 'Vui lòng nhập tên đề.');
      return;
    }
    if (form.uiConfigJson) {
      try {
        JSON.parse(form.uiConfigJson);
      } catch {
        setError('Cấu hình nội dung phải là JSON hợp lệ.');
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

    const payload = buildPayload(form);
    if (isSkillLocked && String(payload.skill || '').toUpperCase() !== pageConfig.skill) {
      setError('Kỹ năng của payload không khớp trang hiện tại. Vui lòng tải lại trang và thử lại.');
      return;
    }
    if (payload.type === 'MODULE_TEST' && ['WRITING', 'SPEAKING'].includes(payload.skill)) {
      if (payload.aiEvaluationMode === 'NONE') {
        setError('Module Test Viết/Nói phải bật chấm bằng AI.');
        return;
      }
      if (!payload.rubricId) {
        setError('Module Test Viết/Nói phải chọn bộ tiêu chí chấm.');
        return;
      }
    }

    setWorking(true);
    setError('');
    setSuccess('');
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
      setForm(lockFormToPage(toForm(saved, pageConfig)));
      setEditorOpen(false);
      setKeyword('');
      setFilters({ type: 'ALL', status: 'ALL' });
      setPage(1);
      setSuccess(editingId ? `Đã cập nhật ${pageConfig.successNoun}.` : `Đã tạo ${pageConfig.successNoun}.`);
    } catch (err) {
      setError(err?.response?.data?.message || `Không lưu được ${pageConfig.successNoun}.`);
    } finally {
      setWorking(false);
    }
  };

  const archiveItem = async (item) => {
    if (!itemBelongsToPage(item)) {
      setError('Nội dung này không thuộc kỹ năng hiện tại. Vui lòng mở đúng trang kỹ năng để chỉnh sửa.');
      setSuccess('');
      return;
    }
    if (!await confirmDialog(`Lưu trữ ${pageConfig.successNoun} “${item.title}”?`, {
      title: `Lưu trữ ${pageConfig.successNoun}`,
      confirmLabel: 'Lưu trữ',
      tone: 'danger',
    })) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await curriculumApi.archiveAssessmentBankItem(item.id);
      setItems((current) => current.map((row) => (
        String(row.id) === String(item.id) ? { ...row, status: 'ARCHIVED' } : row
      )));
      if (String(editingId) === String(item.id)) updateForm('status', 'ARCHIVED');
      setSuccess(`Đã lưu trữ ${pageConfig.successNoun}.`);
    } catch (err) {
      setError(err?.response?.data?.message || `Không lưu trữ được ${pageConfig.successNoun}.`);
    } finally {
      setWorking(false);
    }
  };

  const restoreItem = async (item) => {
    if (!itemBelongsToPage(item)) {
      setError('Nội dung này không thuộc kỹ năng hiện tại. Vui lòng mở đúng trang kỹ năng để chỉnh sửa.');
      setSuccess('');
      return;
    }
    if (!await confirmDialog(`Khôi phục ${pageConfig.successNoun} “${item.title}” về bản nháp?`, {
      title: `Khôi phục ${pageConfig.successNoun}`,
      confirmLabel: 'Khôi phục',
    })) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const saved = await curriculumApi.updateAssessmentBankItem(item.id, buildPayload({
        ...toForm(item, pageConfig),
        status: 'DRAFT',
      }));
      setItems((current) => current.map((row) => (
        String(row.id) === String(saved.id) ? saved : row
      )));
      if (String(editingId) === String(item.id)) setForm(lockFormToPage(toForm(saved, pageConfig)));
      setSuccess(`Đã khôi phục ${pageConfig.successNoun} về bản nháp.`);
    } catch (err) {
      setError(err?.response?.data?.message || `Không khôi phục được ${pageConfig.successNoun}.`);
    } finally {
      setWorking(false);
    }
  };

  const publishItem = async (item) => {
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const saved = await curriculumApi.updateAssessmentBankItem(item.id, buildPayload({
        ...toForm(item, pageConfig),
        status: 'PUBLISHED',
      }));
      setItems((current) => current.map((row) => (String(row.id) === String(saved.id) ? saved : row)));
      if (String(editingId) === String(saved.id)) setForm(lockFormToPage(toForm(saved, pageConfig)));
      setSuccess(`Đã xuất bản ${pageConfig.successNoun}.`);
    } catch (err) {
      setError(err?.response?.data?.message || `Không xuất bản được ${pageConfig.successNoun}.`);
    } finally {
      setWorking(false);
    }
  };

  const canUseBuilder = supportedBuilderSkills.has(String(form.skill || '').toUpperCase());
  const lockedForm = lockFormToPage(form);
  const mockSkillOptions = (form.examCategory || 'IELTS') === 'TOEIC' ? toeicMockSkillOptions : ieltsMockSkillOptions;

  const renderWorkspace = () => {
    if (isSkillLocked && pageConfig.skill === 'LISTENING') {
      return <ListeningPracticeWorkspace form={lockedForm} onChange={updateForm} />;
    }
    if (isSkillLocked && pageConfig.skill === 'READING') {
      return <ReadingPracticeWorkspace form={lockedForm} onChange={updateForm} />;
    }
    if (isSkillLocked && pageConfig.skill === 'WRITING') {
      return <WritingPracticeWorkspace form={lockedForm} onChange={updateForm} />;
    }
    if (isSkillLocked && pageConfig.skill === 'SPEAKING') {
      return <SpeakingPracticeWorkspace form={lockedForm} onChange={updateForm} />;
    }
    if (canUseBuilder) {
      return (
        <AssessmentExamBuilder
          assessment={{
            ...form,
            examType: isMockExamsPage ? (form.examCategory || 'IELTS') : undefined,
          }}
          onChange={updateForm}
        />
      );
    }
    return (
      <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm leading-6 text-amber-900">
        Chọn kỹ năng Nghe, Đọc, Viết hoặc Nói để biên soạn đề trực quan.
      </div>
    );
  };

  const renderFilters = () => (
    <>
      <div className="w-full min-w-0 flex-1 sm:min-w-[300px]">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-[#897270]" />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder={pageConfig.searchPlaceholder}
            className="w-full rounded-lg border border-[#dcc0bf]/50 bg-[#f8f9ff] py-2 pl-10 pr-4 text-sm text-[#0b1c30] outline-none transition focus:border-[#4b0009] focus:bg-white focus:ring-4 focus:ring-[#4b0009]/5"
          />
        </div>
      </div>
      <div className={`grid w-full gap-3 ${isSkillLocked ? 'sm:w-auto' : isMockExamsPage ? 'sm:grid-cols-2 lg:grid-cols-3 lg:w-auto' : 'sm:grid-cols-2 lg:w-auto'}`}>
        {!isSkillLocked && !isMockExamsPage ? (
          <FilterSelect label="Loại đề" onChange={(event) => setFilters((current) => ({ ...current, type: event.target.value }))} options={[allOption, ...typeOptions]} value={filters.type} />
        ) : null}
        {isMockExamsPage ? (
          <FilterSelect label="Kỳ thi" onChange={(event) => setFilters((current) => ({ ...current, examCategory: event.target.value }))} options={[allOption, ...examCategoryOptions]} value={filters.examCategory} />
        ) : null}
        <FilterSelect label="Trạng thái" onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))} options={[allOption, ...statusOptions]} value={filters.status} />
      </div>
      <button
        aria-label="Làm mới danh sách"
        className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#dcc0bf]/40 text-[#564241] transition hover:bg-[#eff4ff]"
        onClick={loadItems}
        type="button"
      >
        <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
      </button>
      {!isSkillLocked ? (
        <button type="button" onClick={startNew} className="inline-flex items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#730014]">
          <Plus className="h-4 w-4" />
          {pageConfig.createLabel}
        </button>
      ) : null}
    </>
  );

  const renderTable = () => {
    if (loading) {
      return <div className="rounded-xl border border-[#dcc0bf]/30 bg-white p-6 text-sm font-semibold text-slate-500">{pageConfig.loadingLabel}</div>;
    }
    if (pageItems.length === 0) {
      return <ManagerEmptyState>{pageConfig.emptyLabel}</ManagerEmptyState>;
    }

    const columns = isSkillLocked
      ? [
        { label: pageConfig.tableTitle, key: 'title' },
        { label: 'Thời lượng', key: 'time', align: 'center' },
        { label: 'Trạng thái', key: 'status' },
        { label: 'Thao tác', key: 'actions', align: 'right' },
      ]
      : isMockExamsPage
        ? [
          { label: pageConfig.tableTitle, key: 'title' },
          { label: 'Kỳ thi', key: 'exam' },
          { label: 'Kỹ năng', key: 'skill' },
          { label: 'Thời lượng', key: 'time', align: 'center' },
          { label: 'Trạng thái', key: 'status' },
          { label: 'Thao tác', key: 'actions', align: 'right' },
        ]
        : [
          { label: pageConfig.tableTitle, key: 'title' },
          { label: 'Loại đề', key: 'type' },
          { label: 'Kỹ năng', key: 'skill' },
          { label: 'Thời lượng', key: 'time', align: 'center' },
          { label: 'Trạng thái', key: 'status' },
          { label: 'Thao tác', key: 'actions', align: 'right' },
        ];

    return (
      <section className="overflow-hidden rounded-xl border border-[#dcc0bf]/30 bg-white shadow-sm">
        <ManagerTable columns={columns} minWidth={isSkillLocked ? '900px' : '1080px'}>
          {pageItems.map((item) => (
            <tr className="transition hover:bg-[#eff4ff]" key={item.id}>
              <td className="px-6 py-5">
                <p className="max-w-[360px] overflow-hidden text-sm font-bold leading-5 text-[#4b0009] [display:-webkit-box] [-webkit-box-orient:vertical] [-webkit-line-clamp:2]">{item.title}</p>
                {item.description ? <RichTextHtml asPlain className="mt-1 max-w-[360px] truncate text-xs text-[#564241]" value={item.description} /> : null}
              </td>
              {!isSkillLocked ? (
                <>
                  {isMockExamsPage ? (
                    <td className="px-6 py-5"><ManagerStatusBadge tone="info">{resolveExamCategory(item)}</ManagerStatusBadge></td>
                  ) : (
                    <td className="px-6 py-5 text-sm text-[#0b1c30]">{formatLabel(item.type)}</td>
                  )}
                  <td className="px-6 py-5"><ManagerStatusBadge tone="info">{formatLabel(item.skill)}</ManagerStatusBadge></td>
                </>
              ) : null}
              <td className="px-6 py-5 text-center text-sm font-semibold text-[#0b1c30]">{item.timeLimitMinutes ? `${item.timeLimitMinutes} phút` : '-'}</td>
              <td className="px-6 py-5"><AssessmentStatusBadge status={item.status} /></td>
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
                  {item.status === 'ARCHIVED' ? (
                    <button
                      className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-[#4b0009] px-4 py-1.5 text-xs font-bold text-white transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-45"
                      disabled={working}
                      onClick={() => restoreItem(item)}
                      type="button"
                    >
                      <RefreshCw className="h-3.5 w-3.5" />
                      Khôi phục
                    </button>
                  ) : item.status === 'DRAFT' ? (
                    <button
                      className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-[#4b0009] px-4 py-1.5 text-xs font-bold text-white transition hover:bg-[#730014] disabled:opacity-45"
                      disabled={working}
                      onClick={() => publishItem(item)}
                      type="button"
                    >
                      <CheckCircle2 className="h-3.5 w-3.5" />
                      Xuất bản
                    </button>
                  ) : item.status === 'PUBLISHED' ? (
                    <button
                      className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-[#4b0009] px-4 py-1.5 text-xs font-bold text-white transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-45"
                      disabled={working}
                      onClick={() => archiveItem(item)}
                      type="button"
                    >
                      <Archive className="h-3.5 w-3.5" />
                      Lưu trữ
                    </button>
                  ) : null}
                </div>
              </td>
            </tr>
          ))}
        </ManagerTable>
        <ManagerTablePagination itemLabel={pageConfig.itemLabel} onChange={setPage} page={page} pageSize={8} totalItems={totalItems} totalPages={totalPages} />
      </section>
    );
  };

  return (
    <div className="space-y-6">
      {!editorOpen ? <ManagementToast message={error} onClose={() => setError('')} /> : null}
      <ManagementToast message={success} onClose={() => setSuccess('')} tone="success" title="Đã cập nhật nội dung" />

      {editorOpen && (
        <AssessmentHubModal onClose={closeEditor}>
          <div className="flex shrink-0 items-start justify-between gap-4 border-b border-[#f0e3e4] px-6 py-5 bg-white">
            <div>
              <p className="text-[12px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">
                {pageConfig.title || 'Biên soạn bài luyện tập'}
              </p>
              <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
                {editingId ? pageConfig.editLabel : pageConfig.createLabel}
              </h2>
              <p className="mt-1 text-xs text-[#8b706e]">
                {isSkillLocked
                  ? `Trang này chỉ lưu ${pageConfig.successNoun} với kỹ năng ${formatLabel(pageConfig.skill)}.`
                  : 'Nội dung tạo ở đây sẽ nằm trong ngân hàng dùng chung, sau đó có thể gắn vào nhiều khóa học hoặc giáo trình.'}
              </p>
            </div>
            <button className="rounded-2xl border border-[#dfbfbd]/65 p-2.5 text-[#730014] transition hover:bg-[#fff2f3]" onClick={closeEditor} type="button">
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-6" ref={editorRef}>
            {error ? <div className={`${ERROR_NOTICE_CLASS} mb-5`} role="alert">{error}</div> : null}
            <div className="grid gap-6 xl:grid-cols-[minmax(0,440px)_1fr]">
              <div className="space-y-4">
                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Tên {pageConfig.successNoun}</span>
                  <input value={form.title} onChange={(event) => updateForm('title', event.target.value)} className={FIELD_CLASS} />
                </label>
                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Mô tả</span>
                  <RichTextEditor
                    helperText=""
                    onChange={(value) => updateForm('description', value)}
                    placeholder="Mô tả đề luyện tập..."
                    size="compact"
                    value={form.description}
                  />
                </label>
                {isSkillLocked ? (
                  <div className="grid gap-3 md:grid-cols-2">
                    <LockedMeta label="Trang kỹ năng" value={pageConfig.title} />
                    <LockedMeta label="Dạng nội dung" value={formatLabel(pageConfig.type)} />
                  </div>
                ) : isMockExamsPage ? (
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Kỳ thi</span>
                      <BrandedSelect value={form.examCategory || 'IELTS'} onChange={(event) => updateForm('examCategory', event.target.value)} options={examCategoryOptions} />
                    </div>
                    <div>
                      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Kỹ năng</span>
                      <BrandedSelect value={form.skill} onChange={(event) => updateForm('skill', event.target.value)} options={mockSkillOptions} />
                    </div>
                  </div>
                ) : (
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
                )}
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
                {['WRITING', 'SPEAKING'].includes(form.skill) ? (
                  <div>
                    <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Bộ tiêu chí chấm</span>
                    <BrandedSelect
                      value={form.rubricId}
                      onChange={(event) => updateForm('rubricId', event.target.value)}
                      options={[
                        { label: 'Chọn bộ tiêu chí', value: '' },
                        ...rubrics
                          .filter((rubric) => rubric.status === 'PUBLISHED' && (rubric.skill === form.skill || rubric.skill === 'MIXED'))
                          .map((rubric) => ({ label: rubric.name, value: String(rubric.id) })),
                      ]}
                    />
                  </div>
                ) : null}
                <label className="block">
                  <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Hướng dẫn làm bài</span>
                  <RichTextEditor
                    helperText=""
                    onChange={(value) => updateForm('instructions', value)}
                    placeholder="Hướng dẫn học viên làm bài..."
                    size="compact"
                    value={form.instructions}
                  />
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
                <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4">
                  <button type="button" disabled={working} onClick={saveItem} className={PRIMARY_BUTTON_CLASS}>
                    <Save className="h-4 w-4" /> Lưu {pageConfig.successNoun}
                  </button>
                  <button type="button" onClick={startNew} className={SECONDARY_BUTTON_CLASS}>
                    <Plus className="h-4 w-4" /> {pageConfig.createLabel}
                  </button>
                </div>
              </div>

              <div className="rounded-[24px] border border-[#ead8d6] bg-white/80 p-4">
                {renderWorkspace()}
              </div>
            </div>
          </div>
        </AssessmentHubModal>
      )}

      {isSkillLocked ? (
        <SkillPracticeShell
          activeSkill={pageConfig.skill}
          createLabel={pageConfig.createLabel}
          filterChildren={renderFilters()}
          onCreate={startNew}
          stats={stats}
          subtitle={pageConfig.subtitle}
          title={pageConfig.title}
        >
          {renderTable()}
        </SkillPracticeShell>
      ) : (
        <>
          <ManagerStatsGrid stats={stats} />
          <ManagerFilterBar>{renderFilters()}</ManagerFilterBar>
          {renderTable()}
        </>
      )}
    </div>
  );
}

function LockedMeta({ label, value }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
      <span className="block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{label}</span>
      <span className="mt-1 block text-sm font-extrabold text-[#0b1c30]">{value}</span>
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

function AssessmentStatusBadge({ status }) {
  const normalized = String(status || '').toUpperCase();
  const tone = normalized === 'PUBLISHED'
    ? 'success'
    : normalized === 'DRAFT'
      ? 'warning'
      : 'neutral';
  return <ManagerStatusBadge tone={tone}>{formatLabel(status)}</ManagerStatusBadge>;
}

function AssessmentHubModal({ children, onClose }) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-5 backdrop-blur-sm bg-black/45 animate-fade-in" role="dialog" aria-modal="true">
      <button
        aria-label="Đóng modal"
        className="absolute inset-0"
        onClick={onClose}
        type="button"
      />
      <div className="relative z-10 flex max-h-[calc(100dvh-2.5rem)] w-full max-w-[1280px] min-h-0 flex-col overflow-hidden rounded-3xl border border-[#dcc0bf]/50 bg-white shadow-2xl pointer-events-auto">
        {children}
      </div>
    </div>,
    document.body
  );
}
