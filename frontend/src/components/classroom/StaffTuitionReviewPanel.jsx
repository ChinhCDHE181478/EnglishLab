import { useEffect, useState } from 'react';
import { Banknote, CheckCircle2 } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { useAppDialog } from '../ui/AppDialog';
import BrandedSelect from '../ui/BrandedSelect';
import TuitionProofMedia from './TuitionProofMedia';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDateTime, formatClassroomPrice, formatRegistrationStatus } from '../../utils/classroomHelpers';

const inputClass = 'w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm outline-none focus:border-[#730014]';

export default function StaffTuitionReviewPanel({ enrollment, onUpdated }) {
  const { confirm: confirmDialog } = useAppDialog();
  const [proofs, setProofs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [message, setMessage] = useState('');
  const [rejectReasons, setRejectReasons] = useState({});
  const [paymentKind, setPaymentKind] = useState('DEPOSIT');
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [note, setNote] = useState('');

  const due = Number(enrollment.tuitionAmountDue || 0);
  const paid = Number(enrollment.tuitionAmountPaid || 0);
  const balance = Math.max(0, due - paid);
  const depositRemaining = enrollment.tuitionFullPaymentRequired
    ? 0
    : Math.min(balance, Number(enrollment.tuitionDepositRemaining || 0));
  const amount = paymentKind === 'DEPOSIT' ? depositRemaining : balance;

  const loadProofs = async () => {
    setLoading(true);
    try {
      setProofs(await classroomApi.getEnrollmentTuitionProofs(enrollment.id));
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể tải minh chứng học phí.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setPaymentKind(depositRemaining > 0 ? 'DEPOSIT' : 'FULL');
    loadProofs();
  }, [enrollment.id, depositRemaining]);

  const refresh = async () => {
    await loadProofs();
    await onUpdated?.();
  };

  const recordCenterPayment = async () => {
    if (amount <= 0) return;
    const confirmed = await confirmDialog(
      `Xác nhận đã nhận ${formatClassroomPrice(amount)} tại trung tâm?`,
      { confirmLabel: 'Xác nhận đã thu', title: 'Ghi nhận học phí' },
    );
    if (!confirmed) return;
    setWorking(true);
    setMessage('');
    try {
      const methodLabel = paymentMethod === 'CASH' ? 'Tiền mặt tại trung tâm' : 'Chuyển khoản tại trung tâm';
      await classroomApi.recordTuitionPayment(enrollment.id, {
        amount,
        paymentKind,
        note: [methodLabel, note.trim()].filter(Boolean).join(' · '),
        assignIfFullyPaid: true,
      });
      setMessage('Đã ghi nhận khoản thu học phí.');
      setNote('');
      await refresh();
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể ghi nhận khoản thu học phí.'));
    } finally {
      setWorking(false);
    }
  };

  const confirmProof = async (proof) => {
    const confirmed = await confirmDialog(
      `Xác nhận minh chứng ${formatClassroomPrice(proof.amount)} đã vào tài khoản trung tâm?`,
      { confirmLabel: 'Xác nhận minh chứng', title: 'Duyệt thanh toán' },
    );
    if (!confirmed) return;
    setWorking(true);
    setMessage('');
    try {
      await classroomApi.confirmTuitionProof(proof.id);
      setMessage('Đã xác nhận minh chứng thanh toán.');
      await refresh();
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể xác nhận minh chứng.'));
    } finally {
      setWorking(false);
    }
  };

  const rejectProof = async (proof) => {
    const reason = rejectReasons[proof.id]?.trim();
    if (!reason) {
      setMessage('Vui lòng nhập lý do từ chối minh chứng.');
      return;
    }
    setWorking(true);
    setMessage('');
    try {
      await classroomApi.rejectTuitionProof(proof.id, { reason });
      setMessage('Đã từ chối minh chứng thanh toán.');
      await refresh();
    } catch (err) {
      setMessage(getClassroomErrorMessage(err, 'Không thể từ chối minh chứng.'));
    } finally {
      setWorking(false);
    }
  };

  return (
    <section className="space-y-4 rounded-2xl border border-[#dfbfbd]/50 bg-white p-4 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 className="flex items-center gap-2 font-['Manrope'] text-lg font-extrabold text-[#0b1c30]"><Banknote className="h-4 w-4 text-[#730014]" />Học phí</h3>
          <p className="mt-1 text-xs text-slate-500">{formatRegistrationStatus(enrollment.registrationStatus, enrollment.registrationStatusLabel)} · {formatClassroomPrice(paid)} / {formatClassroomPrice(due)}</p>
        </div>
        {enrollment.tuitionPaymentDeadline ? <span className="text-xs font-bold text-amber-700">Hạn {formatClassroomDateTime(enrollment.tuitionPaymentDeadline)}</span> : null}
      </div>

      {message ? <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-semibold text-slate-700">{message}</div> : null}

      {balance > 0 && enrollment.hasPendingPayosPayment ? (
        <div className="rounded-xl border border-sky-200 bg-sky-50 px-3 py-3 text-sm font-semibold text-sky-800">
          Đơn PayOS đang chờ xử lý. Hệ thống sẽ tự ghi nhận học phí sau khi PayOS xác nhận.
        </div>
      ) : null}

      {balance > 0 && !enrollment.hasPendingPayosPayment ? (
        <div className="space-y-3 rounded-xl border border-slate-200 bg-slate-50/60 p-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <BrandedSelect
              onChange={(event) => setPaymentKind(event.target.value)}
              options={[
                ...(depositRemaining > 0 ? [{ label: `Đặt cọc ${formatClassroomPrice(depositRemaining)}`, value: 'DEPOSIT' }] : []),
                { label: `Thu toàn bộ ${formatClassroomPrice(balance)}`, value: 'FULL' },
              ]}
              value={paymentKind}
            />
            <BrandedSelect
              onChange={(event) => setPaymentMethod(event.target.value)}
              options={[
                { label: 'Tiền mặt tại trung tâm', value: 'CASH' },
                { label: 'Chuyển khoản tại trung tâm', value: 'BANK_TRANSFER' },
              ]}
              value={paymentMethod}
            />
          </div>
          <input className={inputClass} onChange={(event) => setNote(event.target.value)} placeholder="Mã phiếu thu hoặc ghi chú" value={note} />
          <button className="inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-extrabold text-white disabled:opacity-50" disabled={working || amount <= 0 || enrollment.tuitionPaymentOverdue} onClick={recordCenterPayment} type="button"><CheckCircle2 className="h-4 w-4" />Xác nhận đã nhận {formatClassroomPrice(amount)}</button>
        </div>
      ) : null}

      <div className="space-y-3">
        <p className="text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Minh chứng chuyển khoản</p>
        {loading ? <p className="text-sm text-slate-500">Đang tải minh chứng...</p> : null}
        {!loading && !proofs.length ? <p className="rounded-xl border border-dashed border-slate-200 p-4 text-center text-sm text-slate-500">Chưa có minh chứng nào.</p> : null}
        {proofs.map((proof) => (
          <article className="rounded-xl border border-slate-200 p-4" key={proof.id}>
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div><p className="font-extrabold text-[#0b1c30]">{formatClassroomPrice(proof.amount)}</p><p className="mt-1 text-xs text-slate-500">{proof.paymentKindLabel} · {proof.statusLabel}</p></div>
              {proof.fileUrl ? <TuitionProofMedia alt={`Minh chứng #${proof.id}`} url={proof.fileUrl} /> : null}
            </div>
            {proof.status === 'PENDING' ? (
              <div className="mt-4 space-y-2 border-t border-slate-100 pt-4">
                <input className={inputClass} onChange={(event) => setRejectReasons((current) => ({ ...current, [proof.id]: event.target.value }))} placeholder="Lý do nếu từ chối" value={rejectReasons[proof.id] || ''} />
                <div className="flex justify-end gap-2">
                  <button className="rounded-xl border border-rose-200 px-3 py-2 text-xs font-extrabold text-rose-700 disabled:opacity-50" disabled={working} onClick={() => rejectProof(proof)} type="button">Từ chối</button>
                  <button className="rounded-xl bg-[#730014] px-3 py-2 text-xs font-extrabold text-white disabled:opacity-50" disabled={working} onClick={() => confirmProof(proof)} type="button">Xác nhận</button>
                </div>
              </div>
            ) : proof.reviewNote ? <p className="mt-3 text-xs text-rose-700">{proof.reviewNote}</p> : null}
          </article>
        ))}
      </div>
    </section>
  );
}
