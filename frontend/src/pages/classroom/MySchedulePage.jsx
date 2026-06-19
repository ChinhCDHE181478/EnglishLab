import { useEffect, useMemo, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  Calendar,
  Clock,
  MapPin,
  Video,
  ChevronLeft,
  ChevronRight,
  Play,
  User,
  AlertCircle,
  CheckCircle2,
  ExternalLink,
  Lock,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import Header from '../../components/ai-learning/Header';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import {
  ClassroomErrorState,
  ClassroomLoadingState,
  ClassroomEmptyState,
  LarkJoinButton,
  DetailDrawer,
  ClassroomTypeBadge,
  StatusBadge,
} from '../../components/classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatClassroomDate,
  formatClassroomTime,
  openLarkMeeting,
} from '../../utils/classroomHelpers';
import { getStoredUser, hasAccessToken } from '../../utils/auth';

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

const toDateStr = (d) => d.toISOString().split('T')[0];

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
/**
 * Compute the display status based on real clock time, overriding stale DB values.
 * Backend demo data may seed IN_PROGRESS for past/future sessions, so we correct it here.
 */
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
    return { border: 'border-l-[#b81d2e]', bg: 'bg-white shadow-md', badge: 'text-[#b81d2e]', label: 'Đang học' };
  if (s === 'CANCELLED')
    return { border: 'border-l-gray-300', bg: 'bg-white opacity-50', badge: 'text-gray-400', label: 'Đã hủy' };
  return { border: 'border-l-blue-500', bg: 'bg-white', badge: 'text-blue-600', label: 'Sắp học' };
};

// ─── Main Component ───────────────────────────────────────────────────────────
export default function MySchedulePage() {
  const [viewMode, setViewMode] = useState('week');
  const [weekMonday, setWeekMonday] = useState(() => getWeekMonday(new Date()));
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedSession, setSelectedSession] = useState(null);
  const [larkMessage, setLarkMessage] = useState('');
  const isAuthenticated = Boolean(hasAccessToken() && getStoredUser());

  const loadSchedule = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const myClassrooms = await classroomApi.getMyClassrooms();
      const groups = await Promise.all(
        myClassrooms.map(async (cls) => {
          try {
            const items = await classroomApi.getMyClassroomSessions(cls.id);
            return items.map((s) => ({
              ...s,
              classroomId: cls.id,
              classroomTitle: cls.title,
              teacherName: cls.primaryTeacherName,
            }));
          } catch { return []; }
        }),
      );
      setSessions(groups.flat());
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể tải lịch học.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isAuthenticated) { setLoading(false); return; }
    loadSchedule();
  }, [isAuthenticated, loadSchedule]);

  const todayStr = useMemo(() => new Date().toISOString().split('T')[0], []);

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
    <div className="course-page flex min-h-[100dvh] flex-col bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <style>{`
        .cal-grid-row { display: grid; grid-template-columns: 72px repeat(7, 1fr); min-height: 140px; }
        .cal-grid-head { display: grid; grid-template-columns: 72px repeat(7, 1fr); }
      `}</style>
      <Header />

      {/* ── MAIN AREA ── */}
      <div className="flex-1">
        <div className="mx-auto flex w-full max-w-[1320px] items-start gap-5 px-4 py-5 pb-10 md:px-10 md:pb-12">

          {!isAuthenticated ? (
            <div className="flex flex-1 items-center justify-center py-32">
              <ClassroomEmptyState icon={Lock} title="Chưa đăng nhập" description="Bạn cần đăng nhập để xem lịch học." actionLabel="Đăng nhập" actionTo="/login" />
            </div>
          ) : loading ? (
            <div className="flex flex-1 items-center justify-center py-32">
              <ClassroomLoadingState message="Đang tải lịch học của bạn..." />
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
                      <LegendDot color="bg-blue-500" label="Sắp diễn ra" />
                      <LegendDot color="bg-[#b81d2e]" label="Đang học" />
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
                  /* Outer wrapper: clips horizontal overflow without becoming a vertical scroll container */
                  <div className="overflow-x-auto rounded-b-2xl">
                    <div style={{ minWidth: 580 }}>

                      {/* Day headers — plain (no sticky, works correctly with page scroll) */}
                      <div className="cal-grid-head border-b-2 border-[#eeeeed] bg-white">
                        {/* Time label corner */}
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
                          {/* Time label */}
                          <div className="flex items-start justify-center border-r border-[#eeeeed] bg-[#fafafa] pt-3">
                            <span className="text-[9px] font-semibold leading-tight text-[#8b706e]">{slot.label}</span>
                          </div>
                          {/* Day cells */}
                          {weekDays.map((_, colIdx) => {
                            const isToday = colIdx === todayColIndex;
                            const cellSessions = gridData[rowIdx][colIdx];
                            return (
                              <div
                                key={colIdx}
                                className={`border-r border-[#eeeeed] p-1.5 last:border-r-0 ${isToday ? 'bg-[#fff8f8]' : ''}`}
                              >
                                {cellSessions.map((s) => (
                                  <SessionGridCard key={s.id} session={s} onClick={() => setSelectedSession(s)} onLark={setLarkMessage} />
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
                            <SessionListRow key={session.id} session={session} onClick={() => setSelectedSession(session)} onLark={setLarkMessage} />
                          ))}
                        </div>
                      ))
                    ) : (
                      <ClassroomEmptyState
                        icon={Calendar}
                        title="Chưa có lịch học"
                        description="Bạn chưa có buổi học nào được lên lịch."
                        actionLabel="Xem lớp học"
                        actionTo="/classrooms"
                      />
                    )}
                    {larkMessage && (
                      <div className="rounded-2xl border border-rose-100 bg-rose-50/50 p-3 text-xs text-rose-800 flex items-start gap-2">
                        <AlertCircle className="h-4 w-4 flex-shrink-0 text-rose-600" />
                        {larkMessage}
                      </div>
                    )}
                  </div>
                )}
              </section>

              {/* ── RIGHT: Today sidebar (sticky) ── */}
              <aside className="hidden lg:block flex-shrink-0 sticky top-[72px] self-start" style={{ width: 312 }}>
                <div className="flex flex-col overflow-hidden rounded-2xl border border-[#e2e2e2] bg-white shadow-sm">
                  {/* Sidebar header */}
                  <div className="flex items-center justify-between border-b border-[#eeeeed] px-5 py-4">
                    <h3 className="font-['Manrope'] text-base font-extrabold text-[#4b0009]">Hôm nay</h3>
                    <p className="text-[10px] font-bold text-[#8b706e]">
                      {new Intl.DateTimeFormat('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' }).format(new Date())}
                    </p>
                  </div>

                  {/* Today's sessions */}
                  <div className="px-5 py-4">
                    {todaySessions.length ? (
                      <TodayTimeline sessions={todaySessions} onSelect={setSelectedSession} onLark={setLarkMessage} />
                    ) : (
                      <div className="flex flex-col items-center justify-center py-10 text-center">
                        <div className="mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-[#fff3f4] text-[#730014]">
                          <Calendar className="h-7 w-7" />
                        </div>
                        <p className="text-sm font-extrabold text-[#2b2828]">Không có lịch hôm nay</p>
                        <p className="mt-1 text-xs text-[#8b706e]">Ngày tự do — hãy ôn luyện!</p>
                      </div>
                    )}
                  </div>

                  {/* Summary widget */}
                  <div className="border-t border-[#eeeeed] p-4">
                    <p className="mb-3 text-[10px] font-extrabold uppercase tracking-widest text-[#8b706e]">Tổng kết tuần này</p>
                    <div className="grid grid-cols-2 gap-2">
                      <div className="rounded-xl border border-gray-100 bg-[#fafafa] p-3">
                        <p className="text-[10px] font-semibold uppercase text-[#8b706e]">Giờ học</p>
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
      </div>

      {/* ── FOOTER ── */}
      <CourseFooter />

      {/* ── Detail Drawer ── */}
      <DetailDrawer
        isOpen={Boolean(selectedSession)}
        onClose={() => setSelectedSession(null)}
        title="Chi tiết buổi học"
      >
        {selectedSession && (
          <SessionDetailContent
            session={selectedSession}
            larkMessage={larkMessage}
            onLark={setLarkMessage}
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

function SessionGridCard({ session, onClick, onLark }) {
  const style = getSessionStyle(session);
  const isLive = getEffectiveStatus(session) === 'IN_PROGRESS';
  const isVirtual = session.deliveryMode === 'VIRTUAL';

  const handleLark = (e) => {
    e.stopPropagation();
    const result = openLarkMeeting(session.larkMeetingUrl);
    if (!result.ok) onLark(result.message);
  };

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
        {/* Room / location */}
        <p className="mt-1 flex items-center gap-0.5 text-[9px] text-[#8b706e]">
          {isVirtual
            ? <><Video className="h-2.5 w-2.5 flex-shrink-0 text-purple-500" /><span className="line-clamp-1">Trực tuyến</span></>
            : <><MapPin className="h-2.5 w-2.5 flex-shrink-0 text-[#730014]" /><span className="line-clamp-1">{session.roomName ? `${session.roomName}${session.campusName ? ' · ' + session.campusName : ''}` : 'Đang xếp phòng'}</span></>
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
        <button
          className="mt-1.5 w-full rounded-lg bg-[#b81d2e] py-1 text-[9px] font-extrabold text-white hover:bg-[#4b0009] transition active:scale-95"
          onClick={isVirtual && session.larkMeetingUrl ? handleLark : onClick}
          type="button"
        >
          Vào học
        </button>
      )}
    </div>
  );
}

function SessionListRow({ session, onClick, onLark }) {
  const style = getSessionStyle(session);
  const isLive = getEffectiveStatus(session) === 'IN_PROGRESS';
  const isVirtual = session.deliveryMode === 'VIRTUAL';
  const isLarkJoinable = isVirtual && session.larkJoinable && session.larkMeetingUrl;

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
          <StatusBadge status={session.status} />
          <ClassroomTypeBadge mode={session.deliveryMode} />
        </div>
        <h4 className="font-['Manrope'] text-sm font-extrabold text-[#2b2828] line-clamp-1">{session.classroomTitle}</h4>
        <p className="flex flex-wrap items-center gap-3 text-[10px] text-[#8b706e]">
          {isVirtual ? (
            <span className="flex items-center gap-1 font-bold text-purple-700"><Video className="h-3 w-3" /> Lark</span>
          ) : (
            <span className="flex items-center gap-1"><MapPin className="h-3 w-3" />{session.roomName || 'Đang xếp phòng'}</span>
          )}
          {session.teacherName && (
            <span className="flex items-center gap-1"><User className="h-3 w-3" />{session.teacherName}</span>
          )}
        </p>
      </div>

      {/* Actions */}
      <div className="flex flex-shrink-0 flex-wrap gap-1.5">
        {isLarkJoinable && (
          <LarkJoinButton className="!px-3 !py-1.5 !text-[10px]" onBlocked={onLark} url={session.larkMeetingUrl} />
        )}
        {session.recordingUrl && (
          <a className="inline-flex items-center gap-1 rounded-xl border border-gray-200 bg-white px-3 py-1.5 text-[10px] font-extrabold text-[#584140] hover:bg-gray-50"
            href={session.recordingUrl} rel="noreferrer" target="_blank">
            <Play className="h-3 w-3" /> Ghi âm
          </a>
        )}
        <button
          className="inline-flex items-center gap-1 rounded-xl border border-[#dfbfbd]/60 bg-white px-3 py-1.5 text-[10px] font-extrabold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95"
          onClick={onClick}
          type="button"
        >
          Chi tiết
        </button>
      </div>
    </div>
  );
}

function TodayTimeline({ sessions, onSelect, onLark }) {
  const now = new Date();
  const nowDecimal = now.getHours() + now.getMinutes() / 60;

  return (
    <div className="relative">
      {/* Vertical line */}
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
              {/* Dot */}
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
                    <span className="text-[10px] font-extrabold text-[#b81d2e]">Đang học</span>
                  </div>
                  <h4 className="mt-1 font-['Manrope'] text-sm font-extrabold text-[#4b0009]">{session.classroomTitle}</h4>
                  {session.larkMeetingUrl && (
                    <div className="mt-3 flex items-center justify-between rounded-xl bg-white/80 px-3 py-2">
                      <span className="flex items-center gap-1.5 text-xs font-bold text-[#584140]">
                        <Video className="h-3.5 w-3.5 text-purple-600" /> Lark Meeting
                      </span>
                      <button
                        className="rounded-lg bg-[#b81d2e] px-3 py-1 text-[10px] font-extrabold text-white transition hover:bg-[#4b0009] active:scale-95"
                        onClick={(e) => {
                          e.stopPropagation();
                          const r = openLarkMeeting(session.larkMeetingUrl);
                          if (!r.ok) onLark(r.message);
                        }}
                        type="button"
                      >
                        Tham gia
                      </button>
                    </div>
                  )}
                </div>
              ) : (
                <div
                  className={`cursor-pointer space-y-1 ${isPast ? '' : ''}`}
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
                  {session.teacherName && (
                    <p className="flex items-center gap-1 text-[10px] text-[#8b706e]">
                      <User className="h-3 w-3" /> {session.teacherName}
                    </p>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function SessionDetailContent({ session, larkMessage, onLark }) {
  const isVirtual = session.deliveryMode === 'VIRTUAL';

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
          <StatusBadge status={session.status} />
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
                ? 'Lớp học trực tuyến'
                : `${session.roomName || 'Đang xếp phòng'} · ${session.campusName || 'Cơ sở Hà Nội'}`}
            </span>
          </div>
        </div>
      </div>

      {/* Teacher */}
      {session.teacherName && (
        <div className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-4">
          <div className="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-[#fff1f3] font-['Manrope'] font-extrabold text-[#730014]">
            {session.teacherName.charAt(0).toUpperCase()}
          </div>
          <div>
            <p className="text-[10px] font-bold uppercase tracking-wider text-[#8b706e]">Giảng viên phụ trách</p>
            <p className="mt-0.5 font-extrabold text-[#2b2828]">{session.teacherName}</p>
          </div>
        </div>
      )}

      {/* Lark block */}
      {isVirtual && (
        <div className="rounded-2xl border border-purple-100 bg-purple-50/10 p-5 space-y-3">
          <h4 className="flex items-center gap-1 text-xs font-bold uppercase tracking-wider text-purple-700">
            <Video className="h-3.5 w-3.5" /> Phòng học Lark
          </h4>
          {session.larkMeetingUrl ? (
            <div className="space-y-3">
              <LarkJoinButton className="!bg-purple-700 hover:!bg-purple-800" onBlocked={onLark} url={session.larkMeetingUrl} />
            </div>
          ) : (
            <p className="text-xs text-[#8b706e]">Link phòng học Lark sẽ được cập nhật trước giờ học.</p>
          )}
          {larkMessage && <p className="text-xs font-semibold text-rose-700">{larkMessage}</p>}
        </div>
      )}

      {/* Recording */}
      {session.recordingUrl && (
        <div className="rounded-2xl border border-emerald-100 bg-emerald-50/10 p-5 space-y-3">
          <h4 className="flex items-center gap-1 text-xs font-bold uppercase tracking-wider text-emerald-700">
            <Play className="h-3.5 w-3.5" /> Video ghi âm
          </h4>
          <a
            className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-700 px-5 py-2.5 text-xs font-extrabold text-white shadow-sm transition hover:bg-emerald-800 active:scale-95"
            href={session.recordingUrl}
            rel="noreferrer"
            target="_blank"
          >
            Xem bài giảng <ExternalLink className="h-3.5 w-3.5" />
          </a>
        </div>
      )}

      {/* CTA */}
      <div className="border-t border-gray-100 pt-4">
        <Link
          className="flex items-center justify-center gap-1.5 rounded-2xl bg-[#4b0009] py-3.5 text-sm font-extrabold text-white shadow-sm transition hover:bg-[#730014] active:scale-95"
          to={`/my-classrooms/${session.classroomId}`}
        >
          Vào lớp học <CheckCircle2 className="h-4 w-4" />
        </Link>
      </div>
    </div>
  );
}
