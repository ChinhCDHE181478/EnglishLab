import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, ChevronDown } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { Panel, TextField } from '../../components/content-manager/ContentManagerUi';

const emptyForm = {
  title: '',
  shortDescription: '',
  description: '',
  category: 'IELTS',
  level: 'ADVANCED',
  status: 'DRAFT',
  targetScore: '',
  recommendedCurrentBandMin: '',
  recommendedCurrentBandMax: '',
  targetBand: '',
  targetOutcome: '',
  recommendedNextCourseSlug: '',
  duration: '',
  studyMode: 'Online',
  price: '0',
  salePrice: '',
  thumbnailUrl: '',
  totalLessons: '0',
  totalHours: '0',
  displayOrder: '0',
  featured: false,
  modules: [],
};

const mapCourseToForm = (course = {}) => ({
  title: course.title ?? '',
  shortDescription: course.shortDescription ?? '',
  description: course.description ?? '',
  category: course.category ?? 'IELTS',
  level: course.level ?? 'ADVANCED',
  status: course.status ?? 'DRAFT',
  targetScore: course.targetScore ?? '',
  recommendedCurrentBandMin: course.recommendedCurrentBandMin ?? '',
  recommendedCurrentBandMax: course.recommendedCurrentBandMax ?? '',
  targetBand: course.targetBand ?? '',
  targetOutcome: course.targetOutcome ?? '',
  recommendedNextCourseSlug: course.recommendedNextCourseSlug ?? '',
  duration: course.duration ?? '',
  studyMode: course.studyMode ?? 'Online',
  price: course.price ?? '0',
  salePrice: course.salePrice && Number(course.salePrice) < Number(course.price || 0) ? String(course.salePrice) : '',
  thumbnailUrl: course.thumbnailUrl ?? '',
  totalLessons: String(course.totalLessons ?? 0),
  totalHours: String(course.totalHours ?? 0),
  displayOrder: String(course.displayOrder ?? 0),
  featured: Boolean(course.featured),
  modules: course.modules ?? [],
});

export default function ContentManagerCourseEditorPage() {
  const { slugOrId } = useParams();
  const navigate = useNavigate();
  const editMode = Boolean(slugOrId);
  const [courseId, setCourseId] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(editMode);
  const [saving, setSaving] = useState(false);
  const [savingAction, setSavingAction] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (!editMode) return undefined;

    let active = true;

    courseApi.getManagedOnlineCourse(slugOrId)
      .then((course) => {
        if (!active) return;
        setCourseId(course.id);
        setForm(mapCourseToForm(course));
      })
      .catch(() => {
        if (active) setError('Không tải được chi tiết khóa học.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [editMode, slugOrId]);

  const hasNoStructure = useMemo(() => !form.modules?.length && Number(form.totalLessons || 0) === 0, [form.modules, form.totalLessons]);

  const handleChange = (field) => (event) => {
    const value = field === 'featured' ? event.target.checked : event.target.value;
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit = async (nextStatus = null) => {
    setSaving(true);
    const targetStatus = nextStatus ?? form.status;
    setSavingAction(targetStatus === 'PUBLISHED' && nextStatus === 'PUBLISHED' ? 'publish' : 'save');
    setError('');
    setSuccess('');

    const payload = {
      ...form,
      price: Number(form.price || 0),
      salePrice: form.salePrice === '' ? null : Number(form.salePrice || 0),
      recommendedCurrentBandMin: form.recommendedCurrentBandMin === '' ? null : Number(form.recommendedCurrentBandMin),
      recommendedCurrentBandMax: form.recommendedCurrentBandMax === '' ? null : Number(form.recommendedCurrentBandMax),
      targetBand: form.targetBand === '' ? null : Number(form.targetBand),
      totalLessons: Number(form.totalLessons || 0),
      totalHours: Number(form.totalHours || 0),
      displayOrder: Number(form.displayOrder || 0),
      status: targetStatus,
    };

    try {
      if (editMode && !courseId) {
        throw new Error('Missing course id');
      }

      const response = editMode
        ? await courseApi.updateOnlineCourse(courseId, payload)
        : await courseApi.createOnlineCourse(payload);

      setCourseId(response.id);
      setForm(mapCourseToForm(response));
      setSuccess(targetStatus === 'PUBLISHED' ? 'Khóa học đã được lưu và chuyển sang Published.' : 'Khóa học đã được lưu thành công.');

      if (!editMode) {
        navigate(`/content-manager/courses/${response.slug}/edit`, { replace: true });
      }
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được khóa học.');
    } finally {
      setSaving(false);
      setSavingAction('');
    }
  };

  if (loading) {
    return <div className="rounded-2xl border border-[#dfbfbd]/55 bg-white px-5 py-8 text-sm text-[#584140]">Đang tải khóa học...</div>;
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <Link className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/65 bg-white px-4 py-3 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]" to="/content-manager/courses">
          <ArrowLeft className="h-4 w-4" />
          Back to courses
        </Link>
        {editMode && form.title ? (
          <Link className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]" to={`/content-manager/courses/${slugOrId}/builder`}>
            Open Builder
          </Link>
        ) : null}
      </div>
      <div className="grid gap-6 xl:grid-cols-[1.15fr_0.55fr]">
      <div className="space-y-6">
        {error ? <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">{error}</div> : null}
        {success ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm font-semibold text-emerald-700">{success}</div> : null}

        <Panel className="p-6">
          <div className="grid gap-4 md:grid-cols-2">
            <TextField label="Course title" onChange={handleChange('title')} value={form.title} />
            <SelectField label="Category" onChange={handleChange('category')} options={['IELTS', 'TOEIC', 'COMMUNICATION', 'FOUNDATION', 'ONLINE']} value={form.category} />
            <TextField label="Short description" onChange={handleChange('shortDescription')} value={form.shortDescription} />
            <SelectField label="Level" onChange={handleChange('level')} options={['BEGINNER', 'INTERMEDIATE', 'ADVANCED']} value={form.level} />
            <TextField label="Target label / outcome" onChange={handleChange('targetScore')} value={form.targetScore} />
            <TextField label="Recommended current band min" onChange={handleChange('recommendedCurrentBandMin')} value={String(form.recommendedCurrentBandMin)} />
            <TextField label="Recommended current band max" onChange={handleChange('recommendedCurrentBandMax')} value={String(form.recommendedCurrentBandMax)} />
            <TextField label="Target band number" onChange={handleChange('targetBand')} value={String(form.targetBand)} />
            <TextField label="Recommended next course slug" onChange={handleChange('recommendedNextCourseSlug')} value={form.recommendedNextCourseSlug} />
            <TextField label="Estimated duration" onChange={handleChange('duration')} value={form.duration} />
            <TextField label="Study mode" onChange={handleChange('studyMode')} value={form.studyMode} />
            <TextField label="Price" onChange={handleChange('price')} value={String(form.price)} />
            <TextField label="System sale price" onChange={handleChange('salePrice')} value={String(form.salePrice)} />
            <TextField label="Thumbnail URL" onChange={handleChange('thumbnailUrl')} value={form.thumbnailUrl} />
            <SelectField label="Status" onChange={handleChange('status')} options={['DRAFT', 'PUBLISHED', 'ARCHIVED']} value={form.status} />
            <TextField label="Total lessons" onChange={handleChange('totalLessons')} value={String(form.totalLessons)} />
            <TextField label="Total hours" onChange={handleChange('totalHours')} value={String(form.totalHours)} />
          </div>
          <div className="mt-4 grid gap-4">
            <TextField label="Full description" onChange={handleChange('description')} rows={5} textarea value={form.description} />
            <TextField label="Target output / course completion outcome" onChange={handleChange('targetOutcome')} rows={3} textarea value={form.targetOutcome} />
          </div>
        </Panel>
      </div>

      <div className="space-y-6">
        <Panel className="p-6">
          <div className="rounded-2xl border border-dashed border-[#dfbfbd] bg-[#fcfbfb] p-5 text-sm text-[#584140]">
            Publishing note: set entry band, target band and target outcome here. Then use the builder page to manage modules, lessons and AI module checks.
          </div>
          {hasNoStructure ? (
            <div className="mt-4 rounded-2xl border border-[#f0d8db] bg-[#fff7f7] p-4 text-sm text-[#730014]">
              Warning: this course currently has no modules or lessons. Publishing should be confirmed carefully.
            </div>
          ) : null}
          <label className="mt-4 flex items-center gap-3 text-sm text-[#1a1c1c]">
            <input checked={form.featured} onChange={handleChange('featured')} type="checkbox" />
            Mark as featured
          </label>
          <div className="mt-6 grid gap-3">
            <button className="rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white" disabled={saving} onClick={() => handleSubmit()} type="button">
              {saving && savingAction === 'save' ? 'Saving...' : 'Save Changes'}
            </button>
            <button className="rounded-2xl border border-[#4b0009] px-4 py-3 text-sm font-semibold text-[#4b0009]" disabled={saving} onClick={() => handleSubmit('PUBLISHED')} type="button">
              {saving && savingAction === 'publish' ? 'Publishing...' : 'Publish'}
            </button>
          </div>
        </Panel>
      </div>
      </div>
    </div>
  );
}

function SelectField({ label, value, onChange, options }) {
  const [open, setOpen] = useState(false);
  return (
    <label className="relative block">
      <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">{label}</span>
      <button
        className="flex w-full items-center justify-between rounded-2xl border border-[#dfbfbd]/65 bg-[#fcfbfb] px-4 py-3 text-left text-sm font-semibold text-[#1a1c1c] outline-none transition hover:border-[#730014]/40 hover:bg-[#fff7f7]"
        onBlur={() => window.setTimeout(() => setOpen(false), 120)}
        onClick={() => setOpen((current) => !current)}
        type="button"
      >
        {value}
        <ChevronDown className={`h-4 w-4 text-[#730014] transition ${open ? 'rotate-180' : ''}`} />
      </button>
      {open ? (
        <div className="absolute left-0 top-full z-50 mt-2 w-full overflow-hidden rounded-2xl border border-[#dfbfbd]/75 bg-white p-1 shadow-[0_18px_45px_rgba(75,0,9,0.16)]">
          {options.map((option) => (
            <button
              key={option}
              className={`block w-full rounded-xl px-4 py-2.5 text-left text-sm font-semibold transition ${
                option === value ? 'bg-[#4b0009] text-white' : 'text-[#4b0009] hover:bg-[#fff2f3]'
              }`}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => {
                onChange({ target: { value: option } });
                setOpen(false);
              }}
              type="button"
            >
              {option}
            </button>
          ))}
        </div>
      ) : null}
      <select className="sr-only" onChange={onChange} value={value}>
        {options.map((option) => (
          <option key={option} value={option}>{option}</option>
        ))}
      </select>
    </label>
  );
}
