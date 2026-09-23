import { useDeferredValue, useEffect, useMemo, useState } from 'react';
import { useAppDialog } from '../../components/ui/AppDialog';
import {
  Archive,
  ArrowLeft,
  ArrowRight,
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
  ManagerTaxonomyBadge,
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
import { getMockExamMaxScore } from '../../utils/assessmentExamPolicy';
import { getContentManagerError } from '../../utils/contentManagerFeedback';
import {
  FIELD_CLASS,
  PANEL_CLASS,
  PRIMARY_BUTTON_CLASS,
  SECONDARY_BUTTON_CLASS,
} from '../../utils/formStyles';
import { EMPTY_PAGE, pageParams } from '../../utils/pagination';
import { formatCriteriaSetName } from '../../utils/assessmentRubricLabels';
import {
  normalizeAssessmentMaxScore,
  normalizeAssessmentPassingScore,
} from '../../utils/ieltsBandScale';

const pageMap = {
  listening: {
    title: 'Luyện nghe',
    subtitle: 'Chỉ quản lý nội dung Listening. Bài tạo mới trong trang này luôn là bài nghe.',
    skill: 'LISTENING',
    type: 'LESSON_PRACTICE',
    lockedSkill: true,
    lockedType: true,
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
  },
  reading: {
    title: 'Luyện đọc',
    subtitle: 'Chỉ quản lý nội dung Reading. Bài tạo mới trong trang này luôn là bài đọc.',
    skill: 'READING',
    type: 'LESSON_PRACTICE',
    lockedSkill: true,
    lockedType: true,
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
  },
  writing: {
    title: 'Luyện viết',
    subtitle: 'Chỉ quản lý nội dung Writing. Đề tạo mới trong trang này luôn là đề viết.',
    skill: 'WRITING',
    type: 'WRITING_TASK',
    lockedSkill: true,
    lockedType: true,
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
  },
  speaking: {
    title: 'Luyện nói',
    subtitle: 'Chỉ quản lý nội dung Speaking. Đề tạo mới trong trang này luôn là đề nói.',
    skill: 'SPEAKING',
    type: 'SPEAKING_TASK',
    lockedSkill: true,
    lockedType: true,
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
  },
};

const typeOptions = [
  { label: 'Bài luyện trong bài học', value: 'LESSON_PRACTICE' },
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

const allOption = { label: 'Tất cả', value: 'ALL' };

const isProductiveSkill = (skill) => ['WRITING', 'SPEAKING'].includes(String(skill || '').toUpperCase());
const resolveAutomaticEvaluationMode = (skill) => (
  ['LISTENING', 'READING', 'WRITING', 'SPEAKING'].includes(String(skill || '').toUpperCase())
    ? 'ESTIMATED_BAND'
    : 'EXPLAIN_ONLY'
);

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
    aiEvaluationMode: resolveAutomaticEvaluationMode(skill),
    rubricId: '',
    instructions: '',
    objectiveAnswerKey: '',
    uiConfigJson: isMock ? withExamTypeInConfig('{}', examCategory, skill) : '',
    passingScore: '',
    maxScore: isMock ? getMockExamMaxScore({ examCategory }) : (isProductiveSkill(skill) ? 9 : 100),
    timeLimitMinutes: '',
    status: 'DRAFT',
  };
};

const toForm = (item = {}, pageConfig) => {
  const examCategory = resolveExamCategory(item);
  const skill = item.skill || pageConfig?.skill || 'LISTENING';
  const assessment = { ...item, skill };
  return {
    title: item.title || '',
    description: item.description || '',
    type: item.type || pageConfig?.type || 'LESSON_PRACTICE',
    skill,
    examCategory,
    aiEvaluationMode: resolveAutomaticEvaluationMode(item.skill || pageConfig?.skill),
    rubricId: item.rubric?.id ? String(item.rubric.id) : '',
    instructions: item.instructions || '',
    objectiveAnswerKey: item.objectiveAnswerKey || '',
    uiConfigJson: item.uiConfigJson || '',
    passingScore: normalizeAssessmentPassingScore(assessment) ?? '',
    maxScore: pageConfig?.type === 'MOCK_TEST'
      ? getMockExamMaxScore({ examCategory })
      : normalizeAssessmentMaxScore(assessment) ?? (isProductiveSkill(skill) ? 9 : 100),
    timeLimitMinutes: item.timeLimitMinutes ?? '',
    status: item.status || 'DRAFT',
  };
};

const supportedBuilderSkills = new Set(['LISTENING', 'READING', 'WRITING', 'SPEAKING']);

export default function ContentManagerAssessmentsHubPage({ pageKey }) {
  const { confirm: confirmDialog } = useAppDialog();
  const pageConfig = pageMap[pageKey] || pageMap.listening;
  const isSkillLocked = Boolean(pageConfig.lockedSkill);
  const isTypeLocked = Boolean(pageConfig.lockedType);
  const filtersByPageType = !isSkillLocked;
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
  const deferredKeyword = useDeferredValue(keyword);
  const resetKey = `${pageKey}-${deferredKeyword}-${filters.type}-${filters.status}-${filters.examCategory}`;
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    items,
    8,
    resetKey,
    pageResult,
  );

  const lockFormToPage = (draft, { preserveType = false } = {}) => ({
    ...draft,
    ...(isSkillLocked ? { skill: pageConfig.skill } : {}),
    ...(isTypeLocked && !preserveType ? { type: pageConfig.type } : {}),
  });

  const loadItems = async () => {
    setLoading(true);
    setError('');
    try {
      const baseParams = {
        ...(isSkillLocked ? { skill: pageConfig.skill } : {}),
        ...(filtersByPageType ? { type: pageConfig.type } : {}),
      };
      const params = {
        ...baseParams,
        type: !isTypeLocked && filters.type !== 'ALL' ? filters.type : baseParams.type,
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
      setError(getContentManagerError(err, `Không tải được ${pageConfig.successNoun}.`));
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
          maxScore: getMockExamMaxScore({ examCategory }),
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
        next.aiEvaluationMode = resolveAutomaticEvaluationMode(value);
        next.maxScore = isMockExamsPage
          ? getMockExamMaxScore({ examCategory: next.examCategory })
          : (isProductiveSkill(value) ? 9 : 100);
      }
      if (isMockExamsPage && field === 'maxScore') {
        next.maxScore = getMockExamMaxScore({ examCategory: next.examCategory });
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
    setForm(lockFormToPage(toForm(item, pageConfig), { preserveType: isSkillLocked }));
    setEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const buildPayload = (draft) => {
    const lockedDraft = lockFormToPage(draft, { preserveType: Boolean(editingId) && isSkillLocked });
    const examCategory = isMockExamsPage ? resolveExamCategory(lockedDraft) : null;
    return {
      ...lockedDraft,
      aiEvaluationMode: resolveAutomaticEvaluationMode(lockedDraft.skill),
      uiConfigJson: isMockExamsPage
        ? withExamTypeInConfig(lockedDraft.uiConfigJson, examCategory, lockedDraft.skill)
        : lockedDraft.uiConfigJson,
      passingScore: lockedDraft.passingScore === '' ? null : Number(lockedDraft.passingScore),
      maxScore: isMockExamsPage
        ? getMockExamMaxScore({ examCategory })
        : (lockedDraft.maxScore === '' ? null : Number(lockedDraft.maxScore)),
      timeLimitMinutes: lockedDraft.timeLimitMinutes === '' ? null : Number(lockedDraft.timeLimitMinutes),
    };
  };

  const saveItem = async () => {
    if (!form.title.trim()) {
      setError(isSkillLocked ? `Vui lòng nhập tên ${pageConfig.successNoun}.` : 'Vui lòng nhập tên đề.');
      return;
    }
    if (form.timeLimitMinutes !== '') {
      const durationMinutes = Number(form.timeLimitMinutes);
      if (!Number.isInteger(durationMinutes) || durationMinutes <= 0) {
        setError('Thời lượng phải là số phút nguyên lớn hơn 0.');
        return;
      }
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
    if (isProductiveSkill(payload.skill)) {
      if (!payload.rubricId) {
        setError('Bài Viết/Nói phải chọn bộ tiêu chí chấm.');
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
      setForm(lockFormToPage(toForm(saved, pageConfig), { preserveType: isSkillLocked }));
      setEditorOpen(false);
      setKeyword('');
      setFilters({ type: 'ALL', status: 'ALL', examCategory: 'ALL' });
      setPage(1);
      setSuccess(editingId ? `Đã cập nhật ${pageConfig.successNoun}.` : `Đã tạo ${pageConfig.successNoun}.`);
    } catch (err) {
      setError(getContentManagerError(err, `Không lưu được ${pageConfig.successNoun}.`));
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
      await loadItems();
      setSuccess(`Đã lưu trữ ${pageConfig.successNoun}.`);
    } catch (err) {
      setError(getContentManagerError(err, `Không lưu trữ được ${pageConfig.successNoun}.`));
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
      await loadItems();
      setSuccess(`Đã khôi phục ${pageConfig.successNoun} về bản nháp.`);
    } catch (err) {
      setError(getContentManagerError(err, `Không khôi phục được ${pageConfig.successNoun}.`));
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
      await loadItems();
      setSuccess(`Đã xuất bản ${pageConfig.successNoun}.`);
    } catch (err) {
      setError(getContentManagerError(err, `Không xuất bản được ${pageConfig.successNoun}.`));
    } finally {
      setWorking(false);
    }
  };

  const canUseBuilder = supportedBuilderSkills.has(String(form.skill || '').toUpperCase());
  const lockedForm = lockFormToPage(form);
  const mockSkillOptions = (form.examCategory || 'IELTS') === 'TOEIC' ? toeicMockSkillOptions : ieltsMockSkillOptions;

  const renderWorkspace = () => {
    if (isSkillLocked && pageConfig.skill === 'LISTENING') {
      return <ListeningPracticeWorkspace form={lockedForm} inline onChange={updateForm} />;
    }
    if (isSkillLocked && pageConfig.skill === 'READING') {
      return <ReadingPracticeWorkspace form={lockedForm} inline onChange={updateForm} />;
    }
    if (isSkillLocked && pageConfig.skill === 'WRITING') {
      return <WritingPracticeWorkspace form={lockedForm} inline onChange={updateForm} />;
    }
    if (isSkillLocked && pageConfig.skill === 'SPEAKING') {
      return <SpeakingPracticeWorkspace form={lockedForm} inline onChange={updateForm} />;
    }
    if (canUseBuilder) {
      return (
        <AssessmentExamBuilder
          assessment={{
            ...form,
            examType: isMockExamsPage ? (form.examCategory || 'IELTS') : undefined,
          }}
          inline
          key={form.skill}
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
        {!isTypeLocked && !isMockExamsPage ? (
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

    const columns = [
      { label: pageConfig.tableTitle, key: 'title' },
      ...(isMockExamsPage ? [{ label: 'Kỳ thi', key: 'exam' }] : []),
      ...(!isTypeLocked && !isMockExamsPage ? [{ label: 'Loại đề', key: 'type' }] : []),
      ...(!isSkillLocked ? [{ label: 'Kỹ năng', key: 'skill' }] : []),
      { label: 'Thời lượng', key: 'time', align: 'center' },
      { label: 'Trạng thái', key: 'status' },
      { label: 'Thao tác', key: 'actions', align: 'right' },
    ];

    return (
      <section className="overflow-hidden rounded-xl border border-[#dcc0bf]/30 bg-white shadow-sm">
        <ManagerTable columns={columns} minWidth={isSkillLocked || isTypeLocked ? '900px' : '1080px'}>
          {pageItems.map((item) => (
            <tr className="transition hover:bg-[#eff4ff]" key={item.id}>
              <td className="px-6 py-5">
                <p className="max-w-[360px] overflow-hidden text-sm font-bold leading-5 text-[#4b0009] [display:-webkit-box] [-webkit-box-orient:vertical] [-webkit-line-clamp:2]">{item.title}</p>
                {item.description ? <RichTextHtml asPlain className="mt-1 max-w-[360px] truncate text-xs text-[#564241]" value={item.description} /> : null}
              </td>
              {isMockExamsPage ? (
                <td className="px-6 py-5"><ManagerTaxonomyBadge kind="exam" value={resolveExamCategory(item)} /></td>
              ) : null}
              {!isTypeLocked && !isMockExamsPage ? (
                <td className="px-6 py-5 text-sm text-[#0b1c30]">{formatLabel(item.type)}</td>
              ) : null}
              {!isSkillLocked ? (
                <td className="px-6 py-5"><ManagerTaxonomyBadge kind="skill" value={item.skill} /></td>
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
      <ManagementToast message={error} onClose={() => setError('')} />
      <ManagementToast message={success} onClose={() => setSuccess('')} tone="success" title="Đã cập nhật nội dung" />

      {editorOpen ? (
        <AssessmentAuthoringFlow
          editing={Boolean(editingId)}
          form={form}
          isMockExamsPage={isMockExamsPage}
          isSkillLocked={isSkillLocked}
          isTypeLocked={isTypeLocked}
          key={`${pageKey}-${editingId || 'new'}`}
          onClose={closeEditor}
          onSave={saveItem}
          onUpdate={updateForm}
          pageConfig={pageConfig}
          rubrics={rubrics}
          working={working}
          workspace={renderWorkspace()}
        />
      ) : isSkillLocked ? (
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
    MODULE_TEST: 'Nội dung kỹ năng cũ',
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

function AssessmentAuthoringFlow({
  editing,
  form,
  isMockExamsPage,
  isSkillLocked,
  isTypeLocked,
  onClose,
  onSave,
  onUpdate,
  pageConfig,
  rubrics,
  working,
  workspace,
}) {
  const [step, setStep] = useState(0);
  const [stepError, setStepError] = useState('');
  const productive = isProductiveSkill(form.skill);
  const scoreMaximum = isMockExamsPage ? getMockExamMaxScore(form) : (productive ? 9 : undefined);
  const scoreStep = isMockExamsPage
    ? ((form.examCategory || 'IELTS') === 'TOEIC' ? 5 : 0.5)
    : (productive ? 0.5 : 1);
  const hasContent = hasAuthoredContent(form.uiConfigJson);
  const steps = [
    { label: 'Thông tin chung', description: 'Tên, kỹ năng và thời lượng' },
    { label: 'Nội dung bài', description: 'Đề bài, câu hỏi và học liệu' },
    { label: 'Chấm điểm', description: 'Đáp án, rubric và ngưỡng đạt' },
    { label: 'Kiểm tra', description: 'Rà soát trước khi lưu' },
  ];
  const mockSkillOptions = (form.examCategory || 'IELTS') === 'TOEIC'
    ? toeicMockSkillOptions
    : ieltsMockSkillOptions;
  const availableRubrics = rubrics
    .filter((rubric) => rubric.status === 'PUBLISHED' && rubric.skill === form.skill)
    .map((rubric) => ({ label: formatCriteriaSetName(rubric.name), value: String(rubric.id) }));

  const changeStep = (nextStep) => {
    if (nextStep > step && step === 0 && !form.title.trim()) {
      setStepError(`Vui lòng nhập tên ${pageConfig.successNoun}.`);
      return;
    }
    setStepError('');
    setStep(nextStep);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <section className="overflow-hidden rounded-3xl border border-[#ead8d6] bg-white shadow-sm">
      <header className="flex flex-wrap items-start justify-between gap-4 border-b border-[#f0e3e4] bg-[linear-gradient(135deg,#fffdfd,#fff7f7)] px-5 py-5 sm:px-7">
        <div>
          <button className="mb-3 inline-flex min-h-10 items-center gap-2 text-sm font-bold text-[#730014] hover:underline" onClick={onClose} type="button">
            <ArrowLeft className="h-4 w-4" /> Quay lại danh sách
          </button>
          <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#8a0018]">{pageConfig.title}</p>
          <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#0b1c30]">
            {editing ? pageConfig.editLabel : pageConfig.createLabel}
          </h2>
        </div>
        <button aria-label="Đóng trình biên soạn" className="inline-flex h-11 w-11 items-center justify-center rounded-xl border border-[#dfbfbd]/65 text-[#730014] transition hover:bg-white" onClick={onClose} type="button">
          <X className="h-5 w-5" />
        </button>
      </header>

      <nav aria-label="Các bước biên soạn" className="grid border-b border-[#f0e3e4] bg-white sm:grid-cols-2 xl:grid-cols-4">
        {steps.map((item, index) => {
          const active = step === index;
          const completed = index < step;
          return (
            <button
              aria-current={active ? 'step' : undefined}
              className={`flex min-h-[78px] items-center gap-3 border-b border-[#ead8d6] px-5 py-4 text-left transition sm:border-r xl:border-b-0 xl:last:border-r-0 ${active ? 'bg-[#4b0009] text-white' : 'bg-white text-[#0b1c30] hover:bg-[#fff7f7]'}`}
              key={item.label}
              onClick={() => changeStep(index)}
              type="button"
            >
              <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-sm font-extrabold ${active ? 'bg-white text-[#4b0009]' : completed ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>
                {completed ? <CheckCircle2 className="h-4 w-4" /> : index + 1}
              </span>
              <span className="min-w-0">
                <span className="block text-sm font-extrabold">{item.label}</span>
                <span className={`mt-0.5 block text-xs ${active ? 'text-white/75' : 'text-slate-500'}`}>{item.description}</span>
              </span>
            </button>
          );
        })}
      </nav>

      <div className="min-h-[520px] p-5 sm:p-7">
        {stepError ? <div className="mb-5 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700" role="alert">{stepError}</div> : null}

        {step === 0 ? (
          <div className="mx-auto max-w-4xl space-y-6">
            <AuthoringSection description="Nhập những thông tin học viên cần thấy trước khi bắt đầu." title="Nhận diện bài">
              <div className="grid gap-5 lg:grid-cols-2">
                <label className="block lg:col-span-2">
                  <FieldLabel>Tên {pageConfig.successNoun}</FieldLabel>
                  <input className={FIELD_CLASS} onChange={(event) => onUpdate('title', event.target.value)} value={form.title} />
                </label>
                {isSkillLocked ? <LockedMeta label="Kỹ năng" value={formatLabel(pageConfig.skill)} /> : (
                  <div>
                    <FieldLabel>Kỹ năng</FieldLabel>
                    <BrandedSelect onChange={(event) => onUpdate('skill', event.target.value)} options={isMockExamsPage ? mockSkillOptions : skillOptions} value={form.skill} />
                  </div>
                )}
                {isMockExamsPage ? (
                  <div>
                    <FieldLabel>Kỳ thi</FieldLabel>
                    <BrandedSelect onChange={(event) => onUpdate('examCategory', event.target.value)} options={examCategoryOptions} value={form.examCategory || 'IELTS'} />
                  </div>
                ) : isTypeLocked ? <LockedMeta label="Dạng nội dung" value={formatLabel(form.type)} /> : (
                  <div>
                    <FieldLabel>Loại đề</FieldLabel>
                    <BrandedSelect onChange={(event) => onUpdate('type', event.target.value)} options={typeOptions} value={form.type} />
                  </div>
                )}
                <label className="block">
                  <FieldLabel>Thời lượng</FieldLabel>
                  <div className="relative">
                    <input className={`${FIELD_CLASS} appearance-none pr-16 [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none`} min="1" onChange={(event) => onUpdate('timeLimitMinutes', event.target.value)} placeholder="Ví dụ: 40" step="1" type="number" value={form.timeLimitMinutes} />
                    <span className="pointer-events-none absolute inset-y-0 right-4 flex items-center text-sm font-semibold text-slate-500">phút</span>
                  </div>
                </label>
              </div>
            </AuthoringSection>
            <AuthoringSection description="Mô tả ngắn giúp phân biệt bài trong kho nội dung." title="Mô tả">
              <RichTextEditor helperText="" onChange={(value) => onUpdate('description', value)} placeholder="Mô tả nội dung và mục tiêu của bài..." size="compact" value={form.description} />
            </AuthoringSection>
          </div>
        ) : null}

        {step === 1 ? (
          <div className="mx-auto max-w-6xl">
            <AuthoringSection description={`Chỉ hiển thị công cụ phù hợp với kỹ năng ${formatLabel(form.skill)}.`} title={`Biên soạn nội dung ${formatLabel(form.skill)}`}>
              {workspace}
            </AuthoringSection>
          </div>
        ) : null}

        {step === 2 ? (
          <div className="mx-auto grid max-w-5xl gap-6 lg:grid-cols-[1.05fr_.95fr]">
            <AuthoringSection description="Thiết lập cách hệ thống đánh giá và xác định kết quả đạt." title="Quy tắc chấm">
              <div className="space-y-5">
                {productive ? (
                  <div>
                    <FieldLabel>Bộ tiêu chí chấm</FieldLabel>
                    <BrandedSelect onChange={(event) => onUpdate('rubricId', event.target.value)} options={[{ label: 'Chọn bộ tiêu chí', value: '' }, ...availableRubrics]} value={form.rubricId} />
                  </div>
                ) : (
                  <div className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm font-semibold text-blue-800">
                    Câu trả lời được đối chiếu với đáp án đã thiết lập trong phần Nội dung bài.
                  </div>
                )}
                <label className="block">
                  <FieldLabel>Điểm đạt</FieldLabel>
                  <input className={FIELD_CLASS} max={scoreMaximum} min="0" onChange={(event) => onUpdate('passingScore', event.target.value)} step={scoreStep} type="number" value={form.passingScore} />
                </label>
                {isMockExamsPage ? (
                  <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                    <FieldLabel>Điểm tối đa</FieldLabel>
                    <div className="flex h-11 items-center rounded-xl border border-slate-200 bg-white px-4 text-sm font-bold text-[#0b1c30]">
                      {getMockExamMaxScore(form)} điểm
                    </div>
                  </div>
                ) : (
                  <details className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                    <summary className="cursor-pointer text-sm font-bold text-[#730014]">Thiết lập nâng cao</summary>
                    <label className="mt-4 block">
                      <FieldLabel>Điểm tối đa</FieldLabel>
                      <input className={FIELD_CLASS} max={productive ? '9' : undefined} min="1" onChange={(event) => onUpdate('maxScore', event.target.value)} step={productive ? '0.5' : '1'} type="number" value={form.maxScore} />
                    </label>
                  </details>
                )}
              </div>
            </AuthoringSection>
            <AuthoringSection description="Nội dung này được hiển thị cho học viên trước khi làm bài." title="Hướng dẫn làm bài">
              <RichTextEditor helperText="" onChange={(value) => onUpdate('instructions', value)} placeholder="Nhập hướng dẫn cần thiết..." size="compact" value={form.instructions} />
            </AuthoringSection>
          </div>
        ) : null}

        {step === 3 ? (
          <div className="mx-auto max-w-5xl space-y-6">
            <AuthoringSection description="Kiểm tra các phần chính trước khi lưu vào kho nội dung." title="Tóm tắt">
              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                <ReviewItem label="Tên bài" value={form.title || 'Chưa nhập'} ready={Boolean(form.title.trim())} />
                <ReviewItem label="Kỹ năng" value={formatLabel(form.skill)} ready />
                <ReviewItem label="Nội dung" value={hasContent ? 'Đã biên soạn' : 'Chưa biên soạn'} ready={hasContent} />
                <ReviewItem label="Cách chấm" value={productive ? (form.rubricId ? 'Đã chọn rubric' : 'Chưa chọn rubric') : 'Theo đáp án'} ready={!productive || Boolean(form.rubricId)} />
              </div>
            </AuthoringSection>
            <AuthoringSection description="Chọn Nháp nếu nội dung chưa sẵn sàng cho người học." title="Trạng thái">
              <div className="max-w-sm">
                <BrandedSelect onChange={(event) => onUpdate('status', event.target.value)} options={statusOptions} value={form.status} />
              </div>
            </AuthoringSection>
          </div>
        ) : null}
      </div>

      <footer className="flex flex-wrap items-center justify-between gap-3 border-t border-[#f0e3e4] bg-[#fffafa] px-5 py-4 sm:px-7">
        <button className={SECONDARY_BUTTON_CLASS} disabled={step === 0} onClick={() => changeStep(step - 1)} type="button">
          <ArrowLeft className="h-4 w-4" /> Quay lại
        </button>
        <span className="text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Bước {step + 1} / {steps.length}</span>
        {step < steps.length - 1 ? (
          <button className={PRIMARY_BUTTON_CLASS} onClick={() => changeStep(step + 1)} type="button">
            Tiếp tục <ArrowRight className="h-4 w-4" />
          </button>
        ) : (
          <button className={PRIMARY_BUTTON_CLASS} disabled={working} onClick={onSave} type="button">
            <Save className="h-4 w-4" /> {working ? 'Đang lưu...' : `Lưu ${pageConfig.successNoun}`}
          </button>
        )}
      </footer>
    </section>
  );
}

function hasAuthoredContent(uiConfigJson) {
  if (!String(uiConfigJson || '').trim()) return false;
  try {
    const config = JSON.parse(uiConfigJson);
    return ['parts', 'tasks', 'variants', 'questions', 'sections']
      .some((key) => Array.isArray(config?.[key]) && config[key].length > 0);
  } catch {
    return false;
  }
}

function AuthoringSection({ children, description, title }) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">{title}</h3>
      {description ? <p className="mt-1 text-sm leading-relaxed text-slate-500">{description}</p> : null}
      <div className="mt-5">{children}</div>
    </section>
  );
}

function FieldLabel({ children }) {
  return <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{children}</span>;
}

function ReviewItem({ label, ready, value }) {
  return (
    <div className={`rounded-xl border p-4 ${ready ? 'border-emerald-200 bg-emerald-50' : 'border-amber-200 bg-amber-50'}`}>
      <p className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500">{label}</p>
      <p className={`mt-2 text-sm font-extrabold ${ready ? 'text-emerald-800' : 'text-amber-800'}`}>{value}</p>
    </div>
  );
}
