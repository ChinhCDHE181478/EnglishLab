import MaterialIcon from './MaterialIcon';

const programLinks = ['Luyện thi IELTS', 'Luyện thi TOEIC', 'Tiếng Anh Giao Tiếp', 'Khóa học Online'];
const supportLinks = ['Academic Calendar', 'Faculty Directory', 'Accreditation', 'Privacy Policy'];

const FooterColumn = ({ title, items }) => (
  <div>
    <h4 className="mb-6 font-bold text-white">{title}</h4>
    <ul className="space-y-4">
      {items.map((item) => (
        <li key={item}>
          <a href="#" className="text-[#e2e2e2]/80 transition-all hover:text-[#f9f9f9]">
            {item}
          </a>
        </li>
      ))}
    </ul>
  </div>
);

const CourseFooter = () => (
  <footer className="w-full bg-[#2f3131] pb-12 pt-20">
    <div className="mx-auto mb-12 grid max-w-[1280px] grid-cols-1 gap-6 px-10 md:grid-cols-4">
      <div>
        <a href="#" className="mb-6 block font-['Manrope'] text-2xl font-bold leading-[1.3] text-white">
          EnglishLab
        </a>
        <p className="mb-6 text-sm leading-relaxed text-[#e2e2e2]/70">
          Học viện đào tạo Tiếng Anh học thuật hàng đầu, cam kết mang lại giá trị kiến thức bền vững và thành công vượt mong đợi cho học viên.
        </p>
        <div className="flex gap-4">
          {['public', 'video_library', 'chat'].map((icon) => (
            <a
              key={icon}
              href="#"
              className="flex h-10 w-10 items-center justify-center rounded-full bg-[#e2e2e2]/10 text-white transition-all hover:bg-[#4b0009]"
            >
              <MaterialIcon name={icon} />
            </a>
          ))}
        </div>
      </div>

      <FooterColumn title="Chương trình học" items={programLinks} />
      <FooterColumn title="Thông tin hỗ trợ" items={supportLinks} />

      <div>
        <h4 className="mb-6 font-bold text-white">Liên hệ</h4>
        <ul className="space-y-4 text-sm text-[#e2e2e2]/80">
          <li className="flex items-start gap-3 text-white">
            <MaterialIcon name="location_on" className="text-lg text-[#ffdad8]" />
            123 Đường Sư Vạn Hạnh, Quận 10, TP. Hồ Chí Minh
          </li>
          <li className="flex items-center gap-3 text-white">
            <MaterialIcon name="call" className="text-lg text-[#ffdad8]" />
            1900 6789
          </li>
          <li className="flex items-center gap-3 text-white">
            <MaterialIcon name="mail" className="text-lg text-[#ffdad8]" />
            contact@englishlab.edu.vn
          </li>
        </ul>
      </div>
    </div>

    <div className="mx-auto flex max-w-[1280px] flex-col items-center justify-between gap-4 border-t border-[#e2e2e2]/10 px-10 pt-8 md:flex-row">
      <p className="text-xs text-[#e2e2e2]/60">© 2024 EnglishLab Academy of Excellence</p>
    </div>
  </footer>
);

export default CourseFooter;
