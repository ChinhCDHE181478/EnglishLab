import { useEffect, useMemo, useState } from 'react';
import { ClipboardList, Inbox, MessageSquarePlus, RefreshCw, Search, Send, UserCheck } from 'lucide-react';
import { useLocation } from 'react-router-dom';
import supportApi from '../../api/supportApi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import {
  ERROR_NOTICE_CLASS,
  PRIMARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
} from '../../utils/formStyles';
import {
  formatSupportTime,
  isSupportTicketTerminal,
  supportCategoryLabels,
  supportPriorityClasses,
  supportPriorityLabels,
  supportPriorityOptions,
  supportStatusClasses,
  supportStatusLabels,
  supportStatusOptions,
  supportApiError,
} from '../../utils/supportTicketLabels';

const allStatusOptions = [{ value: 'ALL', label: 'Tất cả trạng thái' }, ...supportStatusOptions];
const allPriorityOptions = [{ value: 'ALL', label: 'Tất cả ưu tiên' }, ...supportPriorityOptions];
const PAGE_SIZE = 8;

export default function ManagerSupportTicketsPage() {
  const location = useLocation();
  const apiScope = location.pathname.startsWith('/manager/') ? 'manager' : 'staff';
  const [tickets, setTickets] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [status, setStatus] = useState('ALL');
  const [priority, setPriority] = useState('ALL');
  const [keyword, setKeyword] = useState('');
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadQueue = async (preferredId) => {
    setLoading(true);
    setError('');
    try {
      const data = await supportApi.listQueue({
        status: status === 'ALL' ? undefined : status,
        priority: priority === 'ALL' ? undefined : priority,
      }, apiScope);
      const items = Array.isArray(data) ? data : [];
      setTickets(items);
      const nextId = preferredId ?? selectedId ?? null;
      const valid = nextId && items.some((item) => item.id === nextId) ? nextId : null;
      setSelectedId(valid);
      if (!valid) setDetail(null);
    } catch (err) {
      setError(supportApiError(err, 'Không tải được danh sách support ticket.'));
    } finally {
      setLoading(false);
    }
  };

  const loadDetail = async (ticketId) => {
    if (!ticketId) return;
    setDetailLoading(true);
    setError('');
    try {
      setDetail(await supportApi.getForStaff(ticketId, apiScope));
    } catch (err) {
      setError(supportApiError(err, 'Không tải được chi tiết ticket.'));
    } finally {
      setDetailLoading(false);
    }
  };

  useEffect(() => { loadQueue(); }, [apiScope, status, priority]);
  useEffect(() => {
    if (selectedId) loadDetail(selectedId);
    else setDetail(null);
  }, [selectedId]);

  const filtered = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) return tickets;
    return tickets.filter((ticket) => [ticket.subject, ticket.requesterName, ticket.requesterEmail, ticket.id]
      .filter((value) => value !== null && value !== undefined)
      .some((value) => String(value).toLowerCase().includes(normalized)));
  }, [keyword, tickets]);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    filtered,
    PAGE_SIZE,
    `${status}-${priority}-${keyword}`,
  );

  const refresh = async (updated) => {
    setDetail(updated);
    await loadQueue(updated?.id);
  };

  const claim = async () => {
    if (!detail) return;
    setWorking(true);
    setError('');
    try {
      await refresh(await supportApi.claim(detail.id, apiScope));
      setSuccess('Đã nhận xử lý ticket.');
    } catch (err) {
      setError(supportApiError(err, 'Không thể nhận xử lý ticket.'));
    } finally {
      setWorking(false);
    }
  };

  const updateTicket = async (data) => {
    if (!detail) return;
    setWorking(true);
    setError('');
    setSuccess('');
    try {
      await refresh(await supportApi.updateAsStaff(detail.id, data, apiScope));
      setSuccess('Đã cập nhật ticket.');
    } catch (err) {
      setError(supportApiError(err, 'Không thể cập nhật ticket.'));
    } finally {
      setWorking(false);
    }
  };

  const addComment = async (event) => {
    event.preventDefault();
    if (!detail || !comment.trim()) return;
    setWorking(true);
    setError('');
    try {
      const updated = await supportApi.replyAsStaff(detail.id, comment.trim(), apiScope);
      setComment('');
      await refresh(updated);
      setSuccess('Đã thêm comment cho học viên.');
    } catch (err) {
      setError(supportApiError(err, 'Không thể thêm comment.'));
    } finally {
      setWorking(false);
    }
  };

  const messages = detail?.messages || [];
  const opening = messages[0] || null;
  const comments = messages.slice(1);

  return (
    <div className="space-y-5">
      {error ? <div className={ERROR_NOTICE_CLASS}>{error}</div> : null}
      {success ? <div className={SUCCESS_NOTICE_CLASS}>{success}</div> : null}

      {/* Danh sách ticket + toolbar gộp 1 card */}
      <section className="overflow-hidden rounded-[24px] border border-[#e8d9d8] bg-white shadow-sm">
        <div className="border-b border-[#f0e4e2] bg-gradient-to-r from-[#fffafa] to-white px-5 py-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2.5">
              <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#fff1f3] text-[#730014]">
                <ClipboardList className="h-4 w-4" />
              </span>
              <div>
                <h3 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Danh sách ticket</h3>
                <p className="text-[11px] font-semibold text-[#8b706e]">
                  {loading ? 'Đang tải...' : `${totalItems} ticket · ${PAGE_SIZE} ticket / trang`}
                </p>
              </div>
            </div>
            <button
              className="inline-flex items-center gap-1.5 rounded-xl border border-[#dfbfbd]/70 bg-white px-3.5 py-2 text-xs font-extrabold text-[#730014] transition hover:bg-[#fff1f3]"
              onClick={() => loadQueue()}
              type="button"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
              Tải lại
            </button>
          </div>

          <div className="mt-4 grid gap-2.5 lg:grid-cols-[1fr_180px_170px]">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#b89a98]" />
              <input
                className="w-full rounded-xl border border-[#e8d9d8] bg-white py-2.5 pl-10 pr-3 text-sm text-[#2b2828] outline-none transition placeholder:text-[#b89a98] focus:border-[#730014] focus:ring-2 focus:ring-[#730014]/10"
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Tìm mã, tiêu đề hoặc học viên..."
                value={keyword}
              />
            </div>
            <BrandedSelect onChange={(event) => setStatus(event.target.value)} options={allStatusOptions} value={status} />
            <BrandedSelect onChange={(event) => setPriority(event.target.value)} options={allPriorityOptions} value={priority} />
          </div>
        </div>

        {!loading && filtered.length === 0 ? (
          <div className="px-5 py-14 text-center">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-[#fff1f3] text-[#730014]">
              <Inbox className="h-7 w-7" />
            </div>
            <p className="mt-4 text-sm font-extrabold text-[#2b2828]">Không có ticket phù hợp</p>
            <p className="mt-1 text-xs text-[#8b706e]">Thử đổi bộ lọc hoặc từ khóa tìm kiếm.</p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full text-left">
                <thead>
                  <tr className="border-b border-[#f0e4e2] bg-[#fffbfb] text-[10px] font-extrabold uppercase tracking-[0.12em] text-[#8b706e]">
                    <th className="px-5 py-3">Mã</th>
                    <th className="px-5 py-3">Tiêu đề</th>
                    <th className="px-5 py-3">Học viên</th>
                    <th className="px-5 py-3">Nhóm</th>
                    <th className="px-5 py-3">Ưu tiên</th>
                    <th className="px-5 py-3">Trạng thái</th>
                    <th className="px-5 py-3 text-right">Cập nhật</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#f5eceb]">
                  {pageItems.map((ticket) => {
                    const active = selectedId === ticket.id;
                    return (
                      <tr
                        className={`cursor-pointer transition ${
                          active
                            ? 'bg-[#fff1f3] ring-1 ring-inset ring-[#730014]/15'
                            : 'hover:bg-[#fffafa]'
                        }`}
                        key={ticket.id}
                        onClick={() => setSelectedId(ticket.id)}
                      >
                        <td className="px-5 py-3.5">
                          <span className="inline-flex rounded-lg bg-[#fff1f3] px-2 py-1 text-xs font-extrabold text-[#730014]">
                            #{ticket.id}
                          </span>
                        </td>
                        <td className="max-w-[260px] px-5 py-3.5">
                          <p className="truncate text-sm font-extrabold text-[#2b2828]">{ticket.subject}</p>
                        </td>
                        <td className="px-5 py-3.5">
                          <p className="text-sm font-semibold text-[#2b2828]">{ticket.requesterName || '—'}</p>
                          <p className="text-[11px] text-[#8b706e]">{ticket.requesterEmail}</p>
                        </td>
                        <td className="px-5 py-3.5 text-sm font-medium text-[#584140]">
                          {supportCategoryLabels[ticket.category]}
                        </td>
                        <td className={`px-5 py-3.5 text-sm font-extrabold ${supportPriorityClasses[ticket.priority]}`}>
                          {supportPriorityLabels[ticket.priority]}
                        </td>
                        <td className="px-5 py-3.5">
                          <span className={`inline-flex rounded-full border px-2.5 py-0.5 text-[11px] font-extrabold ${supportStatusClasses[ticket.status]}`}>
                            {supportStatusLabels[ticket.status]}
                          </span>
                        </td>
                        <td className="px-5 py-3.5 text-right text-[11px] font-semibold text-[#8b706e]">
                          {formatSupportTime(ticket.updatedAt)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {totalPages > 1 ? (
              <div className="border-t border-[#f0e4e2] bg-[#fffbfb] px-5 py-3.5">
                <Pagination
                  page={page}
                  pageSize={PAGE_SIZE}
                  totalItems={totalItems}
                  totalPages={totalPages}
                  onChange={setPage}
                />
              </div>
            ) : null}
          </>
        )}
      </section>

      {/* Chi tiết + xử lý + comment */}
      {selectedId ? (
        <section className="overflow-hidden rounded-[24px] border border-[#e8d9d8] bg-white shadow-sm">
          {detailLoading ? (
            <p className="px-6 py-10 text-sm font-semibold text-[#8b706e]">Đang tải chi tiết ticket...</p>
          ) : null}
          {!detailLoading && detail ? (
            <div>
              <div className="border-b border-[#f0e4e2] bg-gradient-to-r from-[#fffafa] to-white px-6 py-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-lg bg-[#fff1f3] px-2 py-1 text-xs font-extrabold text-[#730014]">
                        #{detail.id}
                      </span>
                      <span className="text-xs font-bold text-[#8b706e]">
                        {supportCategoryLabels[detail.category]}
                      </span>
                      <span className={`rounded-full border px-2.5 py-0.5 text-[11px] font-extrabold ${supportStatusClasses[detail.status]}`}>
                        {supportStatusLabels[detail.status]}
                      </span>
                    </div>
                    <h3 className="mt-2 font-['Manrope'] text-xl font-extrabold text-[#2b2828] md:text-2xl">
                      {detail.subject}
                    </h3>
                    <p className="mt-2 text-sm font-semibold text-[#584140]">
                      {detail.requesterName}
                      <span className="font-medium text-[#8b706e]"> · {detail.requesterEmail}</span>
                    </p>
                    <p className="mt-1 text-[11px] font-semibold text-[#8b706e]">
                      Tạo {formatSupportTime(detail.createdAt)}
                      {' · '}
                      Phụ trách: {detail.assigneeName || 'Chưa nhận'}
                    </p>
                  </div>
                  {!detail.assigneeId && !isSupportTicketTerminal(detail.status) ? (
                    <button className={PRIMARY_BUTTON_CLASS} disabled={working} onClick={claim} type="button">
                      <UserCheck className="h-4 w-4" /> Nhận xử lý
                    </button>
                  ) : null}
                </div>
              </div>

              <div className="space-y-5 p-6">
                <div className="grid gap-3 sm:grid-cols-2">
                  <label className="space-y-1.5 text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">
                    Trạng thái xử lý
                    <BrandedSelect
                      disabled={working}
                      onChange={(event) => updateTicket({ status: event.target.value })}
                      options={supportStatusOptions}
                      value={detail.status}
                    />
                  </label>
                  <label className="space-y-1.5 text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">
                    Độ ưu tiên
                    <BrandedSelect
                      disabled={working}
                      onChange={(event) => updateTicket({ priority: event.target.value })}
                      options={supportPriorityOptions}
                      value={detail.priority}
                    />
                  </label>
                </div>

                <div className="rounded-2xl border border-[#f0e4e2] bg-[#fffafa] p-4">
                  <p className="text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">
                    Nội dung ticket
                  </p>
                  <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-[#2b2828]">
                    {opening?.body || 'Không có mô tả.'}
                  </p>
                  <p className="mt-3 text-[11px] font-semibold text-[#8b706e]">
                    {opening ? `${detail.requesterName} · ${formatSupportTime(opening.createdAt)}` : null}
                  </p>
                </div>

                <div>
                  <div className="mb-3 flex items-center gap-2">
                    <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-[#fff1f3] text-[#730014]">
                      <MessageSquarePlus className="h-4 w-4" />
                    </span>
                    <h4 className="text-sm font-extrabold text-[#2b2828]">Comment</h4>
                    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-bold text-slate-600">
                      {comments.length}
                    </span>
                  </div>

                  {comments.length === 0 ? (
                    <p className="rounded-2xl border border-dashed border-[#e8d9d8] bg-[#fffbfb] px-4 py-8 text-center text-sm font-semibold text-[#8b706e]">
                      Chưa có comment. Thêm comment để phản hồi học viên.
                    </p>
                  ) : (
                    <div className="space-y-2.5">
                      {comments.map((item) => (
                        <article
                          key={item.id}
                          className={`rounded-2xl border p-4 ${
                            item.staffMessage
                              ? 'border-[#f0e4e2] bg-white'
                              : 'border-slate-100 bg-slate-50/70'
                          }`}
                        >
                          <div className="flex flex-wrap items-center justify-between gap-2">
                            <p className="text-xs font-extrabold text-[#2b2828]">
                              {item.staffMessage ? item.authorName : detail.requesterName}
                              <span className={`ml-2 rounded-full px-2 py-0.5 text-[10px] font-bold ${
                                item.staffMessage
                                  ? 'bg-[#fff1f3] text-[#730014]'
                                  : 'bg-white text-slate-600 ring-1 ring-slate-200'
                              }`}
                              >
                                {item.staffMessage ? 'Support' : 'Học viên'}
                              </span>
                            </p>
                            <p className="text-[11px] font-semibold text-[#8b706e]">
                              {formatSupportTime(item.createdAt)}
                            </p>
                          </div>
                          <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-[#584140]">{item.body}</p>
                        </article>
                      ))}
                    </div>
                  )}

                  {!isSupportTicketTerminal(detail.status) ? (
                    <form className="mt-4 space-y-3 rounded-2xl border border-[#f0e4e2] bg-[#fffafa] p-4" onSubmit={addComment}>
                      <label className="block space-y-1.5 text-[11px] font-extrabold uppercase tracking-wider text-[#8b706e]">
                        Thêm comment xử lý
                        <textarea
                          className="min-h-24 w-full resize-y rounded-xl border border-[#e8d9d8] bg-white px-3.5 py-3 text-sm leading-6 text-[#2b2828] outline-none transition focus:border-[#730014] focus:ring-2 focus:ring-[#730014]/10"
                          maxLength={5000}
                          onChange={(event) => setComment(event.target.value)}
                          placeholder="Ghi chú xử lý / phản hồi cho học viên..."
                          value={comment}
                        />
                      </label>
                      <div className="flex justify-end">
                        <button className={PRIMARY_BUTTON_CLASS} disabled={!comment.trim() || working} type="submit">
                          <Send className="h-4 w-4" />
                          {working ? 'Đang gửi...' : 'Gửi comment'}
                        </button>
                      </div>
                    </form>
                  ) : (
                    <p className="mt-4 rounded-2xl border border-slate-100 bg-slate-50 px-4 py-3 text-sm font-semibold text-slate-600">
                      Ticket đã hoàn tất. Đổi trạng thái sang Mới gửi hoặc Đang xử lý nếu cần mở lại trao đổi.
                    </p>
                  )}
                </div>
              </div>
            </div>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}


