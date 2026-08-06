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
  X,
} from 'lucide-react';
import curriculumApi from '../../api/curriculumApi';
import {
  ProgramDetailHero,
  ProgramMetricGrid,
  ProgramSection,
  ProgramStatusPill,
} from '../../components/curriculum/CurriculumProgramUi';
import { ContentManagerLoadingState } from '../../components/content-manager/ContentManagerUi';
import { ENGLISH_SKILL_OPTIONS, englishTrackLabel } from '../../utils/englishProgramProfile';
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
import { useAppDialog } from '../../components/ui/AppDialog';

const refTypeLabel = {
  MATERIAL: 'Tài liệu',
  EXERCISE: 'Bài tập',
  ASSESSMENT: 'Đề kiểm tra',
  FLASHCARD: 'Flashcard',
};

export default function ContentManagerCurriculumProgramDetailPage({ mode = 'OFFLINE' }) {
  const { confirm: confirmDialog } = useAppDialog();
  const { id } = useParams();
  const navigate = useNavigate();
  const isVirtual = mode === 'VIRTUAL';
  const listPath = isVirtual ? '/content-manager/virtual-programs' : '/content-manager/offline-programs';
  const builderPath = `/content-manager/syllabus-builder?programId=${id}`;

  const [program, setProgram] = useState(null);
  const [selectedUnitModal, setSelectedUnitModal] = useState(null);
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

  const handlePublish = async () => {
    if (!await confirmDialog('Các lớp mới có thể sử dụng chương trình ngay sau khi xuất bản.', {
      title: 'Xuất bản chương trình đào tạo',
      confirmLabel: 'Xuất bản',
    })) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const updated = await curriculumApi.publishCurriculumProgram(id);
      setProgram(updated);
      setSuccess('Đã xuất bản giáo trình.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể xuất bản giáo trình.');
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
    if (!await confirmDialog(`Lưu trữ giáo trình “${program?.title}”?`, {
      title: 'Lưu trữ giáo trình',
      confirmLabel: 'Lưu trữ',
      tone: 'danger',
    })) return;
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
  const canPublish = ['DRAFT', 'PENDING_REVIEW', 'REJECTED'].includes(program.status);
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
            {canPublish ? (
              <button className={PRIMARY_BUTTON_CLASS} disabled={working} onClick={handlePublish} type="button">
                <Send className="h-4 w-4" />
                Xuất bản giáo trình
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
          <p className="mt-1 text-xs leading-6">Nhân bản để tạo bản nháp mới, chỉnh sửa rồi tự xuất bản — không sửa trực tiếp bản đang phát hành.</p>
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
              { label: 'Chương trình', value: englishTrackLabel(program.programTrack) },
              { label: 'Cấp độ đầu vào', value: program.entryLevel || '—' },
              {
                label: 'Mục tiêu',
                value: program.examCategory === 'IELTS'
                  ? `Band ${program.targetBand ?? '—'}`
                  : program.examCategory === 'TOEIC'
                    ? `${program.targetScore ?? '—'} điểm`
                    : 'Theo chuẩn đầu ra',
              },
              {
                label: 'Kỹ năng trọng tâm',
                value: String(program.focusSkills || '').split(',').filter(Boolean).map((skill) => (
                  ENGLISH_SKILL_OPTIONS.find((item) => item.value === skill)?.label || skill
                )).join(', ') || '—',
              },
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
                    <article
                      className="group rounded-xl border border-[#dcc0bf]/25 bg-[#fcfbfb] p-4 transition hover:border-[#8a0018] hover:bg-[#fffafa] cursor-pointer"
                      key={unit.id}
                      onClick={() => setSelectedUnitModal(unit)}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <div className="flex items-center gap-2">
                            <p className="text-[10px] font-extrabold uppercase tracking-wide text-[#8a0018]">Buổi {index + 1}</p>
                            {unit.unitCode ? <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[9px] font-bold text-slate-600">{unit.unitCode}</span> : null}
                          </div>
                          <h4 className="font-extrabold text-[#26364a] group-hover:text-[#8a0018] transition">{unit.title}</h4>
                          {unit.description ? <p className="mt-1 text-sm text-[#69778a] line-clamp-2">{unit.description}</p> : null}
                        </div>
                        <span className="rounded-lg bg-white px-2.5 py-1 text-[10px] font-extrabold text-[#730014] ring-1 ring-[#dcc0bf]/40 shrink-0">
                          {refs.length} tài nguyên (Xem chi tiết)
                        </span>
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

        {selectedUnitModal && (
          <div className="fixed inset-0 z-[100] flex items-center justify-center bg-[#1a0004]/50 p-4 backdrop-blur-sm">
            <div className="w-full max-w-2xl max-h-[85vh] overflow-y-auto rounded-[28px] border border-[#ead9db] bg-white p-6 shadow-2xl space-y-6">
              <div className="flex items-start justify-between border-b border-[#f1e4e5] pb-4">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="rounded-full bg-[#fff1f2] px-3 py-0.5 text-xs font-black text-[#8a0018]">
                      Buổi {selectedUnitModal.displayOrder || '—'}
                    </span>
                    {selectedUnitModal.unitCode ? (
                      <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-bold text-slate-600">
                        {selectedUnitModal.unitCode}
                      </span>
                    ) : null}
                  </div>
                  <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#2b2828]">
                    {selectedUnitModal.title}
                  </h3>
                </div>
                <button
                  aria-label="Đóng"
                  className="flex h-9 w-9 items-center justify-center rounded-full border border-slate-200 bg-slate-50 text-slate-500 transition hover:bg-rose-50 hover:text-rose-700"
                  onClick={() => setSelectedUnitModal(null)}
                  type="button"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              {selectedUnitModal.description ? (
                <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-4">
                  <h4 className="text-xs font-extrabold uppercase tracking-wider text-[#8b706e]">Mô tả & Mục tiêu bài học</h4>
                  <p className="mt-1.5 text-sm leading-6 text-[#2b2828] whitespace-pre-line">{selectedUnitModal.description}</p>
                </div>
              ) : null}

              <div className="space-y-4">
                <h4 className="text-xs font-extrabold uppercase tracking-wider text-[#8a0018]">Danh sách học liệu & tài nguyên đính kèm</h4>
                {![
                  ...(selectedUnitModal.materials || []),
                  ...(selectedUnitModal.exercises || []),
                  ...(selectedUnitModal.assessments || []),
                  ...(selectedUnitModal.flashcards || []),
                ].length ? (
                  <p className="rounded-xl border border-dashed border-slate-200 p-4 text-center text-xs font-semibold text-slate-400">
                    Unit này chưa có tài liệu đính kèm.
                  </p>
                ) : (
                  <div className="space-y-2">
                    {[
                      ...(selectedUnitModal.materials || []).map((m) => ({ ...m, typeName: 'Tài liệu học tập' })),
                      ...(selectedUnitModal.exercises || []).map((e) => ({ ...e, typeName: 'Bài tập' })),
                      ...(selectedUnitModal.assessments || []).map((a) => ({ ...a, typeName: 'Đề luyện tập' })),
                      ...(selectedUnitModal.flashcards || []).map((f) => ({ ...f, typeName: 'Bộ Flashcard' })),
                    ].map((item, idx) => (
                      <div className="flex items-center justify-between rounded-xl border border-[#ead9db] bg-[#fffcfc] p-3 text-xs" key={idx}>
                        <div className="flex items-center gap-3 min-w-0">
                          <span className="rounded-lg bg-[#fff1f2] px-2.5 py-1 text-[10px] font-extrabold text-[#8a0018]">
                            {item.typeName}
                          </span>
                          <span className="font-bold text-[#2b2828] truncate">{item.title || item.referenceId || 'Tài liệu chi tiết'}</span>
                        </div>
                        {item.documentUrl || item.materialUrl ? (
                          <a
                            className="inline-flex items-center gap-1 rounded-lg border border-[#dfbfbd] bg-white px-3 py-1 text-[11px] font-bold text-[#730014] hover:bg-[#fff1f2]"
                            href={item.documentUrl || item.materialUrl}
                            target="_blank"
                            rel="noreferrer"
                          >
                            Tải tài liệu
                          </a>
                        ) : (
                          <span className="text-[10px] text-slate-400 font-semibold">Đã tải vào unit</span>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="flex justify-end pt-2">
                <button
                  className="rounded-xl bg-[#4b0009] px-5 py-2.5 text-xs font-extrabold text-white transition hover:bg-[#730014]"
                  onClick={() => setSelectedUnitModal(null)}
                  type="button"
                >
                  Đóng cửa sổ
                </button>
              </div>
            </div>
          </div>
        )}

        <aside className="space-y-5">
          <ProgramSection title="Trạng thái xuất bản">
            <div className="space-y-3 text-sm">
              <ProgramStatusPill label={program.statusLabel || program.status} status={program.status} />
              {program.submittedAt ? <p className="text-[#69778a]">Xuất bản: {formatClassroomDate(program.submittedAt)}</p> : null}
              {program.reviewedAt ? <p className="text-[#69778a]">Ghi nhận lúc: {formatClassroomDate(program.reviewedAt)}</p> : null}
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
