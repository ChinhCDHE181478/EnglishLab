import { useEffect, useMemo, useState } from 'react';

// Client-side pagination helper shared across admin list screens so long lists
// always have page controls instead of dumping every row at once.
export function usePagination(items, pageSize = 10, resetKey) {
  const list = Array.isArray(items) ? items : [];
  const [page, setPage] = useState(1);
  const totalPages = Math.max(1, Math.ceil(list.length / pageSize));

  // Jump back to the first page whenever the active filter/search changes.
  useEffect(() => {
    setPage(1);
  }, [resetKey]);

  // Keep the current page in range when items are removed/reloaded.
  useEffect(() => {
    if (page > totalPages) setPage(totalPages);
  }, [page, totalPages]);

  const pageItems = useMemo(
    () => list.slice((page - 1) * pageSize, page * pageSize),
    [list, page, pageSize],
  );

  return { page, setPage, totalPages, pageItems, totalItems: list.length };
}

export default function Pagination({ page, totalPages, onChange, totalItems, pageSize = 10, className = '', alwaysVisible = false }) {
  if (totalPages <= 1 && !alwaysVisible) return null;

  return (
    <div className={`flex flex-wrap items-center justify-center gap-3 ${className}`}>
      <button
        type="button"
        disabled={page <= 1}
        onClick={() => onChange(page - 1)}
        className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-[#730014] transition hover:bg-[#fff4f5] disabled:cursor-not-allowed disabled:opacity-40"
      >
        Trang trước
      </button>
      <span className="text-sm font-semibold text-slate-600">
        Trang {page} / {totalPages}
      </span>
      <button
        type="button"
        disabled={page >= totalPages}
        onClick={() => onChange(page + 1)}
        className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-[#730014] transition hover:bg-[#fff4f5] disabled:cursor-not-allowed disabled:opacity-40"
      >
        Trang sau
      </button>
    </div>
  );
}
