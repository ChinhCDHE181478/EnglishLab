import { useEffect, useState } from 'react';
import { ClipboardList, LifeBuoy, MessageSquarePlus, Plus, Send, XCircle } from 'lucide-react';
import supportApi from '../api/supportApi';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import BrandedSelect from '../components/ui/BrandedSelect';
import {
  formatSupportTime,
  isSupportTicketTerminal,
  supportCategoryLabels,
  supportCategoryOptions,
  supportPriorityLabels,
  supportStatusClasses,
  supportStatusLabels,
  supportApiError,
} from '../utils/supportTicketLabels';

const emptyForm = { subject: '', category: 'ACCOUNT', message: '' };

export default function SupportTicketsPage() {
  const [tickets, setTickets] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [comment, setComment] = useState('');
  const [showForm, setShowForm] = useState(true);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadTickets = async (preferredId) => {
    setLoading(true);
    setError('');
    try {
      const data = await supportApi.listMyTickets();
      const items = Array.isArray(data) ? data : [];
      setTickets(items);
      const nextId = preferredId ?? selectedId ?? null;
      setSelectedId(nextId && items.some((item) => item.id === nextId) ? nextId : null);
      if (!nextId || !items.some((item) => item.id === nextId)) setDetail(null);
    } catch (err) {
      setError(supportApiError(err, 'Không tải được danh sách ticket.'));
    } finally {
      setLoading(false);
    }
  };

  const loadDetail = async (ticketId) => {
    if (!ticketId) return;
    setDetailLoading(true);
    setError('');
    try {
      setDetail(await supportApi.getMyTicket(ticketId));
    } catch (err) {
      setError(supportApiError(err, 'Không tải được chi tiết ticket.'));
    } finally {
      setDetailLoading(false);
    }
  };

  useEffect(() => { loadTickets(); }, []);
  useEffect(() => {
    if (selectedId) loadDetail(selectedId);
    else setDetail(null);
  }, [selectedId]);

  const canCreate = form.subject.trim().length >= 5 && form.message.trim().length >= 10;

  const createTicket = async (event) => {
    event.preventDefault();
    if (!canCreate) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      const created = await supportApi.createTicket(form);
      setForm(emptyForm);
      setShowForm(false);
      setSuccess(`Đã tạo ticket #${created.id}. Bạn có thể theo dõi trạng thái trong danh sách bên dưới.`);
      await loadTickets(created.id);
      setSelectedId(created.id);
      setDetail(created);
    } catch (err) {
      setError(supportApiError(err, 'Không thể tạo ticket.'));
    } finally {
      setWorking(false);
    }
  };

  const addComment = async (event) => {
    event.preventDefault();
    if (!comment.trim() || !detail) return;
    setWorking(true);
    setError('');
    try {
      const updated = await supportApi.replyAsLearner(detail.id, comment.trim());
      setComment('');
      setDetail(updated);
      await loadTickets(updated.id);
      setSuccess('Đã thêm comment vào ticket.');
    } catch (err) {
      setError(supportApiError(err, 'Không thể thêm comment.'));
    } finally {
      setWorking(false);
    }
  };

  const changeStatus = async (status) => {
    if (!detail) return;
    setWorking(true);
    setError('');
    try {
      const updated = await supportApi.updateMyTicketStatus(detail.id, status);
      setDetail(updated);
      setSuccess(status === 'CLOSED' ? 'Đã đóng ticket.' : 'Đã mở lại ticket.');
      await loadTickets(updated.id);
    } catch (err) {
      setError(supportApiError(err, 'Không thể cập nhật trạng thái ticket.'));
    } finally {
      setWorking(false);
    }
  };

  const messages = detail?.messages || [];
  const opening = messages[0] || null;
  const comments = messages.slice(1);

  return (
    <LearnerPageShell
      title="Support Ticket"
      description="Tạo ticket bằng form, theo dõi trạng thái và trao đổi qua comment (không chat realtime)."
      actions={(
        <button
          className="inline-flex items-center gap-2 rounded-2xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white"
          onClick={() => {
            setShowForm((value) => !value);
            setSelectedId(null);
          }}
          type="button"
        >
          {showForm ? <XCircle className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
          {showForm ? 'Ẩn form tạo' : 'Tạo ticket mới'}
        </button>
      )}
    >
      <div className="space-y-5">
        {error ? <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700">{error}</div> : null}
        {success ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700">{success}</div> : null}

        {/* 1. Form tạo ticket */}
        {showForm ? (
          <form className="rounded-[28px] border border-[#dfbfbd]/50 bg-white p-5 shadow-sm md:p-7" onSubmit={createTicket}>
            <div className="flex items-center gap-2">
              <LifeBuoy className="h-5 w-5 text-[#730014]" />
              <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">Tạo Support Ticket</h2>
            </div>
            <p className="mt-2 text-sm text-[#8b706e]">
              Điền form bên dưới. Sau khi gửi, ticket sẽ xuất hiện trong danh sách để bạn theo dõi trạng thái.
            </p>
            <div className="mt-5 grid gap-4 md:grid-cols-[1fr_240px]">
              <label className="space-y-2 text-sm font-bold text-[#584140]">
                Tiêu đề ticket
                <input
                  className="w-full rounded-2xl border border-[#dfbfbd] bg-[#fffafa] px-4 py-3 font-medium outline-none focus:border-[#730014]"
                  maxLength={160}
                  onChange={(event) => setForm({ ...form, subject: event.target.value })}
                  placeholder="Ví dụ: Không truy cập được khóa học đã mua"
                  value={form.subject}
                />
              </label>
              <label className="space-y-2 text-sm font-bold text-[#584140]">
                Nhóm vấn đề
                <BrandedSelect
                  onChange={(event) => setForm({ ...form, category: event.target.value })}
                  options={supportCategoryOptions}
                  value={form.category}
                />
              </label>
            </div>
            <label className="mt-4 block space-y-2 text-sm font-bold text-[#584140]">
              Mô tả chi tiết
              <textarea
                className="min-h-36 w-full resize-y rounded-2xl border border-[#dfbfbd] bg-[#fffafa] px-4 py-3 font-medium leading-6 outline-none focus:border-[#730014]"
                maxLength={5000}
                onChange={(event) => setForm({ ...form, message: event.target.value })}
                placeholder="Mô tả rõ vấn đề, thời điểm xảy ra, thao tác đã thử..."
                value={form.message}
              />
            </label>
            <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
              <p className="text-xs text-[#8b706e]">Tiêu đề ≥ 5 ký tự · Mô tả ≥ 10 ký tự</p>
              <button
                className="inline-flex items-center gap-2 rounded-2xl bg-[#730014] px-5 py-3 text-sm font-bold text-white disabled:opacity-50"
                disabled={!canCreate || working}
                type="submit"
              >
                <Send className="h-4 w-4" />
                {working ? 'Đang tạo...' : 'Gửi ticket'}
              </button>
            </div>
          </form>
        ) : null}

        {/* 2. Danh sách ticket của người dùng */}
        <section className="overflow-hidden rounded-[28px] border border-[#dfbfbd]/40 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-[#f0e4e2] px-5 py-4">
            <div className="flex items-center gap-2">
              <ClipboardList className="h-5 w-5 text-[#730014]" />
              <h2 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Ticket của tôi</h2>
            </div>
            <span className="text-xs font-bold text-[#8b706e]">{tickets.length} ticket</span>
          </div>

          {loading ? (
            <p className="px-5 py-10 text-sm text-[#8b706e]">Đang tải danh sách...</p>
          ) : tickets.length === 0 ? (
            <div className="px-5 py-12 text-center text-sm text-[#8b706e]">
              <LifeBuoy className="mx-auto mb-3 h-8 w-8 opacity-60" />
              Chưa có ticket. Hãy tạo ticket bằng form phía trên.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full text-left">
                <thead className="bg-[#fff8f8] text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">
                  <tr>
                    <th className="px-5 py-3">Mã</th>
                    <th className="px-5 py-3">Tiêu đề</th>
                    <th className="px-5 py-3">Nhóm</th>
                    <th className="px-5 py-3">Trạng thái</th>
                    <th className="px-5 py-3">Cập nhật</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#f0e4e2]">
                  {tickets.map((ticket) => (
                    <tr
                      className={`cursor-pointer transition hover:bg-[#fffafa] ${selectedId === ticket.id ? 'bg-[#fff3f4]' : ''}`}
                      key={ticket.id}
                      onClick={() => {
                        setSelectedId(ticket.id);
                        setShowForm(false);
                      }}
                    >
                      <td className="px-5 py-3.5 text-sm font-extrabold text-[#730014]">#{ticket.id}</td>
                      <td className="px-5 py-3.5 text-sm font-bold text-[#2b2828]">{ticket.subject}</td>
                      <td className="px-5 py-3.5 text-sm text-[#584140]">{supportCategoryLabels[ticket.category]}</td>
                      <td className="px-5 py-3.5">
                        <span className={`rounded-full border px-2.5 py-0.5 text-[11px] font-bold ${supportStatusClasses[ticket.status]}`}>
                          {supportStatusLabels[ticket.status]}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-xs text-[#8b706e]">{formatSupportTime(ticket.updatedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* 3. Chi tiết + theo dõi trạng thái + comment */}
        {selectedId ? (
          <section className="rounded-[28px] border border-[#dfbfbd]/40 bg-white p-5 shadow-sm md:p-7">
            {detailLoading ? <p className="text-sm text-[#8b706e]">Đang tải chi tiết ticket...</p> : null}
            {!detailLoading && detail ? (
              <div className="space-y-6">
                <div className="flex flex-wrap items-start justify-between gap-4 border-b border-[#f0e4e2] pb-5">
                  <div>
                    <p className="text-xs font-extrabold uppercase tracking-wider text-[#730014]">
                      Ticket #{detail.id} · {supportCategoryLabels[detail.category]}
                    </p>
                    <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{detail.subject}</h2>
                    <p className="mt-2 text-xs text-[#8b706e]">
                      Tạo: {formatSupportTime(detail.createdAt)} · Ưu tiên: {supportPriorityLabels[detail.priority]}
                      {detail.assigneeName ? ` · Người xử lý: ${detail.assigneeName}` : ' · Chưa có người xử lý'}
                    </p>
                  </div>
                  <div className="flex flex-col items-end gap-2">
                    <span className={`rounded-full border px-3 py-1 text-xs font-bold ${supportStatusClasses[detail.status]}`}>
                      {supportStatusLabels[detail.status]}
                    </span>
                    {isSupportTicketTerminal(detail.status) ? (
                      <button
                        className="text-xs font-bold text-[#730014] hover:underline"
                        disabled={working}
                        onClick={() => changeStatus('OPEN')}
                        type="button"
                      >
                        Mở lại ticket
                      </button>
                    ) : (
                      <button
                        className="text-xs font-bold text-slate-500 hover:text-rose-700"
                        disabled={working}
                        onClick={() => changeStatus('CLOSED')}
                        type="button"
                      >
                        Đóng ticket
                      </button>
                    )}
                  </div>
                </div>

                {/* Mô tả ban đầu từ form */}
                <div className="rounded-2xl border border-[#f0e4e2] bg-[#fffafa] p-4">
                  <p className="text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">Nội dung ticket</p>
                  <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-[#2b2828]">
                    {opening?.body || 'Không có mô tả.'}
                  </p>
                  <p className="mt-3 text-[11px] text-[#8b706e]">
                    {opening ? `${opening.authorName || 'Bạn'} · ${formatSupportTime(opening.createdAt)}` : null}
                  </p>
                </div>

                {/* Timeline trạng thái gọn */}
                <div className="grid gap-2 sm:grid-cols-4">
                  {[
                    { key: 'OPEN', label: 'Đã gửi' },
                    { key: 'IN_PROGRESS', label: 'Đang xử lý' },
                    { key: 'WAITING_FOR_LEARNER', label: 'Chờ phản hồi' },
                    { key: 'RESOLVED', label: 'Hoàn tất' },
                  ].map((step) => {
                    const active = detail.status === step.key
                      || (step.key === 'RESOLVED' && isSupportTicketTerminal(detail.status))
                      || (step.key === 'IN_PROGRESS' && detail.status === 'WAITING_FOR_LEARNER');
                    return (
                      <div
                        key={step.key}
                        className={`rounded-xl border px-3 py-2 text-center text-[11px] font-bold ${
                          active ? 'border-[#730014] bg-[#fff1f3] text-[#730014]' : 'border-slate-100 bg-slate-50 text-slate-400'
                        }`}
                      >
                        {step.label}
                      </div>
                    );
                  })}
                </div>

                {/* Comments (không phải chat bubble) */}
                <div>
                  <div className="mb-3 flex items-center gap-2">
                    <MessageSquarePlus className="h-4 w-4 text-[#730014]" />
                    <h3 className="text-sm font-extrabold text-[#2b2828]">Comment trên ticket</h3>
                    <span className="text-xs text-[#8b706e]">({comments.length})</span>
                  </div>

                  {comments.length === 0 ? (
                    <p className="rounded-2xl border border-dashed border-[#dfbfbd] px-4 py-6 text-center text-sm text-[#8b706e]">
                      Chưa có comment. Support sẽ phản hồi tại đây; bạn cũng có thể bổ sung thông tin.
                    </p>
                  ) : (
                    <div className="space-y-3">
                      {comments.map((item) => (
                        <article key={item.id} className="rounded-2xl border border-[#f0e4e2] bg-white p-4">
                          <div className="flex flex-wrap items-center justify-between gap-2">
                            <p className="text-xs font-extrabold text-[#2b2828]">
                              {item.staffMessage ? `Support · ${item.authorName}` : 'Bạn'}
                              {item.staffMessage ? (
                                <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-bold text-slate-600">
                                  Đội hỗ trợ
                                </span>
                              ) : (
                                <span className="ml-2 rounded-full bg-[#fff1f3] px-2 py-0.5 text-[10px] font-bold text-[#730014]">
                                  Học viên
                                </span>
                              )}
                            </p>
                            <p className="text-[11px] text-[#8b706e]">{formatSupportTime(item.createdAt)}</p>
                          </div>
                          <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-[#584140]">{item.body}</p>
                        </article>
                      ))}
                    </div>
                  )}

                  {!isSupportTicketTerminal(detail.status) ? (
                    <form className="mt-4 space-y-3" onSubmit={addComment}>
                      <label className="block space-y-2 text-sm font-bold text-[#584140]">
                        Thêm comment
                        <textarea
                          className="min-h-24 w-full resize-y rounded-2xl border border-[#dfbfbd] bg-[#fffafa] px-4 py-3 text-sm leading-6 outline-none focus:border-[#730014]"
                          maxLength={5000}
                          onChange={(event) => setComment(event.target.value)}
                          placeholder="Bổ sung thông tin hoặc phản hồi cho Support..."
                          value={comment}
                        />
                      </label>
                      <div className="flex justify-end">
                        <button
                          className="inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white disabled:opacity-50"
                          disabled={!comment.trim() || working}
                          type="submit"
                        >
                          <Send className="h-4 w-4" />
                          {working ? 'Đang gửi...' : 'Gửi comment'}
                        </button>
                      </div>
                    </form>
                  ) : (
                    <p className="mt-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm font-semibold text-slate-600">
                      Ticket đã đóng/giải quyết. Mở lại nếu bạn cần trao đổi thêm.
                    </p>
                  )}
                </div>
              </div>
            ) : null}
          </section>
        ) : null}
      </div>
    </LearnerPageShell>
  );
}
