import BrandedSelect from '../ui/BrandedSelect';

const bandOptions = [
  { label: 'Tất cả trình độ', value: '' },
  { label: 'Band 4.5 trở xuống', value: '4.5' },
  { label: 'Band 5.0 đến 5.5', value: '5.5' },
  { label: 'Band 6.0 đến 6.5', value: '6.5' },
  { label: 'Band 7.0 trở lên', value: '7.0' },
];

const targetBandOptions = [
  { label: 'Tất cả mục tiêu', value: '' },
  { label: 'Band 6.0', value: '6.0' },
  { label: 'Band 6.5', value: '6.5' },
  { label: 'Band 7.0', value: '7.0' },
  { label: 'Band 7.5 trở lên', value: '7.5' },
];

const skillOptions = [
  { label: 'Tất cả kỹ năng', value: '' },
  { label: 'Listening', value: 'LISTENING' },
  { label: 'Reading', value: 'READING' },
  { label: 'Writing', value: 'WRITING' },
  { label: 'Speaking', value: 'SPEAKING' },
  { label: 'Vocabulary', value: 'VOCABULARY' },
  { label: 'Grammar', value: 'GRAMMAR' },
];

const fallbackCategoryOptions = [
  { label: 'Tất cả danh mục', value: '' },
  { label: 'IELTS', value: 'IELTS' },
  { label: 'TOEIC', value: 'TOEIC' },
  { label: 'Tiếng Anh giao tiếp', value: 'COMMUNICATION' },
  { label: 'Tiếng Anh nền tảng', value: 'FOUNDATION' },
];

const toeicTargetOptions = [
  { label: 'Tất cả mục tiêu', value: '' },
  { label: 'Từ 450 điểm', value: '450' },
  { label: 'Từ 650 điểm', value: '650' },
  { label: 'Từ 800 điểm', value: '800' },
  { label: 'Từ 900 điểm', value: '900' },
];

const promotionOptions = [
  { label: 'Tất cả trạng thái', value: '' },
  { label: 'Đang giảm giá', value: 'promotion' },
  { label: 'Không ưu đãi', value: 'standard' },
];

const CourseFilters = ({
  keyword,
  filters,
  onKeywordChange,
  onFilterChange,
  onClear,
  categories = [],
  selectedCategory = '',
}) => (
  <aside className="hidden space-y-8 lg:block">
    <div className="sticky top-28">
      <div className="mb-6">
        <p className="mb-2 text-[12px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Tìm và lọc</p>
        <h3 className="mb-3 text-[24px] font-semibold leading-[1.3] text-[#2b2828]">Toàn bộ khóa học</h3>
        <p className="text-sm leading-7 text-[#584140]">
          Thu hẹp danh sách theo trình độ, kỹ năng và mục tiêu để tìm khóa học phù hợp nhanh hơn.
        </p>
        <div className="mt-4 h-1 w-12 rounded bg-[#4b0009]" />
      </div>
      <div className="space-y-6 rounded-[28px] border border-[#dfbfbd]/30 bg-white p-5 shadow-sm">
        <div>
          <p className="mb-3 text-[12px] font-semibold uppercase tracking-[0.1em] text-[#584140]">Tìm kiếm theo tên khóa học</p>
          <input
            className="w-full rounded-[18px] border border-[#dfbfbd]/50 bg-white px-4 py-3 outline-none transition focus:border-[#4b0009] focus:ring-1 focus:ring-[#4b0009]"
            onChange={(event) => onKeywordChange(event.target.value)}
            placeholder="Nhập tên khóa học..."
            value={keyword}
          />
        </div>

        <div>
          <p className="mb-3 text-[12px] font-semibold uppercase tracking-[0.1em] text-[#584140]">Danh mục</p>
          <BrandedSelect
            name="category"
            onChange={onFilterChange}
            options={categories.length
              ? [{ label: 'Tất cả danh mục', value: '' }, ...categories.map((category) => ({ label: category.name, value: category.code }))]
              : fallbackCategoryOptions}
            placeholder="Chọn danh mục"
            value={filters.category}
          />
        </div>

        {selectedCategory === 'IELTS' ? (
          <>
            <div>
              <p className="mb-3 text-[12px] font-semibold uppercase tracking-[0.1em] text-[#584140]">Band IELTS hiện tại</p>
              <BrandedSelect name="currentBand" onChange={onFilterChange} options={bandOptions} placeholder="Chọn band hiện tại" value={filters.currentBand} />
            </div>

            <div>
              <p className="mb-3 text-[12px] font-semibold uppercase tracking-[0.1em] text-[#584140]">Band IELTS mục tiêu</p>
              <BrandedSelect name="targetBand" onChange={onFilterChange} options={targetBandOptions} placeholder="Chọn band mục tiêu" value={filters.targetBand} />
            </div>
          </>
        ) : null}

        {selectedCategory === 'TOEIC' ? (
          <div>
            <p className="mb-3 text-[12px] font-semibold uppercase tracking-[0.1em] text-[#584140]">Điểm TOEIC mục tiêu</p>
            <BrandedSelect name="toeicTarget" onChange={onFilterChange} options={toeicTargetOptions} placeholder="Chọn điểm mục tiêu" value={filters.toeicTarget} />
          </div>
        ) : null}

        <div>
          <p className="mb-3 text-[12px] font-semibold uppercase tracking-[0.1em] text-[#584140]">Kỹ năng trọng tâm</p>
          <BrandedSelect name="skill" onChange={onFilterChange} options={skillOptions} placeholder="Chọn kỹ năng" value={filters.skill} />
        </div>

        <div>
          <p className="mb-3 text-[12px] font-semibold uppercase tracking-[0.1em] text-[#584140]">Ưu đãi</p>
          <BrandedSelect name="promotion" onChange={onFilterChange} options={promotionOptions} placeholder="Chọn trạng thái ưu đãi" value={filters.promotion} />
        </div>
      </div>

      <button className="mt-8 w-full rounded-lg border border-[#4b0009] py-3 text-[14px] font-semibold tracking-[0.02em] text-[#4b0009] transition-all hover:bg-[#4b0009]/5" onClick={onClear} type="button">
        Xóa tất cả bộ lọc
      </button>
    </div>
  </aside>
);

export default CourseFilters;
