import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  AlertTriangle,
  Archive,
  BookOpen,
  Copy,
  Edit3,
  Layers,
  Send,
  Video,
} from 'lucide-react';
import curriculumApi from '../../api/curriculumApi';
import {
  ProgramDetailHero,
  ProgramMetricGrid,
  ProgramSection,
  ProgramStatusPill,
} from '../../components/curriculum/CurriculumProgramUi';
import { ContentManagerLoadingState } from '../../components/content-manager/ContentManagerUi';
import {
  DANGER_BUTTON_CLASS,
  EMPTY_STATE_CLASS,
  ERROR_NOTICE_CLASS,
  GHOST_BUTTON_CLASS,
  PRIMARY_BUTTON_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
} from '../../utils/formStyles';
import { formatClassroomDate } from '../../utils/classroomHelpers';

const refTypeLabel = {
  MATERIAL: 'Tài liệu',
  EXERCISE: 'Bài tập',
  ASSESSMENT: 'Đề kiểm tra',
  FLASHCARD: 'Flashcard',
};

export default function ContentManagerCurriculumProgramDetailPage({ mode = 'OFFLINE' }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const isVirtual = mode === 'VIRTUAL';
  const listPath = isVirtual ? '/content-manager/virtual-programs' : '/content-manager/offline-programs';
  const builderPath = `/content-manager/syllabus-builder?programId=${id}`;

  const [program, setProgram] = useState(null);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadProgram = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await curriculumApi.getCurriculumProgram(id);
      setProgram(data);
    } catch (err) {
      setProgram(null);
      setError(err?.response?.data?.message || 'Không tải được chi tiết giáo trình.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadProgram();
  }, [loadProgram]);

  const handleSubmitReview = async () => {
    if (!window.confirm('Gửi chương trình đào tạo này cho Nhân viên đào tạo rà soát?')) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const updated = await curriculumApi.submitCurriculumProgramForReview(id);
      setProgram(updated);
      setSuccess('Đã gửi duyệt giáo trình.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể gửi duyệt giáo trình.');
    } finally {
      setWorking(false);
    }
  };

  const handleClone = async () => {
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const clone = await curriculumApi.cloneCurriculumProgram(id);
      setSuccess(`Đã nhân bản thành "${clone.title}".`);
      navigate(`${listPath}/${clone.id}`);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể nhân bản giáo trình.');
    } finally {
      setWorking(false);
    }
  };

  const handleArchive = async () => {
    if (program?.activeClassroomCount > 0) {
      setError(`Giáo trình đang được ${program.activeClassroomCount} lớp sắp khai giảng / đang diễn ra sử dụng.`);
      return;
    }
    if (!window.confirm(`Lưu trữ giáo trình "${program?.title}"?`)) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await curriculumApi.archiveCurriculumProgram(id);
      setSuccess('Đã lưu trữ giáo trình.');
      await loadProgram();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể lưu trữ giáo trình.');
    } finally {
      setWorking(false);
    }
  };

  if (loading) {
    return <ContentManagerLoadingState message="Đang tải chi tiết giáo trình..." />;
  }

  if (!program) {
    return (
      <div className={EMPTY_STATE_CLASS}>
        {error || 'Không tìm thấy giáo trình.'}
        <Link className="mt-4 inline-block text-sm font-bold text-[#730014]" to={listPath}>Quay lại danh sách</Link>
      </div>
    );
  }

  const activeClassrooms = (program.usingClassrooms || []).filter((c) => ['UPCOMING', 'ACTIVE'].includes(c.status));
  const pastClassrooms = (program.usingClassrooms || []).filter((c) => !['UPCOMING', 'ACTIVE'].includes(c.status));
  const canSubmitReview = ['DRAFT', 'REJECTED'].includes(program.status);
  const canEditUnits = program.status !== 'ARCHIVED';

  return (
    <div className="space-y-5">
      <ProgramDetailHero
        isVirtual={isVirtual}
        listPath={listPath}
        program={program}
        actions={(
          <>
            <Link className={SECONDARY_BUTTON_CLASS} to={`${listPath}/${program.id}/edit`}>
              <Edit3 className="h-4 w-4" />
              Sửa metadata
            </Link>
            {canEditUnits ? (
              <Link className={PRIMARY_BUTTON_CLASS} to={builderPath}>
                <Layers className="h-4 w-4" />
                Biên soạn
              </Link>
            ) : null}
            <button className={GHOST_BUTTON_CLASS} disabled={working} onClick={handleClone} type="button">
              <Copy className="h-4 w-4" />
              Nhân bản
            </button>
            {canSubmitReview ? (
              <button className={PRIMARY_BUTTON_CLASS} disabled={working} onClick={handleSubmitReview} type="button">
                <Send className="h-4 w-4" />
                Gửi duyệt để xuất bản
              </button>
            ) : null}
            {program.status === 'PUBLISHED' ? (
              <button className={DANGER_BUTTON_CLASS} disabled={working} onClick={handleArchive} type="button">
                <Archive className="h-4 w-4" />
                Lưu trữ
              </button>
            ) : null}
          </>
        )}
      />

      {error ? <div className={ERROR_NOTICE_CLASS}>{error}</div> : null}
      {success ? <div className={SUCCESS_NOTICE_CLASS}>{success}</div> : null}

      {program.status === 'PUBLISHED' ? (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
          <p className="font-extrabold flex items-center gap-2"><AlertTriangle className="h-4 w-4" /> Giáo trình đã xuất bản</p>
          <p className="mt-1 text-xs leading-6">Nhân bản để tạo bản nháp mới, chỉnh sửa rồi gửi duyệt lại — không sửa trực tiếp bản published.</p>
        </div>
      ) : null}

      {program.activeClassroomCount > 0 ? (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-900">
          <p className="font-extrabold">Đang được {program.activeClassroomCount} lớp sử dụng — không thể lưu trữ.</p>
        </div>
      ) : null}

      <div className="grid gap-5 xl:grid-cols-[1fr_320px]">
        <div className="space-y-5">
          <ProgramSection title="Tổng quan">
            <ProgramMetricGrid items={[
              { label: 'Cấp độ đầu vào', value: program.entryLevel || '—' },
              { label: 'Target IELTS', value: program.targetBand ?? '—' },
              { label: 'Target TOEIC', value: program.targetScore ?? '—' },
              { label: 'Số buổi', value: program.totalSessions || 0 },
            ]} />
            {program.outcomes ? (
              <div className="mt-5 rounded-xl border border-[#dcc0bf]/25 bg-[#fcfbfb] p-4">
                <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-[#8b706e]">Chuẩn đầu ra</p>
                <p className="mt-2 whitespace-pre-line text-sm leading-7 text-[#584140]">{program.outcomes}</p>
              </div>
            ) : null}
          </ProgramSection>

          {isVirtual ? (
            <ProgramSection icon={Video} title="Cấu hình virtual">
              <ProgramMetricGrid items={[
                { label: 'Nền tảng', value: program.virtualPlatform || '—' },
                { label: 'Ghi hình', value: program.recordingAllowed ? `${program.recordingAvailableDays || '∞'} ngày` : 'Không' },
                { label: 'Tài liệu', value: program.materialsDownloadable ? 'Cho tải' : 'Chỉ xem' },
                { label: 'Mở phòng trước', value: `${program.sessionOpenBeforeMinutes ?? 0} phút` },
                { label: 'Điểm danh tự động', value: program.autoAttendanceEnabled ? `≥ ${program.minAttendanceMinutes || 15} phút` : 'Không' },
                { label: 'Thiết bị', value: [program.micRequired && 'Mic', program.speakerRequired && 'Loa', program.cameraRequired && 'Camera'].filter(Boolean).join(', ') || 'Không bắt buộc' },
              ]} />
            </ProgramSection>
          ) : null}

          <ProgramSection
            action={canEditUnits ? <Link className={GHOST_BUTTON_CLASS} to={builderPath}>Quản lý nội dung</Link> : null}
            icon={BookOpen}
            title={`Units / buổi học (${program.units?.length || 0})`}
          >
            {!program.units?.length ? (
              <p className="text-sm text-[#69778a]">Chưa có unit — mở Syllabus Builder để biên soạn.</p>
            ) : (
              <div className="space-y-3">
                {program.units.map((unit, index) => {
                  const refs = [...(unit.materials || []), ...(unit.exercises || []), ...(unit.assessments || []), ...(unit.flashcards || [])];
                  return (
                    <article className="rounded-xl border border-[#dcc0bf]/25 bg-[#fcfbfb] p-4" key={unit.id}>
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="text-[10px] font-bold uppercase tracking-wide text-[#8b706e]">Buổi {index + 1}</p>
                          <h4 className="font-extrabold text-[#26364a]">{unit.title}</h4>
                          {unit.description ? <p className="mt-1 text-sm text-[#69778a]">{unit.description}</p> : null}
                        </div>
                        <span className="rounded-lg bg-white px-2.5 py-1 text-[10px] font-bold text-[#69778a] ring-1 ring-[#dcc0bf]/30">{refs.length} học liệu</span>
                      </div>
                      {refs.length ? (
                        <div className="mt-3 flex flex-wrap gap-1.5">
                          {refs.map((ref) => (
                            <span className="rounded-md bg-white px-2 py-0.5 text-[10px] font-bold text-[#53627a] ring-1 ring-[#dcc0bf]/30" key={`${ref.referenceType}-${ref.referenceId}`}>
                              {refTypeLabel[ref.referenceType] || ref.referenceType}: {ref.title || ref.referenceId}
                            </span>
                          ))}
                        </div>
                      ) : null}
                    </article>
                  );
                })}
              </div>
            )}
          </ProgramSection>
        </div>

        <aside className="space-y-5">
          <ProgramSection title="Trạng thái duyệt">
            <div className="space-y-3 text-sm">
              <ProgramStatusPill label={program.statusLabel || program.status} status={program.status} />
              {program.submittedAt ? <p className="text-[#69778a]">Gửi duyệt: {formatClassroomDate(program.submittedAt)}</p> : null}
              {program.reviewedAt ? <p className="text-[#69778a]">Duyệt lúc: {formatClassroomDate(program.reviewedAt)}</p> : null}
              {program.reviewNote ? <p className="rounded-lg bg-rose-50 p-3 text-xs text-rose-800">Lý do: {program.reviewNote}</p> : null}
            </div>
          </ProgramSection>

          <ProgramSection title="Lớp đang dùng">
            {!activeClassrooms.length ? <p className="text-sm text-[#69778a]">Chưa có lớp active/upcoming.</p> : (
              <ul className="space-y-2">
                {activeClassrooms.map((c) => (
                  <li className="rounded-lg border border-emerald-100 bg-emerald-50/40 px-3 py-2 text-xs font-semibold text-emerald-800" key={c.id}>{c.title}</li>
                ))}
              </ul>
            )}
          </ProgramSection>

          {pastClassrooms.length ? (
            <ProgramSection title="Lớp đã từng dùng">
              <ul className="space-y-2">
                {pastClassrooms.slice(0, 8).map((c) => (
                  <li className="rounded-lg border border-[#dcc0bf]/25 bg-white px-3 py-2 text-xs text-[#69778a]" key={c.id}>{c.title}</li>
                ))}
              </ul>
            </ProgramSection>
          ) : null}
        </aside>
      </div>
    </div>
  );
}
