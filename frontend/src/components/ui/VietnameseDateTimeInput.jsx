import { useEffect, useState } from 'react';
import VietnameseDateInput from './VietnameseDateInput';

function splitDateTime(value) {
  if (!value) return { date: '', time: '' };
  const [date = '', rawTime = ''] = String(value).split('T');
  return { date, time: rawTime.slice(0, 5) };
}

export default function VietnameseDateTimeInput({
  className = '',
  id,
  min,
  onChange,
  required = false,
  value,
}) {
  const initial = splitDateTime(value);
  const [date, setDate] = useState(initial.date);
  const [time, setTime] = useState(initial.time);

  useEffect(() => {
    const next = splitDateTime(value);
    setDate(next.date);
    setTime(next.time);
  }, [value]);

  const updateDate = (nextDate) => {
    setDate(nextDate);
    if (!nextDate) {
      onChange('');
    } else if (time) {
      onChange(`${nextDate}T${time}`);
    }
  };

  const updateTime = (event) => {
    const nextTime = event.target.value;
    setTime(nextTime);
    if (date && nextTime) onChange(`${date}T${nextTime}`);
  };

  return (
    <div className="grid grid-cols-[minmax(0,1fr)_8.5rem] gap-2">
      <VietnameseDateInput
        className={className}
        id={id}
        min={min ? String(min).slice(0, 10) : undefined}
        onChange={updateDate}
        required={required}
        value={date}
      />
      <input
        aria-label="Giờ"
        className={className}
        onChange={updateTime}
        required={required}
        type="time"
        value={time}
      />
    </div>
  );
}
