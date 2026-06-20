import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import {
  BookOpen,
  Calendar,
  Clock,
  MapPin,
  Video,
  Users,
  Award,
  Plus,
  ArrowRight,
  ClipboardCheck,
  FileText,
  AlertCircle,
  DollarSign,
  Search,
  CheckCircle2,
  XCircle,
  HelpCircle,
  User,
  Activity,
  History,
  ArrowLeftRight,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import Header from '../../components/ai-learning/Header';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  ClassroomLoadingState,
  ClassroomTabBar,
  PageHero,
  StatusBadge,
  ClassroomTypeBadge,
  TuitionStatusCard,
  ConflictPanel,
} from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { getClassroomErrorMessage, getConflictSummary } from '../../utils/classroomErrorMessages';
import {
  formatClassroomDate,
  formatClassroomDateTime,
  formatClassroomPrice,
  formatDeliveryMode,
  formatRegistrationStatus,
  formatTuitionPaymentKind,
  formatTuitionSettlement,
} from '../../utils/classroomHelpers';

const statusTabs = [
  { id: 'ALL', label: 'Tất cả' },
  { id: 'PENDING_CONFIRMATION', label: 'Chờ xác nhận' },
  { id: 'PENDING_TUITION_PAYMENT', label: 'Chờ học phí' },
  { id: 'DEPOSIT_PAID', label: 'Đã đặt cọc' },
  { id: 'PARTIALLY_PAID', label: 'Thanh toán một phần' },
  { id: 'FULLY_PAID', label: 'Đã thanh toán đủ' },
  { id: 'WAITLIST', label: 'Chờ xếp lớp' },
  { id: 'ASSIGNED', label: 'Đã xếp lớp' },
  { id: 'REJECTED', label: 'Từ chối' },
];

const tuitionKindOptions = [
  { label: 'Đặt cọc', value: 'DEPOSIT' },
  { label: 'Thanh toán một phần', value: 'PARTIAL' },
  { label: 'Thanh toán đủ', value: 'FULL' },
  { label: 'Xác nhận thủ công', value: 'MANUAL_CONFIRMATION' },
];

export default function TrainingManagerClassroomRegistrationsPage() {
  const [activeTab, setActiveTab] = useState('ALL');
  const [registrations, setRegistrations] = useState([]);
  const [classrooms, setClassrooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [selectedId, setSelectedId] = useState('');
  const [tuitionForm, setTuitionForm] = useState({ amount: '', paymentKind: 'PARTIAL', note: '' });
  const [transferClassroomId, setTransferClassroomId] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [tuitionHistory, setTuitionHistory] = useState([]);
  const [conflictResult, setConflictResult] = useState(null);
  const [checkingConflict, setCheckingConflict] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const params = activeTab === 'ALL' ? {} : { status: activeTab };
      const [registrationData, classroomData] = await Promise.all([
        classroomApi.getTrainingManagerRegistrations(params),
        classroomApi.getTrainingManagerClassrooms(),
      ]);
      setRegistrations(registrationData);
      setClassrooms(classroomData);
      if (registrationData.length > 0) {
        // If current selectedId is not in the new list, select the first one
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
    loadData();
    setConflictResult(null);
  }, [activeTab]);

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

  // Calculate statistics for PageHero
  const stats = useMemo(() => {
    const pendingConfirm = registrations.filter((r) => r.registrationStatus === 'PENDING_CONFIRMATION').length;
    const pendingTuition = registrations.filter((r) => r.registrationStatus === 'PENDING_TUITION_PAYMENT').length;
    const waitlist = registrations.filter((r) => r.registrationStatus === 'WAITLIST').length;
    const assigned = registrations.filter((r) => r.registrationStatus === 'ASSIGNED').length;

    return [
      { label: 'Chờ xác nhận', value: pendingConfirm, icon: Clock, color: pendingConfirm > 0 ? 'amber' : 'blue' },
      { label: 'Chờ học phí', value: pendingTuition, icon: DollarSign, color: 'rose' },
      { label: 'Chờ xếp lớp', value: waitlist, icon: Users, color: 'purple' },
      { label: 'Đã xếp lớp', value: assigned, icon: CheckCircle2, color: 'emerald' },
    ];
  }, [registrations]);

  return (
    <div className="course-page flex min-h-[100dvh] flex-col bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />
      <motion.main
        className="mx-auto flex w-full max-w-[1320px] flex-1 flex-col px-4 pb-[80px] pt-8 md:px-10 space-y-8"
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.32, ease: 'easeOut' }}
      >
        {/* Page Hero with operational stats */}
        <PageHero
          title="Quản lý đăng ký lớp & Học phí"
          subtitle="Không gian nghiệp vụ dành cho Training Manager. Thực hiện xác nhận đăng ký giữ chỗ, ghi nhận học phí, kiểm tra trùng lịch học và xếp lớp chính thức cho học viên."
          stats={stats}
          action={
            <Link
              className="inline-flex items-center gap-1.5 rounded-2xl bg-[#4b0009] px-6 py-3.5 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow-lg active:scale-95"
              to="/training-manager/requests"
            >
              Phê duyệt yêu cầu thay đổi
              <ArrowRight className="h-4 w-4" />
            </Link>
          }
        />

        {/* Action Notification */}
        {actionMessage ? (
          <div className={`rounded-2xl border p-4 text-xs flex items-start gap-2 ${
            actionMessage.includes('thành công')
              ? 'bg-emerald-50 border-emerald-100 text-emerald-800'
              : 'bg-rose-50 border-rose-100 text-rose-800'
          }`}>
            {actionMessage.includes('thành công') ? (
              <CheckCircle2 className="h-4 w-4 flex-shrink-0 mt-0.5 text-emerald-700" />
            ) : (
              <AlertCircle className="h-4 w-4 flex-shrink-0 mt-0.5 text-rose-700" />
            )}
            <p className="leading-5">{actionMessage}</p>
          </div>
        ) : null}

        <div className="space-y-6">
          {/* Status Tabs */}
          <ClassroomTabBar activeTab={activeTab} onChange={setActiveTab} tabs={statusTabs} />

          {loading ? <ClassroomLoadingState message="Đang tải danh sách đăng ký..." /> : null}
          {!loading && error ? <ClassroomErrorState message={error} onRetry={loadData} /> : null}
          {!loading && !error && !registrations.length ? (
            <ClassroomEmptyState
              description="Không có hồ sơ đăng ký lớp nào khớp với bộ lọc hiện tại."
              title="Chưa có hồ sơ đăng ký"
              icon={HelpCircle}
            />
          ) : null}

          {!loading && !error && registrations.length ? (
            <div className="grid gap-6 lg:grid-cols-[380px_1fr]">
              {/* Left Sidebar: Registrations List */}
              <aside className="rounded-xl border border-[#e5e7eb] bg-white p-5 shadow-sm space-y-3 max-h-[750px] overflow-y-auto">
                <h3 className="text-xs font-bold text-[#8b706e] uppercase tracking-wider px-2">Danh sách đăng ký</h3>
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
                        <p className={`mt-1.5 text-xs line-clamp-1 ${isSelected ? 'text-white/80' : 'text-[#8b706e]'}`}>
                          {item.classroomTitle}
                        </p>
                        <div className="mt-3 flex items-center justify-between">
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                            isSelected ? 'bg-white/20 text-white' : 'bg-[#fff1f3] text-[#730014]'
                          }`}>
                            {formatRegistrationStatus(item.registrationStatus, item.registrationStatusLabel)}
                          </span>
                          <span className="text-[10px] opacity-75">ID: #{item.id}</span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              </aside>

              {/* Right Panel: Operations Workspace */}
              {selected ? (
                <div className="space-y-6">
                  {/* Learner Profile & Registration Info */}
                  <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-6">
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-50 pb-5">
                      <div className="flex items-center gap-4">
                        <div className="flex h-14 w-12 items-center justify-center rounded-2xl bg-rose-50 text-[#730014] font-extrabold text-lg">
                          {selected.studentName ? selected.studentName.charAt(0).toUpperCase() : 'H'}
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
                          <span>Lớp đăng ký: <strong className="text-[#2b2828]">{selected.classroomTitle}</strong></span>
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

                    {/* Tuition Status Card */}
                    <TuitionStatusCard
                      due={selected.tuitionAmountDue}
                      paid={selected.tuitionAmountPaid}
                      remaining={selected.tuitionRemaining}
                      settlementLabel={selected.tuitionSettlementTypeLabel}
                      settlementNote={selected.tuitionSettlementNote}
                      settlementType={selected.tuitionSettlementType}
                    />
                  </section>

                  {/* Operations CTA Bar */}
                  <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-6">
                    <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828] flex items-center gap-2">
                      <Activity className="h-5 w-5 text-[#730014]" />
                      Thao tác nghiệp vụ
                    </h3>

                    <div className="flex flex-wrap gap-3">
                      {['PENDING_CONFIRMATION', 'WAITLIST'].includes(selected.registrationStatus) ? (
                        <>
                          <button
                            className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014] active:scale-95"
                            onClick={handleConfirm}
                            type="button"
                          >
                            <CheckCircle2 className="h-4 w-4" />
                            Xác nhận đăng ký giữ chỗ
                          </button>
                          <button
                            className="inline-flex items-center gap-1.5 rounded-xl border border-rose-200 bg-rose-50/30 px-5 py-3 text-xs font-extrabold text-rose-700 transition hover:bg-rose-50 active:scale-95"
                            onClick={handleReject}
                            type="button"
                          >
                            <XCircle className="h-4 w-4" />
                            Từ chối đăng ký
                          </button>
                        </>
                      ) : null}

                      {!selected.hasClassAccess ? (
                        <button
                          className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-700 px-5 py-3 text-xs font-extrabold text-white shadow-sm transition hover:bg-emerald-800 active:scale-95"
                          onClick={handleAssign}
                          type="button"
                        >
                          <ClipboardCheck className="h-4 w-4" />
                          Xếp lớp chính thức
                        </button>
                      ) : null}

                      <button
                        className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-5 py-3 text-xs font-extrabold text-[#584140] transition hover:bg-gray-50 active:scale-95"
                        disabled={checkingConflict}
                        onClick={handleConflictCheck}
                        type="button"
                      >
                        {checkingConflict ? (
                          <>Đang kiểm tra...</>
                        ) : (
                          <>
                            <AlertCircle className="h-4 w-4 text-[#730014]" />
                            Kiểm tra trùng lịch học
                          </>
                        )}
                      </button>
                    </div>

                    {/* Reject Reason Input */}
                    {['PENDING_CONFIRMATION', 'WAITLIST'].includes(selected.registrationStatus) && (
                      <div className="space-y-2 pt-2">
                        <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Lý do từ chối (nếu có)</label>
                        <input
                          className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
                          onChange={(event) => setRejectReason(event.target.value)}
                          placeholder="Nhập lý do từ chối đăng ký để gửi thông báo tới học viên..."
                          value={rejectReason}
                        />
                      </div>
                    )}

                    {/* Conflict Check Result Panel */}
                    {conflictResult && (
                      <div className="pt-2">
                        <ConflictPanel conflictResult={conflictResult} />
                      </div>
                    )}
                  </section>

                  {/* Record Tuition Section */}
                  {!selected.hasClassAccess ? (
                    <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-6">
                      <div className="flex items-start gap-4">
                        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-[#730014] flex-shrink-0">
                          <DollarSign className="h-6 w-6" />
                        </div>
                        <div>
                          <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Ghi nhận đóng học phí</h3>
                          <p className="mt-1 text-xs text-[#8b706e] leading-5">Ghi nhận giao dịch nộp học phí trực tiếp tại trung tâm hoặc chuyển khoản thủ công. Hệ thống tự động xếp lớp nếu đã hoàn thành học phí.</p>
                        </div>
                      </div>

                      <div className="grid gap-4 md:grid-cols-2 pt-2">
                        <div className="space-y-2">
                          <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Phân loại thanh toán</label>
                          <BrandedSelect
                            onChange={(event) => setTuitionForm((current) => ({ ...current, paymentKind: event.target.value }))}
                            options={tuitionKindOptions}
                            value={tuitionForm.paymentKind}
                          />
                        </div>
                        <div className="space-y-2">
                          <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Số tiền nộp (VND)</label>
                          <input
                            className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
                            onChange={(event) => setTuitionForm((current) => ({ ...current, amount: event.target.value }))}
                            placeholder="Ví dụ: 2000000"
                            value={tuitionForm.amount}
                          />
                        </div>
                      </div>

                      <div className="space-y-2">
                        <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Ghi chú giao dịch</label>
                        <input
                          className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 px-4 py-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
                          onChange={(event) => setTuitionForm((current) => ({ ...current, note: event.target.value }))}
                          placeholder="Mã giao dịch ngân hàng, người nộp, ngày nộp..."
                          value={tuitionForm.note}
                        />
                      </div>

                      <div className="pt-2 flex justify-end">
                        <button
                          className="inline-flex items-center gap-1.5 rounded-2xl bg-[#4b0009] px-6 py-3.5 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow active:scale-95"
                          onClick={handleRecordTuition}
                          type="button"
                        >
                          <DollarSign className="h-4 w-4" />
                          Ghi nhận giao dịch học phí
                        </button>
                      </div>
                    </section>
                  ) : null}

                  {/* Tuition History Section */}
                  <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-4">
                    <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828] flex items-center gap-2">
                      <History className="h-5 w-5 text-[#730014]" />
                      Lịch sử giao dịch học phí
                    </h3>

                    {tuitionHistory.length ? (
                      <div className="space-y-3">
                        {tuitionHistory.map((payment) => (
                          <div
                            key={payment.id}
                            className="rounded-2xl border border-gray-100 bg-gray-50/30 p-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3"
                          >
                            <div className="space-y-1">
                              <p className="font-['Manrope'] text-base font-extrabold text-emerald-700">
                                + {formatClassroomPrice(payment.amount)}
                              </p>
                              <p className="text-xs text-[#8b706e]">
                                {formatTuitionPaymentKind(payment.paymentKind, payment.paymentKindLabel)}
                                {payment.note ? ` · Ghi chú: ${payment.note}` : ''}
                              </p>
                            </div>
                            <div className="text-right text-[10px] text-gray-400">
                              <p>Người duyệt: {payment.recordedByName || 'Hệ thống'}</p>
                              {payment.createdAt && <p className="mt-0.5">{formatClassroomDateTime(payment.createdAt)}</p>}
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="text-sm text-gray-400 italic">Chưa ghi nhận bất kỳ giao dịch học phí nào.</p>
                    )}
                  </section>

                  {/* Transfer Classroom Section */}
                  {selected.hasClassAccess ? (
                    <section className="rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm space-y-6">
                      <div className="flex items-start gap-4">
                        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-[#730014] flex-shrink-0">
                          <ArrowLeftRight className="h-6 w-6" />
                        </div>
                        <div>
                          <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Chuyển lớp học</h3>
                          <p className="mt-1 text-xs text-[#8b706e] leading-5">Chuyển học viên sang một lớp học khác cùng hệ thống. Hệ thống sẽ tự động đối soát học phí chênh lệch và cập nhật lịch học mới.</p>
                        </div>
                      </div>

                      <div className="flex flex-col gap-4 sm:flex-row sm:items-end pt-2">
                        <div className="flex-1 space-y-2">
                          <label className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Lớp học đích</label>
                          <BrandedSelect
                            className="w-full"
                            onChange={(event) => setTransferClassroomId(event.target.value)}
                            options={classrooms
                              .filter((item) => item.id !== selected.classroomOfferingId)
                              .map((item) => ({ label: item.title, value: String(item.id) }))}
                            placeholder="Chọn lớp học đích..."
                            value={transferClassroomId}
                          />
                        </div>
                        <button
                          className="rounded-2xl bg-[#4b0009] px-6 py-3.5 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow active:scale-95 flex-shrink-0"
                          onClick={handleTransfer}
                          type="button"
                        >
                          Thực hiện chuyển lớp
                        </button>
                      </div>
                    </section>
                  ) : null}
                </div>
              ) : (
                <ClassroomEmptyState
                  description="Hãy chọn một hồ sơ đăng ký ở danh sách bên trái để thực hiện các nghiệp vụ."
                  title="Chưa chọn hồ sơ đăng ký"
                />
              )}
            </div>
          ) : null}
        </div>
      </motion.main>
      <CourseFooter />
    </div>
  );
}
