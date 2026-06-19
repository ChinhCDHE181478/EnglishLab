import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  BookOpen,
  Calendar,
  Clock,
  MapPin,
  Video,
  Users,
  Award,
  Plus,
  ArrowRight,
  ClipboardCheck,
  FileText,
  AlertCircle,
  HelpCircle,
  ExternalLink,
  ChevronRight,
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
import { formatClassroomDate, formatDeliveryMode, formatOfferingStatus } from '../../utils/classroomHelpers';

export default function TeacherDashboardPage() {
  const [classrooms, setClassrooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadClassrooms = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getTeacherAssignedClassrooms();
      setClassrooms(data);
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
    <div className="course-page flex min-h-[100dvh] flex-col bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />
      <main className="mx-auto flex w-full max-w-[1320px] flex-1 flex-col px-4 pb-[80px] pt-8 md:px-10 space-y-8">
        {/* Page Hero with operational stats */}
        <PageHero
          title="Tổng quan giảng dạy"
          subtitle="Không gian làm việc dành riêng cho Giảng viên. Theo dõi lớp học được phân công, quản lý điểm danh, bài tập và gửi yêu cầu thay đổi lịch học."
          stats={stats}
          action={
            <div className="flex flex-wrap gap-3">
              <Link
                className="inline-flex items-center gap-1.5 rounded-2xl bg-[#4b0009] px-6 py-3.5 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow-lg active:scale-95"
                to="/teacher/requests"
              >
                Yêu cầu thay đổi lịch của tôi
                <ArrowRight className="h-4 w-4" />
              </Link>
            </div>
          }
        />

        {/* Cockpit Quick Actions */}
        <div className="grid gap-6 md:grid-cols-3">
          <div className="rounded-3xl border border-gray-100 bg-white p-6 shadow-sm flex items-start gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-[#730014] flex-shrink-0">
              <ClipboardCheck className="h-6 w-6" />
            </div>
            <div>
              <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Điểm danh nhanh</h4>
              <p className="mt-1 text-xs text-[#8b706e] leading-5">Ghi nhận chuyên cần của học viên trực tiếp theo từng buổi học tại phòng học offline hoặc Lark.</p>
            </div>
          </div>

          <div className="rounded-3xl border border-gray-100 bg-white p-6 shadow-sm flex items-start gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-50 text-blue-700 flex-shrink-0">
              <FileText className="h-6 w-6" />
            </div>
            <div>
              <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Giao bài tập mới</h4>
              <p className="mt-1 text-xs text-[#8b706e] leading-5">Tạo bài tập viết, bài đọc hoặc bài tập thực hành kèm hướng dẫn chi tiết và thời hạn nộp bài.</p>
            </div>
          </div>

          <div className="rounded-3xl border border-gray-100 bg-white p-6 shadow-sm flex items-start gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-50 text-amber-700 flex-shrink-0">
              <AlertCircle className="h-6 w-6" />
            </div>
            <div>
              <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Yêu cầu đổi lịch</h4>
              <p className="mt-1 text-xs text-[#8b706e] leading-5">Gửi đề xuất đổi lịch học, đổi phòng học hoặc đổi link Lark trực tiếp tới Training Manager.</p>
            </div>
          </div>
        </div>

        {/* Classrooms List Section */}
        <div className="space-y-6">
          <h2 className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828] flex items-center gap-2">
            <BookOpen className="h-6 w-6 text-[#730014]" />
            Lớp học được phân công
          </h2>

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
              <div className="grid gap-6 md:grid-cols-2">
                {classrooms.map((classroom) => {
                  const isActive = ['ACTIVE', 'IN_PROGRESS'].includes(classroom.classroomStatus);
                  const isVirtual = classroom.deliveryMode === 'VIRTUAL';

                  return (
                    <article
                      key={classroom.id}
                      className="flex flex-col overflow-hidden rounded-[28px] border border-[#dfbfbd]/20 bg-white shadow-sm transition-all duration-300 hover:translate-y-[-4px] hover:shadow-md"
                    >
                      {/* Card Header */}
                      <div className="border-b border-[#dfbfbd]/10 bg-gradient-to-r from-[#fffafb] to-white p-6">
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
                              Sĩ số: <strong className="text-[#2b2828]">{classroom.enrolledCount ?? 0} / {classroom.maxCapacity ?? '—'} học viên</strong>
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

                        {isVirtual && classroom.larkMeetingUrl && (
                          <div className="rounded-2xl bg-purple-50/40 border border-purple-100 p-4 text-xs text-purple-800 flex items-center justify-between">
                            <span className="font-bold flex items-center gap-1">
                              <Video className="h-4 w-4 text-purple-700" /> Lark Meeting Link
                            </span>
                            <a
                              className="font-extrabold text-purple-900 underline flex items-center gap-1"
                              href={classroom.larkMeetingUrl}
                              rel="noreferrer"
                              target="_blank"
                            >
                              Mở Lark <ExternalLink className="h-3 w-3" />
                            </a>
                          </div>
                        )}

                        {!isVirtual && (
                          <div className="rounded-2xl bg-rose-50/20 border border-rose-100/40 p-4 text-xs text-[#584140] flex items-center gap-2">
                            <MapPin className="h-4 w-4 text-[#730014]" />
                            <span>Địa điểm: <strong>{classroom.roomName || 'Chưa xếp phòng'}</strong> · {classroom.campusName || 'Cơ sở Hà Nội'}</span>
                          </div>
                        )}
                      </div>

                      {/* Card Footer */}
                      <div className="border-t border-gray-50 bg-gray-50/30 px-6 py-4 flex items-center justify-between">
                        <span className="text-xs font-bold text-[#8b706e]">
                          ID Lớp: #{classroom.id}
                        </span>

                        <Link
                          className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-2.5 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014] hover:shadow active:scale-95"
                          to={`/teacher/classrooms/${classroom.id}`}
                        >
                          Quản lý lớp học
                          <ChevronRight className="h-3.5 w-3.5" />
                        </Link>
                      </div>
                    </article>
                  );
                })}
              </div>
            ) : null}
          </section>
        </div>
      </main>
      <CourseFooter />
    </div>
  );
}
