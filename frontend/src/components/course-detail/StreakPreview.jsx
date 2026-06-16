const streakCards = [
  { image: '/streaks/streak-3-days.png', label: '3 ngày', title: 'Khởi động thói quen học đều' },
  { image: '/streaks/streak-7-days.png', label: '7 ngày', title: 'Bắt nhịp vững vàng với nhịp học' },
  { image: '/streaks/streak-60-days.png', label: '60 ngày', title: 'Giữ lửa đường dài cho mục tiêu lớn' },
];

const StreakPreview = () => (
  <section className="rounded-[28px] border border-[#dfbfbd]/25 bg-[linear-gradient(135deg,_#4b0009,_#8a0018)] p-6 text-white shadow-sm">
    <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-white/70">Động lực tự học</p>
    <h2 className="mt-2 font-['Manrope'] text-3xl font-extrabold">Chuỗi học tập và sticker mascot</h2>
    <p className="mt-3 max-w-3xl text-sm leading-7 text-white/80">
      Mỗi lần quay lại học, hoàn thành bài tập và giữ streak, bạn sẽ mở thêm các mốc tiến độ để biết mình đang đi đến đâu trên hành trình tự học.
    </p>
    <div className="mt-6 grid gap-4 md:grid-cols-3">
      {streakCards.map((item) => (
        <div key={item.label} className="rounded-3xl border border-white/10 bg-white/10 p-5 backdrop-blur-sm">
          <img alt={item.title} className="mx-auto h-32 w-32 object-contain" src={item.image} />
          <p className="mt-3 text-[11px] font-bold uppercase tracking-[0.14em] text-white/65">{item.label}</p>
          <h3 className="mt-2 text-lg font-extrabold">{item.title}</h3>
        </div>
      ))}
    </div>
  </section>
);

export default StreakPreview;
