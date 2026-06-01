import { Bell, ChevronRight, Search, Settings } from 'lucide-react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { contentManagerNav, contentManagerPageMeta } from './contentManagerConfig';

export function ContentManagerLayout({ children }) {
  const location = useLocation();
  const meta = resolveMeta(location.pathname);
  const crumbs = location.pathname.replace('/content-manager/', '').split('/').filter(Boolean);

  return (
    <div className="min-h-screen bg-[#f9f9f9] font-['Inter'] text-[#1a1c1c]">
      <div className="pointer-events-none fixed inset-0 opacity-[0.045]" style={{ backgroundImage: 'radial-gradient(#4b0009 0.6px, transparent 0.6px)', backgroundSize: '34px 34px' }} />

      <aside className="fixed inset-y-0 left-0 z-30 hidden w-[280px] border-r border-[#dfbfbd]/55 bg-[#4b0009] px-5 py-6 text-white lg:block">
        <div className="mb-8">
          <p className="font-['Manrope'] text-2xl font-extrabold">EnglishLab</p>
          <p className="mt-1 text-xs uppercase tracking-[0.24em] text-white/60">Content Manager</p>
        </div>

        <nav className="space-y-6">
          {contentManagerNav.map((section) => (
            <div key={section.title}>
              <p className="mb-2 px-3 text-[11px] font-semibold uppercase tracking-[0.22em] text-white/35">{section.title}</p>
              <div className="space-y-1">
                {section.items.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.href}
                      className={({ isActive }) =>
                        `flex items-center gap-3 rounded-2xl px-3 py-3 text-sm transition ${
                          isActive ? 'bg-white text-[#4b0009] shadow-[0_14px_32px_rgba(0,0,0,0.22)]' : 'text-white/70 hover:bg-white/8 hover:text-white'
                        }`
                      }
                      to={item.href}
                    >
                      <Icon className="h-4 w-4" />
                      <span className="font-medium">{item.label}</span>
                    </NavLink>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>
      </aside>

      <div className="lg:ml-[280px]">
        <header className="sticky top-0 z-20 border-b border-[#dfbfbd]/50 bg-[#f9f9f9]/95 backdrop-blur">
          <div className="mx-auto flex max-w-[1680px] items-center gap-4 px-4 py-4 sm:px-6 lg:px-8">
            <div className="min-w-0 flex-1">
              <div className="mb-2 flex flex-wrap items-center gap-2 text-sm text-[#584140]">
                <Link className="font-medium text-[#730014] hover:underline" to="/content-manager/dashboard">
                  Content Manager
                </Link>
                {crumbs.map((crumb, index) => {
                  const isLast = index === crumbs.length - 1;
                  const href = `/content-manager/${crumbs.slice(0, index + 1).join('/')}`;
                  const label = crumb.replace(/-/g, ' ');
                  return (
                  <span key={`${crumb}-${index}`} className="inline-flex items-center gap-2 capitalize">
                    <ChevronRight className="h-4 w-4 text-[#aa8e8d]" />
                    {isLast ? (
                      <span className="font-semibold text-[#1a1c1c]">{label}</span>
                    ) : (
                      <Link className="font-medium text-[#730014] hover:underline" to={href}>{label}</Link>
                    )}
                  </span>
                  );
                })}
              </div>
              <div className="relative max-w-xl">
                <Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[#8a6f6d]" />
                <input
                  className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-white py-3 pl-11 pr-4 text-sm outline-none transition focus:border-[#730014] focus:ring-4 focus:ring-[#730014]/8"
                  placeholder={meta.searchPlaceholder}
                  type="text"
                />
              </div>
            </div>
            <div className="hidden items-center gap-3 sm:flex">
              <button className="rounded-2xl border border-[#dfbfbd]/60 bg-white p-3 text-[#584140]" type="button">
                <Bell className="h-4 w-4" />
              </button>
              <button className="rounded-2xl border border-[#dfbfbd]/60 bg-white p-3 text-[#584140]" type="button">
                <Settings className="h-4 w-4" />
              </button>
              <div className="rounded-2xl border border-[#dfbfbd]/60 bg-white px-4 py-3 text-right shadow-sm">
                <p className="text-sm font-semibold text-[#1a1c1c]">EnglishLab Admin</p>
                <p className="text-xs text-[#584140]">Role: Content Manager</p>
              </div>
            </div>
          </div>
        </header>

        <main className="mx-auto max-w-[1680px] px-4 py-6 sm:px-6 lg:px-8">
          <section className="mb-8">
            <h1 className="font-['Manrope'] text-3xl font-extrabold tracking-[-0.03em] text-[#4b0009] sm:text-4xl">{meta.title}</h1>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-[#584140] sm:text-base">{meta.subtitle}</p>
          </section>
          {children}
        </main>
      </div>
    </div>
  );
}

function resolveMeta(pathname) {
  if (contentManagerPageMeta[pathname]) {
    return contentManagerPageMeta[pathname];
  }

  if (/^\/content-manager\/courses\/[^/]+\/edit$/.test(pathname)) {
    return contentManagerPageMeta['/content-manager/courses/:slugOrId/edit'];
  }

  if (/^\/content-manager\/courses\/[^/]+\/builder$/.test(pathname)) {
    return contentManagerPageMeta['/content-manager/courses/:slugOrId/builder'];
  }

  return contentManagerPageMeta['/content-manager/dashboard'];
}

export function Panel({ children, className = '' }) {
  return <section className={`rounded-[28px] border border-[#dfbfbd]/55 bg-white shadow-[0_16px_40px_rgba(75,0,9,0.05)] ${className}`}>{children}</section>;
}

export function StatusBadge({ label }) {
  const tone = {
    DRAFT: 'bg-slate-200 text-slate-700',
    Draft: 'bg-slate-200 text-slate-700',
    PUBLISHED: 'bg-emerald-100 text-emerald-700',
    Published: 'bg-emerald-100 text-emerald-700',
    ARCHIVED: 'bg-rose-100 text-rose-700',
    Archived: 'bg-rose-100 text-rose-700',
    'Pending review': 'bg-amber-100 text-amber-700',
    'PENDING REVIEW': 'bg-amber-100 text-amber-700',
    Ready: 'bg-emerald-100 text-emerald-700',
    Processed: 'bg-emerald-100 text-emerald-700',
    Encoding: 'bg-amber-100 text-amber-700',
  };

  return <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${tone[label] ?? 'bg-[#fff2f3] text-[#730014]'}`}>{label}</span>;
}

export function SectionTitle({ title, action, onAction }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">{title}</h2>
      {action ? (
        <button className="text-sm font-semibold text-[#730014]" onClick={onAction} type="button">
          {action}
        </button>
      ) : null}
    </div>
  );
}

export function FilterChip({ label }) {
  return <div className="inline-flex items-center rounded-2xl border border-[#dfbfbd]/65 bg-white px-4 py-3 text-sm text-[#584140]">{label}</div>;
}

export function TextField({ label, value, onChange, textarea = false, rows = 4 }) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">{label}</span>
      {textarea ? (
        <textarea
          className="min-h-0 w-full rounded-2xl border border-[#dfbfbd]/65 bg-[#fcfbfb] px-4 py-3 text-sm text-[#1a1c1c] outline-none focus:border-[#730014]"
          onChange={onChange}
          rows={rows}
          value={value}
        />
      ) : (
        <input
          className="w-full rounded-2xl border border-[#dfbfbd]/65 bg-[#fcfbfb] px-4 py-3 text-sm text-[#1a1c1c] outline-none focus:border-[#730014]"
          onChange={onChange}
          value={value}
        />
      )}
    </label>
  );
}
