import { useCallback, useEffect, useState } from 'react';
import { Activity, Clock3, Database, Gauge, RefreshCw, Server, TriangleAlert } from 'lucide-react';
import adminApi from '../../api/adminApi';

const formatBytes = (bytes = 0) => {
  if (!bytes) return '0 MB';
  return `${(bytes / 1024 / 1024).toLocaleString('vi-VN', { maximumFractionDigits: 1 })} MB`;
};
const formatUptime = (seconds = 0) => {
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  return `${days ? `${days} ngày ` : ''}${hours} giờ ${minutes} phút`;
};
const formatDateTime = (value) => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'medium' }).format(new Date(value))
  : '—';

function StatusPill({ value }) {
  const healthy = value === 'UP';
  return <span className={`rounded-full px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-wider ${healthy ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-800'}`}>{value}</span>;
}

export default function AdminMonitoringPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async ({ quiet = false } = {}) => {
    if (!quiet) setLoading(true);
    try {
      setData(await adminApi.getApiMonitoring());
      setError('');
    } catch (loadError) {
      setError(loadError?.response?.data?.message || 'Không thể tải số liệu giám sát API.');
    } finally {
      if (!quiet) setLoading(false);
    }
  }, []);

  useEffect(() => {
    const run = async () => { await load(); };
    run();
    const timer = window.setInterval(() => { load({ quiet: true }); }, 30000);
    return () => window.clearInterval(timer);
  }, [load]);

  const heapPercent = data?.maximumHeapBytes ? Math.min(100, data.usedHeapBytes * 100 / data.maximumHeapBytes) : 0;

  return <div>
    <div className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div><p className="text-xs font-bold uppercase tracking-[.18em] text-[#8a0018]">Vận hành kỹ thuật</p><h1 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-slate-900">Giám sát API</h1><p className="mt-2 text-sm text-slate-500">Theo dõi sức khỏe ứng dụng, cơ sở dữ liệu, độ trễ và lỗi HTTP mà không lưu nội dung yêu cầu.</p></div>
      <button className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-bold text-[#730014] shadow-sm hover:bg-[#fffafa] disabled:opacity-50" disabled={loading} onClick={() => load()} type="button"><RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /> Làm mới</button>
    </div>
    {error ? <div className="mb-5 flex items-center justify-between rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700"><span>{error}</span><button className="font-bold underline" onClick={() => load()} type="button">Thử lại</button></div> : null}
    {loading && !data ? <div className="min-h-96 rounded-2xl border border-slate-200 bg-white p-16 text-center text-sm text-slate-500">Đang đọc trạng thái hệ thống...</div> : null}
    {data ? <>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          { icon: Server, label: 'Ứng dụng', value: <StatusPill value={data.applicationStatus} />, detail: `Uptime ${formatUptime(data.uptimeSeconds)}` },
          { icon: Database, label: 'Cơ sở dữ liệu', value: <StatusPill value={data.databaseStatus} />, detail: `${data.databaseLatencyMs} ms cho health check` },
          { icon: Activity, label: 'Request đã đo', value: Number(data.totalRequests || 0).toLocaleString('vi-VN'), detail: `${Number(data.totalErrors || 0).toLocaleString('vi-VN')} request lỗi` },
          { icon: Gauge, label: 'Độ trễ trung bình', value: `${data.averageLatencyMs || 0} ms`, detail: `Cao nhất ${data.maximumLatencyMs || 0} ms` },
        ].map(({ icon: Icon, label, value, detail }) => <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm" key={label}><div className="flex items-center justify-between"><span className="text-xs font-extrabold uppercase tracking-wider text-slate-500">{label}</span><span className="rounded-xl bg-[#fff1f3] p-2 text-[#730014]"><Icon className="h-4 w-4" /></span></div><div className="mt-4 text-2xl font-black text-slate-900">{value}</div><p className="mt-2 text-xs text-slate-500">{detail}</p></article>)}
      </div>
      <div className="mt-5 grid gap-5 xl:grid-cols-[1.5fr_1fr]">
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-100 p-5"><h2 className="text-lg font-extrabold text-slate-900">Endpoint có lưu lượng cao</h2><p className="mt-1 text-xs text-slate-500">Đường dẫn có ID được gom nhóm để số liệu không phân mảnh.</p></div>
          <div className="overflow-x-auto"><table className="w-full min-w-[680px] text-left"><thead className="bg-[#fff8f8] text-[10px] font-extrabold uppercase tracking-wider text-slate-500"><tr><th className="p-4">Phương thức</th><th className="p-4">Endpoint</th><th className="p-4 text-right">Request</th><th className="p-4 text-right">Lỗi</th><th className="p-4 text-right">Trung bình</th><th className="p-4 text-right">Cao nhất</th></tr></thead><tbody className="divide-y divide-slate-100">{(data.busiestRoutes || []).map((route) => <tr className="text-sm hover:bg-[#fffafa]" key={`${route.method}-${route.route}`}><td className="p-4"><span className="rounded-lg bg-slate-100 px-2 py-1 font-mono text-xs font-bold text-slate-700">{route.method}</span></td><td className="p-4 font-mono text-xs font-semibold text-slate-700">{route.route}</td><td className="p-4 text-right font-bold">{route.requestCount}</td><td className={`p-4 text-right font-bold ${route.errorCount ? 'text-rose-700' : 'text-slate-500'}`}>{route.errorCount} ({route.errorRatePercent}%)</td><td className="p-4 text-right">{route.averageLatencyMs} ms</td><td className="p-4 text-right">{route.maximumLatencyMs} ms</td></tr>)}</tbody></table>{!data.busiestRoutes?.length ? <p className="p-10 text-center text-sm text-slate-500">Chưa có request nào được ghi nhận từ lần khởi động này.</p> : null}</div>
        </section>
        <div className="space-y-5">
          <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><div className="flex items-center gap-2"><Clock3 className="h-4 w-4 text-[#730014]" /><h2 className="font-extrabold text-slate-900">Tài nguyên JVM</h2></div><div className="mt-5 flex items-end justify-between"><span className="text-2xl font-black text-slate-900">{formatBytes(data.usedHeapBytes)}</span><span className="text-xs text-slate-500">/ {formatBytes(data.maximumHeapBytes)}</span></div><div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-100"><div className={`h-full rounded-full ${heapPercent > 85 ? 'bg-rose-600' : 'bg-[#730014]'}`} style={{ width: `${heapPercent}%` }} /></div><p className="mt-3 text-xs text-slate-500">{heapPercent.toLocaleString('vi-VN', { maximumFractionDigits: 1 })}% heap · {data.availableProcessors} CPU logic</p></section>
          <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"><div className="flex items-center gap-2 border-b border-slate-100 p-5"><TriangleAlert className="h-4 w-4 text-amber-600" /><h2 className="font-extrabold text-slate-900">Lỗi gần nhất</h2></div><div className="max-h-[420px] divide-y divide-slate-100 overflow-y-auto">{(data.recentFailures || []).map((failure, index) => <article className="p-4" key={`${failure.occurredAt}-${index}`}><div className="flex items-center justify-between gap-3"><span className="font-mono text-xs font-bold text-rose-700">{failure.status} · {failure.method}</span><span className="text-[10px] text-slate-400">{formatDateTime(failure.occurredAt)}</span></div><p className="mt-1 break-all font-mono text-xs text-slate-700">{failure.route}</p><p className="mt-1 text-[10px] text-slate-400">{failure.durationMs} ms{failure.correlationId ? ` · ${failure.correlationId}` : ''}</p></article>)}{!data.recentFailures?.length ? <p className="p-8 text-center text-sm text-slate-500">Chưa ghi nhận lỗi HTTP.</p> : null}</div></section>
        </div>
      </div>
      <p className="mt-4 text-right text-xs text-slate-400">{data.measurementScope} Cập nhật lúc {formatDateTime(data.measuredAt)}.</p>
    </> : null}
  </div>;
}
