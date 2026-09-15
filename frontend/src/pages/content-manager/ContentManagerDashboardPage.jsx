import { useEffect, useMemo, useState } from 'react';
import {
  Archive,
  ArrowRight,
  BookOpen,
  Brain,
  CheckCircle2,
  FileQuestion,
  Layers3,
  Plus,
  RefreshCw,
  Upload,
  Users,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import {
  ContentManagerLoadingState,
  Panel,
  SectionTitle,
} from '../../components/content-manager/ContentManagerUi';
import { ManagerStatusBadge } from '../../components/content-manager/ManagerListUi';
import ManagementToast from '../../components/ui/ManagementToast';
import { getContentManagerError } from '../../utils/contentManagerFeedback';

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
    } catch (err) {
      if (activeRef.current) setError(getContentManagerError(err, 'Chưa tải được dữ liệu quản trị từ backend.'));
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
    { label: 'Khóa học Online', value: stats?.totalCourses ?? 0, icon: BookOpen, iconClassName: 'bg-[#fff1f2] text-[#730014]' },
    { label: 'Đã xuất bản', value: stats?.publishedCourses ?? 0, icon: CheckCircle2, iconClassName: 'bg-emerald-50 text-emerald-700' },
    { label: 'Bài học', value: stats?.totalLessons ?? 0, icon: Layers3, iconClassName: 'bg-amber-50 text-amber-700' },
    { label: 'Lượt ghi danh', value: stats?.totalEnrollments ?? 0, icon: Users, iconClassName: 'bg-sky-50 text-sky-700' },
  ];

  const recentActivity = [...courses]
    .sort((a, b) => new Date(b.updatedAt || b.createdAt || 0) - new Date(a.updatedAt || a.createdAt || 0))
    .slice(0, 4)
    .map((course) => ({
      title: course.title,
      detail: `${course.totalLessons || 0} bài học · ${formatPrice(course.price)}`,
      time: formatDate(course.updatedAt || course.createdAt),
      to: `/content-manager/courses/${course.slug || course.id}/edit`,
      status: course.status,
    }));

  const overviewBars = useMemo(
    () => [
      { label: 'Bản nháp', value: Number(stats?.draftCourses || 0), color: 'bg-amber-500' },
      { label: 'Đã xuất bản', value: Number(stats?.publishedCourses || 0), color: 'bg-emerald-600' },
      { label: 'Lưu trữ', value: Number(stats?.archivedCourses || 0), color: 'bg-slate-500' },
    ],
    [stats],
  );

  const maxBarValue = Math.max(...overviewBars.map((item) => item.value), 1);

  if (loading) {
    return <ContentManagerLoadingState message="Đang tải dữ liệu tổng quan nội dung..." />;
  }

  return (
    <div className="space-y-6">
      <ManagementToast actionLabel="Thử lại" message={error} onAction={() => loadDashboard()} onClose={() => setError('')} />

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {statCards.map((card) => (
          <Panel key={card.label} className="p-5">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">{card.label}</p>
                <p className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#0b1c30]">{card.value}</p>
              </div>
              <span className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl ${card.iconClassName}`}>
                <card.icon aria-hidden="true" className="h-5 w-5" />
              </span>
            </div>
          </Panel>
        ))}
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
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
          <SectionTitle title="Cần xử lý" />
          <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
            <WorkflowLink icon={FileQuestion} label="Khóa học bản nháp" to="/content-manager/courses?status=DRAFT" value={stats?.draftCourses ?? 0} />
            <WorkflowLink icon={Archive} label="Khóa học lưu trữ" to="/content-manager/courses?status=ARCHIVED" value={stats?.archivedCourses ?? 0} />
          </div>
        </Panel>
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.35fr_1fr]">
        <Panel className="p-6">
          <SectionTitle title="Khóa học cập nhật gần đây" />
          <div className="mt-5 space-y-3">
            {recentActivity.length ? (
              recentActivity.map((item) => (
                <Link
                  key={`${item.to}-${item.title}`}
                  className="flex min-h-[88px] items-start gap-4 rounded-xl border border-[#f0e3e4] bg-[#fcfbfb] p-4 transition hover:border-[#730014]/30 hover:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#730014]"
                  to={item.to}
                >
                  <div className="mt-0.5 flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]">
                    <BookOpen aria-hidden="true" className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-bold text-[#0b1c30]">{item.title}</p>
                      <CourseStatusBadge status={item.status} />
                    </div>
                    <p className="mt-1 text-sm text-[#584140]">{item.detail}</p>
                  </div>
                  <span className="shrink-0 text-xs font-semibold text-[#8b706e]">
                    {item.time}
                  </span>
                </Link>
              ))
            ) : (
              <p className="rounded-xl border border-dashed border-[#dfbfbd] p-8 text-center text-sm font-semibold text-[#584140]">
                Chưa có hoạt động khóa học nào.
              </p>
            )}
          </div>
        </Panel>

        <Panel className="p-6">
          <SectionTitle title="Trạng thái khóa học" />
          <div className="mt-5 rounded-[24px] border border-[#f0e3e4] bg-[#fffafb] p-5">
            <div className="space-y-4">
              {overviewBars.map((item) => (
                <div key={item.label} className="space-y-2">
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-semibold text-[#0b1c30]">{item.label}</span>
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

            <p className="mt-5 border-t border-[#f0e3e4] pt-4 text-sm text-[#584140]">
              Tổng cộng <strong className="text-[#0b1c30]">{stats?.totalCourses ?? 0}</strong> khóa học Online.
            </p>
          </div>
        </Panel>
      </section>
    </div>
  );
}

function QuickLink({ icon: Icon, label, to }) {
  return (
    <Link
      className="group flex min-h-16 items-center gap-3 rounded-xl border border-[#f0e3e4] bg-[#fcfbfb] px-4 py-3 text-sm font-bold text-[#0b1c30] transition hover:border-[#730014]/30 hover:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#730014]"
      to={to}
    >
      <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]">
        <Icon aria-hidden="true" className="h-4 w-4" />
      </span>
      <span className="min-w-0 flex-1">{label}</span>
      <ArrowRight aria-hidden="true" className="h-4 w-4 text-[#a38986] transition group-hover:translate-x-0.5 group-hover:text-[#730014]" />
    </Link>
  );
}

function WorkflowLink({ icon: Icon, label, to, value }) {
  return (
    <Link className="group flex min-h-16 items-center gap-3 rounded-xl border border-[#f0d8db] bg-[#fff8f8] p-3 transition hover:border-[#730014]/30 hover:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#730014]" to={to}>
      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white text-[#730014]">
        <Icon aria-hidden="true" className="h-4 w-4" />
      </span>
      <span className="min-w-0 flex-1 text-sm font-bold text-[#0b1c30]">{label}</span>
      <span className="rounded-full bg-[#4b0009] px-3 py-1 text-sm font-extrabold text-white">{value}</span>
      <ArrowRight aria-hidden="true" className="h-4 w-4 text-[#a38986] transition group-hover:translate-x-0.5" />
    </Link>
  );
}

function CourseStatusBadge({ status }) {
  const normalized = String(status || 'DRAFT').toUpperCase();
  const meta = {
    DRAFT: { label: 'Bản nháp', tone: 'warning' },
    PUBLISHED: { label: 'Đã xuất bản', tone: 'success' },
    ARCHIVED: { label: 'Lưu trữ', tone: 'neutral' },
  }[normalized] || { label: formatStatus(normalized), tone: 'neutral' };
  return <ManagerStatusBadge tone={meta.tone}>{meta.label}</ManagerStatusBadge>;
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
