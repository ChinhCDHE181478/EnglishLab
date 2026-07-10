import { useEffect, useState } from 'react';
import { Check, Pencil, Plus, RefreshCw, Trash2, X } from 'lucide-react';
import courseApi from '../../api/courseApi';
import { ContentManagerLoadingState, Panel, StatusBadge, TextField } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';

const emptyForm = {
  code: '',
  name: '',
  description: '',
  displayOrder: '0',
  active: 'true',
};

export default function ContentManagerCategoriesPage() {
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
    if (!window.confirm(`Xóa danh mục “${category.name}”?`)) return;
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
      <div className="flex flex-wrap justify-end gap-3">
        <button
          className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/70 bg-white px-4 py-3 text-sm font-bold text-[#730014]"
          onClick={loadCategories}
          type="button"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </button>
        <button
          className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white disabled:cursor-not-allowed disabled:opacity-50"
          onClick={openCreate}
          type="button"
        >
          <Plus className="h-4 w-4" />
          Thêm danh mục
        </button>
      </div>

      {error ? <Notice tone="error">{error}</Notice> : null}
      {success ? <Notice tone="success">{success}</Notice> : null}

      {editorOpen ? (
        <Panel className="p-6">
          <div className="mb-5 flex items-center justify-between gap-4">
            <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">
              {editingId ? 'Chỉnh sửa danh mục' : 'Thêm danh mục'}
            </h2>
            <button className="rounded-xl p-2 text-[#730014] hover:bg-[#fff2f3]" onClick={closeEditor} type="button">
              <X className="h-5 w-5" />
            </button>
          </div>
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
              <TextField
                label="Mô tả"
                onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
                rows={3}
                textarea
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
        </Panel>
      ) : null}

      <Panel className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full text-left">
            <thead className="bg-[#fbf3f4] text-xs uppercase tracking-[0.16em] text-[#8e7371]">
              <tr>
                {['Thứ tự', 'Mã', 'Tên hiển thị', 'Mô tả', 'Khóa học', 'Trạng thái', 'Thao tác'].map((heading) => (
                  <th key={heading} className="px-5 py-4 font-semibold">{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#f0e3e4]">
              {categories.length ? categories.map((category) => (
                <tr key={category.id}>
                  <td className="px-5 py-4 text-sm">{category.displayOrder}</td>
                  <td className="px-5 py-4 text-sm font-bold text-[#4b0009]">{category.code}</td>
                  <td className="px-5 py-4 font-semibold">{category.name}</td>
                  <td className="max-w-md px-5 py-4 text-sm text-[#584140]">{category.description || 'Chưa có mô tả'}</td>
                  <td className="px-5 py-4 text-sm font-bold">{category.courseCount}</td>
                  <td className="px-5 py-4"><StatusBadge label={category.active ? 'Đang hoạt động' : 'Tạm ngừng'} /></td>
                  <td className="px-5 py-4">
                    <div className="flex gap-2">
                      <button className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]" onClick={() => openEdit(category)} type="button">
                        <Pencil className="h-4 w-4" />
                        Sửa
                      </button>
                      <button className="inline-flex items-center gap-2 rounded-xl border border-rose-200 px-3 py-2 text-sm font-semibold text-rose-700 disabled:cursor-not-allowed disabled:opacity-40" disabled={category.courseCount > 0} onClick={() => deleteCategory(category)} type="button">
                        <Trash2 className="h-4 w-4" />
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
