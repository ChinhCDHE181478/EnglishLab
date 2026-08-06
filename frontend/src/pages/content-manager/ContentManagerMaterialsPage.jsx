import { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { motion } from 'framer-motion';
import {
  Archive,
  BookOpen,
  CheckCircle2,
  Download,
  FilePlus2,
  FileStack,
  Globe,
  Link as LinkIcon,
  Minus,
  PencilLine,
  Plus,
  RefreshCw,
  Save,
  Search,
  Trash2,
  Upload,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { ContentManagerLoadingState, HeaderActions, Panel, SectionTitle, StatusBadge } from '../../components/content-manager/ContentManagerUi';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination from '../../components/ui/Pagination';
import { useAppDialog } from '../../components/ui/AppDialog';
import { ClassroomEmptyState, ClassroomErrorState } from '../../components/classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { stripRichTextToPlain } from '../../utils/lessonRichText';

const PAGE_SIZE = 8;

const emptyForm = {
  title: '',
  description: '',
  fileUrl: '',
  fileType: '',
  materialType: 'PDF',
  provider: 'EnglishLab',
  examCategory: 'IELTS',
  ieltsBandMin: '',
  ieltsBandMax: '',
  toeicScoreMin: '',
  toeicScoreMax: '',
  skill: 'Vocabulary',
  tags: '',
  status: 'DRAFT',
};

const materialTypeOptions = ['PDF', 'DOC', 'SLIDE', 'AUDIO', 'VIDEO', 'LINK', 'WORKSHEET'];
const examOptions = ['IELTS', 'TOEIC', 'GENERAL'];
const skillOptions = [
  { label: 'Từ vựng', value: 'Vocabulary' },
  { label: 'Nghe', value: 'Listening' },
  { label: 'Đọc', value: 'Reading' },
  { label: 'Viết', value: 'Writing' },
  { label: 'Nói', value: 'Speaking' },
  { label: 'Ngữ pháp', value: 'Grammar' },
  { label: 'Tổng hợp', value: 'Mixed' },
];
const statusOptions = ['PUBLISHED', 'DRAFT', 'ARCHIVED'];

const inferFileType = (value) => {
  const text = String(value || '');
  const match = text.match(/\.([a-z0-9]+)(?:\?|$)/i);
  return match ? match[1].toUpperCase() : '';
};

const guessProvider = (url) => {
  const text = String(url || '');
  if (!text) return 'EnglishLab';
  if (/drive\.google/i.test(text)) return 'Google Drive';
  if (/docs\.google/i.test(text)) return 'Google Docs';
  if (/youtube|youtu\.be/i.test(text)) return 'YouTube';
  if (/dropbox/i.test(text)) return 'Dropbox';
  return 'EnglishLab';
};

const toRequestPayload = (form) => ({
  title: form.title.trim(),
  description: form.description.trim(),
  fileUrl: form.fileUrl.trim(),
  fileType: form.fileType.trim() || inferFileType(form.fileUrl),
  materialType: form.materialType || null,
  provider: form.provider.trim() || guessProvider(form.fileUrl),
  examCategory: form.examCategory || null,
  ieltsBandMin: form.ieltsBandMin === '' ? null : Number(form.ieltsBandMin),
  ieltsBandMax: form.ieltsBandMax === '' ? null : Number(form.ieltsBandMax),
  toeicScoreMin: form.toeicScoreMin === '' ? null : Number(form.toeicScoreMin),
  toeicScoreMax: form.toeicScoreMax === '' ? null : Number(form.toeicScoreMax),
  skill: form.skill || null,
  tags: form.tags.trim() || null,
  status: form.status || 'DRAFT',
});

export default function ContentManagerMaterialsPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [composerOpen, setComposerOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [keyword, setKeyword] = useState('');
  const [filters, setFilters] = useState({
    examCategory: 'ALL',
    materialType: 'ALL',
    skill: 'ALL',
    status: 'ALL',
    provider: 'ALL',
  });
  const [currentPage, setCurrentPage] = useState(1);

  const loadItems = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getContentManagerMaterialLibrary();
      setItems(data);
    } catch (err) {
      setItems([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải kho học liệu trung tâm.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadItems();
  }, []);

  useEffect(() => {
    setCurrentPage(1);
  }, [filters, keyword]);

  const providerOptions = useMemo(() => {
    const values = Array.from(new Set(items.map((item) => item.provider).filter(Boolean)));
    return [{ label: 'Tất cả', value: 'ALL' }, ...values.map((value) => ({ label: value, value }))];
  }, [items]);

  const filteredItems = useMemo(
    () =>
      items.filter(
        (item) => {
          const normalizedKeyword = keyword.trim().toLowerCase();
          const haystack = [
            item.title,
            item.description,
            item.provider,
            item.skill,
            item.materialType,
            item.examCategory,
            item.tags,
          ].filter(Boolean).join(' ').toLowerCase();
          return (!normalizedKeyword || haystack.includes(normalizedKeyword)) &&
          (filters.examCategory === 'ALL' || (item.examCategory || 'GENERAL') === filters.examCategory) &&
          (filters.materialType === 'ALL' || (item.materialType || 'LINK') === filters.materialType) &&
          (filters.skill === 'ALL' || (item.skill || 'Mixed') === filters.skill) &&
          (filters.status === 'ALL' || (item.status || 'PUBLISHED') === filters.status) &&
          (filters.provider === 'ALL' || (item.provider || '') === filters.provider);
        },
      ),
    [filters, items, keyword],
  );

  const totalPages = Math.max(1, Math.ceil(filteredItems.length / PAGE_SIZE));
  const pageItems = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredItems.slice(start, start + PAGE_SIZE);
  }, [currentPage, filteredItems]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const resetForm = (open = false) => {
    setEditingId(null);
    setForm(emptyForm);
    setComposerOpen(open);
  };

  const openEdit = (item) => {
    setEditingId(item.id);
    setComposerOpen(true);
    setForm({
      title: item.title || '',
      description: item.description || '',
      fileUrl: item.fileUrl || '',
      fileType: item.fileType || '',
      materialType: item.materialType || 'PDF',
      provider: item.provider || 'EnglishLab',
      examCategory: item.examCategory || 'IELTS',
      ieltsBandMin: item.ieltsBandMin ?? '',
      ieltsBandMax: item.ieltsBandMax ?? '',
      toeicScoreMin: item.toeicScoreMin ?? '',
      toeicScoreMax: item.toeicScoreMax ?? '',
      skill: item.skill || 'Vocabulary',
      tags: item.tags || '',
      status: item.status || 'DRAFT',
    });
    setMessage('');
  };

  const handleUpload = async (file) => {
    if (!file) return;
    setUploading(true);
    setMessage('');
    try {
      const uploaded = await classroomApi.uploadContentManagerMaterialLibraryFile(file);
      const nextUrl = uploaded.url || '';
      setForm((current) => ({
        ...current,
        title: current.title || uploaded.originalFileName || uploaded.fileName || current.title,
        fileUrl: nextUrl,
        fileType: inferFileType(uploaded.originalFileName || uploaded.fileName || nextUrl) || current.fileType,
        provider: current.provider || guessProvider(nextUrl),
      }));
      setMessage('Đã tải tệp lên kho học liệu. Bạn có thể lưu ngay hoặc chỉnh thêm mô tả.');
      setComposerOpen(true);
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể tải tệp học liệu lên.'));
    } finally {
      setUploading(false);
    }
  };

  const handleSave = async () => {
    const validationMessage = validateMaterialForm(form);
    if (validationMessage) {
      setMessage(validationMessage);
      setComposerOpen(true);
      return;
    }

    setSaving(true);
    setMessage('');
    try {
      const payload = toRequestPayload(form);
      let saved;
      if (editingId) {
        saved = await classroomApi.updateContentManagerMaterialLibraryItem(editingId, payload);
        setMessage('Đã cập nhật học liệu trung tâm.');
      } else {
        saved = await classroomApi.createContentManagerMaterialLibraryItem(payload);
        setMessage('Đã thêm học liệu mới vào thư viện trung tâm.');
      }
      if (saved?.id) {
        setItems((current) => {
          const exists = current.some((item) => item.id === saved.id);
          return exists
            ? current.map((item) => (item.id === saved.id ? saved : item))
            : [saved, ...current];
        });
        setCurrentPage(1);
      }
      resetForm(false);
      await loadItems();
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể lưu học liệu trung tâm.'));
      setComposerOpen(true);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (item) => {
    if (!await confirmDialog(`Xóa học liệu “${item.title}” khỏi thư viện trung tâm?`, {
      title: 'Xóa học liệu',
      confirmLabel: 'Xóa học liệu',
      tone: 'danger',
    })) {
      return;
    }
    setMessage('');
    try {
      await classroomApi.deleteContentManagerMaterialLibraryItem(item.id);
      if (editingId === item.id) {
        resetForm(false);
      }
      setMessage('Đã xóa học liệu khỏi thư viện trung tâm.');
      await loadItems();
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể xóa học liệu trung tâm.'));
    }
  };

  const changeMaterialStatus = async (item, status) => {
    if (status === 'ARCHIVED' && !await confirmDialog(`Lưu trữ học liệu “${item.title}”?`, {
      title: 'Lưu trữ học liệu',
      confirmLabel: 'Lưu trữ',
      tone: 'danger',
    })) return;
    setMessage('');
    try {
      const saved = await classroomApi.updateContentManagerMaterialLibraryItem(item.id, toPayload({
        ...emptyForm,
        ...item,
        status,
      }));
      setItems((current) => current.map((row) => (String(row.id) === String(saved.id) ? saved : row)));
      if (String(editingId) === String(saved.id)) openEdit(saved);
      setMessage(status === 'PUBLISHED' ? 'Đã xuất bản học liệu.' : 'Đã lưu trữ học liệu.');
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, status === 'PUBLISHED' ? 'Không thể xuất bản học liệu.' : 'Không thể lưu trữ học liệu.'));
    }
  };

  const stats = useMemo(
    () => ({
      total: items.length,
      published: items.filter((item) => item.status === 'PUBLISHED').length,
      ielts: items.filter((item) => (item.examCategory || 'GENERAL') === 'IELTS').length,
      toeic: items.filter((item) => item.examCategory === 'TOEIC').length,
    }),
    [items],
  );

  if (loading) {
    return <ContentManagerLoadingState message="Đang tải kho học liệu trung tâm..." />;
  }

  if (error) {
    return <ClassroomErrorState message={error} onRetry={loadItems} />;
  }

  return (
    <motion.div
      className="space-y-6"
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.32, ease: 'easeOut' }}
    >
      {message ? (
        <div
          className={`rounded-2xl border px-4 py-3 text-sm ${
            /^Đã /.test(message)
              ? 'border-emerald-100 bg-emerald-50 text-emerald-800'
              : 'border-rose-100 bg-rose-50 text-rose-800'
          }`}
        >
          {message}
        </div>
      ) : null}

      <HeaderActions>
        <button
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#730014] active:scale-[0.98]"
          onClick={() => setComposerOpen(true)}
          type="button"
        >
          <Plus className="h-4 w-4" />
          Thêm học liệu mới
        </button>
      </HeaderActions>

      {composerOpen && (
        <MaterialEditorModal onClose={() => resetForm(false)}>
          <div className="mb-5 flex items-start justify-between gap-3">
            <div>
              <h3 className="font-['Manrope'] text-xl font-extrabold text-[#0b1c30]">
                {editingId ? 'Chỉnh sửa học liệu' : 'Thêm học liệu mới'}
              </h3>
              <p className="mt-1 text-sm text-[#584140]">
                Nhập metadata học liệu; hệ thống sẽ lưu vào thư viện trung tâm để sử dụng lại.
              </p>
            </div>
            <button className="rounded-lg border border-[#dcc0bf]/40 px-3 py-2 text-sm font-bold text-[#4b0009] hover:bg-[#fff7f7]" onClick={() => resetForm(false)} type="button">
              Đóng
            </button>
          </div>

          <div className="space-y-4">
            <TextInput
              label="Tên học liệu *"
              value={form.title}
              onChange={(value) => setForm((current) => ({ ...current, title: value }))}
              placeholder="Ví dụ: Bộ từ vựng IELTS Writing Band 6.5"
            />
            <RichTextEditor
              label="Mô tả"
              onChange={(value) => setForm((current) => ({ ...current, description: value }))}
              placeholder="Mô tả ngắn cách giáo viên nên dùng học liệu này trong lớp hoặc giao cho học viên."
              size="compact"
              value={form.description}
            />

            <div className="space-y-2">
              <label className="block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">Tải tệp lên</label>
              <div className="flex flex-wrap items-center gap-3">
                <label className="inline-flex cursor-pointer items-center gap-2 rounded-2xl border border-dashed border-[#dfbfbd]/75 bg-[#fffafb] px-4 py-3 text-sm font-bold text-[#730014] transition hover:border-[#730014]">
                  <Upload className="h-4 w-4" />
                  {uploading ? 'Đang tải...' : 'Chọn tệp'}
                  <input
                    className="hidden"
                    type="file"
                    accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.zip,.rar,.jpg,.jpeg,.png"
                    onChange={(event) => {
                      handleUpload(event.target.files?.[0] || null);
                      event.target.value = '';
                    }}
                  />
                </label>
                <span className="text-sm text-[#8b706e]">Hoặc dán liên kết ở ô bên dưới nếu học liệu nằm trên nền tảng ngoài.</span>
              </div>
            </div>

            <TextInput
              label="Liên kết tệp *"
              value={form.fileUrl}
              onChange={(value) => setForm((current) => ({ ...current, fileUrl: value, provider: current.provider || guessProvider(value) }))}
              placeholder="https://docs.google.com/document/d/... hoặc link tải tệp"
            />

            <div className="grid gap-4 md:grid-cols-2">
              <TextInput label="Định dạng tệp" value={form.fileType} onChange={(value) => setForm((current) => ({ ...current, fileType: value }))} placeholder="Ví dụ: PDF, DOCX, ZIP" />
              <TextInput label="Nguồn / Nền tảng" value={form.provider} onChange={(value) => setForm((current) => ({ ...current, provider: value }))} placeholder="Ví dụ: Google Drive, Youtube, EnglishLab" />
            </div>

            <div className="grid gap-4 md:grid-cols-3">
              <FilterSelect
                label="Kỹ năng chính"
                value={form.skill}
                options={skillOptions}
                onChange={(value) => setForm((current) => ({ ...current, skill: value }))}
              />
              <FilterSelect
                label="Loại học liệu"
                value={form.materialType}
                options={materialTypeOptions.map((value) => ({ label: value, value }))}
                onChange={(value) => setForm((current) => ({ ...current, materialType: value }))}
              />
              <FilterSelect
                label="Nhóm chứng chỉ"
                value={form.examCategory}
                options={examOptions.map((value) => ({ label: value, value }))}
                onChange={(value) => setForm((current) => ({ ...current, examCategory: value }))}
              />
            </div>

            <div className="grid gap-4 md:grid-cols-4">
              <TextInput label="IELTS Band tối thiểu" value={String(form.ieltsBandMin)} onChange={(value) => setForm((current) => ({ ...current, ieltsBandMin: value }))} placeholder="5.5" />
              <TextInput label="IELTS Band tối đa" value={String(form.ieltsBandMax)} onChange={(value) => setForm((current) => ({ ...current, ieltsBandMax: value }))} placeholder="7.5" />
              <TextInput label="TOEIC điểm tối thiểu" value={String(form.toeicScoreMin)} onChange={(value) => setForm((current) => ({ ...current, ieltsBandMin: value }))} placeholder="550" />
              <TextInput label="TOEIC điểm tối đa" value={String(form.toeicScoreMax)} onChange={(value) => setForm((current) => ({ ...current, ieltsBandMax: value }))} placeholder="850" />
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <TextInput label="Nhãn gợi ý" value={form.tags} onChange={(value) => setForm((current) => ({ ...current, tags: value }))} placeholder="band 6.5, luyện viết, ôn tập" />
              <FilterSelect
                label="Trạng thái"
                value={form.status}
                options={statusOptions.map((value) => ({ label: labelStatus(value), value }))}
                onChange={(value) => setForm((current) => ({ ...current, status: value }))}
              />
            </div>

            <div className="flex flex-wrap gap-3 pt-2">
              <button
                className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#730014] disabled:opacity-60"
                disabled={saving}
                onClick={handleSave}
                type="button"
              >
                <Save className="h-4 w-4" />
                {saving ? 'Đang lưu...' : editingId ? 'Lưu cập nhật' : 'Thêm vào thư viện'}
              </button>
              <button
                className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/70 px-5 py-3 text-sm font-bold text-[#584140] transition hover:bg-[#fff2f3]"
                onClick={() => resetForm(true)}
                type="button"
              >
                <FilePlus2 className="h-4 w-4" />
                Tạo biểu mẫu mới
              </button>
            </div>
          </div>
        </MaterialEditorModal>
      )}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={FileStack} label="Tổng học liệu" value={stats.total} note="Toàn bộ kho trung tâm" />
        <StatCard icon={Archive} label="Đã xuất bản" value={stats.published} note="Giáo viên có thể chọn ngay" />
        <StatCard icon={BookOpen} label="IELTS" value={stats.ielts} note="Theo band mục tiêu" />
        <StatCard icon={Globe} label="TOEIC" value={stats.toeic} note="Theo dải điểm mục tiêu" />
      </section>

      <div className="grid gap-6">
        <div className="space-y-6">
          <Panel className="rounded-xl border-[#dcc0bf]/30 bg-white p-4 shadow-sm">
            <div className="flex flex-wrap items-center gap-4">
              <div className="min-w-[300px] flex-1">
                <div className="relative">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-[#897270]" />
                  <input
                    value={keyword}
                    onChange={(event) => setKeyword(event.target.value)}
                    placeholder="Tìm học liệu, nguồn, kỹ năng hoặc tag..."
                    className="w-full rounded-lg border border-[#dcc0bf]/50 bg-[#f8f9ff] py-2 pl-10 pr-4 text-sm text-[#0b1c30] outline-none transition focus:border-[#4b0009] focus:bg-white focus:ring-4 focus:ring-[#4b0009]/5"
                  />
                </div>
              </div>
              <div className="grid w-full gap-3 sm:grid-cols-2 xl:w-auto xl:grid-cols-5">
                <FilterSelect compact label="Kỳ thi" value={filters.examCategory} options={[{ label: 'Tất cả', value: 'ALL' }, ...examOptions.map((value) => ({ label: value, value }))]} onChange={(value) => setFilters((current) => ({ ...current, examCategory: value }))} />
                <FilterSelect compact label="Loại" value={filters.materialType} options={[{ label: 'Tất cả', value: 'ALL' }, ...materialTypeOptions.map((value) => ({ label: value, value }))]} onChange={(value) => setFilters((current) => ({ ...current, materialType: value }))} />
                <FilterSelect compact label="Kỹ năng" value={filters.skill} options={[{ label: 'Tất cả', value: 'ALL' }, ...skillOptions]} onChange={(value) => setFilters((current) => ({ ...current, skill: value }))} />
                <FilterSelect compact label="Trạng thái" value={filters.status} options={[{ label: 'Tất cả', value: 'ALL' }, ...statusOptions.map((value) => ({ label: labelStatus(value), value }))]} onChange={(value) => setFilters((current) => ({ ...current, status: value }))} />
                <FilterSelect compact label="Nguồn" value={filters.provider} options={providerOptions} onChange={(value) => setFilters((current) => ({ ...current, provider: value }))} />
              </div>
              <button
                aria-label="Làm mới kho học liệu"
                className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#dcc0bf]/40 bg-white text-[#564241] transition hover:bg-[#eff4ff]"
                onClick={loadItems}
                type="button"
              >
                <RefreshCw className="h-4 w-4" />
              </button>
            </div>
          </Panel>

          <Panel className="overflow-hidden rounded-xl border-[#dcc0bf]/30 bg-white shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[1080px] border-collapse text-left">
                <thead>
                  <tr className="border-b border-[#dcc0bf]/30 bg-[#fbf3f4]">
                    {['Học liệu', 'Loại', 'Kỹ năng', 'Kỳ thi / mức', 'Nguồn', 'Trạng thái', 'Cập nhật', 'Thao tác'].map((heading) => (
                      <th className={`px-6 py-4 text-xs font-bold uppercase tracking-[0.12em] text-[#8e7371] ${heading === 'Thao tác' ? 'text-right' : ''}`} key={heading}>{heading}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#dcc0bf]/15">
                  {pageItems.map((item) => (
                    <tr className="transition hover:bg-[#eff4ff]" key={item.id}>
                      <td className="px-6 py-5">
                        <p className="max-w-[320px] overflow-hidden text-sm font-bold leading-5 text-[#4b0009] [display:-webkit-box] [-webkit-box-orient:vertical] [-webkit-line-clamp:2]">{item.title}</p>
                        <p className="mt-1 max-w-[360px] overflow-hidden text-xs leading-5 text-[#584140] [display:-webkit-box] [-webkit-box-orient:vertical] [-webkit-line-clamp:2]">
                          {stripRichTextToPlain(item.description) || item.tags || 'Chưa có mô tả'}
                        </p>
                      </td>
                      <td className="px-6 py-5 text-sm font-semibold text-[#0b1c30]">{item.materialType || 'LINK'}</td>
                      <td className="px-6 py-5 text-sm text-[#564241]">{item.skill || 'Mixed'}</td>
                      <td className="px-6 py-5 text-sm text-[#564241]">{formatTargetRange(item)}</td>
                      <td className="px-6 py-5 text-sm text-[#564241]">{item.provider || 'EnglishLab'}</td>
                      <td className="px-6 py-5"><StatusBadge label={labelStatus(item.status || 'PUBLISHED')} /></td>
                      <td className="px-6 py-5 text-sm text-[#564241]">{formatDate(item.updatedAt)}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-right">
                        <div className="inline-flex items-center justify-end gap-2">
                          <a
                            className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-[#fffafb] px-3 text-xs font-bold text-[#730014] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95"
                            href={item.fileUrl}
                            rel="noreferrer"
                            target="_blank"
                          >
                            <LinkIcon className="h-3.5 w-3.5" />
                            Mở
                          </a>
                          <a
                            aria-label={`Tải tài liệu ${item.title}`}
                            className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 whitespace-nowrap rounded-lg bg-[#730014] px-3 text-xs font-bold text-white transition hover:bg-[#8a0018] active:scale-95"
                            download
                            href={item.fileUrl}
                            rel="noreferrer"
                            target="_blank"
                            title="Tải tài liệu"
                          >
                            <Download className="h-3.5 w-3.5" />
                            Tải
                          </a>
                          <button
                            className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#8b706e]/50 bg-white px-3 text-xs font-bold text-[#4b0009] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95"
                            onClick={() => openEdit(item)}
                            type="button"
                          >
                            <PencilLine className="h-3.5 w-3.5" />
                            Sửa
                          </button>
                          {item.status === 'DRAFT' ? (
                            <button
                              className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg bg-[#730014] px-3 text-xs font-bold text-white whitespace-nowrap transition hover:bg-[#8a0018] active:scale-95"
                              onClick={() => changeMaterialStatus(item, 'PUBLISHED')}
                              type="button"
                            >
                              <CheckCircle2 className="h-3.5 w-3.5" />
                              Xuất bản
                            </button>
                          ) : null}
                          {item.status === 'PUBLISHED' ? (
                            <button
                              aria-label={`Lưu trữ ${item.title}`}
                              className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-rose-200 bg-white text-rose-700 transition hover:bg-rose-50 active:scale-95"
                              onClick={() => changeMaterialStatus(item, 'ARCHIVED')}
                              title="Lưu trữ"
                              type="button"
                            >
                              <Archive className="h-3.5 w-3.5" />
                            </button>
                          ) : null}
                          <button
                            aria-label={`Xóa ${item.title}`}
                            className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-rose-200 bg-white text-rose-700 transition hover:bg-rose-50 active:scale-95"
                            onClick={() => handleDelete(item)}
                            title="Xóa"
                            type="button"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {!filteredItems.length ? (
              <div className="border-t border-[#dcc0bf]/20 px-6 py-10">
                <ClassroomEmptyState
                  title="Chưa có học liệu phù hợp"
                  description="Hãy thêm học liệu mới hoặc nới bộ lọc để xem lại toàn bộ thư viện."
                  actionLabel="Mở biểu mẫu thêm học liệu"
                  onAction={() => setComposerOpen(true)}
                />
              </div>
            ) : (
              <div className="border-t border-[#dcc0bf]/20 bg-[#fbf3f4]/40 px-6 py-4">
                <Pagination
                  onChange={setCurrentPage}
                  page={currentPage}
                  pageSize={PAGE_SIZE}
                  totalItems={filteredItems.length}
                  totalPages={totalPages}
                />
              </div>
            )}
          </Panel>
        </div>

      </div>
    </motion.div>
  );
}

function StatCard({ icon: Icon, label, value, note }) {
  return (
    <Panel className="p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-[#584140]">{label}</p>
          <p className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#4b0009]">{value}</p>
        </div>
        <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]">
          <Icon className="h-5 w-5" />
        </span>
      </div>
      <p className="mt-3 text-sm text-[#584140]">{note}</p>
    </Panel>
  );
}

function FilterSelect({ label, value, options, onChange, compact = false }) {
  const normalizedOptions = compact
    ? options.map((option) => ({ ...option, buttonLabel: `${label}: ${option.label}` }))
    : options;
  return (
    <div className={compact ? '' : 'space-y-2'}>
      {!compact ? <label className="block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">{label}</label> : null}
      <BrandedSelect
        buttonClassName={compact ? 'h-10 rounded-lg border-[#dcc0bf]/50 bg-[#f8f9ff] py-2 text-sm shadow-none' : undefined}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        options={normalizedOptions}
      />
    </div>
  );
}

function TextInput({ label, value, onChange, placeholder }) {
  return (
    <label className="block space-y-2">
      <span className="block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">{label}</span>
      <input
        className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fcfbfb] px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
      />
    </label>
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

function validateMaterialForm(form) {
  if (!form.title.trim()) return 'Vui lòng nhập tên học liệu.';
  if (!form.fileUrl.trim()) return 'Vui lòng tải tệp lên hoặc dán liên kết học liệu.';

  const ieltsMin = parseOptionalNumber(form.ieltsBandMin);
  const ieltsMax = parseOptionalNumber(form.ieltsBandMax);
  const toeicMin = parseOptionalNumber(form.toeicScoreMin);
  const toeicMax = parseOptionalNumber(form.toeicScoreMax);

  if ([ieltsMin, ieltsMax].some((value) => value != null && (!Number.isFinite(value) || value < 0 || value > 9))) {
    return 'Band IELTS phải nằm trong khoảng từ 0 đến 9.';
  }
  if (ieltsMin != null && ieltsMax != null && ieltsMin > ieltsMax) {
    return 'Band IELTS tối thiểu không thể lớn hơn band tối đa.';
  }
  if ([toeicMin, toeicMax].some((value) => value != null && (!Number.isInteger(value) || value < 0 || value > 990))) {
    return 'Điểm TOEIC phải là số nguyên trong khoảng từ 0 đến 990.';
  }
  if (toeicMin != null && toeicMax != null && toeicMin > toeicMax) {
    return 'Điểm TOEIC tối thiểu không thể lớn hơn điểm tối đa.';
  }
  return '';
}

function parseOptionalNumber(value) {
  if (value === '' || value == null) return null;
  return Number(value);
}

function labelStatus(value) {
  const normalized = String(value || '').toUpperCase();
  if (normalized === 'PUBLISHED') return 'Đã xuất bản';
  if (normalized === 'DRAFT') return 'Bản nháp';
  if (normalized === 'ARCHIVED') return 'Lưu trữ';
  return value || 'Không rõ';
}

function formatDate(value) {
  if (!value) return 'Chưa cập nhật';
  return new Date(value).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

function MaterialEditorModal({ children, onClose }) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-hidden px-3 py-4 sm:px-6 animate-fade-in" role="dialog" aria-modal="true">
      <button
        aria-label="Đóng modal"
        className="absolute -inset-10 bg-[#1a0004]/45 backdrop-blur-sm"
        onClick={onClose}
        type="button"
      />
      <div className="relative z-10 w-full max-w-[800px] pointer-events-auto bg-[#fafafa] rounded-3xl border border-[#dcc0bf]/35 p-6 shadow-2xl overflow-y-auto max-h-[90vh]">
        {children}
      </div>
    </div>,
    document.body
  );
}
