import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import paymentApi from '../api/paymentApi';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import Pagination, { usePagination } from '../components/ui/Pagination';
import BrandLoadingState from '../components/ui/BrandLoadingState';
import { hasAccessToken } from '../utils/auth';
import { EMPTY_PAGE, normalizePage, pageParams } from '../utils/pagination';

const statusLabel = (status) => {
  switch (status) {
    case 'PAID': return { text: 'Đã thanh toán', className: 'bg-[#eef8f1] text-[#1f6b3b]' };
    case 'PENDING':
    case 'PROCESSING': return { text: 'Đang xử lý', className: 'bg-[#fff8e8] text-[#8a5b00]' };
    case 'FAILED':
    case 'CANCELLED':
    case 'EXPIRED': return { text: 'Không thành công', className: 'bg-[#ffdad6] text-[#93000a]' };
    default: return { text: status || 'Không xác định', className: 'bg-[#fcf8f8] text-[#584140]' };
  }
};

const orderTypeLabel = (orderType) => (
  orderType === 'CLASSROOM_TUITION' ? 'Học phí lớp' : 'Khóa học online'
);

const formatMoney = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`;

const TransactionHistoryPage = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [orders, setOrders] = useState([]);
  const [pageResult, setPageResult] = useState(EMPTY_PAGE);
  const { page, setPage, totalPages, pageItems: paginatedOrders, totalItems } = usePagination(
    orders,
    5,
    'transaction-history',
    pageResult,
  );

  useEffect(() => {
    let active = true;

    if (!hasAccessToken()) {
      setLoading(false);
      return undefined;
    }

    const loadHistory = async () => {
      setLoading(true);
      setError('');
      try {
        const result = normalizePage(await paymentApi.pageMyOrders(pageParams(page, 5)));
        if (!active) return;
        setPageResult(result);
        setOrders(result.content);
      } catch (err) {
        if (!active) return;
        setError(err?.response?.data?.message || 'Không thể tải lịch sử giao dịch. Vui lòng thử lại.');
      } finally {
        if (active) setLoading(false);
      }
    };

    loadHistory();
    return () => {
      active = false;
    };
  }, [page]);

  return (
    <LearnerPageShell
      title="Lịch sử giao dịch"
      description="Theo dõi các đơn thanh toán thực tế của bạn trên EnglishLab."
    >
      {!hasAccessToken() ? (
        <section className="flex min-h-[420px] flex-1 flex-col items-center justify-center rounded-[28px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center">
          <h2 className="font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Bạn cần đăng nhập để xem lịch sử giao dịch.</h2>
          <div className="mt-6">
            <Link className="rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white transition hover:bg-[#730014]" to="/login" state={{ from: '/transaction-history' }}>
              Đăng nhập
            </Link>
          </div>
        </section>
      ) : loading ? (
        <BrandLoadingState compact className="rounded-[28px]" message="Đang tải lịch sử giao dịch..." />
      ) : error ? (
        <section className="flex min-h-[320px] flex-1 flex-col items-center justify-center rounded-[28px] border border-[#f0d4d7] bg-white px-6 py-16 text-center text-[#93000a]">
          <p>{error}</p>
          <button className="mt-4 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white" onClick={() => window.location.reload()} type="button">
            Thử lại
          </button>
        </section>
      ) : !orders.length ? (
        <section className="flex min-h-[420px] flex-1 flex-col items-center justify-center rounded-[28px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center">
          <h2 className="font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Bạn chưa có giao dịch nào.</h2>
          <p className="mt-3 max-w-xl text-sm leading-7 text-[#584140]">Các đơn thanh toán sẽ xuất hiện tại đây sau khi bạn mua khóa học.</p>
          <div className="mt-6">
            <Link className="rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white transition hover:bg-[#730014]" to="/courses#recommended">
              Xem khóa học
            </Link>
          </div>
        </section>
      ) : (
        <div className="space-y-6 flex-1 flex flex-col justify-between">
          <section className="grid gap-6">
            {paginatedOrders.map((order) => {
              const badge = statusLabel(order.status);
              return (
                <article key={order.orderCode} className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-6 shadow-sm">
                  <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                    <div>
                      <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#730014]">
                        {orderTypeLabel(order.orderType)} · Mã đơn #{order.orderCode}
                      </p>
                      <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
                        {(order.courseTitles || []).join(' · ') || order.description || 'Thanh toán khóa học'}
                      </h2>
                      <p className="mt-2 text-sm leading-7 text-[#584140]">
                        Tạo lúc {order.createdAt ? new Date(order.createdAt).toLocaleString('vi-VN') : '—'}
                        {order.paidAt ? ` · Thanh toán lúc ${new Date(order.paidAt).toLocaleString('vi-VN')}` : ''}
                      </p>
                    </div>
                    <span className={`self-start rounded-full px-3 py-2 text-xs font-extrabold ${badge.className}`}>
                      {badge.text}
                    </span>
                  </div>

                  <div className="mt-4 grid gap-3 md:grid-cols-4">
                    <div className="rounded-2xl bg-[#fcf8f8] px-4 py-3 text-sm text-[#584140]">
                      Số tiền: <strong className="text-[#2b2828]">{formatMoney(order.amount)}</strong>
                    </div>
                    <div className="rounded-2xl bg-[#fcf8f8] px-4 py-3 text-sm text-[#584140]">
                      Giá gốc: <strong className="text-[#2b2828]">{formatMoney(order.originalAmount)}</strong>
                    </div>
                    <div className="rounded-2xl bg-[#fcf8f8] px-4 py-3 text-sm text-[#584140]">
                      Giảm giá: <strong className="text-[#2b2828]">{formatMoney((order.systemDiscountAmount || 0) + (order.learningPathDiscountAmount || 0) + (order.couponDiscountAmount || 0))}</strong>
                    </div>
                    <div className="rounded-2xl bg-[#fcf8f8] px-4 py-3 text-sm text-[#584140]">
                      Mã giảm: <strong className="text-[#2b2828]">{order.discountCodeText || 'Không có'}</strong>
                    </div>
                  </div>
                </article>
              );
            })}
          </section>

          {totalItems > 5 && (
            <div className="flex justify-end">
              <Pagination
                page={page}
                totalPages={totalPages}
                onChange={setPage}
                totalItems={totalItems}
                pageSize={5}
              />
            </div>
          )}
        </div>
      )}
    </LearnerPageShell>
  );
};

export default TransactionHistoryPage;
