import { useMemo, useState } from 'react';
import { ChevronDown } from 'lucide-react';

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
}) {
  const [open, setOpen] = useState(false);
  const normalized = useMemo(() => normalizeOptions(options), [options]);
  const selected = normalized.find((option) => String(option.value) === String(value));
  const label = selected?.buttonLabel || selected?.label || placeholder;

  return (
    <div className="relative">
      <button
        aria-expanded={open}
        className={`flex w-full items-center justify-between gap-3 rounded-[18px] border border-[#dfbfbd]/75 bg-white px-4 py-3 text-left text-sm font-semibold text-[#584140] shadow-[0_10px_24px_rgba(75,0,9,0.06)] transition hover:border-[#cf6f83] focus:border-[#cf6f83] focus:outline-none ${buttonClassName}`}
        disabled={disabled}
        onBlur={() => window.setTimeout(() => setOpen(false), 120)}
        onClick={() => setOpen((current) => !current)}
        type="button"
        title={label}
      >
        <span className="truncate">{label}</span>
        <ChevronDown className={`h-4 w-4 shrink-0 text-[#730014] transition ${open ? 'rotate-180' : ''}`} />
      </button>
      {open ? (
        <div className={`absolute left-0 top-full z-50 mt-2 max-h-72 min-w-full overflow-y-auto rounded-2xl border border-[#dfbfbd]/75 bg-white p-1 shadow-[0_18px_45px_rgba(75,0,9,0.16)] ${menuClassName}`}>
          {normalized.map((option) => (
            <button
              key={option.value}
              className={`block w-full rounded-xl px-4 py-2.5 text-left text-sm font-semibold leading-5 transition ${
                String(option.value) === String(value) ? 'bg-[#4b0009] text-white' : 'text-[#4b0009] hover:bg-[#fff2f3]'
              }`}
              onMouseDown={(event) => event.preventDefault()}
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
          ))}
        </div>
      ) : null}
      <select className="sr-only" disabled={disabled} id={id} name={name} onChange={onChange} value={value}>
        {!selected && placeholder ? <option value="">{placeholder}</option> : null}
        {normalized.map((option) => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
    </div>
  );
}
