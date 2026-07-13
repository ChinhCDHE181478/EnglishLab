import { useState } from 'react';
import { ChevronDown, ClipboardList, LayoutDashboard, LogOut, Settings, Shield, Users } from 'lucide-react';
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
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const logout = () => { clearSession(); navigate('/login', { replace: true }); };
  return <div className="min-h-screen bg-[#f8fafc] font-['Inter'] text-slate-800 lg:pl-[270px]">
    <div className="pointer-events-none fixed inset-0 opacity-[0.02]" style={{ backgroundImage: 'radial-gradient(#4b0009 1px, transparent 1px)', backgroundSize: '24px 24px' }} />
    <aside className="fixed inset-y-0 left-0 z-30 hidden w-[270px] flex-col overflow-hidden border-r border-slate-200 bg-[#4b0009] text-white shadow-xl lg:flex">
      <div className="shrink-0 border-b border-white/10 px-6 pb-5 pt-7"><p className="bg-gradient-to-r from-white to-pink-200 bg-clip-text font-['Manrope'] text-2xl font-black tracking-tight text-transparent">EnglishLab</p><p className="mt-1.5 text-[10px] font-bold uppercase tracking-[0.2em] text-white/50">Hệ thống quản trị</p></div>
      <nav className="flex-1 space-y-1 overflow-y-auto p-4">{items.map(({ to, label, icon: Icon, end }) => <NavLink className={({ isActive }) => `flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-bold transition ${isActive ? 'bg-white text-[#4b0009] shadow-sm' : 'text-white/70 hover:bg-white/10 hover:text-white'}`} end={end} key={to} to={to}><Icon className="h-5 w-5" />{label}</NavLink>)}</nav>
      <button className="m-4 flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-bold text-white/70 hover:bg-white/10 hover:text-white" onClick={logout} type="button"><LogOut className="h-5 w-5" />Đăng xuất</button>
    </aside>
    <header className="sticky top-0 z-20 flex min-h-[82px] items-center justify-between border-b border-slate-200 bg-white/95 px-5 py-4 backdrop-blur lg:px-8"><div className="flex items-center gap-3"><span className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#730014] text-white"><Shield className="h-5 w-5" /></span><div><p className="font-extrabold text-slate-900">Quản trị hệ thống</p><p className="text-xs text-slate-500">Quản lý tài khoản, phân quyền và cấu hình</p></div></div><div className="relative hidden sm:block"><button className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white px-3 py-2 text-left shadow-sm transition hover:bg-slate-50" onClick={() => setAccountMenuOpen((current) => !current)} type="button"><span className="flex h-9 w-9 items-center justify-center rounded-lg bg-[#730014] text-sm font-bold text-white">{(user?.fullName || user?.email || 'A').charAt(0).toUpperCase()}</span><span><span className="block text-sm font-bold text-slate-900">{user?.fullName || user?.email}</span><span className="block text-[10px] font-bold uppercase tracking-wider text-[#8a0018]">Administrator</span></span><ChevronDown className={`h-4 w-4 text-slate-400 transition ${accountMenuOpen ? 'rotate-180' : ''}`} /></button>{accountMenuOpen ? <div className="absolute right-0 top-full z-50 mt-2 w-52 rounded-xl border border-slate-200 bg-white p-1 shadow-xl"><button className="flex w-full items-center gap-2 rounded-lg px-3 py-2.5 text-left text-sm font-bold text-rose-600 hover:bg-rose-50" onClick={logout} type="button"><LogOut className="h-4 w-4" />Đăng xuất</button></div> : null}</div><nav className="flex gap-2 lg:hidden">{items.map(({ to, label, end }) => <NavLink className={({isActive}) => `rounded-lg px-3 py-2 text-xs font-bold ${isActive ? 'bg-[#730014] text-white' : 'text-slate-600'}`} end={end} key={to} to={to}>{label}</NavLink>)}</nav></header>
    <main className="relative mx-auto max-w-[1500px] p-5 lg:p-8"><Outlet /></main>
  </div>;
}
