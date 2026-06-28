import { useEffect, useMemo, useState } from 'react';
import {
  Activity,
  AlertCircle,
  ArrowLeftRight,
  BookOpen,
  Calendar,
  CheckCircle2,
  ClipboardCheck,
  DollarSign,
  HelpCircle,
  History,
  User,
  XCircle,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  ClassroomTabBar,
  ClassroomTypeBadge,
  ConflictPanel,
  StatusBadge,
  TuitionStatusCard,
} from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatClassroomDateTime,
  formatClassroomPrice,
  formatRegistrationStatus,
  formatTuitionPaymentKind,
} from '../../utils/classroomHelpers';

const globalStatusTabs = [
  { id: 'NEEDS_ACTION', label: 'Cần xử lý' },
  { id: 'PENDING_CONFIRMATION', label: 'Chờ xác nhận' },
  { id: 'PENDING_TUITION_PAYMENT', label: 'Chờ học phí' },
  { id: 'DEPOSIT_PAID', label: 'Đã đặt cọc' },
  { id: 'PARTIALLY_PAID', label: 'Thanh toán một phần' },
  { id: 'FULLY_PAID', label: 'Đã thanh toán đủ' },
  { id: 'WAITLIST', label: 'Chờ xếp lớp' },
  { id: 'ASSIGNED', label: 'Đã xếp lớp' },
  { id: 'REJECTED', label: 'Từ chối' },
];

const queueOnlyTabs = [
  { id: 'NEEDS_ACTION', label: 'Cần xử lý' },
  { id: 'ASSIGNED', label: 'Đã xếp lớp' },
];

const tuitionKindOptions = [
  { label: 'Đặt cọc', value: 'DEPOSIT' },
  { label: 'Thanh toán một phần', value: 'PARTIAL' },
  { label: 'Thanh toán đủ', value: 'FULL' },
  { label: 'Xác nhận thủ công', value: 'MANUAL_CONFIRMATION' },
];

export default function TrainingManagerRegistrationPanel({
  classroomOfferingId = null,
  initialEnrollmentId = '',
  initialTab = 'NEEDS_ACTION',
  onUpdated,
}) {
  const [activeTab, setActiveTab] = useState(initialTab);
  const [registrations, setRegistrations] = useState([]);
  const [classrooms, setClassrooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [selectedId, setSelectedId] = useState(initialEnrollmentId);
  const [tuitionForm, setTuitionForm] = useState({ amount: '', paymentKind: 'PARTIAL', note: '' });
  const [transferClassroomId, setTransferClassroomId] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [tuitionHistory, setTuitionHistory] = useState([]);
  const [conflictResult, setConflictResult] = useState(null);
  const [checkingConflict, setCheckingConflict] = useState(false);

  const statusTabs = classroomOfferingId ? queueOnlyTabs : globalStatusTabs;

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const params = {};
      if (classroomOfferingId) {
        params.classroomOfferingId = classroomOfferingId;
      }
      if (activeTab === 'NEEDS_ACTION') {
        params.needsAction = true;
      } else if (activeTab !== 'ALL') {
        params.status = activeTab;
      }

      const [registrationData, classroomData] = await Promise.all([
        classroomApi.getTrainingManagerRegistrations(params),
        classroomApi.getTrainingManagerClassrooms(),
      ]);
      setRegistrations(registrationData);
      setClassrooms(classroomData);
      if (registrationData.length > 0) {
        const exists = registrationData.some((item) => String(item.id) === selectedId);
        if (!exists) {
          setSelectedId(String(registrationData[0].id));
        }
      } else {
        setSelectedId('');
      }
    } catch (err) {
      setRegistrations([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải danh sách đăng ký.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setActiveTab(initialTab);
  }, [initialTab, classroomOfferingId]);

  useEffect(() => {
    loadData();
    setConflictResult(null);
  }, [activeTab, classroomOfferingId]);

  useEffect(() => {
    if (initialEnrollmentId) {
      setSelectedId(String(initialEnrollmentId));
    }
  }, [initialEnrollmentId]);

  const selected = useMemo(
    () => registrations.find((item) => String(item.id) === selectedId) || null,
    [registrations, selectedId],
  );

  useEffect(() => {
    const loadHistory = async () => {
      if (!selected?.id) {
        setTuitionHistory([]);
        return;
      }
      try {
        const history = await classroomApi.getTuitionHistory(selected.id);
        setTuitionHistory(history);
      } catch {
        setTuitionHistory(selected.tuitionPayments || []);
      }
    };
    loadHistory();
    setConflictResult(null);
  }, [selected?.id]);

  const runAction = async (action) => {
    setActionMessage('');
    try {
      await action();
      setActionMessage('Thao tác cập nhật thành công.');
      await loadData();
      onUpdated?.();
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể thực hiện thao tác.'));
    }
  };

  const handleConfirm = () => runAction(() => classroomApi.confirmClassRegistration(selected.id));
  const handleReject = () => runAction(() => classroomApi.rejectClassRegistration(selected.id, { reason: rejectReason }));
  const handleAssign = () => runAction(() => classroomApi.assignStudentToClass(selected.id));
  const handleRecordTuition = () => runAction(() => classroomApi.recordTuitionPayment(selected.id, {
    amount: Number(tuitionForm.amount),
    paymentKind: tuitionForm.paymentKind,
    note: tuitionForm.note || undefined,
    assignIfFullyPaid: true,
  }));
  const handleTransfer = () => runAction(() => classroomApi.transferClassEnrollment(selected.id, {
    targetClassroomOfferingId: Number(transferClassroomId),
  }));

  const handleConflictCheck = async () => {
    setActionMessage('');
    setCheckingConflict(true);
    try {
      const result = await classroomApi.checkEnrollmentConflict(selected.id);
      setConflictResult(result);
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể kiểm tra trùng lịch.'));
    } finally {
      setCheckingConflict(false);
    }
  };

  return (
    <div className="space-y-6">
      {actionMessage ? (
        <div
          className={`rounded-2xl border p-4 text-xs flex items-start gap-2 ${
            actionMessage.includes('thành công')
              ? 'bg-emerald-50 border-emerald-100 text-emerald-800'
              : 'bg-rose-50 border-rose-100 text-rose-800'
          }`}
        >
          <p className="leading-5">{actionMessage}</p>
        </div>
      ) : null}

      <ClassroomTabBar activeTab={activeTab} onChange={setActiveTab} tabs={statusTabs} />

      {loading ? <ClassroomLoadingState message="Đang tải hàng đợi đăng ký..." /> : null}
      {!loading && error ? <ClassroomErrorState message={error} onRetry={loadData} /> : null}
      {!loading && !error && !registrations.length ? (
        <ClassroomEmptyState
          description={classroomOfferingId
            ? 'Lớp này không có hồ sơ đăng ký nào trong bộ lọc hiện tại.'
            : 'Không có hồ sơ đăng ký nào cần xử lý. Đây là trạng thái bình thường khi mọi học viên đã được xếp lớp.'}
          title={activeTab === 'NEEDS_ACTION' ? 'Hàng đợi trống' : 'Chưa có hồ sơ đăng ký'}
          icon={HelpCircle}
        />
      ) : null}

      {!loading && !error && registrations.length ? (
        <div className="grid gap-6 lg:grid-cols-[380px_1fr]">
          <aside className="rounded-xl border border-[#e5e7eb] bg-white p-5 shadow-sm space-y-3 max-h-[750px] overflow-y-auto">
            <h3 className="text-xs font-bold text-[#8b706e] uppercase tracking-wider px-2">Hàng đợi</h3>
            <div className="space-y-2">
              {registrations.map((item) => {
                const isSelected = String(item.id) === selectedId;
                return (
                  <button
                    key={item.id}
                    className={`w-full rounded-2xl p-4 text-left transition-all duration-200 border ${
                      isSelected
                        ? 'bg-[#4b0009] border-[#4b0009] text-white shadow-md shadow-[#4b0009]/10'
                        : 'bg-[#fffafb]/50 border-gray-100 text-[#584140] hover:bg-[#fff3f4] hover:border-[#dfbfbd]/30'
                    }`}
                    onClick={() => setSelectedId(String(item.id))}
                    type="button"
                  >
                    <p className="font-extrabold text-sm">{item.studentName || item.studentEmail}</p>
                    {!classroomOfferingId ? (
                      <p className={`mt-1.5 text-xs line-clamp-1 ${isSelected ? 'text-white/80' : 'text-[#8b706e]'}`}>
                        {item.classroomTitle}
                      </p>
                    ) : null}
                    <div className="mt-3 flex items-center justify-between">
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                        isSelected ? 'bg-white/20 text-white' : 'bg-[#fff1f3] text-[#730014]'
                      }`}
                      >
                        {formatRegistrationStatus(item.registrationStatus, item.registrationStatusLabel)}
                      </span>
                    </div>
                  </button>
                );
              })}
            </div>
          </aside>

          {selected ? (
            <RegistrationDetail
              checkingConflict={checkingConflict}
              classrooms={classrooms}
              conflictResult={conflictResult}
              onAssign={handleAssign}
              onConflictCheck={handleConflictCheck}
              onConfirm={handleConfirm}
              onRecordTuition={handleRecordTuition}
              onReject={handleReject}
              onTransfer={handleTransfer}
              rejectReason={rejectReason}
              selected={selected}
              setRejectReason={setRejectReason}
              setTransferClassroomId={setTransferClassroomId}
              setTuitionForm={setTuitionForm}
              transferClassroomId={transferClassroomId}
              tuitionForm={tuitionForm}
              tuitionHistory={tuitionHistory}
            />
          ) : (
            <ClassroomEmptyState
              description="Chọn một hồ sơ ở danh sách bên trái để xử lý."
              title="Chưa chọn hồ sơ"
            />
          )}
        </div>
      ) : null}
    </div>
  );
}

function RegistrationDetail({
  selected,
  tuitionForm,
  setTuitionForm,
  rejectReason,
  setRejectReason,
  transferClassroomId,
  setTransferClassroomId,
  tuitionHistory,
  conflictResult,
  checkingConflict,
  classrooms,
  onConfirm,
  onReject,
  onAssign,
  onRecordTuition,
  onTransfer,
  onConflictCheck,
}) {
  return (
    <div className="space-y-6">
      <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-50 pb-5">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-12 items-center justify-center rounded-2xl bg-rose-50 text-[#730014] font-extrabold text-lg">
              {(selected.studentName || selected.studentEmail || 'H').charAt(0).toUpperCase()}
            </div>
            <div>
              <h2 className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{selected.studentName || selected.studentEmail}</h2>
              <p className="text-xs text-[#8b706e] mt-0.5">{selected.studentEmail}</p>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <ClassroomTypeBadge mode={selected.deliveryMode} />
            <StatusBadge status={selected.registrationStatus} />
          </div>
        </div>

        <div className="grid gap-6 sm:grid-cols-2 text-sm text-[#584140]">
          <div className="space-y-3">
            <p className="flex items-center gap-2">
              <BookOpen className="h-4 w-4 text-[#730014]" />
              <span>Lớp: <strong className="text-[#2b2828]">{selected.classroomTitle}</strong></span>
            </p>
            <p className="flex items-center gap-2">
              <Calendar className="h-4 w-4 text-[#730014]" />
              <span>Ngày đăng ký: <strong className="text-[#2b2828]">{formatClassroomDateTime(selected.enrolledAt)}</strong></span>
            </p>
          </div>
          <div className="space-y-3">
            <p className="flex items-center gap-2">
              <User className="h-4 w-4 text-[#730014]" />
              <span>Xác nhận bởi: <strong className="text-[#2b2828]">{selected.confirmedByName || '—'}</strong></span>
            </p>
            <p className="flex items-center gap-2">
              <ClipboardCheck className="h-4 w-4 text-[#730014]" />
              <span>Xếp lớp bởi: <strong className="text-[#2b2828]">{selected.assignedByName || '—'}</strong></span>
            </p>
          </div>
        </div>

        <TuitionStatusCard
          due={selected.tuitionAmountDue}
          paid={selected.tuitionAmountPaid}
          remaining={selected.tuitionRemaining}
          settlementLabel={selected.tuitionSettlementTypeLabel}
          settlementNote={selected.tuitionSettlementNote}
          settlementType={selected.tuitionSettlementType}
        />
      </section>

      <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-6">
        <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828] flex items-center gap-2">
          <Activity className="h-5 w-5 text-[#730014]" />
          Thao tác
        </h3>

        <div className="flex flex-wrap gap-3">
          {['PENDING_CONFIRMATION', 'WAITLIST'].includes(selected.registrationStatus) ? (
            <>
              <button className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white shadow-sm hover:bg-[#730014]" onClick={onConfirm} type="button">
                <CheckCircle2 className="h-4 w-4" />
                Xác nhận đăng ký
              </button>
              <button className="inline-flex items-center gap-1.5 rounded-xl border border-rose-200 bg-rose-50/30 px-5 py-3 text-xs font-extrabold text-rose-700 hover:bg-rose-50" onClick={onReject} type="button">
                <XCircle className="h-4 w-4" />
                Từ chối
              </button>
            </>
          ) : null}

          {!selected.hasClassAccess ? (
            <button className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-700 px-5 py-3 text-xs font-extrabold text-white shadow-sm hover:bg-emerald-800" onClick={onAssign} type="button">
              <ClipboardCheck className="h-4 w-4" />
              Xếp lớp chính thức
            </button>
          ) : null}

          <button className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-5 py-3 text-xs font-extrabold text-[#584140] hover:bg-gray-50" disabled={checkingConflict} onClick={onConflictCheck} type="button">
            {checkingConflict ? 'Đang kiểm tra...' : (
              <>
                <AlertCircle className="h-4 w-4 text-[#730014]" />
                Kiểm tra trùng lịch
              </>
            )}
          </button>
        </div>

        {['PENDING_CONFIRMATION', 'WAITLIST'].includes(selected.registrationStatus) ? (
          <input
            className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none focus:border-[#730014]"
            onChange={(event) => setRejectReason(event.target.value)}
            placeholder="Lý do từ chối (nếu có)..."
            value={rejectReason}
          />
        ) : null}

        {conflictResult ? <ConflictPanel conflictResult={conflictResult} /> : null}
      </section>

      {!selected.hasClassAccess ? (
        <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-4">
          <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828] flex items-center gap-2">
            <DollarSign className="h-5 w-5 text-[#730014]" />
            Ghi nhận học phí
          </h3>
          <div className="grid gap-4 md:grid-cols-2">
            <BrandedSelect
              onChange={(event) => setTuitionForm((current) => ({ ...current, paymentKind: event.target.value }))}
              options={tuitionKindOptions}
              value={tuitionForm.paymentKind}
            />
            <input
              className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm"
              onChange={(event) => setTuitionForm((current) => ({ ...current, amount: event.target.value }))}
              placeholder="Số tiền (VND)"
              value={tuitionForm.amount}
            />
          </div>
          <input
            className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm"
            onChange={(event) => setTuitionForm((current) => ({ ...current, note: event.target.value }))}
            placeholder="Ghi chú giao dịch..."
            value={tuitionForm.note}
          />
          <button className="rounded-2xl bg-[#4b0009] px-6 py-3 text-sm font-extrabold text-white hover:bg-[#730014]" onClick={onRecordTuition} type="button">
            Ghi nhận học phí
          </button>
        </section>
      ) : null}

      <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-4">
        <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828] flex items-center gap-2">
          <History className="h-5 w-5 text-[#730014]" />
          Lịch sử học phí
        </h3>
        {tuitionHistory.length ? tuitionHistory.map((payment) => (
          <div className="rounded-2xl border border-gray-100 bg-gray-50/30 p-4" key={payment.id}>
            <p className="font-extrabold text-emerald-700">+ {formatClassroomPrice(payment.amount)}</p>
            <p className="text-xs text-[#8b706e]">
              {formatTuitionPaymentKind(payment.paymentKind, payment.paymentKindLabel)}
              {payment.note ? ` · ${payment.note}` : ''}
            </p>
          </div>
        )) : <p className="text-sm text-gray-400 italic">Chưa có giao dịch.</p>}
      </section>

      {selected.hasClassAccess && classrooms.length ? (
        <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-4">
          <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828] flex items-center gap-2">
            <ArrowLeftRight className="h-5 w-5 text-[#730014]" />
            Chuyển lớp
          </h3>
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
            <BrandedSelect
              className="flex-1"
              onChange={(event) => setTransferClassroomId(event.target.value)}
              options={classrooms
                .filter((item) => item.id !== selected.classroomOfferingId)
                .map((item) => ({ label: item.title, value: String(item.id) }))}
              placeholder="Chọn lớp đích..."
              value={transferClassroomId}
            />
            <button className="rounded-2xl bg-[#4b0009] px-6 py-3 text-sm font-extrabold text-white hover:bg-[#730014]" onClick={onTransfer} type="button">
              Chuyển lớp
            </button>
          </div>
        </section>
      ) : null}
    </div>
  );
}
