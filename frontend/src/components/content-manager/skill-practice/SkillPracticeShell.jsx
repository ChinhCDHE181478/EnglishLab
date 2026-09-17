import { Plus } from 'lucide-react';
import { HeaderActions } from '../ContentManagerUi';
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
        <HeaderActions>
          <button
            className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#730014] active:scale-[0.98]"
            onClick={onCreate}
            type="button"
          >
            <Plus className="h-4 w-4" />
            {createLabel}
          </button>
        </HeaderActions>
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
