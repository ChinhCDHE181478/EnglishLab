import { useEffect, useMemo, useState } from 'react';
import { ArrowDown, ArrowUp, Check, Package, Pencil, Plus, RefreshCw, Trash2, X } from 'lucide-react';
import packageApi from '../../api/packageApi';
import { ContentManagerLoadingState, HeaderActions, Panel, StatusBadge, TextField } from '../../components/content-manager/ContentManagerUi';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { useAppDialog } from '../../components/ui/AppDialog';

const emptyForm = {
  title: '',
  shortDescription: '',
  description: '',
  targetScore: '',
  duration: '',
  studyMode: '',
  price: '0',
  salePrice: '',
  thumbnailUrl: '',
  displayOrder: '0',
  featured: 'false',
  childPackageIds: [],
};

const STATUS_OPTIONS = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'DRAFT', label: 'Nháp' },
  { value: 'PENDING_REVIEW', label: 'Sẵn sàng xuất bản (dữ liệu cũ)' },
  { value: 'PUBLISHED', label: 'Đã xuất bản' },
  { value: 'REJECTED', label: 'Từ chối' },
  { value: 'ARCHIVED', label: 'Lưu trữ' },
];

const TYPE_FILTER_OPTIONS = [
  { value: '', label: 'Tất cả loại gói' },
  { value: 'BUNDLE', label: 'Bundle' },
  { value: 'ONLINE_COURSE', label: 'Khóa online' },
  { value: 'CLASSROOM', label: 'Lớp học' },
  { value: 'MOCK_TEST', label: 'Mock test' },
  { value: 'SUBSCRIPTION', label: 'Subscription' },
];

const money = (value) => {
  const amount = Number(value || 0);
  return Number.isFinite(amount)
    ? amount.toLocaleString('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
    : '—';
};

export default function ContentManagerPackagesPage() {
  const { confirm: confirmDialog } = useAppDialog();
  const [packages, setPackages] = useState([]);
  const [candidates, setCandidates] = useState([]);
  const [packageTypes, setPackageTypes] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [keyword, setKeyword] = useState('');
  const [typeFilter, setTypeFilter] = useState('BUNDLE');
  const [statusFilter, setStatusFilter] = useState('');

  const candidateMap = useMemo(
    () => Object.fromEntries(candidates.map((item) => [String(item.id), item])),
    [candidates]
  );

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [page, candidateList, types] = await Promise.all([
        packageApi.listPackages({
          keyword: keyword.trim() || undefined,
          packageTypeCode: typeFilter || undefined,
          status: statusFilter || undefined,
          page: 0,
          size: 50,
        }),
        packageApi.getBundleCandidates(),
        packageApi.getPackageTypes(),
      ]);
      setPackages(page.content || []);
      setCandidates(candidateList);
      setPackageTypes(types);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách gói học.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const openCreate = () => {
    setEditingId(null);
    setForm(emptyForm);
    setEditorOpen(true);
    setError('');
    setSuccess('');
  };

  const openEdit = (item) => {
    if (item.packageTypeCode !== 'BUNDLE') {
      setError('Chỉ chỉnh sửa gói BUNDLE tại đây. Khóa online/lớp học dùng màn hình riêng.');
      return;
    }
    setEditingId(item.id);
    setForm({
      title: item.title || '',
      shortDescription: item.shortDescription || '',
      description: item.description || '',
      targetScore: item.targetScore || '',
      duration: item.duration || '',
      studyMode: item.studyMode || '',
      price: String(item.price ?? 0),
      salePrice: item.salePrice == null ? '' : String(item.salePrice),
      thumbnailUrl: item.thumbnailUrl || '',
      displayOrder: String(item.displayOrder ?? 0),
      featured: String(Boolean(item.featured)),
      childPackageIds: (item.childPackages || []).map((child) => child.id),
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

  const toggleChild = (childId) => {
    setForm((current) => {
      const exists = current.childPackageIds.includes(childId);
      return {
        ...current,
        childPackageIds: exists
          ? current.childPackageIds.filter((id) => id !== childId)
          : [...current.childPackageIds, childId],
      };
    });
  };

  const moveChild = (childId, direction) => {
    setForm((current) => {
      const ids = [...current.childPackageIds];
      const index = ids.indexOf(childId);
      if (index < 0) return current;
      const target = index + direction;
      if (target < 0 || target >= ids.length) return current;
      [ids[index], ids[target]] = [ids[target], ids[index]];
      return { ...current, childPackageIds: ids };
    });
  };

  const buildPayload = () => ({
    packageTypeCode: 'BUNDLE',
    title: form.title.trim(),
    shortDescription: form.shortDescription.trim() || null,
    description: form.description.trim() || null,
    targetScore: form.targetScore.trim() || null,
    duration: form.duration.trim() || null,
    studyMode: form.studyMode.trim() || null,
    price: Number(form.price || 0),
    salePrice: form.salePrice === '' ? null : Number(form.salePrice),
    thumbnailUrl: form.thumbnailUrl.trim() || null,
    displayOrder: Number(form.displayOrder || 0),
    featured: form.featured === 'true',
    childPackageIds: form.childPackageIds,
  });

  const saveBundle = async () => {
    if (!form.title.trim()) {
      setError('Hãy nhập tên gói bundle.');
      return;
    }
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      if (editingId) {
        await packageApi.updateBundle(editingId, buildPayload());
        setSuccess('Đã cập nhật gói bundle.');
      } else {
        await packageApi.createBundle(buildPayload());
        setSuccess('Đã tạo gói bundle.');
      }
      closeEditor();
      await loadData();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được gói bundle.');
    } finally {
      setSaving(false);
    }
  };

  const runLifecycle = async (item, action) => {
    setError('');
    setSuccess('');
    try {
      if (action === 'publish') await packageApi.publishBundle(item.id);
      if (action === 'archive') await packageApi.archiveBundle(item.id);
      if (action === 'delete') {
        if (!await confirmDialog(`Xóa gói “${item.title}”?`, {
          title: 'Xóa gói học',
          confirmLabel: 'Xóa gói',
          tone: 'danger',
        })) return;
        await packageApi.deleteBundle(item.id);
      }
      setSuccess('Đã cập nhật trạng thái gói.');
      await loadData();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không cập nhật được trạng thái gói.');
    }
  };

  if (loading && !packages.length) {
    return <ContentManagerLoadingState message="Đang tải danh sách gói học..." />;
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
          Tạo bundle
        </button>
      </HeaderActions>

      {error ? <Notice tone="error">{error}</Notice> : null}
      {success ? <Notice tone="success">{success}</Notice> : null}

      <Panel className="p-5">
        <div className="grid gap-3 md:grid-cols-4">
          <TextField
            label="Từ khóa"
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="Tên hoặc slug..."
            value={keyword}
          />
          <Field label="Loại gói">
            <BrandedSelect
              onChange={(value) => setTypeFilter(value)}
              options={TYPE_FILTER_OPTIONS}
              value={typeFilter}
            />
          </Field>
          <Field label="Trạng thái">
            <BrandedSelect
              onChange={(value) => setStatusFilter(value)}
              options={STATUS_OPTIONS}
              value={statusFilter}
            />
          </Field>
          <div className="flex items-end">
            <button
              className="w-full rounded-2xl border border-[#dfbfbd]/70 bg-white px-4 py-3 text-sm font-bold text-[#730014]"
              onClick={loadData}
              type="button"
            >
              Lọc danh sách
            </button>
          </div>
        </div>
        {packageTypes.length > 0 ? (
          <p className="mt-3 text-xs leading-5 text-[#8b706e]">
            Loại gói hệ thống: {packageTypes.map((type) => type.code).join(' · ')}. Chỉ tạo/sửa loại <strong>BUNDLE</strong> tại đây.
          </p>
        ) : null}
      </Panel>

      {editorOpen ? (
        <Panel className="p-6">
          <div className="mb-5 flex items-center justify-between gap-4">
            <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">
              {editingId ? 'Chỉnh sửa bundle' : 'Tạo bundle mới'}
            </h2>
            <button className="rounded-xl p-2 text-[#730014] hover:bg-[#fff2f3]" onClick={closeEditor} type="button">
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <TextField
              label="Tên gói"
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
              value={form.title}
            />
            <TextField
              label="Giá (VND)"
              onChange={(event) => setForm((current) => ({ ...current, price: event.target.value }))}
              type="number"
              value={form.price}
            />
            <TextField
              label="Giá khuyến mãi"
              onChange={(event) => setForm((current) => ({ ...current, salePrice: event.target.value }))}
              type="number"
              value={form.salePrice}
            />
            <TextField
              label="Target score"
              onChange={(event) => setForm((current) => ({ ...current, targetScore: event.target.value }))}
              value={form.targetScore}
            />
            <TextField
              label="Thời lượng"
              onChange={(event) => setForm((current) => ({ ...current, duration: event.target.value }))}
              value={form.duration}
            />
            <TextField
              label="Hình thức học"
              onChange={(event) => setForm((current) => ({ ...current, studyMode: event.target.value }))}
              value={form.studyMode}
            />
            <TextField
              label="Thứ tự hiển thị"
              onChange={(event) => setForm((current) => ({ ...current, displayOrder: event.target.value }))}
              type="number"
              value={form.displayOrder}
            />
            <TextField
              label="Thumbnail URL"
              onChange={(event) => setForm((current) => ({ ...current, thumbnailUrl: event.target.value }))}
              value={form.thumbnailUrl}
            />
            <Field label="Nổi bật">
              <BrandedSelect
                onChange={(value) => setForm((current) => ({ ...current, featured: value }))}
                options={[
                  { value: 'false', label: 'Không' },
                  { value: 'true', label: 'Có' },
                ]}
                value={form.featured}
              />
            </Field>
          </div>

          <div className="mt-4 grid gap-4">
            <RichTextEditor
              label="Mô tả ngắn"
              onChange={(html) => setForm((current) => ({ ...current, shortDescription: html }))}
              placeholder="Tóm tắt gói học..."
              size="compact"
              value={form.shortDescription}
            />
            <RichTextEditor
              label="Mô tả chi tiết"
              onChange={(html) => setForm((current) => ({ ...current, description: html }))}
              placeholder="Mô tả chi tiết nội dung gói, thành phần, lợi ích..."
              size="form"
              value={form.description}
            />
          </div>

          <div className="mt-6">
            <h3 className="mb-3 font-['Manrope'] text-base font-extrabold text-[#4b0009]">
              Thành phần bundle ({form.childPackageIds.length})
            </h3>
            <div className="mb-4 space-y-2">
              {form.childPackageIds.length === 0 ? (
                <p className="text-sm text-[#8b706e]">Chưa chọn sản phẩm con. Chọn khóa online hoặc lớp học bên dưới.</p>
              ) : (
                form.childPackageIds.map((childId, index) => {
                  const child = candidateMap[String(childId)];
                  return (
                    <div
                      key={childId}
                      className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-[#dfbfbd]/55 bg-[#fcfbfb] px-4 py-3"
                    >
                      <div>
                        <p className="text-sm font-bold text-[#1a1c1c]">
                          #{index + 1}. {child?.title || `Gói #${childId}`}
                        </p>
                        <p className="text-xs text-[#8b706e]">
                          {child?.packageTypeCode || '—'} · {child?.status || '—'} · {money(child?.salePrice ?? child?.price)}
                        </p>
                      </div>
                      <div className="flex items-center gap-2">
                        <button className="rounded-xl border border-[#dfbfbd]/70 p-2" onClick={() => moveChild(childId, -1)} type="button">
                          <ArrowUp className="h-4 w-4" />
                        </button>
                        <button className="rounded-xl border border-[#dfbfbd]/70 p-2" onClick={() => moveChild(childId, 1)} type="button">
                          <ArrowDown className="h-4 w-4" />
                        </button>
                        <button className="rounded-xl border border-[#dfbfbd]/70 p-2 text-[#730014]" onClick={() => toggleChild(childId)} type="button">
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            <div className="max-h-64 space-y-2 overflow-y-auto rounded-2xl border border-[#dfbfbd]/45 p-3">
              {candidates.length === 0 ? (
                <p className="text-sm text-[#8b706e]">Chưa có khóa online/lớp học để ghép vào bundle.</p>
              ) : (
                candidates.map((candidate) => {
                  const selected = form.childPackageIds.includes(candidate.id);
                  return (
                    <button
                      key={candidate.id}
                      className={`flex w-full items-center justify-between gap-3 rounded-xl px-3 py-2 text-left text-sm ${
                        selected ? 'bg-[#fff2f3] font-bold text-[#730014]' : 'hover:bg-[#fcfbfb]'
                      }`}
                      onClick={() => toggleChild(candidate.id)}
                      type="button"
                    >
                      <span>
                        {candidate.title}
                        <span className="ml-2 text-xs font-semibold text-[#8b706e]">
                          {candidate.packageTypeCode} · {candidate.status}
                        </span>
                      </span>
                      {selected ? <Check className="h-4 w-4" /> : <Plus className="h-4 w-4 text-[#8b706e]" />}
                    </button>
                  );
                })
              )}
            </div>
          </div>

          <div className="mt-6 flex justify-end gap-3">
            <button
              className="rounded-2xl border border-[#dfbfbd]/70 px-4 py-3 text-sm font-bold text-[#730014]"
              onClick={closeEditor}
              type="button"
            >
              Hủy
            </button>
            <button
              className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white disabled:opacity-50"
              disabled={saving}
              onClick={saveBundle}
              type="button"
            >
              <Check className="h-4 w-4" />
              {saving ? 'Đang lưu...' : 'Lưu bundle'}
            </button>
          </div>
        </Panel>
      ) : null}

      <Panel className="overflow-hidden p-0">
        <div className="overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-[#fff2f3] text-xs uppercase tracking-wide text-[#730014]">
              <tr>
                <th className="px-4 py-3">Gói</th>
                <th className="px-4 py-3">Loại</th>
                <th className="px-4 py-3">Giá</th>
                <th className="px-4 py-3">Trạng thái</th>
                <th className="px-4 py-3">Thành phần</th>
                <th className="px-4 py-3 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {packages.length === 0 ? (
                <tr>
                  <td className="px-4 py-8 text-center text-[#8b706e]" colSpan={6}>
                    Chưa có gói phù hợp bộ lọc.
                  </td>
                </tr>
              ) : (
                packages.map((item) => (
                  <tr key={item.id} className="border-t border-[#f0e2e1]">
                    <td className="px-4 py-3">
                      <div className="flex items-start gap-2">
                        <Package className="mt-0.5 h-4 w-4 text-[#730014]" />
                        <div>
                          <p className="font-bold text-[#1a1c1c]">{item.title}</p>
                          <p className="text-xs text-[#8b706e]">{item.slug}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 font-semibold text-[#4b0009]">{item.packageTypeCode}</td>
                    <td className="px-4 py-3">{money(item.salePrice ?? item.price)}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={item.status} />
                    </td>
                    <td className="px-4 py-3">
                      {item.packageTypeCode === 'BUNDLE'
                        ? `${item.childCount ?? item.childPackages?.length ?? 0} sản phẩm`
                        : '—'}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right">
                      <div className="inline-flex items-center justify-end gap-2">
                        {item.packageTypeCode === 'BUNDLE' ? (
                          <>
                            <button
                              className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#dcc0bf]/50 bg-white px-3 text-xs font-bold text-[#4b0009] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95"
                              onClick={() => openEdit(item)}
                              type="button"
                            >
                              <Pencil className="h-3.5 w-3.5" />
                              Sửa
                            </button>
                            {(item.status === 'DRAFT' || item.status === 'PENDING_REVIEW' || item.status === 'REJECTED') && (
                              <button
                                className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg bg-[#730014] px-3 text-xs font-bold text-white whitespace-nowrap transition hover:bg-[#8a0018] active:scale-95"
                                onClick={() => runLifecycle(item, 'publish')}
                                type="button"
                              >
                                <Check className="h-3.5 w-3.5" />
                                Xuất bản
                              </button>
                            )}
                            {item.status !== 'ARCHIVED' && (
                              <button
                                className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-[#fffafb] px-3 text-xs font-bold text-[#730014] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95"
                                onClick={() => runLifecycle(item, 'archive')}
                                type="button"
                              >
                                <Archive className="h-3.5 w-3.5" />
                                Lưu trữ
                              </button>
                            )}
                            <button
                              aria-label="Xóa"
                              className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-rose-200 bg-white text-rose-700 transition hover:bg-rose-50 active:scale-95"
                              onClick={() => runLifecycle(item, 'delete')}
                              title="Xóa"
                              type="button"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </button>
                          </>
                        ) : (
                          <span className="text-xs text-[#8b706e]">Chỉ xem</span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
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
    <label className="block space-y-2">
      <span className="text-xs font-bold uppercase tracking-wide text-[#8b706e]">{label}</span>
      {children}
    </label>
  );
}

function Notice({ tone, children }) {
  const styles = tone === 'error'
    ? 'border-[#f3b4b0] bg-[#fff5f5] text-[#8a1c1c]'
    : 'border-[#b7e0c2] bg-[#f3fff6] text-[#1f6b3a]';
  return <div className={`rounded-2xl border px-4 py-3 text-sm font-semibold ${styles}`}>{children}</div>;
}

function IconButton({ label, onClick, children }) {
  return (
    <button
      aria-label={label}
      className="rounded-xl border border-[#dfbfbd]/70 p-2 text-[#730014] hover:bg-[#fff2f3]"
      onClick={onClick}
      type="button"
    >
      {children}
    </button>
  );
}
