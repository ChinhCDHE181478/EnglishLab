const filterGroups = [
  { title: 'Mục tiêu', options: ['IELTS Academic', 'TOEIC Listening & Reading', 'Giao tiếp đi làm'] },
  { title: 'Trình độ', options: ['Mới bắt đầu', 'Trung cấp (4.5 - 5.5)', 'Nâng cao (6.0+)'] },
  { title: 'Hình thức', options: ['Học tại trung tâm', 'Học trực tuyến (Live)'] },
];

const CourseFilters = ({ keyword, onKeywordChange, onClear }) => (
  <aside className="hidden space-y-8 lg:block">
    <div className="sticky top-28">
      <div className="mb-6">
        <h3 className="font-headline-md mb-4 text-[24px] font-semibold leading-[1.3]">Bộ lọc</h3>
        <div className="h-1 w-12 rounded bg-[#4b0009]" />
      </div>
      <div className="space-y-6">
        <div>
          <p className="mb-3 text-[12px] font-semibold uppercase leading-none tracking-[0.1em] text-[#584140]">Tìm kiếm</p>
          <input
            className="w-full rounded-lg border border-[#dfbfbd]/50 bg-white px-3 py-2 outline-none transition focus:border-[#4b0009] focus:ring-1 focus:ring-[#4b0009]"
            placeholder="Tên khóa học..."
            value={keyword}
            onChange={(event) => onKeywordChange(event.target.value)}
          />
        </div>
        {filterGroups.map((group) => (
          <div key={group.title}>
            <p className="mb-3 text-[12px] font-semibold uppercase leading-none tracking-[0.1em] text-[#584140]">{group.title}</p>
            <div className="space-y-2">
              {group.options.map((option) => (
                <label key={option} className="group flex cursor-pointer items-center gap-3">
                  <input className="h-4 w-4 rounded border-[#dfbfbd] text-[#4b0009] focus:ring-[#4b0009]" type="checkbox" />
                  <span className="transition-colors group-hover:text-[#4b0009]">{option}</span>
                </label>
              ))}
            </div>
          </div>
        ))}
        <div>
          <p className="mb-3 text-[12px] font-semibold uppercase leading-none tracking-[0.1em] text-[#584140]">Thời lượng</p>
          <select className="w-full rounded-lg border border-[#dfbfbd]/50 bg-white px-3 py-2 outline-none focus:border-[#4b0009] focus:ring-1 focus:ring-[#4b0009]">
            <option>Mọi thời lượng</option>
            <option>Dưới 2 tháng</option>
            <option>2 - 4 tháng</option>
            <option>Trên 4 tháng</option>
          </select>
        </div>
      </div>
      <button className="mt-8 w-full rounded-lg border border-[#4b0009] py-3 text-[14px] font-semibold leading-none tracking-[0.02em] text-[#4b0009] transition-all hover:bg-[#4b0009]/5" type="button" onClick={onClear}>
        Xóa tất cả bộ lọc
      </button>
    </div>
  </aside>
);

export default CourseFilters;
