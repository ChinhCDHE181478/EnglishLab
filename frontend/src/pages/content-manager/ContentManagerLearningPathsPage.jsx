import { useEffect, useMemo, useState } from 'react';
import { ArrowRight, Check, GripVertical, RefreshCw, Route, Settings2, X } from 'lucide-react';
import courseApi from '../../api/courseApi';
import { ContentManagerLoadingState, Panel, TextField } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { buildManagedCoursePayload } from '../../utils/contentManagerCoursePayload';

const pageSize = 5;

export default function ContentManagerLearningPathsPage() {
  const [courses, setCourses] = useState([]);
  const [selectedPath, setSelectedPath] = useState('ALL');
  const [editingCourse, setEditingCourse] = useState(null);
  const [form, setForm] = useState({});
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showColumns, setShowColumns] = useState(false);
  const [visibleColumns, setVisibleColumns] = useState({ outcome: true, next: true });
  const [draggedCourse, setDraggedCourse] = useState(null);
  const [creatingPath, setCreatingPath] = useState(false);

  const loadCourses = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await courseApi.getManagedOnlineCourses({ page: 0, size: 500 });
      setCourses(response.content || []);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được dữ liệu lộ trình học.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCourses();
  }, []);

  const pathOptions = useMemo(() => {
    const paths = new Map();
    courses.forEach((course) => {
      const code = String(course.learningPathCode || '').trim();
      if (!code) return;
      paths.set(code, course.learningPathName || code);
    });
    return [
      { label: 'Tất cả lộ trình', value: 'ALL' },
      { label: 'Chưa thuộc lộ trình', value: 'UNASSIGNED' },
      ...Array.from(paths.entries()).map(([value, label]) => ({ value, label })),
    ];
  }, [courses]);

  const groups = useMemo(() => {
    const grouped = new Map();
    courses.forEach((course) => {
      const code = String(course.learningPathCode || '').trim() || 'UNASSIGNED';
      if (selectedPath !== 'ALL' && selectedPath !== code) return;
      if (!grouped.has(code)) {
        grouped.set(code, {
          code,
          name: code === 'UNASSIGNED' ? 'Chưa thuộc lộ trình' : course.learningPathName || code,
          courses: [],
        });
      }
      grouped.get(code).courses.push(course);
    });
    return Array.from(grouped.values())
      .map((group) => ({
        ...group,
        courses: group.courses.sort((left, right) => {
          const orderDiff = Number(left.learningPathOrder || 0) - Number(right.learningPathOrder || 0);
          return orderDiff || String(left.title).localeCompare(String(right.title), 'vi');
        }),
      }))
      .sort((left, right) => (left.code === 'UNASSIGNED' ? 1 : right.code === 'UNASSIGNED' ? -1 : left.name.localeCompare(right.name, 'vi')));
  }, [courses, selectedPath]);

  const totalPages = Math.max(1, Math.ceil(groups.length / pageSize));
  const visibleGroups = groups.slice((page - 1) * pageSize, page * pageSize);

  useEffect(() => {
    setPage(1);
  }, [selectedPath]);

  useEffect(() => {
    if (page > totalPages) setPage(totalPages);
  }, [page, totalPages]);

  const openEditor = (course) => {
    setEditingCourse(course);
    setForm({
      learningPathCode: course.learningPathCode || '',
      learningPathName: course.learningPathName || '',
      learningPathOrder: String(course.learningPathOrder ?? 0),
      recommendedNextCourseSlug: course.recommendedNextCourseSlug || '',
    });
    setError('');
    setSuccess('');
  };

  const openCreatePath = () => {
    const availableCourse = courses.find((course) => !String(course.learningPathCode || '').trim());
    if (!availableCourse) {
      setError('Cần có ít nhất một khóa học chưa thuộc lộ trình để tạo lộ trình mới.');
      return;
    }
    setCreatingPath(true);
    openEditor(availableCourse);
    setForm({ learningPathCode: '', learningPathName: '', learningPathOrder: '1', recommendedNextCourseSlug: '' });
  };

  const deletePath = async (group) => {
    if (!window.confirm(`Gỡ ${group.courses.length} khóa học khỏi lộ trình “${group.name}”? Khóa học sẽ không bị xóa.`)) return;
    setSaving(true);
    setError('');
    try {
      await Promise.all(group.courses.map((course) => courseApi.updateOnlineCourse(course.id, buildManagedCoursePayload(course, {
        learningPathCode: null,
        learningPathName: null,
        learningPathOrder: null,
        recommendedNextCourseSlug: null,
      }))));
      await loadCourses();
      setSuccess('Đã gỡ các khóa học khỏi lộ trình.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể xóa lộ trình.');
    } finally {
      setSaving(false);
    }
  };

  const handleDrop = async (targetCourse) => {
    if (!draggedCourse || draggedCourse.id === targetCourse.id || draggedCourse.learningPathCode !== targetCourse.learningPathCode) return;
    const pathCourses = courses.filter((course) => course.learningPathCode === targetCourse.learningPathCode)
      .sort((left, right) => Number(left.learningPathOrder || 0) - Number(right.learningPathOrder || 0));
    const from = pathCourses.findIndex((course) => course.id === draggedCourse.id);
    const to = pathCourses.findIndex((course) => course.id === targetCourse.id);
    pathCourses.splice(to, 0, pathCourses.splice(from, 1)[0]);
    const updatedCourses = pathCourses.map((course, index) => ({ ...course, learningPathOrder: index + 1 }));
    setCourses((current) => current.map((course) => updatedCourses.find((item) => item.id === course.id) || course));
    setDraggedCourse(null);
    try {
      await Promise.all(updatedCourses.map((course) => courseApi.updateOnlineCourse(course.id, buildManagedCoursePayload(course, {
        learningPathOrder: course.learningPathOrder,
      }))));
      setSuccess('Đã cập nhật thứ tự các khóa học trong lộ trình.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể lưu thứ tự mới. Vui lòng làm mới và thử lại.');
      await loadCourses();
    }
  };

  const savePath = async () => {
    if (!editingCourse) return;
    const code = form.learningPathCode.trim();
    const name = form.learningPathName.trim();
    if ((code && !name) || (!code && name)) {
      setError('Mã và tên lộ trình phải được nhập cùng nhau.');
      return;
    }

    setSaving(true);
    setError('');
    try {
      const pathCourses = creatingPath || !editingCourse.learningPathCode
        ? [editingCourse]
        : courses.filter((course) => course.learningPathCode === editingCourse.learningPathCode);
      const updatedCourses = await Promise.all(pathCourses.map((course) => courseApi.updateOnlineCourse(
        course.id,
        buildManagedCoursePayload(course, {
          learningPathCode: code || null,
          learningPathName: name || null,
          learningPathOrder: code ? (course.id === editingCourse.id ? Number(form.learningPathOrder || 1) : course.learningPathOrder) : null,
          recommendedNextCourseSlug: course.id === editingCourse.id ? form.recommendedNextCourseSlug.trim() || null : course.recommendedNextCourseSlug,
        }),
      )));
      setCourses((current) => current.map((course) => updatedCourses.find((item) => item.id === course.id) || course));
      setEditingCourse(null);
      setCreatingPath(false);
      setSuccess('Đã cập nhật vị trí của khóa học trong lộ trình.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được lộ trình học.');
    } finally {
      setSaving(false);
    }
  };

  if (loading && !courses.length) {
    return <ContentManagerLoadingState message="Đang tải lộ trình học..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="w-full max-w-md">
          <span className="mb-2 block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">Lộ trình</span>
          <BrandedSelect
            onChange={(event) => setSelectedPath(event.target.value)}
            options={pathOptions}
            value={selectedPath}
          />
        </div>
        <div className="flex items-center gap-2">
        <button className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white" onClick={openCreatePath} type="button">+ Tạo lộ trình</button>
        <button className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm font-bold text-[#730014]" onClick={() => setShowColumns((current) => !current)} type="button"><Settings2 className="h-4 w-4" />Cột hiển thị</button>
        <button
          className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm font-bold text-[#730014]"
          onClick={loadCourses}
          type="button"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </button>
        </div>
      </div>

      {showColumns ? <div className="flex flex-wrap gap-4 rounded-2xl border border-[#ead9db] bg-[#fffdfc] px-5 py-3 text-sm font-semibold text-[#584140]">
        {Object.entries({ outcome: 'Mục tiêu đầu ra', next: 'Khóa học tiếp theo' }).map(([key, label]) => <label className="inline-flex items-center gap-2" key={key}><input checked={visibleColumns[key]} onChange={() => setVisibleColumns((current) => ({ ...current, [key]: !current[key] }))} type="checkbox" />{label}</label>)}
      </div> : null}

      {error ? <Notice tone="error">{error}</Notice> : null}
      {success ? <Notice tone="success">{success}</Notice> : null}

      {editingCourse ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#240005]/40 p-4"><Panel className="max-h-[90vh] w-full max-w-2xl overflow-y-auto p-6 shadow-2xl">
          <div className="mb-5 flex items-center justify-between gap-4">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.18em] text-[#8b706e]">Đang sắp xếp khóa học</p>
              <h2 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#4b0009]">{editingCourse.title}</h2>
            </div>
            <button className="rounded-xl p-2 text-[#730014] hover:bg-[#fff2f3]" onClick={() => { setEditingCourse(null); setCreatingPath(false); }} type="button">
              <X className="h-5 w-5" />
            </button>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <TextField label="Mã lộ trình" onChange={(event) => setForm((current) => ({ ...current, learningPathCode: event.target.value }))} value={form.learningPathCode} />
            <TextField label="Tên lộ trình" onChange={(event) => setForm((current) => ({ ...current, learningPathName: event.target.value }))} value={form.learningPathName} />
            <TextField label="Thứ tự trong lộ trình" onChange={(event) => setForm((current) => ({ ...current, learningPathOrder: event.target.value }))} value={form.learningPathOrder} />
            <TextField label="Slug khóa học tiếp theo" onChange={(event) => setForm((current) => ({ ...current, recommendedNextCourseSlug: event.target.value }))} value={form.recommendedNextCourseSlug} />
          </div>
          <div className="mt-5 flex justify-end gap-3">
            <button className="rounded-2xl border border-[#dfbfbd] px-4 py-3 text-sm font-bold text-[#730014]" onClick={() => { setEditingCourse(null); setCreatingPath(false); }} type="button">Hủy</button>
            <button className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white disabled:opacity-60" disabled={saving} onClick={savePath} type="button">
              <Check className="h-4 w-4" />
              {saving ? 'Đang lưu...' : 'Lưu lộ trình'}
            </button>
          </div>
        </Panel></div>
      ) : null}

      {visibleGroups.length ? visibleGroups.map((group) => (
        <Panel key={group.code} className="overflow-hidden">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#f0e3e4] px-6 py-5">
            <div className="flex items-center gap-3">
              <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]">
                <Route className="h-5 w-5" />
              </span>
              <div>
                <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">{group.name}</h2>
                <p className="mt-1 text-sm text-[#584140]">{group.courses.length} khóa học</p>
              </div>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-[#fff2f3] px-3 py-1 text-xs font-bold text-[#730014]">{group.code}</span>
              {group.code !== 'UNASSIGNED' ? <>
                <button className="rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]" onClick={() => openEditor(group.courses[0])} type="button">Cập nhật</button>
                <button className="rounded-xl border border-rose-200 px-3 py-2 text-sm font-semibold text-rose-700" onClick={() => deletePath(group)} type="button">Xóa lộ trình</button>
              </> : null}
            </div>
          </div>
          <div className="divide-y divide-[#f0e3e4]">
            {group.courses.map((course, index) => (
              <div draggable={group.code !== 'UNASSIGNED'} key={course.id} onDragEnd={() => setDraggedCourse(null)} onDragOver={(event) => event.preventDefault()} onDragStart={() => setDraggedCourse(course)} onDrop={() => handleDrop(course)} className="grid cursor-grab gap-4 px-6 py-5 active:cursor-grabbing lg:grid-cols-[80px_1fr_220px_auto] lg:items-center">
                <div className="text-sm font-bold text-[#730014]">
                  <span className="inline-flex items-center gap-1"><GripVertical className="h-4 w-4 text-[#b99593]" />{group.code === 'UNASSIGNED' ? '—' : `Bước ${course.learningPathOrder || index + 1}`}</span>
                </div>
                <div>
                  <p className="font-bold text-[#1a1c1c]">{course.title}</p>
                  {visibleColumns.outcome ? <p className="mt-1 text-sm text-[#584140]">{course.targetOutcome || course.shortDescription || 'Chưa có mô tả đầu ra.'}</p> : null}
                </div>
                <div className={`flex items-center gap-2 text-sm text-[#584140] ${visibleColumns.next ? '' : 'invisible'}`}>
                  {course.recommendedNextCourseSlug ? (
                    <>
                      <ArrowRight className="h-4 w-4 text-[#730014]" />
                      <span className="truncate">{course.recommendedNextCourseSlug}</span>
                    </>
                  ) : 'Chưa đặt khóa học tiếp theo'}
                </div>
                <div className="text-right text-xs font-semibold text-[#8b706e]">Kéo hàng để đổi thứ tự</div>
              </div>
            ))}
          </div>
        </Panel>
      )) : (
        <Panel className="p-10 text-center text-sm text-[#584140]">Không có khóa học nào trong nhóm này.</Panel>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </div>
  );
}

function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-center gap-3">
      <button className="rounded-xl border border-[#dfbfbd] bg-white px-4 py-2 text-sm font-bold text-[#730014] disabled:opacity-40" disabled={page <= 1} onClick={() => onChange(page - 1)} type="button">Trang trước</button>
      <span className="text-sm font-semibold text-[#584140]">Trang {page} / {totalPages}</span>
      <button className="rounded-xl border border-[#dfbfbd] bg-white px-4 py-2 text-sm font-bold text-[#730014] disabled:opacity-40" disabled={page >= totalPages} onClick={() => onChange(page + 1)} type="button">Trang sau</button>
    </div>
  );
}

function Notice({ children, tone }) {
  const className = tone === 'error'
    ? 'border-[#ba1a1a]/20 bg-[#ffdad6] text-[#93000a]'
    : 'border-emerald-200 bg-emerald-50 text-emerald-700';
  return <div className={`rounded-2xl border px-5 py-4 text-sm font-semibold ${className}`}>{children}</div>;
}
