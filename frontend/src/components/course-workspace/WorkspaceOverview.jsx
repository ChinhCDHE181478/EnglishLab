const WorkspaceOverview = ({ course, enrollment }) => (
  <section className="rounded-[28px] border border-[#dfbfbd]/20 bg-white p-6 shadow-sm">
    <div className="flex flex-col gap-6 lg:flex-row lg:items-center">
      <div className="relative flex h-28 w-28 shrink-0 items-center justify-center">
        <svg className="h-full w-full -rotate-90" viewBox="0 0 120 120">
          <circle cx="60" cy="60" fill="transparent" r="52" stroke="#f1f1f0" strokeWidth="10" />
          <circle
            cx="60"
            cy="60"
            fill="transparent"
            r="52"
            stroke="#8a0018"
            strokeDasharray={326.7}
            strokeDashoffset={326.7 - (326.7 * (enrollment?.progressPercent || 0)) / 100}
            strokeLinecap="round"
            strokeWidth="10"
          />
        </svg>
        <div className="absolute text-center">
          <p className="text-2xl font-extrabold text-[#2b2828]">{enrollment?.progressPercent || 0}%</p>
          <p className="text-[8px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Hoàn thành</p>
        </div>
      </div>
      <div className="flex-1">
        <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Workspace tự học</p>
        <h1 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Tiếp tục học {course.title}</h1>
        <p className="mt-3 text-sm leading-7 text-[#584140]">
          Toàn bộ khóa học được thiết kế cho trải nghiệm tự học. Bạn có thể học video, đọc tài liệu, làm bài tập và giữ streak mỗi ngày.
        </p>
      </div>
    </div>
  </section>
);

export default WorkspaceOverview;
