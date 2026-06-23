import { useCallback, useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  Calendar,
  Clock,
  MapPin,
  Video,
  Users,
  DollarSign,
  BookOpen,
  CheckCircle2,
  AlertCircle,
  Lock,
  Unlock,
  ChevronRight,
  Info,
  Star,
  User,
  Play,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import Header from '../../components/ai-learning/Header';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import {
  ClassroomErrorState,
  ClassroomLoadingState,
  LarkJoinButton,
  ClassroomTypeBadge,
  StatusBadge,
  TuitionStatusCard,
} from '../../components/classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatClassroomDate,
  formatClassroomPrice,
  formatClassroomTime,
  formatOfferingStatus,
  formatRegistrationStatus,
  formatSessionStatus,
  formatTuitionSettlement,
} from '../../utils/classroomHelpers';
import { getStoredUser, hasAccessToken } from '../../utils/auth';
import { PAGE_BODY_CLASS, PAGE_HEADER_CLASS, PAGE_MAIN_STACK_CLASS, PAGE_SHELL_CLASS } from '../../utils/pageLayout';

const capacityPercent = (enrolled, max) => {
  if (!max || max <= 0) return 0;
  return Math.min(100, Math.round(((enrolled ?? 0) / max) * 100));
};

export default function ClassroomPublicDetailPage() {
  const { slugOrId } = useParams();
  const navigate = useNavigate();
  const [offering, setOffering] = useState(null);
  const [registration, setRegistration] = useState(null);
  const [loading, setLoading] = useState(true);
  const [registering, setRegistering] = useState(false);
  const [error, setError] = useState('');
  const [notFound, setNotFound] = useState(false);
  const [actionMessage, setActionMessage] = useState('');
  const [actionSuccess, setActionSuccess] = useState(false);
  const [larkMessage, setLarkMessage] = useState('');
  const isAuthenticated = Boolean(hasAccessToken() && getStoredUser());

  const loadRegistration = useCallback(async (classroomId) => {
    if (!isAuthenticated || !classroomId) {
      setRegistration(null);
      return;
    }
    try {
      const data = await classroomApi.getMyClassRegistration(classroomId);
      setRegistration(data);
    } catch {
      setRegistration(null);
    }
  }, [isAuthenticated]);

  const loadOffering = useCallback(async () => {
    setLoading(true);
    setError('');
    setNotFound(false);
    try {
      const data = await classroomApi.getClassroomOffering(slugOrId);
      setOffering(data);
      await loadRegistration(data.id);
    } catch (err) {
      setOffering(null);
      setRegistration(null);
      setNotFound(err?.response?.status === 404);
      setError(getClassroomErrorMessage(err, 'Không thể tải thông tin lớp học.'));
    } finally {
      setLoading(false);
    }
  }, [loadRegistration, slugOrId]);

  useEffect(() => {
    loadOffering();
  }, [loadOffering]);

  const handleRegister = async (holdSpot) => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/opening-schedule/${slugOrId}` } });
      return;
    }
    setRegistering(true);
    setActionMessage('');
    setActionSuccess(false);
    try {
      const data = await classroomApi.registerForClass(offering.id, { holdSpot });
      setRegistration(data);
      setActionMessage(holdSpot ? 'Đã gửi yêu cầu giữ chỗ thành công. Điều phối đào tạo sẽ xác nhận trong thời gian sớm nhất.' : 'Đã gửi đăng ký lớp thành công. Vui lòng chờ điều phối đào tạo xác nhận.');
      setActionSuccess(true);
      await loadOffering();
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể đăng ký lớp.'));
      setActionSuccess(false);
    } finally {
      setRegistering(false);
    }
  };

  const registrationStatus = registration?.registrationStatus || offering?.registrationStatus;
  const registrationLabel = registration?.registrationStatusLabel || offering?.registrationStatusLabel;
  const hasClassAccess = registration?.hasClassAccess || offering?.hasClassAccess;
  const isRegistered = Boolean(registration || offering?.registered);
  const isVirtual = offering?.deliveryMode === 'VIRTUAL';

  const pct = capacityPercent(offering?.enrolledCount, offering?.maxCapacity);
  const isFull = pct >= 100 || offering?.classroomStatus === 'FULL';
  const canRegister = !isRegistered && !isFull && ['OPEN', 'UPCOMING'].includes(offering?.classroomStatus);

  return (
    <div className={PAGE_SHELL_CLASS}>
      <CourseGlobalStyles />
      <div className={PAGE_HEADER_CLASS}>
        <Header />
      </div>

      <div className={PAGE_BODY_CLASS}>
      <motion.main
        className={PAGE_MAIN_STACK_CLASS}
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.32, ease: 'easeOut' }}
      >
        {loading ? <ClassroomLoadingState message="Đang tải thông tin lớp học..." /> : null}
        {!loading && error && !notFound ? <ClassroomErrorState message={error} onRetry={loadOffering} /> : null}
        {!loading && notFound ? (
          <div className="flex min-h-[420px] flex-1 flex-col items-center justify-center rounded-[28px] border border-[#f0d4d7] bg-white px-6 py-16 text-center shadow-sm">
            <AlertCircle className="h-14 w-14 text-[#93000a]" />
            <h1 className="mt-5 font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Lớp học không còn mở công khai</h1>
            <p className="mt-3 max-w-lg text-sm leading-7 text-[#584140]">
              Lớp học này có thể đã kết thúc, bị ẩn hoặc đường dẫn cũ không còn phù hợp. Hãy quay lại lịch khai giảng để chọn lớp đang mở.
            </p>
            <Link className="mt-7 rounded-2xl bg-[#4b0009] px-6 py-3.5 text-sm font-extrabold text-white transition hover:bg-[#730014]" to="/opening-schedule">
              Xem lịch khai giảng
            </Link>
          </div>
        ) : null}

        {!loading && !error && offering ? (
          <>
            {/* Back link */}
            <Link
              className="inline-flex items-center gap-1.5 text-xs font-bold text-[#8b706e] hover:text-[#730014] transition"
              to="/opening-schedule"
            >
              <ArrowLeft className="h-4 w-4" />
              Quay lại lịch khai giảng
            </Link>

            <div className="grid gap-8 lg:grid-cols-[1fr_380px]">
              {/* ── LEFT COLUMN ── */}
              <div className="space-y-8">
                {/* Hero header */}
                <section className="relative overflow-hidden rounded-[32px] border border-[#dfbfbd]/15 bg-white p-8 shadow-sm">
                  <div className="absolute right-0 top-0 -mr-20 -mt-20 h-72 w-72 rounded-full bg-[#fff3f4] blur-3xl" />
                  <div className="relative">
                    <div className="flex flex-wrap gap-2 mb-4">
                      <ClassroomTypeBadge mode={offering.deliveryMode} />
                      <StatusBadge status={offering.classroomStatus} />
                      {isRegistered && (
                        <StatusBadge status={registrationStatus} />
                      )}
                    </div>
                    <h1 className="font-['Manrope'] text-3xl font-extrabold tracking-tight text-[#2b2828] md:text-4xl">
                      {offering.title}
                    </h1>
                    <p className="mt-3 text-base leading-8 text-[#584140]">
                      {offering.description || offering.shortDescription || 'Đang cập nhật mô tả chi tiết về lớp học này.'}
                    </p>
                  </div>
                </section>

                {/* Info cards grid */}
                <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  <InfoCard
                    icon={<User className="h-5 w-5" />}
                    label="Giảng viên chính"
                    value={offering.primaryTeacherName || 'Đang cập nhật'}
                  />
                  <InfoCard
                    icon={<Calendar className="h-5 w-5" />}
                    label="Ngày khai giảng"
                    value={formatClassroomDate(offering.startDate)}
                  />
                  <InfoCard
                    icon={<Calendar className="h-5 w-5" />}
                    label="Ngày kết thúc"
                    value={formatClassroomDate(offering.endDate)}
                  />
                  {isVirtual ? (
                    <InfoCard
                      icon={<Video className="h-5 w-5 text-purple-700" />}
                      label="Hình thức học"
                      value="Trực tuyến"
                      accent="purple"
                    />
                  ) : (
                    <InfoCard
                      icon={<MapPin className="h-5 w-5" />}
                      label="Địa điểm học"
                      value={offering.offlineAddress || 'Cơ sở Hà Nội'}
                    />
                  )}
                  <div className="sm:col-span-2 lg:col-span-2 rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
                    <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider flex items-center gap-1 mb-3">
                      <Users className="h-4 w-4 text-[#730014]" />
                      Sĩ số lớp học
                    </p>
                    <div className="flex items-center justify-between text-sm mb-2">
                      <span className="font-extrabold text-[#2b2828]">{offering.enrolledCount ?? 0} / {offering.maxCapacity ?? '—'} học viên</span>
                      <span className={`text-xs font-extrabold ${pct >= 90 ? 'text-rose-600' : pct >= 70 ? 'text-amber-600' : 'text-emerald-700'}`}>
                        {pct}% đã lấp đầy
                      </span>
                    </div>
                    <div className="h-2.5 w-full overflow-hidden rounded-full bg-gray-100">
                      <div
                        className={`h-full rounded-full transition-all duration-500 ${pct >= 90 ? 'bg-rose-500' : pct >= 70 ? 'bg-amber-400' : 'bg-emerald-500'}`}
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                </section>

                {/* Virtual Lark block */}
                {isVirtual && offering.defaultLarkMeetingUrl && hasClassAccess && (
                  <section className="rounded-[28px] border border-purple-100 bg-purple-50/10 p-6 space-y-4">
                    <h3 className="font-['Manrope'] text-lg font-extrabold text-purple-950 flex items-center gap-2">
                      <Video className="h-5 w-5 text-purple-700" />
                      Không gian học trực tuyến
                    </h3>
                    <p className="text-sm text-[#584140]">Phòng học Lark đã sẵn sàng. Bạn có thể tham gia lớp học ngay bên dưới.</p>
                    <LarkJoinButton onBlocked={setLarkMessage} url={offering.defaultLarkMeetingUrl} />
                    {larkMessage ? <p className="text-xs text-rose-700 font-semibold">{larkMessage}</p> : null}
                  </section>
                )}

                {/* Syllabus */}
                {offering.syllabusSummary && (
                  <section className="rounded-[28px] border border-[#dfbfbd]/15 bg-white p-6 shadow-sm space-y-4">
                    <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828] flex items-center gap-2">
                      <BookOpen className="h-5 w-5 text-[#730014]" />
                      Chương trình học
                    </h2>
                    <p className="whitespace-pre-line text-sm leading-7 text-[#584140]">{offering.syllabusSummary}</p>
                  </section>
                )}

                {/* Session preview */}
                {offering.sessions?.length > 0 && (
                  <section className="rounded-[28px] border border-[#dfbfbd]/15 bg-white p-6 shadow-sm space-y-4">
                    <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828] flex items-center gap-2">
                      <Clock className="h-5 w-5 text-[#730014]" />
                      Lịch học dự kiến
                    </h2>
                    <div className="space-y-2.5">
                      {offering.sessions.slice(0, 5).map((session) => (
                        <div
                          key={session.id}
                          className="flex flex-col gap-2 rounded-2xl border border-gray-100 bg-[#fffafb]/50 p-4 sm:flex-row sm:items-center sm:justify-between"
                        >
                          <div className="flex items-center gap-3 text-sm text-[#584140]">
                            <Calendar className="h-4 w-4 text-[#730014] flex-shrink-0" />
                            <span className="font-extrabold text-[#2b2828]">{formatClassroomDate(session.sessionDate)}</span>
                            <span>{formatClassroomTime(session.startTime)} – {formatClassroomTime(session.endTime)}</span>
                          </div>
                          <StatusBadge status={session.status} />
                        </div>
                      ))}
                      {offering.sessions.length > 5 && (
                        <p className="text-xs font-bold text-[#8b706e] text-center pt-1">
                          + {offering.sessions.length - 5} buổi học khác...
                        </p>
                      )}
                    </div>
                  </section>
                )}
              </div>

              {/* ── RIGHT COLUMN: sticky action panel ── */}
              <aside className="space-y-5 lg:sticky lg:top-24 lg:self-start">
                {/* Price & CTA */}
                <div className="rounded-[28px] border border-[#dfbfbd]/15 bg-white p-6 shadow-sm space-y-5">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Học phí</p>
                      <p className="font-['Manrope'] text-3xl font-extrabold text-[#4b0009] mt-1">
                        {formatClassroomPrice(offering.price)}
                      </p>
                    </div>
                    <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-50 text-[#730014]">
                      <DollarSign className="h-7 w-7" />
                    </div>
                  </div>

                  {/* Action Message */}
                  {actionMessage && (
                    <div className={`rounded-2xl border p-4 text-xs flex items-start gap-2 ${
                      actionSuccess
                        ? 'bg-emerald-50 border-emerald-100 text-emerald-800'
                        : 'bg-rose-50 border-rose-100 text-rose-800'
                    }`}>
                      {actionSuccess
                        ? <CheckCircle2 className="h-4 w-4 text-emerald-700 flex-shrink-0 mt-0.5" />
                        : <AlertCircle className="h-4 w-4 text-rose-700 flex-shrink-0 mt-0.5" />}
                      <p className="leading-5">{actionMessage}</p>
                    </div>
                  )}

                  {/* CTAs */}
                  <div className="space-y-3">
                    {hasClassAccess && (
                      <Link
                        className="flex items-center justify-center gap-2 w-full rounded-2xl bg-[#4b0009] py-4 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow-lg active:scale-95"
                        to={`/my-classrooms/${offering.id}`}
                      >
                        <Play className="h-4 w-4" />
                        Vào lớp học của tôi
                      </Link>
                    )}

                    {canRegister && (
                      <>
                        <button
                          className="flex items-center justify-center gap-2 w-full rounded-2xl bg-[#4b0009] py-4 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow-lg active:scale-95 disabled:opacity-60"
                          disabled={registering}
                          onClick={() => handleRegister(false)}
                          type="button"
                        >
                          {registering ? 'Đang gửi...' : isAuthenticated ? 'Đăng ký lớp ngay' : 'Đăng nhập để đăng ký'}
                          {!registering && <ChevronRight className="h-4 w-4" />}
                        </button>
                        <button
                          className="flex items-center justify-center gap-2 w-full rounded-2xl border-2 border-[#4b0009] bg-white py-3.5 text-sm font-extrabold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95 disabled:opacity-60"
                          disabled={registering}
                          onClick={() => handleRegister(true)}
                          type="button"
                        >
                          Giữ chỗ trước
                        </button>
                        <p className="text-[10px] text-[#8b706e] text-center leading-4">
                          Giữ chỗ giúp bảo lưu vị trí trong khi bạn chuẩn bị học phí. Điều phối đào tạo sẽ xác nhận.
                        </p>
                      </>
                    )}

                    {isRegistered && !hasClassAccess && (
                      <Link
                        className="flex items-center justify-center gap-2 w-full rounded-2xl border-2 border-[#4b0009] bg-white py-3.5 text-sm font-extrabold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95"
                        to="/my-classrooms"
                      >
                        Xem trạng thái đăng ký
                        <ChevronRight className="h-4 w-4" />
                      </Link>
                    )}

                    {isFull && !isRegistered && (
                      <div className="rounded-2xl bg-rose-50 border border-rose-100 p-4 text-xs text-rose-800 text-center">
                        <p className="font-extrabold">Lớp học đã đủ chỗ</p>
                        <p className="mt-1">Bạn vẫn có thể đăng ký để vào danh sách chờ xếp lớp.</p>
                      </div>
                    )}
                  </div>
                </div>

                {/* Registration Status Card (if registered) */}
                {isRegistered && (
                  <div className="rounded-[28px] border border-[#dfbfbd]/15 bg-white p-6 shadow-sm space-y-4">
                    <h3 className="font-['Manrope'] text-base font-extrabold text-[#2b2828] flex items-center gap-2">
                      {hasClassAccess
                        ? <Unlock className="h-4 w-4 text-emerald-600" />
                        : <Lock className="h-4 w-4 text-amber-600" />}
                      Trạng thái đăng ký
                    </h3>

                    <StatusBadge status={registrationStatus} />

                    {/* Tuition summary */}
                    {(registration?.tuitionAmountDue ?? offering?.tuitionAmountDue) != null && (
                      <TuitionStatusCard
                        due={registration?.tuitionAmountDue ?? offering?.tuitionAmountDue}
                        paid={registration?.tuitionAmountPaid ?? offering?.tuitionAmountPaid ?? 0}
                        remaining={
                          (registration?.tuitionAmountDue ?? offering?.tuitionAmountDue) -
                          (registration?.tuitionAmountPaid ?? offering?.tuitionAmountPaid ?? 0)
                        }
                        settlementType={registration?.tuitionSettlementType || offering?.tuitionSettlementType}
                        settlementLabel={registration?.tuitionSettlementTypeLabel || offering?.tuitionSettlementTypeLabel}
                        settlementNote={registration?.tuitionSettlementNote || offering?.tuitionSettlementNote}
                      />
                    )}

                    <div className="rounded-xl bg-blue-50/50 border border-blue-100/50 p-3 text-[10px] text-blue-800 flex items-start gap-2">
                      <Info className="h-3.5 w-3.5 flex-shrink-0 mt-0.5 text-blue-600" />
                      <p className="leading-4">Học phí được ghi nhận bởi điều phối đào tạo. Liên hệ trung tâm để nộp học phí nếu được yêu cầu.</p>
                    </div>
                  </div>
                )}

                {/* Highlights */}
                <div className="rounded-[28px] border border-[#dfbfbd]/15 bg-white p-6 shadow-sm space-y-3">
                  <h3 className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Cam kết đào tạo</h3>
                  {[
                    'Giảng viên được chứng nhận IELTS / TOEIC chuyên nghiệp',
                    'Lớp học nhỏ, chú trọng phản hồi cá nhân từ giáo viên',
                    'Tài liệu học tập bản quyền, cập nhật theo đề thi thật',
                    'Hỗ trợ đổi lịch linh hoạt qua điều phối đào tạo',
                  ].map((point, i) => (
                    <div key={i} className="flex items-start gap-2.5 text-xs text-[#584140]">
                      <CheckCircle2 className="h-4 w-4 text-emerald-600 flex-shrink-0 mt-0.5" />
                      <span>{point}</span>
                    </div>
                  ))}
                </div>
              </aside>
            </div>
          </>
        ) : null}
      </motion.main>
      </div>

      <CourseFooter />
    </div>
  );
}

function InfoCard({ icon, label, value, accent = 'rose' }) {
  const colors = {
    rose: 'bg-rose-50 text-[#730014]',
    purple: 'bg-purple-50 text-purple-700',
  };
  return (
    <div className="flex items-center gap-3 rounded-2xl border border-gray-100 bg-white p-4 shadow-sm">
      <div className={`flex h-10 w-10 items-center justify-center rounded-xl flex-shrink-0 ${colors[accent]}`}>
        {icon}
      </div>
      <div className="min-w-0">
        <p className="text-[10px] font-bold text-[#8b706e] uppercase tracking-wider">{label}</p>
        <p className="mt-0.5 font-extrabold text-[#2b2828] text-sm truncate">{value}</p>
      </div>
    </div>
  );
}
