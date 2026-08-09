import { useEffect, useMemo, useState } from 'react';
import { useAppDialog } from '../../components/ui/AppDialog';
import { createPortal } from 'react-dom';
import { Check, Edit3, Plus, RefreshCw, Trash2, X } from 'lucide-react';
import courseApi from '../../api/courseApi';
import { HeaderActions, Panel, StatusBadge, TextField } from '../../components/content-manager/ContentManagerUi';
import { formatCoursePrice } from '../../components/course/courseFormatters';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import VietnameseDateTimeInput from '../../components/ui/VietnameseDateTimeInput';

const emptyForm = {
  id: null,
  code: '',
  name: '',
  type: 'PERCENTAGE',
  value: '10',
  usageLimit: '50',
  active: true,
  startsAt: '',
  expiresAt: '',
};

const toDateTimeLocal = (value) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

const toApiDateTime = (value) => (value ? new Date(value).toISOString().slice(0, 19) : null);
const PAGE_SIZE = 8;

export default function ContentManagerDiscountCodesPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [includeInactive, setIncludeInactive] = useState(false);
  const [editorOpen, setEditorOpen] = useState(false);

  const editing = Boolean(form.id);

  const sortedItems = useMemo(
    () => [...items].sort((left, right) => Number(right.id || 0) - Number(left.id || 0)),
    [items],
  );

  const { page, setPage, totalPages, pageItems: visibleItems, totalItems } = usePagination(
    sortedItems,
    PAGE_SIZE,
    `discount-codes-${includeInactive}`
  );

  const loadDiscountCodes = async () => {
    setLoading(true);
    setError('');
    try {
      const page = await courseApi.getDiscountCodes({ size: 100, includeInactive });
      setItems(page.content || []);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải danh sách mã giảm giá.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDiscountCodes();
  }, [includeInactive]);

  const handleChange = (field) => (event) => {
    const value = field === 'active' ? event.target.checked : event.target.value;
    setForm((current) => ({ ...current, [field]: value }));
  };

  const openCreate = () => {
    setForm(emptyForm);
    setEditorOpen(true);
    setMessage('');
    setError('');
  };

  const handleEdit = (item) => {
    setForm({
      id: item.id,
      code: item.code || '',
      name: item.name || '',
      type: item.type || 'PERCENTAGE',
      value: String(item.value ?? ''),
      usageLimit: String(item.usageLimit ?? ''),
      active: Boolean(item.active),
      startsAt: toDateTimeLocal(item.startsAt),
      expiresAt: toDateTimeLocal(item.expiresAt),
    });
    setEditorOpen(true);
    setMessage('');
    setError('');
  };

  const handleReset = () => {
    setForm(emptyForm);
    setEditorOpen(false);
    setMessage('');
    setError('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setMessage('');

    const validationMessage = validateDiscountCode(form);
    if (validationMessage) {
      setError(validationMessage);
      return;
    }

    setSaving(true);

    const payload = {
      code: form.code.trim().toUpperCase(),
      name: form.name.trim(),
      type: form.type,
      value: Number(form.value || 0),
      usageLimit: Number(form.usageLimit || 0),
      active: form.active,
      startsAt: toApiDateTime(form.startsAt),
      expiresAt: toApiDateTime(form.expiresAt),
    };

    try {
      if (editing) {
        await courseApi.updateDiscountCode(form.id, payload);
        setMessage('Đã cập nhật mã giảm giá.');
      } else {
        await courseApi.createDiscountCode(payload);
        setMessage('Đã tạo mã giảm giá.');
      }
      setForm(emptyForm);
      setEditorOpen(false);
      await loadDiscountCodes();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể lưu mã giảm giá.');
    } finally {
      setSaving(false);
    }
  };

  const handleDeactivate = async (id) => {
    const item = items.find((entry) => entry.id === id);
    const unused = item && Number(item.usedCount || 0) === 0 && Number(item.reservedCount || 0) === 0;
    const prompt = unused
      ? `Xóa vĩnh viễn mã giảm giá “${item.code}”?`
      : `Mã “${item.code}” đã được dùng hoặc giữ chỗ — hệ thống sẽ tắt mã thay vì xóa. Tiếp tục?`;
    if (item && !await confirmDialog(prompt, {
      title: unused ? 'Xóa mã giảm giá' : 'Tắt mã giảm giá',
      confirmLabel: unused ? 'Xóa mã' : 'Tắt mã',
      tone: 'danger',
    })) return;
    setSaving(true);
    setError('');
    setMessage('');
    try {
      await courseApi.deleteDiscountCode(id);
      setMessage(unused ? 'Đã xóa mã giảm giá.' : 'Đã tắt mã giảm giá vì đã có lịch sử sử dụng.');
      await loadDiscountCodes();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể xóa mã giảm giá.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      {message ? <div className="mb-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700">{message}</div> : null}

      {editorOpen && (
        <DiscountCodeModal onClose={handleReset}>
          <div className="mb-5 flex items-center justify-between gap-3">
            <div>
              <p className="text-[12px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Biên tập mã giảm giá</p>
              <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
                {editing ? 'Cập nhật mã' : 'Tạo mã mới'}
              </h2>
            </div>
            <button className="rounded-2xl border border-[#dfbfbd]/65 p-3 text-[#730014] transition hover:bg-[#fff2f3]" onClick={handleReset} type="button">
              <X className="h-4 w-4" />
            </button>
          </div>

          {error ? <div className="mb-4 rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-4 py-3 text-sm font-semibold text-[#93000a]">{error}</div> : null}

          <form className="space-y-4" onSubmit={handleSubmit}>
            <TextField label="Mã" onChange={(event) => setForm((current) => ({ ...current, code: event.target.value.toUpperCase() }))} value={form.code} />
            <TextField label="Tên hiển thị" onChange={handleChange('name')} value={form.name} />

            <div>
              <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">Loại giảm giá</span>
              <div className="grid grid-cols-2 gap-2 rounded-2xl border border-[#dfbfbd]/65 bg-[#fcfbfb] p-1">
                {[
                  { label: 'Phần trăm', value: 'PERCENTAGE' },
                  { label: 'VND', value: 'FIXED_AMOUNT' },
                ].map((option) => (
                  <button
                    className={`rounded-xl px-4 py-2.5 text-sm font-extrabold transition ${
                      form.type === option.value ? 'bg-[#4b0009] text-white' : 'text-[#730014] hover:bg-[#fff2f3]'
                    }`}
                    key={option.value}
                    onClick={() => setForm((current) => ({ ...current, type: option.value }))}
                    type="button"
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <TextField label={form.type === 'PERCENTAGE' ? 'Giá trị (%)' : 'Giá trị (VND)'} onChange={handleChange('value')} value={String(form.value)} />
              <TextField label="Giới hạn sử dụng" onChange={handleChange('usageLimit')} value={String(form.usageLimit)} />
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <DateTimeField label="Bắt đầu từ" onChange={(value) => setForm((current) => ({ ...current, startsAt: value }))} value={form.startsAt} />
              <DateTimeField label="Hết hạn lúc" onChange={(value) => setForm((current) => ({ ...current, expiresAt: value }))} value={form.expiresAt} />
            </div>

            <label className="flex items-center gap-3 rounded-2xl border border-[#dfbfbd]/65 bg-[#fcfbfb] px-4 py-3 text-sm font-semibold text-[#1a1c1c]">
              <input checked={form.active} onChange={handleChange('active')} type="checkbox" />
              Đang kích hoạt
            </label>

            <button
              className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-[#4b0009] px-5 py-4 text-sm font-extrabold text-white transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-60"
              disabled={saving}
              type="submit"
            >
              {editing ? <Check className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
              {saving ? 'Đang lưu...' : editing ? 'Lưu thay đổi' : 'Tạo mã'}
            </button>
          </form>
        </DiscountCodeModal>
      )}

      <Panel className="overflow-hidden">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#dfbfbd]/45 px-6 py-5">
          <div>
            <p className="text-[12px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Danh sách mã giảm giá</p>
            <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Kiểm soát sử dụng</h2>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <label className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/65 bg-white px-4 py-3 text-sm font-semibold text-[#584140]">
              <input
                checked={includeInactive}
                onChange={(event) => { setIncludeInactive(event.target.checked); setPage(1); }}
                type="checkbox"
              />
              Hiện mã đã tắt
            </label>
          </div>
        </div>

        <HeaderActions>
          <button
            className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#730014] active:scale-[0.98]"
            onClick={openCreate}
            type="button"
          >
            <Plus className="h-4 w-4" />
            Thêm mã mới
          </button>
        </HeaderActions>

        {loading ? (
          <div className="px-6 py-12 text-sm font-semibold text-[#584140]">Đang tải mã giảm giá...</div>
        ) : sortedItems.length === 0 ? (
          <div className="px-6 py-12 text-sm font-semibold text-[#584140]">Chưa có mã giảm giá nào.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[920px] text-left">
              <thead className="bg-[#fbf3f4] text-[11px] uppercase tracking-[0.16em] text-[#8e7371]">
                <tr>
                  <th className="px-5 py-4">Mã</th>
                  <th className="px-5 py-4">Giá trị</th>
                  <th className="px-5 py-4">Giới hạn</th>
                  <th className="px-5 py-4">Đã dùng</th>
                  <th className="px-5 py-4">Đang giữ</th>
                  <th className="px-5 py-4">Còn lại</th>
                  <th className="px-5 py-4">Trạng thái</th>
                  <th className="px-5 py-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#dfbfbd]/35">
                {visibleItems.map((item) => (
                  <tr key={item.id} className="text-sm text-[#584140]">
                    <td className="px-5 py-4">
                      <p className="font-extrabold text-[#2b2828]">{item.code}</p>
                      <p className="mt-1 text-xs">{item.name}</p>
                    </td>
                    <td className="px-5 py-4 font-semibold text-[#2b2828]">
                      {item.type === 'PERCENTAGE' ? `${Number(item.value || 0)}%` : formatCoursePrice(item.value)}
                    </td>
                    <td className="px-5 py-4">{item.usageLimit}</td>
                    <td className="px-5 py-4">{item.usedCount}</td>
                    <td className="px-5 py-4">{item.reservedCount}</td>
                    <td className="px-5 py-4">{item.remainingUses}</td>
                    <td className="px-5 py-4"><StatusBadge label={item.active ? 'Đang hoạt động' : 'Tạm ngừng'} /></td>
                    <td className="whitespace-nowrap px-5 py-4 text-right">
                      <div className="inline-flex items-center justify-end gap-2">
                        <button
                          aria-label={`Chỉnh sửa mã ${item.code}`}
                          className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#dcc0bf]/50 bg-white px-3 text-xs font-bold text-[#4b0009] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95"
                          onClick={() => handleEdit(item)}
                          title="Chỉnh sửa"
                          type="button"
                        >
                          <Edit3 className="h-3.5 w-3.5" />
                          Sửa
                        </button>
                        <button
                          aria-label={`Xóa mã ${item.code}`}
                          className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-rose-200 bg-white text-rose-700 transition hover:bg-rose-50 disabled:opacity-50 active:scale-95"
                          disabled={saving}
                          onClick={() => handleDeactivate(item.id)}
                          title={Number(item.usedCount || 0) === 0 && Number(item.reservedCount || 0) === 0 ? 'Xóa mã' : 'Tắt mã'}
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
        )}
        {totalPages > 1 && (
          <div className="border-t border-[#dfbfbd]/45 px-6 py-4 bg-[#fffafb]/25">
            <Pagination
              page={page}
              totalPages={totalPages}
              onChange={setPage}
              totalItems={totalItems}
              pageSize={PAGE_SIZE}
            />
          </div>
        )}
      </Panel>
    </div>
  );
}

function validateDiscountCode(form) {
  if (!form.code.trim()) return 'Mã giảm giá không được để trống.';
  if (!/^[A-Z0-9_-]+$/i.test(form.code.trim())) return 'Mã giảm giá chỉ được chứa chữ, số, dấu gạch ngang hoặc gạch dưới.';
  if (!form.name.trim()) return 'Hãy nhập tên hiển thị cho mã giảm giá.';

  const value = Number(form.value);
  const usageLimit = Number(form.usageLimit);
  if (!Number.isFinite(value) || value <= 0) return 'Giá trị giảm giá phải lớn hơn 0.';
  if (form.type === 'PERCENTAGE' && value > 100) return 'Mức giảm theo phần trăm không thể lớn hơn 100%.';
  if (!Number.isInteger(usageLimit) || usageLimit < 0) return 'Giới hạn sử dụng phải là số nguyên không âm.';

  if (form.startsAt && form.expiresAt && new Date(form.startsAt) >= new Date(form.expiresAt)) {
    return 'Thời gian hết hạn phải sau thời gian bắt đầu.';
  }
  return '';
}

function DateTimeField({ label, value, onChange }) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">{label}</span>
      <VietnameseDateTimeInput
        className="w-full rounded-2xl border border-[#dfbfbd]/65 bg-[#fcfbfb] px-4 py-3 text-sm text-[#1a1c1c] outline-none focus:border-[#730014]"
        onChange={onChange}
        value={value}
      />
    </label>
  );
}

function DiscountCodeModal({ children, onClose }) {
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
      <div className="relative z-10 w-full max-w-[600px] pointer-events-auto bg-[#fafafa] rounded-3xl border border-[#dcc0bf]/35 p-6 shadow-2xl overflow-y-auto max-h-[90vh]">
        {children}
      </div>
    </div>,
    document.body
  );
}
