import { useDeferredValue, useEffect, useState } from 'react';
import { RefreshCw, Search, Users } from 'lucide-react';
import courseApi from '../../api/courseApi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import {
  EMPTY_STATE_CLASS,
  ERROR_NOTICE_CLASS,
  GHOST_BUTTON_CLASS,
  PANEL_CLASS,
  SEARCH_INPUT_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
} from '../../utils/formStyles';
import { EMPTY_PAGE, pageParams } from '../../utils/pagination';

const statusOptions = [
  { label: 'Tất cả', value: 'ALL' },
  { label: 'Đang học', value: 'ACTIVE' },
  { label: 'Hoàn thành', value: 'COMPLETED' },
  { label: 'Đã hủy', value: 'CANCELLED' },
];

const statusMeta = {
  ACTIVE: 'bg-emerald-50 text-emerald-700 border border-emerald-100',
  COMPLETED: 'bg-blue-50 text-blue-700 border border-blue-100',
  CANCELLED: 'bg-slate-100 text-slate-600 border border-slate-200',
};

export default function ManagerOnlineEnrollmentsPage() {
  const [enrollments, setEnrollments] = useState([]);
  const [pageResult, setPageResult] = useState(EMPTY_PAGE);
  const [status, setStatus] = useState('ALL');
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [workingId, setWorkingId] = useState(null);
  const deferredKeyword = useDeferredValue(keyword.trim());
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    enrollments,
    10,
    `${deferredKeyword}|${status}`,
    pageResult,
  );

  const loadEnrollments = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await courseApi.getManagerOnlineEnrollmentsPage(pageParams(page, 10, {
        status: status === 'ALL' ? undefined : status,
        keyword: deferredKeyword || undefined,
      }));
      setPageResult(data);
      setEnrollments(data.content);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách ghi danh online.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadEnrollments(); }, [deferredKeyword, page, status]);

  const updateStatus = async (enrollmentId, nextStatus) => {
    setWorkingId(enrollmentId);
    setError('');
    setSuccess('');
    try {
      await courseApi.updateManagerOnlineEnrollment(enrollmentId, { status: nextStatus });
      setSuccess('Đã cập nhật trạng thái ghi danh.');
      await loadEnrollments();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không cập nhật được ghi danh.');
    } finally {
      setWorkingId(null);
    }
  };

  return (
    <div className="space-y-5">
      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      <section className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap items-center gap-3 flex-1">
          <div className="relative min-w-[240px] flex-1">
            <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Tìm học viên hoặc khóa học..." className={`${SEARCH_INPUT_CLASS} h-11 pl-10`} />
          </div>
          <div className="w-48">
            <BrandedSelect value={status} onChange={(event) => setStatus(event.target.value)} options={statusOptions} />
          </div>
        </div>
        <button
          type="button"
          onClick={loadEnrollments}
          className="inline-flex h-11 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-xs font-bold text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
        >
          <RefreshCw className="h-3.5 w-3.5" /> Tải lại
        </button>
      </section>

      {loading ? (
        <div className="space-y-3">
          <div className="h-11 animate-pulse rounded-xl bg-slate-100" />
          <div className="h-64 animate-pulse rounded-xl bg-slate-100" />
        </div>
      ) : enrollments.length === 0 ? (
        <div className={EMPTY_STATE_CLASS}>
          <Users className="mx-auto mb-3 h-8 w-8 text-slate-400" />
          Chưa có ghi danh phù hợp.
        </div>
      ) : (
        <div className="overflow-hidden rounded-xl border border-[#dfbfbd]/40 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1080px] text-left text-sm">
              <thead className="border-b border-[#dfbfbd]/30 bg-[#fbf3f4] text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">
                <tr>
                  <th className="px-5 py-4 w-60">Học viên</th>
                  <th className="px-5 py-4">Khóa học</th>
                  <th className="px-5 py-4 w-32">Tiến độ</th>
                  <th className="px-5 py-4 w-40">Trạng thái</th>
                  <th className="px-5 py-4 w-64">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#dfbfbd]/20">
                {pageItems.map((item) => (
                  <tr key={item.id} className="transition hover:bg-[#fffafb]">
                    <td className="px-5 py-4">
                      <div className="font-extrabold text-[#2b2828]">{item.studentName}</div>
                      <div className="text-xs text-slate-500">{item.studentEmail}</div>
                    </td>
                    <td className="px-5 py-4 font-semibold text-slate-700">{item.packageTitle}</td>
                    <td className="px-5 py-4 font-bold text-slate-600">{item.progressPercent ?? 0}%</td>
                    <td className="px-5 py-4">
                      <span className={`inline-flex rounded-full px-2.5 py-0.5 text-[11px] font-bold ${statusMeta[item.status] || 'bg-slate-100 text-slate-600 border border-slate-200'}`}>
                        {item.status}
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      <div className="flex flex-wrap gap-1.5">
                        {['ACTIVE', 'COMPLETED', 'CANCELLED'].filter((value) => value !== item.status).map((value) => {
                          const styleMap = {
                            ACTIVE: 'border-emerald-200 text-emerald-700 hover:bg-emerald-50',
                            COMPLETED: 'border-blue-200 text-blue-700 hover:bg-blue-50',
                            CANCELLED: 'border-rose-200 text-rose-700 hover:bg-rose-50',
                          };
                          const labelMap = {
                            ACTIVE: 'Kích hoạt',
                            COMPLETED: 'Hoàn thành',
                            CANCELLED: 'Hủy học',
                          };
                          return (
                            <button
                              key={value}
                              type="button"
                              disabled={workingId === item.id}
                              onClick={() => updateStatus(item.id, value)}
                              className={`inline-flex items-center rounded-lg border px-2.5 py-1 text-xs font-bold transition disabled:opacity-60 ${styleMap[value] || 'border-slate-200 text-slate-700 hover:bg-slate-50'}`}
                            >
                              {labelMap[value] || value}
                            </button>
                          );
                        })}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {!loading && filtered.length > 0 && (
        <Pagination page={page} totalPages={totalPages} onChange={setPage} totalItems={totalItems} pageSize={10} />
      )}
    </div>
  );
}
