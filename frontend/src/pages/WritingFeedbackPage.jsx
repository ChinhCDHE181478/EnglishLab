import { useState } from 'react';
import { AlertCircle, CheckCircle2, PenLine, Sparkles } from 'lucide-react';
import Header from '../components/ai-learning/Header';
import CourseFooter from '../components/course/CourseFooter';
import { courseApi } from '../api/courseApi';

const WritingFeedbackPage = () => {
  const [prompt, setPrompt] = useState('');
  const [essayText, setEssayText] = useState('');
  const [targetBand, setTargetBand] = useState('');
  const [feedback, setFeedback] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    const essay = essayText.trim();
    if (essay.length < 80) {
      setError('Bài viết cần có ít nhất 80 ký tự để AI có đủ dữ liệu phân tích.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const result = await courseApi.getWritingFeedback({
        prompt: prompt.trim() || undefined,
        essayText: essay,
        targetExam: 'IELTS',
        targetBand: targetBand ? Number(targetBand) : undefined,
      });
      setFeedback(result);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Chưa thể nhận phản hồi lúc này. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div id="top" className="min-h-screen bg-[#f9f9f9] font-['Inter'] text-[#1a1c1c]">
      <Header />
      <main className="mx-auto max-w-[1280px] px-4 py-12 md:px-10 md:py-16">
        <div className="mb-10 max-w-3xl">
          <span className="mb-4 inline-flex items-center gap-2 rounded-full bg-[#8a0018]/10 px-4 py-2 text-xs font-extrabold uppercase tracking-wider text-[#8a0018]"><Sparkles size={15} /> AI Writing</span>
          <h1 className="font-['Manrope'] text-4xl font-extrabold text-[#4b0009] md:text-5xl">Phản hồi bài Writing độc lập</h1>
          <p className="mt-4 text-lg leading-8 text-[#6a5553]">Dán bài viết ngoài khóa học để nhận điểm ước tính, điểm mạnh và hướng cải thiện ngay lập tức.</p>
        </div>

        <div className="grid gap-8 lg:grid-cols-[1.05fr_0.95fr]">
          <form className="rounded-3xl border border-[#ead9db] bg-white p-6 shadow-sm md:p-8" onSubmit={handleSubmit}>
            <label className="mb-2 block text-sm font-extrabold text-[#4b0009]" htmlFor="writing-prompt">Đề bài hoặc tiêu đề (không bắt buộc)</label>
            <input id="writing-prompt" className="mb-5 w-full rounded-2xl border border-[#ead9db] px-4 py-3 outline-none focus:border-[#8a0018] focus:ring-2 focus:ring-[#8a0018]/10" maxLength={200} onChange={(event) => setPrompt(event.target.value)} placeholder="Ví dụ: Some people believe that..." value={prompt} />
            <div className="mb-2 flex items-center justify-between gap-3"><label className="text-sm font-extrabold text-[#4b0009]" htmlFor="writing-essay">Bài viết</label><span className="text-xs text-[#806765]">{essayText.length}/3000 ký tự</span></div>
            <textarea id="writing-essay" className="min-h-[360px] w-full resize-y rounded-2xl border border-[#ead9db] p-4 leading-7 outline-none focus:border-[#8a0018] focus:ring-2 focus:ring-[#8a0018]/10" maxLength={3000} onChange={(event) => setEssayText(event.target.value)} placeholder="Nhập từ 80 đến 3000 ký tự..." value={essayText} />
            <div className="mt-5 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
              <label className="text-sm font-extrabold text-[#4b0009]">Band mục tiêu
                <select className="mt-2 block rounded-xl border border-[#ead9db] bg-white px-4 py-2.5 font-medium outline-none" onChange={(event) => setTargetBand(event.target.value)} value={targetBand}><option value="">Không chọn</option>{[5, 5.5, 6, 6.5, 7, 7.5, 8, 8.5, 9].map((band) => <option key={band} value={band}>{band}</option>)}</select>
              </label>
              <button className="rounded-full bg-[#730014] px-7 py-3 font-extrabold text-white transition hover:bg-[#4b0009] disabled:cursor-not-allowed disabled:opacity-60" disabled={loading} type="submit">{loading ? 'Đang phân tích...' : 'Nhận phản hồi AI'}</button>
            </div>
            {error ? <p className="mt-4 flex items-center gap-2 text-sm font-semibold text-red-600"><AlertCircle size={17} />{error}</p> : null}
          </form>

          <section className="rounded-3xl border border-[#ead9db] bg-[#fffdfc] p-6 shadow-sm md:p-8" aria-live="polite">
            <div className="mb-6 flex items-center justify-between border-b border-[#ead9db] pb-5"><h2 className="flex items-center gap-2 font-['Manrope'] text-2xl font-extrabold text-[#4b0009]"><PenLine size={23} /> Kết quả phân tích</h2>{feedback?.estimatedScore != null ? <span className="rounded-full bg-[#8a0018] px-4 py-2 font-extrabold text-white">Band {feedback.estimatedScore}</span> : null}</div>
            {loading ? <p className="py-20 text-center font-semibold text-[#730014]">AI đang đọc bài viết của bạn...</p> : null}
            {!loading && !feedback ? <div className="py-16 text-center text-[#806765]"><Sparkles className="mx-auto mb-4 text-[#c45a64]" size={36} /><p>Phản hồi sẽ xuất hiện tại đây sau khi bạn gửi bài.</p></div> : null}
            {!loading && feedback ? <div className="space-y-5">
              <div className="rounded-2xl border border-[#ead9db] bg-white p-5"><h3 className="mb-2 font-extrabold text-[#4b0009]">Nhận xét tổng quan</h3><p className="leading-7 text-[#6a5553]">{feedback.overallFeedback}</p></div>
              {(feedback.strengths || []).length > 0 ? <div><h3 className="mb-3 font-extrabold text-[#4b0009]">Điểm mạnh</h3><div className="space-y-2">{feedback.strengths.map((item) => <p className="flex gap-2 rounded-xl bg-green-50 p-3 text-sm text-green-900" key={item}><CheckCircle2 className="mt-0.5 shrink-0" size={17} />{item}</p>)}</div></div> : null}
              {(feedback.improvements || []).length > 0 ? <div><h3 className="mb-3 font-extrabold text-[#4b0009]">Cần cải thiện</h3><div className="space-y-2">{feedback.improvements.map((item) => <p className="flex gap-2 rounded-xl bg-amber-50 p-3 text-sm text-amber-900" key={item}><AlertCircle className="mt-0.5 shrink-0" size={17} />{item}</p>)}</div></div> : null}
              {(feedback.criteria || []).length > 0 ? <div><h3 className="mb-3 font-extrabold text-[#4b0009]">Theo tiêu chí</h3><div className="grid gap-3 sm:grid-cols-2">{feedback.criteria.map((criterion, index) => <div className="rounded-2xl border border-[#ead9db] bg-white p-4" key={`${criterion.name}-${index}`}><div className="flex justify-between gap-2 font-bold text-[#4b0009]"><span>{criterion.name}</span>{criterion.score != null ? <span>{criterion.score}</span> : null}</div><p className="mt-2 text-sm leading-6 text-[#6a5553]">{criterion.feedback}</p></div>)}</div></div> : null}
              {feedback.fallback ? <p className="text-xs leading-5 text-[#806765]">Đây là phản hồi dự phòng của EnglishLab khi dịch vụ AI tạm thời chưa khả dụng.</p> : null}
            </div> : null}
          </section>
        </div>
      </main>
      <CourseFooter />
    </div>
  );
};

export default WritingFeedbackPage;
