import { useEffect, useMemo, useState } from 'react';
import { BookOpen, Brain, FileQuestion, Plus, RefreshCw, Upload, Wand2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import {
  ContentManagerLoadingState,
  Panel,
  SectionTitle,
  StatusBadge,
} from '../../components/content-manager/ContentManagerUi';

export default function ContentManagerDashboardPage() {
  const [stats, setStats] = useState(null);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadDashboard = async (activeRef = { current: true }) => {
    setLoading(true);
    setError('');
    try {
      const [statsData, coursePage] = await Promise.all([
        courseApi.getManagedCourseStats(),
        courseApi.getManagedOnlineCourses({ page: 0, size: 8 }),
      ]);
      if (!activeRef.current) return;
      setStats(statsData);
      setCourses(coursePage.content || []);
    } catch {
      if (activeRef.current) setError('Chưa tải được dữ liệu quản trị từ backend.');
    } finally {
      if (activeRef.current) setLoading(false);
    }
  };

  useEffect(() => {
    const activeRef = { current: true };
    loadDashboard(activeRef);

    return () => {
      activeRef.current = false;
    };
  }, []);

  const statCards = [
    { label: 'Khóa học online', value: stats?.totalCourses ?? '-', meta: 'Tổng số khóa hiện có' },
    { label: 'Đã xuất bản', value: stats?.publishedCourses ?? '-', meta: 'Đang hiển thị cho học viên' },
    { label: 'Bản nháp', value: stats?.draftCourses ?? '-', meta: 'Cần rà soát thêm' },
    { label: 'Lưu trữ', value: stats?.archivedCourses ?? '-', meta: 'Đã ẩn khỏi học viên' },
    { label: 'Bài học', value: stats?.totalLessons ?? '-', meta: 'Tổng số bài học đang có' },
    { label: 'Lượt ghi danh', value: stats?.totalEnrollments ?? '-', meta: 'Toàn bộ học viên đã đăng ký' },
  ];

  const recentActivity = [...courses]
    .sort((a, b) => new Date(b.updatedAt || b.createdAt || 0) - new Date(a.updatedAt || a.createdAt || 0))
    .slice(0, 4)
    .map((course) => ({
      title: course.title,
      detail: `${formatStatus(course.status)} • ${course.totalLessons || 0} bài học • ${formatPrice(course.price)}`,
      time: formatDate(course.updatedAt || course.createdAt),
      to: `/content-manager/courses/${course.slug}/edit`,
      status: course.status,
    }));

  const overviewBars = useMemo(
    () => [
      { label: 'Nháp', value: Number(stats?.draftCourses || 0), color: 'bg-[#f1c5cb]' },
      { label: 'Xuất bản', value: Number(stats?.publishedCourses || 0), color: 'bg-[#730014]' },
      { label: 'Lưu trữ', value: Number(stats?.archivedCourses || 0), color: 'bg-[#d9b1b6]' },
      { label: 'Bài học', value: Number(stats?.totalLessons || 0), color: 'bg-[#4b0009]' },
    ],
    [stats],
  );

  const maxBarValue = Math.max(...overviewBars.map((item) => item.value), 1);

  if (loading) {
    return <ContentManagerLoadingState message="Đang tải dữ liệu tổng quan nội dung..." />;
  }

  return (
    <div className="space-y-8">
      {error ? (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">
          <span>{error}</span>
          <button
            className="inline-flex items-center gap-2 rounded-xl border border-[#93000a]/25 bg-white/70 px-3 py-2"
            onClick={() => loadDashboard()}
            type="button"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Thử lại
          </button>
        </div>
      ) : null}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {statCards.map((card) => (
          <Panel key={card.label} className="p-5">
            <p className="text-sm font-medium text-[#584140]">{card.label}</p>
            <div className="mt-3 flex items-end justify-between gap-3">
              <p className="font-['Manrope'] text-3xl font-extrabold text-[#4b0009]">{card.value}</p>
              <span className="rounded-full bg-[#fff2f3] px-3 py-1 text-xs font-semibold text-[#730014]">
                {card.meta}
              </span>
            </div>
          </Panel>
        ))}
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.45fr_0.9fr]">
        <Panel className="p-6">
          <SectionTitle title="Thao tác nhanh" />
          <div className="mt-5 grid gap-3 md:grid-cols-2">
            <QuickLink icon={Plus} label="Tạo khóa học online" to="/content-manager/courses?new=1" />
            <QuickLink icon={Upload} label="Tải học liệu lên" to="/content-manager/materials" />
            <QuickLink icon={Brain} label="Quản lý flashcard" to="/content-manager/flashcards" />
            <QuickLink icon={FileQuestion} label="Quản lý đề thi thử" to="/content-manager/mock-exams" />
          </div>
        </Panel>

        <Panel className="p-6">
          <SectionTitle title="Điểm cần chú ý" />
          <div className="mt-5 space-y-4">
            <AttentionCard
              label="Khóa học nháp"
              value={stats?.draftCourses ?? 0}
              note="Nên kiểm tra lại phần đầu ra, học liệu và trạng thái xuất bản."
            />
            <AttentionCard
              label="Khóa học lưu trữ"
              value={stats?.archivedCourses ?? 0}
              note="Đây là nhóm đã ẩn khỏi học viên nhưng vẫn còn trong kho quản lý."
            />
            <AttentionCard
              label="Mật độ bài học"
              value={stats?.totalLessons ?? 0}
              note="Con số này cho bạn biết kho nội dung hiện tại dày hay còn mỏng."
            />
          </div>
        </Panel>
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.35fr_1fr]">
        <Panel className="p-6">
          <SectionTitle title="Hoạt động khóa học gần đây" />
          <div className="mt-5 space-y-3">
            {recentActivity.length ? (
              recentActivity.map((item) => (
                <Link
                  key={item.title}
                  className="flex items-start gap-4 rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] p-4 transition hover:border-[#730014]/30 hover:bg-white"
                  to={item.to}
                >
                  <div className="mt-0.5 flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]">
                    <BookOpen className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-semibold text-[#1a1c1c]">{item.title}</p>
                      <StatusBadge label={item.status} />
                    </div>
                    <p className="mt-1 text-sm text-[#584140]">{item.detail}</p>
                  </div>
                  <span className="text-xs font-semibold uppercase tracking-[0.18em] text-[#9c7f7c]">
                    {item.time}
                  </span>
                </Link>
              ))
            ) : (
              <p className="rounded-2xl border border-dashed border-[#dfbfbd] p-5 text-sm text-[#584140]">
                Chưa có hoạt động khóa học nào.
              </p>
            )}
          </div>
        </Panel>

        <Panel className="p-6">
          <SectionTitle title="Phân bổ nội dung hiện tại" />
          <div className="mt-5 rounded-[24px] border border-[#f0e3e4] bg-[#fffafb] p-5">
            <div className="space-y-4">
              {overviewBars.map((item) => (
                <div key={item.label} className="space-y-2">
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-semibold text-[#4b0009]">{item.label}</span>
                    <span className="font-bold text-[#1a1c1c]">{item.value}</span>
                  </div>
                  <div className="h-3 overflow-hidden rounded-full bg-[#f1e3e4]">
                    <div
                      className={`h-full rounded-full ${item.color}`}
                      style={{ width: `${Math.max((item.value / maxBarValue) * 100, item.value ? 10 : 0)}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-6 grid gap-3 sm:grid-cols-2">
              <MiniInfoCard icon={BookOpen} label="Khóa đang mở" value={stats?.publishedCourses ?? 0} />
              <MiniInfoCard icon={FileQuestion} label="Lượt ghi danh" value={stats?.totalEnrollments ?? 0} />
            </div>
          </div>
        </Panel>
      </section>
    </div>
  );
}

function QuickLink({ icon: Icon, label, to }) {
  return (
    <Link
      className="flex items-center gap-3 rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] px-4 py-4 text-sm font-semibold text-[#1a1c1c] transition hover:border-[#730014]/30 hover:bg-white"
      to={to}
    >
      <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]">
        <Icon className="h-4 w-4" />
      </span>
      {label}
    </Link>
  );
}

function AttentionCard({ label, value, note }) {
  return (
    <div className="rounded-2xl border border-[#f0d8db] bg-[#fff8f8] p-4">
      <div className="flex items-center justify-between gap-3">
        <p className="font-semibold text-[#4b0009]">{label}</p>
        <span className="rounded-full bg-white px-3 py-1 text-sm font-extrabold text-[#730014]">{value}</span>
      </div>
      <p className="mt-2 text-sm text-[#584140]">{note}</p>
    </div>
  );
}

function MiniInfoCard({ icon: Icon, label, value }) {
  return (
    <div className="rounded-2xl border border-[#f0e3e4] bg-white p-4">
      <div className="flex items-center gap-3">
        <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]">
          <Icon className="h-4 w-4" />
        </span>
        <div>
          <p className="text-xs uppercase tracking-[0.14em] text-[#8b706e]">{label}</p>
          <p className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">{value}</p>
        </div>
      </div>
    </div>
  );
}

function formatPrice(value) {
  const amount = Number(value || 0);
  if (!amount) return 'Miễn phí';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
  });
}

function formatStatus(value) {
  const labels = {
    DRAFT: 'Nháp',
    PUBLISHED: 'Đã xuất bản',
    ARCHIVED: 'Lưu trữ',
  };
  return labels[String(value || '').toUpperCase()] || value || 'Nháp';
}
