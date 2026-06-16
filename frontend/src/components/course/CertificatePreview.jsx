const formatDateLong = (value) => {
  if (!value) return 'Ngày hoàn thành chưa được ghi nhận.';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Ngày hoàn thành chưa được ghi nhận.';
  return `${date.getDate().toString().padStart(2, '0')} tháng ${(date.getMonth() + 1).toString().padStart(2, '0')} năm ${date.getFullYear()}`;
};

const CertificatePreview = ({ certificate }) => {
  if (!certificate) return null;

  const verificationUrl = `englishlab.edu.vn/xac-thuc/${certificate.verificationCode}`;

  return (
    <div className="mx-auto w-full max-w-[1122px]">
      <style>
        {`
          @media print {
            body {
              background: white !important;
            }
            .khong-in {
              display: none !important;
            }
            .khung-chung-nhan {
              box-shadow: none !important;
              margin: 0 !important;
            }
            @page {
              size: A4 landscape;
              margin: 0;
            }
          }
        `}
      </style>

      <div className="khung-chung-nhan relative mx-auto h-[794px] overflow-hidden bg-white shadow-[0_18px_45px_rgba(0,0,0,0.12)]">
        <div className="absolute inset-0 bg-[radial-gradient(rgba(115,0,20,0.03)_1px,transparent_1px)] bg-[length:20px_20px] opacity-60" />
        <div className="pointer-events-none absolute left-5 top-5 h-[calc(100%-40px)] w-[calc(100%-40px)] border-4 border-double border-[#dadad9]" />

        <aside className="absolute bottom-5 right-10 top-5 z-10 flex w-[200px] flex-col items-center bg-[#f0f0f0] px-4 pt-[60px] shadow-inner">
          <div className="mb-4 text-center text-xs font-bold uppercase tracking-[0.35em] text-[#7f7f7f]" style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}>
            Chứng nhận hoàn thành
          </div>

          <div className="mt-20 flex h-[150px] w-[150px] items-center justify-center rounded-full border-4 border-double border-[#730014] bg-white">
            <div className="relative flex h-[132px] w-[132px] items-center justify-center rounded-full border border-dashed border-[#730014]">
              <div className="text-center text-[#730014]">
                <p className="text-[10px] font-bold uppercase leading-tight">EnglishLab</p>
                <p className="text-[14px] font-extrabold uppercase">Excellence</p>
                <p className="text-[8px] italic">Chứng nhận</p>
              </div>
            </div>
          </div>

          <div className="mt-auto pb-10 text-center">
            <p className="text-[10px] font-bold uppercase leading-tight tracking-[0.18em] text-[#9b9b9b]">
              EnglishLab Academy
              <br />
              Chuẩn học tập toàn diện
            </p>
          </div>
        </aside>

        <main className="relative z-10 flex h-full flex-col p-20" style={{ width: 'calc(100% - 240px)' }}>
          <header className="mb-16">
            <div className="flex items-center gap-3">
              <span className="flex h-14 w-10 items-center gap-2">
                <span className="h-14 w-4 rounded-[2px] bg-[#8a0018]" />
                <span className="h-10 w-3 rounded-[2px] bg-[#c45a64]" />
              </span>
              <span className="font-['Manrope'] text-4xl font-extrabold tracking-tight text-[#2b2828]">
                English<span className="text-[#8a0018]">Lab</span>
              </span>
            </div>
          </header>

          <section className="mb-6">
            <p className="text-sm font-medium tracking-wide text-[#7f7f7f]">Ngày cấp: {formatDateLong(certificate.completionDate)}</p>
          </section>

          <section className="mb-8">
            <h1 className="font-['Playfair_Display'] text-6xl font-bold text-[#730014]">{certificate.learnerName || 'Học viên EnglishLab'}</h1>
            <div className="mt-4 h-1 w-32 bg-[#730014]/30" />
          </section>

          <section className="mb-12 max-w-2xl">
            <p className="mb-2 text-lg italic text-[#6f6f6f]">Đã hoàn thành xuất sắc khóa học</p>
            <h2 className="text-3xl font-bold leading-tight text-[#2b2828]">{certificate.courseTitle}</h2>
            <p className="mt-4 text-sm leading-relaxed text-[#7f7f7f]">
              Chứng nhận này xác nhận học viên đã hoàn thành đầy đủ các yêu cầu học tập, bài đánh giá bắt buộc và đạt chuẩn đầu ra cam kết của EnglishLab.
            </p>
            <p className="mt-3 text-sm leading-relaxed text-[#7f7f7f]">
              Mục tiêu đầu ra: {certificate.targetOutcome || 'Đang cập nhật mục tiêu đầu ra.'}
            </p>
          </section>

          <footer className="mt-auto flex items-end justify-between">
            <div className="flex flex-col">
              <div className="mb-2 flex h-16 w-48 items-end">
                <div className="font-['Playfair_Display'] text-3xl italic text-[#730014]/80">EnglishLab</div>
              </div>
              <div className="border-t border-[#d4d4d4] pt-2">
                <p className="font-bold text-[#2b2828]">EnglishLab</p>
                <p className="text-xs uppercase tracking-[0.16em] text-[#7f7f7f]">EnglishLab Academy</p>
              </div>
            </div>

            <div className="max-w-xs text-right">
              <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.14em] text-[#9b9b9b]">Xác minh tại:</p>
              <p className="text-sm font-bold text-[#730014] underline">{verificationUrl}</p>
              <p className="mt-2 text-[9px] leading-tight text-[#9b9b9b]">
                Chứng nhận này được cấp bởi EnglishLab Academy. Mã xác thực: {certificate.verificationCode}
              </p>
            </div>
          </footer>
        </main>
      </div>
    </div>
  );
};

export default CertificatePreview;
