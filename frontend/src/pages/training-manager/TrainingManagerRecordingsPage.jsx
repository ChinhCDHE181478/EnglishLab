import { useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Clock3,
  ExternalLink,
  Eye,
  EyeOff,
  Link2,
  Radio,
  RefreshCw,
  Save,
  Video,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import {
  EMPTY_STATE_CLASS,
  ERROR_NOTICE_CLASS,
  FIELD_CLASS,
  PRIMARY_BUTTON_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
} from '../../utils/formStyles';

const RECORDING_STATUS = {
  NOT_AVAILABLE: { label: 'Chưa có bản ghi', className: 'bg-slate-100 text-slate-700', icon: Video },
  SCHEDULED: { label: 'Đã bật tự ghi', className: 'bg-blue-50 text-blue-700', icon: Clock3 },
  RECORDING: { label: 'Đang ghi hình', className: 'bg-red-50 text-red-700', icon: Radio },
  PROCESSING: { label: 'Lark đang xử lý', className: 'bg-amber-50 text-amber-800', icon: RefreshCw },
  READY: { label: 'Sẵn sàng', className: 'bg-emerald-50 text-emerald-700', icon: CheckCircle2 },
  FAILED: { label: 'Cần kiểm tra', className: 'bg-rose-50 text-rose-700', icon: AlertCircle },
};

function formatDuration(value) {
  if (!value) return 'Chưa có';
  const totalSeconds = Math.round(value / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, '0')}`;
}

function formatDateTime(value) {
  if (!value) return 'Chưa có';
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
}

function RecordingStatus({ value }) {
  const status = RECORDING_STATUS[value] || RECORDING_STATUS.NOT_AVAILABLE;
  const Icon = status.icon;
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold ${status.className}`}>
      <Icon className={`h-3.5 w-3.5 ${value === 'PROCESSING' ? 'animate-spin' : ''}`} />
      {status.label}
    </span>
  );
}

export default function TrainingManagerRecordingsPage() {
  const [classrooms, setClassrooms] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [sessions, setSessions] = useState([]);
  const [sessionForms, setSessionForms] = useState({});
  const [loading, setLoading] = useState(true);
  const [loadingSessions, setLoadingSessions] = useState(false);
  const [workingKey, setWorkingKey] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const setSessionData = (sessionList) => {
    setSessions(sessionList);
    setSessionForms(Object.fromEntries(sessionList.map((session) => [session.id, {
      recordingUrl: session.recordingUrl || '',
      recordingVisible: Boolean(session.recordingVisible),
    }])));
  };

  const loadClassrooms = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getManagerClassrooms();
      setClassrooms(data);
      if (!selectedId && data.length > 0) setSelectedId(String(data[0].id));
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách lớp học.');
    } finally {
      setLoading(false);
    }
  };

  const loadDetails = async (classroomId) => {
    if (!classroomId) return;
    setLoadingSessions(true);
    setError('');
    try {
      const sessionList = await classroomApi.getManagerRecordingSessions(classroomId);
      setSessionData(sessionList);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được dữ liệu ghi hình.');
    } finally {
      setLoadingSessions(false);
    }
  };

  useEffect(() => {
    loadClassrooms();
  }, []);

  useEffect(() => {
    if (selectedId) loadDetails(selectedId);
  }, [selectedId]);

  const classroomOptions = classrooms.map((item) => ({ label: item.title, value: String(item.id) }));
  const virtualSessions = useMemo(
    () => sessions.filter((session) => session.deliveryMode === 'VIRTUAL'),
    [sessions],
  );

  const replaceSession = (nextSession) => {
    setSessions((current) => current.map((session) => (
      session.id === nextSession.id ? nextSession : session
    )));
    setSessionForms((current) => ({
      ...current,
      [nextSession.id]: {
        recordingUrl: nextSession.recordingUrl || '',
        recordingVisible: Boolean(nextSession.recordingVisible),
      },
    }));
  };

  const syncSessionRecording = async (sessionId) => {
    setWorkingKey(`sync-${sessionId}`);
    setError('');
    setSuccess('');
    try {
      const session = await classroomApi.syncLarkRecording(sessionId);
      replaceSession(session);
      if (session.recordingSyncStatus === 'READY') {
        setSuccess('Đã nhận bản ghi thật từ Lark. Hãy kiểm tra rồi công bố cho học viên.');
      } else if (session.recordingSyncStatus === 'PROCESSING') {
        setSuccess('Lark vẫn đang xử lý file. Hệ thống sẽ tiếp tục tự đồng bộ.');
      } else {
        setError(session.recordingSyncError || 'Chưa thể lấy bản ghi từ Lark.');
      }
    } catch (err) {
      setError(err?.response?.data?.message || 'Không đồng bộ được bản ghi từ Lark.');
    } finally {
      setWorkingKey('');
    }
  };

  const saveSessionRecording = async (sessionId) => {
    setWorkingKey(`save-${sessionId}`);
    setError('');
    setSuccess('');
    try {
      const session = await classroomApi.updateSessionRecording(sessionId, sessionForms[sessionId]);
      replaceSession(session);
      setSuccess(session.recordingVisible
        ? 'Đã công bố bản ghi cho học viên.'
        : 'Đã lưu bản ghi ở chế độ chưa công bố.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không cập nhật được bản ghi buổi học.');
    } finally {
      setWorkingKey('');
    }
  };

  const togglePublished = (sessionId) => {
    setSessionForms((current) => ({
      ...current,
      [sessionId]: {
        ...current[sessionId],
        recordingVisible: !current[sessionId]?.recordingVisible,
      },
    }));
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Ghi hình buổi học trực tuyến</h2>
          <p className="text-sm text-slate-600">Theo dõi quá trình ghi hình, duyệt và công bố từng buổi cho học viên.</p>
        </div>
        <button type="button" onClick={() => loadDetails(selectedId)} className={SECONDARY_BUTTON_CLASS}>
          <RefreshCw className="h-4 w-4" /> Làm mới trạng thái
        </button>
      </div>

      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      <div className="rounded-lg border border-slate-200 bg-white p-4">
        <BrandedSelect
          label="Lớp học"
          value={selectedId}
          onChange={(event) => setSelectedId(event.target.value)}
          options={classroomOptions}
        />
      </div>

      {loading || loadingSessions ? (
        <div className="flex min-h-48 items-center justify-center text-sm font-semibold text-slate-500">
          <RefreshCw className="mr-2 h-4 w-4 animate-spin" /> Đang tải trạng thái ghi hình...
        </div>
      ) : virtualSessions.length === 0 ? (
        <div className={EMPTY_STATE_CLASS}>Lớp này chưa có buổi học trực tuyến.</div>
      ) : (
        <div className="space-y-4">
          {virtualSessions.map((session) => {
            const form = sessionForms[session.id] || {};
            const hasRecording = Boolean(form.recordingUrl);
            const syncing = workingKey === `sync-${session.id}`;
            const saving = workingKey === `save-${session.id}`;
            return (
              <section key={session.id} className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <div className="mb-2 flex flex-wrap items-center gap-2">
                      <RecordingStatus value={session.recordingSyncStatus} />
                      {session.recordingProvider && (
                        <span className="text-xs font-bold uppercase text-slate-500">Nguồn: {session.recordingProvider}</span>
                      )}
                    </div>
                    <h3 className="font-['Manrope'] text-base font-extrabold text-slate-900">
                      Buổi {session.sessionDate} · {session.startTime?.slice(0, 5)} - {session.endTime?.slice(0, 5)}
                    </h3>
                    <p className="mt-1 text-sm text-slate-600">{session.sessionContent || 'Nội dung buổi học đang cập nhật'}</p>
                  </div>
                  <button
                    type="button"
                    disabled={Boolean(workingKey) || !session.larkMeetingId}
                    onClick={() => syncSessionRecording(session.id)}
                    className={SECONDARY_BUTTON_CLASS}
                    title={!session.larkMeetingId ? 'Bản ghi chỉ có thể đồng bộ sau khi phòng học đã bắt đầu.' : undefined}
                  >
                    <RefreshCw className={`h-4 w-4 ${syncing ? 'animate-spin' : ''}`} />
                    Đồng bộ từ Lark
                  </button>
                </div>

                <dl className="mt-4 grid gap-x-6 gap-y-3 border-y border-slate-100 py-4 text-sm sm:grid-cols-3">
                  <div><dt className="text-slate-500">Thời lượng</dt><dd className="font-semibold text-slate-900">{formatDuration(session.recordingDurationMs)}</dd></div>
                  <div><dt className="text-slate-500">Đồng bộ gần nhất</dt><dd className="font-semibold text-slate-900">{formatDateTime(session.recordingSyncedAt || session.recordingLastAttemptAt)}</dd></div>
                  <div><dt className="text-slate-500">Hết hạn xem</dt><dd className="font-semibold text-slate-900">{formatDateTime(session.recordingExpiresAt)}</dd></div>
                </dl>

                {session.recordingSyncError && (
                  <div className="mt-4 flex gap-2 rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">
                    <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                    <span>{session.recordingSyncError}</span>
                  </div>
                )}

                {hasRecording && (
                  <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
                    <a
                      href={form.recordingUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-2 text-sm font-bold text-[#730014] hover:underline"
                    >
                      <ExternalLink className="h-4 w-4" /> Mở và kiểm tra bản ghi
                    </a>
                    <div className="flex flex-wrap items-center gap-3">
                      <button
                        type="button"
                        role="switch"
                        aria-checked={Boolean(form.recordingVisible)}
                        onClick={() => togglePublished(session.id)}
                        className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-bold ${
                          form.recordingVisible
                            ? 'border-emerald-200 bg-emerald-50 text-emerald-800'
                            : 'border-slate-200 bg-slate-50 text-slate-700'
                        }`}
                      >
                        {form.recordingVisible ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}
                        {form.recordingVisible ? 'Đang công bố' : 'Chưa công bố'}
                      </button>
                      <button
                        type="button"
                        disabled={Boolean(workingKey)}
                        onClick={() => saveSessionRecording(session.id)}
                        className={PRIMARY_BUTTON_CLASS}
                      >
                        <Save className="h-4 w-4" /> {saving ? 'Đang lưu...' : 'Lưu trạng thái'}
                      </button>
                    </div>
                  </div>
                )}

                <details className="mt-4 border-t border-slate-100 pt-4">
                  <summary className="cursor-pointer text-sm font-bold text-slate-600">
                    <span className="inline-flex items-center gap-2"><Link2 className="h-4 w-4" /> Gắn link thủ công khi Lark gặp sự cố</span>
                  </summary>
                  <div className="mt-3 flex flex-col gap-3 sm:flex-row">
                    <input
                      value={form.recordingUrl || ''}
                      onChange={(event) => setSessionForms((current) => ({
                        ...current,
                        [session.id]: { ...current[session.id], recordingUrl: event.target.value },
                      }))}
                      placeholder="https://..."
                      aria-label="Đường dẫn bản ghi thủ công"
                      className={FIELD_CLASS}
                    />
                    <button
                      type="button"
                      disabled={Boolean(workingKey)}
                      onClick={() => saveSessionRecording(session.id)}
                      className={SECONDARY_BUTTON_CLASS}
                    >
                      <Save className="h-4 w-4" /> Lưu link
                    </button>
                  </div>
                </details>
              </section>
            );
          })}
        </div>
      )}
    </div>
  );
}
