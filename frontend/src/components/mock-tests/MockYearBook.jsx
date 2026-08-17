export default function MockYearBook({ year, examType = 'IELTS' }) {
  const isToeic = examType === 'TOEIC';
  return (
    <div className={`el-mock-book${isToeic ? ' el-mock-book--toeic' : ''}`} tabIndex={0}>
      <div className="el-mock-book__stage">
        <div className="el-mock-book__block">
          <div className="el-mock-book__spine" />
          <div className="el-mock-book__page">
            <span className="el-mock-book__page-kicker">EnglishLab</span>
            <p className="el-mock-book__page-title">{isToeic ? `TOEIC Mock ${year}` : `IELTS Mock ${year}`}</p>
            <p className="el-mock-book__page-copy">
              {isToeic
                ? 'Chọn bộ đề bên cạnh để mở Listening và Reading.'
                : 'Chọn tháng bên cạnh để mở đề Listening, Reading, Writing và Speaking.'}
            </p>
          </div>
          <div className="el-mock-book__cover">
            <span className="el-mock-book__brand">EnglishLab</span>
            <span className="el-mock-book__title">
              {isToeic ? 'TOEIC' : 'IELTS'}
              <em>Mock Test</em>
            </span>
            <span className="el-mock-book__year">{year}</span>
          </div>
        </div>
      </div>
      <p className="el-mock-book__hint">Di chuột vào sách để lật trang</p>
    </div>
  );
}
