import { useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertCircle,
  CalendarDays,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Clock3,
  Edit3,
  Loader2,
  Plus,
  PlusCircle,
  ArrowUp,
  ArrowDown,
  GripVertical,
  RefreshCw,
  Send,
  Trash2,
  X,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { curriculumApi } from '../../api/curriculumApi';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import BrandedSelect from '../../components/ui/BrandedSelect';
import VietnameseDateInput from '../../components/ui/VietnameseDateInput';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import ManagementToast from '../../components/ui/ManagementToast';
import { ERROR_NOTICE_CLASS, FIELD_CLASS, PRIMARY_BUTTON_CLASS, SECONDARY_BUTTON_CLASS } from '../../utils/formStyles';
import { formatClassroomDate } from '../../utils/classroomHelpers';
import { getClassroomErrorMessage, getConflictSummary } from '../../utils/classroomErrorMessages';

const statusTabs = [
  { label: 'Bản nháp', value: 'DRAFT' },
  { label: 'Chờ duyệt', value: 'PENDING_APPROVAL' },
  { label: 'Bị từ chối', value: 'REJECTED' },
  { label: 'Đã duyệt', value: 'APPROVED' },
];

const weekdayOptions = [
  ['MONDAY', 'T2'], ['TUESDAY', 'T3'], ['WEDNESDAY', 'T4'], ['THURSDAY', 'T5'],
  ['FRIDAY', 'T6'], ['SATURDAY', 'T7'], ['SUNDAY', 'CN'],
];

const SPECIAL_PRESETS = [
  { code: 'EXAM', label: 'Kiểm tra / Đánh giá', content: 'Kiểm tra / Đánh giá' },
  { code: 'ORIENTATION', label: 'Định hướng / Khai giảng', content: 'Định hướng / Khai giảng' },
  { code: 'MAKEUP', label: 'Học bù', content: 'Học bù' },
  { code: 'REVIEW', label: 'Chữa đề & Ôn tập', content: 'Chữa đề & Ôn tập' },
  { code: 'CUSTOM', label: 'Buổi học đặc biệt khác', content: 'Buổi học đặc biệt khác' },
];

const emptyForm = {
  title: '',
  courseOfferingId: '',
  deliveryType: 'OFFLINE',
  enrollmentRequestIds: [],
  capacity: '20',
  plannedStartDate: '',
  plannedEndDate: '',
  weekdays: [],
  sessionStartTime: '18:30',
  sessionEndTime: '20:30',
  primaryTeacherId: '',
  roomId: '',
  scheduleItems: [],
  note: '',
};

const toLocalDateKey = (date = new Date()) => [
  date.getFullYear(),
  String(date.getMonth() + 1).padStart(2, '0'),
  String(date.getDate()).padStart(2, '0'),
].join('-');

const buildProposalPayload = (form) => {
  const scheduleItems = form.scheduleItems || [];
  const lastSessionDate = scheduleItems
    .map((item) => item.sessionDate)
    .filter(Boolean)
    .sort()
    .at(-1);

  return {
    ...form,
    title: form.title.trim(),
    courseOfferingId: form.courseOfferingId ? Number(form.courseOfferingId) : null,
    deliveryType: form.deliveryType || 'OFFLINE',
    capacity: form.capacity === '' ? null : Number(form.capacity),
    plannedStartDate: form.plannedStartDate,
    endDate: form.plannedEndDate || lastSessionDate || form.plannedStartDate,
    primaryTeacherId: form.primaryTeacherId ? Number(form.primaryTeacherId) : null,
    roomId: form.deliveryType === 'VIRTUAL' ? null : (form.roomId ? Number(form.roomId) : null),
    scheduleItems: scheduleItems.map((item, index) => ({
      sequenceNumber: index + 1,
      sessionDate: item.sessionDate,
      startTime: item.startTime || form.sessionStartTime,
      endTime: item.endTime || form.sessionEndTime,
      deliveryModeOverride: item.deliveryModeOverride || null,
      teacherId: item.teacherId ? Number(item.teacherId) : null,
      roomId: (item.deliveryModeOverride || form.deliveryType) === 'VIRTUAL' ? null : (item.roomId ? Number(item.roomId) : null),
      courseLessonId: item.courseLessonId ? Number(item.courseLessonId) : null,
      sessionContent: item.courseLessonId ? null : (item.sessionContent?.trim() || 'Buổi học đặc biệt'),
      note: item.note?.trim() || null,
    })),
    note: form.note.trim() || null,
  };
};

export default function StaffClassroomProposalsPage() {
  const [status, setStatus] = useState('DRAFT');
  const [proposals, setProposals] = useState([]);
  const [courseOfferings, setCourseOfferings] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingProposal, setEditingProposal] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const loadRequestId = useRef(0);

  const load = async (requestedStatus = status) => {
    const requestId = loadRequestId.current + 1;
    loadRequestId.current = requestId;
    setLoading(true);
    setError('');
    try {
      const [proposalData, offeringData] = await Promise.all([
        enrollmentRequestApi.listStaffClassroomProposals(requestedStatus),
        classroomApi.getStaffPrograms(),
      ]);
      if (requestId !== loadRequestId.current) return;
      setProposals(proposalData);
      setCourseOfferings(offeringData);
    } catch (err) {
      if (requestId !== loadRequestId.current) return;
      setError(err?.response?.data?.message || 'Không thể tải danh sách đề xuất lớp.');
    } finally {
      if (requestId === loadRequestId.current) setLoading(false);
    }
  };

  useEffect(() => {
    load(status);
  }, [status]);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    proposals,
    8,
    status,
  );

  const openCreate = () => {
    setEditingProposal(null);
    setForm(emptyForm);
    setError('');
    setModalOpen(true);
  };

  const openEdit = (proposal) => {
    setEditingProposal(proposal);
    setForm({
      title: proposal.title,
      courseOfferingId: String(proposal.courseOfferingId),
      deliveryType: proposal.deliveryType || 'OFFLINE',
      enrollmentRequestIds: (proposal.members || []).map((item) => item.enrollmentRequestId),
      capacity: String(proposal.capacity || '20'),
      plannedStartDate: proposal.plannedStartDate,
      plannedEndDate: proposal.endDate || proposal.plannedEndDate,
      weekdays: proposal.weekdays || [],
      sessionStartTime: String(proposal.sessionStartTime || '').slice(0, 5) || '18:30',
      sessionEndTime: String(proposal.sessionEndTime || '').slice(0, 5) || '20:30',
      primaryTeacherId: proposal.primaryTeacherId ? String(proposal.primaryTeacherId) : '',
      roomId: proposal.roomId ? String(proposal.roomId) : '',
      scheduleItems: (proposal.scheduleItems || []).map((item, idx) => ({
        sequenceNumber: idx + 1,
        sessionDate: item.sessionDate,
        startTime: String(item.startTime || '').slice(0, 5) || '18:30',
        endTime: String(item.endTime || '').slice(0, 5) || '20:30',
        deliveryModeOverride: item.deliveryModeOverride || null,
        teacherId: item.teacherId ? String(item.teacherId) : '',
        roomId: item.roomId ? String(item.roomId) : '',
        courseLessonId: item.courseLessonId ? String(item.courseLessonId) : null,
        courseLessonTitle: item.courseLessonTitle || '',
        courseUnitTitle: item.courseUnitTitle || '',
        sessionContent: item.sessionContent || '',
        note: item.note || '',
      })),
      note: proposal.staffNote || '',
    });
    setError('');
    setModalOpen(true);
  };

  const updateForm = (patch) => setForm((current) => ({ ...current, ...patch }));

  const toggleWeekday = (weekday) => setForm((current) => ({
    ...current,
    weekdays: current.weekdays.includes(weekday)
      ? current.weekdays.filter((item) => item !== weekday)
      : [...current.weekdays, weekday],
  }));

  const save = async () => {
    if (!form.title.trim() || !form.courseOfferingId) {
      setError('Cần nhập tên lớp và chọn khóa học.');
      return;
    }
    if (!form.plannedStartDate || !form.weekdays.length) {
      setError('Cần nhập ngày bắt đầu và ít nhất một thứ học trong tuần.');
      return;
    }
    if (!Number.isInteger(Number(form.capacity)) || Number(form.capacity) < 1) {
      setError('Sức chứa phải là số nguyên lớn hơn 0.');
      return;
    }
    if (form.plannedStartDate < toLocalDateKey()) {
      setError('Ngày bắt đầu lớp không được ở trong quá khứ.');
      return;
    }
    if (!form.sessionStartTime || !form.sessionEndTime || form.sessionEndTime <= form.sessionStartTime) {
      setError('Giờ kết thúc phải sau giờ bắt đầu.');
      return;
    }
    setWorking(true);
    setError('');
    try {
      const payload = buildProposalPayload(form);
      const saved = editingProposal
        ? await enrollmentRequestApi.updateClassroomProposal(editingProposal.id, payload)
        : await enrollmentRequestApi.createClassroomProposal(payload);
      setSuccess(editingProposal ? `Đã cập nhật ${saved.proposalCode}.` : `Đã tạo bản nháp ${saved.proposalCode}.`);
      setModalOpen(false);
      setEditingProposal(null);
      await load(status);
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể lưu đề xuất lớp.'));
    } finally {
      setWorking(false);
    }
  };

  const submit = async (proposal) => {
    setWorking(true);
    setError('');
    try {
      const submitted = await enrollmentRequestApi.submitClassroomProposal(proposal.id);
      setProposals((current) => current.filter((item) => item.id !== submitted.id));
      setSuccess(`${submitted.proposalCode} đã được gửi xét duyệt.`);
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể gửi đề xuất để duyệt.'));
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-5">
      <section className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex gap-1.5 overflow-x-auto">
          {statusTabs.map((tab) => (
            <button
              className={`shrink-0 rounded-xl px-4 py-2.5 text-xs font-extrabold transition ${status === tab.value
                  ? 'bg-[#4b0009] text-white shadow-sm'
                  : 'bg-slate-50 text-slate-600 hover:bg-slate-100'
                }`}
              key={tab.value}
              onClick={() => setStatus(tab.value)}
              type="button"
            >
              {tab.label}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-3">
          <button
            className="inline-flex h-11 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-xs font-bold text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
            disabled={loading}
            onClick={() => load(status)}
            type="button"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Tải lại
          </button>
          <button
            className="inline-flex h-11 items-center gap-2 rounded-xl bg-[#4b0009] px-4 text-xs font-extrabold text-white transition hover:bg-[#730014] shadow-sm active:scale-95"
            onClick={openCreate}
            type="button"
          >
            <Plus className="h-4 w-4" />
            Tạo đề xuất
          </button>
        </div>
      </section>

      {!modalOpen ? <ManagementToast message={error} onClose={() => setError('')} /> : null}
      <ManagementToast message={success} onClose={() => setSuccess('')} tone="success" title="Đã cập nhật đề xuất" />
      {loading ? <div className="grid gap-4 xl:grid-cols-2">{Array.from({ length: 4 }).map((_, index) => <div className="h-64 animate-pulse rounded-2xl bg-slate-100" key={index} />)}</div> : null}
      {!loading && !pageItems.length ? <div className="flex min-h-[360px] flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white text-center"><CalendarDays className="h-12 w-12 text-slate-300" /><h2 className="mt-4 text-xl font-black text-[#0b1c30]">Không có đề xuất trong tab này</h2><p className="mt-2 text-sm text-slate-500">Draft, Pending và Rejected được tách riêng nên một đề xuất không xuất hiện trùng tab.</p></div> : null}
      {!loading && pageItems.length ? (
        <div className="space-y-4">
          <div className="grid gap-4 xl:grid-cols-2">
            {pageItems.map((proposal) => (
              <ProposalCard
                key={proposal.id}
                onEdit={() => openEdit(proposal)}
                onSubmit={() => submit(proposal)}
                proposal={proposal}
                working={working}
              />
            ))}
          </div>
          {totalPages > 1 ? (
            <div className="rounded-xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
              <Pagination onChange={setPage} page={page} pageSize={8} totalItems={totalItems} totalPages={totalPages} />
            </div>
          ) : null}
        </div>
      ) : null}

      {modalOpen ? (
        <ProposalModal
          courseOfferings={courseOfferings}
          editing={Boolean(editingProposal)}
          editingProposalId={editingProposal?.id}
          error={error}
          form={form}
          onClose={() => setModalOpen(false)}
          onSave={save}
          onToggleWeekday={toggleWeekday}
          onUpdate={updateForm}
          working={working}
        />
      ) : null}
    </div>
  );
}

function ProposalCard({ proposal, onEdit, onSubmit, working }) {
  const canEdit = ['DRAFT', 'REJECTED'].includes(proposal.approvalStatus);
  const canSubmit = ['DRAFT', 'REJECTED'].includes(proposal.approvalStatus);
  const sessionCount = proposal.scheduleItems?.length || proposal.plannedSessionCount || 0;
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <span className={`rounded-full px-3 py-1 text-xs font-extrabold ${proposal.approvalStatus === 'APPROVED' ? 'bg-emerald-50 text-emerald-700' : proposal.approvalStatus === 'REJECTED' ? 'bg-rose-50 text-rose-700' : proposal.approvalStatus === 'PENDING_APPROVAL' ? 'bg-amber-50 text-amber-800' : 'bg-slate-100 text-slate-700'}`}>{proposal.approvalStatusLabel}</span>
          <p className="mt-3 text-xs font-bold text-slate-400">{proposal.proposalCode}</p>
          <h2 className="mt-1 font-['Manrope'] text-lg font-black text-[#0b1c30]">{proposal.title}</h2>
          <p className="mt-1 text-sm text-slate-500">{proposal.courseOfferingTitle}</p>
        </div>
        <span className="rounded-lg bg-slate-100 px-3 py-1 text-xs font-extrabold">{proposal.deliveryType === 'VIRTUAL' ? 'Trực tuyến' : 'Tại trung tâm'}</span>
      </div>
      <div className="mt-4 grid grid-cols-2 gap-3 rounded-xl bg-slate-50 p-4 text-sm">
        <Info icon={Check} label="Sức chứa" value={`${proposal.capacity} học viên`} />
        <Info icon={CalendarDays} label="Thời gian" value={`${formatClassroomDate(proposal.plannedStartDate)} → ${formatClassroomDate(proposal.endDate || proposal.plannedEndDate)}`} />
        <Info icon={Clock3} label="Lịch" value={`${(proposal.weekdays || []).join(', ')} · ${String(proposal.sessionStartTime).slice(0, 5)} (${sessionCount} buổi)`} />
        <Info icon={Check} label="Nguồn lực" value={proposal.primaryTeacherName || 'Chưa chọn giáo viên'} />
      </div>
      {proposal.reviewNote ? <p className="mt-4 rounded-xl bg-rose-50 p-3 text-sm font-semibold text-rose-700">Phản hồi xét duyệt: {proposal.reviewNote}</p> : null}
      {canEdit || canSubmit ? (
        <div className="mt-4 flex justify-end gap-2">
          {canEdit ? <button className={SECONDARY_BUTTON_CLASS} onClick={onEdit} type="button"><Edit3 className="h-4 w-4" />Chỉnh sửa</button> : null}
          {canSubmit ? <button className={PRIMARY_BUTTON_CLASS} disabled={working} onClick={onSubmit} type="button"><Send className="h-4 w-4" />Gửi duyệt</button> : null}
        </div>
      ) : null}
    </article>
  );
}

function ProposalModal({
  courseOfferings,
  editing,
  editingProposalId,
  error,
  form,
  onClose,
  onSave,
  onToggleWeekday,
  onUpdate,
  working,
}) {
  const isVirtual = form.deliveryType === 'VIRTUAL';
  const [courseStructure, setCourseStructure] = useState(null);
  const [loadingStructure, setLoadingStructure] = useState(false);
  const [isScheduleCollapsed, setIsScheduleCollapsed] = useState(false);

  // Load course details to extract ordered units and lessons
  useEffect(() => {
    if (!form.courseOfferingId) {
      setCourseStructure(null);
      return;
    }
    let active = true;
    setLoadingStructure(true);
    classroomApi.getStaffProgram(form.courseOfferingId)
      .then((detail) => {
        if (!active) return;
        setCourseStructure(detail);
      })
      .catch(async () => {
        try {
          const fallback = await curriculumApi.getInstructorLedCourse(form.courseOfferingId);
          if (active) setCourseStructure(fallback);
        } catch {
          if (active) setCourseStructure(null);
        }
      })
      .finally(() => {
        if (active) setLoadingStructure(false);
      });
    return () => { active = false; };
  }, [form.courseOfferingId]);

  // Flattened lessons by plannedSessionCount
  const { flattenedLessons, allLessonOptions } = useMemo(() => {
    const units = [...(courseStructure?.units || [])].sort((a, b) => Number(a.displayOrder ?? a.sequenceNumber ?? 0) - Number(b.displayOrder ?? b.sequenceNumber ?? 0));
    const flat = [];
    const options = [
      { label: 'Kiểm tra / Đánh giá', value: 'SPECIAL:EXAM' },
      { label: 'Định hướng / Khai giảng', value: 'SPECIAL:ORIENTATION' },
      { label: 'Học bù', value: 'SPECIAL:MAKEUP' },
      { label: 'Chữa đề & Ôn tập', value: 'SPECIAL:REVIEW' },
      { label: 'Buổi học đặc biệt khác', value: 'SPECIAL:CUSTOM' },
    ];

    units.forEach((unit) => {
      const lessons = [...(unit.lessons || [])].sort((a, b) => Number(a.sessionNumber ?? a.sequenceNumber ?? 0) - Number(b.sessionNumber ?? b.sequenceNumber ?? 0));
      lessons.forEach((lesson) => {
        const repeatCount = Math.max(1, Number(lesson.plannedSessionCount || 1));
        options.push({
          label: `Bài ${lesson.sessionNumber ?? lesson.sequenceNumber}: ${lesson.title} (${repeatCount} buổi)`,
          value: String(lesson.id),
          description: `Unit ${unit.displayOrder ?? unit.sequenceNumber ?? 0}: ${unit.title}`,
        });
        for (let i = 0; i < repeatCount; i++) {
          flat.push({
            lessonId: String(lesson.id),
            lessonTitle: lesson.title,
            lessonNumber: lesson.sessionNumber ?? lesson.sequenceNumber,
            unitId: String(unit.id),
            unitTitle: `Unit ${unit.displayOrder ?? unit.sequenceNumber ?? 0}: ${unit.title}`,
            plannedSessionCount: repeatCount,
          });
        }
      });
    });

    const selectedCourse = courseOfferings.find((item) => String(item.id) === String(form.courseOfferingId));
    if (!flat.length && (selectedCourse?.totalSessions || 0) > 0) {
      const total = Number(selectedCourse.totalSessions);
      for (let i = 1; i <= total; i++) {
        flat.push({
          lessonId: null,
          lessonTitle: `Buổi học ${i}`,
          lessonNumber: i,
          unitId: null,
          unitTitle: '—',
          plannedSessionCount: 1,
        });
      }
    }

    return { flattenedLessons: flat, allLessonOptions: options };
  }, [courseOfferings, courseStructure, form.courseOfferingId]);

  // Recalculate dates and sequence when items are modified
  const resequenceSchedule = (rawItems, startDate = form.plannedStartDate, weekdays = form.weekdays) => {
    const dayMap = { MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3, THURSDAY: 4, FRIDAY: 5, SATURDAY: 6, SUNDAY: 0 };
    const targetDays = new Set(weekdays.map((w) => dayMap[w]));
    const generatedDates = [];

    if (startDate && targetDays.size > 0) {
      let [y, m, d] = startDate.split('-').map(Number);
      let cursor = new Date(y, m - 1, d);
      let safety = 0;
      while (generatedDates.length < rawItems.length && safety < 600) {
        safety++;
        if (targetDays.has(cursor.getDay())) {
          generatedDates.push(toLocalDateKey(cursor));
        }
        cursor.setDate(cursor.getDate() + 1);
      }
    }

    const nextItems = rawItems.map((item, index) => ({
      ...item,
      sequenceNumber: index + 1,
      sessionDate: generatedDates[index] || item.sessionDate || startDate,
      startTime: item.startTime || form.sessionStartTime || '18:30',
      endTime: item.endTime || form.sessionEndTime || '20:30',
    }));

    const calculatedEndDate = nextItems.length ? nextItems[nextItems.length - 1].sessionDate : startDate;
    return { nextItems, calculatedEndDate };
  };

  // Auto-generate initial schedule items on course, start date or weekdays selection
  const lastGeneratedKey = useRef('');
  useEffect(() => {
    if (!form.plannedStartDate || !form.weekdays.length || !flattenedLessons.length) {
      return;
    }

    const key = `${form.courseOfferingId}_${form.plannedStartDate}_${form.weekdays.join(',')}_${form.sessionStartTime}_${form.sessionEndTime}_${flattenedLessons.length}`;

    // If editing existing proposal with loaded schedule items, don't overwrite on mount
    if (editing && form.scheduleItems?.length && !lastGeneratedKey.current) {
      lastGeneratedKey.current = key;
      return;
    }

    if (lastGeneratedKey.current === key && form.scheduleItems?.length) {
      return;
    }

    lastGeneratedKey.current = key;

    const items = flattenedLessons.map((defaultLesson, index) => {
      const existing = (form.scheduleItems || [])[index];
      const lessonId = existing?.courseLessonId !== undefined ? existing.courseLessonId : defaultLesson?.lessonId || null;
      const matchingOption = allLessonOptions.find((opt) => opt.value === String(lessonId));

      return {
        sequenceNumber: index + 1,
        sessionDate: '',
        startTime: form.sessionStartTime || '18:30',
        endTime: form.sessionEndTime || '20:30',
        deliveryModeOverride: existing?.deliveryModeOverride || null,
        teacherId: existing?.teacherId || '',
        roomId: existing?.roomId || '',
        courseLessonId: lessonId,
        courseLessonTitle: matchingOption?.label || defaultLesson?.lessonTitle || '',
        courseUnitTitle: matchingOption?.description || defaultLesson?.unitTitle || '',
        sessionContent: existing?.sessionContent || (lessonId ? '' : (defaultLesson?.lessonTitle || 'Kiểm tra / Đánh giá')),
        note: existing?.note || '',
      };
    });

    const { nextItems, calculatedEndDate } = resequenceSchedule(items, form.plannedStartDate, form.weekdays);

    onUpdate({
      plannedEndDate: calculatedEndDate,
      scheduleItems: nextItems,
    });

    if (nextItems.length > 6) {
      setIsScheduleCollapsed(true);
    }
  }, [allLessonOptions, editing, flattenedLessons, form.courseOfferingId, form.plannedStartDate, form.scheduleItems, form.sessionEndTime, form.sessionStartTime, form.weekdays]);

  const handleLessonChange = (rowIndex, selectedValue) => {
    const updated = [...(form.scheduleItems || [])];
    const item = { ...updated[rowIndex] };

    if (selectedValue?.startsWith('SPECIAL:')) {
      const presetCode = selectedValue.replace('SPECIAL:', '');
      const preset = SPECIAL_PRESETS.find((p) => p.code === presetCode) || SPECIAL_PRESETS[0];
      item.courseLessonId = null;
      item.courseLessonTitle = preset.label;
      item.courseUnitTitle = '—';
      item.sessionContent = preset.content;
    } else if (!selectedValue) {
      item.courseLessonId = null;
      item.courseLessonTitle = 'Buổi đặc biệt / Tự do';
      item.courseUnitTitle = '—';
      item.sessionContent = 'Kiểm tra / Đánh giá';
    } else {
      const selectedOption = allLessonOptions.find((opt) => opt.value === selectedValue);
      item.courseLessonId = selectedValue;
      item.courseLessonTitle = selectedOption?.label || '';
      item.courseUnitTitle = selectedOption?.description || '';
      item.sessionContent = '';
    }
    updated[rowIndex] = item;
    onUpdate({ scheduleItems: updated });
  };

  const handleDeliveryOverrideChange = (rowIndex, value) => {
    const updated = [...(form.scheduleItems || [])];
    updated[rowIndex] = {
      ...updated[rowIndex],
      deliveryModeOverride: value === 'DEFAULT' ? null : value,
      roomId: value === 'VIRTUAL' ? '' : updated[rowIndex].roomId,
    };
    onUpdate({ scheduleItems: updated });
  };

  const handleContentChange = (rowIndex, value) => {
    const updated = [...(form.scheduleItems || [])];
    updated[rowIndex] = { ...updated[rowIndex], sessionContent: value };
    onUpdate({ scheduleItems: updated });
  };

  const handleNoteChange = (rowIndex, value) => {
    const updated = [...(form.scheduleItems || [])];
    updated[rowIndex] = { ...updated[rowIndex], note: value };
    onUpdate({ scheduleItems: updated });
  };

  const [draggedIndex, setDraggedIndex] = useState(null);
  const [dragOverIndex, setDragOverIndex] = useState(null);

  const handleDragStart = (e, index) => {
    setDraggedIndex(index);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', String(index));
  };

  const handleDragOver = (e, index) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    if (dragOverIndex !== index) {
      setDragOverIndex(index);
    }
  };

  const handleDragEnd = () => {
    setDraggedIndex(null);
    setDragOverIndex(null);
  };

  const handleDrop = (e, targetIndex) => {
    e.preventDefault();
    if (draggedIndex === null || draggedIndex === targetIndex) {
      setDraggedIndex(null);
      setDragOverIndex(null);
      return;
    }

    const updated = [...(form.scheduleItems || [])];
    const [movedItem] = updated.splice(draggedIndex, 1);
    updated.splice(targetIndex, 0, movedItem);

    setDraggedIndex(null);
    setDragOverIndex(null);
    const { nextItems, calculatedEndDate } = resequenceSchedule(updated);
    onUpdate({ scheduleItems: nextItems, plannedEndDate: calculatedEndDate });
  };

  const handleMoveUp = (index) => {
    if (index <= 0) return;
    const updated = [...(form.scheduleItems || [])];
    const [movedItem] = updated.splice(index, 1);
    updated.splice(index - 1, 0, movedItem);
    const { nextItems, calculatedEndDate } = resequenceSchedule(updated);
    onUpdate({ scheduleItems: nextItems, plannedEndDate: calculatedEndDate });
  };

  const handleMoveDown = (index) => {
    if (index >= (form.scheduleItems || []).length - 1) return;
    const updated = [...(form.scheduleItems || [])];
    const [movedItem] = updated.splice(index, 1);
    updated.splice(index + 1, 0, movedItem);
    const { nextItems, calculatedEndDate } = resequenceSchedule(updated);
    onUpdate({ scheduleItems: nextItems, plannedEndDate: calculatedEndDate });
  };

  const handleAddSessionAtStart = () => {
    const updated = [...(form.scheduleItems || [])];
    updated.unshift({
      sequenceNumber: 1,
      sessionDate: form.plannedStartDate || '',
      startTime: form.sessionStartTime || '18:30',
      endTime: form.sessionEndTime || '20:30',
      deliveryModeOverride: null,
      teacherId: '',
      roomId: '',
      courseLessonId: null,
      courseLessonTitle: 'Định hướng / Khai giảng',
      courseUnitTitle: '—',
      sessionContent: 'Định hướng / Khai giảng',
      note: '',
    });
    const { nextItems, calculatedEndDate } = resequenceSchedule(updated);
    onUpdate({ scheduleItems: nextItems, plannedEndDate: calculatedEndDate });
  };

  const handleAddSessionAtEnd = () => {
    const updated = [...(form.scheduleItems || [])];
    updated.push({
      sequenceNumber: updated.length + 1,
      sessionDate: '',
      startTime: form.sessionStartTime || '18:30',
      endTime: form.sessionEndTime || '20:30',
      deliveryModeOverride: null,
      teacherId: '',
      roomId: '',
      courseLessonId: null,
      courseLessonTitle: 'Kiểm tra / Đánh giá',
      courseUnitTitle: '—',
      sessionContent: 'Kiểm tra / Đánh giá',
      note: '',
    });
    const { nextItems, calculatedEndDate } = resequenceSchedule(updated);
    onUpdate({ scheduleItems: nextItems, plannedEndDate: calculatedEndDate });
  };

  const handleInsertSessionBefore = (rowIndex) => {
    const updated = [...(form.scheduleItems || [])];
    updated.splice(rowIndex, 0, {
      sequenceNumber: rowIndex + 1,
      sessionDate: '',
      startTime: form.sessionStartTime || '18:30',
      endTime: form.sessionEndTime || '20:30',
      deliveryModeOverride: null,
      teacherId: '',
      roomId: '',
      courseLessonId: null,
      courseLessonTitle: 'Kiểm tra / Đánh giá',
      courseUnitTitle: '—',
      sessionContent: 'Kiểm tra / Đánh giá',
      note: '',
    });
    const { nextItems, calculatedEndDate } = resequenceSchedule(updated);
    onUpdate({ scheduleItems: nextItems, plannedEndDate: calculatedEndDate });
  };

  const handleRemoveSession = (rowIndex) => {
    const updated = (form.scheduleItems || []).filter((_, idx) => idx !== rowIndex);
    const { nextItems, calculatedEndDate } = resequenceSchedule(updated);
    onUpdate({ scheduleItems: nextItems, plannedEndDate: calculatedEndDate });
  };

  // Availability & Conflict Check State
  const [scheduleValidation, setScheduleValidation] = useState({ status: 'idle', message: '' });
  const [resourceAvailability, setResourceAvailability] = useState({
    status: 'idle', teachers: [], rooms: [], errorMessage: '',
  });

  const readyToLoadAvailability = Boolean(
    form.deliveryType
    && form.plannedStartDate
    && form.weekdays.length
    && form.sessionStartTime
    && form.sessionEndTime,
  );

  const readyToValidate = Boolean(
    readyToLoadAvailability
    && resourceAvailability.status === 'ready'
    && form.primaryTeacherId
    && (isVirtual || form.roomId),
  );

  // Fetch Available Teachers & Rooms
  useEffect(() => {
    if (!readyToLoadAvailability) {
      setResourceAvailability({
        status: 'idle', teachers: [], rooms: [], errorMessage: 'Chọn hình thức và lịch học trước',
      });
      return undefined;
    }

    let active = true;
    setResourceAvailability((current) => ({ ...current, status: 'loading', errorMessage: '' }));
    setScheduleValidation({ status: 'checking', message: 'Đang tải giáo viên và phòng học phù hợp...' });

    const timer = window.setTimeout(async () => {
      try {
        const payload = buildProposalPayload(form);
        const result = await enrollmentRequestApi.getClassroomProposalAvailability(
          payload,
          editingProposalId,
        );
        if (!active) return;
        const teachers = result?.teachers || [];
        const rooms = result?.rooms || [];

        setResourceAvailability({
          status: 'ready',
          teachers,
          rooms,
          errorMessage: '',
        });

        // Only reset selected teacher/room if no longer valid in available options
        if (form.primaryTeacherId && !teachers.some((t) => String(t.id) === String(form.primaryTeacherId))) {
          onUpdate({ primaryTeacherId: '' });
        }
        if (form.roomId && !rooms.some((r) => String(r.id) === String(form.roomId))) {
          onUpdate({ roomId: '' });
        }
      } catch (availabilityError) {
        if (!active) return;
        const msg = getClassroomErrorMessage(availabilityError, 'Không thể tải nguồn lực khả dụng.');
        setResourceAvailability({
          status: 'error', teachers: [], rooms: [], errorMessage: msg,
        });
      }
    }, 350);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [editingProposalId, form.capacity, form.deliveryType, form.plannedEndDate, form.plannedStartDate, form.scheduleItems, form.sessionEndTime, form.sessionStartTime, form.weekdays, readyToLoadAvailability]);

  // Schedule conflict validator
  useEffect(() => {
    if (!readyToValidate) {
      setScheduleValidation({
        status: 'idle',
        message: isVirtual
          ? 'Chọn đủ lịch học và giáo viên để kiểm tra lịch.'
          : 'Chọn đủ lịch học, giáo viên và phòng học để kiểm tra lịch.',
      });
      return undefined;
    }

    let active = true;
    setScheduleValidation({ status: 'checking', message: 'Đang kiểm tra nguồn lực theo lịch đã chọn...' });

    const timer = window.setTimeout(async () => {
      try {
        const payload = buildProposalPayload(form);
        const result = await enrollmentRequestApi.validateClassroomProposalSchedule(
          payload,
          editingProposalId,
        );
        if (!active) return;
        if (result?.hasBlockingConflict) {
          setScheduleValidation({
            status: 'invalid',
            message: getConflictSummary(result) || 'Lịch dự kiến đang bị trùng.',
          });
          return;
        }
        setScheduleValidation({
          status: 'valid',
          message: isVirtual
            ? 'Giáo viên đang trống trong toàn bộ lịch dự kiến.'
            : 'Giáo viên và phòng học đang trống trong toàn bộ lịch dự kiến.',
        });
      } catch (err) {
        if (!active) return;
        setScheduleValidation({
          status: 'invalid',
          message: getClassroomErrorMessage(err, 'Không thể kiểm tra lịch dự kiến.'),
        });
      }
    }, 450);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [editingProposalId, form.capacity, form.deliveryType, form.plannedEndDate, form.plannedStartDate, form.primaryTeacherId, form.roomId, form.scheduleItems, form.sessionEndTime, form.sessionStartTime, form.weekdays, isVirtual, readyToValidate, resourceAvailability.status]);

  const validationStyle = {
    idle: 'border-slate-200 bg-slate-50 text-slate-600',
    checking: 'border-blue-200 bg-blue-50 text-blue-700',
    valid: 'border-emerald-200 bg-emerald-50 text-emerald-700',
    invalid: 'border-rose-200 bg-rose-50 text-rose-700',
  }[scheduleValidation.status];

  const ValidationIcon = scheduleValidation.status === 'checking'
    ? Loader2
    : scheduleValidation.status === 'valid'
      ? CheckCircle2
      : AlertCircle;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm">
      <section aria-modal="true" className="flex max-h-[92vh] w-full max-w-5xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl" role="dialog">
        <div className="flex shrink-0 items-start justify-between border-b border-slate-100 p-6">
          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">
              {editing ? 'Chỉnh sửa bản nháp mở lớp' : 'Lập kế hoạch lớp trước khai giảng'}
            </p>
            <h2 className="mt-2 text-2xl font-black text-[#0b1c30]">{editing ? form.title : 'Đề xuất lớp mới'}</h2>
            <p className="mt-1 max-w-2xl text-sm text-slate-500">Thiết lập lịch học, phân bổ bài học, giáo viên và phòng học dự kiến.</p>
          </div>
          <button aria-label="Đóng" className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 disabled:opacity-50" disabled={working} onClick={onClose} type="button">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto p-6 space-y-6">
          {error ? <div className={`${ERROR_NOTICE_CLASS} mb-4`} role="alert">{error}</div> : null}

          <FormSection number="01" title="Thông tin lớp">
            <div className="grid gap-4 lg:grid-cols-2">
              <TextField label="Tên lớp đề xuất" onChange={(value) => onUpdate({ title: value })} value={form.title} />
              <div>
                <FieldLabel>Khóa học</FieldLabel>
                <BrandedSelect
                  disabled={editing}
                  onChange={(event) => {
                    onUpdate({
                      courseOfferingId: event.target.value,
                      enrollmentRequestIds: [],
                      scheduleItems: [],
                    });
                  }}
                  options={courseOfferings.map((item) => ({
                    label: item.title,
                    value: String(item.id),
                    description: [
                      item.entryLevel || 'Mọi trình độ',
                      item.totalSessions ? `${item.totalSessions} buổi` : '',
                    ].filter(Boolean).join(' · '),
                  }))}
                  placeholder="Chọn khóa học"
                  searchable
                  value={form.courseOfferingId}
                />
              </div>
              <div>
                <FieldLabel>Hình thức tổ chức</FieldLabel>
                <BrandedSelect
                  disabled={editing}
                  onChange={(event) => onUpdate({
                    deliveryType: event.target.value,
                    roomId: event.target.value === 'VIRTUAL' ? '' : form.roomId,
                  })}
                  options={[
                    { label: 'Tại trung tâm', value: 'OFFLINE' },
                    { label: 'Trực tuyến', value: 'VIRTUAL' },
                  ]}
                  value={form.deliveryType || 'OFFLINE'}
                />
              </div>
              <TextField label="Sức chứa" min="1" onChange={(value) => onUpdate({ capacity: value })} type="number" value={form.capacity} />
            </div>
          </FormSection>

          <FormSection number="02" title="Cấu hình lịch học tự động">
            <div className="grid gap-4 sm:grid-cols-2">
              <TextField label="Ngày bắt đầu" min={toLocalDateKey()} onChange={(value) => onUpdate({ plannedStartDate: value })} type="date" value={form.plannedStartDate} />
              <div>
                <FieldLabel>Ngày kết thúc dự kiến (Tự động tính)</FieldLabel>
                <div className="flex h-11 items-center rounded-xl border border-slate-200 bg-slate-100 px-4 text-sm font-bold text-slate-700">
                  {form.plannedEndDate ? formatClassroomDate(form.plannedEndDate) : 'Tự động tính từ lịch học'}
                </div>
              </div>
            </div>
            <div>
              <FieldLabel>Ngày học trong tuần</FieldLabel>
              <div className="flex flex-wrap gap-2">
                {weekdayOptions.map(([value, label]) => (
                  <button
                    className={`h-9 min-w-10 rounded-lg px-3 text-xs font-extrabold ${form.weekdays.includes(value) ? 'bg-[#730014] text-white' : 'bg-slate-100 text-slate-600'}`}
                    key={value}
                    onClick={() => onToggleWeekday(value)}
                    type="button"
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <TextField label="Giờ bắt đầu" onChange={(value) => onUpdate({ sessionStartTime: value })} type="time" value={form.sessionStartTime} />
              <TextField label="Giờ kết thúc" onChange={(value) => onUpdate({ sessionEndTime: value })} type="time" value={form.sessionEndTime} />
            </div>
          </FormSection>

          <FormSection
            action={
              (form.scheduleItems || []).length > 0 ? (
                <div className="flex items-center gap-2">
                  <button
                    className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-bold text-slate-700 hover:bg-slate-50 active:scale-95"
                    onClick={() => setIsScheduleCollapsed((c) => !c)}
                    type="button"
                  >
                    {isScheduleCollapsed ? <ChevronDown className="h-3.5 w-3.5" /> : <ChevronUp className="h-3.5 w-3.5" />}
                    {isScheduleCollapsed ? 'Mở rộng' : 'Thu gọn'}
                  </button>
                  <button
                    className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-bold text-slate-700 hover:bg-slate-50 active:scale-95"
                    onClick={handleAddSessionAtStart}
                    title="Chèn thêm 1 buổi vào đầu lịch học"
                    type="button"
                  >
                    <Plus className="h-3.5 w-3.5" /> Chèn buổi đầu
                  </button>
                  <button
                    className="inline-flex items-center gap-1 rounded-lg bg-[#4b0009] px-2.5 py-1 text-xs font-bold text-white hover:bg-[#730014] active:scale-95"
                    onClick={handleAddSessionAtEnd}
                    title="Thêm 1 buổi vào cuối lịch học"
                    type="button"
                  >
                    <Plus className="h-3.5 w-3.5" /> Thêm buổi cuối
                  </button>
                </div>
              ) : null
            }
            number="03"
            title={`Xem trước lịch chi tiết (${(form.scheduleItems || []).length} buổi)`}
          >
            {loadingStructure ? (
              <div className="py-6 text-center text-sm font-semibold text-slate-500">Đang tải cấu trúc bài học từ khóa học...</div>
            ) : !(form.scheduleItems || []).length ? (
              <div className="rounded-xl border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500">
                Hãy chọn khóa học, ngày bắt đầu và các thứ học trong tuần để hệ thống tự động sinh lịch chi tiết.
              </div>
            ) : isScheduleCollapsed ? (
              <div className="flex items-center justify-between rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-xs text-slate-600">
                <span>Lịch chi tiết gồm <strong>{(form.scheduleItems || []).length} buổi</strong> (từ {formatClassroomDate(form.plannedStartDate)} đến {formatClassroomDate(form.plannedEndDate)}).</span>
                <button
                  className="font-bold text-[#730014] hover:underline"
                  onClick={() => setIsScheduleCollapsed(false)}
                  type="button"
                >
                  Mở rộng bảng lịch
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-[11px] font-extrabold uppercase tracking-wider text-slate-500 border-b border-slate-200">
                      <tr>
                        <th className="px-3 py-3 w-16 text-center">STT</th>
                        <th className="px-3 py-3 w-32 whitespace-nowrap">Ngày học</th>
                        <th className="px-3 py-3 min-w-[320px]">Bài học / Nội dung buổi</th>
                        <th className="px-3 py-3 min-w-[200px]">Ghi chú</th>
                        <th className="px-2 py-3 w-32 text-center">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {(form.scheduleItems || []).map((item, idx) => {
                        const isRowSpecial = !item.courseLessonId;
                        const rowSpecialValue = isRowSpecial
                          ? (SPECIAL_PRESETS.find((p) => p.content === item.sessionContent)?.code ? `SPECIAL:${SPECIAL_PRESETS.find((p) => p.content === item.sessionContent)?.code}` : 'SPECIAL:CUSTOM')
                          : String(item.courseLessonId);

                        const isDragging = draggedIndex === idx;
                        const isOver = dragOverIndex === idx;

                        return (
                          <tr
                            key={idx}
                            draggable
                            onDragStart={(e) => handleDragStart(e, idx)}
                            onDragOver={(e) => handleDragOver(e, idx)}
                            onDragEnd={handleDragEnd}
                            onDrop={(e) => handleDrop(e, idx)}
                            className={`transition ${
                              isDragging ? 'opacity-40 bg-slate-100' : isOver ? 'bg-amber-50/70 border-t-2 border-[#730014]' : 'hover:bg-slate-50/70'
                            }`}
                          >
                            <td className="px-3 py-2.5 text-center">
                              <div className="flex items-center justify-center gap-1.5 font-extrabold text-[#730014]">
                                <span className="cursor-grab active:cursor-grabbing text-slate-300 hover:text-slate-600" title="Kéo thả để đổi thứ tự buổi">
                                  <GripVertical className="h-4 w-4" />
                                </span>
                                <span>{idx + 1}</span>
                              </div>
                            </td>
                            <td className="px-3 py-2.5 font-bold text-slate-700 whitespace-nowrap">
                              {formatClassroomDate(item.sessionDate)}
                            </td>
                            <td className="px-3 py-2">
                              <BrandedSelect
                                buttonClassName="h-9 py-1 text-xs"
                                onChange={(e) => handleLessonChange(idx, e.target.value)}
                                options={allLessonOptions}
                                searchable
                                value={rowSpecialValue}
                              />
                            </td>
                            <td className="px-3 py-2">
                              <input
                                className="h-9 w-full rounded-lg border border-slate-200 bg-slate-50 px-2.5 text-xs text-slate-700 outline-none focus:border-[#730014] focus:bg-white"
                                onChange={(e) => handleNoteChange(idx, e.target.value)}
                                placeholder="Ghi chú tùy chọn..."
                                value={item.note || ''}
                              />
                            </td>
                            <td className="px-2 py-2 text-center whitespace-nowrap">
                              <div className="flex items-center justify-center gap-1">
                                <button
                                  aria-label="Chèn buổi trước dòng này"
                                  className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
                                  onClick={() => handleInsertSessionBefore(idx)}
                                  title="Chèn thêm 1 buổi trước dòng này"
                                  type="button"
                                >
                                  <PlusCircle className="h-3.5 w-3.5" />
                                </button>
                                <button
                                  aria-label="Di chuyển lên"
                                  className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700 disabled:opacity-30 disabled:hover:bg-transparent"
                                  disabled={idx === 0}
                                  onClick={() => handleMoveUp(idx)}
                                  title="Chuyển buổi này lên trên"
                                  type="button"
                                >
                                  <ArrowUp className="h-3.5 w-3.5" />
                                </button>
                                <button
                                  aria-label="Di chuyển xuống"
                                  className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700 disabled:opacity-30 disabled:hover:bg-transparent"
                                  disabled={idx === (form.scheduleItems || []).length - 1}
                                  onClick={() => handleMoveDown(idx)}
                                  title="Chuyển buổi này xuống dưới"
                                  type="button"
                                >
                                  <ArrowDown className="h-3.5 w-3.5" />
                                </button>
                                <button
                                  aria-label="Xóa buổi này"
                                  className="rounded p-1 text-slate-400 hover:bg-rose-50 hover:text-rose-600"
                                  onClick={() => handleRemoveSession(idx)}
                                  title="Xóa buổi này"
                                  type="button"
                                >
                                  <Trash2 className="h-3.5 w-3.5" />
                                </button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </FormSection>

          <FormSection number="04" title="Giáo viên và phòng học">
            <div className={`grid gap-4 ${isVirtual ? '' : 'lg:grid-cols-2'}`}>
              <div>
                <FieldLabel>Giáo viên dự kiến</FieldLabel>
                <BrandedSelect
                  disabled={resourceAvailability.status === 'loading'}
                  onChange={(event) => onUpdate({ primaryTeacherId: event.target.value })}
                  options={resourceAvailability.teachers.map((item) => ({
                    label: item.label || item.fullName || item.email,
                    value: String(item.id),
                  }))}
                  placeholder={
                    !readyToLoadAvailability
                      ? 'Chọn hình thức và lịch học trước'
                      : resourceAvailability.status === 'loading'
                        ? 'Đang tìm giáo viên rảnh...'
                        : resourceAvailability.status === 'error'
                          ? 'Lỗi tải giáo viên'
                          : resourceAvailability.teachers.length === 0
                            ? 'Không có giáo viên rảnh'
                            : 'Chọn giáo viên'
                  }
                  searchable
                  value={form.primaryTeacherId}
                />
                {resourceAvailability.status === 'error' && resourceAvailability.errorMessage ? (
                  <p className="mt-1 text-xs text-rose-600">{resourceAvailability.errorMessage}</p>
                ) : null}
              </div>
              {isVirtual ? (
                <div className="rounded-2xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm font-semibold text-blue-800">
                  Google Meet sẽ được chuẩn bị cho từng buổi học.
                </div>
              ) : (
                <div>
                  <FieldLabel>Phòng học</FieldLabel>
                  <BrandedSelect
                    disabled={resourceAvailability.status === 'loading'}
                    onChange={(event) => onUpdate({ roomId: event.target.value })}
                    options={resourceAvailability.rooms.map((item) => ({
                      label: item.label || item.name,
                      value: String(item.id),
                      description: `${item.capacity || 0} chỗ`,
                    }))}
                    placeholder={
                      !readyToLoadAvailability
                        ? 'Chọn hình thức và lịch học trước'
                        : resourceAvailability.status === 'loading'
                          ? 'Đang tìm phòng trống...'
                          : resourceAvailability.status === 'error'
                            ? 'Lỗi tải phòng học'
                            : resourceAvailability.rooms.length === 0
                              ? 'Không có phòng phù hợp'
                              : 'Chọn phòng trống'
                    }
                    searchable
                    value={form.roomId}
                  />
                  {resourceAvailability.status === 'error' && resourceAvailability.errorMessage ? (
                    <p className="mt-1 text-xs text-rose-600">{resourceAvailability.errorMessage}</p>
                  ) : null}
                </div>
              )}
            </div>
            <div className={`flex items-start gap-2 rounded-xl border px-3 py-3 text-sm font-semibold ${validationStyle}`}>
              <ValidationIcon className={`mt-0.5 h-4 w-4 shrink-0 ${scheduleValidation.status === 'checking' ? 'animate-spin' : ''}`} />
              <span>{scheduleValidation.message}</span>
            </div>
          </FormSection>

          <label className="block">
            <FieldLabel>Ghi chú xét duyệt</FieldLabel>
            <RichTextEditor helperText="" onChange={(value) => onUpdate({ note: value })} placeholder="Ghi chú / lý do đề xuất mở lớp..." size="form" value={form.note} />
          </label>

          <div className="flex justify-end gap-2 border-t border-slate-100 pt-5">
            <button className={SECONDARY_BUTTON_CLASS} disabled={working} onClick={onClose} type="button">Hủy</button>
            <button
              className={PRIMARY_BUTTON_CLASS}
              disabled={working || scheduleValidation.status === 'checking' || scheduleValidation.status === 'invalid'}
              onClick={onSave}
              type="button"
            >
              {working ? 'Đang lưu...' : editing ? 'Lưu chỉnh sửa' : 'Tạo bản nháp'}
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}

function FieldLabel({ children }) { return <span className="mb-2 block text-xs font-bold uppercase tracking-[0.1em] text-slate-500">{children}</span>; }
function FormSection({ action, children, number, title }) {
  return (
    <section className="space-y-4 rounded-2xl border border-slate-200 bg-slate-50/50 p-4 sm:p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-[#730014] text-xs font-extrabold text-white">{number}</span>
          <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">{title}</h3>
        </div>
        {action || null}
      </div>
      {children}
    </section>
  );
}
function TextField({ label, onChange, value, type = 'text', min }) {
  return (
    <label className="block">
      <FieldLabel>{label}</FieldLabel>
      {type === 'date' ? (
        <VietnameseDateInput className={FIELD_CLASS} min={min} onChange={onChange} value={value} />
      ) : (
        <input className={FIELD_CLASS} min={min} onChange={(event) => onChange(event.target.value)} type={type} value={value} />
      )}
    </label>
  );
}
function Info({ icon: Icon, label, value }) { return <div className="flex items-start gap-2"><Icon className="mt-0.5 h-4 w-4 text-[#8a0018]" /><div><p className="text-[10px] font-bold uppercase text-slate-400">{label}</p><p className="mt-0.5 font-bold text-slate-700">{value}</p></div></div>; }
