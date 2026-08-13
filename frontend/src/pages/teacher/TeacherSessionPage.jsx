import { useEffect, useState, useMemo } from 'react';
import { Link, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
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
  Download,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import CoursePageShell from '../../components/course/CoursePageShell';
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
  getClassroomSessionTitle,
  getClassroomSessionUnitLabel,
} from '../../utils/classroomHelpers';
import { downloadCsv, sanitizeCsvFilename } from '../../utils/csvExport';
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
  const [sessionMeta, setSessionMeta] = useState(null);
  const [records, setRecords] = useState({});

  const loadAttendance = async () => {
    setLoading(true);
    setError('');
    try {
      const [data, session] = await Promise.all([
        classroomApi.getSessionAttendance(sessionId),
        classroomApi.getTeacherSession(sessionId),
      ]);
      setAttendance(data);
      setSessionMeta(session);
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

  const handleOpenSession = () => {
    setActionMessage('');
    setLarkMessage('');
    if (!sessionMeta?.larkMeetingUrl) {
      setActionMessage('Staff chưa tạo liên kết Google Meet cho buổi học này.');
      return;
    }
    const roomWindow = window.open(sessionMeta.larkMeetingUrl, '_blank', 'noopener,noreferrer');
    if (!roomWindow) {
      setLarkMessage('Trình duyệt đã chặn cửa sổ mới. Hãy cho phép popup cho EnglishLab rồi thử lại.');
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

  const handleExportAttendance = () => {
    const rows = attendance.map((item) => {
      const key = item.studentId || item.enrollmentId;
      const status = records[key] || item.status || 'PRESENT';
      return [
        item.studentName || `Học viên #${key}`,
        item.studentEmail || '',
        formatAttendanceStatus(status),
        sessionMeta?.sessionDate ? formatClassroomDate(sessionMeta.sessionDate) : '',
        [formatClassroomTime(sessionMeta?.startTime), formatClassroomTime(sessionMeta?.endTime)].filter(Boolean).join(' - '),
        sessionMeta?.classroomTitle || sessionMeta?.classroomName || '',
      ];
    });
    const datePart = sessionMeta?.sessionDate || `session-${sessionId}`;
    downloadCsv(
      `${sanitizeCsvFilename(`diem-danh-${datePart}`)}.csv`,
      ['Tên học viên', 'Email', 'Trạng thái điểm danh', 'Ngày học', 'Giờ học', 'Lớp học'],
      rows
    );
  };

  const attendanceSummaryBar = !loading && !error && attendance.length ? (
    <div className="mt-auto pb-2">
      <div className="flex flex-col gap-4 rounded-2xl border border-[#e5e7eb] bg-white px-5 py-4 shadow-sm sm:flex-row sm:items-center sm:justify-between md:px-6">
        <div className="flex flex-wrap items-center gap-x-6 gap-y-1 text-xs text-[#584140]">
          <span className="font-extrabold uppercase tracking-wider text-[#2b2828]">Tổng hợp nhanh:</span>
          <span className="flex items-center gap-1 font-bold text-emerald-700">
            <span className="h-2 w-2 rounded-full bg-emerald-500" /> Có mặt: {summaryStats.present}
          </span>
          <span className="flex items-center gap-1 font-bold text-rose-700">
            <span className="h-2 w-2 rounded-full bg-rose-500" /> Vắng: {summaryStats.absent}
          </span>
          <span className="flex items-center gap-1 font-bold text-amber-700">
            <span className="h-2 w-2 rounded-full bg-amber-500" /> Muộn: {summaryStats.late}
          </span>
          <span className="flex items-center gap-1 font-bold text-purple-700">
            <span className="h-2 w-2 rounded-full bg-purple-500" /> Có phép: {summaryStats.excused}
          </span>
        </div>
        <button
          className="inline-flex items-center justify-center gap-1.5 rounded-xl bg-[#4b0009] px-6 py-3 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] active:scale-95"
          onClick={handleSaveAttendance}
          type="button"
        >
          <Check className="h-4 w-4" />
          Lưu bảng điểm danh
        </button>
      </div>
    </div>
  ) : null;

  return (
    <CoursePageShell bottomBar={attendanceSummaryBar}>
      <motion.div
        className="flex flex-1 flex-col space-y-8"
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.32, ease: 'easeOut' }}
      >
        {/* ── Session Header ── */}
        <section className="rounded-2xl border border-[#e5e7eb] bg-white p-6 shadow-sm md:p-10">
          <div className="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
            <div className="min-w-0 flex-1">
              <p className="mb-2 text-xs font-semibold text-[#9a8b8a] uppercase tracking-wide">Điểm danh buổi học</p>
              {sessionMeta?.sessionNumber != null ? (
                <p className="mb-1 text-[11px] font-extrabold uppercase tracking-[0.14em] text-[#730014]">
                  Buổi {sessionMeta.sessionNumber}
                </p>
              ) : null}
              <h1 className="font-['Manrope'] text-2xl font-extrabold tracking-tight text-[#1a1c1c] md:text-3xl">
                {sessionMeta
                  ? getClassroomSessionTitle(sessionMeta, formatClassroomDate(sessionMeta.sessionDate))
                  : `Buổi học #${sessionId}`}
              </h1>
              {getClassroomSessionUnitLabel(sessionMeta) ? (
                <p className="mt-1 text-sm font-semibold text-[#8b706e]">{getClassroomSessionUnitLabel(sessionMeta)}</p>
              ) : null}
              {sessionMeta && (
                <div className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-sm text-[#6a5553]">
                  <span className="flex items-center gap-1.5">
                    <Clock className="h-4 w-4 text-[#8a0018]" />
                    {formatClassroomTime(sessionMeta.startTime)} – {formatClassroomTime(sessionMeta.endTime)}
                  </span>
                  {sessionMeta.deliveryMode === 'VIRTUAL' ? (
                    <span className="flex items-center gap-1.5 font-semibold text-sky-700">
                      <Video className="h-4 w-4" /> Trực tuyến
                    </span>
                  ) : (
                    <span className="flex items-center gap-1.5">
                      <MapPin className="h-4 w-4 text-[#8a0018]" />
                      {sessionMeta.roomName || 'Phòng học offline'}
                    </span>
                  )}
                  <span className="flex items-center gap-1.5">
                    <Users className="h-4 w-4 text-[#8a0018]" />
                    {attendance.length} học viên
                  </span>
                </div>
              )}
            </div>

            <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center xl:flex-shrink-0">
              {attendance.length > 0 && (
                <div className="rounded-xl border border-[#e5e7eb] bg-white px-4 py-2.5 min-w-[180px]">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-semibold text-[#6a5553]">Tỉ lệ có mặt</span>
                    <span className="font-extrabold text-emerald-700">
                      {summaryStats.present}/{attendance.length}
                    </span>
                  </div>
                  <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-gray-100">
                    <div
                      className="h-full rounded-full bg-emerald-500 transition-all"
                      style={{ width: `${Math.round((summaryStats.present / attendance.length) * 100)}%` }}
                    />
                  </div>
                </div>
              )}
              {sessionMeta && (
                <Link
                  className="inline-flex flex-shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#e5e7eb] bg-white px-4 py-2 text-sm font-semibold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95"
                  to={`/teacher/classrooms/${sessionMeta.classroomOfferingId || sessionMeta.classroomId}`}
                >
                  <ArrowLeft className="h-4 w-4" />
                  Quay lại lớp học
                </Link>
              )}
            </div>
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
              <section className="rounded-xl border border-[#e5e7eb] bg-white p-5 md:p-8 space-y-5">
                <div className="flex items-start gap-3">
                  <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-[#fff0f1] text-[#8a0018] flex-shrink-0">
                    <Video className="h-5 w-5" />
                  </div>
                  <div>
                    <h2 className="font-semibold text-base text-[#1a1c1c]">Vận hành lớp học trực tuyến</h2>
                    <p className="mt-0.5 text-xs text-[#8b706e]">Mở phòng học trực tuyến, cập nhật liên kết và đóng phòng sau khi kết thúc buổi giảng.</p>
                  </div>
                </div>

                <div className="flex flex-wrap gap-3 pt-2">
                  {sessionMeta?.larkMeetingUrl ? (
                    <button
                      className="inline-flex items-center gap-1.5 rounded-xl bg-[#8a0018] px-5 py-3 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#650011] active:scale-95"
                      onClick={handleOpenSession}
                      type="button"
                    >
                      <Check className="h-4 w-4" />
                      Vào phòng học
                    </button>
                  ) : (
                    <p className="rounded-xl border border-sky-100 bg-sky-50 px-4 py-3 text-xs font-semibold leading-5 text-sky-900">
                      Staff đang chuẩn bị liên kết Google Meet. Liên hệ staff nếu cần hỗ trợ.
                    </p>
                  )}
                  <button
                    className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-5 py-3 text-xs font-extrabold text-[#584140] transition hover:bg-gray-50 active:scale-95"
                    onClick={handleCloseSession}
                    type="button"
                  >
                    <X className="h-4 w-4" />
                    Đóng buổi học trực tuyến
                  </button>
                </div>

                {sessionMeta?.larkSyncError ? (
                  <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
                    <p className="font-extrabold">Chưa thể mở phòng học tự động</p>
                    <p className="mt-1 leading-6">{sessionMeta.larkSyncError}</p>
                    <p className="mt-2 text-xs font-semibold">Staff cần xử lý liên kết Google Meet trước khi giáo viên vào phòng.</p>
                  </div>
                ) : null}

                {sessionMeta?.larkMeetingUrl && (
                  <div className="rounded-2xl border border-[#ead0d2] bg-[#fffafb] p-4">
                    <LarkJoinButton
                      label="Vào phòng học"
                      onBlocked={setLarkMessage}
                      onClick={handleOpenSession}
                      url={sessionMeta.larkMeetingUrl}
                    />
                  </div>
                )}
                {larkMessage ? <p className="text-sm font-semibold text-[#93000a]">{larkMessage}</p> : null}

              </section>
            )}

            {/* Attendance Tool */}
            <section className="rounded-2xl border border-[#e5e7eb] bg-white p-6 shadow-sm md:p-10 space-y-5">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between lg:gap-6">
                <div className="flex items-start gap-3">
                  <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-rose-50 text-[#8a0018] flex-shrink-0">
                    <Users className="h-5 w-5" />
                  </div>
                  <div>
                    <h2 className="font-semibold text-base text-[#1a1c1c]">Điểm danh lớp học</h2>
                    <p className="mt-0.5 text-xs text-[#8b706e]">Chọn trạng thái chuyên cần cho từng học viên. Sử dụng các phím tắt để thao tác nhanh.</p>
                  </div>
                </div>

                <div className="flex flex-wrap gap-2">
                  <button
                    className="inline-flex items-center gap-1 rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-extrabold text-[#4b0009] transition hover:bg-slate-50 disabled:opacity-50"
                    disabled={!attendance.length}
                    onClick={handleExportAttendance}
                    type="button"
                  >
                    <Download className="h-3.5 w-3.5" /> Xuất CSV điểm danh
                  </button>
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
                        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:gap-4">
                          {/* Student info */}
                          <div className="flex min-w-0 flex-1 items-center gap-3">
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
                          <div className="flex flex-wrap gap-1.5 sm:justify-end">
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
      </motion.div>
    </CoursePageShell>
  );
}
