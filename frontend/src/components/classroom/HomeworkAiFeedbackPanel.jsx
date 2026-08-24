import { CheckCircle2, Lightbulb, ListChecks, Target } from 'lucide-react';

const toArray = (value) => (Array.isArray(value) ? value.filter(Boolean) : []);

const parseFeedback = (value) => {
  if (!value) return null;
  if (typeof value === 'object') return value;
  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
};

export default function HomeworkAiFeedbackPanel({ value }) {
  const feedback = parseFeedback(value);
  if (!feedback) return null;

  const criteria = toArray(feedback.criteria?.length ? feedback.criteria : feedback.criterionFeedback);
  const strengths = toArray(feedback.strengths);
  const improvements = toArray(
    feedback.improvements?.length ? feedback.improvements : feedback.weaknesses,
  );
  const suggestions = toArray(
    feedback.suggestions?.length ? feedback.suggestions : feedback.recommendedReview,
  );
  const correctedExamples = toArray(feedback.correctedExamples);

  return (
    <section className="overflow-hidden rounded-2xl border border-[#dfbfbd]/35 bg-white">
      <div className="border-b border-[#dfbfbd]/25 bg-[#fff8f8] px-5 py-4">
        <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Phân tích hỗ trợ từ AI</p>
        {feedback.summary ? (
          <p className="mt-2 text-sm leading-6 text-[#584140]">{feedback.summary}</p>
        ) : null}
      </div>

      {criteria.length ? (
        <div className="grid gap-3 border-b border-[#dfbfbd]/20 p-4 md:grid-cols-2">
          {criteria.map((criterion, index) => (
            <article className="rounded-xl border border-[#f0e2e2] bg-[#fffdfd] p-4" key={`${criterion.name || 'criterion'}-${index}`}>
              <div className="flex items-start justify-between gap-3">
                <p className="text-sm font-extrabold text-[#4b0009]">{criterion.name || `Tiêu chí ${index + 1}`}</p>
                {criterion.score != null ? (
                  <span className="shrink-0 font-['Manrope'] text-lg font-extrabold text-[#8a0018]">{criterion.score}</span>
                ) : null}
              </div>
              {criterion.feedback ? <p className="mt-2 text-sm leading-6 text-[#584140]">{criterion.feedback}</p> : null}
            </article>
          ))}
        </div>
      ) : null}

      <div className="grid gap-3 p-4 md:grid-cols-3">
        <InsightCard icon={CheckCircle2} items={strengths} title="Điểm mạnh" tone="emerald" />
        <InsightCard icon={Target} items={improvements} title="Cần cải thiện" tone="rose" />
        <InsightCard icon={Lightbulb} items={suggestions} title="Hướng luyện tập" tone="amber" />
      </div>

      {correctedExamples.length ? (
        <div className="space-y-3 border-t border-[#dfbfbd]/20 p-4">
          <p className="text-xs font-extrabold uppercase tracking-wider text-[#730014]">Ví dụ sửa lỗi</p>
          {correctedExamples.map((example, index) => (
            <article className="rounded-xl border border-[#f0e2e2] bg-[#fffdfd] p-4" key={`example-${index}`}>
              <p className="text-sm leading-6 text-[#584140]">
                <span className="rounded bg-rose-50 px-2 py-1 text-rose-700 line-through">{example.original}</span>
                <span className="mx-2 font-bold text-[#8b706e]">→</span>
                <span className="rounded bg-emerald-50 px-2 py-1 font-semibold text-emerald-700">{example.corrected}</span>
              </p>
              {example.explanation ? <p className="mt-2 text-xs leading-5 text-[#8b706e]">{example.explanation}</p> : null}
            </article>
          ))}
        </div>
      ) : null}
    </section>
  );
}

function InsightCard({ icon: Icon, items, title, tone }) {
  if (!items.length) return null;
  const tones = {
    emerald: 'border-emerald-100 bg-emerald-50/60 text-emerald-800',
    rose: 'border-rose-100 bg-rose-50/60 text-rose-800',
    amber: 'border-amber-100 bg-amber-50/60 text-amber-800',
  };
  return (
    <article className={`rounded-xl border p-4 ${tones[tone]}`}>
      <p className="flex items-center gap-2 text-xs font-extrabold uppercase tracking-wider">
        <Icon className="h-4 w-4" /> {title}
      </p>
      <ul className="mt-3 space-y-2 text-sm leading-5 text-[#584140]">
        {items.map((item, index) => (
          <li className="flex gap-2" key={`${title}-${index}`}>
            <ListChecks className="mt-0.5 h-3.5 w-3.5 shrink-0" />
            <span>{typeof item === 'string' ? item : item.feedback || item.text || String(item)}</span>
          </li>
        ))}
      </ul>
    </article>
  );
}
