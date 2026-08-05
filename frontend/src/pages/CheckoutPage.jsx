import { useEffect, useMemo, useState } from 'react';
import { CheckCircle2, XCircle } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import paymentApi from '../api/paymentApi';
import { formatCoursePrice } from '../components/course/courseFormatters';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import BrandLoadingState from '../components/ui/BrandLoadingState';
import { readCart, removeCourseFromCart } from '../utils/commerceStore';
import { buildCourseHomePath, normalizeCourse } from '../utils/courseModels';

const isTruthyReturnValue = (value) => String(value || '').toLowerCase() === 'true';
const CLASSROOM_TUITION_RETURN_KEY = 'englishlab.classroomTuitionReturn';

const readClassroomTuitionReturn = () => {
  try {
    const raw = sessionStorage.getItem(CLASSROOM_TUITION_RETURN_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

const CheckoutPage = () => {
  const location = useLocation();
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [couponCode, setCouponCode] = useState('');
  const [quote, setQuote] = useState(null);
  const [quoteLoading, setQuoteLoading] = useState(false);
  const [quoteMessage, setQuoteMessage] = useState('');
  const [autoFreeEnrollmentAttempted, setAutoFreeEnrollmentAttempted] = useState(false);
  const [classroomTuitionReturn] = useState(() => readClassroomTuitionReturn());
  const [paymentReturn, setPaymentReturn] = useState({
    checked: false,
    loading: false,
    status: '',
    paid: false,
    message: '',
    orderCode: null,
    classroomOfferingId: null,
  });

  const returnParams = useMemo(() => new URLSearchParams(location.search), [location.search]);
  const hasPaymentReturn = useMemo(
    () => ['code', 'id', 'cancel', 'status', 'orderCode'].some((key) => returnParams.has(key)),
    [returnParams],
  );

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
  const payableAmount = Number(quote?.totalAmount ?? totalAmount);
  const isFreeCourseCheckout = totalAmount <= 0;
  const isZeroAmountCheckout = payableAmount <= 0;
  const systemDiscountAmount = Number(quote?.systemDiscountAmount ?? 0);
  const couponDiscountAmount = Number(quote?.couponDiscountAmount ?? 0);
  const successCourse = checkoutCourses[0] || null;

  const selectedCourseIds = useMemo(
    () => checkoutCourses.map((course) => course.id).filter(Boolean),
    [checkoutCourses],
  );

  useEffect(() => {
    if (!hasPaymentReturn) {
      return;
    }

    const orderCode = returnParams.get('orderCode');
    const code = returnParams.get('code');
    const status = String(returnParams.get('status') || '').toUpperCase();
    const cancelled = isTruthyReturnValue(returnParams.get('cancel')) || status === 'CANCELLED';

    if (cancelled) {
      setPaymentReturn({
        checked: true,
        loading: false,
        status: 'CANCELLED',
        paid: false,
        message: classroomTuitionReturn
          ? 'Bạn đã hủy thanh toán học phí. Bạn có thể quay lại lớp để thử PayOS hoặc gửi minh chứng chuyển khoản.'
          : 'Bạn đã hủy thanh toán. Giỏ hàng vẫn được giữ để bạn có thể thử lại.',
        orderCode,
        classroomOfferingId: classroomTuitionReturn?.classroomId || null,
      });
      return;
    }

    if (!orderCode) {
      setPaymentReturn({
        checked: true,
        loading: false,
        status: 'UNKNOWN',
        paid: false,
        message: classroomTuitionReturn
          ? 'Chưa thể xác minh đơn học phí vì thiếu mã đơn hàng. Vui lòng quay lại lớp để kiểm tra lịch sử thanh toán.'
          : 'Chưa thể xác minh đơn thanh toán vì thiếu mã đơn hàng. Giỏ hàng vẫn được giữ để bạn có thể kiểm tra lại.',
        orderCode: null,
        classroomOfferingId: classroomTuitionReturn?.classroomId || null,
      });
      return;
    }

    let active = true;
    setPaymentReturn({
      checked: false,
      loading: true,
      status: 'CHECKING',
      paid: false,
      message: 'Đang xác nhận thanh toán với PayOS...',
      orderCode,
      classroomOfferingId: classroomTuitionReturn?.classroomId || null,
    });

    const checkPaymentStatus = async () => {
      try {
        const result = await paymentApi.getPaymentOrderStatus(orderCode);
        if (!active) return;
        const paid = Boolean(result?.paid) || String(result?.status || '').toUpperCase() === 'PAID';
        const isClassroomTuition = Boolean(result?.classroomOfferingId || result?.enrollmentId || classroomTuitionReturn);
        if (paid && !isClassroomTuition) {
          checkoutCourses.forEach((course) => removeCourseFromCart(course.id));
        }
        if (paid && isClassroomTuition) {
          sessionStorage.removeItem(CLASSROOM_TUITION_RETURN_KEY);
        }
        setPaymentReturn({
          checked: true,
          loading: false,
          status: String(result?.status || (paid ? 'PAID' : 'PENDING')).toUpperCase(),
          paid,
          message: result?.message || (paid
            ? (isClassroomTuition
              ? 'Thanh toán thành công. Học phí lớp đã được ghi nhận.'
              : 'Thanh toán thành công. Khóa học đã được thêm vào tài khoản của bạn.')
            : 'Đơn thanh toán đang chờ PayOS xác nhận. Vui lòng tải lại sau vài giây.'),
          orderCode,
          classroomOfferingId: result?.classroomOfferingId || classroomTuitionReturn?.classroomId || null,
        });
      } catch (error) {
        if (!active) return;
        const serverMessage =
          error?.response?.data?.message
          || error?.response?.data?.error
          || error?.response?.data?.data?.message;
        setPaymentReturn({
          checked: true,
          loading: false,
          status: 'ERROR',
          paid: code === '00' && status === 'PAID',
          message: serverMessage || 'Không thể xác nhận trạng thái thanh toán. Vui lòng thử lại.',
          orderCode,
          classroomOfferingId: classroomTuitionReturn?.classroomId || null,
        });
      }
    };

    checkPaymentStatus();

    return () => {
      active = false;
    };
  }, [checkoutCourses, classroomTuitionReturn, hasPaymentReturn, returnParams]);

  const handleConfirmPayment = async () => {
    if (!selectedCourseIds.length || submitting) return;

    setSubmitting(true);
    setSubmitError('');

    try {
      const result = await paymentApi.createPayosLink(selectedCourseIds, couponCode.trim());
      const paidDirectly = String(result?.status || '').toUpperCase() === 'PAID';

      if (paidDirectly) {
        checkoutCourses.forEach((course) => removeCourseFromCart(course.id));
        setPaymentReturn({
          checked: true,
          loading: false,
          status: 'PAID',
          paid: true,
          message: isZeroAmountCheckout
            ? 'Ghi danh thành công. Khóa học miễn phí đã sẵn sàng trong tài khoản của bạn.'
            : 'Thanh toán thành công. Khóa học đã được thêm vào tài khoản của bạn.',
          orderCode: result?.orderCode || null,
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

  useEffect(() => {
    if (
      hasPaymentReturn
      || !isFreeCourseCheckout
      || autoFreeEnrollmentAttempted
      || paymentReturn.checked
      || submitting
      || !selectedCourseIds.length
    ) {
      return;
    }

    setAutoFreeEnrollmentAttempted(true);
    handleConfirmPayment();
  }, [
    autoFreeEnrollmentAttempted,
    hasPaymentReturn,
    isFreeCourseCheckout,
    paymentReturn.checked,
    selectedCourseIds.length,
    submitting,
  ]);

  const handleApplyCoupon = async () => {
    if (!selectedCourseIds.length || quoteLoading) return;

    setQuoteLoading(true);
    setSubmitError('');
    setQuoteMessage('');

    try {
      const result = await paymentApi.quotePayment(selectedCourseIds, couponCode.trim());
      setQuote(result);
      setQuoteMessage(result?.couponMessage || (couponCode.trim() ? 'Mã giảm giá đã sẵn sàng áp dụng.' : 'Đã cập nhật tổng thanh toán.'));
    } catch (error) {
      setQuote(null);
      const serverMessage =
        error?.response?.data?.message
        || error?.response?.data?.error
        || error?.response?.data?.data?.message;
      setSubmitError(serverMessage || 'Không thể kiểm tra mã giảm giá. Vui lòng thử lại.');
    } finally {
      setQuoteLoading(false);
    }
  };

  if (paymentReturn.loading) {
    return (
      <LearnerPageShell
        title="Đang xác nhận thanh toán"
        description="EnglishLab đang kiểm tra trạng thái giao dịch với PayOS."
      >
        <BrandLoadingState className="rounded-[32px]" message={paymentReturn.message || 'Đang xác nhận thanh toán...'} />
      </LearnerPageShell>
    );
  }

  if (paymentReturn.checked) {
    const paid = paymentReturn.paid;
    const isClassroomTuition = Boolean(paymentReturn.classroomOfferingId || classroomTuitionReturn);
    const classroomPath = classroomTuitionReturn?.returnPath
      || (paymentReturn.classroomOfferingId ? `/my-classrooms/${paymentReturn.classroomOfferingId}?tab=payment` : '/my-classrooms');
    return (
      <LearnerPageShell
        title={paid ? 'Thanh toán thành công' : 'Thanh toán chưa hoàn tất'}
        description={paid
          ? (isClassroomTuition
            ? 'Học phí lớp đã được cập nhật vào hồ sơ đăng ký của bạn.'
            : 'Khóa học của bạn đã sẵn sàng trong tài khoản EnglishLab.')
          : (isClassroomTuition
            ? 'Bạn có thể quay lại lớp để thử PayOS lại hoặc gửi minh chứng chuyển khoản.'
            : 'Bạn có thể quay lại giỏ hàng để kiểm tra và thanh toán lại khi sẵn sàng.')}
      >
        <section className="grid min-h-[520px] overflow-hidden rounded-[32px] border border-[#dfbfbd]/25 bg-white shadow-sm lg:grid-cols-[0.95fr_1.05fr]">
          <div className="flex flex-col justify-center p-8 md:p-10">
            {paid ? (
              <CheckCircle2 className="h-12 w-12 text-emerald-600" />
            ) : (
              <XCircle className="h-12 w-12 text-[#93000a]" />
            )}
            <p className="mt-6 text-[12px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">
              {paid ? 'PayOS đã xác nhận' : `Trạng thái: ${paymentReturn.status || 'UNKNOWN'}`}
            </p>
            <h2 className="mt-3 font-['Manrope'] text-4xl font-extrabold text-[#2b2828]">
              {paid
                ? (isClassroomTuition ? 'Học phí lớp đã được ghi nhận.' : 'Khóa học đã được kích hoạt.')
                : 'Giao dịch chưa được ghi nhận thành công.'}
            </h2>
            <p className="mt-4 text-base leading-8 text-[#584140]">{paymentReturn.message}</p>
            {paymentReturn.orderCode ? (
              <p className="mt-4 rounded-2xl bg-[#fcf8f8] px-4 py-3 text-sm font-bold text-[#584140]">
                Mã đơn hàng: <span className="text-[#2b2828]">{paymentReturn.orderCode}</span>
              </p>
            ) : null}
            <div className="mt-8 flex flex-wrap gap-3">
              {paid && !isClassroomTuition && successCourse ? (
                <Link
                  className="rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white transition hover:-translate-y-0.5 hover:bg-[#730014]"
                  to={buildCourseHomePath(successCourse)}
                >
                  Vào khóa học
                </Link>
              ) : null}
              {isClassroomTuition ? (
                <Link
                  className="rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white transition hover:-translate-y-0.5 hover:bg-[#730014]"
                  to={classroomPath}
                >
                  Quay lại lớp học
                </Link>
              ) : (
                <Link
                  className="rounded-2xl border border-[#dfbfbd]/50 px-6 py-4 text-sm font-extrabold text-[#4b0009] transition hover:bg-[#fcf8f8]"
                  to={paid ? '/my-courses' : '/cart'}
                >
                  {paid ? 'Xem khóa học của tôi' : 'Quay lại giỏ hàng'}
                </Link>
              )}
            </div>
          </div>
          <div className="flex items-center justify-center bg-[#fff8f6] p-6">
            <img
              alt="Thanh toán thành công"
              className="max-h-[620px] w-full max-w-[560px] object-contain"
              src="/course-success-hero.png"
            />
          </div>
        </section>
      </LearnerPageShell>
    );
  }

  if (!checkoutCourses.length) {
    return (
      <LearnerPageShell
        title="Thanh toán"
        description="Bạn sẽ xem lại các khóa học đã chọn và hoàn tất thanh toán tại đây."
      >
        <section className="flex min-h-[460px] flex-col items-center justify-center rounded-[32px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center shadow-[0_18px_45px_rgba(75,0,9,0.04)]">
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
            <p className="text-[12px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">
              {isZeroAmountCheckout ? 'Phương thức ghi danh' : 'Phương thức thanh toán'}
            </p>
            <div className="mt-4 rounded-[24px] border border-[#730014]/30 bg-[#fff4f5] px-5 py-4">
              <p className="text-sm font-extrabold text-[#2b2828]">
                {isZeroAmountCheckout ? 'Ghi danh miễn phí' : 'Thanh toán qua PayOS'}
              </p>
              <p className="mt-2 text-sm leading-6 text-[#584140]">
                {isZeroAmountCheckout
                  ? 'Khóa học này không cần thanh toán. EnglishLab sẽ kích hoạt khóa học ngay trong tài khoản của bạn.'
                  : 'Bạn sẽ được chuyển tới cổng thanh toán PayOS để hoàn tất thanh toán an toàn.'}
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
            {systemDiscountAmount > 0 ? (
              <div className="flex items-center justify-between border-t border-[#ead9db] pt-4 text-sm font-bold text-emerald-700">
                <span>Ưu đãi hệ thống</span>
                <span>-{formatCoursePrice(systemDiscountAmount)}</span>
              </div>
            ) : null}
            {couponDiscountAmount > 0 ? (
              <div className="flex items-center justify-between text-sm font-bold text-emerald-700">
                <span>Mã giảm giá {quote?.couponCode ? `(${quote.couponCode})` : ''}</span>
                <span>-{formatCoursePrice(couponDiscountAmount)}</span>
              </div>
            ) : null}
            <div className="flex items-center justify-between border-t border-[#ead9db] pt-4 text-base font-extrabold text-[#2b2828]">
              <span>Tổng thanh toán</span>
              <span>{formatCoursePrice(payableAmount)}</span>
            </div>
          </div>

          <div className="mt-5 rounded-[24px] border border-[#dfbfbd]/35 bg-white p-4">
            <label className="block text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#8b706e]">Mã giảm giá</label>
            <div className="mt-3 flex gap-2">
              <input
                className="min-w-0 flex-1 rounded-2xl border border-[#dfbfbd]/65 bg-[#fcfbfb] px-4 py-3 text-sm font-semibold uppercase text-[#1a1c1c] outline-none transition focus:border-[#730014]"
                onChange={(event) => {
                  setCouponCode(event.target.value.toUpperCase());
                  setQuote(null);
                  setQuoteMessage('');
                }}
                placeholder="Nhập mã"
                value={couponCode}
              />
              <button
                className="shrink-0 rounded-2xl border border-[#dfbfbd] px-4 py-3 text-sm font-extrabold text-[#4b0009] transition hover:bg-[#fff2f3] disabled:cursor-not-allowed disabled:opacity-60"
                disabled={quoteLoading}
                onClick={handleApplyCoupon}
                type="button"
              >
                {quoteLoading ? 'Đang kiểm tra...' : 'Áp dụng'}
              </button>
            </div>
            {quoteMessage ? <p className="mt-3 text-sm font-semibold text-emerald-700">{quoteMessage}</p> : null}
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
              {submitting
                ? isZeroAmountCheckout ? 'Đang ghi danh...' : 'Đang chuyển tới PayOS...'
                : isZeroAmountCheckout ? 'Xác nhận ghi danh' : 'Xác nhận thanh toán'}
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
