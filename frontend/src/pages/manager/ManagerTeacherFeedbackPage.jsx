import { useDeferredValue, useEffect, useMemo, useState } from 'react';
import {
  BarChart3,
  Eye,
  Inbox,
  LoaderCircle,
  MessageSquareText,
  RefreshCw,
  Search,
  ShieldCheck,
  Star,
  TrendingUp,
  Users,
  X,
} from 'lucide-react';
import teacherProfessionalApi from '../../api/teacherProfessionalApi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../../components/ui/Pagination';

const TEACHER_PAGE_SIZE = 8;
const FEEDBACK_PAGE_SIZE = 5;
const feedbackFilterOptions = [
  { label: 'Tất cả giáo viên', value: 'ALL' },
  { label: 'Đã có phản hồi', value: 'HAS_FEEDBACK' },
  { label: 'Chưa có phản hồi', value: 'NO_FEEDBACK' },
];
const paceLabel = { TOO_SLOW: 'Hơi chậm', JUST_RIGHT: 'Phù hợp', TOO_FAST: 'Hơi nhanh' };

const formatScore = (value) => (value == null ? '—' : Number(value).toFixed(2));
const formatPercent = (value) => (value == null ? '—' : `${Number(value).toFixed(0)}%`);
const formatDateTime = (value) => (value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : '—');

export default function ManagerTeacherFeedbackPage() {
  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [selected, setSelected] = useState(null);
  const [query, setQuery] = useState('');
  const [feedbackFilter, setFeedbackFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState('');
  const [detailError, setDetailError] = useState('');
  const deferredQuery = useDeferredValue(query.trim().toLocaleLowerCase('vi-VN'));

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      setItems(await teacherProfessionalApi.listForManager());
    } catch (requestError) {
      setError(requestError?.response?.data?.message || 'Không thể tải tổng hợp đánh giá.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;
    const loadInitialData = async () => {
      setLoading(true);
      setError('');
      try {
        const data = await teacherProfessionalApi.listForManager();
        if (active) setItems(data);
      } catch (requestError) {
        if (active) setError(requestError?.response?.data?.message || 'Không thể tải tổng hợp đánh giá.');
      } finally {
        if (active) setLoading(false);
      }
    };
    loadInitialData();
    return () => { active = false; };
  }, []);

  const filtered = useMemo(() => items.filter((item) => {
    const matchesQuery = !deferredQuery
      || item.teacherName?.toLocaleLowerCase('vi-VN').includes(deferredQuery);
    const hasFeedback = Number(item.responseCount || 0) > 0;
    const matchesFeedback = feedbackFilter === 'ALL'
      || (feedbackFilter === 'HAS_FEEDBACK' && hasFeedback)
      || (feedbackFilter === 'NO_FEEDBACK' && !hasFeedback);
    return matchesQuery && matchesFeedback;
  }), [deferredQuery, feedbackFilter, items]);

  const summary = useMemo(() => {
    const scoredItems = items.filter((item) => item.overallScore != null && Number(item.responseCount || 0) > 0);
    const totalResponses = items.reduce((total, item) => total + Number(item.responseCount || 0), 0);
    const weightedScore = scoredItems.reduce(
      (total, item) => total + (Number(item.overallScore) * Number(item.responseCount || 0)),
      0,
    );
    const weightedRecommendation = scoredItems.reduce(
      (total, item) => total + (Number(item.recommendationPercent || 0) * Number(item.responseCount || 0)),
      0,
    );
    return {
      totalTeachers: items.length,
      teachersWithFeedback: scoredItems.length,
      totalResponses,
      averageScore: totalResponses ? weightedScore / totalResponses : null,
      recommendationPercent: totalResponses ? weightedRecommendation / totalResponses : null,
    };
  }, [items]);

  const resetKey = `${deferredQuery}|${feedbackFilter}`;
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    filtered,
    TEACHER_PAGE_SIZE,
    resetKey,
  );

  const openDetail = async (teacherId) => {
    setSelectedId(teacherId);
    setSelected(null);
    setDetailError('');
    setDetailLoading(true);
    try {
      setSelected(await teacherProfessionalApi.getForManager(teacherId));
    } catch (requestError) {
      setDetailError(requestError?.response?.data?.message || 'Không thể tải phản hồi chi tiết.');
    } finally {
      setDetailLoading(false);
    }
  };

  const closeDetail = () => {
    setSelectedId(null);
    setSelected(null);
    setDetailError('');
  };

  useEffect(() => {
    if (!selectedId) return undefined;
    const previousOverflow = document.body.style.overflow;
    const closeOnEscape = (event) => {
      if (event.key === 'Escape' && !detailLoading) closeDetail();
    };
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', closeOnEscape);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', closeOnEscape);
    };
  }, [detailLoading, selectedId]);

  return (
    <div className="space-y-5">
      <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard icon={Users} label="Tổng giáo viên" value={summary.totalTeachers} />
        <SummaryCard icon={MessageSquareText} label="Tổng phản hồi" value={summary.totalResponses} />
        <SummaryCard icon={Star} label="Điểm trung bình" value={formatScore(summary.averageScore)} suffix="/5" />
        <SummaryCard icon={TrendingUp} label="Sẵn sàng giới thiệu" value={formatPercent(summary.recommendationPercent)} />
      </section>

      <section className="overflow-hidden rounded-[24px] border border-[#e8d9d8] bg-white shadow-sm">
        <div className="border-b border-[#f0e4e2] bg-gradient-to-r from-[#fffafa] to-white px-5 py-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2.5">
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#fff1f3] text-[#730014]">
                <BarChart3 className="h-5 w-5" />
              </span>
              <div>
                <h2 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Hiệu suất giảng viên</h2>
                <p className="text-xs text-[#8b706e]">
                  {loading ? 'Đang tải dữ liệu...' : `${totalItems} giáo viên phù hợp · ${summary.teachersWithFeedback} giáo viên đã có phản hồi`}
                </p>
              </div>
            </div>
            <button
              className="inline-flex h-10 items-center gap-2 rounded-xl border border-[#dfbfbd]/70 bg-white px-3.5 text-xs font-extrabold text-[#730014] transition hover:bg-[#fff1f3] disabled:opacity-60"
              disabled={loading}
              onClick={load}
              type="button"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
              Tải lại
            </button>
          </div>

          <div className="mt-4 grid gap-3 lg:grid-cols-[minmax(0,1fr)_220px]">
            <label className="relative block">
              <span className="sr-only">Tìm giáo viên</span>
              <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[#b89a98]" />
              <input
                className="h-11 w-full rounded-xl border border-[#e8d9d8] bg-white pl-10 pr-4 text-sm text-[#2b2828] outline-none transition placeholder:text-[#b89a98] focus:border-[#730014] focus:ring-2 focus:ring-[#730014]/10"
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Tìm theo tên giáo viên..."
                value={query}
              />
            </label>
            <BrandedSelect
              onChange={(event) => setFeedbackFilter(event.target.value)}
              options={feedbackFilterOptions}
              value={feedbackFilter}
            />
          </div>
        </div>

        {loading ? <State text="Đang tải dữ liệu đánh giá..." loading /> : null}
        {!loading && error ? <State action={load} danger text={error} /> : null}
        {!loading && !error && !pageItems.length ? (
          <State text={items.length ? 'Không tìm thấy giáo viên phù hợp.' : 'Chưa có giáo viên để tổng hợp đánh giá.'} />
        ) : null}
        {!loading && !error && pageItems.length ? (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-[860px] w-full text-left">
                <thead>
                  <tr className="border-b border-[#f0e4e2] bg-[#fffbfb] text-[11px] font-extrabold uppercase tracking-wider text-slate-500">
                    <th className="px-5 py-3">Giáo viên</th>
                    <th className="px-5 py-3 text-center">Phản hồi</th>
                    <th className="px-5 py-3 text-center">Điểm tổng hợp</th>
                    <th className="px-5 py-3 text-center">Giới thiệu</th>
                    <th className="px-5 py-3">Trạng thái dữ liệu</th>
                    <th className="px-5 py-3 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#f5eceb]">
                  {pageItems.map((item) => {
                    const hasFeedback = Number(item.responseCount || 0) > 0;
                    const scoreAvailable = hasFeedback && !item.protectedByAnonymity;
                    return (
                      <tr className="transition hover:bg-[#fffafa]" key={item.teacherId}>
                        <td className="px-5 py-4">
                          <div className="flex items-center gap-3">
                            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#730014] text-sm font-extrabold text-white">
                              {getInitials(item.teacherName)}
                            </span>
                            <div className="min-w-0">
                              <p className="truncate text-sm font-bold text-[#0b1c30]">{item.teacherName || 'Chưa cập nhật tên'}</p>
                              <p className="mt-0.5 text-xs text-slate-500">Mã giáo viên #{item.teacherId}</p>
                            </div>
                          </div>
                        </td>
                        <td className="px-5 py-4 text-center text-sm font-extrabold text-[#0b1c30]">{item.responseCount || 0}</td>
                        <td className="px-5 py-4 text-center">
                          <span className="text-sm font-extrabold text-[#730014]">{scoreAvailable ? `${formatScore(item.overallScore)}/5` : '—'}</span>
                        </td>
                        <td className="px-5 py-4 text-center text-sm font-bold text-slate-700">
                          {scoreAvailable ? formatPercent(item.recommendationPercent) : '—'}
                        </td>
                        <td className="px-5 py-4">
                          <FeedbackStatus item={item} />
                        </td>
                        <td className="px-5 py-4 text-right">
                          <button
                            className="inline-flex h-9 items-center gap-1.5 rounded-xl border border-[#dfbfbd]/70 bg-white px-3 text-xs font-extrabold text-[#730014] transition hover:bg-[#fff1f3]"
                            onClick={() => openDetail(item.teacherId)}
                            type="button"
                          >
                            <Eye className="h-3.5 w-3.5" />
                            Xem chi tiết
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {totalPages > 1 ? (
              <div className="border-t border-[#f0e4e2] bg-[#fffbfb] px-5 py-4">
                <Pagination
                  onChange={setPage}
                  page={page}
                  pageSize={TEACHER_PAGE_SIZE}
                  totalItems={totalItems}
                  totalPages={totalPages}
                />
              </div>
            ) : null}
          </>
        ) : null}
      </section>

      {selectedId ? (
        <TeacherFeedbackModal
          detail={selected}
          error={detailError}
          loading={detailLoading}
          onClose={closeDetail}
          onRetry={() => openDetail(selectedId)}
          teacherId={selectedId}
        />
      ) : null}
    </div>
  );
}

function SummaryCard({ icon: Icon, label, value, suffix = '' }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between gap-3">
        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#fff1f3] text-[#730014]">
          <Icon className="h-5 w-5" />
        </span>
        <p className="font-['Manrope'] text-2xl font-extrabold text-[#0b1c30]">
          {value}<span className="ml-0.5 text-sm text-slate-400">{suffix}</span>
        </p>
      </div>
      <p className="mt-3 text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{label}</p>
    </article>
  );
}

function FeedbackStatus({ item }) {
  if (!Number(item.responseCount || 0)) {
    return <span className="inline-flex rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-extrabold text-slate-600">Chưa có phản hồi</span>;
  }
  if (item.protectedByAnonymity) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2.5 py-1 text-[11px] font-extrabold text-amber-700">
        <ShieldCheck className="h-3.5 w-3.5" />
        Chưa đủ ngưỡng tổng hợp
      </span>
    );
  }
  return <span className="inline-flex rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-extrabold text-emerald-700">Đã có dữ liệu</span>;
}

function TeacherFeedbackModal({ detail, error, loading, onClose, onRetry, teacherId }) {
  const feedback = detail?.feedback || [];
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    feedback,
    FEEDBACK_PAGE_SIZE,
    teacherId,
  );
  const aggregate = detail?.aggregate;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-3 backdrop-blur-sm sm:p-5"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !loading) onClose();
      }}
    >
      <section
        aria-labelledby="teacher-feedback-title"
        aria-modal="true"
        className="flex max-h-[94dvh] w-full max-w-6xl flex-col overflow-hidden rounded-[28px] border border-white/70 bg-white shadow-2xl"
        role="dialog"
      >
        <header className="flex shrink-0 items-start justify-between gap-4 border-b border-[#f0e4e2] bg-gradient-to-r from-[#fffafa] to-white px-5 py-5 sm:px-6">
          <div className="min-w-0">
            <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Chi tiết hiệu suất giảng viên</p>
            <h2 id="teacher-feedback-title" className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
              {aggregate?.teacherName || (loading ? 'Đang tải dữ liệu' : `Giáo viên #${teacherId}`)}
            </h2>
            <p className="mt-1 text-xs text-[#8b706e]">Tổng hợp từ phản hồi ẩn danh sau khi lớp học kết thúc.</p>
          </div>
          <button
            aria-label="Đóng chi tiết hiệu suất"
            className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-[#dfbfbd]/60 bg-white text-[#8b706e] transition hover:border-[#730014] hover:bg-[#fff1f3] hover:text-[#730014] disabled:opacity-50"
            disabled={loading}
            onClick={onClose}
            type="button"
          >
            <X className="h-5 w-5" />
          </button>
        </header>

        <div className="min-h-0 flex-1 overflow-y-auto px-5 py-5 sm:px-6">
          {loading ? <State borderless loading text="Đang tải phản hồi chi tiết..." /> : null}
          {!loading && error ? <State action={onRetry} borderless danger text={error} /> : null}
          {!loading && !error && detail ? (
            <div className="space-y-6">
              <section>
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
                  <DetailMetric icon={Star} label="Tổng hợp" value={`${formatScore(aggregate?.overallScore)}/5`} />
                  <DetailMetric icon={BarChart3} label="Trình bày" value={formatScore(aggregate?.clarityScore)} />
                  <DetailMetric icon={BarChart3} label="Tương tác" value={formatScore(aggregate?.engagementScore)} />
                  <DetailMetric icon={Users} label="Hỗ trợ" value={formatScore(aggregate?.learnerSupportScore)} />
                  <DetailMetric icon={MessageSquareText} label="Phản hồi bài" value={formatScore(aggregate?.feedbackTimelinessScore)} />
                  <DetailMetric icon={ShieldCheck} label="Chuyên nghiệp" value={formatScore(aggregate?.professionalismScore)} />
                </div>
                <div className="mt-3 flex flex-wrap items-center gap-2 rounded-2xl border border-[#f0e4e2] bg-[#fffafa] px-4 py-3 text-xs font-semibold text-[#584140]">
                  <span><strong>{aggregate?.responseCount || 0}</strong> phản hồi</span>
                  <span className="text-[#cfb9b7]">•</span>
                  <span><strong>{formatPercent(aggregate?.recommendationPercent)}</strong> sẵn sàng giới thiệu</span>
                  <span className="text-[#cfb9b7]">•</span>
                  <span>Nhịp độ phổ biến: <strong>{getDominantPace(aggregate?.paceDistribution)}</strong></span>
                </div>
              </section>

              <section>
                <div className="flex flex-wrap items-end justify-between gap-3">
                  <div>
                    <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Phản hồi của học viên</h3>
                    <p className="mt-1 text-xs text-slate-500">Nội dung không hiển thị danh tính người gửi.</p>
                  </div>
                  <span className="rounded-full bg-[#fff1f3] px-3 py-1 text-xs font-extrabold text-[#730014]">{totalItems} phản hồi</span>
                </div>

                {!pageItems.length ? (
                  <div className="mt-4 rounded-2xl border border-dashed border-[#e8d9d8] bg-[#fffbfb] px-5 py-12 text-center">
                    <Inbox className="mx-auto h-8 w-8 text-[#c5aaa8]" />
                    <p className="mt-3 text-sm font-extrabold text-[#2b2828]">Chưa có phản hồi</p>
                    <p className="mt-1 text-xs text-[#8b706e]">Phản hồi sẽ xuất hiện sau khi học viên hoàn tất đánh giá.</p>
                  </div>
                ) : (
                  <div className="mt-4 space-y-3">
                    {pageItems.map((item, index) => (
                      <FeedbackCard
                        item={item}
                        key={item.feedbackId}
                        number={(page - 1) * FEEDBACK_PAGE_SIZE + index + 1}
                      />
                    ))}
                  </div>
                )}

                {totalPages > 1 ? (
                  <div className="mt-4 rounded-2xl border border-[#f0e4e2] bg-[#fffbfb] px-4 py-3">
                    <Pagination
                      compact
                      onChange={setPage}
                      page={page}
                      pageSize={FEEDBACK_PAGE_SIZE}
                      totalItems={totalItems}
                      totalPages={totalPages}
                    />
                  </div>
                ) : null}
              </section>
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}

function DetailMetric({ icon: Icon, label, value }) {
  return (
    <div className="rounded-2xl border border-[#f0e4e2] bg-[#fffbfb] p-3.5">
      <div className="flex items-center gap-2 text-[#9b8582]">
        <Icon className="h-4 w-4" />
        <span className="text-[10px] font-extrabold uppercase tracking-[0.1em]">{label}</span>
      </div>
      <p className="mt-2 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{value}</p>
    </div>
  );
}

function FeedbackCard({ item, number }) {
  const criteria = [
    ['Trình bày', item.clarityScore],
    ['Tương tác', item.engagementScore],
    ['Hỗ trợ', item.learnerSupportScore],
    ['Phản hồi bài', item.feedbackTimelinessScore],
    ['Chuyên nghiệp', item.professionalismScore],
  ];

  return (
    <article className="rounded-2xl border border-[#eee2e3] bg-white p-4 sm:p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-extrabold text-[#2b2828]">Phản hồi ẩn danh #{number}</p>
          <p className="mt-1 text-xs font-semibold text-[#8c716f]">{item.classroomTitle || `Lớp #${item.classroomId}`}</p>
        </div>
        <div className="text-right">
          <p className="text-sm font-extrabold text-[#730014]">{formatScore(item.overallScore)}/5</p>
          <p className="mt-1 text-[11px] text-[#8c716f]">Cập nhật {formatDateTime(item.updatedAt || item.submittedAt)}</p>
        </div>
      </div>

      <div className="mt-3 flex flex-wrap gap-2">
        {criteria.map(([label, value]) => (
          <span className="rounded-lg bg-slate-100 px-2.5 py-1 text-[11px] font-bold text-slate-700" key={label}>
            {label}: {value}/5
          </span>
        ))}
        <span className="rounded-lg bg-[#fff1f3] px-2.5 py-1 text-[11px] font-bold text-[#730014]">
          Nhịp độ: {paceLabel[item.pace] || '—'}
        </span>
        <span className={`rounded-lg px-2.5 py-1 text-[11px] font-bold ${item.wouldRecommend ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
          {item.wouldRecommend ? 'Sẵn sàng giới thiệu' : 'Chưa sẵn sàng giới thiệu'}
        </span>
      </div>

      <div className="mt-4 grid gap-3 lg:grid-cols-2">
        <FeedbackText title="Điểm mạnh" value={item.strengths} />
        <FeedbackText title="Góp ý cải thiện" value={item.improvementSuggestions} />
      </div>
      {item.additionalComment ? (
        <div className="mt-3">
          <FeedbackText title="Nhận xét bổ sung" value={item.additionalComment} />
        </div>
      ) : null}
    </article>
  );
}

function FeedbackText({ title, value }) {
  return (
    <div className="rounded-xl bg-[#faf7f7] p-4">
      <p className="text-[10px] font-extrabold uppercase tracking-widest text-[#9b8582]">{title}</p>
      <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-[#584140]">{value || 'Không có nội dung.'}</p>
    </div>
  );
}

function State({ text, loading = false, danger = false, action, borderless = false }) {
  return (
    <div className={`flex min-h-64 flex-col items-center justify-center p-8 text-center ${borderless ? '' : 'bg-white'} ${danger ? 'text-rose-700' : 'text-[#756361]'}`}>
      {loading ? <LoaderCircle className="h-7 w-7 animate-spin" /> : <MessageSquareText className="h-8 w-8" />}
      <p className="mt-3 text-sm font-semibold">{text}</p>
      {action ? (
        <button className="mt-4 rounded-xl bg-[#730014] px-4 py-2.5 text-xs font-extrabold text-white transition hover:bg-[#4b0009]" onClick={action} type="button">
          Thử lại
        </button>
      ) : null}
    </div>
  );
}

function getInitials(name) {
  const words = String(name || '').trim().split(/\s+/).filter(Boolean);
  if (!words.length) return 'GV';
  return words.slice(-2).map((word) => word.charAt(0).toLocaleUpperCase('vi-VN')).join('');
}

function getDominantPace(distribution = {}) {
  const entries = Object.entries(distribution || {});
  if (!entries.length) return 'Chưa có dữ liệu';
  const [pace] = entries.sort((left, right) => Number(right[1]) - Number(left[1]))[0];
  return paceLabel[pace] || 'Chưa có dữ liệu';
}
