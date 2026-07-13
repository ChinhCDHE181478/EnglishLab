import { useEffect, useMemo, useState } from 'react';
import { BarChart3, BookOpen, Layers3, RefreshCw, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import paymentApi from '../../api/paymentApi';
import { ContentManagerLoadingState, Panel, SectionTitle, StatusBadge } from '../../components/content-manager/ContentManagerUi';

export default function ContentManagerAnalyticsPage() {
  const [stats, setStats] = useState(null);
  const [revenue, setRevenue] = useState(null);
  const [courses, setCourses] = useState([]);
  const [paidOrders, setPaidOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [refundTarget, setRefundTarget] = useState(null);
  const [refundReason, setRefundReason] = useState('');
  const [refunding, setRefunding] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const loadRevenueAnalytics = async () => {
        try {
          return await paymentApi.getRevenueAnalytics();
        } catch {
          return null;
        }
      };
      const loadPaidOrders = async () => {
        try {
          return await paymentApi.listStaffOrders('PAID');
        } catch {
          return [];
        }
      };

      const [statsData, coursePage, revenueData, ordersData] = await Promise.all([
        courseApi.getManagedCourseStats(),
        courseApi.getManagedOnlineCourses({ page: 0, size: 500 }),
        loadRevenueAnalytics(),
        loadPaidOrders(),
      ]);
      setStats(statsData);
      setRevenue(revenueData);
      setCourses(coursePage.content || []);
      setPaidOrders(Array.isArray(ordersData) ? ordersData.filter((order) => order.refundable) : []);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được dữ liệu phân tích nội dung.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const categoryRows = useMemo(() => {
    const counts = new Map();
    courses.forEach((course) => {
      const key = course.categoryName || course.category || 'Chưa phân loại';
      counts.set(key, (counts.get(key) || 0) + 1);
    });
    return Array.from(counts.entries())
      .map(([label, value]) => ({ label, value }))
      .sort((left, right) => right.value - left.value);
  }, [courses]);

  const recentCourses = useMemo(
    () => [...courses]
      .sort((left, right) => new Date(right.updatedAt || 0) - new Date(left.updatedAt || 0))
      .slice(0, 8),
    [courses],
  );

  const handleConfirmRefund = async () => {
    if (!refundTarget?.orderCode || !refundReason.trim()) return;
    setRefunding(true);
    setActionMessage('');
    try {
      await paymentApi.refundCourseOrder(refundTarget.orderCode, refundReason.trim());
      setActionMessage(`Đã hoàn tiền đơn #${refundTarget.orderCode} trên hệ thống (tiền PayOS xử lý thủ công ngoài app).`);
      setRefundTarget(null);
      setRefundReason('');
      await loadData();
    } catch (err) {
      setActionMessage(err?.response?.data?.message || 'Không hoàn tiền được đơn này.');
    } finally {
      setRefunding(false);
    }
  };

  if (loading && !stats) {
    return <ContentManagerLoadingState message="Đang tải dữ liệu phân tích nội dung..." />;
  }

  const statusRows = [
    { label: 'Bản nháp', value: Number(stats?.draftCourses || 0), color: 'bg-[#d98c99]' },
    { label: 'Đã xuất bản', value: Number(stats?.publishedCourses || 0), color: 'bg-[#730014]' },
    { label: 'Lưu trữ', value: Number(stats?.archivedCourses || 0), color: 'bg-[#b9a4a7]' },
  ];
  const maxStatus = Math.max(...statusRows.map((item) => item.value), 1);
  const maxCategory = Math.max(...categoryRows.map((item) => item.value), 1);

  return (
    <div className="space-y-6">
      <div className="flex justify-end">
        <button className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm font-bold text-[#730014]" onClick={loadData} type="button">
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </button>
      </div>

      {error ? (
        <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">
          {error}
        </div>
      ) : null}

      {actionMessage ? (
        <div className="rounded-2xl border border-[#dfbfbd]/40 bg-white px-5 py-4 text-sm font-semibold text-[#584140]">
          {actionMessage}
        </div>
      ) : null}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={BookOpen} label="Khóa học" value={stats?.totalCourses ?? 0} />
        <StatCard icon={Layers3} label="Bài học" value={stats?.totalLessons ?? 0} />
        <StatCard icon={Users} label="Lượt ghi danh" value={stats?.totalEnrollments ?? 0} />
        <StatCard icon={BarChart3} label="Doanh thu (đã thanh toán)" value={`${Number(revenue?.totalRevenueVnd || 0).toLocaleString('vi-VN')} đ`} />
      </section>

      {revenue ? (
        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <StatCard icon={BarChart3} label="Đơn đã thanh toán" value={revenue.paidOrders ?? 0} />
          <StatCard icon={BarChart3} label="Đơn đang chờ" value={revenue.pendingOrders ?? 0} />
          <StatCard icon={BarChart3} label="Đơn thất bại/hoàn" value={revenue.failedOrders ?? 0} />
          <StatCard icon={BarChart3} label="Tổng giảm giá" value={`${Number(revenue.totalDiscountVnd || 0).toLocaleString('vi-VN')} đ`} />
        </section>
      ) : null}

      <Panel className="overflow-hidden">
        <div className="border-b border-[#f0e3e4] px-6 py-5">
          <SectionTitle title="Hoàn tiền đơn khóa học (PayOS)" />
          <p className="mt-2 text-sm text-[#584140]">
            Đánh dấu hoàn trên hệ thống, hủy quyền học và hoàn coupon. Tiền PayOS xử lý thủ công ngoài app.
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full text-left">
            <thead className="bg-[#fbf3f4] text-xs uppercase tracking-[0.16em] text-[#8e7371]">
              <tr>
                {['Mã đơn', 'Học viên', 'Khóa học', 'Số tiền', 'Thanh toán', 'Thao tác'].map((heading) => (
                  <th key={heading} className="px-5 py-4 font-semibold">{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#f0e3e4]">
              {paidOrders.length ? paidOrders.map((order) => (
                <tr key={order.orderCode}>
                  <td className="px-5 py-4 font-semibold">#{order.orderCode}</td>
                  <td className="px-5 py-4 text-sm">
                    <div className="font-semibold text-[#2b2828]">{order.studentName || '—'}</div>
                    <div className="text-[#8b706e]">{order.studentEmail || ''}</div>
                  </td>
                  <td className="px-5 py-4 text-sm">{(order.courseTitles || []).join(' · ') || order.description || '—'}</td>
                  <td className="px-5 py-4 text-sm font-bold">{Number(order.amount || 0).toLocaleString('vi-VN')} đ</td>
                  <td className="px-5 py-4 text-sm">{formatDateTime(order.paidAt)}</td>
                  <td className="px-5 py-4">
                    <button
                      className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-extrabold text-rose-700 hover:bg-rose-100"
                      onClick={() => {
                        setRefundTarget(order);
                        setRefundReason('');
                      }}
                      type="button"
                    >
                      Hoàn tiền
                    </button>
                  </td>
                </tr>
              )) : (
                <tr>
                  <td className="px-5 py-10 text-sm text-[#584140]" colSpan={6}>
                    Không có đơn khóa học PAID nào có thể hoàn.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>

      <section className="grid gap-6 xl:grid-cols-2">
        <Panel className="p-6">
          <SectionTitle title="Trạng thái khóa học" />
          <div className="mt-6 space-y-5">
            {statusRows.map((item) => (
              <ChartRow key={item.label} color={item.color} label={item.label} max={maxStatus} value={item.value} />
            ))}
          </div>
        </Panel>

        <Panel className="p-6">
          <SectionTitle title="Phân bổ theo danh mục" />
          <div className="mt-6 space-y-5">
            {categoryRows.length ? categoryRows.map((item) => (
              <ChartRow key={item.label} color="bg-[#4b0009]" label={item.label} max={maxCategory} value={item.value} />
            )) : (
              <p className="text-sm text-[#584140]">Chưa có dữ liệu danh mục.</p>
            )}
          </div>
        </Panel>
      </section>

      <Panel className="overflow-hidden">
        <div className="border-b border-[#f0e3e4] px-6 py-5">
          <SectionTitle title="Khóa học cập nhật gần đây" />
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full text-left">
            <thead className="bg-[#fbf3f4] text-xs uppercase tracking-[0.16em] text-[#8e7371]">
              <tr>
                {['Khóa học', 'Danh mục', 'Bài học', 'Trạng thái', 'Cập nhật', 'Thao tác'].map((heading) => (
                  <th key={heading} className="px-5 py-4 font-semibold">{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#f0e3e4]">
              {recentCourses.length ? recentCourses.map((course) => (
                <tr key={course.id}>
                  <td className="px-5 py-4 font-semibold">{course.title}</td>
                  <td className="px-5 py-4 text-sm">{course.categoryName || course.category}</td>
                  <td className="px-5 py-4 text-sm">{course.totalLessons || 0}</td>
                  <td className="px-5 py-4"><StatusBadge label={course.status} /></td>
                  <td className="px-5 py-4 text-sm">{formatDate(course.updatedAt)}</td>
                  <td className="px-5 py-4">
                    <Link className="text-sm font-bold text-[#730014] hover:underline" to={`/content-manager/courses/${course.slug}/edit`}>
                      Mở khóa học
                    </Link>
                  </td>
                </tr>
              )) : (
                <tr><td className="px-5 py-10 text-sm text-[#584140]" colSpan={6}>Chưa có khóa học để phân tích.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>

      {refundTarget ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/40" onClick={() => !refunding && setRefundTarget(null)} />
          <div className="relative z-10 w-full max-w-md rounded-xl border border-gray-100 bg-white p-6 shadow-2xl">
            <h3 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">Hoàn tiền đơn khóa học</h3>
            <p className="mt-3 text-sm leading-6 text-[#584140]">
              Hoàn đơn #{refundTarget.orderCode} của {refundTarget.studentEmail}. Hệ thống sẽ hủy quyền khóa học và hoàn coupon.
              Tiền PayOS cần xử lý thủ công.
            </p>
            <textarea
              className="mt-4 min-h-[96px] w-full rounded-2xl border border-[#dfbfbd]/60 px-4 py-3 text-sm outline-none focus:border-[#730014]"
              onChange={(event) => setRefundReason(event.target.value)}
              placeholder="Lý do hoàn tiền (bắt buộc)"
              value={refundReason}
            />
            <div className="mt-6 flex justify-end gap-3">
              <button
                className="rounded-2xl border border-gray-200 px-5 py-3 text-sm font-extrabold text-[#584140]"
                disabled={refunding}
                onClick={() => setRefundTarget(null)}
                type="button"
              >
                Đóng
              </button>
              <button
                className="rounded-2xl bg-[#93000a] px-5 py-3 text-sm font-extrabold text-white disabled:opacity-60"
                disabled={refunding || !refundReason.trim()}
                onClick={handleConfirmRefund}
                type="button"
              >
                {refunding ? 'Đang hoàn...' : 'Xác nhận hoàn tiền'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function StatCard({ icon: Icon, label, value }) {
  return (
    <Panel className="p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-[#584140]">{label}</p>
          <p className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#4b0009]">{value}</p>
        </div>
        <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]">
          <Icon className="h-5 w-5" />
        </span>
      </div>
    </Panel>
  );
}

function ChartRow({ color, label, max, value }) {
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between text-sm">
        <span className="font-semibold text-[#4b0009]">{label}</span>
        <span className="font-bold">{value}</span>
      </div>
      <div className="h-3 overflow-hidden rounded-full bg-[#f1e3e4]">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${value ? Math.max((value / max) * 100, 8) : 0}%` }} />
      </div>
    </div>
  );
}

function formatDate(value) {
  if (!value) return 'Chưa có';
  return new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatDateTime(value) {
  if (!value) return '—';
  return new Date(value).toLocaleString('vi-VN');
}
