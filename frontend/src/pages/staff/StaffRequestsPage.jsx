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
  X,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  ConflictPanel,
  StatusBadge,
} from '../../components/classroom/ClassroomUi';
import { buildChangeRequestDiff, hasBlockingConflict } from '../../utils/changeRequestHelpers';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDateTime } from '../../utils/classroomHelpers';

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

  const loadRequests = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getPendingChangeRequests();
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
  }, []);

  const selected = useMemo(
    () => requests.find((item) => String(item.id) === selectedId) || null,
    [requests, selectedId],
  );

  const diffRows = useMemo(
    () => (selected ? buildChangeRequestDiff(selected.oldValuesJson, selected.newValuesJson) : []),
    [selected],
  );

  const selectedConflict = selected ? conflictResults[selected.id] : null;
  const conflictDetected = hasBlockingConflict(selectedConflict);

  useEffect(() => {
    if (selected?.id && !conflictResults[selected.id] && !checkingConflicts[selected.id]) {
      handleConflictCheck(selected.id, { silent: true });
    }
  }, [selected?.id]);

  const handleReview = async (requestId, action, overrideConflict = false) => {
    setActionMessage('');
    const note = reviewNotes[requestId] || '';
    if (action === 'approve' && overrideConflict && !note.trim()) {
      setActionMessage('Cần ghi chú khi duyệt và ghi đè xung đột lịch học.');
      return;
    }
    try {
      const payload = {
        reviewNote: note,
        overrideConflict: action === 'approve' ? overrideConflict : false,
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

      <section className="flex flex-1 flex-col">
        {loading ? <ClassroomLoadingState message="Đang tải danh sách yêu cầu thay đổi..." /> : null}
        {!loading && error ? <ClassroomErrorState message={error} onRetry={loadRequests} /> : null}
        {!loading && !error && !requests.length ? (
          <ClassroomEmptyState
            description="Hiện tại không có yêu cầu thay đổi nào cần điều phối đào tạo phê duyệt."
            title="Không có yêu cầu cần duyệt"
            icon={HelpCircle}
          />
        ) : null}

        {!loading && !error && requests.length ? (
          <div className="grid gap-6 lg:grid-cols-[380px_1fr]">
            <aside className="rounded-xl border border-[#e5e7eb] bg-white p-5 shadow-sm space-y-3 max-h-[750px] overflow-y-auto">
              <h3 className="text-xs font-bold text-[#8b706e] uppercase tracking-wider px-2">Yêu cầu chờ duyệt</h3>
              <div className="space-y-2">
                {requests.map((item) => {
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
                      <p className="font-extrabold text-sm line-clamp-1">
                        {item.requestTypeLabel || item.requestType}
                      </p>
                      <p className={`mt-1.5 text-xs line-clamp-1 ${isSelected ? 'text-white/80' : 'text-[#8b706e]'}`}>
                        {item.classroomTitle}
                      </p>
                      <p className={`mt-2 text-[10px] ${isSelected ? 'text-white/70' : 'text-gray-400'}`}>
                        GV: {item.requesterName || '—'}
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
                      <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{selected.requestTypeLabel || selected.requestType}</h2>
                      <p className="text-xs text-[#8b706e] mt-0.5">Người gửi: {selected.requesterName || '—'}</p>
                    </div>
                    <StatusBadge status="PENDING" />
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
                    {selected.targetSessionId ? (
                      <p className="flex items-center gap-2">
                        <Clock className="h-4 w-4 text-[#730014]" />
                        <span>Buổi học #{selected.targetSessionId}</span>
                      </p>
                    ) : null}
                  </div>

                  {diffRows.length ? (
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
                      Lý do từ giảng viên
                    </h4>
                    <p className="text-sm text-[#584140] whitespace-pre-wrap leading-6">
                      {selected.reason || 'Không có mô tả chi tiết.'}
                    </p>
                  </div>
                </section>

                <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-6">
                  <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828] flex items-center gap-2">
                    <Activity className="h-5 w-5 text-[#730014]" />
                    Phê duyệt
                  </h3>

                  {selectedConflict ? (
                    <ConflictPanel conflictResult={selectedConflict} />
                  ) : null}

                  <div className="flex flex-wrap gap-3">
                    {!conflictDetected ? (
                      <button
                        className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-700 px-5 py-3 text-xs font-extrabold text-white shadow-sm hover:bg-emerald-800"
                        onClick={() => handleReview(selected.id, 'approve', false)}
                        type="button"
                      >
                        <Check className="h-4 w-4" />
                        Duyệt và áp dụng
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
                    <button
                      className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-5 py-3 text-xs font-extrabold text-[#584140] hover:bg-gray-50"
                      disabled={checkingConflicts[selected.id]}
                      onClick={() => handleConflictCheck(selected.id)}
                      type="button"
                    >
                      {checkingConflicts[selected.id] ? 'Đang kiểm tra...' : 'Kiểm tra lại trùng lịch'}
                    </button>
                  </div>

                  <div className="space-y-2">
                    <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">
                      Ghi chú phản hồi {conflictDetected ? '(bắt buộc khi ghi đè xung đột)' : ''}
                    </label>
                    <textarea
                      className="min-h-[100px] w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none focus:border-[#730014] focus:bg-white"
                      onChange={(event) => setReviewNotes((current) => ({ ...current, [selected.id]: event.target.value }))}
                      placeholder="Ghi chú gửi lại cho giảng viên hoặc lý do từ chối..."
                      value={reviewNotes[selected.id] || ''}
                    />
                  </div>
                </section>
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
