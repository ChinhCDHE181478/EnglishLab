import { Check, Circle, Clock3 } from 'lucide-react';

export const enrollmentStatusMeta = {
  SUBMITTED: { label: 'Đã gửi', tone: 'bg-slate-100 text-slate-700 border-slate-200' },
  INVITATION_SENT: { label: 'Đã gửi lời mời', tone: 'bg-violet-50 text-violet-700 border-violet-200' },
  TEST_SCHEDULED: { label: 'Đã hẹn lịch test', tone: 'bg-sky-50 text-sky-700 border-sky-200' },
  AWAITING_PLACEMENT_TEST: { label: 'Chờ đánh giá đầu vào', tone: 'bg-amber-50 text-amber-800 border-amber-200' },
  PLACEMENT_TEST_COMPLETED: { label: 'Đã có kết quả đầu vào', tone: 'bg-sky-50 text-sky-700 border-sky-200' },
  UNDER_STAFF_REVIEW: { label: 'Đang rà soát', tone: 'bg-violet-50 text-violet-700 border-violet-200' },
  WAITING_FOR_CLASS: { label: 'Đủ điều kiện · Chờ xếp lớp', tone: 'bg-blue-50 text-blue-700 border-blue-200' },
  CLASS_PROPOSED: { label: 'Đã có đề xuất lớp', tone: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  CLASS_ASSIGNED: { label: 'Hoàn tất · Đã xếp lớp', tone: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  REJECTED: { label: 'Đã từ chối', tone: 'bg-rose-50 text-rose-700 border-rose-200' },
  CANCELLED: { label: 'Đã hủy', tone: 'bg-slate-100 text-slate-600 border-slate-200' },
};

export function EnrollmentStatusBadge({ status, label }) {
  const meta = enrollmentStatusMeta[status] || { label: label || status, tone: 'bg-slate-100 text-slate-700 border-slate-200' };
  return (
    <span className={`inline-flex items-center whitespace-nowrap rounded-full px-3 py-1 text-xs font-extrabold border ${meta.tone}`}>
      {label || meta.label}
    </span>
  );
}

export function EnrollmentRequestTimeline({ history = [] }) {
  if (!history.length) {
    return <p className="text-sm text-slate-500">Chưa có cập nhật mới.</p>;
  }

  return (
    <ol className="space-y-0">
      {history.map((item, index) => {
        const isLatest = index === history.length - 1;
        const isComplete = ['CLASS_ASSIGNED', 'REJECTED', 'CANCELLED'].includes(item.toStatus);
        return (
          <li className="relative grid grid-cols-[28px_minmax(0,1fr)] gap-3 pb-5 last:pb-0" key={item.id || `${item.toStatus}-${index}`}>
            {index < history.length - 1 ? <span className="absolute left-[13px] top-7 h-[calc(100%-16px)] w-px bg-slate-200" /> : null}
            <span className={`relative z-10 flex h-7 w-7 items-center justify-center rounded-full ${isLatest && !isComplete ? 'bg-[#730014] text-white' : 'bg-emerald-50 text-emerald-700'}`}>
              {isLatest && !isComplete ? <Clock3 className="h-3.5 w-3.5" /> : <Check className="h-3.5 w-3.5" />}
            </span>
            <div className="pt-0.5">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm font-extrabold text-[#0b1c30]">{enrollmentStatusMeta[item.toStatus]?.label || item.statusLabel || item.toStatus}</p>
                <time className="text-xs font-semibold text-slate-400">
                  {item.createdAt ? new Date(item.createdAt).toLocaleString('vi-VN') : ''}
                </time>
              </div>
              {item.reason ? <p className="mt-1 text-sm leading-6 text-slate-600">{item.reason}</p> : null}
              <p className="mt-1 text-xs text-slate-400">{item.actorName || 'Hệ thống'}</p>
            </div>
          </li>
        );
      })}
    </ol>
  );
}

export function PlacementRequirements({ eligibility }) {
  if (!eligibility) {
    return <p className="text-sm leading-6 text-slate-600">Chưa có kết quả đánh giá đầu vào.</p>;
  }
  if (eligibility.eligible) {
    return (
      <div className="flex items-center gap-2 text-sm font-bold text-emerald-700">
        <Check className="h-4 w-4" />
        Kết quả đã đủ điều kiện đánh giá · {eligibility.recommendedLevel || 'Đã xác định trình độ'}
      </div>
    );
  }
  return (
    <div>
      <p className="text-sm font-bold text-amber-800">Kết quả chưa đủ điều kiện:</p>
      <ul className="mt-2 grid gap-2 sm:grid-cols-2">
        {(eligibility.missingRequirements || []).map((requirement) => (
          <li className="flex items-center gap-2 text-sm text-slate-600" key={requirement}>
            <Circle className="h-3 w-3 fill-amber-400 text-amber-400" />
            {placementRequirementLabel(requirement)}
          </li>
        ))}
      </ul>
    </div>
  );
}

const placementRequirementLabel = (value) => ({
  TEST_SUBMISSION: 'Chưa nộp bài đánh giá đầu vào',
  LISTENING_SCORE: 'Chưa có điểm Listening',
  READING_SCORE: 'Chưa có điểm Reading',
  WRITING_REVIEW: 'Chưa xác nhận kết quả Writing',
  SPEAKING_REVIEW: 'Chưa xác nhận kết quả Speaking',
  RECOMMENDED_LEVEL: 'Chưa chốt trình độ đề xuất',
  OVERALL_SCORE: 'Chưa có kết quả tổng thể',
  RESULT_EXPIRED: 'Kết quả đã hết hiệu lực',
  ATTEMPT_CANCELLED: 'Lượt thi đã bị hủy',
  FRAUD_REVIEW: 'Đang cần kiểm tra tính hợp lệ',
}[value] || value);
