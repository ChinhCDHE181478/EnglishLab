import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Award,
  BookOpenCheck,
  BriefcaseBusiness,
  CheckCircle2,
  ChevronRight,
  FileBadge,
  LoaderCircle,
  PencilLine,
  Plus,
  Search,
  ShieldCheck,
  Star,
  Trash2,
  UserRoundCheck,
  XCircle,
} from 'lucide-react';
import teacherProfessionalApi from '../../api/teacherProfessionalApi';
import { useAppDialog } from '../../components/ui/AppDialog';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import VietnameseDateInput from '../../components/ui/VietnameseDateInput';

const INPUT_CLASS = 'h-11 w-full rounded-xl border border-[#dfbfbd] bg-white px-3 text-sm outline-none transition focus:border-[#8a0018] focus:ring-4 focus:ring-[#8a0018]/5';
const TEXTAREA_CLASS = 'min-h-28 w-full rounded-xl border border-[#dfbfbd] bg-white px-3 py-3 text-sm leading-6 outline-none transition focus:border-[#8a0018] focus:ring-4 focus:ring-[#8a0018]/5';

const CREDENTIAL_TYPES = [
  { label: 'Chứng chỉ ngôn ngữ', value: 'LANGUAGE_CERTIFICATE' },
  { label: 'Bằng cấp học thuật', value: 'ACADEMIC_DEGREE' },
  { label: 'Chứng chỉ giảng dạy', value: 'TEACHING_CERTIFICATE' },
  { label: 'Đào tạo chuyên môn', value: 'PROFESSIONAL_TRAINING' },
  { label: 'Minh chứng khác', value: 'OTHER' },
];

const emptyProfile = {
  headline: '',
  biography: '',
  specializations: '',
  teachingLanguages: '',
  yearsOfExperience: '',
  highestQualification: '',
  publicProfile: false,
};

const emptyCredential = {
  type: 'LANGUAGE_CERTIFICATE',
  title: '',
  issuer: '',
  credentialNumber: '',
  issuedDate: '',
  expiryDate: '',
  documentUrl: '',
};

const emptyEvaluation = {
  periodStart: '',
  periodEnd: '',
  lessonDeliveryScore: '4',
  learnerSupportScore: '4',
  gradingTimelinessScore: '4',
  professionalismScore: '4',
  strengths: '',
  improvementAreas: '',
  actionPlan: '',
};

const errorMessage = (error, fallback) => error?.response?.data?.message || error?.message || fallback;
const formatDate = (value) => value ? new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`)) : 'Không thời hạn';
const credentialStatus = {
  PENDING: { label: 'Chờ xác minh', className: 'bg-amber-100 text-amber-700' },
  VERIFIED: { label: 'Đã xác minh', className: 'bg-emerald-100 text-emerald-700' },
  REJECTED: { label: 'Bị từ chối', className: 'bg-rose-100 text-rose-700' },
  EXPIRED: { label: 'Đã hết hạn', className: 'bg-slate-200 text-slate-600' },
};

export default function TeacherManagementPage({ mode = 'STAFF' }) {
  const isManager = mode === 'MANAGER';
  const { confirm: confirmDialog, prompt: promptDialog } = useAppDialog();
  const [teachers, setTeachers] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState('');
  const [modal, setModal] = useState(null);
  const [form, setForm] = useState(null);
  const [working, setWorking] = useState(false);

  const loadTeachers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const items = isManager
        ? await teacherProfessionalApi.listForManager()
        : await teacherProfessionalApi.listForStaff();
      setTeachers(items);
      setSelectedId((current) => current || items[0]?.teacherId || null);
    } catch (requestError) {
      setTeachers([]);
      setError(errorMessage(requestError, 'Không thể tải danh sách giáo viên.'));
    } finally {
      setLoading(false);
    }
  }, [isManager]);

  const loadDetail = useCallback(async (teacherId) => {
    if (!teacherId) {
      setDetail(null);
      return;
    }
    setDetailLoading(true);
    setError('');
    try {
      setDetail(isManager
        ? await teacherProfessionalApi.getForManager(teacherId)
        : await teacherProfessionalApi.getForStaff(teacherId));
    } catch (requestError) {
      setDetail(null);
      setError(errorMessage(requestError, 'Không thể tải hồ sơ giáo viên.'));
    } finally {
      setDetailLoading(false);
    }
  }, [isManager]);

  useEffect(() => {
    loadTeachers();
  }, [loadTeachers]);

  useEffect(() => {
    loadDetail(selectedId);
  }, [loadDetail, selectedId]);

  const filteredTeachers = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) return teachers;
    return teachers.filter((teacher) => [
      teacher.fullName,
      teacher.email,
      teacher.specializations,
      teacher.highestQualification,
    ].some((value) => String(value || '').toLowerCase().includes(normalized)));
  }, [keyword, teachers]);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    filteredTeachers,
    8,
    `${mode}|${keyword}`
  );

  const refresh = async () => {
    await Promise.all([loadTeachers(), loadDetail(selectedId)]);
  };

  const openProfile = () => {
    setForm({
      ...emptyProfile,
      headline: detail?.headline || '',
      biography: detail?.biography || '',
      specializations: detail?.specializations || '',
      teachingLanguages: detail?.teachingLanguages || '',
      yearsOfExperience: detail?.yearsOfExperience ?? '',
      highestQualification: detail?.highestQualification || '',
      publicProfile: Boolean(detail?.publicProfile),
    });
    setModal({ type: 'profile' });
  };

  const openCredential = (item = null) => {
    setForm(item ? {
      type: item.type,
      title: item.title,
      issuer: item.issuer,
      credentialNumber: item.credentialNumber || '',
      issuedDate: item.issuedDate || '',
      expiryDate: item.expiryDate || '',
      documentUrl: item.documentUrl || '',
    } : { ...emptyCredential });
    setModal({ type: 'credential', item });
  };

  const openEvaluation = (item = null) => {
    setForm(item ? {
      periodStart: item.periodStart || '',
      periodEnd: item.periodEnd || '',
      lessonDeliveryScore: String(item.lessonDeliveryScore ?? 4),
      learnerSupportScore: String(item.learnerSupportScore ?? 4),
      gradingTimelinessScore: String(item.gradingTimelinessScore ?? 4),
      professionalismScore: String(item.professionalismScore ?? 4),
      strengths: item.strengths || '',
      improvementAreas: item.improvementAreas || '',
      actionPlan: item.actionPlan || '',
    } : { ...emptyEvaluation });
    setModal({ type: 'evaluation', item });
  };

  const submitModal = async (event) => {
    event.preventDefault();
    setWorking(true);
    setError('');
    try {
      if (modal.type === 'profile') {
        await teacherProfessionalApi.updateProfile(detail.teacherId, {
          ...form,
          yearsOfExperience: form.yearsOfExperience === '' ? null : Number(form.yearsOfExperience),
        });
      } else if (modal.type === 'credential') {
        const payload = {
          ...form,
          issuedDate: form.issuedDate || null,
          expiryDate: form.expiryDate || null,
          credentialNumber: form.credentialNumber.trim() || null,
          documentUrl: form.documentUrl.trim() || null,
        };
        if (modal.item) {
          await teacherProfessionalApi.updateCredential(detail.teacherId, modal.item.id, payload);
        } else {
          await teacherProfessionalApi.createCredential(detail.teacherId, payload);
        }
      } else if (modal.type === 'evaluation') {
        const payload = {
          ...form,
          lessonDeliveryScore: Number(form.lessonDeliveryScore),
          learnerSupportScore: Number(form.learnerSupportScore),
          gradingTimelinessScore: Number(form.gradingTimelinessScore),
          professionalismScore: Number(form.professionalismScore),
        };
        if (modal.item) {
          await teacherProfessionalApi.updateEvaluation(detail.teacherId, modal.item.id, payload);
        } else {
          await teacherProfessionalApi.createEvaluation(detail.teacherId, payload);
        }
      }
      setModal(null);
      await refresh();
    } catch (requestError) {
      setError(errorMessage(requestError, 'Không thể lưu thay đổi.'));
    } finally {
      setWorking(false);
    }
  };

  const verifyCredential = async (item, status) => {
    let note = '';
    if (status === 'REJECTED') {
      note = await promptDialog('Nhập lý do từ chối minh chứng này.', '', {
        title: 'Từ chối minh chứng',
        confirmLabel: 'Xác nhận từ chối',
        cancelLabel: 'Hủy',
      });
      if (!note?.trim()) return;
    }
    setWorking(true);
    try {
      await teacherProfessionalApi.verifyCredential(detail.teacherId, item.id, { status, note: note || null });
      await refresh();
    } catch (requestError) {
      setError(errorMessage(requestError, 'Không thể cập nhật trạng thái minh chứng.'));
    } finally {
      setWorking(false);
    }
  };

  const deleteCredential = async (item) => {
    const confirmed = await confirmDialog(`Xóa minh chứng “${item.title}”?`, {
      title: 'Xóa minh chứng giáo viên',
      confirmLabel: 'Xóa',
      tone: 'danger',
    });
    if (!confirmed) return;
    setWorking(true);
    try {
      await teacherProfessionalApi.deleteCredential(detail.teacherId, item.id);
      await refresh();
    } catch (requestError) {
      setError(errorMessage(requestError, 'Không thể xóa minh chứng.'));
    } finally {
      setWorking(false);
    }
  };

  const publishEvaluation = async (item) => {
    const confirmed = await confirmDialog(
      'Sau khi công bố, giáo viên sẽ xem được kết quả và đánh giá không thể chỉnh sửa. Tiếp tục?',
      { title: 'Công bố đánh giá hiệu suất', confirmLabel: 'Công bố' }
    );
    if (!confirmed) return;
    setWorking(true);
    try {
      await teacherProfessionalApi.publishEvaluation(detail.teacherId, item.id);
      await refresh();
    } catch (requestError) {
      setError(errorMessage(requestError, 'Không thể công bố đánh giá.'));
    } finally {
      setWorking(false);
    }
  };

  const deleteEvaluation = async (item) => {
    const confirmed = await confirmDialog('Xóa bản nháp đánh giá này?', {
      title: 'Xóa bản nháp',
      confirmLabel: 'Xóa',
      tone: 'danger',
    });
    if (!confirmed) return;
    setWorking(true);
    try {
      await teacherProfessionalApi.deleteEvaluation(detail.teacherId, item.id);
      await refresh();
    } catch (requestError) {
      setError(errorMessage(requestError, 'Không thể xóa bản nháp đánh giá.'));
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="space-y-6">
      {error && !modal ? <p className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-700" role="alert">{error}</p> : null}

      <div className="grid gap-6 xl:grid-cols-[380px_minmax(0,1fr)]">
        <section className="rounded-[24px] border border-[#ead9db] bg-white p-4 shadow-sm">
          <label className="relative block">
            <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[#9b8582]" />
            <input
              className={`${INPUT_CLASS} pl-10`}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Tìm tên, email, chuyên môn..."
              value={keyword}
            />
          </label>
          {loading ? (
            <div className="flex min-h-[520px] items-center justify-center text-sm font-semibold text-[#756361]">
              <LoaderCircle className="mr-2 h-5 w-5 animate-spin text-[#8a0018]" /> Đang tải...
            </div>
          ) : null}
          {!loading && pageItems.length === 0 ? (
            <div className="flex min-h-[520px] flex-col items-center justify-center px-6 text-center">
              <UserRoundCheck className="h-8 w-8 text-[#8a0018]" />
              <p className="mt-4 font-extrabold text-[#341c1d]">Không có giáo viên phù hợp</p>
            </div>
          ) : null}
          {!loading ? (
            <div className="mt-4 space-y-2">
              {pageItems.map((teacher) => (
                <button
                  className={`w-full rounded-2xl border p-4 text-left transition ${
                    selectedId === teacher.teacherId
                      ? 'border-[#8a0018] bg-[#fff1f2] shadow-sm'
                      : 'border-[#ead9db] hover:border-[#dfbfbd] hover:bg-[#fffafa]'
                  }`}
                  key={teacher.teacherId}
                  onClick={() => setSelectedId(teacher.teacherId)}
                  type="button"
                >
                  <div className="flex items-center gap-3">
                    <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#4b0009] text-sm font-black text-white">
                      {String(teacher.fullName || 'G').charAt(0).toUpperCase()}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-extrabold text-[#2b2828]">{teacher.fullName}</p>
                      <p className="truncate text-xs text-[#756361]">{teacher.email}</p>
                    </div>
                    <ChevronRight className="h-4 w-4 text-[#9b8582]" />
                  </div>
                  <div className="mt-3 grid grid-cols-3 gap-2 text-center">
                    <MiniMetric label="Lớp" value={teacher.assignedClassrooms} />
                    <MiniMetric label="Buổi" value={teacher.totalSessions} />
                    <MiniMetric label="Điểm" value={teacher.latestPerformanceScore ?? '—'} />
                  </div>
                </button>
              ))}
            </div>
          ) : null}
          {!loading && totalPages > 1 ? (
            <div className="mt-4">
              <Pagination onChange={setPage} page={page} pageSize={8} totalItems={totalItems} totalPages={totalPages} />
            </div>
          ) : null}
        </section>

        <section className="min-w-0 rounded-[24px] border border-[#ead9db] bg-white p-5 shadow-sm md:p-7">
          {detailLoading ? (
            <div className="flex min-h-[620px] items-center justify-center text-sm font-semibold text-[#756361]">
              <LoaderCircle className="mr-2 h-5 w-5 animate-spin text-[#8a0018]" /> Đang tải hồ sơ...
            </div>
          ) : null}
          {!detailLoading && !detail ? (
            <div className="flex min-h-[620px] flex-col items-center justify-center text-center">
              <BriefcaseBusiness className="h-9 w-9 text-[#8a0018]" />
              <p className="mt-4 font-extrabold text-[#341c1d]">Chọn một giáo viên để xem chi tiết</p>
            </div>
          ) : null}
          {!detailLoading && detail ? (
            <div className="space-y-7">
              <div className="flex flex-col gap-4 border-b border-[#f0e4e2] pb-6 md:flex-row md:items-start md:justify-between">
                <div>
                  <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#8a0018]">Hồ sơ giáo viên</p>
                  <h2 className="mt-2 font-['Manrope'] text-3xl font-black text-[#2b2828]">{detail.fullName}</h2>
                  <p className="mt-1 text-sm text-[#756361]">{detail.email}{detail.phoneNumber ? ` · ${detail.phoneNumber}` : ''}</p>
                  <p className="mt-3 max-w-2xl text-sm font-semibold leading-6 text-[#584140]">
                    {detail.headline || 'Chưa cập nhật tiêu đề chuyên môn.'}
                  </p>
                </div>
                {!isManager ? (
                  <button className="inline-flex items-center justify-center gap-2 rounded-xl bg-[#4b0009] px-4 py-2.5 text-sm font-extrabold text-white hover:bg-[#730014]" onClick={openProfile} type="button">
                    <PencilLine className="h-4 w-4" /> Cập nhật hồ sơ
                  </button>
                ) : (
                  <button className="inline-flex items-center justify-center gap-2 rounded-xl bg-[#4b0009] px-4 py-2.5 text-sm font-extrabold text-white hover:bg-[#730014]" onClick={() => openEvaluation()} type="button">
                    <Plus className="h-4 w-4" /> Tạo kỳ đánh giá
                  </button>
                )}
              </div>

              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <StatCard icon={BookOpenCheck} label="Lớp phụ trách" value={detail.assignedClassrooms} />
                <StatCard icon={BriefcaseBusiness} label="Buổi đã lên lịch" value={detail.totalSessions} />
                <StatCard icon={CheckCircle2} label="Buổi hoàn thành" value={detail.completedSessions} />
                <StatCard icon={Star} label="Điểm gần nhất" value={detail.latestPerformanceScore ?? '—'} />
              </div>

              <div className="grid gap-5 lg:grid-cols-2">
                <InfoBlock label="Chuyên môn" value={detail.specializations || 'Chưa cập nhật'} />
                <InfoBlock label="Học vị cao nhất" value={detail.highestQualification || 'Chưa cập nhật'} />
                <InfoBlock label="Ngôn ngữ giảng dạy" value={detail.teachingLanguages || 'Chưa cập nhật'} />
                <InfoBlock label="Kinh nghiệm" value={detail.yearsOfExperience == null ? 'Chưa cập nhật' : `${detail.yearsOfExperience} năm`} />
              </div>
              {detail.biography ? <InfoBlock label="Giới thiệu chuyên môn" value={detail.biography} /> : null}

              {!isManager ? (
                <CredentialsSection
                  detail={detail}
                  onCreate={() => openCredential()}
                  onDelete={deleteCredential}
                  onEdit={openCredential}
                  onVerify={verifyCredential}
                  working={working}
                />
              ) : (
                <EvaluationsSection
                  detail={detail}
                  onDelete={deleteEvaluation}
                  onEdit={openEvaluation}
                  onPublish={publishEvaluation}
                  working={working}
                />
              )}
            </div>
          ) : null}
        </section>
      </div>

      {modal ? (
        <EditorModal
          error={error}
          form={form}
          modal={modal}
          onChange={(key, value) => setForm((current) => ({ ...current, [key]: value }))}
          onClose={() => setModal(null)}
          onSubmit={submitModal}
          working={working}
        />
      ) : null}
    </div>
  );
}

function CredentialsSection({ detail, onCreate, onDelete, onEdit, onVerify, working }) {
  return (
    <section>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">Minh chứng chuyên môn</h3>
          <p className="mt-1 text-sm text-[#756361]">{detail.verifiedCredentials} minh chứng còn hiệu lực đã được xác minh.</p>
        </div>
        <button className="inline-flex items-center gap-2 rounded-xl border border-[#8a0018] px-4 py-2.5 text-sm font-extrabold text-[#8a0018] hover:bg-[#fff1f2]" onClick={onCreate} type="button">
          <Plus className="h-4 w-4" /> Thêm minh chứng
        </button>
      </div>
      {!detail.credentials?.length ? (
        <div className="mt-4 rounded-2xl border border-dashed border-[#dfbfbd] bg-[#fffafa] p-8 text-center text-sm text-[#756361]">Chưa có bằng cấp hoặc chứng chỉ nào.</div>
      ) : (
        <div className="mt-4 space-y-3">
          {detail.credentials.map((item) => {
            const status = credentialStatus[item.verificationStatus] || credentialStatus.PENDING;
            return (
              <article className="rounded-2xl border border-[#ead9db] p-4" key={item.id}>
                <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <FileBadge className="h-5 w-5 text-[#8a0018]" />
                      <h4 className="font-extrabold text-[#2b2828]">{item.title}</h4>
                      <span className={`rounded-full px-2.5 py-1 text-[10px] font-extrabold ${status.className}`}>{status.label}</span>
                    </div>
                    <p className="mt-2 text-sm text-[#584140]">{item.issuer}{item.credentialNumber ? ` · Mã ${item.credentialNumber}` : ''}</p>
                    <p className="mt-1 text-xs text-[#8c716f]">Cấp: {formatDate(item.issuedDate)} · Hết hạn: {formatDate(item.expiryDate)}</p>
                    {item.verificationNote ? <p className="mt-2 rounded-xl bg-[#fff7f7] p-3 text-xs leading-5 text-[#756361]">{item.verificationNote}</p> : null}
                    {item.documentUrl ? <a className="mt-2 inline-flex text-xs font-bold text-[#8a0018] underline" href={item.documentUrl} rel="noreferrer" target="_blank">Mở minh chứng</a> : null}
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {item.verificationStatus === 'PENDING' ? (
                      <>
                        <button className="rounded-lg bg-emerald-100 px-3 py-2 text-xs font-extrabold text-emerald-700 disabled:opacity-50" disabled={working} onClick={() => onVerify(item, 'VERIFIED')} type="button">Xác minh</button>
                        <button className="rounded-lg bg-rose-100 px-3 py-2 text-xs font-extrabold text-rose-700 disabled:opacity-50" disabled={working} onClick={() => onVerify(item, 'REJECTED')} type="button">Từ chối</button>
                      </>
                    ) : null}
                    <button className="rounded-lg border border-[#dfbfbd] p-2 text-[#756361] hover:text-[#8a0018]" onClick={() => onEdit(item)} type="button"><PencilLine className="h-4 w-4" /></button>
                    <button className="rounded-lg border border-rose-200 p-2 text-rose-600 hover:bg-rose-50" onClick={() => onDelete(item)} type="button"><Trash2 className="h-4 w-4" /></button>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}

function EvaluationsSection({ detail, onDelete, onEdit, onPublish, working }) {
  return (
    <section>
      <div>
        <h3 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">Lịch sử đánh giá</h3>
        <p className="mt-1 text-sm text-[#756361]">Giáo viên chỉ xem được đánh giá đã công bố.</p>
      </div>
      {!detail.evaluations?.length ? (
        <div className="mt-4 rounded-2xl border border-dashed border-[#dfbfbd] bg-[#fffafa] p-8 text-center text-sm text-[#756361]">Chưa có kỳ đánh giá nào.</div>
      ) : (
        <div className="mt-4 space-y-4">
          {detail.evaluations.map((item) => (
            <article className="rounded-2xl border border-[#ead9db] p-5" key={item.id}>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <Award className="h-5 w-5 text-[#8a0018]" />
                    <h4 className="font-extrabold text-[#2b2828]">{formatDate(item.periodStart)} – {formatDate(item.periodEnd)}</h4>
                    <span className={`rounded-full px-2.5 py-1 text-[10px] font-extrabold ${item.status === 'PUBLISHED' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}>
                      {item.status === 'PUBLISHED' ? 'Đã công bố' : 'Bản nháp'}
                    </span>
                  </div>
                  <p className="mt-2 text-3xl font-black text-[#4b0009]">{Number(item.overallScore).toFixed(2)}<span className="text-sm text-[#8c716f]">/5</span></p>
                </div>
                {item.status === 'DRAFT' ? (
                  <div className="flex gap-2">
                    <button className="rounded-lg border border-[#dfbfbd] p-2 text-[#756361] hover:text-[#8a0018]" onClick={() => onEdit(item)} type="button"><PencilLine className="h-4 w-4" /></button>
                    <button className="rounded-lg bg-[#4b0009] px-3 py-2 text-xs font-extrabold text-white disabled:opacity-50" disabled={working} onClick={() => onPublish(item)} type="button">Công bố</button>
                    <button className="rounded-lg border border-rose-200 p-2 text-rose-600 hover:bg-rose-50" onClick={() => onDelete(item)} type="button"><Trash2 className="h-4 w-4" /></button>
                  </div>
                ) : null}
              </div>
              <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                <MiniMetric label="Giảng dạy" value={item.lessonDeliveryScore} />
                <MiniMetric label="Hỗ trợ" value={item.learnerSupportScore} />
                <MiniMetric label="Chấm bài" value={item.gradingTimelinessScore} />
                <MiniMetric label="Tác phong" value={item.professionalismScore} />
              </div>
              <div className="mt-4 grid gap-3 lg:grid-cols-3">
                <InfoBlock label="Điểm mạnh" value={item.strengths || 'Chưa ghi nhận'} />
                <InfoBlock label="Cần cải thiện" value={item.improvementAreas || 'Chưa ghi nhận'} />
                <InfoBlock label="Kế hoạch hành động" value={item.actionPlan || 'Chưa ghi nhận'} />
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function EditorModal({ error, form, modal, onChange, onClose, onSubmit, working }) {
  const title = modal.type === 'profile'
    ? 'Cập nhật hồ sơ chuyên môn'
    : modal.type === 'credential'
      ? `${modal.item ? 'Cập nhật' : 'Thêm'} minh chứng`
      : `${modal.item ? 'Cập nhật' : 'Tạo'} kỳ đánh giá`;
  return (
    <div className="fixed inset-0 z-[80] flex items-center justify-center bg-[#260006]/50 p-4 backdrop-blur-sm">
      <form className="max-h-[92vh] w-full max-w-3xl overflow-y-auto rounded-[28px] border border-[#ead9db] bg-white p-6 shadow-2xl" onSubmit={onSubmit}>
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#8a0018]">Quản lý giáo viên</p>
            <h2 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">{title}</h2>
          </div>
          <button aria-label="Đóng" className="rounded-xl p-2 text-[#756361] hover:bg-[#fff1f2] disabled:opacity-50" disabled={working} onClick={onClose} type="button"><XCircle className="h-5 w-5" /></button>
        </div>
        {error ? <p className="mt-5 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-700" role="alert">{error}</p> : null}
        {modal.type === 'profile' ? <ProfileFields form={form} onChange={onChange} /> : null}
        {modal.type === 'credential' ? <CredentialFields form={form} onChange={onChange} /> : null}
        {modal.type === 'evaluation' ? <EvaluationFields form={form} onChange={onChange} /> : null}
        <div className="mt-6 flex justify-end gap-3">
          <button className="rounded-xl border border-[#dfbfbd] px-5 py-2.5 text-sm font-bold text-[#730014] disabled:opacity-50" disabled={working} onClick={onClose} type="button">Hủy</button>
          <button className="rounded-xl bg-[#4b0009] px-5 py-2.5 text-sm font-extrabold text-white disabled:opacity-60" disabled={working} type="submit">{working ? 'Đang lưu...' : 'Lưu thay đổi'}</button>
        </div>
      </form>
    </div>
  );
}

function ProfileFields({ form, onChange }) {
  return (
    <div className="mt-6 grid gap-4 sm:grid-cols-2">
      <Field label="Tiêu đề chuyên môn"><input className={INPUT_CLASS} maxLength={180} onChange={(e) => onChange('headline', e.target.value)} value={form.headline} /></Field>
      <Field label="Học vị cao nhất"><input className={INPUT_CLASS} maxLength={250} onChange={(e) => onChange('highestQualification', e.target.value)} value={form.highestQualification} /></Field>
      <Field label="Chuyên môn"><input className={INPUT_CLASS} maxLength={700} onChange={(e) => onChange('specializations', e.target.value)} placeholder="IELTS, TOEIC, Academic Writing..." value={form.specializations} /></Field>
      <Field label="Ngôn ngữ giảng dạy"><input className={INPUT_CLASS} maxLength={300} onChange={(e) => onChange('teachingLanguages', e.target.value)} value={form.teachingLanguages} /></Field>
      <Field label="Số năm kinh nghiệm"><input className={INPUT_CLASS} max={60} min={0} onChange={(e) => onChange('yearsOfExperience', e.target.value)} type="number" value={form.yearsOfExperience} /></Field>
      <label className="flex items-center gap-3 self-end rounded-xl border border-[#dfbfbd] px-4 py-3 text-sm font-bold text-[#584140]">
        <input checked={form.publicProfile} onChange={(e) => onChange('publicProfile', e.target.checked)} type="checkbox" /> Cho phép hiển thị hồ sơ công khai
      </label>
      <Field className="sm:col-span-2" label="Giới thiệu chuyên môn"><textarea className={TEXTAREA_CLASS} maxLength={5000} onChange={(e) => onChange('biography', e.target.value)} value={form.biography} /></Field>
    </div>
  );
}

function CredentialFields({ form, onChange }) {
  return (
    <div className="mt-6 grid gap-4 sm:grid-cols-2">
      <Field label="Loại minh chứng"><BrandedSelect buttonClassName="h-11 rounded-xl border-[#dfbfbd] py-2 shadow-none" onChange={(e) => onChange('type', e.target.value)} options={CREDENTIAL_TYPES} value={form.type} /></Field>
      <Field label="Tên chứng chỉ / bằng cấp"><input className={INPUT_CLASS} maxLength={250} onChange={(e) => onChange('title', e.target.value)} required value={form.title} /></Field>
      <Field label="Đơn vị cấp"><input className={INPUT_CLASS} maxLength={250} onChange={(e) => onChange('issuer', e.target.value)} required value={form.issuer} /></Field>
      <Field label="Mã chứng chỉ"><input className={INPUT_CLASS} maxLength={150} onChange={(e) => onChange('credentialNumber', e.target.value)} value={form.credentialNumber} /></Field>
      <Field label="Ngày cấp"><VietnameseDateInput className={INPUT_CLASS} onChange={(value) => onChange('issuedDate', value)} value={form.issuedDate} /></Field>
      <Field label="Ngày hết hạn"><VietnameseDateInput className={INPUT_CLASS} min={form.issuedDate || undefined} onChange={(value) => onChange('expiryDate', value)} value={form.expiryDate} /></Field>
      <Field className="sm:col-span-2" label="URL minh chứng"><input className={INPUT_CLASS} maxLength={700} onChange={(e) => onChange('documentUrl', e.target.value)} placeholder="https://..." type="url" value={form.documentUrl} /></Field>
    </div>
  );
}

function EvaluationFields({ form, onChange }) {
  return (
    <div className="mt-6 space-y-5">
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Từ ngày"><VietnameseDateInput className={INPUT_CLASS} onChange={(value) => onChange('periodStart', value)} required value={form.periodStart} /></Field>
        <Field label="Đến ngày"><VietnameseDateInput className={INPUT_CLASS} min={form.periodStart || undefined} onChange={(value) => onChange('periodEnd', value)} required value={form.periodEnd} /></Field>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[
          ['lessonDeliveryScore', 'Chất lượng giảng dạy'],
          ['learnerSupportScore', 'Hỗ trợ học viên'],
          ['gradingTimelinessScore', 'Đúng hạn chấm bài'],
          ['professionalismScore', 'Tác phong'],
        ].map(([key, label]) => (
          <Field key={key} label={`${label} (1–5)`}>
            <input className={INPUT_CLASS} max="5" min="1" onChange={(e) => onChange(key, e.target.value)} required step="0.25" type="number" value={form[key]} />
          </Field>
        ))}
      </div>
      <div className="grid gap-4 lg:grid-cols-3">
        <Field label="Điểm mạnh"><textarea className={TEXTAREA_CLASS} maxLength={1500} onChange={(e) => onChange('strengths', e.target.value)} value={form.strengths} /></Field>
        <Field label="Cần cải thiện"><textarea className={TEXTAREA_CLASS} maxLength={1500} onChange={(e) => onChange('improvementAreas', e.target.value)} value={form.improvementAreas} /></Field>
        <Field label="Kế hoạch hành động"><textarea className={TEXTAREA_CLASS} maxLength={1500} onChange={(e) => onChange('actionPlan', e.target.value)} value={form.actionPlan} /></Field>
      </div>
    </div>
  );
}

function Field({ label, className = '', children }) {
  return <label className={`block ${className}`}><span className="mb-2 block text-xs font-extrabold uppercase tracking-wide text-[#756361]">{label}</span>{children}</label>;
}

function MiniMetric({ label, value }) {
  return <div className="rounded-xl bg-[#fff7f7] px-2 py-2"><p className="font-black text-[#4b0009]">{value ?? 0}</p><p className="text-[9px] font-bold uppercase text-[#9b8582]">{label}</p></div>;
}

function StatCard({ icon: Icon, label, value }) {
  return <div className="rounded-2xl border border-[#ead9db] bg-[#fffafa] p-4"><Icon className="h-5 w-5 text-[#8a0018]" /><p className="mt-3 text-2xl font-black text-[#341c1d]">{value ?? 0}</p><p className="mt-1 text-xs font-bold text-[#756361]">{label}</p></div>;
}

function InfoBlock({ label, value }) {
  return <div className="rounded-2xl bg-[#f8f5f4] p-4"><p className="text-[10px] font-extrabold uppercase tracking-[0.12em] text-[#9b8582]">{label}</p><p className="mt-2 whitespace-pre-wrap text-sm font-semibold leading-6 text-[#584140]">{value}</p></div>;
}
