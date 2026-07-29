import { useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  CalendarDays,
  Check,
  CheckCircle2,
  Clock3,
  Edit3,
  Loader2,
  Plus,
  RefreshCw,
  Send,
  X,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import BrandedSelect from '../../components/ui/BrandedSelect';
import VietnameseDateInput from '../../components/ui/VietnameseDateInput';
import Pagination, { usePagination } from '../../components/ui/Pagination';
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

const emptyForm = {
  title: '', courseOfferingId: '', enrollmentRequestIds: [], capacity: 20,
  plannedStartDate: '', plannedEndDate: '', weekdays: [], sessionStartTime: '18:30',
  sessionEndTime: '20:30', primaryTeacherId: '', roomId: '', offlineAddress: '',
  note: '',
};

const toLocalDateKey = (date = new Date()) => [
  date.getFullYear(),
  String(date.getMonth() + 1).padStart(2, '0'),
  String(date.getDate()).padStart(2, '0'),
].join('-');

const buildProposalPayload = (form) => ({
  ...form,
  title: form.title.trim(),
  courseOfferingId: form.courseOfferingId ? Number(form.courseOfferingId) : null,
  capacity: form.capacity === '' ? null : Number(form.capacity),
  primaryTeacherId: form.primaryTeacherId ? Number(form.primaryTeacherId) : null,
  roomId: form.roomId ? Number(form.roomId) : null,
  offlineAddress: form.offlineAddress.trim() || null,
  note: form.note.trim() || null,
});

export default function StaffClassroomProposalsPage() {
  const [status, setStatus] = useState('DRAFT');
  const [proposals, setProposals] = useState([]);
  const [courseOfferings, setCourseOfferings] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingProposal, setEditingProposal] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [proposalData, offeringData, teacherData, roomData] = await Promise.all([
        enrollmentRequestApi.listStaffClassroomProposals(status),
        classroomApi.getStaffPrograms(),
        classroomApi.getStaffTeachers(),
        classroomApi.getStaffRooms(),
      ]);
      setProposals(proposalData);
      setCourseOfferings(offeringData);
      setTeachers(teacherData);
      setRooms(roomData);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải danh sách đề xuất lớp.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [status]);

  const selectedOffering = useMemo(() => courseOfferings.find(
    (item) => String(item.id) === String(form.courseOfferingId),
  ) || null, [courseOfferings, form.courseOfferingId]);

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
      enrollmentRequestIds: proposal.members.map((item) => item.enrollmentRequestId),
      capacity: proposal.capacity,
      plannedStartDate: proposal.plannedStartDate,
      plannedEndDate: proposal.plannedEndDate,
      weekdays: proposal.weekdays || [],
      sessionStartTime: String(proposal.sessionStartTime || '').slice(0, 5),
      sessionEndTime: String(proposal.sessionEndTime || '').slice(0, 5),
      primaryTeacherId: proposal.primaryTeacherId ? String(proposal.primaryTeacherId) : '',
      roomId: proposal.roomId ? String(proposal.roomId) : '',
      offlineAddress: proposal.offlineAddress || '',
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
    if (!form.plannedStartDate || !form.plannedEndDate || !form.weekdays.length) {
      setError('Cần nhập khoảng ngày và ít nhất một thứ học trong tuần.');
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
    if (form.plannedEndDate < form.plannedStartDate) {
      setError('Ngày kết thúc phải từ ngày bắt đầu trở đi.');
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
      await load();
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
              className={`shrink-0 rounded-xl px-4 py-2.5 text-xs font-extrabold transition ${
                status === tab.value
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
            onClick={load}
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

      {error ? <div className={ERROR_NOTICE_CLASS}>{error}</div> : null}
      {success ? <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-700">{success}</div> : null}
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
          <div className="rounded-xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
            <Pagination onChange={setPage} page={page} pageSize={8} totalItems={totalItems} totalPages={totalPages} />
          </div>
        </div>
      ) : null}

      {modalOpen ? <ProposalModal courseOfferings={courseOfferings} editing={Boolean(editingProposal)} editingProposalId={editingProposal?.id} form={form} onClose={() => setModalOpen(false)} onSave={save} onToggleWeekday={toggleWeekday} onUpdate={updateForm} rooms={rooms} selectedOffering={selectedOffering} teachers={teachers} working={working} /> : null}
    </div>
  );
}

function ProposalCard({ proposal, onEdit, onSubmit, working }) {
  const canEdit = ['DRAFT', 'REJECTED'].includes(proposal.approvalStatus);
  const canSubmit = ['DRAFT', 'REJECTED'].includes(proposal.approvalStatus);
  return <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><div className="flex items-start justify-between gap-3"><div><span className={`rounded-full px-3 py-1 text-xs font-extrabold ${proposal.approvalStatus === 'APPROVED' ? 'bg-emerald-50 text-emerald-700' : proposal.approvalStatus === 'REJECTED' ? 'bg-rose-50 text-rose-700' : proposal.approvalStatus === 'PENDING_APPROVAL' ? 'bg-amber-50 text-amber-800' : 'bg-slate-100 text-slate-700'}`}>{proposal.approvalStatusLabel}</span><p className="mt-3 text-xs font-bold text-slate-400">{proposal.proposalCode}</p><h2 className="mt-1 font-['Manrope'] text-lg font-black text-[#0b1c30]">{proposal.title}</h2><p className="mt-1 text-sm text-slate-500">{proposal.courseOfferingTitle}</p></div><span className="rounded-lg bg-slate-100 px-3 py-1 text-xs font-extrabold">{proposal.deliveryType}</span></div><div className="mt-4 grid grid-cols-2 gap-3 rounded-xl bg-slate-50 p-4 text-sm"><Info icon={Check} label="Sức chứa" value={`${proposal.capacity} học viên`} /><Info icon={CalendarDays} label="Thời gian" value={`${formatClassroomDate(proposal.plannedStartDate)} → ${formatClassroomDate(proposal.plannedEndDate)}`} /><Info icon={Clock3} label="Lịch" value={`${(proposal.weekdays || []).join(', ')} · ${String(proposal.sessionStartTime).slice(0, 5)}`} /><Info icon={Check} label="Nguồn lực" value={proposal.primaryTeacherName || 'Chưa chọn giáo viên'} /></div>{proposal.reviewNote ? <p className="mt-4 rounded-xl bg-rose-50 p-3 text-sm font-semibold text-rose-700">Phản hồi xét duyệt: {proposal.reviewNote}</p> : null}{canEdit || canSubmit ? <div className="mt-4 flex justify-end gap-2">{canEdit ? <button className={SECONDARY_BUTTON_CLASS} onClick={onEdit} type="button"><Edit3 className="h-4 w-4" />Chỉnh sửa</button> : null}{canSubmit ? <button className={PRIMARY_BUTTON_CLASS} disabled={working} onClick={onSubmit} type="button"><Send className="h-4 w-4" />Gửi duyệt</button> : null}</div> : null}</article>;
}

function ProposalModal({
  courseOfferings,
  editing,
  editingProposalId,
  form,
  onClose,
  onSave,
  onToggleWeekday,
  onUpdate,
  rooms,
  selectedOffering,
  teachers,
  working,
}) {
  const isVirtual = (selectedOffering?.deliveryType || selectedOffering?.deliveryMode) === 'VIRTUAL';
  const [scheduleValidation, setScheduleValidation] = useState({ status: 'idle', message: '' });
  const validationPayload = useMemo(() => buildProposalPayload(form), [form]);
  const readyToValidate = Boolean(
    validationPayload.title
      && validationPayload.courseOfferingId
      && validationPayload.capacity
      && validationPayload.plannedStartDate
      && validationPayload.plannedEndDate
      && validationPayload.weekdays.length
      && validationPayload.sessionStartTime
      && validationPayload.sessionEndTime
      && validationPayload.primaryTeacherId
      && (isVirtual || (validationPayload.roomId && validationPayload.offlineAddress)),
  );

  useEffect(() => {
    if (!readyToValidate) {
      setScheduleValidation({
        status: 'idle',
        message: 'Chọn đủ giáo viên, ngày, giờ và phòng học để hệ thống kiểm tra lịch.',
      });
      return undefined;
    }

    let active = true;
    setScheduleValidation({ status: 'checking', message: 'Đang kiểm tra lịch giáo viên và phòng học...' });
    const timer = window.setTimeout(async () => {
      try {
        const result = await enrollmentRequestApi.validateClassroomProposalSchedule(
          validationPayload,
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
          message: 'Giáo viên và phòng học đang trống trong toàn bộ lịch dự kiến.',
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
  }, [editingProposalId, readyToValidate, validationPayload]);

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
      <section aria-modal="true" className="max-h-[92vh] w-full max-w-4xl overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl" role="dialog">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">
              {editing ? 'Chỉnh sửa bản nháp mở lớp' : 'Lập kế hoạch lớp trước khai giảng'}
            </p>
            <h2 className="mt-2 text-2xl font-black text-[#0b1c30]">{editing ? form.title : 'Đề xuất lớp mới'}</h2>
            <p className="mt-2 max-w-2xl text-sm text-slate-500">Thiết lập lịch học, giáo viên, phòng học và sức chứa dự kiến.</p>
          </div>
          <button className="rounded-lg p-2 text-slate-400 hover:bg-slate-100" onClick={onClose} type="button">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="mt-6 grid gap-5 lg:grid-cols-2">
          <div className="space-y-4">
            <TextField label="Tên lớp đề xuất" onChange={(value) => onUpdate({ title: value })} value={form.title} />
            <div>
              <FieldLabel>Khóa học nền</FieldLabel>
              <BrandedSelect
                disabled={editing}
                onChange={(event) => onUpdate({ courseOfferingId: event.target.value, enrollmentRequestIds: [] })}
                options={courseOfferings.map((item) => ({
                  label: item.title,
                  value: String(item.id),
                  description: `${item.deliveryType || item.deliveryMode} · ${item.entryLevel || 'Mọi trình độ'}`,
                }))}
                placeholder="Chọn khóa học"
                searchable
                value={form.courseOfferingId}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <TextField label="Sức chứa" min="1" onChange={(value) => onUpdate({ capacity: value })} type="number" value={form.capacity} />
              <div>
                <FieldLabel>Giáo viên dự kiến</FieldLabel>
                <BrandedSelect
                  onChange={(event) => onUpdate({ primaryTeacherId: event.target.value })}
                  options={teachers.map((item) => ({ label: item.label || item.fullName || item.email, value: String(item.id) }))}
                  placeholder="Chọn giáo viên"
                  searchable
                  value={form.primaryTeacherId}
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <TextField label="Ngày bắt đầu" min={toLocalDateKey()} onChange={(value) => onUpdate({ plannedStartDate: value })} type="date" value={form.plannedStartDate} />
              <TextField label="Ngày kết thúc" min={form.plannedStartDate || toLocalDateKey()} onChange={(value) => onUpdate({ plannedEndDate: value })} type="date" value={form.plannedEndDate} />
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
            <div className="grid grid-cols-2 gap-3">
              <TextField label="Giờ bắt đầu" onChange={(value) => onUpdate({ sessionStartTime: value })} type="time" value={form.sessionStartTime} />
              <TextField label="Giờ kết thúc" onChange={(value) => onUpdate({ sessionEndTime: value })} type="time" value={form.sessionEndTime} />
            </div>
          </div>

          <div className="space-y-4">
            {isVirtual ? (
              <div className="rounded-2xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
                <p className="font-extrabold">Phòng học Virtual được tạo tự động</p>
                <p className="mt-1 leading-6">
                  Mỗi buổi học trực tuyến sử dụng một phòng Lark riêng
                  và hiển thị trạng thái đồng bộ trong chi tiết lớp.
                </p>
              </div>
            ) : (
              <>
                <div>
                  <FieldLabel>Phòng học</FieldLabel>
                  <BrandedSelect
                    onChange={(event) => onUpdate({ roomId: event.target.value })}
                    options={rooms.map((item) => ({
                      label: item.label || item.name,
                      value: String(item.id),
                      description: `${item.capacity || 0} chỗ`,
                    }))}
                    placeholder="Chọn phòng"
                    searchable
                    value={form.roomId}
                  />
                </div>
                <TextField label="Địa chỉ/cơ sở" onChange={(value) => onUpdate({ offlineAddress: value })} value={form.offlineAddress} />
              </>
            )}
            <div className={`flex items-start gap-2 rounded-xl border px-3 py-3 text-sm font-semibold ${validationStyle}`}>
              <ValidationIcon className={`mt-0.5 h-4 w-4 shrink-0 ${scheduleValidation.status === 'checking' ? 'animate-spin' : ''}`} />
              <span>{scheduleValidation.message}</span>
            </div>
            <label className="block">
              <FieldLabel>Ghi chú xét duyệt</FieldLabel>
              <RichTextEditor helperText="" onChange={(value) => onUpdate({ note: value })} placeholder="Ghi chú / lý do đề xuất mở lớp..." size="form" value={form.note} />
            </label>
          </div>
        </div>

        <div className="mt-6 flex justify-end gap-2 border-t border-slate-100 pt-5">
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
      </section>
    </div>
  );
}

function FieldLabel({ children }) { return <span className="mb-2 block text-xs font-bold uppercase tracking-[0.1em] text-slate-500">{children}</span>; }
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
