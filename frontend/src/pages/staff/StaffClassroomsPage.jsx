import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CalendarDays, CheckCircle2, ChevronRight, Edit3, GraduationCap, Plus, RefreshCw, Search, Users, X } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { ClassroomEmptyState, ClassroomErrorState, ClassroomLoadingState } from '../../components/classroom/ClassroomUi';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import BrandedSelect from '../../components/ui/BrandedSelect';
import VietnameseDateInput from '../../components/ui/VietnameseDateInput';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { validateClassroomOfferingForm } from '../../utils/classroomFormValidation';
import {
  formatClassroomDate,
  formatClassroomPrice,
  formatDeliveryMode,
  formatOfferingStatus,
} from '../../utils/classroomHelpers';

const deliveryModeOptions = [
  { label: 'Tại trung tâm', value: 'OFFLINE' },
  { label: 'Virtual', value: 'VIRTUAL' },
];

const statusOptions = [
  { label: 'Bản nháp', value: 'DRAFT' },
  { label: 'Sắp khai giảng', value: 'UPCOMING' },
  { label: 'Đang hoạt động', value: 'ACTIVE' },
  { label: 'Đã kết thúc', value: 'COMPLETED' },
];

const levelOptions = [
  { label: 'IELTS Foundation', value: 'IELTS Foundation' },
  { label: 'IELTS 4.0 - 5.0', value: 'IELTS 4.0 - 5.0' },
  { label: 'IELTS 5.0 - 6.0', value: 'IELTS 5.0 - 6.0' },
  { label: 'IELTS 6.0+', value: 'IELTS 6.0+' },
];

const initialClassroomForm = {
  title: '',
  deliveryMode: 'OFFLINE',
  classroomStatus: 'DRAFT',
  packageStatus: 'DRAFT',
  trainingProgramId: '',
  entryLevel: 'IELTS Foundation',
  targetScore: '',
  targetOutcome: '',
  maxCapacity: '18',
  startDate: '',
  endDate: '',
  price: '',
  salePrice: '',
  duration: '',
  studyMode: '',
  primaryTeacherId: '',
  defaultRoomId: '',
  offlineAddress: '',
  locationNote: '',
  defaultLarkMeetingUrl: '',
  shortDescription: '',
  description: '',
  syllabusSummary: '',
};

export default function StaffClassroomsPage() {
  const navigate = useNavigate();
  const [classrooms, setClassrooms] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [trainingPrograms, setTrainingPrograms] = useState([]);
  const [classroomForm, setClassroomForm] = useState(initialClassroomForm);
  const [editingId, setEditingId] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [programFilter, setProgramFilter] = useState('ALL');

  const loadClassrooms = async () => {
    setLoading(true);
    setError('');
    try {
      const [data, teacherData, roomData, programData] = await Promise.all([
        classroomApi.getStaffClassrooms(),
        classroomApi.getStaffTeachers(),
        classroomApi.getStaffRooms(),
        classroomApi.getStaffPrograms(),
      ]);
      setClassrooms(data);
      setTeachers(teacherData);
      setRooms(roomData);
      setTrainingPrograms(programData);
    } catch (err) {
      setClassrooms([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải danh sách lớp.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadClassrooms();
  }, []);

  const teacherOptions = useMemo(() => [
    { label: 'Chưa chọn giáo viên', value: '' },
    ...teachers.map((item) => ({ label: item.label, value: String(item.id) })),
  ], [teachers]);

  const roomOptions = useMemo(() => [
    { label: 'Chưa chọn phòng', value: '' },
    ...rooms.map((item) => ({ label: item.label, value: String(item.id) })),
  ], [rooms]);

  const trainingProgramOptions = useMemo(() => [
    { label: 'Chọn chương trình đã xuất bản', value: '' },
    ...trainingPrograms
      .filter((program) => program.deliveryMode === classroomForm.deliveryMode)
      .map((program) => ({
        label: program.title,
        value: String(program.id),
        description: [program.code, program.examCategory, program.entryLevel, program.targetScore ? `Đầu ra ${program.targetScore}` : null].filter(Boolean).join(' · '),
      })),
  ], [classroomForm.deliveryMode, trainingPrograms]);

  const programFilterOptions = useMemo(() => {
    const seen = new Set();
    const opts = [{ label: 'Tất cả chương trình', value: 'ALL' }];
    for (const item of classrooms) {
      const id = item.trainingProgramId;
      const title = item.trainingProgramTitle || item.curriculumProgramTitle;
      if (id && title && !seen.has(id)) {
        seen.add(id);
        opts.push({ label: title, value: String(id) });
      }
    }
    return opts;
  }, [classrooms]);

  const filteredClassrooms = useMemo(() => {
    const search = keyword.trim().toLowerCase();
    return classrooms.filter((item) => {
      const matchesStatus = statusFilter === 'ALL' || item.classroomStatus === statusFilter;
      const matchesProgram = programFilter === 'ALL' || String(item.trainingProgramId) === programFilter;
      const matchesKeyword = !search || [
        item.title,
        item.slug,
        item.trainingProgramTitle,
        item.curriculumProgramTitle,
        item.primaryTeacherName,
        item.deliveryModeLabel,
      ].filter(Boolean).some((value) => String(value).toLowerCase().includes(search));
      return matchesStatus && matchesProgram && matchesKeyword;
    });
  }, [classrooms, keyword, statusFilter, programFilter]);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    filteredClassrooms,
    8,
    `${keyword}|${statusFilter}|${programFilter}`,
  );

  const stats = useMemo(() => ({
    total: classrooms.length,
    active: classrooms.filter((item) => ['UPCOMING', 'ACTIVE'].includes(item.classroomStatus)).length,
    learners: classrooms.reduce((sum, item) => sum + Number(item.enrolledCount || 0), 0),
    programs: new Set(classrooms.map((item) => item.trainingProgramId).filter(Boolean)).size,
  }), [classrooms]);

  const updateClassroomForm = (field, value) => {
    setClassroomForm((current) => {
      const next = { ...current, [field]: value };
      if (field === 'deliveryMode') {
        next.studyMode = value === 'VIRTUAL' ? 'Virtual' : 'Offline tại trung tâm';
        next.trainingProgramId = '';
        next.defaultRoomId = '';
      }
      if (field === 'trainingProgramId') {
        const program = trainingPrograms.find((item) => String(item.id) === String(value));
        if (program) {
          next.entryLevel = program.entryLevel || next.entryLevel;
          next.targetScore = program.targetScore || '';
          next.targetOutcome = program.targetOutcome || '';
          next.price = program.price == null ? '' : String(program.price);
          next.salePrice = program.salePrice == null ? '' : String(program.salePrice);
          next.duration = program.duration || '';
          next.studyMode = program.studyMode || next.studyMode;
        }
      }
      return next;
    });
  };

  const openEdit = async (item) => {
    setWorking(true);
    setMessage('');
    try {
      const detail = await classroomApi.getStaffClassroom(item.id);
      setEditingId(item.id);
      setClassroomForm(mapClassroomToForm(detail));
      setEditorOpen(true);
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể tải thông tin lớp để chỉnh sửa.'));
    } finally {
      setWorking(false);
    }
  };

  const buildPayload = () => ({
    ...classroomForm,
    maxCapacity: Number(classroomForm.maxCapacity || 0),
    trainingProgramId: classroomForm.trainingProgramId ? Number(classroomForm.trainingProgramId) : null,
    curriculumProgramId: null,
    primaryTeacherId: classroomForm.primaryTeacherId ? Number(classroomForm.primaryTeacherId) : null,
    defaultRoomId: classroomForm.deliveryMode === 'OFFLINE' && classroomForm.defaultRoomId ? Number(classroomForm.defaultRoomId) : null,
    price: classroomForm.price ? Number(classroomForm.price) : 0,
    salePrice: classroomForm.salePrice ? Number(classroomForm.salePrice) : null,
    offlineAddress: classroomForm.deliveryMode === 'OFFLINE' ? classroomForm.offlineAddress : '',
    defaultLarkMeetingUrl: '',
  });

  const saveClassroom = async (event) => {
    event.preventDefault();
    if (!editingId) {
      setMessage('Chỉ có thể tạo lớp từ đề xuất đã được phê duyệt.');
      return;
    }
    if (!classroomForm.trainingProgramId) {
      setMessage('Vui lòng chọn chương trình đào tạo đã xuất bản trước khi mở lớp.');
      return;
    }
    const validationMessage = validateClassroomOfferingForm(classroomForm);
    if (validationMessage) {
      setMessage(validationMessage);
      return;
    }
    setWorking(true);
    setMessage('');
    try {
      await classroomApi.updateStaffClassroom(editingId, buildPayload());
      setEditorOpen(false);
      setEditingId(null);
      setClassroomForm(initialClassroomForm);
      setMessage('Đã cập nhật thông tin vận hành lớp.');
      await loadClassrooms();
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể lưu lớp học.'));
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-5">
      {message && !editorOpen ? <div className="rounded-xl border border-[#dfbfbd]/50 bg-[#fffafb] px-4 py-3 text-sm font-bold text-[#730014]" role="alert">{message}</div> : null}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard icon={GraduationCap} label="Tổng lớp" value={stats.total} />
        <MetricCard icon={CheckCircle2} label="Đang vận hành" value={stats.active} />
        <MetricCard icon={Users} label="Tổng học viên" value={stats.learners} />
        <MetricCard icon={CalendarDays} label="Chương trình đang mở" value={stats.programs} />
      </section>

      <section className="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="relative min-w-[240px] flex-1">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input className={`${inputClass} h-11 pl-10`} onChange={(event) => setKeyword(event.target.value)} placeholder="Tìm lớp, chương trình hoặc giáo viên..." value={keyword} />
        </div>
        <div className="w-full sm:w-56">
          <BrandedSelect onChange={(event) => setProgramFilter(event.target.value)} options={programFilterOptions} value={programFilter} />
        </div>
        <div className="w-full sm:w-48">
          <BrandedSelect onChange={(event) => setStatusFilter(event.target.value)} options={[{ label: 'Tất cả trạng thái', value: 'ALL' }, ...statusOptions]} value={statusFilter} />
        </div>
        <div className="flex items-center gap-2">
          <button aria-label="Làm mới danh sách lớp" className="inline-flex h-11 w-11 items-center justify-center rounded-xl border border-slate-200 text-[#730014] hover:bg-slate-50 transition" onClick={loadClassrooms} type="button">
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button className="inline-flex h-11 items-center gap-2 rounded-xl bg-[#4b0009] px-5 text-xs font-extrabold text-white hover:bg-[#730014] transition active:scale-95 whitespace-nowrap shadow-sm" onClick={() => navigate('/staff/classroom-proposals')} type="button">
            <Plus className="h-4 w-4" />
            Đề xuất lớp mới
          </button>
        </div>
      </section>

      {loading ? <ClassroomLoadingState message="Đang tải danh sách lớp..." /> : null}
      {!loading && error ? <ClassroomErrorState message={error} onRetry={loadClassrooms} /> : null}
      {!loading && !error && !filteredClassrooms.length ? <ClassroomEmptyState description="Chưa có lớp phù hợp với điều kiện tìm kiếm." title="Không có lớp học" /> : null}

      {!loading && !error && filteredClassrooms.length ? (
        <section className="overflow-hidden rounded-xl border border-[#dfbfbd]/40 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1080px] text-left text-sm">
              <thead className="border-b border-[#dfbfbd]/30 bg-[#fbf3f4] text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">
                <tr><th className="px-5 py-4">Tên lớp</th><th className="px-5 py-4">Hình thức</th><th className="px-5 py-4 text-center">Sĩ số</th><th className="px-5 py-4">Chương trình</th><th className="px-5 py-4">Khai giảng</th><th className="px-5 py-4">Học phí</th><th className="px-5 py-4">Trạng thái</th><th className="px-5 py-4 text-right">Thao tác</th></tr>
              </thead>
              <tbody className="divide-y divide-[#dfbfbd]/20">
                {pageItems.map((item) => (
                  <tr className="transition hover:bg-[#fffafb]" key={item.id}>
                    <td className="px-5 py-4"><p className="max-w-64 font-extrabold text-[#2b2828]">{item.title}</p><p className="mt-1 text-xs text-[#8b706e]">{item.slug || item.primaryTeacherName || 'Chưa phân công giáo viên'}</p></td>
                    <td className="px-5 py-4">{formatDeliveryMode(item.deliveryMode, item.deliveryModeLabel)}</td>
                    <td className="px-5 py-4 text-center font-bold">{item.enrolledCount ?? 0}/{item.maxCapacity ?? '-'}</td>
                    <td className="px-5 py-4 text-[#584140]">{item.trainingProgramTitle || item.curriculumProgramTitle || 'Chưa gắn'}</td>
                    <td className="px-5 py-4">{formatClassroomDate(item.startDate)}</td>
                    <td className="px-5 py-4 font-bold">{formatClassroomPrice(item.salePrice ?? item.price ?? 0)}</td>
                    <td className="px-5 py-4"><span className="rounded-lg bg-[#fff0f1] px-3 py-1.5 text-xs font-extrabold text-[#730014]">{formatOfferingStatus(item.classroomStatus)}</span></td>
                    <td className="px-5 py-4"><div className="flex justify-end gap-2"><button className="inline-flex items-center gap-1.5 rounded-lg border border-[#dfbfbd] px-3 py-2 text-xs font-bold text-[#730014]" disabled={working} onClick={() => openEdit(item)} type="button"><Edit3 className="h-3.5 w-3.5" />Sửa</button><button className="inline-flex items-center gap-1.5 rounded-lg bg-[#4b0009] px-3 py-2 text-xs font-bold text-white" onClick={() => navigate(`/staff/classrooms/${item.id}`)} type="button">Quản lý<ChevronRight className="h-3.5 w-3.5" /></button></div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="border-t border-[#dfbfbd]/30 px-5 py-4">
            <Pagination onChange={setPage} page={page} pageSize={8} totalItems={totalItems} totalPages={totalPages} />
          </div>
        </section>
      ) : null}

      {editorOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4 backdrop-blur-sm">
          <button aria-label="Đóng cửa sổ" className="absolute inset-0" disabled={working} onClick={() => setEditorOpen(false)} type="button" />
          <form className="relative z-10 flex max-h-[92vh] w-full max-w-5xl flex-col overflow-hidden rounded-3xl bg-white shadow-2xl" onSubmit={saveClassroom}>
            <div className="flex items-center justify-between border-b border-gray-100 px-6 py-5"><div><h3 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">Chỉnh sửa thông tin vận hành</h3><p className="mt-1 text-xs text-[#8b706e]">Cập nhật các thông tin phục vụ vận hành lớp.</p></div><button aria-label="Đóng" className="rounded-xl border border-gray-200 p-2 text-[#584140] disabled:opacity-50" disabled={working} onClick={() => setEditorOpen(false)} type="button"><X className="h-5 w-5" /></button></div>
            <div className="min-h-0 flex-1 overflow-y-auto p-6">
              {message ? <div className="mb-5 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-700" role="alert">{message}</div> : null}
              <ClassroomFormFields form={classroomForm} onChange={updateClassroomForm} roomOptions={roomOptions} teacherOptions={teacherOptions} trainingProgramOptions={trainingProgramOptions} />
            </div>
            <div className="flex justify-end gap-3 border-t border-gray-100 px-6 py-4"><button className="rounded-xl border border-gray-200 px-5 py-2.5 text-sm font-bold text-[#584140] disabled:opacity-50" disabled={working} onClick={() => setEditorOpen(false)} type="button">Hủy</button><button className="rounded-xl bg-[#4b0009] px-5 py-2.5 text-sm font-extrabold text-white disabled:opacity-60" disabled={working} type="submit">{working ? 'Đang lưu...' : 'Lưu thay đổi'}</button></div>
          </form>
        </div>
      ) : null}
    </div>
  );
}

function ClassroomFormFields({ form, onChange, roomOptions, teacherOptions, trainingProgramOptions }) {
  return (
    <div className="grid gap-5 lg:grid-cols-2">
      <Field label="Tên lớp"><input className={inputClass} onChange={(event) => onChange('title', event.target.value)} required value={form.title} /></Field>
      <Field label="Khóa học theo lịch"><BrandedSelect onChange={(event) => onChange('trainingProgramId', event.target.value)} options={trainingProgramOptions} required value={form.trainingProgramId} /></Field>
      <Field label="Hình thức"><BrandedSelect onChange={(event) => onChange('deliveryMode', event.target.value)} options={deliveryModeOptions} value={form.deliveryMode} /></Field>
      <Field label="Trạng thái"><BrandedSelect onChange={(event) => onChange('classroomStatus', event.target.value)} options={statusOptions} value={form.classroomStatus} /></Field>
      <Field label="Level đầu vào"><BrandedSelect onChange={(event) => onChange('entryLevel', event.target.value)} options={levelOptions} value={form.entryLevel} /></Field>
      <Field label="Sĩ số tối đa"><input className={inputClass} min="1" onChange={(event) => onChange('maxCapacity', event.target.value)} required type="number" value={form.maxCapacity} /></Field>
      <Field label="Ngày khai giảng"><VietnameseDateInput className={inputClass} onChange={(value) => onChange('startDate', value)} required value={form.startDate} /></Field>
      <Field label="Ngày kết thúc dự kiến"><VietnameseDateInput className={inputClass} onChange={(value) => onChange('endDate', value)} value={form.endDate} /></Field>
      <Field label="Giáo viên chính"><BrandedSelect onChange={(event) => onChange('primaryTeacherId', event.target.value)} options={teacherOptions} value={form.primaryTeacherId} /></Field>
      {form.deliveryMode === 'OFFLINE' ? <Field label="Phòng học"><BrandedSelect onChange={(event) => onChange('defaultRoomId', event.target.value)} options={roomOptions} value={form.defaultRoomId} /></Field> : <div />}
      <Field label="Học phí"><input className={inputClass} min="0" onChange={(event) => onChange('price', event.target.value)} type="number" value={form.price} /></Field>
      <Field label="Giá ưu đãi"><input className={inputClass} min="0" onChange={(event) => onChange('salePrice', event.target.value)} type="number" value={form.salePrice} /></Field>
      {form.deliveryMode === 'OFFLINE' ? <Field label="Địa điểm học"><input className={inputClass} onChange={(event) => onChange('offlineAddress', event.target.value)} value={form.offlineAddress} /></Field> : null}
      <Field label="Mô tả ngắn" wide>
        <RichTextEditor
          helperText=""
          onChange={(html) => onChange('shortDescription', html)}
          placeholder="Mô tả ngắn về lớp học..."
          size="compact"
          value={form.shortDescription}
        />
      </Field>
    </div>
  );
}

function mapClassroomToForm(item) {
  return {
    ...initialClassroomForm,
    title: item.title || '',
    deliveryMode: item.deliveryMode || 'OFFLINE',
    classroomStatus: item.classroomStatus || 'DRAFT',
    packageStatus: item.packageStatus || 'DRAFT',
    trainingProgramId: item.trainingProgramId ? String(item.trainingProgramId) : '',
    entryLevel: item.entryLevel || 'IELTS Foundation',
    targetScore: item.targetScore || '',
    targetOutcome: item.targetOutcome || '',
    maxCapacity: String(item.maxCapacity ?? 18),
    startDate: item.startDate || '',
    endDate: item.endDate || '',
    price: item.price == null ? '' : String(item.price),
    salePrice: item.salePrice == null ? '' : String(item.salePrice),
    duration: item.duration || '',
    studyMode: item.studyMode || '',
    primaryTeacherId: item.primaryTeacherId ? String(item.primaryTeacherId) : '',
    defaultRoomId: item.roomId ? String(item.roomId) : '',
    offlineAddress: item.offlineAddress || '',
    locationNote: item.locationNote || '',
    shortDescription: item.shortDescription || '',
    description: item.description || '',
    syllabusSummary: item.syllabusSummary || '',
  };
}

function Field({ children, label, wide = false }) {
  return <label className={`block space-y-2 ${wide ? 'lg:col-span-2' : ''}`}><span className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">{label}</span>{children}</label>;
}

function MetricCard({ icon: Icon, label, value }) {
  return <article className="rounded-xl border border-[#dfbfbd]/35 bg-white p-4 shadow-sm"><div className="flex items-center justify-between"><div><p className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">{label}</p><p className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{value}</p></div><span className="rounded-xl bg-[#fff1f3] p-2.5 text-[#730014]"><Icon className="h-5 w-5" /></span></div></article>;
}

const inputClass = 'w-full rounded-xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-3 py-2.5 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white';
