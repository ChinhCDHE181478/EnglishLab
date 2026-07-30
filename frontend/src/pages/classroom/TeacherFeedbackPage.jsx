import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, CheckCircle2, LoaderCircle, LockKeyhole, ShieldCheck, Star } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import classroomApi from '../../api/classroomApi';
import LearnerPageShell from '../../components/learner/LearnerPageShell';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { useAppDialog } from '../../components/ui/AppDialog';

const EMPTY = {
  clarityScore: 0, engagementScore: 0, learnerSupportScore: 0,
  feedbackTimelinessScore: 0, professionalismScore: 0, pace: '',
  wouldRecommend: null, strengths: '', improvementSuggestions: '', additionalComment: '',
};
const CRITERIA = [
  ['clarityScore', 'Trình bày dễ hiểu', 'Giải thích rõ ràng, có cấu trúc và ví dụ phù hợp.'],
  ['engagementScore', 'Khả năng tạo hứng thú', 'Khuyến khích tương tác và duy trì sự tập trung.'],
  ['learnerSupportScore', 'Hỗ trợ học viên', 'Lắng nghe, giải đáp và hỗ trợ đúng nhu cầu.'],
  ['feedbackTimelinessScore', 'Phản hồi bài học', 'Nhận xét bài tập kịp thời, cụ thể và hữu ích.'],
  ['professionalismScore', 'Tác phong chuyên môn', 'Đúng giờ, tôn trọng và chuẩn bị bài đầy đủ.'],
];
const formatDate = (value) => value
  ? new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`)) : '—';
const messageOf = (error, fallback) => error?.response?.data?.message
  || error?.response?.data?.error || fallback;

export default function TeacherFeedbackPage() {
  const { id } = useParams();
  const { alert: alertDialog } = useAppDialog();
  const [forms, setForms] = useState([]);
  const [teacherId, setTeacherId] = useState('');
  const [form, setForm] = useState(EMPTY);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const selected = useMemo(
    () => forms.find((item) => String(item.teacherId) === String(teacherId)),
    [forms, teacherId],
  );

  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        setLoading(true);
        setError('');
        const data = await classroomApi.getMyTeacherFeedback(id);
        if (active) {
          setForms(data);
          setTeacherId(data[0]?.teacherId ? String(data[0].teacherId) : '');
        }
      } catch (requestError) {
        if (active) setError(messageOf(requestError, 'Không thể tải phiếu đánh giá giáo viên.'));
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => { active = false; };
  }, [id]);

  useEffect(() => {
    if (!selected) {
      setForm(EMPTY);
      return;
    }
    setForm({
      clarityScore: selected.clarityScore || 0,
      engagementScore: selected.engagementScore || 0,
      learnerSupportScore: selected.learnerSupportScore || 0,
      feedbackTimelinessScore: selected.feedbackTimelinessScore || 0,
      professionalismScore: selected.professionalismScore || 0,
      pace: selected.pace || '',
      wouldRecommend: selected.wouldRecommend ?? null,
      strengths: selected.strengths || '',
      improvementSuggestions: selected.improvementSuggestions || '',
      additionalComment: selected.additionalComment || '',
    });
  }, [selected]);

  const validate = () => {
    if (CRITERIA.some(([key]) => form[key] < 1 || form[key] > 5)) return 'Vui lòng chấm đủ 5 tiêu chí.';
    if (!form.pace) return 'Vui lòng đánh giá tốc độ giảng dạy.';
    if (form.wouldRecommend == null) return 'Vui lòng chọn câu trả lời về việc giới thiệu giáo viên.';
    if (form.strengths.trim().length < 20) return 'Điểm mạnh cần ít nhất 20 ký tự và phải nêu cụ thể.';
    if (form.improvementSuggestions.trim().length < 20) return 'Góp ý cải thiện cần ít nhất 20 ký tự và phải nêu cụ thể.';
    if (form.strengths.trim().toLowerCase() === form.improvementSuggestions.trim().toLowerCase()) {
      return 'Điểm mạnh và góp ý cải thiện không được giống nhau.';
    }
    return '';
  };

  const save = async () => {
    const invalid = validate();
    if (invalid) {
      await alertDialog({ title: 'Phiếu đánh giá chưa đầy đủ', message: invalid, tone: 'warning' });
      return;
    }
    try {
      setSaving(true);
      const saved = await classroomApi.saveMyTeacherFeedback(id, selected.teacherId, form);
      setForms((current) => current.map((item) => item.teacherId === saved.teacherId ? saved : item));
      await alertDialog({
        title: selected.submitted ? 'Đã cập nhật đánh giá' : 'Đã gửi đánh giá',
        message: 'Phản hồi đã được lưu ẩn danh. Bạn vẫn có thể sửa trong thời hạn đánh giá.',
        tone: 'success',
      });
    } catch (requestError) {
      await alertDialog({
        title: 'Không thể lưu đánh giá',
        message: messageOf(requestError, 'Vui lòng kiểm tra nội dung và thử lại.'),
        tone: 'danger',
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <LearnerPageShell
      eyebrow="Phản hồi khóa học"
      title="Đánh giá giáo viên"
      description="Phản hồi của bạn được bảo mật và giúp trung tâm cải thiện chất lượng giảng dạy."
      actions={<Link className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-4 py-2 text-sm font-bold text-[#730014]" to={`/my-classrooms/${id}`}><ArrowLeft className="h-4 w-4" /> Về lớp học</Link>}
    >
      {loading ? <State icon={LoaderCircle} spin text="Đang tải phiếu đánh giá..." /> : null}
      {!loading && error ? <State text={error} danger /> : null}
      {!loading && !error && !forms.length ? <State text="Lớp học chưa có giáo viên phù hợp để đánh giá." /> : null}
      {!loading && selected ? (
        <div className="space-y-5">
          <section className="grid gap-4 rounded-[28px] border border-[#ead9db] bg-white p-6 shadow-sm md:grid-cols-2">
            <div>
              <p className="text-xs font-extrabold uppercase tracking-widest text-[#8a0018]">Giáo viên cần đánh giá</p>
              <div className="mt-3">
                <BrandedSelect
                  value={teacherId}
                  onChange={(event) => setTeacherId(event.target.value)}
                  options={forms.map((item) => ({
                    value: String(item.teacherId), label: item.teacherName,
                    description: item.submitted ? 'Đã gửi — có thể chỉnh sửa' : 'Chưa gửi đánh giá',
                  }))}
                />
              </div>
            </div>
            <div className={`rounded-2xl border p-4 ${selected.windowOpen ? 'border-emerald-200 bg-emerald-50' : 'border-amber-200 bg-amber-50'}`}>
              <div className="flex items-start gap-3">
                {selected.windowOpen ? <CheckCircle2 className="mt-0.5 h-5 w-5 text-emerald-700" /> : <LockKeyhole className="mt-0.5 h-5 w-5 text-amber-700" />}
                <div>
                  <p className="font-extrabold text-[#2b2828]">{selected.windowOpen ? 'Đang mở đánh giá' : 'Ngoài thời hạn đánh giá'}</p>
                  <p className="mt-1 text-sm leading-6 text-[#756361]">Từ {formatDate(selected.opensOn)} đến hết {formatDate(selected.closesOn)}. Bài đã gửi có thể sửa trong thời gian này.</p>
                </div>
              </div>
            </div>
          </section>
          <section className="rounded-[28px] border border-[#ead9db] bg-white p-6 shadow-sm md:p-8">
            <div className="mb-6 flex items-start gap-3 rounded-2xl bg-[#fff7f7] p-4">
              <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-[#8a0018]" />
              <p className="text-sm leading-6 text-[#584140]"><strong>Quyền riêng tư:</strong> giáo viên chỉ xem số liệu tổng hợp khi đủ số phản hồi tối thiểu; không xem tên học viên, từng phiếu hay bình luận cá nhân.</p>
            </div>
            <fieldset disabled={!selected.editable || saving} className="space-y-7 disabled:opacity-70">
              <div className="space-y-5">
                {CRITERIA.map(([key, label, hint]) => (
                  <RatingRow key={key} label={label} hint={hint} value={form[key]} onChange={(value) => setForm((current) => ({ ...current, [key]: value }))} />
                ))}
              </div>
              <Field label="Tốc độ giảng dạy" required>
                <BrandedSelect
                  value={form.pace}
                  onChange={(event) => setForm((current) => ({ ...current, pace: event.target.value }))}
                  options={[
                    { value: 'TOO_SLOW', label: 'Hơi chậm', description: 'Cần tăng nhịp độ hoặc khối lượng kiến thức.' },
                    { value: 'JUST_RIGHT', label: 'Phù hợp', description: 'Tốc độ cân bằng với khả năng tiếp thu.' },
                    { value: 'TOO_FAST', label: 'Hơi nhanh', description: 'Cần thêm thời gian giải thích và luyện tập.' },
                  ]}
                  placeholder="Chọn nhận xét về tốc độ"
                />
              </Field>
              <Field label="Bạn có sẵn sàng giới thiệu giáo viên này?" required>
                <div className="grid grid-cols-2 gap-3">
                  {[[true, 'Có'], [false, 'Chưa']].map(([value, label]) => (
                    <button key={label} type="button" onClick={() => setForm((current) => ({ ...current, wouldRecommend: value }))} className={`rounded-xl border px-4 py-3 font-bold transition ${form.wouldRecommend === value ? 'border-[#730014] bg-[#730014] text-white' : 'border-[#ead9db] bg-white text-[#584140]'}`}>{label}</button>
                  ))}
                </div>
              </Field>
              <TextField label="Điểm mạnh của giáo viên" value={form.strengths} required hint="Nêu hành vi hoặc ví dụ cụ thể; tối thiểu 20 ký tự." onChange={(value) => setForm((current) => ({ ...current, strengths: value }))} />
              <TextField label="Điều giáo viên có thể cải thiện" value={form.improvementSuggestions} required hint="Góp ý mang tính xây dựng và có thể thực hiện; tối thiểu 20 ký tự." onChange={(value) => setForm((current) => ({ ...current, improvementSuggestions: value }))} />
              <TextField label="Nhận xét bổ sung" value={form.additionalComment} hint="Không bắt buộc." onChange={(value) => setForm((current) => ({ ...current, additionalComment: value }))} />
            </fieldset>
            <div className="mt-8 flex flex-wrap items-center justify-between gap-3 border-t border-[#f0e3e4] pt-6">
              <p className="text-xs text-[#8c716f]">{selected.submitted ? 'Đã gửi — bạn có thể tiếp tục chỉnh sửa trong thời hạn.' : 'Bạn chưa gửi đánh giá cho giáo viên này.'}</p>
              <button disabled={!selected.editable || saving} onClick={save} type="button" className="inline-flex items-center gap-2 rounded-xl bg-[#730014] px-6 py-3 text-sm font-extrabold text-white transition hover:bg-[#5c0010] disabled:cursor-not-allowed disabled:opacity-50">
                {saving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <ShieldCheck className="h-4 w-4" />}
                {selected.submitted ? 'Lưu thay đổi' : 'Gửi đánh giá ẩn danh'}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </LearnerPageShell>
  );
}

function RatingRow({ label, hint, value, onChange }) {
  return <div className="grid gap-3 rounded-2xl border border-[#eee2e3] p-4 md:grid-cols-[1fr_auto] md:items-center"><div><p className="font-extrabold text-[#2b2828]">{label} <span className="text-[#a0001b]">*</span></p><p className="mt-1 text-xs leading-5 text-[#756361]">{hint}</p></div><div aria-label={`Chấm điểm ${label}`} className="flex gap-1.5">{[1, 2, 3, 4, 5].map((score) => <button aria-label={`${score} trên 5`} key={score} onClick={() => onChange(score)} type="button" className="rounded-lg p-1.5 transition hover:bg-[#fff0f1]"><Star className={`h-6 w-6 ${score <= value ? 'fill-[#a0001b] text-[#a0001b]' : 'text-[#d9c9ca]'}`} /></button>)}</div></div>;
}
function Field({ label, required = false, children }) {
  return <div><label className="mb-2 block text-sm font-extrabold text-[#2b2828]">{label} {required ? <span className="text-[#a0001b]">*</span> : null}</label>{children}</div>;
}
function TextField({ label, value, onChange, hint, required = false }) {
  return <Field label={label} required={required}><textarea className="min-h-28 w-full resize-y rounded-2xl border border-[#dfbfbd] bg-white p-4 text-sm leading-6 text-[#2b2828] outline-none focus:border-[#8a0018]" maxLength={1500} onChange={(event) => onChange(event.target.value)} value={value} /><div className="mt-1 flex justify-between gap-3 text-xs text-[#8c716f]"><span>{hint}</span><span>{value.length}/1.500</span></div></Field>;
}
function State({ text, icon: Icon, spin = false, danger = false }) {
  return <div className={`flex min-h-[520px] flex-1 items-center justify-center rounded-[28px] border p-8 text-center text-sm font-semibold ${danger ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-[#ead9db] bg-white text-[#756361]'}`}>{Icon ? <Icon className={`mr-2 h-5 w-5 ${spin ? 'animate-spin' : ''}`} /> : null}{text}</div>;
}
