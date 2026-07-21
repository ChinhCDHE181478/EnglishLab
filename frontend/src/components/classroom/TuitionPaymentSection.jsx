import { useCallback, useEffect, useState } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  CreditCard,
  History,
  Receipt,
  Upload,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import paymentApi from '../../api/paymentApi';
import BrandedSelect from '../ui/BrandedSelect';
import TuitionProofMedia, { LocalFilePreview } from './TuitionProofMedia';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDate, formatClassroomPrice } from '../../utils/classroomHelpers';

const PROOF_KIND_OPTIONS = [
  { label: 'Đặt cọc giữ chỗ', value: 'DEPOSIT' },
  { label: 'Thanh toán một phần', value: 'PARTIAL' },
  { label: 'Thanh toán toàn bộ', value: 'FULL' },
];

const CLASSROOM_TUITION_RETURN_KEY = 'englishlab.classroomTuitionReturn';

const proofStatusStyle = (status) => {
  if (status === 'CONFIRMED') return 'bg-emerald-50 text-emerald-700 border-emerald-100';
  if (status === 'REJECTED') return 'bg-rose-50 text-rose-700 border-rose-100';
  return 'bg-amber-50 text-amber-700 border-amber-100';
};

export default function TuitionPaymentSection({
  classroomId,
  tuitionRemaining = 0,
  canSubmitProof = true,
  onUpdated,
  compact = false,
}) {
  const [history, setHistory] = useState([]);
  const [proofs, setProofs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [payingPayos, setPayingPayos] = useState(false);
  const [message, setMessage] = useState('');
  const [success, setSuccess] = useState(false);
  const [form, setForm] = useState({ file: null, amount: '', paymentKind: 'PARTIAL', note: '' });

  const remaining = Number(tuitionRemaining) > 0 ? Number(tuitionRemaining) : 0;
  const canPayOnline = canSubmitProof && remaining > 0;

  const loadData = useCallback(async () => {
    if (!classroomId) return;
    setLoading(true);
    try {
      const [historyData, proofData] = await Promise.all([
        classroomApi.getMyTuitionHistory(classroomId),
        classroomApi.getMyTuitionProofs(classroomId),
      ]);
      setHistory(historyData);
      setProofs(proofData);
    } catch {
      setHistory([]);
      setProofs([]);
    } finally {
      setLoading(false);
    }
  }, [classroomId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handlePayosPayment = async () => {
    setMessage('');
    setSuccess(false);
    if (!canPayOnline) {
      setMessage('Hiện không thể thanh toán PayOS cho lớp này.');
      return;
    }
    setPayingPayos(true);
    try {
      const result = await paymentApi.createPayosLink([], '', [Number(classroomId)]);
      const paidDirectly = String(result?.status || '').toUpperCase() === 'PAID';
      if (paidDirectly) {
        setMessage(result?.message || 'Đã ghi nhận học phí thành công.');
        setSuccess(true);
        await loadData();
        onUpdated?.();
        return;
      }

      const checkoutUrl =
        result?.checkoutUrl
        || result?.paymentUrl
        || result?.url
        || result?.payUrl
        || result?.data?.checkoutUrl
        || result?.data?.paymentUrl;

      if (!checkoutUrl) {
        throw new Error('missing_checkout_url');
      }

      sessionStorage.setItem(
        CLASSROOM_TUITION_RETURN_KEY,
        JSON.stringify({
          classroomId: Number(classroomId),
          returnPath: `${window.location.pathname}${window.location.search}`,
          orderCode: result?.orderCode || null,
        }),
      );
      window.location.href = checkoutUrl;
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể tạo link thanh toán PayOS.'));
      setSuccess(false);
      setPayingPayos(false);
    }
  };

  const handleSubmitProof = async (event) => {
    event.preventDefault();
    setMessage('');
    setSuccess(false);
    if (!form.file) {
      setMessage('Vui lòng chọn ảnh/tệp minh chứng chuyển khoản.');
      return;
    }
    if (!form.amount || Number(form.amount) <= 0) {
      setMessage('Vui lòng nhập số tiền chuyển khoản hợp lệ.');
      return;
    }
    setSubmitting(true);
    try {
      await classroomApi.submitTuitionProof(classroomId, {
        file: form.file,
        amount: Number(form.amount),
        paymentKind: form.paymentKind,
        note: form.note || undefined,
      });
      setMessage('Đã gửi minh chứng thanh toán. Nhân viên đào tạo sẽ xác nhận trong thời gian sớm nhất.');
      setSuccess(true);
      setForm({ file: null, amount: '', paymentKind: 'PARTIAL', note: '' });
      await loadData();
      onUpdated?.();
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể gửi minh chứng thanh toán.'));
      setSuccess(false);
    } finally {
      setSubmitting(false);
    }
  };

  const shellClass = compact
    ? 'rounded-2xl border border-[#dfbfbd]/15 bg-white p-4 shadow-sm space-y-4'
    : 'rounded-[28px] border border-[#dfbfbd]/15 bg-white p-6 shadow-sm space-y-4';

  return (
    <div className={shellClass}>
      <div className="flex items-start justify-between gap-3">
        <h3 className={`font-['Manrope'] font-extrabold text-[#2b2828] flex items-center gap-2 ${compact ? 'text-sm' : 'text-lg'}`}>
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#fff1f3] text-[#730014]">
            <Receipt className="h-4 w-4" />
          </span>
          Thanh toán học phí
        </h3>
      </div>

      {message ? (
        <div className={`rounded-2xl border p-3 text-xs flex items-start gap-2 ${
          success ? 'bg-emerald-50 border-emerald-100 text-emerald-800' : 'bg-rose-50 border-rose-100 text-rose-800'
        }`}
        >
          {success
            ? <CheckCircle2 className="h-4 w-4 flex-shrink-0 mt-0.5 text-emerald-700" />
            : <AlertCircle className="h-4 w-4 flex-shrink-0 mt-0.5 text-rose-700" />}
          <p className="leading-5">{message}</p>
        </div>
      ) : null}

      {canPayOnline ? (
        <div className="space-y-3 rounded-[20px] border border-[#ecdedd] bg-gradient-to-br from-[#fffafb] to-white p-5">
          <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#730014] flex items-center gap-1.5">
            <CreditCard className="h-3.5 w-3.5" />
            Thanh toán online (PayOS)
          </p>
          <p className="text-xs text-[#584140] leading-5">
            Thanh toán số tiền còn lại{' '}
            <strong className="text-[#2b2828]">{formatClassroomPrice(remaining)}</strong>
            {' '}qua PayOS. Hệ thống sẽ tự ghi nhận học phí sau khi thanh toán thành công.
          </p>
          <button
            className="w-full rounded-2xl bg-[#4b0009] py-3 text-xs font-extrabold text-white transition hover:bg-[#730014] disabled:opacity-60"
            disabled={payingPayos}
            onClick={handlePayosPayment}
            type="button"
          >
            {payingPayos ? 'Đang chuyển tới PayOS...' : `Thanh toán ${formatClassroomPrice(remaining)} qua PayOS`}
          </button>
        </div>
      ) : null}

      {canSubmitProof ? (
        <form className="space-y-4 rounded-[20px] border border-[#ecdedd] bg-gradient-to-br from-[#fffafb] to-white p-5" onSubmit={handleSubmitProof}>
          <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#730014] flex items-center gap-1.5">
            <Upload className="h-3.5 w-3.5" />
            {canPayOnline ? 'Hoặc gửi minh chứng chuyển khoản' : 'Gửi minh chứng chuyển khoản'}
          </p>
          <p className="text-[11px] text-[#8b706e] leading-4">
            Phương án dự phòng khi không dùng được PayOS. Nhân viên đào tạo sẽ xác nhận minh chứng thủ công.
          </p>
          <label className="flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-[#dfc4c2]/60 bg-white px-4 py-6 text-center transition hover:border-[#730014]/40 hover:bg-[#fff7f7]">
            {form.file ? (
              <div className="w-full space-y-2">
                <LocalFilePreview file={form.file} />
                <span className="inline-block text-[11px] font-bold text-[#730014]">Chọn ảnh khác</span>
              </div>
            ) : (
              <>
                <Upload className="mb-2 h-6 w-6 text-[#730014]" />
                <span className="text-xs font-bold text-[#584140]">Chọn ảnh hoặc PDF minh chứng</span>
                <span className="mt-1 text-[10px] text-[#8b706e]">JPG, PNG hoặc PDF — tối đa 20MB</span>
              </>
            )}
            <input
              accept=".jpg,.jpeg,.png,.pdf,image/jpeg,image/png,application/pdf"
              className="sr-only"
              onChange={(event) => setForm((current) => ({ ...current, file: event.target.files?.[0] || null }))}
              type="file"
            />
          </label>
          <div className="grid gap-3 sm:grid-cols-2">
            <input
              className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-white px-4 py-2.5 text-sm text-[#2b2828] outline-none focus:border-[#730014]"
              inputMode="numeric"
              min="1"
              onChange={(event) => setForm((current) => ({ ...current, amount: event.target.value }))}
              placeholder="Số tiền (VND)"
              type="number"
              value={form.amount}
            />
            <BrandedSelect
              onChange={(event) => setForm((current) => ({ ...current, paymentKind: event.target.value }))}
              options={PROOF_KIND_OPTIONS}
              value={form.paymentKind}
            />
          </div>
          <input
            className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-white px-4 py-2.5 text-sm text-[#2b2828] outline-none focus:border-[#730014]"
            onChange={(event) => setForm((current) => ({ ...current, note: event.target.value }))}
            placeholder="Ghi chú (mã giao dịch, ngân hàng...)"
            value={form.note}
          />
          <button
            className="w-full rounded-2xl border border-[#730014]/30 bg-white py-3 text-xs font-extrabold text-[#730014] transition hover:bg-[#fff1f3] disabled:opacity-60"
            disabled={submitting}
            type="submit"
          >
            {submitting ? 'Đang gửi minh chứng...' : 'Gửi minh chứng thanh toán'}
          </button>
        </form>
      ) : null}

      <div className="space-y-2">
        <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Minh chứng đã gửi</p>
        {loading ? <p className="text-xs text-gray-400">Đang tải...</p> : null}
        {!loading && !proofs.length ? (
          <p className="text-xs text-gray-400 italic">Chưa có minh chứng thanh toán nào.</p>
        ) : null}
        {proofs.map((proof) => (
          <div className="rounded-2xl border border-gray-100 bg-white p-3 space-y-1.5" key={proof.id}>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-extrabold text-[#2b2828]">{formatClassroomPrice(proof.amount)}</p>
              <span className={`rounded-full border px-2.5 py-0.5 text-[10px] font-extrabold ${proofStatusStyle(proof.status)}`}>
                {proof.statusLabel || proof.status}
              </span>
            </div>
            <p className="text-[11px] text-[#8b706e]">
              {proof.paymentKindLabel}
              {proof.note ? ` · ${proof.note}` : ''}
            </p>
            {proof.status === 'CONFIRMED' ? (
              <p className="text-[11px] font-bold text-emerald-700">Mã xác nhận: TP-{proof.id}</p>
            ) : null}
            {proof.status === 'REJECTED' && proof.reviewNote ? (
              <p className="text-[11px] text-rose-700">Lý do từ chối: {proof.reviewNote}</p>
            ) : null}
            {proof.fileUrl ? (
              <TuitionProofMedia
                alt={`Minh chứng ${formatClassroomPrice(proof.amount)}`}
                url={proof.fileUrl}
              />
            ) : null}
          </div>
        ))}
      </div>

      <div className="space-y-2">
        <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider flex items-center gap-1.5">
          <History className="h-3.5 w-3.5 text-[#730014]" />
          Lịch sử thanh toán
        </p>
        {!loading && !history.length ? (
          <p className="text-xs text-gray-400 italic">Chưa có giao dịch học phí nào được ghi nhận.</p>
        ) : null}
        {history.map((payment) => (
          <div className="flex items-center justify-between rounded-2xl border border-gray-100 bg-gray-50/30 p-3" key={payment.id}>
            <div>
              <p className="text-sm font-extrabold text-emerald-700">+ {formatClassroomPrice(payment.amount)}</p>
              <p className="text-[11px] text-[#8b706e]">
                {payment.paymentKindLabel}
                {payment.note ? ` · ${payment.note}` : ''}
              </p>
            </div>
            <p className="text-[10px] text-[#8b706e]">{formatClassroomDate(payment.createdAt)}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
