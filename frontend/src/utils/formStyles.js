// Shared form/border styles for Content Manager and training-operation screens.
// consistent with the rest of the EnglishLab admin UI (rounded-2xl, soft slate
// borders, branded focus ring — never the default black outline).

export const FIELD_CLASS =
  'w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-[#730014] focus:bg-white';

export const TEXTAREA_CLASS = `${FIELD_CLASS} min-h-24`;

export const PANEL_CLASS = 'rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm';

export const CARD_CLASS = 'rounded-2xl border border-slate-200 bg-white p-4 shadow-sm';

export const SOFT_CARD_CLASS = 'rounded-2xl border border-slate-200 bg-slate-50 p-4';

export const SECONDARY_BUTTON_CLASS =
  'inline-flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-[#730014] transition hover:bg-[#fff4f5]';

export const GHOST_BUTTON_CLASS =
  'inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-[#730014] transition hover:bg-[#fff4f5]';

export const PRIMARY_BUTTON_CLASS =
  'inline-flex items-center gap-2 rounded-2xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#5d0010] disabled:opacity-60';

export const DANGER_BUTTON_CLASS =
  'inline-flex items-center gap-2 rounded-xl border border-rose-200 bg-white px-3 py-1.5 text-xs font-semibold text-rose-700 transition hover:bg-rose-50';

export const SEARCH_INPUT_CLASS =
  'w-full rounded-2xl border border-slate-200 bg-slate-50 py-2.5 pl-11 pr-4 text-sm outline-none transition focus:border-[#730014] focus:bg-white';

export const ERROR_NOTICE_CLASS =
  'rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700';

export const SUCCESS_NOTICE_CLASS =
  'rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700';

export const EMPTY_STATE_CLASS =
  'rounded-[28px] border border-dashed border-slate-200 bg-white px-6 py-10 text-center text-sm font-semibold text-slate-500';
