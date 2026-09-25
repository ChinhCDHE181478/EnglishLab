import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Activity,
  AlertCircle,
  ArrowRight,
  BookOpen,
  Calendar,
  Check,
  CheckCircle2,
  Clock,
  HelpCircle,
  MessageSquare,
  Search,
  X,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  ClassroomTabBar,
  ConflictPanel,
  StatusBadge,
} from '../../components/classroom/ClassroomUi';
import { buildChangeRequestDiff, hasBlockingConflict, parseChangeRequestValues } from '../../utils/changeRequestHelpers';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDateTime } from '../../utils/classroomHelpers';
import BrandedSelect from '../../components/ui/BrandedSelect';
import AuthenticatedFileLink from '../../components/classroom/AuthenticatedFileLink';

const requestFilters = [
  { id: 'all', label: 'Tất cả' },
  { id: 'pending', label: 'Chờ duyệt' },
  { id: 'approved', label: 'Đã duyệt' },
  { id: 'rejected', label: 'Từ chối' },
];

const emptyCopyByFilter = {
  all: {
    title: 'Chưa có yêu cầu',
    description: 'Chưa có yêu cầu thay đổi nào trong phạm vi của bạn.',
  },
  pending: {
    title: 'Không có yêu cầu cần duyệt',
    description: 'Hiện tại không có yêu cầu thay đổi nào cần điều phối đào tạo phê duyệt.',
  },
  approved: {
    title: 'Chưa có yêu cầu đã duyệt',
    description: 'Các yêu cầu đã duyệt sẽ hiển thị tại đây để bạn xem lại.',
  },
  rejected: {
    title: 'Chưa có yêu cầu bị từ chối',
    description: 'Các yêu cầu đã từ chối sẽ hiển thị tại đây để bạn xem lại.',
  },
};

export default function StaffRequestsPage() {
  const [searchParams] = useSearchParams();
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [reviewNotes, setReviewNotes] = useState({});
  const [selectedId, setSelectedId] = useState(searchParams.get('requestId') || '');
  const [conflictResults, setConflictResults] = useState({});
  const [checkingConflicts, setCheckingConflicts] = useState({});
  const [returnOptions, setReturnOptions] = useState([]);
  const [targetClassSectionId, setTargetClassSectionId] = useState('');
  const [loadingReturnOptions, setLoadingReturnOptions] = useState(false);
  const [rooms, setRooms] = useState([]);
  const [activeFilter, setActiveFilter] = useState('pending');
  const [keyword, setKeyword] = useState('');

  useEffect(() => {
    classroomApi.listRooms().then(setRooms).catch(() => setRooms([]));
  }, []);

  const roomsById = useMemo(
    () => Object.fromEntries(rooms.map((room) => [String(room.id), room.name])),
    [rooms],
  );

  const loadRequests = async () => {
    setLoading(true);
    setError('');
    try {
      const status = activeFilter === 'all' ? 'ALL' : activeFilter.toUpperCase();
      const data = await classroomApi.getStaffChangeRequests(status);
      setRequests(data);
      const requestedId = searchParams.get('requestId');
      if (data.length > 0) {
        const preferredId = requestedId && data.some((item) => String(item.id) === requestedId)
          ? requestedId
          : (data.some((item) => String(item.id) === selectedId) ? selectedId : String(data[0].id));
        setSelectedId(String(preferredId));
      } else {
        setSelectedId('');
      }
    } catch (err) {
      setRequests([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải yêu cầu thay đổi.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRequests();
  }, [activeFilter]);

  const selected = useMemo(
    () => requests.find((item) => String(item.id) === selectedId) || null,
    [requests, selectedId],
  );
  const isPending = selected?.status === 'PENDING';
  const emptyCopy = emptyCopyByFilter[activeFilter] || emptyCopyByFilter.pending;

  const visibleRequests = useMemo(() => {
    const normalized = keyword.trim().toLocaleLowerCase('vi-VN');
    if (!normalized) return requests;
    return requests.filter((item) => {
      const haystack = [
        item.requestTypeLabel,
        item.requestType,
        item.classroomTitle,
        item.requesterName,
        item.reviewerName,
        item.reason,
        item.reviewNote,
        item.id != null ? `#${item.id}` : '',
      ]
        .filter(Boolean)
        .join(' ')
        .toLocaleLowerCase('vi-VN');
      return haystack.includes(normalized);
    });
  }, [keyword, requests]);

  useEffect(() => {
    if (!visibleRequests.length) {
      if (selectedId) setSelectedId('');
      return;
    }
    if (!visibleRequests.some((item) => String(item.id) === selectedId)) {
      setSelectedId(String(visibleRequests[0].id));
    }
  }, [visibleRequests, selectedId]);

  const diffRows = useMemo(
    () => (selected ? buildChangeRequestDiff(selected.oldValuesJson, selected.newValuesJson, { rooms: roomsById }) : []),
    [selected, roomsById],
  );

  const selectedConflict = selected ? conflictResults[selected.id] : null;
  const conflictDetected = hasBlockingConflict(selectedConflict);
  const isSuspensionRequest = selected?.requestType === 'SUSPEND_STUDENT';
  const isReturnRequest = selected?.requestType === 'RESUME_STUDENT';
  const isCourseSuspensionFlow = isSuspensionRequest || isReturnRequest;
  const selectedOldValues = useMemo(
    () => parseChangeRequestValues(selected?.oldValuesJson),
    [selected?.oldValuesJson],
  );
  const selectedNewValues = useMemo(
    () => parseChangeRequestValues(selected?.newValuesJson),
    [selected?.newValuesJson],
  );

  useEffect(() => {
    if (selected?.id && isPending && !isCourseSuspensionFlow && !conflictResults[selected.id] && !checkingConflicts[selected.id]) {
      handleConflictCheck(selected.id, { silent: true });
    }
  }, [selected?.id, isPending, isCourseSuspensionFlow]);

  useEffect(() => {
    if (!selected?.id || !isPending || selected.requestType !== 'RESUME_STUDENT') {
      setReturnOptions([]);
      setTargetClassSectionId('');
      return;
    }
    let active = true;
    const loadReturnOptions = async () => {
      setLoadingReturnOptions(true);
      try {
        const options = await classroomApi.getCourseReturnOptions(selected.id);
        if (active) {
          setReturnOptions(options);
          setTargetClassSectionId('');
        }
      } catch (err) {
        if (active) setActionMessage(getClassroomErrorMessage(err, 'Không thể tải lớp phù hợp.'));
      } finally {
        if (active) setLoadingReturnOptions(false);
      }
    };
    loadReturnOptions();
    return () => { active = false; };
  }, [selected?.id, selected?.requestType, isPending]);

  const handleReview = async (requestId, action, overrideConflict = false) => {
    setActionMessage('');
    const note = reviewNotes[requestId] || '';
    if (action === 'approve' && overrideConflict && !note.trim()) {
      setActionMessage('Cần ghi chú khi duyệt và ghi đè xung đột lịch học.');
      return;
    }
    if (action === 'approve' && isReturnRequest && !targetClassSectionId) {
      setActionMessage('Vui lòng chọn lớp phù hợp trước khi duyệt học viên quay lại.');
      return;
    }
    if (action === 'reject' && isCourseSuspensionFlow && !note.trim()) {
      setActionMessage('Vui lòng nhập lý do từ chối để học viên có thể bổ sung hồ sơ.');
      return;
    }
    try {
      const payload = {
        reviewNote: note,
        overrideConflict: action === 'approve' ? overrideConflict : false,
        targetClassSectionId: action === 'approve' && isReturnRequest
          ? Number(targetClassSectionId)
          : null,
      };
      if (action === 'approve') {
        await classroomApi.approveChangeRequest(requestId, payload);
      } else {
        await classroomApi.rejectChangeRequest(requestId, payload);
      }
      setActionMessage(action === 'approve' ? 'Đã duyệt và áp dụng yêu cầu thành công.' : 'Đã từ chối yêu cầu thành công.');
      setConflictResults((curr) => {
        const next = { ...curr };
        delete next[requestId];
        return next;
      });
      await loadRequests();
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể xử lý yêu cầu.'));
    }
  };

  const handleConflictCheck = async (requestId, options = {}) => {
    if (!options.silent) {
      setActionMessage('');
    }
    setCheckingConflicts((curr) => ({ ...curr, [requestId]: true }));
    try {
      const result = await classroomApi.checkChangeRequestConflict(requestId);
      setConflictResults((curr) => ({ ...curr, [requestId]: result }));
      // The check often returns the same "no conflict" result as before, which leaves the panel
      // looking unchanged — without this the button appears to do nothing when clicked manually.
      if (!options.silent) {
        setActionMessage(hasBlockingConflict(result)
          ? 'Đã kiểm tra lại: phát hiện xung đột lịch học.'
          : 'Đã kiểm tra lại trùng lịch thành công — không phát hiện xung đột.');
      }
    } catch (err) {
      if (!options.silent) {
        setActionMessage(getClassroomErrorMessage(err, 'Không thể kiểm tra trùng lịch.'));
      }
    } finally {
      setCheckingConflicts((curr) => ({ ...curr, [requestId]: false }));
    }
  };

  return (
    <motion.main
      className="space-y-6"
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.32, ease: 'easeOut' }}
    >
      {actionMessage ? (
        <div
          className={`rounded-2xl border p-4 text-xs flex items-start gap-2 ${
            actionMessage.includes('thành công')
              ? 'bg-emerald-50 border-emerald-100 text-emerald-800'
              : 'bg-rose-50 border-rose-100 text-rose-800'
          }`}
        >
          {actionMessage.includes('thành công') ? (
            <CheckCircle2 className="h-4 w-4 flex-shrink-0 mt-0.5 text-emerald-700" />
          ) : (
            <AlertCircle className="h-4 w-4 flex-shrink-0 mt-0.5 text-rose-700" />
          )}
          <p className="leading-5">{actionMessage}</p>
        </div>
      ) : null}

      <section className="flex flex-1 flex-col space-y-4">
        <ClassroomTabBar activeTab={activeFilter} onChange={setActiveFilter} tabs={requestFilters} />

        {loading ? <ClassroomLoadingState message="Đang tải danh sách yêu cầu thay đổi..." /> : null}
        {!loading && error ? <ClassroomErrorState message={error} onRetry={loadRequests} /> : null}
        {!loading && !error && !requests.length ? (
          <ClassroomEmptyState
            description={emptyCopy.description}
            title={emptyCopy.title}
            icon={HelpCircle}
          />
        ) : null}

        {!loading && !error && requests.length && !visibleRequests.length ? (
          <ClassroomEmptyState
            description="Thử đổi từ khóa hoặc xóa bộ lọc tìm kiếm."
            title="Không có yêu cầu khớp tìm kiếm"
            icon={Search}
          />
        ) : null}

        {!loading && !error && visibleRequests.length ? (
          <div className="grid gap-6 lg:grid-cols-[380px_1fr]">
            <aside className="rounded-xl border border-[#e5e7eb] bg-white p-5 shadow-sm space-y-3 max-h-[750px] overflow-y-auto">
              <h3 className="text-xs font-bold text-[#8b706e] uppercase tracking-wider px-2">
                {activeFilter === 'pending' ? 'Yêu cầu chờ duyệt'
                  : activeFilter === 'approved' ? 'Yêu cầu đã duyệt'
                    : activeFilter === 'rejected' ? 'Yêu cầu đã từ chối'
                      : 'Tất cả yêu cầu'}
              </h3>
              <div className="relative px-0.5">
                <Search className="pointer-events-none absolute left-3.5 top-3 h-4 w-4 text-[#9b8582]" />
                <input
                  className="w-full rounded-2xl border border-[#ead9db] bg-white py-2.5 pl-10 pr-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014]"
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="Tìm lớp, người gửi, loại yêu cầu..."
                  type="search"
                  value={keyword}
                />
              </div>
              <div className="space-y-2">
                {visibleRequests.map((item) => {
                  const isSelected = String(item.id) === selectedId;
                  return (
                    <button
                      key={item.id}
                      className={`w-full rounded-2xl p-4 text-left transition-all duration-200 border ${
                        isSelected
                          ? 'bg-[#4b0009] border-[#4b0009] text-white shadow-md shadow-[#4b0009]/10'
                          : 'bg-[#fffafb]/50 border-gray-100 text-[#584140] hover:bg-[#fff3f4] hover:border-[#dfbfbd]/30'
                      }`}
                      onClick={() => setSelectedId(String(item.id))}
                      type="button"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <p className="font-extrabold text-sm line-clamp-1">
                          {item.requestTypeLabel || item.requestType}
                        </p>
                        {!isSelected ? <StatusBadge status={item.status} /> : null}
                      </div>
                      <p className={`mt-1.5 text-xs line-clamp-1 ${isSelected ? 'text-white/80' : 'text-[#8b706e]'}`}>
                        {item.classroomTitle}
                      </p>
                      <p className={`mt-2 text-[10px] ${isSelected ? 'text-white/70' : 'text-gray-400'}`}>
                        {['SUSPEND_STUDENT', 'RESUME_STUDENT'].includes(item.requestType) ? 'HV' : 'GV'}: {item.requesterName || '—'}
                      </p>
                      <p className={`mt-1 text-[10px] ${isSelected ? 'text-white/70' : 'text-gray-400'}`}>
                        Phụ trách: {item.reviewerName || 'Chưa phân công'}
                      </p>
                    </button>
                  );
                })}
              </div>
            </aside>

            {selected ? (
              <div className="space-y-6">
                <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-6">
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-50 pb-5">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{selected.requestTypeLabel || selected.requestType}</h2>
                        {selected.requestType === 'CHANGE_ROOM' ? (
                          <span className="rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-bold text-[#584140]">
                            Phòng cũ: {roomsById[String(selectedOldValues.roomId)] || (selectedOldValues.roomId ? `Phòng #${selectedOldValues.roomId}` : 'Chưa có phòng')}
                          </span>
                        ) : null}
                      </div>
                      <p className="text-xs text-[#8b706e] mt-0.5">Người gửi: {selected.requesterName || '—'}</p>
                      {selected.reviewerName ? (
                        <p className="mt-1 text-xs text-[#8b706e]">Phụ trách: {selected.reviewerName}</p>
                      ) : null}
                    </div>
                    <StatusBadge status={selected.status || 'PENDING'} />
                  </div>

                  <div className="grid gap-4 sm:grid-cols-2 text-sm text-[#584140]">
                    <p className="flex items-center gap-2">
                      <BookOpen className="h-4 w-4 text-[#730014]" />
                      <span>Lớp: <strong>{selected.classroomTitle}</strong></span>
                    </p>
                    <p className="flex items-center gap-2">
                      <Calendar className="h-4 w-4 text-[#730014]" />
                      <span>Gửi lúc: <strong>{formatClassroomDateTime(selected.createdAt)}</strong></span>
                    </p>
                    {selected.reviewedAt ? (
                      <p className="flex items-center gap-2">
                        <Clock className="h-4 w-4 text-[#730014]" />
                        <span>Xử lý lúc: <strong>{formatClassroomDateTime(selected.reviewedAt)}</strong></span>
                      </p>
                    ) : null}
                    {selected.targetSessionId ? (
                      <p className="flex items-center gap-2">
                        <Clock className="h-4 w-4 text-[#730014]" />
                        <span>Buổi học #{selected.targetSessionId}</span>
                      </p>
                    ) : null}
                  </div>

                  {isCourseSuspensionFlow ? (
                    <div className="grid gap-3 rounded-2xl border border-sky-100 bg-sky-50/70 p-5 text-sm text-[#584140] sm:grid-cols-2">
                      <p>Học viên: <strong>{selectedOldValues.studentName || selected.requesterName}</strong></p>
                      <p>Khóa học: <strong>{selectedOldValues.courseTitle || selected.classroomTitle}</strong></p>
                      {isSuspensionRequest ? (
                        <>
                          <p>Tiến độ: <strong>{selectedOldValues.completedSessions}/{selectedOldValues.totalSessions} buổi ({selectedOldValues.progressPercent}%)</strong></p>
                          <p>Thời gian: <strong>{selectedNewValues.requestedStartDate} đến {selectedNewValues.requestedReturnDate}</strong></p>
                          <AuthenticatedFileLink
                            className="inline-flex items-center gap-2 font-bold text-[#730014] hover:underline sm:col-span-2"
                            url={selectedNewValues.proofUrl}
                          >
                            Xem giấy tờ minh chứng
                          </AuthenticatedFileLink>
                        </>
                      ) : (
                        <p>Hạn đăng ký quay lại: <strong>{selectedOldValues.returnDeadline || '—'}</strong></p>
                      )}
                    </div>
                  ) : diffRows.length ? (
                    <div className="rounded-2xl border border-[#f0e4e2] overflow-hidden">
                      <div className="bg-[#fffafb] px-4 py-2 text-xs font-bold uppercase tracking-wider text-[#8b706e]">
                        Thay đổi đề xuất
                      </div>
                      <div className="divide-y divide-[#f0e4e2]">
                        {diffRows.map((row) => (
                          <div className="grid gap-2 px-4 py-3 text-sm sm:grid-cols-[140px_1fr_24px_1fr]" key={row.key}>
                            <span className="font-bold text-[#8b706e]">{row.label}</span>
                            <span className="text-[#584140]">{row.oldValue}</span>
                            <ArrowRight className="hidden h-4 w-4 text-[#730014] sm:block" />
                            <span className="font-extrabold text-[#2b2828]">{row.newValue}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : null}

                  <div className="rounded-2xl border border-gray-100 bg-gray-50/30 p-5 space-y-2">
                    <h4 className="text-xs font-bold text-[#8b706e] uppercase tracking-wider flex items-center gap-1">
                      <MessageSquare className="h-4 w-4 text-[#730014]" />
                      Lý do từ {isCourseSuspensionFlow ? 'học viên' : 'giảng viên'}
                    </h4>
                    <p className="text-sm text-[#584140] whitespace-pre-wrap leading-6">
                      {selected.reason || 'Không có mô tả chi tiết.'}
                    </p>
                  </div>

                  {!isPending && (selected.reviewNote || selected.reviewedAt) ? (
                    <div className="rounded-2xl border border-emerald-100 bg-emerald-50/40 p-5 space-y-2">
                      <h4 className="text-xs font-bold text-[#8b706e] uppercase tracking-wider flex items-center gap-1">
                        <MessageSquare className="h-4 w-4 text-[#730014]" />
                        Ghi chú xử lý
                      </h4>
                      <p className="text-sm text-[#584140] whitespace-pre-wrap leading-6">
                        {selected.reviewNote || 'Không có ghi chú.'}
                      </p>
                    </div>
                  ) : null}
                </section>

                {isPending ? (
                <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-6">
                  <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828] flex items-center gap-2">
                    <Activity className="h-5 w-5 text-[#730014]" />
                    Phê duyệt
                  </h3>

                  {selectedConflict ? (
                    <ConflictPanel conflictResult={selectedConflict} />
                  ) : null}

                  {isReturnRequest ? (
                    <div className="space-y-2">
                      <label className="text-xs font-bold uppercase tracking-wider text-[#8b706e]">
                        Lớp học phù hợp
                      </label>
                      <BrandedSelect
                        disabled={loadingReturnOptions}
                        onChange={(event) => setTargetClassSectionId(event.target.value)}
                        options={returnOptions.map((item) => ({
                          label: item.title,
                          value: String(item.id),
                          description: [
                            Number.isInteger(item.completedSessions) && Number.isInteger(item.totalSessions)
                              ? `Đã học ${item.completedSessions}/${item.totalSessions} buổi`
                              : '',
                            item.scheduleSummary,
                            item.primaryTeacherName,
                          ].filter(Boolean).join(' · '),
                        }))}
                        placeholder={loadingReturnOptions
                          ? 'Đang kiểm tra lớp phù hợp...'
                          : returnOptions.length ? 'Chọn lớp để xếp học viên' : 'Chưa có lớp phù hợp'}
                        searchable
                        value={targetClassSectionId}
                      />
                    </div>
                  ) : null}

                  <div className="flex flex-wrap gap-3">
                    {!conflictDetected ? (
                      <button
                        className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-700 px-5 py-3 text-xs font-extrabold text-white shadow-sm hover:bg-emerald-800"
                        disabled={isReturnRequest && !targetClassSectionId}
                        onClick={() => handleReview(selected.id, 'approve', false)}
                        type="button"
                      >
                        <Check className="h-4 w-4" />
                        {isSuspensionRequest ? 'Duyệt bảo lưu' : isReturnRequest ? 'Xác nhận xếp lớp' : 'Duyệt và áp dụng'}
                      </button>
                    ) : (
                      <button
                        className="inline-flex items-center gap-1.5 rounded-xl bg-amber-700 px-5 py-3 text-xs font-extrabold text-white shadow-sm hover:bg-amber-800"
                        onClick={() => handleReview(selected.id, 'approve', true)}
                        type="button"
                      >
                        <Check className="h-4 w-4" />
                        Duyệt và ghi đè xung đột
                      </button>
                    )}
                    <button
                      className="inline-flex items-center gap-1.5 rounded-xl border border-rose-200 bg-rose-50/30 px-5 py-3 text-xs font-extrabold text-rose-700 hover:bg-rose-50"
                      onClick={() => handleReview(selected.id, 'reject')}
                      type="button"
                    >
                      <X className="h-4 w-4" />
                      Từ chối
                    </button>
                    {!isCourseSuspensionFlow ? (
                      <button
                        className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-5 py-3 text-xs font-extrabold text-[#584140] hover:bg-gray-50"
                        disabled={checkingConflicts[selected.id]}
                        onClick={() => handleConflictCheck(selected.id)}
                        type="button"
                      >
                        {checkingConflicts[selected.id] ? 'Đang kiểm tra...' : 'Kiểm tra lại trùng lịch'}
                      </button>
                    ) : null}
                  </div>

                  <div className="space-y-2">
                    <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">
                      Ghi chú phản hồi {conflictDetected
                        ? '(bắt buộc khi ghi đè xung đột)'
                        : isCourseSuspensionFlow ? '(bắt buộc khi từ chối)' : ''}
                    </label>
                    <textarea
                      className="min-h-[100px] w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none focus:border-[#730014] focus:bg-white"
                      onChange={(event) => setReviewNotes((current) => ({ ...current, [selected.id]: event.target.value }))}
                      placeholder={`Ghi chú gửi lại cho ${isCourseSuspensionFlow ? 'học viên' : 'giảng viên'} hoặc lý do từ chối...`}
                      value={reviewNotes[selected.id] || ''}
                    />
                  </div>
                </section>
                ) : null}
              </div>
            ) : (
              <ClassroomEmptyState
                description="Chọn một yêu cầu ở danh sách bên trái."
                title="Chưa chọn yêu cầu"
              />
            )}
          </div>
        ) : null}
      </section>
    </motion.main>
  );
}
