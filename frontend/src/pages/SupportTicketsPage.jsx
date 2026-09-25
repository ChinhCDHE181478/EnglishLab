import { useEffect, useState } from 'react';
import {
  CheckCircle2,
  ClipboardList,
  LifeBuoy,
  MessageSquarePlus,
  PauseCircle,
  Plus,
  RotateCcw,
  Send,
  Upload,
  X,
  XCircle,
} from 'lucide-react';
import supportApi from '../api/supportApi';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import BrandedSelect from '../components/ui/BrandedSelect';
import VietnameseDateInput from '../components/ui/VietnameseDateInput';
import {
  formatSupportTime,
  isSupportTicketTerminal,
  supportCategoryLabels,
  supportCategoryOptions,
  supportPriorityLabels,
  supportStatusClasses,
  supportStatusLabels,
  supportApiError,
} from '../utils/supportTicketLabels';

const emptyForm = { subject: '', category: 'ACCOUNT', message: '' };
const todayKey = () => {
  const now = new Date();
  return [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
  ].join('-');
};
const addMonthsToDateKey = (dateKey, months) => {
  const [year, month, day] = dateKey.split('-').map(Number);
  const targetMonthStart = new Date(year, month - 1 + months, 1);
  const lastDay = new Date(
    targetMonthStart.getFullYear(),
    targetMonthStart.getMonth() + 1,
    0,
  ).getDate();
  const target = new Date(
    targetMonthStart.getFullYear(),
    targetMonthStart.getMonth(),
    Math.min(day, lastDay),
  );
  return [
    target.getFullYear(),
    String(target.getMonth() + 1).padStart(2, '0'),
    String(target.getDate()).padStart(2, '0'),
  ].join('-');
};
const createEmptySuspensionForm = () => ({
  enrollmentId: '', requestedStartDate: todayKey(), requestedReturnDate: '', reason: '', proofUrl: '',
});
const parseValues = (value) => {
  try { return value ? JSON.parse(value) : {}; } catch { return {}; }
};

export default function SupportTicketsPage() {
  const [tickets, setTickets] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [comment, setComment] = useState('');
  const [showForm, setShowForm] = useState(true);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [classroomIssue, setClassroomIssue] = useState('GENERAL');
  const [suspensionForm, setSuspensionForm] = useState(createEmptySuspensionForm);
  const [suspensionEnrollments, setSuspensionEnrollments] = useState([]);
  const [suspensionRequests, setSuspensionRequests] = useState([]);
  const [uploadingProof, setUploadingProof] = useState(false);

  const loadTickets = async (preferredId) => {
    setLoading(true);
    setError('');
    try {
      const data = await supportApi.listMyTickets();
      const items = Array.isArray(data) ? data : [];
      setTickets(items);
      const nextId = preferredId ?? selectedId ?? null;
      setSelectedId(nextId && items.some((item) => item.id === nextId) ? nextId : null);
      if (!nextId || !items.some((item) => item.id === nextId)) setDetail(null);
    } catch (err) {
      setError(supportApiError(err, 'Không tải được danh sách yêu cầu hỗ trợ.'));
    } finally {
      setLoading(false);
    }
  };

  const loadDetail = async (ticketId) => {
    if (!ticketId) return;
    setDetailLoading(true);
    setError('');
    try {
      setDetail(await supportApi.getMyTicket(ticketId));
    } catch (err) {
      setError(supportApiError(err, 'Không tải được chi tiết yêu cầu.'));
    } finally {
      setDetailLoading(false);
    }
  };

  const loadSuspensionData = async () => {
    try {
      const [eligibility, requests] = await Promise.all([
        supportApi.getCourseSuspensionEligibility(),
        supportApi.listMyCourseSuspensionRequests(),
      ]);
      setSuspensionEnrollments(Array.isArray(eligibility) ? eligibility : []);
      setSuspensionRequests(Array.isArray(requests) ? requests : []);
    } catch (err) {
      setError(supportApiError(err, 'Không tải được thông tin bảo lưu khóa học.'));
    }
  };

  useEffect(() => {
    loadTickets();
    loadSuspensionData();
  }, []);
  useEffect(() => {
    if (selectedId) loadDetail(selectedId);
    else setDetail(null);
  }, [selectedId]);

  const isSuspensionForm = form.category === 'CLASSROOM' && classroomIssue === 'COURSE_SUSPENSION';
  const canCreate = isSuspensionForm
    ? Boolean(
      suspensionForm.enrollmentId
      && suspensionForm.requestedStartDate
      && suspensionForm.requestedReturnDate
      && suspensionForm.reason.trim().length >= 10
      && suspensionForm.proofUrl,
    )
    : form.subject.trim().length >= 5 && form.message.trim().length >= 10;

  const createTicket = async (event) => {
    event.preventDefault();
    if (!canCreate) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      if (isSuspensionForm) {
        const created = await supportApi.createCourseSuspension({
          ...suspensionForm,
          enrollmentId: Number(suspensionForm.enrollmentId),
        });
        setSuspensionForm(createEmptySuspensionForm());
        setSuccess(`Đã gửi yêu cầu bảo lưu #${created.id}.`);
        await loadSuspensionData();
        return;
      }
      const created = await supportApi.createTicket(form);
      setForm(emptyForm);
      setShowForm(false);
      setSuccess(`Đã gửi yêu cầu #${created.id}.`);
      await loadTickets(created.id);
      setSelectedId(created.id);
      setDetail(created);
    } catch (err) {
      setError(supportApiError(err, 'Không thể gửi yêu cầu hỗ trợ.'));
    } finally {
      setWorking(false);
    }
  };

  const uploadSuspensionProof = async (file) => {
    if (!file) return;
    setUploadingProof(true);
    setError('');
    try {
      const uploaded = await supportApi.uploadCourseSuspensionProof(file);
      setSuspensionForm((current) => ({ ...current, proofUrl: uploaded.url }));
    } catch (err) {
      setError(supportApiError(err, 'Không thể tải giấy tờ minh chứng.'));
    } finally {
      setUploadingProof(false);
    }
  };

  const requestCourseReturn = async (suspensionRequestId) => {
    setWorking(true);
    setError('');
    try {
      await supportApi.requestCourseReturn(suspensionRequestId);
      setSuccess('Đã gửi yêu cầu xếp lớp để tiếp tục học.');
      await loadSuspensionData();
    } catch (err) {
      setError(supportApiError(err, 'Không thể gửi yêu cầu học lại.'));
    } finally {
      setWorking(false);
    }
  };

  const addComment = async (event) => {
    event.preventDefault();
    if (!comment.trim() || !detail) return;
    setWorking(true);
    setError('');
    try {
      const updated = await supportApi.replyAsLearner(detail.id, comment.trim());
      setComment('');
      setDetail(updated);
      await loadTickets(updated.id);
      setSuccess('Đã gửi phản hồi.');
    } catch (err) {
      setError(supportApiError(err, 'Không thể gửi phản hồi.'));
    } finally {
      setWorking(false);
    }
  };

  const changeStatus = async (status) => {
    if (!detail) return;
    setWorking(true);
    setError('');
    try {
      const updated = await supportApi.updateMyTicketStatus(detail.id, status);
      setDetail(updated);
      setSuccess('Đã đóng yêu cầu.');
      await loadTickets(updated.id);
    } catch (err) {
      setError(supportApiError(err, 'Không thể cập nhật trạng thái yêu cầu.'));
    } finally {
      setWorking(false);
    }
  };

  const messages = detail?.messages || [];
  const opening = messages[0] || null;
  const comments = messages.slice(1);

  return (
    <LearnerPageShell
      title="Trung tâm hỗ trợ"
      description="Gửi yêu cầu, theo dõi trạng thái và trao đổi với bộ phận hỗ trợ."
      actions={(
        <button
          className="inline-flex items-center gap-2 rounded-2xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white"
          onClick={() => {
            setShowForm((value) => !value);
            setSelectedId(null);
          }}
          type="button"
        >
          {showForm ? <XCircle className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
          {showForm ? 'Đóng biểu mẫu' : 'Gửi yêu cầu mới'}
        </button>
      )}
    >
      <div className="space-y-5">
        {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700">{error}</div> : null}
        {success ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700">{success}</div> : null}

        {/* 1. Form tạo ticket */}
        {showForm ? (
          <form className="rounded-[28px] border border-[#dfbfbd]/50 bg-white p-5 shadow-sm md:p-7" onSubmit={createTicket}>
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-2">
                <LifeBuoy className="h-5 w-5 text-[#730014]" />
                <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">Gửi yêu cầu hỗ trợ</h2>
              </div>
              <button
                aria-label="Đóng biểu mẫu"
                className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-[#dfbfbd]/60 bg-white text-[#8b706e] transition hover:border-[#730014] hover:bg-[#fff1f3] hover:text-[#730014]"
                onClick={() => setShowForm(false)}
                type="button"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <p className="mt-2 text-sm text-[#8b706e]">
              Mô tả vấn đề và cung cấp thông tin cần thiết để được hỗ trợ.
            </p>
            <div className={`mt-5 grid gap-4 ${isSuspensionForm ? '' : 'md:grid-cols-[1fr_240px]'}`}>
              {!isSuspensionForm ? (
                <label className="space-y-2 text-sm font-bold text-[#584140]">
                  Tiêu đề
                  <input
                    className="w-full rounded-2xl border border-[#dfbfbd] bg-[#fffafa] px-4 py-3 font-medium outline-none focus:border-[#730014]"
                    maxLength={160}
                    onChange={(event) => setForm({ ...form, subject: event.target.value })}
                    placeholder="Ví dụ: Không truy cập được khóa học đã mua"
                    value={form.subject}
                  />
                </label>
              ) : null}
              <label className="space-y-2 text-sm font-bold text-[#584140]">
                Nhóm vấn đề
                <BrandedSelect
                  onChange={(event) => {
                    setForm({ ...form, category: event.target.value });
                    if (event.target.value !== 'CLASSROOM') setClassroomIssue('GENERAL');
                  }}
                  options={supportCategoryOptions}
                  value={form.category}
                />
              </label>
            </div>
            {form.category === 'CLASSROOM' ? (
              <label className="mt-4 block space-y-2 text-sm font-bold text-[#584140]">
                Nội dung cần hỗ trợ
                <BrandedSelect
                  onChange={(event) => setClassroomIssue(event.target.value)}
                  options={[
                    { label: 'Hỗ trợ lớp học khác', value: 'GENERAL' },
                    { label: 'Bảo lưu khóa học', value: 'COURSE_SUSPENSION' },
                  ]}
                  value={classroomIssue}
                />
              </label>
            ) : null}
            {isSuspensionForm ? (
              <div className="mt-5 space-y-4 rounded-2xl border border-[#f0e4e2] bg-[#fffafa] p-4 sm:p-5">
                <div className="grid gap-4 md:grid-cols-2">
                  <label className="space-y-2 text-sm font-bold text-[#584140] md:col-span-2">
                    Lớp học đang học
                    <BrandedSelect
                      onChange={(event) => setSuspensionForm((current) => ({ ...current, enrollmentId: event.target.value }))}
                      options={suspensionEnrollments.filter((item) => item.eligible).map((item) => ({
                        label: item.classroomTitle,
                        value: String(item.enrollmentId),
                        description: `${item.courseTitle} · Đã học ${item.completedSessions}/${item.totalSessions} buổi`,
                      }))}
                      placeholder={suspensionEnrollments.some((item) => item.eligible) ? 'Chọn lớp cần bảo lưu' : 'Không có lớp đủ điều kiện'}
                      value={suspensionForm.enrollmentId}
                    />
                  </label>
                  <div className="space-y-2 text-sm font-bold text-[#584140]">
                    Ngày bắt đầu bảo lưu
                    <div className="rounded-2xl border border-[#ead8d6] bg-slate-50 px-4 py-3 font-medium text-[#584140]">
                      {new Intl.DateTimeFormat('vi-VN').format(new Date(`${suspensionForm.requestedStartDate}T00:00:00`))}
                    </div>
                  </div>
                  <label className="space-y-2 text-sm font-bold text-[#584140]">
                    Ngày dự kiến quay lại
                    <VietnameseDateInput
                      className="w-full rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 font-medium outline-none focus:border-[#730014]"
                      max={addMonthsToDateKey(suspensionForm.requestedStartDate || todayKey(), 3)}
                      min={suspensionForm.requestedStartDate || todayKey()}
                      onChange={(value) => setSuspensionForm((current) => ({ ...current, requestedReturnDate: value }))}
                      value={suspensionForm.requestedReturnDate}
                    />
                  </label>
                </div>
                <label className="block space-y-2 text-sm font-bold text-[#584140]">
                  Lý do bảo lưu
                  <textarea
                    className="min-h-28 w-full resize-y rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 font-medium leading-6 outline-none focus:border-[#730014]"
                    maxLength={2000}
                    onChange={(event) => setSuspensionForm((current) => ({ ...current, reason: event.target.value }))}
                    placeholder="Trình bày lý do sức khỏe, công tác, học tập hoặc trường hợp bất khả kháng..."
                    value={suspensionForm.reason}
                  />
                </label>
                <label className="flex cursor-pointer items-center justify-between gap-4 rounded-2xl border border-dashed border-[#cf9b9e] bg-white px-4 py-3 text-sm font-bold text-[#584140]">
                  <span className="flex min-w-0 items-center gap-2">
                    {suspensionForm.proofUrl ? <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" /> : <Upload className="h-4 w-4 shrink-0 text-[#730014]" />}
                    <span className="truncate">{suspensionForm.proofUrl ? 'Đã tải giấy tờ minh chứng' : 'Tải giấy tờ minh chứng'}</span>
                  </span>
                  <input
                    accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
                    className="sr-only"
                    disabled={uploadingProof}
                    onChange={(event) => uploadSuspensionProof(event.target.files?.[0])}
                    type="file"
                  />
                  <span className="shrink-0 text-xs text-[#8b706e]">{uploadingProof ? 'Đang tải...' : 'PDF, Word hoặc ảnh'}</span>
                </label>
                {suspensionEnrollments.filter((item) => !item.eligible).map((item) => (
                  <p className="text-xs text-[#8b706e]" key={item.enrollmentId}>
                    {item.classroomTitle}: {item.eligibilityMessage}
                  </p>
                ))}
              </div>
            ) : (
              <label className="mt-4 block space-y-2 text-sm font-bold text-[#584140]">
                Mô tả chi tiết
                <textarea
                  className="min-h-36 w-full resize-y rounded-2xl border border-[#dfbfbd] bg-[#fffafa] px-4 py-3 font-medium leading-6 outline-none focus:border-[#730014]"
                  maxLength={5000}
                  onChange={(event) => setForm({ ...form, message: event.target.value })}
                  placeholder="Mô tả rõ vấn đề, thời điểm xảy ra, thao tác đã thử..."
                  value={form.message}
                />
              </label>
            )}
            <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
              <p className="text-xs text-[#8b706e]">
                {isSuspensionForm ? 'Thời gian bảo lưu tối đa 3 tháng.' : 'Tiêu đề ≥ 5 ký tự · Mô tả ≥ 10 ký tự'}
              </p>
              <button
                className="inline-flex items-center gap-2 rounded-2xl bg-[#730014] px-5 py-3 text-sm font-bold text-white disabled:opacity-50"
                disabled={!canCreate || working}
                type="submit"
              >
                <Send className="h-4 w-4" />
                {working ? 'Đang gửi...' : 'Gửi yêu cầu'}
              </button>
            </div>
          </form>
        ) : null}

        {suspensionRequests.length ? (
          <section className="overflow-hidden rounded-[28px] border border-[#dfbfbd]/40 bg-white shadow-sm">
            <div className="flex items-center gap-2 border-b border-[#f0e4e2] px-5 py-4">
              <PauseCircle className="h-5 w-5 text-[#730014]" />
              <h2 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Bảo lưu khóa học</h2>
            </div>
            <div className="divide-y divide-[#f0e4e2]">
              {suspensionRequests.map((request) => {
                const oldValues = parseValues(request.oldValuesJson);
                const newValues = parseValues(request.newValuesJson);
                const isSuspension = request.requestType === 'SUSPEND_STUDENT';
                const hasReturnRequest = suspensionRequests.some((item) => (
                  item.requestType === 'RESUME_STUDENT'
                  && Number(parseValues(item.oldValuesJson).suspensionRequestId) === Number(request.id)
                  && ['PENDING', 'APPLIED'].includes(item.status)
                ));
                const canReturn = isSuspension
                  && request.status === 'APPLIED'
                  && newValues.requestedReturnDate >= todayKey()
                  && !hasReturnRequest;
                const returnExpired = isSuspension
                  && request.status === 'APPLIED'
                  && newValues.requestedReturnDate < todayKey()
                  && !hasReturnRequest;
                return (
                  <article className="flex flex-wrap items-center justify-between gap-4 px-5 py-4" key={request.id}>
                    <div className="min-w-0">
                      <p className="text-sm font-extrabold text-[#2b2828]">{request.requestTypeLabel} · #{request.id}</p>
                      <p className="mt-1 text-xs text-[#8b706e]">
                        {request.classroomTitle}
                        {oldValues.courseTitle ? ` · ${oldValues.courseTitle}` : ''}
                      </p>
                      {isSuspension ? (
                        <p className="mt-1 text-xs text-[#584140]">
                          Dự kiến quay lại: {newValues.requestedReturnDate || '—'}
                        </p>
                      ) : null}
                      {request.status === 'PENDING' && request.reviewerName ? (
                        <p className="mt-1 text-xs text-[#584140]">
                          Nhân viên phụ trách: <strong>{request.reviewerName}</strong>
                        </p>
                      ) : null}
                      {returnExpired ? (
                        <p className="mt-1 text-xs font-semibold text-rose-600">
                          Đã quá thời hạn đăng ký học lại. Vui lòng gửi yêu cầu hỗ trợ mới.
                        </p>
                      ) : null}
                    </div>
                    <div className="flex items-center gap-3">
                      <span className={`rounded-full border px-3 py-1 text-xs font-bold ${
                        request.status === 'APPLIED'
                          ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                          : request.status === 'REJECTED'
                            ? 'border-rose-200 bg-rose-50 text-rose-700'
                            : 'border-amber-200 bg-amber-50 text-amber-700'
                      }`}
                      >
                        {request.statusLabel}
                      </span>
                      {canReturn ? (
                        <button
                          className="inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4 py-2 text-xs font-bold text-white disabled:opacity-50"
                          disabled={working}
                          onClick={() => requestCourseReturn(request.id)}
                          type="button"
                        >
                          <RotateCcw className="h-4 w-4" />
                          Đăng ký học lại
                        </button>
                      ) : null}
                    </div>
                  </article>
                );
              })}
            </div>
          </section>
        ) : null}

        {/* 2. Danh sách ticket của người dùng */}
        <section className="overflow-hidden rounded-[28px] border border-[#dfbfbd]/40 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-[#f0e4e2] px-5 py-4">
            <div className="flex items-center gap-2">
              <ClipboardList className="h-5 w-5 text-[#730014]" />
              <h2 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Yêu cầu của tôi</h2>
            </div>
            <span className="text-xs font-bold text-[#8b706e]">{tickets.length} yêu cầu</span>
          </div>

          {loading ? (
            <p className="px-5 py-10 text-sm text-[#8b706e]">Đang tải danh sách...</p>
          ) : tickets.length === 0 ? (
            <div className="px-5 py-12 text-center text-sm text-[#8b706e]">
              <LifeBuoy className="mx-auto mb-3 h-8 w-8 opacity-60" />
              Bạn chưa gửi yêu cầu hỗ trợ nào.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full text-left">
                <thead className="bg-[#fff8f8] text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">
                  <tr>
                    <th className="px-5 py-3">Mã</th>
                    <th className="px-5 py-3">Tiêu đề</th>
                    <th className="px-5 py-3">Nhóm</th>
                    <th className="px-5 py-3">Trạng thái</th>
                    <th className="px-5 py-3">Cập nhật</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#f0e4e2]">
                  {tickets.map((ticket) => (
                    <tr
                      className={`cursor-pointer transition hover:bg-[#fffafa] ${selectedId === ticket.id ? 'bg-[#fff3f4]' : ''}`}
                      key={ticket.id}
                      onClick={() => {
                        setSelectedId(ticket.id);
                        setShowForm(false);
                      }}
                    >
                      <td className="px-5 py-3.5 text-sm font-extrabold text-[#730014]">#{ticket.id}</td>
                      <td className="px-5 py-3.5 text-sm font-bold text-[#2b2828]">{ticket.subject}</td>
                      <td className="px-5 py-3.5 text-sm text-[#584140]">{supportCategoryLabels[ticket.category]}</td>
                      <td className="px-5 py-3.5">
                        <span className={`rounded-full border px-2.5 py-0.5 text-[11px] font-bold ${supportStatusClasses[ticket.status]}`}>
                          {supportStatusLabels[ticket.status]}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-xs text-[#8b706e]">{formatSupportTime(ticket.updatedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* 3. Chi tiết + theo dõi trạng thái + comment */}
        {selectedId ? (
          <section className="rounded-[28px] border border-[#dfbfbd]/40 bg-white p-5 shadow-sm md:p-7">
            {detailLoading ? <p className="text-sm text-[#8b706e]">Đang tải chi tiết yêu cầu...</p> : null}
            {!detailLoading && detail ? (
              <div className="space-y-6">
                <div className="flex flex-wrap items-start justify-between gap-4 border-b border-[#f0e4e2] pb-5">
                  <div>
                    <p className="text-xs font-extrabold uppercase tracking-wider text-[#730014]">
                      Yêu cầu #{detail.id} · {supportCategoryLabels[detail.category]}
                    </p>
                    <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{detail.subject}</h2>
                    <p className="mt-2 text-xs text-[#8b706e]">
                      Tạo: {formatSupportTime(detail.createdAt)} · Ưu tiên: {supportPriorityLabels[detail.priority]}
                      {detail.assigneeName ? ` · Người xử lý: ${detail.assigneeName}` : ' · Chưa có người xử lý'}
                    </p>
                  </div>
                  <div className="flex flex-col items-end gap-2">
                    <span className={`rounded-full border px-3 py-1 text-xs font-bold ${supportStatusClasses[detail.status]}`}>
                      {supportStatusLabels[detail.status]}
                    </span>
                    {!isSupportTicketTerminal(detail.status) ? (
                      <button
                        className="text-xs font-bold text-slate-500 hover:text-rose-700"
                        disabled={working}
                        onClick={() => changeStatus('CLOSED')}
                        type="button"
                      >
                        Đóng yêu cầu
                      </button>
                    ) : null}
                  </div>
                </div>

                {/* Mô tả ban đầu từ form */}
                <div className="rounded-2xl border border-[#f0e4e2] bg-[#fffafa] p-4">
                  <p className="text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">Nội dung yêu cầu</p>
                  <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-[#2b2828]">
                    {opening?.body || 'Không có mô tả.'}
                  </p>
                  <p className="mt-3 text-[11px] text-[#8b706e]">
                    {opening ? `${opening.authorName || 'Bạn'} · ${formatSupportTime(opening.createdAt)}` : null}
                  </p>
                </div>

                {/* Timeline trạng thái gọn */}
                <div className="grid gap-2 sm:grid-cols-4">
                  {[
                    { key: 'OPEN', label: 'Đã gửi' },
                    { key: 'IN_PROGRESS', label: 'Đang xử lý' },
                    { key: 'WAITING_FOR_LEARNER', label: 'Chờ phản hồi' },
                    { key: 'RESOLVED', label: 'Hoàn tất' },
                  ].map((step) => {
                    const active = detail.status === step.key
                      || (step.key === 'RESOLVED' && isSupportTicketTerminal(detail.status))
                      || (step.key === 'IN_PROGRESS' && detail.status === 'WAITING_FOR_LEARNER');
                    return (
                      <div
                        key={step.key}
                        className={`rounded-xl border px-3 py-2 text-center text-[11px] font-bold ${
                          active ? 'border-[#730014] bg-[#fff1f3] text-[#730014]' : 'border-slate-100 bg-slate-50 text-slate-400'
                        }`}
                      >
                        {step.label}
                      </div>
                    );
                  })}
                </div>

                {/* Comments (không phải chat bubble) */}
                <div>
                  <div className="mb-3 flex items-center gap-2">
                    <MessageSquarePlus className="h-4 w-4 text-[#730014]" />
                    <h3 className="text-sm font-extrabold text-[#2b2828]">Trao đổi</h3>
                    <span className="text-xs text-[#8b706e]">({comments.length})</span>
                  </div>

                  {comments.length === 0 ? (
                    <p className="rounded-2xl border border-dashed border-[#dfbfbd] px-4 py-6 text-center text-sm text-[#8b706e]">
                      Chưa có phản hồi. Bạn có thể bổ sung thông tin tại đây.
                    </p>
                  ) : (
                    <div className="space-y-3">
                      {comments.map((item) => (
                        <article key={item.id} className="rounded-2xl border border-[#f0e4e2] bg-white p-4">
                          <div className="flex flex-wrap items-center justify-between gap-2">
                            <p className="text-xs font-extrabold text-[#2b2828]">
                              {item.staffMessage ? `Bộ phận hỗ trợ · ${item.authorName}` : 'Bạn'}
                              {item.staffMessage ? (
                                <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-bold text-slate-600">
                                  Đội hỗ trợ
                                </span>
                              ) : (
                                <span className="ml-2 rounded-full bg-[#fff1f3] px-2 py-0.5 text-[10px] font-bold text-[#730014]">
                                  Học viên
                                </span>
                              )}
                            </p>
                            <p className="text-[11px] text-[#8b706e]">{formatSupportTime(item.createdAt)}</p>
                          </div>
                          <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-[#584140]">{item.body}</p>
                        </article>
                      ))}
                    </div>
                  )}

                  {!isSupportTicketTerminal(detail.status) ? (
                    <form className="mt-4 space-y-3" onSubmit={addComment}>
                      <label className="block space-y-2 text-sm font-bold text-[#584140]">
                        Phản hồi
                        <textarea
                          className="min-h-24 w-full resize-y rounded-2xl border border-[#dfbfbd] bg-[#fffafa] px-4 py-3 text-sm leading-6 outline-none focus:border-[#730014]"
                          maxLength={5000}
                          onChange={(event) => setComment(event.target.value)}
                          placeholder="Bổ sung thông tin hoặc gửi phản hồi..."
                          value={comment}
                        />
                      </label>
                      <div className="flex justify-end">
                        <button
                          className="inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white disabled:opacity-50"
                          disabled={!comment.trim() || working}
                          type="submit"
                        >
                          <Send className="h-4 w-4" />
                          {working ? 'Đang gửi...' : 'Gửi phản hồi'}
                        </button>
                      </div>
                    </form>
                  ) : (
                    <p className="mt-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm font-semibold text-slate-600">
                      Yêu cầu đã được đóng. Hãy gửi yêu cầu mới nếu bạn cần hỗ trợ thêm.
                    </p>
                  )}
                </div>
              </div>
            ) : null}
          </section>
        ) : null}
      </div>
    </LearnerPageShell>
  );
}
