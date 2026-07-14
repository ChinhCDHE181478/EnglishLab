import { Heart, ShoppingCart, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import BrandLoadingState from '../components/ui/BrandLoadingState';
import { useLearnerExperience } from '../context/LearnerExperienceContext';
import {
  addCourseToCart,
  commerceEventName,
  fetchWishlist,
  readWishlist,
  removeCourseFromWishlist,
} from '../utils/commerceStore';
import { formatCoursePrice } from '../components/course/courseFormatters';

const WishlistPage = () => {
  const { addNotification } = useLearnerExperience();
  const [wishlistItems, setWishlistItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    const loadWishlist = async () => {
      setLoading(true);
      try {
        const nextItems = await fetchWishlist();
        if (active) setWishlistItems(nextItems);
      } finally {
        if (active) setLoading(false);
      }
    };
    const syncFromStore = () => {
      setWishlistItems(readWishlist());
    };
    loadWishlist();
    window.addEventListener('storage', syncFromStore);
    window.addEventListener(commerceEventName, syncFromStore);
    return () => {
      active = false;
      window.removeEventListener('storage', syncFromStore);
      window.removeEventListener(commerceEventName, syncFromStore);
    };
  }, []);

  const handleAddToCart = async (course) => {
    const result = await addCourseToCart(course);
    if (result.ok) {
      addNotification({
        title: 'Đã thêm vào giỏ hàng',
        message: `Khóa học ${course.title} đã được thêm vào giỏ hàng của bạn.`,
      });
      return;
    }

    addNotification({
      title: result.reason === 'registered' ? 'Khóa học đã được ghi nhận' : 'Khóa học đã có trong giỏ hàng',
      message: result.reason === 'registered'
        ? 'Bạn đã có khóa học này trong tài khoản học tập.'
        : 'Bạn có thể mở giỏ hàng để tiếp tục xem và thanh toán.',
      type: 'error',
    });
  };

  const handleRemove = async (courseId) => {
    await removeCourseFromWishlist(courseId);
    addNotification({
      title: 'Đã xóa khỏi danh sách yêu thích',
      message: 'Khóa học đã được gỡ khỏi danh sách yêu thích của bạn.',
    });
  };

  return (
    <LearnerPageShell
      actions={(
        <Link
          className="rounded-2xl border border-[#dfbfbd]/30 bg-white px-5 py-3 text-sm font-extrabold text-[#4b0009] transition hover:-translate-y-0.5 hover:bg-[#fff4f5]"
          to="/cart"
        >
          Mở giỏ hàng
        </Link>
      )}
      title="Danh sách yêu thích"
      description="Lưu lại những khóa học bạn quan tâm để xem tiếp vào thời điểm phù hợp."
    >
      {loading ? (
        <BrandLoadingState className="rounded-[32px]" message="Đang tải danh sách yêu thích..." />
      ) : !wishlistItems.length ? (
        <section className="flex min-h-[480px] flex-1 flex-col justify-center rounded-[32px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center shadow-[0_18px_45px_rgba(75,0,9,0.04)] md:min-h-[540px]">
          <h2 className="font-['Manrope'] text-4xl font-extrabold text-[#2b2828]">Bạn chưa lưu khóa học nào vào danh sách yêu thích.</h2>
          <p className="mx-auto mt-4 max-w-2xl text-sm leading-8 text-[#584140]">
            Hãy chọn những khóa học khiến bạn quan tâm để dễ quay lại so sánh, cân nhắc và bắt đầu học sau.
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
              to="/cart"
            >
              Xem giỏ hàng
            </Link>
          </div>
        </section>
      ) : (
        <section className="grid gap-5">
          {wishlistItems.map((course) => (
            <article key={course.id} className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-5 shadow-sm">
              <div className="flex flex-col gap-5 md:flex-row">
                <img alt={course.title} className="h-32 w-full rounded-3xl object-cover md:w-48" src={course.thumbnailUrl} />
                <div className="flex-1">
                  <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                    <div>
                      <p className="inline-flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-[#730014]">
                        <Heart className="h-4 w-4 fill-current" />
                        Đã lưu yêu thích
                      </p>
                      <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{course.title}</h2>
                      <p className="mt-2 text-sm leading-7 text-[#584140]">{course.shortDescription || 'Khóa học đang được cập nhật mô tả.'}</p>
                    </div>
                    <p className="text-2xl font-extrabold text-[#4b0009]">{formatCoursePrice(course.salePrice || course.price)}</p>
                  </div>

                  <div className="mt-4 flex flex-wrap gap-3">
                    <Link className="rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014]" to={`/courses/${course.slug}`}>
                      Xem khóa học
                    </Link>
                    <button className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/30 px-5 py-3 text-sm font-extrabold text-[#4b0009] transition hover:bg-[#fff4f5]" onClick={() => handleAddToCart(course)} type="button">
                      <ShoppingCart className="h-4 w-4" />
                      Thêm vào giỏ hàng
                    </button>
                    <button className="inline-flex items-center gap-2 rounded-2xl border border-[#f0d4d7] px-5 py-3 text-sm font-extrabold text-[#93000a] transition hover:bg-[#fff1f1]" onClick={() => handleRemove(course.id)} type="button">
                      <Trash2 className="h-4 w-4" />
                      Xóa khỏi danh sách yêu thích
                    </button>
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

export default WishlistPage;
