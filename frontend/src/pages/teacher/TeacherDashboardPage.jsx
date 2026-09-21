import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  BookOpen,
  Calendar,
  Clock,
  MapPin,
  Video,
  Users,
  FileText,
  HelpCircle,
  ExternalLink,
  ChevronRight,
  CalendarDays,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import Header from '../../components/ai-learning/Header';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  PageHero,
  ClassroomTypeBadge,
  StatusBadge,
} from '../../components/classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDate, formatClassroomTime } from '../../utils/classroomHelpers';
import { PAGE_BODY_CLASS, PAGE_HEADER_CLASS, PAGE_MAIN_STACK_CLASS, PAGE_SHELL_CLASS } from '../../utils/pageLayout';
import Pagination, { usePagination } from '../../components/ui/Pagination';

const toLocalDateStr = (d) => {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
};

export default function TeacherDashboardPage() {
  const [classrooms, setClassrooms] = useState([]);
  const [todaySessions, setTodaySessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const { page, setPage, totalPages, pageItems: paginatedClassrooms, totalItems } = usePagination(classrooms, 4);

  const loadClassrooms = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getTeacherAssignedClassrooms();
      setClassrooms(data);

      // Fetch today's sessions across all assigned classrooms (best-effort)
      const todayStr = toLocalDateStr(new Date());
      const results = await Promise.allSettled(
        data.map(async (cls) => {
          const items = await classroomApi.getTeacherClassroomSessions(cls.id);
          return items
            .filter((s) => s.sessionDate === todayStr)
            .map((s) => ({
              ...s,
              classroomId: cls.id,
              classroomTitle: cls.title,
              deliveryMode: s.deliveryMode || cls.deliveryMode,
            }));
        }),
      );
      const merged = results
        .filter((r) => r.status === 'fulfilled')
        .flatMap((r) => r.value)
        .sort((a, b) => (a.startTime || '').localeCompare(b.startTime || ''));
      setTodaySessions(merged);
    } catch (err) {
      setClassrooms([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải danh sách lớp được phân công.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadClassrooms();
  }, []);

  // Calculate statistics for PageHero
  const stats = useMemo(() => {
    if (!classrooms.length) return [];
    const activeCount = classrooms.filter((c) => ['ACTIVE', 'IN_PROGRESS'].includes(c.classroomStatus)).length;
    const upcomingCount = classrooms.filter((c) => ['UPCOMING', 'DRAFT'].includes(c.classroomStatus)).length;
    const totalStudents = classrooms.reduce((acc, curr) => acc + (curr.enrolledCount || 0), 0);

    return [
      { label: 'Tổng số lớp', value: classrooms.length, icon: BookOpen, color: 'blue' },
      { label: 'Đang giảng dạy', value: activeCount, icon: Clock, color: 'rose' },
      { label: 'Sắp khai giảng', value: upcomingCount, icon: Calendar, color: 'amber' },
      { label: 'Tổng học viên', value: totalStudents, icon: Users, color: 'emerald' },
    ];
  }, [classrooms]);

  return (
    <div className={PAGE_SHELL_CLASS}>
      <CourseGlobalStyles />
      <div className={PAGE_HEADER_CLASS}>
        <Header />
      </div>
      <div className={PAGE_BODY_CLASS}>
      <motion.main
        className={PAGE_MAIN_STACK_CLASS}
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35, ease: 'easeOut' }}
      >
        {/* Page Hero with operational stats */}
        <PageHero
          title="Tổng quan giảng dạy"
          subtitle="Không gian làm việc dành riêng cho Giảng viên. Theo dõi lớp học được phân công, quản lý điểm danh, bài tập và gửi yêu cầu thay đổi."
          stats={stats}
        />

        {/* Today's teaching sessions */}
        {!loading && !error && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="font-['Manrope'] text-xl font-bold text-[#1a1c1c] flex items-center gap-2">
                <span className="h-5 w-1 rounded-full bg-[#8a0018]" />
                Buổi dạy hôm nay
              </h2>
            </div>

            {todaySessions.length ? (
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                {todaySessions.map((s, idx) => {
                  const isVirtual = s.deliveryMode === 'VIRTUAL';
                  return (
                    <motion.div
                      key={s.id}
                      initial={{ opacity: 0, y: 12 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.28, delay: Math.min(idx * 0.06, 0.3), ease: 'easeOut' }}
                      className="rounded-xl border border-[#e5e7eb] bg-white p-4 transition hover:border-[#dfbfbd] hover:shadow-sm"
                    >
                      <div className="flex items-center gap-2 text-sm font-extrabold text-[#8a0018]">
                        <Clock className="h-4 w-4" />
                        {formatClassroomTime(s.startTime)} – {formatClassroomTime(s.endTime)}
                      </div>
                      <h4 className="mt-2 font-['Manrope'] text-base font-extrabold text-[#2b2828] line-clamp-1">
                        {s.classroomTitle}
                      </h4>
                      <p className="mt-1 flex items-center gap-1.5 text-xs text-[#6a5553]">
                        {isVirtual
                          ? <><Video className="h-3.5 w-3.5 text-sky-600" /> Lớp trực tuyến</>
                          : <><MapPin className="h-3.5 w-3.5 text-[#730014]" /> {s.roomName || 'Đang xếp phòng'}</>}
                      </p>
                      <Link
                        className="mt-3 inline-flex w-full items-center justify-center gap-1.5 rounded-lg bg-[#4b0009] px-4 py-2 text-xs font-extrabold text-white transition hover:bg-[#730014] active:scale-95"
                        to={`/teacher/sessions/${s.id}`}
                      >
                        Vào điểm danh <ChevronRight className="h-3.5 w-3.5" />
                      </Link>
                    </motion.div>
                  );
                })}
              </div>
            ) : (
              <div className="flex items-center gap-3 rounded-xl border border-dashed border-[#e5e7eb] bg-white px-5 py-6 text-sm text-[#6a5553]">
                <CalendarDays className="h-5 w-5 text-[#9a8b8a]" />
                Hôm nay bạn không có buổi dạy nào. Hãy tận dụng thời gian chuẩn bị bài giảng.
              </div>
            )}
          </div>
        )}

        {/* Classrooms List Section */}
        <div className="space-y-6" id="teacher-assigned-classrooms">
          <div className="space-y-1">
            <h2 className="font-['Manrope'] text-xl font-bold text-[#1a1c1c] flex items-center gap-2">
              <span className="h-5 w-1 rounded-full bg-[#8a0018]" />
              Lớp học được phân công
            </h2>
            <p className="text-xs leading-5 text-[#8b706e]">
              Muốn giao bài tập cho lớp nào, bạn cứ bấm nhanh nút <strong>Giao bài tập</strong> ở lớp đó là được nhé!
            </p>
          </div>

          <section className="flex flex-1 flex-col">
            {loading ? <ClassroomLoadingState message="Đang tải danh sách lớp được phân công..." /> : null}
            {!loading && error ? <ClassroomErrorState message={error} onRetry={loadClassrooms} /> : null}
            {!loading && !error && !classrooms.length ? (
              <ClassroomEmptyState
                description="Bạn chưa được phân công giảng dạy lớp học nào trong hệ thống."
                title="Chưa có lớp giảng dạy"
                icon={HelpCircle}
              />
            ) : null}
            {!loading && !error && classrooms.length ? (
              <div className="space-y-6">
                <div className="grid gap-6 md:grid-cols-2">
                  {paginatedClassrooms.map((classroom, idx) => {
                    const isActive = ['ACTIVE', 'IN_PROGRESS'].includes(classroom.classroomStatus);
                    const isVirtual = classroom.deliveryMode === 'VIRTUAL';

                    return (
                      <motion.article
                        key={classroom.id}
                        initial={{ opacity: 0, y: 18 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.32, delay: Math.min(idx * 0.08, 0.4), ease: 'easeOut' }}
                        className="flex flex-col overflow-hidden rounded-xl border border-[#e5e7eb] bg-white shadow-sm transition-shadow hover:shadow-md"
                      >
                        {/* Card Header */}
                        <div className="border-b border-[#f0f0f0] p-5">
                          <div className="flex flex-wrap items-center justify-between gap-3">
                            <div className="flex flex-wrap gap-2">
                              <ClassroomTypeBadge mode={classroom.deliveryMode} />
                              <StatusBadge status={classroom.classroomStatus} />
                            </div>
                          </div>

                          <h3 className="mt-4 font-['Manrope'] text-2xl font-extrabold text-[#2b2828] line-clamp-1">
                            {classroom.title}
                          </h3>
                        </div>

                        {/* Card Body */}
                        <div className="flex-1 p-6 space-y-4">
                          <div className="grid gap-4 text-sm text-[#584140] sm:grid-cols-2">
                            <div className="flex items-center gap-2.5">
                              <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-rose-50 text-[#730014]">
                                <Users className="h-4 w-4" />
                              </div>
                              <span>
                                Sĩ số: <strong className="text-[#2b2828]">{classroom.enrolledCount ?? 0} học viên</strong>
                              </span>
                            </div>

                            <div className="flex items-center gap-2.5">
                              <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-rose-50 text-[#730014]">
                                <Calendar className="h-4 w-4" />
                              </div>
                              <span>
                                Khai giảng: <strong className="text-[#2b2828]">{formatClassroomDate(classroom.startDate)}</strong>
                              </span>
                            </div>
                          </div>

                          {isVirtual && classroom.googleMeetUrl && (
                            <div className="flex items-center justify-between rounded-2xl border border-sky-200 bg-sky-50/50 p-4 text-xs text-sky-800">
                              <span className="font-bold flex items-center gap-1">
                                <Video className="h-4 w-4 text-sky-700" /> Liên kết Google Meet
                              </span>
                              <a
                                className="font-extrabold text-purple-900 underline flex items-center gap-1"
                                href={classroom.googleMeetUrl}
                                rel="noreferrer"
                                target="_blank"
                              >
                                Mở Google Meet <ExternalLink className="h-3 w-3" />
                              </a>
                            </div>
                          )}

                          {!isVirtual && (
                            <div className="rounded-2xl bg-rose-50/20 border border-rose-100/40 p-4 text-xs text-[#584140] flex items-center gap-2">
                              <MapPin className="h-4 w-4 text-[#730014]" />
                              <span>Địa điểm: <strong>{classroom.roomName || 'Chưa xếp phòng'}</strong> · {classroom.offlineAddress || 'Cơ sở Hà Nội'}</span>
                            </div>
                          )}
                        </div>

                        {/* Card Footer */}
                        <div className="border-t border-gray-50 bg-gray-50/30 px-6 py-4 flex flex-wrap items-center justify-between gap-3">
                          <span className="text-xs font-bold text-[#8b706e]">
                            ID Lớp: #{classroom.id}
                          </span>

                          <div className="flex flex-wrap gap-2">
                            <Link
                              className="inline-flex items-center gap-1.5 rounded-xl border border-[#730014]/20 bg-white px-4 py-2.5 text-xs font-extrabold text-[#730014] transition hover:bg-[#fff3f4] active:scale-95"
                              to={`/teacher/classrooms/${classroom.id}?tab=homework&action=create`}
                            >
                              <FileText className="h-3.5 w-3.5" />
                              Giao bài tập
                            </Link>
                            <Link
                              className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-2.5 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014] hover:shadow active:scale-95"
                              to={`/teacher/classrooms/${classroom.id}`}
                            >
                              Quản lý lớp học
                              <ChevronRight className="h-3.5 w-3.5" />
                            </Link>
                          </div>
                        </div>
                      </motion.article>
                    );
                  })}
                </div>
                <div className="flex justify-center pt-4">
                  <Pagination
                    page={page}
                    totalPages={totalPages}
                    onChange={setPage}
                    totalItems={totalItems}
                    pageSize={4}
                  />
                </div>
              </div>
            ) : null}
          </section>
        </div>
      </motion.main>
      </div>
      <CourseFooter />
    </div>
  );
}
