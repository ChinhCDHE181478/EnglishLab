import { useEffect, useMemo, useState } from 'react';
import { Check, ChevronDown, ChevronUp, Plus, RefreshCw, Route, X } from 'lucide-react';
import courseApi from '../../api/courseApi';
import { ContentManagerLoadingState, Panel, TextField } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { buildManagedCoursePayload } from '../../utils/contentManagerCoursePayload';

const PAGE_SIZE = 5;

export default function ContentManagerLearningPathsPage() {
  const [courses, setCourses] = useState([]);
  const [selectedPath, setSelectedPath] = useState('ALL');
  const [expanded, setExpanded] = useState({});
  const [modal, setModal] = useState(null);
  const [form, setForm] = useState({ code: '', name: '' });
  const [courseIds, setCourseIds] = useState([]);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadCourses = async () => {
    setLoading(true);
    try {
      const result = await courseApi.getManagedOnlineCourses({ page: 0, size: 500 });
      setCourses(result.content || []);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải dữ liệu lộ trình.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadCourses(); }, []);

  const groups = useMemo(() => {
    const map = new Map();
    courses.forEach((course) => {
      const code = String(course.learningPathCode || '').trim() || 'UNASSIGNED';
      if (selectedPath !== 'ALL' && selectedPath !== code) return;
      if (!map.has(code)) map.set(code, { code, name: code === 'UNASSIGNED' ? 'Chưa thuộc lộ trình' : course.learningPathName || code, courses: [] });
      map.get(code).courses.push(course);
    });
    return [...map.values()].map((group) => ({
      ...group,
      courses: group.courses.sort((a, b) => Number(a.learningPathOrder || 0) - Number(b.learningPathOrder || 0) || Number(a.id) - Number(b.id)),
    }));
  }, [courses, selectedPath]);

  const pathOptions = useMemo(() => [
    { label: 'Tất cả lộ trình', value: 'ALL' },
    ...groups.map((group) => ({ label: group.name, value: group.code })),
  ], [groups]);
  const totalPages = Math.max(1, Math.ceil(groups.length / PAGE_SIZE));
  const visibleGroups = groups.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  useEffect(() => { setPage(1); }, [selectedPath]);

  const openModal = (group = null, mode = 'edit') => {
    setModal({ group, mode });
    setForm({ code: group?.code === 'UNASSIGNED' ? '' : group?.code || '', name: group?.code === 'UNASSIGNED' ? '' : group?.name || '' });
    setCourseIds(group?.courses.map((course) => course.id) || []);
    setError('');
  };

  const toggleCourse = (id) => setCourseIds((current) => (
    current.includes(id) ? current.filter((value) => value !== id) : [...current, id]
  ));
  const moveCourse = (id, direction) => setCourseIds((current) => {
    const index = current.indexOf(id);
    const target = index + direction;
    if (index < 0 || target < 0 || target >= current.length) return current;
    const next = [...current];
    [next[index], next[target]] = [next[target], next[index]];
    return next;
  });

  const savePath = async () => {
    const code = form.code.trim();
    const name = form.name.trim();
    if (!code || !name || !courseIds.length) {
      setError('Nhập mã, tên lộ trình và chọn ít nhất một khóa học.');
      return;
    }
    setSaving(true);
    try {
      const selectedCourses = courseIds.map((id) => courses.find((course) => course.id === id)).filter(Boolean);
      const removedCourses = (modal?.group?.courses || []).filter((course) => !courseIds.includes(course.id));
      await Promise.all([
        ...selectedCourses.map((course, index) => courseApi.updateOnlineCourse(course.id, buildManagedCoursePayload(course, {
          learningPathCode: code,
          learningPathName: name,
          learningPathOrder: index + 1,
        }))),
        ...removedCourses.map((course) => courseApi.updateOnlineCourse(course.id, buildManagedCoursePayload(course, {
          learningPathCode: null,
          learningPathName: null,
          learningPathOrder: null,
        }))),
      ]);
      setModal(null);
      setSuccess('Đã lưu lộ trình và thứ tự các khóa học.');
      await loadCourses();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể lưu lộ trình.');
    } finally {
      setSaving(false);
    }
  };

  const deletePath = async (group) => {
    if (!window.confirm(`Gỡ ${group.courses.length} khóa học khỏi lộ trình “${group.name}”?`)) return;
    setSaving(true);
    try {
      await Promise.all(group.courses.map((course) => courseApi.updateOnlineCourse(course.id, buildManagedCoursePayload(course, {
        learningPathCode: null,
        learningPathName: null,
        learningPathOrder: null,
      }))));
      setSuccess('Đã gỡ lộ trình.');
      await loadCourses();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể xóa lộ trình.');
    } finally {
      setSaving(false);
    }
  };

  if (loading && !courses.length) return <ContentManagerLoadingState message="Đang tải lộ trình học..." />;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="w-full max-w-md"><span className="mb-2 block text-xs font-bold uppercase tracking-[.16em] text-[#8b706e]">Lộ trình</span><BrandedSelect onChange={(event) => setSelectedPath(event.target.value)} options={pathOptions} value={selectedPath} /></div>
        <div className="flex gap-2"><button className="rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white" onClick={() => openModal()} type="button">+ Tạo lộ trình</button><button className="rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm font-bold text-[#730014]" onClick={loadCourses} type="button"><RefreshCw className="mr-2 inline h-4 w-4" />Làm mới</button></div>
      </div>
      {error ? <Notice tone="error">{error}</Notice> : null}{success ? <Notice>{success}</Notice> : null}
      {visibleGroups.map((group) => {
        const isExpanded = expanded[group.code] !== false;
        return <Panel key={group.code} className="overflow-hidden">
          <div className="flex items-center gap-3 px-6 py-5">
            <button className="flex min-w-0 flex-1 items-center gap-3 text-left" onClick={() => setExpanded((current) => ({ ...current, [group.code]: !isExpanded }))} type="button"><span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]"><Route className="h-5 w-5" /></span><span className="min-w-0"><span className="block truncate font-['Manrope'] text-xl font-extrabold text-[#4b0009]">{group.name}</span><span className="mt-1 block text-sm text-[#584140]">{group.courses.length} khóa học</span></span></button>
            {group.code !== 'UNASSIGNED' ? <div className="flex shrink-0 items-center gap-2"><button className="rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]" onClick={() => openModal(group, 'add')} type="button"><Plus className="mr-1 inline h-4 w-4" />Thêm</button><button className="rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]" onClick={() => openModal(group, 'edit')} type="button">Sửa</button><button className="rounded-xl border border-rose-200 px-3 py-2 text-sm font-semibold text-rose-700" onClick={() => deletePath(group)} type="button">Xóa</button></div> : null}
            <button className="rounded-xl p-2 text-[#730014]" onClick={() => setExpanded((current) => ({ ...current, [group.code]: !isExpanded }))} type="button"><ChevronDown className={`h-5 w-5 transition ${isExpanded ? 'rotate-180' : ''}`} /></button>
          </div>
          {isExpanded ? <div className="divide-y divide-[#f0e3e4] border-t border-[#f0e3e4]">{group.courses.map((course, index) => <div className="grid gap-4 px-6 py-5 lg:grid-cols-[80px_1fr]" key={course.id}><span className="font-bold text-[#730014]">Bước {course.learningPathOrder || index + 1}</span><div><p className="font-bold text-[#1a1c1c]">{course.title}</p><p className="mt-1 text-sm text-[#584140]">{course.targetOutcome || course.shortDescription || 'Chưa có mô tả đầu ra.'}</p></div></div>)}</div> : null}
        </Panel>;
      })}
      {totalPages > 1 ? <div className="flex justify-center gap-3"><button className="rounded-xl border border-[#dfbfbd] px-4 py-2 disabled:opacity-40" disabled={page <= 1} onClick={() => setPage((value) => value - 1)} type="button">Trang trước</button><span className="py-2 text-sm font-semibold">Trang {page}/{totalPages}</span><button className="rounded-xl border border-[#dfbfbd] px-4 py-2 disabled:opacity-40" disabled={page >= totalPages} onClick={() => setPage((value) => value + 1)} type="button">Trang sau</button></div> : null}
      {modal ? <div className="fixed inset-0 z-[100] overflow-y-auto bg-[#240005]/40 p-4"><div className="flex min-h-full items-center justify-center"><Panel className="my-5 w-full max-w-2xl p-6 shadow-2xl"><div className="flex items-center justify-between"><div><p className="text-xs font-bold uppercase tracking-[.18em] text-[#8b706e]">Quản lý lộ trình</p><h2 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#4b0009]">{modal.group ? (modal.mode === 'add' ? 'Thêm khóa học' : 'Cập nhật lộ trình') : 'Tạo lộ trình'}</h2></div><button className="rounded-xl p-2 text-[#730014]" onClick={() => setModal(null)} type="button"><X className="h-5 w-5" /></button></div><div className="mt-5 grid gap-4 md:grid-cols-2"><TextField label="Mã lộ trình" onChange={(event) => setForm((current) => ({ ...current, code: event.target.value }))} value={form.code} /><TextField label="Tên lộ trình" onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} value={form.name} /></div><div className="mt-5 rounded-2xl border border-[#ead9db] bg-[#fffdfc] p-4"><p className="font-extrabold text-[#4b0009]">Khóa học và thứ tự</p><p className="mt-1 text-xs text-[#6a5352]">Dùng mũi tên để sắp xếp. Các thay đổi chỉ được lưu sau khi bấm Lưu lộ trình.</p><div className="mt-3 max-h-72 space-y-2 overflow-y-auto pr-1">{courses.map((course) => { const order = courseIds.indexOf(course.id); return <div className="flex items-center gap-3 rounded-xl bg-white p-2" key={course.id}><input checked={order >= 0} onChange={() => toggleCourse(course.id)} type="checkbox" /><span className="min-w-0 flex-1 truncate text-sm font-semibold">{course.title}</span>{order >= 0 ? <span className="flex"><button className="rounded p-1 disabled:opacity-30" disabled={order === 0} onClick={() => moveCourse(course.id, -1)} type="button"><ChevronUp className="h-4 w-4" /></button><button className="rounded p-1 disabled:opacity-30" disabled={order === courseIds.length - 1} onClick={() => moveCourse(course.id, 1)} type="button"><ChevronDown className="h-4 w-4" /></button></span> : null}</div>; })}</div></div><div className="mt-5 flex justify-end gap-3"><button className="rounded-2xl border border-[#dfbfbd] px-4 py-3 text-sm font-bold text-[#730014]" onClick={() => setModal(null)} type="button">Hủy</button><button className="rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white disabled:opacity-60" disabled={saving} onClick={savePath} type="button"><Check className="mr-2 inline h-4 w-4" />{saving ? 'Đang lưu...' : 'Lưu lộ trình'}</button></div></Panel></div></div> : null}
    </div>
  );
}

function Notice({ children, tone }) { return <div className={`rounded-2xl border px-5 py-4 text-sm font-semibold ${tone === 'error' ? 'border-[#ba1a1a]/20 bg-[#ffdad6] text-[#93000a]' : 'border-emerald-200 bg-emerald-50 text-emerald-700'}`}>{children}</div>; }
