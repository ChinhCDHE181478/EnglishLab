import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
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
  ChevronRight,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  ClassroomTabBar,
  PageHero,
  StatusBadge,
  DetailDrawer,
} from '../../components/classroom/ClassroomUi';
import LearnerPageShell from '../../components/learner/LearnerPageShell';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDateTime } from '../../utils/classroomHelpers';
import { getStoredUser, hasAccessToken } from '../../utils/auth';

const homeworkTabs = [
  { id: 'all', label: 'Tất cả' },
  { id: 'pending', label: 'Chưa nộp' },
  { id: 'submitted', label: 'Đã nộp' },
  { id: 'graded', label: 'Đã chấm' },
  { id: 'overdue', label: 'Quá hạn' },
];

export default function MyHomeworkPage() {
  const [homework, setHomework] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [submittingId, setSubmittingId] = useState(null);
  const [submitAnswers, setSubmitAnswers] = useState({});
  const [actionMessage, setActionMessage] = useState('');
  const [selectedHomework, setSelectedSession] = useState(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  const isAuthenticated = Boolean(hasAccessToken() && getStoredUser());

  const loadHomework = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getMyHomework();
      setHomework(data);
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

  // Determine homework status for filtering
  const getHomeworkStatus = (item) => {
    if (item.mySubmission) {
      return item.mySubmission.score != null ? 'GRADED' : 'SUBMITTED';
    }
    return item.overdue ? 'OVERDUE' : 'NOT_SUBMITTED';
  };

  // Filter and search homework
  const filteredHomework = useMemo(() => {
    return homework.filter((item) => {
      const status = getHomeworkStatus(item);
      const matchesSearch =
        item.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.instruction?.toLowerCase().includes(searchQuery.toLowerCase());

      if (!matchesSearch) return false;

      if (activeTab === 'pending') return status === 'NOT_SUBMITTED';
      if (activeTab === 'submitted') return status === 'SUBMITTED';
      if (activeTab === 'graded') return status === 'GRADED';
      if (activeTab === 'overdue') return status === 'OVERDUE';
      return true;
    });
  }, [homework, activeTab, searchQuery]);

  // Calculate statistics for PageHero
  const stats = useMemo(() => {
    if (!homework.length) return [];

    const pendingCount = homework.filter((h) => getHomeworkStatus(h) === 'NOT_SUBMITTED').length;
    const submittedCount = homework.filter((h) => h.mySubmission && h.mySubmission.score == null).length;
    const gradedCount = homework.filter((h) => h.mySubmission && h.mySubmission.score != null).length;
    const overdueCount = homework.filter((h) => !h.mySubmission && h.overdue).length;

    return [
      { label: 'Chưa nộp', value: pendingCount, icon: Clock, color: pendingCount > 0 ? 'amber' : 'blue' },
      { label: 'Đã nộp', value: submittedCount, icon: CheckCircle2, color: 'blue' },
      { label: 'Đã chấm điểm', value: gradedCount, icon: Award, color: 'emerald' },
      { label: 'Quá hạn nộp', value: overdueCount, icon: AlertCircle, color: overdueCount > 0 ? 'rose' : 'blue' },
    ];
  }, [homework]);

  const handleSubmit = async (homeworkId) => {
    setSubmittingId(homeworkId);
    setActionMessage('');
    try {
      await classroomApi.submitHomework(homeworkId, {
        textAnswer: submitAnswers[homeworkId] || '',
        attachmentUrl: '',
      });
      setActionMessage('Đã nộp bài tập thành công.');
      await loadHomework();
      setIsDrawerOpen(false);
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể nộp bài tập.'));
    } finally {
      setSubmittingId(null);
    }
  };

  const handleOpenSubmitDrawer = (item) => {
    setSelectedSession(item);
    setIsDrawerOpen(true);
  };

  return (
    <LearnerPageShell
      description="Theo dõi bài tập, nộp bài trực tuyến và xem điểm số, nhận xét từ giảng viên."
      title="Bài tập của tôi"
    >
      {!isAuthenticated ? (
        <ClassroomEmptyState
          actionLabel="Đăng nhập"
          actionTo="/login"
          description="Bạn cần đăng nhập để xem bài tập."
          title="Chưa đăng nhập"
        />
      ) : loading ? (
        <ClassroomLoadingState message="Đang tải danh sách bài tập..." />
      ) : error ? (
        <ClassroomErrorState message={error} onRetry={loadHomework} />
      ) : (
        <div className="space-y-8">
          {/* Page Hero with operational stats */}
          <PageHero
            title="Bài tập của tôi"
            subtitle="Hoàn thành các bài tập viết, đọc hoặc bài tập thực hành được giao bởi giảng viên để củng cố kiến thức."
            stats={stats}
          />

          <div className="space-y-6">
            {/* Search and Filters */}
            <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
              <ClassroomTabBar activeTab={activeTab} onChange={setActiveTab} tabs={homeworkTabs} />

              <div className="relative w-full md:w-72">
                <input
                  type="text"
                  className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-white py-3.5 pl-11 pr-4 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
                  placeholder="Tìm theo tên bài tập..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
                <Search className="absolute left-4 top-3.5 h-4.5 w-4.5 text-[#8b706e]" />
              </div>
            </div>

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

            {/* Homework Grid */}
            {filteredHomework.length ? (
              <div className="grid gap-6 md:grid-cols-2">
                {filteredHomework.map((item, idx) => {
                  const status = getHomeworkStatus(item);
                  const isGraded = status === 'GRADED';
                  const isSubmitted = status === 'SUBMITTED';
                  const isOverdue = status === 'OVERDUE';
                  const hasSubmission = !!item.mySubmission;

                  // Calculate visual urgency
                  const isUrgent = !hasSubmission && !isOverdue && new Date(item.deadline) - new Date() < 24 * 60 * 60 * 1000;

                  return (
                    <motion.article
                      key={item.id}
                      initial={{ opacity: 0, y: 16 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.3, delay: Math.min(idx * 0.06, 0.36), ease: 'easeOut' }}
                      className={`flex flex-col overflow-hidden rounded-xl border bg-white transition-shadow hover:shadow-md ${
                        isUrgent ? 'border-amber-300' : 'border-[#e5e7eb]'
                      }`}
                    >
                      {/* Header */}
                      <div className="border-b border-[#f0f0f0] bg-white p-5">
                        <div className="flex items-center justify-between gap-3">
                          <span className="text-xs font-extrabold uppercase tracking-wider text-[#730014]">
                            Lớp học #{item.classroomOfferingId}
                          </span>
                          <StatusBadge status={status} />
                        </div>

                        <h3 className="mt-3 font-['Manrope'] text-xl font-extrabold text-[#2b2828] line-clamp-1">
                          {item.title}
                        </h3>
                      </div>

                      {/* Content */}
                      <div className="flex-1 p-5 space-y-4">
                        <p className="text-sm text-[#584140] line-clamp-3">
                          {item.instruction || 'Không có hướng dẫn chi tiết.'}
                        </p>

                        <div className="grid gap-3 text-xs text-[#8b706e] sm:grid-cols-2 pt-2">
                          <div className="flex items-center gap-2">
                            <Calendar className="h-4 w-4 text-[#730014]" />
                            <span>Hạn nộp: <strong className="text-[#584140]">{formatClassroomDateTime(item.deadline)}</strong></span>
                          </div>
                          {isGraded && (
                            <div className="flex items-center gap-2">
                              <Award className="h-4 w-4 text-emerald-700" />
                              <span>Điểm số: <strong className="text-emerald-700 text-sm font-extrabold">{item.mySubmission.score} / 10</strong></span>
                            </div>
                          )}
                        </div>

                        {/* Urgent Alert */}
                        {isUrgent && (
                          <div className="rounded-xl bg-amber-50 border border-amber-100 p-3 text-xs text-amber-800 flex items-center gap-2">
                            <Clock className="h-4 w-4 text-amber-700 animate-pulse" />
                            <span>Sắp hết hạn nộp bài! (Còn lại dưới 24h)</span>
                          </div>
                        )}

                        {/* Teacher Feedback Preview */}
                        {isGraded && item.mySubmission.feedback && (
                          <div className="rounded-xl bg-emerald-50/30 border border-emerald-100/50 p-4 space-y-1">
                            <p className="text-[10px] font-bold text-emerald-800 uppercase tracking-wider flex items-center gap-1">
                              <MessageSquare className="h-3.5 w-3.5" />
                              Nhận xét từ giảng viên
                            </p>
                            <p className="text-xs text-[#584140] italic">
                              "{item.mySubmission.feedback}"
                            </p>
                          </div>
                        )}
                      </div>

                      {/* Footer */}
                      <div className="border-t border-gray-50 bg-gray-50/30 px-6 py-4 flex items-center justify-between">
                        <Link
                          className="text-xs font-bold text-[#730014] underline"
                          to={`/my-classrooms/${item.classroomOfferingId}`}
                        >
                          Mở lớp học
                        </Link>

                        {!hasSubmission ? (
                          <button
                            className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-2.5 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014] active:scale-95"
                            onClick={() => handleOpenSubmitDrawer(item)}
                            type="button"
                          >
                            <Send className="h-3.5 w-3.5" />
                            Làm bài & Nộp bài
                          </button>
                        ) : (
                          <button
                            className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-5 py-2.5 text-xs font-extrabold text-[#584140] transition hover:bg-gray-50 active:scale-95"
                            onClick={() => handleOpenSubmitDrawer(item)}
                            type="button"
                          >
                            <FileText className="h-3.5 w-3.5" />
                            Xem bài đã nộp
                          </button>
                        )}
                      </div>
                    </motion.article>
                  );
                })}
              </div>
            ) : (
              <ClassroomEmptyState
                actionLabel="Xem lớp của tôi"
                actionTo="/my-classrooms"
                description="Không tìm thấy bài tập nào khớp với bộ lọc."
                title="Chưa có bài tập"
              />
            )}
          </div>
        </div>
      )}

      {/* Submit / View Homework Drawer */}
      <DetailDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        title={selectedHomework?.mySubmission ? 'Bài tập đã nộp' : 'Nộp bài tập'}
      >
        {selectedHomework && (
          <div className="space-y-6">
            {/* Header */}
            <div className="space-y-2">
              <span className="text-xs font-extrabold uppercase tracking-wider text-[#730014]">
                Lớp học #{selectedHomework.classroomOfferingId}
              </span>
              <h3 className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
                {selectedHomework.title}
              </h3>
              <div className="flex items-center gap-2 pt-1">
                <StatusBadge status={getHomeworkStatus(selectedHomework)} />
                <span className="text-xs text-[#8b706e]">
                  Hạn nộp: {formatClassroomDateTime(selectedHomework.deadline)}
                </span>
              </div>
            </div>

            {/* Instruction */}
            <div className="rounded-2xl border border-gray-100 bg-gray-50/30 p-5 space-y-3">
              <h4 className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Đề bài & Hướng dẫn</h4>
              <p className="text-sm text-[#584140] whitespace-pre-wrap leading-6">
                {selectedHomework.instruction || 'Không có hướng dẫn chi tiết.'}
              </p>
            </div>

            {/* Submission Form or View */}
            {selectedHomework.mySubmission ? (
              <div className="space-y-5">
                <div className="rounded-2xl border border-emerald-100 bg-emerald-50/10 p-5 space-y-4">
                  <h4 className="text-xs font-bold text-emerald-800 uppercase tracking-wider flex items-center gap-1">
                    <CheckCircle2 className="h-4 w-4" />
                    Bài làm đã nộp
                  </h4>
                  <div className="rounded-xl bg-white border border-gray-100 p-4 text-sm text-[#2b2828] whitespace-pre-wrap min-h-[100px]">
                    {selectedHomework.mySubmission.textAnswer || 'Không có câu trả lời bằng văn bản.'}
                  </div>
                  <p className="text-xs text-gray-400">
                    Nộp lúc: {formatClassroomDateTime(selectedHomework.mySubmission.submittedAt)}
                  </p>
                </div>

                {/* Score & Feedback */}
                {selectedHomework.mySubmission.score != null && (
                  <div className="rounded-2xl border border-[#dfbfbd]/30 bg-white p-5 space-y-4">
                    <div className="flex items-center justify-between">
                      <h4 className="text-xs font-bold text-[#730014] uppercase tracking-wider flex items-center gap-1">
                        <Award className="h-4 w-4" />
                        Kết quả chấm điểm
                      </h4>
                      <span className="rounded-full bg-emerald-100 px-3 py-1 text-sm font-extrabold text-emerald-800">
                        {selectedHomework.mySubmission.score} / 10 điểm
                      </span>
                    </div>
                    {selectedHomework.mySubmission.feedback ? (
                      <div className="space-y-1">
                        <p className="text-xs font-bold text-[#8b706e]">Nhận xét của giảng viên:</p>
                        <p className="text-sm text-[#584140] italic bg-[#fffafb] border border-[#dfbfbd]/15 p-4 rounded-xl">
                          "{selectedHomework.mySubmission.feedback}"
                        </p>
                      </div>
                    ) : (
                      <p className="text-xs text-gray-400 italic">Giảng viên chưa để lại nhận xét chi tiết.</p>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-4">
                <h4 className="text-xs font-bold text-[#8b706e] uppercase tracking-wider flex items-center gap-1">
                  <FileText className="h-4 w-4 text-[#730014]" />
                  Bài làm của bạn
                </h4>
                <textarea
                  className="min-h-[180px] w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white focus:ring-2 focus:ring-[#730014]/5"
                  onChange={(e) => setSubmitAnswers((curr) => ({ ...curr, [selectedHomework.id]: e.target.value }))}
                  placeholder="Nhập nội dung bài làm hoặc câu trả lời của bạn tại đây..."
                  value={submitAnswers[selectedHomework.id] || ''}
                />

                <div className="pt-4 flex gap-3">
                  <button
                    className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-2xl bg-[#4b0009] py-3.5 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow-lg active:scale-95 disabled:opacity-60"
                    disabled={submittingId === selectedHomework.id}
                    onClick={() => handleSubmit(selectedHomework.id)}
                    type="button"
                  >
                    {submittingId === selectedHomework.id ? (
                      <>Đang nộp bài...</>
                    ) : (
                      <>
                        <Send className="h-4 w-4" />
                        Nộp bài tập ngay
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
  );
}
