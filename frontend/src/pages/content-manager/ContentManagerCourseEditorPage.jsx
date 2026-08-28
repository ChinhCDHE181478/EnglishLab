import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, Brain, Eye, ImagePlus, Upload } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { Panel, TextField } from '../../components/content-manager/ContentManagerUi';
import CourseVersionPanel from '../../components/content-manager/CourseVersionPanel';
import { IeltsBandSelect, ToeicScoreField } from '../../components/content-manager/EnglishScoreFields';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { useAppDialog } from '../../components/ui/AppDialog';
import { findEditableCourseVersion } from '../../utils/courseVersionUi';

const emptyForm = {
  title: '',
  shortDescription: '',
  description: '',
  category: 'IELTS',
  level: 'ADVANCED',
  status: 'DRAFT',
  targetScore: '',
  recommendedCurrentBandMin: '',
  targetBand: '',
  targetOutcome: '',
  duration: '',
  price: '0',
  salePrice: '',
  thumbnailUrl: '',
  featured: false,
  modules: [],
};

const ALLOWED_ENGLISH_CATEGORIES = new Set(['IELTS', 'TOEIC', 'COMMUNICATION', 'FOUNDATION']);

const courseProfileDefaults = (category) => {
  if (category === 'IELTS') {
    return {
      targetScore: '',
      recommendedCurrentBandMin: 4,
      targetBand: 6.5,
    };
  }
  if (category === 'TOEIC') {
    return {
      targetScore: '650',
      recommendedCurrentBandMin: '',
      targetBand: '',
    };
  }
  return {
    targetScore: '',
    recommendedCurrentBandMin: '',
    targetBand: '',
  };
};

const mapCourseToForm = (course = {}) => {
  const category = ALLOWED_ENGLISH_CATEGORIES.has(course.category) ? course.category : 'IELTS';
  const defaults = courseProfileDefaults(category);
  const isIelts = category === 'IELTS';

  return {
    title: course.title ?? '',
    shortDescription: course.shortDescription ?? '',
    description: course.description ?? '',
    category,
    level: course.level ?? 'ADVANCED',
    status: course.status ?? 'DRAFT',
    targetScore: category === 'IELTS' ? '' : (course.targetScore ?? defaults.targetScore),
    recommendedCurrentBandMin: isIelts ? (course.recommendedCurrentBandMin ?? defaults.recommendedCurrentBandMin) : '',
    targetBand: isIelts ? (course.targetBand ?? defaults.targetBand) : '',
    targetOutcome: course.targetOutcome ?? '',
    duration: course.duration ?? '',
    price: course.price ?? '0',
    salePrice: course.salePrice && Number(course.salePrice) < Number(course.price || 0) ? String(course.salePrice) : '',
    thumbnailUrl: course.thumbnailUrl ?? '',
    featured: Boolean(course.featured),
    modules: course.modules ?? [],
  };
};

export default function ContentManagerCourseEditorPage({ slugOrId: propSlugOrId, onClose, onSave }) {
  const { confirm: confirmDialog } = useAppDialog();
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
  const [uploadingThumbnail, setUploadingThumbnail] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [categories, setCategories] = useState([]);
  const [versions, setVersions] = useState([]);
  const [versionBusy, setVersionBusy] = useState(false);

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
        const versionItems = await courseApi.getOnlineCourseVersions(course.id);
        if (active) setVersions(versionItems);
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

  const hasNoStructure = useMemo(
    () => !(form.modules || []).some((module) => (module.lessons || []).length > 0),
    [form.modules],
  );
  const flashcardOverview = useMemo(() => getFlashcardOverview(form.modules), [form.modules]);
  const editableVersion = useMemo(() => findEditableCourseVersion(versions), [versions]);
  const hasPublishedVersion = useMemo(() => versions.some((version) => version.status === 'PUBLISHED'), [versions]);
  const legacyPendingVersion = useMemo(
    () => versions.find((version) => version.status === 'PENDING_REVIEW') || null,
    [versions],
  );
  const canEdit = !editMode || Boolean(editableVersion) || (!hasPublishedVersion && !legacyPendingVersion);
  const categoryOptions = useMemo(() => {
    const fallback = ['IELTS', 'TOEIC', 'COMMUNICATION', 'FOUNDATION'];
    const available = categories
      .filter((category) => ALLOWED_ENGLISH_CATEGORIES.has(category.code))
      .filter((category) => category.active || category.code === form.category)
      .map((category) => ({ label: category.name, value: category.code }));
    return available.length ? available : fallback.map((value) => ({ label: value, value }));
  }, [categories, form.category]);

  const handleChange = (field) => (event) => {
    const value = field === 'featured' ? event.target.checked : event.target.value;
    setForm((current) => (
      field === 'category'
        ? { ...current, category: value, ...courseProfileDefaults(value) }
        : { ...current, [field]: value }
    ));
  };

  const handleThumbnailUpload = async (file) => {
    if (!file) return;
    if (!/^image\/(jpeg|png)$/i.test(file.type)) {
      setError('Chỉ hỗ trợ ảnh JPG hoặc PNG cho ảnh bìa.');
      return;
    }

    setUploadingThumbnail(true);
    setError('');
    try {
      const uploaded = await courseApi.uploadOnlineCourseThumbnail(file);
      if (!uploaded?.url) throw new Error('Máy chủ không trả về liên kết ảnh.');
      setForm((current) => ({ ...current, thumbnailUrl: uploaded.url }));
      setSuccess('Đã tải ảnh bìa lên.');
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Không thể tải ảnh bìa lên.');
    } finally {
      setUploadingThumbnail(false);
    }
  };

  const handleSubmit = async (nextStatus = null) => {
    const publishingVersion = nextStatus === 'PUBLISH';
    const targetStatus = publishingVersion ? form.status : nextStatus ?? form.status;
    const validationMessage = validateCourseForm(form, publishingVersion ? 'PUBLISHED' : targetStatus, hasNoStructure);
    if (validationMessage) {
      setError(validationMessage);
      setSuccess('');
      return;
    }

    if (publishingVersion && !await confirmDialog(
      `Mọi học viên sẽ nhận nội dung v${editableVersion?.versionNumber} sau khi xuất bản; các bài đã hoàn thành vẫn được giữ nguyên.`,
      {
        title: `Xuất bản phiên bản v${editableVersion?.versionNumber}`,
        confirmLabel: 'Xuất bản',
      },
    )) return;

    setSaving(true);
    setSavingAction(publishingVersion ? 'publish' : 'save');
    setError('');
    setSuccess('');

    const payload = {
      title: form.title,
      shortDescription: form.shortDescription,
      description: form.description,
      category: form.category,
      level: form.level,
      targetScore: form.targetScore,
      targetOutcome: form.targetOutcome,
      duration: form.duration,
      thumbnailUrl: form.thumbnailUrl,
      featured: form.featured,
      price: Number(form.price || 0),
      salePrice: form.salePrice === '' ? null : Number(form.salePrice || 0),
      recommendedCurrentBandMin: form.recommendedCurrentBandMin === '' ? null : Number(form.recommendedCurrentBandMin),
      targetBand: form.targetBand === '' ? null : Number(form.targetBand),
      status: targetStatus,
    };

    try {
      if (editMode && !courseId) {
        throw new Error('Missing course id');
      }
      if (editMode && !canEdit) {
        throw new Error('Hãy tạo phiên bản nháp mới trước khi chỉnh sửa khóa học đã xuất bản.');
      }

      let response;
      if (editMode && editableVersion) {
        const updatedVersion = await courseApi.updateOnlineCourseVersion(courseId, editableVersion.id, payload);
        response = updatedVersion.content;
      } else {
        response = editMode
          ? await courseApi.updateOnlineCourse(courseId, payload)
          : await courseApi.createOnlineCourse(payload);
      }

      if (!response) throw new Error('Dữ liệu phiên bản trả về không hợp lệ.');

      if (publishingVersion && editableVersion) {
        await courseApi.publishOnlineCourseVersion(courseId, editableVersion.id);
        setVersions(await courseApi.getOnlineCourseVersions(courseId));
        response = await courseApi.getManagedOnlineCourse(courseId);
      }

      setCourseId(response.id);
      setCourseSlug(response.slug || '');
      setForm(mapCourseToForm(response));
      setSuccess(publishingVersion ? 'Đã lưu và xuất bản phiên bản.' : 'Khóa học đã được lưu thành công.');

      if (!editMode && !onClose) {
        navigate(`/content-manager/courses/${response.slug}/edit`, { replace: true });
      }
      if (onSave) {
        onSave(response);
      }
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Không lưu được khóa học.');
    } finally {
      setSaving(false);
      setSavingAction('');
    }
  };

  const handleCreateVersion = async (changeNote = '') => {
    if (!courseId) return;
    setVersionBusy(true);
    setError('');
    setSuccess('');
    try {
      await courseApi.createOnlineCourseVersion(
        courseId,
        changeNote || 'Cập nhật nội dung từ phiên bản đang xuất bản.',
      );
      const versionItems = await courseApi.getOnlineCourseVersions(courseId);
      setVersions(versionItems);
      setSuccess('Đã tạo bản nháp mới. Các thay đổi từ đây không ảnh hưởng học viên hiện tại.');
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tạo phiên bản mới.');
    } finally {
      setVersionBusy(false);
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
        {editMode && form.title ? (
          <Link className="inline-flex items-center gap-2 rounded-2xl border border-[#730014] bg-white px-4 py-3 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]" to={`/content-manager/courses/${slugOrId}/preview`}>
            <Eye className="h-4 w-4" />
            Xem trước
          </Link>
        ) : null}
      </div>
      <div className="space-y-6">
        {error ? <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">{error}</div> : null}
        {success ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm font-semibold text-emerald-700">{success}</div> : null}

        {editMode ? (
          <CourseVersionPanel
            busy={saving || versionBusy}
            onCreateDraft={handleCreateVersion}
            onSubmitReview={() => handleSubmit('SUBMIT_REVIEW')}
            previewBasePath={`/content-manager/courses/${courseSlug || slugOrId}/preview`}
            versions={versions}
          />
        ) : null}

        <div className="space-y-5">
          <FormSection number="01" title="Thông tin khóa học">
            <div className="grid gap-4 md:grid-cols-2">
              <TextField label="Tên khóa học" onChange={handleChange('title')} value={form.title} />
              <SelectField label="Danh mục" onChange={handleChange('category')} options={categoryOptions} value={form.category} />
              <SelectField label="Trình độ" onChange={handleChange('level')} options={['BEGINNER', 'INTERMEDIATE', 'ADVANCED']} value={form.level} />
              <TextField label="Thời gian hoàn thành dự kiến" onChange={handleChange('duration')} value={form.duration} />
            </div>
          </FormSection>

          <FormSection number="02" title="Đầu vào và kết quả">
            <div className="grid gap-4 md:grid-cols-2">
            {form.category === 'IELTS' ? (
              <>
                <IeltsBandSelect
                  allowEmpty
                  emptyLabel="Không giới hạn band đầu vào"
                  label="Band IELTS đầu vào"
                  onChange={(value) => setForm((current) => ({ ...current, recommendedCurrentBandMin: value }))}
                  value={String(form.recommendedCurrentBandMin)}
                />
                <IeltsBandSelect
                  label="Band IELTS mục tiêu"
                  onChange={(value) => setForm((current) => ({ ...current, targetBand: value }))}
                  value={String(form.targetBand)}
                />
              </>
            ) : null}
            {form.category === 'TOEIC' ? (
              <ToeicScoreField
                label="Điểm TOEIC mục tiêu"
                onChange={(value) => setForm((current) => ({ ...current, targetScore: value }))}
                value={form.targetScore}
              />
            ) : null}
            {['COMMUNICATION', 'FOUNDATION'].includes(form.category) ? <TextField label="Chuẩn đầu ra hiển thị" onChange={handleChange('targetScore')} value={form.targetScore} /> : null}
            </div>
            <div className="mt-4">
              <RichTextEditor
                label="Đầu ra / kết quả hoàn thành khóa học"
                onChange={(html) => setForm((current) => ({ ...current, targetOutcome: html }))}
                placeholder="Học viên đạt được gì sau khi hoàn thành khóa..."
                size="compact"
                value={form.targetOutcome}
              />
            </div>
          </FormSection>

          <FormSection number="03" title="Giá bán và hiển thị">
            <div className="grid gap-4 md:grid-cols-2">
              <TextField label="Giá bán" onChange={handleChange('price')} value={String(form.price)} />
              <TextField label="Giá ưu đãi" onChange={handleChange('salePrice')} value={String(form.salePrice)} />
              <SelectField disabled label="Trạng thái khóa học" onChange={handleChange('status')} options={['DRAFT', 'PUBLISHED', 'ARCHIVED']} value={form.status} />
            </div>
            <div className="mt-4 grid gap-4 lg:grid-cols-[minmax(0,1fr)_200px]">
              <div className="space-y-3">
                <TextField label="Liên kết ảnh bìa" onChange={handleChange('thumbnailUrl')} value={form.thumbnailUrl} />
                <label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-dashed border-[#c99599] bg-[#fffafb] px-4 py-3 text-sm font-bold text-[#730014] transition hover:border-[#730014] hover:bg-[#fff2f3]">
                  <Upload className="h-4 w-4" />
                  {uploadingThumbnail ? 'Đang tải ảnh...' : 'Tải ảnh bìa lên'}
                  <input
                    accept="image/jpeg,image/png"
                    className="hidden"
                    disabled={uploadingThumbnail}
                    onChange={(event) => {
                      handleThumbnailUpload(event.target.files?.[0] || null);
                      event.target.value = '';
                    }}
                    type="file"
                  />
                </label>
              </div>
              <CourseThumbnailPreview url={form.thumbnailUrl} />
            </div>
            <label className="mt-4 flex cursor-pointer items-center gap-3 border-t border-[#f4eeee] pt-4 text-sm text-[#1a1c1c]">
              <input
                className="h-4.5 w-4.5 rounded border-gray-300 text-[#4b0009] focus:ring-[#730014]"
                checked={form.featured}
                onChange={handleChange('featured')}
                type="checkbox"
              />
              <span className="font-semibold text-slate-700">Đánh dấu là khóa học nổi bật</span>
            </label>
          </FormSection>

          <FormSection number="04" title="Giới thiệu khóa học">
            <div className="grid gap-4">
            <RichTextEditor
              label="Mô tả ngắn"
              onChange={(html) => setForm((current) => ({ ...current, shortDescription: html }))}
              placeholder="Tóm tắt hấp dẫn về khóa học (hiển thị trên thẻ/catalog)..."
              size="compact"
              value={form.shortDescription}
            />
            <RichTextEditor
              label="Mô tả đầy đủ"
              onChange={(html) => setForm((current) => ({ ...current, description: html }))}
              placeholder="Mô tả chi tiết nội dung, đối tượng học viên, lộ trình..."
              size="form"
              value={form.description}
            />
            </div>
          </FormSection>
        </div>

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
              {!editMode ? (
                <p className="text-[#584140] font-semibold">
                  Tạo khóa học ở trạng thái nháp trước. Sau đó mở khu vực biên soạn để thêm mô-đun, bài học và tài nguyên.
                </p>
              ) : hasNoStructure ? (
                <p className="text-[#730014] font-semibold">
                  ⚠️ Khóa học này hiện chưa có mô-đun hoặc bài học. Hãy kiểm tra cẩn thận trước khi xuất bản.
                </p>
              ) : (
                <p>
                  Thiết lập band đầu vào, band mục tiêu và đầu ra cho khóa học. Các giá trị này được áp dụng cho lượt ghi danh mới.
                </p>
              )}
            </div>
            
            <div className="flex items-center gap-3">
              <button
                className="rounded-2xl border border-[#dfbfbd] bg-white px-6 py-3 text-sm font-semibold text-[#4b0009] transition hover:bg-[#fff2f3] active:scale-95"
                disabled={saving || !canEdit}
                onClick={() => handleSubmit()}
                type="button"
              >
                {saving && savingAction === 'save' ? (editMode ? 'Đang lưu...' : 'Đang tạo...') : (editMode ? 'Lưu thay đổi' : 'Tạo khóa học')}
              </button>
              {editMode && editableVersion ? (
                <button
                  className="rounded-2xl bg-[#4b0009] px-6 py-3 text-sm font-semibold text-white transition hover:bg-[#730014] active:scale-95 disabled:opacity-60"
                  disabled={saving || versionBusy}
                  onClick={() => handleSubmit('PUBLISH')}
                  type="button"
                >
                  {saving && savingAction === 'publish' ? 'Đang xuất bản...' : 'Lưu và xuất bản'}
                </button>
              ) : null}
            </div>
          </div>
        </Panel>
      </div>
    </div>
  );
}

function CourseThumbnailPreview({ url }) {
  return (
    <div className="flex min-h-36 items-center justify-center overflow-hidden rounded-2xl border border-[#ead9db] bg-[#fffafb]">
      {url ? (
        <img alt="Xem trước ảnh bìa khóa học" className="h-36 w-full object-cover" src={url} />
      ) : (
        <div className="flex flex-col items-center gap-2 px-4 text-center text-xs font-semibold text-[#8b706e]">
          <ImagePlus className="h-6 w-6 text-[#b99694]" />
          Chưa có ảnh bìa
        </div>
      )}
    </div>
  );
}

function SelectField({ label, value, onChange, options, disabled = false }) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">{label}</span>
      <BrandedSelect disabled={disabled} onChange={onChange} options={options} value={value} />
    </label>
  );
}

function FormSection({ children, number, title }) {
  return (
    <Panel className="p-6">
      <div className="mb-5 flex items-center gap-3 border-b border-[#f0e4e5] pb-4">
        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#4b0009] text-xs font-black text-white">{number}</span>
        <h2 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">{title}</h2>
      </div>
      {children}
    </Panel>
  );
}

function validateCourseForm(form, targetStatus, hasNoStructure) {
  if (!form.title.trim()) return 'Tên khóa học không được để trống.';
  if (!ALLOWED_ENGLISH_CATEGORIES.has(form.category)) {
    return 'EnglishLab chỉ cho phép khóa IELTS, TOEIC, tiếng Anh giao tiếp hoặc tiếng Anh nền tảng.';
  }

  const minBand = form.recommendedCurrentBandMin === '' ? null : Number(form.recommendedCurrentBandMin);
  const targetBand = form.targetBand === '' ? null : Number(form.targetBand);
  if (form.category === 'IELTS') {
    if ([minBand, targetBand].some((value) => value != null && (!Number.isFinite(value) || value < 0 || value > 9 || !Number.isInteger(value * 2)))) {
      return 'Band IELTS phải từ 0 đến 9 và tăng theo bước 0.5.';
    }
    if (targetBand == null) return 'Khóa IELTS phải có band mục tiêu.';
  } else if (minBand != null || targetBand != null) {
    return 'Chỉ khóa IELTS mới sử dụng thang band IELTS.';
  }
  if (form.category === 'TOEIC') {
    const score = Number(form.targetScore);
    if (!Number.isInteger(score) || score < 10 || score > 990 || score % 5 !== 0) {
      return 'Điểm TOEIC phải từ 10 đến 990 và tăng theo bước 5.';
    }
  }
  if (targetStatus === 'PUBLISHED' && hasNoStructure) {
    return 'Khóa học cần có ít nhất một mô-đun và bài học trước khi xuất bản.';
  }
  if (targetStatus === 'PUBLISHED' && !String(form.targetOutcome || '').trim()) {
    return 'Khóa học phải mô tả chuẩn đầu ra tiếng Anh trước khi xuất bản.';
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
