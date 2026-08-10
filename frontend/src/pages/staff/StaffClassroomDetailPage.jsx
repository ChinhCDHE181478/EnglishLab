import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { ArrowRightLeft, ChevronDown, ChevronUp, Plus, RefreshCw, Trash2, Users, Video, XCircle } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  ClassroomTabBar,
} from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import VietnameseDateInput from '../../components/ui/VietnameseDateInput';
import { useAppDialog } from '../../components/ui/AppDialog';
import StaffRecordingsPage from './StaffRecordingsPage';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { validateClassroomSessionForm } from '../../utils/classroomFormValidation';
import {
  formatClassroomDate,
  formatClassroomDateTime,
  formatClassroomPrice,
  formatDeliveryMode,
  formatOfferingStatus,
  formatRegistrationStatus,
} from '../../utils/classroomHelpers';

const detailTabs = [
  { id: 'overview', label: 'Tổng quan' },
  { id: 'students', label: 'Học viên' },
  { id: 'schedule', label: 'Lịch học' },
  { id: 'recordings', label: 'Ghi hình' },
];

const deliveryModeOptions = [
  { label: 'Tại trung tâm', value: 'OFFLINE' },
  { label: 'Virtual', value: 'VIRTUAL' },
];

const initialSessionForm = {
  sessionDate: '',
  startTime: '19:00',
  endTime: '21:00',
  deliveryMode: 'OFFLINE',
  teacherId: '',
  roomId: '',
  sessionContent: 'Buổi học',
  note: '',
};

const inputClass = 'w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white';

export default function StaffClassroomDetailPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedTab = searchParams.get('tab') || 'overview';
  const activeTab = detailTabs.some((tab) => tab.id === requestedTab) ? requestedTab : 'overview';

  const [classroom, setClassroom] = useState(null);
  const [syncingSessionId, setSyncingSessionId] = useState(null);
  const [teachers, setTeachers] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [allClassrooms, setAllClassrooms] = useState([]);
  const [transfer, setTransfer] = useState({ enrollment: null, targetId: '' });
  const [studentActionId, setStudentActionId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [actionTone, setActionTone] = useState('success');
  const [sessionForm, setSessionForm] = useState(initialSessionForm);
  const [creatingSession, setCreatingSession] = useState(false);
  const [closingClass, setClosingClass] = useState(false);

  const teacherOptions = useMemo(
    () => [
      { label: 'Chưa chọn giáo viên', value: '' },
      ...teachers.map((item) => ({ label: item.label, value: String(item.id) })),
    ],
    [teachers],
  );

  const roomOptions = useMemo(
    () => [{ label: 'Chưa chọn phòng', value: '' }, ...rooms.map((item) => ({ label: item.label, value: String(item.id) }))],
    [rooms],
  );

  const assignedStudents = useMemo(
    () => (classroom?.enrollments || []).filter((item) => item.registrationStatus === 'ASSIGNED'),
    [classroom],
  );

  const scheduledSessions = useMemo(
    () => [...(classroom?.sessions || [])].sort((left, right) => {
      const leftKey = `${left.sessionDate || ''}T${left.startTime || ''}`;
      const rightKey = `${right.sessionDate || ''}T${right.startTime || ''}`;
      return leftKey.localeCompare(rightKey);
    }),
    [classroom?.sessions],
  );

  const loadClassroom = async () => {
    setLoading(true);
    setError('');
    try {
      const [data, teacherData, roomData, classroomData] = await Promise.all([
        classroomApi.getStaffClassroom(id),
        classroomApi.getStaffTeachers(),
        classroomApi.getStaffRooms(),
        classroomApi.getStaffClassrooms(),
      ]);
      setClassroom(data);
      setTeachers(teacherData);
      setRooms(roomData);
      setAllClassrooms(classroomData);
      setSessionForm((current) => ({
        ...current,
        sessionDate: current.sessionDate || data.startDate || '',
        deliveryMode: data.deliveryMode || current.deliveryMode,
        teacherId: current.teacherId || (data.primaryTeacherId ? String(data.primaryTeacherId) : ''),
        roomId: current.roomId || (data.roomId ? String(data.roomId) : ''),
      }));
    } catch (err) {
      setClassroom(null);
      setError(getClassroomErrorMessage(err, 'Không thể tải chi tiết lớp.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadClassroom();
  }, [id]);

  const setTab = (tabId) => {
    const next = new URLSearchParams(searchParams);
    next.set('tab', tabId);
    next.delete('enrollmentId');
    setSearchParams(next, { replace: true });
  };

  const handleCreateSession = async (event) => {
    event.preventDefault();
    setActionMessage('');
    const validationMessage = validateClassroomSessionForm(sessionForm);
    if (validationMessage) {
      setActionTone('error');
      setActionMessage(validationMessage);
      return;
    }
    setCreatingSession(true);
    try {
      const created = await classroomApi.createStaffClassroomSession(id, {
        ...sessionForm,
        teacherId: sessionForm.teacherId ? Number(sessionForm.teacherId) : null,
        roomId: sessionForm.roomId ? Number(sessionForm.roomId) : null,
      });
      const meetingPending = created.deliveryMode === 'VIRTUAL'
        && !['SYNCED', 'MANUAL'].includes(created.larkSyncStatus);
      if (meetingPending) {
        setActionTone('warning');
        setActionMessage(
          `Đã thêm buổi học nhưng chưa tạo được Google Meet. ${created.larkSyncError || 'Hệ thống sẽ tự thử lại theo lịch.'}`,
        );
      } else {
        setActionTone('success');
        setActionMessage(created.deliveryMode === 'VIRTUAL'
          ? 'Đã thêm buổi học và tạo phòng Google Meet tự động.'
          : 'Đã thêm buổi học vào lịch.');
      }
      setSessionForm((current) => ({ ...initialSessionForm, deliveryMode: current.deliveryMode }));
      await loadClassroom();
    } catch (err) {
      setActionTone('error');
      setActionMessage(getClassroomErrorMessage(err, 'Không thể thêm buổi học.'));
    } finally {
      setCreatingSession(false);
    }
  };

  const handleSyncVirtualMeeting = async (session) => {
    setSyncingSessionId(session.id);
    setActionMessage('');
    try {
      const updated = await classroomApi.syncStaffVirtualSessionMeeting(session.id);
      if (updated.larkSyncStatus === 'SYNCED') {
        setActionTone('success');
        setActionMessage(`Đã tạo phòng Google Meet tự động cho buổi học ngày ${formatClassroomDate(updated.sessionDate)}.`);
      } else {
        setActionTone('error');
        setActionMessage(updated.larkSyncError || 'Chưa thể đồng bộ phòng Google Meet. Vui lòng kiểm tra cấu hình tích hợp.');
      }
      await loadClassroom();
    } catch (err) {
      setActionTone('error');
      setActionMessage(getClassroomErrorMessage(err, 'Không thể đồng bộ phòng Google Meet.'));
    } finally {
      setSyncingSessionId(null);
    }
  };

  const handleCloseClass = async () => {
    if (!await confirmDialog('Lớp sẽ được đóng và không còn hiển thị trong danh sách tuyển sinh.', {
      title: 'Đóng lớp học',
      confirmLabel: 'Đóng lớp',
      tone: 'danger',
    })) {
      return;
    }
    setClosingClass(true);
    setActionMessage('');
    try {
      await classroomApi.closeClassroomOffering(id);
      setActionTone('success');
      setActionMessage('Đã đóng lớp học.');
      await loadClassroom();
    } catch (err) {
      setActionTone('error');
      setActionMessage(getClassroomErrorMessage(err, 'Không thể đóng lớp học.'));
    } finally {
      setClosingClass(false);
    }
  };

  const handleRemoveStudent = async (enrollment) => {
    if (!await confirmDialog(`Loại ${enrollment.studentName || enrollment.studentEmail} khỏi danh sách lớp hiện tại?`, {
      title: 'Loại học viên khỏi lớp',
      confirmLabel: 'Loại khỏi lớp',
      tone: 'danger',
    })) return;
    setStudentActionId(enrollment.id);
    setActionMessage('');
    try {
      await classroomApi.removeStudent(id, enrollment.studentId);
      setActionTone('success');
      setActionMessage('Đã loại học viên khỏi danh sách lớp hiện tại.');
      await loadClassroom();
    } catch (err) {
      setActionTone('error');
      setActionMessage(getClassroomErrorMessage(err, 'Không thể loại học viên khỏi lớp.'));
    } finally {
      setStudentActionId(null);
    }
  };

  const handleTransferStudent = async () => {
    if (!transfer.enrollment || !transfer.targetId) return;
    setStudentActionId(transfer.enrollment.id);
    setActionMessage('');
    try {
      await classroomApi.transferStudent(id, {
        studentId: transfer.enrollment.studentId,
        targetClassroomOfferingId: Number(transfer.targetId),
      });
      setActionTone('success');
      setActionMessage('Đã chuyển học viên sang lớp mới; tên đã được loại khỏi danh sách lớp nguồn.');
      setTransfer({ enrollment: null, targetId: '' });
      await loadClassroom();
    } catch (err) {
      setActionTone('error');
      setActionMessage(getClassroomErrorMessage(err, 'Không thể chuyển lớp cho học viên.'));
    } finally {
      setStudentActionId(null);
    }
  };

  if (loading) {
    return <ClassroomLoadingState message="Đang tải thông tin lớp..." />;
  }

  if (error || !classroom) {
    return (
      <div className="space-y-4">
        <ClassroomErrorState message={error || 'Không tìm thấy lớp học.'} onRetry={loadClassroom} />
        <button
          className="text-sm font-bold text-[#730014] hover:underline"
          onClick={() => navigate('/staff/classrooms')}
          type="button"
        >
          ← Quay lại danh sách lớp
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link className="text-sm font-bold text-[#730014] hover:underline" to="/staff/classrooms">
          ← Danh sách lớp
        </Link>
        <div className="flex flex-wrap gap-2">
          {classroom.classroomStatus !== 'CLOSED' && classroom.classroomStatus !== 'CANCELLED' ? (
            <button
              className="inline-flex items-center gap-2 rounded-xl border border-rose-200 bg-white px-4 py-2.5 text-sm font-extrabold text-rose-700 transition hover:bg-rose-50 disabled:opacity-60"
              disabled={closingClass}
              onClick={handleCloseClass}
              type="button"
            >
              <XCircle className="h-4 w-4" />
              {closingClass ? 'Đang đóng...' : 'Đóng lớp'}
            </button>
          ) : null}
        </div>
      </div>

      {actionMessage ? (
        <div
          aria-live="polite"
          className={`fixed bottom-6 right-6 z-[70] max-w-[min(420px,calc(100vw-2rem))] rounded-2xl border px-4 py-3 text-sm font-semibold shadow-xl ${
          actionTone === 'error'
            ? 'border-red-200 bg-red-50 text-red-800'
            : actionTone === 'warning'
              ? 'border-amber-200 bg-amber-50 text-amber-800'
              : 'border-emerald-100 bg-emerald-50 text-emerald-800'
        }`}
        >
          {actionMessage}
        </div>
      ) : null}

      <section className="rounded-xl border border-[#dfbfbd]/35 bg-white p-5 shadow-sm">
        <div className="flex flex-wrap gap-2">
          <Badge>{formatDeliveryMode(classroom.deliveryMode, classroom.deliveryModeLabel)}</Badge>
          <Badge>{formatOfferingStatus(classroom.classroomStatus)}</Badge>
          <Badge>{classroom.entryLevel || 'Chưa gắn level'}</Badge>
          <Badge>{classroom.curriculumProgramTitle || 'Chưa chọn giáo trình'}</Badge>
        </div>
        <h2 className="mt-3 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{classroom.title}</h2>
        <p className="mt-2 text-sm text-[#584140]">
          Khai giảng: <strong>{formatClassroomDate(classroom.startDate)}</strong>
          {' · '}
          Sĩ số: <strong>{classroom.enrolledCount ?? 0}/{classroom.maxCapacity ?? '-'}</strong>
          {' · '}
          Học phí: <strong>{formatClassroomPrice(classroom.salePrice ?? classroom.price ?? 0)}</strong>
        </p>
      </section>

      <ClassroomTabBar activeTab={activeTab} onChange={setTab} tabs={detailTabs} />

      {activeTab === 'overview' ? (
        <>
          <section className="grid gap-4 md:grid-cols-2">
            <OverviewCard
              description="Học viên đã được xếp lớp chính thức"
              icon={Users}
              label="Đã xếp lớp"
              onClick={() => setTab('students')}
              value={classroom.enrolledCount ?? 0}
            />
            <OverviewCard
              description="Số buổi học đã lên lịch"
              icon={Plus}
              label="Buổi học"
              onClick={() => setTab('schedule')}
              value={classroom.sessions?.length ?? 0}
            />
          </section>
          <CurriculumOverview curriculum={classroom.curriculumProgram} />
        </>
      ) : null}

      {activeTab === 'students' ? (
        <section className="rounded-xl border border-[#e5e7eb] bg-white p-5 shadow-sm">
          <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Học viên đã xếp lớp</h3>
          {assignedStudents.length ? (
            <div className="mt-4 space-y-3">
              {assignedStudents.map((enrollment) => (
                <article className="flex flex-col gap-3 rounded-xl border border-[#f0e4e2] px-4 py-3 text-sm text-[#584140] sm:flex-row sm:items-center sm:justify-between" key={enrollment.id}>
                  <div><p className="font-extrabold text-[#2b2828]">{enrollment.studentName || enrollment.studentEmail}</p><p className="mt-1">{formatRegistrationStatus(enrollment.registrationStatus, enrollment.registrationStatusLabel)}{' · '}{formatClassroomPrice(enrollment.tuitionAmountPaid ?? 0)} / {formatClassroomPrice(enrollment.tuitionAmountDue)}</p></div>
                  <div className="flex flex-wrap gap-2"><button className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-3 py-2 text-xs font-extrabold text-[#730014]" disabled={studentActionId === enrollment.id} onClick={() => setTransfer({ enrollment, targetId: '' })} type="button"><ArrowRightLeft className="h-3.5 w-3.5" />Chuyển lớp</button><button className="inline-flex items-center gap-2 rounded-xl border border-rose-200 px-3 py-2 text-xs font-extrabold text-rose-700" disabled={studentActionId === enrollment.id} onClick={() => handleRemoveStudent(enrollment)} type="button"><Trash2 className="h-3.5 w-3.5" />Loại khỏi lớp</button></div>
                </article>
              ))}
            </div>
          ) : (
            <ClassroomEmptyState
              description="Lớp chưa có học viên."
              title="Chưa có học viên"
            />
          )}
        </section>
      ) : null}

      {transfer.enrollment ? <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4"><section className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl"><h2 className="text-xl font-black text-[#2b2828]">Chuyển lớp cho {transfer.enrollment.studentName || transfer.enrollment.studentEmail}</h2><p className="mt-2 text-sm leading-6 text-[#584140]">Học viên sẽ được chuyển sang lớp đã chọn.</p><div className="mt-5"><BrandedSelect onChange={(event) => setTransfer((current) => ({ ...current, targetId: event.target.value }))} options={allClassrooms.filter((item) => String(item.id) !== String(id) && ['UPCOMING', 'ACTIVE'].includes(item.classroomStatus)).map((item) => ({ value: String(item.id), label: item.title, description: `${formatClassroomDate(item.startDate)} · ${item.primaryTeacherName || 'Chưa có giáo viên'}` }))} placeholder="Chọn lớp đích" searchable value={transfer.targetId} /></div><div className="mt-6 flex justify-end gap-2"><button className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-bold" onClick={() => setTransfer({ enrollment: null, targetId: '' })} type="button">Hủy</button><button className="rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white disabled:opacity-50" disabled={!transfer.targetId || studentActionId === transfer.enrollment.id} onClick={handleTransferStudent} type="button">Xác nhận chuyển lớp</button></div></section></div> : null}

      {activeTab === 'schedule' ? (
        <div className="space-y-5">
          <form className="space-y-4 rounded-xl border border-[#dfbfbd]/35 bg-white p-5 shadow-sm" onSubmit={handleCreateSession}>
            <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Thêm buổi học</h3>
            <div className="grid gap-4 md:grid-cols-4">
              <Field label="Ngày học">
                <VietnameseDateInput className={inputClass} onChange={(value) => setSessionForm((current) => ({ ...current, sessionDate: value }))} required value={sessionForm.sessionDate} />
              </Field>
              <Field label="Bắt đầu">
                <input className={inputClass} onChange={(e) => setSessionForm((c) => ({ ...c, startTime: e.target.value }))} required type="time" value={sessionForm.startTime} />
              </Field>
              <Field label="Kết thúc">
                <input className={inputClass} onChange={(e) => setSessionForm((c) => ({ ...c, endTime: e.target.value }))} required type="time" value={sessionForm.endTime} />
              </Field>
              <Field label="Hình thức">
                <BrandedSelect onChange={(e) => setSessionForm((c) => ({ ...c, deliveryMode: e.target.value }))} options={deliveryModeOptions} value={sessionForm.deliveryMode} />
              </Field>
            </div>
            <div className="grid gap-4 md:grid-cols-2">
              <Field label="Giáo viên">
                <BrandedSelect onChange={(e) => setSessionForm((c) => ({ ...c, teacherId: e.target.value }))} options={teacherOptions} value={sessionForm.teacherId} />
              </Field>
              {sessionForm.deliveryMode === 'OFFLINE' ? (
                <Field label="Phòng">
                  <BrandedSelect onChange={(e) => setSessionForm((c) => ({ ...c, roomId: e.target.value }))} options={roomOptions} value={sessionForm.roomId} />
                </Field>
              ) : null}
            </div>
            <button className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] bg-white px-4 py-2.5 text-sm font-extrabold text-[#4b0009] hover:bg-[#fff3f4] disabled:opacity-60" disabled={creatingSession} type="submit">
              <Plus className="h-4 w-4" />
              {creatingSession ? 'Đang thêm...' : 'Thêm buổi học'}
            </button>
          </form>

          <section className="rounded-xl border border-[#dfbfbd]/35 bg-white p-5 shadow-sm">
            <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Lịch học đã tạo</h3>
            {scheduledSessions.length ? (
              <div className="mt-3 divide-y divide-[#f0e4e2] overflow-hidden rounded-xl border border-[#f0e4e2]">
                {scheduledSessions.map((session) => (
                  <div className="grid gap-3 px-4 py-3 text-sm text-[#584140] md:grid-cols-[1.2fr_.8fr_.8fr_1.4fr]" key={session.id}>
                    <p className="font-semibold text-[#2b2828]">
                      {formatClassroomDateTime(`${session.sessionDate}T${session.startTime}`)}
                    </p>
                    <p>{session.startTime} - {session.endTime}</p>
                    <p>{formatDeliveryMode(session.deliveryMode, session.deliveryModeLabel)}</p>
                    {session.deliveryMode === 'VIRTUAL' ? (
                      <VirtualMeetingStatus
                        onRetry={() => handleSyncVirtualMeeting(session)}
                        session={session}
                        syncing={syncingSessionId === session.id}
                      />
                    ) : (
                      <p className="text-slate-500">{session.roomName || classroom.offlineAddress || 'Chưa chọn phòng'}</p>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <p className="mt-3 rounded-xl bg-[#fffafb] px-4 py-3 text-sm text-[#8b706e]">
                Chưa có buổi học. Thêm buổi khai giảng để học viên nhìn thấy lịch cụ thể.
              </p>
            )}
          </section>
        </div>
      ) : null}

      {activeTab === 'recordings' ? <StaffRecordingsPage classroomId={id} /> : null}
    </div>
  );
}

function VirtualMeetingStatus({ session, syncing, onRetry }) {
  const synced = session.larkSyncStatus === 'SYNCED' && Boolean(session.larkMeetingUrl);
  const terminal = ['COMPLETED', 'CANCELLED'].includes(session.status);
  const statusLabel = {
    DISABLED: 'Google Meet chưa được cấu hình',
    FAILED: 'Tạo phòng Google Meet thất bại',
    MANUAL: 'Liên kết Google Meet nhập thủ công',
    PENDING: 'Đang chờ tạo phòng Google Meet',
    SYNCED: 'Phòng Google Meet đã sẵn sàng',
  }[session.larkSyncStatus] || 'Chưa tạo phòng Google Meet';

  if (synced) {
    return (
      <div className="min-w-0">
        <p className="inline-flex items-center gap-1.5 font-bold text-emerald-700">
          <Video className="h-4 w-4 shrink-0" />
          {statusLabel}
        </p>
        {session.larkMeetingNo ? (
          <p className="mt-1 text-xs text-slate-500">Mã phòng: {session.larkMeetingNo}</p>
        ) : null}
        <a
          className="mt-1 inline-flex text-xs font-extrabold text-[#730014] underline underline-offset-2"
          href={session.larkMeetingUrl}
          rel="noreferrer"
          target="_blank"
        >
          Mở phòng
        </a>
      </div>
    );
  }

  return (
    <div className="min-w-0">
      <p className={`font-bold ${session.larkSyncStatus === 'FAILED' ? 'text-red-700' : 'text-amber-700'}`}>
        {statusLabel}
      </p>
      {session.larkSyncError ? (
        <p className="mt-1 line-clamp-2 text-xs leading-5 text-slate-500" title={session.larkSyncError}>
          {session.larkSyncError}
        </p>
      ) : null}
      {!terminal ? (
        <button
          className="mt-2 inline-flex cursor-pointer items-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-white px-2.5 py-1.5 text-xs font-extrabold text-[#730014] transition-all hover:border-[#730014] hover:bg-[#730014] hover:text-white hover:shadow-sm active:scale-[0.97] disabled:opacity-60 disabled:hover:border-[#dfbfbd] disabled:hover:bg-white disabled:hover:text-[#730014]"
          disabled={syncing}
          onClick={onRetry}
          type="button"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${syncing ? 'animate-spin' : ''}`} />
          {syncing ? 'Đang đồng bộ...' : 'Thử tạo lại'}
        </button>
      ) : null}
    </div>
  );
}

function Badge({ children }) {
  return <span className="rounded-full bg-[#fff1f3] px-3 py-1 text-xs font-bold text-[#730014]">{children}</span>;
}

function Field({ label, children }) {
  return (
    <label className="block space-y-2">
      <span className="text-xs font-bold uppercase tracking-wider text-[#8b706e]">{label}</span>
      {children}
    </label>
  );
}

function OverviewCard({ icon: Icon, label, value, description, onClick }) {
  return (
    <button
      className="rounded-xl border border-[#e5e7eb] bg-white p-5 text-left shadow-sm transition hover:border-[#dfbfbd]/60 hover:shadow-md"
      onClick={onClick}
      type="button"
    >
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#fff1f3] text-[#730014]">
          <Icon className="h-5 w-5" />
        </div>
        <div>
          <p className="text-2xl font-black text-[#2b2828]">{value}</p>
          <p className="text-sm font-extrabold text-[#584140]">{label}</p>
        </div>
      </div>
      <p className="mt-3 text-xs leading-5 text-[#8b706e]">{description}</p>
    </button>
  );
}

function CurriculumOverview({ curriculum }) {
  const [expandedUnitIds, setExpandedUnitIds] = useState(new Set());
  if (!curriculum) {
    return (
      <section className="rounded-xl border border-amber-200 bg-amber-50 p-5 text-sm font-semibold text-amber-800">
        Lớp này chưa được gắn giáo trình. Hãy cập nhật lớp từ trang mở lớp để chọn giáo trình theo band/target.
      </section>
    );
  }
  const units = curriculum.units || [];
  const toggleUnit = (unitId) => {
    setExpandedUnitIds((current) => {
      const next = new Set(current);
      if (next.has(unitId)) next.delete(unitId);
      else next.add(unitId);
      return next;
    });
  };

  const setAllUnitsExpanded = (expanded) => {
    setExpandedUnitIds(expanded ? new Set(units.map((unit) => unit.id)) : new Set());
  };

  return (
    <section className="rounded-xl border border-[#e5e7eb] bg-white p-5 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs font-bold uppercase tracking-wider text-[#8b706e]">Giáo trình đang dùng</p>
          <h3 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{curriculum.title}</h3>
          <p className="mt-1 text-sm text-[#584140]">
            {[curriculum.code, curriculum.examCategory, curriculum.targetBand ? `Band ${curriculum.targetBand}` : null, curriculum.targetScore ? `Target ${curriculum.targetScore}` : null].filter(Boolean).join(' · ')}
          </p>
        </div>
        <Badge>{units.length} unit/buổi</Badge>
      </div>
      {curriculum.outcomes ? <p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-[#584140]">{curriculum.outcomes}</p> : null}
      {units.length ? (
        <div className="mt-5 space-y-3">
          <div className="flex items-center justify-between border-b border-[#f0e4e2] pb-3">
            <p className="text-xs font-bold uppercase tracking-wider text-[#8b706e]">Nội dung theo unit</p>
            <div className="flex items-center gap-3 text-xs font-bold">
              <button className="text-[#730014] transition hover:underline" onClick={() => setAllUnitsExpanded(true)} type="button">Mở rộng tất cả</button>
              <span className="text-[#dcc0bf]">|</span>
              <button className="text-[#584140] transition hover:text-[#730014] hover:underline" onClick={() => setAllUnitsExpanded(false)} type="button">Thu gọn</button>
            </div>
          </div>
          {units.map((unit) => (
            <article key={unit.id} className={`overflow-hidden rounded-xl border bg-white transition ${expandedUnitIds.has(unit.id) ? 'border-[#730014]/35 shadow-sm' : 'border-[#f0e4e2] hover:border-[#dfbfbd]'}`}>
              <button className="flex w-full items-center justify-between gap-4 bg-[#fffafb] p-4 text-left" onClick={() => toggleUnit(unit.id)} type="button">
                <div className="min-w-0">
                  <p className="font-extrabold text-[#2b2828]">{unit.displayOrder ?? 0}. {unit.title}</p>
                  <p className="mt-1 text-xs font-semibold text-[#8b706e]">
                    Học liệu {(unit.materials || []).length} · Bài tập {(unit.exercises || []).length} · Đề {(unit.assessments || []).length} · Flashcard {(unit.flashcards || []).length}
                  </p>
                </div>
                {expandedUnitIds.has(unit.id) ? <ChevronUp className="h-5 w-5 shrink-0 text-[#730014]" /> : <ChevronDown className="h-5 w-5 shrink-0 text-[#8b706e]" />}
              </button>
              {expandedUnitIds.has(unit.id) ? (
                <div className="space-y-4 border-t border-[#f0e4e2] p-4">
                  {unit.description ? <p className="text-sm leading-6 text-[#584140]">{unit.description}</p> : null}
                  {unit.sessionPlan ? <div className="rounded-lg border border-slate-100 bg-slate-50 p-3 text-sm leading-6 text-[#584140]"><p className="mb-1 text-xs font-bold uppercase tracking-wider text-[#8b706e]">Kế hoạch buổi học</p>{unit.sessionPlan}</div> : null}
                  <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                    <CurriculumUnitResources label="Học liệu" items={unit.materials} />
                    <CurriculumUnitResources label="Bài tập" items={unit.exercises} />
                    <CurriculumUnitResources label="Đề đánh giá" items={unit.assessments} />
                    <CurriculumUnitResources label="Flashcard" items={unit.flashcards} />
                  </div>
                </div>
              ) : null}
            </article>
          ))}
        </div>
      ) : (
        <p className="mt-4 rounded-xl bg-slate-50 px-4 py-3 text-sm text-slate-600">Giáo trình này chưa có unit/buổi học.</p>
      )}
    </section>
  );
}

function CurriculumUnitResources({ label, items = [] }) {
  return (
    <div className="rounded-lg border border-slate-100 bg-slate-50/70 p-3">
      <p className="text-xs font-bold uppercase tracking-wider text-[#8b706e]">{label}</p>
      {items.length ? (
        <ul className="mt-2 space-y-2 text-sm text-[#584140]">
          {items.map((item) => <li key={item.id} className="rounded-md bg-white px-2.5 py-2 font-semibold">{item.title || item.name || 'Nội dung đang cập nhật'}</li>)}
        </ul>
      ) : <p className="mt-2 text-xs text-slate-400">Chưa có nội dung.</p>}
    </div>
  );
}
