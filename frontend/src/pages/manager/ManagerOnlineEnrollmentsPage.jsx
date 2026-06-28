import { useEffect, useMemo, useState } from 'react';
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
  const [status, setStatus] = useState('ALL');
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [workingId, setWorkingId] = useState(null);

  const loadEnrollments = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await courseApi.getManagerOnlineEnrollments({
        status: status === 'ALL' ? undefined : status,
        keyword: keyword.trim() || undefined,
      });
      setEnrollments(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách ghi danh online.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadEnrollments(); }, [status]);

  const filtered = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) return enrollments;
    return enrollments.filter((item) => [item.studentName, item.studentEmail, item.packageTitle]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(normalized)));
  }, [enrollments, keyword]);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    filtered,
    10,
    `${keyword}|${status}`,
  );

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
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="font-['Manrope'] text-2xl font-extrabold text-slate-900">Quản lý ghi danh khóa online</h2>
          <p className="mt-1 text-sm text-slate-600">Theo dõi học viên đã mua/ghi danh khóa học tự học trên nền tảng.</p>
        </div>
        <button type="button" onClick={loadEnrollments} className={SECONDARY_BUTTON_CLASS}>
          <RefreshCw className="h-4 w-4" /> Tải lại
        </button>
      </div>

      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      <div className="flex flex-wrap gap-3">
        <div className="relative min-w-[220px] flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Tìm học viên hoặc khóa học..." className={SEARCH_INPUT_CLASS} />
        </div>
        <div className="w-48">
          <BrandedSelect value={status} onChange={setStatus} options={statusOptions} />
        </div>
      </div>

      {loading ? (
        <p className="text-sm font-semibold text-slate-500">Đang tải...</p>
      ) : filtered.length === 0 ? (
        <div className={EMPTY_STATE_CLASS}>
          <Users className="mx-auto mb-3 h-8 w-8 text-slate-400" />
          Chưa có ghi danh phù hợp.
        </div>
      ) : (
        <div className={`${PANEL_CLASS} overflow-hidden !p-0`}>
          <table className="min-w-full text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs font-bold uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-4 py-3">Học viên</th>
                <th className="px-4 py-3">Khóa học</th>
                <th className="px-4 py-3">Tiến độ</th>
                <th className="px-4 py-3">Trạng thái</th>
                <th className="px-4 py-3">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {pageItems.map((item) => (
                <tr key={item.id} className="border-t border-slate-100">
                  <td className="px-4 py-3">
                    <div className="font-semibold text-slate-900">{item.studentName}</div>
                    <div className="text-xs text-slate-500">{item.studentEmail}</div>
                  </td>
                  <td className="px-4 py-3">{item.packageTitle}</td>
                  <td className="px-4 py-3">{item.progressPercent ?? 0}%</td>
                  <td className="px-4 py-3">
                    <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${statusMeta[item.status] || 'bg-slate-100 text-slate-600 border border-slate-200'}`}>
                      {item.status}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      {['ACTIVE', 'COMPLETED', 'CANCELLED'].filter((value) => value !== item.status).map((value) => (
                        <button
                          key={value}
                          type="button"
                          disabled={workingId === item.id}
                          onClick={() => updateStatus(item.id, value)}
                          className={GHOST_BUTTON_CLASS}
                        >
                          {value}
                        </button>
                      ))}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!loading && filtered.length > 0 && (
        <Pagination page={page} totalPages={totalPages} onChange={setPage} totalItems={totalItems} pageSize={10} />
      )}
    </div>
  );
}
