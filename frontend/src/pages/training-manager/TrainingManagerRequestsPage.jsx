import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import {
  BookOpen,
  Calendar,
  Clock,
  MapPin,
  Video,
  Users,
  Award,
  CheckCircle2,
  XCircle,
  AlertCircle,
  ArrowLeft,
  Settings,
  User,
  MessageSquare,
  Search,
  Check,
  X,
  RefreshCw,
  HelpCircle,
  Activity,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import Header from '../../components/ai-learning/Header';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  PageHero,
  StatusBadge,
  ConflictPanel,
} from '../../components/classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDateTime } from '../../utils/classroomHelpers';
import { PAGE_BODY_CLASS, PAGE_HEADER_CLASS, PAGE_MAIN_STACK_CLASS, PAGE_SHELL_CLASS } from '../../utils/pageLayout';

export default function TrainingManagerRequestsPage() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [reviewNotes, setReviewNotes] = useState({});
  const [selectedId, setSelectedId] = useState('');
  const [conflictResults, setConflictResults] = useState({});
  const [checkingConflicts, setCheckingConflicts] = useState({});

  const loadRequests = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getPendingChangeRequests();
      setRequests(data);
      if (data.length > 0) {
        const exists = data.some((item) => String(item.id) === selectedId);
        if (!exists) {
          setSelectedId(String(data[0].id));
        }
      } else {
        setSelectedId('');
      }
    } catch (err) {
      setRequests([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải yêu cầu thay đổi.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRequests();
  }, []);

  const selected = useMemo(
    () => requests.find((item) => String(item.id) === selectedId) || null,
    [requests, selectedId],
  );

  const handleReview = async (requestId, action) => {
    setActionMessage('');
    try {
      const payload = { reviewNote: reviewNotes[requestId] || '' };
      if (action === 'approve') {
        await classroomApi.approveChangeRequest(requestId, payload);
      } else {
        await classroomApi.rejectChangeRequest(requestId, payload);
      }
      setActionMessage(action === 'approve' ? 'Đã duyệt và áp dụng yêu cầu thành công.' : 'Đã từ chối yêu cầu thành công.');
      await loadRequests();
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể xử lý yêu cầu.'));
    }
  };

  const handleConflictCheck = async (requestId) => {
    setActionMessage('');
    setCheckingConflicts((curr) => ({ ...curr, [requestId]: true }));
    try {
      // The API has a checkChangeRequestConflict endpoint
      const result = await classroomApi.checkChangeRequestConflict(requestId);
      setConflictResults((curr) => ({ ...curr, [requestId]: result }));
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể kiểm tra trùng lịch.'));
    } finally {
      setCheckingConflicts((curr) => ({ ...curr, [requestId]: false }));
    }
  };

  // Calculate statistics for PageHero
  const stats = useMemo(() => {
    const pendingCount = requests.filter((r) => r.status === 'PENDING' || !r.status).length;
    const conflictCount = Object.values(conflictResults).filter((r) => r.hasConflict || (r.conflicts && r.conflicts.length > 0)).length;

    return [
      { label: 'Chờ xử lý', value: pendingCount, icon: Clock, color: pendingCount > 0 ? 'amber' : 'blue' },
      { label: 'Phát hiện trùng lịch', value: conflictCount, icon: AlertCircle, color: conflictCount > 0 ? 'rose' : 'blue' },
    ];
  }, [requests, conflictResults]);

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
        {/* Page Hero with operational stats */}
        <PageHero
          title="Phê duyệt yêu cầu thay đổi"
          subtitle="Không gian xử lý phê duyệt các đề xuất từ Giảng viên liên quan đến việc đổi lịch học, đổi phòng học offline, đổi liên kết Lark hoặc hủy buổi học."
          stats={stats}
          action={
            <Link
              className="inline-flex items-center gap-1.5 rounded-2xl border border-[#dfbfbd] bg-white px-5 py-3 text-sm font-extrabold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95"
              to="/training-manager/classroom-registrations"
            >
              <ArrowLeft className="h-4 w-4" />
              Quay lại Quản lý đăng ký & học phí
            </Link>
          }
        />

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

        {/* Workspace Layout */}
        <section className="flex flex-1 flex-col">
          {loading ? <ClassroomLoadingState message="Đang tải danh sách yêu cầu thay đổi..." /> : null}
          {!loading && error ? <ClassroomErrorState message={error} onRetry={loadRequests} /> : null}
          {!loading && !error && !requests.length ? (
            <ClassroomEmptyState
              description="Hiện tại không có yêu cầu thay đổi nào cần Training Manager phê duyệt."
              title="Không có yêu cầu cần duyệt"
              icon={HelpCircle}
            />
          ) : null}

          {!loading && !error && requests.length ? (
            <div className="grid gap-6 lg:grid-cols-[380px_1fr]">
              {/* Left Sidebar: Requests List */}
              <aside className="rounded-xl border border-[#e5e7eb] bg-white p-5 shadow-sm space-y-3 max-h-[750px] overflow-y-auto">
                <h3 className="text-xs font-bold text-[#8b706e] uppercase tracking-wider px-2">Yêu cầu chờ duyệt</h3>
                <div className="space-y-2">
                  {requests.map((item) => {
                    const isSelected = String(item.id) === selectedId;
                    return (
                      <button
                        key={item.id}
                        className={`w-full rounded-2xl p-4 text-left transition-all duration-200 border ${
                          isSelected
                            ? 'bg-[#4b0009] border-[#4b0009] text-white shadow-md shadow-[#4b0009]/10'
                            : 'bg-[#fffafb]/50 border-gray-100 text-[#584140] hover:bg-[#fff3f4] hover:border-[#dfbfbd]/30'
                        }`}
                        onClick={() => setSelectedId(String(item.id))}
                        type="button"
                      >
                        <p className="font-extrabold text-sm line-clamp-1">
                          {item.requestTypeLabel || item.requestType}
                        </p>
                        <p className={`mt-1.5 text-xs line-clamp-1 ${isSelected ? 'text-white/80' : 'text-[#8b706e]'}`}>
                          {item.classroomTitle}
                        </p>
                        <div className="mt-3 flex items-center justify-between">
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                            isSelected ? 'bg-white/20 text-white' : 'bg-[#fff1f3] text-[#730014]'
                          }`}>
                            Giảng viên: {item.requesterName || 'Đang cập nhật'}
                          </span>
                          <span className="text-[10px] opacity-75">ID: #{item.id}</span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              </aside>

              {/* Right Panel: Operations Workspace */}
              {selected ? (
                <div className="space-y-6">
                  {/* Request Detail Card */}
                  <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-6">
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-50 pb-5">
                      <div className="flex items-center gap-4">
                        <div className="flex h-14 w-12 items-center justify-center rounded-2xl bg-rose-50 text-[#730014] font-extrabold text-lg">
                          {selected.requesterName ? selected.requesterName.charAt(0).toUpperCase() : 'G'}
                        </div>
                        <div>
                          <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{selected.requestTypeLabel || selected.requestType}</h2>
                          <p className="text-xs text-[#8b706e] mt-0.5">Người gửi: Giảng viên {selected.requesterName || 'Đang cập nhật'}</p>
                        </div>
                      </div>
                      <StatusBadge status="PENDING" />
                    </div>

                    <div className="grid gap-6 sm:grid-cols-2 text-sm text-[#584140]">
                      <div className="space-y-3">
                        <p className="flex items-center gap-2">
                          <BookOpen className="h-4 w-4 text-[#730014]" />
                          <span>Lớp học: <strong className="text-[#2b2828]">{selected.classroomTitle}</strong></span>
                        </p>
                        <p className="flex items-center gap-2">
                          <Calendar className="h-4 w-4 text-[#730014]" />
                          <span>Thời gian gửi: <strong className="text-[#2b2828]">{formatClassroomDateTime(selected.createdAt)}</strong></span>
                        </p>
                      </div>

                      {selected.targetSessionId && (
                        <div className="space-y-3">
                          <p className="flex items-center gap-2">
                            <Clock className="h-4 w-4 text-[#730014]" />
                            <span>Buổi học áp dụng: <strong className="text-[#2b2828]">Buổi #{selected.targetSessionId}</strong></span>
                          </p>
                        </div>
                      )}
                    </div>

                    {/* Reason Block */}
                    <div className="rounded-2xl border border-gray-100 bg-gray-50/30 p-5 space-y-2">
                      <h4 className="text-xs font-bold text-[#8b706e] uppercase tracking-wider flex items-center gap-1">
                        <MessageSquare className="h-4 w-4 text-[#730014]" />
                        Lý do thay đổi & Đề xuất cụ thể từ Giảng viên
                      </h4>
                      <p className="text-sm text-[#584140] whitespace-pre-wrap leading-6">
                        {selected.reason || 'Không có mô tả chi tiết.'}
                      </p>
                    </div>
                  </section>

                  {/* Conflict Check & Approval Actions */}
                  <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-6">
                    <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828] flex items-center gap-2">
                      <Activity className="h-5 w-5 text-[#730014]" />
                      Thao tác phê duyệt
                    </h3>

                    <div className="flex flex-wrap gap-3">
                      <button
                        className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-700 px-5 py-3 text-xs font-extrabold text-white shadow-sm transition hover:bg-emerald-800 active:scale-95"
                        onClick={() => handleReview(selected.id, 'approve')}
                        type="button"
                      >
                        <Check className="h-4 w-4" />
                        Duyệt & Áp dụng thay đổi
                      </button>
                      <button
                        className="inline-flex items-center gap-1.5 rounded-xl border border-rose-200 bg-rose-50/30 px-5 py-3 text-xs font-extrabold text-rose-700 transition hover:bg-rose-50 active:scale-95"
                        onClick={() => handleReview(selected.id, 'reject')}
                        type="button"
                      >
                        <X className="h-4 w-4" />
                        Từ chối yêu cầu
                      </button>

                      <button
                        className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-5 py-3 text-xs font-extrabold text-[#584140] transition hover:bg-gray-50 active:scale-95"
                        disabled={checkingConflicts[selected.id]}
                        onClick={() => handleConflictCheck(selected.id)}
                        type="button"
                      >
                        {checkingConflicts[selected.id] ? (
                          <>Đang kiểm tra...</>
                        ) : (
                          <>
                            <AlertCircle className="h-4 w-4 text-[#730014]" />
                            Kiểm tra trùng lịch học đề xuất
                          </>
                        )}
                      </button>
                    </div>

                    {/* Review Note Input */}
                    <div className="space-y-2 pt-2">
                      <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Ghi chú phản hồi phê duyệt (không bắt buộc)</label>
                      <textarea
                        className="min-h-[100px] w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
                        onChange={(event) => setReviewNotes((current) => ({ ...current, [selected.id]: event.target.value }))}
                        placeholder="Nhập ghi chú phản hồi, lý do từ chối hoặc hướng dẫn bổ sung cho giảng viên..."
                        value={reviewNotes[selected.id] || ''}
                      />
                    </div>

                    {/* Conflict Check Result Panel */}
                    {conflictResults[selected.id] && (
                      <div className="pt-2">
                        <ConflictPanel conflictResult={conflictResults[selected.id]} />
                      </div>
                    )}
                  </section>
                </div>
              ) : (
                <ClassroomEmptyState
                  description="Hãy chọn một yêu cầu thay đổi ở danh sách bên trái để xem chi tiết và phê duyệt."
                  title="Chưa chọn yêu cầu thay đổi"
                />
              )}
            </div>
          ) : null}
        </section>
      </motion.main>
      </div>
      <CourseFooter />
    </div>
  );
}
