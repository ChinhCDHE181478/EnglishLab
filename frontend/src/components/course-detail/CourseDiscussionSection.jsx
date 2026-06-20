import { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, CheckCircle2, Flag, MessageCircle, Send, ThumbsUp, Loader2, X } from 'lucide-react';
import courseApi from '../../api/courseApi';
import { getStoredUser, hasAccessToken } from '../../utils/auth';

const FILTERS = [
  { id: 'ALL', label: 'Tất cả' },
  { id: 'UNANSWERED', label: 'Chưa trả lời' },
  { id: 'RESOLVED', label: 'Đã giải quyết' },
  { id: 'HELPFUL', label: 'Hữu ích nhất' },
];

const REACTIONS = [
  { id: 'LIKE', label: 'Thích', icon: '/reactions/like.png', activeClass: 'text-blue-600' },
  { id: 'LOVE', label: 'Yêu thích', icon: '/reactions/love.png', activeClass: 'text-rose-600' },
  { id: 'CARE', label: 'Quan tâm', icon: '/reactions/care.png', activeClass: 'text-pink-600' },
  { id: 'LAUGH', label: 'Haha', icon: '/reactions/laugh.png', activeClass: 'text-yellow-600' },
  { id: 'WOW', label: 'Wow', icon: '/reactions/wow.png', activeClass: 'text-orange-500' },
  { id: 'SAD', label: 'Buồn', icon: '/reactions/sad.png', activeClass: 'text-sky-600' },
  { id: 'ANGRY', label: 'Giận', icon: '/reactions/angry.png', activeClass: 'text-red-700' },
];

const formatDate = (value) => {
  if (!value) return '';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
};

const resolveErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.message || fallback;

const normalizeUiMessage = (value, fallback) => {
  const text = String(value || '').trim();
  if (!text) {
    return fallback;
  }
  if (text.includes('could not execute statement') || text.length > 180) {
    return fallback;
  }
  return text;
};

const isSameId = (left, right) => String(left || '') === String(right || '');

const shouldShowInFilter = (thread, activeFilter) => {
  if (activeFilter === 'UNANSWERED') {
    return Number(thread.replyCount || 0) === 0 && !thread.resolved;
  }
  if (activeFilter === 'RESOLVED') {
    return Boolean(thread.resolved);
  }
  return true;
};

const getReactionTotal = (counts = {}) =>
  REACTIONS.reduce((total, reaction) => total + Number(counts?.[reaction.id] || 0), 0);

const getReactionMeta = (reactionId) =>
  REACTIONS.find((reaction) => reaction.id === reactionId) || REACTIONS[0];

const formatCompactCount = (value) => {
  const count = Number(value || 0);
  if (count >= 1000) {
    return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 }).format(count / 1000)}K`;
  }
  return new Intl.NumberFormat('vi-VN').format(count);
};

const getTopReactions = (counts = {}, limit = 2) =>
  REACTIONS
    .map((reaction) => ({ ...reaction, count: Number(counts?.[reaction.id] || 0) }))
    .filter((reaction) => reaction.count > 0)
    .sort((left, right) => right.count - left.count)
    .slice(0, limit);

const getNextReactionState = (counts = {}, currentReaction, nextReaction) => {
  const nextCounts = { ...counts };

  if (currentReaction) {
    nextCounts[currentReaction] = Math.max(0, Number(nextCounts[currentReaction] || 0) - 1);
  }

  if (currentReaction === nextReaction) {
    return {
      reactionCounts: nextCounts,
      myReaction: null,
    };
  }

  nextCounts[nextReaction] = Number(nextCounts[nextReaction] || 0) + 1;
  return {
    reactionCounts: nextCounts,
    myReaction: nextReaction,
  };
};

const ReactionIcon = ({ reaction, className = 'h-5 w-5' }) => (
  <img
    alt={reaction.label}
    className={`${className} object-contain`}
    draggable="false"
    loading="eager"
    src={reaction.icon}
  />
);

const getPreviewReplies = (replies = []) =>
  [...replies]
    .sort((left, right) => {
      const helpfulDiff = Number(right.helpfulCount || 0) - Number(left.helpfulCount || 0);
      if (helpfulDiff !== 0) {
        return helpfulDiff;
      }
      return new Date(right.createdAt || 0).getTime() - new Date(left.createdAt || 0).getTime();
    })
    .slice(0, 2);

const ReactionButton = ({ counts = {}, myReaction, onReact, reactingKey, targetKey }) => {
  const activeReaction = getReactionMeta(myReaction);
  const total = getReactionTotal(counts);
  const active = Boolean(myReaction);
  const busy = reactingKey?.startsWith(`${targetKey}:`);

  return (
    <div className="group relative inline-flex [overflow-anchor:none]">
      <div className="pointer-events-none absolute bottom-full left-0 z-10 h-3 w-full opacity-0 group-hover:pointer-events-auto group-focus-within:pointer-events-auto" />
      <div className="pointer-events-none absolute bottom-full left-0 z-20 flex translate-y-1 scale-95 items-center gap-1 rounded-full border border-slate-200 bg-white px-2 py-1.5 opacity-0 shadow-lg transition group-hover:pointer-events-auto group-hover:translate-y-0 group-hover:scale-100 group-hover:opacity-100 group-focus-within:pointer-events-auto group-focus-within:translate-y-0 group-focus-within:scale-100 group-focus-within:opacity-100">
        {REACTIONS.map((reaction) => (
          <button
            className="flex h-10 w-10 items-center justify-center rounded-full transition hover:-translate-y-1 hover:scale-125 focus:-translate-y-1 focus:scale-125 focus:outline-none"
            disabled={Boolean(reactingKey)}
            key={reaction.id}
            onMouseDown={(event) => event.preventDefault()}
            onPointerDown={(event) => event.preventDefault()}
            onClick={(event) => {
              event.currentTarget.blur();
              onReact(reaction.id);
            }}
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

const ReactionSummary = ({ counts = {}, onOpen }) => {
  const total = getReactionTotal(counts);
  const topReactions = getTopReactions(counts);

  if (total === 0) {
    return null;
  }

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

const ReactionModal = ({ counts = {}, loading, onClose, reactions = [], selectedType, setSelectedType }) => {
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

const CourseDiscussionSection = ({ courseId }) => {
  const loggedIn = hasAccessToken();
  const currentUser = getStoredUser();
  const [filter, setFilter] = useState('ALL');
  const [threads, setThreads] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [questionTitle, setQuestionTitle] = useState('');
  const [questionContent, setQuestionContent] = useState('');
  const [replyContentByThread, setReplyContentByThread] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [reactingKey, setReactingKey] = useState('');
  const [actionKey, setActionKey] = useState('');
  const [openReplyByThread, setOpenReplyByThread] = useState({});
  const [reactionModal, setReactionModal] = useState(null);
  const [reactionModalItems, setReactionModalItems] = useState([]);
  const [reactionModalLoading, setReactionModalLoading] = useState(false);
  const [reactionModalTab, setReactionModalTab] = useState('ALL');

  const discussionStats = useMemo(() => ({
    total: threads.length,
    unanswered: threads.filter((item) => Number(item.replyCount || 0) === 0 && !item.resolved).length,
    resolved: threads.filter((item) => item.resolved).length,
  }), [threads]);

  const loadDiscussions = async (nextFilter = filter) => {
    if (!courseId) return;
    setLoading(true);
    setError('');
    try {
      const items = await courseApi.getCourseDiscussions(courseId, nextFilter);
      setThreads(items);
    } catch (err) {
        setError(normalizeUiMessage(resolveErrorMessage(err), 'Không thể tải thảo luận. Vui lòng thử lại.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDiscussions(filter);
  }, [courseId, filter]);

  const updateThreadInState = (updatedThread) => {
    setThreads((current) => current
      .map((thread) => (thread.id === updatedThread.id ? { ...thread, ...updatedThread } : thread))
      .filter((thread) => shouldShowInFilter(thread, filter)));
  };

  const updateReplyInState = (updatedReply) => {
    setThreads((current) => current.map((thread) => {
      const replies = Array.isArray(thread.replies) ? thread.replies : [];
      if (!replies.some((reply) => reply.id === updatedReply.id)) {
        return thread;
      }
      const nextReplies = replies.map((reply) => (reply.id === updatedReply.id ? { ...reply, ...updatedReply } : reply));

      return {
        ...thread,
        helpfulCount: nextReplies.reduce((total, reply) => total + Number(reply.helpfulCount || 0), 0),
        replies: nextReplies,
      };
    }));
  };

  const updateThreadReactionInState = (threadId, patch) => {
    setThreads((current) => current.map((thread) => (
      thread.id === threadId ? { ...thread, ...patch } : thread
    )));
  };

  const updateReplyReactionInState = (replyId, patch) => {
    setThreads((current) => current.map((thread) => ({
      ...thread,
      replies: Array.isArray(thread.replies)
        ? thread.replies.map((reply) => (reply.id === replyId ? { ...reply, ...patch } : reply))
        : thread.replies,
    })));
  };

  const handleCreateQuestion = async (event) => {
    event.preventDefault();
    setMessage('');
    if (!loggedIn) {
      setMessage('Bạn cần đăng nhập để đặt câu hỏi.');
      return;
    }
    if (!questionTitle.trim() || !questionContent.trim()) {
      setMessage('Vui lòng nhập nội dung câu hỏi.');
      return;
    }
    if (questionContent.trim().length < 20) {
      setMessage('Câu hỏi cần rõ ràng hơn để mọi người có thể hỗ trợ bạn.');
      return;
    }

    setSubmitting(true);
    try {
      const createdThread = await courseApi.createCourseDiscussion(courseId, {
        title: questionTitle.trim(),
        content: questionContent.trim(),
      });
      setQuestionTitle('');
      setQuestionContent('');
      setMessage('Câu hỏi đã được gửi thành công.');
      if (shouldShowInFilter(createdThread, filter)) {
        setThreads((current) => [createdThread, ...current]);
      }
    } catch (err) {
      setMessage(normalizeUiMessage(resolveErrorMessage(err), 'Không thể gửi câu hỏi. Vui lòng thử lại.'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateReply = async (threadId) => {
    setMessage('');
    if (!loggedIn) {
      setMessage('Bạn cần đăng nhập để trả lời thảo luận.');
      return;
    }
    const content = String(replyContentByThread[threadId] || '').trim();
    if (!content) {
      setMessage('Vui lòng nhập nội dung trả lời.');
      return;
    }

    setActionKey(`reply:${threadId}`);
    try {
      const createdReply = await courseApi.createDiscussionReply(threadId, { content });
      setReplyContentByThread((current) => ({ ...current, [threadId]: '' }));
      setOpenReplyByThread((current) => ({ ...current, [threadId]: false }));
      setMessage('Câu trả lời đã được gửi.');
      setThreads((current) => current
        .map((thread) => {
          if (thread.id !== threadId) {
            return thread;
          }

          const replies = Array.isArray(thread.replies) ? thread.replies : [];
          return {
            ...thread,
            replies: [...replies, createdReply],
            replyCount: Number(thread.replyCount || 0) + 1,
          };
        })
        .filter((thread) => shouldShowInFilter(thread, filter)));
    } catch (err) {
      setMessage(normalizeUiMessage(resolveErrorMessage(err), 'Không thể gửi câu trả lời. Vui lòng thử lại.'));
    } finally {
      setActionKey('');
    }
  };

  const handleHelpful = async (replyId) => {
    setMessage('');
    if (!loggedIn) {
      setMessage('Bạn cần đăng nhập để đánh dấu câu trả lời hữu ích.');
      return;
    }
    const reply = threads.flatMap((thread) => thread.replies || []).find((item) => item.id === replyId);
    if (isSameId(reply?.authorId, currentUser?.id)) {
      setMessage('Bạn không thể tự đánh dấu câu trả lời của mình là hữu ích.');
      return;
    }
    setActionKey(`helpful:${replyId}`);
    try {
      const updatedReply = await courseApi.toggleDiscussionReplyHelpful(replyId);
      updateReplyInState(updatedReply);
    } catch (err) {
      setMessage(normalizeUiMessage(resolveErrorMessage(err), 'Không thể cập nhật đánh dấu hữu ích. Vui lòng thử lại.'));
    } finally {
      setActionKey('');
    }
  };

  const handleReaction = async (targetType, id, reactionType) => {
    setMessage('');
    if (!loggedIn) {
      setMessage('Bạn cần đăng nhập để thả cảm xúc.');
      return;
    }

    const nextReactingKey = `${targetType}:${id}:${reactionType}`;
    const threadSnapshot = targetType === 'thread'
      ? threads.find((thread) => thread.id === id)
      : null;
    const replySnapshot = targetType === 'reply'
      ? threads.flatMap((thread) => thread.replies || []).find((reply) => reply.id === id)
      : null;
    const snapshot = targetType === 'thread' ? threadSnapshot : replySnapshot;

    if (snapshot) {
      const optimisticPatch = getNextReactionState(snapshot.reactionCounts, snapshot.myReaction, reactionType);
      if (targetType === 'thread') {
        updateThreadReactionInState(id, optimisticPatch);
      } else {
        updateReplyReactionInState(id, optimisticPatch);
      }
    }

    setReactingKey(nextReactingKey);
    try {
      if (targetType === 'thread') {
        const updatedThread = await courseApi.toggleDiscussionThreadReaction(id, reactionType);
        updateThreadReactionInState(id, {
          reactionCounts: updatedThread.reactionCounts,
          myReaction: updatedThread.myReaction,
        });
      } else {
        const updatedReply = await courseApi.toggleDiscussionReplyReaction(id, reactionType);
        updateReplyReactionInState(id, {
          reactionCounts: updatedReply.reactionCounts,
          myReaction: updatedReply.myReaction,
        });
      }
    } catch (err) {
      if (snapshot) {
        const rollbackPatch = {
          reactionCounts: snapshot.reactionCounts,
          myReaction: snapshot.myReaction,
        };
        if (targetType === 'thread') {
          updateThreadReactionInState(id, rollbackPatch);
        } else {
          updateReplyReactionInState(id, rollbackPatch);
        }
      }
      setMessage(normalizeUiMessage(resolveErrorMessage(err), 'Không thể cập nhật cảm xúc. Vui lòng thử lại.'));
    } finally {
      setReactingKey('');
    }
  };

  const handleToggleReplyBox = (threadId) => {
    setMessage('');
    setOpenReplyByThread((current) => ({ ...current, [threadId]: !current[threadId] }));
  };

  const handleReplyKeyDown = (event, threadId) => {
    if (event.key !== 'Enter' || event.shiftKey) {
      return;
    }
    event.preventDefault();
    handleCreateReply(threadId);
  };

  const handleOpenReactionModal = async (targetType, id, counts) => {
    setReactionModal({ targetType, id, counts });
    setReactionModalItems([]);
    setReactionModalTab('ALL');
    setReactionModalLoading(true);
    try {
      const items = targetType === 'thread'
        ? await courseApi.getDiscussionThreadReactions(id)
        : await courseApi.getDiscussionReplyReactions(id);
      setReactionModalItems(items);
    } catch (err) {
      setMessage(normalizeUiMessage(resolveErrorMessage(err), 'Không thể tải danh sách cảm xúc. Vui lòng thử lại.'));
    } finally {
      setReactionModalLoading(false);
    }
  };

  const handleResolved = async (threadId, replyId = null) => {
    setMessage('');
    if (!loggedIn) {
      setMessage('Bạn cần đăng nhập để đánh dấu đã giải quyết.');
      return;
    }
    setActionKey(`resolved:${threadId}:${replyId || 'thread'}`);
    try {
      const updatedThread = await courseApi.markDiscussionResolved(threadId, replyId);
      setMessage('Thảo luận đã được đánh dấu là đã giải quyết.');
      updateThreadInState(updatedThread);
    } catch (err) {
      setMessage(normalizeUiMessage(resolveErrorMessage(err), 'Không thể đánh dấu đã giải quyết. Vui lòng thử lại.'));
    } finally {
      setActionKey('');
    }
  };

  const handleReport = async (type, id) => {
    setMessage('');
    if (!loggedIn) {
      setMessage('Bạn cần đăng nhập để báo cáo nội dung.');
      return;
    }
    setActionKey(`report:${type}:${id}`);
    try {
      if (type === 'reply') {
        await courseApi.reportDiscussionReply(id, { reason: 'Nội dung không phù hợp với quy tắc cộng đồng.' });
      } else {
        await courseApi.reportDiscussionThread(id, { reason: 'Nội dung không phù hợp với quy tắc cộng đồng.' });
      }
      setMessage('Cảm ơn bạn đã báo cáo. Nội dung sẽ được xem xét.');
    } catch (err) {
      setMessage(normalizeUiMessage(resolveErrorMessage(err), 'Không thể gửi báo cáo. Vui lòng thử lại.'));
    } finally {
      setActionKey('');
    }
  };

  return (
    <section className="rounded-3xl border border-slate-100 bg-white p-6 md:p-8 shadow-sm">
      {/* Header & Stats */}
      <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between border-b border-slate-100 pb-6">
        <div>
          <p className="text-[11px] font-bold uppercase tracking-[0.2em] text-[#8a0018]">Cộng đồng thảo luận</p>
          <h2 className="mt-1 font-['Manrope'] text-2xl md:text-3xl font-extrabold text-[#1e1e1e] tracking-tight">Hỏi đáp & Hỗ trợ</h2>
          <p className="mt-2 max-w-2xl text-sm leading-relaxed text-slate-500">
            Nơi học viên trao đổi kiến thức, hỗ trợ lẫn nhau và tương tác cùng đội ngũ chuyên gia vận hành nội dung.
          </p>
        </div>
        <div className="flex gap-2 rounded-2xl bg-slate-50 p-1.5 border border-slate-100 self-start lg:self-center">
          <div className="px-4 py-2 text-center">
            <p className="text-lg font-bold text-slate-800">{discussionStats.total}</p>
            <p className="text-[11px] font-medium text-slate-400 uppercase tracking-wider">Chủ đề</p>
          </div>
          <div className="h-8 w-[1px] bg-slate-200 self-center"></div>
          <div className="px-4 py-2 text-center">
            <p className="text-lg font-bold text-amber-600">{discussionStats.unanswered}</p>
            <p className="text-[11px] font-medium text-slate-400 uppercase tracking-wider">Chưa đáp</p>
          </div>
          <div className="h-8 w-[1px] bg-slate-200 self-center"></div>
          <div className="px-4 py-2 text-center">
            <p className="text-lg font-bold text-emerald-600">{discussionStats.resolved}</p>
            <p className="text-[11px] font-medium text-slate-400 uppercase tracking-wider">Đã xong</p>
          </div>
        </div>
      </div>

      {/* Rules Banner */}
      <div className="mt-6 rounded-2xl bg-amber-50/60 border border-amber-100 p-4 transition-all hover:bg-amber-50">
        <div className="flex items-start gap-3">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
          <div className="text-xs leading-relaxed text-amber-800">
            <span className="font-bold">Quy tắc thảo luận:</span> Không sử dụng từ ngữ khiếm nhã, xúc phạm hoặc kích động. Nội dung vi phạm sẽ tự động được ẩn để chờ ban quản trị kiểm duyệt.
          </div>
        </div>
      </div>

      {/* Question Form */}
      <form className="mt-6 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm transition-shadow focus-within:shadow-md" onSubmit={handleCreateQuestion}>
        <div className="flex items-center gap-2 text-slate-800">
          <MessageCircle className="h-4 w-4 text-[#8a0018]" />
          <h3 className="font-['Manrope'] text-base font-bold">Tạo thảo luận mới</h3>
        </div>
        
        {!loggedIn && (
          <p className="mt-3 rounded-xl bg-rose-50/70 border border-rose-100 px-4 py-2.5 text-xs font-medium text-[#8a0018]">
            Bạn cần đăng nhập để đóng góp ý kiến hoặc đặt câu hỏi.
          </p>
        )}

        <div className="mt-4 grid gap-3">
          <input
            className="rounded-xl border border-slate-200 bg-slate-50/50 px-4 py-2.5 text-sm outline-none transition placeholder:text-slate-400 focus:border-[#8a0018] focus:bg-white focus:ring-4 focus:ring-[#8a0018]/5"
            disabled={!loggedIn || submitting}
            onChange={(event) => setQuestionTitle(event.target.value)}
            placeholder="Tiêu đề ngắn gọn về vấn đề của bạn..."
            value={questionTitle}
          />
          <textarea
            className="min-h-[100px] rounded-xl border border-slate-200 bg-slate-50/50 px-4 py-2.5 text-sm outline-none transition placeholder:text-slate-400 focus:border-[#8a0018] focus:bg-white focus:ring-4 focus:ring-[#8a0018]/5 resize-y"
            disabled={!loggedIn || submitting}
            onChange={(event) => setQuestionContent(event.target.value)}
            placeholder="Mô tả chi tiết câu hỏi (tối thiểu 20 ký tự) để nhận được câu trả lời chính xác nhất..."
            value={questionContent}
          />
        </div>
        <div className="mt-3 flex justify-end">
          <button
            className="inline-flex items-center gap-2 rounded-xl bg-slate-900 px-5 py-2.5 text-xs font-bold text-white transition-all hover:bg-slate-800 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-40 shadow-sm"
            disabled={!loggedIn || submitting}
            type="submit"
          >
            {submitting ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Send className="h-3.5 w-3.5" />}
            Gửi câu hỏi
          </button>
        </div>
      </form>

      {/* Global Message Alert */}
      <div className="mt-4 min-h-[42px]">
        {message && (
        <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-xs font-medium text-slate-700 shadow-sm">
          {message}
        </div>
        )}
      </div>

      {/* Filters tabs */}
      <div className="mt-8 flex flex-wrap gap-1.5 border-b border-slate-100 pb-3">
        {FILTERS.map((item) => (
          <button
            className={`rounded-xl px-4 py-2 text-xs font-bold transition-all active:scale-[0.97] ${
              filter === item.id
                ? 'bg-[#8a0018] text-white shadow-sm'
                : 'text-slate-500 hover:bg-slate-50 hover:text-slate-800'
            }`}
            key={item.id}
            onClick={() => setFilter(item.id)}
            type="button"
          >
            {item.label}
          </button>
        ))}
      </div>

      {/* Discussion List */}
      <div className="mt-6 space-y-5">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-12 text-slate-400">
            <Loader2 className="h-6 w-6 animate-spin text-slate-300" />
            <p className="mt-2 text-xs font-medium">Đang đồng bộ dữ liệu thảo luận...</p>
          </div>
        ) : error ? (
          <div className="rounded-2xl border border-rose-100 bg-rose-50/50 p-6 text-center shadow-sm">
            <p className="text-sm font-medium text-rose-900">{error}</p>
            <button className="mt-3 rounded-xl bg-slate-900 px-4 py-2 text-xs font-bold text-white transition hover:bg-slate-800" onClick={() => loadDiscussions(filter)} type="button">
              Tải lại trang
            </button>
          </div>
        ) : threads.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-slate-200 bg-white py-12 text-center">
            <h3 className="font-['Manrope'] text-base font-bold text-slate-800">Chưa có thảo luận nào</h3>
            <p className="mt-1 text-xs text-slate-400 max-w-sm mx-auto">Hãy là người đầu tiên đặt câu hỏi để khơi nguồn kiến thức cho lớp học.</p>
          </div>
        ) : threads.map((thread) => (
          <article className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm transition-all hover:shadow-md/50 hover:border-slate-200/60" key={thread.id}>
            {/* Thread Header */}
            <div className="flex items-start justify-between gap-4">
              <div className="space-y-1.5">
                <div className="flex flex-wrap items-center gap-2">
                  <h4 className="font-['Manrope'] text-base font-bold text-slate-900 leading-snug">{thread.title}</h4>
                  {thread.resolved && (
                    <span className="inline-flex items-center gap-1 rounded-md bg-emerald-50 px-2 py-0.5 text-[11px] font-bold text-emerald-700 border border-emerald-100">
                      <CheckCircle2 className="h-3 w-3" />
                      Đã giải quyết
                    </span>
                  )}
                  {thread.status === 'PENDING_REVIEW' && (
                    <span className="rounded-md bg-amber-50 px-2 py-0.5 text-[11px] font-bold text-amber-700 border border-amber-100">Đợi duyệt</span>
                  )}
                </div>
                <p className="text-[11px] font-medium text-slate-400">
                  <span className="font-bold text-slate-600">{thread.authorName}</span> • {formatDate(thread.createdAt)}
                </p>
              </div>
              
              <button
                className="inline-flex h-7 items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 text-[11px] font-medium text-slate-400 transition hover:bg-rose-50 hover:text-rose-600 hover:border-rose-100"
                disabled={Boolean(actionKey)}
                onClick={() => handleReport('thread', thread.id)}
                type="button"
              >
                <Flag className="h-3 w-3" />
                Báo cáo
              </button>
            </div>
            
            <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-600 pl-0.5">{thread.content}</p>

            <div className="mt-4 flex items-center justify-between border-y border-slate-100 py-1">
              <div className="flex items-center gap-2">
                <ReactionButton
                  counts={thread.reactionCounts}
                  myReaction={thread.myReaction}
                  onReact={(reactionType) => handleReaction('thread', thread.id, reactionType)}
                  reactingKey={reactingKey}
                  targetKey={`thread:${thread.id}`}
                />
                <button
                  className="inline-flex h-8 min-w-[74px] items-center justify-center gap-1.5 rounded-lg px-3 text-[11px] font-bold text-slate-500 transition hover:bg-slate-50 hover:text-slate-800 active:scale-[0.97]"
                  onClick={() => handleToggleReplyBox(thread.id)}
                  type="button"
                >
                  <MessageCircle className="h-3.5 w-3.5" />
                  {thread.replyCount || 0}
                </button>
              </div>
              <ReactionSummary
                counts={thread.reactionCounts}
                onOpen={() => handleOpenReactionModal('thread', thread.id, thread.reactionCounts)}
              />
            </div>

            {/* Replies section */}
            {thread.replies && thread.replies.length > 0 && (
              <div className={`mt-5 border-l-2 border-slate-100 pl-4 ${openReplyByThread[thread.id] ? 'max-h-[430px] space-y-2.5 overflow-y-auto pr-2' : 'space-y-2.5'}`}>
                {(openReplyByThread[thread.id] ? thread.replies : getPreviewReplies(thread.replies)).map((reply) => (
                  <div className="rounded-xl border border-slate-50 bg-slate-50/50 p-4 transition-all hover:bg-slate-50" key={reply.id}>
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-xs font-bold text-slate-800">
                        {reply.authorName}
                        {reply.accepted && <span className="ml-2 inline-block rounded bg-emerald-100/80 px-1.5 py-0.5 text-[10px] font-extrabold text-emerald-800">Lời giải đúng</span>}
                      </p>
                      <p className="text-[11px] text-slate-400">{formatDate(reply.createdAt)}</p>
                    </div>
                    <p className="mt-2 whitespace-pre-line text-sm leading-relaxed text-slate-600">{reply.content}</p>
                    {/* Reply actions */}
                    <div className="mt-3 flex flex-wrap gap-1.5">
                      <ReactionButton
                        counts={reply.reactionCounts}
                        myReaction={reply.myReaction}
                        onReact={(reactionType) => handleReaction('reply', reply.id, reactionType)}
                        reactingKey={reactingKey}
                        targetKey={`reply:${reply.id}`}
                      />
                      {!isSameId(reply.authorId, currentUser?.id) && (
                        <button
                          className="inline-flex h-7 items-center gap-1 rounded-lg bg-white border border-slate-200/60 px-2.5 text-[11px] font-bold text-slate-600 transition hover:bg-slate-100 active:scale-[0.96]"
                          disabled={Boolean(actionKey)}
                          onClick={() => handleHelpful(reply.id)}
                          type="button"
                        >
                          <ThumbsUp className="h-3 w-3 text-slate-400" />
                          Hữu ích ({reply.helpfulCount || 0})
                        </button>
                      )}
                      
                      {!thread.resolved && isSameId(thread.authorId, currentUser?.id) && (
                        <button
                          className="h-7 rounded-lg bg-white border border-slate-200/60 px-2.5 text-[11px] font-bold text-emerald-700 transition hover:bg-emerald-50 hover:border-emerald-100 active:scale-[0.96]"
                          disabled={Boolean(actionKey)}
                          onClick={() => handleResolved(thread.id, reply.id)}
                          type="button"
                        >
                          Giải quyết câu hỏi
                        </button>
                      )}
                      
                      <button
                        className="h-7 rounded-lg bg-white border border-slate-200/60 px-2.5 text-[11px] font-medium text-slate-400 transition hover:bg-rose-50 hover:text-rose-600 active:scale-[0.96]"
                        disabled={Boolean(actionKey)}
                        onClick={() => handleReport('reply', reply.id)}
                        type="button"
                      >
                        Báo cáo
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Quick Reply Box */}
            {openReplyByThread[thread.id] && (
              <div className="mt-4 flex items-end gap-2 rounded-2xl bg-slate-100 px-4 py-2">
                <textarea
                  className="max-h-28 min-h-[32px] flex-1 resize-none bg-transparent py-1 text-sm leading-relaxed text-slate-700 outline-none placeholder:text-slate-400 disabled:cursor-not-allowed"
                  disabled={!loggedIn}
                  onKeyDown={(event) => handleReplyKeyDown(event, thread.id)}
                  onChange={(event) => setReplyContentByThread((current) => ({ ...current, [thread.id]: event.target.value }))}
                  placeholder={loggedIn ? 'Viết câu trả lời...' : 'Đăng nhập để trả lời...'}
                  rows={1}
                  value={replyContentByThread[thread.id] || ''}
                />
                <button
                  className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-slate-400 transition hover:bg-white hover:text-[#8a0018] active:scale-[0.95] disabled:cursor-not-allowed disabled:opacity-40"
                  disabled={!loggedIn || actionKey === `reply:${thread.id}` || !String(replyContentByThread[thread.id] || '').trim()}
                  onClick={() => handleCreateReply(thread.id)}
                  title="Gửi câu trả lời"
                  type="button"
                >
                  <Send className="h-4 w-4" />
                </button>
              </div>
            )}
          </article>
        ))}
      </div>
      {reactionModal && (
        <ReactionModal
          counts={reactionModal.counts}
          loading={reactionModalLoading}
          onClose={() => setReactionModal(null)}
          reactions={reactionModalItems}
          selectedType={reactionModalTab}
          setSelectedType={setReactionModalTab}
        />
      )}
    </section>
  );
};

export default CourseDiscussionSection;
