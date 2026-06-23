import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Archive,
  BookOpen,
  FilePlus2,
  FileStack,
  Globe,
  Link as LinkIcon,
  Minus,
  PencilLine,
  Plus,
  RefreshCw,
  Save,
  Trash2,
  Upload,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { ContentManagerLoadingState, Panel, SectionTitle, StatusBadge } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { ClassroomEmptyState, ClassroomErrorState } from '../../components/classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';

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
  status: 'PUBLISHED',
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
  status: form.status || 'PUBLISHED',
});

export default function ContentManagerMaterialsPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [composerOpen, setComposerOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
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
  }, [filters]);

  const providerOptions = useMemo(() => {
    const values = Array.from(new Set(items.map((item) => item.provider).filter(Boolean)));
    return [{ label: 'Tất cả', value: 'ALL' }, ...values.map((value) => ({ label: value, value }))];
  }, [items]);

  const filteredItems = useMemo(
    () =>
      items.filter(
        (item) =>
          (filters.examCategory === 'ALL' || (item.examCategory || 'GENERAL') === filters.examCategory) &&
          (filters.materialType === 'ALL' || (item.materialType || 'LINK') === filters.materialType) &&
          (filters.skill === 'ALL' || (item.skill || 'Mixed') === filters.skill) &&
          (filters.status === 'ALL' || (item.status || 'PUBLISHED') === filters.status) &&
          (filters.provider === 'ALL' || (item.provider || '') === filters.provider),
      ),
    [filters, items],
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

  const resetForm = () => {
    setEditingId(null);
    setForm(emptyForm);
    setComposerOpen(true);
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
      status: item.status || 'PUBLISHED',
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
      if (editingId) {
        await classroomApi.updateContentManagerMaterialLibraryItem(editingId, payload);
        setMessage('Đã cập nhật học liệu trung tâm.');
      } else {
        await classroomApi.createContentManagerMaterialLibraryItem(payload);
        setMessage('Đã thêm học liệu mới vào thư viện trung tâm.');
      }
      resetForm();
      await loadItems();
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể lưu học liệu trung tâm.'));
      setComposerOpen(true);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (item) => {
    if (!window.confirm(`Xóa học liệu "${item.title}" khỏi thư viện trung tâm?`)) {
      return;
    }
    setMessage('');
    try {
      await classroomApi.deleteContentManagerMaterialLibraryItem(item.id);
      if (editingId === item.id) {
        resetForm();
      }
      setMessage('Đã xóa học liệu khỏi thư viện trung tâm.');
      await loadItems();
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể xóa học liệu trung tâm.'));
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

      <Panel className="p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <SectionTitle title={editingId ? 'Chỉnh sửa học liệu' : 'Thêm học liệu mới'} />
            <p className="mt-2 max-w-3xl text-sm leading-7 text-[#584140]">
              Học liệu trung tâm được dùng lại theo band IELTS, dải điểm TOEIC, kỹ năng và mục tiêu lớp học.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            {!composerOpen ? (
              <button
                className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#730014]"
                onClick={() => setComposerOpen(true)}
                type="button"
              >
                <Plus className="h-4 w-4" />
                Thêm học liệu mới
              </button>
            ) : null}

            <button
              className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-[#dfbfbd]/65 bg-white text-[#730014] transition hover:bg-[#fff2f3]"
              onClick={() => setComposerOpen((current) => !current)}
              type="button"
            >
              {composerOpen ? <Minus className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
            </button>
          </div>
        </div>

        {composerOpen ? (
          <div className="mt-6 space-y-4 border-t border-[#f0e3e4] pt-6">
            <TextInput
              label="Tên học liệu *"
              value={form.title}
              onChange={(value) => setForm((current) => ({ ...current, title: value }))}
              placeholder="Ví dụ: Bộ từ vựng IELTS Writing Band 6.5"
            />
            <TextArea
              label="Mô tả"
              value={form.description}
              onChange={(value) => setForm((current) => ({ ...current, description: value }))}
              placeholder="Mô tả ngắn cách giáo viên nên dùng học liệu này trong lớp hoặc giao cho học viên."
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
                    onChange={(event) => handleUpload(event.target.files?.[0] || null)}
                  />
                </label>
                <span className="text-sm text-[#8b706e]">Hoặc dán liên kết ở ô bên dưới nếu học liệu nằm trên nền tảng ngoài.</span>
              </div>
            </div>

            <TextInput
              label="Liên kết hoặc URL tệp *"
              value={form.fileUrl}
              onChange={(value) => setForm((current) => ({ ...current, fileUrl: value, provider: current.provider || guessProvider(value) }))}
              placeholder="https://..."
            />

            <div className="grid gap-4 md:grid-cols-2">
              <FilterSelect
                label="Loại học liệu"
                value={form.materialType}
                options={materialTypeOptions.map((value) => ({ label: value, value }))}
                onChange={(value) => setForm((current) => ({ ...current, materialType: value }))}
              />
              <TextInput
                label="Nguồn cung cấp"
                value={form.provider}
                onChange={(value) => setForm((current) => ({ ...current, provider: value }))}
                placeholder="EnglishLab / Google Drive / ..."
              />
              <FilterSelect
                label="Nhóm kỳ thi"
                value={form.examCategory}
                options={examOptions.map((value) => ({ label: value, value }))}
                onChange={(value) => setForm((current) => ({ ...current, examCategory: value }))}
              />
              <FilterSelect
                label="Kỹ năng"
                value={form.skill}
                options={skillOptions}
                onChange={(value) => setForm((current) => ({ ...current, skill: value }))}
              />
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <TextInput label="IELTS band tối thiểu" value={String(form.ieltsBandMin)} onChange={(value) => setForm((current) => ({ ...current, ieltsBandMin: value }))} placeholder="5.5" />
              <TextInput label="IELTS band tối đa" value={String(form.ieltsBandMax)} onChange={(value) => setForm((current) => ({ ...current, ieltsBandMax: value }))} placeholder="7.0" />
              <TextInput label="TOEIC điểm tối thiểu" value={String(form.toeicScoreMin)} onChange={(value) => setForm((current) => ({ ...current, toeicScoreMin: value }))} placeholder="650" />
              <TextInput label="TOEIC điểm tối đa" value={String(form.toeicScoreMax)} onChange={(value) => setForm((current) => ({ ...current, toeicScoreMax: value }))} placeholder="850" />
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

            <div className="rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] p-4 text-sm leading-7 text-[#584140]">
              Nếu học liệu chưa sẵn sàng cho giáo viên dùng lại, hãy để trạng thái là <strong className="text-[#4b0009]">Bản nháp</strong>.
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
                onClick={resetForm}
                type="button"
              >
                <FilePlus2 className="h-4 w-4" />
                Tạo biểu mẫu mới
              </button>
            </div>
          </div>
        ) : null}
      </Panel>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={FileStack} label="Tổng học liệu" value={stats.total} note="Toàn bộ kho trung tâm" />
        <StatCard icon={Archive} label="Đã xuất bản" value={stats.published} note="Giáo viên có thể chọn ngay" />
        <StatCard icon={BookOpen} label="IELTS" value={stats.ielts} note="Theo band mục tiêu" />
        <StatCard icon={Globe} label="TOEIC" value={stats.toeic} note="Theo dải điểm mục tiêu" />
      </section>

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <div className="space-y-6">
          <Panel className="p-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <SectionTitle title="Bộ lọc thư viện" />
              <button
                className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/65 bg-white px-4 py-3 text-sm font-bold text-[#730014] transition hover:bg-[#fff2f3]"
                onClick={loadItems}
                type="button"
              >
                <RefreshCw className="h-4 w-4" />
                Làm mới dữ liệu
              </button>
            </div>

            <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-5">
              <FilterSelect label="Kỳ thi" value={filters.examCategory} options={[{ label: 'Tất cả', value: 'ALL' }, ...examOptions.map((value) => ({ label: value, value }))]} onChange={(value) => setFilters((current) => ({ ...current, examCategory: value }))} />
              <FilterSelect label="Loại học liệu" value={filters.materialType} options={[{ label: 'Tất cả', value: 'ALL' }, ...materialTypeOptions.map((value) => ({ label: value, value }))]} onChange={(value) => setFilters((current) => ({ ...current, materialType: value }))} />
              <FilterSelect label="Kỹ năng" value={filters.skill} options={[{ label: 'Tất cả', value: 'ALL' }, ...skillOptions]} onChange={(value) => setFilters((current) => ({ ...current, skill: value }))} />
              <FilterSelect label="Trạng thái" value={filters.status} options={[{ label: 'Tất cả', value: 'ALL' }, ...statusOptions.map((value) => ({ label: labelStatus(value), value }))]} onChange={(value) => setFilters((current) => ({ ...current, status: value }))} />
              <FilterSelect label="Nguồn" value={filters.provider} options={providerOptions} onChange={(value) => setFilters((current) => ({ ...current, provider: value }))} />
            </div>
          </Panel>

          <Panel className="overflow-hidden">
            <div className="flex items-center justify-between border-b border-[#f0e3e4] px-6 py-5">
              <SectionTitle title="Danh sách học liệu" />
              <span className="text-sm font-semibold text-[#8b706e]">
                Trang {currentPage}/{totalPages}
              </span>
            </div>

            {!filteredItems.length ? (
              <div className="p-6">
                <ClassroomEmptyState
                  title="Chưa có học liệu phù hợp"
                  description="Hãy thêm học liệu mới hoặc nới bộ lọc để xem lại toàn bộ thư viện."
                  actionLabel="Mở biểu mẫu thêm học liệu"
                  onAction={() => setComposerOpen(true)}
                />
              </div>
            ) : (
              <div className="divide-y divide-[#f0e3e4]">
                {pageItems.map((item) => (
                  <article key={item.id} className="space-y-4 px-6 py-5">
                    <div className="flex flex-wrap items-start justify-between gap-4">
                      <div className="space-y-2">
                        <div className="flex flex-wrap items-center gap-2">
                          <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">{item.title}</h3>
                          <StatusBadge label={labelStatus(item.status || 'PUBLISHED')} />
                        </div>
                        <p className="max-w-3xl text-sm leading-6 text-[#584140]">
                          {item.description || 'Chưa có mô tả. Học liệu này hiện sẵn sàng để giáo viên gắn vào lớp học phù hợp.'}
                        </p>
                      </div>

                      <div className="flex flex-wrap gap-2">
                        <button
                          className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd]/70 bg-white px-4 py-2.5 text-sm font-bold text-[#730014] transition hover:bg-[#fff2f3]"
                          onClick={() => openEdit(item)}
                          type="button"
                        >
                          <PencilLine className="h-4 w-4" />
                          Sửa
                        </button>
                        <button
                          className="inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#730014]"
                          onClick={() => handleDelete(item)}
                          type="button"
                        >
                          <Trash2 className="h-4 w-4" />
                          Xóa
                        </button>
                      </div>
                    </div>

                    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                      <InfoPill label="Loại học liệu" value={item.materialType || 'LINK'} />
                      <InfoPill label="Nguồn" value={item.provider || 'EnglishLab'} />
                      <InfoPill label="Kỹ năng" value={item.skill || 'Mixed'} />
                      <InfoPill label="Kỳ thi / mức" value={formatTargetRange(item)} />
                    </div>

                    <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] px-4 py-3">
                      <div className="text-sm text-[#584140]">
                        Cập nhật gần nhất: <span className="font-semibold text-[#2b2828]">{formatDate(item.updatedAt)}</span>
                      </div>
                      <a
                        className="inline-flex items-center gap-2 text-sm font-bold text-[#730014] hover:underline"
                        href={item.fileUrl}
                        rel="noreferrer"
                        target="_blank"
                      >
                        <LinkIcon className="h-4 w-4" />
                        Mở học liệu
                      </a>
                    </div>
                  </article>
                ))}
              </div>
            )}

            {filteredItems.length ? (
              <div className="flex items-center justify-between border-t border-[#f0e3e4] px-6 py-4">
                <span className="text-sm text-[#8b706e]">
                  Hiển thị {(currentPage - 1) * PAGE_SIZE + 1}-{Math.min(currentPage * PAGE_SIZE, filteredItems.length)} trên {filteredItems.length} học liệu
                </span>
                <div className="flex gap-2">
                  <button
                    className="rounded-xl border border-[#dfbfbd]/70 px-4 py-2 text-sm font-bold text-[#584140] transition hover:bg-[#fff2f3] disabled:opacity-45"
                    disabled={currentPage === 1}
                    onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
                    type="button"
                  >
                    Trang trước
                  </button>
                  <button
                    className="rounded-xl border border-[#dfbfbd]/70 px-4 py-2 text-sm font-bold text-[#584140] transition hover:bg-[#fff2f3] disabled:opacity-45"
                    disabled={currentPage === totalPages}
                    onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
                    type="button"
                  >
                    Trang sau
                  </button>
                </div>
              </div>
            ) : null}
          </Panel>
        </div>

        <Panel className="p-6">
          <SectionTitle title="Truy cập nhanh" />
          <div className="mt-5 space-y-4">
            <div className="rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] p-4">
              <p className="text-sm font-semibold text-[#4b0009]">Theo dõi tài liệu đã gắn</p>
              <p className="mt-2 text-sm leading-7 text-[#584140]">
                Xem lớp nào đang dùng tài liệu nào và tách rõ tài liệu từ kho trung tâm với tài liệu riêng của lớp.
              </p>
              <Link
                className="mt-4 inline-flex items-center rounded-xl border border-[#dfbfbd]/70 bg-white px-4 py-2.5 text-sm font-bold text-[#730014] transition hover:bg-[#fff2f3]"
                to="/content-manager/classrooms"
              >
                Mở tài liệu lớp học
              </Link>
            </div>
            <div className="rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] p-4 text-sm leading-7 text-[#584140]">
              Dùng bộ lọc band, kỹ năng, nguồn và trạng thái để giữ kho học liệu gọn và dễ tái sử dụng.
            </div>
          </div>
        </Panel>
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

function FilterSelect({ label, value, options, onChange }) {
  return (
    <div className="space-y-2">
      <label className="block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">{label}</label>
      <BrandedSelect value={value} onChange={(event) => onChange(event.target.value)} options={options} />
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

function TextArea({ label, value, onChange, placeholder }) {
  return (
    <label className="block space-y-2">
      <span className="block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">{label}</span>
      <textarea
        className="min-h-[128px] w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fcfbfb] px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
      />
    </label>
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
