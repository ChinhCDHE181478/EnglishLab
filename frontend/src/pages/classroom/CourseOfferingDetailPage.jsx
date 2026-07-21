import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, BookOpenCheck, Building2, CalendarDays, CheckCircle2, Clock3, RefreshCw, Send, ShieldCheck, Users, Video } from 'lucide-react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import Header from '../../components/ai-learning/Header';
import { EnrollmentRequestTimeline, EnrollmentStatusBadge, PlacementRequirements } from '../../components/classroom/EnrollmentRequestUi';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import { useAuth } from '../../context/AuthContext';
import { hasAnyUserRole } from '../../utils/auth';
import { PAGE_BODY_CLASS, PAGE_CONTAINER_CLASS, PAGE_SHELL_CLASS } from '../../utils/pageLayout';

const terminalStatuses = new Set(['REJECTED', 'CANCELLED', 'CLASS_ASSIGNED']);

const formatDate = (value) => (value
  ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN')
  : 'Đang cập nhật');

const formatPrice = (value) => new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
}).format(Number(value || 0));

export default function CourseOfferingDetailPage() {
  const { slugOrId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, user } = useAuth();
  const [offering, setOffering] = useState(null);
  const [requests, setRequests] = useState([]);
  const [form, setForm] = useState({ preferredSchedule: '', campusPreference: '', note: '' });
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const isLearner = hasAnyUserRole(user, ['LEARNER']);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const offeringData = await enrollmentRequestApi.getCourseOffering(slugOrId);
      let requestData = [];
      if (isAuthenticated && isLearner) {
        requestData = await enrollmentRequestApi.listMine();
      }
      setOffering(offeringData);
      setRequests(requestData);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải thông tin khóa học.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [isAuthenticated, isLearner, slugOrId]);

  const activeRequest = useMemo(() => requests.find((item) => (
    String(item.courseOfferingId) === String(offering?.id) && !terminalStatuses.has(item.status)
  )) || requests.find((item) => String(item.courseOfferingId) === String(offering?.id)) || null, [offering?.id, requests]);

  const updateForm = (key, value) => setForm((current) => ({ ...current, [key]: value }));

  const submitRequest = async () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location.pathname } });
      return;
    }
    if (!isLearner) {
      setError('Chỉ tài khoản học viên mới có thể gửi yêu cầu đăng ký.');
      return;
    }
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const created = await enrollmentRequestApi.submit({
        courseOfferingId: offering.id,
        preferredSchedule: form.preferredSchedule.trim() || null,
        campusPreference: offering.deliveryType === 'OFFLINE' ? form.campusPreference.trim() || null : null,
        note: form.note.trim() || null,
      });
      setRequests((current) => [created, ...current]);
      setSuccess('Đã gửi yêu cầu. Đây chưa phải xác nhận xếp lớp; Nhân viên đào tạo sẽ tiếp tục rà soát.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể gửi yêu cầu đăng ký.');
    } finally {
      setWorking(false);
    }
  };

  const refreshPlacement = async () => {
    if (!activeRequest) return;
    setWorking(true);
    setError('');
    try {
      const updated = await enrollmentRequestApi.refreshPlacement(activeRequest.id);
      setRequests((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      setSuccess(updated.status === 'UNDER_STAFF_REVIEW'
        ? 'Placement test đã đủ điều kiện và yêu cầu đã được chuyển cho Nhân viên đào tạo.'
        : 'Đã kiểm tra lại. Placement test vẫn còn điều kiện cần hoàn tất.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể kiểm tra lại placement test.');
    } finally {
      setWorking(false);
    }
  };

  const cancelRequest = async () => {
    if (!activeRequest || !window.confirm('Bạn có chắc muốn hủy yêu cầu đăng ký này?')) return;
    setWorking(true);
    setError('');
    try {
      const updated = await enrollmentRequestApi.cancel(activeRequest.id);
      setRequests((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      setSuccess('Đã hủy yêu cầu đăng ký.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể hủy yêu cầu.');
    } finally {
      setWorking(false);
    }
  };

  if (loading) {
    return <PageFrame><div className={`${PAGE_CONTAINER_CLASS} flex min-h-[560px] items-center justify-center`}><div className="h-12 w-12 animate-spin rounded-full border-4 border-slate-200 border-t-[#730014]" /></div></PageFrame>;
  }

  if (!offering || error && !offering) {
    return <PageFrame><div className={`${PAGE_CONTAINER_CLASS} flex min-h-[560px] flex-col items-center justify-center text-center`}><BookOpenCheck className="h-12 w-12 text-slate-300" /><h1 className="mt-4 text-xl font-black text-[#0b1c30]">Không mở được khóa học</h1><p className="mt-2 text-sm text-rose-600">{error || 'Không tìm thấy khóa học.'}</p><Link className="mt-5 rounded-xl bg-[#730014] px-5 py-2.5 text-sm font-bold text-white" to="/opening-schedule">Quay lại danh sách</Link></div></PageFrame>;
  }

  const deliveryType = offering.deliveryType || offering.deliveryMode;
  const isVirtual = deliveryType === 'VIRTUAL';
  const displayedPrice = offering.salePrice ?? offering.price;

  return (
    <PageFrame>
      <main className={`${PAGE_CONTAINER_CLASS} flex-1 py-7 md:py-10`}>
        <Link className="inline-flex items-center gap-2 text-sm font-extrabold text-[#730014]" to="/opening-schedule"><ArrowLeft className="h-4 w-4" />Danh sách khóa học</Link>
        <section className="mt-5 overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
          <div className={`relative px-6 py-10 text-white md:px-10 ${isVirtual ? 'bg-[linear-gradient(125deg,#0b1c30,#3156a3)]' : 'bg-[linear-gradient(125deg,#4b0009,#a61b32)]'}`}>
            <div className="max-w-3xl">
              <span className="inline-flex items-center gap-2 rounded-full bg-white/15 px-3 py-1 text-xs font-extrabold uppercase tracking-[0.12em] backdrop-blur">
                {isVirtual ? <Video className="h-4 w-4" /> : <Building2 className="h-4 w-4" />}{isVirtual ? 'Khóa học Virtual' : 'Khóa học Offline'}
              </span>
              <h1 className="mt-5 font-['Manrope'] text-3xl font-black leading-tight md:text-5xl">{offering.title}</h1>
              <p className="mt-4 max-w-2xl text-sm leading-7 text-white/80 md:text-base">{offering.shortDescription || offering.description || 'Khóa học theo chương trình đào tạo đã được duyệt, có lộ trình và hỗ trợ phân lớp phù hợp.'}</p>
            </div>
          </div>
          <div className="grid divide-y divide-slate-100 md:grid-cols-4 md:divide-x md:divide-y-0">
            <HeroInfo icon={CalendarDays} label="Khai giảng dự kiến" value={formatDate(offering.plannedStartDate)} />
            <HeroInfo icon={Clock3} label="Lịch học dự kiến" value={offering.plannedSchedule || offering.studyMode || 'Đang cập nhật'} />
            <HeroInfo icon={Users} label="Quy mô dự kiến" value={`${offering.capacity ?? offering.maxCapacity ?? 30} học viên`} />
            <HeroInfo icon={BookOpenCheck} label="Trình độ đầu vào" value={offering.entryLevel || 'Sẽ xác định qua placement'} />
          </div>
        </section>

        {error ? <div className="mt-5 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-700">{error}</div> : null}
        {success ? <div className="mt-5 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-700">{success}</div> : null}

        <div className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_390px]">
          <div className="space-y-6">
            <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
              <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">Nội dung chuyên môn</p>
              <h2 className="mt-2 font-['Manrope'] text-2xl font-black text-[#0b1c30]">Chương trình đào tạo áp dụng</h2>
              <p className="mt-3 text-lg font-extrabold text-slate-800">{offering.curriculumProgramTitle || 'Đang cập nhật'}</p>
              <p className="mt-2 text-sm leading-7 text-slate-600">{offering.description || offering.targetOutcome || 'Nội dung cụ thể được triển khai từ chương trình đào tạo đã xuất bản và sẽ được Staff dùng khi đề xuất lớp.'}</p>
              <div className="mt-6 grid gap-3 sm:grid-cols-2">
                <DetailItem label="Mục tiêu" value={offering.targetScore || offering.targetOutcome || 'Theo lộ trình'} />
                <DetailItem label="Thời lượng" value={offering.duration || 'Đang cập nhật'} />
                <DetailItem label="Cách tổ chức" value={offering.studyMode || (isVirtual ? 'Virtual với giảng viên' : 'Offline tại trung tâm')} />
                <DetailItem label="Mã khóa học" value={offering.code} />
              </div>
            </section>

            <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
              <div className="flex items-start gap-3">
                <ShieldCheck className="mt-0.5 h-6 w-6 text-[#8a0018]" />
                <div>
                  <h2 className="font-['Manrope'] text-xl font-black text-[#0b1c30]">Quy trình sau khi gửi yêu cầu</h2>
                  <p className="mt-1 text-sm leading-6 text-slate-500">Học viên không tự vào lớp. Mỗi bước đều được kiểm tra và có lịch sử trạng thái.</p>
                </div>
              </div>
              <div className="mt-6 grid gap-3 md:grid-cols-3">
                <ProcessStep number="01" title="Kiểm tra placement" text="Hệ thống và Staff xác nhận kết quả đủ điều kiện." />
                <ProcessStep number="02" title="Xếp waiting pool" text="Staff chốt trình độ, ca học và nhóm phù hợp." />
                <ProcessStep number="03" title="Manager duyệt lớp" text="Chỉ sau khi duyệt bạn mới được gán vào lớp chính thức." />
              </div>
            </section>

            {activeRequest ? (
              <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div><p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">Yêu cầu của bạn</p><h2 className="mt-2 font-['Manrope'] text-xl font-black text-[#0b1c30]">Lịch sử xử lý</h2></div>
                  <EnrollmentStatusBadge label={activeRequest.statusLabel} status={activeRequest.status} />
                </div>
                <div className="mt-6"><EnrollmentRequestTimeline history={activeRequest.history} /></div>
              </section>
            ) : null}
          </div>

          <aside className="h-fit lg:sticky lg:top-24">
            {activeRequest ? (
              <section className="rounded-2xl border border-[#d8c0c3] bg-white p-6 shadow-lg shadow-[#730014]/5">
                <EnrollmentStatusBadge label={activeRequest.statusLabel} status={activeRequest.status} />
                <h2 className="mt-4 font-['Manrope'] text-xl font-black text-[#0b1c30]">Yêu cầu #{activeRequest.id}</h2>
                <p className="mt-2 text-sm leading-6 text-slate-500">Yêu cầu đang được xử lý và chưa đồng nghĩa với việc đã được xếp lớp.</p>
                <div className="mt-5 rounded-xl bg-amber-50 p-4"><PlacementRequirements eligibility={activeRequest.placementEligibility} /></div>
                {activeRequest.status === 'AWAITING_PLACEMENT_TEST' ? (
                  <div className="mt-4 grid gap-2">
                    <Link className="inline-flex items-center justify-center rounded-xl bg-[#730014] px-4 py-3 text-sm font-extrabold text-white" to="/placement-test">Mở placement test</Link>
                    <button className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 px-4 py-3 text-sm font-extrabold text-slate-700" disabled={working} onClick={refreshPlacement} type="button"><RefreshCw className="h-4 w-4" />Kiểm tra lại kết quả</button>
                  </div>
                ) : null}
                {!['CLASS_PROPOSED', 'CLASS_ASSIGNED', 'REJECTED', 'CANCELLED'].includes(activeRequest.status) ? <button className="mt-4 w-full rounded-xl px-4 py-2.5 text-sm font-bold text-rose-600 hover:bg-rose-50" disabled={working} onClick={cancelRequest} type="button">Hủy yêu cầu</button> : null}
              </section>
            ) : (
              <section className="rounded-2xl border border-[#d8c0c3] bg-white p-6 shadow-lg shadow-[#730014]/5">
                <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">Gửi nhu cầu học</p>
                <div className="mt-3 flex items-end justify-between gap-4 border-b border-slate-100 pb-5">
                  <div><p className="text-xs font-semibold text-slate-400">Học phí tham khảo</p><p className="mt-1 font-['Manrope'] text-2xl font-black text-[#8a0018]">{formatPrice(displayedPrice)}</p></div>
                  <CheckCircle2 className="h-7 w-7 text-emerald-500" />
                </div>
                <div className="mt-5 space-y-4">
                  <TextField label="Ca/lịch bạn ưu tiên" onChange={(value) => updateForm('preferredSchedule', value)} placeholder="Ví dụ: Tối thứ 2, 4, 6" value={form.preferredSchedule} />
                  {!isVirtual ? <TextField label="Cơ sở ưu tiên" onChange={(value) => updateForm('campusPreference', value)} placeholder="Ví dụ: Cầu Giấy" value={form.campusPreference} /> : null}
                  <label className="block"><FieldLabel>Ghi chú cho Nhân viên đào tạo</FieldLabel><textarea className="min-h-24 w-full resize-y rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-[#8a0018] focus:bg-white focus:ring-4 focus:ring-[#8a0018]/5" onChange={(event) => updateForm('note', event.target.value)} placeholder="Mục tiêu, thời gian có thể học hoặc nhu cầu cần hỗ trợ..." value={form.note} /></label>
                </div>
                <button className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-[#730014] px-5 py-3.5 text-sm font-extrabold text-white transition hover:bg-[#8a0018] disabled:opacity-60" disabled={working} onClick={submitRequest} type="button"><Send className="h-4 w-4" />{working ? 'Đang gửi...' : isAuthenticated ? 'Gửi yêu cầu đăng ký' : 'Đăng nhập để gửi yêu cầu'}</button>
                <p className="mt-3 text-center text-xs leading-5 text-slate-400">Nút này chỉ tạo yêu cầu, không tự xác nhận bạn thuộc một lớp.</p>
              </section>
            )}
          </aside>
        </div>
      </main>
    </PageFrame>
  );
}

function PageFrame({ children }) {
  return <div className={PAGE_SHELL_CLASS}><CourseGlobalStyles /><Header /><div className={PAGE_BODY_CLASS}>{children}</div><CourseFooter /></div>;
}

function HeroInfo({ icon: Icon, label, value }) {
  return <div className="flex items-start gap-3 px-5 py-5"><span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#fff4f5] text-[#8a0018]"><Icon className="h-5 w-5" /></span><div><p className="text-[11px] font-bold uppercase tracking-wide text-slate-400">{label}</p><p className="mt-1 text-sm font-extrabold text-slate-700">{value}</p></div></div>;
}

function DetailItem({ label, value }) {
  return <div className="rounded-xl border border-slate-100 bg-slate-50 p-4"><p className="text-xs font-bold uppercase tracking-wide text-slate-400">{label}</p><p className="mt-1.5 text-sm font-extrabold text-slate-700">{value}</p></div>;
}

function ProcessStep({ number, title, text }) {
  return <div className="rounded-xl border border-slate-100 bg-slate-50 p-4"><span className="text-xs font-black text-[#8a0018]">{number}</span><p className="mt-2 font-extrabold text-[#0b1c30]">{title}</p><p className="mt-1 text-sm leading-6 text-slate-500">{text}</p></div>;
}

function FieldLabel({ children }) {
  return <span className="mb-2 block text-xs font-bold uppercase tracking-[0.1em] text-slate-500">{children}</span>;
}

function TextField({ label, onChange, placeholder, value }) {
  return <label className="block"><FieldLabel>{label}</FieldLabel><input className="h-12 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 text-sm outline-none transition focus:border-[#8a0018] focus:bg-white focus:ring-4 focus:ring-[#8a0018]/5" onChange={(event) => onChange(event.target.value)} placeholder={placeholder} value={value} /></label>;
}
