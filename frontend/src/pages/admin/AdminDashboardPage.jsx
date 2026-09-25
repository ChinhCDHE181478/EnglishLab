import { useEffect, useState } from 'react';
import { ArrowRight, ClipboardList, GraduationCap, Settings, ShieldCheck, Users, UserRoundCheck } from 'lucide-react';
import { Link } from 'react-router-dom';
import adminApi from '../../api/adminApi';

export default function AdminDashboardPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      setData(await adminApi.getDashboard());
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được bảng điều khiển quản trị.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const cards = [
    { label: 'Tổng người dùng', value: data?.totalUsers, icon: Users },
    { label: 'Học viên', value: data?.learners, icon: GraduationCap },
    { label: 'Giáo viên', value: data?.teachers, icon: UserRoundCheck },
    { label: 'Nhân sự quản trị', value: data?.staffAndAdmins, icon: ShieldCheck },
  ];

  return (
    <div className="space-y-6">
      <section>
        <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Quản trị</p>
        <h1 className="mt-2 font-['Manrope'] text-2xl font-extrabold tracking-tight text-[#0b1c30] sm:text-3xl">Tổng quan hệ thống</h1>
        <p className="mt-2 text-sm leading-relaxed text-slate-500">Theo dõi tài khoản, phân quyền và hoạt động quản trị.</p>
      </section>

      {error ? (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700">
          <span>{error}</span>
          <button className="rounded-lg bg-white px-3 py-2 text-xs font-bold shadow-sm" onClick={load} type="button">Thử lại</button>
        </div>
      ) : null}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map((card) => (
          <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm" key={card.label}>
            <div className="flex items-center justify-between gap-3">
              <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-[#fff1f3] text-[#730014]">
                <card.icon className="h-5 w-5" />
              </span>
              <span className={`font-['Manrope'] text-3xl font-extrabold text-[#0b1c30] ${loading ? 'h-9 w-16 animate-pulse rounded-lg bg-slate-100 text-transparent' : ''}`}>
                {loading ? '0' : card.value ?? 0}
              </span>
            </div>
            <p className="mt-4 text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{card.label}</p>
          </article>
        ))}
      </section>

      <div className="grid gap-6 xl:grid-cols-[1.35fr_0.65fr]">
        <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Thao tác nhanh</h2>
              <p className="mt-1 text-sm text-slate-500">Các khu vực quản trị thường dùng.</p>
            </div>
            <ShieldCheck className="h-6 w-6 text-[#730014]" />
          </div>
          <div className="mt-5 grid gap-3 sm:grid-cols-2">
            <QuickLink description="Tạo tài khoản và cập nhật phân quyền." icon={Users} title="Quản lý người dùng" to="/admin/users" />
            <QuickLink description="Kiểm tra các thiết lập đang áp dụng." icon={Settings} title="Cấu hình hệ thống" to="/admin/settings" />
          </div>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-[#fff1f3] text-[#730014]">
            <ClipboardList className="h-5 w-5" />
          </span>
          <h2 className="mt-4 font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Nhật ký quản trị</h2>
          <p className="mt-1 text-sm text-slate-500">Theo dõi các thay đổi quan trọng trong hệ thống.</p>
          <Link className="mt-6 inline-flex items-center gap-2 text-sm font-bold text-[#730014] hover:underline" to="/admin/audit-logs">
            Mở nhật ký thao tác <ArrowRight className="h-4 w-4" />
          </Link>
        </section>
      </div>
    </div>
  );
}

function QuickLink({ description, icon: Icon, title, to }) {
  return (
    <Link className="rounded-xl border border-slate-100 bg-slate-50/60 p-4 transition hover:border-[#dfbfbd] hover:bg-white" to={to}>
      <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white text-[#730014] shadow-sm">
        <Icon className="h-4 w-4" />
      </span>
      <p className="mt-3 text-sm font-bold text-[#0b1c30]">{title}</p>
      <p className="mt-1 text-xs leading-5 text-slate-500">{description}</p>
    </Link>
  );
}
