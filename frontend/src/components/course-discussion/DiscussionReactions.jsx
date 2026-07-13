import { useState } from 'react';
import { ThumbsUp, Loader2, X } from 'lucide-react';

export const REACTIONS = [
  { id: 'LIKE', label: 'Thích', icon: '/reactions/like.png', activeClass: 'text-blue-600' },
  { id: 'LOVE', label: 'Yêu thích', icon: '/reactions/love.png', activeClass: 'text-rose-600' },
  { id: 'CARE', label: 'Quan tâm', icon: '/reactions/care.png', activeClass: 'text-pink-600' },
  { id: 'LAUGH', label: 'Haha', icon: '/reactions/laugh.png', activeClass: 'text-yellow-600' },
  { id: 'WOW', label: 'Wow', icon: '/reactions/wow.png', activeClass: 'text-orange-500' },
  { id: 'SAD', label: 'Buồn', icon: '/reactions/sad.png', activeClass: 'text-sky-600' },
  { id: 'ANGRY', label: 'Giận', icon: '/reactions/angry.png', activeClass: 'text-red-700' },
];

export const REPORT_CATEGORIES = [
  { value: 'SPAM', label: 'Spam / quảng cáo' },
  { value: 'INAPPROPRIATE_LANGUAGE', label: 'Ngôn ngữ không phù hợp' },
  { value: 'OFF_TOPIC', label: 'Sai chủ đề' },
  { value: 'HARASSMENT', label: 'Quấy rối' },
  { value: 'OTHER', label: 'Khác' },
];

export const getReactionTotal = (counts = {}) =>
  REACTIONS.reduce((total, reaction) => total + Number(counts?.[reaction.id] || 0), 0);

export const getReactionMeta = (reactionId) =>
  REACTIONS.find((r) => r.id === reactionId) || REACTIONS[0];

export const formatCompactCount = (value) => {
  const count = Number(value || 0);
  if (count >= 1000) {
    return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 }).format(count / 1000)}K`;
  }
  return new Intl.NumberFormat('vi-VN').format(count);
};

export const getTopReactions = (counts = {}, limit = 2) =>
  REACTIONS
    .map((reaction) => ({ ...reaction, count: Number(counts?.[reaction.id] || 0) }))
    .filter((reaction) => reaction.count > 0)
    .sort((left, right) => right.count - left.count)
    .slice(0, limit);

export const getNextReactionState = (counts = {}, currentReaction, nextReaction) => {
  const nextCounts = { ...counts };
  if (currentReaction) {
    nextCounts[currentReaction] = Math.max(0, Number(nextCounts[currentReaction] || 0) - 1);
  }
  if (currentReaction === nextReaction) {
    return { reactionCounts: nextCounts, myReaction: null };
  }
  nextCounts[nextReaction] = Number(nextCounts[nextReaction] || 0) + 1;
  return { reactionCounts: nextCounts, myReaction: nextReaction };
};

export const ReactionIcon = ({ reaction, className = 'h-5 w-5' }) => (
  <img
    alt={reaction.label}
    className={`${className} object-contain`}
    draggable="false"
    loading="eager"
    src={reaction.icon}
  />
);

export const ReactionButton = ({ counts = {}, myReaction, onReact, reactingKey, targetKey }) => {
  const activeReaction = getReactionMeta(myReaction);
  const total = getReactionTotal(counts);
  const active = Boolean(myReaction);
  const busy = reactingKey?.startsWith(`${targetKey}:`);

  return (
    <div className="group relative inline-flex [overflow-anchor:none]">
      {/* Bridge to keep hover open */}
      <div className="pointer-events-none absolute bottom-full left-0 z-10 h-3 w-full opacity-0 group-hover:pointer-events-auto group-focus-within:pointer-events-auto" />
      {/* Reaction picker bar */}
      <div className="pointer-events-none absolute bottom-full left-0 z-20 flex translate-y-1 scale-95 items-center gap-1 rounded-full border border-slate-200 bg-white px-2 py-1.5 opacity-0 shadow-lg transition group-hover:pointer-events-auto group-hover:translate-y-0 group-hover:scale-100 group-hover:opacity-100 group-focus-within:pointer-events-auto group-focus-within:translate-y-0 group-focus-within:scale-100 group-focus-within:opacity-100">
        {REACTIONS.map((reaction) => (
          <button
            className="flex h-10 w-10 items-center justify-center rounded-full transition hover:-translate-y-1 hover:scale-125 focus:-translate-y-1 focus:scale-125 focus:outline-none"
            disabled={Boolean(reactingKey)}
            key={reaction.id}
            onMouseDown={(e) => e.preventDefault()}
            onPointerDown={(e) => e.preventDefault()}
            onClick={(e) => { e.currentTarget.blur(); onReact(reaction.id); }}
            title={reaction.label}
            type="button"
          >
            <ReactionIcon reaction={reaction} className="h-9 w-9" />
          </button>
        ))}
      </div>

      <button
        aria-pressed={active}
        className={`inline-flex h-8 min-w-[74px] items-center justify-center gap-1.5 rounded-lg border border-transparent px-3 text-[11px] font-bold transition hover:bg-slate-50 active:scale-[0.97] disabled:cursor-not-allowed disabled:opacity-60 ${
          active ? activeReaction.activeClass : 'text-slate-500'
        }`}
        disabled={Boolean(reactingKey)}
        onClick={() => onReact(myReaction || 'LIKE')}
        type="button"
      >
        {active ? (
          <ReactionIcon reaction={activeReaction} className="h-4 w-4" />
        ) : (
          <ThumbsUp className="h-3.5 w-3.5" />
        )}
        <span>{busy ? '...' : total}</span>
      </button>
    </div>
  );
};

export const ReactionSummary = ({ counts = {}, onOpen }) => {
  const total = getReactionTotal(counts);
  const topReactions = getTopReactions(counts);

  if (total === 0) return null;

  return (
    <button
      className="ml-auto inline-flex items-center gap-1 rounded-full px-2 py-1 text-[11px] font-bold text-slate-500 transition hover:bg-slate-50 hover:text-slate-800"
      onClick={onOpen}
      type="button"
    >
      <span className="flex -space-x-1">
        {topReactions.map((reaction) => (
          <span
            className="flex h-5 w-5 items-center justify-center rounded-full border border-white bg-white shadow-sm"
            key={reaction.id}
            title={reaction.label}
          >
            <ReactionIcon reaction={reaction} className="h-5 w-5" />
          </span>
        ))}
      </span>
      <span>{formatCompactCount(total)}</span>
    </button>
  );
};

export const ReactionModal = ({ counts = {}, loading, onClose, reactions = [], selectedType, setSelectedType }) => {
  const availableTabs = [
    { id: 'ALL', label: 'Tất cả', count: getReactionTotal(counts) },
    ...REACTIONS
      .map((reaction) => ({ ...reaction, count: Number(counts?.[reaction.id] || 0) }))
      .filter((reaction) => reaction.count > 0),
  ];
  const visibleReactions = selectedType === 'ALL'
    ? reactions
    : reactions.filter((reaction) => reaction.type === selectedType);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 px-4 py-6">
      <div className="flex max-h-[82vh] w-full max-w-xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl">
        <div className="flex items-center gap-2 border-b border-slate-100 px-4 py-3">
          <div className="flex flex-1 gap-1 overflow-x-auto">
            {availableTabs.map((tab) => (
              <button
                className={`flex h-11 shrink-0 items-center gap-2 rounded-lg px-3 text-sm font-bold transition ${
                  selectedType === tab.id
                    ? 'bg-slate-100 text-[#8a0018]'
                    : 'text-slate-500 hover:bg-slate-50 hover:text-slate-800'
                }`}
                key={tab.id}
                onClick={() => setSelectedType(tab.id)}
                type="button"
              >
                {tab.icon && <ReactionIcon reaction={tab} className="h-5 w-5" />}
                <span>{tab.label}</span>
                <span>{formatCompactCount(tab.count)}</span>
              </button>
            ))}
          </div>
          <button
            className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-slate-100 text-slate-600 transition hover:bg-slate-200"
            onClick={onClose}
            type="button"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="min-h-[220px] flex-1 overflow-y-auto px-4 py-3">
          {loading ? (
            <div className="flex items-center justify-center py-16 text-slate-400">
              <Loader2 className="h-5 w-5 animate-spin" />
            </div>
          ) : visibleReactions.length === 0 ? (
            <p className="py-12 text-center text-sm text-slate-400">Chưa có cảm xúc nào.</p>
          ) : (
            <div className="space-y-3">
              {visibleReactions.map((reaction) => {
                const meta = getReactionMeta(reaction.type);
                return (
                  <div className="flex items-center gap-3" key={`${reaction.userId}-${reaction.type}`}>
                    <div className="relative flex h-10 w-10 items-center justify-center rounded-full bg-slate-200 text-sm font-bold text-slate-600">
                      {String(reaction.userName || '?').trim().charAt(0).toUpperCase()}
                      <span className="absolute -bottom-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full border border-white bg-white">
                        <ReactionIcon reaction={meta} className="h-5 w-5" />
                      </span>
                    </div>
                    <p className="min-w-0 flex-1 truncate text-sm font-bold text-slate-800">{reaction.userName}</p>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

/**
 * ReportModal — modal chọn loại báo cáo
 * Props: onClose, onSubmit(category, reason), submitting
 */
export const ReportModal = ({ onClose, onSubmit, submitting }) => {
  const [category, setCategory] = useState('');
  const [reason, setReason] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!category) return;
    if (category === 'OTHER' && !reason.trim()) return;
    onSubmit(category, category === 'OTHER' ? reason.trim() : undefined);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl">
        <h3 className="font-['Manrope'] text-base font-extrabold text-slate-800">Chọn lý do báo cáo</h3>
        <form className="mt-4 space-y-2" onSubmit={handleSubmit}>
          {REPORT_CATEGORIES.map((cat) => (
            <label
              key={cat.value}
              className={`flex cursor-pointer items-center gap-3 rounded-xl border px-4 py-3 text-sm font-semibold transition ${
                category === cat.value
                  ? 'border-[#8a0018] bg-[#fff0f1] text-[#8a0018]'
                  : 'border-slate-200 text-slate-600 hover:border-slate-300 hover:bg-slate-50'
              }`}
            >
              <input
                checked={category === cat.value}
                className="accent-[#8a0018]"
                onChange={() => setCategory(cat.value)}
                type="radio"
                value={cat.value}
              />
              {cat.label}
            </label>
          ))}
          {category === 'OTHER' && (
            <textarea
              className="mt-2 w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-[#8a0018] focus:ring-2 focus:ring-[#8a0018]/10 resize-none"
              onChange={(e) => setReason(e.target.value)}
              placeholder="Mô tả lý do báo cáo..."
              required
              rows={3}
              value={reason}
            />
          )}
          <div className="mt-4 flex gap-2">
            <button
              className="flex-1 rounded-xl border border-slate-200 py-2.5 text-sm font-bold text-slate-600 transition hover:bg-slate-50"
              onClick={onClose}
              type="button"
            >
              Hủy
            </button>
            <button
              className="flex-1 rounded-xl bg-[#8a0018] py-2.5 text-sm font-bold text-white transition hover:bg-[#730014] disabled:opacity-50"
              disabled={!category || (category === 'OTHER' && !reason.trim()) || submitting}
              type="submit"
            >
              {submitting ? 'Đang gửi...' : 'Gửi báo cáo'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
