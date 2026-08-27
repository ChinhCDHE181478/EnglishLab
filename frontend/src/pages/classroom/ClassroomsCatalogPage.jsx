import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  ArrowRight,
  CheckCircle2,
  GraduationCap,
  MapPin,
  MessageSquare,
  Phone,
  RefreshCw,
  Search,
  Video,
} from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import Header from '../../components/ai-learning/Header';
import CourseFooter from '../../components/course/CourseFooter';
import RichTextHtml from '../../components/content-manager/RichTextHtml';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { getStoredUser, hasAccessToken } from '../../utils/auth';
import {
  PAGE_BODY_CLASS,
  PAGE_HEADER_CLASS,
  PAGE_MAIN_STACK_CLASS,
  PAGE_SHELL_CLASS,
} from '../../utils/pageLayout';

const ACTIVE_REQUEST_STATUSES = new Set([
  'SUBMITTED',
  'INVITATION_SENT',
  'TEST_SCHEDULED',
  'AWAITING_PLACEMENT_TEST',
  'PLACEMENT_TEST_COMPLETED',
  'UNDER_STAFF_REVIEW',
  'WAITING_FOR_CLASS',
  'CLASS_PROPOSED',
]);

const CONSULTATION_OPTIONS = [
  { value: 'IELTS_4_SKILLS', label: 'IELTS 4 kỹ năng' },
  { value: 'TOEIC_2_SKILLS', label: 'TOEIC Listening & Reading' },
  { value: 'TOEIC_4_SKILLS', label: 'TOEIC 4 kỹ năng' },
  { value: 'ENGLISH_FOUNDATION', label: 'Tiếng Anh nền tảng & giao tiếp' },
];

const PREFERRED_DAYS = [
  { label: 'Thứ 2', value: 'MONDAY' },
  { label: 'Thứ 3', value: 'TUESDAY' },
  { label: 'Thứ 4', value: 'WEDNESDAY' },
  { label: 'Thứ 5', value: 'THURSDAY' },
  { label: 'Thứ 6', value: 'FRIDAY' },
  { label: 'Thứ 7', value: 'SATURDAY' },
  { label: 'Chủ nhật', value: 'SUNDAY' },
];

const PREFERRED_SHIFTS = [
  { label: 'Sáng', time: '08:00–11:30', value: 'MORNING' },
  { label: 'Chiều', time: '13:30–17:30', value: 'AFTERNOON' },
  { label: 'Tối', time: '18:00–21:30', value: 'EVENING' },
];

const DRAFT_STORAGE_KEY = 'englishlab-course-consultation-draft';

const serializePreferredSchedule = (slots) => PREFERRED_DAYS
  .map((day) => {
    const shifts = PREFERRED_SHIFTS
      .filter((shift) => slots.includes(`${day.value}_${shift.value}`))
      .map((shift) => shift.label);
    return shifts.length ? `${day.label}: ${shifts.join(', ')}` : '';
  })
  .filter(Boolean)
  .join(' · ');

const readSavedDraft = () => {
  try {
    return JSON.parse(sessionStorage.getItem(DRAFT_STORAGE_KEY) || 'null') || {};
  } catch {
    return {};
  }
};

const createInitialForm = (user) => ({
  courseOfferingId: '',
  contactName: user?.fullName || '',
  contactPhone: user?.phoneNumber || '',
  contactEmail: user?.email || '',
  consultationTrack: 'IELTS_4_SKILLS',
  schoolOrCompany: '',
  scoreGoal: user?.studyGoal || '',
  facebookUrl: '',
  preferredSchedule: '',
  preferredScheduleSlots: [],
  notes: '',
  ...readSavedDraft(),
});

const suggestedTrack = (program) => {
  const category = String(program?.examType || program?.examCategory || program?.instructorLedCourseExamType || program?.code || '').toUpperCase();
  if (category.includes('TOEIC')) return 'TOEIC_2_SKILLS';
  if (category.includes('IELTS')) return 'IELTS_4_SKILLS';
  return 'ENGLISH_FOUNDATION';
};

export default function ClassroomsCatalogPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const currentUser = getStoredUser();
  const isAuthenticated = Boolean(hasAccessToken() && currentUser);

  const [programs, setPrograms] = useState([]);
  const [activeRequests, setActiveRequests] = useState([]);
  const [form, setForm] = useState(() => createInitialForm(currentUser));
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [formError, setFormError] = useState('');
  const [success, setSuccess] = useState('');
  const [deliveryFilter, setDeliveryFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const programData = await enrollmentRequestApi.getCourseOfferings();
      setPrograms(programData);
      if (isAuthenticated) {
        try {
          const mine = await enrollmentRequestApi.listMine();
          setActiveRequests(mine.filter((item) => ACTIVE_REQUEST_STATUSES.has(item.status)));
        } catch {
          setActiveRequests([]);
        }
      }
    } catch (loadError) {
      setPrograms([]);
      setError(loadError?.response?.data?.message || 'Không thể tải danh sách khóa học đang nhận đăng ký.');
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (location.hash !== '#dang-ky-tu-van') return;
    const frameId = window.requestAnimationFrame(() => {
      document.getElementById('dang-ky-tu-van')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
    return () => window.cancelAnimationFrame(frameId);
  }, [location.hash]);

  const filteredPrograms = useMemo(() => {
    const normalized = searchQuery.trim().toLocaleLowerCase('vi-VN');
    return programs.filter((program) => {
      const deliveryMode = program.deliveryMode || program.deliveryType;
      if (deliveryFilter !== 'ALL' && deliveryMode !== deliveryFilter) return false;
      if (!normalized) return true;
      return [
        program.title,
        program.code,
        program.shortDescription,
        program.entryLevel,
        program.targetScore,
      ].filter(Boolean).join(' ').toLocaleLowerCase('vi-VN').includes(normalized);
    });
  }, [deliveryFilter, programs, searchQuery]);

  const activeProgramIds = useMemo(
    () => new Set(activeRequests.map((item) => String(item.courseOfferingId)).filter(Boolean)),
    [activeRequests],
  );

  useEffect(() => {
    const requestedProgramId = new URLSearchParams(location.search).get('programId');
    if (!requestedProgramId || !programs.length || activeProgramIds.has(String(requestedProgramId))) return;
    const requestedProgram = programs.find((program) => String(program.id) === String(requestedProgramId));
    if (!requestedProgram) return;
    setForm((current) => String(current.courseOfferingId) === String(requestedProgramId) ? current : ({
      ...current,
      courseOfferingId: String(requestedProgram.id),
      consultationTrack: suggestedTrack(requestedProgram),
    }));
  }, [activeProgramIds, location.search, programs]);

  const programOptions = useMemo(() => programs
    .filter((program) => !activeProgramIds.has(String(program.id)))
    .map((program) => ({
    value: String(program.id),
    label: program.title,
    description: [
      program.deliveryMode === 'VIRTUAL' ? 'Trực tuyến' : 'Tại trung tâm',
      program.entryLevel ? `Đầu vào ${program.entryLevel}` : null,
      program.targetScore ? `Mục tiêu ${program.targetScore}` : null,
    ].filter(Boolean).join(' · '),
    })), [activeProgramIds, programs]);

  const updateField = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
    setFormError('');
  };

  const togglePreferredSchedule = (slot) => {
    setForm((current) => {
      const selected = new Set(
        Array.isArray(current.preferredScheduleSlots) ? current.preferredScheduleSlots : [],
      );
      if (selected.has(slot)) selected.delete(slot);
      else selected.add(slot);
      const preferredScheduleSlots = [...selected];
      return {
        ...current,
        preferredSchedule: serializePreferredSchedule(preferredScheduleSlots),
        preferredScheduleSlots,
      };
    });
    setFormError('');
  };

  const selectProgram = (program) => {
    if (activeProgramIds.has(String(program.id))) {
      setFormError('Bạn đã có một hồ sơ đang được xử lý cho khóa học này. Bạn vẫn có thể chọn khóa học khác.');
      document.getElementById('dang-ky-tu-van')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      return;
    }
    setForm((current) => ({
      ...current,
      courseOfferingId: String(program.id),
      consultationTrack: suggestedTrack(program),
    }));
    document.getElementById('dang-ky-tu-van')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const validateForm = () => {
    if (!form.courseOfferingId) return 'Vui lòng chọn khóa học bạn quan tâm.';
    if (activeProgramIds.has(String(form.courseOfferingId))) {
      return 'Bạn đã có một hồ sơ đang được xử lý cho khóa học này. Vui lòng chọn khóa học khác.';
    }
    if (!form.contactName.trim() || !form.contactPhone.trim() || !form.contactEmail.trim()) {
      return 'Vui lòng điền Họ tên, Số điện thoại và Email.';
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.contactEmail.trim())) {
      return 'Email chưa đúng định dạng.';
    }
    if (form.contactPhone.replace(/\D/g, '').length < 9) return 'Số điện thoại không hợp lệ.';
    return '';
  };

  const submit = async (event) => {
    event.preventDefault();
    const validationMessage = validateForm();
    if (validationMessage) {
      setFormError(validationMessage);
      return;
    }
    if (!isAuthenticated) {
      sessionStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(form));
      navigate('/login', { state: { from: '/opening-schedule#dang-ky-tu-van' } });
      return;
    }

    setSubmitting(true);
    setFormError('');
    setSuccess('');
    const studyWorkGoal = [
      form.schoolOrCompany.trim() ? `Trường/Nơi làm: ${form.schoolOrCompany.trim()}` : '',
      form.scoreGoal.trim() ? `Mục tiêu: ${form.scoreGoal.trim()}` : '',
      form.notes.trim() ? `Ghi chú: ${form.notes.trim()}` : '',
    ].filter(Boolean).join(' | ');

    try {
      const saved = await enrollmentRequestApi.submit({
        courseOfferingId: Number(form.courseOfferingId),
        contactName: form.contactName.trim(),
        contactPhone: form.contactPhone.trim(),
        contactEmail: form.contactEmail.trim(),
        facebookUrl: form.facebookUrl.trim() || null,
        consultationTrack: form.consultationTrack,
        studyWorkGoal: studyWorkGoal || null,
        preferredSchedule: form.preferredSchedule.trim() || null,
      });
      sessionStorage.removeItem(DRAFT_STORAGE_KEY);
      setActiveRequests((current) => [
        saved,
        ...current.filter((item) => item.id !== saved.id),
      ]);
      setForm((current) => ({ ...current, courseOfferingId: '' }));
      setSuccess('Đã gửi đăng ký. Đội ngũ tư vấn sẽ liên hệ để xác nhận lịch tư vấn và đánh giá đầu vào tại trung tâm.');
    } catch (submitError) {
      setFormError(submitError?.response?.data?.message || 'Không thể gửi form đăng ký. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={PAGE_SHELL_CLASS}>
      <CourseGlobalStyles />
      <div className={PAGE_HEADER_CLASS}><Header /></div>
      <div className={PAGE_BODY_CLASS}>
        <main className={`${PAGE_MAIN_STACK_CLASS} gap-8 py-6`}>
          <section className="rounded-3xl border border-[#dfbfbd]/50 bg-[#4b0009] px-6 py-8 text-white shadow-md md:px-10 md:py-10">
            <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="text-xs font-bold uppercase tracking-widest text-[#ffcdd2]">ĐĂNG KÝ HỌC TẠI ENGLISHLAB</p>
                <h1 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-white">
                  Chọn khóa học, EnglishLab sẽ tư vấn lớp phù hợp
                </h1>
                <p className="mt-3 max-w-2xl text-sm leading-6 text-white/80">
                  Chọn khóa học bạn quan tâm. Đội ngũ tư vấn sẽ liên hệ, hẹn lịch đánh giá đầu vào
                  và đề xuất lớp phù hợp với kết quả thực tế.
                </p>
              </div>
              <div className="grid shrink-0 gap-2 rounded-2xl border border-white/15 bg-white/10 px-5 py-4 text-xs backdrop-blur-sm">
                <span className="flex items-center gap-2"><Phone className="h-4 w-4 text-[#ffcdd2]" />Hotline: 0988.123.456</span>
                <span className="flex items-center gap-2"><MapPin className="h-4 w-4 text-[#ffcdd2]" />EnglishLab Campus, Hà Nội</span>
              </div>
            </div>
          </section>

          <section className="space-y-4">
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <div>
                <h2 className="font-['Manrope'] text-2xl font-black text-slate-900">Khóa học đang nhận đăng ký</h2>
                <p className="mt-1 text-sm text-slate-500">Danh sách được cập nhật theo kế hoạch tuyển sinh của trung tâm.</p>
              </div>
              <div className="flex flex-col gap-2 sm:flex-row">
                <div className="w-full sm:w-48">
                  <BrandedSelect
                    onChange={(event) => setDeliveryFilter(event.target.value)}
                    options={[
                      { label: 'Tất cả hình thức', value: 'ALL' },
                      { label: 'Tại trung tâm', value: 'OFFLINE' },
                      { label: 'Trực tuyến', value: 'VIRTUAL' },
                    ]}
                    value={deliveryFilter}
                  />
                </div>
                <label className="relative w-full sm:w-64">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                  <input
                    className="h-11 w-full rounded-xl border border-slate-200 bg-white pl-9 pr-3 text-sm outline-none focus:border-[#730014]"
                    onChange={(event) => setSearchQuery(event.target.value)}
                    placeholder="Tìm khóa học..."
                    value={searchQuery}
                  />
                </label>
              </div>
            </div>

            {loading ? <CourseLoading /> : null}
            {!loading && error ? <CourseError message={error} onRetry={load} /> : null}
            {!loading && !error && !filteredPrograms.length ? <CourseEmpty /> : null}
            {!loading && !error && filteredPrograms.length ? (
              <ProgramList
                registeredProgramIds={activeProgramIds}
                onSelect={selectProgram}
                programs={filteredPrograms}
                selectedProgramId={form.courseOfferingId}
              />
            ) : null}
          </section>

          <section className="scroll-mt-24" id="dang-ky-tu-van">
            <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
              <div className="mb-6 border-b border-slate-100 pb-4">
                <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-[#730014]">
                  <MessageSquare className="h-4 w-4" />Đăng ký học & nhận tư vấn
                </div>
                <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-slate-900">Thông tin đăng ký</h2>
                <p className="mt-1 text-xs text-slate-500">
                  Đội ngũ tư vấn sẽ liên hệ để xác nhận ngày giờ bạn đến trung tâm.
                </p>
              </div>

              {activeRequests.length ? (
                <div className="mb-5 flex flex-col gap-3 rounded-2xl border border-blue-100 bg-blue-50 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-sm font-extrabold text-blue-900">
                      Bạn có {activeRequests.length} hồ sơ đăng ký đang được xử lý
                    </p>
                    <p className="mt-1 text-xs leading-5 text-blue-700">
                      Bạn vẫn có thể đăng ký thêm khóa học khác; mỗi khóa chỉ có một hồ sơ đang hoạt động.
                    </p>
                  </div>
                  <button className="shrink-0 rounded-xl border border-blue-200 bg-white px-4 py-2 text-xs font-bold text-blue-800" onClick={() => navigate('/my-enrollment-requests')} type="button">
                    Theo dõi hồ sơ
                  </button>
                </div>
              ) : null}

              {success ? (
                <div className="mb-5 flex items-start gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3">
                  <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
                  <div>
                    <p className="text-sm font-extrabold text-emerald-900">Hồ sơ đăng ký đã được tiếp nhận</p>
                    <p className="mt-1 text-xs leading-5 text-emerald-700">{success} Bạn có thể tiếp tục chọn một khóa học khác.</p>
                  </div>
                </div>
              ) : null}

              <form className="grid gap-4 md:grid-cols-2" onSubmit={submit}>
                  <div className="md:col-span-2">
                    <FieldLabel>Khóa học quan tâm *</FieldLabel>
                    <BrandedSelect
                      onChange={(event) => {
                        const program = programs.find((item) => String(item.id) === event.target.value);
                        setForm((current) => ({
                          ...current,
                          courseOfferingId: event.target.value,
                          consultationTrack: suggestedTrack(program),
                        }));
                        setFormError('');
                      }}
                      options={programOptions}
                      placeholder="Chọn một khóa học"
                      searchable
                      value={form.courseOfferingId}
                    />
                  </div>
                  <FormField label="Họ và tên *" maxLength={100} onChange={(value) => updateField('contactName', value)} placeholder="Nguyễn Văn A" required value={form.contactName} />
                  <FormField label="Số điện thoại *" maxLength={30} onChange={(value) => updateField('contactPhone', value)} placeholder="0912 345 678" required type="tel" value={form.contactPhone} />
                  <FormField label="Email liên hệ *" maxLength={150} onChange={(value) => updateField('contactEmail', value)} placeholder="nguyenvana@gmail.com" required type="email" value={form.contactEmail} />
                  <div>
                    <FieldLabel>Nhu cầu tư vấn *</FieldLabel>
                    <BrandedSelect onChange={(event) => updateField('consultationTrack', event.target.value)} options={CONSULTATION_OPTIONS} value={form.consultationTrack} />
                  </div>
                  <FormField label="Trường học / Nơi làm việc" maxLength={200} onChange={(value) => updateField('schoolOrCompany', value)} placeholder="ĐH FPT, người đi làm..." value={form.schoolOrCompany} />
                  <FormField label="Mục tiêu điểm số" maxLength={200} onChange={(value) => updateField('scoreGoal', value)} placeholder="IELTS 6.5, TOEIC 750..." value={form.scoreGoal} />
                  <PreferredScheduleField
                    onToggle={togglePreferredSchedule}
                    value={form.preferredScheduleSlots}
                  />
                  <FormField label="Facebook / Zalo" maxLength={500} onChange={(value) => updateField('facebookUrl', value)} placeholder="https://facebook.com/..." value={form.facebookUrl} />
                  <label className="block md:col-span-2">
                    <FieldLabel>Ghi chú thêm</FieldLabel>
                    <textarea className="w-full rounded-xl border border-slate-200 bg-white p-3 text-sm outline-none focus:border-[#730014]" onChange={(event) => updateField('notes', event.target.value)} placeholder="Nhu cầu tư vấn hoặc lưu ý khác..." rows={3} value={form.notes} />
                  </label>
                  {formError ? <div className="md:col-span-2"><FormNotice message={formError} /></div> : null}
                  <div className="flex flex-col items-center pt-2 text-center md:col-span-2">
                    <button className="inline-flex items-center justify-center rounded-xl bg-[#4b0009] px-7 py-3 text-sm font-bold text-white transition hover:bg-[#730014] disabled:opacity-60" disabled={submitting} type="submit">
                      {submitting ? 'Đang gửi...' : isAuthenticated ? 'Gửi đăng ký' : 'Đăng nhập để đăng ký'}
                    </button>
                    <span className="mt-2 text-[11px] text-slate-500">Thông tin chỉ được dùng cho tư vấn và xếp lớp.</span>
                  </div>
              </form>
            </div>
          </section>
        </main>
        <CourseFooter />
      </div>
    </div>
  );
}

function ProgramList({ onSelect, programs, registeredProgramIds, selectedProgramId }) {
  return (
    <>
      <div className="hidden overflow-hidden rounded-2xl border border-[#dcc0bf]/30 bg-white shadow-sm md:block">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[980px] border-collapse text-left">
            <thead className="border-b border-[#dcc0bf]/30 bg-[#fbf3f4] text-xs font-bold uppercase tracking-wider text-[#8e7371]">
              <tr>
                <th className="px-4 py-3">Mã khóa học</th>
                <th className="px-4 py-3">Khóa học</th>
                <th className="px-4 py-3">Hình thức</th>
                <th className="px-4 py-3">Đầu vào</th>
                <th className="px-4 py-3">Mục tiêu</th>
                <th className="px-4 py-3">Thời lượng</th>
                <th className="w-28 px-4 py-3 text-center">Đăng ký</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {programs.map((program) => {
                const selected = String(program.id) === String(selectedProgramId);
                return (
                  <ProgramTableRow
                    key={program.id}
                    onSelect={() => onSelect(program)}
                    program={program}
                    registered={registeredProgramIds.has(String(program.id))}
                    selected={selected}
                  />
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      <div className="divide-y divide-slate-100 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm md:hidden">
        {programs.map((program) => (
          <ProgramMobileRow
            key={program.id}
            onSelect={() => onSelect(program)}
            program={program}
            registered={registeredProgramIds.has(String(program.id))}
            selected={String(program.id) === String(selectedProgramId)}
          />
        ))}
      </div>
    </>
  );
}

function ProgramTableRow({ onSelect, program, registered, selected }) {
  const virtual = (program.deliveryMode || program.deliveryType) === 'VIRTUAL';
  return (
    <tr className={`text-sm transition ${selected ? 'bg-[#fff3f4]' : 'odd:bg-white even:bg-slate-50/70 hover:bg-[#fff8f8]'}`}>
      <td className="px-4 py-3 align-top">
        <span className="font-extrabold text-[#a0001c]">{program.code}</span>
      </td>
      <td className="max-w-xs px-4 py-3 align-top">
        <p className="font-extrabold text-slate-900">{program.title}</p>
        <p className="mt-1 text-[10px] font-extrabold uppercase tracking-[0.12em] text-[#8a0018]">
          {(program.examType || program.examCategory || program.instructorLedCourseExamType) === 'GENERAL_ENGLISH' ? 'General English' : (program.examType || program.examCategory || program.instructorLedCourseExamType || 'IELTS')}
          {program.focusSkills ? ` · ${program.focusSkills.split(',').join(' · ')}` : ''}
        </p>
        <RichTextHtml
          asPlain
          className="mt-1 line-clamp-2 text-xs leading-5 text-slate-500"
          value={program.shortDescription || program.description || 'Lộ trình được thiết kế theo chuẩn đầu ra EnglishLab.'}
        />
      </td>
      <td className="px-4 py-3 align-top">
        <DeliveryBadge virtual={virtual} />
      </td>
      <td className="px-4 py-3 align-top font-semibold text-slate-700">{program.entryLevel || 'Test đầu vào'}</td>
      <td className="px-4 py-3 align-top font-semibold text-slate-700">{program.targetScore || 'Theo lộ trình'}</td>
      <td className="px-4 py-3 align-top text-slate-600">{program.duration || 'Đang cập nhật'}</td>
      <td className="px-4 py-3 text-center align-middle">
        <SelectProgramButton onSelect={onSelect} registered={registered} selected={selected} />
      </td>
    </tr>
  );
}

function ProgramMobileRow({ onSelect, program, registered, selected }) {
  const virtual = (program.deliveryMode || program.deliveryType) === 'VIRTUAL';
  return (
    <article className={selected ? 'bg-[#fff3f4] p-4' : 'bg-white p-4'}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-extrabold text-[#a0001c]">{program.code}</p>
          <h3 className="mt-1 font-['Manrope'] text-base font-black text-slate-900">{program.title}</h3>
          <p className="mt-1 text-[10px] font-bold uppercase tracking-[0.1em] text-[#8a0018]">
            {(program.examType || program.examCategory || program.instructorLedCourseExamType) === 'GENERAL_ENGLISH' ? 'General English' : (program.examType || program.examCategory || program.instructorLedCourseExamType || 'IELTS')}
            {program.focusSkills ? ` · ${program.focusSkills.split(',').join(' · ')}` : ''}
          </p>
        </div>
        <DeliveryBadge virtual={virtual} />
      </div>
      <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-2 text-xs">
        <ProgramDetail label="Đầu vào" value={program.entryLevel || 'Test đầu vào'} />
        <ProgramDetail label="Mục tiêu" value={program.targetScore || 'Theo lộ trình'} />
        <ProgramDetail label="Thời lượng" value={program.duration || 'Đang cập nhật'} />
      </dl>
      <SelectProgramButton className="mt-4 w-full" onSelect={onSelect} registered={registered} selected={selected} />
    </article>
  );
}

function DeliveryBadge({ virtual }) {
  return (
    <span className={`inline-flex whitespace-nowrap items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-bold ${virtual ? 'bg-blue-50 text-blue-700' : 'bg-emerald-50 text-emerald-700'}`}>
      {virtual ? <Video className="h-3 w-3" /> : <MapPin className="h-3 w-3" />}
      {virtual ? 'Trực tuyến' : 'Tại trung tâm'}
    </span>
  );
}

function ProgramDetail({ label, value }) {
  return <div><dt className="font-bold uppercase tracking-wide text-slate-400">{label}</dt><dd className="mt-0.5 font-semibold text-slate-700">{value}</dd></div>;
}

function SelectProgramButton({ className = '', onSelect, registered, selected }) {
  return (
    <button
      className={`${className} inline-flex items-center justify-center gap-1.5 rounded-lg px-3 py-2 text-xs font-extrabold transition ${
        registered
          ? 'cursor-not-allowed border border-slate-200 bg-slate-100 text-slate-500'
          : selected
          ? 'border border-emerald-200 bg-emerald-50 text-emerald-700'
          : 'bg-[#4b0009] text-white hover:bg-[#730014]'
      }`}
      disabled={registered}
      onClick={onSelect}
      type="button"
    >
      {registered
        ? <><CheckCircle2 className="h-3.5 w-3.5" />Đã đăng ký</>
        : selected
          ? <><CheckCircle2 className="h-3.5 w-3.5" />Đã chọn</>
          : <>Chọn <ArrowRight className="h-3.5 w-3.5" /></>}
    </button>
  );
}

function FormField({ label, maxLength, onChange, placeholder, required = false, type = 'text', value }) {
  return (
    <label className="block">
      <FieldLabel>{label}</FieldLabel>
      <input className="h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none transition focus:border-[#730014]" maxLength={maxLength} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} required={required} type={type} value={value} />
    </label>
  );
}

function PreferredScheduleField({ onToggle, value }) {
  const selected = new Set(Array.isArray(value) ? value : []);
  return (
    <fieldset className="md:col-span-2">
      <legend className="mb-1.5 text-xs font-bold text-slate-800">
        Khung giờ học mong muốn <span className="font-medium text-slate-400">(tích các ca có thể học)</span>
      </legend>
      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
        <table className="w-full min-w-[540px] border-collapse text-sm">
          <thead className="bg-slate-50">
            <tr>
              <th className="w-32 border-b border-r border-slate-200 px-3 py-3 text-left font-extrabold text-slate-700">
                Ngày
              </th>
              {PREFERRED_SHIFTS.map((shift) => (
                <th className="border-b border-slate-200 px-3 py-3 text-center" key={shift.value}>
                  <span className="block font-extrabold text-slate-700">{shift.label}</span>
                  <span className="mt-0.5 block text-[10px] font-medium text-slate-400">{shift.time}</span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {PREFERRED_DAYS.map((day) => (
              <tr className="odd:bg-white even:bg-slate-50/60" key={day.value}>
                <th className="border-r border-slate-200 px-3 py-3 text-left font-bold text-slate-700">
                  {day.label}
                </th>
                {PREFERRED_SHIFTS.map((shift) => {
                  const slot = `${day.value}_${shift.value}`;
                  const checked = selected.has(slot);
                  return (
                    <td className={checked ? 'bg-[#fff3f4] px-3 py-3 text-center' : 'px-3 py-3 text-center'} key={shift.value}>
                      <label className="inline-flex cursor-pointer items-center justify-center rounded-lg p-1.5">
                        <input
                          aria-label={`${day.label} ca ${shift.label}`}
                          checked={checked}
                          className="h-5 w-5 cursor-pointer accent-[#730014]"
                          onChange={() => onToggle(slot)}
                          type="checkbox"
                        />
                      </label>
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </fieldset>
  );
}

function FieldLabel({ children }) {
  return <span className="mb-1.5 block text-xs font-bold text-slate-800">{children}</span>;
}

function FormNotice({ message }) {
  return <div className="flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-4 py-2.5 text-xs font-semibold text-rose-700"><AlertCircle className="h-4 w-4 shrink-0" />{message}</div>;
}

function CourseLoading() {
  return <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white">{Array.from({ length: 6 }).map((_, index) => <div className="h-16 animate-pulse border-b border-slate-100 bg-slate-50 last:border-b-0" key={index} />)}</div>;
}

function CourseError({ message, onRetry }) {
  return <div className="flex min-h-64 flex-col items-center justify-center rounded-2xl border border-rose-200 bg-white p-6 text-center"><AlertCircle className="h-9 w-9 text-rose-400" /><p className="mt-3 text-sm text-slate-600">{message}</p><button className="mt-4 inline-flex items-center gap-2 rounded-xl bg-[#4b0009] px-4 py-2 text-sm font-bold text-white" onClick={onRetry} type="button"><RefreshCw className="h-4 w-4" />Thử lại</button></div>;
}

function CourseEmpty() {
  return <div className="flex min-h-64 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center"><GraduationCap className="h-10 w-10 text-slate-300" /><p className="mt-3 font-bold text-slate-700">Không tìm thấy khóa học phù hợp</p><p className="mt-1 text-sm text-slate-400">Hãy đổi từ khóa hoặc hình thức học.</p></div>;
}
