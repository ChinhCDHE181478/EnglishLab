import { useMemo, useState } from 'react';
import { Download, FileText, Paperclip, Plus, Trash2, X } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { ClassroomEmptyState } from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDateTime } from '../../utils/classroomHelpers';

const emptyForm = {
  title: '',
  fileUrl: '',
  sessionId: '',
};

const inferFileType = (fileNameOrUrl) => {
  const value = fileNameOrUrl || '';
  const match = value.match(/\.([a-z0-9]+)(?:\?|$)/i);
  return match ? match[1].toLowerCase() : '';
};

export default function TeacherMaterialsSection({
  classroomId,
  materials,
  sessions,
  onMaterialsChange,
  onMessage,
}) {
  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [attachmentFile, setAttachmentFile] = useState(null);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const sessionOptions = useMemo(
    () => [
      { label: 'Không gắn buổi học cụ thể', value: '' },
      ...(sessions || []).map((session) => ({
        label: `${session.title || `Buổi #${session.id}`} · ${formatClassroomDateTime(session.startTime)}`,
        value: String(session.id),
      })),
    ],
    [sessions],
  );

  const resetForm = () => {
    setForm(emptyForm);
    setAttachmentFile(null);
    setFormOpen(false);
  };

  const openCreateForm = () => {
    setForm(emptyForm);
    setAttachmentFile(null);
    setFormOpen(true);
  };

  const handleSaveMaterial = async () => {
    if (!form.title.trim()) {
      onMessage?.('Vui lòng nhập tiêu đề tài liệu.');
      return;
    }
    if (!attachmentFile && !form.fileUrl.trim()) {
      onMessage?.('Vui lòng tải lên tệp hoặc dán liên kết tài liệu.');
      return;
    }

    setSaving(true);
    onMessage?.('');
    try {
      let fileUrl = form.fileUrl.trim();
      let fileType = inferFileType(fileUrl);
      if (attachmentFile) {
        const uploaded = await classroomApi.uploadHomeworkAttachment(attachmentFile);
        fileUrl = uploaded.url;
        fileType = inferFileType(uploaded.originalFileName || uploaded.fileName || fileUrl);
      }

      await classroomApi.createTeacherMaterial(classroomId, {
        title: form.title.trim(),
        fileUrl,
        fileType: fileType || null,
        visibility: 'LEARNERS_IN_CLASS',
        sessionId: form.sessionId ? Number(form.sessionId) : null,
      });

      const refreshed = await classroomApi.getTeacherMaterials(classroomId);
      onMaterialsChange?.(refreshed);
      onMessage?.('Đã đăng tài liệu mới.');
      resetForm();
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể đăng tài liệu.'));
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteMaterial = async (material) => {
    if (!window.confirm(`Xóa tài liệu "${material.title}"? Hành động này không thể hoàn tác.`)) {
      return;
    }

    setDeletingId(material.id);
    onMessage?.('');
    try {
      await classroomApi.deleteTeacherMaterial(material.id);
      const refreshed = await classroomApi.getTeacherMaterials(classroomId);
      onMaterialsChange?.(refreshed);
      onMessage?.('Đã xóa tài liệu.');
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể xóa tài liệu.'));
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-[#dfbfbd]/20 bg-[#fffafb] p-5">
        <div>
          <h4 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Tài liệu lớp học</h4>
          <p className="mt-1 text-xs leading-5 text-[#584140]">
            Đăng slide, PDF hoặc tài liệu tham khảo để học viên tải về trong tab Tài liệu.
          </p>
        </div>
        <button
          className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014] active:scale-95"
          onClick={openCreateForm}
          type="button"
        >
          <Plus className="h-4 w-4" />
          Đăng tài liệu
        </button>
      </div>

      {formOpen && (
        <div className="rounded-2xl border border-[#dfbfbd]/25 bg-white p-6 shadow-sm space-y-5">
          <div className="flex items-center justify-between gap-3">
            <h5 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Tài liệu mới</h5>
            <button
              className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-[#584140] transition hover:bg-gray-100"
              onClick={resetForm}
              type="button"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <label className="block md:col-span-2">
              <span className="mb-1.5 block text-xs font-extrabold text-[#584140]">Tiêu đề *</span>
              <input
                className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-[#2b2828] outline-none focus:border-[#730014]"
                onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
                placeholder="Ví dụ: Slide buổi 1 - Speaking Part 2"
                value={form.title}
              />
            </label>

            <label className="block md:col-span-2">
              <span className="mb-1.5 block text-xs font-extrabold text-[#584140]">Tệp đính kèm</span>
              <div className="flex flex-wrap items-center gap-3">
                <label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-dashed border-[#dfbfbd] bg-[#fffafb] px-4 py-3 text-xs font-extrabold text-[#730014] transition hover:border-[#730014]">
                  <Paperclip className="h-4 w-4" />
                  Chọn tệp
                  <input
                    accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.zip,.rar,.jpg,.jpeg,.png"
                    className="hidden"
                    onChange={(event) => {
                      setAttachmentFile(event.target.files?.[0] || null);
                      if (event.target.files?.[0]) {
                        setForm((current) => ({ ...current, fileUrl: '' }));
                      }
                    }}
                    type="file"
                  />
                </label>
                {attachmentFile ? (
                  <span className="text-xs font-bold text-[#584140]">{attachmentFile.name}</span>
                ) : (
                  <span className="text-xs text-[#8a7a78]">PDF, Word, PowerPoint, Excel, ảnh… tối đa 20 MB</span>
                )}
              </div>
            </label>

            <label className="block md:col-span-2">
              <span className="mb-1.5 block text-xs font-extrabold text-[#584140]">Hoặc dán liên kết</span>
              <input
                className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-[#2b2828] outline-none focus:border-[#730014] disabled:bg-gray-50"
                disabled={Boolean(attachmentFile)}
                onChange={(event) => setForm((current) => ({ ...current, fileUrl: event.target.value }))}
                placeholder="https://..."
                value={form.fileUrl}
              />
            </label>

            <label className="block md:col-span-2">
              <span className="mb-1.5 block text-xs font-extrabold text-[#584140]">Gắn với buổi học (tuỳ chọn)</span>
              <BrandedSelect
                onChange={(value) => setForm((current) => ({ ...current, sessionId: value }))}
                options={sessionOptions}
                value={form.sessionId}
              />
            </label>
          </div>

          <div className="flex flex-wrap justify-end gap-3 pt-2">
            <button
              className="rounded-xl border border-gray-200 px-5 py-3 text-xs font-extrabold text-[#584140] transition hover:bg-gray-50"
              onClick={resetForm}
              type="button"
            >
              Hủy
            </button>
            <button
              className="rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white transition hover:bg-[#730014] disabled:opacity-60"
              disabled={saving}
              onClick={handleSaveMaterial}
              type="button"
            >
              {saving ? 'Đang lưu…' : 'Đăng tài liệu'}
            </button>
          </div>
        </div>
      )}

      {!materials.length ? (
        <ClassroomEmptyState
          actionLabel="Đăng tài liệu đầu tiên"
          description="Chưa có tài liệu nào được tải lên cho lớp học này. Hãy đăng slide, PDF hoặc liên kết tham khảo."
          onAction={openCreateForm}
          title="Chưa có tài liệu"
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {materials.map((material) => (
            <article
              key={material.id}
              className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm flex flex-col justify-between hover:border-[#dfbfbd]/30 transition"
            >
              <div>
                <div className="flex items-start justify-between gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-rose-50 text-[#730014]">
                    <FileText className="h-5 w-5" />
                  </div>
                  <button
                    className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-[#8a7a78] transition hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
                    disabled={deletingId === material.id}
                    onClick={() => handleDeleteMaterial(material)}
                    title="Xóa tài liệu"
                    type="button"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
                <h4 className="mt-4 font-['Manrope'] text-base font-extrabold text-[#2b2828]">
                  {material.title}
                </h4>
                {material.fileType ? (
                  <p className="mt-1 text-xs font-bold uppercase tracking-wide text-[#8a7a78]">
                    {material.fileType}
                  </p>
                ) : null}
              </div>

              {material.fileUrl ? (
                <div className="mt-4 pt-4 border-t border-gray-50">
                  <a
                    className="inline-flex items-center gap-1.5 text-xs font-extrabold text-[#730014] hover:underline"
                    href={material.fileUrl}
                    rel="noreferrer"
                    target="_blank"
                  >
                    <Download className="h-4 w-4" />
                    Tải tài liệu
                  </a>
                </div>
              ) : null}
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
