import { Bell } from 'lucide-react';

const VARIANTS = {
  rose: {
    wrapper: 'border-dashed border-[#dfbfbd] bg-white shadow-[0_18px_45px_rgba(75,0,9,0.04)]',
    icon: 'bg-[#fff3f4] text-[#8a0018]',
    title: 'text-[#2b2828]',
    description: 'text-[#584140]',
  },
  neutral: {
    wrapper: 'border-dashed border-slate-200 bg-white shadow-sm',
    icon: 'bg-rose-50 text-rose-700',
    title: 'text-slate-900',
    description: 'text-slate-500',
  },
};

export default function NotificationsEmptyState({
  title = 'Chưa có thông báo mới',
  description = '',
  variant = 'neutral',
}) {
  const theme = VARIANTS[variant] || VARIANTS.neutral;
  return (
    <section className={`flex min-h-[420px] flex-1 flex-col items-center justify-center rounded-[32px] border px-6 py-16 text-center ${theme.wrapper}`}>
      <div className={`flex h-14 w-14 items-center justify-center rounded-full ${theme.icon}`}>
        <Bell className="h-6 w-6" />
      </div>
      <h2 className={`mt-5 font-['Manrope'] text-3xl font-extrabold ${theme.title}`}>{title}</h2>
      {description ? (
        <p className={`mx-auto mt-3 max-w-xl text-sm leading-7 ${theme.description}`}>{description}</p>
      ) : null}
    </section>
  );
}
