import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ChevronLeft, Save, X } from 'lucide-react';
import curriculumApi from '../../api/curriculumApi';
import { ContentManagerLoadingState, HeaderActions, Panel } from '../../components/content-manager/ContentManagerUi';
import {
  EnglishEntryLevelField,
  IeltsBandSelect,
  ToeicScoreField,
} from '../../components/content-manager/EnglishScoreFields';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import BrandedSelect from '../../components/ui/BrandedSelect';
import {
  ERROR_NOTICE_CLASS,
  FIELD_CLASS,
  PRIMARY_BUTTON_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
} from '../../utils/formStyles';
import {
  ENGLISH_EXAM_OPTIONS,
  ENGLISH_SKILL_OPTIONS,
  ENGLISH_TRACK_OPTIONS,
  getEnglishProfileDefaults,
  normalizeEnglishEntryLevel,
  normalizeEnglishExamCategory,
  readEnglishFocusSkills,
  validateEnglishProgramProfile,
} from '../../utils/englishProgramProfile';

const platformOptions = [
  { label: 'Google Meet', value: 'GOOGLE_MEET' },
  { label: 'Zoom', value: 'ZOOM' },
  { label: 'Google Meet', value: 'GOOGLE_MEET' },
  { label: 'Liên kết thủ công', value: 'MANUAL' },
];

const toForm = (program = {}) => {
  const examCategory = normalizeEnglishExamCategory(program.examCategory);
  const defaults = getEnglishProfileDefaults(examCategory);
  return {
    title: program.title || '',
    code: program.code || '',
    slug: program.slug || '',
    examCategory,
    programTrack: program.programTrack || defaults.programTrack,
    focusSkills: readEnglishFocusSkills(program.focusSkills, examCategory),
    targetBand: examCategory === 'IELTS' ? (program.targetBand ?? defaults.targetBand) : '',
    targetScore: examCategory === 'TOEIC' ? (program.targetScore ?? defaults.targetScore) : '',
    entryLevel: normalizeEnglishEntryLevel(program.entryLevel, examCategory),
    outcomes: program.outcomes || '',
    teacherGuide: program.teacherGuide || '',
    interactionActivities: program.interactionActivities || '',
    totalSessions: program.totalSessions ?? 0,
    status: program.status || 'DRAFT',
    displayOrder: program.displayOrder ?? 0,
    virtualPlatform: program.virtualPlatform || 'GOOGLE_MEET',
    recordingAllowed: program.recordingAllowed ?? true,
    recordingAvailableDays: program.recordingAvailableDays ?? 30,
    materialsDownloadable: program.materialsDownloadable ?? false,
    sessionOpenBeforeMinutes: program.sessionOpenBeforeMinutes ?? 15,
    sessionCloseAfterMinutes: program.sessionCloseAfterMinutes ?? 30,
    deviceCheckRequired: program.deviceCheckRequired ?? true,
    micRequired: program.micRequired ?? true,
    speakerRequired: program.speakerRequired ?? true,
    cameraRequired: program.cameraRequired ?? false,
    autoAttendanceEnabled: program.autoAttendanceEnabled ?? true,
    minAttendanceMinutes: program.minAttendanceMinutes ?? 45,
  };
};

export default function ContentManagerCurriculumProgramEditPage({ mode = 'OFFLINE' }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const isVirtual = mode === 'VIRTUAL';
  const detailPath = isVirtual ? `/content-manager/virtual-programs/${id}` : `/content-manager/offline-programs/${id}`;
  const [form, setForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadProgram = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setForm(toForm(await curriculumApi.getCurriculumProgram(id)));
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được giáo trình.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadProgram();
  }, [loadProgram]);

  const updateForm = (patch) => setForm((current) => ({ ...current, ...patch }));

  const saveProgram = async () => {
    if (!form.title.trim()) {
      setError('Vui lòng nhập tên giáo trình.');
      return;
    }
    const profileError = validateEnglishProgramProfile(form);
    if (profileError) {
      setError(profileError);
      return;
    }
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const payload = {
        ...form,
        deliveryMode: mode,
        focusSkills: form.focusSkills.join(','),
        targetBand: form.targetBand === '' ? null : Number(form.targetBand),
        targetScore: form.targetScore === '' ? null : Number(form.targetScore),
        totalSessions: Number(form.totalSessions || 0),
        displayOrder: Number(form.displayOrder || 0),
        recordingAllowed: Boolean(form.recordingAllowed),
        recordingAvailableDays: Number(form.recordingAvailableDays || 0),
        materialsDownloadable: Boolean(form.materialsDownloadable),
        sessionOpenBeforeMinutes: Number(form.sessionOpenBeforeMinutes || 0),
        sessionCloseAfterMinutes: Number(form.sessionCloseAfterMinutes || 0),
        deviceCheckRequired: Boolean(form.deviceCheckRequired),
        micRequired: Boolean(form.micRequired),
        speakerRequired: Boolean(form.speakerRequired),
        cameraRequired: Boolean(form.cameraRequired),
        autoAttendanceEnabled: Boolean(form.autoAttendanceEnabled),
        minAttendanceMinutes: Number(form.minAttendanceMinutes || 0),
        virtualPlatform: isVirtual ? form.virtualPlatform : undefined,
      };
      await curriculumApi.updateCurriculumProgram(id, payload);
      setSuccess('Đã lưu metadata giáo trình.');
      navigate(detailPath);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được giáo trình.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <ContentManagerLoadingState message="Đang tải form chỉnh sửa..." />;
  if (!form) return <div className={ERROR_NOTICE_CLASS}>{error || 'Không tìm thấy giáo trình.'}</div>;

  return (
    <div className="space-y-5">
      <HeaderActions>
        <Link className={SECONDARY_BUTTON_CLASS} to={detailPath}>
          <X className="h-4 w-4" />
          Hủy
        </Link>
        <button className={PRIMARY_BUTTON_CLASS} disabled={saving} onClick={saveProgram} type="button">
          <Save className="h-4 w-4" />
          {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
        </button>
      </HeaderActions>

      <div className="flex items-center justify-between gap-3">
        <Link className="inline-flex items-center gap-2 text-sm font-bold text-[#564241] hover:text-[#4b0009]" to={detailPath}>
          <ChevronLeft className="h-4 w-4" />
          Quay lại chi tiết
        </Link>
      </div>

      {error ? <div className={ERROR_NOTICE_CLASS}>{error}</div> : null}
      {success ? <div className={SUCCESS_NOTICE_CLASS}>{success}</div> : null}

      <Panel className="rounded-xl border-[#dcc0bf]/30 p-6 shadow-sm">
        <div className="grid gap-5 xl:grid-cols-2">
          <TextInput label="Tên giáo trình" value={form.title} onChange={(value) => updateForm({ title: value })} />
          <div>
            <FieldLabel>Nhóm thi</FieldLabel>
            <BrandedSelect
              value={form.examCategory}
              onChange={(event) => {
                const examCategory = event.target.value;
                updateForm({ examCategory, ...getEnglishProfileDefaults(examCategory) });
              }}
              options={ENGLISH_EXAM_OPTIONS}
            />
          </div>
          <div>
            <FieldLabel>Loại chương trình</FieldLabel>
            <BrandedSelect value={form.programTrack} onChange={(event) => updateForm({ programTrack: event.target.value })} options={ENGLISH_TRACK_OPTIONS[form.examCategory]} />
          </div>
          <EnglishEntryLevelField
            examCategory={form.examCategory}
            onChange={(value) => updateForm({ entryLevel: value })}
            value={form.entryLevel}
          />
          <TextInput label="Số buổi" type="number" value={form.totalSessions} onChange={(value) => updateForm({ totalSessions: value })} />
          {form.examCategory === 'IELTS' ? (
            <IeltsBandSelect
              label="Band IELTS mục tiêu"
              onChange={(value) => updateForm({ targetBand: value })}
              value={form.targetBand}
            />
          ) : null}
          {form.examCategory === 'TOEIC' ? (
            <ToeicScoreField
              label="Điểm TOEIC mục tiêu"
              onChange={(value) => updateForm({ targetScore: value })}
              value={form.targetScore}
            />
          ) : null}
          <div>
            <FieldLabel>Trạng thái</FieldLabel>
            <div className="rounded-lg border border-[#dcc0bf]/40 bg-[#fcfbfb] px-4 py-3 text-sm font-semibold text-[#584140]">
              {formatProgramStatus(form.status)}
            </div>
          </div>
          <TextInput label="Thứ tự hiển thị" type="number" value={form.displayOrder} onChange={(value) => updateForm({ displayOrder: value })} />
        </div>
        <div className="mt-5">
          <FieldLabel>Kỹ năng trọng tâm</FieldLabel>
          <div className="flex flex-wrap gap-2">
            {ENGLISH_SKILL_OPTIONS.map((skill) => {
              const selected = form.focusSkills.includes(skill.value);
              return (
                <button
                  className={`rounded-full border px-3 py-2 text-xs font-bold transition ${selected ? 'border-[#730014] bg-[#730014] text-white' : 'border-[#dcc0bf] bg-white text-[#584140] hover:border-[#730014]'}`}
                  key={skill.value}
                  onClick={() => updateForm({
                    focusSkills: selected
                      ? form.focusSkills.filter((value) => value !== skill.value)
                      : [...form.focusSkills, skill.value],
                  })}
                  type="button"
                >
                  {skill.label}
                </button>
              );
            })}
          </div>
        </div>
        <div className="mt-5 space-y-4">
          <RichTextEditor label="Chuẩn đầu ra" onChange={(value) => updateForm({ outcomes: value })} placeholder="Chuẩn đầu ra của chương trình..." size="form" value={form.outcomes} />
          <RichTextEditor label="Hướng dẫn giảng viên" onChange={(value) => updateForm({ teacherGuide: value })} placeholder="Gợi ý giảng dạy, lưu ý buổi học..." size="form" value={form.teacherGuide} />
          <RichTextEditor label="Hoạt động tương tác" onChange={(value) => updateForm({ interactionActivities: value })} placeholder="Hoạt động tương tác trong lớp..." size="form" value={form.interactionActivities} />
        </div>

        {isVirtual ? (
          <div className="mt-6 rounded-xl border border-[#dcc0bf]/30 bg-[#fcfbfb] p-4">
            <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Cấu hình virtual</h3>
            <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              <div>
                <FieldLabel>Nền tảng</FieldLabel>
                <BrandedSelect value={form.virtualPlatform} onChange={(event) => updateForm({ virtualPlatform: event.target.value })} options={platformOptions} />
              </div>
              <TextInput label="Ghi hình khả dụng (ngày)" type="number" value={form.recordingAvailableDays} onChange={(value) => updateForm({ recordingAvailableDays: value })} />
              <TextInput label="Mở phòng trước (phút)" type="number" value={form.sessionOpenBeforeMinutes} onChange={(value) => updateForm({ sessionOpenBeforeMinutes: value })} />
              <TextInput label="Đóng phòng sau (phút)" type="number" value={form.sessionCloseAfterMinutes} onChange={(value) => updateForm({ sessionCloseAfterMinutes: value })} />
              <TextInput label="Điểm danh tối thiểu (phút)" type="number" value={form.minAttendanceMinutes} onChange={(value) => updateForm({ minAttendanceMinutes: value })} />
            </div>
            <div className="mt-4 grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
              {[
                ['recordingAllowed', 'Cho phép ghi hình'],
                ['materialsDownloadable', 'Cho phép tải tài liệu'],
                ['deviceCheckRequired', 'Kiểm tra thiết bị'],
                ['micRequired', 'Bắt buộc micro'],
                ['speakerRequired', 'Bắt buộc loa/tai nghe'],
                ['cameraRequired', 'Bắt buộc camera'],
                ['autoAttendanceEnabled', 'Tự động điểm danh'],
              ].map(([key, label]) => (
                <label className="flex items-center gap-2 text-sm font-semibold text-[#584140]" key={key}>
                  <input checked={Boolean(form[key])} onChange={(event) => updateForm({ [key]: event.target.checked })} type="checkbox" />
                  {label}
                </label>
              ))}
            </div>
          </div>
        ) : null}
      </Panel>
    </div>
  );
}

function formatProgramStatus(status) {
  const labels = {
    DRAFT: 'Bản nháp — dùng nút “Xuất bản giáo trình” ở trang chi tiết',
    PENDING_REVIEW: 'Sẵn sàng xuất bản — trạng thái từ luồng cũ',
    PUBLISHED: 'Đã xuất bản',
    REJECTED: 'Cần chỉnh sửa — hoàn thiện rồi xuất bản lại',
    ARCHIVED: 'Đã lưu trữ',
  };
  return labels[String(status || '').toUpperCase()] || status || 'Bản nháp';
}

function FieldLabel({ children }) {
  return <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{children}</span>;
}

function TextInput({ label, value, onChange, type = 'text' }) {
  return (
    <label className="block">
      <FieldLabel>{label}</FieldLabel>
      <input className={FIELD_CLASS} type={type} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}
