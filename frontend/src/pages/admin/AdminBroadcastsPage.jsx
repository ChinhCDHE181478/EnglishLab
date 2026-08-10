import { useCallback, useEffect, useState } from 'react';
import { CalendarClock, Mail, Megaphone, Pencil, Plus, Send, X } from 'lucide-react';
import adminApi from '../../api/adminApi';
import { useAppDialog } from '../../components/ui/AppDialog';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination from '../../components/ui/Pagination';

const EMPTY_FORM = {
  title: '', message: '', targetRole: '', actionPath: '', sendInApp: true, sendEmail: false,
};
const ROLES = [
  { value: '', label: 'Tất cả người dùng' },
  { value: 'LEARNER', label: 'Học viên' },
  { value: 'TEACHER', label: 'Giáo viên' },
  { value: 'STAFF', label: 'Nhân viên đào tạo' },
  { value: 'CONTENT_MANAGER', label: 'Quản lý nội dung' },
  { value: 'MANAGER', label: 'Quản lý đào tạo' },
  { value: 'ADMIN', label: 'Quản trị viên' },
];
const STATUSES = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'DRAFT', label: 'Bản nháp' },
  { value: 'SCHEDULED', label: 'Đã hẹn giờ' },
  { value: 'SENT', label: 'Đã gửi' },
  { value: 'FAILED', label: 'Gửi lỗi' },
  { value: 'CANCELLED', label: 'Đã hủy' },
];
const STATUS_LABELS = Object.fromEntries(STATUSES.filter((item) => item.value).map((item) => [item.value, item.label]));

const formatDateTime = (value) => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : '—';
const errorMessage = (error, fallback) => error?.response?.data?.message || error?.response?.data?.error || fallback;

export default function AdminBroadcastsPage() {
  const dialog = useAppDialog();
  const [items, setItems] = useState([]);
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editor, setEditor] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [scheduleTarget, setScheduleTarget] = useState(null);
  const [scheduledAt, setScheduledAt] = useState('');
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await adminApi.getBroadcasts({ status: status || undefined, page: page - 1, size: 10 });
      setItems(data.content || []);
      setTotalPages(Math.max(1, data.totalPages || 1));
      setTotalItems(data.totalElements || 0);
      setError('');
    } catch (loadError) {
      setError(errorMessage(loadError, 'Không thể tải danh sách thông báo.'));
    } finally {
      setLoading(false);
    }
  }, [page, status]);

  useEffect(() => {
    const run = async () => { await load(); };
    run();
  }, [load]);

  const openEditor = (item) => {
    setEditor(item || {});
    setForm(item ? {
      title: item.title || '',
      message: item.message || '',
      targetRole: item.targetRole || '',
      actionPath: item.actionPath || '',
      sendInApp: item.sendInApp,
      sendEmail: item.sendEmail,
    } : EMPTY_FORM);
  };

  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      const payload = { ...form, targetRole: form.targetRole || null, actionPath: form.actionPath || null };
      if (editor?.id) await adminApi.updateBroadcast(editor.id, payload);
      else await adminApi.createBroadcast(payload);
      setEditor(null);
      await load();
    } catch (saveError) {
      await dialog.alert(errorMessage(saveError, 'Không thể lưu thông báo.'), { title: 'Lưu chưa thành công' });
    } finally {
      setSaving(false);
    }
  };

  const sendNow = async (item) => {
    const accepted = await dialog.confirm(
      `Gửi “${item.title}” ngay bây giờ? Hệ thống sẽ gửi đúng các kênh và nhóm người nhận đã cấu hình.`,
      { title: 'Xác nhận gửi thông báo', confirmLabel: 'Gửi ngay' },
    );
    if (!accepted) return;
    try {
      await adminApi.sendBroadcast(item.id);
      await load();
    } catch (sendError) {
      await dialog.alert(errorMessage(sendError, 'Không thể gửi thông báo.'), { title: 'Gửi chưa thành công' });
    }
  };

  const schedule = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      await adminApi.scheduleBroadcast(scheduleTarget.id, scheduledAt);
      setScheduleTarget(null);
      setScheduledAt('');
      await load();
    } catch (scheduleError) {
      await dialog.alert(errorMessage(scheduleError, 'Không thể hẹn giờ gửi.'), { title: 'Hẹn giờ chưa thành công' });
    } finally {
      setSaving(false);
    }
  };

  const cancel = async (item) => {
    const accepted = await dialog.confirm(`Hủy lịch gửi “${item.title}”?`, {
      title: 'Hủy lịch gửi', confirmLabel: 'Hủy lịch', tone: 'danger',
    });
    if (!accepted) return;
    try {
      await adminApi.cancelBroadcast(item.id);
      await load();
    } catch (cancelError) {
      await dialog.alert(errorMessage(cancelError, 'Không thể hủy lịch gửi.'));
    }
  };

  return (
    <div>
      <div className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8a0018]">Giao tiếp hệ thống</p>
          <h1 className="mt-1 font-['Manrope'] text-2xl font-extrabold tracking-tight text-slate-900 sm:text-3xl">Thông báo hệ thống</h1>
          <p className="mt-1.5 max-w-3xl text-sm leading-relaxed text-slate-500">Soạn bản nháp, hẹn giờ hoặc gửi ngay qua thông báo trong ứng dụng và email. Mỗi chiến dịch chỉ gửi một lần cho mỗi người nhận.</p>
        </div>
        <button className="inline-flex items-center justify-center gap-2 rounded-xl bg-[#730014] px-5 py-3 text-sm font-bold text-white shadow-lg shadow-[#730014]/15 transition hover:bg-[#56000f]" onClick={() => openEditor(null)} type="button">
          <Plus className="h-4 w-4" /> Soạn thông báo
        </button>
      </div>

      <section className="mb-5 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="max-w-xs">
          <BrandedSelect buttonClassName="rounded-xl border-slate-200 py-2.5 shadow-none" onChange={(event) => { setStatus(event.target.value); setPage(1); }} options={STATUSES} value={status} />
        </div>
      </section>

      {error ? <div className="mb-5 flex items-center justify-between rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700"><span>{error}</span><button className="font-bold underline" onClick={load} type="button">Thử lại</button></div> : null}
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        {loading ? <p className="min-h-64 p-12 text-center text-sm text-slate-500">Đang tải thông báo...</p> : null}
        {!loading && !items.length ? <div className="flex min-h-64 flex-col items-center justify-center px-5 text-center"><Megaphone className="h-10 w-10 text-[#b88a91]" /><p className="mt-4 font-bold text-slate-800">Chưa có thông báo phù hợp</p><p className="mt-1 text-sm text-slate-500">Tạo bản nháp đầu tiên để bắt đầu giao tiếp có kiểm soát.</p></div> : null}
        {!loading && items.length ? <div className="divide-y divide-slate-100">{items.map((item) => {
          const editable = ['DRAFT', 'SCHEDULED', 'FAILED'].includes(item.status);
          return <article className="p-5 transition hover:bg-[#fffafa]" key={item.id}>
            <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="rounded-full bg-[#730014]/10 px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-wider text-[#730014]">{STATUS_LABELS[item.status] || item.status}</span>
                  <span className="rounded-full bg-slate-100 px-2.5 py-1 text-[10px] font-bold text-slate-600">{ROLES.find((role) => role.value === (item.targetRole || ''))?.label}</span>
                  {item.sendInApp ? <span className="text-xs font-semibold text-slate-500">Trong ứng dụng</span> : null}
                  {item.sendEmail ? <span className="inline-flex items-center gap-1 text-xs font-semibold text-slate-500"><Mail className="h-3.5 w-3.5" /> Email</span> : null}
                </div>
                <h2 className="mt-3 text-lg font-extrabold text-slate-900">{item.title}</h2>
                <p className="mt-1 whitespace-pre-line text-sm leading-6 text-slate-600">{item.message}</p>
                <div className="mt-3 flex flex-wrap gap-x-5 gap-y-1 text-xs text-slate-500">
                  <span>Tạo: {formatDateTime(item.createdAt)}</span>
                  {item.scheduledAt ? <span>Hẹn gửi: {formatDateTime(item.scheduledAt)}</span> : null}
                  {item.sentAt ? <span>Đã gửi: {formatDateTime(item.sentAt)}</span> : null}
                  {item.status === 'SENT' ? <span>{item.recipientCount} người nhận · {item.inAppSuccessCount} thông báo · {item.emailQueuedCount} email</span> : null}
                </div>
                {item.failureReason ? <p className="mt-3 rounded-xl bg-rose-50 px-3 py-2 text-xs font-semibold text-rose-700">{item.failureReason}</p> : null}
              </div>
              <div className="flex shrink-0 flex-wrap gap-2">
                {editable ? <button className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 px-3 py-2 text-xs font-bold text-slate-700 hover:bg-slate-50" onClick={() => openEditor(item)} type="button"><Pencil className="h-3.5 w-3.5" /> Sửa</button> : null}
                {editable ? <button className="inline-flex items-center gap-1.5 rounded-xl border border-[#d9afb6] px-3 py-2 text-xs font-bold text-[#730014] hover:bg-[#fff3f5]" onClick={() => { setScheduleTarget(item); setScheduledAt(''); }} type="button"><CalendarClock className="h-3.5 w-3.5" /> Hẹn giờ</button> : null}
                {editable ? <button className="inline-flex items-center gap-1.5 rounded-xl bg-[#730014] px-3 py-2 text-xs font-bold text-white hover:bg-[#56000f]" onClick={() => sendNow(item)} type="button"><Send className="h-3.5 w-3.5" /> Gửi ngay</button> : null}
                {item.status === 'SCHEDULED' ? <button className="rounded-xl border border-rose-200 px-3 py-2 text-xs font-bold text-rose-700 hover:bg-rose-50" onClick={() => cancel(item)} type="button">Hủy lịch</button> : null}
              </div>
            </div>
          </article>;
        })}</div> : null}
      </section>
      <Pagination className="mt-5" onChange={setPage} page={page} pageSize={10} totalItems={totalItems} totalPages={totalPages} />

      {editor ? <div aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center bg-[#210006]/55 p-4 backdrop-blur-sm" role="dialog">
        <form className="max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-3xl border border-[#ead7d9] bg-white shadow-2xl" onSubmit={save}>
          <div className="sticky top-0 z-10 flex items-center justify-between border-b border-[#f0e1e3] bg-white px-6 py-5">
            <div><p className="text-xs font-extrabold uppercase tracking-[.16em] text-[#a04b59]">Thông báo hệ thống</p><h2 className="mt-1 text-xl font-extrabold text-[#3f0711]">{editor.id ? 'Chỉnh sửa bản nháp' : 'Soạn thông báo mới'}</h2></div>
            <button aria-label="Đóng" className="rounded-xl p-2 text-slate-400 hover:bg-slate-100" onClick={() => setEditor(null)} type="button"><X className="h-5 w-5" /></button>
          </div>
          <div className="space-y-5 p-6">
            <label className="block"><span className="mb-2 block text-sm font-bold text-slate-700">Tiêu đề *</span><input className="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:border-[#8a0018] focus:ring-4 focus:ring-[#8a0018]/10" maxLength={180} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} required value={form.title} /></label>
            <label className="block"><span className="mb-2 block text-sm font-bold text-slate-700">Nội dung *</span><textarea className="min-h-36 w-full resize-y rounded-xl border border-slate-200 px-4 py-3 text-sm leading-6 outline-none focus:border-[#8a0018] focus:ring-4 focus:ring-[#8a0018]/10" maxLength={4000} onChange={(event) => setForm((current) => ({ ...current, message: event.target.value }))} required value={form.message} /></label>
            <div className="grid gap-5 sm:grid-cols-2">
              <label className="block"><span className="mb-2 block text-sm font-bold text-slate-700">Nhóm người nhận</span><BrandedSelect onChange={(event) => setForm((current) => ({ ...current, targetRole: event.target.value }))} options={ROLES} value={form.targetRole} /></label>
              <label className="block"><span className="mb-2 block text-sm font-bold text-slate-700">Đường dẫn khi nhấn</span><input className="w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:border-[#8a0018]" onChange={(event) => setForm((current) => ({ ...current, actionPath: event.target.value }))} placeholder="/courses hoặc để trống" value={form.actionPath} /></label>
            </div>
            <fieldset><legend className="mb-2 text-sm font-bold text-slate-700">Kênh gửi *</legend><div className="grid gap-3 sm:grid-cols-2">{[['sendInApp', 'Thông báo trong ứng dụng'], ['sendEmail', 'Email']].map(([key, label]) => <label className="flex cursor-pointer items-center gap-3 rounded-xl border border-slate-200 p-4 hover:bg-[#fffafa]" key={key}><input checked={form[key]} className="h-4 w-4 accent-[#730014]" onChange={(event) => setForm((current) => ({ ...current, [key]: event.target.checked }))} type="checkbox" /><span className="text-sm font-bold text-slate-700">{label}</span></label>)}</div></fieldset>
          </div>
          <div className="sticky bottom-0 flex justify-end gap-3 border-t border-[#f0e1e3] bg-[#fffafa] px-6 py-4"><button className="rounded-xl border border-slate-200 px-5 py-2.5 text-sm font-bold" onClick={() => setEditor(null)} type="button">Hủy</button><button className="rounded-xl bg-[#730014] px-5 py-2.5 text-sm font-bold text-white disabled:opacity-50" disabled={saving} type="submit">{saving ? 'Đang lưu...' : 'Lưu bản nháp'}</button></div>
        </form>
      </div> : null}

      {scheduleTarget ? <div aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center bg-[#210006]/55 p-4 backdrop-blur-sm" role="dialog"><form className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl" onSubmit={schedule}><h2 className="text-xl font-extrabold text-[#3f0711]">Hẹn giờ gửi</h2><p className="mt-2 text-sm text-slate-500">{scheduleTarget.title}</p><label className="mt-5 block"><span className="mb-2 block text-sm font-bold text-slate-700">Ngày và giờ gửi *</span><input className="w-full rounded-xl border border-slate-200 px-4 py-3 outline-none focus:border-[#8a0018]" min={new Date(Date.now() + 60000).toISOString().slice(0, 16)} onChange={(event) => setScheduledAt(event.target.value)} required type="datetime-local" value={scheduledAt} /></label><div className="mt-6 flex justify-end gap-3"><button className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-bold" onClick={() => setScheduleTarget(null)} type="button">Hủy</button><button className="rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white disabled:opacity-50" disabled={saving} type="submit">Xác nhận</button></div></form></div> : null}
    </div>
  );
}
