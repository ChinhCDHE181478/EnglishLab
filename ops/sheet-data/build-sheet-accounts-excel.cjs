const fs = require("fs");
const path = require("path");
const XLSX = require(path.join(__dirname, "../../frontend/node_modules/@e965/xlsx"));

const PASSWORD = "Password123!";
const TEACHER_EMAIL = "alien1062004@gmail.com";
const LEARNER_EMAIL = "0386852628z@gmail.com";
const TEACHER_NAMES = [
  "Nguyễn Minh Trí", "Trần Thu Hà", "Lê Quang Huy", "Phạm Ngọc Anh", "Hoàng Mai Linh",
  "Vũ Đình Nam", "Đặng Thảo Nhi", "Bùi Tuấn Kiệt", "Đỗ Mỹ Dung", "Ngô Thanh Sơn",
  "Lý Khánh Vân", "Cao Đức Long", "Mai Phương Thảo", "Trịnh Gia Bảo", "Phan Hà My",
  "Đinh Việt Anh", "Lâm Quỳnh Chi", "Huỳnh Tấn Phát", "Võ Bảo Ngọc"
];
const FIRST_NAMES = [
  "An", "Bình", "Chi", "Dũng", "Giang", "Hà", "Khánh", "Linh", "Minh", "Nam",
  "Oanh", "Phúc", "Quỳnh", "Sơn", "Tâm", "Uyên", "Vy", "Yến", "Huy", "My"
];
const LAST_NAMES = [
  "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Vũ", "Đặng", "Bùi", "Đỗ", "Ngô"
];

const quanTri = [
  ["classroom.admin@englishlab.vn", "Nguyễn Admin", "ADMIN", "Quản trị hệ thống"],
  ["classroom.manager@englishlab.vn", "Quản lý lớp học", "MANAGER", "Quản lý lớp / đề xuất lớp"],
  ["staff@englishlab.vn", "Nhân viên đào tạo", "STAFF", "Tuyển sinh, xếp lớp, học phí"],
  ["content.manager@englishlab.vn", "Quản lý Content", "CONTENT_MANAGER", "Khóa online, nội dung catalog"]
];

const giaoVien = [
  {
    stt: 1,
    email: TEACHER_EMAIL,
    hoTen: "Trần Minh Huy",
    ma: "SHOWCASE",
    ghiChu: "Giáo viên showcase Google Meet / recording. Lớp 01 (offline) và lớp 25 (Live Meet)."
  },
  ...TEACHER_NAMES.map((hoTen, i) => ({
    stt: i + 2,
    email: `gv.sheet.${String(i + 1).padStart(2, "0")}@englishlab.vn`,
    hoTen,
    ma: `GV${String(i + 1).padStart(2, "0")}`,
    ghiChu: "Giáo viên ca tối sheet, 3 buổi/tuần"
  }))
];

const hocVienSheet = [];
for (let i = 1; i <= 299; i += 1) {
  hocVienSheet.push({
    stt: i,
    email: `hs.sheet.${String(i).padStart(3, "0")}@englishlab.vn`,
    hoTen: `${LAST_NAMES[i % LAST_NAMES.length]} ${FIRST_NAMES[i % FIRST_NAMES.length]} ${i}`,
    ma: `HS${String(i).padStart(3, "0")}`
  });
}

const learners = [
  { email: LEARNER_EMAIL, hoTen: "Lê Ngọc Anh" },
  ...hocVienSheet.map((item) => ({ email: item.email, hoTen: item.hoTen }))
];
const teachers = [
  { email: TEACHER_EMAIL, hoTen: "Trần Minh Huy" },
  ...giaoVien.slice(1).map((item) => ({ email: item.email, hoTen: item.hoTen }))
];

const lopHoc = [];
let learnerCursor = 1;
for (let classIndex = 0; classIndex < 30; classIndex += 1) {
  const online = classIndex >= 24;
  const intake = classIndex % 3;
  const mwf = Math.floor(classIndex / 3) % 2 === 0;
  const eveningTwo = classIndex % 2 === 1;
  const teacher = (classIndex === 0 || classIndex === 24)
    ? teachers[0]
    : teachers[1 + (classIndex % 19)];
  const title = `${online ? "IELTS Live Meet" : "IELTS Center"} ${
    intake === 0 ? "K1" : intake === 1 ? "K2" : "K3"
  } ${mwf ? "T2-4-6" : "T3-5-7"} ${eveningTwo ? "Ca 2" : "Ca 1"}`;
  const classLearners = [];
  if (classIndex === 0 || classIndex === 24) {
    classLearners.push(learners[0]);
  }
  while (classLearners.length < 10) {
    const next = learners[learnerCursor % learners.length];
    learnerCursor += 1;
    if (!classLearners.some((item) => item.email === next.email)) {
      classLearners.push(next);
    }
  }
  lopHoc.push({
    stt: classIndex + 1,
    slug: `center-sheet-class-${String(classIndex + 1).padStart(2, "0")}`,
    tenLop: title,
    hinhThuc: online ? "Google Meet" : "Tại trung tâm",
    lich: mwf ? "Thứ 2-4-6" : "Thứ 3-5-7",
    ca: eveningTwo ? "Ca 2 (19:45-21:15)" : "Ca 1 (18:00-19:30)",
    phong: online ? "—" : `P${String((classIndex % 10) + 1).padStart(3, "0")}`,
    gvEmail: teacher.email,
    gvTen: teacher.hoTen,
    siSo: classLearners.length,
    hvEmails: classLearners.map((item) => item.email).join("; "),
    hvTen: classLearners.map((item) => item.hoTen).join("; ")
  });
}

function sheetFromRows(headers, rows) {
  const data = [headers, ...rows];
  const ws = XLSX.utils.aoa_to_sheet(data);
  ws["!cols"] = headers.map((header, col) => {
    let max = String(header).length;
    rows.forEach((row) => {
      const value = row[col] == null ? "" : String(row[col]);
      max = Math.min(60, Math.max(max, value.length));
    });
    return { wch: Math.max(12, max + 2) };
  });
  ws["!autofilter"] = {
    ref: XLSX.utils.encode_range({ s: { r: 0, c: 0 }, e: { r: rows.length, c: headers.length - 1 } })
  };
  return ws;
}

const wb = XLSX.utils.book_new();

XLSX.utils.book_append_sheet(wb, sheetFromRows(
  ["Mục", "Nội dung"],
  [
    ["Chế độ dữ liệu", "Sheet local (APP_SEED_SHEET_ENABLED=true), không dùng cho production"],
    ["Mật khẩu mặc định", PASSWORD],
    ["Ghi chú mật khẩu", "Chỉ áp dụng tài khoản mới tạo. Email Gmail đã tồn tại giữ mật khẩu / Google login cũ, seeder không ghi đè."],
    ["Cơ sở", "EnglishLab Hai Bà Trưng — 123 Phố Huế, Hai Bà Trưng, Hà Nội"],
    ["Lịch trung tâm", "Thứ 2–7, nghỉ Chủ nhật; 2 ca tối 18:00–19:30 và 19:45–21:15"],
    ["Quy mô", "4 quản trị · 20 giáo viên · 300 học viên · 30 lớp (24 offline + 6 Meet) · 10 học viên/lớp"],
    ["Showcase giáo viên", TEACHER_EMAIL],
    ["Showcase học viên", LEARNER_EMAIL],
    ["Học viên showcase — khóa online", "Hoàn thành: IELTS Master Vocabulary. Đang học: E2 Practice, Communication for Work. Có thể thêm Listening (xong) + Reading (đang học)."],
    ["Học viên showcase — lớp", "Lớp 01 offline (center-sheet-class-01) và lớp 25 Live Meet (center-sheet-class-25)"],
    ["Tài khoản kèm test seeder", "certificate.learner@englishlab.vn (Password123!) nếu APP_SEED_TEST_ENABLED=true"],
    ["Cách dùng Excel", "Mỗi tab một nhóm tài khoản. Tab Lớp học ghép GV + 10 HV. Tab Tất cả để lọc nhanh."]
  ]
), "Huong_dan");

XLSX.utils.book_append_sheet(wb, sheetFromRows(
  ["STT", "Vai trò", "Họ tên", "Email", "Mật khẩu", "Ghi chú"],
  quanTri.map((item, i) => [i + 1, item[2], item[1], item[0], PASSWORD, item[3]])
), "Quan_tri");

XLSX.utils.book_append_sheet(wb, sheetFromRows(
  ["STT", "Mã", "Họ tên", "Email", "Mật khẩu", "Vai trò", "Ghi chú"],
  giaoVien.map((item) => [item.stt, item.ma, item.hoTen, item.email, PASSWORD, "TEACHER", item.ghiChu])
), "Giao_vien");

XLSX.utils.book_append_sheet(wb, sheetFromRows(
  ["STT", "Họ tên", "Email", "Mật khẩu", "Vai trò", "Lớp", "Ghi chú"],
  [[
    1,
    "Lê Ngọc Anh",
    LEARNER_EMAIL,
    PASSWORD,
    "LEARNER",
    "center-sheet-class-01; center-sheet-class-25",
    "Nếu Gmail đã có trước: đăng nhập Google / mật khẩu cũ. Placement đã submit qua API. Có homework lớp 01."
  ]]
), "Hoc_vien_showcase");

XLSX.utils.book_append_sheet(wb, sheetFromRows(
  ["STT", "Mã", "Họ tên", "Email", "Mật khẩu", "Vai trò"],
  hocVienSheet.map((item) => [item.stt, item.ma, item.hoTen, item.email, PASSWORD, "LEARNER"])
), "Hoc_vien_sheet");

XLSX.utils.book_append_sheet(wb, sheetFromRows(
  ["STT", "Slug lớp", "Tên lớp", "Hình thức", "Lịch", "Ca", "Phòng", "Email giáo viên", "Tên giáo viên", "Sĩ số", "Email học viên", "Tên học viên"],
  lopHoc.map((item) => [
    item.stt, item.slug, item.tenLop, item.hinhThuc, item.lich, item.ca, item.phong,
    item.gvEmail, item.gvTen, item.siSo, item.hvEmails, item.hvTen
  ])
), "Lop_hoc");

const tatCa = [
  ...quanTri.map((item) => ["Quản trị", item[2], item[1], item[0], PASSWORD, item[3]]),
  ...giaoVien.map((item) => ["Giáo viên", "TEACHER", item.hoTen, item.email, PASSWORD, item.ghiChu]),
  ["Học viên showcase", "LEARNER", "Lê Ngọc Anh", LEARNER_EMAIL, PASSWORD, "Lớp 01 + lớp 25"],
  ...hocVienSheet.map((item) => ["Học viên sheet", "LEARNER", item.hoTen, item.email, PASSWORD, item.ma]),
  ["Test seeder", "LEARNER", "Học viên Chứng nhận Demo", "certificate.learner@englishlab.vn", PASSWORD, "Chỉ khi APP_SEED_TEST_ENABLED=true"]
];
XLSX.utils.book_append_sheet(wb, sheetFromRows(
  ["Nhóm", "Vai trò", "Họ tên", "Email", "Mật khẩu", "Ghi chú"],
  tatCa
), "Tat_ca");

const fileName = "EnglishLab-Sheet-TaiKhoan.xlsx";
const repoPath = path.join(__dirname, fileName);
const downloadsPath = path.join(process.env.USERPROFILE || "", "Downloads", fileName);
XLSX.writeFile(wb, repoPath, { bookSST: true, compression: true });
fs.copyFileSync(repoPath, downloadsPath);
process.stdout.write(`${repoPath}\n${downloadsPath}\n`);
