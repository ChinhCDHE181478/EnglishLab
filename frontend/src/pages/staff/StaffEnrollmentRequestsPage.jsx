import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  BookOpenCheck,
  CalendarClock,
  CheckCircle2,
  Eye,
  RefreshCw,
  Search,
  Send,
  UserPlus,
  UserRoundCheck,
  X,
  XCircle,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import { EnrollmentStatusBadge } from '../../components/classroom/EnrollmentRequestUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import ManagementToast from '../../components/ui/ManagementToast';
import VietnameseDateInput from '../../components/ui/VietnameseDateInput';
import { formatClassroomDate, formatClassroomDateTime } from '../../utils/classroomHelpers';
import {
  getEnrollmentRequestActions,
  getStaffEnrollmentLoadError,
  isAssignableClassroom,
  loadStaffEnrollmentData,
} from '../../utils/enrollmentAssignment';
import { ERROR_NOTICE_CLASS, FIELD_CLASS, PRIMARY_BUTTON_CLASS, SECONDARY_BUTTON_CLASS, TEXTAREA_CLASS } from '../../utils/formStyles';
import { combineLocalDateTime } from '../../utils/vietnameseDate';

const views = [
  { label: 'Tất cả của tôi', value: 'ALL' },
  { label: 'Mới đăng ký', value: 'SUBMITTED' },
  { label: 'Đã hẹn test', value: 'TEST_SCHEDULED' },
  { label: 'Chờ học viên xác nhận', value: 'CLASS_PROPOSED' },
  { label: 'Đủ điều kiện', value: 'WAITING_FOR_CLASS' },
  { label: 'Đã chọn lớp', value: 'CLASS_ASSIGNED' },
  { label: 'Không phù hợp', value: 'REJECTED' },
];

const trackFilterOptions = [
  { label: 'Tất cả chương trình', value: 'ALL' },
  { label: 'IELTS 4 kỹ năng', value: 'IELTS_4_SKILLS' },
  { label: 'TOEIC Listening & Reading', value: 'TOEIC_2_SKILLS' },
  { label: 'TOEIC 4 kỹ năng', value: 'TOEIC_4_SKILLS' },
  { label: 'Tiếng Anh nền tảng', value: 'ENGLISH_FOUNDATION' },
];

const sourceFilterOptions = [
  { label: 'Tất cả nguồn đăng ký', value: 'ALL' },
  { label: 'Đăng ký Online', value: 'ONLINE' },
  { label: 'Tại trung tâm', value: 'CENTER' },
];

const initialAction = {
  type: '',
  item: null,
  classroomId: '',
  recommendedCourseOfferingId: '',
  note: '',
  reason: '',
  appointmentDate: '',
  appointmentTime: '',
  location: 'EnglishLab Campus, Hà Nội',
  eligible: 'true',
};

export default function StaffEnrollmentRequestsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const view = searchParams.get('tab') || 'ALL';
  const setView = (newTab) => {
    setSearchParams((previous) => {
      const next = new URLSearchParams(previous);
      next.set('tab', newTab);
      return next;
    });
  };

  const [requests, setRequests] = useState([]);
  const [classrooms, setClassrooms] = useState([]);
  const [courseOfferings, setCourseOfferings] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [trackFilter, setTrackFilter] = useState('ALL');
  const [sourceFilter, setSourceFilter] = useState('ALL');

  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [classroomLoadError, setClassroomLoadError] = useState('');
  const [courseOfferingLoadError, setCourseOfferingLoadError] = useState('');
  const [success, setSuccess] = useState('');
  const [action, setAction] = useState(initialAction);
  const [assignmentAvailability, setAssignmentAvailability] = useState({ loading: false, ids: null });
  const [centerEnrollmentOpen, setCenterEnrollmentOpen] = useState(false);
  const [detailRequest, setDetailRequest] = useState(null);

  const load = async () => {
    setLoading(true);
    setError('');
    setClassroomLoadError('');
    setCourseOfferingLoadError('');
    const loadCourseOfferings = async () => {
      try {
        return { items: await classroomApi.getStaffInstructorLedCourses(), error: null };
      } catch (requestError) {
        return { items: [], error: requestError };
      }
    };
    const [result, courseOfferingResult] = await Promise.all([
      loadStaffEnrollmentData(
        () => enrollmentRequestApi.listForStaff(),
        () => classroomApi.getStaffClassrooms(),
      ),
      loadCourseOfferings(),
    ]);
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
      setClassrooms(result.classrooms
        .filter((classroom) => isAssignableClassroom(classroom))
        .sort((left, right) => String(left.startDate).localeCompare(String(right.startDate))));
    }
    setCourseOfferings(courseOfferingResult.items || []);
    if (courseOfferingResult.error) {
      setCourseOfferingLoadError('Không thể tải danh sách khóa học phù hợp để đề xuất. Vui lòng tải lại.');
    }
    setLoading(false);
  };

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    setAction(initialAction);
  }, [view]);

  // Tab counts
  const tabCounts = useMemo(() => {
    const counts = { ALL: requests.length };
    for (const item of requests) {
      counts[item.status] = (counts[item.status] || 0) + 1;
    }
    return counts;
  }, [requests]);

  const statusFilterOptions = useMemo(() => views.map((item) => ({
    ...item,
    buttonLabel: item.label,
    description: `${tabCounts[item.value] || 0} hồ sơ`,
  })), [tabCounts]);

  // Stats computation for top cards
  const stats = useMemo(() => ({
    total: requests.length,
    submitted: requests.filter((r) => r.status === 'SUBMITTED').length,
    testScheduled: requests.filter((r) => r.status === 'TEST_SCHEDULED').length,
    waitingForClass: requests.filter((r) => r.status === 'WAITING_FOR_CLASS').length,
  }), [requests]);

  const filteredRequests = useMemo(() => {
    const normalized = keyword.trim().toLocaleLowerCase('vi-VN');
    return requests.filter((item) => {
      const matchesView = view === 'ALL' || item.status === view;
      const matchesKeyword = !normalized || [
        item.learnerName,
        item.learnerEmail,
        item.contactName,
        item.contactEmail,
        item.contactPhone,
        item.courseOfferingTitle,
        item.consultationTrack,
        item.studyWorkGoal,
        item.preferredSchedule,
        item.testLocation,
        item.id,
      ].filter(Boolean).join(' ').toLocaleLowerCase('vi-VN').includes(normalized);

      const matchesTrack = trackFilter === 'ALL' || item.consultationTrack === trackFilter;
      const matchesSource = sourceFilter === 'ALL' || (item.requestSource || 'ONLINE') === sourceFilter;

      return matchesView && matchesKeyword && matchesTrack && matchesSource;
    });
  }, [keyword, requests, sourceFilter, trackFilter, view]);

  const resetKey = `${keyword}|${view}|${trackFilter}|${sourceFilter}`;
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    filteredRequests,
    10,
    resetKey,
  );

  const applyTransition = (updated) => {
    setRequests((current) => current.map((item) => (item.id === updated.id ? updated : item)));
    setDetailRequest((current) => (current?.id === updated.id ? updated : current));
  };

  const runAction = async (operation, successMessage, fallbackMessage) => {
    setWorking(true);
    setError('');
    try {
      const updated = await operation();
      applyTransition(updated);
      setSuccess(successMessage(updated));
      setAction(initialAction);
    } catch (requestError) {
      setError(requestError?.response?.data?.message || fallbackMessage);
    } finally {
      setWorking(false);
    }
  };

  const openAction = async (type, item) => {
    setError('');
    setAction({
      ...initialAction,
      type,
      item,
      note: item.staffNote || '',
    });
    if (type !== 'ASSIGN') {
      setAssignmentAvailability({ loading: false, ids: null });
      return;
    }
    setAssignmentAvailability({ loading: true, ids: null });
    try {
      const availableIds = await enrollmentRequestApi.getAvailableClassroomIds(item.id);
      setAssignmentAvailability({ loading: false, ids: new Set(availableIds.map(String)) });
    } catch (availabilityError) {
      setAssignmentAvailability({ loading: false, ids: new Set() });
      setClassroomLoadError('Không thể kiểm tra lớp phù hợp cho học viên. Vui lòng tải lại.');
    }
  };

  const openActionFromDetail = async (type, item) => {
    setDetailRequest(null);
    await openAction(type, item);
  };

  const scheduleTest = () => {
    const appointmentAt = combineLocalDateTime(action.appointmentDate, action.appointmentTime);
    if (!appointmentAt || !action.location.trim()) {
      setError('Vui lòng nhập đủ ngày, giờ và địa điểm test.');
      return;
    }
    runAction(
      () => enrollmentRequestApi.scheduleTest(action.item.id, {
        appointmentAt,
        location: action.location.trim(),
        note: action.note.trim() || null,
      }),
      (updated) => `Đã chốt lịch test cho ${updated.contactName || updated.learnerName} và gửi email xác nhận.`,
      'Không thể xếp lịch test.',
    );
  };

  const completeTest = () => {
    if (action.eligible === 'false' && !action.note.trim()) {
      setError('Vui lòng ghi rõ lý do học viên chưa đủ điều kiện.');
      return;
    }
    runAction(
      () => enrollmentRequestApi.completeTest(action.item.id, {
        eligible: action.eligible === 'true',
        recommendedCourseOfferingId: action.eligible === 'false' && action.recommendedCourseOfferingId
          ? Number(action.recommendedCourseOfferingId)
          : null,
        note: action.note.trim() || null,
      }),
      (updated) => {
        if (updated.status === 'WAITING_FOR_CLASS') {
          return `Đã gửi kết quả và chuyển ${updated.contactName || updated.learnerName} sang chờ xếp lớp.`;
        }
        if (updated.status === 'CLASS_PROPOSED') {
          return `Đã gửi khóa học đề xuất để ${updated.contactName || updated.learnerName} xác nhận.`;
        }
        return `Đã gửi kết quả đánh giá cho ${updated.contactName || updated.learnerName}.`;
      },
      'Không thể ghi nhận kết quả test.',
    );
  };

  const assignClass = () => {
    if (!action.classroomId) {
      setError('Vui lòng chọn lớp phù hợp cho học viên.');
      return;
    }
    runAction(
      () => enrollmentRequestApi.assignClass(action.item.id, {
        classroomId: Number(action.classroomId),
        note: action.note.trim() || null,
      }),
      (updated) => `Đã chọn lớp và gửi hướng dẫn thanh toán tới ${updated.contactName || updated.learnerName}.`,
      'Không thể xếp lớp cho học viên.',
    );
  };

  const reject = () => {
    if (!action.reason.trim()) {
      setError('Lý do kết thúc hồ sơ là bắt buộc.');
      return;
    }
    runAction(
      () => enrollmentRequestApi.reject(action.item.id, action.reason.trim()),
      (updated) => `Đã kết thúc hồ sơ #${updated.id}.`,
      'Không thể kết thúc hồ sơ.',
    );
  };

  const createAtCenter = async (payload) => {
    setWorking(true);
    setError('');
    try {
      const created = await enrollmentRequestApi.createAtCenter(payload);
      setCenterEnrollmentOpen(false);
      await load();
      setSuccess(
        created.learnerAccountCreated
          ? `Đã tạo tài khoản, giữ chỗ và gửi hướng dẫn thanh toán cho ${created.learnerName}.`
          : `Đã giữ chỗ và gửi hướng dẫn thanh toán cho ${created.learnerName}.`,
      );
    } catch (requestError) {
      setError(requestError?.response?.data?.message || 'Không thể ghi danh học viên tại trung tâm.');
    } finally {
      setWorking(false);
    }
  };

  const confirmHandlers = {
    SCHEDULE: scheduleTest,
    COMPLETE_TEST: completeTest,
    ASSIGN: assignClass,
    REJECT: reject,
  };

  return (
    <div className="space-y-5">
      {/* Top Notifications */}
      {error && !action.type && !centerEnrollmentOpen ? (
        <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-800">{error}</div>
      ) : null}
      {classroomLoadError ? <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-800">{classroomLoadError}</div> : null}
      <ManagementToast message={success} onClose={() => setSuccess('')} tone="success" title="Đã cập nhật hồ sơ" />

      {/* Metric Cards Summary */}
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard icon={BookOpenCheck} label="Hồ sơ phụ trách" value={stats.total} />
        <MetricCard icon={UserPlus} label="Mới đăng ký" value={stats.submitted} />
        <MetricCard icon={CalendarClock} label="Đã hẹn test" value={stats.testScheduled} />
        <MetricCard icon={UserRoundCheck} label="Đủ điều kiện" value={stats.waitingForClass} />
      </section>

      {/* Filter and Action Bar */}
      <section className="flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="relative min-w-[240px] flex-1">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            className={`${inputClass} h-11 pl-10`}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="Tìm học viên, email, SĐT, khóa học..."
            value={keyword}
          />
        </div>
        <div className="w-full sm:w-52">
          <BrandedSelect
            onChange={(event) => setView(event.target.value)}
            options={statusFilterOptions}
            value={view}
          />
        </div>
        <div className="w-full sm:w-52">
          <BrandedSelect
            onChange={(event) => setTrackFilter(event.target.value)}
            options={trackFilterOptions}
            value={trackFilter}
          />
        </div>
        <div className="w-full sm:w-48">
          <BrandedSelect
            onChange={(event) => setSourceFilter(event.target.value)}
            options={sourceFilterOptions}
            value={sourceFilter}
          />
        </div>
        <div className="flex items-center gap-2">
          <button
            aria-label="Tải lại danh sách"
            className="inline-flex h-11 w-11 items-center justify-center rounded-xl border border-slate-200 text-[#730014] transition hover:bg-slate-50 active:scale-95"
            disabled={loading}
            onClick={load}
            type="button"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            className="inline-flex h-11 items-center gap-2 rounded-xl bg-[#4b0009] px-5 text-xs font-extrabold text-white transition hover:bg-[#730014] active:scale-95 whitespace-nowrap shadow-sm"
            onClick={() => { setError(''); setCenterEnrollmentOpen(true); }}
            type="button"
          >
            <UserPlus className="h-4 w-4" />
            Ghi danh tại trung tâm
          </button>
        </div>
      </section>

      {/* Initial Loading Skeleton (only on initial page load without existing data) */}
      {loading && !requests.length ? (
        <div className="space-y-3">
          <div className="h-12 animate-pulse rounded-xl bg-slate-100" />
          <div className="h-48 animate-pulse rounded-xl bg-slate-100" />
        </div>
      ) : null}

      {/* Table List with Smooth Transition on Tab Change */}
      {(!loading || requests.length > 0) && pageItems.length ? (
        <section
          className={`overflow-hidden rounded-xl border border-[#dfbfbd]/40 bg-white shadow-sm transition-all duration-300 ease-out ${
            loading ? 'opacity-50 pointer-events-none scale-[0.998]' : 'opacity-100 scale-100'
          }`}
        >
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1120px] text-left text-sm">
              <thead className="border-b border-[#dfbfbd]/30 bg-[#fbf3f4] text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">
                <tr>
                  <th className="w-16 px-5 py-4">Mã</th>
                  <th className="w-64 px-5 py-4">Học viên / Liên hệ</th>
                  <th className="w-64 px-5 py-4">Khóa học quan tâm</th>
                  <th className="min-w-[220px] px-5 py-4">Lịch test</th>
                  <th className="w-40 px-5 py-4">Ngày đăng ký</th>
                  <th className="w-44 px-5 py-4 text-center">Trạng thái</th>
                  <th className="w-56 px-5 py-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#dfbfbd]/20">
                {pageItems.map((item) => {
                  const actions = getEnrollmentRequestActions(item.status);
                  return (
                    <tr className="transition hover:bg-[#fffafb]" key={item.id}>
                      <td className="px-5 py-4 font-bold text-slate-400">
                        <span>#{item.id}</span>
                        {item.requestSource === 'CENTER' ? (
                          <span className="mt-1 block text-[10px] font-extrabold uppercase tracking-wide text-[#8a0018]">
                            Tại trung tâm
                          </span>
                        ) : null}
                      </td>
                      <td className="px-5 py-4">
                        <p className="font-extrabold text-[#2b2828]">{item.contactName || item.learnerName || 'Học viên'}</p>
                        <p className="mt-0.5 text-xs text-slate-500">{item.contactEmail || item.learnerEmail}</p>
                        <p className="mt-0.5 text-xs font-bold text-slate-600">{item.contactPhone || 'Chưa có SĐT'}</p>
                      </td>
                      <td className="px-5 py-4">
                        <p className="font-bold text-[#4b0009]">{item.courseOfferingTitle || 'Dữ liệu cũ chưa chọn khóa học'}</p>
                        <p className="mt-1 text-xs text-slate-500">{formatConsultationTrack(item.consultationTrack)}</p>
                      </td>
                      <td className="px-5 py-4 text-xs leading-5 text-slate-600">
                        {item.requestSource === 'CENTER' ? (
                          <p className="font-semibold text-slate-500">Không áp dụng</p>
                        ) : item.testAppointmentAt ? (
                          <p className="font-bold text-slate-800">{formatClassroomDateTime(item.testAppointmentAt)}</p>
                        ) : (
                          <p className="font-semibold text-slate-400">Chưa chốt lịch test</p>
                        )}
                        {item.testLocation ? <p>{item.testLocation}</p> : null}
                      </td>
                      <td className="px-5 py-4 text-xs text-slate-500">{formatClassroomDateTime(item.createdAt)}</td>
                      <td className="px-5 py-4 text-center">
                        <EnrollmentStatusBadge label={item.statusLabel} status={item.status} />
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex flex-wrap justify-end gap-1.5">
                          <ActionButton icon={Eye} label="Xem chi tiết" neutral onClick={() => setDetailRequest(item)} />
                          {actions.canSchedule ? (
                            <ActionButton icon={CalendarClock} label="Xếp lịch & gửi email" onClick={() => openAction('SCHEDULE', item)} />
                          ) : null}
                          {actions.canCompleteTest ? (
                            <ActionButton icon={CheckCircle2} label="Ghi kết quả" onClick={() => openAction('COMPLETE_TEST', item)} />
                          ) : null}
                          {actions.canAssign ? (
                            <ActionButton icon={UserRoundCheck} label="Xếp lớp" onClick={() => openAction('ASSIGN', item)} success />
                          ) : null}
                          {actions.canReject ? (
                            <ActionButton danger icon={XCircle} label="Kết thúc" onClick={() => openAction('REJECT', item)} />
                          ) : null}
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

      {detailRequest ? (
        <EnrollmentRequestDetailModal
          item={detailRequest}
          onAction={openActionFromDetail}
          onClose={() => setDetailRequest(null)}
        />
      ) : null}

      {action.type ? (
        <ActionModal
          action={action}
          assignmentAvailability={assignmentAvailability}
          classroomLoadError={classroomLoadError}
          classrooms={classrooms}
          courseOfferings={courseOfferings}
          courseOfferingLoadError={courseOfferingLoadError}
          error={error}
          onChange={setAction}
          onClose={() => setAction(initialAction)}
          onConfirm={confirmHandlers[action.type]}
          working={working}
        />
      ) : null}

      {centerEnrollmentOpen ? (
        <CenterEnrollmentModal
          classroomLoadError={classroomLoadError}
          classrooms={classrooms}
          error={error}
          onClose={() => setCenterEnrollmentOpen(false)}
          onSubmit={createAtCenter}
          working={working}
        />
      ) : null}
    </div>
  );
}

function ActionButton({ danger = false, icon: Icon, label, modal = false, neutral = false, onClick, success = false }) {
  const tone = danger
    ? 'border border-rose-200 text-rose-700 hover:bg-rose-50'
    : success
      ? 'bg-emerald-700 text-white hover:bg-emerald-800'
      : neutral
        ? 'border border-slate-200 bg-white text-slate-700 hover:border-[#dfbfbd] hover:bg-[#fff4f5] hover:text-[#730014]'
        : 'bg-[#4b0009] text-white hover:bg-[#730014]';
  return (
    <button
      className={`inline-flex items-center justify-center gap-1.5 rounded-lg text-xs font-bold transition active:scale-95 ${modal ? 'h-10 px-4' : 'px-2.5 py-1.5'} ${tone}`}
      onClick={onClick}
      type="button"
    >
      <Icon className={modal ? 'h-4 w-4' : 'h-3.5 w-3.5'} />
      {label}
    </button>
  );
}

function EnrollmentRequestDetailModal({ item, onAction, onClose }) {
  const history = item.history || [];
  const actions = getEnrollmentRequestActions(item.status);
  const hasActions = actions.canSchedule || actions.canCompleteTest || actions.canAssign || actions.canReject;
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') onClose();
    };
    document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section aria-labelledby="enrollment-detail-title" aria-modal="true" className="flex max-h-[92vh] w-full max-w-3xl flex-col overflow-hidden rounded-3xl bg-white shadow-2xl" role="dialog">
        <div className="flex shrink-0 items-start justify-between gap-4 border-b border-slate-100 bg-white p-6">
          <div>
            <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Chi tiết hồ sơ đăng ký</p>
            <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]" id="enrollment-detail-title">
              {item.contactName || item.learnerName || 'Học viên'} · #{item.id}
            </h2>
            <p className="mt-1 text-xs text-[#8b706e]">{item.statusLabel || item.status}</p>
          </div>
          <button aria-label="Đóng" autoFocus className="rounded-xl p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700" onClick={onClose} type="button">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="min-h-0 flex-1 space-y-5 overflow-y-auto bg-slate-50/40 p-6">
          <div className="grid gap-4 md:grid-cols-2">
            <DetailSection title="Thông tin liên hệ">
              <DetailRow label="Họ tên" value={item.contactName || item.learnerName} />
              <DetailRow label="Email" value={item.contactEmail || item.learnerEmail} />
              <DetailRow label="Số điện thoại" value={item.contactPhone} />
              <DetailRow label="Nguồn đăng ký" value={item.requestSource === 'CENTER' ? 'Tại trung tâm' : 'Online'} />
            </DetailSection>
            <DetailSection title="Nhu cầu học tập">
              <DetailRow label="Khóa học" value={item.courseOfferingTitle} />
              <DetailRow label="Chương trình" value={formatConsultationTrack(item.consultationTrack)} />
              <DetailRow label="Mục tiêu" value={item.studyWorkGoal} />
              <DetailRow label="Lịch mong muốn" value={item.preferredSchedule} />
            </DetailSection>
          </div>

          {item.requestSource === 'CENTER' ? (
            <DetailSection title="Ghi danh tại trung tâm">
              <div className="grid gap-3 sm:grid-cols-2">
                <DetailRow label="Xử lý" value="Đã chọn lớp và chuyển sang thanh toán" />
                <DetailRow label="Ngày ghi danh" value={formatClassroomDateTime(item.createdAt)} />
              </div>
            </DetailSection>
          ) : (
            <DetailSection title="Lịch đánh giá tại trung tâm">
              <div className="grid gap-3 sm:grid-cols-2">
                <DetailRow label="Thời gian" value={item.testAppointmentAt ? formatClassroomDateTime(item.testAppointmentAt) : 'Chưa chốt lịch'} />
                <DetailRow label="Địa điểm" value={item.testLocation} />
                <DetailRow label="Trình độ xác nhận" value={formatPlacementLevel(item.confirmedLevel)} />
                <DetailRow label="Ngày đăng ký" value={formatClassroomDateTime(item.createdAt)} />
              </div>
            </DetailSection>
          )}

          {item.latestPlacementResult ? <PlacementScoreSummary detailed result={item.latestPlacementResult} /> : null}

          {item.learnerNote || item.staffNote || item.rejectionReason ? (
            <DetailSection title="Ghi chú">
              {item.learnerNote ? <DetailRow label="Học viên" value={item.learnerNote} /> : null}
              {item.staffNote ? <DetailRow label="Nhân viên" value={item.staffNote} /> : null}
              {item.rejectionReason ? <DetailRow label="Lý do kết thúc" value={item.rejectionReason} /> : null}
            </DetailSection>
          ) : null}

          <DetailSection title="Lịch sử xử lý">
            {history.length ? (
              <div className="space-y-3">
                {history.map((entry) => (
                  <div className="border-l-2 border-[#dfbfbd] pl-4" key={entry.id}>
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <p className="text-sm font-bold text-[#0b1c30]">{entry.statusLabel || entry.toStatus}</p>
                      <p className="text-xs text-slate-500">{formatClassroomDateTime(entry.createdAt)}</p>
                    </div>
                    {entry.actorName ? <p className="mt-1 text-xs text-slate-500">Thực hiện bởi {entry.actorName}</p> : null}
                    {entry.reason ? <p className="mt-1 whitespace-pre-wrap text-sm leading-6 text-slate-700">{entry.reason}</p> : null}
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-slate-500">Chưa có lịch sử xử lý.</p>
            )}
          </DetailSection>
        </div>

        <div className="flex shrink-0 flex-col-reverse gap-3 border-t border-slate-100 bg-white px-6 py-4 sm:flex-row sm:items-center sm:justify-between">
          <button className={`${SECONDARY_BUTTON_CLASS} justify-center`} onClick={onClose} type="button">
            Đóng
          </button>
          {hasActions ? (
            <div className="flex flex-wrap justify-end gap-2">
              {actions.canSchedule ? (
                <ActionButton icon={CalendarClock} label="Xếp lịch & gửi email" modal onClick={() => onAction('SCHEDULE', item)} />
              ) : null}
              {actions.canCompleteTest ? (
                <ActionButton icon={CheckCircle2} label="Ghi kết quả" modal onClick={() => onAction('COMPLETE_TEST', item)} />
              ) : null}
              {actions.canAssign ? (
                <ActionButton icon={UserRoundCheck} label="Xếp lớp" modal onClick={() => onAction('ASSIGN', item)} success />
              ) : null}
              {actions.canReject ? (
                <ActionButton danger icon={XCircle} label="Kết thúc" modal onClick={() => onAction('REJECT', item)} />
              ) : null}
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}

function DetailSection({ children, title }) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">{title}</h3>
      <div className="mt-3 space-y-2">{children}</div>
    </section>
  );
}

function DetailRow({ label, value }) {
  return (
    <div className="grid gap-1 text-sm sm:grid-cols-[130px_1fr]">
      <span className="font-bold text-slate-500">{label}</span>
      <span className="whitespace-pre-wrap font-semibold text-slate-800">{value || 'Chưa có thông tin'}</span>
    </div>
  );
}

function PlacementScoreSummary({ detailed = false, result }) {
  const scores = [
    ['Nghe', result.listeningScore],
    ['Đọc', result.readingScore],
    ['Viết', result.writingScore],
    ['Nói', result.speakingScore],
  ].filter(([, score]) => score !== null && score !== undefined);
  return (
    <div className="mt-3 rounded-xl border border-sky-100 bg-sky-50/70 p-3">
      <p className="mb-2 text-[10px] font-extrabold uppercase tracking-[0.12em] text-sky-700">Placement online gần nhất · Tham khảo</p>
      <div className="flex flex-wrap items-center gap-2">
        <span className="font-extrabold text-[#0b1c30]">{result.examType || 'Placement test'}</span>
        {result.overallScore !== null && result.overallScore !== undefined ? (
          <span className="rounded-full bg-white px-2.5 py-0.5 font-extrabold text-sky-800">
            Tổng {result.overallScore}
          </span>
        ) : null}
      </div>
      {scores.length ? (
        <div className={`mt-2 grid gap-2 ${detailed ? 'grid-cols-2 sm:grid-cols-4' : 'grid-cols-2'}`}>
          {scores.map(([label, score]) => (
            <span className="rounded-lg bg-white px-2.5 py-1.5 font-bold text-slate-700 shadow-sm" key={label}>
              {label}: {score}
            </span>
          ))}
        </div>
      ) : null}
      {result.submittedAt ? (
        <p className="mt-2 text-[11px] text-slate-500">Nộp lúc {formatClassroomDateTime(result.submittedAt)}</p>
      ) : null}
    </div>
  );
}

function ActionModal({ action, assignmentAvailability, classroomLoadError, classrooms, courseOfferingLoadError, courseOfferings, error, onChange, onClose, onConfirm, working }) {
  const titles = {
    SCHEDULE: ['Xác nhận lịch hẹn', 'Chọn ngày, giờ và địa điểm. Email xác nhận được gửi cùng lịch hẹn.'],
    COMPLETE_TEST: ['Ghi nhận kết quả đầu vào', 'Nhập kết quả thực tế của buổi đánh giá trực tiếp tại trung tâm.'],
    ASSIGN: ['Chọn lớp và mời thanh toán', 'Chọn lớp thuộc khóa học đã xác nhận. Học viên sẽ nhận hướng dẫn hoàn tất học phí.'],
    REJECT: ['Kết thúc hồ sơ', 'Dùng khi học viên từ chối tiếp tục hoặc hồ sơ không thể xử lý.'],
  };
  const [title, description] = titles[action.type];
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm">
      <section aria-modal="true" className="flex max-h-[90vh] w-full max-w-xl flex-col overflow-hidden rounded-3xl bg-white shadow-2xl" role="dialog">
        <div className="flex items-start justify-between gap-4 border-b border-slate-100 p-6 shrink-0 bg-white">
          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">{title}</p>
            <h2 className="mt-2 text-xl font-black text-[#0b1c30]">{action.item.contactName || action.item.learnerName} · #{action.item.id}</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">{description}</p>
          </div>
          <button className="rounded-lg p-2 text-slate-400" onClick={onClose} type="button">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto p-6 space-y-4">
          {error ? <div className={ERROR_NOTICE_CLASS} role="alert">{error}</div> : null}
          {action.type === 'SCHEDULE' ? (
            <div className="grid gap-4 sm:grid-cols-2">
              <label>
                <FieldLabel>Ngày đến test *</FieldLabel>
                <VietnameseDateInput className={FIELD_CLASS} min={new Date().toISOString().slice(0, 10)} onChange={(value) => onChange({ ...action, appointmentDate: value })} required value={action.appointmentDate} />
              </label>
              <label>
                <FieldLabel>Giờ đến test *</FieldLabel>
                <input className={FIELD_CLASS} onChange={(event) => onChange({ ...action, appointmentTime: event.target.value })} required type="time" value={action.appointmentTime} />
              </label>
              <label className="sm:col-span-2">
                <FieldLabel>Địa điểm *</FieldLabel>
                <input className={FIELD_CLASS} onChange={(event) => onChange({ ...action, location: event.target.value })} value={action.location} />
              </label>
            </div>
          ) : null}

          {action.type === 'COMPLETE_TEST' && action.item.latestPlacementResult ? (
            <PlacementScoreSummary detailed result={action.item.latestPlacementResult} />
          ) : null}

          {action.type === 'COMPLETE_TEST' ? (
            <div className="space-y-4">
              <div>
                <FieldLabel>Kết quả *</FieldLabel>
                <BrandedSelect
                  onChange={(event) => onChange({
                    ...action,
                    eligible: event.target.value,
                    recommendedCourseOfferingId: event.target.value === 'true' ? '' : action.recommendedCourseOfferingId,
                  })}
                  options={[
                    { label: 'Phù hợp với khóa học đã đăng ký', value: 'true' },
                    { label: 'Cần chuyển sang khóa học khác', value: 'false' },
                  ]}
                  value={action.eligible}
                />
              </div>
              {action.eligible === 'false' ? (
                <div>
                  <FieldLabel>Khóa học phù hợp đề xuất</FieldLabel>
                  <BrandedSelect
                    onChange={(event) => onChange({ ...action, recommendedCourseOfferingId: event.target.value })}
                    options={courseOfferings
                      .filter((item) => String(item.id) !== String(action.item.courseOfferingId))
                      .map((item) => ({
                        label: item.title,
                        value: String(item.id),
                        description: item.entryLevel || item.code || '',
                      }))}
                    placeholder="Không đề xuất khóa học khác"
                    searchable
                    value={action.recommendedCourseOfferingId}
                  />
                  <p className="mt-2 text-xs leading-5 text-slate-500">
                    Nếu chọn khóa khác, học viên phải xác nhận trước khi hồ sơ được chuyển sang chờ xếp lớp.
                  </p>
                  {courseOfferingLoadError ? (
                    <p className="mt-2 text-xs font-semibold text-rose-600">{courseOfferingLoadError}</p>
                  ) : null}
                </div>
              ) : null}
            </div>
          ) : null}

          {action.type === 'ASSIGN' ? (
            <div>
              <FieldLabel>Lớp phù hợp *</FieldLabel>
              <BrandedSelect
                disabled={assignmentAvailability.loading}
                onChange={(event) => onChange({ ...action, classroomId: event.target.value })}
                options={classrooms.filter((item) => assignmentAvailability.ids?.has(String(item.id))).map((item) => ({
                  value: String(item.id),
                  label: item.title,
                  description: `${item.instructorLedCourseTitle || 'Chưa gắn khóa học'} · Đầu vào ${item.entryLevel || 'chưa xác định'} · Mục tiêu ${item.targetScore || 'chưa xác định'} · ${formatClassroomDate(item.startDate)} · ${item.enrolledCount || 0} học viên`,
                }))}
                placeholder={assignmentAvailability.loading ? 'Đang loại lớp trùng lịch...' : assignmentAvailability.ids?.size ? 'Chọn lớp không trùng lịch' : 'Chưa có lớp phù hợp'}
                searchable
                value={action.classroomId}
              />
              {!assignmentAvailability.loading && assignmentAvailability.ids && !assignmentAvailability.ids.size ? <p className="mt-3 rounded-xl bg-amber-50 px-4 py-3 text-sm font-semibold leading-6 text-amber-800">{classroomLoadError || 'Không có lớp nào vừa còn chỗ, chưa ghi danh và không trùng lịch hiện tại của học viên.'}</p> : null}
            </div>
          ) : null}

          <label className="block">
            <FieldLabel>{action.type === 'REJECT' ? 'Lý do kết thúc *' : action.type === 'COMPLETE_TEST' && action.eligible === 'false' ? 'Lý do chưa đủ điều kiện *' : action.type === 'COMPLETE_TEST' ? 'Nhận xét đánh giá tại trung tâm' : 'Ghi chú nội bộ'}</FieldLabel>
            <textarea className={TEXTAREA_CLASS} onChange={(event) => onChange({ ...action, [action.type === 'REJECT' ? 'reason' : 'note']: event.target.value })} placeholder="Nội dung chỉ hiển thị trong khu vực vận hành." rows={4} value={action.type === 'REJECT' ? action.reason : action.note} />
          </label>

          <div className="flex justify-end gap-2 pt-2">
            <button className={SECONDARY_BUTTON_CLASS} disabled={working} onClick={onClose} type="button">Đóng</button>
            <button className={action.type === 'REJECT' ? 'inline-flex items-center gap-2 rounded-xl bg-rose-700 px-5 py-3 text-sm font-extrabold text-white' : PRIMARY_BUTTON_CLASS} disabled={working || (action.type === 'ASSIGN' && !classrooms.length)} onClick={onConfirm} type="button">
              <Send className="h-4 w-4" />{working ? 'Đang xử lý...' : title}
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}

const initialCenterEnrollment = {
  fullName: '',
  email: '',
  phoneNumber: '',
  classroomId: '',
  note: '',
};

function CenterEnrollmentModal({ classroomLoadError, classrooms, error, onClose, onSubmit, working }) {
  const [form, setForm] = useState(initialCenterEnrollment);
  const [validationError, setValidationError] = useState('');
  const [accountLookup, setAccountLookup] = useState({ status: 'idle', message: '' });
  const accountResolved = ['existing', 'new'].includes(accountLookup.status);
  const unavailableCourseIds = new Set((accountLookup.unavailableCourseIds || []).map(String));
  const unavailableClassroomIds = new Set((accountLookup.unavailableClassroomIds || []).map(String));
  const availableClassrooms = accountResolved
    ? classrooms.filter((item) => (
      !unavailableCourseIds.has(String(item.instructorLedCourseId))
      && !unavailableClassroomIds.has(String(item.id))
    ))
    : [];

  useEffect(() => {
    const email = form.email.trim().toLowerCase();
    if (!email || !email.includes('@')) {
      setAccountLookup({ status: 'idle', message: '' });
      return undefined;
    }

    let active = true;
    setAccountLookup({ status: 'loading', message: '' });
    const timer = window.setTimeout(async () => {
      try {
        const result = await enrollmentRequestApi.findCenterEnrollmentLearner(email);
        if (!active) return;
        if (result?.existingAccount) {
          setForm((current) => current.email.trim().toLowerCase() === email
            ? {
              ...current,
              fullName: result.fullName || '',
              phoneNumber: result.phoneNumber || '',
            }
            : current);
          setAccountLookup({
            status: 'existing',
            message: result.phoneNumber
              ? 'Đã tìm thấy tài khoản học viên.'
              : 'Đã tìm thấy tài khoản. Vui lòng bổ sung số điện thoại.',
            unavailableCourseIds: result.unavailableCourseIds || [],
            unavailableClassroomIds: result.unavailableClassroomIds || [],
          });
          return;
        }
        setForm((current) => current.email.trim().toLowerCase() === email
          ? { ...current, fullName: '', phoneNumber: '' }
          : current);
        setAccountLookup({
          status: 'new',
          message: 'Email chưa có tài khoản. Nhập thông tin để tạo mới.',
          unavailableCourseIds: [],
          unavailableClassroomIds: [],
        });
      } catch (lookupError) {
        if (!active) return;
        setAccountLookup({
          status: 'error',
          message: lookupError?.response?.data?.message || 'Không thể kiểm tra email học viên.',
        });
      }
    }, 450);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [form.email]);

  const update = (field, value) => {
    setForm((current) => field === 'email'
      ? { ...current, email: value, fullName: '', phoneNumber: '', classroomId: '' }
      : { ...current, [field]: value });
    setValidationError('');
  };

  const submit = () => {
    if (!form.email.trim() || !accountResolved) {
      setValidationError('Vui lòng nhập và kiểm tra email học viên.');
      return;
    }
    if ((accountLookup.status === 'new' && !form.fullName.trim()) || !form.phoneNumber.trim()) {
      setValidationError('Vui lòng nhập đầy đủ họ tên và số điện thoại.');
      return;
    }
    if (!form.classroomId) {
      setValidationError('Vui lòng chọn lớp học.');
      return;
    }
    onSubmit({
      fullName: form.fullName.trim(),
      email: form.email.trim().toLowerCase(),
      phoneNumber: form.phoneNumber.trim(),
      classroomId: Number(form.classroomId),
      note: form.note.trim() || null,
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm">
      <section aria-labelledby="center-enrollment-title" aria-modal="true" className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl bg-white shadow-2xl" role="dialog">
        <div className="flex items-start justify-between gap-4 border-b border-slate-100 p-6 shrink-0 bg-white">
          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">Ghi danh tại trung tâm</p>
            <h2 className="mt-2 text-xl font-black text-[#0b1c30]" id="center-enrollment-title">Kiểm tra tài khoản và xếp lớp</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">Nhập email học viên trước khi chọn lớp.</p>
          </div>
          <button aria-label="Đóng" className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-100" disabled={working} onClick={onClose} type="button">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto p-6 space-y-4">
          {error ? <div className={ERROR_NOTICE_CLASS} role="alert">{error}</div> : null}
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="sm:col-span-2">
              <FieldLabel>Email đăng nhập *</FieldLabel>
              <input autoFocus autoComplete="off" className={FIELD_CLASS} maxLength={150} onChange={(event) => update('email', event.target.value)} type="email" value={form.email} />
              {accountLookup.status === 'loading' ? (
                <p className="mt-2 text-xs font-semibold text-slate-500">Đang kiểm tra tài khoản...</p>
              ) : accountLookup.message ? (
                <p className={`mt-2 text-xs font-semibold ${accountLookup.status === 'error' ? 'text-rose-600' : 'text-slate-500'}`}>
                  {accountLookup.message}
                </p>
              ) : null}
            </label>
            <label>
              <FieldLabel>Họ và tên *</FieldLabel>
              <input className={FIELD_CLASS} disabled={accountLookup.status !== 'new'} maxLength={100} onChange={(event) => update('fullName', event.target.value)} value={form.fullName} />
            </label>
            <label>
              <FieldLabel>Số điện thoại *</FieldLabel>
              <input className={FIELD_CLASS} disabled={!accountResolved || (accountLookup.status === 'existing' && Boolean(form.phoneNumber))} inputMode="tel" maxLength={30} onChange={(event) => update('phoneNumber', event.target.value)} value={form.phoneNumber} />
            </label>
            <div className="sm:col-span-2">
              <FieldLabel>Lớp học *</FieldLabel>
              <BrandedSelect
                disabled={!accountResolved}
                onChange={(event) => update('classroomId', event.target.value)}
                options={availableClassrooms.map((item) => ({
                  value: String(item.id),
                  label: item.title,
                  description: `${formatClassroomDate(item.startDate)} · ${item.enrolledCount || 0} học viên`,
                }))}
                placeholder={availableClassrooms.length ? 'Chọn lớp sắp hoặc đang khai giảng' : 'Chưa có lớp phù hợp'}
                searchable
                value={form.classroomId}
              />
            </div>
          </div>

          <label className="block">
            <FieldLabel>Ghi chú</FieldLabel>
            <textarea className={TEXTAREA_CLASS} maxLength={700} onChange={(event) => update('note', event.target.value)} placeholder="Thông tin cần lưu cùng hồ sơ ghi danh." rows={3} value={form.note} />
          </label>

          {accountResolved && !availableClassrooms.length ? <p className="rounded-xl bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-800">{classroomLoadError || 'Chưa có lớp sắp hoặc đang khai giảng phù hợp.'}</p> : null}
          {validationError ? <p className={ERROR_NOTICE_CLASS}>{validationError}</p> : null}

          <div className="flex justify-end gap-2 border-t border-slate-100 pt-4">
            <button className={SECONDARY_BUTTON_CLASS} disabled={working} onClick={onClose} type="button">Hủy</button>
            <button className={PRIMARY_BUTTON_CLASS} disabled={working || !availableClassrooms.length || !accountResolved} onClick={submit} type="button">
              <UserRoundCheck className="h-4 w-4" />{working ? 'Đang ghi danh...' : 'Ghi danh & gửi thanh toán'}
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}

function MetricCard({ icon: Icon, label, value }) {
  return (
    <article className="rounded-xl border border-[#dfbfbd]/35 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">{label}</p>
          <p className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{value}</p>
        </div>
        <span className="rounded-xl bg-[#fff1f3] p-2.5 text-[#730014]">
          <Icon className="h-5 w-5" />
        </span>
      </div>
    </article>
  );
}

function formatConsultationTrack(value) {
  return {
    IELTS_4_SKILLS: 'IELTS 4 kỹ năng',
    TOEIC_2_SKILLS: 'TOEIC Listening & Reading',
    TOEIC_4_SKILLS: 'TOEIC 4 kỹ năng',
    ENGLISH_FOUNDATION: 'Tiếng Anh nền tảng',
  }[value] || value || 'Chưa chọn';
}

function formatPlacementLevel(value) {
  return {
    BEGINNER: 'Cơ bản',
    INTERMEDIATE: 'Trung cấp',
    ADVANCED: 'Nâng cao',
  }[value] || value || 'Chưa xác nhận';
}

function FieldLabel({ children }) {
  return <span className="mb-2 block text-xs font-bold uppercase tracking-[0.1em] text-slate-500">{children}</span>;
}

function EmptyState() {
  return (
    <section className="flex min-h-[360px] flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center">
      <BookOpenCheck className="h-12 w-12 text-slate-300" />
      <h2 className="mt-4 text-xl font-black text-[#0b1c30]">Không có hồ sơ được phân công</h2>
      <p className="mt-2 text-sm text-slate-500">Không có học viên nào phù hợp với bộ lọc hiện tại.</p>
    </section>
  );
}

const inputClass = 'w-full rounded-xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-3 py-2.5 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white placeholder:text-[#8b706e]';
