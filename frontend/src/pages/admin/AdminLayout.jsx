import { ClipboardList, LayoutDashboard, LogOut, Settings, Shield, Users } from 'lucide-react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { clearSession, getStoredUser } from '../../utils/auth';

const items = [
  { to: '/admin', label: 'Tổng quan', icon: LayoutDashboard, end: true },
  { to: '/admin/users', label: 'Người dùng & vai trò', icon: Users },
  { to: '/admin/settings', label: 'Cấu hình hệ thống', icon: Settings },
  { to: '/admin/audit-logs', label: 'Nhật ký thao tác', icon: ClipboardList },
];

export default function AdminLayout() {
  const navigate = useNavigate();
  const user = getStoredUser();
  const logout = () => { clearSession(); navigate('/login', { replace: true }); };
  return <div className="min-h-screen bg-slate-50 lg:pl-[270px]">
    <aside className="fixed inset-y-0 left-0 z-30 hidden w-[270px] flex-col bg-[#4b0009] text-white lg:flex">
      <div className="border-b border-white/10 px-6 py-7"><p className="font-['Manrope'] text-2xl font-black">EnglishLab</p><p className="mt-1 text-[10px] font-bold uppercase tracking-[0.22em] text-white/50">Quản trị hệ thống</p></div>
      <nav className="flex-1 space-y-2 p-4">{items.map(({ to, label, icon: Icon, end }) => <NavLink className={({ isActive }) => `flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-bold transition ${isActive ? 'bg-white text-[#4b0009]' : 'text-white/70 hover:bg-white/10 hover:text-white'}`} end={end} key={to} to={to}><Icon className="h-5 w-5" />{label}</NavLink>)}</nav>
      <button className="m-4 flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-bold text-white/70 hover:bg-white/10 hover:text-white" onClick={logout} type="button"><LogOut className="h-5 w-5" />Đăng xuất</button>
    </aside>
    <header className="sticky top-0 z-20 flex items-center justify-between border-b border-slate-200 bg-white/90 px-5 py-4 backdrop-blur lg:px-8"><div className="flex items-center gap-3"><span className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#730014] text-white"><Shield className="h-5 w-5" /></span><div><p className="font-extrabold text-slate-900">Admin Console</p><p className="text-xs text-slate-500">{user?.fullName || user?.email}</p></div></div><nav className="flex gap-2 lg:hidden">{items.map(({ to, label, end }) => <NavLink className={({isActive}) => `rounded-lg px-3 py-2 text-xs font-bold ${isActive ? 'bg-[#730014] text-white' : 'text-slate-600'}`} end={end} key={to} to={to}>{label}</NavLink>)}</nav></header>
    <main className="mx-auto max-w-[1500px] p-5 lg:p-8"><Outlet /></main>
  </div>;
}
