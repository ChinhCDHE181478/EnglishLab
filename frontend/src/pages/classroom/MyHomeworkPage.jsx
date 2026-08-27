import { useEffect, useMemo, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link, useSearchParams } from 'react-router-dom';
import {
  BookOpen,
  Calendar,
  Clock,
  CheckCircle2,
  AlertCircle,
  Award,
  FileText,
  Send,
  MessageSquare,
  Search,
  Paperclip,
  Upload,
  ArrowRight,
  RefreshCw,
  Bot,
  BookMarked,
  Info,
  X
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import ClassroomFlashcardsPanel from '../../components/classroom/ClassroomFlashcardsPanel';
import WorkspaceFlashcards from '../../components/course-workspace/WorkspaceFlashcards';
import AuthenticatedFileLink from '../../components/classroom/AuthenticatedFileLink';
import HomeworkSubmissionReview, { hasHomeworkTeacherEvaluation } from '../../components/classroom/HomeworkSubmissionReview';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  StatusBadge,
  DetailDrawer,
} from '../../components/classroom/ClassroomUi';
import LearnerPageShell from '../../components/learner/LearnerPageShell';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import BrandedSelect from '../../components/ui/BrandedSelect';
import ListeningExamMode from '../../components/course-assessment/ListeningExamMode';
import ReadingExamMode from '../../components/course-assessment/ReadingExamMode';
import SpeakingExamMode from '../../components/course-assessment/SpeakingExamMode';
import WritingExamMode from '../../components/course-assessment/WritingExamMode';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { requestExamFullscreen } from '../../utils/examFullscreen';
import { stripRichTextToPlain } from '../../utils/lessonRichText';
import { formatClassroomDateTime, getHomeworkMaxScore, getSubmissionFeedback } from '../../utils/classroomHelpers';
import {
  getHomeworkActivityTypeLabel,
  getHomeworkFeedbackLabel,
  getHomeworkGradingHint,
  getHomeworkSkillLabel,
  isAiGradedHomework,
} from '../../utils/homeworkGradingConfig';
import { getStoredUser, hasAccessToken } from '../../utils/auth';

const homeworkTabs = [
  { id: 'all', label: 'Tất cả bài tập' },
  { id: 'online', label: 'Làm trên hệ thống' },
  { id: 'pending', label: 'Chưa nộp bài' },
  { id: 'submitted', label: 'Đã nộp bài' },
  { id: 'graded', label: 'Đã chấm điểm' },
  { id: 'overdue', label: 'Đã quá hạn' },
];

// ─── Custom Minimalist Status configuration ───────────────────────────────────
const getMinimalistStatusInfo = (status) => {
  const map = {
    NOT_SUBMITTED: { text: 'Chưa nộp bài', dotColor: 'bg-amber-500' },
    SUBMITTED: { text: 'Đã nộp bài', dotColor: 'bg-blue-500' },
    GRADED: { text: 'Đã chấm điểm', dotColor: 'bg-emerald-500' },
    OVERDUE: { text: 'Quá hạn nộp', dotColor: 'bg-rose-500 animate-pulse' },
  };
  return map[status] || { text: 'Đang xử lý', dotColor: 'bg-gray-400' };
};

const parseQuizOptions = (value) => {
  try {
    const parsed = JSON.parse(value || '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const normalizeLegacyQuiz = (quiz) => {
  const questions = (quiz.questions || []).map((question, index) => ({
    number: index + 1,
    submissionKey: question.id,
    prompt: question.prompt,
    options: parseQuizOptions(question.optionsJson).map((option) => ({
      value: option,
      label: option,
    })),
  }));

  return {
    id: `legacy-quiz-${quiz.id}`,
    legacyQuizId: quiz.id,
    sourceType: 'LEGACY_QUIZ',
    classroomOfferingId: quiz.classroomOfferingId,
    sessionId: quiz.sessionId,
    title: quiz.title,
    instruction: quiz.description || 'Hoàn thành các câu hỏi trực tiếp trên hệ thống.',
    deadline: quiz.dueAt,
    maxScore: 10,
    allowResubmission: false,
    status: quiz.status || 'OPEN',
    overdue: Boolean(quiz.dueAt && new Date(quiz.dueAt).getTime() < Date.now()),
    activityType: 'SKILL_PRACTICE',
    activityConfigJson: JSON.stringify({ questions }),
    gradingMode: 'TEACHER',
    skill: 'READING',
    createdAt: quiz.createdAt,
    mySubmission: quiz.submitted ? {
      status: 'GRADED',
      score: quiz.myScore,
      textAnswer: 'Đáp án trắc nghiệm đã được hệ thống ghi nhận.',
      submittedAt: null,
    } : null,
  };
};

const containerVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.05
    }
  }
};

const itemVariants = {
  hidden: { opacity: 0, y: 15 },
  show: { opacity: 1, y: 0, transition: { duration: 0.3, ease: 'easeOut' } }
};

export default function MyHomeworkPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [homework, setHomework] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState(() => (
    searchParams.get('type') === 'online-quiz' ? 'online' : 'all'
  ));
  const [searchQuery, setSearchQuery] = useState('');
  const [submittingId, setSubmittingId] = useState(null);
  const [submitAnswers, setSubmitAnswers] = useState({});
  const [submitFiles, setSubmitFiles] = useState({});
  const [actionMessage, setActionMessage] = useState('');
  const [selectedHomework, setSelectedSession] = useState(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [examHomework, setExamHomework] = useState(null);
  const [examError, setExamError] = useState('');
  const [confirmHomework, setConfirmHomework] = useState(null);
  const [flashcardWorkspace, setFlashcardWorkspace] = useState(null);
  const [flashcardLoadingId, setFlashcardLoadingId] = useState(null);

  const isAuthenticated = Boolean(hasAccessToken() && getStoredUser());

  const loadHomework = async () => {
    setLoading(true);
    setError('');
    try {
      const homeworkData = await classroomApi.getMyHomework();
      let legacyQuizzes = [];
      try {
        legacyQuizzes = await classroomApi.listStudentQuizzes();
      } catch {
        // Homework remains usable if the legacy quiz endpoint is unavailable.
      }
      setHomework([
        ...homeworkData.map((item) => ({ ...item, sourceType: 'HOMEWORK' })),
        ...legacyQuizzes.map(normalizeLegacyQuiz),
      ]);
    } catch (err) {
      setHomework([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải danh sách bài tập.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!isAuthenticated) {
      setLoading(false);
      return;
    }
    loadHomework();
  }, [isAuthenticated]);

  const openFlashcardWorkspace = async (item) => {
    setFlashcardLoadingId(item.id);
    setActionMessage('');
    try {
      const classroom = await classroomApi.getMyClassroom(item.classroomOfferingId);
      setFlashcardWorkspace({
        homework: item,
        curriculum: classroom?.instructorLedCourse,
      });
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể tải bộ flashcard của lớp học.'));
    } finally {
      setFlashcardLoadingId(null);
    }
  };

  useEffect(() => {
    const requestedHomeworkId = searchParams.get('open');
    if (!requestedHomeworkId || !homework.length || examHomework || confirmHomework || flashcardWorkspace) return;
    const requestedHomework = homework.find((item) => String(item.id) === requestedHomeworkId);
    if (!requestedHomework || !usesStructuredWorkspace(requestedHomework)) return;

    const nextParams = new URLSearchParams(searchParams);
    nextParams.delete('open');
    setSearchParams(nextParams, { replace: true });

    if (requestedHomework.activityType === 'FLASHCARD_REVIEW') {
      openFlashcardWorkspace(requestedHomework);
      return;
    }

    setExamError('');
    setConfirmHomework(requestedHomework);
  }, [confirmHomework, examHomework, flashcardWorkspace, homework, searchParams, setSearchParams]);

  const getHomeworkStatus = (item) => {
    if (item.mySubmission) {
      if (item.mySubmission.status === 'GRADED' || item.mySubmission.score != null) return 'GRADED';
      return 'SUBMITTED';
    }
    return item.overdue ? 'OVERDUE' : 'NOT_SUBMITTED';
  };

  const canResubmitHomework = (item) => {
    if (item?.sourceType === 'LEGACY_QUIZ') return false;
    if (!item || item.status !== 'OPEN') return false;
    if (!item.mySubmission) return true;
    if (item.mySubmission.status === 'SUBMITTED') return true;
    return Boolean(item.allowResubmission);
  };

  const filteredHomework = useMemo(() => {
    return homework.filter((item) => {
      const status = getHomeworkStatus(item);
      const matchesSearch =
        item.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.instruction?.toLowerCase().includes(searchQuery.toLowerCase());

      if (!matchesSearch) return false;

      if (activeTab === 'pending') return status === 'NOT_SUBMITTED';
      if (activeTab === 'online') return usesStructuredWorkspace(item);
      if (activeTab === 'submitted') return status === 'SUBMITTED';
      if (activeTab === 'graded') return status === 'GRADED';
      if (activeTab === 'overdue') return status === 'OVERDUE';
      return true;
    });
  }, [homework, activeTab, searchQuery]);

  const counts = useMemo(() => {
    return {
      all: homework.length,
      online: homework.filter(usesStructuredWorkspace).length,
      pending: homework.filter((h) => getHomeworkStatus(h) === 'NOT_SUBMITTED').length,
      submitted: homework.filter((h) => getHomeworkStatus(h) === 'SUBMITTED').length,
      graded: homework.filter((h) => getHomeworkStatus(h) === 'GRADED').length,
      overdue: homework.filter((h) => getHomeworkStatus(h) === 'OVERDUE').length,
    };
  }, [homework]);

  const homeworkTabOptions = useMemo(() => (
    homeworkTabs.map((tab) => ({
      label: `${tab.label} (${counts[tab.id] ?? counts.all})`,
      value: tab.id,
    }))
  ), [counts]);

  const { page, setPage, totalPages, pageItems: paginatedHomeworkList, totalItems } = usePagination(
    filteredHomework,
    6,
    `my-homework-${activeTab}-${searchQuery}`
  );

  const handleSubmit = async (item) => {
    const homeworkId = item.id;
    const file = submitFiles[homeworkId] || null;
    setSubmittingId(homeworkId);
    setActionMessage('');
    try {
      const textAnswer = submitAnswers[homeworkId] || '';
      if (item.sourceType === 'LEGACY_QUIZ') {
        const structured = readStructuredAnswer(textAnswer);
        await classroomApi.submitStudentQuiz(item.legacyQuizId, JSON.stringify(structured.responses || {}));
        setActionMessage('Đã nộp bài làm trên hệ thống và cập nhật điểm vào bảng điểm lớp.');
      } else {
        let attachmentUrl = '';
        if (file) {
          const uploaded = await classroomApi.uploadHomeworkSubmissionAttachment(homeworkId, file);
          attachmentUrl = uploaded.url;
        }
        await classroomApi.submitHomework(homeworkId, { textAnswer, attachmentUrl });
        setActionMessage('Nộp bài tập thành công.');
      }
      await loadHomework();
      setIsDrawerOpen(false);
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể nộp bài tập.'));
    } finally {
      setSubmittingId(null);
    }
  };

  const handleOpenHomework = async (item) => {
    if (item.activityType === 'FLASHCARD_REVIEW') {
      await openFlashcardWorkspace(item);
      return;
    }
    if (usesModuleExamWorkspace(item) && (!item.mySubmission || canResubmitHomework(item))) {
      setExamError('');
      setConfirmHomework(item);
      return;
    }
    setSelectedSession(item);
    setIsDrawerOpen(true);
  };

  const handleCompleteFlashcardHomework = async () => {
    const item = flashcardWorkspace?.homework;
    if (!item || item.mySubmission && !canResubmitHomework(item)) return;

    setSubmittingId(item.id);
    setActionMessage('');
    try {
      await classroomApi.submitHomework(item.id, {
        textAnswer: JSON.stringify({
          activity: 'FLASHCARD_REVIEW',
          completed: true,
          completedAt: new Date().toISOString(),
          curriculumUnitId: item.curriculumUnitId,
        }),
        attachmentUrl: '',
      });
      setActionMessage('Đã ghi nhận hoàn thành ôn tập flashcard.');
      setFlashcardWorkspace(null);
      await loadHomework();
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể ghi nhận tiến độ ôn flashcard.'));
    } finally {
      setSubmittingId(null);
    }
  };

  const handleConfirmStartExam = async () => {
    if (confirmHomework) {
      const fullscreenStarted = await requestExamFullscreen();
      if (!fullscreenStarted) {
        setExamError('Không thể bật chế độ toàn màn hình. Hãy cho phép trình duyệt mở toàn màn hình rồi thử lại.');
        return;
      }
      setExamHomework(confirmHomework);
      setConfirmHomework(null);
    }
  };

  const handleExamSubmit = async (payload) => {
    if (!examHomework) return;
    setSubmittingId(examHomework.id);
    setExamError('');
    try {
      const objective = parseObjectiveExamPayload(payload?.objectiveAnswersJson);
      if (examHomework.sourceType === 'LEGACY_QUIZ') {
        const legacyQuestions = getActivityQuestions(parseActivityConfig(examHomework.activityConfigJson));
        const legacyAnswers = Object.fromEntries(legacyQuestions.map((question) => [
          String(question.submissionKey || question.number),
          objective.responses[String(question.number)] || '',
        ]));
        await classroomApi.submitStudentQuiz(examHomework.legacyQuizId, JSON.stringify(legacyAnswers));
      } else {
        const textAnswer = payload?.submittedText || JSON.stringify(objective, null, 2);
        await classroomApi.submitHomework(examHomework.id, {
          textAnswer,
          attachmentUrl: payload?.submittedAudioUrl || '',
        });
      }
      setActionMessage('Đã nộp bài tập thành công.');
      setExamHomework(null);
      await loadHomework();
    } catch (err) {
      setExamError(getClassroomErrorMessage(err, 'Không thể nộp bài tập. Bài làm vẫn đang được giữ.'));
      throw err;
    } finally {
      setSubmittingId(null);
    }
  };

  return (
    <>
    <LearnerPageShell
      description="Một nơi duy nhất cho bài soạn trên hệ thống, bài giao bằng file và hoạt động ôn tập theo unit."
      title="Bài tập của tôi"
    >
      {!isAuthenticated ? (
        <div className="flex flex-1 flex-col items-center justify-center py-16">
          <ClassroomEmptyState
            actionLabel="Đăng nhập ngay"
            actionTo="/login"
            description="Bạn cần đăng nhập để xem danh sách bài tập được giao."
            title="Yêu cầu đăng nhập"
          />
        </div>
      ) : loading ? (
        <div className="space-y-6 flex-1">
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-24 w-full animate-pulse rounded-[24px] border border-gray-100 bg-white/60 p-4" />
            ))}
          </div>
          <div className="h-12 w-full animate-pulse rounded-[24px] bg-gray-100"></div>
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-72 w-full animate-pulse rounded-[24px] border border-gray-100 bg-white p-6" />
            ))}
          </div>
        </div>
      ) : error ? (
        <div className="flex flex-1 flex-col items-center justify-center py-12">
          <ClassroomErrorState message={error} onRetry={loadHomework} />
        </div>
      ) : homework.length === 0 ? (
        <div className="flex flex-1 flex-col items-center justify-center py-16">
          <ClassroomEmptyState
            actionLabel="Vào lớp của tôi"
            actionTo="/my-classrooms"
            description="Tuyệt vời! Hiện tại bạn không có bài tập nào cần hoàn thành hoặc chưa được giao bài tập."
            title="Không có bài tập nào"
          />
        </div>
      ) : (
        <div className="space-y-8 flex-1">
          {/* Flat Counter Cards */}
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <GlassCounterCard label="Chưa hoàn thành" value={counts.pending} dotColor="bg-amber-500" icon={<Clock className="h-5 w-5" />} />
            <GlassCounterCard label="Đã gửi bài nộp" value={counts.submitted} dotColor="bg-blue-500" icon={<CheckCircle2 className="h-5 w-5" />} />
            <GlassCounterCard label="Đã chấm điểm" value={counts.graded} dotColor="bg-emerald-500" icon={<Award className="h-5 w-5" />} />
            <GlassCounterCard label="Bị quá hạn nộp" value={counts.overdue} dotColor="bg-rose-500" icon={<AlertCircle className="h-5 w-5" />} />
          </div>

          <div className="space-y-6">
            {/* Filter and Search Layout in single premium bar */}
            <section className="grid gap-3 rounded-[24px] border border-[#ead9db]/85 bg-white p-4 shadow-[0_8px_30px_rgba(75,0,9,0.015)] lg:grid-cols-[1fr_280px_auto]">
              <label className="relative block">
                <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[#c2acab]" />
                <input
                  className="w-full rounded-2xl border border-[#dfbfbd]/50 bg-[#fffdfd] py-3 pl-11 pr-4 text-sm outline-none transition focus:border-[#730014] focus:bg-white focus:ring-4 focus:ring-[#730014]/5"
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Tìm kiếm bài tập..."
                  value={searchQuery}
                />
              </label>
              <BrandedSelect
                buttonClassName="h-full rounded-2xl border-[#dfbfbd]/50 bg-[#fffdfd]"
                onChange={(event) => setActiveTab(event.target.value)}
                options={homeworkTabOptions}
                value={activeTab}
              />
              <button
                aria-label="Tải lại"
                className="inline-flex items-center justify-center gap-2 rounded-2xl border border-[#dfbfbd] bg-white px-5 py-3 text-sm font-extrabold text-[#730014] shadow-sm transition hover:bg-[#fff2f3] active:scale-95"
                onClick={loadHomework}
                type="button"
              >
                <RefreshCw className="h-4 w-4" /> Tải lại
              </button>
            </section>

            {/* Notification alert */}
            {actionMessage && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                className={`rounded-xl border p-4 text-xs font-semibold flex items-center gap-2 ${
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
              </motion.div>
            )}

            {/* Premium Homework Card Grid */}
            {/* Premium Homework Card Grid */}
            <AnimatePresence mode="wait">
              {filteredHomework.length > 0 ? (
                <>
                  <motion.div
                  key={activeTab}
                  variants={containerVariants}
                  initial="hidden"
                  animate="show"
                  className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
                >
                  {paginatedHomeworkList.map((item) => {
                    const status = getHomeworkStatus(item);
                    const isGraded = status === 'GRADED';
                    const isOverdue = status === 'OVERDUE';
                    const hasSubmission = !!item.mySubmission;
                    const hasTeacherEvaluation = hasHomeworkTeacherEvaluation(item.mySubmission);

                    const isUrgent = !hasSubmission && !isOverdue && new Date(item.deadline) - new Date() < 24 * 60 * 60 * 1000;
                    const statusInfo = getMinimalistStatusInfo(status);

                    return (
                      <motion.article
                        key={item.id}
                        variants={itemVariants}
                        className={`relative overflow-hidden rounded-[28px] border bg-white shadow-[0_10px_35px_rgba(0,0,0,0.02)] transition-all duration-300 hover:shadow-[0_20px_50px_rgba(115,0,20,0.07)] hover:border-[#730014]/30 hover:-translate-y-1.5 flex flex-col justify-between h-full group ${
                          isUrgent ? 'border-amber-300 ring-2 ring-amber-300/10' : 'border-gray-200/80'
                        }`}
                      >
                        
                        <div className="p-6 space-y-4">
                          {/* Card Header Row */}
                          <div className="flex items-center justify-between w-full">
                            {/* Status Indicator Dot */}
                            <div className="flex items-center gap-2">
                              <span className={`h-2 w-2 rounded-full ${statusInfo.dotColor}`} />
                              <span className="text-[10px] font-extrabold uppercase tracking-wider text-gray-500">
                                {statusInfo.text}
                              </span>
                            </div>

                            {/* Skill Badge */}
                            <span className="inline-flex items-center gap-1 rounded-full bg-[#fff0f1] border border-[#dfbfbd]/45 px-2.5 py-0.5 text-[9px] font-extrabold uppercase tracking-widest text-[#730014]">
                              {getHomeworkSkillLabel(item.skill)}
                            </span>
                          </div>

                          {/* Title block */}
                          <div className="space-y-1">
                            <span className="text-[9px] font-bold text-gray-400 uppercase tracking-widest block">Lớp: #{item.classroomOfferingId}</span>
                            <h3 className="font-['Manrope'] text-base font-extrabold text-[#1a1c1c] leading-snug group-hover:text-[#730014] transition-colors duration-300 line-clamp-2">
                              {item.title}
                            </h3>
                          </div>

                          {/* Skill/Activity Tags */}
                          <div className="flex flex-wrap gap-1.5 pt-1">
                            {isAiGradedHomework(item) && (
                              <span className="inline-flex items-center gap-1 rounded-full bg-[#fff5f5] px-2.5 py-0.5 text-[9px] font-bold text-[#8a0018] border border-[#dfbfbd]/40">
                                <Bot className="h-3 w-3" />
                                AI Review
                              </span>
                            )}
                            {item.curriculumUnitTitle && (
                              <span className="rounded-full bg-gray-50 border border-gray-200/70 px-2.5 py-0.5 text-[9px] font-bold text-gray-600">
                                Unit: {item.curriculumUnitTitle}
                              </span>
                            )}
                            <span className="rounded-full bg-gray-50 border border-gray-200/70 px-2.5 py-0.5 text-[9px] font-bold text-gray-600">
                              {getHomeworkActivityTypeLabel(item.activityType)}
                            </span>
                          </div>

                          {/* Instruction text */}
                          <p className="text-xs text-[#584140] line-clamp-2 leading-relaxed pt-1">
                            {item.instruction || 'Không có mô tả hướng dẫn chi tiết.'}
                          </p>

                          {/* Info block details */}
                          <div className="space-y-3 pt-3 border-t border-gray-50">
                            {item.attachmentUrl && (
                              <AuthenticatedFileLink
                                className="inline-flex items-center gap-1.5 text-xs font-bold text-[#730014] hover:underline"
                                url={item.attachmentUrl}
                              >
                                <Paperclip className="h-3.5 w-3.5 text-[#730014]" />
                                Đề bài đính kèm
                              </AuthenticatedFileLink>
                            )}

                            <div className="flex items-center justify-between text-xs text-[#8b706e]">
                              <span className="flex items-center gap-1.5">
                                <Calendar className="h-4 w-4 text-[#730014]" />
                                Hạn nộp: <strong className="text-[#584140] font-semibold">{formatClassroomDateTime(item.deadline)}</strong>
                              </span>
                            </div>

                            {/* Urgent deadline warning badge */}
                            {isUrgent && (
                              <div className="rounded-xl bg-amber-50/70 border border-amber-100 p-2.5 text-[10px] font-semibold text-amber-800 flex items-center gap-1.5">
                                <Clock className="h-3.5 w-3.5 text-amber-600 animate-pulse" />
                                Nộp gấp! (Còn lại dưới 24 giờ)
                              </div>
                            )}

                            {/* Score Display bar for graded submissions */}
                            {isGraded && (
                              <div className="rounded-2xl border border-emerald-150 bg-emerald-50/15 p-3 flex items-center justify-between">
                                <div className="flex items-center gap-1.5">
                                  <Award className="h-4 w-4 text-emerald-600 shrink-0" />
                                  <span className="text-[10px] font-extrabold uppercase tracking-widest text-[#8b706e]">Kết quả điểm</span>
                                </div>
                                <span className="text-xs font-extrabold text-emerald-700">
                                  {item.mySubmission.score} / {getHomeworkMaxScore(item)} điểm
                                </span>
                              </div>
                            )}

                            {/* Teacher Feedback testimonial block */}
                            {hasTeacherEvaluation && getSubmissionFeedback(item.mySubmission) && (
                              <div className="rounded-2xl bg-[#fff0f1]/30 border border-[#dfbfbd]/20 p-3 space-y-1">
                                <p className="text-[9px] font-extrabold text-[#730014] uppercase tracking-wider flex items-center gap-1">
                                  <MessageSquare className="h-3 w-3" />
                                  {getHomeworkFeedbackLabel(item)}
                                </p>
                                <p className="text-[11px] text-[#584140] italic leading-normal line-clamp-2">
                                  "{stripRichTextToPlain(getSubmissionFeedback(item.mySubmission))}"
                                </p>
                              </div>
                            )}
                          </div>

                        </div>

                        {/* Card Footer */}
                        <div className="border-t border-gray-50 bg-gray-50/40 px-6 py-4 flex items-center justify-between mt-auto">
                          <Link
                            className="text-xs font-bold text-gray-600 hover:text-[#730014] underline"
                            to={`/my-classrooms/${item.classroomOfferingId}`}
                          >
                            Vào lớp học
                          </Link>

                          {!hasSubmission ? (
                            <button
                              className="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-[#730014] to-[#4b0009] px-4 py-2.5 text-xs font-bold text-white shadow-sm transition hover:shadow active:scale-95 btn-hover"
                              disabled={flashcardLoadingId === item.id}
                              onClick={() => handleOpenHomework(item)}
                              type="button"
                            >
                              {flashcardLoadingId === item.id ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <Send className="h-3.5 w-3.5" />}
                              {item.activityType === 'FLASHCARD_REVIEW'
                                ? 'Học flashcard'
                                : usesModuleExamWorkspace(item) ? 'Vào làm bài' : 'Làm & nộp bài'}
                            </button>
                          ) : canResubmitHomework(item) ? (
                            <div className="flex flex-wrap justify-end gap-2">
                              {hasTeacherEvaluation ? (
                                <button
                                  className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-3 py-2.5 text-xs font-bold text-gray-700 transition hover:bg-gray-50 active:scale-95"
                                  onClick={() => {
                                    setSelectedSession(item);
                                    setIsDrawerOpen(true);
                                  }}
                                  type="button"
                                >
                                  <MessageSquare className="h-3.5 w-3.5 text-[#730014]" />
                                  Xem đánh giá
                                </button>
                              ) : null}
                              <button
                                className="inline-flex items-center gap-1.5 rounded-xl border border-[#730014]/20 bg-white px-3 py-2.5 text-xs font-bold text-[#730014] transition hover:bg-[#fff0f1] active:scale-95"
                                disabled={flashcardLoadingId === item.id}
                                onClick={() => handleOpenHomework(item)}
                                type="button"
                              >
                                {flashcardLoadingId === item.id ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <Send className="h-3.5 w-3.5" />}
                                {item.activityType === 'FLASHCARD_REVIEW' ? 'Ôn lại flashcard' : 'Nộp lại bài'}
                              </button>
                            </div>
                          ) : (
                            <button
                              className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-xs font-bold text-gray-700 transition hover:bg-gray-50 active:scale-95"
                              onClick={() => handleOpenHomework(item)}
                              type="button"
                            >
                              {item.activityType === 'FLASHCARD_REVIEW' ? <BookMarked className="h-3.5 w-3.5" /> : <FileText className="h-3.5 w-3.5" />}
                              {item.activityType === 'FLASHCARD_REVIEW' ? 'Ôn lại flashcard' : 'Xem bài nộp'}
                            </button>
                          )}
                        </div>
                      </motion.article>
                    );
                  })}
                </motion.div>

                {filteredHomework.length > 6 && (
                  <div className="mt-6 flex justify-end">
                    <Pagination
                      page={page}
                      totalPages={totalPages}
                      onChange={setPage}
                      totalItems={totalItems}
                      pageSize={6}
                    />
                  </div>
                )}
                </>
              ) : (
                <motion.div
                  key="empty"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                  className="py-12"
                >
                  <ClassroomEmptyState
                    actionLabel="Xem các lớp"
                    actionTo="/my-classrooms"
                    description="Hiện tại không tìm thấy bài tập nào thuộc bộ lọc bạn đã chọn."
                    title="Không có bài tập nào"
                  />
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
      )}

      {/* Detail / Submit Drawer */}
      <DetailDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        title={
          selectedHomework?.mySubmission && !canResubmitHomework(selectedHomework)
            ? 'Đánh giá bài nộp'
            : selectedHomework?.mySubmission
              ? 'Đánh giá & nộp lại'
              : 'Gửi bài làm'
        }
      >
        {selectedHomework && (
          <div className="space-y-6">
            
            <div className="space-y-2">
              <span className="text-[10px] font-extrabold uppercase tracking-widest text-[#730014]">
                Lớp học: #{selectedHomework.classroomOfferingId}
              </span>
              <h3 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c] leading-tight">
                {selectedHomework.title}
              </h3>
              
              <div className="flex flex-wrap items-center gap-2 pt-1">
                <StatusBadge status={getHomeworkStatus(selectedHomework)} />
                <span className="text-xs text-[#8b706e] flex items-center gap-1.5">
                  <Calendar className="h-4 w-4" />
                  Hạn nộp: {formatClassroomDateTime(selectedHomework.deadline)}
                </span>
              </div>
            </div>

            {getHomeworkGradingHint(selectedHomework) && (
              <div className="rounded-xl border border-purple-100 bg-[#fff5f5] p-4 text-xs leading-relaxed text-[#8a0018] flex items-start gap-2 border-[#dfbfbd]/55">
                <Bot className="h-4 w-4 text-[#730014] shrink-0 mt-0.5" />
                <p>{getHomeworkGradingHint(selectedHomework)}</p>
              </div>
            )}

            <div className="rounded-2xl border border-gray-100 bg-gray-50/50 p-5 space-y-3">
              <h4 className="text-xs font-extrabold text-[#8b706e] uppercase tracking-wider">Mô tả bài tập & Hướng dẫn</h4>
              <p className="text-xs text-[#584140] whitespace-pre-wrap leading-relaxed">
                {selectedHomework.instruction || 'Không có mô tả hướng dẫn chi tiết.'}
              </p>
              
              {selectedHomework.attachmentUrl && (
                <AuthenticatedFileLink
                  className="inline-flex items-center gap-1.5 text-xs font-bold text-[#730014] hover:underline"
                  url={selectedHomework.attachmentUrl}
                >
                  <Paperclip className="h-4 w-4" />
                  Tải file đề bài đính kèm
                </AuthenticatedFileLink>
              )}
            </div>

            {/* Answer Display or Submission Workspace Form */}
            {selectedHomework.mySubmission && !canResubmitHomework(selectedHomework) ? (
              <HomeworkSubmissionReview homework={selectedHomework} submission={selectedHomework.mySubmission} />
            ) : (
              <div className="space-y-5">
                {selectedHomework.mySubmission ? (
                  <HomeworkSubmissionReview homework={selectedHomework} submission={selectedHomework.mySubmission} />
                ) : null}
                <h4 className="text-xs font-extrabold text-[#8b706e] uppercase tracking-wider flex items-center gap-1">
                  <FileText className="h-4 w-4 text-[#730014]" />
                  Chi tiết nội dung câu trả lời
                </h4>

                {/* Text response area */}
                {!usesStructuredWorkspace(selectedHomework) && (
                  <textarea
                    className="min-h-[160px] w-full rounded-2xl border border-gray-200 bg-gray-50/20 px-4 py-3 text-xs text-[#1a1c1c] outline-none transition focus:border-[#730014] focus:bg-white focus:ring-2 focus:ring-[#730014]/5 leading-relaxed"
                    onChange={(e) => setSubmitAnswers((curr) => ({ ...curr, [selectedHomework.id]: e.target.value }))}
                    placeholder="Nhập nội dung câu trả lời hoặc câu văn bài làm tại đây..."
                    value={submitAnswers[selectedHomework.id] ?? selectedHomework.mySubmission?.textAnswer ?? ''}
                  />
                )}

                {/* Quiz answer workspace sheet */}
                <HomeworkAnswerWorkspace
                  homework={selectedHomework}
                  onChange={(value) => setSubmitAnswers((curr) => ({ ...curr, [selectedHomework.id]: value }))}
                  value={submitAnswers[selectedHomework.id] ?? selectedHomework.mySubmission?.textAnswer ?? ''}
                />

                {/* Custom File Upload Dropzone input */}
                {selectedHomework.sourceType !== 'LEGACY_QUIZ' ? <div className="space-y-2">
                  <span className="text-[11px] font-extrabold uppercase tracking-widest text-[#8b706e]">Tệp đính kèm bài làm</span>
                  
                  <label className="group block cursor-pointer rounded-2xl border-2 border-dashed border-[#dfbfbd] bg-gray-50/10 p-6 text-center transition hover:border-[#730014] hover:bg-[#fff0f1]/10">
                    <input
                      accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.zip,.rar,.jpg,.jpeg,.png,.mp3,.m4a,.webm"
                      className="sr-only"
                      onChange={(event) => setSubmitFiles((curr) => ({ ...curr, [selectedHomework.id]: event.target.files?.[0] || null }))}
                      type="file"
                    />
                    <div className="flex flex-col items-center justify-center gap-2">
                      <div className="rounded-2xl bg-gray-50 group-hover:bg-[#fff0f1] p-3 text-gray-500 group-hover:text-[#730014] transition-colors duration-300">
                        <Upload className="h-5 w-5" />
                      </div>
                      <span className="text-xs font-bold text-[#1a1c1c]">Chọn tệp tin gửi kèm</span>
                      <span className="text-[10px] text-gray-400 max-w-xs leading-normal">
                        Hỗ trợ PDF, Word, Excel, PowerPoint, ZIP, hình ảnh hoặc các định dạng âm thanh/video
                      </span>

                      {submitFiles[selectedHomework.id] ? (
                        <div className="mt-2.5 rounded-xl bg-[#fff0f1] border border-[#dfbfbd] px-3 py-1.5 text-xs font-bold text-[#730014] flex items-center gap-1.5 justify-center">
                          <CheckCircle2 className="h-4 w-4 text-[#730014]" />
                          {submitFiles[selectedHomework.id].name}
                        </div>
                      ) : selectedHomework.mySubmission?.attachmentUrl ? (
                        <div className="mt-2.5 rounded-xl bg-gray-50 border border-gray-200 px-3 py-1.5 text-xs font-bold text-gray-600 flex items-center gap-1.5 justify-center">
                          <Paperclip className="h-4 w-4" />
                          Giữ nguyên file cũ (Không cần chọn lại trừ khi muốn thay đổi)
                        </div>
                      ) : (
                        <span className="text-[10px] text-gray-400 italic">Có thể bỏ trống nếu chỉ gửi đáp án văn bản</span>
                      )}
                    </div>
                  </label>
                </div> : null}

                {/* Action submit button */}
                <div className="pt-4">
                  <button
                    className="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-[#730014] to-[#4b0009] py-3.5 text-xs font-bold text-white shadow-sm transition hover:shadow active:scale-95 btn-hover"
                    disabled={submittingId === selectedHomework.id}
                    onClick={() => handleSubmit(selectedHomework)}
                    type="button"
                  >
                    {submittingId === selectedHomework.id ? (
                      <>
                        <RefreshCw className="h-4 w-4 animate-spin" />
                        Đang nộp bài nộp...
                      </>
                    ) : (
                      <>
                        <Send className="h-4 w-4" />
                        {selectedHomework.mySubmission ? 'Cập nhật bài làm mới' : 'Nộp bài tập'}
                      </>
                    )}
                  </button>
                </div>

              </div>
            )}

          </div>
        )}
      </DetailDrawer>
    </LearnerPageShell>
    {flashcardWorkspace ? (
      <FlashcardHomeworkWorkspace
        canComplete={!flashcardWorkspace.homework.mySubmission || canResubmitHomework(flashcardWorkspace.homework)}
        curriculum={flashcardWorkspace.curriculum}
        homework={flashcardWorkspace.homework}
        onClose={() => setFlashcardWorkspace(null)}
        onComplete={handleCompleteFlashcardHomework}
        submitting={submittingId === flashcardWorkspace.homework.id}
      />
    ) : null}
    {examHomework ? (
      <HomeworkModuleExam
        homework={examHomework}
        onClose={() => {
          setExamError('');
          setExamHomework(null);
        }}
        onSubmit={handleExamSubmit}
        submitting={submittingId === examHomework.id}
      />
    ) : null}
    {confirmHomework ? (
      <HomeworkConfirmModal
        homework={confirmHomework}
        onClose={() => setConfirmHomework(null)}
        onConfirm={handleConfirmStartExam}
      />
    ) : null}
    {examError ? (
      <div className="fixed bottom-5 left-1/2 z-[160] w-[min(92vw,680px)] -translate-x-1/2 rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm font-bold text-red-700 shadow-xl">
        {examError}
      </div>
    ) : null}
    </>
  );
}

// ─── Glass stats counter component ─────────────────────────────────────────────
function GlassCounterCard({ label, value, dotColor, icon }) {
  return (
    <div className="relative overflow-hidden rounded-[24px] border border-gray-200/80 bg-white p-5 shadow-[0_10px_30px_rgba(0,0,0,0.02)] transition-all duration-300 hover:border-[#dfbfbd]/60 hover:shadow-[0_15px_35px_rgba(75,0,9,0.04)] hover:-translate-y-0.5 flex items-center justify-between group">
      <div className="flex items-center gap-3">
        <div className="rounded-xl p-2.5 shrink-0 bg-[#fff0f1] text-[#730014]">
          {icon}
        </div>
        <div>
          <span className="text-[10px] font-extrabold uppercase tracking-widest text-[#8b706e]">{label}</span>
          <p className="font-['Manrope'] text-2xl font-extrabold text-[#1a1c1c] mt-0.5">{value}</p>
        </div>
      </div>
      <span className={`h-2 w-2 rounded-full ${dotColor} opacity-70 group-hover:opacity-100 group-hover:scale-125 transition-all duration-300`} />
    </div>
  );
}

export function HomeworkModuleExam({ homework, onClose, onSubmit, submitting }) {
  const skill = String(homework?.skill || 'READING').toUpperCase();
  const objectiveConfig = buildHomeworkObjectiveConfig(homework);
  const hasObjectiveQuestions = getActivityQuestions(parseActivityConfig(homework?.activityConfigJson)).length > 0;
  const initialResponses = readStructuredAnswer(homework?.mySubmission?.textAnswer).responses;
  const assessment = {
    title: homework.title,
    timeLimitMinutes: objectiveConfig.durationMinutes,
  };

  if (hasObjectiveQuestions && skill === 'LISTENING') {
    return (
      <ListeningExamMode
        assessment={assessment}
        config={objectiveConfig}
        initialAnswers={initialResponses}
        onClose={onClose}
        onSubmit={onSubmit}
        skipAudioCheck
        submitLabel="Nộp bài tập"
        submitting={submitting}
      />
    );
  }

  if (hasObjectiveQuestions) {
    return (
      <ReadingExamMode
        assessment={assessment}
        config={objectiveConfig}
        initialAnswers={initialResponses}
        onClose={onClose}
        onSubmit={onSubmit}
        submitLabel="Nộp bài tập"
        submitting={submitting}
      />
    );
  }

  if (skill === 'SPEAKING') {
    return (
      <SpeakingExamMode
        config={buildHomeworkSpeakingConfig(homework)}
        initialAudioUrl={homework?.mySubmission?.attachmentUrl || ''}
        onClose={onClose}
        onSubmit={onSubmit}
        submitting={submitting}
        uploadAudio={(file) => classroomApi.uploadHomeworkSubmissionAttachment(homework?.id, file)}
      />
    );
  }

  return (
    <WritingExamMode
      assessment={assessment}
      config={buildHomeworkWritingConfig(homework)}
      initialSubmissionText={homework?.mySubmission?.textAnswer || ''}
      onClose={onClose}
      onSubmit={onSubmit}
      submitLabel="Nộp bài tập"
      submitting={submitting}
    />
  );
}

// ─── Answer Sheet Workspace subcomponent ─────────────────────────────────────────
function HomeworkAnswerWorkspace({ homework, value, onChange }) {
  const config = parseActivityConfig(homework?.activityConfigJson);
  const questions = getActivityQuestions(config);
  const activityType = homework?.activityType || 'TEXT_RESPONSE';
  const structured = readStructuredAnswer(value);

  const updateAnswer = (key, answer) => {
    const next = {
      ...structured,
      responses: {
        ...(structured.responses || {}),
        [key]: answer,
      },
    };
    onChange(JSON.stringify(next, null, 2));
  };

  if (activityType === 'FLASHCARD_REVIEW') {
    const customFlashcards = (Array.isArray(config.flashcards) ? config.flashcards : [])
      .filter((card) => card && typeof card === 'object');
    const flashcardSetIds = config.flashcardSetIds || [];
    return (
      <div className="rounded-xl border border-emerald-100 bg-[#fff5f5] p-4 text-xs leading-relaxed text-[#8a0018] border-[#dfbfbd]/50">
        <p className="font-extrabold flex items-center gap-1.5 text-[#730014]">
          <BookOpen className="h-4 w-4" />
          Yêu cầu học từ vựng Flashcard
        </p>
        <p className="mt-1">
          {homework.curriculumUnitTitle ? `Học phần: ${homework.curriculumUnitTitle}. ` : ''}
          {customFlashcards.length
            ? `Bộ thẻ gồm ${customFlashcards.length} flashcard do giáo viên biên soạn.`
            : flashcardSetIds.length
              ? `Bộ flashcard cần ôn: ${flashcardSetIds.join(', ')}.`
              : 'Vui lòng truy cập thư viện từ vựng trong lớp học để hoàn thành ôn tập flashcard cho unit này.'}
        </p>
      </div>
    );
  }

  if (!['SKILL_PRACTICE', 'MIXED'].includes(activityType) || !questions.length) {
    return null;
  }

  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-100 pb-3">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-wider text-[#8b706e]">Answer Sheet (Phiếu trả lời)</p>
          <p className="mt-0.5 text-xs text-gray-500 leading-normal">
            Điền đáp án cho từng câu hỏi phía dưới. Giảng viên sẽ đối chiếu theo bảng đáp án chuẩn.
          </p>
        </div>
        <span className="rounded-full bg-[#fff0f1] px-2.5 py-1 text-xs font-extrabold text-[#730014]">
          {questions.length} câu hỏi
        </span>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 max-h-[300px] overflow-y-auto pr-1">
        {questions.map((question, index) => {
          const key = String(question.number ?? question.id ?? index + 1);
          const options = Array.isArray(question.options) ? question.options : [];
          return (
            <div className={`space-y-2 ${options.length ? 'sm:col-span-2 rounded-xl border border-gray-100 p-4' : ''}`} key={key}>
              <span className="text-xs font-bold text-gray-600">
                Câu {question.displayNumber ?? key}{question.prompt ? ` · ${question.prompt}` : ''}
              </span>
              {options.length ? (
                <div className="grid gap-2 sm:grid-cols-2">
                  {options.map((option, optionIndex) => {
                    const optionValue = String(option?.value ?? option?.label ?? option);
                    const optionLabel = String(option?.label ?? option?.value ?? option);
                    const checked = structured.responses?.[key] === optionValue;
                    return (
                      <label
                        className={`flex cursor-pointer items-center gap-2 rounded-xl border px-3 py-2.5 text-xs font-semibold transition ${checked ? 'border-[#730014] bg-[#fff0f1] text-[#4b0009]' : 'border-gray-200 bg-white text-[#584140]'}`}
                        key={`${key}-${optionValue}-${optionIndex}`}
                      >
                        <input
                          checked={checked}
                          className="accent-[#730014]"
                          name={`homework-question-${homework.id}-${key}`}
                          onChange={() => updateAnswer(key, optionValue)}
                          type="radio"
                        />
                        {optionLabel}
                      </label>
                    );
                  })}
                </div>
              ) : (
                <input
                  className="w-full rounded-xl border border-gray-200 px-3.5 py-2.5 text-xs outline-none transition focus:border-[#730014] focus:ring-2 focus:ring-[#730014]/5"
                  onChange={(event) => updateAnswer(key, event.target.value)}
                  placeholder="Điền đáp án..."
                  value={structured.responses?.[key] || ''}
                />
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function parseActivityConfig(value) {
  if (!value) return {};
  try {
    const parsed = JSON.parse(String(value));
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function normalizeExamOptions(options = []) {
  return options.map((option, index) => {
    if (option && typeof option === 'object') {
      const value = String(option.value || option.key || String.fromCharCode(65 + index));
      return { value, label: String(option.label || option.text || value) };
    }
    return {
      value: String.fromCharCode(65 + index),
      label: String(option || String.fromCharCode(65 + index)),
    };
  });
}

function normalizeExamQuestion(question, fallbackNumber) {
  return {
    ...question,
    number: Number(question.number || question.id || fallbackNumber),
    prompt: question.prompt || question.question || question.text || `Question ${fallbackNumber}`,
    options: normalizeExamOptions(question.options || []),
  };
}

function buildHomeworkObjectiveConfig(homework) {
  const source = parseActivityConfig(homework?.activityConfigJson);
  const durationMinutes = Number(source.durationMinutes || source.timeLimitMinutes || 30);
  const sourceParts = Array.isArray(source.parts) && source.parts.length ? source.parts : [{
    key: 'homework_part_1',
    partNumber: 1,
    title: homework?.curriculumUnitTitle || homework?.title || 'Bài tập',
    instructions: homework?.instruction || '',
    passage: {
      title: homework?.title || 'Bài tập',
      paragraphs: homework?.instruction ? [{ text: homework.instruction }] : [],
    },
    questions: getActivityQuestions(source),
  }];

  const parts = sourceParts.map((part, partIndex) => {
    let questionCounter = 1;
    const groups = Array.isArray(part.questionGroups) && part.questionGroups.length
      ? part.questionGroups.map((group, groupIndex) => ({
        ...group,
        title: group.title || `Nhóm câu hỏi ${groupIndex + 1}`,
        type: group.type || 'single_choice',
        questions: (group.questions || []).map((question) => {
          const normalized = normalizeExamQuestion(question, questionCounter);
          questionCounter += 1;
          return normalized;
        }),
      }))
      : [{
        title: part.groupTitle || 'Câu hỏi',
        instructions: part.instructions || homework?.instruction || '',
        type: part.type || 'single_choice',
        passage: part.description || '',
        questions: (part.questions || []).map((question) => {
          const normalized = normalizeExamQuestion(question, questionCounter);
          questionCounter += 1;
          return normalized;
        }),
      }];
    const questionNumbers = groups.flatMap((group) => group.questions.map((question) => question.number));
    return {
      ...part,
      key: part.key || `homework_part_${partIndex + 1}`,
      partNumber: Number(part.partNumber || part.part || partIndex + 1),
      title: part.title || `Phần ${partIndex + 1}`,
      questionRange: questionNumbers.length
        ? `Questions ${questionNumbers[0]}-${questionNumbers[questionNumbers.length - 1]}`
        : '',
      passage: part.passage || {
        title: homework?.title || part.title || 'Bài tập',
        paragraphs: homework?.instruction ? [{ text: homework.instruction }] : [],
      },
      questionGroups: groups,
    };
  });

  return {
    ...source,
    key: source.key || `homework_${homework?.id}`,
    title: homework?.title || source.title || 'Bài tập',
    durationMinutes,
    rules: source.rules || ['Làm bài trong thời gian quy định', 'Kiểm tra đáp án trước khi nộp'],
    parts,
  };
}

function buildHomeworkWritingConfig(homework) {
  const source = parseActivityConfig(homework?.activityConfigJson);
  const tasks = Array.isArray(source.tasks) && source.tasks.length ? source.tasks : [{
    key: 'homework_response',
    title: homework?.curriculumUnitTitle || 'Bài viết',
    question: homework?.instruction || 'Hoàn thành bài viết theo yêu cầu của giáo viên.',
    minimumWords: Number(source.minimumWords || source.minWords || 150),
    recommendedMinutes: Number(source.durationMinutes || 40),
  }];
  return {
    ...source,
    key: source.key || `homework_${homework?.id}`,
    title: homework?.title || 'Bài tập Writing',
    durationMinutes: Number(source.durationMinutes || 40),
    tasks,
  };
}

function buildHomeworkSpeakingConfig(homework) {
  const source = parseActivityConfig(homework?.activityConfigJson);
  const selectedVariant = Array.isArray(source.variants) && source.variants.length
    ? source.variants[0]
    : null;
  const sourceParts = selectedVariant?.parts || source.parts;
  const parts = Array.isArray(sourceParts) && sourceParts.length ? sourceParts : [{
    key: 'part_1',
    title: homework?.curriculumUnitTitle || 'Speaking response',
    prompts: [homework?.instruction || 'Trình bày câu trả lời theo yêu cầu của giáo viên.'],
    answerSeconds: Number(source.answerSeconds || 120),
  }];
  return {
    ...source,
    title: homework?.title || 'Bài tập Speaking',
    submissionLabel: homework?.title || 'Bài tập Speaking',
    variantKey: selectedVariant?.key || null,
    variantLabel: selectedVariant?.label || null,
    parts,
  };
}

export function parseObjectiveExamPayload(value) {
  try {
    const parsed = JSON.parse(String(value || '{}'));
    const responses = Object.fromEntries((parsed.responses || []).map((response) => [
      String(response.questionNumber),
      response.answer,
    ]));
    return {
      responses,
      exam: {
        mode: parsed.mode,
        autoSubmitted: Boolean(parsed.autoSubmitted),
        remainingSeconds: parsed.remainingSeconds,
        answeredCount: parsed.answeredCount,
        totalQuestions: parsed.totalQuestions,
        violations: parsed.violations || [],
      },
    };
  } catch {
    return { responses: {}, exam: {} };
  }
}

function getActivityQuestions(config = {}) {
  if (Array.isArray(config.questions)) return config.questions;
  if (Array.isArray(config.items)) return config.items;
  if (Array.isArray(config.parts)) {
    return config.parts.flatMap((part) => (
      part.questions
      || (part.questionGroups || []).flatMap((group) => group.questions || (group.questionNumbers || []).map((number) => ({ number })))
      || []
    ));
  }
  return [];
}

function readStructuredAnswer(value) {
  try {
    const parsed = JSON.parse(String(value || '{}'));
    if (parsed && typeof parsed === 'object') {
      return { responses: {}, ...parsed };
    }
  } catch {
    // Return empty if parsing plain text answer string
  }
  return { responses: {} };
}

function usesStructuredWorkspace(homework) {
  if (!homework) return false;
  if (homework.activityType === 'FLASHCARD_REVIEW') return true;
  return usesModuleExamWorkspace(homework);
}

function usesModuleExamWorkspace(homework) {
  if (!homework || homework.activityType === 'FILE_RESPONSE' || homework.activityType === 'FLASHCARD_REVIEW') {
    return false;
  }
  const questions = getActivityQuestions(parseActivityConfig(homework.activityConfigJson));
  if (questions.length > 0) return true;
  return ['WRITING', 'SPEAKING'].includes(String(homework.skill || '').toUpperCase());
}

export function FlashcardHomeworkWorkspace({ canComplete, curriculum, homework, onClose, onComplete, submitting }) {
  const assignedUnit = (curriculum?.units || []).find(
    (unit) => String(unit.id) === String(homework.curriculumUnitId),
  );
  const customFlashcards = useMemo(() => {
    const config = parseActivityConfig(homework?.activityConfigJson);
    return (Array.isArray(config.flashcards) ? config.flashcards : [])
      .filter((card) => card && typeof card === 'object' && card.term && (card.meaning || card.definition))
      .map((card, index) => ({
        termKey: `homework-${homework?.id}-flashcard-${index + 1}`,
        term: String(card.term),
        meaning: String(card.meaning || card.definition),
        example: [
          card.example ? String(card.example) : '',
          card.commonMistake ? `Lỗi thường gặp: ${card.commonMistake}` : '',
        ].filter(Boolean).join('\n\n'),
        moduleTitle: homework?.title || 'Flashcard bài tập',
        status: 'NEW',
        reviewCount: 0,
        incorrectCount: 0,
      }));
  }, [homework?.activityConfigJson, homework?.id, homework?.title]);

  return (
    <div className="fixed inset-0 z-[155] flex min-h-0 flex-col bg-[#fcf9f8]">
      <header className="shrink-0 border-b border-[#eadfe0] bg-white px-4 py-3 shadow-sm sm:px-6">
        <div className="mx-auto flex max-w-[1320px] items-center justify-between gap-4">
          <div className="min-w-0">
            <p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">
              Flashcard lớp học {assignedUnit ? `· ${assignedUnit.title}` : ''}
            </p>
            <h1 className="truncate font-['Manrope'] text-base font-extrabold text-[#1a1c1c] sm:text-lg">
              {homework.title}
            </h1>
          </div>
          <div className="flex shrink-0 items-center gap-2">
            {canComplete ? (
              <button
                className="inline-flex items-center gap-2 bg-[#730014] px-4 py-2.5 text-xs font-extrabold text-white transition hover:bg-[#4b0009] disabled:cursor-wait disabled:opacity-60"
                disabled={submitting}
                onClick={onComplete}
                type="button"
              >
                {submitting ? <RefreshCw className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
                <span className="hidden sm:inline">Đánh dấu đã ôn xong</span>
                <span className="sm:hidden">Hoàn thành</span>
              </button>
            ) : null}
            <button
              aria-label="Đóng phần học flashcard"
              className="inline-flex h-10 w-10 items-center justify-center border border-[#dfd4d4] bg-white text-[#584140] transition hover:border-[#8a0018] hover:text-[#8a0018]"
              disabled={submitting}
              onClick={onClose}
              title="Đóng"
              type="button"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>
      </header>
      <main className="min-h-0 flex-1 overflow-y-auto px-4 py-6 sm:px-6">
        <div className="mx-auto max-w-[1320px]">
          {customFlashcards.length ? (
            <WorkspaceFlashcards
              course={{ title: homework.title }}
              emptyStateDescription="Bài tập này chưa có flashcard."
              termsOverride={customFlashcards}
            />
          ) : (
            <ClassroomFlashcardsPanel
              curriculum={curriculum}
              initialUnitId={homework.curriculumUnitId}
            />
          )}
        </div>
      </main>
    </div>
  );
}

function getHomeworkExamSummary(homework) {
  const config = parseActivityConfig(homework?.activityConfigJson);
  const skill = String(homework?.skill || '').toUpperCase();
  const questionCount = getActivityQuestions(config).length;
  const writingTaskCount = Array.isArray(config.tasks) ? config.tasks.length : 0;
  const speakingParts = Array.isArray(config.variants) && config.variants.length
    ? config.variants[0]?.parts || []
    : config.parts || [];
  const speakingPromptCount = speakingParts.reduce(
    (total, part) => total + (part.prompts || []).length,
    0,
  );
  const duration = Number(config.durationMinutes || config.timeLimitMinutes || (skill === 'WRITING' ? 40 : 30));

  let contentLabel = `${questionCount} câu hỏi`;
  if (!questionCount && skill === 'WRITING') contentLabel = `${writingTaskCount || 1} bài viết`;
  if (!questionCount && skill === 'SPEAKING') contentLabel = `${speakingPromptCount || 1} phần nói`;

  return { contentLabel, duration };
}

export function HomeworkConfirmModal({ homework, onClose, onConfirm }) {
  if (!homework) return null;
  const summary = getHomeworkExamSummary(homework);
  return (
    <div className="fixed inset-0 z-[160] flex items-center justify-center bg-black/45 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-3xl border border-gray-200/80 bg-white p-6 shadow-2xl space-y-5 animate-in fade-in zoom-in-95 duration-200">
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-[10px] font-extrabold text-[#730014] uppercase tracking-widest">
            <span className="h-4 w-1 rounded-full bg-[#8a0018] shrink-0" />
            Xác nhận vào làm bài
          </div>
          <h3 className="font-['Manrope'] text-base font-extrabold text-[#1a1c1c] leading-snug">
            {homework.title}
          </h3>
          <p className="text-xs text-[#584140] leading-relaxed">
            {homework.instruction || 'Không có mô tả chi tiết bài tập.'}
          </p>
        </div>

        <div className="rounded-2xl bg-[#fff0f1]/35 border border-[#dfbfbd]/25 p-4 space-y-2.5 text-xs text-[#584140]">
          <div className="flex justify-between items-center">
            <span className="font-bold text-gray-500">Kỹ năng học luyện:</span>
            <span className="inline-flex rounded-full bg-[#fff0f1] px-2.5 py-0.5 text-[9px] font-extrabold text-[#730014] border border-[#dfbfbd]/25">
              {getHomeworkSkillLabel(homework.skill)}
            </span>
          </div>
          <div className="flex justify-between items-center">
            <span className="font-bold text-gray-500">Nội dung bài:</span>
            <span className="font-bold text-[#1a1c1c]">{summary.contentLabel}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="font-bold text-gray-500">Thời gian dự kiến:</span>
            <span className="font-bold text-[#1a1c1c]">{summary.duration} phút</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="font-bold text-gray-500">Hạn chót nộp bài:</span>
            <span className="font-bold text-[#1a1c1c]">{formatClassroomDateTime(homework.deadline)}</span>
          </div>
          {getHomeworkGradingHint(homework) && (
            <div className="flex justify-between items-center">
              <span className="font-bold text-gray-500">Hình thức chấm:</span>
              <span className="text-purple-700 font-bold">{getHomeworkGradingHint(homework)}</span>
            </div>
          )}
        </div>

        <div className="rounded-xl border border-amber-100 bg-amber-50/20 p-3 text-[11px] text-amber-800 flex items-start gap-2">
          <Info className="h-4 w-4 shrink-0 text-amber-600 mt-0.5" />
          <p className="leading-normal font-medium">
            Hãy chắc chắn bạn đã sẵn sàng và thiết lập micro hoạt động tốt (nếu làm bài Nói/Speaking) trước khi tiếp tục.
          </p>
        </div>

        <div className="flex items-center justify-end gap-2.5 pt-2">
          <button
            className="rounded-xl border border-gray-200 bg-white px-5 py-2.5 text-xs font-bold text-gray-700 hover:bg-gray-50 transition active:scale-95"
            onClick={onClose}
            type="button"
          >
            Quay lại
          </button>
          <button
            className="rounded-xl bg-gradient-to-r from-[#730014] to-[#4b0009] px-5 py-2.5 text-xs font-bold text-white shadow-sm transition hover:shadow active:scale-95 btn-hover"
            onClick={onConfirm}
            type="button"
          >
            Bắt đầu làm bài
          </button>
        </div>
      </div>
    </div>
  );
}
