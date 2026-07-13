import { useCallback, useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight, Loader2, MessageCircle, Send } from 'lucide-react';
import courseApi from '../../api/courseApi';
import {
  ReactionButton,
  ReactionSummary,
  ReactionModal,
  ReportModal,
  getNextReactionState,
} from '../course-discussion/DiscussionReactions';

const FILTERS = [
  { id: 'ALL', label: 'Tất cả' },
  { id: 'MINE', label: 'Câu hỏi của tôi' },
];

const PAGE_SIZE = 10;

const getErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.response?.data?.description || fallback;

const formatDate = (value) =>
  value
    ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
    : '';

const WorkspaceLessonDiscussion = ({ courseId, lessonId, lessonIds = [], canPersist = false, onDiscussionCreated, addNotification }) => {
  const [filter, setFilter] = useState('ALL');
  const [page, setPage] = useState(0);
  const [threads, setThreads] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [actionKey, setActionKey] = useState('');
  const [reactingKey, setReactingKey] = useState('');
  const [message, setMessage] = useState('');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [expanded, setExpanded] = useState({});
  const [replies, setReplies] = useState({});
  const [reactionModal, setReactionModal] = useState(null);
  const [reactionModalItems, setReactionModalItems] = useState([]);
  const [reactionModalLoading, setReactionModalLoading] = useState(false);
  const [reactionModalTab, setReactionModalTab] = useState('ALL');
  const [reportTarget, setReportTarget] = useState(null);
  const [reportSubmitting, setReportSubmitting] = useState(false);

  const loadDiscussions = useCallback(async () => {
    if (!courseId || (!lessonId && !lessonIds.length)) return;
    setLoading(true);
    setMessage('');
    try {
      if (lessonId) {
        const result = await courseApi.getLessonDiscussions(courseId, lessonId, { filter, page, size: PAGE_SIZE });
        setThreads(result.content || []);
        setTotalElements(result.totalElements || 0);
        setTotalPages(result.totalPages || 0);
      } else {
        const results = await Promise.all(lessonIds.map((id) => courseApi.getLessonDiscussions(courseId, id, { filter, page: 0, size: 100 })));
        const allThreads = results.flatMap((result) => result.content || [])
          .sort((left, right) => new Date(right.createdAt || 0) - new Date(left.createdAt || 0));
        setThreads(allThreads.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE));
        setTotalElements(allThreads.length);
        setTotalPages(Math.ceil(allThreads.length / PAGE_SIZE));
      }
    } catch (error) {
      setMessage(getErrorMessage(error, 'Không thể tải hỏi đáp cho bài học này.'));
    } finally {
      setLoading(false);
    }
  }, [courseId, lessonId, lessonIds, filter, page]);

  // Reset khi đổi bài học
  useEffect(() => {
    setExpanded({});
    setReplies({});
    setTitle('');
    setContent('');
    setFilter('ALL');
    setPage(0);
  }, [courseId, lessonId, lessonIds]);

  // Load khi courseId/lessonId/filter/page thay đổi
  useEffect(() => {
    loadDiscussions();
  }, [loadDiscussions]);

  const handleFilterChange = (nextFilter) => {
    if (nextFilter === 'MINE' && !canPersist) {
      setMessage('Bạn cần đăng nhập để xem câu hỏi của mình.');
      return;
    }
    setMessage('');
    setFilter(nextFilter);
    setPage(0);
  };

  const handlePageChange = (delta) => {
    const next = page + delta;
    if (next < 0 || next >= totalPages) return;
    setPage(next);
  };

  const replaceThread = (updated) =>
    setThreads((cur) => cur.map((t) => (t.id === updated.id ? updated : t)));

  const updateThreadReactionInState = (threadId, patch) =>
    setThreads((cur) => cur.map((t) => (t.id === threadId ? { ...t, ...patch } : t)));

  const updateReplyReactionInState = (replyId, patch) =>
    setThreads((cur) =>
      cur.map((t) => ({
        ...t,
        replies: Array.isArray(t.replies)
          ? t.replies.map((r) => (r.id === replyId ? { ...r, ...patch } : r))
          : t.replies,
      }))
    );

  const requireLogin = () => {
    if (canPersist) return true;
    setMessage('Bạn cần đăng nhập và tham gia khóa học để thực hiện thao tác này.');
    return false;
  };

  const handleCreateThread = async (event) => {
    event.preventDefault();
    if (!lessonId || !requireLogin()) return;
    if (!title.trim() || content.trim().length < 20) {
      setMessage('Vui lòng nhập tiêu đề và nội dung câu hỏi tối thiểu 20 ký tự.');
      return;
    }
    setActionKey('create');
    setMessage('');
    try {
      await courseApi.createLessonDiscussion(courseId, lessonId, {
        title: title.trim(),
        content: content.trim(),
      });
      setTitle('');
      setContent('');
      setMessage('Câu hỏi đã được gửi.');
      // Trigger notification badge
      if (addNotification) {
        addNotification({
          title: 'Câu hỏi đã được gửi',
          message: 'Giáo viên và học viên khác sẽ hỗ trợ bạn sớm.',
          type: 'success',
        });
      }
      if (onDiscussionCreated) onDiscussionCreated();
      await loadDiscussions();
    } catch (error) {
      setMessage(getErrorMessage(error, 'Không thể gửi câu hỏi.'));
    } finally {
      setActionKey('');
    }
  };

  const handleReply = async (threadId) => {
    if (!requireLogin()) return;
    const reply = String(replies[threadId] || '').trim();
    if (!reply) return;
    setActionKey(`reply:${threadId}`);
    try {
      await courseApi.createDiscussionReply(threadId, { content: reply });
      setReplies((cur) => ({ ...cur, [threadId]: '' }));
      await loadDiscussions();
    } catch (error) {
      setMessage(getErrorMessage(error, 'Không thể gửi câu trả lời.'));
    } finally {
      setActionKey('');
    }
  };

  const runThreadAction = async (key, action) => {
    if (!requireLogin()) return;
    setActionKey(key);
    setMessage('');
    try {
      const updated = await action();
      if (updated?.id) replaceThread(updated);
      else await loadDiscussions();
    } catch (error) {
      setMessage(getErrorMessage(error, 'Không thể thực hiện thao tác.'));
    } finally {
      setActionKey('');
    }
  };

  const handleReaction = async (targetType, id, reactionType) => {
    if (!requireLogin()) return;
    const nextKey = `${targetType}:${id}:${reactionType}`;
    const snapshot =
      targetType === 'thread'
        ? threads.find((t) => t.id === id)
        : threads.flatMap((t) => t.replies || []).find((r) => r.id === id);

    if (snapshot) {
      const patch = getNextReactionState(snapshot.reactionCounts, snapshot.myReaction, reactionType);
      if (targetType === 'thread') updateThreadReactionInState(id, patch);
      else updateReplyReactionInState(id, patch);
    }

    setReactingKey(nextKey);
    try {
      if (targetType === 'thread') {
        const updated = await courseApi.toggleDiscussionThreadReaction(id, reactionType);
        updateThreadReactionInState(id, { reactionCounts: updated.reactionCounts, myReaction: updated.myReaction });
      } else {
        const updated = await courseApi.toggleDiscussionReplyReaction(id, reactionType);
        updateReplyReactionInState(id, { reactionCounts: updated.reactionCounts, myReaction: updated.myReaction });
      }
    } catch (error) {
      if (snapshot) {
        const rollback = { reactionCounts: snapshot.reactionCounts, myReaction: snapshot.myReaction };
        if (targetType === 'thread') updateThreadReactionInState(id, rollback);
        else updateReplyReactionInState(id, rollback);
      }
      setMessage(getErrorMessage(error, 'Không thể cập nhật cảm xúc.'));
    } finally {
      setReactingKey('');
    }
  };

  const handleOpenReactionModal = async (targetType, id, counts) => {
    setReactionModal({ targetType, id, counts });
    setReactionModalItems([]);
    setReactionModalTab('ALL');
    setReactionModalLoading(true);
    try {
      const items =
        targetType === 'thread'
          ? await courseApi.getDiscussionThreadReactions(id)
          : await courseApi.getDiscussionReplyReactions(id);
      setReactionModalItems(items);
    } catch {
      // noop
    } finally {
      setReactionModalLoading(false);
    }
  };

  const openReport = (type, id) => {
    if (!requireLogin()) return;
    setReportTarget({ type, id });
  };

  const handleReportSubmit = async (reasonCategory, reason) => {
    if (!reportTarget) return;
    setReportSubmitting(true);
    try {
      if (reportTarget.type === 'thread') {
        await courseApi.reportDiscussionThread(reportTarget.id, { reasonCategory, reason });
      } else {
        await courseApi.reportDiscussionReply(reportTarget.id, { reasonCategory, reason });
      }
      setMessage('Cảm ơn bạn đã báo cáo. Nội dung sẽ được xem xét.');
      setReportTarget(null);
    } catch (error) {
      setMessage(getErrorMessage(error, 'Không thể gửi báo cáo.'));
    } finally {
      setReportSubmitting(false);
    }
  };

  return (
    <div className="space-y-4 pb-5">
      <p className="text-xs leading-5 text-[#6a5352]">
        Đặt câu hỏi cho đúng bài đang học để giáo viên và học viên khác hỗ trợ nhanh hơn.
      </p>

      {/* Create question form */}
      {lessonId ? <form
        className="space-y-2 rounded-2xl border border-[#ead9db] bg-[#fffdfc] p-3"
        onSubmit={handleCreateThread}
      >
        <input
          className="w-full rounded-xl border border-[#dfbfbd] bg-white px-3 py-2 text-xs outline-none focus:border-[#730014]"
          disabled={!canPersist || actionKey === 'create'}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Tiêu đề câu hỏi"
          value={title}
        />
        <textarea
          className="min-h-20 w-full resize-y rounded-xl border border-[#dfbfbd] bg-white px-3 py-2 text-xs leading-5 outline-none focus:border-[#730014]"
          disabled={!canPersist || actionKey === 'create'}
          onChange={(e) => setContent(e.target.value)}
          placeholder="Mô tả câu hỏi (tối thiểu 20 ký tự)"
          value={content}
        />
        <button
          className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-3 py-2 text-xs font-extrabold text-white hover:bg-[#730014] disabled:opacity-50"
          disabled={!canPersist || actionKey === 'create'}
          type="submit"
        >
          {actionKey === 'create' ? <Loader2 className="h-3 w-3 animate-spin" /> : <Send className="h-3 w-3" />}
          Gửi câu hỏi
        </button>
      </form> : <p className="rounded-2xl border border-dashed border-[#ead9db] bg-white px-4 py-3 text-xs text-[#6a5352]">Chọn một bài học cụ thể để gửi câu hỏi mới.</p>}

      {message ? (
        <p className="rounded-xl bg-[#fff0f1] px-3 py-2 text-xs font-semibold text-[#730014]">{message}</p>
      ) : null}

      {/* Filter + total */}
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap gap-1.5">
          {FILTERS.map((item) => (
            <button
              key={item.id}
              className={`rounded-full px-3 py-1.5 text-[11px] font-bold transition ${
                filter === item.id
                  ? 'bg-[#8a0018] text-white'
                  : 'border border-[#ead9db] bg-white text-[#6a5352] hover:bg-[#fff8f8]'
              }`}
              onClick={() => handleFilterChange(item.id)}
              type="button"
            >
              {item.label}
            </button>
          ))}
        </div>
        {!loading && totalElements > 0 && (
          <span className="text-[11px] font-semibold text-[#8c716f]">
            Tổng cộng {totalElements} câu hỏi
          </span>
        )}
      </div>

      {loading && (
        <div className="flex items-center justify-center py-6 text-[#8c716f]">
          <Loader2 className="h-4 w-4 animate-spin" />
          <span className="ml-2 text-xs">Đang tải hỏi đáp...</span>
        </div>
      )}

      {!loading && threads.length === 0 && (
        <div className="rounded-2xl border border-dashed border-[#dfbfbd] bg-[#fff8f8] p-5 text-center text-xs text-[#6a5352]">
          Chưa có câu hỏi nào cho bài học này.
        </div>
      )}

      {/* Thread list */}
      {!loading &&
        threads.map((thread) => (
          <article key={thread.id} className="rounded-2xl border border-[#ead9db] bg-white p-3 shadow-sm">
            <div className="flex items-start justify-between gap-2">
              <div>
                <h4 className="text-sm font-extrabold text-[#1f2430]">{thread.title}</h4>
                <p className="mt-1 text-[10px] text-[#8c716f]">
                  {thread.authorName} · {formatDate(thread.createdAt)}
                </p>
              </div>
              {thread.resolved && (
                <span className="rounded-full bg-emerald-50 px-2 py-1 text-[9px] font-bold text-emerald-700">
                  Đã giải quyết
                </span>
              )}
            </div>
            <p className="mt-2 whitespace-pre-line text-xs leading-5 text-[#5f5353]">{thread.content}</p>

            {/* Actions */}
            <div className="mt-3 flex flex-wrap items-center gap-1 border-t border-[#f0e3e4] pt-2">
              <ReactionButton
                counts={thread.reactionCounts}
                myReaction={thread.myReaction}
                onReact={(type) => handleReaction('thread', thread.id, type)}
                reactingKey={reactingKey}
                targetKey={`thread:${thread.id}`}
              />
              <button
                className="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-[10px] font-bold text-[#6a5352] hover:bg-[#fff8f8]"
                onClick={() => setExpanded((cur) => ({ ...cur, [thread.id]: !cur[thread.id] }))}
                type="button"
              >
                <MessageCircle className="h-3 w-3" />
                Trả lời ({thread.replyCount || 0})
              </button>
              <ReactionSummary
                counts={thread.reactionCounts}
                onOpen={() => handleOpenReactionModal('thread', thread.id, thread.reactionCounts)}
              />
              <button
                className="ml-auto text-[10px] font-bold text-rose-700 hover:underline"
                onClick={() => openReport('thread', thread.id)}
                type="button"
              >
                Báo cáo
              </button>
            </div>

            {/* Replies section */}
            {expanded[thread.id] && (
              <div className="mt-3 space-y-2 border-l-2 border-[#ead9db] pl-3">
                {(thread.replies || []).map((reply) => (
                  <div key={reply.id} className="rounded-xl bg-[#f4f7fb] p-2.5">
                    <p className="text-[10px] font-bold text-[#1f2430]">
                      {reply.authorName} · {formatDate(reply.createdAt)}
                      {reply.accepted && (
                        <span className="ml-2 inline-block rounded bg-emerald-100/80 px-1.5 py-0.5 text-[9px] font-extrabold text-emerald-800">
                          Lời giải đúng
                        </span>
                      )}
                    </p>
                    <p className="mt-1 text-xs leading-5 text-[#5f5353]">{reply.content}</p>
                    <div className="mt-2 flex flex-wrap items-center gap-1.5 text-[10px] font-bold">
                      <ReactionButton
                        counts={reply.reactionCounts}
                        myReaction={reply.myReaction}
                        onReact={(type) => handleReaction('reply', reply.id, type)}
                        reactingKey={reactingKey}
                        targetKey={`reply:${reply.id}`}
                      />
                      <button
                        className="text-[#730014] hover:underline"
                        disabled={Boolean(actionKey)}
                        onClick={() =>
                          runThreadAction(`helpful:${reply.id}`, async () => {
                            await courseApi.toggleDiscussionReplyHelpful(reply.id);
                            return null;
                          })
                        }
                        type="button"
                      >
                        Hữu ích ({reply.helpfulCount || 0})
                      </button>
                      {!thread.resolved && (
                        <button
                          className="text-emerald-700 hover:underline"
                          disabled={Boolean(actionKey)}
                          onClick={() =>
                            runThreadAction(`resolved:${reply.id}`, () =>
                              courseApi.markDiscussionResolved(thread.id, reply.id)
                            )
                          }
                          type="button"
                        >
                          Đánh dấu lời giải
                        </button>
                      )}
                      <button
                        className="text-rose-700 hover:underline"
                        onClick={() => openReport('reply', reply.id)}
                        type="button"
                      >
                        Báo cáo
                      </button>
                    </div>
                  </div>
                ))}

                {/* Reply input */}
                <div className="flex gap-2">
                  <textarea
                    className="min-h-16 flex-1 resize-y rounded-xl border border-[#dfbfbd] px-3 py-2 text-xs outline-none"
                    disabled={!canPersist}
                    onChange={(e) => setReplies((cur) => ({ ...cur, [thread.id]: e.target.value }))}
                    placeholder="Viết câu trả lời..."
                    value={replies[thread.id] || ''}
                  />
                  <button
                    className="self-end rounded-xl bg-[#4b0009] px-3 py-2 text-xs font-bold text-white disabled:opacity-50"
                    disabled={!canPersist || actionKey === `reply:${thread.id}`}
                    onClick={() => handleReply(thread.id)}
                    type="button"
                  >
                    <Send className="h-3 w-3" />
                  </button>
                </div>
              </div>
            )}
          </article>
        ))}

      {/* Pagination */}
      {totalElements > 0 && (
        <div className="flex items-center justify-center gap-3 pt-2">
          <button
            className="inline-flex items-center gap-1 rounded-lg border border-[#ead9db] bg-white px-3 py-1.5 text-xs font-bold text-[#6a5352] transition hover:bg-[#fff8f8] disabled:opacity-40"
            disabled={page === 0 || loading}
            onClick={() => handlePageChange(-1)}
            type="button"
          >
            <ChevronLeft className="h-3.5 w-3.5" />
            Trước
          </button>
          <span className="text-[11px] font-semibold text-[#8c716f]">
            Trang {page + 1} / {totalPages}
          </span>
          <button
            className="inline-flex items-center gap-1 rounded-lg border border-[#ead9db] bg-white px-3 py-1.5 text-xs font-bold text-[#6a5352] transition hover:bg-[#fff8f8] disabled:opacity-40"
            disabled={page >= totalPages - 1 || loading}
            onClick={() => handlePageChange(1)}
            type="button"
          >
            Sau
            <ChevronRight className="h-3.5 w-3.5" />
          </button>
        </div>
      )}

      {/* Reaction modal */}
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

      {/* Report modal */}
      {reportTarget && (
        <ReportModal
          onClose={() => setReportTarget(null)}
          onSubmit={handleReportSubmit}
          submitting={reportSubmitting}
        />
      )}
    </div>
  );
};

export default WorkspaceLessonDiscussion;
