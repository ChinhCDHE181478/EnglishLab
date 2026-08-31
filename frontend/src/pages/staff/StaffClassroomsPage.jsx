import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  BookOpen, Building2, CalendarDays, CheckCircle2, ChevronDown, ChevronRight, ChevronUp, Edit3,
  GraduationCap, Plus, RefreshCw, Search, Trash2, Users, Video, X,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { ClassroomEmptyState, ClassroomErrorState, ClassroomLoadingState, StatusBadge } from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import VietnameseDateInput from '../../components/ui/VietnameseDateInput';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import ManagementToast from '../../components/ui/ManagementToast';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { validateClassroomOfferingForm } from '../../utils/classroomFormValidation';
import { formatClassroomDate, formatClassroomPrice, formatDeliveryMode } from '../../utils/classroomHelpers';

const statusOptions = [
  { label: 'Bản nháp', value: 'DRAFT' },
  { label: 'Sắp khai giảng', value: 'UPCOMING' },
  { label: 'Đang hoạt động', value: 'ACTIVE' },
  { label: 'Đã kết thúc', value: 'COMPLETED' },
  { label: 'Đã hủy', value: 'CANCELLED' },
  { label: 'Đã đóng', value: 'CLOSED' },
];
const deliveryModeOptions = [
  { label: 'Tại trung tâm', value: 'OFFLINE' },
  { label: 'Trực tuyến', value: 'VIRTUAL' },
];

const initialForm = {
  title: '', deliveryMode: 'OFFLINE', classroomStatus: 'DRAFT', originalStatus: 'DRAFT',
  instructorLedCourseId: '', instructorLedCourseTitle: '', instructorLedCourseCode: '',
  maxCapacity: '18', startDate: '', endDate: '', tuitionFeeVnd: '',
  primaryTeacherId: '', regularRoomId: '', offlineAddress: '', locationNote: '',
  scheduleItems: [],
};

export default function StaffClassroomsPage() {
  const navigate = useNavigate();
  const [classrooms, setClassrooms] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [courses, setCourses] = useState([]);
  const [courseStructure, setCourseStructure] = useState(null);
  const [form, setForm] = useState(initialForm);
  const [editingId, setEditingId] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [formError, setFormError] = useState('');
  const [success, setSuccess] = useState('');
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [courseFilter, setCourseFilter] = useState('ALL');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [classroomData, roomData, teacherData, courseData] = await Promise.all([
        classroomApi.getStaffClassrooms(), classroomApi.getStaffRooms(), classroomApi.getStaffTeachers(),
        classroomApi.getStaffPrograms(),
      ]);
      setClassrooms(classroomData);
      setRooms(roomData);
      setTeachers(teacherData);
      setCourses(courseData);
    } catch (err) {
      setClassrooms([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải danh sách lớp.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const roomOptions = useMemo(() => [
    { label: 'Chưa chọn phòng', value: '' },
    ...rooms.map((item) => ({ label: item.label || item.name, value: String(item.id) })),
  ], [rooms]);
  const teacherOptions = useMemo(() => [
    { label: 'Chưa phân công giáo viên', value: '' },
    ...teachers.map((item) => ({ label: item.label || item.fullName || item.name, value: String(item.id) })),
  ], [teachers]);
  const editableCourseOptions = useMemo(() => [
    { label: 'Chọn khóa học có giảng viên', value: '' },
    ...courses.map((item) => ({
      label: item.title,
      value: String(item.id),
      description: [item.code, item.entryLevel, item.targetScore ? `Đầu ra ${item.targetScore}` : null]
        .filter(Boolean).join(' · '),
    })),
  ], [courses]);
  const courseOptions = useMemo(() => {
    const courses = new Map();
    classrooms.forEach((item) => {
      if (item.instructorLedCourseId && item.instructorLedCourseTitle) {
        courses.set(String(item.instructorLedCourseId), item.instructorLedCourseTitle);
      }
    });
    return [{ label: 'Tất cả khóa học', value: 'ALL' }, ...Array.from(courses, ([value, label]) => ({ label, value }))];
  }, [classrooms]);

  const filtered = useMemo(() => {
    const search = keyword.trim().toLowerCase();
    return classrooms.filter((item) => {
      const matchesStatus = statusFilter === 'ALL' || item.classroomStatus === statusFilter;
      const matchesCourse = courseFilter === 'ALL' || String(item.instructorLedCourseId) === courseFilter;
      const matchesSearch = !search || [item.title, item.code, item.instructorLedCourseTitle,
        item.instructorLedCourseCode, item.primaryTeacherName, item.regularRoomName]
        .filter(Boolean).some((value) => String(value).toLowerCase().includes(search));
      return matchesStatus && matchesCourse && matchesSearch;
    });
  }, [classrooms, courseFilter, keyword, statusFilter]);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    filtered, 8, `${keyword}|${statusFilter}|${courseFilter}`,
  );
  const stats = useMemo(() => ({
    total: classrooms.length,
    active: classrooms.filter((item) => ['UPCOMING', 'ACTIVE'].includes(item.classroomStatus)).length,
    learners: classrooms.reduce((sum, item) => sum + Number(item.enrolledCount || 0), 0),
    courses: new Set(classrooms.map((item) => item.instructorLedCourseId).filter(Boolean)).size,
  }), [classrooms]);

  const resetEditor = () => {
    setEditorOpen(false);
    setEditingId(null);
    setForm(initialForm);
    setCourseStructure(null);
    setFormError('');
  };
  const closeEditor = () => { if (!working) resetEditor(); };
  const change = (field, value) => setForm((current) => {
    const next = { ...current, [field]: value };
    if (field === 'instructorLedCourseId') {
      const course = courses.find((item) => String(item.id) === String(value));
      next.instructorLedCourseTitle = course?.title || '';
      next.instructorLedCourseCode = course?.code || '';
    }
    return next;
  });

  const openEdit = async (item) => {
    setWorking(true);
    setFormError('');
    try {
      const detail = await classroomApi.getStaffClassroom(item.id);
      const structure = detail.instructorLedCourse
        || (detail.instructorLedCourseId
          ? await classroomApi.getStaffProgram(detail.instructorLedCourseId)
          : null);
      setEditingId(item.id);
      setForm(mapToForm(detail));
      setCourseStructure(structure);
      setEditorOpen(true);
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể tải thông tin lớp để chỉnh sửa.'));
    } finally {
      setWorking(false);
    }
  };

  const changeCourse = async (value) => {
    change('instructorLedCourseId', value);
    if (!value) {
      setCourseStructure(null);
      return;
    }
    setWorking(true);
    setFormError('');
    try {
      const detail = await classroomApi.getStaffProgram(value);
      setCourseStructure(detail);
    } catch (err) {
      setCourseStructure(null);
      setFormError(getClassroomErrorMessage(err, 'Không thể tải nội dung khóa học.'));
    } finally {
      setWorking(false);
    }
  };

  const save = async (event) => {
    event.preventDefault();
    const limitedEdit = form.originalStatus === 'ACTIVE';
    const capacity = Number(form.maxCapacity);
    const validation = limitedEdit
      ? (!form.title.trim()
        ? 'Tên lớp không được để trống.'
        : !Number.isInteger(capacity) || capacity < 1
          ? 'Sĩ số tối đa phải là số nguyên lớn hơn 0.'
          : !form.primaryTeacherId
            ? 'Lớp đang hoạt động phải có giáo viên chính.'
          : '')
      : validateClassroomOfferingForm({
        ...form, price: form.tuitionFeeVnd, roomId: form.regularRoomId,
      });
    if (validation) { setFormError(validation); return; }
    if (!limitedEdit) {
      const scheduleError = validateScheduleItems(form.scheduleItems);
      if (scheduleError) { setFormError(scheduleError); return; }
    }
    setWorking(true);
    setFormError('');
    try {
      const classroomPayload = {
        title: form.title.trim(),
        deliveryMode: form.deliveryMode,
        classroomStatus: form.classroomStatus,
        instructorLedCourseId: form.instructorLedCourseId ? Number(form.instructorLedCourseId) : null,
        capacity: Number(form.maxCapacity),
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        primaryTeacherId: form.primaryTeacherId ? Number(form.primaryTeacherId) : null,
        roomId: form.deliveryMode === 'OFFLINE' && form.regularRoomId ? Number(form.regularRoomId) : null,
        price: form.tuitionFeeVnd ? Number(form.tuitionFeeVnd) : 0,
        offlineAddress: form.offlineAddress || null,
        locationNote: form.locationNote.trim() || null,
      };
      if (limitedEdit) {
        await classroomApi.updateStaffClassroom(editingId, classroomPayload);
      } else {
        await classroomApi.updateStaffClassroomPrelaunchPlan(editingId, {
          classroom: classroomPayload,
          schedules: form.scheduleItems.map(toSchedulePayload),
        });
      }
      resetEditor();
      setSuccess('Đã cập nhật thông tin lớp.');
      await load();
    } catch (err) {
      setFormError(getClassroomErrorMessage(err, 'Không thể lưu lớp học.'));
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-5">
      <ManagementToast message={success} onClose={() => setSuccess('')} tone="success" title="Đã cập nhật lớp học" />
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Metric icon={GraduationCap} label="Tổng lớp" value={stats.total} />
        <Metric icon={CheckCircle2} label="Đang vận hành" value={stats.active} />
        <Metric icon={Users} label="Tổng học viên" value={stats.learners} />
        <Metric icon={BookOpen} label="Khóa học đang mở" value={stats.courses} />
      </section>

      <section className="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="relative min-w-[240px] flex-1">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input className={`${inputClass} h-11 pl-10`} onChange={(e) => setKeyword(e.target.value)} placeholder="Tìm theo lớp, mã lớp, khóa học hoặc giáo viên..." value={keyword} />
        </div>
        <div className="w-full sm:w-64"><BrandedSelect onChange={(e) => setCourseFilter(e.target.value)} options={courseOptions} value={courseFilter} /></div>
        <div className="w-full sm:w-48"><BrandedSelect onChange={(e) => setStatusFilter(e.target.value)} options={[{ label: 'Tất cả trạng thái', value: 'ALL' }, ...statusOptions]} value={statusFilter} /></div>
        <button aria-label="Làm mới danh sách lớp" className="inline-flex h-11 w-11 items-center justify-center rounded-xl border border-slate-200 text-[#730014]" disabled={loading} onClick={load} type="button">
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </section>

      {loading ? <ClassroomLoadingState message="Đang tải danh sách lớp..." /> : null}
      {!loading && error ? <ClassroomErrorState message={error} onRetry={load} /> : null}
      {!loading && !error && !filtered.length ? <ClassroomEmptyState description="Chưa có lớp phù hợp với điều kiện tìm kiếm." title="Không có lớp học" /> : null}

      {!loading && !error && filtered.length ? (
        <section className="overflow-hidden rounded-xl border border-[#dfbfbd]/40 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1380px] text-left text-sm">
              <thead className="border-b border-[#dfbfbd]/30 bg-[#fbf3f4]"><tr>
                {['Lớp học', 'Khóa học có giảng viên', 'Lịch học', 'Hình thức', 'Giáo viên chính', 'Sĩ số', 'Học phí lớp', 'Trạng thái', 'Thao tác'].map((label) => <th className="px-5 py-4 text-[11px] font-extrabold uppercase tracking-wider text-slate-500" key={label}>{label}</th>)}
              </tr></thead>
              <tbody className="divide-y divide-[#dfbfbd]/20">
                {pageItems.map((item) => <ClassroomRow item={item} key={item.id} navigate={navigate} onEdit={openEdit} working={working} />)}
              </tbody>
            </table>
          </div>
          <div className="border-t border-[#dfbfbd]/30 px-5 py-4"><Pagination onChange={setPage} page={page} pageSize={8} totalItems={totalItems} totalPages={totalPages} /></div>
        </section>
      ) : null}

      {editorOpen ? <Editor courseOptions={editableCourseOptions} courseStructure={courseStructure} form={form} formError={formError} onChange={change} onClose={closeEditor} onCourseChange={changeCourse} onSubmit={save} roomOptions={roomOptions} teacherOptions={teacherOptions} working={working} /> : null}
    </div>
  );
}

function ClassroomRow({ item, navigate, onEdit, working }) {
  const editable = ['DRAFT', 'UPCOMING', 'ACTIVE'].includes(item.classroomStatus);
  return <tr className="transition hover:bg-[#fffafb]">
    <td className="px-5 py-4"><p className="max-w-64 text-sm font-bold text-[#0b1c30]">{item.title}</p><p className="mt-1 text-xs text-slate-500">{item.code || 'Chưa có mã lớp'}</p></td>
    <td className="px-5 py-4"><p className="max-w-64 text-sm font-bold text-[#0b1c30]">{item.instructorLedCourseTitle || 'Chưa gắn khóa học'}</p><p className="mt-1 text-xs text-slate-500">{item.instructorLedCourseCode || ''}</p></td>
    <td className="px-5 py-4"><p className="max-w-60 font-semibold text-slate-700">{item.scheduleSummary || 'Chưa có lịch học'}</p><p className="mt-1 text-xs text-slate-500">{dateRange(item)}</p></td>
    <td className="px-5 py-4"><div className="flex items-center gap-2 font-semibold text-slate-700">{item.deliveryMode === 'VIRTUAL' ? <Video className="h-4 w-4 text-blue-600" /> : <Building2 className="h-4 w-4 text-emerald-600" />}{formatDeliveryMode(item.deliveryMode, item.deliveryModeLabel)}</div><p className="mt-1 max-w-48 text-xs text-slate-500">{item.deliveryMode === 'VIRTUAL' ? meetStatus(item.googleMeetStatus) : item.regularRoomName || item.roomName || 'Chưa chọn phòng thường học'}</p></td>
    <td className="px-5 py-4 font-semibold text-slate-700">{item.primaryTeacherName || 'Chưa phân công'}</td>
    <td className="px-5 py-4 font-bold text-[#0b1c30]">{item.enrolledCount ?? 0}/{item.capacity ?? item.maxCapacity ?? 0}</td>
    <td className="px-5 py-4 font-bold text-[#0b1c30]">{formatClassroomPrice(item.tuitionFeeVnd ?? item.price ?? 0)}</td>
    <td className="px-5 py-4"><StatusBadge status={item.classroomStatus} /></td>
    <td className="px-5 py-4"><div className="flex gap-2">{editable ? <button className={secondaryButton} disabled={working} onClick={() => onEdit(item)} type="button"><Edit3 className="h-3.5 w-3.5" />Sửa</button> : null}<button className={primaryButton} onClick={() => navigate(`/staff/classrooms/${item.id}`)} type="button">Quản lý<ChevronRight className="h-3.5 w-3.5" /></button></div></td>
  </tr>;
}

function Editor({ courseOptions, courseStructure, form, formError, onChange, onClose, onCourseChange, onSubmit, roomOptions, teacherOptions, working }) {
  const fullEdit = ['DRAFT', 'UPCOMING'].includes(form.originalStatus);
  const activeEdit = form.originalStatus === 'ACTIVE';
  const [scheduleCollapsed, setScheduleCollapsed] = useState(form.scheduleItems.length > 6);
  const lessons = useMemo(() => flattenCourseLessons(courseStructure), [courseStructure]);
  const scheduleRange = useMemo(() => {
    const dates = form.scheduleItems
      .map((item) => item.sessionDate)
      .filter(Boolean)
      .sort();
    if (!dates.length) return 'Chưa xác định thời gian';
    return `${formatClassroomDate(dates[0])} - ${formatClassroomDate(dates[dates.length - 1])}`;
  }, [form.scheduleItems]);
  const lessonOptions = useMemo(() => [
    { label: 'Không gắn bài học', value: 'SPECIAL' },
    ...lessons.map((item) => ({
      label: `Bài ${item.lessonNumber}: ${item.lessonTitle}`,
      value: String(item.lessonId),
      description: item.unitTitle,
    })),
  ], [lessons]);
  const overrideTeacherOptions = useMemo(() => [
    { label: 'Theo giáo viên chính', value: '' },
    ...teacherOptions.filter((item) => item.value),
  ], [teacherOptions]);
  const overrideRoomOptions = useMemo(() => [
    { label: 'Theo phòng thường học', value: '' },
    ...roomOptions.filter((item) => item.value),
  ], [roomOptions]);

  const updateSchedule = (index, patch) => {
    const next = [...form.scheduleItems];
    next[index] = { ...next[index], ...patch };
    onChange('scheduleItems', next);
  };
  const removeSchedule = (index) => onChange(
    'scheduleItems',
    form.scheduleItems.filter((_, itemIndex) => itemIndex !== index),
  );
  const addSchedule = () => {
    const previous = form.scheduleItems[form.scheduleItems.length - 1];
    const assignedCounts = form.scheduleItems.reduce((counts, item) => {
      const lessonId = String(item.courseLessonId || '');
      if (lessonId) counts.set(lessonId, (counts.get(lessonId) || 0) + 1);
      return counts;
    }, new Map());
    const occurrenceCounts = new Map();
    const nextLesson = lessons.find((lesson) => {
      const lessonId = String(lesson.lessonId);
      const occurrence = (occurrenceCounts.get(lessonId) || 0) + 1;
      occurrenceCounts.set(lessonId, occurrence);
      return occurrence > (assignedCounts.get(lessonId) || 0);
    });
    onChange('scheduleItems', [...form.scheduleItems, {
      id: null,
      sessionDate: previous?.sessionDate ? nextDate(previous.sessionDate) : form.startDate,
      startTime: previous?.startTime || '18:30',
      endTime: previous?.endTime || '20:30',
      teacherId: '', roomId: '', deliveryModeOverride: '',
      courseLessonId: nextLesson ? String(nextLesson.lessonId) : '',
      courseLessonTitle: nextLesson?.lessonTitle || '',
      courseUnitTitle: nextLesson?.unitTitle || '',
      sessionContent: nextLesson ? '' : 'Nội dung bổ sung', note: '',
    }]);
    setScheduleCollapsed(false);
  };

  return <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4 backdrop-blur-sm">
    <button aria-label="Đóng cửa sổ" className="absolute inset-0" disabled={working} onClick={onClose} type="button" />
    <form aria-modal="true" className="relative z-10 flex max-h-[94vh] w-full max-w-7xl flex-col overflow-hidden rounded-3xl bg-white shadow-2xl" onSubmit={onSubmit} role="dialog">
      <header className="flex items-start justify-between border-b border-slate-100 px-6 py-5"><div><p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">{fullEdit ? 'Lập kế hoạch lớp trước khai giảng' : 'Vận hành lớp học'}</p><h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{fullEdit ? 'Chỉnh sửa kế hoạch lớp' : 'Cập nhật lớp đang hoạt động'}</h2><p className="mt-1 text-xs text-[#8b706e]">{fullEdit ? 'Thiết lập lịch học, phân bổ bài học, giáo viên và phòng học.' : form.instructorLedCourseTitle}</p></div><button aria-label="Đóng" className="rounded-xl border border-slate-200 p-2 text-slate-500" disabled={working} onClick={onClose} type="button"><X className="h-5 w-5" /></button></header>
      <div className="min-h-0 flex-1 overflow-y-auto p-6">
        {formError ? <div className="mb-5 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-700" role="alert">{formError}</div> : null}
        {!fullEdit ? <div className="mb-5 grid gap-3 rounded-2xl border border-[#ead8d5] bg-[#fffafb] p-4 sm:grid-cols-3"><ReadOnly label="Khóa học có giảng viên" value={form.instructorLedCourseTitle || 'Chưa gắn'} /><ReadOnly label="Mã khóa học" value={form.instructorLedCourseCode || 'Chưa có'} /><ReadOnly label="Hình thức" value={formatDeliveryMode(form.deliveryMode)} /></div> : null}
        <section className="rounded-2xl border border-[#ead8d5] bg-[#fffafb] p-5">
          <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Thông tin lớp</h3>
          <div className="mt-4 grid gap-5 lg:grid-cols-2">
            <Field label="Tên lớp"><input className={inputClass} onChange={(e) => onChange('title', e.target.value)} required value={form.title} /></Field>
            <Field label="Sĩ số tối đa"><input className={inputClass} min="1" onChange={(e) => onChange('maxCapacity', e.target.value)} required type="number" value={form.maxCapacity} /></Field>
            {fullEdit ? <>
              <Field label="Khóa học có giảng viên" wide><BrandedSelect onChange={(e) => onCourseChange(e.target.value)} options={courseOptions} required searchable value={form.instructorLedCourseId} /></Field>
              <Field label="Hình thức"><BrandedSelect onChange={(e) => onChange('deliveryMode', e.target.value)} options={deliveryModeOptions} value={form.deliveryMode} /></Field>
              <Field label="Trạng thái"><BrandedSelect onChange={(e) => onChange('classroomStatus', e.target.value)} options={statusOptions.filter((item) => ['DRAFT', 'UPCOMING'].includes(item.value))} value={form.classroomStatus} /></Field>
              <Field label="Giáo viên chính"><BrandedSelect onChange={(e) => onChange('primaryTeacherId', e.target.value)} options={teacherOptions} searchable value={form.primaryTeacherId} /></Field>
              {form.deliveryMode === 'OFFLINE' ? <Field label="Phòng thường học"><BrandedSelect onChange={(e) => onChange('regularRoomId', e.target.value)} options={roomOptions} searchable value={form.regularRoomId} /></Field> : null}
              <Field label="Học phí lớp"><input className={inputClass} min="0" onChange={(e) => onChange('tuitionFeeVnd', e.target.value)} type="number" value={form.tuitionFeeVnd} /></Field>
              <Field label="Ngày bắt đầu"><VietnameseDateInput className={inputClass} onChange={(value) => onChange('startDate', value)} required value={form.startDate} /></Field>
              <Field label="Ngày kết thúc"><VietnameseDateInput className={inputClass} onChange={(value) => onChange('endDate', value)} value={form.endDate} /></Field>
              <Field label="Ghi chú địa điểm" wide><textarea className={inputClass} onChange={(e) => onChange('locationNote', e.target.value)} rows={2} value={form.locationNote} /></Field>
            </> : null}
          </div>
        </section>

        {/* {activeEdit ? <p className="mt-4 text-sm text-slate-500">Đổi giáo viên chính và điều chỉnh từng buổi tại trang quản lý lớp.</p> : null} */}

        {fullEdit ? <>
          <section className="mt-5 rounded-2xl border border-[#ead8d5] bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div><h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Lịch học và phân bổ bài học</h3><p className="mt-1 text-xs text-slate-500">{form.scheduleItems.length} buổi · {scheduleRange}</p></div>
              <div className="flex flex-wrap items-center gap-2">
                {form.scheduleItems.length ? <button aria-expanded={!scheduleCollapsed} className={secondaryButton} onClick={() => setScheduleCollapsed((current) => !current)} type="button">{scheduleCollapsed ? <ChevronDown className="h-4 w-4" /> : <ChevronUp className="h-4 w-4" />}{scheduleCollapsed ? 'Mở rộng' : 'Thu gọn'}</button> : null}
                <button className={secondaryButton} onClick={addSchedule} type="button"><Plus className="h-4 w-4" />Thêm buổi học</button>
              </div>
            </div>
            {scheduleCollapsed && form.scheduleItems.length ? <button className="mt-4 flex w-full items-center justify-between gap-4 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-left transition hover:bg-slate-100" onClick={() => setScheduleCollapsed(false)} type="button"><span className="text-sm font-semibold text-slate-600">{form.scheduleItems.length} buổi · {scheduleRange}</span><span className="shrink-0 text-xs font-extrabold text-[#730014]">Mở danh sách</span></button> : <div className="mt-4 space-y-4">
              {form.scheduleItems.map((item, index) => {
                const effectiveMode = item.deliveryModeOverride || form.deliveryMode;
                return <article className="rounded-2xl border border-slate-200 p-4" key={item.id || `new-${index}`}>
                  <div className="flex items-center justify-between gap-3"><div className="flex items-center gap-2"><span className="flex h-8 w-8 items-center justify-center rounded-lg bg-[#fff0f2] text-xs font-black text-[#730014]">{index + 1}</span><div><p className="text-sm font-extrabold text-[#0b1c30]">{item.courseLessonTitle || item.sessionContent || 'Buổi chưa gắn bài học'}</p><p className="text-xs text-slate-500">{item.courseUnitTitle || 'Nội dung ngoài bài học'}</p></div></div><button aria-label={`Xóa buổi ${index + 1}`} className="rounded-xl border border-rose-200 p-2 text-rose-600" onClick={() => removeSchedule(index)} type="button"><Trash2 className="h-4 w-4" /></button></div>
                  <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                    <Field label="Ngày học"><VietnameseDateInput className={inputClass} onChange={(value) => updateSchedule(index, { sessionDate: value })} required value={item.sessionDate} /></Field>
                    <Field label="Bắt đầu"><input className={inputClass} onChange={(e) => updateSchedule(index, { startTime: e.target.value })} required type="time" value={item.startTime} /></Field>
                    <Field label="Kết thúc"><input className={inputClass} onChange={(e) => updateSchedule(index, { endTime: e.target.value })} required type="time" value={item.endTime} /></Field>
                    <Field label="Bài học"><BrandedSelect onChange={(e) => {
                      const value = e.target.value;
                      const selected = lessons.find((lesson) => String(lesson.lessonId) === value);
                      updateSchedule(index, value === 'SPECIAL' ? { courseLessonId: '', courseLessonTitle: '', courseUnitTitle: '', sessionContent: item.sessionContent || 'Buổi học đặc biệt' } : { courseLessonId: value, courseLessonTitle: selected?.lessonTitle || '', courseUnitTitle: selected?.unitTitle || '', sessionContent: '' });
                    }} options={lessonOptions} searchable value={item.courseLessonId || 'SPECIAL'} /></Field>
                    <Field label="Giáo viên"><BrandedSelect onChange={(e) => updateSchedule(index, { teacherId: e.target.value })} options={overrideTeacherOptions} searchable value={item.teacherId} /></Field>
                    <Field label="Hình thức"><BrandedSelect onChange={(e) => updateSchedule(index, { deliveryModeOverride: e.target.value, roomId: e.target.value === 'VIRTUAL' ? '' : item.roomId })} options={[{ label: 'Theo lớp', value: '' }, ...deliveryModeOptions]} value={item.deliveryModeOverride} /></Field>
                    {effectiveMode === 'OFFLINE' ? <Field label="Phòng học"><BrandedSelect onChange={(e) => updateSchedule(index, { roomId: e.target.value })} options={overrideRoomOptions} searchable value={item.roomId} /></Field> : <ReadOnly label="Phòng học" value="Google Meet của lớp" />}
                    <Field label="Ghi chú"><input className={inputClass} onChange={(e) => updateSchedule(index, { note: e.target.value })} value={item.note} /></Field>
                    {!item.courseLessonId ? <Field label="Nội dung buổi học" wide><input className={inputClass} onChange={(e) => updateSchedule(index, { sessionContent: e.target.value })} required value={item.sessionContent} /></Field> : null}
                  </div>
                </article>;
              })}
              {!form.scheduleItems.length ? <div className="rounded-2xl border border-dashed border-slate-300 px-5 py-10 text-center text-sm text-slate-500"><CalendarDays className="mx-auto h-8 w-8 text-slate-300" /><p className="mt-2">Lớp chưa có lịch học thực tế.</p><button className={`${secondaryButton} mt-4`} onClick={addSchedule} type="button"><Plus className="h-4 w-4" />Thêm buổi học đầu tiên</button></div> : null}
            </div>}
          </section>
        </> : null}
      </div>
      <footer className="flex justify-end gap-3 border-t border-slate-100 px-6 py-4"><button className="min-h-11 rounded-xl border border-slate-200 px-5 text-sm font-bold text-[#584140]" disabled={working} onClick={onClose} type="button">Hủy</button><button className="min-h-11 rounded-xl bg-[#4b0009] px-5 text-sm font-extrabold text-white" disabled={working} type="submit">{working ? 'Đang lưu...' : 'Lưu thay đổi'}</button></footer>
    </form>
  </div>;
}

function mapToForm(item) {
  const sessions = [...(item.sessions || [])].sort((left, right) => (
    `${left.sessionDate || ''}T${left.startTime || ''}`.localeCompare(`${right.sessionDate || ''}T${right.startTime || ''}`)
  ));
  return {
    ...initialForm,
    title: item.title || '',
    deliveryMode: item.deliveryMode || 'OFFLINE',
    classroomStatus: item.classroomStatus || 'DRAFT',
    originalStatus: item.classroomStatus || 'DRAFT',
    instructorLedCourseId: String(item.instructorLedCourseId || ''),
    instructorLedCourseTitle: item.instructorLedCourseTitle || '',
    instructorLedCourseCode: item.instructorLedCourseCode || '',
    maxCapacity: String(item.capacity ?? item.maxCapacity ?? 18),
    startDate: item.startDate || sessions[0]?.sessionDate || '',
    endDate: item.endDate || sessions[sessions.length - 1]?.sessionDate || '',
    tuitionFeeVnd: String(item.tuitionFeeVnd ?? item.price ?? ''),
    primaryTeacherId: String(item.primaryTeacherId || ''),
    regularRoomId: String(item.regularRoomId ?? item.roomId ?? ''),
    offlineAddress: item.offlineAddress || '',
    locationNote: item.locationNote || '',
    scheduleItems: sessions.map((session) => ({
      id: session.id,
      sessionDate: session.sessionDate || '',
      startTime: String(session.startTime || '').slice(0, 5),
      endTime: String(session.endTime || '').slice(0, 5),
      teacherId: session.teacherId && session.teacherId !== item.primaryTeacherId ? String(session.teacherId) : '',
      roomId: session.roomId && session.roomId !== (item.regularRoomId ?? item.roomId) ? String(session.roomId) : '',
      deliveryModeOverride: session.deliveryModeOverride || '',
      courseLessonId: session.courseLessonId ? String(session.courseLessonId) : '',
      courseLessonTitle: session.courseLessonTitle || '',
      courseUnitTitle: session.courseUnitTitle || '',
      sessionContent: session.sessionContent || '',
      note: sanitizeScheduleNote(session.note),
    })),
  };
}

function sanitizeScheduleNote(value) {
  return /^Sinh từ đề xuất CP-/i.test(String(value || '').trim()) ? '' : (value || '');
}

function flattenCourseLessons(course) {
  return [...(course?.units || [])]
    .sort((left, right) => Number(left.displayOrder ?? left.sequenceNumber ?? 0) - Number(right.displayOrder ?? right.sequenceNumber ?? 0))
    .flatMap((unit) => [...(unit.lessons || [])]
      .sort((left, right) => Number(left.sessionNumber ?? left.displayOrder ?? 0) - Number(right.sessionNumber ?? right.displayOrder ?? 0))
      .flatMap((lesson) => Array.from({ length: Math.max(1, Number(lesson.plannedSessionCount || 1)) }, () => ({
        lessonId: lesson.id,
        lessonNumber: lesson.sessionNumber ?? lesson.displayOrder,
        lessonTitle: lesson.title,
        unitTitle: `Unit ${unit.displayOrder ?? unit.sequenceNumber}: ${unit.title}`,
      }))));
}

function nextDate(value) {
  if (!value) return '';
  const date = new Date(`${value}T00:00:00`);
  date.setDate(date.getDate() + 1);
  return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-');
}

function validateScheduleItems(items) {
  if (!items.length) return 'Kế hoạch lớp phải có ít nhất một buổi học.';
  for (const [index, item] of items.entries()) {
    if (!item.sessionDate || !item.startTime || !item.endTime) return `Buổi ${index + 1} chưa đủ ngày và giờ học.`;
    if (item.endTime <= item.startTime) return `Giờ kết thúc của buổi ${index + 1} phải sau giờ bắt đầu.`;
    if (!item.courseLessonId && !item.sessionContent.trim()) return `Buổi ${index + 1} cần chọn bài học hoặc nhập nội dung đặc biệt.`;
  }
  return '';
}

function toSchedulePayload(item) {
  return {
    id: item.id || null,
    sessionDate: item.sessionDate,
    startTime: item.startTime,
    endTime: item.endTime,
    teacherId: item.teacherId ? Number(item.teacherId) : null,
    status: 'SCHEDULED',
    deliveryModeOverride: item.deliveryModeOverride || null,
    roomId: item.roomId ? Number(item.roomId) : null,
    courseLessonId: item.courseLessonId ? Number(item.courseLessonId) : null,
    sessionContent: item.courseLessonId ? null : item.sessionContent.trim(),
    note: item.note.trim() || null,
  };
}

function dateRange(item) { return item.startDate || item.endDate ? `${formatClassroomDate(item.startDate)} - ${formatClassroomDate(item.endDate)}` : 'Chưa xác định thời gian'; }
function meetStatus(status) { return ({ ACTIVE: 'Google Meet sẵn sàng', PENDING: 'Đang tạo Google Meet', FAILED: 'Google Meet cần đồng bộ lại' })[status] || 'Chưa có phòng Google Meet'; }
function ReadOnly({ label, value }) { return <div><p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">{label}</p><p className="mt-1 text-sm font-bold text-[#0b1c30]">{value}</p></div>; }
function Field({ children, label, wide = false }) { return <label className={`block space-y-2 ${wide ? 'lg:col-span-2' : ''}`}><span className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">{label}</span>{children}</label>; }
function Metric({ icon: Icon, label, value }) { return <article className="rounded-xl border border-[#dfbfbd]/35 bg-white p-4 shadow-sm"><div className="flex items-center justify-between"><div><p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">{label}</p><p className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{value}</p></div><span className="rounded-xl bg-[#fff1f3] p-2.5 text-[#730014]"><Icon className="h-5 w-5" /></span></div></article>; }

const inputClass = 'w-full rounded-xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-3 py-2.5 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white';
const secondaryButton = 'inline-flex min-h-11 items-center gap-1.5 rounded-xl border border-[#dfbfbd] px-3 text-xs font-bold text-[#730014] disabled:opacity-50';
const primaryButton = 'inline-flex min-h-11 items-center gap-1.5 rounded-xl bg-[#4b0009] px-3 text-xs font-bold text-white';
