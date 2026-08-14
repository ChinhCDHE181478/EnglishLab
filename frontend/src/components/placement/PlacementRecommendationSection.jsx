import {
  ArrowRight,
  BookOpen,
  Building2,
  CheckCircle2,
  Clock,
  Laptop,
  LockKeyhole,
  RefreshCw,
  Route,
  Sparkles,
  Zap,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import {
  getDeliveryModeLabel,
  getLearningPathStepLabel,
  getPlacementLevelLabel,
  groupPlacementRecommendations,
} from '../../utils/placementRecommendation';

const currency = (value) => (
  value == null ? '' : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value)
);

const skillLabels = {
  LISTENING: 'Listening',
  READING: 'Reading',
  WRITING: 'Writing',
  SPEAKING: 'Speaking',
};

export default function PlacementRecommendationSection({ error, loading, onRetry, recommendation }) {
  if (loading) {
    return (
      <section className="mt-8 space-y-4" aria-label="Đang tải gợi ý học tập">
        <div className="h-6 w-60 animate-pulse rounded-lg bg-[#ead9db]/60" />
        <div className="grid gap-4 md:grid-cols-3">
          {[1, 2, 3].map((item) => (
            <div className="h-64 animate-pulse rounded-2xl border border-[#ead9db]/50 bg-white p-5 shadow-xs" key={item} />
          ))}
        </div>
      </section>
    );
  }

  if (error) {
    return (
      <section className="mt-8 rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center">
        <p className="text-sm font-semibold text-rose-800">{error}</p>
        <button
          className="mt-3 inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4 py-2.5 text-xs font-bold text-white shadow-xs transition hover:bg-[#8a0018]"
          onClick={onRetry}
          type="button"
        >
          <RefreshCw className="h-3.5 w-3.5" /> Thử lại
        </button>
      </section>
    );
  }

  if (!recommendation) return null;
  if (!recommendation.recommendationReady) {
    return (
      <section className="mt-8 rounded-2xl border border-[#dfbfbd]/80 bg-[#fffaf9] p-6">
        <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#730014]">Lộ trình đang được chuẩn bị</p>
        <h2 className="mt-1 font-['Manrope'] text-xl font-bold text-[#0b1c30]">Kết quả của bạn đang chờ xác nhận</h2>
        <p className="mt-2 text-sm leading-relaxed text-[#584140]">{recommendation.message}</p>
        <button
          className="mt-4 inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4 py-2.5 text-xs font-bold text-white shadow-xs transition hover:bg-[#8a0018]"
          onClick={onRetry}
          type="button"
        >
          <RefreshCw className="h-3.5 w-3.5" /> Kiểm tra lại kết quả
        </button>
      </section>
    );
  }

  const grouped = groupPlacementRecommendations(recommendation);
  const path = recommendation.recommendedLearningPath;

  return (
    <div className="mt-8 space-y-8">
      {/* Recommended Courses Section */}
      <section className="space-y-6">
        <div className="border-b border-[#ead9db] pb-3">
          <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#730014]">Gợi ý từ hệ thống</p>
          <h2 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#0b1c30]">Khóa học phù hợp với trình độ của bạn</h2>
        </div>

        <div className="space-y-7">
          <RecommendationGroup icon={BookOpen} items={grouped.online} title="Khóa học Online có bài giảng" type="online" />
          <RecommendationGroup icon={Building2} items={grouped.offline} title="Chương trình đào tạo tại trung tâm" type="training" />
          <RecommendationGroup icon={Laptop} items={grouped.virtual} title="Chương trình đào tạo Virtual (Lớp học trực tuyến)" type="training" />
          {!grouped.offline.length && !grouped.virtual.length && !grouped.online.length ? (
            <div className="rounded-2xl border border-dashed border-[#dfbfbd] bg-white p-6 text-center">
              <p className="text-sm text-[#584140]">Chưa có khóa học xuất bản phù hợp với kết quả hiện tại.</p>
            </div>
          ) : null}
        </div>
      </section>

      {/* Recommended Learning Path Preview */}
      {path ? <LearningPathPreview path={path} /> : null}

      {/* Missing Target Warning */}
      {recommendation.targetMissing ? (
        <section className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 rounded-2xl border border-amber-200 bg-amber-50/60 p-5">
          <div>
            <p className="text-sm font-bold text-[#0b1c30]">Bạn chưa đặt mục tiêu điểm số cá nhân</p>
            <p className="mt-0.5 text-xs text-[#584140]">Cập nhật mục tiêu để gợi ý lộ trình phù hợp và chính xác hơn.</p>
          </div>
          <Link
            className="shrink-0 inline-flex items-center gap-1.5 rounded-xl border border-[#730014] bg-white px-4 py-2 text-xs font-bold text-[#730014] transition hover:bg-[#fff0f1]"
            to="/complete-profile"
          >
            Cập nhật mục tiêu <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </section>
      ) : null}
    </div>
  );
}

function RecommendationGroup({ icon: Icon, items, title, type }) {
  if (!items.length) return null;
  return (
    <div className="space-y-3.5">
      <div className="flex items-center gap-2">
        <Icon className="h-4.5 w-4.5 text-[#730014]" />
        <h3 className="font-['Manrope'] text-base font-bold text-[#0b1c30]">{title}</h3>
        <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-bold text-slate-600">{items.length}</span>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {items.map((item) => (type === 'online' ? <OnlineCard item={item} key={item.id} /> : <TrainingCard item={item} key={item.id} />))}
      </div>
    </div>
  );
}

function OnlineCard({ item }) {
  const price = item.salePrice ?? item.price;
  return (
    <article className="group flex flex-col overflow-hidden rounded-2xl border border-[#ead9db] bg-white shadow-xs transition duration-200 hover:border-[#730014] hover:shadow-md">
      {/* Thumbnail */}
      <div className="relative aspect-[16/9] w-full overflow-hidden bg-slate-100">
        {item.thumbnailUrl ? (
          <img
            alt={item.title}
            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
            src={item.thumbnailUrl}
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-[#730014]/10 to-[#4b0009]/20 text-[#730014]">
            <BookOpen className="h-10 w-10 opacity-40" />
          </div>
        )}
        <div className="absolute left-3 top-3 flex flex-wrap gap-1.5">
          <span className="rounded-md bg-white/90 px-2 py-0.5 text-[10px] font-bold text-[#730014] shadow-xs backdrop-blur-xs">
            Online
          </span>
          {item.level ? (
            <span className="rounded-md bg-slate-900/80 px-2 py-0.5 text-[10px] font-bold text-white backdrop-blur-xs">
              {getPlacementLevelLabel(item.level)}
            </span>
          ) : null}
        </div>
      </div>

      <div className="flex flex-1 flex-col p-4">
        <h4 className="font-['Manrope'] text-base font-bold text-[#0b1c30] group-hover:text-[#730014] transition-colors line-clamp-2">
          {item.title}
        </h4>

        <p className="mt-1.5 line-clamp-2 text-xs leading-relaxed text-[#584140]">
          {item.recommendationReason || item.shortDescription || 'Khóa học thiết kế phù hợp giúp nâng cao band điểm nhanh chóng.'}
        </p>

        <div className="mt-auto border-t border-slate-100 pt-3 flex items-center justify-between gap-2">
          <div>
            {price != null ? (
              <p className="font-['Manrope'] text-sm font-extrabold text-[#730014]">
                {currency(price)}
              </p>
            ) : (
              <span className="text-xs font-semibold text-slate-500">Miễn phí</span>
            )}
          </div>

          <Link
            className="inline-flex items-center gap-1 rounded-xl bg-[#730014] px-3.5 py-2 text-xs font-bold text-white transition hover:bg-[#8a0018]"
            to={`/courses/${item.slug}`}
          >
            Xem khóa học <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
      </div>
    </article>
  );
}

function TrainingCard({ item }) {
  const price = item.salePrice ?? item.price;
  return (
    <article className="group flex flex-col overflow-hidden rounded-2xl border border-[#ead9db] bg-white p-5 shadow-xs transition duration-200 hover:border-[#730014] hover:shadow-md">
      {item.thumbnailUrl ? (
        <div className="mb-3.5 aspect-[16/9] w-full overflow-hidden rounded-xl bg-slate-100">
          <img alt={item.title} className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105" src={item.thumbnailUrl} />
        </div>
      ) : null}

      <div className="flex items-center justify-between gap-2">
        <span className="rounded-md bg-[#fff0f1] px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-[#730014]">
          {getDeliveryModeLabel(item.deliveryMode)}
        </span>
        <span className="text-[11px] font-semibold text-slate-500">
          Đầu vào: {getPlacementLevelLabel(item.entryPlacementLevel)}
        </span>
      </div>

      <h4 className="mt-2.5 font-['Manrope'] text-base font-bold text-[#0b1c30] group-hover:text-[#730014] transition-colors line-clamp-2">
        {item.title}
      </h4>

      <p className="mt-1.5 line-clamp-2 text-xs leading-relaxed text-[#584140]">
        {item.recommendationReason || 'Chương trình đào tạo chuẩn đầu ra theo kết quả đánh giá.'}
      </p>

      <div className="mt-auto border-t border-slate-100 pt-3 flex items-center justify-between gap-2">
        <div>
          {item.totalSessions ? (
            <span className="flex items-center gap-1 text-[11px] text-slate-500">
              <Clock className="h-3 w-3" /> {item.totalSessions} buổi
            </span>
          ) : null}
          {price != null ? (
            <p className="font-['Manrope'] text-sm font-extrabold text-[#730014]">
              {currency(price)}
            </p>
          ) : null}
        </div>

        <Link
          className="inline-flex items-center gap-1 rounded-xl bg-[#730014] px-3.5 py-2 text-xs font-bold text-white transition hover:bg-[#8a0018]"
          to={`/opening-schedule?programId=${item.id}`}
        >
          Xem lịch lớp <ArrowRight className="h-3.5 w-3.5" />
        </Link>
      </div>
    </article>
  );
}

function LearningPathPreview({ path }) {
  return (
    <section className="rounded-2xl border border-[#ead9db] bg-white p-6 shadow-xs">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#730014]">Lộ trình khuyến nghị</p>
          <h2 className="mt-1 font-['Manrope'] text-xl font-bold text-[#0b1c30]">{path.name}</h2>
          {path.recommendationReason ? (
            <p className="mt-1 text-xs text-[#584140]">{path.recommendationReason}</p>
          ) : null}
        </div>

        <Link
          className="inline-flex items-center gap-1.5 rounded-xl border border-[#730014] bg-[#fff0f1] px-4 py-2 text-xs font-bold text-[#730014] transition hover:bg-[#730014] hover:text-white"
          to="/learning-path"
        >
          Xem lộ trình chi tiết <ArrowRight className="h-3.5 w-3.5" />
        </Link>
      </div>

      <div className="mt-5 grid gap-3 md:grid-cols-2">
        {(path.courses || []).map((step, index) => {
          const waived = step.stepStatus === 'PLACEMENT_WAIVED';
          const current = step.stepStatus === 'CURRENT';
          const Icon = waived || step.stepStatus === 'COMPLETED' ? CheckCircle2 : current ? Zap : step.stepStatus === 'LOCKED' ? LockKeyhole : Route;
          return (
            <div
              className={`flex items-start gap-3 rounded-xl border p-3.5 transition ${
                current
                  ? 'border-[#730014] bg-[#fff0f1]'
                  : waived
                  ? 'border-emerald-200 bg-emerald-50/50'
                  : 'border-slate-100 bg-slate-50/50'
              }`}
              key={step.courseId}
            >
              <Icon className={`mt-0.5 h-4.5 w-4.5 shrink-0 ${current ? 'text-[#730014]' : waived ? 'text-emerald-600' : 'text-slate-400'}`} />
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-2">
                  <p className="text-[11px] font-bold text-slate-500">Giai đoạn {step.learningPathOrder || index + 1}</p>
                  <span className={`text-[10px] font-bold ${current ? 'text-[#730014]' : waived ? 'text-emerald-700' : 'text-slate-500'}`}>
                    {getLearningPathStepLabel(step.stepStatus)}
                  </span>
                </div>
                <p className="mt-1 text-sm font-bold text-[#0b1c30] truncate">{step.title}</p>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
