import { useEffect, useMemo, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Award, BookOpen, Clock, Play, GraduationCap, ChevronRight, FileText } from 'lucide-react';
import CertificatePreview from '../components/course/CertificatePreview';
import BrandLoadingState from '../components/ui/BrandLoadingState';
import courseApi from '../api/courseApi';
import { getStoredUser, hasAccessToken } from '../utils/auth';
import { normalizeCourse, normalizeEnrollment } from '../utils/courseModels';
import { formatBandValue } from '../utils/selfPacedHelpers';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import Pagination, { usePagination } from '../components/ui/Pagination';

const tabs = [
  { id: 'all', label: 'Tất cả' },
  { id: 'not-started', label: 'Chưa bắt đầu' },
  { id: 'in-progress', label: 'Đang học' },
  { id: 'completed', label: 'Đã hoàn thành' },
];

const CertificateModal = ({ certificate, onClose }) => {
  if (!certificate) return null;

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(certificate.verificationCode || '');
    } catch {
      // Trình duyệt không hỗ trợ sao chép.
    }
  };

  return (
    <div className="fixed inset-0 z-[80] overflow-y-auto bg-[#220005]/45 px-4 py-10 flex items-center justify-center backdrop-blur-sm">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.95 }}
        className="mx-auto flex w-full max-w-[1160px] flex-col gap-6"
      >
        <div className="khong-in flex flex-wrap justify-end gap-3">
          <button className="rounded-xl bg-[#730014] px-5 py-3 text-xs font-extrabold text-white transition hover:bg-[#4b0009]" onClick={() => window.print()} type="button">
            Tải xuống
          </button>
          <button className="rounded-xl border border-gray-200 bg-white px-5 py-3 text-xs font-extrabold text-[#730014] transition hover:bg-gray-50" onClick={handleCopy} type="button">
            Sao chép mã xác thực
          </button>
          <button className="rounded-xl border border-gray-200 bg-white px-5 py-3 text-xs font-extrabold text-gray-700 transition hover:bg-gray-50" onClick={onClose} type="button">
            Đóng
          </button>
        </div>
        <CertificatePreview certificate={certificate} />
      </motion.div>
    </div>
  );
};

const containerVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.08
    }
  }
};

const itemVariants = {
  hidden: { opacity: 0, y: 15 },
  show: { opacity: 1, y: 0, transition: { duration: 0.35, ease: 'easeOut' } }
};

const MyCoursesPage = () => {
  const [activeTab, setActiveTab] = useState('all');
  const [user, setUser] = useState(() => (hasAccessToken() ? getStoredUser() : null));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [enrollments, setEnrollments] = useState([]);
  const [courseItems, setCourseItems] = useState([]);
  const [activeCertificate, setActiveCertificate] = useState(null);

  const isAuthenticated = Boolean(user && hasAccessToken());

  useEffect(() => {
    let active = true;
    if (!hasAccessToken()) {
      setUser(null);
      setLoading(false);
      return undefined;
    }

    const loadData = async () => {
      setLoading(true);
      setError('');
      try {
        const myEnrollments = (await courseApi.getMyOnlineCourses()).map(normalizeEnrollment);
        if (!active) return;
        setEnrollments(myEnrollments);

        const detailResults = await Promise.allSettled(
          myEnrollments.map(async (enrollment) => {
            const courseResponse = await courseApi.getOnlineCourse(enrollment.courseSlug || enrollment.courseId);
            const completionResponse = await courseApi.getCourseCompletion(enrollment.courseId);
            return {
              course: normalizeCourse({ ...courseResponse, registered: true, progressPercent: enrollment.progressPercent }),
              enrollment,
              completion: completionResponse,
            };
          }),
        );

        if (!active) return;

        const successfulItems = detailResults
          .filter((result) => result.status === 'fulfilled')
          .map((result) => result.value);

        setCourseItems(successfulItems);
        setUser(getStoredUser());
      } catch {
        if (!active) return;
        setCourseItems([]);
        setEnrollments([]);
        setError('Không thể tải tiến độ học tập. Vui lòng thử lại.');
      } finally {
        if (active) setLoading(false);
      }
    };

    loadData();
    return () => {
      active = false;
    };
  }, []);

  const filteredCourses = useMemo(() => courseItems.filter(({ course, completion }) => {
    const progress = Number(completion?.progressPercent ?? course.progressPercent ?? 0);
    const isCompleted = Boolean(completion?.eligibleForCertificate);

    if (activeTab === 'not-started') return progress <= 0;
    if (activeTab === 'in-progress') return progress > 0 && !isCompleted;
    if (activeTab === 'completed') return isCompleted;
    return true;
  }), [activeTab, courseItems]);

  const { page, setPage, totalPages, pageItems: paginatedCourses, totalItems } = usePagination(
    filteredCourses,
    5,
    `my-courses-${activeTab}`
  );

  const openCertificate = async (courseId) => {
    try {
      const certificate = await courseApi.getCourseCertificate(courseId);
      if (certificate?.eligible) {
        setActiveCertificate(certificate);
      }
    } catch {
      setError('Không thể tải chứng nhận hoàn thành. Vui lòng thử lại.');
    }
  };

  return (
    <LearnerPageShell
      title="Khóa học của tôi"
      description="Theo dõi tiến độ học, bài học gần nhất và chứng nhận hoàn thành trực tiếp từ hệ thống."
    >
      {!isAuthenticated ? (
        <section className="rounded-[28px] border border-dashed border-gray-200 bg-white px-6 py-16 text-center shadow-[0_10px_35px_rgba(0,0,0,0.01)]">
          <h2 className="font-['Manrope'] text-lg font-extrabold text-[#1a1c1c]">Bạn cần đăng nhập để xem khóa học của mình.</h2>
          <div className="mt-6">
            <RouterLink className="inline-flex rounded-xl bg-gradient-to-r from-[#730014] to-[#4b0009] px-6 py-3.5 text-xs font-bold text-white shadow-sm transition hover:shadow active:scale-95 btn-hover" to="/login" state={{ from: '/my-courses' }}>
              Đăng nhập tài khoản
            </RouterLink>
          </div>
        </section>
      ) : loading ? (
        <BrandLoadingState className="rounded-[28px]" message="Đang tải dữ liệu khóa học của bạn..." />
      ) : error ? (
        <section className="rounded-[28px] border border-rose-100 bg-[#fff5f5]/30 px-6 py-16 text-center text-xs font-bold text-rose-800">
          {error}
        </section>
      ) : !courseItems.length ? (
        <section className="rounded-[28px] border border-dashed border-gray-200 bg-white px-6 py-16 text-center shadow-[0_10px_35px_rgba(0,0,0,0.01)]">
          <h2 className="font-['Manrope'] text-lg font-extrabold text-[#1a1c1c]">Bạn chưa đăng ký khóa học online nào.</h2>
          <p className="text-xs text-gray-500 mt-2">Hãy khám phá thư viện khóa học phong phú của EnglishLab ngay hôm nay.</p>
          <div className="mt-6">
            <RouterLink className="inline-flex rounded-xl bg-[#730014] px-6 py-3.5 text-xs font-bold text-white transition hover:bg-[#4b0009] active:scale-95" to="/courses">
              Khám phá khóa học
            </RouterLink>
          </div>
        </section>
      ) : (
        <div className="space-y-6">
          {/* Animated Tab Bar */}
          <div className="flex flex-wrap gap-2 border-b border-gray-200 pb-3">
            {tabs.map((tab) => {
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  className={`relative rounded-xl px-4 py-2.5 text-xs font-extrabold tracking-wide transition-all duration-300 ${
                    isActive
                      ? 'bg-gradient-to-r from-[#730014] to-[#4b0009] text-white shadow-md shadow-[#4b0009]/20 scale-[1.02]'
                      : 'bg-white text-[#584140] hover:bg-[#fff0f1] hover:text-[#730014] border border-gray-200'
                  }`}
                  onClick={() => setActiveTab(tab.id)}
                  type="button"
                >
                  {tab.label}
                </button>
              );
            })}
          </div>

          {/* Staggered Course Cards Listing */}
          <AnimatePresence mode="wait">
            {filteredCourses.length ? (
              <motion.section
                key={activeTab}
                variants={containerVariants}
                initial="hidden"
                animate="show"
                className="grid gap-5"
              >
                {paginatedCourses.map(({ course, enrollment, completion }) => (
                  <motion.article
                    key={course.id}
                    variants={itemVariants}
                    className="rounded-[28px] border border-gray-200/80 bg-white p-5 shadow-[0_10px_25px_rgba(0,0,0,0.01)] hover:shadow-[0_20px_50px_rgba(115,0,20,0.04)] hover:border-[#730014]/20 transition duration-300 group"
                  >
                    <div className="flex flex-col gap-6 lg:flex-row lg:items-center">
                      <div className="relative h-36 w-full rounded-2xl overflow-hidden lg:w-56 shrink-0 shadow-inner bg-gray-100">
                        <img
                          alt={course.title}
                          className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                          src={course.thumbnailUrl}
                        />
                        <div className="absolute inset-0 bg-gradient-to-t from-[#1a1c1c]/25 to-transparent" />
                      </div>

                      <div className="flex-1 min-w-0 space-y-4">
                        <div className="flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
                          <div className="space-y-1">
                            <h2 className="font-['Manrope'] text-base font-extrabold text-[#1a1c1c] leading-snug group-hover:text-[#730014] transition-colors duration-300">
                              {course.title}
                            </h2>
                            <p className="text-xs leading-relaxed text-[#584140] font-medium line-clamp-2">
                              {course.targetOutcome || 'Đang cập nhật mục tiêu đầu ra.'}
                            </p>
                          </div>
                          
                          <span className="inline-flex rounded-full bg-[#fff0f1] px-2.5 py-0.5 text-[9px] font-extrabold uppercase tracking-widest text-[#730014] border border-[#dfbfbd]/35 shrink-0 self-start md:self-auto">
                            {completion?.statusReason || completion?.statusLabel || 'Đang cập nhật tiến độ học'}
                          </span>
                        </div>

                        {/* Progress Grid */}
                        <div className="grid gap-3 grid-cols-2 md:grid-cols-4">
                          <KpiMiniCell label="Tiến độ học" value={`${completion?.progressPercent ?? enrollment.progressPercent ?? 0}%`} />
                          <KpiMiniCell label="Bài hoàn thành" value={`${completion?.completedLessons ?? 0} / ${completion?.totalLessons ?? 0}`} />
                          <KpiMiniCell label="Đánh giá đạt" value={`${completion?.completedAssessments ?? completion?.passedAssessments ?? 0} / ${completion?.totalAssessments ?? 0}`} />
                          <KpiMiniCell label="Band mục tiêu" value={course.targetBand ? `Band ${formatBandValue(course.targetBand)}` : 'Đang cập nhật'} />
                        </div>

                        {/* Actions buttons */}
                        <div className="flex flex-wrap gap-2.5 pt-2 border-t border-gray-50 items-center justify-between">
                          <RouterLink
                            className="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-[#730014] to-[#4b0009] px-5 py-2.5 text-xs font-bold text-white shadow-sm transition hover:shadow active:scale-95 btn-hover"
                            to={`/courses/${course.slug}/home`}
                            state={{ course, enrollment }}
                          >
                            <Play className="h-3.5 w-3.5" />
                            {Number(completion?.progressPercent ?? 0) > 0 ? 'Tiếp tục học' : 'Bắt đầu học'}
                          </RouterLink>
                          
                          {completion?.eligibleForCertificate ? (
                            <button
                              className="inline-flex items-center gap-1.5 rounded-xl border border-emerald-200 bg-emerald-50/20 px-5 py-2.5 text-xs font-bold text-emerald-800 transition hover:bg-emerald-50 active:scale-95"
                              onClick={() => openCertificate(course.id)}
                              type="button"
                            >
                              <Award className="h-4 w-4" />
                              Nhận chứng nhận hoàn thành
                            </button>
                          ) : null}
                        </div>
                      </div>
                    </div>
                  </motion.article>
                ))}

                {filteredCourses.length > 5 && (
                  <div className="mt-4 flex justify-end">
                    <Pagination
                      page={page}
                      onChange={setPage}
                      totalItems={totalItems}
                      pageSize={5}
                    />
                  </div>
                )}
              </motion.section>
            ) : (
              <motion.section
                key="empty"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                className="rounded-[28px] border border-dashed border-gray-200 bg-white px-6 py-16 text-center text-xs font-bold text-[#8b706e] shadow-[0_10px_35px_rgba(0,0,0,0.01)]"
              >
                Không có khóa học nào phù hợp với bộ lọc hiện tại.
              </motion.section>
            )}
          </AnimatePresence>
        </div>
      )}
      <AnimatePresence>
        {activeCertificate && (
          <CertificateModal certificate={activeCertificate} onClose={() => setActiveCertificate(null)} />
        )}
      </AnimatePresence>
    </LearnerPageShell>
  );
};

// ─── Local helper KpiMiniCell ───────────────────────────────────────────────
function KpiMiniCell({ label, value }) {
  return (
    <div className="rounded-xl bg-[#fffafb]/60 border border-gray-150 px-4 py-2.5 text-xs">
      <p className="text-[9px] font-extrabold uppercase tracking-wider text-[#8b706e] leading-none mb-1">{label}</p>
      <strong className="text-[#1a1c1c] font-extrabold leading-none">{value}</strong>
    </div>
  );
}

export default MyCoursesPage;
