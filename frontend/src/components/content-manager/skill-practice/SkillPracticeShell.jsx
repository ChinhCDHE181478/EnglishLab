import { Plus } from 'lucide-react';
import { ManagerFilterBar, ManagerStatsGrid } from '../ManagerListUi';

export default function SkillPracticeShell({
  children,
  createLabel,
  filterChildren,
  onCreate,
  stats,
  subtitle,
  title,
}) {
  return (
    <div className="space-y-6">
      <div className="space-y-4">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 className="font-['Manrope'] text-3xl font-extrabold text-[#0b1c30]">{title}</h2>
            {subtitle ? <p className="mt-2 max-w-3xl text-sm leading-6 text-[#564241]">{subtitle}</p> : null}
          </div>
          {onCreate ? (
            <button
              className="inline-flex items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#730014]"
              onClick={onCreate}
              type="button"
            >
              <Plus className="h-4 w-4" />
              {createLabel}
            </button>
          ) : null}
        </div>
      </div>

      <ManagerStatsGrid stats={stats} />

      {filterChildren ? (
        <ManagerFilterBar>
          {filterChildren}
        </ManagerFilterBar>
      ) : null}

      {children}
    </div>
  );
}
