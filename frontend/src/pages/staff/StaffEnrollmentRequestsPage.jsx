import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { BookOpenCheck, Check, ClipboardList, RefreshCw, Search, Send, UserRoundCheck, X } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import { EnrollmentStatusBadge } from '../../components/classroom/EnrollmentRequestUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import {
  getEnrollmentRequestActions,
  getStaffEnrollmentLoadError,
  isAssignableClassroom,
  loadStaffEnrollmentData,
} from '../../utils/enrollmentAssignment';
import { ERROR_NOTICE_CLASS, FIELD_CLASS, PRIMARY_BUTTON_CLASS, SECONDARY_BUTTON_CLASS, TEXTAREA_CLASS } from '../../utils/formStyles';

const views = [
  { label: 'Tất cả', value: 'ALL' },
  { label: 'Mới đăng ký', value: 'SUBMITTED' },
  { label: 'Đã tư vấn', value: 'WAITING_FOR_CLASS' },
  { label: 'Đã xếp lớp', value: 'CLASS_ASSIGNED' },
  { label: 'Từ chối', value: 'REJECTED' },
  { label: 'Đã hủy', value: 'CANCELLED' },
];

const initialAction = { type: '', item: null, classroomId: '', note: '', reason: '' };

export default function StaffEnrollmentRequestsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const view = searchParams.get('tab') || 'ALL';
  const setView = (newTab) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('tab', newTab);
      return next;
    });
  };

  const [requests, setRequests] = useState([]);
  const [classrooms, setClassrooms] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [classroomLoadError, setClassroomLoadError] = useState('');
  const [success, setSuccess] = useState('');
  const [action, setAction] = useState(initialAction);

  const load = async () => {
    setLoading(true);
    setError('');
    setClassroomLoadError('');
    const result = await loadStaffEnrollmentData(
      () => enrollmentRequestApi.listForStaff(view),
      () => classroomApi.getManagerClassrooms(),
    );

    if (result.requestError) {
      setRequests([]);
      setError(getStaffEnrollmentLoadError(result.requestError, 'requests'));
    } else {
      setRequests(result.requests);
    }

    if (result.classroomError) {
      setClassrooms([]);
      setClassroomLoadError(getStaffEnrollmentLoadError(result.classroomError, 'classrooms'));
    } else {
      setClassrooms(
        result.classrooms
          .filter((classroom) => isAssignableClassroom(classroom))
          .sort((left, right) => String(left.startDate).localeCompare(String(right.startDate))),
      );
    }
    setLoading(false);
  };

  useEffect(() => {
    load();
    setAction(initialAction);
  }, [view]);

  const filteredRequests = useMemo(() => {
    const normalized = keyword.trim().toLocaleLowerCase('vi-VN');
    return requests.filter((item) => !normalized || [
      item.learnerName,
      item.learnerEmail,
      item.contactName,
      item.contactEmail,
      item.contactPhone,
      item.desiredClassCode,
      item.consultationTrack,
      item.studyWorkGoal,
      item.requestedClassroomTitle,
      item.courseOfferingTitle,
      item.preferredSchedule,
      item.campusPreference,
    ].filter(Boolean).join(' ').toLocaleLowerCase('vi-VN').includes(normalized));
  }, [keyword, requests]);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    filteredRequests,
    10,
    `${keyword}|${view}`,
  );

  const applyTransition = (updated) => {
    setRequests((current) => (
      view === 'ALL'
        ? current.map((item) => (item.id === updated.id ? updated : item))
        : current.filter((item) => item.id !== updated.id)
    ));
  };

  const completeConsultation = async () => {
    setWorking(true);
    setError('');
    try {
      const updated = await enrollmentRequestApi.completeConsultation(action.item.id, action.note.trim());
      applyTransition(updated);
      setSuccess(`Đã ghi nhận tư vấn xong cho ${updated.contactName || updated.learnerName}; hồ sơ chuyển sang chờ xếp lớp.`);
      setAction(initialAction);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể cập nhật kết quả tư vấn.');
    } finally {
      setWorking(false);
    }
  };

  const assignClass = async () => {
    if (!action.classroomId) {
      setError('Vui lòng chọn lớp phù hợp cho học viên.');
      return;
    }
    setWorking(true);
    setError('');
    try {
      const updated = await enrollmentRequestApi.assignClass(action.item.id, {
        classroomId: Number(action.classroomId),
        note: action.note.trim() || null,
      });
      applyTransition(updated);
      setSuccess(`Đã xếp ${updated.contactName || updated.learnerName} vào lớp #${updated.assignedClassroomId}.`);
      setAction(initialAction);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể xếp lớp cho học viên.');
    } finally {
      setWorking(false);
    }
  };

  const reject = async () => {
    if (!action.reason.trim()) {
      setError('Lý do từ chối là bắt buộc.');
      return;
    }
    setWorking(true);
    setError('');
    try {
      const updated = await enrollmentRequestApi.reject(action.item.id, action.reason.trim());
      applyTransition(updated);
      setSuccess(`Đã từ chối form #${updated.id}.`);
      setAction(initialAction);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể từ chối form.');
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-5">
      <section className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex gap-1.5 overflow-x-auto">
          {views.map((item) => (
            <button
              className={`shrink-0 rounded-xl px-4 py-2.5 text-xs font-extrabold transition ${
                view === item.value
                  ? 'bg-[#4b0009] text-white shadow-sm'
                  : 'bg-slate-50 text-slate-600 hover:bg-slate-100'
              }`}
              key={item.value}
              onClick={() => setView(item.value)}
              type="button"
            >
              {item.label}
            </button>
          ))}
        </div>
        <div className="flex w-full items-center gap-3 md:w-auto">
          <label className="relative flex-1 md:w-80">
            <span className="sr-only">Tìm hồ sơ</span>
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              className={`${FIELD_CLASS} h-11 pl-11`}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Tìm học viên, email, lộ trình..."
              value={keyword}
            />
          </label>
          <button
            aria-label="Tải lại"
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-slate-200 text-[#730014] transition hover:bg-slate-50"
            disabled={loading}
            onClick={load}
            type="button"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </section>

      {error ? <div className={ERROR_NOTICE_CLASS}>{error}</div> : null}
      {classroomLoadError ? <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-800">{classroomLoadError} Danh sách yêu cầu vẫn có thể xem và cập nhật tư vấn; hãy tải lại trước khi xếp lớp.</div> : null}
      {success ? <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-700">{success}</div> : null}
      {loading ? (
        <div className="space-y-3">
          <div className="h-12 animate-pulse rounded-xl bg-slate-100" />
          <div className="h-48 animate-pulse rounded-xl bg-slate-100" />
        </div>
      ) : null}
      {!loading && pageItems.length ? (
        <section className="overflow-hidden rounded-xl border border-[#dfbfbd]/40 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1280px] text-left text-sm">
              <thead className="border-b border-[#dfbfbd]/30 bg-[#fbf3f4] text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">
                <tr>
                  <th className="px-5 py-4 w-20">Mã</th>
                  <th className="px-5 py-4 w-60">Học viên / Liên hệ</th>
                  <th className="px-5 py-4 w-48">Lộ trình quan tâm</th>
                  <th className="px-5 py-4 w-48">Nguyện vọng lớp</th>
                  <th className="px-5 py-4 min-w-[280px]">Mục tiêu & Ghi chú</th>
                  <th className="px-5 py-4 w-36">Ngày đăng ký</th>
                  <th className="px-5 py-4 w-36 text-center">Trạng thái</th>
                  <th className="px-5 py-4 w-52 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#dfbfbd]/20">
                {pageItems.map((item) => {
                  const contactName = item.contactName || item.learnerName || 'Học viên ẩn danh';
                  const contactEmail = item.contactEmail || item.learnerEmail || 'Chưa cung cấp email';
                  const { canCompleteConsultation, canAssign, canReject } = getEnrollmentRequestActions(item.status);
                  return (
                    <tr className="transition hover:bg-[#fffafb]" key={item.id}>
                      <td className="px-5 py-4 font-bold text-slate-400">#{item.id}</td>
                      <td className="px-5 py-4">
                        <p className="font-extrabold text-[#2b2828]">{contactName}</p>
                        <p className="mt-0.5 text-xs text-slate-500">{contactEmail}</p>
                        <p className="mt-0.5 text-xs font-bold text-slate-600">{item.contactPhone || 'Chưa có SĐT'}</p>
                        {item.facebookUrl && (
                          <a href={item.facebookUrl} target="_blank" rel="noopener noreferrer" className="mt-1 inline-flex items-center gap-1 text-[11px] text-[#730014] hover:underline">
                            Facebook Link
                          </a>
                        )}
                      </td>
                      <td className="px-5 py-4">
                        <span className="rounded-lg bg-rose-50 px-2 py-1 text-xs font-bold text-[#8a0018]">
                          {formatConsultationTrack(item.consultationTrack)}
                        </span>
                      </td>
                      <td className="px-5 py-4 text-xs font-bold text-slate-700">
                        {item.desiredClassCode ? (
                          <span className="text-[#4b0009]">{item.desiredClassCode}</span>
                        ) : (
                          <span className="text-slate-400">Chưa chọn mã lớp</span>
                        )}
                        {item.requestedClassroomTitle && (
                          <p className="mt-1 text-[10px] text-slate-400 font-medium">{item.requestedClassroomTitle}</p>
                        )}
                      </td>
                      <td className="px-5 py-4">
                        {item.studyWorkGoal ? (
                          <div className="text-xs text-slate-700">
                            <span className="font-semibold text-slate-500">Mục tiêu:</span> {item.studyWorkGoal}
                          </div>
                        ) : null}
                        {item.notes ? (
                          <div className="mt-1 text-xs text-slate-600">
                            <span className="font-semibold text-slate-500">Ghi chú:</span> {item.notes}
                          </div>
                        ) : null}
                      </td>
                      <td className="px-5 py-4 text-xs text-slate-500">
                        {item.createdAt ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(item.createdAt)) : '-'}
                      </td>
                      <td className="px-5 py-4 text-center">
                        <EnrollmentStatusBadge label={item.statusLabel} status={item.status} />
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex justify-end gap-1.5 flex-wrap">
                          {canCompleteConsultation && (
                            <button
                              className="inline-flex items-center gap-1 rounded-lg bg-[#4b0009] px-2.5 py-1.5 text-xs font-bold text-white transition hover:bg-[#730014]"
                              onClick={() => setAction({ ...initialAction, type: 'CONSULT', item })}
                              type="button"
                            >
                              Đã tư vấn
                            </button>
                          )}
                          {canAssign && (
                            <button
                              className="inline-flex items-center gap-1 rounded-lg bg-emerald-700 px-2.5 py-1.5 text-xs font-bold text-white transition hover:bg-emerald-800"
                              onClick={() => setAction({ ...initialAction, type: 'ASSIGN', item })}
                              type="button"
                            >
                              Xếp lớp
                            </button>
                          )}
                          {canReject && (
                            <button
                              className="inline-flex items-center gap-1 rounded-lg border border-rose-200 px-2.5 py-1.5 text-xs font-bold text-rose-700 transition hover:bg-rose-50"
                              onClick={() => setAction({ ...initialAction, type: 'REJECT', item })}
                              type="button"
                            >
                              Từ chối
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <div className="border-t border-[#dfbfbd]/30 px-5 py-4">
            <Pagination onChange={setPage} page={page} pageSize={10} totalItems={totalItems} totalPages={totalPages} />
          </div>
        </section>
      ) : null}
      {!loading && !error && !filteredRequests.length ? <EmptyState /> : null}
      {action.type ? <ActionModal action={action} classroomLoadError={classroomLoadError} classrooms={classrooms} onChange={setAction} onClose={() => setAction(initialAction)} onConfirm={action.type === 'CONSULT' ? completeConsultation : action.type === 'ASSIGN' ? assignClass : reject} working={working} /> : null}
    </div>
  );
}

function ActionModal({ action, classroomLoadError, classrooms, onChange, onClose, onConfirm, working }) {
  const isReject = action.type === 'REJECT';
  const isAssign = action.type === 'ASSIGN';
  return <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm"><section aria-modal="true" className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl" role="dialog"><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">{isReject ? 'Từ chối form' : isAssign ? 'Staff xếp lớp' : 'Hoàn tất tư vấn'}</p><h2 className="mt-2 text-xl font-black text-[#0b1c30]">{action.item.contactName || action.item.learnerName} · #{action.item.id}</h2></div><button className="rounded-lg p-2 text-slate-400" onClick={onClose} type="button"><X className="h-5 w-5" /></button></div>{isAssign ? <div className="mt-5"><FieldLabel>Lớp phù hợp *</FieldLabel><BrandedSelect onChange={(event) => onChange({ ...action, classroomId: event.target.value })} options={classrooms.map((item) => ({ value: String(item.id), label: item.title, description: `${item.startDate} · ${item.primaryTeacherName || 'Chưa có giáo viên'} · ${item.enrolledCount || 0}/${item.maxCapacity || '∞'} học viên` }))} placeholder={classrooms.length ? 'Chọn lớp sắp khai giảng' : classroomLoadError ? 'Chưa tải được danh sách lớp' : 'Chưa có lớp sắp khai giảng còn chỗ'} searchable value={action.classroomId} />{!classrooms.length ? <p className="mt-3 rounded-xl bg-amber-50 px-4 py-3 text-sm font-semibold leading-6 text-amber-800">{classroomLoadError || 'Hiện chưa có lớp sắp khai giảng, chưa bắt đầu và còn chỗ để xếp học viên.'}</p> : null}</div> : null}<label className="mt-4 block"><FieldLabel>{isReject ? 'Lý do từ chối *' : 'Ghi chú nội bộ'}</FieldLabel><textarea className={TEXTAREA_CLASS} onChange={(event) => onChange({ ...action, [isReject ? 'reason' : 'note']: event.target.value })} placeholder={isReject ? 'Nêu lý do rõ ràng...' : 'Kết quả tư vấn/test bên ngoài, lưu ý khi xếp lớp...'} rows={5} value={isReject ? action.reason : action.note} /></label><div className="mt-6 flex justify-end gap-2"><button className={SECONDARY_BUTTON_CLASS} disabled={working} onClick={onClose} type="button">Hủy</button><button className={isReject ? 'rounded-xl bg-rose-700 px-5 py-3 text-sm font-extrabold text-white' : PRIMARY_BUTTON_CLASS} disabled={working || (isAssign && !classrooms.length)} onClick={onConfirm} type="button"><Send className="h-4 w-4" />{working ? 'Đang xử lý...' : isReject ? 'Từ chối' : isAssign ? 'Xác nhận xếp lớp' : 'Đã tư vấn xong'}</button></div></section></div>;
}

function formatConsultationTrack(value) {
  return {
    IELTS_4_SKILLS: 'IELTS 4 kỹ năng',
    TOEIC_2_SKILLS: 'TOEIC 2 kỹ năng',
    TOEIC_4_SKILLS: 'TOEIC 4 kỹ năng',
  }[value] || value || 'Chưa chọn';
}

function FieldLabel({ children }) { return <span className="mb-2 block text-xs font-bold uppercase tracking-[0.1em] text-slate-500">{children}</span>; }
function EmptyState() {
  return (
    <section className="flex min-h-[360px] flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center">
      <BookOpenCheck className="h-12 w-12 text-slate-300" />
      <h2 className="mt-4 text-xl font-black text-[#0b1c30]">Không có hồ sơ đăng ký</h2>
      <p className="mt-2 text-sm text-slate-500">
        Hiện tại không có học viên nào trong trạng thái này. Hồ sơ mới từ Lịch khai giảng sẽ xuất hiện tại mục Mới đăng ký.
      </p>
    </section>
  );
}
