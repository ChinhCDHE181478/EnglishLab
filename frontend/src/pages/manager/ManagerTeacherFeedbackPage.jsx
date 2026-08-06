import { useEffect, useMemo, useState } from 'react';
import { BarChart3, LoaderCircle, MessageSquareText, Search, ShieldCheck, Star, Users } from 'lucide-react';
import teacherProfessionalApi from '../../api/teacherProfessionalApi';

const score = (value) => value == null ? '—' : Number(value).toFixed(2);
const dateTime = (value) => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : '—';
const paceLabel = { TOO_SLOW: 'Hơi chậm', JUST_RIGHT: 'Phù hợp', TOO_FAST: 'Hơi nhanh' };

export default function ManagerTeacherFeedbackPage() {
  const [items, setItems] = useState([]);
  const [selected, setSelected] = useState(null);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState('');
  const filtered = useMemo(() => items.filter((item) => (
    item.teacherName?.toLowerCase().includes(query.trim().toLowerCase())
  )), [items, query]);

  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        const data = await teacherProfessionalApi.listForManager();
        if (active) setItems(data);
      } catch (requestError) {
        if (active) setError(requestError?.response?.data?.message || 'Không thể tải tổng hợp đánh giá.');
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => { active = false; };
  }, []);

  const openDetail = async (teacherId) => {
    try {
      setDetailLoading(true);
      setError('');
      setSelected(await teacherProfessionalApi.getForManager(teacherId));
    } catch (requestError) {
      setError(requestError?.response?.data?.message || 'Không thể tải phản hồi chi tiết.');
    } finally {
      setDetailLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <section className="rounded-[24px] border border-[#ead9db] bg-white p-4 shadow-sm">
        <div className="relative">
          <Search className="absolute left-4 top-3.5 h-4 w-4 text-[#9b8582]" />
          <input className="w-full rounded-2xl border border-[#ead9db] py-3 pl-11 pr-4 text-sm outline-none focus:border-[#8a0018]" onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo tên giáo viên..." value={query} />
        </div>
      </section>
      {loading ? <State text="Đang tải dữ liệu đánh giá..." loading /> : null}
      {!loading && error ? <State text={error} danger /> : null}
      {!loading && !error && !filtered.length ? <State text="Chưa có học viên gửi đánh giá giáo viên." /> : null}
      {!loading && filtered.length ? (
        <section className="grid gap-4 xl:grid-cols-2">
          {filtered.map((item) => (
            <button className="rounded-[24px] border border-[#ead9db] bg-white p-5 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-[#c9949b]" key={item.teacherId} onClick={() => openDetail(item.teacherId)} type="button">
              <div className="flex items-start justify-between gap-4">
                <div><p className="text-xs font-extrabold uppercase tracking-widest text-[#8a0018]">Giáo viên</p><h2 className="mt-1 text-xl font-black text-[#2b2828]">{item.teacherName}</h2></div>
                <div className="rounded-2xl bg-[#fff0f1] px-4 py-2 text-right"><p className="text-2xl font-black text-[#730014]">{score(item.overallScore)}</p><p className="text-[10px] font-bold text-[#8c716f]">trên 5</p></div>
              </div>
              <div className="mt-5 grid grid-cols-2 gap-3">
                <Metric icon={Users} label="Phản hồi" value={item.responseCount} />
                <Metric icon={ShieldCheck} label="Sẵn sàng giới thiệu" value={`${score(item.recommendationPercent)}%`} />
              </div>
            </button>
          ))}
        </section>
      ) : null}
      {detailLoading ? <State text="Đang tải phản hồi ẩn danh..." loading /> : null}
      {!detailLoading && selected ? (
        <section className="rounded-[28px] border border-[#ead9db] bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div><p className="text-xs font-extrabold uppercase tracking-widest text-[#8a0018]">Chi tiết ẩn danh</p><h2 className="mt-1 text-2xl font-black text-[#2b2828]">{selected.aggregate.teacherName}</h2></div>
            <div className="flex items-center gap-2 rounded-2xl bg-[#fff7f7] px-4 py-3 text-sm font-bold text-[#730014]"><ShieldCheck className="h-5 w-5" /> Không hiển thị danh tính học viên</div>
          </div>
          <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <Metric icon={Star} label="Điểm tổng hợp" value={`${score(selected.aggregate.overallScore)}/5`} />
            <Metric icon={BarChart3} label="Trình bày" value={score(selected.aggregate.clarityScore)} />
            <Metric icon={BarChart3} label="Hỗ trợ" value={score(selected.aggregate.learnerSupportScore)} />
            <Metric icon={Users} label="Giới thiệu" value={`${score(selected.aggregate.recommendationPercent)}%`} />
          </div>
          <h3 className="mt-8 text-lg font-black text-[#2b2828]">Phản hồi của học viên</h3>
          <div className="mt-4 space-y-4">
            {selected.feedback?.map((item, index) => (
              <article className="rounded-2xl border border-[#eee2e3] p-5" key={item.feedbackId}>
                <div className="flex flex-wrap justify-between gap-3"><p className="font-extrabold text-[#2b2828]">Phản hồi ẩn danh #{index + 1} · {item.classroomTitle}</p><p className="text-xs text-[#8c716f]">Cập nhật {dateTime(item.updatedAt)}</p></div>
                <div className="mt-3 flex flex-wrap gap-2 text-xs font-bold"><span className="rounded-full bg-[#fff0f1] px-3 py-1 text-[#730014]">{score(item.overallScore)}/5</span><span className="rounded-full bg-slate-100 px-3 py-1 text-slate-700">Tốc độ: {paceLabel[item.pace] || '—'}</span><span className="rounded-full bg-slate-100 px-3 py-1 text-slate-700">{item.wouldRecommend ? 'Sẵn sàng giới thiệu' : 'Chưa sẵn sàng giới thiệu'}</span></div>
                <div className="mt-4 grid gap-3 lg:grid-cols-2"><Text title="Điểm mạnh" value={item.strengths} /><Text title="Góp ý cải thiện" value={item.improvementSuggestions} /></div>
                {item.additionalComment ? <div className="mt-3"><Text title="Nhận xét bổ sung" value={item.additionalComment} /></div> : null}
              </article>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  );
}

function Metric({ icon: Icon, label, value }) {
  return <div className="rounded-2xl bg-[#faf7f7] p-4"><Icon className="h-5 w-5 text-[#8a0018]" /><p className="mt-2 text-xl font-black text-[#2b2828]">{value}</p><p className="mt-1 text-xs font-bold text-[#756361]">{label}</p></div>;
}
function Text({ title, value }) {
  return <div className="rounded-xl bg-[#faf7f7] p-4"><p className="text-[10px] font-extrabold uppercase tracking-widest text-[#9b8582]">{title}</p><p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-[#584140]">{value}</p></div>;
}
function State({ text, loading = false, danger = false }) {
  return <div className={`flex min-h-60 items-center justify-center rounded-[24px] border p-8 text-center font-semibold ${danger ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-[#ead9db] bg-white text-[#756361]'}`}>{loading ? <LoaderCircle className="mr-2 h-5 w-5 animate-spin" /> : <MessageSquareText className="mr-2 h-5 w-5" />}{text}</div>;
}
