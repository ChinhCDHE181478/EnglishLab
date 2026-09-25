import { useEffect, useMemo, useState } from 'react';
import { BookOpenCheck, CalendarClock, CheckCircle2, RefreshCw, XCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import { EnrollmentRequestTimeline, EnrollmentStatusBadge } from '../../components/classroom/EnrollmentRequestUi';
import TuitionPaymentSection from '../../components/classroom/TuitionPaymentSection';
import LearnerPageShell from '../../components/learner/LearnerPageShell';
import BrandedSelect from '../../components/ui/BrandedSelect';
import ManagementToast from '../../components/ui/ManagementToast';
import { formatClassroomDateTime } from '../../utils/classroomHelpers';

const statusOptions = [
  { label: 'Tất cả trạng thái', value: 'ALL' },
  { label: 'Mới đăng ký', value: 'SUBMITTED' },
  { label: 'Đã gửi lời mời', value: 'INVITATION_SENT' },
  { label: 'Đã hẹn lịch test', value: 'TEST_SCHEDULED' },
  { label: 'Chờ xác nhận khóa đề xuất', value: 'CLASS_PROPOSED' },
  { label: 'Đủ điều kiện - chờ xếp lớp', value: 'WAITING_FOR_CLASS' },
  { label: 'Đã chọn lớp - chờ học phí', value: 'CLASS_ASSIGNED' },
  { label: 'Đã kết thúc', value: 'CLOSED' },
];

export default function MyEnrollmentRequestsPage() {
  const [requests, setRequests] = useState([]);
  const [status, setStatus] = useState('ALL');
  const [expandedId, setExpandedId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [workingId, setWorkingId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      setRequests(await enrollmentRequestApi.listMine());
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải các yêu cầu đăng ký.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const respondToRecommendation = async (request, accepted) => {
    setWorkingId(request.id);
    setError('');
    try {
      const updated = accepted
        ? await enrollmentRequestApi.acceptCourseRecommendation(request.id)
        : await enrollmentRequestApi.declineCourseRecommendation(request.id);
      setRequests((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      setSuccess(accepted
        ? 'Đã xác nhận khóa học đề xuất. Trung tâm sẽ tiếp tục xếp lớp phù hợp.'
        : 'Đã ghi nhận bạn không tham gia khóa học được đề xuất.');
    } catch (requestError) {
      setError(requestError?.response?.data?.message || 'Không thể ghi nhận phản hồi. Vui lòng thử lại.');
    } finally {
      setWorkingId(null);
    }
  };

  const filteredRequests = useMemo(() => requests.filter((item) => {
    if (status === 'ALL') return true;
    if (status === 'CLOSED') return ['REJECTED', 'CANCELLED'].includes(item.status);
    return item.status === status;
  }), [requests, status]);

  return (
    <LearnerPageShell
      actions={(
        <div className="flex gap-2">
          <div className="min-w-[240px]"><BrandedSelect onChange={(event) => setStatus(event.target.value)} options={statusOptions} value={status} /></div>
          <button aria-label="Tải lại" className="flex h-12 w-12 items-center justify-center rounded-xl border border-slate-200 bg-white text-[#730014] shadow-sm" disabled={loading} onClick={load} type="button"><RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /></button>
        </div>
      )}
      description="Theo dõi thư mời, lịch đến trung tâm, kết quả test đầu vào và trạng thái xếp lớp của bạn."
      eyebrow="Theo dõi tư vấn & xếp lớp"
      title="Form đăng ký của tôi"
    >
      <div className="flex flex-1 flex-col">

          <ManagementToast message={success} onClose={() => setSuccess('')} tone="success" title="Đã cập nhật yêu cầu" />

          {error ? <div className="mt-5 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-700">{error}</div> : null}

          {loading ? <div className="mt-6 space-y-4">{Array.from({ length: 3 }).map((_, index) => <div className="h-52 animate-pulse rounded-2xl bg-slate-100" key={index} />)}</div> : null}

          {!loading && !filteredRequests.length ? (
            <section className="mt-6 flex min-h-[420px] flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white px-6 text-center">
              <BookOpenCheck className="h-12 w-12 text-slate-300" />
              <h2 className="mt-4 font-['Manrope'] text-xl font-black text-[#0b1c30]">Chưa có yêu cầu phù hợp</h2>
              <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">Chọn một khóa học đang nhận đăng ký để được tư vấn và hẹn lịch đánh giá đầu vào.</p>
              <Link className="mt-5 rounded-xl bg-[#730014] px-5 py-3 text-sm font-extrabold text-white" to="/opening-schedule#dang-ky-tu-van">Đăng ký học</Link>
            </section>
          ) : null}

          {!loading && filteredRequests.length ? (
            <div className="mt-6 space-y-4">
              {filteredRequests.map((request) => {
                const expanded = expandedId === request.id;
                return (
                  <article className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm" key={request.id}>
                    <div className="grid gap-5 p-5 md:grid-cols-[minmax(0,1fr)_auto] md:p-6">
                      <div>
                        <div className="flex flex-wrap items-center gap-2"><EnrollmentStatusBadge label={request.statusLabel} status={request.status} /><span className="text-xs font-bold text-slate-400">Yêu cầu #{request.id}</span></div>
                        <h2 className="mt-3 font-['Manrope'] text-xl font-black text-[#0b1c30]">{request.courseOfferingTitle || 'Đăng ký học và nhận tư vấn'}</h2>
                        <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-sm text-slate-500">
                          <span className="font-bold">{formatConsultationTrack(request.consultationTrack)}</span>
                          <span>{request.contactPhone || 'Chưa có số điện thoại'}</span>
                          {request.studyWorkGoal ? <span>{request.studyWorkGoal}</span> : null}
                        </div>
                        {request.testAppointmentAt ? (
                          <div className="mt-4 flex items-start gap-2 rounded-xl border border-sky-200 bg-sky-50 px-4 py-3 text-sm text-sky-900">
                            <CalendarClock className="mt-0.5 h-4 w-4 shrink-0" />
                            <span><strong>Lịch đến trung tâm:</strong> {formatClassroomDateTime(request.testAppointmentAt)}{request.testLocation ? ` · ${request.testLocation}` : ''}</span>
                          </div>
                        ) : null}
                        {request.assignedEnrollment ? (
                          <div className="mt-4 space-y-3">
                            <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
                              <p className="font-bold">Lớp đã chọn: {request.assignedClassroomTitle || `#${request.assignedClassroomId}`}</p>
                            </div>
                            <TuitionPaymentSection
                              canSubmitProof={['PENDING_TUITION_PAYMENT', 'DEPOSIT_PAID', 'PARTIALLY_PAID'].includes(request.assignedEnrollment.registrationStatus)}
                              classroomId={request.assignedClassroomId}
                              compact
                              onUpdated={load}
                              tuitionDepositRemaining={request.assignedEnrollment.tuitionDepositRemaining}
                              tuitionFullPaymentRequired={Boolean(request.assignedEnrollment.tuitionFullPaymentRequired)}
                              hasPendingPayosPayment={Boolean(request.assignedEnrollment.hasPendingPayosPayment)}
                              tuitionPaymentDeadline={request.assignedEnrollment.tuitionPaymentDeadline}
                              tuitionPaymentOverdue={Boolean(request.assignedEnrollment.tuitionPaymentOverdue)}
                              tuitionRemaining={Math.max(0, Number(request.assignedEnrollment.tuitionAmountDue || 0) - Number(request.assignedEnrollment.tuitionAmountPaid || 0))}
                            />
                          </div>
                        ) : null}
                        {request.status === 'CLASS_PROPOSED' ? (
                          <div className="mt-4 rounded-2xl border border-indigo-200 bg-indigo-50 p-4">
                            <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-indigo-700">Khóa học được đề xuất</p>
                            <p className="mt-1 font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">{request.courseOfferingTitle}</p>
                            <div className="mt-4 flex flex-wrap gap-2">
                              <button
                                className="inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4 py-2.5 text-xs font-extrabold text-white disabled:opacity-60"
                                disabled={workingId === request.id}
                                onClick={() => respondToRecommendation(request, true)}
                                type="button"
                              >
                                <CheckCircle2 className="h-4 w-4" />
                                Đồng ý khóa học này
                              </button>
                              <button
                                className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-extrabold text-slate-700 disabled:opacity-60"
                                disabled={workingId === request.id}
                                onClick={() => respondToRecommendation(request, false)}
                                type="button"
                              >
                                <XCircle className="h-4 w-4" />
                                Từ chối đề xuất
                              </button>
                            </div>
                          </div>
                        ) : null}
                      </div>
                      <div className="flex flex-wrap items-center gap-2 md:justify-end">
                        <button className="rounded-xl bg-[#730014] px-4 py-2.5 text-xs font-extrabold text-white" onClick={() => setExpandedId(expanded ? null : request.id)} type="button">{expanded ? 'Thu gọn' : 'Xem cập nhật'}</button>
                      </div>
                    </div>
                    <div className="border-t border-slate-100 bg-slate-50/70 px-5 py-4 text-sm leading-6 text-slate-600 md:px-6">{requestGuidance(request)}</div>
                    {expanded ? <div className="border-t border-slate-100 p-5 md:p-6"><EnrollmentRequestTimeline history={request.history} /></div> : null}
                  </article>
                );
              })}
            </div>
          ) : null}
      </div>
    </LearnerPageShell>
  );
}

function formatConsultationTrack(value) {
  return {
    IELTS_4_SKILLS: 'IELTS 4 kỹ năng',
    TOEIC_2_SKILLS: 'TOEIC 2 kỹ năng',
    TOEIC_4_SKILLS: 'TOEIC 4 kỹ năng',
    ENGLISH_FOUNDATION: 'Tiếng Anh nền tảng',
  }[value] || 'Lộ trình sẽ được tư vấn theo nhu cầu';
}

function requestGuidance(request) {
  const enrollment = request.assignedEnrollment;
  if (request.status === 'CLASS_ASSIGNED' && enrollment?.hasPendingTuitionProof) {
    return 'Trung tâm đã nhận minh chứng học phí. Nhân viên đào tạo sẽ kiểm tra và xác nhận khoản thanh toán.';
  }
  if (request.status === 'CLASS_ASSIGNED' && enrollment?.hasPendingPayosPayment) {
    return 'Đơn PayOS đang được xử lý. Học phí sẽ tự cập nhật sau khi PayOS xác nhận giao dịch thành công.';
  }
  if (request.status === 'CLASS_ASSIGNED' && enrollment?.registrationStatus === 'ASSIGNED') {
    return 'Học phí đã được xác nhận. Bạn đã được vào lớp và có thể theo dõi lịch học trong mục Lớp của tôi.';
  }
  return {
    SUBMITTED: 'Hồ sơ đã được tiếp nhận. Đội ngũ tư vấn sẽ liên hệ qua email hoặc số điện thoại bạn cung cấp.',
    INVITATION_SENT: 'Lời mời tư vấn và đánh giá đầu vào đã được gửi. Vui lòng kiểm tra email của bạn.',
    TEST_SCHEDULED: request.testAppointmentAt
      ? 'Lịch hẹn đã được xác nhận. Vui lòng có mặt đúng địa điểm và mang theo thông tin cần thiết.'
      : 'Lịch hẹn đã được xác nhận. Vui lòng kiểm tra email để xem chi tiết.',
    WAITING_FOR_CLASS: 'Kết quả đầu vào đã được ghi nhận. Hồ sơ đang chờ lớp phù hợp.',
    CLASS_PROPOSED: 'Vui lòng xác nhận khóa học được đề xuất để trung tâm tiếp tục xếp lớp.',
    CLASS_ASSIGNED: 'Lớp phù hợp đã được chọn. Vào mục Lớp của tôi để hoàn tất học phí và theo dõi trạng thái.',
    REJECTED: 'Hồ sơ này đã kết thúc. Bạn có thể đăng ký một khóa học khác.',
    CANCELLED: 'Hồ sơ này đã được hủy.',
  }[request.status] || 'Các cập nhật mới nhất của hồ sơ sẽ hiển thị tại đây.';
}
