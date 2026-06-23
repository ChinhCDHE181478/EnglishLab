import { useEffect, useMemo, useState } from 'react';
import { BarChart3, BookOpen, Layers3, RefreshCw, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { ContentManagerLoadingState, Panel, SectionTitle, StatusBadge } from '../../components/content-manager/ContentManagerUi';

export default function ContentManagerAnalyticsPage() {
  const [stats, setStats] = useState(null);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [statsData, coursePage] = await Promise.all([
        courseApi.getManagedCourseStats(),
        courseApi.getManagedOnlineCourses({ page: 0, size: 500 }),
      ]);
      setStats(statsData);
      setCourses(coursePage.content || []);
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

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={BookOpen} label="Khóa học" value={stats?.totalCourses ?? 0} />
        <StatCard icon={Layers3} label="Bài học" value={stats?.totalLessons ?? 0} />
        <StatCard icon={Users} label="Lượt ghi danh" value={stats?.totalEnrollments ?? 0} />
        <StatCard icon={BarChart3} label="Tỷ lệ xuất bản" value={formatPercent(stats?.publishedCourses, stats?.totalCourses)} />
      </section>

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

function formatPercent(value, total) {
  if (!total) return '0%';
  return `${Math.round((Number(value || 0) / Number(total)) * 100)}%`;
}

function formatDate(value) {
  if (!value) return 'Chưa có';
  return new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}
