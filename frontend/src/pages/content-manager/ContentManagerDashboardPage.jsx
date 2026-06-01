import { useEffect, useState } from 'react';
import { Brain, FileQuestion, Plus, Upload, Wand2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { Panel, SectionTitle } from '../../components/content-manager/ContentManagerUi';

export default function ContentManagerDashboardPage() {
  const [stats, setStats] = useState(null);
  const [courses, setCourses] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    Promise.all([
      courseApi.getManagedCourseStats(),
      courseApi.getManagedOnlineCourses({ page: 0, size: 8 }),
    ])
      .then(([statsData, coursePage]) => {
        if (!active) return;
        setStats(statsData);
        setCourses(coursePage.content || []);
      })
      .catch(() => {
        if (active) setError('Chưa tải được dữ liệu quản trị từ backend.');
      });

    return () => {
      active = false;
    };
  }, []);

  const statCards = [
    { label: 'Total Online Courses', value: stats?.totalCourses ?? '-', meta: 'Manager inventory' },
    { label: 'Published Courses', value: stats?.publishedCourses ?? '-', meta: 'Visible to learners' },
    { label: 'Draft Courses', value: stats?.draftCourses ?? '-', meta: 'Needs review' },
    { label: 'Archived Courses', value: stats?.archivedCourses ?? '-', meta: 'Soft-retired' },
    { label: 'Total Lessons', value: stats?.totalLessons ?? '-', meta: 'Across active courses' },
    { label: 'Total Enrollments', value: stats?.totalEnrollments ?? '-', meta: 'Course registrations' },
  ];

  const recentActivity = [...courses]
    .sort((a, b) => new Date(b.updatedAt || b.createdAt || 0) - new Date(a.updatedAt || a.createdAt || 0))
    .slice(0, 4)
    .map((course) => ({
      title: course.title,
      detail: `${course.status || 'DRAFT'} · ${course.totalLessons || 0} lessons · ${formatPrice(course.price)}`,
      time: formatDate(course.updatedAt || course.createdAt),
      to: `/content-manager/courses/${course.slug}/edit`,
    }));

  const healthIssues = [
    { title: `${stats?.draftCourses ?? 0} draft courses need review`, detail: 'Draft count is read directly from backend course status.' },
    { title: `${stats?.archivedCourses ?? 0} archived courses`, detail: 'Archived packages remain hidden from learners but available for manager audit.' },
    { title: `${stats?.totalLessons ?? 0} lessons in active inventory`, detail: 'Lesson count is calculated from saved course modules, not mock data.' },
  ];

  const chartValues = [stats?.draftCourses, stats?.publishedCourses, stats?.archivedCourses, stats?.totalCourses, stats?.totalLessons, stats?.totalEnrollments];

  return (
    <div className="space-y-8">
      {error ? <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">{error}</div> : null}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {statCards.map((card) => (
          <Panel key={card.label} className="p-5">
            <p className="text-sm font-medium text-[#584140]">{card.label}</p>
            <div className="mt-3 flex items-end justify-between gap-3">
              <p className="font-['Manrope'] text-3xl font-extrabold text-[#4b0009]">{card.value}</p>
              <span className="rounded-full bg-[#fff2f3] px-3 py-1 text-xs font-semibold text-[#730014]">{card.meta}</span>
            </div>
          </Panel>
        ))}
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.45fr_0.9fr]">
        <Panel className="p-6">
          <SectionTitle title="Quick Actions" />
          <div className="mt-5 grid gap-3 md:grid-cols-2">
            <QuickLink icon={Plus} label="Create Online Course" to="/content-manager/courses/new" />
            <QuickLink icon={Upload} label="Upload Learning Material" to="/content-manager/materials" />
            <QuickLink icon={Brain} label="Create Flashcard Set" to="/content-manager/flashcards" />
            <QuickLink icon={FileQuestion} label="Create Mock Exam Question" to="/content-manager/mock-exams" />
          </div>
        </Panel>

        <Panel className="p-6">
          <SectionTitle title="Content Health" />
          <div className="mt-5 space-y-4">
            {healthIssues.map((issue) => (
              <div key={issue.title} className="rounded-2xl border border-[#f0d8db] bg-[#fff8f8] p-4">
                <p className="font-semibold text-[#4b0009]">{issue.title}</p>
                <p className="mt-1 text-sm text-[#584140]">{issue.detail}</p>
              </div>
            ))}
          </div>
        </Panel>
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.4fr_1fr]">
        <Panel className="p-6">
          <SectionTitle title="Recent Course Activity" />
          <div className="mt-5 space-y-3">
            {recentActivity.length ? recentActivity.map((item) => (
              <Link key={item.title} className="flex items-start gap-4 rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] p-4 transition hover:border-[#730014]/30 hover:bg-white" to={item.to}>
                <div className="mt-0.5 flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]">
                  <Wand2 className="h-5 w-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-[#1a1c1c]">{item.title}</p>
                  <p className="mt-1 text-sm text-[#584140]">{item.detail}</p>
                </div>
                <span className="text-xs font-semibold uppercase tracking-[0.18em] text-[#9c7f7c]">{item.time}</span>
              </Link>
            )) : <p className="rounded-2xl border border-dashed border-[#dfbfbd] p-5 text-sm text-[#584140]">No course activity yet.</p>}
          </div>
        </Panel>

        <Panel className="p-6">
          <SectionTitle title="Inventory Snapshot" />
          <div className="mt-8 flex h-48 items-end gap-3">
            {chartValues.map((value, index) => (
              <div key={`${index}-${value}`} className="flex-1">
                <div className={`w-full rounded-t-2xl ${index === 1 || index === 4 ? 'bg-[#730014]' : 'bg-[#ead6d8]'}`} style={{ height: `${Math.max(12, Math.min(100, Number(value || 0) * 8))}%` }} />
              </div>
            ))}
          </div>
          <div className="mt-4 flex justify-between text-xs font-semibold uppercase tracking-[0.14em] text-[#a88f8c]">
            {['Draft', 'Pub', 'Arch', 'Total', 'Lessons', 'Enroll'].map((label) => <span key={label}>{label}</span>)}
          </div>
        </Panel>
      </section>
    </div>
  );
}

function QuickLink({ icon: Icon, label, to }) {
  return (
    <Link className="flex items-center gap-3 rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] px-4 py-4 text-sm font-semibold text-[#1a1c1c] transition hover:border-[#730014]/30 hover:bg-white" to={to}>
      <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]">
        <Icon className="h-4 w-4" />
      </span>
      {label}
    </Link>
  );
}

function formatPrice(value) {
  const amount = Number(value || 0);
  if (!amount) return 'Free';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(amount);
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
}
