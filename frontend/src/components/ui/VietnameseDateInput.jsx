import { useEffect, useRef, useState } from 'react';
import { CalendarDays } from 'lucide-react';
import { formatIsoDateForDisplay, maskVietnameseDate, parseVietnameseDate } from '../../utils/vietnameseDate';

export default function VietnameseDateInput({
  className = '',
  id,
  min,
  onChange,
  required = false,
  value,
}) {
  const pickerRef = useRef(null);
  const [displayValue, setDisplayValue] = useState(() => formatIsoDateForDisplay(value));
  const [invalid, setInvalid] = useState(false);

  useEffect(() => {
    setDisplayValue(formatIsoDateForDisplay(value));
    setInvalid(false);
  }, [value]);

  const update = (event) => {
    const nextDisplay = maskVietnameseDate(event.target.value);
    setDisplayValue(nextDisplay);
    setInvalid(false);
    if (!nextDisplay) onChange('');
    const parsed = parseVietnameseDate(nextDisplay);
    if (parsed) onChange(parsed);
  };

  const validate = () => {
    if (!displayValue && !required) {
      setInvalid(false);
      return;
    }
    const parsed = parseVietnameseDate(displayValue);
    const belowMinimum = parsed && min && parsed < min;
    setInvalid(!parsed || Boolean(belowMinimum));
  };

  const selectDate = (event) => {
    const nextValue = event.target.value;
    setDisplayValue(formatIsoDateForDisplay(nextValue));
    setInvalid(false);
    onChange(nextValue);
  };

  const openPicker = () => {
    const picker = pickerRef.current;
    if (!picker) return;
    try {
      if (typeof picker.showPicker === 'function') {
        picker.showPicker();
        return;
      }
    } catch {
      // Fallback for browsers that expose showPicker but reject it.
    }
    picker.focus();
    picker.click();
  };

  return (
    <div>
      <div className="relative">
        <input
          aria-invalid={invalid}
          className={`${className} pr-11 ${invalid ? 'border-rose-400 focus:border-rose-500' : ''}`}
          id={id}
          inputMode="numeric"
          maxLength={10}
          onBlur={validate}
          onChange={update}
          placeholder="dd/mm/yyyy"
          required={required}
          type="text"
          value={displayValue}
        />
        <button
          aria-label="Mở lịch chọn ngày"
          className="absolute inset-y-0 right-0 inline-flex w-11 items-center justify-center rounded-r-xl text-slate-400 transition hover:bg-slate-50 hover:text-[#730014]"
          onClick={openPicker}
          type="button"
        >
          <CalendarDays className="h-4 w-4" />
        </button>
        <input
          aria-hidden="true"
          className="pointer-events-none absolute h-px w-px opacity-0"
          min={min}
          onChange={selectDate}
          ref={pickerRef}
          tabIndex={-1}
          type="date"
          value={parseVietnameseDate(displayValue) || ''}
        />
      </div>
      {invalid ? (
        <p className="mt-1 text-xs font-semibold text-rose-600">
          {min && parseVietnameseDate(displayValue) ? `Ngày phải từ ${formatIsoDateForDisplay(min)} trở đi.` : 'Nhập ngày theo định dạng dd/mm/yyyy.'}
        </p>
      ) : null}
    </div>
  );
}
