const mascotItems = [
  '/streaks/streak-3-days.png',
  '/streaks/streak-7-days.png',
  '/streaks/streak-hug-fire.png',
];

const WorkspaceRightRail = ({ enrollment }) => (
  <aside className="space-y-6">
    <div className="rounded-[28px] border border-[#dfbfbd]/20 bg-white p-6 shadow-sm">
      <div className="flex items-center gap-3">
        <span className="material-symbols-outlined text-3xl text-[#ff7a00]" style={{ fontVariationSettings: "'FILL' 1" }}>local_fire_department</span>
        <div>
          <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Streak hiện tại</p>
          <p className="text-3xl font-extrabold text-[#2b2828]">{enrollment?.streakDays || 0} ngày</p>
        </div>
      </div>
      <p className="mt-4 text-sm leading-7 text-[#584140]">
        Chuỗi được tính từ các ngày bạn hoàn thành bài học trong hệ thống.
      </p>
      <div className="mt-5 flex items-center justify-between gap-3">
        {mascotItems.map((item) => (
          <div key={item} className="flex-1 rounded-2xl bg-[#fff7f6] p-3">
            <img alt="Streak mascot" className="mx-auto h-20 w-20 object-contain" src={item} />
          </div>
        ))}
      </div>
    </div>
    <div className="rounded-[28px] border border-[#dfbfbd]/20 bg-[linear-gradient(135deg,_#fff7f6,_#ffffff)] p-6 shadow-sm">
      <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Tài nguyên</p>
      <h3 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Tự học không cần giáo viên</h3>
      <ul className="mt-4 space-y-3 text-sm leading-7 text-[#584140]">
        <li>Video bài giảng theo module và preview bài học.</li>
        <li>Tài liệu PDF và file bài tập tự luyện.</li>
        <li>Theo dõi tiến độ và streak ngay trong workspace.</li>
      </ul>
    </div>
  </aside>
);

export default WorkspaceRightRail;
