import { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, CheckCircle2, Flag, MessageCircle, Send, ThumbsUp } from 'lucide-react';
import courseApi from '../../api/courseApi';
import { hasAccessToken } from '../../utils/auth';

const FILTERS = [
  { id: 'ALL', label: 'Tất cả' },
  { id: 'UNANSWERED', label: 'Chưa trả lời' },
  { id: 'RESOLVED', label: 'Đã giải quyết' },
  { id: 'HELPFUL', label: 'Hữu ích nhất' },
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

const CourseDiscussionSection = ({ courseId }) => {
  const loggedIn = hasAccessToken();
  const [filter, setFilter] = useState('ALL');
  const [threads, setThreads] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [questionTitle, setQuestionTitle] = useState('');
  const [questionContent, setQuestionContent] = useState('');
  const [replyContentByThread, setReplyContentByThread] = useState({});
  const [submitting, setSubmitting] = useState(false);

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
      setError(resolveErrorMessage(err, 'Không thể tải thảo luận. Vui lòng thử lại.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDiscussions(filter);
  }, [courseId, filter]);

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
      await courseApi.createCourseDiscussion(courseId, {
        title: questionTitle.trim(),
        content: questionContent.trim(),
      });
      setQuestionTitle('');
      setQuestionContent('');
      setMessage('Câu hỏi đã được gửi. Nếu cần kiểm duyệt, nội dung sẽ hiển thị sau khi được xem xét.');
      await loadDiscussions(filter);
    } catch (err) {
      setMessage(resolveErrorMessage(err, 'Không thể gửi câu hỏi. Vui lòng thử lại.'));
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

    try {
      await courseApi.createDiscussionReply(threadId, { content });
      setReplyContentByThread((current) => ({ ...current, [threadId]: '' }));
      setMessage('Câu trả lời đã được gửi.');
      await loadDiscussions(filter);
    } catch (err) {
      setMessage(resolveErrorMessage(err, 'Không thể gửi câu trả lời. Vui lòng thử lại.'));
    }
  };

  const handleHelpful = async (replyId) => {
    setMessage('');
    if (!loggedIn) {
      setMessage('Bạn cần đăng nhập để đánh dấu câu trả lời hữu ích.');
      return;
    }
    try {
      await courseApi.toggleDiscussionReplyHelpful(replyId);
      await loadDiscussions(filter);
    } catch (err) {
      setMessage(resolveErrorMessage(err, 'Không thể cập nhật đánh dấu hữu ích. Vui lòng thử lại.'));
    }
  };

  const handleResolved = async (threadId, replyId = null) => {
    setMessage('');
    if (!loggedIn) {
      setMessage('Bạn cần đăng nhập để đánh dấu đã giải quyết.');
      return;
    }
    try {
      await courseApi.markDiscussionResolved(threadId, replyId);
      setMessage('Thảo luận đã được đánh dấu là đã giải quyết.');
      await loadDiscussions(filter);
    } catch (err) {
      setMessage(resolveErrorMessage(err, 'Không thể đánh dấu đã giải quyết. Vui lòng thử lại.'));
    }
  };

  const handleReport = async (type, id) => {
    setMessage('');
    if (!loggedIn) {
      setMessage('Bạn cần đăng nhập để báo cáo nội dung.');
      return;
    }
    try {
      if (type === 'reply') {
        await courseApi.reportDiscussionReply(id, { reason: 'Nội dung không phù hợp với quy tắc cộng đồng.' });
      } else {
        await courseApi.reportDiscussionThread(id, { reason: 'Nội dung không phù hợp với quy tắc cộng đồng.' });
      }
      setMessage('Cảm ơn bạn đã báo cáo. Nội dung sẽ được xem xét.');
      await loadDiscussions(filter);
    } catch (err) {
      setMessage(resolveErrorMessage(err, 'Không thể gửi báo cáo. Vui lòng thử lại.'));
    }
  };

  return (
    <section className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-[0.24em] text-[#8a0018]">Thảo luận cộng đồng</p>
          <h2 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Hỏi đáp về khóa học</h2>
          <p className="mt-3 max-w-3xl text-sm leading-7 text-[#584140]">
            Đây là nơi học viên trao đổi với nhau và đội ngũ vận hành nội dung có thể kiểm duyệt hoặc hỗ trợ khi cần.
          </p>
        </div>
        <div className="grid grid-cols-3 gap-2 rounded-3xl border border-[#f0d8db] bg-[#fff8f8] p-3 text-center text-sm">
          <div>
            <p className="font-extrabold text-[#4b0009]">{discussionStats.total}</p>
            <p className="text-xs text-[#745d5c]">Chủ đề</p>
          </div>
          <div>
            <p className="font-extrabold text-[#4b0009]">{discussionStats.unanswered}</p>
            <p className="text-xs text-[#745d5c]">Chưa trả lời</p>
          </div>
          <div>
            <p className="font-extrabold text-[#4b0009]">{discussionStats.resolved}</p>
            <p className="text-xs text-[#745d5c]">Đã giải quyết</p>
          </div>
        </div>
      </div>

      <div className="mt-6 rounded-3xl border border-[#f0d8db] bg-[#fff8f8] p-5">
        <div className="flex items-start gap-3">
          <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-[#8a0018]" />
          <div className="text-sm leading-7 text-[#584140]">
            <p className="font-extrabold text-[#4b0009]">Quy tắc thảo luận</p>
            <p>Không sử dụng từ khiếm nhã, xúc phạm, gây chia rẽ, kích động thù hằn hoặc nội dung chống đối. Nội dung vi phạm sẽ được đưa vào trạng thái chờ kiểm duyệt.</p>
          </div>
        </div>
      </div>

      <form className="mt-6 rounded-3xl border border-[#dfbfbd]/50 bg-white p-5 shadow-sm" onSubmit={handleCreateQuestion}>
        <div className="flex items-center gap-2 text-[#4b0009]">
          <MessageCircle className="h-5 w-5" />
          <h3 className="font-['Manrope'] text-xl font-extrabold">Đặt câu hỏi</h3>
        </div>
        {!loggedIn ? (
          <p className="mt-3 rounded-2xl bg-[#fff4f4] px-4 py-3 text-sm font-semibold text-[#8a0018]">
            Bạn cần đăng nhập để đặt câu hỏi.
          </p>
        ) : null}
        <div className="mt-4 grid gap-3">
          <input
            className="rounded-2xl border border-[#dfbfbd] px-4 py-3 text-sm outline-none transition focus:border-[#730014] focus:ring-4 focus:ring-[#730014]/10"
            disabled={!loggedIn || submitting}
            onChange={(event) => setQuestionTitle(event.target.value)}
            placeholder="Nhập tiêu đề câu hỏi..."
            value={questionTitle}
          />
          <textarea
            className="min-h-[120px] rounded-2xl border border-[#dfbfbd] px-4 py-3 text-sm outline-none transition focus:border-[#730014] focus:ring-4 focus:ring-[#730014]/10"
            disabled={!loggedIn || submitting}
            onChange={(event) => setQuestionContent(event.target.value)}
            placeholder="Mô tả điều bạn đang thắc mắc..."
            value={questionContent}
          />
        </div>
        <button
          className="mt-4 inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-60"
          disabled={!loggedIn || submitting}
          type="submit"
        >
          <Send className="h-4 w-4" />
          Gửi câu hỏi
        </button>
      </form>

      {message ? (
        <div className="mt-5 rounded-2xl border border-[#dfbfbd]/60 bg-[#fff8f8] px-4 py-3 text-sm font-semibold text-[#730014]">
          {message}
        </div>
      ) : null}

      <div className="mt-6 flex flex-wrap gap-2">
        {FILTERS.map((item) => (
          <button
            className={`rounded-full px-4 py-2 text-sm font-extrabold transition ${
              filter === item.id ? 'bg-[#4b0009] text-white shadow-sm' : 'border border-[#dfbfbd] bg-white text-[#730014] hover:bg-[#fff2f3]'
            }`}
            key={item.id}
            onClick={() => setFilter(item.id)}
            type="button"
          >
            {item.label}
          </button>
        ))}
      </div>

      <div className="mt-6 space-y-4">
        {loading ? (
          <div className="rounded-3xl border border-[#dfbfbd]/30 bg-[#fff8f8] p-8 text-center text-sm font-semibold text-[#584140]">
            Đang tải thảo luận...
          </div>
        ) : error ? (
          <div className="rounded-3xl border border-[#ba1a1a]/20 bg-[#ffdad6] p-8 text-center text-sm font-semibold text-[#93000a]">
            <p>{error}</p>
            <button className="mt-4 rounded-2xl bg-[#4b0009] px-5 py-3 text-white" onClick={() => loadDiscussions(filter)} type="button">
              Thử lại
            </button>
          </div>
        ) : threads.length === 0 ? (
          <div className="rounded-3xl border border-dashed border-[#dfbfbd] bg-white p-8 text-center">
            <h3 className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Chưa có thảo luận nào cho khóa học này.</h3>
            <p className="mt-2 text-sm text-[#584140]">Bạn có thể là người đầu tiên đặt câu hỏi để cùng cộng đồng học tốt hơn.</p>
          </div>
        ) : threads.map((thread) => (
          <article className="rounded-3xl border border-[#dfbfbd]/45 bg-white p-5 shadow-sm" key={thread.id}>
            <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{thread.title}</h3>
                  {thread.resolved ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-[#f2fff5] px-3 py-1 text-xs font-extrabold text-[#16723a]">
                      <CheckCircle2 className="h-3.5 w-3.5" />
                      Đã giải quyết
                    </span>
                  ) : null}
                  {thread.status === 'PENDING_REVIEW' ? (
                    <span className="rounded-full bg-[#fff2f3] px-3 py-1 text-xs font-extrabold text-[#8a0018]">Chờ kiểm duyệt</span>
                  ) : null}
                </div>
                <p className="mt-1 text-xs font-semibold text-[#8b7473]">
                  {thread.authorName} • {formatDate(thread.createdAt)}
                </p>
              </div>
              <button
                className="inline-flex items-center gap-2 rounded-full border border-[#dfbfbd] px-3 py-2 text-xs font-extrabold text-[#730014] transition hover:bg-[#fff2f3]"
                onClick={() => handleReport('thread', thread.id)}
                type="button"
              >
                <Flag className="h-4 w-4" />
                Báo cáo
              </button>
            </div>
            <p className="mt-4 whitespace-pre-line text-sm leading-7 text-[#584140]">{thread.content}</p>

            <div className="mt-5 space-y-3">
              {thread.replies?.map((reply) => (
                <div className="rounded-2xl border border-[#f0d8db] bg-[#fffafa] p-4" key={reply.id}>
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="text-sm font-extrabold text-[#2b2828]">
                      {reply.authorName}
                      {reply.accepted ? <span className="ml-2 text-xs text-[#16723a]">Câu trả lời được chọn</span> : null}
                    </p>
                    <p className="text-xs font-semibold text-[#8b7473]">{formatDate(reply.createdAt)}</p>
                  </div>
                  <p className="mt-3 whitespace-pre-line text-sm leading-7 text-[#584140]">{reply.content}</p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    <button
                      className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-2 text-xs font-extrabold text-[#730014] transition hover:bg-[#fff2f3]"
                      onClick={() => handleHelpful(reply.id)}
                      type="button"
                    >
                      <ThumbsUp className="h-4 w-4" />
                      Hữu ích ({reply.helpfulCount || 0})
                    </button>
                    {!thread.resolved ? (
                      <button
                        className="rounded-full bg-white px-3 py-2 text-xs font-extrabold text-[#730014] transition hover:bg-[#fff2f3]"
                        onClick={() => handleResolved(thread.id, reply.id)}
                        type="button"
                      >
                        Đánh dấu đã giải quyết
                      </button>
                    ) : null}
                    <button
                      className="rounded-full bg-white px-3 py-2 text-xs font-extrabold text-[#730014] transition hover:bg-[#fff2f3]"
                      onClick={() => handleReport('reply', reply.id)}
                      type="button"
                    >
                      Báo cáo
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-4 grid gap-3">
              <textarea
                className="min-h-[92px] rounded-2xl border border-[#dfbfbd] px-4 py-3 text-sm outline-none transition focus:border-[#730014] focus:ring-4 focus:ring-[#730014]/10"
                disabled={!loggedIn}
                onChange={(event) => setReplyContentByThread((current) => ({ ...current, [thread.id]: event.target.value }))}
                placeholder={loggedIn ? 'Viết câu trả lời của bạn...' : 'Bạn cần đăng nhập để trả lời.'}
                value={replyContentByThread[thread.id] || ''}
              />
              <button
                className="w-fit rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-60"
                disabled={!loggedIn}
                onClick={() => handleCreateReply(thread.id)}
                type="button"
              >
                Gửi trả lời
              </button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
};

export default CourseDiscussionSection;
