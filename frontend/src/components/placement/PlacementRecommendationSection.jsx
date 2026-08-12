import { ArrowRight, BookOpen, Building2, CheckCircle2, Circle, Laptop, LockKeyhole, RefreshCw, Route } from 'lucide-react';
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
        <div className="h-7 w-64 animate-pulse rounded-lg bg-[#ead9db]" />
        <div className="grid gap-4 md:grid-cols-3">
          {[1, 2, 3].map((item) => <div className="h-52 animate-pulse rounded-3xl bg-white" key={item} />)}
        </div>
      </section>
    );
  }

  if (error) {
    return (
      <section className="mt-8 rounded-3xl border border-rose-200 bg-rose-50 p-6 text-center">
        <p className="text-sm font-semibold text-rose-800">{error}</p>
        <button className="mt-4 inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-bold text-white" onClick={onRetry} type="button"><RefreshCw className="h-4 w-4" /> Thử lại</button>
      </section>
    );
  }

  if (!recommendation) return null;
  if (!recommendation.recommendationReady) {
    return (
      <section className="mt-8 rounded-3xl border border-[#dfbfbd] bg-[#fffaf9] p-6 md:p-8">
        <p className="text-xs font-black uppercase tracking-[0.18em] text-[#8a0018]">Lộ trình đang được chuẩn bị</p>
        <h2 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">Kết quả của bạn đang chờ xác nhận</h2>
        <p className="mt-3 max-w-2xl text-sm leading-7 text-[#584140]">{recommendation.message}</p>
        <button className="mt-5 inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-bold text-white" onClick={onRetry} type="button"><RefreshCw className="h-4 w-4" /> Kiểm tra lại kết quả</button>
      </section>
    );
  }

  const grouped = groupPlacementRecommendations(recommendation);
  const path = recommendation.recommendedLearningPath;
  return (
    <div className="mt-8 space-y-8">
      <section>
        <p className="text-xs font-black uppercase tracking-[0.18em] text-[#8a0018]">Kỹ năng cần ưu tiên</p>
        <div className="mt-3 flex flex-wrap gap-2">
          {(recommendation.weakSkills || []).map((skill) => <span className="rounded-full bg-[#fff0f1] px-4 py-2 text-sm font-bold text-[#8a0018]" key={skill}>{skillLabels[skill] || skill}</span>)}
          {!recommendation.weakSkills?.length ? <span className="text-sm text-[#584140]">Chưa có kỹ năng nổi bật cần ưu tiên.</span> : null}
        </div>
      </section>

      <section>
        <h2 className="font-['Manrope'] text-2xl font-black text-[#341c1d]">Khóa học phù hợp với bạn</h2>
        <div className="mt-5 space-y-7">
          <RecommendationGroup icon={Building2} items={grouped.offline} title="Chương trình tại trung tâm" type="training" />
          <RecommendationGroup icon={Laptop} items={grouped.virtual} title="Chương trình Virtual" type="training" />
          <RecommendationGroup icon={BookOpen} items={grouped.online} title="Khóa Online" type="online" />
          {!grouped.offline.length && !grouped.virtual.length && !grouped.online.length ? <p className="rounded-2xl border border-dashed border-[#dfbfbd] bg-white p-6 text-sm text-[#584140]">Chưa có khóa học đã xuất bản phù hợp với kết quả hiện tại.</p> : null}
        </div>
      </section>

      {path ? <LearningPathPreview path={path} /> : null}

      {recommendation.targetMissing ? (
        <section className="rounded-3xl border border-[#dfbfbd] bg-white p-6">
          <p className="font-bold text-[#341c1d]">Bạn chưa đặt mục tiêu điểm số.</p>
          <p className="mt-1 text-sm text-[#584140]">Cập nhật mục tiêu để lộ trình chính xác hơn.</p>
          <Link className="mt-4 inline-flex rounded-2xl border border-[#8a0018]/25 px-5 py-3 text-sm font-bold text-[#8a0018]" to="/complete-profile">Cập nhật hồ sơ</Link>
        </section>
      ) : null}
    </div>
  );
}

function RecommendationGroup({ icon: Icon, items, title, type }) {
  if (!items.length) return null;
  return (
    <div>
      <div className="flex items-center gap-2"><Icon className="h-5 w-5 text-[#8a0018]" /><h3 className="font-['Manrope'] text-lg font-extrabold text-[#341c1d]">{title}</h3></div>
      <div className="mt-3 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {items.map((item) => type === 'training' ? <TrainingCard item={item} key={item.id} /> : <OnlineCard item={item} key={item.id} />)}
      </div>
    </div>
  );
}

function TrainingCard({ item }) {
  const price = item.salePrice ?? item.price;
  return (
    <article className="flex min-h-64 flex-col rounded-3xl border border-[#ead9db] bg-white p-5 shadow-sm">
      <p className="text-xs font-black uppercase tracking-[0.14em] text-[#8a0018]">{getDeliveryModeLabel(item.deliveryMode)} · {getPlacementLevelLabel(item.entryPlacementLevel)}</p>
      <h4 className="mt-2 font-['Manrope'] text-xl font-black text-[#341c1d]">{item.title}</h4>
      <p className="mt-3 text-sm leading-6 text-[#584140]">{item.recommendationReason}</p>
      <div className="mt-auto flex items-end justify-between gap-3 pt-5">
        <div><p className="text-xs text-[#8c716f]">{item.totalSessions || 0} buổi</p>{price != null ? <p className="mt-1 font-black text-[#8a0018]">{currency(price)}</p> : null}</div>
        <Link className="inline-flex items-center gap-1 rounded-xl bg-[#4b0009] px-4 py-2.5 text-xs font-bold text-white" to={`/opening-schedule?programId=${item.id}`}>Xem chương trình <ArrowRight className="h-3.5 w-3.5" /></Link>
      </div>
    </article>
  );
}

function OnlineCard({ item }) {
  return (
    <article className="flex min-h-64 flex-col rounded-3xl border border-[#ead9db] bg-white p-5 shadow-sm">
      <p className="text-xs font-black uppercase tracking-[0.14em] text-[#8a0018]">Online · {getPlacementLevelLabel(item.level)}</p>
      <h4 className="mt-2 font-['Manrope'] text-xl font-black text-[#341c1d]">{item.title}</h4>
      <p className="mt-3 text-sm leading-6 text-[#584140]">{item.recommendationReason || item.shortDescription}</p>
      <Link className="mt-auto inline-flex w-fit items-center gap-1 rounded-xl bg-[#4b0009] px-4 py-2.5 text-xs font-bold text-white" to={`/courses/${item.slug}`}>Xem khóa học <ArrowRight className="h-3.5 w-3.5" /></Link>
    </article>
  );
}

function LearningPathPreview({ path }) {
  return (
    <section className="rounded-3xl border border-[#ead9db] bg-white p-6 md:p-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div><p className="text-xs font-black uppercase tracking-[0.18em] text-[#8a0018]">Lộ trình đề xuất</p><h2 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">{path.name}</h2>{path.recommendationReason ? <p className="mt-2 text-sm text-[#584140]">{path.recommendationReason}</p> : null}</div>
        <Link className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-bold text-white" to="/learning-path">Xem lộ trình chi tiết <ArrowRight className="h-4 w-4" /></Link>
      </div>
      <div className="mt-6 grid gap-3 md:grid-cols-2">
        {(path.courses || []).map((step, index) => {
          const waived = step.stepStatus === 'PLACEMENT_WAIVED';
          const current = step.stepStatus === 'CURRENT';
          const Icon = waived || step.stepStatus === 'COMPLETED' ? CheckCircle2 : current ? Circle : step.stepStatus === 'LOCKED' ? LockKeyhole : Route;
          return <div className={`rounded-2xl border p-4 ${current ? 'border-[#8a0018] bg-[#fff0f1]' : 'border-[#ead9db] bg-[#fffdfc]'}`} key={step.courseId}><div className="flex items-start gap-3"><Icon className={`mt-0.5 h-5 w-5 ${current ? 'text-[#8a0018]' : 'text-[#8c716f]'}`} /><div><p className="text-xs font-bold text-[#8c716f]">Giai đoạn {step.learningPathOrder || index + 1}</p><p className="mt-1 font-extrabold text-[#341c1d]">{step.title}</p><p className="mt-1 text-xs font-bold text-[#730014]">{getLearningPathStepLabel(step.stepStatus)}</p></div></div></div>;
        })}
      </div>
    </section>
  );
}
