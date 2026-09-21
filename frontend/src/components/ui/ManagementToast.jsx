import { useEffect, useEffectEvent, useState } from 'react';
import { AlertCircle, CheckCircle2, X } from 'lucide-react';
import { createPortal } from 'react-dom';

const EXIT_DURATION_MS = 260;
const SUCCESS_DURATION_MS = 4500;
const ERROR_DURATION_MS = 7000;

export default function ManagementToast({ actionLabel, code, duration, message, onAction, onClose, tone = 'error', title }) {
  const [leaving, setLeaving] = useState(false);
  const messageText = typeof message === 'object' ? message?.message : message;
  const closeToast = useEffectEvent(() => onClose?.());
  const messageCode = code
    || (typeof message === 'object' ? message?.code : null)
    || (tone === 'error' ? 'CONTENT_MANAGER_ERROR' : null);

  useEffect(() => {
    setLeaving(false);
    if (!messageText || !onClose) return undefined;

    let closeTimer;
    const dismissTimer = window.setTimeout(() => {
      setLeaving(true);
      closeTimer = window.setTimeout(closeToast, EXIT_DURATION_MS);
    }, duration ?? (tone === 'error' ? ERROR_DURATION_MS : SUCCESS_DURATION_MS));

    return () => {
      window.clearTimeout(dismissTimer);
      if (closeTimer) window.clearTimeout(closeTimer);
    };
  }, [duration, messageText, tone]);

  if (!messageText) return null;

  const isError = tone === 'error';
  const Icon = isError ? AlertCircle : CheckCircle2;
  const resolvedTitle = title || (isError ? 'Không thể hoàn tất' : 'Đã hoàn tất');

  const dismiss = () => {
    if (leaving) return;
    setLeaving(true);
    window.setTimeout(closeToast, EXIT_DURATION_MS);
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
          <p className="mt-1 text-sm leading-6 text-slate-600">{messageText}</p>
          {isError && messageCode ? (
            <p className="mt-2 font-mono text-[11px] font-bold uppercase tracking-[0.08em] text-rose-700">
              Mã lỗi: {messageCode}
            </p>
          ) : null}
          {actionLabel && onAction ? (
            <button
              className={`mt-3 rounded-lg px-3 py-2 text-xs font-extrabold transition ${
                isError
                  ? 'bg-rose-50 text-rose-800 hover:bg-rose-100'
                  : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100'
              }`}
              onClick={onAction}
              type="button"
            >
              {actionLabel}
            </button>
          ) : null}
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
