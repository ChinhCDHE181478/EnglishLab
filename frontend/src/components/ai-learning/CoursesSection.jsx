import { useEffect, useMemo, useState } from 'react';
import courseApi from '../../api/courseApi';
import { PopularCourses } from '../course';
import { normalizeCourse } from '../../utils/courseModels';

const CoursesSection = () => {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    const loadCourses = async () => {
      try {
        const page = await courseApi.getOnlineCourses({ page: 0, size: 100 });
        if (active) setCourses((page.content || []).map(normalizeCourse));
      } catch {
        if (active) setCourses([]);
      } finally {
        if (active) setLoading(false);
      }
    };

    loadCourses();
    return () => {
      active = false;
    };
  }, []);

  const popularCourses = useMemo(() => {
    const featured = courses.filter((course) => course.featured);
    return (featured.length ? featured : courses).slice(0, 4);
  }, [courses]);

  return (
    <section id="courses" className="border-y border-[#dfbfbd]/30 bg-white py-20">
      <div className="mx-auto max-w-7xl px-4 md:px-10">
        {loading ? (
          <div className="py-12 text-center text-sm font-semibold text-[#8c716f]">Đang tải khóa học nổi bật...</div>
        ) : popularCourses.length ? (
          <PopularCourses allCoursesHref="/courses" courses={popularCourses} />
        ) : (
          <div className="py-12 text-center">
            <p className="text-sm font-semibold text-[#8c716f]">Chưa có khóa học nổi bật.</p>
            <a className="mt-4 inline-block font-semibold text-[#730014] hover:underline" href="/courses">
              Xem thư viện khóa học
            </a>
          </div>
        )}
      </div>
    </section>
  );
};

export default CoursesSection;
