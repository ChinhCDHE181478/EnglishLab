import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  BookOpenCheck,
  CalendarClock,
  CheckCircle2,
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
  { label: 'Tất cả', value: 'ALL' },
  { label: 'Mới đăng ký', value: 'SUBMITTED' },
  { label: 'Đã hẹn test', value: 'TEST_SCHEDULED' },
  { label: 'Đủ điều kiện', value: 'WAITING_FOR_CLASS' },
  { label: 'Hoàn tất', value: 'CLASS_ASSIGNED' },
  { label: 'Không phù hợp', value: 'REJECTED' },
];

const placementOptions = [
  { label: 'Cơ bản', value: 'BEGINNER' },
  { label: 'Trung cấp', value: 'INTERMEDIATE' },
  { label: 'Nâng cao', value: 'ADVANCED' },
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
  note: '',
  reason: '',
  appointmentDate: '',
  appointmentTime: '',
  location: 'EnglishLab Campus, Hà Nội',
  eligible: 'true',
  placementLevel: '',
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
  const [keyword, setKeyword] = useState('');
  const [trackFilter, setTrackFilter] = useState('ALL');
  const [sourceFilter, setSourceFilter] = useState('ALL');

  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [classroomLoadError, setClassroomLoadError] = useState('');
  const [success, setSuccess] = useState('');
  const [action, setAction] = useState(initialAction);
  const [assignmentAvailability, setAssignmentAvailability] = useState({ loading: false, ids: null });
  const [centerEnrollmentOpen, setCenterEnrollmentOpen] = useState(false);

  const load = async () => {
    setLoading(true);
    setError('');
    setClassroomLoadError('');
    const result = await loadStaffEnrollmentData(
      () => enrollmentRequestApi.listForStaff(view),
      () => classroomApi.getStaffClassrooms(),
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
      setClassrooms(result.classrooms
        .filter((classroom) => isAssignableClassroom(classroom))
        .sort((left, right) => String(left.startDate).localeCompare(String(right.startDate))));
    }
    setLoading(false);
  };

  useEffect(() => {
    load();
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

      return matchesKeyword && matchesTrack && matchesSource;
    });
  }, [keyword, requests, trackFilter, sourceFilter]);

  const resetKey = `${keyword}|${view}|${trackFilter}|${sourceFilter}`;
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    filteredRequests,
    10,
    resetKey,
  );

  const applyTransition = (updated) => {
    setRequests((current) => (
      view === 'ALL'
        ? current.map((item) => (item.id === updated.id ? updated : item))
        : current.filter((item) => item.id !== updated.id)
    ));
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
    if (action.eligible === 'true' && !action.placementLevel) {
      setError('Vui lòng chọn trình độ phù hợp của học viên.');
      return;
    }
    if (action.eligible === 'false' && !action.note.trim()) {
      setError('Vui lòng ghi rõ lý do học viên chưa đủ điều kiện.');
      return;
    }
    runAction(
      () => enrollmentRequestApi.completeTest(action.item.id, {
        eligible: action.eligible === 'true',
        placementLevel: action.eligible === 'true' ? action.placementLevel : null,
        note: action.note.trim() || null,
      }),
      (updated) => updated.status === 'WAITING_FOR_CLASS'
        ? `Đã xác nhận ${updated.contactName || updated.learnerName} đủ điều kiện học.`
        : `Đã hoàn tất hồ sơ test của ${updated.contactName || updated.learnerName}.`,
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
      (updated) => `Đã xếp lớp và gửi email thông báo tới ${updated.contactName || updated.learnerName}.`,
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
          ? `Đã tạo tài khoản, gửi email thiết lập mật khẩu và xếp ${created.learnerName} vào lớp.`
          : `Đã dùng tài khoản hiện có và xếp ${created.learnerName} vào lớp.`,
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
      {error ? <div className={ERROR_NOTICE_CLASS}>{error}</div> : null}
      {classroomLoadError ? <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-800">{classroomLoadError}</div> : null}
      {success ? <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-700">{success}</div> : null}

      {/* Metric Cards Summary */}
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard icon={BookOpenCheck} label="Tổng hồ sơ" value={stats.total} />
        <MetricCard icon={UserPlus} label="Mới đăng ký" value={stats.submitted} />
        <MetricCard icon={CalendarClock} label="Đã hẹn test" value={stats.testScheduled} />
        <MetricCard icon={UserRoundCheck} label="Đủ điều kiện" value={stats.waitingForClass} />
      </section>

      {/* Status Tabs Bar */}
      <section className="flex flex-wrap items-center gap-2 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
        <div className="flex flex-wrap items-center gap-1.5 min-w-0 flex-1">
          {views.map((item) => {
            const count = item.value === 'ALL' ? requests.length : (tabCounts[item.value] || 0);
            const isActive = view === item.value;
            return (
              <button
                key={item.value}
                type="button"
                onClick={() => setView(item.value)}
                className={`relative inline-flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-extrabold transition-all duration-200 ease-out active:scale-95 ${
                  isActive
                    ? 'bg-[#4b0009] text-white shadow-md shadow-[#4b0009]/15'
                    : 'bg-slate-50 text-slate-600 hover:bg-[#fff0f2] hover:text-[#730014]'
                }`}
              >
                <span>{item.label}</span>
                <span
                  className={`rounded-full px-2 py-0.5 text-[10px] font-extrabold transition-all duration-200 ${
                    isActive ? 'bg-white/20 text-white' : 'bg-slate-200/80 text-slate-700'
                  }`}
                >
                  {count}
                </span>
              </button>
            );
          })}
        </div>
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
            onClick={() => setCenterEnrollmentOpen(true)}
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
            <table className="w-full min-w-[1250px] text-left text-sm">
              <thead className="border-b border-[#dfbfbd]/30 bg-[#fbf3f4] text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">
                <tr>
                  <th className="w-16 px-5 py-4">Mã</th>
                  <th className="w-64 px-5 py-4">Học viên / Liên hệ</th>
                  <th className="w-64 px-5 py-4">Khóa học quan tâm</th>
                  <th className="min-w-[250px] px-5 py-4">Lịch test / Nhu cầu</th>
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
                        {item.testAppointmentAt ? (
                          <p className="font-bold text-slate-800">{formatClassroomDateTime(item.testAppointmentAt)}</p>
                        ) : (
                          <p className="font-semibold text-slate-400">Chưa chốt lịch test</p>
                        )}
                        {item.testLocation ? <p>{item.testLocation}</p> : null}
                        {item.preferredSchedule ? <p className="mt-1">Giờ học mong muốn: {item.preferredSchedule}</p> : null}
                        {item.staffNote ? (
                          <p className="mt-2 rounded-lg bg-amber-50 px-2.5 py-2 text-amber-900">
                            <span className="font-extrabold">Ghi chú xử lý:</span> {item.staffNote}
                          </p>
                        ) : null}
                      </td>
                      <td className="px-5 py-4 text-xs text-slate-500">{formatClassroomDateTime(item.createdAt)}</td>
                      <td className="px-5 py-4 text-center">
                        <EnrollmentStatusBadge label={item.statusLabel} status={item.status} />
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex flex-wrap justify-end gap-1.5">
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

      {action.type ? (
        <ActionModal
          action={action}
          assignmentAvailability={assignmentAvailability}
          classroomLoadError={classroomLoadError}
          classrooms={classrooms}
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
          onClose={() => setCenterEnrollmentOpen(false)}
          onSubmit={createAtCenter}
          working={working}
        />
      ) : null}
    </div>
  );
}

function ActionButton({ danger = false, icon: Icon, label, onClick, success = false }) {
  const tone = danger
    ? 'border border-rose-200 text-rose-700 hover:bg-rose-50'
    : success
      ? 'bg-emerald-700 text-white hover:bg-emerald-800'
      : 'bg-[#4b0009] text-white hover:bg-[#730014]';
  return (
    <button className={`inline-flex items-center gap-1 rounded-lg px-2.5 py-1.5 text-xs font-bold transition ${tone}`} onClick={onClick} type="button">
      <Icon className="h-3.5 w-3.5" />
      {label}
    </button>
  );
}

function ActionModal({ action, assignmentAvailability, classroomLoadError, classrooms, onChange, onClose, onConfirm, working }) {
  const titles = {
    SCHEDULE: ['Xác nhận lịch hẹn', 'Chọn ngày, giờ và địa điểm. Email xác nhận được gửi cùng lịch hẹn.'],
    COMPLETE_TEST: ['Ghi nhận kết quả đầu vào', 'Nhập kết quả thực tế của buổi đánh giá tại trung tâm.'],
    ASSIGN: ['Xếp lớp chính thức', 'Chọn lớp phù hợp theo kết quả test; khóa học học viên quan tâm ban đầu chỉ dùng để tham khảo.'],
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

          {action.type === 'COMPLETE_TEST' ? (
            <div className="space-y-4">
              <div>
                <FieldLabel>Kết quả *</FieldLabel>
                <BrandedSelect onChange={(event) => onChange({ ...action, eligible: event.target.value, note: event.target.value === 'false' ? '' : action.note })} options={[{ label: 'Đủ điều kiện học', value: 'true' }, { label: 'Chưa đủ điều kiện', value: 'false' }]} value={action.eligible} />
              </div>
              {action.eligible === 'true' ? (
                <div>
                  <FieldLabel>Trình độ phù hợp *</FieldLabel>
                  <BrandedSelect onChange={(event) => onChange({ ...action, placementLevel: event.target.value })} options={placementOptions} placeholder="Chọn trình độ" value={action.placementLevel} />
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
                  description: `${item.trainingProgramTitle || 'Chưa gắn khóa học'} · Đầu vào ${item.entryLevel || 'chưa xác định'} · Mục tiêu ${item.targetScore || 'chưa xác định'} · ${formatClassroomDate(item.startDate)} · ${item.enrolledCount || 0}/${item.maxCapacity || '∞'} học viên`,
                }))}
                placeholder={assignmentAvailability.loading ? 'Đang loại lớp trùng lịch...' : assignmentAvailability.ids?.size ? 'Chọn lớp không trùng lịch' : 'Chưa có lớp phù hợp'}
                searchable
                value={action.classroomId}
              />
              {!assignmentAvailability.loading && assignmentAvailability.ids && !assignmentAvailability.ids.size ? <p className="mt-3 rounded-xl bg-amber-50 px-4 py-3 text-sm font-semibold leading-6 text-amber-800">{classroomLoadError || 'Không có lớp nào vừa còn chỗ, chưa ghi danh và không trùng lịch hiện tại của học viên.'}</p> : null}
            </div>
          ) : null}

          <label className="block">
            <FieldLabel>{action.type === 'REJECT' ? 'Lý do kết thúc *' : action.type === 'COMPLETE_TEST' && action.eligible === 'false' ? 'Lý do chưa đủ điều kiện *' : 'Ghi chú nội bộ'}</FieldLabel>
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
  confirmedLevel: '',
  classroomId: '',
  note: '',
};

function CenterEnrollmentModal({ classroomLoadError, classrooms, onClose, onSubmit, working }) {
  const [form, setForm] = useState(initialCenterEnrollment);
  const [validationError, setValidationError] = useState('');

  const update = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
    setValidationError('');
  };

  const submit = () => {
    if (!form.fullName.trim() || !form.email.trim() || !form.phoneNumber.trim()) {
      setValidationError('Vui lòng nhập đầy đủ họ tên, email và số điện thoại.');
      return;
    }
    if (!form.confirmedLevel || !form.classroomId) {
      setValidationError('Vui lòng chọn trình độ đã xác nhận và lớp học.');
      return;
    }
    onSubmit({
      fullName: form.fullName.trim(),
      email: form.email.trim().toLowerCase(),
      phoneNumber: form.phoneNumber.trim(),
      confirmedLevel: form.confirmedLevel,
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
            <h2 className="mt-2 text-xl font-black text-[#0b1c30]" id="center-enrollment-title">Tạo tài khoản và xếp lớp</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">Nếu email đã có tài khoản học viên, hệ thống sẽ dùng tài khoản đó. Tài khoản mới nhận email để tự thiết lập mật khẩu.</p>
          </div>
          <button aria-label="Đóng" className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-100" disabled={working} onClick={onClose} type="button">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto p-6 space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <label>
              <FieldLabel>Họ và tên *</FieldLabel>
              <input autoFocus className={FIELD_CLASS} maxLength={100} onChange={(event) => update('fullName', event.target.value)} value={form.fullName} />
            </label>
            <label>
              <FieldLabel>Số điện thoại *</FieldLabel>
              <input className={FIELD_CLASS} inputMode="tel" maxLength={30} onChange={(event) => update('phoneNumber', event.target.value)} value={form.phoneNumber} />
            </label>
            <label className="sm:col-span-2">
              <FieldLabel>Email đăng nhập *</FieldLabel>
              <input autoComplete="off" className={FIELD_CLASS} maxLength={150} onChange={(event) => update('email', event.target.value)} type="email" value={form.email} />
            </label>
            <div>
              <FieldLabel>Trình độ đã xác nhận *</FieldLabel>
              <BrandedSelect onChange={(event) => update('confirmedLevel', event.target.value)} options={placementOptions} placeholder="Chọn trình độ" value={form.confirmedLevel} />
            </div>
            <div>
              <FieldLabel>Lớp học *</FieldLabel>
              <BrandedSelect
                onChange={(event) => update('classroomId', event.target.value)}
                options={classrooms.map((item) => ({
                  value: String(item.id),
                  label: item.title,
                  description: `${formatClassroomDate(item.startDate)} · ${item.enrolledCount || 0}/${item.maxCapacity || '∞'} học viên`,
                }))}
                placeholder={classrooms.length ? 'Chọn lớp đang tuyển sinh' : 'Chưa có lớp còn chỗ'}
                searchable
                value={form.classroomId}
              />
            </div>
          </div>

          <label className="block">
            <FieldLabel>Ghi chú</FieldLabel>
            <textarea className={TEXTAREA_CLASS} maxLength={700} onChange={(event) => update('note', event.target.value)} placeholder="Thông tin cần lưu cùng hồ sơ ghi danh." rows={3} value={form.note} />
          </label>

          {!classrooms.length ? <p className="rounded-xl bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-800">{classroomLoadError || 'Chưa có lớp đang tuyển sinh và còn chỗ.'}</p> : null}
          {validationError ? <p className={ERROR_NOTICE_CLASS}>{validationError}</p> : null}

          <div className="flex justify-end gap-2 border-t border-slate-100 pt-4">
            <button className={SECONDARY_BUTTON_CLASS} disabled={working} onClick={onClose} type="button">Hủy</button>
            <button className={PRIMARY_BUTTON_CLASS} disabled={working || !classrooms.length} onClick={submit} type="button">
              <UserRoundCheck className="h-4 w-4" />{working ? 'Đang ghi danh...' : 'Tạo tài khoản & xếp lớp'}
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

function FieldLabel({ children }) {
  return <span className="mb-2 block text-xs font-bold uppercase tracking-[0.1em] text-slate-500">{children}</span>;
}

function EmptyState() {
  return (
    <section className="flex min-h-[360px] flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center">
      <BookOpenCheck className="h-12 w-12 text-slate-300" />
      <h2 className="mt-4 text-xl font-black text-[#0b1c30]">Không có hồ sơ đăng ký</h2>
      <p className="mt-2 text-sm text-slate-500">Không có học viên nào phù hợp với điều kiện tìm kiếm hoặc bộ lọc đang chọn.</p>
    </section>
  );
}

const inputClass = 'w-full rounded-xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-3 py-2.5 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white placeholder:text-[#8b706e]';
