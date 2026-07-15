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
      {onCreate ? (
        <div className="flex justify-end">
          <button
            className="inline-flex items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#730014]"
            onClick={onCreate}
            type="button"
          >
            <Plus className="h-4 w-4" />
            {createLabel}
          </button>
        </div>
      ) : null}

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
