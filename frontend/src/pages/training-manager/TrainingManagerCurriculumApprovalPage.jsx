import { useCallback, useEffect, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import {
  ApprovalProgramCard,
  ProgramPageHero,
} from '../../components/curriculum/CurriculumProgramUi';
import { ContentManagerLoadingState } from '../../components/content-manager/ContentManagerUi';
import {
  EMPTY_STATE_CLASS,
  ERROR_NOTICE_CLASS,
  GHOST_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
} from '../../utils/formStyles';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';

export default function TrainingManagerCurriculumApprovalPage() {
  const [programs, setPrograms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [workingId, setWorkingId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [rejectingId, setRejectingId] = useState(null);
  const [rejectReason, setRejectReason] = useState('');

  const loadPrograms = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getTrainingManagerPendingCurriculum();
      setPrograms(data);
    } catch (err) {
      setPrograms([]);
      setError(getClassroomErrorMessage(err, 'Không tải được danh sách giáo trình chờ duyệt.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPrograms();
  }, [loadPrograms]);

  const handleApprove = async (programId) => {
    if (!window.confirm('Duyệt và xuất bản giáo trình này?')) return;
    setWorkingId(programId);
    setError('');
    setSuccess('');
    try {
      await classroomApi.approveCurriculumProgram(programId);
      setSuccess('Đã duyệt giáo trình.');
      setRejectingId(null);
      await loadPrograms();
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể duyệt giáo trình.'));
    } finally {
      setWorkingId(null);
    }
  };

  const handleReject = async (programId) => {
    if (!rejectReason.trim()) {
      setError('Vui lòng nhập lý do từ chối.');
      return;
    }
    setWorkingId(programId);
    setError('');
    setSuccess('');
    try {
      await classroomApi.rejectCurriculumProgram(programId, { reason: rejectReason.trim() });
      setSuccess('Đã từ chối giáo trình.');
      setRejectingId(null);
      setRejectReason('');
      await loadPrograms();
    } catch (err) {
      setError(getClassroomErrorMessage(err, 'Không thể từ chối giáo trình.'));
    } finally {
      setWorkingId(null);
    }
  };

  return (
    <div className="space-y-5">
      <ProgramPageHero
        mode="OFFLINE"
        stats={[{ label: 'Chờ duyệt', value: programs.length }]}
        subtitle="Rà soát chương trình offline/virtual trước khi xuất bản cho đội vận hành mở lớp."
        title="Duyệt giáo trình"
        actions={(
          <button className={GHOST_BUTTON_CLASS} onClick={loadPrograms} type="button">
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Tải lại
          </button>
        )}
      />

      {error ? <div className={ERROR_NOTICE_CLASS}>{error}</div> : null}
      {success ? <div className={SUCCESS_NOTICE_CLASS}>{success}</div> : null}

      {loading ? (
        <ContentManagerLoadingState message="Đang tải hàng đợi duyệt..." />
      ) : !programs.length ? (
        <div className={EMPTY_STATE_CLASS}>Không có giáo trình nào đang chờ duyệt.</div>
      ) : (
        <div className="space-y-4">
          {programs.map((program) => (
            <ApprovalProgramCard
              key={program.id}
              onApprove={handleApprove}
              onReject={handleReject}
              onRejectReasonChange={setRejectReason}
              onToggleReject={(programId) => {
                setRejectingId(rejectingId === programId ? null : programId);
                setRejectReason('');
              }}
              onViewDetail={(item) => {
                window.open(`/content-manager/${item.deliveryMode === 'VIRTUAL' ? 'virtual' : 'offline'}-programs/${item.id}`, '_blank', 'noopener,noreferrer');
              }}
              program={program}
              rejectReason={rejectReason}
              rejectingId={rejectingId}
              workingId={workingId}
            />
          ))}
        </div>
      )}
    </div>
  );
}
