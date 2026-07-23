import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, BookOpenCheck, Save } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import classroomApi from '../../api/classroomApi';
import curriculumApi from '../../api/curriculumApi';
import { ContentManagerLoadingState } from '../../components/content-manager/ContentManagerUi';
import RichTextEditor from '../../components/content-manager/RichTextEditor';
import BrandedSelect from '../../components/ui/BrandedSelect';
import {
  ERROR_NOTICE_CLASS,
  FIELD_CLASS,
  PRIMARY_BUTTON_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
} from '../../utils/formStyles';

const emptyForm = (mode) => ({
  title: '',
  code: '',
  slug: '',
  curriculumProgramId: '',
  shortDescription: '',
  description: '',
  price: '',
  salePrice: '',
  duration: '',
  studyMode: mode === 'VIRTUAL' ? 'Virtual với giảng viên' : 'Offline tại trung tâm',
  capacity: 30,
  plannedStartDate: '',
  plannedSchedule: '',
  thumbnailUrl: '',
  status: 'DRAFT',
  displayOrder: 0,
  featured: false,
});

const statusOptions = [
  { label: 'Nháp', value: 'DRAFT' },
  { label: 'Đã xuất bản', value: 'PUBLISHED' },
  { label: 'Lưu trữ', value: 'ARCHIVED' },
];

const toSlug = (value) => String(value || '')
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/đ/g, 'd')
  .replace(/Đ/g, 'D')
  .replace(/[^\w\s-]/g, '')
  .trim()
  .replace(/\s+/g, '-')
  .replace(/-+/g, '-')
  .toLowerCase();

const makeCode = (title, mode) => {
  const suffix = String(title || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .replace(/[^\w\s]/g, ' ')
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 4)
    .map((word) => word.slice(0, 4).toUpperCase())
    .join('-');
  return [mode, suffix].filter(Boolean).join('-');
};

const toForm = (program, mode) => ({
  ...emptyForm(mode),
  title: program.title || '',
  code: program.code || '',
  slug: program.slug || '',
  curriculumProgramId: program.curriculumProgramId ? String(program.curriculumProgramId) : '',
  shortDescription: program.shortDescription || '',
  description: program.description || '',
  price: program.price ?? '',
  salePrice: program.salePrice ?? '',
  duration: program.duration || '',
  studyMode: program.studyMode || (mode === 'VIRTUAL' ? 'Virtual với giảng viên' : 'Offline tại trung tâm'),
  capacity: program.capacity ?? program.maxCapacity ?? 30,
  plannedStartDate: program.plannedStartDate || '',
  plannedSchedule: program.plannedSchedule || '',
  thumbnailUrl: program.thumbnailUrl || '',
  status: program.status || 'DRAFT',
  displayOrder: program.displayOrder ?? 0,
  featured: Boolean(program.featured),
});

const formatTarget = (curriculum) => {
  if (!curriculum) return '—';
  if (curriculum.targetBand != null) return `IELTS ${curriculum.targetBand}`;
  if (curriculum.targetScore != null) return `TOEIC ${curriculum.targetScore}`;
  return '—';
};

export default function ContentManagerTrainingProgramBuilderPage({ mode = 'OFFLINE' }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const isNew = !id || id === 'new';
  const basePath = mode === 'VIRTUAL' ? '/content-manager/virtual-programs' : '/content-manager/offline-programs';
  const [form, setForm] = useState(() => emptyForm(mode));
  const [curriculums, setCurriculums] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const requests = [curriculumApi.getCurriculumPrograms()];
        if (!isNew) requests.push(classroomApi.getContentManagerProgram(id));
        const [curriculumData, programData] = await Promise.all(requests);
        if (!active) return;
        setCurriculums(curriculumData);
        setForm(programData ? toForm(programData, mode) : emptyForm(mode));
      } catch (err) {
        if (active) setError(err?.response?.data?.message || 'Không tải được khóa học.');
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => {
      active = false;
    };
  }, [id, isNew, mode]);

  const updateForm = (patch) => setForm((current) => ({ ...current, ...patch }));
  const matchingCurriculums = useMemo(() => curriculums.filter((item) => item.deliveryMode === mode), [curriculums, mode]);
  const selectedCurriculum = useMemo(() => matchingCurriculums.find(
    (item) => String(item.id) === String(form.curriculumProgramId),
  ) || null, [form.curriculumProgramId, matchingCurriculums]);

  const save = async () => {
    if (!form.title.trim()) {
      setError('Vui lòng nhập tên khóa học.');
      return;
    }
    if (!form.curriculumProgramId) {
      setError('Vui lòng chọn chương trình đào tạo được sử dụng.');
      return;
    }
    if (!Number.isInteger(Number(form.capacity)) || Number(form.capacity) < 1) {
      setError('Sức chứa dự kiến phải là số nguyên lớn hơn 0.');
      return;
    }
    setSaving(true);
    setError('');
    setSuccess('');
    const payload = {
      ...form,
      title: form.title.trim(),
      code: form.code.trim() || makeCode(form.title, mode),
      slug: form.slug.trim() || toSlug(form.title),
      deliveryType: mode,
      curriculumProgramId: Number(form.curriculumProgramId),
      price: form.price === '' ? 0 : Number(form.price),
      salePrice: form.salePrice === '' ? null : Number(form.salePrice),
      capacity: Number(form.capacity),
      plannedStartDate: form.plannedStartDate || null,
      displayOrder: Number(form.displayOrder || 0),
      featured: Boolean(form.featured),
    };
    try {
      const saved = isNew
        ? await classroomApi.createContentManagerProgram(payload)
        : await classroomApi.updateContentManagerProgram(id, payload);
      setForm(toForm(saved, mode));
      setSuccess(isNew ? 'Đã tạo khóa học.' : 'Đã lưu khóa học.');
      if (isNew) navigate(`${basePath}/${saved.id}/builder`, { replace: true });
    } catch (err) {
      setError(err?.response?.data?.message || 'Không lưu được khóa học.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <ContentManagerLoadingState message="Đang mở khóa học..." />;

  return (
    <div className="space-y-5">
      <section className="rounded-2xl border border-[#dcc0bf]/35 bg-white p-5 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <Link className="inline-flex items-center gap-2 text-sm font-bold text-[#730014]" to={basePath}><ArrowLeft className="h-4 w-4" />Danh sách khóa học</Link>
            <p className="mt-4 text-xs font-extrabold uppercase tracking-[0.16em] text-[#8b706e]">{mode === 'VIRTUAL' ? 'Khóa học Virtual' : 'Khóa học Offline'}</p>
            <h1 className="mt-1 font-['Manrope'] text-2xl font-black text-[#0b1c30]">{isNew ? `Tạo khóa học ${mode === 'VIRTUAL' ? 'Virtual' : 'Offline'}` : form.title}</h1>
            <p className="mt-2 text-sm text-[#584140]">Đóng gói chương trình đào tạo thành khóa học Offline hoặc Virtual để Nhân viên đào tạo đề xuất mở lớp.</p>
          </div>
          <button className={PRIMARY_BUTTON_CLASS} disabled={saving} onClick={save} type="button"><Save className="h-4 w-4" />{saving ? 'Đang lưu...' : isNew ? 'Tạo khóa học' : 'Lưu thay đổi'}</button>
        </div>
      </section>

      {error ? <div className={ERROR_NOTICE_CLASS}>{error}</div> : null}
      {success ? <div className={SUCCESS_NOTICE_CLASS}>{success}</div> : null}

      <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_360px]">
        <div className="space-y-5">
          <BuilderSection number="01" title="Nhận diện và chương trình đào tạo">
            <div className="grid gap-4 md:grid-cols-2">
              <TextInput label="Tên khóa học" value={form.title} onChange={(value) => updateForm({ title: value, code: form.code || makeCode(value, mode), slug: form.slug || toSlug(value) })} />
              <div><FieldLabel>Chương trình đào tạo</FieldLabel><BrandedSelect onChange={(event) => updateForm({ curriculumProgramId: event.target.value })} options={matchingCurriculums.map((item) => ({ label: item.title, value: String(item.id), description: [item.code, item.examCategory, item.status, `${item.totalUnits || 0} unit`].filter(Boolean).join(' · ') }))} placeholder="Chọn chương trình đào tạo" searchable={true} value={form.curriculumProgramId} /></div>
              <TextInput label="Mã khóa học" value={form.code} onChange={(value) => updateForm({ code: value })} />
              <TextInput label="Đường dẫn (slug)" value={form.slug} onChange={(value) => updateForm({ slug: value })} />
            </div>
          </BuilderSection>

          <BuilderSection number="02" title="Đóng gói triển khai và thương mại">
            <div className="grid gap-4 md:grid-cols-2">
              <ReadOnlyField label="Hình thức triển khai" value={mode === 'VIRTUAL' ? 'Virtual' : 'Offline tại trung tâm'} />
              <TextInput label="Thời lượng triển khai" placeholder="Ví dụ: 12 tuần" value={form.duration} onChange={(value) => updateForm({ duration: value })} />
              <TextInput label="Cách tổ chức học" placeholder="Ví dụ: 3 buổi/tuần" value={form.studyMode} onChange={(value) => updateForm({ studyMode: value })} />
              <TextInput label="Sức chứa dự kiến" min="1" type="number" value={form.capacity} onChange={(value) => updateForm({ capacity: value })} />
              <TextInput label="Ngày khai giảng dự kiến" type="date" value={form.plannedStartDate} onChange={(value) => updateForm({ plannedStartDate: value })} />
              <TextInput label="Lịch học dự kiến" placeholder="Ví dụ: Thứ 2, 4, 6 · 18:30–20:30" value={form.plannedSchedule} onChange={(value) => updateForm({ plannedSchedule: value })} />
              <TextInput label="Học phí" min="0" type="number" value={form.price} onChange={(value) => updateForm({ price: value })} />
              <TextInput label="Giá ưu đãi" min="0" type="number" value={form.salePrice} onChange={(value) => updateForm({ salePrice: value })} />
              <div><FieldLabel>Trạng thái</FieldLabel><BrandedSelect onChange={(event) => updateForm({ status: event.target.value })} options={statusOptions} value={form.status} /></div>
              <TextInput label="Thứ tự hiển thị" min="0" type="number" value={form.displayOrder} onChange={(value) => updateForm({ displayOrder: value })} />
              <label className="flex items-center gap-3 rounded-xl border border-[#dcc0bf]/35 bg-[#fcfbfb] px-4 py-3 text-sm font-bold text-[#584140]"><input checked={form.featured} className="h-4 w-4 accent-[#730014]" onChange={(event) => updateForm({ featured: event.target.checked })} type="checkbox" />Đánh dấu khóa học nổi bật</label>
            </div>
          </BuilderSection>

          <BuilderSection number="03" title="Thông tin giới thiệu">
            <div className="grid gap-4">
              <TextInput label="Mô tả ngắn" value={form.shortDescription} onChange={(value) => updateForm({ shortDescription: value })} />
              <RichTextEditor
                label="Mô tả khóa học"
                onChange={(value) => updateForm({ description: value })}
                placeholder="Mô tả chi tiết chương trình đào tạo..."
                size="form"
                value={form.description}
              />
              <TextInput label="Ảnh đại diện" placeholder="URL ảnh đại diện khóa học" value={form.thumbnailUrl} onChange={(value) => updateForm({ thumbnailUrl: value })} />
            </div>
          </BuilderSection>
        </div>

        <aside className="h-fit space-y-4 xl:sticky xl:top-24">
          <section className="rounded-2xl border border-[#dcc0bf]/35 bg-white p-5 shadow-sm">
            <div className="flex items-center gap-3 border-b border-[#f0e4e5] pb-4"><BookOpenCheck className="h-5 w-5 text-[#730014]" /><div><p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#8b706e]">Nguồn nội dung</p><h2 className="mt-1 font-['Manrope'] text-lg font-black text-[#0b1c30]">Chương trình đào tạo áp dụng</h2></div></div>
            {selectedCurriculum ? <div className="mt-4 space-y-3"><p className="font-extrabold text-[#0b1c30]">{selectedCurriculum.title}</p><SummaryRow label="Nhóm thi" value={selectedCurriculum.examCategory || '—'} /><SummaryRow label="Đầu vào" value={selectedCurriculum.entryLevel || '—'} /><SummaryRow label="Mục tiêu" value={formatTarget(selectedCurriculum)} /><SummaryRow label="Cấu trúc" value={`${selectedCurriculum.totalUnits || 0} unit · ${selectedCurriculum.totalSessions || 0} buổi`} /><SummaryRow label="Trạng thái" value={selectedCurriculum.statusLabel || selectedCurriculum.status || '—'} /></div> : <p className="mt-4 text-sm leading-6 text-[#806765]">Chọn chương trình đào tạo để khóa học kế thừa Unit, học liệu, Practice, Module Test và Flashcard.</p>}
          </section>

          <button className={`${PRIMARY_BUTTON_CLASS} w-full justify-center`} disabled={saving} onClick={save} type="button"><Save className="h-4 w-4" />{saving ? 'Đang lưu...' : 'Lưu khóa học'}</button>
          <Link className={`${SECONDARY_BUTTON_CLASS} w-full justify-center`} to={basePath}>Hủy và quay lại</Link>
        </aside>
      </div>
    </div>
  );
}

function BuilderSection({ children, number, title }) {
  return <section className="rounded-2xl border border-[#dcc0bf]/35 bg-white p-5 shadow-sm"><div className="mb-5 flex items-center gap-3 border-b border-[#f0e4e5] pb-4"><span className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#4b0009] text-xs font-black text-white">{number}</span><h2 className="font-['Manrope'] text-lg font-black text-[#0b1c30]">{title}</h2></div>{children}</section>;
}

function FieldLabel({ children }) {
  return <span className="mb-2 block text-xs font-bold uppercase tracking-[0.12em] text-slate-500">{children}</span>;
}

function TextInput({ label, value, onChange, type = 'text', min, placeholder }) {
  return <label className="block"><FieldLabel>{label}</FieldLabel><input className={FIELD_CLASS} min={min} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} type={type} value={value} /></label>;
}

function ReadOnlyField({ label, value }) {
  return <div><FieldLabel>{label}</FieldLabel><div className="rounded-xl border border-[#dcc0bf]/35 bg-[#f8f9ff] px-4 py-3 text-sm font-bold text-[#584140]">{value}</div></div>;
}

function SummaryRow({ label, value }) {
  return <div className="flex items-start justify-between gap-3 border-t border-[#f0e4e5] pt-3 text-sm"><span className="text-[#806765]">{label}</span><span className="text-right font-extrabold text-[#0b1c30]">{value}</span></div>;
}
