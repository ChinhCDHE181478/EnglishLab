import { useEffect, useMemo, useState } from 'react';
import { Eye, EyeOff, RefreshCw, Save, Video } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import {
  CARD_CLASS,
  EMPTY_STATE_CLASS,
  ERROR_NOTICE_CLASS,
  FIELD_CLASS,
  GHOST_BUTTON_CLASS,
  PANEL_CLASS,
  PRIMARY_BUTTON_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
} from '../../utils/formStyles';

export default function TrainingManagerRecordingsPage() {
  const [classrooms, setClassrooms] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [sessions, setSessions] = useState([]);
  const [offeringForm, setOfferingForm] = useState({ recordingUrl: '', recordingVisible: false });
  const [sessionForms, setSessionForms] = useState({});
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

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
    try {
      const [offering, sessionList] = await Promise.all([
        classroomApi.getManagerClassroom(classroomId),
        classroomApi.getTrainingManagerClassroomSessions(classroomId),
      ]);
      setOfferingForm({
        recordingUrl: offering.recordingUrl || '',
        recordingVisible: Boolean(offering.recordingVisible),
      });
      setSessions(sessionList);
      const nextForms = {};
      sessionList.forEach((session) => {
        nextForms[session.id] = {
          recordingUrl: session.recordingUrl || '',
          recordingVisible: Boolean(session.recordingVisible),
        };
      });
      setSessionForms(nextForms);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được dữ liệu ghi hình.');
    }
  };

  useEffect(() => { loadClassrooms(); }, []);
  useEffect(() => { if (selectedId) loadDetails(selectedId); }, [selectedId]);

  const classroomOptions = classrooms.map((item) => ({ label: item.title, value: String(item.id) }));
  const virtualSessions = useMemo(
    () => sessions.filter((session) => session.deliveryMode === 'VIRTUAL'),
    [sessions],
  );

  const saveOfferingRecording = async () => {
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await classroomApi.updateOfferingRecording(selectedId, offeringForm);
      setSuccess('Đã cập nhật ghi hình cấp lớp.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không cập nhật được ghi hình lớp.');
    } finally {
      setWorking(false);
    }
  };

  const saveSessionRecording = async (sessionId) => {
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await classroomApi.updateSessionRecording(sessionId, sessionForms[sessionId]);
      setSuccess('Đã cập nhật ghi hình buổi học.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không cập nhật được ghi hình buổi học.');
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Quản lý ghi hình buổi học</h2>
          <p className="text-sm text-slate-600">Gắn link ghi hình Lark và điều khiển hiển thị cho học viên.</p>
        </div>
        <button type="button" onClick={loadClassrooms} className={SECONDARY_BUTTON_CLASS}>
          <RefreshCw className="h-4 w-4" /> Tải lại
        </button>
      </div>

      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      <div className={PANEL_CLASS}>
        <BrandedSelect label="Lớp học" value={selectedId} onChange={setSelectedId} options={classroomOptions} />
      </div>

      {loading ? <p className="text-sm font-semibold text-slate-500">Đang tải...</p> : (
        <div className="space-y-6">
          <div className={PANEL_CLASS}>
            <h3 className="mb-4 flex items-center gap-2 font-['Manrope'] text-lg font-extrabold text-slate-900">
              <Video className="h-4 w-4 text-[#730014]" /> Ghi hình cấp lớp
            </h3>
            <div className="space-y-4">
              <input
                value={offeringForm.recordingUrl}
                onChange={(e) => setOfferingForm({ ...offeringForm, recordingUrl: e.target.value })}
                placeholder="https://..."
                className={FIELD_CLASS}
              />
              <label className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-semibold text-slate-700">
                <input
                  type="checkbox"
                  checked={offeringForm.recordingVisible}
                  onChange={(e) => setOfferingForm({ ...offeringForm, recordingVisible: e.target.checked })}
                  className="rounded border-slate-300 text-[#730014] focus:ring-[#730014]"
                />
                {offeringForm.recordingVisible ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}
                Cho phép học viên xem ghi hình lớp
              </label>
              <button type="button" disabled={working} onClick={saveOfferingRecording} className={PRIMARY_BUTTON_CLASS}>
                <Save className="h-4 w-4" /> Lưu ghi hình lớp
              </button>
            </div>
          </div>

          <div className="space-y-4">
            <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">Ghi hình theo buổi học ảo</h3>
            {virtualSessions.length === 0 ? (
              <div className={EMPTY_STATE_CLASS}>Lớp này chưa có buổi học trực tuyến.</div>
            ) : virtualSessions.map((session) => (
              <div key={session.id} className={CARD_CLASS}>
                <p className="font-semibold text-slate-900">
                  Buổi {session.sessionDate} · {session.startTime?.slice(0, 5)} - {session.endTime?.slice(0, 5)}
                </p>
                <div className="mt-3 space-y-3">
                  <input
                    value={sessionForms[session.id]?.recordingUrl || ''}
                    onChange={(e) => setSessionForms({
                      ...sessionForms,
                      [session.id]: { ...sessionForms[session.id], recordingUrl: e.target.value },
                    })}
                    placeholder="Link ghi hình buổi học"
                    className={FIELD_CLASS}
                  />
                  <label className="inline-flex items-center gap-2 text-sm font-semibold text-slate-700">
                    <input
                      type="checkbox"
                      checked={Boolean(sessionForms[session.id]?.recordingVisible)}
                      onChange={(e) => setSessionForms({
                        ...sessionForms,
                        [session.id]: { ...sessionForms[session.id], recordingVisible: e.target.checked },
                      })}
                      className="rounded border-slate-300 text-[#730014] focus:ring-[#730014]"
                    />
                    Hiển thị cho học viên
                  </label>
                  <button type="button" disabled={working} onClick={() => saveSessionRecording(session.id)} className={GHOST_BUTTON_CLASS}>
                    Lưu buổi này
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
