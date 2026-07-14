import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, Brain } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { Panel, TextField } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';

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
  learningPathCode: '',
  learningPathName: '',
  learningPathOrder: '',
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
  learningPathCode: course.learningPathCode ?? '',
  learningPathName: course.learningPathName ?? '',
  learningPathOrder: course.learningPathOrder ?? '',
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

export default function ContentManagerCourseEditorPage({ slugOrId: propSlugOrId, onClose, onSave }) {
  const { slugOrId: paramSlugOrId } = useParams();
  const slugOrId = propSlugOrId !== undefined ? propSlugOrId : paramSlugOrId;
  const navigate = useNavigate();
  const editMode = Boolean(slugOrId);
  const [courseId, setCourseId] = useState(null);
  const [courseSlug, setCourseSlug] = useState('');
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(editMode);
  const [saving, setSaving] = useState(false);
  const [savingAction, setSavingAction] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    let active = true;
    const loadCategories = async () => {
      try {
        const items = await courseApi.getManagedCourseCategories();
        if (active) setCategories(items);
      } catch {
        if (active) setCategories([]);
      }
    };

    loadCategories();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!editMode) return undefined;

    let active = true;

    const loadCourse = async () => {
      try {
        const course = await courseApi.getManagedOnlineCourse(slugOrId);
        if (!active) return;
        setCourseId(course.id);
        setCourseSlug(course.slug || '');
        setForm(mapCourseToForm(course));
      } catch {
        if (active) setError('Không tải được chi tiết khóa học.');
      } finally {
        if (active) setLoading(false);
      }
    };

    loadCourse();

    return () => {
      active = false;
    };
  }, [editMode, slugOrId]);

  const hasNoStructure = useMemo(() => !form.modules?.length && Number(form.totalLessons || 0) === 0, [form.modules, form.totalLessons]);
  const flashcardOverview = useMemo(() => getFlashcardOverview(form.modules), [form.modules]);
  const categoryOptions = useMemo(() => {
    const fallback = ['IELTS', 'TOEIC', 'COMMUNICATION', 'FOUNDATION', 'ONLINE'];
    const available = categories
      .filter((category) => category.active || category.code === form.category)
      .map((category) => ({ label: category.name, value: category.code }));
    return available.length ? available : fallback.map((value) => ({ label: value, value }));
  }, [categories, form.category]);

  const handleChange = (field) => (event) => {
    const value = field === 'featured' ? event.target.checked : event.target.value;
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit = async (nextStatus = null) => {
    const targetStatus = nextStatus ?? form.status;
    const validationMessage = validateCourseForm(form, targetStatus, hasNoStructure);
    if (validationMessage) {
      setError(validationMessage);
      setSuccess('');
      return;
    }

    setSaving(true);
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
      learningPathCode: form.learningPathCode.trim() || null,
      learningPathName: form.learningPathName.trim() || null,
      learningPathOrder: form.learningPathOrder === '' ? null : Number(form.learningPathOrder),
      recommendedNextCourseSlug: form.recommendedNextCourseSlug.trim() || null,
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
      setCourseSlug(response.slug || '');
      setForm(mapCourseToForm(response));
      setSuccess(targetStatus === 'PUBLISHED' ? 'Khóa học đã được lưu và chuyển sang trạng thái đã xuất bản.' : 'Khóa học đã được lưu thành công.');

      if (!editMode && !onClose) {
        navigate(`/content-manager/courses/${response.slug}/edit`, { replace: true });
      }
      if (onSave) {
        onSave(response);
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
        {onClose ? (
          <button
            className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/65 bg-white px-4 py-3 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3] active:scale-95"
            onClick={onClose}
            type="button"
          >
            <ArrowLeft className="h-4 w-4" />
            Đóng
          </button>
        ) : (
          <Link className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/65 bg-white px-4 py-3 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]" to="/content-manager/courses">
            <ArrowLeft className="h-4 w-4" />
            Quay lại danh sách khóa học
          </Link>
        )}
        {editMode && form.title ? (
          <Link className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]" to={`/content-manager/courses/${slugOrId}/builder`}>
            Mở khu vực biên soạn
          </Link>
        ) : null}
      </div>
      <div className="space-y-6">
        {error ? <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">{error}</div> : null}
        {success ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm font-semibold text-emerald-700">{success}</div> : null}

        <Panel className="p-6">
          <div className="grid gap-4 md:grid-cols-2">
            <TextField label="Tên khóa học" onChange={handleChange('title')} value={form.title} />
            <SelectField label="Danh mục" onChange={handleChange('category')} options={categoryOptions} value={form.category} />
            <TextField label="Mô tả ngắn" onChange={handleChange('shortDescription')} value={form.shortDescription} />
            <SelectField label="Trình độ" onChange={handleChange('level')} options={['BEGINNER', 'INTERMEDIATE', 'ADVANCED']} value={form.level} />
            <TextField label="Nhãn mục tiêu hiển thị" onChange={handleChange('targetScore')} value={form.targetScore} />
            <TextField label="Band đầu vào tối thiểu" onChange={handleChange('recommendedCurrentBandMin')} value={String(form.recommendedCurrentBandMin)} />
            <TextField label="Band đầu vào tối đa" onChange={handleChange('recommendedCurrentBandMax')} value={String(form.recommendedCurrentBandMax)} />
            <TextField label="Band mục tiêu" onChange={handleChange('targetBand')} value={String(form.targetBand)} />
            <TextField label="Mã lộ trình học" onChange={handleChange('learningPathCode')} value={form.learningPathCode} />
            <TextField label="Tên lộ trình học" onChange={handleChange('learningPathName')} value={form.learningPathName} />
            <TextField label="Thứ tự trong lộ trình" onChange={handleChange('learningPathOrder')} value={String(form.learningPathOrder)} />
            <TextField label="Slug khóa học gợi ý tiếp theo" onChange={handleChange('recommendedNextCourseSlug')} value={form.recommendedNextCourseSlug} />
            <TextField label="Thời lượng ước tính" onChange={handleChange('duration')} value={form.duration} />
            <TextField label="Hình thức học" onChange={handleChange('studyMode')} value={form.studyMode} />
            <TextField label="Giá bán" onChange={handleChange('price')} value={String(form.price)} />
            <TextField label="Giá ưu đãi hệ thống" onChange={handleChange('salePrice')} value={String(form.salePrice)} />
            <TextField label="Liên kết ảnh bìa" onChange={handleChange('thumbnailUrl')} value={form.thumbnailUrl} />
            <SelectField label="Trạng thái" onChange={handleChange('status')} options={['DRAFT', 'PUBLISHED', 'ARCHIVED']} value={form.status} />
            <TextField label="Tổng số bài học" onChange={handleChange('totalLessons')} value={String(form.totalLessons)} />
            <TextField label="Tổng số giờ học" onChange={handleChange('totalHours')} value={String(form.totalHours)} />
          </div>
          <div className="mt-4 grid gap-4">
            <TextField label="Mô tả đầy đủ" onChange={handleChange('description')} rows={5} textarea value={form.description} />
            <TextField label="Đầu ra / kết quả hoàn thành khóa học" onChange={handleChange('targetOutcome')} rows={3} textarea value={form.targetOutcome} />
          </div>
          
          <div className="mt-5 border-t border-[#f4eeee] pt-4">
            <label className="flex items-center gap-3 text-sm text-[#1a1c1c] cursor-pointer">
              <input
                className="h-4.5 w-4.5 rounded border-gray-300 text-[#4b0009] focus:ring-[#730014]"
                checked={form.featured}
                onChange={handleChange('featured')}
                type="checkbox"
              />
              <span className="font-semibold text-slate-700">Đánh dấu là khóa học nổi bật</span>
            </label>
          </div>
        </Panel>

        {editMode && flashcardOverview.setCount > 0 ? (
          <Panel className="p-5 flex items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-[#fff1f2] text-[#730014]"><Brain className="h-5 w-5" /></span>
              <div>
                <h3 className="font-['Manrope'] text-sm font-extrabold text-[#4b0009]">Thẻ ghi nhớ trong khóa học</h3>
                <p className="mt-0.5 text-xs leading-relaxed text-[#584140]">{flashcardOverview.setCount} bộ thẻ · {flashcardOverview.cardCount} thẻ, được gắn từ kho flashcard.</p>
              </div>
            </div>
            {courseSlug ? <Link className="rounded-xl border border-[#dfbfbd] px-4 py-2.5 text-xs font-bold text-[#730014] transition hover:bg-[#fff2f3]" to="/content-manager/flashcards">Mở ngân hàng flashcard</Link> : null}
          </Panel>
        ) : null}

        <Panel className="p-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="text-xs text-[#8b706e] leading-relaxed max-w-xl">
              {hasNoStructure && (
                <p className="text-[#730014] font-semibold">
                  ⚠️ Khóa học này hiện chưa có mô-đun hoặc bài học. Hãy kiểm tra cẩn thận trước khi xuất bản.
                </p>
              )}
              {!hasNoStructure && (
                <p>
                  Hãy thiết lập band đầu vào, band mục tiêu và đầu ra của khóa học. Nhấn Lưu thay đổi hoặc Xuất bản để áp dụng.
                </p>
              )}
            </div>
            
            <div className="flex items-center gap-3">
              <button
                className="rounded-2xl border border-[#4b0009] bg-white px-6 py-3 text-sm font-semibold text-[#4b0009] transition hover:bg-[#fff2f3] active:scale-95"
                disabled={saving}
                onClick={() => handleSubmit()}
                type="button"
              >
                {saving && savingAction === 'save' ? 'Đang lưu...' : 'Lưu thay đổi'}
              </button>
              <button
                className="rounded-2xl bg-[#4b0009] px-6 py-3 text-sm font-semibold text-white transition hover:bg-[#730014] active:scale-95"
                disabled={saving}
                onClick={() => handleSubmit('PUBLISHED')}
                type="button"
              >
                {saving && savingAction === 'publish' ? 'Đang xuất bản...' : 'Xuất bản'}
              </button>
            </div>
          </div>
        </Panel>
      </div>
    </div>
  );
}

function SelectField({ label, value, onChange, options }) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">{label}</span>
      <BrandedSelect onChange={onChange} options={options} value={value} />
    </label>
  );
}

function validateCourseForm(form, targetStatus, hasNoStructure) {
  if (!form.title.trim()) return 'Tên khóa học không được để trống.';
  if (!form.category) return 'Hãy chọn danh mục khóa học.';

  const minBand = form.recommendedCurrentBandMin === '' ? null : Number(form.recommendedCurrentBandMin);
  const maxBand = form.recommendedCurrentBandMax === '' ? null : Number(form.recommendedCurrentBandMax);
  const targetBand = form.targetBand === '' ? null : Number(form.targetBand);
  if ([minBand, maxBand, targetBand].some((value) => value != null && (!Number.isFinite(value) || value < 0 || value > 9))) {
    return 'Band IELTS phải nằm trong khoảng từ 0 đến 9.';
  }
  if (minBand != null && maxBand != null && minBand > maxBand) {
    return 'Band đầu vào tối thiểu không thể lớn hơn band đầu vào tối đa.';
  }

  const pathCode = form.learningPathCode.trim();
  const pathName = form.learningPathName.trim();
  if ((pathCode && !pathName) || (!pathCode && pathName)) {
    return 'Mã và tên lộ trình học phải được nhập cùng nhau.';
  }
  if (pathCode && form.learningPathOrder === '') {
    return 'Hãy nhập thứ tự của khóa học trong lộ trình.';
  }
  if (targetStatus === 'PUBLISHED' && hasNoStructure) {
    return 'Khóa học cần có ít nhất một mô-đun và bài học trước khi xuất bản.';
  }
  return '';
}

function getFlashcardOverview(modules) {
  let setCount = 0;
  let cardCount = 0;
  (modules || []).forEach((module) => (module.lessons || []).forEach((lesson) => {
    if ((lesson.flashcardSets || []).length) {
      setCount += lesson.flashcardSets.length;
      cardCount += lesson.flashcardSets.reduce((sum, set) => sum + countFlashcardCards(set.cardsJson), 0);
      return;
    }
    const content = String(lesson.contentText || '');
    const headings = [...content.matchAll(/^###\s+\d+\.\s+.+$/gm)];
    const cards = headings.filter((heading, index) => {
      const start = (heading.index || 0) + heading[0].length;
      const end = headings[index + 1]?.index ?? content.length;
      return /^\*\*Meaning:\*\*/mi.test(content.slice(start, end));
    });
    if (cards.length) {
      setCount += 1;
      cardCount += cards.length;
    }
  }));
  return { setCount, cardCount };
}

function countFlashcardCards(cardsJson) {
  try {
    const cards = JSON.parse(cardsJson || '[]');
    return Array.isArray(cards) ? cards.length : 0;
  } catch {
    return 0;
  }
}
