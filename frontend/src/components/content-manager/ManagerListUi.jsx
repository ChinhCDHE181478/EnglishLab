import { ChevronLeft, ChevronRight } from 'lucide-react';

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
