import MaterialIcon from './MaterialIcon';

const paths = [
  {
    icon: 'leaderboard',
    title: 'Từ mất gốc đến IELTS 5.0',
    description: 'Lộ trình dài hạn tập trung xây dựng nền tảng từ con số 0, giúp bạn làm quen với định dạng bài thi IELTS.',
  },
  {
    icon: 'auto_graph',
    title: 'Từ IELTS 5.0 đến 6.5+',
    description: 'Khóa học nâng cao giúp bạn làm chủ các dạng bài khó và đạt được mức điểm mục tiêu để du học hoặc định cư.',
  },
  {
    icon: 'rocket_launch',
    title: 'Từ TOEIC 450 đến 750+',
    description: 'Lộ trình chuyên biệt tối ưu điểm số cho sinh viên sắp tốt nghiệp và người đi làm trong thời gian ngắn nhất.',
  },
];

const LearningPaths = () => (
  <section className="mb-[80px]">
    <h2 className="font-headline-lg mb-8 text-center text-[32px] font-bold leading-[1.2]">Lộ trình học tập toàn diện</h2>
    <div className="grid grid-cols-1 gap-8 md:grid-cols-3">
      {paths.map((path) => (
        <div key={path.title} className="group relative overflow-hidden rounded-2xl border border-[#dfbfbd]/30 bg-white p-8 shadow-sm transition-all duration-300 hover:-translate-y-2 hover:shadow-xl">
          <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-[#4b0009]/10">
            <MaterialIcon name={path.icon} className="text-4xl text-[#4b0009]" />
          </div>
          <h3 className="font-headline-md mb-4 text-[24px] font-semibold leading-[1.3]">{path.title}</h3>
          <p className="mb-6 text-[#584140]">{path.description}</p>
          <a className="inline-flex items-center gap-2 font-bold text-[#4b0009]" href="#catalog">
            Chi tiết lộ trình <MaterialIcon name="arrow_forward" className="text-sm transition-transform group-hover:translate-x-1" />
          </a>
        </div>
      ))}
    </div>
  </section>
);

export default LearningPaths;
