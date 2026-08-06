import { useCallback, useEffect, useRef, useState } from 'react';
import { ArchiveRestore, DatabaseBackup, Download, FileArchive, HardDrive, RefreshCw, ShieldAlert, Trash2, Upload, X } from 'lucide-react';
import adminApi from '../../api/adminApi';
import { useAppDialog } from '../../components/ui/AppDialog';
import Pagination from '../../components/ui/Pagination';

const STATUS_LABELS = {
  CREATING: 'Đang tạo', READY: 'Sẵn sàng', FAILED: 'Thất bại', RESTORING: 'Đang phục hồi',
  RESTORED: 'Đã phục hồi', DELETED: 'Đã xóa',
};
const formatBytes = (bytes = 0) => {
  if (!bytes) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / (1024 ** index)).toLocaleString('vi-VN', { maximumFractionDigits: 2 })} ${units[index]}`;
};
const formatDateTime = (value) => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : '—';
const errorMessage = (error, fallback) => error?.response?.data?.message || error?.response?.data?.error || fallback;

export default function AdminBackupsPage() {
  const dialog = useAppDialog();
  const downloadAnchor = useRef(null);
  const [capabilities, setCapabilities] = useState(null);
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [restoreOpen, setRestoreOpen] = useState(false);
  const [restoreFile, setRestoreFile] = useState(null);
  const [confirmation, setConfirmation] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [capabilityData, backupData] = await Promise.all([
        adminApi.getBackupCapabilities(),
        adminApi.getBackups({ page: page - 1, size: 10 }),
      ]);
      setCapabilities(capabilityData);
      setItems(backupData.content || []);
      setTotalPages(Math.max(1, backupData.totalPages || 1));
      setTotalItems(backupData.totalElements || 0);
      setError('');
    } catch (loadError) {
      setError(errorMessage(loadError, 'Không thể tải trạng thái sao lưu.'));
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    const run = async () => { await load(); };
    run();
  }, [load]);

  const createBackup = async () => {
    const accepted = await dialog.confirm('Tạo một bản sao lưu PostgreSQL mới? Thao tác có thể mất vài phút tùy dung lượng dữ liệu.', {
      title: 'Tạo bản sao lưu', confirmLabel: 'Bắt đầu sao lưu',
    });
    if (!accepted) return;
    setWorking(true);
    try {
      const result = await adminApi.createBackup();
      if (result.status === 'FAILED') {
        await dialog.alert(result.failureReason || 'Công cụ sao lưu trả về lỗi.', { title: 'Sao lưu chưa thành công' });
      }
      await load();
    } catch (createError) {
      await dialog.alert(errorMessage(createError, 'Không thể tạo bản sao lưu.'), { title: 'Sao lưu chưa thành công' });
    } finally {
      setWorking(false);
    }
  };

  const download = async (item) => {
    try {
      const response = await adminApi.downloadBackup(item.id);
      const url = URL.createObjectURL(response.data);
      const anchor = downloadAnchor.current;
      anchor.href = url;
      anchor.download = item.fileName;
      anchor.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (downloadError) {
      await dialog.alert(errorMessage(downloadError, 'Không thể tải tệp sao lưu.'));
    }
  };

  const remove = async (item) => {
    const accepted = await dialog.confirm(
      `Xóa vĩnh viễn tệp “${item.fileName}” khỏi máy chủ? Bản ghi lịch sử vẫn được giữ lại nhưng tệp không thể khôi phục.`,
      { title: 'Xóa tệp sao lưu', confirmLabel: 'Xóa tệp', tone: 'danger' },
    );
    if (!accepted) return;
    try {
      await adminApi.deleteBackup(item.id);
      await load();
    } catch (deleteError) {
      await dialog.alert(errorMessage(deleteError, 'Không thể xóa tệp sao lưu.'));
    }
  };

  const restore = async (event) => {
    event.preventDefault();
    setWorking(true);
    try {
      const result = await adminApi.restoreBackup(restoreFile, confirmation);
      setRestoreOpen(false);
      setRestoreFile(null);
      setConfirmation('');
      await load();
      if (result.status === 'RESTORED') {
        await dialog.alert('Dữ liệu đã được phục hồi. Hãy khởi động lại backend trước khi tiếp tục sử dụng để làm mới toàn bộ cache và kết nối.', { title: 'Phục hồi hoàn tất' });
      } else {
        await dialog.alert(result.failureReason || 'Phục hồi chưa hoàn tất.', { title: 'Phục hồi thất bại' });
      }
    } catch (restoreError) {
      await dialog.alert(errorMessage(restoreError, 'Không thể phục hồi dữ liệu.'), { title: 'Phục hồi thất bại' });
    } finally {
      setWorking(false);
    }
  };

  return <div>
    <a className="hidden" ref={downloadAnchor}>Tải xuống</a>
    <div className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div><p className="text-xs font-bold uppercase tracking-[.18em] text-[#8a0018]">An toàn dữ liệu</p><h1 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-slate-900">Sao lưu & phục hồi</h1><p className="mt-2 max-w-3xl text-sm text-slate-500">Bản sao lưu PostgreSQL custom có checksum SHA-256, lưu ngoài source code và chỉ quản trị viên được thao tác.</p></div>
      <div className="flex flex-wrap gap-2"><button className="inline-flex items-center gap-2 rounded-xl border border-[#d9afb6] bg-white px-4 py-2.5 text-sm font-bold text-[#730014] hover:bg-[#fffafa] disabled:cursor-not-allowed disabled:opacity-50" disabled={working || !capabilities?.restoreAvailable} onClick={() => setRestoreOpen(true)} type="button"><Upload className="h-4 w-4" /> Phục hồi</button><button className="inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white shadow-lg shadow-[#730014]/15 hover:bg-[#56000f] disabled:cursor-not-allowed disabled:opacity-50" disabled={working || !capabilities?.backupAvailable} onClick={createBackup} type="button"><DatabaseBackup className="h-4 w-4" /> {working ? 'Đang xử lý...' : 'Tạo bản sao lưu'}</button></div>
    </div>
    {error ? <div className="mb-5 flex items-center justify-between rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700"><span>{error}</span><button className="font-bold underline" onClick={load} type="button">Thử lại</button></div> : null}
    {capabilities ? <section className={`mb-5 rounded-2xl border p-5 ${capabilities.backupAvailable && capabilities.restoreAvailable ? 'border-emerald-200 bg-emerald-50/60' : 'border-amber-200 bg-amber-50'}`}>
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between"><div className="flex items-start gap-3">{capabilities.backupAvailable ? <HardDrive className="mt-0.5 h-5 w-5 text-emerald-700" /> : <ShieldAlert className="mt-0.5 h-5 w-5 text-amber-700" />}<div><h2 className="font-extrabold text-slate-900">{capabilities.backupAvailable ? 'Công cụ PostgreSQL đã sẵn sàng' : 'Máy chủ chưa sẵn sàng sao lưu'}</h2><p className="mt-1 text-sm text-slate-600">{capabilities.backupAvailable ? `${capabilities.pgDumpVersion}${capabilities.pgRestoreVersion ? ` · ${capabilities.pgRestoreVersion}` : ''}` : 'Cài pg_dump và pg_restore hoặc cấu hình đường dẫn công cụ trong biến môi trường backend.'}</p></div></div><button className="inline-flex items-center gap-2 self-start rounded-xl border border-current/15 bg-white/70 px-3 py-2 text-xs font-bold text-slate-700" onClick={load} type="button"><RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} /> Kiểm tra lại</button></div>
    </section> : null}
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      {loading ? <p className="min-h-64 p-12 text-center text-sm text-slate-500">Đang tải lịch sử sao lưu...</p> : null}
      {!loading && !items.length ? <div className="flex min-h-64 flex-col items-center justify-center text-center"><FileArchive className="h-11 w-11 text-[#b88a91]" /><p className="mt-4 font-bold text-slate-800">Chưa có bản sao lưu</p><p className="mt-1 text-sm text-slate-500">Bản sao lưu đã tạo sẽ xuất hiện tại đây cùng checksum và lịch sử thao tác.</p></div> : null}
      {!loading && items.length ? <div className="overflow-x-auto"><table className="w-full min-w-[980px] text-left"><thead className="bg-[#fff8f8] text-[10px] font-extrabold uppercase tracking-wider text-slate-500"><tr><th className="p-4">Tệp</th><th className="p-4">Trạng thái</th><th className="p-4">Dung lượng</th><th className="p-4">Checksum SHA-256</th><th className="p-4">Người tạo / thời gian</th><th className="p-4 text-right">Thao tác</th></tr></thead><tbody className="divide-y divide-slate-100">{items.map((item) => <tr className="hover:bg-[#fffafa]" key={item.id}><td className="p-4"><p className="font-mono text-xs font-bold text-slate-800">{item.fileName}</p>{item.failureReason ? <p className="mt-1 max-w-sm text-xs text-rose-700">{item.failureReason}</p> : null}</td><td className="p-4"><span className={`rounded-full px-2.5 py-1 text-[10px] font-extrabold uppercase ${item.status === 'READY' || item.status === 'RESTORED' ? 'bg-emerald-100 text-emerald-700' : item.status === 'FAILED' ? 'bg-rose-100 text-rose-700' : 'bg-slate-100 text-slate-600'}`}>{STATUS_LABELS[item.status] || item.status}</span></td><td className="p-4 text-sm font-semibold text-slate-600">{formatBytes(item.fileSizeBytes)}</td><td className="p-4"><p className="max-w-[230px] truncate font-mono text-[10px] text-slate-500" title={item.sha256}>{item.sha256 || '—'}</p></td><td className="p-4 text-xs text-slate-600"><p className="font-semibold">{item.createdBy}</p><p className="mt-1">{formatDateTime(item.createdAt)}</p></td><td className="p-4"><div className="flex justify-end gap-2">{item.downloadable ? <button aria-label="Tải xuống" className="rounded-lg border border-slate-200 p-2 text-[#730014] hover:bg-[#fff3f5]" onClick={() => download(item)} title="Tải xuống" type="button"><Download className="h-4 w-4" /></button> : null}{item.status !== 'DELETED' && !['CREATING', 'RESTORING'].includes(item.status) ? <button aria-label="Xóa tệp" className="rounded-lg border border-rose-200 p-2 text-rose-700 hover:bg-rose-50" onClick={() => remove(item)} title="Xóa tệp" type="button"><Trash2 className="h-4 w-4" /></button> : null}</div></td></tr>)}</tbody></table></div> : null}
    </section>
    <Pagination className="mt-5" onChange={setPage} page={page} pageSize={10} totalItems={totalItems} totalPages={totalPages} />
    {restoreOpen ? <div aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center bg-[#210006]/65 p-4 backdrop-blur-sm" role="dialog"><form className="max-h-[92vh] w-full max-w-xl overflow-y-auto rounded-3xl bg-white shadow-2xl" onSubmit={restore}><div className="flex items-start justify-between border-b border-rose-100 p-6"><div className="flex gap-3"><span className="rounded-2xl bg-rose-100 p-3 text-rose-700"><ArchiveRestore className="h-5 w-5" /></span><div><p className="text-xs font-extrabold uppercase tracking-wider text-rose-700">Thao tác rủi ro cao</p><h2 className="mt-1 text-xl font-extrabold text-slate-900">Phục hồi cơ sở dữ liệu</h2></div></div><button aria-label="Đóng" className="rounded-xl p-2 text-slate-400 hover:bg-slate-100" onClick={() => setRestoreOpen(false)} type="button"><X className="h-5 w-5" /></button></div><div className="space-y-5 p-6"><div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-900"><strong>Hệ thống sẽ tự tạo một bản sao lưu an toàn trước khi phục hồi.</strong> Dữ liệu hiện tại sau thời điểm của tệp tải lên có thể bị mất. Không đóng backend trong lúc xử lý và phải khởi động lại backend sau khi hoàn tất.</div><label className="block"><span className="mb-2 block text-sm font-bold text-slate-700">Tệp PostgreSQL custom (.backup) *</span><input accept=".backup,application/octet-stream" className="block w-full rounded-xl border border-slate-200 p-3 text-sm file:mr-4 file:rounded-lg file:border-0 file:bg-[#730014] file:px-3 file:py-2 file:font-bold file:text-white" onChange={(event) => setRestoreFile(event.target.files?.[0] || null)} required type="file" /></label><label className="block"><span className="mb-2 block text-sm font-bold text-slate-700">Nhập chính xác “{capabilities?.restoreConfirmationPhrase}” *</span><input autoComplete="off" className="w-full rounded-xl border border-rose-200 px-4 py-3 font-mono text-sm outline-none focus:border-rose-600 focus:ring-4 focus:ring-rose-600/10" onChange={(event) => setConfirmation(event.target.value)} required value={confirmation} /></label></div><div className="flex justify-end gap-3 border-t border-slate-100 bg-slate-50 p-5"><button className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-bold" onClick={() => setRestoreOpen(false)} type="button">Hủy</button><button className="rounded-xl bg-rose-700 px-4 py-2.5 text-sm font-bold text-white disabled:cursor-not-allowed disabled:opacity-40" disabled={working || !restoreFile || confirmation !== capabilities?.restoreConfirmationPhrase} type="submit">{working ? 'Đang phục hồi...' : 'Phục hồi dữ liệu'}</button></div></form></div> : null}
  </div>;
}
