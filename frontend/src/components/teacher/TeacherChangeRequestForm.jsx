import { useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  AlertTriangle,
  Check,
  Loader2,
  Send,
  Settings,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import BrandedSelect from '../ui/BrandedSelect';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatClassroomDate,
  formatClassroomTime,
} from '../../utils/classroomHelpers';
import {
  buildSlotTimes,
  CLASSROOM_TIME_SLOTS,
  getSessionSlotIndex,
  todayDateInputValue,
} from '../../utils/classroomScheduleSlots';

const REQUEST_TYPE_OPTIONS = [
  { label: 'Đổi lịch buổi học', value: 'RESCHEDULE_SESSION' },
  { label: 'Tạo buổi học bù', value: 'CREATE_MAKEUP_SESSION' },
  { label: 'Đổi phòng học', value: 'CHANGE_ROOM' },
  { label: 'Đổi giáo viên', value: 'CHANGE_TEACHER' },
];

const emptyForm = {
  type: 'RESCHEDULE_SESSION',
  sessionId: '',
  reason: '',
  newDate: todayDateInputValue(),
  slotIndex: '',
  roomId: '',
  teacherId: '',
};

const buildScheduleValues = (session, slot, date, roomId) => {
  const times = buildSlotTimes(slot);
  return JSON.stringify({
    sessionDate: date,
    startTime: times.startTime,
    endTime: times.endTime,
    teacherId: session?.teacherId ?? null,
    roomId: roomId ? Number(roomId) : session?.roomId ?? null,
  });
};

export default function TeacherChangeRequestForm({
  classroomId,
  classroom,
  sessions,
  onMessage,
  onSubmitted,
}) {
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);
  const [checkingSlots, setCheckingSlots] = useState(false);
  const [slotStatus, setSlotStatus] = useState({});
  const [availableRooms, setAvailableRooms] = useState([]);
  const [availableTeachers, setAvailableTeachers] = useState([]);
  const [loadingOptions, setLoadingOptions] = useState(false);

  const isVirtual = classroom?.deliveryMode === 'VIRTUAL';
  const requiresRoom = !isVirtual;
  const isMakeup = form.type === 'CREATE_MAKEUP_SESSION';
  const isScheduleRequest = form.type === 'RESCHEDULE_SESSION' || isMakeup;

  const eligibleSessions = useMemo(
    () => (sessions || []).filter((session) => session.status !== 'COMPLETED' && session.status !== 'CANCELLED'),
    [sessions],
  );

  // Makeup may reference a completed/cancelled source session as context only.
  const selectableSessions = isMakeup ? (sessions || []) : eligibleSessions;

  const selectedSession = useMemo(
    () => selectableSessions.find((session) => String(session.id) === form.sessionId) || null,
    [selectableSessions, form.sessionId],
  );

  const sessionOptions = useMemo(
    () => selectableSessions.map((session) => ({
      label: `Buổi #${session.id}: ${formatClassroomDate(session.sessionDate)} (${formatClassroomTime(session.startTime)})`,
      value: String(session.id),
    })),
    [selectableSessions],
  );

  const currentSlotIndex = selectedSession ? getSessionSlotIndex(selectedSession.startTime) : -1;

  useEffect(() => {
    if (!isScheduleRequest || !form.sessionId || !form.newDate) {
      setSlotStatus({});
      return undefined;
    }

    let active = true;
    setCheckingSlots(true);
    setForm((current) => ({ ...current, slotIndex: '', roomId: '' }));

    const checkSlots = async () => {
      try {
        const results = await Promise.all(
          CLASSROOM_TIME_SLOTS.map(async (slot) => {
            try {
              const result = await classroomApi.checkTeacherChangeConflict({
                requestType: form.type,
                classroomOfferingId: Number(classroomId),
                targetSessionId: Number(form.sessionId),
                newValuesJson: buildScheduleValues(selectedSession, slot, form.newDate, ''),
                reason: 'Kiểm tra trùng lịch',
              });
              return { available: !result?.hasBlockingConflict };
            } catch {
              return { available: false };
            }
          }),
        );
        if (!active) return;
        const nextStatus = {};
        results.forEach((result, index) => {
          nextStatus[index] = result;
        });
        setSlotStatus(nextStatus);

        const preferredIndex =
          (nextStatus[currentSlotIndex]?.available ? currentSlotIndex : null)
          ?? results.findIndex((result) => result.available);
        if (preferredIndex >= 0) {
          setForm((current) => ({ ...current, slotIndex: String(preferredIndex) }));
        }
      } finally {
        if (active) setCheckingSlots(false);
      }
    };

    checkSlots();

    return () => {
      active = false;
    };
  }, [form.type, form.sessionId, form.newDate, classroomId, selectedSession, currentSlotIndex, isScheduleRequest]);

  useEffect(() => {
    const shouldLoadRooms = form.type === 'CHANGE_ROOM'
      || (isScheduleRequest && requiresRoom && form.sessionId && form.slotIndex !== '');

    if (!shouldLoadRooms || !form.sessionId) {
      setAvailableRooms([]);
      return undefined;
    }

    let active = true;
    setLoadingOptions(true);
    setForm((current) => ({ ...current, roomId: '' }));

    const params = {};
    if (isScheduleRequest && form.newDate && form.slotIndex !== '') {
      const slot = CLASSROOM_TIME_SLOTS[Number(form.slotIndex)];
      const times = buildSlotTimes(slot);
      params.sessionDate = form.newDate;
      params.startTime = times.startTime;
      params.endTime = times.endTime;
    }

    const loadAvailableRooms = async () => {
      try {
        const rooms = await classroomApi.getAvailableRooms(form.sessionId, params);
        if (!active) return;
        setAvailableRooms(rooms);
      } catch {
        if (active) setAvailableRooms([]);
      } finally {
        if (active) setLoadingOptions(false);
      }
    };

    loadAvailableRooms();

    return () => {
      active = false;
    };
  }, [form.type, form.sessionId, form.newDate, form.slotIndex, requiresRoom, isScheduleRequest]);

  useEffect(() => {
    if (form.type !== 'CHANGE_TEACHER' || !form.sessionId) {
      setAvailableTeachers([]);
      return undefined;
    }

    let active = true;
    setLoadingOptions(true);
    setForm((current) => ({ ...current, teacherId: '' }));

    const loadAvailableTeachers = async () => {
      try {
        const teachers = await classroomApi.getAvailableTeachers(form.sessionId);
        if (!active) return;
        setAvailableTeachers(teachers);
      } catch {
        if (active) setAvailableTeachers([]);
      } finally {
        if (active) setLoadingOptions(false);
      }
    };

    loadAvailableTeachers();

    return () => {
      active = false;
    };
  }, [form.type, form.sessionId]);

  const availableSlotOptions = CLASSROOM_TIME_SLOTS
    .map((slot, index) => ({ slot, index }))
    .filter(({ index }) => slotStatus[index]?.available)
    .map(({ slot, index }) => ({ label: slot.label, value: String(index) }));

  const roomOptions = availableRooms.map((room) => ({
    label: room.capacity
      ? `${room.name} · Sức chứa ${room.capacity}`
      : room.name,
    value: String(room.id),
  }));

  const teacherOptions = availableTeachers.map((teacher) => ({
    label: teacher.fullName || teacher.email || `Giáo viên #${teacher.id}`,
    value: String(teacher.id),
  }));

  const isSameSchedule = isScheduleRequest
    && selectedSession
    && form.newDate === selectedSession.sessionDate
    && Number(form.slotIndex) === currentSlotIndex;

  const canSubmit = (() => {
    if (!form.sessionId || !form.reason.trim() || submitting) return false;
    if (isScheduleRequest) {
      if (!form.newDate || form.slotIndex === '' || checkingSlots || isSameSchedule) return false;
      if (requiresRoom && !form.roomId) return false;
      return Boolean(slotStatus[Number(form.slotIndex)]?.available);
    }
    if (form.type === 'CHANGE_ROOM') {
      return Boolean(form.roomId) && !loadingOptions;
    }
    if (form.type === 'CHANGE_TEACHER') {
      return Boolean(form.teacherId) && !loadingOptions;
    }
    return false;
  })();

  const buildNewValuesJson = () => {
    if (isScheduleRequest) {
      const slot = CLASSROOM_TIME_SLOTS[Number(form.slotIndex)];
      return buildScheduleValues(selectedSession, slot, form.newDate, form.roomId);
    }
    if (form.type === 'CHANGE_ROOM') {
      return JSON.stringify({ roomId: Number(form.roomId) });
    }
    return JSON.stringify({ teacherId: Number(form.teacherId) });
  };

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setSubmitting(true);
    onMessage?.('');
    try {
      await classroomApi.createChangeRequest({
        requestType: form.type,
        classroomOfferingId: Number(classroomId),
        targetSessionId: Number(form.sessionId),
        newValuesJson: buildNewValuesJson(),
        reason: form.reason.trim(),
      });
      onMessage?.('Đã gửi yêu cầu thay đổi. Điều phối đào tạo sẽ xem xét và phê duyệt.');
      setForm(emptyForm);
      onSubmitted?.();
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể gửi yêu cầu thay đổi.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-start gap-4">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-[#730014] flex-shrink-0">
          <Settings className="h-6 w-6" />
        </div>
        <div>
          <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">Gửi yêu cầu thay đổi</h2>
          <p className="mt-1 text-xs text-[#8b706e] leading-5">
            Chọn buổi học, lịch/phòng/giáo viên mới phù hợp. Hệ thống chỉ hiển thị khung giờ, phòng và giáo viên
            đang rảnh, không trùng lớp khác.
          </p>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <div className="space-y-2">
          <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Loại yêu cầu</label>
          <BrandedSelect
            onChange={(event) => setForm((current) => ({
              ...current,
              type: event.target.value,
              sessionId: '',
              newDate: todayDateInputValue(),
              slotIndex: '',
              roomId: '',
              teacherId: '',
            }))}
            options={REQUEST_TYPE_OPTIONS}
            value={form.type}
          />
        </div>

        <div className="space-y-2">
          <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">
            {isMakeup ? 'Buổi học cần học bù *' : 'Buổi học áp dụng *'}
          </label>
          <BrandedSelect
            onChange={(event) => setForm((current) => ({
              ...current,
              sessionId: event.target.value,
              newDate: isMakeup
                ? todayDateInputValue()
                : selectableSessions.find((session) => String(session.id) === event.target.value)?.sessionDate || current.newDate,
              slotIndex: '',
              roomId: '',
              teacherId: '',
            }))}
            options={sessionOptions}
            placeholder="Chọn buổi học"
            value={form.sessionId}
          />
        </div>
      </div>

      {isScheduleRequest && form.sessionId ? (
        <div className="grid gap-4 md:grid-cols-2">
          <div className="space-y-2">
            <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">
              {isMakeup ? 'Ngày học bù *' : 'Ngày học mới *'}
            </label>
            <input
              className="w-full rounded-xl border border-[#dfbfbd]/60 bg-white px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014]"
              min={todayDateInputValue()}
              onChange={(event) => setForm((current) => ({
                ...current,
                newDate: event.target.value,
                slotIndex: '',
                roomId: '',
              }))}
              type="date"
              value={form.newDate}
            />
          </div>

          <div className="space-y-2">
            <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">
              {isMakeup ? 'Khung giờ học bù *' : 'Khung giờ mới *'}
            </label>
            {checkingSlots ? (
              <div className="flex items-center gap-2 rounded-xl border border-gray-100 bg-gray-50/50 px-4 py-3 text-xs font-semibold text-[#8b706e]">
                <Loader2 className="h-4 w-4 animate-spin" />
                Đang kiểm tra khung giờ trống...
              </div>
            ) : availableSlotOptions.length === 0 ? (
              <div className="flex items-start gap-2 rounded-xl border border-rose-100 bg-rose-50/40 px-4 py-3 text-xs font-semibold text-rose-800">
                <AlertTriangle className="h-4 w-4 flex-shrink-0" />
                Ngày này không còn khung giờ trống cho học viên và giáo viên của lớp.
              </div>
            ) : (
              <BrandedSelect
                onChange={(event) => setForm((current) => ({ ...current, slotIndex: event.target.value, roomId: '' }))}
                options={availableSlotOptions}
                placeholder="Chọn khung giờ"
                value={form.slotIndex}
              />
            )}
          </div>

          {requiresRoom && form.slotIndex !== '' ? (
            <div className="space-y-2 md:col-span-2">
              <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Phòng học trống *</label>
              {loadingOptions ? (
                <div className="flex items-center gap-2 rounded-xl border border-gray-100 bg-gray-50/50 px-4 py-3 text-xs font-semibold text-[#8b706e]">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Đang tìm phòng trống...
                </div>
              ) : roomOptions.length === 0 ? (
                <div className="flex items-start gap-2 rounded-xl border border-amber-100 bg-amber-50/40 px-4 py-3 text-xs font-semibold text-amber-800">
                  <AlertCircle className="h-4 w-4 flex-shrink-0" />
                  Không có phòng trống trong khung giờ đã chọn. Hãy chọn ngày hoặc khung giờ khác.
                </div>
              ) : (
                <BrandedSelect
                  onChange={(event) => setForm((current) => ({ ...current, roomId: event.target.value }))}
                  options={roomOptions}
                  placeholder="Chọn phòng học"
                  value={form.roomId}
                />
              )}
            </div>
          ) : null}
        </div>
      ) : null}

      {form.type === 'CHANGE_ROOM' && form.sessionId ? (
        <div className="space-y-2">
          <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Phòng học trống *</label>
          <p className="text-xs text-[#8b706e]">
            Lọc theo lịch buổi {formatClassroomDate(selectedSession?.sessionDate)} · {formatClassroomTime(selectedSession?.startTime)}–{formatClassroomTime(selectedSession?.endTime)}.
          </p>
          {loadingOptions ? (
            <div className="flex items-center gap-2 rounded-xl border border-gray-100 bg-gray-50/50 px-4 py-3 text-xs font-semibold text-[#8b706e]">
              <Loader2 className="h-4 w-4 animate-spin" />
              Đang tìm phòng trống...
            </div>
          ) : roomOptions.length === 0 ? (
            <div className="flex items-start gap-2 rounded-xl border border-amber-100 bg-amber-50/40 px-4 py-3 text-xs font-semibold text-amber-800">
              <AlertCircle className="h-4 w-4 flex-shrink-0" />
              Không có phòng trống trong khung giờ của buổi học này.
            </div>
          ) : (
            <BrandedSelect
              onChange={(event) => setForm((current) => ({ ...current, roomId: event.target.value }))}
              options={roomOptions}
              placeholder="Chọn phòng học mới"
              value={form.roomId}
            />
          )}
        </div>
      ) : null}

      {form.type === 'CHANGE_TEACHER' && form.sessionId ? (
        <div className="space-y-2">
          <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Giáo viên rảnh *</label>
          <p className="text-xs text-[#8b706e]">
            Chỉ hiển thị giáo viên không dạy lớp nào khác trong khung giờ buổi học này.
          </p>
          {loadingOptions ? (
            <div className="flex items-center gap-2 rounded-xl border border-gray-100 bg-gray-50/50 px-4 py-3 text-xs font-semibold text-[#8b706e]">
              <Loader2 className="h-4 w-4 animate-spin" />
              Đang tìm giáo viên rảnh...
            </div>
          ) : teacherOptions.length === 0 ? (
            <div className="flex items-start gap-2 rounded-xl border border-amber-100 bg-amber-50/40 px-4 py-3 text-xs font-semibold text-amber-800">
              <AlertCircle className="h-4 w-4 flex-shrink-0" />
              Không có giáo viên rảnh trong khung giờ của buổi học này.
            </div>
          ) : (
            <BrandedSelect
              onChange={(event) => setForm((current) => ({ ...current, teacherId: event.target.value }))}
              options={teacherOptions}
              placeholder="Chọn giáo viên thay thế"
              value={form.teacherId}
            />
          )}
        </div>
      ) : null}

      <div className="space-y-2">
        <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Lý do chi tiết *</label>
        <textarea
          className="min-h-[120px] w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white focus:ring-2 focus:ring-[#730014]/5"
          onChange={(event) => setForm((current) => ({ ...current, reason: event.target.value }))}
          placeholder="Mô tả lý do thay đổi để điều phối đào tạo xem xét..."
          value={form.reason}
        />
      </div>

      {isSameSchedule ? (
        <p className="text-xs font-semibold text-[#8b706e]">
          Lịch mới trùng với lịch hiện tại. Hãy chọn ngày hoặc khung giờ khác.
        </p>
      ) : canSubmit ? (
        <div className="flex items-center gap-2 rounded-xl border border-emerald-100 bg-emerald-50/50 px-4 py-3 text-xs font-semibold text-emerald-800">
          <Check className="h-4 w-4" />
          Lựa chọn hợp lệ. Có thể gửi yêu cầu phê duyệt.
        </div>
      ) : null}

      <div className="flex justify-end">
        <button
          className="inline-flex items-center gap-1.5 rounded-2xl bg-[#4b0009] px-6 py-3.5 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
          disabled={!canSubmit}
          onClick={handleSubmit}
          type="button"
        >
          {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
          Gửi yêu cầu phê duyệt
        </button>
      </div>
    </div>
  );
}
