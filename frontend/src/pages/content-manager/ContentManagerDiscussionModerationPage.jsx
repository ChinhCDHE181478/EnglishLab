import { useCallback, useEffect, useState } from 'react';
import { Eye, EyeOff, Flag, RefreshCw, XCircle } from 'lucide-react';
import { courseApi } from '../../api/courseApi';
import { ManagerEmptyState, ManagerFilterBar, ManagerStatusBadge, ManagerTable } from '../../components/content-manager/ManagerListUi';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { useAppDialog } from '../../components/ui/AppDialog';
import ManagementToast from '../../components/ui/ManagementToast';
import { EMPTY_PAGE, pageParams } from '../../utils/pagination';
import { getContentManagerError } from '../../utils/contentManagerFeedback';

const STATUS_FILTERS = [
  { value: 'PENDING', label: 'Đang chờ' },
  { value: 'ACTION_TAKEN', label: 'Đã ẩn' },
  { value: 'DISMISSED', label: 'Đã bỏ qua' },
];

const CATEGORY_FILTERS = [
  { value: '', label: 'Tất cả' },
  { value: 'SPAM', label: 'Spam' },
  { value: 'INAPPROPRIATE_LANGUAGE', label: 'Ngôn ngữ không phù hợp' },
  { value: 'OFF_TOPIC', label: 'Sai chủ đề' },
  { value: 'HARASSMENT', label: 'Quấy rối' },
  { value: 'OTHER', label: 'Khác' },
];

const CATEGORY_LABELS = {
  SPAM: 'Spam / quảng cáo',
  INAPPROPRIATE_LANGUAGE: 'Ngôn ngữ không phù hợp',
  OFF_TOPIC: 'Sai chủ đề',
  HARASSMENT: 'Quấy rối',
  OTHER: 'Khác',
};

const COLUMNS = [
  { key: 'content', label: 'Nội dung báo cáo' },
  { key: 'target', label: 'Loại' },
  { key: 'course', label: 'Khóa học / bài học' },
  { key: 'reporter', label: 'Người báo cáo' },
  { key: 'reason', label: 'Lý do' },
  { key: 'date', label: 'Ngày báo cáo' },
  { key: 'actions', label: 'Thao tác', align: 'right' },
];

export default function ContentManagerDiscussionModerationPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const [status, setStatus] = useState('PENDING');
  const [category, setCategory] = useState('');
  const [pageResult, setPageResult] = useState(EMPTY_PAGE);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [processingId, setProcessingId] = useState(null);
  const resetKey = `moderation-${status}-${category}`;
  const { page, setPage, totalPages, pageItems: reports, totalItems } = usePagination(
    pageResult.content,
    10,
    resetKey,
    pageResult,
  );

  const loadReports = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await courseApi.getDiscussionModerationReportsPage(
        pageParams(page, 10, {
          status,
          category: category || undefined,
        }),
      );
      setPageResult(data);
    } catch (requestError) {
      setError(getContentManagerError(requestError, 'Không thể tải hàng chờ kiểm duyệt.'));
      setPageResult(EMPTY_PAGE);
      return false;
    } finally {
      setLoading(false);
    }
    return true;
  }, [status, category, page]);

  useEffect(() => {
    loadReports();
  }, [loadReports]);

  const handleAction = async (report, action) => {
    const isHide = action === 'hide';
    const confirmed = await confirmDialog(
      isHide
        ? 'Ẩn nội dung này khỏi phần thảo luận của học viên?'
        : 'Bỏ qua báo cáo này và giữ nguyên nội dung?',
      {
        title: isHide ? 'Ẩn nội dung thảo luận' : 'Bỏ qua báo cáo',
        confirmLabel: isHide ? 'Ẩn nội dung' : 'Bỏ qua báo cáo',
        tone: isHide ? 'danger' : 'primary',
      },
    );
    if (!confirmed) return;
    setProcessingId(report.reportId);
    setError('');
    setSuccess('');
    try {
      if (isHide) {
        await courseApi.hideReportedDiscussion(report.reportId);
      } else {
        await courseApi.dismissDiscussionReport(report.reportId);
      }
      const reloaded = await loadReports();
      if (reloaded) {
        setSuccess(isHide
          ? 'Nội dung đã được ẩn khỏi phần hỏi đáp.'
          : report.status === 'ACTION_TAKEN'
            ? 'Nội dung đã được hiển thị lại và báo cáo được chuyển sang đã bỏ qua.'
            : 'Báo cáo đã được bỏ qua và nội dung vẫn được giữ nguyên.');
      }
    } catch (requestError) {
      setError(getContentManagerError(requestError, 'Không thể xử lý báo cáo. Vui lòng thử lại.'));
    } finally {
      setProcessingId(null);
    }
  };

  const changeStatus = (value) => {
    setPage(1);
    setStatus(value);
  };

  const changeCategory = (value) => {
    setPage(1);
    setCategory(value);
  };

  return (
    <div className="space-y-6">
      <ManagementToast message={error} onClose={() => setError('')} />
      <ManagementToast
        message={success}
        onClose={() => setSuccess('')}
        tone="success"
        title="Đã xử lý báo cáo"
      />
      <ManagerFilterBar>
        {/* Status tabs */}
        <div className="flex flex-wrap gap-2" role="tablist" aria-label="Trạng thái báo cáo">
          {STATUS_FILTERS.map((filter) => (
            <button
              className={`rounded-lg px-4 py-2.5 text-sm font-bold transition ${
                status === filter.value
                  ? 'bg-[#4b0009] text-white shadow-sm'
                  : 'border border-[#dcc0bf]/50 bg-white text-[#564241] hover:bg-[#fff6f6]'
              }`}
              key={filter.value}
              onClick={() => changeStatus(filter.value)}
              role="tab"
              type="button"
            >
              {filter.label}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-3" aria-label="Loại báo cáo">
          <label className="text-sm font-bold text-[#564241]" htmlFor="report-category">Loại báo cáo</label>
          <div className="min-w-[190px]">
            <BrandedSelect
              buttonClassName="rounded-lg border-[#dcc0bf]/50 bg-white py-2 text-sm font-semibold text-[#564241] shadow-none"
              id="report-category"
              onChange={(event) => changeCategory(event.target.value)}
              options={CATEGORY_FILTERS}
              value={category}
            />
          </div>
        </div>

        <button
          className="ml-auto inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf]/50 px-4 py-2.5 text-sm font-bold text-[#4b0009] transition hover:bg-[#fff6f6]"
          disabled={loading}
          onClick={loadReports}
          type="button"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </button>
      </ManagerFilterBar>

      <section className="overflow-hidden rounded-xl border border-[#dcc0bf]/30 bg-white shadow-sm">
        {loading ? (
          <div className="px-6 py-16 text-center text-sm font-semibold text-[#564241]">
            Đang tải báo cáo...
          </div>
        ) : null}
        {!loading && reports.length === 0 ? (
          <ManagerEmptyState>Không có báo cáo nào ở trạng thái / loại này.</ManagerEmptyState>
        ) : null}
        {!loading && reports.length > 0 ? (
          <>
            <ManagerTable columns={COLUMNS} minWidth="1440px">
              {reports.map((report) => (
                <tr className="align-top transition hover:bg-[#eff4ff]/35" key={report.reportId}>
                  <td className="w-[330px] px-6 py-5">
                    <div className="mb-2 flex items-center gap-2">
                      <Flag className="h-4 w-4 shrink-0 text-rose-600" />
                      <span className="text-xs font-bold uppercase tracking-wide text-rose-700">
                        {report.reportCount} lượt báo cáo
                      </span>
                    </div>
                    <p className="line-clamp-4 break-words text-sm leading-6 text-[#0b1c30]" title={report.contentPreview}>{report.contentPreview}</p>
                    <p className="mt-2 text-xs text-[#756361]">
                      Tác giả: <span className="font-semibold">{report.targetAuthor}</span>
                    </p>
                    <TargetStatus status={report.currentTargetStatus} />
                  </td>
                  <td className="w-[105px] px-6 py-5">
                    <ManagerStatusBadge tone={report.targetType === 'THREAD' ? 'info' : 'neutral'}>
                      {report.targetType === 'THREAD' ? 'Chủ đề' : 'Trả lời'}
                    </ManagerStatusBadge>
                  </td>
                  <td className="w-[230px] px-6 py-5 text-sm">
                    <p className="line-clamp-2 break-words font-bold leading-5 text-[#0b1c30]" title={report.courseTitle}>{report.courseTitle}</p>
                    {report.lessonTitle ? (
                      <p className="mt-1 text-xs leading-5 text-[#756361]">Bài học: {report.lessonTitle}</p>
                    ) : (
                      <p className="mt-1 text-xs text-[#756361]">Thảo luận cấp khóa học</p>
                    )}
                  </td>
                  <td className="w-[230px] px-6 py-5 text-sm">
                    <p className="font-semibold text-[#0b1c30]">{report.reporterName}</p>
                    <p className="mt-1 break-all text-xs leading-5 text-[#756361]">{report.reporterEmail}</p>
                  </td>
                  <td className="w-[300px] px-6 py-5">
                    <ReportReason report={report} />
                  </td>
                  <td className="w-[155px] whitespace-nowrap px-6 py-5 text-sm text-[#564241]">
                    {formatDate(report.createdAt)}
                  </td>
                  <td className="w-[190px] px-6 py-5 text-right">
                    {report.status === 'PENDING' ? (
                      <div className="flex flex-col items-end gap-2">
                        <button
                          className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-[#730014] px-3.5 py-2 text-xs font-bold text-white transition hover:bg-[#4b0009] disabled:opacity-50"
                          disabled={processingId === report.reportId}
                          onClick={() => handleAction(report, 'hide')}
                          type="button"
                        >
                          <EyeOff className="h-4 w-4" />
                          Ẩn nội dung
                        </button>
                        <button
                          className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg border border-[#dcc0bf] px-3.5 py-2 text-xs font-bold text-[#564241] transition hover:bg-slate-50 disabled:opacity-50"
                          disabled={processingId === report.reportId}
                          onClick={() => handleAction(report, 'dismiss')}
                          type="button"
                        >
                          <XCircle className="h-4 w-4" />
                          Bỏ qua báo cáo
                        </button>
                      </div>
                    ) : report.status === 'ACTION_TAKEN' ? (
                      <button
                        className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg border border-emerald-200 px-3.5 py-2 text-xs font-bold text-emerald-700 transition hover:bg-emerald-50 disabled:opacity-50"
                        disabled={processingId === report.reportId}
                        onClick={() => handleAction(report, 'dismiss')}
                        type="button"
                      >
                        <Eye className="h-4 w-4" />
                        Gỡ ẩn
                      </button>
                    ) : (
                      <button
                        className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg bg-[#730014] px-3.5 py-2 text-xs font-bold text-white transition hover:bg-[#4b0009] disabled:opacity-50"
                        disabled={processingId === report.reportId}
                        onClick={() => handleAction(report, 'hide')}
                        type="button"
                      >
                        <EyeOff className="h-4 w-4" />
                        Ẩn nội dung
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </ManagerTable>

            <div className="border-t border-[#dcc0bf]/20 bg-[#eff4ff]/30 px-6 py-4">
              <Pagination
                alwaysVisible
                onChange={setPage}
                page={page}
                pageSize={10}
                totalItems={totalItems}
                totalPages={totalPages}
              />
            </div>
          </>
        ) : null}
      </section>
    </div>
  );
}

function ReportReason({ report }) {
  const categoryLabel = CATEGORY_LABELS[report.reasonCategory] || report.reasonCategory || 'Chưa phân loại';
  const reason = String(report.reason || '').trim();

  return (
    <div className="min-w-[250px] rounded-xl border border-rose-100 bg-rose-50/45 p-3">
      <span className="inline-flex max-w-full whitespace-nowrap rounded-full border border-rose-200 bg-white px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-wide text-rose-700">
        {categoryLabel}
      </span>
      <p
        className={`mt-2 line-clamp-3 break-words text-sm leading-6 ${reason ? 'text-[#564241]' : 'italic text-[#8b706e]'}`}
        title={reason || 'Không có mô tả bổ sung'}
      >
        {reason || 'Không có mô tả bổ sung'}
      </p>
    </div>
  );
}

function TargetStatus({ status }) {
  const hidden = status === 'HIDDEN';
  return (
    <span
      className={`mt-3 inline-flex rounded-full px-2.5 py-1 text-[10px] font-bold uppercase ${
        hidden ? 'bg-rose-100 text-rose-700' : 'bg-slate-100 text-slate-600'
      }`}
    >
      {hidden
        ? 'Đã ẩn'
        : status === 'PENDING_REVIEW'
        ? 'Chờ kiểm duyệt'
        : status === 'RESOLVED'
        ? 'Đã giải quyết'
        : 'Đang hiển thị'}
    </span>
  );
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
