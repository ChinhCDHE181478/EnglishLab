/** Shared page layout constants (Tailwind). Header/footer: 1280px, main: 1320px. */

export const PAGE_SHELL_CLASS =
  'course-page flex min-h-[100dvh] w-full flex-1 flex-col bg-[#f9f9f9] text-[#1a1c1c]';

export const PAGE_BODY_CLASS = 'flex w-full flex-1 flex-col min-h-0';

export const PAGE_HEADER_CLASS = 'w-full shrink-0';

export const PAGE_FOOTER_CLASS = 'mt-auto w-full shrink-0';

export const PAGE_CONTAINER_CLASS = 'mx-auto w-full max-w-[1320px] px-4 md:px-10';

export const PAGE_SECTION_CARD_CLASS =
  'rounded-2xl border border-[#e5e7eb] bg-white p-6 shadow-sm md:p-8';

export const PAGE_MAIN_STACK_CLASS = `${PAGE_CONTAINER_CLASS} flex flex-1 flex-col min-h-0 space-y-8 pb-8 pt-8`;

export const PAGE_SCHEDULE_CLASS = `${PAGE_CONTAINER_CLASS} flex flex-1 min-h-0 items-start gap-5 py-5 pb-10 md:pb-12`;
