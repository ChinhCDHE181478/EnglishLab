import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight, BadgePercent, CheckCircle2, Route, ShoppingCart } from 'lucide-react';
import courseApi from '../api/courseApi';
import Header from '../components/ai-learning/Header';
import CourseFooter from '../components/course/CourseFooter';
import CourseGlobalStyles from '../components/course/CourseGlobalStyles';
import { formatCoursePrice } from '../components/course/courseFormatters';
import { hasAccessToken } from '../utils/auth';
import { saveLearningPathCheckout } from '../utils/learningPathCheckout';

export default function LearningPathReferencePage() {
  const { code } = useParams();
  const navigate = useNavigate();
  const [path, setPath] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    window.scrollTo(0, 0);
    const load = async () => {
      try {
        setPath(await courseApi.getLearningPathOffer(code));
      } catch (err) {
        setError(err?.response?.data?.message || 'Không thể tải lộ trình này.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [code]);

  const handlePurchase = () => {
    if (!hasAccessToken()) {
      navigate('/login', { state: { from: `/learning-paths/${encodeURIComponent(code)}` } });
      return;
    }
    saveLearningPathCheckout(path);
    navigate(`/checkout?learningPathId=${path.id}`);
  };

  return (
    <div className="flex min-h-screen flex-col bg-[#f9f9f9] text-[#2b2828]">
      <CourseGlobalStyles />
      <Header />
      <main className="mx-auto w-full max-w-[1120px] flex-1 px-4 py-10 md:px-8">
        <Link className="inline-flex items-center gap-2 text-sm font-bold text-[#730014] hover:underline" to="/courses">
          <ArrowLeft className="h-4 w-4" />
          Quay lại khóa học
        </Link>

        {loading ? (
          <p className="py-16 text-center text-[#584140] font-semibold">Đang tải lộ trình...</p>
        ) : error || !path ? (
          <p className="py-16 text-center text-[#584140] font-semibold">{error || 'Không tìm thấy lộ trình này.'}</p>
        ) : (
          <>
            {/* Header Banner */}
            <section className="mt-6 overflow-hidden rounded-3xl border border-[#ead9db] bg-[linear-gradient(135deg,_#fffdfc,_#fff0f1)] p-6 shadow-sm md:p-9">
              <div className="flex items-center gap-4">
                <span className="flex h-14 w-14 items-center justify-center rounded-3xl bg-[#fff0f2] text-[#8a0018] shadow-sm">
                  <Route className="h-7 w-7" />
                </span>
                <div>
                  <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[#8a0018]">
                    Lộ trình trọn gói
                  </p>
                  <h1 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#4b0009]">
                    {path.name}
                  </h1>
                </div>
              </div>
              <p className="mt-4 text-sm leading-relaxed text-[#584140]">
                Học tuần tự theo chương trình và thanh toán một lần cho những khóa bạn chưa sở hữu.
              </p>
            </section>

            <section className="mt-6 grid gap-5 rounded-3xl border border-[#ead9db] bg-white p-6 shadow-sm md:grid-cols-[1fr_auto] md:items-center md:p-8">
              <div>
                <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#8a0018]">Gói lộ trình</p>
                <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
                  {path.purchaseAvailable ? `${path.remainingCourses} khóa học chưa sở hữu` : 'Bạn đã sở hữu toàn bộ lộ trình'}
                </h2>
                {path.discountApplied ? (
                  <p className="mt-3 inline-flex items-center gap-2 text-sm font-bold text-emerald-700">
                    <BadgePercent className="h-4 w-4" /> Ưu đãi lộ trình {path.discountPercent}%
                  </p>
                ) : null}
              </div>
              <div className="min-w-64 rounded-2xl bg-[#fff7f7] p-5">
                {path.purchaseAvailable ? (
                  <>
                    <div className="flex items-center justify-between gap-6 text-sm text-[#584140]">
                      <span>Tạm tính</span>
                      <span className={path.discountApplied ? 'line-through' : 'font-bold text-[#2b2828]'}>{formatCoursePrice(path.subtotalAmount)}</span>
                    </div>
                    {path.discountApplied ? (
                      <div className="mt-2 flex items-center justify-between gap-6 text-sm font-bold text-emerald-700">
                        <span>Giảm theo lộ trình</span>
                        <span>-{formatCoursePrice(path.learningPathDiscountAmount)}</span>
                      </div>
                    ) : null}
                    <div className="mt-4 flex items-center justify-between gap-6 border-t border-[#ead9db] pt-4">
                      <span className="text-sm font-bold text-[#2b2828]">Thanh toán</span>
                      <strong className="text-2xl text-[#4b0009]">{formatCoursePrice(path.totalAmount)}</strong>
                    </div>
                    <button className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-[#4b0009] px-5 py-4 text-sm font-extrabold text-white transition hover:bg-[#730014]" onClick={handlePurchase} type="button">
                      <ShoppingCart className="h-4 w-4" /> Mua lộ trình
                    </button>
                  </>
                ) : (
                  <div className="flex items-center gap-2 font-bold text-emerald-700">
                    <CheckCircle2 className="h-5 w-5" /> Đã sở hữu
                  </div>
                )}
              </div>
            </section>

            {/* Timeline Steps */}
            <div className="mt-8 space-y-0 relative border-l border-[#ead9db] ml-5 md:ml-7 py-2">
              {path.courses.map((course, index) => (
                <div key={course.id} className="relative pl-8 pb-10 last:pb-2">
                  {/* Dot / step counter */}
                  <span className="absolute -left-[20px] top-1.5 flex h-10 w-10 items-center justify-center rounded-full border-2 border-[#dfbfbd] bg-white text-[#8a0018] font-extrabold text-sm shadow-sm">
                          {course.displayOrder || index + 1}
                  </span>
                  
                  {/* Content Card */}
                  <article className="group flex flex-col md:flex-row gap-5 rounded-3xl border border-[#ead9db] bg-white p-5 shadow-sm transition-all duration-200 hover:border-[#dfbfbd] hover:shadow-md">
                    {/* Thumbnail */}
                    {course.thumbnailUrl && (
                      <div className="h-28 w-full md:w-48 overflow-hidden rounded-2xl bg-[#f4eeee] shrink-0">
                        <img
                          alt={course.title}
                          className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
                          src={course.thumbnailUrl}
                        />
                      </div>
                    )}
                    
                    {/* Text info */}
                    <div className="min-w-0 flex-1 flex flex-col justify-center">
                      <span className="text-[11px] font-extrabold uppercase tracking-[0.15em] text-[#8c716f]">
                        Giai đoạn {course.displayOrder || index + 1}
                      </span>
                      <h2 className="mt-2 font-['Manrope'] text-xl font-extrabold text-[#1f1717] group-hover:text-[#730014] transition-colors">
                        {course.title}
                      </h2>
                      <p className="mt-2 text-sm leading-relaxed text-[#584140] line-clamp-2">
                        {course.shortDescription || course.description}
                      </p>
                      <div className="mt-3 flex items-center gap-3">
                        {course.owned ? (
                          <span className="inline-flex items-center gap-1 text-xs font-extrabold text-emerald-700"><CheckCircle2 className="h-4 w-4" /> Đã sở hữu</span>
                        ) : (
                          <span className="text-sm font-extrabold text-[#4b0009]">{formatCoursePrice(course.currentPrice)}</span>
                        )}
                      </div>
                    </div>
                    
                    {/* Link button */}
                    <div className="flex items-center shrink-0">
                      <Link
                        className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition-all hover:bg-[#730014] active:scale-95 shadow-sm"
                        to={`/courses/${course.slug}`}
                      >
                        {course.owned ? 'Vào khóa học' : 'Xem khóa học'}
                        <ArrowRight className="h-4 w-4" />
                      </Link>
                    </div>
                  </article>
                </div>
              ))}
            </div>
          </>
        )}
      </main>
      <CourseFooter />
    </div>
  );
}
