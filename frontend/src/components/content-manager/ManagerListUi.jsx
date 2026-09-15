import {
  BookOpen,
  ChevronLeft,
  ChevronRight,
  Headphones,
  Languages,
  Layers3,
  Mic2,
  NotebookPen,
} from 'lucide-react';

const TAXONOMY_META = {
  skill: {
    LISTENING: { label: 'Nghe', icon: Headphones, className: 'border-sky-200 bg-sky-50 text-sky-800', iconClassName: 'bg-sky-100 text-sky-700' },
    READING: { label: 'Đọc', icon: BookOpen, className: 'border-emerald-200 bg-emerald-50 text-emerald-800', iconClassName: 'bg-emerald-100 text-emerald-700' },
    WRITING: { label: 'Viết', icon: NotebookPen, className: 'border-amber-200 bg-amber-50 text-amber-900', iconClassName: 'bg-amber-100 text-amber-700' },
    SPEAKING: { label: 'Nói', icon: Mic2, className: 'border-rose-200 bg-rose-50 text-rose-800', iconClassName: 'bg-rose-100 text-rose-700' },
    VOCABULARY: { label: 'Từ vựng', icon: Languages, className: 'border-cyan-200 bg-cyan-50 text-cyan-900', iconClassName: 'bg-cyan-100 text-cyan-700' },
    GRAMMAR: { label: 'Ngữ pháp', icon: Languages, className: 'border-orange-200 bg-orange-50 text-orange-900', iconClassName: 'bg-orange-100 text-orange-700' },
    MIXED: { label: 'Tổng hợp', icon: Layers3, className: 'border-[#dfbfbd] bg-[#fff1f2] text-[#730014]', iconClassName: 'bg-[#f8dfe2] text-[#730014]' },
  },
};

const DEFAULT_TAXONOMY_META = {
  skill: { label: 'Chưa xác định', icon: Layers3, className: 'border-slate-200 bg-slate-50 text-slate-700', iconClassName: 'bg-slate-100 text-slate-600' },
};

const splitExamLevel = (value) => {
  const text = String(value || '').trim();
  const match = text.match(/^(IELTS|TOEIC)\b/i);
  if (!match) return { exam: '', detail: text };

  const exam = match[1].toUpperCase();
  let detail = text.slice(match[0].length).replace(/^\s*[·:–—-]\s*/, '').trim();
  if (exam === 'IELTS') {
    detail = detail.replace(/^band\s*/i, '').trim();
    return { exam, detail: detail ? `Band ${detail}` : '' };
  }

  detail = detail.replace(/^điểm\s*/i, '').replace(/\s*điểm$/i, '').trim();
  return { exam, detail: detail ? `${detail} điểm` : '' };
};

export function ManagerStatsGrid({ stats }) {
  if (!stats?.length) return null;

  return (
    <div className="grid gap-6 md:grid-cols-4">
      {stats.map((item) => {
        const Icon = item.icon;
        return (
          <section className="rounded-xl border border-[#dcc0bf]/30 bg-white p-4 shadow-[0_4px_12px_rgba(75,0,9,0.05)]" key={item.label}>
            <div className="mb-1 flex items-center justify-between gap-3">
              <span className={`text-xs font-bold uppercase tracking-[0.14em] ${item.tone || 'text-[#4b0009]'}`}>{item.label}</span>
              {Icon ? <Icon className={`h-5 w-5 ${item.tone || 'text-[#4b0009]'}`} /> : null}
            </div>
            <p className="font-['Manrope'] text-3xl font-extrabold text-[#0b1c30]">{item.value}</p>
          </section>
        );
      })}
    </div>
  );
}

export function ManagerFilterBar({ children }) {
  return (
    <section className="rounded-xl border border-[#dcc0bf]/30 bg-white p-4 shadow-sm">
      <div className="flex flex-wrap items-center gap-4">
        {children}
      </div>
    </section>
  );
}

export function ManagerTable({ columns, children, minWidth = '1040px' }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-left" style={{ minWidth }}>
        <thead>
          <tr className="border-b border-[#dcc0bf]/30 bg-[#fbf3f4]">
            {columns.map((column) => (
              <th
                className={`px-6 py-4 text-[11px] font-extrabold uppercase tracking-wider text-[#8e7371] ${column.align === 'center' ? 'text-center' : ''} ${column.align === 'right' ? 'text-right' : ''}`}
                key={column.key || column.label}
              >
                {column.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-[#dcc0bf]/15">
          {children}
        </tbody>
      </table>
    </div>
  );
}

export function ManagerTablePagination({ page, pageSize, totalItems, totalPages, onChange, itemLabel = 'mục' }) {
  const from = totalItems ? (page - 1) * pageSize + 1 : 0;
  const to = Math.min(page * pageSize, totalItems);
  const pages = buildPageItems(page, totalPages);

  return (
    <div className="flex flex-col gap-3 border-t border-[#dcc0bf]/20 bg-[#eff4ff]/30 px-6 py-4 sm:flex-row sm:items-center sm:justify-between">
      <p className="text-sm text-[#564241]">
        Hiển thị <span className="font-bold text-[#0b1c30]">{from} - {to}</span> của <span className="font-bold text-[#0b1c30]">{totalItems}</span> {itemLabel}
      </p>
      <div className="flex items-center gap-2">
        <button
          className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-[#dcc0bf]/40 text-[#564241] transition hover:bg-[#eff4ff] disabled:opacity-30"
          disabled={page <= 1}
          onClick={() => onChange(page - 1)}
          type="button"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        {pages.map((item, index) => (
          item === 'dots' ? (
            <span className="px-1 text-sm text-[#564241]" key={`${item}-${index}`}>...</span>
          ) : (
            <button
              className={`inline-flex h-8 w-8 items-center justify-center rounded-lg text-sm font-bold transition ${item === page ? 'bg-[#4b0009] text-white' : 'text-[#0b1c30] hover:bg-[#eff4ff]'}`}
              key={item}
              onClick={() => onChange(item)}
              type="button"
            >
              {item}
            </button>
          )
        ))}
        <button
          className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-[#dcc0bf]/40 text-[#564241] transition hover:bg-[#eff4ff] disabled:opacity-30"
          disabled={page >= totalPages}
          onClick={() => onChange(page + 1)}
          type="button"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

export function ManagerStatusBadge({ children, tone = 'neutral' }) {
  const toneClass = {
    success: 'border-emerald-500/20 bg-emerald-100 text-emerald-700',
    warning: 'border-amber-500/20 bg-amber-100 text-amber-700',
    danger: 'border-rose-500/20 bg-rose-100 text-rose-700',
    neutral: 'border-slate-500/20 bg-slate-100 text-slate-700',
    info: 'border-[#dcc0bf]/40 bg-[#dce9ff] text-[#564241]',
  }[tone] || 'border-slate-500/20 bg-slate-100 text-slate-700';

  return (
    <span className={`inline-flex whitespace-nowrap rounded-lg border px-2.5 py-1 text-[11px] font-bold ${toneClass}`}>
      {children}
    </span>
  );
}

export function ManagerTaxonomyBadge({ kind = 'skill', value }) {
  const normalizedValue = String(value || '').trim();
  if (kind === 'exam' || kind === 'level') {
    const { exam, detail } = splitExamLevel(normalizedValue);
    if (!exam) {
      const unavailable = !detail || detail === '-' || /^Chưa\b/i.test(detail);
      return <span className={`text-sm font-extrabold ${unavailable ? 'text-[#735b59]' : 'text-[#6d28d9]'}`}>{detail || 'Chưa xác định'}</span>;
    }
    const examClassName = exam === 'TOEIC' ? 'text-[#1d4ed8]' : 'text-[#8a0018]';
    return (
      <span className="inline-flex max-w-full items-baseline gap-1.5 whitespace-nowrap text-sm" title={[exam, detail].filter(Boolean).join(' · ')}>
        <span className={`font-extrabold ${examClassName}`}>{exam}</span>
        {detail ? <span aria-hidden="true" className="font-bold text-[#a38986]">·</span> : null}
        {detail ? <span className="sr-only">, </span> : null}
        {detail ? <span className="min-w-0 truncate font-bold text-[#0b1c30]">{detail}</span> : null}
      </span>
    );
  }

  const meta = TAXONOMY_META[kind]?.[normalizedValue.toUpperCase()] || DEFAULT_TAXONOMY_META[kind] || DEFAULT_TAXONOMY_META.skill;
  const Icon = meta.icon;
  const label = normalizedValue ? (TAXONOMY_META[kind]?.[normalizedValue.toUpperCase()]?.label || normalizedValue) : meta.label;

  return (
    <span className={`inline-flex min-h-8 max-w-full items-center gap-2 whitespace-nowrap rounded-full border py-1 pl-1 pr-3 text-xs font-extrabold shadow-[0_1px_2px_rgba(11,28,48,0.05)] ${meta.className}`} title={label}>
      <span className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full ${meta.iconClassName}`}>
        <Icon aria-hidden="true" className="h-3.5 w-3.5" />
      </span>
      <span className="truncate">{label}</span>
    </span>
  );
}

export function ManagerEmptyState({ children }) {
  return (
    <div className="rounded-xl border border-dashed border-[#dcc0bf]/50 bg-white px-6 py-12 text-center text-sm font-semibold text-[#564241]">
      {children}
    </div>
  );
}

function buildPageItems(currentPage, totalPages) {
  if (totalPages <= 5) return Array.from({ length: totalPages }, (_, index) => index + 1);
  const items = [1];
  if (currentPage > 3) items.push('dots');
  const start = Math.max(2, currentPage - 1);
  const end = Math.min(totalPages - 1, currentPage + 1);
  for (let item = start; item <= end; item += 1) items.push(item);
  if (currentPage < totalPages - 2) items.push('dots');
  items.push(totalPages);
  return items;
}
