import { useEffect, useMemo, useState } from 'react';
import { Archive, CheckCircle2, Pencil, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { ContentManagerLoadingState, Panel, StatusBadge } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import { useAppDialog } from '../../components/ui/AppDialog';
import { getCourseVersionLabel } from '../../utils/courseVersionUi';

const pageSize = 10;

export default function ContentManagerPublicationPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const [courses, setCourses] = useState([]);
  const [status, setStatus] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [workingId, setWorkingId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadCourses = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await courseApi.getManagedOnlineCourses({ page: 0, size: 500 });
      const failedVersionLoads = [];
      const rows = await Promise.all((response.content || []).map(async (course) => {
        try {
          const versions = await courseApi.getOnlineCourseVersions(course.id);
          const publishableVersion = versions.find((version) => version.status === 'DRAFT')
            || versions.find((version) => version.status === 'PENDING_REVIEW')
            || null;
          return { ...course, versions, publishableVersion };
        } catch {
          failedVersionLoads.push(course.title);
          return { ...course, versions: [], publishableVersion: null };
        }
      }));
      setCourses(rows);
      if (failedVersionLoads.length) {
        setError(`Chưa tải được phiên bản của ${failedVersionLoads.length} khóa học. Hãy bấm Làm mới để thử lại.`);
      }
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
    () => courses.filter((course) => status === 'ALL' || resolvePublicationStatus(course) === status),
    [courses, status],
  );
  const { page, setPage, totalPages, pageItems: rows, totalItems } = usePagination(filtered, pageSize, status);

  const changeStatus = async (course, action) => {
    setWorkingId(course.id);
    setError('');
    setSuccess('');
    try {
      let updated = course;
      if (action === 'PUBLISH') {
        if (!course.publishableVersion) throw new Error('Khóa học chưa có phiên bản nháp để xuất bản.');
        if (!await confirmDialog(
          `Xuất bản v${course.publishableVersion.versionNumber} của khóa học “${course.title}”?\n\nMọi học viên sẽ nhận nội dung phiên bản mới nhất; các bài đã hoàn thành vẫn được giữ nguyên.`,
          {
            title: `Xuất bản phiên bản v${course.publishableVersion.versionNumber}`,
            confirmLabel: 'Xuất bản',
          },
        )) return;
        await courseApi.publishOnlineCourseVersion(course.id, course.publishableVersion.id);
        setSuccess(`Đã xuất bản phiên bản v${course.publishableVersion.versionNumber}.`);
      } else {
        updated = await courseApi.archiveOnlineCourse(course.id);
        setSuccess('Khóa học đã được chuyển vào lưu trữ.');
      }
      if (action === 'PUBLISH') await loadCourses();
      else setCourses((current) => current.map((item) => (item.id === updated.id ? { ...item, ...updated } : item)));
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
              { label: 'Có bản nháp chờ xuất bản', value: 'DRAFT' },
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
                  <td className="px-5 py-4">
                    {course.publishableVersion
                      ? <StatusBadge label={getCourseVersionLabel(course.publishableVersion)} />
                      : <StatusBadge label={course.status} />}
                  </td>
                  <td className="px-5 py-4 text-sm">{formatDate(course.updatedAt)}</td>
                  <td className="px-5 py-4">
                    <div className="flex flex-wrap gap-2">
                      <Link className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]" to={`/content-manager/courses/${course.slug}/edit`}>
                        <Pencil className="h-4 w-4" />
                        Rà soát
                      </Link>
                      {course.publishableVersion ? (
                        <button className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-3 py-2 text-sm font-semibold text-white disabled:opacity-50" disabled={workingId === course.id} onClick={() => changeStatus(course, 'PUBLISH')} type="button">
                          <CheckCircle2 className="h-4 w-4" />
                          Xuất bản v{course.publishableVersion.versionNumber}
                        </button>
                      ) : null}
                      {course.status === 'PUBLISHED' && !course.publishableVersion ? (
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

      <Pagination page={page} totalPages={totalPages} onChange={setPage} totalItems={totalItems} pageSize={pageSize} />
    </div>
  );
}

function resolvePublicationStatus(course) {
  return course.publishableVersion ? 'DRAFT' : course.status;
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
