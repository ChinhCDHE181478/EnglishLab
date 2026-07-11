import { useCallback, useEffect, useMemo, useState } from 'react';
import courseApi from '../../api/courseApi';

const FILTERS = [
  { id: 'ALL', label: 'Tất cả' },
  { id: 'UNANSWERED', label: 'Chưa trả lời' },
  { id: 'RESOLVED', label: 'Đã giải quyết' },
];

const REACTIONS = [
  { type: 'LIKE', icon: 'thumb_up', label: 'Thích' },
  { type: 'LOVE', icon: 'favorite', label: 'Yêu thích' },
  { type: 'WOW', icon: 'sentiment_excited', label: 'Ấn tượng' },
];

const getErrorMessage = (error, fallback) => error?.response?.data?.message || error?.response?.data?.description || fallback;
const formatDate = (value) => value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '';
const reactionTotal = (counts = {}) => Object.values(counts).reduce((total, count) => total + (Number(count) || 0), 0);

const WorkspaceLessonDiscussion = ({ courseId, lessonId, canPersist = false }) => {
  const [filter, setFilter] = useState('ALL');
  const [threads, setThreads] = useState([]);
  const [loading, setLoading] = useState(false);
  const [actionKey, setActionKey] = useState('');
  const [message, setMessage] = useState('');
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [expanded, setExpanded] = useState({});
  const [replies, setReplies] = useState({});

  const loadDiscussions = useCallback(async () => {
    if (!courseId || !lessonId) return;
    setLoading(true);
    setMessage('');
    try {
      setThreads(await courseApi.getLessonDiscussions(courseId, lessonId, filter));
    } catch (error) {
      setMessage(getErrorMessage(error, 'Không thể tải hỏi đáp cho bài học này.'));
    } finally {
      setLoading(false);
    }
  }, [courseId, filter, lessonId]);

  useEffect(() => {
    setExpanded({});
    setReplies({});
    setTitle('');
    setContent('');
    loadDiscussions();
  }, [loadDiscussions]);

  const stats = useMemo(() => ({
    total: threads.length,
    unanswered: threads.filter((thread) => !thread.resolved && !thread.replyCount).length,
    resolved: threads.filter((thread) => thread.resolved).length,
  }), [threads]);

  const replaceThread = (updated) => setThreads((current) => current.map((thread) => thread.id === updated.id ? updated : thread));

  const requireLogin = () => {
    if (canPersist) return true;
    setMessage('Bạn cần đăng nhập và tham gia khóa học để thực hiện thao tác này.');
    return false;
  };

  const handleCreateThread = async (event) => {
    event.preventDefault();
    if (!requireLogin()) return;
    if (!title.trim() || content.trim().length < 20) {
      setMessage('Vui lòng nhập tiêu đề và nội dung câu hỏi tối thiểu 20 ký tự.');
      return;
    }
    setActionKey('create');
    setMessage('');
    try {
      await courseApi.createLessonDiscussion(courseId, lessonId, { title: title.trim(), content: content.trim() });
      setTitle('');
      setContent('');
      setMessage('Câu hỏi đã được gửi.');
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
      setReplies((current) => ({ ...current, [threadId]: '' }));
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

  return (
    <div className="space-y-4 pb-5">
      <div>
        <p className="text-xs leading-5 text-[#6a5352]">Đặt câu hỏi cho đúng bài đang học để giáo viên và học viên khác hỗ trợ nhanh hơn.</p>
      </div>

      <div className="grid grid-cols-3 gap-2">
        {[['Tổng', stats.total], ['Chưa đáp', stats.unanswered], ['Đã xong', stats.resolved]].map(([label, value]) => (
          <div key={label} className="rounded-xl border border-[#ead9db] bg-[#fffdfc] p-2 text-center">
            <p className="text-base font-extrabold text-[#4b0009]">{value}</p>
            <p className="text-[10px] font-bold text-[#8c716f]">{label}</p>
          </div>
        ))}
      </div>

      <form className="space-y-2 rounded-2xl border border-[#ead9db] bg-[#fffdfc] p-3" onSubmit={handleCreateThread}>
        <input className="w-full rounded-xl border border-[#dfbfbd] bg-white px-3 py-2 text-xs outline-none focus:border-[#730014]" disabled={!canPersist || actionKey === 'create'} onChange={(event) => setTitle(event.target.value)} placeholder="Tiêu đề câu hỏi" value={title} />
        <textarea className="min-h-20 w-full resize-y rounded-xl border border-[#dfbfbd] bg-white px-3 py-2 text-xs leading-5 outline-none focus:border-[#730014]" disabled={!canPersist || actionKey === 'create'} onChange={(event) => setContent(event.target.value)} placeholder="Mô tả câu hỏi (tối thiểu 20 ký tự)" value={content} />
        <button className="rounded-xl bg-[#4b0009] px-3 py-2 text-xs font-extrabold text-white hover:bg-[#730014] disabled:opacity-50" disabled={!canPersist || actionKey === 'create'} type="submit">Gửi câu hỏi</button>
      </form>

      {message ? <p className="rounded-xl bg-[#fff0f1] px-3 py-2 text-xs font-semibold text-[#730014]">{message}</p> : null}

      <div className="flex flex-wrap gap-1.5">
        {FILTERS.map((item) => <button key={item.id} className={`rounded-full px-3 py-1.5 text-[11px] font-bold ${filter === item.id ? 'bg-[#8a0018] text-white' : 'border border-[#ead9db] bg-white text-[#6a5352]'}`} onClick={() => setFilter(item.id)} type="button">{item.label}</button>)}
      </div>

      {loading ? <p className="py-6 text-center text-xs text-[#8c716f]">Đang tải hỏi đáp...</p> : null}
      {!loading && !threads.length ? <div className="rounded-2xl border border-dashed border-[#dfbfbd] bg-[#fff8f8] p-5 text-center text-xs text-[#6a5352]">Chưa có câu hỏi nào cho bài học này.</div> : null}

      {!loading && threads.map((thread) => (
        <article key={thread.id} className="rounded-2xl border border-[#ead9db] bg-white p-3 shadow-sm">
          <div className="flex items-start justify-between gap-2">
            <div>
              <h4 className="text-sm font-extrabold text-[#1f2430]">{thread.title}</h4>
              <p className="mt-1 text-[10px] text-[#8c716f]">{thread.authorName} · {formatDate(thread.createdAt)}</p>
            </div>
            {thread.resolved ? <span className="rounded-full bg-emerald-50 px-2 py-1 text-[9px] font-bold text-emerald-700">Đã giải quyết</span> : null}
          </div>
          <p className="mt-2 whitespace-pre-line text-xs leading-5 text-[#5f5353]">{thread.content}</p>
          <div className="mt-3 flex flex-wrap items-center gap-1 border-t border-[#f0e3e4] pt-2">
            {REACTIONS.map((reaction) => <button key={reaction.type} title={reaction.label} className={`inline-flex items-center gap-1 rounded-lg px-2 py-1 text-[10px] font-bold ${thread.myReaction === reaction.type ? 'bg-[#fff0f1] text-[#8a0018]' : 'text-[#6a5352] hover:bg-[#fff8f8]'}`} disabled={Boolean(actionKey)} onClick={() => runThreadAction(`reaction:${thread.id}`, () => courseApi.toggleDiscussionThreadReaction(thread.id, reaction.type))} type="button"><span className="material-symbols-outlined text-[14px]">{reaction.icon}</span>{thread.reactionCounts?.[reaction.type] || 0}</button>)}
            <button className="rounded-lg px-2 py-1 text-[10px] font-bold text-[#6a5352] hover:bg-[#fff8f8]" onClick={() => setExpanded((current) => ({ ...current, [thread.id]: !current[thread.id] }))} type="button">Trả lời ({thread.replyCount || 0})</button>
            <span className="ml-auto text-[10px] text-[#8c716f]">{reactionTotal(thread.reactionCounts)} cảm xúc</span>
            <button className="text-[10px] font-bold text-rose-700" disabled={Boolean(actionKey)} onClick={() => runThreadAction(`report:${thread.id}`, () => courseApi.reportDiscussionThread(thread.id, { reason: 'Nội dung không phù hợp.' }))} type="button">Báo cáo</button>
          </div>

          {expanded[thread.id] ? <div className="mt-3 space-y-2 border-l-2 border-[#ead9db] pl-3">
            {(thread.replies || []).map((reply) => <div key={reply.id} className="rounded-xl bg-[#f4f7fb] p-2.5">
              <p className="text-[10px] font-bold text-[#1f2430]">{reply.authorName} · {formatDate(reply.createdAt)}</p>
              <p className="mt-1 text-xs leading-5 text-[#5f5353]">{reply.content}</p>
              <div className="mt-2 flex flex-wrap gap-2 text-[10px] font-bold">
                <button className="text-[#730014]" disabled={Boolean(actionKey)} onClick={() => runThreadAction(`helpful:${reply.id}`, async () => { await courseApi.toggleDiscussionReplyHelpful(reply.id); return null; })} type="button">Hữu ích ({reply.helpfulCount || 0})</button>
                {!thread.resolved ? <button className="text-emerald-700" disabled={Boolean(actionKey)} onClick={() => runThreadAction(`resolved:${reply.id}`, () => courseApi.markDiscussionResolved(thread.id, reply.id))} type="button">Đánh dấu lời giải</button> : null}
                <button className="text-rose-700" disabled={Boolean(actionKey)} onClick={() => runThreadAction(`report-reply:${reply.id}`, () => courseApi.reportDiscussionReply(reply.id, { reason: 'Nội dung không phù hợp.' }))} type="button">Báo cáo</button>
              </div>
            </div>)}
            <div className="flex gap-2">
              <textarea className="min-h-16 flex-1 resize-y rounded-xl border border-[#dfbfbd] px-3 py-2 text-xs outline-none" disabled={!canPersist} onChange={(event) => setReplies((current) => ({ ...current, [thread.id]: event.target.value }))} placeholder="Viết câu trả lời..." value={replies[thread.id] || ''} />
              <button className="self-end rounded-xl bg-[#4b0009] px-3 py-2 text-xs font-bold text-white disabled:opacity-50" disabled={!canPersist || actionKey === `reply:${thread.id}`} onClick={() => handleReply(thread.id)} type="button">Gửi</button>
            </div>
          </div> : null}
        </article>
      ))}
    </div>
  );
};

export default WorkspaceLessonDiscussion;
