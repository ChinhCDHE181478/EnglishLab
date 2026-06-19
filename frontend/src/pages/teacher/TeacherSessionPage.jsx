import { useEffect, useState, useMemo } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  Calendar,
  Clock,
  MapPin,
  Video,
  Users,
  CheckCircle2,
  XCircle,
  AlertCircle,
  ArrowLeft,
  Settings,
  User,
  Check,
  X,
  Plus,
  RefreshCw,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import Header from '../../components/ai-learning/Header';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  LarkJoinButton,
  PageHero,
  StatusBadge,
  ClassroomTypeBadge,
} from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatAttendanceStatus,
  formatClassroomDate,
  formatClassroomTime,
  formatSessionStatus,
} from '../../utils/classroomHelpers';

const attendanceOptions = [
  { label: 'Có mặt', value: 'PRESENT' },
  { label: 'Vắng mặt', value: 'ABSENT' },
  { label: 'Đi muộn', value: 'LATE' },
  { label: 'Có phép', value: 'EXCUSED' },
];

export default function TeacherSessionPage() {
  const { sessionId } = useParams();
  const [attendance, setAttendance] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [larkMessage, setLarkMessage] = useState('');
  const [larkUrl, setLarkUrl] = useState('');
  const [sessionMeta, setSessionMeta] = useState(null);
  const [records, setRecords] = useState({});

  const loadAttendance = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getSessionAttendance(sessionId);
      setAttendance(data);
      setSessionMeta(data[0] || null);
      const initialRecords = {};
      data.forEach((item) => {
        initialRecords[item.studentId || item.enrollmentId] = item.status || 'PRESENT';
      });
      setRecords(initialRecords);
    } catch (err) {
      setAttendance([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải dữ liệu buổi học.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAttendance();
  }, [sessionId]);

  const handleSaveAttendance = async () => {
    setActionMessage('');
    try {
      const payload = {
        sessionId: Number(sessionId),
        records: attendance.map((item) => ({
          studentId: item.studentId,
          status: records[item.studentId] || item.status || 'PRESENT',
        })),
      };
      const saved = await classroomApi.saveAttendance(payload);
      setAttendance(saved);
      setActionMessage('Đã lưu dữ liệu điểm danh thành công.');
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể lưu điểm danh.'));
    }
  };

  const handleOpenSession = async () => {
    setActionMessage('');
    try {
      const session = await classroomApi.openVirtualSession(sessionId);
      setSessionMeta((current) => ({ ...current, ...session }));
      setActionMessage('Đã mở buổi học trực tuyến thành công.');
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể mở buổi học.'));
    }
  };

  const handleCloseSession = async () => {
    setActionMessage('');
    try {
      const session = await classroomApi.closeVirtualSession(sessionId);
      setSessionMeta((current) => ({ ...current, ...session }));
      setActionMessage('Đã đóng buổi học trực tuyến thành công.');
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể đóng buổi học.'));
    }
  };

  const handleUpdateLark = async () => {
    setActionMessage('');
    try {
      const session = await classroomApi.updateSessionLarkLink(sessionId, { larkMeetingUrl: larkUrl });
      setSessionMeta((current) => ({ ...current, ...session }));
      setActionMessage('Đã cập nhật liên kết Lark thành công.');
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể cập nhật liên kết Lark.'));
    }
  };

  // Quick Action: Mark all present
  const handleMarkAllPresent = () => {
    const updated = {};
    attendance.forEach((item) => {
      updated[item.studentId || item.enrollmentId] = 'PRESENT';
    });
    setRecords(updated);
    setActionMessage('Đã chuyển đổi nhanh: Tất cả Có mặt.');
  };

  // Quick Action: Mark all absent
  const handleMarkAllAbsent = () => {
    const updated = {};
    attendance.forEach((item) => {
      updated[item.studentId || item.enrollmentId] = 'ABSENT';
    });
    setRecords(updated);
    setActionMessage('Đã chuyển đổi nhanh: Tất cả Vắng mặt.');
  };

  // Calculate stats for sticky footer
  const summaryStats = useMemo(() => {
    if (!attendance.length) return { present: 0, absent: 0, late: 0, excused: 0 };
    let present = 0;
    let absent = 0;
    let late = 0;
    let excused = 0;

    attendance.forEach((item) => {
      const key = item.studentId || item.enrollmentId;
      const status = records[key] || item.status || 'PRESENT';
      if (status === 'PRESENT') present++;
      if (status === 'ABSENT') absent++;
      if (status === 'LATE') late++;
      if (status === 'EXCUSED') excused++;
    });

    return { present, absent, late, excused };
  }, [attendance, records]);

  return (
    <div className="course-page flex min-h-[100dvh] flex-col bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />
      <main className="mx-auto flex w-full max-w-[1320px] flex-1 flex-col px-4 pb-[120px] pt-8 md:px-10 space-y-8">
        {/* ── Session Header ── */}
        <section className="relative overflow-hidden rounded-[32px] bg-gradient-to-br from-[#4b0009] via-[#6b000f] to-[#912040] p-8 shadow-lg">
          <div className="absolute -right-16 -top-16 h-64 w-64 rounded-full bg-white/5 blur-3xl" />
          <div className="absolute bottom-0 left-1/4 h-40 w-40 rounded-full bg-white/5 blur-3xl" />
          <div className="relative flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <span className="inline-flex rounded-full border border-white/20 bg-white/10 px-3 py-1 text-[10px] font-extrabold uppercase tracking-widest text-white/80">
                Điểm danh buổi học
              </span>
              <h1 className="mt-3 font-['Manrope'] text-3xl font-extrabold tracking-tight text-white md:text-4xl">
                {sessionMeta?.sessionDate ? formatClassroomDate(sessionMeta.sessionDate) : `Buổi học #${sessionId}`}
              </h1>
              {sessionMeta && (
                <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-sm text-white/75">
                  <span className="flex items-center gap-1.5">
                    <Clock className="h-4 w-4" />
                    {formatClassroomTime(sessionMeta.startTime)} – {formatClassroomTime(sessionMeta.endTime)}
                  </span>
                  {sessionMeta.deliveryMode === 'VIRTUAL' ? (
                    <span className="flex items-center gap-1.5 font-bold text-purple-200">
                      <Video className="h-4 w-4" /> Trực tuyến
                    </span>
                  ) : (
                    <span className="flex items-center gap-1.5">
                      <MapPin className="h-4 w-4" />
                      {sessionMeta.roomName || 'Phòng học offline'}
                    </span>
                  )}
                  <span className="flex items-center gap-1.5">
                    <Users className="h-4 w-4" />
                    {attendance.length} học viên
                  </span>
                </div>
              )}
            </div>

            {sessionMeta && (
              <Link
                className="inline-flex flex-shrink-0 items-center gap-1.5 rounded-2xl border border-white/20 bg-white/10 px-5 py-3 text-sm font-extrabold text-white backdrop-blur-sm transition hover:bg-white/20 active:scale-95"
                to={`/teacher/classrooms/${sessionMeta.classroomOfferingId || id}`}
              >
                <ArrowLeft className="h-4 w-4" />
                Quay lại lớp học
              </Link>
            )}
          </div>
        </section>

        {loading ? <ClassroomLoadingState message="Đang tải dữ liệu buổi học..." /> : null}
        {!loading && error ? <ClassroomErrorState message={error} onRetry={loadAttendance} /> : null}
        {!loading && !error ? (
          <>
            {/* Action Notification */}
            {actionMessage ? (
              <div className={`rounded-2xl border p-4 text-xs flex items-start gap-2 ${
                actionMessage.includes('thành công')
                  ? 'bg-emerald-50 border-emerald-100 text-emerald-800'
                  : 'bg-rose-50 border-rose-100 text-rose-800'
              }`}>
                {actionMessage.includes('thành công') ? (
                  <CheckCircle2 className="h-4 w-4 flex-shrink-0 mt-0.5 text-emerald-700" />
                ) : (
                  <AlertCircle className="h-4 w-4 flex-shrink-0 mt-0.5 text-rose-700" />
                )}
                <p className="leading-5">{actionMessage}</p>
              </div>
            ) : null}

            {/* Virtual Meeting Operations (Lark) */}
            {sessionMeta?.deliveryMode === 'VIRTUAL' && (
              <section className="rounded-[32px] border border-[#dfbfbd]/20 bg-white p-6 shadow-sm space-y-6">
                <div className="flex items-start gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-purple-50 text-purple-700 flex-shrink-0">
                    <Video className="h-6 w-6" />
                  </div>
                  <div>
                    <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">Vận hành lớp học trực tuyến</h2>
                    <p className="mt-1 text-xs text-[#8b706e] leading-5">Mở phòng học trực tuyến, cập nhật liên kết phòng học và đóng phòng học sau khi kết thúc buổi giảng dạy.</p>
                  </div>
                </div>

                <div className="flex flex-wrap gap-3 pt-2">
                  <button
                    className="inline-flex items-center gap-1.5 rounded-xl bg-purple-700 px-5 py-3 text-xs font-extrabold text-white shadow-sm transition hover:bg-purple-800 active:scale-95"
                    onClick={handleOpenSession}
                    type="button"
                  >
                    <Check className="h-4 w-4" />
                    Mở buổi học trực tuyến
                  </button>
                  <button
                    className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-5 py-3 text-xs font-extrabold text-[#584140] transition hover:bg-gray-50 active:scale-95"
                    onClick={handleCloseSession}
                    type="button"
                  >
                    <X className="h-4 w-4" />
                    Đóng buổi học trực tuyến
                  </button>
                </div>

                {sessionMeta?.larkMeetingUrl && (
                  <div className="rounded-2xl bg-purple-50/20 border border-purple-100 p-4">
                    <LarkJoinButton onBlocked={setLarkMessage} url={sessionMeta.larkMeetingUrl} />
                  </div>
                )}
                {larkMessage ? <p className="text-sm font-semibold text-[#93000a]">{larkMessage}</p> : null}

                <div className="flex flex-col gap-3 md:flex-row pt-2">
                  <input
                    className="flex-1 rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
                    onChange={(event) => setLarkUrl(event.target.value)}
                    placeholder="Nhập liên kết phòng học Lark mới..."
                    value={larkUrl}
                  />
                  <button
                    className="rounded-2xl bg-[#4b0009] px-6 py-3.5 text-sm font-extrabold text-white shadow-sm transition hover:bg-[#730014] active:scale-95"
                    onClick={handleUpdateLark}
                    type="button"
                  >
                    Cập nhật Lark Link
                  </button>
                </div>
              </section>
            )}

            {/* Attendance Tool */}
            <section className="rounded-[32px] border border-[#dfbfbd]/20 bg-white p-6 shadow-sm space-y-6">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-start gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-[#730014] flex-shrink-0">
                    <Users className="h-6 w-6" />
                  </div>
                  <div>
                    <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">Điểm danh lớp học</h2>
                    <p className="mt-1 text-xs text-[#8b706e] leading-5">Chọn trạng thái chuyên cần cho từng học viên. Sử dụng các phím tắt để thao tác nhanh.</p>
                  </div>
                </div>

                <div className="flex flex-wrap gap-2">
                  <button
                    className="inline-flex items-center gap-1 rounded-xl border border-emerald-100 bg-emerald-50/50 px-4 py-2 text-xs font-extrabold text-emerald-800 hover:bg-emerald-50 transition"
                    onClick={handleMarkAllPresent}
                    type="button"
                  >
                    <CheckCircle2 className="h-3.5 w-3.5" /> Tất cả Có mặt
                  </button>
                  <button
                    className="inline-flex items-center gap-1 rounded-xl border border-rose-100 bg-rose-50/50 px-4 py-2 text-xs font-extrabold text-rose-800 hover:bg-rose-50 transition"
                    onClick={handleMarkAllAbsent}
                    type="button"
                  >
                    <XCircle className="h-3.5 w-3.5" /> Tất cả Vắng mặt
                  </button>
                </div>
              </div>

              {!attendance.length ? (
                <ClassroomEmptyState
                  description="Chưa có học viên nào được ghi nhận trong danh sách lớp này."
                  title="Danh sách trống"
                />
              ) : (
                <div className="space-y-2.5">
                  {attendance.map((item) => {
                    const key = item.studentId || item.enrollmentId;
                    const currentStatus = records[key] || item.status || 'PRESENT';

                    const statusConfig = {
                      PRESENT: { label: 'Có mặt', bg: 'bg-emerald-500', light: 'bg-emerald-50 border-emerald-200 text-emerald-800', row: 'border-emerald-100 bg-emerald-50/10' },
                      ABSENT: { label: 'Vắng mặt', bg: 'bg-rose-500', light: 'bg-rose-50 border-rose-200 text-rose-800', row: 'border-rose-100 bg-rose-50/10' },
                      LATE: { label: 'Đi muộn', bg: 'bg-amber-400', light: 'bg-amber-50 border-amber-200 text-amber-800', row: 'border-amber-100 bg-amber-50/10' },
                      EXCUSED: { label: 'Có phép', bg: 'bg-blue-400', light: 'bg-blue-50 border-blue-200 text-blue-800', row: 'border-blue-100 bg-blue-50/10' },
                    };
                    const cfg = statusConfig[currentStatus] || statusConfig.PRESENT;

                    return (
                      <article
                        key={key}
                        className={`rounded-2xl border p-4 transition ${cfg.row}`}
                      >
                        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                          {/* Student info */}
                          <div className="flex items-center gap-3">
                            <div className={`flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full text-sm font-extrabold ${
                              currentStatus === 'PRESENT' ? 'bg-emerald-100 text-emerald-800'
                              : currentStatus === 'ABSENT' ? 'bg-rose-100 text-rose-800'
                              : currentStatus === 'LATE' ? 'bg-amber-100 text-amber-800'
                              : 'bg-blue-100 text-blue-800'
                            }`}>
                              {item.studentName ? item.studentName.charAt(0).toUpperCase() : 'H'}
                            </div>
                            <div>
                              <p className="font-extrabold text-[#2b2828]">{item.studentName || `Học viên #${key}`}</p>
                              <p className="text-[10px] font-bold text-[#8b706e]">
                                Ghi nhận trước: {formatAttendanceStatus(item.status)}
                              </p>
                            </div>
                          </div>

                          {/* Visual pill selector */}
                          <div className="flex flex-wrap gap-1.5">
                            {attendanceOptions.map((opt) => {
                              const isSelected = currentStatus === opt.value;
                              const pillColors = {
                                PRESENT: isSelected ? 'bg-emerald-500 text-white shadow-sm shadow-emerald-200' : 'border border-emerald-100 text-emerald-700 hover:bg-emerald-50',
                                ABSENT: isSelected ? 'bg-rose-500 text-white shadow-sm shadow-rose-200' : 'border border-rose-100 text-rose-700 hover:bg-rose-50',
                                LATE: isSelected ? 'bg-amber-400 text-white shadow-sm shadow-amber-200' : 'border border-amber-100 text-amber-700 hover:bg-amber-50',
                                EXCUSED: isSelected ? 'bg-blue-500 text-white shadow-sm shadow-blue-200' : 'border border-blue-100 text-blue-700 hover:bg-blue-50',
                              };
                              return (
                                <button
                                  key={opt.value}
                                  type="button"
                                  className={`rounded-xl px-3 py-1.5 text-[10px] font-extrabold transition-all active:scale-95 ${pillColors[opt.value]}`}
                                  onClick={() => setRecords((curr) => ({ ...curr, [key]: opt.value }))}
                                >
                                  {opt.label}
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </section>
          </>
        ) : null}
      </main>

      {/* Sticky Footer for Attendance Saving */}
      {!loading && !error && attendance.length ? (
        <div className="fixed bottom-0 left-0 right-0 z-30 border-t border-[#dfbfbd]/20 bg-white/90 py-4 shadow-2xl backdrop-blur-md">
          <div className="mx-auto flex max-w-[1320px] flex-col gap-4 px-4 sm:flex-row sm:items-center sm:justify-between md:px-10">
            <div className="flex flex-wrap items-center gap-x-6 gap-y-1 text-xs text-[#584140]">
              <span className="font-extrabold text-[#2b2828] uppercase tracking-wider">Tổng hợp nhanh:</span>
              <span className="flex items-center gap-1 font-bold text-emerald-700">
                <span className="h-2 w-2 rounded-full bg-emerald-500"></span> Có mặt: {summaryStats.present}
              </span>
              <span className="flex items-center gap-1 font-bold text-rose-700">
                <span className="h-2 w-2 rounded-full bg-rose-500"></span> Vắng: {summaryStats.absent}
              </span>
              <span className="flex items-center gap-1 font-bold text-amber-700">
                <span className="h-2 w-2 rounded-full bg-amber-500"></span> Muộn: {summaryStats.late}
              </span>
              <span className="flex items-center gap-1 font-bold text-purple-700">
                <span className="h-2 w-2 rounded-full bg-purple-500"></span> Có phép: {summaryStats.excused}
              </span>
            </div>

            <button
              className="inline-flex items-center justify-center gap-1.5 rounded-2xl bg-[#4b0009] px-6 py-3.5 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow-lg active:scale-95"
              onClick={handleSaveAttendance}
              type="button"
            >
              <Check className="h-4 w-4" />
              Lưu bảng điểm danh
            </button>
          </div>
        </div>
      ) : null}

      <CourseFooter />
    </div>
  );
}
