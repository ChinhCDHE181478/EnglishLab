import { AlertTriangle, Info, Link2, X } from 'lucide-react';
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { createPortal } from 'react-dom';

const AppDialogContext = createContext(null);

const DIALOG_DEFAULTS = {
  alert: {
    title: 'Thông báo',
    confirmLabel: 'Đã hiểu',
  },
  confirm: {
    title: 'Xác nhận thao tác',
    confirmLabel: 'Xác nhận',
    cancelLabel: 'Hủy',
  },
  prompt: {
    title: 'Nhập thông tin',
    confirmLabel: 'Tiếp tục',
    cancelLabel: 'Hủy',
  },
};

export function AppDialogProvider({ children }) {
  const [dialog, setDialog] = useState(null);
  const [inputValue, setInputValue] = useState('');
  const queueRef = useRef([]);
  const activeRef = useRef(null);
  const inputRef = useRef(null);
  const confirmButtonRef = useRef(null);

  const activateNext = useCallback(() => {
    if (activeRef.current || queueRef.current.length === 0) return;
    const next = queueRef.current.shift();
    activeRef.current = next;
    setDialog(next.config);
    setInputValue(next.config.defaultValue ?? '');
  }, []);

  const openDialog = useCallback((config) => new Promise((resolve) => {
    queueRef.current.push({ config, resolve });
    activateNext();
  }), [activateNext]);

  const closeDialog = useCallback((value) => {
    const active = activeRef.current;
    if (!active) return;
    activeRef.current = null;
    setDialog(null);
    active.resolve(value);
    window.setTimeout(activateNext, 0);
  }, [activateNext]);

  useEffect(() => {
    if (!dialog) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const focusTimer = window.setTimeout(() => {
      if (dialog.type === 'prompt') inputRef.current?.focus();
      else confirmButtonRef.current?.focus();
    }, 0);
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        closeDialog(dialog.type === 'prompt' ? null : false);
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      window.clearTimeout(focusTimer);
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [closeDialog, dialog]);

  useEffect(() => () => {
    activeRef.current?.resolve(false);
    queueRef.current.forEach((item) => item.resolve(false));
    activeRef.current = null;
    queueRef.current = [];
  }, []);

  const api = useMemo(() => ({
    alert: (message, options = {}) => openDialog({
      ...DIALOG_DEFAULTS.alert,
      ...options,
      message,
      type: 'alert',
    }).then(() => undefined),
    confirm: (message, options = {}) => openDialog({
      ...DIALOG_DEFAULTS.confirm,
      ...options,
      message,
      type: 'confirm',
    }),
    prompt: (message, defaultValue = '', options = {}) => openDialog({
      ...DIALOG_DEFAULTS.prompt,
      ...options,
      defaultValue,
      message,
      type: 'prompt',
    }),
  }), [openDialog]);

  const submitDialog = (event) => {
    event.preventDefault();
    closeDialog(dialog.type === 'prompt' ? inputValue : true);
  };

  return (
    <AppDialogContext.Provider value={api}>
      {children}
      {dialog && createPortal(
        <div
          aria-labelledby="app-dialog-title"
          aria-modal="true"
          className="fixed inset-0 z-[1000] flex items-center justify-center bg-[#210006]/55 p-4 backdrop-blur-[2px]"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeDialog(dialog.type === 'prompt' ? null : false);
            }
          }}
          role="dialog"
        >
          <form
            className="w-full max-w-lg max-h-[90vh] flex flex-col overflow-hidden rounded-3xl border border-[#ead7d9] bg-white shadow-[0_28px_90px_rgba(56,0,10,0.3)]"
            onSubmit={submitDialog}
          >
            <div className="flex items-start gap-4 border-b border-[#f0e1e3] px-6 py-5">
              <span className={`mt-0.5 flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl ${dialog.tone === 'danger' ? 'bg-rose-100 text-rose-700' : 'bg-[#fff0f2] text-[#8a0018]'}`}>
                {dialog.type === 'prompt'
                  ? <Link2 className="h-5 w-5" />
                  : dialog.tone === 'danger'
                    ? <AlertTriangle className="h-5 w-5" />
                    : <Info className="h-5 w-5" />}
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#a04b59]">EnglishLab</p>
                <h2 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#3f0711]" id="app-dialog-title">
                  {dialog.title}
                </h2>
              </div>
              <button
                aria-label="Đóng hộp thoại"
                className="rounded-xl p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                onClick={() => closeDialog(dialog.type === 'prompt' ? null : false)}
                type="button"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto space-y-4 px-6 py-5">
              <p className="whitespace-pre-line text-[15px] leading-6 text-slate-700">{dialog.message}</p>
              {dialog.type === 'prompt' ? (
                <label className="block">
                  <span className="mb-2 block text-sm font-bold text-[#4b1720]">{dialog.inputLabel || 'Nội dung'}</span>
                  <input
                    className="w-full rounded-xl border border-[#dec7ca] bg-[#fffdfd] px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-[#8a0018] focus:ring-4 focus:ring-[#8a0018]/10"
                    onChange={(event) => setInputValue(event.target.value)}
                    placeholder={dialog.placeholder}
                    ref={inputRef}
                    required={dialog.required}
                    type={dialog.inputType || 'text'}
                    value={inputValue}
                  />
                </label>
              ) : null}
            </div>

            <div className="flex flex-col-reverse gap-3 border-t border-[#f0e1e3] bg-[#fffafa] px-6 py-4 sm:flex-row sm:justify-end">
              {dialog.type !== 'alert' ? (
                <button
                  className="rounded-xl border border-[#dec7ca] bg-white px-5 py-2.5 text-sm font-bold text-[#6d2632] transition hover:bg-[#fff3f4]"
                  onClick={() => closeDialog(dialog.type === 'prompt' ? null : false)}
                  type="button"
                >
                  {dialog.cancelLabel}
                </button>
              ) : null}
              <button
                className={`rounded-xl px-5 py-2.5 text-sm font-bold text-white shadow-sm transition ${dialog.tone === 'danger' ? 'bg-rose-700 hover:bg-rose-800' : 'bg-[#760016] hover:bg-[#570010]'}`}
                ref={confirmButtonRef}
                type="submit"
              >
                {dialog.confirmLabel}
              </button>
            </div>
          </form>
        </div>,
        document.body,
      )}
    </AppDialogContext.Provider>
  );
}

export function useAppDialog() {
  const context = useContext(AppDialogContext);
  if (!context) {
    throw new Error('useAppDialog phải được sử dụng bên trong AppDialogProvider.');
  }
  return context;
}
