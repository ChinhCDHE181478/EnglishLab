import { useEffect, useMemo, useState } from 'react';
import { useAppDialog } from '../../components/ui/AppDialog';
import { Check, Pencil, Plus, RefreshCw, Trash2, X } from 'lucide-react';
import courseApi from '../../api/courseApi';
import { ContentManagerLoadingState, HeaderActions, Panel, StatusBadge, TextField } from '../../components/content-manager/ContentManagerUi';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import { stripRichTextToPlain } from '../../utils/lessonRichText';

const emptyForm = {
  code: '',
  name: '',
  description: '',
  displayOrder: '0',
  active: 'true',
};

export default function ContentManagerCategoriesPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadCategories = async () => {
    setLoading(true);
    setError('');
    try {
      setCategories(await courseApi.getManagedCourseCategories());
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh mục khóa học.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCategories();
  }, []);

  const sortedCategories = useMemo(() => {
    return [...categories].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || String(a.code).localeCompare(String(b.code)));
  }, [categories]);

  const { page, setPage, totalPages, pageItems: paginatedCategories, totalItems } = usePagination(
    sortedCategories,
    8,
    'course-categories'
  );

  const openCreate = () => {
    setEditingId(null);
    setForm(emptyForm);
    setEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const openEdit = (category) => {
    setEditingId(category.id);
    setForm({
      code: category.code,
      name: category.name || '',
      description: category.description || '',
      displayOrder: String(category.displayOrder ?? 0),
      active: String(category.active !== false),
    });
    setEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const closeEditor = () => {
    setEditorOpen(false);
    setEditingId(null);
    setForm(emptyForm);
  };

  const saveCategory = async () => {
    if (!form.name.trim()) {
      setError('Hãy nhập tên hiển thị cho danh mục.');
      return;
    }
    if (!editingId && !form.code.trim()) {
      setError('Hãy nhập mã danh mục.');
      return;
    }

    setSaving(true);
    setError('');
    setSuccess('');
    const payload = {
      code: form.code.trim().toUpperCase(),
      name: form.name.trim(),
      description: form.description.trim() || null,
      displayOrder: Number(form.displayOrder || 0),
      active: form.active === 'true',
    };

    try {
      if (editingId) {
        await courseApi.updateManagedCourseCategory(editingId, payload);
        setSuccess('Đã cập nhật danh mục khóa học.');
      } else {
        await courseApi.createManagedCourseCategory(payload);
        setSuccess('Đã tạo danh mục khóa học.');
      }
      closeEditor();
      await loadCategories();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được danh mục khóa học.');
    } finally {
      setSaving(false);
    }
  };

  const deleteCategory = async (category) => {
    if (!await confirmDialog(`Xóa danh mục “${category.name}”?`, {
      title: 'Xóa danh mục',
      confirmLabel: 'Xóa danh mục',
      tone: 'danger',
    })) return;
    setError('');
    setSuccess('');
    try {
      await courseApi.deleteManagedCourseCategory(category.id);
      setSuccess('Đã xóa danh mục khóa học.');
      await loadCategories();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể xóa danh mục này.');
    }
  };

  if (loading && !categories.length) {
    return <ContentManagerLoadingState message="Đang tải danh mục khóa học..." />;
  }

  return (
    <div className="space-y-6">
      <HeaderActions>
        <button
          className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#730014] active:scale-[0.98]"
          onClick={openCreate}
          type="button"
        >
          <Plus className="h-4 w-4" />
          Thêm danh mục
        </button>
      </HeaderActions>

      {error ? <Notice tone="error">{error}</Notice> : null}
      {success ? <Notice tone="success">{success}</Notice> : null}

      {editorOpen ? (
        <CategoryModal onClose={closeEditor}>
          <div className="mb-5 flex items-center justify-between gap-4">
            <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">
              {editingId ? 'Chỉnh sửa danh mục' : 'Thêm danh mục'}
            </h2>
            <button className="rounded-xl p-2 text-[#730014] hover:bg-[#fff2f3]" onClick={closeEditor} type="button">
              <X className="h-5 w-5" />
            </button>
          </div>
          {error ? <div className="mb-4"><Notice tone="error">{error}</Notice></div> : null}
          <div className="grid gap-4 md:grid-cols-2">
            <Field label="Mã danh mục">
              <input
                className="w-full rounded-2xl border border-[#dfbfbd]/65 bg-[#fcfbfb] px-4 py-3 text-sm font-semibold uppercase text-[#1a1c1c] outline-none focus:border-[#730014] disabled:cursor-not-allowed disabled:opacity-60"
                disabled={Boolean(editingId)}
                maxLength={40}
                onChange={(event) => setForm((current) => ({ ...current, code: event.target.value.replace(/\s+/g, '_') }))}
                placeholder="Ví dụ: BUSINESS_ENGLISH"
                value={form.code}
              />
              {!editingId ? (
                <p className="mt-2 text-xs leading-5 text-[#8b706e]">
                  Chỉ dùng chữ in hoa, số và dấu gạch dưới. Hệ thống tự chuẩn hóa: «{normalizeCategoryCodePreview(form.code) || '…'}».
                </p>
              ) : (
                <p className="mt-2 text-xs text-[#8b706e]">Mã danh mục không thể đổi sau khi tạo.</p>
              )}
            </Field>
            <TextField
              label="Tên hiển thị"
              onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
              value={form.name}
            />
            <TextField
              label="Thứ tự hiển thị"
              onChange={(event) => setForm((current) => ({ ...current, displayOrder: event.target.value }))}
              value={form.displayOrder}
            />
            <Field label="Trạng thái">
              <BrandedSelect
                onChange={(event) => setForm((current) => ({ ...current, active: event.target.value }))}
                options={[
                  { label: 'Đang hoạt động', value: 'true' },
                  { label: 'Tạm ngừng', value: 'false' },
                ]}
                value={form.active}
              />
            </Field>
            <div className="md:col-span-2">
              <RichTextEditor
                label="Mô tả"
                onChange={(html) => setForm((current) => ({ ...current, description: html }))}
                placeholder="Mô tả danh mục khóa học..."
                size="compact"
                value={form.description}
              />
            </div>
          </div>
          <div className="mt-5 flex justify-end gap-3">
            <button className="rounded-2xl border border-[#dfbfbd] px-4 py-3 text-sm font-bold text-[#730014]" onClick={closeEditor} type="button">
              Hủy
            </button>
            <button
              className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white disabled:opacity-60"
              disabled={saving}
              onClick={saveCategory}
              type="button"
            >
              <Check className="h-4 w-4" />
              {saving ? 'Đang lưu...' : 'Lưu danh mục'}
            </button>
          </div>
        </CategoryModal>
      ) : null}

      <Panel className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full text-left">
            <thead className="bg-[#fbf3f4] text-[11px] font-extrabold uppercase tracking-wider text-[#8e7371]">
              <tr>
                {['Thứ tự', 'Mã', 'Tên hiển thị', 'Mô tả', 'Khóa học', 'Trạng thái', 'Thao tác'].map((heading) => (
                  <th key={heading} className="px-5 py-4">{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#f0e3e4]">
              {paginatedCategories.length ? paginatedCategories.map((category) => (
                <tr key={category.id}>
                  <td className="px-5 py-4 text-sm">{category.displayOrder}</td>
                  <td className="px-5 py-4 text-sm font-bold text-[#4b0009]">{category.code}</td>
                  <td className="px-5 py-4 font-semibold">{category.name}</td>
                  <td className="max-w-md px-5 py-4 text-sm text-[#584140]">{stripRichTextToPlain(category.description) || 'Chưa có mô tả'}</td>
                  <td className="px-5 py-4 text-sm font-bold">{category.courseCount}</td>
                  <td className="px-5 py-4"><StatusBadge label={category.active ? 'Đang hoạt động' : 'Tạm ngừng'} /></td>
                  <td className="whitespace-nowrap px-5 py-4 text-right">
                    <div className="inline-flex items-center justify-end gap-2">
                      <button
                        className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#dcc0bf]/50 bg-white px-3 text-xs font-bold text-[#4b0009] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95"
                        onClick={() => openEdit(category)}
                        type="button"
                      >
                        <Pencil className="h-3.5 w-3.5" />
                        Sửa
                      </button>
                      <button
                        className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-rose-200 bg-white px-3 text-xs font-bold text-rose-700 whitespace-nowrap transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-40 active:scale-95"
                        disabled={category.courseCount > 0}
                        onClick={() => deleteCategory(category)}
                        type="button"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                        Xóa
                      </button>
                    </div>
                  </td>
                </tr>
              )) : (
                <tr><td className="px-5 py-10 text-sm text-[#584140]" colSpan={7}>Chưa có danh mục khóa học.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {totalPages > 1 && (
          <div className="border-t border-[#dfbfbd]/45 px-6 py-4 bg-[#fffafb]/25">
            <Pagination
              page={page}
              totalPages={totalPages}
              onChange={setPage}
              totalItems={totalItems}
              pageSize={8}
            />
          </div>
        )}
      </Panel>
    </div>
  );
}

function Field({ label, children }) {
  return (
    <div>
      <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">{label}</span>
      {children}
    </div>
  );
}

function Notice({ children, tone }) {
  const className = tone === 'error'
    ? 'border-[#ba1a1a]/20 bg-[#ffdad6] text-[#93000a]'
    : 'border-emerald-200 bg-emerald-50 text-emerald-700';
  return <div className={`rounded-2xl border px-5 py-4 text-sm font-semibold ${className}`}>{children}</div>;
}

function normalizeCategoryCodePreview(value) {
  return String(value || '')
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
}

function CategoryModal({ children, onClose }) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-hidden p-4 sm:p-6 backdrop-blur-sm bg-black/45 animate-fade-in" role="dialog" aria-modal="true">
      <button
        aria-label="Đóng modal"
        className="absolute inset-0 cursor-default"
        onClick={onClose}
        type="button"
      />
      <div className="relative z-10 flex max-h-[calc(100dvh-2.5rem)] w-full max-w-[760px] min-h-0 flex-col overflow-hidden rounded-3xl border border-[#dcc0bf]/35 bg-[#fafafa] shadow-2xl pointer-events-auto">
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-6">
          {children}
        </div>
      </div>
    </div>
  );
}
