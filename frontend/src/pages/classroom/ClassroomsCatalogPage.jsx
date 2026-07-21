import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  Calendar,
  CalendarDays,
  CheckCircle2,
  Clock,
  MapPin,
  MessageSquare,
  Phone,
  RefreshCw,
  Search,
  Video,
} from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import classroomApi from '../../api/classroomApi';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import Header from '../../components/ai-learning/Header';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { getStoredUser, hasAccessToken } from '../../utils/auth';
import { formatClassroomDate } from '../../utils/classroomHelpers';
import {
  PAGE_BODY_CLASS,
  PAGE_HEADER_CLASS,
  PAGE_MAIN_STACK_CLASS,
  PAGE_SHELL_CLASS,
} from '../../utils/pageLayout';

const ACTIVE_REQUEST_STATUSES = new Set([
  'SUBMITTED',
  'AWAITING_PLACEMENT_TEST',
  'PLACEMENT_TEST_COMPLETED',
  'UNDER_STAFF_REVIEW',
  'WAITING_FOR_CLASS',
  'CLASS_PROPOSED',
]);

const CONSULTATION_OPTIONS = [
  { value: 'IELTS_4_SKILLS', label: 'Luyện thi IELTS 4 kỹ năng' },
  { value: 'TOEIC_2_SKILLS', label: 'Luyện thi TOEIC 2 kỹ năng (Listening & Reading)' },
  { value: 'TOEIC_4_SKILLS', label: 'Luyện thi TOEIC 4 kỹ năng (Nghe, Đọc, Nói, Viết)' },
  { value: 'ENGLISH_FOUNDATION', label: 'Tiếng Anh nền tảng & Giao tiếp' },
];

const DRAFT_STORAGE_KEY = 'englishlab-opening-schedule-consultation-draft';

const isUpcomingOffering = (offering) => {
  if (offering?.classroomStatus !== 'UPCOMING' || !offering?.startDate) return false;
  const startDate = new Date(`${offering.startDate}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return !Number.isNaN(startDate.getTime()) && startDate > today;
};

const readSavedDraft = () => {
  try {
    return JSON.parse(sessionStorage.getItem(DRAFT_STORAGE_KEY) || 'null') || {};
  } catch {
    return {};
  }
};

const createInitialForm = (user) => ({
  contactName: user?.fullName || '',
  contactPhone: user?.phoneNumber || '',
  contactEmail: user?.email || '',
  schoolOrCompany: '',
  scoreGoal: user?.studyGoal || '',
  desiredClassCode: '',
  consultationTrack: 'IELTS_4_SKILLS',
  facebookUrl: '',
  notes: '',
  ...readSavedDraft(),
});

const getLocationDisplay = (offering) => {
  if (offering.deliveryMode === 'VIRTUAL') {
    return {
      isVirtual: true,
      label: 'Online (Lớp ảo)',
    };
  }
  const rawRoom = (offering.roomName || '').trim();
  const room = rawRoom ? (/^phòng/i.test(rawRoom) ? rawRoom : `Phòng ${rawRoom}`) : '';
  const addr = (offering.offlineAddress || '').trim();
  const detail = [room, addr].filter(Boolean).join(' - ');
  return {
    isVirtual: false,
    label: detail ? `EnglishLab - ${detail}` : 'EnglishLab (Hà Nội)',
  };
};

const getClassCode = (offering) => (
  offering.trainingProgramCode || offering.curriculumProgramCode || offering.title || `Lớp #${offering.id}`
);

const getScheduleText = (offering) => (
  offering.scheduleSummary || (
    offering.scheduleDaysOfWeek?.length && offering.typicalStartTime && offering.typicalEndTime
      ? `${offering.scheduleDaysOfWeek.map((day) => (day === 7 ? 'CN' : `T${day + 1}`)).join(', ')} (${String(offering.typicalStartTime).slice(0, 5)} - ${String(offering.typicalEndTime).slice(0, 5)})`
      : 'Cập nhật khi xếp lớp'
  )
);

export default function ClassroomsCatalogPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const currentUser = getStoredUser();
  const isAuthenticated = Boolean(hasAccessToken() && currentUser);

  const [offerings, setOfferings] = useState([]);
  const [existingRequest, setExistingRequest] = useState(null);
  const [form, setForm] = useState(() => createInitialForm(currentUser));
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [formError, setFormError] = useState('');
  const [success, setSuccess] = useState('');
  const [activeTab, setActiveTab] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const page = await classroomApi.getClassroomOfferings({ page: 0, size: 200 });
      setOfferings((page.content || []).filter(isUpcomingOffering));
      if (isAuthenticated) {
        try {
          const mine = await enrollmentRequestApi.listMine();
          setExistingRequest(mine.find((item) => ACTIVE_REQUEST_STATUSES.has(item.status)) || null);
        } catch {
          setExistingRequest(null);
        }
      }
    } catch (loadError) {
      setOfferings([]);
      setError(loadError?.response?.data?.message || 'Không thể tải danh sách lịch khai giảng.');
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (location.hash !== '#dang-ky-tu-van') return;
    const frame = window.requestAnimationFrame(() => {
      document.getElementById('dang-ky-tu-van')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
    return () => window.cancelAnimationFrame(frame);
  }, [location.hash]);

  const filteredOfferings = useMemo(() => {
    return offerings.filter((item) => {
      if (activeTab === 'OFFLINE' && item.deliveryMode !== 'OFFLINE') return false;
      if (activeTab === 'VIRTUAL' && item.deliveryMode !== 'VIRTUAL') return false;

      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const code = getClassCode(item).toLowerCase();
        const title = (item.title || '').toLowerCase();
        const teacher = (item.primaryTeacherName || '').toLowerCase();
        return code.includes(q) || title.includes(q) || teacher.includes(q);
      }
      return true;
    }).sort((a, b) => String(a.startDate).localeCompare(String(b.startDate)));
  }, [offerings, activeTab, searchQuery]);

  const updateField = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
    setFormError('');
  };

  const handleSelectClass = (classCode) => {
    updateField('desiredClassCode', classCode);
    document.getElementById('dang-ky-tu-van')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const validateForm = () => {
    if (!form.contactName.trim() || !form.contactPhone.trim() || !form.contactEmail.trim()) {
      return 'Vui lòng điền Họ tên, Số điện thoại và Email.';
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.contactEmail.trim())) {
      return 'Email chưa đúng định dạng.';
    }
    if (form.contactPhone.replace(/\D/g, '').length < 9) {
      return 'Số điện thoại không hợp lệ.';
    }
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

    const studyWorkGoalCombined = [
      form.schoolOrCompany.trim() ? `Trường/Nơi làm: ${form.schoolOrCompany.trim()}` : '',
      form.scoreGoal.trim() ? `Mục tiêu điểm: ${form.scoreGoal.trim()}` : '',
      form.notes.trim() ? `Ghi chú: ${form.notes.trim()}` : '',
    ].filter(Boolean).join(' | ');

    try {
      const saved = await enrollmentRequestApi.submit({
        contactName: form.contactName.trim(),
        contactPhone: form.contactPhone.trim(),
        contactEmail: form.contactEmail.trim(),
        facebookUrl: form.facebookUrl.trim() || null,
        desiredClassCode: form.desiredClassCode.trim() || null,
        consultationTrack: form.consultationTrack || 'IELTS_4_SKILLS',
        studyWorkGoal: studyWorkGoalCombined || null,
      });
      sessionStorage.removeItem(DRAFT_STORAGE_KEY);
      setExistingRequest(saved);
      setSuccess('Đã gửi thông tin đăng ký tư vấn. Đội ngũ tư vấn sẽ liên hệ lại với bạn sớm nhất!');
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

          {/* ── HEADER HERO ── */}
          <section className="rounded-3xl border border-[#dfbfbd]/50 bg-[#4b0009] px-6 py-8 text-white shadow-md md:px-10 md:py-10">
            <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="text-xs font-bold uppercase tracking-widest text-[#ffcdd2]">TRUNG TÂM ENGLISHLAB</p>
                <h1 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-white md:text-3xl">
                  Lịch Khai Giảng Các Khóa Học
                </h1>
                <p className="mt-2 max-w-2xl text-xs leading-6 text-white/80 md:text-sm">
                  Cập nhật các lớp luyện thi IELTS và TOEIC mới nhất. Đăng ký ngay để nhận tư vấn lộ trình và làm bài kiểm tra trình độ miễn phí.
                </p>
              </div>
              <div className="flex shrink-0 flex-col gap-2 rounded-2xl border border-white/15 bg-white/10 px-5 py-4 text-xs text-white backdrop-blur-sm">
                <div className="flex items-center gap-2">
                  <Phone className="h-4 w-4 text-[#ffcdd2]" />
                  <span className="font-bold">Hotline: 0988.123.456</span>
                </div>
                <div className="flex items-center gap-2">
                  <MapPin className="h-4 w-4 text-[#ffcdd2]" />
                  <span>Cơ sở: EnglishLab Campus (Hà Nội)</span>
                </div>
              </div>
            </div>
          </section>

          {/* ── SCHEDULE SECTION ── */}
          <section className="space-y-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex flex-wrap items-center gap-2">
                {[
                  { id: 'ALL', label: 'Tất cả các lớp' },
                  { id: 'OFFLINE', label: 'Lớp tại Trung tâm' },
                  { id: 'VIRTUAL', label: 'Lớp Trực tuyến (Online)' },
                ].map((tab) => (
                  <button
                    className={`rounded-xl px-4 py-2 text-xs font-bold transition ${
                      activeTab === tab.id
                        ? 'bg-[#4b0009] text-white shadow-sm'
                        : 'border border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                    }`}
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id)}
                    type="button"
                  >
                    {tab.label}
                  </button>
                ))}
              </div>

              <div className="relative w-full sm:w-64">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                <input
                  className="h-9 w-full rounded-xl border border-slate-200 bg-white pl-9 pr-3 text-xs outline-none focus:border-[#730014]"
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Tìm lớp, giảng viên..."
                  type="text"
                  value={searchQuery}
                />
              </div>
            </div>

            {loading ? <ScheduleLoading /> : null}
            {!loading && error ? <ScheduleError message={error} onRetry={load} /> : null}
            {!loading && !error && !filteredOfferings.length ? (
              <ScheduleEmpty activeTab={activeTab} searchQuery={searchQuery} />
            ) : null}

            {!loading && !error && filteredOfferings.length ? (
              <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[760px] border-collapse text-left text-xs">
                    <thead>
                      <tr className="border-b border-[#dfbfbd]/40 bg-[#4b0009] text-white">
                        <th className="px-4 py-3.5 font-bold uppercase tracking-wider">Mã Lớp / Khóa Học</th>
                        <th className="px-4 py-3.5 font-bold uppercase tracking-wider">Lịch Học</th>
                        <th className="px-4 py-3.5 text-center font-bold uppercase tracking-wider">Ngày Khai Giảng</th>
                        <th className="px-4 py-3.5 font-bold uppercase tracking-wider">Giảng Viên</th>
                        <th className="px-4 py-3.5 font-bold uppercase tracking-wider">Địa Điểm / Hình Thức</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {filteredOfferings.map((offering, idx) => {
                        const loc = getLocationDisplay(offering);
                        const code = getClassCode(offering);

                        return (
                          <tr className={`transition hover:bg-[#fff9fa] ${idx % 2 === 1 ? 'bg-slate-50/50' : 'bg-white'}`} key={offering.id}>
                            <td className="px-4 py-3.5">
                              <span className="font-extrabold text-[#730014]">{code}</span>
                              <p className="mt-0.5 font-semibold text-slate-800">{offering.title}</p>
                            </td>
                            <td className="px-4 py-3.5 font-medium text-slate-700">
                              <div className="flex items-center gap-1.5">
                                <Clock className="h-3.5 w-3.5 text-slate-400" />
                                {getScheduleText(offering)}
                              </div>
                            </td>
                            <td className="px-4 py-3.5 text-center">
                              <span className="inline-flex items-center gap-1 rounded-md bg-[#fff0f2] px-2.5 py-1 font-extrabold text-[#730014]">
                                <Calendar className="h-3 w-3" />
                                {formatClassroomDate(offering.startDate)}
                              </span>
                            </td>
                            <td className="px-4 py-3.5 text-slate-700 font-medium">
                              {offering.primaryTeacherName || 'Đang cập nhật'}
                            </td>
                            <td className="px-4 py-3.5">
                              {loc.isVirtual ? (
                                <span className="inline-flex items-center gap-1 font-semibold text-blue-600">
                                  <Video className="h-3.5 w-3.5" />
                                  Online (Lớp ảo)
                                </span>
                              ) : (
                                <span className="font-medium text-slate-700">{loc.label}</span>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            ) : null}
          </section>

          {/* ── CONSULTATION FORM ── */}
          <section className="scroll-mt-24" id="dang-ky-tu-van">
            <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
              <div className="mb-6 border-b border-slate-100 pb-4">
                <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-[#730014]">
                  <MessageSquare className="h-4 w-4" />
                  Đăng ký tư vấn lịch học
                </div>
                <h2 className="mt-1 font-['Manrope'] text-xl font-extrabold text-slate-900 md:text-2xl">
                  Form Nhận Tư Vấn & Đăng Ký Xếp Lớp
                </h2>
                <p className="mt-1 text-xs text-slate-500">
                  Điền thông tin của bạn bên dưới. Tư vấn viên EnglishLab sẽ liên hệ lại trong thời gian sớm nhất.
                </p>
              </div>

              {existingRequest ? (
                <div className="flex flex-col items-center justify-center rounded-2xl bg-slate-50 p-8 text-center">
                  <CheckCircle2 className="h-10 w-10 text-emerald-600" />
                  <h3 className="mt-3 font-['Manrope'] text-lg font-bold text-slate-900">
                    Đã gửi thông tin đăng ký!
                  </h3>
                  <p className="mt-1 max-w-md text-xs text-slate-600">
                    {success || existingRequest.statusLabel || 'Chúng tôi đã nhận thông tin và sẽ sớm liên hệ theo số điện thoại của bạn.'}
                  </p>
                  <button
                    className="mt-4 rounded-xl bg-[#4b0009] px-4 py-2 text-xs font-bold text-white"
                    onClick={() => navigate('/my-enrollment-requests')}
                    type="button"
                  >
                    Xem yêu cầu đã gửi
                  </button>
                </div>
              ) : (
                <form className="grid gap-4 md:grid-cols-2" onSubmit={submit}>
                  <FormField
                    label="Họ và tên *"
                    maxLength={100}
                    onChange={(v) => updateField('contactName', v)}
                    placeholder="Nguyễn Văn A"
                    required
                    value={form.contactName}
                  />

                  <FormField
                    label="Số điện thoại *"
                    maxLength={30}
                    onChange={(v) => updateField('contactPhone', v)}
                    placeholder="0912 345 678"
                    required
                    type="tel"
                    value={form.contactPhone}
                  />

                  <FormField
                    label="Email liên hệ *"
                    maxLength={150}
                    onChange={(v) => updateField('contactEmail', v)}
                    placeholder="nguyenvana@gmail.com"
                    required
                    type="email"
                    value={form.contactEmail}
                  />

                  <div>
                    <label className="block">
                      <span className="mb-1 block text-xs font-bold text-slate-800">Khóa học quan tâm *</span>
                      <BrandedSelect
                        buttonClassName="h-10 rounded-xl border-slate-200 bg-white text-xs shadow-none"
                        onChange={(e) => updateField('consultationTrack', e.target.value)}
                        options={CONSULTATION_OPTIONS}
                        value={form.consultationTrack}
                      />
                    </label>
                  </div>

                  <FormField
                    label="Trường học / Nơi làm việc"
                    maxLength={200}
                    onChange={(v) => updateField('schoolOrCompany', v)}
                    placeholder="ĐH FPT, Người đi làm..."
                    value={form.schoolOrCompany}
                  />

                  <FormField
                    label="Mục tiêu điểm số"
                    maxLength={200}
                    onChange={(v) => updateField('scoreGoal', v)}
                    placeholder="IELTS 6.5, TOEIC 750..."
                    value={form.scoreGoal}
                  />

                  <FormField
                    helper="Có thể để trống nếu bạn muốn tư vấn xếp lớp"
                    label="Mã lớp ưu tiên (nếu có)"
                    maxLength={120}
                    onChange={(v) => updateField('desiredClassCode', v)}
                    placeholder="Mã lớp chọn từ bảng trên"
                    value={form.desiredClassCode}
                  />

                  <FormField
                    helper="Link Facebook hoặc Zalo để tư vấn viên liên hệ"
                    label="Link Facebook / Zalo (tùy chọn)"
                    maxLength={500}
                    onChange={(v) => updateField('facebookUrl', v)}
                    placeholder="https://facebook.com/..."
                    value={form.facebookUrl}
                  />

                  <div className="md:col-span-2">
                    <label className="block">
                      <span className="mb-1 block text-xs font-bold text-slate-800">Ghi chú thêm (khung giờ rảnh, yêu cầu khác)</span>
                      <textarea
                        className="w-full rounded-xl border border-slate-200 bg-white p-3 text-xs outline-none focus:border-[#730014]"
                        onChange={(e) => updateField('notes', e.target.value)}
                        placeholder="Có thể học vào các tối Thứ 2 - 4 - 6..."
                        rows={2}
                        value={form.notes}
                      />
                    </label>
                  </div>

                  {formError ? <div className="md:col-span-2"><FormNotice message={formError} /></div> : null}

                  <div className="md:col-span-2 pt-2 flex flex-col items-center justify-center text-center">
                    <button
                      className="inline-flex items-center justify-center rounded-xl bg-[#4b0009] px-7 py-3 text-xs font-bold text-white transition hover:bg-[#730014] disabled:opacity-60"
                      disabled={submitting}
                      type="submit"
                    >
                      {submitting ? 'Đang gửi...' : isAuthenticated ? 'Gửi đăng ký tư vấn' : 'Đăng nhập để đăng ký'}
                    </button>
                    <span className="mt-2 text-[11px] text-slate-500">
                      Thông tin của bạn sẽ được bảo mật tuyệt đối.
                    </span>
                  </div>
                </form>
              )}
            </div>
          </section>

        </main>
        <CourseFooter />
      </div>
    </div>
  );
}

function FormField({ helper, label, maxLength, onChange, placeholder, required = false, type = 'text', value }) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs font-bold text-slate-800">
        {label}
      </span>
      <input
        className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-xs outline-none transition focus:border-[#730014]"
        maxLength={maxLength}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        required={required}
        type={type}
        value={value}
      />
      {helper ? <span className="mt-1 block text-[11px] text-slate-400">{helper}</span> : null}
    </label>
  );
}

function FormNotice({ message }) {
  return (
    <div className="flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-4 py-2.5 text-xs font-semibold text-rose-700">
      <AlertCircle className="h-4 w-4 shrink-0" />
      {message}
    </div>
  );
}

function ScheduleLoading() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 3 }).map((_, index) => (
        <div className="h-16 animate-pulse rounded-xl bg-slate-100" key={index} />
      ))}
    </div>
  );
}

function ScheduleError({ message, onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-rose-200 bg-white p-6 text-center">
      <AlertCircle className="h-8 w-8 text-rose-400" />
      <p className="mt-2 text-xs text-slate-600">{message}</p>
      <button
        className="mt-3 inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-4 py-2 text-xs font-bold text-white"
        onClick={onRetry}
        type="button"
      >
        <RefreshCw className="h-3.5 w-3.5" />
        Thử lại
      </button>
    </div>
  );
}

function ScheduleEmpty({ activeTab, searchQuery }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white p-8 text-center">
      <CalendarDays className="h-8 w-8 text-slate-300" />
      <p className="mt-2 text-xs font-bold text-slate-600">
        {searchQuery ? 'Không tìm thấy lớp phù hợp với từ khóa' : 'Chưa có lớp khai giảng mới trong danh mục này'}
      </p>
      <p className="mt-1 text-[11px] text-slate-400">
        Bạn có thể gửi thông tin vào form bên dưới để nhận thông báo khi có lớp mới.
      </p>
    </div>
  );
}
