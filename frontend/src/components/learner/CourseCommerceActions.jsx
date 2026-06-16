import { Heart, ShoppingCart } from 'lucide-react';
import { useEffect, useState } from 'react';
import {
  addCourseToCart,
  addCourseToWishlist,
  commerceEventName,
  isCourseInCart,
  isCourseInWishlist,
  removeCourseFromWishlist,
} from '../../utils/commerceStore';
import { useLearnerExperience } from '../../context/LearnerExperienceContext';

const iconButtonClassName =
  'inline-flex h-11 w-11 items-center justify-center rounded-full border border-[#dfbfbd]/45 bg-white text-[#5a0b12] shadow-sm transition hover:-translate-y-0.5 hover:border-[#730014]/35 hover:bg-[#fff4f5] disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0';

const CourseCommerceActions = ({ course, compact = false, className = '' }) => {
  const { addNotification } = useLearnerExperience();
  const [cartAdded, setCartAdded] = useState(() => isCourseInCart(course?.id));
  const [wishlistAdded, setWishlistAdded] = useState(() => isCourseInWishlist(course?.id));

  useEffect(() => {
    const sync = () => {
      setCartAdded(isCourseInCart(course?.id));
      setWishlistAdded(isCourseInWishlist(course?.id));
    };

    sync();
    window.addEventListener('storage', sync);
    window.addEventListener(commerceEventName, sync);

    return () => {
      window.removeEventListener('storage', sync);
      window.removeEventListener(commerceEventName, sync);
    };
  }, [course?.id]);

  const isRegistered = Boolean(course?.registered);
  const buttonSizeClassName = compact ? 'h-10 w-10' : '';
  const iconSizeClassName = compact ? 'h-4 w-4' : 'h-[18px] w-[18px]';

  const handleAddCart = () => {
    const result = addCourseToCart(course);
    if (result.ok) {
      setCartAdded(true);
      addNotification({
        title: 'Đã thêm vào giỏ hàng',
        message: `Khóa học ${course.title} đã được thêm vào giỏ hàng của bạn.`,
      });
      return;
    }

    if (result.reason === 'registered') {
      addNotification({
        title: 'Khóa học đã được ghi nhận',
        message: 'Bạn đã có khóa học này trong tài khoản học tập.',
        type: 'error',
      });
      return;
    }

    addNotification({
      title: 'Khóa học đã có trong giỏ hàng',
      message: 'Bạn có thể mở giỏ hàng để tiếp tục xem và thanh toán.',
      type: 'error',
    });
  };

  const handleAddWishlist = () => {
    if (wishlistAdded) {
      removeCourseFromWishlist(course?.id);
      setWishlistAdded(false);
      addNotification({
        title: 'Đã bỏ khỏi danh sách yêu thích',
        message: `Khóa học ${course.title} đã được gỡ khỏi danh sách yêu thích của bạn.`,
      });
      return;
    }

    const result = addCourseToWishlist(course);
    if (result.ok) {
      setWishlistAdded(true);
      addNotification({
        title: 'Đã lưu vào danh sách yêu thích',
        message: `Khóa học ${course.title} đã được lưu để bạn xem lại sau.`,
      });
      return;
    }

    addNotification({
      title: 'Khóa học đã có trong danh sách yêu thích',
      message: 'Danh sách yêu thích của bạn đã được cập nhật.',
      type: 'error',
    });
  };

  return (
    <div className={`flex flex-wrap items-center gap-2 ${className}`}>
      <button
        aria-label={isRegistered ? 'Khóa học đã được ghi nhận' : cartAdded ? 'Khóa học đã có trong giỏ hàng' : 'Thêm vào giỏ hàng'}
        className={`${iconButtonClassName} ${buttonSizeClassName} ${cartAdded ? 'border-[#730014]/20 bg-[#fff3f4] text-[#730014]' : ''}`}
        disabled={isRegistered || cartAdded}
        onClick={handleAddCart}
        type="button"
      >
        <ShoppingCart className={iconSizeClassName} />
      </button>
      <button
        aria-label={wishlistAdded ? 'Đã lưu vào danh sách yêu thích' : 'Thêm vào danh sách yêu thích'}
        className={`${iconButtonClassName} ${buttonSizeClassName} ${
          wishlistAdded
            ? 'border-[#d94b63]/35 bg-[#fff1f3] text-[#c5162e]'
            : 'hover:text-[#c5162e]'
        }`}
        onClick={handleAddWishlist}
        type="button"
      >
        <Heart className={`${iconSizeClassName} ${wishlistAdded ? 'fill-current' : ''}`} />
      </button>
    </div>
  );
};

export default CourseCommerceActions;
