import { useEffect, useState, useMemo } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  BookOpen,
  Calendar,
  Clock,
  MapPin,
  Video,
  User,
  Award,
  CheckCircle2,
  XCircle,
  AlertCircle,
  FileText,
  MessageSquare,
  ArrowLeft,
  Download,
  Bell,
  Play,
  ExternalLink,
  ChevronRight,
  Info,
  Activity,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  ClassroomTabBar,
  LarkJoinButton,
  PageHero,
  ClassroomTypeBadge,
  StatusBadge,
  ProgressBar,
  ProgressRing,
  Timeline,
  ScheduleCard,
} from '../../components/classroom/ClassroomUi';
import LearnerPageShell from '../../components/learner/LearnerPageShell';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatAttendanceStatus,
  formatClassroomDate,
  formatClassroomDateTime,
  formatClassroomTime,
  formatDeliveryMode,
  formatGradebookFinalResult,
  formatHomeworkStatus,
  getHomeworkMaxScore,
  getSubmissionFeedback,
  isGradebookPassed,
  formatSessionStatus,
  downloadClassroomMaterial,
} from '../../utils/classroomHelpers';
import {
  getHomeworkFeedbackLabel,
  getHomeworkGradingHint,
  getHomeworkSkillLabel,
  isAiGradedHomework,
} from '../../utils/homeworkGradingConfig';

const detailTabs = [
  { id: 'overview', label: 'Tổng quan' },
  { id: 'schedule', label: 'Lịch học' },
  { id: 'homework', label: 'Bài tập' },
  { id: 'attendance', label: 'Điểm danh' },
  { id: 'gradebook', label: 'Bảng điểm' },
  { id: 'materials', label: 'Tài liệu' },
  { id: 'announcements', label: 'Thông báo' },
];

export default function MyClassroomDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('overview');
  const [classroom, setClassroom] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [homework, setHomework] = useState([]);
  const [attendance, setAttendance] = useState([]);
  const [gradebook, setGradebook] = useState(null);
  const [materials, setMaterials] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [syllabus, setSyllabus] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [submittingId, setSubmittingId] = useState(null);
  const [submitAnswers, setSubmitAnswers] = useState({});
  const [actionMessage, setActionMessage] = useState('');
  const [larkMessage, setLarkMessage] = useState('');

  const loadClassroom = async () => {
    setLoading(true);
    setError('');
    try {
      const [classroomData, sessionsData, homeworkData, attendanceData, materialsData, announcementsData, syllabusData] = await Promise.all([
        classroomApi.getMyClassroom(id),
        classroomApi.getMyClassroomSessions(id),
        classroomApi.getMyClassroomHomework(id),
        classroomApi.getMyAttendance(id),
        classroomApi.getMyClassroomMaterials(id),
        classroomApi.getMyClassroomAnnouncements(id),
        classroomApi.getMyClassroomSyllabus(id),
      ]);
      let gradebookData = null;
      try {
        gradebookData = await classroomApi.getMyGradebook(id);
      } catch {
        gradebookData = null;
      }
      setClassroom(classroomData);
      setSessions(sessionsData);
      setHomework(homeworkData);
      setAttendance(attendanceData);
      setGradebook(gradebookData);
      setMaterials(materialsData);
      setAnnouncements(announcementsData);
      setSyllabus(syllabusData);
    } catch (err) {
      const message = getClassroomErrorMessage(err, 'Không thể tải dữ liệu lớp học.');
      if (/không có quyền|không thuộc|chưa đăng ký|not enrolled|access/i.test(message)) {
        navigate('/my-classrooms', {
          replace: true,
          state: { accessMessage: 'Bạn không còn quyền truy cập lớp này. Hãy đăng ký lại nếu muốn tham gia.' },
        });
        return;
      }
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadClassroom();
  }, [id]);

  const handleSubmitHomework = async (homeworkId) => {
    setSubmittingId(homeworkId);
    setActionMessage('');
    try {
      await classroomApi.submitHomework(homeworkId, {
        textAnswer: submitAnswers[homeworkId] || '',
        attachmentUrl: '',
      });
      setActionMessage('Đã nộp bài tập thành công.');
      const refreshed = await classroomApi.getMyClassroomHomework(id);
      setHomework(refreshed);
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể nộp bài tập.'));
    } finally {
      setSubmittingId(null);
    }
  };

  // Calculate Attendance Stats
  const attendanceStats = useMemo(() => {
    if (!attendance.length) return { total: 0, present: 0, absent: 0, late: 0, rate: 0 };
    const total = attendance.length;
    const present = attendance.filter((r) => r.status === 'PRESENT').length;
    const absent = attendance.filter((r) => r.status === 'ABSENT').length;
    const late = attendance.filter((r) => r.status === 'LATE').length;
    const rate = Math.round(((present + late * 0.5) / total) * 100);
    return { total, present, absent, late, rate };
  }, [attendance]);

  // ── Derived data for overview tab ──────────────────────────────────────────
  const nextSession = useMemo(() => {
    const upcoming = sessions.filter((s) => s.status === 'SCHEDULED' || s.status === 'OPEN');
    if (upcoming.length) return upcoming[0];
    return null;
  }, [sessions]);

  const pendingHomework = useMemo(() =>
    homework.filter((h) => !h.mySubmission).sort((a, b) => new Date(a.deadline) - new Date(b.deadline)),
    [homework]);

  const canResubmitHomework = (item) => {
    if (!item || item.status !== 'OPEN' || item.overdue) return false;
    if (!item.mySubmission) return true;
    if (item.mySubmission.status === 'SUBMITTED') return true;
    return Boolean(item.allowResubmission);
  };

  const renderTabContent = () => {
    if (activeTab === 'overview') {
      const isVirtual = classroom.deliveryMode === 'VIRTUAL';
      const totalSessions = sessions.length;
      const attendedCount = attendanceStats.present + Math.round(attendanceStats.late * 0.5);

      return (
        <div className="space-y-6">

          {/* ── Quick Action Bar ── */}
          <div className="flex flex-wrap gap-2.5">
            {isVirtual && classroom.larkMeetingUrl && (
              <LarkJoinButton
                onBlocked={setLarkMessage}
                url={classroom.larkMeetingUrl}
              />
            )}
            <button
              className="inline-flex items-center gap-1.5 rounded-xl border border-[#dfbfbd] bg-white px-4 py-2 text-xs font-extrabold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95"
              onClick={() => setActiveTab('schedule')}
              type="button"
            >
              <Calendar className="h-3.5 w-3.5" />
              Xem lịch học
            </button>
            {pendingHomework.length > 0 && (
              <button
                className="inline-flex items-center gap-1.5 rounded-xl border border-amber-100 bg-amber-50 px-4 py-2 text-xs font-extrabold text-amber-800 transition hover:bg-amber-100 active:scale-95"
                onClick={() => setActiveTab('homework')}
                type="button"
              >
                <AlertCircle className="h-3.5 w-3.5" />
                {pendingHomework.length} bài tập chưa nộp
              </button>
            )}
            {larkMessage && <p className="w-full text-xs text-rose-700 font-semibold">{larkMessage}</p>}
          </div>

          {/* ── KPI Row ── */}
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <KpiCard
              label="Buổi đã học"
              value={`${attendedCount} / ${totalSessions}`}
              sub={totalSessions ? `${Math.round((attendedCount / totalSessions) * 100)}% tham dự` : 'Chưa có dữ liệu'}
              color="rose"
              icon={<CheckCircle2 className="h-4 w-4" />}
            />
            <KpiCard
              label="Bài tập đã nộp"
              value={`${homework.filter((h) => h.mySubmission).length} / ${homework.length}`}
              sub={pendingHomework.length ? `Còn ${pendingHomework.length} chưa nộp` : 'Đã hoàn thành tất cả'}
              color={pendingHomework.length ? 'amber' : 'emerald'}
              icon={<FileText className="h-4 w-4" />}
            />
            <KpiCard
              label="Chuyên cần"
              value={`${attendanceStats.rate}%`}
              sub={attendanceStats.rate >= 80 ? 'Đạt yêu cầu' : 'Cần cải thiện'}
              color={attendanceStats.rate >= 80 ? 'emerald' : 'rose'}
              icon={<Activity className="h-4 w-4" />}
            />
            <KpiCard
              label="Điểm tổng hợp"
              value={gradebook?.homeworkScore != null ? `${gradebook.homeworkScore}/10` : '—'}
              sub={gradebook ? formatGradebookFinalResult(gradebook.finalResult) : 'Chưa công bố'}
              color="blue"
              icon={<Award className="h-4 w-4" />}
            />
          </div>

          {/* ── Next Session ── */}
          <div>
            <h3 className="mb-3 text-xs font-extrabold uppercase tracking-wider text-[#8b706e]">Buổi học tiếp theo</h3>
            {nextSession ? (
              <div className={`rounded-2xl border p-5 ${
                nextSession.status === 'OPEN'
                  ? 'border-emerald-100 bg-emerald-50/20'
                  : 'border-[#dfbfbd]/20 bg-[#fffafb]/30'
              }`}>
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="space-y-2">
                    {nextSession.status === 'OPEN' && (
                      <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-[10px] font-extrabold text-emerald-800">
                        <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-600" />
                        Đang diễn ra
                      </span>
                    )}
                    <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">
                      {nextSession.sessionContent || `Buổi học ngày ${formatClassroomDate(nextSession.sessionDate)}`}
                    </h4>
                    <div className="flex flex-wrap gap-x-4 gap-y-1.5 text-xs text-[#584140]">
                      <span className="flex items-center gap-1.5">
                        <Calendar className="h-3.5 w-3.5 text-[#730014]" />
                        {formatClassroomDate(nextSession.sessionDate)}
                      </span>
                      <span className="flex items-center gap-1.5">
                        <Clock className="h-3.5 w-3.5 text-[#730014]" />
                        {formatClassroomTime(nextSession.startTime)} – {formatClassroomTime(nextSession.endTime)}
                      </span>
                      {isVirtual ? (
                        <span className="flex items-center gap-1.5 text-purple-700 font-bold">
                          <Video className="h-3.5 w-3.5" />
                          Trực tuyến
                        </span>
                      ) : (
                        <span className="flex items-center gap-1.5">
                          <MapPin className="h-3.5 w-3.5 text-[#730014]" />
                          {nextSession.roomName ? `Phòng ${nextSession.roomName}` : 'Đang xếp phòng'} · {nextSession.offlineAddress || 'Cơ sở Hà Nội'}
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="flex flex-shrink-0 flex-wrap gap-2">
                    {isVirtual && nextSession.larkJoinable && nextSession.larkMeetingUrl && (
                      <LarkJoinButton
                        className="!px-4 !py-2 !text-xs"
                        onBlocked={setLarkMessage}
                        url={nextSession.larkMeetingUrl}
                      />
                    )}
                    {nextSession.recordingUrl && (
                      <a
                        className="inline-flex items-center gap-1.5 rounded-xl border border-[#dfbfbd] bg-white px-4 py-2 text-xs font-extrabold text-[#4b0009] hover:bg-[#fff3f4]"
                        href={nextSession.recordingUrl}
                        rel="noreferrer"
                        target="_blank"
                      >
                        <Play className="h-3.5 w-3.5" />
                        Ghi âm
                      </a>
                    )}
                  </div>
                </div>
              </div>
            ) : (
              <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50/50 p-5 text-center text-sm text-[#8b706e]">
                Không có buổi học sắp tới — lớp học đã hoàn thành tất cả các buổi lên lịch.
              </div>
            )}
          </div>

          {/* ── Pending Homework ── */}
          {pendingHomework.length > 0 && (
            <div>
              <h3 className="mb-3 text-xs font-extrabold uppercase tracking-wider text-[#8b706e]">
                Bài tập cần nộp ({pendingHomework.length})
              </h3>
              <div className="space-y-2">
                {pendingHomework.slice(0, 3).map((item) => {
                  const deadline = item.deadline ? new Date(item.deadline) : null;
                  const hoursLeft = deadline ? Math.round((deadline - Date.now()) / 3600000) : null;
                  const isUrgent = hoursLeft != null && hoursLeft < 24 && hoursLeft > 0;
                  const isOverdue = hoursLeft != null && hoursLeft <= 0;

                  return (
                    <div
                      key={item.id}
                      className={`flex items-center justify-between gap-4 rounded-2xl border px-4 py-3 ${
                        isOverdue ? 'border-rose-100 bg-rose-50/20' : isUrgent ? 'border-amber-100 bg-amber-50/20' : 'border-gray-100 bg-white'
                      }`}
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <div className={`flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-xl ${
                          isOverdue ? 'bg-rose-100 text-rose-700' : isUrgent ? 'bg-amber-100 text-amber-700' : 'bg-rose-50 text-[#730014]'
                        }`}>
                          <FileText className="h-4 w-4" />
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-extrabold text-[#2b2828] truncate">{item.title}</p>
                          <p className={`text-[10px] font-bold ${isOverdue ? 'text-rose-700' : isUrgent ? 'text-amber-700' : 'text-[#8b706e]'}`}>
                            {isOverdue ? 'Đã quá hạn' : isUrgent ? `Còn ${hoursLeft} giờ` : `Hạn: ${formatClassroomDate(item.deadline)}`}
                          </p>
                        </div>
                      </div>
                      <button
                        className="flex-shrink-0 rounded-xl bg-[#4b0009] px-3 py-1.5 text-[10px] font-extrabold text-white hover:bg-[#730014] active:scale-95"
                        onClick={() => setActiveTab('homework')}
                        type="button"
                      >
                        Nộp bài
                      </button>
                    </div>
                  );
                })}
                {pendingHomework.length > 3 && (
                  <button
                    className="w-full rounded-xl border border-dashed border-gray-200 py-2 text-xs font-bold text-[#8b706e] hover:border-[#dfbfbd] hover:text-[#730014] transition"
                    onClick={() => setActiveTab('homework')}
                    type="button"
                  >
                    Xem thêm {pendingHomework.length - 3} bài tập khác →
                  </button>
                )}
              </div>
            </div>
          )}

          {/* ── Latest Announcement ── */}
          {announcements.length > 0 && (
            <div>
              <h3 className="mb-3 text-xs font-extrabold uppercase tracking-wider text-[#8b706e]">Thông báo mới nhất</h3>
              <div
                className="rounded-2xl border border-gray-100 bg-white p-5 space-y-2 cursor-pointer hover:border-[#dfbfbd]/50 transition"
                onClick={() => setActiveTab('announcements')}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => e.key === 'Enter' && setActiveTab('announcements')}
              >
                <div className="flex items-start justify-between gap-3">
                  <h4 className="font-['Manrope'] text-sm font-extrabold text-[#2b2828] flex items-center gap-2">
                    <Bell className="h-4 w-4 text-[#730014] flex-shrink-0" />
                    {announcements[0].title}
                  </h4>
                  <span className="flex-shrink-0 text-[10px] font-bold text-gray-400">
                    {formatClassroomDateTime(announcements[0].createdAt)}
                  </span>
                </div>
                <p className="text-xs text-[#584140] line-clamp-2 leading-5 ml-6">
                  {announcements[0].content || announcements[0].body}
                </p>
                {announcements.length > 1 && (
                  <p className="text-[10px] font-bold text-[#730014] ml-6">
                    + {announcements.length - 1} thông báo khác →
                  </p>
                )}
              </div>
            </div>
          )}

          {/* ── Syllabus ── */}
          {syllabus.length > 0 && (
            <div>
              <h3 className="mb-3 text-xs font-extrabold uppercase tracking-wider text-[#8b706e]">Đề cương khóa học</h3>
              <div className="space-y-2">
                {syllabus.map((item, idx) => (
                  <div
                    key={item.id}
                    className="flex items-start gap-4 rounded-2xl border border-gray-100 bg-white p-4 hover:bg-[#fffafb] transition"
                  >
                    <span className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-xl bg-[#fff1f3] text-[10px] font-extrabold text-[#730014]">
                      {item.weekNumber || idx + 1}
                    </span>
                    <div>
                      <p className="text-sm font-extrabold text-[#2b2828]">{item.title}</p>
                      {item.description && <p className="mt-0.5 text-xs text-[#584140] leading-5">{item.description}</p>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      );
    }

    if (activeTab === 'schedule') {
      if (!sessions.length) {
        return (
          <ClassroomEmptyState
            description="Lớp học chưa có buổi học nào được lên lịch."
            title="Chưa có lịch học"
          />
        );
      }
      return (
        <div className="space-y-6">
          <Timeline>
            {sessions.map((session) => {
              const isVirtual = session.deliveryMode === 'VIRTUAL';
              const isLarkJoinable = isVirtual && session.larkJoinable && session.larkMeetingUrl;

              return (
                <ScheduleCard
                  key={session.id}
                  title={session.sessionContent || `Buổi học ngày ${formatClassroomDate(session.sessionDate)}`}
                  subtitle={session.roomName || session.offlineAddress || 'Thông tin phòng học đang cập nhật'}
                  date={formatClassroomDate(session.sessionDate)}
                  time={`${formatClassroomTime(session.startTime)} - ${formatClassroomTime(session.endTime)}`}
                  mode={session.deliveryMode}
                  status={session.status}
                  location={
                    isVirtual
                      ? 'Lớp học trực tuyến'
                      : `${session.roomName || 'Đang xếp phòng'} · ${session.offlineAddress || 'Cơ sở Hà Nội'}`
                  }
                  active={session.status === 'IN_PROGRESS' || session.status === 'OPEN'}
                  cta={
                    <div className="flex flex-wrap items-center gap-3">
                      {isLarkJoinable && (
                        <LarkJoinButton
                          className="!px-4 !py-2.5 !text-xs"
                          onBlocked={setLarkMessage}
                          url={session.larkMeetingUrl}
                        />
                      )}
                      {session.recordingUrl && (
                        <a
                          className="inline-flex items-center gap-1.5 rounded-xl border border-[#dfbfbd] bg-white px-4 py-2.5 text-xs font-extrabold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95"
                          href={session.recordingUrl}
                          rel="noreferrer"
                          target="_blank"
                        >
                          <Play className="h-3.5 w-3.5" />
                          Xem ghi âm
                        </a>
                      )}
                    </div>
                  }
                />
              );
            })}
          </Timeline>
          {larkMessage ? <p className="text-sm font-semibold text-[#93000a]">{larkMessage}</p> : null}
        </div>
      );
    }

    if (activeTab === 'homework') {
      if (!homework.length) {
        return (
          <ClassroomEmptyState
            description="Giảng viên chưa giao bài tập nào cho lớp học này."
            title="Chưa có bài tập"
          />
        );
      }
      return (
        <div className="grid gap-6 md:grid-cols-2">
          {homework.map((item) => {
            const hasSubmission = !!item.mySubmission;
            const isGraded = hasSubmission && item.mySubmission.score != null;
            const isOverdue = item.overdue && !hasSubmission;
            const canSubmit = canResubmitHomework(item);

            return (
              <article
                key={item.id}
                className="flex flex-col overflow-hidden rounded-xl border border-[#e5e7eb] bg-white p-5 transition hover:shadow-sm"
              >
                <div className="flex items-center justify-between gap-3">
                  <span className="text-xs font-extrabold uppercase tracking-wider text-[#730014]">
                    Bài tập viết / thực hành
                  </span>
                  <StatusBadge status={isGraded ? 'GRADED' : hasSubmission ? 'SUBMITTED' : isOverdue ? 'OVERDUE' : 'NOT_SUBMITTED'} />
                </div>

                <h3 className="mt-4 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">
                  {item.title}
                </h3>
                {isAiGradedHomework(item) ? (
                  <p className="mt-1 text-[11px] font-bold text-purple-700">
                    AI chấm · {getHomeworkSkillLabel(item.skill)}
                  </p>
                ) : null}
                {getHomeworkGradingHint(item) && !hasSubmission ? (
                  <p className="mt-2 text-xs leading-5 text-purple-800">{getHomeworkGradingHint(item)}</p>
                ) : null}
                <p className="mt-2 text-sm text-[#584140] line-clamp-3">
                  {item.instruction || 'Không có hướng dẫn chi tiết.'}
                </p>

                <div className="mt-4 text-xs text-[#8b706e] flex items-center gap-2">
                  <Clock className="h-4 w-4 text-[#730014]" />
                  <span>Hạn nộp: <strong>{formatClassroomDateTime(item.deadline)}</strong></span>
                </div>

                {isGraded && (
                  <div className="mt-4 rounded-xl bg-emerald-50 border border-emerald-100 p-4 space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-emerald-800 uppercase tracking-wider flex items-center gap-1">
                        <Award className="h-4 w-4" />
                        Kết quả chấm điểm
                      </span>
                      <strong className="text-emerald-700 text-sm font-extrabold">{item.mySubmission.score} / {getHomeworkMaxScore(item)}</strong>
                    </div>
                    {getSubmissionFeedback(item.mySubmission) && (
                      <p className="text-xs text-[#584140] italic">{getHomeworkFeedbackLabel(item)}: "{getSubmissionFeedback(item.mySubmission)}"</p>
                    )}
                  </div>
                )}

                {hasSubmission && !isGraded && (
                  <div className="mt-4 rounded-xl bg-blue-50 border border-blue-100 p-4">
                    <p className="text-xs font-bold text-blue-800 uppercase tracking-wider flex items-center gap-1">
                      <CheckCircle2 className="h-4 w-4" />
                      {isAiGradedHomework(item) ? 'Đã nộp — đang chờ AI chấm' : 'Đã nộp bài làm'}
                    </p>
                    <p className="mt-1 text-xs text-[#584140] line-clamp-2">Nội dung: "{item.mySubmission.textAnswer}"</p>
                  </div>
                )}

                {canSubmit && (
                  <div className="mt-6 space-y-3">
                    <textarea
                      className="min-h-[100px] w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none focus:border-[#730014] focus:bg-white"
                      onChange={(e) => setSubmitAnswers((curr) => ({ ...curr, [item.id]: e.target.value }))}
                      placeholder="Nhập bài làm của bạn..."
                      value={submitAnswers[item.id] ?? item.mySubmission?.textAnswer ?? ''}
                    />
                    <button
                      className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-2.5 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014] disabled:opacity-60"
                      disabled={submittingId === item.id}
                      onClick={() => handleSubmitHomework(item.id)}
                      type="button"
                    >
                      {submittingId === item.id ? 'Đang nộp...' : hasSubmission ? 'Cập nhật bài nộp' : 'Nộp bài tập'}
                    </button>
                  </div>
                )}
              </article>
            );
          })}
        </div>
      );
    }

    if (activeTab === 'attendance') {
      if (!attendance.length) {
        return (
          <ClassroomEmptyState
            description="Lớp học chưa có dữ liệu điểm danh nào được ghi nhận."
            title="Chưa có điểm danh"
          />
        );
      }

      return (
        <div className="space-y-6">
          {/* Attendance Stats Dashboard */}
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-5 items-center rounded-xl border border-[#ebebeb] bg-white p-5">
            <div className="lg:col-span-2 flex justify-center py-2">
              <ProgressRing percent={attendanceStats.rate} size={110} strokeWidth={10} label="Tỷ lệ chuyên cần" />
            </div>

            <div className="grid grid-cols-3 gap-4 sm:grid-cols-3 lg:col-span-3 w-full">
              <div className="rounded-2xl bg-white border border-gray-100 p-4 text-center">
                <p className="text-xs font-bold text-gray-400 uppercase tracking-wider">Tổng buổi</p>
                <p className="mt-1 font-['Manrope'] text-2xl font-extrabold text-gray-700">{attendanceStats.total}</p>
              </div>
              <div className="rounded-2xl bg-white border border-emerald-100 p-4 text-center">
                <p className="text-xs font-bold text-emerald-600 uppercase tracking-wider">Có mặt</p>
                <p className="mt-1 font-['Manrope'] text-2xl font-extrabold text-emerald-700">{attendanceStats.present}</p>
              </div>
              <div className="rounded-2xl bg-white border-rose-100 p-4 text-center">
                <p className="text-xs font-bold text-rose-600 uppercase tracking-wider">Vắng mặt</p>
                <p className="mt-1 font-['Manrope'] text-2xl font-extrabold text-rose-700">{attendanceStats.absent}</p>
              </div>
            </div>
          </div>

          {/* Attendance Timeline Table */}
          <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white">
            <table className="min-w-full divide-y divide-gray-100 text-left text-sm">
              <thead className="bg-[#fffafb] text-xs font-bold text-[#8b706e] uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-4">Buổi học</th>
                  <th className="px-6 py-4">Ghi chú / Chi tiết</th>
                  <th className="px-6 py-4">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-[#584140]">
                {attendance.map((record, idx) => (
                  <tr key={record.id || `${record.sessionId}-${idx}`} className="hover:bg-[#fffafb]/30">
                    <td className="whitespace-nowrap px-6 py-4 font-extrabold text-[#2b2828]">
                      Buổi #{record.sessionId}
                    </td>
                    <td className="px-6 py-4">
                      {record.note || 'Điểm danh lớp học'}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold ${
                        record.status === 'PRESENT'
                          ? 'bg-emerald-50 text-emerald-700'
                          : record.status === 'LATE'
                            ? 'bg-amber-50 text-amber-700'
                            : 'bg-rose-50 text-rose-700'
                      }`}>
                        {formatAttendanceStatus(record.status)}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      );
    }

    if (activeTab === 'gradebook') {
      if (!gradebook) {
        return (
          <ClassroomEmptyState
            description="Bảng điểm chính thức chưa được công bố bởi giảng viên hoặc điều phối đào tạo."
            title="Chưa có bảng điểm"
          />
        );
      }

      const finalResultLabel = formatGradebookFinalResult(gradebook.finalResult);
      const isPassed = isGradebookPassed(gradebook.finalResult);

      return (
        <div className="space-y-6">
          {/* Grade Summary Cards */}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm space-y-2">
              <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Điểm bài tập</p>
              <p className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{gradebook.homeworkScore ?? '—'}</p>
              <div className="h-1.5 w-full bg-gray-100 rounded-full overflow-hidden">
                <div className="h-full bg-[#730014] rounded-full" style={{ width: `${(gradebook.homeworkScore || 0) * 10}%` }}></div>
              </div>
            </div>

            <div className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm space-y-2">
              <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Điểm kiểm tra (Quiz)</p>
              <p className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{gradebook.quizScore ?? '—'}</p>
              <div className="h-1.5 w-full bg-gray-100 rounded-full overflow-hidden">
                <div className="h-full bg-blue-600 rounded-full" style={{ width: `${(gradebook.quizScore || 0) * 10}%` }}></div>
              </div>
            </div>

            <div className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm space-y-2">
              <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Chuyên cần</p>
              <p className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{gradebook.attendancePercent != null ? `${gradebook.attendancePercent}%` : '—'}</p>
              <div className="h-1.5 w-full bg-gray-100 rounded-full overflow-hidden">
                <div className="h-full bg-emerald-600 rounded-full" style={{ width: `${gradebook.attendancePercent || 0}%` }}></div>
              </div>
            </div>

            <div className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm space-y-2">
              <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Điểm phát biểu / tương tác</p>
              <p className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{gradebook.participationScore ?? '—'}</p>
              <div className="h-1.5 w-full bg-gray-100 rounded-full overflow-hidden">
                <div className="h-full bg-purple-600 rounded-full" style={{ width: `${(gradebook.participationScore || 0) * 10}%` }}></div>
              </div>
            </div>
          </div>

          {/* Final Result Banner */}
          <div className={`rounded-xl border p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 ${
            isPassed ? 'border-emerald-100 bg-emerald-50/30' : 'border-amber-100 bg-amber-50/30'
          }`}>
            <div>
              <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Kết quả tổng quát</p>
              <h4 className={`mt-1 font-['Manrope'] text-2xl font-extrabold ${isPassed ? 'text-emerald-800' : 'text-amber-800'}`}>
                {finalResultLabel}
              </h4>
            </div>
            <span className={`inline-flex items-center gap-1 rounded-full px-4 py-1.5 text-xs font-extrabold uppercase tracking-wider ${
              isPassed ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
            }`}>
              {isPassed ? <CheckCircle2 className="h-4 w-4" /> : <Info className="h-4 w-4" />}
              {isPassed ? 'Hoàn thành khóa học' : 'Đang đánh giá'}
            </span>
          </div>

          {/* Teacher Comment */}
          {gradebook.teacherComment && (
            <div className="rounded-2xl border border-[#dfbfbd]/20 bg-white p-5 space-y-2">
              <h4 className="text-xs font-bold text-[#730014] uppercase tracking-wider flex items-center gap-1">
                <MessageSquare className="h-4 w-4" />
                Nhận xét tổng hợp từ giảng viên
              </h4>
              <p className="text-sm text-[#584140] leading-6 italic">
                "{gradebook.teacherComment}"
              </p>
            </div>
          )}
        </div>
      );
    }

    if (activeTab === 'materials') {
      if (!materials.length) {
        return (
          <ClassroomEmptyState
            description="Chưa có tài liệu học tập nào được chia sẻ cho lớp học này."
            title="Chưa có tài liệu"
          />
        );
      }
      return (
        <div className="grid gap-4 sm:grid-cols-2">
          {materials.map((material) => (
            <article
              key={material.id}
              className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm flex flex-col justify-between hover:border-[#dfbfbd]/50 transition"
            >
              <div>
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-rose-50 text-[#730014] mb-4">
                  <FileText className="h-5 w-5" />
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828] line-clamp-1">
                    {material.title}
                  </h4>
                  <span className={`inline-flex rounded-full px-3 py-1 text-[10px] font-extrabold ${
                    material.sourceType === 'CENTER_LIBRARY'
                      ? 'bg-emerald-100 text-emerald-700'
                      : 'bg-[#fff2f3] text-[#730014]'
                  }`}>
                    {material.sourceType === 'CENTER_LIBRARY' ? 'Từ trung tâm' : 'Riêng của lớp'}
                  </span>
                </div>
                {material.description ? (
                  <p className="mt-2 text-xs text-[#584140] line-clamp-2 leading-5">{material.description}</p>
                ) : null}
                <div className="mt-3 space-y-1 text-[11px] font-semibold text-[#8b706e]">
                  <p>Nguồn: <span className="text-[#584140]">{material.provider || 'EnglishLab'}</span></p>
                  <p>Buổi học: <span className="text-[#584140]">{material.sessionTitle || 'Không gắn buổi cụ thể'}</span></p>
                </div>
              </div>

              {material.fileUrl && (
                <div className="mt-4 pt-4 border-t border-gray-50">
                  <button
                    className="inline-flex items-center gap-1.5 text-xs font-extrabold text-[#730014] hover:underline"
                    onClick={() => downloadClassroomMaterial(material)}
                    type="button"
                  >
                    <Download className="h-4 w-4" />
                    Tải về máy
                  </button>
                </div>
              )}
            </article>
          ))}
        </div>
      );
    }

    // Announcements Tab
    if (!announcements.length) {
      return (
        <ClassroomEmptyState
          description="Lớp học chưa có thông báo chính thức nào."
          title="Chưa có thông báo"
        />
      );
    }

    return (
      <div className="space-y-4">
        {announcements.map((announcement) => (
          <article
            key={announcement.id}
            className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm space-y-3 hover:border-[#dfbfbd]/30 transition"
          >
            <div className="flex items-center justify-between gap-3">
              <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828] flex items-center gap-2">
                <Bell className="h-4.5 w-4.5 text-[#730014]" />
                {announcement.title}
              </h4>
              <span className="text-[10px] font-bold text-gray-400">
                {formatClassroomDateTime(announcement.createdAt)}
              </span>
            </div>
            <p className="text-sm leading-7 text-[#584140] whitespace-pre-wrap">
              {announcement.content || announcement.body}
            </p>
          </article>
        ))}
      </div>
    );
  };

  return (
    <LearnerPageShell
      actions={(
        <Link
          className="inline-flex items-center gap-1.5 rounded-2xl border border-[#dfbfbd] bg-white px-5 py-3 text-sm font-extrabold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95"
          to="/my-classrooms"
        >
          <ArrowLeft className="h-4 w-4" />
          Quay lại danh sách
        </Link>
      )}
      description={classroom?.shortDescription || 'Theo dõi lịch học, bài tập, điểm danh và bảng điểm của lớp.'}
      title={classroom?.title || 'Chi tiết lớp học'}
    >
      {loading ? <ClassroomLoadingState message="Đang tải dữ liệu lớp..." /> : null}
      {!loading && error ? <ClassroomErrorState message={error} onRetry={loadClassroom} /> : null}
      {!loading && !error && classroom ? (
        <div className="space-y-8">
          {/* ── Classroom Hero ── */}
          <div className="rounded-xl border border-[#ebebeb] bg-white px-5 py-5 md:px-6">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div className="min-w-0">
                <div className="flex flex-wrap gap-2 mb-3">
                  <ClassroomTypeBadge mode={classroom.deliveryMode} />
                  <StatusBadge status={classroom.classroomStatus} />
                </div>
                <h1 className="font-['Manrope'] text-2xl font-extrabold tracking-tight text-[#1a1c1c] md:text-3xl">
                  {classroom.title}
                </h1>
                <p className="mt-1 text-sm text-[#6a5553]">
                  Giảng viên: <strong className="text-[#1a1c1c]">{classroom.primaryTeacherName || 'Đang cập nhật'}</strong>
                  {classroom.deliveryMode === 'OFFLINE' && classroom.offlineAddress && (
                    <span> · <MapPin className="inline h-3.5 w-3.5 align-text-bottom" /> {classroom.offlineAddress}</span>
                  )}
                </p>
                {/* Progress bar */}
                {classroom.progressPercent != null && (
                  <div className="mt-3 max-w-xs">
                    <div className="flex justify-between text-[10px] font-semibold text-[#9a8b8a] mb-1">
                      <span>Tiến độ khóa học</span>
                      <span>{classroom.progressPercent}%</span>
                    </div>
                    <div className="h-1.5 w-full overflow-hidden rounded-full bg-gray-100">
                      <div className="h-full rounded-full bg-[#8a0018] transition-all" style={{ width: `${classroom.progressPercent}%` }} />
                    </div>
                  </div>
                )}
              </div>

              {classroom.deliveryMode === 'VIRTUAL' && classroom.larkMeetingUrl && (
                <div className="flex-shrink-0">
                  <LarkJoinButton onBlocked={setLarkMessage} url={classroom.larkMeetingUrl} />
                </div>
              )}
            </div>
          </div>

          <div className="space-y-6">
            {/* Tab Bar */}
            <ClassroomTabBar activeTab={activeTab} onChange={setActiveTab} tabs={detailTabs} />

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

            {/* Tab Content Panel */}
            <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 min-h-[300px]">
              {renderTabContent()}
            </section>
          </div>
        </div>
      ) : null}
    </LearnerPageShell>
  );
}

// ─── KPI mini-card ────────────────────────────────────────────────────────────
function KpiCard({ label, value, sub, color, icon }) {
  const colors = {
    rose: { border: 'border-rose-100', bg: 'bg-rose-50/50', icon: 'bg-rose-100 text-[#730014]', label: 'text-[#730014]' },
    emerald: { border: 'border-emerald-100', bg: 'bg-emerald-50/50', icon: 'bg-emerald-100 text-emerald-700', label: 'text-emerald-700' },
    amber: { border: 'border-amber-100', bg: 'bg-amber-50/50', icon: 'bg-amber-100 text-amber-700', label: 'text-amber-700' },
    blue: { border: 'border-blue-100', bg: 'bg-blue-50/50', icon: 'bg-blue-100 text-blue-700', label: 'text-blue-700' },
  };
  const c = colors[color] || colors.rose;
  return (
    <div className={`rounded-2xl border ${c.border} ${c.bg} p-4 space-y-2`}>
      <div className="flex items-center justify-between">
        <span className="text-[10px] font-extrabold uppercase tracking-wider text-[#8b706e]">{label}</span>
        <div className={`flex h-6 w-6 items-center justify-center rounded-lg ${c.icon}`}>{icon}</div>
      </div>
      <p className={`font-['Manrope'] text-xl font-extrabold ${c.label}`}>{value}</p>
      <p className="text-[10px] font-bold text-[#8b706e]">{sub}</p>
    </div>
  );
}
