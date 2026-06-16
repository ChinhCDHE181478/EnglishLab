import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import courseApi from '../api/courseApi';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import { hasAccessToken } from '../utils/auth';
import { normalizeCourse, normalizeEnrollment } from '../utils/courseModels';

const TransactionHistoryPage = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [items, setItems] = useState([]);

  useEffect(() => {
    let active = true;

    if (!hasAccessToken()) {
      setLoading(false);
      return undefined;
    }

    const loadHistory = async () => {
      setLoading(true);
      setError('');

      try {
        const enrollments = (await courseApi.getMyOnlineCourses()).map(normalizeEnrollment);
        const detailResults = await Promise.allSettled(
          enrollments.map(async (enrollment) => {
            const courseResponse = await courseApi.getOnlineCourse(enrollment.courseSlug || enrollment.courseId);
            return {
              enrollment,
              course: normalizeCourse({ ...courseResponse, registered: true }),
            };
          }),
        );

        if (!active) return;

        setItems(
          detailResults
            .filter((result) => result.status === 'fulfilled')
            .map((result) => result.value),
        );
      } catch {
        if (!active) return;
        setError('Không thể tải lịch sử giao dịch. Vui lòng thử lại.');
      } finally {
        if (active) setLoading(false);
      }
    };

    loadHistory();
    return () => {
      active = false;
    };
  }, []);

  return (
    <LearnerPageShell
      title="Lịch sử giao dịch"
      description="Theo dõi các khóa học đã được ghi nhận vào tài khoản học tập của bạn."
    >
      {!hasAccessToken() ? (
        <section className="rounded-[28px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center">
          <h2 className="font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Bạn cần đăng nhập để xem lịch sử giao dịch.</h2>
          <div className="mt-6">
            <Link className="rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white transition hover:bg-[#730014]" to="/login" state={{ from: '/transaction-history' }}>
              Đăng nhập
            </Link>
          </div>
        </section>
      ) : loading ? (
        <section className="rounded-[28px] border border-[#dfbfbd]/25 bg-white px-6 py-16 text-center text-[#584140]">
          Đang tải lịch sử giao dịch...
        </section>
      ) : error ? (
        <section className="rounded-[28px] border border-[#f0d4d7] bg-white px-6 py-16 text-center text-[#93000a]">
          {error}
        </section>
      ) : !items.length ? (
        <section className="rounded-[28px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center">
          <h2 className="font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Bạn chưa có giao dịch nào.</h2>
          <div className="mt-6">
            <Link className="rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white transition hover:bg-[#730014]" to="/courses">
              Xem khóa học
            </Link>
          </div>
        </section>
      ) : (
        <section className="grid gap-6">
          {items.map(({ enrollment, course }) => (
            <article key={course.id} className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-6 shadow-sm">
              <div className="flex flex-col gap-5 md:flex-row md:items-center">
                <img
                  alt={course.title}
                  className="h-28 w-full rounded-3xl object-cover md:w-44"
                  src={course.thumbnailUrl}
                />
                <div className="flex-1">
                  <div className="flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
                    <div>
                      <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#730014]">Đã mua</p>
                      <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{course.title}</h2>
                      <p className="mt-2 text-sm leading-7 text-[#584140]">
                        {course.targetOutcome || 'Khóa học đã được ghi nhận thành công vào tài khoản của bạn.'}
                      </p>
                    </div>
                    <span className="rounded-full bg-[#eef8f1] px-3 py-2 text-xs font-extrabold text-[#1f6b3b]">
                      Đã ghi nhận thành công
                    </span>
                  </div>

                  <div className="mt-4 grid gap-3 md:grid-cols-3">
                    <div className="rounded-2xl bg-[#fcf8f8] px-4 py-3 text-sm text-[#584140]">
                      Tiến độ học hiện tại: <strong className="text-[#2b2828]">{enrollment.progressPercent}%</strong>
                    </div>
                    <div className="rounded-2xl bg-[#fcf8f8] px-4 py-3 text-sm text-[#584140]">
                      Giá hiện tại: <strong className="text-[#2b2828]">{course.price > 0 ? `${course.price.toLocaleString('vi-VN')} đ` : 'Miễn phí'}</strong>
                    </div>
                    <div className="rounded-2xl bg-[#fcf8f8] px-4 py-3 text-sm text-[#584140]">
                      Chuỗi học: <strong className="text-[#2b2828]">{enrollment.streakDays} ngày</strong>
                    </div>
                  </div>

                  <div className="mt-4 flex flex-wrap gap-3">
                    <Link className="rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014]" to={`/courses/${course.slug}/learn`}>
                      Tiếp tục học
                    </Link>
                    <Link className="rounded-2xl border border-[#dfbfbd]/30 px-5 py-3 text-sm font-extrabold text-[#4b0009]" to={`/courses/${course.slug}`}>
                      Xem khóa học
                    </Link>
                  </div>
                </div>
              </div>
            </article>
          ))}
        </section>
      )}
    </LearnerPageShell>
  );
};

export default TransactionHistoryPage;
