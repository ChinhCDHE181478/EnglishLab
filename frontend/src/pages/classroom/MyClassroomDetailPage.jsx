import { useEffect, useState, useMemo } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
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
  Building,
  Upload,
  RefreshCw,
  Send,
  Plus,
  Sparkles
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import VirtualJoinButton from '../../components/classroom/VirtualJoinButton';
import TuitionPaymentSection from '../../components/classroom/TuitionPaymentSection';
import ClassroomFlashcardsPanel from '../../components/classroom/ClassroomFlashcardsPanel';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  ProgressRing,
} from '../../components/classroom/ClassroomUi';
import LearnerPageShell from '../../components/learner/LearnerPageShell';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatAttendanceStatus,
  formatClassroomDate,
  formatClassroomDateTime,
  formatClassroomPrice,
  formatClassroomTime,
  formatGradebookFinalResult,
  isGradebookPassed,
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
  { id: 'curriculum', label: 'Giáo trình' },
  { id: 'flashcards', label: 'Flashcard' },
  { id: 'schedule', label: 'Lịch học' },
  { id: 'payment', label: 'Học phí' },
  { id: 'homework', label: 'Bài tập' },
  { id: 'attendance', label: 'Điểm danh' },
  { id: 'gradebook', label: 'Bảng điểm' },
  { id: 'materials', label: 'Tài liệu' },
  { id: 'announcements', label: 'Thông báo' },
];

const usesModuleExamWorkspace = (homework) => {
  if (!homework || homework.activityType === 'FILE_RESPONSE' || homework.activityType === 'FLASHCARD_REVIEW') {
    return false;
  }
  try {
    const config = JSON.parse(homework.activityConfigJson || '{}');
    const questions = [
      ...(config.questions || []),
      ...(config.items || []),
      ...(config.parts || []).flatMap((part) => (
        part.questions || (part.questionGroups || []).flatMap((group) => group.questions || [])
      )),
    ];
    if (questions.length) return true;
  } catch {
    // Writing and Speaking can still use the module workspace without objective JSON.
  }
  return ['WRITING', 'SPEAKING'].includes(String(homework.skill || '').toUpperCase());
};

const usesInteractiveHomeworkWorkspace = (homework) => (
  homework?.activityType === 'FLASHCARD_REVIEW' || usesModuleExamWorkspace(homework)
);

export default function MyClassroomDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('overview');
  const [classroom, setClassroom] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [homework, setHomework] = useState([]);
  const [attendance, setAttendance] = useState([]);
  const [attendanceDisputes, setAttendanceDisputes] = useState([]);
  const [gradebook, setGradebook] = useState(null);
  const [materials, setMaterials] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [syllabus, setSyllabus] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [submittingId, setSubmittingId] = useState(null);
  const [submitAnswers, setSubmitAnswers] = useState({});
  const [submitFiles, setSubmitFiles] = useState({});
  const [actionMessage, setActionMessage] = useState('');
  const [larkMessage, setLarkMessage] = useState('');
  const [disputeForm, setDisputeForm] = useState({ attendanceId: null, reason: '' });
  const [submittingDispute, setSubmittingDispute] = useState(false);

  const loadClassroom = async () => {
    setLoading(true);
    setError('');
    try {
      const loadAttendanceDisputes = async () => {
        try {
          return await classroomApi.listMyAttendanceDisputes();
        } catch {
          return [];
        }
      };

      const [classroomData, sessionsData, homeworkData, attendanceData, materialsData, announcementsData, syllabusData, disputeData] = await Promise.all([
        classroomApi.getMyClassroom(id),
        classroomApi.getMyClassroomSessions(id),
        classroomApi.getMyClassroomHomework(id),
        classroomApi.getMyAttendance(id),
        classroomApi.getMyClassroomMaterials(id),
        classroomApi.getMyClassroomAnnouncements(id),
        classroomApi.getMyClassroomSyllabus(id),
        loadAttendanceDisputes(),
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
      setAttendanceDisputes(disputeData);
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
    const file = submitFiles[homeworkId] || null;
    setSubmittingId(homeworkId);
    setActionMessage('');
    try {
      let attachmentUrl = '';
      if (file) {
        const uploaded = await classroomApi.uploadHomeworkSubmissionAttachment(file);
        attachmentUrl = uploaded.url;
      }
      await classroomApi.submitHomework(homeworkId, {
        textAnswer: submitAnswers[homeworkId] || '',
        attachmentUrl,
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

  const handleCreateAttendanceDispute = async (attendanceId) => {
    if (!disputeForm.reason.trim()) {
      setActionMessage('Vui lòng nhập lý do khiếu nại điểm danh.');
      return;
    }
    setSubmittingDispute(true);
    setActionMessage('');
    try {
      await classroomApi.createAttendanceDispute(attendanceId, disputeForm.reason.trim());
      setActionMessage('Đã gửi khiếu nại điểm danh. Training Manager sẽ xử lý và phản hồi.');
      setDisputeForm({ attendanceId: null, reason: '' });
      setAttendanceDisputes(await classroomApi.listMyAttendanceDisputes());
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không gửi được khiếu nại điểm danh.'));
    } finally {
      setSubmittingDispute(false);
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
    if (activeTab === 'curriculum') {
      return <LearnerCurriculumPanel curriculum={classroom?.curriculumProgram} />;
    }

    if (activeTab === 'flashcards') {
      return <ClassroomFlashcardsPanel curriculum={classroom?.curriculumProgram} />;
    }

    if (activeTab === 'overview') {
      const isVirtual = classroom.deliveryMode === 'VIRTUAL';
      const totalSessions = sessions.length;
      const attendedCount = attendanceStats.present + Math.round(attendanceStats.late * 0.5);

      return (
        <div className="space-y-8">
          {/* ── Quick Action Bar ── */}
          <div className="flex flex-wrap gap-2.5">
            {isVirtual && nextSession?.larkJoinable && (nextSession.larkMeetingUrl || nextSession.id) && (
              <VirtualJoinButton
                classroomId={id}
                sessionId={nextSession.id}
                onBlocked={setLarkMessage}
                url={nextSession.larkMeetingUrl}
              />
            )}
            <button
              className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-xs font-bold text-gray-700 transition hover:bg-gray-50 active:scale-95"
              onClick={() => setActiveTab('schedule')}
              type="button"
            >
              <Calendar className="h-4 w-4 text-[#730014]" />
              Xem lịch học chi tiết
            </button>
            {pendingHomework.length > 0 && (
              <button
                className="inline-flex items-center gap-1.5 rounded-xl border border-[#dfbfbd] bg-[#fff5f5] px-4 py-2.5 text-xs font-bold text-[#730014] transition hover:bg-[#fff0f1] active:scale-95"
                onClick={() => setActiveTab('homework')}
                type="button"
              >
                <AlertCircle className="h-4 w-4" />
                {pendingHomework.length} bài tập cần hoàn thành
              </button>
            )}
            {larkMessage && <p className="w-full text-xs text-rose-700 font-semibold">{larkMessage}</p>}
          </div>

          {/* ── KPI Row ── */}
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <KpiCard
              label="Buổi học tham dự"
              value={`${attendedCount} / ${totalSessions}`}
              sub={totalSessions ? `${Math.round((attendedCount / totalSessions) * 100)}% số buổi` : 'Chưa diễn ra'}
              icon={<CheckCircle2 className="h-4.5 w-4.5" />}
            />
            <KpiCard
              label="Bài tập đã nộp"
              value={`${homework.filter((h) => h.mySubmission).length} / ${homework.length}`}
              sub={pendingHomework.length ? `Còn ${pendingHomework.length} bài chưa nộp` : 'Đã nộp đầy đủ'}
              icon={<FileText className="h-4.5 w-4.5" />}
            />
            <KpiCard
              label="Tỷ lệ chuyên cần"
              value={`${attendanceStats.rate}%`}
              sub={attendanceStats.rate >= 80 ? 'Đạt chuyên cần' : 'Dưới chỉ tiêu (80%)'}
              icon={<Activity className="h-4.5 w-4.5" />}
            />
            <KpiCard
              label="Điểm tích lũy"
              value={gradebook?.homeworkScore != null ? `${gradebook.homeworkScore}/10` : '—'}
              sub={gradebook ? formatGradebookFinalResult(gradebook.finalResult) : 'Đang cập nhật'}
              icon={<Award className="h-4.5 w-4.5" />}
            />
          </div>

          {/* ── Next Session ── */}
          <div className="space-y-3">
            <div className="flex items-center gap-2 mb-1.5">
              <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
              <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#1a1c1c]">Buổi học sắp tới</h3>
            </div>
            {nextSession ? (
              <div className={`rounded-3xl border p-6 bg-white shadow-[0_10px_30px_rgba(0,0,0,0.01)] ${
                nextSession.status === 'OPEN'
                  ? 'border-emerald-200 ring-2 ring-emerald-500/5'
                  : 'border-gray-200/80'
              }`}>
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="space-y-2">
                    {nextSession.status === 'OPEN' && (
                      <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 border border-emerald-200 px-3 py-0.5 text-[9px] font-extrabold text-emerald-800 uppercase tracking-widest">
                        <span className="h-1.5 w-1.5 animate-ping rounded-full bg-emerald-500" />
                        Đang diễn ra
                      </span>
                    )}
                    <h4 className="font-['Manrope'] text-base font-extrabold text-[#1a1c1c]">
                      {nextSession.sessionContent || `Buổi học ngày ${formatClassroomDate(nextSession.sessionDate)}`}
                    </h4>
                    
                    <div className="flex flex-wrap gap-x-4 gap-y-2 text-xs text-[#584140]">
                      <span className="flex items-center gap-1.5">
                        <Calendar className="h-4 w-4 text-[#730014]" />
                        {formatClassroomDate(nextSession.sessionDate)}
                      </span>
                      <span className="flex items-center gap-1.5">
                        <Clock className="h-4 w-4 text-[#730014]" />
                        {formatClassroomTime(nextSession.startTime)} – {formatClassroomTime(nextSession.endTime)}
                      </span>
                      {isVirtual ? (
                        <span className="flex items-center gap-1.5 text-purple-700 font-semibold">
                          <Video className="h-4 w-4" />
                          Học trực tuyến
                        </span>
                      ) : (
                        <span className="flex items-center gap-1.5">
                          <MapPin className="h-4 w-4 text-[#730014]" />
                          {nextSession.roomName ? `Phòng ${nextSession.roomName}` : 'Chờ xếp phòng'} · {nextSession.offlineAddress || 'Cơ sở Hà Nội'}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="flex flex-shrink-0 flex-wrap gap-2">
                    {isVirtual && nextSession.larkJoinable && (nextSession.larkMeetingUrl || nextSession.id) && (
                      <VirtualJoinButton
                        className="!px-4 !py-2.5 !text-xs"
                        classroomId={id}
                        sessionId={nextSession.id}
                        onBlocked={setLarkMessage}
                        url={nextSession.larkMeetingUrl}
                      />
                    )}
                    {nextSession.recordingUrl && (
                      <a
                        className="inline-flex items-center justify-center gap-1.5 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-xs font-bold text-gray-700 hover:bg-gray-50 transition active:scale-95"
                        href={nextSession.recordingUrl}
                        rel="noreferrer"
                        target="_blank"
                      >
                        <Play className="h-4 w-4 text-[#730014]" />
                        Xem ghi hình
                      </a>
                    )}
                  </div>
                </div>
              </div>
            ) : (
              <div className="rounded-3xl border border-dashed border-gray-200 bg-gray-50/20 p-8 text-center text-xs font-semibold text-[#8b706e]">
                Không có buổi học sắp tới. Lớp học đã hoàn thành lộ trình đào tạo.
              </div>
            )}
          </div>

          {/* ── Pending Homework ── */}
          {pendingHomework.length > 0 && (
            <div className="space-y-3">
              <div className="flex items-center gap-2 mb-1.5">
                <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
                <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#1a1c1c]">
                  Bài tập cần nộp ({pendingHomework.length})
                </h3>
              </div>
              <div className="grid gap-3">
                {pendingHomework.slice(0, 3).map((item) => {
                  const deadline = item.deadline ? new Date(item.deadline) : null;
                  const hoursLeft = deadline ? Math.round((deadline - Date.now()) / 3600000) : null;
                  const isUrgent = hoursLeft != null && hoursLeft < 24 && hoursLeft > 0;
                  const isOverdue = hoursLeft != null && hoursLeft <= 0;

                  return (
                    <div
                      key={item.id}
                      className={`flex items-center justify-between gap-4 rounded-2xl border p-4 transition ${
                        isOverdue
                          ? 'border-rose-100 bg-[#fff5f5]/30'
                          : isUrgent
                            ? 'border-amber-100 bg-amber-50/20'
                            : 'border-gray-200/80 bg-white'
                      }`}
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <div className={`flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-xl ${
                          isOverdue ? 'bg-rose-50 text-rose-700' : isUrgent ? 'bg-amber-50 text-amber-700' : 'bg-[#fff0f1] text-[#730014]'
                        }`}>
                          <FileText className="h-5 w-5" />
                        </div>
                        <div className="min-w-0">
                          <p className="text-xs font-bold text-[#1a1c1c] truncate">{item.title}</p>
                          <p className={`text-[10px] font-bold ${isOverdue ? 'text-rose-700' : isUrgent ? 'text-amber-700' : 'text-[#8b706e]'}`}>
                            {isOverdue ? 'Đã quá hạn nộp' : isUrgent ? `Còn ${hoursLeft} giờ nộp bài` : `Hạn chót: ${formatClassroomDateTime(item.deadline)}`}
                          </p>
                        </div>
                      </div>
                      <button
                        className="flex-shrink-0 rounded-xl border border-gray-200 hover:border-[#dfbfbd] bg-white px-4 py-2 text-xs font-bold text-gray-700 hover:text-[#730014] transition active:scale-95"
                        onClick={() => setActiveTab('homework')}
                        type="button"
                      >
                        Làm bài
                      </button>
                    </div>
                  );
                })}
                
                {pendingHomework.length > 3 && (
                  <button
                    className="w-full rounded-2xl border border-dashed border-gray-200 py-3 text-xs font-bold text-[#8b706e] hover:border-[#dfbfbd] hover:text-[#730014] bg-white transition"
                    onClick={() => setActiveTab('homework')}
                    type="button"
                  >
                    Xem tất cả {pendingHomework.length} bài tập trong tab Bài tập →
                  </button>
                )}
              </div>
            </div>
          )}

          {/* ── Latest Announcement ── */}
          {announcements.length > 0 && (
            <div className="space-y-3">
              <div className="flex items-center gap-2 mb-1.5">
                <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
                <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#1a1c1c]">Thông báo mới</h3>
              </div>
              <div
                className="rounded-3xl border border-gray-200/80 bg-white p-5 space-y-3 cursor-pointer hover:border-[#dfbfbd] transition shadow-[0_10px_30px_rgba(0,0,0,0.01)]"
                onClick={() => setActiveTab('announcements')}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => e.key === 'Enter' && setActiveTab('announcements')}
              >
                <div className="flex items-center justify-between gap-3 border-b border-gray-50 pb-2">
                  <h4 className="font-['Manrope'] text-sm font-extrabold text-[#1a1c1c] flex items-center gap-2">
                    <Bell className="h-4.5 w-4.5 text-[#730014] flex-shrink-0" />
                    {announcements[0].title}
                  </h4>
                  <span className="flex-shrink-0 text-[10px] font-bold text-gray-400">
                    {formatClassroomDateTime(announcements[0].createdAt)}
                  </span>
                </div>
                <p className="text-xs text-[#584140] line-clamp-2 leading-relaxed">
                  {announcements[0].content || announcements[0].body}
                </p>
                {announcements.length > 1 && (
                  <p className="text-[10px] font-extrabold text-[#730014] pt-1">
                    Xem {announcements.length - 1} thông báo cũ khác tại đây →
                  </p>
                )}
              </div>
            </div>
          )}

          {/* ── Syllabus ── */}
          {syllabus.length > 0 && (
            <div className="space-y-3">
              <div className="flex items-center gap-2 mb-1.5">
                <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
                <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#1a1c1c]">Lộ trình chương trình</h3>
              </div>
              <div className="grid gap-3">
                {syllabus.map((item, idx) => (
                  <div
                    key={item.id}
                    className="flex items-start gap-4 rounded-2xl border border-gray-200/80 bg-white p-4 hover:border-[#dfbfbd]/50 transition"
                  >
                    <span className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-xl bg-[#fff0f1] text-xs font-extrabold text-[#730014]">
                      {item.weekNumber || idx + 1}
                    </span>
                    <div className="space-y-0.5">
                      <p className="text-xs font-extrabold text-[#1a1c1c]">{item.title}</p>
                      {item.description && <p className="text-[11px] text-[#584140] leading-relaxed">{item.description}</p>}
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
          <div className="flex items-center gap-2 mb-4.5">
            <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
            <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#1a1c1c]">Lịch trình chi tiết các buổi học</h3>
          </div>
          
          <div className="relative border-l-2 border-gray-200 pl-6 ml-4 space-y-6 py-2">
            {sessions.map((session) => {
              const isVirtual = session.deliveryMode === 'VIRTUAL';
              const isLarkJoinable = isVirtual && session.larkJoinable && (session.larkMeetingUrl || session.id);
              const isActive = session.status === 'IN_PROGRESS' || session.status === 'OPEN';

              return (
                <div key={session.id} className="relative">
                  {/* Timeline dot */}
                  <div className={`absolute -left-[33px] top-6 h-3.5 w-3.5 rounded-full border-2 bg-white transition ${
                    isActive ? 'border-[#730014] ring-4 ring-[#730014]/10 scale-110' : 'border-gray-300'
                  }`} />
                  
                  {/* Card item */}
                  <div className={`rounded-2xl border p-5 bg-white transition shadow-[0_10px_25px_rgba(0,0,0,0.01)] ${
                    isActive ? 'border-[#730014]/50 ring-2 ring-[#730014]/5' : 'border-gray-200/80'
                  }`}>
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                      <div className="space-y-3">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="inline-flex items-center gap-1 rounded-full bg-gray-50 border border-gray-150 px-2.5 py-0.5 text-[9px] font-extrabold uppercase tracking-widest text-[#584140]">
                            {isVirtual ? 'Zoom/Meet' : 'Tại cơ sở'}
                          </span>
                          <span className="inline-flex items-center rounded-full bg-gray-50 border border-gray-150 px-2 py-0.5 text-[9px] font-extrabold uppercase tracking-widest text-gray-500">
                            {session.status}
                          </span>
                          {isActive && (
                            <span className="rounded-full bg-[#fff0f1] px-2 py-0.5 text-[9px] font-extrabold uppercase tracking-widest text-[#730014] animate-pulse">
                              Đang mở
                            </span>
                          )}
                        </div>

                        <h3 className="font-['Manrope'] text-sm font-extrabold text-[#1a1c1c]">
                          {session.sessionContent || `Buổi học ngày ${formatClassroomDate(session.sessionDate)}`}
                        </h3>

                        <div className="grid gap-x-6 gap-y-2 text-xs text-[#584140] sm:grid-cols-2">
                          <div className="flex items-center gap-1.5">
                            <Calendar className="h-4 w-4 text-[#730014] shrink-0" />
                            <span>{formatClassroomDate(session.sessionDate)}</span>
                          </div>
                          <div className="flex items-center gap-1.5">
                            <Clock className="h-4 w-4 text-[#730014] shrink-0" />
                            <span>{formatClassroomTime(session.startTime)} - {formatClassroomTime(session.endTime)}</span>
                          </div>
                          <div className="flex items-center gap-1.5 col-span-1 sm:col-span-2">
                            <MapPin className="h-4 w-4 text-[#730014] shrink-0" />
                            <span className="truncate">
                              {isVirtual
                                ? 'Phòng học trực tuyến'
                                : `${session.roomName || 'Đang xếp phòng'} · ${session.offlineAddress || 'Cơ sở Hà Nội'}`}
                            </span>
                          </div>
                        </div>
                      </div>

                      <div className="flex-shrink-0 flex items-center pt-2 sm:pt-0">
                        {isLarkJoinable && (
                          <VirtualJoinButton
                            className="!px-4 !py-2.5 !text-xs"
                            classroomId={id}
                            sessionId={session.id}
                            onBlocked={setLarkMessage}
                            url={session.larkMeetingUrl}
                          />
                        )}
                        {session.recordingUrl && (
                          <a
                            className="inline-flex items-center justify-center gap-1.5 rounded-xl border border-gray-200 bg-white px-4 py-2 text-xs font-bold text-gray-700 hover:bg-gray-50 transition active:scale-95"
                            href={session.recordingUrl}
                            rel="noreferrer"
                            target="_blank"
                          >
                            <Play className="h-4 w-4 text-[#730014]" />
                            Xem lại ghi hình
                          </a>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
          {larkMessage && <p className="text-xs font-bold text-rose-700">{larkMessage}</p>}
        </div>
      );
    }

    if (activeTab === 'payment') {
      const tuitionDue = classroom.tuitionAmountDue ?? 0;
      const tuitionRemaining = tuitionDue - (classroom.tuitionAmountPaid ?? 0);
      const canSubmitProof = classroom.registrationStatus !== 'WAITLIST'
        && tuitionDue > 0
        && (classroom.tuitionAmountPaid ?? 0) < tuitionDue;
      return (
        <div className="space-y-6">
          {tuitionDue > 0 && (
            <div className="rounded-2xl border border-[#dfbfbd]/45 bg-[#fff5f5]/15 p-5 text-xs text-[#584140] flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2 mb-1.5">
                  <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
                  <p className="font-extrabold text-[#1a1c1c] uppercase tracking-widest text-[10px]">Học phí lớp học</p>
                </div>
                <p className="mt-1 font-['Manrope'] text-lg font-extrabold text-[#1a1c1c]">
                  Tổng phải đóng: {formatClassroomPrice(tuitionDue)}
                </p>
              </div>
              <div className="text-right">
                <span className="text-xs font-bold block">
                  Đã đóng: <strong className="text-emerald-700">{formatClassroomPrice(classroom.tuitionAmountPaid ?? 0)}</strong>
                </span>
                {tuitionRemaining > 0 && (
                  <span className="text-xs font-bold block mt-0.5 text-rose-700">
                    Còn lại: {formatClassroomPrice(tuitionRemaining)}
                  </span>
                )}
              </div>
            </div>
          )}
          <TuitionPaymentSection
            canSubmitProof={canSubmitProof}
            classroomId={id}
            tuitionRemaining={tuitionRemaining}
            onUpdated={async () => {
              try {
                const refreshed = await classroomApi.getMyClassroom(id);
                setClassroom(refreshed);
              } catch {
                // ignore refresh errors
              }
            }}
          />
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
        <div className="space-y-6">
          <div className="flex items-center gap-2 mb-4">
            <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
            <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#1a1c1c]">Danh sách bài tập thực hành</h3>
          </div>
          
          <div className="grid gap-6 md:grid-cols-2">
            {homework.map((item) => {
              const hasSubmission = !!item.mySubmission;
              const isGraded = hasSubmission && item.mySubmission.score != null;
              const isOverdue = item.overdue && !hasSubmission;
              const canSubmit = canResubmitHomework(item);
              
              const isUrgent = !hasSubmission && !isOverdue && new Date(item.deadline) - new Date() < 24 * 60 * 60 * 1000;
              const statusInfo = getMinimalistStatusInfo(isGraded ? 'GRADED' : hasSubmission ? 'SUBMITTED' : isOverdue ? 'OVERDUE' : 'NOT_SUBMITTED');

              return (
                <article
                  key={item.id}
                  className={`relative overflow-hidden rounded-[26px] border p-6 bg-white shadow-[0_10px_30px_rgba(0,0,0,0.01)] transition duration-300 hover:shadow-[0_20px_50px_rgba(115,0,20,0.05)] hover:border-[#730014]/20 flex flex-col justify-between ${
                    isUrgent ? 'border-amber-300' : 'border-gray-200/80'
                  }`}
                >
                  <div className="space-y-4">
                    {/* Card Header */}
                    <div className="flex items-center justify-between w-full">
                      <div className="flex items-center gap-2">
                        <span className={`h-2 w-2 rounded-full ${statusInfo.dotColor}`} />
                        <span className="text-[10px] font-extrabold uppercase tracking-wider text-gray-500">
                          {statusInfo.text}
                        </span>
                      </div>

                      <span className="inline-flex items-center rounded-full bg-[#fff0f1] px-2.5 py-0.5 text-[9px] font-extrabold uppercase tracking-widest text-[#730014]">
                        {getHomeworkSkillLabel(item.skill)}
                      </span>
                    </div>

                    {/* Title & info tags */}
                    <div className="space-y-1">
                      <h3 className="font-['Manrope'] text-base font-extrabold text-[#1a1c1c] leading-snug">
                        {item.title}
                      </h3>
                      
                      <div className="flex flex-wrap gap-1.5 pt-1">
                        {isAiGradedHomework(item) && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-[#fff5f5] px-2 py-0.5 text-[9px] font-bold text-[#8a0018] border border-[#dfbfbd]/40">
                            <Sparkles className="h-3 w-3" />
                            AI Review
                          </span>
                        )}
                        {getHomeworkGradingHint(item) && !hasSubmission && (
                          <span className="text-[10px] text-purple-700 font-bold block">{getHomeworkGradingHint(item)}</span>
                        )}
                      </div>
                    </div>

                    <p className="text-xs text-[#584140] line-clamp-3 leading-relaxed">
                      {item.instruction || 'Không có mô tả chi tiết bài tập.'}
                    </p>

                    <div className="flex items-center gap-2 text-xs text-[#8b706e] pt-2 border-t border-gray-50">
                      <Clock className="h-4 w-4 text-[#730014] shrink-0" />
                      <span>Hạn nộp: <strong className="text-[#584140] font-semibold">{formatClassroomDateTime(item.deadline)}</strong></span>
                    </div>

                    {/* Graded block display */}
                    {isGraded && (
                      <div className="rounded-xl border border-emerald-100 bg-emerald-50/15 p-4 space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="text-[10px] font-extrabold text-emerald-800 uppercase tracking-widest flex items-center gap-1">
                            <Award className="h-4 w-4" />
                            Điểm chấm
                          </span>
                          <strong className="text-emerald-700 text-xs font-extrabold">{item.mySubmission.score} / {getHomeworkMaxScore(item)} điểm</strong>
                        </div>
                        {getSubmissionFeedback(item.mySubmission) && (
                          <p className="text-xs text-[#584140] italic leading-normal border-t border-emerald-500/10 pt-2 mt-2">
                            {getHomeworkFeedbackLabel(item)}: "{getSubmissionFeedback(item.mySubmission)}"
                          </p>
                        )}
                      </div>
                    )}

                    {/* Submitted but not graded block */}
                    {hasSubmission && !isGraded && (
                      <div className="rounded-xl border border-blue-100 bg-blue-50/15 p-4">
                        <p className="text-[10px] font-extrabold text-blue-800 uppercase tracking-widest flex items-center gap-1.5">
                          <CheckCircle2 className="h-4 w-4 text-blue-600" />
                          {isAiGradedHomework(item) ? 'Đang chờ kết quả chấm AI' : 'Đã nộp bài học'}
                        </p>
                        <p className="mt-1.5 text-xs text-[#584140] line-clamp-2 leading-relaxed">Đã gửi: "{item.mySubmission.textAnswer}"</p>
                      </div>
                    )}

                    {canSubmit && usesInteractiveHomeworkWorkspace(item) ? (
                      <div className="border-t border-gray-50 pt-3">
                        <Link
                          className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014]"
                          to={`/my-homework?open=${item.id}`}
                        >
                          {item.activityType === 'FLASHCARD_REVIEW' ? <BookOpen className="h-4 w-4" /> : <FileText className="h-4 w-4" />}
                          {item.activityType === 'FLASHCARD_REVIEW'
                            ? 'Học flashcard theo unit'
                            : hasSubmission ? 'Vào làm lại bài' : 'Vào phòng làm bài'}
                        </Link>
                      </div>
                    ) : null}

                    {/* File and free-form submissions stay inline. */}
                    {canSubmit && !usesInteractiveHomeworkWorkspace(item) && (
                      <div className="space-y-3 pt-3 border-t border-gray-50">
                        
                        {/* File picker Dropzone area */}
                        <div className="space-y-1">
                          <label className="group block cursor-pointer rounded-xl border border-dashed border-gray-200 bg-gray-50/10 p-4 text-center transition hover:border-[#730014] hover:bg-[#fff0f1]/10">
                            <input
                              accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.zip,.rar,.jpg,.jpeg,.png,.mp3,.m4a,.webm"
                              className="sr-only"
                              onChange={(event) => setSubmitFiles((curr) => ({ ...curr, [item.id]: event.target.files?.[0] || null }))}
                              type="file"
                            />
                            <div className="flex flex-col items-center justify-center gap-1">
                              <Upload className="h-4 w-4 text-gray-400 group-hover:text-[#730014] transition-colors" />
                              <span className="text-[10px] font-bold text-gray-700">Đính kèm tệp làm bài</span>
                              {submitFiles[item.id] ? (
                                <span className="text-[10px] font-bold text-emerald-700">{submitFiles[item.id].name}</span>
                              ) : item.mySubmission?.attachmentUrl ? (
                                <span className="text-[10px] font-medium text-gray-500">Giữ file đính kèm cũ</span>
                              ) : null}
                            </div>
                          </label>
                        </div>

                        <textarea
                          className="min-h-[100px] w-full rounded-2xl border border-gray-200 bg-gray-50/10 px-4 py-3 text-xs text-[#1a1c1c] outline-none transition focus:border-[#730014] focus:bg-white leading-relaxed"
                          onChange={(e) => setSubmitAnswers((curr) => ({ ...curr, [item.id]: e.target.value }))}
                          placeholder="Nhập nội dung trả lời..."
                          value={submitAnswers[item.id] ?? item.mySubmission?.textAnswer ?? ''}
                        />
                        
                        <button
                          className="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-[#730014] to-[#4b0009] px-5 py-2.5 text-xs font-bold text-white shadow-sm transition hover:shadow active:scale-95 btn-hover"
                          disabled={submittingId === item.id}
                          onClick={() => handleSubmitHomework(item.id)}
                          type="button"
                        >
                          {submittingId === item.id ? (
                            <>
                              <RefreshCw className="h-4 w-4 animate-spin" />
                              Đang nộp...
                            </>
                          ) : (
                            <>
                              <Send className="h-3.5 w-3.5" />
                              {hasSubmission ? 'Cập nhật bài làm' : 'Nộp bài làm'}
                            </>
                          )}
                        </button>
                      </div>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
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
          <div className="flex items-center gap-2 mb-4">
            <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
            <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#1a1c1c]">Báo cáo chuyên cần</h3>
          </div>
          
          {/* Attendance Stats Dashboard */}
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-5 items-center rounded-2xl border border-gray-200/80 bg-white p-5 shadow-[0_10px_30px_rgba(0,0,0,0.01)]">
            <div className="lg:col-span-2 flex justify-center py-2">
              <ProgressRing percent={attendanceStats.rate} size={110} strokeWidth={10} label="Tỷ lệ chuyên cần" />
            </div>

            <div className="grid grid-cols-3 gap-4 sm:grid-cols-3 lg:col-span-3 w-full">
              <div className="rounded-xl bg-white border border-gray-250/60 p-4 text-center">
                <p className="text-[10px] font-extrabold text-[#8b706e] uppercase tracking-wider">Tổng số buổi</p>
                <p className="mt-1.5 font-['Manrope'] text-2xl font-extrabold text-gray-700">{attendanceStats.total}</p>
              </div>
              <div className="rounded-xl bg-[#fffafb] border border-emerald-100 p-4 text-center">
                <p className="text-[10px] font-extrabold text-emerald-700 uppercase tracking-wider">Có mặt</p>
                <p className="mt-1.5 font-['Manrope'] text-2xl font-extrabold text-emerald-700">{attendanceStats.present}</p>
              </div>
              <div className="rounded-xl bg-[#fffafb] border border-rose-100 p-4 text-center">
                <p className="text-[10px] font-extrabold text-rose-700 uppercase tracking-wider">Vắng mặt</p>
                <p className="mt-1.5 font-['Manrope'] text-2xl font-extrabold text-rose-700">{attendanceStats.absent}</p>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2 mt-8 mb-4">
            <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
            <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#1a1c1c]">Bảng điểm danh chi tiết</h3>
          </div>

          {/* Attendance Timeline Table */}
          <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white">
            <table className="min-w-full divide-y divide-gray-150 text-left text-sm">
              <thead className="bg-[#fffafb] text-[10px] font-extrabold text-[#8b706e] uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-4">Buổi học</th>
                  <th className="px-6 py-4">Ghi chú</th>
                  <th className="px-6 py-4">Trạng thái</th>
                  <th className="px-6 py-4 text-right">Hành động khiếu nại</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-[#584140]">
                {attendance.map((record, idx) => {
                  const dispute = attendanceDisputes.find((item) => Number(item.attendanceId) === Number(record.id));
                  const isDisputeOpen = Number(disputeForm.attendanceId) === Number(record.id);
                  return (
                    <tr key={record.id || `${record.sessionId}-${idx}`} className="align-top hover:bg-gray-50/40">
                      <td className="whitespace-nowrap px-6 py-4.5 font-bold text-[#1a1c1c]">
                        Buổi #{record.sessionId}
                      </td>
                      <td className="px-6 py-4.5 text-xs">
                        {record.note || 'Không có ghi chú'}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4.5">
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-[10px] font-extrabold ${
                          record.status === 'PRESENT'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                            : record.status === 'LATE'
                              ? 'bg-amber-50 text-amber-700 border border-amber-100'
                              : 'bg-rose-50 text-rose-700 border border-rose-100'
                        }`}>
                          {formatAttendanceStatus(record.status)}
                        </span>
                      </td>
                      <td className="min-w-[260px] px-6 py-4.5 text-right flex justify-end">
                        {dispute ? (
                          <div className="rounded-xl border border-gray-200 bg-gray-50/50 p-3 text-[11px] text-left max-w-xs leading-normal">
                            <p className="font-extrabold text-[#730014] uppercase tracking-wider text-[9px]">{formatDisputeStatus(dispute.status)}</p>
                            {dispute.reason && <p className="mt-1 text-gray-500">Lý do: "{dispute.reason}"</p>}
                            {dispute.reviewNote && <p className="mt-1.5 pt-1.5 border-t border-gray-200 text-[#584140] font-medium">Bản duyệt: "{dispute.reviewNote}"</p>}
                          </div>
                        ) : isDisputeOpen ? (
                          <div className="space-y-2 text-left w-full max-w-sm">
                            <textarea
                              className="min-h-16 w-full rounded-xl border border-gray-200 bg-white px-3 py-2 text-xs outline-none focus:border-[#730014]"
                              onChange={(event) => setDisputeForm((current) => ({ ...current, reason: event.target.value }))}
                              placeholder="Mô tả cụ thể lý do vắng mặt hoặc đi muộn..."
                              value={disputeForm.reason}
                            />
                            <div className="flex gap-2 justify-end">
                              <button
                                className="rounded-lg bg-[#4b0009] px-3.5 py-1.5 text-xs font-bold text-white disabled:opacity-60 hover:bg-[#730014] transition"
                                disabled={submittingDispute}
                                onClick={() => handleCreateAttendanceDispute(record.id)}
                                type="button"
                              >
                                {submittingDispute ? 'Đang gửi...' : 'Gửi'}
                              </button>
                              <button
                                className="rounded-lg border border-gray-200 px-3.5 py-1.5 text-xs font-bold text-gray-600 bg-white hover:bg-gray-50 transition"
                                onClick={() => setDisputeForm({ attendanceId: null, reason: '' })}
                                type="button"
                              >
                                Hủy
                              </button>
                            </div>
                          </div>
                        ) : (
                          <button
                            className="rounded-lg border border-[#dfbfbd] bg-white px-3 py-1.5 text-xs font-bold text-[#730014] hover:bg-[#fff0f1] transition active:scale-95"
                            onClick={() => setDisputeForm({ attendanceId: record.id, reason: '' })}
                            type="button"
                          >
                            Gửi khiếu nại
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
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
          <div className="flex items-center gap-2 mb-4">
            <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
            <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#1a1c1c]">Bảng điểm chi tiết</h3>
          </div>
          
          {/* Grade Summary Cards */}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <GradeIndicatorCard label="Bài tập" score={gradebook.homeworkScore} color="red" />
            {gradebook.quizScore != null ? (
              <GradeIndicatorCard label="Quiz" score={gradebook.quizScore} color="blue" />
            ) : null}
            <GradeIndicatorCard label="Chuyên cần" score={gradebook.attendancePercent != null ? gradebook.attendancePercent / 10 : null} suffix="%" customScore={gradebook.attendancePercent} color="emerald" />
            <GradeIndicatorCard label="Tương tác phát biểu" score={gradebook.participationScore} color="purple" />
          </div>

          {/* Final Result Banner */}
          <div className="flex items-center gap-2 mt-8 mb-4">
            <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
            <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#1a1c1c]">Tổng kết kết quả</h3>
          </div>
          
          <div className={`rounded-2xl border p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 ${
            isPassed ? 'border-emerald-100 bg-emerald-50/15' : 'border-amber-100 bg-amber-50/15'
          }`}>
            <div>
              <p className="text-[10px] font-extrabold text-[#8b706e] uppercase tracking-wider">Tổng kết kết quả</p>
              <h4 className={`mt-1 font-['Manrope'] text-xl font-extrabold ${isPassed ? 'text-emerald-800' : 'text-amber-800'}`}>
                {finalResultLabel}
              </h4>
            </div>
            <span className={`inline-flex items-center gap-1.5 rounded-full px-4 py-1.5 text-xs font-extrabold uppercase tracking-widest ${
              isPassed ? 'bg-emerald-100 text-emerald-800 border border-emerald-200' : 'bg-amber-100 text-amber-800 border border-amber-200'
            }`}>
              {isPassed ? <CheckCircle2 className="h-4 w-4" /> : <Info className="h-4 w-4" />}
              {isPassed ? 'Đã hoàn thành' : 'Đang xử lý'}
            </span>
          </div>

          {/* Teacher Comment */}
          {gradebook.teacherComment && (
            <div className="rounded-2xl border border-gray-200/80 bg-white p-5 space-y-2 shadow-[0_10px_30px_rgba(0,0,0,0.01)]">
              <h4 className="text-[10px] font-extrabold text-[#730014] uppercase tracking-wider flex items-center gap-1.5">
                <MessageSquare className="h-4 w-4" />
                Đánh giá tổng hợp của giảng viên
              </h4>
              <p className="text-xs text-[#584140] leading-relaxed italic">
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
        <div className="space-y-6">
          <div className="flex items-center gap-2 mb-4">
            <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
            <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#1a1c1c]">Tài liệu & Học liệu</h3>
          </div>
          
          <div className="grid gap-4 sm:grid-cols-2">
            {materials.map((material) => (
              <article
                key={material.id}
                className="rounded-[24px] border border-gray-200/80 bg-white p-5 shadow-[0_10px_30px_rgba(0,0,0,0.01)] flex flex-col justify-between hover:border-[#dfbfbd] transition duration-300"
              >
                <div>
                  <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#fff0f1] text-[#730014] mb-3">
                    <FileText className="h-5 w-5" />
                  </div>
                  
                  <div className="flex flex-wrap items-center gap-2">
                    <h4 className="font-['Manrope'] text-sm font-extrabold text-[#1a1c1c] line-clamp-1">
                      {material.title}
                    </h4>
                    <span className={`inline-flex rounded-full px-2 py-0.5 text-[8px] font-extrabold uppercase tracking-widest ${
                      material.sourceType === 'CENTER_LIBRARY'
                        ? 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                        : 'bg-[#fff0f1] text-[#730014] border border-[#dfbfbd]/35'
                    }`}>
                      {material.sourceType === 'CENTER_LIBRARY' ? 'Trung tâm' : 'Lớp học'}
                    </span>
                  </div>
                  {material.description && (
                    <p className="mt-2 text-xs text-[#584140] line-clamp-2 leading-relaxed">{material.description}</p>
                  )}
                  <div className="mt-3.5 space-y-1 text-[10px] font-bold text-[#8b706e] uppercase tracking-wider">
                    <p>Cung cấp: <span className="text-[#584140]">{material.provider || 'EnglishLab'}</span></p>
                    <p>Buổi học: <span className="text-[#584140]">{material.sessionTitle || 'Không gắn buổi'}</span></p>
                  </div>
                </div>

                {material.fileUrl && (
                  <div className="mt-4 pt-4 border-t border-gray-50 flex justify-end">
                    <button
                      className="inline-flex items-center gap-1.5 text-xs font-bold text-[#730014] hover:underline"
                      onClick={() => downloadClassroomMaterial(material)}
                      type="button"
                    >
                      <Download className="h-4 w-4" />
                      Tải học liệu (.pdf/.docx)
                    </button>
                  </div>
                )}
              </article>
            ))}
          </div>
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
      <div className="space-y-6">
        <div className="flex items-center gap-2 mb-4">
          <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
          <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#1a1c1c]">Thông báo chính thức từ lớp học</h3>
        </div>
        
        <div className="space-y-4">
          {announcements.map((announcement) => (
            <article
              key={announcement.id}
              className="rounded-[24px] border border-gray-200/80 bg-white p-5 shadow-[0_10px_30px_rgba(0,0,0,0.01)] space-y-3 hover:border-[#dfbfbd] transition duration-300"
            >
              <div className="flex items-center justify-between gap-3 border-b border-gray-50 pb-2">
                <h4 className="font-['Manrope'] text-sm font-extrabold text-[#1a1c1c] flex items-center gap-2">
                  <Bell className="h-4.5 w-4.5 text-[#730014]" />
                  {announcement.title}
                </h4>
                <span className="text-[10px] font-bold text-gray-400">
                  {formatClassroomDateTime(announcement.createdAt)}
                </span>
              </div>
              <p className="text-xs leading-relaxed text-[#584140] whitespace-pre-wrap">
                {announcement.content || announcement.body}
              </p>
            </article>
          ))}
        </div>
      </div>
    );
  };

  const currentClassStatusInfo = useMemo(() => {
    if (!classroom) return null;
    return getMinimalistStatusInfo(classroom);
  }, [classroom]);

  return (
    <LearnerPageShell hideHeader={true}>
      {loading ? <ClassroomLoadingState message="Đang tải dữ liệu lớp học..." /> : null}
      {!loading && error ? <ClassroomErrorState message={error} onRetry={loadClassroom} /> : null}
      
      {!loading && !error && classroom ? (
        <div className="space-y-8 flex-1">
          
          {/* ── Classroom Apple Hero (Merged Card Banner) ── */}
          <div className="rounded-[28px] border border-gray-200/80 bg-white p-6 shadow-[0_10px_35px_rgba(0,0,0,0.015)] transition-all duration-300 hover:shadow-[0_15px_45px_rgba(75,0,9,0.04)]">
            <div className="flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
              
              <div className="space-y-3 flex-1 min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-gray-50 border border-gray-150 px-2.5 py-0.5 text-[9px] font-extrabold uppercase tracking-widest text-[#584140]">
                    {classroom.deliveryMode === 'VIRTUAL' ? <Video className="h-3 w-3 text-purple-600" /> : <Building className="h-3 w-3 text-[#730014]" />}
                    {classroom.deliveryMode === 'VIRTUAL' ? 'Học Online' : 'Tại trung tâm'}
                  </span>
                  
                  {currentClassStatusInfo && (
                    <div className="flex items-center gap-1.5 border border-gray-200 bg-gray-50/50 rounded-full px-2.5 py-0.5">
                      <span className={`h-1.5 w-1.5 rounded-full ${currentClassStatusInfo.dotColor}`} />
                      <span className="text-[9px] font-extrabold uppercase tracking-wider text-gray-500">
                        {currentClassStatusInfo.text}
                      </span>
                    </div>
                  )}
                </div>

                <div className="flex items-center gap-3">
                  <span className="h-6 w-1 shrink-0 rounded-full bg-[#8a0018]" />
                  <h1 className="font-['Manrope'] text-xl font-extrabold tracking-tight text-[#1a1c1c] md:text-2xl leading-snug">
                    {classroom.title}
                  </h1>
                </div>

                {classroom.shortDescription && (
                  <p className="text-xs leading-relaxed text-[#584140] pl-4">{classroom.shortDescription}</p>
                )}
                
                <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-[#584140] pl-4">
                  <span className="flex items-center gap-1.5">
                    <User className="h-4 w-4 text-[#730014]" />
                    Giảng viên: <strong className="text-[#1a1c1c] font-semibold">{classroom.primaryTeacherName || 'Đang cập nhật'}</strong>
                  </span>
                  {classroom.deliveryMode === 'OFFLINE' && classroom.offlineAddress && (
                    <span className="flex items-center gap-1.5">
                      <MapPin className="h-4 w-4 text-[#730014]" />
                      Địa điểm: {classroom.offlineAddress}
                    </span>
                  )}
                </div>

                {/* Progress bar */}
                {classroom.progressPercent != null && (
                  <div className="pt-2 pl-4 max-w-sm">
                    <div className="flex justify-between text-[10px] font-extrabold text-[#8b706e] uppercase tracking-wider mb-1">
                      <span>Tiến độ khóa học</span>
                      <span>{classroom.progressPercent}%</span>
                    </div>
                    <div className="h-1.5 w-full overflow-hidden rounded-full bg-gray-100">
                      <div className="h-full rounded-full bg-gradient-to-r from-[#730014] to-rose-600 transition-all duration-500" style={{ width: `${classroom.progressPercent}%` }} />
                    </div>
                  </div>
                )}
              </div>

              <div className="flex flex-shrink-0 flex-wrap gap-2 pt-2 md:pt-0">
                {classroom.deliveryMode === 'VIRTUAL' && nextSession?.larkJoinable && (nextSession.larkMeetingUrl || nextSession.id) && (
                  <VirtualJoinButton
                    classroomId={id}
                    sessionId={nextSession.id}
                    onBlocked={setLarkMessage}
                    url={nextSession.larkMeetingUrl}
                  />
                )}
                <Link
                  className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-5 py-3 text-xs font-bold text-gray-700 transition hover:bg-gray-50 active:scale-95 shadow-sm"
                  to="/my-classrooms"
                >
                  <ArrowLeft className="h-4 w-4" />
                  Quay lại lớp học
                </Link>
              </div>

            </div>
          </div>

          <div className="space-y-6">
            {/* Minimal Tab Bar */}
            <div className="flex flex-wrap gap-2 border-b border-gray-200 pb-3">
              {detailTabs.map((tab) => {
                const isActive = activeTab === tab.id;
                return (
                  <button
                    key={tab.id}
                    className={`relative rounded-xl px-4 py-2.5 text-xs font-extrabold tracking-wide transition-all duration-300 ${
                      isActive
                        ? 'bg-gradient-to-r from-[#730014] to-[#4b0009] text-white shadow-md shadow-[#4b0009]/20 scale-[1.02]'
                        : 'bg-white text-[#584140] hover:bg-[#fff0f1] hover:text-[#730014] border border-gray-200'
                    }`}
                    onClick={() => setActiveTab(tab.id)}
                    type="button"
                  >
                    {tab.label}
                  </button>
                );
              })}
            </div>

            {/* Action Notification message */}
            {actionMessage && (
              <div
                className={`rounded-2xl border p-4 text-xs font-semibold flex items-center gap-2 ${
                  actionMessage.includes('thành công')
                    ? 'bg-emerald-50 border-emerald-100 text-emerald-800'
                    : 'bg-rose-50 border-rose-100 text-rose-800'
                }`}
              >
                {actionMessage.includes('thành công') ? (
                  <CheckCircle2 className="h-4.5 w-4.5 text-emerald-600" />
                ) : (
                  <AlertCircle className="h-4.5 w-4.5 text-rose-600" />
                )}
                <p>{actionMessage}</p>
              </div>
            )}

            {/* Tab Content Panel wrapper */}
            <section className={activeTab === 'flashcards'
              ? 'min-h-[300px]'
              : 'min-h-[300px] rounded-[28px] border border-gray-200/80 bg-white p-6 shadow-[0_10px_35px_rgba(0,0,0,0.015)]'}>
              {renderTabContent()}
            </section>
          </div>

        </div>
      ) : null}
    </LearnerPageShell>
  );
}

// ─── Local KPI mini-card ──────────────────────────────────────────────────────
function KpiCard({ label, value, sub, icon }) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-[0_10px_30px_rgba(0,0,0,0.015)] flex flex-col justify-between space-y-2 hover:border-[#dfbfbd] transition duration-300 h-full">
      <div className="flex items-center justify-between">
        <span className="text-[10px] font-extrabold uppercase tracking-widest text-[#8b706e]">{label}</span>
        <div className="rounded-lg p-2 bg-[#fff0f1] text-[#730014] shrink-0">
          {icon}
        </div>
      </div>
      <div>
        <p className="font-['Manrope'] text-lg font-extrabold text-[#1a1c1c]">{value}</p>
        <p className="text-[10px] font-bold text-[#8b706e] mt-0.5">{sub}</p>
      </div>
    </div>
  );
}

// ─── Local Grade indicator card ───────────────────────────────────────────────
function GradeIndicatorCard({ label, score, suffix = '', customScore, color }) {
  const isAvailable = score != null;
  const percent = isAvailable ? score * 10 : 0;
  
  const colorsMap = {
    red: 'bg-[#730014]',
    blue: 'bg-blue-600',
    emerald: 'bg-emerald-600',
    purple: 'bg-purple-600',
  };
  const barColor = colorsMap[color] || colorsMap.red;

  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-[0_10px_30px_rgba(0,0,0,0.01)] space-y-3 hover:border-[#dfbfbd]/50 transition duration-300">
      <p className="text-[10px] font-extrabold text-[#8b706e] uppercase tracking-wider">{label}</p>
      <p className="font-['Manrope'] text-2xl font-extrabold text-[#1a1c1c]">
        {isAvailable ? `${customScore ?? score}${suffix}` : '—'}
      </p>
      <div className="h-1.5 w-full bg-gray-100 rounded-full overflow-hidden">
        <div className={`h-full rounded-full ${barColor}`} style={{ width: `${percent}%` }} />
      </div>
    </div>
  );
}

// ─── Curriculum Panel Subcomponent ───────────────────────────────────────────
function LearnerCurriculumPanel({ curriculum }) {
  if (!curriculum) {
    return (
      <ClassroomEmptyState
        description="Lớp học này chưa được liên kết với giáo trình chính thức."
        title="Chưa có giáo trình"
      />
    );
  }
  const units = curriculum.units || [];
  return (
    <div className="space-y-6">
      
      {/* Curriculum intro */}
      <div className="rounded-[24px] border border-gray-200 bg-white p-5 shadow-[0_10px_30px_rgba(0,0,0,0.01)] space-y-2">
        <div className="flex items-center gap-2 mb-2">
          <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
          <p className="text-[10px] font-extrabold uppercase tracking-widest text-[#1a1c1c]">Thông tin giáo trình chính thức</p>
        </div>
        <h3 className="font-['Manrope'] text-lg font-extrabold text-[#1a1c1c]">{curriculum.title}</h3>
        <p className="text-xs text-[#584140] font-semibold">
          {[
            curriculum.examCategory,
            curriculum.targetBand ? `Band mục tiêu: ${curriculum.targetBand}` : null,
            curriculum.targetScore ? `Score: ${curriculum.targetScore}` : null,
            curriculum.entryLevel ? `Đầu vào: ${curriculum.entryLevel}` : null
          ].filter(Boolean).join(' · ')}
        </p>
        {curriculum.outcomes && (
          <p className="mt-4 whitespace-pre-wrap text-xs leading-relaxed text-[#584140] border-t border-gray-50 pt-3">
            {curriculum.outcomes}
          </p>
        )}
      </div>

      {/* Units list */}
      {units.length ? (
        <div className="space-y-4">
          {units.map((unit) => (
            <article key={unit.id} className="rounded-[24px] border border-gray-200/85 bg-white p-5 shadow-[0_10px_30px_rgba(0,0,0,0.01)] space-y-3 hover:border-[#dfbfbd] transition duration-300">
              <h4 className="font-['Manrope'] text-sm font-extrabold text-[#1a1c1c]">
                {unit.displayOrder ?? 0}. {unit.title}
              </h4>
              {unit.description && <p className="text-xs text-[#584140] leading-relaxed">{unit.description}</p>}
              {unit.sessionPlan && (
                <p className="text-[11px] whitespace-pre-wrap leading-relaxed text-gray-500 bg-gray-50/50 p-3 rounded-xl">
                  {unit.sessionPlan}
                </p>
              )}
              
              <div className="mt-4 grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                <LearnerRefList title="Học liệu học tập" refs={unit.materials} />
                <LearnerRefList title="Bài tập củng cố" refs={unit.exercises} />
                <LearnerRefList title="Đề khảo sát" refs={unit.assessments} />
                <LearnerRefList title="Flashcards học từ" refs={unit.flashcards} />
              </div>
            </article>
          ))}
        </div>
      ) : (
        <ClassroomEmptyState
          description="Giáo trình này chưa được thiết kế các học phần chi tiết."
          title="Chưa có nội dung giáo trình"
        />
      )}
    </div>
  );
}

// ─── Reference list subcomponent ──────────────────────────────────────────────
function LearnerRefList({ title, refs = [] }) {
  const visibleRefs = refs.filter((ref) => String(ref.status || '').toUpperCase() !== 'ARCHIVED');
  return (
    <div className="rounded-2xl border border-gray-150 bg-[#fffafb]/60 p-4 space-y-2.5">
      <p className="text-[9px] font-extrabold uppercase tracking-widest text-[#8b706e] border-b border-gray-100 pb-1.5">{title}</p>
      {visibleRefs.length ? (
        <div className="space-y-2">
          {visibleRefs.map((ref) => (
            <div key={`${ref.type}-${ref.id}`} className="rounded-xl bg-white border border-gray-200/70 px-3 py-2.5 shadow-[0_2px_8px_rgba(0,0,0,0.015)]">
              <p className="font-extrabold text-xs text-[#1a1c1c] leading-snug">{ref.title}</p>
              {ref.subtitle && <p className="mt-1 text-[10px] text-[#8b706e] leading-none">{ref.subtitle}</p>}
            </div>
          ))}
        </div>
      ) : (
        <p className="text-[10px] text-gray-400 italic">Chưa có nội dung</p>
      )}
    </div>
  );
}

function getMinimalistStatusInfo(classroomOrStatus) {
  const status = typeof classroomOrStatus === 'string' ? classroomOrStatus : classroomOrStatus.classroomStatus;
  const configMap = {
    // Classroom Offering Statuses
    UPCOMING: { text: 'Sắp khai giảng', dotColor: 'bg-blue-500 animate-pulse' },
    OPEN: { text: 'Đang mở đăng ký', dotColor: 'bg-emerald-500 animate-ping' },
    FULL: { text: 'Đã đủ chỗ', dotColor: 'bg-amber-500' },
    ACTIVE: { text: 'Đang diễn ra', dotColor: 'bg-emerald-500 animate-pulse' },
    IN_PROGRESS: { text: 'Đang diễn ra', dotColor: 'bg-emerald-500 animate-pulse' },
    COMPLETED: { text: 'Đã kết thúc', dotColor: 'bg-gray-400' },
    CLOSED: { text: 'Đã đóng', dotColor: 'bg-gray-400' },

    // Homework Submission Statuses
    NOT_SUBMITTED: { text: 'Chưa nộp', dotColor: 'bg-amber-500' },
    SUBMITTED: { text: 'Đã nộp bài làm', dotColor: 'bg-blue-500 animate-pulse' },
    GRADED: { text: 'Đã chấm điểm', dotColor: 'bg-emerald-500' },
    OVERDUE: { text: 'Quá hạn nộp', dotColor: 'bg-rose-500 animate-pulse' },
  };

  return configMap[status] || { text: status || 'Chờ cập nhật', dotColor: 'bg-gray-400' };
}
