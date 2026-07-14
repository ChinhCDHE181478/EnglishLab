import { useEffect, useMemo, useState } from 'react';
import {
  Download,
  FileText,
  Paperclip,
  Plus,
  ShieldCheck,
  Trash2,
  Upload,
  X,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { ClassroomEmptyState } from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../ui/Pagination';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDate, formatClassroomDateTime, formatClassroomTime } from '../../utils/classroomHelpers';

const uploadFormInitial = {
  title: '',
  fileUrl: '',
  description: '',
  sessionId: '',
};

const inferFileType = (fileNameOrUrl) => {
  const match = String(fileNameOrUrl || '').match(/\.([a-z0-9]+)(?:\?|$)/i);
  return match ? match[1].toUpperCase() : '';
};

const isMandatoryMaterial = (material) => (
  material?.mandatory === true
  || ['PROGRAM_LIBRARY', 'CURRICULUM_LIBRARY'].includes(String(material?.sourceType || '').toUpperCase())
);

const sessionOptionsFor = (sessions) => ([
  { label: 'Không gắn buổi học cụ thể', value: '' },
  ...(sessions || []).map((session) => ({
    label: `${session.title || session.sessionContent || `Buổi #${session.id}`} · ${formatSessionSummary(session)}`,
    value: String(session.id),
  })),
]);

export default function TeacherMaterialsSection({
  classroomId,
  materials,
  sessions,
  onMaterialsChange,
  onMessage,
}) {
  const [formOpen, setFormOpen] = useState(false);
  const [uploadForm, setUploadForm] = useState(uploadFormInitial);
  const [attachmentFile, setAttachmentFile] = useState(null);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const sessionOptions = useMemo(() => sessionOptionsFor(sessions), [sessions]);
  
  const mandatoryMaterials = useMemo(
    () => materials.filter(isMandatoryMaterial),
    [materials],
  );
  
  const supplementaryMaterials = useMemo(
    () => materials.filter((material) => !isMandatoryMaterial(material)),
    [materials],
  );

  const { page: mandPage, setPage: setMandPage, totalPages: mandTotalPages, pageItems: paginatedMandatory, totalItems: mandTotalItems } = usePagination(
    mandatoryMaterials,
    4,
    `mandatory-${classroomId}`
  );
  
  const { page: suppPage, setPage: setSuppPage, totalPages: suppTotalPages, pageItems: paginatedSupplementary, totalItems: suppTotalItems } = usePagination(
    supplementaryMaterials,
    4,
    `supplementary-${classroomId}`
  );

  const refreshMaterials = async () => {
    const refreshed = await classroomApi.getTeacherMaterials(classroomId);
    onMaterialsChange?.(refreshed);
  };

  const openCreateForm = () => {
    setUploadForm(uploadFormInitial);
    setAttachmentFile(null);
    setFormOpen(true);
  };

  const resetForm = () => {
    setUploadForm(uploadFormInitial);
    setAttachmentFile(null);
    setFormOpen(false);
  };

  const handleSaveMaterial = async () => {
    if (!uploadForm.title.trim()) {
      onMessage?.('Vui lòng nhập tiêu đề tài liệu.');
      return;
    }
    if (!attachmentFile && !uploadForm.fileUrl.trim()) {
      onMessage?.('Vui lòng tải lên tệp hoặc dán liên kết tài liệu.');
      return;
    }

    setSaving(true);
    onMessage?.('');
    try {
      let fileUrl = uploadForm.fileUrl.trim();
      let fileType = inferFileType(fileUrl);
      if (attachmentFile) {
        const uploaded = await classroomApi.uploadHomeworkAttachment(attachmentFile);
        fileUrl = uploaded.url;
        fileType = inferFileType(uploaded.originalFileName || uploaded.fileName || fileUrl);
      }

      await classroomApi.createTeacherMaterial(classroomId, {
        title: uploadForm.title.trim(),
        fileUrl,
        fileType: fileType || null,
        description: uploadForm.description.trim() || null,
        materialType: fileType || 'LINK',
        provider: 'Giáo viên phụ trách',
        visibility: 'LEARNERS_IN_CLASS',
        sessionId: uploadForm.sessionId ? Number(uploadForm.sessionId) : null,
      });
      await refreshMaterials();
      onMessage?.('Đã thêm tài liệu bổ trợ cho lớp.');
      resetForm();
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể đăng tài liệu bổ trợ cho lớp.'));
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteMaterial = async (material) => {
    if (isMandatoryMaterial(material)) {
      onMessage?.('Học liệu bắt buộc phải được cập nhật từ chương trình, không thể xóa trực tiếp trong lớp.');
      return;
    }
    if (!window.confirm(`Xóa tài liệu bổ trợ "${material.title}" khỏi lớp này?`)) return;

    setDeletingId(material.id);
    onMessage?.('');
    try {
      await classroomApi.deleteTeacherMaterial(material.id);
      await refreshMaterials();
      onMessage?.('Đã xóa tài liệu bổ trợ khỏi lớp.');
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể xóa tài liệu.'));
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <section className="rounded-2xl border border-[#dfbfbd]/20 bg-[#fffafb] p-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="max-w-3xl">
            <div className="flex items-center gap-2">
              <ShieldCheck className="h-5 w-5 text-[#730014]" />
              <h4 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Tài liệu lớp học</h4>
            </div>
            <p className="mt-2 text-sm leading-6 text-[#584140]">
              Học liệu bắt buộc được lấy tự động từ chương trình đã duyệt. Giáo viên chỉ bổ sung tài liệu riêng phát sinh trong quá trình giảng dạy.
            </p>
          </div>
          <button
            className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-4 py-2.5 text-sm font-extrabold text-white transition hover:bg-[#730014] active:scale-95"
            onClick={openCreateForm}
            type="button"
          >
            <Plus className="h-4 w-4" />
            Thêm tài liệu bổ trợ
          </button>
        </div>

        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <StatPill label="Bắt buộc từ chương trình" value={mandatoryMaterials.length} />
          <StatPill label="Bổ trợ của giáo viên" value={supplementaryMaterials.length} />
          <StatPill label="Tổng tài liệu" value={materials.length} />
        </div>
      </section>

      {formOpen && (
        <EditorModal onClose={resetForm}>
          <SupplementaryMaterialForm
            attachmentFile={attachmentFile}
            form={uploadForm}
            onAttachmentChange={(file) => {
              setAttachmentFile(file);
              if (file) setUploadForm((current) => ({ ...current, fileUrl: '' }));
            }}
            onCancel={resetForm}
            onChange={(field, value) => setUploadForm((current) => ({ ...current, [field]: value }))}
            onSave={handleSaveMaterial}
            saving={saving}
            sessionOptions={sessionOptions}
          />
        </EditorModal>
      )}

      <section className="space-y-4">
        <div>
          <h5 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Học liệu bắt buộc của chương trình</h5>
          <p className="mt-1 text-sm text-[#584140]">Nội dung này do Content Manager chuẩn hóa và tự động áp dụng cho lớp.</p>
        </div>

        {!mandatoryMaterials.length ? (
          <ClassroomEmptyState
            description="Chương trình của lớp chưa được cấu hình học liệu bắt buộc. Vui lòng liên hệ Content Manager; giáo viên không cần chọn lại từ kho trung tâm."
            title="Chương trình chưa có học liệu"
          />
        ) : (
          <div className="space-y-4">
            <div className="divide-y divide-[#e9d7d6]/40 rounded-2xl border border-[#e9d7d6]/65 bg-white shadow-sm overflow-hidden">
              {paginatedMandatory.map((material) => (
                <MaterialRow
                  key={material.id}
                  material={material}
                  mandatory
                />
              ))}
            </div>
            
            {mandTotalPages > 1 && (
              <div className="flex justify-end mt-2">
                <Pagination
                  page={mandPage}
                  onChange={setMandPage}
                  totalPages={mandTotalPages}
                  totalItems={mandTotalItems}
                  pageSize={4}
                />
              </div>
            )}
          </div>
        )}
      </section>

      <section className="space-y-4">
        <div>
          <h5 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Tài liệu bổ trợ của giáo viên</h5>
          <p className="mt-1 text-sm text-[#584140]">Worksheet, ghi chú hoặc liên kết chỉ phát sinh cho lớp học này.</p>
        </div>

        {!supplementaryMaterials.length ? (
          <ClassroomEmptyState
            actionLabel="Thêm tài liệu bổ trợ"
            description="Lớp chưa có tài liệu bổ trợ. Học liệu bắt buộc phía trên vẫn được giữ nguyên."
            onAction={openCreateForm}
            title="Chưa có tài liệu bổ trợ"
          />
        ) : (
          <div className="space-y-4">
            <div className="divide-y divide-[#e9d7d6]/40 rounded-2xl border border-[#e9d7d6]/65 bg-white shadow-sm overflow-hidden">
              {paginatedSupplementary.map((material) => (
                <MaterialRow
                  deleting={deletingId === material.id}
                  key={material.id}
                  material={material}
                  onDelete={() => handleDeleteMaterial(material)}
                />
              ))}
            </div>

            {suppTotalPages > 1 && (
              <div className="flex justify-end mt-2">
                <Pagination
                  page={suppPage}
                  onChange={setSuppPage}
                  totalPages={suppTotalPages}
                  totalItems={suppTotalItems}
                  pageSize={4}
                />
              </div>
            )}
          </div>
        )}
      </section>
    </div>
  );
}

function EditorModal({ children, onClose }) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-hidden px-3 py-4 sm:px-6" role="dialog" aria-modal="true">
      <button
        aria-label="Đóng modal"
        className="absolute -inset-10 bg-[#1a0004]/45 backdrop-blur-sm"
        onClick={onClose}
        type="button"
      />
      <div className="relative z-10 w-full max-w-[640px] pointer-events-auto bg-[#fafafa] rounded-3xl border border-[#dcc0bf]/35 p-6 shadow-2xl overflow-y-auto max-h-[90vh]">
        {children}
      </div>
    </div>
  );
}

function SupplementaryMaterialForm({
  attachmentFile,
  form,
  onAttachmentChange,
  onCancel,
  onChange,
  onSave,
  saving,
  sessionOptions,
}) {
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3 border-b border-gray-100 pb-3">
        <div>
          <h5 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Tài liệu bổ trợ mới</h5>
          <p className="mt-1 text-xs text-[#584140]">Tài liệu này chỉ áp dụng cho lớp hiện tại, không thay đổi chương trình gốc.</p>
        </div>
        <button className="rounded-lg p-2 text-[#584140] hover:bg-gray-100" onClick={onCancel} type="button">
          <X className="h-4 w-4" />
        </button>
      </div>

      <div className="grid gap-4">
        <TextInput label="Tiêu đề *" onChange={(value) => onChange('title', value)} placeholder="Ví dụ: Worksheet ôn tập buổi 3" value={form.title} />
        <div className="space-y-2">
          <span className="block text-xs font-extrabold uppercase tracking-[0.16em] text-[#584140]">Tệp đính kèm</span>
          <div className="flex flex-wrap items-center gap-3">
            <label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-dashed border-[#dfbfbd] bg-[#fffafb] px-4 py-3 text-xs font-extrabold text-[#730014] hover:border-[#730014]">
              <Paperclip className="h-4 w-4" />
              Chọn tệp
              <input
                accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.zip,.rar,.jpg,.jpeg,.png"
                className="hidden"
                onChange={(event) => onAttachmentChange(event.target.files?.[0] || null)}
                type="file"
              />
            </label>
            <span className="text-xs text-[#8a7a78]">{attachmentFile?.name || 'PDF, Word, PowerPoint, Excel hoặc ảnh; tối đa 20 MB'}</span>
          </div>
        </div>
        <TextInput disabled={Boolean(attachmentFile)} label="Hoặc dán liên kết" onChange={(value) => onChange('fileUrl', value)} placeholder="https://..." value={form.fileUrl} />
        <TextArea label="Mô tả ngắn" onChange={(value) => onChange('description', value)} placeholder="Mục đích sử dụng của tài liệu." value={form.description} />
        <FilterSelect label="Gắn với buổi học" onChange={(value) => onChange('sessionId', value)} options={sessionOptions} value={form.sessionId} />
      </div>

      <div className="mt-5 flex flex-wrap justify-end gap-3 border-t border-gray-100 pt-3">
        <button className="rounded-xl border border-gray-200 px-5 py-3 text-xs font-extrabold text-[#584140] hover:bg-gray-50 active:scale-95" onClick={onCancel} type="button">Hủy</button>
        <button className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white hover:bg-[#730014] disabled:opacity-60 active:scale-95" disabled={saving} onClick={onSave} type="button">
          <Upload className="h-4 w-4" />
          {saving ? 'Đang lưu...' : 'Lưu tài liệu bổ trợ'}
        </button>
      </div>
    </div>
  );
}

function MaterialRow({ material, mandatory = false, deleting = false, onDelete }) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 hover:bg-[#fffcfc] transition">
      <div className="flex items-start gap-4 min-w-0 flex-1">
        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#fff2f3] text-[#730014]">
          <FileText className="h-5 w-5" />
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h6 className="font-['Manrope'] text-sm font-extrabold text-[#2b2828] truncate max-w-sm sm:max-w-md">{material.title}</h6>
            {mandatory ? (
              <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-[10px] font-bold text-emerald-700 border border-emerald-200 shrink-0">
                <ShieldCheck className="h-3 w-3" /> Bắt buộc
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 rounded-full bg-[#fff2f3] px-2.5 py-0.5 text-[10px] font-bold text-[#730014] border border-[#f0d8db] shrink-0">
                Bổ trợ
              </span>
            )}
            {material.fileType && (
              <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-bold text-slate-600 uppercase shrink-0">
                {material.fileType}
              </span>
            )}
          </div>
          <p className="mt-1 text-xs text-[#584140] line-clamp-2">{material.description || 'Tài liệu đã sẵn sàng cho học viên trong lớp.'}</p>
          
          <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-[#8a7a78]">
            <span className="flex items-center gap-1">
              <span className="font-semibold text-[#8b706e]">Gắn với:</span>
              <span>{material.curriculumUnitTitle || material.sessionTitle || 'Áp dụng chung cho lớp'}</span>
            </span>
            <span className="flex items-center gap-1">
              <span className="font-semibold text-[#8b706e]">Nguồn:</span>
              <span>{material.provider || (mandatory ? 'EnglishLab' : 'Giáo viên phụ trách')}</span>
            </span>
          </div>
        </div>
      </div>
      
      <div className="flex items-center justify-end gap-2 shrink-0 self-end sm:self-center">
        {material.fileUrl && (
          <a
            className="inline-flex items-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-white px-3.5 py-2 text-xs font-bold text-[#730014] transition hover:bg-[#fff2f3] active:scale-95"
            href={material.fileUrl}
            rel="noreferrer"
            target="_blank"
          >
            <Download className="h-3.5 w-3.5" /> Mở tài liệu
          </a>
        )}
        {!mandatory && (
          <button
            className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-transparent text-[#8a7a78] hover:bg-red-50 hover:text-red-600 transition disabled:opacity-50 active:scale-95"
            disabled={deleting}
            onClick={onDelete}
            title="Xóa tài liệu bổ trợ"
            type="button"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        )}
      </div>
    </div>
  );
}

function FilterSelect({ label, value, options, onChange }) {
  return (
    <div className="space-y-2">
      <label className="block text-xs font-extrabold uppercase tracking-[0.16em] text-[#584140]">{label}</label>
      <BrandedSelect onChange={(event) => onChange(event.target.value)} options={options} value={value} />
    </div>
  );
}

function TextInput({ label, value, onChange, placeholder, className = '', disabled = false }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1.5 block text-xs font-extrabold text-[#584140]">{label}</span>
      <input className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-[#2b2828] outline-none focus:border-[#730014] disabled:bg-gray-50" disabled={disabled} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} value={value} />
    </label>
  );
}

function TextArea({ label, value, onChange, placeholder, className = '' }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1.5 block text-xs font-extrabold text-[#584140]">{label}</span>
      <textarea className="min-h-[120px] w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-[#2b2828] outline-none focus:border-[#730014]" onChange={(event) => onChange(event.target.value)} placeholder={placeholder} value={value} />
    </label>
  );
}

function StatPill({ label, value }) {
  return (
    <div className="rounded-2xl border border-[#f0e3e4] bg-white px-4 py-3">
      <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-[#8b706e]">{label}</p>
      <p className="mt-2 text-lg font-extrabold text-[#2b2828]">{value}</p>
    </div>
  );
}

function formatSessionSummary(session) {
  const date = session?.sessionDate ? formatClassroomDate(session.sessionDate) : '';
  const time = session?.startTime ? formatClassroomTime(session.startTime) : '';
  if (date && time) return `${date} · ${time}`;
  if (date) return date;
  if (time) return time;
  return formatClassroomDateTime(session?.updatedAt);
}
