import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CalendarDays, ChevronRight, Megaphone, Plus, Save } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { ClassroomEmptyState, ClassroomErrorState, ClassroomLoadingState } from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
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
  curriculumProgramId: '',
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

export default function ManagerClassroomsPage() {
  const navigate = useNavigate();
  const [classrooms, setClassrooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [actionStatus, setActionStatus] = useState('success');
  const [teachers, setTeachers] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [curriculumPrograms, setCurriculumPrograms] = useState([]);
  const [classroomForm, setClassroomForm] = useState(initialClassroomForm);
  const [creatingClassroom, setCreatingClassroom] = useState(false);

  const publishedCount = useMemo(
    () => classrooms.filter((item) => ['UPCOMING', 'ACTIVE'].includes(item.classroomStatus)).length,
    [classrooms],
  );

  const teacherOptions = useMemo(
    () => [
      { label: 'Chưa chọn giáo viên', value: '' },
      ...teachers.map((item) => {
        const [name, ...rest] = String(item.label || '').split(' - ');
        return {
          label: name || item.label,
          description: rest.join(' - '),
          value: String(item.id),
        };
      }),
    ],
    [teachers],
  );

  const roomOptions = useMemo(
    () => [{ label: 'Chưa chọn phòng', value: '' }, ...rooms.map((item) => ({ label: item.label, value: String(item.id) }))],
    [rooms],
  );

  const curriculumOptions = useMemo(
    () => [
      { label: 'Chưa chọn giáo trình', value: '' },
      ...curriculumPrograms
        .filter((program) => program.deliveryMode === classroomForm.deliveryMode)
        .filter((program) => String(program.status || '').toUpperCase() !== 'ARCHIVED')
        .map((program) => ({
          label: program.title,
          value: String(program.id),
          description: [
            program.code,
            program.examCategory,
            program.targetBand ? `Band ${program.targetBand}` : null,
            program.targetScore ? `Target ${program.targetScore}` : null,
            program.entryLevel,
          ].filter(Boolean).join(' · '),
        })),
    ],
    [classroomForm.deliveryMode, curriculumPrograms],
  );

  const loadClassrooms = async () => {
    setLoading(true);
    setError('');
    try {
      const [data, teacherData, roomData, curriculumData] = await Promise.all([
        classroomApi.getManagerClassrooms(),
        classroomApi.getTrainingManagerTeachers(),
        classroomApi.getTrainingManagerRooms(),
        classroomApi.getTrainingManagerCurriculumPrograms(),
      ]);
      setClassrooms(data);
      setTeachers(teacherData);
      setRooms(roomData);
      setCurriculumPrograms(curriculumData);
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

  const showMessage = (message, status = 'success') => {
    setActionStatus(status);
    setActionMessage(message);
  };

  const updateClassroomForm = (field, value) => {
    setClassroomForm((current) => {
      const next = { ...current, [field]: value };
      if (field === 'deliveryMode') {
        next.studyMode = value === 'VIRTUAL' ? 'Virtual' : 'Offline tại trung tâm';
        next.curriculumProgramId = '';
      }
      return next;
    });
  };

  const buildClassroomPayload = () => ({
    ...classroomForm,
    maxCapacity: Number(classroomForm.maxCapacity || 0),
    curriculumProgramId: classroomForm.curriculumProgramId ? Number(classroomForm.curriculumProgramId) : null,
    primaryTeacherId: classroomForm.primaryTeacherId ? Number(classroomForm.primaryTeacherId) : null,
    defaultRoomId: classroomForm.defaultRoomId ? Number(classroomForm.defaultRoomId) : null,
    price: classroomForm.price ? Number(classroomForm.price) : 0,
    salePrice: classroomForm.salePrice ? Number(classroomForm.salePrice) : null,
    offlineAddress: classroomForm.deliveryMode === 'OFFLINE' ? classroomForm.offlineAddress : '',
    defaultLarkMeetingUrl: '',
  });

  const handleCreateClassroom = async (event) => {
    event.preventDefault();
    setCreatingClassroom(true);
    setActionMessage('');
    try {
      const created = await classroomApi.createManagerClassroom(buildClassroomPayload());
      showMessage('Đã tạo lớp mới. Chuyển sang trang quản lý lớp để thêm buổi học và xử lý đăng ký.');
      setClassroomForm(initialClassroomForm);
      await loadClassrooms();
      navigate(`/training-manager/classrooms/${created.id}?tab=schedule`);
    } catch (err) {
      showMessage(getClassroomErrorMessage(err, 'Không thể tạo lớp mới.'), 'error');
    } finally {
      setCreatingClassroom(false);
    }
  };

  return (
    <div className="space-y-5">
      <section className="grid gap-3 md:grid-cols-2">
        <MetricCard icon={CalendarDays} label="Tổng lớp đang quản lý" value={classrooms.length} />
        <MetricCard icon={Megaphone} label="Đã lên lịch khai giảng" value={publishedCount} />
      </section>

      {actionMessage ? (
        <div className={`rounded-2xl border px-4 py-3 text-sm font-semibold ${
          actionStatus === 'success'
            ? 'border-emerald-100 bg-emerald-50 text-emerald-800'
            : 'border-rose-100 bg-rose-50 text-rose-800'
        }`}
        >
          {actionMessage}
        </div>
      ) : null}

      <section className="grid gap-5 xl:grid-cols-[380px_1fr]">
        <form className="space-y-4 rounded-xl border border-[#dfbfbd]/45 bg-white p-4 shadow-sm" onSubmit={handleCreateClassroom}>
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#fff1f3] text-[#730014]">
              <Plus className="h-4 w-4" />
            </div>
            <div>
              <h2 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Tạo lớp khai giảng</h2>
              <p className="text-xs text-[#8b706e]">Dùng cho cả lớp offline và virtual.</p>
            </div>
          </div>

          <Field label="Tên lớp">
            <input
              className={inputClass}
              onChange={(event) => updateClassroomForm('title', event.target.value)}
              placeholder="IELTS Foundation - Tối 2/4/6"
              required
              value={classroomForm.title}
            />
          </Field>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Hình thức">
              <BrandedSelect
                onChange={(event) => updateClassroomForm('deliveryMode', event.target.value)}
                options={deliveryModeOptions}
                value={classroomForm.deliveryMode}
              />
            </Field>
            <Field label="Trạng thái ban đầu">
              <BrandedSelect
                onChange={(event) => updateClassroomForm('classroomStatus', event.target.value)}
                options={statusOptions}
                value={classroomForm.classroomStatus}
              />
            </Field>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Level đầu vào">
              <BrandedSelect
                onChange={(event) => updateClassroomForm('entryLevel', event.target.value)}
                options={levelOptions}
                value={classroomForm.entryLevel}
              />
            </Field>
            <Field label="Sĩ số tối đa">
              <input
                className={inputClass}
                min="1"
                onChange={(event) => updateClassroomForm('maxCapacity', event.target.value)}
                required
                type="number"
                value={classroomForm.maxCapacity}
              />
            </Field>
          </div>

          <Field label="Giáo trình">
            <BrandedSelect
              menuClassName="w-[min(520px,calc(100vw-2rem))]"
              onChange={(event) => updateClassroomForm('curriculumProgramId', event.target.value)}
              options={curriculumOptions}
              value={classroomForm.curriculumProgramId}
              placeholder="Chọn giáo trình theo target/band"
            />
          </Field>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Ngày khai giảng">
              <input
                className={inputClass}
                onChange={(event) => updateClassroomForm('startDate', event.target.value)}
                required
                type="date"
                value={classroomForm.startDate}
              />
            </Field>
            <Field label="Ngày kết thúc dự kiến">
              <input
                className={inputClass}
                onChange={(event) => updateClassroomForm('endDate', event.target.value)}
                type="date"
                value={classroomForm.endDate}
              />
            </Field>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Học phí">
              <input
                className={inputClass}
                min="0"
                onChange={(event) => updateClassroomForm('price', event.target.value)}
                placeholder="6500000"
                type="number"
                value={classroomForm.price}
              />
            </Field>
            <Field label="Ưu đãi">
              <input
                className={inputClass}
                min="0"
                onChange={(event) => updateClassroomForm('salePrice', event.target.value)}
                placeholder="5900000"
                type="number"
                value={classroomForm.salePrice}
              />
            </Field>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Giáo viên chính">
              <BrandedSelect
                onChange={(event) => updateClassroomForm('primaryTeacherId', event.target.value)}
                options={teacherOptions}
                value={classroomForm.primaryTeacherId}
              />
            </Field>
            {classroomForm.deliveryMode === 'OFFLINE' ? (
              <Field label="Phòng học">
                <BrandedSelect
                  menuClassName="w-[min(420px,calc(100vw-2rem))]"
                  onChange={(event) => updateClassroomForm('defaultRoomId', event.target.value)}
                  options={roomOptions}
                  value={classroomForm.defaultRoomId}
                />
              </Field>
            ) : (
              <InfoNote
                label="Link virtual"
                text="Hệ thống sẽ tự tạo hoặc đồng bộ link Lark khi bạn thêm buổi học virtual."
              />
            )}
          </div>

          {classroomForm.deliveryMode === 'OFFLINE' ? (
            <Field label="Địa điểm học">
              <input
                className={inputClass}
                onChange={(event) => updateClassroomForm('offlineAddress', event.target.value)}
                placeholder="Cơ sở Nguyễn Trãi, phòng 301"
                value={classroomForm.offlineAddress}
              />
            </Field>
          ) : null}

          <Field label="Mô tả ngắn">
            <textarea
              className={`${inputClass} min-h-[84px]`}
              onChange={(event) => updateClassroomForm('shortDescription', event.target.value)}
              placeholder="Lớp dành cho học viên mới hoàn thành placement..."
              value={classroomForm.shortDescription}
            />
          </Field>

          <button
            className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-[#4b0009] px-4 py-2.5 text-sm font-extrabold text-white transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-60"
            disabled={creatingClassroom}
            type="submit"
          >
            <Save className="h-4 w-4" />
            {creatingClassroom ? 'Đang tạo lớp...' : 'Tạo lớp khai giảng'}
          </button>
        </form>

        <section className="min-w-0 space-y-5">
          {loading ? <ClassroomLoadingState message="Đang tải danh sách lớp..." /> : null}
          {!loading && error ? <ClassroomErrorState message={error} onRetry={loadClassrooms} /> : null}
          {!loading && !error && !classrooms.length ? (
            <ClassroomEmptyState description="Chưa có lớp học nào. Hãy tạo lớp đầu tiên ở form bên trái." title="Chưa có lớp" />
          ) : null}

          {!loading && !error && classrooms.length ? (
            <div className="space-y-3">
              <p className="text-sm text-[#8b706e]">
                Chọn một lớp để mở trang quản lý: hàng đợi đăng ký, học viên, lịch học và công bố khai giảng.
              </p>
              {classrooms.map((item) => (
                <button
                  className="flex w-full items-center justify-between gap-4 rounded-xl border border-[#f0e4e2] bg-white px-4 py-4 text-left shadow-sm transition hover:border-[#dfbfbd] hover:bg-[#fffafb]"
                  key={item.id}
                  onClick={() => navigate(`/training-manager/classrooms/${item.id}`)}
                  type="button"
                >
                  <div className="min-w-0">
                    <p className="truncate font-['Manrope'] text-base font-extrabold text-[#2b2828]">{item.title}</p>
                    <p className="mt-1 text-xs text-[#8b706e]">
                      {formatDeliveryMode(item.deliveryMode, item.deliveryModeLabel)}
                      {' · '}
                      {formatOfferingStatus(item.classroomStatus)}
                      {' · '}
                      Khai giảng {formatClassroomDate(item.startDate)}
                    </p>
                    <p className="mt-1 text-xs text-[#584140]">
                      Sĩ số {item.enrolledCount ?? 0}/{item.maxCapacity ?? '-'}
                      {' · '}
                      {formatClassroomPrice(item.salePrice ?? item.price ?? 0)}
                    </p>
                    <p className="mt-1 text-xs text-[#584140]">
                      Giáo trình: {item.curriculumProgramTitle || 'Chưa chọn'}
                    </p>
                  </div>
                  <ChevronRight className="h-5 w-5 flex-shrink-0 text-[#730014]" />
                </button>
              ))}
            </div>
          ) : null}
        </section>
      </section>
    </div>
  );
}

const inputClass = 'w-full rounded-xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-3 py-2.5 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white';

function Field({ children, label }) {
  return (
    <label className="block space-y-2">
      <span className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">{label}</span>
      {children}
    </label>
  );
}

function InfoNote({ label, text }) {
  return (
    <div className="space-y-2">
      <span className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">{label}</span>
      <p className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-xs leading-5 text-slate-600">
        {text}
      </p>
    </div>
  );
}

function MetricCard({ icon: Icon, label, value }) {
  return (
    <article className="rounded-xl border border-[#dfbfbd]/35 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-[#fff1f3] text-[#730014]">
          <Icon className="h-4 w-4" />
        </div>
        <div>
          <p className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">{label}</p>
          <p className="mt-0.5 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{value}</p>
        </div>
      </div>
    </article>
  );
}
