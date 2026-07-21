import { useEffect, useMemo, useState } from 'react';
import { BookOpenCheck, Check, ClipboardList, RefreshCw, Search, Send, UserRoundCheck, X } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import { EnrollmentStatusBadge } from '../../components/classroom/EnrollmentRequestUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { ERROR_NOTICE_CLASS, FIELD_CLASS, PRIMARY_BUTTON_CLASS, SECONDARY_BUTTON_CLASS, TEXTAREA_CLASS } from '../../utils/formStyles';

const views = [
  { label: 'Tất cả', value: 'ALL' },
  { label: 'Mới đăng ký', value: 'SUBMITTED' },
  { label: 'Đã tư vấn', value: 'WAITING_FOR_CLASS' },
  { label: 'Đã xếp lớp', value: 'CLASS_ASSIGNED' },
  { label: 'Từ chối', value: 'REJECTED' },
  { label: 'Đã hủy', value: 'CANCELLED' },
];

const initialAction = { type: '', item: null, classroomId: '', note: '', reason: '' };

export default function StaffEnrollmentRequestsPage() {
  const [view, setView] = useState('ALL');
  const [requests, setRequests] = useState([]);
  const [classrooms, setClassrooms] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [action, setAction] = useState(initialAction);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [requestData, classroomData] = await Promise.all([
        enrollmentRequestApi.listForStaff(view),
        classroomApi.getManagerClassrooms(),
      ]);
      setRequests(requestData);
      setClassrooms(classroomData.filter((item) => ['UPCOMING', 'ACTIVE'].includes(item.classroomStatus)));
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải hàng đợi form đăng ký.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    setAction(initialAction);
  }, [view]);

  const filteredRequests = useMemo(() => {
    const normalized = keyword.trim().toLocaleLowerCase('vi-VN');
    return requests.filter((item) => !normalized || [
      item.learnerName,
      item.learnerEmail,
      item.contactName,
      item.contactEmail,
      item.contactPhone,
      item.desiredClassCode,
      item.consultationTrack,
      item.studyWorkGoal,
      item.requestedClassroomTitle,
      item.courseOfferingTitle,
      item.preferredSchedule,
      item.campusPreference,
    ].filter(Boolean).join(' ').toLocaleLowerCase('vi-VN').includes(normalized));
  }, [keyword, requests]);

  const completeConsultation = async () => {
    setWorking(true);
    setError('');
    try {
      const updated = await enrollmentRequestApi.completeConsultation(action.item.id, action.note.trim());
      setRequests((current) => current.filter((item) => item.id !== updated.id));
      setSuccess(`Đã ghi nhận tư vấn xong cho ${updated.contactName || updated.learnerName}; hồ sơ chuyển sang chờ xếp lớp.`);
      setAction(initialAction);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể cập nhật kết quả tư vấn.');
    } finally {
      setWorking(false);
    }
  };

  const assignClass = async () => {
    if (!action.classroomId) {
      setError('Vui lòng chọn lớp phù hợp cho học viên.');
      return;
    }
    setWorking(true);
    setError('');
    try {
      const updated = await enrollmentRequestApi.assignClass(action.item.id, {
        classroomId: Number(action.classroomId),
        note: action.note.trim() || null,
      });
      setRequests((current) => current.filter((item) => item.id !== updated.id));
      setSuccess(`Đã xếp ${updated.contactName || updated.learnerName} vào lớp #${updated.assignedClassroomId}.`);
      setAction(initialAction);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể xếp lớp cho học viên.');
    } finally {
      setWorking(false);
    }
  };

  const reject = async () => {
    if (!action.reason.trim()) {
      setError('Lý do từ chối là bắt buộc.');
      return;
    }
    setWorking(true);
    setError('');
    try {
      const updated = await enrollmentRequestApi.reject(action.item.id, action.reason.trim());
      setRequests((current) => current.filter((item) => item.id !== updated.id));
      setSuccess(`Đã từ chối form #${updated.id}.`);
      setAction(initialAction);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể từ chối form.');
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-5">
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="bg-[linear-gradient(120deg,#4b0009,#8a0018)] px-6 py-7 text-white">
          <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
            <div><span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-extrabold uppercase tracking-[0.12em]"><ClipboardList className="h-4 w-4" />Tư vấn & xếp lớp</span><h1 className="mt-4 font-['Manrope'] text-2xl font-black md:text-3xl">Form đăng ký từ Lịch khai giảng</h1><p className="mt-2 max-w-2xl text-sm leading-6 text-white/75">Staff liên hệ, test và tư vấn bên ngoài hệ thống; khi đã chốt, Staff chủ động chọn lớp phù hợp cho học viên.</p></div>
            <div className="rounded-xl bg-white/10 px-5 py-3 text-center"><p className="text-2xl font-black">{filteredRequests.length}</p><p className="text-xs font-bold text-white/65">Hồ sơ trong tab</p></div>
          </div>
        </div>
        <div className="flex gap-2 overflow-x-auto border-b border-slate-100 px-4 py-3">{views.map((item) => <button className={`shrink-0 rounded-xl px-4 py-2 text-xs font-extrabold ${view === item.value ? 'bg-[#730014] text-white' : 'bg-slate-50 text-slate-600'}`} key={item.value} onClick={() => setView(item.value)} type="button">{item.label}</button>)}</div>
        <div className="grid gap-3 p-4 md:grid-cols-[minmax(0,1fr)_48px]"><label className="relative"><span className="sr-only">Tìm hồ sơ</span><Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><input className={`${FIELD_CLASS} pl-11`} onChange={(event) => setKeyword(event.target.value)} placeholder="Tìm học viên, email, lớp đăng ký hoặc ca liên hệ..." value={keyword} /></label><button aria-label="Tải lại" className="flex h-12 items-center justify-center rounded-xl border border-slate-200 text-[#730014]" disabled={loading} onClick={load} type="button"><RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /></button></div>
      </section>

      {error ? <div className={ERROR_NOTICE_CLASS}>{error}</div> : null}
      {success ? <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-700">{success}</div> : null}
      {loading ? <div className="grid gap-4 xl:grid-cols-2">{Array.from({ length: 4 }).map((_, index) => <div className="h-64 animate-pulse rounded-2xl bg-slate-100" key={index} />)}</div> : null}
      {!loading && filteredRequests.length ? <div className="grid gap-4 xl:grid-cols-2">{filteredRequests.map((request) => <EnrollmentCard key={request.id} onAssign={() => setAction({ ...initialAction, type: 'ASSIGN', item: request })} onConsult={() => setAction({ ...initialAction, type: 'CONSULT', item: request })} onReject={() => setAction({ ...initialAction, type: 'REJECT', item: request })} request={request} />)}</div> : null}
      {!loading && !filteredRequests.length ? <EmptyState /> : null}
      {action.type ? <ActionModal action={action} classrooms={classrooms} onChange={setAction} onClose={() => setAction(initialAction)} onConfirm={action.type === 'CONSULT' ? completeConsultation : action.type === 'ASSIGN' ? assignClass : reject} working={working} /> : null}
    </div>
  );
}

function EnrollmentCard({ request, onAssign, onConsult, onReject }) {
  const canProcess = ['SUBMITTED', 'UNDER_STAFF_REVIEW'].includes(request.status);
  const canAssign = ['SUBMITTED', 'UNDER_STAFF_REVIEW', 'WAITING_FOR_CLASS'].includes(request.status);
  const contactName = request.contactName || request.learnerName;
  const contactEmail = request.contactEmail || request.learnerEmail;
  return <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><div className="flex flex-wrap items-start justify-between gap-3"><div><EnrollmentStatusBadge label={request.statusLabel} status={request.status} /><p className="mt-3 text-xs font-bold text-slate-400">Form #{request.id}</p><h2 className="mt-1 font-['Manrope'] text-lg font-black text-[#0b1c30]">{contactName}</h2><p className="text-sm text-slate-500">{contactEmail}</p><p className="mt-1 text-sm font-bold text-slate-700">{request.contactPhone || 'Chưa có số điện thoại'}</p></div><span className="rounded-lg bg-[#fff1f3] px-3 py-1 text-xs font-extrabold text-[#8a0018]">Tư vấn chung</span></div><div className="mt-4 rounded-xl bg-slate-50 p-4"><p className="text-sm font-extrabold text-[#0b1c30]">Nguyện vọng: {request.desiredClassCode || 'Chưa chọn mã lớp'}</p><div className="mt-2 grid gap-2 text-xs text-slate-500 sm:grid-cols-2"><span>Lộ trình: {formatConsultationTrack(request.consultationTrack)}</span><span>Facebook: {request.facebookUrl || 'Chưa cung cấp'}</span></div>{request.studyWorkGoal ? <p className="mt-3 border-t border-slate-200 pt-3 text-xs leading-5 text-slate-600">Học tập/công việc & mục tiêu: {request.studyWorkGoal}</p> : null}{request.requestedClassroomTitle ? <p className="mt-2 text-xs text-slate-500">Dữ liệu cũ từng chọn lớp: {request.requestedClassroomTitle}</p> : null}</div>{canAssign ? <div className="mt-4 flex flex-wrap justify-end gap-2"><button className="inline-flex items-center gap-2 rounded-xl border border-rose-200 px-4 py-2.5 text-xs font-extrabold text-rose-700" onClick={onReject} type="button"><X className="h-3.5 w-3.5" />Từ chối</button>{canProcess ? <button className={SECONDARY_BUTTON_CLASS} onClick={onConsult} type="button"><Check className="h-4 w-4" />Đã tư vấn</button> : null}<button className={PRIMARY_BUTTON_CLASS} onClick={onAssign} type="button"><UserRoundCheck className="h-4 w-4" />Xếp vào lớp</button></div> : null}</article>;
}

function ActionModal({ action, classrooms, onChange, onClose, onConfirm, working }) {
  const isReject = action.type === 'REJECT';
  const isAssign = action.type === 'ASSIGN';
  return <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm"><section aria-modal="true" className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl" role="dialog"><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">{isReject ? 'Từ chối form' : isAssign ? 'Staff xếp lớp' : 'Hoàn tất tư vấn'}</p><h2 className="mt-2 text-xl font-black text-[#0b1c30]">{action.item.contactName || action.item.learnerName} · #{action.item.id}</h2></div><button className="rounded-lg p-2 text-slate-400" onClick={onClose} type="button"><X className="h-5 w-5" /></button></div>{isAssign ? <div className="mt-5"><FieldLabel>Lớp phù hợp *</FieldLabel><BrandedSelect onChange={(event) => onChange({ ...action, classroomId: event.target.value })} options={classrooms.map((item) => ({ value: String(item.id), label: item.title, description: `${item.startDate || 'Chưa có ngày'} · ${item.primaryTeacherName || 'Chưa có giáo viên'}` }))} placeholder="Chọn lớp sau khi đã test và tư vấn" searchable value={action.classroomId} /></div> : null}<label className="mt-4 block"><FieldLabel>{isReject ? 'Lý do từ chối *' : 'Ghi chú nội bộ'}</FieldLabel><textarea className={TEXTAREA_CLASS} onChange={(event) => onChange({ ...action, [isReject ? 'reason' : 'note']: event.target.value })} placeholder={isReject ? 'Nêu lý do rõ ràng...' : 'Kết quả tư vấn/test bên ngoài, lưu ý khi xếp lớp...'} rows={5} value={isReject ? action.reason : action.note} /></label><div className="mt-6 flex justify-end gap-2"><button className={SECONDARY_BUTTON_CLASS} disabled={working} onClick={onClose} type="button">Hủy</button><button className={isReject ? 'rounded-xl bg-rose-700 px-5 py-3 text-sm font-extrabold text-white' : PRIMARY_BUTTON_CLASS} disabled={working} onClick={onConfirm} type="button"><Send className="h-4 w-4" />{working ? 'Đang xử lý...' : isReject ? 'Từ chối' : isAssign ? 'Xác nhận xếp lớp' : 'Đã tư vấn xong'}</button></div></section></div>;
}

function formatConsultationTrack(value) {
  return {
    IELTS_4_SKILLS: 'IELTS 4 kỹ năng',
    TOEIC_2_SKILLS: 'TOEIC 2 kỹ năng',
    TOEIC_4_SKILLS: 'TOEIC 4 kỹ năng',
  }[value] || value || 'Chưa chọn';
}

function FieldLabel({ children }) { return <span className="mb-2 block text-xs font-bold uppercase tracking-[0.1em] text-slate-500">{children}</span>; }
function EmptyState() { return <section className="flex min-h-[360px] flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center"><BookOpenCheck className="h-12 w-12 text-slate-300" /><h2 className="mt-4 text-xl font-black text-[#0b1c30]">Không có form trong tab này</h2><p className="mt-2 text-sm text-slate-500">Các form từ Lịch khai giảng sẽ xuất hiện ở tab Mới đăng ký.</p></section>; }
