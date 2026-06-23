import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Copy, Plus, RefreshCw, X } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { Panel, StatusBadge } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';

const pageMap = {
  listening: {
    heading: 'quản lý bài nghe',
    matcher: (assessment) => String(assessment.skill || '').toUpperCase() === 'LISTENING',
  },
  writing: {
    heading: 'quản lý bài viết',
    matcher: (assessment) => {
      const skill = String(assessment.skill || '').toUpperCase();
      const type = String(assessment.type || '').toUpperCase();
      return skill === 'WRITING' || type === 'WRITING_TASK';
    },
  },
  reading: {
    heading: 'quản lý bài đọc',
    matcher: (assessment) => String(assessment.skill || '').toUpperCase() === 'READING',
  },
  speaking: {
    heading: 'quản lý bài nói',
    matcher: (assessment) => {
      const skill = String(assessment.skill || '').toUpperCase();
      const type = String(assessment.type || '').toUpperCase();
      return skill === 'SPEAKING' || type === 'SPEAKING_TASK';
    },
  },
  mockExams: {
    heading: 'quản lý đề thi thử',
    matcher: (assessment) => String(assessment.type || '').toUpperCase() === 'MOCK_TEST',
  },
};

const pageSize = 12;

export default function ContentManagerAssessmentsHubPage({ pageKey }) {
  const navigate = useNavigate();
  const pageConfig = pageMap[pageKey];
  const [courses, setCourses] = useState([]);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [warning, setWarning] = useState('');
  const [filters, setFilters] = useState({ course: 'ALL', status: 'ALL' });
  const [page, setPage] = useState(1);
  const [reuseDialog, setReuseDialog] = useState(false);
  const [targetCourseId, setTargetCourseId] = useState('');
  const [targetModuleId, setTargetModuleId] = useState('');
  const [targetModules, setTargetModules] = useState([]);
  const [loadingTargetModules, setLoadingTargetModules] = useState(false);
  const [reusing, setReusing] = useState(false);
  const loadData = async (activeRef = { current: true }) => {
    setLoading(true);
    setError('');
    setWarning('');
    try {
      const page = await courseApi.getManagedOnlineCourses({ page: 0, size: 200 });
      if (!activeRef.current) return;
      const managedCourses = page.content || [];
      setCourses(managedCourses);

      const assessmentGroups = await Promise.all(
        managedCourses.map(async (course) => {
          try {
            const items = await courseApi.getManagedCourseAssessments(course.id);
            return { course, items, failed: false };
          } catch {
            return { course, items: [], failed: true };
          }
        }),
      );
      if (!activeRef.current) return;
      const failedCourses = assessmentGroups.filter((group) => group.failed);
      if (failedCourses.length) {
        setWarning(
          `Không tải được bài kiểm tra của ${failedCourses.length} khóa học. `
          + 'Danh sách bên dưới vẫn hiển thị các dữ liệu đã tải thành công.',
        );
      }

      const flattened = assessmentGroups.flatMap(({ course, items }) =>
        (items || [])
          .filter((assessment) => pageConfig.matcher(assessment))
          .map((assessment) => ({
            id: `${course.id}-${assessment.id || assessment.title}`,
            assessmentId: assessment.id,
            moduleId: assessment.moduleId,
            title: assessment.title || 'Bài đánh giá chưa đặt tên',
            courseTitle: course.title,
            moduleTitle: assessment.moduleTitle || 'Cuối khóa',
            skill: formatLabel(assessment.skill),
            type: formatLabel(assessment.type),
            status: assessment.active === false ? 'ARCHIVED' : course.status || 'DRAFT',
            slug: course.slug,
            courseId: String(course.id),
            updatedAt: formatDate(course.updatedAt),
            assessment,
          })),
      );

      setRows(flattened);
    } catch {
      if (activeRef.current) setError(`Không tải được dữ liệu ${pageConfig.heading}.`);
    } finally {
      if (activeRef.current) setLoading(false);
    }
  };

  useEffect(() => {
    const activeRef = { current: true };
    loadData(activeRef);
    return () => {
      activeRef.current = false;
    };
  }, [pageConfig, pageKey]);

  const courseOptions = useMemo(
    () => [
      { label: 'Tất cả khóa học', value: 'ALL' },
      ...courses.map((course) => ({ label: course.title, value: String(course.id) })),
    ],
    [courses],
  );

  const filteredRows = useMemo(
    () =>
      rows
        .filter((row) => filters.course === 'ALL' || row.courseId === filters.course)
        .filter((row) => filters.status === 'ALL' || row.status === filters.status),
    [filters, rows],
  );
  const totalPages = Math.max(1, Math.ceil(filteredRows.length / pageSize));
  const visibleRows = filteredRows.slice((page - 1) * pageSize, page * pageSize);

  useEffect(() => {
    setPage(1);
  }, [filters.course, filters.status, pageKey]);

  useEffect(() => {
    if (page > totalPages) setPage(totalPages);
  }, [page, totalPages]);

  useEffect(() => {
    if (!targetCourseId || reuseDialog === false) {
      setTargetModules([]);
      return undefined;
    }
    let active = true;
    setLoadingTargetModules(true);
    courseApi.getManagedOnlineCourse(targetCourseId)
      .then((course) => {
        if (active) setTargetModules(course.modules || []);
      })
      .catch(() => {
        if (active) setTargetModules([]);
      })
      .finally(() => {
        if (active) setLoadingTargetModules(false);
      });
    return () => { active = false; };
  }, [reuseDialog, targetCourseId]);

  const openReuseDialog = (source = null) => {
    setReuseDialog(source || { isNew: true });
    setTargetCourseId('');
    setTargetModuleId('');
    setTargetModules([]);
  };

  const reuseAssessment = async () => {
    if (!targetCourseId) {
      setError('Hãy chọn khóa học nhận bài kiểm tra.');
      return;
    }
    const targetCourse = courses.find((course) => String(course.id) === String(targetCourseId));
    if (!targetCourse) return;
    setReusing(true);
    setError('');
    try {
      const targetAssessments = await courseApi.getManagedCourseAssessments(targetCourse.id);
      const assessment = reuseDialog?.isNew
        ? createNewAssessment(pageKey, targetAssessments.length + 1, targetModuleId)
        : toReusableAssessment(reuseDialog, targetAssessments.length + 1, targetModuleId);
      const saved = await courseApi.saveManagedCourseAssessments(targetCourse.id, [...targetAssessments, assessment]);
      const created = [...saved].sort((left, right) => Number(right.id || 0) - Number(left.id || 0))[0];
      setReuseDialog(false);
      navigate(`/content-manager/courses/${targetCourse.slug}/builder?assessmentId=${encodeURIComponent(created?.id || '')}`);
    } catch (requestError) {
      setError(requestError?.response?.data?.message || 'Không thể thêm bài kiểm tra vào khóa học đã chọn.');
    } finally {
      setReusing(false);
    }
  };

  return (
    <motion.div
      className="space-y-6"
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.32, ease: 'easeOut' }}
    >
      <div className="flex justify-end">
        <div className="flex flex-wrap justify-end gap-3">
          <button className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#730014]" onClick={() => openReuseDialog(null)} type="button"><Plus className="h-4 w-4" /> Thêm bài mới</button>
          <button
            className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/65 bg-white px-4 py-3 text-sm font-bold text-[#730014] transition hover:bg-[#fff2f3]"
            onClick={() => loadData()}
            type="button"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới dữ liệu
          </button>
        </div>
      </div>

      {error ? (
        <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">
          {error}
        </div>
      ) : null}
      {warning ? (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-800">
          {warning}
        </div>
      ) : null}

      <Panel className="p-6">
        <div className="grid gap-3 lg:grid-cols-[1fr_1fr_auto]">
          <FilterField label="Khóa học">
            <BrandedSelect
              onChange={(event) => setFilters((current) => ({ ...current, course: event.target.value }))}
              options={courseOptions}
              value={filters.course}
            />
          </FilterField>
          <FilterField label="Trạng thái">
            <BrandedSelect
              onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))}
              options={[
                { label: 'Tất cả trạng thái', value: 'ALL' },
                { label: 'Nháp', value: 'DRAFT' },
                { label: 'Đã xuất bản', value: 'PUBLISHED' },
                { label: 'Lưu trữ', value: 'ARCHIVED' },
              ]}
              value={filters.status}
            />
          </FilterField>
          <div className="flex items-end">
            <Link
              className="inline-flex w-full items-center justify-center rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#730014] lg:w-auto"
              to="/content-manager/courses"
            >
              Mở quản lý khóa học
            </Link>
          </div>
        </div>
      </Panel>

      <Panel className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full text-left">
            <thead className="bg-[#fbf3f4] text-xs uppercase tracking-[0.18em] text-[#8e7371]">
              <tr>
                {['Tên nội dung', 'Khóa học', 'Thuộc phần', 'Kỹ năng', 'Loại', 'Trạng thái', 'Cập nhật', 'Thao tác'].map((heading) => (
                  <th key={heading} className="px-5 py-4 font-semibold">
                    {heading}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#f0e3e4]">
              {loading ? (
                <tr>
                  <td className="px-5 py-8 text-sm text-[#584140]" colSpan={8}>
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : visibleRows.length ? (
                visibleRows.map((row) => (
                  <tr key={row.id}>
                    <td className="px-5 py-4 font-semibold text-[#1a1c1c]">{row.title}</td>
                    <td className="px-5 py-4 text-sm">{row.courseTitle}</td>
                    <td className="px-5 py-4 text-sm">{row.moduleTitle}</td>
                    <td className="px-5 py-4 text-sm">{row.skill}</td>
                    <td className="px-5 py-4 text-sm">{row.type}</td>
                    <td className="px-5 py-4">
                      <StatusBadge label={row.status} />
                    </td>
                    <td className="px-5 py-4 text-sm">{row.updatedAt}</td>
                    <td className="px-5 py-4">
                      <div className="flex flex-wrap gap-2">
                        <Link
                          className="rounded-xl border border-[#dfbfbd]/60 px-3 py-2 text-sm font-medium text-[#730014] transition hover:bg-[#fff2f3]"
                          to={`/content-manager/courses/${row.slug}/builder?assessmentId=${encodeURIComponent(row.assessmentId || '')}${row.moduleId ? `&moduleId=${encodeURIComponent(row.moduleId)}` : ''}`}
                        >
                          Mở bài kiểm tra
                        </Link>
                        <Link
                          className="rounded-xl bg-[#4b0009] px-3 py-2 text-sm font-medium text-white transition hover:bg-[#730014]"
                          to={`/content-manager/courses/${row.slug}/builder?assessmentId=${encodeURIComponent(row.assessmentId || '')}${row.moduleId ? `&moduleId=${encodeURIComponent(row.moduleId)}` : ''}`}
                        >
                          Chỉnh sửa bài kiểm tra
                        </Link>
                        <button className="inline-flex items-center gap-1 rounded-xl border border-[#dfbfbd]/60 px-3 py-2 text-sm font-medium text-[#730014] transition hover:bg-[#fff2f3]" onClick={() => openReuseDialog(row)} type="button"><Copy className="h-3.5 w-3.5" /> Thêm vào khóa học</button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td className="px-5 py-8 text-sm text-[#584140]" colSpan={8}>
                    Chưa có nội dung nào để quản lý trong mục này.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>

      {totalPages > 1 ? (
        <div className="flex items-center justify-center gap-3">
          <button className="rounded-xl border border-[#dfbfbd] bg-white px-4 py-2 text-sm font-bold text-[#730014] disabled:opacity-40" disabled={page <= 1} onClick={() => setPage((current) => current - 1)} type="button">
            Trang trước
          </button>
          <span className="text-sm font-semibold text-[#584140]">Trang {page} / {totalPages}</span>
          <button className="rounded-xl border border-[#dfbfbd] bg-white px-4 py-2 text-sm font-bold text-[#730014] disabled:opacity-40" disabled={page >= totalPages} onClick={() => setPage((current) => current + 1)} type="button">
            Trang sau
          </button>
        </div>
      ) : null}
      {reuseDialog !== false ? <ReuseDialog courses={courses} loadingModules={loadingTargetModules} modules={targetModules} onClose={() => setReuseDialog(false)} onSubmit={reuseAssessment} reusing={reusing} source={reuseDialog?.isNew ? null : reuseDialog} targetCourseId={targetCourseId} targetModuleId={targetModuleId} onTargetCourseChange={(value) => { setTargetCourseId(value); setTargetModuleId(''); }} onTargetModuleChange={setTargetModuleId} /> : null}
    </motion.div>
  );
}

function ReuseDialog({ courses, loadingModules, modules, onClose, onSubmit, onTargetCourseChange, onTargetModuleChange, reusing, source, targetCourseId, targetModuleId }) {
  const moduleOptions = [{ label: 'Đặt ở cuối khóa', value: '' }, ...modules.map((module) => ({ label: module.title, value: String(module.id) }))];
  return <div className="fixed inset-0 z-[100] flex items-center justify-center bg-[#1a0004]/45 p-4 backdrop-blur-sm"><div className="w-full max-w-lg rounded-[28px] bg-white p-6 shadow-2xl"><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-[.16em] text-[#8b706e]">{source ? 'Tái sử dụng bài kiểm tra' : 'Tạo bài kiểm tra'}</p><h3 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#4b0009]">{source ? source.title : 'Bài kiểm tra mới'}</h3></div><button aria-label="Đóng" className="rounded-xl border border-[#dfbfbd] p-2 text-[#730014]" onClick={onClose} type="button"><X className="h-5 w-5" /></button></div><p className="mt-3 text-sm leading-6 text-[#584140]">{source ? 'Bản sao sẽ được thêm vào vị trí bạn chọn. Bạn có thể chỉnh sửa độc lập sau đó.' : 'Một bài kiểm tra trống sẽ được thêm vào vị trí bạn chọn và mở ngay khu vực biên soạn.'}</p><div className="mt-5"><label className="mb-2 block text-xs font-bold uppercase tracking-[.14em] text-[#8b706e]">Khóa học nhận bài kiểm tra</label><BrandedSelect onChange={(event) => onTargetCourseChange(event.target.value)} options={[{ label: 'Chọn khóa học', value: '' }, ...courses.map((course) => ({ label: course.title, value: String(course.id) }))]} value={targetCourseId} /></div>{targetCourseId ? <div className="mt-4"><label className="mb-2 block text-xs font-bold uppercase tracking-[.14em] text-[#8b706e]">Vị trí đặt bài kiểm tra</label><BrandedSelect disabled={loadingModules} onChange={(event) => onTargetModuleChange(event.target.value)} options={loadingModules ? [{ label: 'Đang tải mô-đun...', value: '' }] : moduleOptions} value={targetModuleId} /></div> : null}<div className="mt-6 flex justify-end gap-3"><button className="rounded-xl border border-[#dfbfbd] px-4 py-3 text-sm font-bold text-[#730014]" onClick={onClose} type="button">Hủy</button><button className="rounded-xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white disabled:opacity-50" disabled={reusing || !targetCourseId || loadingModules} onClick={onSubmit} type="button">{reusing ? 'Đang thêm...' : source ? 'Thêm vào khóa học' : 'Tạo và biên soạn'}</button></div></div></div>;
}

function toReusableAssessment(source, displayOrder, moduleId) {
  const assessment = source.assessment || {};
  return {
    title: assessment.title || source.title,
    description: assessment.description || '',
    type: assessment.type || 'LESSON_PRACTICE',
    skill: assessment.skill || 'MIXED',
    moduleId: moduleId || null,
    rubricId: assessment.rubricId || null,
    instructions: assessment.instructions || '',
    objectiveAnswerKey: assessment.objectiveAnswerKey || '',
    uiConfigJson: assessment.uiConfigJson || null,
    passingScore: assessment.passingScore ?? null,
    maxScore: assessment.maxScore ?? 100,
    timeLimitMinutes: assessment.timeLimitMinutes ?? null,
    aiEvaluationMode: assessment.aiEvaluationMode || 'NONE',
    active: assessment.active !== false,
    displayOrder,
  };
}

function createNewAssessment(pageKey, displayOrder, moduleId) {
  const skillByPage = { listening: 'LISTENING', reading: 'READING', writing: 'WRITING', speaking: 'SPEAKING', mockExams: 'MIXED' };
  const typeByPage = { writing: 'WRITING_TASK', speaking: 'SPEAKING_TASK', mockExams: 'MOCK_TEST' };
  return { title: 'Bài kiểm tra mới', description: '', type: typeByPage[pageKey] || 'LESSON_PRACTICE', skill: skillByPage[pageKey] || 'MIXED', moduleId: moduleId || null, rubricId: null, instructions: '', objectiveAnswerKey: '', uiConfigJson: null, passingScore: null, maxScore: 100, timeLimitMinutes: null, aiEvaluationMode: 'NONE', active: true, displayOrder };
}

function FilterField({ label, children }) {
  return (
    <div className="space-y-2">
      <label className="block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">{label}</label>
      {children}
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
    QUIZ: 'Câu hỏi luyện tập',
  };
  return labels[text] || value || '-';
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}
