import { useEffect, useState, useMemo } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  BookOpen,
  Calendar,
  Clock,
  MapPin,
  Video,
  Users,
  Plus,
  ArrowLeft,
  FileText,
  Bell,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  ChevronRight,
  User,
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
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatClassroomDate,
  formatClassroomDateTime,
  formatClassroomTime,
  formatAssessmentType,
  formatGradebookFinalResult,
  formatSessionStatus,
} from '../../utils/classroomHelpers';
import { PAGE_BODY_CLASS, PAGE_HEADER_CLASS, PAGE_MAIN_STACK_CLASS, PAGE_SECTION_CARD_CLASS, PAGE_SHELL_CLASS } from '../../utils/pageLayout';
import TeacherHomeworkSection from '../../components/teacher/TeacherHomeworkSection';
import TeacherMaterialsSection from '../../components/teacher/TeacherMaterialsSection';
import TeacherChangeRequestForm from '../../components/teacher/TeacherChangeRequestForm';
import TeacherGradebookSection from '../../components/teacher/TeacherGradebookSection';
import { downloadCsv, sanitizeCsvFilename } from '../../utils/csvExport';

const teacherTabs = [
  { id: 'sessions', label: 'Buổi học' },
  { id: 'curriculum', label: 'Giáo trình' },
  { id: 'students', label: 'Học viên' },
  { id: 'homework', label: 'Bài tập' },
  { id: 'gradebook', label: 'Bảng điểm' },
  { id: 'materials', label: 'Tài liệu' },
  { id: 'announcements', label: 'Thông báo' },
  { id: 'change-requests', label: 'Gửi yêu cầu' },
];

export default function TeacherClassroomPage() {
  const { id } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedInitialTab = searchParams.get('tab');
  const initialTab = requestedInitialTab === 'quizzes' ? 'homework' : requestedInitialTab;
  const [activeTab, setActiveTab] = useState(() => (
    teacherTabs.some((tab) => tab.id === initialTab) ? initialTab : 'sessions'
  ));
  const [classroom, setClassroom] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [homework, setHomework] = useState([]);
  const [gradebook, setGradebook] = useState([]);
  const [materials, setMaterials] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');

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

  useEffect(() => {
    const requestedTab = searchParams.get('tab');
    const tab = requestedTab === 'quizzes' ? 'homework' : requestedTab;
    if (tab && teacherTabs.some((item) => item.id === tab)) {
      setActiveTab(tab);
      if (requestedTab === 'quizzes') {
        const nextParams = new URLSearchParams(searchParams);
        nextParams.set('tab', 'homework');
        setSearchParams(nextParams, { replace: true });
      }
    }
  }, [searchParams]);

  const handleTabChange = (tabId) => {
    setActiveTab(tabId);
    if (tabId === 'sessions') {
      setSearchParams({}, { replace: true });
      return;
    }
    const nextParams = new URLSearchParams();
    nextParams.set('tab', tabId);
    setSearchParams(nextParams, { replace: true });
  };

  const openHomeworkTab = (action) => {
    setActiveTab('homework');
    const nextParams = new URLSearchParams();
    nextParams.set('tab', 'homework');
    if (action === 'create') {
      nextParams.set('action', 'create');
    }
    setSearchParams(nextParams, { replace: true });
  };

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

  const handleUnpublishGradebook = async () => {
    setActionMessage('');
    try {
      const data = await classroomApi.unpublishGradebook(id);
      setGradebook(data);
      setActionMessage('Đã thu hồi công bố bảng điểm.');
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể thu hồi công bố bảng điểm.'));
    }
  };

  const handleExportGradebook = () => {
    const rows = gradebook.map((entry) => [
      entry.studentName || `Học viên #${entry.studentId}`,
      entry.studentEmail || '',
      entry.attendancePercent ?? '',
      entry.homeworkScore ?? '',
      entry.quizScore ?? '',
      entry.participationScore ?? '',
      entry.finalResult ?? '',
      entry.status || '',
    ]);
    downloadCsv(
      `${sanitizeCsvFilename(`bang-diem-${classroom?.title || id}`)}.csv`,
      ['Tên học viên', 'Email', 'Chuyên cần (%)', 'Điểm bài tập', 'Điểm quiz', 'Điểm phát biểu', 'Điểm/Kết quả cuối', 'Trạng thái'],
      rows
    );
  };

  // Build a beautiful student roster using gradebook entries
  const studentRoster = useMemo(() => {
    if (!gradebook.length) return [];
    return gradebook.map((entry) => ({
      id: entry.studentId || entry.id,
      name: entry.studentName || `Học viên #${entry.studentId}`,
      email: entry.studentEmail || 'Chưa cập nhật',
      attendance: entry.attendancePercent != null ? `${entry.attendancePercent}%` : '—',
      assignmentScore: entry.homeworkScore ?? '—',
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
    pendingGrading: homework.reduce((sum, item) => sum + (item.pendingGradingCount || 0), 0),
    completed: pastSessions.length,
    upcoming: upcomingSessions.length,
  }), [gradebook, homework, pastSessions, upcomingSessions]);

  const renderChangeRequestForm = () => (
    <TeacherChangeRequestForm
      classroom={classroom}
      classroomId={id}
      onMessage={setActionMessage}
      sessions={sessions}
    />
  );

  const renderContent = () => {
    if (activeTab === 'curriculum') {
      return <TeacherCurriculumPanel curriculum={classroom?.curriculumProgram} />;
    }

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
                      <p className="text-[10px] font-bold text-[#8b706e]">Bài tập</p>
                      <p className="font-['Manrope'] font-extrabold text-[#2b2828]">{student.assignmentScore}</p>
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
      return (
        <TeacherHomeworkSection
          classroomId={id}
          curriculumUnits={classroom?.curriculumProgram?.units || []}
          homework={homework}
          initialOpenCreate={searchParams.get('action') === 'create'}
          onGradebookChange={setGradebook}
          onHomeworkChange={setHomework}
          onCreateFormOpened={() => {
            const nextParams = new URLSearchParams(searchParams);
            nextParams.delete('action');
            setSearchParams(nextParams, { replace: true });
          }}
          onMessage={setActionMessage}
          sessions={sessions}
        />
      );
    }

    if (activeTab === 'gradebook') {
      return (
        <div className="space-y-6">
          <div className="flex justify-end">
            <button className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-5 py-3 text-xs font-extrabold text-[#4b0009] transition hover:bg-slate-50 disabled:opacity-50" disabled={!gradebook.length} onClick={handleExportGradebook} type="button">
              <Download className="h-4 w-4" />
              Xuất CSV bảng điểm
            </button>
          </div>
          <TeacherGradebookSection
            classroomId={id}
            gradebook={gradebook}
            onGradebookChange={setGradebook}
            onMessage={setActionMessage}
            onPublish={handlePublishGradebook}
            onUnpublish={handleUnpublishGradebook}
          />
        </div>
      );
    }

    if (activeTab === 'materials') {
      return (
        <TeacherMaterialsSection
          classroomId={id}
          materials={materials}
          onMaterialsChange={setMaterials}
          onMessage={setActionMessage}
          sessions={sessions}
        />
      );
    }

    if (activeTab === 'change-requests') {
      return renderChangeRequestForm();
    }

    if (activeTab === 'announcements') {
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
    }

    return null;
  };

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
        {/* Class Header */}
        <section className={PAGE_SECTION_CARD_CLASS}>
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
              <div className="flex flex-col gap-3 md:grid md:grid-cols-[minmax(0,1fr)_auto] md:items-center md:gap-4">
                <h1 className="font-['Manrope'] text-2xl font-extrabold tracking-tight text-[#1a1c1c] md:text-3xl">
                  {classroom?.title || 'Đang tải thông tin lớp...'}
                </h1>
                {classroom && (
                  <div className="flex flex-wrap gap-2">
                    <button
                      className="inline-flex flex-shrink-0 items-center gap-1.5 rounded-lg bg-[#4b0009] px-4 py-2 text-sm font-semibold text-white transition hover:bg-[#730014] active:scale-95"
                      onClick={() => openHomeworkTab('create')}
                      type="button"
                    >
                      <Plus className="h-4 w-4" /> Giao bài tập
                    </button>
                    <Link
                      className="inline-flex flex-shrink-0 items-center gap-1.5 rounded-lg border border-[#e5e7eb] bg-white px-4 py-2 text-sm font-semibold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95"
                      to="/teacher/schedule"
                    >
                      <Calendar className="h-4 w-4" /> Lịch dạy
                    </Link>
                  </div>
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
                <button
                  className="text-left transition hover:opacity-90"
                  onClick={() => openHomeworkTab()}
                  type="button"
                >
                  <HeaderStat
                    icon={FileText}
                    label="Bài chờ chấm"
                    value={teacherStats.pendingGrading}
                    tone={teacherStats.pendingGrading > 0 ? 'blue' : 'gray'}
                  />
                </button>
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
              <ClassroomTabBar activeTab={activeTab} onChange={handleTabChange} tabs={teacherTabs} />

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
          </>
        ) : null}
      </motion.main>
      </div>
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

function TeacherCurriculumPanel({ curriculum }) {
  if (!curriculum) {
    return (
      <ClassroomEmptyState
        description="Lớp này chưa được gắn giáo trình. Training Manager cần chọn giáo trình khi mở hoặc cập nhật lớp."
        title="Chưa có giáo trình"
      />
    );
  }
  const units = curriculum.units || [];
  return (
    <div className="space-y-5">
      <div className="rounded-2xl border border-[#dfbfbd]/20 bg-[#fffafb] p-5">
        <p className="text-xs font-extrabold uppercase tracking-wider text-[#8b706e]">Giáo trình gốc</p>
        <h3 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{curriculum.title}</h3>
        <p className="mt-1 text-sm text-[#584140]">
          {[curriculum.code, curriculum.examCategory, curriculum.targetBand ? `Band ${curriculum.targetBand}` : null, curriculum.targetScore ? `Target ${curriculum.targetScore}` : null].filter(Boolean).join(' · ')}
        </p>
        {curriculum.teacherGuide ? (
          <div className="mt-4 rounded-xl bg-white p-4">
            <p className="text-xs font-extrabold uppercase tracking-wider text-[#8b706e]">Hướng dẫn giảng viên</p>
            <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-[#584140]">{curriculum.teacherGuide}</p>
          </div>
        ) : null}
      </div>
      {units.length ? (
        <div className="space-y-3">
          {units.map((unit) => (
            <CurriculumUnitCard key={unit.id} unit={unit} />
          ))}
        </div>
      ) : (
        <ClassroomEmptyState
          description="Giáo trình này chưa có unit hoặc buổi học."
          title="Chưa có nội dung giáo trình"
        />
      )}
    </div>
  );
}

function CurriculumUnitCard({ unit }) {
  return (
    <article className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
      <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">
        {unit.displayOrder ?? 0}. {unit.title}
      </h4>
      {unit.description ? <p className="mt-1 text-sm text-[#584140]">{unit.description}</p> : null}
      {unit.sessionPlan ? <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-[#584140]">{unit.sessionPlan}</p> : null}
      <div className="mt-4 grid gap-3 md:grid-cols-2">
        <CurriculumRefList title="Học liệu" refs={unit.materials} />
        <CurriculumRefList title="Luyện tập trong giáo trình" refs={unit.exercises} />
        <CurriculumRefList title="Bài đánh giá theo Unit" refs={unit.assessments} />
        <CurriculumRefList title="Flashcard" refs={unit.flashcards} />
      </div>
    </article>
  );
}

function CurriculumRefList({ title, refs = [] }) {
  return (
    <div className="rounded-xl border border-gray-100 bg-[#fffafb] p-3">
      <p className="text-[10px] font-extrabold uppercase tracking-wider text-[#8b706e]">{title}</p>
      {refs.length ? (
        <div className="mt-2 space-y-1.5">
          {refs.map((ref) => (
            <div key={`${ref.type}-${ref.id}`} className="rounded-lg bg-white px-3 py-2 text-xs">
              <p className="font-extrabold text-[#2b2828]">{ref.title}</p>
              <p className="mt-0.5 text-[#8b706e]">{[
                ref.skill,
                ref.type === 'ASSESSMENT' ? formatAssessmentType(ref.subtitle) : ref.subtitle,
                ref.status,
              ].filter(Boolean).join(' · ')}</p>
              {ref.fileUrl ? (
                <a className="mt-2 inline-flex font-extrabold text-[#730014] hover:underline" href={ref.fileUrl} rel="noreferrer" target="_blank">
                  Mở học liệu
                </a>
              ) : null}
            </div>
          ))}
        </div>
      ) : (
        <p className="mt-2 text-xs text-[#8b706e]">Chưa gắn.</p>
      )}
    </div>
  );
}
