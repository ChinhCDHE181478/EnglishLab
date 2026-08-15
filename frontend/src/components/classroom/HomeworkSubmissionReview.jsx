import { Award, CheckCircle2, Clock3, MessageSquareText, Paperclip } from 'lucide-react';
import { formatClassroomDateTime, getHomeworkMaxScore, getSubmissionFeedback } from '../../utils/classroomHelpers';
import { getHomeworkFeedbackLabel } from '../../utils/homeworkGradingConfig';
import AuthenticatedFileLink from './AuthenticatedFileLink';
import HomeworkAnnotatedText from './HomeworkAnnotatedText';
import HomeworkAiFeedbackPanel from './HomeworkAiFeedbackPanel';
import RichTextHtml from '../content-manager/RichTextHtml';

export function hasHomeworkTeacherEvaluation(submission) {
  return Boolean(
    submission
    && (submission.status === 'GRADED'
      || submission.score != null
      || getSubmissionFeedback(submission)
      || submission.annotations?.length),
  );
}

export default function HomeworkSubmissionReview({ homework, submission }) {
  if (!submission) return null;

  const annotations = Array.isArray(submission.annotations) ? submission.annotations : [];
  const feedback = getSubmissionFeedback(submission);
  const hasEvaluation = hasHomeworkTeacherEvaluation(submission);
  const textAnswer = String(submission.textAnswer || '').trim();

  return (
    <section className="overflow-hidden rounded-2xl border border-[#dfbfbd]/45 bg-white shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#dfbfbd]/25 bg-[#fffafb] px-5 py-4">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Bài đã nộp</p>
          <p className="mt-1 text-xs text-[#584140]">
            {submission.submittedAt
              ? `Nộp lúc ${formatClassroomDateTime(submission.submittedAt)}`
              : 'Đã ghi nhận bài nộp'}
          </p>
        </div>
        <span className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-extrabold ${
          hasEvaluation
            ? 'border-emerald-200 bg-emerald-50 text-emerald-800'
            : 'border-amber-200 bg-amber-50 text-amber-800'
        }`}>
          {hasEvaluation ? <CheckCircle2 className="h-3.5 w-3.5" /> : <Clock3 className="h-3.5 w-3.5" />}
          {hasEvaluation ? 'Đã có đánh giá' : 'Đang chờ đánh giá'}
        </span>
      </div>

      {hasEvaluation ? (
        <div className="border-b border-[#dfbfbd]/25 px-5 py-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2 text-[#730014]">
              <Award className="h-4 w-4" />
              <h4 className="text-xs font-extrabold uppercase tracking-[0.14em]">Đánh giá của giảng viên</h4>
            </div>
            {submission.score != null ? (
              <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-extrabold text-emerald-800">
                {submission.score} / {getHomeworkMaxScore(homework)} điểm
              </span>
            ) : null}
          </div>

          {feedback ? (
            <div className="mt-4 rounded-xl border border-[#dfbfbd]/30 bg-[#fffafb] p-4">
              <p className="flex items-center gap-1.5 text-xs font-extrabold text-[#730014]">
                <MessageSquareText className="h-4 w-4" />
                {getHomeworkFeedbackLabel(homework)}
              </p>
              <RichTextHtml className="mt-2 text-sm leading-6 text-[#584140]" value={feedback} />
            </div>
          ) : (
            <p className="mt-3 text-xs text-[#8b706e]">Giảng viên chưa để lại nhận xét tổng thể.</p>
          )}

          {submission.gradedAt ? (
            <p className="mt-3 text-[11px] text-[#8b706e]">
              Cập nhật đánh giá: {formatClassroomDateTime(submission.gradedAt)}
            </p>
          ) : null}

          {submission.aiFeedbackJson ? (
            <div className="mt-4">
              <HomeworkAiFeedbackPanel value={submission.aiFeedbackJson} />
            </div>
          ) : null}
        </div>
      ) : null}

      <div className="space-y-4 px-5 py-4">
        {textAnswer ? (
          <div className="space-y-2">
            <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
              Nội dung bài làm{annotations.length ? ` và ${annotations.length} ghi chú theo đoạn` : ''}
            </p>
            <HomeworkAnnotatedText
              annotations={annotations}
              canvasClassName="min-h-[160px]"
              className="text-xs"
              text={textAnswer}
            />
          </div>
        ) : (
          <p className="text-xs text-[#8b706e]">Bài nộp không có nội dung trả lời dạng văn bản.</p>
        )}

        {submission.attachmentUrl ? (
          <AuthenticatedFileLink
            className="inline-flex items-center gap-1.5 text-xs font-bold text-[#730014] hover:underline"
            url={submission.attachmentUrl}
          >
            <Paperclip className="h-4 w-4" />
            Mở tệp bài làm đã nộp
          </AuthenticatedFileLink>
        ) : null}
      </div>
    </section>
  );
}
