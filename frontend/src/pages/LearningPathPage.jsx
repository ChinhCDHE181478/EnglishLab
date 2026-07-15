import { useCallback, useEffect, useMemo, useState } from 'react';
import { ArrowLeft, ArrowRight, CheckCircle2, Circle, LockKeyhole, Route, Target } from 'lucide-react';
import { Link } from 'react-router-dom';
import courseApi from '../api/courseApi';
import Header from '../components/ai-learning/Header';
import { CourseFooter, CourseGlobalStyles } from '../components/course';

const statusInfo = (course, path) => {
  if (course.completed) return { label: 'Đã hoàn thành', tone: 'bg-emerald-50 text-emerald-700 border-emerald-200', icon: CheckCircle2 };
  if (['ACTIVE', 'COMPLETED'].includes(course.enrollmentStatus)) return { label: 'Đang học', tone: 'bg-amber-50 text-amber-700 border-amber-200', icon: Circle };
  if (String(course.courseId) === String(path.nextCourseId)) return { label: 'Tiếp theo', tone: 'bg-[#fff0f1] text-[#8a0018] border-[#e5bcc2]', icon: ArrowRight };
  return { label: 'Chưa mở', tone: 'bg-slate-50 text-slate-500 border-slate-200', icon: LockKeyhole };
};

const actionInfo = (course) => {
  if (course.completed || ['ACTIVE', 'COMPLETED'].includes(course.enrollmentStatus)) {
    return { label: course.completed ? 'Xem lại khóa học' : 'Tiếp tục học', to: `/courses/${course.slug}/learn` };
  }
  return { label: 'Đăng ký', to: `/courses/${course.slug}` };
};

const LearningPathPage = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadPath = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setData(await courseApi.getMyLearningPath());
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải lộ trình học. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPath();
  }, [loadPath]);

  const totals = useMemo(() => {
    const paths = data?.paths || [];
    return paths.reduce((result, path) => ({
      courses: result.courses + Number(path.totalCourses || 0),
      completed: result.completed + Number(path.completedCourses || 0),
    }), { courses: 0, completed: 0 });
  }, [data]);
  const progress = totals.courses ? Math.round((totals.completed / totals.courses) * 100) : 0;

  return (
    <div className="course-page min-h-screen bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />
      <main className="mx-auto max-w-[1200px] px-4 pb-20 pt-8 md:px-10">
        <Link className="mb-6 inline-flex items-center gap-2 text-sm font-bold text-[#8a0018] hover:underline" to="/courses">
          <ArrowLeft className="h-4 w-4" />
          Quay lại danh sách khóa học
        </Link>
        <section className="overflow-hidden rounded-3xl border border-[#ead9db] bg-[linear-gradient(135deg,_#fffdfc,_#fff0f1)] p-6 shadow-sm md:p-9">
          <div className="grid gap-7 lg:grid-cols-[1fr_360px] lg:items-center">
            <div>
              <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[#8a0018]">Lộ trình cá nhân</p>
              <h1 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#4b0009] md:text-4xl">Con đường học tập của bạn</h1>
              <p className="mt-3 max-w-2xl text-sm leading-7 text-[#584140]">Theo dõi từng bước, tiếp tục khóa đang học và biết chính xác khóa học nên bắt đầu tiếp theo.</p>
              <div className="mt-5 flex flex-wrap gap-3">
                <span className="rounded-full border border-[#e6c8cc] bg-white px-4 py-2 text-sm font-bold text-[#730014]">{data?.targetExam || 'Chưa chọn kỳ thi'}</span>
                <span className="rounded-full border border-[#e6c8cc] bg-white px-4 py-2 text-sm font-bold text-[#730014]">Band hiện tại: {data?.currentBand ?? 'Chưa cập nhật'}</span>
                <span className="rounded-full border border-[#e6c8cc] bg-white px-4 py-2 text-sm font-bold text-[#730014]">Mục tiêu: {data?.targetScore || 'Chưa cập nhật'}</span>
              </div>
            </div>
            <div className="rounded-3xl border border-white/80 bg-white/80 p-5 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="text-sm font-bold text-[#584140]">Tiến độ toàn lộ trình</span>
                <strong className="text-2xl text-[#8a0018]">{progress}%</strong>
              </div>
              <div className="mt-4 h-3 overflow-hidden rounded-full bg-[#f2e4e5]"><div className="h-full rounded-full bg-[#8a0018] transition-all" style={{ width: `${progress}%` }} /></div>
              <p className="mt-3 text-xs font-semibold text-[#8c716f]">Đã hoàn thành {totals.completed}/{totals.courses} khóa học</p>
            </div>
          </div>
        </section>

        {loading ? <div className="mt-8 rounded-3xl border border-[#ead9db] bg-white p-12 text-center text-sm font-semibold text-[#584140]">Đang tải lộ trình học...</div> : null}
        {!loading && error ? <div className="mt-8 rounded-3xl border border-rose-200 bg-rose-50 p-8 text-center"><p className="text-sm font-semibold text-rose-800">{error}</p><button className="mt-4 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-bold text-white" onClick={loadPath} type="button">Thử lại</button></div> : null}
        {!loading && !error && !(data?.paths || []).length ? (
          <section className="mt-8 rounded-3xl border border-dashed border-[#dfbfbd] bg-white p-12 text-center">
            <Route className="mx-auto h-10 w-10 text-[#8a0018]" />
            <h2 className="mt-4 font-['Manrope'] text-2xl font-extrabold text-[#4b0009]">Bạn chưa có lộ trình phù hợp</h2>
            <p className="mx-auto mt-2 max-w-lg text-sm leading-7 text-[#584140]">Các khóa học có lộ trình đang được cập nhật. Bạn vẫn có thể khám phá catalog và chọn khóa phù hợp với mục tiêu hiện tại.</p>
            <Link className="mt-5 inline-flex rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-bold text-white" to="/courses">Khám phá khóa học</Link>
          </section>
        ) : null}

        <div className="mt-8 space-y-8">
          {(data?.paths || []).map((path) => {
            const pathProgress = path.totalCourses ? Math.round((path.completedCourses / path.totalCourses) * 100) : 0;
            return (
              <section key={path.code} className="overflow-hidden rounded-3xl border border-[#ead9db] bg-white shadow-sm">
                <div className="flex flex-wrap items-center justify-between gap-4 border-b border-[#f0e3e4] px-6 py-5 md:px-8">
                  <div className="flex items-center gap-3">
                    <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#fff0f1] text-[#8a0018]"><Target className="h-6 w-6" /></span>
                    <div><h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">{path.name || path.code}</h2><p className="mt-1 text-xs font-semibold text-[#8c716f]">{path.completedCourses}/{path.totalCourses} khóa · {pathProgress}% hoàn thành</p></div>
                  </div>
                  <span className="rounded-full bg-[#fff2f3] px-3 py-1.5 text-xs font-bold text-[#730014]">{path.code}</span>
                </div>
                <div className="px-5 py-6 md:px-8">
                  {(path.courses || []).map((course, index) => {
                    const state = statusInfo(course, path);
                    const Icon = state.icon;
                    const action = actionInfo(course);
                    const isCurrent = String(course.courseId) === String(path.currentStepCourseId);
                    return (
                      <div key={course.courseId} className="relative grid gap-4 pb-7 last:pb-0 md:grid-cols-[50px_120px_1fr_auto] md:items-center">
                        {index < path.courses.length - 1 ? <span className="absolute left-[24px] top-11 hidden h-[calc(100%-28px)] w-px bg-[#ead9db] md:block" /> : null}
                        <span className={`z-10 flex h-12 w-12 items-center justify-center rounded-full border-2 bg-white ${isCurrent ? 'border-[#8a0018] text-[#8a0018]' : 'border-[#dfbfbd] text-[#8c716f]'}`}><Icon className="h-5 w-5" /></span>
                        <div className="hidden h-20 overflow-hidden rounded-2xl bg-[#f4eeee] md:block">{course.thumbnailUrl ? <img alt="" className="h-full w-full object-cover" src={course.thumbnailUrl} /> : null}</div>
                        <div>
                          <div className="flex flex-wrap items-center gap-2"><span className="text-[11px] font-extrabold uppercase tracking-[0.15em] text-[#8c716f]">Bước {course.learningPathOrder || index + 1}</span><span className={`rounded-full border px-2.5 py-1 text-[10px] font-bold ${state.tone}`}>{state.label}</span></div>
                          <h3 className="mt-2 font-['Manrope'] text-lg font-extrabold text-[#1f1717]">{course.title}</h3>
                          {course.enrollmentStatus !== 'NOT_ENROLLED' ? <div className="mt-3 flex items-center gap-3"><div className="h-2 w-40 overflow-hidden rounded-full bg-[#f1e6e7]"><div className="h-full bg-[#8a0018]" style={{ width: `${course.progressPercent || 0}%` }} /></div><span className="text-xs font-bold text-[#730014]">{course.progressPercent || 0}%</span></div> : null}
                        </div>
                        <Link className={`inline-flex items-center justify-center gap-2 rounded-2xl px-4 py-2.5 text-sm font-extrabold ${isCurrent || !course.lockedReason ? 'bg-[#4b0009] text-white hover:bg-[#730014]' : 'border border-[#dfbfbd] bg-white text-[#730014]'}`} to={action.to}>{action.label}<ArrowRight className="h-4 w-4" /></Link>
                      </div>
                    );
                  })}
                </div>
              </section>
            );
          })}
        </div>
      </main>
      <CourseFooter />
    </div>
  );
};

export default LearningPathPage;
