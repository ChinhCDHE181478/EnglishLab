import { useEffect, useMemo, useState } from 'react';
import {
  BookCopy,
  Download,
  FileText,
  Paperclip,
  Plus,
  RefreshCw,
  Trash2,
  Upload,
  X,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { ClassroomEmptyState } from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDate, formatClassroomDateTime, formatClassroomTime } from '../../utils/classroomHelpers';

const PAGE_SIZE = 6;

const uploadFormInitial = {
  title: '',
  fileUrl: '',
  description: '',
  sessionId: '',
};

const libraryFilterInitial = {
  examCategory: 'ALL',
  materialType: 'ALL',
  skill: 'ALL',
};

const attachFormInitial = {
  sessionId: '',
};

const inferFileType = (fileNameOrUrl) => {
  const value = fileNameOrUrl || '';
  const match = value.match(/\.([a-z0-9]+)(?:\?|$)/i);
  return match ? match[1].toUpperCase() : '';
};

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
  const [activeMode, setActiveMode] = useState('library');
  const [libraryItems, setLibraryItems] = useState([]);
  const [libraryLoading, setLibraryLoading] = useState(false);
  const [libraryLoaded, setLibraryLoaded] = useState(false);
  const [libraryFilters, setLibraryFilters] = useState(libraryFilterInitial);
  const [libraryPage, setLibraryPage] = useState(1);
  const [attachForm, setAttachForm] = useState(attachFormInitial);
  const [attachingId, setAttachingId] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [uploadForm, setUploadForm] = useState(uploadFormInitial);
  const [attachmentFile, setAttachmentFile] = useState(null);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const sessionOptions = useMemo(() => sessionOptionsFor(sessions), [sessions]);

  const loadLibrary = async () => {
    setLibraryLoading(true);
    try {
      const data = await classroomApi.getTeacherMaterialLibrary();
      setLibraryItems(data);
      setLibraryLoaded(true);
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể tải kho học liệu trung tâm.'));
    } finally {
      setLibraryLoading(false);
    }
  };

  useEffect(() => {
    if (activeMode === 'library' && !libraryLoaded) {
      loadLibrary();
    }
  }, [activeMode, libraryLoaded]);

  useEffect(() => {
    setLibraryPage(1);
  }, [libraryFilters]);

  const filteredLibrary = useMemo(() => libraryItems.filter((item) => (
    (libraryFilters.examCategory === 'ALL' || (item.examCategory || 'GENERAL') === libraryFilters.examCategory)
    && (libraryFilters.materialType === 'ALL' || (item.materialType || 'LINK') === libraryFilters.materialType)
    && (libraryFilters.skill === 'ALL' || (item.skill || 'Mixed') === libraryFilters.skill)
  )), [libraryFilters, libraryItems]);

  const totalLibraryPages = Math.max(1, Math.ceil(filteredLibrary.length / PAGE_SIZE));
  const libraryPageItems = useMemo(() => {
    const start = (libraryPage - 1) * PAGE_SIZE;
    return filteredLibrary.slice(start, start + PAGE_SIZE);
  }, [filteredLibrary, libraryPage]);

  useEffect(() => {
    if (libraryPage > totalLibraryPages) {
      setLibraryPage(totalLibraryPages);
    }
  }, [libraryPage, totalLibraryPages]);

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

  const refreshMaterials = async () => {
    const refreshed = await classroomApi.getTeacherMaterials(classroomId);
    onMaterialsChange?.(refreshed);
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
        provider: 'Teacher upload',
        visibility: 'LEARNERS_IN_CLASS',
        sourceType: 'CLASSROOM_UPLOAD',
        sessionId: uploadForm.sessionId ? Number(uploadForm.sessionId) : null,
      });

      await refreshMaterials();
      onMessage?.('Đã thêm tài liệu riêng cho lớp.');
      resetForm();
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể đăng tài liệu cho lớp.'));
    } finally {
      setSaving(false);
    }
  };

  const handleAttachLibraryItem = async (item) => {
    setAttachingId(item.id);
    onMessage?.('');
    try {
      await classroomApi.attachTeacherLibraryMaterial(classroomId, {
        centerMaterialId: item.id,
        sessionId: attachForm.sessionId ? Number(attachForm.sessionId) : null,
      });
      await refreshMaterials();
      onMessage?.('Đã gắn học liệu trung tâm vào lớp.');
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể gắn học liệu trung tâm vào lớp.'));
    } finally {
      setAttachingId(null);
    }
  };

  const handleDeleteMaterial = async (material) => {
    if (!window.confirm(`Xóa tài liệu "${material.title}" khỏi lớp này?`)) {
      return;
    }

    setDeletingId(material.id);
    onMessage?.('');
    try {
      await classroomApi.deleteTeacherMaterial(material.id);
      await refreshMaterials();
      onMessage?.('Đã xóa tài liệu khỏi lớp.');
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể xóa tài liệu.'));
    } finally {
      setDeletingId(null);
    }
  };

  const materialStats = useMemo(() => ({
    center: materials.filter((item) => item.sourceType === 'CENTER_LIBRARY').length,
    custom: materials.filter((item) => item.sourceType !== 'CENTER_LIBRARY').length,
  }), [materials]);

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-[#dfbfbd]/20 bg-[#fffafb] p-5">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h4 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Tài liệu lớp học</h4>
            <p className="mt-1 text-sm leading-6 text-[#584140]">
              Chọn từ kho học liệu trung tâm để tái sử dụng, hoặc tải thêm tài liệu riêng cho lớp khi cần.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <button
              className={`rounded-xl px-4 py-2.5 text-sm font-extrabold transition ${
                activeMode === 'library'
                  ? 'bg-[#4b0009] text-white'
                  : 'border border-[#dfbfbd]/70 bg-white text-[#730014] hover:bg-[#fff2f3]'
              }`}
              onClick={() => setActiveMode('library')}
              type="button"
            >
              Chọn từ trung tâm
            </button>
            <button
              className={`rounded-xl px-4 py-2.5 text-sm font-extrabold transition ${
                activeMode === 'custom'
                  ? 'bg-[#4b0009] text-white'
                  : 'border border-[#dfbfbd]/70 bg-white text-[#730014] hover:bg-[#fff2f3]'
              }`}
              onClick={() => setActiveMode('custom')}
              type="button"
            >
              Tải riêng cho lớp
            </button>
          </div>
        </div>

        <div className="mt-4 grid gap-3 md:grid-cols-3">
          <StatPill label="Đã gắn từ trung tâm" value={materialStats.center} />
          <StatPill label="Tài liệu riêng của lớp" value={materialStats.custom} />
          <StatPill label="Tổng tài liệu đang có" value={materials.length} />
        </div>
      </div>

      {activeMode === 'library' ? (
        <div className="space-y-5 rounded-2xl border border-[#dfbfbd]/20 bg-white p-5 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h5 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Kho học liệu trung tâm</h5>
              <p className="mt-1 text-sm text-[#584140]">
                Học liệu ở đây do Content Manager chuẩn hóa theo band IELTS hoặc dải điểm TOEIC để dùng lại giữa nhiều lớp.
              </p>
            </div>
            <button
              className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd]/70 bg-white px-4 py-2.5 text-sm font-extrabold text-[#730014] transition hover:bg-[#fff2f3]"
              onClick={loadLibrary}
              type="button"
            >
              <RefreshCw className={`h-4 w-4 ${libraryLoading ? 'animate-spin' : ''}`} />
              Làm mới kho
            </button>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <FilterSelect
              label="Kỳ thi"
              value={libraryFilters.examCategory}
              options={[{ label: 'Tất cả', value: 'ALL' }, { label: 'IELTS', value: 'IELTS' }, { label: 'TOEIC', value: 'TOEIC' }, { label: 'Tổng quát', value: 'GENERAL' }]}
              onChange={(value) => setLibraryFilters((current) => ({ ...current, examCategory: value }))}
            />
            <FilterSelect
              label="Loại học liệu"
              value={libraryFilters.materialType}
              options={[{ label: 'Tất cả', value: 'ALL' }, ...buildDistinctOptions(libraryItems, 'materialType')]}
              onChange={(value) => setLibraryFilters((current) => ({ ...current, materialType: value }))}
            />
            <FilterSelect
              label="Kỹ năng"
              value={libraryFilters.skill}
              options={[{ label: 'Tất cả', value: 'ALL' }, ...buildDistinctOptions(libraryItems, 'skill')]}
              onChange={(value) => setLibraryFilters((current) => ({ ...current, skill: value }))}
            />
            <FilterSelect
              label="Gắn vào buổi học"
              value={attachForm.sessionId}
              options={sessionOptions}
              onChange={(value) => setAttachForm({ sessionId: value })}
            />
          </div>

          {!filteredLibrary.length ? (
            <ClassroomEmptyState
              title="Chưa có học liệu phù hợp"
              description="Hãy nới bộ lọc hoặc nhờ Content Manager bổ sung học liệu trung tâm cho band và kỹ năng này."
            />
          ) : (
            <div className="space-y-4">
              <div className="grid gap-4 xl:grid-cols-2">
                {libraryPageItems.map((item) => (
                  <article key={item.id} className="rounded-2xl border border-gray-100 bg-[#fcfbfb] p-5 transition hover:border-[#dfbfbd]/40">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-rose-50 text-[#730014]">
                            <BookCopy className="h-5 w-5" />
                          </div>
                          <div>
                            <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">{item.title}</h4>
                            <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
                              {item.materialType || 'LINK'} • {item.skill || 'Mixed'}
                            </p>
                          </div>
                        </div>
                        <p className="mt-3 text-sm leading-6 text-[#584140]">
                          {item.description || 'Học liệu này đã sẵn sàng để gắn vào lớp.'}
                        </p>
                      </div>

                      <button
                        className="rounded-xl bg-[#4b0009] px-4 py-2.5 text-sm font-extrabold text-white transition hover:bg-[#730014] disabled:opacity-60"
                        disabled={attachingId === item.id}
                        onClick={() => handleAttachLibraryItem(item)}
                        type="button"
                      >
                        {attachingId === item.id ? 'Đang gắn...' : 'Gắn vào lớp'}
                      </button>
                    </div>

                    <div className="mt-4 grid gap-3 md:grid-cols-2">
                      <InfoPill label="Mức phù hợp" value={formatTargetRange(item)} />
                      <InfoPill label="Nguồn" value={item.provider || 'EnglishLab'} />
                    </div>

                    {item.fileUrl ? (
                      <div className="mt-4 border-t border-gray-100 pt-4">
                        <a
                          className="inline-flex items-center gap-2 text-sm font-extrabold text-[#730014] hover:underline"
                          href={item.fileUrl}
                          rel="noreferrer"
                          target="_blank"
                        >
                          <Download className="h-4 w-4" />
                          Xem học liệu gốc
                        </a>
                      </div>
                    ) : null}
                  </article>
                ))}
              </div>

              <div className="flex items-center justify-between">
                <span className="text-sm text-[#8b706e]">
                  Trang {libraryPage}/{totalLibraryPages}
                </span>
                <div className="flex gap-2">
                  <button
                    className="rounded-xl border border-[#dfbfbd]/70 px-4 py-2 text-sm font-bold text-[#584140] transition hover:bg-[#fff2f3] disabled:opacity-45"
                    disabled={libraryPage === 1}
                    onClick={() => setLibraryPage((page) => Math.max(1, page - 1))}
                    type="button"
                  >
                    Trang trước
                  </button>
                  <button
                    className="rounded-xl border border-[#dfbfbd]/70 px-4 py-2 text-sm font-bold text-[#584140] transition hover:bg-[#fff2f3] disabled:opacity-45"
                    disabled={libraryPage === totalLibraryPages}
                    onClick={() => setLibraryPage((page) => Math.min(totalLibraryPages, page + 1))}
                    type="button"
                  >
                    Trang sau
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="space-y-5 rounded-2xl border border-[#dfbfbd]/20 bg-white p-5 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h5 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Tài liệu riêng của lớp</h5>
              <p className="mt-1 text-sm text-[#584140]">
                Dùng cho worksheet, ghi chú phát sinh hoặc tài liệu chỉ áp dụng cho lớp này.
              </p>
            </div>
            <button
              className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-4 py-2.5 text-sm font-extrabold text-white transition hover:bg-[#730014]"
              onClick={openCreateForm}
              type="button"
            >
              <Plus className="h-4 w-4" />
              Thêm tài liệu riêng
            </button>
          </div>

          {formOpen ? (
            <div className="rounded-2xl border border-[#dfbfbd]/25 bg-[#fcfbfb] p-5">
              <div className="flex items-center justify-between gap-3">
                <h6 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Biểu mẫu tài liệu mới</h6>
                <button
                  className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-[#584140] transition hover:bg-gray-100"
                  onClick={resetForm}
                  type="button"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <div className="mt-4 grid gap-4 md:grid-cols-2">
                <TextInput
                  label="Tiêu đề *"
                  value={uploadForm.title}
                  onChange={(value) => setUploadForm((current) => ({ ...current, title: value }))}
                  placeholder="Ví dụ: Worksheet luyện nghe tuần 3"
                  className="md:col-span-2"
                />

                <div className="space-y-2 md:col-span-2">
                  <label className="block text-xs font-extrabold uppercase tracking-[0.16em] text-[#584140]">Tệp đính kèm</label>
                  <div className="flex flex-wrap items-center gap-3">
                    <label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-dashed border-[#dfbfbd] bg-[#fffafb] px-4 py-3 text-xs font-extrabold text-[#730014] transition hover:border-[#730014]">
                      <Paperclip className="h-4 w-4" />
                      Chọn tệp
                      <input
                        accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.zip,.rar,.jpg,.jpeg,.png"
                        className="hidden"
                        onChange={(event) => {
                          const file = event.target.files?.[0] || null;
                          setAttachmentFile(file);
                          if (file) {
                            setUploadForm((current) => ({ ...current, fileUrl: '' }));
                          }
                        }}
                        type="file"
                      />
                    </label>
                    {attachmentFile ? (
                      <span className="text-xs font-bold text-[#584140]">{attachmentFile.name}</span>
                    ) : (
                      <span className="text-xs text-[#8a7a78]">PDF, Word, PowerPoint, Excel, ảnh... tối đa 20 MB</span>
                    )}
                  </div>
                </div>

                <TextInput
                  label="Hoặc dán liên kết"
                  value={uploadForm.fileUrl}
                  onChange={(value) => setUploadForm((current) => ({ ...current, fileUrl: value }))}
                  placeholder="https://..."
                  className="md:col-span-2"
                  disabled={Boolean(attachmentFile)}
                />

                <TextArea
                  label="Mô tả ngắn"
                  value={uploadForm.description}
                  onChange={(value) => setUploadForm((current) => ({ ...current, description: value }))}
                  placeholder="Ghi chú nhanh để học viên biết tài liệu này dùng cho mục gì."
                  className="md:col-span-2"
                />

                <FilterSelect
                  label="Gắn với buổi học"
                  value={uploadForm.sessionId}
                  options={sessionOptions}
                  onChange={(value) => setUploadForm((current) => ({ ...current, sessionId: value }))}
                />
              </div>

              <div className="mt-5 flex flex-wrap justify-end gap-3">
                <button
                  className="rounded-xl border border-gray-200 px-5 py-3 text-xs font-extrabold text-[#584140] transition hover:bg-gray-50"
                  onClick={resetForm}
                  type="button"
                >
                  Hủy
                </button>
                <button
                  className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white transition hover:bg-[#730014] disabled:opacity-60"
                  disabled={saving}
                  onClick={handleSaveMaterial}
                  type="button"
                >
                  <Upload className="h-4 w-4" />
                  {saving ? 'Đang lưu...' : 'Lưu tài liệu riêng'}
                </button>
              </div>
            </div>
          ) : null}
        </div>
      )}

      {!materials.length ? (
        <ClassroomEmptyState
          actionLabel={activeMode === 'library' ? 'Mở kho trung tâm' : 'Tạo tài liệu đầu tiên'}
          description="Lớp học này chưa có tài liệu nào. Bạn có thể chọn học liệu từ trung tâm hoặc thêm tài liệu riêng cho lớp."
          onAction={activeMode === 'library' ? () => setActiveMode('library') : openCreateForm}
          title="Chưa có tài liệu"
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {materials.map((material) => (
            <article
              key={material.id}
              className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm transition hover:border-[#dfbfbd]/30"
            >
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

              <div className="mt-4 space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                  <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">{material.title}</h4>
                  <SourceBadge value={material.sourceType} />
                </div>
                <p className="text-sm leading-6 text-[#584140]">
                  {material.description || 'Tài liệu này đã sẵn sàng cho học viên trong lớp.'}
                </p>
              </div>

              <div className="mt-4 grid gap-3">
                <InfoPill label="Buổi học gắn kèm" value={material.sessionTitle || 'Không gắn buổi cụ thể'} />
                <InfoPill label="Nguồn" value={material.provider || (material.sourceType === 'CENTER_LIBRARY' ? 'EnglishLab' : 'Teacher upload')} />
              </div>

              {material.fileUrl ? (
                <div className="mt-4 border-t border-gray-50 pt-4">
                  <a
                    className="inline-flex items-center gap-1.5 text-sm font-extrabold text-[#730014] hover:underline"
                    href={material.fileUrl}
                    rel="noreferrer"
                    target="_blank"
                  >
                    <Download className="h-4 w-4" />
                    Mở tài liệu
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

function FilterSelect({ label, value, options, onChange }) {
  return (
    <div className="space-y-2">
      <label className="block text-xs font-extrabold uppercase tracking-[0.16em] text-[#584140]">{label}</label>
      <BrandedSelect value={value} onChange={(event) => onChange(event.target.value)} options={options} />
    </div>
  );
}

function TextInput({ label, value, onChange, placeholder, className = '', disabled = false }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1.5 block text-xs font-extrabold text-[#584140]">{label}</span>
      <input
        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-[#2b2828] outline-none focus:border-[#730014] disabled:bg-gray-50"
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        value={value}
      />
    </label>
  );
}

function TextArea({ label, value, onChange, placeholder, className = '' }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1.5 block text-xs font-extrabold text-[#584140]">{label}</span>
      <textarea
        className="min-h-[120px] w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-[#2b2828] outline-none focus:border-[#730014]"
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        value={value}
      />
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

function InfoPill({ label, value }) {
  return (
    <div className="rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] px-4 py-3">
      <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-[#8b706e]">{label}</p>
      <p className="mt-2 text-sm font-semibold text-[#2b2828]">{value}</p>
    </div>
  );
}

function SourceBadge({ value }) {
  const isCenter = value === 'CENTER_LIBRARY';
  return (
    <span className={`inline-flex rounded-full px-3 py-1 text-xs font-bold ${
      isCenter ? 'bg-emerald-100 text-emerald-700' : 'bg-[#fff2f3] text-[#730014]'
    }`}>
      {isCenter ? 'Từ trung tâm' : 'Riêng của lớp'}
    </span>
  );
}

function formatTargetRange(item) {
  if ((item.examCategory || 'GENERAL') === 'TOEIC') {
    if (item.toeicScoreMin != null || item.toeicScoreMax != null) {
      return `${item.toeicScoreMin ?? '?'} - ${item.toeicScoreMax ?? '?'}`;
    }
    return 'TOEIC chung';
  }
  if (item.ieltsBandMin != null || item.ieltsBandMax != null) {
    return `Band ${item.ieltsBandMin ?? '?'} - ${item.ieltsBandMax ?? '?'}`;
  }
  return item.examCategory || 'Tổng quát';
}

function buildDistinctOptions(items, key) {
  const values = Array.from(new Set(items.map((item) => item[key]).filter(Boolean)));
  return values.map((value) => ({ label: value, value }));
}

function formatSessionSummary(session) {
  const date = session?.sessionDate ? formatClassroomDate(session.sessionDate) : '';
  const time = session?.startTime ? formatClassroomTime(session.startTime) : '';
  if (date && time) return `${date} · ${time}`;
  if (date) return date;
  if (time) return time;
  return formatClassroomDateTime(session?.updatedAt);
}
