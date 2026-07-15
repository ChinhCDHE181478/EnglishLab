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
import TuitionPaymentSection from '../../components/classroom/TuitionPaymentSection';
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

  const handleRegister = async () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/opening-schedule/${slugOrId}` } });
      return;
    }
    setRegistering(true);
    setActionMessage('');
    setActionSuccess(false);
    try {
      const data = await classroomApi.registerForClass(offering.id, { holdSpot: false });
      setRegistration(data);
      await loadOffering();

      if (data.registrationStatus === 'WAITLIST') {
        setActionMessage('Lớp hiện đã đủ chỗ. Bạn đã được thêm vào danh sách chờ và chưa cần thanh toán.');
        setActionSuccess(true);
        return;
      }
      if (data.hasClassAccess) {
        setActionMessage('Bạn đã đăng ký thành công lớp học miễn phí này.');
        setActionSuccess(true);
        return;
      }

      // Chỉ tạo hồ sơ chờ thanh toán; học viên tự bấm PayOS / gửi minh chứng ở khu vực bên dưới.
      setActionMessage('Đã tạo hồ sơ chờ thanh toán học phí. Vui lòng thanh toán qua PayOS hoặc gửi minh chứng chuyển khoản bên dưới để hoàn tất đăng ký.');
      setActionSuccess(true);
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể đăng ký lớp.'));
      setActionSuccess(false);
    } finally {
      setRegistering(false);
    }
  };

  const handleCancelRegistration = async () => {
    setRegistering(true);
    setActionMessage('');
    setActionSuccess(false);
    try {
      await classroomApi.cancelClassRegistration(offering.id);
      setRegistration(null);
      setActionMessage('Đã hủy đăng ký lớp. Bạn có thể đăng ký lại bất cứ lúc nào khi lớp còn mở.');
      setActionSuccess(true);
      await loadOffering();
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể hủy đăng ký.'));
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
  const isOpenStatus = ['OPEN', 'UPCOMING'].includes(offering?.classroomStatus);
  // Lớp đầy vẫn cho đăng ký — backend tự xếp vào danh sách chờ (WAITLIST).
  const canRegister = !isRegistered && isOpenStatus;
  const canCancelRegistration = isRegistered && !hasClassAccess && registrationStatus !== 'ASSIGNED';
  const curriculumProgram = offering?.curriculumProgram;
  const sessionCount = offering?.sessions?.length || 0;
  const sessionDurationMinutes = (() => {
    const first = offering?.sessions?.find((s) => s.startTime && s.endTime);
    if (!first) return null;
    const [sh, sm] = first.startTime.split(':').map(Number);
    const [eh, em] = first.endTime.split(':').map(Number);
    const minutes = (eh * 60 + em) - (sh * 60 + sm);
    return minutes > 0 ? minutes : null;
  })();

  const tuitionDue = Number(registration?.tuitionAmountDue ?? offering?.tuitionAmountDue ?? 0);
  const tuitionPaid = Number(registration?.tuitionAmountPaid ?? offering?.tuitionAmountPaid ?? 0);
  const tuitionRemaining = Number(
    registration?.tuitionRemaining ?? Math.max(0, tuitionDue - tuitionPaid),
  );
  const hasTuitionInfo = (registration?.tuitionAmountDue ?? offering?.tuitionAmountDue) != null;
  const needsTuitionPayment = isRegistered
    && registrationStatus !== 'WAITLIST'
    && !hasClassAccess
    && registrationStatus !== 'ASSIGNED'
    && tuitionRemaining > 0;
  const showTuitionPaymentSection = isRegistered
    && registrationStatus !== 'WAITLIST'
    && !hasClassAccess
    && registrationStatus !== 'ASSIGNED';

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
                <section className="relative overflow-hidden rounded-[28px] border border-[#dfc4c2]/40 shadow-[0_20px_50px_rgba(75,0,9,0.08)]">
                  <div className="absolute inset-0 bg-gradient-to-br from-[#3d0008] via-[#730014] to-[#9a1830]" />
                  <div className="pointer-events-none absolute -right-10 top-0 h-48 w-48 rounded-full bg-white/10 blur-3xl" />
                  <div className="pointer-events-none absolute -left-8 bottom-0 h-36 w-36 rounded-full bg-[#4b0009]/40 blur-2xl" />
                  <div className="relative p-8 text-white md:p-10">
                    <div className="mb-4 flex flex-wrap gap-2">
                      <ClassroomTypeBadge mode={offering.deliveryMode} />
                      <StatusBadge status={offering.classroomStatus} />
                      {isRegistered ? <StatusBadge status={registrationStatus} /> : null}
                    </div>
                    <h1 className="font-['Manrope'] text-3xl font-extrabold tracking-tight md:text-4xl max-w-3xl">
                      {offering.title}
                    </h1>
                    <p className="mt-4 max-w-2xl text-sm leading-7 text-white/85 md:text-base">
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
                      icon={<Video className="h-5 w-5" />}
                      label="Hình thức học"
                      value="Trực tuyến"
                    />
                  ) : (
                    <InfoCard
                      icon={<MapPin className="h-5 w-5" />}
                      label="Địa điểm học"
                      value={offering.offlineAddress || 'Cơ sở Hà Nội'}
                    />
                  )}
                  {!isVirtual && offering.roomName ? (
                    <InfoCard
                      icon={<MapPin className="h-5 w-5" />}
                      label="Phòng học"
                      value={offering.roomName}
                    />
                  ) : null}
                  <InfoCard
                    icon={<Clock className="h-5 w-5" />}
                    label="Số buổi học"
                    value={sessionCount > 0
                      ? `${sessionCount} buổi${sessionDurationMinutes ? ` · ${sessionDurationMinutes} phút/buổi` : ''}`
                      : (curriculumProgram?.totalSessions ? `${curriculumProgram.totalSessions} buổi (dự kiến)` : 'Đang cập nhật')}
                  />
                  {offering.entryLevel ? (
                    <InfoCard
                      icon={<Star className="h-5 w-5" />}
                      label="Trình độ đầu vào"
                      value={offering.entryLevel}
                    />
                  ) : null}
                  {offering.targetOutcome ? (
                    <InfoCard
                      icon={<CheckCircle2 className="h-5 w-5" />}
                      label="Mục tiêu đầu ra"
                      value={offering.targetOutcome}
                    />
                  ) : null}
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
                  <section className="rounded-[28px] border border-[#dfbfbd]/20 bg-[#fffafb] p-6 space-y-4">
                    <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828] flex items-center gap-2">
                      <Video className="h-5 w-5 text-[#730014]" />
                      Không gian học trực tuyến
                    </h3>
                    <p className="text-sm text-[#584140]">Phòng học Lark đã sẵn sàng. Bạn có thể tham gia lớp học ngay bên dưới.</p>
                    <LarkJoinButton onBlocked={setLarkMessage} url={offering.defaultLarkMeetingUrl} />
                    {larkMessage ? <p className="text-xs text-rose-700 font-semibold">{larkMessage}</p> : null}
                  </section>
                )}

                {/* Curriculum program (giáo trình theo lộ trình) */}
                {curriculumProgram && (
                  <section className="rounded-[28px] border border-[#dfbfbd]/15 bg-white p-6 shadow-sm space-y-4">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828] flex items-center gap-2">
                        <BookOpen className="h-5 w-5 text-[#730014]" />
                        Giáo trình: {curriculumProgram.title}
                      </h2>
                      <span className="rounded-full border border-rose-100 bg-rose-50 px-3 py-1 text-[10px] font-extrabold text-[#730014]">
                        {curriculumProgram.examCategory}
                        {curriculumProgram.targetBand ? ` · Target ${curriculumProgram.targetBand}` : ''}
                        {curriculumProgram.targetScore ? ` · Target ${curriculumProgram.targetScore}` : ''}
                      </span>
                    </div>
                    {curriculumProgram.outcomes ? (
                      <p className="whitespace-pre-line text-sm leading-7 text-[#584140]">{curriculumProgram.outcomes}</p>
                    ) : null}
                    {curriculumProgram.units?.length > 0 && (
                      <div className="space-y-2.5">
                        <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Chương trình học theo buổi</p>
                        {curriculumProgram.units.map((unit, idx) => {
                          const resourceCount = (unit.materials?.length || 0)
                            + (unit.exercises?.length || 0)
                            + (unit.assessments?.length || 0)
                            + (unit.flashcards?.length || 0);
                          return (
                            <div className="rounded-2xl border border-gray-100 bg-[#fffafb]/50 p-4" key={unit.id}>
                              <div className="flex flex-wrap items-center justify-between gap-2">
                                <p className="text-sm font-extrabold text-[#2b2828]">
                                  Buổi {idx + 1}: {unit.title}
                                </p>
                                {resourceCount > 0 && (
                                  <span className="text-[10px] font-bold text-[#8b706e]">{resourceCount} học liệu / bài tập</span>
                                )}
                              </div>
                              {unit.description ? (
                                <p className="mt-1 text-xs leading-5 text-[#584140]">{unit.description}</p>
                              ) : null}
                            </div>
                          );
                        })}
                      </div>
                    )}
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
                      Lịch học toàn khóa ({offering.sessions.length} buổi)
                    </h2>
                    <div className="max-h-[420px] space-y-2.5 overflow-y-auto pr-1">
                      {offering.sessions.map((session, idx) => (
                        <div
                          key={session.id}
                          className="flex flex-col gap-2 rounded-2xl border border-gray-100 bg-[#fffafb]/50 p-4 sm:flex-row sm:items-center sm:justify-between"
                        >
                          <div className="flex flex-wrap items-center gap-3 text-sm text-[#584140]">
                            <span className="text-[10px] font-extrabold text-[#8b706e]">Buổi {idx + 1}</span>
                            <Calendar className="h-4 w-4 text-[#730014] flex-shrink-0" />
                            <span className="font-extrabold text-[#2b2828]">{formatClassroomDate(session.sessionDate)}</span>
                            <span>{formatClassroomTime(session.startTime)} – {formatClassroomTime(session.endTime)}</span>
                            {session.roomName ? (
                              <span className="text-xs font-bold text-[#8b706e]">Phòng {session.roomName}</span>
                            ) : null}
                            {session.sessionContent ? (
                              <span className="text-xs text-[#8b706e] truncate max-w-[240px]">{session.sessionContent}</span>
                            ) : null}
                          </div>
                          <StatusBadge status={session.status} />
                        </div>
                      ))}
                    </div>
                  </section>
                )}
              </div>

              {/* ── RIGHT COLUMN: sticky action panel ── */}
              <aside className="space-y-5 lg:sticky lg:top-24 lg:self-start">
                {/* Price & CTA */}
                <div className="rounded-[28px] border border-[#dfbfbd]/15 bg-white p-6 shadow-sm space-y-5">
                  <div>
                    <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Học phí</p>
                    <p className="font-['Manrope'] text-3xl font-extrabold text-[#4b0009] mt-1">
                      {formatClassroomPrice(offering.price)}
                    </p>
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
                          onClick={handleRegister}
                          type="button"
                        >
                          {registering
                            ? 'Đang gửi...'
                            : !isAuthenticated
                              ? 'Đăng nhập để đăng ký'
                              : isFull
                                ? 'Tham gia danh sách chờ'
                                : 'Đăng ký và thanh toán'}
                          {!registering && <ChevronRight className="h-4 w-4" />}
                        </button>
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

                    {canCancelRegistration && (
                      <button
                        className="flex items-center justify-center gap-2 w-full rounded-2xl border border-rose-200 bg-white py-3 text-xs font-extrabold text-rose-700 transition hover:bg-rose-50 active:scale-95 disabled:opacity-60"
                        disabled={registering}
                        onClick={handleCancelRegistration}
                        type="button"
                      >
                        {registering ? 'Đang xử lý...' : 'Hủy hồ sơ đăng ký'}
                      </button>
                    )}

                    {isFull && !isRegistered && (
                      <div className="rounded-2xl bg-rose-50 border border-rose-100 p-4 text-xs text-rose-800 text-center">
                        <p className="font-extrabold">Lớp học đã đủ chỗ</p>
                        <p className="mt-1">
                          Đăng ký ngay để vào danh sách chờ xếp lớp
                          {offering.waitlistCount > 0 ? ` (hiện có ${offering.waitlistCount} người đang chờ)` : ''}.
                        </p>
                      </div>
                    )}
                  </div>
                </div>

                {/* Registration Status Card (if registered) */}
                {isRegistered ? (
                  <div className="rounded-[24px] border border-[#dfbfbd]/20 bg-white p-4 shadow-sm space-y-3">
                    <div className="flex items-center justify-between gap-2">
                      <h3 className="font-['Manrope'] text-sm font-extrabold text-[#2b2828] flex items-center gap-2">
                        {hasClassAccess
                          ? <Unlock className="h-4 w-4 text-emerald-600" />
                          : <Lock className="h-4 w-4 text-amber-600" />}
                        Hồ sơ đăng ký
                      </h3>
                      <StatusBadge status={registrationStatus} />
                    </div>

                    {registrationStatus === 'WAITLIST' ? (
                      <div className="rounded-xl border border-amber-200 bg-amber-50/70 px-3 py-2.5 text-amber-950">
                        <p className="text-[10px] font-extrabold uppercase tracking-wider text-amber-700">
                          Danh sách chờ
                        </p>
                        <p className="mt-1 text-[11px] leading-4 text-amber-800">
                          Lớp hiện đã đủ chỗ. Bạn sẽ được mời thanh toán khi có suất trống.
                        </p>
                      </div>
                    ) : null}

                    {hasTuitionInfo ? (
                      <TuitionStatusCard
                        compact
                        due={tuitionDue}
                        paid={tuitionPaid}
                        remaining={tuitionRemaining}
                        settlementType={registration?.tuitionSettlementType || offering?.tuitionSettlementType}
                        settlementLabel={registration?.tuitionSettlementTypeLabel || offering?.tuitionSettlementTypeLabel}
                        settlementNote={registration?.tuitionSettlementNote || offering?.tuitionSettlementNote}
                      />
                    ) : null}

                    {needsTuitionPayment ? (
                      <div className="rounded-xl bg-blue-50/60 border border-blue-100/60 px-3 py-2 text-[10px] text-blue-800 flex items-start gap-2">
                        <Info className="h-3.5 w-3.5 flex-shrink-0 mt-0.5 text-blue-600" />
                        <p className="leading-4">
                          Thanh toán qua PayOS hoặc gửi minh chứng chuyển khoản bên dưới để hoàn tất đăng ký.
                        </p>
                      </div>
                    ) : null}
                  </div>
                ) : null}

                {/* Tuition payment: PayOS + proof fallback — only when still unpaid */}
                {showTuitionPaymentSection ? (
                  <TuitionPaymentSection
                    canSubmitProof
                    classroomId={offering.id}
                    tuitionRemaining={tuitionRemaining}
                    onUpdated={() => loadRegistration(offering.id)}
                  />
                ) : null}

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

function InfoCard({ icon, label, value }) {
  return (
    <div className="flex items-center gap-3 rounded-2xl border border-gray-100 bg-white p-4 shadow-sm">
      <div className="flex h-10 w-10 items-center justify-center rounded-xl flex-shrink-0 bg-rose-50 text-[#730014]">
        {icon}
      </div>
      <div className="min-w-0">
        <p className="text-[10px] font-bold text-[#8b706e] uppercase tracking-wider">{label}</p>
        <p className="mt-0.5 font-extrabold text-[#2b2828] text-sm truncate">{value}</p>
      </div>
    </div>
  );
}
