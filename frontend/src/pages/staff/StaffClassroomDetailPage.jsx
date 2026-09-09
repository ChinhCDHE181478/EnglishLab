import { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { ArrowRightLeft, Bell, ChevronDown, ChevronUp, Pencil, Plus, RefreshCw, Trash2, UserRoundCheck, Users, Video, X, XCircle } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  ClassroomTabBar,
} from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import RichTextHtml from '../../components/content-manager/RichTextHtml';
import ManagementToast from '../../components/ui/ManagementToast';
import VietnameseDateInput from '../../components/ui/VietnameseDateInput';
import { useAppDialog } from '../../components/ui/AppDialog';
import StaffRecordingsPage from './StaffRecordingsPage';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { validateClassroomSessionForm } from '../../utils/classroomFormValidation';
import {
  formatClassroomDate,
  formatClassroomPrice,
  formatDeliveryMode,
  formatOfferingStatus,
  formatRegistrationStatus,
  formatSessionStatus,
  getClassroomSessionTitle,
  getClassroomSessionUnitLabel,
} from '../../utils/classroomHelpers';

const detailTabs = [
  { id: 'overview', label: 'Tổng quan' },
  { id: 'students', label: 'Học viên' },
  { id: 'schedule', label: 'Lịch học' },
  { id: 'announcements', label: 'Thông báo' },
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
  courseLessonId: '',
  sessionContent: 'Buổi học',
  note: '',
};

const inputClass = 'w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white';

const readSessionDraft = (key) => {
  try {
    const storedDraft = window.sessionStorage.getItem(key);
    return storedDraft ? JSON.parse(storedDraft) : null;
  } catch {
    return null;
  }
};

const writeSessionDraft = (key, form) => {
  try {
    window.sessionStorage.setItem(key, JSON.stringify(form));
  } catch {
    // Draft persistence must not block scheduling when browser storage is unavailable.
  }
};

const clearSessionDraft = (key) => {
  try {
    window.sessionStorage.removeItem(key);
  } catch {
    // The form can still be reset in memory when browser storage is unavailable.
  }
};

export default function StaffClassroomDetailPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedTab = searchParams.get('tab') || 'overview';
  const activeTab = detailTabs.some((tab) => tab.id === requestedTab) ? requestedTab : 'overview';

  const [classroom, setClassroom] = useState(null);
  const [announcements, setAnnouncements] = useState([]);
  const [announcementComposerOpen, setAnnouncementComposerOpen] = useState(false);
  const [announcementForm, setAnnouncementForm] = useState({ title: '', content: '' });
  const [savingAnnouncement, setSavingAnnouncement] = useState(false);
  const [syncingSessionId, setSyncingSessionId] = useState(null);
  const [allClassrooms, setAllClassrooms] = useState([]);
  const [transfer, setTransfer] = useState({ enrollment: null, targetId: '' });
  const [studentActionId, setStudentActionId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [actionTone, setActionTone] = useState('success');
  const [sessionForm, setSessionForm] = useState(initialSessionForm);
  const [creatingSession, setCreatingSession] = useState(false);
  const [editingSessionId, setEditingSessionId] = useState(null);
  const [sessionModalOpen, setSessionModalOpen] = useState(false);
  const [availableResources, setAvailableResources] = useState({ status: 'idle', teachers: [], rooms: [] });
  const [replacement, setReplacement] = useState({ open: false, teacherId: '', options: [], loading: false });
  const [replacingTeacher, setReplacingTeacher] = useState(false);
  const [closingClass, setClosingClass] = useState(false);

  const teacherOptions = useMemo(
    () => [
      { label: 'Giáo viên chính của lớp', value: '' },
      ...availableResources.teachers.map((item) => ({
        label: item.fullName || item.email,
        value: String(item.id),
        description: item.email,
      })),
    ],
    [availableResources.teachers],
  );

  const roomOptions = useMemo(
    () => [{ label: 'Chưa chọn phòng', value: '' }, ...availableResources.rooms.map((item) => ({
      label: item.name,
      value: String(item.id),
      description: `${item.capacity || 0} chỗ`,
    }))],
    [availableResources.rooms],
  );

  const sessionDraftStorageKey = `englishlab.staff.classroom.${id}.session-draft`;

  const getDefaultSessionForm = () => ({
    ...initialSessionForm,
    sessionDate: classroom?.startDate || '',
    deliveryMode: classroom?.deliveryMode || 'OFFLINE',
    teacherId: classroom?.primaryTeacherId ? String(classroom.primaryTeacherId) : '',
    roomId: classroom?.roomId ? String(classroom.roomId) : '',
  });

  const courseLessonOptions = useMemo(() => [
    { label: 'Chọn bài học', value: '' },
    ...(classroom?.instructorLedCourse?.units || []).flatMap((unit) => (
      (unit.lessons || []).map((plan) => ({
        label: `Bài ${plan.sessionNumber}: ${plan.title}`,
        value: String(plan.id),
      }))
    )),
  ], [classroom?.instructorLedCourse?.units]);

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
  const classIsMutable = ['DRAFT', 'UPCOMING', 'ACTIVE'].includes(classroom?.classroomStatus);

  const loadClassroom = async () => {
    setLoading(true);
    setError('');
    try {
      const [data, classroomData, announcementsData] = await Promise.all([
        classroomApi.getStaffClassroom(id),
        classroomApi.getStaffClassrooms(),
        classroomApi.getStaffClassroomAnnouncements(id),
      ]);
      setClassroom(data);
      setAllClassrooms(classroomData);
      setAnnouncements(announcementsData);
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

  const createAnnouncement = async () => {
    const title = announcementForm.title.trim();
    const content = announcementForm.content.trim();
    if (!title || !content) {
      setActionTone('error');
      setActionMessage('Vui lòng nhập tiêu đề và nội dung thông báo.');
      return;
    }

    setSavingAnnouncement(true);
    try {
      const created = await classroomApi.createStaffClassroomAnnouncement(id, { title, content });
      setAnnouncements((current) => [created, ...current]);
      setAnnouncementForm({ title: '', content: '' });
      setAnnouncementComposerOpen(false);
      setActionTone('success');
      setActionMessage('Đã gửi thông báo tới lớp học.');
    } catch (err) {
      setActionTone('error');
      setActionMessage(getClassroomErrorMessage(err, 'Không thể gửi thông báo.'));
    } finally {
      setSavingAnnouncement(false);
    }
  };

  useEffect(() => {
    loadClassroom();
  }, [id]);

  useEffect(() => {
    if (!sessionModalOpen) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [sessionModalOpen]);

  useEffect(() => {
    if (!sessionModalOpen || !sessionForm.sessionDate || !sessionForm.startTime || !sessionForm.endTime
        || sessionForm.endTime <= sessionForm.startTime) {
      setAvailableResources({ status: 'idle', teachers: [], rooms: [] });
      return undefined;
    }

    let active = true;
    setAvailableResources((current) => ({ ...current, status: 'loading' }));
    const timer = window.setTimeout(async () => {
      const params = {
        sessionDate: sessionForm.sessionDate,
        startTime: sessionForm.startTime,
        endTime: sessionForm.endTime,
        excludeSessionId: editingSessionId || undefined,
      };
      try {
        const [availableTeachers, availableRooms] = await Promise.all([
          classroomApi.getStaffAvailableTeachers(params),
          sessionForm.deliveryMode === 'OFFLINE' ? classroomApi.getStaffAvailableRooms(params) : Promise.resolve([]),
        ]);
        if (active) setAvailableResources({ status: 'ready', teachers: availableTeachers, rooms: availableRooms });
      } catch (availabilityError) {
        if (active) setAvailableResources({ status: 'error', teachers: [], rooms: [] });
      }
    }, 300);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [editingSessionId, sessionForm.deliveryMode, sessionForm.endTime, sessionForm.sessionDate, sessionForm.startTime, sessionModalOpen]);

  useEffect(() => {
    if (!sessionModalOpen || editingSessionId) return;
    writeSessionDraft(sessionDraftStorageKey, sessionForm);
  }, [editingSessionId, sessionDraftStorageKey, sessionForm, sessionModalOpen]);

  const setTab = (tabId) => {
    const next = new URLSearchParams(searchParams);
    next.set('tab', tabId);
    next.delete('enrollmentId');
    setSearchParams(next, { replace: true });
  };

  const handleSaveSession = async (event) => {
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
      const { deliveryMode, ...sessionValues } = sessionForm;
      const payload = {
        ...sessionValues,
        deliveryModeOverride: deliveryMode === classroom.deliveryMode ? null : deliveryMode,
        teacherId: sessionForm.teacherId ? Number(sessionForm.teacherId) : null,
        roomId: sessionForm.roomId ? Number(sessionForm.roomId) : null,
        courseLessonId: sessionForm.courseLessonId ? Number(sessionForm.courseLessonId) : null,
      };
      const created = editingSessionId
        ? await classroomApi.updateStaffClassroomSession(editingSessionId, payload)
        : await classroomApi.createStaffClassroomSession(id, payload);
      const meetingPending = created.effectiveDeliveryMode === 'VIRTUAL'
        && created.googleMeetStatus !== 'READY';
      if (meetingPending) {
        setActionTone('warning');
        setActionMessage(
          `${editingSessionId ? 'Đã cập nhật' : 'Đã thêm'} buổi học nhưng chưa tạo được Google Meet. ${created.googleMeetSyncError || 'Hệ thống sẽ tự thử lại theo lịch.'}`,
        );
      } else {
        setActionTone('success');
        setActionMessage(editingSessionId
          ? 'Đã cập nhật buổi học.'
          : created.deliveryMode === 'VIRTUAL'
            ? 'Đã thêm buổi học và tạo phòng Google Meet tự động.'
            : 'Đã thêm buổi học vào lịch.');
      }
      if (!editingSessionId) {
        clearSessionDraft(sessionDraftStorageKey);
      }
      setSessionModalOpen(false);
      setEditingSessionId(null);
      setSessionForm(getDefaultSessionForm());
      await loadClassroom();
    } catch (err) {
      setActionTone('error');
      setActionMessage(getClassroomErrorMessage(err, editingSessionId ? 'Không thể cập nhật buổi học.' : 'Không thể thêm buổi học.'));
    } finally {
      setCreatingSession(false);
    }
  };

  const handleSyncVirtualMeeting = async (session) => {
    setSyncingSessionId(session.id);
    setActionMessage('');
    try {
      const updated = await classroomApi.syncStaffVirtualSessionMeeting(session.id);
      if (updated.googleMeetStatus === 'READY') {
        setActionTone('success');
        setActionMessage(`Đã tạo phòng Google Meet tự động cho buổi học ngày ${formatClassroomDate(updated.sessionDate)}.`);
      } else {
        setActionTone('error');
        setActionMessage(updated.googleMeetSyncError || 'Chưa thể đồng bộ phòng Google Meet. Vui lòng kiểm tra cấu hình tích hợp.');
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

  const openSessionEditor = (session) => {
    setEditingSessionId(session.id);
    setSessionForm({
      sessionDate: session.sessionDate || '',
      startTime: String(session.startTime || '').slice(0, 5),
      endTime: String(session.endTime || '').slice(0, 5),
      deliveryMode: session.effectiveDeliveryMode || classroom.deliveryMode || 'OFFLINE',
      teacherId: session.teacherId ? String(session.teacherId) : '',
      roomId: session.roomId ? String(session.roomId) : '',
      courseLessonId: session.courseLessonId ? String(session.courseLessonId) : '',
      sessionContent: session.sessionContent || 'Buổi học',
      note: session.note || '',
    });
    setActionMessage('');
    setTab('schedule');
    setSessionModalOpen(true);
  };

  const openSessionCreator = () => {
    const defaults = getDefaultSessionForm();
    const draft = readSessionDraft(sessionDraftStorageKey);
    setEditingSessionId(null);
    setSessionForm({
      ...defaults,
      ...(draft && typeof draft === 'object' ? draft : {}),
      deliveryMode: defaults.deliveryMode,
      teacherId: defaults.teacherId,
    });
    setActionMessage('');
    setSessionModalOpen(true);
  };

  const dismissSessionModal = () => {
    if (!editingSessionId) {
      writeSessionDraft(sessionDraftStorageKey, sessionForm);
    }
    setSessionModalOpen(false);
    setEditingSessionId(null);
  };

  const cancelSessionModal = () => {
    if (!editingSessionId) {
      clearSessionDraft(sessionDraftStorageKey);
    }
    setSessionModalOpen(false);
    setEditingSessionId(null);
    setSessionForm(getDefaultSessionForm());
  };

  const openTeacherReplacement = async () => {
    setReplacement({ open: true, teacherId: '', options: [], loading: true });
    try {
      const options = await classroomApi.getAvailableReplacementTeachers(id);
      setReplacement({
        open: true,
        teacherId: '',
        options: options.filter((item) => String(item.id) !== String(classroom.primaryTeacherId)),
        loading: false,
      });
    } catch (err) {
      setReplacement({ open: false, teacherId: '', options: [], loading: false });
      setActionTone('error');
      setActionMessage(getClassroomErrorMessage(err, 'Không thể tải danh sách giáo viên có thể nhận lớp.'));
    }
  };

  const handleReplaceTeacher = async () => {
    if (!replacement.teacherId || !classroom.primaryTeacherId) return;
    setReplacingTeacher(true);
    try {
      await classroomApi.replaceClassroomTeacher(
        id,
        classroom.primaryTeacherId,
        Number(replacement.teacherId),
      );
      setReplacement({ open: false, teacherId: '', options: [], loading: false });
      setActionTone('success');
      setActionMessage('Đã đổi giáo viên chính.');
      await loadClassroom();
    } catch (err) {
      setActionTone('error');
      setActionMessage(getClassroomErrorMessage(err, 'Không thể đổi giáo viên chính.'));
    } finally {
      setReplacingTeacher(false);
    }
  };

  const sessionFormFields = (
    <>
      <div className="grid gap-4 md:grid-cols-2">
        <Field label="Ngày học">
          <VietnameseDateInput className={inputClass} onChange={(value) => setSessionForm((current) => ({ ...current, sessionDate: value, teacherId: '', roomId: '' }))} required value={sessionForm.sessionDate} />
        </Field>
        <Field label="Hình thức buổi học">
          <BrandedSelect onChange={(event) => setSessionForm((current) => ({ ...current, deliveryMode: event.target.value, roomId: '' }))} options={deliveryModeOptions} value={sessionForm.deliveryMode} />
        </Field>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <Field label="Bắt đầu">
          <input className={inputClass} onChange={(event) => setSessionForm((current) => ({ ...current, startTime: event.target.value, teacherId: '', roomId: '' }))} required type="time" value={sessionForm.startTime} />
        </Field>
        <Field label="Kết thúc">
          <input className={inputClass} onChange={(event) => setSessionForm((current) => ({ ...current, endTime: event.target.value, teacherId: '', roomId: '' }))} required type="time" value={sessionForm.endTime} />
        </Field>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <Field label="Giáo viên buổi học">
          <BrandedSelect disabled={availableResources.status === 'loading'} onChange={(event) => setSessionForm((current) => ({ ...current, teacherId: event.target.value }))} options={teacherOptions} placeholder={availableResources.status === 'loading' ? 'Đang tìm giáo viên rảnh...' : 'Chọn giáo viên'} searchable value={sessionForm.teacherId} />
        </Field>
        {sessionForm.deliveryMode === 'OFFLINE' ? (
          <Field label="Phòng học">
            <BrandedSelect disabled={availableResources.status === 'loading'} onChange={(event) => setSessionForm((current) => ({ ...current, roomId: event.target.value }))} options={roomOptions} placeholder={availableResources.status === 'loading' ? 'Đang tìm phòng trống...' : 'Chọn phòng trống'} searchable value={sessionForm.roomId} />
          </Field>
        ) : null}
      </div>
      {courseLessonOptions.length > 1 ? (
        <Field label="Bài học trong khóa học">
          <BrandedSelect
            onChange={(event) => setSessionForm((current) => ({ ...current, courseLessonId: event.target.value }))}
            options={courseLessonOptions}
            searchable
            value={sessionForm.courseLessonId}
          />
        </Field>
      ) : null}
    </>
  );

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
          {classIsMutable ? (
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

      {!sessionModalOpen && !replacement.open && !transfer.enrollment ? (
        <ManagementToast
          message={actionMessage}
          onClose={() => setActionMessage('')}
          tone={actionTone === 'success' ? 'success' : 'error'}
        />
      ) : null}

      <section className="rounded-xl border border-[#dfbfbd]/35 bg-white p-5 shadow-sm">
        <div className="flex flex-wrap gap-2">
          <Badge>{formatDeliveryMode(classroom.deliveryMode, classroom.deliveryModeLabel)}</Badge>
          <Badge>{formatOfferingStatus(classroom.classroomStatus)}</Badge>
          <Badge>{classroom.instructorLedCourse?.entryLevel || 'Chưa gắn level'}</Badge>
          <Badge>{classroom.instructorLedCourseTitle || 'Chưa gắn khóa học'}</Badge>
        </div>
        <h2 className="mt-3 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{classroom.title}</h2>
        <p className="mt-2 text-sm text-[#584140]">
          Khai giảng: <strong>{formatClassroomDate(classroom.startDate)}</strong>
          {' · '}
          Sĩ số: <strong>{classroom.enrolledCount ?? 0} học viên</strong>
          {' · '}
          Học phí: <strong>{formatClassroomPrice(classroom.tuitionFeeVnd ?? classroom.price ?? 0)}</strong>
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
          <section className="rounded-xl border border-[#dfbfbd]/35 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Giáo viên đứng lớp</p>
                <h3 className="mt-2 font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">
                  {classroom.primaryTeacherName || 'Chưa có giáo viên chính'}
                </h3>
              </div>
              {classIsMutable && classroom.primaryTeacherId ? (
                <button className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] bg-white px-4 py-2.5 text-sm font-extrabold text-[#730014] hover:bg-[#fff3f4]" onClick={openTeacherReplacement} type="button">
                  <UserRoundCheck className="h-4 w-4" />
                  Đổi giáo viên chính
                </button>
              ) : null}
            </div>
          </section>
          <CourseStructureOverview course={classroom.instructorLedCourse} />
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
                  {classIsMutable ? <div className="flex flex-wrap gap-2"><button className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-3 py-2 text-xs font-extrabold text-[#730014]" disabled={studentActionId === enrollment.id} onClick={() => setTransfer({ enrollment, targetId: '' })} type="button"><ArrowRightLeft className="h-3.5 w-3.5" />Chuyển lớp</button><button className="inline-flex items-center gap-2 rounded-xl border border-rose-200 px-3 py-2 text-xs font-extrabold text-rose-700" disabled={studentActionId === enrollment.id} onClick={() => handleRemoveStudent(enrollment)} type="button"><Trash2 className="h-3.5 w-3.5" />Loại khỏi lớp</button></div> : null}
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

      {replacement.open ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm">
          <section aria-modal="true" className="w-full max-w-xl rounded-2xl bg-white p-6 shadow-2xl" role="dialog">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Thay đổi nhân sự lớp</p>
                <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Đổi giáo viên chính</h2>
              </div>
              <button aria-label="Đóng" className="rounded-lg p-2 text-slate-400 hover:bg-slate-100" disabled={replacingTeacher} onClick={() => setReplacement({ open: false, teacherId: '', options: [], loading: false })} type="button"><X className="h-5 w-5" /></button>
            </div>
            {actionMessage && actionTone === 'error' ? <div className="mt-5 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700" role="alert">{actionMessage}</div> : null}
            <div className="mt-5">
              <Field label="Giáo viên mới">
                <BrandedSelect
                  disabled={replacement.loading || replacingTeacher}
                  onChange={(event) => setReplacement((current) => ({ ...current, teacherId: event.target.value }))}
                  options={replacement.options.map((item) => ({ label: item.fullName || item.email, value: String(item.id), description: item.email }))}
                  placeholder={replacement.loading ? 'Đang tải giáo viên...' : replacement.options.length ? 'Chọn giáo viên nhận lớp' : 'Không có giáo viên phù hợp'}
                  searchable
                  value={replacement.teacherId}
                />
              </Field>
            </div>
            <div className="mt-6 flex justify-end gap-2">
              <button className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-bold" disabled={replacingTeacher} onClick={() => setReplacement({ open: false, teacherId: '', options: [], loading: false })} type="button">Hủy</button>
              <button className="rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white disabled:opacity-50" disabled={!replacement.teacherId || replacingTeacher} onClick={handleReplaceTeacher} type="button">{replacingTeacher ? 'Đang thay đổi...' : 'Xác nhận đổi giáo viên'}</button>
            </div>
          </section>
        </div>
      ) : null}

      {transfer.enrollment ? <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4"><section className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl"><h2 className="text-xl font-black text-[#2b2828]">Chuyển lớp cho {transfer.enrollment.studentName || transfer.enrollment.studentEmail}</h2><p className="mt-2 text-sm leading-6 text-[#584140]">Học viên sẽ được chuyển sang lớp đã chọn.</p>{actionMessage && actionTone === 'error' ? <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700" role="alert">{actionMessage}</div> : null}<div className="mt-5"><BrandedSelect onChange={(event) => setTransfer((current) => ({ ...current, targetId: event.target.value }))} options={allClassrooms.filter((item) => String(item.id) !== String(id) && ['UPCOMING', 'ACTIVE'].includes(item.classroomStatus)).map((item) => ({ value: String(item.id), label: item.title, description: `${formatClassroomDate(item.startDate)} · ${item.primaryTeacherName || 'Chưa có giáo viên'}` }))} placeholder="Chọn lớp đích" searchable value={transfer.targetId} /></div><div className="mt-6 flex justify-end gap-2"><button className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-bold" onClick={() => setTransfer({ enrollment: null, targetId: '' })} type="button">Hủy</button><button className="rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white disabled:opacity-50" disabled={!transfer.targetId || studentActionId === transfer.enrollment.id} onClick={handleTransferStudent} type="button">Xác nhận chuyển lớp</button></div></section></div> : null}

      {sessionModalOpen && typeof document !== 'undefined' ? createPortal(
        <div className="fixed inset-0 z-[100] flex min-h-[100dvh] w-screen items-center justify-center bg-slate-950/55 p-4 backdrop-blur-sm">
          <form aria-labelledby="session-editor-title" aria-modal="true" className="flex max-h-[calc(100dvh-2rem)] w-full max-w-2xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl" onSubmit={handleSaveSession} role="dialog">
            <div className="flex items-start justify-between gap-4 border-b border-slate-100 px-6 py-5">
              <div>
                <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">{editingSessionId ? 'Điều chỉnh lịch học' : 'Tạo lịch học'}</p>
                <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]" id="session-editor-title">{editingSessionId ? 'Chỉnh sửa buổi học' : 'Thêm buổi học'}</h2>
              </div>
              <button aria-label="Đóng và giữ bản nháp" className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 disabled:opacity-50" disabled={creatingSession} onClick={dismissSessionModal} type="button"><X className="h-5 w-5" /></button>
            </div>
            <div className="min-h-0 flex-1 space-y-4 overflow-y-auto px-6 py-5">
              {actionMessage && actionTone === 'error' ? <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700" role="alert">{actionMessage}</div> : null}
              {sessionFormFields}
            </div>
            <div className="flex justify-end gap-2 border-t border-slate-100 px-6 py-4">
              <button className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-bold text-slate-600 disabled:opacity-50" disabled={creatingSession} onClick={cancelSessionModal} type="button">Hủy</button>
              <button className="rounded-xl bg-[#730014] px-5 py-2.5 text-sm font-extrabold text-white disabled:opacity-60" disabled={creatingSession} type="submit">{creatingSession ? 'Đang lưu...' : editingSessionId ? 'Lưu thay đổi' : 'Thêm buổi học'}</button>
            </div>
          </form>
        </div>,
        document.body,
      ) : null}

      {activeTab === 'schedule' ? (
        <div className="space-y-5">
          <section className="rounded-xl border border-[#dfbfbd]/35 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Lịch học đã tạo</h3>
              </div>
              {classIsMutable ? <button className="inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-extrabold text-white hover:bg-[#59000f]" onClick={openSessionCreator} type="button">
                <Plus className="h-4 w-4" />
                Thêm buổi học
              </button> : null}
            </div>
            {scheduledSessions.length ? (
              <div className="mt-5 overflow-x-auto rounded-xl border border-[#dfbfbd]/40">
                <table className="w-full min-w-[1120px] text-left text-sm">
                  <thead className="border-b border-[#dfbfbd]/30 bg-[#fbf3f4] text-[11px] font-extrabold uppercase tracking-wider text-slate-500">
                    <tr>
                      <th className="px-5 py-4">Ngày học</th>
                      <th className="px-5 py-4">Thời gian</th>
                      <th className="px-5 py-4">Hình thức</th>
                      <th className="px-5 py-4">Giáo viên</th>
                      <th className="min-w-64 px-5 py-4">Phòng học / Google Meet</th>
                      <th className="px-5 py-4 text-center">Trạng thái</th>
                      <th className="px-5 py-4 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#dfbfbd]/20">
                    {scheduledSessions.map((session) => (
                      <tr className="transition hover:bg-[#fffafb]" key={session.id}>
                        <td className="px-5 py-4">
                          <p className="font-bold text-[#0b1c30]">{formatClassroomDate(session.sessionDate)}</p>
                          <p className="mt-1 text-xs font-bold text-[#4b0009]">
                            {session.sessionNumber != null ? `Buổi ${session.sessionNumber} — ` : ''}{getClassroomSessionTitle(session, `Buổi học #${session.id}`)}
                          </p>
                          {getClassroomSessionUnitLabel(session) ? <p className="mt-1 text-xs text-slate-500">{getClassroomSessionUnitLabel(session)}</p> : null}
                        </td>
                        <td className="whitespace-nowrap px-5 py-4 font-semibold text-[#584140]">{session.startTime} - {session.endTime}</td>
                        <td className="whitespace-nowrap px-5 py-4">{formatDeliveryMode(session.effectiveDeliveryMode, session.effectiveDeliveryModeLabel)}</td>
                        <td className="px-5 py-4">
                          <p className="font-bold text-[#0b1c30]">{session.teacherName || 'Chưa chọn giáo viên'}</p>
                          {session.teacherId && String(session.teacherId) !== String(classroom.primaryTeacherId) ? <p className="mt-1 text-xs font-extrabold text-amber-700">Dạy thay</p> : null}
                        </td>
                        <td className="px-5 py-4">
                          {session.effectiveDeliveryMode === 'VIRTUAL' ? (
                            <VirtualMeetingStatus
                              onRetry={() => handleSyncVirtualMeeting(session)}
                              session={session}
                              syncing={syncingSessionId === session.id}
                            />
                          ) : (
                            <p className="text-sm text-slate-600">{session.roomName || classroom.offlineAddress || 'Chưa chọn phòng'}</p>
                          )}
                        </td>
                        <td className="whitespace-nowrap px-5 py-4 text-center"><SessionStatusBadge status={session.status} /></td>
                        <td className="px-5 py-4 text-right">
                          {classIsMutable && !['COMPLETED', 'CANCELLED'].includes(session.status) ? (
                            <button aria-label={`Chỉnh sửa buổi học ngày ${formatClassroomDate(session.sessionDate)}`} className="inline-flex items-center gap-1.5 rounded-lg border border-[#dfbfbd] px-3 py-2 text-xs font-bold text-[#730014] hover:bg-[#fff3f4]" onClick={() => openSessionEditor(session)} type="button">
                              <Pencil className="h-3.5 w-3.5" />
                              Sửa
                            </button>
                          ) : <span className="text-xs font-semibold text-slate-400">Đã khóa</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="mt-3 rounded-xl bg-[#fffafb] px-4 py-3 text-sm text-[#8b706e]">
                Chưa có buổi học. Thêm buổi khai giảng để học viên nhìn thấy lịch cụ thể.
              </p>
            )}
          </section>
        </div>
      ) : null}

      {activeTab === 'announcements' ? (
        <section className="space-y-5 rounded-xl border border-[#dfbfbd]/35 bg-white p-5 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Thông báo lớp học</h3>
              <p className="mt-1 text-sm text-[#8b706e]">Gửi cho học viên và giáo viên của lớp.</p>
            </div>
            <button className="inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-extrabold text-white transition hover:bg-[#59000f]" onClick={() => setAnnouncementComposerOpen((open) => !open)} type="button">
              <Plus className="h-4 w-4" />
              {announcementComposerOpen ? 'Đóng soạn thông báo' : 'Tạo thông báo'}
            </button>
          </div>

          {announcementComposerOpen ? (
            <div className="space-y-4 rounded-xl border border-[#dfbfbd]/60 bg-[#fffafb] p-4">
              <input className={inputClass} maxLength={220} onChange={(event) => setAnnouncementForm((current) => ({ ...current, title: event.target.value }))} placeholder="Tiêu đề thông báo" value={announcementForm.title} />
              <textarea className={`${inputClass} min-h-32`} onChange={(event) => setAnnouncementForm((current) => ({ ...current, content: event.target.value }))} placeholder="Nội dung thông báo" value={announcementForm.content} />
              <div className="flex justify-end gap-2">
                <button className="rounded-xl border border-[#dfbfbd] px-4 py-2.5 text-sm font-bold text-[#584140] hover:bg-white" disabled={savingAnnouncement} onClick={() => setAnnouncementComposerOpen(false)} type="button">Hủy</button>
                <button className="rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white hover:bg-[#59000f] disabled:cursor-wait disabled:opacity-60" disabled={savingAnnouncement} onClick={createAnnouncement} type="button">{savingAnnouncement ? 'Đang gửi...' : 'Gửi thông báo'}</button>
              </div>
            </div>
          ) : null}

          {announcements.length ? (
            <div className="space-y-3">
              {announcements.map((announcement) => (
                <article className="rounded-xl border border-[#dfbfbd]/35 p-4" key={announcement.id}>
                  <div className="flex items-start justify-between gap-3">
                    <h4 className="flex items-center gap-2 font-['Manrope'] text-base font-extrabold text-[#0b1c30]"><Bell className="h-4 w-4 text-[#730014]" />{announcement.title}</h4>
                    <span className="whitespace-nowrap text-xs text-[#8b706e]">{formatClassroomDate(announcement.createdAt)}</span>
                  </div>
                  <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-[#584140]">{announcement.content || announcement.body}</p>
                </article>
              ))}
            </div>
          ) : <ClassroomEmptyState title="Chưa có thông báo" description="Tạo thông báo đầu tiên cho lớp học." />}
        </section>
      ) : null}

      {activeTab === 'recordings' ? <StaffRecordingsPage classroomId={id} /> : null}
    </div>
  );
}

function VirtualMeetingStatus({ session, syncing, onRetry }) {
  const synced = session.googleMeetStatus === 'READY' && Boolean(session.googleMeetUrl);
  const terminal = ['COMPLETED', 'CANCELLED'].includes(session.status);
  const statusLabel = {
    NOT_CREATED: 'Chưa tạo phòng Google Meet',
    CREATING: 'Đang tạo phòng Google Meet',
    FAILED: 'Tạo phòng Google Meet thất bại',
    READY: 'Phòng Google Meet đã sẵn sàng',
  }[session.googleMeetStatus] || 'Chưa tạo phòng Google Meet';

  if (synced) {
    return (
      <div className="min-w-0">
        <p className="inline-flex items-center gap-1.5 font-bold text-emerald-700">
          <Video className="h-4 w-4 shrink-0" />
          {statusLabel}
        </p>
        {session.googleMeetSpaceName ? (
          <p className="mt-1 text-xs text-slate-500">Mã phòng: {session.googleMeetSpaceName}</p>
        ) : null}
        <a
          className="mt-1 inline-flex text-xs font-extrabold text-[#730014] underline underline-offset-2"
          href={session.googleMeetUrl}
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
      <p className={`font-bold ${session.googleMeetStatus === 'FAILED' ? 'text-red-700' : 'text-amber-700'}`}>
        {statusLabel}
      </p>
      {session.googleMeetSyncError ? (
        <p className="mt-1 line-clamp-2 text-xs leading-5 text-slate-500" title={session.googleMeetSyncError}>
          {session.googleMeetSyncError}
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

function SessionStatusBadge({ status }) {
  const tone = {
    CANCELLED: 'bg-rose-50 text-rose-700',
    COMPLETED: 'bg-emerald-50 text-emerald-700',
    IN_PROGRESS: 'bg-blue-50 text-blue-700',
    MAKEUP: 'bg-amber-50 text-amber-800',
    OPEN: 'bg-blue-50 text-blue-700',
    RESCHEDULED: 'bg-amber-50 text-amber-800',
    SCHEDULED: 'bg-slate-100 text-slate-700',
  }[status] || 'bg-slate-100 text-slate-700';
  return <span className={`inline-flex rounded-full px-3 py-1 text-xs font-extrabold ${tone}`}>{formatSessionStatus(status)}</span>;
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

function CourseStructureOverview({ course }) {
  const [expandedUnitIds, setExpandedUnitIds] = useState(new Set());
  if (!course) {
    return (
      <section className="rounded-xl border border-amber-200 bg-amber-50 p-5 text-sm font-semibold text-amber-800">
        Lớp này chưa được gắn khóa học. Hãy cập nhật lớp từ trang mở lớp để chọn khóa học theo band/target.
      </section>
    );
  }
  const units = course.units || [];
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
          <p className="text-xs font-bold uppercase tracking-wider text-[#8b706e]">Khóa học đang dùng</p>
          <h3 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{course.title}</h3>
          <p className="mt-1 text-sm text-[#584140]">
            {[course.code, course.examCategory, course.targetBand ? `Band ${course.targetBand}` : null, course.targetScore ? `Target ${course.targetScore}` : null].filter(Boolean).join(' · ')}
          </p>
        </div>
        <Badge>{units.length} Unit · {course.totalLessons ?? course.totalSessions ?? 0} bài học</Badge>
      </div>
      {course.outcomes ? <p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-[#584140]">{course.outcomes}</p> : null}
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
                  {unit.description ? <RichTextHtml className="text-sm leading-6 text-[#584140]" value={unit.description} /> : null}
                  {unit.lessons?.length ? (
                    <div className="space-y-2 rounded-lg border border-slate-100 bg-slate-50 p-3">
                      {unit.lessons.map((plan) => (
                        <div className="border-l-2 border-[#dfbfbd] pl-3" key={plan.id}>
                          <p className="text-[10px] font-extrabold uppercase tracking-wider text-[#730014]">Bài {plan.sessionNumber}</p>
                          <p className="text-sm font-bold text-[#2b2828]">{plan.title}</p>
                        </div>
                      ))}
                    </div>
                  ) : null}
                  <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                    <CourseUnitResources label="Học liệu" items={unit.materials} />
                    <CourseUnitResources label="Bài tập" items={unit.exercises} />
                    <CourseUnitResources label="Đề đánh giá" items={unit.assessments} />
                    <CourseUnitResources label="Flashcard" items={unit.flashcards} />
                  </div>
                </div>
              ) : null}
            </article>
          ))}
        </div>
      ) : (
        <p className="mt-4 rounded-xl bg-slate-50 px-4 py-3 text-sm text-slate-600">Khóa học này chưa có Unit.</p>
      )}
    </section>
  );
}

function CourseUnitResources({ label, items = [] }) {
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
