import { Trash2 } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import { useLearnerExperience } from '../context/LearnerExperienceContext';
import {
  addCourseToWishlist,
  commerceEventName,
  readCart,
  removeCourseFromCart,
} from '../utils/commerceStore';
import { formatCoursePrice } from '../components/course/courseFormatters';

const CartPage = () => {
  const { addNotification } = useLearnerExperience();
  const [cartItems, setCartItems] = useState(() => readCart());

  useEffect(() => {
    const sync = () => setCartItems(readCart());
    window.addEventListener('storage', sync);
    window.addEventListener(commerceEventName, sync);
    return () => {
      window.removeEventListener('storage', sync);
      window.removeEventListener(commerceEventName, sync);
    };
  }, []);

  const totalPrice = useMemo(
    () => cartItems.reduce((sum, item) => sum + Number(item.salePrice || item.price || 0), 0),
    [cartItems],
  );

  const handleRemove = (courseId) => {
    removeCourseFromCart(courseId);
    addNotification({
      title: 'Đã xóa khỏi giỏ hàng',
      message: 'Khóa học đã được xóa khỏi giỏ hàng của bạn.',
    });
  };

  const handleSaveForLater = (course) => {
    const result = addCourseToWishlist(course);
    removeCourseFromCart(course.id);
    addNotification({
      title: result.ok ? 'Đã chuyển sang danh sách yêu thích' : 'Đã xóa khỏi giỏ hàng',
      message: result.ok
        ? 'Khóa học đã được lưu để bạn xem lại sau.'
        : 'Khóa học đã được xóa khỏi giỏ hàng vì bạn đã lưu trước đó.',
    });
  };

  return (
    <LearnerPageShell
      actions={(
        <Link
          className="rounded-2xl border border-[#dfbfbd]/30 bg-white px-5 py-3 text-sm font-extrabold text-[#4b0009] transition hover:-translate-y-0.5 hover:bg-[#fff4f5]"
          to="/wishlist"
        >
          Mở danh sách yêu thích
        </Link>
      )}
      title="Giỏ hàng"
      description="Xem lại các khóa học bạn đã chọn trước khi chuyển sang bước thanh toán."
    >
      {!cartItems.length ? (
        <section className="flex min-h-[480px] flex-1 flex-col justify-center rounded-[32px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center shadow-[0_18px_45px_rgba(75,0,9,0.04)] md:min-h-[540px]">
          <h2 className="font-['Manrope'] text-4xl font-extrabold text-[#2b2828]">Bạn chưa thêm khóa học nào vào giỏ hàng.</h2>
          <p className="mx-auto mt-4 max-w-2xl text-sm leading-8 text-[#584140]">
            Hãy khám phá danh sách khóa học phù hợp để chọn chương trình học bạn muốn bắt đầu trong thời gian tới.
          </p>
          <div className="mt-6 flex flex-wrap justify-center gap-3">
            <Link
              className="rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white transition hover:-translate-y-0.5 hover:bg-[#730014]"
              to="/courses"
            >
              Xem khóa học
            </Link>
            <Link
              className="rounded-2xl border border-[#dfbfbd]/30 bg-white px-6 py-4 text-sm font-extrabold text-[#4b0009] transition hover:-translate-y-0.5 hover:bg-[#fff4f5]"
              to="/wishlist"
            >
              Xem danh sách yêu thích
            </Link>
          </div>
        </section>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[1.45fr_0.55fr]">
          <section className="space-y-5">
            {cartItems.map((course) => (
              <article key={course.id} className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-5 shadow-sm">
                <div className="flex flex-col gap-5 md:flex-row">
                  <img alt={course.title} className="h-32 w-full rounded-3xl object-cover md:w-48" src={course.thumbnailUrl} />
                  <div className="flex-1">
                    <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                      <div>
                        <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#730014]">{course.categoryName || 'Khóa học trực tuyến'}</p>
                        <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{course.title}</h2>
                        <p className="mt-2 text-sm leading-7 text-[#584140]">{course.shortDescription || 'Khóa học đang được cập nhật mô tả.'}</p>
                      </div>
                      <div className="text-right">
                        {Number(course.originalPrice || 0) > Number(course.salePrice || course.price || 0) ? (
                          <p className="text-sm font-bold text-[#8c716f] line-through">{formatCoursePrice(course.originalPrice)}</p>
                        ) : null}
                        <p className="mt-1 text-2xl font-extrabold text-[#4b0009]">{formatCoursePrice(course.salePrice || course.price)}</p>
                      </div>
                    </div>

                    <div className="mt-4 flex flex-wrap gap-3">
                      <Link className="rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014]" to={`/courses/${course.slug}`}>
                        Xem khóa học
                      </Link>
                      <button className="rounded-2xl border border-[#dfbfbd]/30 px-5 py-3 text-sm font-extrabold text-[#4b0009] transition hover:bg-[#fff4f5]" onClick={() => handleSaveForLater(course)} type="button">
                        Chuyển sang danh sách yêu thích
                      </button>
                      <button className="inline-flex items-center gap-2 rounded-2xl border border-[#f0d4d7] px-5 py-3 text-sm font-extrabold text-[#93000a] transition hover:bg-[#fff1f1]" onClick={() => handleRemove(course.id)} type="button">
                        <Trash2 className="h-4 w-4" />
                        Xóa khỏi giỏ hàng
                      </button>
                    </div>
                  </div>
                </div>
              </article>
            ))}
          </section>

          <aside className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-6 shadow-sm">
            <h2 className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Tóm tắt đơn hàng</h2>
            <div className="mt-5 space-y-4 text-sm text-[#584140]">
              <div className="flex items-center justify-between">
                <span>Số khóa học</span>
                <strong className="text-[#2b2828]">{cartItems.length}</strong>
              </div>
              <div className="flex items-center justify-between">
                <span>Tạm tính</span>
                <strong className="text-[#2b2828]">{formatCoursePrice(totalPrice)}</strong>
              </div>
              <div className="flex items-center justify-between border-t border-[#f1e4e5] pt-4">
                <span className="font-bold text-[#2b2828]">Tổng thanh toán</span>
                <strong className="text-lg text-[#4b0009]">{formatCoursePrice(totalPrice)}</strong>
              </div>
            </div>
            <Link className="mt-6 inline-flex w-full items-center justify-center rounded-2xl bg-[#4b0009] px-5 py-4 text-sm font-extrabold text-white transition hover:bg-[#730014]" to="/checkout">
              Tiếp tục thanh toán
            </Link>
          </aside>
        </div>
      )}
    </LearnerPageShell>
  );
};

export default CartPage;
