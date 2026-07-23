import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  AlertTriangle,
  ArrowRight,
  CalendarDays,
  CheckSquare,
  ClipboardList,
  Users,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import { ClassroomEmptyState, ClassroomErrorState, ClassroomLoadingState } from '../../components/classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDate, formatClassroomDateTime } from '../../utils/classroomHelpers';

export default function TrainingManagerDashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [enrollmentRequests, setEnrollmentRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadDashboard = async () => {
    setLoading(true);
    setError('');
    try {
      const [data, requestData] = await Promise.all([
        classroomApi.getTrainingManagerDashboard(),
        enrollmentRequestApi.listForStaff('ALL'),
      ]);
      setDashboard(data);
      setEnrollmentRequests(requestData);
    } catch (err) {
      setDashboard(null);
      setEnrollmentRequests([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải bảng điều khiển vận hành.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  if (loading) {
    return <ClassroomLoadingState message="Đang tải việc cần làm hôm nay..." />;
  }

  if (error) {
    return <ClassroomErrorState message={error} onRetry={loadDashboard} />;
  }

  const newRequestCount = enrollmentRequests.filter((item) => (
    ['SUBMITTED', 'UNDER_STAFF_REVIEW'].includes(item.status)
  )).length;
  const waitingForClassCount = enrollmentRequests.filter((item) => item.status === 'WAITING_FOR_CLASS').length;
  const assignedCount = enrollmentRequests.filter((item) => item.status === 'CLASS_ASSIGNED').length;
  const actionableRequests = enrollmentRequests.filter((item) => (
    ['SUBMITTED', 'UNDER_STAFF_REVIEW', 'WAITING_FOR_CLASS'].includes(item.status)
  ));
  const changeActionItems = (dashboard?.actionItems || []).filter((item) => item.changeRequestId);
  const hasWork = actionableRequests.length + changeActionItems.length > 0;

  return (
    <motion.div
      className="space-y-6"
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.32, ease: 'easeOut' }}
    >
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          href="/staff/enrollment-requests"
          icon={ClipboardList}
          label="Yêu cầu mới"
          value={newRequestCount}
        />
        <SummaryCard
          href="/staff/enrollment-requests"
          icon={Users}
          label="Chờ xếp lớp"
          value={waitingForClassCount}
        />
        <SummaryCard
          href="/staff/enrollment-requests"
          icon={CalendarDays}
          label="Đã xếp lớp"
          value={assignedCount}
        />
        <SummaryCard
          href="/staff/requests"
          icon={CheckSquare}
          label="Yêu cầu vận hành"
          value={dashboard?.pendingChangeRequestCount ?? 0}
        />
      </section>

      <div className="grid gap-6 xl:grid-cols-[1.4fr_1fr]">
        <section className="rounded-xl border border-[#e5e7eb] bg-white p-5 shadow-sm">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h2 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Việc cần làm</h2>
            <Link className="text-xs font-bold text-[#730014] hover:underline" to="/staff/enrollment-requests">
              Xem yêu cầu đăng ký
            </Link>
          </div>

          {!hasWork ? (
            <ClassroomEmptyState
              description="Không có học viên chờ tư vấn, chờ xếp lớp hoặc yêu cầu vận hành cần xử lý."
              title="Hôm nay không có việc khẩn"
            />
          ) : (
            <div className="space-y-2">
              {actionableRequests.slice(0, 12).map((item) => (
                <Link
                  className="flex items-start justify-between gap-3 rounded-2xl border border-gray-100 bg-[#fffafb]/60 px-4 py-3 transition hover:border-[#dfbfbd]/50 hover:bg-[#fff3f4]"
                  key={`enrollment-request-${item.id}`}
                  to="/staff/enrollment-requests"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-extrabold text-[#2b2828]">
                      {item.contactName || item.learnerName || item.contactEmail || item.learnerEmail}
                    </p>
                    <p className="mt-0.5 text-xs text-[#8b706e]">{item.statusLabel || 'Yêu cầu đăng ký'}</p>
                    {item.createdAt ? (
                      <p className="mt-1 text-[10px] text-gray-400">{formatClassroomDateTime(item.createdAt)}</p>
                    ) : null}
                  </div>
                  <ArrowRight className="mt-1 h-4 w-4 flex-shrink-0 text-[#730014]" />
                </Link>
              ))}
              {changeActionItems.map((item) => (
                <Link
                  className="flex items-start justify-between gap-3 rounded-2xl border border-gray-100 bg-[#fffafb]/60 px-4 py-3 transition hover:border-[#dfbfbd]/50 hover:bg-[#fff3f4]"
                  key={`${item.kind}-${item.enrollmentId || item.changeRequestId}`}
                  to={item.href || '#'}
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-extrabold text-[#2b2828]">{item.title}</p>
                    <p className="mt-0.5 text-xs text-[#8b706e]">{item.subtitle}</p>
                    {item.createdAt ? (
                      <p className="mt-1 text-[10px] text-gray-400">{formatClassroomDateTime(item.createdAt)}</p>
                    ) : null}
                  </div>
                  <ArrowRight className="mt-1 h-4 w-4 flex-shrink-0 text-[#730014]" />
                </Link>
              ))}
            </div>
          )}
        </section>

        <section className="rounded-xl border border-[#e5e7eb] bg-white p-5 shadow-sm">
          <h2 className="mb-4 font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Lớp cần chú ý</h2>
          {(dashboard?.classroomAlerts || []).length ? (
            <div className="space-y-3">
              {dashboard.classroomAlerts.map((alert) => (
                <Link
                  className="block rounded-2xl border border-amber-100 bg-amber-50/40 p-4 transition hover:bg-amber-50"
                  key={`${alert.classroomOfferingId}-${alert.alertType}`}
                  to={alert.href || `/staff/classrooms/${alert.classroomOfferingId}`}
                >
                  <div className="flex items-start gap-3">
                    <AlertTriangle className="mt-0.5 h-4 w-4 flex-shrink-0 text-amber-700" />
                    <div>
                      <p className="text-sm font-extrabold text-[#2b2828]">{alert.title}</p>
                      <p className="mt-1 text-xs leading-5 text-amber-900">{alert.alertMessage}</p>
                      {alert.startDate ? (
                        <p className="mt-1 text-[10px] text-amber-800/80">
                          Khai giảng: {formatClassroomDate(alert.startDate)}
                          {' · '}
                          {alert.enrolledCount}/{alert.maxCapacity} chỗ
                          {' · '}
                          {alert.sessionCount} buổi
                        </p>
                      ) : null}
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <p className="text-sm text-[#8b706e]">Không có lớp nào cần can thiệp gấp trong 2 tuần tới.</p>
          )}
        </section>
      </div>
    </motion.div>
  );
}

function SummaryCard({ icon: Icon, label, value, href }) {
  return (
    <Link
      className="rounded-xl border border-[#e5e7eb] bg-white p-4 shadow-sm transition hover:border-[#dfbfbd]/60 hover:shadow-md"
      to={href}
    >
      <div className="flex items-center justify-between gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#fff1f3] text-[#730014]">
          <Icon className="h-5 w-5" />
        </div>
        <span className="font-['Manrope'] text-2xl font-black text-[#2b2828]">{value}</span>
      </div>
      <p className="mt-3 text-xs font-bold uppercase tracking-wider text-[#8b706e]">{label}</p>
    </Link>
  );
}
