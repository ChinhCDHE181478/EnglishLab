const WEEKDAYS = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
];

const parseLocalDate = (dateKey) => {
  const [year, month, day] = String(dateKey || '').split('-').map(Number);
  if (!year || !month || !day) return null;
  const date = new Date(year, month - 1, day);
  return Number.isNaN(date.getTime()) ? null : date;
};

const toLocalDateKey = (date) => [
  date.getFullYear(),
  String(date.getMonth() + 1).padStart(2, '0'),
  String(date.getDate()).padStart(2, '0'),
].join('-');

export const getWeekdayForDate = (dateKey) => {
  const date = parseLocalDate(dateKey);
  return date ? WEEKDAYS[date.getDay()] : '';
};

export const includeStartDateWeekday = (weekdays = [], startDate = '') => {
  const startWeekday = getWeekdayForDate(startDate);
  if (!startWeekday || weekdays.includes(startWeekday)) return weekdays;
  return [...weekdays, startWeekday];
};

export const alignStartDateToWeekdays = (startDate, weekdays = []) => {
  const cursor = parseLocalDate(startDate);
  const allowedWeekdays = new Set(weekdays);
  if (!cursor || !allowedWeekdays.size) return startDate;

  for (let offset = 0; offset < 7; offset += 1) {
    if (allowedWeekdays.has(WEEKDAYS[cursor.getDay()])) return toLocalDateKey(cursor);
    cursor.setDate(cursor.getDate() + 1);
  }
  return startDate;
};

export const toggleProposalWeekday = (startDate, weekdays = [], weekday) => {
  if (!weekdays.includes(weekday)) {
    return {
      plannedStartDate: startDate,
      weekdays: [...weekdays, weekday],
    };
  }

  const remainingWeekdays = weekdays.filter((item) => item !== weekday);
  if (!remainingWeekdays.length) {
    return { plannedStartDate: startDate, weekdays };
  }

  return {
    plannedStartDate: weekday === getWeekdayForDate(startDate)
      ? alignStartDateToWeekdays(startDate, remainingWeekdays)
      : startDate,
    weekdays: remainingWeekdays,
  };
};
