import { useDeferredValue, useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import {
  Calendar,
  Clock,
  MapPin,
  Video,
  Award,
  CheckCircle2,
  XCircle,
  AlertCircle,
  ArrowLeft,
  Settings,
  User,
  MessageSquare,
  Search,
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
  PageHero,
  StatusBadge,
  RequestStatusTimeline,
} from '../../components/classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDateTime } from '../../utils/classroomHelpers';
import { PAGE_BODY_CLASS, PAGE_HEADER_CLASS, PAGE_MAIN_STACK_CLASS, PAGE_SHELL_CLASS } from '../../utils/pageLayout';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import { EMPTY_PAGE, normalizePage, pageParams } from '../../utils/pagination';

const requestFilters = [
  { id: 'all', label: 'Tất cả' },
  { id: 'pending', label: 'Chờ duyệt' },
  { id: 'approved', label: 'Đã duyệt' },
  { id: 'rejected', label: 'Từ chối' },
];

export default function TeacherRequestsPage() {
  const [requests, setRequests] = useState([]);
  const [pageResult, setPageResult] = useState(EMPTY_PAGE);
  const [requestStats, setRequestStats] = useState({ total: 0, pending: 0, approved: 0, rejected: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeFilter, setActiveFilter] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const deferredSearchQuery = useDeferredValue(searchQuery.trim());
  const resetKey = `${activeFilter}-${deferredSearchQuery}`;
  const { page, setPage, totalPages, pageItems: paginatedRequests, totalItems } = usePagination(
    requests,
    4,
    resetKey,
    pageResult,
  );

  const loadRequests = async () => {
    setLoading(true);
    setError('');
    try {
      const [pagePayload, statsPayload] = await Promise.all([
        classroomApi.getMyChangeRequestsPage(pageParams(page, 4, {
          status: activeFilter === 'all' ? undefined : activeFilter.toUpperCase(),
          keyword: deferredSearchQuery || undefined,
        })),
        classroomApi.getMyChangeRequestStats(),
      ]);
      const result = normalizePage(pagePayload);
      setPageResult(result);
      setRequests(result.content);
      setRequestStats({
        total: Number(statsPayload?.total || 0),
        pending: Number(statsPayload?.pending || 0),
        approved: Number(statsPayload?.approved || 0),
        rejected: Number(statsPayload?.rejected || 0),
      });
    } catch (err) {
      setRequests([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải yêu cầu của bạn.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRequests();
  }, [activeFilter, deferredSearchQuery, page]);

  // Calculate stats for PageHero
  const stats = useMemo(() => {
    if (!requestStats.total) return [];
    const pendingCount = requestStats.pending;
    const approvedCount = requestStats.approved;
    const rejectedCount = requestStats.rejected;

    return [
      { label: 'Tổng yêu cầu', value: requestStats.total, icon: Settings, color: 'blue' },
      { label: 'Chờ duyệt', value: pendingCount, icon: Clock, color: pendingCount > 0 ? 'amber' : 'blue' },
      { label: 'Đã duyệt', value: approvedCount, icon: CheckCircle2, color: 'emerald' },
      { label: 'Từ chối', value: rejectedCount, icon: XCircle, color: rejectedCount > 0 ? 'rose' : 'blue' },
    ];
  }, [requestStats]);

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
          title="Theo dõi yêu cầu"
          subtitle="Theo dõi tiến trình phê duyệt các đề xuất thay đổi lịch, phòng học hoặc giáo viên thay thế từ điều phối đào tạo."
          stats={stats}
          action={
            <Link
              className="inline-flex items-center gap-1.5 rounded-2xl border border-[#dfbfbd] bg-white px-5 py-3 text-sm font-extrabold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95"
              to="/teacher"
            >
              <ArrowLeft className="h-4 w-4" />
              Quay lại trang giảng dạy
            </Link>
          }
        />

        <div className="space-y-6">
          {/* Search and Filters */}
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <ClassroomTabBar activeTab={activeFilter} onChange={setActiveFilter} tabs={requestFilters} />

            <div className="relative w-full md:w-72">
              <input
                type="text"
                className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-white py-3.5 pl-11 pr-4 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
                placeholder="Tìm theo tên lớp hoặc lý do..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              <Search className="absolute left-4 top-3.5 h-4.5 w-4.5 text-[#8b706e]" />
            </div>
          </div>

          {/* Requests List */}
          {loading ? <ClassroomLoadingState message="Đang tải danh sách yêu cầu..." /> : null}
          {!loading && error ? <ClassroomErrorState message={error} onRetry={loadRequests} /> : null}
          {!loading && !error && !requests.length ? (
            <ClassroomEmptyState
              description="Không tìm thấy yêu cầu thay đổi nào khớp với bộ lọc."
              title="Chưa có yêu cầu"
            />
          ) : null}

          {!loading && !error && requests.length ? (
            <div className="space-y-6">
              <div className="grid gap-6 md:grid-cols-2">
                {paginatedRequests.map((request, idx) => (
                  <motion.article
                    key={request.id}
                    initial={{ opacity: 0, y: 16 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.3, delay: Math.min(idx * 0.06, 0.36), ease: 'easeOut' }}
                    className="flex flex-col overflow-hidden rounded-xl border border-[#e5e7eb] bg-white p-5 transition hover:border-[#d0c4c3] hover:shadow-sm space-y-5"
                  >
                    {/* Card Header */}
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <span className="inline-flex rounded-full bg-[#fff1f3] px-3 py-1 text-xs font-extrabold text-[#730014]">
                          {request.requestTypeLabel || request.requestType || 'Yêu cầu'}
                        </span>
                        <h3 className="mt-3 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">
                          {request.classroomTitle || `Lớp học #${request.classroomOfferingId}`}
                        </h3>
                      </div>
                      <StatusBadge status={request.status} />
                    </div>

                    {/* Reason Block */}
                    <div className="rounded-2xl border border-gray-100 bg-gray-50/30 p-4 space-y-2">
                      <p className="text-[10px] font-bold text-[#8b706e] uppercase tracking-wider flex items-center gap-1">
                        <MessageSquare className="h-3.5 w-3.5 text-[#730014]" />
                        Lý do đề xuất thay đổi
                      </p>
                      <p className="text-sm text-[#584140] whitespace-pre-wrap">
                        {request.reason || 'Không có mô tả chi tiết.'}
                      </p>
                    </div>

                    {/* Request Timeline */}
                    <RequestStatusTimeline
                      createdAt={request.createdAt}
                      reviewNote={request.reviewNote}
                      reviewedAt={request.reviewedAt || request.updatedAt}
                      reviewerName={request.reviewerName}
                      status={request.status}
                    />

                    {/* Card Footer */}
                    <div className="pt-4 border-t border-gray-50 flex items-center justify-between text-[10px] font-bold text-gray-400">
                      <span>Yêu cầu ID: #{request.id}</span>
                      <span>Gửi lúc: {formatClassroomDateTime(request.createdAt)}</span>
                    </div>
                  </motion.article>
                ))}
              </div>
              <div className="flex justify-center pt-4">
                <Pagination
                  page={page}
                  totalPages={totalPages}
                  onChange={setPage}
                  totalItems={totalItems}
                  pageSize={4}
                />
              </div>
            </div>
          ) : null}
        </div>
      </motion.main>
      </div>
      <CourseFooter />
    </div>
  );
}
