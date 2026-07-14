import { useEffect, useMemo, useRef, useState } from 'react';
import { AlertTriangle, ArrowDown, ArrowLeft, ArrowUp, CheckCircle2, GripVertical, Plus, Trash2, Upload, X, XCircle } from 'lucide-react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import curriculumApi from '../../api/curriculumApi';
import AssessmentExamBuilder from '../../components/content-manager/AssessmentExamBuilder';
import { Panel, StatusBadge, TextField } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import {
  IELTS_MAX_BAND,
  normalizeAssessmentMaxScore,
  normalizeAssessmentPassingScore,
  usesBandScale,
} from '../../utils/ieltsBandScale';

const COURSE_LEVEL_KEY = 'course';
const CONTENT_TYPE_OPTIONS = ['VIDEO', 'ARTICLE', 'ASSIGNMENT', 'QUIZ'];
const ASSESSMENT_TYPE_OPTIONS = ['MODULE_TEST', 'LESSON_PRACTICE', 'MOCK_TEST', 'WRITING_TASK', 'SPEAKING_TASK', 'QUIZ'];
const ASSESSMENT_SKILL_OPTIONS = ['LISTENING', 'READING', 'WRITING', 'SPEAKING', 'VOCABULARY', 'GRAMMAR', 'MIXED'];
const AI_MODE_OPTIONS = ['NONE', 'EXPLAIN_ONLY', 'RUBRIC_FEEDBACK', 'ESTIMATED_BAND'];

const createTempId = (prefix) => `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;

const reorder = (items, fromIndex, toIndex) => {
  if (fromIndex === toIndex || fromIndex < 0 || toIndex < 0) return items;
  const next = [...items];
  const [moved] = next.splice(fromIndex, 1);
  next.splice(toIndex, 0, moved);
  return next.map((item, index) => ({ ...item, displayOrder: index + 1 }));
};

const resolveModuleKey = (module) => String(module?.id ?? module?.tempId ?? COURSE_LEVEL_KEY);

const createAssessmentDraft = ({ moduleKey, moduleTitle = null, displayOrder = 1 }) => ({
  id: null,
  localKey: createTempId('assessment'),
  moduleKey,
  moduleTitle,
  assessmentBankItemId: '',
  rubricId: '',
  title: '',
  description: '',
  type: 'MODULE_TEST',
  skill: 'MIXED',
  aiEvaluationMode: 'EXPLAIN_ONLY',
  instructions: '',
  objectiveAnswerKey: '',
  uiConfigJson: '',
  passingScore: '',
  maxScore: '9',
  timeLimitMinutes: '',
  displayOrder,
  active: true,
});

const createAssessmentDraftFromBank = ({ bankItem, moduleKey, moduleTitle = null, displayOrder = 1 }) => ({
  id: null,
  localKey: createTempId('assessment'),
  moduleKey,
  moduleTitle,
  assessmentBankItemId: String(bankItem.id),
  rubricId: bankItem.rubric?.id ? String(bankItem.rubric.id) : '',
  title: bankItem.title || '',
  description: bankItem.description || '',
  type: bankItem.type || 'MODULE_TEST',
  skill: bankItem.skill || 'MIXED',
  aiEvaluationMode: bankItem.aiEvaluationMode || 'NONE',
  instructions: bankItem.instructions || '',
  objectiveAnswerKey: bankItem.objectiveAnswerKey || '',
  uiConfigJson: bankItem.uiConfigJson || extractEmbeddedUiConfig(bankItem.instructions),
  passingScore: normalizeScalar(bankItem.passingScore),
  maxScore: normalizeScalar(bankItem.maxScore, '9'),
  timeLimitMinutes: normalizeScalar(bankItem.timeLimitMinutes),
  displayOrder,
  active: true,
});

const normalizeScalar = (value, fallback = '') => (value == null ? fallback : String(value));

const normalizeTranscriptSegments = (segments, keepEmpty = false) => (Array.isArray(segments) ? segments : [])
  .map((segment) => ({
    startSeconds: Number(segment?.startSeconds),
    endSeconds: Number(segment?.endSeconds),
    text: keepEmpty ? String(segment?.text || '') : String(segment?.text || '').trim(),
  }))
  .filter((segment) => (keepEmpty || segment.text) && Number.isFinite(segment.startSeconds) && Number.isFinite(segment.endSeconds));

function extractEmbeddedUiConfig(instructions) {
  const marker = '[ENGLISHLAB_UI_CONFIG]';
  const text = String(instructions || '');
  const markerIndex = text.indexOf(marker);
  if (markerIndex < 0) return '';
  return text.slice(markerIndex + marker.length).trim();
}

const normalizeAssessmentStructure = (items, modules) => {
  const moduleById = new Map((modules || []).map((module) => [String(module.id), module]));
  return (items || []).map((assessment, index) => {
    const matchedModule = assessment.moduleId == null ? null : moduleById.get(String(assessment.moduleId));
    return {
      id: assessment.id ?? null,
      localKey: assessment.id ? `assessment-${assessment.id}` : createTempId('assessment'),
      moduleKey: matchedModule ? resolveModuleKey(matchedModule) : COURSE_LEVEL_KEY,
      moduleTitle: matchedModule?.title || assessment.moduleTitle || null,
      assessmentBankItemId: assessment.assessmentBankItemId ? String(assessment.assessmentBankItemId) : '',
      rubricId: assessment.rubric?.id ? String(assessment.rubric.id) : '',
      title: assessment.title || '',
      description: assessment.description || '',
      type: assessment.type || 'MODULE_TEST',
      skill: assessment.skill || 'MIXED',
      aiEvaluationMode: assessment.aiEvaluationMode || 'EXPLAIN_ONLY',
      instructions: assessment.instructions || '',
      objectiveAnswerKey: assessment.objectiveAnswerKey || '',
      uiConfigJson: assessment.uiConfigJson || extractEmbeddedUiConfig(assessment.instructions),
      passingScore: normalizeScalar(assessment.passingScore),
      maxScore: normalizeScalar(assessment.maxScore, '9'),
      timeLimitMinutes: normalizeScalar(assessment.timeLimitMinutes),
      displayOrder: assessment.displayOrder ?? index + 1,
      active: assessment.active !== false,
    };
  });
};

const buildAssessmentPayload = (items, localModules, persistedModules) => {
  const moduleIdByKey = new Map(
    (localModules || []).map((module, index) => [resolveModuleKey(module), persistedModules?.[index]?.id ?? module.id ?? null]),
  );

  return (items || []).map((assessment, index) => ({
    id: assessment.id || null,
    moduleId: assessment.moduleKey === COURSE_LEVEL_KEY ? null : moduleIdByKey.get(assessment.moduleKey) ?? null,
    assessmentBankItemId: assessment.assessmentBankItemId ? Number(assessment.assessmentBankItemId) : null,
    rubricId: assessment.rubricId ? Number(assessment.rubricId) : null,
    title: assessment.title?.trim() || `Bài đánh giá ${index + 1}`,
    description: assessment.description?.trim() || '',
    type: assessment.type || 'MODULE_TEST',
    skill: assessment.skill || 'MIXED',
    aiEvaluationMode: assessment.aiEvaluationMode || 'EXPLAIN_ONLY',
    instructions: ['LISTENING', 'READING'].includes(String(assessment.skill || '').toUpperCase())
      ? ''
      : assessment.instructions?.trim() || '',
    objectiveAnswerKey: assessment.objectiveAnswerKey?.trim() || '',
    uiConfigJson: assessment.uiConfigJson?.trim() || null,
    passingScore: normalizeAssessmentPassingScore(assessment),
    maxScore: normalizeAssessmentMaxScore(assessment),
    timeLimitMinutes: assessment.timeLimitMinutes === '' ? null : Number(assessment.timeLimitMinutes),
    displayOrder: Number(assessment.displayOrder || index + 1),
    active: assessment.active !== false,
  }));
};

export default function ContentManagerCourseBuilderPage() {
  const { slugOrId } = useParams();
  const [searchParams] = useSearchParams();
  const [course, setCourse] = useState(null);
  const [assessments, setAssessments] = useState([]);
  const [rubrics, setRubrics] = useState([]);
  const [assessmentBankItems, setAssessmentBankItems] = useState([]);
  const [flashcardSets, setFlashcardSets] = useState([]);
  const [selectedModuleBankAssessmentId, setSelectedModuleBankAssessmentId] = useState('');
  const [selectedCourseBankAssessmentId, setSelectedCourseBankAssessmentId] = useState('');
  const [selectedLessonFlashcardSetId, setSelectedLessonFlashcardSetId] = useState('');
  const [activeModuleIndex, setActiveModuleIndex] = useState(0);
  const [activeLessonIndex, setActiveLessonIndex] = useState(0);
  const [dragState, setDragState] = useState(null);
  const [lessonModalOpen, setLessonModalOpen] = useState(false);
  const [uploadFile, setUploadFile] = useState(null);
  const [uploadingVideo, setUploadingVideo] = useState(false);
  const [refreshingTranscript, setRefreshingTranscript] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [toasts, setToasts] = useState([]);
  const handledRouteTargetRef = useRef('');

  const pushToast = (message, type = 'success') => {
    const id = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    setToasts((current) => [...current, { id, message, type }]);
    window.setTimeout(() => {
      setToasts((current) => current.filter((toast) => toast.id !== id));
    }, 3600);
  };

  const dismissToast = (id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  };

  useEffect(() => {
    let active = true;

    const loadBuilder = async () => {
      try {
        const [courseData, rubricItems, bankItems, flashcardItems] = await Promise.all([
          courseApi.getManagedOnlineCourse(slugOrId),
          courseApi.getManagedAssessmentRubrics(),
          curriculumApi.getAssessmentBank(),
          curriculumApi.getFlashcardSets(),
        ]);
        if (!active) return;

        const normalizedCourse = normalizeCourseStructure(courseData);
        setCourse(normalizedCourse);
        setRubrics(Array.isArray(rubricItems) ? rubricItems : []);
        setAssessmentBankItems((Array.isArray(bankItems) ? bankItems : []).filter((item) => item.status !== 'ARCHIVED'));
        setFlashcardSets((Array.isArray(flashcardItems) ? flashcardItems : []).filter((item) => item.status !== 'ARCHIVED'));

        if (!normalizedCourse.id) {
          setAssessments([]);
          return;
        }

        const assessmentItems = await courseApi.getManagedCourseAssessments(normalizedCourse.id);
        if (!active) return;
        setAssessments(normalizeAssessmentStructure(assessmentItems, normalizedCourse.modules));
      } catch {
        if (active) setError('Không tải được dữ liệu builder.');
      }
    };

    loadBuilder();

    return () => {
      active = false;
    };
  }, [slugOrId]);

  useEffect(() => {
    if (!course) return;
    const assessmentId = searchParams.get('assessmentId');
    const moduleId = searchParams.get('moduleId');
    if (!assessmentId && !moduleId) return;

    const routeTarget = `${slugOrId}:${assessmentId || ''}:${moduleId || ''}`;
    if (handledRouteTargetRef.current === routeTarget) return;

    const assessment = assessmentId
      ? assessments.find((item) => String(item.id) === String(assessmentId))
      : null;
    if (assessmentId && !assessment) return;
    const targetModuleKey = assessment?.moduleKey
      || (moduleId ? String(moduleId) : COURSE_LEVEL_KEY);
    const moduleIndex = course.modules.findIndex((module) => resolveModuleKey(module) === targetModuleKey);
    if (moduleIndex >= 0) {
      setActiveModuleIndex(moduleIndex);
      setActiveLessonIndex(0);
    }

    handledRouteTargetRef.current = routeTarget;

    window.setTimeout(() => {
      const targetId = assessmentId ? `assessment-editor-${assessmentId}` : 'course-assessments';
      document.getElementById(targetId)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 180);
  }, [assessments, course, searchParams]);

  const modules = course?.modules || [];
  const activeModule = modules[activeModuleIndex] || null;
  const lessons = activeModule?.lessons || [];
  const activeLesson = lessons[activeLessonIndex] || null;
  const activeModuleKey = resolveModuleKey(activeModule);
  const moduleAssessments = assessments.filter((assessment) => assessment.moduleKey === activeModuleKey);
  const courseLevelAssessments = assessments.filter((assessment) => assessment.moduleKey === COURSE_LEVEL_KEY);
  const assessmentBankOptions = useMemo(
    () => buildAssessmentBankOptions(assessmentBankItems),
    [assessmentBankItems],
  );
  const flashcardSetOptions = useMemo(
    () => buildFlashcardSetOptions(flashcardSets),
    [flashcardSets],
  );

  const totalLessons = useMemo(
    () => modules.reduce((sum, module) => sum + (module.lessons?.length || 0), 0),
    [modules],
  );
  const totalHours = useMemo(
    () => Math.max(1, Math.ceil(modules.reduce(
      (sum, module) => sum + (module.lessons || []).reduce((lessonSum, lesson) => lessonSum + Number(lesson.durationMinutes || 0), 0),
      0,
    ) / 60)),
    [modules],
  );

  const updateModule = (field) => (event) => {
    const value = event.target.value;
    setCourse((current) => {
      if (!current) return current;
      return {
        ...current,
        modules: current.modules.map((module, index) => (index === activeModuleIndex ? { ...module, [field]: value } : module)),
      };
    });
  };

  const updateLesson = (field) => (event) => {
    const value = field === 'preview' ? event.target.checked : event.target.value;
    setCourse((current) => {
      if (!current) return current;
      return {
        ...current,
        modules: current.modules.map((module, moduleIndex) => {
          if (moduleIndex !== activeModuleIndex) return module;
          return {
            ...module,
            lessons: (module.lessons || []).map((lesson, lessonIndex) => (
              lessonIndex === activeLessonIndex ? { ...lesson, [field]: value } : lesson
            )),
          };
        }),
      };
    });
  };

  const patchActiveLesson = (patch) => {
    setCourse((current) => {
      if (!current) return current;
      return {
        ...current,
        modules: current.modules.map((module, moduleIndex) => {
          if (moduleIndex !== activeModuleIndex) return module;
          return {
            ...module,
            lessons: (module.lessons || []).map((lesson, lessonIndex) => (
              lessonIndex === activeLessonIndex ? { ...lesson, ...patch } : lesson
            )),
          };
        }),
      };
    });
  };

  const addModule = () => {
    setCourse((current) => {
      if (!current) return current;
      return {
        ...current,
        modules: [
          ...current.modules,
          {
            tempId: createTempId('module'),
            title: `Mô-đun ${current.modules.length + 1}`,
            description: '',
            displayOrder: current.modules.length + 1,
            lessons: [],
          },
        ],
      };
    });
    setActiveModuleIndex(modules.length);
    setActiveLessonIndex(0);
    pushToast('Đã thêm mô-đun mới. Bấm Lưu thay đổi trình xây dựng để lưu xuống hệ thống.');
  };

  const addLesson = () => {
    if (!activeModule) {
      pushToast('Hãy chọn hoặc tạo mô-đun trước khi thêm bài học.', 'warning');
      return;
    }

    setCourse((current) => {
      if (!current) return current;
      return {
        ...current,
        modules: current.modules.map((module, moduleIndex) => {
          if (moduleIndex !== activeModuleIndex) return module;
          const currentLessons = module.lessons || [];
          return {
            ...module,
            lessons: [
              ...currentLessons,
              {
                tempId: createTempId('lesson'),
                title: `Bài học ${activeModuleIndex + 1}.${currentLessons.length + 1}`,
                description: '',
                contentType: 'VIDEO',
                contentText: '',
                videoUrl: '',
                materialUrl: '',
                transcriptSegments: [],
                flashcardSets: [],
                durationMinutes: '',
                displayOrder: currentLessons.length + 1,
                preview: false,
              },
            ],
          };
        }),
      };
    });
    setActiveLessonIndex(lessons.length);
    setLessonModalOpen(true);
    pushToast('Đã thêm bài học mới. Điền nội dung rồi bấm Lưu thay đổi trình xây dựng.');
  };

  const addAssessment = (scope = 'module') => {
    if (scope === 'module' && !activeModule) {
      pushToast('Hãy chọn mô-đun trước khi thêm bài đánh giá.', 'warning');
      return;
    }

    setAssessments((current) => {
      const groupKey = scope === 'module' ? activeModuleKey : COURSE_LEVEL_KEY;
      const existingCount = current.filter((item) => item.moduleKey === groupKey).length;
      return [
        ...current,
        createAssessmentDraft({
          moduleKey: groupKey,
          moduleTitle: scope === 'module' ? activeModule?.title || 'Mô-đun hiện tại' : null,
          displayOrder: existingCount + 1,
        }),
      ];
    });

    pushToast(scope === 'module'
      ? 'Đã thêm bài đánh giá cuối mô-đun.'
      : 'Đã thêm bài đánh giá cuối khóa.');
  };

  const addAssessmentFromBank = (scope = 'module') => {
    if (scope === 'module' && !activeModule) {
      pushToast('Hãy chọn mô-đun trước khi thêm đề từ kho.', 'warning');
      return;
    }
    const selectedId = scope === 'module' ? selectedModuleBankAssessmentId : selectedCourseBankAssessmentId;
    const bankItem = assessmentBankItems.find((item) => String(item.id) === String(selectedId));
    if (!bankItem) {
      pushToast('Hãy chọn một đề trong ngân hàng đề.', 'warning');
      return;
    }

    const groupKey = scope === 'module' ? activeModuleKey : COURSE_LEVEL_KEY;
    const alreadyLinked = assessments.some((item) => (
      item.moduleKey === groupKey && String(item.assessmentBankItemId || '') === String(bankItem.id)
    ));
    if (alreadyLinked) {
      pushToast('Đề này đã được gắn trong khu vực đang chọn.', 'warning');
      return;
    }

    setAssessments((current) => {
      const existingCount = current.filter((item) => item.moduleKey === groupKey).length;
      return [
        ...current,
        createAssessmentDraftFromBank({
          bankItem,
          moduleKey: groupKey,
          moduleTitle: scope === 'module' ? activeModule?.title || 'Mô-đun hiện tại' : null,
          displayOrder: existingCount + 1,
        }),
      ];
    });

    if (scope === 'module') {
      setSelectedModuleBankAssessmentId('');
    } else {
      setSelectedCourseBankAssessmentId('');
    }
    pushToast(scope === 'module' ? 'Đã gắn đề từ kho vào mô-đun.' : 'Đã gắn đề từ kho vào cuối khóa.');
  };

  const addFlashcardSetToActiveLesson = () => {
    if (!activeLesson) {
      pushToast('Hãy chọn bài học trước khi gắn flashcard.', 'warning');
      return;
    }
    const set = flashcardSets.find((item) => String(item.id) === String(selectedLessonFlashcardSetId));
    if (!set) {
      pushToast('Hãy chọn một bộ flashcard trong kho.', 'warning');
      return;
    }
    const currentSets = Array.isArray(activeLesson.flashcardSets) ? activeLesson.flashcardSets : [];
    if (currentSets.some((item) => String(item.id) === String(set.id))) {
      pushToast('Bộ flashcard này đã được gắn vào bài học.', 'warning');
      return;
    }
    patchActiveLesson({ flashcardSets: [...currentSets, set] });
    setSelectedLessonFlashcardSetId('');
    pushToast('Đã gắn bộ flashcard từ kho vào bài học.');
  };

  const removeFlashcardSetFromActiveLesson = (setId) => {
    if (!activeLesson) return;
    patchActiveLesson({
      flashcardSets: (activeLesson.flashcardSets || []).filter((set) => String(set.id) !== String(setId)),
    });
    pushToast('Đã gỡ bộ flashcard khỏi bài học.', 'warning');
  };

  const updateAssessment = (assessmentKey, field, value) => {
    setAssessments((current) => current.map((assessment) => (
      assessment.localKey === assessmentKey
        ? { ...assessment, [field]: value }
        : assessment
    )));
  };

  const deleteAssessment = (assessmentKey) => {
    setAssessments((current) => current.filter((assessment) => assessment.localKey !== assessmentKey));
    pushToast('Đã gỡ bài kiểm tra khỏi nội dung khóa học.', 'warning');
  };

  const deleteModule = (moduleIndex) => {
    const module = modules[moduleIndex];
    if (!module) return;

    const assessmentCount = assessments.filter(
      (assessment) => assessment.moduleKey === resolveModuleKey(module),
    ).length;
    const confirmed = window.confirm(
      `Xóa mô-đun "${module.title}" cùng ${module.lessons?.length || 0} bài học`
      + `${assessmentCount ? ` và ${assessmentCount} bài kiểm tra` : ''}? `
      + 'Thay đổi sẽ được ghi nhận khi bạn bấm Lưu thay đổi.',
    );
    if (!confirmed) return;

    const removedModuleKey = resolveModuleKey(module);
    setCourse((current) => {
      if (!current) return current;
      const nextModules = current.modules
        .filter((_, index) => index !== moduleIndex)
        .map((item, index) => ({ ...item, displayOrder: index + 1 }));
      return { ...current, modules: nextModules };
    });
    setAssessments((current) =>
      current.filter((assessment) => assessment.moduleKey !== removedModuleKey),
    );
    setLessonModalOpen(false);
    setUploadFile(null);
    setActiveModuleIndex(Math.max(0, Math.min(moduleIndex, modules.length - 2)));
    setActiveLessonIndex(0);
    pushToast('Đã xóa mô-đun khỏi bản chỉnh sửa. Bấm Lưu thay đổi để ghi vào hệ thống.', 'warning');
  };

  const moveModule = (fromIndex, toIndex) => {
    setCourse((current) => {
      if (!current) return current;
      return { ...current, modules: reorder(current.modules, fromIndex, toIndex) };
    });
    setActiveModuleIndex(toIndex);
    setActiveLessonIndex(0);
  };

  const moveLesson = (fromIndex, toIndex) => {
    setCourse((current) => {
      if (!current) return current;
      return {
        ...current,
        modules: current.modules.map((module, moduleIndex) => (
          moduleIndex !== activeModuleIndex ? module : { ...module, lessons: reorder(module.lessons || [], fromIndex, toIndex) }
        )),
      };
    });
    setActiveLessonIndex(toIndex);
  };

  const deleteLesson = (lessonIndex) => {
    const lesson = lessons[lessonIndex];
    if (!lesson) return;

    const confirmed = !lesson.id || window.confirm(`Xóa bài học "${lesson.title}"? Thay đổi sẽ được ghi nhận khi bạn bấm Lưu thay đổi.`);
    if (!confirmed) return;

    setCourse((current) => {
      if (!current) return current;
      return {
        ...current,
        modules: current.modules.map((module, moduleIndex) => {
          if (moduleIndex !== activeModuleIndex) return module;
          const nextLessons = (module.lessons || [])
            .filter((_, index) => index !== lessonIndex)
            .map((item, index) => ({ ...item, displayOrder: index + 1 }));
          return { ...module, lessons: nextLessons };
        }),
      };
    });

    if (lessonIndex === activeLessonIndex) {
      setUploadFile(null);
      setLessonModalOpen(false);
      setActiveLessonIndex(Math.max(0, Math.min(lessonIndex, lessons.length - 2)));
    } else if (lessonIndex < activeLessonIndex) {
      setActiveLessonIndex((current) => Math.max(0, current - 1));
    }

    pushToast('Đã xóa bài học khỏi trình xây dựng. Bấm Lưu thay đổi trình xây dựng để lưu lại.', 'warning');
  };

  const handleBunnyUpload = async () => {
    if (!course?.id || !activeLesson?.id || !uploadFile) return;

    setUploadingVideo(true);
    setUploadProgress(0);
    setError('');

    try {
      const response = await courseApi.uploadLessonVideo(
        course.id,
        activeLesson.id,
        uploadFile,
        activeLesson.title || uploadFile.name,
        (event) => {
          if (!event.total) return;
          setUploadProgress(Math.round((event.loaded * 100) / event.total));
        },
      );
      patchActiveLesson({
        ...(response.lesson || {}),
        tempId: activeLesson.tempId,
        contentType: 'VIDEO',
      });
      setUploadFile(null);
      setUploadProgress(100);
      pushToast('Tải video lên thành công.');
    } catch (err) {
      const message = err?.response?.data?.message || 'Không upload được video lên Bunny.';
      setError(message);
      pushToast(message, 'error');
    } finally {
      setUploadingVideo(false);
    }
  };

  const handleRefreshTranscript = async () => {
    if (!course?.id || !activeLesson?.id || !activeLesson.videoUrl) {
      pushToast('Hãy lưu bài học và thêm liên kết YouTube trước khi lấy bản chép lời.', 'warning');
      return;
    }

    setRefreshingTranscript(true);
    setError('');
    try {
      const updatedCourse = await courseApi.refreshLessonTranscript(course.id, activeLesson.id);
      const normalizedCourse = normalizeCourseStructure(updatedCourse);
      const updatedLesson = normalizedCourse.modules
        ?.flatMap((module) => module.lessons || [])
        .find((lesson) => String(lesson.id) === String(activeLesson.id));
      const segmentCount = updatedLesson?.transcriptSegments?.length || 0;
      if (updatedLesson) {
        patchActiveLesson({ transcriptSegments: updatedLesson.transcriptSegments || [] });
      }
      pushToast(segmentCount
        ? `Đã lấy ${segmentCount} đoạn bản chép lời từ video.`
        : 'Video chưa có caption công khai. Bạn vẫn có thể nhập bản chép lời thủ công bên dưới.',
        segmentCount ? 'success' : 'warning');
    } catch (refreshError) {
      setError(refreshError?.response?.data?.message || 'Không thể lấy bản chép lời từ video lúc này.');
    } finally {
      setRefreshingTranscript(false);
    }
  };

  const handleSave = async () => {
    if (!course?.id) return;

    setError('');
    const validationMessage = validateBuilderState(modules, assessments);
    if (validationMessage) {
      setError(validationMessage);
      pushToast(validationMessage, 'error');
      return;
    }

    setSaving(true);

    try {
      const payload = {
        title: course.title,
        shortDescription: course.shortDescription,
        description: course.description,
        category: course.category,
        level: course.level,
        status: course.status,
        targetScore: course.targetScore,
        recommendedCurrentBandMin: course.recommendedCurrentBandMin ?? null,
        recommendedCurrentBandMax: course.recommendedCurrentBandMax ?? null,
        targetBand: course.targetBand ?? null,
        learningPathCode: course.learningPathCode ?? null,
        learningPathName: course.learningPathName ?? null,
        learningPathOrder: Number(course.learningPathOrder || 0),
        targetOutcome: course.targetOutcome ?? null,
        recommendedNextCourseSlug: course.recommendedNextCourseSlug ?? null,
        duration: course.duration,
        studyMode: course.studyMode,
        price: Number(course.price || 0),
        thumbnailUrl: course.thumbnailUrl,
        totalLessons,
        totalHours,
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
            transcriptSegments: normalizeTranscriptSegments(lesson.transcriptSegments),
            flashcardSetIds: (lesson.flashcardSets || []).map((set) => Number(set.id)).filter(Boolean),
            durationMinutes: normalizeDurationForSave(lesson),
            displayOrder: lessonIndex + 1,
            preview: Boolean(lesson.preview),
          })),
        })),
      };

      const updatedCourse = await courseApi.updateOnlineCourse(course.id, payload);
      const normalizedCourse = normalizeCourseStructure(updatedCourse);
      const assessmentPayload = buildAssessmentPayload(assessments, modules, normalizedCourse.modules);
      const updatedAssessments = await courseApi.saveManagedCourseAssessments(normalizedCourse.id, assessmentPayload);

      setCourse(normalizedCourse);
      setAssessments(normalizeAssessmentStructure(updatedAssessments, normalizedCourse.modules));
      pushToast('Đã lưu thay đổi nội dung khóa học.');
    } catch (err) {
      const message = err?.response?.data?.message || 'Không lưu được thay đổi nội dung khóa học.';
      setError(message);
      pushToast(message, 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4">
      <ToastStack toasts={toasts} onDismiss={dismissToast} />
      <div className="flex flex-wrap items-center gap-3">
        <Link className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/65 bg-white px-4 py-3 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]" to="/content-manager/courses">
          <ArrowLeft className="h-4 w-4" />
          Quay lại danh sách khóa học
        </Link>
        {course?.slug ? (
          <Link className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]" to={`/content-manager/courses/${course.slug}/edit`}>
            Chỉnh sửa thông tin khóa học
          </Link>
        ) : null}
        {course ? (
          <button className="ml-auto rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]" disabled={saving} onClick={handleSave} type="button">
            {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
          </button>
        ) : null}
      </div>

      {error ? <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">{error}</div> : null}

      {!course ? (
        <div className="rounded-2xl border border-[#dfbfbd]/55 bg-white px-5 py-8 text-sm text-[#584140]">Đang tải khu vực biên soạn...</div>
      ) : (
        <div className="grid gap-6 xl:grid-cols-[320px_minmax(0,1fr)]">
          <Panel className="overflow-hidden p-0">
            <div className="flex items-center justify-between border-b border-[#f0e3e4] px-5 py-4">
              <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">Mô-đun</h2>
              <button className="inline-flex h-10 w-10 items-center justify-center rounded-xl bg-[#4b0009] text-white transition hover:bg-[#730014]" onClick={addModule} title="Thêm mô-đun" type="button">
                <Plus className="h-4 w-4" />
              </button>
            </div>
            <div className="space-y-3 p-4">
              {modules.length ? modules.map((module, index) => (
                <div
                  key={module.id || module.tempId}
                  className={`block w-full cursor-grab rounded-2xl border p-4 text-left transition active:cursor-grabbing ${index === activeModuleIndex ? 'border-[#4b0009] bg-[#fff7f7]' : 'border-[#eadcdc] bg-white hover:border-[#730014]/30'}`}
                  draggable
                  onClick={() => {
                    setActiveModuleIndex(index);
                    setActiveLessonIndex(0);
                  }}
                  onDragStart={() => setDragState({ type: 'module', index })}
                  onDragOver={(event) => event.preventDefault()}
                  onDrop={(event) => {
                    event.preventDefault();
                    if (dragState?.type === 'module') moveModule(dragState.index, index);
                    setDragState(null);
                  }}
                  role="button"
                  tabIndex={0}
                >
                  <div className="flex gap-3">
                    <div className="mt-0.5 flex flex-col items-center gap-1">
                      <GripVertical className="h-4 w-4 text-[#730014]" />
                      <button
                        className="rounded-lg p-1 text-[#730014] transition hover:bg-[#fff2f3] disabled:cursor-not-allowed disabled:opacity-35"
                        disabled={index === 0}
                        onClick={(event) => {
                          event.stopPropagation();
                          moveModule(index, index - 1);
                        }}
                        title="Đưa mô-đun lên"
                        type="button"
                      >
                        <ArrowUp className="h-3.5 w-3.5" />
                      </button>
                      <button
                        className="rounded-lg p-1 text-[#730014] transition hover:bg-[#fff2f3] disabled:cursor-not-allowed disabled:opacity-35"
                        disabled={index === modules.length - 1}
                        onClick={(event) => {
                          event.stopPropagation();
                          moveModule(index, index + 1);
                        }}
                        title="Đưa mô-đun xuống"
                        type="button"
                      >
                        <ArrowDown className="h-3.5 w-3.5" />
                      </button>
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="font-semibold text-[#1a1c1c]">{module.title}</p>
                      <p className="mt-1 text-sm text-[#584140]">
                        {module.lessons?.length ?? 0} bài học • {assessments.filter((item) => item.moduleKey === resolveModuleKey(module)).length} bài kiểm tra
                      </p>
                    </div>
                    <button
                      aria-label={`Xóa mô-đun ${module.title}`}
                      className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-xl border border-[#f0c7c7] bg-white text-[#93000a] transition hover:bg-[#ffdad6]"
                      onClick={(event) => {
                        event.stopPropagation();
                        deleteModule(index);
                      }}
                      title="Xóa mô-đun"
                      type="button"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              )) : (
                <div className="rounded-2xl border border-dashed border-[#dfbfbd] p-4 text-sm text-[#584140]">Khóa học chưa có mô-đun.</div>
              )}
            </div>
          </Panel>

          <div className="space-y-4">
            <Panel className="p-6" id="course-assessments">
              <div className="grid gap-4 md:grid-cols-2">
                <TextField label="Tên mô-đun" onChange={updateModule('title')} value={activeModule?.title || ''} />
                <TextField label="Mô tả mô-đun" onChange={updateModule('description')} value={activeModule?.description || ''} />
              </div>
              <div className="mt-4 flex flex-wrap items-center gap-3">
                <button className="inline-flex items-center gap-2 rounded-2xl border border-[#4b0009] px-4 py-3 text-sm font-semibold text-[#4b0009] transition hover:bg-[#fff2f3]" disabled={!activeModule} onClick={addLesson} type="button">
                  <Plus className="h-4 w-4" />
                  Thêm bài học
                </button>
                <button className="inline-flex items-center gap-2 rounded-2xl border border-[#4b0009] px-4 py-3 text-sm font-semibold text-[#4b0009] transition hover:bg-[#fff2f3]" disabled={!activeModule} onClick={() => addAssessmentFromBank('module')} type="button">
                  <Plus className="h-4 w-4" />
                  Thêm từ kho đề
                </button>
                <span className="text-sm text-[#584140]">{totalLessons} bài học • {totalHours} giờ</span>
              </div>
            </Panel>

            <Panel className="p-5">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Bài học đang chọn</p>
                  {activeLesson ? (
                    <>
                      <h3 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#1a1c1c]">{activeLesson.title}</h3>
                      <p className="mt-2 max-w-4xl text-sm leading-6 text-[#584140]">{activeLesson.description || 'Chưa có mô tả cho bài học này.'}</p>
                      <div className="mt-4 grid gap-3 sm:grid-cols-3">
                        <div className="rounded-2xl border border-[#f0e3e4] bg-[#fffafb] p-3 text-sm">
                          <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#8b706e]">Loại nội dung</p>
                          <p className="mt-1 font-bold text-[#4b0009]">{getContentTypeLabel(activeLesson.contentType)}</p>
                        </div>
                        <div className="rounded-2xl border border-[#f0e3e4] bg-[#fffafb] p-3 text-sm">
                          <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#8b706e]">Thời lượng</p>
                          <p className="mt-1 font-bold text-[#4b0009]">{getLessonDurationLabel(activeLesson)}</p>
                        </div>
                        <div className="rounded-2xl border border-[#f0e3e4] bg-[#fffafb] p-3 text-sm">
                          <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#8b706e]">Thẻ ghi nhớ</p>
                          <p className="mt-1 font-bold text-[#4b0009]">{countLessonFlashcardSets(activeLesson)} bộ • {countLessonFlashcards(activeLesson)} thẻ</p>
                        </div>
                      </div>
                    </>
                  ) : (
                    <p className="mt-2 text-sm text-[#584140]">Chọn một bài học hoặc thêm bài học mới để bắt đầu biên tập nội dung.</p>
                  )}
                </div>
                {activeLesson ? (
                  <button
                    className="rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]"
                    onClick={() => setLessonModalOpen(true)}
                    type="button"
                  >
                    Chỉnh sửa bài học
                  </button>
                ) : null}
              </div>

              {activeLesson ? (
                <div className="mt-5 rounded-2xl border border-[#f0e3e4] bg-[#fffafb] p-4 text-sm text-[#584140]">
                  <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
                    <div>
                      <p className="font-semibold text-[#4b0009]">Thẻ ghi nhớ của bài học</p>
                      <p className="mt-1">
                        {activeLesson.videoUrl ? 'Đã liên kết video.' : 'Chưa liên kết video.'}
                        {activeLesson.contentText ? ' Nội dung bài học đã sẵn sàng.' : ' Chưa có nội dung bài học.'}
                      </p>
                    </div>
                    <div className="grid min-w-0 gap-2 sm:grid-cols-[minmax(260px,1fr)_auto] xl:min-w-[520px]">
                      <BrandedSelect
                        onChange={(event) => setSelectedLessonFlashcardSetId(event.target.value)}
                        options={flashcardSetOptions}
                        placeholder="Chọn bộ flashcard trong kho"
                        value={selectedLessonFlashcardSetId}
                      />
                      <button
                        className="rounded-xl bg-[#4b0009] px-4 py-3 font-semibold text-white transition hover:bg-[#730014]"
                        onClick={addFlashcardSetToActiveLesson}
                        type="button"
                      >
                        Thêm từ kho
                      </button>
                    </div>
                  </div>
                  {(activeLesson.flashcardSets || []).length ? (
                    <div className="mt-3 flex flex-wrap gap-2">
                      {activeLesson.flashcardSets.map((set) => (
                        <span className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] bg-white px-3 py-2 text-xs font-semibold text-[#4b0009]" key={set.id}>
                          {set.title}
                          <button className="text-[#93000a]" onClick={() => removeFlashcardSetFromActiveLesson(set.id)} type="button">Gỡ</button>
                        </span>
                      ))}
                    </div>
                  ) : (
                    <p className="mt-3 text-xs">Chưa gắn bộ flashcard nào từ kho.</p>
                  )}
                </div>
              ) : null}
            </Panel>

            <div className="space-y-3">
              {lessons.length ? lessons.map((lesson, index) => (
                <Panel
                  key={lesson.id || lesson.tempId}
                  className={`cursor-grab p-4 transition active:cursor-grabbing ${index === activeLessonIndex ? 'border-[#4b0009] bg-[#fff7f7]' : 'hover:border-[#730014]/30'}`}
                  draggable
                  onDragStart={() => setDragState({ type: 'lesson', index })}
                  onDragOver={(event) => event.preventDefault()}
                  onDrop={(event) => {
                    event.preventDefault();
                    if (dragState?.type === 'lesson') moveLesson(dragState.index, index);
                    setDragState(null);
                  }}
                >
                  <div className="flex w-full items-center gap-4">
                    <div className="flex items-center gap-1">
                      <GripVertical className="h-4 w-4 text-[#730014]" />
                      <button
                        className="rounded-lg p-1 text-[#730014] transition hover:bg-[#fff2f3] disabled:cursor-not-allowed disabled:opacity-35"
                        disabled={index === 0}
                        onClick={() => moveLesson(index, index - 1)}
                        title="Đưa bài học lên"
                        type="button"
                      >
                        <ArrowUp className="h-3.5 w-3.5" />
                      </button>
                      <button
                        className="rounded-lg p-1 text-[#730014] transition hover:bg-[#fff2f3] disabled:cursor-not-allowed disabled:opacity-35"
                        disabled={index === lessons.length - 1}
                        onClick={() => moveLesson(index, index + 1)}
                        title="Đưa bài học xuống"
                        type="button"
                      >
                        <ArrowDown className="h-3.5 w-3.5" />
                      </button>
                    </div>
                    <button
                      className="min-w-0 flex-1 text-left"
                      onClick={() => setActiveLessonIndex(index)}
                      type="button"
                    >
                      <div>
                        <p className="font-semibold text-[#1a1c1c]">{lesson.title}</p>
                        <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-[#584140]">
                          <span>{getContentTypeLabel(lesson.contentType)}</span>
                          <span>{getLessonDurationLabel(lesson)}</span>
                          <span>{lesson.preview ? 'Có xem trước' : 'Không xem trước'}</span>
                          {countLessonFlashcardSets(lesson) ? <span>{countLessonFlashcardSets(lesson)} bộ flashcard</span> : null}
                        </div>
                      </div>
                    </button>
                    <StatusBadge label={course.status} />
                    <button
                      className="rounded-xl border border-[#dfbfbd]/70 px-3 py-2 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]"
                      onClick={() => {
                        setActiveLessonIndex(index);
                        setLessonModalOpen(true);
                      }}
                      type="button"
                    >
                      Chỉnh sửa
                    </button>
                    <button
                      className="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-[#f0c7c7] bg-white text-[#93000a] transition hover:bg-[#ffdad6] disabled:cursor-not-allowed disabled:opacity-45"
                      disabled={saving || uploadingVideo}
                      onClick={() => deleteLesson(index)}
                      title="Xóa bài học"
                      type="button"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </Panel>
              )) : (
                <Panel className="p-6 text-sm text-[#584140]">Mô-đun này chưa có bài học nào. Hãy thêm bài học ở phần đầu mô-đun.</Panel>
              )}
            </div>

            <Panel className="p-6">
              <div className="mb-4 flex items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Bài kiểm tra mô-đun</p>
                  <h3 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">
                    {activeModule ? `Bài kiểm tra của ${activeModule.title}` : 'Bài kiểm tra'}
                  </h3>
                </div>
              </div>
              <AssessmentBankAttachBar
                disabled={!activeModule}
                onAdd={() => addAssessmentFromBank('module')}
                onChange={(event) => setSelectedModuleBankAssessmentId(event.target.value)}
                options={assessmentBankOptions}
                value={selectedModuleBankAssessmentId}
              />
              {moduleAssessments.length ? (
                <div className="space-y-4">
                  {moduleAssessments.map((assessment, index) => (
                    <AssessmentEditorCard
                      key={assessment.localKey}
                      assessment={assessment}
                      rubricOptions={buildRubricOptions(rubrics, assessment.skill)}
                      onDelete={() => deleteAssessment(assessment.localKey)}
                      onFieldChange={(field, value) => updateAssessment(assessment.localKey, field, value)}
                      title={`Bài kiểm tra mô-đun ${index + 1}`}
                    />
                  ))}
                </div>
              ) : (
                <div className="rounded-2xl border border-dashed border-[#dfbfbd] p-4 text-sm text-[#584140]">
                  Mô-đun này chưa có bài kiểm tra nào.
                </div>
              )}
            </Panel>

            <Panel className="p-6">
              <div className="mb-4 flex items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Bài kiểm tra cuối khóa</p>
                  <h3 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">Bài đánh giá cuối khóa</h3>
                </div>
              </div>
              <AssessmentBankAttachBar
                onAdd={() => addAssessmentFromBank('course')}
                onChange={(event) => setSelectedCourseBankAssessmentId(event.target.value)}
                options={assessmentBankOptions}
                value={selectedCourseBankAssessmentId}
              />
              {courseLevelAssessments.length ? (
                <div className="space-y-4">
                  {courseLevelAssessments.map((assessment, index) => (
                    <AssessmentEditorCard
                      key={assessment.localKey}
                      assessment={assessment}
                      rubricOptions={buildRubricOptions(rubrics, assessment.skill)}
                      onDelete={() => deleteAssessment(assessment.localKey)}
                      onFieldChange={(field, value) => updateAssessment(assessment.localKey, field, value)}
                      title={`Bài kiểm tra cuối khóa ${index + 1}`}
                    />
                  ))}
                </div>
              ) : (
                <div className="rounded-2xl border border-dashed border-[#dfbfbd] p-4 text-sm text-[#584140]">
                  Khóa học này chưa có bài kiểm tra cuối khóa.
                </div>
              )}
            </Panel>
          </div>
        </div>
      )}

      <LessonEditorModal
        activeLesson={activeLesson}
        onBunnyUpload={handleBunnyUpload}
        onChangeLesson={updateLesson}
        onPatchLesson={patchActiveLesson}
        onClose={() => setLessonModalOpen(false)}
        onRefreshTranscript={handleRefreshTranscript}
        open={lessonModalOpen}
        refreshingTranscript={refreshingTranscript}
        uploadFile={uploadFile}
        uploadingVideo={uploadingVideo}
        uploadProgress={uploadProgress}
        onSelectUploadFile={setUploadFile}
      />
    </div>
  );
}

function countLessonFlashcards(lesson) {
  const bankCardCount = (lesson?.flashcardSets || []).reduce((sum, set) => sum + countFlashcardSetCards(set), 0);
  if (bankCardCount > 0) return bankCardCount;
  const content = String(lesson?.contentText || '');
  const headings = [...content.matchAll(/^###\s+\d+\.\s+.+$/gm)];
  return headings.filter((heading, index) => {
    const start = (heading.index || 0) + heading[0].length;
    const end = headings[index + 1]?.index ?? content.length;
    return /^\*\*Meaning:\*\*/mi.test(content.slice(start, end));
  }).length;
}

function countLessonFlashcardSets(lesson) {
  return Array.isArray(lesson?.flashcardSets) ? lesson.flashcardSets.length : 0;
}

function countFlashcardSetCards(set) {
  try {
    const cards = JSON.parse(set?.cardsJson || '[]');
    return Array.isArray(cards) ? cards.length : 0;
  } catch {
    return 0;
  }
}

function normalizeCourseStructure(course) {
  return {
    ...course,
    modules: (course.modules || []).map((module, moduleIndex) => ({
      ...module,
      tempId: module.tempId || createTempId('module'),
      displayOrder: module.displayOrder ?? moduleIndex + 1,
      lessons: (module.lessons || []).map((lesson, lessonIndex) => ({
        ...lesson,
        tempId: lesson.tempId || createTempId('lesson'),
        contentType: formatContentType(lesson.contentType || (lesson.videoUrl ? 'VIDEO' : 'ARTICLE')),
        flashcardSets: Array.isArray(lesson.flashcardSets) ? lesson.flashcardSets : [],
        displayOrder: lesson.displayOrder ?? lessonIndex + 1,
      })),
    })),
  };
}

function validateBuilderState(modules, assessments) {
  for (let moduleIndex = 0; moduleIndex < modules.length; moduleIndex += 1) {
    const module = modules[moduleIndex];
    if (!String(module.title || '').trim()) {
      return `Mô-đun ${moduleIndex + 1} chưa có tên.`;
    }
    for (let lessonIndex = 0; lessonIndex < (module.lessons || []).length; lessonIndex += 1) {
      const lesson = module.lessons[lessonIndex];
      if (!String(lesson.title || '').trim()) {
        return `Bài học ${lessonIndex + 1} trong mô-đun "${module.title}" chưa có tên.`;
      }
      const duration = lesson.durationMinutes === '' || lesson.durationMinutes == null
        ? 0
        : Number(lesson.durationMinutes);
      if (!Number.isFinite(duration) || duration < 0) {
        return `Thời lượng của bài học "${lesson.title}" không hợp lệ.`;
      }
      const transcriptSegments = Array.isArray(lesson.transcriptSegments) ? lesson.transcriptSegments : [];
      for (let segmentIndex = 0; segmentIndex < transcriptSegments.length; segmentIndex += 1) {
        const segment = transcriptSegments[segmentIndex];
        const startSeconds = Number(segment?.startSeconds);
        const endSeconds = Number(segment?.endSeconds);
        if (!String(segment?.text || '').trim()) {
          return `Đoạn chép lời ${segmentIndex + 1} của bài học "${lesson.title}" chưa có nội dung.`;
        }
        if (!Number.isFinite(startSeconds) || !Number.isFinite(endSeconds) || startSeconds < 0 || endSeconds <= startSeconds) {
          return `Mốc thời gian của đoạn chép lời ${segmentIndex + 1} trong bài học "${lesson.title}" không hợp lệ.`;
        }
        if (segmentIndex > 0 && startSeconds < Number(transcriptSegments[segmentIndex - 1]?.endSeconds)) {
          return `Các đoạn chép lời trong bài học "${lesson.title}" đang bị chồng thời gian.`;
        }
      }
    }
  }

  for (let index = 0; index < assessments.length; index += 1) {
    const assessment = assessments[index];
    if (!String(assessment.title || '').trim()) {
      return `Bài kiểm tra ${index + 1} chưa có tên.`;
    }
    const maxScore = Number(assessment.maxScore);
    const passingScore = assessment.passingScore === '' || assessment.passingScore == null
      ? null
      : Number(assessment.passingScore);
    if (!Number.isFinite(maxScore) || maxScore <= 0) {
      return `Điểm tối đa của bài kiểm tra "${assessment.title}" phải lớn hơn 0.`;
    }
    if (usesBandScale(assessment) && maxScore > IELTS_MAX_BAND) {
      return `Điểm tối đa của bài kiểm tra "${assessment.title}" không được vượt quá band ${IELTS_MAX_BAND}.`;
    }
    if (passingScore != null && (!Number.isFinite(passingScore) || passingScore < 0 || passingScore > maxScore)) {
      return `Điểm đạt của bài kiểm tra "${assessment.title}" phải nằm trong khoảng từ 0 đến điểm tối đa.`;
    }
    if (assessment.aiEvaluationMode !== 'NONE' && !assessment.rubricId) {
      return `Bài kiểm tra "${assessment.title}" cần chọn tiêu chí chấm.`;
    }
    const isStructuredObjectiveExam = ['LISTENING', 'READING'].includes(String(assessment.skill || '').toUpperCase())
      && ['MODULE_TEST', 'MOCK_TEST'].includes(String(assessment.type || '').toUpperCase());
    if (isStructuredObjectiveExam) {
      try {
        const uiConfig = JSON.parse(String(assessment.uiConfigJson || ''));
        const answerKey = JSON.parse(String(assessment.objectiveAnswerKey || ''));
        if (!Array.isArray(uiConfig.parts) || !uiConfig.parts.length) {
          return `Bài kiểm tra "${assessment.title}" chưa có cấu trúc đề thi.`;
        }
        if (!answerKey || typeof answerKey !== 'object' || Array.isArray(answerKey)) {
          return `Bài kiểm tra "${assessment.title}" chưa có đáp án hợp lệ.`;
        }
      } catch {
        return `Bài kiểm tra "${assessment.title}" có cấu hình đề hoặc đáp án chưa hợp lệ.`;
      }
    }
  }
  return '';
}

function AssessmentBankAttachBar({ disabled = false, onAdd, onChange, options, value }) {
  return (
    <div className="mb-4 grid gap-3 rounded-2xl border border-[#dfbfbd] bg-[#fffafb] p-4 md:grid-cols-[minmax(0,1fr)_auto]">
      <BrandedSelect
        disabled={disabled}
        onChange={onChange}
        options={options}
        placeholder="Chọn đề trong ngân hàng đề"
        value={value}
      />
      <button
        className="rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-50"
        disabled={disabled}
        onClick={onAdd}
        type="button"
      >
        Thêm từ kho đề
      </button>
    </div>
  );
}

function AssessmentEditorCard({ assessment, rubricOptions, onDelete, onFieldChange, title }) {
  const isBankLinked = Boolean(assessment.assessmentBankItemId);
  const scoreLabel = usesBandScale(assessment)
    ? `Điểm tối đa (band IELTS, tối đa ${IELTS_MAX_BAND})`
    : 'Điểm tối đa';

  return (
    <div
      className="scroll-mt-32 rounded-3xl border border-[#eadcdc] bg-[#fffafb] p-5"
      id={assessment.id ? `assessment-editor-${assessment.id}` : undefined}
    >
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">{title}</p>
          <p className="mt-1 text-sm text-[#584140]">{assessment.moduleTitle || 'Điểm kiểm tra cuối khóa'}</p>
        </div>
        <button
          className="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-[#f0c7c7] bg-white text-[#93000a] transition hover:bg-[#ffdad6]"
          onClick={onDelete}
          type="button"
        >
          <Trash2 className="h-4 w-4" />
        </button>
      </div>

      {isBankLinked ? (
        <div className="mb-4 rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm text-[#584140]">
          <p className="font-semibold text-[#4b0009]">{assessment.title}</p>
          <p className="mt-1">{getAssessmentTypeLabel(assessment.type)} • {getSkillLabel(assessment.skill)} • {getAiModeLabel(assessment.aiEvaluationMode)}</p>
          {assessment.description ? <p className="mt-2 leading-6">{assessment.description}</p> : null}
          <p className="mt-2 text-xs font-semibold uppercase tracking-[0.14em] text-[#8b706e]">Nguồn: ngân hàng đề #{assessment.assessmentBankItemId}</p>
        </div>
      ) : null}

      <div className="grid gap-4 md:grid-cols-2">
        {!isBankLinked ? (
          <>
            <TextField label="Tên bài kiểm tra" onChange={(event) => onFieldChange('title', event.target.value)} value={assessment.title} />
            <TextField label="Mô tả" onChange={(event) => onFieldChange('description', event.target.value)} value={assessment.description} />
            <SelectField label="Loại bài kiểm tra" onChange={(event) => onFieldChange('type', event.target.value)} options={toSelectOptions(ASSESSMENT_TYPE_OPTIONS, getAssessmentTypeLabel)} value={assessment.type} />
            <SelectField label="Kỹ năng" onChange={(event) => onFieldChange('skill', event.target.value)} options={toSelectOptions(ASSESSMENT_SKILL_OPTIONS, getSkillLabel)} value={assessment.skill} />
            <SelectField label="Chế độ chấm" onChange={(event) => onFieldChange('aiEvaluationMode', event.target.value)} options={toSelectOptions(AI_MODE_OPTIONS, getAiModeLabel)} value={assessment.aiEvaluationMode} />
          </>
        ) : null}
        <SelectField label="Tiêu chí chấm" onChange={(event) => onFieldChange('rubricId', event.target.value)} options={rubricOptions} value={assessment.rubricId || ''} />
        {!isBankLinked ? (
          <>
            <TextField label="Điểm đạt" onChange={(event) => onFieldChange('passingScore', event.target.value)} value={assessment.passingScore} />
            <TextField label={scoreLabel} onChange={(event) => onFieldChange('maxScore', event.target.value)} value={assessment.maxScore} />
            <TextField label="Giới hạn thời gian (phút)" onChange={(event) => onFieldChange('timeLimitMinutes', event.target.value)} value={assessment.timeLimitMinutes} />
          </>
        ) : null}
        <TextField label="Thứ tự hiển thị" onChange={(event) => onFieldChange('displayOrder', event.target.value)} value={String(assessment.displayOrder || '')} />
      </div>

      {!isBankLinked ? (
        <>
          <div className="mt-4 grid gap-4">
            {!['LISTENING', 'READING'].includes(String(assessment.skill || '').toUpperCase()) ? (
              <TextField label="Đáp án tham chiếu" onChange={(event) => onFieldChange('objectiveAnswerKey', event.target.value)} rows={4} textarea value={assessment.objectiveAnswerKey} />
            ) : null}
          </div>

          <AssessmentExamBuilder assessment={assessment} onChange={onFieldChange} />
        </>
      ) : null}

      <label className="mt-4 flex items-center gap-3 rounded-2xl border border-[#f0e3e4] bg-white px-4 py-3 text-sm font-semibold text-[#1a1c1c]">
        <input checked={Boolean(assessment.active)} className="h-4 w-4 accent-[#4b0009]" onChange={(event) => onFieldChange('active', event.target.checked)} type="checkbox" />
        Đang kích hoạt
      </label>
    </div>
  );
}

function LessonEditorModal({
  activeLesson,
  onBunnyUpload,
  onChangeLesson,
  onPatchLesson,
  onClose,
  onRefreshTranscript,
  onSelectUploadFile,
  open,
  refreshingTranscript,
  uploadFile,
  uploadingVideo,
  uploadProgress,
}) {
  if (!open || !activeLesson) return null;

  const contentType = formatContentType(activeLesson.contentType);
  const isVideo = contentType === 'VIDEO';
  const contentLabel = getContentLabel(contentType);

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-[#1a1c1c]/45 px-4 py-6 backdrop-blur-sm">
      <div className="flex max-h-[92vh] w-full max-w-5xl flex-col overflow-hidden rounded-[28px] border border-[#dfbfbd]/75 bg-white shadow-[0_28px_80px_rgba(75,0,9,0.24)]">
        <div className="flex items-start justify-between gap-4 border-b border-[#f0e3e4] px-6 py-5">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Biên tập bài học</p>
            <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#1a1c1c]">{activeLesson.title || 'Bài học chưa đặt tên'}</h2>
          </div>
          <button
            className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl border border-[#dfbfbd]/70 bg-white text-[#730014] transition hover:bg-[#fff2f3]"
            onClick={onClose}
            type="button"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="overflow-y-auto px-6 py-6">
          <div className="grid gap-6 lg:grid-cols-[0.85fr_1.15fr]">
            <div className="space-y-4">
              <TextField label="Tên bài học" onChange={onChangeLesson('title')} value={activeLesson.title || ''} />
              <TextField label="Mô tả" onChange={onChangeLesson('description')} rows={4} textarea value={activeLesson.description || ''} />
              <SelectField label="Loại nội dung" onChange={onChangeLesson('contentType')} options={toSelectOptions(CONTENT_TYPE_OPTIONS)} value={contentType} />
              {isVideo ? (
                <div className="rounded-2xl border border-[#f0e3e4] bg-[#fffafb] px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Thời lượng</p>
                  <p className="mt-1 text-sm font-semibold text-[#4b0009]">Tự động lấy từ thông tin video</p>
                  <p className="mt-1 text-xs leading-5 text-[#584140]">Không cần nhập tay cho video. Hệ thống sẽ dùng thông tin của nguồn video khi có dữ liệu.</p>
                </div>
              ) : (
                <TextField label="Thời lượng (phút)" onChange={onChangeLesson('durationMinutes')} value={String(activeLesson.durationMinutes || '')} />
              )}
              <TextField label="Liên kết tài liệu" onChange={onChangeLesson('materialUrl')} value={activeLesson.materialUrl || ''} />
              <label className="flex items-center gap-3 rounded-2xl border border-[#f0e3e4] bg-[#fffafb] px-4 py-3 text-sm font-semibold text-[#1a1c1c]">
                <input checked={Boolean(activeLesson.preview)} className="h-4 w-4 accent-[#4b0009]" onChange={onChangeLesson('preview')} type="checkbox" />
                Cho phép xem trước
              </label>
            </div>

            <div className="space-y-4">
              {isVideo ? (
                <>
                  <TextField label="Liên kết video" onChange={onChangeLesson('videoUrl')} value={activeLesson.videoUrl || ''} />
                  <TranscriptEditor
                    onChange={(transcriptSegments) => onPatchLesson({ transcriptSegments })}
                    onRefresh={onRefreshTranscript}
                    refreshing={refreshingTranscript}
                    segments={activeLesson.transcriptSegments}
                    videoUrl={activeLesson.videoUrl}
                  />
                  <div className="rounded-2xl border border-[#dfbfbd]/65 bg-[#fffafb] p-4">
                    <div className="mb-3 flex items-center justify-between gap-3">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Tải video lên hệ thống</p>
                        {/* <p className="mt-1 text-sm text-[#584140]">
                          {activeLesson.bunnyVideoId ? `Mã video: ${activeLesson.bunnyVideoId}` : 'Tải video trực tiếp lên .'}
                        </p> */}
                      </div>
                      <Upload className="h-5 w-5 text-[#730014]" />
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <label className="inline-flex cursor-pointer items-center rounded-xl border border-[#dfbfbd]/70 bg-white px-3 py-2 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]">
                        {uploadFile ? uploadFile.name : 'Chọn video'}
                        <input accept="video/*" className="sr-only" onChange={(event) => onSelectUploadFile(event.target.files?.[0] || null)} type="file" />
                      </label>
                      <button
                        className="rounded-xl bg-[#4b0009] px-3 py-2 text-sm font-semibold text-white transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-45"
                        disabled={!activeLesson.id || !uploadFile || uploadingVideo}
                        onClick={onBunnyUpload}
                        type="button"
                      >
                        {uploadingVideo ? `Đang tải ${uploadProgress}%` : 'Tải lên'}
                      </button>
                    </div>
                    {!activeLesson.id ? (
                      <p className="mt-3 text-xs font-semibold text-[#93000a]">Hãy lưu khu vực biên soạn trước để bài học có ID rồi mới tải video lên.</p>
                    ) : null}
                    {uploadingVideo ? (
                      <div className="mt-3 h-2 overflow-hidden rounded-full bg-[#f1dfe1]">
                        <div className="h-full rounded-full bg-[#730014] transition-all" style={{ width: `${uploadProgress}%` }} />
                      </div>
                    ) : null}
                  </div>
                </>
              ) : null}

              <TextField
                label={contentLabel}
                onChange={onChangeLesson('contentText')}
                rows={isVideo ? 12 : 20}
                textarea
                value={activeLesson.contentText || ''}
              />
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-[#f0e3e4] bg-[#fffafb] px-6 py-4">
          <p className="text-sm text-[#584140]">Các thay đổi chỉ được ghi vào hệ thống sau khi bạn bấm Lưu thay đổi.</p>
          <button
            className="rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]"
            onClick={onClose}
            type="button"
          >
            Xong chỉnh sửa
          </button>
        </div>
      </div>
    </div>
  );
}

function TranscriptEditor({ segments, onChange, onRefresh, refreshing, videoUrl }) {
  const normalizedSegments = normalizeTranscriptSegments(segments, true);
  const updateSegment = (index, field, value) => {
    const next = normalizedSegments.map((segment, segmentIndex) => (
      segmentIndex === index
        ? { ...segment, [field]: field === 'text' ? value : Number(value) }
        : segment
    ));
    onChange(next);
  };

  return (
    <section className="rounded-2xl border border-[#dfbfbd]/65 bg-[#fffafb] p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Bản chép lời video</p>
          <p className="mt-1 text-sm leading-6 text-[#584140]">Thêm từng đoạn có mốc thời gian để học viên theo dõi và bấm chuyển đến đúng vị trí trong video.</p>
        </div>
        <button
          className="rounded-xl border border-[#dfbfbd]/70 bg-white px-3 py-2 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3] disabled:cursor-not-allowed disabled:opacity-45"
          disabled={!videoUrl || refreshing}
          onClick={onRefresh}
          type="button"
        >
          {refreshing ? 'Đang lấy caption...' : 'Lấy caption YouTube'}
        </button>
      </div>

      <div className="mt-4 space-y-3">
        {normalizedSegments.map((segment, index) => (
          <div className="grid gap-2 rounded-xl border border-[#f0e3e4] bg-white p-3 md:grid-cols-[88px_88px_minmax(0,1fr)_auto]" key={`${index}-${segment.startSeconds}-${segment.endSeconds}`}>
            <input
              aria-label={`Bắt đầu đoạn ${index + 1}`}
              className="rounded-lg border border-[#dfbfbd]/65 px-3 py-2 text-sm outline-none focus:border-[#730014]"
              min="0"
              onChange={(event) => updateSegment(index, 'startSeconds', event.target.value)}
              step="0.1"
              type="number"
              value={segment.startSeconds}
            />
            <input
              aria-label={`Kết thúc đoạn ${index + 1}`}
              className="rounded-lg border border-[#dfbfbd]/65 px-3 py-2 text-sm outline-none focus:border-[#730014]"
              min="0"
              onChange={(event) => updateSegment(index, 'endSeconds', event.target.value)}
              step="0.1"
              type="number"
              value={segment.endSeconds}
            />
            <textarea
              aria-label={`Nội dung đoạn ${index + 1}`}
              className="min-h-[44px] rounded-lg border border-[#dfbfbd]/65 px-3 py-2 text-sm leading-6 outline-none focus:border-[#730014]"
              onChange={(event) => updateSegment(index, 'text', event.target.value)}
              rows={2}
              value={segment.text}
            />
            <button
              aria-label={`Xóa đoạn ${index + 1}`}
              className="rounded-lg px-2 py-2 text-[#93000a] transition hover:bg-[#fff2f3]"
              onClick={() => onChange(normalizedSegments.filter((_, segmentIndex) => segmentIndex !== index))}
              type="button"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        ))}
      </div>

      <button
        className="mt-4 rounded-xl border border-dashed border-[#bf7783] px-3 py-2 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]"
        onClick={() => onChange([...normalizedSegments, {
          startSeconds: normalizedSegments.at(-1)?.endSeconds || 0,
          endSeconds: (normalizedSegments.at(-1)?.endSeconds || 0) + 10,
          text: '',
        }])}
        type="button"
      >
        + Thêm đoạn chép lời
      </button>
    </section>
  );
}

function SelectField({ label, value, onChange, options }) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">{label}</span>
      <BrandedSelect onChange={onChange} options={options || []} value={value} />
    </label>
  );
}

function ToastStack({ toasts, onDismiss }) {
  if (!toasts.length) return null;

  return (
    <div className="fixed right-6 top-6 z-[80] flex w-[min(380px,calc(100vw-32px))] flex-col gap-3">
      {toasts.map((toast) => {
        const tone = toastTone(toast.type);
        const Icon = tone.icon;
        return (
          <div
            key={toast.id}
            className={`flex items-start gap-3 rounded-2xl border bg-white/95 px-4 py-3 shadow-[0_18px_45px_rgba(75,0,9,0.16)] backdrop-blur ${tone.border}`}
          >
            <span className={`mt-0.5 inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-xl ${tone.iconBg} ${tone.iconText}`}>
              <Icon className="h-4 w-4" />
            </span>
            <p className="min-w-0 flex-1 pt-1 text-sm font-semibold leading-5 text-[#2b2828]">{toast.message}</p>
            <button
              className="rounded-lg p-1 text-[#8b706e] transition hover:bg-[#fff2f3] hover:text-[#730014]"
              onClick={() => onDismiss(toast.id)}
              type="button"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        );
      })}
    </div>
  );
}

function toastTone(type) {
  if (type === 'error') {
    return {
      border: 'border-[#ffb4ab]',
      icon: XCircle,
      iconBg: 'bg-[#ffdad6]',
      iconText: 'text-[#93000a]',
    };
  }
  if (type === 'warning') {
    return {
      border: 'border-[#f2cf8f]',
      icon: AlertTriangle,
      iconBg: 'bg-[#fff1cf]',
      iconText: 'text-[#7b5300]',
    };
  }
  return {
    border: 'border-[#b9dec5]',
    icon: CheckCircle2,
    iconBg: 'bg-[#e7f6ec]',
    iconText: 'text-[#176b3a]',
  };
}

function buildRubricOptions(rubrics, skill) {
  const base = [{ value: '', label: 'Chưa chọn tiêu chí chấm' }];
  const matched = (rubrics || [])
    .filter((rubric) => !skill || rubric.skill === skill || rubric.skill === 'MIXED')
    .map((rubric) => ({
      value: String(rubric.id),
      label: `${rubric.name} (${getSkillLabel(rubric.skill)})`,
    }));
  return [...base, ...matched];
}

function buildAssessmentBankOptions(items) {
  const base = [{ value: '', label: 'Chọn đề trong ngân hàng đề' }];
  const options = (items || []).map((item) => ({
    value: String(item.id),
    label: item.title || `Đề #${item.id}`,
    description: `${getAssessmentTypeLabel(item.type)} • ${getSkillLabel(item.skill)} • ${getAiModeLabel(item.aiEvaluationMode)}`,
  }));
  return [...base, ...options];
}

function buildFlashcardSetOptions(items) {
  const base = [{ value: '', label: 'Chọn bộ flashcard trong kho' }];
  const options = (items || []).map((item) => ({
    value: String(item.id),
    label: item.title || `Bộ flashcard #${item.id}`,
    description: `${countFlashcardSetCards(item)} thẻ${item.skill ? ` • ${getSkillLabel(item.skill)}` : ''}`,
  }));
  return [...base, ...options];
}

function toSelectOptions(values, getLabel = (value) => value) {
  return values.map((value) => ({ value, label: getLabel(value) }));
}

function formatContentType(value) {
  return String(value || 'VIDEO').toUpperCase();
}

function getContentTypeLabel(value) {
  const map = {
    VIDEO: 'Video',
    ARTICLE: 'Bài đọc',
    ASSIGNMENT: 'Bài tập',
    QUIZ: 'Trắc nghiệm',
  };

  return map[formatContentType(value)] || formatContentType(value);
}

function normalizeDurationForSave(lesson) {
  const value = lesson?.durationMinutes;
  if (value === '' || value == null) return 0;
  return Number(value || 0);
}

function getLessonDurationLabel(lesson) {
  const duration = Number(lesson?.durationMinutes || 0);
  if (duration > 0) return `${duration} phút`;
  return formatContentType(lesson?.contentType) === 'VIDEO' ? 'Tự động' : '0 phút';
}

function getContentLabel(contentType) {
  if (contentType === 'VIDEO') return 'Ghi chú bài học';
  if (contentType === 'ASSIGNMENT') return 'Hướng dẫn bài tập';
  if (contentType === 'QUIZ') return 'Nội dung trắc nghiệm';
  return 'Nội dung bài học';
}

function getAssessmentTypeLabel(value) {
  const map = {
    MODULE_TEST: 'Kiểm tra mô-đun',
    LESSON_PRACTICE: 'Luyện tập theo bài',
    MOCK_TEST: 'Đề thi thử',
    WRITING_TASK: 'Bài viết',
    SPEAKING_TASK: 'Bài nói',
    QUIZ: 'Trắc nghiệm',
  };

  return map[value] || value;
}

function getSkillLabel(value) {
  const map = {
    LISTENING: 'Nghe',
    READING: 'Đọc',
    WRITING: 'Viết',
    SPEAKING: 'Nói',
    VOCABULARY: 'Từ vựng',
    GRAMMAR: 'Ngữ pháp',
    MIXED: 'Tổng hợp',
  };

  return map[value] || value;
}

function getAiModeLabel(value) {
  const map = {
    NONE: 'Không dùng AI',
    EXPLAIN_ONLY: 'Chỉ giải thích',
    RUBRIC_FEEDBACK: 'Phản hồi theo tiêu chí',
    ESTIMATED_BAND: 'Ước lượng band',
  };

  return map[value] || value;
}
