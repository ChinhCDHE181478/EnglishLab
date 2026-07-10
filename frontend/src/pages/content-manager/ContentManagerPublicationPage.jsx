import { useEffect, useMemo, useState } from 'react';
import { Archive, CheckCircle2, Pencil, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { ContentManagerLoadingState, Panel, StatusBadge } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';

const pageSize = 10;

export default function ContentManagerPublicationPage() {
  const [courses, setCourses] = useState([]);
  const [status, setStatus] = useState('ALL');
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [workingId, setWorkingId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadCourses = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await courseApi.getManagedOnlineCourses({ page: 0, size: 500 });
      setCourses(response.content || []);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được hàng chờ xuất bản.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCourses();
  }, []);

  const filtered = useMemo(
    () => courses.filter((course) => status === 'ALL' || course.status === status),
    [courses, status],
  );
  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const rows = filtered.slice((page - 1) * pageSize, page * pageSize);

  useEffect(() => {
    setPage(1);
  }, [status]);

  const changeStatus = async (course, action) => {
    setWorkingId(course.id);
    setError('');
    setSuccess('');
    try {
      let updated = course;
      if (action === 'SUBMIT_REVIEW') {
        updated = await courseApi.submitOnlineCourseForReview(course.id);
        setSuccess('Khóa học đã được gửi duyệt.');
      } else if (action === 'PUBLISHED') {
        updated = await courseApi.publishOnlineCourse(course.id);
        setSuccess('Khóa học đã được xuất bản.');
      } else {
        updated = await courseApi.archiveOnlineCourse(course.id);
        setSuccess('Khóa học đã được chuyển vào lưu trữ.');
      }
      setCourses((current) => current.map((item) => (item.id === updated.id ? updated : item)));
    } catch (err) {
      setError(err?.response?.data?.message || 'Không cập nhật được trạng thái xuất bản.');
    } finally {
      setWorkingId(null);
    }
  };

  if (loading && !courses.length) {
    return <ContentManagerLoadingState message="Đang tải hàng chờ xuất bản..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="w-full max-w-sm">
          <span className="mb-2 block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">Trạng thái</span>
          <BrandedSelect
            onChange={(event) => setStatus(event.target.value)}
            options={[
              { label: 'Tất cả trạng thái', value: 'ALL' },
              { label: 'Bản nháp', value: 'DRAFT' },
              { label: 'Chờ duyệt', value: 'PENDING_REVIEW' },
              { label: 'Bị từ chối', value: 'REJECTED' },
              { label: 'Đã xuất bản', value: 'PUBLISHED' },
              { label: 'Lưu trữ', value: 'ARCHIVED' },
            ]}
            value={status}
          />
        </div>
        <button className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm font-bold text-[#730014]" onClick={loadCourses} type="button">
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </button>
      </div>

      {error ? <Notice tone="error">{error}</Notice> : null}
      {success ? <Notice tone="success">{success}</Notice> : null}

      <Panel className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full text-left">
            <thead className="bg-[#fbf3f4] text-xs uppercase tracking-[0.16em] text-[#8e7371]">
              <tr>
                {['Khóa học', 'Danh mục', 'Cấu trúc', 'Trạng thái', 'Cập nhật', 'Thao tác'].map((heading) => (
                  <th key={heading} className="px-5 py-4 font-semibold">{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#f0e3e4]">
              {rows.length ? rows.map((course) => (
                <tr key={course.id}>
                  <td className="px-5 py-4">
                    <p className="font-bold text-[#1a1c1c]">{course.title}</p>
                    <p className="mt-1 text-xs text-[#8b706e]">{course.slug}</p>
                  </td>
                  <td className="px-5 py-4 text-sm">{course.categoryName || course.category}</td>
                  <td className="px-5 py-4 text-sm">{course.modules?.length || 0} mô-đun · {course.totalLessons || 0} bài học</td>
                  <td className="px-5 py-4"><StatusBadge label={course.status} /></td>
                  <td className="px-5 py-4 text-sm">{formatDate(course.updatedAt)}</td>
                  <td className="px-5 py-4">
                    <div className="flex flex-wrap gap-2">
                      <Link className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]" to={`/content-manager/courses/${course.slug}/edit`}>
                        <Pencil className="h-4 w-4" />
                        Rà soát
                      </Link>
                      {(course.status === 'DRAFT' || course.status === 'REJECTED') ? (
                        <button className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-3 py-2 text-sm font-semibold text-white disabled:opacity-50" disabled={workingId === course.id} onClick={() => changeStatus(course, 'SUBMIT_REVIEW')} type="button">
                          <CheckCircle2 className="h-4 w-4" />
                          Gửi duyệt
                        </button>
                      ) : null}
                      {course.status === 'PENDING_REVIEW' ? (
                        <span className="inline-flex items-center rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm font-semibold text-amber-800">
                          Đang chờ Manager duyệt
                        </span>
                      ) : null}
                      {course.status === 'PUBLISHED' ? (
                        <button className="inline-flex items-center gap-2 rounded-xl border border-rose-200 px-3 py-2 text-sm font-semibold text-rose-700 disabled:opacity-50" disabled={workingId === course.id} onClick={() => changeStatus(course, 'ARCHIVED')} type="button">
                          <Archive className="h-4 w-4" />
                          Lưu trữ
                        </button>
                      ) : null}
                    </div>
                  </td>
                </tr>
              )) : (
                <tr><td className="px-5 py-10 text-sm text-[#584140]" colSpan={6}>Không có khóa học nào ở trạng thái này.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>

      {totalPages > 1 ? (
        <div className="flex items-center justify-center gap-3">
          <button className="rounded-xl border border-[#dfbfbd] bg-white px-4 py-2 text-sm font-bold text-[#730014] disabled:opacity-40" disabled={page <= 1} onClick={() => setPage((current) => current - 1)} type="button">Trang trước</button>
          <span className="text-sm font-semibold text-[#584140]">Trang {page} / {totalPages}</span>
          <button className="rounded-xl border border-[#dfbfbd] bg-white px-4 py-2 text-sm font-bold text-[#730014] disabled:opacity-40" disabled={page >= totalPages} onClick={() => setPage((current) => current + 1)} type="button">Trang sau</button>
        </div>
      ) : null}
    </div>
  );
}

function formatDate(value) {
  if (!value) return 'Chưa có';
  return new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function Notice({ children, tone }) {
  const className = tone === 'error'
    ? 'border-[#ba1a1a]/20 bg-[#ffdad6] text-[#93000a]'
    : 'border-emerald-200 bg-emerald-50 text-emerald-700';
  return <div className={`rounded-2xl border px-5 py-4 text-sm font-semibold ${className}`}>{children}</div>;
}
