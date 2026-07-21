import { useEffect, useMemo, useState } from 'react';
import { CheckCircle2, FileCheck2, GitBranch, RefreshCw, Search, ShieldCheck, XCircle } from 'lucide-react';
import courseApi from '../../api/courseApi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { getCourseVersionLabel } from '../../utils/courseVersionUi';

const statusOptions = [
  { label: 'Chờ duyệt', value: 'PENDING_REVIEW' },
  { label: 'Đã xuất bản', value: 'PUBLISHED' },
  { label: 'Bị từ chối', value: 'REJECTED' },
  { label: 'Tất cả', value: 'ALL' },
];

const statusMeta = {
  PENDING_REVIEW: { label: 'Chờ duyệt', className: 'bg-amber-50 text-amber-700 border-amber-100' },
  PUBLISHED: { label: 'Đã xuất bản', className: 'bg-emerald-50 text-emerald-700 border-emerald-100' },
  REJECTED: { label: 'Bị từ chối', className: 'bg-rose-50 text-rose-700 border-rose-100' },
  DRAFT: { label: 'Bản nháp', className: 'bg-slate-50 text-slate-600 border-slate-200' },
  ARCHIVED: { label: 'Lưu trữ', className: 'bg-slate-100 text-slate-500 border-slate-200' },
};

export default function ManagerCourseApprovalPage() {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [status, setStatus] = useState('PENDING_REVIEW');
  const [keyword, setKeyword] = useState('');
  const [modal, setModal] = useState(null);
  const [reviewNote, setReviewNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [pendingVersions, setPendingVersions] = useState([]);

  const loadCourses = async () => {
    setLoading(true);
    setError('');
    try {
      const [response, versionItems] = await Promise.all([
        courseApi.getManagedOnlineCourses({ page: 0, size: 500 }),
        courseApi.getPendingOnlineCourseVersions(),
      ]);
      setCourses(response.content || []);
      setPendingVersions(versionItems);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được hàng chờ duyệt khóa học.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCourses();
  }, []);

  const filteredCourses = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return courses
      .filter((course) => status === 'ALL' || course.status === status)
      .filter((course) => {
        if (!normalizedKeyword) return true;
        return [course.title, course.slug, course.categoryName, course.category]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(normalizedKeyword));
      })
      .sort((left, right) => new Date(right.updatedAt || 0) - new Date(left.updatedAt || 0));
  }, [courses, keyword, status]);

  const stats = useMemo(() => ({
    pending: courses.filter((course) => course.status === 'PENDING_REVIEW').length,
    published: courses.filter((course) => course.status === 'PUBLISHED').length,
    rejected: courses.filter((course) => course.status === 'REJECTED').length,
  }), [courses]);

  const openModal = (course, action) => {
    setModal({ course, action });
    setReviewNote(action === 'approve' ? 'Đã rà soát nội dung, đủ điều kiện xuất bản.' : '');
    setError('');
    setSuccess('');
  };

  const openVersionModal = (version, action) => {
    setModal({ course: version.content || {}, version, action });
    setReviewNote(action === 'approve' ? 'Đã rà soát nội dung phiên bản, đủ điều kiện áp dụng cho lượt ghi danh mới.' : '');
    setError('');
    setSuccess('');
  };

  const closeModal = () => {
    setModal(null);
    setReviewNote('');
  };

  const submitReview = async () => {
    if (!modal) return;
    if (modal.action === 'reject' && !reviewNote.trim()) {
      setError('Vui lòng nhập lý do từ chối để Content Manager biết cần sửa gì.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      if (modal.version) {
        if (modal.action === 'approve') {
          await courseApi.publishOnlineCourseVersion(modal.version.courseId, modal.version.id);
        } else {
          await courseApi.rejectOnlineCourseVersion(modal.version.courseId, modal.version.id, reviewNote.trim());
        }
        setPendingVersions((current) => current.filter((version) => version.id !== modal.version.id));
        setSuccess(modal.action === 'approve'
          ? `Đã xuất bản phiên bản v${modal.version.versionNumber}; học viên cũ vẫn giữ phiên bản trước.`
          : `Đã trả phiên bản v${modal.version.versionNumber} về bản nháp kèm phản hồi.`);
      } else {
        const updated = modal.action === 'approve'
          ? await courseApi.approveOnlineCourse(modal.course.id, reviewNote.trim())
          : await courseApi.rejectOnlineCourse(modal.course.id, reviewNote.trim());
        setCourses((current) => current.map((course) => (course.id === updated.id ? updated : course)));
        setSuccess(modal.action === 'approve' ? 'Đã duyệt và xuất bản khóa học.' : 'Đã từ chối khóa học và lưu ghi chú phản hồi.');
      }
      closeModal();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể xử lý duyệt khóa học.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <section className="overflow-hidden rounded-[28px] border border-slate-200 bg-gradient-to-br from-[#4b0009] via-[#730014] to-[#a6122a] p-6 text-white shadow-xl">
        <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr] lg:items-end">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-3 py-1 text-xs font-extrabold uppercase tracking-[0.16em] text-white/80">
              <ShieldCheck className="h-4 w-4" />
              Manager review gate
            </div>
            <h2 className="mt-5 font-['Manrope'] text-3xl font-black tracking-tight md:text-4xl">
              Duyệt khóa học và phiên bản trước khi lên kệ
            </h2>
            <p className="mt-3 max-w-3xl text-sm leading-7 text-white/75">
              Khóa học mới và các phiên bản cập nhật v2, v3 đều được gom tại đây để Manager không phải tìm ở nhiều màn hình.
            </p>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <HeroStat label="Chờ duyệt" value={stats.pending} />
            <HeroStat label="Đã xuất bản" value={stats.published} />
            <HeroStat label="Bị từ chối" value={stats.rejected} />
          </div>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="grid gap-3 lg:grid-cols-[1fr_260px_auto] lg:items-center">
          <label className="relative block">
            <Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 pl-11 pr-4 text-sm outline-none transition focus:border-[#730014] focus:bg-white"
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Tìm khóa học, slug hoặc danh mục..."
              value={keyword}
            />
          </label>
          <BrandedSelect
            onChange={(event) => setStatus(event.target.value)}
            options={statusOptions}
            value={status}
          />
          <button
            className="inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-extrabold text-[#730014] transition hover:bg-[#fff4f5]"
            onClick={loadCourses}
            type="button"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </button>
        </div>
      </section>

      {error ? <Notice tone="error">{error}</Notice> : null}
      {success ? <Notice tone="success">{success}</Notice> : null}

      <section className="overflow-hidden rounded-2xl border border-amber-200 bg-white shadow-sm">
        <div className="flex flex-col gap-3 border-b border-amber-100 bg-amber-50 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-amber-100 text-amber-700"><GitBranch className="h-5 w-5" /></span>
            <div>
              <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">Phiên bản cập nhật chờ duyệt</h3>
              <p className="text-xs font-semibold text-slate-500">v2, v3 xuất hiện sau khi Content Manager bấm gửi duyệt.</p>
            </div>
          </div>
          <span className="w-fit rounded-full bg-amber-600 px-3 py-1 text-xs font-extrabold text-white">{pendingVersions.length} phiên bản</span>
        </div>
        {loading ? (
          <div className="px-5 py-8 text-center text-sm font-semibold text-slate-500">Đang tải phiên bản chờ duyệt...</div>
        ) : !pendingVersions.length ? (
          <div className="px-5 py-8 text-center text-sm font-semibold text-slate-500">Không có phiên bản đã gửi duyệt. Bản nháp chưa gửi sẽ không xuất hiện trong hàng chờ Manager.</div>
        ) : (
          <div className="divide-y divide-slate-100">
            {pendingVersions.map((version) => (
              <article className="grid gap-4 p-5 lg:grid-cols-[1fr_auto] lg:items-center" key={version.id}>
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs font-extrabold text-amber-700">{getCourseVersionLabel(version)}</span>
                    <span className="text-xs font-semibold text-slate-400">Khóa học #{version.courseId}</span>
                  </div>
                  <h4 className="mt-2 font-['Manrope'] text-xl font-extrabold text-slate-900">{version.content?.title || 'Khóa học chưa có tiêu đề'}</h4>
                  <p className="mt-1 text-sm text-slate-500">{version.changeNote || 'Content Manager chưa ghi chú thay đổi.'}</p>
                  <p className="mt-2 text-xs font-bold text-slate-400">{version.totalRequiredLessons || 0} bài bắt buộc · {version.totalRequiredAssessments || 0} bài đánh giá</p>
                </div>
                <div className="flex flex-wrap gap-2 lg:justify-end">
                  <button className="inline-flex items-center gap-2 rounded-2xl bg-emerald-600 px-4 py-3 text-sm font-extrabold text-white transition hover:bg-emerald-700" onClick={() => openVersionModal(version, 'approve')} type="button">
                    <CheckCircle2 className="h-4 w-4" /> Duyệt và xuất bản
                  </button>
                  <button className="inline-flex items-center gap-2 rounded-2xl border border-rose-200 px-4 py-3 text-sm font-extrabold text-rose-700 transition hover:bg-rose-50" onClick={() => openVersionModal(version, 'reject')} type="button">
                    <XCircle className="h-4 w-4" /> Yêu cầu chỉnh sửa
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        {loading ? (
          <div className="flex min-h-[360px] items-center justify-center text-sm font-bold text-slate-500">
            Đang tải hàng chờ duyệt...
          </div>
        ) : !filteredCourses.length ? (
          <div className="flex min-h-[360px] flex-col items-center justify-center px-6 text-center">
            <FileCheck2 className="h-14 w-14 text-[#730014]" />
            <h3 className="mt-4 font-['Manrope'] text-2xl font-extrabold text-slate-900">Không có khóa học phù hợp</h3>
            <p className="mt-2 max-w-xl text-sm leading-7 text-slate-500">
              Hàng chờ đang sạch. Khi Content Manager gửi duyệt khóa học mới, khóa đó sẽ xuất hiện tại đây.
            </p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {filteredCourses.map((course) => (
              <article className="grid gap-4 p-5 transition hover:bg-slate-50 lg:grid-cols-[1.5fr_0.7fr_auto] lg:items-center" key={course.id}>
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusBadge status={course.status} />
                    <span className="text-xs font-semibold text-slate-400">#{course.id}</span>
                  </div>
                  <h3 className="mt-2 font-['Manrope'] text-xl font-extrabold text-slate-900">{course.title}</h3>
                  <p className="mt-1 text-sm text-slate-500">{course.slug}</p>
                  <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-600">
                    {course.shortDescription || course.targetOutcome || 'Khóa học chưa có mô tả ngắn.'}
                  </p>
                </div>
                <div className="grid grid-cols-3 gap-2 text-center lg:grid-cols-1 lg:text-left">
                  <MiniMetric label="Danh mục" value={course.categoryName || course.category || 'Chưa phân loại'} />
                  <MiniMetric label="Cấu trúc" value={`${course.modules?.length || 0} mô-đun · ${course.totalLessons || 0} bài`} />
                  <MiniMetric label="Cập nhật" value={formatDate(course.updatedAt)} />
                </div>
                <div className="flex flex-wrap justify-end gap-2">
                  <button
                    className="inline-flex items-center gap-2 rounded-2xl bg-emerald-600 px-4 py-3 text-sm font-extrabold text-white shadow-sm transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
                    disabled={course.status !== 'PENDING_REVIEW'}
                    onClick={() => openModal(course, 'approve')}
                    type="button"
                  >
                    <CheckCircle2 className="h-4 w-4" />
                    Duyệt
                  </button>
                  <button
                    className="inline-flex items-center gap-2 rounded-2xl border border-rose-200 bg-white px-4 py-3 text-sm font-extrabold text-rose-700 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-50"
                    disabled={course.status !== 'PENDING_REVIEW'}
                    onClick={() => openModal(course, 'reject')}
                    type="button"
                  >
                    <XCircle className="h-4 w-4" />
                    Từ chối
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      {modal ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 px-4 py-8 backdrop-blur-sm">
          <section className="w-full max-w-xl rounded-[28px] border border-slate-200 bg-white p-6 shadow-2xl">
            <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#730014]">
              {modal.version
                ? `${modal.action === 'approve' ? 'Duyệt và xuất bản' : 'Yêu cầu chỉnh sửa'} phiên bản v${modal.version.versionNumber}`
                : modal.action === 'approve' ? 'Duyệt khóa học' : 'Từ chối khóa học'}
            </p>
            <h3 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-slate-900">{modal.course.title}</h3>
            {modal.version ? (
              <div className="mt-5 max-h-64 overflow-y-auto rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <div className="grid grid-cols-2 gap-3">
                  <MiniMetric label="Phiên bản" value={`v${modal.version.versionNumber}`} />
                  <MiniMetric label="Cấu trúc" value={`${modal.version.totalRequiredLessons || 0} bài · ${modal.version.totalRequiredAssessments || 0} đánh giá`} />
                </div>
                <p className="mt-4 text-sm leading-6 text-slate-600">{modal.course.shortDescription || modal.course.description || 'Chưa có mô tả khóa học.'}</p>
                <div className="mt-4 space-y-2">
                  {(modal.course.modules || []).map((module, index) => (
                    <div className="rounded-xl border border-slate-200 bg-white px-3 py-2" key={module.id || module.title || index}>
                      <p className="text-sm font-extrabold text-slate-800">Mô-đun {index + 1}: {module.title}</p>
                      <p className="mt-1 text-xs text-slate-500">{module.lessons?.length || 0} bài học</p>
                    </div>
                  ))}
                </div>
              </div>
            ) : null}
            <label className="mt-5 block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">
                {modal.action === 'approve' ? 'Ghi chú duyệt' : 'Lý do từ chối'}
              </span>
              <textarea
                className="min-h-32 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-[#730014] focus:bg-white"
                onChange={(event) => setReviewNote(event.target.value)}
                placeholder={modal.action === 'approve' ? 'Ghi chú nội bộ cho lần duyệt này...' : 'Nêu rõ phần cần sửa: nội dung, cấu trúc, band, tài liệu...'}
                value={reviewNote}
              />
            </label>
            <div className="mt-6 flex flex-wrap justify-end gap-3">
              <button
                className="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-extrabold text-slate-600 transition hover:bg-slate-50"
                onClick={closeModal}
                type="button"
              >
                Hủy
              </button>
              <button
                className={`rounded-2xl px-5 py-3 text-sm font-extrabold text-white shadow-sm transition disabled:cursor-not-allowed disabled:opacity-60 ${
                  modal.action === 'approve' ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-rose-600 hover:bg-rose-700'
                }`}
                disabled={submitting}
                onClick={submitReview}
                type="button"
              >
                {submitting ? 'Đang xử lý...' : modal.action === 'approve' ? 'Xác nhận duyệt' : 'Xác nhận từ chối'}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  );
}

function HeroStat({ label, value }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/10 p-4 text-center">
      <p className="font-['Manrope'] text-3xl font-black">{value}</p>
      <p className="mt-1 text-xs font-bold uppercase tracking-[0.14em] text-white/60">{label}</p>
    </div>
  );
}

function StatusBadge({ status }) {
  const meta = statusMeta[status] || { label: status || 'Không rõ', className: 'bg-slate-50 text-slate-600 border-slate-200' };
  return <span className={`rounded-full border px-3 py-1 text-xs font-extrabold ${meta.className}`}>{meta.label}</span>;
}

function MiniMetric({ label, value }) {
  return (
    <div className="rounded-2xl bg-slate-50 px-3 py-2">
      <p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-slate-400">{label}</p>
      <p className="mt-1 text-sm font-bold text-slate-800">{value}</p>
    </div>
  );
}

function Notice({ children, tone }) {
  const className = tone === 'error'
    ? 'border-rose-200 bg-rose-50 text-rose-700'
    : 'border-emerald-200 bg-emerald-50 text-emerald-700';
  return <div className={`rounded-2xl border px-5 py-4 text-sm font-bold ${className}`}>{children}</div>;
}

function formatDate(value) {
  if (!value) return 'Chưa có';
  return new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}
