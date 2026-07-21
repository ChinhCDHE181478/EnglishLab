import { useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  ArrowLeft,
  BookOpen,
  CheckCircle2,
  Clock3,
  ExternalLink,
  FileText,
  Layers3,
  MonitorPlay,
  ShieldCheck,
  Tags,
} from 'lucide-react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { looksLikeRichTextHtml, sanitizeLessonHtml } from '../../utils/lessonRichText';
import { formatModuleTitle } from '../../utils/courseModuleTitle';

const statusMeta = {
  DRAFT: { label: 'Bản nháp', banner: 'border-amber-200 bg-amber-50 text-amber-900', badge: 'bg-amber-100 text-amber-800' },
  PENDING_REVIEW: { label: 'Chờ duyệt', banner: 'border-sky-200 bg-sky-50 text-sky-900', badge: 'bg-sky-100 text-sky-800' },
  PUBLISHED: { label: 'Đã xuất bản', banner: 'border-emerald-200 bg-emerald-50 text-emerald-900', badge: 'bg-emerald-100 text-emerald-800' },
  REJECTED: { label: 'Bị từ chối', banner: 'border-rose-200 bg-rose-50 text-rose-900', badge: 'bg-rose-100 text-rose-800' },
  ARCHIVED: { label: 'Đã lưu trữ', banner: 'border-slate-300 bg-slate-100 text-slate-800', badge: 'bg-slate-200 text-slate-700' },
};

export default function ContentManagerCoursePreviewPage() {
  const { slugOrId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedVersionId = searchParams.get('versionId') || '';
  const [preview, setPreview] = useState(null);
  const [versions, setVersions] = useState([]);
  const [activeLessonId, setActiveLessonId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const managedCourse = await courseApi.getManagedOnlineCourse(slugOrId);
      const versionItems = await courseApi.getOnlineCourseVersions(managedCourse.id);
      setVersions(versionItems);
      const selectedVersion = versionItems.find((item) => String(item.id) === String(selectedVersionId))
        || versionItems.find((item) => item.status === 'DRAFT')
        || versionItems.find((item) => item.status === 'PENDING_REVIEW')
        || versionItems.find((item) => item.status === 'PUBLISHED');
      const data = selectedVersion
        ? await courseApi.getManagedOnlineCourseVersionPreview(managedCourse.id, selectedVersion.id)
        : await courseApi.getManagedOnlineCoursePreview(slugOrId);
      setPreview(data);
      const firstLesson = data?.modules?.flatMap((module) => module.lessons || [])[0];
      setActiveLessonId(firstLesson?.id || null);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải bản xem trước khóa học.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [selectedVersionId, slugOrId]);

  const lessonItems = useMemo(() => (preview?.modules || []).flatMap((module) => (
    (module.lessons || []).map((lesson) => ({ lesson, module }))
  )), [preview?.modules]);
  const activeItem = lessonItems.find((item) => String(item.lesson.id) === String(activeLessonId)) || lessonItems[0] || null;
  const course = preview?.course;
  const selectedVersion = versions.find((item) => String(item.id) === String(selectedVersionId))
    || versions.find((item) => item.status === 'DRAFT')
    || versions.find((item) => item.status === 'PENDING_REVIEW')
    || versions.find((item) => item.status === 'PUBLISHED');
  const status = statusMeta[selectedVersion?.status] || statusMeta[course?.status] || statusMeta.DRAFT;

  if (loading) {
    return <PreviewLoading />;
  }

  if (error || !preview || !course) {
    return (
      <div className="flex min-h-[520px] flex-col items-center justify-center rounded-[28px] border border-rose-200 bg-white px-6 text-center">
        <AlertTriangle className="h-14 w-14 text-rose-400" />
        <h1 className="mt-4 text-2xl font-black text-[#0b1c30]">Không mở được bản xem trước</h1>
        <p className="mt-2 max-w-xl text-sm leading-6 text-slate-500">{error || 'Dữ liệu preview không hợp lệ.'}</p>
        <div className="mt-5 flex gap-2">
          <Link className="rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-bold text-[#730014]" to="/content-manager/courses">Quay lại danh sách</Link>
          <button className="rounded-2xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white" onClick={load} type="button">Thử lại</button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center gap-3">
        <Link className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/65 bg-white px-4 py-3 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]" to="/content-manager/courses">
          <ArrowLeft className="h-4 w-4" />
          Quay lại danh sách
        </Link>
        <Link className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]" to={`/content-manager/courses/${course.slug}/edit`}>
          Chỉnh sửa thông tin
        </Link>
        <Link className="inline-flex items-center gap-2 rounded-2xl border border-[#730014] bg-white px-4 py-3 text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]" to={`/content-manager/courses/${course.slug}/builder`}>
          Mở trình biên soạn
        </Link>
      </div>

      <section className={`flex flex-col justify-between gap-3 rounded-2xl border px-5 py-4 sm:flex-row sm:items-center ${status.banner}`}>
        <div className="flex items-start gap-3">
          <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0" />
          <div>
            <p className="text-sm font-extrabold">Chế độ xem trước · {selectedVersion ? `v${selectedVersion.versionNumber} · ${statusMeta[selectedVersion.status]?.label || selectedVersion.status}` : status.label}</p>
            <p className="mt-1 text-xs leading-5 opacity-75">Chế độ xem trước dành cho người biên soạn. Không lưu tiến độ học hay kết quả bài làm.</p>
          </div>
        </div>
        <div className="flex min-w-[220px] items-center gap-2">
          <BrandedSelect
            buttonClassName="py-2 shadow-none"
            onChange={(event) => setSearchParams({ versionId: event.target.value })}
            options={versions.map((version) => ({
              value: String(version.id),
              label: `v${version.versionNumber} · ${statusMeta[version.status]?.label || version.status}`,
              description: version.changeNote || 'Không có ghi chú thay đổi',
            }))}
            placeholder="Chọn phiên bản để xem"
            value={selectedVersion?.id ? String(selectedVersion.id) : ''}
          />
          <span className={`shrink-0 rounded-full px-3 py-1 text-xs font-extrabold ${status.badge}`}>Chỉ đọc</span>
        </div>
      </section>

      <CourseHero course={course} moduleCount={preview.modules.length} status={status} />

      <ValidationWarnings warnings={preview.validationWarnings || []} />

      <div className="grid min-h-[620px] gap-5 xl:grid-cols-[340px_minmax(0,1fr)]">
        <CourseOutline
          activeLessonId={activeItem?.lesson?.id}
          modules={preview.modules || []}
          onSelectLesson={setActiveLessonId}
        />
        <div className="min-w-0 space-y-5">
          {activeItem ? (
            <LessonPreview lesson={activeItem.lesson} module={activeItem.module} />
          ) : (
            <div className="flex min-h-[420px] flex-col items-center justify-center rounded-[28px] border border-dashed border-slate-300 bg-white text-center">
              <BookOpen className="h-14 w-14 text-slate-300" />
              <h2 className="mt-4 text-xl font-black text-[#0b1c30]">Khóa học chưa có bài học để xem</h2>
            </div>
          )}
          <AssessmentPreview assessments={preview.assessments || []} modules={preview.modules || []} />
        </div>
      </div>
    </div>
  );
}

function PreviewLoading() {
  return <div className="space-y-5"><div className="h-14 animate-pulse rounded-2xl bg-slate-100" /><div className="h-80 animate-pulse rounded-[28px] bg-slate-100" /><div className="grid gap-5 xl:grid-cols-[340px_1fr]"><div className="h-[560px] animate-pulse rounded-[28px] bg-slate-100" /><div className="h-[560px] animate-pulse rounded-[28px] bg-slate-100" /></div></div>;
}

function CourseHero({ course, moduleCount, status }) {
  return (
    <section className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm">
      <div className="grid lg:grid-cols-[380px_minmax(0,1fr)]">
        <div className="flex min-h-[270px] items-center justify-center bg-gradient-to-br from-[#4b0009] to-[#a6122a]">
          {course.thumbnailUrl ? <img alt={course.title} className="h-full max-h-[360px] w-full object-cover" src={course.thumbnailUrl} /> : <BookOpen className="h-20 w-20 text-white/30" />}
        </div>
        <div className="p-6 md:p-8">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`rounded-full px-3 py-1 text-xs font-extrabold ${status.badge}`}>{status.label}</span>
            <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-extrabold text-slate-600">{course.level || 'Chưa có level'}</span>
            <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-extrabold text-slate-600">{course.studyMode || 'Online'}</span>
          </div>
          <h1 className="mt-4 font-['Manrope'] text-3xl font-black leading-tight text-[#0b1c30] md:text-4xl">{course.title}</h1>
          <p className="mt-3 text-sm leading-7 text-slate-600">{course.description || course.shortDescription || 'Khóa học chưa có mô tả.'}</p>
          <div className="mt-6 grid grid-cols-2 gap-3 md:grid-cols-4">
            <HeroMetric icon={Layers3} label="Mô-đun" value={moduleCount} />
            <HeroMetric icon={BookOpen} label="Bài học" value={course.totalLessons || 0} />
            <HeroMetric icon={Clock3} label="Thời lượng" value={course.duration || `${course.totalHours || 0} giờ`} />
            <HeroMetric icon={Tags} label="Học phí" value={formatPrice(course.salePrice ?? course.price)} />
          </div>
          <p className="mt-5 text-xs font-semibold text-slate-400">Danh mục: {course.categoryName || course.category || 'Chưa phân loại'} · slug: {course.slug}</p>
        </div>
      </div>
    </section>
  );
}

function HeroMetric({ icon: Icon, label, value }) {
  return <div className="rounded-2xl bg-slate-50 p-3"><Icon className="h-4 w-4 text-[#8a0018]" /><p className="mt-2 text-[10px] font-extrabold uppercase tracking-[0.1em] text-slate-400">{label}</p><p className="mt-1 text-sm font-black text-[#0b1c30]">{value}</p></div>;
}

function ValidationWarnings({ warnings }) {
  if (!warnings.length) {
    return (
      <section className="flex items-center gap-3 rounded-2xl border border-emerald-200 bg-emerald-50/70 px-4 py-3 text-sm font-semibold text-emerald-800">
        <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
        Nội dung khóa học đã đầy đủ thông tin cần thiết.
      </section>
    );
  }
  const hasErrors = warnings.some((w) => w.severity === 'ERROR');

  return (
    <section className={`rounded-2xl border p-4 transition ${hasErrors ? 'border-rose-200 bg-rose-50/60' : 'border-amber-200 bg-amber-50/60'}`}>
      <div className="flex items-center gap-2.5">
        <AlertTriangle className={`h-4 w-4 shrink-0 ${hasErrors ? 'text-rose-600' : 'text-amber-600'}`} />
        <h2 className="text-sm font-extrabold text-[#2b2828]">
          Cần hoàn thiện {warnings.length} mục trước khi phát hành
        </h2>
      </div>
      <div className="mt-3 space-y-2">
        {warnings.map((warning, index) => (
          <div className="flex items-start gap-3 rounded-xl border border-white/80 bg-white/90 px-3.5 py-2.5 shadow-sm" key={`${warning.code}-${index}`}>
            <span className={`mt-0.5 shrink-0 rounded-md px-2 py-0.5 text-[11px] font-extrabold ${warning.severity === 'ERROR' ? 'bg-rose-100 text-rose-700' : 'bg-amber-100 text-amber-800'}`}>
              {warning.severity === 'ERROR' ? 'Cần bổ sung' : 'Lưu ý'}
            </span>
            <p className="text-xs font-semibold leading-5 text-[#3f3030]">
              {warning.message}
            </p>
          </div>
        ))}
      </div>
    </section>
  );
}

function CourseOutline({ activeLessonId, modules, onSelectLesson }) {
  return (
    <aside className="self-start overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-sm xl:sticky xl:top-5">
      <div className="border-b border-slate-100 bg-[#fff7f7] px-5 py-4"><p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">Nội dung khóa học</p><h2 className="mt-1 text-xl font-black text-[#0b1c30]">Mô-đun và bài học</h2></div>
      <div className="max-h-[680px] overflow-y-auto p-3">
        {modules.map((module, moduleIndex) => (
          <section className="mb-3 overflow-hidden rounded-2xl border border-slate-100" key={module.id || moduleIndex}>
            <div className="bg-slate-50 px-4 py-3"><h3 className="text-sm font-black text-slate-800">{formatModuleTitle(module.title, moduleIndex)}</h3><p className="mt-1 text-xs text-slate-500">{module.lessons?.length || 0} bài học</p></div>
            <div className="p-2">
              {(module.lessons || []).map((lesson, lessonIndex) => (
                <button className={`flex w-full items-start gap-3 rounded-xl px-3 py-3 text-left transition ${String(activeLessonId) === String(lesson.id) ? 'bg-[#730014] text-white' : 'text-slate-600 hover:bg-slate-50'}`} key={lesson.id || lessonIndex} onClick={() => onSelectLesson(lesson.id)} type="button">
                  <span className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[10px] font-black ${String(activeLessonId) === String(lesson.id) ? 'bg-white/15' : 'bg-slate-100 text-slate-500'}`}>{lessonIndex + 1}</span>
                  <span className="min-w-0"><span className="block text-sm font-extrabold leading-5">{lesson.title}</span><span className={`mt-1 block text-[10px] font-semibold ${String(activeLessonId) === String(lesson.id) ? 'text-white/60' : 'text-slate-400'}`}>{formatContentType(lesson.contentType)} · {lesson.durationMinutes || 0} phút</span></span>
                </button>
              ))}
              {!module.lessons?.length ? <p className="px-3 py-5 text-center text-xs text-slate-400">Chưa có bài học</p> : null}
            </div>
          </section>
        ))}
      </div>
    </aside>
  );
}

function LessonPreview({ lesson, module }) {
  const embedUrl = getVideoEmbedUrl(lesson.bunnyCdnUrl || lesson.videoUrl);
  const directVideoUrl = (lesson.bunnyCdnUrl || lesson.videoUrl) && !embedUrl ? lesson.bunnyCdnUrl || lesson.videoUrl : '';
  return (
    <article className="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-sm">
      {embedUrl ? <div className="aspect-video bg-black"><iframe allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowFullScreen className="h-full w-full" src={embedUrl} title={lesson.title} /></div> : null}
      {directVideoUrl ? <div className="bg-black"><video className="aspect-video w-full" controls preload="metadata" src={directVideoUrl}><track kind="captions" /></video></div> : null}
      <div className="p-6 md:p-8">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div><p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">{module.title}</p><h2 className="mt-2 font-['Manrope'] text-3xl font-black text-[#0b1c30]">{lesson.title}</h2></div>
          <span className="rounded-xl bg-slate-100 px-3 py-2 text-xs font-extrabold text-slate-600">{formatContentType(lesson.contentType)} · {lesson.durationMinutes || 0} phút</span>
        </div>
        {lesson.description ? <p className="mt-4 text-sm leading-7 text-slate-500">{lesson.description}</p> : null}
        <LessonContent content={lesson.contentText} />
        {!lesson.contentText && !embedUrl && !directVideoUrl ? <div className="mt-5 rounded-2xl border border-dashed border-amber-300 bg-amber-50 p-5 text-sm font-bold text-amber-800">Bài học chưa có nội dung chính để hiển thị.</div> : null}
        {lesson.materialUrl ? <a className="mt-5 inline-flex items-center gap-2 rounded-2xl border border-[#dcb6bb] bg-[#fff8f8] px-4 py-3 text-sm font-extrabold text-[#8a0018]" href={lesson.materialUrl} rel="noreferrer" target="_blank"><FileText className="h-4 w-4" />Mở tài liệu bài học<ExternalLink className="h-3.5 w-3.5" /></a> : null}
        <TranscriptPreview segments={lesson.transcriptSegments || []} />
        <FlashcardPreview sets={lesson.flashcardSets || []} />
      </div>
    </article>
  );
}

/** Convert a subset of Markdown to HTML so both storage formats render correctly in preview. */
const markdownToHtml = (text = '') => {
  const lines = String(text).split('\n');
  const out = [];
  let inUl = false;
  let inOl = false;

  const flushList = () => {
    if (inUl) { out.push('</ul>'); inUl = false; }
    if (inOl) { out.push('</ol>'); inOl = false; }
  };

  const inlineMarkdown = (s) =>
    s
      .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      .replace(/\*([^*]+)\*/g, '<em>$1</em>');

  for (const raw of lines) {
    const line = raw.trimEnd();
    if (!line.trim()) {
      flushList();
      out.push('<br>');
      continue;
    }
    if (line.startsWith('# '))  { flushList(); out.push(`<h1>${inlineMarkdown(line.slice(2))}</h1>`); continue; }
    if (line.startsWith('## ')) { flushList(); out.push(`<h2>${inlineMarkdown(line.slice(3))}</h2>`); continue; }
    if (line.startsWith('### ')) { flushList(); out.push(`<h3>${inlineMarkdown(line.slice(4))}</h3>`); continue; }
    if (line.startsWith('#### ')) { flushList(); out.push(`<h4>${inlineMarkdown(line.slice(5))}</h4>`); continue; }

    const olMatch = line.match(/^(\d+)\.\s+(.*)/);
    if (olMatch) {
      if (!inOl) { if (inUl) { out.push('</ul>'); inUl = false; } out.push('<ol>'); inOl = true; }
      out.push(`<li>${inlineMarkdown(olMatch[2])}</li>`);
      continue;
    }

    if (line.startsWith('- ') || line.startsWith('* ')) {
      if (!inUl) { if (inOl) { out.push('</ol>'); inOl = false; } out.push('<ul>'); inUl = true; }
      out.push(`<li>${inlineMarkdown(line.slice(2))}</li>`);
      continue;
    }

    flushList();
    const boldHeading = line.match(/^\*\*([^*]+)\*\*:?\s*$/);
    if (boldHeading) { out.push(`<h4>${boldHeading[1]}</h4>`); continue; }
    out.push(`<p>${inlineMarkdown(line)}</p>`);
  }
  flushList();
  return out.join('\n');
};

const isRichTextHtml = (value = '') => /\s*<\/?(?:h[1-6]|p|div|strong|em|u|s|ul|ol|li|blockquote|pre|a|br|hr)\b/i.test(String(value));

const LESSON_HTML_CLASSES = [
  'mt-5 select-text rounded-[24px] border border-[#ead9db] bg-[#fffdfc] p-5 text-sm leading-7 text-[#3f3030]',
  'selection:bg-[#fff0f1] selection:text-[#4b0009]',
  '[&_a]:font-semibold [&_a]:text-[#730014] [&_a]:underline',
  '[&_h1]:mb-3 [&_h1]:text-xl [&_h1]:font-bold [&_h1]:text-[#1f2430]',
  '[&_h2]:mb-2 [&_h2]:mt-6 [&_h2]:text-base [&_h2]:font-bold [&_h2]:text-[#1f2430]',
  '[&_h3]:mb-1.5 [&_h3]:mt-5 [&_h3]:text-sm [&_h3]:font-bold [&_h3]:uppercase [&_h3]:tracking-wide [&_h3]:text-[#5f5353]',
  '[&_h4]:mb-1 [&_h4]:mt-4 [&_h4]:text-sm [&_h4]:font-bold [&_h4]:text-[#1f2430]',
  '[&_p]:my-1.5',
  '[&_ul]:my-3 [&_ul]:space-y-1 [&_ul]:pl-0 [&_ul>li]:flex [&_ul>li]:gap-2 [&_ul>li]:items-baseline [&_ul>li]:before:mt-1.5 [&_ul>li]:before:h-1.5 [&_ul>li]:before:w-1.5 [&_ul>li]:before:shrink-0 [&_ul>li]:before:rounded-full [&_ul>li]:before:bg-[#4b0009] [&_ul>li]:before:content-[""]',
  '[&_ol]:my-3 [&_ol]:list-decimal [&_ol]:pl-6 [&_ol>li]:pl-1 [&_ol>li]:marker:font-bold [&_ol>li]:marker:text-[#4b0009]',
  '[&_blockquote]:my-4 [&_blockquote]:border-l-4 [&_blockquote]:border-[#dfbfbd] [&_blockquote]:bg-[#fff7f7] [&_blockquote]:px-4 [&_blockquote]:py-2 [&_blockquote]:italic [&_blockquote]:text-[#584140]',
  '[&_strong]:font-bold [&_strong]:text-[#1f2430] [&_b]:font-bold [&_b]:text-[#1f2430]',
  '[&_pre]:overflow-x-auto [&_pre]:rounded-xl [&_pre]:bg-slate-900 [&_pre]:p-4 [&_pre]:text-white',
].join(' ');

function LessonContent({ content }) {
  if (!content) return null;
  const html = isRichTextHtml(content)
    ? sanitizeLessonHtml(content)
    : sanitizeLessonHtml(markdownToHtml(content));

  return (
    <div
      className={LESSON_HTML_CLASSES}
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}

function TranscriptPreview({ segments }) {
  if (!segments.length) return null;
  return <section className="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-5"><h3 className="font-black text-[#0b1c30]">Transcript ({segments.length} đoạn)</h3><div className="mt-3 max-h-72 space-y-2 overflow-y-auto">{segments.map((segment, index) => <div className="grid gap-2 rounded-xl bg-white p-3 text-sm sm:grid-cols-[72px_1fr]" key={`${segment.startSeconds}-${index}`}><span className="font-extrabold text-[#730014]">{formatSeconds(segment.startSeconds)}</span><span className="leading-6 text-slate-600">{segment.text}</span></div>)}</div></section>;
}

function FlashcardPreview({ sets }) {
  if (!sets.length) return null;
  return <section className="mt-6"><h3 className="font-black text-[#0b1c30]">Flashcards ({sets.length} bộ)</h3><div className="mt-3 grid gap-3 sm:grid-cols-2">{sets.map((set) => <div className="rounded-2xl border border-slate-200 bg-white p-4" key={set.id}><p className="font-extrabold text-[#730014]">{set.title}</p><p className="mt-2 text-xs text-slate-500">{countFlashcards(set)} thẻ · {set.skill || 'Tổng hợp'}</p></div>)}</div></section>;
}

function AssessmentPreview({ assessments, modules }) {
  const moduleNames = Object.fromEntries(modules.map((module) => [String(module.id), module.title]));
  return (
    <section className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex items-center gap-3"><div className="rounded-xl bg-[#fff4f5] p-2.5 text-[#730014]"><MonitorPlay className="h-5 w-5" /></div><div><p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">Read-only assessment preview</p><h2 className="mt-1 text-xl font-black text-[#0b1c30]">Bài tập và đánh giá ({assessments.length})</h2></div></div>
      {assessments.length ? <div className="mt-5 grid gap-3 md:grid-cols-2">{assessments.map((assessment) => <article className="rounded-2xl border border-slate-200 bg-slate-50 p-4" key={assessment.id}><div className="flex flex-wrap gap-2"><span className="rounded-lg bg-white px-2 py-1 text-[10px] font-extrabold text-[#730014]">{assessment.type}</span><span className="rounded-lg bg-white px-2 py-1 text-[10px] font-extrabold text-slate-600">{assessment.skill}</span></div><h3 className="mt-3 font-extrabold text-[#0b1c30]">{assessment.title}</h3><p className="mt-1 text-xs text-slate-500">{assessment.moduleId ? moduleNames[String(assessment.moduleId)] || `Mô-đun #${assessment.moduleId}` : 'Đánh giá cuối khóa'}</p><p className="mt-3 text-xs font-bold text-slate-600">Điểm đạt: {assessment.resolvedPassingThreshold ?? assessment.passingScore ?? '—'} / {assessment.maxScore ?? '—'} · {assessment.timeLimitMinutes || 0} phút</p></article>)}</div> : <p className="mt-5 rounded-2xl border border-dashed border-slate-300 p-6 text-center text-sm text-slate-400">Khóa học chưa có bài đánh giá.</p>}
    </section>
  );
}

function getVideoEmbedUrl(url) {
  if (!url) return '';
  const value = String(url).trim();
  if (/iframe\.mediadelivery\.net\/embed\//i.test(value)) return value;
  const youtubeMatch = value.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([^&?/]+)/);
  return youtubeMatch?.[1] ? `https://www.youtube.com/embed/${youtubeMatch[1]}` : '';
}

function formatPrice(value) {
  const amount = Number(value || 0);
  return amount === 0 ? 'Miễn phí' : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(amount);
}

function formatContentType(value) {
  const labels = { VIDEO: 'Video', ARTICLE: 'Bài đọc', ASSIGNMENT: 'Bài tập', QUIZ: 'Trắc nghiệm' };
  return labels[String(value || '').toUpperCase()] || value || 'Bài học';
}

function formatSeconds(value) {
  const seconds = Math.max(0, Number(value || 0));
  return `${Math.floor(seconds / 60)}:${String(Math.floor(seconds % 60)).padStart(2, '0')}`;
}

function countFlashcards(set) {
  try {
    const cards = JSON.parse(set.cardsJson || '[]');
    return Array.isArray(cards) ? cards.length : 0;
  } catch {
    return 0;
  }
}
