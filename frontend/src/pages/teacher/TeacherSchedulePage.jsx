import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  AlertCircle,
  AlertTriangle,
  Calendar,
  CalendarClock,
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock,
  Loader2,
  MapPin,
  Send,
  User,
  Video,
  ArrowRight,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import Header from '../../components/ai-learning/Header';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import BrandedSelect from '../../components/ui/BrandedSelect';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  ClassroomTypeBadge,
  DetailDrawer,
  LarkJoinButton,
  StatusBadge,
} from '../../components/classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDate, formatClassroomTime } from '../../utils/classroomHelpers';
import { PAGE_BODY_CLASS, PAGE_HEADER_CLASS, PAGE_SCHEDULE_CLASS, PAGE_SHELL_CLASS } from '../../utils/pageLayout';

// ─── Calendar constants ───────────────────────────────────────────────────────
const TIME_SLOTS = [
  { label: '08:00 – 10:00', start: 8,  end: 10 },
  { label: '10:00 – 12:00', start: 10, end: 12 },
  { label: '14:00 – 16:00', start: 14, end: 16 },
  { label: '17:00 – 19:00', start: 17, end: 19 },
  { label: '19:00 – 21:00', start: 19, end: 21 },
];

const DAY_VI = ['THỨ 2', 'THỨ 3', 'THỨ 4', 'THỨ 5', 'THỨ 6', 'THỨ 7', 'CN'];

// ─── Date helpers ─────────────────────────────────────────────────────────────
const getWeekMonday = (d) => {
  const date = new Date(d);
  date.setHours(0, 0, 0, 0);
  const day = date.getDay();
  date.setDate(date.getDate() - (day === 0 ? 6 : day - 1));
  return date;
};

const toDateStr = (d) => {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
};

const getSessionRow = (startTime) => {
  if (!startTime) return -1;
  const h = parseInt(startTime.split(':')[0], 10);
  for (let i = 0; i < TIME_SLOTS.length; i++) {
    if (h >= TIME_SLOTS[i].start && h < TIME_SLOTS[i].end) return i;
  }
  return -1;
};

const getDayIndex = (dateStr) => {
  const d = new Date(`${dateStr}T00:00:00`);
  const day = d.getDay();
  return day === 0 ? 6 : day - 1;
};

const formatMonthYear = (date) =>
  new Intl.DateTimeFormat('vi-VN', { month: 'long', year: 'numeric' }).format(date);

const formatFullDate = (dateStr) =>
  new Intl.DateTimeFormat('vi-VN', {
    weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric',
  }).format(new Date(dateStr));

// ─── Session visual style ─────────────────────────────────────────────────────
const getEffectiveStatus = (session) => {
  if (session.status === 'CANCELLED') return 'CANCELLED';
  if (!session.sessionDate || !session.startTime) return session.status;
  const now = new Date();
  const start = new Date(`${session.sessionDate}T${session.startTime}`);
  const endTime = session.endTime || (() => {
    const t = session.startTime.split(':').map(Number);
    return `${String(t[0] + 2).padStart(2, '0')}:${String(t[1]).padStart(2, '0')}:00`;
  })();
  const end = new Date(`${session.sessionDate}T${endTime}`);
  if (now >= end) return 'COMPLETED';
  if (now >= start && now < end) return 'IN_PROGRESS';
  return 'SCHEDULED';
};

const getSessionStyle = (session) => {
  const s = getEffectiveStatus(session);
  if (s === 'COMPLETED')
    return { border: 'border-l-emerald-500', bg: 'bg-white', badge: 'text-emerald-600', label: 'Hoàn thành' };
  if (s === 'IN_PROGRESS')
    return { border: 'border-l-[#b81d2e]', bg: 'bg-white shadow-md', badge: 'text-[#b81d2e]', label: 'Đang dạy' };
  if (s === 'CANCELLED')
    return { border: 'border-l-gray-300', bg: 'bg-white opacity-50', badge: 'text-gray-400', label: 'Đã hủy' };
  return { border: 'border-l-blue-500', bg: 'bg-white', badge: 'text-blue-600', label: 'Sắp dạy' };
};

// ─── Main Component ───────────────────────────────────────────────────────────
export default function TeacherSchedulePage() {
  const [viewMode, setViewMode] = useState('week');
  const [weekMonday, setWeekMonday] = useState(() => getWeekMonday(new Date()));
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedSession, setSelectedSession] = useState(null);

  const loadSchedule = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const assigned = await classroomApi.getTeacherAssignedClassrooms();
      const groups = await Promise.allSettled(
        assigned.map(async (cls) => {
          try {
            const items = await classroomApi.getTeacherClassroomSessions(cls.id);
            return items.map((s) => ({
              ...s,
              classroomId: cls.id,
              classroomTitle: cls.offeringTitle || cls.name || 'Lớp học',
              deliveryMode: s.deliveryMode || cls.deliveryMode,
            }));
          } catch { return []; }
        }),
      );
      setSessions(groups.filter((r) => r.status === 'fulfilled').flatMap((r) => r.value));
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể tải lịch dạy.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadSchedule();
  }, [loadSchedule]);

  const todayStr = useMemo(() => toDateStr(new Date()), []);

  const weekDays = useMemo(() =>
    Array.from({ length: 7 }, (_, i) => {
      const d = new Date(weekMonday);
      d.setDate(d.getDate() + i);
      return d;
    }),
    [weekMonday],
  );

  const weekSessions = useMemo(() => {
    const strs = new Set(weekDays.map(toDateStr));
    return sessions.filter((s) => strs.has(s.sessionDate));
  }, [sessions, weekDays]);

  const gridData = useMemo(() => {
    const g = Array.from({ length: TIME_SLOTS.length }, () =>
      Array.from({ length: 7 }, () => []),
    );
    weekSessions.forEach((s) => {
      const col = getDayIndex(s.sessionDate);
      const row = getSessionRow(s.startTime);
      if (row >= 0 && col >= 0 && col < 7) g[row][col].push(s);
    });
    return g;
  }, [weekSessions]);

  const todaySessions = useMemo(() =>
    sessions
      .filter((s) => s.sessionDate === todayStr)
      .sort((a, b) => (a.startTime || '').localeCompare(b.startTime || '')),
    [sessions, todayStr],
  );

  const sortedSessions = useMemo(() =>
    [...sessions].sort((a, b) =>
      `${a.sessionDate}T${a.startTime || '00:00'}`.localeCompare(`${b.sessionDate}T${b.startTime || '00:00'}`),
    ),
    [sessions],
  );

  const groupedSessions = useMemo(() => {
    const g = {};
    sortedSessions.forEach((s) => {
      if (!g[s.sessionDate]) g[s.sessionDate] = [];
      g[s.sessionDate].push(s);
    });
    return Object.entries(g);
  }, [sortedSessions]);

  const weekStats = useMemo(() => ({
    hours: weekSessions.reduce((acc, s) => {
      const sh = parseInt(s.startTime?.split(':')[0] || '0', 10);
      const eh = parseInt(s.endTime?.split(':')[0] || '0', 10);
      return acc + Math.max(0, eh - sh);
    }, 0),
    upcoming: sessions.filter((s) => s.sessionDate >= todayStr && s.status !== 'CANCELLED').length,
  }), [weekSessions, sessions, todayStr]);

  const prevWeek = () => setWeekMonday((m) => { const d = new Date(m); d.setDate(d.getDate() - 7); return d; });
  const nextWeek = () => setWeekMonday((m) => { const d = new Date(m); d.setDate(d.getDate() + 7); return d; });
  const goToday = () => setWeekMonday(getWeekMonday(new Date()));

  const todayColIndex = useMemo(() => weekDays.findIndex((d) => toDateStr(d) === todayStr), [weekDays, todayStr]);

  return (
    <div className={PAGE_SHELL_CLASS}>
      <CourseGlobalStyles />
      <style>{`
        .cal-grid-row { display: grid; grid-template-columns: 72px repeat(7, 1fr); min-height: 140px; }
        .cal-grid-head { display: grid; grid-template-columns: 72px repeat(7, 1fr); }
      `}</style>
      <div className={PAGE_HEADER_CLASS}>
        <Header />
      </div>

      <div className={PAGE_BODY_CLASS}>
      <motion.div
        className="flex flex-1 flex-col min-h-0"
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.32, ease: 'easeOut' }}
      >
        <div className={PAGE_SCHEDULE_CLASS}>

          {loading ? (
            <div className="flex flex-1 items-center justify-center py-32">
              <ClassroomLoadingState message="Đang tải lịch dạy của bạn..." />
            </div>
          ) : error ? (
            <div className="flex flex-1 items-center justify-center py-32">
              <ClassroomErrorState message={error} onRetry={loadSchedule} />
            </div>
          ) : (
            <>
              {/* ── LEFT: Calendar ── */}
              <section className="flex flex-1 min-w-0 flex-col rounded-2xl border border-[#e2e2e2] bg-white shadow-sm" style={{ minHeight: 'calc(100vh - 220px)' }}>

                {/* Toolbar */}
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#e2e2e2] bg-white px-5 py-3 rounded-t-2xl">
                  <div className="flex items-center gap-3">
                    <h2 className="font-['Manrope'] text-lg font-extrabold capitalize text-[#2b2828]">
                      {formatMonthYear(weekDays[0])}
                    </h2>
                    <div className="flex gap-1">
                      <button className="rounded-lg border border-gray-200 p-1.5 text-[#584140] transition hover:bg-[#fff3f4] hover:text-[#730014]" onClick={prevWeek} type="button">
                        <ChevronLeft className="h-3.5 w-3.5" />
                      </button>
                      <button className="rounded-lg border border-gray-200 px-3 py-1 text-[10px] font-extrabold text-[#584140] transition hover:bg-[#fff3f4] hover:text-[#730014]" onClick={goToday} type="button">
                        Hôm nay
                      </button>
                      <button className="rounded-lg border border-gray-200 p-1.5 text-[#584140] transition hover:bg-[#fff3f4] hover:text-[#730014]" onClick={nextWeek} type="button">
                        <ChevronRight className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="hidden items-center gap-4 rounded-full border border-gray-100 bg-[#f4f3f3] px-4 py-1.5 sm:flex">
                      <LegendDot color="bg-blue-500" label="Sắp dạy" />
                      <LegendDot color="bg-[#b81d2e]" label="Đang dạy" />
                      <LegendDot color="bg-emerald-500" label="Hoàn thành" />
                    </div>
                    <div className="flex rounded-xl border border-gray-200 bg-[#f4f3f3] p-0.5">
                      {[{ id: 'week', label: 'Tuần' }, { id: 'list', label: 'Danh sách' }].map((v) => (
                        <button
                          key={v.id}
                          className={`rounded-lg px-4 py-1.5 text-xs font-bold transition ${viewMode === v.id ? 'bg-white shadow-sm text-[#4b0009]' : 'text-[#8b706e] hover:text-[#4b0009]'}`}
                          onClick={() => setViewMode(v.id)}
                          type="button"
                        >
                          {v.label}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

                {/* ── WEEK VIEW ── */}
                {viewMode === 'week' && (
                  <div className="overflow-x-auto rounded-b-2xl">
                    <div style={{ minWidth: 580 }}>

                      {/* Day headers */}
                      <div className="cal-grid-head border-b-2 border-[#eeeeed] bg-white">
                        <div className="border-r border-[#eeeeed]" />
                        {weekDays.map((day, i) => {
                          const isToday = i === todayColIndex;
                          return (
                            <div
                              key={i}
                              className={`flex flex-col items-center justify-center border-r border-[#eeeeed] py-3 last:border-r-0 ${isToday ? 'bg-[#fff3f4]' : ''}`}
                            >
                              <p className={`text-[9px] font-extrabold uppercase tracking-widest ${isToday ? 'text-[#4b0009]' : 'text-[#8b706e]'}`}>
                                {DAY_VI[i]}
                              </p>
                              {isToday ? (
                                <span className="mt-1 flex h-7 w-7 items-center justify-center rounded-full bg-[#4b0009] font-['Manrope'] text-sm font-extrabold text-white">
                                  {day.getDate()}
                                </span>
                              ) : (
                                <span className="mt-1 font-['Manrope'] text-sm font-extrabold text-[#2b2828]">
                                  {day.getDate()}
                                </span>
                              )}
                            </div>
                          );
                        })}
                      </div>

                      {/* Time rows */}
                      {TIME_SLOTS.map((slot, rowIdx) => (
                        <div key={rowIdx} className="cal-grid-row border-b border-[#eeeeed] last:border-b-0">
                          <div className="flex items-start justify-center border-r border-[#eeeeed] bg-[#fafafa] pt-3">
                            <span className="text-[9px] font-semibold leading-tight text-[#8b706e]">{slot.label}</span>
                          </div>
                          {weekDays.map((_, colIdx) => {
                            const isToday = colIdx === todayColIndex;
                            const cellSessions = gridData[rowIdx][colIdx];
                            return (
                              <div
                                key={colIdx}
                                className={`border-r border-[#eeeeed] p-1.5 last:border-r-0 ${isToday ? 'bg-[#fff8f8]' : ''}`}
                              >
                                {cellSessions.map((s) => (
                                  <SessionGridCard key={s.id} session={s} onClick={() => setSelectedSession(s)} />
                                ))}
                              </div>
                            );
                          })}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* ── LIST VIEW ── */}
                {viewMode === 'list' && (
                  <div className="p-5 space-y-6">
                    {groupedSessions.length ? (
                      groupedSessions.map(([date, items]) => (
                        <div key={date} className="space-y-2">
                          <div className="py-2">
                            <h3 className="flex items-center gap-2 text-xs font-extrabold text-[#730014]">
                              <Calendar className="h-3.5 w-3.5" />
                              {formatFullDate(date)}
                              {date === todayStr && (
                                <span className="inline-flex rounded-full bg-[#4b0009] px-2 py-0.5 text-[9px] font-extrabold text-white">Hôm nay</span>
                              )}
                            </h3>
                          </div>
                          {items.map((session) => (
                            <SessionListRow key={session.id} session={session} onClick={() => setSelectedSession(session)} />
                          ))}
                        </div>
                      ))
                    ) : (
                      <ClassroomEmptyState
                        icon={Calendar}
                        title="Chưa có lịch dạy"
                        description="Bạn chưa được phân công buổi dạy nào."
                        actionLabel="Xem lớp giảng dạy"
                        actionTo="/teacher"
                      />
                    )}
                  </div>
                )}
              </section>

              {/* ── RIGHT: Today sidebar ── */}
              <aside className="hidden lg:block flex-shrink-0 sticky top-[72px] self-start" style={{ width: 312 }}>
                <div className="flex flex-col overflow-hidden rounded-2xl border border-[#e2e2e2] bg-white shadow-sm">
                  <div className="flex items-center justify-between border-b border-[#eeeeed] px-5 py-4">
                    <h3 className="font-['Manrope'] text-base font-extrabold text-[#4b0009]">Hôm nay</h3>
                    <p className="text-[10px] font-bold text-[#8b706e]">
                      {new Intl.DateTimeFormat('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' }).format(new Date())}
                    </p>
                  </div>

                  <div className="px-5 py-4">
                    {todaySessions.length ? (
                      <TodayTimeline sessions={todaySessions} onSelect={setSelectedSession} />
                    ) : (
                      <div className="flex flex-col items-center justify-center py-10 text-center">
                        <div className="mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-[#fff3f4] text-[#730014]">
                          <Calendar className="h-7 w-7" />
                        </div>
                        <p className="text-sm font-extrabold text-[#2b2828]">Không có buổi dạy hôm nay</p>
                        <p className="mt-1 text-xs text-[#8b706e]">Không có lịch — hãy chuẩn bị bài giảng!</p>
                      </div>
                    )}
                  </div>

                  <div className="border-t border-[#eeeeed] p-4">
                    <p className="mb-3 text-[10px] font-extrabold uppercase tracking-widest text-[#8b706e]">Tổng kết tuần này</p>
                    <div className="grid grid-cols-2 gap-2">
                      <div className="rounded-xl border border-gray-100 bg-[#fafafa] p-3">
                        <p className="text-[10px] font-semibold uppercase text-[#8b706e]">Giờ dạy</p>
                        <p className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">{weekStats.hours}h</p>
                      </div>
                      <div className="rounded-xl border border-emerald-100 bg-emerald-50/60 p-3">
                        <p className="text-[10px] font-semibold uppercase text-[#8b706e]">Sắp tới</p>
                        <p className="font-['Manrope'] text-xl font-extrabold text-emerald-700">{weekStats.upcoming}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </aside>
            </>
          )}
        </div>
      </motion.div>
      </div>

      <CourseFooter />

      {/* Detail Drawer */}
      <DetailDrawer
        isOpen={Boolean(selectedSession)}
        onClose={() => setSelectedSession(null)}
        title="Chi tiết buổi dạy"
      >
        {selectedSession && (
          <SessionDetailContent
            session={selectedSession}
            onSubmitted={() => {
              loadSchedule();
            }}
            onSessionUpdated={(updated) => {
              setSelectedSession((current) => (
                current?.id === updated.id ? { ...current, ...updated } : current
              ));
              setSessions((prev) => prev.map((item) => (
                item.id === updated.id ? { ...item, ...updated } : item
              )));
            }}
          />
        )}
      </DetailDrawer>
    </div>
  );
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function LegendDot({ color, label }) {
  return (
    <div className="flex items-center gap-1.5">
      <span className={`h-2 w-2 rounded-full ${color}`} />
      <span className="text-[10px] font-extrabold uppercase tracking-wider text-[#8b706e]">{label}</span>
    </div>
  );
}

function SessionGridCard({ session, onClick }) {
  const style = getSessionStyle(session);
  const isLive = getEffectiveStatus(session) === 'IN_PROGRESS';
  const isVirtual = session.deliveryMode === 'VIRTUAL';

  return (
    <div
      className={`mb-1 flex min-h-[80px] cursor-pointer flex-col justify-between overflow-hidden rounded-xl border-l-[3px] px-2.5 py-2 shadow-sm transition hover:shadow-md ${style.border} ${style.bg} ${isLive ? 'ring-1 ring-[#b81d2e]/20' : ''}`}
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && onClick()}
    >
      <div>
        <h4 className="text-[10px] font-extrabold leading-tight text-[#2b2828] line-clamp-2">{session.classroomTitle}</h4>
        {session.sessionContent && (
          <p className="mt-0.5 text-[9px] text-[#8b706e] line-clamp-1">{session.sessionContent}</p>
        )}
        <p className="mt-1 flex items-center gap-0.5 text-[9px] text-[#8b706e]">
          {isVirtual
            ? <><Video className="h-2.5 w-2.5 flex-shrink-0 text-purple-500" /><span className="line-clamp-1">Trực tuyến</span></>
            : <><MapPin className="h-2.5 w-2.5 flex-shrink-0 text-[#730014]" /><span className="line-clamp-1">{session.roomName ? `${session.roomName}${session.offlineAddress ? ' · ' + session.offlineAddress : ''}` : 'Đang xếp phòng'}</span></>
          }
        </p>
      </div>
      <div className="mt-1.5 flex items-center justify-between">
        <span className={`text-[9px] font-extrabold uppercase ${style.badge}`}>
          {isLive && <span className="mr-1 inline-block h-1.5 w-1.5 animate-ping rounded-full bg-current" />}
          {style.label}
        </span>
      </div>
      {isLive && (
        <Link
          className="mt-1.5 w-full rounded-lg bg-[#b81d2e] py-1 text-center text-[9px] font-extrabold text-white hover:bg-[#4b0009] transition active:scale-95"
          to={`/teacher/sessions/${session.id}`}
          onClick={(e) => e.stopPropagation()}
        >
          Quản lý
        </Link>
      )}
    </div>
  );
}

function SessionListRow({ session, onClick }) {
  const style = getSessionStyle(session);
  const isLive = getEffectiveStatus(session) === 'IN_PROGRESS';
  const isVirtual = session.deliveryMode === 'VIRTUAL';

  return (
    <div className={`flex flex-col gap-3 rounded-2xl border p-4 transition sm:flex-row sm:items-center ${
      isLive ? 'border-[#b81d2e]/20 bg-[#fff3f4]/30' : 'border-gray-100 bg-white hover:bg-gray-50/30'
    }`}>
      {/* Time block */}
      <div className={`flex w-24 flex-shrink-0 flex-col items-center rounded-xl border py-2 text-center ${
        isLive ? 'border-[#b81d2e]/20 bg-[#b81d2e]/5' : 'border-gray-100 bg-gray-50'
      }`}>
        <p className={`text-xs font-extrabold ${isLive ? 'text-[#b81d2e]' : 'text-[#2b2828]'}`}>
          {formatClassroomTime(session.startTime)}
        </p>
        <p className="text-[10px] text-[#8b706e]">{formatClassroomTime(session.endTime)}</p>
      </div>

      {/* Info */}
      <div className="flex-1 min-w-0 space-y-1">
        <div className="flex flex-wrap items-center gap-1.5">
          <StatusBadge status={getEffectiveStatus(session)} />
          <ClassroomTypeBadge mode={session.deliveryMode} />
        </div>
        <h4 className="font-['Manrope'] text-sm font-extrabold text-[#2b2828] line-clamp-1">{session.classroomTitle}</h4>
        <p className="flex flex-wrap items-center gap-3 text-[10px] text-[#8b706e]">
          {isVirtual ? (
            <span className="flex items-center gap-1 font-bold text-purple-700"><Video className="h-3 w-3" /> Lark</span>
          ) : (
            <span className="flex items-center gap-1"><MapPin className="h-3 w-3" />{session.roomName || 'Đang xếp phòng'}</span>
          )}
          {session.sessionContent && (
            <span className="flex items-center gap-1"><User className="h-3 w-3" />{session.sessionContent}</span>
          )}
        </p>
      </div>

      {/* Actions */}
      <div className="flex flex-shrink-0 flex-wrap gap-1.5">
        <Link
          className="inline-flex items-center gap-1 rounded-xl border border-[#dfbfbd]/60 bg-white px-3 py-1.5 text-[10px] font-extrabold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95"
          to={`/teacher/sessions/${session.id}`}
        >
          Quản lý <ArrowRight className="h-3 w-3" />
        </Link>
        <button
          className="inline-flex items-center gap-1 rounded-xl border border-gray-200 bg-white px-3 py-1.5 text-[10px] font-extrabold text-[#584140] transition hover:bg-gray-50"
          onClick={onClick}
          type="button"
        >
          Chi tiết
        </button>
      </div>
    </div>
  );
}

function TodayTimeline({ sessions, onSelect }) {
  const now = new Date();
  const nowDecimal = now.getHours() + now.getMinutes() / 60;

  return (
    <div className="relative">
      <div className="absolute left-3 top-2 bottom-2 w-px bg-[#dfbfbd]/50" />
      <div className="space-y-7 pl-10">
        {sessions.map((session) => {
          const sh = parseInt(session.startTime?.split(':')[0] || '0', 10);
          const eh = parseInt(session.endTime?.split(':')[0] || '0', 10);
          const isLive = sh <= nowDecimal && nowDecimal < eh;
          const isPast = eh <= nowDecimal;

          const dotColor = isPast
            ? 'bg-emerald-500 ring-emerald-100'
            : isLive
              ? 'bg-[#b81d2e] ring-[#b81d2e]/20 animate-pulse'
              : 'bg-gray-300 ring-gray-100';

          return (
            <div key={session.id} className="relative">
              <div className={`absolute -left-[31px] top-1 h-4 w-4 rounded-full border-2 border-white ring-4 ${dotColor}`} />

              {isLive ? (
                <div
                  className="cursor-pointer rounded-2xl border border-[#b81d2e]/20 bg-[#fff3f4]/60 p-4"
                  onClick={() => onSelect(session)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => e.key === 'Enter' && onSelect(session)}
                >
                  <div className="flex items-center gap-2">
                    <p className="text-[10px] font-extrabold uppercase text-[#b81d2e]">
                      {formatClassroomTime(session.startTime)} – {formatClassroomTime(session.endTime)}
                    </p>
                    <span className="flex h-1.5 w-1.5 rounded-full bg-[#b81d2e] animate-ping" />
                    <span className="text-[10px] font-extrabold text-[#b81d2e]">Đang dạy</span>
                  </div>
                  <h4 className="mt-1 font-['Manrope'] text-sm font-extrabold text-[#4b0009]">{session.classroomTitle}</h4>
                  <Link
                    to={`/teacher/sessions/${session.id}`}
                    className="mt-3 flex items-center justify-between rounded-xl bg-[#4b0009] px-3 py-2"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <span className="text-xs font-bold text-white">Quản lý buổi học</span>
                    <ArrowRight className="h-3.5 w-3.5 text-white" />
                  </Link>
                </div>
              ) : (
                <div
                  className="cursor-pointer space-y-1"
                  onClick={() => onSelect(session)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => e.key === 'Enter' && onSelect(session)}
                >
                  <p className={`text-[10px] font-extrabold uppercase ${isPast ? 'text-emerald-600' : 'text-[#8b706e]'}`}>
                    {formatClassroomTime(session.startTime)} – {formatClassroomTime(session.endTime)}
                    {isPast && ' • Hoàn thành'}
                  </p>
                  <h4 className="text-sm font-extrabold text-[#2b2828]">{session.classroomTitle}</h4>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function SessionDetailContent({ session, onSubmitted, onSessionUpdated }) {
  const isVirtual = session.deliveryMode === 'VIRTUAL';
  const effectiveStatus = getEffectiveStatus(session);
  const isLocked = ['COMPLETED', 'CANCELLED'].includes(effectiveStatus) || session.locked;
  const canOpenLarkRoom = isVirtual && !['CANCELLED'].includes(effectiveStatus);

  const [showReschedule, setShowReschedule] = useState(false);
  const [larkMessage, setLarkMessage] = useState('');
  const [openingLark, setOpeningLark] = useState(false);

  const handleOpenLarkRoom = async () => {
    setLarkMessage('');
    const roomWindow = window.open('about:blank', '_blank');
    if (roomWindow) {
      roomWindow.opener = null;
    }
    if (!canOpenLarkRoom) {
      roomWindow?.close();
      setLarkMessage('Buổi học đã kết thúc hoặc đã hủy nên không thể tạo phòng Lark.');
      return;
    }

    setOpeningLark(true);
    try {
      const updated = await classroomApi.openVirtualSession(session.id);
      onSessionUpdated?.(updated);
      if (updated.larkMeetingUrl) {
        if (roomWindow) {
          roomWindow.location.replace(updated.larkMeetingUrl);
        } else {
          setLarkMessage('Trình duyệt đã chặn cửa sổ mới. Hãy cho phép popup cho EnglishLab rồi thử lại.');
        }
      } else {
        roomWindow?.close();
        setLarkMessage('Chưa tạo được link Lark. Vào Quản lý buổi học để nhập link thủ công.');
      }
    } catch (err) {
      roomWindow?.close();
      setLarkMessage(getClassroomErrorMessage(err, 'Không thể tạo phòng Lark.'));
    } finally {
      setOpeningLark(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="space-y-3">
        <p className="text-xs font-extrabold uppercase tracking-wider text-[#730014]">{session.classroomTitle}</p>
        <h3 className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
          {session.sessionContent || `Buổi học ngày ${formatClassroomDate(session.sessionDate)}`}
        </h3>
        <div className="flex flex-wrap gap-2">
          <ClassroomTypeBadge mode={session.deliveryMode} />
          <StatusBadge status={effectiveStatus} />
        </div>
      </div>

      {/* Time & location */}
      <div className="rounded-2xl border border-gray-100 bg-gray-50/30 p-5 space-y-3">
        <h4 className="text-xs font-bold uppercase tracking-wider text-[#8b706e]">Thời gian & Địa điểm</h4>
        <div className="space-y-2.5 text-sm text-[#584140]">
          <div className="flex items-center gap-3">
            <Calendar className="h-4 w-4 flex-shrink-0 text-[#730014]" />
            <span>{formatFullDate(session.sessionDate)}</span>
          </div>
          <div className="flex items-center gap-3">
            <Clock className="h-4 w-4 flex-shrink-0 text-[#730014]" />
            <span>{formatClassroomTime(session.startTime)} – {formatClassroomTime(session.endTime)}</span>
          </div>
          <div className="flex items-center gap-3">
            {isVirtual ? <Video className="h-4 w-4 flex-shrink-0 text-purple-700" /> : <MapPin className="h-4 w-4 flex-shrink-0 text-[#730014]" />}
            <span>
              {isVirtual
                ? 'Lớp học trực tuyến (Lark)'
                : `${session.roomName || 'Đang xếp phòng'} · ${session.offlineAddress || 'Cơ sở Hà Nội'}`}
            </span>
          </div>
        </div>
      </div>

      {/* Lark block */}
      {isVirtual && (
        <div className="rounded-2xl border border-purple-100 bg-purple-50/10 p-5 space-y-3">
          <h4 className="flex items-center gap-1 text-xs font-bold uppercase tracking-wider text-purple-700">
            <Video className="h-3.5 w-3.5" /> Phòng học Lark
          </h4>
          {session.larkMeetingUrl ? (
            <>
              <p className="text-xs leading-6 text-[#8b706e]">
                Liên kết phòng học đã sẵn sàng. Bạn có thể vào phòng trực tiếp từ đây.
              </p>
              <LarkJoinButton
                label={effectiveStatus === 'COMPLETED' ? 'Xem lại phòng Lark' : 'Vào phòng Lark'}
                onBlocked={setLarkMessage}
                onClick={handleOpenLarkRoom}
                url={session.larkMeetingUrl}
              />
            </>
          ) : (
            <>
              <p className="text-xs leading-6 text-[#8b706e]">
                Buổi học chưa có link Lark. Bấm nút bên dưới để tạo phòng tự động hoặc vào trang quản lý buổi học để nhập link thủ công.
              </p>
              {canOpenLarkRoom ? (
                <button
                  className="inline-flex items-center justify-center gap-1.5 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014] disabled:opacity-60"
                  disabled={openingLark}
                  onClick={handleOpenLarkRoom}
                  type="button"
                >
                  {openingLark ? <Loader2 className="h-4 w-4 animate-spin" /> : <Video className="h-4 w-4" />}
                  {openingLark ? 'Đang tạo phòng...' : 'Tạo và mở phòng Lark'}
                </button>
              ) : (
                <p className="rounded-xl border border-gray-100 bg-white/70 px-4 py-3 text-xs font-semibold text-[#8b706e]">
                  Buổi học đã kết thúc mà chưa có link Lark được lưu.
                </p>
              )}
            </>
          )}
          {larkMessage ? (
            <p className="text-xs font-semibold text-[#93000a]">{larkMessage}</p>
          ) : null}
        </div>
      )}

      {/* Reschedule form */}
      {showReschedule ? (
        <RescheduleForm
          session={session}
          onCancel={() => setShowReschedule(false)}
          onSubmitted={() => {
            setShowReschedule(false);
            onSubmitted?.();
          }}
        />
      ) : (
        <div className="border-t border-gray-100 pt-4 space-y-2">
          {!isLocked && (
            <button
              type="button"
              onClick={() => setShowReschedule(true)}
              className="flex w-full items-center justify-center gap-1.5 rounded-2xl bg-[#4b0009] py-3.5 text-sm font-extrabold text-white shadow-sm transition hover:bg-[#730014] active:scale-95"
            >
              <CalendarClock className="h-4 w-4" /> Chuyển lịch dạy
            </button>
          )}
          {isLocked && (
            <p className="rounded-xl border border-gray-100 bg-gray-50/50 px-4 py-3 text-center text-xs font-semibold text-[#8b706e]">
              Buổi học đã kết thúc hoặc đã hủy nên không thể đổi lịch.
            </p>
          )}
          <Link
            className="flex items-center justify-center gap-1.5 rounded-2xl border border-[#e5e7eb] py-3 text-sm font-bold text-[#1a1c1c] transition hover:bg-gray-50"
            to={`/teacher/sessions/${session.id}`}
          >
            Quản lý buổi học <ArrowRight className="h-4 w-4" />
          </Link>
          <Link
            className="flex items-center justify-center gap-1.5 rounded-2xl border border-[#e5e7eb] py-3 text-sm font-bold text-[#1a1c1c] transition hover:bg-gray-50"
            to={`/teacher/classrooms/${session.classroomOfferingId || session.classroomId}`}
          >
            Xem lớp học
          </Link>
        </div>
      )}
    </div>
  );
}

const pad2 = (n) => String(n).padStart(2, '0');

function RescheduleForm({ session, onCancel, onSubmitted }) {
  const todayStr = useMemo(() => {
    const d = new Date();
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
  }, []);

  const buildNewValues = (slot, date) =>
    JSON.stringify({
      sessionDate: date,
      startTime: `${pad2(slot.start)}:00:00`,
      endTime: `${pad2(slot.end)}:00:00`,
      teacherId: session.teacherId ?? null,
      roomId: session.roomId ?? null,
    });

  const [newDate, setNewDate] = useState(session.sessionDate || todayStr);
  const [slotIndex, setSlotIndex] = useState(null);
  const [reason, setReason] = useState('');
  const [checking, setChecking] = useState(false);
  // slotStatus[i] = { available: bool, conflicts: [...] }
  const [slotStatus, setSlotStatus] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const offeringId = session.classroomOfferingId || session.classroomId;
  const currentSlotIndex = getSessionRow(session.startTime);

  // When date changes, probe every time slot and keep only the free ones
  useEffect(() => {
    let active = true;
    setError('');
    setSuccess('');
    setSlotStatus({});
    setSlotIndex(null);
    if (!newDate) return undefined;

    setChecking(true);
    const checkSlots = async () => {
      try {
        const results = await Promise.all(
          TIME_SLOTS.map(async (slot) => {
            try {
              const res = await classroomApi.checkTeacherChangeConflict({
            requestType: 'RESCHEDULE_SESSION',
            classroomOfferingId: offeringId,
            targetSessionId: session.id,
            newValuesJson: buildNewValues(slot, newDate),
            reason: 'Kiểm tra trùng lịch',
              });
              return { available: !res?.hasBlockingConflict, conflicts: res?.conflicts || [] };
            } catch {
              return { available: false, conflicts: [], failed: true };
            }
          }),
        );
        if (!active) return;
        const map = {};
        results.forEach((r, i) => { map[i] = r; });
        setSlotStatus(map);
        // Auto-select the first free slot (prefer the current slot if still free)
        const firstFree =
          (map[currentSlotIndex]?.available ? currentSlotIndex : null) ??
          results.findIndex((r) => r.available);
        setSlotIndex(firstFree >= 0 ? firstFree : null);
        if (results.every((r) => r.failed)) {
          setError('Không thể kiểm tra trùng lịch. Vui lòng thử lại.');
        }
      } finally {
        if (active) setChecking(false);
      }
    };

    checkSlots();

    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [newDate]);

  const availableOptions = TIME_SLOTS
    .map((s, i) => ({ slot: s, index: i }))
    .filter(({ index }) => slotStatus[index]?.available)
    .map(({ slot, index }) => ({ label: slot.label, value: String(index) }));

  const removedCount = TIME_SLOTS.length - availableOptions.length;
  const hasChecked = Object.keys(slotStatus).length > 0;
  const noSlotAvailable = hasChecked && availableOptions.length === 0;

  const isSameAsCurrent = newDate === session.sessionDate && slotIndex === currentSlotIndex;

  const canSubmit =
    !!newDate &&
    slotIndex != null &&
    slotStatus[slotIndex]?.available &&
    !isSameAsCurrent &&
    !checking &&
    reason.trim().length > 0 &&
    !submitting;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setSubmitting(true);
    setError('');
    try {
      await classroomApi.createChangeRequest({
        requestType: 'RESCHEDULE_SESSION',
        classroomOfferingId: offeringId,
        targetSessionId: session.id,
        newValuesJson: buildNewValues(TIME_SLOTS[slotIndex], newDate),
        reason: reason.trim(),
      });
      setSuccess('Đã gửi yêu cầu chuyển lịch. Yêu cầu sẽ được Quản lý đào tạo xác nhận trước khi áp dụng.');
      setTimeout(() => onSubmitted?.(), 1400);
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể gửi yêu cầu chuyển lịch.'));
    } finally {
      setSubmitting(false);
    }
  };

  if (success) {
    return (
      <div className="rounded-2xl border border-emerald-100 bg-emerald-50/40 p-5 text-center space-y-3">
        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 text-emerald-700">
          <CheckCircle2 className="h-6 w-6" />
        </div>
        <p className="text-sm font-semibold leading-6 text-emerald-800">{success}</p>
      </div>
    );
  }

  return (
    <div className="rounded-2xl border border-[#e5e7eb] bg-white p-5 space-y-4">
      <div className="flex items-center gap-2">
        <CalendarClock className="h-4 w-4 text-[#730014]" />
        <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Chuyển lịch buổi dạy</h4>
      </div>
      <p className="text-xs leading-6 text-[#8b706e]">
        Chọn ngày mới — hệ thống chỉ hiển thị các khung giờ mà học viên trong lớp không bị
        trùng lịch với lớp khác. Yêu cầu cần Quản lý đào tạo xác nhận trước khi áp dụng.
      </p>

      {/* New date */}
      <div className="space-y-1.5">
        <label className="text-xs font-bold uppercase tracking-wider text-[#8b706e]">Ngày mới</label>
        <input
          type="date"
          min={todayStr}
          value={newDate}
          onChange={(e) => setNewDate(e.target.value)}
          className="w-full rounded-xl border border-[#dfbfbd]/60 bg-white px-4 py-2.5 text-sm text-[#2b2828] outline-none transition focus:border-[#730014]"
        />
      </div>

      {/* New time slot */}
      <div className="space-y-1.5">
        <label className="text-xs font-bold uppercase tracking-wider text-[#8b706e]">Khung giờ trống</label>
        {checking ? (
          <div className="flex items-center gap-2 rounded-xl border border-gray-100 bg-gray-50/50 px-4 py-3 text-xs font-semibold text-[#8b706e]">
            <Loader2 className="h-4 w-4 animate-spin" /> Đang tìm khung giờ trống...
          </div>
        ) : noSlotAvailable ? (
          <div className="flex items-start gap-2 rounded-xl border border-rose-100 bg-rose-50/40 px-4 py-3 text-xs font-semibold text-rose-800">
            <AlertTriangle className="h-4 w-4 flex-shrink-0" />
            Ngày này không còn khung giờ nào trống cho học viên của lớp. Hãy chọn ngày khác.
          </div>
        ) : (
          <>
            <BrandedSelect
              value={slotIndex != null ? String(slotIndex) : ''}
              onChange={(e) => setSlotIndex(Number(e.target.value))}
              options={availableOptions}
              placeholder="Chọn khung giờ"
            />
            {removedCount > 0 && (
              <p className="flex items-center gap-1 text-[11px] text-[#8b706e]">
                <AlertCircle className="h-3 w-3" />
                Đã ẩn {removedCount} khung giờ bị trùng lịch học viên.
              </p>
            )}
          </>
        )}
      </div>

      {/* Reason */}
      <div className="space-y-1.5">
        <label className="text-xs font-bold uppercase tracking-wider text-[#8b706e]">Lý do chuyển lịch</label>
        <textarea
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="Nhập lý do để Quản lý đào tạo xem xét..."
          className="min-h-[80px] w-full rounded-xl border border-[#dfbfbd]/60 bg-white px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014]"
        />
      </div>

      {/* Status hints */}
      {error ? (
        <div className="flex items-start gap-2 rounded-xl border border-rose-100 bg-rose-50/50 px-4 py-3 text-xs font-semibold text-rose-800">
          <AlertCircle className="h-4 w-4 flex-shrink-0" /> {error}
        </div>
      ) : isSameAsCurrent ? (
        <p className="text-xs font-semibold text-[#8b706e]">Khung giờ này trùng với lịch hiện tại. Hãy chọn ngày hoặc khung giờ khác.</p>
      ) : canSubmit ? (
        <div className="flex items-center gap-2 rounded-xl border border-emerald-100 bg-emerald-50/50 px-4 py-3 text-xs font-semibold text-emerald-800">
          <Check className="h-4 w-4" /> Khung giờ trống. Có thể gửi yêu cầu chuyển.
        </div>
      ) : null}

      {/* Actions */}
      <div className="flex gap-2 pt-1">
        <button
          type="button"
          onClick={onCancel}
          className="flex-1 rounded-xl border border-[#e5e7eb] py-3 text-sm font-bold text-[#1a1c1c] transition hover:bg-gray-50"
        >
          Hủy
        </button>
        <button
          type="button"
          onClick={handleSubmit}
          disabled={!canSubmit}
          className="flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-[#4b0009] py-3 text-sm font-extrabold text-white shadow-sm transition hover:bg-[#730014] active:scale-95 disabled:cursor-not-allowed disabled:opacity-40"
        >
          {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
          Gửi yêu cầu
        </button>
      </div>
    </div>
  );
}
