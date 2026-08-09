import { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { BookMarked, Check, GraduationCap, Link2, Pencil, Plus, RefreshCw, Save, Search, Trash2, X } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import classroomApi from '../../api/classroomApi';
import courseApi from '../../api/courseApi';
import curriculumApi from '../../api/curriculumApi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import {
  EnglishEntryLevelField,
  IeltsBandSelect,
  ToeicScoreField,
} from '../../components/content-manager/EnglishScoreFields';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import { HeaderActions } from '../../components/content-manager/ContentManagerUi';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import { useAppDialog } from '../../components/ui/AppDialog';
import {
  ERROR_NOTICE_CLASS,
  FIELD_CLASS,
  SUCCESS_NOTICE_CLASS,
  TEXTAREA_CLASS,
} from '../../utils/formStyles';
import {
  ENGLISH_EXAM_OPTIONS,
  ENGLISH_SKILL_OPTIONS,
  ENGLISH_TRACK_OPTIONS,
  getEnglishProfileDefaults,
  normalizeEnglishEntryLevel,
  normalizeEnglishExamCategory,
  readEnglishFocusSkills,
  validateEnglishProgramProfile,
} from '../../utils/englishProgramProfile';

const emptyUnit = {
  title: '',
  description: '',
  displayOrder: 0,
  sessionPlan: '',
};

const emptyAttach = {
  unitId: '',
  type: 'MATERIAL',
  resourceId: '',
  displayOrder: 0,
  note: '',
};

const emptyProgramForm = {
  title: '',
  code: '',
  slug: '',
  deliveryMode: 'OFFLINE',
  examCategory: 'IELTS',
  programTrack: 'IELTS_ACADEMIC',
  focusSkills: ['LISTENING', 'READING', 'WRITING', 'SPEAKING'],
  targetBand: 6.5,
  targetScore: '',
  entryLevel: '4.0',
  outcomes: '',
  totalSessions: 0,
  status: 'DRAFT',
  teacherGuide: '',
  interactionActivities: '',
  displayOrder: 0,
  virtualPlatform: '',
  recordingAllowed: false,
  recordingAvailableDays: 0,
  materialsDownloadable: false,
  sessionOpenBeforeMinutes: 0,
  sessionCloseAfterMinutes: 0,
  deviceCheckRequired: false,
  micRequired: false,
  speakerRequired: false,
  cameraRequired: false,
  autoAttendanceEnabled: false,
  minAttendanceMinutes: 0,
};

const deliveryModeOptions = [
  { label: 'Tại trung tâm', value: 'OFFLINE' },
  { label: 'Trực tuyến với giảng viên', value: 'VIRTUAL' },
];

const typeOptions = [
  { label: 'Học liệu', value: 'MATERIAL', description: 'Gắn file/tài liệu từ kho học liệu trung tâm.' },
  { label: 'Bài tập', value: 'EXERCISE', description: 'Gắn bài tập dùng chung từ ngân hàng bài tập.' },
  { label: 'Đề luyện tập/kiểm tra', value: 'ASSESSMENT', description: 'Gắn đề dùng chung từ ngân hàng đề.' },
  { label: 'Flashcard', value: 'FLASHCARD', description: 'Gắn bộ flashcard độc lập.' },
];

const refGroups = [
  { key: 'materials', title: 'Học liệu' },
  { key: 'exercises', title: 'Bài tập' },
  { key: 'assessments', title: 'Đề' },
  { key: 'flashcards', title: 'Flashcard' },
];

const asList = (value) => (Array.isArray(value) ? value : value?.content || value?.items || []);
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

const getReadableResourceText = (value) => {
  const config = parseStructuredResource(value);
  return config ? describeStructuredResource(config) : value;
};

const toSlug = (value) => String(value || '')
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/đ/g, 'd')
  .replace(/Đ/g, 'D')
  .replace(/[^\w\s-]/g, '')
  .trim()
  .replace(/\s+/g, '-')
  .replace(/-+/g, '-')
  .toLowerCase();

const makeCode = (title, mode) => {
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
  return [mode, ...words].filter(Boolean).join('-');
};

const toProgramForm = (program) => {
  const examCategory = normalizeEnglishExamCategory(program?.examCategory);
  const defaults = getEnglishProfileDefaults(examCategory);
  return {
    ...emptyProgramForm,
    ...program,
    examCategory,
    programTrack: program?.programTrack || defaults.programTrack,
    focusSkills: readEnglishFocusSkills(program?.focusSkills, examCategory),
    targetBand: examCategory === 'IELTS' ? (program?.targetBand ?? defaults.targetBand) : '',
    targetScore: examCategory === 'TOEIC' ? (program?.targetScore ?? defaults.targetScore) : '',
    entryLevel: normalizeEnglishEntryLevel(program?.entryLevel, examCategory),
    totalSessions: program?.totalSessions ?? 0,
  };
};

const toProgramPayload = (form, forceDraft = false) => ({
  title: form.title.trim(),
  code: form.code.trim() || makeCode(form.title, form.deliveryMode),
  slug: form.slug.trim() || toSlug(form.title),
  deliveryMode: form.deliveryMode,
  examCategory: form.examCategory,
  programTrack: form.programTrack,
  focusSkills: form.focusSkills.join(','),
  targetBand: form.targetBand === '' ? null : Number(form.targetBand),
  targetScore: form.targetScore === '' ? null : Number(form.targetScore),
  entryLevel: form.entryLevel?.trim() || null,
  outcomes: form.outcomes?.trim() || null,
  teacherGuide: form.teacherGuide?.trim() || null,
  interactionActivities: form.interactionActivities?.trim() || null,
  totalSessions: Number(form.totalSessions || 0),
  status: forceDraft ? 'DRAFT' : (form.status || 'DRAFT'),
  displayOrder: Number(form.displayOrder || 0),
  virtualPlatform: form.virtualPlatform || null,
  recordingAllowed: Boolean(form.recordingAllowed),
  recordingAvailableDays: Number(form.recordingAvailableDays || 0),
  materialsDownloadable: Boolean(form.materialsDownloadable),
  sessionOpenBeforeMinutes: Number(form.sessionOpenBeforeMinutes || 0),
  sessionCloseAfterMinutes: Number(form.sessionCloseAfterMinutes || 0),
  deviceCheckRequired: Boolean(form.deviceCheckRequired),
  micRequired: Boolean(form.micRequired),
  speakerRequired: Boolean(form.speakerRequired),
  cameraRequired: Boolean(form.cameraRequired),
  autoAttendanceEnabled: Boolean(form.autoAttendanceEnabled),
  minAttendanceMinutes: Number(form.minAttendanceMinutes || 0),
});

export default function ContentManagerSyllabusBuilderPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedProgramId = searchParams.get('programId') || '';
  const requestedUnitId = searchParams.get('unitId');
  const requestedPanel = searchParams.get('panel');
  const [programs, setPrograms] = useState([]);
  const [selectedProgramId, setSelectedProgramId] = useState(requestedProgramId);
  const [programDetail, setProgramDetail] = useState(null);
  const [programForm, setProgramForm] = useState(emptyProgramForm);
  const [programCreatorOpen, setProgramCreatorOpen] = useState(false);
  const [programEditorOpen, setProgramEditorOpen] = useState(false);
  const [unitForm, setUnitForm] = useState(emptyUnit);
  const [editingUnitId, setEditingUnitId] = useState(null);
  const [unitEditorOpen, setUnitEditorOpen] = useState(requestedPanel === 'unit');
  const [attachForm, setAttachForm] = useState(() => ({ ...emptyAttach, unitId: requestedUnitId || '' }));
  const [resourcePanelOpen, setResourcePanelOpen] = useState(requestedPanel === 'resource');
  const [expandedUnitId, setExpandedUnitId] = useState(requestedUnitId);
  const [resourceDetailUnitId, setResourceDetailUnitId] = useState(null);
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

  const loadPrograms = async () => {
    setLoading(true);
    setError('');
    try {
      const programData = await curriculumApi.getCurriculumPrograms();
      setPrograms(programData);
      setSelectedProgramId((current) => (
        current && programData.some((program) => String(program.id) === current) ? current : ''
      ));
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách giáo trình.');
    } finally {
      setLoading(false);
    }
  };

  const loadProgramDetail = async (programId) => {
    if (!programId) {
      setProgramDetail(null);
      return;
    }
    setError('');
    try {
      const data = await curriculumApi.getCurriculumProgram(programId);
      setProgramDetail(data);
      setAttachForm((current) => ({
        ...current,
        unitId: current.unitId || (data?.units?.[0]?.id ? String(data.units[0].id) : ''),
      }));
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được nội dung giáo trình.');
    }
  };

  const loadBanks = async () => {
    try {
      const [materials, exercises, assessments, flashcards] = await Promise.all([
        classroomApi.getContentManagerMaterialLibrary(),
        courseApi.getExerciseBankItems({ includeInactive: true }),
        curriculumApi.getAssessmentBank(),
        curriculumApi.getFlashcardSets(),
      ]);
      setBanks({
        materials: asList(materials),
        exercises: asList(exercises),
        assessments: asList(assessments),
        flashcards: asList(flashcards),
      });
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được các kho tài nguyên.');
    }
  };

  const reloadAll = async () => {
    await Promise.all([loadPrograms(), loadBanks()]);
    if (selectedProgramId) {
      await loadProgramDetail(selectedProgramId);
    }
  };

  useEffect(() => {
    loadPrograms();
    loadBanks();
  }, []);

  useEffect(() => {
    if (selectedProgramId) {
      loadProgramDetail(selectedProgramId);
      setEditingUnitId(null);
      setUnitForm(emptyUnit);
      setAttachForm({ ...emptyAttach, unitId: requestedUnitId || '' });
    } else {
      setProgramDetail(null);
    }
  }, [requestedUnitId, selectedProgramId]);

  const openProgramWorkspace = (program) => {
    setSelectedProgramId(String(program.id));
    setExpandedUnitId(null);
    setResourceDetailUnitId(null);
    setSearchParams({ programId: String(program.id) }, { replace: true });
    setKeyword('');
    setError('');
    setSuccess('');
  };

  const closeProgramWorkspace = () => {
    setSelectedProgramId('');
    setExpandedUnitId(null);
    setResourceDetailUnitId(null);
    setSearchParams({}, { replace: true });
    setProgramDetail(null);
    setKeyword('');
    setEditingUnitId(null);
    setUnitForm(emptyUnit);
    setAttachForm(emptyAttach);
    setError('');
    setSuccess('');
  };

  const selectProgram = (programId) => {
    setSelectedProgramId(programId);
    setExpandedUnitId(null);
    setResourceDetailUnitId(null);
    setSearchParams(programId ? { programId } : {}, { replace: true });
  };

  const updateProgramForm = (patch) => {
    setProgramForm((current) => {
      const next = { ...current, ...patch };
      if (Object.prototype.hasOwnProperty.call(patch, 'title')) {
        next.code = current.code || makeCode(patch.title, current.deliveryMode);
        next.slug = current.slug || toSlug(patch.title);
      }
      if (Object.prototype.hasOwnProperty.call(patch, 'deliveryMode') && !current.code) {
        next.code = makeCode(current.title, patch.deliveryMode);
      }
      return next;
    });
  };

  const openProgramCreator = () => {
    setProgramForm(emptyProgramForm);
    setProgramCreatorOpen(true);
    setError('');
    setSuccess('');
  };

  const closeProgramCreator = () => {
    setProgramCreatorOpen(false);
    setProgramForm(emptyProgramForm);
  };

  const openProgramEditor = () => {
    if (!programDetail) return;
    setProgramForm(toProgramForm(programDetail));
    setProgramEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const closeProgramEditor = () => {
    setProgramEditorOpen(false);
    setProgramForm(emptyProgramForm);
  };

  const createProgram = async (event) => {
    event.preventDefault();
    if (!programForm.title.trim()) {
      setError('Vui lòng nhập tên giáo trình.');
      return;
    }
    const generatedCode = programForm.code.trim() || makeCode(programForm.title, programForm.deliveryMode);
    if (!generatedCode) {
      setError('Vui lòng nhập mã giáo trình.');
      return;
    }
    const profileError = validateEnglishProgramProfile(programForm);
    if (profileError) {
      setError(profileError);
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const saved = await curriculumApi.createCurriculumProgram({
        ...toProgramPayload(programForm, true),
        code: generatedCode,
        displayOrder: 0,
      });
      setPrograms((current) => [saved, ...current]);
      setSelectedProgramId(String(saved.id));
      setSearchParams({ programId: String(saved.id) }, { replace: true });
      setProgramDetail(saved);
      closeProgramCreator();
      setSuccess('Đã tạo giáo trình. Bắt đầu thêm unit/buổi học cho giáo trình này.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tạo được giáo trình.');
    } finally {
      setWorking(false);
    }
  };

  const updateProgram = async (event) => {
    event.preventDefault();
    if (!programDetail || !programForm.title.trim() || !programForm.code.trim()) {
      setError('Vui lòng nhập đầy đủ tên và mã giáo trình.');
      return;
    }
    const profileError = validateEnglishProgramProfile(programForm);
    if (profileError) {
      setError(profileError);
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const saved = await curriculumApi.updateCurriculumProgram(programDetail.id, toProgramPayload(programForm));
      setProgramDetail(saved);
      setPrograms((current) => current.map((program) => (
        String(program.id) === String(saved.id) ? { ...program, ...saved } : program
      )));
      closeProgramEditor();
      setSuccess('Đã cập nhật thông tin giáo trình.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không cập nhật được thông tin giáo trình.');
    } finally {
      setWorking(false);
    }
  };

  const programsOptions = programs.map((program) => ({
    label: `${program.title} · ${program.code}`,
    value: String(program.id),
    description: `${program.deliveryModeLabel || program.deliveryMode} · ${program.examCategory || 'IELTS'}${program.targetBand ? ` · Band ${program.targetBand}` : ''}${program.targetScore ? ` · Target ${program.targetScore}` : ''}`,
  }));

  const units = useMemo(
    () => [...(programDetail?.units || [])].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || a.id - b.id),
    [programDetail],
  );
  const resourceDetailUnit = units.find((unit) => String(unit.id) === String(resourceDetailUnitId)) || null;

  useEffect(() => {
    if (requestedPanel !== 'unit' || !requestedUnitId || !units.length) return;
    const unit = units.find((item) => String(item.id) === String(requestedUnitId));
    if (!unit) return;
    setEditingUnitId(unit.id);
    setUnitForm({
      title: unit.title || '',
      description: unit.description || '',
      displayOrder: unit.displayOrder ?? 0,
      sessionPlan: unit.sessionPlan || '',
    });
    setUnitEditorOpen(true);
  }, [requestedPanel, requestedUnitId, units]);

  const filteredUnits = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) return units;
    return units.filter((unit) => [unit.title, unit.description, unit.sessionPlan]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(normalized)));
  }, [units, keyword]);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    filteredUnits,
    UNIT_PAGE_SIZE,
    `${selectedProgramId}-${keyword}`,
  );

  useEffect(() => {
    if (!requestedUnitId || !filteredUnits.length) return;
    const unitIndex = filteredUnits.findIndex((unit) => String(unit.id) === String(requestedUnitId));
    if (unitIndex >= 0) setPage(Math.floor(unitIndex / UNIT_PAGE_SIZE) + 1);
  }, [filteredUnits, requestedUnitId, setPage]);

  const unitOptions = units.map((unit) => ({
    label: `${unit.displayOrder ?? 0}. ${unit.title}`,
    value: String(unit.id),
  }));

  const currentResources = useMemo(() => {
    if (attachForm.type === 'MATERIAL') return banks.materials;
    if (attachForm.type === 'EXERCISE') return banks.exercises;
    if (attachForm.type === 'ASSESSMENT') return banks.assessments;
    return banks.flashcards;
  }, [attachForm.type, banks]);

  const selectedAttachUnit = units.find((unit) => String(unit.id) === String(attachForm.unitId)) || null;
  const selectedGroup = refGroups.find((group) => group.key === ({
    MATERIAL: 'materials',
    EXERCISE: 'exercises',
    ASSESSMENT: 'assessments',
    FLASHCARD: 'flashcards',
  })[attachForm.type]);
  const attachedResourceIds = new Set(
    (selectedAttachUnit?.[selectedGroup?.key] || []).map((reference) => String(reference.resourceId)),
  );
  const availableResources = currentResources.filter((item) => !attachedResourceIds.has(String(item.id)));
  const selectedResource = currentResources.find((item) => String(item.id) === String(attachForm.resourceId));
  const resourceOptions = availableResources.map((item) => ({
    label: item.title,
    value: String(item.id),
    description: [
      item.skill,
      item.materialType || item.exerciseType || item.type || item.examCategory,
      formatStatusLabel(item.status || (item.active === false ? 'INACTIVE' : 'ACTIVE')),
    ].filter(Boolean).join(' · '),
  }));

  const updateUnitInState = (savedUnit) => {
    setProgramDetail((current) => {
      if (!current) return current;
      const exists = current.units?.some((unit) => String(unit.id) === String(savedUnit.id));
      return {
        ...current,
        units: exists
          ? current.units.map((unit) => (String(unit.id) === String(savedUnit.id) ? savedUnit : unit))
          : [...(current.units || []), savedUnit],
      };
    });
    setAttachForm((current) => ({ ...current, unitId: String(savedUnit.id) }));
  };

  const resetUnitForm = () => {
    setEditingUnitId(null);
    setUnitForm(emptyUnit);
    setUnitEditorOpen(true);
    setSearchParams({ programId: selectedProgramId, panel: 'unit' }, { replace: true });
    setError('');
    setSuccess('');
  };

  const openEditUnit = (unit) => {
    setEditingUnitId(unit.id);
    setUnitForm({
      title: unit.title || '',
      description: unit.description || '',
      displayOrder: unit.displayOrder ?? 0,
      sessionPlan: unit.sessionPlan || '',
    });
    setAttachForm((current) => ({ ...current, unitId: String(unit.id) }));
    setUnitEditorOpen(true);
    setSearchParams({ programId: selectedProgramId, unitId: String(unit.id), panel: 'unit' }, { replace: true });
  };

  const closeUnitEditor = () => {
    setUnitEditorOpen(false);
    setSearchParams({
      programId: selectedProgramId,
      ...(expandedUnitId ? { unitId: String(expandedUnitId) } : {}),
    }, { replace: true });
  };

  const openResourcePanel = (unitId = attachForm.unitId) => {
    const resolvedUnitId = unitId ? String(unitId) : attachForm.unitId;
    setAttachForm((current) => ({
      ...current,
      unitId: resolvedUnitId || current.unitId,
      resourceId: '',
      note: '',
    }));
    setResourcePanelOpen(true);
    setSearchParams({
      programId: selectedProgramId,
      ...(resolvedUnitId ? { unitId: resolvedUnitId } : {}),
      panel: 'resource',
    }, { replace: true });
  };

  const closeResourcePanel = () => {
    setResourcePanelOpen(false);
    setSearchParams({
      programId: selectedProgramId,
      ...(attachForm.unitId ? { unitId: String(attachForm.unitId) } : {}),
    }, { replace: true });
  };

  const selectAttachUnit = (unitId) => {
    setAttachForm((current) => ({ ...current, unitId, resourceId: '' }));
    setSearchParams({
      programId: selectedProgramId,
      ...(unitId ? { unitId } : {}),
      panel: 'resource',
    }, { replace: true });
  };

  const saveUnit = async () => {
    if (!selectedProgramId) {
      setError('Vui lòng chọn giáo trình.');
      return;
    }
    if (!unitForm.title.trim()) {
      setError('Vui lòng nhập tên unit/buổi học.');
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    const payload = {
      ...unitForm,
      displayOrder: Number(unitForm.displayOrder || 0),
    };
    try {
      const saved = editingUnitId
        ? await curriculumApi.updateCurriculumUnit(editingUnitId, payload)
        : await curriculumApi.createCurriculumUnit(selectedProgramId, payload);
      updateUnitInState(saved);
      setEditingUnitId(saved.id);
      closeUnitEditor();
      setUnitForm({
        title: saved.title || '',
        description: saved.description || '',
        displayOrder: saved.displayOrder ?? 0,
        sessionPlan: saved.sessionPlan || '',
      });
      setSuccess(editingUnitId ? 'Đã cập nhật unit.' : 'Đã tạo unit mới.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được unit.');
    } finally {
      setWorking(false);
    }
  };

  const deleteUnit = async (unit) => {
    if (!await confirmDialog(`Xóa unit “${unit.title}” khỏi giáo trình?`, {
      title: 'Xóa unit',
      confirmLabel: 'Xóa unit',
      tone: 'danger',
    })) return;
    setWorking(true);
    setError('');
    try {
      await curriculumApi.deleteCurriculumUnit(unit.id);
      setProgramDetail((current) => ({
        ...current,
        units: (current?.units || []).filter((item) => String(item.id) !== String(unit.id)),
      }));
      if (String(editingUnitId) === String(unit.id)) resetUnitForm();
      setSuccess('Đã xóa unit.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không xóa được unit.');
    } finally {
      setWorking(false);
    }
  };

  const attachResource = async () => {
    if (!attachForm.unitId || !attachForm.resourceId) {
      setError('Vui lòng chọn unit và tài nguyên cần gắn.');
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    const payload = {
      resourceId: Number(attachForm.resourceId),
      displayOrder: Number(attachForm.displayOrder || 0),
      note: attachForm.note,
    };
    try {
      let savedUnit;
      if (attachForm.type === 'MATERIAL') {
        savedUnit = await curriculumApi.attachUnitMaterial(attachForm.unitId, payload);
      } else if (attachForm.type === 'EXERCISE') {
        savedUnit = await curriculumApi.attachUnitExercise(attachForm.unitId, payload);
      } else if (attachForm.type === 'ASSESSMENT') {
        savedUnit = await curriculumApi.attachUnitAssessment(attachForm.unitId, payload);
      } else {
        savedUnit = await curriculumApi.attachUnitFlashcard(attachForm.unitId, payload);
      }
      updateUnitInState(savedUnit);
      setExpandedUnitId(savedUnit.id);
      setSearchParams({ programId: selectedProgramId, unitId: String(savedUnit.id) }, { replace: true });
      setAttachForm((current) => ({
        ...current,
        resourceId: '',
        displayOrder: Number(current.displayOrder || 0) + 1,
        note: '',
      }));
      setSuccess(`Đã gắn ${typeOptions.find((option) => option.value === attachForm.type)?.label?.toLowerCase() || 'tài nguyên'} vào unit.`);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không gắn được tài nguyên.');
    } finally {
      setWorking(false);
    }
  };

  const detachResource = async (ref) => {
    if (!await confirmDialog(`Gỡ “${ref.title}” khỏi unit này?`, {
      title: 'Gỡ tài nguyên',
      confirmLabel: 'Gỡ tài nguyên',
      tone: 'danger',
    })) return;
    setWorking(true);
    setError('');
    try {
      await curriculumApi.detachReference(ref.type, ref.id);
      await loadProgramDetail(selectedProgramId);
      setSuccess('Đã gỡ tài nguyên khỏi unit.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không gỡ được tài nguyên.');
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-6">
      {error && !programCreatorOpen && !programEditorOpen ? <div className={ERROR_NOTICE_CLASS} role="alert">{error}</div> : null}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      {programCreatorOpen ? (
        <SyllabusProgramCreateModal
          error={error}
          form={programForm}
          onChange={updateProgramForm}
          onClose={closeProgramCreator}
          onSubmit={createProgram}
          saving={working}
        />
      ) : null}

      {programEditorOpen ? (
        <SyllabusProgramCreateModal
          error={error}
          form={programForm}
          mode="edit"
          onChange={updateProgramForm}
          onClose={closeProgramEditor}
          onSubmit={updateProgram}
          saving={working}
        />
      ) : null}

      {!selectedProgramId ? (
        <SyllabusProgramListPanel
          programs={programs}
          loading={loading}
          onCreate={openProgramCreator}
          onOpen={openProgramWorkspace}
          onRefresh={loadPrograms}
        />
      ) : (
        <>
          <section className="overflow-hidden rounded-xl border border-[#dcc0bf]/30 bg-white shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-4 border-b border-[#dcc0bf]/25 bg-[#fbf3f4] px-6 py-5">
              <div className="min-w-0">
                <button
                  className="mb-4 inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf]/40 bg-white px-3 py-2 text-sm font-bold text-[#4b0009] transition hover:bg-[#fff7f7]"
                  onClick={closeProgramWorkspace}
                  type="button"
                >
                  <span aria-hidden="true">&lt;</span>
                  Quay lại danh sách giáo trình
                </button>
                <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8e7371]">Biên soạn giáo trình</p>
                <h2 className="mt-2 max-w-3xl break-words font-['Manrope'] text-3xl font-extrabold text-[#0b1c30]">{programDetail?.title}</h2>
                <p className="mt-2 text-sm text-[#584140]">
                  {programDetail ? `${programDetail.code} · ${programDetail.deliveryModeLabel || programDetail.deliveryMode || 'Giáo trình'}` : 'Đang tải giáo trình...'}
                </p>
              </div>
              <div className="flex flex-wrap gap-3">
                <button type="button" onClick={openProgramEditor} className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg border border-[#dcc0bf]/40 bg-white px-4 py-2.5 text-sm font-bold text-[#4b0009] transition hover:bg-[#fff7f7]">
                  <Pencil className="h-4 w-4" /> Chỉnh sửa thông tin
                </button>
                <button type="button" onClick={reloadAll} className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg border border-[#dcc0bf]/40 bg-white px-4 py-2.5 text-sm font-bold text-[#4b0009] transition hover:bg-[#fff7f7]">
                  <RefreshCw className="h-4 w-4" /> Tải lại
                </button>
              </div>
            </div>

            <div className="grid gap-3 px-6 py-5 sm:grid-cols-2 lg:grid-cols-5">
              <InfoTile label="Mã giáo trình" value={programDetail?.code || '-'} />
              <InfoTile label="Hình thức" value={programDetail?.deliveryModeLabel || programDetail?.deliveryMode || '-'} />
              <InfoTile label="Target" value={programDetail?.targetBand || programDetail?.targetScore || '-'} />
              <InfoTile label="Unit hiện có" value={units.length} />
              <InfoTile label="Số buổi dự kiến" value={programDetail?.totalSessions ?? '-'} />
            </div>
          </section>

          <div className="space-y-6">
              <section className="rounded-xl border border-[#dcc0bf]/30 bg-white p-4 shadow-sm">
                <div className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_260px]">
                  <div className="relative">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-[#897270]" />
                    <input
                      value={keyword}
                      onChange={(event) => setKeyword(event.target.value)}
                      placeholder="Tìm unit, mô tả hoặc kế hoạch buổi học..."
                      className="w-full rounded-lg border border-[#dcc0bf]/50 bg-[#f8f9ff] py-2 pl-10 pr-4 text-sm text-[#0b1c30] outline-none transition focus:border-[#4b0009] focus:bg-white focus:ring-4 focus:ring-[#4b0009]/5"
                    />
                  </div>
                  <BrandedSelect
                    value={selectedProgramId}
                    onChange={(event) => selectProgram(event.target.value)}
                    options={programsOptions}
                    placeholder={loading ? 'Đang tải giáo trình...' : 'Chọn giáo trình'}
                    searchable={true}
                  />
                </div>
              </section>

      {unitEditorOpen && (
        <UnitEditorModal onClose={closeUnitEditor}>
          <section className="space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">
                {editingUnitId ? 'Chỉnh sửa unit' : 'Thêm unit mới'}
              </h3>
              <button className="rounded-lg border border-[#dcc0bf]/40 px-3 py-2 text-sm font-bold text-[#4b0009] hover:bg-[#fff7f7]" onClick={closeUnitEditor} type="button">
                Đóng
              </button>
            </div>
            <div className="mt-4 grid gap-4 lg:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Tiêu đề</span>
                <input value={unitForm.title} onChange={(event) => setUnitForm({ ...unitForm, title: event.target.value })} className={FIELD_CLASS} />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Thứ tự</span>
                <input type="number" min="0" value={unitForm.displayOrder} onChange={(event) => setUnitForm({ ...unitForm, displayOrder: event.target.value })} className={FIELD_CLASS} />
              </label>
            </div>
            <div className="mt-4 space-y-4">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Mô tả</span>
                <RichTextEditor
                  helperText=""
                  onChange={(value) => setUnitForm({ ...unitForm, description: value })}
                  placeholder="Mô tả unit..."
                  size="compact"
                  value={unitForm.description}
                />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Kế hoạch buổi học</span>
                <RichTextEditor
                  helperText=""
                  onChange={(value) => setUnitForm({ ...unitForm, sessionPlan: value })}
                  placeholder="Kế hoạch buổi học..."
                  size="form"
                  value={unitForm.sessionPlan}
                />
              </label>
            </div>
            <div className="mt-5 flex flex-wrap gap-2 border-t border-[#dcc0bf]/20 pt-4">
              <button type="button" onClick={saveUnit} disabled={working || !selectedProgramId} className="inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#730014] disabled:opacity-60">
                <Save className="h-4 w-4" /> Lưu unit
              </button>
              <button type="button" onClick={resetUnitForm} className="inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf]/40 px-4 py-2.5 text-sm font-bold text-[#4b0009] hover:bg-[#fff7f7]">
                <Plus className="h-4 w-4" /> Mới
              </button>
            </div>
          </section>
        </UnitEditorModal>
      )}

      {resourcePanelOpen && (
        <ResourceAttachModal onClose={closeResourcePanel}>
          <section className="space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Gắn tài nguyên vào unit</h3>
                <p className="mt-1 text-sm font-semibold text-[#584140]">
                  {selectedAttachUnit ? `${selectedAttachUnit.displayOrder}. ${selectedAttachUnit.title}` : 'Chọn unit nhận nội dung'}
                </p>
              </div>
              <button className="rounded-lg border border-[#dcc0bf]/40 px-3 py-2 text-sm font-bold text-[#4b0009] hover:bg-[#fff7f7]" onClick={closeResourcePanel} type="button">
                Đóng
              </button>
            </div>
            <div className="mt-4 grid gap-4 lg:grid-cols-4">
              <FieldSelect label="Unit nhận tài nguyên" value={attachForm.unitId} onChange={selectAttachUnit} options={unitOptions} placeholder="Chọn unit" />
              <FieldSelect label="Loại tài nguyên" value={attachForm.type} onChange={(value) => setAttachForm({ ...attachForm, type: value, resourceId: '' })} options={typeOptions} />
              <FieldSelect label="Tài nguyên" value={attachForm.resourceId} onChange={(value) => setAttachForm({ ...attachForm, resourceId: value })} options={resourceOptions} placeholder={resourceOptions.length ? 'Chọn tài nguyên' : 'Kho này đang trống'} disabled={!resourceOptions.length} />
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Thứ tự</span>
                <input type="number" min="0" value={attachForm.displayOrder} onChange={(event) => setAttachForm({ ...attachForm, displayOrder: event.target.value })} className={FIELD_CLASS} />
              </label>
              <label className="block lg:col-span-4">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Ghi chú</span>
                <textarea value={attachForm.note} onChange={(event) => setAttachForm({ ...attachForm, note: event.target.value })} rows={3} className={TEXTAREA_CLASS} />
              </label>
            </div>
            {selectedResource ? (
              <div className="mt-4 border-l-4 border-[#8a0018] bg-[#fbf3f4] px-4 py-3">
                <p className="text-sm font-extrabold text-[#26364a]">{selectedResource.title}</p>
                <p className="mt-1 text-xs leading-5 text-[#584140]">
                  {[...new Set([getReadableResourceText(selectedResource.description), getReadableResourceText(selectedResource.prompt), selectedResource.skill, selectedResource.examCategory]
                    .filter(Boolean))].join(' · ') || 'Tài nguyên đã sẵn sàng để gắn vào unit.'}
                </p>
              </div>
            ) : null}
            {!resourceOptions.length && selectedAttachUnit ? (
              <p className="mt-4 border border-[#dcc0bf]/30 bg-[#fcfbfb] px-4 py-3 text-sm font-semibold text-[#584140]">
                Tất cả tài nguyên thuộc loại này đã được gắn vào unit, hoặc kho tài nguyên đang trống.
              </p>
            ) : null}
            <button type="button" onClick={attachResource} disabled={working || !attachForm.unitId || !attachForm.resourceId} className="mt-5 inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#730014] disabled:opacity-60">
              <Link2 className="h-4 w-4" /> Gắn tài nguyên
            </button>
            {selectedAttachUnit ? (
              <div className="mt-6 border-t border-[#dcc0bf]/25 pt-5">
                <h4 className="text-xs font-extrabold uppercase tracking-[0.12em] text-[#8b706e]">Nội dung hiện có trong unit</h4>
                <UnitResourceGroups
                  onDetach={detachResource}
                  unit={selectedAttachUnit}
                  working={working}
                />
              </div>
            ) : null}
          </section>
        </ResourceAttachModal>
      )}

      {resourceDetailUnit ? (
        <UnitResourceDetailModal onClose={() => setResourceDetailUnitId(null)}>
          <section>
            <div className="flex items-start justify-between gap-4 border-b border-[#dcc0bf]/30 pb-5">
              <div className="min-w-0">
                <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8e7371]">Tài nguyên của unit</p>
                <h3 className="mt-2 font-['Manrope'] text-xl font-extrabold text-[#0b1c30]">
                  {resourceDetailUnit.displayOrder ?? 0}. {resourceDetailUnit.title}
                </h3>
                {resourceDetailUnit.description ? (
                  <p className="mt-2 text-sm leading-6 text-[#584140]">{resourceDetailUnit.description}</p>
                ) : null}
              </div>
              <button
                aria-label="Đóng danh sách tài nguyên"
                className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-[#dcc0bf]/50 bg-white text-[#4b0009] transition hover:bg-[#fff1f3]"
                onClick={() => setResourceDetailUnitId(null)}
                type="button"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <UnitResourceGroups
              onDetach={detachResource}
              unit={resourceDetailUnit}
              working={working}
            />
          </section>
        </UnitResourceDetailModal>
      ) : null}

              <section className="overflow-hidden rounded-xl border border-[#dcc0bf]/30 bg-white shadow-sm">
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#dcc0bf]/25 px-6 py-4">
                  <div>
                    <h3 className="font-['Manrope'] text-xl font-extrabold text-[#0b1c30]">Cấu trúc unit</h3>
                    <p className="mt-1 text-sm text-[#584140]">{filteredUnits.length} unit phù hợp.</p>
                  </div>
                  <button type="button" onClick={resetUnitForm} className="inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#730014]">
                    <Plus className="h-4 w-4" /> Thêm unit
                  </button>
                </div>

                {loading ? (
                  <div className="p-6 text-sm font-semibold text-slate-500">Đang tải...</div>
                ) : !selectedProgramId ? (
                  <div className="p-8 text-center text-sm font-semibold text-[#584140]">Chọn giáo trình để bắt đầu biên soạn.</div>
                ) : filteredUnits.length === 0 ? (
                  <div className="p-8 text-center text-sm font-semibold text-[#584140]">Giáo trình này chưa có unit/buổi học.</div>
                ) : (
                  <>
                    <div className="overflow-x-auto">
                      <table className="w-full min-w-[860px] border-collapse text-left">
                        <thead>
                          <tr className="border-b border-[#dcc0bf]/30 bg-[#fbf3f4]">
                            {['Unit', 'Kế hoạch', 'Tài nguyên', 'Thao tác'].map((heading) => (
                              <th className={`px-6 py-4 text-xs font-bold uppercase tracking-[0.12em] text-[#8e7371] ${heading === 'Thao tác' ? 'text-right' : ''}`} key={heading}>{heading}</th>
                            ))}
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-[#dcc0bf]/15">
                          {pageItems.map((unit) => {
                            const resourceCount = refGroups.reduce((total, group) => total + (unit[group.key]?.length || 0), 0);
                            return (
                              <tr className="align-top transition hover:bg-[#eff4ff]" key={unit.id}>
                                <td className="px-6 py-5">
                                  <p className="text-sm font-extrabold text-[#4b0009]">{unit.displayOrder ?? 0}. {unit.title}</p>
                                  {unit.description ? <p className="mt-1 max-w-[360px] text-xs leading-5 text-[#584140]">{unit.description}</p> : null}
                                </td>
                                <td className="px-6 py-5 text-sm leading-6 text-[#564241]">
                                  <p className="max-w-[320px] whitespace-pre-wrap">{unit.sessionPlan || 'Chưa có kế hoạch buổi học.'}</p>
                                </td>
                                <td className="px-6 py-5">
                                  <span className="inline-flex rounded-lg border border-[#dcc0bf]/40 bg-[#dce9ff] px-2.5 py-1 text-[11px] font-bold uppercase tracking-[0.08em] text-[#564241]">
                                    {resourceCount} tài nguyên
                                  </span>
                                  <div className="mt-2 flex flex-wrap gap-1.5">
                                    {refGroups.map((group) => (
                                      <span className="rounded-md bg-[#fbf3f4] px-2 py-1 text-[10px] font-bold text-[#8e7371]" key={group.key}>
                                        {group.title}: {unit[group.key]?.length || 0}
                                      </span>
                                    ))}
                                  </div>
                                  {resourceCount ? (
                                    <button
                                      className="mt-3 inline-flex items-center gap-1.5 text-xs font-extrabold text-[#730014] hover:text-[#4b0009]"
                                      onClick={() => setResourceDetailUnitId(String(unit.id))}
                                      type="button"
                                    >
                                      Xem tài nguyên
                                    </button>
                                  ) : null}
                                </td>
                                <td className="px-6 py-5 text-right">
                                  <div className="flex justify-end gap-2">
                                    <button type="button" onClick={() => openEditUnit(unit)} className="rounded-lg border border-[#dcc0bf]/50 px-3 py-1.5 text-xs font-bold text-[#4b0009] transition hover:bg-[#fff7f7]">Sửa</button>
                                    <button type="button" onClick={() => openResourcePanel(unit.id)} className="rounded-lg border border-[#dcc0bf]/40 px-3 py-1.5 text-xs font-bold text-[#4b0009] transition hover:bg-[#fff7f7]">Gắn</button>
                                    <button type="button" onClick={() => deleteUnit(unit)} disabled={working} className="rounded-lg border border-rose-200 px-3 py-1.5 text-xs font-bold text-rose-700 transition hover:bg-rose-50 disabled:opacity-50">
                                      <Trash2 className="h-3.5 w-3.5" />
                                    </button>
                                  </div>
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                    <div className="border-t border-[#dcc0bf]/20 bg-[#fbf3f4]/40 px-6 py-4">
                      <Pagination page={page} totalPages={totalPages} onChange={setPage} totalItems={totalItems} pageSize={UNIT_PAGE_SIZE} />
                    </div>
                  </>
                )}
              </section>
          </div>
        </>
      )}
    </div>
  );
}

function InfoTile({ label, value }) {
  return (
    <div className="rounded-xl border border-[#dcc0bf]/25 bg-[#fcfbfb] px-4 py-3">
      <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8b706e]">{label}</p>
      <p className="mt-2 break-words text-sm font-extrabold text-[#0b1c30]">{value}</p>
    </div>
  );
}

function FieldSelect({ label, value, options, onChange, placeholder, disabled }) {
  return (
    <div>
      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">{label}</span>
      <BrandedSelect
        value={value}
        onChange={(event) => onChange(event.target.value)}
        options={options}
        placeholder={placeholder}
        disabled={disabled}
      />
    </div>
  );
}

function SyllabusProgramCreateModal({ error, form, mode = 'create', onChange, onClose, onSubmit, saving }) {
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
      <div className="relative z-10 flex max-h-[calc(100dvh-2rem)] w-full max-w-[760px] min-h-0 flex-col overflow-hidden rounded-xl bg-white shadow-2xl sm:max-h-[calc(100dvh-3rem)]">
        <form className="flex min-h-0 flex-1 flex-col" onSubmit={onSubmit}>
          <div className="border-b border-[#dcc0bf]/20 p-5">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Biên soạn giáo trình</p>
              <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#0b1c30]">{editing ? 'Chỉnh sửa thông tin giáo trình' : 'Tạo giáo trình mới'}</h2>
              <p className="mt-2 text-sm leading-6 text-[#584140]">
                {editing ? 'Cập nhật thông tin chung của giáo trình. Các unit và tài nguyên đã biên soạn vẫn được giữ nguyên.' : 'Tạo khung giáo trình trước, sau đó thêm unit/buổi học và gắn tài nguyên ngay trong trang này.'}
              </p>
            </div>
          </div>

          <div className="min-h-0 flex-1 space-y-4 overflow-y-auto overscroll-contain p-5">
            {error ? <div className={ERROR_NOTICE_CLASS} role="alert">{error}</div> : null}
            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Tên giáo trình</span>
              <input className={FIELD_CLASS} onChange={(event) => onChange({ title: event.target.value })} value={form.title} />
            </label>
            <div className="grid gap-4 md:grid-cols-2">
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Mã</span>
                <input className={FIELD_CLASS} onChange={(event) => onChange({ code: event.target.value })} value={form.code} />
              </label>
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Slug</span>
                <input className={FIELD_CLASS} onChange={(event) => onChange({ slug: event.target.value })} value={form.slug} />
              </label>
              <FieldSelect label="Hình thức" onChange={(value) => onChange({ deliveryMode: value })} options={deliveryModeOptions} value={form.deliveryMode} />
              <FieldSelect
                label="Nhóm thi"
                onChange={(value) => onChange({ examCategory: value, ...getEnglishProfileDefaults(value) })}
                options={ENGLISH_EXAM_OPTIONS}
                value={form.examCategory}
              />
              <FieldSelect label="Loại chương trình" onChange={(value) => onChange({ programTrack: value })} options={ENGLISH_TRACK_OPTIONS[form.examCategory]} value={form.programTrack} />
              {form.examCategory === 'IELTS' ? (
                <IeltsBandSelect
                  label="Band IELTS mục tiêu"
                  onChange={(value) => onChange({ targetBand: value })}
                  value={form.targetBand}
                />
              ) : null}
              {form.examCategory === 'TOEIC' ? (
                <ToeicScoreField
                  label="Điểm TOEIC mục tiêu"
                  onChange={(value) => onChange({ targetScore: value })}
                  value={form.targetScore}
                />
              ) : null}
              <EnglishEntryLevelField
                examCategory={form.examCategory}
                onChange={(value) => onChange({ entryLevel: value })}
                value={form.entryLevel}
              />
              <label className="block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Tổng số buổi dự kiến</span>
                <input className={FIELD_CLASS} min="0" onChange={(event) => onChange({ totalSessions: event.target.value })} type="number" value={form.totalSessions} />
              </label>
            </div>
            <div>
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Kỹ năng trọng tâm</span>
              <div className="flex flex-wrap gap-2">
                {ENGLISH_SKILL_OPTIONS.map((skill) => {
                  const selected = form.focusSkills.includes(skill.value);
                  return (
                    <button
                      className={`rounded-full border px-3 py-2 text-xs font-bold transition ${selected ? 'border-[#730014] bg-[#730014] text-white' : 'border-[#dcc0bf] bg-white text-[#584140]'}`}
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
            <label className="block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Chuẩn đầu ra</span>
              <RichTextEditor
                helperText=""
                onChange={(value) => onChange({ outcomes: value })}
                placeholder="Chuẩn đầu ra của chương trình..."
                size="form"
                value={form.outcomes}
              />
            </label>
          </div>

          <div className="flex flex-wrap justify-end gap-3 border-t border-[#dcc0bf]/20 p-5">
            <button className="rounded-lg border border-[#dcc0bf]/40 px-4 py-2.5 text-sm font-bold text-[#4b0009] disabled:opacity-50" disabled={saving} onClick={onClose} type="button">Hủy</button>
            <button className="inline-flex items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white disabled:opacity-60" disabled={saving} type="submit">
              {editing ? <Save className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
              {saving ? 'Đang lưu...' : (editing ? 'Lưu thay đổi' : 'Tạo và biên soạn')}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}

function SyllabusProgramListPanel({ programs, loading, onCreate, onOpen, onRefresh }) {
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [modeFilter, setModeFilter] = useState('ALL');
  const [listPage, setListPage] = useState(1);

  const statusOptions = useMemo(() => {
    const values = [...new Set(programs.map((item) => item.status).filter(Boolean))];
    return [{ label: 'Trạng thái: Tất cả', value: 'ALL' }, ...values.map((value) => ({ label: `Trạng thái: ${formatStatusLabel(value)}`, value }))];
  }, [programs]);

  const modeOptions = useMemo(() => {
    const values = [...new Set(programs.map((item) => item.deliveryMode).filter(Boolean))];
    return [{ label: 'Hình thức: Tất cả', value: 'ALL' }, ...values.map((value) => ({ label: `Hình thức: ${formatDeliveryMode(value)}`, value }))];
  }, [programs]);

  const filteredPrograms = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return programs.filter((item) => {
      const status = item.status || '';
      const statusMatched = statusFilter === 'ALL' || status === statusFilter;
      const modeMatched = modeFilter === 'ALL' || item.deliveryMode === modeFilter;
      const haystack = [
        item.title,
        item.code,
        item.slug,
        item.examCategory,
        item.entryLevel,
        item.targetBand,
        item.targetScore,
        item.deliveryModeLabel,
        item.deliveryMode,
      ].filter(Boolean).join(' ').toLowerCase();
      return statusMatched && modeMatched && (!normalizedKeyword || haystack.includes(normalizedKeyword));
    });
  }, [programs, keyword, modeFilter, statusFilter]);

  const totalListPages = Math.max(1, Math.ceil(filteredPrograms.length / PAGE_SIZE));
  const visiblePrograms = filteredPrograms.slice((listPage - 1) * PAGE_SIZE, listPage * PAGE_SIZE);

  const stats = useMemo(() => {
    const publishedCount = programs.filter((item) => item.status === 'PUBLISHED').length;
    const draftCount = programs.filter((item) => item.status === 'DRAFT').length;
    const totalUnits = programs.reduce((total, item) => total + Number(item.totalUnits ?? item.unitCount ?? item.units?.length ?? 0), 0);
    return [
      { label: 'Tổng giáo trình', value: programs.length, icon: BookMarked, tone: 'text-[#4b0009]' },
      { label: 'Có unit nội dung', value: programs.filter((item) => Number(item.totalUnits ?? item.unitCount ?? item.units?.length ?? 0) > 0).length, icon: Check, tone: 'text-emerald-700' },
      { label: 'Bản nháp', value: draftCount, icon: GraduationCap, tone: 'text-amber-700' },
      { label: 'Tổng unit', value: totalUnits, icon: Link2, tone: 'text-[#005236]' },
    ];
  }, [programs]);

  useEffect(() => {
    setListPage(1);
  }, [keyword, modeFilter, statusFilter]);

  useEffect(() => {
    if (listPage > totalListPages) setListPage(totalListPages);
  }, [listPage, totalListPages]);

  return (
    <div className="space-y-6">
      <HeaderActions>
        <button className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#730014] active:scale-[0.98]" onClick={onCreate} type="button">
          <Plus className="h-4 w-4" />
          Tạo giáo trình nội dung
        </button>
      </HeaderActions>

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
                placeholder="Tìm giáo trình, mã, danh mục hoặc target..."
                className="w-full rounded-lg border border-[#dcc0bf]/50 bg-[#f8f9ff] py-2 pl-10 pr-4 text-sm text-[#0b1c30] outline-none transition focus:border-[#4b0009] focus:bg-white focus:ring-4 focus:ring-[#4b0009]/5"
              />
            </div>
          </div>
          <div className="w-full sm:w-[220px]">
            <BrandedSelect
              onChange={(event) => setModeFilter(event.target.value)}
              options={modeOptions}
              value={modeFilter}
            />
          </div>
          <div className="w-full sm:w-[220px]">
            <BrandedSelect
              onChange={(event) => setStatusFilter(event.target.value)}
              options={statusOptions}
              value={statusFilter}
            />
          </div>
          <button
            aria-label="Làm mới danh sách giáo trình"
            type="button"
            onClick={onRefresh}
            className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#dcc0bf]/40 text-[#564241] transition hover:bg-[#eff4ff]"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </section>

      <section className="overflow-hidden rounded-xl border border-[#dcc0bf]/30 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[1080px] border-collapse text-left">
            <thead>
              <tr className="border-b border-[#dcc0bf]/30 bg-[#fbf3f4]">
                {['Giáo trình nội dung', 'Unit/Buổi học', 'Dùng làm mẫu', 'Lớp đang dùng', 'Trạng thái', 'Cập nhật', 'Thao tác'].map((heading) => (
                  <th
                    className={`px-6 py-4 text-xs font-bold uppercase tracking-[0.12em] text-[#8e7371] ${heading === 'Unit/Buổi học' ? 'text-center' : ''} ${heading === 'Thao tác' ? 'text-right' : ''}`}
                    key={heading}
                  >
                    {heading}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#dcc0bf]/15">
              {visiblePrograms.map((item) => (
                <tr className="transition hover:bg-[#eff4ff]" key={item.id}>
                  <td className="px-6 py-5">
                    <div className="min-w-0">
                      <p className="max-w-[300px] overflow-hidden text-sm font-bold leading-5 text-[#4b0009] [display:-webkit-box] [-webkit-box-orient:vertical] [-webkit-line-clamp:2]">{item.title}</p>
                      <p className="mt-1 text-xs text-[#584140]">{item.code || item.slug || '-'}</p>
                    </div>
                  </td>
                  <td className="px-6 py-5 text-center text-sm font-semibold text-[#0b1c30]">
                    {Number(item.totalUnits ?? item.unitCount ?? item.units?.length ?? 0)} / {item.totalSessions ?? item.sessionCount ?? '-'}
                  </td>
                  <td className="px-6 py-5 text-sm text-[#564241]">{item.deliveryModeLabel || formatDeliveryMode(item.deliveryMode)}</td>
                  <td className="px-6 py-5 text-sm text-[#564241]">{item.activeClassroomCount ?? item.classroomUsageCount ?? 0}</td>
                  <td className="px-6 py-5"><StatusBadge value={item.status} /></td>
                  <td className="px-6 py-5 text-sm text-[#564241]">{item.updatedAt ? new Date(item.updatedAt).toLocaleDateString('vi-VN') : '-'}</td>
                  <td className="px-6 py-5 text-right">
                    <button
                      className="inline-flex items-center gap-1.5 rounded-lg border border-[#dcc0bf]/50 px-3 py-1.5 text-xs font-bold text-[#4b0009] transition hover:bg-[#fff7f7]"
                      onClick={() => onOpen(item)}
                      type="button"
                    >
                      Mở biên soạn
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {!filteredPrograms.length ? (
          <div className="border-t border-[#dcc0bf]/20 px-6 py-10 text-center text-sm font-semibold text-[#584140]">
            Không có giáo trình phù hợp với bộ lọc hiện tại.
          </div>
        ) : (
          <div className="border-t border-[#dcc0bf]/20 bg-[#fbf3f4]/40 px-6 py-4">
            <Pagination
              page={listPage}
              totalPages={totalListPages}
              onChange={setListPage}
              totalItems={filteredPrograms.length}
              pageSize={PAGE_SIZE}
            />
          </div>
        )}
      </section>
    </div>
  );
}

function UnitResourceGroups({ onDetach, unit, working }) {
  const total = refGroups.reduce((sum, group) => sum + (unit?.[group.key]?.length || 0), 0);
  if (!total) {
    return <p className="mt-3 text-sm font-semibold text-[#69778a]">Unit này chưa có tài nguyên nào.</p>;
  }

  return (
    <div className="mt-5 space-y-5">
      {refGroups.map((group) => {
        const references = unit?.[group.key] || [];
        if (!references.length) return null;
        return (
          <section className="overflow-hidden rounded-xl border border-[#dcc0bf]/35 bg-white" key={group.key}>
            <div className="flex items-center justify-between gap-3 border-b border-[#dcc0bf]/25 bg-[#fbf3f4] px-4 py-3">
              <h5 className="text-xs font-extrabold uppercase tracking-[0.1em] text-[#8b706e]">{group.title}</h5>
              <span className="text-xs font-bold text-[#69778a]">{references.length}</span>
            </div>
            <div className="divide-y divide-[#dcc0bf]/25 px-4">
              {references.map((reference) => (
                <div className="flex items-start justify-between gap-3 py-3" key={`${reference.type}-${reference.id}`}>
                  <div className="min-w-0">
                    <p className="text-sm font-extrabold text-[#26364a]">{reference.title || `Tài nguyên #${reference.resourceId}`}</p>
                    <p className="mt-1 text-xs leading-5 text-[#584140]">
                      {[reference.skill, getReadableResourceText(reference.subtitle), getReadableResourceText(reference.note)].filter(Boolean).join(' · ') || 'Không có ghi chú bổ sung.'}
                    </p>
                  </div>
                  <button
                    aria-label={`Gỡ ${reference.title || 'tài nguyên'} khỏi unit`}
                    className="inline-flex h-9 w-9 shrink-0 items-center justify-center border border-rose-200 text-rose-700 transition hover:bg-rose-50 disabled:opacity-50"
                    disabled={working}
                    onClick={() => onDetach(reference)}
                    title="Gỡ khỏi unit"
                    type="button"
                  >
                    <Trash2 className="h-4 w-4" />
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

function StatusBadge({ value }) {
  return (
    <span className="inline-flex rounded-lg border border-[#dcc0bf]/40 bg-[#fbf3f4] px-3 py-1.5 text-xs font-extrabold uppercase tracking-[0.08em] text-[#4b0009]">
      {formatStatusLabel(value)}
    </span>
  );
}

function formatStatusLabel(value) {
  const labels = {
    ACTIVE: 'Đang hoạt động',
    APPROVED: 'Đã duyệt',
    INACTIVE: 'Tạm ngừng',
    ONGOING: 'Đang hoạt động',
    UPCOMING: 'Sắp khai giảng',
    PUBLISHED: 'Đã xuất bản',
    DRAFT: 'Bản nháp',
    ARCHIVED: 'Đã lưu trữ',
    COMPLETED: 'Đã kết thúc',
    CANCELLED: 'Đã hủy',
  };
  return labels[value] || value || 'Chưa rõ';
}

function formatDeliveryMode(value) {
  const labels = {
    OFFLINE: 'Offline',
    VIRTUAL: 'Virtual',
    ONLINE: 'Online',
    HYBRID: 'Hybrid',
  };
  return labels[value] || value || '-';
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
      <div className="relative z-10 flex max-h-[calc(100dvh-2rem)] w-full max-w-[800px] min-h-0 flex-col overflow-hidden rounded-3xl border border-[#dcc0bf]/35 bg-[#fafafa] shadow-2xl sm:max-h-[calc(100dvh-3rem)]">
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-5 sm:p-6">{children}</div>
      </div>
    </div>,
    document.body
  );
}

function UnitResourceDetailModal({ children, onClose }) {
  return <UnitEditorModal onClose={onClose}>{children}</UnitEditorModal>;
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
      <div className="relative z-10 flex max-h-[calc(100dvh-2rem)] w-full max-w-[900px] min-h-0 flex-col overflow-hidden rounded-3xl border border-[#dcc0bf]/35 bg-[#fafafa] shadow-2xl sm:max-h-[calc(100dvh-3rem)]">
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-5 sm:p-6">{children}</div>
      </div>
    </div>,
    document.body
  );
}
