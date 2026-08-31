import { useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { ChevronDown, Search } from 'lucide-react';

const normalizeOptions = (options = []) => options.map((option) => {
  if (typeof option === 'string' || typeof option === 'number') {
    return { label: String(option), value: String(option) };
  }
  return {
    label: String(option?.label ?? option?.value ?? ''),
    buttonLabel: option?.buttonLabel ? String(option.buttonLabel) : '',
    value: String(option?.value ?? option?.label ?? ''),
    description: option?.description ? String(option.description) : '',
  };
});

export default function BrandedSelect({
  id,
  name,
  value,
  onChange,
  options = [],
  placeholder = 'Chọn mục',
  disabled = false,
  buttonClassName = '',
  menuClassName = '',
  menuPlacement = 'auto',
  searchable = false,
}) {
  const [open, setOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [menuCoords, setMenuCoords] = useState({ top: 0, left: 0, width: 0, isTop: false });
  const containerRef = useRef(null);
  const menuRef = useRef(null);

  const updatePosition = () => {
    if (!containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();

    setMenuCoords({
      top: rect.bottom + 6,
      left: Math.max(8, Math.min(rect.left, window.innerWidth - rect.width - 8)),
      width: Math.max(rect.width, 260),
    });
  };

  useEffect(() => {
    if (!open) return undefined;
    updatePosition();
    const handleScrollOrResize = () => updatePosition();
    window.addEventListener('resize', handleScrollOrResize);
    window.addEventListener('scroll', handleScrollOrResize, true);
    return () => {
      window.removeEventListener('resize', handleScrollOrResize);
      window.removeEventListener('scroll', handleScrollOrResize, true);
    };
  }, [open, menuPlacement]);

  const normalized = useMemo(() => normalizeOptions(options), [options]);
  const selected = normalized.find((option) => String(option.value) === String(value));
  const label = selected?.buttonLabel || selected?.label || placeholder;

  // Handle click outside to close menu
  useEffect(() => {
    if (!open) return;
    function handleClickOutside(event) {
      if (
        containerRef.current
        && !containerRef.current.contains(event.target)
        && menuRef.current
        && !menuRef.current.contains(event.target)
      ) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [open]);

  // Reset search query when menu closes
  useEffect(() => {
    if (!open) {
      setSearchQuery('');
    }
  }, [open]);

  const filteredOptions = useMemo(() => {
    if (!searchQuery) return normalized;
    const query = searchQuery.toLowerCase().trim();
    return normalized.filter((option) =>
      option.label.toLowerCase().includes(query) ||
      (option.description && option.description.toLowerCase().includes(query))
    );
  }, [normalized, searchQuery]);

  const isSearchEnabled = searchable || normalized.length > 8;

  return (
    <div className="relative" ref={containerRef}>
      <button
        aria-expanded={open}
        className={`flex w-full items-center justify-between gap-3 rounded-[18px] border border-[#dfbfbd]/75 bg-white px-4 py-3 text-left text-sm font-semibold text-[#584140] shadow-[0_10px_24px_rgba(75,0,9,0.06)] transition hover:border-[#cf6f83] focus:border-[#cf6f83] focus:outline-none ${buttonClassName}`}
        disabled={disabled}
        onClick={() => {
          if (!open) updatePosition();
          setOpen((current) => !current);
        }}
        type="button"
        title={label}
      >
        <span className="truncate">{label}</span>
        <ChevronDown className={`h-4 w-4 shrink-0 text-[#730014] transition ${open ? 'rotate-180' : ''}`} />
      </button>
      {open && typeof document !== 'undefined' ? createPortal(
        <div
          ref={menuRef}
          style={{
            position: 'fixed',
            top: `${menuCoords.top}px`,
            left: `${menuCoords.left}px`,
            width: `${menuCoords.width}px`,
            zIndex: 99999,
          }}
          className={`max-h-80 overflow-hidden rounded-2xl border border-[#dfbfbd]/75 bg-white p-1 shadow-[0_18px_45px_rgba(75,0,9,0.22)] flex flex-col ${menuClassName}`}
        >
          {isSearchEnabled ? (
            <div className="sticky top-0 z-10 bg-white px-2 py-1.5 border-b border-[#dfbfbd]/30">
              <div className="relative flex items-center">
                <Search className="absolute left-3 h-3.5 w-3.5 text-slate-400" />
                <input
                  type="text"
                  placeholder="Tìm kiếm..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 pl-8 pr-3 py-1.5 text-xs text-slate-900 outline-none transition focus:border-[#730014] focus:bg-white"
                />
              </div>
            </div>
          ) : null}
          <div className="overflow-y-auto max-h-64 flex-1 mt-1">
            {filteredOptions.length ? (
              filteredOptions.map((option) => (
                <button
                  key={option.value}
                  className={`block w-full rounded-xl px-4 py-2.5 text-left text-sm font-semibold leading-5 transition ${
                    String(option.value) === String(value) ? 'bg-[#4b0009] text-white' : 'text-[#4b0009] hover:bg-[#fff2f3]'
                  }`}
                  onClick={() => {
                    onChange?.({ target: { name, id, value: option.value } });
                    setOpen(false);
                  }}
                  title={option.label}
                  type="button"
                >
                  <span className="block whitespace-normal break-words">{option.label}</span>
                  {option.description ? (
                    <span className={`mt-0.5 block whitespace-normal break-words text-xs font-medium ${
                      String(option.value) === String(value) ? 'text-white/75' : 'text-[#8b706e]'
                    }`}
                    >
                      {option.description}
                    </span>
                  ) : null}
                </button>
              ))
            ) : (
              <div className="px-4 py-3 text-center text-xs text-slate-500">
                Không tìm thấy kết quả
              </div>
            )}
          </div>
        </div>,
        document.body
      ) : null}
    </div>
  );
}
