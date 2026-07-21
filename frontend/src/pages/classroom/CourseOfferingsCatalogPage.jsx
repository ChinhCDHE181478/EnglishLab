import { useEffect, useMemo, useState } from 'react';
import { ArrowRight, BookOpenCheck, CalendarDays, Clock3, Search, Users, Video, Building2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import Header from '../../components/ai-learning/Header';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { PAGE_BODY_CLASS, PAGE_CONTAINER_CLASS, PAGE_SHELL_CLASS } from '../../utils/pageLayout';

const modeOptions = [
  { label: 'Tất cả hình thức', value: 'ALL' },
  { label: 'Khóa học Offline', value: 'OFFLINE' },
  { label: 'Khóa học Virtual', value: 'VIRTUAL' },
];

const formatDate = (value) => (value
  ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN')
  : 'Đang cập nhật');

const formatPrice = (value) => new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
}).format(Number(value || 0));

export default function CourseOfferingsCatalogPage() {
  const [offerings, setOfferings] = useState([]);
  const [mode, setMode] = useState('ALL');
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadOfferings = async () => {
    setLoading(true);
    setError('');
    try {
      setOfferings(await enrollmentRequestApi.getCourseOfferings());
    } catch (err) {
      setOfferings([]);
      setError(err?.response?.data?.message || 'Không thể tải danh sách khóa học đang nhận đăng ký.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOfferings();
  }, []);

  const filteredOfferings = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLocaleLowerCase('vi-VN');
    return offerings.filter((offering) => {
      const deliveryType = offering.deliveryType || offering.deliveryMode;
      const modeMatched = mode === 'ALL' || deliveryType === mode;
      const text = [
        offering.title,
        offering.code,
        offering.curriculumProgramTitle,
        offering.entryLevel,
        offering.plannedSchedule,
      ].filter(Boolean).join(' ').toLocaleLowerCase('vi-VN');
      return modeMatched && (!normalizedKeyword || text.includes(normalizedKeyword));
    });
  }, [keyword, mode, offerings]);

  const counts = useMemo(() => ({
    all: offerings.length,
    offline: offerings.filter((item) => (item.deliveryType || item.deliveryMode) === 'OFFLINE').length,
    virtual: offerings.filter((item) => (item.deliveryType || item.deliveryMode) === 'VIRTUAL').length,
  }), [offerings]);

  return (
    <div className={PAGE_SHELL_CLASS}>
      <CourseGlobalStyles />
      <Header />
      <div className={PAGE_BODY_CLASS}>
        <section className="relative overflow-hidden border-b border-[#ead9da] bg-[linear-gradient(135deg,#fff7f7_0%,#f4f7ff_48%,#fff_100%)]">
          <div className={`${PAGE_CONTAINER_CLASS} relative py-12 md:py-16`}>
            <div className="absolute -right-16 -top-20 h-64 w-64 rounded-full bg-[#730014]/5 blur-3xl" />
            <div className="relative max-w-3xl">
              <span className="inline-flex items-center gap-2 rounded-full border border-[#d9bfc2] bg-white/80 px-3 py-1 text-xs font-extrabold uppercase tracking-[0.14em] text-[#730014]">
                <BookOpenCheck className="h-4 w-4" /> Lịch dự kiến
              </span>
              <h1 className="mt-5 font-['Manrope'] text-3xl font-black tracking-tight text-[#0b1c30] md:text-5xl">
                Chọn khóa học phù hợp, <span className="text-[#8a0018]">EnglishLab sẽ xếp lớp</span> cùng bạn
              </h1>
              <p className="mt-4 max-w-2xl text-base leading-7 text-slate-600">
                Gửi nhu cầu học Offline hoặc Virtual. Nhân viên đào tạo sẽ kiểm tra placement test, xác nhận trình độ và đưa bạn vào nhóm chờ phù hợp trước khi đề xuất lớp.
              </p>
            </div>
            <div className="relative mt-8 grid max-w-2xl grid-cols-3 gap-3">
              <StatCard label="Đang nhận đăng ký" value={counts.all} />
              <StatCard label="Offline" value={counts.offline} />
              <StatCard label="Virtual" value={counts.virtual} />
            </div>
          </div>
        </section>

        <main className={`${PAGE_CONTAINER_CLASS} flex-1 py-8 md:py-10`}>
          <section className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[minmax(0,1fr)_260px] md:p-5">
            <label className="relative block">
              <span className="sr-only">Tìm khóa học</span>
              <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                className="h-12 w-full rounded-xl border border-slate-200 bg-slate-50 pl-11 pr-4 text-sm font-semibold text-[#0b1c30] outline-none transition focus:border-[#8a0018] focus:bg-white focus:ring-4 focus:ring-[#8a0018]/5"
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Tìm theo tên khóa học, cấp độ hoặc lịch dự kiến..."
                value={keyword}
              />
            </label>
            <BrandedSelect onChange={(event) => setMode(event.target.value)} options={modeOptions} value={mode} />
          </section>

          {error ? (
            <section className="mt-6 rounded-2xl border border-rose-200 bg-rose-50 p-8 text-center">
              <p className="font-bold text-rose-700">{error}</p>
              <button className="mt-4 rounded-xl bg-[#730014] px-5 py-2.5 text-sm font-bold text-white" onClick={loadOfferings} type="button">Thử lại</button>
            </section>
          ) : null}

          {loading ? (
            <div className="mt-6 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
              {Array.from({ length: 6 }).map((_, index) => <div className="h-[390px] animate-pulse rounded-2xl bg-slate-100" key={index} />)}
            </div>
          ) : null}

          {!loading && !error && !filteredOfferings.length ? (
            <section className="mt-6 flex min-h-[360px] flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white px-6 text-center">
              <BookOpenCheck className="h-12 w-12 text-slate-300" />
              <h2 className="mt-4 font-['Manrope'] text-xl font-black text-[#0b1c30]">Chưa có khóa học phù hợp</h2>
              <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">Thử đổi hình thức hoặc từ khóa. Các khóa học mới sẽ xuất hiện khi Content Manager xuất bản.</p>
            </section>
          ) : null}

          {!loading && !error && filteredOfferings.length ? (
            <div className="mt-6 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
              {filteredOfferings.map((offering) => <OfferingCard key={offering.id} offering={offering} />)}
            </div>
          ) : null}
        </main>
      </div>
      <CourseFooter />
    </div>
  );
}

function StatCard({ label, value }) {
  return (
    <div className="rounded-2xl border border-white/80 bg-white/75 px-4 py-4 shadow-sm backdrop-blur">
      <p className="font-['Manrope'] text-2xl font-black text-[#730014]">{value}</p>
      <p className="mt-1 text-xs font-bold text-slate-500">{label}</p>
    </div>
  );
}

function OfferingCard({ offering }) {
  const deliveryType = offering.deliveryType || offering.deliveryMode;
  const isVirtual = deliveryType === 'VIRTUAL';
  const displayedPrice = offering.salePrice ?? offering.price;
  return (
    <article className="group flex h-full flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition duration-300 hover:-translate-y-1 hover:border-[#d7b9bd] hover:shadow-xl hover:shadow-[#730014]/5">
      <div className={`relative h-36 overflow-hidden ${isVirtual ? 'bg-[linear-gradient(135deg,#0b1c30,#3156a3)]' : 'bg-[linear-gradient(135deg,#4b0009,#a61b32)]'}`}>
        {offering.thumbnailUrl ? <img alt="" className="h-full w-full object-cover opacity-55 transition duration-500 group-hover:scale-105" src={offering.thumbnailUrl} /> : null}
        <div className="absolute inset-0 bg-gradient-to-t from-black/45 to-transparent" />
        <span className="absolute left-4 top-4 inline-flex items-center gap-1.5 rounded-full bg-white/95 px-3 py-1 text-xs font-extrabold text-[#0b1c30] shadow-sm">
          {isVirtual ? <Video className="h-3.5 w-3.5 text-sky-600" /> : <Building2 className="h-3.5 w-3.5 text-[#8a0018]" />}
          {isVirtual ? 'Virtual' : 'Offline'}
        </span>
        <p className="absolute bottom-4 left-4 right-4 text-xs font-bold uppercase tracking-[0.12em] text-white/90">{offering.code}</p>
      </div>
      <div className="flex flex-1 flex-col p-5">
        <p className="text-xs font-extrabold uppercase tracking-[0.12em] text-[#8a0018]">{offering.entryLevel || 'Mọi trình độ'}</p>
        <h2 className="mt-2 line-clamp-2 font-['Manrope'] text-xl font-black leading-7 text-[#0b1c30]">{offering.title}</h2>
        <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-500">{offering.shortDescription || offering.targetOutcome || 'Lộ trình học có hướng dẫn theo chương trình đào tạo đã duyệt.'}</p>
        <div className="mt-5 grid gap-3 border-y border-slate-100 py-4 text-sm">
          <Info icon={CalendarDays} label="Khai giảng dự kiến" value={formatDate(offering.plannedStartDate)} />
          <Info icon={Clock3} label="Lịch dự kiến" value={offering.plannedSchedule || offering.studyMode || 'Đang cập nhật'} />
          <Info icon={Users} label="Quy mô dự kiến" value={`${offering.capacity ?? offering.maxCapacity ?? 30} học viên`} />
        </div>
        <div className="mt-auto flex items-end justify-between gap-4 pt-5">
          <div>
            <p className="text-xs font-semibold text-slate-400">Học phí tham khảo</p>
            <p className="mt-1 font-['Manrope'] text-lg font-black text-[#8a0018]">{formatPrice(displayedPrice)}</p>
          </div>
          <Link className="inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4 py-2.5 text-xs font-extrabold text-white transition hover:bg-[#8a0018]" to={`/opening-schedule/${offering.slug || offering.id}`}>
            Xem chi tiết <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
      </div>
    </article>
  );
}

function Info({ icon: Icon, label, value }) {
  return (
    <div className="grid grid-cols-[20px_minmax(0,1fr)] gap-2">
      <Icon className="mt-0.5 h-4 w-4 text-[#8a0018]" />
      <div className="min-w-0">
        <p className="text-[11px] font-bold uppercase tracking-wide text-slate-400">{label}</p>
        <p className="mt-0.5 truncate font-bold text-slate-700">{value}</p>
      </div>
    </div>
  );
}
