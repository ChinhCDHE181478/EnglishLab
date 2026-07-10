export const CLASSROOM_TIME_SLOTS = [
  { label: '08:00 – 10:00', start: 8, end: 10 },
  { label: '10:00 – 12:00', start: 10, end: 12 },
  { label: '14:00 – 16:00', start: 14, end: 16 },
  { label: '17:00 – 19:00', start: 17, end: 19 },
  { label: '19:00 – 21:00', start: 19, end: 21 },
];

export const pad2 = (value) => String(value).padStart(2, '0');

export const getSessionSlotIndex = (startTime) => {
  if (!startTime) return -1;
  const hour = parseInt(String(startTime).split(':')[0], 10);
  for (let index = 0; index < CLASSROOM_TIME_SLOTS.length; index += 1) {
    const slot = CLASSROOM_TIME_SLOTS[index];
    if (hour >= slot.start && hour < slot.end) return index;
  }
  return -1;
};

export const buildSlotTimes = (slot) => ({
  startTime: `${pad2(slot.start)}:00:00`,
  endTime: `${pad2(slot.end)}:00:00`,
});

export const todayDateInputValue = () => {
  const date = new Date();
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`;
};
