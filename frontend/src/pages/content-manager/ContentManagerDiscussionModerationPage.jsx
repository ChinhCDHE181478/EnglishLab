import { useCallback, useEffect, useState } from 'react';
import { AlertTriangle, EyeOff, Flag, RefreshCw, ShieldCheck, XCircle } from 'lucide-react';
import { courseApi } from '../../api/courseApi';
import { ManagerEmptyState, ManagerFilterBar, ManagerStatusBadge, ManagerTable } from '../../components/content-manager/ManagerListUi';
import BrandedSelect from '../../components/ui/BrandedSelect';

const STATUS_FILTERS = [
  { value: 'PENDING', label: 'Đang chờ' },
  { value: 'ACTION_TAKEN', label: 'Đã ẩn' },
  { value: 'DISMISSED', label: 'Đã bỏ qua' },
];

const CATEGORY_FILTERS = [
  { value: '', label: 'Tất cả' },
  { value: 'SPAM', label: 'Spam' },
  { value: 'INAPPROPRIATE_LANGUAGE', label: 'Ngôn ngữ...' },
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
  const [status, setStatus] = useState('PENDING');
  const [category, setCategory] = useState('');
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [processingId, setProcessingId] = useState(null);

  const loadReports = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await courseApi.getDiscussionModerationReports(status, category);
      setReports(data);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Không thể tải hàng chờ kiểm duyệt.');
      setReports([]);
    } finally {
      setLoading(false);
    }
  }, [status, category]);

  useEffect(() => {
    loadReports();
  }, [loadReports]);

  const handleAction = async (report, action) => {
    const isHide = action === 'hide';
    const confirmed = window.confirm(
      isHide
        ? 'Ẩn nội dung này khỏi phần thảo luận của học viên?'
        : 'Bỏ qua báo cáo này và giữ nguyên nội dung?'
    );
    if (!confirmed) return;
    setProcessingId(report.reportId);
    setError('');
    try {
      if (isHide) {
        await courseApi.hideReportedDiscussion(report.reportId);
      } else {
        await courseApi.dismissDiscussionReport(report.reportId);
      }
      await loadReports();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Không thể xử lý báo cáo. Vui lòng thử lại.');
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div className="space-y-6">
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
              onClick={() => setStatus(filter.value)}
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
            onChange={(event) => setCategory(event.target.value)}
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

      {error ? (
        <div className="flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700">
          <AlertTriangle className="h-4 w-4" />
          {error}
        </div>
      ) : null}

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
          <ManagerTable columns={COLUMNS} minWidth="1320px">
            {reports.map((report) => (
              <tr className="align-top transition hover:bg-[#eff4ff]/35" key={report.reportId}>
                <td className="max-w-[360px] px-6 py-5">
                  <div className="mb-2 flex items-center gap-2">
                    <Flag className="h-4 w-4 shrink-0 text-rose-600" />
                    <span className="text-xs font-bold uppercase tracking-wide text-rose-700">
                      {report.reportCount} lượt báo cáo
                    </span>
                  </div>
                  <p className="line-clamp-4 text-sm leading-6 text-[#0b1c30]">{report.contentPreview}</p>
                  <p className="mt-2 text-xs text-[#756361]">
                    Tác giả: <span className="font-semibold">{report.targetAuthor}</span>
                  </p>
                  <TargetStatus status={report.currentTargetStatus} />
                </td>
                <td className="px-6 py-5">
                  <ManagerStatusBadge tone={report.targetType === 'THREAD' ? 'info' : 'neutral'}>
                    {report.targetType === 'THREAD' ? 'Chủ đề' : 'Trả lời'}
                  </ManagerStatusBadge>
                </td>
                <td className="max-w-[230px] px-6 py-5 text-sm">
                  <p className="font-bold text-[#0b1c30]">{report.courseTitle}</p>
                  {report.lessonTitle ? (
                    <p className="mt-1 text-xs leading-5 text-[#756361]">Bài học: {report.lessonTitle}</p>
                  ) : (
                    <p className="mt-1 text-xs text-[#756361]">Thảo luận cấp khóa học</p>
                  )}
                </td>
                <td className="px-6 py-5 text-sm">
                  <p className="font-semibold text-[#0b1c30]">{report.reporterName}</p>
                  <p className="mt-1 text-xs text-[#756361]">{report.reporterEmail}</p>
                </td>
                <td className="max-w-[220px] px-6 py-5 text-sm leading-6 text-[#564241]">
                  {/* Show reasonCategory as badge if present */}
                  {report.reasonCategory && (
                    <span className="mb-1.5 inline-block rounded-full bg-rose-50 px-2 py-0.5 text-[10px] font-bold text-rose-700 border border-rose-100">
                      {CATEGORY_LABELS[report.reasonCategory] || report.reasonCategory}
                    </span>
                  )}
                  <br />
                  {report.reason || (report.reasonCategory !== 'OTHER' ? '' : 'Không cung cấp lý do')}
                </td>
                <td className="whitespace-nowrap px-6 py-5 text-sm text-[#564241]">
                  {formatDate(report.createdAt)}
                </td>
                <td className="px-6 py-5 text-right">
                  {report.status === 'PENDING' ? (
                    <div className="flex justify-end gap-2">
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
                  ) : (
                    <div className="inline-flex items-center gap-2 text-xs font-semibold text-[#756361]">
                      <ShieldCheck className="h-4 w-4 text-emerald-600" />
                      {report.reviewedBy ? `Bởi ${report.reviewedBy}` : 'Đã xử lý'}
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </ManagerTable>
        ) : null}
      </section>
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
