import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import classroomApi from '../api/classroomApi';
import courseApi from '../api/courseApi';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import WorkspaceFlashcards, { extractVocabularyTerms } from '../components/course-workspace/WorkspaceFlashcards';
import BrandedSelect from '../components/ui/BrandedSelect';

const classroomTypeLabel = (deliveryMode) => {
  if (deliveryMode === 'OFFLINE') return 'Lớp tại trung tâm';
  if (deliveryMode === 'VIRTUAL') return 'Lớp trực tuyến';
  return 'Lớp học';
};

const toClassroomFlashcardCourse = (classroom) => ({
  title: classroom?.title || classroom?.learningPackageTitle || `Lớp học #${classroom?.id}`,
  modules: (classroom?.curriculumProgram?.units || [])
    .filter((unit) => (unit.flashcards || []).length > 0)
    .map((unit) => ({
      id: `classroom-unit-${unit.id}`,
      title: unit.title,
      lessons: [{
        id: `classroom-unit-${unit.id}-flashcards`,
        title: `Flashcards · ${unit.title}`,
        flashcardSets: (unit.flashcards || []).map((reference) => ({
          id: reference.resourceId,
          title: reference.title,
          description: reference.note || reference.subtitle,
          cardsJson: reference.contentJson || '[]',
        })),
      }],
    })),
});

export default function FlashcardPracticePage() {
  const [courses, setCourses] = useState([]);
  const [classrooms, setClassrooms] = useState([]);
  const [sourceKey, setSourceKey] = useState('');
  const [studyCourse, setStudyCourse] = useState(undefined);
  const [terms, setTerms] = useState([]);
  const [selectedModule, setSelectedModule] = useState('ALL');
  const [loadingSources, setLoadingSources] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const loadRequestId = useRef(0);

  const sources = useMemo(() => [
    ...courses.map((course) => ({
      key: `ONLINE:${course.courseId || course.id}`,
      id: Number(course.courseId || course.id),
      type: 'ONLINE',
      title: course.courseTitle || course.title,
      label: `[Khóa học online] ${course.courseTitle || course.title}`,
      terms: course.terms,
      studyCourse: { id: course.courseId || course.id, title: course.courseTitle || course.title }
    })),
    ...classrooms.map((classroom) => ({
      key: `CLASSROOM:${classroom.id}`,
      id: Number(classroom.id),
      type: 'CLASSROOM',
      title: classroom.title || classroom.learningPackageTitle || `Lớp học #${classroom.id}`,
      label: `[${classroomTypeLabel(classroom.deliveryMode)}] ${classroom.title || classroom.learningPackageTitle || `Lớp học #${classroom.id}`}`,
      terms: classroom.terms,
      studyCourse: toClassroomFlashcardCourse(classroom.detail)
    })),
  ], [classrooms, courses]);

  const selectedSource = useMemo(
    () => sources.find((source) => source.key === sourceKey),
    [sourceKey, sources],
  );

  const loadTerms = useCallback(async () => {
    const requestId = ++loadRequestId.current;
    if (!selectedSource) {
      setStudyCourse(undefined);
      setTerms([]);
      setLoading(false);
      return;
    }

    const hasCache = selectedSource.terms && selectedSource.terms.length > 0;
    if (!hasCache) {
      setLoading(true);
    }
    setError('');
    try {
      if (selectedSource.type === 'ONLINE') {
        const data = await courseApi.getGlobalFlashcardPractice({ courseId: selectedSource.id });
        if (requestId !== loadRequestId.current) return;
        setStudyCourse(selectedSource.studyCourse);
        setTerms(data);
        
        // Update cache in state
        setCourses((prev) => prev.map((c) => 
          (c.courseId || c.id) === selectedSource.id ? { ...c, terms: data } : c
        ));
      } else {
        const classroom = await classroomApi.getMyClassroom(selectedSource.id);
        const classroomCourse = toClassroomFlashcardCourse(classroom);
        const data = extractVocabularyTerms(classroomCourse);
        if (requestId !== loadRequestId.current) return;
        setStudyCourse(classroomCourse);
        setTerms(data);
        
        // Update cache in state
        setClassrooms((prev) => prev.map((c) => 
          c.id === selectedSource.id ? { ...c, detail: classroom, terms: data } : c
        ));
      }
    } catch (requestError) {
      if (requestId !== loadRequestId.current) return;
      if (!hasCache) {
        setStudyCourse(undefined);
        setTerms([]);
      }
      setError(requestError.response?.data?.message || 'Chưa thể tải flashcard. Vui lòng thử lại.');
    } finally {
      if (requestId === loadRequestId.current) setLoading(false);
    }
  }, [selectedSource]);

  useEffect(() => {
    let active = true;
    const loadSources = async () => {
      setLoadingSources(true);
      setError('');
      try {
        const [courseResult, classroomResult] = await Promise.allSettled([
          courseApi.getMyOnlineCourses(),
          classroomApi.getMyClassrooms(),
        ]);

        const fetchedCourses = courseResult.status === 'fulfilled' ? courseResult.value : [];
        const fetchedClassrooms = classroomResult.status === 'fulfilled' ? classroomResult.value : [];

        // Load course flashcards in parallel to filter out those with 0 flashcards
        const coursesWithFlashcards = [];
        const coursePromises = fetchedCourses.map(async (course) => {
          try {
            const data = await courseApi.getGlobalFlashcardPractice({ courseId: course.courseId || course.id });
            if (data && data.length > 0) {
              coursesWithFlashcards.push({
                ...course,
                terms: data,
              });
            }
          } catch (e) {
            console.error("Error loading course flashcards", e);
          }
        });

        // Load classroom details in parallel to filter out those with 0 flashcards
        const classroomsWithFlashcards = [];
        const classroomPromises = fetchedClassrooms.map(async (classroom) => {
          try {
            const detail = await classroomApi.getMyClassroom(classroom.id);
            const classroomCourse = toClassroomFlashcardCourse(detail);
            const termsList = extractVocabularyTerms(classroomCourse);
            if (termsList && termsList.length > 0) {
              classroomsWithFlashcards.push({
                ...classroom,
                detail,
                terms: termsList,
              });
            }
          } catch (e) {
            console.error("Error loading classroom details", e);
          }
        });

        await Promise.all([...coursePromises, ...classroomPromises]);

        if (active) {
          setCourses(coursesWithFlashcards);
          setClassrooms(classroomsWithFlashcards);
        }
      } catch (err) {
        if (active) setError('Chưa thể tải danh sách khóa học và lớp học của bạn.');
      } finally {
        if (active) setLoadingSources(false);
      }
    };

    loadSources();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (selectedSource) {
      setTerms(selectedSource.terms || []);
      setStudyCourse(selectedSource.studyCourse);
    } else {
      setTerms([]);
      setStudyCourse(undefined);
    }
  }, [selectedSource]);

  useEffect(() => {
    const fetchTerms = async () => {
      await loadTerms();
    };
    fetchTerms();
  }, [loadTerms]);

  const sourceOptions = sources.map((source) => ({ label: source.label, value: source.key }));

  const moduleOptions = useMemo(() => {
    if (!terms || terms.length === 0) return [];
    const uniqueModules = Array.from(new Set(terms.map((t) => t.moduleTitle).filter(Boolean)));
    return [
      { label: 'Toàn bộ bài học', value: 'ALL' },
      ...uniqueModules.map((m) => ({ label: m, value: m })),
    ];
  }, [terms]);

  const filteredTerms = useMemo(() => {
    if (!selectedModule || selectedModule === 'ALL') return terms;
    return terms.filter((t) => t.moduleTitle === selectedModule);
  }, [terms, selectedModule]);

  const emptyDescription = selectedSource?.type === 'CLASSROOM'
    ? 'Giáo trình của lớp này chưa có flashcard.'
    : 'Khóa học online này chưa có flashcard.';

  return (
    <LearnerPageShell
      description="Chọn khóa học để ôn flashcard đúng với nội dung bạn đang học."
      eyebrow="Flashcard practice"
      title="Luyện từ vựng"
    >
      <section className="mb-8 grid gap-3 rounded-[24px] border border-[#ead9db]/85 bg-white p-4 shadow-[0_8px_30px_rgba(75,0,9,0.015)] lg:grid-cols-[1fr_280px_auto]">
        <BrandedSelect
          buttonClassName="h-full rounded-2xl border-[#dfbfbd]/50 bg-[#fffdfd]"
          disabled={loadingSources}
          onChange={(event) => {
            setSourceKey(event.target.value);
            setSelectedModule('ALL');
          }}
          options={sourceOptions}
          placeholder={loadingSources ? 'Đang tải nội dung học...' : 'Chọn khóa học hoặc lớp học'}
          value={sourceKey}
          searchable={true}
        />
        
        <BrandedSelect
          buttonClassName="h-full rounded-2xl border-[#dfbfbd]/50 bg-[#fffdfd]"
          disabled={!sourceKey || loading || moduleOptions.length <= 1}
          onChange={(event) => setSelectedModule(event.target.value)}
          options={moduleOptions}
          placeholder="Toàn bộ bài học"
          value={selectedModule}
        />

        <button
          className="inline-flex items-center justify-center gap-2 rounded-2xl border border-[#dfbfbd] bg-white px-6 py-3.5 text-sm font-extrabold text-[#730014] shadow-sm transition hover:bg-[#fff2f3] active:scale-95 disabled:opacity-50"
          disabled={loading || !sourceKey}
          onClick={loadTerms}
          type="button"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </button>
      </section>

      {error ? <div className="mb-6 rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm font-semibold text-red-700">{error}</div> : null}
      {!sourceKey && !loadingSources ? <section className="rounded-[28px] border border-dashed border-[#dfbfbd] bg-white p-12 text-center font-semibold text-[#584140] shadow-sm">Chọn khóa học online hoặc lớp học để bắt đầu luyện flashcard.</section> : null}
      {loading ? <section className="rounded-[28px] border border-[#ead9db] bg-white p-12 text-center font-semibold text-[#584140] shadow-sm">Đang tải bộ flashcard...</section> : null}
      {sourceKey && !loading ? <>
        {filteredTerms.length ? <p className="mb-4 text-sm font-semibold text-[#6a5553]">Đã tải {filteredTerms.length} thẻ từ {selectedSource?.label.toLowerCase()}.</p> : null}
        <WorkspaceFlashcards
          course={studyCourse}
          emptyStateDescription={emptyDescription}
          key={`${sourceKey}-${selectedModule}`}
          termsOverride={filteredTerms}
        />
      </> : null}
    </LearnerPageShell>
  );
}
