import { useEffect, useState } from 'react';
import { ArrowRight, ClipboardCheck, GraduationCap, LifeBuoy, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import classroomApi from '../../api/classroomApi';
import courseApi from '../../api/courseApi';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import supportApi from '../../api/supportApi';
import { formatClassroomDateTime } from '../../utils/classroomHelpers';

export default function ManagerDashboardPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [proposals, demand, enrollments, tickets, operations] = await Promise.all([
        enrollmentRequestApi.listManagerClassroomProposals('PENDING_APPROVAL'),
        enrollmentRequestApi.getManagerEnrollmentDemand(),
        courseApi.getManagerOnlineEnrollmentsPage({ page: 0, size: 1 }),
        supportApi.pageQueue({ page: 0, size: 1 }, 'manager'),
        classroomApi.getStaffDashboard(),
      ]);
      setData({ proposals, demand, enrollments, tickets, operations });
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải bảng điều khiển quản lý.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  if (loading) {
    return <div className="min-h-[360px] animate-pulse rounded-2xl bg-slate-100" />;
  }

  if (error) {
    return (
      <div className="flex min-h-[360px] flex-col items-center justify-center rounded-2xl border border-rose-200 bg-white p-6 text-center">
        <p className="text-sm font-semibold text-rose-700">{error}</p>
        <button className="mt-4 rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white" onClick={load} type="button">
          Thử lại
        </button>
      </div>
    );
  }

  const pendingProposals = data?.proposals || [];
  const demand = data?.demand || [];
  const cards = [
    { label: 'Đề xuất chờ duyệt', value: pendingProposals.length, icon: ClipboardCheck, to: '/manager/classroom-proposals' },
    { label: 'Yêu cầu vận hành', value: data?.operations?.pendingChangeRequestCount || 0, icon: Users, to: '/manager/requests' },
    { label: 'Ghi danh online', value: data?.enrollments?.totalElements || 0, icon: GraduationCap, to: '/manager/online-enrollments' },
    { label: 'Yêu cầu hỗ trợ', value: data?.tickets?.totalElements || 0, icon: LifeBuoy, to: '/manager/support-tickets' },
  ];

  return (
    <div className="space-y-6">
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map((card) => (
          <Link className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-[#dfbfbd] hover:shadow-md" key={card.label} to={card.to}>
            <div className="flex items-center justify-between gap-3">
              <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-[#fff1f3] text-[#730014]">
                <card.icon className="h-5 w-5" />
              </span>
              <span className="font-['Manrope'] text-3xl font-extrabold text-[#0b1c30]">{card.value}</span>
            </div>
            <p className="mt-4 text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{card.label}</p>
          </Link>
        ))}
      </section>

      <div className="grid gap-6 xl:grid-cols-[1.25fr_0.75fr]">
        <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between gap-3">
            <h2 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Đề xuất cần duyệt</h2>
            <Link className="text-xs font-bold text-[#730014] hover:underline" to="/manager/classroom-proposals">Xem tất cả</Link>
          </div>
          <div className="mt-4 space-y-3">
            {pendingProposals.slice(0, 5).map((proposal) => (
              <Link className="flex items-center justify-between gap-4 rounded-xl border border-slate-100 bg-slate-50/60 px-4 py-3 hover:bg-[#fff7f8]" key={proposal.id} to="/manager/classroom-proposals">
                <div className="min-w-0">
                  <p className="truncate text-sm font-bold text-[#0b1c30]">{proposal.title}</p>
                  <p className="mt-1 text-xs text-slate-500">{proposal.proposalCode} · {proposal.createdAt ? formatClassroomDateTime(proposal.createdAt) : 'Chờ duyệt'}</p>
                </div>
                <ArrowRight className="h-4 w-4 shrink-0 text-[#730014]" />
              </Link>
            ))}
            {!pendingProposals.length ? <p className="py-10 text-center text-sm text-slate-500">Không có đề xuất đang chờ duyệt.</p> : null}
          </div>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Nhu cầu mở lớp</h2>
          <div className="mt-4 space-y-3">
            {demand.slice(0, 5).map((item) => (
              <div className="rounded-xl border border-slate-100 px-4 py-3" key={item.courseOfferingId}>
                <div className="flex items-start justify-between gap-3">
                  <p className="text-sm font-bold text-[#0b1c30]">{item.courseOfferingTitle}</p>
                  <span className="rounded-lg bg-[#fff1f3] px-2.5 py-1 text-xs font-extrabold text-[#730014]">{item.totalRegistrations || 0}</span>
                </div>
                <p className="mt-1 text-xs text-slate-500">Đề xuất mở {item.suggestedClassCount || 0} lớp</p>
              </div>
            ))}
            {!demand.length ? <p className="py-10 text-center text-sm text-slate-500">Chưa có nhu cầu mở lớp mới.</p> : null}
          </div>
        </section>
      </div>
    </div>
  );
}
