import { Link } from 'react-router-dom';

const WorkspaceOverview = ({
  course,
  enrollment,
  workspaceMode,
  hasVocabularyTerms,
  onWorkspaceModeChange,
}) => {
  const progressPercent = Math.min(100, Math.max(0, Number(enrollment?.progressPercent || 0)));
  const detailPath = `/courses/${course?.slug || course?.id}`;

  return (
    <section className="rounded-[14px] border border-[#e5d7d9] bg-white px-4 py-3 shadow-sm">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
        <Link
          className="group inline-flex w-fit shrink-0 items-center gap-2 text-sm font-extrabold text-[#8a0018] transition hover:text-[#4b0009] lg:min-w-[260px]"
          to={detailPath}
          reloadDocument
        >
          <span className="material-symbols-outlined text-[18px]">arrow_back</span>
          <span className="group-hover:underline">Quay lại chi tiết khóa học</span>
        </Link>

        <div className="min-w-0 flex-1">
          <div className="mb-2 flex items-center justify-between gap-3">
            <span className="text-xs font-black uppercase tracking-[0.16em] text-[#8c716f]">Tiến trình</span>
            <span className="text-sm font-extrabold text-[#4b0009]">{progressPercent}%</span>
          </div>
          <div className="h-2 overflow-hidden rounded-full bg-[#f0e5e7]">
            <div
              className="h-full rounded-full bg-[#4b0009] transition-all duration-500"
              style={{ width: `${progressPercent}%` }}
            />
          </div>
        </div>

        <div className="flex shrink-0 justify-start lg:justify-end">
          <div className="inline-flex rounded-full border border-[#e4d5d7] bg-[#f9f9f9] p-1">
            <button
              className={`rounded-full px-4 py-2 text-sm font-extrabold transition ${
                workspaceMode === 'learn'
                  ? 'bg-[#4b0009] text-white shadow-sm'
                  : 'text-[#584140] hover:bg-white'
              }`}
              type="button"
              onClick={() => onWorkspaceModeChange?.('learn')}
            >
              Học theo bài
            </button>
            <button
              className={`rounded-full px-4 py-2 text-sm font-extrabold transition ${
                workspaceMode === 'flashcards'
                  ? 'bg-[#4b0009] text-white shadow-sm'
                  : 'text-[#584140] hover:bg-white'
              } disabled:cursor-not-allowed disabled:opacity-45`}
              type="button"
              disabled={!hasVocabularyTerms}
              onClick={() => onWorkspaceModeChange?.('flashcards')}
            >
              Thẻ ghi nhớ
            </button>
          </div>
        </div>
      </div>
    </section>
  );
};

export default WorkspaceOverview;
