import { useEffect, useState } from 'react';
import { GraduationCap, ShieldCheck, Users, UserRoundCheck } from 'lucide-react';
import adminApi from '../../api/adminApi';

export default function AdminDashboardPage() {
  const [data, setData] = useState(null); const [error, setError] = useState('');
  useEffect(() => { const load = async () => { try { setData(await adminApi.getDashboard()); } catch (err) { setError(err.response?.data?.message || 'Không tải được dashboard.'); } }; load(); }, []);
  const cards = [
    ['Tổng người dùng', data?.totalUsers, Users], ['Học viên', data?.learners, GraduationCap], ['Giáo viên', data?.teachers, UserRoundCheck], ['Nhân sự / Admin', data?.staffAndAdmins, ShieldCheck],
  ];
  return <div><div className="mb-7"><p className="text-xs font-bold uppercase tracking-[0.18em] text-[#8a0018]">Quản trị</p><h1 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-slate-900">Tổng quan hệ thống</h1><p className="mt-2 text-sm text-slate-500">Theo dõi tài khoản và phân quyền trên EnglishLab.</p></div>{error ? <p className="rounded-xl bg-rose-50 p-4 text-rose-700">{error}</p> : null}<div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">{cards.map(([label,value,Icon]) => <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm" key={label}><div className="flex items-center justify-between"><p className="text-sm font-bold text-slate-500">{label}</p><Icon className="h-5 w-5 text-[#8a0018]" /></div><p className="mt-5 font-['Manrope'] text-4xl font-extrabold text-slate-900">{value ?? '—'}</p></section>)}</div></div>;
}
