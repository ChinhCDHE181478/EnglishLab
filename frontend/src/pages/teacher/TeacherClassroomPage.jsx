import { useEffect, useState, useMemo } from 'react';
import { Link, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  BookOpen,
  Calendar,
  Clock,
  MapPin,
  Video,
  Users,
  Award,
  Plus,
  ArrowLeft,
  FileText,
  Bell,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  ChevronRight,
  Send,
  User,
  Settings,
  Download,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import Header from '../../components/ai-learning/Header';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  ClassroomTabBar,
  StatusBadge,
  ClassroomTypeBadge,
} from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatClassroomDate,
  formatClassroomDateTime,
  formatClassroomTime,
  formatGradebookFinalResult,
  formatSessionStatus,
  isGradebookPassed,
} from '../../utils/classroomHelpers';

const teacherTabs = [
  { id: 'sessions', label: 'Buổi học' },
  { id: 'students', label: 'Học viên' },
  { id: 'homework', label: 'Bài tập' },
  { id: 'gradebook', label: 'Bảng điểm' },
  { id: 'materials', label: 'Tài liệu' },
  { id: 'announcements', label: 'Thông báo' },
];

const requestTypeOptions = [
  { label: 'Đổi lịch buổi học', value: 'RESCHEDULE_SESSION' },
  { label: 'Đổi phòng học', value: 'CHANGE_ROOM' },
  { label: 'Đổi giáo viên', value: 'CHANGE_TEACHER' },
  { label: 'Hủy buổi học', value: 'CANCEL_SESSION' },
];

export default function TeacherClassroomPage() {
  const { id } = useParams();
  const [activeTab, setActiveTab] = useState('sessions');
  const [classroom, setClassroom] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [homework, setHomework] = useState([]);
  const [gradebook, setGradebook] = useState([]);
  const [materials, setMaterials] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [requestForm, setRequestForm] = useState({ type: 'RESCHEDULE_SESSION', reason: '', sessionId: '' });

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [classroomData, sessionsData, homeworkData, gradebookData, materialsData, announcementsData] = await Promise.all([
        classroomApi.getTeacherClassroom(id),
        classroomApi.getTeacherClassroomSessions(id),
        classroomApi.getTeacherHomework(id),
        classroomApi.getTeacherGradebook(id),
        classroomApi.getTeacherMaterials(id),
        classroomApi.getTeacherAnnouncements(id),
      ]);
      setClassroom(classroomData);
      setSessions(sessionsData);
      setHomework(homeworkData);
      setGradebook(gradebookData);
      setMaterials(materialsData);
      setAnnouncements(announcementsData);
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể tải dữ liệu lớp.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [id]);

  const handlePublishGradebook = async () => {
    setActionMessage('');
    try {
      const data = await classroomApi.publishGradebook(id);
      setGradebook(data);
      setActionMessage('Đã công bố bảng điểm thành công.');
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể công bố bảng điểm.'));
    }
  };

  const handleCreateRequest = async () => {
    setActionMessage('');
    try {
      await classroomApi.createChangeRequest({
        classroomOfferingId: Number(id),
        requestType: requestForm.type,
        reason: requestForm.reason,
        targetSessionId: requestForm.sessionId ? Number(requestForm.sessionId) : null,
      });
      setActionMessage('Đã gửi yêu cầu thay đổi thành công.');
      setRequestForm({ type: 'RESCHEDULE_SESSION', reason: '', sessionId: '' });
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể gửi yêu cầu.'));
    }
  };

  // Build a beautiful student roster using gradebook entries
  const studentRoster = useMemo(() => {
    if (!gradebook.length) return [];
    return gradebook.map((entry) => ({
      id: entry.studentId || entry.id,
      name: entry.studentName || `Học viên #${entry.studentId}`,
      email: entry.studentEmail || 'Chưa cập nhật',
      attendance: entry.attendancePercent != null ? `${entry.attendancePercent}%` : '—',
      hwScore: entry.homeworkScore ?? '—',
      quizScore: entry.quizScore ?? '—',
      participation: entry.participationScore ?? '—',
      result: formatGradebookFinalResult(entry.finalResult),
      isAtRisk: entry.attendancePercent != null && entry.attendancePercent < 80,
    }));
  }, [gradebook]);

  // Group sessions into upcoming vs past
  const { upcomingSessions, pastSessions } = useMemo(() => {
    const now = new Date();
    const upcoming = sessions.filter((s) => s.status === 'SCHEDULED' || s.status === 'OPEN');
    const past = sessions.filter((s) => s.status === 'COMPLETED' || s.status === 'CANCELLED');
    return { upcomingSessions: upcoming, pastSessions: past };
  }, [sessions]);

  // Teacher-level stats
  const teacherStats = useMemo(() => ({
    enrolled: gradebook.length,
    atRisk: gradebook.filter((e) => e.attendancePercent != null && e.attendancePercent < 80).length,
    pendingGrading: homework.filter((h) => h.submissionCount > 0 && !h.gradedCount).length,
    completed: pastSessions.length,
    upcoming: upcomingSessions.length,
  }), [gradebook, homework, pastSessions, upcomingSessions]);

  const renderContent = () => {
    if (activeTab === 'sessions') {
      if (!sessions.length) {
        return (
          <ClassroomEmptyState
            description="Chưa có buổi học nào được lên lịch cho lớp học này."
            title="Chưa có buổi học"
          />
        );
      }

      const SessionRow = ({ session }) => {
        const isPast = session.status === 'COMPLETED' || session.status === 'CANCELLED';
        const isLive = session.status === 'OPEN';
        const isVirtual = classroom?.deliveryMode === 'VIRTUAL';
        return (
          <article
            className={`flex flex-col gap-4 rounded-2xl border p-5 transition md:flex-row md:items-center md:justify-between ${
              isLive
                ? 'border-emerald-100 bg-emerald-50/10'
                : isPast
                  ? 'border-gray-100 bg-gray-50/30 opacity-75'
                  : 'border-[#dfbfbd]/15 bg-white hover:border-[#dfbfbd]/40'
            }`}
          >
            <div className="flex items-start gap-4">
              {/* Session number indicator */}
              <div className={`flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-2xl font-['Manrope'] text-sm font-extrabold ${
                isLive ? 'bg-emerald-100 text-emerald-800' : isPast ? 'bg-gray-100 text-gray-500' : 'bg-rose-50 text-[#730014]'
              }`}>
                {sessions.indexOf(session) + 1}
              </div>
              <div className="space-y-1.5 min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  {isLive && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-extrabold text-emerald-800">
                      <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-600" /> Đang diễn ra
                    </span>
                  )}
                  <StatusBadge status={session.status} />
                </div>
                <h3 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">
                  {session.sessionContent || `Buổi học ngày ${formatClassroomDate(session.sessionDate)}`}
                </h3>
                <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-[#584140]">
                  <span className="flex items-center gap-1">
                    <Calendar className="h-3.5 w-3.5 text-[#730014]" />
                    {formatClassroomDate(session.sessionDate)}
                  </span>
                  <span className="flex items-center gap-1">
                    <Clock className="h-3.5 w-3.5 text-[#730014]" />
                    {formatClassroomTime(session.startTime)} – {formatClassroomTime(session.endTime)}
                  </span>
                  {isVirtual ? (
                    <span className="flex items-center gap-1 text-purple-700 font-bold">
                      <Video className="h-3.5 w-3.5" /> Trực tuyến
                    </span>
                  ) : (
                    <span className="flex items-center gap-1">
                      <MapPin className="h-3.5 w-3.5" />
                      {session.roomName || 'Chưa xếp phòng'}
                    </span>
                  )}
                </div>
              </div>
            </div>
            <Link
              className={`inline-flex items-center gap-1.5 rounded-xl px-5 py-2.5 text-xs font-extrabold transition active:scale-95 flex-shrink-0 ${
                isPast
                  ? 'border border-gray-200 bg-white text-gray-600 hover:bg-gray-50'
                  : 'bg-[#4b0009] text-white shadow-sm hover:bg-[#730014] hover:shadow'
              }`}
              to={`/teacher/sessions/${session.id}`}
            >
              {isPast ? 'Xem điểm danh' : 'Vào điểm danh'}
              <ChevronRight className="h-3.5 w-3.5" />
            </Link>
          </article>
        );
      };

      return (
        <div className="space-y-6">
          {/* Upcoming sessions */}
          {upcomingSessions.length > 0 && (
            <div className="space-y-3">
              <h4 className="text-xs font-extrabold uppercase tracking-wider text-[#8b706e] flex items-center gap-2">
                <span className="h-2 w-2 rounded-full bg-amber-400" />
                Buổi học sắp tới ({upcomingSessions.length})
              </h4>
              {upcomingSessions.map((s) => <SessionRow key={s.id} session={s} />)}
            </div>
          )}

          {/* Past sessions */}
          {pastSessions.length > 0 && (
            <div className="space-y-3">
              <h4 className="text-xs font-extrabold uppercase tracking-wider text-[#8b706e] flex items-center gap-2">
                <span className="h-2 w-2 rounded-full bg-gray-300" />
                Buổi học đã qua ({pastSessions.length})
              </h4>
              {pastSessions.map((s) => <SessionRow key={s.id} session={s} />)}
            </div>
          )}
        </div>
      );
    }

    if (activeTab === 'students') {
      if (!studentRoster.length) {
        return (
          <ClassroomEmptyState
            description="Chưa có học viên nào được xếp vào lớp này."
            title="Danh sách học viên trống"
          />
        );
      }

      const atRiskCount = studentRoster.filter((s) => s.isAtRisk).length;

      return (
        <div className="space-y-4">
          {/* At-risk banner */}
          {atRiskCount > 0 && (
            <div className="flex items-start gap-3 rounded-2xl border border-rose-100 bg-rose-50/30 p-4">
              <AlertCircle className="h-5 w-5 flex-shrink-0 text-rose-600 mt-0.5" />
              <div>
                <p className="text-sm font-extrabold text-rose-800">
                  {atRiskCount} học viên có chuyên cần dưới 80%
                </p>
                <p className="text-xs text-rose-700 mt-0.5">
                  Cân nhắc liên hệ hoặc gửi nhắc nhở để giúp học viên cải thiện tỷ lệ tham dự.
                </p>
              </div>
            </div>
          )}

          {/* Student cards */}
          <div className="space-y-2">
            {studentRoster.map((student) => {
              const attendancePct = parseInt(student.attendance) || 0;
              const attColor = attendancePct >= 80 ? 'bg-emerald-500' : attendancePct >= 60 ? 'bg-amber-400' : 'bg-rose-500';
              const attTextColor = attendancePct >= 80 ? 'text-emerald-700' : attendancePct >= 60 ? 'text-amber-700' : 'text-rose-700';

              return (
                <div
                  key={student.id}
                  className={`flex flex-col gap-4 rounded-2xl border p-4 transition sm:flex-row sm:items-center ${
                    student.isAtRisk ? 'border-rose-100 bg-rose-50/10' : 'border-gray-100 bg-white hover:bg-[#fffafb]/30'
                  }`}
                >
                  {/* Avatar + Name */}
                  <div className="flex items-center gap-3 flex-shrink-0 sm:w-56">
                    <div className={`flex h-10 w-10 items-center justify-center rounded-full font-['Manrope'] font-extrabold text-sm ${
                      student.isAtRisk ? 'bg-rose-100 text-rose-700' : 'bg-rose-50 text-[#730014]'
                    }`}>
                      {student.name.charAt(0).toUpperCase()}
                    </div>
                    <div className="min-w-0">
                      <p className="font-extrabold text-[#2b2828] truncate">{student.name}</p>
                      <p className="text-[10px] text-[#8b706e] truncate">{student.email}</p>
                    </div>
                  </div>

                  {/* Attendance progress */}
                  <div className="flex-1 min-w-0 space-y-1.5">
                    <div className="flex items-center justify-between text-[10px] font-bold">
                      <span className="text-[#8b706e]">Chuyên cần</span>
                      <span className={attTextColor}>{student.attendance}</span>
                    </div>
                    <div className="h-1.5 w-full overflow-hidden rounded-full bg-gray-100">
                      <div className={`h-full rounded-full transition-all ${attColor}`} style={{ width: student.attendance !== '—' ? student.attendance : '0%' }} />
                    </div>
                  </div>

                  {/* Scores */}
                  <div className="flex items-center gap-6 flex-shrink-0 text-sm">
                    <div className="text-center">
                      <p className="text-[10px] font-bold text-[#8b706e]">Homework</p>
                      <p className="font-['Manrope'] font-extrabold text-[#2b2828]">{student.hwScore}</p>
                    </div>
                    <div className="text-center">
                      <p className="text-[10px] font-bold text-[#8b706e]">Quiz</p>
                      <p className="font-['Manrope'] font-extrabold text-[#2b2828]">{student.quizScore}</p>
                    </div>
                    <div className="text-center">
                      <p className="text-[10px] font-bold text-[#8b706e]">Kết quả</p>
                      <span className={`inline-flex rounded-full px-2.5 py-0.5 text-[10px] font-extrabold ${
                        ['PASSED', 'ĐẠT'].includes(student.result) ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-600'
                      }`}>
                        {student.result === 'CHƯA CÓ' ? '—' : student.result}
                      </span>
                    </div>
                  </div>

                  {student.isAtRisk && (
                    <div className="flex flex-shrink-0 items-center gap-1 rounded-xl border border-rose-100 bg-rose-50 px-2.5 py-1 text-[10px] font-extrabold text-rose-700">
                      <AlertCircle className="h-3 w-3" /> Cần chú ý
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      );
    }

    if (activeTab === 'homework') {
      if (!homework.length) {
        return (
          <ClassroomEmptyState
            description="Chưa có bài tập nào được giao cho lớp học này."
            title="Chưa có bài tập"
          />
        );
      }
      return (
        <div className="grid gap-6 md:grid-cols-2">
          {homework.map((item) => (
            <article
              key={item.id}
              className="rounded-xl border border-[#e5e7eb] bg-white p-5 flex flex-col justify-between hover:border-[#d0c4c3] transition"
            >
              <div>
                <div className="flex items-center justify-between gap-3">
                  <span className="text-xs font-extrabold uppercase tracking-wider text-[#730014]">Bài tập viết</span>
                  <StatusBadge status="ACTIVE" />
                </div>
                <h3 className="mt-4 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{item.title}</h3>
                <p className="mt-2 text-sm text-[#584140] line-clamp-3">{item.instruction || 'Không có hướng dẫn chi tiết.'}</p>
              </div>

              <div className="mt-6 pt-4 border-t border-gray-50 flex items-center justify-between text-xs text-[#8b706e]">
                <span className="flex items-center gap-1"><Clock className="h-4 w-4 text-[#730014]" /> Hạn nộp: {formatClassroomDateTime(item.deadline)}</span>
                <span className="font-bold text-gray-400">ID: #{item.id}</span>
              </div>
            </article>
          ))}
        </div>
      );
    }

    if (activeTab === 'gradebook') {
      return (
        <div className="space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-4 bg-[#fffafb] border border-[#dfbfbd]/15 p-5 rounded-2xl">
            <div>
              <h4 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Công bố bảng điểm chính thức</h4>
              <p className="text-xs text-[#584140] mt-1">Sau khi chấm điểm đầy đủ, hãy công bố bảng điểm để học viên có thể xem kết quả đánh giá.</p>
            </div>
            <button
              className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014] active:scale-95"
              onClick={handlePublishGradebook}
              type="button"
            >
              <Award className="h-4 w-4" />
              Công bố bảng điểm
            </button>
          </div>

          {!gradebook.length ? (
            <ClassroomEmptyState
              description="Chưa có dữ liệu bảng điểm nào được ghi nhận."
              title="Chưa có bảng điểm"
            />
          ) : (
            <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white">
              <table className="min-w-full divide-y divide-gray-100 text-left text-sm">
                <thead className="bg-[#fffafb] text-xs font-bold text-[#8b706e] uppercase tracking-wider">
                  <tr>
                    <th className="px-6 py-4">Học viên</th>
                    <th className="px-6 py-4">Homework</th>
                    <th className="px-6 py-4">Quiz</th>
                    <th className="px-6 py-4">Chuyên cần</th>
                    <th className="px-6 py-4">Phát biểu</th>
                    <th className="px-6 py-4">Kết quả</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 text-[#584140]">
                  {gradebook.map((entry) => (
                    <tr key={entry.studentId || entry.id} className="hover:bg-[#fffafb]/30">
                      <td className="px-6 py-4 font-extrabold text-[#2b2828]">
                        {entry.studentName || `Học viên #${entry.studentId}`}
                      </td>
                      <td className="px-6 py-4 font-bold">{entry.homeworkScore ?? '—'}</td>
                      <td className="px-6 py-4 font-bold">{entry.quizScore ?? '—'}</td>
                      <td className="px-6 py-4 font-bold">{entry.attendancePercent != null ? `${entry.attendancePercent}%` : '—'}</td>
                      <td className="px-6 py-4 font-bold">{entry.participationScore ?? '—'}</td>
                      <td className="px-6 py-4">
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold ${
                          isGradebookPassed(entry.finalResult)
                            ? 'bg-emerald-50 text-emerald-700'
                            : 'bg-amber-50 text-amber-700'
                        }`}>
                          {formatGradebookFinalResult(entry.finalResult)}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      );
    }

    if (activeTab === 'materials') {
      if (!materials.length) {
        return (
          <ClassroomEmptyState
            description="Chưa có tài liệu nào được tải lên cho lớp học này."
            title="Chưa có tài liệu"
          />
        );
      }
      return (
        <div className="grid gap-4 sm:grid-cols-2">
          {materials.map((material) => (
            <article
              key={material.id}
              className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm flex flex-col justify-between hover:border-[#dfbfbd]/30 transition"
            >
              <div>
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-rose-50 text-[#730014] mb-4">
                  <FileText className="h-5 w-5" />
                </div>
                <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">
                  {material.title}
                </h4>
                {material.description ? (
                  <p className="mt-2 text-xs text-[#584140] line-clamp-2">{material.description}</p>
                ) : null}
              </div>

              {material.fileUrl && (
                <div className="mt-4 pt-4 border-t border-gray-50">
                  <a
                    className="inline-flex items-center gap-1.5 text-xs font-extrabold text-[#730014] hover:underline"
                    href={material.fileUrl}
                    rel="noreferrer"
                    target="_blank"
                  >
                    <Download className="h-4 w-4" />
                    Tải tài liệu
                  </a>
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
          description="Chưa có thông báo chính thức nào được gửi tới lớp học này."
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
    <div className="course-page flex min-h-[100dvh] flex-col bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />
      <motion.main
        className="mx-auto flex w-full max-w-[1320px] flex-1 flex-col px-4 pb-[80px] pt-8 md:px-10 space-y-8"
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.32, ease: 'easeOut' }}
      >
        {/* Class Header */}
        <section className="border-b border-[#ebebeb] bg-white pb-6 pt-2">
          <div className="flex flex-col gap-4">
            <div>
              <div className="flex flex-wrap items-center gap-2 mb-2">
                <Link className="inline-flex items-center gap-1 text-xs font-semibold text-[#8b706e] hover:text-[#8a0018] transition" to="/teacher">
                  <ArrowLeft className="h-3.5 w-3.5" /> Giảng dạy
                </Link>
                <span className="text-[#d0c4c3]">›</span>
                {classroom && <ClassroomTypeBadge mode={classroom.deliveryMode} />}
                {classroom && <StatusBadge status={classroom.classroomStatus} />}
              </div>
              <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <h1 className="font-['Manrope'] text-2xl font-extrabold tracking-tight text-[#1a1c1c] md:text-3xl">
                  {classroom?.title || 'Đang tải thông tin lớp...'}
                </h1>
                {classroom && (
                  <Link
                    className="inline-flex flex-shrink-0 items-center gap-1.5 rounded-lg border border-[#e5e7eb] bg-white px-4 py-2 text-sm font-semibold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95"
                    to="/teacher/schedule"
                  >
                    <Calendar className="h-4 w-4" /> Lịch dạy
                  </Link>
                )}
              </div>
            </div>

            {/* Operational stats row */}
            {classroom && (
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
                <HeaderStat
                  icon={Users}
                  label="Sĩ số"
                  value={`${classroom.enrolledCount ?? 0}/${classroom.maxCapacity ?? '—'}`}
                  tone="rose"
                />
                <HeaderStat
                  icon={AlertCircle}
                  label="Cần chú ý"
                  value={teacherStats.atRisk}
                  tone={teacherStats.atRisk > 0 ? 'amber' : 'gray'}
                />
                <HeaderStat
                  icon={FileText}
                  label="Bài chờ chấm"
                  value={teacherStats.pendingGrading}
                  tone={teacherStats.pendingGrading > 0 ? 'blue' : 'gray'}
                />
                <HeaderStat
                  icon={CheckCircle2}
                  label="Buổi đã học"
                  value={teacherStats.completed}
                  tone="emerald"
                />
                <HeaderStat
                  icon={Clock}
                  label="Buổi sắp tới"
                  value={teacherStats.upcoming}
                  tone="purple"
                />
              </div>
            )}
          </div>
        </section>

        {loading ? <ClassroomLoadingState message="Đang tải dữ liệu quản lý lớp..." /> : null}
        {!loading && error ? <ClassroomErrorState message={error} onRetry={loadData} /> : null}
        {!loading && !error && classroom ? (
          <>
            {/* Tab Bar */}
            <div className="space-y-6">
              <ClassroomTabBar activeTab={activeTab} onChange={setActiveTab} tabs={teacherTabs} />

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
              <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm min-h-[300px]">
                {renderContent()}
              </section>
            </div>

            {/* Change Request Section */}
            <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-6">
              <div className="flex items-start gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-[#730014] flex-shrink-0">
                  <Settings className="h-6 w-6" />
                </div>
                <div>
                  <h2 className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Gửi yêu cầu thay đổi</h2>
                  <p className="mt-1 text-xs text-[#8b706e] leading-5">Đề xuất thay đổi lịch, phòng học, giáo viên thay thế hoặc hủy buổi học. Đề xuất sẽ được gửi tới Training Manager phê duyệt.</p>
                </div>
              </div>

              <div className="grid gap-4 md:grid-cols-2 pt-2">
                <div className="space-y-2">
                  <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Loại đề xuất thay đổi</label>
                  <BrandedSelect
                    onChange={(event) => setRequestForm((current) => ({ ...current, type: event.target.value }))}
                    options={requestTypeOptions}
                    value={requestForm.type}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Buổi học áp dụng (nếu có)</label>
                  <BrandedSelect
                    onChange={(event) => setRequestForm((current) => ({ ...current, sessionId: event.target.value }))}
                    options={[
                      { label: 'Không chọn (Áp dụng toàn khóa)', value: '' },
                      ...sessions.map((session) => ({
                        label: `Buổi #${session.id}: ${formatClassroomDate(session.sessionDate)} (${formatClassroomTime(session.startTime)})`,
                        value: String(session.id),
                      })),
                    ]}
                    placeholder="Chọn buổi học áp dụng"
                    value={requestForm.sessionId}
                  />
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Lý do chi tiết & Đề xuất cụ thể</label>
                <textarea
                  className="min-h-[120px] w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white focus:ring-2 focus:ring-[#730014]/5"
                  onChange={(event) => setRequestForm((current) => ({ ...current, reason: event.target.value }))}
                  placeholder="Vui lòng nhập lý do thay đổi chi tiết và đề xuất ngày/giờ/phòng mới để Training Manager tiện duyệt..."
                  value={requestForm.reason}
                />
              </div>

              <div className="pt-2 flex justify-end">
                <button
                  className="inline-flex items-center gap-1.5 rounded-2xl bg-[#4b0009] px-6 py-3.5 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow active:scale-95"
                  onClick={handleCreateRequest}
                  type="button"
                >
                  <Send className="h-4 w-4" />
                  Gửi yêu cầu phê duyệt
                </button>
              </div>
            </section>
          </>
        ) : null}
      </motion.main>
      <CourseFooter />
    </div>
  );
}

function HeaderStat({ icon: Icon, label, value, tone = 'rose' }) {
  const toneMap = {
    rose: 'bg-rose-50 text-[#8a0018]',
    amber: 'bg-amber-50 text-amber-700',
    blue: 'bg-blue-50 text-blue-700',
    emerald: 'bg-emerald-50 text-emerald-700',
    purple: 'bg-purple-50 text-purple-700',
    gray: 'bg-gray-100 text-gray-500',
  };
  return (
    <div className="flex items-center gap-3 rounded-xl border border-[#e5e7eb] bg-white px-4 py-3">
      <span className={`flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg ${toneMap[tone] || toneMap.rose}`}>
        <Icon className="h-4.5 w-4.5" />
      </span>
      <div className="min-w-0">
        <p className="text-[10px] font-bold uppercase tracking-wide text-[#9a8b8a] truncate">{label}</p>
        <p className="font-['Manrope'] text-lg font-extrabold leading-tight text-[#1a1c1c]">{value}</p>
      </div>
    </div>
  );
}
