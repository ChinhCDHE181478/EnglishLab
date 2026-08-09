import { useEffect, useMemo, useRef, useState } from 'react';
import { RefreshCw, TriangleAlert } from 'lucide-react';
import dictionaryApi from '../../api/dictionaryApi';

const lookupCache = new Map();
const ENGLISH_TERM_PATTERN = /^[A-Za-z][A-Za-z' -]{0,118}[A-Za-z]$|^[A-Za-z]$/;

const DEFAULT_INPUT_CLASS = 'w-full rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]';
const DEFAULT_TEXTAREA_CLASS = 'min-h-[82px] w-full rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]';
const LABEL_CLASS = 'mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500';

const getErrorMessage = (error) => error?.response?.data?.message
  || error?.response?.data?.error
  || error?.message
  || 'Không thể tải gợi ý từ điển lúc này.';

const unique = (values) => [...new Set(values.map((value) => String(value || '').trim()).filter(Boolean))];

export function buildMeaningOptions(entry) {
  const vietnamese = String(entry?.meaningVietnamese || '').trim();
  if (!vietnamese) return [];
  const alternatives = vietnamese
    .split(/[;,/\n]+/)
    .map((value) => value.trim())
    .filter((value) => value.length > 1);
  return unique([vietnamese, ...alternatives]).map((value) => ({
    label: value,
    value,
    description: entry?.phonetic ? `${entry.word} ${entry.phonetic}` : entry?.word,
  }));
}

export function buildExampleOptions(entry) {
  return unique((entry?.meanings || []).flatMap((meaning) => (
    (meaning.definitions || []).map((definition) => definition.example)
  ))).map((value) => ({ label: value, value }));
}

function SuggestionMenu({ loading, error, options, emptyMessage, onRetry, onSelect }) {
  return (
    <div className="absolute left-0 right-0 top-full z-50 mt-1 max-h-64 overflow-y-auto rounded-xl border border-[#dfbfbd] bg-white p-1.5 shadow-[0_16px_36px_rgba(75,0,9,0.16)]">
      {loading ? (
        <p className="flex items-center gap-2 px-3 py-2.5 text-xs font-semibold text-[#584140]">
          <RefreshCw className="h-3.5 w-3.5 animate-spin text-[#730014]" /> Đang tra cứu...
        </p>
      ) : null}
      {!loading && error ? (
        <div className="flex items-start justify-between gap-3 px-3 py-2.5 text-xs text-rose-700">
          <span className="flex min-w-0 items-start gap-2"><TriangleAlert className="mt-0.5 h-3.5 w-3.5 shrink-0" /> {error}</span>
          <button className="shrink-0 font-extrabold underline" onMouseDown={(event) => event.preventDefault()} onClick={onRetry} type="button">Thử lại</button>
        </div>
      ) : null}
      {!loading && !error && options.map((option) => (
        <button
          className="block w-full rounded-lg px-3 py-2.5 text-left text-sm text-[#2b2828] transition hover:bg-[#fff1f2]"
          key={option.value}
          onMouseDown={(event) => event.preventDefault()}
          onClick={() => onSelect(option.value)}
          type="button"
        >
          <span className="block font-semibold">{option.label}</span>
          {option.description ? <span className="mt-0.5 block text-xs text-[#8b706e]">{option.description}</span> : null}
        </button>
      ))}
      {!loading && !error && !options.length ? <p className="px-3 py-2.5 text-xs text-[#8b706e]">{emptyMessage}</p> : null}
    </div>
  );
}

export default function FlashcardDictionaryAssistant({
  term = '',
  meaning = '',
  example = '',
  onMeaningChange,
  onExampleChange,
  meaningInputClassName = DEFAULT_INPUT_CLASS,
  exampleInputClassName = DEFAULT_TEXTAREA_CLASS,
  showLabels = false,
}) {
  const normalizedTerm = term.trim().replace(/\s+/g, ' ').toLowerCase();
  const meaningRef = useRef(null);
  const exampleRef = useRef(null);
  const [activeField, setActiveField] = useState(null);
  const [lookupTerm, setLookupTerm] = useState('');
  const [entry, setEntry] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    const closeOnOutsideClick = (event) => {
      if (!meaningRef.current?.contains(event.target) && !exampleRef.current?.contains(event.target)) {
        setActiveField(null);
      }
    };
    document.addEventListener('mousedown', closeOnOutsideClick);
    return () => document.removeEventListener('mousedown', closeOnOutsideClick);
  }, []);

  useEffect(() => {
    if (lookupTerm && lookupTerm !== normalizedTerm) {
      setLookupTerm('');
      setEntry(null);
      setError('');
      setLoading(false);
    }
  }, [lookupTerm, normalizedTerm]);

  useEffect(() => {
    if (!lookupTerm) return undefined;
    const cached = lookupCache.get(lookupTerm);
    if (cached) {
      setEntry(cached);
      setError('');
      setLoading(false);
      return undefined;
    }

    const controller = new AbortController();
    const timer = window.setTimeout(async () => {
      setLoading(true);
      setError('');
      try {
        const result = await dictionaryApi.lookup(lookupTerm, { signal: controller.signal });
        if (controller.signal.aborted) return;
        lookupCache.set(lookupTerm, result);
        setEntry(result);
      } catch (lookupError) {
        if (controller.signal.aborted) return;
        setEntry(null);
        setError(getErrorMessage(lookupError));
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }, 350);
    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [lookupTerm, retryKey]);

  const meaningOptions = useMemo(() => buildMeaningOptions(entry), [entry]);
  const exampleOptions = useMemo(() => buildExampleOptions(entry), [entry]);

  const openSuggestions = (field) => {
    setActiveField(field);
    if (!normalizedTerm || !ENGLISH_TERM_PATTERN.test(normalizedTerm)) {
      setLookupTerm('');
      setEntry(null);
      setLoading(false);
      setError(normalizedTerm
        ? 'Thuật ngữ chỉ được chứa chữ cái tiếng Anh, dấu cách, dấu nháy đơn hoặc dấu gạch nối.'
        : 'Hãy nhập thuật ngữ tiếng Anh trước.');
      return;
    }
    setError('');
    setLookupTerm(normalizedTerm);
  };

  const retry = () => {
    if (!normalizedTerm || !ENGLISH_TERM_PATTERN.test(normalizedTerm)) return;
    lookupCache.delete(normalizedTerm);
    setLookupTerm(normalizedTerm);
    setRetryKey((value) => value + 1);
  };

  return (
    <>
      <label className="block" ref={meaningRef}>
        {showLabels ? <span className={LABEL_CLASS}>Định nghĩa</span> : null}
        <div className="relative">
          <input
            className={meaningInputClassName}
            onChange={(event) => onMeaningChange(event.target.value)}
            onFocus={() => openSuggestions('MEANING')}
            placeholder="Định nghĩa / nghĩa tiếng Việt"
            value={meaning}
          />
          {activeField === 'MEANING' ? (
            <SuggestionMenu
              emptyMessage={entry?.vietnameseMeaningAvailable === false ? 'Dịch vụ chưa trả về nghĩa tiếng Việt. Bạn vẫn có thể nhập thủ công.' : 'Không có nghĩa phù hợp để lựa chọn.'}
              error={error}
              loading={loading}
              onRetry={retry}
              onSelect={(value) => {
                onMeaningChange(value);
                setActiveField(null);
              }}
              options={meaningOptions}
            />
          ) : null}
        </div>
      </label>

      <label className="block" ref={exampleRef}>
        {showLabels ? <span className={LABEL_CLASS}>Ví dụ</span> : null}
        <div className="relative">
          <textarea
            className={exampleInputClassName}
            onChange={(event) => onExampleChange(event.target.value)}
            onFocus={() => openSuggestions('EXAMPLE')}
            placeholder="Ví dụ sử dụng"
            rows={3}
            value={example}
          />
          {activeField === 'EXAMPLE' ? (
            <SuggestionMenu
              emptyMessage="Từ điển chưa có câu ví dụ cho thuật ngữ này. Bạn vẫn có thể nhập thủ công."
              error={error}
              loading={loading}
              onRetry={retry}
              onSelect={(value) => {
                onExampleChange(value);
                setActiveField(null);
              }}
              options={exampleOptions}
            />
          ) : null}
        </div>
      </label>
    </>
  );
}
