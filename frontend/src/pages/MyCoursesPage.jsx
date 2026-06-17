import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import Header from '../components/ai-learning/Header';
import CertificatePreview from '../components/course/CertificatePreview';
import CourseFooter from '../components/course/CourseFooter';
import CourseGlobalStyles from '../components/course/CourseGlobalStyles';
import courseApi from '../api/courseApi';
import { getStoredUser, hasAccessToken } from '../utils/auth';
import { normalizeCourse, normalizeEnrollment } from '../utils/courseModels';
import { formatBandValue } from '../utils/selfPacedHelpers';

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
    <div className="fixed inset-0 z-[80] overflow-y-auto bg-[#220005]/45 px-4 py-10">
      <div className="mx-auto flex max-w-[1160px] flex-col gap-6">
        <div className="khong-in flex flex-wrap justify-end gap-3">
          <button className="rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white" onClick={() => window.print()} type="button">
            Tải xuống
          </button>
          <button className="rounded-2xl border border-[#dfbfbd]/30 bg-white px-5 py-3 text-sm font-extrabold text-[#4b0009]" onClick={handleCopy} type="button">
            Sao chép mã xác thực
          </button>
          <button className="rounded-2xl border border-[#dfbfbd]/30 bg-white px-5 py-3 text-sm font-extrabold text-[#4b0009]" onClick={onClose} type="button">
            Đóng
          </button>
        </div>
        <CertificatePreview certificate={certificate} />
      </div>
    </div>
  );
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
    <div className="course-page min-h-screen bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />
      <main className="mx-auto max-w-[1320px] px-4 pb-[80px] pt-8 md:px-10">
        <section className="rounded-[32px] border border-[#dfbfbd]/30 bg-white p-8 shadow-sm">
          <h1 className="font-['Manrope'] text-4xl font-extrabold text-[#2b2828]">Khóa học của tôi</h1>
          <p className="mt-3 max-w-3xl text-sm leading-7 text-[#584140]">
            Theo dõi tiến độ học, bài học gần nhất và chứng nhận hoàn thành lấy trực tiếp từ máy chủ.
          </p>
        </section>

        {!isAuthenticated ? (
          <section className="mt-8 rounded-[28px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center">
            <h2 className="font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Bạn cần đăng nhập để xem khóa học của mình.</h2>
            <div className="mt-6">
              <Link className="rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white transition hover:bg-[#730014]" to="/login" state={{ from: '/my-courses' }}>
                Đăng nhập
              </Link>
            </div>
          </section>
        ) : loading ? (
          <section className="mt-8 rounded-[28px] border border-[#dfbfbd]/25 bg-white px-6 py-16 text-center text-[#584140]">
            Đang tải dữ liệu khóa học của bạn...
          </section>
        ) : error ? (
          <section className="mt-8 rounded-[28px] border border-[#f0d4d7] bg-white px-6 py-16 text-center text-[#93000a]">
            {error}
          </section>
        ) : !courseItems.length ? (
          <section className="mt-8 rounded-[28px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center">
            <h2 className="font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Bạn chưa đăng ký khóa học nào.</h2>
            <div className="mt-6">
              <Link className="rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white transition hover:bg-[#730014]" to="/courses">
                Xem khóa học
              </Link>
            </div>
          </section>
        ) : (
          <>
            <section className="mt-8 flex flex-wrap gap-3">
              {tabs.map((tab) => (
                <button key={tab.id} className={`rounded-full px-4 py-3 text-sm font-extrabold ${activeTab === tab.id ? 'bg-[#4b0009] text-white' : 'bg-white text-[#584140]'}`} onClick={() => setActiveTab(tab.id)} type="button">
                  {tab.label}
                </button>
              ))}
            </section>

            {filteredCourses.length ? (
              <section className="mt-8 grid gap-6">
                {filteredCourses.map(({ course, enrollment, completion }) => (
                    <article key={course.id} className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-6 shadow-sm">
                      <div className="flex flex-col gap-5 lg:flex-row lg:items-center">
                        <img alt={course.title} className="h-36 w-full rounded-3xl object-cover lg:w-56" src={course.thumbnailUrl} />
                        <div className="flex-1">
                          <div className="flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
                            <div>
                              <h2 className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{course.title}</h2>
                              <p className="mt-2 text-sm leading-7 text-[#584140]">{course.targetOutcome || 'Đang cập nhật mục tiêu đầu ra.'}</p>
                            </div>
                            <span className="rounded-full bg-[#fff1f3] px-3 py-2 text-xs font-extrabold text-[#730014]">
                              {completion?.statusReason || completion?.statusLabel || 'Đang cập nhật tiến độ học'}
                            </span>
                          </div>

                          <div className="mt-4 grid gap-3 md:grid-cols-4">
                            <div className="rounded-2xl bg-[#fcf8f8] px-4 py-3 text-sm text-[#584140]">Tiến độ học: <strong className="text-[#2b2828]">{completion?.progressPercent ?? enrollment.progressPercent ?? 0}%</strong></div>
                            <div className="rounded-2xl bg-[#fcf8f8] px-4 py-3 text-sm text-[#584140]">Bài đã hoàn thành: <strong className="text-[#2b2828]">{completion?.completedLessons ?? 0}/{completion?.totalLessons ?? 0}</strong></div>
                            <div className="rounded-2xl bg-[#fcf8f8] px-4 py-3 text-sm text-[#584140]">Bài đánh giá đạt: <strong className="text-[#2b2828]">{completion?.completedAssessments ?? completion?.passedAssessments ?? 0}/{completion?.totalAssessments ?? 0}</strong></div>
                            <div className="rounded-2xl bg-[#fcf8f8] px-4 py-3 text-sm text-[#584140]">Band mục tiêu: <strong className="text-[#2b2828]">{course.targetBand ? `Band ${formatBandValue(course.targetBand)}` : 'Đang cập nhật'}</strong></div>
                          </div>

                          <div className="mt-4 flex flex-wrap gap-3">
                            <Link className="rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014]" to={`/courses/${course.slug}/home`} state={{ course, enrollment }}>
                              {Number(completion?.progressPercent ?? 0) > 0 ? 'Tiếp tục học' : 'Bắt đầu học'}
                            </Link>
                            {completion?.eligibleForCertificate ? (
                              <button className="rounded-2xl border border-[#d7eadf] bg-[#f5fff8] px-5 py-3 text-sm font-extrabold text-[#1f6b3b]" onClick={() => openCertificate(course.id)} type="button">
                                Nhận chứng nhận
                              </button>
                            ) : null}
                          </div>
                        </div>
                      </div>
                    </article>
                ))}
              </section>
            ) : (
              <section className="mt-8 rounded-[28px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center text-[#584140]">
                Không có khóa học nào phù hợp với bộ lọc hiện tại.
              </section>
            )}

          </>
        )}
      </main>
      <CourseFooter />
      <CertificateModal certificate={activeCertificate} onClose={() => setActiveCertificate(null)} />
    </div>
  );
};

export default MyCoursesPage;
