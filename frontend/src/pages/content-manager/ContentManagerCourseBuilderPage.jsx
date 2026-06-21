import { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, ArrowDown, ArrowLeft, ArrowUp, CheckCircle2, ChevronDown, GripVertical, Plus, Trash2, Upload, X, XCircle } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { Panel, StatusBadge, TextField } from '../../components/content-manager/ContentManagerUi';
import { IELTS_MAX_BAND, normalizeAssessmentMaxScore, normalizeAssessmentPassingScore } from '../../utils/ieltsBandScale';

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
  rubricId: '',
  title: '',
  description: '',
  type: 'MODULE_TEST',
  skill: 'MIXED',
  aiEvaluationMode: 'EXPLAIN_ONLY',
  instructions: '',
  objectiveAnswerKey: '',
  passingScore: '',
  maxScore: '9',
  timeLimitMinutes: '',
  displayOrder,
  active: true,
});

const normalizeScalar = (value, fallback = '') => (value == null ? fallback : String(value));

const normalizeAssessmentStructure = (items, modules) => {
  const moduleById = new Map((modules || []).map((module) => [String(module.id), module]));
  return (items || []).map((assessment, index) => {
    const matchedModule = assessment.moduleId == null ? null : moduleById.get(String(assessment.moduleId));
    return {
      id: assessment.id ?? null,
      localKey: assessment.id ? `assessment-${assessment.id}` : createTempId('assessment'),
      moduleKey: matchedModule ? resolveModuleKey(matchedModule) : COURSE_LEVEL_KEY,
      moduleTitle: matchedModule?.title || assessment.moduleTitle || null,
      rubricId: assessment.rubric?.id ? String(assessment.rubric.id) : '',
      title: assessment.title || '',
      description: assessment.description || '',
      type: assessment.type || 'MODULE_TEST',
      skill: assessment.skill || 'MIXED',
      aiEvaluationMode: assessment.aiEvaluationMode || 'EXPLAIN_ONLY',
      instructions: assessment.instructions || '',
      objectiveAnswerKey: assessment.objectiveAnswerKey || '',
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
    rubricId: assessment.rubricId ? Number(assessment.rubricId) : null,
    title: assessment.title?.trim() || `Bài đánh giá ${index + 1}`,
    description: assessment.description?.trim() || '',
    type: assessment.type || 'MODULE_TEST',
    skill: assessment.skill || 'MIXED',
    aiEvaluationMode: assessment.aiEvaluationMode || 'EXPLAIN_ONLY',
    instructions: assessment.instructions?.trim() || '',
    objectiveAnswerKey: assessment.objectiveAnswerKey?.trim() || '',
    passingScore: normalizeAssessmentPassingScore(assessment),
    maxScore: normalizeAssessmentMaxScore(assessment),
    timeLimitMinutes: assessment.timeLimitMinutes === '' ? null : Number(assessment.timeLimitMinutes),
    displayOrder: Number(assessment.displayOrder || index + 1),
    active: assessment.active !== false,
  }));
};

export default function ContentManagerCourseBuilderPage() {
  const { slugOrId } = useParams();
  const [course, setCourse] = useState(null);
  const [assessments, setAssessments] = useState([]);
  const [rubrics, setRubrics] = useState([]);
  const [activeModuleIndex, setActiveModuleIndex] = useState(0);
  const [activeLessonIndex, setActiveLessonIndex] = useState(0);
  const [dragState, setDragState] = useState(null);
  const [lessonModalOpen, setLessonModalOpen] = useState(false);
  const [uploadFile, setUploadFile] = useState(null);
  const [uploadingVideo, setUploadingVideo] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [toasts, setToasts] = useState([]);

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
        const [courseData, rubricItems] = await Promise.all([
          courseApi.getManagedOnlineCourse(slugOrId),
          courseApi.getManagedAssessmentRubrics(),
        ]);
        if (!active) return;

        const normalizedCourse = normalizeCourseStructure(courseData);
        setCourse(normalizedCourse);
        setRubrics(Array.isArray(rubricItems) ? rubricItems : []);

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

  const modules = course?.modules || [];
  const activeModule = modules[activeModuleIndex] || null;
  const lessons = activeModule?.lessons || [];
  const activeLesson = lessons[activeLessonIndex] || null;
  const activeModuleKey = resolveModuleKey(activeModule);
  const moduleAssessments = assessments.filter((assessment) => assessment.moduleKey === activeModuleKey);
  const courseLevelAssessments = assessments.filter((assessment) => assessment.moduleKey === COURSE_LEVEL_KEY);

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
            title: `Module ${current.modules.length + 1}`,
            description: '',
            displayOrder: current.modules.length + 1,
            lessons: [],
          },
        ],
      };
    });
    setActiveModuleIndex(modules.length);
    setActiveLessonIndex(0);
    pushToast('Đã thêm mô-đun mới. Bấm Save Builder Changes để lưu xuống hệ thống.');
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
                title: `Lesson ${activeModuleIndex + 1}.${currentLessons.length + 1}`,
                description: '',
                contentType: 'VIDEO',
                contentText: '',
                videoUrl: '',
                materialUrl: '',
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
    pushToast('Đã thêm bài học mới. Điền nội dung rồi bấm Save Builder Changes.');
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

  const updateAssessment = (assessmentKey, field, value) => {
    setAssessments((current) => current.map((assessment) => (
      assessment.localKey === assessmentKey
        ? { ...assessment, [field]: value }
        : assessment
    )));
  };

  const deleteAssessment = (assessmentKey) => {
    setAssessments((current) => current.filter((assessment) => assessment.localKey !== assessmentKey));
    pushToast('Đã gỡ bài đánh giá khỏi builder.', 'warning');
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

    const confirmed = !lesson.id || window.confirm(`Xóa bài học "${lesson.title}"? Save Builder Changes sẽ lưu thay đổi này.`);
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

    pushToast('Đã xóa bài học khỏi builder. Bấm Save Builder Changes để lưu thay đổi.', 'warning');
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
      pushToast('Upload video lên Bunny thành công.');
    } catch (err) {
      const message = err?.response?.data?.message || 'Không upload được video lên Bunny.';
      setError(message);
      pushToast(message, 'error');
    } finally {
      setUploadingVideo(false);
    }
  };

  const handleSave = async () => {
    if (!course?.id) return;

    setSaving(true);
    setError('');

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
      pushToast('Đã lưu thay đổi builder thành công.');
    } catch (err) {
      const message = err?.response?.data?.message || 'Không lưu được thay đổi builder.';
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
          Back to courses
        </Link>
        {course?.slug ? (
          <Link className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]" to={`/content-manager/courses/${course.slug}/edit`}>
            Edit metadata
          </Link>
        ) : null}
        {course ? (
          <button className="ml-auto rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]" disabled={saving} onClick={handleSave} type="button">
            {saving ? 'Saving...' : 'Save Builder Changes'}
          </button>
        ) : null}
      </div>

      {error ? <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">{error}</div> : null}

      {!course ? (
        <div className="rounded-2xl border border-[#dfbfbd]/55 bg-white px-5 py-8 text-sm text-[#584140]">Đang tải builder...</div>
      ) : (
        <div className="grid gap-6 xl:grid-cols-[320px_1fr_320px]">
          <Panel className="overflow-hidden p-0">
            <div className="flex items-center justify-between border-b border-[#f0e3e4] px-5 py-4">
              <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">Modules</h2>
              <button className="inline-flex h-10 w-10 items-center justify-center rounded-xl bg-[#4b0009] text-white transition hover:bg-[#730014]" onClick={addModule} title="Add module" type="button">
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
                        title="Move module up"
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
                        title="Move module down"
                        type="button"
                      >
                        <ArrowDown className="h-3.5 w-3.5" />
                      </button>
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="font-semibold text-[#1a1c1c]">{module.title}</p>
                      <p className="mt-1 text-sm text-[#584140]">
                        {module.lessons?.length ?? 0} lessons • {assessments.filter((item) => item.moduleKey === resolveModuleKey(module)).length} assessments
                      </p>
                    </div>
                  </div>
                </div>
              )) : (
                <div className="rounded-2xl border border-dashed border-[#dfbfbd] p-4 text-sm text-[#584140]">No modules yet.</div>
              )}
            </div>
          </Panel>

          <div className="space-y-4">
            <Panel className="p-6">
              <div className="grid gap-4 md:grid-cols-2">
                <TextField label="Module title" onChange={updateModule('title')} value={activeModule?.title || ''} />
                <TextField label="Module description" onChange={updateModule('description')} value={activeModule?.description || ''} />
              </div>
              <div className="mt-4 flex flex-wrap items-center gap-3">
                <button className="inline-flex items-center gap-2 rounded-2xl border border-[#4b0009] px-4 py-3 text-sm font-semibold text-[#4b0009] transition hover:bg-[#fff2f3]" disabled={!activeModule} onClick={addLesson} type="button">
                  <Plus className="h-4 w-4" />
                  Add lesson
                </button>
                <button className="inline-flex items-center gap-2 rounded-2xl border border-[#4b0009] px-4 py-3 text-sm font-semibold text-[#4b0009] transition hover:bg-[#fff2f3]" disabled={!activeModule} onClick={() => addAssessment('module')} type="button">
                  <Plus className="h-4 w-4" />
                  Add module assessment
                </button>
                <span className="text-sm text-[#584140]">{totalLessons} lessons • {totalHours} hours</span>
              </div>
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
                        title="Move lesson up"
                        type="button"
                      >
                        <ArrowUp className="h-3.5 w-3.5" />
                      </button>
                      <button
                        className="rounded-lg p-1 text-[#730014] transition hover:bg-[#fff2f3] disabled:cursor-not-allowed disabled:opacity-35"
                        disabled={index === lessons.length - 1}
                        onClick={() => moveLesson(index, index + 1)}
                        title="Move lesson down"
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
                          <span>{formatContentType(lesson.contentType)}</span>
                          <span>{getLessonDurationLabel(lesson)}</span>
                          <span>Preview: {lesson.preview ? 'Yes' : 'No'}</span>
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
                      Edit
                    </button>
                    <button
                      className="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-[#f0c7c7] bg-white text-[#93000a] transition hover:bg-[#ffdad6] disabled:cursor-not-allowed disabled:opacity-45"
                      disabled={saving || uploadingVideo}
                      onClick={() => deleteLesson(index)}
                      title="Delete lesson"
                      type="button"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </Panel>
              )) : (
                <Panel className="p-6 text-sm text-[#584140]">This module has no lessons yet. Add one from the module header.</Panel>
              )}
            </div>

            <Panel className="p-6">
              <div className="mb-4 flex items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Module checks</p>
                  <h3 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">
                    {activeModule ? `Assessments for ${activeModule.title}` : 'Assessments'}
                  </h3>
                </div>
                <button className="rounded-2xl border border-[#4b0009] px-4 py-2 text-sm font-semibold text-[#4b0009] transition hover:bg-[#fff2f3]" disabled={!activeModule} onClick={() => addAssessment('module')} type="button">
                  Add assessment
                </button>
              </div>
              {moduleAssessments.length ? (
                <div className="space-y-4">
                  {moduleAssessments.map((assessment, index) => (
                    <AssessmentEditorCard
                      key={assessment.localKey}
                      assessment={assessment}
                      rubricOptions={buildRubricOptions(rubrics, assessment.skill)}
                      onDelete={() => deleteAssessment(assessment.localKey)}
                      onFieldChange={(field, value) => updateAssessment(assessment.localKey, field, value)}
                      title={`Module assessment ${index + 1}`}
                    />
                  ))}
                </div>
              ) : (
                <div className="rounded-2xl border border-dashed border-[#dfbfbd] p-4 text-sm text-[#584140]">
                  This module does not have an assessment yet.
                </div>
              )}
            </Panel>

            <Panel className="p-6">
              <div className="mb-4 flex items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Final checks</p>
                  <h3 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">Course-level assessments</h3>
                </div>
                <button className="rounded-2xl border border-[#4b0009] px-4 py-2 text-sm font-semibold text-[#4b0009] transition hover:bg-[#fff2f3]" onClick={() => addAssessment('course')} type="button">
                  Add final assessment
                </button>
              </div>
              {courseLevelAssessments.length ? (
                <div className="space-y-4">
                  {courseLevelAssessments.map((assessment, index) => (
                    <AssessmentEditorCard
                      key={assessment.localKey}
                      assessment={assessment}
                      rubricOptions={buildRubricOptions(rubrics, assessment.skill)}
                      onDelete={() => deleteAssessment(assessment.localKey)}
                      onFieldChange={(field, value) => updateAssessment(assessment.localKey, field, value)}
                      title={`Course assessment ${index + 1}`}
                    />
                  ))}
                </div>
              ) : (
                <div className="rounded-2xl border border-dashed border-[#dfbfbd] p-4 text-sm text-[#584140]">
                  No final assessment has been added for this course yet.
                </div>
              )}
            </Panel>
          </div>

          <Panel className="p-6">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Selected lesson</p>
            {activeLesson ? (
              <div className="mt-4 space-y-4">
                <div>
                  <h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">{activeLesson.title}</h3>
                  <p className="mt-2 text-sm leading-6 text-[#584140]">{activeLesson.description || 'No description yet.'}</p>
                </div>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div className="rounded-2xl border border-[#f0e3e4] bg-[#fffafb] p-3">
                    <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#8b706e]">Type</p>
                    <p className="mt-1 font-bold text-[#4b0009]">{formatContentType(activeLesson.contentType)}</p>
                  </div>
                  <div className="rounded-2xl border border-[#f0e3e4] bg-[#fffafb] p-3">
                    <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#8b706e]">Duration</p>
                    <p className="mt-1 font-bold text-[#4b0009]">{getLessonDurationLabel(activeLesson)}</p>
                  </div>
                </div>
                <div className="rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] p-4 text-sm leading-6 text-[#584140]">
                  {activeLesson.videoUrl ? 'Video linked.' : 'No video linked.'}
                  {activeLesson.contentText ? ' Lesson content is available.' : ' No lesson content yet.'}
                </div>
                <button
                  className="w-full rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]"
                  onClick={() => setLessonModalOpen(true)}
                  type="button"
                >
                  Open lesson editor
                </button>
              </div>
            ) : (
              <div className="mt-4 rounded-2xl border border-dashed border-[#dfbfbd] p-4 text-sm text-[#584140]">
                Select a lesson or add a new one to edit content.
              </div>
            )}
          </Panel>
        </div>
      )}

      <LessonEditorModal
        activeLesson={activeLesson}
        onBunnyUpload={handleBunnyUpload}
        onChangeLesson={updateLesson}
        onClose={() => setLessonModalOpen(false)}
        open={lessonModalOpen}
        uploadFile={uploadFile}
        uploadingVideo={uploadingVideo}
        uploadProgress={uploadProgress}
        onSelectUploadFile={setUploadFile}
      />
    </div>
  );
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
        displayOrder: lesson.displayOrder ?? lessonIndex + 1,
      })),
    })),
  };
}

function AssessmentEditorCard({ assessment, rubricOptions, onDelete, onFieldChange, title }) {
  return (
    <div className="rounded-3xl border border-[#eadcdc] bg-[#fffafb] p-5">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">{title}</p>
          <p className="mt-1 text-sm text-[#584140]">{assessment.moduleTitle || 'Course-level checkpoint'}</p>
        </div>
        <button
          className="inline-flex h-9 w-9 items-center justify-center rounded-xl border border-[#f0c7c7] bg-white text-[#93000a] transition hover:bg-[#ffdad6]"
          onClick={onDelete}
          type="button"
        >
          <Trash2 className="h-4 w-4" />
        </button>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <TextField label="Assessment title" onChange={(event) => onFieldChange('title', event.target.value)} value={assessment.title} />
        <TextField label="Description" onChange={(event) => onFieldChange('description', event.target.value)} value={assessment.description} />
        <SelectField label="Assessment type" onChange={(event) => onFieldChange('type', event.target.value)} options={toSelectOptions(ASSESSMENT_TYPE_OPTIONS)} value={assessment.type} />
        <SelectField label="Skill" onChange={(event) => onFieldChange('skill', event.target.value)} options={toSelectOptions(ASSESSMENT_SKILL_OPTIONS)} value={assessment.skill} />
        <SelectField label="AI mode" onChange={(event) => onFieldChange('aiEvaluationMode', event.target.value)} options={toSelectOptions(AI_MODE_OPTIONS)} value={assessment.aiEvaluationMode} />
        <SelectField label="Rubric" onChange={(event) => onFieldChange('rubricId', event.target.value)} options={rubricOptions} value={assessment.rubricId || ''} />
        <TextField label="Passing score" onChange={(event) => onFieldChange('passingScore', event.target.value)} value={assessment.passingScore} />
        <TextField label={`Max score (IELTS band tối đa ${IELTS_MAX_BAND})`} onChange={(event) => onFieldChange('maxScore', event.target.value)} value={assessment.maxScore} />
        <TextField label="Time limit (minutes)" onChange={(event) => onFieldChange('timeLimitMinutes', event.target.value)} value={assessment.timeLimitMinutes} />
        <TextField label="Display order" onChange={(event) => onFieldChange('displayOrder', event.target.value)} value={String(assessment.displayOrder || '')} />
      </div>

      <div className="mt-4 grid gap-4">
        <TextField label="Instructions" onChange={(event) => onFieldChange('instructions', event.target.value)} rows={5} textarea value={assessment.instructions} />
        <TextField label="Objective answer key" onChange={(event) => onFieldChange('objectiveAnswerKey', event.target.value)} rows={4} textarea value={assessment.objectiveAnswerKey} />
      </div>

      <label className="mt-4 flex items-center gap-3 rounded-2xl border border-[#f0e3e4] bg-white px-4 py-3 text-sm font-semibold text-[#1a1c1c]">
        <input checked={Boolean(assessment.active)} className="h-4 w-4 accent-[#730014]" onChange={(event) => onFieldChange('active', event.target.checked)} type="checkbox" />
        Active assessment
      </label>
    </div>
  );
}

function LessonEditorModal({
  activeLesson,
  onBunnyUpload,
  onChangeLesson,
  onClose,
  onSelectUploadFile,
  open,
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
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Lesson editor</p>
            <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#1a1c1c]">{activeLesson.title || 'Untitled lesson'}</h2>
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
              <TextField label="Lesson title" onChange={onChangeLesson('title')} value={activeLesson.title || ''} />
              <TextField label="Description" onChange={onChangeLesson('description')} rows={4} textarea value={activeLesson.description || ''} />
              <SelectField label="Content type" onChange={onChangeLesson('contentType')} options={toSelectOptions(CONTENT_TYPE_OPTIONS)} value={contentType} />
              {isVideo ? (
                <div className="rounded-2xl border border-[#f0e3e4] bg-[#fffafb] px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Duration</p>
                  <p className="mt-1 text-sm font-semibold text-[#4b0009]">Auto from video metadata</p>
                  <p className="mt-1 text-xs leading-5 text-[#584140]">Không cần nhập tay cho video. Hệ thống sẽ dùng metadata của nguồn video khi có dữ liệu.</p>
                </div>
              ) : (
                <TextField label="Duration minutes" onChange={onChangeLesson('durationMinutes')} value={String(activeLesson.durationMinutes || '')} />
              )}
              <TextField label="Material URL" onChange={onChangeLesson('materialUrl')} value={activeLesson.materialUrl || ''} />
              <label className="flex items-center gap-3 rounded-2xl border border-[#f0e3e4] bg-[#fffafb] px-4 py-3 text-sm font-semibold text-[#1a1c1c]">
                <input checked={Boolean(activeLesson.preview)} className="h-4 w-4 accent-[#730014]" onChange={onChangeLesson('preview')} type="checkbox" />
                Preview lesson
              </label>
            </div>

            <div className="space-y-4">
              {isVideo ? (
                <>
                  <TextField label="Video URL" onChange={onChangeLesson('videoUrl')} value={activeLesson.videoUrl || ''} />
                  <div className="rounded-2xl border border-[#dfbfbd]/65 bg-[#fffafb] p-4">
                    <div className="mb-3 flex items-center justify-between gap-3">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Bunny upload</p>
                        <p className="mt-1 text-sm text-[#584140]">
                          {activeLesson.bunnyVideoId ? `Video ID: ${activeLesson.bunnyVideoId}` : 'Upload video trực tiếp lên Bunny Stream.'}
                        </p>
                      </div>
                      <Upload className="h-5 w-5 text-[#730014]" />
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <label className="inline-flex cursor-pointer items-center rounded-xl border border-[#dfbfbd]/70 bg-white px-3 py-2 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]">
                        {uploadFile ? uploadFile.name : 'Choose video'}
                        <input accept="video/*" className="sr-only" onChange={(event) => onSelectUploadFile(event.target.files?.[0] || null)} type="file" />
                      </label>
                      <button
                        className="rounded-xl bg-[#4b0009] px-3 py-2 text-sm font-semibold text-white transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-45"
                        disabled={!activeLesson.id || !uploadFile || uploadingVideo}
                        onClick={onBunnyUpload}
                        type="button"
                      >
                        {uploadingVideo ? `Uploading ${uploadProgress}%` : 'Upload'}
                      </button>
                    </div>
                    {!activeLesson.id ? (
                      <p className="mt-3 text-xs font-semibold text-[#93000a]">Save builder trước để lesson có ID rồi mới upload được video.</p>
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
          <p className="text-sm text-[#584140]">Changes stay local until you click Save Builder Changes.</p>
          <button
            className="rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]"
            onClick={onClose}
            type="button"
          >
            Done editing
          </button>
        </div>
      </div>
    </div>
  );
}

function SelectField({ label, value, onChange, options }) {
  const [open, setOpen] = useState(false);
  const normalizedOptions = options || [];
  const selectedOption = normalizedOptions.find((option) => String(option.value) === String(value)) || normalizedOptions[0];

  return (
    <label className="relative block">
      <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">{label}</span>
      <button
        className="flex w-full items-center justify-between rounded-2xl border border-[#dfbfbd]/65 bg-[#fcfbfb] px-4 py-3 text-left text-sm font-semibold text-[#1a1c1c] outline-none transition hover:border-[#730014]/40 hover:bg-[#fff7f7]"
        onBlur={() => window.setTimeout(() => setOpen(false), 120)}
        onClick={() => setOpen((current) => !current)}
        type="button"
      >
        {selectedOption?.label || value || 'Select'}
        <ChevronDown className={`h-4 w-4 text-[#730014] transition ${open ? 'rotate-180' : ''}`} />
      </button>
      {open ? (
        <div className="absolute left-0 top-full z-50 mt-2 w-full overflow-hidden rounded-2xl border border-[#dfbfbd]/75 bg-white p-1 shadow-[0_18px_45px_rgba(75,0,9,0.16)]">
          {normalizedOptions.map((option) => (
            <button
              key={option.value}
              className={`block w-full rounded-xl px-4 py-2.5 text-left text-sm font-semibold transition ${
                String(option.value) === String(value) ? 'bg-[#4b0009] text-white' : 'text-[#4b0009] hover:bg-[#fff2f3]'
              }`}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => {
                onChange({ target: { value: option.value } });
                setOpen(false);
              }}
              type="button"
            >
              {option.label}
            </button>
          ))}
        </div>
      ) : null}
      <select className="sr-only" onChange={onChange} value={value}>
        {normalizedOptions.map((option) => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
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
  const base = [{ value: '', label: 'No rubric' }];
  const matched = (rubrics || [])
    .filter((rubric) => !skill || rubric.skill === skill || rubric.skill === 'MIXED')
    .map((rubric) => ({
      value: String(rubric.id),
      label: `${rubric.name} (${rubric.skill})`,
    }));
  return [...base, ...matched];
}

function toSelectOptions(values) {
  return values.map((value) => ({ value, label: value }));
}

function formatContentType(value) {
  return String(value || 'VIDEO').toUpperCase();
}

function normalizeDurationForSave(lesson) {
  const value = lesson?.durationMinutes;
  if (value === '' || value == null) return 0;
  return Number(value || 0);
}

function getLessonDurationLabel(lesson) {
  const duration = Number(lesson?.durationMinutes || 0);
  if (duration > 0) return `${duration} min`;
  return formatContentType(lesson?.contentType) === 'VIDEO' ? 'Auto' : '0 min';
}

function getContentLabel(contentType) {
  if (contentType === 'VIDEO') return 'Study notes';
  if (contentType === 'ASSIGNMENT') return 'Assignment instructions';
  if (contentType === 'QUIZ') return 'Quiz content';
  return 'Lesson content';
}
