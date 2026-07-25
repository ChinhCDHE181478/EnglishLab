import { useEffect, useMemo, useState } from 'react';
import { BookOpenCheck, CalendarClock, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import Header from '../../components/ai-learning/Header';
import { EnrollmentRequestTimeline, EnrollmentStatusBadge } from '../../components/classroom/EnrollmentRequestUi';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { formatClassroomDateTime } from '../../utils/classroomHelpers';
import { PAGE_BODY_CLASS, PAGE_CONTAINER_CLASS, PAGE_SHELL_CLASS } from '../../utils/pageLayout';

const statusOptions = [
  { label: 'Tất cả trạng thái', value: 'ALL' },
  { label: 'Mới đăng ký', value: 'SUBMITTED' },
  { label: 'Đã gửi lời mời', value: 'INVITATION_SENT' },
  { label: 'Đã hẹn lịch test', value: 'TEST_SCHEDULED' },
  { label: 'Đủ điều kiện - chờ xếp lớp', value: 'WAITING_FOR_CLASS' },
  { label: 'Hoàn tất - Đã xếp lớp', value: 'CLASS_ASSIGNED' },
  { label: 'Đã kết thúc', value: 'CLOSED' },
];

export default function MyEnrollmentRequestsPage() {
  const [requests, setRequests] = useState([]);
  const [status, setStatus] = useState('ALL');
  const [expandedId, setExpandedId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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

  const filteredRequests = useMemo(() => requests.filter((item) => {
    if (status === 'ALL') return true;
    if (status === 'CLOSED') return ['REJECTED', 'CANCELLED'].includes(item.status);
    return item.status === status;
  }), [requests, status]);

  return (
    <div className={PAGE_SHELL_CLASS}>
      <CourseGlobalStyles />
      <Header />
      <div className={PAGE_BODY_CLASS}>
        <main className={`${PAGE_CONTAINER_CLASS} flex-1 py-8 md:py-10`}>
          <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
            <div>
              <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">Theo dõi tư vấn & xếp lớp</p>
              <h1 className="mt-2 font-['Manrope'] text-3xl font-black text-[#0b1c30]">Form đăng ký của tôi</h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">Theo dõi thư mời, lịch đến trung tâm, kết quả test đầu vào và trạng thái xếp lớp của bạn.</p>
            </div>
            <div className="flex gap-2">
              <div className="min-w-[240px]"><BrandedSelect onChange={(event) => setStatus(event.target.value)} options={statusOptions} value={status} /></div>
              <button aria-label="Tải lại" className="flex h-12 w-12 items-center justify-center rounded-xl border border-slate-200 bg-white text-[#730014] shadow-sm" disabled={loading} onClick={load} type="button"><RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /></button>
            </div>
          </div>

          {error ? <div className="mt-5 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-700">{error}</div> : null}

          {loading ? <div className="mt-6 space-y-4">{Array.from({ length: 3 }).map((_, index) => <div className="h-52 animate-pulse rounded-2xl bg-slate-100" key={index} />)}</div> : null}

          {!loading && !filteredRequests.length ? (
            <section className="mt-6 flex min-h-[420px] flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white px-6 text-center">
              <BookOpenCheck className="h-12 w-12 text-slate-300" />
              <h2 className="mt-4 font-['Manrope'] text-xl font-black text-[#0b1c30]">Chưa có yêu cầu phù hợp</h2>
              <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">Chọn một khóa học đang nhận đăng ký để Staff liên hệ và hẹn lịch test.</p>
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
                      </div>
                      <div className="flex flex-wrap items-center gap-2 md:justify-end">
                        <button className="rounded-xl bg-[#730014] px-4 py-2.5 text-xs font-extrabold text-white" onClick={() => setExpandedId(expanded ? null : request.id)} type="button">{expanded ? 'Thu gọn' : 'Xem timeline'}</button>
                      </div>
                    </div>
                    <div className="border-t border-slate-100 bg-slate-50/70 px-5 py-4 text-sm leading-6 text-slate-600 md:px-6">{request.staffNote ? `Ghi chú từ Staff: ${request.staffNote}` : 'Staff sẽ gửi email và gọi điện để thống nhất lịch tư vấn, test đầu vào.'}</div>
                    {expanded ? <div className="border-t border-slate-100 p-5 md:p-6"><EnrollmentRequestTimeline history={request.history} /></div> : null}
                  </article>
                );
              })}
            </div>
          ) : null}
        </main>
      </div>
      <CourseFooter />
    </div>
  );
}

function formatConsultationTrack(value) {
  return {
    IELTS_4_SKILLS: 'IELTS 4 kỹ năng',
    TOEIC_2_SKILLS: 'TOEIC 2 kỹ năng',
    TOEIC_4_SKILLS: 'TOEIC 4 kỹ năng',
    ENGLISH_FOUNDATION: 'Tiếng Anh nền tảng',
  }[value] || 'Lộ trình đang được Staff tư vấn';
}
