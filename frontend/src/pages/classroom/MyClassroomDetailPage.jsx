import { useEffect, useState, useMemo } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
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
  Sparkles,
  X,
  Paperclip,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import VirtualJoinButton from '../../components/classroom/VirtualJoinButton';
import TuitionPaymentSection from '../../components/classroom/TuitionPaymentSection';
import ClassroomFlashcardsPanel from '../../components/classroom/ClassroomFlashcardsPanel';
import Pagination, { usePagination } from '../../components/ui/Pagination';
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
  formatAssessmentType,
  formatClassroomDate,
  formatClassroomDateTime,
  formatClassroomPrice,
  formatClassroomTime,
  formatGradebookFinalResult,
  isGradebookPassed,
  downloadClassroomMaterial,
  formatSessionStatus,
  getHomeworkMaxScore,
  getSubmissionFeedback,
} from '../../utils/classroomHelpers';
import {
  getHomeworkFeedbackLabel,
  getHomeworkGradingHint,
  getHomeworkSkillLabel,
  isAiGradedHomework,
} from '../../utils/homeworkGradingConfig';

const getEffectiveSessionStatus = (session) => {
  if (!session) return 'SCHEDULED';
  if (session.status === 'CANCELLED') return 'CANCELLED';
  if (!session.sessionDate || !session.startTime) return session.status;
  
  const now = new Date();
  const endTime = session.endTime || (() => {
    const t = session.startTime.split(':').map(Number);
    return `${String(t[0] + 2).padStart(2, '0')}:${String(t[1]).padStart(2, '0')}:00`;
  })();
  
  const end = new Date(`${session.sessionDate}T${endTime}`);
  if (now >= end) return 'COMPLETED';
  
  const start = new Date(`${session.sessionDate}T${session.startTime}`);
  if (session.status === 'OPEN' || (now >= start && now < end)) {
    return 'OPEN';
  }
  
  return session.status || 'SCHEDULED';
};

const detailTabs = [
  { id: 'overview', label: 'Tổng quan' },
  { id: 'curriculum', label: 'Giáo trình' },
  { id: 'flashcards', label: 'Flashcard' },
  { id: 'practice', label: 'Luyện tập' },
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
  const [selectedHomeworkForSubmission, setSelectedHomeworkForSubmission] = useState(null);
  const [showAllSyllabus, setShowAllSyllabus] = useState(false);
  const [expandedUnits, setExpandedUnits] = useState(new Set());

  const { page: sessionsPage, setPage: setSessionsPage, totalPages: sessionsTotalPages, pageItems: paginatedSessions, totalItems: sessionsTotalItems } = usePagination(
    sessions,
    10,
    `sessions-${activeTab}`
  );

  const { page: homeworkPage, setPage: setHomeworkPage, totalPages: homeworkTotalPages, pageItems: paginatedHomeworkList, totalItems: homeworkTotalItems } = usePagination(
    homework,
    6,
    `homework-${activeTab}`
  );

  const { page: attendancePage, setPage: setAttendancePage, totalPages: attendanceTotalPages, pageItems: paginatedAttendance, totalItems: attendanceTotalItems } = usePagination(
    attendance,
    8,
    `attendance-${activeTab}`
  );

  const { page: materialsPage, setPage: setMaterialsPage, totalPages: materialsTotalPages, pageItems: paginatedMaterialsList, totalItems: materialsTotalItems } = usePagination(
    materials,
    6,
    `materials-${activeTab}`
  );

  const { page: announcementsPage, setPage: setAnnouncementsPage, totalPages: announcementsTotalPages, pageItems: paginatedAnnouncements, totalItems: announcementsTotalItems } = usePagination(
    announcements,
    5,
    `announcements-${activeTab}`
  );

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
    const upcoming = sessions
      .filter((s) => {
        const eff = getEffectiveSessionStatus(s);
        return eff === 'SCHEDULED' || eff === 'OPEN';
      })
      .sort((a, b) => a.sessionDate.localeCompare(b.sessionDate) || a.startTime.localeCompare(b.startTime));

    if (upcoming.length) {
      const s = upcoming[0];
      return {
        ...s,
        effectiveStatus: getEffectiveSessionStatus(s),
      };
    }
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

  const handleSyllabusClick = (syllabusItem) => {
    const matchingUnit = classroom?.curriculumProgram?.units?.find(
      (unit) => unit.title.trim().toLowerCase() === syllabusItem.title.trim().toLowerCase()
    );

    if (matchingUnit) {
      setActiveTab('curriculum');
      setExpandedUnits(new Set([matchingUnit.id]));
      setTimeout(() => {
        const element = document.getElementById(`curriculum-unit-${matchingUnit.id}`);
        if (element) {
          element.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      }, 150);
    } else {
      setActiveTab('curriculum');
    }
  };

  const renderTabContent = () => {
    if (activeTab === 'curriculum') {
      return (
        <LearnerCurriculumPanel
          curriculum={classroom?.curriculumProgram}
          onOpenPractice={() => setActiveTab('practice')}
          onOpenFlashcards={() => setActiveTab('flashcards')}
          expandedUnits={expandedUnits}
          setExpandedUnits={setExpandedUnits}
        />
      );
    }

    if (activeTab === 'practice') {
      return <ClassroomPracticePanel classroomId={id} curriculum={classroom?.curriculumProgram} />;
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
              value={gradebook?.homeworkAverage != null ? `${gradebook.homeworkAverage}/10` : '—'}
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
                nextSession.effectiveStatus === 'OPEN'
                  ? 'border-emerald-200 ring-2 ring-emerald-500/5'
                  : 'border-gray-200/80'
              }`}>
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="space-y-2">
                    {nextSession.effectiveStatus === 'OPEN' && (
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
            <div className="space-y-4">
              <div className="flex items-center gap-2 mb-1">
                <span className="h-4 w-1 shrink-0 rounded-full bg-[#8a0018]" />
                <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#1a1c1c]">Lộ trình chương trình</h3>
              </div>

              {/* Connected vertical timeline roadmap */}
              <div className="relative pl-6 ml-4 border-l-2 border-dashed border-[#dfbfbd]/50 space-y-4 py-1.5">
                {(showAllSyllabus ? syllabus : syllabus.slice(0, 4)).map((item, idx) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => handleSyllabusClick(item)}
                    className="w-full text-left relative flex items-start gap-4 select-none outline-none group cursor-pointer active:scale-[0.995] transition-transform duration-150"
                  >
                    {/* Timeline node */}
                    <span className="absolute -left-[37px] top-0.5 flex h-7.5 w-7.5 shrink-0 items-center justify-center rounded-full bg-[#fff0f1] text-[11px] font-extrabold text-[#730014] border-2 border-white ring-4 ring-[#fff0f1]/20 shadow-[0_2px_8px_rgba(115,0,20,0.08)] group-hover:bg-[#730014] group-hover:text-white transition-colors duration-200">
                      {item.weekNumber || idx + 1}
                    </span>

                    <div className="flex-1 rounded-2xl border border-gray-200/80 bg-white p-3.5 group-hover:border-[#730014]/30 group-hover:shadow-[0_10px_25px_rgba(115,0,20,0.04)] transition duration-200">
                      <p className="text-xs font-extrabold text-[#1a1c1c] leading-snug group-hover:text-[#730014] transition-colors">{item.title}</p>
                      {item.description && (
                        <p className="mt-1 text-[11px] text-[#584140] leading-relaxed line-clamp-2">
                          {item.description}
                        </p>
                      )}
                    </div>
                  </button>
                ))}
              </div>

              {syllabus.length > 4 && (
                <div className="flex justify-center pt-2">
                  <button
                    type="button"
                    onClick={() => setShowAllSyllabus(!showAllSyllabus)}
                    className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-xs font-bold text-gray-700 hover:bg-gray-50 active:scale-95 transition"
                  >
                    {showAllSyllabus ? (
                      <>
                        <ChevronUp className="h-4 w-4 text-[#730014]" />
                        Thu gọn lộ trình
                      </>
                    ) : (
                      <>
                        <ChevronDown className="h-4 w-4 text-[#730014]" />
                        Xem toàn bộ lộ trình ({syllabus.length} bài học)
                      </>
                    )}
                  </button>
                </div>
              )}
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
            {paginatedSessions.map((session) => {
              const isVirtual = session.deliveryMode === 'VIRTUAL';
              const isLarkJoinable = isVirtual && session.larkJoinable && (session.larkMeetingUrl || session.id);
              const effStatus = getEffectiveSessionStatus(session);
              const isActive = effStatus === 'OPEN';

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
                            {formatSessionStatus(effStatus)}
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
          
          {sessionsTotalPages > 1 && (
            <div className="flex justify-center mt-6">
              <Pagination
                page={sessionsPage}
                onChange={setSessionsPage}
                totalPages={sessionsTotalPages}
                totalItems={sessionsTotalItems}
                pageSize={10}
              />
            </div>
          )}
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
            {paginatedHomeworkList.map((item) => {
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
                    isUrgent ? 'border-amber-300 ring-2 ring-amber-300/10' : 'border-gray-200/80'
                  }`}
                >
                  <div className="flex-1 flex flex-col justify-between space-y-4">
                    <div className="space-y-3">
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
                        <h3 className="font-['Manrope'] text-sm font-extrabold text-[#1a1c1c] leading-snug">
                          {item.title}
                        </h3>
                        
                        <div className="flex flex-wrap gap-1.5 pt-0.5">
                          {isAiGradedHomework(item) && (
                            <span className="inline-flex items-center gap-1 rounded-full bg-[#fff5f5] px-2 py-0.5 text-[9px] font-bold text-[#8a0018] border border-[#dfbfbd]/40">
                              <Sparkles className="h-3 w-3" />
                              AI Review
                            </span>
                          )}
                          {getHomeworkGradingHint(item) && !hasSubmission && (
                            <span className="text-[9px] text-purple-700 font-bold bg-purple-50 px-2 py-0.5 rounded-full border border-purple-100">{getHomeworkGradingHint(item)}</span>
                          )}
                        </div>
                      </div>

                      <p className="text-xs text-[#584140] line-clamp-2 leading-relaxed">
                        {item.instruction || 'Không có mô tả chi tiết bài tập.'}
                      </p>

                      <div className="flex items-center gap-2 text-xs text-[#8b706e] pt-1">
                        <Clock className="h-4 w-4 text-[#730014] shrink-0" />
                        <span>Hạn nộp: <strong className="text-[#584140] font-semibold">{formatClassroomDateTime(item.deadline)}</strong></span>
                      </div>

                      {/* Graded block display */}
                      {isGraded && (
                        <div className="rounded-xl border border-emerald-100 bg-emerald-50/15 p-3 flex items-center justify-between">
                          <span className="text-[10px] font-extrabold text-emerald-800 uppercase tracking-widest flex items-center gap-1">
                            <Award className="h-4 w-4" />
                            Đã chấm điểm
                          </span>
                          <strong className="text-emerald-700 text-xs font-extrabold">{item.mySubmission.score} / {getHomeworkMaxScore(item)} điểm</strong>
                        </div>
                      )}

                      {/* Submitted but not graded block */}
                      {hasSubmission && !isGraded && (
                        <div className="rounded-xl border border-blue-150 bg-blue-50/10 p-3 flex items-center justify-between">
                          <span className="text-[10px] font-extrabold text-blue-800 uppercase tracking-widest flex items-center gap-1.5">
                            <CheckCircle2 className="h-4 w-4 text-blue-600" />
                            {isAiGradedHomework(item) ? 'Chờ AI chấm điểm' : 'Đã nộp bài'}
                          </span>
                          <span className="text-[10px] text-gray-500 font-bold">Chờ giảng viên duyệt</span>
                        </div>
                      )}
                    </div>

                    <div className="border-t border-gray-100 pt-3">
                      {usesInteractiveHomeworkWorkspace(item) ? (
                        <Link
                          className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-[#4b0009] px-5 py-2.5 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014]"
                          to={`/my-homework?open=${item.id}`}
                        >
                          {item.activityType === 'FLASHCARD_REVIEW' ? <BookOpen className="h-4 w-4" /> : <FileText className="h-4 w-4" />}
                          {item.activityType === 'FLASHCARD_REVIEW'
                            ? 'Học flashcard theo unit'
                            : hasSubmission && canSubmit ? 'Làm lại bài tập' : 'Bắt đầu làm bài'}
                        </Link>
                      ) : (
                        <button
                          className={`inline-flex w-full items-center justify-center gap-2 rounded-xl px-5 py-2.5 text-xs font-extrabold transition active:scale-95 ${
                            canSubmit
                              ? 'bg-[#4b0009] text-white hover:bg-[#730014]'
                              : 'border border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
                          }`}
                          onClick={() => setSelectedHomeworkForSubmission(item)}
                          type="button"
                        >
                          {canSubmit ? (
                            <>
                              <Upload className="h-4 w-4" />
                              {hasSubmission ? 'Cập nhật bài làm' : 'Nộp bài làm'}
                            </>
                          ) : (
                            <>
                              <FileText className="h-4 w-4 text-[#730014]" />
                              Xem chi tiết bài làm
                            </>
                          )}
                        </button>
                      )}
                    </div>
                  </div>
                </article>
              );
            })}
          </div>

          {homeworkTotalPages > 1 && (
            <div className="flex justify-center mt-6">
              <Pagination
                page={homeworkPage}
                onChange={setHomeworkPage}
                totalPages={homeworkTotalPages}
                totalItems={homeworkTotalItems}
                pageSize={6}
              />
            </div>
          )}
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
                {paginatedAttendance.map((record, idx) => {
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

          {attendanceTotalPages > 1 && (
            <div className="flex justify-end mt-4">
              <Pagination
                page={attendancePage}
                onChange={setAttendancePage}
                totalPages={attendanceTotalPages}
                totalItems={attendanceTotalItems}
                pageSize={8}
              />
            </div>
          )}
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
          <div className="grid gap-4 sm:grid-cols-2">
            <GradeIndicatorCard label="Điểm TB bài tập" score={gradebook.homeworkAverage} color="red" />
            <GradeIndicatorCard label="Chuyên cần" score={gradebook.attendancePercent != null ? gradebook.attendancePercent / 10 : null} suffix="%" customScore={gradebook.attendancePercent} color="emerald" />
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

          {materialsTotalPages > 1 && (
            <div className="flex justify-center mt-6">
              <Pagination
                page={materialsPage}
                onChange={setMaterialsPage}
                totalPages={materialsTotalPages}
                totalItems={materialsTotalItems}
                pageSize={6}
              />
            </div>
          )}
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
          {paginatedAnnouncements.map((announcement) => (
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

        {announcementsTotalPages > 1 && (
          <div className="flex justify-center mt-6">
            <Pagination
              page={announcementsPage}
              onChange={setAnnouncementsPage}
              totalPages={announcementsTotalPages}
              totalItems={announcementsTotalItems}
              pageSize={5}
            />
          </div>
        )}
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

          {selectedHomeworkForSubmission && (
            <EditorModal onClose={() => setSelectedHomeworkForSubmission(null)}>
              <HomeworkSubmissionForm
                homework={selectedHomeworkForSubmission}
                attachmentFile={submitFiles[selectedHomeworkForSubmission.id]}
                textAnswer={submitAnswers[selectedHomeworkForSubmission.id] ?? selectedHomeworkForSubmission.mySubmission?.textAnswer ?? ''}
                onAttachmentChange={(file) => setSubmitFiles((curr) => ({ ...curr, [selectedHomeworkForSubmission.id]: file }))}
                onTextChange={(val) => setSubmitAnswers((curr) => ({ ...curr, [selectedHomeworkForSubmission.id]: val }))}
                onCancel={() => setSelectedHomeworkForSubmission(null)}
                onSubmit={async () => {
                  await handleSubmitHomework(selectedHomeworkForSubmission.id);
                  setSelectedHomeworkForSubmission(null);
                }}
                submitting={submittingId === selectedHomeworkForSubmission.id}
                canResubmitHomework={canResubmitHomework}
              />
            </EditorModal>
          )}

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

function ClassroomPracticePanel({ classroomId, curriculum }) {
  const [selectedPractice, setSelectedPractice] = useState(null);
  const [practices, setPractices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [responseText, setResponseText] = useState('');
  const [savingExerciseId, setSavingExerciseId] = useState(null);

  useEffect(() => {
    let active = true;
    const loadPractice = async () => {
      setLoading(true);
      setError('');
      try {
        const data = await classroomApi.getClassroomPractice(classroomId);
        if (active) setPractices(data || []);
      } catch (err) {
        if (active) setError(getClassroomErrorMessage(err, 'Không thể tải nội dung luyện tập.'));
      } finally {
        if (active) setLoading(false);
      }
    };
    loadPractice();
    return () => {
      active = false;
    };
  }, [classroomId]);

  const practiceUnits = useMemo(() => {
    const grouped = new Map();
    practices.forEach((practice) => {
      if (!grouped.has(practice.unitId)) {
        grouped.set(practice.unitId, {
          id: practice.unitId,
          displayOrder: practice.unitDisplayOrder,
          title: practice.unitTitle,
          exercises: [],
        });
      }
      grouped.get(practice.unitId).exercises.push(practice);
    });
    return [...grouped.values()].sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
  }, [practices]);

  const openPractice = (exercise) => {
    setSelectedPractice(exercise);
    setResponseText(exercise.responseText || '');
    setError('');
  };

  const completePractice = async (exercise) => {
    setSavingExerciseId(exercise.exerciseId);
    setError('');
    try {
      const updated = await classroomApi.completeClassroomPractice(classroomId, exercise.exerciseId, { responseText });
      setPractices((current) => current.map((item) => (
        item.exerciseId === updated.exerciseId ? updated : item
      )));
      setSelectedPractice(updated);
      setResponseText(updated.responseText || '');
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể lưu kết quả luyện tập.'));
    } finally {
      setSavingExerciseId(null);
    }
  };

  if (!curriculum) {
    return <ClassroomEmptyState description="Lớp học chưa được liên kết với giáo trình." title="Chưa có nội dung luyện tập" />;
  }
  if (loading) {
    return <ClassroomLoadingState label="Đang tải nội dung luyện tập..." />;
  }
  if (error && !practices.length) {
    return <ClassroomErrorState message={error} />;
  }
  if (!practiceUnits.length) {
    return <ClassroomEmptyState description="Giáo trình hiện chưa có bài luyện tập được xuất bản." title="Chưa có bài luyện tập" />;
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-[#dfbfbd]/40 bg-white p-5">
        <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#730014]">Luyện tập theo giáo trình</p>
        <h2 className="mt-2 text-xl font-black text-[#1a1c1c]">{curriculum.title}</h2>
        <p className="mt-2 text-sm leading-6 text-[#6f5b59]">Đây là nội dung có sẵn của khóa đang học, không phải bài tập về nhà và không tính vào bảng điểm lớp.</p>
      </div>

      {error ? <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs font-semibold text-rose-700">{error}</div> : null}
      {practiceUnits.map((unit) => (
        <section className="rounded-2xl border border-gray-200 bg-white p-5" key={unit.id}>
          <div className="mb-4">
            <p className="text-[10px] font-extrabold uppercase tracking-widest text-[#8b706e]">Unit {unit.displayOrder}</p>
            <h3 className="mt-1 text-base font-black text-[#1a1c1c]">{unit.title}</h3>
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            {unit.exercises.map((exercise) => {
              const isOpen = selectedPractice?.exerciseId === exercise.exerciseId;
              return (
                <article className={`rounded-xl border p-4 transition ${isOpen ? 'border-[#730014]/40 bg-[#fffafb]' : 'border-gray-100 bg-gray-50/60'}`} key={exercise.exerciseId}>
                  <p className="text-sm font-extrabold text-[#262222]">{exercise.title}</p>
                  <div className="mt-2 flex flex-wrap gap-2 text-[10px] font-bold uppercase tracking-wider text-[#8b706e]">
                    {exercise.skill ? <span>{exercise.skill}</span> : null}
                    <span>Practice</span>
                    {exercise.completed ? <span className="text-emerald-700">Đã hoàn thành</span> : null}
                  </div>
                  {isOpen ? (
                    <div className="mt-4 space-y-3 border-t border-[#dfbfbd]/40 pt-4">
                      <p className="whitespace-pre-wrap text-sm leading-6 text-[#584140]">{exercise.instruction || exercise.note || 'Nội dung chi tiết đang được cập nhật.'}</p>
                      {exercise.note && exercise.instruction ? <p className="text-xs italic text-[#8b706e]">{exercise.note}</p> : null}
                      <label className="block space-y-2">
                        <span className="text-xs font-extrabold text-[#584140]">Câu trả lời / ghi chú tự luyện</span>
                        <textarea className="min-h-32 w-full rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]" onChange={(event) => setResponseText(event.target.value)} placeholder="Nhập câu trả lời hoặc ghi lại phần cần xem lại..." value={responseText} />
                      </label>
                      <div className="flex flex-wrap gap-2">
                        <button className="rounded-lg bg-[#730014] px-4 py-2.5 text-xs font-extrabold text-white disabled:opacity-60" disabled={savingExerciseId === exercise.exerciseId} onClick={() => completePractice(exercise)} type="button">
                          {savingExerciseId === exercise.exerciseId ? 'Đang lưu...' : exercise.completed ? 'Cập nhật lượt luyện' : 'Hoàn thành lượt luyện'}
                        </button>
                        <button className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-xs font-extrabold text-[#584140]" onClick={() => setSelectedPractice(null)} type="button">Thu gọn</button>
                      </div>
                    </div>
                  ) : (
                    <button className="mt-4 inline-flex items-center gap-2 rounded-lg bg-[#730014] px-4 py-2.5 text-xs font-extrabold text-white" onClick={() => openPractice(exercise)} type="button">
                      <Play className="h-3.5 w-3.5" />
                      {exercise.completed ? 'Luyện tập lại' : 'Bắt đầu luyện tập'}
                    </button>
                  )}
                </article>
              );
            })}
          </div>
        </section>
      ))}
    </div>
  );
}

// ─── Curriculum Panel Subcomponent ───────────────────────────────────────────
function LearnerCurriculumPanel({
  curriculum,
  onOpenPractice,
  onOpenFlashcards,
  expandedUnits,
  setExpandedUnits,
}) {
  if (!curriculum) {
    return (
      <ClassroomEmptyState
        description="Lớp học này chưa được liên kết với giáo trình chính thức."
        title="Chưa có giáo trình"
      />
    );
  }
  const units = curriculum.units || [];

  const toggleUnit = (unitId) => {
    setExpandedUnits((prev) => {
      const next = new Set(prev);
      if (next.has(unitId)) {
        next.delete(unitId);
      } else {
        next.add(unitId);
      }
      return next;
    });
  };

  const expandAll = () => {
    setExpandedUnits(new Set(units.map(u => u.id)));
  };

  const collapseAll = () => {
    setExpandedUnits(new Set());
  };

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
          <div className="flex items-center justify-between border-b border-gray-100 pb-2.5">
            <h4 className="text-xs font-extrabold uppercase tracking-widest text-slate-500">
              Chi tiết lộ trình ({units.length} chuyên đề)
            </h4>
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={expandAll}
                className="text-[10px] font-bold text-[#730014] hover:underline"
              >
                Mở rộng tất cả
              </button>
              <span className="text-gray-300">|</span>
              <button
                type="button"
                onClick={collapseAll}
                className="text-[10px] font-bold text-slate-500 hover:underline"
              >
                Thu gọn tất cả
              </button>
            </div>
          </div>

          <div className="space-y-3">
            {units.map((unit) => {
              const isExpanded = expandedUnits.has(unit.id);
              const totalResources = (unit.materials?.length ?? 0) + (unit.exercises?.length ?? 0) + (unit.assessments?.length ?? 0) + (unit.flashcards?.length ?? 0);
              
              return (
                <article
                  key={unit.id}
                  id={`curriculum-unit-${unit.id}`}
                  className={`rounded-2xl border transition-all duration-300 bg-white ${
                    isExpanded ? 'border-[#730014]/30 shadow-md shadow-[#730014]/5' : 'border-gray-200/80 shadow-[0_4px_15px_rgba(0,0,0,0.005)] hover:border-gray-300'
                  }`}
                >
                  {/* Accordion Trigger Header */}
                  <button
                    type="button"
                    onClick={() => toggleUnit(unit.id)}
                    className="flex w-full items-center justify-between p-4.5 text-left select-none outline-none"
                  >
                    <div className="flex items-center gap-3.5 pr-4">
                      {/* Circle Number */}
                      <span className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-extrabold shrink-0 ${
                        isExpanded ? 'bg-[#730014] text-white' : 'bg-[#fff0f1] text-[#730014]'
                      }`}>
                        {String(unit.displayOrder ?? 0).padStart(2, '0')}
                      </span>
                      
                      <div>
                        <h4 className="font-['Manrope'] text-sm font-extrabold text-[#1a1c1c] leading-snug">
                          {unit.title}
                        </h4>
                        {!isExpanded && (
                          <div className="flex items-center gap-2 mt-1 text-[10px] font-bold text-slate-400">
                            <span>{totalResources} học liệu học tập</span>
                            {unit.description && (
                              <>
                                <span>·</span>
                                <span className="line-clamp-1">{unit.description}</span>
                              </>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                    
                    <div className="flex items-center gap-3 shrink-0">
                      {isExpanded ? (
                        <ChevronUp className="h-4.5 w-4.5 text-[#730014]" />
                      ) : (
                        <ChevronDown className="h-4.5 w-4.5 text-slate-400" />
                      )}
                    </div>
                  </button>

                  {/* Accordion Content Block */}
                  {isExpanded && (
                    <div className="border-t border-gray-100 p-5 bg-[#fafafa]/45 rounded-b-2xl space-y-4">
                      {unit.description && (
                        <p className="text-xs text-[#584140] leading-relaxed">
                          {unit.description}
                        </p>
                      )}
                      
                      {unit.sessionPlan && (
                        <div className="text-[11px] whitespace-pre-wrap leading-relaxed text-gray-500 bg-white border border-gray-100 p-4 rounded-xl">
                          <span className="font-bold text-slate-700 block mb-1 text-[10px] uppercase tracking-wider">Kế hoạch buổi học:</span>
                          {unit.sessionPlan}
                        </div>
                      )}
                      
                      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 pt-1">
                        <LearnerRefList
                          title="Học liệu học tập"
                          refs={unit.materials}
                          type="materials"
                          unitId={unit.id}
                        />
                        <LearnerRefList
                          title="Luyện tập trong giáo trình"
                          refs={unit.exercises}
                          type="exercises"
                          unitId={unit.id}
                          onOpenPractice={onOpenPractice}
                        />
                        <LearnerRefList
                          title="Bài đánh giá theo Unit"
                          refs={unit.assessments}
                          type="assessments"
                          unitId={unit.id}
                        />
                        <LearnerRefList
                          title="Flashcards học từ"
                          refs={unit.flashcards}
                          type="flashcards"
                          unitId={unit.id}
                          onOpenFlashcards={onOpenFlashcards}
                        />
                      </div>
                    </div>
                  )}
                </article>
              );
            })}
          </div>
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
function LearnerRefList({
  title,
  refs = [],
  type,
  unitId,
  onOpenPractice,
  onOpenFlashcards,
}) {
  const visibleRefs = refs.filter((ref) => String(ref.status || '').toUpperCase() !== 'ARCHIVED');
  return (
    <div className="rounded-2xl bg-[#fffafb]/75 p-4 space-y-2.5">
      <p className="text-[9px] font-extrabold uppercase tracking-widest text-[#8b706e] border-b border-gray-100 pb-1.5">{title}</p>
      {visibleRefs.length ? (
        <div className="space-y-2">
          {visibleRefs.map((ref) => {
            if (type === 'materials') {
              if (ref.fileUrl) {
                return (
                  <a
                    key={`${ref.type || 'material'}-${ref.id}`}
                    href={ref.fileUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="group block rounded-xl bg-white border border-gray-100 px-3 py-2.5 shadow-[0_2px_8px_rgba(0,0,0,0.015)] hover:border-[#730014]/30 hover:bg-[#fff5f5]/5 transition duration-200 cursor-pointer"
                  >
                    <div className="flex items-start justify-between gap-1">
                      <p className="font-extrabold text-xs text-[#1a1c1c] leading-snug group-hover:text-[#730014] transition-colors">{ref.title}</p>
                      <Download className="h-3.5 w-3.5 text-gray-400 group-hover:text-[#730014] shrink-0 transition-colors" />
                    </div>
                    {ref.subtitle && <p className="mt-1 text-[10px] text-[#8b706e] leading-none">{type === 'assessments' ? formatAssessmentType(ref.subtitle) : ref.subtitle}</p>}
                  </a>
                );
              }
              return (
                <div key={`${ref.type || 'material'}-${ref.id}`} className="rounded-xl bg-white border border-gray-100/50 px-3 py-2.5 shadow-[0_2px_8px_rgba(0,0,0,0.015)] opacity-85">
                  <p className="font-bold text-xs text-slate-500 leading-snug">{ref.title}</p>
                  {ref.subtitle && <p className="mt-1 text-[10px] text-slate-400 leading-none">{type === 'assessments' ? formatAssessmentType(ref.subtitle) : ref.subtitle}</p>}
                </div>
              );
            }

            if (type === 'exercises') {
              return (
                <button
                  key={`${ref.type}-${ref.id}`}
                  type="button"
                  onClick={onOpenPractice}
                  className="w-full text-left group block rounded-xl bg-white border border-gray-100 px-3 py-2.5 shadow-[0_2px_8px_rgba(0,0,0,0.015)] hover:border-[#730014]/30 hover:bg-[#fff5f5]/5 transition duration-200 cursor-pointer"
                >
                  <p className="font-extrabold text-xs text-[#1a1c1c] leading-snug group-hover:text-[#730014] transition-colors">{ref.title}</p>
                  <div className="mt-1.5 flex items-center justify-between gap-1">
                    <span className="inline-flex rounded-full border border-blue-100 bg-blue-50 px-1.5 py-0.5 text-[8px] font-bold uppercase tracking-wider text-blue-700">
                      Luyện tập
                    </span>
                    <Play className="h-3 w-3 text-slate-400 group-hover:text-[#730014] transition-colors" />
                  </div>
                </button>
              );
            }

            if (type === 'flashcards') {
              return (
                <button key={`${ref.type}-${ref.id}`} type="button" onClick={onOpenFlashcards} className="w-full rounded-xl border border-gray-100 bg-white px-3 py-2.5 text-left hover:border-[#730014]/30">
                  <p className="text-xs font-extrabold text-[#1a1c1c]">{ref.title}</p>
                  <span className="mt-1 inline-block text-[9px] font-bold text-[#730014]">Mở flashcard</span>
                </button>
              );
            }

            return (
              <div key={`${ref.type || 'resource'}-${ref.id}`} className="rounded-xl border border-gray-100 bg-white px-3 py-2.5 shadow-[0_2px_8px_rgba(0,0,0,0.015)]">
                <p className="font-bold text-xs text-slate-600 leading-snug">{ref.title}</p>
                <div className="mt-1 flex items-center justify-between">
                  <span className="inline-flex rounded-full border border-gray-100 bg-gray-50 px-1.5 py-0.5 text-[8px] font-bold uppercase tracking-wider text-gray-500">
                    Trong giáo trình
                  </span>
                </div>
              </div>
            );
          })}
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

function EditorModal({ children, onClose }) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-hidden px-3 py-4 sm:px-6" role="dialog" aria-modal="true">
      <button
        aria-label="Đóng modal"
        className="absolute inset-0 bg-[#1a0004]/45 backdrop-blur-sm"
        onClick={onClose}
        type="button"
      />
      <div className="relative z-10 w-full max-w-[640px] pointer-events-auto bg-[#fafafa] rounded-3xl border border-[#dcc0bf]/35 p-6 shadow-2xl overflow-y-auto max-h-[90vh]">
        {children}
      </div>
    </div>
  );
}

function HomeworkSubmissionForm({
  homework,
  attachmentFile,
  textAnswer,
  onAttachmentChange,
  onTextChange,
  onCancel,
  onSubmit,
  submitting,
  canResubmitHomework,
}) {
  const hasSubmission = !!homework.mySubmission;
  const isGraded = hasSubmission && homework.mySubmission.score != null;
  const canSubmit = canResubmitHomework(homework);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3 border-b border-gray-100 pb-3">
        <div>
          <h5 className="font-['Manrope'] text-sm font-extrabold text-[#2b2828]">{homework.title}</h5>
          <p className="mt-1 text-[11px] text-[#584140]">
            Hạn nộp bài: <strong className="text-[#730014]">{formatClassroomDateTime(homework.deadline)}</strong>
          </p>
        </div>
        <button className="rounded-lg p-2 text-[#584140] hover:bg-gray-100" onClick={onCancel} type="button">
          <X className="h-4 w-4" />
        </button>
      </div>

      <div className="space-y-3">
        {homework.instruction && (
          <div className="rounded-xl bg-gray-50/50 p-4 border border-gray-100 text-xs text-[#584140] leading-relaxed">
            <span className="font-bold text-slate-700 block mb-1">Hướng dẫn làm bài:</span>
            {homework.instruction}
          </div>
        )}

        {isGraded && (
          <div className="rounded-xl border border-emerald-100 bg-emerald-50/15 p-4 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-extrabold text-emerald-800 uppercase tracking-widest flex items-center gap-1">
                <Award className="h-4 w-4" />
                Điểm chấm từ giảng viên
              </span>
              <strong className="text-emerald-700 text-xs font-extrabold">{homework.mySubmission.score} / {getHomeworkMaxScore(homework)} điểm</strong>
            </div>
            {getSubmissionFeedback(homework.mySubmission) && (
              <p className="text-xs text-[#584140] italic leading-normal border-t border-emerald-500/10 pt-2 mt-2">
                {getHomeworkFeedbackLabel(homework)}: "{getSubmissionFeedback(homework.mySubmission)}"
              </p>
            )}
          </div>
        )}

        {hasSubmission && homework.mySubmission.attachmentUrl && (
          <div className="rounded-xl bg-slate-50 border border-slate-200 p-3.5 flex items-center justify-between text-xs">
            <span className="font-semibold text-slate-700">Tệp bài làm đã nộp:</span>
            <a
              className="inline-flex items-center gap-1.5 text-xs font-bold text-[#730014] hover:underline"
              href={homework.mySubmission.attachmentUrl}
              rel="noreferrer"
              target="_blank"
            >
              <Download className="h-4 w-4" /> Tải về tệp đã nộp
            </a>
          </div>
        )}

        {canSubmit ? (
          <div className="space-y-4">
            <div className="space-y-2">
              <span className="block text-xs font-extrabold uppercase tracking-[0.16em] text-[#584140]">Đính kèm tệp bài làm</span>
              <div className="flex flex-wrap items-center gap-3">
                <label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-dashed border-[#dfbfbd] bg-[#fffafb] px-4 py-3 text-xs font-extrabold text-[#730014] hover:border-[#730014]">
                  <Paperclip className="h-4 w-4" />
                  Chọn tệp
                  <input
                    accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.zip,.rar,.jpg,.jpeg,.png,.mp3,.m4a,.webm"
                    className="hidden"
                    onChange={(event) => onAttachmentChange(event.target.files?.[0] || null)}
                    type="file"
                  />
                </label>
                <span className="text-xs text-[#8a7a78]">{attachmentFile?.name || 'Chọn tệp bài làm (PDF, Word, Excel, nén,...)'}</span>
              </div>
            </div>

            <div className="space-y-2">
              <span className="block text-xs font-extrabold uppercase tracking-[0.16em] text-[#584140]">Nội dung câu trả lời</span>
              <textarea
                className="min-h-[140px] w-full rounded-2xl border border-gray-200 bg-white px-4 py-3 text-xs text-[#1a1c1c] outline-none transition focus:border-[#730014] leading-relaxed"
                onChange={(e) => onTextChange(e.target.value)}
                placeholder="Nhập nội dung câu trả lời hoặc ghi chú gửi kèm..."
                value={textAnswer}
              />
            </div>
          </div>
        ) : (
          hasSubmission && (
            <div className="space-y-3">
              <div className="space-y-2">
                <span className="block text-xs font-extrabold uppercase tracking-[0.16em] text-slate-500">Nội dung câu trả lời đã nộp:</span>
                <div className="rounded-xl border border-gray-200 bg-gray-50/50 p-4 text-xs text-[#1a1c1c] leading-relaxed whitespace-pre-wrap">
                  {homework.mySubmission.textAnswer || 'Không có nội dung trả lời dạng văn bản.'}
                </div>
              </div>
            </div>
          )
        )}
      </div>

      <div className="mt-5 flex flex-wrap justify-end gap-3 border-t border-gray-100 pt-3">
        <button className="rounded-xl border border-gray-200 px-5 py-3 text-xs font-extrabold text-[#584140] hover:bg-gray-50 active:scale-95" onClick={onCancel} type="button">
          {canSubmit ? 'Hủy' : 'Đóng'}
        </button>
        {canSubmit && (
          <button
            className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white hover:bg-[#730014] disabled:opacity-60 active:scale-95"
            disabled={submitting}
            onClick={onSubmit}
            type="button"
          >
            {submitting ? (
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
        )}
      </div>
    </div>
  );
}
