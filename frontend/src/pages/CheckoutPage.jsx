import { useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import paymentApi from '../api/paymentApi';
import { formatCoursePrice } from '../components/course/courseFormatters';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import { readCart, removeCourseFromCart } from '../utils/commerceStore';
import { buildCourseHomePath, normalizeCourse } from '../utils/courseModels';

const CheckoutPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');

  const checkoutCourses = useMemo(() => {
    const rawCourse = location.state?.course;
    if (rawCourse) {
      return [normalizeCourse(rawCourse)];
    }
    return readCart().map(normalizeCourse);
  }, [location.state]);

  const totalAmount = useMemo(
    () => checkoutCourses.reduce((sum, course) => sum + Number(course.salePrice || course.price || 0), 0),
    [checkoutCourses],
  );

  const selectedCourseIds = useMemo(
    () => checkoutCourses.map((course) => course.id).filter(Boolean),
    [checkoutCourses],
  );

  const handleConfirmPayment = async () => {
    if (!selectedCourseIds.length || submitting) return;

    setSubmitting(true);
    setSubmitError('');

    try {
      const result = await paymentApi.createPayosLink(selectedCourseIds);
      const paidDirectly = String(result?.status || '').toUpperCase() === 'PAID';

      if (paidDirectly) {
        checkoutCourses.forEach((course) => removeCourseFromCart(course.id));
        const firstCourse = checkoutCourses[0];
        navigate(buildCourseHomePath(firstCourse), {
          replace: true,
          state: {
            course: firstCourse,
            paymentSuccessMessage: 'Thanh toán thành công. Khóa học đã được thêm vào tài khoản của bạn.',
          },
        });
        return;
      }

      const checkoutUrl =
        result?.checkoutUrl
        || result?.paymentUrl
        || result?.url
        || result?.payUrl
        || result?.data?.checkoutUrl
        || result?.data?.paymentUrl;

      if (!checkoutUrl) {
        throw new Error('missing_checkout_url');
      }

      window.location.href = checkoutUrl;
    } catch (error) {
      const serverMessage =
        error?.response?.data?.message
        || error?.response?.data?.error
        || error?.response?.data?.data?.message;
      setSubmitError(serverMessage || 'Chưa thể chuyển sang bước thanh toán. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  if (!checkoutCourses.length) {
    return (
      <LearnerPageShell
        title="Thanh toán"
        description="Bạn sẽ xem lại các khóa học đã chọn và hoàn tất thanh toán tại đây."
      >
        <section className="rounded-[32px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center shadow-[0_18px_45px_rgba(75,0,9,0.04)]">
          <h2 className="font-['Manrope'] text-4xl font-extrabold text-[#2b2828]">Bạn chưa chọn khóa học để thanh toán.</h2>
          <p className="mx-auto mt-4 max-w-2xl text-sm leading-8 text-[#584140]">
            Hãy quay lại danh sách khóa học và thêm chương trình học phù hợp trước khi tiếp tục.
          </p>
          <div className="mt-6 flex justify-center">
            <Link
              className="rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white transition hover:-translate-y-0.5 hover:bg-[#730014]"
              to="/courses"
            >
              Xem khóa học
            </Link>
          </div>
        </section>
      </LearnerPageShell>
    );
  }

  return (
    <LearnerPageShell
      title="Thanh toán"
      description="Bạn sẽ xem lại các khóa học đã chọn và hoàn tất thanh toán tại đây."
    >
      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <section className="rounded-[32px] border border-[#dfbfbd]/25 bg-white p-6 shadow-sm">
          <div className="mb-6">
            <p className="text-[12px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Khóa học đã chọn</p>
            <h2 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Xác nhận thông tin khóa học</h2>
          </div>

          <div className="space-y-4">
            {checkoutCourses.map((course) => (
              <article key={course.id} className="flex flex-col gap-5 rounded-[28px] border border-[#dfbfbd]/20 bg-[#fcf8f8] p-5 md:flex-row">
                <img
                  alt={course.title}
                  className="h-40 w-full rounded-[24px] object-cover md:w-56"
                  src={course.thumbnailUrl}
                />
                <div className="flex-1">
                  <p className="text-[11px] font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">
                    {course.categoryName || course.category || 'Khóa học trực tuyến'}
                  </p>
                  <h3 className="mt-2 text-2xl font-extrabold text-[#2b2828]">{course.title}</h3>
                  <p className="mt-3 text-sm leading-7 text-[#584140]">
                    {course.shortDescription || course.description || 'Khóa học đang được cập nhật mô tả.'}
                  </p>

                  <div className="mt-4 grid gap-3 md:grid-cols-2">
                    <div className="rounded-2xl bg-white px-4 py-3 text-sm text-[#584140]">
                      Mục tiêu đầu ra:
                      <strong className="ml-1 text-[#2b2828]">{course.targetOutcome || 'Đang cập nhật'}</strong>
                    </div>
                    <div className="rounded-2xl bg-white px-4 py-3 text-sm text-[#584140]">
                      Số bài học:
                      <strong className="ml-1 text-[#2b2828]">{course.totalLessons || 0} bài</strong>
                    </div>
                  </div>
                </div>
              </article>
            ))}
          </div>

          <div className="mt-6">
            <p className="text-[12px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Phương thức thanh toán</p>
            <div className="mt-4 rounded-[24px] border border-[#730014]/30 bg-[#fff4f5] px-5 py-4">
              <p className="text-sm font-extrabold text-[#2b2828]">Thanh toán qua PayOS</p>
              <p className="mt-2 text-sm leading-6 text-[#584140]">
                Bạn sẽ được chuyển tới cổng thanh toán PayOS để hoàn tất thanh toán an toàn.
              </p>
            </div>
          </div>
        </section>

        <aside className="rounded-[32px] border border-[#dfbfbd]/25 bg-white p-6 shadow-sm">
          <p className="text-[12px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Thông tin thanh toán</p>
          <h2 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Tóm tắt đơn hàng</h2>

          <div className="mt-6 space-y-4 rounded-[28px] bg-[#fcf8f8] p-5">
            {checkoutCourses.map((course) => (
              <div key={course.id} className="flex items-start justify-between gap-4 text-sm text-[#584140]">
                <span>{course.title}</span>
                <strong className="text-right text-[#2b2828]">{formatCoursePrice(course.salePrice || course.price)}</strong>
              </div>
            ))}
            <div className="flex items-center justify-between border-t border-[#ead9db] pt-4 text-base font-extrabold text-[#2b2828]">
              <span>Tổng thanh toán</span>
              <span>{formatCoursePrice(totalAmount)}</span>
            </div>
          </div>

          {submitError ? (
            <div className="mt-4 rounded-2xl border border-[#f0d4d7] bg-[#fff6f7] px-4 py-3 text-sm font-semibold text-[#93000a]">
              {submitError}
            </div>
          ) : null}

          <div className="mt-6 flex flex-col gap-3">
            <button
              className="rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white transition hover:-translate-y-0.5 hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0"
              disabled={submitting}
              onClick={handleConfirmPayment}
              type="button"
            >
              {submitting ? 'Đang chuyển tới PayOS...' : 'Xác nhận thanh toán'}
            </button>
            <Link
              className="rounded-2xl border border-[#dfbfbd]/30 px-6 py-4 text-center text-sm font-extrabold text-[#4b0009] transition hover:bg-[#fcf8f8]"
              to="/cart"
            >
              Quay lại giỏ hàng
            </Link>
          </div>
        </aside>
      </div>
    </LearnerPageShell>
  );
};

export default CheckoutPage;
