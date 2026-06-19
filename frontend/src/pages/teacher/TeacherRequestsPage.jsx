import { useEffect, useMemo, useState } from 'react';
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

const requestFilters = [
  { id: 'all', label: 'Tất cả' },
  { id: 'pending', label: 'Chờ duyệt' },
  { id: 'approved', label: 'Đã duyệt' },
  { id: 'rejected', label: 'Từ chối' },
];

export default function TeacherRequestsPage() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeFilter, setActiveFilter] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');

  const loadRequests = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getMyChangeRequests();
      setRequests(data);
    } catch (err) {
      setRequests([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải yêu cầu của bạn.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRequests();
  }, []);

  // Filter and search requests
  const filteredRequests = useMemo(() => {
    return requests.filter((req) => {
      const matchesSearch =
        req.classroomTitle?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        req.reason?.toLowerCase().includes(searchQuery.toLowerCase());

      if (!matchesSearch) return false;

      if (activeFilter === 'pending') return req.status === 'PENDING';
      if (activeFilter === 'approved') return ['APPROVED', 'APPLIED'].includes(req.status);
      if (activeFilter === 'rejected') return req.status === 'REJECTED';
      return true;
    });
  }, [requests, activeFilter, searchQuery]);

  // Calculate stats for PageHero
  const stats = useMemo(() => {
    if (!requests.length) return [];
    const pendingCount = requests.filter((r) => r.status === 'PENDING').length;
    const approvedCount = requests.filter((r) => ['APPROVED', 'APPLIED'].includes(r.status)).length;
    const rejectedCount = requests.filter((r) => r.status === 'REJECTED').length;

    return [
      { label: 'Tổng yêu cầu', value: requests.length, icon: Settings, color: 'blue' },
      { label: 'Chờ duyệt', value: pendingCount, icon: Clock, color: pendingCount > 0 ? 'amber' : 'blue' },
      { label: 'Đã duyệt', value: approvedCount, icon: CheckCircle2, color: 'emerald' },
      { label: 'Từ chối', value: rejectedCount, icon: XCircle, color: rejectedCount > 0 ? 'rose' : 'blue' },
    ];
  }, [requests]);

  return (
    <div className="course-page flex min-h-[100dvh] flex-col bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />
      <main className="mx-auto flex w-full max-w-[1320px] flex-1 flex-col px-4 pb-[80px] pt-8 md:px-10 space-y-8">
        {/* Page Hero with operational stats */}
        <PageHero
          title="Yêu cầu thay đổi lịch trình"
          subtitle="Theo dõi tiến trình phê duyệt các đề xuất thay đổi lịch học, phòng học hoặc giáo viên thay thế từ Training Manager."
          stats={stats}
          action={
            <Link
              className="inline-flex items-center gap-1.5 rounded-2xl border border-[#dfbfbd] bg-white px-5 py-3 text-sm font-extrabold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95"
              to="/teacher"
            >
              <ArrowLeft className="h-4 w-4" />
              Quay lại cockpit giảng dạy
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
          {!loading && !error && !filteredRequests.length ? (
            <ClassroomEmptyState
              description="Không tìm thấy yêu cầu thay đổi nào khớp với bộ lọc."
              title="Chưa có yêu cầu"
            />
          ) : null}

          {!loading && !error && filteredRequests.length ? (
            <div className="grid gap-6 md:grid-cols-2">
              {filteredRequests.map((request) => (
                <article
                  key={request.id}
                  className="flex flex-col overflow-hidden rounded-[28px] border border-[#dfbfbd]/20 bg-white p-6 shadow-sm hover:border-[#dfbfbd]/40 transition space-y-6"
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
                </article>
              ))}
            </div>
          ) : null}
        </div>
      </main>
      <CourseFooter />
    </div>
  );
}
