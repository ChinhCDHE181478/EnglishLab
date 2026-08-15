import { useEffect, useState } from 'react';
import { AlertCircle, CheckCircle2, X } from 'lucide-react';
import { createPortal } from 'react-dom';

const EXIT_DURATION_MS = 260;

export default function ManagementToast({ message, onClose, tone = 'error', title }) {
  const [leaving, setLeaving] = useState(false);

  useEffect(() => {
    setLeaving(false);
  }, [message, tone]);

  if (!message) return null;

  const isError = tone === 'error';
  const Icon = isError ? AlertCircle : CheckCircle2;
  const resolvedTitle = title || (isError ? 'Không thể hoàn tất' : 'Đã hoàn tất');

  const dismiss = () => {
    if (leaving) return;
    setLeaving(true);
    window.setTimeout(onClose, EXIT_DURATION_MS);
  };

  return createPortal(
    <div className="pointer-events-none fixed right-4 top-5 z-[90] w-[calc(100%-2rem)] max-w-[420px] sm:right-6">
      <div
        className={`pointer-events-auto flex items-start gap-3 rounded-2xl border bg-white p-4 shadow-[0_20px_55px_rgba(15,23,42,0.2)] ${
          isError ? 'border-rose-200' : 'border-emerald-200'
        } ${leaving ? 'animate-toast-out' : 'animate-toast-in'}`}
        role={isError ? 'alert' : 'status'}
      >
        <span className={`mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${
          isError ? 'bg-rose-50 text-rose-700' : 'bg-emerald-50 text-emerald-700'
        }`}>
          <Icon className="h-5 w-5" />
        </span>
        <div className="min-w-0 flex-1">
          <p className="text-sm font-extrabold text-[#0b1c30]">{resolvedTitle}</p>
          <p className="mt-1 text-sm leading-6 text-slate-600">{message}</p>
        </div>
        <button
          aria-label="Đóng thông báo"
          className="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
          onClick={dismiss}
          type="button"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>,
    document.body,
  );
}
